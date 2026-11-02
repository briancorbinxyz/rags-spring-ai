# Closing spectastic honestly at Episode 6

Research pass, 29 Oct 2026, on branch `episode-5` at `f877ee7`. Nothing was run, nothing was
judged, no branch was changed, and the vault was read only. Every file path below is absolute or
relative to `/Users/briancorbin/Code/rags/rags-spring-ai`.

Labels used throughout: **[decided]** = the author has settled it in the live plan or a draft's
callout; **[open decision]** = the plan's own *Open decisions* list or a draft callout that says
"yours to decide"; **[inference]** = mine, from the evidence cited.

---

## 1. The Spring AI debt, measured component by component

### Takeaway

The Java reaches the end of Episode 5's **laptop** tier and stops. Keyword search, vector search,
RRF, identifier lookup, the standing filter, the cross-encoder and the supersession redirect all
exist in Java; **typed expansion, the scope arms, the router, the bitemporal corpus, the timeline,
`moved_in`/`changed_between` and the whole 5090 tier exist only in Python.** A reader cannot
reproduce any published Episode 5 5090 number or any Episode 6 number from the Java. The author has
already written this down in the Episode 6 draft and decided not to claim a Java line.

### Cited findings

**Java tree, 52 files** (not 41 — count re-measured) under
`src/main/java/xyz/briancorbin/rags/`, packages `spectastic/` (5), `search/` (20), `units/` (6),
`answer/` (7), `eval/` (10), plus `App`, `ArchiveConfiguration`, `RunConfiguration`,
`EmbeddingDemo` — [`find src/main/java -name '*.java'`].

**Prototype, 15 modules** under `tools/ratchet-proto/ratchet/` on `origin/episode-4-prototype`:
`bm25` (64 lines), `corpus` (418), `embed` (240), `html` (305), `idlookup` (149), `llamacpp` (410),
`pipeline` (917), `rerank` (252), `revisions` (181), `score` (259), `splitter` (57),
`timecorpus` (215), `timeline` (125), `tok` (59), `trec` (91) —
[`git ls-tree origin/episode-4-prototype -- tools/ratchet-proto`].

Stage by stage:

| pipeline stage | Java | Python prototype | reproducible from Java? |
|---|---|---|---|
| element units from HTML | `spectastic/SpectasticReader`, `spectastic/SpecCorpus`, `units/ElementUnits`, `units/Address`, `units/Units` | `ratchet/html.py`, `ratchet/corpus.py`, `ratchet/splitter.py` | **yes** |
| keyword search (BM25, SQLite FTS5) | `search/KeywordSearch`, `search/StopWords` | `ratchet/bm25.py`, `ratchet/tok.py` | **yes** |
| identifier lookup | `search/IdentifierLookup`, `search/IdentifierGrammar`, `spectastic/SpecGrammar` | `ratchet/idlookup.py` | **yes** |
| standing / lifecycle filter | `units/StandingFilter`, `units/Standing`, `units/Standings`, `spectastic/Lifecycle` | `pipeline.py` (`_live_items`, `_admits`) | **yes** |
| dense search + embedding | `search/VectorSearch`, `Embedder`, `ModelEmbedder`, `CachingEmbedder`, `PrefixedEmbedder`, `PrefixedEmbeddingModel`, `EmbeddingPrefix`, `VectorCache`, `OllamaModel` | `ratchet/embed.py` | **laptop tier only** (Ollama `nomic-embed-text:v1.5`; no llama.cpp path) |
| RRF (k = 60) | `search/ReciprocalRankFusion` | `pipeline.py:rrf()` | **yes** |
| cross-encoder rerank | `search/CrossEncoderReranker` (HTTP to `tools/rerank-service/serve.py`) | `ratchet/rerank.py`, `ratchet/llamacpp.py` | **laptop tier only** (`ms-marco-MiniLM-L-6-v2`; no Qwen3-Reranker-8B path) |
| supersession redirect | `search/SupersessionRedirect`, `search/Successors`, `spectastic/Supersession` | `pipeline.py:redirect()` | **yes** |
| chain assembly as Spring AI | `search/ElementRetriever` (a `DocumentRetriever`), `search/PostProcessors`, `search/TopDocuments` | `pipeline.py:Pipeline.run()` | **yes, to step 6** |
| **anchored typed expansion (`X2`/`E5-2`)** | **absent** | `pipeline.py:expand()` (817), `Graph.typed_neighbours`, `typed_pages`, `TYPED_WEIGHTS`, `EXPAND_MAX = 5` | **no** |
| **RRF over neighbours (`EpiB`)** | **absent** | `pipeline.py:rrf_neighbours()` (891) | **no** |
| **scope arms (named document, siblings)** | **absent** | `scope_units`, `scope_ranking`, `siblings`, `_named_scope`, `_scope_bm25`, `SCOPE_MAX`, `SIBLING_SEEDS`, `SIBLING_MAX` | **no** |
| **section / `#context` collection** | **absent** | `section_ranking`, `context_ranking`, `_section_kinds`, `_named_kind`, `_why_asked` | **no** |
| **intent routing / excluded-unit admission** | **absent** | `intent_ranking`, `excluded_ranking`, `history_picks`, `scope_picks`, `_history_asked`, `admit_history` | **no** |
| **bitemporal corpus (states, intervals)** | **absent** | `ratchet/timecorpus.py` — `State`, `TimeCorpus.at`, `history_of`, `build` from git | **no** |
| **timeline / slices** | **absent** | `ratchet/timeline.py` — `Timeline.at`, `items_at`, `_slices` cache | **no** |
| **`changed_between` / `moved_in`** | **absent** | `timeline.py:changed_between()`, `timeline.py:moved_in()` | **no** |
| **git revision access (as-known, ground truth)** | **absent** | `ratchet/revisions.py` — `GitHistory`, `Revision`, `verdict`, `covered` | **no** |
| scoring, qrels, run files | `eval/GoldenScorer`, `GoldenSet`, `GoldenRun`, `Ratchet`, `RetrievalRun`, `RunCsv`, `RunJsonl`, `RunSummary`, `TrecFiles`, `BlindAudit` | `ratchet/score.py`, `ratchet/trec.py`, `run.py`, `trec_check.py`, `combine.py`, `run_timeline.py`, `story.py` | **yes for the main set; no combined-105 tool in Java** |

