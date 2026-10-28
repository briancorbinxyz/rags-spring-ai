# Series Plan: structural map, unanswerable-set audit, episode structure

Source: `/Users/briancorbin/Documents/Brain/Library/03 Resources/RAGs to Riches, Series Plan.md`
2,001 lines, 23,640 words. Front matter: `stage: seedling`, `publish: false`.
Read in full on 27 Oct 2026. File not edited.

Word counts below are the words between a heading and the next heading (the heading line itself
excluded). Line numbers are given so passages can be found again without re-reading.

---

# 1. Structural map

## 1.1 Document-level framing

Immediately under the H1 there is a callout (lines 7–8) that governs how the whole file is to be
read:

> **Current state, 23 Oct 2026**
> The sections from "Current state" to "Changes to the plan below" are the record as of 23 Oct 2026, and they win over anything further down. Everything below them is the plan as written on 14 Oct.

So the file is in two halves. Lines 10–1861 are the live record (plus some newer plan). Lines
1864–2001 are a frozen 14 Oct plan kept for reference. The precedence rule is the single most
important structural fact about the document.

## 1.2 Heading list, in order, with word counts and kind

Legend for kind: **plan** = what will be done; **record** = what was done and measured;
**decision** = a choice made with its reasoning; **reference** = standing rules, vocabulary,
constraints. Many sections are mixed; where so, the dominant kind is first.

