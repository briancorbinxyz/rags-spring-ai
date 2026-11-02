# Close spectastic, then aggregate in finance

## Executive summary

**Close spectastic at Episode 6. Move to finance. But move there for aggregation questions, not for multi-hop retrieval.**

Everything still needed to ship Episode 6 honestly is writing. Four items have to land, and not one of them is a code run, a Java port or a judging session. Rewrite Episode 6's *Next* section so it closes the series instead of promising a seventh spectastic step. State the Spring AI position in the article's own voice. State the unjudged position in all three of its layers. Correct the golden set's own shipped README, which is now wrong about several things. The three genuinely expensive jobs — the fourth audit round, porting Episode 6's retrieval to Java, and building a high-end GPU tier in Java — are each already covered by a decision you have made or a caveat you have written down. That is the difference between a debt and a lie.

Four findings drive the whole decision.

**First, spectastic has no unseen questions left.** The 13 held-out questions were scored at the end of Episode 5 and again at the end of Episode 6. The 15 routing questions were scored once, on 28 October. Under your own rule — reserve questions are scored once, at an episode's end — there is nothing left to spend. A seventh spectastic episode would have to either grow the question set or re-report ground it has already measured.

**Second, finance does not need multi-hop retrieval, and the published record says so clearly.** Eleven financial question-answering benchmarks are usually cited in this area. The number of them that require retrieving evidence from two or more different filings is **zero**. Ten hand the model a single page, a single report or a single table. The eleventh, SEC-QA, is built on a private commercial database and was never released. Saying this plainly in print would itself be a contribution, because the literature routinely mislabels its own benchmarks — MultiHiertt's "multi-hop" means across tables inside one annual report, and the document is supplied with the question.

**Third, top-K aggregation is a genuinely different problem, and the retrieval machinery you have built cannot do it.** Hit@4 asks whether at least one relevant passage appeared in the top four results. An aggregation question is only correct if the *complete* set of matching entities was found. Those are different quantities, not a strong and a weak measure of the same thing. The clearest published demonstration is that retrieving more does not help: on AGGBench, a benchmark of aggregation questions over a document collection, raising the number of retrieved passages from 5 to 50 lifted chunk recall only slightly (from 0.042 to 0.072) while the count error in the answers got roughly four times worse (from 0.463 to 1.855). So the obvious engineering reflex — retrieve more, rerank harder — actively hurts.

**Fourth, you already own the hard half of what an aggregation episode needs, and the missing half is small.** The knowledge-base schema is not on a side branch; it is already on `episode-5` and in your working tree, and its test suite is green. It models three separate clocks: when a fact is true in the world, when this knowledge base recorded it, and when the source published it. That is exactly right for institutional holdings data, where a position is true at the quarter end but only becomes knowable up to 45 days later. What the schema does not have is anywhere to put a number you would add up. There is no `sum()` and no `avg()` anywhere in its 95,313 bytes of SQL. Adding a holdings table is roughly fifteen lines of DDL. The hard half — identity, validity, provenance, status — is built and tested. The easy half is unwritten.

**What the next episodes should be.** Episode 7 is the epilogue, as planned — the honest accounting of what failed. Episode 8 builds the structured fact layer: a Form 13F profile, a holdings table, an "as at this date" top-K query, and a new scoring artefact, because your current evaluation tables cannot score a ranked list of companies at all. Episode 9 tackles set completeness and operational exposure, where the published evidence is that precision is achievable and recall has never been measured. Episode 10, only if Episode 9 produces a gold set, tackles the synthesis stage and runs the experiment nobody has run: does adding a synthesis step make the system more confidently wrong?

One caution to carry through all of it. A finance corpus needs its own held-out and routing reserves, written and frozen before anything is scored. Spectastic's are spent, and they are about spectastic anyway.

---

## Four writing items close spectastic; the Java port is a decision, not a debt

The substantive work on spectastic is finished. The numbers exist. The runs are committed. The corrections have been applied across Episodes 3 to 6. Two diagrams are drawn. Both reserve question sets have been spent and recorded as spent. The ratchet's fourth clause is written — it fires if `groundless@4` rises, or if any groundless answer scores above the median grounded one — and it was deliberately defined with no threshold in it, so it could be computed retrospectively over stored runs without moving a single published number.

What is missing is not measurement. It is disclosure.

The table below lists the four items that must land. The thing to take from it is that each one is a paragraph or a short section of prose, and each one prevents a specific dishonesty rather than adding a feature.

| Must close | Why Episode 6 cannot honestly ship without it |
|---|---|
| Rewrite *Next* so it points at the epilogue, then at a new corpus | As written it leans on *Time, continued* and reads as a promise of a seventh spectastic episode, which the plan has decided against |
| State the Spring AI position in the article's voice | A series called *with Spring AI* whose headline number comes from Python has to say so where a reader will see it |
| State the unjudged position, all three layers | The headline figure of 83 of 98 answerable questions over a 105-question set is not honest without it |
| Correct `src/main/resources/golden/README.md` | It ships with the code, it is the golden set's own record, and a reader who follows it to the `ir_measures` command gets a different number from the article's |

### How far the Java actually reaches

The Java implementation reaches the end of Episode 5's laptop tier and stops there.

Several things do exist in Java. Keyword search over SQLite's full-text index, vector search, reciprocal rank fusion at k=60, identifier lookup, the standing filter, the cross-encoder reranker and the supersession redirect are all present. There are 52 Java source files.

Several things exist only in the Python prototype. Typed expansion, the four scope arms, the intent router, the bitemporal corpus, the timeline, `moved_in`, `changed_between` and the whole high-end GPU tier are Python only. A search across the Java tree for expansion, timelines, bitemporality, cutoffs, routing or intent rankings returns nothing relevant.

Two details make the gap concrete. The Java reranker is an HTTP call out to a local Python service running `ms-marco-MiniLM-L-6-v2`, a 22-million-parameter model. Its own doc comment explains why: running a cross-encoder in-process in Java would need an ONNX export and a tokenizer, which is "an episode of its own". And `Episode5RatchetTest` deliberately asserts the laptop tier's *losing* numbers. Step 5 is pinned at Hit@4 of 36, all-evidence of 33, stale of 0; step 6 at 32, 29, 0. The test skips cleanly if the local embedding model and rerank service are absent. There is no Java test, config or code path for the larger Qwen3 embedding and reranking models at all.

So a reader working from the Java alone can reproduce Episode 3's baseline, all of Episode 4's main line, and Episode 5's laptop tier including both of its failing steps. They cannot reproduce Episode 5's headline of 44 of 47, Episode 6's 46 of 47 or 47 of 47, the 83 of 98 over the combined 105 questions, or anything on the time question set. **Four of the six episodes are reproducible from Java. The two with the loudest numbers are not.**

That reframes what the debt is. The missing piece is not retrieval code. It is model plumbing, and model plumbing is the one piece you have already judged out of scope. Writing typed expansion in Java would take about a day and is the spine of Episode 6, worth two extra questions on every tier — but it would not make a single published number reproducible, because the published numbers come from models the Java cannot load.

**The plan and the Episode 6 draft contradict each other here, and the contradiction has to be resolved rather than left standing.** The draft's delete-before-publishing callout says the main line is the prototype's on the high-end GPU, that the Java carries none of Episode 6's retrieval, and that the article should not claim a Java line until the port exists. The series plan says the opposite, or at least more: under *Episode 6 — Links, History and Time*, it says the episode "also needs the Java port — the slice behind a `DocumentRetriever`". One of those has to go.

