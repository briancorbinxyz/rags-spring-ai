package xyz.briancorbin.rags;

import java.util.SequencedSet;

/**
 * How an archive writes the names of things, so a question that names one can be recognised.
 *
 * <p>The idea of an identifier is not spectastic's: most archives give their parts names and most
 * questions use them. Which shapes count is very much spectastic's — {@code FR-012}, {@code
 * REQ-TOOL-001}, "spec 045", {@code 045-run-budget} — and so is how an address says which part of
 * the archive it belongs to.
 *
 * <p>This says only what a question looks like it names. Whether any of it exists is {@link
 * IdentifierLookup}'s question, because that is the one holding the archive.
 */
public interface IdentifierGrammar {

  /**
   * What a question appears to name: identifiers, then the scopes it qualified them by, each once,
   * in the order the question said them.
   */
  record Named(SequencedSet<String> ids, SequencedSet<String> scopes) {}

  /** Everything in {@code question} shaped like an identifier or a scope. */
  Named named(String question);

  /**
   * The scope the unit at {@code address} belongs to, written the way a question would name it, so
   * that {@link Named#scopes()} and this can be compared directly.
   */
  String scopeOf(String address);
}
