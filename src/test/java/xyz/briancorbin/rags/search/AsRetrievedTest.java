package xyz.briancorbin.rags.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;

/// The joiner keeps the order it was given, and the stock one does not.
///
/// This is a regression guard for a bug that would not have shown up as a failure anywhere: wiring
/// the element search behind Spring AI's advisor with its default joiner silently sinks every
/// pinned unit, because a pinned unit carries no score. Hit@4 would simply have got worse, with
/// nothing to say why.
class AsRetrievedTest {

  private static final Query ASKED = new Query("what does FR-003 require?");

  @Test
  void aPinnedUnitWithNoScoreStaysWhereTheRetrieverPutIt() {
    var joined = new AsRetrieved().join(oneListOf(pinned("FR-003"), ranked("other", 0.91)));

    assertEquals(List.of("FR-003", "other"), idsOf(joined));
  }

  /// Not a complaint about Spring AI: the default is right for joining several expanded queries,
  /// which is what it is for. It is wrong here, and this says so in the one place a reader would
  /// check.
  @Test
  void whereasTheStockJoinerSendsItToTheBottom() {
    var joined =
        new ConcatenationDocumentJoiner().join(oneListOf(pinned("FR-003"), ranked("other", 0.91)));

    assertEquals(List.of("other", "FR-003"), idsOf(joined));
  }

  @Test
  void andEachDocumentComesBackOnce() {
    var joined = new AsRetrieved().join(oneListOf(ranked("same", 0.5), ranked("same", 0.5)));

    assertEquals(List.of("same"), idsOf(joined));
  }

  private static Map<Query, List<List<Document>>> oneListOf(Document... documents) {
    return Map.of(ASKED, List.of(List.of(documents)));
  }

  private static Document pinned(String id) {
    return Document.builder()
        .id(id)
        .text("...")
        .metadata(Map.of(ElementRetriever.PINNED, true))
        .build();
  }

  private static Document ranked(String id, double score) {
    return Document.builder().id(id).text("...").score(score).build();
  }

  private static List<String> idsOf(List<Document> documents) {
    return documents.stream().map(Document::getId).toList();
  }
}
