# Spring AI 2.0.1: routing, query classification and conditional retrieval surface

> **Method note.** Where precision mattered I read the source **pinned to the `v2.0.1` git tag** (`raw.githubusercontent.com/spring-projects/spring-ai/v2.0.1/...`) rather than trusting prose. The reference docs served at `docs.spring.io/spring-ai/reference/` (no version segment) currently render **2.0.2 (Stable)**, with 1.1.8 and 1.0.9 also stable and 2.1.0-RC1 as preview — so doc pages cited below are 2.0.2 prose describing a 2.0.x surface, while every interface signature and field list is from the 2.0.1 tag itself. Where the two could diverge I say so.

---

## 1. The RAG advisor surface: interfaces, methods, and the right seam

### Takeaway

The whole modular-RAG surface of 2.0.1 is **seven small functional interfaces** in `org.springframework.ai.rag.*`, all of which extend `java.util.function.Function`/`BiFunction`. There is **no router abstraction of any kind** — not in the RAG module, not anywhere in the 2.0.1 tree. `RetrievalAugmentationAdvisor` has a fixed pipeline shape (transform → expand → retrieve → join → post-process → augment) with no conditional branch and no "pick a retriever" slot. Routing must live in application code, either above the advisor or smuggled inside a `QueryTransformer`/`DocumentRetriever` implementation.

### Cited Findings

