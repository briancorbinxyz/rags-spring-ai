#!/usr/bin/env python3
"""Shared pieces: corpus loading, chunking, BM25, Ollama calls, entity dictionary.

Stdlib only, deliberately: no third-party Python packages are needed anywhere in
this spike, so there is nothing for the author to install to rerun it.
"""

import json
import math
import os
import re
import time
import urllib.request

OLLAMA = os.environ.get("OLLAMA_HOST", "http://localhost:11434")
EMBED_MODEL = "nomic-embed-text:v1.5"   # the project's default embedding model
GEN_MODEL = "qwen3:8b"                  # local reader / extractor

# Pinned generation options. Matches the project's convention of pinning
# seed, top_p, top_k and keep_alive so a rerun reproduces.
GEN_OPTS = {
    "temperature": 0.0,
    "top_p": 1.0,
    "top_k": 1,
    "seed": 20261029,
    "num_ctx": 6144,
    "num_predict": 2600,
}
KEEP_ALIVE = "20m"


# ----------------------------- corpus --------------------------------------

def load_corpus(path):
    with open(os.path.join(path, "corpus.json")) as f:
        return json.load(f)


def doc_blob(d):
    return "TITLE: %s\nDATE: %s\nSOURCE: %s\nTYPE: %s\n\n%s" % (
        d["title"], d["date"], d["source"], d["type"], d["text"])


def chunk_corpus(docs, target_words=120, overlap=25):
    """Passage chunks, the unit a passage retriever actually indexes."""
    chunks = []
    for d in docs:
        words = d["text"].split()
        if not words:
            continue
        step = max(1, target_words - overlap)
        i = 0
        n = 0
        while i < len(words):
            piece = " ".join(words[i:i + target_words])
            chunks.append({
                "chunk_id": "%s#%d" % (d["id"], n),
                "doc_id": d["id"],
                "title": d["title"],
                "source": d["source"],
                "type": d["type"],
                "date": d["date"],
                "text": piece,
                # what a retriever shows a reader: title plus passage
                "indexed": "%s (%s, %s). %s" % (d["title"], d["source"], d["date"], piece),
            })
            n += 1
            if i + target_words >= len(words):
                break
            i += step
    return chunks


# ----------------------------- BM25 ----------------------------------------

_TOK = re.compile(r"[a-z0-9]+")


def tok(s):
    return _TOK.findall(s.lower())


class BM25:
    """Okapi BM25, k1=1.2 b=0.75."""

    def __init__(self, texts, k1=1.2, b=0.75):
        self.k1, self.b = k1, b
        self.docs = [tok(t) for t in texts]
        self.N = len(self.docs)
        self.len = [len(d) for d in self.docs]
        self.avgdl = sum(self.len) / max(1, self.N)
        self.tf = []
        df = {}
        for d in self.docs:
            c = {}
            for w in d:
                c[w] = c.get(w, 0) + 1
            self.tf.append(c)
            for w in c:
                df[w] = df.get(w, 0) + 1
        self.idf = {w: math.log(1 + (self.N - n + 0.5) / (n + 0.5)) for w, n in df.items()}

    def score(self, query):
        q = tok(query)
        out = [0.0] * self.N
        for i in range(self.N):
            tf, dl = self.tf[i], self.len[i]
            s = 0.0
            for w in q:
                f = tf.get(w)
                if not f:
                    continue
                s += self.idf[w] * f * (self.k1 + 1) / (
                    f + self.k1 * (1 - self.b + self.b * dl / self.avgdl))
            out[i] = s
        return out


# ----------------------------- Ollama --------------------------------------

def _post(path, payload, timeout=600):
    req = urllib.request.Request(
        OLLAMA + path,
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read())


def embed(texts, prefix="search_document: "):
    """nomic-embed-text was trained with search_document:/search_query: prefixes;
    the project's PrefixedEmbeddingModel adds them, so we do the same."""
    out = []
    B = 16
    for i in range(0, len(texts), B):
        batch = [prefix + t for t in texts[i:i + B]]
        r = _post("/api/embed", {"model": EMBED_MODEL, "input": batch,
                                 "keep_alive": KEEP_ALIVE})
        out.extend(r["embeddings"])
    return out


def cosine(a, b):
    num = sum(x * y for x, y in zip(a, b))
    na = math.sqrt(sum(x * x for x in a))
    nb = math.sqrt(sum(x * x for x in b))
    return num / (na * nb) if na and nb else 0.0


def generate(prompt, think=False, opts=None):
    o = dict(GEN_OPTS)
    if opts:
        o.update(opts)
    t0 = time.time()
    r = _post("/api/generate", {
        "model": GEN_MODEL, "prompt": prompt, "stream": False,
        "think": think, "options": o, "keep_alive": KEEP_ALIVE,
    })
    return r.get("response", ""), time.time() - t0


def first_json(s):
    """Pull the first JSON object or array out of a model response."""
    s = re.sub(r"<think>.*?</think>", "", s, flags=re.S)
    s = re.sub(r"^```(?:json)?|```$", "", s.strip(), flags=re.M).strip()
    ia, io = s.find("["), s.find("{")
    # Whichever bracket comes first decides what this response IS. Trying "{"
    # on a response that opens with "[" would parse only the array's first
    # object and return a dict, silently dropping every other relation.
    array_first = ia >= 0 and (io < 0 or ia < io)
    if array_first:
        return _balanced(s, ia, "[", "]") or salvage_array(s)
    if io >= 0:
        return _balanced(s, io, "{", "}")
    return None


