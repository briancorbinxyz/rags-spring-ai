package xyz.briancorbin.rags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;

/**
 * The archive as element units: one per element with an id, in archive order.
 *
 * <p>This is what both searches search and what everything else addresses. A unit is a {@link
 * Document} from {@link SpectasticReader}, and a long one is split into parts that share an
 * address, so {@link #parts} is how you ask for a unit rather than one piece of it.
 *
 * <p>It holds no index of its own. {@link KeywordSearch} and {@link VectorSearch} each build their
 * own over the same units, which is what lets their results be fused: the same unit, found two
 * ways.
 */
public final class ElementUnits {

  private final List<Document> units;
  private final Map<String, Integer> rows = new HashMap<>(); // document id -> archive position
  private final LinkedHashMap<String, List<Document>> parts =
      new LinkedHashMap<>(); // address -> parts

  public ElementUnits(List<Document> units) {
    this.units = List.copyOf(units);
    for (int row = 0; row < this.units.size(); row++) {
      Document unit = this.units.get(row);
      rows.put(unit.getId(), row);
      parts.computeIfAbsent(Units.address(unit), address -> new ArrayList<>()).add(unit);
    }
  }

  /** Every unit, in archive order; a split unit's parts are each their own entry. */
  public List<Document> all() {
    return units;
  }

  /** Every part of the unit at {@code address}, in order; empty if there is no such unit. */
  public List<Document> parts(String address) {
    return parts.getOrDefault(address, List.of());
  }

  /** Addresses in archive order, each once: one per unit, however many parts it has. */
  public SequencedSet<String> addresses() {
    return parts.sequencedKeySet();
  }

  /** The units' element ids, for the identifier lookup. */
  public Set<String> anchors() {
    return units.stream()
        .map(unit -> String.valueOf(unit.getMetadata().get(Units.ANCHOR)))
        .collect(Collectors.toSet());
  }

  /** A unit's position in the archive, which is how ties are broken. */
  public int row(Document unit) {
    return rows.get(unit.getId());
  }

  public int size() {
    return units.size();
  }

  /**
   * A copy of a unit carrying a score; the stored unit is left alone.
   *
   * <p>A search returns copies because a score belongs to a result, not to the archive: the same
   * unit comes back from BM25 with a BM25 score, from the vectors with a cosine, and from the
   * fusion with neither.
   */
  public static Document scored(Document unit, Double score) {
    return Document.builder()
        .id(unit.getId())
        .text(unit.getText())
        .metadata(new HashMap<>(unit.getMetadata()))
        .score(score)
        .build();
  }
}
