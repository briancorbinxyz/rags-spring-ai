---
stage: seedling
publish: false
---
# RAGs to Riches, Series Plan

>[!note] Current state, 23 Oct 2026
>The sections from "Current state" to "Changes to the plan below" are the record as of 23 Oct 2026, and they win over anything further down. Everything below them is the plan as written on 14 Oct.

## Current state

- **Golden set v2 is frozen** (`episode-4` branch, `src/main/resources/golden/`). The main set has 53 questions, 47 of them answerable. G-005, G-035 and G-040 are dropped. G-076 and a new intent stratum (G-077 to G-081) are added. 14 passages are accepted by a blind audit. There are 13 held-out questions and 24 time questions (`questions-time.jsonl`). Every audit decision is in `results/episode-4/`.
- **Episode 4's main line is Steps 1 to 3 plus the supersession redirect**, and the Java now reproduces it under Gradle: `./gradlew test` passes with `RatchetTest` asserting Hit@4 30/32/32/32, all-evidence@4 24/26/26/26 and stale@4 3/3/1/0, cross-checked with `ir_measures` (`Success@4` 30/32/32/32, `RR@10` up to 0.624, `Judged@4` 0.61). Episode 3's search scores 10/47. Draft: [[RAGs to Riches with Spring AI, Episode 4, Knowledge Base]].
- **Episode 5's laptop tier is measured on the frozen set, and both steps fail the ratchet.** Branch `episode-5`, four commits, pushed. With `nomic-embed-text:v1.5`: step 5 (BM25 + dense, RRF k = 60) reaches Hit@4 36/47 and all-evidence@4 33 and **fails on relation 6 → 4** (losing G-031 and G-036); step 6 (+ `ms-marco-MiniLM-L-6-v2`) falls to 32/47 and 28 and **fails outright**. `qwen3-embedding:0.6b` tells the same story at 35/47 and 33/47. The Java matches the prototype exactly: the same 50 results, same order, all 53 questions, steps 4–6. Draft: [[RAGs to Riches with Spring AI, Episode 5, Bigger Models]].
- **Episode 5's 5090 tier is run on the frozen set** (23 Oct 2026, from WSL), and the arc holds with a twist. Three runs: `nomic` as the anchor, which reproduces the Mac's nomic line exactly (X1 32/47·26, H1 36/47·33, failing on relation 6 → 4), `Qwen3-Embedding-8B` with `bge-reranker-v2-m3`, and the same embedder with `Qwen3-Reranker-8B`. Step 5 **passes** on the Qwen3 embedder; step 6 **passes** with Qwen3-Reranker at **44/47** and all-evidence@4 40, and **fails** with `bge-reranker-v2-m3` (36/47, all-evidence@4 32). 44 is one off the ceiling: step 5 leaves an expected passage within the top 50 for 45 of the 47. Qwen3-Reranker also separates the unanswerable questions cleanly — they top out at 0.869, the answerable start at 0.915 — which is Rule 10's evidence.
- **G-043 is cleared, and `stale@4` was the only thing step 6 was failing on.** The main line's supersession redirect now **drops** what was superseded, applied changes included, instead of keeping it behind its successor: spec 095's FR-013 was rewritten by three applied changes in two days, and nothing on those proposals says which rewrite is live. Re-run on the 5090 on 23 Oct, the new rule takes `stale@4` from 1 to 0 at identical Hit@4, all-evidence@4 and per-stratum counts on all three runs, and Episode 4's line does not move. Re-run again the same evening with `S4` and `S5` in the chain (`a71891d`), so every verdict compares against the row before it rather than collapsing four steps: step 5 passes, step 6 passes with Qwen3-Reranker, step 6 fails with `bge`. Why the rule changed: [[RAGs to Riches, The G-043 Problem]]; the runs themselves: [[RAGs to Riches, What Is Left to Run on the 5090]].
- **The hosted tier leaves Episode 5** and becomes an episode of its own, "local against remote", below. Without Bedrock access it would be OpenAI `text-embedding-3-large` alone, and OpenAI has no reranker, so the tier could only run half the pipeline — which muddies the comparison rather than extending it. Episode 5's thesis stands on the two local tiers.
- **The audit is done (24 Oct), and it changed nothing anyone had quoted.** Round 3 read 272 unjudged passages — every one in a top four across the three 5090 runs at steps 4, 5 and 6 — and accepted **one**: `031-init-tools/spec.html#scenarios`, which answers G-076 whole. Four were *partly*, 267 *no*. No Hit@4 moved on any tier; the one number that did is the laptop's step 6 all-evidence@4, 28 → 29. `Judged@4` rises to 0.73 on the keyword line and **0.55** on the 44/47 row. The golden set stays frozen: `BlindAudit` (Java) and `run.py --audit` (prototype) fold each round's acceptances in at scoring time instead. The finding worth keeping: dense search returns a great deal that looks like an answer to a machine, and almost none of it is one to a person. Superseded below: Dense retrieval surfaced 152 new (question, passage) pairs over 36 questions that nobody has judged, mostly identifier (44) and intent (37). `Judged@4` is 0.61 on the keyword line, 0.53 on the laptop's fused lines, 0.56 on the 5090's step 5 and **0.49** on its 44/47 row — the better the retrieval, the less of it anyone has checked. Rounds 1 and 2 are in `results/episode-4/golden-audit-2026-10.jsonl` (226 decisions), so this is round 3. All of it lower than the earlier 71%/60–63% estimate, which assumed this audit had happened.
- **Known limit of the TREC cross-check**: a split unit's parts share an address, and `TrecFiles.run` writes each passage once, so when two parts of one unit land in a top four the run file promotes a fifth item and `Success@4` reads one higher than our Hit@4 (step 6, G-013). Our scorer counts items, which is what the reader is given, so it stays the published number. Not an issue for BM25-only steps; the cross-encoder causes it.

## Episode map (tentative numbering)

| Episode | Subject | Main line | Rules |
|---|---|---|---|
| 4 | Knowledge base: the golden set audit, then structure-aware search | Element units + BM25, identifier lookup, lifecycle filter, supersession redirect | 5 Know the answers before you ask · 6 Test the test · 7 Respect the seams · 8 Answer in the right tense |
| 5 | Bigger models | Hybrid search (RRF, k = 60), cross-encoder reranking; Mac against 5090; Qwen3-Reranker. Both steps fail the ratchet on the laptop; on the 5090 both pass, step 6 at 44/47 with Qwen3-Reranker-8B | 9 *Make the model beat keywords* · 10 *Know when you don't know* |
| 6 | Links, history and time | Anchored typed expansion (`X2` on the BM25 line, `E5-2` on the dense line: **+2 Hit@4 and +2 all-evidence on every tier**, 44→46 on the best); supersession followed backwards and *ordered*; query routing; and the time axis pulled forward from the later episode — as-of, as-known, what-changed | Rule 8 applied to the past on purpose · a new one on ordering |
| later | Local against remote | The same frozen set through Bedrock — Titan Text Embeddings, and Cohere Embed v4 with Rerank — against the laptop and the 5090. Correctness is the control, not the subject: cost per question, latency and its spread, what a model pinned only by its name does to a frozen golden set, and what leaves the building | *A model you don't host can change under you* |
| later | Time, continued | Whatever Episode 6's time half does not finish: valid-time indeterminacy when a cutoff lands inside a same-day cluster, and time-varying collection statistics | Rule 8 extended to dates |
| later | Judging answers | An automated judge, measured against Episode 3's hand labels | Rule 3 extended; Rule 6 applied to judges; Rule 2's caveat |
| later | Generator swap | The chat model changes | none planned |
| later | Finance corpus | Official and unofficial sources, PDFs, access | *Retrieved text is data, not instructions*; *Retrieve only what the reader may read*; Rule 7's no-seams case; Rule 5's blind spot |

## Later: local against remote

Moved out of Episode 5 on 23 Oct. Episode 5's arc is the laptop against the 5090 and it is complete without a hosted tier, so the remote models get their own episode — and the question changes with them. Not *which one retrieves best*: the local tiers already answer that, and an embedder behind an API is not going to settle it. The question is **what you give up, and what you get, by not owning the hardware**.

Correctness is the control. A remote tier clears the ratchet like any other step, because that is the series' method, but a win there is not the story.

- **Lineup, Bedrock first.** Spring AI supports Bedrock, so it stays a Spring AI episode, and Bedrock is the access that exists. Exact model ids and version dates confirmed and pinned at run time:
  - **Amazon Titan Text Embeddings** (V2, `amazon.titan-embed-text-v2:0` — confirm at run time). Available at work, so this is the one that will actually run. There is no Titan reranker, so the reranking step pairs it with Cohere's or with the local MiniLM — which is its own small result about what a provider does and does not sell you.
  - **Cohere Embed v4 with Rerank (3.5 or 4)**, also on Bedrock. The only set where both halves come from one provider, which is what makes a clean two-step remote line.
  - Alternatives if neither is reachable: OpenAI `text-embedding-3-large` (the most common embedder, supported directly by Spring AI, no reranker of its own), Voyage (embeddings plus `rerank-2.5`), and Google's Gemini embedding.
- **The dimensions that are the point.**
  - **Cost**, per run and per question, from the API's own token counts rather than an estimate.
  - **Latency**, and its spread rather than its mean: reranking is one call per passage over fifty passages, so a p95 is what a reader would feel.
  - **Drift.** A hosted model changes under the same name. Pin the id, the version date and the run date, cache the vectors and keep them — and then say what a frozen golden set is actually worth when the model behind it cannot be frozen. This is the new rule's evidence.
  - **Failure.** Rate limits, timeouts, a half-finished batch: what the pipeline does when the embedder is someone else's service. The local tiers cannot fail this way, so nothing in the code has ever had to handle it.
  - **What leaves the building.** spectastic is MIT licensed and public, so sending it to an API is fine. Saying so plainly is the point, because a private archive changes the trade — which leads into the finance corpus and *Retrieve only what the reader may read*.
  - **Operational shape.** Credentials, regions, and a vector store holding vectors from a model nobody here can re-run.
- **Same rules as the local runs.** Fixed defaults, the same configurations (S4, S5, H1), the ratchet, no tuning. Held-out questions are scored once, at the end.
- **Spring AI wiring.** Embeddings go through Spring AI's own `EmbeddingModel` for the provider. The reranker is a `DocumentPostProcessor` that calls the provider's rerank endpoint, because Spring AI has no reranking abstraction.
- **Prototype work.** Hosted backends in `ratchet/embed.py` and `ratchet/rerank.py` (credentials from the environment, a vector cache keyed by model *and* date, cost logging), and a run script of its own (`run_remote.sh`).
- **The story it can tell.** What the archive costs to embed through an API, what it costs to answer one question, how much of that you wait for, and what happens to a reproducible benchmark the first time the provider ships a new version of a model you named but do not control.

## Episode 6: links, history and time

Written 24 Oct, after research into what is already measured and what the field knows. Time is
pulled forward from the later episode by decision: an "as of" question is a relation question in
another dimension, and splitting them would mean building the same mechanism twice.

### The thesis, in one line

**An archive needs two keys, not one: a *filter* key and an *order* key, and conflating them is the
bug.** A date answers "which revision was in force on 1 July". It cannot answer "which of two
same-day changes superseded the other", because that information was never recorded at that
granularity. The order key is a *sequence*, not a time.

This is the generic pattern the episode exists to state, and it is corpus-agnostic: anything with
revisions — a wiki, a system-versioned table, a contract repository — needs both keys.

### Why this is not a new idea, and why it is not in the literature either

No source in the temporal-database canon prescribes a tie-break for equal timestamps. Every system
that needs a total order borrows a non-temporal second key: Datomic orders by transaction id and
carries `:db/txInstant` separately; XTDB breaks ties by transaction id; Launchpad/Bazaar abandoned
revision dates for branch sequence numbers because "clock problems and merged revisions mean the
revision date is not always in order". Snodgrass and Dyreson give the principled framing — a time
recorded at one granularity is *indeterminate* at every finer one, so the honest answer to a cutoff
landing inside a same-day cluster may be a set rather than a winner.

### What the corpus actually gives us (measured 24 Oct)

- **Dates are day-granular everywhere.** The markup is `<time datetime="2026-08-14">`. There is no
  finer timestamp to be more precise with.
- **Same-day collisions are the norm.** 22 specs have 2+ applied changes; **11 of them** have 2+
  applied on the same day. Spec 000 has five on 2026-06-12, spec 042 four on 2026-07-11.