Confirmed by grep: nothing in `src/main/java` or `src/test/java` mentions expansion, a timeline,
bitemporality, `moved_in`, `changedBetween`, a cutoff, routing or an intent ranking. The only
`scope` hits are `SpecGrammar`'s identifier scoping and `Units.SCOPE`
[`grep -rniE "expand|timeline|bitemporal|moved_in|cutoff|routing|intent" src/main/java src/test/java`].

**The Java's reranker is a shim, and says so.** `CrossEncoderReranker`'s own doc comment: *"running
a cross-encoder in-process in Java means an ONNX export and a tokenizer, which is an episode of its
own. One HTTP call keeps the scores identical to the prototype's, which is what the parity check
needs."* The model named is `cross-encoder/ms-marco-MiniLM-L-6-v2` behind
`tools/rerank-service/serve.py` — the 22M laptop model, not Qwen3-Reranker-8B
[`src/main/java/xyz/briancorbin/rags/search/CrossEncoderReranker.java:24-38`].

**The Java's ratchet tests assert the laptop's *losing* numbers, deliberately.**
`Episode5RatchetTest` asserts step 5 at Hit@4 36, all-evidence 33, stale 0, and step 6 at 32/29/0,
with the comment *"Episode 5's two steps on the laptop tier, through the ratchet, and both of them
lose… A step that fails the ratchet is a result, so it is asserted like one."* It needs Ollama
`nomic-embed-text:v1.5` at a pinned digest and the local rerank service, and *skips cleanly
without* them. There is no Java test, config or code path for `Qwen3-Embedding-8B` or
`Qwen3-Reranker-8B` [`src/test/java/xyz/briancorbin/rags/eval/Episode5RatchetTest.java:46-58,
122-150, 229-251`].

**`RatchetTest` (Episode 4) does reproduce in Java**: Hit@4 30/32/32/32, all-evidence 24/26/26/26,
stale 3/3/1/0, with run files written to `build/episode-4/` for the `ir_measures` cross-check
[`src/test/java/xyz/briancorbin/rags/eval/RatchetTest.java:168-172`;
`src/main/resources/golden/README.md`, "As a TREC test collection"].

**Test suites.** Java: 20 test files, including `ArchitectureTest` (ArchUnit slices),
`RatchetTest`, `Episode5RatchetTest`, `TrecFilesTest` (fails if the shipped `qrels.txt` and the
golden set disagree), plus unit tests for `KeywordSearch`, `IdentifierLookup`,
`SupersessionRedirect`, `StopWords`, `PrefixedEmbeddingModel`, `GoldenScorer`, `GoldenSet`,
`GoldenRun`, `SpecCorpus`, `SpectasticReader`, `Address`, and the three `answer/` tests. Python: 6
test files, 71 test functions — `test_idlookup` (11), `test_llamacpp` (25), `test_score` (20),
`test_story` (6), `test_text` (6), `test_trec` (3)
[`find src/test -name '*.java'`; `git show origin/episode-4-prototype:tools/ratchet-proto/tests/*`].

**Neither side has a unit test for any Episode 6 mechanism.** There is no `test_pipeline.py`, no
`test_timeline.py`, no `test_timecorpus.py`, no `test_revisions.py` and no `test_corpus.py`;
`pipeline.py` is 917 lines and `timecorpus.py` + `timeline.py` are 340 more, all untested. Java has
nothing to test, since none of it is ported.

**The author has already decided how to handle this in the article.** Episode 6's draft-status
callout: *"**The main line is the prototype's, on the 5090**, as Episode 5's was. The Java on
`episode-5` has the pinned model call (`e51fd74`) and none of Episode 6's retrieval. Porting the
bitemporal corpus and the four scope arms is the build this article implies, and the article should
not claim a Java line until it exists."* **[decided]**
[vault: `RAGs to Riches with Spring AI, Episode 6, Links, History and Time.md:22`].

**The live plan asks for the opposite — or at least more.** Under *Episode 6 — Links, History and
Time*, *What it gains*: *"It also needs **the Java port** — the slice behind a `DocumentRetriever` —
because Episode 6's mechanisms exist only in the Python prototype, and the series is called *with
Spring AI*."* **[open decision — the plan and the draft callout disagree]**
[vault: `RAGs to Riches, Series Plan.md`].

**The repository README stops at Episode 4.** On `episode-5` the README's episode table has rows
for 1–4 only, and the only run sections are *Run Episode 4*, *Run Episode 3*, *Run Episode 2*.
There is no *Run Episode 5*, no Episode 5 or 6 row, and no mention of the 5090 tier or
`tools/rerank-service` [`README.md:1-73`].

### Inferences

- **[inference]** A reader can reproduce, from Java alone: Episode 3's baseline, all of Episode 4's
  main line, and Episode 5's **laptop** tier including both failing steps. They cannot reproduce
  Episode 5's headline (44/47 with Qwen3-Reranker-8B), nor Episode 6's 46/47 or 47/47, nor the
  83/98 over 105, nor anything on the time set. Four of the six episodes are therefore
  Java-reproducible; the two with the loudest numbers are not.
- **[inference]** The Java port of Episode 6 is not one job but five of very different size:
  (a) typed expansion, which is `expand()` plus `Graph` and reads only declared links — small, maybe
  a day, and it is the episode's spine (+2 Hit@4 on every tier); (b) the four scope arms — larger,
  several days, and they are what takes 46 → 47; (c) the bitemporal corpus and `Timeline` behind a
  `DocumentRetriever` — the biggest, because it needs git access from Java and a state table, and
  this is the one the plan names; (d) `moved_in`/`changed_between` — small once (c) exists;
  (e) a 5090 tier in Java at all — an ONNX export and tokenizer for the reranker, which the Java's
  own comment calls "an episode of its own", plus a llama.cpp or equivalent embedder path.
