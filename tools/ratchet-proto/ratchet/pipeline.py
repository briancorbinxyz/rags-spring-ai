"""The retrieval configurations: Episode 4's main line, Episode 5's two graph steps, ablations.

One question goes through, in order:

1. candidate lists: BM25 and/or dense, each over items the lifecycle filter allows (a filter
   applied before the depth cut, as a store-side filter would be);
2. fusion: reciprocal rank fusion, sum of 1/(60 + rank), when there are two lists;
3. identifier lookup: pinned units go first, above the fusion, in pin order;
4. reranker: the cross-encoder reorders the first 50 unpinned items;
5. anchored typed expansion (E5-2): typed-link neighbours of the first pinned unit, inserted right
   after it;
6. supersession redirect (E5-1): a stale item is replaced by its current successor and dropped,
   applied history included -- a requirement rewritten several times leaves one archived proposal
   per rewrite, and none of them says which rewrite is the live one.

Every list is cut to 50 items.
"""

from __future__ import annotations

import collections
import re
from dataclasses import dataclass, replace

import numpy as np

from ratchet import bm25 as B
from ratchet import idlookup as I

from .corpus import EXCLUDED

DEPTH_SINGLE = 50
DEPTH_FUSED = 100  # each list's depth before fusion
RRF_K = 60
RERANK_TOP = 50
#: A spec runs to about a hundred units, so this admits essentially any one spec whole.
SCOPE_MAX = 200
EXPAND_MAX = 5
SIBLING_SEEDS = 5      # how many ranked items name a spec directory
SIBLING_MAX = 200      # how many sibling units may be handed to the reranker
TYPED_WEIGHTS = {"typed:validates": 2.0, "typed:cites": 2.0, "typed:delta": 2.0, "typed:target": 1.5, "typed:defer-to": 1.5}


@dataclass
class Config:
    key: str
    label: str
    items: str = "units"  # units | chunks
    bm25: bool = True
    dense: bool = False
    ids: bool = False
    lifecycle: bool = False
    rerank: bool = False
    expand: bool = False
    redirect: str | None = None  # None | "replace" (the rule) | "behind" (keeps applied history)
    rrf_neighbours: bool = False
    siblings: bool = False  # Episode 6: sibling artifacts of the best-ranked spec directories
    scope: bool = False  # Episode 6: the named spec's own units, when it named no id
    scope_rrf: bool = False  # Episode 6: rank inside the named spec and fuse, rather than admit
    rerank_rrf: bool = False  # Episode 6: the reranker as a vote fused with the ranking, not a replacement
    declared_names: bool = False  # Episode 6: resolve a spec named by the archive's own slug
    doc_scope: bool = False  # Episode 6: narrow scope_rrf to the document kind the question named
    doc_scope_default: bool = False  # ...and treat a spec named with no document kind as its spec.html
    route_scope: bool = False  # Episode 6: rerank only when the question named no haystack
    scope_rerank: int = 0  # Episode 6: rerank this many of the named scope's own ranking first
    scope_pin: int = 0  # Episode 6: pin what a reader picks out of the named scope, unfused
    admit_history: bool = False  # Episode 6: let a history question see what the filter hides
    history_keeps_superseded: bool = False  # Episode 6: ...and let the redirect leave it in place
    history_pin: int = 0  # Episode 6: let a reader choose among the units the filter hides
    reader_pin: int = 0  # Episode 6: let a chooser pick from the top of the ranking and pin it
    intent_admit: int = 0  # Episode 6: how many of each section kind an intent model names
    intent_pin: bool = False  # Episode 6: and those candidates go above the ranking
    kind_in_slice: bool = False  # Episode 6: a document kind resolves to a name the slice holds
    context_scope: bool = False  # Episode 6: a why-question's collection is the archive's #context
    #: Measured and rejected; kept so the result can be reproduced. Through the whole pipeline it
    #: moves G-080 from 21st to **23rd** and costs 92s against 2s. The reader ranked the answer
    #: first of its own pool of 62 and the fusion put it 23rd, which is the dilution lesson for the
    #: fifth time. See `section_ranking`.
    section_scope: int = 0
    corpus: str = "ep4"  # ep4 | legacy (earlier prototype's units: no heading path, no parts)
    dash_tokens: bool = False  # FTS5 tokenchars '-' (earlier prototype)
    linker: str = "ep4"  # ep4 | prior
    needs: tuple = ()  # "dense", "rerank"


def rrf(lists: list, k: int = RRF_K, weights=None) -> list:
    sc = collections.defaultdict(float)
    order = {}
    for li, L in enumerate(lists):
        w = 1.0 if weights is None else weights[li]
        for r, (n, _) in enumerate(L, 1):
            sc[n] += w / (k + r)
            order.setdefault(n, (li, r))
    return sorted(((n, s) for n, s in sc.items()), key=lambda x: (-x[1], order[x[0]]))


class Index:
    """Everything one item set (units or chunks) needs: BM25, vectors, keys."""

    def __init__(self, items, bm25, vectors=None):
        self.items = items
        self.bm25 = bm25
        self.vectors = vectors
        self.live = np.array([it.lifecycle not in EXCLUDED for it in items])

    def dense(self, qv, lifecycle: bool, depth: int) -> list:
        sims = self.vectors @ qv
        if lifecycle:
            sims = np.where(self.live, sims, -np.inf)
        top = np.argsort(-sims, kind="stable")[:depth]
        return [(int(n), float(sims[n])) for n in top if np.isfinite(sims[n])]


