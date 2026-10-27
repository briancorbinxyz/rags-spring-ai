"""Episode 4 step 5: a cross-encoder over the top 50, through sentence-transformers or llama.cpp.

``--reranker cross-encoder/ms-marco-MiniLM-L-6-v2`` loads the model from the Hugging Face hub
(or its local cache). Without sentence-transformers, or without the model, the reranker
configurations are skipped and the report says so. Pinned identifier hits are never reranked.

``--reranker llamacpp:<alias>`` talks to a llama-server started with ``--reranking`` (its
``/v1/rerank``; ``--llamacpp-rerank-url``). That one was asked for by name, so any problem stops
the run: a server that is down, serves another model, refuses an input as too long, or returns
scores that fail a sanity pair (a GGUF converted without its rerank head scores null or garbage).
"""

from __future__ import annotations

import sys

from . import llamacpp as L


class Reranker:
    #: Scores every passage on its own scale; it does not choose a subset. See `selects` below.
    selects = False

    def __init__(self, name: str, max_length: int = 512):
        from sentence_transformers import CrossEncoder

        self.name = name
        self.model = CrossEncoder(name, max_length=max_length)
        self.cache = {}

    def scores(self, query: str, passages: list) -> list:
        todo = [p for p in passages if (query, p) not in self.cache]
        if todo:
            out = self.model.predict([(query, p) for p in todo], batch_size=32, show_progress_bar=False)
            for p, s in zip(todo, out):
                self.cache[(query, p)] = float(s)
        return [self.cache[(query, p)] for p in passages]


class LlamaCppReranker:
    selects = False
    batch = 64
    SANITY = ("What is the capital of France?",
              ["Paris is the capital and largest city of France.", "Bananas are a good source of potassium."])

    def __init__(self, alias: str, url: str, cache_dir=None):
        m = L.model(alias, "rerank")
        self.m = m
        self.srv = L.Server(url)
        self.srv.reachable(f"Start it with: llama-server {' '.join(m.server_args(L.port_of(url)))}")
        self.srv.check_alias(alias)
        self.model_id, self.provenance = L.gguf_identity(self.srv, m, cache_dir)
        self.name = f"llamacpp:{alias}"
        self.cache = {}
        good, bad = self._score(*self.SANITY)
        if not good > bad:
            raise SystemExit(f"{alias} fails the sanity pair (relevant {good:.4g} <= irrelevant {bad:.4g}): "
                             f"this GGUF does not rerank. Refusing to run the reranker rows with it.")
        self.sanity = (good, bad)

    def _score(self, query: str, docs: list) -> list:
        try:
            d = self.srv.post("/v1/rerank", {"model": self.m.alias, "query": query, "documents": docs,
                                             "top_n": len(docs)})
        except L.ServerError as e:
            if e.too_long:
                L.too_long_exit(self.m.alias, f"llama-server refused a query + passage as too long: {e.message}")
            raise
        by = {r["index"]: r.get("relevance_score", r.get("score")) for r in d["results"]}
        return L.check_scores([by.get(i) for i in range(len(docs))], len(docs), self.m.alias)

    def scores(self, query: str, passages: list) -> list:
        todo = list(dict.fromkeys(p for p in passages if (query, p) not in self.cache))
        for i in range(0, len(todo), self.batch):
            part = todo[i : i + self.batch]
            for p, s in zip(part, self._score(query, part)):
                self.cache[(query, p)] = float(s)
        return [self.cache[(query, p)] for p in passages]


