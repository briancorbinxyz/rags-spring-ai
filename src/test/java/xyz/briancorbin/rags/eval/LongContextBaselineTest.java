package xyz.briancorbin.rags.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/// The two decisions the long-context baseline makes before any model is called: which spec a
/// question is about, and how big a window that spec needs.
///
/// The window matters more than it looks. Too small and Ollama truncates the spec silently, and the
/// run measures the truncation rather than the retrieval; too large and the laptop reserves a
/// key-value cache it has to find in swap, which is how two runs died. So the sizing is pinned
/// here, against the one spec whose token count was actually measured.
class LongContextBaselineTest {

  /// The largest spec in the set, measured: 24,598 words, which Ollama tokenised as 20,482.
  private static final int LARGEST_SPEC_WORDS = 24_598;

  /// The smallest spec a question named in the last run.
  private static final int SMALLEST_SPEC_WORDS = 2_648;

  @Test
  void theLargestSpecGetsTheLargestWindow() {
    assertEquals(24_576, LongContextBaseline.windowFor(LARGEST_SPEC_WORDS));
  }

  @Test
  void aSmallSpecDoesNotReserveTheLargestWindow() {
    assertEquals(8_192, LongContextBaseline.windowFor(SMALLEST_SPEC_WORDS));
  }

  /// A word is worth about 0.83 tokens in this corpus, so counting one token per word and adding
  /// room to answer in has to leave the measured spec inside its window, not merely near it.
  @Test
  void theMeasuredSpecFitsItsWindowWithRoomToAnswer() {
    int window = LongContextBaseline.windowFor(LARGEST_SPEC_WORDS);
    int measuredTokens = 20_482;
    assertTrue(
        window - measuredTokens >= 2_048,
        "the largest spec needs room to answer in, not just room to arrive: " + window);
  }

  /// Every window boundary, from just inside to just over, because an off-by-one here truncates a
  /// spec and nothing in the output says so.
  @Test
  void eachWindowHoldsWhatItClaimsTo() {
    assertEquals(8_192, LongContextBaseline.windowFor(8_192 - 2_048));
    assertEquals(12_288, LongContextBaseline.windowFor(8_192 - 2_048 + 1));
    assertEquals(12_288, LongContextBaseline.windowFor(12_288 - 2_048));
    assertEquals(16_384, LongContextBaseline.windowFor(12_288 - 2_048 + 1));
    assertEquals(24_576, LongContextBaseline.windowFor(16_384 - 2_048 + 1));
  }

  /// Beyond the largest window the sizing caps rather than throws: the cap is safe because the
  /// largest spec is known to fit inside it, and a longer corpus would need measuring again.
  @Test
  void aSpecLongerThanAnyWindowIsCappedAtTheLargest() {
    assertEquals(24_576, LongContextBaseline.windowFor(100_000));
  }

  @Test
  void findsTheSpecAQuestionNamesInProseAndInSlugs() {
    assertEquals(
        Optional.of("045"), LongContextBaseline.theOneSpecNamed("What does spec 045 say?"));
    assertEquals(
        Optional.of("065"),
        LongContextBaseline.theOneSpecNamed("See 065-rate-limits for the rule"));
    assertEquals(
        Optional.of("105"), LongContextBaseline.theOneSpecNamed("slice 105 and spec 105 again"));
  }

  @Test
  void aQuestionNamingTwoSpecsOrNoneIsNotThisExperiment() {
    assertEquals(
        Optional.empty(), LongContextBaseline.theOneSpecNamed("spec 045 against spec 065"));
    assertEquals(Optional.empty(), LongContextBaseline.theOneSpecNamed("Which database is used?"));
  }
}
