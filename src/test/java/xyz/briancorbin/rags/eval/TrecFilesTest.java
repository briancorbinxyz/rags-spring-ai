package xyz.briancorbin.rags.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import xyz.briancorbin.rags.eval.BlindAudit.Decision;
import xyz.briancorbin.rags.eval.GoldenSet.Accept;
import xyz.briancorbin.rags.eval.GoldenSet.Question;

class TrecFilesTest {

  private static Question q(
      String id, String stratum, List<String> expected, List<Accept> accept, List<String> stale) {
    return new Question(id, "?", stratum, expected, accept, stale, "", "c64d41c", "accepted");
  }

  @Test
  void qrelsJudgeEachPassageOnceInAFixedOrder() {
    var questions = threeQuestionsOutOfOrder();
    var audit = anAuditThatAlsoNamesAPassageTheQuestionCallsStale();

    assertEquals(
        List.of(
            "G-001 0 c#1 1",
            "G-001 0 d#2 1",
            "G-001 0 e#3 1",
            "G-001 0 f#4 0",
            "G-001 0 z#9 0",
            "G-002 0 a#1 1",
            "G-002 0 b#2 1",
            "G-002 0 a#old 0"),
        TrecFiles.qrels(questions, audit));
  }

  /// Given out of order on purpose: a qrels file is sorted by id so it is byte-for-byte repeatable.
  private static List<Question> threeQuestionsOutOfOrder() {
    return List.of(
        q(
            "G-002",
            "stale",
            List.of("a#1"),
            List.of(new Accept("b#2", "question")),
            List.of("a#old")),
        q("G-001", "relation", List.of("c#1", "d#2"), List.of(new Accept("e#3", "d#2")), List.of()),
        q("G-009", "unanswerable", List.of(), List.of(), List.of()));
  }

  /// The last decision is on a passage G-002 already calls stale: the first rule to name a passage
  /// wins, so the audit does not get to overrule the question.
  private static List<Decision> anAuditThatAlsoNamesAPassageTheQuestionCallsStale() {
    return List.of(
        new Decision("G-001", "z#9", "no", null),
        new Decision("G-001", "e#3", "standin", "a#1"),
        new Decision("G-001", "f#4", "partly", null),
        new Decision("G-002", "a#old", "whole", null));
  }

  @Test
  void runLinesKeepTheFirstOccurrenceOfASplitPassage() {
    var lines =
        TrecFiles.run("G-044", List.of("p#1", "p#2", "p#1", "p#3"), new TrecFiles.Run("S1", 50));
    assertEquals(
        List.of(
            "G-044 Q0 p#1 1 1.000000 S1",
            "G-044 Q0 p#2 2 0.500000 S1",
            "G-044 Q0 p#3 3 0.333333 S1"),
        lines);
  }

  @Test
  void runLinesStopAtDepth() {
    var ranked = java.util.stream.IntStream.range(0, 100).mapToObj(i -> "p#" + i).toList();
    assertEquals(50, TrecFiles.run("q", ranked, new TrecFiles.Run("x", 50)).size());
  }

  /// The shipped qrels are exactly what the golden set and the audit say. If this fails, regenerate
  /// `qrels.txt` (see the golden set's README) rather than editing it by hand.
  @Test
  void theShippedQrelsMatchTheGoldenSetAndTheAudit() throws IOException {
    var questions = GoldenSet.read(new ClassPathResource("golden/questions.jsonl"));
    var audit = BlindAudit.read(BlindAudit.ROUNDS);
    var shipped =
        new String(
            new ClassPathResource("golden/qrels.txt").getInputStream().readAllBytes(),
            StandardCharsets.UTF_8);
    assertEquals(String.join("\n", TrecFiles.qrels(questions, audit)) + "\n", shipped);
  }
}
