package xyz.briancorbin.rags.search;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import xyz.briancorbin.rags.units.ElementUnits;
import xyz.briancorbin.rags.units.Units;

/**
 * Episode 5's reranking (Step 6): a cross-encoder reads the question and a passage together.
 *
 * <p>An embedding model turns a question and a passage into vectors separately, and never sees them
 * side by side. A cross-encoder does the opposite: one pass over the pair, which is why it scores
 * better and why it costs so much more. So it is used as a second pass over the first {@value
 * ElementRetriever#DEPTH} results, not as a search.
 *
 * <p>It reads exactly what a reader would be given, {@link Units#passage}: the heading path, then
 * the unit's text. Pinned units are never reranked — they were looked up by name, and a model's
 * opinion does not outrank an identifier — so they keep their place at the front.
 *
 * <p>Spring AI 2.0.1 has no reranking abstraction, so this is a plain {@link
 * DocumentPostProcessor}. The model itself is {@code cross-encoder/ms-marco-MiniLM-L-6-v2} run by
 * sentence-transformers behind {@code tools/rerank-service}: running a cross-encoder in-process in
 * Java means an ONNX export and a tokenizer, which is an episode of its own. One HTTP call keeps
 * the scores identical to the prototype's, which is what the parity check needs.
 */
public final class CrossEncoderReranker implements DocumentPostProcessor {

  /** Where {@code tools/rerank-service/serve.py} listens by default. */
  public static final String DEFAULT_URL = "http://127.0.0.1:8099";

  private final URI endpoint;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public CrossEncoderReranker(String baseUrl) {
    this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/rerank");
  }

  @Override
  public List<Document> process(Query query, List<Document> documents) {
    var pinned = new ArrayList<Document>();
    var rest = new ArrayList<Document>();
    for (Document doc : documents) {
      (ElementRetriever.isPinned(doc) ? pinned : rest).add(doc);
    }
    int top = Math.min(ElementRetriever.DEPTH, rest.size()); // every candidate the retriever kept
    List<Document> head = rest.subList(0, top);
    if (head.isEmpty()) {
      return documents;
    }
    List<Double> scores = scores(query.text(), head.stream().map(Units::passage).toList());

    var order = new ArrayList<Integer>();
    for (int i = 0; i < head.size(); i++) {
      order.add(i);
    }
    // Best first; equal scores keep the order the fusion gave them.
    order.sort(Comparator.comparingDouble(position -> -scores.get(position)));

    var out = new ArrayList<Document>(documents.size());
    out.addAll(pinned);
    for (int i : order) {
      out.add(ElementUnits.scored(head.get(i), scores.get(i)));
    }
    out.addAll(rest.subList(top, rest.size()));
    return List.copyOf(out);
  }

  /** One score per passage, higher is more relevant; the scale is the model's own. */
  private List<Double> scores(String question, List<String> passages) {
    try {
      HttpResponse<String> response =
          http.send(scoreRequest(question, passages), HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException(
            "The reranker at %s answered %d: %s"
                .formatted(endpoint, response.statusCode(), response.body()));
      }
      return parsed(response.body(), passages.size());
    } catch (IOException e) {
      throw new UncheckedIOException("The reranker at " + endpoint + " is not answering", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private HttpRequest scoreRequest(String question, List<String> passages) {
    var body = new LinkedHashMap<String, Object>();
    body.put("query", question);
    body.put("passages", passages);
    return HttpRequest.newBuilder(endpoint)
        .timeout(Duration.ofMinutes(10))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.shared().writeValueAsString(body)))
        .build();
  }

  /** One score per passage, or an exception: a short list would silently mis-order the results. */
  private List<Double> parsed(String body, int expected) {
    var parsed = JsonMapper.shared().readValue(body, new TypeReference<Map<String, Object>>() {});
    @SuppressWarnings("unchecked")
    List<Number> raw = (List<Number>) parsed.get("scores");
    if (raw == null || raw.size() != expected) {
      throw new IllegalStateException(
          "The reranker returned %s scores for %d passages"
              .formatted(raw == null ? "no" : String.valueOf(raw.size()), expected));
    }
    return raw.stream().map(Number::doubleValue).toList();
  }

  /** The model the service has loaded, or empty if it is not answering: for a test's assumption. */
  public static java.util.Optional<String> modelAt(String baseUrl) {
    try {
      var request =
          HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$", "") + "/health"))
              .timeout(Duration.ofSeconds(5))
              .GET()
              .build();
      HttpResponse<String> response =
          HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(3))
              .build()
              .send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return java.util.Optional.empty();
      }
      var parsed =
          JsonMapper.shared()
              .readValue(response.body(), new TypeReference<Map<String, Object>>() {});
      return java.util.Optional.ofNullable((String) parsed.get("model"));
    } catch (IOException | InterruptedException | RuntimeException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return java.util.Optional.empty();
    }
  }
}
