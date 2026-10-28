# The 5090's two owed runs: the routing set and the `moved` window rule

Qwen3-Embedding-8B with Qwen3-Reranker-8B, 28 Oct, spectastic `c64d41c` (722 commits, reachability
23/23). Every number is a floor: no blind-audit round has seen the routing set or a time candidate.

## 1 · The routing set, the whole ladder, once (`5090-routing.*`)

Run once, as written. No arm was added after looking.

| key | configuration | Hit@4 | MRR@50 | groundless@4 | of those, confident |
|---|---|---|---|---|---|
| A | naive 800-token chunks, dense only | 8/15 | 0.439 | 7/15 | 4 |
| B | naive 800-token chunks, BM25 only | 1/15 | 0.088 | 14/15 | 4 |
| C | element units, dense only | 11/15 | 0.486 | 4/15 | 1 |
| S1 | element units + BM25 | 3/15 | 0.091 | 12/15 | 5 |
| S2 | + identifier lookup | 3/15 | 0.091 | 12/15 | 5 |
| S3 | + lifecycle filter | 3/15 | 0.102 | 12/15 | 10 |
| S4 | + dense, RRF k=60 | 6/15 | 0.411 | 9/15 | 1 |
| S5 | + cross-encoder rerank top 50 | **14/15** | 0.822 | 1/15 | 0 |
| E5-2 | + anchored typed expansion | **14/15** | 0.822 | 1/15 | 0 |
| G9 | Episode 6's surviving arms | **14/15** | 0.822 | 1/15 | 0 |
| GA | G9 + `#context` for why-questions | **14/15** | 0.833 | 1/15 | 0 |

Against the laptop's floors (1/15 at Episode 3's shape, `E5-2` 6/15, `GA` 5/15):

- **The `E5-2`/`GA` inversion does not reproduce.** Both are 14/15 here and `GA`'s MRR is higher.
  The `admit_history` firing on "before" that costs the laptop a question costs nothing at this
  tier.
- **Nearly all of the lift is the reranker.** S4 → S5 gains eight questions (R-005, R-006, R-007,
  R-008, R-011, R-013, R-014, R-015) and loses none. The routing set is hard for retrieval, not for
  a reader of the right size: dense alone on element units already finds 11.
- **Confidence comes back where the reranker is absent.** S3 is groundless on 12 and confident on
  10 of them; from S5 down, the one groundless answer is not confident. So at this tier the
  Refusal Correction's "on unfamiliar questions, confident wrongness comes back" is true of the
  BM25 line and of the laptop, and **not** of the 5090's main line.

**Did not move: R-004** (`intent-noword`), outside 50 on every rung including `A`, `B` and `C`. The
question never names npm, publishing or a registry by those words ("before any of it was on a
public registry … talk them through"), and the top ten are other specs' `#questions` and
`#scenarios`. A recall miss, not a ranking one. Left unrun as an epilogue candidate, per the rule.

## 2 · The slice under `moved` (`5090-slice-moved.*`)

**A correction to the handover's command.** `--window-rule` is read only with `--changed-only`, so
the command as written ran the unrestricted slice and `moved` never applied. It was re-run with
`--changed-only --window-rule moved`. The `began` baseline is `5090-slice-changed-arms`, which used
`--changed-only`.

| key | `began` answered | `moved` answered | `began` located · told | `moved` located · told |
|---|---|---|---|---|
| T5 | 7/9 | 7/9 | 1 · 1 | 1 · 1 |
| TD | 8/9 | 8/9 | 2 · 2 | 2 · 2 |
| TF | 8/9 | 8/9 | 1 · 1 | 1 · 1 |
| TK | 8/9 | 8/9 | 1 · 1 | 1 · 1 |

**Ratchet: pass.** Every cutoff row is identical under both rules, so no stratum loses anything.
`moved` is the better predicate and can ship, but on this tier it **locates nothing new**: the
laptop's 3 of 5 does not reproduce. It moves ranks, every one of them up:

| question | `began` (TF) | `moved` (TF) | why it is still out, if it is |
|---|---|---|---|
| G-067 | 20 | 14 | The change proposal's own copy outranks the requirement (see the ceiling file). |
| G-068 | 3 | **1** | |
| G-069 | miss | 10 | Names no spec, so the scope arms never fire; under `moved` it reaches the candidates, at 10. |
| G-070 | miss | miss | Still outside 50 under every config. |
| G-071 | 22 | 5 | One place outside the cut. `TD` has it at **4** (located), as before. |

The cutoff rows, G-073 and G-074 included, are unchanged from the ceiling file.

## 3 · The one number (`5090-combined-105.*`)

`combine.py --at-episode-end`, sources in precedence order: `5090-slice-moved:TK` →
`time-e6:GA` → `sections:GA` → `heldout-e6:GA` → `routing:GA`.

| questions | answerable | Hit@4 | all@4 | MRR@50 | groundless@4 | confidently wrong |
|---|---|---|---|---|---|---|
| **105 of 105** | 98 | **83/98** (84%) | 75 | 0.762 | 22/105 | 2 |

Against the figure to beat (69/83, groundless@4 21/90, 90 of 105): the 15 routing questions add 14
answered and one groundless. The time set reads 15/23 (G-075 is the abstention row and counts as
unanswerable); `moved` located nothing new, so it contributes no change there. Both confidently-wrong rows
are in the held-out set, which is scored once and stays untouched.
