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
    try (var keywords =
        KeywordSearch.of(new ElementUnits(new SpectasticReader(Archives.tiny(dir)).get()))) {
      var ids = new IdentifierLookup(keywords.units(), new SpecGrammar());
      assertEquals(
          List.of("specs/002-other/spec.html#FR-001"),
          ids.lookup("What does FR-001 in spec 002 require?"));
      assertEquals(
          List.of("specs/002-other/spec.html#FR-001"),
          ids.lookup("What does Spec 002's FR-001 require?"));
      assertEquals(List.of("specs/001-demo/spec.html#FR-001"), ids.lookup("FR-001 of 001-demo"));
    }
  }

  @Test
  void anIdInMoreThanOneSpecNamesNothingWithoutASpec(@TempDir Path dir) throws Exception {
    try (var keywords =
        KeywordSearch.of(new ElementUnits(new SpectasticReader(Archives.tiny(dir)).get()))) {
      assertEquals(
          List.of(),
          new IdentifierLookup(keywords.units(), new SpecGrammar())
              .lookup("What does FR-001 require?"));
    }
  }

  @Test
  void anIdInOneSpecNamesEveryPageOfItCurrentFirst(@TempDir Path dir) throws Exception {
    try (var keywords =
        KeywordSearch.of(new ElementUnits(new SpectasticReader(Archives.tiny(dir)).get()))) {
      assertEquals(
          List.of(
              "specs/001-demo/spec.html#FR-003",
              "specs/001-demo/changes/archive/2026-01-01-log-to-stdout/proposal.html#FR-003"),
          new IdentifierLookup(keywords.units(), new SpecGrammar())
              .lookup("What does FR-003 require?"));
      assertEquals(
          List.of(),
          new IdentifierLookup(keywords.units(), new SpecGrammar())
              .lookup("what does fr-003 require?"));
    }
  }

  /**
   * The archive is three digits wide, and both the pattern and {@code specOf} used to say so. A
   * fixed three-digit read makes 1000 and 1001 the same spec, which turns "pin only if exactly one
   * spec has this id" into pinning both.
   */
  @Test
  void aSpecNumberedPastNineNinetyNineIsStillOneSpec(@TempDir Path dir) throws Exception {
    try (var keywords =
        KeywordSearch.of(
            new ElementUnits(new SpectasticReader(Archives.pastNineNinetyNine(dir)).get()))) {
      var ids = new IdentifierLookup(keywords.units(), new SpecGrammar());
      assertEquals(
          List.of("specs/1000-alpha/spec.html#FR-009"),
          ids.lookup("What does FR-009 in spec 1000 require?"));
      assertEquals(List.of("specs/1001-beta/spec.html#FR-009"), ids.lookup("FR-009 of 1001-beta"));
      assertEquals(
          List.of(),
          ids.lookup("What does FR-009 require?"),
          "two specs have it, so neither is named");
    }
  }

  @Test
  void aNamedUnitIsPinnedAboveTheSearch(@TempDir Path dir) throws Exception {
    try (var keywords =
        KeywordSearch.of(new ElementUnits(new SpectasticReader(Archives.tiny(dir)).get()))) {
      var retriever =
          ElementRetriever.builder()
              .keywords(keywords)
              .identifiers(new IdentifierLookup(keywords.units(), new SpecGrammar()))
              .filter(Lifecycle.IN_FORCE)
              .build();
      var found =
          retriever.retrieve(
              new Query("Does the other service in spec 002 log requests, per FR-001?"));
      assertEquals("specs/002-other/spec.html#FR-001", Units.address(found.getFirst()));
      assertEquals(null, found.getFirst().getScore()); // pinned, not scored
    }
  }
}