| # | Line | Heading | Words | Kind | What it contains |
|---|---|---|---|---|---|
| 1 | 5 | `# RAGs to Riches, Series Plan` | 43 | reference | Title plus the precedence callout above. |
| 2 | 10 | `## Current state` | 859 | record | Eight dense bullets: golden set v2 frozen (53 questions, 47 answerable); Episode 4's main line and its Java ratchet numbers; Episode 5's laptop tier failing the ratchet; Episode 5's 5090 tier passing at 44/47; G-043 cleared and `stale@4` 1 → 0; the hosted tier leaving Episode 5; audit round 3; the TREC cross-check limitation. This is the summary everything else is measured against. |
| 3 | 21 | `## Episode map (tentative numbering)` | 368 | plan + reference | One table. Rows for Episodes 4, 5, 6 and five `later` rows (local against remote; Time, continued; Judging answers; Generator swap; Finance corpus), each with subject, main line and rules. No rows for Episodes 1–3. |
| 4 | 34 | `## Later: local against remote` | 610 | plan + decision | The hosted-tier episode, moved out of Episode 5 on 23 Oct. Bedrock lineup (Titan, Cohere Embed v4 + Rerank), the five dimensions (cost, latency, drift, failure, what leaves the building), same rules as local runs, Spring AI wiring, prototype work. |
| 5 | 56 | `## Episode 6: links, history and time` | 46 | plan | Two-sentence preamble: written 24 Oct, time pulled forward by decision. |
| 6 | 62 | `### The thesis, in one line` | 90 | reference | The episode's thesis: an archive needs a *filter* key and an *order* key, and the order key is a sequence not a time. |
| 7 | 72 | `### Why this is not a new idea, and why it is not in the literature either` | 102 | reference | Datomic, XTDB, Launchpad/Bazaar, Snodgrass and Dyreson. No canon source prescribes a tie-break for equal timestamps. |
| 8 | 82 | `### What the corpus actually gives us (measured 24 Oct)` | 229 | record | Day-granular dates; 22 specs with 2+ applied changes, 11 with same-day collisions; three clocks; 722 commits, 555 touching `specs/`, 25 tags; 29 referenced commits reachable; 68 commits sharing a committer timestamp. |
| 9 | 102 | `### The arms` | 311 | plan + record | Table of four coded arms (`X2`, `H2`, `E5-2`, `EpiB`) with 5090 numbers, flagged "stale twice over"; then six new arms proposed (ordering resolution, as-of retrieval, routing, declared-edge extraction, sibling edge, the trigger). |
| 10 | 133 | `### The result that is already in hand, and it is the episode's spine` | 168 | record + reference | `E5-2` +2 on every tier, `EpiB` −8; the "reserve slots after the anchor vs merge into the ranking" distinction, stated as a block quote. |
| 11 | 151 | `### What the meta declares, and what it does not` | 265 | record | Which spectastic relations are declared MUSTs and which are only convention; the 3,280 cross-artifact references and the 300:1 asymmetry; `verify.html`'s 1,130 links against 3 bare references; there is no DTD/XSD. |
| 12 | 179 | `### Arm 8: extract what is already declared` | 127 | record + plan | Table of ~1,500 declared-but-unextracted edges; `principles.html` absent from the corpus. |
| 13 | 197 | `### Arm 9: the sibling-artifact edge, and the trap around it` | 696 | record + decision | Reachability table (2/8 to 8/8); the recurring-anchor-id edge dead on arrival; `EpiB`'s 36/47 against `E5-1`'s 44/47; the 5090 run showing `E6-1`/`E6-2` identical to `E5-1`/`E5-2`; the "place, not merely nominate" lesson. |
| 14 | 263 | `### The main line: typed expansion, measured twice` | 335 | record | The four-row table (main set and held-out, `E5-1` vs `E5-2`), what expansion is, why it cannot dilute (fires on 10 of 53), what it moved (G-029, G-030). |
| 15 | 295 | `### Structured query rewriting: researched 25 Oct, and not worth building` | 416 | decision + reference | The idea and its rejection: ceiling 0 of 3, the circularity objection, the literature's negative results, the policy table, the variance argument. |
| 16 | 333 | `` ### `defer-to` is discarded, and the seam it exposes `` | 244 | record + decision | All 166 `defer-to` edges are unit→page and anchor on sections; the fix is kept though it changes nothing; the section seam is named. |
| 17 | 360 | `### What survives, in three families` | 296 | record | Lifecycle admission / defer-to resolution / prose-only table; `EXCLUDED` removes 196 of 11,492 units; the two findings that cut against each other; the precision constraint that lifecycle must be a rejectable hypothesis. |
| 18 | 388 | `### An incidental finding` | 98 | record | G-072's `plan.html` renamed to `design.html`; the earlier "unreachable" claim struck through and corrected. |
| 19 | 398 | `### The time questions, measured 26 Oct, and the line scores zero on history` | 323 | record | The six-configuration table over 24 time questions; the ratchet rejecting Episode 5's own line on this set; the line is best where the question wants the present. |
| 20 | 423 | `#### Confirmed on the 5090, 26 Oct: it is the mechanism, not the model` | 415 | record | Four-tier table; history 0/7 for every model; `stale@4` getting worse as models improve; clean vs artefact hits. |
| 21 | 464 | `#### G-057 is the spine of the episode` | 227 | record | Three facts about G-057, including that D-005 is Episode 4's published motivating example; both redirect rules drop it. |
| 22 | 486 | `#### Four of the five as-of "hits" are not hits` | 124 | record | The same address is both expected and listed-stale on six questions; `as-of` and `as-known` are not measurable on today's scorer. |
| 23 | 498 | `#### The ground truth is recoverable from git — tested 26 Oct` | 321 | record | All 23 distinct commits reachable; 8 of 9 cutoff questions recover a different address; the G-062/G-065/G-072 spot-check table; first-parent position validated on all five `what-changed` questions. |
| 24 | 531 | `#### Three gotchas, corrected against the actual repository` | 188 | record + reference | Gotchas 1 and 2 do not bite (0 merge commits); gotcha 7 sharpens (committer date monotonic, not injective). |
| 25 | 548 | `#### The revision axis exists, and it reprices everything — 26 Oct` | 445 | record | `Revisions` built; the re-pricing table; every `as-of` and `as-known` question scores zero on every configuration; two independent methods agree. |
| 26 | 587 | `#### The corpus decision, measured 26 Oct — and the premise was wrong by 411x` | 188 | record + decision | The 5.85 million figure was wrong; 14,225 distinct unit-revisions, 1.3x the current index; 89% of addresses never changed. |
| 27 | 609 | `#### But gotcha 3 is real, and it bites exactly where the questions are` | 358 | record + decision | The idf drift table over six cutoffs; 4 of 9 cutoffs in the severe region; FTS5's `bm25()` cannot be scoped, so either BM25 moves in-house or FTS5 is demoted. |
| 28 | 641 | `#### Six ways to make FTS5 bitemporal, spiked and measured — 26 Oct` | 552 | record | `timecorpus` built (15,616 states, 1.36x); the six-spike table with S4 as ground truth; statistics change the top four on 5 of 9; S2/S5 withdrawn over a tokenizer disagreement; S6 and S3 dead. |
| 29 | 686 | `#### The decision: materialise the slice, and keep FTS5's BM25` | 132 | decision | S4 at 0.09 s per cutoff; an explicit reversal of the author's earlier recommendation; the idf clamp checked and cleared. |
| 30 | 698 | `#### The shipped line already has this flaw, and it is measurably invisible` | 150 | record | The published line computes idf over 196 units it then filters; identical top-1 and top-4 on 47/47; the magnitude is the story. |
| 31 | 712 | `#### The target schema, which is a two-column extension of the one we have` | 360 | decision + reference | The SQL schema; four decisions (half-open with sentinel, normalise cutoffs at ingest, dense integer position, identity is the address); two verified FTS5 facts. |
| 32 | 750 | `#### What the literature says, and the one thing it has not done` | 589 | reference | Berberich et al. 2007 and Staudinger et al. 2024; the possibly-original S1-vs-S4 result; the argument strengthens as a corpus shrinks; scale-dependent techniques fail here; the alive-at-T bitmap; no shipped system does as-of ranked full-text over git. |
| 33 | 800 | `#### Retrieval over the slice: 0/9 to 6/9, and what each step bought — 26 Oct` | 305 | record | The T1–T5 step table; G-072 as the identifier lookup's win; T3 earning its place; G-075 counted as the abstention row. |
| 34 | 828 | `#### The three that remain all name a spec and name no id` | 225 | record + decision | G-063, G-073, G-074; the scope arm (perfect reach, poor conversion) and scope-RRF. |
| 35 | 847 | `#### The reranker wins two and loses two, and fusing it wins nothing` | 158 | record | T7/T8/T9 per-question rank table; `rerank_rrf` measured and rejected. |
| 36 | 862 | `#### The named document, and the router: 6 of 9 to 8 of 9` | 258 | record + decision | Dense within the named document; the router's disjoint-by-mechanism table; the configuration table to 8/9. |
| 37 | 891 | `#### A correction that gained the eighth question and retired a flag` | 280 | record + decision | The dense-only scope ranking was wrong; a subset is a collection with its own statistics; `doc_scope_default` retired; the rename trap. |
| 38 | 914 | `#### G-073 is a negation failure, and that is a finding rather than a gap` | 108 | record | T-1203 ranks 47th of 184 within its own spec; the top of the ranking is the semantic opposite. |
| 39 | 924 | `#### What the 8 of 9 covers, because it is not 8 of 24` | 219 | reference + record | The per-stratum state table; the claim's shape (one point in time, two points, none). |
| 40 | 943 | `#### Withdrawn is not superseded, and only one of them is a time problem` | 496 | record + decision | The per-question admissibility table; born-withdrawn records are beyond bitemporality; the 12,022-address quantification; the filter's 1.7% is two populations. |
| 41 | 991 | `#### Fusing a reader's judgement throws it away — measured three times` | 438 | record + decision | T9, TG, TH; the two paths want different readers; `scope_pin` and the 9/9 table. |
| 42 | 1029 | `` #### And `scope_pin` failed the main set too, for a reason worth the episode `` | 383 | record + decision | 30/47 against 33; the reader selected 15, 20, 19 of twenty; the selectivity rule and its two-way validation. |
| 43 | 1065 | `#### The 5090 ran it, and the ratchet takes the 9 of 9 away` | 368 | record | Three runs: TJ reproduces at 9/9; Qwen3-Reranker-8B trades G-073 for G-074 at 8/9; `scope_pin` fails the main-set ratchet 46 → 45; the query-time rule measured dead. |
| 44 | 1105 | `#### So the honest line is 7 of 9, and 9 of 9 is an epilogue` | 239 | decision | The laptop/5090/ratchet table; `route_scope` and `scope_pin` become epilogues; the smaller, stronger claim. |
| 45 | 1128 | `` #### The section seam, closed: a why-question's collection is `#context` `` | 310 | record + decision | G-080's diagnosis; all five `intent` questions expect a `#context` unit; the `GA` table taking the laptop to 35/47; fused and never pinned. |
| 46 | 1159 | `#### A chooser can pick the kind, and its cost disqualifies it anyway` | 265 | record + decision | A reader puts G-080's `#context` first of 62; it is 1 of 2 and costs an hour (70 s per question, 53 questions); its ordering is usable and its choosing is not. |
| 47 | 1185 | `#### And every other section kind is just as sharp, which is the trap` | 270 | record + decision | Per-kind rank table (`#changelog`, `#risks`, `#intent`, `#requirements`); both trigger-free designs fail in opposite directions; the arm ships for `#context` only. |
| 48 | 1212 | `#### Just-in-time slicing is right here and is not the general answer` | 593 | decision + reference | What a slice buys (membership vs statistics); dense never had the problem; the 21x blow-up if everything were materialised; the general design; day-snapping 478 → 54; the unbounded-dict gap. |
| 49 | 1270 | `` #### So the episode closes at 9 of 9, and the configuration is `TJ` `` | 220 | record + decision | `TJ`'s definition and the six-step arc, each step forced by the previous failure. |
| 50 | 1291 | `#### The router does not generalise, and the main set says so` | 415 | record + decision | `G1`/`G2`/`G3` main-set table; `G3` fails at 31/47; the two lost questions are the router's own trigger; the premise is false in general. |
| 51 | 1328 | `#### The negation probe, and why pointwise judging cannot do it` | 711 | record + reference | Pointwise judging saturates (10/10 to six passages); listwise works (9th → 1st); the diagnosis sequence; the Ollama silent-truncation near-miss; what is unsettled; the cost shape. |
| 52 | 1387 | `#### The arms cannot move the main line, proven by running it` | 124 | record | Before/after Hit@4 across six configurations, identical on every measure. |
| 53 | 1401 | `#### What this changes about the episode, and what is now done` | 329 | decision + record | The main line restated; three struck-through done items; the single open question is G-073, which is not a time question. |
| 54 | 1430 | `#### Reachability, settled before retrieval` | 94 | record | 29 expected addresses: 18 reachable, 11 removed by the filter, 1 absent; six questions unreachable on the snapshot. |
| 55 | 1440 | `### Routing, and the trap the golden set already set` | 127 | plan + reference | The 24 `router` annotations, the five routes, the predicted misroutes; run the naive router first; a gap — the cited report does not exist. |
| 56 | 1455 | `### Ports, so none of this knows what a spec is` | 129 | reference | `Revisions`, `Links`, and the observation that a bitemporal filter has no port today. |
| 57 | 1468 | `### Gotchas, each one load-bearing` | 276 | reference | Eight numbered gotchas (git `--before`, topological order, time-varying idf, "as of" ambiguity, boundary inclusivity, materialising with `ls-tree`, author vs committer date, hash the result set). |
| 58 | 1491 | `### Questions, and what is unmeasured` | 177 | record + plan | `questions-time.jsonl` composition; the 14 revision-dependent questions; the fields the scorer does not read; held-out numbers. |
| 59 | 1508 | `### Decisions outstanding` | 181 | decision | Tier decided (the main line leaves the laptop); whether the 24 time questions join the frozen set; the 14 draft ordering questions; audit round 4 deferred. |
| 60 | 1523 | `### Stale numbers elsewhere in this plan, now corrected or flagged` | 86 | reference | Names the stale X2 and 41/41 figures, the stale "Episode 5: relationships" section, and Episode 4's untrue published sentence. |
| 61 | 1532 | `## The cut of 4, re-examined — 26 Oct` | 61 | record | Where the 4 came from — Spring AI's advisor default — and the principle attached to it. |
| 62 | 1539 | `### Hit@4 is insensitive, so the number survives` | 251 | record + decision | The @1–@8 sweep; 75% in ranks 1–4; moving to @5 would be fitting; the token-budget argument is now weak but moot. |
| 63 | 1561 | `### all-evidence@4 is a different matter, and it does *not* survive unexamined` | 268 | record | Compound-question distribution per set; held-out is 85% compound; the sensitivity sweep; six questions found all their evidence and the window could not hold it; the metric conflates two things. |
| 64 | 1592 | `### What to do about it, and what not to` | 149 | decision | Do not widen the cut; report all-evidence@4 beside all-evidence@10; Episode 7 should decide the reader's window. |
| 65 | 1608 | `## The 5090 closes it — 27 Oct` | 15 | record | One line: three runs at `2953b08`, verified from committed JSONL. |
| 66 | 1612 | `### The main set is closed: 47/47` | 122 | record | `E5-2` 46/47 → `G9` 46/47 → `GA` 47/47; G-080 won at rank 1; `intent` 4/5 → 5/5. |
| 67 | 1627 | `` ### And `TF` answers G-073, which makes every rejected arm unnecessary `` | 135 | record + decision | `TF` 9/14 and 8/9 on cutoff rows; the whole apparatus built around G-073 is surplus; the list of what survives. |
| 68 | 1642 | `` ### `what-changed`, and the cut of 4 again `` | 109 | record | `T5` 1/5, `TD` 2/5; G-068 and G-071 at rank 3; three misses sitting one place outside the cut. |
| 69 | 1653 | `### Where the three sets finish` | 83 | record | Six-row summary table across main, the four time strata, and held-out. |
| 70 | 1664 | `## The overnight run on the 5090 — 27 Oct` | 427 | record | The before/after table across all sets (time 14/24 → 16/24, held-out 6/13 → 7/13 and "spent, once"); pinned model call; `context_scope` inert on the time set; history 4/7; declined fitted triggers; the `admit_history` correction. |
| 71 | 1702 | `## The Spring AI defaults audit — 27 Oct` | 63 | record | Scope and provenance of the audit: three rounds read from the 2.0.1 `-sources.jar` in the local Gradle cache. |
| 72 | 1710 | `### Verified here, on this corpus and this machine` | 357 | record | A warning callout correcting the section itself (the app was not affected); Ollama's 4,096 context; the answerer's 2,260-token total; `JsoupDocumentReader` flattening the archive's structure; the silent discard not firing. |
| 73 | 1749 | `### What the audit actually changes` | 403 | record + decision | No determinism by construction; "accept all" drops negative-similarity documents; `SimpleVectorStore` cannot break a tie reproducibly; `truncate(false)`; smaller findings including the `allowEmptyContext` asymmetry and the topK misattribution. |
| 74 | 1790 | `### What to do, in order` | 129 | plan + decision | Four actions: pin the model call, set `num_ctx` deliberately, say what the naive baseline was, leave Hit@4 at 4 and pair all-evidence@4 with @10. |
| 75 | 1802 | `## Rules register` | 404 | reference | Table of Rules 1–8 with episode, how each compares with research, and what is still to do. |
| 76 | 1815 | `## Rules still to write` | 265 | reference + plan | Table of five unwritten rules: *Make the model beat keywords* (5), *Know when you don't know* (5), *A model you don't host can change under you*, *Retrieved text is data, not instructions*, *Retrieve only what the reader may read*. |
| 77 | 1827 | `## Shared vocabulary` | 255 | reference | Ours/industry/where table, 18 rows. |
| 78 | 1852 | `## Changes to the plan below` | 276 | decision | Seven bullets recording amendments to the 14 Oct plan, each dated. |
| 79 | 1864 | `## The rule: the main line only gets better` | 62 | reference | The ratchet rule, stated in full. **Start of the 14 Oct plan half.** |
| 80 | 1868 | `### How the rule is enforced` | 195 | reference | Five bullets: the ratchet test, what counts as worse, accept-or-reject whole steps, held-out questions, full runs once per episode. |
| 81 | 1880 | `### What that changes` | 107 | decision | Was-planned/now table, five rows. |
| 82 | 1890 | `## Episode 4: element-level hybrid search` | 0 | plan | Heading only. |
| 83 | 1892 | `### What it fixes from Episode 3` | 77 | record | Four Episode 3 failures, on the pre-v2 44-question set. |
| 84 | 1899 | `### Main-line steps, in order, with the prototype's evidence` | 266 | plan | Six-step table with prototype results and ratchet risk. |
| 85 | 1914 | `### Unchanged from the earlier plan` | 145 | plan + reference | One SQLite file; our own fusion code; 10,540 units from 627 files; Spring AI wiring; QuoteResolver; the number-in-claim check. |
| 86 | 1930 | `### Measured, every step` | 29 | reference | Four bullets naming the measures. |
| 87 | 1937 | `### Epilogues (candidates)` | 53 | plan | Five candidate epilogues. |
| 88 | 1945 | `## Episode 5: relationships — versions and typed links` | 0 | plan | Heading only. **Flagged stale at line 1527.** |
| 89 | 1947 | `### Starting point` | 101 | plan | The `links` table, version signals, typed and untyped link counts. |
| 90 | 1959 | `### Main-line steps` | 188 | plan | Two steps (supersession redirect, anchored typed expansion) with pre-v2 prototype results; Spring AI wiring. |
| 91 | 1968 | `### Honest scope` | 55 | reference | Typed links fix one kind of question; two questions in the then-current set. |
| 92 | 1972 | `### Epilogues (already measured)` | 109 | record | Six measured-and-rejected ideas, on the /44-era prototype. |
| 93 | 1981 | `## Before the golden set is frozen` | 86 | plan | Four numbered tasks. **Marked done at line 1857.** |
| 94 | 1993 | `## Build order for Episode 4` | 64 | plan | Six numbered steps. |

