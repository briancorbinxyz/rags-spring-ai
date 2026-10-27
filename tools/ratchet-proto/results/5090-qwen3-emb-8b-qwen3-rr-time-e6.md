# Ratchet prototype: llamacpp:qwen3-embedding-8b-q8_0

Run 2026-10-27 00:56 · spectastic c64d41c · 10,540 element units in 11,492 items (units over 250 words split into parts) · 10,540 prior-prototype units · 2,147 naive chunks

- Embedder: llama.cpp Qwen/Qwen3-Embedding-8B-GGUF:Q8_0 as `qwen3-embedding-8b-q8_0` (pooling last); file /home/briancorbin/.cache/huggingface/hub/models--Qwen--Qwen3-Embedding-8B-GGUF/snapshots/69d0e58a13e463cd99a9b83e3f5fee7c10265fab/Qwen3-Embedding-8B-Q8_0.gguf; sha256 d20ddc71e8a5c4344f2343481e242233a997dc5eaff442427a945836c97b4deb; 7567295488 params, n_embd 4096, n_ctx_train 40960; slot context 2048; server build b11832-643b0cdac; queries as 'Instruct: Given a web search query, retrieve relevant passages that answer the query\nQuery:{text}', documents as '{text}'; <|endoftext|> added by the GGUF's tokenizer (add_eos_token); inputs must fit 2048 tokens (checked, never truncated)
- Reranker: llama.cpp ethicalabs/Qwen3-Reranker-8B-GGUF:Q8_0 as `qwen3-reranker-8b-q8_0` (pooling rank); file /home/briancorbin/.cache/huggingface/hub/models--ethicalabs--Qwen3-Reranker-8B-GGUF/snapshots/a521f5253a5c8c6e390af5458fdb736274127052/Qwen3-Reranker-8B.Q8_0.gguf; sha256 2f63dc6f2aa0b1f2b8f92004754049fe7c63954a9e6e856dc262c376d4e2cd26; 7567320064 params, n_embd 4096, n_ctx_train 40960; slot context 2048; server build b11832-643b0cdac; sanity pair 0.9571 > 1.17e-05; query + passage must fit 2048 tokens (refused, never truncated)
- Context tokens: cl100k_base via tiktoken (SHA-256 checked); counted over the text given to a reader (heading path + unit text, or the chunk)
- Golden set: src/main/resources/golden/questions-time.jsonl (answerable 24, unanswerable 0); run time 44 s

## Episode 4 main line and Episode 5 (cumulative)

Each row adds one step to the row above. Verdict against the previous row that ran: **pass** unless overall Hit@4 or all-evidence@4 drops, a stratum loses more than one question at Hit@4, or stale-in-top-4 rises.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| E5-2 | E5-2 + anchored typed expansion | 11/24 (46%) | – | 0.349 | 10 | 10 | 10 | 880 / 1354 | (start) |  |

## Episode 5 on the BM25 line (no embedder involved)

The same two Episode 5 steps on top of step 3, so their effect can be read without any embedder.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|

## Variants, epilogue and ablations

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max |
|---|---|---|---|---|---|---|---|---|
| G5 | E5-2 + a history question may see what the filter hides | 13/24 (54%) | – | 0.429 | 12 | 12 | 11 | 914 / 1354 |
| G7 | G5 + the redirect yields to a history question | 16/24 (67%) | – | 0.532 | 13 | 13 | 9 | 930 / 1354 |
| G9 | Episode 6: the arms that survived their ratchet | 15/24 (62%) | – | 0.500 | 13 | 13 | 9 | 926 / 1354 |
| GA | G9 + a why-question may see the archive's #context sections | 15/24 (62%) | – | 0.500 | 13 | 13 | 9 | 926 / 1354 |

## Per stratum: Hit@4 · all@4 · all@10

| key | as-known (4) | as-of (5) | history (7) | history-decoy (3) | what-changed (5) | stale@4 |
|---|---|---|---|---|---|---|
| E5-2 | 1 · 1 · 1 | 5 · 5 · 5 | 0 · 0 · 0 | 3 · 2 · 2 | 2 · 2 · 2 | 10 |
| G5 | 1 · 1 · 1 | 5 · 5 · 5 | 2 · 2 · 2 | 3 · 2 · 2 | 2 · 2 · 2 | 11 |
| G7 | 1 · 1 · 1 | 5 · 5 · 5 | 5 · 3 · 3 | 3 · 2 · 2 | 2 · 2 · 2 | 9 |
| G9 | 1 · 1 · 1 | 5 · 5 · 5 | 4 · 3 · 3 | 3 · 2 · 2 | 2 · 2 · 2 | 9 |
| GA | 1 · 1 · 1 | 5 · 5 · 5 | 4 · 3 · 3 | 3 · 2 · 2 | 2 · 2 · 2 | 9 |

## Checks against Episode 3 and the earlier prototype

- Naive chunks rebuilt: **2,147** (Episode 3 manifest: 2,147). Every Episode 3 "chunk N of M" total matches this splitter: 0 mismatches.
- Answerable questions with an expected element whole in some naive chunk: **14/44** (Episode 3: 38/44); expected elements whole in some chunk: 18/30.

## Unanswerable questions: top-1 score of the last ranking stage (hint only)

Pinned identifier hits have no score and are left out. Scores are not comparable across rows (BM25, cosine, RRF and cross-encoder scales differ). Declining needs the full run with the chat model.

| key | answerable, unpinned: n · min · p25 · p50 · max | unanswerable: n · min · p25 · p50 · max |
|---|---|---|
| E5-2 | 23 · 0.858 · 0.936 · 0.972 · 1 | – |
| G5 | 23 · 0.858 · 0.941 · 0.988 · 1 | – |
| G7 | 23 · 0.858 · 0.941 · 0.988 · 1 | – |
| G9 | 23 · 0.858 · 0.941 · 0.975 · 1 | – |
| GA | 23 · 0.858 · 0.941 · 0.975 · 1 | – |
