# Ratchet prototype: llamacpp:qwen3-embedding-8b-q8_0

Run 2026-10-23 00:52 · spectastic c64d41c · 10,540 element units in 11,492 items (units over 250 words split into parts) · 10,540 prior-prototype units · 2,147 naive chunks

- Embedder: llama.cpp Qwen/Qwen3-Embedding-8B-GGUF:Q8_0 as `qwen3-embedding-8b-q8_0` (pooling last); file /home/briancorbin/.cache/huggingface/hub/models--Qwen--Qwen3-Embedding-8B-GGUF/snapshots/69d0e58a13e463cd99a9b83e3f5fee7c10265fab/Qwen3-Embedding-8B-Q8_0.gguf; sha256 d20ddc71e8a5c4344f2343481e242233a997dc5eaff442427a945836c97b4deb; 7567295488 params, n_embd 4096, n_ctx_train 40960; slot context 2048; server build b11832-643b0cdac; queries as 'Instruct: Given a web search query, retrieve relevant passages that answer the query\nQuery:{text}', documents as '{text}'; <|endoftext|> added by the GGUF's tokenizer (add_eos_token); inputs must fit 2048 tokens (checked, never truncated)
- Reranker: llama.cpp ethicalabs/Qwen3-Reranker-8B-GGUF:Q8_0 as `qwen3-reranker-8b-q8_0` (pooling rank); file /home/briancorbin/.cache/huggingface/hub/models--ethicalabs--Qwen3-Reranker-8B-GGUF/snapshots/a521f5253a5c8c6e390af5458fdb736274127052/Qwen3-Reranker-8B.Q8_0.gguf; sha256 2f63dc6f2aa0b1f2b8f92004754049fe7c63954a9e6e856dc262c376d4e2cd26; 7567320064 params, n_embd 4096, n_ctx_train 40960; slot context 2048; server build b11832-643b0cdac; sanity pair 0.9571 > 1.17e-05; query + passage must fit 2048 tokens (refused, never truncated)
- Context tokens: cl100k_base via tiktoken (SHA-256 checked); counted over the text given to a reader (heading path + unit text, or the chunk)
- Golden set: src/main/resources/golden/questions.jsonl (answerable 47, unanswerable 6); run time 105 s

## Episode 4 main line and Episode 5 (cumulative)

Each row adds one step to the row above. Verdict against the previous row that ran: **pass** unless overall Hit@4 or all-evidence@4 drops, a stratum loses more than one question at Hit@4, or stale-in-top-4 rises.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| S1 | 1 element units + BM25 | 30/47 (64%) | – | 0.503 | 24 | 30 | 3 | 530 / 1227 | (start) |  |
| S2 | 2 + identifier lookup | 32/47 (68%) | – | 0.591 | 26 | 32 | 3 | 531 / 1227 | **pass** (vs S1) | +2 G-026 G-028 / −0 |
| S3 | 3 + lifecycle filter | 32/47 (68%) | – | 0.609 | 26 | 32 | 1 | 531 / 1227 | **pass** (vs S2) | +0 / −0 |
| S4 | 4 + dense, RRF k=60 | 37/47 (79%) | – | 0.704 | 34 | 37 | 1 | 581 / 1164 | **pass** (vs S3) | +6 G-006 G-016 G-018 G-020 G-079 G-081 / −1 G-036 |
| S5 | 5 + cross-encoder rerank top 50 | 44/47 (94%) | – | 0.860 | 40 | 41 | 1 | 638 / 1371 | **pass** (vs S4) | +8 G-007 G-011 G-012 G-014 G-015 G-017 G-036 G-078 / −1 G-029 |
| E5-1 | E5-1 + supersession redirect (history kept behind) | 44/47 (94%) | – | 0.870 | 40 | 41 | 1 | 638 / 1371 | **pass** (vs S5) | +0 / −0 |
| E5-2 | E5-2 + anchored typed expansion | 46/47 (98%) | – | 0.890 | 42 | 43 | 1 | 636 / 1371 | **pass** (vs E5-1) | +2 G-029 G-030 / −0 |

## Episode 5 on the BM25 line (no embedder involved)

