#!/usr/bin/env python3
"""Relational arm: the SAME extracted relations, in SQLite and DuckDB.

This arm exists to answer one question the author needs: if plain SQL answers it
as well as Cypher does, the win belongs to having a structured layer, not to a
graph database.

It consumes graphbuild.build() output unchanged, exactly as arm_graph.py does.

Usage:
  python3 arm_sql.py --relations out/relations.json --out out/sql.json --tag main
"""

import argparse
import json
import os
import sqlite3
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import graphbuild  # noqa: E402

DDL = """
CREATE TABLE entity (name TEXT PRIMARY KEY, kind TEXT);
CREATE TABLE node   (kind TEXT, name TEXT);
CREATE TABLE rel (
  rtype TEXT, from_kind TEXT, from_e TEXT, to_kind TEXT, to_e TEXT,
  stance TEXT, doc_id TEXT, doc_type TEXT, customer_named INTEGER, quote TEXT);
CREATE TABLE claim (
  ctype TEXT, company TEXT, n INTEGER, of_m INTEGER,
  customer_class TEXT, doc_id TEXT, quote TEXT);
CREATE TABLE ranked (company TEXT, rank INTEGER, universe TEXT, doc_id TEXT);
CREATE INDEX i_rel_type ON rel(rtype);
CREATE INDEX i_rel_from ON rel(from_e);
CREATE INDEX i_rel_to   ON rel(to_e);
"""

# ---------------------------------------------------------------------------
# The queries. Deliberately written to mirror the Cypher one-for-one so the
# comparison is about the engine and the language, not about a cleverer idea.
# ---------------------------------------------------------------------------

SUPPLIER = "Kestrel Memory Works"

Q = {}

Q["S1_explicit_supply"] = """
SELECT r.to_e AS producer, COUNT(DISTINCT r.doc_id) AS docs,
       GROUP_CONCAT(DISTINCT r.doc_id) AS evidence
FROM rel r JOIN entity e ON e.name = r.to_e
WHERE r.rtype='SUPPLIES' AND r.from_e='Kestrel Memory Works'
  AND e.kind='GPUProducer'
GROUP BY r.to_e ORDER BY producer;
"""

Q["S2_part_chain"] = """
-- two hops: producer uses a part the supplier makes
SELECT u.from_e AS producer, u.to_e AS part,
       GROUP_CONCAT(DISTINCT u.doc_id) AS producer_side_docs
FROM rel u
JOIN rel m ON m.rtype='MAKES_PART' AND m.to_e = u.to_e
          AND m.from_e='Kestrel Memory Works'
JOIN entity e ON e.name = u.from_e
WHERE u.rtype='USES_PART' AND e.kind='GPUProducer'
GROUP BY u.from_e, u.to_e ORDER BY producer;
"""

Q["S3_qualification_chain"] = """
-- three hops: supplier's part qualified on a platform the producer makes
SELECT g.from_e AS producer, q.to_e AS platform, q.doc_id AS qual_doc,
       q.customer_named AS customer_was_named, g.doc_id AS platform_doc
FROM rel q
JOIN rel g ON g.rtype='MAKES_PLATFORM' AND g.to_e = q.to_e
JOIN entity e ON e.name = g.from_e
WHERE q.rtype='QUALIFIED_ON' AND e.kind='GPUProducer'
  AND (q.from_e='Kestrel Memory Works'
       OR q.from_e IN (SELECT to_e FROM rel
                       WHERE rtype='MAKES_PART'
                         AND from_e='Kestrel Memory Works'))
ORDER BY producer;
"""

