package xyz.briancorbin.rags;

import java.nio.file.Path;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import xyz.briancorbin.rags.search.AsRetrieved;
import xyz.briancorbin.rags.search.ElementRetriever;
import xyz.briancorbin.rags.search.IdentifierLookup;
import xyz.briancorbin.rags.search.KeywordSearch;
import xyz.briancorbin.rags.search.PostProcessors;
import xyz.briancorbin.rags.search.StopWords;
import xyz.briancorbin.rags.search.SupersessionRedirect;
import xyz.briancorbin.rags.search.TopDocuments;
import xyz.briancorbin.rags.spectastic.Lifecycle;
import xyz.briancorbin.rags.spectastic.SpecGrammar;
import xyz.briancorbin.rags.spectastic.SpectasticReader;
import xyz.briancorbin.rags.units.ElementUnits;
import xyz.briancorbin.rags.units.StandingFilter;

/// The search the episodes measured, wired for the app: element units, keyword search, the units a
/// question names pinned above the ranking, out-of-date units filtered out, and superseded records
/// redirected to what replaced them.
///
/// Until this existed, every improvement from Episode 4 on lived in the evaluation harness, and
/// `--archive.run=ask` still answered through Episode 3's 800-token chunks. The ratchet's own
/// promise was that a step which passes can go straight into Beta, so here it does.
///
/// This is **Episode 4's step 4**, which is the line the Java build reproduces. Dense fusion and
/// cross-encoder reranking are not here: the first needs 11,492 passages embedded before the app
/// can answer, and the second needs a model the Java side has no client for. Both are measured in
/// the prototype, and Episodes 5 and 6 say so.
///
/// Set `archive.search=chunks` to go back to Episode 3's vector store.
@Configuration
@ConditionalOnProperty(name = "archive.search", havingValue = "elements", matchIfMissing = true)
class ElementSearchConfiguration {

  /// Every element of every page, each with the standing it carries.
  @Bean
  ElementUnits elementUnits(@Value("${spectastic.path}") Path checkout) {
    return new ElementUnits(new SpectasticReader(checkout).get(), Lifecycle::of);
  }

  /// BM25 over the units. Closed with the context: it owns an SQLite FTS5 table.
  @Bean
  KeywordSearch keywordSearch(ElementUnits units) {
    return KeywordSearch.of(
        units, StopWords.of("stopwords/english.txt", "stopwords/spectastic.txt"));
  }

  @Bean
  IdentifierLookup identifierLookup(ElementUnits units) {
    return new IdentifierLookup(units, new SpecGrammar());
  }

  @Bean
  ElementRetriever elementRetriever(KeywordSearch keywords, IdentifierLookup identifiers) {
    return ElementRetriever.builder()
        .keywords(keywords)
        .identifiers(identifiers)
        .filter(StandingFilter.of(Lifecycle.IN_FORCE))
        .build();
  }

  /// What Beta searches through. The retriever hands on everything a top 50 could need, so the cut
  /// to the reader's window happens here, and the redirect runs after it: a superseded record takes
  /// its replacement's place rather than a slot of its own.
  ///
  /// The query augmenter is left at its default, which matters more than it looks: Spring AI's
  /// `ContextualQueryAugmenter` has `allowEmptyContext=false`, so if a post-processor ever returns
  /// an empty list the advisor tells the model the question is outside its knowledge base. Beta
  /// *can* decline here, in other words — what the series has not done is choose the score at which
  /// a post-processor should empty the list.
  @Bean
  RetrievalAugmentationAdvisor elementSearchAdvisor(
      ElementRetriever retriever,
      KeywordSearch keywords,
      ThreadPoolTaskExecutor retrievalExecutor,
      @Value("${archive.passages:4}") int passages) {
    return RetrievalAugmentationAdvisor.builder()
        .documentRetriever(retriever)
        // Without this the advisor's default joiner re-sorts by score and sinks the pinned units.
        .documentJoiner(new AsRetrieved())
        .documentPostProcessors(
            PostProcessors.of(new TopDocuments(passages), new SupersessionRedirect(keywords)))
        // Ours, so that the context shuts it down. See the bean below.
        .taskExecutor(retrievalExecutor)
        .build();
  }

  /// The pool the advisor expands queries on, as a bean rather than its own.
  ///
  /// Left to itself `RetrievalAugmentationAdvisor` builds a `ThreadPoolTaskExecutor` named
  /// `ai-advisor-` inside its constructor, with four core threads and no owner. Spring never sees
  /// it, so nothing ever shuts it down, and `ThreadPoolTaskExecutor`'s threads are **not daemon
  /// threads** — so a command-line program that answers its questions and finishes keeps the JVM
  /// alive, parked on an empty queue, with nothing in the log to say why. Measured here: two golden
  /// runs wrote their results and then sat for sixteen hours.
  ///
  /// Owning it as a bean is not enough on its own, which took a second measurement to learn: the
  /// context is closed by a JVM shutdown hook, and the JVM will not begin shutting down while a
  /// non-daemon thread is alive, so the pool waits for the hook and the hook waits for the pool.
  /// `setDaemon(true)` is what breaks that: the JVM can exit, and the hook still closes the pool in
  /// an application that outlives one question.
  ///
  /// One thread is enough while no query transformer expands a question into several.
  @Bean(destroyMethod = "shutdown")
  ThreadPoolTaskExecutor retrievalExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("archive-retrieval-");
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(4);
    executor.setDaemon(true);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(10);
    return executor;
  }
}
