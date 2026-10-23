package xyz.briancorbin.rags.spectastic;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.jsoup.nodes.Element;
import xyz.briancorbin.rags.units.Standing;

/**
 * Where a record is in its life, read from the marks spectastic puts on it.
 *
 * <ul>
 *   <li>{@link #CURRENT}: what the archive says today;
 *   <li>{@link #HISTORY}: an applied change under {@code changes/archive/}, the proposal that made
 *       the current text what it is;
 *   <li>{@link #WITHDRAWN}, {@link #SUPERSEDED}, {@link #REJECTED}: out of date, and kept out of
 *       the search.
 * </ul>
 *
 * <p>{@link #OUT_OF_DATE} is the one list of which those are, and {@link #IN_FORCE} is its
 * complement, so adding a sixth lifecycle puts it on one side only, and never on both or neither.
 *
 * <p>{@code IN_FORCE} is not the same idea as the {@code stale@4} the ratchet counts. That one is
 * per question: the golden set names the records that would mislead a particular question, and four
 * of the nine it names are current or applied history. A record can be in force and still be the
 * wrong thing to hand a reader.
 */
public enum Lifecycle implements Standing {
  CURRENT,
  HISTORY,
  WITHDRAWN,
  SUPERSEDED,
  REJECTED;

  /** Replaced, withdrawn or never accepted: the search leaves these out. */
  public static final Set<Lifecycle> OUT_OF_DATE =
      Collections.unmodifiableSet(EnumSet.of(WITHDRAWN, SUPERSEDED, REJECTED));

  /**
   * A search filter: everything {@link #OUT_OF_DATE} is not. Applied history stays, because it is
   * the change that made the current text what it is.
   */
  public static final Set<Lifecycle> IN_FORCE =
      Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.copyOf(OUT_OF_DATE)));

  /** A search filter: every unit, whatever state it is in. */
  public static final Set<Lifecycle> ALL =
      Collections.unmodifiableSet(EnumSet.allOf(Lifecycle.class));

  private static final Set<String> EXCLUDED = Set.of("withdrawn", "superseded", "rejected");

  @Override
  public boolean outOfDate() {
    return OUT_OF_DATE.contains(this);
  }

  @Override
  public String label() {
    return name().toLowerCase(Locale.ROOT);
  }

  /** Current first, then the applied change that produced it, then everything out of date. */
  @Override
  public int precedence() {
    return switch (this) {
      case CURRENT -> 0;
      case HISTORY -> 1;
      default -> 2;
    };
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
