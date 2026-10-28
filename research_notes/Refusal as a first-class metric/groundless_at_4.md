# Refusal, measured — retroactively, from Episode 3 onward

Added 27 Oct 2026. `ratchet/score.py:groundless()`, reported by `run.py` beside `Hit@4` on every
run from now on, and recomputed from runs already stored so the series can say what it was doing
all along.

## The deficit

`Hit@4` is reported over the **answerable** questions. The main set holds 53 questions of which 47
are answerable and **six exist solely to test refusal** — "Is there a JetBrains plugin?", "Can it
export to .docx?". Those six are outside the denominator and were scored `answered=None`, so the
headline number could not express them even in principle. `47/47` is `47` of `53`.

Nothing in the pipeline declines. On the six, it serves four passages every time: G-050 returns a
triage card at top-1 **+1.213**, G-046 another at **+0.977** — both above the score of answers it
gets *right*.

## The counter

**`groundless@4`** — questions whose top four holds no expected passage.

For an answerable question that is a miss. For an unanswerable one it is *always* true, because
there is nothing it could correctly serve: the only way out is to decline. **No threshold appears
in the definition**, which is what lets it be computed backwards over stored runs.

Beside it, the number that decides whether declining is buildable at all: how many groundless
answers score **above the median grounded one**. Near zero means the score separates and a refusal
line exists. Otherwise the system cannot be made to decline by thresholding what it already
computes.

It is derived from the scorer's own `first_hit`, never re-derived from `top10` — doing the latter
disagreed with the published `47/47` by two, because the scorer knows about multi-part units and a
naive set intersection does not. **No published number moves.**

## What the series was actually doing

| run | grounded | groundless@4 | missed | unanswerable | **confident** |
|---|---|---|---|---|---|
| **Episode 3's naive attempt** (`EP3`, 5090) | 10/47 | **37/47** | 31 | 6 | **24 of 37** |
| Episode 5 line (`E5-2`, 5090, Qwen3 pair) | 46/53 | 7/53 | 1 | 6 | **0 of 7** |
| **Episode 6 line (`GA`) — the 47/47** | **47/53** | **6/53** | 0 | 6 | **0 of 6** |
| Episode 6 line, laptop (`GA`) | 35/53 | 18/53 | 12 | 6 | 0 of 18 |
| element units + BM25 only (`S1`, laptop) | 30/53 | 23/53 | 17 | 6 | **4 of 23** |
| the new routing set (`E5-2`, laptop) | 6/15 | 9/15 | 9 | 0 | **4 of 9** |

Three things fall out of that table that no previous number said.

**Episode 3's naive attempt was not merely bad at finding — it was confidently wrong.** 37 of 47
groundless, and **two thirds of those scored above the median of the answers it got right**. The
published story was about retrieval quality; the refusal dimension makes it much worse, and it is
the honest baseline the series should have been measuring against from the start.

**The series fixed calibration, not just recall.** Groundless answers fall 37/47 -> 6/53 and
confidently-groundless 24 -> **0**. Every remaining groundless answer on the 47/47 run is one of
the six that cannot be answered, and **not one of them outscores the median correct answer** — so a
refusal line exists on this corpus and the system is one threshold away from being able to decline.
That is a stronger claim than 47/47 and it was there in the stored data all along.

**The cross-encoder is what makes it separable.** BM25 alone leaves 4 of 23 groundless answers
scoring above the median correct one; adding the reader takes that to 0. The reranker's value is
not only better ranking, it is *knowing when it has nothing*.

**And it does not hold on unseen questions.** On the 15 routing questions, 4 of 9 groundless
answers outscore the median correct one, and under pinning 9 of 13 do. Confident wrongness returns
the moment the corpus region is unfamiliar — so the clean separation above is a property of the
questions the system has been developed against, not of the system.

## What this changes about the target

A 100% target is not the problem; `Hit@4` being the thing at 100% is. For a corpus where accuracy
matters the target is

> **answered-correctly + correctly-refused = 100%, confidently-wrong = 0**

and the series has been reporting only the first term. The six refusal questions make the second
measurable as soon as a declining mechanism exists; `groundless@4`'s confident count makes the
third measurable today, with no new mechanism and no threshold.

## Not done, deliberately

No refusal **mechanism** has been added. A line at zero on the cross-encoder logit is principled —
"the reader says its best passage is not relevant" — but measured, it keeps 32/35 correct answers
on the laptop main set while admitting only 4 of 12 misses, and on the routing set it keeps 1 of 6.
The two sets disagree about where the line belongs, and that disagreement is itself the finding:
**confidence is not calibrated across corpus regions**, so a single threshold fitted on either set
would be fitting. The mechanism should wait for a question set that has been through a blind audit.

## Reproducing

```
run.py ...                    # every run now prints groundless@4 beside Hit@4
scratchpad/groundless.py manifest.json   # recomputes it over any stored runs
```
