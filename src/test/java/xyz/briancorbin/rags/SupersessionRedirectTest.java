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
  void aSupersededResultGivesItsPlaceToItsSuccessorAndIsItselfDropped(@TempDir Path dir)
      throws Exception {
    try (var keywords = KeywordSearch.of(new ElementUnits(new ElementReader(Archives.tiny(dir)).get()))) {
      var redirect = new SupersessionRedirect(keywords);
      var in =
          List.of(
              keywords.units().parts(SPEC + "#FR-002").getFirst(),
              keywords.units().parts(CHANGE + "#FR-003").getFirst(),
              keywords.units().parts(SPEC + "#FR-001").getFirst(),
              keywords.units().parts(SPEC + "#D-001").getFirst());
      var out = redirect.process(new Query("Where does the service log?"), in);
      // Two of the four results lead to FR-003 -- the superseded requirement and the applied change
      // that rewrote it -- so four results come back as three.
      assertEquals(
          List.of(SPEC + "#FR-003", SPEC + "#FR-001", SPEC + "#D-002"),
          out.stream().map(ElementReader::address).toList());
      assertEquals(SPEC + "#FR-002", out.getFirst().getMetadata().get("redirected_from"));
    }
  }
}
