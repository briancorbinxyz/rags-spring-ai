# Routing a question to a structural part of a document, and resolving names to canonical structural keys

Scope note for the report writer: this round deliberately avoids re-treading the earlier
GraphRAG-vs-declared-edges round (`research_notes/Schema implied relations in spectastic/`) and the
earlier query-rewriting/intent-routing round (`research_notes/Structured query rewriting for retrieval/`).
Where those overlap, I only add *new* ground: relation-typed expansion and edge traversal at query
time, typed-edge weighting, and the alias/canonical-key resolution problem.

Two framing conclusions that run through everything below, stated up front because the citations are
scattered across sections:

1. **The literature's field-weighting machinery is almost entirely fitted machinery.** BM25F,
   learned field selection, mFAR gating — all of them need labelled queries, and the one paper that
   runs BM25F with *uniform* (unfitted) weights finds it is **worse than plain BM25 on 2 of 3
   datasets**. There is no measured result I could find for an unfitted field-weighting scheme that
   beats a single-field baseline. This is the strongest available argument for the project's
   "definitions not thresholds" preference: the fielded-retrieval literature cannot be adopted
   wholesale at 53+24+13 labelled questions.
2. **Where structure does help without fitting, it helps through *content placed into the unit*, not
   through a weight.** The one placebo-controlled ablation of document structure finds that real
   heading paths beat shuffled heading paths of identical form — i.e. the gain is semantic content
   in the indexed text, and that is a definition, not a threshold.

---

## Q1. Field/section-aware retrieval: how do systems route a query to the right field or section, and what is measured to work?

### Takeaway

Fielded retrieval splits into (a) score-combination over fields (BM25F / BM25E, and its modern
learned descendant mFAR) and (b) structure-aware chunking that folds the section identity into the
indexed unit. (b) has the only placebo-controlled evidence and the only mechanism that needs no
fitted weight; (a) works but its gains are *entirely* attributable to query-conditioned learned
weights, and the unfitted version regresses below single-field BM25.

### Cited Findings

**BM25F and field weighting — the fitting cost is explicit in the literature**

