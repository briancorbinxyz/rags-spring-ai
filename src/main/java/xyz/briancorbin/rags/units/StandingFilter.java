package xyz.briancorbin.rags.units;

import java.util.Set;

/**
 * Which standings a search will look at.
 *
 * <p>A set of standings cannot say "all of them" without listing them, and listing them means
 * knowing whose archive it is. So "all" is a state of this filter rather than a set, and the search
 * can be told to look at everything without ever naming a single standing.
 *
 * @param allowed the standings to look at, empty when {@link #everything} is true
 * @param everything every standing, including ones this code has never heard of
 */
public record StandingFilter(Set<? extends Standing> allowed, boolean everything) {

  public StandingFilter {
    allowed = Set.copyOf(allowed);
  }

  /** Every standing: the search leaves nothing out. */
  public static StandingFilter all() {
    return new StandingFilter(Set.of(), true);
  }

  /** Only these: anything else is left out of the search entirely. */
  public static StandingFilter of(Set<? extends Standing> allowed) {
    return new StandingFilter(allowed, false);
  }

  public boolean allows(Standing standing) {
    return everything || allowed.contains(standing);
  }

  /** True when the filter allows nothing at all, which a search must answer with no results. */
  public boolean allowsNothing() {
    return !everything && allowed.isEmpty();
  }
}
