#!/usr/bin/env python3
"""Non-graph arm: BM25 + dense passage retrieval, with a local LLM reader.

Three sub-arms, weakest to strongest, so the baseline is not a straw man:

  B1  one vague query -> top-k passages -> LLM reads and answers.
      This is the naive RAG setup.

  B2  one targeted query PER CANDIDATE PAIR ("does Kestrel supply X?") ->
      top-k passages for that pair -> LLM returns a per-pair verdict.
      Aggregated by taking every pair the reader called yes.
      This is what a competent engineer does instead of B1.

  B3  B2's per-pair evidence scores, then the cardinality claim applied in
      application code: keep the top 4 by evidence, exclude the weakest.
      This is the strongest non-graph arm, and it is the one that matters:
      if B3 matches the graph, the win belongs to the structured aggregation
      step rather than to a graph database.

Usage:
  python3 arm_retrieval.py --corpus corpus --out out/retrieval.json
  python3 arm_retrieval.py --corpus corpus_noagg --out out/retrieval_noagg.json \
      --no-aggregate
"""

import argparse
import json
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common  # noqa: E402

SUPPLIER = "Kestrel Memory Works"

VAGUE_Q = ("Kestrel Memory Works supplies memory to four of the world's five top GPU "
           "producers. Which four GPU producers does Kestrel Memory Works supply, and "
           "which one of the top five does it not supply?")

B1_READER = """You are answering a question from retrieved document passages only.

COMPANY REGISTER:
{register}

RETRIEVED PASSAGES:
{passages}

QUESTION: {question}

Answer strictly from the passages. Note that two companies appearing in the same
document does not mean one supplies the other.

Return ONLY JSON:
{{"supplied": ["<canonical company name>", ...],
  "excluded": "<canonical company name or null>",
  "confidence": 0.0-1.0,
  "reasoning": "<two sentences max>"}}

JSON:"""

B2_READER = """You are judging ONE question from retrieved document passages only.

RETRIEVED PASSAGES:
{passages}

QUESTION: Do the passages give evidence that {supplier} supplies memory
components to {producer}?

Rules:
  - Co-appearance in a lawsuit, a standards body, a working group, a trade body
    board, or a software-only partnership is NOT evidence of supply.
  - Evidence may be indirect: for example {producer} using or recruiting for a
    part that {supplier} manufactures.
  - Judge only {producer}. Do not consider any other company.

Return ONLY JSON:
{{"verdict": "yes"|"no"|"unclear",
  "evidence_strength": 0.0-1.0,
  "basis": "<one sentence, citing the passage>"}}

JSON:"""


class Index:
    def __init__(self, chunks):
        self.chunks = chunks
        t0 = time.time()
        self.bm25 = common.BM25([c["indexed"] for c in chunks])
        self.t_bm25 = time.time() - t0
        t0 = time.time()
        self.vecs = common.embed([c["indexed"] for c in chunks])
        self.t_dense = time.time() - t0

    def search(self, q, k=12, kk=60):
        """Reciprocal rank fusion of BM25 and dense."""
        bs = self.bm25.score(q)
        qv = common.embed([q], prefix="search_query: ")[0]
        ds = [common.cosine(qv, v) for v in self.vecs]
        rb = sorted(range(len(bs)), key=lambda i: -bs[i])
        rd = sorted(range(len(ds)), key=lambda i: -ds[i])
        pos_b = {i: r for r, i in enumerate(rb)}
        pos_d = {i: r for r, i in enumerate(rd)}
        fused = sorted(
            range(len(self.chunks)),
            key=lambda i: -(1.0 / (kk + 1 + pos_b[i]) + 1.0 / (kk + 1 + pos_d[i])),
        )
        return [self.chunks[i] for i in fused[:k]]