class Graph:
    def __init__(self, corpus, idindex):
        self.adj = collections.defaultdict(list)  # node -> [(node, weight, type)]
        for s, d, t in corpus.edges:
            s, d = tuple(s), tuple(d)
            w = TYPED_WEIGHTS.get(t, 1.0)
            self.adj[s].append((d, w, t))
            self.adj[d].append((s, w, t))
        self.supersede = corpus.supersede
        self.page_supersede = {k: v for k, v in corpus.page_supersede.items() if k != v}
        self.idx = idindex

    def typed_neighbours(self, key: str) -> list:
        out = []
        for (kind, d), w, t in self.adj[("u", key)]:
            if kind == "u" and t.startswith("typed:") and d not in out:
                out.append(d)
        return out

    def typed_pages(self, key: str) -> list:
        """Typed edges that land on a page rather than a unit.

        All 166 `defer-to` edges are of this shape: a slice defers a concern to another spec, not
        to one element of it. `typed_neighbours` keeps only unit destinations, so until these are
        resolved to a unit the relation is parsed, stored and then dropped on the floor.
        """
        out = []
        for (kind, d), w, t in self.adj[("u", key)]:
            if kind == "p" and t.startswith("typed:") and d not in out:
                out.append(d)
        return out


class Pipeline:
    def __init__(self, units: Index, chunks: Index | None, idindex, graph: Graph, reranker=None,
                 scope_reranker=None, intent=None):
        """`scope_reranker` reads the narrow, question-named case; `reranker` reads the open one.

        They are different jobs and the measurements said so twice. A listwise reader discriminates
        where a cross-encoder cannot — it is the only thing that found G-073's negation — and it is
        priced per candidate per query, about four and a half minutes over fifty candidates against
        ninety seconds over twenty. A cross-encoder is cheap over fifty and is what finds the
        questions that name no haystack at all, which the reader alone scores 5 of 9 on.

        So the narrow reader gets the window the question itself narrowed, and the cross-encoder
        keeps the open ranking. One reranker for both was the wrong shape, not the wrong model.
        """
        #: question text -> the section kinds an intent model said could hold the answer. The
        #: model is a fixed function of the question, pinned at temperature 0 and seed 42, so its
        #: answers are computed once and read from here: the run stays deterministic and a model
        #: that is down costs nothing but the arm.
        self.intent = intent or {}
        self._kind_index: dict[str, object] = {}
        self._has_file: set | None = None
        self.u = units
        self.c = chunks
        self.idx = idindex
        self.g = graph
        self.reranker = reranker
        self.scope_reranker = scope_reranker
        self._dirs = None

    # --- helpers ----------------------------------------------------------------------------
    def _by_dir(self):
        """Unit numbers by spec directory, built once: the artifact family's own index."""
        if self._dirs is None:
            self._dirs = collections.defaultdict(list)
            for n, it in enumerate(self.u.items):
                if it is not None and getattr(it, "source", None):
                    self._dirs[it.source.rsplit("/", 1)[0]].append(n)
        return self._dirs

    def _key(self, n):
        it = self.u.items[n]
        return f"{it.source}#{it.anchor}"

    def _live_items(self, key, lifecycle):
        ns = self.idx.items(key)
        return [n for n in ns if not lifecycle or self.u.items[n].lifecycle not in EXCLUDED]

    def _best_part(self, q, key):
        ns = self.idx.items(key)
        if len(ns) == 1:
            return ns[0]
        r = self.u.bm25.search(q, depth=len(ns), scope=ns)
        return r[0][0] if r else ns[0]

    def _best_in_page(self, q, page, cache):
        if (q, page) not in cache:
            scope = [it.n for it in self.u.items if it.source == page and it.lifecycle not in EXCLUDED]
            r = self.u.bm25.search(q, depth=1, scope=scope) if scope else []
            cache[(q, page)] = r[0][0] if r else None
        return cache[(q, page)]

    # --- one question ----------------------------------------------------------------------
    def run(self, cfg: Config, q: str, qv=None) -> tuple[list, float | None, int]:
        ix = self.u if cfg.items == "units" else self.c
        items = ix.items
        allow = (lambda n: items[n].lifecycle not in EXCLUDED) if cfg.lifecycle else None
        depth = DEPTH_FUSED if (cfg.bm25 and cfg.dense) else DEPTH_SINGLE
        lists = []
        if cfg.bm25:
            lists.append(ix.bm25.search(q, allow=allow, depth=depth))
        if cfg.dense:
            lists.append(ix.dense(qv, cfg.lifecycle, depth))
        if cfg.scope_rrf:
            inside = self.scope_ranking(q, qv, cfg, depth)
            if inside:
                lists.append(inside)
        if cfg.admit_history:
            left_behind = self.excluded_ranking(q, qv, cfg, depth)
            if left_behind:
                lists.append(left_behind)
        if cfg.context_scope:
            why = self.context_ranking(q, qv, cfg, depth)
            if why:
                lists.append(why)
        if cfg.intent_admit:
            admitted = self.intent_ranking(q, cfg, depth)
            if admitted:
                lists.append(admitted)
        if cfg.section_scope:
            sections = self.section_ranking(q, qv, cfg)
            if sections:
                lists.append(sections)
        fused = rrf(lists) if len(lists) > 1 else lists[0]

        pinned_keys, pinned = [], []
        if cfg.ids and cfg.items == "units":
            pinned_keys = [k for k in self.idx.lookup(q, cfg.linker) if self._live_items(k, cfg.lifecycle)]
            for k in pinned_keys:
                pinned += [n for n in self._live_items(k, cfg.lifecycle) if n not in pinned]
        if cfg.scope_pin:
            for n in self.scope_picks(q, qv, cfg):
                if n not in pinned:
                    pinned.append(n)
        if cfg.history_pin and self._history_asked(q):
            for n in self.history_picks(q, qv, cfg):
                if n not in pinned:
                    pinned.append(n)
        if cfg.intent_pin and cfg.intent_admit:
            # A candidate is pinned when its *own section kind* ranks it inside the depth the
            # measurement uses — the kind is a collection, so this is the collection's own
            # judgement of its own members, and `intent_admit` is that depth. The model only chose
            # which kinds were asked; it has no say in which member is confident.
            for n, _ in self.intent_ranking(q, cfg, depth):
                if n not in pinned:
                    pinned.append(n)
        pinned_set = set(pinned)
        rest = [(n, s) for n, s in fused if n not in pinned_set]
        if cfg.reader_pin:
            # The plainest form of the pin: a chooser reads the top of the ranking the pipeline
            # already produced and its picks go above it. It earns its place only where the
            # collection has been narrowed to something worth reading whole — a `what-changed`
            # question searching just the units that moved in its window is 109 to 441 of them —
            # and the selectivity rule in `_picks` still refuses an enumeration.
            for n in self._picks(q, rest[:cfg.reader_pin]):
                if n not in pinned:
                    pinned.append(n)
            pinned_set = set(pinned)
            rest = [(n, sc) for n, sc in rest if n not in pinned_set]

        extra = self.siblings(rest, pinned_set, cfg) if cfg.siblings else []
        if cfg.scope:
            extra = extra + self.scope_units(q, pinned_set, extra, cfg)

        rerank = cfg.rerank
        if rerank and cfg.route_scope and self._named_scope(q):
            # The question named its own haystack, so the scoped ranking is better evidence
            # than a cross-encoder's reading of the whole archive. Measured: reranking costs
            # G-063 (5th -> absent) and G-074 (1st -> 14th), both of which the scoped ranking
            # had right, and buys nothing on either. Where the question names nothing, the
            # cross-encoder is the only thing that finds the answer at all.
            rerank = False
        if rerank:
            head = rest[:RERANK_TOP] + [(n, 0.0) for n in extra]
            scores = self.reranker.scores(q, [items[n].passage() for n, _ in head])
            order = sorted(range(len(head)), key=lambda i: -scores[i])
            reranked = [(head[i][0], scores[i]) for i in order]
            if cfg.rerank_rrf:
                # The reranker as a second opinion rather than the last word. Replacing the ranking
                # outright costs as much as it buys: measured on the cutoff questions it wins G-062
                # and G-066 and loses G-063 and G-074, which had been 5th and 2nd. Fusing the two
                # orders keeps a unit that one of them likes and the other merely tolerates.
                rest = rrf([reranked, rest[:RERANK_TOP]]) + rest[RERANK_TOP:]
            else:
                rest = reranked + rest[RERANK_TOP:]
        elif extra:
            rest = rest + [(n, 0.0) for n in extra]

        top1 = rest[0][1] if rest else None
        final = (pinned + [n for n, _ in rest])[:50]

        if cfg.expand and pinned_keys:
            final = self.expand(final, pinned_keys[0], q)
        if cfg.rrf_neighbours:
            final = self.rrf_neighbours(final, q)
        if cfg.redirect and not (cfg.history_keeps_superseded and self._history_asked(q)):
            # The redirect is right about the present and wrong about the past. It replaces a
            # superseded record with what replaced it, which is what a reader asking "what does this
            # require?" wants and the opposite of what "what did it originally say?" wants. G-057's
            # D-005 is `current`, so the standing filter never touches it; the redirect is what
            # removes it, and no amount of admitting excluded units reaches it. Same trigger, same
            # rejectability: the record stays in place and the ranking may still bury it.
            final = self.redirect(final, q, cfg.redirect)
        return final[:50], top1, len(pinned)

    # --- Episode 6 --------------------------------------------------------------------------
    def scope_units(self, q, pinned_set, already, cfg):
        """The units of a spec the question named, when it named no id that pinned anything.

        The identifier lookup's rule is that "a spec with no id pins nothing", and it is right to
        refuse: pinning a whole spec above the ranking would bury a better answer elsewhere. But
        refusing to *pin* is not a reason to refuse to *look*. "As spec 095's task list stood on 15
        August" names its haystack exactly, and the unit that answers it shares almost no words with
        it, so the ranking never reaches the spec at all.

        So the spec's units join the rerank window and the cross-encoder decides — the same shape as
        `siblings`, and for the same reason. This is a hypothesis the reranker can reject, not a
        filter: if the named spec holds nothing relevant, nothing is promoted and the arm costs only
        the reading.

        Deliberately **not** ranked by BM25 before the reranker sees them. That was the trap the
        sibling arm fell into: pre-filtering the candidates with the same lexical signal that failed
        to find them is a guarantee of finding nothing new.
        """
        if cfg.items != "units":
            return []
        ids, specs = I.parse(q, self.idx.anchors,
                             self.idx.names if cfg.declared_names else None)
        if len(specs) != 1 or ids:
            return []
        seen = set(pinned_set) | set(already)
        # `I.spec_of` and not `Item.spec`: the item carries the whole slug
        # ("078-federated-resource-uri") and the question names the number, so the comparison has
        # to use the identifier lookup's own reading of which spec a unit is in.
        out = [n for n, it in enumerate(self.u.items)
               if I.spec_of(f"{it.source}#{it.anchor}") == specs[0]
               and n not in seen and self._admits(it, cfg)]
        return out[:SCOPE_MAX]

    def scope_ranking(self, q, qv, cfg, depth):
        """Rank only inside the spec the question named, and let RRF decide how much it is worth.

        `scope_units` admits the whole spec to the rerank window and depends on the cross-encoder to
        find the answer in it. Measured, the cross-encoder does not: it put G-063's unit 41st of 141
        and pushed G-074's from 3rd to 32nd. So this is the other way of using the same signal — the
        named spec gets *its own ranking*, and that ranking joins the fusion as a third list beside
        BM25 and dense. A unit that is first inside its spec and five-hundredth overall comes out
        high without being pinned, and a spec that holds nothing relevant contributes a list of
        irrelevant units that RRF buries. Rejectable, which a filter would not be.

        **Hybrid, with the scope's own BM25 statistics.** I first built this dense-only, reasoning
        that a cosine has no collection statistics so restricting it is exact, while restricting
        BM25 would reproduce the very error this episode is about. That was wrong, and wrong in an
        instructive way: it is only true of BM25 scored with *global* statistics over a filtered row
        set. An index built over the scope has the scope's statistics, which is correct for exactly
        the reason S4 is correct for a cutoff — a subset is a collection, and a collection has its
        own document frequencies. Measured, the lexical half is the stronger one here: spec-local
        BM25 puts G-063's unit 1st of 91 and G-074's 1st of 201, where dense alone managed 7th and
        2nd.
        """
        if cfg.items != "units" or qv is None or self.u.vectors is None:
            return []
        ids, specs = I.parse(q, self.idx.anchors,
                             self.idx.names if cfg.declared_names else None)
        if len(specs) != 1 or ids:
            return []
        inside = [n for n, it in enumerate(self.u.items)
                  if I.spec_of(f"{it.source}#{it.anchor}") == specs[0] and self._admits(it, cfg)]
        if cfg.doc_scope:
            kind = self._named_kind(q, cfg)
            if kind:
                inside = [n for n in inside if self.u.items[n].source.endswith("/" + kind)]
        if not inside:
            return []
        sims = self.u.vectors[inside] @ qv
        dense = [(inside[i], float(sims[i]))
                 for i in sorted(range(len(inside)), key=lambda i: -sims[i])]
        lexical = self._scope_bm25(inside).search(q, depth=len(inside))
        fused = rrf([[(inside[n], s) for n, s in lexical], dense])
        if cfg.scope_rerank and (self.scope_reranker or self.reranker) is not None:
            fused = self._reread(q, fused, cfg.scope_rerank)
        return fused[:depth]

    #: Words that say a question is asking about a record that has been left behind. The archive's
    #: own vocabulary first — `withdrawn`, `superseded`, `rejected` are the lifecycle's literal
    #: names — then the English a reader actually writes. Measured on the three question sets: this
    #: list fires on 9 of the 10 questions that need the excluded units, and also on 2 of the 3
    #: history-decoys and 5 main-set questions, which is precisely why what follows is a candidate
    #: list and not a filter flip.
    HISTORY_MARKERS = (
        "withdrawn", "withdraw", "superseded", "supersede", "rejected", "reject", "retired",
        "abandoned", "dropped", "shelved", "set aside", "originally", "original", "first",
        "earlier", "used to", "before", "previously", "replaced", "no longer", "former",
    )

    def _history_asked(self, q: str) -> bool:
        lowered = q.lower()
        return any(re.search(r"\b" + marker.replace(" ", r"\s+") + r"\b", lowered)
                   for marker in self.HISTORY_MARKERS)

    #: Section kinds worth a collection of their own: the archive has 31 with forty or more units.
    SECTION_MIN = 40

    def section_ranking(self, q, qv, cfg):
        """The best few of every section kind, put in order by a reader that chooses.

        The per-kind collections are each very sharp — `#changelog` ranks G-051's answer 2nd of 596,
        `#risks` ranks G-058's 1st of 415, `#context` ranks G-080's 1st of 129 — and the problem was
        never finding the unit inside its kind. It was knowing *which kind*. Two trigger-free ways of
        avoiding that question both failed: one combined collection of 4,926 sections is blunter
        than any of them, and one fused list per kind is worse still, because 31 lists each offer a
        confident rank-1 and RRF cannot tell them apart, so the answer lands in a tie-tier near 9th.

        A reader can tell them apart, which is the one thing RRF could not do. Offered the best two
        of each kind it ranked G-080's `#context` **first of 62**, where RRF had it 9th and the
        combined collection 41st. It is not a cure — G-058's `#risks` came 6th — but it needs no
        trigger, and a trigger per section kind was the thing worth avoiding.

        **And it is rejected, on two counts.** Through the whole pipeline it moves G-080 from 21st
        to **23rd**: the reader had the answer first of its own pool and the fusion put it 23rd,
        which is the same dilution that killed `T9`, `TG`, `TH` and `reader_pin`. And it costs **92
        seconds for one question** against 2 — about an hour for a 53-question configuration,
        against a minute for the whole `E5-2` line. Either count would be enough.

        Kept rather than deleted because the contrast is the useful part: `context_scope` is two
        small index builds, no model call at all, and it won two questions.

        Its **order** joins the fusion, and its picks are not pinned. For G-080 it chose 27 of 62,
        which the selectivity rule in `_picks` accepts by a hair and which would be a catastrophic
        pin: `scope_pin` lost the 5090 main set by pinning 15 to 20. A chooser's ordering is useful
        here and its choosing is not.
        """
        reader = self.scope_reranker or self.reranker
        if cfg.items != "units" or qv is None or self.u.vectors is None:
            return []
        if reader is None or not getattr(reader, "selects", False):
            return []
        pool = []
        for members in self._section_kinds(cfg):
            lexical = self._scope_bm25(members).search(q, depth=len(members))
            ranked = [(members[n], sc) for n, sc in lexical]
            sims = self.u.vectors[members] @ qv
            dense = [(members[i], float(sims[i]))
                     for i in sorted(range(len(members)), key=lambda i: -sims[i])]
            pool += [n for n, _ in rrf([ranked, dense])[:cfg.section_scope]]
        # The reader reads a window, and a pool longer than it would be silently truncated.
        pool = pool[:getattr(reader, "window", len(pool))]
        if not pool:
            return []
        scores = reader.scores(q, [self.u.items[n].passage() for n in pool])
        order = sorted(range(len(pool)), key=lambda i: -scores[i])
        return [(pool[i], float(len(pool) - r)) for r, i in enumerate(order)]

    def _section_kinds(self, cfg):
        """Unit numbers grouped by section anchor, for anchors the archive uses often. Built once."""
        if getattr(self, "_sections", None) is None:
            by = collections.defaultdict(list)
            for n, it in enumerate(self.u.items):
                if it.anchor and not any(ch.isdigit() for ch in it.anchor):
                    by[it.anchor].append(n)
            self._sections = [v for v in by.values() if len(v) >= self.SECTION_MIN]
        return [[n for n in members if self._admits(self.u.items[n], cfg)]
                for members in self._sections]

    #: A question shaped like "why does this exist". Poor precision on purpose: it fires on 19
    #: questions across the three sets and only 4 of them want a `#context`, which is why what
    #: follows is a fused list the ranking may ignore rather than anything that commits.
    WHY = (r"\bwhy\b", r"what problem", r"what led", r"what prompted")

    def _why_asked(self, q: str) -> bool:
        lowered = q.lower()
        return any(re.search(pattern, lowered) for pattern in self.WHY)

    def context_ranking(self, q, qv, cfg, depth):
        """The archive's `#context` sections, ranked among themselves.

        All five of the main set's `intent` questions expect a `#context` unit — 5 of 5, and not by
        accident: spectastic's schema puts the problem a spec exists to solve in `#context`, so a
        question asking *why* something exists is asking for that section by name without naming it.
        The same holds for `intent`, `risks` and `changelog` in other strata, which is the section
        seam the plan has carried since the typed-expansion work: expansion anchors only on element
        ids, and the sections are where a good deal of the meaning lives.

        Measured over the 129 in-force `#context` units, the expected one ranks **first** for
        G-077, G-079, G-080 and G-081 and sixth for G-078. G-080 is the 5090 main line's only miss
        and G-081 the laptop's, and neither has any other structural signal — no id, no named spec,
        no typed edge. A collection of 129 is what gives them one.

        Fused, never pinned. The trigger is a `why` and most `why` questions want something else
        entirely, so this has to be a suggestion the ranking can decline.
        """
        if cfg.items != "units" or not self._why_asked(q):
            return []
        sections = [n for n, it in enumerate(self.u.items)
                    if it.anchor == "context" and self._admits(it, cfg)]
        if not sections:
            return []
        lexical = self._scope_bm25(sections).search(q, depth=len(sections))
        ranked = [(sections[n], sc) for n, sc in lexical]
        if qv is not None and self.u.vectors is not None:
            sims = self.u.vectors[sections] @ qv
            dense = [(sections[i], float(sims[i]))
                     for i in sorted(range(len(sections)), key=lambda i: -sims[i])]
            ranked = rrf([ranked, dense])
        return ranked[:depth]

    def intent_ranking(self, q, cfg, depth):
        """The best few of each section kind an intent model named, offered to the fusion.

        Every earlier attempt at the section seam asked a *chooser* for one kind, and all of them
        land near chance: RRF over 31 kinds puts the answer about 9th, a reader 6th at 92 seconds a
        question, the `WHY` list is 21% precise, and each kind's own score margin picks the right
        kind 2 times in 18 against a 4% floor. The shape was the problem. Choosing is a precision
        job and nothing here is precise.

        Admitting is a recall job, and a recall job may be wrong cheaply. The per-kind collections
        are already sharp — `#risks` holds G-058's answer first of 415 — so the model only has to
        make sure the right kind's best candidates are *in the pool*, and the fusion and the
        reranker decide from there. Measured over the 18 questions whose evidence is a named
        section, the kind-recall of the named set is 4/18 for today's `WHY` list, 7/18 for a
        repaired list of the same shape, and 11/18 for `qwen3:8b` naming a mean of 5.2 kinds.

        Admitting all 25 kinds has perfect recall and was already rejected, so recall alone is not
        the mechanism: what matters is recall *per candidate admitted*. This arm offers
        `intent_admit` from each named kind, round-robin, so no score is ever compared across two
        collections with different statistics — the kinds take turns, which is a rank and not a
        number.
        """
        named = self.intent.get(q) or ()
        if cfg.items != "units" or not named:
            return []
        per_kind = []
        for kind in sorted(named):
            if kind not in self._kind_index:
                members = [n for n, it in enumerate(self.u.items) if it.anchor == kind]
                # Renumbered, as `_scope_bm25` does: a kind is a collection, so the index it gets
                # is its own and `search` answers in its own numbering.
                local = [replace(self.u.items[n], n=i) for i, n in enumerate(members)]
                self._kind_index[kind] = (members, B.BM25(local) if members else None)
            members, index = self._kind_index[kind]
            if not index:
                continue
            best = index.search(q, depth=cfg.intent_admit)
            per_kind.append([members[n] for n, _ in best])
        out, rank = [], 0
        while any(per_kind):                      # round-robin: each kind takes a turn
            for picks in per_kind:
                if picks:
                    n = picks.pop(0)
                    if not self._admits(self.u.items[n], cfg):
                        continue
                    if n not in [m for m, _ in out]:
                        out.append((n, 1.0 / (rank + 1)))
                        rank += 1
        return out[:depth]

    def excluded_ranking(self, q, qv, cfg, depth):
        """The units the standing filter hides, ranked among themselves and offered to the fusion.

        The filter exists because a search that cannot tell history from current text answers from
        the past with confidence, and it has earned its place — Episode 4 measured it. But three
        strata of questions ask *about* what it hides, and for them the filter is not a safeguard,
        it is the reason the answer cannot be found at any depth. Six questions across the held-out
        and time sets expect a unit that is `withdrawn`, `superseded` or `rejected`, and a withdrawn
        proposal was *never* anything else, so no slice of history reaches it either.

        **A candidate list, not a switch.** `admit_history` does not unfilter the search; it builds
        a *separate* ranking over the excluded units and fuses it in, so the reranker and the
        fusion can refuse it. That matters because the trigger is lexical and two of the three
        history-decoys carry a marker while needing current text — G-060 says "no longer", G-061
        says "originally" — and the golden set calls G-061 an expected misroute on purpose. A filter
        flip would answer the decoys wrongly with the same confidence it answers the history
        questions rightly.

        **With their own statistics**, as everything else in this episode: 196 excluded units are a
        collection, and a collection has its own document frequencies. Scoring them with the whole
        archive's would repeat the mistake the as-of measurement exposed.
        """
        if cfg.items != "units" or not self._history_asked(q):
            return []
        hidden = [n for n, it in enumerate(self.u.items) if it.lifecycle in EXCLUDED]
        if not hidden:
            return []
        lexical = self._scope_bm25(hidden).search(q, depth=len(hidden))
        ranked = [(hidden[n], s) for n, s in lexical]
        if qv is not None and self.u.vectors is not None:
            sims = self.u.vectors[hidden] @ qv
            dense = [(hidden[i], float(sims[i]))
                     for i in sorted(range(len(hidden)), key=lambda i: -sims[i])]
            ranked = rrf([ranked, dense])
        return ranked[:depth]

    def history_picks(self, q, qv, cfg) -> list:
        """What a reader chooses among the units the standing filter hides.

        `admit_history` makes them reachable and the ranking then buries them: measured on the time
        set, G-054 lands 2nd but G-052 11th, G-057 11th, G-056 21st and G-055 39th. Reachable and
        unread is the sibling arm's lesson a third time, and the answer is the one that worked for
        the named scope — let a reader that *chooses* pick from the small population, and pin what
        it picks, under the same rule that a selection covering most of what it read is an
        enumeration and worth nothing.

        The population here is 196 units out of 11,492, and a history question is asking about
        exactly those. That is a far narrower bet than pinning inside a named spec, which is what
        failed the main set at 46/47 — and the trigger fires on only five main-set questions.
        """
        if cfg.items != "units" or qv is None:
            return []
        ranked = self.excluded_ranking(q, qv, cfg, cfg.history_pin)
        return self._picks(q, ranked)

    def scope_picks(self, q, qv, cfg) -> list:
        """What a reader picks out of the window the question named, pinned rather than fused.

        Fusing a reader's judgement throws it away, and that has now been measured twice. As an RRF
        vote beside the ranking it put everything at 7th or 8th and committed to nothing (`T9`); as
        a third list inside the scope ranking it moved G-073 from 28th to 16th and no further,
        because global BM25 and dense outvoted it two to one (`TH`). A reader that can tell "leave
        it untouched" from "add it" is the only thing here that can answer the question, so its
        answer has to survive the pipeline.

        The identifier lookup already has the shape for this: an exact match is **pinned** above the
        ranking and never reranked. A named scope is weaker evidence than a named id, which is why
        the lookup refuses to pin a whole spec — but the reader's *selection* inside that spec is
        not a whole spec. A listwise call returns the few identifiers it considers useful (three of
        twenty, in the probe), so the set is chosen by the reader rather than by a `k` I picked, and
        that is what gets pinned.
        """
        if cfg.items != "units" or qv is None:
            return []
        reader = self.scope_reranker or self.reranker
        if reader is None or not getattr(reader, "selects", False):
            # Pinning is for a reader that *chooses*. A cross-encoder scores every candidate on its
            # own scale — MiniLM's logits run negative — so reading a choice out of the sign of a
            # score would pin an arbitrary subset and call it a judgement. Such a reader can still
            # reorder (`scope_rerank`); it cannot pin.
            return []
        ranked = self.scope_ranking(q, qv, replace(cfg, scope_rerank=0), cfg.scope_pin)
        if not ranked:
            return []
        return self._picks(q, ranked)

    def _picks(self, q, ranked) -> list:
        """A reader's choice out of a candidate list, or nothing if it did not really choose."""
        reader = self.scope_reranker or self.reranker
        if not ranked or reader is None or not getattr(reader, "selects", False):
            return []
        scores = reader.scores(q, [self.u.items[n].passage() for n, _ in ranked])
        picked = [(ranked[i][0], scores[i]) for i in range(len(ranked)) if scores[i] > 0]
        if len(picked) * 2 >= len(ranked):
            # A selection that includes most of what it read is an *enumeration*, not a judgement,
            # and carries no discriminating information — so pinning it pins the whole scope, which
            # is the thing the identifier lookup rightly refuses to do. Measured: on the nine cutoff
            # questions the reader picks 3 of 20 and pinning it answers them; on G-031, G-032 and
            # G-044 it picks 15, 20 and 19 of 20 and pinning it buries answers that were 4th, 1st
            # and 1st. The answer was inside the named scope every time, so the failure is the
            # reader's selectivity and not the seam.
            return []
        picked.sort(key=lambda row: -row[1])
        return [n for n, _ in picked]

    def _reread(self, q, ranked, window):
        """Let the reranker read the named scope's ranking before it is fused with the rest.

        A listwise reader has to hold every candidate at once, and its judgement degrades as the
        list grows — the long-context "lost in the middle" effect. The named scope is the one place
        where a short, high-quality list is available: the question said to look there, so the
        reader gets the scope's own top few rather than fifty candidates drawn from the whole
        archive. Reranking here and fusing afterwards keeps the rest of the ranking honest.
        """
        reader = self.scope_reranker or self.reranker
        head = ranked[:window]
        scores = reader.scores(q, [self.u.items[n].passage() for n, _ in head])
        order = sorted(range(len(head)), key=lambda i: -scores[i])
        return [(head[i][0], scores[i]) for i in order] + ranked[window:]

    def _scope_bm25(self, inside):
        """An FTS5 index over the scope alone, so its statistics are the scope's. Kept per scope."""
        key = (id(self.u.items), tuple(inside[:1]), len(inside))
        cached = getattr(self, "_scope_indexes", None)
        if cached is None:
            cached = self._scope_indexes = {}
        if key not in cached:
            local = [replace(self.u.items[n], n=i) for i, n in enumerate(inside)]
            cached[key] = B.BM25(local)
        return cached[key]

    def _named_scope(self, q: str) -> bool:
        """Whether the question said where to look: exactly one spec, and no id that would pin.

        This is the router's one decision, and it is the weakest thing in the episode: the rule was
        chosen while looking at these nine questions, so it needs the held-out and time sets to say
        whether it generalises. The *rationale* is prior to the data -- a question that names its
        haystack has given better evidence than a reranker's reading of the whole archive -- but a
        rationale is not a measurement.
        """
        ids, specs = I.parse(q, self.idx.anchors)
        return len(specs) == 1 and not ids

    #: The documents a spectastic spec is made of, and how a question refers to each. The archive
    #: has a fixed, small set of these, so naming them is adapter knowledge rather than a guess.
    #: The documents a spectastic spec is made of, how a question refers to each, and the
    #: filenames that document has *gone by* — newest name first.
    #:
    #: The last part is the correction. The archive's set of documents is not fixed over time:
    #: `plan.html` became `design.html` on 30 July 2026, across 321 files in one commit. A table of
    #: constants cannot say that, and measured over the three question sets the two rows it forced
    #: are the only two that have never once been right — `plan` fires three times and wants
    #: `plan.html` never, because no such page exists today, while the one question that does want
    #: it (G-072, asking as of 1 July) asks for it by the name it has *now*.
    #:
    #: So a kind resolves against the collection rather than against a constant: the first name it
    #: has gone by that is actually present. In a current corpus "plan" and "design" both reach
    #: `design.html`; in a slice taken before the rename both reach `plan.html`. No rename detector
    #: is needed and none is used — git's is a similarity heuristic with a threshold, and 42 of its
    #: 178 renames here depend on where that threshold is set. The slice is a fact.
    DOC_KINDS = (
        (("task list", "tasks", "task"), ("tasks.html",)),
        (("design", "plan"), ("design.html", "plan.html")),
        (("verification", "verify"), ("verify.html",)),
        (("triage log", "triage"), ("triage-log.html",)),
        (("specification", "spec"), ("spec.html",)),
    )

    def _named_kind(self, q: str, cfg) -> str | None:
        """Which of a spec's documents the question asked about, if it said.

        "spec 095's task list" and "spec 105's design" name the document as well as the spec, which
        narrows the haystack from a couple of hundred units to a few dozen — and a few dozen is
        small enough to rank in. The words are matched after the spec is named, so "spec 078" does
        not read as a request for spec.html.

        `doc_scope_default` additionally treats a spec named with no document kind as a reference to
        its `spec.html`. It is **off by default and reported separately**, because I chose it
        knowing it would help G-063, and nine questions cannot tell a sound prior from a fitted one.
        """
        lowered = q.lower()
        for words, names in self.DOC_KINDS:
            for word in words:
                if word == "spec" or word == "specification":
                    continue  # "spec 078" is the scope, not the document
                if word in lowered:
                    return self._present(names, cfg)
        return self._present(("spec.html",), cfg) if cfg.doc_scope_default else None

    def _present(self, names, cfg):
        """The first name a document has gone by that this collection actually holds.

        Without `kind_in_slice` the newest name is returned whether or not it is there, which is
        what every configuration measured before this did, so their numbers are unchanged.
        """
        if not cfg.kind_in_slice:
            return names[0]
        if self._has_file is None:
            self._has_file = {it.source.rsplit("/", 1)[-1] for it in self.u.items}
        for name in names:
            if name in self._has_file:
                return name
        return names[0]

    def _admits(self, it, cfg):
        return not cfg.lifecycle or it.lifecycle not in EXCLUDED


    def siblings(self, rest, pinned_set, cfg):
        """Units from the sibling artifacts of the best-ranked spec directories.

        A spec's spec.html, design.html, tasks.html and verify.html belong together whether or
        not anything links them: the artifact family says so, the way a 13D/A is an amendment to
        a 13D without saying it in prose. Only verify->spec is a declared MUST; the rest is
        convention, legible in the link direction (tasks->spec 1413, design->spec 596, against
        spec->design 2), so this is an inferred relation and is named one.

        The candidates are handed to the reranker unfiltered, which is the point. Choosing among
        them by BM25 first reproduces the miss the edge exists to fix: the wanted unit is the one
        the lexical matcher does not see. The cap is on how many may be scored, never on which.
        """
        if cfg.items != "units":
            return []
        by_dir = self._by_dir()
        seen_pages, dirs = set(), []
        for n, _ in rest[:SIBLING_SEEDS]:
            source = self.u.items[n].source
            seen_pages.add(source)
            d = source.rsplit("/", 1)[0]
            if d not in dirs:
                dirs.append(d)
        out = []
        for d in dirs:
            for n in by_dir.get(d, ()):
                it = self.u.items[n]
                if it.source in seen_pages or n in pinned_set:
                    continue
                if cfg.lifecycle and not self._live_items(self._key(n), True):
                    continue
                out.append(n)
                if len(out) >= SIBLING_MAX:
                    return out
        return out

    # --- Episode 5 --------------------------------------------------------------------------
    def expand(self, final, anchor_key, q):
        """E5-2: typed-link neighbours of the anchor (validates, cites, target incl. spec-delta,
        defer-to resolved to a unit), one hop, at most 5 units, ranked by BM25 among themselves,
        inserted right after the anchor's own item(s).

        `defer-to` lands on a page, never a unit, so it was silently dropped until the page
        resolution below was added; the docstring had promised it since Episode 5.
        """
        cand = [k for k in self.g.typed_neighbours(anchor_key) if self._live_items(k, True)]
        # A typed edge onto a page becomes the page's best unit for this question, as the
        # supersession redirect already resolves a superseded page.
        cache = {}
        for page in self.g.typed_pages(anchor_key):
            b = self._best_in_page(q, page, cache)
            if b is not None:
                k = self._key(b)
                if k not in cand and self._live_items(k, True):
                    cand.append(k)
        if not cand:
            return final
        scope = [n for k in cand for n in self._live_items(k, True)]
        ranked = [n for n, _ in self.u.bm25.search(q, depth=len(scope), scope=scope)]
        units_in_order, best = [], {}
        for n in ranked:
            k = self._key(n)
            if k not in best:
                best[k] = n
                units_in_order.append(k)
        for k in cand:
            if k not in best:
                best[k] = self._live_items(k, True)[0]
                units_in_order.append(k)
        nb = [best[k] for k in units_in_order[:EXPAND_MAX]]
        anchor_items = [n for n in final if self._key(n) == anchor_key] or self._live_items(anchor_key, True)
        head = anchor_items + [n for n in nb if n not in anchor_items]
        return head + [n for n in final if n not in head]

    def redirect(self, final, q, mode):
        """E5-1: replace each stale item by the current element that superseded it (spec-delta
        targets, in-place 'superseded' status with a link, prose 'superseded by'), following up to
        three hops; a superseded page maps by the same anchor, else its best BM25 unit. The item
        itself is dropped. ``mode="behind"`` is Episode 4's first rule, which kept an
        applied-history item right behind its successor: see Episode 5's epilogue for why it went."""
        out = []
        cache = {}
        for n in final:
            it = self.u.items[n]
            key = j = self._key(n)
            for _ in range(3):
                if j in self.g.supersede:
                    j = self.g.supersede[j]
                elif j.split("#")[0] in self.g.page_supersede:
                    tp = self.g.page_supersede[j.split("#")[0]]
                    cand = f"{tp}#{j.split('#')[1]}"
                    if self.idx.items(cand):
                        j = cand
                    else:
                        b = self._best_in_page(q, tp, cache)
                        if b is None:
                            break
                        j = self._key(b)
                else:
                    break
            if j == key:
                if n not in out:
                    out.append(n)
                continue
            m = self._best_part(q, j)
            if m not in out:
                out.append(m)
            if mode == "behind" and it.lifecycle == "history" and n not in out:
                out.append(n)
        return out

    def rrf_neighbours(self, final, q, seeds_n=5, add=5, gw=0.5, k=RRF_K):
        """Epilogue (measured worse before): neighbours of the first five items over all link
        types, scored by link weight / seed rank, RRF-merged with the list (k=60, weight 0.5);
        the seeds themselves are not in the graph list."""
        cache = {}
        seeds = final[:seeds_n]
        seed_keys = {self._key(s) for s in seeds}
        sc = collections.defaultdict(float)
        for r, s in enumerate(seeds):
            it = self.u.items[s]
            for node, f in ((("u", self._key(s)), 1.0), (("p", it.source), 0.5)):
                for (kind, d), w, t in self.g.adj[node]:
                    if kind == "u":
                        if not self._live_items(d, True):
                            continue
                        uk, ww = d, w
                    else:
                        b = self._best_in_page(q, d, cache)
                        if b is None:
                            continue
                        uk, ww = self._key(b), w * 0.5
                    if uk in seed_keys:
                        continue
                    sc[uk] += ww * f / (r + 1)
        graph = [self._best_part(q, uk) for uk, _ in sorted(sc.items(), key=lambda x: -x[1])[:add]]
        merged = rrf([[(n, 0) for n in final], [(n, 0) for n in graph]], k=k, weights=[1.0, gw])
        return [n for n, _ in merged][:50]
