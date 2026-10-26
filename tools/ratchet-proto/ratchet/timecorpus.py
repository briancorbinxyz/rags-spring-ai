"""Every state every unit has ever been in, each with the interval it was in force.

The corpus `corpus.build` produces is one snapshot: what the archive says now. A time question asks
what it said then, so this builds the other thing — one row per *state* of a unit, carrying the
half-open interval `[from_pos, to_pos)` of first-parent positions over which that text was current.
Filtering to a point in time is then `from_pos <= at < to_pos`, and the snapshot corpus is the
special case `at = the last position`.

It is much smaller than it sounds. Walking this archive gives **14,225 distinct unit-states against
10,540 units in the snapshot — 1.3x** — because 89% of addresses never change their text once. The
cost of keeping all of history at this granularity is a third more rows, not the 555x a
revision-per-index reading suggests.

Three decisions worth stating, each from a measurement rather than a preference:

**Intervals are half-open, and the end is a sentinel rather than NULL.** `LIVE` is a position past
any real one, so `from_pos <= at < to_pos` needs no special case for the current state and no
`COALESCE` in an index. The golden set's cutoffs are stated as `23:59:59Z`, which a half-open
interval handles without anyone having to decide what "as of the end of the day" includes.

**Positions, not dates.** 68 of this archive's 722 commits share a committer timestamp with another,
so a date cannot order two states that changed in the same second — and it would order them
arbitrarily but *stably*, which is the failure that passes a regression test. G-069 is that case in
the ground truth. `Revisions.at()` converts a cutoff to a position once, at the edge.

**A unit's identity is its address, and addresses outlive their pages.** 98 of the 725 spec pages
that have ever existed are not in the current tree — 004's `plan.html` became `design.html` — so a
state is closed when its page is deleted or renamed away, and the old address keeps its history.
G-072 asks about exactly that, and it is answerable only because the interval survives the rename.
"""

from __future__ import annotations

import subprocess
from dataclasses import dataclass, field
from pathlib import Path

from ratchet import corpus as C
from ratchet import html as H

#: A position after any real one: the state is still in force. A sentinel rather than NULL so that
#: one predicate serves every query and an index over it needs no coalescing.
LIVE = 1 << 30


@dataclass(frozen=True)
class State:
    """One unit's text over one interval of the archive's history."""

    address: str
    source: str
    anchor: str
    spec: str
    lifecycle: str
    title: str
    path: str
    heading: str
    text: str
    part: int
    parts: int
    from_pos: int
    to_pos: int = LIVE

    @property
    def live(self) -> bool:
        return self.to_pos == LIVE

    def covers(self, at: int) -> bool:
        return self.from_pos <= at < self.to_pos

    def passage(self) -> str:
        return f"{self.path}\n{self.text}" if self.path else self.text


@dataclass
class TimeCorpus:
    root: str
    states: list[State] = field(default_factory=list)
    positions: dict[str, int] = field(default_factory=dict)  # commit -> first-parent position

    def at(self, position: int) -> list[State]:
        """The states in force at a position: the snapshot corpus, for any point in the history."""
        return [s for s in self.states if s.covers(position)]

    def history_of(self, address: str) -> list[State]:
        return sorted((s for s in self.states if s.address == address), key=lambda s: s.from_pos)


def _git(root: Path, *args: str) -> str:
    return subprocess.run(["git", "-C", str(root), *args], capture_output=True, text=True).stdout


def _page_map(root: Path, commit: str, source: str) -> dict[str, tuple]:
    """One revision of one page, as units — through `corpus.page_units`, so the rule has one home."""
    html = _git(root, "cat-file", "-p", f"{commit}:{source}")
    if not html:
        return {}
    tree = H.parse(html)
    body = H.find_tag(tree, "body")
    if body is None:
        return {}
    title_el = H.find_tag(tree, "title")
    title = C._strip_join(title_el) if title_el else ""
    status_el = H.find_tag(tree, "spec-status")
    status = status_el.get("value") if status_el is not None else None
    out: dict[str, tuple] = {}
    for anchor, lc, hpath, heading, chunks in C.page_units(source, body, title, status):
        if anchor not in out:  # getElementById: the first one is the element
            out[anchor] = (lc, title, hpath, heading, tuple(chunks))
    return out


