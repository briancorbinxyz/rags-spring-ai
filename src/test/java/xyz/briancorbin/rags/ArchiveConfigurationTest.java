package xyz.briancorbin.rags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import xyz.briancorbin.rags.eval.GoldenRun;

/// Which search Beta gets, and that the answer is the element search unless someone asks otherwise.
///
/// Episode 4's promise was that a step which passes the ratchet can go straight into Beta. Until
/// Episode 6 it couldn't: every improvement from Episode 4 on lived in the evaluation harness while
/// the app still answered through 800-token chunks. These assert the wiring, so the app and the
/// measurements cannot drift apart again without a test going red. None of them loads a context or
/// reads the archive.
class ArchiveConfigurationTest {

  private final ArchiveConfiguration configuration = new ArchiveConfiguration();

  @Test
  void theElementSearchIsWhatBetaSearchesWhenItIsWired() {
    var elements =
        RetrievalAugmentationAdvisor.builder().documentRetriever(query -> List.of()).build();

    var chosen = configuration.retrievalAdvisor(Optional.of(elements), null);

    assertSame(elements, chosen, "Beta is not searching the element units");
  }

  /// The one path that searches without an advisor has to search the same way the advisor does, or
  /// a call that failed would be scored against a different search than the one that was used.
  @Test
  void theDirectSearchFollowsWhicheverSearchTheAdvisorUses() {
    var unit = Document.builder().id("specs/040-run-budget/design.html#D-001").text("...").build();

    var found = configuration.passages(Optional.of(query -> List.of(unit)), null);

    assertSame(unit, found.forQuestion("how is the run budget estimated?").getFirst());
  }

  /// An end-to-end run scores by address when the search returns element units, and by text overlap
  /// only for Episode 3's chunks. Getting this backwards does not fail — it reports a much lower
  /// Hit@4 than the ratchet does, from the same run, and says nothing about why.
  @Test
  void anElementSearchIsScoredByAddressAndChunksByOverlap() {
    assertEquals(GoldenRun.By.OVERLAP, RunConfiguration.scoreBy("chunks"));
    assertEquals(GoldenRun.By.ADDRESS, RunConfiguration.scoreBy("elements"));
  }

  /// The dangerous direction: an unset or unexpected value must not quietly pick the chunk scorer,
  /// because the element search is what runs by default.
  @Test
  void andAnythingElseIsScoredTheWayTheDefaultSearchNeeds() {
    assertEquals(GoldenRun.By.ADDRESS, RunConfiguration.scoreBy(""));
    assertEquals(GoldenRun.By.ADDRESS, RunConfiguration.scoreBy("  ELEMENTS  "));
  }

  /// `archive.search` is how a reader goes back to Episode 3's chunks. Leaving it unset must not.
  @Test
  void theElementSearchIsTheDefaultAndNotSomethingToOptInTo() {
    var condition = ElementSearchConfiguration.class.getAnnotation(ConditionalOnProperty.class);

    assertTrue(List.of(condition.name()).contains("archive.search"));
    assertEquals("elements", condition.havingValue());
    assertTrue(condition.matchIfMissing(), "the element search has become opt-in");
  }
}
