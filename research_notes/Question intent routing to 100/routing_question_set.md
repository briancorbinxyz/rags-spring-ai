# The routing question set: 15 questions, frozen before measurement

`src/main/resources/golden/questions-routing.jsonl`, written 27 Oct 2026 against the pinned
spectastic commit `c64d41c`. **Every question carries `status: "draft"`** — no blind-audit round
has looked at a single candidate passage, so every number this set ever produces is a floor in the
same sense the held-out numbers are.

It is a **separate file**. Golden set v2 stays frozen at 53 questions.

## What it is for

One rule — `WHY = (r"\bwhy\b", r"what problem", r"what led", r"what prompted")` — decides whether
the archive's `#context` sections are searched. Measured over the existing 90 questions it scores
**21% precision at 80% recall**. The two strata here attack its two failure modes separately, which
the existing sets cannot distinguish:

| stratum | n | the rule's behaviour | what the question wants |
|---|---|---|---|
| `intent-noword` | 8 | **fires on none of them** | the spec's `#context` |
| `triage-why` | 7 | **fires on all of them** | a `<spec-triage>` card, *not* `#context` |

So `intent-noword` measures recall — an intent question a reader would plausibly write that
contains none of the four patterns. And `triage-why` measures precision — a question that asks
*why* and whose answer is a root cause in a triage log, where admitting `#context` is actively the
wrong move. Together they are the first questions in this project that can tell the two apart.

`triage-why` also fills a real hole: `triage-log.html` has 156 cards, each with a declared
`<spec-triage>` shape carrying a headline of the form *"Debugging X; root cause was Y; fixed by Z"*
plus `Expected` / `Actual` / `Diagnosis`. The existing `DOC_KINDS` row for it scores **0%
precision and 0% recall**. Nothing in the three existing sets asks a why-question of a triage card.

## Properties, recorded before any retriever saw it

| | |
|---|---|
| names a spec the literal way | **1/15** |
| names a spec only via a declared alias | **0/15** |
| names an element id | **0/15** |
| trips a `HISTORY_MARKER` while wanting current text | 2/15 |
| expected anchors | `#context` x8, `T-001` x7 |
| mean question length | 154 chars |

**Fourteen of fifteen offer no structural handle whatsoever.** That is deliberate and it is the
hard case: no spec number, no element id, no document name, so `scope_rrf`, `doc_scope` and the
identifier lookup are all silent and the section seam is the only mechanism that can act. The
existing main set has five such questions (G-077..G-081); this triples that.

The two that trip a history marker do so on the word "before" while wanting current text, which
makes them incidental history-decoys. That was not designed, and it is worth keeping: "before X
existed" is the natural way to ask what X was for.

## How the gold was chosen, and the ambiguity I had to avoid

Passages first, questions second — each question is written *from* a passage found by walking the
archive's structure, never from what a retriever returned. Two groups were rejected for ambiguous
gold rather than question quality:

- the `#context` sections of specs **006-014** all say some version of *"today's verb is a markdown
  procedure and does not reach Tier 2 MCP, Tier 3 VS Code or Tier 6 web editor"*, so a question on
  any one of them is answered by eight others;
- the `#context` sections of specs **112-120** are one guardrail programme with the same problem.

R-008 initially used `022-explore#context` and was replaced when the check caught that an existing
set already uses that address. Every expected address is verified present in the corpus and unused
by the other three sets.

For `triage-why` the gold is determinate because the card states its own root cause. Where the
card explicitly exonerates the spec — R-009's *"Spec FR-002 is correct as written; the leak is
purely in the description string"*, R-014's FR-006 — the spec address is deliberately **not**
expected evidence, and the note on each question says so.

## Pre-registered predictions

Written before the set was run, so that a result confirming them means something:

1. **`intent-noword` will score badly on the current line.** `WHY` fires on none of them, so
   `context_scope` never fires, and 8/8 have no structural handle. Prediction: most of the eight
   miss at depth 4.
2. **`triage-why` will be worse, not merely as bad.** The rule fires on all seven and admits
   `#context` — a collection that cannot contain the answer. Prediction: the admitted arm actively
   displaces, so these score below `intent-noword`.
3. **The intent admitter should help the first stratum and not the second.** It names a mean of 5.2
   kinds and reached 11/18 kind-recall on the existing sets; naming `#context` is the easy case.
   For `triage-why` it must name a `T-NNN` card id, which is **not a section kind at all** and is
   outside the vocabulary the admitter is given — so prediction: no help, by construction.
4. **Pinning will fail here too**, for the reason it failed before: a kind's confidence is about
   the kind, not the question.