def _balanced(s, i, opener, closer):
    """Parse the bracket-balanced span starting at s[i], or None."""
    depth = 0
    instr = False
    esc = False
    for j in range(i, len(s)):
        ch = s[j]
        if instr:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                instr = False
            continue
        if ch == '"':
            instr = True
        elif ch == opener:
            depth += 1
        elif ch == closer:
            depth -= 1
            if depth == 0:
                try:
                    return json.loads(s[i:j + 1])
                except Exception:
                    return None
    return None


def salvage_array(s):
    """Recover the complete objects from a JSON array the model truncated.

    Generation cut off by num_predict is a real and common failure; without this
    the whole document's relations are silently lost, which would understate the
    graph arm for the wrong reason.
    """
    if "[" not in s:
        return None
    body = s[s.find("[") + 1:]
    out = []
    depth = 0
    start = None
    instr = False
    esc = False
    for j, ch in enumerate(body):
        if instr:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                instr = False
            continue
        if ch == '"':
            instr = True
        elif ch == "{":
            if depth == 0:
                start = j
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0 and start is not None:
                try:
                    out.append(json.loads(body[start:j + 1]))
                except Exception:
                    pass
                start = None
    return out or None


# ------------------------- entity dictionary -------------------------------
# Supplied to BOTH the extractor and the retrieval reader, identically, so the
# arms are on equal footing. It encodes only what a company-register lookup
# would give you: canonical names, aliases, and what kind of company each is.
# It deliberately does NOT encode any supply relation.

ENTITIES = [
    {"canon": "Tessara Graphics Corporation", "kind": "GPUProducer",
     "aliases": ["Tessara", "Tessara Graphics"]},
    {"canon": "Nyxvolt Systems Inc.", "kind": "GPUProducer",
     "aliases": ["Nyxvolt Systems", "Nyxvolt Sys"]},
    {"canon": "Orbitine Compute AB", "kind": "GPUProducer",
     "aliases": ["Orbitine", "Orbitine Compute"]},
    {"canon": "Halcyon Microsystems", "kind": "GPUProducer",
     "aliases": ["Halcyon Micro", "Halcyon"]},
    {"canon": "Veradyne Silicon N.V.", "kind": "GPUProducer",
     "aliases": ["Veradyne", "Veradyne Silicon"]},
    {"canon": "Brightfell Graphics", "kind": "GPUProducer", "aliases": ["Brightfell"]},
    {"canon": "Cindermere Visual Systems", "kind": "GPUProducer",
     "aliases": ["Cindermere", "Cindermere Visual"]},
    {"canon": "Kestrel Memory Works", "kind": "MemorySupplier",
     "aliases": ["Kestrel Memory", "KMW"]},
    {"canon": "Lumenstor Memory", "kind": "MemorySupplier", "aliases": ["Lumenstor"]},
    {"canon": "Sable Dynamics", "kind": "MemorySupplier", "aliases": ["Sable"]},
    {"canon": "Arcwell Semiconductor", "kind": "MemorySupplier", "aliases": ["Arcwell"]},
    {"canon": "Cobalt Ridge Networking", "kind": "OtherCompany",
     "aliases": ["Cobalt Ridge"]},
    {"canon": "Peregrine Automotive Electronics", "kind": "OtherCompany",
     "aliases": ["Peregrine Automotive", "Peregrine"]},
    {"canon": "Thornbury Aerospace Systems", "kind": "OtherCompany",
     "aliases": ["Thornbury Aerospace", "Thornbury"]},
    {"canon": "Nyxvolt Labs", "kind": "OtherCompany", "aliases": []},
    {"canon": "Kestrel Analytics Group", "kind": "OtherCompany",
     "aliases": ["Kestrel Analytics"]},
    {"canon": "Calderway Substrates", "kind": "OtherCompany", "aliases": ["Calderway"]},
    {"canon": "Pellingham Test Services", "kind": "OtherCompany", "aliases": ["Pellingham"]},
    {"canon": "Vorsa Thermal", "kind": "OtherCompany", "aliases": ["Vorsa"]},
    {"canon": "Open Memory Standards Council", "kind": "Organisation",
     "aliases": ["OMSC"]},
    {"canon": "Semiconductor Alliance of Europe", "kind": "Organisation",
     "aliases": ["SAE"]},
]

CANON = {e["canon"]: e for e in ENTITIES}
KIND = {e["canon"]: e["kind"] for e in ENTITIES}


def entity_dictionary_text():
    lines = []
    for e in ENTITIES:
        al = (" (also written: %s)" % ", ".join(e["aliases"])) if e["aliases"] else ""
        lines.append("- %s [%s]%s" % (e["canon"], e["kind"], al))
    lines.append("")
    lines.append("Caution on similar names, which are DIFFERENT companies:")
    lines.append("- 'Nyxvolt Labs' is NOT 'Nyxvolt Systems Inc.'")
    lines.append("- 'Kestrel Analytics Group' is NOT 'Kestrel Memory Works'")
    lines.append("- 'Halcyon Micro' IS the same company as 'Halcyon Microsystems'")
    return "\n".join(lines)


def resolve(name):
    """Map a surface name to a canonical entity, longest-alias-first."""
    if not name:
        return None
    s = name.strip().lower().rstrip(".,;")
    best = None
    for e in ENTITIES:
        for cand in [e["canon"]] + e["aliases"]:
            c = cand.lower()
            if s == c or s.startswith(c) or c.startswith(s):
                if best is None or len(c) > best[1]:
                    best = (e["canon"], len(c))
    return best[0] if best else None
