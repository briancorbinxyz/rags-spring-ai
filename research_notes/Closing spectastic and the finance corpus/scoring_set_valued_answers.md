# Measuring answers that are ranked sets, computed aggregates, or synthesised consensus

Research current as of 2026-10-29. Every claim below carries a URL. Where a figure, formula or
rule could not be verified against a primary source it is recorded under **Gaps** rather than
asserted. Sections marked **DESIGN** are my own proposal, not established practice.

## What metrics exist for set-valued and ranked-list answers?

### Takeaway

There are two distinct families and they answer different questions. Set measures (exact match,
set P/R/F1, Jaccard) ignore order and are what TREC itself used for list questions; rank-similarity
measures (Kendall's tau, AP correlation, RBO) compare two orderings and only RBO is designed for
truncated, non-conjoint top-K lists with a quantified residual. Graded-relevance measures (nDCG,
ERR) can be applied to an entity list unchanged, because nothing in them requires the ranked items
to be documents — this is exactly what the TREC Entity track did.

### Cited Findings

**Set precision / recall / F1 — the TREC list-question definition (the direct precedent)**

- TREC 2004 QA scored list questions with set measures over *instances*, not documents: "A system's
  response to a list question was scored using instance precision (IP) and instance recall (IR)
  based on the list of known instances. Let S be the the number of known instances, D be the number
  of correct, distinct responses returned by the system, and N be the total number of responses
  returned by the system. Then IP = D/N and IR = D/S." Precision and recall were combined "using the
  F measure with equal weight given to recall and precision: F = 2 · IP · IR / (IP + IR)". The list
  component score was "the average F score over the 55 questions" (Section 2.2, *Overview of the
  TREC 2004 Question Answering Track*, Ellen M. Voorhees, NIST, TREC-13, 2004) — [TREC 2004 QA overview](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf)
- The word **distinct** in `D` is the de-duplication rule: duplicates in the response are not
  rewarded. The gold side is "the list of known instances", i.e. an explicitly enumerated complete-as-
  known set — [same source](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf)
- Scale of the TREC 2004 list task, which is informative for how large a gold set has to be: "number
  of answers per question is 8.8, with 2 as the smallest number of answers, and 41 as the maximum
  number of answers" — [same source](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf)
- TREC measured the evaluation's *own* error term by looking at runs whose list components were
  identical: "There were 15 pairs of runs with identical list components. Seven pairs had identical
  average F scores… The largest difference in average F scores for identical list components was
  0.006, and the largest number of individual questions judged differently for a single run pair was
  7." — [same source](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf)
- Modern set-valued QA uses the same thing without a cutoff: QAMPARI (open-domain questions whose
  answers are entity lists) says "recall, precision, and F1 are standardly computed by comparing 𝒜
  and 𝒜pred, allowing for aliases", averaged across examples, with no recall cap and no @K
  truncation on the answer side; a gold answer "is covered if it or one of its aliases are in 𝒜pred",
  and all Wikidata aliases count as correct (Section 4.2 and Appendix D). Best reported F1 across
  ODQA models was 32.8 — [QAMPARI, GEM workshop 2023](https://arxiv.org/abs/2205.12665v3), [HTML](https://arxiv.org/html/2205.12665v3), [ACL Anthology 2023.gem-1.9](https://aclanthology.org/2023.gem-1.9.pdf)
- `ir_measures` already exposes the unranked set family, so a qrels-shaped gold record can be scored
  with them without new code: "SetP … the number of relevant docs divided by the total number
  retrieved"; "SetR … the number of relevant docs divided by the total number of relevant documents";
  "SetF … the harmonic mean of SetP and SetR", with a beta parameter; "SetAP … SetP * SetR" —
  [ir_measures measures reference](https://ir-measur.es/en/latest/measures.html)

**What each set measure rewards, under truncation, and under ties**

- Exact set match rewards nothing partially: it is all-or-nothing. In the text-to-SQL tradition this
  is implemented not on whole strings but clause-wise: Spider's exact-match evaluation does not
  "simply conduct… string comparison between the predicted and gold SQL queries"; instead it
  "decompose[s] each SQL into several clauses, and conduct[s] set comparison in each SQL clause" —
  [Spider task page](https://yale-lily.github.io/spider)
- Set P/R/F1 and Jaccard are order-blind, so they are *tie-proof by construction*: equal holdings
  values cannot change the score because position never enters the formula. This is the practical
  reason to put the set measure first and the order measure second.
- Under truncation to length K, set recall is bounded above by K/|gold| and set precision is
  computed against whatever the system returned (`N` in the TREC formula), which is why TREC's `N`
  is the system's own response length and not a fixed cutoff — [TREC 2004 QA overview](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf)

**Rank-similarity measures: Kendall's tau, AP correlation, RBO**

- Kendall's tau and Spearman's rho require conjoint, untruncated, equal-length lists and weight all
  depths equally — the RBO authors' stated motivation is precisely that "no existing metric" met the
  three requirements of handling lists that do not share the same items, favouring top ranks, and
  never decreasing with deeper evaluation. "Set similarity is a more natural basis for comparing
  indefinite and truncated lists than the more widely used one of correlation." — [Webber, Moffat & Zobel, *A similarity measure for indefinite rankings*, ACM TOIS 28(4), Nov 2010, art. 20, pp. 20.1–20.38, DOI 10.1145/1852102.1852106](https://people.eng.unimelb.edu.au/ammoffat/abstracts/wmz10acmtois.html); [author's version PDF](http://www.williamwebber.com/research/papers/wmz10_tois.pdf)
- **AP correlation (tau_AP)** is the top-weighted rank correlation: "tauAP is the AP rank correlation
  coefficient by Yilmaz et al., where neither vector can contain tied items", from Yilmaz, Aslam &
  Robertson, "A New Rank Correlation Coefficient for Information Retrieval", ACM SIGIR 2008. Ties
  need the Urbano & Marrero variants: "tauAP_a and tauAP_b are the versions developed by Urbano and
  Marrero to cope with ties under the scenarios of accuracy and agreement, respectively" (Urbano &
  Marrero, "The Treatment of Ties in AP Correlation", ACM ICTIR 2017) — [ircor::tauAP documentation](https://search.r-project.org/CRAN/refmans/ircor/html/tauAP.html)
- **RBO — exact formulas** (all from the TOIS paper, Section 4). Overlap at depth d:
  `X_{S,T,d} = |S_{:d} ∩ T_{:d}|` (Eq. 2). Agreement: `A_{S,T,d} = X_{S,T,d} / d` (Eq. 3). Average
  overlap: `AO(S,T,k) = (1/k) Σ_{d=1}^{k} A_d` (Eq. 4). The weighted-overlap family:
  `SIM(S,T,w) = Σ_{d=1}^{∞} w_d · A_d` (Eq. 5). With geometric weights `w_d = (1−p)·p^{d−1}`:
  **`RBO(S,T,p) = (1 − p) Σ_{d=1}^{∞} p^{d−1} · A_d`** (Eq. 7). "Rank-biased overlap falls in the
  range [0, 1], where 0 means disjoint, and 1 means identical." — [RBO paper PDF](http://www.williamwebber.com/research/papers/wmz10_tois.pdf)
- RBO's parameter p: "The parameter p determines how steep the decline in weights is: the smaller p,
  the more top-weighted the metric is. In the limit, when p = 0, only the top-ranked item is
  considered, and the RBO score is either zero or one." The weight of the first d ranks is
  `W_RBO(1:d) = 1 − p^{d−1} + ((1−p)/p) · d · (ln(1/(1−p)) − Σ_{i=1}^{d−1} p^i / i)` (Eq. 21), and
  "p = 0.9 means that the first 10 ranks have 86% of the weight of the evaluation; to give the top
  50 ranks the same weight involves taking p = 0.98" (Section 4.3) — [RBO paper PDF](http://www.williamwebber.com/research/papers/wmz10_tois.pdf)
- RBO under truncation is the key property: partial evaluation gives `RBO_MIN` (a tight lower bound,
  monotonically non-decreasing with depth, Eqs. 11–12) and a residual `RBO_RES` to a maximum, so
  "the RBO score can then be quoted either as base+residual or as a min–max range" (Section 4.4).
  Crucially, the base score is depth-dependent and must not be used naively on short lists: "if two
  search engines each only supply 7 results to a query, and the p parameter employed is 0.9, then
  even if both results lists are identical (to the supplied depth), the base RBO score will only be
  0.767. In such situations, base RBO can easily become a measure of result list length, not
  difference." — [RBO paper PDF](http://www.williamwebber.com/research/papers/wmz10_tois.pdf)
- The point estimate for a truncated list is **`RBO_EXT(S,T,p,k) = (X_k/k) · p^k + ((1−p)/p) · Σ_{d=1}^{k} (X_d/d) · p^d`**
  (Eq. 23). It assumes "that the degree of agreement seen at k is expected to continue to higher
  ranks". `RBO_EXT` "is not monotonic; it could either increase or decrease as the prefix lengthens"
  but "will always increase with increasing agreement and decrease with decreasing agreement", and
  it is bounded by `RBO_MIN` and `RBO_MAX` (Section 4.4) — [RBO paper PDF](http://www.williamwebber.com/research/papers/wmz10_tois.pdf)
- **RBO under ties** (Section 4.6, the single most relevant detail for equal holdings values): "Ties
  may be handled by assuming that, if t items are tied for ranks d to d + (t − 1), they all occur at
  rank d. To support this, we modify the definition of agreement given in Equation 3:
  **`A_{S,T,d} = 2 · X_{S,T,d} / (|S_{:d}| + |T_{:d}|)`** (Eq. 28). Equations 3 and 28 are equivalent
  in the absence of ties extending over rank d, but in the presence of such ties, the former
  formulation can lead to agreements greater than 1." — [RBO paper PDF](http://www.williamwebber.com/research/papers/wmz10_tois.pdf)
- **RBO on unequal-length lists** (same section): "The formula for RBO_MIN given in Equation 11
  handles uneven rankings without modification, since it is implicitly assumed that ∀d ∈ {s+1,…,l},
  S_d ∉ L; that is, we assume maximal disjointness". `RBO_MAX` assumes every item in the extension of
  the shorter list matches one in the longer, so `X^max_d − X^min_d = d − s` — [RBO paper PDF](http://www.williamwebber.com/research/papers/wmz10_tois.pdf)
- `1 − RBO` is a metric (proved in Section 4.5); RBO itself is a similarity, not a distance — [RBO paper PDF](http://www.williamwebber.com/research/papers/wmz10_tois.pdf)
- Tie handling in implementations has been a known trap: the widely used Python implementation notes
  "The definition of overlap has been modified to account for ties. Without this, results for lists
  with tied items were being inflated", and that this modification is *not* in the paper's own code —
  [dlukes/rbo](https://github.com/dlukes/rbo). The Rust `rbo` crate (MIT, v0.2.7, Aug 2022) says it
  was checked against Webber's original reference code — [crate docs](https://docs.rs/crate/rbo/0.2.7)

**Graded-relevance measures applied to entity lists**

- nDCG and ERR need no modification to run on an entity list; they only need a graded label per
  ranked item. `ir_measures` describes nDCG as using "graded labels — systems that put the highest
  graded documents at the top of the ranking", normalised "against the ideal ranking, sorted by
  descending graded label", and ERR as "a precision-focused measure" that "extends reciprocal rank
  to graded relevance and uses a cascade model of how users scan a ranking" — [ir_measures measures reference](https://ir-measur.es/en/latest/measures.html)
- The TREC Entity track did exactly this with entities (see the IR-precedent section below for the
  qrels format): "The main evaluation measure we use is NDCG@R; that is, the normalized discounted
  cumulative gain at rank R (the number of primaries and relevants for that topic) where a record
  with a primary gets gain 2, and a record with a relevant gets gain 1. We also report on P@10, the
  fraction of records in the first ten ranks with a primary." — [TREC 2009 Entity track overview, Section 2.3.3](https://trec.nist.gov/pubs/trec18/papers/ENT09.OVERVIEW.pdf)

### Inferences

- For a top-K entity answer, the set measure and the order measure are answering genuinely different
  questions, and "9 of 10 members right in the wrong order" is exactly the case that separates them:
  set F1 would score 0.9/0.9, while RBO would be well below 1. Reporting both is the only way to
  make that distinction legible.
- Because holdings values can tie, any order measure chosen here must be one with a published tie
  rule. RBO has one (Eq. 28) and tau_AP has one only via the Urbano & Marrero variants; plain
  Kendall's tau and plain tau_AP do not. That argues for RBO over tau_AP for this project.
- RBO's residual is structurally the same honesty device as `Judged@4`: it reports the part of the
  comparison that the available data cannot settle, instead of silently assuming it.

### Gaps

- I did not find a published application of **ERR** to an entity or holdings list specifically; the
  `ir_measures` definition is generic and the TREC Entity track used nDCG@R, R-Precision and MAP,
  not ERR — [ir_measures](https://ir-measur.es/en/latest/measures.html), [TREC 2010 Entity overview](https://trec.nist.gov/pubs/trec19/papers/ENTITY.OVERVIEW.pdf)
- I did not find a primary source giving a published convention for **Jaccard** on ranked answer sets
  in an IR/QA benchmark. It is standard set similarity (|A∩B|/|A∪B|) but I could not cite a benchmark
  that adopts it as a primary answer metric, so it should be treated as available but unprecedented
  in this setting.
- The exact nDCG tie-breaking convention (what a scorer does when two gold entities have equal gain
  and the system orders them arbitrarily) is not stated on the `ir_measures` page — [ir_measures](https://ir-measur.es/en/latest/measures.html)

## Numeric answers with tolerance: what published benchmarks actually do

### Takeaway

There is no single convention, and the most-cited financial benchmark is stricter than its reputation
suggests: FinQA's released evaluation code uses **exact equality after rounding the executed program
result to 5 decimal places**, with no relative tolerance at all. Claims of a "1% tolerance" in FinQA
circulate in secondary literature and I could not confirm them against the primary artefacts.

### Cited Findings

- **FinQA** (Chen et al., 2021) — the official evaluation code compares with exact equality:
  execution accuracy is `if exe_res == gold_res:`, and the only precision handling anywhere in the
  path is `this_res = round(this_res, 5)` applied inside `eval_program` to the predicted result; the
  gold answer is compared as stored, without rounding. Program accuracy does not compare numbers at
  all — it is symbolic: `return sym_prog1 == sym_prog2` after `simplify(..., evaluate=False)` —
  [czyssrs/FinQA evaluate.py](https://raw.githubusercontent.com/czyssrs/FinQA/master/code/evaluate/evaluate.py)
- A 2026 preprint attributes a ~1% relative tolerance to FinQA/Chen et al. 2021, and a different 2026
  preprint instead describes a FinQA-style rule that normalises formats, requires matching signs, and
  compares after rounding to the gold or predicted decimal places "with no tolerance beyond that
  rounding". These two are in direct conflict and neither matches the released code, so the 1% figure
  should be treated as unverified folklore — conflicting secondary accounts surfaced via search over
  [arXiv financial-QA preprints](https://arxiv.org/pdf/2605.05409); primary code says exact equality:
  [FinQA evaluate.py](https://raw.githubusercontent.com/czyssrs/FinQA/master/code/evaluate/evaluate.py)
- **TAT-QA** (Zhu et al., ACL 2021) uses EM and F1 and makes *scale* a separately scored prediction
  target rather than a tolerance: "A right prediction of a numerical answer should not only include
  the right number but also the correct scale", with scales drawn from None, Thousand, Million,
  Billion, Percent, and the predicted scale combined with the numeric output before comparison with
  gold (Section 2, Table/§2-7) — [TAT-QA paper](https://arxiv.org/pdf/2105.07624)
- The F1 that TAT-QA and its successors inherit is DROP's **numeracy-focused F1**: F1 is set to 0
  when the gold and predicted answers contain different numbers, "regardless of other word overlap";
  if the gold label is a number the prediction must contain that number irrespective of token
  overlap — [DROP, Dua et al. 2019](https://arxiv.org/pdf/1903.00161)
- **BIRD** (Li et al., NeurIPS 2023) scores the *result set*, not the number, with exact set equality
  (details in the next section) — [BIRD paper](https://arxiv.org/pdf/2305.03111), [HTML v3](https://arxiv.org/html/2305.03111v3)
- **DocFinQA** (Reddy et al., ACL 2024 short) extends FinQA questions to full SEC filings — average
  context grows from under 700 words to about 123k words — and reports an `ACC` accuracy metric
  alongside retrieval hit rate `HR`. Best retrieval-pipeline result reported is GPT-3.5 at ~42.6%;
  fine-tuned ColBERT reached HR@1 0.35 and HR@3 0.55; GPT-4 scored 46.5% on documents up to 100K
  tokens and 23.0% on the longest documents — [DocFinQA HTML](https://arxiv.org/html/2401.06915v1), [ACL Anthology 2024.acl-short.42](https://preview.aclanthology.org/add_acl24_videos/2024.acl-short.42)

### Inferences

- For a finance project the FinQA code is the defensible precedent to cite if you want exactness, and
  TAT-QA is the defensible precedent if you want to score *scale* separately (which matters a great
  deal for 13F values, where thousands-vs-dollars is the commonest error and is a different fault
  from a wrong row set).
- Because the published practice is split and poorly documented, any tolerance this project adopts
  has to be stated as its own rule rather than attributed to a benchmark.

### Gaps

- **DocFinQA's exact numeric-matching rule is unverified.** I could not find a statement of its
  tolerance; the HTML I located reports the ACC metric but the search did not surface the matching
  rule — [DocFinQA](https://arxiv.org/html/2401.06915v1)
- I did not verify the FinQA paper's own prose about execution accuracy; the arXiv abstract page does
  not define any metric — [FinQA abstract](https://arxiv.org/abs/2109.00122). The code is the only
  primary artefact I verified.
- I found no benchmark using **binned** numeric scoring. If one exists I did not locate it.

## Execution accuracy versus answer match

### Takeaway

The standard metrics are exact-set-match accuracy (syntax-level, clause-wise), execution accuracy
(result-level), and BIRD's valid-efficiency score (result-level gated, weighted by speed). All three
share one blind spot that is published and quantified: a query that returns the right rows for the
wrong reason — a *spurious* query — counts as correct, and the fix is to execute against multiple
perturbed databases.

### Cited Findings

- **Execution accuracy (EX)**, BIRD Section 5: "the proportion of examples in the evaluation set for
  which the executed results of both the predicted and ground-truth SQLs are identical, relative to
  the overall number of SQLs", i.e. `EX = (Σ_{n=1}^{N} 𝟙(V_n, V̂_n)) / N` where the indicator is 1 iff
  `V_n = V̂_n`. `V_n` is the result set of the ground-truth SQL and `V̂_n` that of the prediction; the
  comparison is exact set equality and the main text specifies no row-ordering or tolerance rule —
  [BIRD, arXiv 2305.03111, NeurIPS 2023 Datasets & Benchmarks, §5](https://arxiv.org/html/2305.03111v3)
- **Valid Efficiency Score (VES)**, BIRD Section 5 Eq. 4: `VES = (Σ_{n=1}^{N} 𝟙(V_n, V̂_n) · R(Y_n, Ŷ_n)) / N`
  with `R(Y_n, Ŷ_n) = sqrt(E(Y_n) / E(Ŷ_n))`, where `E(·)` "measure[s] the absolute execution
  efficiency for each SQL in a given environment". VES "is designed to measure the efficiency of valid
  SQLs generated by models" and "Any SQL queries that fail to fetch the correct values will be
  declared invalid" — so correctness is a hard gate and speed only modulates credit already earned —
  [BIRD §5](https://arxiv.org/html/2305.03111v3)
- **Exact set match accuracy** is the syntax-side metric and is explicitly *not* string comparison:
  Spider "decompose[s] each SQL into several clauses, and conduct[s] set comparison in each SQL
  clause" — [Spider task page](https://yale-lily.github.io/spider). Spider's own leaderboard notes
  "Our current models do not predict any value in SQL conditions so that we do not provide execution
  accuracies", which is why exact set match existed at all — [same](https://yale-lily.github.io/spider)
- **The right-rows-wrong-reason problem, named and fixed.** Zhong, Yu & Klein, "Semantic Evaluation
  for Text-to-SQL with Distilled Test Suites" (EMNLP 2020) propose **test-suite accuracy** as a
  tractable proxy for semantic accuracy: distil a small set of databases that achieves high code
  coverage of the gold query from a large pool of randomly generated databases, then score the
  prediction on its execution results across that whole suite. They evaluated 21 Spider leaderboard
  models, manually checked 100 disagreements with the standard metric and found their method correct
  in every case, and report that Spider's standard metric had a **2.5% false-negative rate on average
  and 8.1% in the worst case**. Released suites cover eleven text-to-SQL datasets, and the repository
  states the metric is now the official one for Spider, SParC and CoSQL —
  [arXiv 2010.02840](https://ar5iv.labs.arxiv.org/html/2010.02840), [alphaXiv listing](https://alphaxiv.org/abs/2010.02840), [taoyds/test-suite-sql-eval](https://github.com/taoyds/test-suite-sql-eval), [EMNLP 2020 anthology](https://preview.aclanthology.org/setup/2020.emnlp-main.29)
- **Why spurious queries arise.** The Stack Exchange text-to-SQL paper calls queries that are wrong
  but return the gold answer on the test database "spurious", notes they inflate the apparent
  correct count, and cites running predictions on modified databases as the remedy. An incomplete
  `WHERE` clause passes whenever every relevant test record happens to satisfy the missing condition,
  and sparse test databases make this worse because many wrong queries return empty results that look
  correct — [Text-to-SQL in the Wild, arXiv 2106.05006](https://arxiv.org/pdf/2106.05006)
- **Known gold-data errors in BIRD.** A June 2026 blog post reports that twenty-two gold SQL queries
  in BIRD are outright wrong. This is a vendor/independent blog, not peer-reviewed, and I did not
  verify the count — [beancount.io research log, 2026-06-06](https://beancount.io/bean-labs/research-logs/2026/06/06/bird-benchmark-text-to-sql-real-database-gap)

### Inferences

- Execution accuracy is a *necessary* but not sufficient check for a structured-layer answer: it
  cannot distinguish "the system computed the right set" from "the system computed a wrong set that
  happens to coincide on this corpus". The cheap, established mitigation for a single-corpus project
  is the test-suite idea in miniature — evaluate the same question against more than one period or
  more than one filer slice and require both to pass.
- VES is irrelevant to a blog series measuring retrieval quality; it measures latency, not
  correctness, and would only muddy a ratchet of absolute rules.

### Gaps

- BIRD's main text does not state whether its set comparison is order-sensitive. Secondary sources
  assert set-based equality ignoring row order unless an `ORDER BY` is specified, but I could not
  confirm that from the paper — [BIRD §5](https://arxiv.org/html/2305.03111v3)
- The FLEX LLM-judge-based alternative to execution accuracy (reported to raise agreement with human
  judgement and shift leaderboard rankings on Spider and BIRD) surfaced only as a search snippet;
  I did not fetch and verify its figures — [arXiv 2409.19014](https://arxiv.org/abs/2409.19014v1)

## Does anything in the IR tradition already cover this? (the reusable qrels precedent)

### Takeaway

Yes, and it is the most directly reusable thing in this whole note: the **TREC Entity track adapted
the qrels format for entity lists by adding an equivalence-class column**, so that one conceptual
entity with several surface forms (several homepages, several names) collapses to one gold answer at
scoring time. That is exactly the problem a 13F-derived company list has — the same company appears
under several CUSIPs, issuer-name spellings and CIKs.

### Cited Findings

**TREC Entity track (2009), related entity finding — the qrels adaptation**

- Entity identity was defined operationally, not ontologically: "A web entity is uniquely
  identifiable by one of its primary homepages. Real-world entities can be represented by multiple
  homepages; a clearly preferred one cannot always be given. As a work-around, **entity resolution is
  addressed at evaluation time**" (Section 2.3) — [TREC 2009 Entity track overview](https://trec.nist.gov/pubs/trec18/papers/ENT09.OVERVIEW.pdf)
- Two-phase assessment: "All runs were pooled down to 10 records, and for each record entry,
  judgments were made for the homepage (HP and WP) and the name (NAME) fields." Then "Assessors
  matched primary pages (HP and WP) to correct names, creating a set of equivalence classes for the
  right answers to each topic (i.e., addressing the resolution of entities)" (Section 2.3.1) —
  [same](https://trec.nist.gov/pubs/trec18/papers/ENT09.OVERVIEW.pdf)
- Graded judgments, three points: "Homepages were judged on a three-point relevance scale: (0)
  non-relevant, (1) relevant ('descriptive') or (2) primary ('authoritative')." Names were likewise
  judged on a three-level scale (Section 2.3.1) — [same](https://trec.nist.gov/pubs/trec18/papers/ENT09.OVERVIEW.pdf)
- **The 2009 qrels format, quoted exactly** (Section 2.3.2): "In the qrels file, the fields are:
  `topic-entry_type docid_or_name rel class`. Where topic-entry type denotes the topic ID (first
  half) and the field (second half), e.g., '1-HP' is the HP field for topic 1; docid or name is a
  document ID (for fields HP1..HP3 and WP) or a name (for field NAME); rel is {0, 1, 2} as described
  above; and **class is an integer value, where lines with the same topic number and class correspond
  to the same entity**" — [same](https://trec.nist.gov/pubs/trec18/papers/ENT09.OVERVIEW.pdf)
- Metrics (Section 2.3.3): NDCG@R with R = number of primaries plus relevants for the topic, gain 2
  for primary and 1 for relevant; plus P@10, "the fraction of records in the first ten ranks with a
  primary". Explicit note: "evaluation results are not computed using the standard trec eval tool,
  but a script developed specifically for the 2009 edition of the Entity track", published at
  `http://trec.nist.gov/data/entity/09/eval-entity.pl` — [same](https://trec.nist.gov/pubs/trec18/papers/ENT09.OVERVIEW.pdf)
- Five alternative gain schemes were reported alongside the default, varying which identifier fields
  count (HP-only default; HP+NAME with "an extra gain of 1 … if an exact name is returned along with
  a primary homepage", max gain 3; WP-only; HP+WP; HP+WP+NAME) — [same, §3.1](https://trec.nist.gov/pubs/trec18/papers/ENT09.OVERVIEW.pdf)
- 2009 scale and difficulty: 20 topics; 13 groups, 41 runs; best NDCG@R 0.3061 (KMR1PU, Purdue) with
  P@10 0.2350 — [same, Table 1](https://trec.nist.gov/pubs/trec18/papers/ENT09.OVERVIEW.pdf)

**TREC Entity track (2010) — the format and metrics changed slightly, which is itself instructive**

- 2010 qrels (Section 2.2.1): "In the qrels file, the fields are: `topic doc name rel class rel_name`
  … topic denotes the topic ID …, doc is a ClueWeb document ID, name is the normalized name of the
  entity, rel is the judgment for the document (0, 1, or 2), class is a class number for the
  document, and rel name is the judgment for the name." Plus: "primary homepages are grouped
  together; primary documents in the same class are equivalent, and correct names for them are all
  valid" — [TREC 2010 Entity track overview](https://trec.nist.gov/pubs/trec19/papers/ENTITY.OVERVIEW.pdf)
- 2010 metrics (Section 2.2.2): "The main evaluation measure we use is NDCG@R … where **primary
  homepages get gain 3 and relevant homepages get gain 1**. We also report on R-Precision (precision
  at rank R), and Mean Average Precision, both computed over primary pages only." (Note the gain for
  primary changed from 2 in 2009 to 3 in 2010.) — [same](https://trec.nist.gov/pubs/trec19/papers/ENTITY.OVERVIEW.pdf)
- The track checked whether the metric choice mattered: "The Kendall tau rank coefficients indicate
  very strong correlation between the rankings of participating systems using various metrics (0.92
  for NDCG@R vs. MAP, 0.94 for NDCG@R vs. rPrec, and 0.94 for MAP vs. rPrec)" — [same](https://trec.nist.gov/pubs/trec19/papers/ENTITY.OVERVIEW.pdf)
- 2010 scale: 14 groups, 48 runs; best automatic NDCG@R 0.3694 (bitDSHPRun, BIT) with MAP 0.2726 and
  rPrec 0.3075; best manual 0.3897 — [same, Tables 1–2](https://trec.nist.gov/pubs/trec19/papers/ENTITY.OVERVIEW.pdf)

**TREC QA list questions (already quoted above)** — set P/R/F1 over "known instances", the
order-free precedent — [TREC 2004 QA overview §2.2](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf)

**TREC Genomics aspect-level MAP** — a third shape: grouping retrieved passages by *aspect* so that
each contributes distinct information, then scoring with MAP-like measures at document, passage and
aspect level. Example official run scores from the TREC 2006 appendices: UniNE3 aspect MAP 0.226
against document MAP 0.373; NTUadh2 aspect MAP 0.235; i2rg063 aspect MAP 0.080 —
[TREC 2007 Genomics plenary overview](https://dmice.ohsu.edu/hersh/genomics/trec-07-plenary.pdf),
[UniNE3 appendix](https://trec.nist.gov/pubs/trec15/appendices/genomics/UniNE3.main.pdf),
[NTUadh2 appendix](https://trec.nist.gov/pubs/trec15/appendices/genomics/NTUadh2.main.pdf)

### Inferences

- The Entity-track `class` column is the single reusable mechanic for this project: a gold record for
  "top 10 companies exposed to Y" wants one row per *company*, with an equivalence class that lists
  every acceptable identifier (CIK, CUSIP set, issuer-name variants) so that a correct answer spelled
  differently is still correct. This is a strict superset of the existing
  `question 0 passage relevance` qrels line — it adds one column, and leaves existing lines valid.
- The Entity track's two changes between 2009 and 2010 (gain 2 → 3 for primary; adding a separate
  `rel_name` judgment) are a warning: gain weights and identifier judgments are policy, not fact.
  A ratchet of absolute rules should freeze them explicitly.
- The 0.92–0.94 Kendall tau between NDCG@R, MAP and rPrec on the Entity track is evidence that for
  entity lists, *which* ranked measure you pick matters much less than whether your gold set is
  complete. That supports spending the effort on completeness, not on metric selection.

### Gaps

- **TREC Table/Dataset search metrics were not verified.** I did not locate a primary overview for a
  TREC table-search or dataset-search task with its qrels format and metrics, nor for the NTCIR Data
  Search task. Report this as unresearched rather than absent.
- **TREC expert finding (Enterprise track) was not verified.** I did not fetch an Enterprise track
  overview, so I cannot state its qrels format (candidate-ID-keyed qrels) or its metrics (commonly
  cited as MAP/MRR/P@k) from a primary source. Starting point: [TREC Enterprise track data](https://trec.nist.gov/data/enterprise.html)
- The TREC Genomics **aspect-level AP formula** itself is not quoted here; the search surfaced run
  scores and the task description but the exact computation was cut off — [TREC 2007 Genomics overview](https://dmice.ohsu.edu/hersh/genomics/trec-07-plenary.pdf)

## Aggregation-specific correctness traps

### Takeaway

Scoring an aggregate only on its final number is unsafe for four distinct reasons, and for 13F
specifically the double-counting trap is not a modelling nicety — it is written into the form's own
instructions. Form 13F's "Rules to Prevent Duplicative Reporting" plus the Column 7 "Other Managers"
mechanism mean a naive sum over filings can both over-count and under-count the same economic
holding. I found **no published evaluation practice** that scores an aggregate answer's supporting
rows; this is the clearest unevaluated area in the whole note.

### Cited Findings

**The regulatory mechanics that create the traps (primary source: the form itself)**

- **Duplicative reporting is designed into 13F** (General Instruction 2, "Rules to Prevent Duplicative
  Reporting"): "If two or more Managers, each of which is required by rule 13f-1 to file a report on
  Form 13F for the reporting period, exercise investment discretion with respect to the same
  securities, **only one such Manager must include information regarding such securities in its
  reports on Form 13F**." And: "A Manager having securities over which it exercises investment
  discretion that are reported by another Manager (or Managers) must identify the Manager(s) reporting
  on its behalf" — [SEC Form 13F](https://www.sec.gov/files/form13f.pdf)
- **Three report types, and only one of them is self-contained** (Special Instruction 5): a "13F
  NOTICE" omits both the Summary Page and the Information Table entirely because all its holdings are
  reported by someone else; a "13F HOLDINGS REPORT" contains everything; a "13F COMBINATION REPORT"
  is the dangerous middle case where "only part of the securities … is reported by another Manager
  (or Managers)" and it includes both the Other Managers list *and* an Information Table —
  [SEC Form 13F](https://www.sec.gov/files/form13f.pdf)
- **Column 7 is the shared-discretion pointer, not a quantity split** (Special Instruction, Column 7.
  Other Managers): "Identify each other Manager on whose behalf this Form 13F report is being filed
  with whom investment discretion is shared as to any reported holding by entering in this column the
  number assigned to the Manager in the List of Other Included Managers… A Manager must report the
  conditions of sharing discretion with other Managers consistently for all holdings reported on a
  single line." — [SEC Form 13F](https://www.sec.gov/files/form13f.pdf)
- **The filing's own control totals exist and can be used as a cross-check** (Special Instruction 6):
  "Enter as the Information Table Entry Total the total number of line entries providing holdings
  information included in the Information Table" and "Enter as the Information Table Value Total the
  aggregate fair market value of all holdings reported in this report, i.e., the total for Column 4
  (Fair Market Value) of all line entries" — [SEC Form 13F](https://www.sec.gov/files/form13f.pdf)
- **A legal de-minimis hole in any 13F-derived gold set** (Special Instruction 9): "A Manager may omit
  holdings otherwise reportable if the Manager holds, on the period end date, fewer than 10,000 shares
  (or less than $200,000 principal amount in the case of convertible debt securities) and less than
  $200,000 aggregate fair market value" — [SEC Form 13F](https://www.sec.gov/files/form13f.pdf)
- The reporting threshold that bounds who files at all: managers with "an aggregate fair market value
  on the last trading day of any month of any calendar year of at least $100,000,000" must file,
  within 45 days of each quarter end (General Instruction 1) — [SEC Form 13F](https://www.sec.gov/files/form13f.pdf)
- A worked example of the double-counting trap, with the two symmetric errors named: a Berkshire
  Hathaway holdings row for Ally Financial with 2,803,875 shares carrying three other-manager
  references, where "the three references don't turn one share quantity into three separate blocks".
  The two failure modes identified are (a) dropping rows because their security identifiers match,
  which "can remove real reported quantities", and (b) "expanding each row into a separate record for
  every linked manager, then adding those records", which "can count the same reported block
  repeatedly". **Caveat: this is a vendor blog (updated October 2026), not peer-reviewed or
  regulatory** — [Altss research blog](https://altss.com/blog/13f-shared-discretion-berkshire-national-indemnity-reporting)
- Examples of the disclosure structure in real filings (showing the Other Managers list, not
  discussing counting): [Loomis Sayles 13F-HR FY2000](https://www.sec.gov/Archives/edgar/data/0000109880/000092701600002968/0001.txt),
  [Wynnefield Capital 13F-HR FY2005](https://www.sec.gov/Archives/edgar/data/0001056835/000091142005000328/d1109878.txt)

**The database-theory side: aggregate provenance is a known hard case**

- Amsterdamer, Deutch & Tannen (2011) argue that semiring-based tuple annotation breaks down for
  aggregate queries, so values as well as tuples must be annotated — i.e. row-level provenance alone
  does not explain an aggregate — [arXiv 1101.1110 summary](https://www.emergentmind.com/papers/1101.1110)
- ProvSQL's developer documentation states that a grouped row's provenance combines its contributing
  tuples but that naive summing "can over-count how often a row exists", requiring a dedicated δ
  operator — [ProvSQL aggregation docs](https://api.pgxn.org/src/provsql/provsql-1.12.0/doc/source/dev/aggregation.rst), [query-rewriting docs](https://api.pgxn.org/src/provsql/provsql-1.2.3/doc/source/dev/query-rewriting.rst)
- Work on "counterbalancing" explains surprising aggregate results using data *outside* the
  provenance, which pure provenance methods miss — [NSF PAR 10278465](https://par.nsf.gov/biblio/10278465)
- Practical cost warning: storing every source row ID beside every output row inflates metadata
  quickly under joins and aggregations; tiered evidence is recommended instead. (Vendor guide,
  September 2026, not peer-reviewed) — [oneuptime blog](https://oneuptime.com/blog/post/2026-09-07-row-level-data-provenance-storage/view)

**Why the final number alone is not enough — the transferable published result**

- The spurious-query result from the text-to-SQL literature is the cleanest published statement of
  "right total from wrong rows": a prediction that returns the gold result on the test database can
  be semantically wrong, the standard metric rewards it, and sparse data makes it worse because wrong
  queries returning empty results look correct — [Text-to-SQL in the Wild](https://arxiv.org/pdf/2106.05006).
  The established fix is multiple perturbed databases — [Zhong, Yu & Klein 2020](https://ar5iv.labs.arxiv.org/html/2010.02840)

### Inferences

- The four traps map onto distinct, separately-measurable faults, and conflating them is what makes a
  single aggregate number useless as a diagnostic:
  1. *Right total, wrong rows* — detectable only by scoring the supporting row set, or by re-running
     the aggregate on a perturbed slice (the test-suite idea).
  2. *Double counting* — specific to 13F combination reports and Column 7; detectable by checking the
     answer against the filing's own Information Table Value Total and Entry Total control figures,
     which the form requires the filer to supply.
  3. *Missing rows producing a smaller but plausible total* — undetectable from the number alone
     because plausibility is preserved; detectable only against a complete gold row set, or bounded
     by the de-minimis rule (holdings under 10,000 shares and under $200,000 may be legally absent
     from the source, so they can never be in a gold set built from 13F alone).
  4. *"The set is wrong" versus "the extraction was wrong"* — these need two separate scores: a set
     score over entities and a value score over the numbers attached to them. One combined number
     cannot separate them.
- Trap 2 has an unusually cheap check here: the SEC form mandates self-reported control totals, so
  an aggregate built from the Information Table can be reconciled against the filer's own stated
  totals without any human judgement. I have not seen this used as an evaluation metric and would
  class adopting it as design, not practice.

### Gaps

- **I found no published evaluation practice that scores provenance or row-level grounding for an
  aggregate answer in a QA/RAG benchmark.** The provenance literature I found is database-theoretic
  (how to compute provenance for aggregates) and the QA literature I found scores only final answers
  or final-answer attribution for prose claims. Flag this to the reader as substantially unevaluated.
  Searched and surfaced only DB-theory results: [arXiv 1101.1110](https://www.emergentmind.com/papers/1101.1110), [ProvSQL](https://api.pgxn.org/src/provsql/provsql-1.12.0/doc/source/dev/aggregation.rst)
- I did not find a peer-reviewed treatment of 13F double counting with a measured error rate; the only
  analytical source located is a vendor blog — [Altss](https://altss.com/blog/13f-shared-discretion-berkshire-national-indemnity-reporting)
- Forms 13D and 13G were named in the brief but I did not research their specific duplicate-reporting
  or group-filing mechanics (Schedule 13D/G group filings under §13(d)(3) raise an analogous
  "one holding, several filers" problem). Record as unresearched.

## Completeness is the hard part: how to measure and report it

### Takeaway

The IR tradition's answer is not "build a complete gold set" — it is "declare how incomplete yours
is, and use measures designed to survive it." The three published tools are pooling with a stated
pool depth, the `Judged@k` family, and the condensed/inferred measures bpref and infAP. For top-K
aggregate answers the most honest analogue is a per-question completeness flag plus a residual, in
the same spirit as RBO's min–max range.

### Cited Findings

- **Pooling with a declared depth is the established construction.** The TREC Entity track's pool was
  stated exactly: "All runs were pooled down to 10 records, and for each record entry, judgments were
  made for the homepage (HP and WP) and the name (NAME) fields" — [TREC 2009 Entity overview §2.3.1](https://trec.nist.gov/pubs/trec18/papers/ENT09.OVERVIEW.pdf)
- **TREC's list-question gold set was explicitly a set of *known* instances**, not a claimed-complete
  one: instance recall is `IR = D/S` where "S be the the number of known instances". The wording is
  the honesty device — recall is measured against what is known, and the metric name does not
  overclaim — [TREC 2004 QA overview §2.2](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf)
- **bpref** (Buckley & Voorhees, "Retrieval Evaluation with Incomplete Information", SIGIR 2004,
  pp. 25–32, DOI 10.1145/1008992.1009000): the paper tests whether Cranfield-style evaluation holds
  up when judgments are missing, finds common measures become unreliable under substantial
  incompleteness, and introduces bpref, which tracks average precision closely when judgments are
  complete and degrades more gracefully when they are not — [webis anthology record](https://ir.webis.de/anthology/2004.sigirconf_conference-2004.5/), [NIST record with abstract](https://www.nist.gov/node/761376), [full PDF](https://corpora.tika.apache.org/base/docs/govdocs1/022/022403.pdf)
- bpref's mechanism, as `ir_measures` states it: "This measure examines the relative ranks of judged
  relevant and non-relevant documents" and **it ignores unjudged documents entirely** — [ir_measures](https://ir-measur.es/en/latest/measures.html).
  A corollary noted in the follow-up literature: bpref penalises a system when it ranks a judged
  non-relevant document above a judged relevant one, independently of how unjudged documents are
  ranked — [as summarised alongside the inferred-AP work](https://www-nlpir.nist.gov/projects/tv2006/infap/inferredAP.pdf)
- **infAP** (Yilmaz & Aslam, "Estimating Average Precision with Incomplete and Imperfect Judgments",
  CIKM 2006): "AP implementation that accounts for pooled-but-unjudged documents"; it treats unjudged
  documents as non-relevant and skips pooled-but-unjudged ones, which are marked with a score of −1 —
  [ir_measures](https://ir-measur.es/en/latest/measures.html), [NIST inferred-AP paper](https://www-nlpir.nist.gov/projects/tv2006/infap/inferredAP.pdf), [journal version, Inf Retrieval 2008](https://link.springer.com/doi/10.1007/s10791-008-9059-7)
- **`Judged@k`** is already the right shape and is defined generically: "Percentage of results in the
  top k (cutoff) results that have relevance judgments", equivalent to P@k with a relevance threshold
  below any judgment — [ir_measures](https://ir-measur.es/en/latest/measures.html)
- **RBO's residual is the rank-side equivalent of `Judged@k`**: on partial evaluation the measure is
  quoted "either as base+residual or as a min–max range", and "The minimum, maximum, and residual
  scores on partial RBO evaluation are all monotonic in depth" — [RBO paper](http://www.williamwebber.com/research/papers/wmz10_tois.pdf)
- **Judging-error reporting as a practice.** TREC deliberately did not normalise away assessor
  disagreement on list questions: "NIST does not adjust the judgments to make identical runs match
  because then we wouldn't know what the naturally occurring error rate was, and doing so would bias
  the scores of systems that submitted identical component runs" — [TREC 2004 QA overview §2.2](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf)
- **A structural completeness bound specific to 13F.** Because of Special Instruction 9, holdings of
  fewer than 10,000 shares *and* under $200,000 market value may be omitted by the filer, and
  because of General Instruction 1 managers below $100,000,000 need not file at all. A gold set built
  from 13F is therefore provably incomplete at the tail by construction, and that incompleteness is
  quantifiable and citable rather than merely suspected — [SEC Form 13F](https://www.sec.gov/files/form13f.pdf)

### Inferences

- For a top-K question, the asymmetry is the whole problem: **recall against a known-instance list is
  honest; precision is not**, because a system answer not in the gold set is indistinguishable from a
  gold-set omission. TREC's `IR = D/S` phrasing concedes this by calling S "known instances". The
  analogue of `Judged@4` for a top-K answer is therefore a per-question statement of how many of the
  system's K returned members were *adjudicated at all*.
- bpref and infAP are not directly reusable here, because they are AP variants over a ranking with
  unjudged holes, and a top-K computed set has a different failure mode (gold-set omissions, not
  unjudged documents). Their transferable lesson is the design principle: define the measure over
  *judged* items only, and report the unjudged fraction separately.
- A completeness check this project can actually run, and which does not exist in the IR literature
  because the IR literature has no filer-supplied control totals: reconcile a computed aggregate
  against the Information Table Entry Total and Value Total that the form requires. Call that a
  completeness *audit*, not a completeness *proof*.

### Gaps

- I did not find a published protocol for building a provably complete gold set for an aggregation
  query. The closest published practices are pooling with a declared depth and TREC's "known
  instances" wording, both of which concede incompleteness rather than eliminating it.
- I did not verify the exact bpref formula from a primary source (the `ir_measures` page gives prose
  only, and I did not extract the formula from the SIGIR PDF). The formula should be taken from
  [Buckley & Voorhees 2004 §4](https://corpora.tika.apache.org/base/docs/govdocs1/022/022403.pdf)
  before publication rather than from memory.

## A concrete recommendation for top-K questions — DESIGN

### Takeaway

Add one gold file in an extended qrels shape (existing columns untouched, two columns added), report
exactly two numbers plus one honesty number, and state incompleteness per question. Everything
structural here has a precedent; the specific combination is my design.

### Cited Findings (what each choice rests on)

- The extended qrels shape with an entity-equivalence column is **established practice**, from the
  Entity track: `topic doc name rel class rel_name`, where "class is an integer value, where lines
  with the same topic number and class correspond to the same entity" —
  [TREC 2009 §2.3.2](https://trec.nist.gov/pubs/trec18/papers/ENT09.OVERVIEW.pdf), [TREC 2010 §2.2.1](https://trec.nist.gov/pubs/trec19/papers/ENTITY.OVERVIEW.pdf)
- Set F1 over distinct instances, with the gold set called "known instances", is **established
  practice**: `IP = D/N`, `IR = D/S`, `F = 2·IP·IR/(IP+IR)` —
  [TREC 2004 QA overview §2.2](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf)
- RBO with an explicit residual, and its tie rule `A_d = 2·X_d/(|S_{:d}| + |T_{:d}|)`, is
  **established practice** for truncated non-conjoint top-K lists — [RBO, §§4.1–4.6](http://www.williamwebber.com/research/papers/wmz10_tois.pdf)
- Scoring scale separately from value is **established practice** (TAT-QA: "A right prediction of a
  numerical answer should not only include the right number but also the correct scale") —
  [TAT-QA](https://arxiv.org/pdf/2105.07624)
- Reporting the judged fraction rather than assuming completeness is **established practice**
  (`Judged@k`) — [ir_measures](https://ir-measur.es/en/latest/measures.html)

### The scheme (DESIGN)

**1. Gold record — one file, `topk_qrels`, one line per gold member.**

```
question  class  identifier  identifier_type  rank  value  value_scale  completeness
```

- `question` — the existing question ID, so the file keys the same way as the current qrels.
- `class` — the entity-equivalence class, copied verbatim in spirit from the Entity track: all lines
  sharing (`question`, `class`) are the *same company*, however spelled.
- `identifier` / `identifier_type` — one line per acceptable surface form (CIK, CUSIP, issuer name
  as filed), so the matcher is data, not code. This is the Entity track's "entity resolution is
  addressed at evaluation time" moved into the gold file.
- `rank` — the gold position, with **equal ranks permitted** for genuine ties in holdings value. Ties
  must be representable in the gold record or the tie-aware RBO agreement formula has nothing to work
  with.
- `value` / `value_scale` — the numeric fact attached to the member, scale stored explicitly, so a
  thousands-vs-dollars error scores as a value error and not as a set error.
- `completeness` — per *question*, one of `complete` / `pool-k` / `partial`. `complete` is only
  claimable where the aggregate reconciles against the filer's own Information Table Entry Total and
  Value Total ([SEC Form 13F, Special Instruction 6](https://www.sec.gov/files/form13f.pdf)).

The existing `question 0 passage relevance` lines are untouched and remain the retrieval gold. The
new file is a second artefact, scored by a second scorer; the ratchet on Hit@4/MRR/Recall@4 is
unaffected.

**2. Report exactly two metrics, plus one honesty number.**

- **Set F1 over equivalence classes** — primary. `IP = D/N`, `IR = D/S`, `F = 2·IP·IR/(IP+IR)`, where
  D counts *distinct gold classes* matched, N is the number of distinct classes the system returned,
  S is the number of gold classes with `completeness` ≠ `partial`. This is the TREC list measure,
  unchanged, with the Entity track's class collapsing applied before counting. It is order-blind, so
  it is tie-proof, and it is the number that answers "did it find the right companies".
- **RBO_EXT at p = 0.9 with its residual** — secondary, and only on the subset where the set answer
  is non-empty. `RBO_EXT(S,T,p,k) = (X_k/k)·p^k + ((1−p)/p)·Σ_{d=1}^{k}(X_d/d)·p^d`, with agreement
  computed by the tie-aware Eq. 28. p = 0.9 is chosen because the paper states it puts 86% of the
  weight in the first 10 ranks, which is exactly the K in "top 10 companies"
  ([RBO §4.3](http://www.williamwebber.com/research/papers/wmz10_tois.pdf)). This is the number that
  answers "did it get the order right", and it is the one that distinguishes 9-of-10-misordered from
  9-of-10-in-order. **Report the residual alongside it; never report the point estimate alone**, for
  the reason the paper gives — on short lists base RBO becomes a measure of list length.
- **`Adjudicated@K`** (my name, by analogy with `Judged@4`) — the fraction of the K members the
  system returned that appear in the gold file at all, in any class, as relevant or non-relevant.
  This is the direct analogue of `Judged@4`: it says how much of the answer the gold set was able to
  rule on. A high set F1 with a low `Adjudicated@K` is an unverifiable result and should be labelled
  as one.

**3. Partial credit, stated rather than smoothed.** Report set F1 and RBO as two numbers, not a
composite. The project already resists composite scores; a weighted blend of membership and order
would hide exactly the distinction the brief asks for. (TREC did blend components —
`FinalScore = .5·FactoidAccuracy + .25·ListAveF + .25·OtherAveF`
— [TREC 2004 §2.4](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf) — but that was to rank
participants, not to diagnose a system.)

**4. Incompleteness statement.** For each top-K question, publish the `completeness` value and, where
it is not `complete`, the reason. Two reasons are structural and citable rather than apologetic: the
de-minimis omission allowance (under 10,000 shares *and* under $200,000 may be legally absent) and
the $100,000,000 filing threshold — [SEC Form 13F, General Instruction 1, Special Instruction 9](https://www.sec.gov/files/form13f.pdf).
State plainly that precision against a `partial` gold set is a lower bound, not a measurement.

**5. Two optional extras, both cheap, both design.**

- A **value score** separate from the set score: for members present in both the answer and gold,
  the fraction whose `value` matches after scale normalisation. Use FinQA's exactness
  (`round(x, 5)` then equality — [FinQA evaluate.py](https://raw.githubusercontent.com/czyssrs/FinQA/master/code/evaluate/evaluate.py))
  rather than inventing a tolerance, and if a tolerance is wanted, state it as the project's own rule
  because published practice is split.
- A **miniature test suite**: run each top-K question against a second quarter's filings. If the
  system's set is right in one quarter and wrong in the next in a way the gold set does not explain,
  the first result was probably right-rows-wrong-reason. This is Zhong, Yu & Klein's distilled test
  suite reduced to its cheapest form — [arXiv 2010.02840](https://ar5iv.labs.arxiv.org/html/2010.02840)

### Which parts are established and which are mine

- **Established:** the equivalence-class qrels column; set IP/IR/F over distinct instances; the
  "known instances" framing of recall; RBO with ties and a reported residual; p = 0.9's 86% top-10
  weight; separate scoring of numeric scale; reporting a judged fraction; perturbed-execution testing.
- **Mine (DESIGN):** the specific eight-column `topk_qrels` layout; the `completeness` flag and its
  reconciliation rule against the filer's own control totals; the name and definition of
  `Adjudicated@K`; the refusal to blend set F1 with RBO; the two-quarter mini test suite; the
  decision to report exactly these three numbers.

### Gaps

- I have no published precedent for reconciling an evaluation gold set against a filer's self-reported
  control totals. It follows directly from the form's Special Instruction 6, but I found no paper
  doing it.
- I have no published precedent for a measure named like `Adjudicated@K` applied to a computed set.
  It is `Judged@k` transposed, and should be presented as such.

---

# Part 2: scoring a synthesised consensus (three-stage questions)

Covers "give me the consensus current outlook of all the companies with exposure to company Z":
stage 1 resolves an entity set (scored by Part 1), stage 2 extracts a judgement from each entity's
prose, stage 3 synthesises one consensus. Stages 2 and 3 have no gold answer in the Part 1 sense.

## Scoring a synthesised answer with no gold

### Takeaway

Four families exist and none of them establishes correctness. LLM-as-judge establishes *relative
preference* and carries measured biases large enough to invert a verdict. Reference-free faithfulness
metrics (AIS, RAGAS faithfulness, FActScore, SummaC, QAFactEval) establish *groundedness in the
retrieved context* — which is not the same as being right, and on a consensus question is arguably the
only thing worth claiming. Human pairwise preference establishes what humans prefer, with a measured
ceiling of about 81% human–human agreement. Nugget evaluation establishes *coverage of atomic claims*
and is the only one of the four that decomposes into individually judgeable units.

### Cited Findings

**LLM-as-judge: what it establishes, and the measured biases**

All figures from Zheng et al., "Judging LLM-as-a-Judge with MT-Bench and Chatbot Arena", NeurIPS 2023
— [arXiv 2306.05685](https://arxiv.org/pdf/2306.05685), [HTML v4](https://arxiv.org/html/2306.05685v4):

- **Position bias** (§3.3, Table 2) — consistency, i.e. how often a judge gives the same verdict when
  the two answers are swapped: GPT-4 **65.0%**, GPT-3.5 **46.2%**, Claude-v1 **23.8%**. The paper
  notes "Only GPT-4 outputs consistent results in more than 60% of cases." Few-shot prompting raised
  GPT-4 to **77.5%** (§D.2, Table 13).
- **Verbosity bias** (§3.3, Table 3) — failure rate under the "repetitive list" attack, where an
  answer is made longer without adding information: GPT-4 **8.7%**, GPT-3.5 **91.3%**, Claude-v1
  **91.3%**.
- **Self-enhancement bias** (§3.3, Figure 2(b)) — GPT-4's win rate for its own answers was about
  **10 percentage points** higher than humans gave it; Claude-v1's about **25 points**. The authors
  explicitly caution that "limited data means they cannot determine whether self-enhancement bias
  exists" as a general phenomenon.
- **Agreement with humans** (§4.2, Table 5): GPT-4 pairwise vs. humans **85%** non-tie agreement on
  first turn, against **81%** human–human; **85%** on second turn. Counting ties and inconsistent
  votes (setup S1) drops first-turn agreement to **66%**. On Chatbot Arena (Table 6), GPT-4 vs.
  humans reached **87%** in the non-tie setup. The headline claim is agreement "above 80%", matching
  human-to-human agreement, over 3K controlled expert votes and 3K crowdsourced votes.

What this can establish: which of two synthesised answers a judge prefers, reproducibly, if you
average over swapped positions. What it cannot establish: that either answer is correct; and the
66% figure with ties included is the honest number for a project that cares about absolute rules.

**Reference-free faithfulness and attribution**

- **AIS (Attributable to Identified Sources)** — Rashkin et al., *Computational Linguistics* 49(4),
  2023: a framework for checking whether a generated statement is backed by a provided source, with a
  two-stage human annotation pipeline, released data and guidelines, validated on two conversational
  QA datasets, a summarisation dataset and a table-to-text dataset —
  [ACL Anthology 2023.cl-4.2](https://aclanthology.org/2023.cl-4.2/), [PDF](https://aclanthology.org/2023.cl-4.2.pdf), [Google Research](https://research.google/pubs/pub52156/)
- **AutoAIS evaluators disagree at the instance level even at similar accuracy.** "Verify with
  Caution" re-evaluates five AutoAIS evaluators on LLM-AggreFact and finds that evaluators with
  similar balanced accuracy often disagree per instance, which can change system-level conclusions —
  [arXiv 2501.14883](https://arxiv.org/html/2501.14883v2), [PDF](https://arxiv.org/pdf/2501.14883)
- **RAGAS faithfulness** — the documented definition is a bare claim ratio: "A response is considered
  faithful if all its claims can be supported by the retrieved context", scored as
  `(number of claims in the response supported by the retrieved context) / (total number of claims in
  the response)` — [RAGAS faithfulness docs](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/faithfulness/).
  The documentation page states **no validation against human judgement and gives no agreement
  figures**, including for the HHEM-2.1-Open classifier it mentions.
- **FActScore** — Min et al., EMNLP 2023. For a response y with atomic facts `A_y`:
  `f(y) = (1/|A_y|) Σ_{a ∈ A_y} I[a is supported by C]`, where C is the knowledge source; model-level
  `FActScore(M) = E_{x ∈ X}[f(M_x) | M_x responds]`, where "ℳx responds means ℳ did not abstain from
  responding to the prompt x". An atomic fact is "a short sentence conveying one piece of
  information", labelled Supported / Not-supported / Irrelevant. The automatic estimator's **error
  rate against human-estimated FActScore was 1.4% for InstructGPT, 0.4% for ChatGPT and 0.8% for
  PerplexityAI** (best variant per model, Table 3), summarised in the abstract as "less than a 2%
  error rate" — [arXiv 2305.14251 HTML](https://arxiv.org/html/2305.14251v2)
- **SummaC / QAFactEval** — the SummaC benchmark casts factual consistency as binary classification
  and reports **balanced accuracy** computed "by tuning the threshold of metrics on the validation
  sets, and then computing the balanced accuracy on the test sets", with AUC-ROC alongside.
  QAFactEval reports "a 14% average improvement over previous QA-based metrics on the SummaC factual
  consistency benchmark" and also beats the best entailment-based metric —
  [QAFactEval, NAACL 2022, arXiv 2112.08542](https://arxiv.org/pdf/2112.08542), [ar5iv](https://ar5iv.labs.arxiv.org/html/2112.08542), [anthology](https://preview.aclanthology.org/setup/2022.naacl-main.187)
- In AlignScore's cross-benchmark table, QAFactEval averages **83.8** across CGS, XSF, PolyTope,
  FactCC, SummEval and FRANK, above SummaC-ZS (79.0) and SummaC-CONV (81.0) and below UniEval (84.6) —
  [AlignScore, arXiv 2305.16739](https://arxiv.org/pdf/2305.16739). **Caveat:** the snippet does not
  label whether that column is AUC-ROC or balanced accuracy, so treat the comparison as indicative.
- A documented comparability trap: QAFactEval's authors relabelled part of the PolyTope data —
  "The original SummaC benchmark included the Omission and Addition errors of this proposed typology
  as factual inconsistencies, but these are largely extractive, factually consistent summaries. We
  thus label these examples as factually consistent" — so scores on that subset are not directly
  comparable to the original SummaC setup — [QAFactEval](https://arxiv.org/pdf/2112.08542)

**Human pairwise preference**

- The ceiling is measured and it is not 100%: human–human non-tie agreement was **81%** on MT-Bench
  first turn (§4.2, Table 5). MT-bench questions, 3K expert votes and 30K conversations with human
  preferences are public via FastChat — [Zheng et al.](https://arxiv.org/html/2306.05685v4)

### Inferences

- On a question with no gold consensus, the only claim that can be made rigorously is **groundedness**:
  every clause of the synthesised outlook is supported by a retrieved passage. FActScore's formula is
  the cleanest published way to state that, and it has the best-verified automatic-estimator error
  rate of the options here (under 2%).
- RAGAS faithfulness has the same shape as FActScore but, per its own documentation, no published
  human-agreement figure. For a project with a ratchet of absolute rules, that is a reason to cite
  FActScore's formula and say so, rather than to adopt a library default.
- If LLM-as-judge is used at all, it must be used pairwise with position swapping, and the reported
  number must be accompanied by the 65.0% / 66% consistency figures, because an inconsistency rate of
  that size is larger than most differences a blog post would be trying to report.

### Gaps

- **Rashkin et al.'s human inter-annotator agreement figures are unverified.** The search surfaced the
  framework and its validation tasks but no agreement numbers; they would need to be read from the
  paper or its repository — [ACL Anthology 2023.cl-4.2](https://aclanthology.org/2023.cl-4.2/), repository referenced as `github.com/google-research-datasets/AIS`
- QAFactEval's own balanced-accuracy table was not retrieved; I have only the "14% average
  improvement" claim and a third-party aggregator's Spearman figures (SummEval 42.8, QAGS-XSum 44.1,
  QAGS-CNNDM 63.1) which I was unable to verify against the paper and therefore do not rely on —
  [QAFactEval](https://arxiv.org/pdf/2112.08542)
- SummaC's own paper (Laban et al., TACL 2022) was not fetched; the benchmark description above comes
  from QAFactEval's account of it.

## Nugget-based evaluation: the closest fit, with exact formulas

### Takeaway

Nugget evaluation is the right frame for a consensus answer and it has two usable generations of
formula: TREC's original human nugget pyramid with `F(β=3)` and a character-length proxy for
precision (2003–2004 QA track), and the TREC RAG track's AutoNuggetizer with strict vital/all recall,
`V_strict` as the primary metric, and published Kendall tau agreement with human nugget evaluation of
**0.887 at the run level**.

### Cited Findings

**The TREC nugget tradition — the original definition, quoted**

From *Overview of the TREC 2004 Question Answering Track*, §2.3 ("Other questions"), which carried
over the TREC 2003 definition-question methodology —
[TREC 2004 QA overview](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf):

- What a nugget is, and the atomicity test that makes it judgeable: "An information nugget is an
  atomic piece of information about the target that is interesting (in the assessor's opinion) and was
  not part of an earlier question in the series or an answer to an earlier question in the series. An
  information nugget is atomic **if the assessor can make a binary decision as to whether the nugget
  appears in a response**."
- Importance labels: "the assessor marked some nuggets as **vital**, meaning that this information
  must be returned for a response to be good. Non-vital nuggets act as don't care conditions in that
  the assessor believes the information in the nugget to be interesting enough that returning the
  information is acceptable in, but not necessary for, a good response."
- Conceptual, not lexical, matching, counted once: "A response contained a nugget if there was a
  conceptual match between the response and the nugget; that is, the match was independent of the
  particular wording used in either the nugget or the response. **A nugget match was marked at most
  once per response** — if the response contained more than one match for a nugget, an arbitrary
  match was marked and the remainder were left unmarked."
- Recall: "the **nugget recall** of the response is the ratio of the number of matched nuggets to the
  total number of vital nuggets in the list."
- Precision, and the honest admission of why it is a proxy: "**Nugget precision is much more difficult
  to compute since there is no effective way of enumerating all the concepts in a response.** Instead,
  a measure based on length (in non-white space characters) is used as an approximation to nugget
  precision. The length-based measure starts with an initial allowance of 100 characters for each
  (vital or non-vital) nugget matched. If the total system response is less than this number of
  characters, the value of the measure is 1.0. Otherwise, the measure's value decreases as the length
  increases using the function `1 − (length − allowance)/length`."
- The score: "**`F(β = 3) = 10 · precision · recall / (9 · precision + recall)`**", i.e. recall three
  times as important as precision, averaged over the 64 Other questions (one of 65 was dropped as
  mistakenly unjudged).
- The same honesty note as for list questions: identical Other-question components received different
  scores from assessors; the per-pair average `F(β=3)` differences were {0.012, 0.0, 0.0, 0.0, 0.0,
  0.007, 0.0, 0.007, 0.003, 0.007, 0.0, 0.012, 0.003} and the number of questions scored differently
  per pair {12, 0, 0, 0, 0, 5, 5, 4, 3, 3, 10, 4, 1}. Best score reported: 0.460 (NUSCHUA2).

**TREC Temporal Summarization track — nuggets over time**

- Credit is per *unique* nugget, so repeating a sentence earns nothing extra; the count of unique
  matching nuggets is the **gain**. **Expected gain** is a precision-style measure (gain per update);
  **comprehensiveness** is a recall-style measure (gain divided by the total number of nuggets);
  **latency-penalised gain** discounts each match by how long after the nugget's timestamp the first
  matching sentence appeared; the summary F1 is the harmonic mean of expected gain and
  comprehensiveness — [Real-Time Web Scale Event Summarization, arXiv 1605.03664](https://arxiv.org/pdf/1605.03664)
- Scale of the numbers in practice (TREC 2014 official appendices): average expected gain roughly
  0.03–0.06 while comprehensiveness ranges about 0.31–0.48; e.g. uogTr4A averaged 0.03 expected gain
  and 0.48 comprehensiveness — [uogTr4A appendix](https://trec.nist.gov/pubs/trec23/appendices/tempsumm/uogTr4AC.pdf), [2APSal appendix](https://trec.nist.gov/pubs/trec23/appendices/tempsumm/2APSal.pdf)
- The track ran 2013–2015 and merged into Real-Time Summarization in 2016 — [as summarised in arXiv 1904.00688](https://arxiv.org/pdf/1904.00688)

**TREC RAG track (2024–) — AutoNuggetizer, the current formulas**

From Pradeep et al., "The Great Nugget Recall: Automating Fact Extraction and RAG Evaluation with
Large Language Models" — [arXiv 2504.15068 HTML](https://arxiv.org/html/2504.15068v1); initial TREC
2024 RAG report — [arXiv 2411.09607](https://arxiv.org/abs/2411.09607v1), [PDF](https://arxiv.org/pdf/2411.09607):

- Per-nugget match: `p_i = 1` if the nugget's assignment is **"support"**, 0 otherwise — so
  `partial_support` does **not** count as a match under the strict variant.
- **`A_strict` = (Σ_i p_i) / N_nuggets** — "a score based on all nuggets in an answer, but with strict
  nugget matching".
- **`V_strict` = (Σ_i p_i^v) / N_vital`** over vital nuggets only — the paper's **primary evaluation
  metric**.
- Run score = mean of per-query scores.
- Labels: "Vital nuggets represent concepts that must be present in a 'good' answer", while "okay
  nuggets contribute worthwhile information about the target but are not essential" — the same
  vital/okay distinction as TREC 2003–2004, assigned by GPT-4o rather than a human assessor.
- Caps: the creation prompt asks for "at most 30 nuggets (can be less), keeping only the most vital
  ones"; after importance labelling the nuggets are **sorted by importance and the top 20 kept**;
  manual creation is capped at 20.
- **Agreement with human nugget evaluation (Kendall's τ):** AutoNuggets+Edits with manual assignment,
  run level, **τ = 0.887 (`V_strict`)** and **0.901 (`A_strict`)**; ManualNuggets with manual
  assignment, run level, τ = 0.727 and 0.758. Fully automatic: per-topic average τ 0.360–0.539, all
  topic/run pairs τ 0.297–0.438. Automating assignment only raised per-topic average τ for `V_strict`
  from 0.490 to 0.662 and all topic/run τ from 0.328 to 0.599.
- Initial TREC 2024 RAG deployment covered 21 topics and 45 runs — [arXiv 2411.09607](https://arxiv.org/abs/2411.09607v1)
- Nugget evaluation is being pushed beyond RAG as a diagnostic: "Chatbot Arena Meets Nuggets: Towards
  Explanations and Diagnostics in the Evaluation of LLM Responses" — [arXiv 2504.20006](https://arxiv.org/html/2504.20006v2)

**TREC 2025 RAGTIME — nuggets *plus* citation grounding, which is the directly relevant refinement**

- Per a search-surfaced reading of the RAGTIME overview: assessors "assess each report sentence
  against each nugget", and "**A nugget is considered covered only if the sentence is also grounded in
  its citations**"; short topics used mostly manual citation-support judgment with a classifier filling
  in rare unjudged citations, and long topics (10,000-character limit) were evaluated with Auto-ARGUE —
  [Overview of the TREC 2025 RAGTIME Track, arXiv 2602.10024](https://arxiv.org/pdf/2602.10024).
  **I did not fetch and verify this overview directly; treat the quotes as snippet-level.**
- A noted limitation of RAGTIME for retrieval evaluation: "there is no explicit annotation on which
  documents support or contain those nuggets" — [as reported alongside the RAGTIME material](https://arxiv.org/pdf/2602.10024)
- AutoNuggetizer's LLM assessors are reported to be **stricter** than NIST human assessors when
  assigning nuggets to answers — [arXiv 2504.15068](https://arxiv.org/html/2504.15068v1)
- A caution on auto-evaluation robustness: one 2026 preprint argues the Auto-ARGUE-style evaluation is
  "only robust while its key elements stay secret" — [arXiv 2601.13227](https://arxiv.org/pdf/2601.13227).
  Snippet-level, unverified.

### Inferences

- Nugget evaluation is the only option here that preserves this project's existing discipline exactly:
  a nugget is defined by its *binary judgeability* ("atomic if the assessor can make a binary decision
  as to whether the nugget appears in a response"), which is the same contract as a qrels relevance
  judgement on one passage. The per-nugget judgement is auditable; a scalar judge score is not.
- The TREC 2004 precision proxy is the part not to copy. A length-based stand-in for precision was a
  2004 workaround for prose answers, and for a consensus-outlook answer the modern equivalent —
  requiring each sentence to be grounded in its citations, as RAGTIME does — is both stricter and more
  informative.
- The τ = 0.887 run-level agreement is a *run-level* figure. The per-topic figures (0.360–0.539 fully
  automatic) are much weaker, which means an auto-nugget score is defensible for comparing two system
  configurations across a question set and not defensible for claiming anything about one question.
  For a 105-question frozen set used to compare configurations, that is the right side of the line.

### Gaps

- **I could not verify a TREC 2026 RAG track nugget protocol.** The searches reached the 2024 report,
  the 2025 AutoNuggetizer paper and the TREC 2025 RAGTIME overview, but not 2026 track guidelines.
  Starting point for verification: [TREC RAG track](https://trec-rag.github.io/) and the NIST TREC
  publications index.
- The RAGTIME quotes above are snippet-level and should be checked against the overview before
  publication — [arXiv 2602.10024](https://arxiv.org/pdf/2602.10024)
- The exact Temporal Summarization formulas for expected gain and latency-penalised gain were read
  from a third-party paper's account rather than the track overview — [arXiv 1605.03664](https://arxiv.org/pdf/1605.03664)

## Compounding error, measured honestly

### Takeaway

There is no established, validated way to propagate stage error into a single final-answer metric for
a three-stage pipeline. The published guidance is the opposite: decompose and report per stage. The
only depth-dependent bound I found is a single 2026 theory preprint stating a worst-case floor, which
its own author frames as adversarial, and I could not verify it. "We cannot bound this" is the
defensible position and it has support in the literature's practice, not just its silence.

### Cited Findings

- **The established practice is stage-wise reporting, not composition.** Practitioner guidance is
  consistent: score retrieval (context precision and recall) and generation (faithfulness and answer
  relevance) separately, because "if you only measure the final answer, you can't tell whether a bad
  answer came from bad retrieval or bad generation" — [Maxim AI guide to tracing and evaluating RAG pipelines](https://www.getmaxim.ai/articles/a-complete-guide-to-tracing-and-evaluating-rag-pipelines.md), [mixpeek RAG evaluation guide](https://mixpeek.com/guides/how-to-evaluate-a-rag-pipeline). Vendor guides, not peer-reviewed.
- **TREC's own composition was for ranking participants, with fixed declared weights, not for
  propagating uncertainty**: `FinalScore = .5 · FactoidAccuracy + .25 · ListAveF + .25 · OtherAveF`
  (§2.4) — [TREC 2004 QA overview](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf)
- **A claimed worst-case depth bound, unverified.** A 2026 preprint states that for a k-stage
  retrieve-then-generate pipeline any attribution method has a worst-case error floor of roughly
  `Ω(k · ε_stage)`, where `ε_stage` is the per-stage post-rationalisation rate, and that "attribution
  infrastructure must also decompose by stage, with budget scaling in k". The same source frames this
  as a worst case over adversarially constructed pipelines, so real systems may do better —
  [arXiv 2605.23024](https://arxiv.org/pdf/2605.23024). **Snippet-level, not fetched, not
  peer-reviewed; cite as a claim, not a result.**
- **Propagation estimates in the literature are correlational, by their authors' own admission.** The
  CHARM framework estimates stage-to-stage propagation as a conditional probability on the pipeline's
  DAG edges computed offline, and states that "this estimation treats co-occurrence of errors at
  adjacent stages as evidence of propagation rather than independent occurrence", and that "formally
  establishing causal transmission remains open" — [arXiv 2606.04435](https://arxiv.org/pdf/2606.04435). Snippet-level, unverified.
- **Final-trace evaluation demonstrably misattributes.** An agentic-RAG failure-attribution paper
  argues "an evaluator that only inspects the final trace may flag a late low-coverage hop or label
  the failure as answer generation, missing the earliest causal fault" — [arXiv 2608.20627](https://arxiv.org/pdf/2608.20627). Snippet-level.
- **Attribution accuracy, where measured, is near but not exact.** AgentEval reports a median distance
  of 1 hop between attributed and true root cause, with **72% of errors within 1 hop and 91% within 2
  hops**, and finds multi-branch convergence and long propagation chains the dominant error patterns —
  [arXiv 2604.23581](https://arxiv.org/pdf/2604.23581). Snippet-level, unverified.
- **Fine-grained fault attribution is an active research problem, not a solved metric.** Capital One's
  RAFFLES frames the task as "identifying not only which component fails but also the specific failure
  mode at each step" — [Capital One / RAFFLES](https://www.capitalone.com/tech/publications/raffles-reasoning-based-attribution-of-faults/), [capitalone.science](https://capitalone.science/publications/raffles-reasoning-based-attribution-of-faults/)
- **The honest precedent for reporting an unresolvable gap already exists in IR, twice over:** RBO
  reports base + residual rather than a point estimate, because the tail is not knowable —
  [RBO §4.4](http://www.williamwebber.com/research/papers/wmz10_tois.pdf); and `Judged@k` reports the
  fraction of the top k that has judgements at all — [ir_measures](https://ir-measur.es/en/latest/measures.html)

### Inferences

- The structurally honest statement for a three-stage question is a **conditional one**: report the
  stage-3 score *on the subset where stage 1 met a stated recall threshold*, and report stage-1 recall
  separately. That is not error propagation; it is conditioning, and it avoids claiming a composite
  the literature cannot justify.
- Since stage-1 recall for a 13F-derived entity set is itself only measurable against a possibly
  incomplete gold set (Part 1), a composite would be compounding an unbounded quantity with a bounded
  one. Declining to publish a single composite score is the position the evidence supports.
- The 2026 preprints above all point the same way methodologically — decompose by stage — while
  disagreeing on whether propagation can be quantified. The agreement is usable; the quantities are
  not.

### Gaps

- **No validated stage-wise error-propagation bound for multi-stage RAG was found.** The only bound
  located is a single unverified theory preprint describing a worst case —
  [arXiv 2605.23024](https://arxiv.org/pdf/2605.23024). Record as "we cannot bound this", with that URL
  as the evidence for *why* rather than a result to lean on.
- All five 2026 preprints in this section were surfaced as search snippets and not fetched. None
  should be cited as a result without being read.

## The connection to the existing "confidently wrong" metric

### Takeaway

The direct published analogue is the overconfidence-under-noisy-retrieval literature, which measures
exactly the phenomenon the existing refusal metric targets — confidence unwarranted by the retrieved
evidence — but measures it as calibration (ECE, AUROC) rather than as "groundless answers scoring
above the median grounded one". The natural extension of the existing metric to synthesised answers
is a *groundedness-stratified* version of it, and that specific construction is my design.

### Cited Findings

- **Noise inflates false certainty, measurably.** A NAACL 2026 paper finds that noisy retrieved
  context makes LLM verbal confidence poorly calibrated, and that "contradictory or irrelevant
  evidence tends to inflate the model's false certainty, leading to severe overconfidence"; it
  proposes noise-aware calibration rules — [arXiv 2601.11004](https://papers.cool/arxiv/2601.11004), [summary](https://tldr.takara.ai/p/2601.11004). Snippet-level, unverified.
- **Post-hoc fixes do not remove it.** A 2026 comparison reports that self-evaluation prompting cuts
  overconfident wrong answers but barely improves calibration error; temperature scaling improves
  calibration error but leaves most wrong answers still overconfident; supervised fine-tuning
  suppresses overconfidence at a cost to accuracy — [arXiv 2604.05306](https://arxiv.org/pdf/2604.05306). Authors' own reported results, snippet-level.
- **Evidence-sufficiency calibration is weak where measured.** A 2025 SQuAD 2.0 study found calibrated
  evidence sufficiency substantially reduced unsupported answers versus a forced-answer system, but
  the evidence calibration itself remained weak at **AUROC 0.5475** — [JTIE / Garuda record](https://garuda.kemdiktisaintek.go.id/documents/detail/6386035), [ICNLSP 2026 related paper](https://aclanthology.org/2026.icnlsp-1.6.pdf)
- **Uncertainty is only a hallucination signal when evidence constrains the answer.** Ansari &
  Agrahari (ICNLSP 2026) argue uncertainty measures RAG hallucination risk only when retrieved
  evidence actually constrains the answer; their perplexity-based detector reached **0.81–0.91 AUROC**
  on targeted unsupported edits — [ACL Anthology 2026.icnlsp-1.6](https://aclanthology.org/2026.icnlsp-1.6.pdf)
- **Calibration-oriented RAG exists as a framing** — [arXiv 2411.08891 HTML](https://arxiv.org/html/2411.08891v2), [PDF](https://arxiv.org/pdf/2411.08891.pdf)
- **FActScore's abstention handling is the one piece of established machinery that matches a refusal
  metric.** Its model-level score is conditioned on the model having answered:
  `FActScore(M) = E_{x ∈ X}[f(M_x) | M_x responds]`, where "ℳx responds means ℳ did not abstain from
  responding to the prompt x" — [FActScore](https://arxiv.org/html/2305.14251v2). A groundedness score
  that silently averages over refusals is not comparable across systems; FActScore solves this by
  conditioning explicitly.

### Inferences (and DESIGN)

- The existing metric — groundless answers scoring above the median grounded one — is a
  *stratified comparison*: it partitions answers by whether they had evidence and compares score
  distributions. That construction transfers directly to synthesised answers, and the published
  literature does not appear to contain it in that form.
- **DESIGN — what a synthesised-answer version would have to count**, in the same shape as the
  existing metric:
  1. **A groundedness score per answer**, computed as FActScore's ratio over the answer's atomic
     claims against the retrieved context only:
     `f(y) = (1/|A_y|) Σ_{a ∈ A_y} I[a supported by retrieved context]` — [FActScore](https://arxiv.org/html/2305.14251v2). A refusal is excluded from the average and
     counted separately, per FActScore's abstention conditioning.
  2. **A confidence or assertiveness score per answer**, operationalised without a model: count of
     unhedged declarative claims about the consensus (e.g. "the consensus is X") divided by total
     claims. Hedged claims are not penalised. This keeps the metric auditable in the same way a
     nugget judgement is auditable.
  3. **The stratified comparison itself**: the fraction of *low-groundedness* answers whose
     assertiveness exceeds the **median assertiveness of high-groundedness answers**. That is the
     exact analogue of "groundless answers scoring above the median grounded one", with groundedness
     substituted for having-evidence and assertiveness substituted for score.
  4. **A denominator statement**: how many questions had enough stage-1 recall to be gradeable at all.
     Without it the metric measures the easy subset.
- Why not reuse ECE or AUROC from the calibration papers: they require a correctness label per answer,
  which a consensus question does not have. The stratified construction above needs only a
  groundedness score, which is computable without a gold consensus. That is the reason to depart from
  published practice here, and it should be stated as a departure.

### Gaps

- **I found no published metric of the form "unwarranted confidence relative to retrieved evidence"
  for synthesised, gold-free answers.** The calibration literature measures confidence against
  correctness, not against groundedness. If this project publishes the stratified version it is
  proposing a measure, not adopting one.
- Four of the five sources in this section are 2026 snippet-level results that I did not fetch. The
  AUROC 0.5475 and 0.81–0.91 figures should be verified before publication.

## A minimal recommendation for consensus questions — DESIGN

### Takeaway

Treat a consensus question as three separately-reported stages, publish a nugget score and a
groundedness score, and refuse to publish a composite or a correctness claim about the consensus
itself. The nugget machinery is established; the gold-record layout and the refusals are mine.

### The scheme (DESIGN)

**1. Gold record — `consensus_nuggets`, one line per nugget.**

```
question  nugget_id  nugget_text  importance  source_entity  source_passage  completeness
```

- `nugget_text` must satisfy the TREC atomicity test verbatim — "atomic if the assessor can make a
  binary decision as to whether the nugget appears in a response"
  ([TREC 2004 §2.3](https://trec.nist.gov/pubs/trec13/papers/QA.OVERVIEW.pdf)). If it fails that test
  it is not a nugget and must be split.
- `importance` ∈ {`vital`, `okay`}, with the TREC 2003/2004 semantics carried forward by
  AutoNuggetizer: vital = "must be present in a 'good' answer"; okay = "worthwhile … but not
  essential" ([arXiv 2504.15068](https://arxiv.org/html/2504.15068v1)).
- `source_entity` ties each nugget to the stage-1 company it came from. This is the column that makes
  stage-1 incompleteness visible inside the stage-3 score: if a company is missing from the retrieved
  set, every nugget attributed to it is unreachable, and that is countable rather than invisible.
- `source_passage` is the passage ID, in the same namespace as the existing qrels. This lets a nugget
  be scored the RAGTIME way — covered only if the answer sentence is also grounded in its citation
  ([arXiv 2602.10024](https://arxiv.org/pdf/2602.10024), snippet-level).
- `completeness` — per question: `complete` is never claimable for a consensus question, so the
  permitted values are `pool-k` and `partial` only.

Cap nuggets at 20 per question, following AutoNuggetizer's post-labelling cap
([arXiv 2504.15068](https://arxiv.org/html/2504.15068v1)).

**2. Report three numbers and nothing combined.**

- **`V_strict` = (Σ_i p_i^v) / N_vital`**, where `p_i = 1` only on a `support` assignment and
  `partial_support` scores 0 — the TREC RAG track's primary metric, unchanged
  ([arXiv 2504.15068](https://arxiv.org/html/2504.15068v1)). Report `A_strict` alongside if wanted.
  Do **not** adopt TREC 2004's character-length precision proxy; it exists because 2004 had no better
  option.
- **Groundedness**, as FActScore's ratio over the answer's atomic claims against the retrieved
  context: `f(y) = (1/|A_y|) Σ_{a ∈ A_y} I[a supported]`, conditioned on non-abstention
  ([arXiv 2305.14251](https://arxiv.org/html/2305.14251v2)). Prefer this formula over the RAGAS
  library metric with the same shape, because RAGAS's documentation publishes no human-agreement
  figure ([RAGAS docs](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/faithfulness/)).
- **Stage-1 recall, separately**, from Part 1's set F1 — plus **Reachable@question**: the fraction of
  vital nuggets whose `source_entity` was actually retrieved. A `V_strict` of 0.6 against a
  `Reachable@question` of 0.6 is a ceiling effect, not a synthesis failure, and that must be visible.

**3. Refuse to report, and say why.**

- **No composite stage score.** No validated propagation bound exists; the only candidate is an
  unverified adversarial worst case ([arXiv 2605.23024](https://arxiv.org/pdf/2605.23024)). Publish
  "we cannot bound this."
- **No correctness claim about the consensus.** There is no gold consensus, so exact match and set F1
  do not apply; the publishable claims are coverage (`V_strict`) and groundedness, both of which are
  claims about the *answer's relationship to evidence*, not about the world.
- **No per-question nugget claim from an automatic nugget score.** Run-level agreement with human
  nugget evaluation reaches τ = 0.887, but per-topic agreement is only 0.360–0.539 fully automatic
  ([arXiv 2504.15068](https://arxiv.org/html/2504.15068v1)). Automatic nugget scores compare
  configurations across the frozen question set; they do not settle a single question.
- **No bare LLM-as-judge number.** If used at all, swap positions and publish the consistency rate
  next to the verdict: GPT-4's own swap consistency was 65.0% and its agreement with humans drops to
  66% once ties and inconsistent votes are counted
  ([Zheng et al. §§3.3, 4.2](https://arxiv.org/html/2306.05685v4)).

### Which parts are established and which are mine

- **Established:** the nugget definition and atomicity test; vital/okay labels; `V_strict` and
  `A_strict` with strict `support`-only matching; the 20-nugget cap; FActScore's formula and its
  abstention conditioning; nugget-covered-only-if-citation-grounded (RAGTIME, snippet-level);
  stage-wise rather than composite reporting; reporting judge consistency with judge verdicts.
- **Mine (DESIGN):** the seven-column `consensus_nuggets` layout; the `source_entity` column as the
  device that makes stage-1 incompleteness countable inside the stage-3 score; `Reachable@question`;
  the explicit refusal list; the assertiveness-versus-groundedness stratified metric in the previous
  section.

### Gaps

- The RAGTIME citation-grounding rule, which this recommendation leans on, is snippet-level and
  unverified — [arXiv 2602.10024](https://arxiv.org/pdf/2602.10024).
- No TREC 2026 RAG nugget protocol was verified; if the 2026 edition changed `V_strict` or the caps,
  this recommendation would need updating — [TREC RAG track](https://trec-rag.github.io/)
