"""Six ways to make FTS5 bitemporal, measured against the one that is correct by construction.

The question is not whether a bitemporal table is possible — it is, and `timecorpus` builds it. The
question is what happens to BM25, whose statistics (collection size, average length, document
frequency) are properties of the collection *at a point in time*. Put every state in one FTS5 table
and `bm25()` scores against statistics that include the future.

Ground truth is S4: an FTS5 index holding exactly the states in force at the cutoff, scored by
FTS5's own `bm25()`. That is not an approximation of the right answer, it is the right answer, and
the only reason not to ship it is cost. Everything else is measured against it.
"""

from __future__ import annotations

import math
import pickle
import re
import sqlite3
import sys
import time
from collections import Counter, defaultdict

sys.path.insert(0, "/private/tmp/claude-501/-Users-briancorbin-Code-rags-rags-spring-ai/4f24c815-84f0-4ab3-aafb-a3f513e16c8c/scratchpad/proto6/tools/ratchet-proto")

from ratchet import bm25 as B
from ratchet import timecorpus as TC

WEIGHTS = {"title": 5.0, "ident": 10.0, "body": 1.0}
K1, BPARAM = 1.2, 0.75
WORD = re.compile(r"[A-Za-z0-9][A-Za-z0-9\-]*")


def columns(state) -> dict:
    return {"title": " ".join(x for x in (state.path or state.title, state.heading) if x),
            "ident": state.anchor,
            "body": state.text}


def terms(text: str) -> list[str]:
    return [t.lower() for t in WORD.findall(text)]


# --- FTS5-backed spikes ------------------------------------------------------------------------

class Fts5Index:
    """One FTS5 table over a given set of states, with the interval as UNINDEXED columns."""

    def __init__(self, states, name="f"):
        self.states = list(states)
        self.db = sqlite3.connect(":memory:")
        self.db.execute(f"create virtual table {name} using fts5("
                        f"title, ident, body, vfrom UNINDEXED, vto UNINDEXED, tokenize='unicode61')")
        self.db.executemany(
            f"insert into {name}(rowid,title,ident,body,vfrom,vto) values (?,?,?,?,?,?)",
            [(i, *(columns(s)[c] for c in ("title", "ident", "body")), s.from_pos, s.to_pos)
             for i, s in enumerate(self.states)])
        self.db.commit()
        self.name = name

    def search(self, query, at=None, depth=10, filter_interval=True):
        match = B.fts_query(query)
        if not match:
            return []
        w = (WEIGHTS["title"], WEIGHTS["ident"], WEIGHTS["body"])
        sql = (f"select rowid, bm25({self.name},{w[0]},{w[1]},{w[2]}) s "
               f"from {self.name} where {self.name} match ?")
        args = [match]
        if filter_interval and at is not None:
            sql += " and vfrom<=? and vto>?"
            args += [at, at]
        sql += " order by s, rowid limit ?"
        args.append(depth)
        return [(self.states[r].address, -s) for r, s in self.db.execute(sql, args)]

    def size(self):
        return self.db.execute(
            "select sum(pgsize) from dbstat where name like ?", (f"{self.name}%",)).fetchone()[0] or 0


class S1GlobalPostfilter:
    """One index over every state. Filter by interval, score with whole-table statistics."""
    label = "S1  one index, post-filter, FTS5 bm25 (global stats)"

    def __init__(self, corpus):
        self.index = Fts5Index(corpus.states)

    def search(self, query, at, depth=10):
        return self.index.search(query, at, depth)


class S3Partitioned:
    """One index per slice of history; a state joins every slice its interval overlaps.

    The literature's staircase/sharding idea at the smallest useful scale. Statistics are those of
    the slice, so they are wrong by less than global ones and the index is bigger by the overlap.
    """
    label = "S3  partitioned by epoch, FTS5 bm25 (slice stats)"

    def __init__(self, corpus, slices=8):
        last = max(corpus.positions.values())
        edges = [round(i * (last + 1) / slices) for i in range(slices + 1)]
        self.ranges = list(zip(edges, edges[1:]))
        self.parts = []
        for lo, hi in self.ranges:
            members = [s for s in corpus.states if s.from_pos < hi and s.to_pos > lo]
            self.parts.append(Fts5Index(members, name="f"))
        self.rows = sum(len(p.states) for p in self.parts)

    def search(self, query, at, depth=10):
        for (lo, hi), part in zip(self.ranges, self.parts):
            if lo <= at < hi:
                return part.search(query, at, depth)
        return self.parts[-1].search(query, at, depth)


class S4PerCutoff:
    """An index holding exactly what was in force. Correct by construction; the ground truth."""
    label = "S4  index per cutoff, exactly the live rows (GROUND TRUTH)"

    def __init__(self, corpus):
        self.corpus = corpus
        self.cache = {}
        self.build_times = []

    def index_at(self, at):
        if at not in self.cache:
            t0 = time.time()
            self.cache[at] = Fts5Index(self.corpus.at(at))
            self.build_times.append(time.time() - t0)
        return self.cache[at]

    def search(self, query, at, depth=10):
        return self.index_at(at).search(query, at, depth, filter_interval=False)


class S6TwoPath:
    """Keep the production index for now, add a second one for everything retired.

    The cheap option: nothing about the current line changes, and a time query unions the two. Each
    table's statistics are its own, so the live table's are right for 'now' and wrong for any cutoff.
    """
    label = "S6  current index + retired index, union (per-table stats)"

    def __init__(self, corpus):
        self.live = Fts5Index([s for s in corpus.states if s.live])
        self.dead = Fts5Index([s for s in corpus.states if not s.live])

    def search(self, query, at, depth=10):
        both = self.live.search(query, at, depth) + self.dead.search(query, at, depth)
        both.sort(key=lambda r: -r[1])
        return both[:depth]


