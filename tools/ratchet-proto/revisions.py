#!/usr/bin/env python3
"""Can the time questions' ground truth be recovered from git, and what orders it?

The 24 questions in ``questions-time.jsonl`` ask what a record said at a point in the past. Nothing
in the retrieval line can answer that yet, because the corpus is one snapshot. Before designing
anything that could, this answers the prior question: are the answers *there* to be retrieved? If
``versions`` cannot be reconstructed from the archive's history, the fourteen revision-dependent
questions are unmeasurable whatever gets built, and that is worth knowing before building it.

It reports four things, and exits non-zero if the first one fails:

1. **Reachability.** Every commit the question file names, present in this clone. A ``--depth 1``
    clone fails here, which is the common case: both runner scripts create one.
2. **Recovery.** Each cutoff question's address as it stood at its cutoff, and whether that differs
    from what the current snapshot serves. A question whose answer is accidentally still current
    would be measuring nothing.
3. **Ordering.** Whether first-parent position reproduces the declared before/intermediate/after
    order in ``versions`` — the ordering ground truth.
4. **Repository shape.** Merges, whether committer and author dates run monotonically along
    first-parent, and how many commits share a timestamp with another. Ordering keys depend on all
    three, and the answers are properties of this archive rather than of git in general.

    tools/ratchet-proto/revisions.py
    tools/ratchet-proto/revisions.py --spectastic ../spectastic --verbose

Nothing here is a retrieval step and nothing here is scored. It materialises revisions with
``cat-file`` rather than a worktree per revision, which is the difference between no disk and
roughly 61 MB each.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
sys.path.insert(0, str(HERE))

from ratchet import html as H  # noqa: E402
from ratchet import score as S  # noqa: E402


class History:
    """The first-parent chain, and the two things a revision is addressed by.

    Position on that chain is the index key rather than a date. A date is the obvious choice and the
    wrong one: committer timestamps are shared by commits made in the same second, so they order a
    pair of revisions arbitrarily but *stably*, which is the failure that hides from a regression
    test. Position is a total order whenever the history is linear, and `shape` says whether it is.
    """

    def __init__(self, root: Path):
        self.root = root
        chain = [l.split() for l in self._git("log", "--first-parent", "--format=%H %ct %at",
                                              "--reverse").splitlines() if l.strip()]
        self.order = [c[0] for c in chain]
        self.position = {c[0]: i for i, c in enumerate(chain)}
        self.committed = {c[0]: int(c[1]) for c in chain}
        self.authored = {c[0]: int(c[2]) for c in chain}

    def _git(self, *args: str) -> str:
        done = subprocess.run(["git", "-C", str(self.root), *args],
                              capture_output=True, text=True)
        return done.stdout

    def is_shallow(self) -> bool:
        return (self.root / ".git" / "shallow").exists() or \
            self._git("rev-parse", "--is-shallow-repository").strip() == "true"

    def at(self, when: str) -> str | None:
        """The last commit on the chain whose committer time is at or before `when`.

        Not ``rev-list --before``, which stops traversal at the first commit outside the window
        instead of filtering, and on a branching history returns something surprising. This archive
        is linear, so the two agree — but the agreement is a property of the archive, not a reason
        to rely on the flag.
        """
        want = int(dt.datetime.fromisoformat(when.replace("Z", "+00:00")).timestamp())
        found = None
        for commit in self.order:
            if self.committed[commit] > want:
                break
            found = commit
        return found

    def element(self, commit: str, address: str) -> tuple[str | None, str]:
        """The normalised text of one element, as it stood in one revision."""
        path, _, ident = address.partition("#")
        page = self._git("cat-file", "-p", f"{commit}:{path}")
        if not page:
            return None, "page absent at that commit"
        found = H.parse(page).find(lambda e: e.id == ident)
        if found is None:
            return None, "page present, element absent"
        return S.normalise(H.jsoup_text(found)), ""

    def shape(self) -> dict:
        merges = int(self._git("rev-list", "--merges", "--count", "HEAD").strip() or 0)
        back_c = sum(1 for i in range(1, len(self.order))
                     if self.committed[self.order[i]] < self.committed[self.order[i - 1]])
        back_a = sum(1 for i in range(1, len(self.order))
                     if self.authored[self.order[i]] < self.authored[self.order[i - 1]])
        groups: dict[int, int] = {}
        for commit in self.order:
            groups[self.committed[commit]] = groups.get(self.committed[commit], 0) + 1
        tied = {t: n for t, n in groups.items() if n > 1}
        return {"commits": len(self.order), "merges": merges,
                "committer_date_backwards": back_c, "author_date_backwards": back_a,
                "tied_groups": len(tied), "tied_commits": sum(tied.values())}


def commits_named(questions: list[dict]) -> set[str]:
    named = set()
    for q in questions:
        for v in (q.get("versions") or []) + (q.get("stale_versions") or []):
            named.add(v["commit"])
    return named


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--spectastic", default=str(REPO.parent / "spectastic"))
    ap.add_argument("--golden", default=str(REPO / "src/main/resources/golden/questions-time.jsonl"))
    ap.add_argument("--verbose", action="store_true", help="also print the recovered text")
    args = ap.parse_args()

    root = Path(args.spectastic).resolve()
    if not (root / "specs").is_dir():
        raise SystemExit(f"no spectastic checkout at {root}")
    history = History(root)
    questions = [json.loads(l) for l in Path(args.golden).read_text(encoding="utf-8").splitlines()
                 if l.strip()]

    print(f"# {root}, {history.shape()['commits']} commits on the first-parent chain\n")

    # --- 1. reachability -----------------------------------------------------------------------
    named = commits_named(questions)
    missing = sorted(c for c in named if c not in history.position)
    print(f"## Reachability: {len(named) - len(missing)}/{len(named)} named commits present")
    if missing:
        print(f"\n{len(missing)} are not in this clone, so the ground truth cannot be read.")
        if history.is_shallow():
            print("This is a shallow clone. Both runner scripts create one with --depth 1:\n"
                  "    git -C %s fetch --unshallow" % root)
        else:
            for c in missing:
                print(f"    {c}")
        return 1

    # --- 2. recovery ---------------------------------------------------------------------------
    print("\n## Recovery: each cutoff question's address as it stood at its cutoff\n")
    print(f"{'id':7} {'stratum':12} {'cutoff':21} {'revision':14} then vs now")
    print("-" * 78)
    recovered = differs = absent = 0
    for q in questions:
        if not q.get("cutoff"):
            continue
        address = (q["expected"] or q["stale"])[0]
        revision = history.at(q["cutoff"])
        then, why = history.element(revision, address)
        now, _ = history.element(q["commit"], address)
        tag = f"{revision[:8]} p{history.position[revision]:<4}"
        if then is None:
            absent += 1
            print(f"{q['id']:7} {q['stratum']:12} {q['cutoff']:21} {tag} not recovered ({why})")
            continue
        recovered += 1
        differs += then != now
        print(f"{q['id']:7} {q['stratum']:12} {q['cutoff']:21} {tag} "
              f"{'DIFFERENT' if then != now else 'identical'}")
        if args.verbose:
            print(f"        then: {then[:150]}")
            print(f"        now : {(now or '(address absent)')[:150]}")
    print("-" * 78)
    print(f"recovered {recovered}, of which {differs} differ from what is served today; "
          f"{absent} not recovered")

    # --- 3. ordering ---------------------------------------------------------------------------
    print("\n## Ordering: does first-parent position reproduce the declared role order?\n")
    for q in questions:
        versions = q.get("versions") or []
        if not versions:
            continue
        at = [history.position[v["commit"]] for v in versions]
        stamps = [history.committed[v["commit"]] for v in versions]
        ordered = all(at[i] < at[i + 1] for i in range(len(at) - 1))
        tied = " — committer dates TIE, position is the only key" if len(set(stamps)) < len(stamps) \
            else ""
        print(f"  {q['id']}  {at}  {'OK' if ordered else 'WRONG'}{tied}")

    # --- 4. repository shape -------------------------------------------------------------------
    shape = history.shape()
    print("\n## Repository shape, on which the ordering key depends\n")
    print(f"  merge commits                         {shape['merges']}")
    print(f"  committer date goes backwards          {shape['committer_date_backwards']} times")
    print(f"  author date goes backwards             {shape['author_date_backwards']} times")
    print(f"  commits sharing a committer timestamp  {shape['tied_commits']} "
          f"in {shape['tied_groups']} groups")
    if shape["merges"] == 0:
        print("\nLinear, so position is a total order and a usable index key.")
    else:
        print("\nNot linear: position along first parent is still total, but it is a choice of path "
              "rather than the only reading of the history.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
