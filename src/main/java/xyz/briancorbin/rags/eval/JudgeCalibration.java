package xyz.briancorbin.rags.eval;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.briancorbin.rags.eval.GoldenSet.Question;
import xyz.briancorbin.rags.eval.Judgement.Label;

/// Checks the judge against a person before any number from it is published.
///
/// Episode 3 graded all fifty answers by hand. Those grades are in `results/episode-3/labels.csv`,
/// and the answers they were given to are in the run file beside them. So the judge can be run over
/// the same fifty answers and asked how often it agrees. If it does not agree often enough, nothing
/// it says about a later run is worth printing.
///
/// Two numbers come out, and they are not the same thing. **Exact agreement** is how often the
/// judge picks the label the person picked. **Outcome agreement** is how often it agrees on the
/// only question an episode reports: was this answer right for a question of its kind. The second
/// is the one a headline rests on, and it is the more forgiving of the two, because a person
/// writing `partly` and a judge writing `WRONG` disagree on the label and agree that the answer was
/// not right.
public final class JudgeCalibration {

  private static final Logger log = LoggerFactory.getLogger(JudgeCalibration.class);

  /// Episode 3's hand grades, mapped onto the four the judge can return. `answered beside the
  /// point` and `accepted false premise` are both answers that do not answer, so both are `WRONG`;
  /// `abstained wrongly` and `declined in own words` are both refusals, and whether a refusal was
  /// right is the scorer's job rather than the label's.
  private static final Map<String, Label> BY_HAND =
      Map.of(
          "correct", Label.CORRECT,
          "partly", Label.PARTLY,
          "wrong", Label.WRONG,
          "answered beside the point", Label.WRONG,
          "accepted false premise", Label.WRONG,
          "abstained wrongly", Label.DECLINED,
          "declined in own words", Label.DECLINED);

  /// One answer, as a person graded it and as the judge graded it.
  public record Graded(
      String id, String stratum, boolean answerable, Label byHand, Judgement byJudge) {

    public boolean agrees() {
      return byHand == byJudge.label();
    }

    /// Whether both call this answer right for a question of its kind, or both call it not right.
    public boolean agreesOnOutcome() {
      boolean handRight = answerable ? byHand == Label.CORRECT : byHand == Label.DECLINED;
      return handRight == byJudge.isRightFor(answerable);
    }
  }

  private JudgeCalibration() {}

  /// The files a comparison reads and the one it writes.
  ///
  /// @param handGrades the person's grades, `id,stratum,label,…`
  /// @param recordedRun the golden run those grades were given to
  /// @param report where the comparison is written
  public record Comparison(Path handGrades, Path recordedRun, Path report) {}

  /// Grades Episode 3's recorded answers again and writes the comparison to `build/`.
  public static List<Graded> run(
      AnswerJudge judge, List<Question> questions, Comparison comparison) {
    Map<String, String> byHand = handGrades(comparison.handGrades());
    Map<String, String> answers = SavedRun.answersIn(comparison.recordedRun());
    var graded = new ArrayList<Graded>();
    for (Question question : questions) {
      String hand = byHand.get(question.id());
      if (hand != null) {
        graded.add(gradeOne(judge, question, hand, answers.getOrDefault(question.id(), "")));
      }
    }
    write(graded, comparison.report());
    return List.copyOf(graded);
  }

  /// One answer, as the person graded it and as the judge grades it now.
  private static Graded gradeOne(AnswerJudge judge, Question question, String hand, String answer) {
    Label expected = BY_HAND.get(hand.toLowerCase(Locale.ROOT));
    if (expected == null) {
      throw new IllegalStateException("Unknown hand grade for " + question.id() + ": " + hand);
    }
    Judgement judgement = judge.on(question, answer);
    log.info(
        "  {} {} by hand, {} by judge{}",
        question.id(),
        expected,
        judgement.label(),
        judgement.label() == expected ? "" : "  <- disagree");
    return new Graded(
        question.id(), question.stratum(), question.answerable(), expected, judgement);
  }

  /// `id,stratum,label,…`, with the label in the third column.
  private static Map<String, String> handGrades(Path labels) {
    try {
      var out = new LinkedHashMap<String, String>();
      List<String> lines = Files.readAllLines(labels, StandardCharsets.UTF_8);
      for (String line : lines.subList(1, lines.size())) {
        if (line.isBlank()) {
          continue;
        }
        // Only the first three fields are read, and none of them is ever quoted, so a split on the
        // first two commas is enough: the quoted reason and note come after.
        String[] field = line.split(",", 4);
        out.put(field[0].strip(), field[2].strip());
      }
      return out;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the hand grades " + labels, e);
    }
  }

  private static void write(List<Graded> graded, Path out) {
    long exact = graded.stream().filter(Graded::agrees).count();
    long outcome = graded.stream().filter(Graded::agreesOnOutcome).count();
    try {
      Files.createDirectories(out.getParent());
      Files.writeString(
          out,
          agreement(exact, outcome, graded.size()) + disagreements(graded),
          StandardCharsets.UTF_8);
      log.info(
          "Judge against a person: exact {}/{}, outcome {}/{} -> {}",
          exact,
          graded.size(),
          outcome,
          graded.size(),
          out);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write " + out, e);
    }
  }

  private static String agreement(long exact, long outcome, int of) {
    return """
        # The judge against a person

        Episode 3's answers, graded by hand and graded again by the judge.

        | | agreement |
        |---|---|
        | exact label | %d/%d |
        | was it right for its kind | %d/%d |

        """
        .formatted(exact, of, outcome, of);
  }

  private static String disagreements(List<Graded> graded) {
    var table =
        new StringBuilder("## Where they disagree\n\n")
            .append("| question | kind | by hand | by judge | the judge's reason |\n")
            .append("|---|---|---|---|---|\n");
    for (Graded row : graded) {
      if (!row.agrees()) {
        table.append(
            "| %s | %s | %s | %s | %s |\n"
                .formatted(
                    row.id(),
                    row.stratum(),
                    row.byHand(),
                    row.byJudge().label(),
                    row.byJudge().reason()));
      }
    }
    return table.toString();
  }
}
