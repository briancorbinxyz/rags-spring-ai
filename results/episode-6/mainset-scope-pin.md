# Ratchet prototype: ollama:nomic-embed-text:v1.5

Run 2026-10-26 19:16 · spectastic c64d41c · 10,540 element units in 11,492 items (units over 250 words split into parts) · 10,540 prior-prototype units · 2,147 naive chunks

- Embedder: Ollama model nomic-embed-text:v1.5, digest 0a109f422b47e3a30ba2b10eca18548e944e8a23073ee3f3e947efcf3c45e59f
- Reranker: reranker cross-encoder/ms-marco-MiniLM-L-6-v2 via sentence-transformers
- Context tokens: cl100k_base via tiktoken (SHA-256 checked); counted over the text given to a reader (heading path + unit text, or the chunk)
- Golden set: src/main/resources/golden/questions.jsonl (answerable 47, unanswerable 6); run time 56 s

## Episode 4 main line and Episode 5 (cumulative)

Each row adds one step to the row above. Verdict against the previous row that ran: **pass** unless overall Hit@4 or all-evidence@4 drops, a stratum loses more than one question at Hit@4, or stale-in-top-4 rises.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|
| E5-2 | E5-2 + anchored typed expansion | 33/47 (70%) | – | 0.674 | 31 | 37 | 0 | 720 / 1371 | (start) |  |

## Episode 5 on the BM25 line (no embedder involved)

The same two Episode 5 steps on top of step 3, so their effect can be read without any embedder.

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max | verdict | won / lost at Hit@4 |
|---|---|---|---|---|---|---|---|---|---|---|

## Variants, epilogue and ablations

| key | configuration | Hit@4 | Partial@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx tokens mean / max |
|---|---|---|---|---|---|---|---|---|
| G4 | G2 + what a reader picks out of the named scope, pinned | 33/47 (70%) | – | 0.670 | 31 | 37 | 0 | 702 / 1371 |
| EP3 | Episode 3 run replayed (its real nomic top-4, rescored here) | 10/41 (24%) | 14 | 0.173 (@4) | 9 | 9 | 1 | 2809 / 3172 |

## Per stratum: Hit@4 · all@4 · all@10

| key | single-fact (10) | paraphrase (10) | identifier (8) | relation (7) | stale (7) | intent (5) | stale@4 |
|---|---|---|---|---|---|---|---|
| E5-2 | 7 · 7 · 10 | 3 · 3 · 6 | 8 · 8 · 8 | 6 · 4 · 4 | 7 · 7 · 7 | 2 · 2 · 2 | 0 |
| G4 | 7 · 7 · 10 | 3 · 3 · 6 | 8 · 8 · 8 | 6 · 4 · 4 | 7 · 7 · 7 | 2 · 2 · 2 | 0 |
| EP3 | 5 · 5 · 5 | 2 · 2 · 2 | 1 · 1 · 1 | 1 · 0 · 0 | 1 · 1 · 1 | 0 · 0 · 0 | 1 |

## Checks against Episode 3 and the earlier prototype

- Naive chunks rebuilt: **2,147** (Episode 3 manifest: 2,147). Every Episode 3 "chunk N of M" total matches this splitter: 0 mismatches.
- Answerable questions with an expected element whole in some naive chunk: **40/44** (Episode 3: 38/44); expected elements whole in some chunk: 48/56.
- Episode 3's own retrieved top-4 lists, rescored by this scorer: Hit@4 10/44, Partial@4 14/44, MRR@4 0.17; single 5, paraphrase 2, identifier 1, relation 1, stale 1 (Episode 3 summary: 9, 11, 0.14; 5, 2, 1, 1, 0).

## Unanswerable questions: top-1 score of the last ranking stage (hint only)

Pinned identifier hits have no score and are left out. Scores are not comparable across rows (BM25, cosine, RRF and cross-encoder scales differ). Declining needs the full run with the chat model.

| key | answerable, unpinned: n · min · p25 · p50 · max | unanswerable: n · min · p25 · p50 · max |
|---|---|---|
| E5-2 | 37 · -6.59 · 1.34 · 4.3 · 7.82 | 6 · -4.74 · -2.44 · 0.278 · 1.21 |
| G4 | 32 · -6.59 · 0.641 · 3.78 · 7.82 | 6 · -4.74 · -2.44 · 0.278 · 1.21 |
