# Has explicit intent routing been superseded by adaptive and agentic RAG? (as of October 2026)

Scope note for the report writer: every number below is attributed. Where a figure comes from a
vendor blog, a secondary summary, or a paper whose authors propose a competing method, it is marked
as such inline. Several promising 2026 papers were only reachable as compressed PDFs and are listed
in Gaps rather than guessed at.

Two framings recur and should be kept distinct throughout the report:
1. **Adaptive-RAG-family methods mostly sell cost reduction, not quality gain.** Their own tables
   usually show the always-expensive baseline winning on quality.
2. **The decision point matters more than the sophistication of the decider.** The strongest
   measured results in 2025-2026 are post-retrieval and cheap; the weakest are pre-retrieval and
   query-only, regardless of whether the router is regex, a learned classifier, or an LLM.

## The adaptive-RAG family: decisions, costs, measured gains, and training requirements

### Takeaway
Of the seven canonical methods, only three are usable without training (FLARE, DRAGIN, Self-Route),
and the two most-cited trained ones (Self-RAG, Adaptive-RAG) report **lower quality than their own
always-expensive baseline** — their contribution is step/latency reduction. Adaptive-RAG's own Table
2 shows its gains are entirely in multi-hop and slightly *negative* on single-hop, which is the
decisive fact for a single-hop-heavy specification corpus.

### Cited Findings