- **Three clocks, and the obvious one is wrong.** A proposal's folder date is when it was
  *proposed*; the changelog entry is when it was *applied* (the entry dated 14 Aug reads "Applied
  2026-08-13-address-a-screen"); the git commit is when it was *recorded*.
- **The history is present and cheap.** `spectastic` was a shallow one-commit clone;
  `git fetch --unshallow` took 2 seconds and 20 MB. 722 commits, 25 tags, **555 of them touching
  `specs/`, each a distinct tree**. All **29 commits** the time questions reference are reachable,
  and `v0.1.0-pre.24` exists. The time questions are answerable.
- **G-043 dissolves with history present.** The three applied changes the day-granular dates could
  not separate are 19 minutes and 10 hours apart in git, strictly ordered. Episode 5 had to drop
  history to answer it; the sequence was in the repository all along.
- **But 68 commits share an identical committer timestamp**, across 16 timestamps, one covering 19
  commits. Committer date happens to be monotonic here (0 backward steps in 721) — a measured
  property of this repo, not a guarantee.

### The arms

Four of these are already coded in the prototype and have numbers. **Every number below is stale
twice over** — produced under the old `behind` redirect rule and on pre-round-3-audit qrels
(`6615f99`, 23 Oct) — so the first task is a re-run, not new code.

| key | what it does | 5090 Hit@4 · all@4 (nomic / qwen3+bge / qwen3+Qwen3-RR) |
|---|---|---|
| `X2` | `X1` + anchored typed expansion, BM25 line | 33·28 / 33·28 / 33·28 |
| `H2` | `H1` + expansion, no reranker | 37 / 38 / 38 |
| `E5-2` | the full line + expansion | 37·33 / 38·34 / **46·42** |
| `EpiB` | RRF-merge of the top 5's neighbours, all link types | 27·24 / 29·26 / 36·31 |

Expansion is one hop over typed links only (`validates`, `cites`, `target` incl. `spec-delta`,
`defer-to`), at most 5 units, BM25-ranked among themselves, **inserted after the anchor and after
the reranker**.

New arms the episode needs:

5. **Ordering resolution.** Group a spec's applied changes by the requirement their delta targets,
   order by the changelog's document order, and only the last produced the current text. This is
   the epilogue's promise and it gives back what Episode 5 gave up: `screen-as-a-kind`'s FR-013 is
   byte-for-byte the current text and was dropped only because nothing distinguished it.
6. **As-of retrieval** at a cutoff, over multiple revisions.
7. **Routing**, naive first — see below.
8. **Extract the edges already declared.** Not an experiment: a bug. See below.
9. **The sibling-artifact edge, reranked** — the one untested cell. See below.
10. **The trigger.** Expansion seeds only from `pinned_keys[0]`, and **7 of the 8 held-out misses
    have no pinned unit at all**. No edge type reaches them until the seed comes from the ranking.
    This is a separate experiment from the edge class, and it is the one `EpiB` got wrong.

### The result that is already in hand, and it is the episode's spine

`E5-2` adds **+2 Hit@4 and +2 all-evidence on every tier**, and `EpiB` loses 8 on the best line. The
literature predicts the opposite for `E5-2`: link expansion reliably helps a weak anchor
(HotpotQA recall@2 10.3 → 17.3 over TF-IDF) and *hurts* a strong one (a dense retriever reached
65.9 with no links at all; SentenceBERT plus citation neighbours scored **worse** than without).

Our pair resolves the contradiction, and the distinction is not "graphs help or hurt":

> **Expansion that reserves slots after the anchor helps. Expansion that merges neighbours into the
> ranking hurts.** `E5-2` and `EpiB` differ in exactly that, on the same runs, with everything else
> held constant.

Every negative result in the literature is a *merge*. That is a finding worth the episode on its
own, and it is not published anywhere: there is no retrieval literature on **typed** link expansion
at all, and none isolating the **pure-pointer** case — a passage that only references the answer
without discussing it, which is exactly G-036.

### What the meta declares, and what it does not

Researched 24 Oct. The intuition that spectastic's grammar implies relations — a design comes from
a spec, tasks from a design, as a 13D/A is an amendment to a 13D without saying so — is half right,
and the half that is wrong is instructive.

**Declared as MUSTs, with validator rules:** `verify → spec + tasks` (021 FR-002: *"MUST aggregate
… by reference only — as links to the anchors in the sibling spec.html / tasks.html — and MUST NOT
copy their prose"*), and `parent → child` slicing via `<spec-parent specid>` with a reciprocal
`defer-to`.

**Not declared:** design-from-spec and tasks-from-design. They live only in command markdown, and
`principles.html` P-8 disqualifies that register itself — *"command markdown, skill descriptions,
and rules files guide but never enforce"*. By spectastic's own standard they are convention.

**But the data states what the meta does not.** Of 3,280 resolved intra-spec cross-artifact
references: tasks→spec 1,413, design→spec 596, verify→spec 501, verify→tasks 393, tasks→design
187 — against spec→design **2**. A 300:1 asymmetry. An inferred `derives-from` edge is defensible
if labelled as inferred from link direction rather than claimed as declared.

**The cleanest line in the research**, and an article paragraph on its own: `verify.html` has 1,130
links and 3 bare prose references, because its relation was declared *and enforced*. Every authored
artifact is the opposite. **Declaring a relation is what makes it extractable.**

There is no DTD, XSD or RelaxNG — a design decision says so outright: *"the `rules/` directory IS
the schema"*. So the relation set is read from 64 rule files or measured from the corpus, not from
a schema document.

### Arm 8: extract what is already declared

This is not an experiment. Roughly **1,500 declared edges are missing from the graph**:

| relation | declared | extracted |
|---|---|---|
| `spec-decision grounding` | 469 | **0** |
| `spec-risk status` / `by` | 360 / 242 | **0** |
| `spec-triage layer` | ~168 | **0** |
| `spec-parent specid` | 22 | **0** |
| `li defer-to` | 674 | 166 (−75%) |
| `spec-delta target` | 770 | 423 (−45%) |
| `typed:validates`, `typed:cites` | 122, 868 | complete |

And **`principles.html` is absent from the corpus entirely** — 1,675 bare `P-N` mentions against 202
hyperlinks, so every design decision is justified against principles the graph cannot reach. That
is 14 ids in one file. Do this before inventing an edge class.

### Arm 9: the sibling-artifact edge, and the trap around it

Measured 24 Oct against the 8 held-out misses, counting reach from the retrieved top 10:

| mechanism | reaches | note |
|---|---|---|
| typed edges (today) | 2/8 | the baseline |
| typed + hrefs | 3/8 | |
| recurring anchor id | **0/8** | dead — see below |
| same-page | 4/8 | |
| design↔spec, tasks↔design, verify↔spec | **4/8** | no better than same-page |
| sibling artifacts in the spec dir | 6/8 | 100% current |
| sibling + `changes/**/proposal.html` | **8/8** | 21% history, 2% withdrawn |

Seven of the eight have *every* expected address unretrieved at depth 50, so this is new
reachability, not re-ranking. It is a **failure-mode mechanism, not a stratum mechanism**: it fixes
*right spec found, wrong artifact scored*, which is 6 of 8 held-out misses and 9 of 10 main-set
misses on the fusion line. It is also the only thing that reaches G-080, via
`verify.html → spec.html#context`, which no parsed edge touches.

**The recurring-anchor-id edge is dead on arrival.** Zero identifier-shaped anchors recur across
sibling artifacts anywhere in the corpus. Of the 321 `(dir, anchor)` pairs that do recur, `T-n` is
a namespace collision — `tasks.html#T-001` is a work item, `triage-log.html#T-001` is a triage
entry — so an unqualified id edge wires ~205 wrong edges against **zero** right ones.

**The trap: this has already been measured once, and it lost.** `EpiB` is close to a sibling edge
in effect — it seeds from the top 5, follows all link types, and `href:same-spec` is the largest
edge class at 6,754 of 12,283. It scored **36/47 against `E5-1`'s 44/47**: won one, **lost nine**,
four of them `identifier`, meaning pinned lookups were displaced. The cost explains it: sibling-dir
offers a median **242 extra candidates per query** against typed expansion's median 3 — 52× the
pool, against an `EXPAND_MAX` of 5.

**Run on the 5090, 24 Oct (`694bf0e`): measured, and dead.** `E6-1` matches `E5-1` and `E6-2`
matches `E5-2` on *every* measure — Hit@4, all-evidence@4, MRR to three decimals, every stratum,
even the abstention score distribution. Only the context-token count moves (645 against 635), so
siblings do reach the returned top four on some questions; they never change an answer. Non-answers
displacing non-answers.

By the criterion set in advance, flat is a pass: it does not lose, so `EpiB`'s result does not
generalise, and anchored insertion really is safe where RRF merging was not. But safe and useless
is useless. **The hypothesis — "a reranker that can read will convert the reach" — is tested and
false**, on the best reader available.

**The methodological finding is the keeper.** Reachability analysis said sibling edges reach 6 of 8
held-out misses, and 8 of 8 including proposals. **Zero converted.** Being in the pool is not being
chosen, and a reach number is a ceiling, not a forecast. Any future arm should be sized by what it
converts, never by what it reaches.

**And the reason, which is the episode's real lesson.** Typed expansion runs *after* the reranker:
its neighbours are never scored on text, they are placed because of what they are. `E6` put its
candidates *into* the rerank pool, where they competed on text like everything else — discarding
the one thing the edge knew. The relation nominated them; nothing let the relation place them.

> **An edge is only worth following if you let it place, not merely nominate.**

The field offers no cover either way. Across four SEC graph systems, form type is used as a filter
or an edge attribute, **never as the edge source**, and no published ablation of type-implied edges
exists. The nearest evidence base is requirements traceability, whose verdict fits: IR-based
recovery buys recall cheaply and precision expensively, and the field's own answer to the precision
ceiling is to add structural signal to the textual one. The fan-out mechanism is documented
(“neighbourhood explosion”, hub bias); its magnitude has never been measured. Cap at index time.

One borrowed caution, from the SEC's own Inspector General finding that nobody monitors Form 13F
for accuracy: **a type-implied edge is a claim to verify, not a fact.** The archive has 9 broken
intra-spec anchors already.

### The main line: typed expansion, measured twice

`E5-2` post-audit on the 5090, and it is the episode:

| | Hit@4 | all@4 | relation | verdict |
|---|---|---|---|---|
| main set, `E5-1` | 44/47 | 40 | 5/7 | start |
| main set, `E5-2` | **46/47** | **42** | **7/7** | **pass**, +2 (G-029, G-030) / −0 |
| held-out, `E5-1` | 5/13 | 2 | 3/8 | start |
| held-out, `E5-2` | **6/13** | **3** | 4/8 | **pass**, +1 (H-001) / −0 |

**What it is.** After the search has run and the reranker has had its say, *if the question named
something* — an identifier the lookup could pin — follow that pinned unit's declared typed links
one hop (`validates`, `cites`, `target` incl. `spec-delta`, `defer-to`), take at most five
neighbours, and insert them immediately behind the pinned unit. Everything below keeps its order.

**What it is doing.** The question names a thing; the lookup finds that thing; expansion hands the
reader what that thing *points at*. For a relation question this is not a heuristic — the answer
*is* the edge. "Which success criteria validate FR-008?" `validates` runs SC→FR, so the neighbours
of FR-008 across that edge are the answer by the markup's own definition.

**Why it cannot dilute.** It fires on **10 of 53 questions** — the 8 identifier and 2 relation ones,
the only ones where anything gets pinned — and changes the top ten on **3**. On the other 43 it does
nothing at all. Bounded at five, additive at the head.

**What it did when it ran:** G-029's expected passages went from ranks `[13, None]` to `[2, 3]`;
G-030's from `[None, None]` — unreachable at any depth, by any ranking — to `[4, 2]`.

It also contradicts the literature, which says expansion helps a weak anchor and hurts a strong one
(HotpotQA 10.3 → 17.3 over TF-IDF; a dense retriever at 65.9 with no links; SentenceBERT *worse*
with citation neighbours). This is the strongest anchor in the series and expansion still gains two.

### Structured query rewriting: researched 25 Oct, and not worth building

The idea: a step that recasts the question into a structured form — `{spec, id, lifecycle, intent}`
— so the pinning, filtering and routing machinery can be driven from it. It is the natural answer
to the trigger problem, and the measurement says no.

**The main-set ceiling from text extraction alone is 0 of 3.** It cannot move the headline number.

- **No question ever names an id without also naming a spec** (0 of 90), and every in-text id
  already pins (12 of 12). Extending the identifier lookup has no target on this golden set.
- Of the 22 "spec but no id" questions, the id is in the text **once** — and that one already pins.
  In nine the in-spec target is a *section* anchor (`#requirements`, `#dependencies`, `#intent`)
  that `ID_TOKEN` cannot match by construction.
- **The circularity, which is the real objection.** Unlocking H-012 means knowing the retired task
  is `T-1404`. A question-only step cannot produce that; a corpus-grounded one is doing retrieval.

**The literature agrees, and for the familiar reason — baseline strength.** Query expansion and
rewriting gain over BM25, gain modestly over tuned dense retrievers, and go *negative* in front of
a cross-encoder: query2doc takes RankT5 on DL19 from 0.737 to **0.572** and MonoT5 from 0.695 to
**0.473**. A reranker-safe variant recovers **+0.006 nDCG@10** on BEIR for several generations per
query. A controlled rewriting study measures FiQA at **−9.0%** (p < 0.001), 225 of 648 queries
degraded against 122 improved, with no cheap signal distinguishing them — lexical substitution
appears in ~96% of degraded, improved and unchanged alike.

Its policy table is the one to keep: **always-rewrite (0.554) is worse than never-rewrite (0.556)**;
a learned per-query gate reaches 0.563 and is not significantly better than never (p > 0.12); the
best non-oracle policy is **dataset-level** gating at 0.572 — decide once, offline, by measurement.
That is this series' method under another name.

**And it would break the ratchet.** query2doc's own authors report run-to-run variance of **±1.14
and ±1.63 nDCG@10** across three runs — a noise floor that swallows most gains claimed anywhere in
that literature. Temperature 0 and a fixed seed do not restore determinism. Grammar-constrained
decoding buys validity, not reproducibility. Cost: a model call measured at **>2000 ms** against
BM25's 16 ms, in a pipeline whose only query-time model call is embedding the question.

Nobody has measured the closest analogue — extract an identifier and *pin* with it, versus match it
as text — on retrieval metrics. That gap stays open.

### `defer-to` is discarded, and the seam it exposes

Found while testing the above, and worth more than the idea that found it.

All **166** `typed:defer-to` edges are unit→page. `Graph.typed_neighbours` keeps only
`kind == "u"` destinations, so the relation is parsed, stored and dropped at traversal — while the
expansion's docstring has promised "defer-to resolved to a unit" since Episode 5. Fixed 25 Oct by
resolving the page to its best unit for the question, exactly as the supersession redirect already
resolves a superseded page.

**The fix changes nothing, and why is the finding.** All 166 `defer-to` edges originate from
*section* anchors — `#requirements` 143, `#scope` 17, `#goals` 6 — and none from an id-shaped one.
Expansion can only anchor on what the identifier lookup pinned, which is always an id. So:

| | anchored on | reaches |
|---|---|---|
| identifier lookup → expansion | **id** anchors | `validates`, `cites`, `target` |
| `defer-to` | **section** anchors | nothing — no path in |

Zero defer-to edges are reachable from any of the ten pinned anchors. The same fact appears in the
structuring measurement: nine of the "spec but no id" misses target section anchors. **The
interesting relations hang off sections, and the pinning mechanism only sees identifiers.** That is
a seam, not a bug, and it is the clearest statement yet of what the trigger arm has to fix.

Keep the fix regardless: it is correct, costs nothing, and the moment anything anchors on a section
those 166 edges become live.

### What survives, in three families

Measured over every miss in both sets:

| family | reach | status |
|---|---|---|
| **Lifecycle admission** | H-009, H-010, H-012 + ~~5 time questions~~ **3** (see *Withdrawn is not superseded*) | the word is literal in the question |
| **Defer-to resolution** | H-003, H-004, G-031/G-032 at depth 4 | fixed, but unreachable until the trigger changes |
| Prose-only, no edge | 8 of 12 misses | unreachable by any field; 5 units have **zero** typed edges |

`EXCLUDED = {"withdrawn", "superseded", "rejected"}` removes 196 of 11,492 units and **keeps all
1,710 history units** — so the filter is narrower than it looks, but those three classes make three
held-out and five time questions unreachable at any depth. **Corrected 26 Oct:** only three of
those five are beyond reach — the born-withdrawn ones — because the other two were `current` at an
earlier position and a slice admits them. See *Withdrawn is not superseded, and only one of them is
a time problem*.

**Two findings that cut against each other.** On the main set a word list beats a model: the
lifecycle words are literal. On the time set a model beats a word list: its router records four or
five HISTORY questions whose intent is semantic and not lexical — *"set aside"*, *"shelved"*,
*"first settle on"*, *"ever consider"*. The answer differs by question set, which is exactly the
dataset-level gating the literature found to be the only policy that beats doing nothing.

**And a hard precision constraint:** two of the three history-decoys carry the trigger word while
needing *current* units, and the golden set's own router calls G-061 an "expected misroute". A
lifecycle field must be a **rejectable hypothesis, not a switch** — the same shape as the
placement-versus-nomination lesson from `E6`.

### An incidental finding

- **G-072's expected unit lives in `plan.html`, of which the corpus holds zero pages** at
  `c64d41c`, because the file was renamed to `design.html` on 2026-07-31 in `836dc1a`.
  ~~Unreachable regardless of any mechanism~~ — **that was wrong, and measuring it on 26 Oct showed
  why**: at G-072's own cutoff the page exists, and `plan.html#D-005` recovers cleanly with the
  status the question asks for. It is unreachable by a *snapshot* index and perfectly reachable by a
  revision one. `run.py`'s ground-truth loader did crash on it, and a missing page now yields the
  empty text, as a missing element already did.

### The time questions, measured 26 Oct, and the line scores zero on history

The gap is closed: all 24 ran, on the laptop tier, through `S3`, `X1`, `X2`, `H1`, `E5-1`, `E5-2`
and the three `b` variants. `results/episode-6/` holds the run files, the per-question JSONL and the
reading. No audit round has ever looked at a time candidate, so every number is a floor.

| key | Hit@4 | all@4 | stale@4 | history (7) | h-decoy (3) | as-of (5) | what-changed (5) | as-known (4) |
|---|---|---|---|---|---|---|---|---|
| S3 | 4/24 | 4 | 4 | **1** | 1 | 1 | 0 | 1 |
| X1 | 3/24 | 3 | 4 | **0** | 1 | 1 | 0 | 1 |
| X2 | 3/24 | 3 | 4 | **0** | 1 | 1 | 0 | 1 |
| H1 | 8/24 | 8 | 8 | **0** | 2 | 4 | 1 | 1 |
| E5-1 | 7/24 | 6 | 6 | **0** | 3 | 4 | 0 | 0 |
| E5-2 | 7/24 | 6 | 6 | **0** | 3 | 4 | 0 | 0 |

**The ratchet rejects Episode 5's own line on this set.** `X1` fails against `S3` — Hit@4 4→3,
all@4 4→3, one question lost — and `E5-1` fails against `S3` on stale@4 4→6. The rule the series
publishes, applied to the new question set, calls the published steps a regression for time. That is
not an argument against the rule; it is the rule doing its job on a dimension it was never shown.

**The line is best exactly where the question wants the present.** `E5-1` answers 3 of 3
history-decoys — the questions that carry a history word but need current text — and 0 of 7
history. Every mechanism the series has built is a mechanism for suppressing the past, so the set
that asks about the past is the set where they subtract.

#### Confirmed on the 5090, 26 Oct: it is the mechanism, not the model

Run in `e59d5c9` across `nomic`, `qwen3-emb-8b` and `qwen3-emb-8b-qwen3-rr`, on an unshallowed
corpus that `revisions.py` verified. `E5-1`, with `stale@4` beside each row because the two move
together:

| tier | Hit@4 | stale@4 | history | h-decoy | as-of | what-changed | as-known |
|---|---|---|---|---|---|---|---|
| laptop, nomic | 7/24 | 6 | **0/7** | 3/3 | 4/5 | 0/5 | 0/4 |
| 5090, nomic | 10/24 | 7 | **0/7** | 3/3 | 4/5 | 2/5 | 1/4 |
| 5090, Qwen3-Emb-8B | 10/24 | 8 | **0/7** | 3/3 | **5/5** | 1/5 | 1/4 |
| 5090, + Qwen3-Reranker-8B | 11/24 | 10 | **0/7** | 3/3 | **5/5** | 2/5 | 1/4 |

**History is 1 of 7 on `S3` and 0 on `X1` for every model tested.** Identical from nomic to
Qwen3-Reranker-8B. The question the 5090 run existed to answer — *is this a small-model problem?* —
is answered no, and the episode's thesis survives it. Buying a bigger model is not a route past the
supersession redirect, because the redirect runs before the model ever sees the unit.

**And the better model makes this set worse.** `stale@4` on `E5-1` goes 6 → 7 → 8 → **10** as the
models improve, while `S3` sits at 4 throughout. A better retriever is better at finding the right
*address*, and on a time question the right address holds the wrong text. **On this question set
model quality is anti-correlated with answer quality** — which is the sharpest argument the series
has yet made for why a measurement has to match the question's own unit.

**The best configuration scores 5 of 5 on `as-of`, and every one is the artefact.** Counting only
hits with no stale passage in the same top four:

| | clean hits | artefact hits |
|---|---|---|
| 5090, nomic | 4 | 6 |
| 5090, Qwen3-Emb-8B | 3 | 7 |
| 5090, + Qwen3-Reranker-8B | 4 | 7 |

The four clean ones are **G-059** and **G-061** — history-decoys, which want current text — and
**G-067** and **G-068**, both `what-changed`. So of the 21 questions that genuinely ask about the
past, the best embedder and reranker available answer **two**. That number is the episode.

It also settles the tier question for Episode 6's own ratchet in the most convenient way possible:
every finding here is identical on the laptop and the 5090, so nothing in this episode depends on
which tier it is defined against.

#### G-057 is the spine of the episode

Its expected unit is `specs/004-npm-publish-workflow/design.html#D-005`, and three things about it
line up too neatly to pass over:

1. **The lifecycle filter never touches it.** D-005 is `lifecycle=current`; its supersession is
   prose inside the unit — `Status Superseded by D-006`.
2. **The supersession redirect takes it out.** `S3` ranks D-005 third. `X1` ranks D-006 third, in
   the same slot, and D-005 leaves the top 50. Everything downstream inherits that, which is why
   `X1`, `H1`, `E5-1` and `E5-2` all score zero on history while `S3` scores one.
3. **D-005 is that redirect's named motivating example in the published Episode 4 article** — "that
   last stale record is spec 004's decision D-005 … the mark sits in a status element inside the
   decision, not in the attribute Step 3 reads". Episode 4's hero fix and Episode 6's hardest miss
   are the same unit, by name, in print.

**Both redirect rules do it**, which kills the obvious fix before it is proposed: `X1b`, `E5-1b`,
`H1b` — keep the replaced unit behind its successor, Episode 4's first rule — are identical to their
base rows on every measure. That rule governs an *applied change*, and D-005 is a decision
superseded by a sibling decision, which it does not cover. So `d7a1be4` is not the culprit and
reverting it would buy nothing. The category `Lifecycle` still lacks is wider than *superseded
applied change*.

#### Four of the five as-of "hits" are not hits

On G-062 through G-065 — and on G-060 and G-074 — **the same address is both the expected answer and
a listed stale answer**. Hit@4 and stale@4 fire on one passage. The retriever finds the right place
and serves the wrong revision, and an address-level measure cannot tell those apart; `H1`'s 8 hits
come with 8 stales, one per hit.

So `as-of` and `as-known` are **not measurable at all** on today's scorer, and `E5-1`'s +5 over `S3`
is mostly this artefact. Nine questions carry a `cutoff` and five a `window`; for all fourteen the
ground truth lives in `versions` and `stale_versions`, which the scorer does not read. **A revision
axis is not an improvement to the measurement, it is the precondition for having one.**

#### The ground truth is recoverable from git — tested 26 Oct

Before designing anything to retrieve revisions, the prior question: can the answers be recovered
at all? If `versions` cannot be reconstructed, the 14 revision-dependent questions are unmeasurable
whatever the architecture. They can, and the test settles the ordering key as well.

**Every commit the question file names is reachable** — all **23 distinct** commits (29 mentions;
several questions share one), in the unshallowed clone. No gaps. The probe is
`tools/ratchet-proto/revisions.py` (`1ef9377`): no models, a second to run, non-zero exit if the
clone cannot answer.

**8 of the 9 cutoff questions recover their address as it stood at the cutoff, and all 8 differ
from what the retriever serves today.** Not one is accidentally still correct. The ninth is G-075,
whose page does not exist at its cutoff — which is the right answer, since G-075 is the
unanswerable-by-design row with an empty `expected`.

Spot-checked against the golden set's own `core`, and it matches word for word:

| | at the cutoff | now, which is what gets served | `core` says |
|---|---|---|---|
| G-062 | "a **16-file** tree" | "a **40-file** tree" | 16 files |
| G-065 | "**Under spectastic-vivid**, an artifact MUST present a sticky … header" | "**Every artifact** MUST present a sticky top header" | only vivid had it |
| G-072 | "D-005 · … Status **Accepted**" | *address does not exist* | Accepted |

So the revision axis is not only necessary, it is **sufficient**: everything these questions need is
in git, and `ls-tree`/`cat-file --batch` reaches it without materialising a worktree.

**First-parent position is the index key, and it is validated.** It reproduces the declared
`before → intermediate → after` order on all five `what-changed` questions — G-067 `[433, 494, 505,
521]`, G-068 `[306, 716]`, G-069 `[214, 268, 270, 277]`, G-070 `[2, 546, 698]`, G-071 `[570, 634]` —
including G-069, where the two intermediates **share a committer timestamp** and date alone cannot
order them.

#### Three gotchas, corrected against the actual repository

The research gotchas were right in general and two of them do not apply here, which is worth knowing
before building around them:

- **Gotcha 1 (`--before` stops traversal) does not bite spectastic.** The history has **0 merge
  commits** across all 722, so `--before` and `--until` return the same commit and it is the correct
  one. Still use the filtering form; just do not describe it as a bug this project hit.
- **Gotcha 2 (topological order is not unique) does not bite either**, for the same reason: a linear
  history makes first-parent position a total order, which is exactly why it works as a key.
- **Gotcha 7 sharpens.** Committer date is **monotonic** along first-parent — 0 backward steps —
  so it is a usable sort key; **author date is not**, going backwards once (`b88df5e8` → `9691dc25`).
  But committer date is **not injective**: 68 commits share a timestamp with another, in 16 groups.
  Monotonic but not unique is precisely the dangerous case gotcha 8 warns about, and G-069 is it.

**So: order by first-parent position, derive it once, and never sort by a date at query time.**

#### The revision axis exists, and it reprices everything — 26 Oct

`Revisions` is built in the prototype (`1096536`): the known states of an address, in order, each
with a point in time, with `GitHistory` as this corpus's adapter and position on the first-parent
chain as the key. `revisions.py --score` re-prices a run already on disk — **no retrieval changes
and nothing is re-run**, so the correction is arithmetic over existing rankings and cannot be
confused with a retrieval result. `history` and `history-decoy` keep address-level scoring, because
they ask about archived *documents* and the snapshot holds those under their own addresses.

| run · `E5-1` | Hit@4 | revision-aware | as-of | as-known |
|---|---|---|---|---|
| laptop, nomic | 7/24 | **3/24** | 4 → **0** | 0 |
| 5090, nomic | 10/24 | **5/24** | 4 → **0** | 1 → **0** |
| 5090, Qwen3-Emb-8B | 10/24 | **4/24** | 5 → **0** | 1 → **0** |
| 5090, + Qwen3-Reranker-8B | 11/24 | **5/24** | 5 → **0** | 1 → **0** |

**Every `as-of` and every `as-known` question scores zero, on every configuration, on every model.**
All nine cutoff questions, all three tiers, `S3` through `E5-2`. Qwen3's perfect 5 of 5 is 0 of 5.
This is the number that was hiding behind the address-level measure, and it is the episode's
headline: *the pipeline has never answered a single question about the past.*

**And the five survivors do not survive reading.** Of the best row's five, three are
history-decoys — G-059, G-060, G-061 — which want *current* text, so they are not questions about
the past at all. The other two are `what-changed`, G-067 and G-068, and the coverage check kills
them: a single snapshot supplies **1 of G-067's 4** declared versions and **1 of G-068's 2**, so a
reader gets one state and cannot see the change. G-069 is 0 of 4 — even its *after* state has moved
on since.

So of the **21 questions that genuinely ask about the past, the line answers none of them
completely**, on the best hardware and the best models available.

**Two independent methods agree, which is why I believe it.** Counting hits that carry no stale
passage in the same top four — a ranking-level test that knows nothing about git — gives G-059,
G-061, G-067, G-068. The revision-aware rescore gives those plus G-060. Same two `what-changed`
questions, same conclusion, from rankings rather than from revisions. The one disagreement is
instructive rather than worrying: G-060 is a decoy whose expected address is also listed stale, so
the ranking test cannot tell a stale *revision* from a stale *passage* and wrongly drops it, while
the revision test knows a decoy wants today's text.

#### The corpus decision, measured 26 Oct — and the premise was wrong by 411x

The plan has been carrying "555 revisions in one FTS5 table" as the reason a revision-aware corpus
is hard, implying 555 x 10,540 = **5.85 million** rows. That number is wrong, and so is the fear
built on it.

| | |
|---|---|
| commits touching `specs/` | 555 |
| **page**-revisions (page, commit) where a page was added or changed | **3,190** |
| spec pages that ever existed | 725 (627 now — **98 have been deleted or renamed away**) |
| **distinct unit-revisions, by text** | **14,225** |
| the current index | 10,540 |
| ratio | **1.3x** |

**89% of addresses never changed once** — 10,927 of 12,227 have exactly one text state. So the whole
history, at the granularity the search actually indexes, is a third larger than the snapshot already
indexed. Nothing here needs an index per revision, or a worktree, or a time-travelling query that
swaps text in after the fact. **One table of 14,225 states, each carrying a validity interval in
first-parent positions, is the whole corpus.** Those 98 vanished pages are also why G-072 is
answerable at all.

#### But gotcha 3 is real, and it bites exactly where the questions are

Having been wrong in one direction, worth not being wrong in the other. The collection does not
merely change, it **grew sevenfold**, and `idf` with it:

| cutoff | revision | units in force | avg words | median \|Δidf\| vs now | p95 |
|---|---|---|---|---|---|
| 2026-06-20 | `60cda7bc` p57 | 1,535 | 94.6 | **0.656** | 3.155 |
| 2026-07-01 | `5833e536` p158 | 2,762 | 118.0 | **0.460** | 2.180 |
| 2026-08-12 | `ddfa4961` p448 | 7,934 | 115.8 | 0.201 | 1.117 |
| 2026-08-15 | `8a4f6167` p551 | 9,118 | 124.4 | 0.082 | 0.408 |
| 2026-09-11 | `b632e936` p697 | 10,009 | 137.2 | 0.039 | 0.107 |
| now | `c64d41c` p721 | 10,678 | 134.7 | — | — |

Measured over every term of length > 3 in the golden set's own questions. At the earliest cutoffs the
collection is **14% of its current size** and the median term's `idf` is off by 0.66 with a p95 of
3.16 — against `idf` values that run about 1 to 8. That is not a rounding error, it is a different
ranking. And **4 of the 9 cutoffs sit in the severe region**: G-065 and G-066 in June, G-062 and
G-072 on 1 July.

So: scoring with whole-table statistics would be wrong, and wrong worst where the hardest questions
are. The fix is not a bigger index, it is **collection statistics taken at the cutoff** — which the
validity interval makes computable per query, since both `N` and `df`-at-cutoff fall out of the same
column that does the filtering.

**The architectural consequence, and it is the episode's real decision.** FTS5's own `bm25()` scores
against whole-table statistics and cannot be told to use a subset, so for an as-of query it is
unusable. Either BM25 moves into our own code over the filtered state set, or FTS5 is demoted to a
candidate generator and the scoring is ours. Episode 4 chose FTS5 precisely so that BM25 was not our
code to get wrong; time takes that choice back, and the article should say so plainly rather than
quietly reimplementing it.

#### Six ways to make FTS5 bitemporal, spiked and measured — 26 Oct

The corpus is built and validated: `ratchet/timecorpus.py` walks the first-parent chain and emits
one row per *state* of a unit with the half-open interval `[from_pos, to_pos)` it was in force over.
**15,616 unit-states against 11,492 in the snapshot — 1.36x.** Filtered to the last position it
reproduces `corpus.build()` exactly: 11,492 states, zero differences in any field. And **15 of 15
declared versions in the golden set land exactly on a state boundary**, G-069's tied pair at p268
and p270 included — separated by position where no date can separate them.

That required lifting the unit rule out of `corpus.build` into `corpus.page_units`, so a past
revision of a page is asked the same question as a present one. Proven inert: 11,492 units, 0
mismatching fields, pages, titles and edges all equal.

**Ground truth is S4** — an FTS5 index holding exactly the states in force at the cutoff, scored by
FTS5's own `bm25()`. Not an approximation of the right answer; the right answer.

| spike | rows | build | top-4 == truth | top-1 == truth | query |
|---|---|---|---|---|---|
| **S4** index per cutoff, exactly the live rows | 15,616 | **0.09 s** each | — *(truth)* | — | 63 ms |
| S1 one index, post-filter, FTS5 `bm25()` | 15,616 | 0.2 s | **4/9** | 6/9 | 12 ms |
| S3 partitioned into 8 epochs | **59,203** | 0.6 s | 3/9 | 8/9 | 7 ms |
| S6 current index + retired index, union | 15,616 | 0.2 s | 3/9 | 4/9 | 12 ms |
| S2 one index + `fts5vocab(instance)`, exact stats | 15,616 | 2.3 s | *withdrawn* | *withdrawn* | 5.1 s |
| S5 own postings, own BM25, exact stats | 15,616 | 0.5 s | *withdrawn* | *withdrawn* | 3.6 s |

**The headline: statistics change the answer.** S1 and S4 run the *identical* scorer and differ only
in which rows the statistics come from. They agree on the top 4 for **4 of 9** cutoff questions. So
scoring with whole-table statistics changes the top four on **five of the nine**, which settles that
this is a real problem and not a theoretical one.

**S2 and S5 are withdrawn, and the reason matters.** They implement BM25 ourselves with
cutoff-exact statistics, which is the interesting idea; they also disagree *with each other* on 7 of
9, and two independent implementations of one formula cannot both be right. The cause is the
tokenizer: FTS5's `unicode61` splits on `-` and the prototype's own word regex keeps it, so the two
count different tokens. **The approach is not disproven, my implementation of it is** — and that is
exactly why the harness compared them against each other rather than only against the truth.

**S6 is dead, and the research says why independently.** The union of a live index and a retired one
scores each row against its own table's statistics, and FTS5 scores from different tables are not
comparable — confirmed from the FTS5 source, and visible here as 3/9. This was the cheap option I
floated; it is measurably wrong, which is the answer to floating it.

**S3 costs 3.8x the rows** (59,203, because a state joins every epoch its interval overlaps) to be
*worse* than S1 on the top four.

#### The decision: materialise the slice, and keep FTS5's BM25

**S4 at 0.09 s per cutoff.** It is correct by construction, it uses FTS5's own scorer, and it needs
no reimplementation of BM25 — so Episode 4's reason for choosing FTS5 (that BM25 was not our code to
get wrong) survives intact. I was wrong two notes ago to recommend taking BM25 back into our own
code; the measurement says materialise the slice instead, and it costs nine hundredths of a second.

One hazard checked and cleared: FTS5 clamps `idf` to `1e-6` when `N < 2 x df`, which would make a
small slice rank arbitrarily. **It never fires here** — across all nine cutoffs the worst term
reaches 39% of `N`, and the clamp needs more than 50%. So the ground truth is sound at the smallest
slice too (1,565 units).

#### The shipped line already has this flaw, and it is measurably invisible

`KeywordSearch` is `fts5(heading_path, anchor, body, lifecycle unindexed)` and filters with
`and lifecycle in (...)` *after* `bm25()` has scored the whole table — so the published line already
computes `idf` over 196 units the filter is about to remove. I expected that to matter. **It does
not:** on the 47 answerable questions, indexing only the in-force units gives a top-1 identical on
**47/47** and a top-4 identical on **47/47**. Only the top 10 moves, on 14 questions.

So the flaw is real, 1.7% of rows wide, and changes nothing any published measure looks at. At the
earliest cutoff the same flaw is 87% of rows wide and changes the top four on five of nine
questions. **The magnitude is the whole story, and time is what changes the magnitude** — which is a
better argument for the episode than a bug would have been, because nothing published has to be
corrected.

#### The target schema, which is a two-column extension of the one we have

The existing table already carries a non-text property for filtering, so the bitemporal version is
the same pattern and not a new idea:

```sql
create virtual table units using fts5(
  heading_path, anchor, body,
  lifecycle  unindexed,   -- standing: is this in force *now*
  valid_from unindexed,   -- position this text became current
  valid_to   unindexed    -- position it stopped; LIVE sentinel while current
);
```

`lifecycle` is the first of the plan's two keys and `valid_from`/`valid_to` is the second, side by
side on one row. Four decisions, each from a source or a measurement rather than taste:

- **Half-open `[from, to)` with a sentinel, never NULL.** Every system that documents a convention
  uses half-open; SQL Server's sentinel is `9999-12-31 23:59:59.99`, MariaDB's is `2147483647.999999`.
  One predicate then serves every query and an index over it needs no coalescing.
- **Normalise the cutoffs at ingest.** The golden set states them as `23:59:59Z`, which in a
  half-open schema leaves a one-second hole per day. Store the next day's `00:00:00Z` and keep the
  "through 1 March" phrasing for the reader.
- **A dense integer position, not a timestamp.** No vendor documentation prescribes a tiebreaker for
  tied period bounds at all; the systems that cope avoid the clock entirely (Dolt keys on commit
  hash, MarkLogic on an ingestible watermark, and SQL Server's own docs fall back to
  `ROW_NUMBER()`). Our 68 tied commits need exactly this.
- **Identity is the address, and a rename is a *valid-time* change, not a correction.** No product
  examined offers any facility for a key that changes, so all of them key history on something
  immutable. Getting the rename backwards is the dangerous case because *"what is it called now"*
  keeps working while *"what was it called in March"* silently breaks — and 98 of our 725 pages have
  been renamed or deleted.

Two FTS5 facts worth carrying into the build, both verified against 3.53.4 rather than inferred:
`bm25()` cannot be scoped to a subset by any SQL-level means, so S4's materialisation is the only
route to correct statistics without a custom C extension; and `MATCH 'valid_from:1'` on an
`unindexed` column **parses and silently returns nothing**, so the query builder has to reject it.

#### What the literature says, and the one thing it has not done

Three research rounds, saved under `research_notes/Bitemporal FTS5 for time travel search/`.

**Nobody accepts global statistics.** The two published answers sit at opposite ends of the cost
curve. Berberich et al. (BTW 2007) *materialise* them: `N(t)`, `avdl(t)` and `df(v,t)` over only the
versions valid at `t`, recomputed collection-wide **at regular intervals** and scanned at query
time — which is S4 with a cache, so our choice is the literature's. Staudinger, Piroi & Rauber
(SIGIR-AP 2024) compute `df` at retrieval time instead, and put the case better than I did: with
global statistics, queries "are not even reproducible for the parts of the corpus that have **not**
changed."

**The experiment we ran does not appear to exist in the literature.** The research found no paper
measuring the ranking error from substituting global statistics for time-local ones. Berberich
measures the error from approximating *tf*; the 2024 paper argues the point qualitatively. So S1
against S4 — same scorer, same rows, statistics the only difference, top four changed on 5 of 9 — may
be a small original result. Claim it as "we could not find it", not as "it has never been done".

**And the argument gets stronger as a corpus gets smaller**, which is the opposite of what I
assumed. With small `df`, one document appearing or disappearing moves `log(N/df)` materially, so
global statistics are *less* defensible at 10⁴ documents than at 10⁷. Our 5-of-9 is what that
predicts.

**Every scale-dependent technique fails here, and the numbers say why.** The papers' corpora run
3.13 to 35 versions per document; ours is **1.35**. Temporal coalescing could save at most ~26% of
index size at a cost of Kendall's τ ≈ 0.8 and roughly 75 of the top 100 retained — a bad trade for a
1.36x index. Slicing costs a documented **2–3x** index size, and we measured 3.8x for a worse
answer. Anand et al. (SIGIR 2011) shard by docid instead at ~1% overhead, which beats tuned slicing
everywhere — and is still solving a problem we do not have.

**What does transfer, and it is the better implementation of S2.** An alive-at-`T` bitmap, Zoekt
style, is about **1.8 KB** at our size, and `df(v,T)` is then a popcount of the posting bitmap ANDed
with it — version-count-independent, and nothing like the per-query posting walk my S2 did. The 2024
paper's 2000 ms latency is an artefact of unindexed SQL aggregation rather than evidence that
`df`-at-`T` is intrinsically slow, which is the same diagnosis as my own 5.1 s. If S4's
materialisation ever stops being cheap enough, the bitmap is the next thing to build, not S2.

**One architectural detail worth taking even inside S4:** Berberich keeps the `tf` and `idf`
components **separately**, "in contrast to common practice… where only their product is kept",
because a length-normalised payload changes on every edit. Their measurement backs it — BM25
coalesces far worse than tf-idf at every error bound for exactly that reason. Keep raw `tf` in the
posting and normalise at query time.

**No shipped system does as-of-`T` ranked full-text over a git history.** Web archives sidestep it
with URL search plus timestamp-ordered version lists and stock Solr on global statistics; code
search does not index history at all — Zoekt carries a per-blob branch bitmask, Sourcegraph dedupes
blobs and caps at 64 branches per repository. Worth saying in the article, carefully: the reason is
plausibly that archive users "mostly do not search with any temporal restriction", which is a fact
about their users and not about whether the question is worth answering.

#### Retrieval over the slice: 0/9 to 6/9, and what each step bought — 26 Oct

With the bitemporal corpus in place, a slice is just a list of units, so Episode 4's `Pipeline` runs
on it unchanged. Scoring is revision-aware for free: the slice holds only the states in force, so an
address retrieved from it *is* the right revision. G-075 is counted as its abstention row — it has
an empty `expected` and no state at its cutoff, so 9/9 means eight answered and G-075 correctly
declined.

| step | hits | what moved |
|---|---|---|
| snapshot corpus, any model | **0/9** | the baseline the episode opens with |
| T1 BM25 over the slice | 4/9 | G-064, G-065, G-074, G-075 |
| T2 + identifier lookup | 5/9 | **G-072** at rank 1 |
| T3 + standing as of *then* | 5/9 | no change in rank, but see below |
| T4 + dense, RRF | 5/9 | G-062 reaches rank 7 |
| T5 + cross-encoder | **6/9** | **G-062** r1, **G-066** r4; and **G-074 lost**, 3 → 14 |

**G-072 is the identifier lookup's win, and it is the clean demonstration.** Its unit is
`004/plan.html#D-005`, at a path that does not exist in the current archive. In the slice it is
simply there, the lookup pins "D-005 in spec 004", and it comes first. No new mechanism — the
existing one, pointed at the right collection.

**T3 earns its place even though the number does not move.** G-073's unit, `095/tasks.html#T-1203`,
is `superseded` today and the standing filter removes it; at p551 no retirement had been recorded,
so the slice carries it as `current`. The snapshot corpus has one lifecycle per address and a state
has one per interval, which is the whole reason the question is answerable at all. It is reachable
from T3 onward and the ranking is what fails.

#### The three that remain all name a spec and name no id

G-063 says "spec 078", G-073 "spec 095's task list", G-074 "spec 105's design". The identifier
lookup documents its own refusal here — *"A spec with no id pins nothing"* — and it is right to
refuse, because pinning a whole spec above the ranking would bury a better answer elsewhere. But
refusing to *pin* is not a reason to refuse to *look*, and these questions have named their haystack
exactly while sharing almost no vocabulary with the unit that answers them.

Two ways to use that, both built, neither a filter:

- **The scope arm** admits the named spec's units to the rerank window, the same seam the sibling
  arm used. **Reach becomes perfect** — 91, 184 and 200 units admitted, the expected unit among them
  every time. Conversion does not: the cross-encoder puts G-063's unit 41st of 141 and pushes
  G-074's from 3rd to 32nd. Reach is a ceiling, not a forecast — the sibling arm's lesson, again.
- **Scope-RRF** gives the named spec *its own ranking* and fuses it as a third list beside BM25 and
  dense. Dense only, and deliberately: a cosine has no collection statistics, so restricting it to a
  subset is exact, where restricting BM25 would reproduce the very error this episode is about.
  Without any reranker it takes **G-063 from absent to rank 5** and **G-074 to rank 2**.

#### The reranker wins two and loses two, and fusing it wins nothing

| | G-062 | G-063 | G-066 | G-073 | G-074 |
|---|---|---|---|---|---|
| T7 scope-RRF, no reranker | 7 | **5** | absent | absent | **2** |
| T8 scope-RRF + reranker | **1** | absent | **4** | absent | 17 |
| T9 the reranker as an RRF vote | 2 | 8 | 8 | absent | 7 |

The union of T7 and T8 is eight of nine. So I tried the obvious reconciliation — the reranker as a
second opinion fused with the ranking rather than replacing it — and it is **worse than either**:
everything lands at rank 7 or 8, just outside the cut. Fusing two confident and opposed orderings
produces a ranking that commits to nothing. Worth keeping as a result: `rerank_rrf` is measured and
rejected, and the lesson is that a cross-encoder is useful precisely because it overrules, so making
it vote removes what it was for.

#### The named document, and the router: 6 of 9 to 8 of 9

**A question that names a document as well as a spec has narrowed its own haystack to a few dozen
units, and a few dozen is small enough to rank in.** "Spec 095's task list" is `tasks.html`; "spec
105's design" is `design.html`. Dense within the named document puts G-074's unit **1st of 17** and
G-063's **3rd of 34**. The document kinds come from the archive's own fixed set — `spec.html`,
`design.html`, `tasks.html`, `verify.html`, `plan.html`, `triage-log.html` — so naming them is
adapter knowledge, not a guess about language.

**The router resolves a conflict that fusion could not.** The two groups are disjoint *by
mechanism*:

| | names a spec | found by |
|---|---|---|
| G-063, G-073, G-074 | yes | the scoped ranking; **ruined** by the cross-encoder |
| G-062, G-066 | no — "the npm publishing spec", "the spec verb" | the cross-encoder, and nothing else |

So `route_scope` reranks only when the question named no haystack. That is not a tuned threshold,
it is a statement about evidence: a question that says where to look has given better evidence than
a reranker's reading of the whole archive, and a question that says nothing has given none.

| configuration | hits |
|---|---|
| snapshot corpus, any model | **0/9** |
| T5 slice + Episode 5's line | 6/9 |
| T7 + the named spec ranked and fused | 6/9 |
| **TD routed, the scope's own statistics** | **8/9** |
| TE the same, reading a bare spec as its `spec.html` | 8/9 |

#### A correction that gained the eighth question and retired a flag

I built the scope ranking **dense-only**, and wrote the reason into the docstring: a cosine has no
collection statistics, so restricting it to a subset is exact, while restricting BM25 would
reproduce the very error this episode is about. **That was wrong**, and wrong in a way worth keeping
in the article. It is true only of BM25 scored with *global* statistics over a filtered row set. An
index built over the scope has **the scope's own statistics**, which is correct for precisely the
reason S4 is correct for a cutoff: a subset is a collection, and a collection has its own document
frequencies. The same sentence twice, once about time and once about scope.

And the lexical half turns out to be the stronger one: spec-local BM25 puts G-063's unit **1st of
91** and G-074's **1st of 201**, where dense alone reached 7th and 2nd.

That also **retires `doc_scope_default`**, which existed only to buy G-063 by reading a bare "spec
078" as its `spec.html` — the one choice I had made with the answer in front of me. With the scope's
own statistics G-063 comes 4th without it, and `TE` now equals `TD`. The honest configuration and
the optimistic one have converged, so **8/9 is the honest number** and the flag is dead weight.

**A trap worth keeping.** A *hard* restriction to the named document would be worse than a boost,
and G-072 shows why: it asks about "spec 004's **design**", and in July 2026 that document was
called `plan.html`. Restricting by filename fails precisely where a rename matters. The identifier
lookup gets G-072 right because it pins on "D-005 in spec 004" and never looks at the filename.

#### G-073 is a negation failure, and that is a finding rather than a gap

Within its own spec, dense ranks `T-1203` **47th of 184**. What it prefers is
`T-1300: "Add screen to RESOURCE_KINDS"` — and the question asks about a task telling implementers
**not to add** a new resource kind. The top of the ranking is the semantic *opposite* of the answer.
Dense embeddings are known to be weak at negation, and nothing in this stack has any mechanism for
it; the decoy is additionally the very change that would later retire T-1203. A cross-encoder reads
the question and the passage together and ought to do better here, which makes the reranker the
lever for this question too, not a new arm.

#### What the 8 of 9 covers, because it is not 8 of 24

`run_timeline` scores the **nine questions carrying a single `cutoff`** — five `as-of`, four
`as-known`. That is the claim, and it should be written that way everywhere: eight of those nine
against zero on the snapshot, not eight of twenty-four.

| stratum | n | state |
|---|---|---|
| `as-of` + `as-known` | 9 | **8 answered**, G-073 outstanding |
| `history-decoy` | 3 | 3 answered — they want current text, which the snapshot already served |
| `history` | 7 | **0 answered.** Four reachable in principle, three born-withdrawn and beyond reach, and nothing picks the position because the question names no cutoff |
| `what-changed` | 5 | **unmeasured, not merely unanswered** — a `window` has a `from` and a `to`, so it needs *two* slices and a diff, and nothing is built for that shape |

So the episode's claim has a clean shape, and stating the shape is better than stating a bigger
number. **A bitemporal corpus answers the questions that name one point in time, almost
completely.** Questions that name **two** points need a mechanism that does not exist yet.
Questions that name **none** — the whole history stratum — need the cutoff to be *inferred*, which
is the trigger problem wearing a different hat, and the thing the next episode's work actually is.

#### Withdrawn is not superseded, and only one of them is a time problem

The nine cutoff questions were the ones a slice could obviously help. Asking the same question of
the seven **history** questions — *is the expected unit admissible at any position at all?* — splits
them in two, and the split is the useful part. (Positions here come from the ground truth, so this
is a **ceiling**: what a trigger could win if one existed, not a mechanism.)

| | now | admissible at | |
|---|---|---|---|
| G-054 | `rejected` | **p510–525 as current** | the lifecycle changed |
| G-055 (×4 units) | `superseded` | **p495–556 as current** | the lifecycle changed |
| G-056 | `superseded` | **p380–560 as current** | the lifecycle changed |
| G-057 | `current` | p380–LIVE | admissible now; the *redirect* drops it |
| G-051, G-052 | `withdrawn` | **never** | born withdrawn (the `#changelog` unit is reachable) |
| G-058 (×2 units) | `withdrawn` | **never** | born withdrawn |

**A withdrawn proposal was never anything else.** It is a proposal that was declined, so its units
carry `withdrawn` from their first state to their last, and there is no position at which the
standing filter would have admitted them. **Bitemporality cannot reach them** — not because the
corpus is missing anything, but because nothing about them ever changed.

Superseded and rejected are the opposite: the lifecycle *moved*, so a slice from before the move
admits the unit and the question becomes answerable. That is three of the seven history questions
reachable in principle by the same mechanism the cutoff questions use, plus G-057, which needs the
redirect taught about time rather than the filter.

So the plan's long-carried **lifecycle admission** arm finally has a precise scope: it is needed
only for **born-withdrawn** records, it is the only thing that can reach G-051, G-052 and G-058, and
it is *not* a time mechanism at all. Which also means the earlier framing — "five time questions
wholly unreachable because of the filter" — was conflating two different causes, and only one of
them was ever a bitemporal problem.

All five `what-changed` questions' expected units were `current` somewhere inside their window, so
none of them is blocked this way.

**And it quantifies cleanly over the whole archive.** Of 12,022 addresses that have ever existed:

| | addresses | |
|---|---|---|
| never excluded at any point | 11,843 | the filter never touches them |
| **excluded at some point, not all** | **97** | **a slice reaches these** |
| excluded in every state | **82** | no position admits them (81 withdrawn, 1 rejected) |

The snapshot's filter removes 196 units today — 96 superseded, 90 withdrawn, 10 rejected — and the
split above says what that costs. Most of the superseded ones were `current` earlier, so a
bitemporal corpus hands them back. The withdrawn ones were not, so it cannot. **The filter's 1.7%
is really two populations**, and the series has been describing them as one.

#### Fusing a reader's judgement throws it away — measured three times

The listwise reader can answer G-073; getting its answer *through the pipeline* turned out to be the
whole difficulty, and three attempts failed in the same way.

| shape | what happened |
|---|---|
| `T9` the reader as an RRF vote beside the ranking | everything lands 7th or 8th; **5/9**. Two confident, opposed orderings fused commit to nothing |
| `TG` the reader on the named scope, no open reranker | **5/9**. The scoped questions are fixed and G-062 and G-066 — which name no spec — lose their only mechanism |
| `TH` the reader on the scope, cross-encoder on the rest | **7/9**. G-073 moves 28th → **16th** and stops there, because global BM25 and dense outvote the reader two to one in the fusion |

`TH` is the instructive one: the reader *is* working and the RRF is undoing it. Which also settled
a design question the earlier runs only hinted at — **the two paths want different readers.** A
listwise reader discriminates where a cross-encoder cannot and costs about four and a half minutes
over fifty candidates against ninety seconds over twenty; a cross-encoder is cheap over fifty and is
the only thing that finds the questions naming no haystack at all. One reranker for both was the
wrong *shape*, not the wrong model, so `Pipeline` now takes two.

**And the fix is a seam the architecture already had.** An identifier hit is **pinned** above the
ranking and never reranked. The identifier lookup refuses to pin a whole spec, rightly — but the
reader's *selection* inside that spec is not a whole spec. A listwise call returns the few
identifiers it finds useful (three of twenty, in the probe), so the set is chosen by the reader
rather than by a `k` I picked. `scope_pin` pins exactly that.

| configuration | hits |
|---|---|
| snapshot corpus, every model and step | **0/9** |
| `TD` the router, which does not generalise | 8/9 |
| **`TJ` what the reader picks, pinned** | **9/9** |

`TJ` is `ids + lifecycle + dense + rerank + scope_rrf + doc_scope + scope_pin`, and it carries **no
`route_scope`** — the one arm that failed its generalisation test is gone from the answer. G-073's
T-1203 comes first of three pinned, G-063's FR-005 first of three, and G-075 still abstains with
nothing pinned at all, because it names no spec and so has no scope to read.

It is **9 of the nine single-`cutoff` questions, not 9 of 24** — the history stratum still needs its
cutoff inferred, and `what-changed` still needs two slices and a diff.

#### And `scope_pin` failed the main set too, for a reason worth the episode

30/47 against `E5-2`'s 33, losing G-031, G-032 and G-044. Worse than the router. But the diagnosis
is the opposite of what I expected, and it is the most useful thing the whole arc produced.

**The answer was inside the named scope every time.** Pinning was not wrong in principle. What was
wrong is *how much* got pinned: the reader selected **15, 20 and 19** of twenty candidates on those
three questions, against **3 of twenty** on the cutoff questions. Pinning a "selection" that
includes almost everything pins the whole scope — precisely what the identifier lookup refuses to
do, and for precisely the reason it refuses.

So the failure is the reader's **selectivity**, not the seam, and that gives a rule with a prior
justification rather than a fitted threshold: **a selection that includes most of what it read is an
enumeration, not a judgement.** It carries no discriminating information, so there is nothing to
pin. `scope_picks` now returns nothing unless the reader named a strict minority of its window.

That rule has to hold both ways to be worth anything — the cutoff questions must stay at 9/9 and the
main set must return to 33/47 — and it was written after seeing both failures, so it is exactly the
kind of claim this series is supposed to distrust until it is measured twice. **It was, and it
holds:**

| | cutoff questions | main set |
|---|---|---|
| `scope_pin` alone | 9/9 | 30/47 — **fails** |
| **`scope_pin` + a selection must be a minority** | **9/9** | **33/47, all@4 31, stale@4 0 — ratchet `(True, [])`** |

And not as a net wash: **lost none, won none**, identical on every stratum — identifier 8/8,
relation 6/7, stale 7/7, single-fact 7/10, paraphrase 3/10, intent 2/5. The arm is *silent* where it
is not needed rather than merely balanced.

**It fires on three of the nine.** G-063, G-073 and G-074 pin three units each; G-072 pins one, from
the identifier lookup rather than the reader; the other five pin nothing, and G-075 pins nothing
because it names no spec and so has no scope to read. A mechanism that touches three questions and
leaves forty-four alone is the opposite of a blunt instrument, which is what the ratchet is for
distinguishing.

#### The 5090 ran it, and the ratchet takes the 9 of 9 away

Three runs, verified from the committed JSONL rather than read off the message.

**(b) The headline reproduces.** With `qwen3:8b` reading the named scope, `TJ` is **9/9** on the
main-line tier, pinning at rank 1 on G-063, G-072, G-073 and G-074. Nothing about the result was
laptop-specific.

**(a) Qwen3-Reranker-8B nearly replaces the reader, and the trade is exact.** The `selects` guard
holds, so nothing is pinned and the reranker can only reorder — and reordering alone lifts G-073
from **rank 29 to 2**, which is the negation case that defeated three MS-MARCO cross-encoders. But
the same rerank sends G-074 from **1 to 5**. A scorer trades one question for the other and lands on
8/9. So an instruction-following *reranker* can see negation; it still cannot do what a *chooser*
does, which is leave the rest of the ranking alone.

**(c) And `scope_pin` fails the main-set ratchet on this tier.**

| | Hit@4 | all@4 | stale@4 | MRR |
|---|---|---|---|---|
| `E5-2` | 46/47 | 42 | 0 | 0.890 |
| `G2` scope arms, no pin | 46/47 | 42 | 0 | 0.890 |
| `G4` + pin | **45/47** | 42 | 0 | 0.873 |

`(False, ['Hit@4 46→45'])`. Lost G-036, won nothing. The arm pins on 15 of 53 questions and hurts
exactly two:

- G-033 `[1, 17] → [4, 19]`
- G-036 `[17, 4] → [16, 5]`

**Both are relation questions, and relation questions name a spec as the premise rather than the
haystack.** G-033 names 023 and its answers span 023 *and* 024; G-036 names 076 and its answers span
076 *and* 071. Pinning three units of the named spec fills the top four with premise material and
pushes the answer out. On the laptop's 33/47 baseline that was absorbed; at 46/47 there is no slack,
and **a pin on a strong baseline can only displace.**

I looked for a query-time rule that would separate the rescue from the misfire — pin only when the
ranking has not already reached the named scope — and **measured it dead**: "a unit of the named
scope is already in the top four" is true for *every* pinned question, G-073 included. There is no
signal there.

#### So the honest line is 7 of 9, and 9 of 9 is an epilogue

| | laptop | 5090 | main-set ratchet |
|---|---|---|---|
| snapshot, any model | 0/9 | 0/9 | — |
| `T3` slice + standing as of then | 5/9 | 5/9 | — |
| `T5` slice + Episode 5's own line | 6/9 | **7/9** | clean |
| `TF` + the scope arms | 6/9 | *to run* | **passes both tiers** |
| `TD` + the router | 8/9 | 8/9 | **fails**, laptop 33→31 |
| `TJ` + the pin | **9/9** | **9/9** | **fails**, 5090 46→45 |

The series' rule is absolute and it applies to my own work: a step that fails the ratchet is a
result, not a step. So **`route_scope` and `scope_pin` are epilogues** — published with their
numbers, as Episode 5 published a step 6 that failed — and the main line stops where the ratchet
says it stops.

Which leaves a better claim than the one I was chasing. **On the tier the main line is defined on,
the bitemporal corpus alone takes the cutoff questions from 0 of 9 to 7 of 9, with no new retrieval
mechanism whatsoever** — the identifier lookup, the standing filter, dense fusion and the
cross-encoder, all unchanged, pointed at the right collection. Everything after that buys one or two
questions and costs a main-set question somewhere. That is the episode, and it is a stronger result
for being smaller.

#### The section seam, closed: a why-question's collection is `#context`

G-080 was the 5090 main line's only miss and had **no structural signal at all** — no id, no named
spec, no typed edge between anything. Looking at where it actually lands showed the pipeline finding
the right *page* and the wrong *section*: rank 8 was `025-id-uniqueness/spec.html#scenarios`, and
the answer is that page's `#context`.

Which generalises, because the archive says so rather than because three questions do. **All five
of the main set's `intent` questions expect a `#context` unit** — 5 of 5 — and spectastic's schema
puts the problem a spec exists to solve in `#context`. A question asking *why* something exists is
asking for that section without naming it. This is the section seam the plan has carried since the
typed-expansion work: expansion anchors only on element ids, and a great deal of the meaning lives
in sections.

**A subset is a collection, for the fourth time this episode.** Over the 129 in-force `#context`
units the expected one ranks **first** for G-077, G-079, G-080 and G-081, and sixth for G-078.

Fused and never pinned, because the trigger is deliberately poor: a `why` shape fires on 19
questions across the three sets and only 4 want a `#context`, so it has to be a suggestion the
ranking can decline.

| main set, laptop | Hit@4 | all@4 | stale@4 | ratchet |
|---|---|---|---|---|
| `E5-2` | 33/47 | 31 | 0 | — |
| `G9` the earlier keepers | 33/47 | 31 | 0 | clean |
| **`GA` + `context_scope`** | **35/47** | **33** | 0 | **`(True, [])`** |

Won G-078 and G-081 from absent to rank 3, lost nothing. G-080 reaches 21 — reachable where it was
nowhere, and the fusion is diluting a list that had it *first*, so the 5090's stronger reranker is
the test that matters, G-080 being the only miss there.

#### A chooser can pick the kind, and its cost disqualifies it anyway

RRF cannot choose between 31 section kinds, and that is a structural limit rather than a tuning
one: each kind offers a confident rank-1 and there is nothing to discriminate with. **A reader
can.** Given the best two of every kind it put G-080's `#context` **first of 62**, against 9th under
the fused-per-kind design and 41st in one combined collection. No trigger at all, which was the
whole point of trying it.

Two things stop it being a step.

**It is 1 of 2.** G-058's `#risks` came 6th. So the mechanism is not the general answer to the
section seam either.

**And it costs an hour.** One listwise call per question over fifty candidates ran about **70
seconds**, so a 53-question configuration takes **just over an hour** — against roughly a minute for
the whole `E5-2` line. A 60x increase in the cost of a run, for at best one or two questions, is
disqualifying before any score is read. I killed the full run at 61 minutes rather than finish it,
and measured the one question that matters on its own.

Worth recording precisely because it is the opposite of the `#context` arm's economics: that one is
two small index builds and no model call at all, and it won two questions.

**Its ordering is usable and its choosing is not.** For G-080 the reader picked **27 of 62** — which
the selectivity rule accepts by a hair, and which would be a catastrophic pin, since `scope_pin`
lost the 5090 main set by pinning 15 to 20. So only the order joins the fusion.

#### And every other section kind is just as sharp, which is the trap

The same measurement over each kind, including the units the filter hides:

| collection | size | and the expected unit's rank |
|---|---|---|
| `#changelog` | 596 | G-051 **2**, G-052 **3**, G-058 9 |
| `#risks` | 415 | G-058 **1**, H-010 **2** |
| `#intent` | 156 | G-006 **1**, G-018 **1**, H-009 **2**, H-011 **1** |
| `#requirements` | 121 | G-031 **1**, G-032 **1**, H-004 **1** |

So five more questions are one trigger away. **Both trigger-free designs fail, and they fail in
opposite directions**, which is what makes the boundary real rather than a shrug:

- **One combined collection of all 4,926 section units** is much blunter — G-051 drops 2 → 13,
  G-052 3 → 14, G-080 1 → 41, G-078 6 → 97. Mixing `#changelog` with `#risks` and `#context`
  reintroduces exactly the competition that narrowing removed.
- **One fused list per kind** is worse still: there are **31 section kinds** with 40+ units, so 31
  lists each contribute a confident rank-1 item and the answer lands in a tie-tier around 9th or
  10th. G-051 goes 2 → 38, G-078 6 → 164.

The per-kind collection is sharp *and* needs to know which kind. For `#context` the mapping is the
archive's own schema and holds 5 of 5; for `#risks`, `#changelog` and `#intent` I would be inventing
a trigger from two or three examples each, which is the line between "the schema says so" and "these
questions say so". **So the arm ships for `#context` and stops there**, and the other four questions
are recorded as one trigger away rather than quietly fitted.

#### Just-in-time slicing is right here and is not the general answer

`Timeline.at(position)` builds an FTS5 table and an id index over the units in force at that
position, keeps it in a dict, and reuses it. Embeddings are **not** recomputed: the vector cache is
keyed by text, so a slice only gathers rows that already exist. Materialising one costs about
0.09 s.

**A slice buys two different things, and only one of them is expensive.**

1. **Membership** — which units existed and were in force at `T`. This is an interval predicate,
   `from <= T < to`, and it is cheap at any scale. It needs no materialisation at all.
2. **Collection statistics at `T`** — `N`, average length, and every `df`. This is the only reason
   we materialise, because `bm25()` cannot be told that a subset is the collection.

**And the dense half never had this problem.** A cosine has no collection statistics, so restricting
it to the rows live at `T` is exact with no index built. Only the lexical half has an as-of problem
— which is worth saying plainly, because it halves the architecture question.

**What materialising everything would cost here.** The archive has 722 commits, **478 positions
where some unit's text changes**, and 54 distinct days carrying a commit. Mean slice ≈ 6,184 units:

| | rows |
|---|---|
| one bitemporal table, every state once | **15,616** |
| all 54 day-slices materialised | **~334,000** — a **21x** blow-up |

The papers quote 2–3x for time-slicing, and ours is far worse for a specific reason: **slicing's
cost scales with the number of timepoints you materialise, not with how much changed.** At 1.36
versions per document, copying the unchanged 89% into 54 slices is nearly pure waste.

Which is exactly why just-in-time works: we only ever materialise the cutoffs actually asked about —
**6 of the 478**. It is *lazy* slicing, and it is cheap only while the workload has few distinct
cutoffs.

**Where it breaks, and what replaces it.** FTS5 build is roughly linear in tokens, so 0.09 s at
10,500 units is about 90 s at 10 million — no longer just-in-time by any reading. Memory is
`slices x index`, and a production workload's cutoffs are unbounded. The general design is the one
the research pointed at:

- **one index over all versions**, with the interval as columns for membership;
- **`df` at `T` computed rather than materialised** — an alive-at-`T` bitmap (about 1.8 KB at our
  size, and independent of version count) makes `df(term, T)` a popcount of the posting bitmap ANDed
  with it. Staudinger et al.'s 2,000 ms was unindexed SQL aggregation, not evidence that this is
  intrinsically slow;
- **dense untouched** — one index plus the interval filter, exact, forever.
- and the cost of all that is **BM25 in our own code**, which is precisely what Episode 4 chose
  FTS5 to avoid. That is the trade, stated plainly.

**One lever makes caching work far better than it looks.** The archive's own dates are day-granular,
so snapping a cutoff to a day reduces the distinct slices from 478 to **54** — and a real workload
asking "as of last Tuesday" hits the same slice every time. Berberich et al. do exactly this,
materialising statistics at regular intervals and accepting bounded error; snapping to a granularity
the data already has costs nothing at all.

**A gap in what I built:** `Timeline._slices` is an unbounded dict. Nine questions over six
positions never noticed. Anything real needs an LRU and a memory budget, and the episode should say
so rather than present a leak as a design.

#### So the episode closes at 9 of 9, and the configuration is `TJ`

`ids + lifecycle + dense + rerank + scope_rrf + doc_scope + scope_pin`, over a bitemporal corpus,
with a cross-encoder on the open ranking and a listwise reader on the window the question named. No
`route_scope`, no `doc_scope_default` — both of the arms I had flagged as possibly fitted were
measured and are **absent from the answer**, one because a better reading made it unnecessary and
one because it failed outright.

The arc, with each step forced by the previous failure rather than chosen:

1. The snapshot answers **0 of 9**, and a better model does not help — `stale@4` gets *worse* as the
   models grow.
2. A bitemporal corpus is 1.36x the snapshot, not 555x, and materialising a slice costs 0.09 s.
3. Over a slice, Episode 4's own pipeline reaches **6 of 9** with no new mechanism at all.
4. A question that names a spec has named its haystack, and a subset is a collection with its own
   statistics — **8 of 9**.
5. G-073 is a negation question, defeating lexical retrieval, dense retrieval and three
   cross-encoders. A pointwise judge saturates at 10/10; a listwise one chooses correctly.
6. Fusing a confident reader's judgement throws it away, three times over. Pinning its *selection*
   keeps it — **9 of 9** — and pinning its *enumeration* breaks everything else.

#### The router does not generalise, and the main set says so

The plan said `route_scope`'s rule needed validating on questions it was not built for, because it
was chosen while looking at nine. It was, and **it failed**. The main set, 47 answerable questions,
on top of `E5-2`:

| configuration | Hit@4 | all@4 | MRR | verdict |
|---|---|---|---|---|
| `E5-2`, the baseline | 33/47 | 31 | 0.674 | — |
| `G1` + the named spec ranked and fused | 33/47 | 31 | 0.675 | **inert** |
| `G2` + narrowed to the named document | 33/47 | 31 | 0.675 | **inert** |
| `G3` + reranking only when nothing was named | **31/47** | 30 | 0.654 | **fails the ratchet** |

So the two scope arms carry: harmless where they were not needed, worth two questions where they
were. The **router does not**, and loses two main-set questions outright. It was never a statement
about evidence after all — it was a workaround for the cross-encoder ruining three scoped rankings,
and a workaround generalises about as well as the weakness it works around.

That is the right way for this to come out. The arms that encode something true about the archive —
a question names its haystack; a subset is a collection with its own statistics — generalise. The
arm that encodes something true about *MiniLM* does not.

**And the two questions it loses are its own trigger, which rules out coincidence.** G-031 opens
"Spec 058's corpus-license rule…" and G-044 says "the verify view (spec 021)": a spec named, no id,
exactly the case `route_scope` switches the reranker off for. G-031 falls from 4th to 6th and G-044
from **1st to 13th**.

So the router's premise — *a question that names its haystack does not need a reranker* — is simply
**false in general**. The cross-encoder *helps* spec-naming questions on the main set and *hurts*
them on three time questions, and the rule generalised a property of three rows into a policy. Worth
saying in the article as the clearest small example of the series' own discipline working: the rule
was flagged as fitted when it was written, the test was named in advance, and it failed the test.

**And it makes the reader the question.** `route_scope` exists only to switch off a reranker that
hurts. A reader that does not hurt scoped rankings removes the need for it rather than leaving it
unvalidated, which is what `TF` — rerank everywhere, no router — is being measured for.

#### The negation probe, and why pointwise judging cannot do it

MS-MARCO cross-encoders are weak at negation, so the one mechanism left that addresses the actual
failure — rather than scaling the one that failed — is a model that can be *told* to watch for it.
`--reranker ollama:<model>` does that: a fixed instruction, one call per passage, a 0-10 relevance
score. It is a probe, not a candidate for the main line, and it **refuted its own hypothesis**.

G-073 went from 9th to 6th in its document, and the reason is not that the judge disagreed with the
answer. It is that **the judge gave 10 out of 10 to six different passages**, T-1203 among them, and
the "add screen to RESOURCE_KINDS" tasks too. Nothing was separated; the order came from the
tie-break.

That is a nameable failure mode rather than a disappointment: **pointwise relevance judging
saturates.** Every task in that document *is* about the question's topic, so an absolute score has
no way to express "this one and not that one". The negation distinction is inherently comparative,
and a scale that rates each candidate alone cannot carry it. On the two questions that were already
working the judge was harmless — G-063 2nd → 1st, G-074 1st → 1st — which confirms it is
non-discriminating rather than wrong.

The measured fix for saturation is a **listwise** judgement: one call, all candidates, and the model
must *choose* an order — the standard shape from the RankGPT line of work. **It works.** Over the
named document's own top twenty, G-073's answering unit goes from 9th to **1st**, where three
cross-encoders and the pointwise judge all failed, and G-063 and G-074 stay 1st.

So the sequence is worth writing down as it happened, because each step was diagnosed from the
previous one's failure rather than guessed: negation defeats lexical and dense retrieval → it
defeats three MS-MARCO cross-encoders, which the literature predicts → an instruction-following
judge can be *told* about negation, but a pointwise one saturates at 10/10 → so ask it to compare
instead, and it chooses correctly.

**One near-miss worth keeping.** Ollama defaults the context window to a few thousand tokens
regardless of what the model declares — qwen3:8b declares 40,960 and sets none in its modelfile —
and a prompt over the limit is **silently truncated**. A listwise call over fifty candidates runs to
about 7,500 tokens, so the tail of the candidate list would never have been read while the reply
still looked like a ranking. The twenty-candidate probe survived only because it fitted. `num_ctx`
is now explicit and the call checks `prompt_eval_count` against it and raises, because a ranking
over candidates the model never saw is worse than no ranking: it is indistinguishable from one.

**What is not yet settled** is whether this survives the pipeline's own window. The probe read
twenty candidates from the named document; the rerank window is fifty drawn from the whole archive,
and a listwise reader's judgement degrades as the list grows. So `TG` gives the reader the named
scope's top twenty and fuses afterwards, which matches the probe — measured against `TF`, which
reranks the full fifty.

**First result in: `TD` with the listwise reader is 8/9, the same as with MiniLM.** Which figures,
and is worth stating rather than hiding: `TD`'s router switches the reranker *off* for exactly the
questions that name a spec, so a better reader has almost nothing to do there. It confirms the
reader is not harmful and settles nothing else, because `TD` is not a shippable configuration — its
router loses two main-set questions. The configuration that matters is `TG`: no router, and the
reader given the named scope's own top twenty.

**And the cost says the same thing the quality argument does.** Measured on this laptop under
contention, the prompt rate is about 28 tokens a second, so a fifty-candidate listwise prompt — some
7,500 tokens — prefills in roughly **four and a half minutes**, against about ninety seconds for
twenty. A listwise reader is priced per candidate *per query*, which is a different cost shape from
a cross-encoder and worth saying plainly in the article: it is affordable over a window the question
itself narrowed, and not affordable over the whole ranking. The scope arm is what makes the reader
usable, not merely better.

#### The arms cannot move the main line, proven by running it

Every new path is behind a flag that defaults false, and the one edit to existing flow is an
extract-variable whose `else` branch is the old line verbatim. That is an argument, so it was also
measured: the main set, six configurations, the pipeline before the arms against the pipeline after.

| | S3 | X1 | X2 | H1 | E5-1 | E5-2 |
|---|---|---|---|---|---|---|
| before, Hit@4 | 32/47 | 32/47 | 33/47 | 36/47 | 32/47 | 33/47 |
| after, Hit@4 | 32/47 | 32/47 | 33/47 | 36/47 | 32/47 | 33/47 |

Identical on Hit@4, all-evidence@4, MRR to three decimals and stale@4, on every row. The episode's
new machinery is additive in fact and not merely by intention.

#### What this changes about the episode, and what is now done

The main line is not "typed expansion plus a time filter", and it never was. It is: **make the
corpus able to say "then", and almost everything else follows from mechanisms the series already
has.** All three steps below are built and measured as of 26 Oct.

1. ~~**Revision-aware scoring**~~ — done, and it needed no new scorer in the end. A slice holds only
   the states in force, so a retrieved address *is* the right revision and the measurement is
   correct by construction rather than by a correction. `revisions.py --score` remains for
   re-pricing runs made against a snapshot.
2. ~~**A corpus that holds revisions**~~ — `timecorpus`, 15,616 unit-states, 1.36x the snapshot, and
   the gotcha-3 fear was off by 411x. The architectural decision turned out to be **materialise the
   slice** at 0.09 s, which keeps FTS5's own BM25 and so keeps Episode 4's reason for choosing it.
3. ~~**The retrieval arms**~~ — and the striking thing is that the arms the earlier plan proposed
   (the trigger, lifecycle admission, the declared edges) were **not** what the time questions
   needed. What they needed was the identifier lookup pointed at the right collection, the standing
   filter evaluated *then*, and a scope the question had already named. **0/9 to 8/9**, with no new
   edge class and no new model.

**The single open question is G-073, and it is not a time question any more.** It is a negation
question that happens to have a cutoff. Everything bitemporal about it works: the slice carries
T-1203 as `current` because no retirement had been recorded, which is the whole reason it is
answerable. What fails is ranking a passage that says *leave it alone* above passages that say
*add it*, and that would fail identically on a snapshot.

So Episode 6's claim is bounded and clean: a bitemporal corpus answers eight of the nine questions
the series wrote to defeat a snapshot, and the ninth fails for a reason that has nothing to do with
time.

#### Reachability, settled before retrieval

Of 29 expected addresses: **18 reachable, 11 removed by the lifecycle filter, 1 absent from the
corpus**. Six questions are unreachable at any depth by any model *on the snapshot* — G-054
(rejected), G-055, G-056,
G-073 (superseded), G-058 (withdrawn), G-072 (page absent) — which confirms the five-from-lifecycle
estimate above and adds G-072 to it. G-051 and G-052 are permanently *partial*: one of two expected
addresses is withdrawn, so all-evidence@4 is out of reach while the filter stands. The retrieval
numbers match the prediction exactly: every one of those six scored `None` at every depth.

### Routing, and the trap the golden set already set

The 24 `router` annotations in `questions-time.jsonl` describe five routes — CURRENT, HISTORY,
AS_OF(cutoff), CHANGE(element, from, to), AS_KNOWN(cutoff) — and a marker-list router, and then
**predict its failures**: G-054 through G-058 misroute to CURRENT (5 of the 7 history questions),
and G-061 misroutes to HISTORY because the decoy carries `originally`. G-056 and G-061 are a
matched pair across the `original` / `originally` boundary, which is exact-token matching, not
stemming.

So run the naive router first and publish the misroutes. Skipping to something cleverer throws away
a result the question set was built to produce.

**Gap:** the "report" those annotations cite, with the full narrow and broad lists, is not in the
repo or the vault. The lists have to be reconstructed or rewritten before this arm can run.

### Ports, so none of this knows what a spec is

- **`Revisions`** — the known states of an address, in order, each with a point in time. Spectastic's
  adapter reads git and the changelog; another corpus reads `valid_from` or a version list. This is
  where the two keys live, and it is the new port the episode is really about.
- **`Links`** — typed edges, one hop. `Successors` today is forward-only with no order; it becomes a
  special case of `Links` + `Revisions`.
- **A bitemporal filter has no port at all today.** Standing (in force / left behind) and as-of
  (what was in force then) are different axes, and `StandingFilter` answers only the first.

`PostProcessors` is the seam an expansion step plugs into; `Lifecycle` needs the category Episode 5
named and did not add — *superseded applied change*.

### Gotchas, each one load-bearing

1. **`git rev-list -n1 --before=<date>` does not do what it looks like.** In testing it returned the
   *newest* commit on the branch. `--since`/`--before` stop traversal at the first commit outside
   the window rather than filtering; `--since-as-filter` is the one that filters.
2. **Topological order is not unique** — git's own docs give two valid orderings for one example —
   so it cannot be a stable index key. Use first-parent order or an explicit counter.
3. **`idf` is time-varying.** Collection statistics — document count, average length, df — must be
   those that held at the cutoff. Indexing 555 revisions in one FTS5 table makes BM25 wrong for
   every as-of query. This is an architectural constraint, not a detail.
4. **"As of" is ambiguous by construction.** SQL Server's `FOR SYSTEM_TIME AS OF` means *as known
   at*; Oracle's `AS OF PERIOD FOR` means *as valid at*. Our `as-of` / `as-known` split is clearer
   than the industry's — worth saying so in the article.
5. **Boundary inclusivity bites.** The golden set's cutoffs are `23:59:59Z`; state them as closed
   bounds rather than leaving it to be inferred. MariaDB manages to be half-open in `FROM...TO` and
   fully inclusive in `BETWEEN` within one product.
6. **Materialise revisions with `ls-tree -r | cat-file --batch`** (0.11 s, no disk), not worktrees
   (~61 MB each — 34 GB for 555 revisions).
7. **Author date is not committer date.** 62 of 722 commits differ, from rebases or amends; rebase
   and cherry-pick preserve author date and reset committer date, and squash stamps both.
8. **Hash the result set per run.** A deterministic-but-arbitrary tie-break is the dangerous case
   precisely because it is stable, and therefore invisible to regression tests.

### Questions, and what is unmeasured

`questions-time.jsonl` holds 24 (history 7, history-decoy 3, as-of 5, what-changed 5, as-known 4;
G-053 retired), and as of 26 Oct it has been run — see *The time questions, measured 26 Oct* above.
What is still unmeasured is narrower and harder: the 14 questions whose correctness is a revision
rather than an address. G-067 is the G-043 case on purpose; G-069's two intermediates sit at an
identical committer timestamp, `2026-07-12T01:39:22Z`, confirmed in the file. The fields are already
built for this: `cutoff`, `cutoff_ref`, `window`, `versions` (ordered
before/intermediate/after — the ordering ground truth), `stale_versions` (which includes the
*current* text as a wrong answer), `attr_checks`, `absent_checks`. **Nothing in the scorer reads any
of them.**

Held-out says where the value has to come from: Episode 4's line finds **0 of 8** unseen relation
questions, and the best 5090 row reaches 3 of 8. Of the 13, only 3 are `anchored` and 5 of the 8
relation questions carry a typed link — so anchored expansion can only reach a part of the gap, and
the plan should not pretend otherwise.

### Decisions outstanding

- ~~**Which tier the main line is defined on.**~~ **Decided 26 Oct: the main line leaves the
  laptop.** The series is about getting from rags to riches, not about what fits on one machine, so
  if the next level needs a bigger model then that is the finding and it gets published as one. The
  main line is defined on the 5090, with the hosted tier as the rung anyone can reach for money —
  which promotes *Later: local against remote* from a curiosity to the reproducibility story.
  `Episode5RatchetTest` keeps its laptop assertions as a second tier, because they are what a reader
  can re-run; they are no longer the definition. Cost is to be minimised at first, not optimised.
- **Whether the 24 time questions join the frozen set or are scored separately**, like held-out. My
  view: separately — the ratchet's denominator should not move mid-series.
- The 14 draft ordering questions, still needing a file and an id range.
- Round 4 of the audit (105 held-out candidates) is deferred; until it runs every held-out number
  is a floor.

### Stale numbers elsewhere in this plan, now corrected or flagged

The old Episode 6 row quoted X2 as "33/47, all-evidence@4 26 → 28" (keeping rule, pre-audit) and
"E5-2 reaches 41/41 on the shared questions" — **no run producing a 41/41 denominator could be
found**; treat it as unverified. The section "Episode 5: relationships — versions and typed links"
below is stale twice over: still numbered Episode 5, and its figures are pre-v2, /44-era numbers
from the keyword prototype. Episode 4's **published** prose still says an applied change "stays,
right behind its successor", which `d7a1be4` made untrue.

## The cut of 4, re-examined — 26 Oct

Where it came from: **Spring AI's advisor default.** Episode 3 defines Hit@4 as "the top four
chunks, *the advisor's default*", so the metric was set to measure what the reader is actually
handed — the same principle that settled the TREC disagreement, *"our scorer counts items, which is
what the reader is given"*. A sound principle attached to an inherited number.

### Hit@4 is insensitive, so the number survives

| set | @1 | @2 | @3 | **@4** | @5 | @6 | @8 |
|---|---|---|---|---|---|---|---|
| main, 5090 `G2` | 39 | 43 | 44 | **46** | 46 | 46 | 46 |
| held-out, `G9` | 5 | 6 | 6 | **6** | 6 | 6 | 6 |
| time cutoff+window, `TD` | 5 | 5 | 6 | **8** | 10 | 10 | 10 |

Pooled over 80 questions: **75% land in ranks 1–4 and 2% in 5–10.** 39 of the main set's 46 hits are
at rank **1**. The system is mostly right at the top or nowhere near, so on the main and held-out
sets @4 is indistinguishable from @8 and the choice carries no weight.

The one place it moves is the time set, +2 at @5 — **G-068 and G-071, both `what-changed`**, the
stratum already known to have a *metric mismatch* (the question asks for a diff, Hit@k measures a
passage). The sensitivity appears exactly where the metric is the wrong shape. And since I know
those two sit at rank 5, **moving to @5 to collect them would be fitting**, and the gain would be an
artefact of the mismatch rather than better retrieval.

The original budget argument is now weak — four passages is ~636 tokens (median 608, max 1,371), so
a fifth costs ~159 — but the insensitivity makes it moot. The number survives for a better reason
than the one it was chosen for.

### all-evidence@4 is a different matter, and it does *not* survive unexamined

It inherits the same 4 and should not, because **its difficulty scales with the question.** Hit@4
needs 1 of 4 slots; all-evidence@4 needs *every* expected passage inside the same 4.

| set | questions needing 1 passage | 2 | 3 | 4 |
|---|---|---|---|---|
| main | 39 | 7 | 1 (G-076) | — |
| **held-out** | 2 | **11** | — | — |
| time | 18 | 4 | — | **1 (G-055)** |

**Held-out is 85% compound**, which is why its all-evidence sits at 3 of 13 while Hit@4 is 6 — every
question there needs two of four slots filled correctly. And **G-055 needs all four**: all-evidence@4
for it demands a perfect, exclusive top four with nothing else in it.

Unlike Hit@4, it is sensitive:

| set | @2 | @3 | **@4** | @5 | @6 | @10 |
|---|---|---|---|---|---|---|
| main, 5090 `G2` | 37 | 39 | **42** | 43 | 44 | 44 |
| held-out, `G9` | 2 | 3 | **3** | 3 | 3 | 4 |
| time, `TD` | 5 | 6 | **8** | 10 | 10 | 10 |

**Six questions found all their evidence and the window could not hold it**: G-031 `[1, 6]`, G-033
`[1, 17]`, G-036 `[16, 4]`, G-076 `[5, 5, 1]`, H-004 `[1, 8]`, H-011 `[11, 1]`.

So all-evidence@4 conflates two different things:

1. **did retrieval find all the evidence** — a property of the search;
2. **does the reader's window hold it** — a property of the *system*, and not of retrieval at all.

### What to do about it, and what not to

Not to widen the cut. all-evidence@4 is honest *as a system measure*: it answers "can the reader
actually answer this", and with a four-passage window a four-passage question genuinely has no
slack. That is the truth about the system rather than a defect in the metric.

What is missing is its companion. **Report all-evidence@4 beside all-evidence@10**, so a window
failure is distinguishable from a retrieval failure. On the evidence above that is 6 questions
across the three sets — including 1 of 13 held-out, which is 8% of a set whose all-evidence number
is otherwise the series' worst-looking.

And it argues for something Episode 7 should decide rather than Episode 6 assume: **if compound
questions are the point, four passages may be the wrong window for the reader**, not the wrong
number for the metric. G-076 at `[5, 5, 1]` is a reader-window question wearing a retrieval
question's clothes.

## The 5090 closes it — 27 Oct

Three runs at `2953b08`, verified from the committed JSONL rather than read off the message.

### The main set is closed: 47/47

| | Hit@4 | all@4 | stale@4 | MRR | ratchet |
|---|---|---|---|---|---|
| `E5-2` | 46/47 | 42 | 0 | 0.890 | — |
| `G9` the earlier keepers | 46/47 | 42 | 0 | 0.890 | clean |
| **`GA` + `context_scope`** | **47/47** | **43** | 0 | **0.911** | **`(True, [])`** |

Won G-080, lost nothing. Its rank is **1**, and the top hit is `025-id-uniqueness/spec.html#context`
— the exact unit the `#context` collection ranked first on the laptop, converted by a stronger
reranker precisely as predicted. `intent` goes 4/5 → 5/5.

So the arm that closed the main set is two small index builds and **no model call at all**, triggered
by the archive's own schema.

### And `TF` answers G-073, which makes every rejected arm unnecessary

`T5` 8/14, `T7` 6/14, **`TF` 9/14** — and on the nine cutoff rows that is **8/9**, because **`TF`
gains G-073 at rank 2** and loses nothing.

That is the result worth the most. G-073 is the negation question that defeated lexical retrieval,
dense retrieval and three MS-MARCO cross-encoders on the laptop; it took a listwise chooser *and* a
pin to answer there, and the pin then failed this tier's main-set ratchet. On the main line it falls
to the **ratchet-clean** configuration by reordering alone.

**So the whole apparatus built around it is surplus.** `route_scope`, `scope_pin`, `history_pin`,
`reader_pin`, `rerank_rrf` and `section_scope` were each measured and rejected, and the questions
they were invented for are answered without them. What survives is: the bitemporal corpus, the scope
arms, `admit_history`, `history_keeps_superseded`, `context_scope` — and a reranker that can read.

### `what-changed`, and the cut of 4 again

With `--changed-only`: `T5` 1/5, **`TD` 2/5**. G-068 and G-071 were rank 5 on the laptop and reach
**rank 3** here. G-067 is 33rd; G-069 and G-070 stay outside 50. The corpus still holds **every**
declared state for all five.

And a third question lands on the boundary: **G-074 is rank 5** on this tier. With G-068 and G-071
having been rank 5 on the laptop, that is three of the series' remaining misses sitting exactly one
place outside the cut — which is the sharpest argument yet for *The cut of 4, re-examined* below,
and exactly why moving the cut to collect them would be fitting rather than measuring.

### Where the three sets finish

| set | result | missing |
|---|---|---|
| **main, 47 answerable** | **47/47** | — |
| time, 9 cutoff rows | **8/9** | G-074, at rank 5 |
| time, 5 `what-changed` | 2/5 | evidence complete, ranking short |
| time, 7 history | 1/7 | five reachable at 11–39; every fix costs a decoy |
| time, 3 decoys | 3/3 | — |
| held-out, 13 | 6/13 | not yet run here — the deferred decision |

## The overnight run on the 5090 — 27 Oct

From [[RAGs to Riches, 5090 Overnight Handover]]. Every number is from committed JSONL: `e51fd74` on
`episode-5`, and `96c2dd0`, `72d3305` and `f88cb7e` on `episode-4-prototype`. Draft:
[[RAGs to Riches with Spring AI, Episode 6, Links, History and Time]].

| set | before tonight | after | by |
|---|---|---|---|
| main, 47 | 47/47 | 47/47 | `GA` |
| time, 9 cutoff rows | 8/9 | 8/9 | `TF`; G-074 at 5 |
| time, 5 `what-changed` | 2/5 *located* | **1/5 located and told** by `TF` (2 by `TD`, which fails the main set) | told is 5/5 from the archive |
| time, 7 history | 1/7 (laptop) | **4/7** | `GA` over the snapshot: G-052, G-054, G-056, G-057 |
| time, 3 decoys | 3/3 | 3/3 | untouched at 1, 1, 1 |
| **time, 24** | 14/24 | **16/24** | |
| held-out, 13 | 6/13 | **7/13**, all@4 3 → 5, all@10 4 → 5, MRR 0.349 → 0.435. **Spent, once.** A floor | `GA` |

- **Pinned the model call** (`e51fd74`): the three commentators run on `OllamaChatOptions`, with
  seed 42, `top_p` 0.9, `top_k` 40 and `keep_alive` 30m. Verified through a proxy logging every
  `/api/chat` body. The unpinned app gave **three different answers in three runs** of one question,
  and the pinned one gave byte-identical answers and requests in two. `./gradlew check` is green.
  Provenance: ollama 0.40.11, qwen3:8b `500a1f067a9f…` (Q4_K_M) at `num_ctx` 16384.
- **`context_scope` does nothing on the time set** (`TK` = `TF` everywhere). **G-074 and G-073 are
  mutually exclusive**: reranking the named document puts G-074 at 5, and not reranking it loses
  G-073.
- **History on the 5090 is 4/7 with the decoys untouched.** `G7` (without the scope arms) reaches
  5/7 with G-051 first; the scope arms move it to 12th, and they close the main set, so the trade
  stands.
- **`what-changed`'s three misses find the right spec and rank the story of the change** (its
  `#changelog`, tasks or proposal) above the changed requirement. Some of those passages may
  answer the question; they are audit candidates, not judged.