The recommendation is the draft's position, plus the cheapest honest fix. Say in the article exactly which tier the Java reproduces and which it does not. Then add a *Run Episode 5* section to the repository README. That README's episode table currently stops at Episode 4, there is no instruction anywhere for the laptop tier the Java *can* reproduce, and `tools/rerank-service` is never mentioned. That is an hour of work and it is the largest single increase in the series' reproducibility available at that price.

### The unjudged position has three layers, and only the first is what people mean by "the audit"

**Layer one: round 3 of the blind audit judged 54 passages across 7 questions, out of a pool that spanned 40 questions.** The 54 decisions break down as 1 "whole", 7 "partly" and 46 "no". The seven questions are exactly the ones the freeze changed: the six added, plus G-007 which was reworded. So 33 of those 40 pool questions received no decision at all.

A correction belongs here, because it is already in the repository and would otherwise be repeated. An earlier tally said round 3 made 272 judgments. It did not. The `qrels.txt` file adjudicates the question, because it records refusals as well as acceptances — every "no" appears as a zero line. The file gained exactly 54 lines, going from 291 to 345. The pool's *count* of 40 questions reproduces reliably; the pool's *size* does not, coming out at 263 under the likeliest configuration and 262 under others. That is why the article now quotes no pool size at all.

**Layer two: audit round 4, over the 105 held-out candidates, is unrun.** It is reserved for you — the plan says "Mine to judge, not Claude's" — and until it runs, every held-out number is a floor. That is fine, provided the word "floor" appears next to the number.

**Layer three is the sharpest, and it is not in the plan's open-decisions list.** The `qrels.txt` file holds relevance judgments for the 47 answerable main-set questions only. The 24 time questions, the 13 held-out questions and the 15 routing questions have no lines in it whatsoever. So for 52 of the 105 questions, `Judged@4` — the fraction of the top four results that anyone has actually looked at — is not low. **It is undefined.** No external scorer can check those numbers at all.

Meanwhile the main set's own judged fraction is uncomfortable. `Judged@4` is **0.55** on the best row, the 44-of-47 configuration, after round 3 lifted it from 0.49. On the plain keyword line it is 0.73. The pattern is worth stating in the article because it is counter-intuitive: the better the retrieval, the less of what the reader is shown has ever been read by a human. At 0.55, nearly half of the best row has never been looked at.

### What is droppable, and why saying so is enough

Audit round 4 is droppable because the consequence is already defined and honest: every held-out number is a floor. Extending `qrels.txt` to the other three question sets — the 52 questions with no judgments — is several judging sessions and is author-only. Porting Episode 6's retrieval to Java is a week or more and, per the argument above, buys no reproducibility of a published figure. Building a high-end GPU tier in Java is droppable permanently: with it, the series acquires a model-plumbing subplot that is not what the series is about.

One droppable item deserves a sentence of acknowledgement rather than silence. Neither side has a unit test for any Episode 6 mechanism. The Python `pipeline.py` is 917 lines, `timecorpus.py` is 215 and `timeline.py` is 125, and all of them are untested; the prototype's 71 test functions cover identifier lookup, the llama.cpp wrapper, scoring, story generation, text handling and TREC file formats only. That is the series' own Rule 6 — test the test — going unapplied to the episode that most needs it.

## What rests on your judgment, and nothing here settles it

Five calls are explicitly yours. Two of them can still move an Episode 6 number.

**Audit round 4.** Reserved to you, and the consequence is already written down. The only unforgivable option is publishing 7 of 13 without the word "floor".

**Whether withdrawn records count as history.** This is the one open decision that still moves a published number. The shipped line has half-made it already: `admit_history` ranks everything in the excluded set, withdrawn records included, and question G-052 is answered because of that. If the answer is no, the history stratum drops from 4 of 7 to 3 of 7, and Episode 6's history numbers change.

**Whether the 24 time questions join the frozen set.** Episode 6 is built on them, and they are the one set the plan still lists as undecided.

**The G-033 hop.** Its pointer in `specs/023-explore-graduation/design.html#D-006` is the placeholder `TBD-explore-restore`. The real mapping to spec 024 lives in a document that is not in the question's expected answers. It collapses to a single hop in practice only because the slug `explore-restore` happens to string-match the directory name. The golden note already asks you to confirm you accept that hop.

**G-051 and G-058.** Neither is a retrieval problem. G-051's first hop sits at rank 1 under one saved configuration; adding the scope arms that close the main set pushes it to rank 12, and the record already says that trade stands. G-058 is "born withdrawn": `admit_history` offers its units and they still rank outside the top 50, while the `#risks` collection ranks it first — one fitted trigger away. One is a fusion trade, the other a filter policy. Several passages in both questions' top-four lists are themselves unjudged audit candidates.

## Four question shapes, and finance genuinely offers three of them

The whole decision turns on separating four shapes that the literature mixes together. Here is what each one means.

**Retrieval multi-hop** means the second piece of evidence is not findable from the question alone. You have to retrieve document A, read it, and only then can you work out where B is.

**Numerical multi-step** means all the evidence is already in one place and retrievable in one shot, but the answer needs several arithmetic or logical steps.

**Top-K aggregation** means the answer is a ranked set computed over the whole collection — "the top ten companies exposed to Y" — and it is only right if nothing was missed.

**Composite synthesis** means resolving a set, extracting a judgement from each member, and writing one summary across all of them.

### Finance does not have retrieval multi-hop

Of the eleven commonly cited financial question-answering benchmarks, ten are single-document or have no retrieval corpus at all, and the eleventh is not available.

The reason is in how they were built. FinQA's authors filtered report pages down to those containing at most one table, then had an expert write questions about that page. An annotator who only ever saw one page cannot produce a question with a retrieval dependency across two documents. ConvFinQA and DocFinQA both reuse FinQA's questions, so three of the eleven are the same annotation effort re-cut three ways. TAT-QA hands the model a table plus its surrounding text. FinanceBench ties each entry to one document. FinDER links each question to its own company's annual report. SEC-QA is the sole exception with genuine cross-document questions, and it is built on private commercial data and was never released.

The 2025–26 benchmarks that advertise cross-document financial multi-hop turn out to be parallel retrieval wearing the word. FinReflectKG-MultiHop, built from annual filings, breaks down as **48.7% within a single document, 41.6% across years for the same company, and only 9.7% across companies**. Its own conclusion claims coverage of cross-company scenarios while cross-company is under a tenth of the generated set. Fin-RATE's cross-entity tier fixes the candidate set inside the question itself by naming the industry sector and the fiscal year, so a query rewriter can emit a list of fully specified single-hop queries before any retrieval happens.

One benchmark is worth correcting by name, because it is the one most often cited as evidence for financial multi-hop retrieval. **MultiHiertt's "multi-hop" means across several tables inside one annual report, and that report is supplied with the question.** It is a document-internal table-linking task.

SEC-QA's own complexity taxonomy lists "parallel reference" separately from "multi-hop". That is confirmation from inside the finance literature that this distinction is the right one and not an invention of this report.

Exactly one question template in that whole literature is genuinely hop-dependent: "among these companies, what is metric 2 for the company with the highest metric 1?" You have to retrieve metric 1 for every candidate, rank them, and only then know whose metric 2 to fetch. That is an argmax-then-lookup shape. It needs an aggregate, not a graph edge.

### What finance does have is numerical multi-step, and that is a reasoning problem

Where the corpus is one document, retrieval is at or near its ceiling and the reasoning is the whole bottleneck.

On FinQA, the retriever reaches **recall@3 of 89.66%** — that is a retrieval metric, measuring whether the supporting facts were found. The best system then scores **61.24 on execution accuracy**, which is an answer metric measuring whether the computed result matched the gold result. Human experts score 91.16 on the same answer metric. The authors sampled 50 errors and found **about 15% came from the retriever**; roughly half the rest came from missing financial knowledge and the other half mainly from numerical reasoning.

