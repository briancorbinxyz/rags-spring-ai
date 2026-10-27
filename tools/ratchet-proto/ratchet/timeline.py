"""Episode 4's pipeline, pointed at the archive as it stood at a moment.

The point of a bitemporal corpus is that nothing downstream has to know about it. A slice of
`TimeCorpus` at a position is a list of units like any other, so `Pipeline` — BM25, the identifier
lookup, the standing filter, dense fusion, the reranker — runs on it unchanged. What differs is only
which units exist and what standing each carried *then*.

Two consequences worth stating, because they are the episode's argument.

**Scoring becomes revision-aware for free.** The slice holds only the states in force at the
position, so an address retrieved from it *is* the right revision. The whole problem the earlier
measurement exposed — the right place serving the wrong text, counted as a hit — cannot arise here,
because the wrong text is not in the collection.

**And the standing filter finally means what it says.** `095/tasks.html#T-1203` is `superseded`
today and the filter removes it; on 15 August 2026 no retirement had been recorded, so the slice
carries it as `current` and G-073's "was there still an open task" has an answer. A snapshot corpus
has one lifecycle per address. A state has one per interval.
"""

from __future__ import annotations

from dataclasses import replace

from ratchet import bm25 as B
from ratchet import corpus as C
from ratchet import idlookup as I
from ratchet import pipeline as P


def items_at(corpus, at: int) -> list:
    """The slice as `Item`s, so every existing component reads it without being told anything."""
    out: list[C.Item] = []
    for state in corpus.at(at):
        out.append(C.Item(len(out), state.source, state.anchor, state.spec, state.lifecycle,
                          state.title, state.path, state.heading, state.text,
                          part=state.part, parts=state.parts))
    return out


class Timeline:
    """One pipeline per position, built on demand and kept.

    Building a slice's index costs about 0.09 s, which is why materialising it beats every attempt
    to make one index serve every moment: `bm25()` cannot be told that only some rows are the
    collection, and a slice *is* a collection.
    """

    def __init__(self, corpus, reranker=None, embedder=None, scope_reranker=None):
        self.corpus = corpus
        self.reranker = reranker
        self.scope_reranker = scope_reranker
        self.embedder = embedder
        self._slices: dict[int, tuple] = {}

    def at(self, position: int, restrict: frozenset | None = None) -> tuple:
        """The slice in force at a position, optionally narrowed to a set of addresses.

        `restrict` is how a `what-changed` question gets answered. It names two points, and what it
        asks for is the *difference* between them — which no single passage states, so ranking over
        everything in force looks for a passage that does not exist. The bitemporal corpus knows
        something no snapshot can: **which units actually changed inside the window.** That set is
        1.1% to 9.2% of the archive on four of the five such questions, and it contains the expected
        address on all five.

        And it needs no new machinery, because it is the episode's own pattern once more: a subset
        is a collection, so it gets an index and statistics of its own and everything downstream
        reads it as an ordinary corpus.
        """
        key = (position, restrict)
        if key not in self._slices:
            items = items_at(self.corpus, position)
            if restrict is not None:
                items = [replace(it, n=i) for i, it in
                         enumerate(it for it in items if it.address in restrict)]
            index = P.Index(items, B.BM25(items), self._vectors(items))
            self._slices[key] = (items, P.Pipeline(index, None, I.IdIndex(items), None,
                                                   self.reranker, self.scope_reranker))
        return self._slices[key]

    def changed_between(self, lo: int, hi: int) -> frozenset:
        """The addresses whose text moved inside `(lo, hi]` — what "what changed" literally means."""
        return frozenset(s.address for s in self.corpus.states if lo < s.from_pos <= hi)

    def _vectors(self, items):
        if self.embedder is None:
            return None
        return self.embedder.embed_docs([it.passage() for it in items])

    def search(self, config, question: str, position: int, qvec=None, restrict=None) -> list:
        items, pipe = self.at(position, restrict)
        ranked, _top1, _pinned = pipe.run(config, question, qvec)
        return [items[n].address for n in ranked]
