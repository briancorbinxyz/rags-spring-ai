package xyz.briancorbin.rags.eval;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import tools.jackson.databind.json.JsonMapper;
import xyz.briancorbin.rags.eval.GoldenScorer.Score;
import xyz.briancorbin.rags.eval.GoldenSet.Question;
import xyz.briancorbin.rags.search.ElementRetriever;
import xyz.briancorbin.rags.units.Units;

/**
 * Runs the golden set through a search, with no model: what came back, and where the expected
 * passages were in it. This is what the ratchet compares.
 *
 * <p>With element units a result <em>is</em> an element, so scoring is matching addresses: a result
 * is a hit when its address is an expected one, or one the audit accepted. A split unit's parts
 * each count as a result, so a long unit can take two of the top four.
 */
public final class RetrievalRun {

  /** The golden set scores the top four, which is what a reader is handed. */
  public static final int K = 4;

  /** Stale records are looked for in the first ten results. */
  static final int STALE_DEPTH = 10;

  /** One step of the main line: a search, and what runs after it (or null). */
  public record Step(
      String name, String label, DocumentRetriever retriever, DocumentPostProcessor after) {

    public List<Document> search(String question) {
      var query = new Query(question);
      List<Document> found = retriever.retrieve(query);
      return after == null ? found : after.process(query, found);
    }
  }

  /**
   * One question's outcome: the addresses in rank order, how they scored, and the first stale one.
   */
  public record Result(Question question, List<String> ranked, Score score, Integer firstStale) {

    public boolean staleAt(int k) {
      return firstStale != null && firstStale <= k;
    }
  }

  private RetrievalRun() {}

  public static List<Result> run(Step step, List<Question> questions) {
    var results = new ArrayList<Result>();
    for (Question q : questions) {
      List<String> ranked = step.search(q.question()).stream().map(Units::address).toList();
      results.add(new Result(q, ranked, GoldenScorer.byAddress(q, ranked), firstStale(q, ranked)));
    }
    return results;
  }

  private static Integer firstStale(Question q, List<String> ranked) {
    for (int i = 0; i < Math.min(STALE_DEPTH, ranked.size()); i++) {
      if (q.stale().contains(ranked.get(i))) {
        return i + 1;
      }
    }
    return null;
  }

  /**
   * Writes {@code <step>.run} (TREC), {@code <step>.jsonl} (every question's top ten and score)
   * into {@code dir}.
   */
  public static void write(Step step, List<Result> results, Path dir) {
    var trec = new ArrayList<String>();
    var json = new ArrayList<String>();
    for (Result result : results) {
      trec.addAll(
          TrecFiles.run(
              result.question().id(), result.ranked(), step.name(), ElementRetriever.DEPTH));
      var line = new LinkedHashMap<String, Object>();
      line.put("step", step.name());
      line.put("id", result.question().id());
      line.put("stratum", result.question().stratum());
      line.put("expected", result.question().expected());
      line.put("first_hit", result.score().firstHit());
      line.put("first_all", result.score().firstAll());
      line.put("first_stale", result.firstStale());
      line.put("top10", result.ranked().subList(0, Math.min(10, result.ranked().size())));
      json.add(JsonMapper.shared().writeValueAsString(line));
    }
    try {
      Files.createDirectories(dir);
      TrecFiles.write(trec, dir.resolve(step.name() + ".run"));
      Files.write(dir.resolve(step.name() + ".jsonl"), json);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
