# Does financial-document QA actually need multi-hop retrieval?

Scope note for the report writer. This note extends, and does not repeat,
`research_notes/Schema implied relations in spectastic/schema_implied_edges.md` (SEC form-type relation
family, submissions/XBRL APIs, Financial Statement Data Sets, 13F data sets, 8-K item numbers, exhibits
incorporated by reference) and
`research_notes/Multi hop questions and graph retrieval/multihop_external_evidence.md` (general multi-hop and
GraphRAG evidence, traversal depth, benchmark validity). Both are taken as given.

**The three-way label used on every benchmark and every number below.**

- `[RETRIEVAL-HOP]` — passage B is not findable from the question alone; you must retrieve A, read it, and only
  then can you locate B. Usually spans documents.
- `[PARALLEL-DOC]` — the evidence genuinely lives in two or more documents, but every piece is findable from
  the question alone, because the question names the companies, the metrics and the years. This is a
  **recall@k budget** problem (you need *n* independently-findable chunks inside one *k*), not a hop problem.
  A graph index does not remove the dependency, because there was no dependency.
- `[NUM-MULTISTEP]` — all evidence is in one table, one page or one document, already retrievable in one shot;
  the difficulty is several arithmetic or logical steps. A *reasoning* difficulty. A graph index does nothing.
- `[LONG-CTX]` — the evidence is in one very large document; the difficulty is locating a span inside it.
  A one-hop ranking problem at a different granularity.

**Metric discipline.** Every figure is tagged `[RET]` (recall@k, precision@k, nDCG, Hit Rate@k, page recall) or
`[ANS]` (execution accuracy, EM, F1 over answer tokens, accuracy, LLM-judge correctness, BERTScore, RAGAS
scores). RAGAS "context recall"/"context precision" are tagged `[ANS-JUDGE]` throughout, because they are
LLM-judged estimates computed without gold passage labels — they are **not** qrels-based retrieval metrics and
must never be reported next to recall@k as if they were comparable.

**The three headline conclusions, up front, because the rest is detail.**

1. Of the eleven canonical financial QA benchmarks the assignment names, **the number that require retrieving
   from two or more different filings is zero.** FinQA, ConvFinQA, TAT-QA, MultiHiertt, DocFinQA, FinanceBench,
   FinDER, FinQABench, FinTextQA and FinMMR are all single-document or no-corpus tasks. SEC-QA is the sole
   exception in that list, and it is not redistributable.
2. The 2025–2026 benchmarks that *do* claim cross-document financial multi-hop (FinReflectKG-MultiHop, LOFin,
   Fin-RATE) are overwhelmingly `[PARALLEL-DOC]`, not `[RETRIEVAL-HOP]`. Cross-company questions are **9.7%**
   of FinReflectKG-MultiHop's generated set; Fin-RATE's cross-entity task names the sector and fiscal year and
   wants ~5.6 chunks per question.
3. Where retrieval *is* the bottleneck in finance — and in open-domain settings it badly is — the failure is a
   **first-hop** failure: finding the right filing and the right page from a fully-specified question. Nothing
   in the literature attributes a finance retrieval failure to an inability to chain.

---

## Q1. For each major financial QA benchmark: what does it test, corpus size, documents per question, evidence within one document or across several?

### Takeaway
Every benchmark in the canonical set is a single-document task, and most of them do not even have a retrieval
corpus — the table-plus-paragraphs context is handed to the model. The word "multi-hop" in this literature
almost always means *multi-step arithmetic*, or *multiple tables inside one annual report*, or *multiple turns
of a dialogue*; it does not mean a retrieval dependency across filings.

### Cited Findings