- The complete non-test source listing of `spring-ai-rag` at tag `v2.0.1` is 14 classes/interfaces plus package-info files: `Query`, `advisor/RetrievalAugmentationAdvisor`, `generation/augmentation/{QueryAugmenter, ContextualQueryAugmenter}`, `postretrieval/document/DocumentPostProcessor`, `preretrieval/query/expansion/{QueryExpander, MultiQueryExpander}`, `preretrieval/query/transformation/{QueryTransformer, CompressionQueryTransformer, RewriteQueryTransformer, TranslationQueryTransformer}`, `retrieval/join/{DocumentJoiner, ConcatenationDocumentJoiner}`, `retrieval/search/{DocumentRetriever, VectorStoreDocumentRetriever}`, `util/PromptAssert` — [spring-ai git tree, tag v2.0.1](https://github.com/spring-projects/spring-ai/tree/v2.0.1/spring-ai-rag/src/main/java/org/springframework/ai/rag)
- There is **no `DocumentSelector`** in 2.0.1 (it appears in neither the tree nor the reference page) — [spring-ai tree v2.0.1](https://github.com/spring-projects/spring-ai/tree/v2.0.1/spring-ai-rag); [RAG reference page](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- Exact signatures, verbatim from the v2.0.1 tag:

  ```java
  package org.springframework.ai.rag;
  public record Query(String text, List<Message> history, Map<String, Object> context) {
      public Query(String text) { this(text, List.of(), Map.of()); }
      public Builder mutate();            // text / history / context
      public static Builder builder();
  }
  ```
  ```java
  package org.springframework.ai.rag.preretrieval.query.transformation;
  public interface QueryTransformer extends Function<Query, Query> {
      Query transform(Query query);
      default Query apply(Query query) { return transform(query); }
  }
  ```
  ```java
  package org.springframework.ai.rag.preretrieval.query.expansion;
  public interface QueryExpander extends Function<Query, List<Query>> {
      List<Query> expand(Query query);
  }
  ```
  ```java
  package org.springframework.ai.rag.retrieval.search;
  public interface DocumentRetriever extends Function<Query, List<Document>> {
      List<Document> retrieve(Query query);
  }
  ```
  ```java
  package org.springframework.ai.rag.retrieval.join;
  public interface DocumentJoiner extends Function<Map<Query, List<List<Document>>>, List<Document>> {
      List<Document> join(Map<Query, List<List<Document>>> documentsForQuery);
  }
  ```
  ```java
  package org.springframework.ai.rag.postretrieval.document;
  public interface DocumentPostProcessor extends BiFunction<Query, List<Document>, List<Document>> {
      List<Document> process(Query query, List<Document> documents);
  }
  ```
  ```java
  package org.springframework.ai.rag.generation.augmentation;
  public interface QueryAugmenter extends BiFunction<Query, List<Document>, Query> {
      Query augment(Query query, List<Document> documents);
  }
  ```
  — all read from [raw source at tag v2.0.1](https://github.com/spring-projects/spring-ai/tree/v2.0.1/spring-ai-rag/src/main/java/org/springframework/ai/rag)
- `RetrievalAugmentationAdvisor.builder()` exposes `.documentRetriever(...)`, `.queryTransformers(...)`, `.queryAugmenter(...)`; it requires the `spring-ai-rag` artifact. `QuestionAnswerAdvisor.builder(vectorStore)` exposes `.searchRequest(...)`, `.promptTemplate(...)` and the constant `QuestionAnswerAdvisor.FILTER_EXPRESSION`; it requires `spring-ai-vector-store-advisor` — [RAG reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- `VectorStoreDocumentRetriever.builder()` exposes `.vectorStore(...)`, `.similarityThreshold(...)`, `.topK(...)`, and `.filterExpression(...)` in both a **static** and a **`Supplier<Filter.Expression>`** form, plus a `FILTER_EXPRESSION` constant readable from the advisor context per request — [RAG reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- The reference page's "Modules" section is organised strictly as Pre-Retrieval (Query Transformation, Query Expansion) → Retrieval (Document Search, Document Join) → Post-Retrieval (Document Post-Processing) → Generation (Query Augmentation). There is **no stage for selection, routing or classification**, and no table of alternatives — [RAG reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- The design is credited to the paper *"Modular RAG: Transforming RAG Systems into LEGO-like Reconfigurable Frameworks"* (arXiv:2407.21059) — [RAG reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- `DocumentJoiner`'s javadoc is the only place multiple sources are acknowledged: "Joins documents retrieved across multiple queries and daa [sic] sources" — [DocumentJoiner.java, v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-rag/src/main/java/org/springframework/ai/rag/retrieval/join/DocumentJoiner.java)
- Only one third-party `DocumentPostProcessor` is named in the docs as an example: `JinferDocumentPostProcessor` (re-ranking, removing irrelevant/redundant documents, compressing content) — [RAG reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)

### Inferences

- **The correct seam for "decide something about the question, then change what gets retrieved" is `DocumentRetriever`, not `QueryTransformer`.** `QueryTransformer` is `Query → Query`: it can only rewrite text, history and the `context` map. `DocumentRetriever` is `Query → List<Document>`: a single implementation can classify, branch to BM25 / identifier lookup / vector search, apply the standing-lifecycle filter, run RRF, and return — all behind one interface the advisor already accepts. For this project, a `RoutingDocumentRetriever implements DocumentRetriever` is the one-line integration point.
- **The second-best seam is a two-step: classify in a `QueryTransformer`, consume the verdict downstream.** Because `Query` carries an arbitrary `Map<String, Object> context`, a classifying `QueryTransformer` can write `query.mutate().context(Map.of("intent", ...))` and a downstream `DocumentRetriever` or a `Supplier`-based `filterExpression` can read it. This is the only framework-sanctioned channel for passing a classification decision through the pipeline, and it is a convention, not an API.
- **`DocumentPostProcessor` cannot route**, because it runs after retrieval: it sees `(Query, List<Document>)` and can only filter/reorder what was already fetched. It is the right seam for the project's existing cross-encoder and nothing more.
- Routing expressed as `QueryExpander` + `DocumentJoiner` (fan out to everything, then merge) is structurally possible but is *conditional retrieval inverted* — it pays for every branch. It is what the framework nudges you towards, and it is the opposite of what intent routing is for.
- The advisor pipeline is **unconditional by construction**: every interface is a total `Function`, none returns an `Optional` or a branch descriptor, and the advisor builder has no predicate slot. "Should I retrieve at all?" cannot be expressed in the advisor API — only "retrieve and then possibly return nothing", via `ContextualQueryAugmenter.allowEmptyContext(...)`.

### Gaps

- `RetrievalAugmentationAdvisor`'s full builder surface (does it accept `queryExpander`, `documentJoiner`, `documentPostProcessors` as named setters, and in what cardinality?) was not confirmed signature-by-signature from source — the reference page shows only three builder methods, and I did not read `RetrievalAugmentationAdvisor.java` line by line. The three unlisted stages are documented as pipeline participants, so they are almost certainly settable, but **I did not verify the method names**.
- The 2.0.2 reference page **no longer carries the "Modular RAG is an experimental feature and is subject to change" warning** that search snippets attribute to earlier (1.x / 2.1-SNAPSHOT) renderings of the same page. I could not determine whether the 2.0.1 rendering specifically carried that warning. Treat the experimental label as **unresolved for 2.0.1** rather than removed. (See §2 for what this means for the blog series.)

---

## 2. Is there a query-classification or routing component at all?

### Takeaway

**No.** There is nothing named router, routing, classifier or classification in the Spring AI 2.0.1 source tree relevant to queries. The only routing content Spring AI publishes is a *documentation pattern* in "Building Effective Agents" whose `RoutingWorkflow` class lives in the **examples repository**, not the library. The issue tracker shows no routing feature request and no roadmap item.

### Cited Findings

- A case-insensitive scan of the **entire** `v2.0.1` file tree for `rout|classif|intent` returns exactly two hits, both irrelevant: `models/spring-ai-openai/.../DiarizedJsonMisclassificationRecovery.java` (audio diarization) and its test — [spring-ai git tree, tag v2.0.1](https://github.com/spring-projects/spring-ai/tree/v2.0.1)
- A GitHub issue search for `repo:spring-projects/spring-ai query routing in:title` returns **zero results**. A search for `router OR classification OR classifier in:title` returns two unrelated items: open PR #1926 "feat: core abstraction for image classification models" and open issue #3857 "Enhance AI Error Classification with Message Content Analysis for Smarter Retry Decisions" — [github.com/spring-projects/spring-ai/pull/1926](https://github.com/spring-projects/spring-ai/pull/1926); [github.com/spring-projects/spring-ai/issues/3857](https://github.com/spring-projects/spring-ai/issues/3857)
- The reference docs *do* describe a Routing pattern, as pattern 3 of 5 in "Building Effective Agents" (the others: Chain Workflow, Parallelization Workflow, Orchestrator-Workers, Evaluator-Optimizer): "The Routing pattern implements intelligent task distribution, enabling specialized handling for different types of input." When to use: "Complex tasks with distinct categories of input"; "When different inputs require specialized processing"; "When classification can be handled accurately" — [Building Effective Agents reference](https://docs.spring.io/spring-ai/reference/api/effective-agents.html)
- The page's entire routing code sample is:

  ```java
  @Autowired
  private ChatClient chatClient;

  RoutingWorkflow workflow = new RoutingWorkflow(chatClient);

  Map<String, String> routes = Map.of(
      "billing",   "You are a billing specialist. Help resolve billing issues...",
      "technical", "You are a technical support engineer. Help solve technical problems...",
      "general",   "You are a customer service representative. Help with general inquiries...");

  String input = "My account was charged twice last week";
  String response = workflow.route(input, routes);
  ```
  The page never shows `RoutingWorkflow`'s package, definition, constructor or `route(...)` signature, and never shows how `input` becomes a route key. It links to `spring-projects/spring-ai-examples/agentic-patterns/routing-workflow` and says that directory "contains all the code for the examples that follow" — [Building Effective Agents reference](https://docs.spring.io/spring-ai/reference/api/effective-agents.html)
- The Spring agentic-patterns blog series runs from January 2025 ("Building Effective Agents with Spring AI (Part 1)") to at least January 2026 ("Spring AI Agentic Patterns (Part 4): Subagent Orchestration", described as hierarchical delegation to specialised subagents with their own context windows, "inspired by Claude Code's subagents") — [spring.io/blog/2025/01/21/spring-ai-agentic-patterns](https://spring.io/blog/2025/01/21/spring-ai-agentic-patterns); [spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents](https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents/)
- Spring AI 2.0.0 went GA on 2026-06-12; 2.0.1 followed on 2026-08-21 with "more than 80 issues and pull requests" — [Spring AI 2.0.0 GA](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/); [Spring AI 2.0.1 Available Now](https://spring.io/blog/2026/08/21/spring-ai-2-0-1-available-now)
- A Spring blog post of 2026-10-02, "Spring AI Modular RAG and TypeSafe Jev: Retrieve More, Keep Only What Answers", demonstrates a modular-RAG pipeline where LLM calls rewrite the question pre-retrieval and a `DocumentPostProcessor` filters and reranks before the prompt — i.e. Spring's own current showcase of modular RAG is **expansion-plus-rerank, not routing** — [spring.io/blog/2026/10/02/spring-ai-modular-rag-typesafe-jev](https://spring.io/blog/2026/10/02/spring-ai-modular-rag-typesafe-jev)

### Inferences

- **Shipped in 2.0.1:** the seven RAG interfaces and their handful of implementations; `FILTER_EXPRESSION` plumbing; `DocumentJoiner` for multi-query/multi-source merge.
- **Experimental in 2.0.1:** unresolved — the modular-RAG experimental warning is present in some renderings of the page and absent from the current 2.0.2 one (see §1 Gaps). The *safe claim for the blog* is that the modular-RAG API is young and its own docs have carried an instability warning within its lifetime.
- **Not present, must be hand-written:** query classification; any router abstraction; conditional retrieval; multi-retriever selection; a semantic router; a label-set classifier; any registry mapping intents to retrievers. All of it.
- `RoutingWorkflow` is **sample code in `spring-ai-examples`**, not API. A blog series that cites it as "Spring AI's routing support" would be wrong. The honest framing: Spring AI documents routing as a *pattern you write*, in exactly the same sense that it documents chaining as a pattern you write — the framework contribution is `ChatClient`, not the router.
- The absence of any routing issue in the tracker is a meaningful negative: this is not a feature with a roadmap, a milestone or a community champion. Nothing suggests 2.1 will change that (the 2.1-SNAPSHOT RAG page shows the same module list).
- **For the series' central question:** intent routing on Spring AI 2.0.1 is *a story that happens to be written in Java*. Spring AI supplies the chat/embedding client, the structured-output machinery, the advisor chain to hang the result on, and `DocumentRetriever` as the plug. It supplies none of the routing. The interesting Spring AI angle is therefore not "look what the framework gives you" but "here is the one correct seam (`DocumentRetriever`), here is what the framework's abstraction hides (logprobs — §4), and here is where its structured-output retry loop genuinely buys you something (§3)."

### Gaps

- I did not open `spring-projects/spring-ai-examples/agentic-patterns/routing-workflow` to read the actual `RoutingWorkflow` implementation. Whether its classifier is prompt-plus-parse, structured output, or embeddings is **unverified** — worth one fetch before the blog describes it.
- Spring AI maintainer talks from 2025-2026 were not surveyed; I found no transcript or slide deck in search results that addressed query routing. Absence of evidence here, not evidence of absence.

---

## 3. Structured classification output

### Takeaway

2.0 is a real step change here, and it is the strongest Spring AI story in this whole research round. 2.0 adds **three stacked mechanisms**: prompt-plus-parse (`BeanOutputConverter`, as in 1.x), request-side provider-native schema enforcement (`useProviderStructuredOutput()`), and a response-side **self-correcting retry loop** (`validateSchema()` → `StructuredOutputValidationAdvisor`, 3 attempts by default). None of these is constrained decoding on Ollama; for genuine constrained decoding the docs point at a third-party provider with GBNF grammars.

### Cited Findings

- `.entity(...)` accepts a `Class`, a `ParameterizedTypeReference`, **or a converter instance**, and in 2.0 also a second argument: an `EntityParamSpec` consumer exposing `validateSchema()` and `useProviderStructuredOutput()` — [Output Converters (2.0.2)](https://docs.spring.io/spring-ai/reference/api/structured-output/converters.html); [Schema Validation & Self-Correction](https://docs.spring.io/spring-ai/reference/api/structured-output/validation.html)
- `BeanOutputConverter<T>` (`org.springframework.ai.converter`) injects format instructions requesting JSON matching a **`DRAFT_2020_12`** schema derived from the type, then deserialises with a `JsonMapper`. Property order is controllable with `@JsonPropertyOrder`. The docs call it "a best effort" and say the model is "not guaranteed" to comply — [Output Converters](https://docs.spring.io/spring-ai/reference/api/structured-output/converters.html)
- **New in 2.0:** `StructuredOutputConverter.getJsonSchema()` is a default method returning the schema or `NO_JSON_SCHEMA` (`""`). It is the hook both reliability switches depend on — a custom converter that leaves it at the default silently disables both — [Output Converters](https://docs.spring.io/spring-ai/reference/api/structured-output/converters.html)
- The converter classes at `v2.0.1` are `org.springframework.ai.converter.{StructuredOutputConverter, BeanOutputConverter, MapOutputConverter, ListOutputConverter, AbstractConversionServiceOutputConverter, AbstractMessageOutputConverter}` in `spring-ai-model`; `StructuredOutputValidationAdvisor` is at `org.springframework.ai.chat.client.advisor` in `spring-ai-client-chat`; `StructuredOutputChatOptions` and `DefaultStructuredOutputChatOptions` are at `org.springframework.ai.model.tool` in `spring-ai-model` — [spring-ai git tree, tag v2.0.1](https://github.com/spring-projects/spring-ai/tree/v2.0.1)
- **Self-correcting loop, exact behaviour:** model responds → response validated against the JSON schema for the target type → pass returns the typed result; fail appends the validation error (docs' examples: `missing required field "actor"`, `expected array, got string`) to the user prompt and re-issues the call, **up to 3 attempts by default**. Token usage is **accumulated across all attempts**, so the returned `ChatResponse` reports the total. **Streaming is not supported** with `validateSchema()` — [Schema Validation & Self-Correction](https://docs.spring.io/spring-ai/reference/api/structured-output/validation.html)
- Builder: `StructuredOutputValidationAdvisor.builder().outputType(X.class).maxRepeatAttempts(5).build()`, or `.outputJsonSchema(myConverter.getJsonSchema())` (mutually exclusive with `outputType`); an optional custom `JsonMapper`; registerable as a `defaultAdvisors(...)`. An explicitly registered advisor replaces the auto-registered one — [Schema Validation & Self-Correction](https://docs.spring.io/spring-ai/reference/api/structured-output/validation.html)
- **Parse failure:** the docs state `BeanOutputConverter` "will throw" on output that is not parseable JSON but **do not name the exception type**; null-return is not documented; automatic retry is not a converter behaviour — [Output Converters](https://docs.spring.io/spring-ai/reference/api/structured-output/converters.html). Likewise the validation page **does not name the exception** thrown when all attempts are exhausted — [Schema Validation & Self-Correction](https://docs.spring.io/spring-ai/reference/api/structured-output/validation.html)
- **Provider-native path:** `spec -> spec.useProviderStructuredOutput()` removes the JSON format instruction from the prompt and sends the schema as an API field. Global enablement via the advisor param `AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT`, per request or as a builder default. Support is detected by **whether the model's chat options implement `StructuredOutputChatOptions`**; if they don't, **the flag is silently ignored** and the prompt-based path is used — [Provider-Native Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output/native.html)
- Native-support table: OpenAI (GPT-4o+), Anthropic (Claude 3.5 Sonnet+), Google GenAI (Gemini 1.5 Pro+), Mistral AI (Mistral Small+), **Ollama (model-specific)**, and "Qxotic jinfer" (all chat models, "schema compiled to a grammar and enforced during sampling") — [Provider-Native Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output/native.html)
- Native output is **off by default**, has **partial schema support** (`$ref`, deeply nested arrays, `allOf`/`anyOf`/`oneOf`, regex patterns and recursive types are named as common limitations), and on OpenAI **rejects top-level arrays** — `List<T>` must be wrapped in a record. The prompt-based default does accept top-level arrays — [Provider-Native Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output/native.html)
- 1.x → 2.0 **behaviour change in `BeanOutputConverter`**: it now uses `JsonSchemaGenerator`; `@JsonProperty(required = false)` or a bare `@JsonProperty` is **no longer treated as required**; Kotlin nullable/defaulted properties drop out of `required`; primitives gain OpenAPI `format` hints (`int32`, `int64`, `date-time`); `postProcessSchema(JsonNode)` **was removed** in favour of overriding `generateSchema()` and calling `super.generateSchema()` — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- The docs recommend `@JsonProperty(required = true, ...)` so the generated schema marks fields required — [Ollama Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)

### Inferences

- **An LLM intent classifier can be made schema-deterministic but not output-deterministic.** `validateSchema()` guarantees you get a well-formed instance of `record Intent(IntentLabel label)` or an exhausted-retry failure; it guarantees nothing about *which* label. For a regression-gated suite, that means a two-layer assertion: schema conformance is a framework property you can take for granted, and label correctness is a dataset property you must measure.
- **The retry loop is a determinism hazard for a gated suite, not only a help.** Because the validation error is appended to the user prompt and the call re-issued, attempt 2 runs a *different prompt* from attempt 1. With a pinned seed, attempt 1 is reproducible; attempt 2 is reproducible only if the error text is byte-stable. A regression suite should therefore either set `maxRepeatAttempts(1)` to make failures loud and the prompt fixed, or accept that the recorded transcript includes the repair turns. Accumulated token counts across attempts also means any token-budget assertion must account for repairs.
- **`useProviderStructuredOutput()` failing silently is the sharpest footgun in this area.** If `OllamaChatOptions` did not implement `StructuredOutputChatOptions`, the switch would be a no-op with no warning. It *does* implement it at v2.0.1 (§4), so the project is fine — but any custom `ChatOptions`, or a swap to a provider without it, degrades to prompt-plus-parse invisibly. Worth an assertion in the test suite rather than trust.
- **Constrained decoding is not available via Ollama through Spring AI.** Ollama's `format`-as-schema is enforcement by the Ollama runtime's own sampler, which the docs themselves describe as partial and model-dependent; the only entry in the native table claiming grammar-constrained *sampling* is the third-party jinfer provider (`JinferChatOptions.grammar(...)`, GBNF). For a hard guarantee, that is the only Spring AI-shaped option.
- The `BeanOutputConverter` `required` semantics change is a **genuine 1.x-tutorial invalidator** with teeth: a classifier record carried over from a 1.x blog post may now emit a schema where the label field is optional, which silently weakens both native enforcement and `validateSchema()`.

### Gaps

- **The exception types are unnamed in the docs** both for a converter parse failure and for exhausted validation attempts. These matter for writing a test that asserts failure, and I did not read `BeanOutputConverter.java` / `StructuredOutputValidationAdvisor.java` to recover them. **Flag as a must-check-in-source item.**
- The builder method name for supplying a custom `JsonMapper` to `StructuredOutputValidationAdvisor` is not documented.
- Whether `validateSchema()` is provider-restricted is not stated; the page does not restrict it, and only mentions Ollama in the context of *native* output limitations.

---

## 4. Ollama specifics through Spring AI

### Takeaway

Everything the project needs is reachable through `OllamaChatOptions` in 2.0.1 — **except logprobs, which are not reachable at all**. `OllamaChatOptions` at `v2.0.1` carries `seed`, `numCtx`, `keepAlive`, `format` (as `Object`, so string *or* schema map), `outputSchema(String)`, and a first-class `ThinkOption` with `enableThinking()` / `disableThinking()` / `thinkLow()` / `thinkMedium()` / `thinkHigh()`. The class implements `StructuredOutputChatOptions`, so `useProviderStructuredOutput()` is live for Ollama.

### Cited Findings

- `OllamaChatOptions` at v2.0.1 declares: `public class OllamaChatOptions implements ToolCallingChatOptions, StructuredOutputChatOptions` — [OllamaChatOptions.java, v2.0.1 L48](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- Its full field list at v2.0.1 (all `@Nullable` unless noted): `useNUMA, numCtx, numBatch, numGPU, mainGPU, lowVRAM, f16KV, logitsAll, vocabOnly, useMMap, useMLock, numThread, numKeep, seed, numPredict, topK, topP, minP, tfsZ, typicalP, repeatLastN, temperature, repeatPenalty, presencePenalty, frequencyPenalty, mirostat, mirostatTau, mirostatEta, penalizeNewline, stop, model (non-null), format (Object), keepAlive (String), truncate, thinkOption (ThinkOption), toolCallbacks, toolContext` — [OllamaChatOptions.java, v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- **There is no `logprobs` field.** A case-insensitive grep for `logprob` across **every** `.java` file in `models/spring-ai-ollama/src/main/java` at v2.0.1 returns exactly two hits, both in javadoc, not fields:
  ```
  /**
   * Return logits for all the tokens, not just the last one.
   * To enable completions to return logprobs, this must be true.
   */
  private final @Nullable Boolean logitsAll;
  ```
  (one in `OllamaChatOptions.java`, one in the parallel comment in `OllamaEmbeddingOptions.java`). `OllamaApi.java` contains **zero** occurrences of `logprob` or `logits` — [OllamaChatOptions.java, v2.0.1 L152-156](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java); [OllamaApi.java, v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaApi.java)
- For contrast, `OpenAiChatOptions.java` at v2.0.1 contains **13** occurrences of `logprobs` — so logprobs are a concept Spring AI models for OpenAI and simply does not plumb for Ollama — [OpenAiChatOptions.java, v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-openai/src/main/java/org/springframework/ai/openai/OpenAiChatOptions.java)
- `NON_SUPPORTED_FIELDS = List.of("model", "format", "keep_alive", "truncate")` — i.e. these four are request-level fields, not entries in Ollama's nested `options` object — [OllamaChatOptions.java, v2.0.1 L52](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- Builder methods confirmed in source at v2.0.1: `.outputSchema(@Nullable String)` (which parses the schema and assigns it to `format` via `jsonHelper.fromJsonToMap(...)`), `.thinkOption(ThinkOption)`, and the convenience setters assigning `ThinkOption.ThinkBoolean.ENABLED` / `.DISABLED` and `ThinkOption.ThinkLevel.LOW` / `.MEDIUM` / `.HIGH`. `ThinkOption` is its own type at `org.springframework.ai.ollama.api.ThinkOption` — [OllamaChatOptions.java, v2.0.1 L1051-1118](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- `OllamaOptions` is **deprecated** in favour of `OllamaChatOptions` (chat) and `OllamaEmbeddingOptions` (embedding) — [Ollama Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- Documented properties and defaults: `spring.ai.ollama.chat.seed` (default `-1`; "fixed value gives repeatable output for the same prompt"), `spring.ai.ollama.chat.format` (`"json"` or a JSON Schema object), `spring.ai.ollama.chat.num-ctx` (default `2048`), `spring.ai.ollama.chat.keep_alive` (default `5m`, **shown with an underscore** unlike the surrounding kebab-case names), `spring.ai.ollama.chat.think`, `top-k` (`40`), `top-p` (`0.9`), `temperature` (`0.8`), `num-predict` (`-1`). **`logprobs` is not documented; `logits-all` is listed and is a different setting** — [Ollama Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- Thinking: models on Ollama 0.12+ such as `qwen3`, `deepseek-r1`, `deepseek-v3.1`, `gpt-oss` **auto-enable thinking when the option is unset**; the trace is read back via `response.getResult().getMetadata().get("thinking")` while the answer stays in `getOutput().getText()`. GPT-OSS takes levels rather than a boolean — [Ollama Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- **1.x → 2.0 rename:** `spring.ai.ollama.chat.think-option` → `spring.ai.ollama.chat.think`. Also general: the `.options` segment was dropped from **non-chat** property keys (deprecated `.options` variants still work); `ChatModel.getDefaultOptions()` → `getOptions()`; `copy()` / `fromOptions()` → `mutate()...build()`; `N(...)` → `n(...)` — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- Ollama-specific provider property for JSON: `spring.ai.ollama.chat.format`, where "the only accepted value is `json`" per the native-structured-output page — which **conflicts** with the Ollama chat page's statement that `format` accepts `"json"` **or a JSON Schema object**, and with the source, where `format` is typed `Object` and `.outputSchema(...)` writes a parsed schema map into it — [Provider-Native Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output/native.html) vs. [Ollama Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html) and [OllamaChatOptions.java, v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java)
- Named Ollama native-output hazard: reasoning models such as `qwen3:8b` and `qwen3.5:9b` may return reasoning as plain text, producing `StreamReadException: Unrecognized token 'The': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')` in `BeanOutputConverter`. Documented workarounds: switch model (e.g. `llama3.1:latest`), fall back to prompt-based, or combine `useProviderStructuredOutput()` with `validateSchema()` — [Provider-Native Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output/native.html)
- Upstream Ollama logprobs status is **murky**: an `ollama-python` issue reports passing `logprobs` to `chat()` returned no token-level probabilities and was undocumented; a third-party library obtains per-token logprobs from the **`/api/generate`** endpoint with `logprobs=True`, a `suffix`, and `num_predict=0`; a third-party API reference mirror lists the chat endpoint with no logprobs field — [ollama/ollama-python#517](https://github.com/ollama/ollama-python/issues/517); [ollama-classifier backend source](https://ollama-classifier.readthedocs.io/en/latest/_modules/ollama_classifier/backends/ollama.html); [Ollama API reference mirror](https://ollama.collabnix.com/docs/api/)

### Inferences

**Per-item reachability through the Spring AI abstraction, at 2.0.1:**

| Capability | Through `OllamaChatOptions`? | Note |
|---|---|---|
| `seed` | **Yes** | field + `spring.ai.ollama.chat.seed`, default `-1` |
| `top_p` / `top_k` / `temperature` | **Yes** | — |
| `num_ctx` | **Yes** | `numCtx` + `spring.ai.ollama.chat.num-ctx`, default 2048 |
| `keep_alive` | **Yes** | request-level field (in `NON_SUPPORTED_FIELDS`, i.e. not nested under `options`) |
| `format: "json"` | **Yes** | `format` is `Object` |
| JSON-schema structured output | **Yes** | `.outputSchema(String)`, plus portable `useProviderStructuredOutput()` since the class implements `StructuredOutputChatOptions` |
| disable thinking (`think`) | **Yes** | `.disableThinking()` / `ThinkOption`; **and you need it** — thinking auto-enables on qwen3/deepseek/gpt-oss when unset |
| **per-token logprobs** | **No** | **not reachable**; only `logitsAll`, the upstream *enabling* flag, with no response-side plumbing |

- **This is the clearest case of the abstraction hiding something the project needs.** `logitsAll` is present as a request option and its own javadoc says it is the prerequisite "to enable completions to return logprobs" — but `OllamaApi` has no logprobs response field, so Spring AI can set the switch and then discard the result. Any confidence-calibrated classifier (abstain below a logprob threshold; margin between top-1 and top-2 labels) is **unreachable through `ChatClient` on Ollama in 2.0.1**, and would require dropping to the Ollama HTTP API directly — which, per the upstream evidence, may itself only work on `/api/generate`, not `/api/chat`. That makes logprob-gated routing a two-layer problem, not a one-line config.
- `disableThinking()` is load-bearing for this project independently of routing: a reasoning model that auto-enables thinking is both non-deterministic in latency and the documented cause of the `StreamReadException` in native structured output. The pinned-model story (seed, top_p, top_k, keep_alive) is **incomplete without `.disableThinking()`** if any qwen3/deepseek/gpt-oss model is in play.
- The `keep_alive` property being rendered with an underscore while its neighbours are kebab-case is almost certainly a docs inconsistency rather than two real keys, but since the project already sets it, it is worth confirming the binding resolves.
- The two docs pages contradict each other on whether Ollama's `format` accepts a schema. **The source is decisive and says yes** (`Object` + `.outputSchema(...)`); the native page's "only accepted value is `json`" should be read as describing the raw *property* binding, not the programmatic builder.

### Gaps

- Whether current Ollama (late 2026) returns logprobs on `/api/chat` is **unresolved**: I found only a possibly-stale `ollama-python` issue, a third-party library using `/api/generate`, and a third-party docs mirror. I did not reach the official Ollama API docs in `ollama/ollama/docs/api.md`. This should be checked directly against the installed Ollama version before the series claims logprobs are unavailable *upstream* — the confirmed claim is narrower: **unavailable through Spring AI 2.0.1**.
- Whether `spring.ai.ollama.chat.keep_alive` and a kebab-case `keep-alive` both bind was not verified.

---

## 5. Embedding-based classification in Java

### Takeaway

Spring AI gives you `EmbeddingModel` and nothing else. The only thing named `EmbeddingUtils` in the 2.0.1 tree is a **float/double array-conversion helper** — there is no cosine similarity, no dot product, no normalisation, no similarity utility anywhere in the framework. A semantic router is entirely hand-rolled, and I found no semantic-router project in the Spring ecosystem.

### Cited Findings

- `org.springframework.ai.model.EmbeddingUtils` (in `spring-ai-model`) at v2.0.1 is a `final` class with a private constructor whose entire public surface is type conversion: `doubleToFloat(List<Double>) -> List<Float>`, `toPrimitive(List<Float>) -> float[]`, `toPrimitive(Float[]) -> float[]`, `toFloatArray(float[]) -> Float[]`. **No similarity method.** — [EmbeddingUtils.java, v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-model/src/main/java/org/springframework/ai/model/EmbeddingUtils.java)
- A tree-wide scan for `similarity|cosine|distance` in `.java` files at v2.0.1 returns only store-specific and observability types: `org.springframework.ai.observation.conventions.VectorStoreSimilarityMetric`, `CouchbaseSimilarityFunction`, Elasticsearch `SimilarityFunction`, and a Redis distance-metric integration test. There is **no general-purpose similarity utility** — [spring-ai git tree, tag v2.0.1](https://github.com/spring-projects/spring-ai/tree/v2.0.1)
- A scan for `EmbeddingUtils|VectorUtil|MathUtil` returns only `EmbeddingUtils` (above), its test, and `VertexAiEmbeddingUtils` (provider-specific) — [spring-ai git tree, tag v2.0.1](https://github.com/spring-projects/spring-ai/tree/v2.0.1)
- A GitHub issue-title search across the repo for `router OR classification OR classifier` turns up no semantic-router work (see §2) — [spring-projects/spring-ai issues](https://github.com/spring-projects/spring-ai/issues)
- The closest Java-ecosystem router I found is **not** Spring AI: AgentScope Java documents a router in which "an LLM or rule-based classifier" picks which specialist agents to call and what sub-query to send each. It runs on Spring Boot but is a different framework — [java.agentscope.io routing docs](https://java.agentscope.io/en/multi-agent/routing.html)
- Published latency figures for the two router styles, from a third-party course rather than a primary benchmark: rule-based systems add 10-50ms; LLM-based classifiers add 500-2000ms — [Educative, "Routing with LLM-based classifiers for complex tasks"](https://educative.io/courses/advanced-rag-techniques-choosing-the-right-approach/routing-with-llm-based-classifiers-for-complex-tasks)
- General statement of the semantic-router pattern: it "commonly uses an embedding representation of the query and compares it with vectors for supported intents, tools, or agents" — [vLLM blog, "Signal-Decision Driven Architecture"](https://vllm.ai/blog/2025-11-19-signal-decision)

### Inferences

- An embedding classifier in Spring AI 2.0.1 is: one `embeddingModel.embed(List.of(labels...))` call at startup, one `embed(query)` per request, and **your own** cosine loop over `float[]`. That is perhaps fifteen lines — which is precisely the problem for a "with Spring AI" framing: the framework's contribution is the `EmbeddingModel` port and nothing above it.
- There is a real angle here even so: because the embeddings are `float[]` and the label set is tiny and fixed, an embedding router is *deterministic* in a way an LLM classifier is not — same model, same labels, same query gives the same argmax, with no seed, no temperature and no retry loop. For a regression-gated suite that is a materially different testing story from §3, and it is the one place where the Java/Spring version is arguably *easier* to make deterministic than the Python prototype's LLM route.
- The latency figures are third-party and uncited-to-primary; use them as orders of magnitude only, or better, measure on the project's own 5090 tier rather than quoting them.

### Gaps

- I did not search Spring Community / `spring-ai-community` GitHub org, Awesome-Spring-AI lists, or Maven Central for a third-party `semantic-router`-style Java library. **Absence here is unsearched, not established.** One targeted search of the `spring-ai-community` org would close this.
- No primary benchmark for rule-based vs LLM-classifier routing latency was found.

---

## 6. Tool calling / function calling as a routing mechanism

### Takeaway

Tool calling in 2.0 is substantially re-architected — the execution loop moved out of `ChatModel` and into an auto-registered `ToolCallingAdvisor` — and it *could* express a router (one tool per retriever, let the model pick). But the docs give no mechanism for forcing a tool choice, explicitly warn that selection accuracy degrades past ~30 tools, and make no determinism promise. For a 3-5 branch retrieval router it is expressive but strictly weaker than structured-output classification, because you lose the ability to assert on the decision separately from the action.

### Cited Findings

- **The core 2.0 change:** "the per-`ChatModel` execution loop from 1.x has been removed. `ChatClient` auto-registers a `ToolCallingAdvisor`, which runs the loop. Calling `ChatModel` directly sends tool definitions, but tool calls in the response are **not** executed automatically." — [Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html)
- Interface, verbatim:
  ```java
  public interface ToolCallback {
      ToolDefinition getToolDefinition();
      ToolMetadata getToolMetadata();
      String call(String toolInput);
      String call(String toolInput, ToolContext toolContext);
  }
  ```
  — [Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html)
- Declarative: `@Tool(name, description, returnDirect, resultConverter)` and `@ToolParam(description, required)`; parameters are **required by default**, and `@Nullable` also marks one optional. Programmatic: `MethodToolCallback.builder().toolDefinition(ToolDefinitions.builder(method)...).toolMethod(method).toolObject(obj).build()` and `FunctionToolCallback.builder(name, fn).inputType(X.class).build()`. `ToolDefinition` (`name()`, `description()`, `inputSchema()`) is what the model sees; `ToolMetadata.builder().returnDirect(true)` carries return-direct — [Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html)
- Wiring: `.tools(...)` per call (appended to defaults), `.defaultTools(...)` on the builder, `.toolContext(Map)` merged with runtime winning. `ChatModel`-level tools set via `ToolCallingChatOptions.builder().toolCallbacks(tools).build()` are **overridden** by a request-level tool list. `DefaultChatClient` enforces **exactly one `ToolAdvisor`** in the chain — [Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html)
- Loop extension points on `ToolCallingAdvisor` (protected hooks): `doInitializeLoop`/`doInitializeLoopStream`, `doBeforeCall`/`doBeforeStream`, `doAfterCall`/`doAfterStream`, `doFinalizeLoop`/`doFinalizeLoopStream`. Custom builders register as a `ToolCallingAdvisor.Builder<?>` bean with `@ConditionalOnMissingBean` — [Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html)
- Opt-out: `.advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))` per call, or `spring.ai.chat.client.tool-calling.enabled=false` globally; then drive `toolCallingManager.executeToolCalls(prompt, response.chatResponse())` in your own `while (response.chatResponse().hasToolCalls())` loop — [Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html)
- Limits: `ToolCallingManager.builder().maxCallsPerTool(40).maxCallsPerTool("search", 10).excludeToolFromLimit("...").maxTotalToolCalls(150).onLimitExceeded(ToolCallLimitBehavior.THROW | RETURN_ERROR_RESPONSE)`. Defaults `DEFAULT_MAX_CALLS_PER_TOOL` = 40, `DEFAULT_MAX_TOTAL_TOOL_CALLS` = 150; counts are **per turn, scoped from the last `UserMessage`**. On breach `DefaultToolCallingManager` throws `ToolCallLimitExceededException` carrying the tool name (null for a total breach), the limit and the partial `ToolExecutionResult`; `ToolCallingAdvisor` catches it and returns **one `Generation`** via `ex.buildGeneration()` with finish reason `ToolCallLimitExceededException.FINISH_REASON`, preserving earlier successes under `METADATA_PARTIAL_TOOL_RESPONSES`. Properties: `spring.ai.tools.limits.{max-calls-per-tool-default, max-calls-per-tool.<name>, excluded-tools, max-total-tool-calls, on-limit-exceeded}` — [Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html); [Spring AI 2.0.1 Available Now](https://spring.io/blog/2026/08/21/spring-ai-2-0-1-available-now)
- Errors: tool exceptions wrap in `ToolExecutionException` and pass to `ToolExecutionExceptionProcessor.process(...)`; `DefaultToolExecutionExceptionProcessor` returns `RuntimeException` messages to the model and **always rethrows checked exceptions and `Error`s**; override via `new DefaultToolExecutionExceptionProcessor(true)` or `spring.ai.tools.throw-exception-on-error=true` — [Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html)
- Resolution: `ToolCallbackResolver.resolve(String toolName)` returns `@Nullable ToolCallback`, default `StaticToolCallbackResolver`. **Resolution fallback is off by default** (`spring.ai.tools.resolution.fallback.enabled=false`); the docs warn enabling it makes every resolvable tool executable. Local/MCP name collisions are rejected by `.tools()`/`.defaultTools()` with `IllegalStateException`, but outside `ChatClient` the first match wins silently unless you call `ToolCallingChatOptions.validateToolCallbacks(...)` — [Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html)
- **Reliability guidance, as given:** write a clear `description` ("Without it the model has no guidance on when to call the tool"); mark `required` correctly, since "a required parameter the model cannot know forces it to invent a value, which causes hallucinated arguments"; keep destructive tools out of defaults; **"At 30+ tools, selection accuracy degrades"** — use `ToolSearchToolCallingAdvisor` (`spring.ai.chat.client.tool-search-advisor.enabled=true`) for progressive disclosure. The page **does not promise deterministic tool selection** and **does not document `toolChoice`, `internalToolExecutionEnabled`, or any way to force a tool call** — [Tool Calling reference](https://docs.spring.io/spring-ai/reference/api/tools.html)
- New in 2.0: `ToolSearchToolCallingAdvisor` with starter `spring-ai-starter-tool-search-advisor`, index type via `tool-index-type` = `regex` | `lucene` | `vector`. Also new marker interfaces `ToolAdvisor` and `MemoryAdvisor` — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- 2.0.1 specifically: **OpenAI strict tool calling now defaults to `false`**, to fix 400 errors on optional tool parameters — [Spring AI 2.0.1 Available Now](https://spring.io/blog/2026/08/21/spring-ai-2-0-1-available-now)

### Inferences

- A tool-selection router is **structurally easy and epistemically worse** than a classifier. With `@Tool`-annotated `searchByIdentifier` / `searchByKeyword` / `searchSemantic` methods on the chat client, the model routes implicitly. But the decision and the retrieval collapse into one step, so a regression suite can only assert on the final answer, not on the route — the opposite of what a gated intent-routing suite needs. Structured-output classification keeps the verdict as a first-class, assertable value.
- `ToolSearchToolCallingAdvisor` with `tool-index-type: vector` is, read sideways, **Spring AI's only shipped semantic router** — it semantically selects among tools rather than retrievers. It is a genuinely interesting framing for the series, but it routes *tools for an agent*, not *retrievers for a query*, and the docs motivate it by scale (30+ tools), not by intent.
- The absence of any documented `toolChoice`/forced-tool mechanism on the tools page is the decisive argument against tool-calling-as-router for a deterministic suite: you cannot even force the model to make *a* choice, let alone a reproducible one.
- The `ChatClient`-auto-registers-`ToolCallingAdvisor` change is a hard **1.x-tutorial invalidator**: any carried-over code that explicitly adds `.advisors(ToolCallingAdvisor.builder().build())` now applies it twice.

### Gaps

- `toolChoice` / forced tool invocation may exist on the sub-pages the tools page links to (`api/tools/tool-calling-advisor.html`, `api/tools/chatmodel-tool-calling.html`) or in provider-specific options — **I did not fetch those**. `OpenAiChatOptions` certainly has `toolChoice` historically; whether `OllamaChatOptions` has an equivalent is unconfirmed (it was **not** in the v2.0.1 field list I read, which is good evidence it does not).

---

## 7. Testing and determinism

### Takeaway

`spring-ai-test` is **not** a test-double or record/replay library. At v2.0.1 it contains eight files, and its headline feature is `BasicEvaluationTest` — an LLM-as-judge base class that calls a real model. There is **no recorded-cassette facility, no `MockChatModel`, no stub `ChatClient`** in the framework. The determinism story for an LLM-in-the-loop component is assembled from pinned Ollama options (§4), schema validation (§3) and your own fixtures.

### Cited Findings

- Complete `spring-ai-test/src/main/java` listing at v2.0.1: `org/springframework/ai/test/CurlyBracketEscaper.java`, `test/chat/client/advisor/{AbstractToolCallingAdvisorAutoRegistrationIT, AbstractToolCallingAdvisorIT, MockWeatherService}.java`, `test/options/AbstractChatOptionsTests.java`, `test/vectorstore/{BaseVectorStoreTests, ObservationTestUtil}.java`, `utils/AudioPlayer.java` — [spring-ai tree v2.0.1](https://github.com/spring-projects/spring-ai/tree/v2.0.1/spring-ai-test)
- Its resources are four StringTemplate prompts under `prompts/spring/test/evaluation/` (`qa-evaluator-accurate-answer.st`, `qa-evaluator-fact-based-answer.st`, `qa-evaluator-not-related-message.st`, `user-evaluator-message.st`) plus three sample text documents — [spring-ai tree v2.0.1](https://github.com/spring-projects/spring-ai/tree/v2.0.1/spring-ai-test)
- The module README advertises exactly three features: "**BasicEvaluationTest**: A base test class for evaluating question-answer quality using AI models", "**Vector Store Testing**", "**Audio Testing**" — [spring-ai-test README, v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-test/README.md)
- `BasicEvaluationTest` usage is `extends BasicEvaluationTest` + `evaluateQuestionAndAnswer(question, answer, factBased)`. It "requires a `ChatModel` bean (typically OpenAI)" and evaluation prompt templates on the classpath. Its process: "1. Checks if the answer is related to the question 2. Evaluates the accuracy/appropriateness of the answer 3. Fails the test with detailed feedback if the answer is inadequate" — [spring-ai-test README, v2.0.1](https://github.com/spring-projects/spring-ai/blob/v2.0.1/spring-ai-test/README.md)
- Note the asymmetry: `MockWeatherService` and the `Abstract*IT` classes are shipped in `src/main` of the test module — they are **harness for Spring AI's own provider ITs**, not an application-facing mocking API — [spring-ai tree v2.0.1](https://github.com/spring-projects/spring-ai/tree/v2.0.1/spring-ai-test)
- `spring.ai.ollama.chat.seed` is documented as giving "repeatable output for the same prompt" (default `-1`) — [Ollama Chat reference](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- `validateSchema()` **does not support streaming** and accumulates token usage across attempts — both relevant to how a test observes a call — [Schema Validation & Self-Correction](https://docs.spring.io/spring-ai/reference/api/structured-output/validation.html)

### Inferences

- **`BasicEvaluationTest` is the wrong tool for a regression-gated routing suite**, and saying so is a defensible blog point. It is non-deterministic (a second model judges), it needs a live model, it asserts on answer *quality* rather than on a routing *decision*, and it gives a boolean rather than a confusion matrix. A routing suite wants a fixed labelled set, an argmax, and per-class precision/recall — none of which Spring AI offers.
- Because `ChatModel` and `EmbeddingModel` are plain interfaces, the practical pattern is the ordinary Spring one: hand-write a stub `ChatModel` returning canned `ChatResponse`s for unit tests, and run a separate, slower, seed-pinned integration suite against real Ollama for the actual classification accuracy numbers. Spring AI contributes the interface boundary; the discipline is yours. This is the same conclusion as §2 and §5, and it is the honest through-line for the series.
- The determinism toolkit that *does* exist is the union of: pinned `seed` + `top_p`/`top_k`, `.disableThinking()`, `num_ctx` pinned so context truncation cannot vary, `keep_alive` so the model is not reloaded mid-suite, a fixed `outputSchema`, and `maxRepeatAttempts(1)` to keep the prompt from mutating under retry. Every one of those is reachable through Spring AI; none of them is a *testing* feature.

### Gaps

- **I found no evidence of any record/replay or cassette facility in Spring AI 2.0.1**, and no community convention for one surfaced in my searches. I did not specifically search for WireMock-based Spring AI testing recipes or a `spring-ai-community` test module — so "nothing exists" is confirmed only for the framework's own `spring-ai-test` module.
- How practitioners actually regression-test LLM-in-the-loop components in the Spring ecosystem was not established from primary sources; I have the framework's surface, not the community's practice.

---

## 8. Migration / version notes: what invalidates older tutorials

### Takeaway

The 1.x → 2.0 break is large and lands squarely on the advisor chain, which is exactly where a routing blog post would reach. Four changes will silently break or mislead code copied from 1.x tutorials: auto-registered `ToolCallingAdvisor`, the removal of `PromptChatMemoryAdvisor`, the vector-store-advisor module rename, and `BeanOutputConverter`'s new `required` semantics.

### Cited Findings

- **Module rename:** `spring-ai-advisors-vector-store` → **`spring-ai-vector-store-advisor`** (the name the project already uses) — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html); [Spring AI 2.0.1 Available Now](https://spring.io/blog/2026/08/21/spring-ai-2-0-1-available-now)
- **Class rename:** `ToolCallAdvisor` → `ToolCallingAdvisor` — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- **Behaviour change:** "`ChatClient` now auto-registers `ToolCallingAdvisor`. Remove explicit `.advisors(ToolCallingAdvisor.builder().build())` calls, or they will be applied twice." — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- **Removed:** `PromptChatMemoryAdvisor` (use `MessageChatMemoryAdvisor`; same builder API, but history is sent as chat messages rather than injected into the system prompt); `ChatMemory.DEFAULT_CONVERSATION_ID` (`"default"`); `.conversationId(String)` on `MessageChatMemoryAdvisor.Builder` and `VectorStoreChatMemoryAdvisor.Builder` — pass it per call via `.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "my-session"))`; `streamToolCallResponses` on three advisor builders and its two properties — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- **Advisor ordering changed:** `Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER` moved from `HIGHEST_PRECEDENCE + 1000` to `HIGHEST_PRECEDENCE + 200`; `ToolCallingAdvisor` sits at `HIGHEST_PRECEDENCE + 300`. `BaseChatMemoryAdvisor.getConversationId(Map, String)` → `getConversationId(Map)`, and the ID is now **required** — missing it throws `IllegalArgumentException`. `ToolExecutionEligibilityPredicate` / `DefaultToolExecutionEligibilityPredicate` were removed in favour of `ToolCallingAdvisor.Builder.toolExecutionEligibilityChecker(Function<ChatResponse, Boolean>)` — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- **`FunctionCallback` API gone:** `FunctionCallback` → `ToolCallback`, `FunctionCallingOptions` → `ToolCallingChatOptions`. `SpringBeanToolCallbackResolver` and `toolNames()` removed — declare `ToolCallback` beans or `@Tool` objects and pass with `.tools(...)` — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- **Options API:** `ChatModel.getDefaultOptions()` → `getOptions()`; `copy()`/`fromOptions()` → `mutate()...build()`; `N(...)` → `n(...)`; `.options` segment dropped from non-chat property keys (deprecated variants still work) — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- **JSON internals:** `JsonParser` (deprecated) → `JsonHelper`; `ModelOptionsUtils.JSON_MAPPER` → `JacksonUtils.getDefaultJsonMapper()`; `McpJsonParser` removed — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- **Removed modules:** `spring-ai-spring-cloud-bindings`; Azure Cosmos DB modules (now external); `spring-ai-hanadb-store`; `spring-ai-azure-openai` → use `spring-ai-openai` (drop the `Azure` prefix); `spring-ai-openai-sdk` → use `spring-ai-openai` (drop the `Sdk` suffix); `spring-ai-oci-genai` → moved to `oracle/spring-cloud-oracle` — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- **MCP package renames:** `org.springaicommunity.mcp.{annotation,method,provider}.*` → `org.springframework.ai.mcp.annotation{,.method,.provider}.*`; MCP transport Maven group `io.modelcontextprotocol.sdk` → `org.springframework.ai` — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- **An OpenRewrite recipe exists:** `org.springframework.ai.migration.M3MigrateMcpAnnotations` and `M3MigrateMcpSpringTransports` in `migrate-to-2-0-0-M3.yaml` — [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- **2.0.1-specific items:** deprecated Mistral chat-model constants retired; Redis chat-memory module renamed to `spring-ai-autoconfigure-model-chat-memory-repository-redis`; Couchbase vector store now uses the Spring Boot-managed client; OpenAI strict tool calling defaults to `false`; new tool-call limit configured via `ToolCallingAdvisor`, raising `ToolCallLimitExceededException` — [Spring AI 2.0.1 Available Now](https://spring.io/blog/2026/08/21/spring-ai-2-0-1-available-now)
- Release timeline for dating sources: 2.0.0-M3 (2026-03-17), 2.0.0-M4 (2026-03-26), 2.0.0 GA (2026-06-12), 2.0.1 (2026-08-21) — [M3](https://spring.io/blog/2026/03/17/spring-ai-2-0-0-M3-and-1-1-3-and-1-0-4-available/); [M4](https://spring.io/blog/2026/03/26/spring-ai-2-0-0-M4-and-1-1-4-and-1-0-5-available/); [GA](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/); [2.0.1](https://spring.io/blog/2026/08/21/spring-ai-2-0-1-available-now)

### Inferences

- **The RAG interfaces themselves appear unchanged across 1.x → 2.0** — the upgrade notes carry no RAG section, and the only RAG-adjacent entries are the vector-store-advisor module rename and the chat-memory changes. So 1.x tutorials about `QueryTransformer`/`DocumentRetriever` shapes are probably still *structurally* accurate. What breaks is the surrounding wiring: imports, advisor registration, the memory advisor, and the converter's schema semantics.
- **Dating heuristic for the series:** anything published before ~June 2026 is 1.x unless it says otherwise, and the January 2025 agentic-patterns post — still the canonical routing reference — is firmly a 1.x-era artifact even though the docs page it became is maintained per-version.
- The biggest silent hazard for a routing post specifically is the **auto-registered `ToolCallingAdvisor`**: a tool-based router copied from a 1.x post will double-register and misbehave without any compile error.

### Gaps

- The upgrade-notes page is ~174k characters and I read the first 100k. The final ~74k were **not** read, and may contain RAG, provider-native-structured-output, or validation migration sections that would change the "no RAG section" inference above. **This is the single most valuable remaining fetch** (same URL, `offset: 100000`).
- Whether Spring AI 2.0.1 is formally supported on **Spring Boot 4.1.1 and Java 25** was not verified from a compatibility matrix; the project reports running it, but I found no primary statement of supported baselines.

---

## Cross-cutting summary for the series' decision

### Shipped in 2.0.1 and genuinely useful for intent routing
- `DocumentRetriever` (`Query → List<Document>`) as a clean plug for a hand-written router — `org.springframework.ai.rag.retrieval.search`
- `Query.context()` as the channel for passing a classification verdict down the pipeline
- `VectorStoreDocumentRetriever.filterExpression(Supplier<...>)` + the `FILTER_EXPRESSION` context key, for per-request conditional filtering
- `DocumentPostProcessor` for the existing cross-encoder
- `.entity(X.class, spec -> spec.useProviderStructuredOutput().validateSchema())` + `StructuredOutputValidationAdvisor` (3 attempts default) for a schema-reliable LLM classifier
- `OllamaChatOptions`: `seed`, `topK`, `topP`, `numCtx`, `keepAlive`, `format`/`outputSchema`, and `ThinkOption` / `.disableThinking()`
- `ToolSearchToolCallingAdvisor` with `tool-index-type: vector` — a shipped semantic router, but over *tools*, not retrievers

### Experimental / unresolved in 2.0.1
- Modular RAG's experimental label: present in some renderings of the RAG page, absent from the current 2.0.2 one; status for 2.0.1 specifically **unconfirmed**
- Provider-native structured output on Ollama: shipped but documented as model-specific, off by default, with partial schema support and a named failure mode on reasoning models

### Not present — must be hand-written
- Any router abstraction, at all, anywhere in the tree
- Query classification of any kind
- Conditional retrieval / branching in the advisor pipeline
- Multi-retriever selection (only `DocumentJoiner`'s fan-out-and-merge)
- Cosine similarity or any similarity utility (`EmbeddingUtils` is array conversion only)
- A semantic router over an embedded label set
- Record/replay or test doubles for model calls
- A forced tool choice / `toolChoice` on Ollama options

### Where the abstraction hides something the project needs
- **Per-token logprobs on Ollama.** `OllamaChatOptions` exposes `logitsAll` — whose own javadoc says it is the prerequisite for returning logprobs — while `OllamaApi` has no logprobs response field at all. OpenAI's options mention `logprobs` 13 times; Ollama's, zero. Confidence-thresholded or margin-based routing is therefore **unreachable through `ChatClient`** and requires bypassing Spring AI (and possibly `/api/chat` too — see §4 Gaps).
- **`useProviderStructuredOutput()` fails silently** when chat options don't implement `StructuredOutputChatOptions`, degrading to prompt-plus-parse with no warning.
- **Thinking auto-enables** on qwen3/deepseek/gpt-oss when unset, which both perturbs determinism and is the documented cause of native-structured-output parse failures.
- **`validateSchema()` mutates the prompt on retry**, so a seed-pinned call is only reproducible on the first attempt.