def build(root: Path, progress=None) -> TimeCorpus:
    """Walk the first-parent chain once, opening and closing a state whenever a unit's text moves."""
    root = Path(root)
    chain = [l.split() for l in
             _git(root, "log", "--first-parent", "--format=%H", "--reverse").splitlines() if l.strip()]
    position = {c[0]: i for i, c in enumerate(chain)}
    corpus = TimeCorpus(root=str(root), positions=position)

    touching = _git(root, "log", "--first-parent", "--reverse", "-M", "--format=@%H",
                    "--name-status", "--diff-filter=AMRD", "--", "specs/*.html")
    live: dict[str, tuple] = {}            # address -> the unit tuple currently in force
    opened: dict[str, int] = {}            # address -> position its current state opened at
    commit = None
    for line in touching.splitlines():
        if line.startswith("@"):
            commit = line[1:]
            continue
        if not line.strip() or commit is None:
            continue
        fields = line.split("\t")
        status, paths = fields[0], fields[1:]
        changed, removed = [], []
        if status.startswith("R"):
            removed.append(paths[0])
            changed.append(paths[1])
        elif status.startswith("D"):
            removed.append(paths[0])
        else:
            changed.append(paths[0])
        at = position[commit]

        fresh: dict[str, tuple] = {}
        for source in changed:
            if not source.endswith(".html"):
                continue
            for anchor, unit in _page_map(root, commit, source).items():
                fresh[f"{source}#{anchor}"] = unit
        gone = {a for a in live
                if any(a.startswith(f"{source}#") for source in removed if source.endswith(".html"))}
        touched_pages = {source for source in changed if source.endswith(".html")}
        gone |= {a for a in live
                 if a.split("#")[0] in touched_pages and a not in fresh}

        for address in gone:
            corpus.states.extend(_emit(address, live.pop(address), opened.pop(address), at))
        for address, unit in fresh.items():
            if live.get(address) == unit:
                continue
            if address in live:
                corpus.states.extend(_emit(address, live[address], opened[address], at))
            live[address] = unit
            opened[address] = at
        if progress:
            progress(at, len(corpus.states), len(live))

    for address, unit in live.items():
        corpus.states.extend(_emit(address, unit, opened[address], LIVE))
    corpus.states.sort(key=lambda s: (s.address, s.from_pos, s.part))
    return corpus


def _emit(address: str, unit: tuple, from_pos: int, to_pos: int) -> list[State]:
    lc, title, hpath, heading, chunks = unit
    source, _, anchor = address.partition("#")
    return [State(address, source, anchor, C.spec_of(source), lc, title, hpath, heading, chunk,
                  i + 1, len(chunks), from_pos, to_pos)
            for i, chunk in enumerate(chunks)]


# --- cache ------------------------------------------------------------------------------------

VERSION = "v1"


def cache_file(cache_dir: Path, commit: str) -> Path:
    return Path(cache_dir) / f"timecorpus-{commit[:7]}-{VERSION}.json"


def save(corpus: TimeCorpus, path: Path) -> None:
    import json
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text(json.dumps({
        "root": corpus.root, "positions": corpus.positions,
        "states": [[s.address, s.source, s.anchor, s.spec, s.lifecycle, s.title, s.path,
                    s.heading, s.text, s.part, s.parts, s.from_pos, s.to_pos]
                   for s in corpus.states]}), encoding="utf-8")


def load(path: Path) -> TimeCorpus:
    import json
    raw = json.loads(Path(path).read_text(encoding="utf-8"))
    return TimeCorpus(root=raw["root"], positions=raw["positions"],
                      states=[State(*row) for row in raw["states"]])


def load_or_build(root: Path, cache_dir: Path, commit: str, progress=None) -> TimeCorpus:
    """Built once per archive revision and kept: the walk costs about 45 seconds."""
    path = cache_file(cache_dir, commit)
    if path.exists():
        return load(path)
    corpus = build(root, progress)
    save(corpus, path)
    return corpus
