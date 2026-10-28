# Internal evidence: what our hand-written routers actually score

Measured 27 Oct 2026 by the coordinator, not from literature. Scripts:
`scratchpad/lists.py`, `lists2.py`, `survive.py`, `survive2.py`, `wc_arm.py`.
All 90 questions: main 53, time 24, held-out 13.

## The three English lists are classifiers nobody scored

Each reads the question and picks a structural slice to search. None was ever measured as a
classifier. Scored per row, independently (ground truth: does the golden set expect a passage in
that section / document / lifecycle):

| list | fires | wanted | precision | recall |
|---|---:|---:|---:|---:|
| `WHY` -> `#context` | 19 | 5 | **21%** | 80% |
| `HISTORY_MARKERS` -> withdrawn + archived | 20 | 8 | **25%** | 62% |
| `DOC_KINDS` `task list` -> `tasks.html` | 6 | 6 | 50% | 50% |
| `DOC_KINDS` `design` -> `design.html` | 16 | 17 | 50% | 47% |
| `DOC_KINDS` `verification` -> `verify.html` | 3 | 1 | 33% | 100% |
| `DOC_KINDS` `triage log` -> `triage-log.html` | 2 | 5 | **0%** | **0%** |
| `DOC_KINDS` `plan` -> `plan.html` | 3 | 1 | **0%** | **0%** |

`WHY`'s 21% is not a discovery: its own comment states 4/19 and calls it "poor precision on
purpose". The measurement confirms the comment and extends it to the other two lists.

**Two rows have never been right once.** `triage log` fires twice, neither question wants it, and
misses all five that do. `plan` fires three times, none wants it, and misses the one that does
(G-072) **because the file was renamed to `design.html` on 30 July 2026** and the row still names a
path that no longer exists. The rename argument is visible in the confusion matrix.

## `_named_kind` scored as it is actually used

The per-row table overstates each row's traffic, because `_named_kind` returns the FIRST match in
table order. The real decision, scored generously (a question whose evidence spans two documents
counts correct if the router names either):

```
names a document    27  (30% of 90)
and is right        11  ->  precision 41%
stays silent        63
```

**It is wrong more often than right when it commits**, and it is silent on 54 questions whose
evidence sits in exactly one document. Its confusions are not noise, they are systematic:

```
 4x  said design.html      answer in spec.html
 2x  said triage-log.html  answer in design.html
 1x  said design.html      answer in plan.html     <- G-072, the rename
```

Why the architecture survives this: the routed result is **fused, never committed**
(`scope_rrf`), and an empty scope silently returns `[]` (`pipeline.py:363`), so the open ranking
covers. The cost is that **no measurement can distinguish "the scope arm was not needed" from
"the scope arm asked for a document that does not exist at that position."**

## The window predicates for `what-changed`

Candidate-set definitions replacing `changed_between(lo, hi)`; neither reads a score.
*across* = in force at both window ends, text differs. *within* = absent at the window start, took
more than one state inside it.

| id | `changed_between` | across | within | union | answer in |
|---|---:|---:|---:|---:|---|
| G-067 | 360 | 25 | 17 | 42 | within |
| G-068 | 109 | 8 | 5 | 13 | across |
| G-069 | 381 | 19 | 0 | 19 | across |
| G-070 | 4,434 | 96 | 363 | 459 | across |
| G-071 | 250 | 12 | 15 | 27 | across |

All five answers reachable; haystack cut 10-100x. Ranked with BM25 + dense, **no reranker**:

| id | `changed_between` rank | window-union rank |
|---|---:|---:|
| G-067 | absent | 24 |
| G-068 | 1 | 1 |
| G-069 | 40 | **4** |
| G-070 | 33 | 8 |
| G-071 | 5 | 6 |
| **Hit@4** | **1/5** | **2/5** |

The restrict is clearly the right mechanism and clearly not sufficient alone. Open question for the
report: does a cross-encoder over 13-459 candidates convert ranks 6 and 8 into top-4, and is that
a reader improvement or a fit?

## Caveats on the above

- Ground truth for document routing is "the golden set expects a passage in this file", which is a
  proxy for "the question named this document". **Precision is the sound half**; recall mixes
  "router missed a named document" with "the question never named one".
- `wants_retired` reads `/withdrawn/` or `/archive/` in the expected path. A born-withdrawn record
  not under those paths would be missed.
- n is tiny throughout. A 21% precision on 19 firings has a wide interval; the 0%/0% rows are the
  only ones where the direction is beyond doubt.

