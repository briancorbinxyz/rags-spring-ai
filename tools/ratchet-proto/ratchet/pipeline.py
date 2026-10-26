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
    doc_scope: bool = False  # Episode 6: narrow scope_rrf to the document kind the question named
    doc_scope_default: bool = False  # ...and treat a spec named with no document kind as its spec.html
    route_scope: bool = False  # Episode 6: rerank only when the question named no haystack
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
    def __init__(self, units: Index, chunks: Index | None, idindex, graph: Graph, reranker=None):
        self.u = units
        self.c = chunks
        self.idx = idindex
        self.g = graph
        self.reranker = reranker
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
        fused = rrf(lists) if len(lists) > 1 else lists[0]

        pinned_keys, pinned = [], []
        if cfg.ids and cfg.items == "units":
            pinned_keys = [k for k in self.idx.lookup(q, cfg.linker) if self._live_items(k, cfg.lifecycle)]
            for k in pinned_keys:
                pinned += [n for n in self._live_items(k, cfg.lifecycle) if n not in pinned]
        pinned_set = set(pinned)
        rest = [(n, s) for n, s in fused if n not in pinned_set]

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
        if cfg.redirect:
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
        ids, specs = I.parse(q, self.idx.anchors)
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
        ids, specs = I.parse(q, self.idx.anchors)
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
        return fused[:depth]

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
    DOC_KINDS = (
        (("task list", "tasks", "task"), "tasks.html"),
        (("design",), "design.html"),
        (("verification", "verify"), "verify.html"),
        (("triage log", "triage"), "triage-log.html"),
        (("plan",), "plan.html"),
        (("specification", "spec"), "spec.html"),
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
        for words, filename in self.DOC_KINDS:
            for word in words:
                if word == "spec" or word == "specification":
                    continue  # "spec 078" is the scope, not the document
                if word in lowered:
                    return filename
        return "spec.html" if cfg.doc_scope_default else None

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
