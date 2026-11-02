package xyz.briancorbin.rags;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import xyz.briancorbin.rags.answer.Archive;
import xyz.briancorbin.rags.answer.CitationChecker;
import xyz.briancorbin.rags.answer.Passages;
import xyz.briancorbin.rags.answer.QuoteResolver;
import xyz.briancorbin.rags.search.PrefixedEmbeddingModel;
import xyz.briancorbin.rags.spectastic.SpecCorpus;

/// The archive's parts: the embedding model, the vector store, the corpus, and the two commentators
/// with their advisors.
@Configuration
class ArchiveConfiguration {

  /// Spring AI leaves an option out of the request when it is null, and Ollama then fills in its
  /// own: a random seed, `top_p` 0.9, `top_k` 40, and a five-minute `keep_alive` after which the
  /// next call reloads the model and decides its load-time options again. So each is set here. Only
  /// `OllamaChatOptions` can carry a seed; the portable `ChatOptions` has none. `num_ctx` is set
  /// once, by `spring.ai.ollama.chat.num-ctx`, and reaches every request already.
  static final int SEED = 42;

  static final double TOP_P = 0.9;
  static final int TOP_K = 40;

  /// Long enough that a golden run never reloads the model between calls.
  static final String KEEP_ALIVE = "30m";

  /// The options every commentator runs with: its temperature, and the rest pinned.
  static OllamaChatOptions.Builder pinned(double temperature) {
    return OllamaChatOptions.builder()
        .temperature(temperature)
        .seed(SEED)
        .topP(TOP_P)
        .topK(TOP_K)
        .keepAlive(KEEP_ALIVE);
  }

  /// Spring AI's Ollama embedding model with nomic's task prefixes added. Primary, so anything that
  /// asks for an `EmbeddingModel` gets the prefixed one.
  @Bean
  @Primary
  PrefixedEmbeddingModel nomicEmbeddingModel(OllamaEmbeddingModel ollama) {
    return new PrefixedEmbeddingModel(ollama);
  }

  /// In memory, exact search over every vector, saved to and loaded from a JSON file.
  @Bean
  SimpleVectorStore archiveStore(PrefixedEmbeddingModel embeddingModel) {
    return SimpleVectorStore.builder(embeddingModel).build();
  }

  @Bean
  SpecCorpus specCorpus(
      SimpleVectorStore store,
      PrefixedEmbeddingModel embeddingModel,
      @Value("${spectastic.path}") Path checkout,
      @Value("${spectastic.commit:}") String commit,
      @Value("${spring.ai.ollama.embedding.model}") String model) {
    return new SpecCorpus(
        new SpecCorpus.Checkout(checkout, commit, Path.of("build", "archive")),
        new SpecCorpus.Embedding(embeddingModel, model),
        store);
  }

  /// What Beta searches through: the element search of Episode 4 when it is wired
  /// (`archive.search=elements`, the default), and Episode 3's chunk store otherwise. The chat
  /// clients take this rather than a store, so neither of them knows which search it got.
  @Bean
  Advisor retrievalAdvisor(
      Optional<RetrievalAugmentationAdvisor> elements, SimpleVectorStore store) {
    return elements
        .<Advisor>map(advisor -> advisor)
        .orElseGet(() -> QuestionAnswerAdvisor.builder(store).build());
  }

  /// The same search again, for the one path that has to run it directly: a chat call that failed
  /// before its advisor could report what it found. It has to match the advisor, or a failure would
  /// be scored against a different search than the one that was used.
  @Bean
  Passages passages(Optional<DocumentRetriever> elements, SimpleVectorStore store) {
    return elements
        .<Passages>map(retriever -> question -> retriever.retrieve(new Query(question)))
        .orElse(
            question -> store.similaritySearch(SearchRequest.builder().query(question).build()));
  }

  /// Quoter finds things. Its retrieval advisor searches on every call and pastes the passages it
  /// finds into the prompt. The memory advisor runs first, so it stores the question as asked, not
  /// with four passages pasted under it.
  @Bean
  ChatClient quoterChatClient(
      ChatClient.Builder builder,
      ChatMemory chatMemory,
      @Qualifier("retrievalAdvisor") Advisor retrieval,
      @Value("classpath:persona/quoter.md") Resource persona) {
    return builder
        .defaultSystem(persona)
        // Copying wants a cool head; Quoter's personality lives in the aside.
        .defaultOptions(pinned(0.3))
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), retrieval)
        .build();
  }

  /// Answerer answers. No retriever: Answerer sees only the quotations the program traced.
  @Bean
  ChatClient answererChatClient(
      ChatClient.Builder builder,
      ChatMemory chatMemory,
      @Value("classpath:persona/answerer.md") Resource persona) {
    return builder
        .defaultSystem(persona)
        .defaultOptions(pinned(0.4))
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build();
  }

  /// The answerer alone, for `archive.mode=direct`: it searches and answers in one call.
  @Bean
  ChatClient directAnswererChatClient(
      ChatClient.Builder builder,
      ChatMemory chatMemory,
      @Qualifier("retrievalAdvisor") Advisor retrieval,
      @Value("classpath:persona/answerer-direct.md") Resource persona) {
    return builder
        .defaultSystem(persona)
        .defaultOptions(pinned(0.3))
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), retrieval)
        .build();
  }

  @Bean
  QuoteResolver quoteResolver() {
    return new QuoteResolver();
  }

  @Bean
  CitationChecker citationChecker() {
    return new CitationChecker();
  }

  /// `archive.mode=quoted` (the default): the quoter finds and quotes, the answerer answers only
  /// from what was traced. `direct`: the answerer alone.
  @Bean
  Archive archive(
      @Qualifier("quoterChatClient") ChatClient quoter,
      @Qualifier("answererChatClient") ChatClient answerer,
      @Qualifier("directAnswererChatClient") ChatClient directAnswerer,
      Passages passages,
      QuoteResolver resolver,
      CitationChecker citations,
      @Value("${archive.mode:quoted}") String mode) {
    return new Archive(
        new Archive.Roles(
            Archive.Mode.valueOf(mode.strip().toUpperCase(Locale.ROOT)),
            quoter,
            answerer,
            directAnswerer),
        new Archive.Checks(resolver, citations),
        passages);
  }
}