- BM25F field-weight tuning can require as many as **2|F|+1 independent parameter searches**; with
  uniform weights BM25F performed *worse than BM25* on some datasets, which the authors read as
  evidence that weight choice is entirely dataset-dependent — [Multi-Field Adaptive Retrieval (mFAR), arXiv 2410.20056](https://arxiv.org/html/2410.20056v2)
- Concrete unfitted-BM25F numbers (mFAR Table 12, weights **not tuned**) vs BM25 (Table 1), Hit@1:
  Amazon **0.183 vs 0.483**, MAG **0.451 vs 0.471**, Prime **0.142 vs 0.167**. Uniform-weight BM25F
  loses badly on Amazon and loses mildly on the other two — [mFAR](https://arxiv.org/html/2410.20056v2)
  - Caveat flagged in the fetch: Table 12 lists BM25's MAG MRR as 0.472 while Table 1 gives 0.572,
    so the two tables are not perfectly consistent. Treat the Amazon collapse as the robust signal
    and the MAG/Prime deltas as approximate.
- In BM25F and mixture-of-field language models, **field weights are fixed across query terms and
  estimated on held-out training queries**; Kim & Croft propose per-term field relevance as the
  alternative — [search summary of Kim & Croft field weighting](https://ciir-publications.cs.umass.edu/getpdf.php?id=1051)
- BM25F was extended to element level as **BM25E** for XML retrieval by Lu, Robertson & MacFarlane
  (2005/2006), with field weights **tuned on INEX 2004 topics and assessments** — [Field-Weighted XML Retrieval Based on BM25, City Research Online](https://openaccess.city.ac.uk/id/eprint/5224)
- Field weighting has a measurable **bias** side-effect: on TREC News collections, as title fields
  are weighted more heavily, bias increases while retrieval performance decreases; weighting
  content-based fields improved performance while reducing bias — [Strathclyde, "The impact of fielding on retrieval performance and bias"](https://strathprints.strath.ac.uk/64162)
- Microsoft Research learned BM25-style field weights with LambdaRank/NDCG rather than grid search —
  [Learning to rank with BM25-style functions, MSR tech report](https://microsoft.com/en-us/research/wp-content/uploads/2016/02/LearningBM25MSRTechReport.pdf)

**mFAR (ICLR 2025 Spotlight) — the strongest current measured result on query-conditioned field routing**

Setup: STaRK benchmark (Amazon / MAG / Prime), documents decomposed into fields (8 / 5 / 22 fields
respectively), each field indexed **independently** by both a lexical and a dense scorer.

- Score function: `s(q,d) = Σ_f Σ_m G(q,f,m) · s_f^m(q, x_f)` where the gate
  `G(q,f,m) = softmax({a_fm^T q})` is a softmax over **all field×scorer pairs**, conditioned *only*
  on the query embedding (shared Contriever encoder) — [mFAR](https://arxiv.org/html/2410.20056v2)
- Test results (Hit@1 / Recall@20 / MRR), STaRK average: BM25 **0.374 / 0.561 / 0.462**;
  single-field dense Contriever-FT **0.360 / 0.569 / 0.467**; mFAR-Lexical **0.339 / 0.549 / 0.437**;
  mFAR-Dense **0.411 / 0.641 / 0.520**; **mFAR-All 0.437 / 0.662 / 0.545** — [mFAR](https://arxiv.org/html/2410.20056v2)
- **Ablation removing query conditioning** (keeping global field weights): STaRK-average Hit@1 falls
  0.437 → **0.338**, R@20 0.662 → **0.602**, MRR 0.545 → **0.456** (relative losses ≈22.6% / 9.1% /
  16.3%); on Prime MRR falls 0.512 → **0.368** — [mFAR](https://arxiv.org/html/2410.20056v2)
- Fitting cost: the adaptation parameters are small (**768·|F|**, called "negligible" against
  Contriever's ~110M), but the **training query counts are 6K (Amazon), 8K (MAG), 6.1K (Prime)** —
  [mFAR](https://arxiv.org/html/2410.20056v2)
- Field-importance findings: zeroing Amazon's `authors` field with the lexical scorer costs
  **0.152 Hit@1**; zeroing Prime's `enzyme` field costs **0.004**. The paper explicitly warns that
  zeroing one scorer for a field can barely move results while zeroing both drops them, so a small
  single-scorer delta **does not** mean a field is unimportant — [mFAR](https://arxiv.org/html/2410.20056v2)
- The authors **expected** Prime (full of initialisms and IDs) to rely most on lexical scores; it did
  not — Prime leaned on dense, and lexical reliance was highest on MAG. Qualitatively, lexical
  matching helped on an institution name ("Eckerd College"), and BM25 was hurt by over-weighting a
  repeated keyword ("Physics") in an author list — [mFAR](https://arxiv.org/html/2410.20056v2)

**Section-aware retrieval and chunking — the placebo-controlled result**

- "Does Document Structure Help Dense Retrieval? A Placebo-Controlled Ablation of Four Mechanisms
  Across Two Corpora" (Kuehlkamp, Moreira, Rund; Oct 2026) ablates four treatments —
  structure-aligned chunking, LLM-generated chunk context, heading-path metadata, hierarchical
  two-stage retrieval — under one protocol with **matched chunk sizes** and a **semantically null
  placebo (heading paths shuffled across documents)**. Corpora: 200 Wikipedia Featured Articles
  (951 queries) and 1,585 QASPER papers (4,303 questions). Metric: coverage-aware nDCG@10 —
  [alphaXiv 2610.10170](https://alphaxiv.org/abs/2610.10170v1)
- Effects (Wikipedia / QASPER, cov-nDCG@10): structure-aligned chunks with real heading paths vs
  contextualised fixed windows **+0.022 / +0.012**; real heading paths vs placebo **+0.010 / +0.016**;
  **naive two-stage hierarchical retrieval −0.033 / −0.015**, traced to low first-stage section
  recall. Effect sizes Cohen's dz ≈ **0.06–0.11**, four pre-registered contrasts, document-clustered
  bootstrap with **Holm correction**; structure-vs-placebo and structure-vs-window are Holm-significant
  and consistent in direction across both corpora — [alphaXiv 2610.10170](https://alphaxiv.org/abs/2610.10170v1)
- Descriptive (Table 2, strong embedder, no reranker): QASPER real-path **0.125** vs placebo
  **0.109**; Wikipedia **0.413** vs **0.403** (no CIs or significance marks in that table) —
  [alphaXiv 2610.10170](https://alphaxiv.org/abs/2610.10170v1)
- Authors' conclusions: structure helps **modestly**; the benefit comes from **meaningful content
  rather than extra tokens**; **where a document is cut matters more than what prefix is attached**
  (section-aligned chunks with no prefix also beat LLM-generated context); gold human-annotated
  structure beat LLM-induced structure on Wikipedia but not on QASPER; results are retrieval-only
  and do not demonstrate better downstream answers — [alphaXiv 2610.10170](https://alphaxiv.org/abs/2610.10170v1)

**Learned routing over a structure tree**

- RDR² ("Equipping Retrieval-Augmented LLMs with Document Structure Awareness", Findings of EMNLP
  2025) formulates reading as **dynamic routing over a Document Structure Tree**, with an LLM agent
  iteratively choosing **Answer / Expand (unfold headings) / Refuse**. Motivation stated in the
  paper: fixed chunking limits query-adaptive selection, and ranking passages by relevance score
  disrupts their original order in the source. It reports SOTA on five datasets (the EMNLP listing
  cites three — versions differ), largest gains on multi-document synthesis, and introduces a way to
  curate routing training data from **questions and documents alone, with no answer supervision** —
  [arXiv 2510.04293](https://arxiv.org/html/2510.04293) / [ACL Findings EMNLP 2025](https://preview.aclanthology.org/new-sigs/2025.findings-emnlp.1339/)
- Heading-depth evidence from a vision-guided chunking paper: 2-level hierarchies "lost important
  contextual granularity" for complex documents while 4+ levels "introduced unnecessary
  fragmentation that degraded retrieval performance"; it settled on three levels (underlying numbers
  not visible in the snippet) — [arXiv 2506.16035](https://arxiv.org/pdf/2506.16035)
- HiChunk (Tencent) + Auto-Merge retrieval reports "superior chunking accuracy and significantly
  improved RAG performance, particularly for complex, evidence-dense query tasks" — not a controlled
  heading-hierarchy ablation.

**Prediction of "which field should I search" — thin and mostly patents**

- Query performance prediction for multifield retrieval exists as a **patent** (pseudo-reference list
  per field) — [USPTO 11436288](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/11436288);
  and "recommending fields for a query based on prior queries" (from prior query logs and user
  attributes) — [USPTO 10747756](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/10747756).
  Neither is an evaluated public method.
- On the general selective-processing idea, Hauff et al. (2010) found many existing query performance
  predictors **do not correlate strongly enough** to reliably improve retrieval in selective query
  expansion or meta-search; generalisation of QPP across collections and rankers is still an open
  problem — [arXiv 2504.01101](https://arxiv.org/abs/2504.01101v1)

**Expressibility in the target stack (SQLite FTS5)**

- FTS5 supports **column filters** written inside the MATCH string as `column:term`; the docs note
  that the expression left of MATCH is usually the table name, "the exception is when specifying a
  column-filter" — [SQLite FTS5 docs](https://sqlite.org/fts5.html) (section 3.6 "Column Filters";
  the fetch only returned the TOC, so the exact wording is corroborated by secondary sources below)
- FTS5 `bm25()` takes **one positional weight per declared column**, in declaration order, including
  UNINDEXED columns — e.g. `bm25(articles, 10.0, 1.0, 5.0)`. Scores are negative, more relevant =
  more negative, so `ORDER BY rank` ASC is already best-first — [DEV: SQLite FTS5 inverted index and BM25](https://dev.to/vahid_aghajani_60ce9dbec9/sqlite-fts5-how-full-text-search-actually-works-inverted-index-bm25-2kl8); [xqlite FTS5 guide](https://xqlite.hexdocs.pm/full_text_search.html)
- **Per-row/dynamic column weights are not supported**: a SQLite forum reply states this would require
  a custom auxiliary function, since the built-in `bm25()` is itself just an example FTS5 custom
  function — [SQLite forum](https://sqlite.org/forum/forumpost/d5cfe2565b)

### Inferences

- The *only* mechanism in this literature that is a definition rather than a threshold is
  **structure-aligned chunking with the real heading path included in the indexed text**. For the
  spectastic corpus that means each element unit's indexed text should carry its declared section
  name (`#context`, `#intent`, `#risks`, `#changelog`), its artifact name (`tasks`, `design`, …),
  and its spec slug (`spectastic`) — as *content*, not as a weight. Both motivating failures are
  partly addressed by this one change: failure 1 because the slug `spectastic` becomes lexically
  matchable in every unit of `specs/000-spectastic/`, and failure 2 because the word "tasks" (the
  artifact name) becomes lexically present in units paraphrased as "drew up work" only if the
  question *also* carries a term the unit now contains — see Q7 for the residual gap.
- The placebo design is the right template for the project's no-tuning discipline: if the project
  adds heading-path text to units, the honest control is **shuffled heading paths**, not "no heading
  path". A gain over no-path can be pure token inflation; a gain over shuffled-path cannot.
- mFAR's query-conditioned gate is the measured mechanism, but it needs ~6–8K training queries.
  At 53+24+13 questions the gate is **not learnable**. Its *ablation* is the usable finding: global
  (non-query-conditioned) field weights retain only ~77% of the Hit@1 benefit, and global weights are
  exactly the thing that would need fitting. So the realistic choice is **uniform-weight fielded
  scoring (measurably risky, per Table 12) or no field weighting at all**.
- mFAR's negative surprise on Prime is directly relevant: a corpus full of IDs and initialisms did
  *not* turn out to be the one that most needed lexical field scoring. That is a caution against
  assuming `FR-012`/`T-1203`-style ids make the lexical arm decisive; it should be measured on the
  project's own strata, not assumed.
- FTS5's positional `bm25()` weights are a per-query-shape constant, which means "field weighting"
  in this stack is implementable but every weight is a number someone chose. A **column filter**
  (`section:context`), by contrast, is a boolean derivable from the schema — cheaper to defend under
  the no-tuning rule, and the subject of Q2.
- The −0.033 hierarchical-retrieval regression is the clearest warning for a two-stage
  "pick the section, then search inside it" design: the paper's own diagnosis is **first-stage
  section recall**. A routing stage that is not near-perfect loses more than the structure gains.

### Gaps

- No authoritative head-to-head benchmark of BM25F weighting strategies on standard collections
  (the search explicitly turned up none).
- No measured result for section/field routing at tiny-eval scale; every evaluated method assumes
  thousands of labelled queries.
- Vespa rank-profile field weighting guidance: searched, not found. Vespa's serving-tuning docs cover
  `fast-search` and attribute-vs-index choices (query-plan performance), not rank-profile field
  weights — [Vespa feature tuning](https://docs.vespa.ai/en/performance/feature-tuning.html).
- The exact wording of FTS5's §3.6 Column Filters was not retrievable (sqlite.org/fts5.html returned
  only a table of contents to the fetcher); the behaviour is corroborated by secondary sources only.

---

## Q2. Metadata filtering vs soft boosting: hard pre-filter or fuse, and how do production systems detect/recover from an empty filtered set?

### Takeaway

I found **no controlled head-to-head experiment** comparing hard structural filters against soft
boosting for retrieval quality. What exists is (a) product documentation that makes the two modes
explicit and picks defaults by context, and (b) solid evidence on the *failure* side: filters degrade
recall **silently**, and post-filtering under-fills k. The recovery patterns in the wild are
overfetch, switch strategies by estimated selectivity, or relax the filter — all of which introduce
a number.

### Cited Findings

- **Google Discovery Engine documents both modes and switches default by context** (vendor doc, not
  an experiment): for **single-datastore** search the default is to apply NL-extracted filters as
  **hard filters**; for **multi-datastore** search the default is **soft boosts**. Hard: "Results that
  do not pass the extracted filters will not be returned in the result set." Soft: "Results that pass
  the filters will be boosted up to higher ranks in the result set." — [Google Cloud DiscoveryEngine ExtractedFilterBehavior](https://docs.cloud.google.com/python/docs/reference/discoveryengine/latest/google.cloud.discoveryengine_v1.types.SearchRequest.NaturalLanguageQueryUnderstandingSpec.ExtractedFilterBehavior)
- **Post-filtering is the usual cause of under-filled results**: ANN search runs first, non-matching
  results are dropped, so a selective filter leaves fewer than k or none. Overfetching reduces the
  risk, but the right multiplier depends on filter selectivity and the distribution of eligible
  vectors, so **a fixed multiplier can work for one tenant and fail for another** — [DEV Community](https://dev.to/puffball1567/why-filtered-vector-search-returns-fewer-than-k-results-23bo)
- **Pre-filtering avoids empty results but costs**: Pinecone's write-up says it is accurate but slow
  at scale because it brute-forces every remaining vector (vendor) — [Pinecone, vector search filtering](https://www.pinecone.io/learn/vector-search-filtering/md/)
- **Joint/filter-aware traversal is the middle ground**: Redis describes checking the filter during
  graph traversal, avoiding both a full subset scan and end-stage discarding; effectiveness depends
  on the index (vendor) — [Redis](https://redis.io/blog/metadata-filtering-vector-search-precision.md)
- **Silent recall loss is measured**: in Qdrant's benchmark, broad-value filters — especially an AND
  over two broad values — **lowered recall substantially while queries still returned results and
  did not look broken** (vendor benchmark) — [Qdrant, pre- vs post-filtering](https://qdrant.tech/blog/pre-filtering-vs-post-filtering/index.md)
- **Filtering vs not filtering** (not hard vs soft): Redis reports a benchmark where metadata
  filtering raised system accuracy from **0.12 to 0.61** (vendor) — [Redis](https://redis.io/blog/metadata-filtering-vector-search-precision.md)
- **Recovery patterns, all heuristic**: one guide recommends estimating filter selectivity, choosing
  a strategy from it, and falling back between strategies; its thresholds (<10% / >50% selectivity)
  are presented **without supporting evidence** — [OneUptime](https://oneuptime.com/blog/post/2026-01-30-metadata-filtering/view).
  A tutorial offers three empty-result responses — return empty, return empty with a warning, or
  retry with a broader search — but its examples are score-threshold-driven, so it does not address
  filter-caused emptiness directly — [FreeAcademy](https://freeacademy.ai/es/lessons/querying-and-filtering)
- Implementation-level caution (blog, not measured): applying metadata filters after vector retrieval
  can hurt recall, while integrating them into ANN traversal "requires careful implementation to
  avoid collapsing retrieval quality entirely" — [tianpan.co](https://tianpan.co/blog/2026-04-17-hybrid-retrieval-architecture-beyond-embeddings)

### Inferences

- For this project the empty-filtered-set problem is **structurally avoidable rather than
  threshold-managed**, because the filter predicate is derived from a declared, enumerable schema.
  The defensible rule is a *definition*: apply a structural filter only when the resolved key is
  known to exist in the index (the spec slug is in the declared slug set; the section name is in the
  declared section vocabulary). A filter on a key that is in the schema cannot return zero units
  unless the corpus itself is empty for that key — which is checkable at index time, once, not per
  query.
- That gives a cleanly stated **two-tier rule with no tuned number**: (1) resolve the question's
  mention to a canonical key; (2) if resolution succeeds *and* the key has non-zero units in the
  index, hard-filter; (3) otherwise do nothing (not "boost by 1.4"). The thing being avoided is a
  soft boost weight, which is a fitted number and would be unfalsifiable against 53 questions.
- Because SQLite FTS5 is a **pre-filtering** store by nature (the filter is a SQL `WHERE` or a
  column filter inside MATCH, applied as part of the scan), the post-filtering under-fill failure
  mode largely does not apply to the lexical arm. It **does** apply to the dense arm if the dense
  index is ANN with post-filter semantics. Asymmetric filter semantics across the two arms before
  RRF is a real hazard: the lexical arm returns k filtered hits while the dense arm returns fewer,
  which silently re-weights the fusion. Worth checking in the implementation.
- The Qdrant finding (recall drops while results still look fine) is the argument for instrumenting
  the filter itself: log, per query, the resolved key, the filter's cardinality, and whether the gold
  unit was inside the filtered set. That is a diagnostic, not a tuned parameter, and it is how a
  regression ratchet can catch a filter that starts excluding answers.

### Gaps

- **No measured hard-filter vs soft-boost comparison found.** The nearest thing is Google's
  context-dependent default, which is a design choice with no published evaluation behind it. This
  is the single biggest evidential hole in the brief.
- No published production postmortem on empty-filtered-set detection with real numbers; everything
  found is guidance-level.
- No evidence on how filter application interacts with **RRF fusion** when the two arms have
  different filter semantics.

---

## Q3. Entity / alias / mention resolution to canonical ids, with a small fully-declared inventory

### Takeaway

The classic three-stage pipeline (mention detection → candidate generation → disambiguation) is the
right decomposition, but nearly all of its machinery exists to handle **ambiguity at Wikipedia
scale**. The load-bearing number for this project is that the **mention-prior alone** — a pure alias
table, no model — reaches **71.9 on AIDA-B** where strong neural models reach the mid-80s. With ~100
entities and a *fully declared* alias inventory (folder names), the ambiguity the neural stack buys
down is largely absent, so an alias table is the right mechanism and the gap to a neural linker
should be small to nil.

### Cited Findings

- **Alias tables are the classic candidate generator**: early candidate generation mostly used
  non-neural approaches such as **TF-IDF or alias tables** (Wu et al., 2020, BLINK); later work moved
  to dense embeddings from pretrained transformers — [BLINK/Wu et al. via arXiv 2003.01343](https://arxiv.org/pdf/2003.01343)
- **Prior-alone baseline, measured**: the mention–entity prior p(e|m) alone reaches **71.9 on
  AIDA-B**, well below the local neural models in the same table, which score in the mid-80s. The
  prior is used both as a feature and for candidate selection — [Ganea & Hofmann, "Deep Joint Entity Disambiguation with Local Neural Attention", arXiv 1704.04920](https://arxiv.org/pdf/1704.04920)
- **Definition of the prior**: `p(e|m) = count(m,e) / count(m)` — the share of anchors with surface
  form *m* pointing to entity *e*, usually estimated from Wikipedia anchor text — [Entity Linking Meets Deep Learning survey, arXiv 2109.12520](https://arxiv.org/pdf/2109.12520)
- **Survey-level range**: the same survey puts prior-alone accuracy at roughly **70–85%** on common
  datasets; another paper claims it exceeds 80% on real-world datasets citing a different source.
  These ranges are **not directly comparable** to the 71.9 AIDA-B figure (different datasets,
  different prior estimations) — [arXiv 2109.12520](https://arxiv.org/pdf/2109.12520); [arXiv 2002.04936](https://arxiv.org/pdf/2002.04936)
- **"Argmax of prior" is the standard strong baseline** for AIDA in several papers, including a 2020
  sentence-level joint-embedding paper and the PBoH work (priors from Wikipedia and CrossWikis) —
  [arXiv 2002.04936](https://arxiv.org/pdf/2002.04936); [PBoH poster](https://people.csail.mit.edu/oct/a1_poster_pboh.pdf)
- **Alias-table coverage when the inventory is declared**: a cross-lingual candidate-generation paper
  using Wikidata alias lists as a lookup table reports **more than 50% of ALIAS mentions covered**
  by the table. Caveat: cross-lingual setting, so the figure likely does not transfer — [arXiv 2003.01343](https://arxiv.org/pdf/2003.01343)
- **ReFinED**: end-to-end, doing mention detection, fine-grained entity typing and disambiguation for
  all mentions in a document in a **single forward pass**; claims **>60× faster** than competitive
  approaches and **+3.7 F1 average** over SOTA on standard EL datasets, generalises to Wikidata
  (~15× more entities than Wikipedia), supports zero-shot linking — [ReFinED, NAACL 2022 industry track](https://aclanthology.org/2022.naacl-industry.24/)
- **ReFinED vs BLINK, timing and accuracy**: ReFinED is reported **6× faster** than the BLINK
  bi-encoder, which also has an **average F1 9 points lower** — [ReFinED, arXiv 2207.04108](https://arxiv.org/pdf/2207.04108)
  (via search summary; the >60× and 6× figures come from different comparisons in the paper —
  >60× against "competitive existing approaches", 6× against BLINK specifically)
- **BLINK architecture**: bi-encoder for candidate retrieval, cross-encoder for ranking; one
  production adaptation replaces the dense retrieval step with **Elasticsearch**, retrieving
  candidate entities with their embeddings from ES — [BLINK with Elasticsearch, arXiv 2205.04438](https://ar5iv.labs.arxiv.org/html/2205.04438)
- **Robustness caution on priors**: entity "overshadowing" — the case where the prior is
  systematically wrong for a less-popular entity sharing a surface form — is the documented weakness
  of prior-based disambiguation — [Robustness Evaluation of Entity Disambiguation Using Prior Probes, arXiv 2108.10949](https://arxiv.org/pdf/2108.10949)
- **Priors drift over time** — [On the Temporality of Priors in Entity Linking, arXiv 2101.05593](https://arxiv.org/pdf/2101.05593)
- A 2026 paper routes adaptively between cheap linking and LLM reasoning per mention rather than
  paying for the heavy path on every mention — [Leveraging LLMs in Entity Linking via Adaptive Routing and Targeted Reasoning, arXiv 2510.20098](https://arxiv.org/pdf/2510.20098)

### Inferences

- The project's situation is the **best case** for an alias table and the **worst case** for neural
  entity linking. `specs/000-spectastic/` *declares* the mapping `spectastic → 000`. The alias
  inventory is ~100 entries, closed, and derivable by `ls`. There is no popularity prior to estimate
  because there is (almost certainly) no surface-form collision: the 71.9-vs-mid-80s gap on AIDA is
  a gap created by ambiguity, and if the slug set has no ambiguous surface forms, that gap closes to
  near zero. Concretely: build the alias table at index time from the directory listing — the
  canonical key is `000`, the surface forms are `spectastic`, `000-spectastic`, `000`, and
  (optionally) tokenisations of the slug. This is a **definition derived from the schema**, exactly
  what the project prefers, and it costs no labelled data.
- The one real decision is **candidate generation for inexact mentions** — a question that writes
  "spec-tastic", "Spectastic CLI" or a misspelling. Options, in increasing cost:
  (1) exact match on normalised surface forms (lowercase, strip hyphens/underscores) — a definition;
  (2) string-similarity candidate generation (edit distance / trigram) with an acceptance rule —
  introduces **one threshold**, and that threshold is the thing to flag under the no-tuning rule;
  (3) embedding-based candidate generation — a threshold plus a model.
  A defensible way to keep (2) threshold-free at ~100 entities: take the **single nearest** canonical
  key by similarity and accept it only if it is unambiguously nearest (a margin rule is still a
  number; "unique exact match after normalisation, else no filter" is not). If a threshold is
  unavoidable, note that choosing it on 53 questions is fitting on eval.
- **Minimum labelled-data requirement**: an alias table built from declared folder names needs
  **zero** labelled questions to construct. It needs labelled questions only to *measure* whether it
  helped — and the measurement is per-question binary (did the right spec get resolved), so a handful
  of questions that name a spec by name suffices as a smoke test. By contrast ReFinED/BLINK-class
  linkers are pretrained but would need the ~100 entities described (descriptions + types) to link
  zero-shot, and no labelled data; their cost here is a model dependency and latency, for ambiguity
  this corpus does not have.
- Mention **detection** is the part the project cannot skip and the part the alias table does not
  solve. "Did spectastic ever consider…" requires knowing that `spectastic` is a mention at all. At
  ~100 closed entities the cheap definition is **dictionary matching over the question text** (longest
  normalised match against the alias table, case-insensitive), which is the pre-neural standard and
  needs no model. That single change fixes motivating failure 1 without an LLM in the path.
- Entity overshadowing and prior drift are the two documented weaknesses of prior-based linking, and
  **neither applies** when the alias table is a bijection declared by the filesystem. They become
  relevant only if slugs are reused or renamed — see Q6.

### Gaps

- I could not retrieve ReFinED's **candidate generation** details (whether it uses an alias table and
  a p(e|m) prior, candidate count, per-dataset AIDA F1, parameter counts, or an ablation isolating
  the entity prior). Both the PDF and the ACL abstract page failed to yield it; the ACL page carries
  abstract-level claims only. The report should not assert how ReFinED generates candidates.
- No source found that measures alias-table-only linking on a **small closed inventory** (~100
  entities). The claim that the neural gap closes at small, unambiguous inventories is my inference
  from the ambiguity-driven nature of the 71.9-vs-85 gap, **not** a cited result.
- No source found on string-similarity candidate generation with a **threshold-free** acceptance rule.

---

## Q4. Declared vs inferred relations: relation-typed expansion, edge traversal at query time, tunability of typed edge weights

### Takeaway

Relation-typed traversal at query time is well described mechanically but **thinly evaluated as an
isolated component**; the standard formulation selects edges whose *relation* embedding is similar
to the query (a top-K, i.e. a tuned number) and expands by BFS/DFS from matched seeds. I found **no
source** that evaluates typed-edge *weighting* as tunable-without-fitting, and no source that
compares declared typed edges against LLM-extracted typed edges on traversal quality specifically.

### Cited Findings

- **Relational matching as the standard query-time traversal primitive**: the GraphRAG survey
  describes selecting the **top-K edges whose relations are similar to the query**, with those matches
  deciding which edges the traversal explores next; "graph traversal algorithms (e.g., BFS, DFS) can
  expand this set from the initial entity and relation matches" — [Retrieval-Augmented Generation with Graphs (GraphRAG survey), arXiv 2501.00309](https://arxiv.org/pdf/2501.00309)
- **Typed edges feeding a ranking walk**: PROPEX-RAG retrieves top-k fact triples by embedding
  similarity, then seeds a **personalized PageRank** over a graph whose edges encode typed semantic
  relations — [PROPEX-RAG, arXiv 2511.01802](https://arxiv.org/pdf/2511.01802)
- **Engineering trade-off on edge typing**: FalkorDB's docs argue for **one generic relationship type
  with a vector index over all edges** (fewer indexes), keeping the original type as a **property** so
  you can still filter by type in custom Cypher (vendor doc) — [FalkorDB graph schema](https://docs.falkordb.com/graphrag/graph-schema)
- **Edge metadata carries time and provenance, not type**: an overview notes entities and
  relationships carry metadata like confidence scores, source documents, temporal ranges — implying
  time filtering happens through edge *properties* rather than the edge type. **No source found that
  evaluates this** — [memx.app](https://memx.app/blog/graphrag-vs-vector-rag-when-graphs-win/)
- **Ordering of hybrid stages**: a June 2026 article recommends running vector recall first and graph
  traversal second when a query needs both detail recall and relationship reasoning, at the cost of
  added latency (blog, not measured) — [memx.app](https://memx.app/blog/graphrag-vs-vector-rag-when-graphs-win/)
- A worked typed-edge traversal walkthrough (chain of relations from a seed entity) — [LogRocket](https://blog.logrocket.com/graph-rag-relationship-retrieval/)

### Inferences

- The survey's formulation puts a **top-K and a similarity over relation names** in the traversal
  path. For this project both can be replaced by definitions, because the relation vocabulary is
  closed and declared: the edge types are the artifact family (`spec`→`design`→`tasks`→`verify`→
  `triage-log`), the shared-anchor-id relation, and section containment. "Which edge type do I
  follow" over a ~5-type vocabulary is a **lookup from question shape to edge type**, not a learned
  top-K over embeddings. That is the clearest place in this whole brief where the literature offers a
  threshold and the schema offers a definition.
- The FalkorDB pattern (generic edge + type as a property) maps directly onto a relational
  implementation: a single `edges(src, dst, type)` table in SQLite with `type` as a filterable
  column, rather than one table or index per relation type. Expansion is then a join with a
  `WHERE type IN (...)` whose membership comes from the question's resolved intent — no weight.
- **Typed edge weighting is the part to avoid.** No source measures it, and weighting edge types is
  precisely a per-type fitted scalar: with ~5 types and 53 questions, any weight vector is fitted on
  eval. The weight-free alternatives are (a) include/exclude a type (boolean, derivable from the
  question's resolved intent) and (b) hop budget 1 (a definition: "the siblings of a retrieved unit",
  not "the top-K most relevant edges"). Both are expressible in the target stack as SQL.
- The −0.033 two-stage regression from Q1 and the survey's top-K relational matching are the same
  warning from two directions: **any stage that narrows before the main retrieval must be near-perfect
  or it costs more than it gains**. The safe composition is therefore *expansion after* a full
  unfiltered retrieval (add siblings of what was already found) rather than *restriction before* it.
  Expansion can only add recall; restriction can remove the answer.

### Gaps

- No evaluation isolating relation-typed traversal from the rest of a GraphRAG pipeline, so no number
  for "what typed expansion is worth".
- No source comparing **declared** typed edges against **LLM-extracted** typed edges on traversal
  quality (the prior round covered declared-vs-extracted graphs generally; this specific comparison
  was not found).
- No source on whether typed edge weights are tunable without fitting — searched, nothing found.
  Report this as an evidential absence, not as "weights are fine".
- No 2026 work found on time-aware traversal over relation-typed edges.

---

## Q5. Schema-aware query understanding: natural language → structural predicate, and how it is validated

### Takeaway

The mechanism (LLM function-calling / constrained generation producing a filter object against a
known schema) is a shipped product feature at AWS, NVIDIA, Google and in Haystack/LangChain, but
**nobody publishes accuracy for it**. The one measured thing adjacent to it is structured-output
reliability, and that evidence is bad: popular constrained-decoding implementations reach **as low as
50% coverage** on real-world JSON schemas, recoverable to **~95% with a validation-feedback loop** at
~1–3 extra seconds per schema.

### Cited Findings

- **No dedicated text-to-filter accuracy benchmark found.** The closest academic hit is a 2026 arXiv
  paper evaluating LLMs on retrieving food/nutrition context for RAG, focused on whether LLMs can
  translate NL queries into structured metadata filters against Chroma; it reports that **retrieval
  accuracy drops sharply as query complexity rises**. Caveat: the indexed text for this paper was a
  generated summary, so its numbers should be checked against the full PDF before use — [arXiv 2603.09704](https://arxiv.org/pdf/2603.09704)
- **Product implementations (vendor, no accuracy figures)**: AWS "intelligent metadata filtering" on
  Amazon Bedrock uses **function calling** to extract filters from queries — [AWS ML blog](https://aws.amazon.com/blogs/machine-learning/streamline-rag-applications-with-intelligent-metadata-filtering-using-amazon-bedrock);
  NVIDIA's RAG Blueprint offers LLM-based filter generation for Milvus (Elasticsearch in later
  versions) — [NVIDIA RAG custom metadata](https://docs.nvidia.com/rag/2.6.0/custom-metadata.html);
  Haystack and Flowise both ship extract-metadata retrievers — [Haystack](https://haystack.ai/blog/extracting-metadata-filter/); [Flowise](https://docs.flowiseai.com/integrations/langchain/retrievers/extract-metadata-retriever).
  None report accuracy.
- **The exact failure mode that matches motivating failure 1**, stated independently: "sometimes the
  metadata filter is not obvious based on the natural language in the question", with a worked example
  where **an episode number had to be translated into a stored ID format** — [Lance Martin, Retrieval Architectures](https://rlancemartin.notion.site/Retrieval-Architectures-18502448c85240828f33716740f9574b)
- **Structured-generation reliability, measured**: JSONSchemaBench (Geng et al.) builds a benchmark of
  **10K real-world JSON schemas** and scores six frameworks (Guidance, Outlines, llama.cpp, XGrammar,
  OpenAI, Gemini) on efficiency, constraint coverage and output quality, motivated by "poor
  understanding of the effectiveness of the methods in practice" — [arXiv 2501.10868](https://arxiv.org/html/2501.10868v2)
- **The reliability critique**: "Good-Enough Structured Generation: A Case Study on JSON Schema"
  reports popular implementations reaching **as low as 50% coverage** on real-world schemas, and that
  using **validation failures as feedback in a loop reaches ~95% coverage** at a cost of roughly
  **1–3 extra seconds per schema**. Attributed causes: grammar-constrained decoding is limited to
  context-free grammars while real schemas need context-sensitive checks, plus implementation
  problems such as **token-boundary misalignment** and **state explosion** — [NeurIPS 2025 listing](https://nips.cc/virtual/2025/131662)
  (figures taken from the listing/abstract, not the full text)
- Practitioner overview of token-masking-based grammar-constrained generation (opinion-level, not
  empirical) — [tianpan.co](https://tianpan.co/blog/2026-04-16-grammar-constrained-generation-output-reliability)

### Inferences

- The combination of findings here argues **against** putting an LLM in the filter-construction path
  for this project, and the argument is not aesthetic:
  (a) no published accuracy for text-to-filter at all;
  (b) the measured accuracy that does exist degrades with query complexity;
  (c) structured-output coverage is itself only ~50% without a retry loop, and the fix costs 1–3s per
  call — on local Ollama models, likely worse than the cloud figures the paper measured;
  (d) a non-deterministic filter breaks the regression ratchet, because the same question can produce
  a different predicate on a re-run.
  A dictionary-match + schema-lookup path is deterministic, has no coverage question, and costs
  microseconds.
- Where an LLM *is* defensible is **validation**: the JSON-schema work's finding that a
  validate-and-retry loop recovers coverage generalises to "never trust generated structure
  unvalidated". If the project ever does generate a predicate, the validator is free here because the
  schema is closed: every produced key must be a member of the declared slug set / section
  vocabulary / artifact set, and an out-of-vocabulary key means *discard the predicate*, not retry.
  That is a definition, and it converts an LLM failure into a no-op rather than a wrong filter.
- Validation design for the tiny-eval regime: the right unit of measurement is **predicate accuracy**
  (did the resolved key equal the gold key), not end-to-end answer quality, because predicate accuracy
  is cheap to label by hand and does not consume the eval budget on a downstream metric. For 53+24+13
  questions, labelling the gold spec/section/artifact per question is a one-off manual pass and gives
  a diagnostic that is independent of the ratchet.

### Gaps

- **No per-model text-to-filter accuracy figures exist in anything I found.** This is a genuine hole
  in the literature, not a search failure — the vendor docs that ship the feature publish none.
- JSONSchemaBench per-framework coverage numbers were not retrieved (only the critique's aggregate
  "as low as 50%" and "~95% with feedback").
- No evidence on constrained generation with local small models via Ollama specifically; the measured
  frameworks are cloud APIs plus llama.cpp/XGrammar, and llama.cpp's inclusion is the only local
  datapoint.

---

## Q6. Rename/alias over time: a structural key whose name changed

### Takeaway

The established production practice is an **alias/redirect layer in the index**, and Elasticsearch
implements it as a first-class mapping type: an `alias` field is an alternate name usable in place of
the target field in searches. The migration pattern is well documented. I found no *measured* study
of alias/redirect edges in search indexes — this is engineering practice, not literature.

### Cited Findings

- **Elasticsearch field aliases** were added in **6.4**: an `alias` mapping type defines an alternate
  name for a field in an index, which "can then be used in place of the target field in searches" —
  [Elastic, Introducing Field Aliases](https://www.elastic.co/blog/introducing-field-aliases-in-elasticsearch)
- **The rename migration pattern**: add an alias on each existing index mapping the **new** name to
  the **old** field, while newly created indices use the new name directly; searching with the new
  field name then works across all indices while the rename is in progress — [Elastic](https://www.elastic.co/blog/introducing-field-aliases-in-elasticsearch);
  corroborated by an Elastic engineering update describing old indices getting aliases carrying the
  new field names so existing indices stay searchable under the new schema — [This Week in Elasticsearch and Apache Lucene, 2018-07-20](https://www.elastic.co/blog/this-week-in-elasticsearch-and-apache-lucene-2018-07-20)
- **Index renames**: Elasticsearch has **no rename API** for indices; the alias is the substitute,
  giving an existing index a second name without moving data. The recommended discipline is that
  clients use the alias name and **never** the physical index name, so the underlying index can be
  swapped later — [pulse.support KB](https://pulse.support/kb/elasticsearch-rename-index)
- Not applicable despite appearing in searches: Elastic's **rename processor** is an ingest-pipeline
  feature, not search-time aliasing — [rename processor docs](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/rename-processor.html)
- **Prior drift is a documented entity-linking problem** in its own right, which is the same hazard
  seen from the alias side — [On the Temporality of Priors in Entity Linking, arXiv 2101.05593](https://arxiv.org/pdf/2101.05593)

### Inferences

- The Elasticsearch pattern transfers directly and cheaply: the alias table from Q3 becomes
  **many-to-one** (`surface form → canonical key`) rather than one-to-one, and a rename is simply an
  extra row. Both the old and the new name resolve to the same canonical key, so a question using
  either reaches the content, and **nothing in the retrieval path changes**. This is the cleanest
  "definition not threshold" instance in the whole brief: alias history is data, not a tuned
  behaviour.
- The discipline Elastic recommends — clients address the alias, never the physical name — maps to:
  **the canonical key is the spec number, never the slug**. The slug is one surface form among
  possibly several over time; the number `000` is the stable identity. The project's folder naming
  `000-spectastic` already encodes exactly this split, which is why the mapping is free to extract.
- Where this gets harder: a renamed *section* (`#risks` → `#hazards`) also needs an alias row, and
  unlike folder names, old section names may only exist in git history. If the project wants
  rename-robust section routing, the alias inventory for sections has to be sourced from the repo
  history or from a declared changelog — which the corpus happens to have (`#changelog`). Worth
  checking whether renames are recorded there; if so the alias table is derivable from declared
  content rather than hand-written.

### Gaps

- **No measured evaluation of alias/redirect edges in search indexes.** All sources are vendor
  documentation and engineering practice. No recall/precision numbers exist for "did aliasing recover
  the questions that used the old name" in any published study I found.
- No source on alias handling for *section* or *anchor* renames (as opposed to field/index renames).
- Lucene-level alias handling was not covered by the results; only the Elasticsearch layer.

---

## Q7. Section-name-to-intent mappings: is there prior art on *learning* "a why-question wants #context" from the schema?

### Takeaway

I found **no prior art on learning a question-type → section-name mapping from a schema**, either as
a paper or as a production write-up. The adjacent work either learns routing over a structure tree
with supervision (RDR², and its supervision is question+document derived, not schema-derived) or
hand-specifies the mapping. This should be reported as a genuine gap, and it is the weakest-evidenced
of all the brief's questions.

### Cited Findings

- The closest thing is **RDR²**'s routing policy over a Document Structure Tree, which is **trained**,
  not derived — though notably its training data is curated "using only questions and documents (no
  answer supervision required)" — [arXiv 2510.04293](https://arxiv.org/html/2510.04293)
- The placebo ablation's finding bears on the alternative to a mapping: real heading paths **as
  content** beat a form-matched placebo (+0.010 Wikipedia / +0.016 QASPER cov-nDCG@10), and
  section-aligned chunks with **no prefix at all** also beat LLM-generated context — i.e. the
  retriever can exploit section identity **without any explicit intent→section rule** — [alphaXiv 2610.10170](https://alphaxiv.org/abs/2610.10170v1)
- The same paper's **naive two-stage hierarchical retrieval −0.033 / −0.015**, traced to first-stage
  section recall, is direct evidence against a hard intent→section routing stage unless it is
  near-perfect — [alphaXiv 2610.10170](https://alphaxiv.org/abs/2610.10170v1)
- My search on this specific question returned education/curriculum-mapping literature and nothing on
  retrieval, confirming the term does not exist as a retrieval research topic (searched
  "question type maps to document section learning intent to section mapping").

### Inferences

- Given no prior art, the honest framings for the project are:
  (a) **don't route to sections at all** — instead make the section name part of each unit's indexed
  text (the one placebo-validated mechanism) and let the existing BM25+dense+RRF+cross-encoder stack
  discover the association. "Why" questions then match `#context` units because the word "context"
  and the section's own prose are both in the unit. This needs **zero labelled data** and **zero new
  components**;
  (b) if an explicit mapping is wanted, **hand-write it from the schema** and treat it as part of the
  schema (a declared table, versioned with the corpus), which is a definition. Learning it would
  require labelled (question-type, gold-section) pairs — and at 53+24+13 questions, learning a
  mapping over 4 section names from 53 examples **is fitting on eval**.
- A rough minimum-data estimate for (b)-as-learned, offered as reasoning rather than a cited rule:
  distinguishing 4 section targets reliably needs on the order of tens of examples *per target* with
  held-out validation, so low hundreds of labelled questions. That is an order of magnitude more than
  the project has, and the labels would have to be new questions, not the eval sets.
- Motivating failure 2 ("drew up work" → `tasks.html`) is **not** a section-routing problem and should
  not be solved with one. A word→filename table misses the paraphrase; so would a learned
  intent→artifact router trained on 53 questions. The mechanism that addresses it without fitting is
  option (a) plus **declared relation expansion** (Q4): retrieve whatever the question does match
  (likely the design units about screens), then expand along the declared `design→tasks` sibling edge
  at hop 1. The question never has to contain the word "tasks", and no threshold is introduced. This
  is the composition the two motivating failures jointly point at: **alias resolution for named
  entities (Q3), content-embedded structure for sections (Q1), sibling expansion for artifact-type
  paraphrase (Q4)** — all three definitions, none of them a tuned number.

### Gaps

- **No prior art found** on learning or deriving intent→section mappings from a schema. Reported as
  an absence.
- No source quantifying how much of section identity a reranker can already recover from
  heading-path text alone, so inference (a)'s strength is unmeasured on this corpus.
- RDR²'s results could not be pinned to a dataset count (the arXiv version says five datasets, the
  EMNLP listing three), so the magnitude of its gains should be reported cautiously.

---

## Cross-cutting: what needs a fitted number, and the minimum labelled data

### Takeaway

Of the mechanisms surveyed, four are definitions derivable from the declared schema and need **zero**
labelled questions to construct; the rest all require fitted weights or thresholds and, at the
measured scale in the literature, thousands of labelled queries.

### Cited Findings (sources already given above; this is a consolidation, not new claims)

| Mechanism | Needs fitting? | Minimum labelled data | Evidence quality |
|---|---|---|---|
| Alias table from declared folder names (`000-spectastic` → `000`) | No — a definition | 0 to build; a handful of questions to smoke-test | Prior-alone EL baseline 71.9 AIDA-B establishes alias tables as a serious baseline; no study at ~100 closed entities |
| Heading path / artifact name / spec slug as indexed **content** | No — a definition | 0 | Placebo-controlled, Holm-significant, +0.010/+0.016 cov-nDCG@10 vs placebo |
| Hard structural filter gated on "resolved key exists in index" | No — a definition | 0 | No hard-vs-soft experiment exists; emptiness is structurally excluded |
| Hop-1 expansion along declared artifact-family edges | No — a definition | 0 | Mechanically standard (GraphRAG survey); not independently evaluated |
| Soft boost weight for a matched structural predicate | **Yes** — one scalar per predicate | Unknown; unfalsifiable at 53 questions | No measured comparison found |
| BM25F / FTS5 positional column weights | **Yes** — up to 2\|F\|+1 searches | Thousands (INEX-scale topic sets) | Uniform weights measurably worse than BM25 on Amazon (0.183 vs 0.483 H@1) |
| mFAR-style query-conditioned field gate | **Yes** — 768·\|F\| params | **6K–8K training queries** in the published setup | ICLR 2025 Spotlight; +0.063 H@1 over BM25 average |
| Top-K relation-similarity edge selection | **Yes** — K plus a similarity | Not reported | Survey-level description only |
| Typed edge weights | **Yes** — one per type | Not reported | **No evaluation found at all** |
| String-similarity candidate generation acceptance | **Yes** — one threshold | Choosing it on 53 questions = fitting on eval | No threshold-free variant found |
| Overfetch multiplier for post-filtered ANN | **Yes** — and "a fixed multiplier can work for one tenant and fail for another" | n/a | Practitioner, explicit that it does not generalise |
| LLM-generated structural predicate | Not a weight, but non-deterministic | 0 to run; labels needed to trust it | ~50% structured-output coverage without a retry loop; no text-to-filter accuracy published anywhere |
| Learned intent→section mapping | **Yes** | Low hundreds (my estimate, uncited) | No prior art found |

### Inferences

- The four zero-fitting mechanisms are mutually composable and between them address both motivating
  failures. None of them touches the regression ratchet's integrity, because none of them has a
  number that could have been chosen by looking at the eval sets.
- The literature's measured wins (mFAR +0.063 average H@1 over BM25) are **unavailable** at this
  project's data scale, and the unfitted version of the same idea is a measured *regression*. The
  correct reading is not "fielded retrieval doesn't work" but "fielded *weighting* is a data-scale
  bet this project cannot make, while fielded *filtering* and fielded *content* are not bets at all".
- Two directional asymmetries worth stating plainly in the report, both supported above:
  **expansion is safe, restriction is not** (expansion can only add recall; the −0.033 two-stage
  result and the Qdrant silent-recall-loss result are both restriction failures), and
  **content is safe, weights are not** (the placebo result validates content; every weight in the
  table above is unvalidated at this scale).

### Gaps

- The "low hundreds" figure for learning an intent→section mapping is my own reasoning, with no
  source. It is flagged as such in Q7 and should be presented as an estimate or dropped.
- No source gives a minimum-labelled-data figure for *any* of these mechanisms at small scale; every
  figure in the table is either "the published setup used N" or "not reported".