- **[inference]** Only (e) is strictly necessary to make a *published* number reproducible from
  Java, and it is the one piece the author has already judged out of scope. So "reproduce the
  published numbers in Java" is not reachable by writing retrieval code — it needs the model
  plumbing first. That reframes the debt: it is not a port, it is a tier.
- **[inference]** The cheapest honest fix is documentary, not a port: say in Episode 5 and Episode 6
  exactly which tier the Java reproduces and which it does not, and add a *Run Episode 5* section to
  the README so the laptop tier is actually runnable from the instructions. That is hours, not days.

### Gaps

- I did not run the Java build, so "the Java reproduces Episode 4's line" rests on the asserted
  constants in `RatchetTest` and the plan's record of `./gradlew test` passing, not on a run here.
- No estimate exists anywhere in the repo or vault for how long the Java port would take. My
  sizings above are inferences from module size and dependency shape.

---

## 2. Is the bitemporal work in Java at all?

### Takeaway

No. Not one line. The bitemporal corpus, the timeline, the slice-as-collection idea,
`changed_between` and `moved_in` are all Python, and all on `origin/episode-4-prototype`.

### Cited findings

- `ratchet/timecorpus.py` (215 lines) holds `State` (with `live()`, `covers(at)`, `passage()`),
  `TimeCorpus.at(position)`, `TimeCorpus.history_of(address)`, a `build(root)` that walks git, and
  `save`/`load`/`load_or_build` with a cache file. A built cache is committed at
  `tools/ratchet-proto/cache/timecorpus-c64d41c-v1.json`.
- `ratchet/timeline.py` (125 lines) holds `items_at`, `Timeline.at(position, restrict)` with the
  `_slices` dict, `changed_between(lo, hi)`, `moved_in(lo, hi)` and `search(...)`. Its module
  docstring is the episode's argument in prose: *"A slice of `TimeCorpus` at a position is a list of
  units like any other, so `Pipeline` … runs on it unchanged."*
- `moved_in`'s docstring records both the measurement and the known hole: *"`changed_between`
  answers 'what began a state inside the window', and that admits the proposal which *narrates* a
  change… four to ten times the size of the set which actually moved — 3,598 against 517 on
  G-070"*, and *"What it does not cover is a **repeal**… so this is a substitution detector. That is
  worth saying plainly because the set holds no repeal question, and so nothing here would tell
  us."*
- `Timeline._slices` has no eviction. The plan lists it under *After the series → Time, continued*:
  *"the `Timeline._slices` cache that has no eviction."*
- `ratchet/revisions.py` (181 lines) is the git-history reader — `GitHistory.of`, `.at`, `.text`,
  `.is_shallow`, `.shape`, plus `verdict` and `covered` — also Python-only.
- Java grep for `timeline|bitemporal|moved_in|changedBetween|cutoff|slice` over `src/main/java` and
  `src/test/java` returns only `SpecGrammar`'s "slice 040" identifier alias and `GoldenScorer`'s
  "800-token slices of a page's text". Neither is temporal.

### Inferences

- **[inference]** Porting the bitemporal corpus to Java means giving Java a git reader, which no
  current Java class has — `SpectasticReader` reads a checkout, not a history. That is the single
  largest piece of the Spring AI debt and the reason I would not call it a must-close for Episode 6.

### Gaps

- None material. The absence is unambiguous.

---

## 3. The one-shot sets: have held-out (13) and routing (15) been spent?

### Takeaway

**Both are spent.** Held-out has been scored at the end of Episode 5 *and* the end of Episode 6, at
both tiers. Routing was scored once on the 5090 on 28 Oct as the whole ladder. Under the plan's own
rule — *"Held-out and routing questions are scored once, at an episode's end"* — neither can be
re-spent on a finance-corpus episode, and both are floors until audit round 4 runs.

### Cited findings

**The rule, as written.** *"**Held-out questions are scored once**, at the end of each episode.
Until the fourth audit round runs over the 105 held-out candidates, every held-out number is a
floor."* And: *"**The reserve stays reserved.** Held-out and routing questions are scored once, at
an episode's end, so the combined figure is an end-of-episode figure and `combine.py` refuses the
reserve without `--at-episode-end`."* **[decided]** [vault: `RAGs to Riches, Series Plan.md`].

**The guard is in code.** *"`combine.py` refuses a reserve source unless `--at-episode-end` is
passed, exactly as `run.py` refuses the held-out file"*
[`research_notes/The whole question set/README.md`, *The reserve*].

**Held-out, spend by spend:**

| when | tier | file | result |
|---|---|---|---|
| 24 Oct, Episode 5's end | laptop (nomic + MiniLM) | `results/episode-5/heldout-laptop.md` | `S3` 2/13 → `E5-1` 4/13; `X1` 2/13; `H1` 2/13 |
| 24 Oct, Episode 5's end | 5090, three model sets | `5090-nomic-heldout.*`, `5090-qwen3-emb-8b-heldout.*`, `5090-qwen3-emb-8b-qwen3-rr-heldout.*` | (prototype branch) |
| 26 Oct, Episode 6's end | laptop | `results/episode-6/heldout-laptop-e6.md` | `E5-2` 5/13 → `G9` 6/13 |
| 27 Oct, Episode 6's end | 5090 | `5090-qwen3-emb-8b-qwen3-rr-e6-heldout.*`, `5090-qwen3-emb-8b-qwen3-rr-heldout-e6.*` (`G9`, `GA`) | **6/13 → 7/13**, per the draft callout |

Episode 6's draft callout states it outright: *"**The held-out set is spent.** It was scored once,
on the 5090, on 27 Oct (`72d3305`): 6/13 → 7/13. It is a floor, because round 4 of the blind audit
(105 candidates) is unrun."* **[decided]** [vault: Episode 6 draft, line 23].

