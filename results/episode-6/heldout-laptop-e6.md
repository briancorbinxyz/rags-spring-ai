# Ratchet prototype: ollama:nomic-embed-text:v1.5

Run 2026-10-26 21:35 · spectastic c64d41c · 10,540 element units in 11,492 items (units over 250 words split into parts) · 10,540 prior-prototype units · 2,147 naive chunks

- Embedder: Ollama model nomic-embed-text:v1.5, digest 0a109f422b47e3a30ba2b10eca18548e944e8a23073ee3f3e947efcf3c45e59f
- Reranker: reranker cross-encoder/ms-marco-MiniLM-L-6-v2 via sentence-transformers
- Context tokens: cl100k_base via tiktoken (SHA-256 checked); counted over the text given to a reader (heading path + unit text, or the chunk)
- Golden set: /Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions-heldout.jsonl (answerable 13, unanswerable 0); run time 13 s

## Episode 4 main line and Episode 5 (cumulative)

Each row adds one step to the row above. Verdict against the previous row that ran: **pass** unless overall Hit@4 or all-evidence@4 drops, a stratum loses more than one question at Hit@4, or stale-in-top-4 rises.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| E5-2 | E5-2 + anchored typed expansion | 5/13 (38%) | – | 0.353 | 2 | 4 | 0 | 790 / 1172 | (start) |  |

## Episode 5 on the BM25 line (no embedder involved)

The same two Episode 5 steps on top of step 3, so their effect can be read without any embedder.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|

## Variants, epilogue and ablations

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max |
|---|---|---|---|---|---|---|---|---|
| G9 | Episode 6: the arms that survived their ratchet | 6/13 (46%) | – | 0.441 | 3 | 4 | 0 | 749 / 951 |

## Per stratum: Hit@4 · all@4 · all@10

| key | relation (8) | history (5) | stale@4 |
|---|---|---|---|
| E5-2 | 3 · 1 · 2 | 2 · 1 · 2 | 0 |
| G9 | 3 · 1 · 2 | 3 · 2 · 2 | 0 |

## Checks against Episode 3 and the earlier prototype

- Naive chunks rebuilt: **2,147** (Episode 3 manifest: 2,147). Every Episode 3 "chunk N of M" total matches this splitter: 0 mismatches.
- Answerable questions with an expected element whole in some naive chunk: **12/44** (Episode 3: 38/44); expected elements whole in some chunk: 19/24.

## Unanswerable questions: top-1 score of the last ranking stage (hint only)

Pinned identifier hits have no score and are left out. Scores are not comparable across rows (BM25, cosine, RRF and cross-encoder scales differ). Declining needs the full run with the chat model.

| key | answerable, unpinned: n · min · p25 · p50 · max | unanswerable: n · min · p25 · p50 · max |
|---|---|---|
| E5-2 | 12 · -3.54 · 1.4 · 2.04 · 4.59 | – |
| G9 | 12 · -3.54 · 1.31 · 2.04 · 5.09 | – |