## 1.3 Shape and weight

- Episode 6 (lines 56–1530) is 72% of the document — about 17,000 words across 50 headings, almost
  all of it `###`/`####` record written between 24 and 27 Oct.
- The deepest nesting is four levels (`####` under `### The time questions, measured 26 Oct`),
  and that single `###` section carries 33 `####` subsections, lines 423–1438.
- Everything about Episodes 1, 2 and 3 is incidental reference inside other sections. No section
  describes them.
- The 14 Oct plan half (lines 1864–2001) is about 1,400 words, under 6% of the file, and two of its
  sections are explicitly superseded.

## 1.4 Standing rules and constraints, quoted exactly

### The ratchet

Line 1866 (`## The rule: the main line only gets better`):

> Each episode's main line ends at least as good as the one before it, on every measure the golden set tracks. A step that makes things worse doesn't ship in the main line. It goes into the episode's epilogue with its numbers and the reason it lost. Readers still learn from it, and the archive the next episode builds on never regresses.

Lines 1870–1875 (`### How the rule is enforced`):

> - **A ratchet test, run like Episode 3's fitness functions.** Each episode commits its retrieval-only results file. A `RatchetTest` re-runs the golden set without the chat model (only question embeddings) and compares against the previous episode's file.
> - **What counts as worse:**
>   - overall Hit@4 or all-evidence@4 drops;
>   - any single stratum loses more than one question (one question is noise on strata of 8 to 10);
>   - more stale elements appear in the context;
>   - fewer unanswerable questions are correctly declined (measured on the full run).

Line 1116 (`#### So the honest line is 7 of 9...`):

> The series' rule is absolute and it applies to my own work: a step that fails the ratchet is a result, not a step.

Line 1518 (`### Decisions outstanding`), on whether the time questions join the frozen set:

> My view: separately — the ratchet's denominator should not move mid-series.

### No tuning

Line 1876:

> - **Accept or reject whole steps; don't tune them.** Every step uses defaults fixed in advance, such as reciprocal rank fusion with k = 60 and the stock reranker. On 44 questions, tuning until the ratchet passes would only fit the golden set.

Line 51 (`## Later: local against remote`):

> - **Same rules as the local runs.** Fixed defaults, the same configurations (S4, S5, H1), the ratchet, no tuning. Held-out questions are scored once, at the end.

Line 1554 (`### Hit@4 is insensitive...`), the no-fitting rule applied to the metric itself:

> And since I know those two sit at rank 5, **moving to @5 to collect them would be fitting**, and the gain would be an artefact of the mismatch rather than better retrieval.

Line 1209 (`#### And every other section kind is just as sharp...`):

> **So the arm ships for `#context` and stops there**, and the other four questions are recorded as one trigger away rather than quietly fitted.

Line 1694 (`## The overnight run on the 5090`):

> - **Declined:** fitted triggers for `#changelog`, `#risks` and `#intent`. Showing one is inert would mean scoring the held-out set again.

Line 1042 (`#### And `scope_pin` failed the main set too...`), the selectivity rule:

> **a selection that includes most of what it read is an enumeration, not a judgement.**

### How the held-out set is scored

Line 1877:

> - **Held-out questions.** The 12 to 15 new relation and history questions written before the freeze aren't looked at during the build. They're scored once, at the end of each episode, to check that a gain isn't an artifact of the questions it was built against.

Line 1878:

> - Full runs with Beta and Alpha, and hand labels, happen once per episode, on the final main line.

Line 1678 (`## The overnight run on the 5090`), held-out row:

> **Spent, once.** A floor

Line 1520 (`### Decisions outstanding`):

> - Round 4 of the audit (105 held-out candidates) is deferred; until it runs every held-out number is a floor.

Line 402 (`### The time questions, measured 26 Oct`):

> No audit round has ever looked at a time candidate, so every number is a floor.

### How the golden set is frozen

Line 12 (`## Current state`):

> - **Golden set v2 is frozen** (`episode-4` branch, `src/main/resources/golden/`). The main set has 53 questions, 47 of them answerable. G-005, G-035 and G-040 are dropped. G-076 and a new intent stratum (G-077 to G-081) are added. 14 passages are accepted by a blind audit. There are 13 held-out questions and 24 time questions (`questions-time.jsonl`). Every audit decision is in `results/episode-4/`.

Line 18:

> The golden set stays frozen: `BlindAudit` (Java) and `run.py --audit` (prototype) fold each round's acceptances in at scoring time instead.

### Which tier the main line is defined on

Line 1510 (`### Decisions outstanding`):

> - ~~**Which tier the main line is defined on.**~~ **Decided 26 Oct: the main line leaves the laptop.** The series is about getting from rags to riches, not about what fits on one machine, so if the next level needs a bigger model then that is the finding and it gets published as one. The main line is defined on the 5090, with the hosted tier as the rung anyone can reach for money — which promotes *Later: local against remote* from a curiosity to the reproducibility story. `Episode5RatchetTest` keeps its laptop assertions as a second tier, because they are what a reader can re-run; they are no longer the definition. Cost is to be minimised at first, not optimised.

### What the published number counts

Line 19 (`## Current state`):

> Our scorer counts items, which is what the reader is given, so it stays the published number.

Line 1534 (`## The cut of 4, re-examined`):

> Where it came from: **Spring AI's advisor default.** Episode 3 defines Hit@4 as "the top four chunks, *the advisor's default*", so the metric was set to measure what the reader is actually handed — the same principle that settled the TREC disagreement, *"our scorer counts items, which is what the reader is given"*. A sound principle attached to an inherited number.

Line 1799 (`### What to do, in order`):

> 4. Leave Hit@4 at 4 (measured insensitive), and pair **all-evidence@4 with all-evidence@10** so a window failure reads differently from a retrieval failure. See *The cut of 4, re-examined*.

### The measures themselves

Line 1932 (`### Measured, every step`):

> - Hit@4, MRR and all-evidence@4/@10, per stratum.
> - Stale elements in the context.
> - Where the right unit ranked before and after the reranker.
> - Context tokens and latency.

### Which corpus commit is pinned

The plan never states a pin as a rule. What it states as fact:

- Line 391: "`c64d41c`" is the commit at which G-072's `plan.html` holds zero pages.
- Line 621, the "now" row of the idf table: "| now | `c64d41c` p721 | 10,678 | 134.7 | — | — |"
- Line 91: "`spectastic` was a shallow one-commit clone; `git fetch --unshallow` took 2 seconds and 20 MB. 722 commits, 25 tags, **555 of them touching `specs/`, each a distinct tree**."
- Line 1921: "**Units.** 10,540 from 627 files. Every golden address is a unit."
- Line 646: "**15,616 unit-states against 11,492 in the snapshot — 1.36x.**"

So `c64d41c` (first-parent position 721) is the de facto snapshot, but there is no sentence making
it a standing rule, and two different unit counts for the snapshot are in use (10,540 at line 1921
and 11,492 at lines 370 and 646).

### Mechanism rules derived from measurement

Line 142 (`### The result that is already in hand`):

> **Expansion that reserves slots after the anchor helps. Expansion that merges neighbours into the ranking hurts.**

Line 250 (`### Arm 9`):

> **An edge is only worth following if you let it place, not merely nominate.**

Line 242 (`### Arm 9`):

> Being in the pool is not being chosen, and a reach number is a ceiling, not a forecast. Any future arm should be sized by what it converts, never by what it reaches.

Line 386 (`### What survives, in three families`):

> A lifecycle field must be a **rejectable hypothesis, not a switch**

Line 1389 (`#### The arms cannot move the main line`):

> Every new path is behind a flag that defaults false, and the one edit to existing flow is an extract-variable whose `else` branch is the old line verbatim.

Line 766 (`#### What the literature says...`):

> Claim it as "we could not find it", not as "it has never been done".

Line 261 (`### Arm 9`):

> **a type-implied edge is a claim to verify, not a fact.**

Line 1363 (`#### The negation probe`):

> `num_ctx` is now explicit and the call checks `prompt_eval_count` against it and raises, because a ranking over candidates the model never saw is worse than no ranking: it is indistinguishable from one.

### The eight gotchas (lines 1470–1489), each stated as a standing constraint

> 1. **`git rev-list -n1 --before=<date>` does not do what it looks like.** In testing it returned the *newest* commit on the branch. `--since`/`--before` stop traversal at the first commit outside the window rather than filtering; `--since-as-filter` is the one that filters.
> 2. **Topological order is not unique** — git's own docs give two valid orderings for one example — so it cannot be a stable index key. Use first-parent order or an explicit counter.
> 3. **`idf` is time-varying.** Collection statistics — document count, average length, df — must be those that held at the cutoff. Indexing 555 revisions in one FTS5 table makes BM25 wrong for every as-of query. This is an architectural constraint, not a detail.
> 4. **"As of" is ambiguous by construction.** SQL Server's `FOR SYSTEM_TIME AS OF` means *as known at*; Oracle's `AS OF PERIOD FOR` means *as valid at*. Our `as-of` / `as-known` split is clearer than the industry's — worth saying so in the article.
> 5. **Boundary inclusivity bites.** The golden set's cutoffs are `23:59:59Z`; state them as closed bounds rather than leaving it to be inferred. MariaDB manages to be half-open in `FROM...TO` and fully inclusive in `BETWEEN` within one product.
> 6. **Materialise revisions with `ls-tree -r | cat-file --batch`** (0.11 s, no disk), not worktrees (~61 MB each — 34 GB for 555 revisions).
> 7. **Author date is not committer date.** 62 of 722 commits differ, from rebases or amends; rebase and cherry-pick preserve author date and reset committer date, and squash stamps both.
> 8. **Hash the result set per run.** A deterministic-but-arbitrary tie-break is the dangerous case precisely because it is stable, and therefore invisible to regression tests.

### Schema decisions stated as rules (lines 729–743)

> - **Half-open `[from, to)` with a sentinel, never NULL.**
> - **Normalise the cutoffs at ingest.** The golden set states them as `23:59:59Z`, which in a half-open schema leaves a one-second hole per day. Store the next day's `00:00:00Z` and keep the "through 1 March" phrasing for the reader.
> - **A dense integer position, not a timestamp.**
> - **Identity is the address, and a rename is a *valid-time* change, not a correction.**

Line 546:

> **So: order by first-parent position, derive it once, and never sort by a date at query time.**

### Vocabulary rule