MultiHiertt is starker. Its published error analysis over 100 sampled test errors contains **no retrieval category at all**. The categories are: wrong operand or span 43%, missing operand 21%, wrong program 19%, lack of domain knowledge 4%. Two caveats belong with those numbers. The categories sum to 87%, and the remaining 13% is unexplained in the paper. And these are reasoning-time errors over contexts that were *given* to the model, so they are not evidence about extracting facts from a document into a table — treating them that way is an inference, not the paper's claim.

The difficulty gradient runs with arithmetic depth, not retrieval depth. FinQA scores 67.61 on one-step questions, 59.08 on two-step, and **22.78 on anything above two steps**. MultiHiertt's answer-set exact match falls from 43.62 at one step to 15.14 above three steps.

The implication for the series is direct. If you move to finance in order to test *retrieval*, these benchmarks cannot serve you. They would measure the reader, not the retriever.

### Top-K aggregation fails by construction, and raising k makes it worse

Two papers state the by-construction argument in print. AGGBench — a February 2026 benchmark of aggregation queries over a document collection — formalises the task as corpus-bounded with a strict completeness requirement: the system must retrieve *every* matching entity, not a relevant one. Its abstract says plainly that existing approaches, both text-to-SQL and retrieval-augmented generation, fail to achieve that completeness. The TAG paper (Table-Augmented Generation, CIDR 2025) makes the complementary point about retrieval: it is limited to relevance-based point lookups over a few records, and leaves counting, arithmetic and filtering to a single call to an error-prone language model.

The decisive measurement is AGGBench's sweep over how many passages are retrieved. For plain retrieval-augmented generation, going from the top 5 to the top 50 passages moved chunk-level recall from **0.042 to 0.072** — a retrieval metric, barely moving — while the normalised count error in the answers rose from **0.463 to 1.855**, roughly four times worse. That is an answer-set metric, and lower is better. More retrieval, worse aggregate.

Two features of that benchmark's corpus explain why no single cutoff can work. Relevant evidence is sparse and the sparsity varies: 294 of 4,755 chunks are relevant in the base corpus (6.18%), falling to 178 of 16,294 (1.09%) in the expanded one. Note an inconsistency in the paper itself — its introduction states a minimum density of 0.85%, which conflicts with the 1.09% in the body. And the answers are large: **165 of its 362 queries have more than five answers**, with maxima of 20 and 29. A fixed top-4 or top-10 cannot return those sets even with perfect ranking.

**Two widely quoted numbers in this area are wrong, and both would be easy to repeat in print.**

The first concerns TAG-Bench. Its results table is often cited as showing that retrieval-augmented generation scores zero on aggregation. **It does not. The aggregation column reads "N/A" for all five methods tested.** The paper did not score that category quantitatively and offers only qualitative discussion of those queries. The zeros that are real are in the overall, match, comparison and ranking columns. The nearest scored proxy for aggregation is the ranking column, where plain retrieval scores 0.00, text-to-SQL scores 0.10, and a hand-written TAG pipeline scores 0.40.

The second concerns the text-to-SQL benchmarks. It is commonly asserted that aggregate queries are the weakest category on BIRD or Spider 2.0. **No public source breaks either benchmark's failures down by SQL clause.** A targeted search found none. The honest statement is that the benchmarks contain aggregation and nobody publishes the per-clause split. What *is* verifiable is the difficulty jump between benchmark generations: the same code-agent system built on o1-preview solves **21.3% of Spider 2.0, 91.2% of Spider 1.0 and 73.0% of BIRD**, all measured as execution accuracy.

### Composite synthesis is the one regime where graph retrieval has a real claim

GraphRAG's own paper frames corpus-wide questions as query-focused summarisation rather than retrieval, and that framing is honest. This does not contradict the earlier conclusion about spectastic. It confirms the boundary GraphRAG's authors themselves draw, with factoid lookup on the other side of it.

But the measuring instrument comes attached to the claim, and it is weak. GraphRAG's reported win rests on **125 machine-generated questions per corpus, judged pairwise by a language model on comprehensiveness and diversity, with no gold answers anywhere**. Its only piece of non-preference evidence is that the global configuration produced **31 to 34 claims per response against 25 to 26** for plain vector retrieval. That is more claims, not more correct claims.

For this series that matters more than usual. Adopting that instrument would mean swapping a falsifiable metric for an unfalsifiable one at exactly the stage where it matters most. "More comprehensive" and "more claims" are both perfectly compatible with "more confidently wrong".

### Where the notes disagree, and how to read it

One of the research notes recommends moving *away* from finance, toward a scoped subset of US case law with a hand-built layer of how later courts treated each precedent.

That recommendation is sound for the question it was asked. It was briefed on the multi-hop and latent-relation question only, before aggregation and consensus were on the table. Its finding stands on its own terms: finance's relation families are mostly declared or derivable from identifiers, and the one genuinely hard obstacle in finance is entity resolution — matching names to entities — rather than relation inference.

The aggregation case for finance is a separate argument, and it is stronger. It rests on a question class with no published measurement at all, rather than on a relation type with a measured null result.

Both can be true at once, and the resolution is simple. **Do not go to finance for latent relations. Go for aggregation, as-of aggregation and set completeness.**

## What finance buys, what it costs, and three traps that corrupt a top-K silently

### The head start is bigger than the brief assumed in one place and nil in another

Start with a correction that changes the size of the head start in both directions.

**The knowledge-base schema is not on a side branch.** All four schema commits sit on the `episode-3` line that `episode-5` descends from, and the files are in your working tree now. Comparing the two branches on that directory produces an empty diff. There is nothing to merge and nothing to port. The only thing unique to the `finance-corpus` branch is the Python fetcher: 12 files, 1,209 lines, with no Java coupling at all.

Both executable pieces still work. The schema's end-to-end test ran **84 of its 93 checks with zero failures**. The nine that did not run are the YAML cross-checks, skipped because PyYAML is not installed in the environment where it was tested; they verify that the link-type catalogue and the consequence table match what is seeded into the SQL. The trace is otherwise byte-identical to the committed version across a jump from SQLite 3.45.1 to 3.53.4. The fetcher passes all 24 of its tests on Python 3.14.7, and it uses only the standard library.

**A live fetch did happen, and its record survives — but the documents do not.** On 14 October the fetcher made **98 requests, every one returning HTTP 200, totalling 178,892,093 bytes (about 170 MiB), in 17.5 minutes.** It collected 24 filings across 15 companies plus 22 regulatory documents. Two items in that haul are especially useful: `ipoe-402-2021`, the Item 4.02 non-reliance notice that the entire 8-K profile was written around, and **five point-in-time snapshots of the same regulation, 17 CFR 240.15c6-1, at four different dates**. One item failed — the 2022 DTCC settlement playbook, throttled by the Internet Archive — and it happens to be the target of the only hand-authored supersession link in the repository.

The catch is that the document bytes were never retained. The raw directory is deliberately git-ignored, and the sibling private repository that holds the manifest has no working tree checked out. The manifest is reproducible — every row carries a SHA-256 — but re-materialising the corpus means re-running against EDGAR. Budget one clean eighteen-minute run plus the DTCC retry, not zero.

### The strongest asset is the three-clock design

This is the sharpest argument for finance that the artefacts support, so it is worth being precise about.

