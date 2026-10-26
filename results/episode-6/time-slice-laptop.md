# The slice ratchet: ollama:nomic-embed-text:v1.5 / cross-encoder/ms-marco-MiniLM-L-6-v2

Run 2026-10-26 18:33 · spectastic c64d41c · 15,616 unit-states · 9 questions with a cutoff

Each question is answered against the archive as it stood at its own cutoff, so a retrieved address is the right revision by construction. G-075 is the abstention row: answered when no passage the golden set marks stale reaches the top four.

| key | configuration | answered |
|---|---|---|
| T1 | 1 BM25 over the slice | **4/9** |
| T2 | 2 + identifier lookup | **5/9** |
| T3 | 3 + standing as of then | **5/9** |
| T4 | 4 + dense, RRF k=60 | **5/9** |
| T5 | 5 + cross-encoder rerank | **6/9** |
| T6 | variant: + the named spec admitted to the rerank window | **6/9** |
| T7 | variant: + the named spec ranked and fused, no reranker | **5/9** |
| T9 | variant: the reranker as an RRF vote rather than the last word | **5/9** |
| TD | 6 + the named document, reranked only when nothing was named | **7/9** |
| TE | variant: TD reading a bare spec as its spec.html | **8/9** |

## Per question

| question | T1 | T2 | T3 | T4 | T5 | T6 | T7 | T9 | TD | TE |
|---|---|---|---|---|---|---|---|---|---|---|
| G-062 as-of | (33) | (33) | (30) | (7) | rank 1 | rank 1 | (7) | rank 2 | rank 1 | rank 1 |
| G-063 as-of | miss | miss | miss | miss | miss | (41) | (5) | (8) | (5) | rank 4 |
| G-064 as-of | rank 2 | rank 2 | rank 2 | rank 2 | rank 1 | rank 1 | rank 2 | rank 1 | rank 1 | rank 1 |
| G-065 as-of | rank 2 | rank 2 | rank 2 | rank 3 | rank 1 | rank 1 | rank 3 | rank 1 | rank 1 | rank 1 |
| G-066 as-of | miss | miss | miss | (21) | rank 4 | rank 4 | (21) | (8) | rank 4 | rank 4 |
| G-072 as-known | (50) | rank 1 | rank 1 | rank 1 | rank 1 | rank 1 | rank 1 | rank 1 | rank 1 | rank 1 |
| G-073 as-known | miss | miss | miss | miss | miss | miss | miss | miss | (49) | (49) |
| G-074 as-known | rank 3 | rank 3 | rank 3 | rank 3 | (14) | (32) | rank 2 | (7) | rank 1 | rank 1 |
| G-075 as-known | abstained | abstained | abstained | abstained | abstained | abstained | abstained | abstained | abstained | abstained |
