# The slice ratchet: llamacpp:qwen3-embedding-8b-q8_0 / llamacpp:qwen3-reranker-8b-q8_0

Run 2026-10-26 19:41 · spectastic c64d41c · 15,616 unit-states · 9 questions with a cutoff

Each question is answered against the archive as it stood at its own cutoff, so a retrieved address is the right revision by construction. G-075 is the abstention row: answered when no passage the golden set marks stale reaches the top four.

| key | configuration | answered |
|---|---|---|
| T3 | 3 + standing as of then | **5/9** |
| T5 | 5 + cross-encoder rerank | **7/9** |
| TD | 6 + the named document, reranked only when nothing was named | **8/9** |
| TJ | 8 + what a reader picks out of the named scope, pinned | **8/9** |

## Per question

| question | T3 | T5 | TD | TJ |
|---|---|---|---|---|
| G-062 as-of | (30) | rank 2 | rank 2 | rank 2 |
| G-063 as-of | miss | rank 1 | rank 1 | rank 1 |
| G-064 as-of | rank 2 | rank 1 | rank 1 | rank 1 |
| G-065 as-of | rank 2 | rank 1 | rank 1 | rank 1 |
| G-066 as-of | miss | rank 2 | rank 2 | rank 2 |
| G-072 as-known | rank 1 | rank 1 | rank 1 | rank 1 |
| G-073 as-known | miss | miss | (29) | rank 2 |
| G-074 as-known | rank 3 | (5) | rank 1 | (5) |
| G-075 as-known | abstained | abstained | abstained | abstained |