Line 1829 (`## Shared vocabulary`):

> Use the industry term alongside ours the first time it appears.

### The precedence rule

Line 8:

> The sections from "Current state" to "Changes to the plan below" are the record as of 23 Oct 2026, and they win over anything further down. Everything below them is the plan as written on 14 Oct.

### Rules register and rules still to write

Rules 1–8 (line 1806–1813), stated as names:

> 1. Compute what can be computed, and let the model talk about the result. (Episode 2)
> 2. If code reads it, give it a type — ask for a shape, not prose. (Episode 2)
> 3. Trust, but verify. Check model output against something the model didn't write. (Episode 2)
> 4. Never let data lose its provenance. (Episode 3)
> 5. Know the answers before you ask. (Episode 4)
> 6. Test the test. (Episode 4)
> 7. Respect the seams. (Episode 4)
> 8. Answer in the right tense. (Renamed 20 Oct from "Keep the past; answer from the present".) (Episode 4)

Rules still to write (lines 1819–1823): *Make the model beat keywords* (5), *Know when you don't
know* (5), *A model you don't host can change under you* (local against remote), *Retrieved text is
data, not instructions* (finance corpus), *Retrieve only what the reader may read* (finance corpus).

Line 1858:

> - **The ratchet is the series' method, not a numbered rule.** Rule 5 is now "Know the answers before you ask", and Rule 8 "Answer in the right tense".

## 1.5 Every number reported as a result

Grouped by what the number belongs to. Configuration keys are the document's own.

### Episode 3 (naive baseline)

| Number | Configuration / set | Line |
|---|---|---|
| Hit@4 10/47 | "Episode 3's search scores 10/47" | 13 |
| strict Hit@4 20%, right spec in top four for 39 of 44 | Episode 3, pre-v2 44-question set | 1894 |
| identifier questions 13% | Episode 3 | 1895 |
| superseded text presented as current 3 times | Episode 3 | 1897 |
| Alpha got 4 of 6 unanswerable questions wrong | Episode 3, full run | 1820 |
| 2,147 naive chunks, 0 discarded, shortest 59 chars | Episode 3 ingestion | 1745 |
| 16,797 chars → 9,815 chars of text, 0 newlines, 0 of 20 ids addressable | `025-id-uniqueness/spec.html` under default `JsoupDocumentReader` | 1737 |

### Episode 4 (element units + BM25 + identifier lookup + lifecycle filter + supersession redirect)

| Number | Configuration / set | Line |
|---|---|---|
| Hit@4 30/32/32/32 | `RatchetTest` steps 1–3 + redirect, main set (denominator 47) | 13 |
| all-evidence@4 24/26/26/26 | same | 13 |
| stale@4 3/3/1/0 | same | 13 |
| `Success@4` 30/32/32/32, `RR@10` up to 0.624, `Judged@4` 0.61 | `ir_measures` cross-check | 13 |
| `Judged@4` 0.73 | keyword line, post-round-3 audit | 18 |
| Hit@4 20% → 64% | prototype step 1, pre-v2 | 1905 |
| identifier 4/8 → 8/8, overall 73% | prototype step 2, pre-v2 | 1906 |
| stale in top four 3 → 1 | prototype step 3, pre-v2 | 1907 |
| 10,540 units from 627 files | Episode 4 index | 1921 |
| 11,492 units; `EXCLUDED` removes 196 | lifecycle filter | 370 |
| 196 units = 96 superseded, 90 withdrawn, 10 rejected | snapshot filter | 986 |
| top-1 and top-4 identical on 47/47 when only in-force units are indexed | shipped `KeywordSearch` flaw test | 703 |

### Episode 5, laptop tier (nomic unless stated)

| Number | Configuration / set | Line |
|---|---|---|
| Hit@4 36/47, all-evidence@4 33 | step 5 (BM25 + dense, RRF k = 60), fails relation 6 → 4, losing G-031 and G-036 | 14 |
| Hit@4 32/47, all-evidence@4 28 | step 6 (+ `ms-marco-MiniLM-L-6-v2`), fails outright | 14 |
| 35/47 and 33/47 | `qwen3-embedding:0.6b` | 14 |
| all-evidence@4 28 → 29 | laptop step 6 after round-3 audit | 18 |
| `Judged@4` 0.53 | laptop fused lines | 18 |

### Episode 5, 5090 tier

| Number | Configuration / set | Line |
|---|---|---|
| X1 32/47 · 26, H1 36/47 · 33 | nomic anchor, reproduces the Mac exactly, fails relation 6 → 4 | 15 |
| 44/47, all-evidence@4 40 | step 6 with `Qwen3-Reranker-8B`, passes | 15 |
| 36/47, all-evidence@4 32 | step 6 with `bge-reranker-v2-m3`, fails | 15 |
| expected passage within top 50 for 45 of 47 | step 5 ceiling | 15 |
| unanswerable top out at 0.869, answerable start at 0.915 | Qwen3-Reranker score separation | 15 |
| `Judged@4` 0.55 (earlier 0.49), 0.56 on step 5 | 44/47 row | 18 |
| stale@4 1 → 0 | new redirect rule, all three runs | 16 |

### Episode 6 arms, pre-audit and flagged stale (line 104: "Every number below is stale twice over")

| key | 5090 Hit@4 · all@4 (nomic / qwen3+bge / qwen3+Qwen3-RR) | Line |
|---|---|---|
| `X2` | 33·28 / 33·28 / 33·28 | 110 |
| `H2` | 37 / 38 / 38 | 111 |
| `E5-2` | 37·33 / 38·34 / **46·42** | 112 |
| `EpiB` | 27·24 / 29·26 / 36·31 | 113 |

### Episode 6, main set, post-audit on the 5090

| Number | Configuration | Line |
|---|---|---|
| 44/47, all@4 40, relation 5/7 | `E5-1` | 269 |
| 46/47, all@4 42, relation 7/7, +2 (G-029, G-030) / −0 | `E5-2`, passes | 270 |
| 46/47, all@4 42, stale@4 0, MRR 0.890 | `E5-2` | 1084, 1616 |
| 46/47, all@4 42, stale@4 0, MRR 0.890 | `G2` scope arms, no pin — clean | 1085 |
| 45/47, all@4 42, stale@4 0, MRR 0.873 | `G4` + pin — `(False, ['Hit@4 46→45'])`, lost G-036 | 1086 |
| 46/47, all@4 42, stale@4 0, MRR 0.890 | `G9` the earlier keepers — clean | 1617 |
| **47/47, all@4 43, stale@4 0, MRR 0.911** | `GA` + `context_scope` — `(True, [])`, won G-080 at rank 1, intent 4/5 → 5/5 | 1618 |
| 36/47 against `E5-1`'s 44/47, won one, lost nine | `EpiB` | 224 |
| `E6-1` = `E5-1` and `E6-2` = `E5-2` on every measure; context tokens 645 against 635 | sibling-edge run `694bf0e` | 229 |
| pins on 15 of 53 questions; G-033 `[1, 17] → [4, 19]`, G-036 `[17, 4] → [16, 5]` | `scope_pin` on the 5090 | 1088 |

### Episode 6, main set, laptop tier

| Number | Configuration | Line |
|---|---|---|
| 33/47, all@4 31, stale@4 0, MRR 0.674 | `E5-2` baseline | 1151, 1299 |
| 33/47, all@4 31, MRR 0.675 | `G1` named spec ranked and fused — inert | 1300 |
| 33/47, all@4 31, MRR 0.675 | `G2` narrowed to the named document — inert | 1301 |
| 31/47, all@4 30, MRR 0.654 | `G3` rerank only when nothing named — fails the ratchet, loses G-031 and G-044 | 1302 |
| 30/47 | `scope_pin` alone — fails, losing G-031, G-032, G-044 | 1031, 1052 |
| 33/47, all@4 31, stale@4 0 — `(True, [])` | `scope_pin` + minority rule; identifier 8/8, relation 6/7, stale 7/7, single-fact 7/10, paraphrase 3/10, intent 2/5 | 1053 |
| 33/47, all@4 31, stale@4 0 | `G9` the earlier keepers — clean | 1152 |
| 35/47, all@4 33, stale@4 0 — `(True, [])` | `GA` + `context_scope`, won G-078 and G-081 | 1153 |
| Hit@4 32/47, 32/47, 33/47, 36/47, 32/47, 33/47 | S3, X1, X2, H1, E5-1, E5-2 — identical before and after the new machinery | 1395 |

### Episode 6, held-out set (13 questions)

| Number | Configuration | Line |
|---|---|---|
| 5/13, all@4 2, relation 3/8 | `E5-1` | 271 |
| 6/13, all@4 3, relation 4/8 | `E5-2`, passes, +1 (H-001) | 272 |
| 0 of 8 unseen relation questions | Episode 4's line | 1503 |
| 3 of 8 | best 5090 row | 1504 |
| 6/13 | before the overnight run; "not yet run here" | 1662 |
| 7/13, all@4 3 → 5, all@10 4 → 5, MRR 0.349 → 0.435 | `GA`, overnight run, "Spent, once. A floor" | 1678 |
| @1–@10 sweep: 5, 6, 6, **6**, 6, 6, 6 | held-out `G9`, Hit@k | 1544 |
| all-evidence @2–@10: 2, 3, **3**, 3, 3, 4 | held-out `G9` | 1581 |
| 2 questions need 1 passage, 11 need 2 — 85% compound | held-out composition | 1569, 1572 |

### Episode 6, time set (24 questions)

| key | Hit@4 | all@4 | stale@4 | history (7) | h-decoy (3) | as-of (5) | what-changed (5) | as-known (4) | Line |
|---|---|---|---|---|---|---|---|---|---|
| S3 | 4/24 | 4 | 4 | 1 | 1 | 1 | 0 | 1 | 406 |
| X1 | 3/24 | 3 | 4 | 0 | 1 | 1 | 0 | 1 | 407 |
| X2 | 3/24 | 3 | 4 | 0 | 1 | 1 | 0 | 1 | 408 |
| H1 | 8/24 | 8 | 8 | 0 | 2 | 4 | 1 | 1 | 409 |
| E5-1 | 7/24 | 6 | 6 | 0 | 3 | 4 | 0 | 0 | 410 |
| E5-2 | 7/24 | 6 | 6 | 0 | 3 | 4 | 0 | 0 | 411 |

`E5-1` across tiers (line 429): laptop nomic 7/24 stale 6; 5090 nomic 10/24 stale 7, what-changed
2/5, as-known 1/4; 5090 Qwen3-Emb-8B 10/24 stale 8, as-of 5/5; 5090 + Qwen3-Reranker-8B 11/24 stale
10, as-of 5/5, what-changed 2/5. History 0/7 and decoys 3/3 on every row.

