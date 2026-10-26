# Structured query rewriting and question-structuring before retrieval

Scope note for the report writer: the single most decision-relevant finding below is that the
two best-measured "strong baseline" results in this literature (Weller et al. EACL 2024; Li et al.
SIGIR 2024) both find that generative query expansion **hurts** strong rankers, and that the
most popular techniques (HyDE, query2doc) were never evaluated with a cross-encoder reranker on
top. Everything else is secondary to that.

## Q1: What are the distinct families, what do they output, and what do they gain?

### Takeaway
Seven reasonably distinct families exist, but their measured gains are almost all against
unsupervised or sparse first-stage baselines (BM25, Contriever), not against a tuned hybrid-plus-
reranker stack. The gains are real but concentrated where the baseline is weak: HyDE gains
+10.7 nDCG@10 over BM25 on TREC DL19 and query2doc gains +15.0, while the same expansions
*subtract* 0.10–0.22 nDCG@10 when placed in front of a MonoT5/RankT5 cross-encoder.

### Cited Findings

**HyDE / hypothetical-document generation** (output: a generated fake answer passage, embedded and used as the query vector)
- HyDE prompts an instruction-following LLM to write a hypothetical document, then embeds it with an unsupervised contrastive encoder (Contriever) to retrieve real documents — [Gao et al., arXiv 2212.10496](https://arxiv.org/abs/2212.10496)
- TREC DL19 nDCG@10: BM25 50.6, Contriever 44.5, **HyDE 61.3**; DL20: BM25 48.0, Contriever 42.1, **HyDE 57.9**. Recall@1k DL19: BM25 75.0, HyDE 88.0 — [Gao et al., arXiv 2212.10496](https://arxiv.org/html/2212.10496v1)
- But fine-tuned retrievers already match or beat it on nDCG@10: DL19 ANCE 64.5 and DPR 62.2 vs HyDE 61.3; DL20 DPR 65.3 and ANCE 64.6 vs HyDE 57.9 — [Gao et al., arXiv 2212.10496](https://arxiv.org/html/2212.10496v1)
- BEIR nDCG@10, HyDE vs BM25: SciFact 69.1/67.9, ArguAna 46.6/39.7, TREC-COVID 59.3/**59.5** (BM25 wins), FiQA 27.3/23.6, DBPedia 36.8/31.8, TREC-NEWS 44.0/39.5. Contriever-FT (fine-tuned) beats HyDE on FiQA (32.9) and DBPedia (41.3) — [Gao et al., arXiv 2212.10496](https://arxiv.org/html/2212.10496v1)
- The paper evaluates no reranker and reports no cost or latency analysis — [Gao et al., arXiv 2212.10496](https://arxiv.org/html/2212.10496v1)

**query2doc / query expansion with generated terms** (output: original query concatenated with a generated pseudo-document; original query repeated to keep its term weight)
- MS MARCO dev MRR@10: BM25 18.4 → BM25+query2doc 21.4. TREC DL19 nDCG@10: 51.2 → 66.2; DL20: 47.7 → 62.9. Classical RM3 expansion *lowers* MS MARCO MRR@10 to 15.8 — [Wang et al., EMNLP 2023 / arXiv 2303.07678](https://ar5iv.labs.arxiv.org/html/2303.07678)
- Gains shrink sharply as the retriever strengthens: DPR 33.7 → 35.1 MRR@10 (+1.4), SimLM 41.1 → 41.5 (+0.4), E5-base+KD 40.7 → 41.5 (+0.8) — [Wang et al., arXiv 2303.07678](https://ar5iv.labs.arxiv.org/html/2303.07678)
- Authors' own summary: "boosts the performance of BM25 by 3% to 15%" on MS-MARCO and TREC DL — [Wang et al., arXiv 2303.07678](https://arxiv.org/pdf/2303.07678)
- The paper reports **no** cross-encoder reranker results; a cross-encoder appears only as a distillation teacher, and the authors note gains "tend to diminish" in that setting — [Wang et al., arXiv 2303.07678](https://ar5iv.labs.arxiv.org/html/2303.07678)

**Query rewriting / reformulation (free-form)** (output: a replacement query string)
- Rewrite-Retrieve-Read: an LLM writes a search query, a web search engine retrieves, a frozen reader answers; a trainable small-LM rewriter is tuned by RL on the frozen reader's feedback. Evaluated on open-domain QA and multiple-choice QA — [Ma et al., EMNLP 2023 / arXiv 2305.14283](https://aclanthology.org/2023.emnlp-main.322)

**Conversational query rewriting** (output: a de-contextualised, self-contained utterance)
- TREC CAsT 2019 overview: manual (human) rewrites beat the best automatic system by roughly **35% relative** — [Vakulenko et al. 2021](https://irlab.science.uva.nl/wp-content/papercite-data/pdf/vakulenko-2021-comparison.pdf)
- Vakulenko et al. compare rewriting methods on CAsT 2019 and 2020 under one shared retrieval pipeline, and find combining methods of different types reaches SOTA on both — [Vakulenko et al. 2021](https://irlab.science.uva.nl/wp-content/papercite-data/pdf/vakulenko-2021-comparison.pdf)
- QuReTeC on CAsT 2020 beats both the automatic median run and the organizers' supplied rewrites, but the authors' manual-rewrite run still beats their best automatic run. Error analysis found automatic rewrites sometimes *helped* by adding history terms the human rewrites omitted — [Voskarides et al., arXiv 2102.08795](https://arxiv.org/pdf/2102.08795)
- CMQR (multi-query rewriting) reports surpassing the manual-rewrite oracle on QReCC overall, while manual rewrites remain strong on the test-only TREC-CAsT set — [arXiv 2406.18960](https://arxiv.org/pdf/2406.18960)

**Decomposition into sub-questions** (output: N sub-queries, each retrieved separately)
- RQ-RAG trains a 7B Llama2 to choose among rewriting, decomposition, disambiguation, or skipping retrieval, via special control tokens; ~40k training instances built by having ChatGPT produce refined queries and regenerating answers against actual retrieval results. Reported gain: **+1.9% average over previous SOTA across three single-hop QA datasets**, plus improvement on multi-hop QA — [Chan et al., arXiv 2404.00610](https://arxiv.org/html/2404.00610v1)
- DMQR-RAG notes training-based methods like RQ-RAG "require substantial costs for dataset construction and training" — [arXiv 2411.13154](https://arxiv.org/html/2411.13154v1)

**Semantic parsing to a formal query** (output: executable SQL / Cypher / SPARQL)
- BIRD text-to-SQL: human execution accuracy 92.96%; GPT-4 in the original paper reached 54.89% — [BIRD, NeurIPS 2023](https://nips.cc/virtual/2023/poster/73529)
- Current best reported single-model BIRD score is Gemini-SQL2 at 80.04%, a 12.92-point gap to the human figure; no technical report or API published, so unverifiable — [aiweekly.co](https://aiweekly.co/alerts/googles-gemini-sql2-tops-bird-text-to-sql-at-8004). A March 2026 preprint states SOTA still trails human data engineers by about 11 points — [ReViSQL, arXiv 2603.20004](https://arxiv.org/pdf/2603.20004v1.pdf)
- CypherBench: LLMs reach up to **61.58% execution accuracy** on its harder retrieval tasks over full-scale knowledge graphs; the authors note existing KBQA benchmarks rely on text-to-SPARQL over RDF whose very large schemas make zero-shot evaluation hard — [Feng et al., arXiv 2412.18702](https://arxiv.org/pdf/2412.18702)
- Text-to-Cypher failure distribution for a local Qwen3.5-9B: **answer mismatch 52%** of incorrect outputs, execution failure 33%, schema-invalid query 10%, parse error 5% — i.e. the dominant failure is a query that runs cleanly and returns the wrong rows — [PIPE-Cypher, arXiv 2606.08481](https://arxiv.org/pdf/2606.08481)
- CyDRA proposes an 11-category Cypher failure taxonomy and reports +8.92 points average execution accuracy over a zero-shot baseline on CypherBench across five LLMs — [ICML 2026](https://icml.cc/virtual/2026/78014)
- A Gothenburg thesis found a Text2CypherRetriever over a T-REx property graph lost about **11 accuracy points and about 31 points of paraphrase consistency** vs text-based baselines — [gupea.ub.gu.se](https://gupea.ub.gu.se/handle/2077/89828)
- SPARQL QA evaluation is fragmented across BLEU, execution accuracy and graph isomorphism, which makes cross-paper comparison unreliable — [ELMKE 2026, CEUR-WS Vol-4246](https://ceur-ws.org/Vol-4246/elmke-2.pdf)

**Entity linking as a retrieval step** (output: a KB entity id for each detected mention)
- ELQ is a bi-encoder doing mention detection and linking in one pass. On WebQSP and GraphQuestions with extended multi-entity annotations it beats prior SOTA by **12.7% and 19.6% F1** respectively, and the authors report it significantly improves downstream QA performance of GraphRetriever — [Li et al., EMNLP 2020](https://aclanthology.org/2020.emnlp-main.522)
- ELQ is reported as about **2x faster end-to-end** than neural baselines, which the follow-up work cites as what makes it viable as a pipeline step — [Information Retrieval with Entity Linking, arXiv 2404.08678](https://arxiv.org/pdf/2404.08678)

### Inferences
- The families split cleanly into two groups by what they do to the query vector: *additive* (HyDE, query2doc, expansion — they lengthen and blur the query) and *structural* (semantic parsing, entity linking, slot filling — they produce a side-channel constraint rather than a changed text query). Only the additive group is what the strong-baseline negative results measure, so the structural group's risk profile is different and separately evidenced.
- The pattern across HyDE and query2doc is monotone in baseline strength: the gain is roughly 11–15 nDCG@10 points over BM25, 1–3 over a tuned dense retriever, and negative over a cross-encoder. For a pipeline that already fuses BM25 and dense and reranks, extrapolating from the BM25 number is the central mistake available to make here.

### Gaps
- **Slot filling / structured intent extraction as a retrieval step** — I found no paper measuring slot extraction specifically as a pre-retrieval step with named retrieval baselines. The literature on slot filling is dialogue-state-tracking literature, measured on joint goal accuracy, not on retrieval metrics. Report this as unevidenced rather than as a measured technique.
- Balog's *Entity-Oriented Search* book was in the suggested sources; I was not able to retrieve quotable measured figures from it within budget.

## Q2: Which have results against a strong baseline (BM25 + dense fusion + cross-encoder)?

### Takeaway
Essentially none of the popular techniques were evaluated that way by their own authors, and the
two studies that *did* put generative expansion in front of strong cross-encoder rankers both
found large regressions. This is the clearest finding in the whole review and it points the
opposite way from the popular framing.

### Cited Findings
- Weller et al. tested **11 expansion techniques across 12 datasets and 24 retrieval models**. Headline: expansion helps weak retrievers and tends to hurt strong ones. Error analysis attributes it to added text creating noise that blurs the ranking of top documents. Practical advice: use expansions for weaker models, or when the target data differs in format from the training corpus, and otherwise skip them — [Weller et al., EACL 2024 Findings / arXiv 2309.08541](https://arxiv.org/abs/2309.08541)
- Li et al. put expansions in front of MonoT5 and RankT5 cross-encoders. nDCG@10 (DL19 / DL20 / BEIR / Wiki+News):
  - RankT5 no expansion **0.737 / 0.736 / 0.508 / 0.557**
  - RankT5 + query2doc 0.572 / 0.637 / 0.437 / 0.502 — a drop of **0.165 on DL19**, 0.099 on DL20, ~0.07 on BEIR
  - RankT5 + RM3 0.681 / 0.695 / 0.487 / 0.539
  - RankT5 + query2keyword 0.690 / 0.688 / 0.493 / 0.556
  - MonoT5 no expansion **0.695 / 0.720 / 0.470 / 0.519**
  - MonoT5 + query2doc 0.473 / 0.613 / 0.392 / 0.459 — a drop of **0.222 on DL19**
  — [Li et al., SIGIR 2024 / arXiv 2311.09175](https://arxiv.org/html/2311.09175v2)
- Li et al.'s recovery method (Q2D2K: generate keywords via a reasoning chain, then fuse rankings across expanded queries) claws back only a small net gain over no expansion at all: RankT5 +0.014 DL19, +0.016 DL20, **+0.006 BEIR**, +0.013 Wiki+News; MonoT5 +0.029 / +0.010 / +0.006 / +0.013 — [Li et al., arXiv 2311.09175](https://arxiv.org/html/2311.09175v2)
- Li et al. report no HyDE numbers, stating only that long generated passages "drift too much from the distribution of queries" — the HyDE-with-reranker combination is unquantified in that paper — [Li et al., arXiv 2311.09175](https://arxiv.org/html/2311.09175v2)
- Cost of Li et al.'s recovery procedure: generate 2 documents, extract 5 keywords from each, repeat 3 times, keep top 3 keywords by self-consistency, then **one separate reranking pass per keyword** over BM25's top-1000, alongside the original query — [Li et al., arXiv 2311.09175](https://arxiv.org/html/2311.09175v2)

### Inferences
- Plainly stated for the report: HyDE and query2doc are popular and were evaluated against weak baselines. HyDE's comparison set is BM25 and unsupervised Contriever; query2doc's best-case numbers are BM25-relative, and its own dense-retriever rows show the gain collapsing to under a point. Neither paper reports a reranked pipeline.
- The one serious attempt to make expansion work in front of a strong reranker (Li et al.) ends up +0.006 nDCG@10 on BEIR for a procedure costing multiple generations plus multiple full reranking passes per query. For a precision-sensitive top-4 target, that is a cost-benefit case against, not for.

### Gaps
- I found **no** study evaluating any of these techniques on top of exactly BM25 + dense fusion (RRF or similar) + cross-encoder. Li et al.'s setup is BM25 top-1000 → cross-encoder, i.e. sparse-only first stage. Treat "no fusion-plus-reranker evidence exists" as the honest answer rather than interpolating.

## Q3: Evidence on query rewriting hurting, and on selective/adaptive rewriting

### Takeaway
There is a directly on-point controlled study: on FiQA, prompt-only LLM rewriting cost 9.0%
nDCG@10 on average across four dense retrievers, with p < 0.001, and the documented mechanisms are
exactly the "rewrite breaks a query that worked" modes — terminology drift, hallucinated context,
over-specification, over-formalisation. Selective rewriting is studied and the honest result is
that learned gating recovers very little of the oracle gain.

### Cited Findings

**The controlled regression study** — 648 FiQA / 300 SciFact / 50 TREC-COVID queries, four dense retrievers (MPNet and BGE, each base and FiQA-post-trained), rewriter Ministral 8B prompt-only at temperature 0.7, max 100 tokens, FAISS k=100, nDCG@10 with pytrec_eval, 10,000-iteration bootstrap and paired t-tests:
- Mean deltas from rewriting: **FiQA −9.0%, SciFact +0.3%, TREC-COVID +5.1%** — [Not All Queries Need Rewriting, arXiv 2603.13301](https://arxiv.org/html/2603.13301v1) (also listed at [ICLR 2026](https://www.iclr.cc/virtual/2026/10016859))
- MPNet base FiQA: 0.500 → 0.448 (−10.3%), Δ = −0.051, 95% CI [−0.067, −0.036], **p < 0.001**; Recall@10 dropped 9.4%
- BGE base FiQA: 0.391 → 0.357 (−8.7%); MPNet-FiQA post-trained: 0.507 → 0.452 (−10.9%)
- TREC-COVID MPNet base: 0.513 → 0.559, Δ = +0.046, 95% CI [+0.001, +0.094], p = 0.024 — **marginal after Bonferroni correction** (α = 0.017)
- SciFact MPNet base: p = 0.47, no effect
- Rewrite rates: 99.4% FiQA, 98.0% SciFact, 100% TREC-COVID — the model rewrites essentially everything when asked to
— all from [arXiv 2603.13301](https://arxiv.org/html/2603.13301v1)

**Documented failure modes** (FiQA, MPNet base; of 648 queries, **225 degraded, 122 improved, 301 unchanged**):
- *Terminology drift* — domain jargon replaced with an adjacent term ("transfer" → "rollover"); nDCG@10 **1.0 → 0.0**
- *Context injection / hallucinated assumption* — rewriter added an unsupported domain ("e-commerce") to a personal-finance query; 0.83 → 0.0
- *Over-specification* — a broad query narrowed to a subtype; 0.63 → 0.0
- *Over-formalisation* — informal wording that matched the corpus made formal; **1.0 → 0.0**
- Critically, lexical substitution appeared in 96.0% of degraded queries but also 95.9% of improved and 95.3% of unchanged ones, so **the presence of substitution does not predict the outcome** — there is no cheap surface signal for "this rewrite will hurt"
— [arXiv 2603.13301](https://arxiv.org/html/2603.13301v1)

**Selective / adaptive rewriting**, same paper, nDCG@10 by policy (FiQA / SciFact / TREC-COVID / avg):
- Never rewrite 0.500 / 0.656 / 0.513 / **0.556**
- Always rewrite 0.448 / 0.655 / 0.559 / **0.554** (worse on average than never rewriting)
- Dataset-level gating 0.500 / 0.656 / 0.559 / **0.572**
- Learned gate (logistic regression) 0.499 / 0.653 / 0.537 / **0.563**; gating AUC only 0.593 / 0.547 / 0.612; gated vs never-rewrite **p > 0.12, not significant**
- ΔVOR threshold 0.478 / 0.661 / 0.562 / 0.567
- Oracle upper bound 0.530 / 0.689 / 0.594 / **0.604** (FiQA oracle gain +3.0pp, ~6% relative)
— [arXiv 2603.13301](https://arxiv.org/html/2603.13301v1)

**Other regression evidence**
- TREC RAG'24: a single rewrite alone scored **Recall@500 0.217 vs 0.320 for the original query** — the rewrite was materially worse than doing nothing. Combining the original query with several rewrites performed best; gains levelled off after about three rewrites — [UiS-IAI@LiveRAG, arXiv 2506.22210](https://arxiv.org/pdf/2506.22210)
- query2doc itself regresses on BEIR when stacked on dense retrievers: SimLM+query2doc −0.6 on NFCorpus and −2.9 on SciFact; E5+query2doc −2.9 on SciFact; authors suggest distribution mismatch — [Wang et al., arXiv 2303.07678](https://ar5iv.labs.arxiv.org/html/2303.07678)
- query2doc with smaller generators produces shorter text with more factual errors: DL19 nDCG@10 52.0 (babbage) and 55.1 (curie) vs 66.2 (davinci-003) — i.e. most of the reported gain is bought with a large model — [Wang et al., arXiv 2303.07678](https://ar5iv.labs.arxiv.org/html/2303.07678)
- Iterative self-correction with GPT-4 was mixed: DL19 fell 69.2 → 68.6 while DL20 rose 64.5 → 64.8 — [Wang et al., arXiv 2303.07678](https://ar5iv.labs.arxiv.org/html/2303.07678)
- DMQR-RAG's dynamic selection reduced the rewrite count by roughly **40%** vs a fixed four rewrites while improving retrieval and answer accuracy; the authors argue avoiding unnecessary rewrites reduces noisy retrieved documents — [arXiv 2411.13154](https://arxiv.org/pdf/2411.13154)
- R²Adapter reports gains saturating when rewriting is applied to all routed queries, concluding selective rewriting is sufficient — [arXiv 2609.02894](https://arxiv.org/pdf/2609.02894) (recent preprint, unreviewed)
- Older precedent for per-query rewrite selection: Macdonald, Tonellotto and Ounis (SIGIR 2017) selected among query rewritings per query using predicted execution time, cutting mean response time 49% without significantly hurting effectiveness — [CNR open portal](https://openportal.isti.cnr.it/data/2017/384717/2017_384717.postprint.pdf)
- Rewriting as bias mitigation: simple LLM rewriting gave the largest aggregate retriever-bias reduction (54%) but failed where multiple biases combined — [Goyal et al., ACL 2026 Findings](https://preview.aclanthology.org/ingest-acl/2026.findings-acl.414/)
- Human rewrites that preserve meaning still destabilise adaptive RAG: more compressed phrasing and higher query loss amplify subquery drift and increase retrieval failure — [arXiv 2604.10745](https://arxiv.org/pdf/2604.10745) (recent preprint, unreviewed)

### Inferences
- The decisive number for a precision-sensitive system is the policy table: *always rewrite* (0.554) is worse on average than *never rewrite* (0.556), and the learned gate's advantage over never-rewriting is not statistically significant. The best policy in that study is a per-dataset constant decision, not a per-query model — which for a single-corpus system reduces to "decide once, offline, by measurement."
- The direction of harm is domain-coupled in a way that matters here: rewriting helped most where the baseline was weak and the query was short and jargon-light (TREC-COVID, 50 queries, marginal significance), and hurt most where queries were already well-formed and used corpus-matching domain vocabulary (FiQA). A system whose questions already use the corpus's own identifiers and terminology sits on the FiQA side of that divide.
- The four failure modes are all precision-destroying rather than recall-destroying — they move a document from rank 1 to outside the cutoff entirely (1.0 → 0.0 on nDCG@10 in two of the four cases). That maps directly onto a top-4-of-4 scoring criterion: these are exactly the regressions such a metric punishes hardest.

### Gaps
- arXiv 2603.13301 reports **no inference cost figures** (no latency, tokens or compute per query), only that post-training was kept compute-comparable.
- Its failure-mode taxonomy comes from a manual read of the 20 most severely degraded queries — a small qualitative sample, not a labelled distribution.
- No regression study I found uses a reranked pipeline, so whether a cross-encoder would repair these specific rewrite failures is unmeasured.

## Q4: Entity and identifier linking as a retrieval step

### Takeaway
This is the thinnest evidence base relative to how central the question is. ELQ is well
measured as an *entity linking* task (large F1 gains, 2x speed) and is reported to improve
downstream QA, but I could not retrieve numbers isolating "extract an identifier and use it to pin
or filter results" against a text-matching baseline on retrieval metrics.

### Cited Findings
- ELQ: bi-encoder, single pass, joint mention detection and disambiguation; **+12.7% F1 on WebQSP and +19.6% F1 on GraphQuestions** over prior SOTA under extended multi-entity annotations; authors report it significantly improves downstream QA performance of GraphRetriever; released in the facebookresearch/BLINK repo — [Li et al., EMNLP 2020](https://aclanthology.org/2020.emnlp-main.522)
- ELQ's joint approach lets multiple candidate mentions be considered simultaneously, and it is about **2x faster end-to-end** than neural baselines, which the follow-up cites as making it suitable for downstream QA tasks — [Information Retrieval with Entity Linking, arXiv 2404.08678](https://arxiv.org/pdf/2404.08678)
- Counter-evidence on the structured-pin approach generally: replacing text retrieval with graph-query retrieval over a property graph cost about **11 accuracy points and about 31 points of paraphrase consistency** vs text-based baselines — i.e. the structured path was both less accurate and markedly less stable under rephrasing — [gupea.ub.gu.se](https://gupea.ub.gu.se/handle/2077/89828)
- Where structured retrieval does win, it is with schema awareness and a narrow domain: on 40 pharmacology questions a schema-aware text-to-Cypher pipeline scored 85% vs 75% for standalone GPT-4o, and purpose-built domain tools scored 98%. Single small study — [arXiv 2603.15080](https://arxiv.org/pdf/2603.15080)

### Inferences
- The strongest available argument for identifier extraction is not a retrieval-metric gain but a *failure-mode* argument: a correctly extracted identifier used as a filter cannot produce the four documented rewrite regressions (drift, hallucinated context, over-specification, over-formalisation), because it does not alter the text query at all. The risk shifts entirely to extraction precision — a wrong identifier filters the right answer out completely, which is a worse failure than a blurred query.
- The pharmacology result (98% for purpose-built domain tools vs 85% for generated graph queries) and the paraphrase-consistency loss together suggest that where the identifier space is small and known, a deterministic extractor beats a generative one. That favours a pattern-matched or gazetteer-based identifier extractor over an LLM for this specific job.

### Gaps
- **I found no measured comparison of "extract identifier, filter/pin results" vs "match the identifier as text" on retrieval metrics.** This is the question closest to the target system and the literature does not answer it directly. The report should say so plainly rather than borrow the ELQ F1 numbers as if they were retrieval gains.
- I could not read arXiv 2404.08678 (returned as an unparseable PDF), so its own retrieval numbers for entity-linking-augmented IR are unretrieved. It remains the single most promising unread source for this question.
- Balog's entity-oriented search literature was not successfully mined within budget.

## Q5: Determinism and reproducibility of a rewriting step

### Takeaway
The IR literature largely does not address this, but the one quantification I found is striking:
query2doc's own authors measured run-to-run variance of **±1.14 and ±1.63 nDCG@10** across three
runs on TREC DL19/DL20 — variance of the same order as the entire gain claimed by many rewriting
papers. Grammar-constrained decoding is the available mitigation and it is measured, but on
output validity, not on retrieval stability.

### Cited Findings
- query2doc sparse results across 3 runs: **DL19 64.8 ± 1.14, DL20 60.9 ± 1.63**, versus 66.2 and 62.9 reported in the main table — the headline numbers sit above the 3-run mean — [Wang et al., arXiv 2303.07678](https://ar5iv.labs.arxiv.org/html/2303.07678)
- HyDE samples generations at **temperature 0.7**, i.e. the published method is non-deterministic by construction — [Gao et al., arXiv 2212.10496](https://arxiv.org/html/2212.10496v1)
- The regression study's rewriter also ran at temperature 0.7 — [arXiv 2603.13301](https://arxiv.org/html/2603.13301v1)
- Temperature 0 and a fixed seed do **not** guarantee reproducibility: finite-precision arithmetic makes results depend on execution order — [Non-Determinism of "Deterministic" LLM Settings, arXiv 2408.04667](https://arxiv.org/html/2408.04667v5)
- Named causes of residual nondeterminism: GPU floating-point computation, small differences in top-k log probabilities, sparse mixture-of-experts routing, and varying inference hardware — [arXiv 2408.04667](https://arxiv.org/html/2408.04667v5)
- For small models (2–8B) on multiple-choice benchmarks, the share of questions answered consistently across repeated runs is typically **50–80% at low temperatures**, varying considerably by model — [arXiv 2408.04667](https://arxiv.org/html/2408.04667v5)
- Token-probability-level nondeterminism can significantly affect generated text whenever temperature > 0, with the largest effects for mid-range probabilities rather than those near 0 or 1 — [arXiv 2601.06118](https://arxiv.org/html/2601.06118v1)
- Benchmark results are conventionally reported as a single run, which hides this variance; the authors argue nondeterminism metrics should be included in any ML evaluation — [arXiv 2408.04667](https://arxiv.org/html/2408.04667v5), and see [arXiv 2410.03492](https://arxiv.org/html/2410.03492v1) on quantifying uncertainty in benchmark scores
- Grammar-constrained decoding masks tokens that would break the grammar by zeroing their probability — [Pre3, arXiv 2506.03887](https://arxiv.org/html/2506.03887v1)
- SQL and similar logical forms are describable by deterministic context-free grammars, and an LR parser can constrain a semantic parser to emit only derivable sequences; LR(1) grammars parse deterministically with one token of lookahead — [ACL Findings 2021](https://aclweb.org/anthology/2021.findings-acl.108.pdf); [Pre3, arXiv 2506.03887](https://arxiv.org/html/2506.03887v1)
- Grammar-constrained decoding over Wikidata information extraction beat unconstrained few-shot prompting with LLaMA models and approached task-specific finetuned models — [arXiv 2305.13971](https://arxiv.org/abs/2305.13971v1)
- Constrained decoding has real overhead: XGrammar adds up to **37.5% latency at batch size 512** — [ACL 2025](https://preview.aclanthology.org/ingestion-acl-25/2025.acl-long.551.pdf); engines reduce it by precomputing context-independent tokens, and tokens fixed by the grammar can skip sampling entirely

### Inferences
- The query2doc variance figure is the most useful single fact for this question: a technique reporting a +15-point gain over BM25 carries ±1.1–1.6 points of run-to-run noise, which means that any *small* rewriting gain (the +0.4 to +1.4 MRR@10 seen over dense retrievers, or Li et al.'s +0.006 BEIR) is inside or near the noise floor of a single-run evaluation. A rewriting step added to an evaluated pipeline makes its own benchmark number a sample rather than a measurement.
- Nobody in this literature treats "the same question retrieves differently on different runs" as a correctness problem in its own right; it appears only as reported error bars or not at all. That is a genuine gap and the report should name it as such rather than imply the field has a recommended practice.
- Grammar-constrained decoding does not make the *choice* of output deterministic — it makes the output *well-formed*. It removes parse errors (the 5% category in the Cypher failure breakdown) and schema-invalid queries (10%), but not the dominant 52% answer-mismatch category. It is a validity guarantee, not a reproducibility guarantee. A rule-based or regex extractor is the only option in this review that is actually deterministic.

### Gaps
- I found **no** IR-specific study of rewrite nondeterminism's effect on retrieval metrics across repeated runs beyond query2doc's own 3-run table. The nondeterminism literature I located is LLM-benchmark literature, not retrieval literature.
- No work I found proposes constrained or grammar-based *query rewriting* (as opposed to constrained formal-query generation) to avoid a free-form model. Treat that as an unexplored design space rather than a documented technique.

## Q6: Routing and intent classification

### Takeaway
Routing is well motivated and poorly measured. I found cost-saving figures but no study
quantifying the error cost of misrouting as a retrieval or answer-quality penalty — the closest
thing is the regression study's dataset-level gating result, which is routing at a coarse grain
and is the best-performing policy it tested.

### Cited Findings
- RAGRouter-Bench baseline study: its best high-accuracy classifier reaches **28.1% token savings** in a post-hoc cost simulation; notes a uniform RAG paradigm is the common default in deployed systems — [arXiv 2604.03455](https://arxiv.org/pdf/2604.03455) (recent preprint, unreviewed)
- CA-RAG selects retrieval depth per query by balancing predicted answer quality against latency and billed tokens, with weights tunable toward speed, savings or quality without retraining; under that framing misrouting manifests as a quality penalty or wasted tokens — [MDPI AI 7(250)](https://mdpi-res.com/d_attachment/ai/ai-07-00250/article_deploy/ai-07-00250.pdf)
- An ADR hypergraph-RAG design uses an LLM intent classifier emitting a category **plus a confidence score, with a conservative fallback** — the architectural answer to misrouting risk is an abstain path — [arXiv 2609.29282](https://arxiv.org/pdf/2609.29282) (recent preprint, unreviewed)
- R²Adapter routes queries to a graph-based RAG system and then thresholds which routed queries get rewritten, finding selective rewriting sufficient — [arXiv 2609.02894](https://arxiv.org/pdf/2609.02894) (recent preprint, unreviewed)
- Relevant adjacent result: in the regression study, **dataset-level routing (rewrite one corpus, not the others) was the best non-oracle policy at 0.572 avg nDCG@10**, beating the learned per-query gate at 0.563 and always-rewrite at 0.554 — [arXiv 2603.13301](https://arxiv.org/html/2603.13301v1)
- RQ-RAG's control-token design is effectively a learned router over {rewrite, decompose, disambiguate, no retrieval}, for a +1.9% average gain on single-hop QA — [arXiv 2404.00610](https://arxiv.org/html/2404.00610v1)

### Inferences
- The per-query gating AUCs in the regression study (0.547–0.612) are the most concrete evidence available on how hard the routing decision actually is: barely above chance for deciding something as simple as "should this query be rewritten." A router deciding among several retrieval strategies is a harder version of the same problem, and that AUC range should temper expectations for any learned per-query dispatcher.
- Vendor claims of 95% LLM-call reductions and 40% cost cuts from agentic routing circulate widely but are marketing figures without controlled measurement — [droptica.com](https://www.droptica.com/blog/intelligent-routing-chatbot-questions-how-we-cut-ai-api-costs-95/); [labs.adaline.ai](https://labs.adaline.ai/p/building-production-ready-agentic). The report should not reuse them.

### Gaps
- **No source quantifies the error cost of misrouting directly** (e.g. "nDCG@10 when a query is sent to the wrong strategy vs the right one"). Routing papers report savings and aggregate accuracy, not the conditional penalty. This is a real gap and should be stated.
- Several routing sources are 2026 preprints without peer review (2604.03455, 2609.29282, 2609.02894); their figures should be attributed as preprint claims.

## Cross-cutting: inference cost per query

### Takeaway
The only paper in this set with concrete latency numbers makes the cost very legible: a rewriting
step turns a 16ms retrieval into a ~2-second one, dominated by the LLM call.

### Cited Findings
- query2doc, MS MARCO dev, top-100, single thread: BM25 index search **16 ms**; BM25+query2doc index search **177 ms** (longer queries cost more index time); the **LLM call exceeds 2000 ms** depending on server load. The method requires token-by-token autoregressive decoding — [Wang et al., arXiv 2303.07678](https://ar5iv.labs.arxiv.org/html/2303.07678)
- query2doc's experiments consumed about 550k API calls; the extracted page text appears to state roughly 55k dollars of API spend but the figure was garbled in extraction and **I could not verify it** — [Wang et al., arXiv 2303.07678](https://ar5iv.labs.arxiv.org/html/2303.07678)
- HyDE in practice is often run with multiple generations averaged — Haystack's implementation generates five hypothetical documents, embeds each and averages the vectors, i.e. five LLM calls per query — [Haystack docs](https://docs.haystack.deepset.ai/docs/hypothetical-document-embeddings-hyde)
- Li et al.'s reranker-safe expansion needs 3 rounds × 2 documents of generation plus one reranking pass per retained keyword over BM25's top-1000 — several generations and several full reranking passes per query — [arXiv 2311.09175](https://arxiv.org/html/2311.09175v2)
- Elastic's practitioner write-up notes HyDE adds an LLM round trip to every query, and that when the model commits to one interpretation of an ambiguous question it can narrow retrieval instead of broadening it. Their own test improved precision and recall by 50% on two of three test queries and tied on the third — a three-query sample, not a benchmark — [Elastic Search Labs](https://www.elastic.co/search-labs/es/blog/hyde-semantic-search-elasticsearch)
- A follow-up paper notes HyDE "is highly reliant on the LLMs' parametric knowledge, which can be a barrier in deployment for out-of-domain corpora settings" — [arXiv 2410.21242](https://arxiv.org/html/2410.21242v1)
- arXiv 2603.13301, the most directly relevant regression study, reports **no** cost figures at all

### Inferences
- For a pipeline whose only query-time model call is embedding the question, every technique in this review except deterministic entity/identifier extraction and grammar-constrained parsing adds at least one generative call, i.e. a roughly two-orders-of-magnitude increase in query-time latency by query2doc's own measurements, before any accuracy effect is counted.
- The HyDE-with-five-generations practice multiplies both cost and the run-to-run variance problem; averaging embeddings reduces variance in the vector but still samples five times from a temperature-0.7 distribution per query.
