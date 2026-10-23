# Ratchet prototype: llamacpp:qwen3-embedding-8b-q8_0

Run 2026-10-23 19:22 · spectastic c64d41c · 10,540 element units in 11,492 items (units over 250 words split into parts) · 10,540 prior-prototype units · 2,147 naive chunks

- Embedder: llama.cpp Qwen/Qwen3-Embedding-8B-GGUF:Q8_0 as `qwen3-embedding-8b-q8_0` (pooling last); file /home/briancorbin/.cache/huggingface/hub/models--Qwen--Qwen3-Embedding-8B-GGUF/snapshots/69d0e58a13e463cd99a9b83e3f5fee7c10265fab/Qwen3-Embedding-8B-Q8_0.gguf; sha256 d20ddc71e8a5c4344f2343481e242233a997dc5eaff442427a945836c97b4deb; 7567295488 params, n_embd 4096, n_ctx_train 40960; slot context 2048; server build b11832-643b0cdac; queries as 'Instruct: Given a web search query, retrieve relevant passages that answer the query\nQuery:{text}', documents as '{text}'; <|endoftext|> added by the GGUF's tokenizer (add_eos_token); inputs must fit 2048 tokens (checked, never truncated)
- Reranker: llama.cpp gpustack/bge-reranker-v2-m3-GGUF:FP16 as `bge-reranker-v2-m3-fp16` (pooling rank); file /home/briancorbin/.cache/huggingface/hub/models--gpustack--bge-reranker-v2-m3-GGUF/snapshots/3093af03b1a635e67b084b1d8c03c5f5e020fd05/bge-reranker-v2-m3-FP16.gguf; sha256 5df93be121c09c43432102ad2b9569d369ccb85c209ca7583e8ccd28f0e41b88; 567753729 params, n_embd 1024, n_ctx_train 8192; slot context 2048; server build b11832-643b0cdac; sanity pair 7.794 > -11.04; query + passage must fit 2048 tokens (refused, never truncated)
- Context tokens: cl100k_base via tiktoken (SHA-256 checked); counted over the text given to a reader (heading path + unit text, or the chunk)
- Golden set: src/main/resources/golden/questions.jsonl (answerable 47, unanswerable 6); run time 12 s

## Episode 4 main line and Episode 5 (cumulative)

Each row adds one step to the row above. Verdict against the previous row that ran: **pass** unless overall Hit@4 or all-evidence@4 drops, a stratum loses more than one question at Hit@4, or stale-in-top-4 rises.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| S3 | 3 + lifecycle filter | 32/47 (68%) | – | 0.609 | 26 | 32 | 1 | 531 / 1227 | (start) |  |
| E5-1 | E5-1 + supersession redirect (superseded history dropped) | 36/47 (77%) | – | 0.718 | 32 | 37 | 0 | 812 / 1492 | **fail**: relation 6→4 (vs S3) | +8 G-006 G-007 G-016 G-018 G-020 G-078 G-079 G-081 / −4 G-002 G-013 G-029 G-036 |

## Episode 5 on the BM25 line (no embedder involved)

The same two Episode 5 steps on top of step 3, so their effect can be read without any embedder.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| S3 | 3 + lifecycle filter | 32/47 (68%) | – | 0.609 | 26 | 32 | 1 | 531 / 1227 | (start) |  |
| X1 | S3 + supersession redirect (superseded history dropped) | 32/47 (68%) | – | 0.630 | 26 | 32 | 0 | 536 / 1227 | **pass** (vs S3) | +0 / −0 |