The schema's `version` table carries three distinct kinds of time. `valid_from` and `valid_to` say when something holds in the world. `recorded_from` and `recorded_to` say when this knowledge base recorded it. `published_at` says when the source itself published, sent or accepted it. A `clock` table freezes both axes so answers do not flip mid-run, and asking "as known at time t" is just the same views with the recorded clock set to t. The predicate for "only what had been filed by then" already exists in the link resolver.

That design has already caught a real bug, and the finance corpus is what caught it. The v1.1 changelog records that the CURRENT view checked only the upper valid-time bound, so a rule that had been adopted but was not yet in force showed up as current. CURRENT now checks both valid-time bounds and the knowledge-time bounds against the frozen clock.

On institutional holdings data this distinction is unavoidable and consequential. A Form 13F position is *true* at the quarter end but only becomes *knowable* up to 45 days later, because that is the filing deadline. So a reader asking "who held company Y as at the second quarter" on 1 August must get a different, correct, smaller answer than a reader asking the same question on 1 September. **No spectastic question makes that distinction bite.** The schema's own README concedes that only 9% of live spectastic elements carry their own date, and Episode 6 found commit timestamps so unreliable that it keyed the corpus on git first-parent position instead — 68 commits share a timestamp, in 16 groups.

Moving to finance would also pay off a defect Episode 6 recorded and could not fix with one time axis. No spectastic edge carries a validity interval, and the consequence is measurable: of the 7,956 distinct unit-to-unit edges in the snapshot, only **27.6% had both endpoints in force at position 300 of 721**, with 386 edges having a near end in force and a far end absent. A holdings row with its own validity interval is a validity-bearing edge by construction.

### The gap is one table, and it is the whole of the work

The schema models documents, units, texts, typed relations and judgments. It does not model a measured fact extracted from inside a filing.

Four candidate tables were checked and all four fail. `facet` holds document-scoped string tags; it can say "this document is furnished", not "this manager held 412,000 shares worth $18.3m at 30 June". `claim` holds a sentence with one subject and no object, so a manager-to-issuer pair has nowhere to go. `entity_link` is the one entity-to-entity table and it fails three ways: it has no measure column, its `type` is a closed five-value constraint with no holding relation in it, and its primary key allows exactly one row per entity pair per type — so the same manager-to-issuer pair cannot carry a first-quarter row and a second-quarter row. On `link`, the only floating-point column is `confidence`, bounded between 0 and 1, and the consequence table reads it purely as belief: how sure we are the relation exists, never how much of anything it represents.

The summary statistic is blunt. **There is no `sum()` and no `avg()` anywhere in 95,313 bytes of SQL.** Every aggregate that does appear is structural bookkeeping over dates, row counts and label strings.

Nor is there a profile for any of the three filing families in question. The strings `13F`, `13D`, `13G` and `CUSIP` appear nowhere on the branch — not in the schema, not in a profile, not in the fetcher's configuration. The existing SEC profiles match only annual and current reports, so a Form 13F arriving at the ingester would match no profile and fall into the quarantine queue, which is what the design intends but is not what you want. And those profiles are specifications rather than code: four reviewed designs, zero test fixtures (the `fixtures/` directory does not exist although every profile ends with a line pointing at it), three named Java processor beans that are unimplemented, and a cited research file, `official_relationships.md`, that does not exist anywhere in the repository or the vault.

There is one genuinely encouraging precedent, and it is precisely bounded. The ACME-03 test query is a real relational entity query: a set-building common table expression over aliases and entity links, a join to validity-bounded company identifiers, a valid-time test against the frozen clock, a correlated string aggregation for the provenance path, and an ordering. **Three of the four ingredients of "group by issuer, order by total value descending, limit 10" are already demonstrated. The fourth has no column behind it.**

Adding a holdings table is about fifteen lines of DDL in the schema's own extension idiom, referencing tables that already exist. But note one thing before presenting it as a proof of anything. The schema's own stated test for being generic is "a second, very different source going through an adapter *with no change to the tables*". **A holdings pack changes the tables.** That is worth knowing in advance.

### The evaluation head start is nil, and that is the largest single gap

There are no finance questions, no relevance judgments, no runs and no baseline.

The six ACME questions are marked as candidates and run over hand-seeded synthetic rows, not over any fetched filing. They are worth something precise but narrow: a worked specification of what an "as known at a date" finance question looks like, complete with expected answers tagged as answer, conflict or decoy. They are a template, not a set. The `golden/finance/` directory is marked "(to come)" and does not exist. The ground-truth ledger has **six rows** in `facts.csv`, and `events.csv` is a header line with nothing under it. No synthetic document has been written.

Compare Episode 6, which has 24 time questions, saved runs across four model configurations, and `qrels.txt` adjudication.

There is, though, a cheap first question set hiding in that thin material. The ledger's row for the move to T+2 settlement carries a `known_from` of 22 March 2017 against a `valid_from` of 5 September 2017 — a 167-day gap between when a fact became knowable and when it became true. Combined with the five point-in-time snapshots of one regulation, that is enough to write a genuine valid-time and knowledge-time question set with no synthetic layer at all.

### Three traps would corrupt a top-K answer silently

These three matter more than the usual data-quality grumbles, because each one produces a confidently precise wrong number. No retrieval metric catches them, and execution accuracy does not catch them either, because the query executes perfectly against the wrong set of rows.

**Trap one: amendments.** Form 13F's instructions require an amendment either to restate the entire report, or to include only entries that are new relative to the current public report. Those two cases need opposite handling: a restatement must *replace* the original, an add-new-entries amendment must be *unioned* with it. Get it backwards and you either drop positions or double-count them. The discriminator is a checkbox on the cover page, not the form type. Vendor practice confirms de-duplication to the most recent filing per manager-quarter is mandatory. And amendments are not automatically better: Anderson and Brockman found that amended reports are *sometimes less accurate than the originals*. There is a second-order version of this trap too. A naive "latest filing wins" rule silently converts an as-of question into a best-known-now question, which is a different question with a different right answer.

**Trap two: duplicative reporting is designed into the form.** If two managers share investment discretion over the same securities, only one of them must report those securities, and the other must identify who is reporting on its behalf. There are three report types and only one is self-contained. A notice report omits the holdings table entirely. A holdings report contains everything. A **combination report** is the dangerous middle case, where only part of the holdings is reported elsewhere, and it contains both a list of other managers and a holdings table. Column 7 of that table is a shared-discretion pointer, not a split of the quantity. Both symmetric errors are real: dropping rows because their security identifiers match removes genuine reported quantities, while expanding each row into one record per linked manager and summing those counts the same block repeatedly. Two caveats. **Combination reports are not addressed in any source the research found** — not in the one open-source implementation, not in any paper. This is a live unknown. And the only analytical treatment of the double-counting problem located anywhere is a vendor blog post, not peer-reviewed or regulatory work.

**Trap three: one manager, several filing entities.** "The top ten companies exposed to Y", ranked by manager, is wrong if a single asset manager files under multiple company identifiers. This is the most under-documented gotcha in the available prior art: the one documented implementation describes only the joins from filing to company identifier and filing to manager name, and says nothing about consolidating one manager across several identifiers.

That third trap is the same problem the latent-relation research measured from another angle, and the numbers there are sobering. The PUDL project's pipeline for extracting subsidiary lists from annual-report exhibits matches **about 2% of unique extracted subsidiaries to an SEC company identifier, covering 19% of filers**, at an average of about 3.5 subsidiary identifiers per company identifier. The reason is simply that the exhibit reports no identifiers, so the match has to be made on exact name plus jurisdiction. Matching those subsidiaries onward to an external utility registry on shared names covers about **0.04%**. The approach is deliberately biased toward precision, so most subsidiaries stay unmatched by design. One more caveat on that pipeline: its extraction model was fine-tuned on 160 labelled documents and validated against 72 manually transcribed tables, reaching 95% accuracy — but precision, recall and F1 are not reported, and filing years 2018 to 2022 were not extracted at all.

