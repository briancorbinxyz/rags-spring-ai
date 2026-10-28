# The intent model as an admitter, not a chooser

Measured 27 Oct 2026, laptop tier (`nomic-embed-text:v1.5` + `ms-marco-MiniLM-L-6-v2`,
`qwen3:8b` Q4_K_M as the intent model, temperature 0, seed 42, thinking disabled, Ollama
structured output over an enum of the declared section kinds). Scripts: `scratchpad/intent_recall.py`,
`dilution.py`; configs `GI`, `GJ` in `run.py`; arm `Pipeline.intent_ranking`.

The reframing that prompted this: **the model need not decide anything, only ensure the right
anchors are in the top X**, working with the mechanisms already there. Every previous attempt at
the section seam asked a *chooser* for one kind, and all of them land near chance — RRF over 31
kinds puts the answer about 9th, a reader 6th at 92 s a question, the `WHY` list is 21% precise,
and each kind's own score margin picks right 2 times in 18 against a 4% floor.

## 1. Kind recall — the model does its job

18 of the 90 questions have a named section among their expected passages. Recall of the true kind
in the named set, with the set's size as the price:

| arm | kind recall | mean kinds named | named nothing |
|---|---:|---:|---:|
| `WHY` regex, today | **4/18** | 0.2 | 71/90 |
| a repaired list of the same shape | **7/18** | 0.7 | 41/90 |
| all 25 kinds (the no-model control) | 18/18 | 25.0 | 0/90 |
| **`qwen3:8b`** | **11/18** | 5.2 | 9/90 |

Nearly triple today's recall. The three-way comparison earns its keep: the honest gain is **11
against 7**, not 11 against 4 — a good deal of the defect was list quality, not lexical matching.
Cost **2,743 ms a question**, well above the 0.5–1.4 s the cost research estimated for this tier.

Five of the seven misses want `#intent` or `#context`, which the model confuses with `#decisions`
and `#assumptions` — adjacent concepts, and a systematic blind spot rather than noise. G-058 is
among the misses: it named `approach, assumptions, decisions, intent`, and wanted `risks`.

## 2. End-to-end — and it gains nothing

`GI` is `GA` plus the arm, two per named kind, round-robin so no score is compared across two
collections with different statistics.

| config | Hit@4 | all@4 | MRR | verdict |
|---|---|---|---|---|
| `GA` | 35/47 | 33 | 0.689 | the line |
| `GI` — two of each named kind | 35/47 | 33 | 0.690 | **ratchet-neutral, gains nothing** |
| `GJ` — four of each named kind | 35/47 | **32** | 0.690 | **fails the ratchet** |

`GI` is live, not inert: four questions move (G-002 9→7, G-016 10→9, G-015 and G-077 reorder).
None crosses the depth-4 line. `GJ` deepens admission to the depth the measurement itself uses and
**loses an all-evidence question** — more admission is worse.

## 3. Why — admission was never the bottleneck

Three ranks per question: where the answer sits in its own kind, where it sits in the round-robin
the arm hands to the fusion, and where it ends up.

| id | kind | kind pop | in its kind | in the arm | final |
|---|---|---:|---:|---:|---:|
| G-006 | `#intent` | 156 | **1** | — | 1 |
| G-009 | `#run` | 98 | **1** | 5 | **7** |
| G-018 | `#intent` | 156 | **1** | — | 1 |
| G-031 | `#requirements` | 121 | **1** | 5 | 4 |
| G-032 | `#requirements` | 121 | **1** | 3 | 1 |
| G-036 | `#dependencies` | 96 | **1** | **2** | **9** |
| G-077 | `#context` | 130 | **1** | 5 | 1 |
| G-078 | `#context` | 130 | 9 | — | 3 |
| G-079 | `#context` | 130 | **1** | 3 | 1 |
| G-080 | `#context` | 130 | 4 | — | **21** |
| G-081 | `#context` | 130 | **1** | — | 3 |

**Nine of eleven answers are rank 1 inside their own section kind.** G-036 is admitted *second* of
everything the arm offers and finishes **9th**. G-009 is admitted 5th and finishes 7th. The
candidate is in the pool, near the top of the pool, and the pipeline puts it out of reach.

So the conclusion is not that the model failed. **The model did exactly what it was asked and the
fusion threw the result away** — the sixth instance of the episode's own thread, and the first one
quantified at candidate level rather than from aggregates. `GJ` confirms it from the other side:
adding candidates costs an all-evidence question.

## What is left, and why I did not do it

