#!/usr/bin/env python3
"""The time questions' ground truth, and what a retrieval run is really worth against it.

Two jobs, both read-only, neither of them a retrieval step.

**The probe** (default) answers the question that comes before any design: are the answers there to
be retrieved? If the archive's history cannot supply them, the fourteen revision-dependent questions
are unmeasurable whatever gets built. It reports reachability of every commit the question file
names, whether each cutoff question's address can be recovered as it stood at its cutoff, whether
first-parent position reproduces the declared version ordering, and the shape of the history that
ordering depends on. It exits non-zero if a named commit is missing, which is usually a ``--depth 1``
clone — both runner scripts make one.

**The re-score** (``--score``) answers what a run already on disk is worth. A time question with a
cutoff asks what an address said *then*, and the pipeline serves one snapshot, so a retrieved
address is only an answer when its text has not changed since. Where it has, the run found the right
place and served the wrong text, and ``Hit@4`` counted it. This re-prices any run's ``.jsonl``
without re-running anything, which is the point: the correction is arithmetic over rankings that
already exist, so it cannot be confused with a change in retrieval.

    tools/ratchet-proto/revisions.py
    tools/ratchet-proto/revisions.py --verbose
    tools/ratchet-proto/revisions.py --score results/episode-6/time-laptop.jsonl
    tools/ratchet-proto/revisions.py --score tools/ratchet-proto/results/5090-*-time.jsonl
"""

from __future__ import annotations

import argparse
import collections
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
sys.path.insert(0, str(HERE))

from ratchet import revisions as V  # noqa: E402

DEPTH = 4
ORDER = ["history", "history-decoy", "as-of", "what-changed", "as-known"]


def questions_in(path: Path) -> list[dict]:
    return [json.loads(l) for l in path.read_text(encoding="utf-8").splitlines() if l.strip()]


def commits_named(questions: list[dict]) -> set[str]:
    return {v["commit"] for q in questions
            for v in (q.get("versions") or []) + (q.get("stale_versions") or [])}


# --- the probe --------------------------------------------------------------------------------

def probe(history: V.GitHistory, questions: list[dict], verbose: bool) -> int:
    print(f"# {history.root}, {len(history)} commits on the first-parent chain\n")

    named = commits_named(questions)
    missing = sorted(c for c in named if c not in history)
    print(f"## Reachability: {len(named) - len(missing)}/{len(named)} named commits present")
    if missing:
        print(f"\n{len(missing)} are not in this clone, so the ground truth cannot be read.")
        if history.is_shallow():
            print("This is a shallow clone. Both runner scripts create one with --depth 1:\n"
                  f"    git -C {history.root} fetch --unshallow")
        else:
            for commit in missing:
                print(f"    {commit}")
        return 1

    print("\n## Recovery: each cutoff question's address as it stood at its cutoff\n")
    print(f"{'id':7} {'stratum':12} {'cutoff':21} {'revision':14} then vs now")
    print("-" * 78)
    recovered = differs = absent = 0
    for q in questions:
        if not q.get("cutoff"):
            continue
        address = (q["expected"] or q["stale"])[0]
        revision = history.at(q["cutoff"])
        then = history.text(revision.commit, address)
        now = history.text(q["commit"], address)
        if then is None:
            absent += 1
            print(f"{q['id']:7} {q['stratum']:12} {q['cutoff']:21} {str(revision):14} "
                  f"not recovered (absent at that commit)")
            continue
        recovered += 1
        differs += then != now
        print(f"{q['id']:7} {q['stratum']:12} {q['cutoff']:21} {str(revision):14} "
              f"{'DIFFERENT' if then != now else 'identical'}")
        if verbose:
            print(f"        then: {then[:150]}")
            print(f"        now : {(now or '(address absent)')[:150]}")
    print("-" * 78)
    print(f"recovered {recovered}, of which {differs} differ from what is served today; "
          f"{absent} not recovered")

    print("\n## Ordering: does first-parent position reproduce the declared role order?\n")
    for q in questions:
        versions = q.get("versions") or []
        if not versions:
            continue
        at = [history.of(v["commit"]).position for v in versions]
        stamps = [history.of(v["commit"]).committed for v in versions]
        ordered = all(at[i] < at[i + 1] for i in range(len(at) - 1))
        tied = " — committer dates TIE, position is the only key" \
            if len(set(stamps)) < len(stamps) else ""
        print(f"  {q['id']}  {at}  {'OK' if ordered else 'WRONG'}{tied}")

    shape = history.shape()
    print("\n## Repository shape, on which the ordering key depends\n")
    print(f"  merge commits                         {shape['merges']}")
    print(f"  committer date goes backwards          {shape['committer_date_backwards']} times")
    print(f"  author date goes backwards             {shape['author_date_backwards']} times")
    print(f"  commits sharing a committer timestamp  {shape['tied_commits']} "
          f"in {shape['tied_groups']} groups")
    print("\n" + ("Linear, so position is a total order and a usable index key."
                  if shape["merges"] == 0 else
                  "Not linear: position along first parent is still total, but it is a choice of "
                  "path rather than the only reading of the history."))
    return 0


