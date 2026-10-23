package xyz.briancorbin.rags.units;

import org.springframework.ai.document.Document;

/// What a unit carries, and how to read it back.
///
/// Every search, filter, redirect and report in this codebase goes through these keys, so they are
/// declared once here rather than wherever they were first needed. A unit has a source, an id, a
/// heading path, a position among its parts, a standing, and possibly a successor, whatever archive
/// it came out of.
///
/// The standing is stored as a label, and turning a label back into a standing needs {@link
/// Standings}, which the archive hands in: see {@link ElementUnits#standing(Document)}.
public final class Units {

  private Units() {}

  public static final String SOURCE = Address.SOURCE;
  // The two keys below keep the spelling they were first stored under, so that run files and
  // caches written by earlier episodes still read. The names here are the generic ones.
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

  /// `<source>#<anchor>`: the address the golden set and the run files use.
  public static String address(Document unit) {
    return unit.getMetadata().get(SOURCE) + "#" + unit.getMetadata().get(ANCHOR);
  }

  /// What a reader is given, and what is embedded and reranked: the heading path in front of the
  /// text, so a passage's own id still carries which part of the archive it belongs to.
  public static String passage(Document unit) {
    String path = String.valueOf(unit.getMetadata().getOrDefault(HEADING_PATH, ""));
    return path.isEmpty() ? unit.getText() : path + "\n" + unit.getText();
  }

  /// The label a unit's standing was stored under; {@link Standings} says what it means.
  public static Object standingLabel(Document unit) {
    return unit.getMetadata().get(STANDING);
  }
}
