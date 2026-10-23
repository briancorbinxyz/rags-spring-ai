package xyz.briancorbin.rags;

import java.util.Optional;

/**
 * What replaced what, so a search can follow a paper trail instead of answering from the past.
 *
 * <p>Two questions, because an archive can replace a record or a whole page: which record now says
 * what this one used to, and which page now stands where this one did.
 *
 * <p>How that is discovered is the archive's business and nobody else's. Spectastic says it three
 * ways — "superseded by" in prose before a link, a change proposal's {@code spec-delta target}, and
 * an in-place {@code spec-status} with a link after it — and {@link Supersession} reads all three.
 * The redirect that uses this never learns which way any of it was said.
 */
public interface Successors {

  /** The record that replaced the one at {@code address}, if one did. */
  Optional<String> successor(String address);

  /** The page that replaced the page {@code source}, if one did. */
  Optional<String> pageSuccessor(String source);

  /** Nothing was ever replaced: for a corpus with no notion of supersession. */
  static Successors none() {
    return new Successors() {
      @Override
      public Optional<String> successor(String address) {
        return Optional.empty();
      }

      @Override
      public Optional<String> pageSuccessor(String source) {
        return Optional.empty();
      }
    };
  }
}
