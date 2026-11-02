#!/usr/bin/env python3
"""Is B3's answer evidence-driven, or an artefact of tie-breaking?

B3 ranks the five candidates by per-pair evidence and keeps the top four. In the
primary run three candidates tie at 0.0, so the cut between "supplied" and
"excluded" is decided by whatever order Python's stable sort happened to see --
that is, by the order the candidate list was written in, not by evidence.

This recomputes B3 over every permutation of the candidate list, reusing the
stored per-pair verdicts, with no new model calls. If B3 is sound, the answer
should be stable across permutations.

Usage:
  python3 b3_tiebreak_test.py --retrieval out/retrieval.json \
      --truth truth/ground_truth.json
"""

import argparse
import itertools
import json


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--retrieval", required=True)
    ap.add_argument("--truth", required=True)
    a = ap.parse_args()

    T = json.load(open(a.truth))
    gold_exc = T["excluded"]
    gold_sup = set(T["supplied"])
    R = json.load(open(a.retrieval))

    for rep in R["reps"]:
        pairs = rep["B2"]["pairs"]

        def key(p):
            v = pairs[p]
            bump = {"yes": 1.0, "unclear": 0.4, "no": 0.0}.get(v["verdict"], 0.0)
            return bump + v["evidence_strength"]

        names = list(pairs)
        scores = {p: key(p) for p in names}
        print("rep %d scores: %s" % (rep["rep"], {k: round(v, 2) for k, v in scores.items()}))
        ties = {}
        for p, s in scores.items():
            ties.setdefault(s, []).append(p)
        print("  tie groups: %s" % {s: v for s, v in ties.items() if len(v) > 1})

        correct = 0
        total = 0
        excluded_counts = {}
        for perm in itertools.permutations(names):
            ranked = sorted(perm, key=lambda p: -scores[p])
            exc = ranked[4]
            sup = set(ranked[:4])
            total += 1
            excluded_counts[exc] = excluded_counts.get(exc, 0) + 1
            if exc == gold_exc and sup == gold_sup:
                correct += 1
        print("  exact-correct on %d/%d candidate orderings (%.0f%%)"
              % (correct, total, 100.0 * correct / total))
        print("  excluded chosen: %s" % dict(sorted(
            excluded_counts.items(), key=lambda kv: -kv[1])))
        print()


if __name__ == "__main__":
    main()
