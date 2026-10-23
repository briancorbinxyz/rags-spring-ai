package xyz.briancorbin.rags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import xyz.briancorbin.rags.Ratchet.Tally;
import xyz.briancorbin.rags.RetrievalRun.Step;

/**
 * Episode 4's main line, step by step, through the ratchet: the golden set, no model, and each step
 * against the one before. Writes each step's run file to {@code build/episode-4/}.
 *
 * <p>Needs the spectastic checkout at {@code ../spectastic} (or {@code -Dspectastic.path=…}) at the
 * commit the golden set was frozen against; skipped otherwise.
 */
class RatchetTest {

  static final String COMMIT = "c64d41c2e5d0d42c4fefd1b506cb083b9812bf6c";

  @Test
  void everyStepOfTheMainLinePassesTheRatchet() throws Exception {
    Path checkout = Path.of(System.getProperty("spectastic.path", "../spectastic"));
    assumeTrue(
        Files.isDirectory(checkout.resolve("specs")),
        "no spectastic checkout at " + checkout.toAbsolutePath());
    assumeTrue(
        SpecCorpus.commitOf(checkout).equals(COMMIT), "the golden set is frozen at " + COMMIT);

    var questions = GoldenSet.read(new ClassPathResource("golden/questions.jsonl"));
    try (var keywords = KeywordSearch.of(new ElementUnits(new ElementReader(checkout).get()))) {
      assertEquals(10_540, keywords.units().addresses().size());
      var ids = new IdentifierLookup(keywords.units(), new SpecGrammar());
      // The retriever hands on everything a top 50 could need; cutting to 50 is a step of its own,
      // so Episode 5 can rerank before the cut rather than after it.
      var cut = new TopDocuments(ElementRetriever.DEPTH);
      var steps =
          List.of(
              new Step(
                  "step-1",
                  "element units + BM25",
                  ElementRetriever.builder().keywords(keywords).build(),
                  cut),
              new Step(
                  "step-2",
                  "+ identifier lookup",
                  ElementRetriever.builder().keywords(keywords).identifiers(ids).build(),
                  cut),
              new Step(
                  "step-3",
                  "+ lifecycle filter",
                  ElementRetriever.builder()
                      .keywords(keywords)
                      .identifiers(ids)
                      .filter(Lifecycle.IN_FORCE)
                      .build(),
                  cut),
              new Step(
                  "step-4",
                  "+ supersession redirect",
                  ElementRetriever.builder()
                      .keywords(keywords)
                      .identifiers(ids)
                      .filter(Lifecycle.IN_FORCE)
                      .build(),
                  PostProcessors.of(cut, new SupersessionRedirect(keywords))));

      var tallies = new ArrayList<Tally>();
      for (Step step : steps) {
        var results = RetrievalRun.run(step, questions);
        RetrievalRun.write(step, results, Path.of("build/episode-4"));
        Tally tally = Tally.of(results);
        if (!tallies.isEmpty()) {
          List<String> why = Ratchet.failures(tallies.getLast(), tally);
          assertTrue(why.isEmpty(), step.name() + " fails the ratchet: " + why);
        }
        tallies.add(tally);
      }

      // The numbers Episode 4 prints. A change here is a new result, not a test to update quietly.
      assertEquals(List.of(30, 32, 32, 32), tallies.stream().map(Tally::hits).toList());
      assertEquals(List.of(24, 26, 26, 26), tallies.stream().map(Tally::allEvidence).toList());
      assertEquals(List.of(3, 3, 1, 0), tallies.stream().map(Tally::stale).toList());
      assertEquals(
          Map.of(
              "single-fact",
              8,
              "paraphrase",
              2,
              "identifier",
              8,
              "relation",
              6,
              "stale",
              7,
              "intent",
              1),
          tallies.getLast().hitsByStratum());
    }
  }

  @Test
  void aStepFailsIfItGoesBackwardsOnAnyRule() {
    var before = new Tally(47, 32, 26, 1, Map.of("identifier", 8, "paraphrase", 2));
    assertEquals(
        List.of(),
        Ratchet.failures(
            before, new Tally(47, 32, 26, 0, Map.of("identifier", 7, "paraphrase", 3))));
    assertEquals(
        List.of("Hit@4 32→31", "all-evidence@4 26→25", "identifier 8→6", "stale@4 1→2"),
        Ratchet.failures(
            before, new Tally(47, 31, 25, 2, Map.of("identifier", 6, "paraphrase", 3))));
  }
}
