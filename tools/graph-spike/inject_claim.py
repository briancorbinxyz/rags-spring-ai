#!/usr/bin/env python3
"""ORACLE variant: inject the aggregate claim the extractor failed to capture.

This is not a result on its own. It exists to separate two things that the
primary run confounds:

  * whether the graph can ANSWER the question (it can, via absence, Q11), and
  * whether the graph can VERIFY the answer against the stated count (Q9),
    which needs the "four of five" claim as a node.

In the primary run the local 8B extractor never produced that claim: 0/5 reps on
the claim document, each time emitting three hallucinated named SUPPLIES edges
instead (see out/claim_stability.txt). So Q9 returns nothing there. The exact
claim injected here is the one the SAME prompt produced on an earlier isolated
run of the same document, so it is a recoverable extraction target rather than
an invention:

  {"type": "AGGREGATE_SUPPLY_CLAIM", "from": "Kestrel Memory Works",
   "props": {"n": 4, "of_m": 5, "customer_class": "GPU producers"}}

Anything derived from this file must be labelled "oracle claim" in the write-up.

Usage:
  python3 inject_claim.py --relations out/relations.json \
      --out out/relations_oracleclaim.json
"""

import argparse
import json

CLAIM = {
    "doc_id": "KMW-2026-014",
    "doc_type": "press_release",
    "doc_date": "2026-07-21",
    "type": "AGGREGATE_SUPPLY_CLAIM",
    "from_raw": "Kestrel Memory Works",
    "to_raw": None,
    "from": "Kestrel Memory Works",
    "to": None,
    "stance": "asserted",
    "quote": ("Kestrel supplies high-bandwidth memory to four of the world's "
              "five top GPU producers"),
    "props": {"n": 4, "of_m": 5, "customer_class": "GPU producers"},
    "_ORACLE": True,
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--relations", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--n", type=int, default=4, help="the N in 'N of five'")
    ap.add_argument("--of-m", type=int, default=5)
    a = ap.parse_args()

    d = json.load(open(a.relations))
    # Remove the hallucinated named SUPPLIES edges from the claim document and
    # put the claim it should have produced in their place.
    before = len(d["relations"])
    d["relations"] = [r for r in d["relations"]
                      if not (r["doc_id"] == CLAIM["doc_id"] and r["type"] == "SUPPLIES")]
    removed = before - len(d["relations"])
    claim = dict(CLAIM)
    claim["props"] = {"n": a.n, "of_m": a.of_m, "customer_class": "GPU producers"}
    word = {2: "two", 3: "three", 4: "four", 5: "five"}[a.n]
    claim["quote"] = ("Kestrel supplies high-bandwidth memory to %s of the "
                      "world's five top GPU producers" % word)
    d["relations"].append(claim)
    d["oracle_claim_injected"] = True
    d["hallucinated_supply_edges_removed"] = removed
    with open(a.out, "w") as f:
        json.dump(d, f, indent=1)
    print("removed %d hallucinated SUPPLIES edges from %s, injected oracle claim "
          "n=%d of_m=%d" % (removed, CLAIM["doc_id"], a.n, a.of_m))


if __name__ == "__main__":
    main()
