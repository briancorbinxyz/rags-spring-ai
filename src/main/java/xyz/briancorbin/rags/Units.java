package xyz.briancorbin.rags;

import org.springframework.ai.document.Document;

/**
 * What a unit carries, and how to read it back.
 *
 * <p>Every search, filter, redirect and report in this codebase goes through these keys, so they
 * are declared once here rather than wherever they were first needed. None of them is spectastic's:
 * a unit has a source, an id, a heading path, a position among its parts, a standing, and possibly
 * a successor, whatever archive it came out of.
 *
 * <p>Reading a unit's standing is the one place the generic side still names the archive, because
 * only the archive knows what its own labels mean. With a second corpus it becomes a port; with one
 * it would be an interface with a single implementation and nothing gained.
 */
public final class Units {

  private Units() {}

  public static final String SOURCE = Address.SOURCE;
  public static final String SCOPE = Address.SPEC;
  public static final String ANCHOR = "anchor";
  public static final String TITLE = "title";
  public static final String HEADING_PATH = "heading_path";
  public static final String HEADING = "heading";
  public static final String STANDING = "lifecycle";
  public static final String PART = "part";
  public static final String PARTS = "parts";
  public static final String SUPERSEDED_BY = "superseded_by";
  public static final String PAGE_SUPERSEDED_BY = "page_superseded_by";

  /** {@code <source>#<anchor>}: the address the golden set and the run files use. */
  public static String address(Document unit) {
    return unit.getMetadata().get(SOURCE) + "#" + unit.getMetadata().get(ANCHOR);
  }

  /**
   * What a reader is given, and what is embedded and reranked: the heading path in front of the
   * text, so {@code FR-001} on its own still carries where it belongs.
   */
  public static String passage(Document unit) {
    String path = String.valueOf(unit.getMetadata().getOrDefault(HEADING_PATH, ""));
    return path.isEmpty() ? unit.getText() : path + "\n" + unit.getText();
  }

  /** Where the unit stands, read back from the label stored on it. */
  public static Standing standing(Document unit) {
    return Lifecycle.of(unit.getMetadata().get(STANDING));
  }
}