**Routing, spent once.** *"Run once, as written. No arm was added after looking."* The full ladder
on the 5090, 28 Oct: `A` 8/15, `B` 1/15, `C` 11/15, `S1` 3, `S2` 3, `S3` 3, `S4` 6, **`S5` 14/15**,
`E5-2` 14/15, `G9` 14/15, `GA` 14/15, missing only `R-004`. Laptop floors: 1/15 at Episode 3's
shape, `E5-2` 6/15, `GA` 5/15
[`git show origin/episode-4-prototype:tools/ratchet-proto/results/5090-routing-and-moved.md`].

**The routing set is still draft and unaudited.** All 15 questions carry `"status": "draft"`; the
plan calls it *"new, draft status, no blind audit"*; and the Episode 6 correction file re-verified
it: *"All 15 are still `\"status\": \"draft\"`"*
[`src/main/resources/golden/questions-routing.jsonl`; vault plan, *The question sets*;
`research_notes/Unanswerable set corrections/episode_6.md:38`].

**Where they land in the published figure.** The 105-question closing number for Episode 6 —
83/98 Hit@4, groundless@4 22/105, 2 confidently wrong on the 5090; 60/98 and 45/105 on the laptop —
is built with `combine.py --at-episode-end` and sources
`5090-slice-moved:TK → time-e6:GA → sections:GA → heldout-e6:GA → routing:GA`. So both reserves are
*inside the headline Episode 6 ships* [`research_notes/The whole question set/README.md`;
`5090-routing-and-moved.md` §3].

### Inferences

- **[inference]** Held-out has been spent **twice** (Episode 5's end and Episode 6's end), which is
  within the letter of the rule — once per episode — but it means its 13 questions have now informed
  two published figures and can no longer function as an unseen set for anything. The same questions
  cannot credibly open a finance-corpus episode either, because they are spectastic questions; the
  constraint for Episodes 7+ is not "the reserve is used up" so much as "there is no reserve left
  for spectastic, and a new corpus needs its own".
- **[inference]** This is the strongest argument *for* closing spectastic at Episode 6 rather than
  stretching to a seventh spectastic episode: the series has no unseen spectastic questions left to
  spend. A seventh spectastic episode would have to either grow the set (which the plan lists under
  *After the series*) or report only on sets it has already measured.
- **[inference]** The routing set is the weakest of the four as evidence, being draft, unaudited and
  outside `qrels.txt` entirely. Episode 6 is right to keep it out of the per-episode results tables
  and in *Next* plus the epilogue, which is what the plan already decides.

### Gaps

- I did not verify that the *same* held-out run was not re-read mid-episode in a way that would
  breach the spirit of the rule. The `combine.py` guard and `run.py`'s refusal make it unlikely, but
  I did not audit every invocation.

---

## 4. What remains unjudged, and how it limits the claims

### Takeaway

Audit round 3 judged **54 passages over 7 questions** — the seven the freeze changed — out of a pool
that spanned **40 questions**. Audit round 4, over the 105 held-out candidates, is unrun and
reserved for the author. `qrels.txt` holds judgments for the 47 answerable main-set questions
**only**: the time (24), held-out (13) and routing (15) sets have no lines at all, so `Judged@4` is
not even definable for 52 of the 105 questions. **I judged nothing.**

### Cited findings

**The README is stale on the audit.** `src/main/resources/golden/README.md` describes rounds 1 and 2
only, states *"291 judgments in all"*, has no row for `questions-routing.jsonl`, and says the time
questions *"have never been run against a retriever"* — which is no longer true. The shipped
`qrels.txt` is **345 lines** [`wc -l src/main/resources/golden/qrels.txt`].

**Round 3, from the evidence file.** `results/episode-5/golden-audit-round3.jsonl` is 54 lines:
**1 whole, 7 partly, 46 no**, over exactly seven ids — `G-080` (14), `G-078` (12), `G-076` (10),
`G-081` (9), `G-007` (4), `G-079` (4), `G-014` (1). The one acceptance is
`specs/031-init-tools/spec.html#scenarios`, answering G-076 whole.

**The correction, and why 272 was wrong.** HEAD (`f877ee7`, *"round 3 judged 54 passages, not
272"*): *"`qrels.txt` adjudicates, because it records refusals as well as acceptances… All 192
`no`s from rounds 1 and 2 are present there as 0-lines, and all 46 from round 3. Had 267 been made,
qrels would carry them; it gained exactly 54 lines, 291 to 345."* The seven questions are *"exactly
the questions the freeze changed: the six added, plus G-007 which was reworded and G-014 which was
among the six fixed."* The pool's **40 questions** reproduces exactly; its **size does not** (263
with the likeliest configs, 262 with others), so the article now quotes no pool size. Commit
`ac6e6ba`'s message carries the wrong tally and stays as it is **[decided]**
[`git show HEAD`; `research_notes/Unanswerable set corrections/episode_5.md:414-440`].

**What that leaves unjudged, in the author's own words.** *"Episode 5 is corrected, and says plainly
that the `Judged@4` caveat is narrowed rather than settled: the rest of that pool is still unread,
and 0.55 on the best row means nearly half of what a reader is shown there has never been looked
at."* [`git show HEAD` commit message].

**`Judged@4` by line**, from the plan's record: 0.73 on the keyword line, **0.55** on the 44/47 row
after round 3 (0.61 and 0.49 respectively before it). *"The better the retrieval, the less of it
anyone has checked."* [`research_notes/Series plan archive/Series Plan as of 27 Oct 2026.md`,
*Current state*].

**Round 4 is unrun and reserved.** Plan, *Open decisions*: *"Audit round 4, over the 105 held-out
candidates — unrun, and until it runs every held-out number is a floor. **Mine to judge, not
Claude's.**"* **[decided — and the reason the held-out and time numbers are floors]**
[vault: `RAGs to Riches, Series Plan.md`].

**The time and routing sets have never been audited at all.** `results/episode-6/README.md`:
*"`--audit` is passed empty on purpose: no blind-audit round has ever looked at a time candidate, so
every number here is a floor in the same sense the held-out numbers are."* And
`5090-routing-and-moved.md`: *"Every number is a floor: no blind-audit round has seen the routing
set or a time candidate."*

