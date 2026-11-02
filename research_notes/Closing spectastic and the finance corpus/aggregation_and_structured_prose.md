# Aggregation and top-K questions over a document corpus: structured layer + prose retrieval

Scope note on metrics, applied throughout. Three families are kept separate and labelled every time:
- **Retrieval metrics** — recall@k, chunk-level recall, Hit@4, nDCG. Measure whether evidence was found.
- **Execution / answer-set metrics** — execution accuracy (EA), exact set match, exact match (EM), ACE/NACE (count error). Measure whether the computed answer equals the gold answer.
- **Answer-quality / generation metrics** — F1 over a returned set, numeracy F1, BLEU, faithfulness scores. Measure the text or the set the user sees.

Research current as of 2026-10-29. Publication dates given for every source. No sec.gov fetches were made; the one SEC-rules citation below is via reginfo.gov.

---

## Key Question 1: The architectural patterns for answering aggregation questions over a corpus

### Takeaway

Five documented patterns exist (text-to-SQL over an extracted relational layer; separate table-store + text-index fusion; agentic tool-calling; semantic/metric layer; SPARQL/Cypher generation), and the literature is explicit that the first and the last are the only ones that can *compute* an aggregate, while the prose half is a separate retrieval problem bolted on. The most load-bearing finding is that the published framings of this problem — TAG (CIDR 2025) and AGGBench (Feb 2026) — both say in their abstracts that *neither* text-to-SQL *nor* RAG is sufficient on its own, for different reasons.

### Cited Findings

