# Episode 6: the time questions, measured for the first time

`questions-time.jsonl` had never been run against a retriever. These are its first numbers.

```
tools/ratchet-proto/run.py \
  --golden src/main/resources/golden/questions-time.jsonl \
  --audit \
  --embedder ollama:nomic-embed-text:v1.5 \
  --reranker cross-encoder/ms-marco-MiniLM-L-6-v2 \
  --only S3,X1,X2,H1,E5-1,E5-2        --out results/episode-6/time-laptop
  --only S3,X1,X1b,H1,H1b,E5-1,E5-1b,E5-2,E5-2b --out results/episode-6/time-laptop-keep
```

`--audit` is passed empty on purpose: no blind-audit round has ever looked at a time candidate, so
every number here is a floor in the same sense the held-out numbers are.

## What it says

The main line scores **0 of 7 history questions**, and the one history question it could reach it
loses at the step that was added to fix the present. Full reading in the series plan, under
*Episode 6: links, history and time*. The three results that matter:

1. **The ratchet rejects Episode 5's own line on this set.** `X1` fails against `S3`
   (Hit@4 4→3, all@4 4→3, −1 G-057) and `E5-1` fails against `S3` (stale@4 4→6). The rule the
   series publishes, applied to the new set, calls the published steps a regression for time.
2. **G-057 is the spine.** Its expected unit, `004/design.html#D-005`, is `lifecycle=current` —
   the filter never touches it. The **supersession redirect** takes it out, swapping it for D-006
   at rank 3. D-005 is the named motivating example for that redirect in the published Episode 4
   article. Episode 4's hero fix is the exact unit a history question asks about. Both redirect
   rules do it: `X1b`/`E5-1b`/`H1b` (keep the replaced unit behind its successor) are identical on
   every measure, because that rule governs applied changes, not a decision superseded by a sibling.
3. **Four of the five as-of "hits" are not hits.** On G-062 through G-065 the same address is both
   the expected answer and a listed stale answer, so Hit@4 and stale@4 fire on one passage. The
   address is right and the revision served is wrong, and an address-level measure cannot tell the
   difference. `as-of` and `as-known` are unmeasurable here until there is a revision axis.

## Confirmed on the 5090 — run 26 Oct, `e59d5c9` on `episode-4-prototype`

All 24 through the same six configurations on `nomic`, `qwen3-emb-8b` and `qwen3-emb-8b-qwen3-rr`,
against an unshallowed corpus that `revisions.py` verified. Results in
`tools/ratchet-proto/results/5090-*-time.{md,jsonl}` on that branch. `E5-1`:

| tier | Hit@4 | stale@4 | history | h-decoy | as-of | what-changed | as-known |
|---|---|---|---|---|---|---|---|
| laptop, nomic | 7/24 | 6 | **0/7** | 3/3 | 4/5 | 0/5 | 0/4 |
| 5090, nomic | 10/24 | 7 | **0/7** | 3/3 | 4/5 | 2/5 | 1/4 |
| 5090, Qwen3-Emb-8B | 10/24 | 8 | **0/7** | 3/3 | **5/5** | 1/5 | 1/4 |
| 5090, + Qwen3-Reranker-8B | 11/24 | 10 | **0/7** | 3/3 | **5/5** | 2/5 | 1/4 |

1. **History is 1/7 on `S3` and 0/7 on `X1` for every model.** The mechanism is wrong, not the
   model, and that is what the run existed to establish.
2. **A better model makes this set worse.** `stale@4` on `E5-1` goes 6 → 7 → 8 → 10 while `S3` stays
   at 4. A better retriever finds the right address more often, and the right address holds the
   wrong text.
3. **The best configuration scores 5/5 on `as-of`, all five the artefact.** Counting only hits with
   no stale passage in the same top four, the best row's 11 hits are 4 clean and 7 artefact; the
   clean ones are G-059 and G-061 (decoys, wanting current text) and G-067, G-068 (`what-changed`).
   Of the 21 questions that genuinely ask about the past, the best models available answer **two**.

