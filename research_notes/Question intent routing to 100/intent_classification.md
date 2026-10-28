# Model-based query intent classification for retrieval routing

Scope note: the driving system is a Java 25 / Spring Boot 4.1.1 / Spring AI 2.0.1 RAG over an HTML spec archive, routing on three hand-written English lists (a 4-regex `WHY` list at measured precision 4/19, a 20-word `HISTORY_MARKERS` list, a 6-entry word→filename table), with fusion rather than commitment compensating for the bad precision. Eval sets are tiny (53 main / 47 answerable, 24 time, 13 held-out) and there is a no-tuning-on-eval rule plus a no-regression ratchet. Findings below are filtered for that situation.

Two cross-cutting caveats on sourcing: (1) several of the most relevant results are 2026 arXiv preprints, not peer-reviewed; I flag them. (2) Three papers I wanted (RealRoute 2604.20860, the EMNLP-2024 industry paper 2410.01627, the Coverage Illusion full text 2605.27220) are PDF-only and did not yield extractable body text, so only their abstract-level numbers appear here. Li & Roth's original accuracy figures could not be retrieved — see Gaps.

---

## Q1. Intent taxonomies for question/query classification — which are useful for *routing* rather than analytics?

### Takeaway
The classic QA taxonomies (TREC/Li & Roth: 6 coarse, ~47–50 fine labels over *answer type*) classify what kind of answer is wanted, not which collection holds it, so they are the wrong axis for section/document routing; the taxonomies that actually drive routing in current systems are small, hand-authored, *corpus-shaped* label sets (one label per retrievable partition), and the measured evidence says narrow label scopes are what make the out-of-scope class detectable at all.