**FinQA (EMNLP 2021)** — `[NUM-MULTISTEP]`, single page
- 8,281 QA pairs: 6,251 train / 883 validation / 1,147 test (75/10/15), splits with no overlapping input
  reports; **2,789 report pages** (Table 1), drawn from S&P 500 earnings reports **1999–2019** via FinTabNet;
  12,719 pages were selected for annotation — [FinQA, ar5iv](https://ar5iv.labs.arxiv.org/html/2109.00122)
- Questions were written **per page**, each page assigned to one or two experts, and pages were filtered to
  those containing **at most one table**; the retriever operates on the input page — [FinQA,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2109.00122). Documents per question: **1 page**.
- Supporting facts per question: 46.30% one fact, 42.63% two, 11.07% more than two. Reasoning program steps:
  **59.10% one step, 32.71% two steps, 8.19% three or more** (max 5) — [FinQA,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2109.00122)
- Evidence type: 23.42% text-only, 62.43% table-only, 14.15% both — [FinQA,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2109.00122)

**ConvFinQA (EMNLP 2022)** — `[NUM-MULTISTEP]`, single report; the "hops" are dialogue turns
- **3,892 conversations / 14,115 questions** (3,037 / 421 / 434 train/dev/test), average 3.67 questions per
  conversation, **2,066 report pages** (Table 2, "Statistics of ConvFinQA"); built by **decomposing and
  concatenating FinQA's multi-hop questions** — [ConvFinQA,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2210.03849)
- The two FinQA questions used in a hybrid conversation are "based on the **same report**"; conversation types
  2,715 simple / 1,177 hybrid — [ConvFinQA, ar5iv](https://ar5iv.labs.arxiv.org/html/2210.03849)
- **Over 60% of questions depend on earlier questions**; in a 200-sample check, 65.0% of second-set hybrid
  questions depend on the first set. Program steps: 34.73% number selection, 35.10% one-step, 25.41% two-step,
  4.75% over three — [ConvFinQA, ar5iv](https://ar5iv.labs.arxiv.org/html/2210.03849)
- This is the closest thing in the canonical set to a genuine dependency structure, and it is a **conversational
  coreference** dependency inside one report, not a retrieval dependency across documents.

**TAT-QA (ACL 2021)** — `[NUM-MULTISTEP]`, no retrieval task at all
- **16,552 questions associated with 2,757 hybrid contexts** from real-world financial reports, where a hybrid
  context is a table plus its accompanying text — [TAT-QA repo](https://github.com/NExTplusplus/tat-qa)
- Operations required include addition, subtraction, multiplication, division, counting, comparison, sorting
  and combinations; answers are single-span, multi-span or free-form with derivation and scale —
  [TAT-QA](https://github.com/NExTplusplus/tat-qa)
- Documents per question: **one hybrid context, supplied with the question**. The repo page does not address
  multi-document evidence at all — [TAT-QA](https://github.com/NExTplusplus/tat-qa)

**MultiHiertt (ACL 2022)** — `[NUM-MULTISTEP]` + `[LONG-CTX]` *within one annual report*
- **10,440 QA pairs over 2,513 documents** (Table 3), splits 7,830 / 1,044 / 1,566; **average 3.89 tables per
  document** and **1,645.9 words per input text**; average **7.02 supporting facts per question** (Section
  3.4) — [MultiHiertt, ar5iv](https://ar5iv.labs.arxiv.org/html/2206.01347)
- Evidence coverage: 10.24% text-only, **33.09% one table, 7.93% multiple tables without text**, 48.74% text
  plus table, and **of the text-plus-table questions 23.20% need more than one table** — [MultiHiertt,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2206.01347)
- Reasoning steps: 1 step 28.94%, 2 steps 37.76%, 3 steps 15.21%, more than 3 steps 18.10%, **average 2.47** —
  [MultiHiertt, ar5iv](https://ar5iv.labs.arxiv.org/html/2206.01347)
- **This is the benchmark most often miscited as evidence for financial multi-hop retrieval.** Its "multi-hop"
  is *across tables inside a single annual report*, and the document is given. The paper does not state that
  any question's evidence spans two documents.

**FinanceBench (arXiv Nov 2023)** — `[NUM-MULTISTEP]` per question; one document per entry
- **10,231 question/answer/evidence triplets** over **40 U.S.-listed companies and 361 public filings** (the
  appendix says 360 documents), filings dated **2015 to 2023**; document types 10-K, 10-Q, 8-K and earnings
  reports — [FinanceBench, ar5iv](https://ar5iv.labs.arxiv.org/html/2311.11944)
- **Each entry is tied to one document.** Metrics-generated numerical questions are labelled as needing one
  financial statement or multiple statements, and some questions draw on multiple passages *within* the
  document — [FinanceBench, ar5iv](https://ar5iv.labs.arxiv.org/html/2311.11944)
- Composition: **925 domain-relevant entries built from 25 question types, 1,323 novel-generated, 7,983
  metrics-generated** — [FinanceBench, ar5iv](https://ar5iv.labs.arxiv.org/html/2311.11944)
- Gold evidence: every entry carries evidence text, a **1-indexed evidence page number** and the full page
  text — [FinanceBench, ar5iv](https://ar5iv.labs.arxiv.org/html/2311.11944)

**DocFinQA (ACL 2024 short)** — `[LONG-CTX]`, explicitly one filing per question
- **7,437 questions** (from FinQA's 8,281) over **801 unique SEC filings**, splits 5,735 / 780 / 922; average
  context **123,453 words** (Table 1) against "under 700 words in FinQA"; ~6.5% (543) of data points dropped
  at download and 117 more at PDF conversion — [DocFinQA, arXiv
  HTML](https://arxiv.org/html/2401.06915v3)
- Gold labels: FinQA's golden context is mapped to a **target chunk c★** from the chunked document; chunks are
  2,750 characters (~509 tokens) with 20% overlap, matched by four-gram similarity — [DocFinQA,
  arXiv](https://arxiv.org/html/2401.06915v3)
- "More than 40%" of the 100K+ token cases remain unanswerable even at 128K tokens — [DocFinQA,
  arXiv](https://arxiv.org/html/2401.06915v3)
- DocFinQA is the cleanest demonstration in finance that the hard part of a single filing is **finding the
  page**, which is a one-hop ranking problem at page granularity.

**FinDER (ICLR 2025 workshop / ICAIF 2025)** — `[RETRIEVAL]` single-filing, jargon-resolution
- **5,703 expert-annotated query–evidence–answer triplets** (Section 3); corpus is the **latest Form 10-K
  filings for 490 S&P 500 companies** (Section 3.1) — [FinDER, arXiv
  HTML](https://arxiv.org/html/2504.15800v2)
- Each question is **linked to its own company's 10-K**, with "one or more passages" of ground-truth evidence
  (Section 3). The paper does **not** state that evidence spans multiple filings, and gives no
  documents-per-query figure — [FinDER](https://arxiv.org/html/2504.15800v2)
- Questions fall into eight financial domains, with Company Overview and Financials the largest — [FinDER
  summary](https://arxiv.org/html/2504.15800v2)

**FinTextQA (ACL 2024 long, pp. 6025–6047)** — `[NUM-MULTISTEP]`-free long-form text QA; not SEC filings
- **1,262 high-quality, source-attributed QA pairs** extracted from **finance textbooks and government agency
  websites**; the authors' motivation is that "there is currently no LFQA dataset specifically tailored for the
  finance domain" — [FinTextQA, arXiv](https://arxiv.org/html/2405.09980v1); [ACL
  Anthology](https://aclanthology.org/2024.acl-long.328)
- Best configuration: Ada2 embedder + Automated Merged Retrieval + Bge-Reranker-Base + Baichuan2-7B
  generator — [FinTextQA](https://arxiv.org/html/2405.09980v1). Hosted at HF `GPS-Lab/FinTextQA`.
- Not a filings corpus and not multi-hop; it is long-form answer generation over reference text.

**SEC-QA (arXiv June 2024; FinNLP 2025, pp. 221–236)** — the one canonical benchmark with `[PARALLEL-DOC]` and
a trace of `[RETRIEVAL-HOP]`
- Corpus scope: **18 publicly traded S&P 500 companies, 10 metrics, 2010–2023**, over 10-K, 10-Q and 8-K
  (Section 3.2) — [SEC-QA, ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394)
- Question sets evaluated: single-value (Section 4.3, count not stated), **24 compound-metric questions**
  (Section 4.4), **135 multi-document questions** (Section 4.5) — [SEC-QA,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394)
- Multi-document templates (Section 4.5), verbatim: "How much common dividends did {company} pay in the last
  {num_year} years in US dollars?"; "What is the percentage difference of {company1}'s {metric} compared to
  that of {company2}?"; "What is {company}'s overall revenue growth over the last {num_year}-year period?";
  "Among {company_names}, what is the {metric2} of the company that has the highest {metric1}?" — [SEC-QA,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394)
- Section 3.3 names its complexity types as **parallel reference, multi-hop, structural (document and
  collection), and multi-output** — i.e. the paper itself separates parallel reference from multi-hop —
  [SEC-QA, ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394)
- Gold evidence: the backing database **records the document that sources each value**, which the authors use
  to compute document- and page-level retrieval metrics; **no passage- or chunk-level gold is mentioned** —
  [SEC-QA, ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394)
- Table 1 lists SEC-QA with the figures "123,000" and "1,315", but the extracted table does not label the
  columns, so **which is the question count and which the document count is unresolved** — [SEC-QA,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394). Recorded as a gap rather than asserted.

**FinMMR (arXiv Aug 2025; ICCV 2025)** — `[NUM-MULTISTEP]`, multimodal, no retrieval corpus
- Bilingual; about **4.3K questions and 8.7K images across 14 categories** (tables, bar charts, ownership
  structure charts), spanning 14 financial subdomains; adapted from existing financial reasoning benchmarks
  plus new questions over recent Chinese financial research reports; answers judged against a **0.2% error
  margin** — [FinMMR, arXiv HTML](https://arxiv.org/html/2508.04625v1)
- Best hard-problem accuracy `[ANS]`: **53.0%** in the arXiv version versus **51.4%** in the ICCV version —
  the two sources conflict — [arXiv](https://arxiv.org/html/2508.04625v1); [ICCV 2025
  poster](https://iccv.thecvf.com/virtual/2025/poster/1794)
- Images are supplied with the question. There is no corpus to retrieve from, so FinMMR cannot bear on a
  retrieval question at all.

**FinQABench** — used as a FinanceRAG subset, provenance unverified
- Described by a FinanceRAG participant as "**10-K Reports, focusing on detecting hallucinations and ensuring
  factual correctness in the generated responses**" — [Multi-Reranker, arXiv
  2411.16732](https://arxiv.org/pdf/2411.16732)
- I could not locate a primary FinQABench paper, dataset card, question count or licence. Recorded as a gap.

**2025–2026 benchmarks that explicitly claim cross-document financial multi-hop**

*FinReflectKG-MultiHop (arXiv 2510.02906, Oct 2025; NeurIPS 2025 workshop listing)* — `[PARALLEL-DOC]`
dominant
- **555 QA pairs** in the curated subset; experiments run on a **top-150 subset** selected by quality-rubric
  score, spanning the Financials and Information Technology GICS sectors (Section 4). Corpus: **10-K filings
  from S&P 100 companies, 2022–2024** — [FinReflectKG-MultiHop, arXiv
  HTML](https://arxiv.org/html/2510.02906v1)
- Scope breakdown (Section 3.3): **2-hop patterns 52%, 3-hop patterns 48%**; **intra-document 48.7%, inter-year
  same company 41.6%, cross-company same year 9.7%** — [FinReflectKG-MultiHop,
  arXiv](https://arxiv.org/html/2510.02906v1)
- Built from FinReflectKG, an agentic KG over SEC 10-Ks with **24 entity types and 29 relationship types**;
  patterns generated by Qwen3-235B-A22B, rubric-filtered below 8/10, then executed against the graph to
  confirm they are represented; chunks ranked by betweenness centrality (Section 3.2) —
  [arXiv](https://arxiv.org/html/2510.02906v1)
- The parent KG is described as **17.5M triplets across 743 S&P 500 companies, 2014–2024** (Section 3.1),
  which **conflicts** with the S&P 100 corpus description — [arXiv](https://arxiv.org/html/2510.02906v1)
- Nearly half the set is intra-document, i.e. `[NUM-MULTISTEP]`/`[LONG-CTX]` by this note's taxonomy, inside a
  benchmark whose title is "MultiHop".

*LOFin / HiREC (arXiv 2505.20368 v3, cs.IR)* — the only public open-domain finance benchmark with page-level
retrieval gold
- **LOFin-1.6k: 1,595 QA pairs** — FinQA **1,112 (69.7%)**, SEC-QA **333 (20.9%)**, FinanceBench **150
  (9.4%)** (Table 9); by answer type 925 Numeric (Table), 273 Numeric (Text), 397 Textual (Table 13). The
  earlier LOFin-1.4k had 1,389 pairs; the paper says 205 pairs were added while Appendix A says 206 were added
  to the SEC-QA slice (127 → 333) — **these figures do not reconcile** — [HiREC, arXiv
  HTML](https://arxiv.org/html/2505.20368v3)
- Corpus: **145,897 SEC reports** (10-K, 10-Q, 8-K) from **516 S&P 500 companies, October 2001 to April
  2025** — [HiREC](https://arxiv.org/html/2505.20368v3)
- Table 1 characterises the sources: **FinQA and FinanceBench are mainly single-document; SEC-QA and LOFin are
  multi-document** — [HiREC](https://arxiv.org/html/2505.20368v3). Exact per-question single-vs-multi counts
  are **not reported**.
- Gold evidence is **page-level**: retrieval recall and precision are measured against ground-truth evidence
  pages — [HiREC](https://arxiv.org/html/2505.20368v3)

*Fin-RATE (arXiv 2602.07294 v4, KDD '26)* — `[PARALLEL-DOC]` by construction, and explicit about it
- Three tasks, "each task contains 2,500 instances" (7,500 total by arithmetic; no combined total is stated).
  Corpus: **15,311 document chunks from 2,472 filings by 43 companies, 2020–2025**, averaging **2,649 words per
  chunk**; forms include 10-K, 10-Q, 8-K, DEF 14A, 6-K, S-8, 424B series and SC series — [Fin-RATE, arXiv
  HTML](https://arxiv.org/html/2602.07294)
- Task tiers: **DR-QA** single-document, single chunk; **EC-QA** cross-entity, same GICS sector and fiscal
  year, averaging **5.6 chunks**; **LT-QA** longitudinal, one company across years, averaging **3.0 chunks** —
  [Fin-RATE](https://arxiv.org/html/2602.07294)
- Gold evidence is **chunk-level** ("gold chunks", a "gold set" per query); both gold-context and RAG settings
  are evaluated — [Fin-RATE](https://arxiv.org/html/2602.07294)
- The paper gives inconsistent industry counts ("36 industries" §3.1 versus "seven major industry sectors"
  Appendix A), and two Appendix versions list different sector counts — [Fin-RATE,
  arXiv](https://arxiv.org/html/2602.07294)

### Inferences
- The canonical benchmarks were built by handing an expert a page or a report and asking for a question. That
  construction method **cannot produce a retrieval hop**, because the annotator never had a second document.
  FinQA's page filter ("at most one table") makes this explicit, and ConvFinQA and DocFinQA both inherit FinQA's
  questions, so three of the eleven are the same annotation effort re-cut.
- MultiHiertt is the single most important mislabel to correct: a corpus of 2,513 documents with 3.89 tables
  each, where the question needs two of those tables, is a **document-internal table-linking** task. If
  Spectastic's corpus were re-described in those terms, it would look identical in kind.
- SEC-QA's own complexity taxonomy — "parallel reference" listed separately from "multi-hop" — is independent
  confirmation from inside the finance literature that this note's distinction is the right one.
- Of SEC-QA's four multi-document templates, three are parallel (dividends over N years; A versus B; growth
  over N years — every operand is named). Only "among {company_names}, what is the {metric2} of the company
  that has the highest {metric1}?" is genuinely hop-dependent: you must first retrieve metric1 for every
  candidate, rank them, and only then do you know which company's metric2 to fetch. That is **one template
  family**, and it is the argmax-then-lookup shape, which query decomposition handles without a graph.

### Gaps
- **Documents per question is not reported as a statistic by any of the eleven canonical benchmarks.** It has
  to be inferred from construction (one page, one hybrid context, one filing). Nobody publishes the
  distribution.
- SEC-QA Table 1's "123,000" and "1,315" are unlabelled in the extracted text — [SEC-QA,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394). Do not assert either as the question or document count.
- FinQABench: no primary source found for question count, construction or licence.
- LOFin does not publish the single-vs-multi-document split of its own 1,595 questions, only the per-source
  characterisation in Table 1 — [HiREC](https://arxiv.org/html/2505.20368v3).
- FinReflectKG-MultiHop's full (uncurated) dataset size is not stated — [arXiv](https://arxiv.org/html/2510.02906v1).
- I did not verify FinRAGBench-V ([arXiv 2505.17471](https://arxiv.org/pdf/2505.17471)) or FinMRAGBench ([ACL
  Findings 2026](https://preview.aclanthology.org/ingest-acl/2026.findings-acl.187/)); both are multimodal RAG
  benchmarks that surfaced in search and may be worth a later pass.

---

## Q2. Which of these genuinely require two or more different filings or companies, and in what proportion?

### Takeaway
**Almost all financial QA is single-document numerical reasoning.** State it plainly: of the eleven canonical
benchmarks, ten have one document (or no corpus) per question, and the eleventh (SEC-QA) is not
redistributable. The three 2025–2026 benchmarks built for cross-document finance are between 9.7% and 33%
cross-company, and what they test is *parallel set retrieval over named entities*, not a hop whose second step
is unknowable from the question.

### Cited Findings
- Single-document or no-corpus by construction: FinQA (one page, max one table) —
  [ar5iv](https://ar5iv.labs.arxiv.org/html/2109.00122); ConvFinQA (both source questions "based on the same
  report") — [ar5iv](https://ar5iv.labs.arxiv.org/html/2210.03849); TAT-QA (one hybrid context supplied) —
  [repo](https://github.com/NExTplusplus/tat-qa); MultiHiertt (multiple tables within one report) —
  [ar5iv](https://ar5iv.labs.arxiv.org/html/2206.01347); DocFinQA (one filing, 801 filings for 7,437
  questions) — [arXiv](https://arxiv.org/html/2401.06915v3); FinanceBench ("each entry is tied to one
  document") — [ar5iv](https://ar5iv.labs.arxiv.org/html/2311.11944); FinDER (question linked to its own
  company's 10-K) — [arXiv](https://arxiv.org/html/2504.15800v2); FinTextQA (textbooks and agency websites) —
  [arXiv](https://arxiv.org/html/2405.09980v1); FinMMR (images supplied) —
  [arXiv](https://arxiv.org/html/2508.04625v1).
- LOFin's own table states **FinQA and FinanceBench are mainly single-document** and only SEC-QA and LOFin are
  multi-document (Table 1) — [HiREC](https://arxiv.org/html/2505.20368v3). In LOFin-1.6k the multi-document
  slice is therefore the SEC-QA portion: **333 of 1,595 = 20.9%** (Table 9) —
  [HiREC](https://arxiv.org/html/2505.20368v3)
- SEC-QA evaluates **135 multi-document questions** (Section 4.5) against 18 companies and 10 metrics
  (Section 3.2) — [ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394)
- FinReflectKG-MultiHop: **cross-company and same year is 9.7%**; inter-year same company 41.6%;
  intra-document 48.7% (Section 3.3) — [arXiv](https://arxiv.org/html/2510.02906v1). The paper's conclusion
  claims coverage of "cross-company and inter-year scenarios" while cross-company is under a tenth of the
  generated set.
- Fin-RATE: of 7,500 instances, **one third (2,500) is single-chunk DR-QA**, one third is cross-entity EC-QA
  (avg 5.6 chunks, *same sector and fiscal year*), one third is longitudinal LT-QA (avg 3.0 chunks, *same
  company*) — [arXiv](https://arxiv.org/html/2602.07294)
- Fin-RATE's EC-QA is explicitly scoped to the **same GICS sector and fiscal year**, so the candidate set is
  fixed by the question's own constraints — [arXiv](https://arxiv.org/html/2602.07294)

### Inferences
- Combining the two honest multi-document counts: the largest genuinely multi-filing financial QA evaluation
  anywhere in the published literature is **Fin-RATE's 5,000 multi-chunk instances**, followed by **LOFin's
  333**, **SEC-QA's 135** and **FinReflectKG-MultiHop's 555 (of which ~54 are cross-company)**. Against
  FinQA's 8,281 / TAT-QA's 16,552 / MultiHiertt's 10,440 / FinanceBench's 10,231, the multi-document corner of
  this field is roughly two orders of magnitude smaller.
- Every multi-document template I could read names its entities. "Percentage difference of {company1} versus
  {company2}", "last {num_year} years", "same sector and fiscal year" — in all of these, a query rewriter can
  emit *n* fully-specified single-hop queries before any retrieval happens. This is the `[PARALLEL-DOC]`
  signature, and the project's existing structured-query-rewriting work is the matching tool, not a graph index.
- The one shape that is genuinely hop-dependent (SEC-QA's argmax-then-lookup) requires *aggregation over a
  candidate set*, which a document graph does not provide either — you need a numeric index or a computed
  table, i.e. the XBRL `frames` route from the schema note, not edge traversal.
- Therefore the premise under consideration — "a finance corpus that may need multi-hop" — **does not hold in
  the published record**. A finance corpus would give the series a *harder first hop* (many filings, numbers
  that look alike, jargon), a *bitemporal* story (filing date versus period of report), and a *numerical
  reasoning* story. It would not give it a retrieval-hop story.

### Gaps
- Nobody reports, for any financial benchmark, the proportion of questions where the second piece of evidence
  is **not identifiable from the question text**. That is the number that would settle this, and it does not
  exist. The nearest proxies are SEC-QA's complexity taxonomy (uncounted) and FinReflectKG's hop-count split
  (52% 2-hop / 48% 3-hop), which counts *graph pattern length*, not retrieval dependency —
  [arXiv](https://arxiv.org/html/2510.02906v1).
- No finance benchmark has had the DiRe-style or shortcut-solvability analysis applied to it that the general
  multi-hop note records for HotpotQA and MuSiQue. So the finance equivalent of "more than half do not require
  multi-hop reasoning" is **unmeasured**.

---

## Q3. Where do systems actually fail — retrieval recall or numerical reasoning?

### Takeaway
It splits cleanly by setting, and the split is decisive. On the **context-given, single-document** benchmarks
retrieval is at or near ceiling and reasoning is the whole bottleneck — FinQA's retriever gets recall@3 89.66%
while the best system scores 61.24 execution accuracy against 91.16 for human experts, and MultiHiertt's error
taxonomy contains **no retrieval category at all**. On the **open-domain** benchmarks retrieval collapses and
becomes the larger bottleneck — but it collapses on the *first* hop, and even with perfect evidence the answer
ceiling is 65.90.

### Cited Findings

**Single-document: retrieval near ceiling, reasoning bound**
- FinQA retriever (BERT-base) `[RET]` **recall@3 89.66%, recall@5 93.63%** (TF-IDF recall@5 82.91%) —
  [FinQA, ar5iv](https://ar5iv.labs.arxiv.org/html/2109.00122)
- FinQA `[ANS]` execution accuracy: best model FinQANet (RoBERTa-large) **61.24**; **FinQANet-Gold
  (RoBERTa-large) 70.00** with gold facts; **Human Expert 91.16**; General Crowd 50.68 (Table 2). Program
  accuracy: best 58.86, human expert 87.49 — [FinQA, ar5iv](https://ar5iv.labs.arxiv.org/html/2109.00122)
- FinQA error analysis on 50 sampled errors: "**about 15% of errors came from the retriever**"; about half the
  remainder from lack of financial knowledge, the other half mainly numerical reasoning errors (multi-step
  programs, unit conversion, matching numbers to years) — [FinQA,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2109.00122)
- FinQA degradation by step count `[ANS]`: 1-step **67.61**, 2-step **59.08**, more than 2 steps **22.78** —
  the cliff is arithmetic depth, not retrieval — [FinQA,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2109.00122)
- ConvFinQA `[RET]` FinQANet retriever with conversation context **86.38% recall for top-3 facts**; GPT-3
  retriever on a 300-example sample 74.25% average recall. `[ANS]` FinQANet (RoBERTa-large) **68.90** exec acc
  versus **FinQANet-Gold 77.32** and **Human Expert 89.44** (Table 3) — [ConvFinQA,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2210.03849). Retrieval costs ~8.4 points; the remaining ~20.5
  points to human parity are reasoning.
- MultiHiertt `[RET]` fact retrieval recall **top-10 76.4%, top-15 80.8%**; `[ANS]` best MT2Net
  (RoBERTa-large) test **EM 36.22 / F1 38.43** against **Human Expert EM 83.12 / F1 87.03** (60 sampled test
  examples) — [MultiHiertt, ar5iv](https://ar5iv.labs.arxiv.org/html/2206.01347)
- MultiHiertt error analysis (100 sampled test errors, Table 6): **wrong operand or span 43%, missing operand
  21%, wrong program 19%, lack of domain knowledge 4%**; the paper states about **64% of errors come from
  failing to integrate supporting facts correctly**. There is **no retrieval error category**, and the listed
  categories sum to 87% with the remaining 13% unexplained — [MultiHiertt,
  ar5iv](https://ar5iv.labs.arxiv.org/html/2206.01347)
- MultiHiertt degradation `[ANS]` F1 by steps: 1 step 47.80, 2 steps 37.91, 3 steps 24.57, >3 steps 17.19; and
  by table count: table-only 38.55 falling to **24.96 when ≥2 tables**, table-text 35.15 falling to 23.36 —
  [MultiHiertt, ar5iv](https://ar5iv.labs.arxiv.org/html/2206.01347)
- On the FinanceRAG family (FinQABench, FinQA, ConvFinQA, FinanceBench, TATQA), a two-stage reranking pipeline
  reaches `[RET]` **nDCG@20 0.7918** (jina-reranker-v3 → Qwen3-Reranker-8B), versus 0.7260 for the no-reranker
  row, while generation accuracy `[ANS]` is reported separately at 0.8152 (Claude-4.6 Opus with context
  management) — [Hierarchical Reranking for Scalable Financial RAG,
  arXiv 2607.27523](https://arxiv.org/html/2607.27523v1). Rerankers, not graphs, are what moves retrieval here.

**Open-domain: retrieval collapses, on the first hop**
- FinanceBench `[ANS]`, human-eval sample n=150 — the single most useful table in the finance literature for
  this decision:

  | Configuration | Correct | Incorrect | Failed to answer |
  |---|---|---|---|
  | GPT-4-Turbo, Closed Book | 14 (9%) | 5 (3%) | 126 (88%) |
  | Llama2, Shared Vector Store | 29 (19%) | 104 (70%) | 17 (11%) |
  | GPT-4-Turbo, **Shared** Vector Store | 29 (19%) | 20 (13%) | 101 (68%) |
  | Llama2, Single Vector Store | 62 (41%) | 81 (54%) | 7 (5%) |
  | GPT-4-Turbo, **Single** Vector Store | 75 (50%) | 17 (11%) | 58 (39%) |
  | Claude2, Long Context | 114 (76%) | 32 (21%) | 4 (3%) |
  | GPT-4-Turbo, Long Context | 118 (79%) | 26 (17%) | 6 (4%) |
  | GPT-4-Turbo, **Oracle** | 128 (85%) | 22 (15%) | 0 (0%) |

  — [FinanceBench, ar5iv](https://ar5iv.labs.arxiv.org/html/2311.11944). The headline "81%" is the shared-store
  row: 68% refused plus 13% incorrect. **The paper gives no formal retrieval-versus-reasoning error split.**
- The FinanceBench jump from **Shared Vector Store 19% → Single Vector Store 50% → Oracle 85%** is a pure
  retrieval effect over a corpus of ~360 filings, with the question itself unchanged —
  [ar5iv](https://ar5iv.labs.arxiv.org/html/2311.11944)
- LOFin-1.4k, Table 2, over 145,897 filings — `[RET]` average page recall against `[ANS]` average answer
  accuracy, with *k* the average passages sent to the generator:

  | Method | page recall `[RET]` | answer acc `[ANS]` | k |
  |---|---|---|---|
  | GPT-4o (zero-shot) | – | 13.92 | – |
  | Perplexity | – | 10.55 | – |
  | Self-RAG | 18.96 | 7.63 | 10.0 |
  | RQ-RAG | 18.54 | 8.34 | 36.0 |
  | IRCoT (BM25) | 25.15 | 22.31 | 20.0 |
  | HybridSearch (BM25+dense) | 24.39 | 25.62 | 10.0 |
  | HHR | 33.31 | 28.67 | 10.0 |
  | Dense (text-embedding-3-small + DeBERTa-v3 reranker) | 34.78 | 29.22 | 10.0 |
  | HiREC | **45.35** | **42.36** | 3.7 |
  | **Gold evidence** | **100** | **65.90** | 1.3 |

  — [HiREC](https://arxiv.org/html/2505.20368v3). Table 3 adds HiREC page precision 21.79.
- LOFin per-source (Table 6, page recall / answer accuracy): FinanceBench Dense 26.11 / 33.33, HiREC 40.00 /
  50.00; FinQA Dense 40.38 / 27.61, HiREC 52.49 / 41.61; **SEC-QA Dense 15.70 / 9.45, HiREC 19.97 / 14.17** —
  [HiREC](https://arxiv.org/html/2505.20368v3). The multi-document slice is where both retrieval and answers
  are worst by a wide margin.
- LOFin-1.6k (Tables 10–11): Dense precision 3.38 / recall 29.27 / accuracy 26.12; HiREC precision 20.27 /
  recall 41.70 / accuracy 35.61 — [HiREC](https://arxiv.org/html/2505.20368v3)
- Fin-RATE `[RET]` best R@10: **DR 44.84 (BM25+reranker), EC 9.73 (vector+reranker), LT 44.25
  (BM25+reranker)**; `[ANS]` best with top-5 retrieved context: DR 26.20, **EC 10.80**, LT 18.80; `[ANS]` with
  **gold context**: GPT-5-websearch 42.96 / 43.64 / 43.52, best DR 57.48 (Fin-R1); **human experts 92.40 (DR),
  82.68 (EC), 85.88 (LT)** (Tables 2–4) — [Fin-RATE](https://arxiv.org/html/2602.07294)
- Fin-RATE error attribution: "**Missing Evidence accounts for 75.44% of EC-QA retrieval errors**" (Figure 5
  text); BM25-versus-dense top-passage Jaccard similarity is **0.0412 (DR), 0.0113 (EC), 0.0603 (LT)**, which
  the authors link to weak hybrid fusion — [Fin-RATE](https://arxiv.org/html/2602.07294)
- DocFinQA `[RET]` fine-tuned ColBERT **HR@1 0.35, HR@3 0.55** (a 91% average HR improvement over
  Sentence-BERT; **BM25 "performed poorly"**); `[ANS]` test GPT-3.5 42.6 with FT ColBERT, dev **upper bound
  with the original golden chunk 67.3**, retrieved-context configurations 24.8–40.7; 100K+ case study (200
  docs): **human 41.0**, GPT-4 Retrieval 47.5, GPT-4 S2A 23.0, GPT-4 Iterative 20.0 (Tables 2–4) —
  [DocFinQA](https://arxiv.org/html/2401.06915v3). The authors attribute human difficulty to "the difficulty
  of finding the golden page".
- FinDER `[ANS-JUDGE]` RAGAS context recall (Table 4, total row): **BM25 11.68, GTE 17.83, mE5 17.36,
  E5-Mistral 25.95**; precision on a 500-query sample (Table 5): BM25 10.8, GTE 18.1, mE5 17.5, E5-Mistral
  25.7. LLM reranking of E5-Mistral's top 10 down to 5 (Table 6, F1): Claude-3.7-Sonnet 63.05, GPT-o1 62.90 —
  [FinDER](https://arxiv.org/html/2504.15800v2). These are LLM-judged, not qrels-based.
- SEC-QA `[RET]`/`[ANS]` correlation: R² between accuracy and **page recall 0.94**, **document recall 0.76**,
  **page precision 0.12** (Section 5.1) — [SEC-QA, ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394)
- SEC-QA `[ANS]`: single-value CodeGen+DocS+PageR **89.5%** vs Vanilla RAG 55.8% vs CodeGen+PageR 31.6%
  (Section 4.3); compound-value (Table 3) Vanilla RAG 8.3%, Multi Query RAG 16.7%, CodeGen+PageR 37.5%,
  CodeGen+DocS+PageR 33.3%; **multi-document 108/135 (80%) at k=32** falling to 74 (55%) at k=4, CodeGen+PageR
  71/135 (52%) at k=48, Vanilla RAG 41/135 (30%) at k=32 (Section 4.5). CodeGen systems make about **4× as many
  LLM calls** (Section 5.4) — [SEC-QA, ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394)

### Inferences
- **The decisive pattern: retrieval is near-ceiling exactly where the corpus is one document, and the
  bottleneck there is unambiguously reasoning.** FinQA 89.66 recall@3 with a 15%-of-errors retrieval share;
  MultiHiertt with an error taxonomy that is 83% operand/program errors and 0% retrieval. If the series moves
  to finance to test *retrieval*, these benchmarks cannot serve — they would measure the reader, not the
  retriever.
- The open-domain numbers do show a real retrieval problem (LOFin page recall 34.78 for dense+reranker,
  Fin-RATE EC R@10 9.73), but read the shape of it: Fin-RATE's EC-QA failure is **75.44% "Missing Evidence"**
  over a **5.6-chunk gold set** evaluated at **top-5**. You cannot fit 5.6 gold chunks into k=5. That is a
  *budget* failure, and the fix is a larger k, per-entity sub-queries, or set-aware evaluation — not edges.
  SEC-QA shows the same thing from the other side: multi-document accuracy rises from 55% at k=4 to 80% at
  k=32 with the retriever unchanged.
- **Gold-evidence ceilings prove reasoning is also broken in the open-domain setting.** LOFin's gold-evidence
  row is 100 page recall and only **65.90** answer accuracy; Fin-RATE's gold-context best is **43.64** on EC
  against 82.68 for human experts. So in finance, perfect retrieval buys you a third to two-thirds of the
  answers. Any episode that improves retrieval on a finance corpus will be measuring against a reader that is
  itself the dominant error source — which makes `[ANS]` metrics nearly useless there and makes passage-level
  `[RET]` gold mandatory.
- SEC-QA's R² of 0.94 between answer accuracy and *page* recall versus 0.12 for page *precision* is the
  strongest published statement that in finance the thing to optimise is recall of the right page. That is a
  first-hop ranking objective, and it is precisely what BM25+dense+RRF+cross-encoder is for.
- BM25 behaves oddly in this domain and it is worth a sentence in the episode: BM25+reranker is the **best**
  retriever on Fin-RATE's DR and LT tasks (44.84 and 44.25 R@10) yet "performed poorly" on DocFinQA, and the
  BM25/dense overlap is near zero (Jaccard 0.0113–0.0603). Low overlap is the condition under which RRF pays
  most — a point the project's existing pipeline can claim support for.

### Gaps
- **No financial benchmark publishes a clean joint error decomposition** ("x% of failures are because the gold
  page was never retrieved, y% because the arithmetic was wrong"). FinQA's 15% retriever share is the closest,
  on 50 sampled errors. FinanceBench explicitly declines to split them —
  [ar5iv](https://ar5iv.labs.arxiv.org/html/2311.11944).
- Fin-RATE's abstract figures (accuracy dropping "by 18.60% and 14.35%") could not be reconciled with Table 2,
  and the paper does not say which figure maps to LT-QA versus EC-QA, nor whether they are absolute or relative
  — [arXiv](https://arxiv.org/html/2602.07294). Record as unverified.
- LOFin's own inconsistency (205 versus 206 added pairs) suggests its tables should be re-derived from the repo
  before any figure is published — [HiREC](https://arxiv.org/html/2505.20368v3).

---

## Q4. Is there published evidence of graph or multi-hop retrieval helping on financial corpora specifically?

### Takeaway
There are four finance-specific graph-retrieval evaluations I could find, and **not one of them reports a
qrels-based retrieval metric against a tuned BM25+dense+cross-encoder baseline**. The project's standing
finding holds in the finance literature without exception. Worse for the graph case: where a finance paper does
report retrieval recall against gold, the graph **loses on recall** (HybridRAG) or wins on retrieval and then
**fails to convert it into answers** (FinGEAR's GraphRAG arm).

### Cited Findings

**HybridRAG (arXiv 2408.04948, Aug 2024; BlackRock + NVIDIA)** — no gold-passage metrics, no competitive baseline
- Corpus: **50 earnings call transcripts** from Nifty-50 constituents, Q1 FY2024 (quarter ending June 2023);
  **400 questions** randomly selected with gold answers taken from the transcripts — [HybridRAG, arXiv
  HTML](https://arxiv.org/html/2408.04948v1)
- All reported metrics are RAGAS `[ANS-JUDGE]` (Table 5, "Performance Metrics for Different RAG Pipelines"):

  | Pipeline | Faithfulness | Answer Relevance | Context Precision | Context Recall |
  |---|---|---|---|---|
  | VectorRAG | 0.94 | 0.91 | 0.84 | **1.00** |
  | GraphRAG | 0.96 | 0.89 | 0.96 | **0.85** |
  | HybridRAG | 0.96 | 0.96 | 0.79 | **1.00** |

  — [HybridRAG](https://arxiv.org/html/2408.04948v1)
- "**No retrieval metric such as recall@k or nDCG against gold passages is reported. The paper does not say
  whether any gold passage labels exist.**" The vector baseline is OpenAI text-embedding-ada-002 in Pinecone
  with similarity search, 20 candidate chunks → 4 retrieved contexts (Table 3); **BM25, hybrid sparse+dense
  search and cross-encoder reranking are not mentioned** — [HybridRAG](https://arxiv.org/html/2408.04948v1)
- Internal inconsistencies to flag if cited: "50" companies/documents in Table 1 versus "over 50 companies" in
  the text; 400 questions versus Table 1's average of 16 questions per document across 50 documents (~800);
  chunk size 1024 with zero overlap in Table 3 versus 2024 characters with 204 overlap in Section 4.1 —
  [HybridRAG](https://arxiv.org/html/2408.04948v1)
- A secondary search result attributes to this line of work an accuracy rise "from 40 to 55 percent using
  GPT-4o, and from 36.36 to 56 percent using GPT-4o-mini on synthetic multi-hop financial queries" over SEC
  10-Q filings — I could **not** verify these numbers in the paper HTML I fetched; treat as unverified —
  [search result, HybridRAG PDF](https://arxiv.org/pdf/2408.04948)

**FinGEAR (arXiv 2509.12042, Sept 2025)** — the only finance paper with gold-based retrieval recall for
GraphRAG and LightRAG
- Setup: FinQA's 8,281 QA pairs re-grounded on **720 full-length 10-K filings** from S&P 500 companies
  (1999–2019), PDF→Markdown; FinGEAR itself is a **hierarchical Summary Tree + Question Tree per SEC Item**
  (UMAP + GMM clustering, "depth at most 2"), **not** a graph; LightRAG and GraphRAG are the graph baselines
  and RAPTOR the tree baseline — [FinGEAR, arXiv HTML](https://arxiv.org/html/2509.12042v1)
- `[RET]` Table 1, Precision / Recall / F1 at **k = 5 / 10 / 15**:

  | Method | Precision | Recall | F1 |
  |---|---|---|---|
  | General RAG (= vanilla BM25 + dense, App. C) | 0.37 / 0.37 / 0.30 | **0.24 / 0.26 / 0.28** | 0.29 / 0.30 / 0.29 |
  | Self-RAG | 0.74 / 0.60 / 0.55 | 0.27 / 0.28 / 0.31 | 0.39 / 0.38 / 0.40 |
  | LightRAG | 0.88 / 0.85 / 0.85 | 0.39 / 0.42 / 0.47 | 0.54 / 0.56 / 0.60 |
  | GraphRAG | 0.88 / 0.89 / 0.87 | **0.56 / 0.55 / 0.55** | 0.67 / 0.66 / 0.66 |
  | RAPTOR | 0.69 / 0.65 / 0.62 | 0.11 / 0.14 / 0.22 | 0.19 / 0.23 / 0.32 |
  | FinGEAR | 0.79 / 0.76 / 0.72 | **0.61 / 0.62 / 0.65** | 0.69 / 0.68 / 0.68 |

  — [FinGEAR](https://arxiv.org/html/2509.12042v1)
- `[ANS]` Table 3, accuracy with a fixed GPT-4o-mini reader at k = 5 / 10 / 15: General RAG **29.8 / 30.3 /
  30.5**, Self-RAG 28.7 / 29.8 / 27.4, LightRAG 35.7 / 58.8 / 36.5, **GraphRAG 28.4 / 29.1 / 29.4**, RAPTOR
  34.0 / 20.9 / 37.3, FinGEAR **49.1 / 49.7 / 50.0** — [FinGEAR](https://arxiv.org/html/2509.12042v1)
- Baselines **not** reported with numbers: pure BM25, pure dense, hybrid, HiQA. The cross-encoder
  (BAAI/bge-reranker-large) appears **only inside FinGEAR**, ablated as "No Reranker" in Table 2 —
  [FinGEAR](https://arxiv.org/html/2509.12042v1)
- The paper does not state whether a query's gold evidence spans multiple filings, and notes in Limitations
  that **FinQA labels only one relevant span per query** — [FinGEAR](https://arxiv.org/html/2509.12042v1)
- LightRAG's 58.8 at k=10 (versus 35.7 at k=5 and 36.5 at k=15) is a non-monotonic outlier the paper does not
  explain; do not cite it as a trend.

**FinReflectKG-MultiHop (arXiv 2510.02906, Oct 2025)** — the "KG retrieval" arm is an oracle-evidence arm
- The three evidence settings are **S1 KG-linked minimal evidence (exact interconnected KG chunks)**, **S2
  page-window evidence (±5 pages around relevant chunks, deduplicated and source-tagged, meant to approximate
  vector/semantic search)**, and **S3 distractor-augmented** — [arXiv](https://arxiv.org/html/2510.02906v1)
- Metrics are `[ANS-JUDGE]` LLM-as-a-Judge correctness 0–10 (Qwen3-235B, with Gemini 2.5 Pro as a second
  judge), `[ANS]` BERTScore F1 (deberta-xlarge-mnli), and token counts (Tables 2–3). "**The paper reports no
  recall, precision, or evidence-hit metrics.**" There is **no BM25, dense-hybrid or cross-encoder reranker
  baseline** — [arXiv](https://arxiv.org/html/2510.02906v1)
- The "~24%" headline is Table 10's per-model KG-over-page-window correctness gains **13.6% (GPT-OSS-120B),
  20.0% (GPT-OSS-20B), 24.9% (Qwen3-32B), 39.2% (Qwen3-8B)**, mean ≈24.4%; the "~84.5%" is Table 11's
  **input-token** savings 84.2 / 84.2 / 84.8 / 84.8% — [arXiv](https://arxiv.org/html/2510.02906v1)
- The judge (Qwen3-235B) is from the same family as the pattern generator; the authors acknowledge the possible
  bias and add Gemini 2.5 Pro as a check — [arXiv](https://arxiv.org/html/2510.02906v1)

**GraphRAG for finance, GenAIK workshop (ACL 2025)** — no retrieval metrics, no named baseline
- Reports a **6% reduction in hallucinations** and an **80% decrease in token usage** "compared with
  conventional RAG methods", plus contradiction-detection complexity falling from O(n²) to O(k·n) and a
  "734-fold decrease" in token consumption in a DORA-versus-FFIEC HybridRAG comparison. The benchmark is named
  as "Finance Bench"; the regulatory comparison is EU DORA versus US FFIEC guidelines — [ACL Anthology
  2025.genaik-1.6](https://aclanthology.org/2025.genaik-1.6/)
- No recall, precision or hit-rate figures; the only baseline named is "conventional RAG methods". The authors'
  own conclusion: graph retrieval's "performance and token usage depend on the dataset, knowledge graph design,
  and retrieval task" — [ACL Anthology](https://aclanthology.org/2025.genaik-1.6/)

**What the strongest finance retrieval papers compare against (for the baseline-quality ledger)**
- LOFin/HiREC evaluates Self-RAG, RQ-RAG, IRCoT (BM25), HybridSearch (BM25+dense scores), HHR and Dense
  (text-embedding-3-small + **DeBERTa-v3 reranker**). **GraphRAG, HybridRAG and DocFinQA are discussed in
  related work only and are not evaluated as baselines** — [HiREC](https://arxiv.org/html/2505.20368v3)
- The FinDER-based reranking study uses SQLite **FTS5** full-text search (top-20, GPT-4.1-extracted keywords) +
  text-embedding-3-small in FAISS (top-30) + **RRF with k=60** + jina-reranker-v2-base-multilingual — a
  pipeline essentially identical in shape to this project's — and reports **no nDCG, Recall@k or MAP at all**,
  only `[ANS-JUDGE]` LLM-judge scores (Table I: avg 6.02 with reranking versus 4.95 without; Score≥8 49.0%
  versus 33.5%) — [arXiv 2603.16877](https://arxiv.org/html/2603.16877v1)
- The FinanceRAG competition entry "Multi-Reranker" reports `[RET]` **nDCG@10 0.63996** on the Task 1 public
  leaderboard with per-dataset reranker selection (bge-reranker-v2-m3, jina-reranker-v2-base-multilingual,
  gte-multilingual-reranker-base) and **no BM25 or dense baseline numbers** — [arXiv
  2411.16732](https://arxiv.org/html/2411.16732v1)

### Inferences
- **The HybridRAG row is the single most useful number in this whole question.** On a finance corpus, with the
  authors' own metric, the graph arm's context recall is **0.85 against the plain vector arm's 1.00**. The
  graph bought precision (0.96 vs 0.84) and paid recall. Given SEC-QA's R²=0.94 for page recall versus 0.12
  for page precision, the graph traded away the quantity that predicts finance answer accuracy for the one
  that does not.
- **FinGEAR is the cleanest counter-example to "graph retrieval helps on filings".** GraphRAG more than doubles
  retrieval recall over the unreranked BM25+dense bag (0.55 vs 0.26 at k=10) and still **loses on answer
  accuracy** (29.1 vs 30.3). Meanwhile a plain two-level hierarchical index over the document's own Item
  structure beats it on both (0.62 recall, 49.7 accuracy). That is the schema-derived-structure result from the
  prior note reappearing in finance: *document structure beats induced edges*.
- FinReflectKG-MultiHop's design means its headline cannot support a retrieval claim at all. S1 is the set of
  exact interconnected KG chunks — i.e. **gold evidence** — and S2 is a ±5-page window. The comparison measures
  *how much a tight evidence set beats a loose one for a reader*, which is the same effect as FinanceBench's
  oracle-versus-retrieved rows (85% vs 50%) and LOFin's gold row (65.90 vs 42.36). It is not evidence that a
  graph index retrieves better; there is no retriever in the comparison.
- **The project's standing claim survives intact and can be stated more strongly for finance than for the
  general literature.** In general multi-hop work the gap was "no paper uses a tuned BM25+dense+cross-encoder
  baseline". In finance it is worse: of the four graph papers, two report no retrieval metric of any kind, one
  reports only LLM-judged RAGAS proxies, and the one with gold-based recall (FinGEAR) compares against an
  explicitly *vanilla* BM25+dense bag with no reranker. Nobody in finance has run the baseline this project
  already has.
- Corollary for the episode plan: a finance episode could produce a genuinely novel measurement — the first
  reranked-hybrid-versus-graph comparison on filings with page-level qrels. LOFin is the only public dataset
  that makes it possible.

### Gaps
- FinGEAR's PDF body text would not extract; all FinGEAR figures above come from the arXiv HTML rendering of
  v1 — [https://arxiv.org/html/2509.12042v1](https://arxiv.org/html/2509.12042v1). Confirm Table 1/3 numbers
  against the camera-ready before publishing.
- I found **no** study evaluating Microsoft GraphRAG (or LightRAG, or HippoRAG) over SEC filings with
  **qrels-based** recall@k or nDCG and a reranked hybrid baseline. A targeted search for "GraphRAG 10-K
  retrieval recall hybrid baseline" returned nothing matching — the closest hits were HybridRAG (10-Q, RAGAS
  only), the general-text RAG-vs-GraphRAG study ([arXiv
  2502.11371](https://arxiv.org/html/2502.11371v1), not finance) and a scientific-facility study ([arXiv
  2607.24663](https://arxiv.org/pdf/2607.24663), not finance, strict vital recall 70.3% with the authors
  themselves saying the lead is not statistically supported).
- The AWS capital-markets GraphRAG post and the sec-graph / SEC-GraphRAG repos from the prior note still report
  no retrieval accuracy metrics; nothing I found in this pass changes that.

---

## Q5. Practical availability: licence, document redistributability, and whether a gold *retrieval* set exists

### Takeaway
Only two public finance datasets give passage- or page-level gold against a real multi-document corpus —
**LOFin** (page-level, 145,897 filings) and **Fin-RATE** (chunk-level, 15,311 chunks) — and both are 2025–2026
preprints whose licences I could not fully verify. Everything older either has no retrieval corpus at all
(so Hit@k is undefined) or is answer-only. **SEC-QA, the one canonical multi-document benchmark, is built on a
private commercial database and is not released.**

### Cited Findings

| Benchmark | Licence | Documents redistributable? | Passage/page-level gold for `[RET]` scoring? |
|---|---|---|---|
| FinQA | Paper states "dataset and code are publicly available" at [github.com/czyssrs/FinQA](https://github.com/czyssrs/FinQA); MultiHiertt's paper records FinQA's licence as **MIT** — [MultiHiertt §7](https://ar5iv.labs.arxiv.org/html/2206.01347) | Yes — the 2,789 report pages are distributed as the task context | **Supporting facts within the given page only.** No corpus, so no qrels and no Hit@k over documents |
| ConvFinQA | "dataset and code is publicly available"; **no licence stated in the paper** — [ar5iv](https://ar5iv.labs.arxiv.org/html/2210.03849) | Yes (2,066 report pages as context) | Same as FinQA; retrieval is over facts in one page |
| TAT-QA | **CC BY 4.0** per the repo page, **MIT** per the repo sidebar — the page does not reconcile them — [repo](https://github.com/NExTplusplus/tat-qa) | Yes (2,757 hybrid contexts); test-set ground truth released Jan 2024 | **No** — context is given; there is nothing to retrieve |
| MultiHiertt | **No licence stated for MultiHiertt itself**; §7 says the source licences (FinQA **MIT**, FinTabNet **CDLA-Permissive-1.0**) permit distributing additional annotations — [ar5iv](https://ar5iv.labs.arxiv.org/html/2206.01347) | Yes (2,513 documents) | **Intra-document fact-level only** (avg 7.02 supporting facts/question). Usable for within-document Hit@k, not for corpus retrieval |
| FinanceBench | Open subset **cc-by-nc-4.0**, 150 rows — [HF card](https://huggingface.co/datasets/PatronusAI/financebench). Full 10,231 requires a licence from Patronus — [ar5iv](https://ar5iv.labs.arxiv.org/html/2311.11944) | **No** — "The PDFs linked in the dataset can be found here as well" (a GitHub folder); the dataset itself is JSON with a `doc_link` column to externally hosted filings — [HF card](https://huggingface.co/datasets/PatronusAI/financebench) | **Yes, page-level** — `evidence` text plus `evidence_page_num`, plus full page text — for the 150 public rows only |
| DocFinQA | arXiv page shows **CC BY-NC-SA 4.0**; the text does not separately state the dataset licence — [arXiv](https://arxiv.org/html/2401.06915v3) | 801 filings were downloaded from EDGAR and converted with Kensho Extract; the ACL version says the dataset is publicly accessible — redistribution of the converted documents needs checking in the repo | **Yes, chunk-level** — the gold chunk c★ (2,750 chars, 20% overlap). Genuine within-document Hit@k |
| FinDER | **No licence stated.** Paper: used for "an official evaluation with partners", public release "at a later time", contact support@linqalpha.com — [arXiv](https://arxiv.org/html/2504.15800v2). A third-party page claims HF availability at `Linq-AI-Research/FinDER` — [alphaxiv](https://alphaxiv.org/benchmarks/yonsei-university/finder) | Corpus is 10-Ks of 490–500 S&P 500 companies "preprocessed into searchable paragraphs"; redistribution unstated | Evidence passages exist in the triplets, but the paper evaluates with **RAGAS LLM-judged context recall/precision**, not qrels — [arXiv](https://arxiv.org/html/2504.15800v2) |
| FinQABench | Unknown | Unknown | Unknown — see Gaps |
| FinTextQA | ACL 2024; hosted at HF `GPS-Lab/FinTextQA` — [arXiv](https://arxiv.org/html/2405.09980v1) | Source-attributed passages from textbooks and agency sites | Source attribution exists but it is a long-form answer task; no filings corpus |
| SEC-QA | **No licence and no public release stated**; data comes from "private-sector financial data from market-trusted sources" (§3.2), generated automatically from an existing financial database — [ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394) | **No** | **Document- and page-level provenance only** (the database records the source document per value); **no passage/chunk gold** — [ar5iv](https://ar5iv.labs.arxiv.org/html/2406.14394) |
| FinMMR | ICCV 2025 / arXiv Aug 2025 — [arXiv](https://arxiv.org/html/2508.04625v1) | 8.7K images supplied with questions | **No retrieval corpus at all** |
| LOFin / HiREC | Code and benchmark at [github.com/deep-over/LOFin-bench-HiREC](https://github.com/deep-over/LOFin-bench-HiREC); **licence not stated in the paper** — [arXiv](https://arxiv.org/html/2505.20368v3) | Corpus is 145,897 SEC reports; the repo's distribution model is unverified | **Yes, page-level, over an open-domain corpus** — the only public finance benchmark where qrels-style page recall/precision is already the headline metric |
| Fin-RATE | Body text gives no dataset licence; a footnote reads "cc-license: by" (CC BY, version unconfirmed); HF `GGLabYale/Fin-RATE` — [arXiv](https://arxiv.org/html/2602.07294) | 15,311 chunks of filing text are the corpus | **Yes, chunk-level gold set per query**, with cross-entity and longitudinal tiers |
| FinReflectKG-MultiHop | Paper page **CC BY-NC-ND 4.0**; data at an anonymous 4open.science repo; parent FinReflectKG on HF under iGeniusAI — [arXiv](https://arxiv.org/html/2510.02906v1) | KG triplets with source attribution; filings not redistributed | **No retrieval gold and no retrieval metrics** |

- Reinforcing the qrels point for the FinanceRAG family: the two strongest recent pipelines over it report
  either `[RET]` nDCG only at the leaderboard level (0.63996 overall, no per-dataset or baseline breakdown) —
  [Multi-Reranker](https://arxiv.org/html/2411.16732v1) — or `[RET]` **nDCG@20 only** with "no BM25 baseline"
  and no mention of qrels — [arXiv 2607.27523](https://arxiv.org/html/2607.27523v1). The competition's own
  per-dataset query and corpus counts were **not** recoverable from any paper I could read, and the
  `Linq-AI-Research/FinanceRAG` HF page returned HTTP 401 to my fetch.

### Inferences
- For a project that scores with Hit@k and qrels, the practical shortlist is exactly: **LOFin** (page-level,
  open-domain, includes the SEC-QA-derived multi-document slice — and already reports the reranked-dense
  baseline this project would want to beat), **DocFinQA** (chunk-level, within-document, 801 filings),
  **FinanceBench's 150** (page-level, NC licence) and **Fin-RATE** (chunk-level, with an explicit cross-entity
  tier). Everything else is answer-only or context-given.
- The NC clauses matter here: FinanceBench's public subset is **cc-by-nc-4.0** and DocFinQA's arXiv page is
  **CC BY-NC-SA 4.0**. For a blog series that is itself non-commercial this is probably fine, but figures
  reproduced from them inherit the share-alike/NC conditions, and the FinanceBench PDFs are not
  redistributable from the dataset at all.
- A genuine risk for the "move to finance" plan: the only benchmark whose *questions* are multi-document and
  whose *corpus* is public is LOFin, and its multi-document slice is 333 questions inherited from SEC-QA
  templates — i.e. the series would end up measuring itself against 333 parallel-retrieval questions with
  page-level gold, where the published state of the art has page recall 19.97 and answer accuracy 14.17
  ([HiREC Table 6](https://arxiv.org/html/2505.20368v3)). That is a hard, honest target, but it is a
  *first-hop recall* target.

### Gaps
- FinQABench: no licence, size or primary source found.
- LOFin's licence and whether the 145,897-filing corpus is distributed (versus a download script) —
  unverified; the repo would settle it.
- Fin-RATE's licence version, and whether the 15,311 chunks are redistributed — unverified beyond the
  "cc-license: by" footnote.
- FinanceRAG per-dataset query/corpus counts and whether qrels were released for all seven subsets — not
  recoverable from the papers; HF org page returned 401.
- Whether DocFinQA's converted markdown documents are redistributed or must be re-fetched from EDGAR.

---

## Q6. SEC EDGAR as a raw corpus: practical gotchas, and is an "as filed at date D" view reconstructible?

### Takeaway
EDGAR is unusually friendly to a bitemporal design — it natively carries **two dates** (the date EDGAR accepted
the filing, and the period the filing reports on), and amendments arrive as **separate submissions that never
overwrite the original**, so "as filed at date D" is reconstructible by filtering on acceptance date. The
practical costs are elsewhere: a request-rate ceiling with mandatory self-identification, a structural
discontinuity at the inline-XBRL phase-in dates, near-duplicate text between a filing and its amendment, and
item segmentation inside documents that run to hundreds of pages.

*No sec.gov URL was fetched for this section, per the constraint. Everything below is from third-party
documentation and literature, and is flagged as such. The prior note's sec.gov-sourced findings on the
submissions API, XBRL APIs, Financial Statement Data Sets, 13F data sets, 8-K item metadata and exhibits
incorporated by reference are taken as given and not repeated.*

### Cited Findings

**Fair access, rate limits and the User-Agent**
- "**The SEC doesn't publish specific rate limits**"; the recommended guideline is "**No more than 10 requests
  per second**", plus keeping daily volume reasonable and avoiding excessive concurrency —
  [edgartools, SEC compliance](https://edgartools.readthedocs.io/en/stable/resources/sec-compliance/)
- The library defaults to **9 requests per second** (`EDGAR_RATE_LIMIT_PER_SEC`), and the setting must be
  applied **before `edgar` is first imported** because the limiter is built at import time —
  [edgartools](https://edgartools.readthedocs.io/en/stable/resources/sec-compliance/)
- Identity goes in the **`User-Agent` header**; required details are a **name or organisation name and an email
  address**, set via `set_identity(name, email, organization)` or the `EDGAR_NAME` / `EDGAR_EMAIL` environment
  variables. Without either, a generic identity is sent —
  [edgartools](https://edgartools.readthedocs.io/en/stable/resources/sec-compliance/)
- Violations produce **HTTP 429**, **HTTP 403** and **temporary IP blocks "typically 10 minutes to 24 hours"**,
  with longer blocks for persistent violations —
  [edgartools](https://edgartools.readthedocs.io/en/stable/resources/sec-compliance/)
- The SEC's Fair Access Policy is summarised as requiring all automated EDGAR access to: identify the accessing
  party in HTTP requests, limit request rates, respect `robots.txt`, and access data at appropriate hours —
  [edgartools](https://edgartools.readthedocs.io/en/stable/resources/sec-compliance/)
- Operational advice: delays between batch operations (0.2s in the example), exponential backoff with random
  jitter, circuit breakers, off-peak scheduling; the page describes peak hours as 09:30–16:00 ET **but does not
  attribute that to an SEC source** — [edgartools](https://edgartools.readthedocs.io/en/stable/resources/sec-compliance/)
- A second third-party source states the same ceiling, quoting SEC Developer Resources: each user is capped at
  **10 requests per second however many machines they use**, with a User-Agent carrying a name and contact
  email — [edgarWebR README](https://packages.oit.ncsu.edu/cran/web/packages/edgarWebR/readme/README.html)

**The two date axes — bitemporality is native**
- Third-party field documentation for EDGAR metadata: **`filingDate` is the ISO date the form was accepted by
  EDGAR**, and **`reportDate` is the period of report (the fiscal period covered)**; the same metadata carries
  **`isXBRL` / `isInlineXBRL` flags** — [sec-api.io, EDGAR Index APIs](https://sec-api.io/docs/edgar-index-apis).
  Vendor documentation; flagged as such.
- Filing metadata is retrievable in bulk without touching document bodies: quarterly indexes via
  `get_filings(year=...)`, or a company's whole submission history in one request via `company.get_filings()`;
  **document content such as `filing.text` costs an extra HTTP request** —
  [edgartools, performance](https://edgartools.readthedocs.io/en/stable/resources/performance/)
- Filings can be filtered by exact date or range, e.g. `filings.filter(filing_date="2023-02-23")` and ranges
  like `2024-02-29:2024-03-15` — [edgartools](https://edgartools.readthedocs.io/en/stable/resources/performance/)

**Restatements, amendments and point-in-time reconstruction**
- The core failure mode in non-point-in-time stores: "**Non-PIT databases typically overwrite historical data
  with data that was later changed or corrected by the company**" — [S&P Capital IQ, point-in-time versus
  lagged fundamentals](https://spglobal.com/content/dam/spglobal/mi/en/documents/general/sp-capitaliq-quantamental-point-in-time-vs-lagged-fundamentals.pdf)
- The bias it creates: a corrected value stamped with the original filing date is "information that genuinely
  did not exist on that date" — [as-reported versus restated
  data](https://dev.to/tradevodata/as-reported-vs-restated-financial-data-why-the-difference-matters-for-backtesting-1big)
- A **separate** and often-missed bias is reporting lag: joining on the fiscal-period-end date leaks data,
  because one EDGAR-derived sample measured fundamentals becoming public **an average of 43 days after the
  fiscal period ended** — [point-in-time fundamentals from SEC
  EDGAR](https://dev.to/tradevodata/how-to-build-a-point-in-time-fundamentals-database-from-sec-edgar-and-when-not-to-2gn6)
- Amendment handling: "an amendment must never overwrite the original record in place", and a **restatement
  that replaces the original filing must be distinguished from an amendment that adds positions** — treating
  one as the other "corrupts the history" (stated for 13F) — [13F point-in-time
  discipline](https://arkolith.com/blog/13f-data-for-quant-research)
- The recommended design is exactly this project's: store **two timestamps per record — the period it describes
  and the date it became public — and query with an explicit as-of date**, described as bitemporal storage with
  event time and knowledge time as separate axes — [point-in-time data
  explained](https://arkolith.com/blog/point-in-time-data-explained)
- All of the restatement sources above except the S&P paper are **vendor blogs with a commercial interest**;
  the S&P white paper is the most authoritative, and **I found no SEC or peer-reviewed source on EDGAR
  amendment handling specifically**.

**Inline XBRL — a structural discontinuity in the corpus**
- Phase-in by filer status, keyed to **fiscal periods ending on or after**: **15 June 2019** for large
  accelerated filers using US GAAP, **15 June 2020** for accelerated filers using US GAAP, **15 June 2021** for
  all other filers including non-accelerated and IFRS filers; compliance begins with the first Form 10-Q filed
  after the applicable date — [Deloitte DART, SEC adopts Inline XBRL for tagged
  data](https://dart.deloitte.com/USDART/tree/vsid/433086); [Locke Lord capital markets
  alert](https://capitalmarkets.lockelord.com/?p=49)
- **Cover-page tagging** for Forms 10-K, 10-Q and 8-K became applicable to large accelerated filers on **15
  June 2019** — [Deloitte DART](https://dart.deloitte.com/USDART/tree/vsid/433086)
- The rule changed the *format*, not the scope: "the amendments modify existing XBRL requirements, they do not
  change the categories of filers or scope of disclosures subject to XBRL requirements" — [Deloitte
  DART](https://dart.deloitte.com/USDART/tree/vsid/433086)

**Document parsing, item segmentation and size**
- The reference corpus-construction effort over EDGAR (EDGAR-CORPUS / EDGAR-CRAWLER, 10-Ks 1993–2020)
  **removed tables to keep only textual data**, stripped HTML, and split filings into items **using regular
  expressions**; the authors' stated motivation is that "existing download toolkits do not support the
  extraction of specific item sections", over documents that "may contain hundreds of pages" —
  [EDGAR-CORPUS, EconNLP 2021](https://aclanthology.org/2021.econlp-1.2); [arXiv
  PDF](https://arxiv.org/pdf/2109.14394)
- The published dataset card notes its train/val/test splits "do not have any special meaning" and recommends
  EDGAR-CRAWLER for loading specific years or companies — [HF
  edgar-corpus](https://huggingface.co/datasets/c3po-ai/edgar-corpus)
- DocFinQA's independent experience of the same problem: 801 filings pulled from EDGAR and parsed to markdown
  with a **finance-specific extractor (Kensho Extract)**, losing ~6.5% (543) of data points at download and
  117 more at PDF conversion, with the resulting average document at **123,453 words** —
  [DocFinQA](https://arxiv.org/html/2401.06915v3)

### Inferences
- **Answering the bitemporality question directly: yes.** EDGAR gives both a filing/acceptance date and a
  period-of-report, exposed as separate metadata fields retrievable in bulk without downloading bodies; and
  because an amendment is a new submission under a `/A` form type with its own accession number (prior note),
  the original is never mutated. So "the corpus as it stood on date D" = filter on acceptance date ≤ D, and
  "what the company said *about* period P as of D" = the latest submission with reportDate = P and filingDate
  ≤ D. That is a textbook bitemporal query and it maps straight onto the project's existing FTS5 time-travel
  work. Restatements are recoverable as the *difference between two such views*.
- The thing EDGAR does **not** give you for free is the join: the Financial Statement Data Sets preserve
  "as filed" values (prior note) but a text corpus needs you to decide, per period, whether the 10-K or the
  10-K/A is "the" document. Keep both and you get near-duplicate retrieval results (most of an amendment's text
  is the original's); keep only the latest and you have silently destroyed the bitemporal property. This is a
  real episode-sized problem and it is *the same shape* as Spectastic's version-of-record question.
- The inline-XBRL dates create a hard seam in any multi-year corpus: a 10-K for FY2018 and a 10-K for FY2021
  from the same company are different kinds of artefact, the later one carrying facts tagged inline in the HTML
  itself. A single HTML-to-text pipeline will treat them inconsistently, and cover-page tagging means even the
  front matter differs. Any "retrieval improved/worsened over time" chart over an EDGAR corpus has to control
  for this or it is measuring the parser.
- Rate limits are not a research obstacle at this project's scale but they are a *reproducibility* obstacle:
  at 9–10 requests per second with one request for metadata and another for each document body, a
  100,000-filing corpus is hours of polite crawling and a 403 risk mid-run, which argues for building from the
  bulk data sets where possible rather than per-filing fetches (and, for this report's purposes, for not
  crawling at all).

### Gaps
- **Filing size limits**: I could not verify any official per-document or per-submission byte limit without
  fetching sec.gov. The edgartools compliance page "gives no guidance on filing size". Record as an open
  question; the figure exists in EDGAR Filer Manual documentation.
- **Exhibit sprawl**: no third-party source I found quantifies exhibits per filing or the share of a 10-K's
  bytes that are exhibits. The prior note establishes the *semantics* (incorporation by reference, furnished
  versus filed) but not the volume.
- No authoritative (SEC or peer-reviewed) source on EDGAR amendment/restatement handling was found; the
  evidence above is one S&P white paper plus vendor blogs.
- Whether EDGAR full-text search coverage begins in 2001 (a commonly repeated figure) could not be confirmed
  without fetching sec.gov.
- The peak-hours claim (09:30–16:00 ET) in the edgartools page is unattributed and should not be presented as
  SEC guidance.

---

## Appendix: source-quality ledger for this note

- **Primary, verified from the paper's own text**: FinQA, ConvFinQA, MultiHiertt, FinanceBench, DocFinQA,
  FinDER, HiREC/LOFin, FinReflectKG-MultiHop, Fin-RATE, HybridRAG, FinGEAR, the FinanceRAG reranking papers.
  All numbers above carry the table or section they came from where the source gave one.
- **Metadata-only pages that did not yield numbers**: [ACL SEC-QA
  record](https://aclanthology.org/2025.finnlp-2.15), [ACL FinQA
  record](https://aclanthology.org/2021.emnlp-main.300/), [ACL GenAIK GraphRAG
  record](https://aclanthology.org/2025.genaik-1.6/) (abstract only). SEC-QA and FinQA figures were recovered
  from ar5iv renderings instead.
- **PDFs that would not extract in this environment** (same limitation as the prior note): [Multi-Reranker
  PDF](https://arxiv.org/pdf/2411.16732), [HiREC PDF](https://arxiv.org/pdf/2505.20368), [FinGEAR
  PDF](https://arxiv.org/pdf/2509.12042). HTML renderings were used; confirm against camera-ready before
  publishing any figure.
- **Vendor/secondary sources, used only where no primary source exists and flagged inline**: edgartools and
  edgarWebR documentation (EDGAR access rules), sec-api.io (metadata field definitions), S&P Capital IQ white
  paper plus three vendor blogs (restatements and point-in-time), Deloitte DART and Locke Lord (inline XBRL
  phase-in — law-firm and accounting-firm summaries of the SEC rule, not the rule itself), alphaxiv (FinDER
  availability claim, contradicting the paper).
- **Known internal contradictions in the cited papers, flagged for the writer**: FinanceBench 361 versus 360
  filings; FinReflectKG S&P 100 corpus versus a parent KG over 743 S&P 500 companies; LOFin 205 versus 206
  added pairs; Fin-RATE 36 industries versus seven sectors; HybridRAG 50 versus "over 50" companies, 400
  questions versus ~800 implied, two different chunk sizes; FinMMR 53.0% versus 51.4% hard accuracy; TAT-QA
  CC BY 4.0 versus MIT; DocFinQA 7,621 (v1) versus 7,437 (v3/ACL) questions.
