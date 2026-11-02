#!/usr/bin/env python3
"""Score every arm against truth/ground_truth.json. The only file that reads truth.

Usage:
  python3 score.py --truth truth/ground_truth.json \
      --graph out/graph.json --sql out/sql.json --retrieval out/retrieval.json \
      --out out/scores.json
"""

import argparse
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common  # noqa: E402


def canon_set(xs):
    out = set()
    for x in xs or []:
        c = common.resolve(x)
        if c:
            out.add(c)
    return out


def prf(pred, gold):
    pred, gold = set(pred), set(gold)
    tp = len(pred & gold)
    p = tp / len(pred) if pred else 0.0
    r = tp / len(gold) if gold else 0.0
    return {
        "tp": tp, "fp": sorted(pred - gold), "fn": sorted(gold - pred),
        "precision": round(p, 3), "recall": round(r, 3),
        "f1": round(2 * p * r / (p + r), 3) if (p + r) else 0.0,
    }


def judge(name, supplied, excluded, gold_sup, gold_exc, expressible=True, note=None):
    s = canon_set(supplied) if supplied is not None else None
    e = common.resolve(excluded) if excluded else None
    rec = {
        "arm": name,
        "can_express_absence": expressible,
        "predicted_supplied": sorted(s) if s is not None else None,
        "predicted_excluded": e,
        "exact_set_match": (s == set(gold_sup)) if s is not None else False,
        "excluded_correct": (e == gold_exc),
        "decoy_false_positive": bool(s and gold_exc in s) or (e is not None and e != gold_exc),
    }
    rec["metrics"] = prf(s, gold_sup) if s is not None else None
    if note:
        rec["note"] = note
    return rec


# ---------------- parse cypher-shell / sqlite text output ------------------

def parse_plain_table(text):
    lines = [l for l in (text or "").strip().splitlines() if l.strip()]
    if not lines:
        return [], []
    cols = [c.strip().strip('"') for c in lines[0].split(",")]
    rows = []
    for l in lines[1:]:
        # cypher-shell plain format is CSV-ish; lists contain commas inside []
        parts, buf, depth, instr = [], "", 0, False
        for ch in l:
            if ch == '"':
                instr = not instr
            if ch == "[":
                depth += 1
            elif ch == "]":
                depth -= 1
            if ch == "," and depth == 0 and not instr:
                parts.append(buf)
                buf = ""
            else:
                buf += ch
        parts.append(buf)
        rows.append([p.strip().strip('"') for p in parts])
    return cols, rows


