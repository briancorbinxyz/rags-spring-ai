package xyz.briancorbin.rags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.rag.Query;

class KeywordIndexTest {

  @Test
  void theQueryIsEveryWordThatIsntAStopWordQuotedAndOred() {
    assertEquals("\"FR-012\" OR \"p-map\"", KeywordIndex.matchQuery("What does FR-012 say about the p-map?"));
    assertEquals("", KeywordIndex.matchQuery("What is it?"));
  }

  @Test
  void theIdColumnOutweighsTheText(@TempDir Path dir) throws Exception {
    try (var index = new KeywordIndex(new ElementReader(Archives.tiny(dir)).get())) {
      var top = index.search("FR-002", 10, false);
      assertEquals("specs/001-demo/spec.html#FR-002", ElementReader.address(top.getFirst()));
      assertTrue(top.getFirst().getScore() > 0);
    }
  }

  @Test
  void theLifecycleFilterRunsBeforeTheCut(@TempDir Path dir) throws Exception {
    try (var index = new KeywordIndex(new ElementReader(Archives.tiny(dir)).get())) {
      var all = index.search("log to a file", 1, false);
      var current = index.search("log to a file", 1, true);
      assertEquals("specs/001-demo/spec.html#FR-002", ElementReader.address(all.getFirst()));
      assertEquals(1, current.size());
      assertTrue(current.stream().noneMatch(d -> ElementReader.lifecycle(d).excluded()));
    }
  }

  @Test
  void aSearchIsADocumentRetriever(@TempDir Path dir) throws Exception {
    try (var index = new KeywordIndex(new ElementReader(Archives.tiny(dir)).get())) {
      var retriever = ElementRetriever.keywords(index).currentOnly();
      var found = retriever.retrieve(new Query("Which service must answer within a second?"));
      assertEquals("specs/002-other/spec.html#FR-001", ElementReader.address(found.getFirst()));
    }
  }
}
