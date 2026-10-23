# Ratchet prototype: llamacpp:nomic-embed-text-v1.5-f16

Run 2026-10-23 00:44 · spectastic c64d41c · 10,540 element units in 11,492 items (units over 250 words split into parts) · 10,540 prior-prototype units · 2,147 naive chunks

- Embedder: llama.cpp nomic-ai/nomic-embed-text-v1.5-GGUF:F16 as `nomic-embed-text-v1.5-f16` (pooling mean); file /home/briancorbin/.cache/huggingface/hub/models--nomic-ai--nomic-embed-text-v1.5-GGUF/snapshots/0188c9bf409793f810680a5a431e7b899c46104c/nomic-embed-text-v1.5.f16.gguf; sha256 f7af6f66802f4df86eda10fe9bbcfc75c39562bed48ef6ace719a251cf1c2fdb; 136727040 params, n_embd 768, n_ctx_train 2048; slot context 2048; server build b11832-643b0cdac; queries as 'search_query: {text}', documents as 'search_document: {text}'; inputs must fit 2048 tokens (checked, never truncated)
- Longest embedder input embedded in this run: 1043 tokens (texts already in the vector cache are not re-counted)
- Reranker: llama.cpp gpustack/bge-reranker-v2-m3-GGUF:FP16 as `bge-reranker-v2-m3-fp16` (pooling rank); file /home/briancorbin/.cache/huggingface/hub/models--gpustack--bge-reranker-v2-m3-GGUF/snapshots/3093af03b1a635e67b084b1d8c03c5f5e020fd05/bge-reranker-v2-m3-FP16.gguf; sha256 5df93be121c09c43432102ad2b9569d369ccb85c209ca7583e8ccd28f0e41b88; 567753729 params, n_embd 1024, n_ctx_train 8192; slot context 2048; server build b11832-643b0cdac; sanity pair 7.794 > -11.04; query + passage must fit 2048 tokens (refused, never truncated)
- Context tokens: cl100k_base via tiktoken (SHA-256 checked); counted over the text given to a reader (heading path + unit text, or the chunk)
- Golden set: src/main/resources/golden/questions.jsonl (answerable 47, unanswerable 6); run time 64 s

## Episode 4 main line and Episode 5 (cumulative)