**`qrels.txt` covers 47 question ids, all `G-0xx` from the main set** — G-001…G-044 less the three
dropped (G-005, G-035, G-040), plus G-076…G-081. No `G-051`…`G-075` (time), no `H-0xx` (held-out),
no `R-0xx` (routing) [`awk '{print $1}' qrels.txt | sort -u`].

**Nobody has ever judged a candidate for any of the six unanswerable questions.** Plan: *"**G6:** no
audit round has ever judged a candidate for any of the six; both decision files hold 280 judgments
between them and not one is for `G-045`–`G-050`. That follows from the pooling rule, not from
nerve."* (226 in `results/episode-4/golden-audit-2026-10.jsonl` + 54 in round 3 = 280, which
reconciles.) **[decided — closed]**

### Inferences

- **[inference]** The honest unjudged inventory for Episode 6 is three-layered, and only the first
  layer is what people mean by "the audit": (1) **33 of the 40 pool questions from round 3 got no
  decision** — the pool spanned 40, decisions landed on 7; (2) **audit round 4 over the 105 held-out
  candidates is wholly unrun**, which is what makes every held-out number a floor; (3) **52 of the
  105 questions have no qrels lines whatsoever**, so no external scorer can check the time, held-out
  or routing numbers at all, and `Judged@4` for them is not low — it is undefined.
- **[inference]** Layer (3) is the sharpest limit on the Episode 6 headline, and it is not mentioned
  in the plan's *Open decisions*. The 83/98-over-105 figure is reported with `Judged@4` available
  for only the 47 main-set questions inside it. A one-paragraph caveat would close it honestly; a
  qrels extension to the other three sets would close it properly and is a judging session (or
  three) of work, i.e. author-only.
- **[inference]** `src/main/resources/golden/README.md` is now the most-stale consequential document
  in the repository: it is the golden set's own record, it is shipped in `src/main/resources`, and it
  is wrong about the number of judgments, the number of rounds and the state of the time set, and
  silent about the routing file. Correcting it is an hour and needs no judging.

### Gaps

- I did not and will not determine which specific candidates in the round-3 pool remain unread; that
  would require reconstructing the pool, and the reading of it is the author's.

---

## 5. What the plan says about Episodes 7+, the epilogue, and a corpus change

### Takeaway

**Six episodes and an epilogue. Nothing more.** Episode 7 is the epilogue — the honest accounting of
what failed — and it is **planned with no draft**. Everything else, the finance corpus included, is
listed under *After the series* as a candidate, not an episode. The plan anticipates a corpus change
but on no stated terms beyond "a second, very different source through the same adapter".

### Cited findings

**The frame.** *"Rewritten 27 Oct 2026. **Six episodes and an epilogue. Nothing more.**"* The
episode table's row 7: *"| 7 | *epilogue* | planned | what didn't work, what we still cannot
measure, and where to go next |"* **[decided]** [vault: `RAGs to Riches, Series Plan.md`].

**The epilogue's contents are already specified, in five bullets:** section-kind choosing failing
five ways; pinning failing three times (`scope_pin`, `reader_pin`, `intent_pin` — *"it fixed the one
question predicted and broke eighteen"*); a declining mechanism measured for and deliberately
unbuilt, *"because a threshold fitted on either available question set disagrees with the other"*;
*"**The question sets are too small**, and this is where to say so with the arithmetic"*; and the
routing set *"as evidence that confident wrongness returns on unfamiliar questions"*
[vault: `RAGs to Riches, Series Plan.md`, *Episode 7 — epilogue*].

**No epilogue draft exists.** A search of the vault for an Episode 7 or epilogue file returns
nothing; the six episode drafts are the only ones
[`find /Users/briancorbin/Documents/Brain -name "RAGs to Riches*"`]. Much of its raw material does
exist, in `research_notes/Question intent routing to 100/`,
`research_notes/Refusal as a first-class metric/groundless_at_4.md`, and Episode 6's own
*Epilogue: what the 5090 made unnecessary* section, which already tables six rejected arms.

**After the series, in the author's stated order:** *Local against remote* (*"the reproducibility
story, and the only one with a developed case"*); *Time, continued* (the 7 history questions, the 5
`what-changed` questions, the 14 draft ordering questions, the `Timeline._slices` cache with no
eviction); *Judging answers*; *Growing the question sets* (*"the move that would make a future 100%
mean something, and the prerequisite for testing a refusal threshold honestly"*); **The finance
corpus** (*"a second, very different source through the same adapter"*); *A generator swap*
**[decided]** [vault: `RAGs to Riches, Series Plan.md`, *After the series*].

**The finance corpus terms, from the old plan.** The 27 Oct snapshot's episode map carries the only
statement of what it would be *for*: *"| later | Finance corpus | Official and unofficial sources,
PDFs, access | *Retrieved text is data, not instructions*; *Retrieve only what the reader may read*;
Rule 7's no-seams case; Rule 5's blind spot |"*. The *Local against remote* section adds the link:
*"spectastic is MIT licensed and public, so sending it to an API is fine. Saying so plainly is the
point, because a private archive changes the trade — which leads into the finance corpus and
*Retrieve only what the reader may read*."*
[`research_notes/Series plan archive/Series Plan as of 27 Oct 2026.md`].

**Work on the finance corpus has already started, outside the main line.** `origin/finance-corpus`
(head `4f89d0e`, merged from `episode-3`) carries a knowledge-base schema v2 — `docs/kb-schema/`
with `schema.sql`, `apply.sql`, `extensions.sql`, `link-types.yaml`, `consequences.yaml`, an ACME
end-to-end test, and seven source profiles: `sec-edgar-10k`, `sec-edgar-8k`, `federal-register`,
`eur-lex`, `email`, `chat-slack`, `research-note`. There is also an empty `rags-finance-corpus/`
working directory in the tree (a bare `.git` only). The branch's README is still Episode 3's
**[inference: so it is a spike, not a continuation of the measured line]**
[`git ls-tree -r origin/finance-corpus`].

**Episode 6's own *Next* leans elsewhere.** Draft callout: *"**Next episode is not settled.** The
plan's candidates are *Local against remote* and *Time, continued*. The *Next* section below leans
on the second, because that is where the open questions are."* **[open decision]** [vault: Episode 6
draft, line 29].

