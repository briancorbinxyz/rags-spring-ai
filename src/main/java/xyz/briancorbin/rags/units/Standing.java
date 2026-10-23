package xyz.briancorbin.rags.units;

/**
 * Where a record stands: whether it still speaks for the archive, or has been left behind by it.
 *
 * <p>The search needs three things of it and nothing else — a label to store, whether to leave the
 * record out, and which of two records answering the same question to prefer. What the standings
 * *are*, and how they are read off a record, belong to the archive. An archive with no notion of
 * being out of date answers {@code false} and {@code 0} to everything here.
 */
public interface Standing {

  /** The value stored on a unit, and matched against when a search filters on standing. */
  String label();

  /** Replaced, withdrawn or never accepted: the search leaves these out. */
  boolean outOfDate();

  /**
   * Lower is preferred when two records could answer the same question, so an identifier lookup
   * pins the most authoritative first. It is not the same idea as {@link #outOfDate()}: an archive
   * can keep records that are in force and still rank them.
   */
  int precedence();
}
