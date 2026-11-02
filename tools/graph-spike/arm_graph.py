#!/usr/bin/env python3
"""Graph arm: load the extracted relations into Neo4j, then query in Cypher.

Talks to Neo4j through cypher-shell, so every query that produced a number in
the notes is the literal text of cypher/queries.cypher and can be rerun by hand.

Usage:
  python3 arm_graph.py --relations out/relations.json --out out/graph.json \
      --db neo4j --tag main
"""

import argparse
import json
import os
import re
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common      # noqa: E402
import graphbuild  # noqa: E402

SHELL = os.environ.get("CYPHER_SHELL", "/opt/homebrew/bin/cypher-shell")
USER = os.environ.get("NEO4J_USER", "neo4j")
PW = os.environ.get("NEO4J_PASSWORD", "graphspike123")
URI = os.environ.get("NEO4J_URI", "neo4j://localhost:7687")


def run_cypher(stmt, db="neo4j", fmt="plain"):
    env = dict(os.environ, JAVA_HOME="/opt/homebrew/opt/openjdk@21")
    p = subprocess.run(
        [SHELL, "-a", URI, "-u", USER, "-p", PW, "-d", db, "--format", fmt,
         "--fail-at-end"],
        input=stmt, capture_output=True, text=True, env=env, timeout=600)
    return p.returncode, p.stdout, p.stderr


def q(s):
    return "'" + str(s).replace("\\", "\\\\").replace("'", "\\'") + "'"


def load(nodes, edges, claims, db):
    """Build the property graph. Returns (seconds, statement_text)."""
    stmts = ["MATCH (n) DETACH DELETE n;"]

    for n in nodes:
        if n["label"] == "Company":
            stmts.append("MERGE (c:Company {name:%s}) SET c.kind=%s;"
                         % (q(n["name"]), q(n.get("kind", "UnknownCompany"))))
        else:
            stmts.append("MERGE (x:%s {name:%s});" % (n["label"], q(n["name"])))

    for e in edges:
        stmts.append(
            "MATCH (a:%s {name:%s}), (b:%s {name:%s}) "
            "MERGE (a)-[r:%s {doc_id:%s}]->(b) "
            "SET r.stance=%s, r.quote=%s, r.doc_type=%s, r.customer_named=%s;"
            % (e["from_kind"], q(e["from"]), e["to_kind"], q(e["to"]), e["type"],
               q(e["doc_id"]), q(e["stance"]), q(e["quote"]), q(e["doc_type"]),
               "true" if e.get("customer_named", True) else "false"))

    for c in claims:
        p = c["props"] or {}
        if c["type"] == "AGGREGATE_SUPPLY_CLAIM":
            try:
                n_ = int(p.get("n"))
                m_ = int(p.get("of_m"))
            except Exception:
                continue
            stmts.append(
                "MATCH (a:Company {name:%s}) "
                "MERGE (k:Claim {doc_id:%s, n:%d, of_m:%d}) "
                "SET k.customer_class=%s, k.quote=%s "
                "MERGE (a)-[:MAKES_CLAIM]->(k);"
                % (q(c["company"]), q(c["doc_id"]), n_, m_,
                   q(p.get("customer_class", "")), q(c["quote"])))
        elif c["type"] == "RANKED_TOP":
            try:
                rank = int(p.get("rank"))
            except Exception:
                continue
            # Key the Universe by the ranking's OWN universe string. Lumping
            # every RANKED_TOP into one 'top5_gpu' node was wrong: the corpus
            # also contains faithful "ranked first at the industry awards"
            # rankings, which polluted the universe and broke the cardinality
            # check. The query picks the universe it wants.
            if rank < 1:
                continue
            uni = str(p.get("universe") or "unspecified").lower()
            uni = re.sub(r"[^a-z0-9]+", "_", uni).strip("_")[:60] or "unspecified"
            stmts.append(
                "MATCH (a:Company {name:%s}) "
                "MERGE (u:Universe {name:%s}) "
                "MERGE (a)-[r:RANKED_IN]->(u) SET r.rank=%d, r.doc_id=%s;"
                % (q(c["company"]), q(uni), rank, q(c["doc_id"])))

    text = "\n".join(stmts)
    t0 = time.time()
    rc, out, err = run_cypher(text, db=db)
    dt = time.time() - t0
    if rc != 0:
        print("LOAD FAILED rc=%d\n%s" % (rc, err[-3000:]))
    return dt, text, rc, err


def split_queries(path):
    blocks, name, buf = [], None, []
    for line in open(path):
        if line.startswith("// @"):
            if name:
                blocks.append((name, "".join(buf)))
            name, buf = line[4:].strip(), []
        elif name:
            buf.append(line)
    if name:
        blocks.append((name, "".join(buf)))
    return blocks


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--relations", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--queries", default=None)
    ap.add_argument("--db", default="neo4j")
    ap.add_argument("--tag", default="main")
    ap.add_argument("--repair-direction", action="store_true",
                    help="re-orient SUPPLIES edges the extractor inverted")
    ap.add_argument("--corpus", default=None,
                    help="corpus dir; enables the provenance check")
    a = ap.parse_args()

    here = os.path.dirname(os.path.abspath(__file__))
    qpath = a.queries or os.path.join(here, "cypher", "queries.cypher")

    rels = json.load(open(a.relations))["relations"]
    docs = common.load_corpus(a.corpus) if a.corpus else None
    nodes, edges, claims = graphbuild.build(
        rels, repair_direction=a.repair_direction, docs=docs)
    inverted = graphbuild.count_inverted_supply(edges)
    print("nodes=%d edges=%d claims=%d (from %d extracted relations)"
          % (len(nodes), len(edges), len(claims), len(rels)))
    print("direction: repair=%s flipped=%d, still-inverted SUPPLIES edges=%d"
          % (a.repair_direction, getattr(graphbuild.build, "n_direction_flipped", 0),
             len(inverted)))

    t_load, load_text, rc, err = load(nodes, edges, claims, a.db)
    print("load: %.2fs rc=%d" % (t_load, rc))
    with open(os.path.join(os.path.dirname(a.out),
                           "graph_load_%s.cypher" % a.tag), "w") as f:
        f.write(load_text)

    results = {}
    for name, stmt in split_queries(qpath):
        t0 = time.time()
        rc, out, err = run_cypher(stmt, db=a.db)
        results[name] = {
            "cypher": stmt.strip(),
            "seconds": round(time.time() - t0, 3),
            "rc": rc,
            "stdout": out.strip(),
            "stderr": err.strip()[-1500:],
        }
        print("\n----- %s (%.3fs, rc=%d) -----" % (name, results[name]["seconds"], rc))
        print(out.strip()[:2500] or err.strip()[:1200])

    payload = {
        "tag": a.tag,
        "relations_file": a.relations,
        "n_extracted_relations": len(rels),
        "n_nodes": len(nodes), "n_edges": len(edges), "n_claims": len(claims),
        "load_seconds": round(t_load, 2),
        "repair_direction": a.repair_direction,
        "provenance_check": bool(a.corpus),
        "n_provenance_dropped": getattr(graphbuild.build, "n_provenance_dropped", 0),
        "provenance_dropped": getattr(graphbuild.build, "provenance_dropped", []),
        "n_direction_flipped": getattr(graphbuild.build, "n_direction_flipped", 0),
        "inverted_supply_edges_remaining": inverted,
        "queries": results,
        "nodes": nodes, "edges": edges, "claims": claims,
    }
    with open(a.out, "w") as f:
        json.dump(payload, f, indent=1)
    print("\nwrote", a.out)


if __name__ == "__main__":
    main()
