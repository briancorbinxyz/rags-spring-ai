"""The known states of an address, in order, each with a point in time.

This is the port Episode 6 is really about. The search has always asked *where* a record is; a time
question asks *when*, and the two keys are independent: a lifecycle says whether a record is in
force now, and a revision says what it said then. `StandingFilter` answers the first and nothing
answers the second.

The capability is three questions. Which revision was in force at a moment; what one address said in
one revision; and how two revisions order. An archive may answer them from a version-control
history, a `valid_from` column, or a list of prior values kept beside the current one — `GitHistory`
is this corpus's adapter and the only one here, but nothing above it knows that.

**Ordering is a position, not a date.** A date is the obvious key and the wrong one. In this archive
68 of 722 commits share a committer timestamp with another, in 16 groups, so a date orders those
pairs arbitrarily — and *stably*, which is the failure that passes a regression test every time. One
of them is ground truth: G-069's two intermediate revisions are a second apart in neither direction.
Position on the first-parent chain is a total order whenever the history is linear, and
`GitHistory.shape` reports whether it is.
"""

from __future__ import annotations

import datetime as dt
import subprocess
from dataclasses import dataclass
from pathlib import Path

from ratchet import html as H
from ratchet import score as S


@dataclass(frozen=True)
class Revision:
    """One known state of the archive: what to ask for, and where it sits."""

    commit: str
    position: int
    committed: int

    def __str__(self) -> str:
        return f"{self.commit[:8]} p{self.position}"


class GitHistory:
    """`Revisions` over a git checkout, reading the first-parent chain.

    Revisions are materialised with ``cat-file`` rather than a worktree each, which is the
    difference between no disk and roughly 61 MB per revision — 34 GB over this archive.
    """

    def __init__(self, root: Path):
        self.root = Path(root)
        chain = [line.split() for line in
                 self._git("log", "--first-parent", "--format=%H %ct %at", "--reverse").splitlines()
                 if line.strip()]
        self._chain = [Revision(c[0], i, int(c[1])) for i, c in enumerate(chain)]
        self._by_commit = {r.commit: r for r in self._chain}
        self._authored = {c[0]: int(c[2]) for c in chain}
        self._text: dict[tuple[str, str], str | None] = {}

    def _git(self, *args: str) -> str:
        return subprocess.run(["git", "-C", str(self.root), *args],
                              capture_output=True, text=True).stdout

    # --- the capability ---------------------------------------------------------------------

    def __len__(self) -> int:
        return len(self._chain)

    def __contains__(self, commit: str) -> bool:
        return commit in self._by_commit

    def of(self, commit: str) -> Revision | None:
        return self._by_commit.get(commit)

    def at(self, when: str) -> Revision | None:
        """The last revision whose committer time is at or before `when`.

        Deliberately not ``rev-list --before``, which stops traversal at the first commit outside
        the window rather than filtering it, and on a branching history answers something
        surprising. This archive is linear so the two agree, but that is a property of the archive.
        """
        want = int(dt.datetime.fromisoformat(when.replace("Z", "+00:00")).timestamp())
        found = None
        for revision in self._chain:
            if revision.committed > want:
                break
            found = revision
        return found

    def text(self, commit: str, address: str) -> str | None:
        """What one address said in one revision, or None if it did not exist there.

        A page renamed since is simply absent, which is the right answer and not an error: 004's
        `plan.html` became `design.html`, and G-072 asks about it under the old name.
        """
        key = (commit, address)
        if key in self._text:
            return self._text[key]
        path, _, ident = address.partition("#")
        page = self._git("cat-file", "-p", f"{commit}:{path}")
        found = H.parse(page).find(lambda e: e.id == ident) if page else None
        self._text[key] = S.normalise(H.jsoup_text(found)) if found is not None else None
        return self._text[key]

    def is_shallow(self) -> bool:
        return (self.root / ".git" / "shallow").exists() or \
            self._git("rev-parse", "--is-shallow-repository").strip() == "true"

    def shape(self) -> dict:
        """Whether position is a safe key here, and whether a date would have been."""
        backwards = {"committer": 0, "author": 0}
        for i in range(1, len(self._chain)):
            if self._chain[i].committed < self._chain[i - 1].committed:
                backwards["committer"] += 1
            if self._authored[self._chain[i].commit] < self._authored[self._chain[i - 1].commit]:
                backwards["author"] += 1
        groups: dict[int, int] = {}
        for revision in self._chain:
            groups[revision.committed] = groups.get(revision.committed, 0) + 1
        tied = {t: n for t, n in groups.items() if n > 1}
        return {"commits": len(self._chain),
                "merges": int(self._git("rev-list", "--merges", "--count", "HEAD").strip() or 0),
                "committer_date_backwards": backwards["committer"],
                "author_date_backwards": backwards["author"],
                "tied_groups": len(tied), "tied_commits": sum(tied.values())}


# --- scoring a retrieved address against the revision the question asked about -----------------

#: Strata whose correctness is an address. `history` and `history-decoy` ask about archived
#: *documents* — a withdrawn proposal, a superseded task — and those are in the snapshot under their
#: own addresses, so address-level scoring already answers them correctly and is left alone.
BY_ADDRESS = ("history", "history-decoy")

WRONG_REVISION = "wrong-revision"
RIGHT_REVISION = "right-revision"
NOT_RETRIEVED = "not-retrieved"
NOT_IN_SNAPSHOT = "not-in-snapshot"


def verdict(history: GitHistory, question: dict, address: str, retrieved: bool) -> str:
    """Whether a retrieved address actually answered a time question.

    The pipeline serves one snapshot, so the text behind a retrieved address is whatever that
    address says at the corpus commit. For a question with a cutoff that is only the answer when the
    text has not changed since. Where it has, the retriever found the right *place* and served the
    wrong *text* — which an address-level measure scores as a hit, and which is the whole reason
    `as-of` and `as-known` cannot be read off today's numbers.
    """
    if not retrieved:
        return NOT_RETRIEVED
    cutoff = question.get("cutoff")
    if not cutoff:
        return RIGHT_REVISION
    wanted = history.at(cutoff)
    then = history.text(wanted.commit, address) if wanted else None
    now = history.text(question["commit"], address)
    if now is None:
        return NOT_IN_SNAPSHOT
    if then is None:
        return WRONG_REVISION
    return RIGHT_REVISION if then == now else WRONG_REVISION


def covered(history: GitHistory, question: dict) -> tuple[int, int]:
    """For a `what-changed` question, how many of its declared versions the snapshot can supply.

    The question asks for the successive states of one address, so the ground truth is several
    revisions of it and a single snapshot holds one. This counts how many of those states the
    corpus commit's text actually matches — the ceiling on what any ranking over this corpus could
    show a reader, before retrieval is considered at all.
    """
    versions = question.get("versions") or []
    if not versions:
        return 0, 0
    address = (question.get("expected") or [None])[0]
    now = history.text(question["commit"], address) if address else None
    same = sum(1 for v in versions
               if history.text(v["commit"], v.get("address", address)) == now)
    return same, len(versions)
