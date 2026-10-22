package xyz.briancorbin.rags;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.rag.Query;

class IdentifierLookupTest {

  @Test
  void anIdWithASpecNamesThatIdInThatSpec(@TempDir Path dir) throws Exception {
    try (var index = new KeywordIndex(new ElementReader(Archives.tiny(dir)).get())) {
      var ids = new IdentifierLookup(index);
      assertEquals(List.of("specs/002-other/spec.html#FR-001"), ids.lookup("What does FR-001 in spec 002 require?"));
      assertEquals(List.of("specs/002-other/spec.html#FR-001"), ids.lookup("What does Spec 002's FR-001 require?"));
      assertEquals(List.of("specs/001-demo/spec.html#FR-001"), ids.lookup("FR-001 of 001-demo"));
    }
  }

  @Test
  void anIdInMoreThanOneSpecNamesNothingWithoutASpec(@TempDir Path dir) throws Exception {
    try (var index = new KeywordIndex(new ElementReader(Archives.tiny(dir)).get())) {
      assertEquals(List.of(), new IdentifierLookup(index).lookup("What does FR-001 require?"));
    }
  }

  @Test
  void anIdInOneSpecNamesEveryPageOfItCurrentFirst(@TempDir Path dir) throws Exception {
    try (var index = new KeywordIndex(new ElementReader(Archives.tiny(dir)).get())) {
      assertEquals(
          List.of(
              "specs/001-demo/spec.html#FR-003",
              "specs/001-demo/changes/archive/2026-01-01-log-to-stdout/proposal.html#FR-003"),
          new IdentifierLookup(index).lookup("What does FR-003 require?"));
      assertEquals(List.of(), new IdentifierLookup(index).lookup("what does fr-003 require?"));
    }
  }

  @Test
  void aNamedUnitIsPinnedAboveTheSearch(@TempDir Path dir) throws Exception {
    try (var index = new KeywordIndex(new ElementReader(Archives.tiny(dir)).get())) {
      var retriever =
          ElementRetriever.builder()
              .index(index)
              .identifiers(new IdentifierLookup(index))
              .filter(Lifecycle.CURRENT_ONLY)
              .build();
      var found = retriever.retrieve(new Query("Does the other service in spec 002 log requests, per FR-001?"));
      assertEquals("specs/002-other/spec.html#FR-001", ElementReader.address(found.getFirst()));
      assertEquals(null, found.getFirst().getScore()); // pinned, not scored
    }
  }
}