## Variants, epilogue and ablations

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max |
|---|---|---|---|---|---|---|---|---|
| E5-1b | variant: redirect keeps applied history behind its successor (Episode 4's first rule) | 36/47 (77%) | – | 0.718 | 32 | 37 | 1 | 821 / 1492 |
| X1b | variant: X1 with the keeping redirect | 32/47 (68%) | – | 0.630 | 26 | 32 | 0 | 535 / 1227 |
| H1 | S3 + redirect + dense RRF (no reranker) | 37/47 (79%) | – | 0.715 | 34 | 37 | 0 | 574 / 1152 |
| H1b | variant: H1 with the keeping redirect | 37/47 (79%) | – | 0.715 | 34 | 37 | 0 | 581 / 1164 |
| EP3 | Episode 3 run replayed (its real nomic top-4, rescored here) | 10/41 (24%) | 14 | 0.173 (@4) | 9 | 9 | 1 | 2809 / 3172 |

## Per stratum: Hit@4 · all@4 · all@10

| key | single-fact (10) | paraphrase (10) | identifier (8) | relation (7) | stale (7) | intent (5) | stale@4 |
|---|---|---|---|---|---|---|---|
| S3 | 8 · 7 · 7 | 2 · 2 · 5 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 1 · 1 · 2 | 1 |
| E5-1 | 9 · 8 · 9 | 4 · 4 · 6 | 8 · 8 · 8 | 4 · 1 · 3 | 7 · 7 · 7 | 4 · 4 · 4 | 0 |
| E5-1b | 9 · 8 · 9 | 4 · 4 · 6 | 8 · 8 · 8 | 4 · 1 · 3 | 7 · 7 · 7 | 4 · 4 · 4 | 1 |
| X1 | 8 · 7 · 7 | 2 · 2 · 5 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 1 · 1 · 2 | 0 |
| X1b | 8 · 7 · 7 | 2 · 2 · 5 | 8 · 8 · 8 | 6 · 1 · 3 | 7 · 7 · 7 | 1 · 1 · 2 | 0 |
| H1 | 9 · 8 · 8 | 5 · 5 · 8 | 8 · 8 · 8 | 5 · 3 · 3 | 7 · 7 · 7 | 3 · 3 · 3 | 0 |
| H1b | 9 · 8 · 8 | 5 · 5 · 8 | 8 · 8 · 8 | 5 · 3 · 3 | 7 · 7 · 7 | 3 · 3 · 3 | 0 |
| EP3 | 5 · 5 · 5 | 2 · 2 · 2 | 1 · 1 · 1 | 1 · 0 · 0 | 1 · 1 · 1 | 0 · 0 · 0 | 1 |

## Checks against Episode 3 and the earlier prototype

- Naive chunks rebuilt: **2,147** (Episode 3 manifest: 2,147). Every Episode 3 "chunk N of M" total matches this splitter: 0 mismatches.
- Answerable questions with an expected element whole in some naive chunk: **40/44** (Episode 3: 38/44); expected elements whole in some chunk: 48/56.
- Episode 3's own retrieved top-4 lists, rescored by this scorer: Hit@4 10/44, Partial@4 14/44, MRR@4 0.17; single 5, paraphrase 2, identifier 1, relation 1, stale 1 (Episode 3 summary: 9, 11, 0.14; 5, 2, 1, 1, 0).

## Unanswerable questions: top-1 score of the last ranking stage (hint only)

Pinned identifier hits have no score and are left out. Scores are not comparable across rows (BM25, cosine, RRF and cross-encoder scales differ). Declining needs the full run with the chat model.

| key | answerable, unpinned: n · min · p25 · p50 · max | unanswerable: n · min · p25 · p50 · max |
|---|---|---|
| S3 | 37 · 13.8 · 19.2 · 26.8 · 55.1 | 6 · 11.1 · 11.7 · 15.4 · 31.1 |
| E5-1 | 37 · -5.13 · -0.178 · 1.57 · 6.88 | 6 · -2.91 · -1.14 · -0.97 · 0.3 |
| E5-1b | 37 · -5.13 · -0.178 · 1.57 · 6.88 | 6 · -2.91 · -1.14 · -0.97 · 0.3 |
| X1 | 37 · 13.8 · 19.2 · 26.8 · 55.1 | 6 · 11.1 · 11.7 · 15.4 · 31.1 |
| X1b | 37 · 13.8 · 19.2 · 26.8 · 55.1 | 6 · 11.1 · 11.7 · 15.4 · 31.1 |
| H1 | 37 · 0.0188 · 0.0313 · 0.0323 · 0.0328 | 6 · 0.0196 · 0.0214 · 0.0228 · 0.0323 |
| H1b | 37 · 0.0188 · 0.0313 · 0.0323 · 0.0328 | 6 · 0.0196 · 0.0214 · 0.0228 · 0.0323 |