# --- the re-score -----------------------------------------------------------------------------

def rescore(history: V.GitHistory, questions: list[dict], runs: list[Path]) -> int:
    by_id = {q["id"]: q for q in questions}
    print("# Address-level Hit@4, and what survives asking for the right revision\n")
    print("A time question with a cutoff wants what an address said *then*. The pipeline serves one\n"
          "snapshot, so a retrieved address answers it only where the text has not changed since.\n"
          "`history` and `history-decoy` ask about archived documents, which the snapshot holds\n"
          "under their own addresses, so they are scored by address as before.\n")
    for path in runs:
        rows = [json.loads(l) for l in path.read_text(encoding="utf-8").splitlines() if l.strip()]
        by_config: dict[str, list] = collections.defaultdict(list)
        for row in rows:
            by_config[row["config"]].append(row)
        print(f"## {path.name}\n")
        print(f"| config | Hit@4 | revision-aware | " + " | ".join(ORDER) + " |")
        print("|---" * (len(ORDER) + 3) + "|")
        for config, group in by_config.items():
            address_hits = revision_hits = 0
            per = {s: [0, 0] for s in ORDER}
            for row in group:
                q = by_id[row["id"]]
                got = [a for a, r in zip(row["expected"], row["ranks"])
                       if r is not None and r <= DEPTH]
                hit = bool(got)
                kept = any(V.verdict(history, q, a, True) == V.RIGHT_REVISION for a in got) \
                    if q["stratum"] not in V.BY_ADDRESS else hit
                address_hits += hit
                revision_hits += kept
                per[row["stratum"]][0] += hit
                per[row["stratum"]][1] += kept
            cells = " | ".join(f"{a} → **{k}**" if a != k else str(a) for a, k in
                               (per[s] for s in ORDER))
            print(f"| {config} | {address_hits}/{len(group)} | **{revision_hits}/{len(group)}** "
                  f"| {cells} |")
        print()

    print("## What-changed: how many declared versions one snapshot can supply\n")
    for q in questions:
        same, total = V.covered(history, q)
        if total:
            print(f"  {q['id']}  {same} of {total} declared versions match the snapshot's text")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--spectastic", default=str(REPO.parent / "spectastic"))
    ap.add_argument("--golden", default=str(REPO / "src/main/resources/golden/questions-time.jsonl"))
    ap.add_argument("--score", nargs="+", metavar="RUN.jsonl",
                    help="re-price these runs instead of probing the ground truth")
    ap.add_argument("--verbose", action="store_true", help="the probe also prints recovered text")
    args = ap.parse_args()

    root = Path(args.spectastic).resolve()
    if not (root / "specs").is_dir():
        raise SystemExit(f"no spectastic checkout at {root}")
    history = V.GitHistory(root)
    questions = questions_in(Path(args.golden))
    if args.score:
        return rescore(history, questions, [Path(p) for p in args.score])
    return probe(history, questions, args.verbose)


if __name__ == "__main__":
    sys.exit(main())