The general lesson is worth naming explicitly in print. **Entity resolution looks like multi-hop retrieval, because the symptom is identical: the system cannot connect these two documents.** The diagnosis is different, and a graph does not fix it. A language-model extractor that mints one node per surface form makes it worse.

### Beyond the traps, 13F is structurally incomplete, and that is citable rather than merely suspected

Five limits are written into the rules themselves, which means they can be stated as facts rather than as worries.

The form covers long positions in US-listed equity only; shorts, cash, bonds and non-US holdings are excluded by rule. Filings arrive up to 45 days after the quarter end. Only managers holding at least $100,000,000 must file at all, so for small holders absence is not evidence. There is a de-minimis allowance: a manager may omit a holding of fewer than 10,000 shares *and* under $200,000 in value. And holdings granted confidential treatment are simply absent, with no placeholder marking the gap, so a filer's reported total can be understated.

Two external findings sharpen the picture. An SEC Inspector General review found that **no SEC division or office monitors Form 13F filings for accuracy and completeness**, so errors surface only through confidential-treatment processing or outside reports. That report is cited by URL and was not fetched. And the cleaned-vendor cautionary tale is directly on point: Backus, Conlon and Sinkinson abandoned the standard commercial dataset and rebuilt from original filings, after finding a run of quarters beginning in 2011 where over 10% of S&P 500 firms showed very few reported owners. One caveat on that: the page hosting their account returned HTTP 410 Gone on 29 October, so those quotes come from search extracts rather than a successful fetch, and should be re-verified before print.

There is one unusually cheap counterweight, and it appears to be unused in the literature. The form requires the filer to state its own control totals: the number of line entries in the holdings table, and the aggregate value of all of them. So any aggregate built from the table can be reconciled against the filer's own stated figures with no human judgement involved. No published evaluation does this. Adopting it would be a design proposal, not established practice.

### Licensing: one hard blocker with a documented detour, and one clean refusal

**The security identifier used throughout 13F — the CUSIP — is proprietary, and storing it is the licensable act.** The CUSIP Global Services FAQ states that a licence is required when an end-user's database containing CUSIP data is updated, stored or maintained, while display-only access through an authorised distributor does not require one. There is a fee waiver for end-users maintaining fewer than 500 securities. An academic or research profile is a discretionary factor in fee decisions, not an exemption. Three caveats, all of which matter: that FAQ is dated **4 December 2015**, no policy specific to academic research current as of October 2026 was found, and a 2022 class action challenging vendor-channel restrictions is unresolved. There is also a counterweight in the provider's own regulatory comment, saying it does not charge for use limited to regulatory reporting and that identifiers are available without licence from certain public sources.

The documented detour is to never materialise a mapping from CUSIP to ticker, and instead resolve identifiers per call through OpenFIGI, which is openly licensed. For the series the practical rule follows: the CUSIP column is the one field a published demo corpus cannot redistribute.

On the supply-chain side the licence position decides the matter cleanly in the other direction. The two established commercial relationship datasets are institution-licensed and terminal-licensed respectively, and one has been litigated over redistribution rights. **A public blog series cannot ship either.** Self-built extraction over a pre-packaged filings corpus is the only publishable route, and it comes with no precision number attached.

On benchmarks, the only public finance dataset with page-level retrieval gold over an open-domain corpus is **LOFin**, which covers 145,897 SEC reports, and its licence is not stated in the paper. FinanceBench's public subset is 150 rows under a non-commercial licence, and its PDFs are not redistributable from the dataset at all.

## Measurement has to change, and some things must be refused rather than scored

### Keep four families of metric apart, and say which is which every time

This discipline matters more here than anywhere else in the series, because the literature mixes them constantly.

**Retrieval metrics** — Hit@4, recall@k, nDCG, page recall — measure whether the evidence was found. **Extraction metrics** — precision, recall and F1 over relations or labels — measure whether the fact was read correctly out of the document. **Execution accuracy** measures whether the computed result set equals the gold result set. **Answer and judge metrics** — set F1, exact match, nugget coverage, language-model preference, citation precision — measure what the user actually sees.

One worked example of why the discipline pays. In the finance literature, several papers report RAGAS "context recall" and "context precision" next to real retrieval numbers. Those RAGAS figures are language-model-judged estimates computed without any gold passage labels. They are answer-judge metrics, not retrieval metrics, and they must never be printed beside recall@k as if the two were comparable.

### Your current scorecard cannot detect the new failure class, and neither can the schema's tables

Hit@4 asks whether a relevant passage appeared in the top four. Its maximum is achieved by returning *one* good passage. An aggregation question's correctness is a property of the complete set. So Hit@4 is not a weak proxy here — it is measuring a different quantity entirely.

The schema's evaluation tables cannot express the answer either. The `expected` table is keyed on unit identifiers. The `result` table records the rank of the first expected unit, the rank by which every expected unit was retrieved, whether stale or poisoned passages appeared in the top results, whether an injection was followed, and whether anything leaked. **Every one of those six columns is a rank over retrieved passages.** There is no column for a correct ordered list of entities, no column for a numeric answer, and no column for a tolerance.

So a top-K episode needs a new scoring artefact *as well as* a new question set. That is a second, independent piece of missing work, and it is the kind this series takes seriously, because the ratchet is the method.

### For ranked sets, the precedent already exists in TREC

The reusable mechanic comes from the TREC Entity track, and it is almost exactly what a holdings question needs. That track adapted the relevance-judgment file format for entity lists by adding an equivalence-class column. Its format is topic, document, name, relevance, class, name-relevance, and lines sharing a topic and a class refer to the same entity however it is spelled. The track's own explanation is that entity resolution is addressed at evaluation time rather than at indexing time.

That is precisely the holdings problem: one company appears under several security identifiers, several issuer-name spellings as the filer typed them, and several company identifiers. The format is a strict superset of your existing `question 0 passage relevance` line. It adds one column and leaves every existing line valid.

For the set measure, use the TREC 2004 list-question definition unchanged: instance precision is correct distinct responses over total responses, instance recall is correct distinct responses over **known** instances, and the score is their harmonic mean. The word "known" is the honesty device. Recall is measured against what is known, and the metric's own name does not overclaim. For scale, that task averaged 8.8 answers per question with a maximum of 41, and the track measured its own error rate by comparing runs with identical list components: the largest difference in average score was 0.006.

For the order measure, use rank-biased overlap rather than Kendall's tau, for one concrete reason. **Holdings values tie, and rank-biased overlap has a published tie rule while plain Kendall's tau and plain AP correlation do not.** Set its parameter to 0.9, which the paper states puts 86% of the evaluation weight in the first ten ranks — exactly the K in "top ten companies". And always report its residual alongside the score, never the point estimate alone. The paper gives the reason: on short lists the base score becomes a measure of list length rather than of difference. Two identical seven-item lists at that parameter score only **0.767**.

One finding should discourage any temptation to agonise over metric choice. The Entity track checked how much the choice mattered and found Kendall tau correlations of **0.92 to 0.94** between NDCG@R, mean average precision and R-precision. Which ranked measure you pick matters far less than whether the gold set is complete. Spend the effort on completeness.

### Numeric tolerance has to be your own stated rule