Q["S4_decoy_exposure"] = """
SELECT other, rtype AS signal, COUNT(DISTINCT doc_id) AS docs,
       GROUP_CONCAT(DISTINCT doc_id) AS evidence
FROM (
  SELECT to_e AS other, rtype, doc_id FROM rel
   WHERE from_e='Kestrel Memory Works'
     AND rtype IN ('LITIGATES_AGAINST','CO_MEMBER','PARTNERS_NON_SUPPLY')
  UNION ALL
  SELECT from_e AS other, rtype, doc_id FROM rel
   WHERE to_e='Kestrel Memory Works'
     AND rtype IN ('LITIGATES_AGAINST','CO_MEMBER','PARTNERS_NON_SUPPLY')
)
GROUP BY other, signal ORDER BY docs DESC, other;
"""

# The money query: absence plus counting, in one statement.
Q["S5_absence_ranked"] = """
WITH universe AS (
  SELECT DISTINCT r.company FROM ranked r JOIN entity e ON e.name = r.company
   WHERE r.universe LIKE '%gpu%' AND r.rank <= 5 AND e.kind = 'GPUProducer'
),
w_explicit AS (
  SELECT to_e AS producer, COUNT(DISTINCT doc_id) * 1.0 AS w FROM rel
   WHERE rtype='SUPPLIES' AND from_e='Kestrel Memory Works' GROUP BY to_e
),
w_part AS (
  SELECT u.from_e AS producer, COUNT(DISTINCT u.doc_id) * 0.6 AS w
    FROM rel u JOIN rel m ON m.rtype='MAKES_PART' AND m.to_e=u.to_e
                         AND m.from_e='Kestrel Memory Works'
   WHERE u.rtype='USES_PART' GROUP BY u.from_e
),
w_qual AS (
  SELECT g.from_e AS producer, COUNT(DISTINCT q.doc_id) * 0.5 AS w
    FROM rel q JOIN rel g ON g.rtype='MAKES_PLATFORM' AND g.to_e=q.to_e
   WHERE q.rtype='QUALIFIED_ON'
     AND (q.from_e='Kestrel Memory Works'
          OR q.from_e IN (SELECT to_e FROM rel WHERE rtype='MAKES_PART'
                           AND from_e='Kestrel Memory Works'))
   GROUP BY g.from_e
),
w_weak AS (
  SELECT other AS producer,
         SUM(CASE WHEN rtype='PROGRAMME_REF' THEN 0.4 ELSE 0.25 END) AS w
    FROM (
      SELECT DISTINCT to_e AS other, rtype, doc_id FROM rel
        WHERE from_e='Kestrel Memory Works'
          AND rtype IN ('PROGRAMME_REF','CO_PRESENTS')
      UNION
      SELECT DISTINCT from_e AS other, rtype, doc_id FROM rel
        WHERE to_e='Kestrel Memory Works'
          AND rtype IN ('PROGRAMME_REF','CO_PRESENTS')
    ) GROUP BY other
),
nonsupply AS (
  SELECT other AS producer, COUNT(DISTINCT doc_id) AS c FROM (
      SELECT to_e AS other, doc_id FROM rel
        WHERE from_e='Kestrel Memory Works'
          AND rtype IN ('LITIGATES_AGAINST','CO_MEMBER','PARTNERS_NON_SUPPLY')
      UNION ALL
      SELECT from_e AS other, doc_id FROM rel
        WHERE to_e='Kestrel Memory Works'
          AND rtype IN ('LITIGATES_AGAINST','CO_MEMBER','PARTNERS_NON_SUPPLY')
  ) GROUP BY other
)
SELECT u.company AS producer,
       ROUND(COALESCE(e.w,0)+COALESCE(p.w,0)+COALESCE(q.w,0)+COALESCE(k.w,0), 3)
         AS supply_evidence,
       COALESCE(n.c,0) AS non_supply_comentions,
       CASE WHEN COALESCE(e.w,0)+COALESCE(p.w,0)+COALESCE(q.w,0)+COALESCE(k.w,0) = 0
            THEN 1 ELSE 0 END AS no_supply_evidence_at_all
FROM universe u
LEFT JOIN w_explicit e ON e.producer=u.company
LEFT JOIN w_part     p ON p.producer=u.company
LEFT JOIN w_qual     q ON q.producer=u.company
LEFT JOIN w_weak     k ON k.producer=u.company
LEFT JOIN nonsupply  n ON n.producer=u.company
ORDER BY supply_evidence ASC, producer;
"""

