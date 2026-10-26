# Ratchet prototype: ollama:nomic-embed-text:v1.5

Run 2026-10-26 15:57 · spectastic c64d41c · 10,540 element units in 11,492 items (units over 250 words split into parts) · 10,540 prior-prototype units · 2,147 naive chunks

- Embedder: Ollama model nomic-embed-text:v1.5, digest 0a109f422b47e3a30ba2b10eca18548e944e8a23073ee3f3e947efcf3c45e59f
- Reranker: reranker cross-encoder/ms-marco-MiniLM-L-6-v2 via sentence-transformers
- Context tokens: cl100k_base via tiktoken (SHA-256 checked); counted over the text given to a reader (heading path + unit text, or the chunk)
- Golden set: /Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions-time.jsonl (answerable 24, unanswerable 0); run time 29 s

## Episode 4 main line and Episode 5 (cumulative)

Each row adds one step to the row above. Verdict against the previous row that ran: **pass** unless overall Hit@4 or all-evidence@4 drops, a stratum loses more than one question at Hit@4, or stale-in-top-4 rises.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| S3 | 3 + lifecycle filter | 4/24 (17%) | – | 0.081 | 4 | 5 | 4 | 613 / 1024 | (start) |  |
| E5-1 | E5-1 + supersession redirect (superseded history dropped) | 7/24 (29%) | – | 0.252 | 6 | 10 | 6 | 914 / 1281 | **fail**: stale@4 4→6 (vs S3) | +5 G-059 G-061 G-062 G-063 G-064 / −2 G-057 G-074 |
| E5-2 | E5-2 + anchored typed expansion | 7/24 (29%) | – | 0.252 | 6 | 10 | 6 | 914 / 1281 | **pass** (vs E5-1) | +0 / −0 |

## Episode 5 on the BM25 line (no embedder involved)

The same two Episode 5 steps on top of step 3, so their effect can be read without any embedder.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| S3 | 3 + lifecycle filter | 4/24 (17%) | – | 0.081 | 4 | 5 | 4 | 613 / 1024 | (start) |  |
| X1 | S3 + supersession redirect (superseded history dropped) | 3/24 (12%) | – | 0.104 | 3 | 4 | 4 | 614 / 1130 | **fail**: Hit@4 4→3; all@4 4→3 (vs S3) | +0 / −1 G-057 |

## Variants, epilogue and ablations

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max |
|---|---|---|---|---|---|---|---|---|
| E5-1b | variant: redirect keeps applied history behind its successor (Episode 4's first rule) | 7/24 (29%) | – | 0.252 | 6 | 10 | 6 | 913 / 1247 |
| E5-2b | variant: E5-2 with the keeping redirect | 7/24 (29%) | – | 0.252 | 6 | 10 | 6 | 913 / 1247 |
| X1b | variant: X1 with the keeping redirect | 3/24 (12%) | – | 0.104 | 3 | 4 | 4 | 614 / 1024 |
| H1 | S3 + redirect + dense RRF (no reranker) | 8/24 (33%) | – | 0.251 | 8 | 10 | 8 | 824 / 1306 |
| H1b | variant: H1 with the keeping redirect | 8/24 (33%) | – | 0.251 | 8 | 10 | 8 | 830 / 1207 |

## Per stratum: Hit@4 · all@4 · all@10

| key | as-known (4) | as-of (5) | history (7) | history-decoy (3) | what-changed (5) | stale@4 |
|---|---|---|---|---|---|---|
| S3 | 1 · 1 · 1 | 1 · 1 · 2 | 1 · 1 · 1 | 1 · 1 · 1 | 0 · 0 · 0 | 4 |
| E5-1 | 0 · 0 · 0 | 4 · 4 · 5 | 0 · 0 · 0 | 3 · 2 · 3 | 0 · 0 · 2 | 6 |
| E5-2 | 0 · 0 · 0 | 4 · 4 · 5 | 0 · 0 · 0 | 3 · 2 · 3 | 0 · 0 · 2 | 6 |
| E5-1b | 0 · 0 · 0 | 4 · 4 · 5 | 0 · 0 · 0 | 3 · 2 · 3 | 0 · 0 · 2 | 6 |
| E5-2b | 0 · 0 · 0 | 4 · 4 · 5 | 0 · 0 · 0 | 3 · 2 · 3 | 0 · 0 · 2 | 6 |
| X1 | 1 · 1 · 1 | 1 · 1 · 2 | 0 · 0 · 0 | 1 · 1 · 1 | 0 · 0 · 0 | 4 |
| X1b | 1 · 1 · 1 | 1 · 1 · 2 | 0 · 0 · 0 | 1 · 1 · 1 | 0 · 0 · 0 | 4 |
| H1 | 1 · 1 · 1 | 4 · 4 · 5 | 0 · 0 · 0 | 2 · 2 · 3 | 1 · 1 · 1 | 8 |
| H1b | 1 · 1 · 1 | 4 · 4 · 5 | 0 · 0 · 0 | 2 · 2 · 3 | 1 · 1 · 1 | 8 |

## Checks against Episode 3 and the earlier prototype

- Naive chunks rebuilt: **2,147** (Episode 3 manifest: 2,147). Every Episode 3 "chunk N of M" total matches this splitter: 0 mismatches.
- Answerable questions with an expected element whole in some naive chunk: **14/44** (Episode 3: 38/44); expected elements whole in some chunk: 18/30.

## Unanswerable questions: top-1 score of the last ranking stage (hint only)

Pinned identifier hits have no score and are left out. Scores are not comparable across rows (BM25, cosine, RRF and cross-encoder scales differ). Declining needs the full run with the chat model.

| key | answerable, unpinned: n · min · p25 · p50 · max | unanswerable: n · min · p25 · p50 · max |
|---|---|---|
| S3 | 23 · 15.7 · 21.8 · 28.2 · 49.9 | – |
| E5-1 | 23 · -2.02 · 1.12 · 1.87 · 5.49 | – |
| E5-2 | 23 · -2.02 · 1.12 · 1.87 · 5.49 | – |
| E5-1b | 23 · -2.02 · 1.12 · 1.87 · 5.49 | – |
| E5-2b | 23 · -2.02 · 1.12 · 1.87 · 5.49 | – |
| X1 | 23 · 15.7 · 21.8 · 28.2 · 49.9 | – |
| X1b | 23 · 15.7 · 21.8 · 28.2 · 49.9 | – |
| H1 | 23 · 0.0203 · 0.0287 · 0.0306 · 0.0328 | – |
| H1b | 23 · 0.0203 · 0.0287 · 0.0306 · 0.0328 | – |