**A widely repeated claim about FinQA is wrong, and it would be easy to repeat in print.** FinQA is commonly said to allow roughly a 1% relative tolerance on numeric answers. Its released evaluation code does no such thing. It compares with exact equality, and the only precision handling anywhere in the path is rounding the predicted result to five decimal places; the gold answer is compared as stored. Two 2026 preprints attribute conflicting tolerance rules to FinQA, and neither matches the released code. **Treat the 1% figure as unverified folklore.**

TAT-QA is the better precedent for a different reason: it makes *scale* a separately scored prediction target rather than folding it into a tolerance. A correct numeric answer must carry the correct scale — none, thousands, millions, billions or percent. That matters enormously for holdings data, where confusing thousands with dollars is the commonest error and is a fundamentally different fault from returning the wrong set of rows.

Execution accuracy has a published blind spot worth importing deliberately. A query that returns the right rows for the wrong reason counts as correct, and the fix is to execute against multiple perturbed databases. Measured against that stricter test, Spider's standard metric had a **2.5% false-negative rate on average and 8.1% at worst**. The cheap version for a single corpus is to run each top-K question against a second quarter's filings and require both to pass. If the set is right in one quarter and wrong in the next in a way the gold set does not explain, the first result was probably right-rows-wrong-reason.

### For set completeness, the published evidence is bad, and the diagnosis is specific

This is the hardest empirical finding in the whole brief, and it lands squarely on the "find all companies with exposure to Z" step.

Total Recall QA is a 2026 benchmark built on the premise that answering correctly requires retrieving *all* relevant documents. Its numbers are brutal. Agentic systems — the ones that plan their own searches — reach cumulative intermediate recall of **2.69, 2.63 and 13.10 across its three datasets** for a ReAct-style agent. Best end-to-end exact match is **11.24, 6.12 and 2.55**.

Perfect retrieval does not rescue it. With oracle retrieval, the strongest model tested reached exact match of **56.80, 27.11 and 5.56**, against closed-book scores of 30.18, 10.33 and 1.67. And **90.9% to 95.9% of the errors that remained under oracle retrieval were reasoning errors**, not gaps in the model's knowledge.

The diagnosis is what makes this useful rather than merely discouraging. The agents' sub-query counts stayed roughly constant regardless of how many gold entities a question had. As they issued more sub-queries they mainly retrieved distracting entities. **The failure is query planning, not ranking.** That is the one place where a materialised edge table genuinely helps, because it converts "issue the right sub-queries" into "traverse". It does not make global graph summarisation the answer. And the hard part stays extraction, not the data structure.

One negative result to state plainly: **no set-completeness benchmark exists over financial filings.** Everything published is over Wikipedia-derived or e-commerce data. The series would be the first to measure it on a finance corpus, which is an opportunity and a caveat at once.

### For synthesised answers, nuggets preserve the discipline you already have

Nugget evaluation is the only option here that keeps the series' existing contract intact. A nugget is defined by its *binary judgeability* — it is atomic if an assessor can make a yes-or-no decision about whether it appears in a response. That is the same contract as a relevance judgment on one passage. A scalar judge score is not auditable; a per-nugget decision is.

The current machinery is the TREC retrieval-augmented generation track's AutoNuggetizer. Its primary metric, `V_strict`, is the fraction of *vital* nuggets that the answer supports, where partial support scores zero. Its agreement with human nugget evaluation is **a Kendall tau of 0.887 at the run level**, which is strong — but only **0.360 to 0.539 per topic when fully automatic**. Read that correctly. An automatic nugget score is defensible for comparing two system configurations across a frozen question set. It is not defensible for claiming anything about a single question.

For groundedness, use FActScore's formula with its abstention handling, rather than a library metric of the same shape. FActScore computes the fraction of a response's atomic facts that are supported by the knowledge source, and conditions the model-level score on the model having actually answered rather than abstained. Its automatic estimator's error against human-computed scores was **1.4%, 0.4% and 0.8%** across three systems — under 2%. The widely used RAGAS faithfulness metric has the same shape but, by its own documentation's admission, publishes no human-agreement figure at all. For a series with a ratchet of absolute rules, that is a reason to cite the formula and not the library default.

### What must be refused rather than scored

Four refusals, each with a reason that can be printed.

**No composite score across stages.** There is no validated error-propagation bound for a multi-stage retrieval pipeline. The only candidate located is a single unverified theory preprint describing an adversarial worst case, which its own framing concedes real systems may beat. The multiplicative model — multiply the per-stage success rates — is folklore-grade, sourced to a practitioner blog post, and is an upper bound anyway because the stages are not independent. "We cannot bound this" is the defensible publishable position, and the right alternative is conditioning: report the final-stage score *on the subset where the first stage met a stated recall threshold*, and report the first-stage recall separately.

**No correctness claim about a synthesised consensus.** There is no gold consensus, so exact match and set F1 do not apply. The publishable claims are coverage and groundedness, and both are claims about the answer's relationship to the evidence, not about the world.

**Do not use the word "consensus".** In finance it names a specific structured product: a filtered average of numeric analyst estimates, with documented rules about accounting basis, timing and analyst selection. The main provider includes only estimates prepared on the accounting basis used by the majority of contributing analysts, and excludes and footnotes the rest; the panel spans more than 30,000 analysts at more than 3,000 brokerage firms. A second provider runs several *different* consensus constructions, which is itself the point. What the pipeline would produce is a prose synthesis of company self-descriptions — a different object, from different authors, with no estimate distribution behind it. "A synthesised summary of what each exposed company says about its own outlook" keeps the demo and drops the false claim. Reserving the real word also leaves room to contrast the two, which is a better post than conflating them.

**No bare language-model-judge number.** If a judge is used at all, swap the answer positions and publish the consistency rate next to the verdict. GPT-4's own consistency when the two answers are swapped was **65.0%**, and its agreement with humans falls from 85% to **66%** once ties and inconsistent votes are counted. The human-to-human ceiling on the same task is **81%**. An inconsistency rate of that size is larger than most differences a blog post would be trying to report.

### Three things nobody has measured, which are the publishable gaps

These are negative results, and they deserve the same prominence as the positive findings, because they are what the series could close.

**Nobody has measured whether complete-set retrieval is achievable with dense retrieval at all** — for instance by thresholding similarity instead of fixing a cutoff. The question appears to be open.

**Nobody scores an aggregate answer's supporting rows.** The provenance literature that exists is database theory about how to compute provenance for aggregate queries. The question-answering literature scores only final answers, or attribution for prose claims. There is also no evaluation of faithfulness for an aggregate as distinct from a passage-grounded answer, and no documented convention for whether such an answer should cite the rows, the source filings or a summary. Attribution for genuinely aggregative claims is worse than hard — it is ill-posed under the standard formulation, because no single retrieved passage entails a statement like "most exposed companies are cautious".

**Nobody has measured whether a synthesis stage increases confident wrongness** relative to the same evidence shown un-synthesised. This is the single most publishable gap the series is positioned to close, and the ingredients are all in hand. Models over-answer at rates of **65% to 91%** under conflicting evidence. Self-reported confidence is nearly useless for detecting it: trained readouts from output confidence reach only **0.54 to 0.67 AUROC** on whether an answer attempt was even admissible, while readouts from hidden states reach 0.97 to 0.99 on the same task. And graph-based global summarisation produces more claims per response at unchanged per-claim reliability, which arithmetically means more wrong claims per answer. Reporting a confidently-wrong rate per claim *and* per answer, before and after adding a synthesis stage, is cheap, novel, and exactly this series' method.

## The alternatives, fairly: case law is the only measured latent relation

If the goal were latent relations rather than aggregation, finance would be the wrong corpus and US case law would be the right one. It is worth stating why, because the recommendation exists in the notes and should not be dismissed by omission.