Revision-aware rescore of `E5-1` (line 557): laptop nomic 7/24 → 3/24; 5090 nomic 10/24 → 5/24;
Qwen3-Emb-8B 10/24 → 4/24; + Qwen3-Reranker-8B 11/24 → 5/24. Every `as-of` and `as-known` → 0.

Clean vs artefact hits (line 452): 5090 nomic 4 clean / 6 artefact; Qwen3-Emb-8B 3 / 7; +
Qwen3-Reranker-8B 4 / 7.

Slice retrieval, nine cutoff questions (line 808): snapshot 0/9; T1 4/9; T2 5/9; T3 5/9; T4 5/9;
T5 6/9. Then (line 883): T7 6/9; **TD 8/9**; TE 8/9. Then (line 1015): TJ **9/9**.
Then (line 996): T9 5/9; TG 5/9; TH 7/9.

Combined ladder (line 1107): snapshot 0/9 both tiers; T3 5/9 / 5/9; T5 6/9 laptop / **7/9** 5090;
TF 6/9 laptop; TD 8/9 / 8/9 (fails ratchet, laptop 33→31); TJ 9/9 / 9/9 (fails ratchet, 5090 46→45).

27 Oct 5090 (line 1629): `T5` 8/14, `T7` 6/14, **`TF` 9/14** — 8/9 on the cutoff rows, gaining
G-073 at rank 2.

`what-changed` with `--changed-only` (line 1644): `T5` 1/5, `TD` 2/5; G-068 and G-071 at rank 3,
G-067 33rd, G-069 and G-070 outside 50. G-074 at rank 5.

Where the three sets finish (line 1655): main 47/47; time 9 cutoff rows 8/9 (G-074 at rank 5); time
5 `what-changed` 2/5; time 7 history 1/7; time 3 decoys 3/3; held-out 6/13.

Overnight run (line 1670): main 47/47 → 47/47 by `GA`; time 9 cutoff 8/9 → 8/9 by `TF`; time
`what-changed` 2/5 located → 1/5 located and told by `TF` (2 by `TD`); time history 1/7 → **4/7**
by `GA` (G-052, G-054, G-056, G-057); decoys 3/3 → 3/3 at ranks 1, 1, 1; **time 24: 14/24 →
16/24**; held-out 6/13 → 7/13. Also `G7` without the scope arms reaches history 5/7 with G-051
first.

### The cut of 4

Hit@k sweep (line 1541): main 5090 `G2` 39, 43, 44, **46**, 46, 46, 46 at @1–@8; held-out `G9` 5,
6, 6, **6**, 6, 6, 6; time cutoff+window `TD` 5, 5, 6, **8**, 10, 10, 10. Pooled over 80 questions,
75% in ranks 1–4 and 2% in 5–10; 39 of the main set's 46 hits are at rank 1.

all-evidence@k sweep (line 1578): main 5090 `G2` 37, 39, **42**, 43, 44, 44; held-out `G9` 2, 3,
**3**, 3, 3, 4; time `TD` 5, 6, **8**, 10, 10, 10.

Compound distribution (line 1566): main 39 need 1 passage, 7 need 2, 1 needs 3 (G-076); held-out 2
and 11; time 18, 4, and 1 needing 4 (G-055).

Six window failures (line 1584): G-031 `[1, 6]`, G-033 `[1, 17]`, G-036 `[16, 4]`, G-076
`[5, 5, 1]`, H-004 `[1, 8]`, H-011 `[11, 1]`.

Context budget (line 1557): four passages ≈ 636 tokens (median 608, max 1,371); a fifth ≈ 159.

### Corpus and infrastructure measurements

| Number | What | Line |
|---|---|---|
| 722 commits, 25 tags, 555 touching `specs/`; unshallow 2 s and 20 MB | spectastic history | 91 |
| 29 referenced commits reachable; 23 distinct | time questions | 93, 504 |
| 68 commits share a committer timestamp across 16 groups; 0 backward steps in 721 | committer date | 98, 541 |
| 62 of 722 commits have author ≠ committer date | gotcha 7 | 1486 |
| 0 merge commits across all 722 | gotchas 1 and 2 | 536 |
| 3,190 page-revisions; 725 spec pages ever (627 now, 98 deleted or renamed); 14,225 distinct unit-revisions against 10,540 now — 1.3x; 89% of addresses never changed (10,927 of 12,227) | corpus sizing | 593 |
| 15,616 unit-states against 11,492 — 1.36x; filtered to the last position reproduces `corpus.build()` exactly (11,492 states, 0 differences); 15 of 15 declared versions land on a state boundary | `timecorpus` | 646 |
| 12,022 addresses ever: 11,843 never excluded, 97 excluded sometimes, 82 always (81 withdrawn, 1 rejected) | lifecycle quantification | 978 |
| 478 positions where text changes, 54 distinct commit days; mean slice ≈ 6,184 units; 15,616 rows against ~334,000 for 54 day-slices — 21x | slicing cost | 1230 |
| 6 of 478 cutoffs actually asked about | just-in-time slicing | 1243 |
| idf drift: 1,535 units and median \|Δidf\| 0.656 / p95 3.155 at 2026-06-20 … 10,678 units at `c64d41c` p721 | gotcha 3 | 614 |
| 4 of 9 cutoffs in the severe region (G-065, G-066, G-062, G-072) | idf drift | 626 |
| FTS5 spikes: S4 15,616 rows, 0.09 s build, 63 ms query; S1 0.2 s, 4/9 top-4, 6/9 top-1, 12 ms; S3 59,203 rows, 0.6 s, 3/9, 8/9, 7 ms; S6 3/9, 4/9; S2 2.3 s build, 5.1 s query, withdrawn; S5 0.5 s, 3.6 s, withdrawn | six spikes | 657 |
| S2 and S5 disagree with each other on 7 of 9 | tokenizer bug | 672 |
| worst term reaches 39% of N; clamp needs > 50%; smallest slice 1,565 units | idf clamp check | 694 |
| 1,130 links against 3 bare prose references in `verify.html` | declared relations | 171 |
| 3,280 resolved intra-spec cross-artifact references; spec→design 2, a 300:1 asymmetry | declared relations | 166 |
| ~1,500 declared edges missing: `spec-decision grounding` 469/0, `spec-risk` 360/0 and 242/0, `spec-triage layer` ~168/0, `spec-parent specid` 22/0, `li defer-to` 674/166, `spec-delta target` 770/423; `typed:validates` 122 and `typed:cites` 868 complete | arm 8 | 183 |
| `principles.html` absent: 1,675 bare `P-N` mentions against 202 hyperlinks, 14 ids in one file | arm 8 | 193 |
| 166 `defer-to` edges all unit→page; `#requirements` 143, `#scope` 17, `#goals` 6; zero reachable from any pinned anchor | defer-to | 337, 344 |
| sibling reach: typed 2/8, typed+hrefs 3/8, recurring anchor id 0/8, same-page 4/8, artifact pairs 4/8, sibling dir 6/8, sibling + proposals 8/8 | arm 9 | 201 |
| 321 recurring `(dir, anchor)` pairs; ~205 wrong edges against zero right ones | recurring anchor id | 218 |
| `href:same-spec` is 6,754 of 12,283 edges; sibling-dir median 242 extra candidates against typed expansion's median 3 — 52x, against `EXPAND_MAX` of 5 | arm 9 cost | 224 |
| 9 broken intra-spec anchors | archive quality | 261 |
| 272 unjudged passages read in round 3, 1 accepted, 4 partly, 267 no; 226 decisions in rounds 1–2; 152 new pairs over 36 questions (identifier 44, intent 37) | audit | 18 |
| expansion fires on 10 of 53 questions, changes the top ten on 3 | typed expansion | 284 |
| 0 of 90 questions name an id without a spec; 12 of 12 in-text ids pin; 22 "spec but no id" questions, 9 target section anchors | query rewriting | 304 |
| `#context` 129 in-force units: expected ranks 1, 1, 1, 1 and 6 for G-077–G-081; a `why` shape fires on 19 questions across three sets and only 4 want `#context` | `context_scope` | 1142, 1146 |
| per-kind ranks: `#changelog` 596 units (G-051 2, G-052 3, G-058 9), `#risks` 415 (G-058 1, H-010 2), `#intent` 156 (G-006 1, G-018 1, H-009 2, H-011 1), `#requirements` 121 (G-031 1, G-032 1, H-004 1) | section kinds | 1189 |
| one combined collection of 4,926 section units: G-051 2→13, G-052 3→14, G-080 1→41, G-078 6→97; 31 kinds with 40+ units: G-051 2→38, G-078 6→164 | trigger-free designs | 1199 |
| a reader put G-080's `#context` first of 62 (9th fused per kind, 41st combined); picked 27 of 62 | chooser | 1163, 1181 |
| 70 s per question, just over an hour for 53, killed at 61 minutes; against ~1 minute for `E5-2` — 60x | chooser cost | 1172 |
| listwise over 50 candidates ≈ 7,500 tokens, ~4.5 minutes at 28 tokens/s; ~90 s over 20 | reader cost | 1379 |
| pointwise judge gave 10/10 to six passages; G-073 9th → 6th; listwise 9th → **1st**; G-063 2→1, G-074 1→1 | negation probe | 1336, 1344, 1350 |
| reader selected 15, 20 and 19 of twenty on the main set against 3 of twenty on cutoff questions | selectivity | 1035 |
| 29 expected time addresses: 18 reachable, 11 removed by the filter, 1 absent | reachability | 1432 |
| qwen3:8b declares 40,960, Ollama loads 4,096; answerer total 2,260 tokens = 55% of 4,096, 1,836 spare (persona 419, passages 1,371, preamble ~350, question ~120) | defaults audit | 1719, 1727 |
| three different answers in three unpinned runs; byte-identical in two pinned runs; ollama 0.40.11, qwen3:8b `500a1f067a9f…` Q4_K_M at `num_ctx` 16384, seed 42, `top_p` 0.9, `top_k` 40, `keep_alive` 30m | pinned model call `e51fd74` | 1680 |
| `BatchingStrategy` limit is 7372, not the javadoc's 7371 | defaults audit | 1788 |
| HotpotQA recall@2 10.3 → 17.3 over TF-IDF; a dense retriever at 65.9 with no links | literature | 137 |
| query2doc: RankT5 DL19 0.737 → 0.572, MonoT5 0.695 → 0.473; reranker-safe variant +0.006 nDCG@10; FiQA −9.0% (225 of 648 degraded, 122 improved); always-rewrite 0.554 vs never 0.556, learned gate 0.563, dataset-level 0.572; variance ±1.14 and ±1.63 nDCG@10; >2000 ms against BM25's 16 ms | literature | 312–328 |
| papers' corpora 3.13–35 versions per document against our 1.35; coalescing saves ~26% at Kendall's τ ≈ 0.8; slicing costs 2–3x; Anand et al. ~1% overhead; alive-at-T bitmap ~1.8 KB; Staudinger 2,000 ms | literature | 773–784 |
| Episode 5's rejected epilogues: 30/44 against 32/44 (RRF merge); 19/20 falls to 8/20 (graph-first); 17/20 against 19/20 (simulated wrong anchor) | 14 Oct plan, pre-v2 | 1886, 1887, 1964 |