class OllamaJudgeReranker:
    """An instruction-following model asked, per passage, whether it answers the question.

    Not a cross-encoder. The MS-MARCO cross-encoders are trained on short web queries and are weak
    at negation, which is the one thing G-073 turns on: it asks about a task telling implementers
    *not to add* a resource kind, and every lexical and dense ranker prefers the tasks that add one.
    MiniLM-L6 put the answering unit 60th of 215, MiniLM-L12 58th, mxbai-rerank-base 26th. A model
    that can be *told* to watch for negation is the only remaining mechanism, so it is worth one
    measurement — and it may still fail, which is the point of measuring rather than assuming.

    Pointwise and slow: one call per passage. It exists to test a hypothesis about negation, not to
    sit in the main line, and the instruction is fixed so that nothing here is tuned per question.
    """

    selects = False  # a 0-10 score per passage, and it saturates: see the class docstring

    INSTRUCTION = (
        "Does this passage, on its own, answer the question? Reply with one integer from 0 to 10 "
        "and nothing else, where 10 means it directly answers the question and 0 means it is "
        "irrelevant. Pay attention to negation: a passage saying something must NOT be done does "
        "not answer a question about doing it, and a passage saying something must be done does "
        "not answer a question about not doing it."
    )

    def __init__(self, model: str, url: str = "http://localhost:11434", think: bool = False):
        self.name = f"ollama:{model}"
        self.model = model
        self.url = url.rstrip("/")
        self.think = think
        self.cache = {}

    def _ask(self, query: str, passage: str) -> float:
        import json
        import urllib.request

        prompt = f"Question: {query}\n\nPassage:\n{passage}\n\n{self.INSTRUCTION}"
        if not self.think:
            prompt += " /no_think"
        body = json.dumps({"model": self.model, "prompt": prompt, "stream": False,
                           "think": self.think,
                           "options": {"temperature": 0, "num_predict": 1200}}).encode()
        request = urllib.request.Request(f"{self.url}/api/generate", body,
                                         {"Content-Type": "application/json"})
        reply = json.loads(urllib.request.urlopen(request, timeout=300).read())["response"]
        tail = reply.split("</think>")[-1]
        digits = [t for t in "".join(c if c.isdigit() else " " for c in tail).split() if t]
        return float(digits[-1]) if digits else 0.0

    def scores(self, query: str, passages: list) -> list:
        for passage in passages:
            if (query, passage) not in self.cache:
                self.cache[(query, passage)] = self._ask(query, passage)
        return [self.cache[(query, p)] for p in passages]