### Inferences

- **[inference]** There is a live tension the author should resolve before Episode 6 ships: the
  plan says six episodes and an epilogue, Episode 6's *Next* section leans towards *Time, continued*
  as if a seventh step were coming, and the task framing here says Episodes 7+ move to a different
  corpus. Episode 6's closing paragraphs will read as a promise of more spectastic unless *Next* is
  rewritten to point at the epilogue and then at a new corpus. That is a paragraph, not a project,
  but it is the one editorial change that actually "closes" spectastic.
- **[inference]** The epilogue is the natural and already-provisioned home for every arm Episode 6
  rejected, and Episode 6's draft has duplicated some of that material in its own *Epilogue* section.
  Someone has to decide which lives where, or the two will contradict each other.
- **[inference]** The finance corpus is better served by the `finance-corpus` branch's schema work
  than by anything in the spectastic question sets, and it will need its own golden set from scratch —
  including its own held-out and routing reserves, since spectastic's are spent and are about
  spectastic anyway.

### Gaps

- The plan states no entry criteria for a corpus change (no "spectastic is closed when X"). If the
  author wants a stated bar, it does not exist yet and would have to be written.
- I did not read the `finance-corpus` branch's schema docs in depth; the task scoped me to
  spectastic's close-out.

---

## 6. The ratchet, and whether every episode's numbers are reproducible under it

### Takeaway

The ratchet now has **four clauses** on a fixed denominator of **77 open questions**, defined on the
5090, with a `groundless@4` clause that replaced a structurally dead one. Episodes 3 and 4
reproduce; Episode 5's laptop tier reproduces in Java; Episode 5's 5090 tier and all of Episode 6
reproduce only from the prototype's stored runs. Nothing published moves if the code is refactored,
because nothing from Episode 2 onward is published — and `episode-5` has no `-refactored` sibling
branch while 2, 3 and 4 do.

### Cited findings

**The four clauses, verbatim.** *"**What counts as worse:** 1. overall `Hit@4` or
`all-evidence@4` drops; 2. any single stratum loses more than one question; 3. more stale elements
appear in the context; 4. **`groundless@4` rises, or any groundless answer scores above the median
grounded one.**"* **[decided]**

**Clause 4 replaced a dead clause.** *"Clause 4 replaces *'fewer unanswerable questions are
correctly declined (measured on the full run)'*. That clause was written and structurally dead: the
ratchet test runs the golden set **without the chat model**, so no declining happens inside it and
the clause could never fire."*

**`groundless@4` is retro-computable by construction.** *"**No threshold appears in the
definition**, which is why it can be recomputed over runs already stored without moving any
published number."* It was called `unwarranted@4` until 28 Oct. And: *"**groundless@4 over 53 is
just `53 − Hit@4`** while nothing declines, so every groundless figure now in the articles is exact
arithmetic from a published Hit@4 rather than a new measurement."* **[decided]**

**The other standing rules:** *"Accept or reject whole steps; do not tune them"*; *"**The ratchet's
denominator does not move mid-series.** It is the **77 open questions**… and a step that fails the
ratchet is a result rather than a step"*; *"**The main line is defined on the 5090.** Laptop numbers
are a floor."* [all from vault: `RAGs to Riches, Series Plan.md`, *Standing rules*].

**Nothing published can move.** *"**Settled 28 Oct 2026: only Episode 1 is live.** Everything from
Episode 2 onward is incubating. The old plan's references to Episodes 3 and 4 being 'in print' were
wrong, and the vault's `publish: false` was right all along. So every correction below is a **fix
before publishing**, no published number moves, and nothing has to be framed as an erratum."*
**[decided]** [vault: `RAGs to Riches, Series Plan.md`].

**Frontmatter check — one inconsistency.** Episode 1 `publish: true` (2,177 words); **Episode 2
`publish: true`** (5,791 words); Episodes 3–6 `publish: false` (6,415 / 7,604 / 7,527 / 6,639).
Episode 2's flag contradicts the plan's "only Episode 1 is live"
[`grep -m1 '^publish:'` across the six drafts].

**The refactor convention, and the gap in it.** Local branches `episode-2-refactored`,
`episode-3-refactored`, `episode-4-refactored` each end in a commit *"Episode N, refactored: the
final shape, this episode's behaviour"*. **There is no `episode-5-refactored` and no `episode-6`
branch at all.** `episode-4-refactored`'s `RatchetTest` asserts exactly the same constants as
`episode-5`'s — `List.of(30, 32, 32, 32)`, `List.of(24, 26, 26, 26)`, `List.of(3, 3, 1, 0)`,
`10_540` addresses — so Episode 4's numbers are held identical across the refactor. It has no
`Episode5RatchetTest`, correctly [`git branch -a`; `git show episode-4-refactored:…/RatchetTest.java`].

**Episode 6's time set fails the ratchet against Episode 5's own line, and that is a published
finding.** `results/episode-6/README.md`: *"**The ratchet rejects Episode 5's own line on this
set.** `X1` fails against `S3` (Hit@4 4→3, all@4 4→3, −1 G-057) and `E5-1` fails against `S3`
(stale@4 4→6). The rule the series publishes, applied to the new set, calls the published steps a
regression for time."* And *"**G-057 is the spine.** … The **supersession redirect** takes it out…
D-005 is the named motivating example for that redirect in the published Episode 4 article. Episode
4's hero fix is the exact unit a history question asks about."*

**A unit-count discrepancy between the plan and the Java.** The plan: *"The snapshot is **11,492
units**; the figure 10,540 appears in the old plan and is wrong."* The Java test is named
`theArchiveReadsAsTenAndAHalfThousandUnits` and asserts `10_540`. The prototype's run headers
reconcile the two: *"10,540 element units in 11,492 items (units over 250 words split into
parts)"* [vault plan; `RatchetTest.java:77-79`; `results/episode-5/heldout-laptop.md` header].

### Inferences