- **Declined:** fitted triggers for `#changelog`, `#risks` and `#intent`. Showing one is inert
  would mean scoring the held-out set again.
- **Correction, and it's a decision for you.** `admit_history` ranks all of `EXCLUDED`, withdrawn
  included, so the shipped line *already* lets a history question see withdrawn records on the
  snapshot, and G-052 is answered by one. "No position admits them" is true of the slice only.
  G-058 is admitted and still outside 50: a ranking problem. The case is in
  `tools/ratchet-proto/results/5090-episode6-ceiling.md`.

## The Spring AI defaults audit — 27 Oct

Prompted by a question about Hit@4 and widened: the naive first attempt was never meant to stay, and
nobody had looked at what else it inherited. Three research rounds, all read from the
**`-sources.jar` for exactly 2.0.1 in this machine's own Gradle cache** — the bytes the project
compiles against — and cross-checked against the `v2.0.1` tag. Notes in
`research_notes/Spring AI defaults audit/`.

### Verified here, on this corpus and this machine

>[!warning] Corrected 27 Oct, measured on the 5090 through a logging proxy
>The **app** was not affected. `spring.ai.ollama.chat.num-ctx=16384` (in `application.properties`
>since 11 Oct) reaches every request, so the committed body was `{temperature, num_ctx: 16384}`, not
>`{temperature}`. The 4,096 trap below is real for an *unconfigured* call, which is what the
>prototype's listwise reader would have been without its explicit `num_ctx` and its
>`prompt_eval_count` check.

