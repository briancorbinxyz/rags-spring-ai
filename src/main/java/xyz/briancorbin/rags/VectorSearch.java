package xyz.briancorbin.rags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.ai.document.Document;

/**
 * Dense search over the element units: the same units BM25 indexes, as vectors.
 *
 * <p>A unit is embedded as {@link Units#passage}, the heading path in front of the text, so {@code
 * FR-001} on its own still carries which spec's requirements it belongs to — the same text a reader
 * would be given, and the same text the reranker scores. Each part of a split unit is its own
 * vector.
 *
 * <p>Every vector is scaled to length one, so the cosine of two of them is their dot product, and a
 * search is an exact scan over all of them. There is no approximate index here on purpose: ten
 * thousand units is small, the scan takes milliseconds, and an exact answer means a difference in
 * the results is never the index's fault. The lifecycle filter is applied during the scan, before
 * the depth cut, the way a vector store applies a metadata filter.
 */
public final class VectorSearch {

  private final ElementUnits units;
  private final float[][] vectors; // aligned with units, each of length one
  private final Lifecycle[] lifecycles;
  private final Embedder embedder;

  private VectorSearch(
      ElementUnits units, float[][] vectors, Lifecycle[] lifecycles, Embedder embedder) {
    this.units = units;
    this.vectors = vectors;
    this.lifecycles = lifecycles;
    this.embedder = embedder;
  }

  /**
   * Embeds every unit through {@code embedder}, which is where the prefixes and the cache live.
   *
   * <p>A unit is embedded as its passage — the heading path in front of the text — so {@code
   * FR-001} on its own still carries which spec's requirements it belongs to.
   */
  public static VectorSearch of(ElementUnits units, Embedder embedder) {
    List<String> passages = units.all().stream().map(Units::passage).toList();
    float[][] vectors = normalisedAll(embedder.passages(passages));
    var lifecycles = units.all().stream().map(Units::standing).toArray(Lifecycle[]::new);
    return new VectorSearch(units, vectors, lifecycles, embedder);
  }

  /**
   * The {@code depth} nearest units to a question, nearest first, each carrying its cosine. Only
   * units whose lifecycle is in {@code filter} are considered, so the cut keeps {@code depth} of
   * the units the filter allows.
   */
  public List<Document> search(String question, int depth, Set<? extends Standing> filter) {
    if (depth <= 0) {
      return List.of();
    }
    float[] query = query(question);
    var allowed = new ArrayList<Integer>();
    for (int i = 0; i < units.size(); i++) {
      if (filter.contains(lifecycles[i])) {
        allowed.add(i);
      }
    }
    var similarity = new double[units.size()];
    for (int i : allowed) {
      similarity[i] = dot(vectors[i], query);
    }
    // Best first; equal scores keep archive order, as the prototype's stable argsort does.
    allowed.sort(
        Comparator.comparingDouble((Integer row) -> -similarity[row]).thenComparingInt(row -> row));
    var out = new ArrayList<Document>(Math.min(depth, allowed.size()));
    for (int i = 0; i < Math.min(depth, allowed.size()); i++) {
      int row = allowed.get(i);
      out.add(ElementUnits.scored(units.all().get(row), similarity[row]));
    }
    return out;
  }

  /** A question as a vector of length one. */
  public float[] query(String question) {
    return normalise(embedder.question(question));
  }

  public int dimensions() {
    return vectors.length == 0 ? 0 : vectors[0].length;
  }

  private static double dot(float[] a, float[] b) {
    double sum = 0;
    for (int i = 0; i < a.length; i++) {
      sum += (double) a[i] * b[i];
    }
    return sum;
  }

  private static float[][] normalisedAll(float[][] raw) {
    var out = new float[raw.length][];
    for (int row = 0; row < raw.length; row++) {
      out[row] = normalise(raw[row]);
    }
    return out;
  }

  /** A copy scaled to length one; a zero vector is left as it is, as the prototype leaves it. */
  private static float[] normalise(float[] v) {
    double sum = 0;
    for (float x : v) {
      sum += (double) x * x;
    }
    double norm = Math.sqrt(sum);
    if (norm == 0) {
      return v.clone();
    }
    var out = new float[v.length];
    for (int i = 0; i < v.length; i++) {
      out[i] = (float) (v[i] / norm);
    }
    return out;
  }
}
