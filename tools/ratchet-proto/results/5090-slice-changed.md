# The slice ratchet: llamacpp:qwen3-embedding-8b-q8_0 / llamacpp:qwen3-reranker-8b-q8_0

Run 2026-10-26 23:28 · spectastic c64d41c · 15,616 unit-states · 14 questions with a cutoff

Each question is answered against the archive as it stood at its own cutoff, so a retrieved address is the right revision by construction. G-075 is the abstention row: answered when no passage the golden set marks stale reaches the top four.

| key | configuration | answered |
|---|---|---|
| T5 | 5 + cross-encoder rerank | **8/14** |
| TD | 6 + the named document, reranked only when nothing was named | **10/14** |

## Per question

| question | T5 | TD |
|---|---|---|
| G-062 as-of | rank 2 | rank 2 |
| G-063 as-of | rank 1 | rank 1 |
| G-064 as-of | rank 1 | rank 1 |
| G-065 as-of | rank 1 | rank 1 |
| G-066 as-of | rank 2 | rank 2 |
| G-067 what-changed | miss | (33) |
| G-068 what-changed | rank 3 | rank 3 |
| G-069 what-changed | miss | miss |
| G-070 what-changed | miss | miss |
| G-071 what-changed | (22) | rank 3 |
| G-072 as-known | rank 1 | rank 1 |
| G-073 as-known | miss | (29) |
| G-074 as-known | (5) | rank 1 |
| G-075 as-known | abstained | abstained |
