package xyz.briancorbin.rags;

import java.util.Locale;
import java.util.Set;
import org.jsoup.nodes.Element;

/**
 * Where a record is in its life, read from the marks spectastic puts on it.
 *
 * <ul>
 *   <li>{@link #CURRENT}: what the archive says today;
 *   <li>{@link #HISTORY}: an applied change under {@code changes/archive/}, the proposal that made
 *       the current text what it is;
 *   <li>{@link #WITHDRAWN}, {@link #SUPERSEDED}, {@link #REJECTED}: out of date, and kept out of
 *       the search (Step 3).
 * </ul>
 */
public enum Lifecycle {
  CURRENT,
  HISTORY,
  WITHDRAWN,
  SUPERSEDED,
  REJECTED;

  private static final Set<String> EXCLUDED = Set.of("withdrawn", "superseded", "rejected");

  /** Withdrawn, superseded or rejected: the search leaves these out. */
  public boolean excluded() {
    return this == WITHDRAWN || this == SUPERSEDED || this == REJECTED;
  }

  /** The value stored in a {@code Document}'s metadata. */
  public String value() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static Lifecycle of(Object value) {
    return valueOf(String.valueOf(value).toUpperCase(Locale.ROOT));
  }

  /**
   * Reads an element's lifecycle from its folder and from the {@code data-status} (or {@code
   * status}) on the element or any element around it. The nearest status wins.
   */
  static Lifecycle read(String source, Element el) {
    if (source.contains("/changes/withdrawn/")) {
      return WITHDRAWN;
    }
    String nearest = null;
    boolean excluded = false;
    for (Element e = el; e != null; e = e.parent()) {
      String status = e.attr("data-status").isEmpty() ? e.attr("status") : e.attr("data-status");
      if (status.isEmpty()) {
        continue;
      }
      if (nearest == null) {
        nearest = status;
      }
      excluded |= EXCLUDED.contains(status);
    }
    if (excluded) {
      return EXCLUDED.contains(nearest) ? of(nearest) : SUPERSEDED;
    }
    return source.contains("/changes/archive/") ? HISTORY : CURRENT;
  }
}