Each row adds one step to the row above. Verdict against the previous row that ran: **pass** unless overall Hit@4 or all-evidence@4 drops, a stratum loses more than one question at Hit@4, or stale-in-top-4 rises.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| S1 | 1 element units + BM25 | 30/47 (64%) | – | 0.503 | 24 | 30 | 3 | 530 / 1227 | (start) |  |
| S2 | 2 + identifier lookup | 32/47 (68%) | – | 0.591 | 26 | 32 | 3 | 531 / 1227 | **pass** (vs S1) | +2 G-026 G-028 / −0 |
| S3 | 3 + lifecycle filter | 32/47 (68%) | – | 0.609 | 26 | 32 | 1 | 531 / 1227 | **pass** (vs S2) | +0 / −0 |
| S4 | 4 + dense, RRF k=60 | 36/47 (77%) | – | 0.688 | 33 | 34 | 0 | 664 / 1341 | **fail**: relation 6→4 (vs S3) | +6 G-006 G-016 G-017 G-018 G-020 G-079 / −2 G-031 G-036 |
| S5 | 5 + cross-encoder rerank top 50 | 35/47 (74%) | – | 0.638 | 31 | 35 | 2 | 838 / 1539 | **fail**: Hit@4 36→35; all@4 33→31; stale@4 0→2 (vs S4) | +2 G-007 G-031 / −3 G-002 G-017 G-029 |
| E5-1 | E5-1 + supersession redirect (history kept behind) | 35/47 (74%) | – | 0.652 | 31 | 35 | 1 | 843 / 1539 | **pass** (vs S5) | +0 / −0 |
| E5-2 | E5-2 + anchored typed expansion | 37/47 (79%) | – | 0.670 | 33 | 37 | 1 | 830 / 1539 | **pass** (vs E5-1) | +2 G-029 G-030 / −0 |

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
| E5-1r | variant: redirect drops applied history (earlier prototype's rule) | 35/47 (74%) | – | 0.652 | 31 | 35 | 0 | 834 / 1539 |
| E5-2r | variant: E5-2 with the dropping redirect | 37/47 (79%) | – | 0.670 | 33 | 37 | 0 | 821 / 1539 |
| EpiB | epilogue: RRF-merge neighbours (k=60, w=0.5) instead of anchored expansion | 27/47 (57%) | – | 0.348 | 24 | 35 | 2 | 756 / 1539 |
| H1 | S3 + redirect + dense RRF (no reranker) | 36/47 (77%) | – | 0.688 | 33 | 34 | 0 | 664 / 1341 |
| H2 | H1 + anchored typed expansion (no reranker) | 37/47 (79%) | – | 0.702 | 35 | 36 | 0 | 664 / 1341 |
| A | naive 800-token chunks, dense only | 10/47 (21%) | 13 | 0.179 | 9 | 13 | 1 | 2850 / 3172 |
| B | naive 800-token chunks, BM25 only | 20/47 (43%) | 21 | 0.293 | 16 | 29 | 4 | 2819 / 3170 |
| C | element units, dense only | 26/47 (55%) | – | 0.448 | 24 | 27 | 1 | 738 / 1345 |
| D | element units, dense + id lookup + lifecycle | 31/47 (66%) | – | 0.568 | 29 | 32 | 0 | 747 / 1184 |
| E | hybrid without lifecycle filter (S4 minus step 3) | 36/47 (77%) | – | 0.674 | 33 | 34 | 2 | 656 / 1251 |
| F1 | hybrid without id lookup (S4 minus step 2) | 33/47 (70%) | – | 0.612 | 30 | 31 | 0 | 668 / 1341 |
| F2 | naive chunks, BM25 + dense RRF (no id lookup, no lifecycle) | 15/47 (32%) | 20 | 0.276 | 14 | 21 | 3 | 2811 / 3191 |
| F3 | element units, BM25 + dense RRF (no id lookup, no lifecycle) | 33/47 (70%) | – | 0.598 | 30 | 31 | 2 | 660 / 1251 |
| F4 | S3 without heading path and parts (prior units, Ep4 linker) | 33/47 (70%) | – | 0.585 | 27 | 34 | 1 | 524 / 2124 |
| F5 | S5 without dense (BM25 + id + lifecycle + rerank) | 35/47 (74%) | – | 0.668 | 32 | 36 | 2 | 789 / 1610 |
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
| S4 | 9 · 8 · 9 | 6 · 6 · 6 | 8 · 8 · 8 | 4 · 2 · 2 | 7 · 7 · 7 | 2 · 2 · 2 | 0 |
| S5 | 9 · 8 · 9 | 5 · 5 · 6 | 8 · 8 · 8 | 4 · 1 · 3 | 7 · 7 · 7 | 2 · 2 · 2 | 2 |
| E5-1 | 9 · 8 · 9 | 5 · 5 · 6 | 8 · 8 · 8 | 4 · 1 · 3 | 7 · 7 · 7 | 2 · 2 · 2 | 1 |
| E5-2 | 9 · 8 · 9 | 5 · 5 · 6 | 8 · 8 · 8 | 6 · 3 · 5 | 7 · 7 · 7 | 2 · 2 · 2 | 1 |
| E5-1r | 9 · 8 · 9 | 5 · 5 · 6 | 8 · 8 · 8 | 4 · 1 · 3 | 7 · 7 · 7 | 2 · 2 · 2 | 0 |
| E5-2r | 9 · 8 · 9 | 5 · 5 · 6 | 8 · 8 · 8 | 6 · 3 · 5 | 7 · 7 · 7 | 2 · 2 · 2 | 0 |
| EpiB | 5 · 5 · 8 | 3 · 3 · 6 | 6 · 6 · 8 | 5 · 2 · 4 | 6 · 6 · 7 | 2 · 2 · 2 | 2 |
| X1 | 8 · 7 · 7 | 2 · 2 · 5 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 1 · 1 · 2 | 0 |
| X2 | 8 · 7 · 7 | 2 · 2 · 5 | 8 · 8 · 8 | 7 · 3 · 5 | 7 · 7 · 7 | 1 · 1 · 2 | 0 |
| H1 | 9 · 8 · 9 | 6 · 6 · 6 | 8 · 8 · 8 | 4 · 2 · 2 | 7 · 7 · 7 | 2 · 2 · 2 | 0 |
| H2 | 9 · 8 · 9 | 6 · 6 · 6 | 8 · 8 · 8 | 5 · 4 · 4 | 7 · 7 · 7 | 2 · 2 · 2 | 0 |
| A | 5 · 5 · 6 | 2 · 2 · 3 | 1 · 1 · 1 | 1 · 0 · 1 | 1 · 1 · 2 | 0 · 0 · 0 | 1 |
| B | 9 · 8 · 8 | 3 · 3 · 4 | 0 · 0 · 4 | 3 · 0 · 4 | 4 · 4 · 7 | 1 · 1 · 2 | 4 |
| C | 8 · 7 · 8 | 4 · 4 · 5 | 3 · 3 · 4 | 3 · 2 · 2 | 6 · 6 · 6 | 2 · 2 · 2 | 1 |
| D | 8 · 7 · 8 | 4 · 4 · 5 | 8 · 8 · 8 | 3 · 2 · 2 | 6 · 6 · 7 | 2 · 2 · 2 | 0 |
| E | 9 · 8 · 9 | 6 · 6 · 6 | 8 · 8 · 8 | 4 · 2 · 2 | 7 · 7 · 7 | 2 · 2 · 2 | 2 |
| F1 | 9 · 8 · 9 | 6 · 6 · 6 | 5 · 5 · 5 | 4 · 2 · 2 | 7 · 7 · 7 | 2 · 2 · 2 | 0 |
| F2 | 8 · 7 · 8 | 2 · 2 · 4 | 1 · 1 · 3 | 1 · 1 · 2 | 2 · 2 · 3 | 1 · 1 · 1 | 3 |
| F3 | 9 · 8 · 9 | 6 · 6 · 6 | 5 · 5 · 5 | 4 · 2 · 2 | 7 · 7 · 7 | 2 · 2 · 2 | 2 |
| F4 | 8 · 7 · 8 | 2 · 2 · 6 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 2 · 2 · 2 | 1 |
| F5 | 9 · 8 · 9 | 4 · 4 · 5 | 8 · 8 · 8 | 4 · 2 · 4 | 7 · 7 · 7 | 3 · 3 · 3 | 2 |
| F6 | 8 · 7 · 8 | 3 · 3 · 5 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 2 · 2 · 2 | 1 |
| F7 | 8 · 7 · 8 | 2 · 2 · 6 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 1 · 1 · 2 | 1 |
| EP3 | 5 · 5 · 5 | 2 · 2 · 2 | 1 · 1 · 1 | 1 · 0 · 0 | 1 · 1 · 1 | 0 · 0 · 0 | 1 |

## Checks against Episode 3 and the earlier prototype

- Naive chunks rebuilt: **2,147** (Episode 3 manifest: 2,147). Every Episode 3 "chunk N of M" total matches this splitter: 0 mismatches.
- Answerable questions with an expected element whole in some naive chunk: **40/44** (Episode 3: 38/44); expected elements whole in some chunk: 48/56.
- Episode 3's own retrieved top-4 lists, rescored by this scorer: Hit@4 10/44, Partial@4 14/44, MRR@4 0.17; single 5, paraphrase 2, identifier 1, relation 1, stale 1 (Episode 3 summary: 9, 11, 0.14; 5, 2, 1, 1, 0).
- Config A's top-4 equals Episode 3's top-4 (same chunks, same order) on **43/50** questions. With the same nomic-embed-text digest it should be at or near 50/50 (Ollama's batching can move near-ties; nomic through llama.cpp is another build of the same weights, so expect most, not all); with any other embedder it means nothing.

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
| S4 | 37 · 0.0228 · 0.0311 · 0.032 · 0.0328 | 6 · 0.0164 · 0.0215 · 0.0239 · 0.0315 |
| S5 | 37 · -5.97 · -0.00146 · 1.57 · 6.88 | 6 · -2.91 · -2.45 · -0.838 · 0.297 |
| E5-1 | 37 · -5.97 · -0.00146 · 1.57 · 6.88 | 6 · -2.91 · -2.45 · -0.838 · 0.297 |
| E5-2 | 37 · -5.97 · -0.00146 · 1.57 · 6.88 | 6 · -2.91 · -2.45 · -0.838 · 0.297 |
| E5-1r | 37 · -5.97 · -0.00146 · 1.57 · 6.88 | 6 · -2.91 · -2.45 · -0.838 · 0.297 |
| E5-2r | 37 · -5.97 · -0.00146 · 1.57 · 6.88 | 6 · -2.91 · -2.45 · -0.838 · 0.297 |
| EpiB | 37 · -5.97 · -0.00146 · 1.57 · 6.88 | 6 · -2.91 · -2.45 · -0.838 · 0.297 |
| X1 | 37 · 13.8 · 19.2 · 26.8 · 55.1 | 6 · 11.1 · 11.7 · 15.4 · 31.1 |
| X2 | 37 · 13.8 · 19.2 · 26.8 · 55.1 | 6 · 11.1 · 11.7 · 15.4 · 31.1 |
| H1 | 37 · 0.0228 · 0.0311 · 0.032 · 0.0328 | 6 · 0.0164 · 0.0215 · 0.0239 · 0.0315 |
| H2 | 37 · 0.0228 · 0.0311 · 0.032 · 0.0328 | 6 · 0.0164 · 0.0215 · 0.0239 · 0.0315 |
| A | 47 · 0.671 · 0.726 · 0.751 · 0.823 | 6 · 0.65 · 0.69 · 0.692 · 0.768 |
| B | 47 · 11.3 · 16.6 · 23.4 · 44.8 | 6 · 9.09 · 9.1 · 11.5 · 21.1 |
| C | 47 · 0.679 · 0.742 · 0.764 · 0.862 | 6 · 0.654 · 0.717 · 0.718 · 0.768 |
| D | 37 · 0.691 · 0.744 · 0.765 · 0.837 | 6 · 0.654 · 0.701 · 0.718 · 0.768 |
| E | 37 · 0.0227 · 0.0311 · 0.032 · 0.0328 | 6 · 0.0164 · 0.0214 · 0.0261 · 0.0315 |
| F1 | 47 · 0.0228 · 0.0311 · 0.032 · 0.0328 | 6 · 0.0164 · 0.0215 · 0.0239 · 0.0315 |
| F2 | 47 · 0.0227 · 0.0301 · 0.0323 · 0.0328 | 6 · 0.0233 · 0.0256 · 0.0267 · 0.0323 |
| F3 | 47 · 0.0227 · 0.0311 · 0.032 · 0.0328 | 6 · 0.0164 · 0.0214 · 0.0261 · 0.0315 |
| F4 | 37 · 13.7 · 18.8 · 27.8 · 55.9 | 6 · 11.2 · 11.4 · 15.5 · 31.6 |
| F5 | 37 · -5.13 · -0.32 · 1.57 · 6.88 | 6 · -3.89 · -3.14 · -2.68 · 0.297 |
| F6 | 37 · 14 · 18.6 · 27.9 · 56.1 | 6 · 11.2 · 11.2 · 15.5 · 31.3 |
| F7 | 37 · 13.6 · 19.2 · 26.6 · 54.9 | 6 · 11.2 · 11.9 · 15.5 · 31.5 |
