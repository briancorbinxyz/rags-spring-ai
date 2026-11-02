#!/usr/bin/env python3
"""Score the follow-up variants on the three-state outcome.

Exact-set-match is the wrong metric when the corpus genuinely cannot single out
one producer. What matters here is whether each arm reports the RIGHT STATE and,
when ambiguous, the right candidate pool -- and in particular whether it says
"ambiguous" rather than silently returning a row.

Only this script and score.py read truth.

Usage:
  python3 score_variants.py --out out/scores_variants.json
"""

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common  # noqa: E402

VARIANTS = ["ambig", "unique", "unsat"]


def cypher_row(path, qname):
    d = json.load(open(path))["queries"][qname]
    lines = [l for l in (d["stdout"] or "").strip().splitlines() if l.strip()]
    if len(lines) < 2:
        return None, d["seconds"]
    cols = [c.strip().strip('"') for c in lines[0].split(",")]
    # split the data row respecting [..] lists
    parts, buf, depth = [], "", 0
    for ch in lines[1]:
        if ch == "[":
            depth += 1
        elif ch == "]":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append(buf)
            buf = ""
        else:
            buf += ch
    parts.append(buf)
    return dict(zip(cols, [p.strip() for p in parts])), d["seconds"]


def as_set(cell):
    if cell is None:
        return None
    c = str(cell).strip()
    if c in ("", "NULL", "None", "[]"):
        return None
    if c.startswith("["):
        try:
            return {common.resolve(x) or x for x in json.loads(c)}
        except Exception:
            return {common.resolve(x.strip().strip('"')) or x.strip().strip('"')
                    for x in c.strip("[]").split(",") if x.strip()}
    return {common.resolve(x.strip()) or x.strip() for x in c.split("|") if x.strip()}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    a = ap.parse_args()

    rows = []
    for v in VARIANTS:
        T = json.load(open("truth_v_%s/ground_truth.json" % v))
        R = T["evidence_resolvability"]
        for c in ("claim", "noclaim"):
            expected = R["expected_state"].upper() if c == "claim" else "NO_CLAIM"
            exp_excl = set(R["expected_excluded_set"] or []) if c == "claim" else None
            exp_pool = set(R["expected_ambiguous_pool"] or []) if c == "claim" else None

            g, g_s = cypher_row("out/graph_v_%s_%s.json" % (v, c), "Q12_three_state")
            sp = json.load(open("out/sql_v_%s_%s.json" % (v, c)))["sqlite"]["S8_three_state"]
            s_row = dict(zip(sp["columns"], sp["rows"][0])) if sp.get("rows") else None

            rec = {
                "variant": v, "claim": c, "expected_state": expected,
                "cypher_state": (g or {}).get("state", "").strip('"') if g else None,
                "sqlite_state": (s_row or {}).get("state"),
                "cypher_seconds": g_s,
                "sqlite_seconds": sp.get("seconds"),
                "n_with_evidence": (g or {}).get("n_with_evidence"),
                "n_zero_evidence": (g or {}).get("n_zero_evidence"),
                "expected_excluded_set": sorted(exp_excl) if exp_excl else None,
                "expected_ambiguous_pool": sorted(exp_pool) if exp_pool else None,
                "cypher_excluded_certain": sorted(as_set((g or {}).get("excluded_certain")) or []) or None,
                "cypher_ambiguous_pool": sorted(as_set((g or {}).get("ambiguous_pool")) or []) or None,
            }
            rec["state_correct"] = (rec["cypher_state"] == expected)
            rec["engines_agree"] = (rec["cypher_state"] == rec["sqlite_state"])
            got_excl = as_set((g or {}).get("excluded_certain"))
            got_pool = as_set((g or {}).get("ambiguous_pool"))
            if expected == "UNIQUE":
                rec["set_correct"] = (got_excl == exp_excl)
            elif expected == "AMBIGUOUS":
                rec["set_correct"] = (got_pool == exp_pool)
                rec["truth_inside_pool"] = bool(
                    got_pool and T["excluded"] in got_pool)
            else:
                rec["set_correct"] = (got_excl is None and got_pool is None)
            rows.append(rec)

    with open(a.out, "w") as f:
        json.dump({"label": "followup_cardinality_variants", "rows": rows}, f, indent=2)

    print("=" * 96)
    print("FOLLOW-UP: variants where cardinality is necessary, not merely available")
    print("=" * 96)
    hdr = "%-10s %-8s %-15s %-15s %-7s %-7s %-6s %-6s %s"
    print(hdr % ("variant", "claim", "expected", "got(cypher)", "state",
                 "set", "agree", "|E|", "|Z|"))
    for r in rows:
        print(hdr % (r["variant"], r["claim"], r["expected_state"],
                     r["cypher_state"],
                     "YES" if r["state_correct"] else "no",
                     "YES" if r["set_correct"] else "no",
                     "YES" if r["engines_agree"] else "NO",
                     r["n_with_evidence"], r["n_zero_evidence"]))
    print()
    for r in rows:
        if r["expected_state"] == "AMBIGUOUS" and r["claim"] == "claim":
            print("ambiguous pool reported : %s" % r["cypher_ambiguous_pool"])
            print("true excluded inside it : %s" % r.get("truth_inside_pool"))
    print("\ncost: cypher Q12 %.3fs vs sqlite S8 %.4fs"
          % (rows[0]["cypher_seconds"], rows[0]["sqlite_seconds"]))
    print("wrote", a.out)


if __name__ == "__main__":
    main()