def listify(cell):
    """'[\"A\", \"B\"]' -> ['A','B'];  'A | B' -> ['A','B']"""
    if cell is None:
        return []
    c = str(cell).strip()
    if c in ("", "[]", "NULL", "None"):
        return []
    if c.startswith("["):
        try:
            return json.loads(c.replace('""', '"'))
        except Exception:
            return [x.strip().strip('"') for x in c.strip("[]").split(",") if x.strip()]
    if "|" in c:
        return [x.strip() for x in c.split("|") if x.strip()]
    if "," in c:
        return [x.strip() for x in c.split(",") if x.strip()]
    return [c]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--truth", required=True)
    ap.add_argument("--graph", action="append", default=[], help="tag=path")
    ap.add_argument("--sql", action="append", default=[], help="tag=path")
    ap.add_argument("--retrieval", default=None)
    ap.add_argument("--out", required=True)
    ap.add_argument("--label", default="primary")
    a = ap.parse_args()

    T = json.load(open(a.truth))
    gold_sup, gold_exc = T["supplied"], T["excluded"]
    arms = []

    # ------------------------------- graph -------------------------------
    # The claim-gated query (Q9) is preferred when it returns anything, because
    # it both answers and verifies against the stated count. When extraction
    # lost the claim, Q9 is empty and the claim-free query (Q11) is the arm's
    # answer. Which one was used is recorded, never silently substituted.
    for spec in (a.graph or []):
        tag, path = spec.split("=", 1) if "=" in spec else ("graph", spec)
        if not os.path.exists(path):
            continue
        G = json.load(open(path))
        qs = G["queries"]
        used, sup, exc, extra = None, None, None, {}
        for qname in ("Q9_elimination", "Q11_absence_only_answer"):
            cols, rows = parse_plain_table(qs.get(qname, {}).get("stdout", ""))
            if not rows:
                continue
            d = dict(zip(cols, rows[0]))
            sup = listify(d.get("supplied"))
            exc_l = listify(d.get("excluded"))
            exc = exc_l[0] if len(exc_l) == 1 else None
            extra = {k: d.get(k) for k in
                     ("n", "m", "n_with_evidence", "n_without", "claim_satisfied")}
            extra["raw_excluded"] = exc_l
            used = qname
            break
        r = judge("graph_neo4j[%s]" % tag, sup, exc, gold_sup, gold_exc,
                  note="query=%s; %s; Q9_empty=%s" % (
                      used, json.dumps(extra),
                      not parse_plain_table(
                          qs.get("Q9_elimination", {}).get("stdout", ""))[1]))
        r["query_used"] = used
        r["ranking_raw"] = qs.get("Q8_absence_ranked", {}).get("stdout", "")
        r["decoy_query_raw"] = qs.get("Q7_decoy_exposure", {}).get("stdout", "")
        r["naive_cooccurrence_raw"] = qs.get("Q10_naive_cooccurrence", {}).get("stdout", "")
        r["cost"] = {"load_seconds": G.get("load_seconds"),
                     "query_seconds": {k: v["seconds"] for k, v in qs.items()},
                     "n_nodes": G.get("n_nodes"), "n_edges": G.get("n_edges"),
                     "provenance_dropped": G.get("n_provenance_dropped"),
                     "direction_flipped": G.get("n_direction_flipped")}
        arms.append(r)

    # -------------------------------- sql --------------------------------
    for spec in (a.sql or []):
        tag, path = spec.split("=", 1) if "=" in spec else ("sql", spec)
        if not os.path.exists(path):
            continue
        S = json.load(open(path))
        used, sup, exc, extra = None, None, None, {}
        for qname in ("S6_elimination", "S7_absence_only_answer"):
            el = S["sqlite"].get(qname, {})
            if not el.get("rows"):
                continue
            d = dict(zip(el["columns"], el["rows"][0]))
            # S6 fails open in SQL where Cypher fails closed: a missing claim
            # gives n = NULL rather than no row. Treat that as unusable.
            if qname == "S6_elimination" and d.get("n") in (None, ""):
                continue
            sup = listify(d.get("supplied"))
            exc_l = listify(d.get("excluded"))
            exc = exc_l[0] if len(exc_l) == 1 else None
            extra = {k: d.get(k) for k in
                     ("n", "m", "n_with_evidence", "n_without", "claim_satisfied")}
            used = qname
            break
        r = judge("sql_sqlite[%s]" % tag, sup, exc, gold_sup, gold_exc,
                  note="query=%s; %s" % (used, json.dumps(extra)))
        r["query_used"] = used
        r["ranking_raw"] = S["sqlite"].get("S5_absence_ranked", {}).get("text", "")
        r["cost"] = {"load_seconds": S.get("sqlite_load_seconds"),
                     "query_seconds": {k: v["seconds"] for k, v in S["sqlite"].items()}}
        r["duckdb_stdout"] = S.get("duckdb", {}).get("stdout", "")[:4000]
        arms.append(r)

    # ----------------------------- retrieval -----------------------------
    if a.retrieval and os.path.exists(a.retrieval):
        R = json.load(open(a.retrieval))
        decoys = set(T["decoys"])
        for rep in R["reps"]:
            for key, expressible, note in [
                ("B1", False,
                 "single vague query; a passage retriever has no way to represent "
                 "'which entity has no evidence', so any correct answer here is the "
                 "reader guessing from what was retrieved"),
                ("B2", False,
                 "per-pair retrieval, verdicts taken at face value; no cardinality"),
                ("B3", True,
                 "per-pair retrieval + the 4-of-5 constraint applied in Python"),
            ]:
                d = rep.get(key) or {}
                r = judge("retrieval_%s_rep%d" % (key, rep["rep"]),
                          d.get("supplied"), d.get("excluded"),
                          gold_sup, gold_exc, expressible=expressible,
                          note=note + ("; " + d["note"] if d.get("note") else ""))
                got = set(d.get("retrieved_docs") or [])
                for v in (d.get("pairs") or {}).values():
                    got |= set(v.get("retrieved_docs") or [])
                r["decoy_docs_retrieved"] = sorted(got & decoys)
                if key == "B2":
                    r["per_pair"] = {
                        k: {"verdict": v["verdict"],
                            "evidence_strength": v["evidence_strength"],
                            "basis": v.get("basis")}
                        for k, v in (d.get("pairs") or {}).items()}
                if key == "B3":
                    r["ranking"] = d.get("ranking")
                r["cost"] = {"seconds": d.get("seconds")}
                arms.append(r)
        arms.append({"arm": "retrieval_index_cost",
                     "cost": {"index_seconds": R.get("index_seconds"),
                              "n_chunks": R.get("n_chunks"),
                              "embed_model": R.get("embed_model")}})

    out = {"label": a.label, "truth": {"supplied": gold_sup, "excluded": gold_exc},
           "arms": arms}
    with open(a.out, "w") as f:
        json.dump(out, f, indent=1)

    print("=" * 78)
    print("TRUTH  supplied = %s" % ", ".join(gold_sup))
    print("TRUTH  excluded = %s" % gold_exc)
    print("=" * 78)
    hdr = "%-30s %-8s %-9s %-6s %-6s %-6s %s"
    print(hdr % ("arm", "exact", "excl-ok", "prec", "rec", "decoy", "expressible"))
    for r in arms:
        if "exact_set_match" not in r:
            continue
        m = r.get("metrics") or {}
        print(hdr % (r["arm"][:30],
                     "YES" if r["exact_set_match"] else "no",
                     "YES" if r["excluded_correct"] else "no",
                     m.get("precision", "-"), m.get("recall", "-"),
                     "FP" if r["decoy_false_positive"] else "-",
                     "yes" if r["can_express_absence"] else "NO"))
    print("\nwrote", a.out)


if __name__ == "__main__":
    main()