## Repriced against the revision axis — `1096536`

`Revisions` is built (`ratchet/revisions.py`, with `GitHistory` as this corpus's adapter), and
`revisions.py --score` re-prices a run already on disk. Nothing is re-run and no retrieval changes,
so the correction is arithmetic over existing rankings:

```bash
tools/ratchet-proto/revisions.py --score results/episode-6/time-laptop.jsonl \
    tools/ratchet-proto/results/5090-*-time.jsonl
```

| run · `E5-1` | Hit@4 | revision-aware | as-of | as-known |
|---|---|---|---|---|
| laptop, nomic | 7/24 | **3/24** | 4 → **0** | 0 |
| 5090, nomic | 10/24 | **5/24** | 4 → **0** | 1 → **0** |
| 5090, Qwen3-Emb-8B | 10/24 | **4/24** | 5 → **0** | 1 → **0** |
| 5090, + Qwen3-Reranker-8B | 11/24 | **5/24** | 5 → **0** | 1 → **0** |

**Every `as-of` and every `as-known` question scores zero** — all nine cutoff questions, every
configuration, every model. Qwen3's perfect 5 of 5 is 0 of 5.

Of the best row's five survivors, three are history-decoys (G-059, G-060, G-061) wanting *current*
text, so not questions about the past. The other two are `what-changed`, and the coverage check
finishes them: one snapshot supplies 1 of G-067's 4 declared versions and 1 of G-068's 2, so a
reader gets one state and cannot see the change. G-069 is 0 of 4.

**Of the 21 questions that genuinely ask about the past, this line answers none completely.**

Cross-checked by a method sharing nothing with it: counting hits that carry no stale passage in the
same top four gives G-059, G-061, G-067, G-068 — the same two `what-changed` questions. The one
difference is G-060, a decoy whose expected address is also listed stale, which the ranking test
drops because it cannot tell a stale revision from a stale passage.

## Reachability, before any retrieval

Of 29 expected addresses across the 24 questions: **18 reachable, 11 removed by the lifecycle
filter, 1 absent from the corpus entirely**. Six questions are wholly unreachable at any depth by
any model — G-054 (rejected), G-055, G-056, G-073 (superseded), G-058 (withdrawn), and G-072, whose
`plan.html` does not exist at `c64d41c` because it was renamed to `design.html` on 2026-07-31.

G-051 and G-052 are permanently partial: one of two expected addresses is withdrawn, so
all-evidence@4 is out of reach while the filter stands.

## A fix the run needed

`run.py:element_texts` read every expected address out of the checkout and crashed on G-072, whose
page is not in this snapshot. A missing page now yields the empty text, exactly as a missing element
already did. The time questions make that case ordinary rather than exceptional: they name addresses
as they stood at a cutoff, and this corpus is one snapshot.

## The ground truth is recoverable from git — tested 26 Oct

The prior question, before any retrieval design: can the answers be recovered at all?

- **All 23 distinct commits** named in `questions-time.jsonl` (29 mentions; several questions share
  one) are reachable in the unshallowed clone.
- **8 of 9 cutoff questions** recover their address as it stood at the cutoff, and **all 8 differ
  from what the retriever serves today**. The ninth, G-075, has no page at its cutoff — correct,
  since it is the unanswerable-by-design row.
- Spot-checked against each question's own `core`, matching word for word: G-062 "16-file tree" then
  against "40-file tree" now; G-065 "Under spectastic-vivid" then against "Every artifact" now;
  G-072 "Status Accepted" then, address absent now.
- **G-072 is recoverable after all.** It is unreachable by a snapshot index and clean under a
  revision one, so the earlier "unreachable regardless of any mechanism" was wrong.

**First-parent position is the index key.** It reproduces the declared before/intermediate/after
order on all five `what-changed` questions, including G-069, whose two intermediates share a
committer timestamp so no date can order them.

Three research gotchas corrected against the real repository: spectastic has **0 merge commits** in
722, so neither the `--before`-traversal trap nor the non-unique-topological-order trap applies
here. Committer date is monotonic along first-parent (0 backward steps) and **author date is not**
(one backward step, `b88df5e8` → `9691dc25`) — but committer date is not injective: 68 commits share
a timestamp, in 16 groups. Order by position, derive it once, never sort by a date at query time.

The probe that produced this is `tools/ratchet-proto/revisions.py` on `episode-4-prototype`
(`1ef9377`), and is the seed of the `Revisions` port. It re-runs in a second with no models:

```bash
python3 tools/ratchet-proto/revisions.py --spectastic ../spectastic
```

It exits non-zero on an unreachable commit and names the likely cause, because the likely cause is a
`--depth 1` clone and both runner scripts make one.


## Retrieval over the slice — 26 Oct

Once the corpus can express "then", Episode 4's pipeline runs on a slice unchanged, and scoring is
revision-aware for free: the slice holds only what was in force, so a retrieved address *is* the
right revision.

| configuration | answered |
|---|---|
| snapshot corpus, every model and every step | **0/9** |
| T1 BM25 over the slice | 4/9 |
| T2 + identifier lookup (G-072) | 5/9 |
| T5 + dense and the cross-encoder | 6/9 |
| T7 + the named spec ranked and fused | 6/9 |
| **TD + the named document, routed, scope-local statistics** | **8/9** |
| TE the same, reading a bare spec as its `spec.html` | 8/9 |

**G-075 is the abstention row and it is not a free pass.** It asks whether the archive described
something on 11 September 2026; it did not, because spec 111 was first recorded on the 13th. Asked
against the snapshot, the pipeline returns a top four made *entirely* of spec 111, with the passage
the golden set marks stale at rank 3 — on the laptop and on the 5090, Qwen3-Reranker included. It
would answer "yes", with citations. Against the slice, spec 111 is not in the collection, so no
stale passage can appear. A question is counted answered here only when no stale passage reaches the
top four, which the snapshot fails and every slice configuration passes.

**G-072 is the identifier lookup's win**, and the cleanest demonstration: its unit is
`004/plan.html#D-005`, a path the current archive does not have, and in the slice it is simply there.

**The standing filter earns its step without moving the number.** `095/tasks.html#T-1203` is
`superseded` today; at p551 no retirement had been recorded, so the slice carries it as `current`.
A snapshot has one lifecycle per address; a state has one per interval.

**The router.** Questions naming a spec are answered by the scoped ranking and *ruined* by the
cross-encoder (G-063 5th → absent, G-074 1st → 14th). Questions naming nothing — "the npm publishing
spec", "the spec verb" — are found only by the cross-encoder. Disjoint by mechanism, so `route_scope`
reranks only when the question named no haystack.

**A correction worth keeping.** The scope ranking was dense-only at first, on the reasoning that a
cosine has no collection statistics so restricting it is exact, while restricting BM25 would
reproduce this episode's own error. That holds only for BM25 scored with *global* statistics over a
filtered row set. An index built over the scope has the scope's own statistics — correct for exactly
the reason an index per cutoff is correct. The same sentence twice, once about time and once about
scope. The lexical half is the stronger one: spec-local BM25 puts G-063's unit 1st of 91 and
G-074's 1st of 201, against dense's 7th and 2nd.

That retired `doc_scope_default`, which existed only to buy G-063 and was the one choice made with
the answer in view. With the scope's own statistics G-063 comes 4th without it, so `TE` equals `TD`.
**8/9 is the honest number.**

### G-073 is unreached, and it is a negation failure

It asks whether an open task told implementers *not to add* a new resource kind. The answering unit
says to leave the kind list untouched; every ranker prefers the tasks that *add* it.

| ranker | rank of the answering unit |
|---|---|
| dense, within the 57-unit document | 18 |
| BM25 with the document's own statistics | 7 of 57 |
| the two fused | 9 of 57 |
| `ms-marco-MiniLM-L-6-v2` | 60 of 215 |
| `ms-marco-MiniLM-L-12-v2` | 58 of 215 |
| `mxbai-rerank-base-v1` | 26 of 215 |

Two bi-encoder configurations and three cross-encoders agree. Nothing in this stack addresses
negation, and the decoy is additionally the very change that would later retire the answer.


## The negation probe: pointwise judging saturates

`--reranker ollama:<model>` asks an instruction-following model, per passage, whether it answers the
question, with a fixed instruction naming negation explicitly. It refuted its own hypothesis.

G-073 moved 9th → 6th in its document, and **not because the judge disagreed**: it scored **10 out
of 10 for six different passages**, T-1203 and the "add screen to RESOURCE_KINDS" tasks alike.
Nothing was separated and the order came from the tie-break. On the two questions already working
it was harmless — G-063 2nd → 1st, G-074 1st → 1st — so it is non-discriminating rather than wrong.

**Pointwise relevance judging saturates.** Every task in that document *is* about the question's
topic, so an absolute score cannot express "this one and not that one". The negation distinction is
comparative by nature, and a scale that rates each candidate alone cannot carry it.

## Listwise judging finds it

Ask for a comparison rather than a score — one call, every candidate, the model must choose an
order (the RankGPT shape). Over the named document's own top twenty:

| question | fused rank | after listwise |
|---|---|---|
| **G-073** | 9 | **1** |
| G-063 | 2 | **1** |
| G-074 | 1 | **1** |

Each step of this chain came from the previous failure rather than a guess: negation defeats lexical
and dense retrieval, then three MS-MARCO cross-encoders (as the literature predicts), then a
pointwise judge that saturates — so ask it to compare, and it chooses correctly.

**A near-miss worth recording.** Ollama defaults the context window to a few thousand tokens
whatever the model declares — qwen3:8b declares 40,960 and its modelfile sets none — and a longer
prompt is **silently truncated**. A listwise call over fifty candidates is about 7,500 tokens, so
the tail of the list would never have been read while the reply still looked like a ranking. The
twenty-candidate probe survived only because it fitted. `num_ctx` is now explicit and the call
checks `prompt_eval_count` and raises: a ranking over candidates the model never saw is worse than
no ranking, because it is indistinguishable from one.

## The arms cannot move the main line, measured

| | S3 | X1 | X2 | H1 | E5-1 | E5-2 |
|---|---|---|---|---|---|---|
| before the arms | 32/47 | 32/47 | 33/47 | 36/47 | 32/47 | 33/47 |
| after | 32/47 | 32/47 | 33/47 | 36/47 | 32/47 | 33/47 |

Identical on Hit@4, all-evidence@4, MRR to three decimals and stale@4, every row. Every new path is
behind a flag defaulting false and the one edit to existing flow is an extract-variable whose `else`
branch is the old line verbatim — but that is an argument, so it was run as well.


## Withdrawn is not superseded, and only one of them is a time problem

Asking of the seven **history** questions *is the expected unit admissible at any position at all?*
splits them in two. (Positions come from the ground truth, so this is a ceiling — what a trigger
could win if one existed, not a mechanism.)

| | now | admissible at | |
|---|---|---|---|
| G-054 | `rejected` | p510–525 as `current` | the lifecycle changed |
| G-055 (×4) | `superseded` | p495–556 as `current` | the lifecycle changed |
| G-056 | `superseded` | p380–560 as `current` | the lifecycle changed |
| G-057 | `current` | p380–LIVE | admissible now; the *redirect* drops it |
| G-051, G-052 | `withdrawn` | **never** | born withdrawn |
| G-058 (×2) | `withdrawn` | **never** | born withdrawn |

A withdrawn proposal was never anything else — its units carry `withdrawn` from first state to last
— so no position would have admitted them. **Bitemporality cannot reach them**, not because the
corpus lacks anything but because nothing about them ever changed. Superseded and rejected are the
opposite: the lifecycle moved, so a slice from before the move admits the unit.

Over the whole archive, of 12,022 addresses that have ever existed:

| | addresses | |
|---|---|---|
| never excluded | 11,843 | the filter never touches them |
| **excluded at some point, not all** | **97** | **a slice reaches these** |
| excluded in every state | **82** | no position admits them (81 withdrawn, 1 rejected) |

So the filter's 196 removed units today (96 superseded, 90 withdrawn, 10 rejected) are **two
populations**, and the series has been describing them as one. The long-carried *lifecycle
admission* arm therefore has a precise scope at last: it is needed only for born-withdrawn records,
it is the only thing that can reach G-051, G-052 and G-058, and it is not a time mechanism at all.


## What the 8/9 does and does not cover

`run_timeline` scores the **nine questions carrying a single `cutoff`** — the five `as-of` and four
`as-known` rows. That is the claim: eight of those nine, against zero on the snapshot. It is not
eight of twenty-four.

The other fifteen stand as follows:

| stratum | n | state |
|---|---|---|
| `as-of` + `as-known` | 9 | **8 answered** (TD), G-073 outstanding |
| `history-decoy` | 3 | 3 answered — they want current text, which the snapshot already serves |
| `history` | 7 | **0 answered.** Four are reachable in principle (G-054, G-055, G-056 at an earlier position; G-057 needs the redirect taught about time), three are born-withdrawn and beyond reach. No mechanism picks the position, because the question states no cutoff. |
| `what-changed` | 5 | **unmeasured, and not merely unanswered.** These carry a `window` with a `from` and a `to`, so answering one means retrieving from *two* slices and saying what differs. Nothing is built for that shape, and `run_timeline` skips them. |

So the honest shape of the episode is: a bitemporal corpus answers the questions that name **one**
point in time, almost completely. Questions that name **two** points need a mechanism that does not
exist yet, and questions that name **none** — the history stratum — need the cutoff to be inferred,
which is the trigger problem in a new guise.


## 9/9: pin what the reader picks

The listwise reader could answer G-073 from the first probe. Getting its answer *through* the
pipeline was the difficulty, and three shapes failed the same way.

| shape | hits | why |
|---|---|---|
| `T9` the reader as an RRF vote beside the ranking | 5/9 | two confident, opposed orderings fused commit to nothing — everything lands 7th or 8th |
| `TG` the reader on the named scope, no open reranker | 5/9 | fixes the scoped questions, loses G-062 and G-066, which name no spec and have no other mechanism |
| `TH` the reader on the scope, cross-encoder on the rest | 7/9 | the reader works and the fusion undoes it: G-073 moves 28th → **16th** and stops, outvoted two to one |
| **`TJ` what the reader picks, pinned** | **9/9** | the reader's answer survives the pipeline |

`TH` also settled a design question. **The two paths want different readers**: a listwise reader
discriminates where a cross-encoder cannot and costs ~4.5 min over fifty candidates against ~90 s
over twenty, while a cross-encoder is cheap over fifty and is the only thing that finds questions
naming no haystack at all. One reranker for both was the wrong *shape*, not the wrong model, so
`Pipeline` takes two.

The fix used a seam that already existed: an identifier hit is **pinned** above the ranking and
never reranked. `idlookup` refuses to pin a whole spec, rightly — but the reader's *selection*
inside that spec is not a whole spec, and a listwise call returns the few identifiers it finds
useful (three of twenty), so the set is chosen by the reader rather than by a `k` I picked.

| question | TD | TJ |
|---|---|---|
| G-063 | rank 4 | **rank 1** |
| G-073 | (28) | **rank 1** |
| the other seven | unchanged | unchanged |

`TJ` is `ids + lifecycle + dense + rerank + scope_rrf + doc_scope + scope_pin` and carries **no
`route_scope`** — the one arm that failed its generalisation test is absent from the answer. G-075
still abstains with nothing pinned, because it names no spec and so has no scope to read.


## Validated: a selection is not an enumeration

`scope_pin` answered all nine and cost three main-set questions (33/47 → 30/47), worse than the
router it replaced. The diagnosis was the opposite of what I expected.

**The answer was inside the named scope all three times.** Pinning was not wrong in principle; the
*amount* was. On G-031, G-032 and G-044 the reader selected **15, 20 and 19** of twenty candidates,
against **3 of twenty** on the cutoff questions. Pinning a selection that includes almost everything
pins the whole scope — exactly what `idlookup` refuses to do, for exactly the reason it refuses.

So the failure is the reader's **selectivity**, not the seam. A selection covering most of what it
read is an enumeration, not a judgement: it carries no discriminating information, so there is
nothing to pin. `scope_picks` returns nothing unless the reader named a strict minority of its
window — a rule with a prior justification rather than a fitted threshold, but one written after
seeing both failures, so it had to be measured both ways.

| | cutoff questions | main set |
|---|---|---|
| `scope_pin` alone | 9/9 | 30/47 — **fails** |
| **`scope_pin` + minority rule** | **9/9** | **33/47, all@4 31, stale@4 0, ratchet `(True, [])`** |

Not a net wash: **lost none, won none**, identical on every stratum. And it fires on three of the
nine — G-063, G-073, G-074 pin three units each, G-072 pins one from the identifier lookup, the rest
pin nothing. A mechanism that touches three questions and leaves forty-four alone.

## The configuration, and the arc

**`TJ`** = `ids + lifecycle + dense + rerank + scope_rrf + doc_scope + scope_pin`, over a bitemporal
corpus, with a cross-encoder on the open ranking and a listwise reader on the window the question
named. **No `route_scope`, no `doc_scope_default`** — both arms flagged as possibly fitted were
measured and are absent from the answer.

1. The snapshot answers **0/9**, and a better model does not help: `stale@4` gets *worse* as models grow.
2. A bitemporal corpus is **1.36×** the snapshot, not 555×; a slice costs **0.09 s**.
3. Over a slice, Episode 4's own pipeline reaches **6/9** with no new mechanism.
4. A named spec is a named haystack, and a subset is a collection with its own statistics — **8/9**.
5. G-073 is negation: it defeats lexical, dense and three cross-encoders; a pointwise judge
   saturates at 10/10, a listwise one chooses.
6. Fusing a confident reader throws its judgement away, three times over. Pinning its *selection*
   keeps it — **9/9**; pinning its *enumeration* breaks everything else.


## The 5090 verdict: the ratchet takes the 9/9 away

Three runs on the main-line tier, verified from the committed JSONL.

**The headline reproduces.** With `qwen3:8b` reading the named scope, `TJ` is **9/9**, pinning at
rank 1 on G-063, G-072, G-073 and G-074. Nothing was laptop-specific.

**Qwen3-Reranker-8B nearly replaces the reader, and the trade is exact.** The `selects` guard holds,
so it can only reorder — and reordering alone lifts G-073 from **rank 29 to 2**, the negation case
that defeated three MS-MARCO cross-encoders. But the same rerank sends G-074 from **1 to 5**. 8/9.
An instruction-following *reranker* can see negation; it still cannot do what a *chooser* does, which
is leave the rest of the ranking alone.

**And `scope_pin` fails the main-set ratchet here.**

| | Hit@4 | all@4 | stale@4 | MRR |
|---|---|---|---|---|
| `E5-2` | 46/47 | 42 | 0 | 0.890 |
| `G2` scope arms, no pin | 46/47 | 42 | 0 | 0.890 |
| `G4` + pin | **45/47** | 42 | 0 | 0.873 |

`(False, ['Hit@4 46→45'])` — lost G-036, won nothing. It pins on 15 of 53 questions and hurts two:
G-033 `[1,17] → [4,19]` and G-036 `[17,4] → [16,5]`.

**Both are relation questions, which name a spec as the premise rather than the haystack.** G-033
names 023 and answers span 023 *and* 024; G-036 names 076 and answers span 076 *and* 071. Pinning
three units of the named spec fills the top four with premise material. At 33/47 that was absorbed;
at 46/47 there is no slack — **a pin on a strong baseline can only displace.**

A query-time rescue rule would fix this if one existed. "A unit of the named scope is already in the
top four" is **true for every pinned question, G-073 included**, so that signal is dead.

## Where the line actually stops

| | laptop | 5090 | main-set ratchet |
|---|---|---|---|
| snapshot, any model | 0/9 | 0/9 | — |
| `T3` slice + standing as of then | 5/9 | 5/9 | — |
| `T5` slice + Episode 5's own line | 6/9 | **7/9** | clean |
| `TF` + the scope arms | 6/9 | *to run* | **passes both tiers** |
| `TD` + the router | 8/9 | 8/9 | **fails**, laptop 33→31 |
| `TJ` + the pin | **9/9** | **9/9** | **fails**, 5090 46→45 |

A step that fails the ratchet is a result, not a step — that rule applies to this work too. So
`route_scope` and `scope_pin` are **epilogues**, published with their numbers as Episode 5 published
a failing step 6, and the main line stops where the ratchet says.

Which leaves a better claim than the one being chased: **on the tier the main line is defined on,
the bitemporal corpus alone takes the cutoff questions from 0 of 9 to 7 of 9 with no new retrieval
mechanism at all** — identifier lookup, standing filter, dense fusion and cross-encoder, unchanged,
pointed at the right collection. Everything after that buys one or two questions and costs a
main-set question somewhere.


## The section seam: a why-question's collection is `#context`

G-080 was the 5090 main line's only miss and had **no structural signal at all** — no id, no named
spec, no typed edge. Where it actually lands is the clue: the pipeline finds the right *page* and
the wrong *section*. Rank 8 was `025-id-uniqueness/spec.html#scenarios`; the answer is that page's
`#context`.

That generalises because the archive says so: **all five main-set `intent` questions expect a
`#context` unit**, and spectastic's schema puts the problem a spec exists to solve there. Over the
129 in-force `#context` units the expected one ranks **first** for G-077, G-079, G-080 and G-081,
sixth for G-078. A subset is a collection, for the fourth time this episode.

Fused, never pinned: the `why` trigger fires on 19 questions across the three sets and only 4 want a
`#context`.

| main set, laptop | Hit@4 | all@4 | stale@4 | ratchet |
|---|---|---|---|---|
| `E5-2` | 33/47 | 31 | 0 | — |
| `G9` the earlier keepers | 33/47 | 31 | 0 | clean |
| **`GA` + `context_scope`** | **35/47** | **33** | 0 | **`(True, [])`** |

Won G-078 and G-081 from absent to rank 3, lost nothing. G-080 reaches 21 — reachable where it was
nowhere, with the fusion diluting a list that had it first, so the 5090's stronger reranker is the
test that matters.

### Every other section kind is as sharp, which is the trap

| collection | size | expected unit's rank |
|---|---|---|
| `#changelog` | 596 | G-051 **2**, G-052 **3**, G-058 9 |
| `#risks` | 415 | G-058 **1**, H-010 **2** |
| `#intent` | 156 | G-006 **1**, G-018 **1**, H-009 **2**, H-011 **1** |
| `#requirements` | 121 | G-031 **1**, G-032 **1**, H-004 **1** |

Five more questions are one trigger away — and **both trigger-free designs fail, in opposite
directions**:

- **One combined collection** of all 4,926 section units is blunter: G-051 2 → 13, G-052 3 → 14,
  G-080 1 → 41, G-078 6 → 97. Mixing `#changelog` with `#risks` and `#context` reintroduces the
  competition that narrowing removed.
- **One fused list per kind** is worse: there are **31 kinds** with 40+ units, so 31 lists each
  offer a confident rank-1 and the answer lands in a tie-tier near 9th. G-051 2 → 38, G-078 6 → 164.

So the per-kind collection is sharp *and* needs to know which kind. For `#context` that mapping is
the archive's own schema and holds 5 of 5. For `#risks`, `#changelog` and `#intent` it would mean
inventing a trigger from two or three examples each — the line between what the schema says and what
these questions say. **The arm ships for `#context` and stops there**, and the other four are
recorded as one trigger away rather than quietly fitted.