## 1.6 Out of date, contradictory or superseded

The document flags some of this itself. Items marked **self-flagged** are already named in
`### Stale numbers elsewhere in this plan, now corrected or flagged` (line 1523) or in the
precedence callout.

1. **Self-flagged.** Everything below `## Changes to the plan below` is the 14 Oct plan and is
   overridden by the record above it (line 8).
2. **Self-flagged** (line 1527). "The old Episode 6 row quoted X2 as '33/47, all-evidence@4 26 → 28'
   (keeping rule, pre-audit) and 'E5-2 reaches 41/41 on the shared questions' — **no run producing a
   41/41 denominator could be found**; treat it as unverified."
3. **Self-flagged** (line 1527). "The section 'Episode 5: relationships — versions and typed links'
   below is stale twice over: still numbered Episode 5, and its figures are pre-v2, /44-era numbers
   from the keyword prototype."
4. **Self-flagged** (line 1530). "Episode 4's **published** prose still says an applied change
   'stays, right behind its successor', which `d7a1be4` made untrue."
5. **Self-flagged** (line 104). The Episode 6 arms table is "stale twice over — produced under the
   old `behind` redirect rule and on pre-round-3-audit qrels (`6615f99`, 23 Oct)".
6. **Episode map rows 5 and 6 are behind the record.** Row 5 says "step 6 at 44/47 with
   Qwen3-Reranker-8B"; row 6 says "44→46 on the best". The main set is 47/47 as of line 1618.
7. **Three different closing claims for Episode 6's time half, in document order.** Line 1105: "the
   honest line is 7 of 9, and 9 of 9 is an epilogue". Line 1270: "the episode closes at 9 of 9, and
   the configuration is `TJ`". Line 1629: `TF` reaches 8/9 and "the whole apparatus built around it
   is surplus". The 27 Oct sections win by date, so the 9/9 and the `TJ` section headings are
   superseded by `TF` at 8/9.
8. **`### The time questions, measured 26 Oct, and the line scores zero on history`** is contradicted
   by line 1675, where history is 4/7 on the 5090 with `GA`.
9. **The born-withdrawn claim is corrected by a later section.** Line 962: "**Bitemporality cannot
   reach them**". Line 1696: "`admit_history` ranks all of `EXCLUDED`, withdrawn included, so the
   shipped line *already* lets a history question see withdrawn records on the snapshot, and G-052 is
   answered by one. 'No position admits them' is true of the slice only."
10. **`### Where the three sets finish` (line 1653) is superseded within hours** by
    `## The overnight run on the 5090` (line 1664): held-out 6/13 → 7/13, time history 1/7 → 4/7,
    `what-changed` reframed from "located" to "located and told".
11. **The Spring AI audit corrects itself.** The warning callout at line 1712 says the app was not
    affected because `spring.ai.ollama.chat.num-ctx=16384` has reached every request since 11 Oct,
    while the body of the section at line 1721 says "Spring AI never sets `num_ctx`". Line 1751 adds
    a second parenthetical correction.
12. **`G2` has two incompatible definitions.** Line 1301 (laptop): "`G2` + narrowed to the named
    document". Line 1085 (5090): "`G2` scope arms, no pin". Line 1543 uses "main, 5090 `G2`" for the
    cut-of-4 analysis.
13. **The cut-of-4 analysis predates `GA`.** Line 1543 has main 5090 `G2` at 46 and line 1547 says
    "39 of the main set's 46 hits are at rank **1**". The main set is now 47/47.
14. **The no-tuning rule's denominator is stale.** Line 1876: "On 44 questions, tuning until the
    ratchet passes would only fit the golden set." The set is 53 with 47 answerable.
15. **Held-out size is stated two ways.** Line 1877 says "The 12 to 15 new relation and history
    questions"; line 1857 and line 12 say 13.
16. **`## Before the golden set is frozen` is done** (line 1857) but still reads as an open task
    list, and item 2 says "Sign off the 50 questions" — the set is 53.
17. **`## Episode 4: element-level hybrid search` is misnamed for what shipped.** Steps 4 and 5
    (vectors and reranker) moved to Episode 5 (line 1855), so Episode 4's main line is steps 1–3 plus
    the redirect (line 13). The section's own step table still lists six steps as Episode 4's main
    line.
18. **The supersession redirect's home contradicts itself.** Line 1885 (14 Oct table): "Moved to
    **Episode 5**, which becomes 'relationships'". Line 1854: "**The supersession redirect is back in
    Episode 4.**"
19. **The generator swap has two homes.** Line 31 (episode map): a `later` row. Line 1888: "Generator
    model swap | Episode 7".
20. **The snapshot's unit count is given as both 10,540 and 11,492** (lines 1921, 1996 against lines
    370, 646, 652). The 10,540/10,678 figures also appear as index sizes at lines 600 and 621.
21. **Rules 9 and 10 are cited as Episode 5's rules in the episode map (line 26) while sitting in
    `## Rules still to write`** (lines 1819–1820). The `## Rules register` table stops at Rule 8.
22. **"The advisor's default" is a misattribution, per the document itself.** Line 1534 grounds Hit@4
    in Episode 3's phrase; line 1781 says "`QuestionAnswerAdvisor` defines no topK — the 4 is
    `SearchRequest.DEFAULT_TOP_K`, so Episode 3's 'the advisor's default' misattributes it."
23. **An open decision is already being acted on.** Line 1517 lists "Whether the 24 time questions
    join the frozen set or are scored separately" as outstanding, while every results table from line
    1653 onward already reports them separately.
24. **Strikethrough edits left in place**: line 366 (`~~5 time questions~~ **3**`), line 394 (G-072
    "~~Unreachable regardless of any mechanism~~"), lines 1407–1414 (three done items), line 1510
    (the tier decision). These read as history rather than as current statements.
25. **An unresolved internal gap.** Line 1452: "**Gap:** the 'report' those annotations cite, with
    the full narrow and broad lists, is not in the repo or the vault."
26. **A known defect recorded as a gap, not fixed.** Line 1266: "`Timeline._slices` is an unbounded
    dict."
27. **Line 18 contains an internal supersession marker inside one bullet** — the clause beginning
    "Superseded below:" reports the pre-round-3 `Judged@4` figures (0.61 / 0.53 / 0.56 / 0.49) in the
    same sentence as the post-audit ones (0.73 / 0.55).

---

# 2. The unanswerable-set audit

Background given in the assignment: the main set is 53 questions, 47 answerable and 6 unanswerable
(G-045 to G-050); `Hit@4` is computed over the 47; the 6 are scored `answered=None` and excluded
from the denominator.

## 2.1 Everywhere the plan describes the golden set's size or composition

**It says 53 and 47. It never says 6, and it never names G-045 to G-050.** A grep for `G-045`
through `G-050` returns nothing in the file.

The composition statement, line 12, the only one:

> - **Golden set v2 is frozen** (`episode-4` branch, `src/main/resources/golden/`). The main set has 53 questions, 47 of them answerable. G-005, G-035 and G-040 are dropped. G-076 and a new intent stratum (G-077 to G-081) are added. 14 passages are accepted by a blind audit. There are 13 held-out questions and 24 time questions (`questions-time.jsonl`). Every audit decision is in `results/episode-4/`.

It states 53 and 47 and leaves the remainder to arithmetic. It does not say "6", does not give their
ids, and does not say what they are for.

Other places the size appears:

- Line 14: "The Java matches the prototype exactly: the same 50 results, same order, **all 53
  questions**, steps 4–6."
- Line 284: "It fires on **10 of 53 questions** — the 8 identifier and 2 relation ones, the only
  ones where anything gets pinned — and changes the top ten on **3**. On the other 43 it does nothing
  at all." (43 = 53 − 10, so the unanswerable six are inside the 43.)
- Line 1088: "The arm pins on 15 of 53 questions and hurts exactly two".
- Line 1173: "so a **53-question configuration** takes **just over an hour**".
- Line 1294: "The main set, **47 answerable questions**, on top of `E5-2`".
- Line 703: "on the **47 answerable questions**, indexing only the in-force units gives a top-1
  identical on **47/47** and a top-4 identical on **47/47**."
- Line 1657: "| **main, 47 answerable** | **47/47** | — |".
- Line 1672: "| main, 47 | 47/47 | 47/47 | `GA` |".
- Stale sizes: line 1876 "On 44 questions"; line 1984 "Sign off the 50 questions"; line 1894 "39 of
  44".

**Does it explain the purpose of the unanswerable questions?** Only once, obliquely, and not in any
section about the golden set. The explanation is a cell in `## Rules still to write` (line 1820):

> | Know when you don't know. | 5 | Abstention: Alpha got 4 of 6 unanswerable questions wrong in Episode 3. Retrieval-augmented models both over-refuse and are badly calibrated. Qwen3-Reranker's top score separates all six unanswerable questions | "Do Retrieval Augmented Language Models Know When They Don't Know?" (AAAI) |

That is the only sentence in 23,600 words that says what the six are for, and it frames them as
evidence for an unwritten rule rather than as part of the evaluation set's design.

The time set has its own single unanswerable row, and *that* one is explained twice:

- Line 510: "The ninth is G-075, whose page does not exist at its cutoff — which is the right answer,
  since **G-075 is the unanswerable-by-design row with an empty `expected`**."
- Line 804: "**G-075 is counted as its abstention row** — it has an empty `expected` and no state at
  its cutoff, so 9/9 means eight answered and G-075 correctly declined."

So the document has precise, working language for abstention on the time set and none for the main
set's six.

## 2.2 Everywhere it states the ratchet rule or the metrics — and whether refusal appears

**The ratchet rule itself (line 1866)** mentions no refusal:

> Each episode's main line ends at least as good as the one before it, on every measure the golden set tracks. A step that makes things worse doesn't ship in the main line. It goes into the episode's epilogue with its numbers and the reason it lost. Readers still learn from it, and the archive the next episode builds on never regresses.

**The enforcement list (lines 1871–1875)** is the one place refusal appears in a rule:

> - **What counts as worse:**
>   - overall Hit@4 or all-evidence@4 drops;
>   - any single stratum loses more than one question (one question is noise on strata of 8 to 10);
>   - more stale elements appear in the context;
>   - **fewer unanswerable questions are correctly declined (measured on the full run).**