The relation in question is **treatment**: whether a later court followed, distinguished, limited or overruled an earlier decision. The citation itself is authored, so the candidate set is cheap and reliable. The treatment is in the prose. And it is the only candidate anywhere in the survey where latency is independently *measured* rather than merely plausible: the best models reach **79.1% on a coarse treatment classification and 67.7% on a fine-grained one**, against an expert-annotated set of 239 real citations yielding 329 high-confidence citing relationships. So current models get it wrong roughly one time in five on the coarse scheme and one time in three on the fine one. A related corpus, CLERC, gives an external anchor at **48.3% recall@1000** — a retrieval metric — so hand-built numbers would sit against a real baseline rather than float free.

Two caveats are load-bearing. **The Caselaw Access Project's own licence could not be retrieved**; its about and docs pages returned title-only content, and the CC0 designation appears only on a third-party mirror. What is confirmed is that the commercial restriction expired on 1 March 2024, releasing nearly 7 million US state and federal decisions. And the obvious UK alternative is closed outright: **the Open Justice Licence v2.0 permits copying and republishing but excludes computational analysis, including indexing by search engines, which is the whole project.**

The other candidates fail cleanly, and briefly.

UK legislation is the best bitemporal corpus available at any size — both time axes explicit at section level, permanent URIs for sections, and a named open licence. But its amendment relations are *editorial data* with in-force dates attached, maintained by professionals. Using it for a latent-relation episode would be the spectastic null result in a new costume. Using it for a bitemporality episode would be excellent.

Patents, Wikipedia-derived question sets and open-source issue trackers all have authored cross-reference layers — citation lists, hyperlinks, `Fixes #123` trailers — and therefore fall inside the authored-link null result already established.

Incident reports have the cleanest statement of the thesis found anywhere. A study over roughly 140,000 NASA aviation-safety narratives states explicitly that **the structured fields do not cover why the incident took place**, so the work has to rely on free text. But the largest annotated causal set located is **151 relations over four documents**, there is no passage-level gold anywhere, and the richest corpora are internal and not redistributable.

The sharpest structural finding across the whole survey is worth stating on its own. **The corpora that have usable relevance judgments and the corpora that have genuinely latent relations are disjoint sets.** NFCorpus (3,600 documents, 38.2 relevant per query), SciFact (5,000 documents) and ArguAna (8,670 documents) fit the scale almost perfectly and have real judgments — and none of them has cross-document relational structure worth expanding. Picking one of those because the metrics would be easy would produce a third null result in a row, and this time a self-inflicted one.

## Defects worth fixing whichever way you go

A handful of items are actionable regardless of the decision, and none is expensive.

The shipped golden-set README is now the most stale consequential document in the repository. It describes rounds 1 and 2 only, states "291 judgments" against a file that is 345 lines long, has no row for the routing question file, and says the time questions have never been run against a retriever — which Episode 6 is built on.

**Episode 2's frontmatter says `publish: true`**, which contradicts the plan's "only Episode 1 is live". Check it before Episode 6 ships, because it decides whether a correction is a fix or an erratum.

The plan's word counts are a couple of days stale. Episode 4 is 7,604 words, not 7,532; Episode 5 is 7,527, not 7,082; Episode 6 is 6,639, not 6,589. So Episode 4 is still the longest but by 77 words rather than 450, and the trim case is weaker than the plan states. That one is your call and the draft was not touched.

The 10,540-versus-11,492 discrepancy is a vocabulary bug, not a measurement error. There are 10,540 *addresses* and 11,492 *items* after units over 250 words are split into parts. The plan calls 10,540 wrong without that distinction, and the Java test's name says "units" when it means addresses. One sentence and a test rename.

`Timeline._slices` has no eviction. There is no `episode-5-refactored` branch to match the ones for Episodes 2, 3 and 4 — a convention gap rather than a correctness risk, since the ratchet tests assert the article's constants directly and would fail loudly rather than drift quietly. And Episode 6's own *Epilogue* section and the planned Episode 7 both hold the rejected arms, so someone has to decide which lives where before the epilogue is drafted, or the two will contradict each other.

On the finance side: `official_relationships.md` is cited by both SEC profiles and does not exist, though the research itself survives in the vault's *Corpus Ingester* note. The `fixtures/` directory does not exist although every profile ends with a line pointing into it. And the 2022 DTCC playbook, the target of the repository's only hand-authored supersession link, was never successfully fetched.

Two further items are honest caveats rather than defects, and both should survive into print.

`moved_in`'s own docstring records that `changed_between` admits any proposal that *narrates* a change, which makes the admitted set four to ten times larger than the set that actually moved — **3,598 against 517 on question G-070**. It also cannot detect a repeal, so it is strictly a substitution detector. The docstring says this plainly, and adds the reason it matters: the question set holds no repeal question, so nothing in the measurements would have told us.

And the time set's standing is itself a published finding worth keeping rather than softening. **The ratchet rejects Episode 5's own line on that set.** One configuration fails against the Episode 5 baseline on both Hit@4 and all-evidence; another fails on stale passages in the top four, rising from 4 to 6. Question G-057 is the spine of it, and the irony is the point: the supersession redirect — Episode 4's hero fix — is exactly what takes G-057 out.

## An ordered plan for the next episodes

### Episode 7 — the epilogue, as planned

It is already specified in five bullets and has no draft, but most of its raw material exists. Section-kind choosing failed five ways. Pinning failed three times; one of those attempts "fixed the one question predicted and broke eighteen". A declining mechanism was measured for and deliberately not built, because a threshold fitted on either available question set disagrees with the other. The question sets are too small, and this is where to say so with the arithmetic. And the routing set is evidence that confident wrongness returns on unfamiliar questions.

Add one bullet from this report: the three-layer unjudged inventory, with the fact that **52 of the 105 questions have no relevance judgments at all**, so the judged fraction for them is undefined rather than low.

Write this before any finance work starts. It is the piece that makes the spectastic numbers legible, and the material is freshest now.

### Episode 8 — the structured layer: a holdings table, an as-of top-K, and a new scoring artefact

This is the episode the existing assets actually support.

Write a Form 13F source profile and a holdings pack — roughly manager, issuer, security identifier, period, value, shares and share type, plus the three time columns the schema already uses elsewhere. Say plainly in the article that this changes the tables, and therefore tests the schema's genericity claim harder than the README's own version of that claim allows.

Build from the SEC's structured tab-separated data sets — the submission, cover-page and holdings-table files — rather than from PDFs. That moves the extraction risk away from optical character recognition and table-structure recovery, and toward *semantic* reconciliation, which is where the three traps live.

Handle the three traps explicitly and in the text, because they are the episode's real content. The restate-versus-add-entries flag on amendments. Combination reports and the shared-discretion column, where the published record has a genuine hole rather than a solved answer. And consolidating one manager across several filing identifiers. Never materialise a mapping from security identifier to ticker.

Make the headline claim the as-of one, because that is the thing no published work addresses. **"Who held company Y as at the second quarter of 2026" has two correct answers**: the best-known-now answer, and the as-reported-at-the-time answer. A bitemporal holdings table answers both. A single-version table answers neither reliably. The research found nothing at all on as-of aggregation in a language-model, text-to-SQL or retrieval setting: no benchmark includes such questions, no paper evaluates whether a model generates temporally correct SQL against a bitemporal schema, and no documented practice exists for expressing "as known at date D" in a natural-language interface.

Reconcile every computed aggregate against the filer's own stated entry and value totals, and call that a completeness *audit*, not a completeness *proof*.

