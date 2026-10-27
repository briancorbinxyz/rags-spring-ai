# Spring AI 2.0.1 chat / model-call option defaults, with Ollama as the provider

**Verification basis for everything below unless stated otherwise:** Spring AI source read at the
git tag **`v2.0.1`** (tag object `655d6b24ee15da353d2446a4da8f126a93cefc70`, resolved via the GitHub
API on 2026-10-27), and Ollama source read on the **`main`** branch on 2026-10-27 (latest published
tag at that moment: `v0.40.11`).

**Two standing caveats the report writer should carry through:**

1. The published reference docs at `docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html`
   now serve **2.0.2** as "stable" (the 2.0-pinned URL `/reference/2.0/...` 302-redirects to the
   unversioned stable page). Every property-table value quoted from the docs below is therefore
   **verified for the 2.0.2 page**, while every constant and null-check is **verified for the
   2.0.1 source tag**. I found no difference between them on this surface, but the distinction
   matters for a reproducibility claim.
2. Ollama-side behaviour is verified against `main`, **not** against a pinned Ollama release. The
   automatic `num_ctx` tiering described below is recent; an older Ollama binary on the machine
   will behave differently. The audit should record the exact `ollama -v` of the run.

Where a default is undocumented and I read source instead, it is marked **[source-read]**.

---

## `ChatOptions` defaults in 2.0.1: `maxTokens`, `topP`, `topK`, `frequencyPenalty`, `presencePenalty`, `stopSequences`, `seed` — and what a `null` option sends over the wire

### Takeaway

Every portable `ChatOptions` field in 2.0.1 is `@Nullable` with **no initializer**, so the default
of each one is literally `null`; `seed` is **not part of `ChatOptions` at all** in 2.0.1. A `null`
option is **omitted from the JSON body entirely** — Spring AI's Ollama options map is filtered of
null values before serialization, so "default" means "the key never appears in the request" and
Ollama applies its own default. **[source-read]**

### Cited Findings

- The `ChatOptions` interface in 2.0.1 declares exactly these getters, each annotated
  `@Nullable`: `getModel()`, `getFrequencyPenalty()`, `getMaxTokens()`, `getPresencePenalty()`,
  `getStopSequences()`, `getTemperature()`, `getTopK()`, `getTopP()`. There is **no `getSeed()`**
  (a case-insensitive grep for "seed" in `ChatOptions.java` and `DefaultChatOptions.java` returns
  zero hits) — [ChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-model/src/main/java/org/springframework/ai/chat/prompt/ChatOptions.java)
- `DefaultChatOptions` — the concrete class produced by `ChatOptions.builder()` — holds eight
  fields, all `private final @Nullable` with **no initializer and no fallback in the
  constructor**: `model`, `frequencyPenalty`, `maxTokens`, `presencePenalty`, `stopSequences`,
  `temperature`, `topK`, `topP`. So `ChatOptions.builder().temperature(0.3).build()` yields an
  object whose other seven accessors return `null` — [DefaultChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-model/src/main/java/org/springframework/ai/chat/prompt/DefaultChatOptions.java)
- Builder setters are themselves `@Nullable`-typed (`B maxTokens(@Nullable Integer maxTokens)`),
  i.e. "unset" and "explicitly null" are the same state — [ChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-model/src/main/java/org/springframework/ai/chat/prompt/ChatOptions.java)