Two things about that clause. It is the fourth of four, and it is the only one qualified "measured on
the full run" — that is, on a run with the chat model, not on the retrieval-only `RatchetTest`
described in the bullet immediately above it (line 1870: "A `RatchetTest` re-runs the golden set
**without the chat model** (only question embeddings)"). So the refusal clause is, by its own
wording, outside the test that enforces the ratchet. No verdict anywhere in the document cites it.

**The measures list (line 1932)** does not mention refusal:

> - Hit@4, MRR and all-evidence@4/@10, per stratum.
> - Stale elements in the context.
> - Where the right unit ranked before and after the reranker.
> - Context tokens and latency.

**Every ratchet verdict in the document is phrased purely in Hit@4, all-evidence@4, stale@4 and
MRR.** Examples, quoted: "`(False, ['Hit@4 46→45'])`" (line 1088); "**`(True, [])`**" (lines 1153,
1618); "ratchet `(True, [])`" (line 1053); "**fails on relation 6 → 4**" (line 14); "`X1` fails
against `S3` — Hit@4 4→3, all@4 4→3, one question lost — and `E5-1` fails against `S3` on stale@4
4→6" (line 413).

**The metric sections do not mention refusal.** `## The cut of 4, re-examined` (lines 1532–1606)
analyses Hit@4 and all-evidence@4 over "80 questions" pooled and never discusses the excluded six.
`### What to do, in order` item 4 (line 1799) proposes one new metric pairing — all-evidence@4 beside
all-evidence@10 — and nothing about abstention.

**The one incidental mention in a results context** is line 231, inside the sibling-edge run:

> `E6-1` matches `E5-1` and `E6-2` matches `E5-2` on *every* measure — Hit@4, all-evidence@4, MRR to three decimals, every stratum, **even the abstention score distribution**.

So an "abstention score distribution" exists in the prototype's output and is used as a sameness
check. It is never reported as a number and never scored.

**The Spring AI audit asserts abstention is measured** (line 1783):

> `RetrievalAugmentationAdvisor` defaults `allowEmptyContext = false`, replacing the question with a canned refusal when nothing is retrieved — and `QuestionAnswerAdvisor` behaves *oppositely*, which matters **because abstention is a measured stratum**.

That claim is inconsistent with everything else in the document: no stratum list anywhere includes
abstention. The per-stratum enumerations are, at line 1055: "identifier 8/8, relation 6/7, stale
7/7, single-fact 7/10, paraphrase 3/10, intent 2/5" — six strata, 47 questions, no abstention
stratum. The audit's own figures also note (line 18) the per-stratum audit counts as "identifier
(44) and intent (37)".

## 2.3 Every score with a denominator of 47, quoted

Main set, in document order.

| Quote | Line |
|---|---|
| "Episode 3's search scores 10/47." | 13 |
| "step 5 (BM25 + dense, RRF k = 60) reaches Hit@4 36/47 and all-evidence@4 33" | 14 |
| "step 6 (+ `ms-marco-MiniLM-L-6-v2`) falls to 32/47 and 28" | 14 |
| "`qwen3-embedding:0.6b` tells the same story at 35/47 and 33/47" | 14 |
| "X1 32/47·26, H1 36/47·33" | 15 |
| "step 6 **passes** with Qwen3-Reranker at **44/47** and all-evidence@4 40" | 15 |
| "**fails** with `bge-reranker-v2-m3` (36/47, all-evidence@4 32)" | 15 |
| "step 5 leaves an expected passage within the top 50 for 45 of the 47" | 15 |
| "**0.49** on its 44/47 row" (`Judged@4`) | 18 |
| "step 6 at 44/47 with Qwen3-Reranker-8B" (episode map) | 26 |
| "It scored **36/47 against `E5-1`'s 44/47**: won one, **lost nine**" | 224 |
| "\| main set, `E5-1` \| 44/47 \| 40 \| 5/7 \| start \|" | 269 |
| "\| main set, `E5-2` \| **46/47** \| **42** \| **7/7** \| **pass**, +2 (G-029, G-030) / −0 \|" | 270 |
| "on the 47 answerable questions, indexing only the in-force units gives a top-1 identical on **47/47** and a top-4 identical on **47/47**" | 703 |
| "30/47 against `E5-2`'s 33, losing G-031, G-032 and G-044" | 1031 |
| "the main set must return to 33/47" | 1046 |
| "\| `scope_pin` alone \| 9/9 \| 30/47 — **fails** \|" | 1052 |
| "\| **`scope_pin` + a selection must be a minority** \| **9/9** \| **33/47, all@4 31, stale@4 0 — ratchet `(True, [])`** \|" | 1053 |
| "\| `E5-2` \| 46/47 \| 42 \| 0 \| 0.890 \|" | 1084 |
| "\| `G2` scope arms, no pin \| 46/47 \| 42 \| 0 \| 0.890 \|" | 1085 |
| "\| `G4` + pin \| **45/47** \| 42 \| 0 \| 0.873 \|" | 1086 |
| "On the laptop's 33/47 baseline that was absorbed; at 46/47 there is no slack" | 1097 |
| "\| `E5-2` \| 33/47 \| 31 \| 0 \| — \|" | 1151 |
| "\| `G9` the earlier keepers \| 33/47 \| 31 \| 0 \| clean \|" | 1152 |
| "\| **`GA` + `context_scope`** \| **35/47** \| **33** \| 0 \| **`(True, [])`** \|" | 1153 |
| "The main set, 47 answerable questions, on top of `E5-2`" | 1294 |
| "\| `E5-2`, the baseline \| 33/47 \| 31 \| 0.674 \| — \|" | 1299 |
| "\| `G1` + the named spec ranked and fused \| 33/47 \| 31 \| 0.675 \| **inert** \|" | 1300 |
| "\| `G2` + narrowed to the named document \| 33/47 \| 31 \| 0.675 \| **inert** \|" | 1301 |
| "\| `G3` + reranking only when nothing was named \| **31/47** \| 30 \| 0.654 \| **fails the ratchet** \|" | 1302 |
| "\| before, Hit@4 \| 32/47 \| 32/47 \| 33/47 \| 36/47 \| 32/47 \| 33/47 \|" | 1395 |
| "\| after, Hit@4 \| 32/47 \| 32/47 \| 33/47 \| 36/47 \| 32/47 \| 33/47 \|" | 1396 |
| "The old Episode 6 row quoted X2 as \"33/47, all-evidence@4 26 → 28\"" (flagged stale) | 1525 |
| "### The main set is closed: 47/47" | 1612 |
| "\| `E5-2` \| 46/47 \| 42 \| 0 \| 0.890 \| — \|" | 1616 |
| "\| `G9` the earlier keepers \| 46/47 \| 42 \| 0 \| 0.890 \| clean \|" | 1617 |
| "\| **`GA` + `context_scope`** \| **47/47** \| **43** \| 0 \| **0.911** \| **`(True, [])`** \|" | 1618 |
| "\| **main, 47 answerable** \| **47/47** \| — \|" | 1657 |
| "\| main, 47 \| 47/47 \| 47/47 \| `GA` \|" | 1672 |

Also denominator-47 by implication, written without the slash: line 13's "Hit@4 30/32/32/32,
all-evidence@4 24/26/26/26 and stale@4 3/3/1/0" and "`Success@4` 30/32/32/32", and line 1541's
Hit@k sweep "39 \| 43 \| 44 \| **46** \| 46 \| 46 \| 46" for "main, 5090 `G2`".

Not denominator 47, for contrast: the held-out rows (`/13`), the time rows (`/24`, `/9`, `/14`,
`/5`, `/7`, `/3`), and the pre-v2 rows (`/44`, `/32`, `/20`).

## 2.4 Sections that already discuss abstention or refusal, quoted

There are six places. None is about the main set's six questions.

**(a) The only quantitative statement about the six** — line 15, inside `## Current state`:

> Qwen3-Reranker also separates the unanswerable questions cleanly — they top out at 0.869, the answerable start at 0.915 — which is Rule 10's evidence.

This is the strongest existing foundation: a measured, clean separation at a numeric threshold
between 0.869 and 0.915, on the 5090 tier with `Qwen3-Reranker-8B`. It is stated as evidence for an
unwritten rule, not as a metric.

**(b) The unwritten rule** — line 1820, `## Rules still to write`:

> | Know when you don't know. | 5 | Abstention: Alpha got 4 of 6 unanswerable questions wrong in Episode 3. Retrieval-augmented models both over-refuse and are badly calibrated. Qwen3-Reranker's top score separates all six unanswerable questions | "Do Retrieval Augmented Language Models Know When They Don't Know?" (AAAI) |

**(c) The ratchet clause** — line 1875:

> - fewer unanswerable questions are correctly declined (measured on the full run).

**(d) The vocabulary row** — line 1847, `## Shared vocabulary`:

> | "Not in the archive." | Abstention, selective answering | Episodes 3, 5 |

**(e) The time set's abstention row, which works** — lines 510–512 and 804–806:

> The ninth is G-075, whose page does not exist at its cutoff — which is the right answer, since G-075 is the unanswerable-by-design row with an empty `expected`.

> G-075 is counted as its abstention row — it has an empty `expected` and no state at its cutoff, so 9/9 means eight answered and G-075 correctly declined.

> G-073's T-1203 comes first of three pinned, G-063's FR-005 first of three, and **G-075 still abstains with nothing pinned at all**, because it names no spec and so has no scope to read. (line 1023)

**(f) The Spring AI finding that bears directly on refusal behaviour** — line 1762 and line 1783:

> **"Accept all" does not.** `SIMILARITY_THRESHOLD_ACCEPT_ALL = 0.0` and the filter is `score >= threshold`, while cosine runs [−1, 1] — so **negative-similarity documents are dropped**, before `.limit(topK)`, so fewer than four results is normal. Confined to the answerer path (`Archive`, `SpecCorpus`), which is Episodes 2–3; the Episode 4–6 numbers use our own `VectorSearch`.

> `RetrievalAugmentationAdvisor` defaults `allowEmptyContext = false`, replacing the question with a canned refusal when nothing is retrieved — and `QuestionAnswerAdvisor` behaves *oppositely*, which matters because abstention is a measured stratum.

Related but about the retriever rather than the answerer, and reusable as vocabulary — line 831:

> The identifier lookup documents its own refusal here — *"A spec with no id pins nothing"* — and it is right to refuse, because pinning a whole spec above the ranking would bury a better answer elsewhere. But refusing to *pin* is not a reason to refuse to *look*

And line 1147, on a deliberately weak trigger:

> Fused and never pinned, because the trigger is deliberately poor: a `why` shape fires on 19 questions across the three sets and only 4 want a `#context`, so it has to be a suggestion the ranking can decline.

## 2.5 Where a new standing rule about refusal would naturally belong

Ranked by fit, with the anchor text to attach to.

1. **`### How the rule is enforced`, the "What counts as worse" list, lines 1871–1875.** The clause
   is already there and already broken: it is the only criterion qualified "measured on the full
   run", so it sits outside the retrieval-only `RatchetTest` that produces every verdict in the
   document. A refusal metric with a denominator of 6 (or 53) would turn that clause into something
   the ratchet can actually evaluate. This is the single most natural home.

2. **`## Rules still to write`, the *Know when you don't know* row, line 1820.** The rule is already
   drafted and already assigned to Episode 5, and its "Why" cell already holds the only two measured
   facts about the six (Alpha 4 of 6 wrong; the 0.869/0.915 separation). Promoting it into
   `## Rules register` (lines 1802–1813) as Rule 10 would also close the gap where the episode map
   (line 26) cites Rules 9 and 10 as Episode 5's while neither is written.

3. **`## Current state`, the golden-set bullet, line 12.** The composition sentence — "The main set
   has 53 questions, 47 of them answerable" — is where "and 6 unanswerable, G-045 to G-050, written
   to test declining" belongs, together with how they are scored. This is the sentence any rewrite
   will be read against.

4. **`### Measured, every step`, line 1932.** The four-item list of what every step reports. A
   refusal measure belongs beside Hit@4, all-evidence@4/@10 and stale elements, or it will not be in
   any results table.

5. **`### What to do about it, and what not to` / `### What to do, in order`, lines 1592 and 1790.**
   These are the document's two metric-decision sections. Item 4 at line 1799 already changes the
   metric set ("pair all-evidence@4 with all-evidence@10"), so a refusal metric is the same kind of
   decision in the same place.

6. **`### Decisions outstanding`, line 1508.** Its second bullet is already about denominators — "the
   ratchet's denominator should not move mid-series" — which is exactly the question a refusal metric
   raises: scored over 6, over 53, or as a second number beside Hit@4 over 47.

7. **`## Shared vocabulary`, line 1847.** The row `| "Not in the archive." | Abstention, selective answering | Episodes 3, 5 |`
   would need a name for the metric and an episode number once the rule exists.

8. **`### Verified here, on this corpus and this machine` / `### What the audit actually changes`,
   lines 1710 and 1749.** The `allowEmptyContext` asymmetry and the "accept all" threshold finding
   are the mechanism by which the shipped system does or does not decline; a refusal rule should
   cross-reference them, and the line "abstention is a measured stratum" (line 1784) needs correcting
   either way.

9. **A gap worth noting: there is no section about the main set's strata.** The strata are only ever
   enumerated inline inside results prose (line 1055: "identifier 8/8, relation 6/7, stale 7/7,
   single-fact 7/10, paraphrase 3/10, intent 2/5"). If refusal is to be a stratum, the rewrite needs
   a section that does not currently exist.

---

# 3. Episode structure as currently planned

## 3.1 Status at a glance

| Episode | Subject | Status per this document |
|---|---|---|
| 1 | not described | Not mentioned anywhere. |
| 2 | not described; rules 1–3 and structured output are attributed to it | Published. Referenced only through rules and the answerer path. |
| 3 | the naive baseline | Published. Referenced as the baseline to beat and as the source of Hit@4's definition and the hand labels. |
| 4 | Knowledge base: the golden set audit, then structure-aware search | Published (line 476 refers to "the published Episode 4 article"; line 1530 to "Episode 4's **published** prose"). A draft note also exists (line 13). |
| 5 | Bigger models | Measured on both tiers, **drafted, not published** — "Draft: [[RAGs to Riches with Spring AI, Episode 5, Bigger Models]]" (line 14). |
| 6 | Links, history and time | Measured through 27 Oct, **drafted, not published** — "Draft: [[RAGs to Riches with Spring AI, Episode 6, Links, History and Time]]" (line 1668). |
| 7 | Generator model swap | **Planned only**, and only in two one-line mentions. |

Note the ambiguity about Episode 4: line 13 labels it "Draft:" while lines 476 and 1530 describe it
as in print. The safest reading is that the article is published and the vault note is the draft it
was written from.

## 3.2 What each episode covers

**Episode 1.** Nothing. There is no row in the episode map and no mention in the text.

**Episode 2.** Not described as an episode. Reconstructable only from references: Rule 1 "Compute
what can be computed, and let the model talk about the result", Rule 2 "If code reads it, give it a
type — ask for a shape, not prose" (with a caveat added 20 Oct), Rule 3 "Trust, but verify"; and the
vocabulary rows for "Fitness functions on model output" and "Structured output" (lines 1806–1808,
1844, 1846).

**Episode 3.** The naive RAG baseline, and the series' before-picture. Rule 4 "Never let data lose
its provenance". It defines Hit@4 as "the top four chunks, *the advisor's default*" (line 1534),
supplies the hand labels a later judging episode will be measured against, and scores 10/47 on
retrieval (line 13). Its specific weaknesses are catalogued at lines 1894–1897 and its real cause
diagnosed at line 1735: the default `JsoupDocumentReader` flattened the archive, stripping every
element id and every newline. Line 1797 says Episode 3's article should be corrected to say so.

**Episode 4 — "Knowledge base: the golden set audit, then structure-aware search".** Main line:
element units + BM25, identifier lookup, lifecycle filter, supersession redirect (line 25). Rules 5
Know the answers before you ask, 6 Test the test, 7 Respect the seams, 8 Answer in the right tense.
Record: `./gradlew test` passes with `RatchetTest` asserting Hit@4 30/32/32/32, all-evidence@4
24/26/26/26, stale@4 3/3/1/0 (line 13). Two things published in it are now wrong or re-decided: the
redirect's "stays, right behind its successor" sentence (line 1530) and the attribution of the 4 in
Hit@4 (line 1781). Its 14 Oct planning sections survive at lines 1890–1943 and still list the
vectors and reranker steps that moved to Episode 5.

**Episode 5 — "Bigger models".** Hybrid search (RRF, k = 60) and cross-encoder reranking; Mac
against 5090; Qwen3-Reranker (line 26). Rules 9 *Make the model beat keywords* and 10 *Know when you
don't know*, both still unwritten. Record: both steps fail the ratchet on the laptop; on the 5090
both pass, step 6 reaching 44/47 with Qwen3-Reranker-8B. The hosted tier was removed from it on
23 Oct (line 17) and became its own later episode. Drafted, not published.

**Episode 6 — "Links, history and time".** The largest part of the document. Planned as anchored
typed expansion plus supersession followed backwards and ordered, query routing, and the time axis
pulled forward (line 27). As built and measured, the claim changed to: make the corpus able to say
"then", and the existing mechanisms do the rest (line 1403). What ships: the bitemporal corpus, the
scope arms, `admit_history`, `history_keeps_superseded`, `context_scope`, and a reranker that can
read (line 1640). Results: main set 47/47 by `GA`; nine cutoff questions 8/9 by `TF`; time set
16/24; held-out 7/13. What was measured and rejected: `route_scope`, `scope_pin`, `history_pin`,
`reader_pin`, `rerank_rrf`, `section_scope`, `EpiB`, the sibling edge, structured query rewriting,
the recurring-anchor-id edge, and the section-kind chooser. Drafted, not published.

**Episode 7 — generator model swap.** Two mentions only, and no section of its own:

- Line 1888, in the 14 Oct `### What that changes` table: "| Generator model swap | Episode 7 |"
- Line 1603, from the cut-of-4 analysis: "it argues for something **Episode 7 should decide rather
  than Episode 6 assume**: if compound questions are the point, four passages may be the wrong window
  for the reader, not the wrong number for the metric."

The episode map (line 31) lists "Generator swap" as a `later` row with "none planned" for rules,
which contradicts the Episode 7 assignment.

## 3.3 Content assigned beyond Episode 7

All of it is in the episode map's `later` rows (lines 28–32) and is unnumbered. One has a section of
its own.

- **Local against remote** (lines 28, 34–54, and a 600-word section): the frozen set through
  Bedrock — Titan Text Embeddings, Cohere Embed v4 with Rerank — against the laptop and the 5090.
  Correctness is the control; the subject is cost per question, latency and its spread, drift,
  failure modes, and what leaves the building. Rule: *A model you don't host can change under you*.
  Promoted on 26 Oct from "a curiosity to the reproducibility story" (line 1513).
- **Time, continued** (line 29): "Whatever Episode 6's time half does not finish: valid-time
  indeterminacy when a cutoff lands inside a same-day cluster, and time-varying collection
  statistics". Rule 8 extended to dates. From Episode 6's record, what it inherits is: the seven
  `history` questions (which need a cutoff inferred), the five `what-changed` questions (which need
  two slices and a diff, line 935), the 14 draft ordering questions, and the `Timeline._slices` LRU.
- **Judging answers** (line 30): an automated judge measured against Episode 3's hand labels. Rule 3
  extended; Rule 6 applied to judges; Rule 2's caveat.
- **Generator swap** (line 31): "The chat model changes", rules "none planned". Conflicts with the
  Episode 7 assignment above.
- **Finance corpus** (line 32): official and unofficial sources, PDFs, access. Rules *Retrieved text
  is data, not instructions*, *Retrieve only what the reader may read*, plus Rule 7's no-seams case
  and Rule 5's blind spot.

## 3.4 Content left unassigned

- **Audit round 4** — "105 held-out candidates" — "deferred; until it runs every held-out number is
  a floor" (line 1520). No episode named.
- **The 14 draft ordering questions**, "still needing a file and an id range" (line 1519). No episode
  named.
- **Whether the 24 time questions join the frozen set or are scored separately** (line 1517). Open.
- **The routing report** the 24 `router` annotations cite: "not in the repo or the vault. The lists
  have to be reconstructed or rewritten before this arm can run" (line 1452).
- **Five questions "one trigger away"** via `#risks`, `#changelog` and `#intent` collections,
  "recorded as one trigger away rather than quietly fitted" (line 1209) and declined on 27 Oct
  (line 1694). No episode named.
- **The `what-changed` audit candidates**: "Some of those passages may answer the question; they are
  audit candidates, not judged" (line 1693).
- **`Timeline._slices` needs an LRU and a memory budget** (line 1266). No episode named.
- **The `admit_history` / withdrawn correction** is explicitly handed back: "**Correction, and it's a
  decision for you.**" (line 1696).
- **Episode 4's published prose needs a correction** (line 1530), and Episode 3's needs one too
  (line 1797). Neither is assigned to a task or an episode.
- **The five epilogue candidates for Episode 4** (line 1937: sqlite-vec, query rewriting shown once,
  Lucene, the embedder swap) are unassigned leftovers from the 14 Oct plan.