Q["S7_absence_only_answer"] = """
-- Elimination WITHOUT the cardinality claim. Mirrors Cypher Q11.
WITH universe AS (
  SELECT DISTINCT r.company FROM ranked r JOIN entity e ON e.name = r.company
   WHERE r.universe LIKE '%gpu%' AND r.rank <= 5 AND e.kind = 'GPUProducer'
),
routes AS (
  SELECT u.company AS producer, (
    (SELECT COUNT(DISTINCT doc_id) FROM rel
      WHERE rtype='SUPPLIES' AND from_e='Kestrel Memory Works' AND to_e=u.company)
  + (SELECT COUNT(DISTINCT x.doc_id) FROM rel x
      JOIN rel mm ON mm.rtype='MAKES_PART' AND mm.to_e=x.to_e
                 AND mm.from_e='Kestrel Memory Works'
      WHERE x.rtype='USES_PART' AND x.from_e=u.company)
  + (SELECT COUNT(DISTINCT q.doc_id) FROM rel q
      JOIN rel g ON g.rtype='MAKES_PLATFORM' AND g.to_e=q.to_e
      WHERE q.rtype='QUALIFIED_ON' AND g.from_e=u.company
        AND (q.from_e='Kestrel Memory Works'
             OR q.from_e IN (SELECT to_e FROM rel WHERE rtype='MAKES_PART'
                              AND from_e='Kestrel Memory Works')))
  + (SELECT COUNT(DISTINCT doc_id) FROM rel
      WHERE rtype IN ('PROGRAMME_REF','CO_PRESENTS')
        AND ((from_e='Kestrel Memory Works' AND to_e=u.company)
          OR (to_e='Kestrel Memory Works' AND from_e=u.company)))
  ) AS routes
  FROM universe u
)
SELECT (SELECT COUNT(*) FROM routes WHERE routes > 0) AS n_with_evidence,
       (SELECT COUNT(*) FROM routes WHERE routes = 0) AS n_without,
       (SELECT GROUP_CONCAT(producer, ' | ') FROM routes WHERE routes > 0) AS supplied,
       (SELECT GROUP_CONCAT(producer, ' | ') FROM routes WHERE routes = 0) AS excluded;
"""

Q["S6_elimination"] = """
WITH universe AS (
  SELECT DISTINCT r.company FROM ranked r JOIN entity e ON e.name = r.company
   WHERE r.universe LIKE '%gpu%' AND r.rank <= 5 AND e.kind = 'GPUProducer'),
m AS (SELECT COUNT(*) AS m FROM universe),
cl AS (SELECT c.n AS n FROM claim c, m
        WHERE c.ctype='AGGREGATE_SUPPLY_CLAIM' AND c.company='Kestrel Memory Works'
          AND c.of_m = m.m),
routes AS (
  SELECT u.company AS producer, (
    (SELECT COUNT(DISTINCT doc_id) FROM rel
      WHERE rtype='SUPPLIES' AND from_e='Kestrel Memory Works' AND to_e=u.company)
  + (SELECT COUNT(DISTINCT x.doc_id) FROM rel x
      JOIN rel mm ON mm.rtype='MAKES_PART' AND mm.to_e=x.to_e
                 AND mm.from_e='Kestrel Memory Works'
      WHERE x.rtype='USES_PART' AND x.from_e=u.company)
  + (SELECT COUNT(DISTINCT q.doc_id) FROM rel q
      JOIN rel g ON g.rtype='MAKES_PLATFORM' AND g.to_e=q.to_e
      WHERE q.rtype='QUALIFIED_ON' AND g.from_e=u.company
        AND (q.from_e='Kestrel Memory Works'
             OR q.from_e IN (SELECT to_e FROM rel WHERE rtype='MAKES_PART'
                              AND from_e='Kestrel Memory Works')))
  + (SELECT COUNT(DISTINCT doc_id) FROM rel
      WHERE rtype IN ('PROGRAMME_REF','CO_PRESENTS')
        AND ((from_e='Kestrel Memory Works' AND to_e=u.company)
          OR (to_e='Kestrel Memory Works' AND from_e=u.company)))
  ) AS routes
  FROM universe u
)
SELECT (SELECT n FROM cl LIMIT 1) AS n, (SELECT m FROM m) AS m,
       (SELECT COUNT(*) FROM routes WHERE routes > 0) AS n_with_evidence,
       CASE WHEN (SELECT COUNT(*) FROM routes WHERE routes > 0)
                 = (SELECT n FROM cl LIMIT 1) THEN 1 ELSE 0 END AS claim_satisfied,
       (SELECT GROUP_CONCAT(producer, ' | ') FROM routes WHERE routes > 0) AS supplied,
       (SELECT GROUP_CONCAT(producer, ' | ') FROM routes WHERE routes = 0) AS excluded;
"""


