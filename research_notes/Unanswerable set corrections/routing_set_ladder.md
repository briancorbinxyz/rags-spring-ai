# The routing set, run through the whole ladder

Run 27 Oct 2026, laptop tier (`nomic-embed-text:v1.5` + `ms-marco-MiniLM-L-6-v2`), so the series
has the same per-episode arc for the 15 new routing questions that it has for the main set.
`groundless@4` is reported throughout.

| step | config | Hit@4 | groundless@4 | confidently wrong |
|---|---|---|---|---|
| Episode 3's shape: naive chunks, dense only | `A` | 1/15 | 14/15 | 3 |
| Episode 3's shape: naive chunks, BM25 only | `B` | 1/15 | 14/15 | 4 |
| element units, dense only | `C` | 4/15 | 11/15 | 5 |
| element units + BM25 | `S1` | 3/15 | 12/15 | 5 |
| + identifier lookup | `S2` | 3/15 | 12/15 | 5 |
| + lifecycle filter | `S3` | 3/15 | 12/15 | **10** |
| + dense, RRF k=60 | `S4` | 4/15 | 11/15 | **0** |
| + cross-encoder rerank | `S5` | 5/15 | 10/15 | 3 |
| **Episode 5's published line** | `E5-2` | **6/15** | 9/15 | 3 |
| Episode 6's surviving arms | `G9` | 5/15 | 10/15 | 3 |
| + the `#context` arm | `GA` | 5/15 | 10/15 | 2 |

Episode 3's naive shape answers **1 of 15**. The series' best answers **6 of 15**. So these
questions are hard in a way the existing sets are not, and the whole stack still misses nine of
them — which is the headroom the main set no longer has.

The ladder also reproduces, on unseen questions, the one regression already isolated on this set:
`E5-2` at 6/15 against `G9` at 5/15, caused by `admit_history` firing on the word "before".

## Superseded by the 5090, 28 Oct — both conclusions above are wrong

The same ladder ran at the main tier on 28 Oct (`66eaeff`, Qwen3-Embedding-8B with
Qwen3-Reranker-8B). Keep this file as the laptop floor, but **read the two paragraphs above as
laptop artefacts**, because neither survives:

| step | config | laptop Hit@4 | **5090 Hit@4** | 5090 groundless@4 | confident |
|---|---|---|---|---|---|
| naive chunks, dense only | `A` | 1/15 | 8/15 | 7/15 | 4 |
| naive chunks, BM25 only | `B` | 1/15 | 1/15 | 14/15 | 4 |
| element units, dense only | `C` | 4/15 | 11/15 | 4/15 | 1 |
| element units + BM25 | `S1` | 3/15 | 3/15 | 12/15 | 5 |
| + identifier lookup | `S2` | 3/15 | 3/15 | 12/15 | 5 |
| + lifecycle filter | `S3` | 3/15 | 3/15 | 12/15 | **10** |
| + dense, RRF k=60 | `S4` | 4/15 | 6/15 | 9/15 | 1 |
| + cross-encoder rerank | `S5` | 5/15 | **14/15** | 1/15 | **0** |
| Episode 5's published line | `E5-2` | 6/15 | **14/15** | 1/15 | 0 |
| Episode 6's surviving arms | `G9` | 5/15 | **14/15** | 1/15 | 0 |
| + the `#context` arm | `GA` | 5/15 | **14/15** | 1/15 | 0 |

**The headroom was the laptop's, not the questions'.** `GA` misses one question, `R-004`.

**The `E5-2`/`GA` inversion does not reproduce.** Both sit at 14/15 and `GA`'s MRR is the higher of
the two. So `admit_history` firing on "before" costs a question at the laptop tier and costs nothing
at this one — which makes it a tier artefact, not a mechanism defect, and it should not be written
up as one.

**Nearly all of the lift is the reranker.** `S4` → `S5` gains eight questions — R-005, R-006,
R-007, R-008, R-011, R-013, R-014, R-015 — and loses none. Dense alone on element units already
finds 11, so this set is hard for *retrieval* and easy for a reader of the right size. The usable
finding is about where generalisation lives: a 22M-parameter cross-encoder answers 5 of these and an
8B one answers 14, on a set no part of the stack was built against.

**The confidence column flips meaning too.** At the 5090, `S3` is groundless on 12 and confident on
10 of them, and from `S5` down the single groundless answer is not confident. So *on unfamiliar
questions, confident wrongness comes back* is true of the BM25 line and of the laptop, and **false
of the main line**.

**R-004 did not move on any rung**, including `A`, `B` and `C` — outside 50 everywhere. It never
names npm, publishing or a registry in those words ("before any of it was on a public registry …
talk them through"), and its top ten are other specs' `#questions` and `#scenarios`. That is a
recall miss, not a ranking one. Left unrun as an epilogue candidate, per the rule against adding an
arm after seeing the set.

## A caveat on the confidence column

"Confidently wrong" counts groundless answers scoring above the **median grounded** one. When
only three answers are grounded the median rests on a single question, so the count is unstable at
the bottom of the ladder. The 10 at `S3` should not be read as a finding about the lifecycle
filter; `S4`'s 0 and the main set's larger samples are the trustworthy end of this column.

The metric is sound where there are enough correct answers to form a median. That is a real limit
and it is another reason the question sets need to be bigger.
