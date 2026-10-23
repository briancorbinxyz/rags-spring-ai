package xyz.briancorbin.rags.search;

import java.util.List;

/// An {@link Embedder} that adds the task prefixes a model was trained with, and hands the rest
/// down.
///
/// `nomic-embed-text` learned which of a passage and a question it was being given from a prefix,
/// and `qwen3-embedding` from an instruction on the query side only. Without them both still return
/// numbers, just worse ones, and nothing warns you. Which prefixes belong to which model is {@link
/// EmbeddingPrefix}'s business; applying them is this class's.
///
/// This sits at the top of the stack so that everything below it — the cache especially — sees the
/// text the model will actually be sent.
public final class PrefixedEmbedder implements Embedder {

  private final Embedder delegate;
  private final EmbeddingPrefix prefix;

  public PrefixedEmbedder(Embedder delegate, EmbeddingPrefix prefix) {
    this.delegate = delegate;
    this.prefix = prefix;
  }

  @Override
  public float[][] passages(List<String> texts) {
    return delegate.passages(texts.stream().map(prefix::document).toList());
  }

  @Override
  public float[] question(String text) {
    return delegate.question(prefix.query(text));
  }
}
