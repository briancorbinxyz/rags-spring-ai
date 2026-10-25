# Ratchet prototype: llamacpp:qwen3-embedding-8b-q8_0

Run 2026-10-24 22:36 · spectastic c64d41c · 10,540 element units in 11,492 items (units over 250 words split into parts) · 10,540 prior-prototype units · 2,147 naive chunks

- Embedder: llama.cpp Qwen/Qwen3-Embedding-8B-GGUF:Q8_0 as `qwen3-embedding-8b-q8_0` (pooling last); file /home/briancorbin/.cache/huggingface/hub/models--Qwen--Qwen3-Embedding-8B-GGUF/snapshots/69d0e58a13e463cd99a9b83e3f5fee7c10265fab/Qwen3-Embedding-8B-Q8_0.gguf; sha256 d20ddc71e8a5c4344f2343481e242233a997dc5eaff442427a945836c97b4deb; 7567295488 params, n_embd 4096, n_ctx_train 40960; slot context 2048; server build b11832-643b0cdac; queries as 'Instruct: Given a web search query, retrieve relevant passages that answer the query\nQuery:{text}', documents as '{text}'; <|endoftext|> added by the GGUF's tokenizer (add_eos_token); inputs must fit 2048 tokens (checked, never truncated)
- Reranker: llama.cpp ethicalabs/Qwen3-Reranker-8B-GGUF:Q8_0 as `qwen3-reranker-8b-q8_0` (pooling rank); file /home/briancorbin/.cache/huggingface/hub/models--ethicalabs--Qwen3-Reranker-8B-GGUF/snapshots/a521f5253a5c8c6e390af5458fdb736274127052/Qwen3-Reranker-8B.Q8_0.gguf; sha256 2f63dc6f2aa0b1f2b8f92004754049fe7c63954a9e6e856dc262c376d4e2cd26; 7567320064 params, n_embd 4096, n_ctx_train 40960; slot context 2048; server build b11832-643b0cdac; sanity pair 0.9571 > 1.17e-05; query + passage must fit 2048 tokens (refused, never truncated)
- Context tokens: cl100k_base via tiktoken (SHA-256 checked); counted over the text given to a reader (heading path + unit text, or the chunk)
- Golden set: src/main/resources/golden/questions.jsonl (answerable 47, unanswerable 6); run time 169 s

## Episode 4 main line and Episode 5 (cumulative)

Each row adds one step to the row above. Verdict against the previous row that ran: **pass** unless overall Hit@4 or all-evidence@4 drops, a stratum loses more than one question at Hit@4, or stale-in-top-4 rises.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| E5-1 | E5-1 + supersession redirect (superseded history dropped) | 44/47 (94%) | – | 0.870 | 40 | 42 | 0 | 635 / 1371 | (start) |  |
| E5-2 | E5-2 + anchored typed expansion | 46/47 (98%) | – | 0.890 | 42 | 44 | 0 | 632 / 1371 | **pass** (vs E5-1) | +2 G-029 G-030 / −0 |

## Episode 5 on the BM25 line (no embedder involved)

The same two Episode 5 steps on top of step 3, so their effect can be read without any embedder.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|

## Variants, epilogue and ablations

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max |
|---|---|---|---|---|---|---|---|---|
| E6-1 | E5-1 + sibling artifacts of the best-ranked specs, reranked | 44/47 (94%) | – | 0.870 | 40 | 42 | 0 | 645 / 1407 |
| E6-2 | E5-2 + sibling artifacts (typed expansion and the family edge together) | 46/47 (98%) | – | 0.890 | 42 | 44 | 0 | 642 / 1407 |
| EP3 | Episode 3 run replayed (its real nomic top-4, rescored here) | 10/41 (24%) | 14 | 0.173 (@4) | 9 | 9 | 1 | 2809 / 3172 |

## Per stratum: Hit@4 · all@4 · all@10

| key | single-fact (10) | paraphrase (10) | identifier (8) | relation (7) | stale (7) | intent (5) | stale@4 |
|---|---|---|---|---|---|---|---|
| E5-1 | 10 · 9 · 10 | 10 · 10 · 10 | 8 · 8 · 8 | 5 · 2 · 3 | 7 · 7 · 7 | 4 · 4 · 4 | 0 |
| E5-2 | 10 · 9 · 10 | 10 · 10 · 10 | 8 · 8 · 8 | 7 · 4 · 5 | 7 · 7 · 7 | 4 · 4 · 4 | 0 |
| E6-1 | 10 · 9 · 10 | 10 · 10 · 10 | 8 · 8 · 8 | 5 · 2 · 3 | 7 · 7 · 7 | 4 · 4 · 4 | 0 |
| E6-2 | 10 · 9 · 10 | 10 · 10 · 10 | 8 · 8 · 8 | 7 · 4 · 5 | 7 · 7 · 7 | 4 · 4 · 4 | 0 |
| EP3 | 5 · 5 · 5 | 2 · 2 · 2 | 1 · 1 · 1 | 1 · 0 · 0 | 1 · 1 · 1 | 0 · 0 · 0 | 1 |

## Checks against Episode 3 and the earlier prototype

- Naive chunks rebuilt: **2,147** (Episode 3 manifest: 2,147). Every Episode 3 "chunk N of M" total matches this splitter: 0 mismatches.
- Answerable questions with an expected element whole in some naive chunk: **40/44** (Episode 3: 38/44); expected elements whole in some chunk: 48/56.
- Episode 3's own retrieved top-4 lists, rescored by this scorer: Hit@4 10/44, Partial@4 14/44, MRR@4 0.17; single 5, paraphrase 2, identifier 1, relation 1, stale 1 (Episode 3 summary: 9, 11, 0.14; 5, 2, 1, 1, 0).

## Unanswerable questions: top-1 score of the last ranking stage (hint only)

Pinned identifier hits have no score and are left out. Scores are not comparable across rows (BM25, cosine, RRF and cross-encoder scales differ). Declining needs the full run with the chat model.

| key | answerable, unpinned: n · min · p25 · p50 · max | unanswerable: n · min · p25 · p50 · max |
|---|---|---|
| E5-1 | 37 · 0.915 · 0.993 · 0.998 · 1 | 6 · 0.106 · 0.684 · 0.75 · 0.869 |
| E5-2 | 37 · 0.915 · 0.993 · 0.998 · 1 | 6 · 0.106 · 0.684 · 0.75 · 0.869 |
| E6-1 | 37 · 0.915 · 0.993 · 0.998 · 1 | 6 · 0.106 · 0.684 · 0.75 · 0.869 |
| E6-2 | 37 · 0.915 · 0.993 · 0.998 · 1 | 6 · 0.106 · 0.684 · 0.75 · 0.869 |