Q["S8_three_state"] = """
-- THE THREE-STATE QUERY, mirroring Cypher Q12. Reports unique / ambiguous /
-- unsatisfiable explicitly rather than returning a row the caller must trust.
WITH universe AS (
  SELECT DISTINCT r.company FROM ranked r JOIN entity e ON e.name = r.company
   WHERE r.universe LIKE '%gpu%' AND r.rank <= 5 AND e.kind = 'GPUProducer'
),
m AS (SELECT COUNT(*) AS m FROM universe),
cl AS (SELECT c.n AS n FROM claim c, m
        WHERE c.ctype='AGGREGATE_SUPPLY_CLAIM' AND c.company='Kestrel Memory Works'
          AND c.of_m = m.m),
routes AS (
  SELECT u.company AS producer, (
    (SELECT COUNT(DISTINCT doc_id) FROM rel
      WHERE rtype='SUPPLIES' AND from_e='Kestrel Memory Works' AND to_e=u.company)
  + (SELECT COUNT(DISTINCT x.doc_id) FROM rel x
      JOIN rel mm ON mm.rtype='MAKES_PART' AND mm.to_e=x.to_e
                 AND mm.from_e='Kestrel Memory Works'
      WHERE x.rtype='USES_PART' AND x.from_e=u.company)
  + (SELECT COUNT(DISTINCT q.doc_id) FROM rel q
      JOIN rel g ON g.rtype='MAKES_PLATFORM' AND g.to_e=q.to_e
      WHERE q.rtype='QUALIFIED_ON' AND g.from_e=u.company
        AND (q.from_e='Kestrel Memory Works'
             OR q.from_e IN (SELECT to_e FROM rel WHERE rtype='MAKES_PART'
                              AND from_e='Kestrel Memory Works')))
  + (SELECT COUNT(DISTINCT doc_id) FROM rel
      WHERE rtype IN ('PROGRAMME_REF','CO_PRESENTS')
        AND ((from_e='Kestrel Memory Works' AND to_e=u.company)
          OR (to_e='Kestrel Memory Works' AND from_e=u.company)))
  ) AS routes
  FROM universe u
),
agg AS (
  SELECT (SELECT n FROM cl LIMIT 1) AS n,
         (SELECT m FROM m) AS m,
         (SELECT COUNT(*) FROM routes WHERE routes > 0) AS n_ev,
         (SELECT COUNT(*) FROM routes WHERE routes = 0) AS n_zero,
         (SELECT GROUP_CONCAT(producer, ' | ') FROM routes WHERE routes > 0) AS E,
         (SELECT GROUP_CONCAT(producer, ' | ') FROM routes WHERE routes = 0) AS Z
)
SELECT CASE
         WHEN n IS NULL        THEN 'NO_CLAIM'
         WHEN n_ev > n         THEN 'UNSATISFIABLE'
         WHEN n_zero = m - n   THEN 'UNIQUE'
         WHEN n_zero > m - n   THEN 'AMBIGUOUS'
         ELSE 'UNSATISFIABLE'
       END AS state,
       n, m, CASE WHEN n IS NULL THEN NULL ELSE m - n END AS n_excluded,
       n_ev AS n_with_evidence, n_zero AS n_zero_evidence,
       E AS supplied_certain,
       CASE WHEN n IS NOT NULL AND n_zero = m - n THEN Z END AS excluded_certain,
       CASE WHEN n IS NOT NULL AND n_zero > m - n THEN Z END AS ambiguous_pool
FROM agg;
"""