**Ollama gives us 10% of the model.** qwen3:8b declares **40,960** context, its modelfile sets
nothing, and Ollama loads it at **4,096** — measured on this laptop, not inferred. Spring AI never
sets `num_ctx`: it is `@Nullable` with no default and null options are filtered out of the request
body entirely. Spring's docs table says 2048 and current Ollama resolves `OLLAMA_CONTEXT_LENGTH=0`
from VRAM into 4096/32768/262144, so **both** numbers in circulation are wrong for us.

Does the answerer overflow it? **No, but it runs on half a window:**

| | tokens |
|---|---|
| largest persona (`answerer.md`) | 419 |
| four passages, worst measured | 1,371 |
| structured-output preamble (est.) | ~350 |
| the question (est.) | ~120 |
| **total** | **2,260 — 55% of 4,096**, 1,836 spare |

**The naive baseline could not see the archive's structure, and that is a defaults finding.** The
default `JsoupDocumentReader` takes `selector "body"` and returns `Element.text()`. On
`025-id-uniqueness/spec.html`: 16,797 chars on disk become 9,815 chars of text, **0 newlines
survive**, and of 20 element ids **none remains addressable**. So Episode 3's pipeline was handed a
single flattened string with the ids, headings, tables and lists stripped — the very structure
Episode 4's element units are built from. And since no newline survives, the splitter's `'\n'`
boundary **can never fire** on this content. The naive baseline was weak for a reason more specific
than "chunking is crude".

