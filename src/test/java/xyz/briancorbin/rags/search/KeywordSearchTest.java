package xyz.briancorbin.rags.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.rag.Query;
import xyz.briancorbin.rags.spectastic.Archives;
import xyz.briancorbin.rags.spectastic.Lifecycle;
import xyz.briancorbin.rags.spectastic.SpectasticReader;
import xyz.briancorbin.rags.units.ElementUnits;
import xyz.briancorbin.rags.units.StandingFilter;
import xyz.briancorbin.rags.units.Units;

class KeywordSearchTest {

  @Test
  void theQueryIsEveryWordThatIsntAStopWordQuotedAndOred() {
    assertEquals(
        "\"FR-012\" OR \"p-map\"",
        KeywordSearch.matchQuery(
            "What does FR-012 say about the p-map?",
            StopWords.of("stopwords/english.txt", "stopwords/spectastic.txt")));
    assertEquals(
        "",
        KeywordSearch.matchQuery(
            "What is it?", StopWords.of("stopwords/english.txt", "stopwords/spectastic.txt")));
  }

  @Test
  void theIdColumnOutweighsTheText(@TempDir Path dir) throws Exception {
    try (var keywords =
        KeywordSearch.of(
            new ElementUnits(new SpectasticReader(Archives.tiny(dir)).get(), Lifecycle::of),
            StopWords.of("stopwords/english.txt", "stopwords/spectastic.txt"))) {
      var top = keywords.search("FR-002", 10, StandingFilter.all());
      assertEquals("specs/001-demo/spec.html#FR-002", Units.address(top.getFirst()));
      assertTrue(top.getFirst().getScore() > 0);
    }
  }

  @Test
  void theLifecycleFilterRunsBeforeTheCut(@TempDir Path dir) throws Exception {
    try (var keywords =
        KeywordSearch.of(
            new ElementUnits(new SpectasticReader(Archives.tiny(dir)).get(), Lifecycle::of),
            StopWords.of("stopwords/english.txt", "stopwords/spectastic.txt"))) {
      var all = keywords.search("log to a file", 1, StandingFilter.all());
      var current = keywords.search("log to a file", 1, StandingFilter.of(Lifecycle.IN_FORCE));
      assertEquals("specs/001-demo/spec.html#FR-002", Units.address(all.getFirst()));
      assertEquals(1, current.size());
      assertTrue(current.stream().noneMatch(d -> keywords.units().standing(d).outOfDate()));
    }
  }

  @Test
  void aSearchIsADocumentRetriever(@TempDir Path dir) throws Exception {
    try (var keywords =
        KeywordSearch.of(
            new ElementUnits(new SpectasticReader(Archives.tiny(dir)).get(), Lifecycle::of),
            StopWords.of("stopwords/english.txt", "stopwords/spectastic.txt"))) {
      var retriever =
          ElementRetriever.builder()
              .keywords(keywords)
              .filter(StandingFilter.of(Lifecycle.IN_FORCE))
              .build();
      var found = retriever.retrieve(new Query("Which service must answer within a second?"));
      assertEquals("specs/002-other/spec.html#FR-001", Units.address(found.getFirst()));
    }
  }
}
