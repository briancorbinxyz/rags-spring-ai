package xyz.briancorbin.rags.search;

import java.util.List;

/// Text as vectors, exactly as the model returned them.
///
/// Two methods, because an embedding model can be told which of the two it is being handed: a
/// passage to store, or a question to search with. Some models were trained to be told, and answer
/// worse when they are not.
///
/// Implementations are stacked, each one thing: {@link PrefixedEmbedder} adds what the model
/// expects to see, {@link CachingEmbedder} serves what has been embedded before, and {@link
/// ModelEmbedder} is the only one that calls a model. The order matters — prefixing sits above
/// caching, so what is cached is the text that was actually sent.
///
/// The vectors come back unscaled. Normalising is the searcher's business, not the model's: it does
/// it so that a dot product is a cosine.
public interface Embedder {

  /// Vectors for passages being stored, in the order they were given.
  float[][] passages(List<String> texts);

  /// The vector for one question being searched with.
  float[] question(String text);
}
