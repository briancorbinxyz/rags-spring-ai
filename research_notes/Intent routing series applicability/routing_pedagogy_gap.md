# How query/intent routing is written about, taught and measured in the RAG practitioner literature

**Scope note.** This round is about *presentation and evaluation*, not technique. Classifier technique (zero-shot vs trained, calibration, abstention mechanics) was covered in an earlier round and is deliberately not re-derived here.

**Verification discipline used.** Findings marked **[fetched]** were read directly from the source page. Findings marked **[search-reported]** come from search-engine summarisation of a PDF I did not open; treat the exact figures as provisional and re-check before publication. Findings marked **[vendor]** are framework or product documentation — advocacy for their own abstraction, not evidence.

---

## 1. What does the practitioner canon actually say about routing?

### Takeaway

Routing is taught almost universally as a **design pattern with a code example and no evaluation**. Framework documentation defines the mechanism (how to wire a branch), lists the selector variants, and stops. Not one framework page I read discusses router accuracy, confidence thresholds, abstention, or failure modes. The single exception is `semantic-router`, which ships an `evaluate()` method — an abstraction that at least *admits* the router is a classifier.

### Cited Findings

- **LlamaIndex router module guide** [fetched] [vendor] frames routers as "modules that take in a user query and a set of choices (defined by metadata) and return one or more of them" and as "simple but powerful modules that use LLMs for decision making capabilities." The page documents LLM selectors vs Pydantic selectors, single vs multi variants, standalone selector use, and marks `ToolRetrieverRouterQueryEngine` as beta. It contains **no** mention of evaluation, accuracy, confidence scores, abstention, no-match behaviour, or any failure mode — [LlamaIndex docs](https://developers.llamaindex.ai/python/framework/module_guides/querying/router/)
- **LlamaIndex `RouterQueryEngine` example** [vendor] exposes the decision via `selector_result` in the response metadata, including the chosen index and a free-text "reason" for the choice. That reason string is the closest thing the framework offers to an auditable decision record, and it is not a score — [LlamaIndex RouterQueryEngine](https://developers.llamaindex.ai/python/examples/query_engine/routerqueryengine/)
- **LangChain routing how-to** [search-reported] [vendor] presents exactly two approaches: "Conditionally return runnables from a RunnableLambda (recommended) or using a RunnableBranch (legacy)." `RunnableBranch` is "initialized with a list of (condition, runnable) pairs and a default runnable"; it "selects the first condition to evaluate to True." Semantic routing appears as a separate section whose idea is "to use embeddings to route a query to the most relevant prompt." No evaluation guidance appears on the page — [LangChain Python how-to: routing](https://python.langchain.com/docs/how_to/routing); [LangChain JS routing](https://js.langchain.com/docs/how_to/routing)
- A third-party write-up states `LLMRouterChain` — the original LangChain LLM-powered router — "is deprecated in latest versions" [search-reported, unverified against LangChain's own deprecation notice] — [Towards Data Science: Routing in RAG-driven applications](https://towardsdatascience.com/routing-in-rag-driven-applications-a685460a7220/)
- **Haystack `ConditionalRouter`** [search-reported] [vendor] "routes your data through different paths down the pipeline by evaluating the conditions that you specified," with each route defined as a dictionary and the idiomatic pattern being a specific condition plus a catch-all whose condition is literally `{{ True }}`. The docs carry one operational caveat worth noting: an optional variable used in a condition but not supplied at runtime "is evaluated as None," which can change the routing outcome **without raising an error** — [Haystack ConditionalRouter](https://docs.haystack.deepset.ai/docs/conditionalrouter)
- **`semantic-router` (aurelio-labs)** [search-reported] [vendor] is the exception: the `HybridRouter` API documents an `evaluate` method that "takes inputs and expected route labels and returns the accuracy of the route selection as a float," and the library has route-threshold optimisation/training. So one framework in the ecosystem treats the router as a classifier with a labelled test set — [Aurelio docs: HybridRouter](https://docs.aurelio.ai/docs/semantic-router/client-reference/routers/hybrid); [Aurelio docs: routers](https://docs.aurelio.ai/docs/semantic-router/user-guide/components/routers)
- **Courses.** DeepLearning.AI's *Building AI Applications with Haystack* (intermediate, 1h35m, 8 video lessons, 6 code examples) includes a conditional-routing lesson whose stated goal is "to create a branching pipeline with a fall back to web-search when the LLM does not have the context needed to fully respond to the user's query." The syllabus describes the mechanism, not its measurement — [DeepLearning.AI course page](https://www.deeplearning.ai/short-courses/building-ai-applications-with-haystack)
- **Taxonomy as the pedagogical unit.** The recurring teaching device is a three-way taxonomy — *logical/keyword*, *semantic/embedding*, *LLM/agentic* — rather than a measurement. A representative practitioner article defines logical routers as working "on discrete rules, such as conditions on string length, file names, or integer values, rather than on understanding the query's intent." The taxonomy is the lesson; the reader is not told which to pick on evidence — [Towards Data Science: Routing in RAG-driven applications](https://towardsdatascience.com/routing-in-rag-driven-applications-a685460a7220/)
- **Cookbook-level treatment** follows the same shape: the Mistral/LangGraph adaptive-RAG cookbook routes between a vectorstore and web search using an LLM with structured output, prompted with a description of what the vectorstore covers. No router eval — [Mistral docs: Adaptive RAG with LangChain](https://docs.mistral.ai/resources/cookbooks/third_party-langchain-adaptive_rag_mistral)

### Inferences

- The framework documentation is *written for the author of the pipeline, not the operator of it*. Every page answers "how do I express a branch?" and none answers "is my branch right?" That is a coherent editorial choice for API reference material, but because these pages are also the de facto curriculum, the omission propagates into practice.
- LlamaIndex's phrase "simple but powerful" is the load-bearing rhetorical move in the whole canon. It licenses the reader to skip measurement: a *simple* component does not obviously need a test set.
- The `semantic-router` `evaluate()` method is the strongest counter-evidence to a claim of total absence. A rigorous treatment should cite it rather than claim nobody has thought of this — but note it returns **accuracy only**, a single scalar, which is precisely the metric that hides the failure the series found (see §2 and §6).

### Gaps

- **Books remain unverified and this is the weakest part of these notes.** I could not confirm whether Chip Huyen's *AI Engineering* (O'Reilly, 9798341621701 is a different book; Huyen's is 9781098166298) has a dedicated routing treatment. Her author bio frames models as "one component of an application that also needs routing, caching, guardrails, observability, and failure handling," which suggests routing is discussed as architecture, but I could not read the relevant chapter — [O'Reilly: AI Engineering](https://oreilly.com/library/view/ai-engineering/9781098166298)
- *Hands-On RAG for Production* (Mendelevitch & Bao, O'Reilly, May 2026) is the most relevant current practitioner title. Its Ch.2 covers the base stack as ingestion flow + query flow (parsing, chunking, embedding, indexing, vector search, reranking, generation) and Ch.3 covers scaling, advanced retrieval, guardrails and hallucination handling. **No routing section appeared in the visible TOC**, and a direct fetch of Ch.3 returned HTTP 403 (paywalled), so I cannot state what it says about routers — [O'Reilly: Hands-On RAG for Production, ch02](https://www.oreilly.com/library/view/hands-on-rag-for/9798341621701/ch02.html)
- I found no conference talk (AI Engineer World's Fair or similar, 2024–2026) that presents router metrics. Absence of evidence here is weak evidence — talk recordings are poorly indexed — so do not assert "no talks exist."

---

## 2. Does anyone publish a router's measured precision/recall? (central question)

### Takeaway

**The premise is mostly right but not entirely, and the exception is important enough that the series must cite it.** In the *practitioner* literature — books, courses, framework docs, engineering blogs — I found essentially nothing that reports classifier-level precision/recall for a retrieval router. But in the 2026 *academic* literature there is now at least one production case study that publishes exactly this kind of embarrassing number, and it is uncannily close to the series' own findings. Notably, even that paper reports **recall and accuracy but not precision**.

### Cited Findings

**The one close match — and it is close.**

- **"The Coverage Illusion: From Pre-retrieval Routing Failure to Post-retrieval Cascades in a Production RAG System"**, Zafar Hussain & Kristoffer Nielbo, arXiv:2605.27220, submitted 26 May 2026, cs.CL. Production RAG over the Danish National Encyclopedia, 20,000 query-workflow pairs, five candidate retrieval workflows — [arXiv abstract](https://arxiv.org/abs/2605.27220); [full HTML](https://arxiv.org/html/2605.27220v1)
- Their **hand-crafted string rules** (query length, question marks, Danish question words) achieved **38.4% routing accuracy**, which they characterise as "just above the uniform-random baseline of 20% for five classes" (§5.1) [fetched]
- **Per-class recall for the rule router**: Semantic 63.2%, HyDE 37.3%, Hybrid 25.8%, and **QE-CE and Semantic-CE receive zero correct predictions** (§5.1) [fetched]. A rule with zero correct predictions is published. This is the direct analogue of the series' 0% precision rule.
- The rule router's downstream composite quality was **4.202 vs 4.258 for the trivial Always-HyDE baseline** on a stratified test split (n=494) — i.e. the router was *worse than not routing* (§5.1) [fetched]
- Learned classifiers barely beat the no-router baseline: best was LR over TF-IDF char (3–5)-grams at CO 4.269 (**+0.011** vs Always-HyDE), with LR over multilingual embeddings at 3.850 (**−0.408**). Oracle ceiling was 4.733 (§5.2, Table 2, n=442) [fetched]
- **Degeneracy is reported explicitly**: the best LR model "route[s] over 90% of queries to HyDE" on 5,000 unseen real user queries; a fine-tuned all-MiniLM-L6-v2 router sent 73.8% to HyDE and 26.2% to Hybrid and **none to any other workflow**; regression routers above δ=0.75 "collapse to Always-HyDE" (§5.2–5.4) [fetched]
- Crucially for the series' angle: **this paper reports accuracy, per-class recall and composite downstream quality, but no precision and no F1 for any router** [fetched]. The precision-side view — "when this rule fires, how often is it right?" — is absent even here.
- The paper also names the frameworks: §2 observes that LangChain and LlamaIndex use "hand-crafted heuristics" for routing [fetched]
- And it kills a specific folk heuristic: for one-to-three-word queries "the most are still routed to HyDE," so **query length is not a reliable routing signal** (§5.2) [fetched]

**Other academic work reporting router-level classifier metrics.**

- **RAGRoute** — "Efficient Federated Search for Retrieval-Augmented Generation using Lightweight Routing", Dhasade, Guerraoui, Kermarrec, Petrescu, Pires, Randl & de Vos, arXiv:2502.19280, submitted 26 Feb 2025, v2 9 Apr 2026, DAIS 2026 (earlier version EuroMLSys 2025). This router selects **data sources**, not models: "dynamically selects relevant data sources at query time using a neural classifier." [fetched, abstract] — [arXiv](https://arxiv.org/abs/2502.19280)
  - The abstract page reports **only end-to-end system results**: retrieval "matching the accuracy of querying all sources," with up to **80.65%** reduction in communication volume and **52.50%** in latency [fetched]
  - The PDF reportedly scores the router on accuracy, precision, recall, F1 and AUC, with accuracy from **83.05% (FEB4RAG) to 90.93% (MMLU)** and recall between **82.92% and 89.70%**, deliberately tuned to favour recall because missing a relevant source costs more than an extra query [search-reported — **verify in the PDF before citing**]
- **ORDER: Task-Conditioned Routing for Retrieval-Augmented Generation**, Aurélien Pellet, Julien Perez, Marie Puren, arXiv:2609.17012, 15 Sep 2026. Routes to **retrieval sources/pre-built indices**, not LLMs: a supervised query router "predicts which collections are most likely to contain relevant evidence," with nearest-centroid assignment to task clusters that each carry their own chunking, metadata filtering and reranking config [fetched, abstract] — [arXiv](https://arxiv.org/abs/2609.17012)
  - The abstract page contains **no** precision/recall and only the qualitative claim that query conditioning "consistently outperforms both naive baselines and strong state-of-the-art RAG systems" [fetched]. A per-class precision/recall table with weighted average 0.80 is reported by search summarisation of the PDF [search-reported — **unverified, do not cite without reading the PDF**]
- **RAGRouter-Bench: A Dataset and Benchmark for Adaptive RAG Routing**, Ziqi Wang, Xi Zhu, Shuhang Lin, Haochen Xue, Minghao Guo, Yongfeng Zhang, arXiv:2602.00296, 30 Jan 2026 (v2 4 Apr 2026). Routes among **RAG paradigms** — "routing as context-dependent paradigm selection" — not among retrievers and not among LLMs. The abstract names "quantitative metrics," LLM-as-a-Judge evaluation, generation quality and resource consumption, but **no router classification metrics** [fetched] — [arXiv](https://arxiv.org/abs/2602.00296)
- A companion baseline study (arXiv:2604.03455) reportedly compares classifiers on routing accuracy and macro-F1 under 5-fold CV, with TF-IDF+SVM best at **93.2% accuracy / 0.928 macro-F1** against a **52.9%** majority-class baseline [search-reported] — [arXiv PDF](https://arxiv.org/pdf/2604.03455)

**Practitioner side: nothing comparable for retrieval routing.**

- Third-party evaluation guidance for LlamaIndex proposes evaluating "the router as its own layer, using a custom LLM judge to score routing accuracy," and notes that the built-in `FaithfulnessEvaluator` and `RelevancyEvaluator` "do not understand router decisions" [search-reported] [vendor] — [FutureAGI: evaluating LlamaIndex RAG 2026](https://futureagi.com/blog/evaluating-llamaindex-rag-2026/). This is a *proposal to measure*, not a published measurement — and it reaches for an LLM judge rather than a labelled set and a confusion matrix.
- An Adaptive-RAG hobby project README reports per-route precision and recall with a confusion matrix and routing accuracy [search-reported] — [GitHub: R4V1-11/Adaptive-RAG](https://github.com/R4V1-11/Adaptive-RAG). A project README, unreviewed, over a toy corpus.

**The discipline exists next door, in contact-centre intent classification.** These are vendor blogs, but they articulate the exact methodology the RAG routing literature lacks:

- A router with **0.92 macro accuracy** that still "misrouted cancellations to billing and left a fraud queue empty"; the scalar decomposes into "0.97 on the common five and 0.31 on the long tail of 45, and the headline won't move." The recommendation is per-intent precision, recall, support and F1 on a production-distribution set, "treating the confusion matrix as the primary artifact" [search-reported] [vendor] — [FutureAGI: intent classification evaluation pipeline](https://futureagi.com/blog/intent-classification-evaluation-pipeline-2026/)
- "Offline quality per intent, not average: precision, recall, macro-F1," combined with online outcomes (containment, transfer rate), plus calibration checks because "a model that says 0.92 and is right 0.60 is a transfer machine." Confusion pairs are ranked by consequence (refund vs cancellation: high; password reset vs locked account: critical, because of missing step-up auth) [search-reported] [vendor] — [Teammates.ai: intention detection for routing](https://teammates.ai/blog/intention-detection-that-routes-customers-to-the-right-outcome)
- One practitioner blog reports a deployment that cut end-to-end routing latency from 5,000ms to 100ms "while achieving 92–96% precision after iterative example refinement" [search-reported] — [TianPan.co: the intent classification layer most agent routers skip](https://tianpan.co/blog/2026/04/16/intent-classification-agent-routers). Note the title: even this author's framing is that the layer is *skipped*.

### Inferences

- **State the finding this way, not as "nobody publishes this."** The accurate claim is: *per-rule precision for a retrieval router is published essentially nowhere in the practitioner literature; the closest published analogue is an academic production case study (Coverage Illusion, 2026) that reports accuracy and per-class recall, including a zero-recall class, and still does not report precision.* That is a defensible, specific, checkable claim and it is more interesting than the overclaim.
- The series' numbers (21%, 25%, 0% per-rule precision; 41% when the document router commits) are in the same register as Coverage Illusion's (38.4% accuracy; two classes at zero). Two independent systems landing near random is a real signal, and the series can honestly say its results *corroborate* a 2026 finding rather than stand alone. That is a stronger rhetorical position than novelty.
- The **precision/recall asymmetry in what gets reported is itself a finding**. Academic routers are evaluated as multi-class classifiers, so the natural reporting unit is accuracy and macro-F1 — aggregate, per-system. The series' unit is *per-rule precision*: "when this specific hand-written rule fires, how often is the route correct?" That is a maintenance-oriented question, not a model-selection question, and I found no published treatment that asks it of a rule set. **This is the genuinely novel framing.**
- The contact-centre NLU world already has the methodology (per-class precision/recall, confusion matrix as primary artifact, calibration, consequence-ranked confusion pairs). It has not crossed into RAG routing pedagogy. The series can import it and say so.

### Gaps

- I did not read the RAGRoute, ORDER or RAGRouter-Bench PDFs. Three figures above are search-summarised and must be verified. Treat any of them as unusable until read.
- I found **no** engineering blog post from a named company reporting its retrieval router's measured precision. That absence is the central negative finding, but it is an absence over the indexed web, not a proof.
- No evidence found either way on whether any RAG book reports router metrics (see §1 gaps).

---

## 3. How are routers evaluated when they are evaluated? Is there a methodology or benchmark?

### Takeaway

There is a mature benchmark methodology for **model** routers and effectively none for **retrieval** routers. Model-router evaluation has converged on cost-quality tradeoff curves (APGR, CPT) rather than classification metrics. Where retrieval routers are evaluated at all, it is end-to-end downstream quality, usually against a no-router baseline — which is, to be fair, the right outer-loop test.

### Cited Findings

- **RouterBench** (arXiv:2403.12031, ICML 2024) is explicitly motivated by "the lack of a standard way to evaluate LLM routers." It releases >405k pre-generated inference outcomes from representative LLMs so routers can be tested without running inference, over prompts drawn from MBPP, GSM-8K, Winogrande, HellaSwag, MMLU, MT-Bench and others. Code/data at `withmartian/routerbench`. A reported finding is that some earlier routing methods fail to generalise to complex tasks and newer models, while simple routing performs well in several areas — [arXiv](https://arxiv.org/abs/2403.12031); [ICML listing](https://icml.cc/virtual/2024/39041); [GitHub](https://github.com/withmartian/routerbench)
- **RouteLLM** (arXiv:2406.18665, ICLR 2025; UC Berkeley, Anyscale, Canva) trains routers on human preference data from Chatbot Arena to choose between a strong and a weak LLM, reporting ">2x" cost reduction "without sacrificing response quality" on MMLU, MT-Bench and GSM8K. Its two metrics are **APGR** (Average Performance Gap Recovered — how much of the weak-to-strong gap the router recovers under a cost constraint) and **CPT** (Call-Performance Threshold — minimal strong-model usage to hit a performance target). Router variants explored: similarity-weighted ranking, matrix factorisation, BERT query classifier, causal-LLM query classifier — [arXiv HTML](https://arxiv.org/html/2406.18665v2); [ICLR 2025 poster](https://iclr.cc/virtual/2025/poster/30737)
- Note what APGR and CPT are **not**: they are not precision, recall or accuracy. Model-router evaluation deliberately reports a *tradeoff curve*, because a model router's errors are graded (a weaker answer) rather than categorical (the wrong corpus).
- The field now has **several competing model-router benchmarks** — a 2026 comparison table lists RouterBench alongside RouterEval, RouterArena and LLMRouterBench [search-reported] — [arXiv PDF 2601.07206](https://web3.arxiv.org/pdf/2601.07206)
- **Independent cross-router comparison** (arXiv:2608.14641) evaluates four open-source routers on a common interface across four benchmarks. On RouterBench, Aurelio's `semantic-router` reached a **0.683** success rate vs **0.300** for LiteLLM and RouteLLM routers and **0.400** for vLLM's, a gap of 0.383 over cheap-only routing (95% paired-bootstrap CI [0.233, 0.533], Holm-adjusted p<0.01). On BFCL v4 (function calling) **no pair of routers differed significantly** once trials were averaged within 30 tasks — Aurelio 0.811 vs cheap-small baseline 0.833 [search-reported] — [arXiv PDF](https://arxiv.org/pdf/2608.14641). This is methodologically the most rigorous router evaluation I found, and it evaluates these libraries as *model* routers.
- **Retrieval-side evaluation is downstream-quality-against-baseline.** Coverage Illusion's primary metric is a "Composite Overall" (CO) quality score compared against trivial always-one-workflow baselines and an oracle ceiling (4.733), with the router's job being to close the gap between them. Their best learned router captured **3.7% of the oracle gap** (Ridge, δ=0.75) and a GBM regression captured **1.4%** [fetched] — [arXiv HTML](https://arxiv.org/html/2605.27220v1)
- The headline cost/latency framing dominates the retrieval side too: Coverage Illusion's cascade claims **+0.140 CO over Always-HyDE, 31.8% latency reduction, and 72.2% of real user queries served without LLM augmentation** [fetched]. RAGRoute's claims are 80.65% communication reduction and 52.50% latency reduction [fetched].
- **A cost-aware retrieval-routing study exists** (arXiv:2606.02581, "Cost-Aware Query Routing in RAG: Empirical Analysis of Retrieval Depth Tradeoffs") [search-reported, not read] — [arXiv PDF](https://arxiv.org/pdf/2606.02581)

### Inferences

- Metrics that appear, in rough order of frequency: **end-to-end answer quality** (always), **cost/token savings** (usually), **latency** (usually), **routing accuracy** (sometimes, academic only), **per-class recall** (rarely), **per-rule precision** (not found).
- The oracle-ceiling framing in Coverage Illusion is the single most adoptable methodological idea for the series: report the router against *both* a trivial always-one-route baseline *and* an oracle upper bound, and quote the fraction of the oracle gap captured. The series' non-regression rule is the always-one-route baseline in disguise; adding the oracle ceiling would make the gain legible as a percentage of what is achievable rather than an absolute delta.
- There is **no benchmark for retrieval routers** with anything like RouterBench's status. RAGRouter-Bench (paradigm selection) is the nearest candidate and is nine months old.

### Gaps

- I could not confirm whether any retrieval-routing benchmark has been adopted by a second group (the test of whether a benchmark is real). Given RAGRouter-Bench's date, likely not yet.

---

## 4. The model-routing vs retrieval-routing conflation

### Takeaway

The asymmetry is real, large, and worth naming precisely. **Model routing** (which LLM answers?) has multiple benchmarks, an ICML paper, an ICLR paper, dedicated metrics, cross-router comparison studies and commercial products. **Retrieval routing** (which corpus/retriever/index answers?) has a handful of 2025–2026 papers, no established benchmark, and no agreed metric. The two are routinely discussed under the same word "routing," and the mature literature is almost entirely about the former.

### Cited Findings

- **Model routers** — RouterBench (ICML 2024, 405k outcomes, public dataset) — [arXiv](https://arxiv.org/abs/2403.12031); RouteLLM (ICLR 2025, preference data, APGR/CPT, >2x cost reduction) — [arXiv](https://arxiv.org/html/2406.18665v2); plus RouterEval, RouterArena, LLMRouterBench [search-reported] — [arXiv PDF 2601.07206](https://web3.arxiv.org/pdf/2601.07206); plus a four-router common-interface comparison with bootstrap CIs and Holm correction [search-reported] — [arXiv PDF 2608.14641](https://arxiv.org/pdf/2608.14641)
- **Retrieval/source routers** — RAGRoute selects **data sources** for federated search (DAIS 2026 / EuroMLSys 2025) [fetched] — [arXiv](https://arxiv.org/abs/2502.19280); ORDER's QRe "predicts which **collections** are most likely to contain relevant evidence" (Sep 2026) [fetched] — [arXiv](https://arxiv.org/abs/2609.17012); Coverage Illusion routes among five **retrieval workflows** (May 2026) [fetched] — [arXiv](https://arxiv.org/abs/2605.27220)
- **A third, often-conflated category: paradigm routing.** RAGRouter-Bench routes among **RAG paradigms** (naive/advanced/graph/etc.), explicitly "context-dependent paradigm selection" — neither model nor collection selection [fetched] — [arXiv](https://arxiv.org/abs/2602.00296)
- **Name collisions make this worse.** "Semantic router" names at least two unrelated projects: `aurelio-labs/semantic-router` (an embedding-similarity decision layer over utterance examples, MIT-licensed) and the **vLLM Semantic Router**, which reports a 10.24-point accuracy gain on MMLU-Pro and is a *model/reasoning-mode* router — [arXiv PDF 2510.08731](https://arxiv.org/pdf/2510.08731); [vLLM Semantic Router](https://arxiv.org/pdf/2603.04444). Attributing vLLM's numbers to Aurelio's library (an easy mistake that search results invite) would be wrong.
- The same libraries are evaluated *as model routers* in the cross-router comparison, even though `semantic-router` is widely used for intent routing — the benchmark infrastructure pulls everything toward model routing [search-reported] — [arXiv PDF 2608.14641](https://arxiv.org/pdf/2608.14641)

### Inferences

- The structural reason for the asymmetry: **model routing has a free oracle and retrieval routing does not.** RouterBench works because you can pre-generate every model's answer to every prompt once and then evaluate any router offline, for free. There is no equivalent for retrieval routing — the candidate set is corpus-specific, the labels are per-deployment, and no shared dataset of "which collection actually contained the answer" exists. That is exactly why per-deployment measurement (what the series does) is the only available instrument, and it is a point worth making explicitly.
- A second reason: model routing's payoff is **cost**, which is universally legible and dollar-denominated. Retrieval routing's payoff is **relevance**, which is corpus-specific and only measurable against a question set you built yourself. The literature follows the measurable money.
- Therefore the series should (a) define its terms in one sentence up front, (b) cite RouteLLM/RouterBench as the thing it is *not* about, and (c) explain the oracle-availability asymmetry as the reason the retrieval side is under-measured. That diagnosis is, as far as I can tell, not stated anywhere in the existing literature.

### Gaps

- I did not attempt a systematic count (e.g. citation counts or paper tallies) to quantify the asymmetry. The claim above is qualitative and based on what surfaced across eight searches; it should be phrased as "markedly asymmetric," not given a ratio.

---

## 5. Known anti-patterns and published failures

### Takeaway

Real, specific failure reports exist, but they are thin and scattered — mostly a single academic case study plus GitHub issue threads and blog posts. The recurring named failures are: degenerate collapse to one route, zero-recall routes, silent empty retrieval, silent default fallback that masquerades as success, and silent condition evaluation on missing variables.

### Cited Findings

**Degenerate collapse (best-evidenced).**

- Coverage Illusion, all [fetched]: best LR routes **>90% of 5,000 unseen real queries to a single workflow**; retraining on real user queries leaves routing "limited to the two cheapest workflows"; the fine-tuned neural router assigns **nothing** to three of five workflows; regression routers above a threshold "collapse to Always-HyDE"; two classes get **zero correct predictions** under the rule router — [arXiv HTML](https://arxiv.org/html/2605.27220v1)
- The same paper names the generalisation failure directly: synthetic queries suggested "LLM augmentation is needed for over 90% of queries," but "only 27.8% of real user queries need LLM augmentation" — a **synthetic-vs-real distribution mismatch** that invalidated the design-time analysis [fetched]

**Silent failures.**

- An arXiv paper on retrieval-state lock-in reports that in one clinical run **every silent error was an empty-retrieval case**, and proposes "the empty-route/abstention observable" as "the simplest reliable flag for this absence lock-in" [search-reported] — [arXiv HTML 2606.22728](https://arxiv.org/html/2606.22728)
- Haystack's own docs note that an optional variable used in a routing condition but not supplied at runtime "is evaluated as None," changing the route **without raising an error** [search-reported] [vendor] — [Haystack ConditionalRouter](https://docs.haystack.deepset.ai/docs/conditionalrouter)
- A model-router issue in the OpenHands agent SDK argues that "when the classifier fails to produce a usable answer, the tool silently falls back to `meta.default_model`," so a failure "looks like a normal successful classification"; the proposed fix is to fail loudly and retryably on no match [search-reported, low reliability — a GitHub issue] — [OpenHands issue #5068](https://github.com/OpenHands/software-agent-sdk/issues/5068)
- A related issue proposes treating "none of these" as a **first-class router outcome** with an owner-configurable fallback covering abstention, unknown IDs, errors and timeouts, "instead of forcing a pick or failing the turn" [search-reported, low reliability — a GitHub issue in a small repo] — [ghost issue #77](https://github.com/ferdousbhai/ghost/issues/77)
- **No reject threshold in the default framework router**: a third-party tutorial states `RouterQueryEngine.from_defaults()` "always selects the highest-confidence match and has no builtin reject threshold," and that the fallback example on the page is not actually implemented in the code [search-reported, third-party tutorial] — [theneuralbase: fallback routing when no match](https://theneuralbase.com/llamaindex/learn/intermediate/fallback-routing-when-no-match/). **This is consistent with what I read directly on the official LlamaIndex router page, which does not mention abstention or no-match handling at all** [fetched].

**Misrouting and blast radius.**

- A production write-up lists router misclassification as a failure category with a concrete example: "vacation days" queries routed to a Holiday expert instead of a Policies expert [search-reported] [blog] — [Medium: Most RAG failures are routing failures](https://medium.com/@sumeet7.kumar/most-rag-failures-are-routing-failures-e6e36748d224)
- Router as single point of failure: "an LLM router is making a semantic judgment," and that judgment gates everything downstream [search-reported] [vendor/blog]
- Access-control routing is called out as the highest-severity class: "access control bugs in application-layer filter logic are among the most severe and hardest-to-detect failures in production AI systems" [search-reported] [blog] — [DEV: RAG has a routing problem](https://dev.to/hosseinhezami/rag-has-a-routing-problem-knowing-which-knowledge-source-to-trust-5akf)
- **Keyword rules as a safety net, not a router**: multiple practitioner sources report that vector search alone fails on queries hinging on "a specific keyword, an exact product name, an error code, a policy term," and that keyword search "acts as a safety net against semantic retrievers failing on named entities" [search-reported] [blogs] — [managed-code: why RAG fails in production](https://managed-code.com/blog-post/why-rag-fails-in-production-9-failure-points-and-fixes)
- A separate production paper notes that "synchronization cost is consistently underestimated at design time" — the maintenance-burden claim, though for corpus sync rather than keyword lists specifically [search-reported] — [arXiv PDF 2605.03275](https://arxiv.org/pdf/2605.03275)

### Inferences

- **Drift as the corpus changes** is the one anti-pattern on the brief for which I found no direct published evidence. The nearest proxies are Coverage Illusion's synthetic-vs-real distribution mismatch (query-side drift, well evidenced) and the under-estimated synchronisation cost claim (corpus-side, weakly evidenced). If the series has measured corpus-drift effects on a rule set, that is likely original.
- **Maintenance burden of keyword lists** is asserted everywhere and measured nowhere. No source I found quantifies the cost of maintaining routing rules, the rate at which rules go stale, or how many rules in a deployed set are dead. The series' per-rule precision table *is* a maintenance instrument: a rule at 0% precision is a dead rule, and that is a measurable, publishable maintenance finding rather than a complaint.
- The GitHub-issue evidence for silent-fallback is thematically right but the repos are small and some look auto-generated. Use the Haystack `None`-evaluation caveat and the official LlamaIndex page's silence as the citable evidence instead; both are first-party.

### Gaps

- I found no corporate engineering post-mortem (the Shopify/Stripe/Notion-style write-up with graphs) about a RAG router failing in production. If one exists it is not well indexed.
- No quantified data on rule-list maintenance cost, rule staleness rate, or over-routing incidence.

---

## 6. Where keyword/regex routing is still defended

### Takeaway

There is a serious, consistent published case for lexical/deterministic routing, but it rests on **determinism, auditability, latency and cost** — never on measured accuracy. The consensus position across sources is hybrid: deterministic rules route, the model handles the residue, and the LLM is kept as a classifier rather than the verdict. Notably, the most rigorous study available found hand-written rules at near-random accuracy *and* found learned classifiers barely beating no router at all — which cuts both ways.

### Cited Findings

- **Reproducible logs are the core argument**: "Rules produce reproducible logs of what logic executed and why. LLM outputs require prompt logging, output logging, context capture, and additional review to explain why the system said what it said" [search-reported] [vendor blog] — [stablekernel: LLMs versus rules in enterprise voice ordering](https://stablekernel.com/blogs/llms-versus-rules-enterprise-voice-flows)
- **The tradeoff stated cleanly**: "rules win on speed, cost, determinism, and auditability; LLMs win on coverage, nuance, multilingual reach, and policy iteration speed" [search-reported] [vendor] — [scorable.ai: rule-based filters vs LLM moderation](https://scorable.ai/post/rule-based-filters-vs-llm-moderation)
- **Repeatability in regulated settings**: a security compliance paper selected a deterministic verifier for "repeatability and auditability properties" and "operational simplicity," observing that an LLM judge "adds a second non-deterministic decision layer," "complicating cross-run comparisons and the audit of the verdict itself" [search-reported] — [arXiv PDF 2605.05501 (SOCpilot)](https://arxiv.org/pdf/2605.05501)
- **The architectural pattern with the strongest published backing**: "Deterministic code routes, diff reviews catch the questionable ones, humans sample what's left. The LLM ends up as a classifier, never the verdict" [search-reported] [blog] — [DEV: an alternative to LLM quality gates](https://dev.to/zxpmail/an-alternative-to-llm-quality-gates-deterministic-routing-sampling-1ilf)
- **Deterministic rules + evidence + fail-closed abstention** as a named contract: a medical-billing verification paper proposes exactly this triple as a "decision-basis contract" for auditable LLM-assisted decisions [search-reported] — [arXiv PDF 2609.12156](https://arxiv.org/pdf/2609.12156); see also Policy-as-Skill, "Governed LLM Decision Support with Evidence, Deterministic Control, and Audit" — [arXiv PDF 2609.27087](https://arxiv.org/pdf/2609.27087)
- **A matched-comparison design worth copying**: AT-CXR (medical imaging triage) builds "a deterministic router over the same inputs and thresholds as an LLM-decided policy, so the two differ only in how actions are selected" [search-reported, results not read] — [arXiv PDF 2508.19322](https://arxiv.org/pdf/2508.19322)
- **Counter-evidence against rules, from the best study available**: hand-crafted rules at 38.4% accuracy vs a 20% random baseline, worse downstream than not routing at all (CO 4.202 vs 4.258) [fetched] — [arXiv HTML 2605.27220](https://arxiv.org/html/2605.27220v1)
- **But the same study is counter-evidence against replacing them with a model**: the best learned classifier gained **+0.011 CO** over Always-HyDE and captured at most **3.7% of the oracle gap**; TF-IDF char n-grams beat multilingual embeddings, which the authors attribute to "surface variation weakly tracking keyword matching against the index" [fetched]. A lexical feature space outperformed a semantic one on their routing task.

### Inferences

- **If the series concludes that repaired rules beat a model, that conclusion is well supported by the literature and under-argued in it.** Coverage Illusion is a paper in which lexical character n-grams beat embeddings, every learned router degenerated, and the honest finding was "don't route, cascade." The defence of rules exists but is argued on governance grounds; nobody has argued it on *measured accuracy* grounds with a repaired rule set as the treatment. That is an open position.
- The AT-CXR matched-design idea — same inputs, same thresholds, only the decision rule differs — is the cleanest experimental template available for a rules-vs-model comparison, and it maps directly onto the series' differential-proof habit (run the old implementation against the new one).
- The cascade result deserves attention as a *rival to routing itself*: Coverage Illusion's conclusion is not "route better" but "stop routing and escalate on empty results" — cheapest-first, escalate only when a step returns nothing. If the series does not address the cascade alternative, a knowledgeable reader will ask why.

### Gaps

- No source quantifies the auditability benefit (e.g. mean time to diagnose a misroute under rules vs under a model). The argument is universally qualitative.

---

## 7. Reporting conventions worth adopting

### Takeaway

**There is no checklist, template or convention for documenting a routing component.** I found nothing resembling a "router card." The nearest usable material is (a) contact-centre intent-classification practice, (b) the "decision-basis contract" framing from regulated-domain papers, and (c) Coverage Illusion's reporting shape. A reusable template is a genuine gap the series could fill.

### Cited Findings

- **No router card exists.** RAG evaluation surveys decompose the system into retrieval and generation and stop there. One survey "breaks RAG evaluation into internal components and their interactions," framing the work as "two core problems" — retrieval quality and generation quality — [arXiv PDF 2504.14891](https://arxiv.org/pdf/2504.14891). A systematic review of **63 academic articles** covers "datasets, retrievers, indexing and databases, and the generator" [search-reported] — [arXiv PDF 2504.20119](https://arxiv.org/pdf/2504.20119v1). **The router is not an evaluated component in either taxonomy.**
- Modular evaluation frameworks are component-swappable but router-blind: SCARF "supports easy replacement of individual system components, such as testing various vector databases" — [arXiv PDF 2504.07803](https://arxiv.org/pdf/2504.07803v1); XRAG "evaluates advanced RAG modules across pre-retrieval, retrieval, post-retrieval, and generation stages" — [arXiv PDF 2412.15529](https://arxiv.org/pdf/2412.15529v3.pdf). Routing sits in "pre-retrieval" and is not broken out.
- Ragas is described as measuring "the RAG pipeline at different component levels," for systems "built from retrieval and generation components" [search-reported] [vendor] — [EvalScope: RAG evaluation survey](https://evalscope.readthedocs.io/en/latest/blog/RAG/RAG_Evaluation.html)
- LlamaIndex's own evaluators do not model routing: `FaithfulnessEvaluator` and `RelevancyEvaluator` "do not understand router decisions" [search-reported] [vendor] — [FutureAGI](https://futureagi.com/blog/evaluating-llamaindex-rag-2026/)
- **Nearest thing to a convention, from contact-centre practice** [search-reported] [vendor]: per-intent precision, recall, support and F1 on a production-distribution set; the confusion matrix as the primary artifact, not a derived one; confusion *pairs* ranked by business consequence; calibration checked separately from accuracy; offline per-intent numbers paired with online outcomes (containment, transfer rate) — [FutureAGI](https://futureagi.com/blog/intent-classification-evaluation-pipeline-2026/); [Teammates.ai](https://teammates.ai/blog/intention-detection-that-routes-customers-to-the-right-outcome)
- **"Decision-basis contract"** is the most quotable framing for auditable routing: deterministic rules + verbatim evidence + fail-closed abstention [search-reported] — [arXiv PDF 2609.12156](https://arxiv.org/pdf/2609.12156)
- **Abstention as a first-class documented outcome**, with a defined fallback covering abstain / unknown / error / timeout, is proposed in issue threads rather than in any documentation standard [search-reported, low reliability] — [ghost issue #77](https://github.com/ferdousbhai/ghost/issues/77)
- **Coverage Illusion's de facto reporting template** [fetched], which is the best published example to point at: candidate route set enumerated; rule provenance stated (query length, question marks, Danish question words); accuracy against an explicit random baseline (38.4% vs 20%); per-class recall including zero-recall classes; routing *distribution* on unseen real queries (>90% to one route) as a degeneracy check; downstream quality against both a trivial always-one-route baseline and an oracle ceiling; fraction of oracle gap captured — [arXiv HTML](https://arxiv.org/html/2605.27220v1)

### Inferences

- A **router card** the series could define and reuse per episode, synthesised from the above, would plausibly be the most durable artefact of the episode: (1) what the router decides and the enumerated candidate set; (2) rule provenance — who wrote each rule and why; (3) per-rule precision and support on a labelled set, with a confusion matrix; (4) accuracy against an explicit random/majority baseline, so the reader can see "barely better than chance"; (5) the routing *distribution* on held-out real questions, as the degeneracy check; (6) abstention rate and what happens on abstention; (7) the failure mode when wrong — silent empty result, wrong corpus, or degraded answer; (8) downstream quality against both always-one-route and oracle.
- Items (4) and (5) are the two the existing practitioner literature never asks for, and both are cheap to compute. A router at 41% precision that sends 90% of traffic to one collection is a different animal from a router at 41% precision that spreads traffic evenly, and no framework or course teaches a reader to check.
- Importing the contact-centre convention wholesale is defensible and should be credited rather than presented as new.

### Gaps

- No standards body, no framework, and no book I could reach publishes a router documentation template. I searched for this directly and found nothing; the absence looks genuine rather than an indexing artefact.

---

## 8. The pedagogical gap: what would a rigorous treatment add?

### Takeaway

The canon teaches routing as **plumbing** — a taxonomy of three mechanisms plus a code sample — and never as a **classifier with a measurable error rate**. The specific, defensible gap is **per-rule precision on a hand-written rule set, with its support, its abstention behaviour, and its distribution over held-out real questions**. One 2026 academic paper has published adjacent numbers (accuracy and per-class recall, including a zero-recall class), so the series should position itself as corroborating and extending that, not as first.

### Cited Findings

- Frameworks define the mechanism and omit measurement entirely: the LlamaIndex router page's full conceptual content is that routers are "simple but powerful modules that use LLMs for decision making capabilities," with no evaluation, confidence, abstention or failure-mode discussion [fetched] — [LlamaIndex](https://developers.llamaindex.ai/python/framework/module_guides/querying/router/)
- LangChain's page is a mechanism menu — recommended `RunnableLambda` vs legacy `RunnableBranch`, plus embedding-similarity routing — with no evaluation section [search-reported] [vendor] — [LangChain](https://python.langchain.com/docs/how_to/routing)
- Haystack's `ConditionalRouter` idiom is "a specific condition plus a catch-all `{{ True }}`" — i.e. the framework's own default teaches a router that **cannot abstain**, only default [search-reported] [vendor] — [Haystack](https://docs.haystack.deepset.ai/docs/conditionalrouter)
- The DeepLearning.AI Haystack course teaches routing as a fallback-to-web-search pattern, with no measurement of the branch condition [search-reported] — [DeepLearning.AI](https://www.deeplearning.ai/short-courses/building-ai-applications-with-haystack)
- RAG evaluation surveys (63-article systematic review; the comprehensive survey; XRAG; SCARF; Ragas) decompose RAG into retrieval and generation and do not treat the router as an evaluated component [search-reported / titles verified] — [2504.14891](https://arxiv.org/pdf/2504.14891); [2504.20119](https://arxiv.org/pdf/2504.20119v1); [2412.15529](https://arxiv.org/pdf/2412.15529v3.pdf)
- Mature router benchmarks exist but are for **model** routing and use tradeoff metrics (APGR, CPT), not classification metrics — [RouteLLM](https://arxiv.org/html/2406.18665v2); [RouterBench](https://arxiv.org/abs/2403.12031)
- The one close counterexample publishes accuracy and per-class recall for a production retrieval router, including two zero-recall rules, and **still reports no precision or F1 for any router** [fetched] — [Coverage Illusion](https://arxiv.org/html/2605.27220v1)
- The methodology the series needs already exists, fully formed, in contact-centre intent classification — per-class precision/recall, confusion matrix as primary artifact, consequence-ranked confusion pairs, calibration — and has not crossed into RAG routing pedagogy [search-reported] [vendor] — [FutureAGI](https://futureagi.com/blog/intent-classification-evaluation-pipeline-2026/); [Teammates.ai](https://teammates.ai/blog/intention-detection-that-routes-customers-to-the-right-outcome)

### Inferences — what a rigorous treatment would add, concretely

1. **Treat the router as a classifier and name its metric.** The canon's framing is "a branch in a pipeline"; the reframe is "a multi-class classifier with per-class precision, recall and support, gating everything downstream." Nothing in the framework documentation prompts a reader to make this move.
2. **Per-rule precision, not system accuracy.** This is the specific novelty. Academic routers are scored as whole systems (accuracy, macro-F1); the series scores *individual hand-written rules* ("when this rule fires, how often is the route right?"), which is the unit a maintainer acts on — a 0%-precision rule is a deletable rule. I found no published treatment at this granularity.
3. **The random/majority baseline, stated.** Coverage Illusion's "38.4% vs a 20% uniform baseline for five classes" is the move that converts a number into a verdict. A 41% precision figure means nothing until the reader knows the base rate.
4. **The routing distribution as a mandatory check.** Report where traffic actually goes on held-out real questions. Coverage Illusion caught ">90% to one route" and a neural router that used two of five routes; neither would be visible from an accuracy figure. No framework or course teaches this check.
5. **Abstention as a documented outcome with a named failure mode.** Haystack's catch-all `{{True}}` and LlamaIndex's always-pick-the-top-choice selector both make "no route matched" structurally unrepresentable. Documenting what the router does when it abstains — and showing that empty retrieval is a *silent* error, per the retrieval-state lock-in finding — is absent from the canon.
6. **A matched rules-vs-model comparison, AT-CXR style**: same inputs, same thresholds, only the decision rule differs. This is the only design that can support a "repaired rules beat the model" claim, and it is also exactly the differential-proof discipline the series already uses.
7. **Precise vocabulary separating model routing from retrieval routing**, plus the diagnosis of *why* the retrieval side is under-measured: model routing has a reusable offline oracle (pre-generate every model's answer once) and a dollar-denominated payoff; retrieval routing has neither, so every evaluation must be built per-deployment. I did not find this explanation stated anywhere, and it is the honest answer to "why has nobody published these numbers?"
8. **A reusable router card** (the eight items in §7) — no such template exists in any framework, book, course or survey I could reach.
9. **Engage the cascade alternative.** Coverage Illusion's conclusion was not "route better" but "don't route — run cheapest-first and escalate when a step returns no documents" (+0.140 CO, −31.8% latency, 72.2% of queries served without LLM augmentation). A treatment that only repairs its router without addressing why not to have one will look incomplete to a reader who knows this paper.

### Inferences — where the premise should be tempered

- **"Numbers unusual to see published at all" is nearly right, not right.** Hussain & Nielbo published a production rule-based router at near-random accuracy with two zero-recall classes in May 2026. Claiming primacy would be falsifiable by a single search. Claiming *corroboration plus a finer granularity (per-rule precision) plus the maintenance framing* is both true and more useful.
- **The canon is not uniformly silent.** `semantic-router` ships `evaluate()`; LlamaIndex exposes `selector_result` with a reason string; RouteLLM and RouterBench are rigorous about what they measure. The gap is specifically in **retrieval routing within practitioner-facing material**, and it is a gap in *evaluation culture*, not in anyone's capability.
- **The series' numbers are not unusually bad.** 21%/25%/0% per-rule precision and 41% on commitment sit squarely alongside 38.4% rule accuracy and zero-recall classes in a published production system. The interesting claim is not "my rules were terrible"; it is "hand-written routing rules land near chance reliably enough that two independent systems found it, and nobody in the practitioner literature measures it."
- **Honest counterweight to the likely conclusion**: the best available evidence also shows learned routers barely beating no router at all (+0.011 CO; 3.7% of the oracle gap). So "repaired rules beat a model" may well replicate — but it may replicate for the uninteresting reason that *nothing* routes well on that kind of task, which would be worth saying.

### Gaps

- **Books are the largest unverified area.** No confirmation either way on routing coverage in Huyen's *AI Engineering* or Mendelevitch & Bao's *Hands-On RAG for Production* (both paywalled; one fetch returned 403). Before publishing a claim about "the books," check those two tables of contents and indexes directly.
- **No conference-talk evidence.** I found no indexed 2024–2026 talk presenting router metrics, but talk content is poorly searchable and this should not be asserted as an absence.
- **Three figures above are search-summarised from unread PDFs** (RAGRoute's 83.05–90.93% accuracy / 82.92–89.70% recall; ORDER's weighted-average 0.80 precision/recall; RAGRouter-Bench baseline's 93.2% / 0.928 macro-F1). RAGRoute's and ORDER's abstract pages, which I did read, show **no** classifier metrics. Verify in the PDFs before citing — if RAGRoute does report per-source precision/recall/F1/AUC, it is the strongest existing counterexample to the central claim and must be cited.
