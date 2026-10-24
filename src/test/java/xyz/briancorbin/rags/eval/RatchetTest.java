package xyz.briancorbin.rags.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import xyz.briancorbin.rags.eval.Ratchet.Tally;
import xyz.briancorbin.rags.eval.RetrievalRun.Step;
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

/// Episode 4's main line, step by step, through the ratchet: the golden set, no model, and each
/// step against the one before. Writes each step's run file to `build/episode-4/`.
///
/// Needs the spectastic checkout at `../spectastic` (or `-Dspectastic.path=…`) at the commit the
/// golden set was frozen against; skipped otherwise.
class RatchetTest {

  static final String COMMIT = "c64d41c2e5d0d42c4fefd1b506cb083b9812bf6c";

  private static final Path RUNS = Path.of("build/episode-4");

  private static KeywordSearch keywords;
  private static IdentifierLookup ids;
  private static TopDocuments cut;
  private static List<GoldenSet.Question> questions;

  /// Reading the archive and indexing 10,540 units is the expensive part, so it happens once.
  @BeforeAll
  static void theArchiveIsReadAndIndexedOnce() {
    Path checkout = Path.of(System.getProperty("spectastic.path", "../spectastic"));
    assumeTrue(
        Files.isDirectory(checkout.resolve("specs")),
        "no spectastic checkout at " + checkout.toAbsolutePath());
    assumeTrue(
        SpecCorpus.commitOf(checkout).equals(COMMIT), "the golden set is frozen at " + COMMIT);

    questions = BlindAudit.audited(GoldenSet.read(new ClassPathResource("golden/questions.jsonl")));
    keywords =
        KeywordSearch.of(
            new ElementUnits(new SpectasticReader(checkout).get(), Lifecycle::of),
            StopWords.of("stopwords/english.txt", "stopwords/spectastic.txt"));
    ids = new IdentifierLookup(keywords.units(), new SpecGrammar());
    // The retriever hands on everything a top 50 could need; cutting to 50 is a step of its own,
    // so Episode 5 can rerank before the cut rather than after it.
    cut = new TopDocuments(ElementRetriever.DEPTH);
  }

  @AfterAll
  static void theKeywordTableIsClosed() {
    if (keywords != null) {
      keywords.close();
    }
  }

  @Test
  void theArchiveReadsAsTenAndAHalfThousandUnits() {
    assertEquals(10_540, keywords.units().addresses().size());
  }

  @Test
  void everyStepOfTheMainLinePassesTheRatchet() {
    List<Tally> tallies = tallyEachStepOf(episode4MainLine());

    assertThatNoStepLostGround(tallies);
    assertThatTheNumbersAreTheOnesTheArticlePrints(tallies);
  }

  @Test
  void aStepFailsIfItGoesBackwardsOnAnyRule() {
    var before = new Tally(47, 32, 26, 1, Map.of("identifier", 8, "paraphrase", 2));
    var sideways = new Tally(47, 32, 26, 0, Map.of("identifier", 7, "paraphrase", 3));
    var backwards = new Tally(47, 31, 25, 2, Map.of("identifier", 6, "paraphrase", 3));

    assertEquals(List.of(), Ratchet.failures(before, sideways));
    assertEquals(
        List.of("Hit@4 32→31", "all-evidence@4 26→25", "identifier 8→6", "stale@4 1→2"),
        Ratchet.failures(before, backwards));
  }

  // --- given -----------------------------------------------------------------------------------

  private static List<Step> episode4MainLine() {
    return List.of(
        elementUnitsAndKeywordSearch(),
        plusIdentifierLookup(),
        plusTheStandingFilter(),
        plusTheSupersessionRedirect());
  }

  private static Step elementUnitsAndKeywordSearch() {
    return new Step(
        "step-1",
        "element units + BM25",
        ElementRetriever.builder().keywords(keywords).build(),
        cut);
  }

  private static Step plusIdentifierLookup() {
    return new Step(
        "step-2",
        "+ identifier lookup",
        ElementRetriever.builder().keywords(keywords).identifiers(ids).build(),
        cut);
  }

  private static Step plusTheStandingFilter() {
    return new Step("step-3", "+ lifecycle filter", inForce().build(), cut);
  }

  private static Step plusTheSupersessionRedirect() {
    return new Step(
        "step-4",
        "+ supersession redirect",
        inForce().build(),
        PostProcessors.of(cut, new SupersessionRedirect(keywords)));
  }

  private static ElementRetriever.Builder inForce() {
    return ElementRetriever.builder()
        .keywords(keywords)
        .identifiers(ids)
        .filter(StandingFilter.of(Lifecycle.IN_FORCE));
  }

  // --- when ------------------------------------------------------------------------------------

  private static List<Tally> tallyEachStepOf(List<Step> steps) {
    var tallies = new ArrayList<Tally>();
    for (Step step : steps) {
      var results = RetrievalRun.run(step, questions);
      RetrievalRun.write(step, results, RUNS);
      tallies.add(Tally.of(results));
    }
    return tallies;
  }

  // --- then ------------------------------------------------------------------------------------

  private static void assertThatNoStepLostGround(List<Tally> tallies) {
    for (int step = 1; step < tallies.size(); step++) {
      List<String> why = Ratchet.failures(tallies.get(step - 1), tallies.get(step));
      assertTrue(why.isEmpty(), "step-" + (step + 1) + " fails the ratchet: " + why);
    }
  }

  /// The numbers Episode 4 prints. A change here is a new result, not a test to update quietly.
  private static void assertThatTheNumbersAreTheOnesTheArticlePrints(List<Tally> tallies) {
    assertEquals(List.of(30, 32, 32, 32), tallies.stream().map(Tally::hits).toList());
    assertEquals(List.of(24, 26, 26, 26), tallies.stream().map(Tally::allEvidence).toList());
    assertEquals(List.of(3, 3, 1, 0), tallies.stream().map(Tally::stale).toList());
    assertEquals(
        Map.of(
            "single-fact", 8,
            "paraphrase", 2,
            "identifier", 8,
            "relation", 6,
            "stale", 7,
            "intent", 1),
        tallies.getLast().hitsByStratum());
  }
}
