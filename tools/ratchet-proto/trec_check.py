"""Score the prototype's TREC run files with a scorer we didn't write, and compare with our own numbers.

    python3 tools/ratchet-proto/trec_check.py results/run            # after run.py --out results/run
    python3 tools/ratchet-proto/trec_check.py results/run S1 S3 X1   # only these configurations

Needs ``pip install ir-measures``. Reads ``src/main/resources/golden/qrels.txt`` (regenerate it with
``python3 -m ratchet.trec``), ``<out>.<config>.run`` and ``<out>.jsonl``. Our Hit@4 must equal
Success@4 for every configuration, or the export (or the scorer) is wrong.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
QRELS = REPO / "src/main/resources/golden/qrels.txt"


def main(argv: list) -> int:
    try:
        import ir_measures
        from ir_measures import RR, Judged, R, Success
    except ImportError:
        print("pip install ir-measures", file=sys.stderr)
        return 2
    out = Path(argv[0])
    keys = argv[1:] or sorted(p.name[len(out.name) + 1:-4] for p in out.parent.glob(f"{out.name}.*.run"))
    golden = {json.loads(l)["id"]: json.loads(l) for l in
              (REPO / "src/main/resources/golden/questions.jsonl").read_text(encoding="utf-8").splitlines() if l.strip()}
    rows = [json.loads(l) for l in out.with_suffix(".jsonl").read_text(encoding="utf-8").splitlines() if l.strip()]
    qrels = list(ir_measures.read_trec_qrels(str(QRELS)))
    print(f"{'config':8} {'Hit@4 (ours)':>12} {'Success@4':>10} {'RR@10':>6} {'R@4':>5} {'Judged@4':>8}")
    bad = 0
    for key in keys:
        run = list(ir_measures.read_trec_run(str(out.parent / f"{out.name}.{key}.run")))
        agg = ir_measures.calc_aggregate([Success @ 4, RR @ 10, R @ 4, Judged @ 4], qrels, run)
        mine = [r for r in rows if r.get("config") == key and golden[r["id"]]["stratum"] != "unanswerable"]
        hits = sum(1 for r in mine if r["first_hit"] is not None and r["first_hit"] <= 4)
        n = len(mine)
        succ = round(agg[Success @ 4] * n)
        bad += succ != hits
        print(f"{key:8} {hits:>9}/{n} {succ:>7}/{n} {agg[RR @ 10]:>6.3f} {agg[R @ 4]:>5.2f} {agg[Judged @ 4]:>8.2f}"
              + ("" if succ == hits else "  MISMATCH"))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
