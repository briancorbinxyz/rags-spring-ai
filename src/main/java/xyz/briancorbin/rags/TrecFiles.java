package xyz.briancorbin.rags;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.Set;
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
    questions.stream()
        .filter(Question::answerable)
        .sorted(Comparator.comparing(Question::id))
        .forEach(
            q -> {
              var judged = new LinkedHashMap<String, Integer>();
              q.expected().forEach(e -> judged.putIfAbsent(e, 1));
              q.accept().forEach(a -> judged.putIfAbsent(a.address(), 1));
              q.stale().forEach(s -> judged.putIfAbsent(s, 0));
              audit.stream()
                  .filter(d -> d.id().equals(q.id()))
                  .sorted(Comparator.comparing(Decision::candidate))
                  .forEach(d -> judged.putIfAbsent(d.candidate(), d.accepted() ? 1 : 0));
              judged.forEach((p, rel) -> lines.add(q.id() + " 0 " + p + " " + rel));
            });
    return lines;
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
