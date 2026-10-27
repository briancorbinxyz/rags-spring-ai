# The slice ratchet: llamacpp:qwen3-embedding-8b-q8_0 / llamacpp:qwen3-reranker-8b-q8_0

Run 2026-10-27 00:09 · spectastic c64d41c · 15,616 unit-states · 14 questions with a cutoff

Each question is answered against the archive as it stood at its own cutoff, so a retrieved address is the right revision by construction. G-075 is the abstention row: answered when no passage the golden set marks stale reaches the top four.

`what-changed` is scored apart and never counted in `answered`: *located* is the address in the top four, *told* is located **and** the address's chain of states rebuilt from the archive equal to the declared `versions`.

| key | configuration | answered | what-changed: located · told |
|---|---|---|---|
| T5 | 5 + cross-encoder rerank | **7/9** | 1 · 1 of 5 |
| T7 | variant: + the named spec ranked and fused, no reranker | **6/9** | 0 · 0 of 5 |
| TF | 7 + the named document, reranked everywhere | **8/9** | 1 · 1 of 5 |

## Per question

| question | T5 | T7 | TF |
|---|---|---|---|
| G-062 as-of | rank 2 | (8) | rank 2 |
| G-063 as-of | rank 1 | rank 1 | rank 1 |
| G-064 as-of | rank 1 | rank 2 | rank 1 |
| G-065 as-of | rank 1 | rank 2 | rank 1 |
| G-066 as-of | rank 2 | (43) | rank 2 |
| G-067 what-changed | (19) | (31) | (18) |
| G-068 what-changed | rank 4 | (9) | rank 4 |
| G-069 what-changed | miss | miss | miss |
| G-070 what-changed | miss | miss | miss |
| G-071 what-changed | (22) | (13) | (16) |
| G-072 as-known | rank 1 | rank 1 | rank 1 |
| G-073 as-known | miss | miss | rank 2 |
| G-074 as-known | (5) | rank 1 | (5) |
| G-075 as-known | abstained | abstained | abstained |