- **[inference]** Every published episode's numbers are reproducible under the ratchet *in the sense
  the rule cares about* — the stored runs and the asserted constants pin them, and `groundless@4`
  was deliberately defined so it could be added retrospectively without moving anything. The
  exposure is not that a number would move; it is that for Episode 5's 5090 tier and all of Episode
  6 the only artefact that can reproduce it is the prototype's stored `.run` and `.jsonl` files, not
  runnable code a reader has.
- **[inference]** No branch's numbers would move under a refactor, because `RatchetTest` and
  `Episode5RatchetTest` assert the article's constants directly and would fail loudly rather than
  drift. The missing `episode-5-refactored` branch is a convention gap, not a correctness risk — but
  it is the kind of gap that gets harder to close the further the series moves on.
- **[inference]** The 10,540-versus-11,492 business is a vocabulary bug, not a measurement one:
  10,540 *addresses*, 11,492 *items/passages* after splitting units over 250 words. The plan calls
  10,540 "wrong" without that distinction, and the Java test's name says "units" when it means
  addresses. Worth one sentence in the plan and a test rename; it costs minutes and removes a
  reader's inevitable question.
- **[inference]** Episode 2's `publish: true` is either a stale flag or the plan is wrong about
  "only Episode 1 is live". Given memory and the plan both say Episode 1 only, I read it as a stale
  flag — but it is exactly the kind of thing that decides whether a correction is a fix or an
  erratum, so it should be checked before Episode 6 ships.

### Gaps

- I could not determine whether `episode-5-refactored` is deliberately absent (Episode 5 not yet
  final) or simply not done. Nothing in the plan or the drafts mentions the refactored branches at
  all.

---

## 7. Episode 4's length — pending the author, not a task

### Takeaway

Episode 4 is the longest draft in the series at **7,604 words** by direct count, and a trim was
mooted and explicitly deferred. This is noted, not actioned.

### Cited findings

- Measured word counts, 29 Oct: Ep 1 **2,177** (`publish: true`), Ep 2 **5,791**, Ep 3 **6,415**,
  Ep 4 **7,604**, Ep 5 **7,527**, Ep 6 **6,639** [`wc -w` over the six vault drafts].
- The plan records slightly different figures: *"Word counts went 5,473 → 6,415 (Ep 3), 5,697 →
  **7,532** (Ep 4), 5,989 → 7,082 (Ep 5), 5,839 → 6,589 (Ep 6). … Episode 4 is now the longest in
  the series by 450 words and may want a trim."* **[open decision — the author's]**
  [vault: `RAGs to Riches, Series Plan.md`, *Before each episode ships*].
- Episode 4's correction list is the largest single gain in the series: *"**Episode 4** — a new
  section of 250 to 350 words. The biggest gap in the series."* Thirty-four numbered edit items
  [`research_notes/Unanswerable set corrections/episode_4.md`].

### Inferences

- **[inference]** The plan's figures are a day or two stale: Ep 4 is 7,604 not 7,532, Ep 5 is 7,527
  not 7,082, Ep 6 is 6,639 not 6,589. Ep 4 is still the longest, but by 77 words rather than 450, so
  "the longest in the series by 450 words" no longer holds. The trim case is weaker than the plan
  states. **This is the author's call and I did not touch the draft.**

### Gaps

- None.

---

## 8. Net: the ordered closing checklist

### Takeaway

Four items must close for Episode 6 to ship honestly, and every one of them is writing or a
correction — **not a run, not a port, not a judging session**. The three items that cost real
compute or real judgement (round 4, the Java port of the Episode 6 retrieval, a 5090 tier in Java)
are all droppable for Episode 6 provided the caveats are stated, because the author has already
decided the main line is the prototype's and that the article will not claim a Java line.

### The checklist, ordered

**MUST CLOSE — Episode 6 cannot honestly ship without these.**

