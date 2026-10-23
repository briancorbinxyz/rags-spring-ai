package xyz.briancorbin.rags.search;

import java.util.Optional;

/**
 * What replaced what, so a search can follow a paper trail instead of answering from the past.
 *
 * <p>Two questions, because an archive can replace a record or a whole page: which record now says
 * what this one used to, and which page now stands where this one did.
 *
 * <p>How that is discovered is the archive's business and nobody else's. An archive may say it in
 * markup, in prose, in a side table or not at all; whatever it does, the redirect that reads this
 * never learns which.
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
