# The Cost of Putting a Language Model in the Retrieval Path

All prices observed **2026-10-27** unless stated otherwise. All latency figures carry hardware and model where the source gave them; where the source did not, that is flagged.

**Standing token assumptions** used throughout for the "one short classification call" unit of work, so every number below is comparable:

- **Prompt:** ~250 tokens of fixed instruction (route definitions, 3-4 few-shot examples, output schema) + ~20 tokens of user query = **270 input tokens**.
- **Output:** ~15 tokens, a single JSON object such as `{"route":"vector","confidence":0.91}`.
- **Rerank unit of work for comparison:** 1 query against **50 passages** of ~150 tokens each.

These are my assumptions, not a measured workload. Every derived figure below is linear in them, so halving the prompt halves the input cost.

---

## Latency budgets: small instruct model doing short classification via Ollama

### Takeaway

On an RTX 5090 a 270-token prompt plus 15 output tokens from an 8B model is roughly **100-130 ms of model compute**, dominated by generation rather than prompt processing; on Apple Silicon the same work is plausibly **0.4-1.1 s** for an 8B and only safely sub-200 ms if you drop to a 1-3B model. Apple Silicon prompt-processing rates for 8B models are the single worst-sourced number in this whole note. Cold start adds **1.8-10 s** for a small model, which `keep_alive` eliminates entirely at the price of permanently resident memory.

### Cited Findings

**RTX 5090 (consumer GPU tier)**