1. **Rewrite Episode 6's *Next* so it closes spectastic.** Today it leans on *Time, continued* and
   reads as a promise of a seventh spectastic step, while the plan says six episodes and an
   epilogue. **Cost: a paragraph, under an hour.** Without it the article promises work the series
   has decided not to do. [Evidence: Episode 6 draft lines 29, 400–418 vs. the plan's *Six episodes
   and an epilogue*. **[open decision]** — which candidate it points at is the author's.]

2. **State the Spring AI position in the article, in the article's own voice.** The honest sentence
   already exists in the draft's delete-before-publishing callout; it has to survive into the text:
   the main line is the prototype's on the 5090, the Java carries Episodes 3–5's laptop line, and
   Episode 6's retrieval is not in Java. **Cost: a short section or a long paragraph, half a day.**
   Episode 6 cannot ship without it — a series called *with Spring AI* whose headline comes from
   Python needs to say so where a reader will see it. [Draft line 22 **[decided]**; contradicted by
   the plan's *"It also needs the Java port"* **[open decision]** — pick one.]

3. **State the unjudged position, all three layers.** Round 3 judged 54 over 7 of a 40-question
   pool; round 4 is unrun so every held-out number is a floor; and 52 of the 105 questions have no
   `qrels` lines at all, so `Judged@4` is undefined for the time, held-out and routing sets.
   `Judged@4` 0.55 on the best row means nearly half of what a reader is shown has never been read.
   **Cost: a paragraph in Episode 6 plus a line in Episode 5, a few hours.** The 83/98-over-105
   headline is not honest without it. [`git show HEAD`; `results/episode-6/README.md`;
   `5090-routing-and-moved.md`; qrels id audit.]

4. **Correct `src/main/resources/golden/README.md`.** It is shipped with the code, it is the golden
   set's own record, and it is wrong: rounds 1–2 only, "291 judgments" against a 345-line file, no
   row for `questions-routing.jsonl` (15), and *"[the time questions] have never been run against a
   retriever"* when Episode 6 is built on them. **Cost: an hour. No judging required.** A reader who
   follows it to the `ir_measures` command gets a different number from the article's.

**SHOULD CLOSE — Episode 6 can ship without these, but the series is weaker for it.**

5. **Add a *Run Episode 5* section to the repo README and an Episode 5 row to its table.** The
   README on `episode-5` documents up to Episode 4 only; there is no instruction anywhere for the
   laptop tier the Java *can* reproduce, nor a mention of `tools/rerank-service`. **Cost: an hour.**
   This is the cheapest single increase in the series' reproducibility, and it closes part of item 2
   without writing any Java.

6. **Settle whether the 24 time questions join the frozen set.** Episode 6 is built on them, and
   they are the one set the plan still lists as undecided. **Cost: a decision, plus a re-check pass
   if yes.** **[open decision — plan's *Open decisions*, first bullet.]**

7. **Settle whether withdrawn records count as history.** *"it's mine to make"* says the draft, and
   the shipped line has already half-made it: `admit_history` ranks everything in `EXCLUDED`,
   `withdrawn` included, and **G-052 is answered because of it**. If the answer is no, history drops
   **4/7 → 3/7** and Episode 6's history numbers change. **Cost: a decision; a re-run only if the
   answer is no.** This is the one open decision that can still move an Episode 6 number.
   **[open decision — plan, and Episode 6 draft line 24.]**

8. **Draw Episode 6's own three diagrams.** The *refusal branch* figure is drawn
   (`attachments/RAGs to Riches Episode 6, the refusal branch.excalidraw.md` + PNG, 29 Oct), as is
   Episode 4's *every stage returns a list*. The draft's callout asks for three more: the state table
   (one unit, three intervals), a slice cut through it at one position, and the four collections as
   subsets of one archive. **Cost: a day's drawing.** The episode's thesis is *two keys, not one* and
   *a subset is a collection*, and neither currently has a picture. Ship-able without, but it is the
   episode whose argument most needs one.

9. **Reconcile Episode 6's own *Epilogue* section against the planned Episode 7.** Both now hold the
   rejected arms — `scope_pin`, `reader_pin`, `section_scope`, `rerank_rrf`, `route_scope`,
   `history_pin`. **Cost: an editorial decision plus a trim, half a day.** Decide which lives where
   before the epilogue is drafted, or they will contradict each other.

10. **Fix the plan's own stale numbers.** Word counts (Ep 4 is 7,604 not 7,532; Ep 5 7,527 not
    7,082; Ep 6 6,639 not 6,589, so "longest by 450 words" is now "by 77"), and the
    10,540-versus-11,492 unit count, which is addresses versus split items and not an error at all.
    Also check Episode 2's `publish: true` frontmatter against *"only Episode 1 is live"*. **Cost:
    under an hour.** [Ep 4's trim itself stays **pending the author**, per item 7 above.]

**DROPPABLE for Episode 6 — real work, but it belongs after the close, and Episode 6 can ship
honestly without it provided items 2 and 3 are done.**

11. **Audit round 4, over the 105 held-out candidates.** **Cost: a judging session, author-only, and
    explicitly *"Mine to judge, not Claude's"*.** Droppable because the plan has already defined the
    consequence: every held-out number is a floor until it runs, and saying "floor" is honest. It
    does not block Episode 6; it blocks any *claim* that the held-out number is final — and the only
    thing that would be unforgivable is publishing 7/13 without the word "floor".

12. **Extend `qrels.txt` to the time, held-out and routing sets.** 52 of 105 questions have no
    judgments, which is what makes `Judged@4` undefined outside the main set. **Cost: several
    judging sessions, author-only.** Droppable for Episode 6 given item 3's caveat; a prerequisite
    for the plan's *Growing the question sets*.

13. **Port Episode 6's retrieval to Java** — typed expansion first (small, a day, and it is the
    episode's +2 spine), then the four scope arms (several days), then the bitemporal corpus and
    `Timeline` behind a `DocumentRetriever` (largest; Java has no git reader today), then
    `moved_in`/`changed_between`. **Cost: a week or more.** Droppable because the author has already
    decided the article will not claim a Java line — but the plan asks for it, so the drop needs to
    be an explicit decision rather than a silence. **[the plan and the draft disagree here; this is
    the biggest single open decision on the list.]**

14. **A 5090 tier in Java at all** — an ONNX export and tokenizer for the reranker (*"an episode of
    its own"*, per the Java's own comment), plus a non-Ollama embedder path. **Cost: an episode.**
    Droppable, and I would say permanently: without it no published headline is Java-reproducible,
    and with it the series acquires a model-plumbing subplot that is not what it is about. Say it
    plainly in item 2 instead.

15. **Unit tests for the Episode 6 mechanisms.** `pipeline.py` (917 lines), `timecorpus.py` (215)
    and `timeline.py` (125) have no tests on either side; the prototype's 71 tests cover
    `idlookup`, `llamacpp`, `score`, `story`, `text` and `trec` only. **Cost: a day or two.**
    Droppable for a prototype whose results are all in committed run files — but it is the series'
    own Rule 6, *test the test*, going unapplied to the episode that needs it most. **[inference:
    worth a sentence of acknowledgement in the article if it is not going to be done.]**

16. **An `episode-5-refactored` branch**, to match 2, 3 and 4. **Cost: the refactor pass the other
    three got.** Droppable; a convention gap, not a correctness one, since `Episode5RatchetTest`
    asserts the article's constants and would fail loudly rather than drift.

### Can Episode 6 honestly ship?

**[inference] Yes — on items 1 to 4 alone, and all four are writing.** The substantive work is done:
the numbers exist, the runs are committed, the corrections are through, two diagrams are drawn, both
reserves are spent and recorded as spent, and the ratchet's refusal clause is written. What is
missing is not measurement but disclosure: which tier the Java reaches, how much of what a reader is
shown has never been judged, and that spectastic ends here. The three expensive items — round 4, the
Java port, a Java 5090 tier — are each already handled by an explicit decision or an explicit
caveat, which is the difference between a debt and a lie.

### Gaps

- **Costs are my estimates.** The repository and the plan contain no effort estimates for any of
  these items; the figures above are inferences from file size, module coupling and the author's own
  record of how long comparable passes took (e.g. "applied 28 Oct, Episodes 3 to 6. All four edit
  lists are through").
- I did not attempt to order items 5–10 against each other by value; they are listed roughly in
  increasing cost.