**The silent discard does not fire on us.** `minChunkLengthToEmbed = 5` drops any chunk of ≤5
characters with no log. Measured over all **2,147** naive chunks: **0 discarded**, and the shortest
chunk we produce is 59 characters. Episode 3's number is safe from it — worth stating precisely
because it was the first thing I went looking for.

### What the audit actually changes

*(Fixed 27 Oct in `e51fd74`: seed, `top_p`, `top_k` and `keep_alive` are now pinned. The body carried
`num_ctx` as well as `temperature`; see the correction above.)*

**We have no determinism, by construction.** Our request body carries exactly one option,
`{"temperature": 0.3}` — everything else is null and omitted, so Ollama's `seed: -1` (random),
`top_p 0.9` and `top_k 40` apply. And **`seed` is not in `ChatOptions` in 2.0.1 at all**; it is
reachable only through `OllamaChatOptions.builder()` or `spring.ai.ollama.chat.seed`. A series whose
discipline is *pin everything* has been running its answerer non-reproducibly for three episodes.
That is the finding, and it is not about a default being wrong so much as about one never being
chosen.

**"Accept all" does not.** `SIMILARITY_THRESHOLD_ACCEPT_ALL = 0.0` and the filter is
`score >= threshold`, while cosine runs [−1, 1] — so **negative-similarity documents are dropped**,
before `.limit(topK)`, so fewer than four results is normal. Confined to the answerer path
(`Archive`, `SpecCorpus`), which is Episodes 2–3; the Episode 4–6 numbers use our own `VectorSearch`.

