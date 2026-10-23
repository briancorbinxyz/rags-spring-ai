package xyz.briancorbin.rags.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;
import xyz.briancorbin.rags.eval.Ratchet.Tally;
import xyz.briancorbin.rags.eval.RetrievalRun.Step;
import xyz.briancorbin.rags.search.CachingEmbedder;
import xyz.briancorbin.rags.search.CrossEncoderReranker;
import xyz.briancorbin.rags.search.ElementRetriever;
import xyz.briancorbin.rags.search.Embedder;
import xyz.briancorbin.rags.search.EmbeddingPrefix;
import xyz.briancorbin.rags.search.IdentifierLookup;
import xyz.briancorbin.rags.search.KeywordSearch;
import xyz.briancorbin.rags.search.ModelEmbedder;
import xyz.briancorbin.rags.search.OllamaModel;
import xyz.briancorbin.rags.search.PostProcessors;
import xyz.briancorbin.rags.search.PrefixedEmbedder;
import xyz.briancorbin.rags.search.StopWords;
import xyz.briancorbin.rags.search.SupersessionRedirect;
import xyz.briancorbin.rags.search.TopDocuments;
import xyz.briancorbin.rags.search.VectorCache;
import xyz.briancorbin.rags.search.VectorSearch;
import xyz.briancorbin.rags.spectastic.Lifecycle;
import xyz.briancorbin.rags.spectastic.SpecCorpus;
import xyz.briancorbin.rags.spectastic.SpecGrammar;
import xyz.briancorbin.rags.spectastic.SpectasticReader;
import xyz.briancorbin.rags.units.ElementUnits;
import xyz.briancorbin.rags.units.StandingFilter;

/// Episode 5's two steps on the laptop tier, through the ratchet, and both of them lose.
///
/// Step 5 fuses dense search into Episode 4's step 4, and step 6 reranks what comes back. The
/// numbers asserted here are the ones the article prints: step 5 finds four more questions and
/// still fails, because the relation questions lose two; step 6 is worse than step 5 outright. A
/// step that fails the ratchet is a result, so it is asserted like one. A change here is a new
/// result, not a test to update quietly.
///
/// Needs, and skips cleanly without: the spectastic checkout at the pinned commit; Ollama serving
/// `nomic-embed-text:v1.5` at the pinned digest; and, for step 6 only,
/// `tools/rerank-service/serve.py`. Vectors come from the cache shared with the prototype
/// (`-Dvectors.cache=…`); with a cold cache the first run embeds all 11,492 passages, which takes
/// about ten minutes.
class Episode5RatchetTest {

  static final String MODEL = "nomic-embed-text:v1.5";

  /// The build of the model the published numbers were measured with.
  static final String DIGEST = "0a109f422b47";

  static final Path VECTORS =
      Path.of(System.getProperty("vectors.cache", "tools/ratchet-proto/cache/emb"));

  private static final Path RUNS = Path.of("build/episode-5");
  private static final String RERANK_URL =
      System.getProperty("rerank.url", CrossEncoderReranker.DEFAULT_URL);

  private static KeywordSearch keywords;
  private static VectorCache cache;
  private static VectorSearch vectors;
  private static ElementRetriever fused;
  private static TopDocuments cut;
  private static SupersessionRedirect redirect;
  private static List<GoldenSet.Question> questions;
  private static Tally four;
  private static Tally five;

  /// Reading the archive, indexing it and embedding 11,492 passages happens once for the class.
  @BeforeAll
  static void theArchiveAndTheEmbeddingsAreReady() {
    Path checkout = frozenCheckout();
    String modelId = OllamaModel.id(MODEL, measuredModel());
    questions = GoldenSet.read(new ClassPathResource("golden/questions.jsonl"));
    keywords =
        KeywordSearch.of(
            new ElementUnits(new SpectasticReader(checkout).get(), Lifecycle::of),
            StopWords.of("stopwords/english.txt", "stopwords/spectastic.txt"));
    cache = new VectorCache(VECTORS.resolve(OllamaModel.cacheFile(modelId)));
    vectors = VectorSearch.of(keywords.units(), embedder(modelId));
    cut = new TopDocuments(ElementRetriever.DEPTH);
    redirect = new SupersessionRedirect(keywords);
    fused = inForce().vectors(vectors).build();
    four = tally(episode4sLastStep());
    five = tally(plusDenseSearchFused());
  }

  @AfterAll
  static void theKeywordTableAndTheCacheAreClosed() {
    if (keywords != null) {
      keywords.close();
    }
    if (cache != null) {
      cache.close();
    }
  }

  @Test
  void theArchiveReadsAsTenAndAHalfThousandUnits() {
    assertEquals(10_540, keywords.units().addresses().size());
  }

  @Test
  void theEmbeddingsHaveTheModelsDimensions() {
    assertEquals(768, vectors.dimensions());
  }

  @Test
  void fusingDenseSearchFindsFourMoreAndStillLoses() {
    assertThatStepFiveIsWhatTheArticlePrints();
    assertEquals(
        List.of("relation 6→4"),
        Ratchet.failures(four, five),
        "step 5 loses the relation questions");
  }

