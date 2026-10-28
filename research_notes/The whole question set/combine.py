#!/usr/bin/env python3
"""One scoreboard over every question the series has written, not one set at a time.

The series grew four question files and reported each on its own: 53 on the snapshot, 24 about
time, 13 held out, 15 about routing. Four headlines is three too many. It also flatters the main
set, because the main set is the one the work was done against — 47 of 47 is a real number and a
narrow one, and the only honest way to say how far the retriever has come is to put every question
the series has ever written into a single denominator.

This reads the per-question rows the two runners already write and merges them. Nothing is scored
here, so no measurement can be invented: a question's result is whichever run already produced it.

**Sources are declared in precedence order, and the first one holding a question owns it.** That is
how the time set gets its honest reading without a special case. Fourteen of its questions name a
cutoff or a window and belong on a slice of the archive, so the slice run is declared first and
answers for those; the other ten have no cutoff, are absent from the slice run, and fall through to
the snapshot run behind it.

**The reserve stays reserved.** Held-out and routing questions are scored once, at an episode's end,
and a combined headline recomputed on every run would quietly spend them. So a reserve source is
refused unless `--at-episode-end` is passed, exactly as `run.py` refuses the held-out file. Without
it this reports the open sets alone and says how many questions it is not looking at.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
sys.path.insert(0, str(HERE))

from ratchet import score as S  # noqa: E402

UNIVERSE = [
    ("main", "questions.jsonl", False),
    ("time", "questions-time.jsonl", False),
    ("heldout", "questions-heldout.jsonl", True),
    ("routing", "questions-routing.jsonl", True),
]


def read_source(spec: str) -> tuple:
    """`<set>=<path>:<config>` — one run's rows for one configuration, keyed by question."""
    name, _, rest = spec.partition("=")
    path, _, config = rest.rpartition(":")
    rows = {}
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        row = json.loads(line)
        if row["config"] == config:
            rows[row["id"]] = row
    if not rows:
        raise SystemExit(f"{path} holds no rows for config {config}")
    return name, Path(path), config, rows


def as_result(row: dict, question: dict) -> S.QResult:
    """A row back into the scorer's own type, so every number here is the scorer's."""
    return S.QResult(row["id"], row.get("stratum") or question["stratum"],
                     list(row.get("expected") or []), list(row.get("ranks") or []),
                     list(row.get("partial_ranks") or []), list(row.get("stale_ranks") or []),
                     list(row.get("top10") or []), row.get("context_tokens_top4") or 0,
                     row.get("top1_score"), row.get("pinned") or 0)


