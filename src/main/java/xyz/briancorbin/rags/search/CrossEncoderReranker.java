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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import xyz.briancorbin.rags.units.ElementUnits;
import xyz.briancorbin.rags.units.Units;

/// Reranking: a cross-encoder reads the question and a passage together.
///
/// An embedding model turns a question and a passage into vectors separately, and never sees them
/// side by side. A cross-encoder does the opposite: one pass over the pair, which is why it scores
/// better and why it costs so much more. So it is used as a second pass over the first {@value
/// ElementRetriever#DEPTH} results, not as a search.
///
/// It reads exactly what a reader would be given, {@link Units#passage}: the heading path, then the
/// unit's text. Pinned units are never reranked — they were looked up by name, and a model's
/// opinion does not outrank an identifier — so they keep their place at the front.
///
/// Spring AI 2.0.1 has no reranking abstraction, so this is a plain {@link DocumentPostProcessor}.
/// The model itself is `cross-encoder/ms-marco-MiniLM-L-6-v2` run by sentence-transformers behind
/// `tools/rerank-service`: running a cross-encoder in-process in Java means an ONNX export and a
/// tokenizer, which is an episode of its own. One HTTP call keeps the scores identical to the
/// prototype's, which is what the parity check needs.
public final class CrossEncoderReranker implements DocumentPostProcessor {

  private static final Logger log = LoggerFactory.getLogger(CrossEncoderReranker.class);

  /// Where `tools/rerank-service/serve.py` listens by default.
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
    List<Double> scores;
    try {
      scores = scores(query.text(), head.stream().map(Units::passage).toList());
    } catch (RuntimeException e) {
      // Episode 2's lesson, applied where it was first dropped: an outage should cost the
      // improvement, not the answer. The fused order is what this stage was handed, and it is a
      // perfectly good ranking — it is simply the one from before the reranker had an opinion.
      // Loud, because a run that quietly stops reranking is a run whose numbers mean something
      // else.
      log.warn("Reranking skipped, serving the fused order: {}", e.getMessage());
      return documents;
    }

    return reordered(pinned, head, scores, rest.subList(top, rest.size()));
  }

  /// The pinned units, then the rescored candidates best first, then whatever was past the depth
  /// the reranker reads. Equal scores keep the order the fusion gave them.
  private static List<Document> reordered(
      List<Document> pinned, List<Document> head, List<Double> scores, List<Document> tail) {
    var order = new ArrayList<Integer>();
    for (int i = 0; i < head.size(); i++) {
      order.add(i);
    }
    order.sort(Comparator.comparingDouble(position -> -scores.get(position)));

    var out = new ArrayList<Document>(pinned.size() + head.size() + tail.size());
    out.addAll(pinned);
    for (int i : order) {
      out.add(ElementUnits.scored(head.get(i), scores.get(i)));
    }
    out.addAll(tail);
    return List.copyOf(out);
  }

  /// One score per passage, higher is more relevant; the scale is the model's own.
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
        // Seconds, not minutes. Fifty passages through a cross-encoder is a second or two; ten
        // minutes is not patience, it is a hang nobody will wait out.
        .timeout(Duration.ofSeconds(30))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(JsonMapper.shared().writeValueAsString(body)))
        .build();
  }

  /// One score per passage, or an exception: a short list would silently mis-order the results.
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

  /// The model the service has loaded, or empty if it is not answering: for a test's assumption.
  public static Optional<String> modelAt(String baseUrl) {
    try {
      HttpResponse<String> response = health(baseUrl);
      if (response.statusCode() != 200) {
        return Optional.empty();
      }
      var parsed =
          JsonMapper.shared()
              .readValue(response.body(), new TypeReference<Map<String, Object>>() {});
      // Name and revision together, because a name alone is not a version: the service pins a
      // commit of the model repository and reports it, so a run can record what it scored with.
      String name = (String) parsed.get("model");
      Object revision = parsed.get("revision");
      return Optional.ofNullable(name).map(it -> revision == null ? it : it + "@" + revision);
    } catch (IOException | RuntimeException e) {
      return Optional.empty();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  private static HttpResponse<String> health(String baseUrl)
      throws IOException, InterruptedException {
    var request =
        HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$", "") + "/health"))
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();
    // Closed, unlike the instance client above, which lives as long as the reranker does: an
    // unclosed HttpClient holds a non-daemon thread, and this one is built for a single question
    // about the service and then abandoned.
    try (var probe = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
      return probe.send(request, HttpResponse.BodyHandlers.ofString());
    }
  }
}