**Pattern A — text-to-SQL over an extracted relational layer.**
- The canonical statement of its scope limit: "Text2SQL methods focus solely on natural language questions that can be expressed in relational algebra" — i.e. anything needing world knowledge or semantic judgement about a row falls outside it. Biswal, Patel et al., *Text2SQL is Not Enough: Unifying AI and Databases with TAG*, arXiv August 2024, CIDR 2025 — [arXiv:2408.14717](https://arxiv.org/html/2408.14717v1); [CIDR 2025 page](https://www.vldb.org/cidrdb/2025/text2sql-is-not-enough-unifying-ai-and-databases-with-tag.html)
- Index-time requirement is a populated relational schema. Spider 2.0 documents what "real" schemas look like: 632 enterprise workflow problems over databases from real applications that "often have over 1,000 columns", stored in BigQuery/Snowflake, across multiple SQL dialects. Lei, Chen, Ye et al., *Spider 2.0*, submitted 12 November 2024, revised 17 March 2025, ICLR 2025 Oral — [arXiv:2411.07763](https://arxiv.org/abs/2411.07763)
- Where it breaks, measured: the same code-agent setup (o1-preview) solves **21.3% of Spider 2.0** tasks vs **91.2% on Spider 1.0** and **73.0% on BIRD** — all three are *execution accuracy*, not retrieval or answer-quality. [arXiv:2411.07763](https://arxiv.org/abs/2411.07763)

**Pattern B — hybrid: table store and text index queried separately, then fused.**
- This is exactly the OTT-QA setting: "OTT-QA extends Hybrid-QA into a large-scale open-domain QA dataset over tables and text, requiring both table and passage to be retrieved before answering questions." Chen, Chang, Schlinger, Wang, Cohen, *Open Question Answering over Tables and Text*, October 2020 (ICLR 2021) — [arXiv:2010.10439](https://arxiv.org/pdf/2010.10439)
- Index-time requirement is two indexes plus a linking artefact. One line of work materialises the join at index time: a "fused block" representation of 5,409,903 table-text blocks, averaging 357.5 tokens per block and 12.9 blocks per table, over the OTT-QA corpus — [Mixed-modality Representation Learning and Pre-training for Joint Table-and-Text Retrieval in OpenQA, October 2022, arXiv:2210.05197](https://arxiv.org/pdf/2210.05197)
- Where it breaks, measured: open-domain retrieval collapses performance. Closed-setting HybridQA reaches 43.8 EM on test, while OTT-QA dev/test sit at roughly 10 EM — both *answer-set EM*, not retrieval. [HybridQA paper, April 2020, arXiv:2004.07347](https://ar5iv.labs.arxiv.org/html/2004.07347); OTT-QA figures per [arXiv:2010.10439](https://arxiv.org/pdf/2010.10439)

**Pattern C — agentic tool-calling: model issues a DB query, then retrieves prose.**
- TAG is the published framing of this: Table-Augmented Generation, where the model can invoke both relational execution and LM-based semantic operators (built on the LOTUS runtime). Hand-written TAG pipelines reached **0.55 overall exact match** vs **≤0.20 for every baseline**, and up to **3.1× lower execution time**. Table 1, [arXiv:2408.14717](https://arxiv.org/html/2408.14717v1)
- AGGBench's DFA (Disambiguation–Filtering–Aggregation) is the agentic counterpart for the no-schema case, decomposing aggregation into stages. Zhu, Xu, Li, Liu, Qiu, Chen, Jin, *Aggregation Queries over Unstructured Text: Benchmark and Agentic Method*, v1 1 February 2026, v2 3 February 2026 — [arXiv:2602.01355](https://arxiv.org/abs/2602.01355)
- Where it breaks, measured: AGGBench Section 6.4.2 reports ~15% of DFA errors come from incorrect tool parameterisation and ~5% from redundant repeated tool calls. [arXiv:2602.01355v2](https://arxiv.org/html/2602.01355v2)

**Pattern D — semantic layer / metric layer.**
- The design claim: semantic and metrics layers "define business-facing abstractions over physical data" — approved metrics, dimensions, joins and access policies — so the LLM selects from governed definitions rather than inventing SQL. [Cube, *Semantic layer vs metrics layer*](https://cube.dev/articles/semantic-layer-vs-metrics-layer)
- The strongest form removes SQL generation from the model entirely: "The LLM handles natural language; Mosaic generates the query." This is a vendor claim. [Strategy Software blog](https://www.strategy.com/software/blog/metrics-layer-vs-universal-semantic-layer-core-differences-in-ai-data-architecture)
- One academic treatment, GROUND, treats the semantic layer as an enforceable control point and reports that *enforced* governance properties were consistent across models while *judgement* tasks (e.g. recognising an undefined metric) stayed model-dependent. I could not independently verify this paper's identity or figures — see Gaps. [arXiv:2608.26157](https://arxiv.org/pdf/2608.26157)
- Division of labour, per Cube: a metrics layer fixes disagreeing dashboards; "If the next thing on your roadmap is an AI agent answering freely... you'll want the entity model underneath." [Cube](https://cube.dev/articles/semantic-layer-vs-metrics-layer)

**Pattern E — SPARQL / Cypher generation over a knowledge graph.**
- CypherBench states the aggregation argument directly: embedding-based retrieval "typically cannot handle complex aggregation queries where the answer may depend on thousands of entities." 11 multi-domain property graphs, 7.8M entities, >10,000 questions; reported execution accuracy up to **61.58%**. Feng et al., December 2024, ACL 2025 — [arXiv:2412.18702](https://arxiv.org/pdf/2412.18702)
- Index-time requirement is a graph with a schema. CypherBench also records execution *results*, so it doubles as a generic KBQA benchmark for non-Cypher methods. [arXiv:2412.18702](https://arxiv.org/pdf/2412.18702)
- Breadth benchmark: *Mind the Query* (EMNLP 2025 Industry Track), 27,529 NL→Cypher pairs across 11 real-world graph datasets, validated by schema, runtime and value checks plus manual review, with complexity tiers for fine-grained evaluation. [ACL Anthology author page](https://aclanthology.org/people/vashu-chauhan/unverified/)
- A domain benchmark that explicitly tiers to top-K: bio2C spans "filtering conditions, logical operators, aggregation functions, and collection-processing constructs," with its hardest tier using WITH-clause multi-stage aggregation such as top-k ranking; queries were capped at <30 answers for manual verification.
- Where it breaks: small evaluation sets are common (Fact Finder's PrimeKG set is only **69** text-to-Cypher pairs). Fact Finder, August 2024 — [arXiv:2408.03010](https://arxiv.org/pdf/2408.03010)

### Inferences

- The project's question class ("top 5 companies a company is exposed to") is relational algebra — join, group, sum, order, limit — with no semantic judgement in the predicate. That puts it squarely inside Pattern A's expressive scope, which is the *good* news: unlike TAG-Bench's queries, it does not need an LM operator in the predicate. The hard part is therefore not query generation but the existence and correctness of the table (see Q4).
- Patterns A and E are substitutable for this project; the deciding factor is not expressiveness (both do group-by and top-K) but what the extraction produces. A 13F information table is natively relational, so a graph adds a layer without adding power for these three question forms.
- Pattern C is the only one that natively produces *both* halves the author wants (the ranking and the prose explanation) in one control flow, which is why TAG's framing maps most directly onto the blog series' problem statement.

### Gaps

- I could not verify the GROUND paper ([arXiv:2608.26157](https://arxiv.org/pdf/2608.26157)) — it appeared only as a search snippet; its title, authors, date and reported figures are unconfirmed. Treat its claims as unverified until the abstract page is read.
- The bio2C benchmark appeared only in search snippets; I did not locate its canonical URL, publication venue or date, so its tiering claim is uncited.
- No source compared Patterns A–E head-to-head on the *same* aggregation workload. The pattern-by-pattern figures above are not mutually comparable.

---

## Key Question 2: Why vector retrieval fails at aggregation, specifically

### Takeaway

Two papers state the by-construction argument in print (TAG: RAG is "limited to simple relevance-based point lookups to a few data records"; AGGBench: "Existing paradigms such as Text-to-SQL and Retrieval-Augmented Generation fail to achieve this completeness"), and AGGBench supplies the decisive *measurement*: as top-k rises from 5 to 50 on its core set, Naive RAG's chunk recall moves 0.042 → 0.072 while its count error (NACE) gets **four times worse**, 0.463 → 1.855. Raising k does not fix aggregation; it degrades it.

### Cited Findings

**The by-construction statements.**
- "Existing paradigms such as Text-to-SQL and Retrieval-Augmented Generation fail to achieve this completeness." AGGBench formalises entity-level aggregation as *corpus-bounded with strict completeness requirements* — the system must retrieve **every** matching entity, not a relevant one. [arXiv:2602.01355](https://arxiv.org/abs/2602.01355), February 2026
- TAG on RAG: it is "limited to simple relevance-based point lookups to a few data records," and it leaves counting, math and filtering to "a single invocation of the error-prone LM." [arXiv:2408.14717](https://arxiv.org/html/2408.14717v1), August 2024 / CIDR 2025
- TAG's diagnosis of one concrete aggregation failure: RAG's answer was incomplete because "most of the relevant races are not retrieved." [arXiv:2408.14717](https://arxiv.org/html/2408.14717v1)
- CypherBench: embedding retrieval "typically cannot handle complex aggregation queries where the answer may depend on thousands of entities." [arXiv:2412.18702](https://arxiv.org/pdf/2412.18702), December 2024

**The measurement — RAG scores zero on aggregation under exact match.**
- TAG-Bench Table 1, *exact-match accuracy* (answer-set metric), with execution time in seconds:

| Method | Overall | Match | Comparison | Ranking | Aggregation |
|---|---|---|---|---|---|
| Text2SQL | 0.17 (5.63s) | 0.20 (4.72s) | 0.20 (4.01s) | 0.10 (7.26s) | N/A (6.53s) |
| RAG | **0.00** (3.23s) | 0.00 (3.73s) | 0.00 (2.29s) | 0.00 (2.01s) | N/A (4.89s) |
| Retrieval + LM Rank | 0.02 (4.82s) | 0.00 (6.20s) | 0.05 (4.19s) | 0.00 (3.42s) | N/A (5.46s) |
| Text2SQL + LM | 0.13 (9.08s) | 0.10 (11.25s) | 0.10 (3.89s) | 0.20 (11.80s) | N/A (9.38s) |
| Hand-written TAG | 0.55 (2.94s) | 0.60 (1.70s) | 0.65 (5.05s) | 0.40 (2.50s) | N/A (2.50s) |

  Source: [arXiv:2408.14717](https://arxiv.org/html/2408.14717v1). **Important caveat: the aggregation column is "N/A" for every method — the paper did not score the aggregation category quantitatively and offers only qualitative analysis for those queries.** The nearest scored proxy is Ranking: RAG 0.00, Retrieval+LM Rank 0.00, Text2SQL 0.10, TAG 0.40.
- Baseline recall on AGGBench is catastrophically low. Table 1 (GPT-4o-mini), *NACE mean* (answer-set count error, lower better) / *chunk-level recall* (retrieval metric, higher better):
  - LLM-only: 1.770 / **0**
  - Naive RAG: 0.533 / **0.008**
  - LightRAG: 1.311 / 0.031
  - HippoRAG: 0.456 / 0.092
  - DeepNote: 4.174 / 0.031
  - RAT: 0.793 / 0.018
  - ReAct: 0.807 / 0.030
  - Single Agent: 1.966 / 0.042
  - Multi-Agent: 1.891 / 0.044
  - smolagents: 0.742 / 0.026
  - DFA (proposed): 0.861 / **0.211**

  Source: [arXiv:2602.01355v2](https://arxiv.org/html/2602.01355v2). Note DFA has the best *recall* but **not** the best NACE — HippoRAG (0.456) and Naive RAG (0.533) have lower count error. Completeness and count accuracy come apart.
- On AGGBench-Core, DFA reaches recall **0.623** vs **0.096** for the best baseline (LightRAG), ~6.5×. The "5×" figure quoted in the Introduction and Section 6.3.2 refers to Core. On full AGGBench, DFA's recall is ~2.3× HippoRAG's. [arXiv:2602.01355v2](https://arxiv.org/html/2602.01355v2)

**The decisive top-k sweep (Appendix A.7.1, Table 7, AGGBench-Core).**
- Naive RAG, k = 5 → 50: chunk recall **0.042 → 0.072** (peak 0.093 at k=20), while NACE mean **0.463 → 1.855**. More retrieval, four times the count error.
- LightRAG, chunk top-k 5 → 50: recall **0.033 → 0.147**, NACE mean roughly flat **0.930 → 0.888** (peak 0.902 at k=20).
- Source: [arXiv:2602.01355v2](https://arxiv.org/html/2602.01355v2). This is the single clearest published measurement that enlarging k is not a fix: recall rises, the aggregate gets worse or stays wrong.

**Why the corpus makes it worse — evidence density.**
- AGGBench base corpus: 4,755 chunks of which **294 relevant (6.18%)**. Expanded corpus: 16,294 chunks of which **178 relevant (1.09%)**. The Introduction states a minimum density of 0.85%, which **conflicts** with the 1.09% in Section 6.2.2 — flagging the inconsistency rather than picking one. [arXiv:2602.01355v2](https://arxiv.org/html/2602.01355v2)
- Answer-set sizes are large: **165 of 362 queries have answers larger than 5**; maximum answer size is 20 (single-condition) and 29 (composite). A fixed k=4 or k=10 cannot return these sets even with perfect ranking. [arXiv:2602.01355v2](https://arxiv.org/html/2602.01355v2)

**Corroborating but weaker sources.**
- A personal-memory RAG evaluation found aggregate questions that failed at Top 5 were partly recovered at Top 10 and Top 20, but some remained unresolved, which the authors suggest may need a redesigned retrieval unit or memory structure — [arXiv:2608.27809](https://arxiv.org/pdf/2608.27809). I did not verify this paper's metadata; treat as corroborating only.
- A theoretical argument that the number of top-k subsets a single-vector retriever can represent is bounded by embedding dimension, so set-selection queries eventually exceed what single-vector search can express — [Medium, data-science-collective](https://medium.com/data-science-collective/the-retrieval-mistake-most-rag-teams-havent-understood-8b864c034ee7). Secondary source, theoretical argument, not peer-reviewed; treat as a perspective.
- Practitioner-level confirmation of the same failure shape: a RAG app's "which" and "highest" chips "return incomplete or wrong answers because they ask about the whole portfolio," judged a retrieval limitation rather than a model problem — [GitHub issue](https://github.com/samww/product-portfolio-rag/issues/73). Anecdotal.

### Inferences

- The project's existing scorecard cannot detect this failure class at all. Hit@4 asks "did a relevant passage appear in the top 4" — a retrieval metric whose maximum is achieved by returning *one* good passage. An aggregation question's correctness is a property of the *complete* set, so Hit@4 is not merely a weak proxy, it is measuring a different quantity. TREC qrels assume graded relevance per document; aggregation gold answers are sets of entities with a numeric answer. Neither instrument can express "the sum is wrong."
- The AGGBench top-k sweep implies the obvious engineering reflex — raise k, rerank harder — is actively harmful for aggregates. The cross-encoder reranker the series has built optimises for precision at the top of a ranked list, which is orthogonal to set completeness.
- AGGBench's 1.09% evidence density is a reasonable analogue for a filings corpus: for "all holders of company Y," the relevant filings are a tiny, fixed-size subset of the corpus, and the subset size varies per query (one issuer may have 40 institutional holders, another 900). No single k can be right.

### Gaps

- AGGBench's query types are **not** decomposed into count / sum / top-k / argmax / group-by. The paper describes only "how many" count-style aggregations with AND/OR composition. **There is therefore no published measurement specific to top-K or argmax over a text corpus that I could find** — the closest is AGGBench's count setting and TAG-Bench's unscored aggregation column. This is a real hole directly over the project's question class.
- TAG-Bench's aggregation category is unscored (N/A for all five methods). Anyone citing "RAG scores 0 on aggregation" from TAG is over-reading Table 1: the 0.00 figures are overall/match/comparison/ranking.
- No paper I found measures whether a *complete-set* retrieval objective (as distinct from top-k) is achievable with dense retrieval at all, e.g. by thresholding similarity rather than fixing k.

---

## Key Question 3: Benchmarks that actually test it

### Takeaway

The text-to-SQL family (Spider/Spider 2.0/BIRD) tests aggregation inside SQL but assumes the table already exists and contains no prose; the hybrid family (HybridQA/OTT-QA/TAT-QA/MultiHiertt/FeTaQA) tests table+prose but with tiny per-question contexts, not corpus-wide aggregation; and only AGGBench and TAG-Bench test aggregation *over a collection*. Nothing tests all three properties at once at corpus scale, which is precisely the project's setting.

### Cited Findings

**Text-to-SQL family — aggregation yes, prose no, table given.**

| Benchmark | Date | Size / corpus | Aggregation? | Table+prose? | Metric |
|---|---|---|---|---|---|
| BIRD | May 2023 (NeurIPS 2023) | 12,751 text-to-SQL pairs, 95 databases, 33.4 GB, 37 domains | Yes, within SQL | No | Execution accuracy + Valid Efficiency Score (VES) |
| Spider 2.0 | Nov 2024 (ICLR 2025 Oral) | 632 enterprise workflow problems; DBs often >1,000 columns; BigQuery/Snowflake; multiple dialects | Yes, plus transformation pipelines | No | Execution accuracy |
| BookSQL | NAACL 2024 | 100k NL–SQL pairs; accounting DBs of 1M records | Yes (accounting domain) | No | **Exact match**, not execution accuracy |
| BULL | 2024 | Fund, stock and macro-economy DBs from Hundsun Technologies' financial analysis business | Yes | No | Not captured — see Gaps |

- BIRD figures and scope: 12,751 pairs, 95 databases, 33.4 GB total, 37 professional domains; VES introduced "to evaluate the efficiency of generated SQLs in addition to the standard execution accuracy." Li et al., *Can LLM Already Serve as A Database Interface?*, May 2023 — [arXiv:2305.03111](https://arxiv.org/abs/2305.03111v2); [NeurIPS 2023 poster](https://neurips.cc/virtual/2023/poster/73529)
- BIRD headline *execution accuracy*: the arXiv v1 abstract reports **ChatGPT 40.08%** against **human 92.96%**; the NeurIPS version reports **GPT-4 54.89%** vs the same human 92.96%. The two versions quote different top models — do not conflate them. [arXiv:2305.03111](https://arxiv.org/abs/2305.03111v2); [NeurIPS](https://neurips.cc/virtual/2023/poster/73529)
- Spider 2.0 *execution accuracy*: o1-preview in a code-agent framework solves **21.3%**, vs **91.2% on Spider 1.0** and **73.0% on BIRD** — same system, three benchmarks, directly comparable. [arXiv:2411.07763](https://arxiv.org/abs/2411.07763)
- BookSQL: "100k natural language queries-SQL pairs, and accounting databases of 1 million records"; its benchmark table reports **exact match**, and RESDSQL scored **10.8% on BookSQL vs 80.5% on Spider** — note this is EM, a *different* metric from execution accuracy, so the gap is not directly comparable to BIRD's EA numbers. NAACL 2024 — [arXiv:2406.07860](https://arxiv.org/abs/2406.07860v1)
- BULL/FinSQL: BULL is "collected from the practical financial analysis business of Hundsun Technologies Inc., including databases for fund, stock, and macro economy," and FinSQL "achieves the state-of-the-art Text-to-SQL performance at a small cost" on it. [arXiv:2406.07860 context / alphaXiv overview](https://alphaxiv.org/overview/2406.07860v1)
- Execution accuracy's definition, as generally stated: EA = (1/N) Σ I(V_i, V̂_i), the indicator being 1 when predicted and gold result sets match; equality is usually **set-based, ignoring row order unless sorting is specified** — [arXiv:2602.21480](https://arxiv.org/pdf/2602.21480). This matters for top-K questions: a benchmark that ignores row order would score a correctly-membered but mis-ranked top-5 as correct. I could not verify this paper's metadata; the formula is standard but confirm against the official harness before relying on it.
- Label-quality caveat on BIRD: a blog post reports **22 gold SQL queries are outright wrong** and that correcting them shifts model rankings — [beancount.io research log, 6 June 2026](https://beancount.io/bean-labs/research-logs/2026/06/06/bird-benchmark-text-to-sql-real-database-gap). Secondary source, unverified; flagged because it bears on any headline EA figure.

**Hybrid table-and-text family — prose yes, corpus-wide aggregation no.**

| Benchmark | Date | Size / corpus | Aggregation? | Table+prose? | Metric |
|---|---|---|---|---|---|
| HybridQA | April 2020 | ~70,000 QA pairs over ~13,000 Wikipedia tables; table and passages **given** per question | Multi-hop, limited arithmetic | Yes | EM / F1 |
| OTT-QA | Oct 2020 (ICLR 2021) | 41,469 train / 2,214 dev / 2,158 test (~45K); corpus **410,740 tables + 6,342,314 passages** | Multi-hop; "retrieving and aggregating information" | Yes, both must be retrieved | EM / F1 |
| TAT-QA | May 2021 (ACL 2021) | Financial reports; hybrid table+text contexts | **Yes — explicitly** | Yes | **Numeracy F1 + EM** |
| MultiHiertt | June 2022 | 10,440 QA pairs over 2,513 documents, ~3.89 tables/doc, from FinTabNet S&P 500 annual reports | Yes, multi-step numerical | Yes | **EM + numeracy F1 (DROP-style)** |
| FeTaQA | April 2021 (TACL 2022) | 10K Wikipedia table / question / free-form answer / supporting-cell tuples | Incidental | Table → free-form prose | BLEU (generation metric) |

- HybridQA *EM*: "the EM scores obtained by two baselines are below 20%, while the hybrid model can achieve an EM over 40%", with humans far ahead; an aggregator lists **43.8 EM** on test. [arXiv:2004.07347](https://ar5iv.labs.arxiv.org/html/2004.07347)
- OTT-QA splits and corpus: 41,469 / 2,214 / 2,158; **410,740 tables and 6,342,314 passages**; "45K human-annotated questions that require retrieving and aggregating information over tables and text from the whole Wikipedia." Sources round the total variously (45K to 45,814) — use the split numbers. *EM* on dev/test is roughly **10**. [arXiv:2010.10439](https://arxiv.org/pdf/2010.10439)
- TAT-QA, the most aggregation-centric: numerical reasoning is "usually required," listing addition, subtraction, multiplication, division, **counting, comparison/sorting**, and compositions. TAGOP scores **58.0% F1**, an 11.1-point absolute gain over the prior best; expert humans **90.8% F1**. Zhu, Lei, Huang, Wang, Zhang, Lv, Feng, Chua, submitted 17 May 2021, rev. 1 June 2021, ACL 2021 — [arXiv:2105.07624](https://arxiv.org/abs/2105.07624). A secondary source additionally gives TAGOP's EM as **50.1%** and human EM as **84.1%** — [beancount.io, 14 May 2026](https://beancount.io/bean-labs/research-logs/2026/05/14/tat-qa-hybrid-tabular-textual-financial-question-answering); the EM pair is **not** confirmed from the paper page, treat as unverified.
- MultiHiertt full statistics: 10,440 QA pairs with annotated reasoning programs and supporting facts; 2,513 documents averaging 3.89 tables; source FinTabNet S&P 500 annual reports plus 2,119 QA examples reused from FinQA; average input 1,645.9 words / 68.06 sentences per document; tables average 10.78 rows × 4.97 columns; splits 7,830 / 1,044 / 1,566; average 2.47 reasoning steps and 7.02 supporting facts per question. Zhao, Li, Chen, Wang, Cohan, Zhang, June 2022 — [arXiv:2206.01347](https://ar5iv.labs.arxiv.org/html/2206.01347)
- MultiHiertt *EM / numeracy F1*, Table 4 — the key point is how far below human these sit:

| System | Dev EM/F1 | Test EM/F1 |
|---|---|---|
| Longformer + Reasoning | 2.71 / 6.93 | 2.86 / 6.23 |
| Facts Retrieving + TAPAS | 8.94 / 10.70 | 7.67 / 10.04 |
| Facts Retrieving + NumNet | 10.32 / 12.59 | 10.77 / 12.02 |
| TAGOP (RoBERTa-large) | 19.16 / 21.08 | 17.81 / 19.35 |
| Facts Retrieving + Seq2Prog | 26.19 / 28.74 | 24.58 / 26.30 |
| FinQANet (RoBERTa-large) | 32.41 / 35.37 | 31.72 / 33.60 |
| MT2Net (RoBERTa-large) | 37.05 / 39.96 | **36.22 / 38.43** |
| Human expert | – | **83.12 / 87.03** |

  Human figures are two professionals on 60 sampled test questions. Source: [arXiv:2206.01347](https://ar5iv.labs.arxiv.org/html/2206.01347)
- MultiHiertt's *retrieval* metric, reported separately: supporting-fact recall **76.4% at top-10** and **80.8% at top-15**. Note the contrast — retrieval recall ~80% while answer-set EM is ~36%. [arXiv:2206.01347](https://ar5iv.labs.arxiv.org/html/2206.01347)
- MultiHiertt breakdown by type and depth (test, MT2Net RoBERTa-large, EM / F1) — performance falls as tables and steps multiply:
  - Text-only **49.26 / 53.29**; Table-only **36.77 / 38.55** (≥2 tables: **24.32 / 24.96**); Table-text **33.04 / 35.15** (≥2 tables: **21.04 / 23.36**)
  - 1 step **43.62 / 47.80**; 2 steps **34.67 / 37.91**; 3 steps **22.43 / 24.57**; >3 steps **15.14 / 17.19**
  - Source: [arXiv:2206.01347](https://ar5iv.labs.arxiv.org/html/2206.01347)
- FeTaQA: "10K Wikipedia-based table, question, free-form answer, supporting table cells pairs," answers being "human-generated explanations involving entities and their high-level relations"; the authors provide a semantic-parsing pipeline and an end-to-end generation baseline and show both are challenged. Nan et al., April 2021, TACL 2022 — [arXiv:2104.00369](https://arxiv.org/abs/2104.00369v1); [TACL](https://transacl.org/index.php/tacl/article/view/3133)

**Aggregation over a document collection — the only two that test it.**
- AGGBench: 45 research papers; **362 aggregation queries** (100 base single-condition, 262 composite: 42 AND, 220 OR; 228 two-condition, 34 three-condition); 4,755 chunks base / 16,294 expanded. Metrics are **chunk-level recall (retrieval)** and **ACE / NACE absolute and normalised count error (answer-set)** — notably **neither set F1 nor exact set match is reported**, and NACE can exceed 1. Also evaluates a SpiderQA variant (Table 5, Appendix A.6.1): DFA NACE mean 0.262, ACE 4.000, recall 0.891, vs best baseline recall 0.865 (LightRAG). [arXiv:2602.01355v2](https://arxiv.org/html/2602.01355v2)
- TAG-Bench: **80 queries** — 40 needing parametric/world knowledge, 40 needing semantic reasoning — across four BIRD query types at 20 each (match, comparison, ranking, aggregation), over five BIRD databases: california_schools, debit_card_specializing, formula_1, codebase_community, european_football_2. Metric is **exact match**; code public under the TAG-Research org (TAG-Bench). [arXiv:2408.14717](https://arxiv.org/html/2408.14717v1)
- TAG-Bench knowledge vs reasoning split (Table 2, EM / execution time): Text2SQL 0.20 (5.23s) / 0.10 (5.52s); RAG 0.00 / 0.00; Retrieval+LM Rank 0.03 / 0.00; Text2SQL+LM 0.10 / 0.20; Hand-written TAG 0.53 (3.50s) / 0.60 (2.24s). [arXiv:2408.14717](https://arxiv.org/html/2408.14717v1)
- Internal inconsistencies in the TAG paper, flagged: the experimental setup names Llama-3.1 **70B** while Appendix B says **80B**; and the abstract's "20–65% higher accuracy" range is loosely stated — the 65% is the *comparison-query accuracy* (0.65), not an overall improvement. [arXiv:2408.14717](https://arxiv.org/html/2408.14717v1)

### Inferences

- There is a clean split in what each family omits. Spider/BIRD give you a correct table and ask only whether the model can write SQL. The hybrid family gives you a per-question context of one or a few tables and asks for arithmetic. **Neither tests the project's actual task: build the table from a corpus, then aggregate over all of it.** AGGBench is the closest in shape (corpus-bounded, completeness-required) but tests counts, not top-K rankings, over 45 research papers rather than a large filings corpus.
- MultiHiertt's figure pair is the most instructive single data point for this project: **~80% supporting-fact retrieval recall coexisting with ~36% answer EM**. Good retrieval is nowhere near sufficient for a numeric answer. A project whose current scoreboard is Hit@4 should expect its retrieval numbers to carry over and its answer numbers to start near zero.
- The >3-steps row (15.14 EM) is the relevant difficulty estimate for "top 10 companies exposed to Y," which is a 4+ step computation (resolve Y → find holdings of Y → join to managers → sum per manager → rank → cut at 10).
- TAT-QA and MultiHiertt are both *financial*, both built on real reports, and both have published aggregation-heavy splits. If the project wants an off-the-shelf dev set for a finance aggregation pivot, these are the only two credible candidates — but both are single-document contexts, so they test the arithmetic, not the corpus-scale join.

### Gaps

- **No GROUP BY / ranking subset breakdown exists in public form for BIRD or Spider 2.0.** I searched specifically for it and found none; Spider 2.0's paper has an error analysis, but no source I reached classified failures by SQL clause. The common assertion that "aggregate queries are the weakest category" is therefore **not verifiable** from the sources I found, and should not be asserted in the report. The honest statement is: the benchmarks contain aggregation, and nobody I found publishes the per-clause split.
- BIRD's two reported top-model EA figures (40.08% ChatGPT in arXiv v1 vs 54.89% GPT-4 in the NeurIPS version) come from different paper versions. I did not resolve which is canonical for citation.
- BULL's execution accuracy figures were not obtained; only its description and FinSQL's "state-of-the-art" claim.
- BookSQL reports **exact match** not execution accuracy, so the "10.8% vs 80.5% Spider" gap cannot be lined up against BIRD/Spider EA figures.
- A benchmarklist.com page showed Spider 2.0 top scores "in the mid-90s percent", which contradicts the paper's 21.3%. I could not determine what that page is measuring (possibly a different subset or a different Spider version) and so **do not** report it as a figure — [benchmarklist.com](https://benchmarklist.com/benchmarks/spider_2_0/). Flagged as an unresolved conflict.
- FeTaQA's canonical BLEU is unresolved: an aggregator lists 40.5, 39.05 and ~0.33 for apparently the same dataset under different task labels — [wizwand](https://www.wizwand.com/dataset/fetaqa). Do not quote a FeTaQA BLEU without reading the paper's own table.
- I found no finance-specific text-to-SQL benchmark over *SEC filings* specifically. BookSQL is accounting ledgers; BULL is fund/stock/macro DBs from a vendor.

---

## Key Question 4: Which part is hard — query generation, entity resolution, or the extraction that built the table?

### Takeaway

Where papers decompose error, generation of the query is the *minority* of failures. MultiHiertt attributes ~64% of errors to getting the operands wrong (wrong span 43% + missing operand 21%) against only 19% wrong program; AGGBench's agentic method's largest identified failure is tool parameterisation (~15%); and the extraction literature finds structural table errors (missing/extra rows and columns, misaligned spanning cells, wrong header associations) persist even in the strongest 2026 models. For a project that must build the table from filings itself, the numbers go wrong before any query runs.

### Cited Findings

**Error decomposition in hybrid numerical QA — operands, not programs.**
- MultiHiertt error analysis over 100 sampled test errors: **wrong operand or span 43%**, **missing operand 21%**, **wrong program 19%**, **lack of domain knowledge 4%**. The paper attributes ~**64%** of errors (wrong operand/span + missing operand) to failing to integrate supporting facts correctly. It does **not** formally split retrieval vs program generation; the 19% wrong-program figure is the closest proxy for generation error. [arXiv:2206.01347](https://ar5iv.labs.arxiv.org/html/2206.01347), June 2022
- The same paper's depth gradient shows the compounding: 1 step 43.62 EM → >3 steps 15.14 EM; and ≥2 tables drops table-only from 36.77 to 24.32 EM. [arXiv:2206.01347](https://ar5iv.labs.arxiv.org/html/2206.01347)

**Error decomposition in agentic aggregation.**
- AGGBench Section 6.4.2 reports two DFA failure patterns: ~**15%** of errors from incorrect tool parameterisation and ~**5%** from redundant repeated tool calls. The paper **does not** break errors into disambiguation vs filtering vs aggregation vs extraction, despite those being its own three stages. Table 4 reports disambiguation *classification accuracy* (a different quantity from error attribution) and Table 6 compares clustering methods for aggregation (LLM calls and candidate pairs), not error sources. [arXiv:2602.01355v2](https://arxiv.org/html/2602.01355v2), February 2026
- That the authors made disambiguation a named, measured stage at all is itself evidence that entity resolution is treated as a first-class failure mode in corpus-scale aggregation, not an afterthought. [arXiv:2602.01355v2](https://arxiv.org/html/2602.01355v2)

**Extraction error — structure fails before content does.**
- A multimodal-LLM table-extraction benchmark reports **structural layout errors (missing or extra rows and columns) as a separate metric from content accuracy**, using GriTS F1, structural layout F1 and table shape accuracy alongside cell-content metrics. *Benchmarking Table Extraction: Multimodal LLMs vs Traditional OCR*, XLLM 2025 — [ACL Anthology](https://aclanthology.org/2025.xllm-1.2.pdf)
- Frontier models still make structural mistakes: "even the top-scoring Gemini 3 models exhibit errors upon manual inspection (misaligned spanning cells, altered values, incorrect header-cell associations)", and broken or ambiguous associations are counted as errors. *Beyond String Matching: Semantic Evaluation of PDF Table Extraction*, 2026 — [arXiv:2603.18652v2](https://arxiv.org/html/2603.18652v2)
- Structure degrades disproportionately under document distortion: "the structure-recognition accuracy was found to be significantly degraded due to document transformation," though correcting rotation alone recovers much of it. *Robustness of Structured Data Extraction from Perspectively Distorted Documents*, November 2025 — [arXiv:2511.17607](https://arxiv.org/pdf/2511.17607)
- Known structural triggers: tables "often exhibit diverse structures, such as merged cells and nested headers," which makes identifying row-column relationships challenging. *SciEx framework*, December 2025 — [arXiv:2512.10004](https://arxiv.org/pdf/2512.10004)
- Format matters enormously: a hybrid OCR-LLM study found table-based extraction achieves **perfect accuracy on spreadsheets**, while PDF results showed a speed/accuracy trade-off. October 2025 — [arXiv:2510.10138](https://arxiv.org/pdf/2510.10138)

**Entity resolution as the join-side failure.**
- The 13F literature makes the entity problem concrete rather than abstract: issuer names appear "as the filer typed them," so a name-based join is unreliable and CUSIP is the dependable key. See Q5 for the full chain. [pipeworx-io/mcp-sec-13f](https://github.com/pipeworx-io/mcp-sec-13f)
- The Backus–Conlon–Sinkinson case is an existence proof that extraction/ingestion error dominates in exactly this domain: they abandoned the standard cleaned vendor dataset and rebuilt from original filings after finding that from 2011 onward, "over 10% of S&P 500 firms have very few reported owners" in the Thomson Reuters series — a *data* error that would silently corrupt every top-K-holders answer. [Yale faculty data page, as summarised in search results](https://faculty.som.yale.edu/michaelsinkinson/common-ownership-data/)

### Inferences

- The decisive finding the brief asked for is supported, with a caveat about directness. No single paper says "extraction error dominates aggregation QA." But the available decompositions all point the same way: MultiHiertt's 64% operand-integration errors vs 19% program errors; 2026 table-extraction papers still finding altered values and wrong header associations in frontier models; and in the 13F domain specifically, a published research team discarding the industry-standard cleaned dataset because its *contents* were wrong. For a project that would build the holdings table itself from filings, the expected failure is a wrong number in a correctly-executed query — the worst possible failure mode, because it is confidently and precisely wrong, and no retrieval metric or execution-accuracy metric will catch it.
- This inverts the natural build order. Benchmarks like Spider and BIRD measure the *last* step and hand you a correct database; the project's risk is concentrated in the step those benchmarks assume away. A pilot should therefore spend its first effort on measuring extraction fidelity against a hand-audited sample of filings, with a table-structure metric (GriTS F1 / shape accuracy) rather than any QA metric.
- 13F's saving grace relative to the extraction literature: the SEC publishes structured TSVs (SUBMISSION / COVERPAGE / INFOTABLE) rather than only PDFs, which puts it closer to the "perfect accuracy on spreadsheets" end of the hybrid OCR-LLM finding than to the PDF end. The extraction risk shifts from OCR/structure to *semantic* reconciliation — amendments, duplicate lines, manager identity — which is where the 13F-specific gotchas in Q5 live.

### Gaps

- No paper I found runs the clean ablation the project would most want: build the table by extraction, measure table accuracy, then measure downstream aggregation accuracy, and report the share of answer error attributable to extraction vs query generation. **This appears genuinely unpublished.**
- AGGBench does not decompose error along its own three stages, so the ~15% + ~5% figures account for only about a fifth of its failures; the remaining ~80% is unattributed in the paper.
- MultiHiertt's error categories are annotated on model outputs over *given* contexts, so its 43%/21% operand errors are reasoning-time errors, **not** extraction errors from document to table. Transferring this to "extraction dominates" is my inference, not the paper's claim.
- I did not reach the Spider 2.0 error-analysis section, so I cannot say whether its 21.3% failures are schema-comprehension, dialect or logic errors.

---

## Key Question 5: The 13F case concretely

### Takeaway

There is no peer-reviewed system paper on 13F exposure QA, but there is one well-documented open-source implementation and one well-documented academic dataset rebuild, and between them the practical gotchas are fully enumerated: CUSIP is proprietary (the hard blocker, routed around via OpenFIGI), issuer names are as-typed, a manager can report the same security on multiple lines, amendments may either restate or add, and the form covers long US-listed equity only, filed up to 45 days late, above a $100M threshold, with confidential-treatment holdings silently absent.

### Cited Findings

**CUSIP licensing — the blocker, and the documented route around it.**
- 13F has no ticker column; positions are keyed by CUSIP, and "CUSIP identifiers are proprietary to CUSIP Global Services." For that reason the pipeworx 13F pack **does not build or ship a bulk CUSIP-to-ticker table**; tickers are resolved per call through OpenFIGI, described as "openly licensed Bloomberg FIGI", and the resolved name is matched against `issuer_name`, with the response reporting `matched_on` when it falls back to a name match. [pipeworx-io/mcp-sec-13f](https://github.com/pipeworx-io/mcp-sec-13f)
- CGS's own terms: a license "is also required when an end-user's database that contains CUSIP Data is updated, stored, maintained", whereas "End-users who obtain display-only access to CGS Data through an Authorized Distributor's on-line screen display do not have to enter into a license agreement." So *storing* a CUSIP-keyed table is the licensable act. FAQ dated 4 December 2015 — [CGS Website FAQ](https://www.cusip.com/pdf/CGS%20Website%20FAQ12.4.15%20(ACS)_seb.pdf)
- Small-volume waiver: CGS "waives the license fees for an end-user customer who receives access to CGS Data from an Authorized Distributor in machine readable or downloadable format" maintaining fewer than 500 securities containing CGS identifiers. [CGS FAQ, 2015](https://www.cusip.com/pdf/CGS%20Website%20FAQ12.4.15%20(ACS)_seb.pdf)
- Academic/research profile is a discretionary fee factor, not an exemption: complimentary CGS services "may be available with a CGS license", with fee decisions weighing factors including a customer's academic or research profile. [CGS FAQ, 2015](https://www.cusip.com/pdf/CGS%20Website%20FAQ12.4.15%20(ACS)_seb.pdf)
- Vendor-channel restriction is litigated: a class action alleges "CGS inserted a new clause in its agreements with Data Vendors prohibiting vendors from providing CUSIPs to institutions that did not sign a license agreement directly with S&P", with that license prohibiting uses not specifically allowed. Filed 2022 — [ASPPA](https://www.asppa-net.org/news/2022/3/class-action-suit-challenges-big-cusip-licensing-fees/); commentary at [Reason Foundation](https://reason.org/commentary/class-action-lawsuits-against-cusip-could-improve-government-transparency/)
- Counterweight from CGS's own regulatory comment: CGS does not charge for CUSIP use limited to regulatory reporting, and "CUSIP identifiers and related information also are available without license or fee from certain public sources." CGS comment to FDIC — [FDIC Federal Register publication](https://www.fdic.gov/federal-register-publications/cusip-global-services-scott-j-preiss-rin-3064-af96)
- Why CUSIP is needed at all: issuer names are inconsistent across filers, so CUSIP is the reliable identifier; the first six characters identify the issuer and the remainder the specific security plus a check digit — e.g. GOOGL and GOOG have different CUSIPs sharing the same six-character issuer prefix. A practitioner guide maps CUSIPs via the OpenFIGI API (up to 100 mapping jobs per request) and reports roughly **50,000 unique CUSIPs** across 13F filings for a six-month period in 2021. [Medium, Jan](https://medium.com/@jan_5421/analyzing-13f-sec-filings-and-buy-sell-activities-of-institutional-investment-managers-using-python-8bba3dfafd7d)

**What a working queryable holdings layer looks like — the one documented implementation.**
- Ingests the SEC Form 13F structured data set, a rolling three-month ZIP of seven TSVs, using three: SUBMISSION (accession→CIK), COVERPAGE (accession→manager), INFOTABLE (holdings). Refresh reads the index page for the newest release because window filenames don't follow calendar quarters. [pipeworx-io/mcp-sec-13f](https://github.com/pipeworx-io/mcp-sec-13f)
- The physical design detail that makes top-K-holders answerable: a `(cusip, value_usd DESC)` index turns manager-first data into a single-query answer to "which managers hold this security." [pipeworx-io/mcp-sec-13f](https://github.com/pipeworx-io/mcp-sec-13f)
- Duplicate-line handling, stated: "A single manager can report the same security on several lines" — different investment discretion, or puts and calls — and `who_owns` collapses these per manager before ranking, "so the output reflects holders rather than individual lines." [pipeworx-io/mcp-sec-13f](https://github.com/pipeworx-io/mcp-sec-13f)
- Query surface, which maps directly onto the author's three question forms: `who_owns` (managers holding a security, ranked by position size, with `limit` — i.e. top-K holders); `manager_portfolio` (one manager's positions, largest first — i.e. top-K exposures); `manager_changes` (NEW/EXITED/INCREASED/DECREASED between two quarters); `security_flows` (total value and holder count per quarter, plus buyers and sellers); `manager_profile` (top-5 and top-10 concentration, turnover, top positions — explicitly **no sector exposure, since 13F has no sector field**); `holdings_coverage`. [pipeworx-io/mcp-sec-13f](https://github.com/pipeworx-io/mcp-sec-13f)
- Stated limitations, verbatim where quoted: scope "Long US-listed equity only" — shorts, cash and bonds excluded by rule, as are non-US holdings; timing "Quarterly, and late", up to 45 days after period end; threshold — a manager below the $100M floor doesn't file, so "absence is not evidence"; identifiers — issuer names are "as the filer typed them." [pipeworx-io/mcp-sec-13f](https://github.com/pipeworx-io/mcp-sec-13f)

**Amendments and combination reports.**
- The rule: an amendment must either restate the entire report **or** include only holdings entries new relative to the current public report for the same period. The manager must check the amendment box on the Cover Page, enter the amendment number, and indicate whether the amendment is a restatement or adds new holdings entries; "Each amendment must include a complete Cover Page and, if applicable, a Summary Page and Information Table." Form 13F General Instructions — [reginfo.gov](https://www.reginfo.gov/public/do/DownloadDocument?objectID=81978801)
- This is the single most dangerous join semantics in the dataset: an "add-new-entries" amendment must be **unioned** with the original, while a "restatement" amendment must **replace** it. Getting this backwards double-counts or drops positions, and the discriminator is a cover-page flag, not the form type.
- Vendor practice confirms de-duplication is required and non-trivial: both versions appear in the data, and "consumers must de-duplicate by keeping only the most recent filing per manager-quarter." [sec-api.io Form 13F dataset card](https://sec-api.io/datasets/form-13f-holdings)
- Amendments can be *worse* than originals: Anderson and Brockman found "amended 13F reports are sometimes less accurate than the original filings." *Form 13F (Mis)Filings* — [SSRN abstract 2809128](https://www.ssrn.com/abstract=2809128)
- Academic practice is often to ignore amendments: one study uses only original filings, noting "in less than 2% of our sample, we include data from 13F amendments."

**Data-quality literature.**
- Nobody checks the filings: an SEC Inspector General review found "no SEC division or office monitors the Form 13F filings for accuracy and completeness," so errors surface only via confidential-treatment processing or outside reports. *Review of the SEC's Section 13(f) Reporting Requirements* — [SEC OIG report 480](https://www.sec.gov/files/480.pdf) (cited by URL only; not fetched, per constraint)
- Anderson and Brockman on bank holding company filings: "reported holdings do not always appear on the SEC's Official List", "market prices of Official List securities are often inaccurate", and amendments are sometimes less accurate than originals. [SSRN 2809128](https://www.ssrn.com/abstract=2809128)
- Confidential treatment creates silent absence: Aragon, Hertzel and Shi report confidential-treatment holdings differ statistically from public positions in size, ownership share and returns, over a 1999–2013 sample — [ResearchGate](https://www.researchgate.net/publication/228963032_Why_Do_Hedge_Funds_Avoid_Disclosure_Evidence_from_Confidential_13F_Filings). Commentary notes "there is no placeholder, no indication that data is missing", so a filer's reported total can be understated.
- Identifier drift over time: when a fund changes structure, ticker or CUSIP, filers don't always update consistently, "creating phantom positions and broken time series." [Dakota, *The 10 Biggest Problems With 13F Filing Data*](https://www.dakota.com/reports-blog/the-10-biggest-problems-with-13f-filing-data-and-why-almost-no-one-has-solved-them). Practitioner source.
- The cleaned-vendor-data cautionary tale: Backus, Conlon and Sinkinson "found issues with the Thomson Reuters (TR) 'S34' dataset used by many researchers and frequently accessed via Wharton Research Data Services (WRDS)" and built their own dataset from the original SEC 13(f) filings. WRDS had already fixed a lack of BlackRock holdings records, but "even with the updated dataset posted in the summer of 2018, we discovered a number of discrepancies." Their appendix shows some S&P 500 firms with implausibly few owners, and "a series of quarters beginning in 2011 where over 10% of S&P 500 firms have very few reported owners", while their scraped data look consistent. [Yale common-ownership data page](https://faculty.som.yale.edu/michaelsinkinson/common-ownership-data/) — **note: this URL returned HTTP 410 Gone on 2026-10-29**; the quotes above come from search-result extracts, not a successful fetch. See Gaps.
- Raw filings are hard for a stated reason: 13F filings "are not formatted uniformly and they have reporting errors and other typos." [Alpha Architect, 22 July 2016](https://alphaarchitect.com/beware-of-institutional-ownership-data-from-13f-filings/)
- Related dispute showing how ownership aggregates get contested: BlackRock argues a 13F-based common-ownership study filled gaps during airline bankruptcies with stale holdings, overstating ownership in about half the study quarters. January 2019 — [BlackRock policy spotlight](https://BlackRock.com/corporate/literature/whitepaper/policy-spotlight-common-ownership-data-is-incorrect-january-2019.pdf). One-sided view of an interested party.
- Reporting delay has its own literature: *Why Do Institutions Delay Reporting Their Shareholdings? Evidence from Form 13F* — [Rodney White Center working paper 13-15, Wharton](https://rodneywhitecenter.wharton.upenn.edu/wp-content/uploads/2014/04/13-15.musto_.pdf)

**Adjacent prior art on SEC-filings graphs.**
- A DeepLearning.AI lesson adds a second SEC dataset of institutional manager holdings to a 10-K-derived graph specifically so users can ask market-dynamics questions — the most direct tutorial for the ownership angle, page ~960 days old as of the search. [DeepLearning.AI, *Knowledge Graphs for RAG*, lesson 7](https://learn.deeplearning.ai/courses/knowledge-graphs-rag/lesson/7/expanding-the-sec-knowledge-graph)
- A Neo4j GraphRAG example repo combines 10-K filings with Form 13 institutional investment data; described as work-in-progress. [neo4j-examples/sec-edgar-notebooks](https://github.com/neo4j-examples/sec-edgar-notebooks)
- `hopgraph` does multi-hop QA over SEC filings with Neo4j + pgvector + Claude and validates each answer's citations against what was retrieved; its example chains NVIDIA → supplier → director → customer board. **Its benchmark is listed as pending, so it reports no accuracy figures.** [sharmaakhil-work/hopgraph](https://github.com/sharmaakhil-work/hopgraph)
- Scope reminder from multiple practitioner sources: 13F covers only long US-listed equity positions filed up to 45 days after quarter end, so any exposure analysis is a lagged, partial snapshot. [geminiq](https://www.geminiq.com/blog/institutional-ownership-13f-filing-explained)

### Inferences

- "Exposure" is not a 13F field, and this is the project's biggest definitional trap. 13F gives *manager → security → value/shares*. "Companies a company is exposed to" requires a chosen semantics — common ownership? supply chain? shared holders? — none of which 13F supplies directly. The `manager_profile` tool's explicit note that there is no sector exposure "since 13F has no sector field" is the same wall in miniature. The author will have to define exposure as a derived metric, which is exactly the job of a semantic/metric layer (Pattern D), and exactly the kind of definition a text-to-SQL model should not be allowed to invent per query.
- The CUSIP position looks workable but needs care. Storing a CUSIP-keyed INFOTABLE is storing CUSIP Data, which per the 2015 FAQ triggers the license requirement; the documented mitigation is to never materialise a CUSIP→ticker mapping and to resolve identifiers per call via FIGI. A blog series that publishes a corpus or a demo dataset should treat the CUSIP column as the one field it cannot redistribute.
- 13D/13G are a different shape from 13F and the author's framing may conflate them. 13F is a quarterly position table (the thing you aggregate); 13D/13G are event-driven >5% beneficial-ownership filings that are substantially narrative. That split is actually convenient: 13F gives the structured layer, 13D/13G give the prose the explanation cites.
- Manager identity across CIKs is the most under-documented gotcha. The one implementation describes only accession→CIK and accession→manager joins and does not explain consolidating one manager across multiple CIKs — yet "top 10 companies exposed to Y" ranked by *manager* is wrong if a single asset manager files under several CIKs or via combination reports. This is an unsolved problem in the available prior art, not a solved one.

### Gaps

- **No peer-reviewed or published system paper builds a queryable 13F holdings table and evaluates exposure/ownership QA over it.** I found prior art (pipeworx MCP pack, Neo4j example, DeepLearning.AI lesson, hopgraph) but no accuracy results for any ownership-exposure QA system. hopgraph's benchmark is explicitly pending.
- The Yale common-ownership data page returned **HTTP 410 Gone** when fetched on 2026-10-29. The Backus–Conlon–Sinkinson quotes above are from search-result extracts and should be re-verified from the paper or a live mirror before the report quotes them. I also did not obtain their dataset's coverage figures, quarter range, manager-identity methodology, amendment handling, or license terms.
- **Combination reports are not addressed in any source I found.** The pipeworx documentation does not cover them; the General Instructions cover amendments but I did not obtain their combination-report provisions (and did not fetch sec.gov per the constraint). This is a live unknown — a 13F combination report means one filer reports holdings also reported by another, and naive summation across filers double-counts.
- Amendment handling in the one implementation is unclear: "amendment dedupe" is mentioned in the context of a server-side aggregation function, but the documentation does not describe how 13F-HR/A restatement-vs-new-entries is distinguished.
- Confidential treatment is **not mentioned** in the pipeworx limitations list, despite being a documented source of silent under-reporting.
- The under-reporting of **short positions and derivatives** is confirmed only at the level of the form's scope ("Long US-listed equity only"; shorts, cash and bonds excluded by rule). I found no study *measuring* the magnitude of what this omits — e.g. what fraction of true economic exposure 13F misses. The Agarwal–Jiang–Tang–Yang line of work on hidden holdings is adjacent but I did not locate and verify it.
- The CGS FAQ is dated December 2015. I found **no CGS policy specific to academic research current as of October 2026**, and no resolution of the 2022 class action. Any licensing conclusion should be stated as "as of the 2015 FAQ" with a recommendation to confirm directly.
- I did not verify whether the SEC's own structured 13F data sets include CUSIP under terms that differ from CGS's general license.

---

## Key Question 6: Combining the computed aggregate with the prose — citation, grounding, and faithfulness

### Takeaway

This is the weakest-evidenced part of the whole question set. There is documented practice for *verifying* numeric claims against a source table by executing code, and there are metrics for entity-level and structural faithfulness in table-to-text, but **I found no evaluation of faithfulness for an aggregate answer as distinct from a passage-grounded answer, and no documented convention for whether such an answer cites the rows, the filings, or the summary.** Flag this as unevaluated.

### Cited Findings

**Executable verification — the one method that actually fits an aggregate.**
- A 2026 paper translates each generated inference into a Python verification function "that is executed against the source table to address faithfulness", drawing on program-aided reasoning which hands numerical operations to code. *From Tables to Quantified Statements: Evaluating LLM Inference Generation through Executable Verification* — [arXiv:2609.23966](https://arxiv.org/pdf/2609.23966). I did not verify this paper's metadata beyond the search extract.
- This is the closest documented practice to grounding a sum or a ranking: recompute it, don't score its wording.

**What goes wrong in table-grounded generation.**
- TabFaith (SURGeLLM 2026) reports LLM table summaries hallucinate numerical values, misattribute entities to rows or columns, **fabricate comparative rankings**, and conflate temporal references. The ranking-fabrication finding is directly on point for top-K answers.
- The same work finds BLEU, PARENT and BERTScore correlate poorly with structural faithfulness, **r ≤ 0.60**, because they ignore table schema and cell structure. So the standard generation metrics cannot score an aggregate answer's fidelity.
- Entity-centric metrics: Liu et al. measure table-record coverage and the ratio of hallucinated entities, reporting strong agreement with human judgements. *Towards Faithfulness in Open Domain Table-to-text Generation from an Entity-centric View*, AAAI 2021, arXiv February 2021 — [arXiv:2102.08585](https://arxiv.org/pdf/2102.08585); [AAAI](https://ojs.aaai.org/index.php/AAAI/article/view/17583). These target entity *mentions*, so a wrong number or a wrong ordering that names no new entity can pass — that limitation is my inference, not the paper's claim.
- QA-based and token-level alternatives: Data-QuestEval generates questions from the text and answers them against the table, comparing answers; Kasner et al. (2021) detect incorrect values and statements at token level.
- General background on the hallucination taxonomy: *Survey of Hallucination in Natural Language Generation*, February 2022 — [arXiv:2202.03629](https://arxiv.org/pdf/2202.03629)

**Citation validation in practice (rather than in evaluation).**
- The one SEC-domain implementation that addresses citation does it by *validation against retrieval*: hopgraph pairs a Neo4j graph with vector search and "validated citations", checking each answer's citations against what was retrieved. No accuracy figures; benchmark pending. [sharmaakhil-work/hopgraph](https://github.com/sharmaakhil-work/hopgraph)
- Governance practice from the semantic-layer world, which is the nearest thing to a convention: constrain the AI to approved tables and joins, match warehouse and BI permissions for AI queries, and **log every prompt, query and result**. [Querio](https://querio.ai/articles/deliver-trusted-ai-powered-self-service-analytics). Vendor source. The logged query is effectively the citation — provenance as the executed SQL, not as a document reference.
- Provenance-by-field in the one 13F implementation: `who_owns` reports `matched_on` when a ticker resolution falls back to a name match, surfacing identifier-resolution uncertainty in the response rather than hiding it. [pipeworx-io/mcp-sec-13f](https://github.com/pipeworx-io/mcp-sec-13f)
- Benchmark design note that bears on citation: CypherBench records query execution *results*, which makes the executed query plus its result set the verifiable artefact. [arXiv:2412.18702](https://arxiv.org/pdf/2412.18702)

### Inferences

- There are three candidate citation targets and the literature implies a different one for each half of the answer. For the *ranking*, the honest citation is the executed query plus the row set (the semantic-layer/CypherBench position — provenance is the query, reproducibility is the test). For the *explanation prose*, it is the ordinary passage citation the project already does. Conflating them — citing a filing as support for a computed number — would be the specific failure mode TabFaith describes as fabricated comparative rankings dressed in real references.
- Faithfulness for an aggregate is categorically a *recomputation* problem, not a *textual entailment* problem, which is why the executable-verification approach is the only one that fits and why the r ≤ 0.60 finding for BLEU/PARENT/BERTScore matters: the project cannot reuse generation metrics here.
- A defensible evaluation design the project could adopt, assembled from these sources rather than taken from any one: exact set match and set F1 on the returned top-K (answer-set metrics, as AGGBench notably *fails* to report), Kendall's tau or similar on the ordering, recomputation checks per numeric claim in the prose (executable verification), and the existing Hit@4/qrels machinery retained unchanged for the explanation passages only. The clean separation of the three metric families is the point.

### Gaps

- **No evaluation of faithfulness for aggregate answers as distinct from passage-grounded answers was found.** The search returned no direct evaluation of aggregate-answer hallucination, e.g. checking a SUM or AVG result against the source. This is the clearest unevaluated area in the brief, and should be reported as such.
- **No documented convention** for whether an aggregate answer cites rows, source filings, or a generated summary. I found implementation choices (hopgraph's citation validation, semantic-layer query logging) but no standard, guideline or study comparing them.
- TabFaith appeared only in search extracts: I could not confirm its venue URL, authors, date beyond "SURGeLLM 2026", or the exact table containing the r ≤ 0.60 correlation figure. Treat the numerical claim as unverified.
- The executable-verification paper ([arXiv:2609.23966](https://arxiv.org/pdf/2609.23966)) was not fetched; title, authors and date are from the search extract only, and I have no figures from it.
- Data-QuestEval and Kasner et al. (2021) are cited here second-hand via the 2026 paper's related work, not from their own sources.
- No source evaluates faithfulness of a *hybrid* answer where the number comes from a database and the explanation from a passage — which is precisely the project's architecture.

---

## Key Question 7: "As of" aggregation — bitemporal, slowly-changing dimensions, restatement

### Takeaway

The warehousing and temporal-database literature has mature, standardised machinery for exactly this (ISO SQL:2011 bitemporal tables, SCD patterns, restatement views), and it is explicit that aggregating against dimensions that change over time is misleading unless the changes are modelled. What does **not** exist in anything I found is a treatment of bitemporal *as-of* aggregation in an LLM/RAG setting — no benchmark, no evaluation, no documented practice for asking a model for an as-of ranking.

### Cited Findings

- The three query modes, named: valid-time, transaction-time, and the combined mode, which answers "what the system believed at a given point about a given effective date" — illustrated with asking what was claimed about a policy's effective date, as known on a specific later day. [geosurge.ai, *Temporal Databases and Bitemporal Modeling for Audit-Ready Enterprise Knowledge Bases*](https://geosurge.ai/corpus/gen-4152/database-theory/temporal-databases-and-bitemporal-modeling-for-audit-ready-enterprise-knowledge-bases.html). Low-authority source; the valid/transaction-time distinction is standard and well-established independently.
- Standardisation and vendor support: bitemporal tables are part of the **ISO SQL:2011** standard and are supported by IBM, Oracle and Teradata. The same article argues SCD Type 2 "doesn't capture complete history" and proposes bitemporal dimensions instead. [TDAN, *The Case Against Slowly-Changing Dimensions, Part One*](https://tdan.com/?p=18505)
- The aggregation-specific warning, from the academic SCD literature: aggregating measures against dynamic dimensions can mislead unless the dimension changes are taken into account; the paper proposes a fourth response beyond Kimball's three classic SCD types. *Dimensional modeling by using a new response to slowly changing dimensions*, Aalborg University — [VBN](https://vbn.aau.dk/da/publications/dimensional-modeling-by-using-a-new-response-to-slowly-changing-d)
- The restatement implementation pattern: join the dimension table to itself to build a non-persistent restatement view, then join that view to the fact table — the standard way to report history with updated attribute values. [Justia patent 20110264618](https://patents.justia.com/patent/20110264618)
- Reference text on the pitfalls of aggregating dimensions: *Mastering Data Warehouse Design*, §8.2 "Aggregating Dimensions" — [O'Reilly](https://www.oreilly.com/library/view/mastering-data-warehouse/9780471777090/9780471777090_aggregating_dimensions.html)
- Product-level implementations exist: AtScale's SCD support uses natural keys with non-equi joins on transaction dates and effective dimension dates (public preview) — [AtScale docs](https://documentation.atscale.com/container/creating-and-sharing-cubes/creating-cubes/modeling-cube-dimensions/edit-dimensions-in-a-cube/scd); and pg_trickle describes a temporal stream table in PostgreSQL answering `AS OF TIMESTAMP` queries against historical row versions — [pgxn.org roadmap](https://api.pgxn.org/src/pg_trickle/pg_trickle-0.34.0/roadmap/v0.36.0.md)
- Why this is not optional in the 13F domain: amendments may **restate** the whole report for a past period, so a holdings fact for quarter Q can change after Q — a transaction-time change to a valid-time fact, which is the textbook bitemporal case. [Form 13F General Instructions, reginfo.gov](https://www.reginfo.gov/public/do/DownloadDocument?objectID=81978801)
- And the delay makes the two clocks diverge by design: filings arrive up to **45 days** after period end, so "as at date D" and "as filed by date D" are different questions with different answers. [pipeworx-io/mcp-sec-13f](https://github.com/pipeworx-io/mcp-sec-13f); [geminiq](https://www.geminiq.com/blog/institutional-ownership-13f-filing-explained)
- Restatement is not merely theoretical in 13F: amendments "are sometimes less accurate than the original filings" [SSRN 2809128](https://www.ssrn.com/abstract=2809128), and vendor data contains both versions requiring de-duplication to the latest filing per manager-quarter [sec-api.io](https://sec-api.io/datasets/form-13f-holdings).

### Inferences

- The project's bitemporal corpus is a genuine asset here and maps onto a solved problem with a standard name. "Top 10 holders of Y as at 2019-Q3" has two correct answers depending on the clock chosen: the best-known-now answer (valid time Q3, transaction time = latest) and the as-reported-then answer (valid time Q3, transaction time ≤ D). A bitemporal holdings table answers both; a single-version table answers neither reliably.
- The 45-day lag plus restating amendments means a naive "latest filing wins" de-duplication silently converts an as-of question into a best-known-now question. That is a correctness bug that no retrieval metric and no execution-accuracy metric would catch, because the SQL executes correctly against the wrong row set — the same confidently-precise-but-wrong failure shape as the extraction errors in Q4.
- This is also the sharpest *differentiator* for a blog series that already has a bitemporal corpus: as-of aggregation is a question class the series could pose and answer that the mainstream text-to-SQL and RAG literature does not address at all. The gap below is an opportunity, not just a hole.

### Gaps

- **I found nothing on bitemporal or as-of aggregation in an LLM, text-to-SQL or RAG setting.** No benchmark includes as-of aggregation questions; no paper evaluates whether a model generates temporally correct SQL against a bitemporal schema; no documented practice exists for expressing "as known at date D" in a natural-language analytics interface. Multiple searches did not surface one.
- The sources for the temporal machinery are thinner than for the other questions: a patent, a low-authority article, a vendor doc, a PostgreSQL extension roadmap and one academic paper. The underlying concepts (ISO SQL:2011 temporal tables, Kimball SCD types) are uncontroversial and well-established, but I did not obtain a canonical academic survey of bitemporal aggregation, and no source gave a concrete SQL pattern for aggregating under bitemporal as-of restatement.
- I did not find whether any of Spider 2.0, BIRD or the Cypher benchmarks include temporal-table or `AS OF` syntax in their gold queries, which would be the natural place to check whether models can even express this.
- No measurement of how often 13F amendments materially change an aggregate — i.e. how much the as-reported-then and best-known-now rankings actually differ. The <2%-of-sample figure from one academic study is about how often amendments were *used*, not about their effect on a ranking.
