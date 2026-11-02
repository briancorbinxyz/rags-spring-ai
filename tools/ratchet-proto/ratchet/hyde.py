"""HyDE: search with a hypothetical answer instead of the question.

The idea, from Gao et al.'s *Precise Zero-Shot Dense Retrieval without Relevance Labels* (2022):
a question and the passage that answers it are written in different registers, which is exactly
what hurts the paraphrase and intent strata. So ask a model to *write the passage it expects*, and
embed that instead. The hypothetical can be wrong on the facts and still land in the right
neighbourhood, because it is only ever used as a query vector.

This exists to answer a reviewer: before concluding that the laptop needs an 8-billion-parameter
embedder, try the cheap thing. HyDE needs no new model — the chat model and the embedder are
already pinned for the series — so it is the one laptop alternative that can be measured here.

Deterministic by construction: temperature 0, the series' seed, a fixed prompt, and the
hypothetical cached on disk by (model, prompt-hash, question) so a re-run embeds the same text.
"""

from __future__ import annotations

import hashlib
import json
import sqlite3
import urllib.request
from pathlib import Path

#: Written once. A change here is a new measurement, so it is a constant rather than an argument.
PROMPT = (
    "Write a short passage from a software project's specification archive that would answer the "
    "question below. Write it as the specification itself would — declarative, specific, no "
    "preamble, no hedging, and never refer to the question. Two or three sentences. If you do not "
    "know the specifics, invent plausible ones: this text is used only to find the real passage."
    "\n\nQuestion: {question}\n\nPassage:"
)

SEED = 42
TEMPERATURE = 0.0
MAX_TOKENS = 220


class Hyde:
    """Hypothetical passages for questions, generated once and cached."""

    def __init__(self, model: str, url: str = "http://localhost:11434", cache: Path | None = None):
        self.model = model
        self.url = url
        self.prompt_id = hashlib.sha256(PROMPT.encode("utf-8")).hexdigest()[:12]
        self.db = None
        if cache is not None:
            cache.parent.mkdir(parents=True, exist_ok=True)
            self.db = sqlite3.connect(cache)
            self.db.execute("create table if not exists h (k text primary key, text text)")
            self.db.commit()

    def _key(self, question: str) -> str:
        return hashlib.sha256(
            f"{self.model}\0{self.prompt_id}\0{question}".encode("utf-8")
        ).hexdigest()

    def _cached(self, key: str):
        if self.db is None:
            return None
        row = self.db.execute("select text from h where k = ?", (key,)).fetchone()
        return row[0] if row else None

    def _store(self, key: str, text: str) -> None:
        if self.db is not None:
            self.db.execute("insert or replace into h values (?, ?)", (key, text))
            self.db.commit()

    def _generate(self, question: str) -> str:
        body = json.dumps(
            {
                "model": self.model,
                "prompt": PROMPT.format(question=question) + " /no_think",
                "stream": False,
                "think": False,
                "options": {
                    "temperature": TEMPERATURE,
                    "seed": SEED,
                    "num_predict": MAX_TOKENS,
                },
            }
        ).encode()
        request = urllib.request.Request(
            f"{self.url}/api/generate", body, {"Content-Type": "application/json"}
        )
        reply = json.loads(urllib.request.urlopen(request, timeout=300).read())["response"]
        return reply.split("</think>")[-1].strip()

    def passage(self, question: str) -> str:
        """The hypothetical answer for `question`, from cache when it is there."""
        key = self._key(question)
        text = self._cached(key)
        if text is None:
            text = self._generate(question)
            self._store(key, text)
        return text

    def passages(self, questions: list) -> list:
        return [self.passage(q) for q in questions]

    def close(self) -> None:
        if self.db is not None:
            self.db.close()
