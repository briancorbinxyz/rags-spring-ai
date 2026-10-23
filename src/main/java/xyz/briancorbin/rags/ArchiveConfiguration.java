package xyz.briancorbin.rags;

import java.nio.file.Path;
import java.util.Locale;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import xyz.briancorbin.rags.answer.Archive;
import xyz.briancorbin.rags.answer.CitationChecker;
import xyz.briancorbin.rags.answer.QuoteResolver;
import xyz.briancorbin.rags.search.PrefixedEmbeddingModel;
import xyz.briancorbin.rags.spectastic.SpecCorpus;

/// The archive's parts: the embedding model, the vector store, the corpus, and the two commentators
/// with their advisors.
@Configuration
class ArchiveConfiguration {

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

  /// Quoter finds things. Its {@link QuestionAnswerAdvisor} searches the store on every call and
  /// pastes the top four chunks into the prompt. The memory advisor runs first, so it stores the
  /// question as asked, not with four chunks pasted under it.
  @Bean
  ChatClient quoterChatClient(
      ChatClient.Builder builder,
      ChatMemory chatMemory,
      SimpleVectorStore store,
      @Value("classpath:persona/quoter.md") Resource persona) {
    return builder
        .defaultSystem(persona)
        // Copying wants a cool head; Quoter's personality lives in the aside.
        .defaultOptions(ChatOptions.builder().temperature(0.3))
        .defaultAdvisors(
            MessageChatMemoryAdvisor.builder(chatMemory).build(),
            QuestionAnswerAdvisor.builder(store).build())
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
        .defaultOptions(ChatOptions.builder().temperature(0.4))
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
        .build();
  }

  /// The answerer alone, for `archive.mode=direct`: it searches and answers in one call.
  @Bean
  ChatClient soloAnswererChatClient(
      ChatClient.Builder builder,
      ChatMemory chatMemory,
      SimpleVectorStore store,
      @Value("classpath:persona/answerer-solo.md") Resource persona) {
    return builder
        .defaultSystem(persona)
        .defaultOptions(ChatOptions.builder().temperature(0.3))
        .defaultAdvisors(
            MessageChatMemoryAdvisor.builder(chatMemory).build(),
            QuestionAnswerAdvisor.builder(store).build())
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
      @Qualifier("soloAnswererChatClient") ChatClient soloAnswerer,
      SimpleVectorStore store,
      QuoteResolver resolver,
      CitationChecker citations,
      @Value("${archive.mode:quoted}") String mode) {
    return new Archive(
        new Archive.Roles(
            Archive.Mode.valueOf(mode.strip().toUpperCase(Locale.ROOT)),
            quoter,
            answerer,
            soloAnswerer),
        new Archive.Checks(resolver, citations),
        store);
  }
}
