package xyz.briancorbin.rags.units;

/**
 * Turns the label stored on a unit back into the standing it meant.
 *
 * <p>A unit carries its standing as a label, because a unit is written to JSON and read back by
 * things that are not this program. Only the archive knows what its own labels mean, so it hands
 * this in alongside the units rather than the search guessing.
 */
@FunctionalInterface
public interface Standings {

  /** The standing that {@code label} names. */
  Standing of(Object label);
}
