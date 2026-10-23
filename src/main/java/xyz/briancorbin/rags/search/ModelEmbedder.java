package xyz.briancorbin.rags.search;

import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;

/**
 * The {@link Embedder} that actually calls a model, a batch at a time.
 *
 * <p>Nothing else in the stack talks to the model, so this is the only place a request is made and
 * the only place the batch size matters.
 */
public final class ModelEmbedder implements Embedder {

  /** What Ollama's embedding endpoint is sent at once. */
  static final int BATCH = 32;

  private final EmbeddingModel model;

  public ModelEmbedder(EmbeddingModel model) {
    this.model = model;
  }

  @Override
  public float[][] passages(List<String> texts) {
    var vectors = new float[texts.size()][];
    for (int from = 0; from < texts.size(); from += BATCH) {
      List<String> batch = texts.subList(from, Math.min(from + BATCH, texts.size()));
      var response = model.call(new EmbeddingRequest(batch, null));
      for (int i = 0; i < batch.size(); i++) {
        vectors[from + i] = response.getResults().get(i).getOutput();
      }
    }
    return vectors;
  }

  @Override
  public float[] question(String text) {
    return model
        .call(new EmbeddingRequest(List.of(text), null))
        .getResults()
        .getFirst()
        .getOutput();
  }
}
