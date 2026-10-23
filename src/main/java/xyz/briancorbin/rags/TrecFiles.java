package xyz.briancorbin.rags;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.SequencedMap;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.json.JsonMapper;
import xyz.briancorbin.rags.GoldenSet.Question;

/**
 * The golden set as a TREC test collection, so our numbers can be checked with information
 * retrieval's own scorers ({@code trec_eval}, {@code ir_measures}, {@code ranx}) instead of ours.
 *
 * <ul>
 *   <li><b>qrels</b>: one judgment per line, {@code question 0 passage relevance}. Relevance 1 is an
 *       expected passage or one the blind audit accepted; 0 is a passage someone judged and found
 *       wanting: a stale record, or an audit decision of "partly" or "doesn't answer it".
 *       Unanswerable questions have no relevant passage, so they have no lines.
 *   <li><b>run</b>: one result per line, {@code question Q0 passage rank score run-name}. A long
 *       unit is split into parts, so a ranking can name a passage twice; the scorers keep one entry
 *       per passage, so only the first is written.
 * </ul>
 *
 * <p>Our Hit@4 is {@code Success@4} in those tools and our MRR is {@code RR}. All-evidence@4 has no
 * exact counterpart: a stand-in covers one expected element, which qrels can't say.
 *
 * <p>The same rules, and byte-for-byte the same qrels, as {@code tools/ratchet-proto/ratchet/trec.py}.
 */
public final class TrecFiles {

  /** One blind-audit decision: {@code whole}, {@code standin}, {@code partly} or {@code no}. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Decision(String id, String candidate, String decision) {

    boolean accepted() {
      return "whole".equals(decision) || "standin".equals(decision);
    }
  }

  private TrecFiles() {}

  /**
   * Judgments in a fixed order: per question, expected, then accepted, then stale, then the audit's
   * other decisions sorted by passage. A passage is judged once, by the first rule that names it.
   */
  public static List<String> qrels(List<Question> questions, List<Decision> audit) {
    var lines = new ArrayList<String>();
    for (Question question : answerableById(questions)) {
      judgmentsFor(question, audit)
          .forEach(
              (passage, relevance) ->
                  lines.add(question.id() + " 0 " + passage + " " + relevance));
    }
    return lines;
  }

  /** The answerable questions by id, so a qrels file comes out the same every time. */
  private static List<Question> answerableById(List<Question> questions) {
    return questions.stream()
        .filter(Question::answerable)
        .sorted(Comparator.comparing(Question::id))
        .toList();
  }

  /**
   * One question's judgments, in the order they are written: expected, then accepted, then stale,
   * then whatever else the audit decided. A passage is judged once, by the first rule that names it.
   */
  private static SequencedMap<String, Integer> judgmentsFor(Question question, List<Decision> audit) {
    var judged = new LinkedHashMap<String, Integer>();
    question.expected().forEach(expected -> judged.putIfAbsent(expected, 1));
    question.accept().forEach(accepted -> judged.putIfAbsent(accepted.address(), 1));
    question.stale().forEach(stale -> judged.putIfAbsent(stale, 0));
    audit.stream()
        .filter(decision -> decision.id().equals(question.id()))
        .sorted(Comparator.comparing(Decision::candidate))
        .forEach(decision -> judged.putIfAbsent(decision.candidate(), decision.accepted() ? 1 : 0));
    return judged;
  }

  /**
   * One line per passage, first occurrence only, ranks from 1. The score is 1/rank, because the
   * scorers sort by score and this keeps our order.
   */
  public static List<String> run(String questionId, List<String> ranked, String runName, int depth) {
    var lines = new ArrayList<String>();
    var seen = new HashSet<String>();
    for (String passage : ranked) {
      if (lines.size() == depth) {
        break;
      }
      if (seen.add(passage)) {
        int rank = lines.size() + 1;
        lines.add(
            "%s Q0 %s %d %s %s"
                .formatted(
                    questionId, passage, rank, String.format(Locale.ROOT, "%.6f", 1.0 / rank), runName));
      }
    }
    return lines;
  }

  /** Reads the audit decisions, one JSON object per line. */
  public static List<Decision> readAudit(Path file) {
    try {
      var out = new ArrayList<Decision>();
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        if (!line.isBlank()) {
          out.add(JsonMapper.shared().readValue(line, Decision.class));
        }
      }
      return out;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the audit " + file, e);
    }
  }

  /** Writes lines with a trailing newline, as the scorers expect. */
  public static void write(List<String> lines, Path file) {
    try {
      Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
