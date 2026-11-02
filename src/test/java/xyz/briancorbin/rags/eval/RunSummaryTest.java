package xyz.briancorbin.rags.eval;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import xyz.briancorbin.rags.spectastic.SpecCorpus;

/// The stamp above the table, which is the only place a reader learns what the numbers were
/// measured with.
///
/// Worth its own test because the stamp fails quietly. A pinned seed that never reaches the stamp
/// records a run as repeatable without saying what it would have to be repeated with, and nothing
/// about the table looks wrong when that happens.
class RunSummaryTest {

  private static final SpecCorpus.Manifest CORPUS =
      new SpecCorpus.Manifest(
          "c64d41c2e5d0d42c4fefd1b506cb083b9812bf6c", "nomic", 627, 2147, 300_000, 1043);

  @Test
  void theStampSaysHowTheTokensWereChosen() {
    String stamp = summaryWith(new GoldenRun.Sampling(42, 0.9, 40, "30m", Optional.of("0.40.3")));

    assertTrue(stamp.contains("seed 42"), stamp);
    assertTrue(stamp.contains("top_p 0.9"), stamp);
    assertTrue(stamp.contains("top_k 40"), stamp);
    assertTrue(stamp.contains("keep_alive 30m"), stamp);
    assertTrue(stamp.contains("Ollama 0.40.3"), stamp);
  }

  /// A version Ollama would not tell us is said to be missing rather than left blank or guessed: an
  /// empty field reads as "no version", which is a different claim from "did not answer".
  @Test
  void anUnreachableOllamaIsRecordedAsUnreported() {
    String stamp = summaryWith(new GoldenRun.Sampling(42, 0.9, 40, "30m", Optional.empty()));

    assertTrue(stamp.contains("Ollama not reported"), stamp);
    assertFalse(stamp.contains("Optional"), stamp);
  }

  @Test
  void theStampStillSaysWhatTheCorpusWas() {
    String stamp = summaryWith(new GoldenRun.Sampling(42, 0.9, 40, "30m", Optional.of("0.40.3")));

    assertTrue(stamp.contains("Commit c64d41c"), stamp);
    assertTrue(stamp.contains("627 files, 2147 chunks"), stamp);
  }

  private static String summaryWith(GoldenRun.Sampling sampling) {
    var setup =
        new GoldenRun.Setup(CORPUS, "qwen3:8b", "nomic-embed-text:v1.5", Map.of(), sampling, "abc");
    return RunSummary.summary(List.of(), setup);
  }
}