What would preserve a confident arm's judgement is pinning, and pinning has failed the ratchet
twice — `scope_pin` lost the 5090 main set, `reader_pin` was confounded and then inert. The narrow
untried version is to pin an admitted candidate **only when its own kind ranks it first**, taking
the confidence from the collection's own statistics rather than from a model. On this evidence that
would fire on 9 of 11 and would pick up G-036 and G-009.

It is also a conjunction of conditions chosen while looking at eleven questions, which is a lookup
table with a mechanism's manners. The evaluation research is explicit that high precision on the
eval set is the *suspicious* outcome. So it is named here and left unrun, and it is the first thing
a larger question set would let us test honestly.

## Reproducing

```
python intent_recall.py qwen3:8b              # writes intent_named.json
run.py --intent intent_named.json --only GA,GI,GJ
```

`intent_admit` defaults to 0, so every configuration measured before this existed is untouched; the
named sets are cached, so a run is deterministic and a model that is down costs only the arm.

---

# The pin, spiked

The rule as set: **pin an admitted candidate when its own section kind ranks it inside depth 4.**
That is better than the "rank 1" version drafted above, because it reuses the depth the measurement
already justifies instead of inventing a new one — and it reaches G-080, which sits 4th in
`#context` and which a rank-1 rule would never have touched. Configs `GK` (pre-registered),
`GL`/`GM` (exploratory, for the curve).

| config | Hit@4 | all@4 | MRR |
|---|---|---|---|
| `GA` | 35/47 | 33 | 0.689 |
| `GK` — pinned when its kind ranks it in the first four | **18/47** | 15 | 0.348 |
| `GL` — ...the first one | **18/47** | 15 | 0.386 |
| `GM` — ...the first two | **18/47** | 15 | 0.363 |

**Pinning one candidate is as destructive as pinning four**, so the quantity is not the mechanism.
Per question: it **broke 18, fixed 1, kept 17**.

And the one it fixed is the one the dilution table predicted:

```
G-036:  9 -> 2      the question whose answer was rank 1 of 96 in #dependencies
G-009:  7 -> 5      improved, still a miss
G-080: 21 -> 16     improved, still a miss
```

while the 18 it broke were mostly sitting at rank 1:

```
G-001: 1 -> 27   G-006: 1 -> 29   G-004: 1 -> 23   G-019: 1 -> 23
G-010: 1 -> 23   G-020: 1 -> 22   G-042: 2 -> 22   G-041: 1 -> 20
```

## Why, precisely

A section kind always ranks one of its members first. It does that whether or not the question
wants that kind at all — the ranking is *within* the collection and knows nothing about whether the
collection was the right one. So the confidence is real and it is **confidence about the kind, not
about the question**, and pinning converts the model's 5.2-kinds-per-question imprecision directly
into the top four. The model named something on 85 of 90 questions, so almost every question got a
pin, including every question that was already answered.

Fusion is what made the imprecision affordable. Pinning removes exactly that.

This is the **third** pin to fail the ratchet here, after `scope_pin` (lost the 5090 main set by
pinning 15 to 20) and `reader_pin` (confounded, then inert). The pattern is now worth stating as a
result rather than rediscovering: on this corpus a confident sub-collection's judgement survives
only as a *vote*. Promoted to a verdict it loses, every time, and the stronger the sub-collection's
own confidence the more damage it does.

# What was fixed

`DOC_KINDS` resolved a question's document kind to a constant filename. Two of its rows had never
once been right, and one of them named `plan.html`, a page that stopped existing on 30 July 2026
when 321 files were renamed in a single commit.

A kind now resolves to **the first name it has gone by that the collection actually holds**, which
needs no rename detector — git's is a similarity heuristic, and 42 of the 178 renames it finds here
depend on where the threshold is set, whereas the slice is a fact. Proven:

| | flag off | flag on |
|---|---|---|
| current corpus, "design" / "plan" | `design.html` | `design.html` |
| slice at 1 July 2026, "design" / "plan" | `design.html` (absent — the arm goes dark) | **`plan.html`** |

G-072 expects `specs/004-npm-publish-workflow/plan.html#D-005`, so the kind now follows the rename
across the cutoff.

**It changes no score, and that was the expectation**: G-072 is already answered at rank 1 through
the identifier lookup, which never reads a filename. Ratchet-neutral on both sets — slice 8/9 with
what-changed told 3/5 under `TD` and `TS` alike, main set 35/47 / all@4 33 / MRR 0.689 under `GA`
and `GS` alike. And because merging the two rows changed unflagged behaviour, that was proven
differentially rather than assumed: `GA` before the merge against `GA` after it, **0 of 53
questions changed**, with G-007 (10th) and G-020 (1st) identical.

What it buys is not a number. It removes a case where the scope arm asked for a document that did
not exist and returned nothing silently, which is the one thing no measurement here could see.