def tally(results: list) -> dict:
    answerable = [r for r in results if r.expected]
    without_ground, missed, unanswerable, confident, total = S.groundless(results)
    return {
        "n": len(results),
        "answerable": len(answerable),
        "hit4": sum(1 for r in answerable if r.hit_at(4)),
        "all4": sum(1 for r in answerable if r.all_at(4)),
        "stale4": sum(1 for r in results if r.stale_at(4)),
        "mrr": round(sum(r.rr() for r in answerable) / len(answerable), 3) if answerable else 0.0,
        "groundless": without_ground, "missed": missed, "unanswerable": unanswerable,
        "confident": confident, "total": total,
    }


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--source", action="append", default=[], metavar="SET=PATH:CONFIG",
                    help="in precedence order; the first source holding a question owns it")
    ap.add_argument("--at-episode-end", action="store_true",
                    help="admit the reserve sets, which are scored once and then spent")
    ap.add_argument("--golden-dir", default=str(REPO / "src/main/resources/golden"),
                    help="where the four question files live; the whole universe is read from here")
    ap.add_argument("--label", default="", help="what this line of the stack is")
    ap.add_argument("--out", default="", help="markdown output path, without suffix")
    args = ap.parse_args()

    golden = Path(args.golden_dir)
    universe = [(name, golden / filename, is_reserve) for name, filename, is_reserve in UNIVERSE]
    absent = [path.name for _, path, _ in universe if not path.exists()]
    if absent:
        raise SystemExit(f"{golden} is missing {', '.join(absent)} — the denominator would be "
                         f"wrong and silently so; pass --golden-dir")

    questions, belongs, reserved = {}, {}, set()
    for name, path, is_reserve in universe:
        for line in path.read_text(encoding="utf-8").splitlines():
            if not line.strip():
                continue
            question = json.loads(line)
            questions[question["id"]] = question
            belongs[question["id"]] = name
        if is_reserve:
            reserved.add(name)

    owned, sources = {}, []
    for spec in args.source:
        name, path, config, rows = read_source(spec)
        if name in reserved and not args.at_episode_end:
            raise SystemExit(f"refusing to read the {name} set: it is scored once, at an episode's "
                             f"end — pass --at-episode-end if that is where you are")
        taken = 0
        for qid, row in rows.items():
            if qid in questions and qid not in owned:
                owned[qid] = as_result(row, questions[qid])
                taken += 1
        sources.append((name, path, config, len(rows), taken))

    admitted = {qid for qid in questions if belongs[qid] not in reserved or args.at_episode_end}
    scored = [r for qid, r in owned.items() if qid in admitted]
    missing = sorted(qid for qid in admitted if qid not in owned)

    lines = [f"# The whole question set{': ' + args.label if args.label else ''}", ""]
    lines.append(f"The series has written **{len(questions)} questions** across four files. This "
                 f"scores **{len(scored)}** of them from runs that already happened.")
    if not args.at_episode_end:
        held = len(questions) - len(admitted)
        lines.append("")
        lines.append(f"The reserve is not open: **{held} questions** (held-out and routing) are not "
                     f"in any number below, because they are scored once, at an episode's end.")
    lines += ["", "## Sources, in precedence order", "",
              "| set | run | config | rows | questions it owns |", "|---|---|---|---|---|"]
    for name, path, config, rows, taken in sources:
        lines.append(f"| {name} | `{path.name}` | `{config}` | {rows} | {taken} |")

    whole = tally(scored)
    lines += ["", "## The headline", "",
              "| questions | answerable | Hit@4 | all-evidence@4 | MRR@50 | groundless@4 | "
              "unanswerable | confidently wrong | stale@4 |", "|---|---|---|---|---|---|---|---|---|",
              f"| {whole['n']} | {whole['answerable']} | **{whole['hit4']}/{whole['answerable']}** "
              f"({whole['hit4'] * 100 // max(1, whole['answerable'])}%) | {whole['all4']} | "
              f"{whole['mrr']} | {whole['groundless']}/{whole['n']} | {whole['unanswerable']} | "
              f"{whole['confident']} | {whole['stale4']} |"]

    lines += ["", "## By set", "",
              "| set | questions | Hit@4 | groundless@4 | confidently wrong |", "|---|---|---|---|---|"]
    for name, _, is_reserve in universe:
        rows = [r for r in scored if belongs[r.qid] == name]
        if not rows:
            continue
        got = tally(rows)
        lines.append(f"| {name}{' (reserve)' if is_reserve else ''} | {got['n']} | "
                     f"{got['hit4']}/{got['answerable']} | {got['groundless']}/{got['n']} | "
                     f"{got['confident']} |")

    lines += ["", "## By stratum", "", "| stratum | questions | Hit@4 | groundless@4 |",
              "|---|---|---|---|"]
    for stratum in sorted({r.stratum for r in scored}):
        rows = [r for r in scored if r.stratum == stratum]
        got = tally(rows)
        lines.append(f"| {stratum} | {got['n']} | {got['hit4']}/{got['answerable']} | "
                     f"{got['groundless']}/{got['n']} |")

    if missing:
        lines += ["", "## Admitted but unscored", "",
                  f"**{len(missing)} questions** no source covered: " +
                  ", ".join(f"`{qid}`" for qid in missing)]

    report = "\n".join(lines) + "\n"
    print(report)
    if args.out:
        Path(args.out).with_suffix(".md").write_text(report, encoding="utf-8")
        print(f"wrote {Path(args.out).with_suffix('.md')}", file=sys.stderr)


if __name__ == "__main__":
    main()
