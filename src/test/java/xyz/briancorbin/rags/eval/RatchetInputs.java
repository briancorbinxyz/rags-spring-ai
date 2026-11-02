package xyz.briancorbin.rags.eval;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// What a ratchet run needs before it can mean anything: the pinned checkout, the pinned model, the
/// reranker.
///
/// `./gradlew test` skips when one is missing, so a reader who cloned the repo without the archive
/// still gets a green build. `./gradlew ratchetTest` fails instead.
///
/// Both audiences are served, which is the point. A regression test that turns green when its
/// inputs are gone is a false guarantee, and Episode 4 sells the ratchet as a regression test.
/// Anyone copying it into CI would have got no protection at all.
final class RatchetInputs {

  /// Set by the `ratchetTest` task, and by nothing else.
  private static final boolean REQUIRED = Boolean.getBoolean("ratchet.required");

  private RatchetInputs() {}

  /// Skips the test when the input is missing, or fails if this run demanded it.
  static void present(boolean there, String whatIsMissing) {
    if (REQUIRED && !there) {
      throw new AssertionError("the ratchet needs what it measures: " + whatIsMissing);
    }
    assumeTrue(there, whatIsMissing);
  }
}