Ship the extended gold file alongside it: set F1 over entity equivalence classes as the primary number, rank-biased overlap with its residual as the secondary, and an adjudicated-fraction honesty number saying how much of the returned answer the gold set was able to rule on at all. Report those as separate numbers and refuse to blend them, because a weighted blend of membership and order would hide exactly the distinction that matters — nine of ten companies right in the wrong order scores well on the set measure and poorly on the order measure, and both facts are worth knowing. State per question whether the gold is complete, pooled to a depth, or partial, with the de-minimis allowance and the $100,000,000 filing threshold as the citable reasons completeness is sometimes unavailable. The existing relevance judgments and the Hit@4 ratchet stay untouched; this is a second artefact scored by a second scorer.

One sequencing point inside the episode. Measure extraction fidelity against a hand-audited sample of filings *first*, using a table-structure metric rather than any question-answering metric. The expected failure mode is a wrong number inside a correctly executed query, which is the worst failure available, because it is confidently and precisely wrong and nothing in the existing scorecard catches it.

### Episode 9 — set completeness, and the operational-exposure question

"All companies with exposure to Z" cannot be answered by retrieving Z's documents at all. The disclosure obligation sits on the *dependent* company — the one with the concentration risk — not on Z. Z's own filings may never mention the dependent company. That asymmetry is what makes the step a recall problem rather than a lookup, and it is the first genuinely undeclared, directional, asymmetric relation the series has found in finance.

Two warnings have to be carried into print rather than smoothed over, because the available numbers are not what they look like.

The best published figure for extracting these relations is an **88% audit confirmation rate on 149 directed edges across 42 companies**, rising to 100% when "economically plausible but weakly documented" relationships are counted as confirmed. **That is an audit confirmation rate on a sample, not a precision score against a gold set, and no recall is reported at all.** Presenting it as precision would be dishonest. A second 2026 pipeline, working from search snippets, **reports no precision or recall for relationship discovery whatsoever**, and explicitly says its source-credibility tiers are not to be read as a precision estimate. It is unusually candid about the rest, too: its output is a web-visible evidence graph rather than a complete ledger, a missing edge does not establish that no relationship exists, and its coverage advantage over an existing dataset does not measure recall.

The direction of all the published evidence is the same: **precision is achievable and recall is unmeasured.** For a step whose correctness criterion *is* completeness, that is the worst possible shape of evidence, and the episode should say so up front.

On the disclosure rule, there is a correction to carry. The accounting standard (ASC 280-10-50-42) requires an entity to disclose that a customer accounting for 10% or more of revenue exists, the amount of that revenue, and the segment reporting it — and explicitly states the entity **need not disclose the identity** of a major customer. Separately, Regulation S-K has historically been described as requiring the customer's *name* where sales to it are 10% or more of consolidated revenue *and* losing it would be materially adverse. **That naming test could not be verified from primary rule text.** Comment letters cite the paragraph variously as Item 101(c)(1)(vii) and (c)(1)(viii). The consolidated text of 17 CFR 229.101 shows no 10% threshold and no naming language at all. No source found says the requirement was eliminated. Either the 2020 modernisation absorbed it into principles-based language or the rendering consulted was incomplete. **Do not assert "Item 101(c)(1)(vii), 10%, name required" as current law** until the amended Item 101(c) in 85 FR 63726 has been read directly.

The usable narrative beat is better than the simple version anyway. Where a name is given, it is given at the issuer's discretion under a materiality judgement. So the relation is not merely undeclared — it is **selectively declared**, and the ground truth for "all companies with exposure to Z" is partly a function of who chose to name whom.

One missing number is worth chasing because it would settle the episode's feasibility. **No source quantifies what fraction of real customer-concentration disclosures name the counterparty versus writing "one customer" or "a significant customer".** That is the most decision-relevant gap in the whole brief, and it is measurable on the series' own corpus.

### Episode 10 — the synthesis stage, and whether it makes things worse

Attempt this only if Episode 9 produced a gold exposure set, because the synthesis score has to be reported *conditional on* first-stage recall meeting a stated threshold, not composed with it.

Target guidance in earnings-call remarks and press releases rather than narrative management discussion. The reason is a drafting fact rather than a modelling one: filings are written defensively and symmetrically, with a careful discussion stating both upside and downside for the same driver. A polarity classifier over that text will produce a near-neutral aggregate for almost every company, which makes the synthesis vacuous. Guidance in calls and releases is numeric, directional and revised, which is a far better target.

State the negative result plainly in the article. **There is no benchmark for document-level or company-level outlook extraction, and none for forward-looking-statement extraction as opposed to sentence classification.** The existing benchmarks measure something else: one asks whether a sentence contains a forward-looking statement, others ask what polarity a sentence has. What this pipeline needs is a judgement about a company's overall directional outlook, and no gold labels or reported metrics for that task were found anywhere. The stage would be unmeasurable with off-the-shelf instruments, which is worse than it being hard.

One nuance on legal boilerplate, which cuts the opposite way to the obvious guess. Courts have repeatedly held that generic forward-looking disclaimers do *not* earn the statutory safe harbour. So the legal pressure runs *against* boilerplate, and substantive tailored risk language is legally advantaged and therefore present. Boilerplate is a precision nuisance — high-volume near-duplicate legend text that any cue-word extractor will fire on — not an information-theoretic wall. The cheap mitigation is a boilerplate filter before extraction, and its effect can be measured directly.

Report four numbers and nothing combined: `V_strict` over vital nuggets, a FActScore-shaped groundedness figure conditioned on non-abstention, first-stage set F1 separately, and a reachability figure giving the fraction of vital nuggets whose source company was actually retrieved. That last one matters because without it a ceiling effect masquerades as a synthesis failure — a `V_strict` of 0.6 against a reachability of 0.6 is not a synthesis problem.

Then run the experiment nobody has run: the confidently-wrong rate, per claim and per answer, with and without the synthesis stage, over the same evidence.

## Conclusion

The two halves of this decision have different shapes, and noticing that is the most useful thing to take away.

Closing spectastic is a **disclosure** problem. Everything needed has already been measured. What remains is saying which tier the Java reaches, saying how much of what a reader is shown has never been judged, and saying that the series ends here. All three are writing, and all three are cheap.

Moving to finance is an **instrumentation** problem, and it inverts the usual order of work. Spider and BIRD hand you a correct database and measure only the last step, which is writing the query. The series' risk is concentrated almost entirely in the step those benchmarks assume away. A wrong number inside a correctly executed query is confidently and precisely wrong, and neither a retrieval metric nor execution accuracy will catch it. The good news is that the bitemporal machinery which makes an as-of aggregate correct is already built, tested and green, while the fact table it would operate on does not exist and is about fifteen lines of DDL. The hard half is done and the easy half is unwritten. That is an unusually good position to be in, as long as the article is clear about which half the 84 passing checks actually evidence.

The deeper shift is in what makes finance worth the move. It is not that finance is harder than spectastic. It is certainly not that finance needs multi-hop retrieval, because the published record is clear that it does not — and saying so in print is a contribution in itself, since the literature mislabels its own benchmarks. What makes it worth the move is that three question shapes the series could pose and answer are unmeasured by anyone: as-of aggregation in a language-model setting, provenance for an aggregate answer, and whether synthesising across a set raises confident wrongness. Each of those is a negative result sitting in the open, waiting for someone to close it. The series' distinctive asset has never been its retrieval stack; plenty of people have one of those, and in fact nobody in the finance literature has even run the reranked hybrid baseline this project already has. The asset is the discipline: freeze a question set, keep the metric families apart, and report the fraction nobody looked at. That discipline is worth more on a corpus where confident precision is the default failure mode than on one where the relations were written down all along.
