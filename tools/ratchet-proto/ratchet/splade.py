"""SPLADE: a learned sparse retriever, tried in place of (or beside) BM25.

From Formal et al., *SPLADE v2* (2021). A masked-language model reads a passage and, for every
vocabulary term, says how much that term should weigh in the passage's bag of words, including terms
the passage never uses. The result is still an inverted index and still a dot product, so it is the
lexical half of the hybrid with the vocabulary gap partly learned away: the paraphrase and intent
strata are exactly the questions whose words are not the archive's words.

This exists for the same reviewer as `hyde.py` (RAG-I5): before concluding that the laptop needs an
8-billion-parameter embedder, try the cheap alternatives. A BERT-base SPLADE is 110M parameters and
runs on the laptop.

Deterministic by construction: a pinned model revision, a fixed maximum length, and the passage
vectors cached on disk by (model, revision, max length, the passages' own hash), so a re-run scores
against the identical index. Queries are encoded the same way and are cheap enough not to cache.

Every term weight is ``log(1 + relu(logit))``, max-pooled over the tokens -- the published SPLADE
max pooling. A passage longer than the encoder's window is **not** silently truncated: the count of
truncated passages is reported, because the encoder cannot read past 512 tokens and the run should
say how often that happened.
"""

from __future__ import annotations

import hashlib
import sys
from pathlib import Path

import numpy as np

#: naver/splade-cocondenser-ensembledistil, the commit measured. splade-v3 is gated on the hub;
#: this is the strongest ungated SPLADE in the BERT-base size.
MODEL = "naver/splade-cocondenser-ensembledistil"
REVISION = "49cf4c7b0db5b870a401ddf5e2669993ef3699c7"
MAX_LENGTH = 512
BATCH = 8
SEQ_CHUNK = 64


class Splade:
    def __init__(self, name: str = MODEL, revision: str = REVISION, device: str | None = None,
                 max_length: int = MAX_LENGTH):
        import torch
        from transformers import AutoModelForMaskedLM, AutoTokenizer

        self.torch = torch
        self.name, self.revision, self.max_length = name, revision, max_length
        self.model_id = f"{name}@{revision[:12]}"
        self.device = device or ("mps" if torch.backends.mps.is_available() else "cpu")
        self.tok = AutoTokenizer.from_pretrained(name, revision=revision)
        self.model = AutoModelForMaskedLM.from_pretrained(name, revision=revision,
                                                          dtype=torch.float32).to(self.device).eval()
        self.vocab = self.model.config.vocab_size
        self.truncated = 0

    def _encode(self, texts: list) -> list:
        """[(term ids, weights)] per text, in order."""
        torch = self.torch
        out = []
        with torch.inference_mode():
            for i in range(0, len(texts), BATCH):
                batch = texts[i:i + BATCH]
                enc = self.tok(batch, padding=True, truncation=True, max_length=self.max_length,
                               return_tensors="pt").to(self.device)
                # Never the whole (batch, seq, vocab) logits tensor: at 8 x 512 x 30,522 floats it
                # is 500 MB, and relu/log1p/mask each made another. That is what grew swap by 3 GB
                # on 1 Nov and tripped the guard. The encoder runs once; the vocabulary head runs
                # over SEQ_CHUNK positions at a time and keeps a running max -- the same numbers.
                hidden = self.model.base_model(**enc).last_hidden_state  # (b, seq, 768)
                mask = enc["attention_mask"].unsqueeze(-1)
                rep = None
                for s in range(0, hidden.shape[1], SEQ_CHUNK):
                    part = self.model.cls(hidden[:, s:s + SEQ_CHUNK])
                    part.relu_().log1p_().mul_(mask[:, s:s + SEQ_CHUNK])
                    m = part.max(dim=1).values
                    rep = m if rep is None else torch.maximum(rep, m)
                    del part
                rep = rep.float().cpu().numpy()
                del hidden
                if self.device == "mps":
                    torch.mps.empty_cache()
                for row in rep:
                    nz = np.nonzero(row)[0].astype(np.int32)
                    out.append((nz, row[nz].astype(np.float32)))
        return out

    def count_truncated(self, texts: list) -> int:
        lengths = [len(ids) for ids in self.tok(texts, add_special_tokens=True, truncation=False)["input_ids"]]
        return sum(1 for n in lengths if n > self.max_length)

    def index(self, texts: list, cache_dir: Path | None = None):
        """A `SparseIndex` over `texts`, from the on-disk cache when it is there."""
        h = hashlib.sha256()
        for t in texts:
            h.update(t.encode("utf-8"))
            h.update(b"\0")
        key = hashlib.sha256(f"{self.name}\0{self.revision}\0{self.max_length}\0{h.hexdigest()}"
                             .encode()).hexdigest()[:16]
        path = Path(cache_dir) / f"splade-{key}.npz" if cache_dir else None
        self.truncated = self.count_truncated(texts)
        if path is not None and path.exists():
            z = np.load(path)
            indptr, indices, data = z["indptr"], z["indices"], z["data"]
            print(f"SPLADE: {len(texts)} passage vectors from {path.name}", file=sys.stderr)
        else:
            # Longest first, so a batch pads to similar lengths; results go back to text order.
            order = sorted(range(len(texts)), key=lambda i: -len(texts[i]))
            vecs = [None] * len(texts)
            done = 0
            for start in range(0, len(order), 256):
                chunk = order[start:start + 256]
                for i, v in zip(chunk, self._encode([texts[i] for i in chunk])):
                    vecs[i] = v
                done += len(chunk)
                print(f"    SPLADE {done}/{len(texts)}", file=sys.stderr)
            indptr = np.zeros(len(texts) + 1, dtype=np.int64)
            indptr[1:] = np.cumsum([len(v[0]) for v in vecs])
            indices = np.concatenate([v[0] for v in vecs])
            data = np.concatenate([v[1] for v in vecs])
            if path is not None:
                path.parent.mkdir(parents=True, exist_ok=True)
                np.savez(path, indptr=indptr, indices=indices, data=data)
        from scipy.sparse import csr_matrix

        m = csr_matrix((data, indices, indptr), shape=(len(texts), self.vocab))
        return SparseIndex(self, m)


class SparseIndex:
    """The BM25 interface over SPLADE vectors: ``search(q, allow, depth, scope)``."""

    def __init__(self, enc: Splade, matrix):
        self.enc = enc
        self.m = matrix
        self.q = {}

    def _query(self, q: str) -> np.ndarray:
        if q not in self.q:
            ids, w = self.enc._encode([q])[0]
            v = np.zeros(self.enc.vocab, dtype=np.float32)
            v[ids] = w
            self.q[q] = v
        return self.q[q]

    def search(self, q: str, allow=None, depth: int = 100, scope=None) -> list:
        """[(item n, score)] best first. Only items sharing at least one weighted term are returned,
        as only items matching at least one query term come back from FTS5. ``allow`` filters before
        the depth cut; ties break on the item number, as BM25's ``order by s, rowid`` does."""
        s = np.asarray(self.m @ self._query(q)).ravel()
        if scope is not None:
            keep = np.zeros(len(s), dtype=bool)
            keep[list(scope)] = True
            s = np.where(keep, s, 0.0)
        cand = np.nonzero(s > 0)[0]
        cand = cand[np.lexsort((cand, -s[cand]))]
        out = []
        for n in cand:
            n = int(n)
            if allow is None or allow(n):
                out.append((n, float(s[n])))
                if len(out) >= depth:
                    break
        return out