class OllamaListwiseReranker:
    """One call, every candidate, and the model must choose an order.

    The pointwise judge above saturated: asked to rate each passage from 0 to 10 it gave 10 to six
    different ones, the answering unit and the passages saying the opposite alike, so nothing was
    separated and the order fell out of the tie-break. An absolute scale cannot express "this one
    and not that one" when every candidate is about the question's topic, and negation is exactly
    that kind of distinction — comparative by nature.

    So this asks for a comparison instead. It is the standard listwise shape from the RankGPT line
    of work, and on G-073 it moves the answering unit from 9th to 1st where three cross-encoders and
    the pointwise judge all failed.

    Candidates the model does not name keep their incoming order, because a model that returns three
    identifiers out of twenty has *selected* rather than ordered, and discarding the rest would
    throw away a ranking that was already better than nothing.
    """

    #: This reader *chooses*: it names the candidates it finds useful and is silent about the rest,
    #: which is what makes its output something to pin rather than something to sort by. A reader
    #: that scores every candidate on its own scale has not chosen anything, and reading a choice
    #: out of the sign of a logit would be meaningless.
    selects = True

    #: Passages are truncated for the prompt only. A listwise call has to hold every candidate at
    #: once, so this trades tail detail for being able to compare them at all.
    CHARS = 600

    #: Enough for 50 candidates at CHARS each, with room for the instruction and the reply.
    CONTEXT = 16384

    def __init__(self, model: str, url: str = "http://localhost:11434", window: int = 50):
        self.name = f"ollama-listwise:{model}"
        self.model = model
        self.url = url.rstrip("/")
        self.window = window
        self.cache = {}

    def _order(self, query: str, passages: list) -> list:
        import json
        import urllib.request

        numbered = "\n\n".join(f"[{i + 1}] {p[:self.CHARS]}" for i, p in enumerate(passages))
        prompt = (
            f"Question: {query}\n\nCandidate passages:\n\n{numbered}\n\n"
            "Order the candidates from most to least useful for answering the question. "
            "Negation decides this: a passage saying something must NOT be done answers a question "
            "about not doing it, and does NOT answer a question about doing it. "
            "Reply with the identifiers only, best first, as a comma-separated list like "
            "[3],[1],[7]. No other text. /no_think")
        # num_ctx is not optional here. Ollama defaults it to a few thousand tokens regardless of
        # what the model supports — qwen3:8b declares 40,960 and its modelfile sets none — and a
        # prompt over that limit is **silently truncated**, so the tail of the candidate list would
        # simply not be read and the ranking would look like a judgement.
        body = json.dumps({"model": self.model, "prompt": prompt, "stream": False, "think": False,
                           "options": {"temperature": 0, "num_predict": 1500,
                                       "num_ctx": self.CONTEXT}}).encode()
        request = urllib.request.Request(f"{self.url}/api/generate", body,
                                         {"Content-Type": "application/json"})
        # A listwise call is one HTTP request holding every candidate, so a hung one stalls the
        # whole run with nothing to show for it: a thirty-minute timeout meant a single bad call
        # looked exactly like slow progress for half an hour. Five minutes is well past the
        # measured cost — about 30s for fifty candidates uncontended, four and a half minutes at
        # worst under contention — and a stall now says so.
        answer = json.loads(urllib.request.urlopen(request, timeout=300).read())
        if answer.get("prompt_eval_count", 0) >= self.CONTEXT:
            raise RuntimeError(
                f"the listwise prompt filled the context ({answer['prompt_eval_count']} tokens of "
                f"{self.CONTEXT}): candidates were dropped, so this is not a ranking")
        reply = answer["response"]
        tail = reply.split("</think>")[-1]
        out = []
        for token in tail.replace("[", " ").replace("]", " ").replace(",", " ").split():
            if token.isdigit():
                i = int(token) - 1
                if 0 <= i < len(passages) and i not in out:
                    out.append(i)
        return out

    def scores(self, query: str, passages: list) -> list:
        """Scores standing in for an order: chosen candidates descend, the rest keep their place.

        The pipeline sorts by score with a stable sort, so giving every unchosen candidate the same
        score leaves them in the order they arrived in.
        """
        head = passages[:self.window]
        key = (query, tuple(head))
        if key not in self.cache:
            self.cache[key] = self._order(query, head)
        chosen = self.cache[key]
        ranks = {i: len(head) - r for r, i in enumerate(chosen)}
        return [float(ranks.get(i, 0)) for i in range(len(passages))]


def make(name: str | None, llamacpp_url: str = "http://127.0.0.1:8091", cache_dir=None):
    """(reranker or None, message)."""
    if not name or name == "none":
        return None, "no reranker requested"
    if name.startswith("ollama-listwise:"):
        rr = OllamaListwiseReranker(name[len("ollama-listwise:"):])
        return rr, f"reranker {rr.name}: one listwise call over the first {rr.window} candidates"
    if name.startswith("ollama:"):
        rr = OllamaJudgeReranker(name[len("ollama:"):])
        return rr, f"reranker {rr.name}: an instruction-following judge, pointwise, one call per passage"
    if name.startswith("llamacpp:"):
        rr = LlamaCppReranker(name[len("llamacpp:"):], llamacpp_url, cache_dir)
        return rr, (f"{rr.provenance}; sanity pair {rr.sanity[0]:.4g} > {rr.sanity[1]:.4g}; "
                    f"query + passage must fit {L.input_limit(rr.srv, rr.m)} tokens (refused, never truncated)")
    try:
        import sentence_transformers  # noqa: F401
    except ImportError:
        return None, f"reranker {name} skipped: sentence-transformers is not installed (pip install sentence-transformers)"
    try:
        return Reranker(name), f"reranker {name} via sentence-transformers"
    except Exception as e:  # no network, no cached model
        print(f"reranker {name} could not be loaded: {e}", file=sys.stderr)
        return None, f"reranker {name} skipped: could not be loaded ({type(e).__name__})"
