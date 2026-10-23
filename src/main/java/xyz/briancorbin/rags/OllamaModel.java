package xyz.briancorbin.rags;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * What model Ollama actually has, and which build of it.
 *
 * <p>A tag is not a version: {@code nomic-embed-text:v1.5} can be re-pulled and be different bytes.
 * The digest is the build, so the digest is what a vector cache key and a published number have to
 * carry. {@link #id} is the form the prototype keys its cache by, so the two share one cache file.
 */
public final class OllamaModel {

  /** Where Ollama listens unless told otherwise. */
  public static final String DEFAULT_URL = "http://localhost:11434";

  private OllamaModel() {}

  /** The model's digest, or empty if Ollama is not answering or does not have it. */
  public static Optional<String> digest(String baseUrl, String model) {
    try {
      var request =
          HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$", "") + "/api/tags"))
              .timeout(Duration.ofSeconds(10))
              .GET()
              .build();
      HttpResponse<String> response =
          HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()
              .send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return Optional.empty();
      }
      var parsed =
          JsonMapper.shared().readValue(response.body(), new TypeReference<Map<String, Object>>() {});
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> models = (List<Map<String, Object>>) parsed.get("models");
      if (models == null) {
        return Optional.empty();
      }
      return models.stream()
          .filter(listed -> model.equals(listed.get("name")) || model.equals(listed.get("model")))
          .map(listed -> (String) listed.get("digest"))
          .filter(digest -> digest != null && !digest.isBlank())
          .findFirst();
    } catch (IOException | RuntimeException e) {
      return Optional.empty();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  /** {@code ollama:<model>@<first twelve of the digest>}: what the vector cache is keyed by. */
  public static String id(String model, String digest) {
    return "ollama:" + model + "@" + digest.substring(0, Math.min(12, digest.length()));
  }

  /** The cache file for a model id, named as the prototype names it. */
  public static String cacheFile(String modelId) {
    return modelId.replaceAll("[^A-Za-z0-9.-]+", "_") + ".sqlite";
  }
}
