package xyz.briancorbin.rags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.rag.Query;

class KeywordSearchTest {

  @Test
  void theQueryIsEveryWordThatIsntAStopWordQuotedAndOred() {
    assertEquals(
        "\"FR-012\" OR \"p-map\"",
        KeywordSearch.matchQuery("What does FR-012 say about the p-map?"));
    assertEquals("", KeywordSearch.matchQuery("What is it?"));
  }

  @Test
  void theIdColumnOutweighsTheText(@TempDir Path dir) throws Exception {
    try (var keywords =
        KeywordSearch.of(new ElementUnits(new SpectasticReader(Archives.tiny(dir)).get()))) {
      var top = keywords.search("FR-002", 10, Lifecycle.ALL);
      assertEquals("specs/001-demo/spec.html#FR-002", Units.address(top.getFirst()));
      assertTrue(top.getFirst().getScore() > 0);
    }
  }

  @Test
  void theLifecycleFilterRunsBeforeTheCut(@TempDir Path dir) throws Exception {
    try (var keywords =
        KeywordSearch.of(new ElementUnits(new SpectasticReader(Archives.tiny(dir)).get()))) {
      var all = keywords.search("log to a file", 1, Lifecycle.ALL);
      var current = keywords.search("log to a file", 1, Lifecycle.IN_FORCE);
      assertEquals("specs/001-demo/spec.html#FR-002", Units.address(all.getFirst()));
      assertEquals(1, current.size());
      assertTrue(current.stream().noneMatch(d -> Units.standing(d).outOfDate()));
    }
  }

  @Test
  void aSearchIsADocumentRetriever(@TempDir Path dir) throws Exception {
    try (var keywords =
        KeywordSearch.of(new ElementUnits(new SpectasticReader(Archives.tiny(dir)).get()))) {
      var retriever =
          ElementRetriever.builder().keywords(keywords).filter(Lifecycle.IN_FORCE).build();
      var found = retriever.retrieve(new Query("Which service must answer within a second?"));
      assertEquals("specs/002-other/spec.html#FR-001", Units.address(found.getFirst()));
    }
  }
}
