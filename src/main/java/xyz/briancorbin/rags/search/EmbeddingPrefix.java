package xyz.briancorbin.rags.search;

import java.util.Locale;

/**
 * The text an embedding model is actually given, as its model card asks for it.
 *
 * <p>An embedding model that was trained on prefixed text expects them, and without them it still
 * returns numbers, just worse ones, with nothing to warn you. The two on the laptop tier want
 * different things:
 *
 * <ul>
 *   <li>{@link #NOMIC}: {@code search_document: } in front of a passage and {@code search_query: }
 *       in front of a question, the same strings {@link PrefixedEmbeddingModel} sends.
 *   <li>{@link #QWEN3}: passages bare, and a question behind the task sentence from the
 *       Qwen3-Embedding model card's own example. The card asks for a task sentence per use case;
 *       keeping the card's default is what stops it being tuned on the golden set.
 * </ul>
 *
 * <p>The prefix is part of what gets embedded, so it is part of what the vector cache is keyed by:
 * change a prefix and every vector is computed again, which is the point.
 */
public enum EmbeddingPrefix {

  /** nomic-embed-text, which learned which text is which from a prefix. */
  NOMIC(PrefixedEmbeddingModel.DOCUMENT, PrefixedEmbeddingModel.QUERY),

  /** Qwen3-Embedding: documents bare, questions behind the model card's default task. */
  QWEN3("", "Instruct: " + EmbeddingPrefix.QWEN3_TASK + "\nQuery:"),

  /** Anything else: the text as it is. */
  NONE("", "");

  /** The Qwen3-Embedding model card's example task, used unchanged. */
  static final String QWEN3_TASK =
      "Given a web search query, retrieve relevant passages that answer the query";

  private final String document;
  private final String query;

  EmbeddingPrefix(String document, String query) {
    this.document = document;
    this.query = query;
  }

  /** Which prefixes a model wants, read from its name. */
  public static EmbeddingPrefix of(String model) {
    String name = model.toLowerCase(Locale.ROOT);
    int colon = name.indexOf(':');
    String bare = colon < 0 ? name : name.substring(colon + 1); // ollama:nomic-embed-text:v1.5
    if (bare.startsWith("nomic-embed-text") || name.startsWith("nomic-embed-text")) {
      return NOMIC;
    }
    if (bare.startsWith("qwen3-embedding") || name.startsWith("qwen3-embedding")) {
      return QWEN3;
    }
    return NONE;
  }

  /** A passage as the model wants it. */
  public String document(String passage) {
    return document + passage;
  }

  /** A question as the model wants it. */
  public String query(String question) {
    return query + question;
  }
}
