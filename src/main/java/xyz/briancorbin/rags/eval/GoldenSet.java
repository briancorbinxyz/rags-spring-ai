package xyz.briancorbin.rags.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

/**
 * The golden set: questions with known answers and known sources, so a change to the pipeline can
 * be measured rather than judged by feel.
 *
 * <p>One JSON object per line, in the format the search series shares. Every later episode is
 * measured on these same questions, and the set is versioned with the corpus: each question records
 * the spectastic commit it was written against.
 */
public final class GoldenSet {

  /** The kinds of question, each testing one thing. */
  public static final List<String> STRATA =
      List.of(
          "single-fact", "paraphrase", "identifier", "relation", "stale", "intent", "unanswerable");

  /**
   * A passage the blind audit accepted as answering a question too.
   *
   * @param address the accepted element address
   * @param forTarget {@code question} when it answers the whole question, or the one expected
   *     address it can stand in for
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Accept(String address, @JsonProperty("for") String forTarget) {

    public boolean wholeQuestion() {
      return "question".equals(forTarget);
    }
  }

  /**
   * One question.
   *
   * @param id a stable id, e.g. {@code G-012}
   * @param question what a reader would ask
   * @param stratum which kind of question it is, one of {@link #STRATA}
   * @param expected the element addresses that answer it, {@code file#element-id}; empty for an
   *     unanswerable question
   * @param accept passages the audit accepted as answering it too
   * @param stale for a stale question, the withdrawn or superseded elements that would answer it
   *     wrongly
   * @param answer a one-line reference answer
   * @param commit the spectastic commit the question was checked against
   * @param status {@code candidate} until a person has checked it against the source, then {@code
   *     accepted}
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Question(
      String id,
      String question,
      String stratum,
      List<String> expected,
      List<Accept> accept,
      List<String> stale,
      String answer,
      String commit,
      String status) {

    public Question {
      expected = expected == null ? List.of() : List.copyOf(expected);
      accept = accept == null ? List.of() : List.copyOf(accept);
      stale = stale == null ? List.of() : List.copyOf(stale);
      status = status == null ? "candidate" : status;
    }

    public boolean answerable() {
      return !"unanswerable".equals(stratum);
    }
  }

  private GoldenSet() {}

  /** Reads a JSON-lines file; blank lines and lines starting with {@code //} are skipped. */
  public static List<Question> read(Resource resource) {
    var questions = new ArrayList<Question>();
    try (var in =
        new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = in.readLine()) != null) {
        if (line.isBlank() || line.strip().startsWith("//")) {
          continue;
        }
        questions.add(JsonMapper.shared().readValue(line, Question.class));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the golden set " + resource, e);
    }
    return List.copyOf(questions);
  }
}
