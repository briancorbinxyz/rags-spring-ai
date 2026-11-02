package xyz.briancorbin.rags.eval;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import xyz.briancorbin.rags.search.ElementRetriever;
import xyz.briancorbin.rags.search.IdentifierLookup;
import xyz.briancorbin.rags.search.KeywordSearch;
import xyz.briancorbin.rags.search.PostProcessors;
import xyz.briancorbin.rags.search.StopWords;
import xyz.briancorbin.rags.search.SupersessionRedirect;
import xyz.briancorbin.rags.search.TopDocuments;
import xyz.briancorbin.rags.spectastic.Lifecycle;
import xyz.briancorbin.rags.spectastic.SpecCorpus;
import xyz.briancorbin.rags.spectastic.SpecGrammar;
import xyz.briancorbin.rags.spectastic.SpectasticReader;
import xyz.briancorbin.rags.units.ElementUnits;
import xyz.briancorbin.rags.units.StandingFilter;

/// What each retrieval step costs in time, per question, on the laptop tier.
///
/// The series reports end-to-end time per question, which is dominated by the two model calls and
/// says nothing about the search. This times the search alone, step by step, with no model in the
/// process at all — so the numbers are the retrieval cost and only that.
///
/// **What this is not.** It is the keyword line: BM25 over element units, the identifier lookup,
/// the lifecycle filter and the supersession redirect. Dense search and reranking are left out on
/// purpose, because one needs an embedding service and the other an HTTP call to a model, and
/// timing either here would measure the network and the GPU rather than the step. Nor is this the
/// 5090 tier — this machine is the laptop, and the article says which rows are which.
///
/// **And it is not a benchmark.** One JVM, one warm run, no isolation from whatever else the
/// machine is doing, wall clock rather than CPU time. It is good enough to say which step costs
/// something and which is free, which is the question. It is not good enough to publish a
/// millisecond figure from, and the report says so.
class StepLatencyTest {

  /// Passes over the golden set. The first is thrown away: a cold JIT and a cold FTS5 page cache
  /// make it two or three times slower than the steady state, and the steady state is the answer.
  private static final int PASSES = 4;

  private static final Path REPORT = Path.of("build/episode-4/step-latency.md");

  private static KeywordSearch keywords;
  private static List<GoldenSet.Question> questions;

  @BeforeAll
  static void theArchiveIsRead() {
    Path checkout = frozenCheckout();
    questions = BlindAudit.audited(GoldenSet.read(new ClassPathResource("golden/questions.jsonl")));
    keywords =
        KeywordSearch.of(
            new ElementUnits(new SpectasticReader(checkout).get(), Lifecycle::of),
            StopWords.of("stopwords/english.txt", "stopwords/spectastic.txt"));
  }

  @AfterAll
  static void theKeywordTableIsClosed() {
    if (keywords != null) {
      keywords.close();
    }
  }

  /// One step's per-question times, in milliseconds, sorted.
  private record Timing(String step, String label, List<Double> millis) {

    double at(double quantile) {
      return millis.get(Math.min(millis.size() - 1, (int) (quantile * millis.size())));
    }

    double total() {
      return millis.stream().mapToDouble(Double::doubleValue).sum();
    }
  }

  @Test
  void timesEveryStepOfTheKeywordLine() {
    var timings = new ArrayList<Timing>();
    for (RetrievalRun.Step step : steps()) {
      timings.add(time(step));
    }
    write(report(timings));
    assertTrue(timings.stream().allMatch(t -> !t.millis().isEmpty()), "nothing was timed");
  }

  // --- measuring ---------------------------------------------------------------------------------

  /// Every question through `step`, {@value #PASSES} times, keeping the last pass.
  private static Timing time(RetrievalRun.Step step) {
    List<Double> last = List.of();
    for (int pass = 0; pass < PASSES; pass++) {
      var millis = new ArrayList<Double>();
      for (GoldenSet.Question question : questions) {
        long from = System.nanoTime();
        step.search(question.question());
        millis.add((System.nanoTime() - from) / 1_000_000.0);
      }
      last = millis;
    }
    return new Timing(step.name(), step.label(), last.stream().sorted().toList());
  }

  // --- reporting ---------------------------------------------------------------------------------

  private static String report(List<Timing> timings) {
    return caveat() + table(timings);
  }

  private static String caveat() {
    return """
        # What each retrieval step costs

        The keyword line only, with no model in the process: BM25 over the element units, the
        identifier lookup, the lifecycle filter and the supersession redirect. Dense search and
        reranking are left out, because timing them here would measure an embedding service and a
        GPU rather than a step. This machine is the **laptop** tier.

        Wall clock, one JVM, %d passes over the %d questions with the first discarded for JIT and
        page-cache warmup, and nothing isolating the machine. Read it as which steps cost something
        and which are free. Do not quote a millisecond figure from it.

        """
        .formatted(PASSES, questions.size());
  }

  private static String table(List<Timing> timings) {
    var out = new StringBuilder("| step | p50 | p95 | max | whole set |\n|---|---|---|---|---|\n");
    for (Timing t : timings) {
      out.append(
          "| %s, %s | %.1f ms | %.1f ms | %.1f ms | %.2f s |%n"
              .formatted(
                  t.step(), t.label(), t.at(0.50), t.at(0.95), t.at(0.999), t.total() / 1000.0));
    }
    return out.toString();
  }

  private static void write(String report) {
    try {
      Files.createDirectories(REPORT.getParent());
      Files.writeString(REPORT, report, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    System.out.println(report);
  }

  // --- given -----------------------------------------------------------------------------------

  private static Path frozenCheckout() {
    Path checkout = Path.of(System.getProperty("spectastic.path", "../spectastic"));
    RatchetInputs.present(
        Files.isDirectory(checkout.resolve("specs")),
        "no spectastic checkout at " + checkout.toAbsolutePath());
    RatchetInputs.present(
        SpecCorpus.commitOf(checkout).equals(RatchetTest.COMMIT),
        "the golden set is frozen at " + RatchetTest.COMMIT);
    return checkout;
  }

  /// Episode 4's four steps, built the way `RatchetTest` builds them.
  private static List<RetrievalRun.Step> steps() {
    var identifiers = new IdentifierLookup(keywords.units(), new SpecGrammar());
    var cut = new TopDocuments(ElementRetriever.DEPTH);
    return List.of(
        new RetrievalRun.Step(
            "step-1",
            "element units + BM25",
            ElementRetriever.builder().keywords(keywords).build(),
            cut),
        new RetrievalRun.Step(
            "step-2",
            "+ identifier lookup",
            ElementRetriever.builder().keywords(keywords).identifiers(identifiers).build(),
            cut),
        new RetrievalRun.Step("step-3", "+ lifecycle filter", inForce(identifiers), cut),
        new RetrievalRun.Step(
            "step-4",
            "+ supersession redirect",
            inForce(identifiers),
            PostProcessors.of(cut, new SupersessionRedirect(keywords))));
  }

  private static ElementRetriever inForce(IdentifierLookup identifiers) {
    return ElementRetriever.builder()
        .keywords(keywords)
        .identifiers(identifiers)
        .filter(StandingFilter.of(Lifecycle.IN_FORCE))
        .build();
  }
}
