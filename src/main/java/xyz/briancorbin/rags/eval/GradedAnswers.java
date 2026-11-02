package xyz.briancorbin.rags.eval;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.briancorbin.rags.eval.GoldenSet.Question;
import xyz.briancorbin.rags.eval.Judgement.Label;

/// Grades the answers of a saved golden run, so an episode can say whether better retrieval became
/// better answers.
///
/// It reads a run file rather than answering again, which means a run already on disk can be graded
/// again when the judge changes without spending the models a second time. The same property makes
/// `unsupported@4` worth having, and for the same reason: a measurement you can recompute over old
/// runs is one you can change your mind about honestly.
///
/// Read {@link JudgeCalibration} before reading any number from here. The judge agrees with the
/// person who graded Episode 3 on 42 of 47 labels and on 44 of 47 outcomes, and that figure is
/// in-sample: the judge's prompt was corrected against those same 47 answers. There is no held-out
/// set, because 47 hand-graded answers is all there is.
public final class GradedAnswers {

  private static final Logger log = LoggerFactory.getLogger(GradedAnswers.class);

  /// One graded answer of a run.
  public record Row(String id, String stratum, boolean answerable, Judgement judgement) {

    public boolean isRight() {
      return judgement.isRightFor(answerable);
    }
  }

  private GradedAnswers() {}

  /// Grades every answer in `run` and writes the summary beside it, as `<run>-graded.md`, so a
  /// grading is never separated from the run it graded.
  public static List<Row> of(AnswerJudge judge, List<Question> questions, Path run) {
    Path out = besideTheRun(run);
    Map<String, String> answers = SavedRun.answersIn(run);
    var rows = new ArrayList<Row>();
    for (Question question : questions) {
      if (!answers.containsKey(question.id())) {
        continue;
      }
      Judgement judgement = judge.on(question, answers.get(question.id()));
      rows.add(new Row(question.id(), question.stratum(), question.answerable(), judgement));
      log.info(
          "  {} {}{}",
          question.id(),
          judgement.label(),
          rows.getLast().isRight() ? "" : "  <- not right");
    }
    write(rows, out);
    return List.copyOf(rows);
  }

  /// `build/episode-3-qwen3_8b-duo-k4.jsonl` becomes `build/episode-3-qwen3_8b-duo-k4-graded.md`.
  private static Path besideTheRun(Path run) {
    String name = run.getFileName().toString();
    int dot = name.lastIndexOf('.');
    return run.resolveSibling((dot < 0 ? name : name.substring(0, dot)) + "-graded.md");
  }

  private static void write(List<Row> rows, Path out) {
    String report = summary(rows) + byKind(rows) + everyAnswer(rows);
    try {
      Files.createDirectories(out.getParent());
      Files.writeString(out, report, StandardCharsets.UTF_8);
      log.info("Answers graded -> {}", out);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write " + out, e);
    }
  }

  private static String summary(List<Row> rows) {
    long right = rows.stream().filter(Row::isRight).count();
    long answerable = rows.stream().filter(Row::answerable).count();
    return """
        # The answers, graded

        Graded by the judge, which agrees with a person on 44 of 47 outcomes \u2014 in-sample; see \
        `judge-calibration.md`.

        | | count |
        |---|---|
        | right for its kind | %d/%d |
        | stated the answer | %d/%d |
        | declined | %d |

        """
        .formatted(
            right,
            rows.size(),
            count(rows, Label.CORRECT),
            answerable,
            count(rows, Label.DECLINED));
  }

  private static String byKind(List<Row> rows) {
    var byStratum = new LinkedHashMap<String, List<Row>>();
    for (Row row : rows) {
      byStratum.computeIfAbsent(row.stratum(), stratum -> new ArrayList<>()).add(row);
    }
    var table = new StringBuilder("## By kind\n\n");
    table.append(
        "| kind | right | correct | partly | wrong | declined |\n|---|---|---|---|---|---|\n");
    byStratum.forEach(
        (stratum, kind) ->
            table.append(
                "| %s | %d/%d | %d | %d | %d | %d |\n"
                    .formatted(
                        stratum,
                        kind.stream().filter(Row::isRight).count(),
                        kind.size(),
                        count(kind, Label.CORRECT),
                        count(kind, Label.PARTLY),
                        count(kind, Label.WRONG),
                        count(kind, Label.DECLINED))));
    return table.append('\n').toString();
  }

  private static String everyAnswer(List<Row> rows) {
    var table = new StringBuilder("## Every answer\n\n");
    table.append("| question | kind | grade | the judge's reason |\n|---|---|---|---|\n");
    for (Row row : rows) {
      table.append(
          "| %s | %s | %s | %s |\n"
              .formatted(
                  row.id(), row.stratum(), row.judgement().label(), row.judgement().reason()));
    }
    return table.toString();
  }

  private static long count(List<Row> rows, Label label) {
    return rows.stream().filter(row -> row.judgement().label() == label).count();
  }
}