**Adaptive-RAG (Jeong et al., NAACL 2024; arXiv 2403.14403) — pre-retrieval, trained classifier**
- Decision: route each query to *no retrieval* / *single-step retrieval* / *multi-step retrieval*. Made **before** retrieval, from the query text alone — [arXiv HTML](https://arxiv.org/html/2403.14403v1)
- Decider: **T5-Large, 770M parameters**, cross-entropy, lr 3e-5 — [arXiv HTML](https://arxiv.org/html/2403.14403v1)
- Training data: silver labels built by **sampling and annotating 400 queries from each of six datasets**; label A if the no-retrieval model answers correctly, B if only single+multi-step succeed, otherwise B for single-hop datasets and C for multi-hop datasets (i.e. labels are partly *dataset identity*, not query content) — [arXiv HTML](https://arxiv.org/html/2403.14403v1)
- Classifier accuracy (Table 4): **54.52% overall** — 30.52% on the "no retrieval" class, 66.28% on "one step", 65.45% on "multi" — [arXiv HTML](https://arxiv.org/html/2403.14403v1)
- Averages across six datasets, FLAN-T5-XL (3B), with steps and time relative to single-step=1.00:
  | Method | EM | F1 | Acc | Steps | Time |
  |---|---|---|---|---|---|
  | No Retrieval | 14.87 | 21.12 | 15.97 | 0.00 | 0.11 |
  | Single-step | 34.83 | 44.31 | 38.87 | 1.00 | 1.00 |
  | Adaptive Retrieval | 23.87 | 32.24 | 26.73 | 0.50 | 0.56 |
  | Self-RAG* | 9.90 | 20.79 | 31.57 | 0.72 | 0.43 |
  | **Adaptive-RAG** | **37.17** | **46.94** | **42.10** | **2.17** | **3.60** |
  | Multi-step (always) | 39.00 | 48.85 | 43.70 | 4.69 | 8.81 |
  | Adaptive-RAG w/ Oracle | 45.00 | 56.28 | 49.90 | 1.28 | 2.11 |
  — [arXiv HTML](https://arxiv.org/html/2403.14403v1)
- So: Adaptive-RAG is **+2.63 F1 over always-single-step at 3.6x the time**, and **−1.91 F1 below always-multi-step at 0.41x the time**. The oracle router would score 56.28 F1 at 1.28 steps, so **9.34 F1 is lost to routing error** — [arXiv HTML](https://arxiv.org/html/2403.14403v1)
- Single-hop results (F1, Adaptive-RAG vs always-single-step): SQuAD **38.30 vs 39.30**, TriviaQA **60.70 vs 62.40**, Natural Questions **47.30 vs 47.30** (identical — the router sent everything to single-step, 1.00 steps). Adaptive-RAG is never better than single-step on a single-hop dataset — [arXiv HTML](https://arxiv.org/html/2403.14403v1)
- Multi-hop results (F1): MuSiQue **31.80 vs 22.80**, HotpotQA **53.82 vs 46.15**, 2WikiMultiHopQA **49.75 vs 47.90**, at 2.63-3.55 steps — [arXiv HTML](https://arxiv.org/html/2403.14403v1)
- Classifier size barely matters: accuracy changes little from T5-Small to T5-Large (per an alphaXiv summary of the paper — secondary) — [alphaXiv](https://www.alphaxiv.org/abs/2403.14403)

**Self-RAG (Asai et al., arXiv 2310.11511) — interleaved, requires fine-tuning the generator**
- Decision: emit "reflection tokens" to decide *retrieve or not*, then critique each retrieved passage (relevance) and its own output (support, usefulness). Decisions happen inside generation — [arXiv abs](https://arxiv.org/abs/2310.11511)
- Requires training **both** a critic and the generator. Critic data is distilled from GPT-4 by sampling **4k-20k examples per reflection-token type**; the authors say they distil rather than call GPT-4 at inference because proprietary-model dependence "raises API costs and hurts reproducibility" — [arXiv PDF](https://arxiv.org/pdf/2310.11511) (via search summary of the paper's own text)
- The generator is trained on **over 150K instruction-following instances** with retrieved passages and reflection tokens (figure as restated by a 2026 paper citing Self-RAG) — [arXiv 2604.22261](https://arxiv.org/pdf/2604.22261)
- Critic quality: the distilled Llama2-7B critic reaches **over 90% agreement with GPT-4** for most token categories (secondary: Medium paper review) — [paper review](https://artgor.medium.com/paper-review-self-rag-learning-to-retrieve-generate-and-critique-through-self-reflection-3ae3ccac3c4e)
- **Critical negative result for anyone without the fine-tune:** CRAG's authors reproduced Self-RAG on a plain `LLaMA2-hf-7b` (not the Self-RAG-trained checkpoint) and got PopQA **29.0**, Bio **32.2**, PubHealth **0.7**, ARC **23.9** — versus plain RAG on the same base model at **50.5 / 44.9 / 48.9 / 43.4**. The mechanism does not transfer to a model that was not trained for it — [CRAG arXiv HTML](https://arxiv.org/html/2401.15884v3)
- Independently, Adaptive-RAG's table shows Self-RAG* averaging **20.79 F1 vs 44.31** for plain single-step retrieval in their harness (different base model, so treat as a cross-setup comparison, not a clean head-to-head) — [arXiv HTML](https://arxiv.org/html/2403.14403v1)

**CRAG / Self-CRAG (arXiv 2401.15884) — post-retrieval, trained evaluator + web search**
- Decision: a retrieval evaluator grades retrieved documents into Correct / Incorrect / Ambiguous, then *decomposes-and-refines* the correct ones or *escalates to web search* — made **after** retrieval — [arXiv HTML](https://arxiv.org/html/2401.15884v3)
- Decider: **T5-large (~0.77B), fine-tuned**. Trained on PopQA: gold Wikipedia titles give positive labels, negatives sampled from retrieval results; of ~14k PopQA samples, 1,399 held out for test and the rest used for fine-tuning — [arXiv HTML](https://arxiv.org/html/2401.15884v3)
- The decision thresholds are **hand-tuned per dataset**: (0.59, −0.99) for PopQA, (0.5, −0.91) for PubHealth and ARC, (0.95, −0.91) for Biography — [arXiv HTML](https://arxiv.org/html/2401.15884v3)
- Accuracy (Bio is FactScore), generator = SelfRAG-LLaMA2-7b: RAG 52.8 / 59.2 / 39.0 / 53.2; **CRAG 59.8 / 74.1 / 75.6 / 68.6**; Self-RAG 54.9 / 81.2 / 72.4 / 67.3; **Self-CRAG 61.8 / 86.2 / 74.8 / 67.2** (PopQA / Bio / PubHealth / ARC) — [arXiv HTML](https://arxiv.org/html/2401.15884v3)
- Latency per instance on PopQA (Table 6): RAG **0.363s**, CRAG **0.512s** (1.41x), Self-RAG **0.741s** (2.04x), Self-CRAG **0.908s** (2.50x). TFLOPs/token: RAG 26.5, CRAG 27.2, Self-RAG 26.5-132.4, Self-CRAG 27.2-80.2. The paper says these exclude retrieval and are rough — [arXiv HTML](https://arxiv.org/html/2401.15884v3)
- CRAG depends on **Google Search API** for the Incorrect/Ambiguous branches and on **GPT-3.5-Turbo** to rewrite questions into keyword queries. Removing the rewriting step drops CRAG from 59.8 to 56.2 on PopQA — [arXiv HTML](https://arxiv.org/html/2401.15884v3)
- Adding web results to plain RAG without the self-correction logic gives only 50.5 → 52.2, so the authors attribute most of the gain to the correction mechanism rather than the extra text — [arXiv HTML](https://arxiv.org/html/2401.15884v3)

**Self-Route (Li et al., EMNLP 2024 Industry; arXiv 2407.16833) — post-retrieval, prompt-only, no training**
- Decision: send query + retrieved chunks to the LLM, which may answer or reply "unanswerable"; only the declined queries escalate to a full long-context call — [arXiv PDF](https://arxiv.org/pdf/2407.16833); [ACL Anthology](https://aclanthology.org/2024.emnlp-industry.66/)
- Cost: GPT-4o uses **61% of long-context's tokens**; Gemini-1.5-Pro **38.6%** (per a review of the paper). A second review states "up to a 65% reduction"; a third source gives 65%/39% and conflicts with the first on which model is which — treat the exact split as uncertain, the direction (large saving) as consistent — [Liner review](https://liner.com/review/retrieval-augmented-generation-or-longcontext-llms-comprehensive-study-and-hybrid); [IntuitionLabs](https://intuitionlabs.ai/articles/long-context-ai-vs-retrieval-rag)
- Quality: the paper reports long-context **consistently outperforms RAG**, and Self-Route reaches "performance comparable to LC using much less tokens" — again a cost result, not a quality win — [arXiv PDF](https://arxiv.org/pdf/2407.16833)

**FLARE (arXiv 2305.06983) and DRAGIN (ACL 2024; arXiv 2403.10081) — training-free, decide *when* mid-generation**
- FLARE triggers retrieval when the model generates low-probability tokens; DRAGIN uses token-level entropy plus attention to decide both *when* to retrieve and *what* to query with. Both are training-free — [DRAGIN HTML](https://arxiv.org/html/2403.10081); [ACL version](https://aclanthology.org/2024.acl-long.702.pdf)
- Retrieval-call overhead: in DRAGIN's own tables FLARE "requires the fewest retrieval calls across all datasets", DRAGIN next, both below fixed-interval baselines — [DRAGIN HTML](https://arxiv.org/html/2403.10081)
- A competing method reports average retrieval counts of **1.59 (FLARE)** and **3.31 (DRAGIN)** vs 1.29 for its own — [ETC, arXiv 2511.09980](https://arxiv.org/pdf/2511.09980)
- Another competing method reports FLARE and DRAGIN doing **2.67x and 6.83x more retrieval calls** than theirs, and that **DRAGIN runs multi-step retrieval in 98.8% of cases** — i.e. its "adaptive" decision is nearly always "yes" — [Probing-RAG, arXiv 2410.13339](https://arxiv.org/pdf/2410.13339)
- Quality: DRAGIN beats FLARE on IIRC EM **0.187 vs 0.128** (LLaMA2-13B-Chat) — [DRAGIN HTML](https://arxiv.org/html/2403.10081); and on 2WikiMultihopQA **0.220 vs 0.143** and HotpotQA **0.232 vs 0.149** per a third paper's table — [ETC](https://arxiv.org/pdf/2511.09980)
- Caveat flagged by the search itself: all three overhead comparisons come from papers proposing rivals, so they may favour their own method — and no source reported wall-clock latency or token cost for FLARE/DRAGIN.

**2025-2026 successors**
- **RASER** (arXiv 2606.02488, June 2026, under review) — the closest published analogue to a cheapest-first cascade. RASER-2 decides *stop or escalate* after a one-shot RAG pass; RASER-3 chooses among one-shot / PRUNE / IRCoT with an explicit cost-accuracy term. **Zero router-side LLM tokens.** Numbers in the next section — [arXiv abs](https://arxiv.org/abs/2606.02488); [arXiv HTML](https://arxiv.org/html/2606.02488v1)
- **ACC-RAG** (EMNLP 2025 Findings) concedes its selector "incurs additional computational overhead during inference" and argues the trade is worth it — [ACL Anthology](https://aclanthology.org/2025.findings-emnlp.1307.pdf)
- An ACL 2025 Findings paper on accelerating adaptive RAG states plainly that "A-RAG exacerbates the pre-existing efficiency challenges inherent in RAG" — [ACL Anthology](https://aclanthology.org/2025.findings-acl.1384.pdf)
- **Tiny-Critic RAG** (arXiv 2603.00846) deploys a LoRA-tuned small language model as the pass/fail critic with constrained decoding to two tokens, claiming "routing accuracy comparable to GPT-4o-mini while reducing latency by an order of magnitude". The abstract gives no model sizes, dataset names, or numbers — [arXiv abs](https://arxiv.org/abs/2603.00846)
- **Route Before Retrieve** (arXiv 2605.10235) builds a cheaper RAG-vs-long-context router and reports cost "about one-fifth that of Self-Route" with a Qwen3-1.7B router (abstract-level only; see Gaps) — [arXiv PDF](https://arxiv.org/pdf/2605.10235)

### Inferences
- Judged by the papers' own tables, **none of Self-RAG, Adaptive-RAG, or Self-Route beats its always-expensive baseline on quality.** All three are Pareto moves on the cost axis. If the series' ratchet gates on retrieval quality rather than cost, these methods do not clear it by construction.
- Adaptive-RAG's 54.52% classifier accuracy and 30.52% recall on the "no retrieval" class are in the same range as the series' measured 21-41% regex precision. The lesson is not "regex is uniquely bad" but "query-only classification of this kind is hard for everyone, including a 770M fine-tuned T5".
- CRAG's per-dataset hand-tuned thresholds are exactly the "tuned numbers" the series says it avoids. They also do not generalise: the evaluator is trained on PopQA and applied unchanged elsewhere, with thresholds re-tuned per target dataset.
- FLARE and DRAGIN are the only genuinely prompt-/training-free members, but both need token-level probabilities (and DRAGIN needs attention weights). That is an inference, not a cited claim, but it is an engineering blocker worth checking against the target stack before planning an episode around either.

### Gaps
- No single credible table compares all seven methods under one backbone and one dataset. A 2025 survey (arXiv 2506.00054) does tabulate Self-RAG, DRAGIN, and CRAG/Self-CRAG rows on LLaMA2-7B/13B, but a referee-style review of it says the central tables contain "transcription, labeling, and convention errors" and that the appendix raw scores are the trustworthy part — do not cite that survey's tables without checking the originals — [survey](https://arxiv.org/html/2506.00054v1); [review](https://pith.science/paper/2506.00054)
- Self-RAG's exact critic-training instance total and the GPT-4 labelling spend were not found in any primary source.
- No absolute latency or token-cost figures for FLARE or DRAGIN were found anywhere.

## Agentic RAG: what it denotes, and what is actually measured

### Takeaway
The 2026 systematisation paper defines agentic RAG by the presence of an autonomous control policy
and contains **no measured experiments at all**; its cost claims are qualitative. The two 2026 papers
that do measure it both find that a **well-built single-pass or fixed "enhanced" pipeline matches or
beats the agentic loop** on retrieval quality, at 1.5-3.3x less token cost — including on a local 7B
model.

### Cited Findings

**What the term denotes**
- The 2026 SoK defines agentic RAG as a finite-horizon POMDP and states it "is not defined by the presence of a retriever, but by the presence of an autonomous control policy" over a discrete action space, distinguishing it from Active RAG, which is "fundamentally a single-pass generative process". Four necessary properties: iterative control, dynamic retrieval, tool-mediated interaction, state persistence — and the paper says these are "not sufficient to guarantee stability or safety" — [SoK, arXiv 2603.07379](https://arxiv.org/html/2603.07379v1)
- That SoK reports **no benchmark results, accuracy scores, or dataset-level comparisons**; Tables I-VII are conceptual or qualitative mappings. Its only cost statements are unquantified: self-verification "effectively doubles the generation latency"; multi-agent collaboration has the "highest token amplification profile"; tree-based/interleaved reasoning is "extremely expensive" — [SoK](https://arxiv.org/html/2603.07379v1)
- The SoK also says the field "lacks a systematic understanding" of agentic RAG as a sequential decision system, that standardised evaluation frameworks are absent, that LLM self-critique has "evaluation blind spots", and that **tool-routing reliability is "a first-class failure point"** — [SoK](https://arxiv.org/html/2603.07379v1)
- The 2025 agentic RAG survey (arXiv 2501.09136) reports agentic RAG's strongest gains in "domains with structured knowledge and explicit constraints"; no cost or latency figures were found in it — [arXiv](https://arxiv.org/pdf/2501.09136)

**"Is Agentic RAG worth it?" (arXiv 2601.07711, v2) — the cleanest controlled comparison found**
- Compares **Naive RAG**, **Enhanced RAG** (fixed pipeline: explicit routing + HyDE rewriting + cross-encoder reranking), and **Agentic RAG** (an LLM orchestrator deciding whether to retrieve, how to rewrite, and whether to re-retrieve; single RAG tool, PocketFlow). Datasets NQ, FiQA, FEVER, CQADupStack-English; embedder text-embedding-3-small; judge Selene-70B — [arXiv HTML](https://arxiv.org/html/2601.07711v2)
- **Document-list quality (NDCG@10, average):** Naive **45.5**, Enhanced without rewriting **48.0**, Enhanced with rewriting **49.5**, Agentic **43.9**. The agent's repeated retrieval "gave no benefit": it re-retrieved only **~10% of the time**, and when it did, **53% of the documents were unchanged** — [arXiv HTML](https://arxiv.org/html/2601.07711v2)
- **Query rewriting (NDCG@10, average):** Naive **50.3**, Enhanced **52.8**, Agentic **55.6** — the one axis where the agent wins; biggest gain on NQ (51.7 vs 43.9) and roughly even on FiQA — [arXiv HTML](https://arxiv.org/html/2601.07711v2)
- **Cost:** Agentic RAG used **~2.7-3.6x more input tokens** (avg 3.3x), **~1.7-1.8x more output tokens** (avg 1.9x), and **~1.5x longer per query** — [arXiv HTML](https://arxiv.org/html/2601.07711v2)
- **Scaling negative:** the authors had to **exclude NQ and FEVER from some tests because their knowledge bases were too large for Agentic RAG to run in reasonable time** — [arXiv HTML](https://arxiv.org/html/2601.07711v2)
- Conclusion: neither approach is universally superior; "a well-optimized Enhanced pipeline can match or beat Agentic RAG at lower cost", and the best move may be adding an explicit reranking step to agentic pipelines — [arXiv HTML](https://arxiv.org/html/2601.07711v2)

**"Dissecting Agentic RAG: A Component Ablation for Multi-Hop QA with a Local 7B Model" (arXiv 2606.21553)**
- Setup: **Qwen2.5-7B-Instruct served locally via Ollama** on an RTX A6000 (49GB); 5,000-question fixed random sample of HotpotQA distractor dev — [arXiv HTML](https://arxiv.org/html/2606.21553v1)
- | Variant | EM | F1 | Latency (ms) |
  |---|---|---|---|
  | Single-pass baseline | 43.1 | 54.0 | (printed as "0,546") |
  | Agentic full, adaptive routing | 53.2 | 61.6 | 5,642 |
  | **Hybrid-only (fixed RRF, no routing)** | **55.0** | **63.5** | 5,688 |
  | no-decomp | 51.9 | 60.1 | 2,546 |
  | no-reranker | 51.5 | 59.4 | 5,648 |
  | steps-1 | 46.1 | 53.9 | 5,426 |
  | steps-2 | 52.9 | 61.3 | 5,628 |
  | steps-3 | 53.2 | 61.6 | 5,643 |
  | dense-only | 53.0 | 61.9 | 5,623 |
  | sparse-only | 53.0 | 61.2 | 5,681 |
  — [arXiv HTML](https://arxiv.org/html/2606.21553v1)
- Full agentic pipeline beats single-pass by **+10.1 EM / +7.6 F1**, but **fixed hybrid retrieval beats the adaptive router by +1.8 EM / +1.9 F1 (p<0.001)** — [arXiv HTML](https://arxiv.org/html/2606.21553v1)
- Authors state the result **"contradicts the motivation for adaptive routing"**, and that fixed, simpler choices are "competitive with or better than" adaptive ones — [arXiv HTML](https://arxiv.org/html/2606.21553v1)
- Cause: the rule-based routing heuristic fires on named entities, dates, and numbers, which appear in nearly all multi-hop sub-questions, so **~72% of retrieval calls went to BM25**, discarding the complementary dense signal. Top-ranked evidence came from BM25 for 79.2% of questions, hybrid 16.3%, dense 4.5% — [arXiv HTML](https://arxiv.org/html/2606.21553v1)
- Per-strategy EM (BM25 53.9, hybrid 50.9, dense 50.7) is explicitly called "likely selection bias", since the heuristic routes simpler queries to BM25 — [arXiv HTML](https://arxiv.org/html/2606.21553v1)
- Dense-only (53.0), sparse-only (53.0), and adaptive (53.2) are near-identical, which the authors read as **loop depth mattering more than per-step strategy choice** — [arXiv HTML](https://arxiv.org/html/2606.21553v1)
- The paper reports **no token costs and no router accuracy** (the routing is rule-based; only selection frequencies are reported) — [arXiv HTML](https://arxiv.org/html/2606.21553v1)

**Contrary measured evidence**
- An underwriting study found "the Agentic RAG pipeline attains the highest accuracy under both base models it tested", beating a single-LLM baseline and naive RAG — narrow domain, single application — [arXiv 2607.07858](https://arxiv.org/pdf/2607.07858)
- A production-finance example cited in a vendor comparison shows retrieval accuracy **54.12% → 62.35%** with latency **0.79s → 5.02s** — [Taskade](https://www.taskade.com/blog/llm-rag-agent-agentic) (vendor blog; primary source not identified)

**Vendor claims — flag as unverified**
- Multiple vendor posts put agentic RAG at **3-10x the token cost and 2-5x the latency** of traditional RAG, one attributing this to a "March 2026 MarsDevs production benchmark" — [Sphere](https://www.sphereinc.com/blogs/agentic-rag-vs-traditional-rag-vs-chatgpt); [starmorph](https://blog.starmorph.com/blog/rag-techniques-compared-best-practices-guide)
- A "roughly 42% precision lift on complex queries" is attributed to Forrester, but the same source dates the report as both March and May 2026 — **do not use** — [Sphere](https://www.sphereinc.com/blogs/agentic-rag-vs-traditional-rag-vs-chatgpt)
- One vendor table claims a **~3,500% cost increase per query** and **151% latency rise** over 100 queries, with the agentic system winning 52% of comparisons vs 38% — [Taskade](https://www.taskade.com/blog/llm-rag-agent-agentic)
- A Towards Data Science piece advises using agentic RAG "only when there is clear evidence that single-pass retrieval fails for a significant portion of queries" — practitioner opinion, no experiment — [TDS](https://towardsdatascience.com/agentic-rag-vs-classic-rag-from-a-pipeline-to-a-control-loop/)

### Inferences
- The two 2026 papers that measure agentic loops properly both land on the same conclusion from opposite directions: the gain attributed to "agency" is mostly attributable to **more retrieval rounds** and **query rewriting**, not to the decision-making. Strip the agency and keep the rounds (fixed hybrid, fixed rewrite, fixed rerank) and quality is equal or better at a fraction of the cost.
- Agentic RAG as described in the SoK is a research programme with an acknowledged evaluation vacuum, not a settled technique that supersedes anything. "Agentic RAG has superseded routing" is not a claim the 2026 literature supports with numbers.
- The 2601.07711 KB-size exclusion is a direct warning for a large HTML specification archive: agentic loops over a big index may be impractical before they are inaccurate.

### Gaps
- A "RAGCap-Bench" result claiming "+33% average accuracy" for agentic approaches was cited by one vendor article without clear sourcing; the primary benchmark was not located.
- No independent reproduction of the MarsDevs or Forrester figures was found.

## Has routing been absorbed into tool calling?

### Takeaway
No — and the best measured evidence runs the other way. The one controlled comparison found shows an
explicit router beating an LLM-decides-its-own-retrieval agent by **+23.3 points of F1 and +35.1
points of recall** on one dataset, because the agent over-retrieved; the 2026 SoK independently names
tool-routing reliability a "first-class failure point". The surrounding reliability literature is
thin and largely non-peer-reviewed.

### Cited Findings
- **Explicit router vs agent-decides, same system, same data** (intent handling; recall / F1 over 500 valid + 500 invalid queries per dataset):
  | Dataset | Enhanced (explicit routing) | Agentic (LLM decides) |
  |---|---|---|
  | FiQA | 95.1 / 95.7 | 97.7 / 98.8 |
  | **FEVER** | **84.4 / 87.9** | **49.3 / 64.6** |
  | CQADupStack-EN | 94.7 / 96.6 | 100 / 99.8 |
  Naive baseline was 66.7 F1 on all three. The agent "did well in narrow domains but underperformed on FEVER, mainly because it over-retrieved" — [arXiv 2601.07711](https://arxiv.org/html/2601.07711v2)
- The same paper's upside for the agent: it "handles intent routing without hand-crafted examples" — i.e. it buys you out of maintaining a rule list, at 3.3x input tokens — [arXiv 2601.07711](https://arxiv.org/html/2601.07711v2)
- The 2026 SoK lists tool-routing reliability as "a first-class failure point" for agentic RAG and says LLM self-critique has "evaluation blind spots" — [SoK](https://arxiv.org/html/2603.07379v1)
- A router paper for agentic tool calling reports that using an LLM as the judge was "expensive, non-deterministic, and surprisingly unreliable on structured output", so the authors evaluated against structured comparison instead — [Switchcraft, arXiv 2605.07112](https://arxiv.org/pdf/2605.07112)
- **Unverified blog figures, widely repeated:** letting the LLM choose among all tools degrades with tool count — ~94% accuracy at 50 tools, **64% at 200 tools, 20% at 417 tools** — used to argue for an intent classifier that narrows the tool set first. No methodology given; treat as anecdote — [TianPan.co](https://tianpan.co/blog/2026/04/16/intent-classification-agent-routers)
- A confidence-gated hybrid study (different domain: emotion recognition) warns that cheap-classifier-plus-LLM-fallback helps only under specific conditions and is "not a general accuracy-boosting property", depending on the base classifier having a failure mode the LLM does not share — [arXiv 2609.17977](https://arxiv.org/pdf/2609.17977)
- Classifier routing overhead is small: ~15-45ms for a BERT-based router (RouteLLM), per vendor-style sources — [LogRocket](https://blog.logrocket.com/llm-routing-right-model-for-requests/)
- Practitioner consensus in the 2026 write-ups is a **cascade, not a replacement**: deterministic rules first, semantic routing for clear-cut cases, LLM routing only for ambiguous ones — [123ofai](https://123ofai.com/articles/blocks/agent-router); [LogRocket](https://blog.logrocket.com/llm-routing-right-model-for-requests/)

### Inferences
- The FEVER collapse is the most useful single datapoint in this whole research round for the blog's purposes: the failure mode of tool-call routing is *not* "it picks the wrong tool" but "it retrieves when it should not", which is precisely the failure an explicit, measured router is built to prevent and to *measure*.
- Tool calling absorbs the *plumbing* of routing (no hand-written rule list) but not the *accountability*: you cannot report a precision number for a decision the model makes implicitly and differently each run.

### Gaps
- **No controlled head-to-head study measuring tool-selection reliability of LLM function calling against an explicit classifier on the same labelled set** was found. This is a genuine hole in the 2026 literature, and worth stating as such in the report. BFCL / tau-bench with a routing baseline would be the place to look next.

## Query decomposition and planning as an alternative to routing

### Takeaway
Decomposition is worth real but modest points — and on a frontier model it can be **worse than a
single well-retrieved pass**. The 2026 numbers show decomposition baselines (Self-Ask, ChainRAG,
IRCoT) *losing* to one-shot RAG on MuSiQue with a 120B model while costing 3x the tokens, and
helping clearly only for mid-tier open models.

### Cited Findings
- **RASER's Table 3 (GPT-OSS-120B, F1 with mean tokens), MuSiQue / 2Wiki / HotpotQA:**
  | Method | MuSiQue | 2Wiki | HotpotQA |
  |---|---|---|---|
  | One-shot RAG | **0.488** (1.5k) | 0.754 (1.1k) | 0.777 (1.4k) |
  | Always-PRUNE | 0.528 (4.8k) | 0.761 (3.3k) | 0.769 (3.7k) |
  | IRCoT* (iterative) | 0.455 (4.3k) | 0.781 (3.0k) | 0.763 (3.6k) |
  | Self-Ask* (decomposition) | 0.482 (4.6k) | 0.743 (3.5k) | 0.724 (4.1k) |
  | ChainRAG† (decomposition) | 0.465 (4.6k) | 0.763 (3.6k) | 0.727 (3.6k) |
  | RASER-2 | 0.502 (2.1k) | 0.763 (1.3k) | 0.779 (1.5k) |
  | RASER-3 | 0.510 (2.2k) | 0.774 (1.6k) | 0.787 (1.8k) |
  Decomposition and iterative retrieval **lose to one-shot RAG on MuSiQue and HotpotQA** at ~3x the tokens — [arXiv HTML](https://arxiv.org/html/2606.02488v1)
- The same paper says IRCoT* "adds little for frontier LLMs and has real value for mid-tier open LLMs on MuSiQue and 2Wiki". Llama-3.1-8B on MuSiQue: always-IRCoT* **0.376** (5.1k tokens) vs RASER-3 0.330 (3.0k) vs always-PRUNE 0.264 (3.8k) — [arXiv HTML](https://arxiv.org/html/2606.02488v1)
- **Decomposition ablation on a local 7B** (HotpotQA, Qwen2.5-7B via Ollama): removing decomposition costs **1.3 EM / 1.5 F1** (51.9/60.1 vs 53.2/61.6) but **halves latency** (2,546ms vs 5,643ms). Removing the reranker costs more (51.5 EM) at no latency saving — [arXiv HTML](https://arxiv.org/html/2606.21553v1)
- In the same ablation, **loop depth** is the dominant factor: steps-1 **46.1 EM**, steps-2 **52.9**, steps-3 **53.2** — the second hop is worth +6.8 EM, the third +0.3 — [arXiv HTML](https://arxiv.org/html/2606.21553v1)
- Agentic query rewriting beat both fixed pipelines on retrieval NDCG@10 (55.6 vs 52.8 Enhanced/HyDE vs 50.3 naive) — the single clearest win for a generative pre-retrieval step in the 2026 comparison — [arXiv 2601.07711](https://arxiv.org/html/2601.07711v2)
- Known failure mode: a multi-hop system "can miss key entities during sub-question decomposition, which significantly degrades the retrieval performance" — [Lost-in-Retrieval, ACL 2025](https://aclanthology.org/2025.acl-long.1089.pdf)
- RASER's own sensitivity check: swapping the decomposition baseline (Self-Ask* for ChainRAG) changes GPT-OSS-120B F1 by at most 0.020 per dataset — the choice of decomposition method matters little — [arXiv HTML](https://arxiv.org/html/2606.02488v1)

### Inferences
- For a corpus where questions ask about two time points or need several documents, the evidence says the second retrieval round is where the points are (+6.8 EM from step 2 in the local-7B ablation), and that you can get those rounds from a **fixed** two-pass design rather than from a classifier or an agent deciding to take them.
- Decomposition and routing are not substitutes on the evidence: decomposition buys multi-document coverage, routing buys cost control. The 2026 numbers favour spending on the former and deriving the latter cheaply.

### Gaps
- No study found that directly pits "classify the compound question" against "decompose the compound question" on the same corpus with the same budget.

## Where adaptive methods are reported to FAIL or not pay off

### Takeaway
This is the best-evidenced section, and the answer is blunt: **in 2026 there are now multiple
measured results where adaptive/agentic retrieval gave no gain or hurt** — a fixed hybrid beating an
adaptive router on a local 7B, agent-decided retrieval collapsing from 87.9 to 64.6 F1 through
over-retrieval, agentic re-retrieval giving no NDCG benefit, and every pre-retrieval routing method
in a 240k-document production study failing to beat "always do the expensive thing".

### Cited Findings

**Adaptive routing beaten by a fixed choice**
- Fixed hybrid RRF beats the full adaptive pipeline by **+1.8 EM / +1.9 F1 (p<0.001)** on HotpotQA with a local Qwen2.5-7B; the authors say this "contradicts the motivation for adaptive routing" and that fixed, simpler choices are "competitive with or better than" adaptive ones — [arXiv 2606.21553](https://arxiv.org/html/2606.21553v1)

**The cost of the retrieve-or-not decision being wrong**
- "Adaptive Retrieval" (popularity-threshold baseline) halves retrieval calls (0.50 steps) and loses **12.07 F1** against always-single-step (32.24 vs 44.31 average) — the price of being wrong about "no retrieval needed" — [arXiv 2403.14403](https://arxiv.org/html/2403.14403v1)
- Adaptive-RAG's classifier gets the "no retrieval" class right only **30.52%** of the time; the oracle router would gain **+9.34 F1** over the real one — routing error is the single largest term in its error budget — [arXiv 2403.14403](https://arxiv.org/html/2403.14403v1)
- Over-retrieval is the measured failure direction for LLM-decided retrieval: FEVER intent F1 **87.9 → 64.6** — [arXiv 2601.07711](https://arxiv.org/html/2601.07711v2)
- DRAGIN's "adaptive" decision is multi-step **98.8% of the time** — an adaptive mechanism that has effectively collapsed to "always" — [Probing-RAG](https://arxiv.org/pdf/2410.13339)

**Adaptive/agentic steps that produced no measurable benefit**
- Agentic re-retrieval: document-list NDCG@10 **43.9** vs **49.5** for a fixed reranking pipeline; it re-retrieved ~10% of the time and 53% of documents were unchanged when it did — [arXiv 2601.07711](https://arxiv.org/html/2601.07711v2)
- Per-step strategy choice barely matters: dense-only 53.0, sparse-only 53.0, adaptive 53.2 EM — [arXiv 2606.21553](https://arxiv.org/html/2606.21553v1)
- Adding post-retrieval signals to a multimodal router gave a held-out gain of **+0.00288 with 95% CI [−0.00726, 0.01107]** — i.e. **not distinguishable from zero** (dev-split gain was +0.01621 [0.00436, 0.02960], so the dev result did not replicate). Per-modality final gains: Document +0.00290, Audio +0.00575, Video 0.00000 — [Beyond the Query, arXiv 2609.12437](https://arxiv.org/html/2609.12437v1)

**Gains concentrating in multi-hop and vanishing on single-hop**
- Adaptive-RAG single-hop F1 vs always-single-step: SQuAD 38.30 vs **39.30**, TriviaQA 60.70 vs **62.40**, NQ 47.30 vs 47.30. Multi-hop: MuSiQue 31.80 vs 22.80, HotpotQA 53.82 vs 46.15 — [arXiv 2403.14403](https://arxiv.org/html/2403.14403v1)
- Adaptive-RAG's own framing: multi-step retrieval "is powerful but largely inefficient for simple queries" — the method exists to avoid a cost, not to gain accuracy, on simple queries — [ACL Anthology](https://aclanthology.org/2024.naacl-long.389.pdf)
- RASER's recoverability analysis (Table 1), share of multi-hop questions by outcome:
  | Reader | One-shot already correct | No method helps | Small/unclear gain | Bridge retrieval helps |
  |---|---|---|---|---|
  | GPT-OSS-120B | 53% | 14% | 19% | 14% |
  | Gemma-3-31B | 48% | 19% | 13% | 20% |
  | Mistral-S-119B | 37% | 20% | 9% | 34% |
  | Phi-4-mini | 31% | 25% | 15% | 29% |
  | Llama-3.1-8B | 27% | 27% | 8% | 38% |
  | Llama-3-8B | 24% | 26% | 12% | 37% |
  **14-27% of questions are unrecoverable by any tested strategy**, and for a frontier reader only ~14% actually benefit from the extra step — so the headroom any router can address is small — [arXiv HTML](https://arxiv.org/html/2606.02488v1)
- RASER-2 escalates ~14% of questions; Appendix E says 9-16% of questions actually benefit from PRUNE — the addressable fraction and the escalation rate are both about one question in seven — [arXiv HTML](https://arxiv.org/html/2606.02488v1)

**Latency and token multipliers (measured, not vendor)**
- Agentic vs Enhanced: **3.3x input tokens, 1.9x output tokens, 1.5x wall-clock** — [arXiv 2601.07711](https://arxiv.org/html/2601.07711v2)
- Always-PRUNE uses **~3x** one-shot RAG's tokens; iterative IRCoT 3-4x — [arXiv 2606.02488](https://arxiv.org/html/2606.02488v1)
- Adaptive-RAG: **3.60x** the time of single-step on average — [arXiv 2403.14403](https://arxiv.org/html/2403.14403v1)
- CRAG **1.41x**, Self-RAG **2.04x**, Self-CRAG **2.50x** the per-instance latency of plain RAG — [arXiv 2401.15884](https://arxiv.org/html/2401.15884v3)
- Local 7B agentic loop: **5,642ms** per query vs a single-pass baseline; decomposition alone accounts for ~3,100ms of that — [arXiv 2606.21553](https://arxiv.org/html/2606.21553v1)

**Honest acknowledgements by the method authors themselves**
- ACC-RAG: its selector "incurs additional computational overhead during inference" — [ACL](https://aclanthology.org/2025.findings-emnlp.1307.pdf)
- "A-RAG exacerbates the pre-existing efficiency challenges inherent in RAG" — [ACL](https://aclanthology.org/2025.findings-acl.1384.pdf)
- Selective-retrieval approaches are "usually effective, but [have] a large overhead due to the need for multiple calls to the LM" (secondary, well-sourced practitioner survey) — [Sumit's Diary](https://blog.reachsumit.com/posts/2025/09/deciding-when-not-to-retrieve/)

### Inferences
- There is now enough measured negative evidence that an episode arguing "adaptive is not automatically better" is defensible on citations alone, and would not be contrarianism.
- The recurring pattern across 2606.21553, 2601.07711, 2609.12437 and 2605.27220 is **a dev-set or intuition-level gain that does not survive a matched control**. The series' ratchet is exactly the instrument that catches this class of error, which is a strong narrative hook.
- The addressable-headroom numbers (RASER: ~14% of multi-hop questions benefit; Coverage Illusion: at most 2.2% of the oracle gap reachable pre-retrieval) suggest the honest question is not "router or adaptive?" but "how large is the decision's headroom on *this* corpus?" — a measurable quantity, and a better episode than either architecture.

### Gaps
- I did not find a paper whose *stated purpose* is to report adaptive RAG failing; all the negative results above are ablations or secondary findings inside papers proposing something. Worth saying plainly in the report.
- "Insider Knowledge: How Much Can RAG Systems Gain from Evaluation Secrets?" (arXiv 2601.13227) appeared in search and sounds directly relevant to a no-tuning-on-eval rule, but I did not read it. Unverified lead.

## Does any of this work with a small local model?

### Takeaway
Yes for the *deciding*, no for the *critiquing*. The two strongest 2026 results both put the decision
in a **non-LLM classifier over cheap retrieval features** (RASER: gradient-boosted trees, **zero
router-side LLM tokens**) rather than in a small model's judgement; meanwhile the one paper that ran
a full agentic loop on a local 7B found its adaptive router losing to a fixed choice. Self-RAG-style
reflection is measurably unavailable without the fine-tune.

### Cited Findings
- **A 7B local decision-maker was measured and the adaptive component lost.** Qwen2.5-7B-Instruct via Ollama on an RTX A6000: fixed hybrid 55.0 EM beat adaptive routing 53.2 EM — [arXiv 2606.21553](https://arxiv.org/html/2606.21553v1)
- **RASER's router is not an LLM at all.** RASER-2 is a single gradient-boosted classifier (log loss, 100 trees, max depth 3, lr 0.1, subsample 0.8, scikit-learn), threshold θ=0.20, labelled 1 only when PRUNE beats one-shot RAG by more than τ=0.1 F1. RASER-3 is three gradient-boosted regressors with a cost-aware argmax f̂_r(s) − λ·c̄_r. **Router-side LLM tokens: zero.** — [arXiv HTML](https://arxiv.org/html/2606.02488v1)
- RASER's six features, all derived from one cheap pass: `confidence` (rule-based, from one-shot RAG and question type), `ans_len`, `bridge_cues` (multi-hop linguistic patterns), `score_gap` (top-1 minus top-5 dense score), `score_top1`, `qtype` (entity/date/yes-no/count/other). **Feature importance: score_gap 0.38, score_top1 0.35, confidence 0.12, ans_len 0.12, bridge_cues 0.02, qtype 0.01** — the two retrieval-score features carry 73% of the signal and the query-text features carry ~3% — [arXiv HTML](https://arxiv.org/html/2606.02488v1)
- RASER classifier-choice ablation: F1 ranges only **0.520-0.531** across six classifier families for RASER-2 and **0.546-0.562** for RASER-3; the deployed GBM is not even the best. The decider's sophistication is nearly irrelevant — [arXiv HTML](https://arxiv.org/html/2606.02488v1)
- RASER results with 8B-class local readers (F1 / mean tokens, MuSiQue): Llama-3.1-8B always-IRCoT* 0.376 (5.1k) vs RASER-3 0.330 (3.0k); Llama-3-8B always-IRCoT* 0.354 (4.7k) vs RASER-3 0.309 (2.8k). RASER-3 reaches **87-97% of always-IRCoT* F1 at 47-62% of its tokens** for mid-tier LLMs — [arXiv HTML](https://arxiv.org/html/2606.02488v1)
- **Small models rely on retrieved evidence far more than large ones** (200 edited-evidence MuSiQue questions; reader outputs the edited answer vs keeps the gold answer from memory): GPT-OSS-120B **41% edited / 38% gold**; Gemma-3-31B **52% / 21%**; Llama-3-8B **67% / 9%**. So retrieval-quality decisions matter *more* on a local 8B, and large-model benchmark F1 partly reflects memorisation — [arXiv HTML](https://arxiv.org/html/2606.02488v1)
- **Both agentic and fixed pipelines scale with model size at similar rates**, so agency does not substitute for capacity: Qwen3 0.6B→32B on FiQA, Enhanced **51.4 → 95.5**, Agentic **58.4 → 91.8** (Selene binary judge). The agentic system is ahead at 0.6B and behind at 32B — [arXiv 2601.07711](https://arxiv.org/html/2601.07711v2)
- **Self-RAG's reflection machinery does not work on an untrained base model:** plain LLaMA2-hf-7b with Self-RAG scored PopQA 29.0 / Bio 32.2 / PubHealth 0.7 / ARC 23.9, versus plain RAG at 50.5 / 44.9 / 48.9 / 43.4 — [arXiv 2401.15884](https://arxiv.org/html/2401.15884v3)
- Tiny-Critic RAG claims a LoRA-tuned SLM critic with constrained two-token decoding matches GPT-4o-mini routing accuracy at an order of magnitude lower latency — but publishes no sizes or numbers in the abstract, and **requires LoRA fine-tuning** — [arXiv abs](https://arxiv.org/abs/2603.00846)
- Adjacent (model routing, not retrieval routing): Qwen2.5-7B-Instruct-based routers reach **80-90% of GPT-5's routing accuracy at one-fifth the cost** — [xRouter, arXiv 2510.08439](https://arxiv.org/html/2510.08439v1); and BEST-Route reports "up to 60% cost reduction with less than 1% performance degradation", though it routes sample counts, not retrieval — [arXiv 2506.22716](https://arxiv.org/html/2506.22716v1)
- Retrieval-feature-based routers are reported robust: "robust to k, embedding size, data sparsity, retrieval degradation, and generalise to unseen queries and models with as little as 1% historical data" (ContextualRouter, as surfaced in search — primary not read) — see Gaps

### Inferences
- The RASER feature-importance table is the single most actionable finding for the series: **73% of the routing signal lives in two numbers the retriever already returns** (top-1 score and the top-1-to-top-5 gap), and the hand-written-pattern feature (`bridge_cues`) contributes **0.02**. That is a measured, quantitative statement of why the regex list is capped — and of what to replace it with, without an LLM and without a fine-tune.
- A small local model is a poor *critic* (CRAG/Self-RAG transfer failure, Tiny-Critic needs LoRA) but the decision does not need a model at all. "Deterministic decider over retrieval signals" is the local-friendly option that 2026 evidence actually supports.
- Because an 8B reader follows retrieved evidence 67% of the time versus 41% for a 120B, retrieval-quality work has *more* leverage on the series' local tier than the published frontier-model numbers suggest. Conversely, frontier-model adaptive-RAG gains are partly masked by memorisation and will not transfer downward unchanged.

### Gaps
- Tiny-Critic RAG's and Route Before Retrieve's full numbers are unverified (PDFs were unreadable; abstracts only). "Route Before Retrieve" reportedly uses a Qwen3-1.7B router at ~1/5 Self-Route's cost — worth a follow-up fetch of its HTML.
- ContextualRouter's primary source was not located; its robustness claim is from a search summary only.

## Determinism and regression-testing an adaptive pipeline

### Takeaway
There is **no literature on regression-gating an adaptive retrieval pipeline** — the one systematic
RAG-reproducibility study explicitly does not cover LLM generation, and the agentic SoK does not
discuss determinism at all beyond noting the policy is stochastic. What *is* measured is reassuring
for the deterministic side: indexes, ANN search, and distributed retrieval are bit-reproducible, so
all the non-determinism a series would be importing comes from the adaptive decision itself.

### Cited Findings
- **"On The Reproducibility Limitations of RAG Systems" (arXiv 2509.18869)** — what is deterministic:
  - Same-configuration embeddings over 5 runs: **L2 = 0.00, cosine = 1.00** for all 8 configurations (4 precisions × det./non-det.) across BGE, E5, and Qwen
  - Index build and CPU search: Exact Match, Jaccard, and Kendall's Tau all **1.00** for Flat_L2, Flat_IP, IVF, HNSW_accurate, HNSW_fast, LSH (fixed seed 42)
  - GPU search and 4-node distributed retrieval: **1.00 on all metrics** across 12 index/sharding combinations
  — [arXiv HTML](https://arxiv.org/html/2509.18869)
- What is *not* stable, and by how much:
  - **Embedding-model choice** is the largest variance source: BGE vs E5 overlap **0.540**, RBO 0.570, Kendall τ 0.384; BGE vs Qwen 0.454 / 0.486 / 0.338; E5 vs Qwen 0.432 / 0.474 / 0.322, with Kendall p-values above 0.05 — [arXiv HTML](https://arxiv.org/html/2509.18869)
  - **Adding documents** (~1,980 into an existing index) displaced roughly 20% of the top-50: overlap 0.793 (HNSW) / 0.806 (IVF) / 0.804 (LSH), RBO 0.725 / 0.730 / 0.735, but **Kendall τ = 1.000 for all three** — the authors call it "displacement, not re-ranking". Per-query HNSW overlap ranged 0.64-0.92 — [arXiv HTML](https://arxiv.org/html/2509.18869)
  - Precision changes cause tiny drift: FP32 vs FP16 L2 **5.74e-04**, vs TF32 4.09e-04, vs BF16 6.31e-03 — [arXiv HTML](https://arxiv.org/html/2509.18869)
  - **cuDNN determinism flags "had no observable effect"** on these embedding models, and non-deterministic mode was **10-15% faster with identical outputs** — so the determinism flags are costing speed for nothing — [arXiv HTML](https://arxiv.org/html/2509.18869)
  - The paper **does not evaluate LLM generation or temperature**, and lists CI/CD integration for continuous validation as **future work** — [arXiv HTML](https://arxiv.org/html/2509.18869)
- Recommendations from that paper: verify reproducibility empirically rather than trusting framework flags; report the embedding model explicitly; version and manage data updates; track precision; report spread alongside means (it reports means over 5 runs and gives variance for few metrics) — [arXiv HTML](https://arxiv.org/html/2509.18869)
- A systems-level 2026 study treats stability "as an empirical property of a configuration" and measures variance across repeated seeded runs — [arXiv 2606.28337](https://arxiv.org/pdf/2606.28337)
- Secondary/weaker: a framework summary says lowering temperature to zero "reduces fluency without removing the non-determinism of softmax sampling", and reports a **preliminary** finding that GPT-5 as an LLM ranker showed **3-5% rank disagreement on identical inputs** — [ReproRAG summary, emergentmind](https://www.emergentmind.com/topics/reprorag-framework)
- A public RAG-eval issue makes the practical point directly: the pipeline is non-deterministic at temperature 0, so single runs prove nothing; it proposes repeated runs and per-question outcome distributions — [GitHub issue](https://github.com/fedepacher/rag/issues/32)
- The agentic SoK: determinism "is not discussed beyond noting the policy is stochastic. Seeds, run-to-run variance, and replication are not addressed." It also argues Exact Match and Success Rate miss trajectory quality and that **Pass@k hides reliability problems** — [SoK](https://arxiv.org/html/2603.07379v1)
- The one adaptive method whose decision is exactly reproducible by construction: RASER's gradient-boosted router makes **zero LLM calls**, so its routing decision is a deterministic function of six numeric features — [arXiv HTML](https://arxiv.org/html/2606.02488v1)

### Inferences
- Pinning seed/top_p/top_k buys the series less than it appears: the measured evidence says the retrieval stack is already deterministic and the embedding stack is deterministic per-configuration, while the generation stack is not reliably pinned by temperature 0. An adaptive router implemented as an LLM call therefore injects non-determinism into the *one* decision the ratchet is measuring.
- A deterministic router is not merely conservative under a hard ratchet — it is the only variant where a single run constitutes evidence. An adaptive router would require the repeated-runs-and-distributions protocol that practitioners recommend but that no published adaptive-RAG paper actually follows.
- RASER's design is the proof that "adaptive" and "deterministic" are not opposites: a cascade whose escalation decision is a threshold over retrieval scores is adaptive per-query and reproducible run-to-run.

### Gaps
- **No paper found on regression-gating or CI for an adaptive retrieval pipeline.** This appears to be genuinely unaddressed as of October 2026, and the report should say so.
- No study found that measures run-to-run variance of an adaptive *routing decision* specifically (as opposed to retrieval or generation).

## The honest framing question: is a deterministic, schema-derived router defensible?

### Takeaway
There is now a 2026 peer-reviewable production study on a 240,000-document curated encyclopedia whose
conclusion is almost exactly the series' position — **every pre-retrieval routing method failed to
beat "always do the expensive thing", while a training-free post-retrieval cascade beat all of them
and cut latency 31.8%** — and whose explanation is structural, not a capability complaint. The
counter-evidence is honest too: in a multimodal setting, post-retrieval signals did *not* reliably
beat a query-only control. The defensible position is neither "routing is obsolete" nor "routing is
fine", but **"the decision belongs after retrieval, and it should be cheap and deterministic"**.

### Cited Findings

**"The Coverage Illusion: From Pre-retrieval Routing Failure to Post-retrieval Cascades in a Production RAG System" (Hussain & Nielbo, arXiv 2605.27220, CC BY-SA)**
- Testbed: the Danish National Encyclopedia (lex.dk), "a corpus of approximately 240,000 professionally curated articles"; five retrieval workflows × four query conditions × 1,000 queries = **20,000 judged query-workflow pairs**; only one condition is live user traffic — [arXiv HTML](https://arxiv.org/html/2605.27220v1)
- **Hand-written rules: 38.4% accuracy vs a 20% random baseline for five classes** — and on the stratified test split their composite score (CO 4.202) was **below Always-HyDE (4.258)** — [arXiv HTML](https://arxiv.org/html/2605.27220v1)
- Learned routers barely moved it: best was logistic regression over TF-IDF character n-grams at **CO 4.269, +0.011 over Always-HyDE**; gradient boosting over multilingual embeddings 4.184; logistic regression over multilingual embeddings 3.850 (worse than naive). A fine-tuned all-MiniLM-L6-v2 routed 73.8% of queries to HyDE and scored **3.804**, below Always-HyDE — [arXiv HTML](https://arxiv.org/html/2605.27220v1)
- **The structural ceiling, quantified:** "every pre-retrieval method covered at most **2.2%** of the gap between Always-HyDE and the oracle"; the best pre-retrieval method on unseen real queries routed "over 90%" of queries to HyDE and captured "under 5%" of the augmentation-free opportunity, while **47.6% of real user queries are retrieval-only** (52.4% need augmentation) — [arXiv HTML](https://arxiv.org/html/2605.27220v1)
- The stated reason: whether a query needs augmentation is determined by "**what the retrieval index contains, not by what the query says**" — [arXiv HTML](https://arxiv.org/html/2605.27220v1)
- **The cascade that worked:** run workflows in ascending cost order, stop at the first that returns at least one document, otherwise escalate — an **O(1) check on retriever output, with no training**. Main configuration Hybrid → QE-CE → HyDE — [arXiv HTML](https://arxiv.org/html/2605.27220v1)
- Measured: CO **3.944 → 4.084 (+0.140)** on real user queries, covering **30% of the oracle gap** (vs ≤2.2% for every pre-retrieval method); only **27.8% of queries reached LLM augmentation** versus 100% for Always-HyDE; latency **96.2s → 65.6s, −31.8%**; the Hybrid → HyDE variant scored 4.068 and saved 34.7% latency; **the cascade beat Always-HyDE in every ordering tested** — [arXiv HTML](https://arxiv.org/html/2605.27220v1)
- Worst-case path (15.5% of queries) averaged ~161s in the experiments, estimated ~125s in production, about **1.3x the HyDE baseline** — the cascade's tail cost, honestly reported — [arXiv HTML](https://arxiv.org/html/2605.27220v1)
- Authors' own limits: a single corpus, a deferral policy that sets failed retrievals to CO = 1.0, a Danish keyword-heavy query distribution; they "make no claims about other settings" — [arXiv HTML](https://arxiv.org/html/2605.27220v1)

**Independent corroboration of the same mechanism**
- RASER's feature importances put **0.73 of the routing signal in two retrieval-score features** and **0.03 in the query-text features** (`bridge_cues` 0.02, `qtype` 0.01) — an independent, quantitative version of "the index, not the query, decides" — [arXiv HTML](https://arxiv.org/html/2606.02488v1)
- The local-7B ablation shows what goes wrong with query-surface rules concretely: a heuristic keyed on named entities, dates, and numbers fired on nearly every multi-hop sub-question and sent **72%** of retrieval calls to BM25 — [arXiv 2606.21553](https://arxiv.org/html/2606.21553v1)

**The honest counter-evidence**
- **Post-retrieval signals are not automatically better.** In a multimodal adaptive-RAG routing study (answer generator Qwen2.5-Omni-7B; histogram gradient-boosting routers), adding retrieval signals to a query-only router gained **+0.01621 on dev [0.00436, 0.02960]** but only **+0.00288 on the held-out split, CI [−0.00726, 0.01107]** — not distinguishable from zero. On audio, query+retrieval was *worse* (Spearman ρ 0.3054 vs 0.3397 query-only) — [arXiv HTML](https://arxiv.org/html/2609.12437v1)
- That paper's methodological demand is directly useful to the series: retrieval-state features should get credit for routing value "only when they beat a matched query-only control". The authors explicitly say their result does **not** show routing is unnecessary or that retrieval state carries no information in principle — [arXiv HTML](https://arxiv.org/html/2609.12437v1)
- Its per-step best-case gains also bound the headroom: DOC_CLIP 0.0792 (help rate 9.5%), whole-page 0.0368 (4.67%), audio 0.130 (18%), video 0.065 (12%), tile (help 4%, **harm 7%**) — [arXiv HTML](https://arxiv.org/html/2609.12437v1)
- And the Coverage Illusion study is itself an argument *against* hand rules as such: 38.4% accuracy is barely above random, and the authors attribute the failure of *all* pre-retrieval methods "to the missing post-retrieval signal, not model complexity" — [arXiv HTML](https://arxiv.org/html/2605.27220v1)

**Practitioner side (vendor/blog — flag as such)**
- "Rules-based routing is transparent and auditable, making it preferred in regulated industries"; "when something goes wrong, you can trace exactly why a routing decision was made" — [123ofai](https://123ofai.com/articles/blocks/agent-router); [DEV](https://dev.to/aiwithmohit/the-silent-quality-collapse-why-your-llm-router-is-costing-you-more-than-you-saved-17l1)
- One vendor article argues router savings "often hide token bloat, cache invalidation, and compounding quality loss" and recommends deterministic workflows for well-defined, compliance-heavy tasks — [jinba.io](https://jinba.io/blog/model-routing-vs-deterministic-workflows-cost)
- A 2026 governance paper reports that a deterministic controller "changes the class distribution", i.e. auditability can cost aggregate metrics — [arXiv 2609.27087](https://arxiv.org/pdf/2609.27087)

### Inferences
- The defensible 2026 position, fully supported by citations: *explicit routing has not been superseded by adaptive or agentic RAG; it has been relocated.* The decision survives, is still worth making, and is still worth measuring — but the evidence says make it **after** the first retrieval, from retrieval signals, with a decider that costs no LLM tokens and returns the same answer twice.
- "Schema-derived and deterministic" is not just conservatism, on three independent measured grounds: (a) pre-retrieval query-only routing has a quantified ceiling of ~2.2% of oracle headroom in a 240k curated corpus; (b) the signal that works (top-1 score, score gap) is a property the retriever already returns, not a tuned number; (c) an adaptive LLM router is the one component that would make a single ratchet run stop being evidence, and no published work offers a protocol for gating it.
- The strongest honest caveat for the episode: `bridge_cues` at 0.02 importance and 38.4% rule accuracy say the series' *current* regex router is weak in the same way everyone's is, and the Coverage Illusion paper would predict that **improving the regex lists cannot pay** — the measured move is to re-site the decision, not to sharpen it. An episode that frames routing as "the honest foundation" should probably frame it as *the honest foundation that gets superseded by its own measurement*, which is a stronger story than either "routing is obsolete" or "routing is fine".
- The multimodal null result is the right place to be sceptical of the series' own hypothesis: the post-retrieval advantage replicated on a 240k text encyclopedia but not on a multimodal corpus, so it should be stated as corpus-dependent and measured, not as a law.

### Gaps
- Both the Coverage Illusion and Beyond the Query papers are single-corpus studies, and the former is one corpus in Danish with a keyword-heavy query distribution. No multi-corpus replication of the pre-retrieval ceiling was found.
- No source argues the *opposite* case with evidence — i.e. I found no measured study claiming an adaptive/LLM router beats a deterministic post-retrieval cascade on retrieval quality. Absence of that result in the searches performed is weak evidence, not proof.
- Nothing was found specific to Java 25 / Spring Boot 4.1.1 / Spring AI 2.0.1 implementations of any of these methods; the practicality notes above are engineering inference, not citation. Methods needing token-level logprobs (FLARE) or attention weights (DRAGIN) should be checked against Ollama/Spring AI capabilities before being considered available.