If 3 holds, the honest reading is that the admitter fixes a recall hole in a hand-written rule and
nothing more — which is a real but small result, and the set is what makes it sayable.

---

## Result: the set does not work, and the fault is mine

Scored once, as pre-registered. Two of the four predictions held; the two about the retriever were
wrong, and wrong for a reason that invalidates most of the set.

| config | Hit@4 | all@4 | MRR |
|---|---|---|---|
| `S1` — element units + BM25, nothing else | **12/15** | 12 | 0.773 |
| `E5-2` — the published line | **15/15** | 15 | 0.911 |
| `GA` | **15/15** | 15 | 0.911 |
| `GI` — + the intent admitter | 15/15 | 15 | 0.911 |
| `GK` — + pinning | **3/15** | 3 | 0.169 |

**Prediction 3 held exactly.** The admitter's kind-recall is **4/8** on `intent-noword` against the
`WHY` list's 0/8, and **0/7** on `triage-why` — zero by construction, because a `T-NNN` card id is
not a section kind and is not in the vocabulary the admitter is given.

**Prediction 4 held.** Pinning takes 12/15 to 3/15.

**Predictions 1 and 2 were wrong**, and not because the retriever is better than I thought. Per
question:

```
            bm25  full  admit   pin
R-001          1     1      1    22      R-009 .. R-015, every triage-why:
R-002         19     3      3     2        bm25 rank 1, all seven
R-003          2     1      1    21
R-004          -     3      3    27
R-005         26     1      1    27
R-006          1     1      1     1
R-007          1     1      1    17
R-008          1     1      1     2
```

**Plain BM25 answers 12 of the 15 at rank 1 or 2, and all seven `triage-why` questions at rank 1.**
The routing rule never gets a chance to matter on them. Only R-002, R-004 and R-005 carry signal —
three questions, which by the sign test is worth p=0.25 and is no result at all.

### Why: I wrote the questions from the passages

Each question was authored by reading its answer passage, which leaks the passage's rare words into
the question. Measured as the share of a question's content words occurring in its expected
passage:

| stratum | n | mean overlap |
|---|---|---|
| `stale` | 7 | 65% |
| `single-fact` | 10 | 55% |
| **this set** | 15 | **53%** |
| `relation` | 7 | 43% |
| **`intent` — the stratum this set extends** | 5 | **32%** |
| `identifier` | 8 | 21% |
| `paraphrase` | 10 | 20% |

53% is not an outlier against the whole audited set (mean 39%), but against the 32% of the stratum
it was written to extend it is 21 points more generous — and for the triage half it is worse than
the average suggests, because describing a bug tends to use the bug's own distinctive nouns.
R-010 names `@spectastic/render` and the `latest` tag, and the card is the only place those
co-occur, so BM25 cannot miss it.

### What a question that tests the rule looks like

The `paraphrase` stratum sits at 20% overlap and is the discipline this set needed: describe the
symptom in a reader's own words and make the retriever bridge the gap. For `triage-why` that means
asking about a bug **without** naming the package, the file, the flag or the identifier — "a
release reported success but installing the tool still gave an old build" rather than naming
`@spectastic/render`.

That is harder to write and it is the only version worth having. The 12 questions that BM25 answers
at rank 1 should be rewritten to that standard or deleted; the three that carry signal (R-002,
R-004, R-005) are worth keeping as they are.

**Nothing here should be cited as a measurement of the routing rule.** What the set did establish
is the admitter's `0/7` on triage cards, which is structural and does not depend on question
difficulty.

---

# Rewrite, and the predictions for it

All 15 rewritten 27 Oct 2026 as a spectastic.io visitor would ask them, with the passages closed.
Measured before scoring: **mean overlap 20%, max 33%** (was 53% / 86%), against `paraphrase` 20%,
`intent` 32%, audited mean 39%. **0 of 15 name a spec or an element id.**

Predictions, written before the run:

1. **BM25 alone collapses.** It took 12/15 on the first draft. Prediction: **6/15 or fewer.**
2. **`intent-noword` misses several on the published line.** `WHY` fires on none of the eight, so
   `context_scope` never fires, and none of the eight offers a structural handle.
3. **`triage-why` scores below `intent-noword`.** `WHY` fires on all seven and admits `#context`,
   a collection that cannot hold the answer, so the arm actively displaces.
4. **The admitter helps the first stratum only.** It must name a section kind; a `T-NNN` card id is
   not one, so `triage-why` is 0 by construction however the question is worded.