def fmt(passages):
    return "\n\n".join(
        "[%d] (%s) %s" % (i + 1, p["doc_id"], p["indexed"]) for i, p in enumerate(passages))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--corpus", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--k", type=int, default=12)
    ap.add_argument("--no-aggregate", action="store_true",
                    help="corpus has no '4 of 5' claim: B3 may not use cardinality")
    ap.add_argument("--reps", type=int, default=1)
    a = ap.parse_args()

    docs = common.load_corpus(a.corpus)
    chunks = common.chunk_corpus(docs)
    print("docs=%d chunks=%d" % (len(docs), len(chunks)))

    idx = Index(chunks)
    print("index: bm25 %.2fs, dense %.2fs (%s)"
          % (idx.t_bm25, idx.t_dense, common.EMBED_MODEL))

    producers = [e["canon"] for e in common.ENTITIES
                 if e["kind"] == "GPUProducer"]
    # The top-five universe, as a competent engineer would have it from the
    # corpus's own ranking record. The graph arm gets the same list from its
    # RANKED_TOP edges, so the arms are on equal footing here.
    top5 = ["Tessara Graphics Corporation", "Nyxvolt Systems Inc.",
            "Orbitine Compute AB", "Halcyon Microsystems", "Veradyne Silicon N.V."]

    result = {
        "corpus": a.corpus, "k": a.k,
        "embed_model": common.EMBED_MODEL, "gen_model": common.GEN_MODEL,
        "index_seconds": {"bm25": round(idx.t_bm25, 2), "dense": round(idx.t_dense, 2)},
        "n_docs": len(docs), "n_chunks": len(chunks),
        "aggregate_available": not a.no_aggregate,
        "reps": [],
    }

    for rep in range(a.reps):
        rec = {"rep": rep}

        # ---------------- B1: one vague query ----------------
        t0 = time.time()
        ps = idx.search(VAGUE_Q, k=a.k)
        q1 = VAGUE_Q if not a.no_aggregate else (
            "Which of the world's five top GPU producers does Kestrel Memory Works "
            "supply memory to, and which does it not supply?")
        raw, _ = common.generate(B1_READER.format(
            register=common.entity_dictionary_text(), passages=fmt(ps), question=q1))
        j = common.first_json(raw) or {}
        rec["B1"] = {
            "retrieved_docs": [p["doc_id"] for p in ps],
            "supplied": [common.resolve(x) or x for x in (j.get("supplied") or [])],
            "excluded": common.resolve(j.get("excluded")) or j.get("excluded"),
            "confidence": j.get("confidence"),
            "reasoning": j.get("reasoning"),
            "seconds": round(time.time() - t0, 2),
            "raw": raw[-1500:],
        }
        print("B1 supplied=%s excluded=%s" % (rec["B1"]["supplied"], rec["B1"]["excluded"]))

        # ---------------- B2: per-pair ----------------
        t0 = time.time()
        pairs = {}
        for prod in top5:
            q = ("Does %s supply memory components to %s? %s %s memory supply "
                 "agreement qualification stacks" % (SUPPLIER, prod, SUPPLIER, prod))
            pp = idx.search(q, k=a.k)
            raw, _ = common.generate(B2_READER.format(
                passages=fmt(pp), supplier=SUPPLIER, producer=prod))
            jj = common.first_json(raw) or {}
            try:
                strength = float(jj.get("evidence_strength") or 0.0)
            except Exception:
                strength = 0.0
            pairs[prod] = {
                "verdict": (jj.get("verdict") or "unclear").lower(),
                "evidence_strength": strength,
                "basis": jj.get("basis"),
                "retrieved_docs": [p["doc_id"] for p in pp],
            }
            print("   B2 %-32s %-8s %.2f" % (prod, pairs[prod]["verdict"], strength))
        t_pairs = time.time() - t0

        rec["B2"] = {
            "pairs": pairs,
            "supplied": sorted(p for p, v in pairs.items() if v["verdict"] == "yes"),
            "seconds": round(t_pairs, 2),
        }
        yes = rec["B2"]["supplied"]
        rec["B2"]["excluded"] = ([p for p in top5 if p not in yes][0]
                                 if len(yes) == 4 else None)

        # ---------------- B3: per-pair + cardinality in code ----------------
        if a.no_aggregate:
            rec["B3"] = {
                "supplied": None, "excluded": None,
                "note": "no aggregate '4 of 5' claim in this corpus, so the "
                        "cardinality constraint is unavailable; B3 is undefined "
                        "and collapses to B2.",
                "seconds": 0.0,
            }
        else:
            def key(p):
                v = pairs[p]
                bump = {"yes": 1.0, "unclear": 0.4, "no": 0.0}.get(v["verdict"], 0.0)
                return bump + v["evidence_strength"]
            ranked = sorted(top5, key=key, reverse=True)
            rec["B3"] = {
                "ranking": [(p, round(key(p), 3)) for p in ranked],
                "supplied": sorted(ranked[:4]),
                "excluded": ranked[4],
                "seconds": 0.0,
                "note": "reuses B2 retrieval and judgements; the only extra work is "
                        "sorting by evidence and cutting at 4.",
            }
            print("B3 excluded=%s" % rec["B3"]["excluded"])

        result["reps"].append(rec)

    os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
    with open(a.out, "w") as f:
        json.dump(result, f, indent=1)
    print("wrote", a.out)


if __name__ == "__main__":
    main()
