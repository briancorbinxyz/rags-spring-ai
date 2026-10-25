# Ratchet prototype: llamacpp:nomic-embed-text-v1.5-f16

Run 2026-10-24 20:14 · spectastic c64d41c · 10,540 element units in 11,492 items (units over 250 words split into parts) · 10,540 prior-prototype units · 2,147 naive chunks

- Embedder: llama.cpp nomic-ai/nomic-embed-text-v1.5-GGUF:F16 as `nomic-embed-text-v1.5-f16` (pooling mean); file /home/briancorbin/.cache/huggingface/hub/models--nomic-ai--nomic-embed-text-v1.5-GGUF/snapshots/0188c9bf409793f810680a5a431e7b899c46104c/nomic-embed-text-v1.5.f16.gguf; sha256 f7af6f66802f4df86eda10fe9bbcfc75c39562bed48ef6ace719a251cf1c2fdb; 136727040 params, n_embd 768, n_ctx_train 2048; slot context 2048; server build b11832-643b0cdac; queries as 'search_query: {text}', documents as 'search_document: {text}'; inputs must fit 2048 tokens (checked, never truncated)
- Longest embedder input embedded in this run: 42 tokens (texts already in the vector cache are not re-counted)
- Reranker: llama.cpp gpustack/bge-reranker-v2-m3-GGUF:FP16 as `bge-reranker-v2-m3-fp16` (pooling rank); file /home/briancorbin/.cache/huggingface/hub/models--gpustack--bge-reranker-v2-m3-GGUF/snapshots/3093af03b1a635e67b084b1d8c03c5f5e020fd05/bge-reranker-v2-m3-FP16.gguf; sha256 5df93be121c09c43432102ad2b9569d369ccb85c209ca7583e8ccd28f0e41b88; 567753729 params, n_embd 1024, n_ctx_train 8192; slot context 2048; server build b11832-643b0cdac; sanity pair 7.794 > -11.04; query + passage must fit 2048 tokens (refused, never truncated)
- Context tokens: cl100k_base via tiktoken (SHA-256 checked); counted over the text given to a reader (heading path + unit text, or the chunk)
- Golden set: src/main/resources/golden/questions-heldout.jsonl (answerable 13, unanswerable 0); run time 5 s

## Episode 4 main line and Episode 5 (cumulative)

Each row adds one step to the row above. Verdict against the previous row that ran: **pass** unless overall Hit@4 or all-evidence@4 drops, a stratum loses more than one question at Hit@4, or stale-in-top-4 rises.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| S3 | 3 + lifecycle filter | 2/13 (15%) | – | 0.070 | 0 | 0 | 0 | 644 / 1053 | (start) |  |
| E5-1 | E5-1 + supersession redirect (superseded history dropped) | 4/13 (31%) | – | 0.229 | 3 | 3 | 0 | 956 / 1449 | **pass** (vs S3) | +2 H-004 H-006 / −0 |

## Episode 5 on the BM25 line (no embedder involved)

The same two Episode 5 steps on top of step 3, so their effect can be read without any embedder.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| S3 | 3 + lifecycle filter | 2/13 (15%) | – | 0.070 | 0 | 0 | 0 | 644 / 1053 | (start) |  |
| X1 | S3 + supersession redirect (superseded history dropped) | 2/13 (15%) | – | 0.082 | 1 | 1 | 0 | 637 / 1053 | **pass** (vs S3) | +0 / −0 |

## Variants, epilogue and ablations

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max |
|---|---|---|---|---|---|---|---|---|
| H1 | S3 + redirect + dense RRF (no reranker) | 2/13 (15%) | – | 0.112 | 0 | 0 | 0 | 758 / 1109 |

## Per stratum: Hit@4 · all@4 · all@10

| key | relation (8) | history (5) | stale@4 |
|---|---|---|---|
| S3 | 0 · 0 · 0 | 2 · 0 · 0 | 0 |
| E5-1 | 2 · 1 · 1 | 2 · 2 · 2 | 0 |
| X1 | 0 · 0 · 0 | 2 · 1 · 1 | 0 |
| H1 | 1 · 0 · 0 | 1 · 0 · 0 | 0 |

## Checks against Episode 3 and the earlier prototype

- Naive chunks rebuilt: **2,147** (Episode 3 manifest: 2,147). Every Episode 3 "chunk N of M" total matches this splitter: 0 mismatches.
- Answerable questions with an expected element whole in some naive chunk: **12/44** (Episode 3: 38/44); expected elements whole in some chunk: 19/24.

## Unanswerable questions: top-1 score of the last ranking stage (hint only)

Pinned identifier hits have no score and are left out. Scores are not comparable across rows (BM25, cosine, RRF and cross-encoder scales differ). Declining needs the full run with the chat model.

| key | answerable, unpinned: n · min · p25 · p50 · max | unanswerable: n · min · p25 · p50 · max |
|---|---|---|
| S3 | 12 · 14.2 · 16.9 · 20.1 · 30.2 | – |
| E5-1 | 12 · -4.33 · -1.33 · 0.434 · 4.11 | – |
| X1 | 12 · 14.2 · 16.9 · 20.1 · 30.2 | – |
| H1 | 12 · 0.0234 · 0.0298 · 0.031 · 0.0328 | – |
