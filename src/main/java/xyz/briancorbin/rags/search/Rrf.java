package xyz.briancorbin.rags.search;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import xyz.briancorbin.rags.units.ElementUnits;

/**
 * Reciprocal rank fusion: how two rankings that disagree, and whose scores can't be compared,
 * become one ranking.
 *
 * <p>Each list gives a unit {@code 1 / (k + rank)}, and a unit's score is the sum over the lists it
 * appears in. Only the ranks are read, never the scores, which is the point: BM25's score and a
 * cosine aren't on the same scale, and nothing has to be normalised for them to be fused. A unit
 * both lists rank well beats a unit only one of them loves.
 *
 * <p>{@code k} damps the top of each list: with {@value #K} the gap between rank 1 and rank 2 is
 * small, so one list can't carry a unit on its own. It is the value the paper used and the value
 * everyone kept; it is fixed in advance here, like every other setting.
 *
 * <p>Ties are broken by where a unit first appeared — earlier list first, then better rank — so
 * fusing is deterministic, and fusing one list leaves it alone.
 */
public final class Rrf {

  /** The constant from the paper, fixed in advance and never tuned. */
  public static final int K = 60;

  private Rrf() {}

  /** The lists fused, best first, each unit once, carrying its fused score. */
  public static List<Document> fuse(List<List<Document>> lists) {
    var fused = new Fused();
    for (int list = 0; list < lists.size(); list++) {
      fused.add(lists.get(list), list);
    }
    return fused.ranked();
  }

  /**
   * What a fusion accumulates: each unit's running score, where it was first seen, and the unit
   * itself. Three maps that are only ever used together, so they travel together.
   */
  private static final class Fused {

    private final Map<String, Double> score = new LinkedHashMap<>();
    private final Map<String, long[]> first = new LinkedHashMap<>(); // id -> {list, rank}
    private final Map<String, Document> unit = new LinkedHashMap<>();

    /** One ranking's contribution: 1/(K + rank) for each unit it names. */
    void add(List<Document> ranking, int list) {
      for (int i = 0; i < ranking.size(); i++) {
        Document doc = ranking.get(i);
        int rank = i + 1;
        score.merge(doc.getId(), 1.0 / (K + rank), Double::sum);
        if (first.putIfAbsent(doc.getId(), new long[] {list, rank}) == null) {
          unit.put(doc.getId(), doc);
        }
      }
    }

    /** Best score first; a tie goes to whichever list saw it first, then to its rank there. */
    List<Document> ranked() {
      return score.keySet().stream()
          .sorted(
              Comparator.comparingDouble((String id) -> -score.get(id))
                  .thenComparingLong(id -> first.get(id)[0])
                  .thenComparingLong(id -> first.get(id)[1]))
          .map(id -> ElementUnits.scored(unit.get(id), score.get(id)))
          .toList();
    }
  }

  /** The fused score of each unit, for a test or a report. */
  static Map<String, Double> scores(List<List<Document>> lists) {
    var out = new LinkedHashMap<String, Double>();
    for (Document d : fuse(lists)) {
      out.put(d.getId(), d.getScore());
    }
    return out;
  }
}