**`SimpleVectorStore` cannot break a tie reproducibly.** The stream source is
`ConcurrentHashMap.values()` and `.sorted()` is stable, so an exact tie at the 4th/5th boundary is
resolved by hash-bucket order — not stable across runs or re-ingests. `dotProduct` accumulates in
`float`, which can manufacture ties at 768+ dims. Again answerer-path only, but it bears on Hit@4
wherever that path is measured.

**`truncate(false)` is better than I feared.** Reading `EmbedHandler`, with `truncate:false`
Ollama returns **HTTP 400** rather than truncating, so the embedding path converts invisible quality
loss into a loud mid-ingestion failure. It does not raise any limit, and
**`spring.ai.ollama.embedding.num-ctx` is dead configuration in 2.0.1** — the docs list ~30 option
rows that `OllamaEmbeddingProperties` does not bind, silently ignored by relaxed binding.

**Smaller things worth a line each.** `EmbeddingMath.norm()` returns the *squared* norm
(`dotProduct(v,v)`; the cosine compensates, a direct caller does not). `QuestionAnswerAdvisor`
defines no topK — the 4 is `SearchRequest.DEFAULT_TOP_K`, so Episode 3's "the advisor's default"
misattributes it. `RetrievalAugmentationAdvisor` defaults `allowEmptyContext = false`, replacing the
question with a canned refusal when nothing is retrieved — and `QuestionAnswerAdvisor` behaves
*oppositely*, which matters because abstention is a measured stratum. Its default joiner re-sorts
score-descending and de-duplicates **even with a single retriever**, clobbering any deliberate
ordering a custom `DocumentRetriever` produced. `DocumentPostProcessor` has **no** specified
ordering, so `@Order` on one does nothing. `BatchingStrategy` **throws** on an over-limit document
rather than splitting or dropping it, and its limit is 7372 not the javadoc's 7371.

