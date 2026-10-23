package xyz.briancorbin.rags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;

/**
 * Dense search over the element units: the same units BM25 indexes, as vectors.
 *
 * <p>A unit is embedded as {@link ElementReader#passage}, the heading path in front of the text, so
 * {@code FR-001} on its own still carries which spec's requirements it belongs to — the same text a
 * reader would be given, and the same text the reranker scores. Each part of a split unit is its own
 * vector.
 *
 * <p>Every vector is scaled to length one, so the cosine of two of them is their dot product, and
 * a search is an exact scan over all of them. There is no approximate index here on purpose: ten
 * thousand units is small, the scan takes milliseconds, and an exact answer means a difference in
 * the results is never the index's fault. The lifecycle filter is applied during the scan, before
 * the depth cut, the way a vector store applies a metadata filter.
 */
public final class VectorSearch {

  /** What Ollama's embedding endpoint is sent at once. */
  static final int BATCH = 32;

  private final ElementUnits units;
  private final float[][] vectors; // aligned with units, each of length one
  private final Lifecycle[] lifecycles;
  private final EmbeddingModel model;
  private final String modelId;
  private final EmbeddingPrefix prefix;
  private final VectorCache cache;

  private VectorSearch(ElementUnits units, float[][] vectors, Lifecycle[] lifecycles,
      EmbeddingModel model, String modelId, EmbeddingPrefix prefix, VectorCache cache) {
    this.units = units;
    this.vectors = vectors;
    this.lifecycles = lifecycles;
    this.model = model;
    this.modelId = modelId;
    this.prefix = prefix;
    this.cache = cache;
  }

  /**
   * Embeds every unit, reading what the cache already has and asking the model only for the rest.
   *
   * @param modelId what the cache is keyed by: the model and its digest, never just its name
   */
  public static VectorSearch of(ElementUnits units, EmbeddingModel model, String modelId,
      EmbeddingPrefix prefix, VectorCache cache) {
    var texts =
        units.all().stream().map(unit -> prefix.document(ElementReader.passage(unit))).toList();
    var vectors = new float[units.size()][];
    var missing = new ArrayList<Integer>();
    for (int i = 0; i < texts.size(); i++) {
      float[] cached = cache.get(modelId, texts.get(i));
      if (cached == null) {
        missing.add(i);
      } else {
        vectors[i] = normalise(cached);
      }
    }
    for (int from = 0; from < missing.size(); from += BATCH) {
      List<Integer> batch = missing.subList(from, Math.min(from + BATCH, missing.size()));
      List<String> send = batch.stream().map(texts::get).toList();
      var response = model.call(new EmbeddingRequest(send, null));
      for (int j = 0; j < batch.size(); j++) {
        float[] raw = response.getResults().get(j).getOutput();
        cache.put(modelId, send.get(j), raw);
        vectors[batch.get(j)] = normalise(raw);
      }
      cache.commit();
    }
    var lifecycles = units.all().stream().map(ElementReader::lifecycle).toArray(Lifecycle[]::new);
    return new VectorSearch(units, vectors, lifecycles, model, modelId, prefix, cache);
  }

  /**
   * The {@code depth} nearest units to a question, nearest first, each carrying its cosine. Only
   * units whose lifecycle is in {@code filter} are considered, so the cut keeps {@code depth} of
   * the units the filter allows.
   */
  public List<Document> search(String question, int depth, Set<Lifecycle> filter) {
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

  /** A question as a vector of length one, cached like a passage. */
  public float[] query(String question) {
    String text = prefix.query(question);
    float[] cached = cache.get(modelId, text);
    if (cached != null) {
      return normalise(cached);
    }
    float[] raw = model.call(new EmbeddingRequest(List.of(text), null)).getResults().getFirst().getOutput();
    cache.put(modelId, text, raw);
    cache.commit();
    return normalise(raw);
  }

  /** The model and digest the vectors were made with, as the report prints it. */
  public String modelId() {
    return modelId;
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
