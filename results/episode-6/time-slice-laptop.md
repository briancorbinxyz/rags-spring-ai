# The slice ratchet: ollama:nomic-embed-text:v1.5 / cross-encoder/ms-marco-MiniLM-L-6-v2

Run 2026-10-26 18:37 · spectastic c64d41c · 15,616 unit-states · 9 questions with a cutoff

Each question is answered against the archive as it stood at its own cutoff, so a retrieved address is the right revision by construction. G-075 is the abstention row: answered when no passage the golden set marks stale reaches the top four.

| key | configuration | answered |
|---|---|---|
| T3 | 3 + standing as of then | **5/9** |
| T5 | 5 + cross-encoder rerank | **6/9** |
| T7 | variant: + the named spec ranked and fused, no reranker | **6/9** |
| TD | 6 + the named document, reranked only when nothing was named | **8/9** |
| TE | variant: TD reading a bare spec as its spec.html | **8/9** |

## Per question

| question | T3 | T5 | T7 | TD | TE |
|---|---|---|---|---|---|
| G-062 as-of | (30) | rank 1 | (7) | rank 1 | rank 1 |
| G-063 as-of | miss | miss | rank 4 | rank 4 | rank 3 |
| G-064 as-of | rank 2 | rank 1 | rank 2 | rank 1 | rank 1 |
| G-065 as-of | rank 2 | rank 1 | rank 3 | rank 1 | rank 1 |
| G-066 as-of | miss | rank 4 | (21) | rank 4 | rank 4 |
| G-072 as-known | rank 1 | rank 1 | rank 1 | rank 1 | rank 1 |
| G-073 as-known | miss | miss | miss | (28) | (28) |
| G-074 as-known | rank 3 | (14) | rank 2 | rank 1 | rank 1 |
| G-075 as-known | abstained | abstained | abstained | abstained | abstained |