def populate(cur, nodes, edges, claims):
    for n in nodes:
        cur.execute("INSERT INTO node VALUES (?,?)", (n["label"], n["name"]))
        if n["label"] == "Company":
            cur.execute("INSERT OR REPLACE INTO entity VALUES (?,?)",
                        (n["name"], n.get("kind", "UnknownCompany")))
    for e in edges:
        cur.execute("INSERT INTO rel VALUES (?,?,?,?,?,?,?,?,?,?)",
                    (e["type"], e["from_kind"], e["from"], e["to_kind"], e["to"],
                     e["stance"], e["doc_id"], e.get("doc_type", ""),
                     1 if e.get("customer_named", True) else 0, e.get("quote", "")))
    for c in claims:
        p = c["props"] or {}
        if c["type"] == "AGGREGATE_SUPPLY_CLAIM":
            try:
                cur.execute("INSERT INTO claim VALUES (?,?,?,?,?,?,?)",
                            (c["type"], c["company"], int(p["n"]), int(p["of_m"]),
                             str(p.get("customer_class", "")), c["doc_id"], c["quote"]))
            except Exception:
                pass
        elif c["type"] == "RANKED_TOP":
            try:
                r = int(p["rank"])
            except Exception:
                continue
            if r >= 1:
                cur.execute("INSERT INTO ranked VALUES (?,?,?,?)",
                            (c["company"], r,
                             str(p.get("universe") or "unspecified").lower(),
                             c["doc_id"]))