### Cited Findings
- The TREC question classification dataset (Li & Roth, COLING 2002) is 5,500 train / 500 test questions, with 6 coarse labels (DESC, ENTY, ABBR, HUM, NUM, LOC) and 47 level-2 labels; the 500-question test split is the TREC-10 set — [TensorFlow Datasets catalog: trec](https://www.tensorflow.org/datasets/catalog/trec); [HuggingFace CogComp/trec loader](https://huggingface.co/datasets/CogComp/trec/blob/748e977e7c0ee3b1307e166678291bd5d7a13399/trec.py)
- Standard intent-detection label spaces in the current literature are large and application-shaped, not linguistic: CLINC150 (150 intents / 10 domains + 1,000 explicit out-of-scope utterances), Banking77 (77), MASSIVE (60), MTOP (117), ATIS (26, 22 present in train), SNIPS (7), HINT3 Curekart (20) / Powerplay11 (53) / Sofmattress (20) — [Selecting Open-Weight LMs for Zero-Shot Intent Classification (arXiv 2607.27421)](https://arxiv.org/html/2607.27421)
- Label-scheme ambiguity, not model weakness, is the dominant error source: on MASSIVE the single most common confusion was `play music` vs `music query`, and it was the top confusion for 21 of 41 models evaluated — [arXiv 2607.27421](https://arxiv.org/html/2607.27421)
- Narrower intent scopes measurably improve out-of-scope detection across models, and OOS performance generally degrades as the label space grows (trend varies by model); the paper's first practical recommendation is to design intent taxonomies with narrower scopes — [Intent Detection in the Age of LLMs, EMNLP 2024 industry (arXiv 2410.01627)](https://www.alphaxiv.org/abs/2410.01627)
- Compound/compositional intents (e.g. ATIS `flight+airfare`) are where zero-shot LLMs degrade: the model returns "plausible but non-canonical label combinations" — [When Do LLMs Replace Fine-Tuned NLU? (arXiv 2608.20371)](https://arxiv.org/html/2608.20371)
- Routing taxonomies in RAG are increasingly defined over *retrieval effort* rather than topic: a cost-aware router classifies by query complexity to decide retrieval depth, and reports that always-heavy retrieval inflates prompt tokens 36% (343 vs 252 per query) with no quality gain on most queries, while always-skip underserves complex queries; savings concentrate in simple definitional queries — [Cost-Aware Query Routing in RAG (arXiv 2606.02581)](https://arxiv.org/pdf/2606.02581)

### Inferences
- For this system the useful taxonomy is not factoid/list/definition/causal/temporal — it is a 4-way (or 5-way with NONE) set of labels that name the actual retrievable partitions: `context-section`, `retired-units-admissible`, `document-selection`, plus NONE/plain. Each label must be defined by what it *changes about retrieval*, otherwise it is analytics.
- The MASSIVE `play music` vs `music query` result is the direct analogue of the `WHY` list's 4/19 problem: "why" questions and "what does X do" questions are adjacent in label space, and the fix is a sharper label definition (with explicit negative examples), not a better classifier.
- The 2608.20371 compound-intent finding argues against a single-label router here: "why was the retired unit withdrawn" is simultaneously WHY and HISTORY, and a single-label classifier will be forced into a non-canonical answer.

### Gaps
- I could not retrieve Li & Roth's own reported accuracy figures (the ~90% coarse / ~84% fine numbers commonly quoted); search returned only dataset documentation. The primary source is COLING 2002 C02-1150 and would need direct fetch.
- I found no published intent taxonomy specifically for *section-level* routing inside structured/spec documents.

---

## Q2. Zero-/few-shot LLM classification: how reliable, with what numbers, and what prompt/label-set practices?

### Takeaway
The single most useful measured result for this project: zero-shot LLM classification *loses badly to a trained encoder on small label sets* (−11.8 pts on 26-intent ATIS) but *ties on large ones* (−0.6 pts, n.s., on 150-intent CLINC150) — and it *wins decisively on the out-of-scope class* (OOS F1 85.0 vs 73.0 fine-tuned, OOS recall 85.6% vs 58.1%). Since the failing `WHY` list is essentially an OOS/precision problem on a tiny label set, this is the relevant asymmetry.

### Cited Findings
Head-to-head (accuracy, macro-F1 in parentheses), Claude Haiku zero-shot vs trained baselines — [arXiv 2608.20371](https://arxiv.org/html/2608.20371):
- ATIS: TF-IDF+LR 95.2 (68.0) | ST+kNN full 86.2 (36.8) | ST+kNN 5-shot 28.1 (22.1) | RoBERTa fine-tuned 95.9 (63.1) | Claude 0-shot 84.1 (42.4)
- CLINC150: TF-IDF+LR 80.9 (85.4) | ST+kNN full 75.1 (81.3) | ST+kNN 5-shot 64.2 (69.9) | RoBERTa fine-tuned 89.1 (92.0) | Claude 0-shot 88.5 (88.8)
- ATIS gap RoBERTa−Claude = 11.8 pts (CI [9.2, 14.4], p = 2×10⁻⁴; McNemar p = 2.5×10⁻¹⁶). CLINC150 gap = 0.6 pts (CI [−0.4, +1.7], p = 0.24, not significant).
- The paper explicitly corrects earlier literature: the apparent LLM advantage on ATIS came from a tiny baseline training split (N = 56).
- Out-of-scope (CLINC150, OOS as positive class): TF-IDF+LR in-scope 90.7 / OOS recall 36.4 / OOS F1 51.7; RoBERTa ft 96.0 / 58.1 / 73.0; Claude 0-shot 89.2 / **85.6** / **85.0**. The LLM is simply allowed to answer `oos`; no threshold is used.
- Schema agility: a RoBERTa trained on 75-intent "App A" scores 96.8 on App A and **0.0** on a disjoint 75-intent App B; schema-prompted Claude scores 94.3 / 93.9 with no retraining (±1 pt CI).
- ASR/noise robustness (TTS → noise → Whisper, 120 utterances/condition): clean 92.5 (TF-IDF) vs 98.3 (Claude); at 0 dB with mean WER 28.9%, 80.0 vs 92.5.
- Latency/cost per 1,000 calls: TF-IDF+LR local <0.1 ms p50/p95, $0.00; RoBERTa local 2.4 ms p50, $0.00; Claude Haiku API 981 ms p50, 1,787 ms p95, $0.25/1K. (The paper's "~10³× slower" claim matches the TF-IDF comparison; vs RoBERTa it is ~400×.)
- Prompting used in that study: label set + JSON output schema in the prompt; in the dynamic-schema test only the relevant app's label list is supplied. No prompt-variant ablation reported.

Open-weight local models, 41 models / 15 families / 135M–9B, 8 zero-shot IC datasets, group-balanced aggregate accuracy — [arXiv 2607.27421](https://arxiv.org/html/2607.27421):
- Mistral-7B-Instruct-v0.3 **0.660** (top, Pareto-optimal) | Qwen2-7B-Instruct 0.646 | Llama-3.1-8B-Instruct 0.644 | Yi-1.5-9B-Chat 0.638 | Qwen2.5-7B-Instruct 0.625 | Qwen2.5-3B-Instruct **0.632** (best sub-4B) | Qwen2-1.5B-Instruct 0.512 (only sub-3B above 0.5) | Qwen2.5-0.5B-Instruct 0.274 | DeepSeek-R1-Distill-Qwen-1.5B 0.002 (token-budget artifact: `<think>` traces consumed the 20-token budget)
- Scale alone is not a reliable predictor of zero-shot IC performance; instruction tuning helped in all six cleanly matched base→instruct pairs (+0.012 to +0.095).
- Top-5 models on MASSIVE are statistically indistinguishable (pairwise McNemar p = 0.1422–0.3384).
- Per-dataset average accuracy: SNIPS (7 labels) 0.807 | CLINC150 (150) 0.468 | MASSIVE (60) 0.432 | Banking77 (77) 0.388 | MTOP (117) 0.348 | Powerplay11 (53) 0.299 — accuracy is *not* monotone in label count, and label count is confounded with domain.
- Decoding setup: free-text greedy, **max 20 output tokens**, stop tokens, no constrained decoding, no chain-of-thought; exact match after normalisation; invalid output counts as wrong.
- Robustness perturbations: lowercasing cost 0.0 pts for all 28 competitive models; punctuation removal negligible; random adjacent word swap ~2.2 pts; typo robustness varied 1.3 pts (Qwen2.5-7B-Instruct, best) to 25.9 pts.
- Latency/VRAM (V100-32GB, vLLM, fp16, per sample): Qwen2.5-0.5B 9.0 ms / 0.93 GB | Qwen2.5-3B 20.8 ms / 5.8 GB | Mistral-7B-v0.3 48.5 ms / 13.51 GB | Llama-3.1-8B 45.6 ms / 14.99 GB | Qwen2-7B 55.0 ms / 14.25 GB. Not Ollama, not quantised, not CPU.
- Caveats stated by the authors: 500-example subsets (except full HINT3 test sets), one perturbation seed, ATIS skewed to majority class, SNIPS now saturated.

Other measured comparisons:
- A Reddit misinformation-response study: fine-tuned RoBERTa 0.62 macro-F1 vs 0.50 for the best zero-shot configuration (Claude Haiku 4.5) — [Long live fine-tuning (Springer)](https://link.springer.com/article/10.1007/s13278-026-01633-0)
- Action-prediction: fine-tuned BERT 57.14% vs 53.18% best zero-shot LLM, with LLMs showing strong single-class bias — [arXiv 2502.12073](https://arxiv.org/pdf/2502.12073)
- Hybrid small-classifier + LLM: within ~2% of native LLM accuracy at ~50% lower latency on datasets with consistent train/test distribution; a two-step OOS method using LLM internal representations improved OOS accuracy and F1 by >5% for Mistral-7B — [arXiv 2410.01627](https://www.alphaxiv.org/abs/2410.01627)
- Intent routing on hidden-state features vs an LLM call (Qwen3/Llama 1B–32B, 3 seeds): zero-shot LLM call is often competitive and sometimes better (Qwen3-8B 0-shot: MATH500 99.60; Qwen3-32B 0-shot GSM8K 86.91 → **97.80 with 3-shot**), while a fine-tuned RoBERTa scored only 42.33 ± 13.05 on MATH500 — [Training-Free vs Training-Based Intent Classification in LLMs (arXiv 2608.02415)](https://arxiv.org/html/2608.02415v1)
- The same paper's headline: "there is no one-fits-all method for intent classification" — training-based stronger on fine-grained tasks, training-free more robust on noisy/ambiguous prompts — [arXiv 2608.02415](https://arxiv.org/html/2608.02415v1)
- Adversarial misdirection collapses *every* method: MATH500 disguised as code (Easy/Medium/Hard) — training-free VecStat:Cos 78.60/54.80/0.20; trained Avg-MLP 12.67/4.20/0.00; GPT-5 LLM call 93→98/98/64 (the strongest, still 64 at Hard) — [arXiv 2608.02415](https://arxiv.org/html/2608.02415v1)

### Inferences
- The ATIS-vs-CLINC150 split predicts that a 4–5 label router here sits in the *unfavourable* regime for a zero-shot LLM relative to a trained classifier — but you have no training data, and the 5-shot kNN numbers (28.1 ATIS / 64.2 CLINC150) show the trained alternatives are worse than the LLM when labels are scarce. With ≤13 held-out and no label budget, zero-shot is the only option that is not a tuning violation.
- The OOS asymmetry (85.0 vs 73.0 F1) is the strongest argument for replacing the `WHY` regexes with an LLM: the measured failure of that list is 15 false positives out of 19 fires, i.e. a NONE-class problem, exactly where the LLM's advantage is largest. An explicit `NONE` label with permission to use it is the mechanism, not a threshold.
- The 20-output-token budget finding is an operational landmine for local Ollama serving: a reasoning-distilled or thinking-enabled model will emit `<think>` and score ~0. Any Spring AI implementation must disable thinking or budget tokens generously, and must count unparseable output as NONE rather than as a route.
- Mistral-7B-Instruct-v0.3 (0.660) at ~48 ms/sample fp16 and Qwen2.5-3B-Instruct (0.632) at ~21 ms / 5.8 GB are the two defensible Ollama picks — the 3B is the laptop tier, the 7B the 5090 tier, and the aggregate gap between them is only 0.028. Qwen2.5-7B-Instruct's typo robustness (1.3 pts vs up to 25.9) is a reason to prefer it over Mistral if inputs are user-typed.
- Few-shot is the cheapest available accuracy lever that does not require fitting: +10.9 pts on GSM8K intent ID from 0-shot to 3-shot in 2608.02415. Few-shot examples should be *authored* (hand-written exemplars illustrating the label boundary), not *sampled from the eval set* — the latter is a ratchet violation.

### Gaps
- No study I found ablates prompt structure (label-list ordering, definitions vs bare labels, negative examples) with numbers; 2608.20371 and 2607.27421 both explicitly report no prompt-variant ablation. So "best practice" for prompt structure is unmeasured.
- No measured comparison of constrained/grammar-constrained decoding vs free-text + parse for intent labels. 2607.27421 used no constrained decoding and counted invalid output as wrong; what fraction of errors were parse failures is not broken out.

---

## Q3. Calibration and abstention: which confidence signal actually correlates with correctness, and how to threshold without fitting on the eval set?

### Takeaway
The best-measured 2026 result for *classification* confidence says token logprobs are mediocre (AUROC 0.623), self-consistency voting is the *worst* useful signal (0.573 at 4× cost), and verbalised confidence variants are the best (0.661–0.710) — with "top-2 verbalisation" giving the best calibration (ECE 0.131–0.202) at 2.3× cost. Crucially, reported rankings flip depending on an evaluation detail (AUARC interpolation), and the abstention thresholds published are all fitted on held-out data, which this project cannot do.

### Cited Findings
Qwen3-32B, averaged over SST-2 (872/2 classes), SST-5 (2,210/5), Yahoo! topic (6,000/10), Amazon ESCI (8,604/4) — [Evaluation Pitfalls and Sparsity Limitations in LLM-based Confidence Estimates for Classification (arXiv 2608.04899)](https://arxiv.org/html/2608.04899); also [ACL Findings 2026](https://aclanthology.org/2026.findings-acl.1671.pdf):

| Method | AUROC | ECE | AUARC (stepwise) | % in top-5 values | Cost |
|---|---|---|---|---|---|
| Random | 0.482 | 0.223 | 0.652 | 0 | 1.0× |
| Consistency sampling (self-consistency) | 0.573 | 0.296 | 0.683 | 100 | 4.0× |
| Verbalization sampling | 0.678 | 0.238 | 0.734 | 45 | 6.8× |
| **Top-2 verbalization** | **0.689** | **0.202** | 0.731 | 93 | 2.3× |
| Vanilla verbalization | 0.661 | 0.235 | 0.713 | 92 | 1.7× |
| Token logprobs | 0.623 | 0.283 | 0.723 | 0 | 1.0× |
| Verbalization logprobs | 0.670 | 0.234 | **0.736** | 1 | 1.7× |

- Claude 3.7 Sonnet, no reasoning: consistency sampling AUROC 0.579 / ECE 0.266; vanilla verbalization 0.691 / 0.184; verbalization sampling 0.705 / 0.187; top-2 verbalization **0.710 / 0.131** — [arXiv 2608.04899](https://arxiv.org/html/2608.04899)
- **Evaluation pitfall:** AUARC rankings invert with interpolation choice. Consistency sampling 0.808 (linear) → 0.683 (stepwise), rank 1 → rank 5. Token logprobs are unaffected (0.723 both ways) — [arXiv 2608.04899](https://arxiv.org/html/2608.04899)
- **Sparsity:** Qwen3-32B on SST-2 emits only eight distinct vanilla verbalised confidences, with more than half exactly 95%; across datasets 45–93% of confidences fall in the five most common values. This makes a fine-grained verbalised threshold meaningless — [arXiv 2608.04899](https://arxiv.org/html/2608.04899)
- "Verbalization logprobs" = expected value over the tens/units digit token distributions, Σ10d·P(tens=d) + Σd·P(units=d) (digits assumed independent, acknowledged as an approximation). Gains +2.3 AUARC / +0.9 AUROC over vanilla verbalisation and +1.3 AUARC / +4.7 AUROC over token logprobs, in a **single** inference call; unique values rise from 8–12 to 630–2,227; Spearman ρ with vanilla verbalisation averages 0.89 — [arXiv 2608.04899](https://arxiv.org/html/2608.04899)
- Self-consistency was run at 4 samples, temperature 1.0, "because prior work reports performance saturates early"; other sample counts were not tested — [arXiv 2608.04899](https://arxiv.org/html/2608.04899)
- Abstention/selective-prediction numbers (SST-2, Qwen3-32B): rejecting 205 samples → 97.9% accuracy; rejecting 345 → 98.7%. Reaching ≥98% accuracy requires rejecting 39.7% of inputs with vanilla verbalisation but only 28.1% with verbalization logprobs (≈11.6% more data retained) — [arXiv 2608.04899](https://arxiv.org/html/2608.04899)
- Logprobs are simply unavailable on several commercial APIs (OpenAI, Claude, Bedrock are named) — [arXiv 2608.04899](https://arxiv.org/html/2608.04899); a separate judge study confirms "Claude never returned logprobs" — [Rethinking Verbalized Confidence for LLM-as-a-Judge (arXiv 2609.10996)](https://arxiv.org/html/2609.10996v1)
- Verbalised confidence has become the more subjectivity-robust soft signal on top-tier GPT-family releases, compared with logprob-based G-Eval — [arXiv 2609.10996](https://arxiv.org/html/2609.10996v1)
- Earlier evidence agrees in direction: verbalised confidences emitted as output tokens are typically better calibrated than the model's conditional probabilities on TriviaQA, SciQ and TruthfulQA for RLHF models including Claude — [Strategies for Eliciting Calibrated Confidence Scores from LLMs, EMNLP 2023](https://aclanthology.org/2023.emnlp-main.330.pdf)
- One sequence-level-logprob calibration measurement on an open-weight intent classifier: Qwen2.5-7B-Instruct had the lowest ECE among competitive models at **0.066** (MASSIVE only) — [arXiv 2607.27421](https://arxiv.org/html/2607.27421)
- A practitioner blog recommends treating verbalised confidence as an ensemble signal and never as the primary score, using logprob aggregation where available — [futureagi.com](https://futureagi.com/blog/evaluating-llm-confidence-uncertainty-2026/) (vendor blog, not measurement)
- Sample-complexity result for *trained* calibration of hidden-state intent classifiers, if that route is ever considered: NormStat needs N = Ω(ε⁻²) samples per class, VecStat N = Ω(d·ε⁻²) with d the hidden dimension; empirical error decays ≈N^−0.5 over 512→32,768 samples — [arXiv 2608.02415](https://arxiv.org/html/2608.02415v1)

### Inferences
- Self-consistency voting is the technique to *skip*: 4× cost for AUROC 0.573 (Qwen3-32B) / 0.579 (Claude), barely above random's 0.482 and below plain verbalisation. For a local Ollama router this is the worst cost/benefit on the table.
- Ollama *does* expose per-token logprobs, so the verbalization-logprobs trick is implementable locally (unlike on Claude/Bedrock) — it is the only signal that is both single-call and non-sparse (630–2,227 distinct values), which is what you need if a threshold is ever set. Whether Spring AI 2.0.1 surfaces Ollama logprobs through its abstraction is an integration risk to check.
- Given the no-tuning rule, the honest design is **threshold-free abstention**: make `NONE` an explicit label and let the model emit it (the mechanism that produced OOS F1 85.0 in 2608.20371), rather than deriving a numeric confidence and cutting it at a fitted point. Every published threshold in 2608.04899 is a held-out fit; with 13 held-out questions any threshold you pick is noise and a ratchet hazard.
- If a numeric gate is wanted anyway, the defensible non-fitted choices are (a) the sparsity structure itself — verbalised output is effectively 3-valued, so "route only on the top mode (e.g. 95)" is a *structural* not fitted rule; or (b) top-2 verbalisation with the rule "route only if top-1 label and the gap to top-2 both exceed the model's own stated margin", again structural. Both should be declared as untuned priors, not calibrated thresholds.
- The AUARC interpolation finding is a direct warning about this project's own evaluation: on 47 answerable questions, a routing-precision metric computed with different tie/abstain conventions can reorder candidate routers. Fix and document the convention before comparing.

### Gaps
- **p(true) is not evaluated** in 2608.04899 (discussed only as related work), and I found no 2026 measurement of p(true) for short-label classification. Its reliability here is unknown.
- No published method for setting an abstention threshold without any held-out labelled data. Everything I found assumes a calibration split.
- No measurement of confidence calibration for *routing* decisions specifically (i.e. does router confidence predict downstream answer quality) — all figures above are classification correctness.

---

## Q4. Routing architectures: hard vs soft vs learned fusion, and router failure modes

### Takeaway
There is no clean published head-to-head of hard routing vs fusion for multi-collection retrieval; what the 2026 evidence does say is that query-only (pre-retrieval) routing is structurally unable to know what it needs to know — the need is "only revealed after searching the index" — and the best-performing production answer in that paper is neither hard routing nor fusion but a **cheapest-first post-retrieval cascade** that escalates when a step returns no documents.

### Cited Findings
- "The Coverage Illusion": existing adaptive-RAG systems choose a retrieval path before retrieval, using only the input query; the paper states "the need for LLM augmentation is only revealed after searching the index," and that comparing four machine-learning paradigms confirmed pre-retrieval routing cannot close the gap — [The Coverage Illusion (arXiv 2605.27220v1)](https://arxiv.org/abs/2605.27220v1)
- Same paper: synthetic queries suggest >90% of queries need LLM augmentation; under the production deferral policy only **27.8%** of *real* user queries do. Synthetic benchmark queries are more verbose and grammatically complete than real user queries, which are brief and sparse — [arXiv 2605.27220v1](https://arxiv.org/abs/2605.27220v1)
- Its post-retrieval cascade vs Always-HyDE: composite overall score +0.140, latency −31.8%, and 72.2% of real queries served without LLM augmentation; the authors note it needs no training overhead and no secondary serving infrastructure — [arXiv 2605.27220v1](https://arxiv.org/abs/2605.27220v1)
- Hard routing *is* used as a named baseline in multi-source routing benchmarks (2/3/4-source setups, EM and F1), but I could not extract the numbers — [RealRoute (arXiv 2604.20860)](https://arxiv.org/pdf/2604.20860)
- Routing-then-fusing is the dominant practical pattern: ModaRoute selects a subset of modalities per query then merges with rank-based fusion, choosing linear fusion over RRF for comparable accuracy at lower overhead — [Smart Routing for Multimodal Video Retrieval (arXiv 2507.13374)](https://arxiv.org/pdf/2507.13374)
- A fusion-router variant assigns *weights* per source via an LLM and then does weighted reciprocal rank fusion, rather than choosing one source — [Driving Video Retrieval with Structured Grounding (arXiv 2606.09109)](https://arxiv.org/pdf/2606.09109)
- In a *non-retrieval* setting (multi-task model merging, 8- and 30-task), hard routing to a single predicted path was both faster and more accurate than soft routing — [Training-free Task Classification for Multi-Task Model Merging (arXiv 2606.22589)](https://arxiv.org/pdf/2606.22589)
- Cost-aware routing's own caveat: its complexity signal correlates only weakly with billed tokens, so it may misroute some queries — [arXiv 2606.02581](https://arxiv.org/pdf/2606.02581)
- Agentic-RAG failure modes: semantic drift, where reformulated queries gradually diverge from the user's original need, and errors compounding across iterations — [SoK: Agentic RAG (arXiv 2603.07379)](https://arxiv.org/pdf/2603.07379)
- Agentic loops can discard good results and end up worse than one-shot retrieval; ambiguous queries and answers spread across documents are the classic one-shot failure cases — [ByteByteGo, How Agentic RAG Works](https://blog.bytebytego.com/p/how-agentic-rag-works) (practitioner explainer)
- Small query perturbations can shift retrieval enough to degrade answers — [Retrieved But Not Reliable (arXiv 2608.24977)](https://arxiv.org/pdf/2608.24977)
- Automated RAG testing at scale found 21,633 failures across 72,000 test executions (retrieval inaccuracies and unsupported hallucinations) — [RAG-TESTER (arXiv 2608.00054)](https://arxiv.org/pdf/2608.00054)
- A practitioner post reports adding query routing lifted accuracy from 58% to 67% on its own 500-query test set — [dev.to](https://dev.to/exploredataaiml/building-an-intelligent-rag-system-with-query-routing-validation-and-self-correction-2e4k) (single unreplicated anecdote)

### Inferences
- The project's existing choice — route then *fuse* rather than commit — is the architecture the literature actually supports for a low-precision router, and the Coverage Illusion result says the precision ceiling is partly structural, not a classifier-quality problem. So replacing regexes with a classifier should be expected to improve *fusion weights*, not to license hard routing.
- The most transferable idea for this system is the cascade inversion: instead of deciding from the question whether to search `#context`, search cheaply first and escalate to `#context` / retired-units only when the cheap pass returns nothing (or nothing above threshold). That converts a precision-4/19 prediction problem into a zero-precision-required fallback, needs no labelled data, no threshold fit, and is ratchet-safe because the cheap path is the current behaviour.
- The 90%-synthetic vs 27.8%-real divergence is a caution about the 53/24/13 question sets: if they were authored rather than harvested from real use, measured router precision will be optimistic about verbose questions and pessimistic about terse ones.
- Hard routing is only defensible for the one sub-decision where the label space is a genuine partition with a verifiable key: the 6-entry word→filename table. A wrong document is recoverable by fusion; a wrong *document* commit is not. The WHY/HISTORY decisions are admission filters (they widen the candidate pool), which is inherently soft and should stay soft.

### Gaps
- No extractable numbers for hard routing vs fusion from RealRoute; a direct fetch of its HTML version would be needed.
- I found no paper reporting routing precision/recall *and* downstream answer quality for section-level routing inside a single document corpus — the closest (Coverage Illusion) routes over retrieval *strategies*, not sections.

---

## Q5. Small-model and non-LLM options: embeddings, logistic regression, semantic routers — cost/latency/accuracy

### Takeaway
Embedding nearest-centroid/kNN routers are 2–3 orders of magnitude cheaper than an LLM call (~5–50 ms vs ~1 s) and reach high accuracy *when you have labelled example questions per route* — the measured 5-shot numbers (28.1 ATIS / 64.2 CLINC150) show they collapse without them, which is precisely this project's constraint. TF-IDF + logistic regression is the surprising strong baseline: 95.2 on ATIS, beating zero-shot Claude by 11 points at zero cost and <0.1 ms.

### Cited Findings
- TF-IDF + logistic regression: ATIS 95.2 (macro-F1 68.0), CLINC150 80.9 (85.4); <0.1 ms p50 and p95, $0.00; but OOS recall only 36.4 / OOS F1 51.7 — [arXiv 2608.20371](https://arxiv.org/html/2608.20371)
- Sentence-transformer + kNN: ATIS 86.2 full-data vs **28.1 at 5-shot**; CLINC150 75.1 full vs **64.2 at 5-shot** — [arXiv 2608.20371](https://arxiv.org/html/2608.20371)
- Nearest-centroid over e5-large-v2 embeddings for 44-task selection: ~22 ms total selection time, 89.7% selection accuracy — [Semantic Adapter Routing (arXiv 2606.19079)](https://arxiv.org/pdf/2606.19079)
- Fine-tuned encoder router in telecom intent-based networking: ~99% route accuracy, 94.6% route-plus-branch accuracy (MPNet slightly ahead of MiniLM); ~4.8 ms average for the local pipeline vs ~4.2 s for a cloud LLM baseline — though the paper does not separate embedding latency from the rest of the pipeline, so 4.8 ms is not pure router latency — [Semantic Routing Framework for Multi-Domain Telecom IBN (arXiv 2610.24487)](https://arxiv.org/html/2610.24487)
- IBM's semantic router for vLLM (routing to reasoning, not pure intent): MMLU-Pro across 14 domains 58.57% vs 48.33% direct vLLM, latency 24.76 s → 13.09 s on an NVIDIA L4; the paper's own tables give 47.1% latency reduction and 48.5% token reduction (a secondary blog misreports 48.5% as latency) — [When to Reason: Semantic Router for vLLM (arXiv 2510.08731)](https://arxiv.org/html/2510.08731v1)
- Practitioner figures: embedding routers ~16–100 ms, 92–96% precision in production "after example refinement" — [TianPan.co](https://tianpan.co/blog/2026/04/16/intent-classification-agent-routers) (blog, unverified; it also claims the IBM router uses ModernBERT, which I could not confirm from the paper); 5–20 ms for embedding + classification, with advice to measure in your own path — [TrueFoundry](https://www.truefoundry.com/blog/llm-routing-cost-quality-aware-model-selection) (vendor); ~5,000 ms → ~100 ms — [truto.one](https://truto.one/blog/how-to-implement-semantic-routing-for-ai-agents-to-select-api-endpoints) (vendor)
- A vendor blog claims a fine-tuned lightweight classifier trades at most 2% accuracy for 50% lower latency vs LLM-based classification, and that rule-based methods run in microseconds but "break on any query that deviates from expected phrasing" — [trickywombat.ai](https://www.trickywombat.ai/our-technology/query-intent) (marketing, not measurement)
- Hidden-state training-free routers (VecStat:Cos over the first ~12 of 28 layers) hit GSM8K 100.00, HumanEval 95.73, MATH500 94.20, Programming 99.34 on Qwen3-8B — but the same method scores **2.06** on Prealgebra, i.e. catastrophic variance by label; VecStat stays near its best from 32 to 512 input tokens — [arXiv 2608.02415](https://arxiv.org/html/2608.02415v1)
- Mixed-intent calibration RMSE (Qwen3-1.7B, lower better): VecStat 0.1216 code-first / 0.0858 math-first; NormStat 0.2213 / 0.2098; Avg-MLP 0.2451 / 0.1764; Tail-MLP 0.2412 / 0.2688 — [arXiv 2608.02415](https://arxiv.org/html/2608.02415v1)
- No standard head-to-head benchmark of semantic-router *libraries* on a shared intent dataset was found in search.

### Inferences
- **Minimum viable labelled data, by technique** (synthesising the above): LLM zero-shot with an explicit NONE label — 0 labelled questions, only authored label definitions; LLM few-shot — 2–5 *authored* exemplars per label (3-shot gave +10.9 pts on one intent task); embedding nearest-centroid / semantic-router — the 5-shot kNN collapse (28.1 on a 26-label set) says ~5/class is not enough on small confusable label sets, so budget ≥20–30 questions per route; TF-IDF+LR or a fine-tuned encoder — hundreds (ATIS used 4,978 train; the paper notes that a tiny N=56 split produced misleading results). With 53+24+13 = 90 total questions, **only the first two are compatible with the no-tuning rule**.
- Any embedding-centroid router built from the project's own eval questions is a direct tuning-on-eval violation; built from *newly authored* questions it is legitimate but then its route definitions and the eval set are independent, and the 5-shot evidence says it will underperform the LLM until the authored set is sizeable.
- Java/JVM cost note: TF-IDF+LR and nearest-centroid are both trivially implementable in Java 25 (no Python training infra) — centroid routing is one embedding call via Spring AI's `EmbeddingModel` plus cosine similarity; a fine-tuned RoBERTa/MPNet is where Python training infrastructure becomes a real cost, and 2608.20371's App-A/App-B result (96.8 → 0.0 on a schema change) shows that cost recurs on every taxonomy edit.
- Latency budget: an extra local 3B–7B classification call costs ~20–50 ms of GPU compute per the vLLM figures but realistically a few hundred ms through Ollama with prompt processing; against a RAG pipeline already doing embedding + retrieval + generation this is minor on the 5090 tier and noticeable on the laptop tier. An embedding-centroid router reuses an embedding you may already compute, so its marginal cost is ~0.

### Gaps
- No measured Ollama/quantised (GGUF, 4-bit) intent-classification accuracy figures; 2607.27421 is fp16 on a V100 and explicitly does not test quantisation or CPU. Quantisation effect on short-label classification accuracy is unknown.
- No published evaluation of `semantic-router`, LlamaIndex routers or Haystack routers against each other or against a shared dataset; the only numbers available for these are vendor/blog claims. Vendor documentation claims capability, not accuracy.

---

## Q6. Multi-label and compositional intent (a question that is both causal and temporal)

### Takeaway
Single-label forcing is a measured failure mode — zero-shot LLMs return "non-canonical" compound labels on ATIS and few-shot LLMs systematically emit *incomplete* label sets — and the state of the art for genuinely novel combinations is clause-level factorisation, where whole-utterance baselines score 0.0 exact match and a clause-factorised decoder retains 91.1.

### Cited Findings
- On unseen intent combinations with discourse shift, whole-utterance baselines reach **0.0** exact match while the clause-factorised decoder ClauseCompose retains **91.1**; the paper argues existing multi-intent benchmarks mostly test familiar co-occurrence patterns — [Known Intents, New Combinations (arXiv 2603.28929)](https://arxiv.org/pdf/2603.28929)
- Zero-shot LLMs on ATIS compound intents (e.g. flight+airfare) "often return plausible but non-canonical label combinations", and this is cited as a main cause of the 11.8-point ATIS gap — [arXiv 2608.20371](https://arxiv.org/html/2608.20371)
- Few-shot LLMs "tend to output only some of the gold intents" in multi-label settings — [Instance Relation Learning Network for Few-shot Multi-label Intent Detection (arXiv 2510.07776)](https://arxiv.org/pdf/2510.07776)
- LLMs generalise better than SetFit under test-distribution shift and do identify multiple intents in complex queries; OOS detection depends on label-set scope and label-space size — [arXiv 2410.01627](https://www.alphaxiv.org/abs/2410.01627)
- A supervised BERT classifier outperformed the best few-shot generative LLM on multi-label dialogue intents — [Multi-Intent Recognition in Dialogue Understanding (arXiv 2509.10010)](https://arxiv.org/pdf/2509.10010)
- A pointer-network approach addresses joint extraction and detection of multi-label multi-class intents — [arXiv 2410.22476](https://arxiv.org/pdf/2410.22476)
- An LLM framework for holistic intent detection additionally recognises *dependencies* between intents and uses intent selection to control prompt length, aiming to make small open-source models workable — [ResearchGate listing](https://www.researchgate.net/publication/398976944_Holistic_Intent_Detection_With_LLM_for_Emotion_AI_Multi-intent_Detection_and_Dependency_Recognition) (listing only; I could not verify its numbers)

### Inferences
- The right shape for this router is **independent binary decisions, not one multi-class label**: `needs_context_section: bool`, `admits_retired_units: bool`, `document: enum|null`. This sidesteps the compound-label pathology entirely (there is no "non-canonical combination" to emit) and matches the fact that WHY and HISTORY are independent admission filters. One structured-output call can return all three.
- The ClauseCompose 0.0-vs-91.1 result argues that if a question *is* compositional ("why was the superseded clause removed"), classifying the whole question as one thing is the worst option; decomposing per clause and OR-ing the admission flags is the robust move, and is cheap to do with independent booleans.
- The "few-shot LLMs emit incomplete label sets" finding predicts a specific bug: with a multi-label prompt the model will name WHY and silently drop HISTORY. Independent boolean fields with required JSON keys force an answer on each axis and make the omission impossible.

### Gaps
- No numbers on multi-label *routing* (as opposed to multi-label intent labelling) — i.e. no study measures retrieval quality when both of two routes are admitted vs one.

---

## Q7. Regex/keyword baselines vs learned classifiers: how much is actually gained?

### Takeaway
The clearest head-to-head I found puts a "carefully designed" rule baseline at ~75% accuracy / 0.40 F-score against a CNN at ~81.6% / 0.50 — a ~6-point accuracy and ~10-point F gain — and production search studies report +4 to +8 points over a production linear baseline. But the strongest single number for this project's situation is not from the rule-vs-learned literature at all: it is the 85.6% vs 36.4% OOS-recall gap between an LLM and TF-IDF+LR, because the failing `WHY` list is a false-positive problem.

### Cited Findings
- CNN query-intent classifier vs a hand-built rule baseline: rule ~75% accuracy / F 0.40 → CNN ~81.6% / F 0.50; on a second dataset the rule baseline scored 77% accuracy with precision 0.45 / recall 0.34 / F 0.36; the authors state the learned models "significantly outperform the carefully designed rule-based baseline" (the learned figures on that second table were cut off in the retrieved snippet) — [Query Intent Detection using Convolutional Neural Networks, QRUMS 2016](https://people.cs.pitt.edu/~hashemi/papers/QRUMS2016_HBHashemi.pdf)
- Production search intent: offline accuracy gains over the production baseline of +4.14% (CNN) and +7.11% (LSTM) on English, with much larger F1 gains on the company class, and +8.20% accuracy for a BERT-based model on a second task; online, +0.43% search-session success for English. Note the baseline was a linear model, not a pure keyword rule — [Deep Search Query Intent Understanding (arXiv 2008.06759)](https://arxiv.org/pdf/2008.06759)
- Keyword rules' characteristic failure is recall on novel phrasing: a rule baseline's recall was 0.34 against precision 0.45 in the table above — [QRUMS 2016](https://people.cs.pitt.edu/~hashemi/papers/QRUMS2016_HBHashemi.pdf)
- Rule-based methods run in microseconds but "break on any query that deviates from expected phrasing" — [trickywombat.ai](https://www.trickywombat.ai/our-technology/query-intent) (marketing claim, not measurement)
- An SEO practitioner guide argues a rule system's value comes precisely from being tuned to the niche, which is also what makes it auditable and defensible — [MLforSEO](https://www.mlforseo.com/machine-learning-implementation-guides/serp-analysis/search-intent-classification-with-machine-learning-approaches/) (practitioner, not measurement)
- For contrast, the cheap *learned* option that needs training data beats the LLM on in-scope accuracy but loses catastrophically on the none-class: TF-IDF+LR OOS recall 36.4 / F1 51.7 vs Claude 0-shot 85.6 / 85.0 — [arXiv 2608.20371](https://arxiv.org/html/2608.20371)

### Inferences
- The published rule-vs-learned gains (~6 points accuracy, ~10 points F) are *smaller than this project's current defect*. A 4-regex list firing 19 times with 4 true positives is 21% precision — far below the ~45% precision of the published "carefully designed" rule baseline. So the realistic gain here is much larger than the literature's 6 points, because the incumbent is well below a competent rule baseline, not because learned classifiers are strong.
- Conversely: a *better regex list* is an under-explored cheap win. The literature's rule baselines reach 75–77% accuracy with precision ~0.45; the current list is at 0.21. Some of the gap is list quality (e.g. `\bwhy\b` fires on "why does X return Y" which is a behaviour question, not a rationale question), which is fixable without any model. The honest comparison for the ratchet is three-way: current regexes, repaired regexes, LLM classifier.
- The strongest single argument for the model-based route is not accuracy but **schema agility and OOS**: 85.0 vs 73.0 OOS F1, plus 94.3/93.9 vs 96.8/0.0 when the label schema changes. A spec archive's section vocabulary will change; a regex list and a trained encoder both have to be rewritten/retrained, a prompted label set does not.

### Gaps
- I found no study that reports precision/recall for *intent routing* in RAG specifically (as opposed to intent classification accuracy). The project's own 4/19 measurement appears to be better evidence than anything published for this exact decision.
- No measurement of how much of a regex router's error is fixable by improving the regexes vs inherent to lexical matching.