5. **Pinning fails**, as it has three times.
6. **R-001 is the question I expect to have got wrong** — 30% overlap and three clauses. If it is
   BM25 rank 1 I will cut it rather than patch it.

## Result of the rewrite: the set works, and it caught a regression

| config | Hit@4 | all@4 | MRR |
|---|---|---|---|
| `S1` element units + BM25 | **3/15** | 3 | 0.091 |
| `S2` + identifier lookup | 3/15 | 3 | 0.091 |
| `S3` + lifecycle filter | 3/15 | 3 | 0.102 |
| `S4` + dense, RRF | 4/15 | 4 | 0.223 |
| `S5` + cross-encoder | 5/15 | 5 | 0.305 |
| **`E5-2` the published Episode 5 line** | **6/15** | **6** | **0.311** |
| `G9` Episode 6's surviving arms | 5/15 | 5 | 0.306 |
| `GA` + the `#context` arm | 5/15 | 5 | 0.301 |
| `GI` + the intent admitter | 5/15 | 5 | 0.301 |
| `GK` + pinning | 2/15 | 2 | 0.059 |

**Prediction 1 holds decisively: BM25 alone falls from 12/15 on the first draft to 3/15.** The
rewrite is a question set rather than a vocabulary-matching exercise, and the ladder now climbs the
way the main set's does — 3, 3, 3, 4, 5, 6 — so each step is doing visible work.

Predictions 2, 3 and 5 hold. By stratum on `E5-2`: `intent-noword` **4/8**, `triage-why` **2/7**,
so the half the `WHY` rule fires on scores worse than the half it ignores, as predicted. Pinning
takes 5/15 to 2/15. Prediction 4 holds again: the admitter's kind-recall is **5/8** and **0/7**, and
end-to-end it is inert (R-008 moves 28 to 26 and nothing crosses depth 4).

Prediction 6 was half right. R-001 is BM25 rank **3**, not 1 — the weakest of the eight but not a
dud, so it stays.

### The regression, isolated

**On this set the published Episode 5 line beats the Episode 6 line, 6/15 against 5/15.** One
question accounts for it, and an ablation names the arm:

| config | Hit@4 | |
|---|---|---|
| `E5-2` | 6/15 | |
| `G9` | 5/15 | |
| **`G9h` — `G9` without `admit_history`** | **6/15** | recovers it exactly |
| `G9s` — `G9` without the named-scope arms | 5/15 | not the cause |

R-010 goes **4 -> 6** at `G9` and 6 -> 7 at `GA`. It names no spec, so `scope_rrf` and `doc_scope`
cannot fire; what fires is `admit_history`, because the question contains the word **"before"**:

> *"Everything in a publish ran green, and one of the pieces I pulled down afterwards was still the
> build from **before**. Why would just one of them be left stuck?"*

`HISTORY_MARKERS` holds 20 words including `before`, `earlier`, `originally`, `previously`, `first`.
Four of these fifteen questions trip it — R-002 and R-004 and R-010 on "before", R-012 on "earlier"
— and **none of the four wants retired text.** Measured over the original 90 questions that list
runs at 25% precision; here it costs a hit outright.

An honest note on my own first reading: I attributed the loss to `context_scope` and the `WHY` list
before running the ablation, and that was wrong — `G9` is already at 5/15 before `context_scope` is
added. `context_scope` does damage R-014 separately, pushing it from 21st to outside the top 50,
but R-014 was never a hit so Hit@4 does not see it.

### Why this matters more than the score

`admit_history` **passed its own ratchet** on the main, time and held-out sets. It is in the
Episode 6 line because it earned its place there. A set of fifteen questions written for a
different purpose, by someone who had never run them, found that it costs a question the moment a
reader uses the word "before" to mean "the previous build" rather than "the superseded record".

That is the argument for more questions, made concretely rather than from a power calculation: the
lexical triggers are not merely imprecise, they are **actively harmful on questions the existing
sets do not contain**, and nothing in the existing 90 could have revealed it.

### Standing limits on these numbers

- `status: "draft"` on all 15. No blind-audit round has looked at a single candidate, so every
  number here is a floor.
- 15 questions, so a one-question difference is p=1.0 on a sign test. **6/15 against 5/15 is not a
  significant result** and must not be reported as one. What is solid is the *mechanism*, isolated
  by ablation and explained by a word in the question — not the margin.
- Seven questions are absent in every configuration (R-004, R-006, R-007, R-009, R-011, R-013,
  R-015). R-004 has 0% lexical overlap with its passage and nothing finds it at any depth, which
  makes it the most useful single question in the set.