  @Test
  void rerankingIsWorseThanFusingOutright() {
    assumeTrue(
        CrossEncoderReranker.modelAt(RERANK_URL).isPresent(),
        "no reranker at " + RERANK_URL + ": start tools/rerank-service/serve.py for step 6");
    assertEquals(
        "cross-encoder/ms-marco-MiniLM-L-6-v2", CrossEncoderReranker.modelAt(RERANK_URL).get());

    Tally six = tally(plusCrossEncoderReranking());

    assertThatStepSixIsWhatTheArticlePrints(six);
    assertEquals(
        List.of("Hit@4 36→32", "all-evidence@4 33→28", "single-fact 9→7", "paraphrase 6→3"),
        Ratchet.failures(five, six),
        "step 6 is worse than step 5 outright");
  }

  // --- given -----------------------------------------------------------------------------------

  private static Path frozenCheckout() {
    Path checkout = Path.of(System.getProperty("spectastic.path", "../spectastic"));
    assumeTrue(
        Files.isDirectory(checkout.resolve("specs")),
        "no spectastic checkout at " + checkout.toAbsolutePath());
    assumeTrue(
        SpecCorpus.commitOf(checkout).equals(RatchetTest.COMMIT),
        "the golden set is frozen at " + RatchetTest.COMMIT);
    return checkout;
  }

  /// The digest, not just the name: another build of the same model is a new baseline.
  private static String measuredModel() {
    String url = System.getProperty("ollama.url", OllamaModel.DEFAULT_URL);
    Optional<String> digest = OllamaModel.digest(url, MODEL);
    assumeTrue(
        digest.isPresent(),
        "Ollama at " + url + " has no " + MODEL + " (ollama pull " + MODEL + ")");
    assumeTrue(
        digest.get().startsWith(DIGEST),
        "a different build of %s: %s, not %s. The numbers are a new baseline."
            .formatted(MODEL, digest.get(), DIGEST));
    return digest.get();
  }

  /// Three layers, each one thing: add the prefixes the model wants, serve what the cache already
  /// has, call the model. Prefixing is outermost so the cache key is the text that was sent.
  private static Embedder embedder(String modelId) {
    var ollama =
        OllamaEmbeddingModel.builder()
            .ollamaApi(OllamaApi.builder().baseUrl(ollamaUrl()).build())
            // truncate=false: a passage too long for the model is an error, never a silent half.
            .options(OllamaEmbeddingOptions.builder().model(MODEL).truncate(false).build())
            .build();
    return new PrefixedEmbedder(
        new CachingEmbedder(new ModelEmbedder(ollama), cache, modelId), EmbeddingPrefix.of(MODEL));
  }

  private static String ollamaUrl() {
    return System.getProperty("ollama.url", OllamaModel.DEFAULT_URL);
  }

  private static ElementRetriever.Builder inForce() {
    return ElementRetriever.builder()
        .keywords(keywords)
        .identifiers(new IdentifierLookup(keywords.units(), new SpecGrammar()))
        .filter(StandingFilter.of(Lifecycle.IN_FORCE));
  }

  private static Step episode4sLastStep() {
    return new Step("step-4", "+ supersession redirect", inForce().build(), redirect);
  }

  private static Step plusDenseSearchFused() {
    return new Step("step-5", "+ dense search, fused", fused, PostProcessors.of(cut, redirect));
  }

  private static Step plusCrossEncoderReranking() {
    return new Step(
        "step-6",
        "+ cross-encoder reranking",
        fused,
        PostProcessors.of(new CrossEncoderReranker(RERANK_URL), cut, redirect));
  }

  // --- when ------------------------------------------------------------------------------------

  private static Tally tally(Step step) {
    var results = RetrievalRun.run(step, questions);
    RetrievalRun.write(step, results, RUNS);
    parity(step, results, RUNS);
    Tally tally = Tally.of(results);
    assertTrue(tally.questions() == 47, "47 answerable questions, not " + tally.questions());
    return tally;
  }

  // --- then ------------------------------------------------------------------------------------

  /// Four more questions found, seven more with all their evidence, and it still loses.
  private static void assertThatStepFiveIsWhatTheArticlePrints() {
    assertEquals(36, five.hits());
    assertEquals(33, five.allEvidence());
    assertEquals(0, five.stale());
    assertEquals(
        Map.of(
            "single-fact", 9,
            "paraphrase", 6,
            "identifier", 8,
            "relation", 4,
            "stale", 7,
            "intent", 2),
        five.hitsByStratum());
  }

  private static void assertThatStepSixIsWhatTheArticlePrints(Tally six) {
    assertEquals(32, six.hits());
    assertEquals(28, six.allEvidence());
    assertEquals(0, six.stale());
    assertEquals(
        Map.of(
            "single-fact", 7,
            "paraphrase", 3,
            "identifier", 8,
            "relation", 5,
            "stale", 7,
            "intent", 2),
        six.hitsByStratum());
  }

  /// Every question's whole ranking, one line each, for the diff against the prototype.
  ///
  /// The run file can't serve: TREC keeps one line per passage, and a split unit's parts share an
  /// address, so a ranking that names a unit twice comes out one line shorter. Parity is about the
  /// items, duplicates and all, so the items are written as they are.
  private static void parity(Step step, List<RetrievalRun.Result> results, Path out) {
    var lines = new ArrayList<String>();
    for (RetrievalRun.Result r : results) {
      lines.add(
          JsonMapper.shared()
              .writeValueAsString(
                  Map.of("step", step.name(), "id", r.question().id(), "ranked", r.ranked())));
    }
    try {
      Files.createDirectories(out);
      Files.write(out.resolve("parity-" + step.name() + ".jsonl"), lines);
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }
}