### What to do, in order

1. **Pin the model call.** Set `seed`, `top_p`, `top_k`, `num_ctx` and `keep_alive` explicitly via
   `OllamaChatOptions`. This is the only item that invalidates something: answerer results to date
   are not reproducible, and the series says they should be.
2. **Set `num_ctx` deliberately** rather than inheriting a VRAM tier, and record `ollama -v` beside
   the model digest — the tiering is a property of the host, so the pinned digest is not enough.
3. **Say what the naive baseline actually was.** Episode 3 should name the reader's flattening, not
   just the 800-token chunks. It is a better story and it is the truth.
4. Leave Hit@4 at 4 (measured insensitive), and pair **all-evidence@4 with all-evidence@10** so a
   window failure reads differently from a retrieval failure. See *The cut of 4, re-examined*.

## Rules register

| Rule | Episode | How it compares with research and practice | Still to do |
|---|---|---|---|
| 1. Compute what can be computed, and let the model talk about the result. | 2 | Aligned: program-aided language models (PAL) hand computation to an interpreter; eval practice uses code-based checks for objective rules | none |
| 2. If code reads it, give it a type — ask for a shape, not prose. | 2 | Aligned, with a known cost: strict formats can hurt reasoning ("Let Me Speak Freely?", 2024); the fix is to put reasoning before the answer, or convert after. The caveat was added to Episode 2 on 20 Oct | Apply it in the judging episode (the judge's reasoning goes before its verdict). Possible rewrite: *Shape the output, not the thinking* |
| 3. Trust, but verify. Check model output against something the model didn't write. | 2 | Aligned: ALCE found even the best models lack complete citation support about half the time. Episode 3 found the gap: checks prove provenance, not relevance | Extend in the judging episode; the judge is checked against hand labels (Thomas et al.; Husain) |
| 4. Never let data lose its provenance. | 3 | Aligned: attribution and citation research; OWASP LLM08 recommends retrieval logs and source authentication | Carry into the finance corpus (source authentication) |
| 5. Know the answers before you ask. | 4 | Aligned with eval-driven development. Blind spot: practice starts from error analysis of real failures, and questions written from the passages lean towards keyword search (G-007) | Future golden sets include real readers' questions |
| 6. Test the test. | 4 | Strongly aligned: pooling bias in IR test collections (Buckley, Dimmick, Soboroff and Voorhees, 2007); blind assessment is standard at TREC | Apply to the automated judge |
| 7. Respect the seams. | 4 | Aligned: retrieval granularity matters (Dense X Retrieval, EMNLP 2024); structure-aware chunking; contextual retrieval | The no-seams case (PDFs, transcripts) in the finance corpus |
| 8. Answer in the right tense. (Renamed 20 Oct from "Keep the past; answer from the present".) | 4 | Aligned: outdated information reduces accuracy and leads to confident wrong answers (HoH, 2025); knowledge conflicts (OWASP LLM08) | The present is the default in Episode 4; Episode 6 answers about the past on purpose, and the time episode by date |

## Rules still to write

| Rule (working wording) | Episode | Why | Sources |
|---|---|---|---|
| Make the model beat keywords. | 5 | BM25 is a robust zero-shot baseline that dense retrieval often fails to beat; reranking wins at high computational cost. It's the Mac result | BEIR (NeurIPS 2021) |
| Know when you don't know. | 5 | Abstention: Alpha got 4 of 6 unanswerable questions wrong in Episode 3. Retrieval-augmented models both over-refuse and are badly calibrated. Qwen3-Reranker's top score separates all six unanswerable questions | "Do Retrieval Augmented Language Models Know When They Don't Know?" (AAAI) |
| A model you don't host can change under you. | local against remote | A hosted model is a moving dependency published under a fixed name, and a frozen golden set cannot freeze it. Pin the id, the version date and the run date; cache the vectors and keep them, because the only reproducible thing about a remote run is its output | To find: the providers' own model-versioning and deprecation notices; reproducibility in IR evaluation |
| Retrieved text is data, not instructions. | finance corpus | Indirect prompt injection; OWASP's top LLM risk; hidden instructions in retrieved documents are LLM08's own example | OWASP Top 10 for LLMs 2025 (LLM01, LLM08); Greshake et al., 2023 |
| Retrieve only what the reader may read. | finance corpus (and the RMS prototype) | Permission-aware retrieval is LLM08's first prevention | OWASP LLM08:2025 |

Considered and folded into existing rules: scoring retrieval apart from the answer (Rule 5's explanation), and checking the judge (Rule 6).

## Shared vocabulary

Use the industry term alongside ours the first time it appears.

| Ours | Industry | Where |
|---|---|---|
| Golden set | Eval set; test collection (IR) | Episodes 3, 4 |
| Expected addresses | Relevance judgments, qrels | Episode 3 |
| Hit@4 | Hit rate, success@4 | Episode 3 |
| All-evidence@4 | Full recall@4; the passages are supporting facts (multi-hop QA) | Episode 4 |
| Blind audit; passages nobody judged | Blind assessment; pooling bias (unjudged documents counted as non-relevant) | Episode 4 |
| The ratchet | Regression test or gate | Episode 4 |
| Held-out questions | Held-out test set | Episode 4 |
| Element units, heading path | Structure-aware chunking; contextual headers (contextual retrieval) | Episode 4 |
| Keyword search | Lexical or sparse retrieval | Episode 4 |
| Identifier lookup | Exact-match lookup, pinned | Episode 4 |
| Lifecycle filter | Metadata filtering | Episode 4 |
| Fitness functions on model output | Evals; guardrails in production | Episode 2 |
| Provenance | Attribution, citations | Episode 3 |
| Structured output | Structured output, constrained decoding | Episode 2 |
| "Not in the archive." | Abstention, selective answering | Episodes 3, 5 |
| Fusion, reranker | Hybrid search, reciprocal rank fusion, cross-encoder reranking | Episode 5 |
| History questions | Query routing, intent classification | Episode 6 |
| Local tier, remote tier | Self-hosted against managed (API) models | Local against remote |

## Changes to the plan below

- **The supersession redirect is back in Episode 4.** Before the audit it was worth +2; with the audit, it passes on stale@4 alone (1 → 0).
- **Embeddings (step 4) and the reranker (step 5) are no longer Episode 4 epilogues.** They're Episode 5, with the 5090 runs.
- **Episodes 5 and 6 swapped (20 Oct).** Embeddings come first; links and history move to Episode 6. The prototype's config names (E5-1, E5-2) still say Episode 5.
- **"Before the golden set is frozen" is done.** The held-out set has 13 questions, and all-evidence@k is in the Java harness (`episode-4`, 14e0e5c).
- **The ratchet is the series' method, not a numbered rule.** Rule 5 is now "Know the answers before you ask", and Rule 8 "Answer in the right tense".
- **The hosted tier leaves Episode 5 (23 Oct).** It becomes "local against remote", an episode about cost, latency, drift and what leaves the building, with correctness as the control. Bedrock first — Titan Text Embeddings, then Cohere Embed with Rerank — because that is the access that exists, and a one-provider set is what makes a two-step remote line possible at all.
- **Time moves into Episode 6 (24 Oct).** As-of, as-known and what-changed were a later episode; an "as of" question is a relation question in another dimension, and building the mechanism twice makes no sense. The later Time episode keeps whatever Episode 6 does not finish.
- **The supersession redirect drops what it replaced (23 Oct).** It used to keep an applied change behind its successor. G-043 is why it changed, and `stale@4` 1 → 0 on the 5090 is what it bought.


## The rule: the main line only gets better

Each episode's main line ends at least as good as the one before it, on every measure the golden set tracks. A step that makes things worse doesn't ship in the main line. It goes into the episode's epilogue with its numbers and the reason it lost. Readers still learn from it, and the archive the next episode builds on never regresses.

### How the rule is enforced

- **A ratchet test, run like Episode 3's fitness functions.** Each episode commits its retrieval-only results file. A `RatchetTest` re-runs the golden set without the chat model (only question embeddings) and compares against the previous episode's file.
- **What counts as worse:**
  - overall Hit@4 or all-evidence@4 drops;
  - any single stratum loses more than one question (one question is noise on strata of 8 to 10);
  - more stale elements appear in the context;
  - fewer unanswerable questions are correctly declined (measured on the full run).
- **Accept or reject whole steps; don't tune them.** Every step uses defaults fixed in advance, such as reciprocal rank fusion with k = 60 and the stock reranker. On 44 questions, tuning until the ratchet passes would only fit the golden set.
- **Held-out questions.** The 12 to 15 new relation and history questions written before the freeze aren't looked at during the build. They're scored once, at the end of each episode, to check that a gain isn't an artifact of the questions it was built against.
- Full runs with Beta and Alpha, and hand labels, happen once per episode, on the final main line.

### What that changes

| Was planned | Now |
|---|---|
| Every Episode 4 arm reported side by side | Main-line steps ship only if they pass the ratchet; the rest go to the epilogue |
| Supersession redirect in Episode 4 (research suggestion) | Moved to **Episode 5**, which becomes "relationships": versions and typed links |
| Episode 5's merge of graph neighbors into the ranking (planned default) | **Epilogue.** Measured worse than no graph: 30/44 against 32/44 |
| Graph-first as a filter or candidate set | **Epilogue.** Fragile when the starting element is wrong: 19/20 falls to 8/20 |
| Generator model swap | Episode 7 |

## Episode 4: element-level hybrid search

### What it fixes from Episode 3

- Search finds the right neighborhood but not the right passage: right spec in the top four for 39 of 44, but strict Hit@4 only 20%.
- Identifier questions hit 13%.
- 800-token chunks ignore element boundaries. In G-009, Beta quoted the tidy line from the plan instead of the run-on that held the result. (Brian's note: "the correct answer was ranked higher so seems selection needs adjustment.")
- Superseded text was presented as current three times.

### Main-line steps, in order, with the prototype's evidence

The prototype is keyword and graph only, in Python, with no embeddings. The golden set favors keyword search because its questions were written from their passages.

| Step | What it adds | Prototype result | Ratchet risk |
|---|---|---|---|
| 1. Element units + BM25 | One unit per element with an `id`, keyword search with weighted fields | Hit@4 20% → 64% | Low |
| 2. Identifier lookup | `FR-012` in spec 040 → exact row, pinned first. Case-insensitive spec matching ("Spec 058's") | Identifier 4/8 → 8/8; overall 73% | Low |
| 3. Lifecycle filter | Leave out withdrawn, superseded and rejected; keep applied history (G-006 and G-018 need it) | Stale in top four: 3 → 1 | Low |
| 4. Vectors fused with BM25 | nomic vectors, reciprocal rank fusion with k = 60. Identifier hits stay pinned above the fusion | Not measurable here | **Medium.** It should lift paraphrase (2/10) but may dilute the strata keyword search already wins. If it fails the ratchet, vectors go to the epilogue, which is itself a finding |
| 5. Reranker | A small cross-encoder (MiniLM-L6) running in process over the top 50. Pinned identifier hits are exempt | Not measurable here | **Medium**, same reason |
| 6. Beta sees rank and address | Numbered context with addresses, for G-009 | n/a (full run only) | Judged on the full run's labels |

Steps 4 and 5 are where the ratchet earns its keep. Readers see a step tried, measured and either kept or moved to the epilogue.

### Unchanged from the earlier plan

- **One SQLite file**, read with `sqlite-jdbc`:
  - an FTS5 table using `unicode61 tokenchars '-'`, so ids stay whole;
  - an indexed table for identifier lookup;
  - float32 vectors, searched by exact cosine in Java.
- **Our own fusion code.** Spring AI 2.0.1 has none, and its joiner re-sorts by score, so the retriever sets `Document.score` in rank order.
- **Units.** 10,540 from 627 files. Every golden address is a unit. Long units are split into parts, and each unit's text starts with its heading path.
- **Spring AI wiring:**
  - `RetrievalAugmentationAdvisor` with our own `DocumentRetriever` and a reranking `DocumentPostProcessor`;
  - retrieved documents read back from `rag_document_context`;
  - `.responseEntity()` so the entity and the metadata come back together;
  - a custom augmenter template that keeps Beta's persona.
- **QuoteResolver** is unchanged; quotations now trace to exact addresses.
- **The number-in-claim check** is recorded, not enforced.

### Measured, every step

- Hit@4, MRR and all-evidence@4/@10, per stratum.
- Stale elements in the context.
- Where the right unit ranked before and after the reranker.
- Context tokens and latency.

### Epilogues (candidates)

- Whichever of steps 4 and 5 fails the ratchet, with its numbers.
- The embedder swap to `qwen3-embedding:0.6b` joins the main line only if it passes.
- sqlite-vec as an alternative to the Java cosine scan.
- Query rewriting, shown once with a follow-up question.
- Lucene as the pure-Java alternative store.

## Episode 5: relationships — versions and typed links

### Starting point

Episode 4's main line, unchanged. Episode 5 adds a `links` table to the same SQLite file, built by the element reader. Each link records its type and how it was derived.

- **Versions:**
  - a proposal's `<spec-delta target>` points to the current element it changes;
  - an element marked `data-status="superseded"`, plus its link to the replacement;
  - a page marked `<spec-status value="superseded">`, plus its links across specs;
  - prose of the form "superseded by <a>".
- **Typed links:** `validates`, `defer-to`, `cites`, `target`.
- **Untyped:** about 2,100 same-page, 7,000 same-spec and 1,800 cross-spec links, recorded but not followed in the main line.

### Main-line steps

| Step | What it adds | Prototype result (on Episode 4's keyword prototype) |
|---|---|---|
| 1. Supersession redirect | When retrieval lands on an old version, put the current version first. For applied history, keep the archived copy behind it rather than replacing it | Hit@4 32 → 34; stale stratum 6/8 → 8/8; stale in top four 1 → 0. It also catches 004 D-005, superseded in place but not marked, which the lifecycle filter misses |
| 2. Anchored typed expansion | Start from the element the question names (identifier lookup, already in Episode 4). Follow typed links only, one hop, at most five neighbors, inserted right after the anchor, **after** the reranker so it can't push them back down | All-evidence@4 26 → 30 with step 1. G-029 and G-030 fixed through `validates`. Simulated wrong anchor: 17/20, against 19/20 when the anchor is right |

Spring AI wiring: step 2 is a `DocumentPostProcessor` that reads the anchor recorded by the retriever. Post-processors receive the original query, not a rewritten one, so the anchor travels in document metadata. Step 1 is the last post-processor.

### Honest scope

Typed links fix one kind of question: those where spectastic links the named element to the answer. That's two questions in the current set, so the held-out relation questions decide whether the result stands. Links across specs reach the right page, not the right element. G-033's relationship is only in prose. Paraphrase questions are untouched.

### Epilogues (already measured)

- Neighbors merged into the ranking with reciprocal rank fusion (k = 60, weight 0.5): 30/44, worse than no graph, because neighbors outranked the elements they came from.
- Graph-first as a candidate set: fine when the starting element is right, 8/20 when it's wrong.
- Graph-first as a spec filter: no gain even when right.
- Following all links instead of typed ones: the 7,000 same-spec links flood the results.
- Two hops as the default.
- The traversal-order experiment itself (before against after, with vectors and the reranker on). It's the comparison no published paper has run, so it makes a strong epilogue regardless of the result.

## Before the golden set is frozen

1. Remaining label checks: G-013, 015, 018, 024 and 044.
2. Sign off the 50 questions.
3. Add 12 to 15 held-out questions:
   - relation questions where typed links exist;
   - relation questions where links are only in prose;
   - history questions ("what did the withdrawn proposal want?").

   Episode 3's baseline is re-scored on them once, retrieval only, so every episode reports against the same set.
4. Add all-evidence@k to the harness, so Episodes 3 and 4 already report the number Episode 5 has to beat.

## Build order for Episode 4

1. Branch `episode-4` from `episode-3`.
2. Element reader and its tests. Check: about 10,540 units and every golden address present.
3. SQLite index, retrieval-only harness, the ratchet test, and steps 1 to 3.
4. Golden set frozen.
5. Steps 4 and 5, each accepted or moved to the epilogue by the ratchet.
6. Advisor wiring, step 6, a full run, labels, then the article.
