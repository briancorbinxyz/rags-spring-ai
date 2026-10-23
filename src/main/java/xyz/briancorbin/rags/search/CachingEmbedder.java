package xyz.briancorbin.rags.search;

import java.util.ArrayList;
import java.util.List;

/// An {@link Embedder} that serves what has been embedded before, and asks the one underneath only
/// for the rest.
///
/// Keyed by the model id and the exact text handed down, so this has to sit below whatever adds the
/// prefixes: what is cached must be what was sent. A different model, a different prefix or an
/// edited passage is a different key, and so a different vector.
///
/// What the model returned is stored unchanged. The searcher scales vectors to length one when it
/// reads them, which keeps the stored bytes the same as the prototype's and lets the two share a
/// cache file.
public final class CachingEmbedder implements Embedder {

  private final Embedder delegate;
  private final VectorCache cache;
  private final String modelId;

  /// @param modelId what the cache is keyed by: the model and its digest, never just its name
  public CachingEmbedder(Embedder delegate, VectorCache cache, String modelId) {
    this.delegate = delegate;
    this.cache = cache;
    this.modelId = modelId;
  }

  @Override
  public float[][] passages(List<String> texts) {
    var vectors = new float[texts.size()][];
    var missing = new ArrayList<Integer>();
    for (int row = 0; row < texts.size(); row++) {
      float[] cached = cache.get(modelId, texts.get(row));
      if (cached == null) {
        missing.add(row);
      } else {
        vectors[row] = cached;
      }
    }
    if (missing.isEmpty()) {
      return vectors;
    }
    List<String> send = missing.stream().map(texts::get).toList();
    float[][] embedded = delegate.passages(send);
    for (int i = 0; i < missing.size(); i++) {
      cache.put(modelId, send.get(i), embedded[i]);
      vectors[missing.get(i)] = embedded[i];
    }
    cache.commit();
    return vectors;
  }

  @Override
  public float[] question(String text) {
    float[] cached = cache.get(modelId, text);
    if (cached != null) {
      return cached;
    }
    float[] embedded = delegate.question(text);
    cache.put(modelId, text, embedded);
    cache.commit();
    return embedded;
  }

  /// The model and digest the vectors were made with, as the report prints it.
  public String modelId() {
    return modelId;
  }
}