## Correction: one predicate, not two (and the reranked result)

The research found a false negative the two-part version cannot see: **change-and-change-back
(A->B->A)**, where the endpoint texts match although the address moved twice. The fix drops the text
comparison entirely — **a state boundary strictly inside the window**, partitioned on presence at
the window start. Simpler, cheaper, and it subsumes both halves.

Candidate sets are the same or near-identical, and it exposes two classes the two-part version
cannot express:

| id | boundary (moved) | born-once | repealed | two-part |
|---|---:|---:|---:|---:|
| G-067 | 42 | 211 | 0 | 42 |
| G-068 | 13 | 82 | 0 | 13 |
| G-069 | 20 | 361 | 0 | 19 |
| G-070 | 517 | 3,598 | 0 | 459 |
| G-071 | 27 | 175 | 0 | 27 |

**`born-once` — created inside the window, never touched again — is the narrating proposal, and it
is 4-10x larger than the set that actually moved.** That is "the story of a change outranks the
change" as a number rather than a phrase. `repealed` (in force at the start, gone by the end) is
0 in all five windows, so the mechanism is a *substitution* detector and we have no question that
would warn us: a repeal question would fail silently.

Reranked with Episode 5's own reader (`nomic-embed-text` + `ms-marco-MiniLM-L-6-v2`), same depth,
same reader throughout; cells are candidates/rank:

| id | no restrict | `began` (today) | `boundary` |
|---|---|---|---|
| G-067 | 9,537 / - | 360 / - | 42 / 25 |
| G-068 | 11,492 / 24 | 109 / 5 | 13 / **1** |
| G-069 | 4,454 / - | 381 / 31 | 20 / **4** |
| G-070 | 11,492 / 5 | 4,434 / 3 | 517 / **4** |
| G-071 | 10,771 / 49 | 250 / 45 | 27 / 15 |
| **Hit@4** | **0/5** | **1/5** | **3/5** |

Worth stating against the hierarchical-retrieval regression (-0.033 cov-nDCG@10) in
`section_field_routing.md`: this is a restriction, which is the shape that measures badly. The
defence is not that restriction is safe — it is that the restriction is a **definition** whose
recall was *verified* (5/5 addresses present) rather than assumed, and that the open ranking
remains available as a fallback when the predicate returns an empty set.

Script: `scratchpad/simpler.py`, `scratchpad/wc_rerank.py`.

## Endpoint resolution: already correct, and partly done for us

- `GitHistory.at(when)` is "the last revision whose committer time is at or before `when`", which
  is the point-in-time convention legal publishers use (*as at end of* a date). Resolving a
  window's start to the day's *first* position would silently pull same-day changes in.
- Release-tag windows are **pre-resolved in the golden set** (`from_ref: "v0.1.0-pre.27"` carries
  `from: "2026-09-14T00:11:49Z"`), so the tag -> commit -> nearest-first-parent-ancestor hazard does
  not bite here. It also means the golden set performs a resolution step a production retriever
  would have to do itself, and **we have never measured that step**.

## Reader ablation: G-071's problem is the reader, not the restriction

The two earlier passes used different readers, so they were not a controlled comparison. This one
holds the boundary restrict fixed and varies only the cross-encoder.

| id | candidates | no reader | reader |
|---|---:|---:|---:|
| G-067 | 42 | 24 | 25 |
| G-068 | 13 | **1** | **1** |
| G-069 | 20 | 5 | **4** |
| G-070 | 517 | 19 | **4** |
| G-071 | 27 | **6** | 15 |
| **Hit@4** | | **1/5** | **3/5** |

**The cross-encoder demotes G-071 from 6 to 15** while earning G-070 (19 -> 4) and G-069 (5 -> 4).
So the reader is not uniformly good on this stratum, and G-071 is a reader failure inside a
27-candidate set whose recall is verified — which makes it a question for a stronger reader
(Qwen3-Reranker-8B on the 5090 tier), not for another restriction. G-067 is 24/25 either way: a
genuine ranking loss in a 42-candidate set with no mechanism in hand.

Script: `scratchpad/reader_ablation.py`.

### Honest forecast for the eight

| banked on measurement | +2 | G-069, G-070 (both at rank 4 — one-rank margins, not robust) |
| plausible, untested | +3 | G-058 (escalation), G-055 (sibling expansion on the declared edge), G-071 (stronger reader) |
| no honest path | 3 | G-067 (ranking loss, 42 candidates), G-051, G-074 |

**18/24 proven, ~21/24 plausible, 24/24 not honestly reachable.**
