package xyz.briancorbin.rags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

class GoldenSetTest {

  @Test
  void readsAcceptedPassages() {
    var questions =
        GoldenSet.read(
            new ByteArrayResource(
                """
                {"id":"G-001","question":"Q","stratum":"relation","expected":["a#1","b#2"],"accept":[{"address":"c#3","for":"b#2"},{"address":"d#4","for":"question"}]}
                """
                    .getBytes(StandardCharsets.UTF_8)));
    var accept = questions.get(0).accept();
    assertEquals(2, accept.size());
    assertEquals("b#2", accept.get(0).forTarget());
    assertFalse(accept.get(0).wholeQuestion());
    assertTrue(accept.get(1).wholeQuestion());
  }

  @Test
  void readsOneQuestionPerLineAndSkipsCommentsAndBlanks() {
    String jsonl =
        """
        // a comment
        {"id":"G-001","question":"Why no index?","stratum":"paraphrase","expected":["specs/059/spec.html#FR-001"],"answer":"Below the threshold.","commit":"c64d41c","note":"ignored"}

        {"id":"G-002","question":"Which database?","stratum":"unanswerable"}
        """;
    var questions = GoldenSet.read(new ByteArrayResource(jsonl.getBytes(StandardCharsets.UTF_8)));
    assertEquals(2, questions.size());
    assertEquals("candidate", questions.get(0).status());
    assertTrue(questions.get(1).expected().isEmpty());
    assertFalse(questions.get(1).answerable());
  }

  /**
   * The shipped set is well formed: unique ids, known strata, and sources where there should be.
   */
  @Test
  void theShippedSetIsWellFormed() {
    var questions = GoldenSet.read(new ClassPathResource("golden/questions.jsonl"));
    var ids = new HashSet<String>();
    for (var q : questions) {
      assertTrue(ids.add(q.id()), "duplicate id " + q.id());
      assertTrue(GoldenSet.STRATA.contains(q.stratum()), q.id() + " has stratum " + q.stratum());
      assertEquals(q.answerable(), !q.expected().isEmpty(), q.id() + " expected addresses");
      if ("stale".equals(q.stratum())) {
        assertFalse(q.stale().isEmpty(), q.id() + " has no stale addresses");
      }
      assertEquals("accepted", q.status(), q.id() + " is not signed off");
      for (var a : q.accept()) {
        assertTrue(
            a.wholeQuestion() || q.expected().contains(a.forTarget()),
            q.id() + ": accepted " + a.address() + " stands in for " + a.forTarget());
      }
    }
  }

  /**
   * With a spectastic checkout next to this repository, every address must name a real element.
   * Without one the test has nothing to check against and passes.
   */
  @Test
  void everyAddressResolvesInTheCheckout() {
    Path checkout = Path.of("../spectastic");
    if (!Files.isDirectory(checkout.resolve("specs"))) {
      return;
    }
    var scorer = new GoldenScorer(checkout);
    for (var q : GoldenSet.read(new ClassPathResource("golden/questions.jsonl"))) {
      for (String a : q.expected()) {
        assertTrue(scorer.elementText(a).isPresent(), q.id() + ": no element at " + a);
      }
      for (String a : q.stale()) {
        assertTrue(scorer.elementText(a).isPresent(), q.id() + ": no element at " + a);
      }
      for (var a : q.accept()) {
        assertTrue(scorer.elementText(a.address()).isPresent(), q.id() + ": no element at " + a);
      }
    }
  }
}
