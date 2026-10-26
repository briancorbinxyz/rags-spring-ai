# The slice ratchet: ollama:nomic-embed-text:v1.5 / cross-encoder/ms-marco-MiniLM-L-6-v2

Run 2026-10-26 19:03 · spectastic c64d41c · 15,616 unit-states · 9 questions with a cutoff

Each question is answered against the archive as it stood at its own cutoff, so a retrieved address is the right revision by construction. G-075 is the abstention row: answered when no passage the golden set marks stale reaches the top four.

| key | configuration | answered |
|---|---|---|
| T5 | 5 + cross-encoder rerank | **6/9** |
| TD | 6 + the named document, reranked only when nothing was named | **8/9** |
| TH | 8 + a listwise reader on the named scope, the cross-encoder on the rest | **7/9** |

## Per question

| question | T5 | TD | TH |
|---|---|---|---|
| G-062 as-of | rank 1 | rank 1 | rank 1 |
| G-063 as-of | miss | rank 4 | (5) |
| G-064 as-of | rank 1 | rank 1 | rank 1 |
| G-065 as-of | rank 1 | rank 1 | rank 1 |
| G-066 as-of | rank 4 | rank 4 | rank 4 |
| G-072 as-known | rank 1 | rank 1 | rank 1 |
| G-073 as-known | miss | (28) | (16) |
| G-074 as-known | (14) | rank 1 | rank 1 |
| G-075 as-known | abstained | abstained | abstained |
