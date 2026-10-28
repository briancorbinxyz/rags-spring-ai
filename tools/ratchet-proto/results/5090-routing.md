# Ratchet prototype: llamacpp:qwen3-embedding-8b-q8_0

Run 2026-10-28 12:12 · spectastic c64d41c · 10,540 element units in 11,492 items (units over 250 words split into parts) · 10,540 prior-prototype units · 2,147 naive chunks

- Embedder: llama.cpp Qwen/Qwen3-Embedding-8B-GGUF:Q8_0 as `qwen3-embedding-8b-q8_0` (pooling last); file /home/briancorbin/.cache/huggingface/hub/models--Qwen--Qwen3-Embedding-8B-GGUF/snapshots/69d0e58a13e463cd99a9b83e3f5fee7c10265fab/Qwen3-Embedding-8B-Q8_0.gguf; sha256 d20ddc71e8a5c4344f2343481e242233a997dc5eaff442427a945836c97b4deb; 7567295488 params, n_embd 4096, n_ctx_train 40960; slot context 2048; server build b11832-643b0cdac; queries as 'Instruct: Given a web search query, retrieve relevant passages that answer the query\nQuery:{text}', documents as '{text}'; <|endoftext|> added by the GGUF's tokenizer (add_eos_token); inputs must fit 2048 tokens (checked, never truncated)
- Longest embedder input embedded in this run: 67 tokens (texts already in the vector cache are not re-counted)
- Reranker: llama.cpp ethicalabs/Qwen3-Reranker-8B-GGUF:Q8_0 as `qwen3-reranker-8b-q8_0` (pooling rank); file /home/briancorbin/.cache/huggingface/hub/models--ethicalabs--Qwen3-Reranker-8B-GGUF/snapshots/a521f5253a5c8c6e390af5458fdb736274127052/Qwen3-Reranker-8B.Q8_0.gguf; sha256 2f63dc6f2aa0b1f2b8f92004754049fe7c63954a9e6e856dc262c376d4e2cd26; 7567320064 params, n_embd 4096, n_ctx_train 40960; slot context 2048; server build b11832-643b0cdac; sanity pair 0.9571 > 1.17e-05; query + passage must fit 2048 tokens (refused, never truncated)
- Context tokens: cl100k_base via tiktoken (SHA-256 checked); counted over the text given to a reader (heading path + unit text, or the chunk)
- Golden set: src/main/resources/golden/questions-routing.jsonl (answerable 15, unanswerable 0); run time 51 s

## Episode 4 main line and Episode 5 (cumulative)

Each row adds one step to the row above. Verdict against the previous row that ran: **pass** unless overall Hit@4 or all-evidence@4 drops, a stratum loses more than one question at Hit@4, or stale-in-top-4 rises.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| S1 | 1 element units + BM25 | 3/15 (20%) | – | 0.091 | 3 | 4 | 0 | 726 / 956 | (start) |  |
| S2 | 2 + identifier lookup | 3/15 (20%) | – | 0.091 | 3 | 4 | 0 | 726 / 956 | **pass** (vs S1) | +0 / −0 |
| S3 | 3 + lifecycle filter | 3/15 (20%) | – | 0.102 | 3 | 4 | 0 | 722 / 956 | **pass** (vs S2) | +0 / −0 |
| S4 | 4 + dense, RRF k=60 | 6/15 (40%) | – | 0.411 | 6 | 8 | 0 | 799 / 1048 | **pass** (vs S3) | +3 R-002 R-003 R-009 / −0 |
| S5 | 5 + cross-encoder rerank top 50 | 14/15 (93%) | – | 0.822 | 14 | 14 | 0 | 811 / 1260 | **pass** (vs S4) | +8 R-005 R-006 R-007 R-008 R-011 R-013 R-014 R-015 / −0 |
| E5-2 | E5-2 + anchored typed expansion | 14/15 (93%) | – | 0.822 | 14 | 14 | 0 | 823 / 1260 | **pass** (vs S5) | +0 / −0 |

## Episode 5 on the BM25 line (no embedder involved)

