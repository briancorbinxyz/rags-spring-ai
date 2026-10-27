# Spring AI 2.0.1 — Retrieval & RAG Surface Defaults

**Scope / provenance note.** Almost nothing in this surface is documented as a default in the
reference docs; the reference `vectordbs` page quotes `SearchRequest`'s two constants and nothing
else. **Every other value below was read from source.** The primary source used is the official
`-sources.jar` for **exactly 2.0.1**, resolved from Maven Central into this machine's Gradle cache
(so the bytes match what the project compiles against):

- `org.springframework.ai:spring-ai-vector-store:2.0.1` — `SearchRequest`, `SimpleVectorStore`
- `org.springframework.ai:spring-ai-vector-store-advisor:2.0.1` — `QuestionAnswerAdvisor`
- `org.springframework.ai:spring-ai-client-chat:2.0.1` — `MessageChatMemoryAdvisor`, `Advisor`, `BaseAdvisor`
- `org.springframework.ai:spring-ai-model:2.0.1` — `ChatMemory`, `MessageWindowChatMemory`
- `org.springframework.ai:spring-ai-rag:2.0.1` — `RetrievalAugmentationAdvisor`, `VectorStoreDocumentRetriever`, `ContextualQueryAugmenter`, `ConcatenationDocumentJoiner`, `DocumentRetriever`, `DocumentPostProcessor`

Extracted copies for citation live at
`/private/tmp/claude-501/-Users-briancorbin-Code-rags-rags-spring-ai/4f24c815-84f0-4ab3-aafb-a3f513e16c8c/scratchpad/sai201/`
(scratch, will not persist). Project pins `springAiVersion = "2.0.1"`, Boot `4.1.1`, Java `25` —
confirmed in `/Users/briancorbin/Code/rags/rags-spring-ai/build.gradle.kts`.

Comparison against 1.x was done against the `1.0.x` branch on GitHub (`gh api .../contents/...?ref=1.0.x`),
so 1.x claims are for **1.0.x**, not 1.1.x, unless stated.

---

## `SearchRequest` defaults

### Takeaway

Default `topK` is `DEFAULT_TOP_K = 4` and default threshold is
`SIMILARITY_THRESHOLD_ACCEPT_ALL = 0.0` — those are the exact constant names, and both are
*unchanged* from 1.0.x. "Accept all" is a misnomer: because the filter is `score >= 0.0`, any
document with a **negative** cosine similarity is silently discarded, so the default threshold
is a real filter, not a no-op. Threshold semantics are explicitly client-side post-processing,
and the 0..1 contract is enforced by the builder but *not* guaranteed by every store's scores.

### Cited Findings