The same two Episode 5 steps on top of step 3, so their effect can be read without any embedder.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| S3 | 3 + lifecycle filter | 32/47 (68%) | – | 0.609 | 26 | 32 | 1 | 531 / 1227 | (start) |  |
| X1 | S3 + supersession redirect (history kept behind) | 32/47 (68%) | – | 0.630 | 26 | 32 | 0 | 535 / 1227 | **pass** (vs S3) | +0 / −0 |
| X2 | S3 + redirect + anchored typed expansion | 33/47 (70%) | – | 0.641 | 28 | 34 | 0 | 526 / 1227 | **pass** (vs X1) | +1 G-030 / −0 |

## Variants, epilogue and ablations

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max |
|---|---|---|---|---|---|---|---|---|
| P0 | prior units, FTS tokenchars '-', BM25 | 28/47 (60%) | – | 0.452 | 22 | 30 | 3 | 486 / 1506 |
| P1 | prior units, FTS tokenchars '-', BM25 + prior id lookup + lifecycle | 32/47 (68%) | – | 0.566 | 26 | 33 | 1 | 487 / 1506 |
| P2 | prior units, default tokenizer, BM25 | 29/47 (62%) | – | 0.450 | 23 | 31 | 3 | 519 / 2124 |
| P3 | prior units, default tokenizer, BM25 + id lookup + lifecycle | 33/47 (70%) | – | 0.585 | 27 | 34 | 1 | 524 / 2124 |
| E5-1r | variant: redirect drops applied history (earlier prototype's rule) | 44/47 (94%) | – | 0.870 | 40 | 41 | 0 | 635 / 1371 |
| E5-2r | variant: E5-2 with the dropping redirect | 46/47 (98%) | – | 0.890 | 42 | 43 | 0 | 632 / 1371 |
| EpiB | epilogue: RRF-merge neighbours (k=60, w=0.5) instead of anchored expansion | 36/47 (77%) | – | 0.481 | 31 | 41 | 1 | 638 / 1371 |
| H1 | S3 + redirect + dense RRF (no reranker) | 37/47 (79%) | – | 0.715 | 34 | 37 | 0 | 581 / 1164 |
| H2 | H1 + anchored typed expansion (no reranker) | 38/47 (81%) | – | 0.726 | 36 | 39 | 0 | 580 / 1164 |
| A | naive 800-token chunks, dense only | 19/47 (40%) | 23 | 0.315 | 17 | 30 | 3 | 2818 / 3169 |
| B | naive 800-token chunks, BM25 only | 20/47 (43%) | 21 | 0.293 | 16 | 29 | 4 | 2819 / 3170 |
| C | element units, dense only | 28/47 (60%) | – | 0.517 | 27 | 35 | 3 | 592 / 1391 |
| D | element units, dense + id lookup + lifecycle | 33/47 (70%) | – | 0.675 | 32 | 37 | 1 | 608 / 1391 |
| E | hybrid without lifecycle filter (S4 minus step 3) | 37/47 (79%) | – | 0.690 | 34 | 37 | 3 | 587 / 1164 |
| F1 | hybrid without id lookup (S4 minus step 2) | 34/47 (72%) | – | 0.639 | 31 | 36 | 1 | 588 / 1164 |
| F2 | naive chunks, BM25 + dense RRF (no id lookup, no lifecycle) | 24/47 (51%) | 26 | 0.381 | 21 | 33 | 5 | 2862 / 3181 |
| F3 | element units, BM25 + dense RRF (no id lookup, no lifecycle) | 34/47 (72%) | – | 0.624 | 31 | 36 | 3 | 594 / 1164 |
| F4 | S3 without heading path and parts (prior units, Ep4 linker) | 33/47 (70%) | – | 0.585 | 27 | 34 | 1 | 524 / 2124 |
| F5 | S5 without dense (BM25 + id + lifecycle + rerank) | 39/47 (83%) | – | 0.777 | 35 | 36 | 1 | 632 / 1407 |
| F6 | S3 without parts (long units whole) | 34/47 (72%) | – | 0.602 | 28 | 33 | 1 | 563 / 2171 |
| F7 | S3 without heading path | 32/47 (68%) | – | 0.591 | 26 | 34 | 1 | 497 / 1257 |
| EP3 | Episode 3 run replayed (its real nomic top-4, rescored here) | 10/41 (24%) | 14 | 0.173 (@4) | 9 | 9 | 1 | 2809 / 3172 |

## Per stratum: Hit@4 · all@4 · all@10

| key | single-fact (10) | paraphrase (10) | identifier (8) | relation (7) | stale (7) | intent (5) | stale@4 |
|---|---|---|---|---|---|---|---|
| P0 | 8 · 7 · 9 | 2 · 2 · 4 | 4 · 4 · 5 | 6 · 1 · 3 | 6 · 6 · 7 | 2 · 2 · 2 | 3 |
| P1 | 8 · 7 · 9 | 2 · 2 · 4 | 8 · 8 · 8 | 6 · 1 · 3 | 6 · 6 · 7 | 2 · 2 · 2 | 1 |
| P2 | 8 · 7 · 8 | 2 · 2 · 6 | 4 · 4 · 5 | 6 · 1 · 3 | 7 · 7 · 7 | 2 · 2 · 2 | 3 |
| P3 | 8 · 7 · 8 | 2 · 2 · 6 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 2 · 2 · 2 | 1 |
| S1 | 8 · 7 · 7 | 2 · 2 · 5 | 6 · 6 · 6 | 6 · 1 · 3 | 7 · 7 · 7 | 1 · 1 · 2 | 3 |
| S2 | 8 · 7 · 7 | 2 · 2 · 5 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 1 · 1 · 2 | 3 |
| S3 | 8 · 7 · 7 | 2 · 2 · 5 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 1 · 1 · 2 | 1 |
| S4 | 9 · 8 · 8 | 5 · 5 · 8 | 8 · 8 · 8 | 5 · 3 · 3 | 7 · 7 · 7 | 3 · 3 · 3 | 1 |
| S5 | 10 · 9 · 9 | 10 · 10 · 10 | 8 · 8 · 8 | 5 · 2 · 3 | 7 · 7 · 7 | 4 · 4 · 4 | 1 |
| E5-1 | 10 · 9 · 9 | 10 · 10 · 10 | 8 · 8 · 8 | 5 · 2 · 3 | 7 · 7 · 7 | 4 · 4 · 4 | 1 |
| E5-2 | 10 · 9 · 9 | 10 · 10 · 10 | 8 · 8 · 8 | 7 · 4 · 5 | 7 · 7 · 7 | 4 · 4 · 4 | 1 |
| E5-1r | 10 · 9 · 9 | 10 · 10 · 10 | 8 · 8 · 8 | 5 · 2 · 3 | 7 · 7 · 7 | 4 · 4 · 4 | 0 |
| E5-2r | 10 · 9 · 9 | 10 · 10 · 10 | 8 · 8 · 8 | 7 · 4 · 5 | 7 · 7 · 7 | 4 · 4 · 4 | 0 |
| EpiB | 8 · 7 · 9 | 9 · 8 · 10 | 4 · 4 · 8 | 4 · 1 · 3 | 7 · 7 · 7 | 4 · 4 · 4 | 1 |
| X1 | 8 · 7 · 7 | 2 · 2 · 5 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 1 · 1 · 2 | 0 |
| X2 | 8 · 7 · 7 | 2 · 2 · 5 | 8 · 8 · 8 | 7 · 3 · 5 | 7 · 7 · 7 | 1 · 1 · 2 | 0 |
| H1 | 9 · 8 · 8 | 5 · 5 · 8 | 8 · 8 · 8 | 5 · 3 · 3 | 7 · 7 · 7 | 3 · 3 · 3 | 0 |
| H2 | 9 · 8 · 8 | 5 · 5 · 8 | 8 · 8 · 8 | 6 · 5 · 5 | 7 · 7 · 7 | 3 · 3 · 3 | 0 |
| A | 5 · 4 · 8 | 6 · 6 · 7 | 1 · 1 · 4 | 2 · 1 · 4 | 3 · 3 · 4 | 2 · 2 · 3 | 3 |
| B | 9 · 8 · 8 | 3 · 3 · 4 | 0 · 0 · 4 | 3 · 0 · 4 | 4 · 4 · 7 | 1 · 1 · 2 | 4 |
| C | 7 · 6 · 8 | 6 · 6 · 8 | 3 · 3 · 6 | 3 · 3 · 3 | 7 · 7 · 7 | 2 · 2 · 3 | 3 |
| D | 7 · 6 · 8 | 6 · 6 · 8 | 8 · 8 · 8 | 3 · 3 · 3 | 7 · 7 · 7 | 2 · 2 · 3 | 1 |
| E | 9 · 8 · 8 | 5 · 5 · 8 | 8 · 8 · 8 | 5 · 3 · 3 | 7 · 7 · 7 | 3 · 3 · 3 | 3 |
| F1 | 9 · 8 · 8 | 5 · 5 · 8 | 5 · 5 · 7 | 5 · 3 · 3 | 7 · 7 · 7 | 3 · 3 · 3 | 1 |
| F2 | 9 · 8 · 8 | 4 · 4 · 6 | 0 · 0 · 5 | 4 · 2 · 4 | 4 · 4 · 7 | 3 · 3 · 3 | 5 |
| F3 | 9 · 8 · 8 | 5 · 5 · 8 | 5 · 5 · 7 | 5 · 3 · 3 | 7 · 7 · 7 | 3 · 3 · 3 | 3 |
| F4 | 8 · 7 · 8 | 2 · 2 · 6 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 2 · 2 · 2 | 1 |
| F5 | 10 · 9 · 9 | 6 · 6 · 6 | 8 · 8 · 8 | 5 · 2 · 3 | 7 · 7 · 7 | 3 · 3 · 3 | 1 |
| F6 | 8 · 7 · 8 | 3 · 3 · 5 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 2 · 2 · 2 | 1 |
| F7 | 8 · 7 · 8 | 2 · 2 · 6 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 1 · 1 · 2 | 1 |
| EP3 | 5 · 5 · 5 | 2 · 2 · 2 | 1 · 1 · 1 | 1 · 0 · 0 | 1 · 1 · 1 | 0 · 0 · 0 | 1 |

## Checks against Episode 3 and the earlier prototype

- Naive chunks rebuilt: **2,147** (Episode 3 manifest: 2,147). Every Episode 3 "chunk N of M" total matches this splitter: 0 mismatches.
- Answerable questions with an expected element whole in some naive chunk: **40/44** (Episode 3: 38/44); expected elements whole in some chunk: 48/56.
- Episode 3's own retrieved top-4 lists, rescored by this scorer: Hit@4 10/44, Partial@4 14/44, MRR@4 0.17; single 5, paraphrase 2, identifier 1, relation 1, stale 1 (Episode 3 summary: 9, 11, 0.14; 5, 2, 1, 1, 0).
- Config A's top-4 equals Episode 3's top-4 (same chunks, same order) on **1/50** questions. With the same nomic-embed-text digest it should be at or near 50/50 (Ollama's batching can move near-ties; nomic through llama.cpp is another build of the same weights, so expect most, not all); with any other embedder it means nothing.

## Unanswerable questions: top-1 score of the last ranking stage (hint only)

Pinned identifier hits have no score and are left out. Scores are not comparable across rows (BM25, cosine, RRF and cross-encoder scales differ). Declining needs the full run with the chat model.

| key | answerable, unpinned: n · min · p25 · p50 · max | unanswerable: n · min · p25 · p50 · max |
|---|---|---|
| P0 | 47 · 13.3 · 19.5 · 24.7 · 54.3 | 6 · 11.5 · 13 · 15.8 · 33.8 |
| P1 | 37 · 13.3 · 19 · 26.5 · 54.3 | 6 · 11.2 · 11.5 · 15.8 · 33.8 |
| P2 | 47 · 13.7 · 19.7 · 26.5 · 55.9 | 6 · 11.4 · 12.5 · 15.5 · 31.6 |
| P3 | 37 · 13.7 · 18.8 · 27.8 · 55.9 | 6 · 11.2 · 11.4 · 15.5 · 31.6 |
| S1 | 47 · 13.8 · 19.3 · 26.5 · 55.1 | 6 · 11.7 · 12.3 · 15.4 · 31.1 |
| S2 | 37 · 13.8 · 19.2 · 26.8 · 55.1 | 6 · 11.7 · 12.3 · 15.4 · 31.1 |
| S3 | 37 · 13.8 · 19.2 · 26.8 · 55.1 | 6 · 11.1 · 11.7 · 15.4 · 31.1 |
| S4 | 37 · 0.0188 · 0.0313 · 0.0323 · 0.0328 | 6 · 0.0196 · 0.0214 · 0.0228 · 0.0323 |
| S5 | 37 · 0.915 · 0.993 · 0.998 · 1 | 6 · 0.106 · 0.684 · 0.75 · 0.869 |
| E5-1 | 37 · 0.915 · 0.993 · 0.998 · 1 | 6 · 0.106 · 0.684 · 0.75 · 0.869 |
| E5-2 | 37 · 0.915 · 0.993 · 0.998 · 1 | 6 · 0.106 · 0.684 · 0.75 · 0.869 |
| E5-1r | 37 · 0.915 · 0.993 · 0.998 · 1 | 6 · 0.106 · 0.684 · 0.75 · 0.869 |
| E5-2r | 37 · 0.915 · 0.993 · 0.998 · 1 | 6 · 0.106 · 0.684 · 0.75 · 0.869 |
| EpiB | 37 · 0.915 · 0.993 · 0.998 · 1 | 6 · 0.106 · 0.684 · 0.75 · 0.869 |
| X1 | 37 · 13.8 · 19.2 · 26.8 · 55.1 | 6 · 11.1 · 11.7 · 15.4 · 31.1 |
| X2 | 37 · 13.8 · 19.2 · 26.8 · 55.1 | 6 · 11.1 · 11.7 · 15.4 · 31.1 |
| H1 | 37 · 0.0188 · 0.0313 · 0.0323 · 0.0328 | 6 · 0.0196 · 0.0214 · 0.0228 · 0.0323 |
| H2 | 37 · 0.0188 · 0.0313 · 0.0323 · 0.0328 | 6 · 0.0196 · 0.0214 · 0.0228 · 0.0323 |
| A | 47 · 0.549 · 0.637 · 0.669 · 0.807 | 6 · 0.499 · 0.561 · 0.636 · 0.691 |
| B | 47 · 11.3 · 16.6 · 23.4 · 44.8 | 6 · 9.09 · 9.1 · 11.5 · 21.1 |
| C | 47 · 0.54 · 0.667 · 0.711 · 0.843 | 6 · 0.511 · 0.552 · 0.647 · 0.711 |
| D | 37 · 0.54 · 0.691 · 0.711 · 0.843 | 6 · 0.511 · 0.552 · 0.647 · 0.711 |
| E | 37 · 0.0183 · 0.0313 · 0.0323 · 0.0328 | 6 · 0.0195 · 0.0214 · 0.0246 · 0.0323 |
| F1 | 47 · 0.0188 · 0.0309 · 0.032 · 0.0328 | 6 · 0.0196 · 0.0214 · 0.0228 · 0.0323 |
| F2 | 47 · 0.0266 · 0.0313 · 0.0325 · 0.0328 | 6 · 0.0262 · 0.0278 · 0.0296 · 0.0323 |
| F3 | 47 · 0.0183 · 0.0309 · 0.032 · 0.0328 | 6 · 0.0195 · 0.0214 · 0.0246 · 0.0323 |
| F4 | 37 · 13.7 · 18.8 · 27.8 · 55.9 | 6 · 11.2 · 11.4 · 15.5 · 31.6 |
| F5 | 37 · 0.582 · 0.987 · 0.997 · 1 | 6 · 0.106 · 0.472 · 0.583 · 0.892 |
| F6 | 37 · 14 · 18.6 · 27.9 · 56.1 | 6 · 11.2 · 11.2 · 15.5 · 31.3 |
| F7 | 37 · 13.6 · 19.2 · 26.6 · 54.9 | 6 · 11.2 · 11.9 · 15.5 · 31.5 |