The same two Episode 5 steps on top of step 3, so their effect can be read without any embedder.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| S3 | 3 + lifecycle filter | 3/15 (20%) | – | 0.102 | 3 | 4 | 0 | 722 / 956 | (start) |  |

## Variants, epilogue and ablations

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max |
|---|---|---|---|---|---|---|---|---|
| G9 | Episode 6: the arms that survived their ratchet | 14/15 (93%) | – | 0.822 | 14 | 14 | 0 | 823 / 1260 |
| GA | G9 + a why-question may see the archive's #context sections | 14/15 (93%) | – | 0.833 | 14 | 14 | 0 | 815 / 1260 |
| A | naive 800-token chunks, dense only | 8/15 (53%) | 9 | 0.439 | 8 | 9 | 0 | 2934 / 3170 |
| B | naive 800-token chunks, BM25 only | 1/15 (7%) | 1 | 0.088 | 1 | 2 | 0 | 2936 / 3162 |
| C | element units, dense only | 11/15 (73%) | – | 0.486 | 11 | 13 | 0 | 795 / 1101 |

## Per stratum: Hit@4 · all@4 · all@10

| key | intent-noword (8) | triage-why (7) | stale@4 |
|---|---|---|---|
| S1 | 1 · 1 · 2 | 2 · 2 · 2 | 0 |
| S2 | 1 · 1 · 2 | 2 · 2 · 2 | 0 |
| S3 | 1 · 1 · 2 | 2 · 2 · 2 | 0 |
| S4 | 3 · 3 · 5 | 3 · 3 · 3 | 0 |
| S5 | 7 · 7 · 7 | 7 · 7 · 7 | 0 |
| E5-2 | 7 · 7 · 7 | 7 · 7 · 7 | 0 |
| G9 | 7 · 7 · 7 | 7 · 7 · 7 | 0 |
| GA | 7 · 7 · 7 | 7 · 7 · 7 | 0 |
| A | 4 · 4 · 5 | 4 · 4 · 4 | 0 |
| B | 0 · 0 · 0 | 1 · 1 · 2 | 0 |
| C | 7 · 7 · 7 | 4 · 4 · 6 | 0 |

## Checks against Episode 3 and the earlier prototype

- Naive chunks rebuilt: **2,147** (Episode 3 manifest: 2,147). Every Episode 3 "chunk N of M" total matches this splitter: 0 mismatches.
- Answerable questions with an expected element whole in some naive chunk: **13/44** (Episode 3: 38/44); expected elements whole in some chunk: 13/15.
- Config A's top-4 equals Episode 3's top-4 (same chunks, same order) on **0/50** questions. With the same nomic-embed-text digest it should be at or near 50/50 (Ollama's batching can move near-ties; nomic through llama.cpp is another build of the same weights, so expect most, not all); with any other embedder it means nothing.

## Unanswerable questions: top-1 score of the last ranking stage (hint only)

Pinned identifier hits have no score and are left out. Scores are not comparable across rows (BM25, cosine, RRF and cross-encoder scales differ). Declining needs the full run with the chat model.

| key | answerable, unpinned: n · min · p25 · p50 · max | unanswerable: n · min · p25 · p50 · max |
|---|---|---|
| S1 | 15 · 15.9 · 18.4 · 20.6 · 29.5 | – |
| S2 | 15 · 15.9 · 18.4 · 20.6 · 29.5 | – |
| S3 | 15 · 15.9 · 18.1 · 19.8 · 29.5 | – |
| S4 | 15 · 0.0207 · 0.025 · 0.0296 · 0.0325 | – |
| S5 | 15 · 0.91 · 0.978 · 0.995 · 0.999 | – |
| E5-2 | 15 · 0.91 · 0.978 · 0.995 · 0.999 | – |
| G9 | 15 · 0.91 · 0.978 · 0.995 · 0.999 | – |
| GA | 15 · 0.91 · 0.951 · 0.995 · 0.999 | – |
| A | 15 · 0.499 · 0.528 · 0.567 · 0.71 | – |
| B | 15 · 12.9 · 15.2 · 18.5 · 25 | – |
| C | 15 · 0.532 · 0.557 · 0.593 · 0.74 | – |
