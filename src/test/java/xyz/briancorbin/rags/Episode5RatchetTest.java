package xyz.briancorbin.rags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;
import xyz.briancorbin.rags.Ratchet.Tally;
import xyz.briancorbin.rags.RetrievalRun.Step;

/**
 * Episode 5's two steps on the laptop tier, through the ratchet, and both of them lose.
 *
 * <p>Step 5 fuses dense search into Episode 4's step 4, and step 6 reranks what comes back. The
 * numbers asserted here are the ones the article prints: step 5 finds four more questions and still
 * fails, because the relation questions lose two; step 6 is worse than step 5 outright. A step that
 * fails the ratchet is a result, so it is asserted like one. A change here is a new result, not a
 * test to update quietly.
 *
 * <p>Needs, and skips cleanly without: the spectastic checkout at the pinned commit; Ollama serving
 * {@code nomic-embed-text:v1.5} at the pinned digest; and, for step 6 only, {@code
 * tools/rerank-service/serve.py}. Vectors come from the cache shared with the prototype ({@code
 * -Dvectors.cache=…}); with a cold cache the first run embeds all 11,492 passages, which takes
 * about ten minutes.
 */
class Episode5RatchetTest {

  static final String MODEL = "nomic-embed-text:v1.5";

  /** The build of the model the published numbers were measured with. */
  static final String DIGEST = "0a109f422b47";

  static final Path VECTORS =
      Path.of(System.getProperty("vectors.cache", "tools/ratchet-proto/cache/emb"));

  @Test
  void bothEpisode5StepsLoseOnTheLaptopTier() throws Exception {
    Path checkout = Path.of(System.getProperty("spectastic.path", "../spectastic"));
    assumeTrue(
        Files.isDirectory(checkout.resolve("specs")),
        "no spectastic checkout at " + checkout.toAbsolutePath());
    assumeTrue(
        SpecCorpus.commitOf(checkout).equals(RatchetTest.COMMIT),
        "the golden set is frozen at " + RatchetTest.COMMIT);

    String url = System.getProperty("ollama.url", OllamaModel.DEFAULT_URL);
    Optional<String> digest = OllamaModel.digest(url, MODEL);
    assumeTrue(
        digest.isPresent(),
        "Ollama at " + url + " has no " + MODEL + " (ollama pull " + MODEL + ")");
    assumeTrue(
        digest.get().startsWith(DIGEST),
        "a different build of "
            + MODEL
            + ": "
            + digest.get()
            + ", not "
            + DIGEST
            + ". The numbers are a new baseline.");

    String rerankUrl = System.getProperty("rerank.url", CrossEncoderReranker.DEFAULT_URL);
    Optional<String> reranker = CrossEncoderReranker.modelAt(rerankUrl);

    var embedding =
        OllamaEmbeddingModel.builder()
            .ollamaApi(OllamaApi.builder().baseUrl(url).build())
            // truncate=false: a passage too long for the model is an error, never a silent half.
            .options(OllamaEmbeddingOptions.builder().model(MODEL).truncate(false).build())
            .build();

    var questions = GoldenSet.read(new ClassPathResource("golden/questions.jsonl"));
    String modelId = OllamaModel.id(MODEL, digest.get());
    Path out = Path.of("build/episode-5");

    try (var keywords = KeywordSearch.of(new ElementUnits(new ElementReader(checkout).get()));
        var cache = new VectorCache(VECTORS.resolve(OllamaModel.cacheFile(modelId)))) {
      assertEquals(10_540, keywords.units().addresses().size());
      var ids = new IdentifierLookup(keywords.units(), new SpecGrammar());
      // Three layers, each one thing: add the prefixes the model wants, serve what the cache
      // already has, call the model. Prefixing is outermost so the cache key is the text sent.
      Embedder embedder =
          new PrefixedEmbedder(
              new CachingEmbedder(new ModelEmbedder(embedding), cache, modelId),
              EmbeddingPrefix.of(MODEL));
      var vectors = VectorSearch.of(keywords.units(), embedder);
      assertEquals(768, vectors.dimensions());

      var step4 =
          new Step(
              "step-4",
              "+ supersession redirect",
              ElementRetriever.builder()
                  .keywords(keywords)
                  .identifiers(ids)
                  .filter(Lifecycle.IN_FORCE)
                  .build(),
              new SupersessionRedirect(keywords));

      var fused =
          ElementRetriever.builder()
              .keywords(keywords)
              .vectors(vectors)
              .identifiers(ids)
              .filter(Lifecycle.IN_FORCE)
              .build();
      var cut = new TopDocuments(ElementRetriever.DEPTH);
      var redirect = new SupersessionRedirect(keywords);

      var step5 =
          new Step("step-5", "+ dense search, fused", fused, PostProcessors.of(cut, redirect));

      Tally four = tally(step4, questions, out);
      Tally five = tally(step5, questions, out);

      // Step 5: four more questions found, seven more with all their evidence — and it still loses.
      assertEquals(36, five.hits());
      assertEquals(33, five.allEvidence());
      assertEquals(0, five.stale());
      assertEquals(
          Map.of(
              "single-fact",
              9,
              "paraphrase",
              6,
              "identifier",
              8,
              "relation",
              4,
              "stale",
              7,
              "intent",
              2),
          five.hitsByStratum());
      assertEquals(
          List.of("relation 6→4"),
          Ratchet.failures(four, five),
          "step 5 loses the relation questions");

      assumeTrue(
          reranker.isPresent(),
          "no reranker at " + rerankUrl + ": start tools/rerank-service/serve.py for step 6");
      assertEquals("cross-encoder/ms-marco-MiniLM-L-6-v2", reranker.get());
      var step6 =
          new Step(
              "step-6",
              "+ cross-encoder reranking",
              fused,
              PostProcessors.of(new CrossEncoderReranker(rerankUrl), cut, redirect));

      Tally six = tally(step6, questions, out);
      assertEquals(32, six.hits());
      assertEquals(28, six.allEvidence());
      assertEquals(0, six.stale());
      assertEquals(
          Map.of(
              "single-fact",
              7,
              "paraphrase",
              3,
              "identifier",
              8,
              "relation",
              5,
              "stale",
              7,
              "intent",
              2),
          six.hitsByStratum());
      assertEquals(
          List.of("Hit@4 36→32", "all-evidence@4 33→28", "single-fact 9→7", "paraphrase 6→3"),
          Ratchet.failures(five, six),
          "step 6 is worse than step 5 outright");
    }
  }

  private static Tally tally(Step step, List<GoldenSet.Question> questions, Path out) {
    var results = RetrievalRun.run(step, questions);
    RetrievalRun.write(step, results, out);
    parity(step, results, out);
    Tally tally = Tally.of(results);
    assertTrue(tally.questions() == 47, "47 answerable questions, not " + tally.questions());
    return tally;
  }

  /**
   * Every question's whole ranking, one line each, for the diff against the prototype.
   *
   * <p>The run file can't serve: TREC keeps one line per passage, and a split unit's parts share an
   * address, so a ranking that names a unit twice comes out one line shorter. Parity is about the
   * items, duplicates and all, so the items are written as they are.
   */
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