def run_duckdb(nodes, edges, claims, outdir):
    """Same tables, same SQL, DuckDB CLI. Returns (ok, {name: text})."""
    sqlpath = os.path.join(outdir, "duckdb_script.sql")
    lines = [DDL.replace("CREATE INDEX i_rel_type ON rel(rtype);", "")
             .replace("CREATE INDEX i_rel_from ON rel(from_e);", "")
             .replace("CREATE INDEX i_rel_to   ON rel(to_e);", "")]

    def lit(v):
        if v is None:
            return "NULL"
        if isinstance(v, int):
            return str(v)
        return "'" + str(v).replace("'", "''") + "'"

    for n in nodes:
        lines.append("INSERT INTO node VALUES (%s,%s);" % (lit(n["label"]), lit(n["name"])))
        if n["label"] == "Company":
            lines.append("INSERT INTO entity VALUES (%s,%s) ON CONFLICT DO NOTHING;"
                         % (lit(n["name"]), lit(n.get("kind", "UnknownCompany"))))
    for e in edges:
        lines.append("INSERT INTO rel VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s);" % tuple(
            lit(x) for x in (e["type"], e["from_kind"], e["from"], e["to_kind"], e["to"],
                             e["stance"], e["doc_id"], e.get("doc_type", ""),
                             1 if e.get("customer_named", True) else 0,
                             e.get("quote", ""))))
    for c in claims:
        p = c["props"] or {}
        if c["type"] == "AGGREGATE_SUPPLY_CLAIM":
            try:
                lines.append("INSERT INTO claim VALUES (%s,%s,%d,%d,%s,%s,%s);"
                             % (lit(c["type"]), lit(c["company"]), int(p["n"]),
                                int(p["of_m"]), lit(str(p.get("customer_class", ""))),
                                lit(c["doc_id"]), lit(c["quote"])))
            except Exception:
                pass
        elif c["type"] == "RANKED_TOP":
            try:
                r = int(p["rank"])
            except Exception:
                continue
            if r >= 1:
                lines.append("INSERT INTO ranked VALUES (%s,%d,%s,%s);"
                             % (lit(c["company"]), r,
                                lit(str(p.get("universe") or "unspecified").lower()),
                                lit(c["doc_id"])))

    out = {}
    for name in ["S1_explicit_supply", "S5_absence_ranked", "S6_elimination",
                 "S8_three_state"]:
        sql = Q[name]
        # DuckDB spells these two differently from SQLite.
        sql = sql.replace("GROUP_CONCAT(DISTINCT r.doc_id)", "string_agg(DISTINCT r.doc_id, ',')")
        sql = sql.replace("GROUP_CONCAT(producer, ' | ')", "string_agg(producer, ' | ')")
        sql = sql.replace("GROUP_CONCAT(DISTINCT doc_id)", "string_agg(DISTINCT doc_id, ',')")
        lines.append(".print === %s ===" % name)
        lines.append(sql)
    with open(sqlpath, "w") as f:
        f.write("\n".join(lines))
    p = subprocess.run(["duckdb", "-box", "-init", "/dev/null"],
                       stdin=open(sqlpath), capture_output=True, text=True, timeout=300)
    return p.returncode, p.stdout, p.stderr, sqlpath


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--relations", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--tag", default="main")
    ap.add_argument("--repair-direction", action="store_true")
    ap.add_argument("--corpus", default=None)
    a = ap.parse_args()

    outdir = os.path.dirname(os.path.abspath(a.out))
    os.makedirs(outdir, exist_ok=True)

    rels = json.load(open(a.relations))["relations"]
    import common as _c
    docs = _c.load_corpus(a.corpus) if a.corpus else None
    nodes, edges, claims = graphbuild.build(
        rels, repair_direction=a.repair_direction, docs=docs)

    # ------------------------- SQLite -------------------------
    con = sqlite3.connect(":memory:")
    cur = con.cursor()
    t0 = time.time()
    cur.executescript(DDL)
    populate(cur, nodes, edges, claims)
    con.commit()
    t_load = time.time() - t0

    results = {}
    for name, sql in Q.items():
        t0 = time.time()
        try:
            cur.execute(sql)
            cols = [d[0] for d in cur.description]
            rows = cur.fetchall()
            txt = " | ".join(cols) + "\n" + "\n".join(
                " | ".join("" if v is None else str(v) for v in r) for r in rows)
            err = None
        except Exception as e:
            txt, cols, rows, err = "", [], [], str(e)
        results[name] = {"sql": sql.strip(), "seconds": round(time.time() - t0, 4),
                         "columns": cols, "rows": rows, "text": txt, "error": err}
        print("\n----- sqlite %s (%.4fs) -----" % (name, results[name]["seconds"]))
        print(txt or ("ERROR: " + str(err)))

    # ------------------------- DuckDB -------------------------
    rc, dout, derr, sqlpath = run_duckdb(nodes, edges, claims, outdir)
    print("\n===== duckdb (rc=%d, script=%s) =====" % (rc, sqlpath))
    print(dout[:4000] or derr[:2000])

    with open(a.out, "w") as f:
        json.dump({
            "tag": a.tag,
            "repair_direction": a.repair_direction,
            "provenance_check": bool(a.corpus),
            "n_provenance_dropped": getattr(graphbuild.build, "n_provenance_dropped", 0),
            "n_direction_flipped": getattr(graphbuild.build, "n_direction_flipped", 0),
            "n_nodes": len(nodes), "n_edges": len(edges), "n_claims": len(claims),
            "sqlite_load_seconds": round(t_load, 4),
            "sqlite": results,
            "duckdb": {"rc": rc, "stdout": dout, "stderr": derr[-2000:],
                       "script": sqlpath},
        }, f, indent=1)
    print("\nwrote", a.out)


if __name__ == "__main__":
    main()