- `OllamaChatOptions.toMap()` ends with
  `return map.entrySet().stream().filter(kv -> kv.getValue() != null).collect(Collectors.toMap(Entry::getKey, Entry::getValue));`
  — null-valued options are dropped before the request is built (line 643) — [OllamaChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- `OllamaChatModel.ollamaChatRequest(...)` sets the two out-of-`options` fields only when non-null:
  `if (requestOptions.getFormat() != null) { requestBuilder.format(...); }` and
  `if (requestOptions.getKeepAlive() != null) { requestBuilder.keepAlive(...); }` — [OllamaChatModel.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/OllamaChatModel.java)
- `OllamaChatOptions` declares `private static final List<String> NON_SUPPORTED_FIELDS = List.of("model", "format", "keep_alive", "truncate");` and a static
  `filterNonSupportedFields(Map)` that strips those four from the nested `options` map — they are
  top-level Ollama API fields, not sampling options — [OllamaChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- In 2.0.x, `ChatClient`'s `.defaultOptions(...)` / `.options(...)` take a **`ChatOptions.Builder`,
  not a built `ChatOptions`**, and act as a *customizer*: `DefaultChatClient` does
  `ChatOptions.Builder<?> requestOptionsBuilder = prompt.getOptions().mutate(); spec.getOptionsCustomizer().combineWith(requestOptionsBuilder);`
  — [DefaultChatClient.java @ v2.0.1, lines 127-134](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/DefaultChatClient.java)
- `combineWith` is explicitly null-safe — its javadoc reads "Mutate this builder by taking all
  `other`'s values that are non-null, retaining `this` other values" — and the implementation is a
  chain of `if (that.<field> != null)` guards. **Exception:** `stopSequences` is *concatenated*,
  not replaced (`merged.addAll(that.stopSequences)`) — [DefaultChatOptionsBuilder.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-model/src/main/java/org/springframework/ai/chat/prompt/DefaultChatOptionsBuilder.java)
- `OllamaChatOptions.AbstractBuilder.combineWith(...)` calls `super.combineWith(other)` first (so
  the portable fields, including `temperature`, are copied from a plain `ChatOptions.builder()`),
  then copies its own Ollama-specific fields under the same `!= null` guards
  (`format`, `keepAlive`, `truncate`, `thinkOption`, `useNUMA`, `numCtx`, `numBatch`, `numGPU`,
  `mainGPU`, `lowVRAM`, `f16KV`, `logitsAll`, `vocabOnly`, `useMMap`, `useMLock`, …) —
  [OllamaChatOptions.java @ v2.0.1, lines 801-860](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)

### Inferences

- For this project's exact configuration — `.defaultOptions(ChatOptions.builder().temperature(0.3))`
  against Ollama — the resulting request's nested `"options"` object contains **exactly one key**,
  `"temperature": 0.3` (0.4 for the one role). No `num_ctx`, `num_predict`, `top_p`, `top_k`,
  `seed`, or `repeat_penalty` key is emitted, and no `keep_alive` or `format` top-level field is
  emitted. Consequently every other knob is **Ollama's** default, not Spring AI's.
- "Spring AI's default for `topP`" is not a number; the only defensible phrasing in the articles is
  "Spring AI sends no `top_p`, so Ollama's `0.9` applies". Attributing `0.9` to Spring AI would be
  wrong.
- Because the portable builder cannot express `seed`, a project that only ever touches
  `ChatOptions.builder()` has **no way to set a seed**; it must switch to
  `OllamaChatOptions.builder()` (which does have `seed`). See the determinism section.

### Gaps

- I verified that the nested `options` map omits nulls and that `format`/`keep_alive` are omitted
  when null, but I did **not** inspect the Jackson inclusion annotations on the top-level
  `OllamaApi.ChatRequest` record, so I cannot state whether unrelated top-level fields (e.g.
  `think`) appear as explicit `null`s in the body. Confirm by capturing one real request body
  (e.g. an HTTP proxy or `OLLAMA_DEBUG=1`) if the article needs the literal JSON.
- I did not find a Spring AI document that states "a null option is omitted"; this is a
  source-read conclusion, not a documented contract, and could in principle change in a patch
  release.

---

## What the Ollama provider itself defaults, and whether Spring AI ever sets `num_ctx`

### Takeaway

**Spring AI 2.0.1 never sets `num_ctx` unless you set it.** `OllamaChatOptions.numCtx` is
`private final @Nullable Integer` with no initializer, and `OllamaChatProperties.numCtx` is
`@Nullable` with no default, so the key is simply absent from the request. The `2048` in the
Spring AI property table is **transcribed from Ollama's own historical defaults**, not a value
Spring AI applies — and it is now **stale**: current Ollama picks the context length automatically
from available VRAM (4096 / 32768 / 262144). The only non-null default `OllamaChatOptions`
actually injects is the **model name**, which falls back to `OllamaModel.MISTRAL.id()`.
**[source-read]**

### Cited Findings

Spring AI side:

- `OllamaChatOptions` has 39 private fields; a grep for field initializers finds **only two static
  constants** (`jsonHelper`, `NON_SUPPORTED_FIELDS`). Every option field is
  `private final @Nullable …` with no initializer, and the constructor assigns them straight
  through — [OllamaChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- The numbers in that class are **javadoc only**. Verbatim: `/** Sets the size of the context
  window used to generate the next token. (Default: 2048) */ private final @Nullable Integer
  numCtx;`, `/** (Default: 4) */ private final @Nullable Integer numKeep;`, `/** Maximum number of
  tokens to predict when generating text. (Default: 128, -1 = infinite generation, -2 = fill
  context) */ private final @Nullable Integer numPredict;`, `/** … (Default: -1) */ private final
  @Nullable Integer seed;`. The class javadoc attributes these to Ollama's `modelfile.mdx` and
  `api/types.go` — [OllamaChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- The one genuine Spring-side default: the constructor does
  `this.model = model != null ? model : OllamaModel.MISTRAL.id();` — an unset model silently
  becomes Mistral. (In practice `spring.ai.ollama.chat.model` or the auto-configuration supplies
  one; `OllamaChatModel` additionally asserts `model must not be null` before building the
  request.) — [OllamaChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- `OllamaChatProperties` (prefix `spring.ai.ollama.chat`) declares `private @Nullable String
  keepAlive;` and `private @Nullable Integer numCtx;` — no defaults — and passes them through to
  the options builder (`.keepAlive(this.keepAlive)`, `.numCtx(this.numCtx)`) — [OllamaChatProperties.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/auto-configurations/models/spring-ai-autoconfigure-model-ollama/src/main/java/org/springframework/ai/model/ollama/autoconfigure/OllamaChatProperties.java)
- The reference docs table lists `spring.ai.ollama.chat.num-ctx` default `2048`,
  `num-predict` `-1`, `keep_alive` `5m`, `top-p` `0.9`, `top-k` `40`, `seed` `-1`,
  `repeat-penalty` `1.1`, `temperature` `0.8`, `num-keep` `4`, `stop` none — **and states the
  provenance**: "The remaining `options` properties are based on the [Ollama Valid Parameters and
  Values] and [Ollama Types]. **The default values are based on the [Ollama Types Defaults].**"
  (page served as 2.0.2) — [Spring AI Ollama Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- Also from that page, for completeness on adjacent defaults:
  `spring.ai.ollama.init.pull-model-strategy` = `never`, `spring.ai.ollama.init.timeout` = `5m`,
  `spring.ai.ollama.init.max-retries` = `0` — [Spring AI Ollama Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)

Ollama side:

- `api.DefaultOptions()` on `main` returns, verbatim: `NumPredict: -1`, `NumKeep: 4`,
  `Temperature: 0.8`, `TopK: 40`, `TopP: 0.9`, `TypicalP: 1.0`, `RepeatLastN: 64`,
  `RepeatPenalty: 1.0`, `PresencePenalty: 0.0`, `FrequencyPenalty: 0.0`, `Seed: -1`, and in the
  nested `Runner`: `NumCtx: int(envconfig.ContextLength())`, `NumBatch: 512`, `NumGPU: -1`,
  `NumThread: 0`, `DraftNumPredict: 4`, `UseMMap: nil` — [ollama/api/types.go](https://github.com/ollama/ollama/blob/main/api/types.go)
  - Note two divergences from the Spring AI table: Ollama's current `RepeatPenalty` is **`1.0`**,
    not the `1.1` the Spring docs list, and current `NumPredict` is `-1` while the
    `OllamaChatOptions` javadoc still says `128`. The Spring-side numbers are drifting from the
    upstream source they were copied from.
- `envconfig.ContextLength = Uint("OLLAMA_CONTEXT_LENGTH", 0)` — the env default is **0**, meaning
  "automatic", and the CLI help string reads
  `"Context length to use unless otherwise specified (default: 4k/32k/256k based on VRAM)"` —
  [ollama/envconfig/config.go](https://github.com/ollama/ollama/blob/main/envconfig/config.go)
- The automatic tier is chosen once at server start from total VRAM:
  `case totalVRAM >= 47*format.GibiByte: s.defaultNumCtx = 262144`,
  `case totalVRAM >= 23*format.GibiByte: s.defaultNumCtx = 32768`,
  `default: s.defaultNumCtx = 4096`, followed by
  `slog.Info("vram-based default context", "total_vram", …, "default_num_ctx", s.defaultNumCtx)`;
  it is applied at `opts.NumCtx = s.defaultNumCtx` — [ollama/server/routes.go](https://github.com/ollama/ollama/blob/main/server/routes.go)
- Precedence is explicit in `usesAutomaticNumCtx(model, requestOpts)`: it returns `false` if
  `requestOpts["num_ctx"]` is present, `false` if `model.Options["num_ctx"]` is present (i.e. a
  **Modelfile `PARAMETER num_ctx` wins over the automatic/env default**), and otherwise
  `envconfig.ContextLength() == 0` — [ollama/server/routes.go](https://github.com/ollama/ollama/blob/main/server/routes.go)
- Ollama's own API docs give **no default for `num_ctx`**; it appears only in an options example
  (`"num_ctx": 1024`). The docs do describe `prompt_eval_count` as "number of tokens in the
  prompt" and `eval_count` as "number of tokens in the response", and the `/api/generate` and
  `/api/chat` sections say nothing about overflow behaviour. The only documented `truncate`
  parameter is on `/api/embed`, where it "truncates the end of each input to fit within context
  length", defaults to `true`, and "Returns error if `false` and context length is exceeded" —
  [ollama/docs/api.md](https://github.com/ollama/ollama/blob/main/docs/api.md)
- `typical_p` is now deprecated upstream: `var deprecatedOptions = []string{"typical_p"}` with a
  `slog.Warn("deprecated option provided", …)`; the API docs repeat that it "is deprecated and may
  be removed in a future release". `OllamaChatOptions` still exposes `typicalP` (and `tfsZ`) —
  [ollama/server/routes.go](https://github.com/ollama/ollama/blob/main/server/routes.go);
  [ollama/docs/api.md](https://github.com/ollama/ollama/blob/main/docs/api.md)

### Inferences

- Resolution order for the effective context window in this project, end to end:
  request `options.num_ctx` (**never sent by this project**) → Modelfile `PARAMETER num_ctx`
  (whatever the pulled model ships, or a custom Modelfile) → `OLLAMA_CONTEXT_LENGTH` (unset by
  default) → VRAM tier chosen at server start. On an M1 Mac well under 23 GiB of detected VRAM
  that lands on **4096**, which is exactly the regime where a 40k-capable model is silently
  capped. (Re-measure on the actual machine rather than asserting the tier from the model name.)
- The widely-quoted "Ollama defaults to 2048" is **historical**. For a 2026-dated audit the
  honest statement is "Ollama's default is not a fixed number any more: it is 0/automatic, which
  resolves to 4096, 32768 or 262144 by VRAM at server start, and the server logs the value it
  picked." The `vram-based default context` log line is the cheapest ground truth for the article.
- Because Spring AI sends no `num_ctx`, the Spring-side configuration is *not* the place to look
  for the truncation bug the project already established — the number in effect comes from the
  Ollama server and the model's Modelfile. Setting `spring.ai.ollama.chat.num-ctx` explicitly is
  what converts that from an environment-dependent value into a reproducible one.
- Detection, consistent with what the project already established: compare the response's
  `prompt_eval_count` against the `num_ctx` actually in effect. Since the project doesn't set
  `num_ctx`, "in effect" must be read from `/api/show` (Modelfile parameters) or the server log,
  not from the Spring configuration.

### Gaps

- `prompt_eval_count`-vs-`num_ctx` truncation detection and the silent-truncation behaviour itself
  were **taken as established by the project** per the brief; I did not re-derive them, and I did
  not locate the specific `server/prompt.go` truncation code on `main` to re-quote.
- I could not pin the Ollama version at which the VRAM-tiered automatic context length replaced a
  fixed default (the change is present on `main`; I did not bisect the releases). If the article
  dates the change, that needs a release-notes citation.
- Whether `ChatResponse` metadata in Spring AI 2.0.1 surfaces `prompt_eval_count` under a stable
  key (vs. requiring the raw Ollama response) was not verified in this pass.

---

## `keep_alive`

### Takeaway

Ollama's default is **5 minutes**, implemented as `keepAlive = 5 * time.Minute` in
`envconfig.KeepAlive()`; Spring AI 2.0.1 sends **no** `keep_alive` field unless configured, so the
5-minute default applies. A run with gaps longer than 5 minutes between calls will have the model
unloaded and reloaded mid-run, which changes load-time-dependent settings and timings.

### Cited Findings

- `func KeepAlive() (keepAlive time.Duration) { keepAlive = 5 * time.Minute; if s := Var("OLLAMA_KEEP_ALIVE"); s != "" { … } }`,
  with the comment "Negative values are treated as infinite. Zero is treated as no keep alive.
  Default is 5 minutes." A negative value returns `time.Duration(math.MaxInt64)` —
  [ollama/envconfig/config.go](https://github.com/ollama/ollama/blob/main/envconfig/config.go)
- Ollama's API docs: `keep_alive` "controls how long the model will stay loaded into memory
  following the request (default: `5m`)" — [ollama/docs/api.md](https://github.com/ollama/ollama/blob/main/docs/api.md)
- Spring AI's `OllamaChatOptions.keepAlive` is `private final @Nullable String keepAlive;` with no
  default, documented as "Sets the length of time for Ollama to keep the model loaded. Valid
  values for this setting are parsed by ParseDuration in Go." — [OllamaChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- It is emitted only when non-null (`if (requestOptions.getKeepAlive() != null)`) and is excluded
  from the nested `options` map via `NON_SUPPORTED_FIELDS` because it is a top-level API field —
  [OllamaChatModel.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/OllamaChatModel.java)
- The docs table lists `spring.ai.ollama.chat.keep_alive` with default `5m` — but, per the same
  page's provenance sentence, that is Ollama's default being documented, not a value Spring AI
  sets — [Spring AI Ollama Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)

### Inferences

- The reproducibility consequence is specifically about **load-time options**. `OllamaChatOptions`
  separates its fields with the comment "Following fields are options which must be set when the
  model is loaded into memory" (`numCtx`, `numBatch`, `numGPU`, `mainGPU`, `lowVRAM`, `f16KV`,
  `useMMap`, `useMLock`, `numThread`) from "Following fields are predict options used at runtime".
  An unload/reload is therefore the moment at which the effective `num_ctx`, GPU layer split and
  thread count are re-decided — and, on current Ollama, the moment the automatic tier is re-applied
  against whatever VRAM is free then. Two halves of one experiment separated by a >5-minute pause
  can legitimately run under different load-time parameters.
- For an auditable run the mitigation is to pin `spring.ai.ollama.chat.keep-alive` (e.g. `-1` /
  `30m`) or `OLLAMA_KEEP_ALIVE`, and to record it, rather than relying on calls arriving close
  enough together.

### Gaps

- I did not verify whether Ollama's reload emits anything in the HTTP response a caller could use
  to detect that a reload happened (the `load_duration` field is the obvious candidate but I did
  not confirm its semantics on `main`). Treat "detect the reload from the response" as unverified.

---

## Determinism with `temperature` set but `seed`, `top_p`, `top_k` defaulted

### Takeaway

Nothing is reproducible by default. With no `seed` sent, Ollama uses `Seed: -1` (random), so
`temperature=0.3` plus default `top_p=0.9` / `top_k=40` gives a *differently* sampled run every
time. `temperature <= 0` **does** switch Ollama to greedy argmax decoding **[source-read]**, and
Spring AI does pass `seed` through — but **only via `OllamaChatOptions`**, since the portable
`ChatOptions` has no `seed` field at all in 2.0.1.

### Cited Findings

- `api.DefaultOptions()` sets `Seed: -1` — [ollama/api/types.go](https://github.com/ollama/ollama/blob/main/api/types.go)
- The sampler's options struct carries both a seed and an explicit *was it set* flag:
  `Seed int` and `UseSeed bool` — [ollama/mlxrunner/sample/sample.go](https://github.com/ollama/ollama/blob/main/mlxrunner/sample/sample.go)
- Greedy decoding at non-positive temperature, verbatim from the sampler:
  `if slot.opts.Temperature <= 0 { choice := scores.Argmax(-1, true); … }`, with the surrounding
  comment "Greedy decoding proposes the argmax as a point mass and still advances the seeded
  stream."; a second site reads `if slot.opts.Temperature == 0 { return slot.baseScores(ctx, logits) }`
  — [ollama/mlxrunner/sample/sample.go](https://github.com/ollama/ollama/blob/main/mlxrunner/sample/sample.go)
- `OllamaChatOptions` **does** have a `seed` field (`private final @Nullable Integer seed;`,
  documented "Sets the random number seed to use for generation. Setting this to a specific number
  will make the model generate the same text for the same prompt. (Default: -1)"), and it is
  serialized into the `options` map when non-null — [OllamaChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- The portable `ChatOptions` / `DefaultChatOptions` have **no seed member** (zero grep hits) —
  [ChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-model/src/main/java/org/springframework/ai/chat/prompt/ChatOptions.java)
- `spring.ai.ollama.chat.seed` exists as a configuration property (docs table, default listed as
  `-1`, i.e. Ollama's) — so a seed can be pinned in `application.yaml` without touching Java —
  [Spring AI Ollama Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)

### Inferences

- What *is* reproducible with this project's current configuration: the request body (one option,
  `temperature`), the prompt, and the model digest. What is **not**: the sampled token sequence,
  because the seed is random per request and `temperature=0.3` leaves a live distribution over the
  top-40 / top-0.9 mass.
- `temperature=0` would give greedy decoding and make the seed irrelevant on the sampling path —
  but greedy is still only reproducible *for a fixed load configuration*: batching
  (`OLLAMA_NUM_PARALLEL`), `num_batch`, GPU layer split and the effective `num_ctx` all affect
  numerics, and all of those are re-decided on reload (see `keep_alive`). So "temperature 0 =
  deterministic" is too strong a claim for an article; "greedy, hence no sampling noise, given an
  unchanged model load" is defensible.
- Minimum changes to make the project's runs reproducible: set `seed` (and ideally `top_p`,
  `top_k`, `repeat_penalty`, `num_ctx`, `num_predict`, `keep_alive`) explicitly — which requires
  either `OllamaChatOptions.builder()` in place of `ChatOptions.builder()`, or
  `spring.ai.ollama.chat.*` properties. The portable builder alone cannot do it.

### Gaps

- The greedy / `UseSeed` code I quoted is from `mlxrunner/sample/sample.go`, the **MLX runner**
  (the Apple-Silicon path — directly relevant to an M1/aarch64-darwin host, but not proof for the
  llama.cpp/CUDA runner). I did not locate an equivalent sampler file for the non-MLX runner on
  `main` (`sample/` no longer exists as a top-level package there), so "temperature <= 0 is greedy"
  is verified **only for the MLX runner on `main`**.
- I found no upstream Ollama statement that a fixed `seed` + fixed options guarantees
  bit-identical output across requests, restarts, or `OLLAMA_NUM_PARALLEL` values. Do not claim it.

---

## Structured output: what 2.0.1 does by default for entity mapping, and the retry/repair default

### Takeaway

By default, `ChatClient` entity mapping in 2.0.1 is a **prompt instruction plus a single parse
attempt** — `BeanOutputConverter` generates a JSON schema, wraps it in a fixed instruction
paragraph, and `ChatModelCallAdvisor` **appends that paragraph to the user message**. There is
**no** JSON-schema-constrained decode and **no** retry/repair by default: native structured output
(sending the schema as Ollama's `format`) is **opt-in**, and the schema-validating retry loop is a
separate advisor you must register, whose own builder default is `maxRepeatAttempts = 3`.
**[source-read]**

### Cited Findings

- `ChatClient.entity(...)` / `responseEntity(...)` all construct `new BeanOutputConverter<>(type)`
  and route through `doSingleWithBeanOutputConverter` / `doResponseEntity` — [DefaultChatClient.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/DefaultChatClient.java)
- The default path is explicitly labelled prompt-based in the code comment:
  `if (StringUtils.hasText(outputConverter.getFormat())) { // Used for default structured output format support, based on prompt instructions. this.request.context().put(ChatClientAttributes.OUTPUT_FORMAT.getKey(), outputConverter.getFormat()); }`,
  and the native branch is guarded by
  `if (Boolean.TRUE.equals(this.request.context().get(ChatClientAttributes.STRUCTURED_OUTPUT_NATIVE.getKey()))) { … STRUCTURED_OUTPUT_SCHEMA … }`
  with the comment "Used for native structured output support, e.g. AI model API should provide
  structured output support." — [DefaultChatClient.java @ v2.0.1, lines 592-608](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/DefaultChatClient.java)
- The exact instruction text appended by default, verbatim from `BeanOutputConverter.getFormat()`:

  ```
  Your response should be in JSON format.
  Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
  Do not include markdown code blocks in your response.
  Remove the ```json markdown from the output.
  Here is the JSON Schema instance your output must adhere to:
  ```%s```
  ```
  — [BeanOutputConverter.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-model/src/main/java/org/springframework/ai/converter/BeanOutputConverter.java)
- `ChatModelCallAdvisor.augmentWithFormatInstructions(...)` decides between the two: it reads
  `OUTPUT_FORMAT` and `STRUCTURED_OUTPUT_SCHEMA` from context, returns the request untouched if
  both are blank, and only takes the native branch when
  `chatClientRequest.context().containsKey(ChatClientAttributes.STRUCTURED_OUTPUT_NATIVE.getKey())`
  **and** the prompt's options implement `StructuredOutputChatOptions` — in which case it does
  `structuredOutputChatOptions.mutate().outputSchema(outputSchema).build()`. Otherwise it falls
  through to
  `prompt.augmentUserMessage(userMessage -> userMessage.mutate().text(userMessage.getText() + System.lineSeparator() + outputFormat).build())`
  — [ChatModelCallAdvisor.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/ChatModelCallAdvisor.java)
- The context keys are `OUTPUT_FORMAT("spring.ai.chat.client.output.format")`,
  `STRUCTURED_OUTPUT_SCHEMA("spring.ai.chat.client.structured.output.schema")`,
  `STRUCTURED_OUTPUT_NATIVE("spring.ai.chat.client.structured.output.native")` — [ChatClientAttributes.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/ChatClientAttributes.java)
- On Ollama, native structured output maps to the API's `format` field:
  `OllamaChatOptions` implements `ToolCallingChatOptions, StructuredOutputChatOptions`, and
  `getOutputSchema()` is a view over `format` — "If format is a simple string (e.g., \"json\"),
  return it as-is", otherwise `jsonHelper.toJson(this.format)`. `format` is documented as "Sets the
  desired format of output from the LLM. The only valid values are null or \"json\"." —
  [OllamaChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- `StructuredOutputChatOptions` is the 2.0 "mixin interface for ChatModels that support structured
  output… unified way to set and get the output JSON schema", exposing
  `@Nullable String getOutputSchema()` and `B outputSchema(@Nullable String outputSchema)` —
  [StructuredOutputChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-model/src/main/java/org/springframework/ai/model/tool/StructuredOutputChatOptions.java)
- Ollama does support schema-constrained output: `format` "Accepts either `\"json\"` (any JSON
  structure) or a JSON Schema object (enforced structure)", and the docs mark the JSON-Schema form
  "Recommended for Production", with `.outputSchema(jsonSchemaString)` as the Spring-side call;
  the page also warns to use `@JsonProperty(required = true, …)` so the generated schema marks
  fields required — [Spring AI Ollama Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html).
  Ollama's own docs add that in JSON mode "It's important to instruct the model to use JSON in the
  `prompt`" — [ollama/docs/api.md](https://github.com/ollama/ollama/blob/main/docs/api.md)
- The repair loop is an **opt-in advisor**, `StructuredOutputValidationAdvisor`
  ("Validates the JSON output against a JSON schema derived from the configured output type or a
  pre-supplied schema string… When validation fails, the advisor appends the validation error to
  the user message and re-invokes the model, repeating up to `maxRepeatAttempts` times…
  **Streaming responses are not supported.**"). Its builder default is
  `private int maxRepeatAttempts = 3;`, the constructor asserts `maxRepeatAttempts >= 0`, the loop
  is `for (var currentAttemptNumber = 1 + this.maxRepeatAttempts; currentAttemptNumber > 0 && !isValidationSuccess; currentAttemptNumber--)`,
  and validation uses `SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)` —
  [StructuredOutputValidationAdvisor.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/StructuredOutputValidationAdvisor.java)

### Inferences

- For this project (`ResponseEntity` / entity mapping, nothing opted in): each call makes **one**
  model request; the schema travels as English prose appended to the user message; a malformed
  response is a **parse failure, not a retry**. There is no default `maxAttempts` at the
  structured-output layer at all — the only `3` in this surface belongs to an advisor the project
  does not register. (`RetryUtils.DEFAULT_RETRY_TEMPLATE` is wired into `OllamaChatModel`, but that
  is transport/HTTP-level retry, not malformed-JSON repair.)
- Two compounding costs worth naming in the audit: the appended instruction paragraph plus the full
  JSON schema **inflates the prompt**, which interacts directly with the un-set `num_ctx` and
  Ollama's silent truncation — and the truncation can land on the schema text itself, which is at
  the *end* of the user message. That is a plausible mechanism for intermittent parse failures.
- Switching to native structured output (`.outputSchema(...)` → Ollama `format`) both shrinks the
  prompt and makes well-formedness a decoding constraint rather than a request; it is the single
  highest-leverage change on this surface.

### Gaps

- I did not pin down the exact public API by which `STRUCTURED_OUTPUT_NATIVE` is set (it is read
  from the request context and is also referenced by `AdvisorParams` and by
  `ChatClientNativeStructuredResponseTests` / `ChatClientResponseEntityTests`); the likely route is
  the `entity(type, entitySpecConsumer)` / advisor-params overloads. Confirm the exact call before
  publishing a code sample.
- I did not verify whether any auto-configuration registers `StructuredOutputValidationAdvisor` by
  default under some property; my reading of `DefaultChatClient` and `ChatModelCallAdvisor` shows
  no default registration, but I did not grep the auto-configuration modules for it.

---

## What changed on this surface between Spring AI 1.x and 2.0.x

### Takeaway

The behaviour that matters most for an audit — "unset means omitted, so the provider decides" — is
**unchanged** from 1.0.0. What changed is the API surface around it: `OllamaOptions` was split into
a chat-specific `OllamaChatOptions`, the options objects became **immutable with
builder-`combineWith` merging** (and `ChatClient.defaultOptions` now takes a *builder*), the
Ollama property prefix moved from `spring.ai.ollama.chat.options.*` to `spring.ai.ollama.chat.*`,
and structured output gained a native-schema path plus a validating-retry advisor.

### Cited Findings

- **`OllamaOptions` → `OllamaChatOptions`.** At `v1.0.0` the file
  `models/spring-ai-ollama/.../api/OllamaOptions.java` exists (1048 lines,
  `public class OllamaOptions implements ToolCallingChatOptions, EmbeddingOptions`, mutable fields
  with `@JsonProperty("num_ctx")` etc.) — [OllamaOptions.java @ v1.0.0](https://github.com/spring-projects/spring-ai/blob/v1.0.0/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaOptions.java).
  At `v2.0.1` that path is **absent** from the repository tree; the chat class is
  `OllamaChatOptions implements ToolCallingChatOptions, StructuredOutputChatOptions` with
  `private final` fields and a protected constructor — [OllamaChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- **The null-means-omit behaviour is the same in 1.0.0.** A grep for field initializers in
  `OllamaOptions.java` @ v1.0.0 finds only `NON_SUPPORTED_FIELDS` and the collection fields
  (`toolCallbacks = new ArrayList<>()`, `toolNames = new HashSet<>()`,
  `toolContext = new HashMap<>()`) — no `num_ctx`/`seed`/`top_p` defaults — and the same
  `NON_SUPPORTED_FIELDS = List.of("model", "format", "keep_alive", "truncate")` and `toMap()` are
  present — [OllamaOptions.java @ v1.0.0](https://github.com/spring-projects/spring-ai/blob/v1.0.0/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaOptions.java)
- **Property prefix move.** `OllamaChatProperties` @ v2.0.1 has `CONFIG_PREFIX = "spring.ai.ollama.chat"`
  and carries a nested deprecated holder whose accessors are annotated
  `@DeprecatedConfigurationProperty(replacement = "spring.ai.ollama.chat.num-ctx")` /
  `"spring.ai.ollama.chat.keep-alive"` / `"spring.ai.ollama.chat.model"` / `.format` / `.truncate` /
  `.think` / `.use-numa`, each `@Deprecated(since = "2.0.0", forRemoval = true)` — i.e. the old
  `…chat.options.*` names still bind but are deprecated as of 2.0.0 — [OllamaChatProperties.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/auto-configurations/models/spring-ai-autoconfigure-model-ollama/src/main/java/org/springframework/ai/model/ollama/autoconfigure/OllamaChatProperties.java)
- **Options merging is now builder-based.** `ChatOptions` @ v2.0.1 declares `ChatOptions.Builder<?> mutate()`
  ("Concrete ChatOptions classes must implement this and return the most concrete builder
  implementation") and `B combineWith(ChatOptions.Builder<?> other)`; `DefaultChatClient` calls
  `prompt.getOptions().mutate()` + `combineWith` instead of merging option beans — [ChatOptions.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-model/src/main/java/org/springframework/ai/chat/prompt/ChatOptions.java);
  [DefaultChatClient.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/DefaultChatClient.java)
- **Model-level option merge is now trivial in the Ollama model.**
  `OllamaChatModel.buildRequestPrompt(Prompt)` @ v2.0.1 is just
  `if (prompt.getOptions() == null) { return prompt.mutate().chatOptions(this.getOptions()).build(); } else { return prompt; }`
  — the runtime options are used as-is and then cast to `OllamaChatOptions`, so merging with the
  model's defaults is expected to have happened in the `ChatClient` layer — [OllamaChatModel.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/OllamaChatModel.java)
- **Tool execution moved out of the model.** `OllamaChatModel.Builder.toolCallingManager(...)` is
  `@Deprecated(since = "2.0.0", forRemoval = true)` with "internal tool execution in
  `OllamaChatModel` is superseded by `ToolCallingAdvisor` used via `ChatClient`" — [OllamaChatModel.java @ v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/OllamaChatModel.java)
- **2.0.1 specifically** is "the first maintenance release after the 2.0.0 GA", dated 2026-08-21,
  fixing 80+ issues/PRs. Its upgrade notes include: deprecated Mistral chat model constants
  removed; the Redis chat memory auto-configuration artifact id renamed; Couchbase vector store now
  uses the Spring Boot-managed client; and — a **default change** — "OpenAI tool-calling strict
  mode now defaults to `false`", addressing 400s on optional tool parameters. New: a configurable
  tool-call limit throwing `ToolCallLimitExceededException` via `ToolCallingAdvisor` — [Spring AI 2.0.1 Available Now](https://spring.io/blog/2026/08/21/spring-ai-2-0-1-available-now)

### Inferences

- No 1.x→2.0 change alters the central audit finding: in both lines, an unset option is absent from
  the Ollama request, and `num_ctx` is never set by Spring AI. The articles' 1.x-era claims about
  *which* defaults apply remain valid in substance; what needs updating is class names
  (`OllamaOptions` → `OllamaChatOptions`), property names (`…chat.options.num-ctx` →
  `…chat.num-ctx`), and the `defaultOptions(builder)` signature.
- The `combineWith` semantics are a new, quiet hazard: `stopSequences` **accumulate** across
  default and request-level options rather than the later one replacing the earlier. A project that
  set stop sequences in two places in 2.0.x gets the union.
- The `StructuredOutputChatOptions` mixin, the `STRUCTURED_OUTPUT_NATIVE` flag, and
  `StructuredOutputValidationAdvisor` are the 2.0-era answer to malformed structured output; none
  of them is on by default, so a project that upgraded from 1.x and changed nothing is still on the
  prompt-instruction, single-attempt path.

### Gaps

- I could not retrieve a 2.0 release-notes / migration document: `github.com/spring-projects/spring-ai/wiki/Spring-AI-2.0-Release-Notes`
  returned only the wiki home ("Welcome to the spring-ai wiki!"). Every 1.x→2.0 change listed above
  is therefore **source-read / deprecation-annotation-read**, not quoted from an official upgrade
  guide. If the report needs a citable migration guide, that URL has to be found first.
- I did not diff the 1.x and 2.0 `BeanOutputConverter.getFormat()` instruction text, so I cannot
  say whether the exact prompt wording changed between the lines (relevant if the project's earlier
  articles quoted the 1.x paragraph).
- Spring Boot 4.1.1 / Java 25 specifics: I found nothing on this options surface that depends on
  the Boot or JDK version, and I did not independently verify the 2.0.1 ↔ Boot 4.1.1 ↔ Java 25
  compatibility matrix.