- Exact constants, `SearchRequest.java` (2.0.1, `spring-ai-vector-store`): `public static final double SIMILARITY_THRESHOLD_ACCEPT_ALL = 0.0;` and `public static final int DEFAULT_TOP_K = 4;` — [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/SearchRequest.java); also quoted verbatim in the reference docs — [Spring AI reference, Vector Databases](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- Field initialisers (so `SearchRequest.builder().query(q).build()` yields exactly these): `private int topK = DEFAULT_TOP_K;`, `private double similarityThreshold = SIMILARITY_THRESHOLD_ACCEPT_ALL;`, and `private String query = "";` — [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/SearchRequest.java)
- Javadoc on the constant: "A threshold value of 0.0 means any similarity is accepted or disable the similarity threshold filtering. A threshold value of 1.0 means an exact match is required." — [SearchRequest javadoc 2.0.x](https://docs.spring.io/spring-ai/docs/2.0.x/api/org/springframework/ai/vectorstore/SearchRequest.html)
- Threshold is **client-side**, per the builder javadoc: "Only documents with similarity score equal or greater than the 'threshold' will be returned. Note that this is a post-processing step performed on the client not the server side." — [SearchRequest.Builder javadoc 2.0.1](https://docs.spring.io/spring-ai/docs/2.0.1/api/org/springframework/ai/vectorstore/SearchRequest.Builder.html)
- Builder validation: `similarityThreshold(double)` asserts `threshold >= 0 && threshold <= 1` ("Similarity threshold must be in [0,1] range."); `topK(int)` asserts `topK >= 0` with the message "TopK should be positive." (note: the assert actually permits `0`, which would return nothing — the message is wrong) — [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/SearchRequest.java)
- `similarityThresholdAll()` exists purely as an alias: "Sets disables the similarity threshold by setting it to 0.0 - all results are accepted." — [SearchRequest.Builder javadoc 2.0.1](https://docs.spring.io/spring-ai/docs/2.0.1/api/org/springframework/ai/vectorstore/SearchRequest.Builder.html)
- The reference docs are internally inconsistent about the boundary: the parameter description says only documents "with a similarity **above** this value are returned", while the code and the usage example say `>=` — [Spring AI reference, Vector Databases](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- The docs do **not** state whether every store normalises raw scores into 0..1, nor where each store evaluates the threshold; the `vectordbs` page is silent on both — [Spring AI reference, Vector Databases](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- Unchanged from 1.0.x: both constants are byte-identical on the `1.0.x` branch (verified via `gh api repos/spring-projects/spring-ai/contents/spring-ai-vector-store/.../SearchRequest.java?ref=1.0.x`) — [1.0.x source](https://github.com/spring-projects/spring-ai/blob/1.0.x/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/SearchRequest.java)

### Inferences

- **The default threshold can silently discard results.** In `SimpleVectorStore` the predicate is `document.getScore() != null && document.getScore() >= request.getSimilarityThreshold()`. Cosine similarity ranges over `[-1, 1]`, but the threshold is constrained to `[0, 1]`. So with the default `0.0`, every document whose embedding is *orthogonal-or-worse* to the query (cosine `< 0`) is dropped before `limit(topK)`. For a project auditing silent inheritance: the default is "accept all non-negative", not "accept all". With most modern sentence-embedding models negative cosines are rare but not impossible, so the practical effect is usually nil — the audit-relevant point is that the floor is `0.0`, not `-1.0`, and it is not expressible as "no filter".
- **Fewer than `topK` results is a normal default outcome.** Threshold filtering is applied *before* `.limit(topK)` in `SimpleVectorStore.doSimilaritySearch`, so a `topK=4` request returns ≤ 4, never a padded 4.
- **It does differ per store, structurally.** Because the builder contract is "0..1, higher is more similar" but most real vector databases return a *distance* (and some return unbounded inner products), each adapter must map its native metric into 0..1 before comparing. That mapping is per-adapter, not in `SearchRequest`. So an identical `similarityThreshold(0.5)` is not guaranteed to mean the same cut across stores. I could not verify the specific per-store formulas (see Gaps) — but the design makes `SimpleVectorStore` (raw cosine, no transform) the *only* store in this list where the score the threshold sees is literally cosine similarity.

### Gaps

- I did not verify the per-store score transforms (pgvector, Redis, Chroma, Qdrant, Milvus, etc.) for 2.0.1 — each adapter's `doSimilaritySearch` would need reading individually, and the project only uses `SimpleVectorStore`, so this was out of budget. The claim "threshold semantics differ per implementation" is an inference from the architecture, not a verified per-store comparison.
- No primary source states whether all 2.0.1 stores clamp negative similarities to 0 or pass them through.

---

## `QuestionAnswerAdvisor` defaults

### Takeaway

`QuestionAnswerAdvisor.builder(store).build()` takes a bare `SearchRequest.builder().build()` —
so its topK is **not** its own constant, it *is* `SearchRequest.DEFAULT_TOP_K = 4` (and threshold
`0.0`) inherited wholesale. Default advisor `order` is `DEFAULT_ORDER = 0`. The retrieved-documents
key is `"qa_retrieved_documents"`. Order *does* matter against `MessageChatMemoryAdvisor`, and the
relative ordering **changed in 2.0.0**.

### Cited Findings

- Default search request is a bare `SearchRequest`, in `QuestionAnswerAdvisor.Builder`: `private SearchRequest searchRequest = SearchRequest.builder().build();` — i.e. the advisor defines **no** topK of its own; the 4 comes from `SearchRequest.DEFAULT_TOP_K`. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-vector-store-advisor/src/main/java/org/springframework/ai/chat/client/advisor/vectorstore/QuestionAnswerAdvisor.java)
- Default order: `private static final int DEFAULT_ORDER = 0;`, used as `private int order = DEFAULT_ORDER;` in the builder. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-vector-store-advisor/src/main/java/org/springframework/ai/chat/client/advisor/vectorstore/QuestionAnswerAdvisor.java)
- Metadata / context keys: `public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";` and `public static final String FILTER_EXPRESSION = "qa_filter_expression";`. The same `"qa_retrieved_documents"` string is used both as the `ChatClientRequest` context key *and* as the `ChatResponse` metadata key (`chatResponseBuilder.metadata(RETRIEVED_DOCUMENTS, ...)` in `after(...)`). [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-vector-store-advisor/src/main/java/org/springframework/ai/chat/client/advisor/vectorstore/QuestionAnswerAdvisor.java)
- Default scheduler: `BaseAdvisor.DEFAULT_SCHEDULER`, which is `Schedulers.boundedElastic()` (`BaseAdvisor.java`: `Scheduler DEFAULT_SCHEDULER = Schedulers.boundedElastic();`). [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/api/BaseAdvisor.java)
- Default prompt template is `DEFAULT_PROMPT_TEMPLATE`, a `new PromptTemplate(...)` text block defined **inline as a private static final field in `QuestionAnswerAdvisor` itself** (not in a `.st` resource, not in a shared constants class). Exact text quoted below.
- Template contract is documented: the `QuestionAnswerAdvisor` "expects a template with the following placeholders": "a `query` placeholder to receive the user question" and "a `question_answer_context` placeholder to receive the retrieved context" — [Spring AI Upgrade Notes, 1.0.0-RC1 breaking changes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- Advisor ordering constant changed in 2.0.0: "`Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER` changed from `Ordered.HIGHEST_PRECEDENCE + 1000` to `Ordered.HIGHEST_PRECEDENCE + 200`, placing memory advisors at their default order *outside* the `ToolCallingAdvisor` (`HIGHEST_PRECEDENCE + 300`)." — [Spring AI Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html); confirmed in 2.0.1 source: `int DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER = Ordered.HIGHEST_PRECEDENCE + 200;` in `Advisor.java`
- The module itself was renamed in 2.0.0: "The `spring-ai-advisors-vector-store` module has been renamed to `spring-ai-vector-store-advisor` to better align with the naming conventions of other Spring AI modules." — [Spring AI Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- Template, order and metadata key are **unchanged** from 1.0.x: the `1.0.x` branch file has the identical `DEFAULT_PROMPT_TEMPLATE` lines, `DEFAULT_ORDER: 0`, `RETRIEVED_DOCUMENTS: "qa_retrieved_documents"`, and `private SearchRequest searchRequest = SearchRequest.builder().build();` — [1.0.x source](https://github.com/spring-projects/spring-ai/blob/1.0.x/advisors/spring-ai-advisors-vector-store/src/main/java/org/springframework/ai/chat/client/advisor/vectorstore/QuestionAnswerAdvisor.java)

### The default prompt template, verbatim (2.0.1)

```java
private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
		{query}

		Context information is below, surrounded by ---------------------

		---------------------
		{question_answer_context}
		---------------------

		Given the context and provided history information and not prior knowledge,
		reply to the user comment. If the answer is not in the context, inform
		the user that you can't answer the question.
		""");
```

Defined in
`spring-ai-vector-store-advisor/.../org/springframework/ai/chat/client/advisor/vectorstore/QuestionAnswerAdvisor.java`,
as a private static field of the advisor class.

### Inferences

- **Hit@4 is the right metric and for the right reason, but attribute it to `SearchRequest`.** The 4 is not a `QuestionAnswerAdvisor` decision at all; the advisor has no topK constant. Anyone changing `SearchRequest.DEFAULT_TOP_K` (or passing their own `SearchRequest`) moves the advisor's k. Articles that describe "the advisor's default topK of 4" are describing an inherited value.
- **Order matters, and memory wins by a wide margin.** `Ordered.HIGHEST_PRECEDENCE` is `Integer.MIN_VALUE`, so `MessageChatMemoryAdvisor`'s default order is `Integer.MIN_VALUE + 200` ≈ `-2147483448`, versus `QuestionAnswerAdvisor`'s `0`. Lower order = earlier `before()`. So with both at defaults: memory's `before()` runs first (injecting history messages and recording the user message), then QA's `before()` runs and augments the user message with retrieved context. Consequence worth auditing: **the text the memory advisor persists for this turn is captured before QA augments it** in `before()`, but `MessageChatMemoryAdvisor.before` calls `chatMemory.add(conversationId, userMessage)` on the message from *its own* mutated request — so the stored user message is the raw question, not the context-stuffed prompt. That is the desirable behaviour, and it is a consequence of the default ordering, not of anything the project configured.
- **The prompt template's `{query}` placement means the question appears twice** in the final user message when combined with `augmentUserMessage` — the template renders `{query}` at the top and the advisor passes the result to `prompt().augmentUserMessage(augmentedUserText)`. Worth checking in the project's actual rendered prompts.
- **A `query` set on a hand-built `SearchRequest` is ignored.** `before()` does `SearchRequest.from(this.searchRequest).query(<user message text>)`, unconditionally overwriting the query. Only `topK`, `similarityThreshold` and `filterExpression` survive from a supplied `SearchRequest`.
- **The default template is not validated.** Unlike `ContextualQueryAugmenter` (which calls `PromptAssert.templateHasRequiredPlaceholders(template, "query", "context")`), the 2.0.1 `QuestionAnswerAdvisor` constructor performs no placeholder assertion — a custom template missing `{question_answer_context}` fails at render time, not at build time. Read from source; not documented.

### Gaps

- Nothing in the reference docs states `DEFAULT_ORDER = 0` for `QuestionAnswerAdvisor`; this is source-only.

---

## `SimpleVectorStore` and `EmbeddingMath`

### Takeaway

Cosine similarity over **un-normalised** stored vectors, computed in the nested
`SimpleVectorStore.EmbeddingMath` class. Results are sorted by score descending and then
truncated to `topK`. **Ties are resolved non-deterministically**, because the stream source is a
`ConcurrentHashMap.values()` whose encounter order is unspecified.

### Cited Findings

- Class javadoc (2.0.1): "Similarity search is performed using cosine similarity over all stored vectors." and "**NOTE**: This implementation is not designed for production use and should only be used for testing or demonstration purposes." — [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/SimpleVectorStore.java)
- Observability declares the metric: `createObservationContextBuilder` sets `.similarityMetric(VectorStoreSimilarityMetric.COSINE.value())` and `.collectionName("in-memory-map")`, with provider `VectorStoreProvider.SIMPLE`. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/SimpleVectorStore.java)
- Storage is `protected Map<String, SimpleVectorStoreContent> store = new ConcurrentHashMap<>();`, keyed by document id. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/SimpleVectorStore.java)
- The whole search pipeline, verbatim (2.0.1):
  ```java
  @Override
  public List<Document> doSimilaritySearch(SearchRequest request) {
      float[] userQueryEmbedding = getUserQueryEmbedding(request.getQuery());
      return this.store.values()
          .stream()
          .filter(document -> doFilterPredicate(request.getFilterExpression()).test(document))
          .map(content -> content
              .toDocument(EmbeddingMath.cosineSimilarity(userQueryEmbedding, content.getEmbedding())))
          .filter(document -> document.getScore() != null && document.getScore() >= request.getSimilarityThreshold())
          .sorted(Comparator.comparing(Document::getScore).reversed())
          .limit(request.getTopK())
          .toList();
  }
  ```
- `EmbeddingMath` is a `public static final` nested utility class with a private throwing constructor ("This is a utility class and cannot be instantiated"). Its three methods, verbatim:
  ```java
  public static double cosineSimilarity(float[] vectorX, float[] vectorY) {
      // null checks -> RuntimeException("Vectors must not be null")
      // length check -> IllegalArgumentException("Vectors lengths must be equal")
      float dotProduct = dotProduct(vectorX, vectorY);
      float normX = norm(vectorX);
      float normY = norm(vectorY);
      if (normX == 0 || normY == 0) {
          throw new IllegalArgumentException("Vectors cannot have zero norm");
      }
      return dotProduct / (Math.sqrt(normX) * Math.sqrt(normY));
  }
  public static float dotProduct(float[] vectorX, float[] vectorY) { /* sum of products, float accumulator */ }
  public static float norm(float[] vector) { return dotProduct(vector, vector); }
  ```
- No normalisation at write time: `doAdd` stores `this.embeddingModel.embed(document)` raw into `SimpleVectorStoreContent`, with no unit-length step. Likewise `getUserQueryEmbedding` returns `this.embeddingModel.embed(query)` raw. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/SimpleVectorStore.java)
- Metadata filtering is evaluated in-process by `SimpleVectorStoreFilterExpressionEvaluator`; a `null` filter expression yields `document -> true`. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/SimpleVectorStore.java)
- Essentially unchanged from 1.0.x: the `1.0.x` `doSimilaritySearch` is the same pipeline in the same order, and `cosineSimilarity`/`norm` are character-identical. The only 2.0.1 differences are cosmetic: the filter predicate is inlined rather than hoisted to a local, and the score filter gained a `document.getScore() != null &&` null guard. — [1.0.x source](https://github.com/spring-projects/spring-ai/blob/1.0.x/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/SimpleVectorStore.java)

### Inferences

- **`norm()` is misnamed but the maths is correct.** `norm(v)` returns `dotProduct(v, v)` — the *squared* L2 norm, not the norm. `cosineSimilarity` then divides by `Math.sqrt(normX) * Math.sqrt(normY)`, which recovers `|x| * |y|`. So the returned value is genuine cosine similarity in `[-1, 1]`. Anyone calling `EmbeddingMath.norm(v)` directly and expecting a norm gets its square. This is a trap for a project that reaches into `EmbeddingMath` itself.
- **Vectors are not normalised, so this is true cosine, not dot product.** The division by magnitudes happens per query at search time, on every stored vector, every call. Cost is O(n·d) per search with no index — fine for article-scale corpora, and the reason the javadoc says not for production.
- **Float accumulation.** `dotProduct` accumulates into a `float`, then the result is widened to `double` only for the final division. For high-dimensional embeddings (768–3072 dims) that is measurably lossier than a `double` accumulator, and can perturb the ordering of near-tied documents. Source-only observation; not documented.
- **Tie-breaking is non-deterministic — the most audit-relevant finding here.** `Stream.sorted` is stable, so ties preserve *encounter order* — but the encounter order is `ConcurrentHashMap.values().stream()`, which is hash-bucket order and depends on the id hashes and the table's resize history, not insertion order. Two documents with exactly equal scores can therefore swap places between runs or after re-ingesting the corpus in a different order. For a Hit@4 metric this matters at the k boundary: an exact tie for 4th/5th place is not reproducibly resolved. (Exact float ties are uncommon with real embeddings but arise immediately with duplicated or near-duplicate chunks.)
- There is **no documented default on ordering or ties** anywhere — the `vectordbs` reference page does not describe `SimpleVectorStore`'s similarity function at all, let alone its sort. Entirely source-derived.

---

## `MessageChatMemoryAdvisor` defaults

### Takeaway

The advisor itself holds **no window size** — it has no memory default at all beyond order and
scheduler. The window lives in whichever `ChatMemory` you hand it; the usual
`MessageWindowChatMemory` defaults to `DEFAULT_MAX_MESSAGES = 20`. On overflow, oldest non-system
messages are evicted, system messages are always preserved, and in **2.0.1** the cut is snapped
forward to a USER message so the kept window starts on a complete turn — which means the window
can hold *fewer* than 20. 2.0.0 also made `conversationId` mandatory.

### Cited Findings

- `MessageChatMemoryAdvisor` has exactly three fields: `chatMemory`, `order`, `scheduler`. No size, no window, no truncation. Builder defaults: `private int order = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;` and `private Scheduler scheduler = BaseAdvisor.DEFAULT_SCHEDULER;`. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/MessageChatMemoryAdvisor.java)
- `Advisor.java` (2.0.1): `int DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER = Ordered.HIGHEST_PRECEDENCE + 200;` — i.e. `Integer.MIN_VALUE + 200`. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/api/Advisor.java)
- `MessageWindowChatMemory` (2.0.1, `spring-ai-model`): `private static final int DEFAULT_MAX_MESSAGES = 20;`, with `private int maxMessages = DEFAULT_MAX_MESSAGES;` in its builder and a constructor assert `maxMessages > 0`. Default repository is `new InMemoryChatMemoryRepository()`. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-model/src/main/java/org/springframework/ai/chat/memory/MessageWindowChatMemory.java)
- Class javadoc on overflow: "maintains a message window of a specified size, ensuring that the total number of messages does not exceed the specified limit. When the number of messages exceeds the maximum size, older messages are evicted." and: "Messages of type `SystemMessage` are treated specially: if a new `SystemMessage` is added, all previous `SystemMessage` instances are removed from the memory. Also, if the total number of messages exceeds the limit, the `SystemMessage` messages are preserved while evicting other types of messages." [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-model/src/main/java/org/springframework/ai/chat/memory/MessageWindowChatMemory.java)
- 2.0.1 eviction, verbatim comments from source: "Raw cut: the number of non-system messages that must be removed to fit within maxMessages." then "**Snap the cut forward to the nearest USER message so the kept window always starts at a complete turn. This prevents keeping an assistant reply or tool result without the user message that originated its turn.**" Implemented as `int cutIndex = processedMessages.size() - this.maxMessages;` followed by a `while` loop advancing `cutIndex` while the message at `nonSystemIndices.get(cutIndex)` is not `MessageType.USER`.
- 1.0.x eviction had **no turn-snapping** — it was a raw count cut: `int messagesToRemove = processedMessages.size() - this.maxMessages;` then a single pass dropping the first `messagesToRemove` non-system messages. Verified via `gh api repos/spring-projects/spring-ai/contents/spring-ai-model/.../MessageWindowChatMemory.java?ref=1.0.x`. — [1.0.x source](https://github.com/spring-projects/spring-ai/blob/1.0.x/spring-ai-model/src/main/java/org/springframework/ai/chat/memory/MessageWindowChatMemory.java)
- `DEFAULT_MAX_MESSAGES = 20` is the same in 1.0.x and 2.0.1 (verified on both).
- 2.0.0 breaking change — conversation id now mandatory: "The conversation ID is no longer optional for the built-in memory advisors (`MessageChatMemoryAdvisor` and `VectorStoreChatMemoryAdvisor`). Every call through these advisors must supply `ChatMemory.CONVERSATION_ID` via the advisor context. If the value is absent or `null`, the advisor throws an `IllegalArgumentException` immediately." — [Spring AI Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- 2.0.0: "The constant `ChatMemory.DEFAULT_CONVERSATION_ID` (value `\"default\"`) has been removed from the `ChatMemory` interface." and "The `.conversationId(String)` builder method has been removed from `MessageChatMemoryAdvisor.Builder` and `VectorStoreChatMemoryAdvisor.Builder`. Setting a default conversation ID at construction time is no longer supported." — [Spring AI Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html). Confirmed in 2.0.1 source: `ChatMemory` declares only `String CONVERSATION_ID = "chat_memory_conversation_id";`, and `BaseChatMemoryAdvisor.getConversationId(Map)` does `Assert.notNull(context.get(ChatMemory.CONVERSATION_ID), "conversationId cannot be null");`
- 2.0.0: "`BaseChatMemoryAdvisor.getConversationId(Map, String)` has been replaced by `getConversationId(Map)`" and "`PromptChatMemoryAdvisor` has been removed. Use `MessageChatMemoryAdvisor` as a replacement." — [Spring AI Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)

### Inferences

- **"Default memory window" is a category error for the advisor.** `MessageChatMemoryAdvisor.builder(chatMemory).build()` requires a `ChatMemory` argument, so there is no advisor-level window to inherit silently. The silently-inherited 20 comes from `MessageWindowChatMemory.builder().build()` (or from Boot auto-configuration supplying that bean). The project should audit which `ChatMemory` it constructs.
- **20 messages is ~10 turns, and it counts messages not tokens.** Overflow is message-count-driven and completely blind to token length, so a long RAG-augmented user message counts as 1, exactly like "ok". In a RAG app where every user message has been augmented with retrieved context, the effective context growth per remembered message is large and the window gives no protection against it.
- **2.0.1's turn-snapping means the window is a soft maximum.** The `while` loop only ever advances `cutIndex`, so it evicts *at least* as many messages as the raw cut — never fewer. With a trailing assistant message plus a tool-call sequence, a `maxMessages = 20` memory can settle at 17 or 18 messages. Any article that asserted "the last 20 messages are kept" was accurate for 1.0.x and is now an upper bound.
- **This is the most likely silent breakage across the three articles.** `DEFAULT_CONVERSATION_ID` removal is a compile error, so it would have surfaced. But the ordering constant moving from `HIGHEST_PRECEDENCE + 1000` to `+ 200` is a *silent behavioural* change: nothing fails, the advisor just sits at a different point in the chain relative to `ToolCallingAdvisor` (`+ 300`). In 1.x, memory at `+1000` was *inside* the tool-calling advisor; in 2.0.x, at `+200`, it is *outside*. If any article demonstrated memory interacting with tool calls, its described behaviour no longer holds.

### Gaps

- I did not confirm whether `spring-ai-autoconfigure-model-chat-memory:2.0.1` registers a `MessageWindowChatMemory` bean by default (and thus whether the 20 is inherited without the project naming it). The grep attempt failed on a shell glob error and I did not have budget to retry. This is worth one follow-up check for the audit.

---

## The `org.springframework.ai.rag` package: omitted stages and post-processor ordering

### Takeaway

`RetrievalAugmentationAdvisor` requires only a `documentRetriever`; everything else has a default.
Omitted stages are either no-ops (`queryTransformers` → empty list, `queryExpander` → `null`
meaning single query, `documentPostProcessors` → empty list) or get a concrete default
(`documentJoiner` → `ConcatenationDocumentJoiner`, `queryAugmenter` → `ContextualQueryAugmenter`
with `allowEmptyContext = false`). **`DocumentPostProcessor` ordering is not specified by the
interface** — there is no `Ordered`, no `@Order`, no priority; the advisor simply iterates the
list in the order you supplied it.

### Cited Findings

- `RetrievalAugmentationAdvisor` constructor defaults, verbatim (2.0.1):
  ```java
  this.queryTransformers = queryTransformers != null ? queryTransformers : List.of();
  this.queryExpander = queryExpander;                       // stays null
  this.documentJoiner = documentJoiner != null ? documentJoiner : new ConcatenationDocumentJoiner();
  this.documentPostProcessors = documentPostProcessors != null ? documentPostProcessors : List.of();
  this.queryAugmenter = queryAugmenter != null ? queryAugmenter : ContextualQueryAugmenter.builder().build();
  this.taskExecutor = taskExecutor != null ? taskExecutor : buildDefaultTaskExecutor();
  this.scheduler = scheduler != null ? scheduler : BaseAdvisor.DEFAULT_SCHEDULER;
  this.order = order != null ? order : 0;
  ```
  [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-rag/src/main/java/org/springframework/ai/rag/advisor/RetrievalAugmentationAdvisor.java)
- Only `documentRetriever` is mandatory: `Assert.state(this.documentRetriever != null, "documentRetriever cannot be null");` in `Builder.build()`. Default advisor order is `0` (same as `QuestionAnswerAdvisor`).
- Null `queryExpander` means exactly one query: `List<Query> expandedQueries = this.queryExpander != null ? this.queryExpander.expand(transformedQuery) : List.of(transformedQuery);`
- Default task executor: `ThreadPoolTaskExecutor` with `threadNamePrefix "ai-advisor-"`, `corePoolSize 4`, `maxPoolSize 16`, and a `ContextPropagatingTaskDecorator`. (Note: this is created per-advisor and never shut down.)
- Context / metadata key: `public static final String DOCUMENT_CONTEXT = "rag_document_context";` — distinct from `QuestionAnswerAdvisor`'s `"qa_retrieved_documents"`.
- Post-processors are applied as a plain ordered fold, with no sorting: `for (var documentPostProcessor : this.documentPostProcessors) { documents = documentPostProcessor.process(originalQuery, documents); }`
- `DocumentPostProcessor` is `interface DocumentPostProcessor extends BiFunction<Query, List<Document>, List<Document>>` with a single `process(Query, List<Document>)` and a default `apply` delegating to it. **It does not extend `Ordered`** and declares no priority member. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-rag/src/main/java/org/springframework/ai/rag/postretrieval/document/DocumentPostProcessor.java)
- `DocumentRetriever` is `interface DocumentRetriever extends Function<Query, List<Document>>` with `List<Document> retrieve(Query)` and a default `apply` delegating to it. No defaults, no ordering. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-rag/src/main/java/org/springframework/ai/rag/retrieval/search/DocumentRetriever.java)
- `VectorStoreDocumentRetriever` (the built-in retriever) explicitly inherits `SearchRequest`'s constants:
  ```java
  this.similarityThreshold = similarityThreshold != null ? similarityThreshold
          : SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL;
  this.topK = topK != null ? topK : SearchRequest.DEFAULT_TOP_K;
  this.filterExpression = filterExpression != null ? filterExpression : () -> null;
  ```
  Note its asserts are *stricter* than `SearchRequest`'s: `topK == null || topK > 0` (rejects 0) and `similarityThreshold == null || similarityThreshold >= 0.0` (**no upper bound of 1.0**, unlike `SearchRequest.Builder`). Its context key is `public static final String FILTER_EXPRESSION = "vector_store_filter_expression";`. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-rag/src/main/java/org/springframework/ai/rag/retrieval/search/VectorStoreDocumentRetriever.java)
- `ConcatenationDocumentJoiner` (the default joiner) **de-duplicates by document id, keeping the first occurrence, then re-sorts by score descending**, treating a null score as `0.0`: `.collect(Collectors.toMap(Document::getId, Function.identity(), (existing, duplicate) -> existing))` then `.sorted(Comparator.comparingDouble((Document doc) -> doc.getScore() != null ? doc.getScore() : 0.0).reversed())`. [GitHub source](https://github.com/spring-projects/spring-ai/blob/2.0.x/spring-ai-rag/src/main/java/org/springframework/ai/rag/retrieval/join/ConcatenationDocumentJoiner.java)
- `ContextualQueryAugmenter` defaults: `DEFAULT_ALLOW_EMPTY_CONTEXT = false`; default document formatter joins `Document::getText` with `System.lineSeparator()`; and it *validates* its template via `PromptAssert.templateHasRequiredPlaceholders(this.promptTemplate, "query", "context")`. Its default template, verbatim:
  ```
  Context information is below.

  ---------------------
  {context}
  ---------------------

  Given the context information and no prior knowledge, answer the query.

  Follow these rules:

  1. If the answer is not in the context, just say that you don't know.
  2. Avoid statements like "Based on the context..." or "The provided information...".

  Query: {query}

  Answer:
  ```
  and its default empty-context template, verbatim:
  ```
  The user query is outside your knowledge base.
  Politely inform the user that you can't answer it.
  ```
- Module description: `spring-ai-rag` is a "Comprehensive framework for Retrieval Augmented Generation: Modular architecture for RAG pipelines; `RetrievalAugmentationAdvisor` as main entry point; Functional programming principles with composable components" — [Spring AI Upgrade Notes, Module Structure](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)

### Inferences

- **`DocumentPostProcessor` ordering is caller-defined and position-dependent, with no framework guarantee.** Because the advisor folds the list, each processor sees the *output* of the previous one, and the list order is whatever was passed to `documentPostProcessors(...)`. A re-ranker placed after a truncating processor can only re-rank the survivors. Since the interface offers no `Ordered`, Spring bean-ordering annotations on a `DocumentPostProcessor` implementation have no effect here — the advisor never consults them. Worth stating plainly in the audit: a project implementing `DocumentPostProcessor` controls ordering only by list position.
- **`allowEmptyContext = false` is the most consequential undocumented RAG default.** With the stock augmenter, zero retrieved documents does not mean "answer from the model's own knowledge" — it replaces the user's query entirely with "The user query is outside your knowledge base. Politely inform the user that you can't answer it." The user's actual question never reaches the model. Combined with the `>= 0.0` threshold floor and `topK = 4`, an empty result set produces a canned refusal. Any evaluation harness measuring answer quality must distinguish this from a model failure.
- **`QuestionAnswerAdvisor` and `RetrievalAugmentationAdvisor` behave differently on empty retrieval.** `QuestionAnswerAdvisor` renders its template with an empty `{question_answer_context}` and still passes the real question through; `RetrievalAugmentationAdvisor` (default augmenter) substitutes the refusal. Same framework, same default-everything posture, opposite behaviour. If the project's articles switched between the two, this is a behavioural discontinuity that defaults alone caused.
- **The default joiner silently re-sorts and de-duplicates even in the single-retriever, single-query case.** With no expander and one retriever, the retriever's own ordering is replaced by a score-descending sort over a `HashMap`-backed collection — reintroducing the same unspecified-encounter-order tie problem as `SimpleVectorStore`, and clobbering any intentional non-score ordering a custom `DocumentRetriever` produced. A project that implements `DocumentRetriever` and returns documents in a deliberate order should know that order does not survive the default joiner. Also: documents with `null` scores are all mapped to `0.0` and sink to the bottom together.
- **Two different metadata keys** (`"qa_retrieved_documents"` vs `"rag_document_context"`) means tooling that reads retrieved documents off the `ChatResponse` must know which advisor produced it.

### Gaps

- I did not verify whether Boot auto-configuration in 2.0.1 registers any `DocumentPostProcessor` or `QueryTransformer` beans that would be picked up implicitly — the advisor's builder takes explicit lists, so I believe not, but I did not confirm there is no auto-configuration collecting them.

---

## Summary: what changed between 1.x and 2.0.x in this surface

### Takeaway

The retrieval core is stable — `SearchRequest`'s two constants, `SimpleVectorStore`'s cosine
pipeline and `QuestionAnswerAdvisor`'s template/order/metadata-key are all unchanged from 1.0.x
to 2.0.1. The breakage is concentrated in **chat memory** and in **module coordinates**.

### Cited Findings

| Item | 1.0.x | 2.0.1 | Source |
|---|---|---|---|
| `SearchRequest.DEFAULT_TOP_K` | 4 | 4 | verified both branches |
| `SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL` | 0.0 | 0.0 | verified both branches |
| `QuestionAnswerAdvisor` default template | identical text | identical text | verified both branches |
| `QuestionAnswerAdvisor.DEFAULT_ORDER` | 0 | 0 | verified both branches |
| `QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS` | `"qa_retrieved_documents"` | `"qa_retrieved_documents"` | verified both branches |
| `SimpleVectorStore` search pipeline | cosine → threshold → sort desc → limit | same (+ null-score guard) | verified both branches |
| `EmbeddingMath.cosineSimilarity` / `norm` | identical | identical | verified both branches |
| `MessageWindowChatMemory.DEFAULT_MAX_MESSAGES` | 20 | 20 | verified both branches |
| `MessageWindowChatMemory` eviction | raw count cut | **count cut snapped forward to next USER message** | verified both branches |
| `Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER` | `HIGHEST_PRECEDENCE + 1000` | `HIGHEST_PRECEDENCE + 200` | [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html) + 2.0.1 source |
| `ChatMemory.DEFAULT_CONVERSATION_ID` | `"default"` | **removed** | [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html) + 2.0.1 source |
| `MessageChatMemoryAdvisor.Builder.conversationId(String)` | present | **removed** | [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html) |
| `BaseChatMemoryAdvisor.getConversationId` | `(Map, String)` | `(Map)`, throws if absent | [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html) + 2.0.1 source |
| `PromptChatMemoryAdvisor` | present | **removed** | [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html) |
| Vector-store advisor module | `spring-ai-advisors-vector-store` | `spring-ai-vector-store-advisor` | [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html) |
| Jackson | Jackson 2 | **Jackson 3** (`tools.jackson.*` imports in `SimpleVectorStore`) | 2.0.1 source; [2.0.0-M3 release post](https://spring.io/blog/2026/03/17/spring-ai-2-0-0-M3-and-1-1-3-and-1-0-4-available/) |

- Earlier, pre-1.0 renames that may affect very old articles: `VectorStoreChatMemoryAdvisor`'s `CHAT_MEMORY_RETRIEVE_SIZE_KEY` → `TOP_K`, and `DEFAULT_CHAT_MEMORY_RESPONSE_SIZE` (100) → `DEFAULT_TOP_K` with new default **20**, both in 1.0.0-RC1 — [Spring AI Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- The `QuestionAnswerAdvisor` "self-contained templates" change (template must carry `{query}` and `{question_answer_context}` itself) also landed in **1.0.0-RC1**, not 2.0 — [Spring AI Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)
- 2.0.x removed SAP HANA DB and Infinispan vector store integrations (not used by this project) — [Spring AI 1.0.7 / 1.1.6 / 2.0.0-M6 release post](https://spring.io/blog/2026/05/08/spring-ai-1-0-7-1-1-6-2-0-0-M6-available-now/)
- 2.0.1 renamed the Redis chat memory auto-configuration module to `spring-ai-autoconfigure-model-chat-memory-repository-redis` — [v2.0.1 release notes](https://newreleases.io/project/github/spring-projects/spring-ai/release/v2.0.1)

### Inferences

- **For the three published articles, two silent changes matter and one loud one does not.** The `DEFAULT_CONVERSATION_ID` removal breaks compilation, so it cannot have gone unnoticed. The two that change behaviour without complaint are (a) the memory advisor's default order moving from `+1000` to `+200`, which relocates memory relative to `ToolCallingAdvisor` at `+300`, and (b) `MessageWindowChatMemory` now evicting to a turn boundary, so the window holds ≤ 20 rather than exactly 20.
- **The retrieval claims in the articles are safe to carry forward.** topK 4, threshold 0.0, cosine over un-normalised vectors, score-descending truncation, and the QA prompt template are all identical between 1.0.x and 2.0.1 — nothing in that chain needs re-derivation for 2.0.1.

### Gaps

- The reference docs' 2.0.0 upgrade section is long and I read it in two windows; I found no `QuestionAnswerAdvisor`, `SearchRequest`, `SimpleVectorStore`, `DocumentRetriever` or `DocumentPostProcessor` entries in either window, and the direct source comparison corroborates "no change". But I cannot claim to have read 100% of that page, so a minor documented note may exist that I did not see.
- I compared against the `1.0.x` branch only. If an article was written against **1.1.x**, deltas specific to 1.1 (between 1.0.x and 2.0.x) were not checked.
- `github.com/spring-projects/spring-ai/wiki` has no release-notes page (the wiki home is empty, last edited Jan 26 2026), so there is no wiki-based changelog to cross-check against.