# --- our own BM25, so the statistics can be whatever we say they are ---------------------------

class OwnBm25:
    """BM25 over the state list, with the collection statistics taken at the cutoff.

    This is what FTS5 will not do: `bm25()` has no way to be told that only a subset of rows is the
    collection. Here `N`, the average length and every `df` are computed over exactly the states in
    force, which is the definition the as-of question needs.

    S2 reads those statistics out of one FTS5 index through `fts5vocab`; S5 keeps its own postings.
    The two should agree exactly, and the point of having both is that they are independent.
    """

    def __init__(self, label):
        self.label = label

    def score(self, query, at, depth, n_docs, avg_len, df, postings, length, address):
        qterms = [t.lower() for t in B.fts_query(query).replace('"', "").split(" OR ") if t]
        scores = defaultdict(float)
        for term in qterms:
            n = df(term, at)
            if not n:
                continue
            idf = math.log(1 + (n_docs - n + 0.5) / (n + 0.5))
            for doc, col, freq in postings(term, at):
                dl = length(doc, col)
                al = avg_len(col, at) or 1
                denom = freq + K1 * (1 - BPARAM + BPARAM * dl / al)
                scores[doc] += WEIGHTS[col] * idf * (freq * (K1 + 1)) / (denom or 1)
        best = sorted(scores.items(), key=lambda kv: (-kv[1], kv[0]))[:depth]
        return [(address(d), s) for d, s in best]


class S5OwnPostings(OwnBm25):
    label = "S5  our own BM25, own postings, cutoff-exact stats (no FTS5)"

    def __init__(self, corpus):
        super().__init__(self.label)
        self.states = corpus.states
        self.post = defaultdict(list)      # term -> [(doc, col, freq)]
        self.len = {}                      # (doc, col) -> tokens
        self.live_by_pos = []
        for i, s in enumerate(self.states):
            for col, text in columns(s).items():
                toks = terms(text)
                self.len[(i, col)] = len(toks)
                for term, freq in Counter(toks).items():
                    self.post[term].append((i, col, freq))

    def _live(self, at):
        return {i for i, s in enumerate(self.states) if s.covers(at)}

    def search(self, query, at, depth=10):
        live = self._live(at)
        def df(term, _):
            return len({d for d, c, f in self.post.get(term, []) if d in live})
        def postings(term, _):
            return [(d, c, f) for d, c, f in self.post.get(term, []) if d in live]
        def avg_len(col, _):
            vals = [self.len[(d, col)] for d in live]
            return sum(vals) / len(vals) if vals else 1
        self._avg = {}
        return self.score(query, at, depth, len(live), avg_len, df, postings,
                          lambda d, c: self.len[(d, c)], lambda d: self.states[d].address)


class S2VocabExact(OwnBm25):
    """One FTS5 index, and the statistics recovered from it per cutoff through `fts5vocab`.

    `fts5vocab(f, instance)` exposes one row per term occurrence — (term, doc, col, offset) — so
    joining it against the live row set gives an exact `df` and exact lengths for any point in time,
    from a single index. This is the interesting one: it keeps FTS5 doing the matching and the
    storage, and takes back only the arithmetic that FTS5 cannot parameterise.
    """
    label = "S2  one index + fts5vocab(instance), cutoff-exact stats"

    def __init__(self, corpus):
        super().__init__(self.label)
        self.index = Fts5Index(corpus.states)
        self.states = self.index.states
        db = self.index.db
        db.execute("create virtual table vi using fts5vocab(f, instance)")
        # one pass over the index to recover postings and lengths, as a store would do at build time
        self.post = defaultdict(list)
        counts = Counter()
        self.len = Counter()
        for term, doc, col, _off in db.execute("select term, doc, col, offset from vi"):
            counts[(term, doc, col)] += 1
            self.len[(doc, col)] += 1
        for (term, doc, col), freq in counts.items():
            self.post[term].append((doc, col, freq))

    def _live(self, at):
        return {i for i, s in enumerate(self.states) if s.covers(at)}

    def search(self, query, at, depth=10):
        live = self._live(at)
        def df(term, _):
            return len({d for d, c, f in self.post.get(term, []) if d in live})
        def postings(term, _):
            return [(d, c, f) for d, c, f in self.post.get(term, []) if d in live]
        def avg_len(col, _):
            vals = [self.len[(d, col)] for d in live]
            return sum(vals) / len(vals) if vals else 1
        return self.score(query, at, depth, len(live), avg_len, df, postings,
                          lambda d, c: self.len[(d, c)], lambda d: self.states[d].address)


class S2bVocabGlobal(S2VocabExact):
    """S2's formula with whole-collection statistics: isolates the statistics from the formula."""
    label = "S2b same formula, GLOBAL stats (isolates the stats error)"

    def _live(self, at):  # scoring set is still the live rows; the statistics are not
        return super()._live(at)

    def search(self, query, at, depth=10):
        live = super()._live(at)
        allrows = set(range(len(self.states)))
        def df(term, _):
            return len({d for d, c, f in self.post.get(term, [])})
        def postings(term, _):
            return [(d, c, f) for d, c, f in self.post.get(term, []) if d in live]
        def avg_len(col, _):
            vals = [self.len[(d, col)] for d in allrows]
            return sum(vals) / len(vals) if vals else 1
        return self.score(query, at, depth, len(allrows), avg_len, df, postings,
                          lambda d, c: self.len[(d, c)], lambda d: self.states[d].address)
