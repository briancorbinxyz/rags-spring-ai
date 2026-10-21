package xyz.briancorbin.rags;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.rag.Query;

class SupersessionRedirectTest {

  private static final String SPEC = "specs/001-demo/spec.html";
  private static final String CHANGE = "specs/001-demo/changes/archive/2026-01-01-log-to-stdout/proposal.html";

  @Test
  void aSupersededResultGivesItsPlaceToItsSuccessorAndAnAppliedChangeStaysBehindIt(@TempDir Path dir)
      throws Exception {
    try (var index = new KeywordIndex(new ElementReader(Archives.tiny(dir)).get())) {
      var redirect = new SupersessionRedirect(index);
      var in =
          List.of(
              index.parts(SPEC + "#FR-002").getFirst(),
              index.parts(CHANGE + "#FR-003").getFirst(),
              index.parts(SPEC + "#FR-001").getFirst(),
              index.parts(SPEC + "#D-001").getFirst());
      var out = redirect.process(new Query("Where does the service log?"), in);
      assertEquals(
          List.of(SPEC + "#FR-003", CHANGE + "#FR-003", SPEC + "#FR-001", SPEC + "#D-002"),
          out.stream().map(ElementReader::address).toList());
      assertEquals(SPEC + "#FR-002", out.getFirst().getMetadata().get("redirected_from"));
    }
  }
}