- Llama-3.1-Tulu-3-8B **Q8_0**, llama.cpp b4397: **11,198.06 tokens/sec prompt processing** at 2048-token batch (SE +/- 3.12, N = 3) and **158.57 tokens/sec** text generation — [OpenBenchmarking.org result 2501259-PTS-NVIDIALL46](https://mail.openbenchmarking.org/result/2501259-PTS-NVIDIALL46)
- Mistral-7B **Q8_0** on the same scoreboard: ~**166 tokens/sec** generation; RTX 5090 prompt processing was **1.17x** the RTX 4090 — [Phoronix, llama.cpp AI performance with the GeForce RTX 5090](https://www.phoronix.com/review/nvidia-rtx5090-llama-cpp/2)
- Qwen3 8B **Q4_K** at 4K context: ~**11,933 tokens/sec** prompt processing, ~**200 tokens/sec** generation; generation falls to **59 tokens/sec at 128K** context — [Hardware Corner, RTX 5090 LLM benchmark results](https://www.hardware-corner.net/rtx-5090-llm-benchmarks/)
- A May 2026 community briefing claims 9,300 tok/s pp and 273 tok/s tg on the 5090 after a CUDA header patch; **I could not verify this against a primary source** — reported as unverified by [the search summary of ai-feed.ngrok.app](https://ai-feed.ngrok.app/scripts/script-2026-05-28-local.txt)

**Apple Silicon (laptop tier)**

- llama.cpp community benchmark, **Llama 2 7B Q4_0**, 512-token prompt / 128-token generation: 40-core **M4 Max ~83 tok/s** generation, 32-core binned part **~70 tok/s** — [llama.cpp Apple Silicon performance thread, as reported via markaicode](https://markaicode.com/benchmarks/ollama-llama-31-m4-max-throughput-benchmark/)
- Contributor post (Jan 2026, llama.cpp build 8e672ef), 7B model on M4 Max: **Q4_0 ~69.95 tok/s** vs **Q8_0 ~43.87 tok/s** generation — [markaicode, llama.cpp Q4_K_M vs Q8_0 on M4 Max](https://markaicode.com/benchmarks/llamacpp-phi-3-m4-max-throughput-benchmark/)
- **Qwen3-8B 4-bit on M4 Max: 76.9 tok/s on llama.cpp vs 93.3 tok/s on MLX**; llama.cpp trails MLX for models under ~14B, gap closing above — [SitePoint, local LLMs on Apple Silicon 2026](https://www.sitepoint.com/local-llms-apple-silicon-mac-2026/)
- Qwen3 8B via **Ollama's MLX engine** on M4 Max 30-core GPU (June 2026 test): **112 tok/s** generation — [markaicode, Ollama Qwen 3 M4 Max latency benchmark](https://markaicode.com/benchmarks/ollama-qwen-3-m4-max-latency-benchmark/)
- **M1 Max prefill** at 8.5K context: llama.cpp/GGUF **37.8 s** vs MLX 49.4 s (model not stated in the source) — implies ~225 tok/s prompt eval — [SitePoint](https://www.sitepoint.com/local-llms-apple-silicon-mac-2026/)
- An aggregator lists Llama 3.1 8B on M4 Max at **~82.6 tok/s decode and TTFT 2,344 ms at Q4_K_M**, but names no backend and no prompt length; the site presents it as an **estimate** — [willitrunai, Llama 3.1 8B on M4 Max 48GB](https://willitrunai.com/can-run/llama-3.1-8b-on-m4-max-48gb)
- A Hugging Face imatrix log shows **~899 tokens/sec prompt eval** for an 8B Llama 3.1 run, **hardware not stated**, so it cannot be attributed to Apple Silicon — [HF legraphista/Meta-Llama-3.1-8B-Instruct-IMat-GGUF imatrix.log](https://huggingface.co/legraphista/Meta-Llama-3.1-8B-Instruct-IMat-GGUF/blob/6a31be1a2d8a11245c4f418df83fde6a295d80f9/imatrix.log)
- Prompt processing on Apple Silicon scales with **GPU core count**; text generation scales with **memory bandwidth** — [llama.cpp benchmark thread guidance, via markaicode](https://markaicode.com/benchmarks/ollama-llama-31-m4-max-throughput-benchmark/)

**Cold start / model load**

- Ollama default: "**By default models are kept in memory for 5 minutes before being unloaded**"; a **negative** `keep_alive` keeps it loaded indefinitely, **0** unloads right after the response, and a per-request `keep_alive` overrides the global `OLLAMA_KEEP_ALIVE` — [Ollama FAQ](https://docs.ollama.com/faq)
- Preloading is supported: send an **empty request** to `/api/generate` or `/api/chat` with the model name, or `ollama run <model> ""` — [Ollama FAQ](https://docs.ollama.com/faq)
- Measured cold loads (all blog-grade, differing hardware/storage): **1.8 s** for Phi-3-mini Q4_K_M on an RTX 4090 (Ollama v0.1.32 — the source itself flags this version as outdated) — [markaicode, Ollama Phi-3 RTX 4090 latency benchmark](https://markaicode.com/benchmarks/ollama-phi-3-rtx-4090-latency-benchmark/); **~2.3 s** for Llama 3.1 8B on an RTX 4090 — [markaicode, Ollama vs vLLM latency](https://markaicode.com/vs/ollama-vllm-latency-production-benchmark/); **~3-10 s** generic 7B, and **70B at ~18 s from NVMe vs ~74 s from SATA SSD** — [Leanpub, Benchmarking model inference speeds](https://leanpub.com/read/ollama/benchmarking-model-inference-speeds)
- Warm time-to-first-token on consumer GPUs reported at **0.3-0.4 s** (0.35 s median TTFT on an RTX 4090) — [markaicode, Ollama vs vLLM latency](https://markaicode.com/vs/ollama-vllm-latency-production-benchmark/). Note this is an order of magnitude above the ~23 ms of raw prompt-processing compute the 5090 figures imply, i.e. **Ollama/HTTP overhead dominates the prompt side for a short prompt.**

### Inferences

- **RTX 5090, 8B Q4/Q8 model compute:** prompt 270 / ~11,500 tok/s = **~23 ms**; generation 15 / ~180 tok/s = **~83 ms**. Total **~106 ms of model compute.** Generation is ~4x the prompt cost, so *shortening the output matters more than shortening the prompt* — pushing the classifier to emit a bare label instead of a JSON object with a confidence field is the single cheapest latency win available.
- Adding the reported 0.3-0.4 s warm TTFT floor, a realistic **end-to-end 5090 figure is ~130-400 ms**, with the spread governed by server overhead rather than by the model. The 0.35 s median TTFT datapoint is from a 4090 and a different model, so treat ~130 ms as the compute-bound floor and ~400 ms as the observed-stack ceiling.
- **Apple Silicon, 8B Q4:** generation 15 / ~77 tok/s = **~195 ms**. Prompt processing is the unknown; at the 225-899 tok/s bracket the sources admit, 270 tokens is **300 ms to 1.2 s**. So **~0.5-1.4 s total** for an 8B, which is a *user-visible* addition to a retrieval path. Dropping to a 1-3B instruct model should cut generation roughly 3-5x by parameter count, but **I found no M-series benchmark for a 1-3B instruct model to confirm this**.
- `keep_alive` buys exactly one thing: it converts a **1.8-10 s** cold load into **0 ms**, at the cost of holding the model's full weights resident for the whole idle window. For a routing classifier on bursty traffic with a 5-minute default, every gap longer than 5 minutes pays the full reload. A negative `keep_alive` (indefinite) is the right setting for a classifier precisely because it is small and called on every query.
- **Model swapping is the failure mode that destroys the budget.** If the classifier, embedder and reranker do not all fit resident, each query pays a load. Three evictions per query at even 2 s each is 6 s — fifty times the model compute. This makes memory headroom, not tokens/sec, the real constraint on the laptop tier.

### Gaps

- **No primary `llama-bench` pp512/tg128 table for M1-M4 at 8B** was retrievable; the llama.cpp GitHub benchmark megathread is the right primary source and I could not reach it. The Apple Silicon prompt-processing number should be measured locally (`llama-bench -m <model>.gguf -p 512 -n 128 -ngl 99`) before being published.
- **No benchmark at all for 1B-3B instruct models on either tier** — the whole corpus of published figures is 7B/8B. The "use a small model for routing" recommendation is therefore unquantified.
- No measurement of Ollama's own per-request HTTP/scheduling overhead isolated from model compute. The 0.3-0.4 s TTFT figures bundle them.

---

## Concurrency and memory: three models resident on 24-32GB

### Takeaway

Ollama will hold multiple models only if they **all fully fit**, and its default `OLLAMA_MAX_LOADED_MODELS` of 3 per GPU happens to be exactly the number a classifier + embedder + reranker pipeline needs. At Q4_K_M three 8B models are ~14 GiB of weights before KV cache, which fits 32GB comfortably and 24GB tightly; parallelism multiplies the context memory, not just the weights.

### Cited Findings

- `OLLAMA_MAX_LOADED_MODELS` — "the maximum number of models that can be loaded concurrently provided they fit in available memory. **The default is 3 * the number of GPUs or 3 for CPU inference**" — [Ollama FAQ](https://docs.ollama.com/faq)
- `OLLAMA_NUM_PARALLEL` — "the maximum number of parallel requests each model will process at the same time, **default 1**" — [Ollama FAQ](https://docs.ollama.com/faq)
- `OLLAMA_MAX_QUEUE` — "**the default is 512**"; beyond that "the server returns a **503** error" — [Ollama FAQ](https://docs.ollama.com/faq)
- Memory scaling: "**RAM scales with `OLLAMA_NUM_PARALLEL` * `OLLAMA_CONTEXT_LENGTH`**. For example, a 2K context with 4 parallel requests becomes an 8K context." With GPU inference, **a new model must fully fit in VRAM to load concurrently** — [Ollama FAQ](https://docs.ollama.com/faq)
- Windows with Radeon GPUs **defaults to one model at a time** — [Ollama FAQ](https://docs.ollama.com/faq)
- If a new model will not fit, Ollama **unloads idle models to make room** and the request is **queued** until the model is loaded; raising `MAX_LOADED_MODELS` only helps if VRAM can hold them all. In one worked example an 8B plus a 32B do not both fit and Ollama **evicts one on every switch** — [eastondev, Ollama multi-model deployment](https://eastondev.com/blog/en/posts/ai/20260406-ollama-multi-model-deployment/); corroborated by [DEV, Ollama keeps reloading the model](https://dev.to/jovan_chan_9500711396d4e6/ollama-keeps-reloading-the-model-fix-vram-unloading-cold-starts-and-model-swapping-2026-25b7)
- Model file sizes, Llama-3.1-8B: **Q4_K_M 4.58 GiB**, **Q8_0 7.95 GiB** — [bmdpat, q4_k_m vs q8_0](https://bmdpat.com/blog/q4-k-m-vs-q8-0-which-gguf-quant-should-you-use-2026); a generic 7B table gives **~4.1 GB (Q4_K_M) vs ~7.0 GB (Q8_0)** against 7.0 GB fp16 — [GGUF Quantization Guide](https://czmissioncontrol.duckdns.org/skills/mlops/inference/llama-cpp/references/quantization.md)

### Inferences

- **RTX 5090 (32 GB):** Qwen3-Embedding-8B + Qwen3-Reranker-8B + an 8B classifier at **Q4_K_M** is ~**13.7 GiB** of weights, leaving ~18 GiB for KV caches and framework overhead. Comfortable. At **Q8_0** it is ~**23.9 GiB**, leaving ~8 GiB — workable but no longer comfortable once reranking 50 passages fills context. **A mixed policy (Q8_0 for embedder and reranker, where published retrieval numbers were measured at fp16; Q4_K_M for the classifier, where the task is a short label) is the configuration the memory arithmetic actually points to.**
- **24 GB cards:** ~13.7 GiB of Q4 weights leaves ~10 GiB, and the reranker's context for 50 passages is the thing that will push it over. Three 8B models at Q8_0 **do not fit** 24 GB.
- **Apple Silicon unified memory** is the easier case for capacity (a 32-48 GB machine holds all three) and the harder case for speed, since the three models now contend for the same memory bandwidth that generation speed depends on.
- `OLLAMA_NUM_PARALLEL=1` (the default) means **the classifier serialises behind the reranker** if both are called in one request flow. That is fine for a sequential pipeline, but a service handling concurrent users will queue — and the 512-deep queue means requests fail with 503 rather than degrading gracefully. Raising `NUM_PARALLEL` multiplies context memory per the FAQ formula, so it trades directly against the three-models-resident budget.
- Because a mis-sized set of models causes **eviction on every switch** rather than an error, the symptom of under-provisioned VRAM is a silent 10-50x latency regression, not a crash. This is the most important operational point in this section.

### Gaps

- **No published memory footprint for Qwen3-Embedding-8B or Qwen3-Reranker-8B GGUF quants specifically.** I used Llama-3.1-8B as a size proxy; actual files may differ, particularly for the reranker's head.
- No source measured the **wall-clock cost of an Ollama model swap** under VRAM pressure (as distinct from a cold load from disk), so the "eviction thrash" penalty is inferred from cold-load figures.
- No source on `nomic-embed-text` or `ms-marco-MiniLM-L-6-v2` resident footprint — these are small enough (hundreds of MB) that the laptop tier's current configuration is almost certainly not memory-bound, but I have no citation.

---

## Quantisation: accuracy cost for short classification specifically

### Takeaway

Q8_0 is effectively lossless and Q4_K_M costs a small, consistently-reported penalty — but **every number I found is perplexity or general task-average, not short classification**. The one encouraging signal is that *downstream task gaps are consistently smaller than perplexity gaps*, which is the right direction for a constrained-output task.

### Cited Findings

- Generic 7B, fp16 baseline perplexity **5.9565**: **Q8_0 ~+0.03%**, **Q4_K_M ~+1.7%** — [GGUF Quantization Guide](https://czmissioncontrol.duckdns.org/skills/mlops/inference/llama-cpp/references/quantization.md). *Note: a second site carries the identical table, so this is one source, not two.*
- Llama-3 8B, llama.cpp perplexity readme as cited: **Q8_0 PPL 6.234284**, **Q4_K_M PPL 6.407115** (lower is better) — [bmdpat](https://bmdpat.com/blog/q4-k-m-vs-q8-0-which-gguf-quant-should-you-use-2026)
- **Task benchmarks are much closer than perplexity:** an arXiv study (2601.14277) as cited reports task averages of **Q4_K_M 69.15** vs **Q8_0 69.41** — a 0.26-point gap — [bmdpat](https://bmdpat.com/blog/q4-k-m-vs-q8-0-which-gguf-quant-should-you-use-2026). I did not fetch the paper itself.
- Community eval: Q8_0 beat Q4_K_M on **HumanEval by ~1.8 points** but **scored lower on HellaSwag**, while being slower and using more RAM — a single anecdotal run — [bmdpat](https://bmdpat.com/blog/q4-k-m-vs-q8-0-which-gguf-quant-should-you-use-2026)
- Q4_K_M is the usual default recommendation for quality/speed balance — [GGUF Quantization Guide](https://czmissioncontrol.duckdns.org/skills/mlops/inference/llama-cpp/references/quantization.md); [webscraft, GGUF quantization for Ollama](https://webscraft.org/blog/kvantuvannya-gguf-dlya-ollama-scho-oznachayut-q4km-q80-ta-iq4xs-yake-vibrati-pid-svoye-zalizo?lang=en)
- Throughput cost of the higher quant on Apple Silicon is real: 7B on M4 Max, **Q4_0 ~69.95 tok/s vs Q8_0 ~43.87 tok/s** — a **~37% generation slowdown** — [markaicode](https://markaicode.com/benchmarks/llamacpp-phi-3-m4-max-throughput-benchmark/)

### Inferences

- The **perplexity-vs-task divergence is the load-bearing finding**: a ~1.7% perplexity penalty corresponding to a ~0.4% task-average penalty suggests quantisation damage falls mostly on the long tail of the distribution, which a classification task with 3-5 permitted labels largely does not sample. This supports Q4_K_M for a router, but it is an argument from mechanism, not measurement.
- The HellaSwag/HumanEval inconsistency is a warning that **per-task behaviour is not monotonic in bit width**. A published classification number measured at fp16 cannot be assumed to transfer to Q4_K_M; it has to be re-measured on the quant you ship.
- Because the output is a constrained label, **grammar-constrained decoding (GBNF / Ollama's structured output) removes the format-failure mode that quantisation would otherwise aggravate**, leaving only label-choice error. That changes what you need to measure: not "did it produce valid JSON" but "did it pick the right route". I found no source quantifying this.

### Gaps

- **I found no benchmark of quantisation effect on short classification or intent-routing accuracy for small instruct models.** This is a genuine hole in the published literature as far as these searches reached, and should be stated as such rather than papered over. The honest recommendation is a differential run: the same labelled routing set through fp16, Q8_0 and Q4_K_M of the same model, reporting per-label confusion.
- The arXiv citation (2601.14277) is second-hand; I did not verify the paper exists or that the numbers are quoted correctly.
- No data on quantisation effects for **embedding or reranker** models specifically, which matters because the 5090 tier's published Qwen3-Embedding/Reranker retrieval numbers are fp16.

---

## Third-party pricing, current (observed 2026-10-27)

### Takeaway

A short classification call on the cheapest viable hosted models costs **$0.02-0.04 per 1,000 queries**, falling to about **$0.012 with prompt caching**. Hosted *reranking* is one to two orders of magnitude more expensive per query: **$2.00 per 1,000 searches** for Cohere, **~$0.43 per 1,000** for token-billed Voyage rerank-2 on a 50-passage set.

### Cited Findings

**Classification-capable small models (per 1M tokens, standard tier)**

| Model | Input | Output | Source |
|---|---|---|---|
| `gpt-5-nano` | $0.05 | $0.40 | [OpenAI pricing](https://developers.openai.com/api/docs/pricing) |
| `gpt-4.1-nano` | $0.10 | $0.40 | [OpenAI pricing](https://developers.openai.com/api/docs/pricing) |
| `gpt-6-luna` | $0.10 | $0.50 | [OpenAI pricing](https://developers.openai.com/api/docs/pricing) |
| `gpt-4o-mini` | $0.15 | $0.60 | [OpenAI pricing](https://developers.openai.com/api/docs/pricing) |
| `gpt-5.4-nano` | $0.20 | $1.25 | [OpenAI pricing](https://developers.openai.com/api/docs/pricing) |
| `gemini-2.5-flash-lite` | $0.10 | $0.40 | [Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing) |
| `gemini-3.1-flash-lite` | $0.25 | $1.50 | [Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing) |
| `gemini-3.5-flash-lite` | $0.30 | $2.50 | [Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing) |
| `gemini-2.5-flash` | $0.30 | $2.50 | [Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing) |
| Claude Haiku 5.5 (prompt <= 100K) | $0.10 | $0.50 | [Claude pricing](https://platform.claude.com/docs/en/about-claude/pricing) |
| Claude Haiku 5.5 (prompt > 100K) | $0.50 | $2.50 | [Claude pricing](https://platform.claude.com/docs/en/about-claude/pricing) |
| Claude Haiku 4.5 | $1.00 | $5.00 | [Claude pricing](https://platform.claude.com/docs/en/about-claude/pricing) |

- **Note on the price trend:** the newest small models are *more* expensive than their predecessors, not less — `gemini-2.5-flash-lite` ($0.10/$0.40) is cheaper than `gemini-3.5-flash-lite` ($0.30/$2.50), and `gpt-5-nano` ($0.05/$0.40) is cheaper than `gpt-5.4-nano` ($0.20/$1.25) — [Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing); [OpenAI pricing](https://developers.openai.com/api/docs/pricing). A routing classifier is one of the few workloads where staying on an older small model is defensible on price alone.
- **Prompt caching (Claude Haiku 5.5):** 5m cache write **$0.125/MTok**, 1h cache write **$0.20/MTok**, cache hits and refreshes **$0.01/MTok** — [Claude pricing](https://platform.claude.com/docs/en/about-claude/pricing)
- **Batch API:** 50% discount on input and output (Haiku 5.5 batch: **$0.05 / $0.25** per MTok) — [Claude pricing](https://platform.claude.com/docs/en/about-claude/pricing). Not applicable to a synchronous retrieval path, but relevant if routing decisions are precomputed for a known query set.
- **US-only inference** via `inference_geo` carries a **1.1x multiplier** on all token categories for Claude 4.6+ models; global routing is standard price — [Claude pricing](https://platform.claude.com/docs/en/about-claude/pricing)
- Cohere's **legacy** per-token pricing (existing customers only): Command R 03-2024 $0.50/$1.50, Aya Expanse 8B/32B $0.50/$1.50 per MTok — [Cohere pricing](https://cohere.com/pricing). The page **no longer publishes per-token rates for current Command models, nor a per-search Rerank price, nor per-token Embed prices.**

**Hosted reranking**

- **Cohere Rerank 3.5 on AWS Bedrock: $2.00 per 1,000 queries** — [AWS Bedrock pricing](https://aws.amazon.com/bedrock/pricing/) *(the firmest rerank price I obtained from a first-party page)*
- Cohere's own definition: a **search unit is one query with up to 100 documents**; the page gives the definition but **no dollar figure** — [Cohere pricing](https://cohere.com/pricing)
- Blog-grade figures for Cohere's current generation: **Rerank 4 Fast $2.00 / Rerank 4 Pro $2.50 per 1,000 searches**; longer documents are **chunked and each chunk counts toward the 100** — [Puter developer, Cohere API pricing (Jun 2026)](https://developer.puter.com/tutorials/cohere-api-pricing/). **Conflicting:** DeployBase gives $1.00 per 1,000 queries for Rerank v3 — [DeployBase](https://deploybase.ai/articles/cohere-pricing); a Learning Engineering page gives "~$2 per million tokens of reranked text", contradicting the per-search model entirely — [learn.engineering.vips.edu](https://learn.engineering.vips.edu/ai-models/cohere-rerank-3). **I would discount both of the latter.**
- **Voyage (official):** reranking is billed on **total processed tokens = query_tokens x num_documents + all document tokens**. **rerank-2: $0.05 per 1M tokens. rerank-2-lite: $0.02 per 1M tokens.** First **200M tokens free per account** for both — [Voyage AI pricing](https://docs.voyageai.com/docs/pricing)
- Voyage **rerank-3 at $0.05/1M input** appears only on a third-party calculator and **not** on Voyage's own page — [Maxim Bifrost model library](https://www.getmaxim.ai/bifrost/model-library/compare/voyage/rerank-3.md)
- **AWS Bedrock Amazon Rerank:** AWS docs confirm the **billing model** — "a query is a single call to the reranker model that can contain **up to 100 document chunks**", surfaced as the **`SearchUnits`** CloudWatch metric — but **do not state the rate** — [AWS, Rerank pricing and search units](https://docs.aws.amazon.com/bedrock/latest/userguide/rerank-pricing.html). A third-party comparison dated 2026-10-18 gives **$1.00 per 1,000 queries**, and a LiteLLM test asserts $0.001 per search unit, which matches — [DevelopersDigest, Amazon Bedrock reranker](https://www.developersdigest.tech/models/rerankers/amazon-bedrock); [LiteLLM commit](https://git.kvant.cloud/phoenix/litellm-mirror/commit/4337f1657e13a6d35527a400e3be17c11d4b662b). **Unconfirmed against AWS's own pricing page.**
- **Jina:** the reranker page states pricing "follows the same **token-based** structure as the Embeddings API", with tokens **shared across all Jina APIs on the same key**, but **publishes no rate** — [Jina Reranker](https://jina.ai/reranker/). Aggregators conflict: $0.05/1M vs $0.02/1M for `jina-reranker-v2-base-multilingual` — [Maxim comparison](https://www.getmaxim.ai/bifrost/model-library/compare/cohere/rerank-3-vs-jina-reranker-v2). **Treat Jina rerank pricing as unestablished.**

**Hosted embeddings**

- `text-embedding-3-small` **$0.02/1M**, `text-embedding-ada-002` $0.10/1M, `text-embedding-3-large` $0.13/1M — [OpenAI pricing](https://developers.openai.com/api/docs/pricing)
- `gemini-embedding-2` text input **$0.20/1M** (batch $0.10/1M) — [Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing)
- Cohere Embed English/Multilingual on Bedrock: on-demand example at **$0.0001 per 1,000 tokens = $0.10/1M**; provisioned throughput e.g. Embed 3 English **$7.12/hour** no-commitment — [AWS Bedrock pricing](https://aws.amazon.com/bedrock/pricing/)
- Cohere **Model Vault** dedicated instances (not per-token): Embed 5 Fast/Pro **$3.00/hr small, $5.00/hr medium** ($2,000 / $3,250 monthly); Rerank 5 Fast/Pro **$4.00/hr medium** ($2,500 monthly) — [Cohere pricing](https://cohere.com/pricing)

### Inferences — cost per 1,000 queries

Using the standing assumptions (270 input / 15 output tokens per classification; 50 passages x 150 tokens per rerank). **These are my arithmetic, not quoted figures.**

**One classification call per 1,000 queries:**

| Model | Input cost | Output cost | **Total / 1,000 queries** |
|---|---|---|---|
| `gpt-5-nano` | 0.27 MTok x $0.05 = $0.0135 | 0.015 x $0.40 = $0.0060 | **$0.0195** |
| Claude Haiku 5.5 | 0.27 x $0.10 = $0.0270 | 0.015 x $0.50 = $0.0075 | **$0.0345** |
| `gemini-2.5-flash-lite` | 0.27 x $0.10 = $0.0270 | 0.015 x $0.40 = $0.0060 | **$0.0330** |
| `gpt-4.1-nano` | $0.0270 | $0.0060 | **$0.0330** |
| `gemini-3.5-flash-lite` | 0.27 x $0.30 = $0.0810 | 0.015 x $2.50 = $0.0375 | **$0.1185** |
| Claude Haiku 4.5 | 0.27 x $1.00 = $0.2700 | 0.015 x $5.00 = $0.0750 | **$0.3450** |

**With prompt caching (Haiku 5.5, 250 of 270 input tokens cached):** 0.25 MTok x $0.01 + 0.02 MTok x $0.10 + 0.015 MTok x $0.50 = $0.0025 + $0.0020 + $0.0075 = **$0.012 per 1,000 queries** — a ~65% reduction, because the fixed instruction block is the bulk of the prompt. Caching is the single highest-leverage lever on a hosted router, and it is available precisely because a router's prompt is almost entirely fixed.

**One rerank over 50 passages per 1,000 queries:**

- **Cohere Rerank (per-search, Bedrock): 50 passages = 1 search unit → $2.00 per 1,000 queries.** Per-search billing means reranking 10 passages costs the same as 100.
- **Voyage rerank-2 (token-billed):** 20 query tokens x 50 docs + (50 x 150) doc tokens = 1,000 + 7,500 = **8,500 tokens/query** → 8.5 MTok per 1,000 queries x $0.05/MTok = **$0.425 per 1,000 queries.**
- **Voyage rerank-2-lite:** 8.5 MTok x $0.02 = **$0.17 per 1,000 queries.**
- **Amazon Rerank (if the unofficial $1.00/1,000 holds): $1.00 per 1,000 queries.**

**One query embedding per 1,000 queries:** 20 tokens x 1,000 = 0.02 MTok. `text-embedding-3-small`: **$0.0004.** Negligible at any price on the page.

### Gaps

- **Cohere's own pricing page no longer publishes per-search Rerank or per-token Embed rates** for current models, pushing you to Bedrock's page or to sales. The $2.00/1,000 I use is Bedrock's figure for Rerank **3.5**, not Cohere's current Rerank 4/5 generation.
- **No first-party price found for Amazon Rerank or any Jina reranker.** Both billing *models* are documented; neither *rate* is.
- No first-party Titan embedding or Amazon Nova price was readable from the Bedrock pricing page in the portion fetched (the sections rendered without figures).
- Aggregator searches returned several model names I could not corroborate on any first-party page (e.g. "Perplexity Decider Flash v1.2 4B", "Ling 3.0 Flash", "DeepSeek V4 Flash"). **I have excluded all of them.** Only prices read off a provider's own documentation are in the table above.

---

## The marginal-cost framing: classifier vs the rerank already in the path

### Takeaway

Against **per-search** hosted reranking the classification call is a **rounding error (~1.7% of rerank cost)**. Against **token-billed lite** reranking it is **material (~20%)**. On *latency*, local tiers invert the conclusion: on Apple Silicon the classifier can cost more wall-clock than the rerank it precedes.

### Inferences (all arithmetic mine, from the cited unit prices above)

**Cost, per 1,000 queries, hosted:**

| Path component | Cost / 1,000 q | Share |
|---|---|---|
| Query embedding (`text-embedding-3-small`) | $0.0004 | 0.02% |
| Classification (Haiku 5.5, uncached) | $0.0345 | 1.7% |
| Classification (Haiku 5.5, cached) | $0.0120 | 0.6% |
| Rerank, 50 passages (Cohere 3.5 on Bedrock) | $2.0000 | 98.3% |
| **Total (uncached classifier)** | **$2.0349** | |

- **Classifier / rerank = 0.0345 / 2.00 = 1.7%.** Against Cohere's per-search billing, the classification call is **unambiguously a rounding error** — you could add twenty of them before the rerank step noticed.
- Against **Voyage rerank-2** ($0.425): 0.0345 / 0.425 = **8.1%** — still small.
- Against **Voyage rerank-2-lite** ($0.17): 0.0345 / 0.17 = **20.3%** — **material**. A team that has already optimised to the cheapest token-billed reranker cannot wave the classifier through.
- The framing therefore **depends entirely on which reranker is in the path**, and the spread between the cheapest and dearest hosted rerank options (**$0.17 to $2.00 per 1,000**, a 12x range) is far wider than the cost of the classifier itself. *The honest version of the claim is: the classifier is free compared to the reranker you probably chose, and the reranker choice is where the money actually is.*
- **Asymmetric expensive-model risk:** if the router is Claude Haiku **4.5** rather than **5.5**, its cost rises 10x to $0.345/1,000 — 17% of a Cohere rerank, and **twice** a Voyage rerank-2-lite. Model selection within a vendor's "small" tier swings the marginal cost by an order of magnitude.

**Latency, per query:**

| Tier | Rerank, 50 passages | Classification (270 in / 15 out) | Classifier as share |
|---|---|---|---|
| RTX 5090, 8B models | prompt-dominated: ~7,500 passage tokens / ~11,500 tok/s ~= **650 ms** | **~106 ms** compute | **~16%** |
| Apple Silicon, 8B models | ~7,500 / 225-899 tok/s = **8.3-33 s** (!) | **~0.5-1.4 s** | ~4-17% |
| Hosted | network + provider latency (unmeasured here) | network + provider latency | — |

- On the **5090**, a cross-encoder rerank of 50 passages is prompt-processing-bound and the classifier's 15 generated tokens are a genuine but modest ~16% addition. **Generation, not prompt processing, is what the classifier adds** — the opposite of the rerank step's profile, which is why the two costs do not simply add in the way a token count would suggest.
- The Apple Silicon rerank estimate above is derived from the *same badly-sourced* prompt-eval bracket as the classifier estimate, so the **ratio** is more trustworthy than either absolute number. The absolute figures suggest an 8B cross-encoder reranking 50 passages on a laptop is already outside an interactive budget, which would make the classifier the least of that tier's problems — consistent with the laptop tier using `ms-marco-MiniLM-L-6-v2` (a ~22M-parameter cross-encoder) rather than an 8B reranker.
- **The genuinely load-bearing conclusion for the series:** on the 5090 and hosted tiers, one classification call is cheap in both money and time, and the decision should be made on *accuracy and operational risk*, not cost. On the laptop tier the classifier is the one new resident model that may not fit alongside the others, and **eviction thrash — not tokens — is what will blow the budget.**

### Gaps

- I have **no measured latency for a cross-encoder rerank of 50 passages on either tier**; the figures above are derived from general prompt-processing rates and assume rerank scoring is prompt-bound with negligible generation. For `ms-marco-MiniLM-L-6-v2` specifically (which is not an autoregressive model and is not served the same way) this derivation does not apply at all.
- No measurement of hosted-API round-trip latency for any of the priced models, so the hosted row of the latency table is empty. This is a real hole for the "is it a rounding error" judgement on the hosted tier.

---

## Caching routing and classification decisions

### Takeaway

Exact-match on a normalised query is cheap and safe; embedding-similarity caching reports **40-70% hit rates for classification/routing specifically** but the ranges conflict between sources and all are vendor or blog figures. The correctness hazard is that a cached *route* can outlive the corpus that justified it.

### Cited Findings

- **Classification and intent routing hit rates:** one source gives **40-60%**, another **50-70%** ("discrete input space, limited query variety"). The two disagree; treat as **~40-70%** and validate against your own logs — [tianpan.co, semantic caching for LLM applications](https://tianpan.co/blog/2026-04-09-semantic-caching-llm-production); [tianpan.co, the cost tier most teams skip](https://tianpan.co/blog/2026-04-10-semantic-caching-llm-production)
- **General production hit rates: 20-45%**, spanning **10% for open-ended chat to 70% for structured FAQ** — [tianpan.co](https://tianpan.co/blog/2026-04-09-semantic-caching-llm-production)
- **Vendor "95%" figures refer to match accuracy on hits, not hit frequency** — [tianpan.co](https://tianpan.co/blog/2026-04-09-semantic-caching-llm-production). This is the most useful caveat in the section: the headline number vendors quote answers a different question than the one you are asking.
- **Research vs production:** a cited GPTSemCache study reported **61.6-68.8% hit rates across query categories with 97%+ accuracy**; sources note production traffic is messier — [tianpan.co](https://tianpan.co/blog/2026-04-09-semantic-caching-llm-production)
- **One analysis of 100,000 production queries: 18% exact duplicates, 47% semantically similar** — the author treats 47% as the practical ceiling for that workload — [tianpan.co](https://tianpan.co/blog/2026-04-10-semantic-caching-llm-production)
- **Failure mode:** a poor embedding model maps semantically distinct queries to nearby vectors and returns a wrong cached response; thresholds are typically tuned around **cosine similarity > 0.8-0.85** — [tianpan.co](https://tianpan.co/blog/2026-04-09-semantic-caching-llm-production); [Portkey, reducing LLM costs and latency with semantic cache](https://portkey.ai/blog/reducing-llm-costs-and-latency-semantic-cache/)
- Standard practice is to **analyse a week of query logs before estimating hit rate** — [tianpan.co](https://tianpan.co/blog/2026-04-09-semantic-caching-llm-production)

### Inferences

- At a **47% hit rate** (the one figure backed by a stated query volume), caching halves the classifier's marginal money and latency: Haiku 5.5 falls from $0.0345 to **~$0.018 per 1,000 queries**, and the average added latency on the 5090 from ~106 ms to **~56 ms**. Combined with prompt caching, the hosted router's cost approaches **~$0.006 per 1,000 queries**.
- **Caching a routing decision is materially safer than caching a generated answer**, and this asymmetry deserves emphasis: the cached value is a **small enum**, not prose. A false cache hit mis-routes a query — degrading retrieval, which the rest of the pipeline may still recover from — rather than returning a confidently wrong answer. That makes a *more aggressive* similarity threshold defensible for routing than the 0.8-0.85 the sources quote for response caching.
- **The corpus-change hazard is real but narrow.** A route is a decision about *which index or strategy to query*, not about *what the answer is*. It goes stale when the **set of routes or their coverage** changes — a new collection added, a collection emptied, a filter's semantics changed — not when individual documents change. The correct invalidation key is therefore a **version of the routing configuration and corpus topology**, not a document checksum or a TTL. Nothing in the sources states this; it follows from what a route is.
- The corollary hazard is **confidence decay**: a classifier that emitted `{"route":"vector","confidence":0.55}` was already near a boundary. Caching the low-confidence decisions is where a cache does the most damage, and declining to cache below a confidence floor costs little because those queries are rare by construction.
- **Exact-match on a normalised query (lowercased, whitespace-collapsed, punctuation-stripped) captures the ~18% duplicate traffic at zero accuracy risk and zero embedding cost.** For a router this should be the default before any similarity cache is considered — it is a hash map, needs no embedding call, and cannot produce a false hit.

### Gaps

- **All hit-rate figures trace to one author's blog posts** (tianpan.co) plus vendor content. I found no peer-reviewed or first-party production study. The 40-70% routing range should be presented as indicative only.
- **No source addressed cache invalidation on corpus change for routing decisions specifically.** My invalidation-key reasoning above is inference, not citation.
- No reported data on whether an embedding-similarity cache for routing is cheaper than the classifier it replaces, once the embedding call and vector lookup are counted. On the laptop tier this is not obviously a win.

---

## Operational reality and determinism

### Takeaway

Determinism is **not achievable** in the strict sense on any batched server, hosted or local: the measured cause is **batch-variant kernels**, not sampling, and fixing it costs **1.6-2.1x throughput**. The larger operational risks are a silent model swap underneath you and a client-side read timeout you did not configure — including a **documented tokeniser change that inflates token counts ~30% for identical text**.

### Cited Findings

**Measured non-determinism (primary source)**

- **Qwen3-235B-A22B-Instruct-2507**, non-thinking mode, one prompt, **1,000 completions at temperature 0**, 1,000 tokens each: **80 unique completions**; the most common appeared **78 times**. The **first 102 tokens were identical**; divergence at **token 103** — all produced "Feynman was born on May 11, 1918, in", after which **992 continued "Queens, New York" and 8 "New York City"** — [Thinking Machines Lab, Defeating nondeterminism in LLM inference](https://thinkingmachines.ai/blog/defeating-nondeterminism-in-llm-inference/)
- **With batch-invariant kernels, all 1,000 completions were identical** — [Thinking Machines Lab](https://thinkingmachines.ai/blog/defeating-nondeterminism-in-llm-inference/)
- **Cost of determinism:** Qwen-3-8B, one GPU, 1,000 sequences of 90-110 output tokens — vLLM default **26 s**, unoptimised deterministic **55 s (~2.1x)**, with an improved attention kernel **42 s (~1.6x)**. The batch-invariant matmul kernel alone loses **~20%** vs cuBLAS — [Thinking Machines Lab](https://thinkingmachines.ai/blog/defeating-nondeterminism-in-llm-inference/)
- **Mechanism:** the forward pass is run-to-run deterministic but **not batch-invariant**; different matmul shapes mean different reduction order, hence bit-different outputs. So the *same* request can yield different results depending on **what else was in the batch** — i.e. on server load you do not control — [spheron.network, vLLM batch invariance](https://www.spheron.network/blog/vllm-batch-invariance-why-temperature-0-isnt-deterministic/); [DEV, why temperature=0 still gives different answers](https://dev.to/ji_ai/why-temperature0-still-gives-different-answers-batch-invariance-49h)
- **Conflicting cost figures:** SGLang's deterministic mode measured at an average **34.35% throughput slowdown (24.4%-46.0%)** against Thinking Machines' originally reported 61.5% overhead — [tianpan.co, temperature zero is not determinism](https://tianpan.co/blog/2026-06-30-temperature-zero-is-not-determinism)
- **Caveat worth flagging:** vLLM's **async scheduling is on by default and can still break batch invariance even with the determinism flag set** — [tianpan.co](https://tianpan.co/blog/2026-06-30-temperature-zero-is-not-determinism)

**Model churn and silent change**

- **Tokeniser change:** "Claude 4.7 and later models and Claude Mythos Preview use a newer tokenizer... This tokenizer produces **approximately 30% more tokens for the same text**." Claude Sonnet 4.6 and earlier use the previous tokeniser — [Claude pricing](https://platform.claude.com/docs/en/about-claude/pricing). **A model upgrade therefore changes your bill for byte-identical traffic.**
- **Retirement is routine:** Claude Sonnet 4 and Claude Haiku 3.5 are marked **retired** (except on certain cloud resellers) on the live pricing page — [Claude pricing](https://platform.claude.com/docs/en/about-claude/pricing)
- **Pricing-shape change, not just rate change:** Claude Haiku 5.5 is **priced by prompt length**, with a cliff above 100,000 input tokens, and "a request over the threshold pays the higher prices **even when part of its prompt is a cache hit**" — [Claude pricing](https://platform.claude.com/docs/en/about-claude/pricing)
- A developer post asserts temperature = 0 never guaranteed identical outputs on prior models either; **I found no primary vendor source for this and treat it as unverified** — [tianpan.co](https://tianpan.co/blog/2026-06-30-temperature-zero-is-not-determinism)

**Timeouts and queueing**

- Ollama returns **503** once `OLLAMA_MAX_QUEUE` (default **512**) is exceeded — [Ollama FAQ](https://docs.ollama.com/faq)
- **Spring AI + Ollama read timeout:** a community write-up reports Spring AI Ollama timing out at **~10 s** against a local model **despite a 120 s timeout configured in `application.yml`**, attributing it to the default **OkHttp read timeout**, which is separate from the model-side option. It further reports that **`spring.restclient.read-timeout` is bypassed because Spring AI Ollama builds its own `RestClient`**, and that **`spring.okhttp.read-timeout` does not take effect either**; the proposed workaround is a **custom `OllamaApi` bean with a longer OkHttp read timeout** — [besthub.dev, how to fix Spring AI Ollama timeout](https://www.besthub.dev/articles/how-to-fix-spring-ai-ollama-timeout-full-guide-to-configuring-okhttp-read-timeout-abd463506d77); same scenario described at [gitcode.csdn.net](https://gitcode.csdn.net/6a2176d410ee7a33f277a3bd.html)
- A Chinese-language article lists `spring.ai.ollama.client.connect-timeout` and `read-timeout` with suggested values of 30 s and 300 s. **I could not confirm these property names against official Spring AI docs** — [deepseek.csdn.net](https://deepseek.csdn.net/6a7d4d73662f9a54cb9c0ffd.html)

### Inferences

- **What is actually achievable on determinism:**
  - **(a) Ollama locally:** with `OLLAMA_NUM_PARALLEL=1` (the default), a pinned model digest, `temperature=0`, fixed `seed`, and pinned `top_p`/`top_k`, the batch composition is **constant at 1**, which removes the specific cause Thinking Machines isolated. This should be *substantially* more reproducible than a hosted endpoint — but it is an argument from mechanism; **I found no measurement of Ollama/llama.cpp run-to-run determinism.** Note also that raising `NUM_PARALLEL` for throughput **reintroduces batch variance**: the determinism and concurrency knobs are in direct conflict.
  - **(b) Hosted APIs:** batch composition depends on other customers' traffic. The Thinking Machines result (**80 unique completions from 1,000 identical temperature-0 requests**) is the honest expectation, and no vendor flag available to an API client can fix it, because the fix is server-side kernels.
  - **Crucially, divergence was at token 103.** A **15-token** classification output sits well inside the 102-token identical prefix of that experiment. This is a single prompt on one model and **must not be generalised into a guarantee** — but it is the most relevant quantitative signal available, and it points the right way: *short, constrained outputs are where non-determinism is least likely to bite.* Constrained decoding over a 3-5 label vocabulary narrows it further, since a logit perturbation must cross the gap to a different permitted token to change the answer.
- **The testing burden** is the real tax, and it is structural: a regex router is a pure function you unit-test exhaustively; a model router is a **statistical component** requiring a labelled routing set, an accuracy threshold in CI, and re-measurement on every model, quant and version change. Pinning the model digest makes the test suite meaningful; without it the suite tests whatever the provider served that morning.
- **Deprecation risk is concrete, not hypothetical** — two Haiku/Sonnet generations are already marked retired on a live pricing page. A hosted router needs the model identifier to be configuration, a fallback path, and the labelled routing set kept runnable, because a forced migration is a question of when.
- **The tokeniser finding is the sharpest operational warning in this note:** a provider can change your cost for identical traffic by ~30% through a model upgrade alone. Any cost model built on token counts must be re-derived, not merely re-priced, when the model changes.
- **JVM/HTTP-side considerations that materially move the numbers:**
  1. **Read timeout must exceed worst-case cold start, not warm latency.** With cold loads at 1.8-10 s and a reported ~10 s effective default, a classifier call after an idle period can time out on an otherwise healthy server. Either set the read timeout above the cold-load ceiling **or** eliminate cold starts with a negative `keep_alive` plus a startup preload — preferably both.
  2. **Connection reuse.** At ~100 ms of model compute, a fresh TCP + TLS handshake per call is a large relative overhead. The HTTP client must pool keep-alive connections to the Ollama endpoint; for local plaintext HTTP the saving is smaller but the per-call connection setup is still not free against a 100 ms budget.
  3. **A custom `OllamaApi` bean appears to be the only reliable timeout lever** per the reported findings — the obvious `spring.*` properties are reported not to apply. This should be verified against Spring AI 2.0.1's own `OllamaApi` source rather than trusted from a blog.
  4. **Separate timeout budgets per model.** One global timeout cannot serve both a 15-token classification (~100 ms) and a 50-passage rerank (~650 ms on a 5090, far more on a laptop). A single value will be either too tight for the rerank or useless as a circuit breaker for the classifier.
  5. **Retries are dangerous on a queued server.** Ollama queues up to 512 and then 503s; retrying a timed-out classification adds load to a server that is already behind, and because the first request may still be executing, the retry can trigger the very eviction that caused the delay. A router should **fail open to the regex rule** rather than retry — the fallback is free, correct-ish, and bounded, which is a property very few LLM-in-the-path components enjoy.

### Gaps

- **I found no engineering post-mortem specifically about an LLM in a retrieval/routing path.** The determinism literature is about generation; the operational literature I reached is vendor and blog content. A genuine post-mortem corpus for this pattern does not appear to exist publicly, which is itself worth stating in the report.
- **No measured evidence of Ollama or llama.cpp run-to-run determinism** with fixed seed and `NUM_PARALLEL=1`. This is the most valuable missing measurement in this note and is trivially obtainable locally: N identical calls, hash the outputs, report the unique count. The series could generate the first citable number here.
- No measured non-determinism rate for **short classification outputs** on any hosted API — the Thinking Machines experiment used 1,000-token generations. The token-103 observation is suggestive, not transferable.
- No data on how often providers **silently update a model behind a stable alias**, as opposed to publishing a deprecation. Pinning a dated model identifier is the mitigation; its availability varies by provider and I did not verify it per provider.
- Spring AI 2.0.1 / Spring Boot 4.1.1 / Java 25 specifics are **unverified against official documentation**; every Spring-side finding here is community-sourced and the version in the blog posts is not stated.
