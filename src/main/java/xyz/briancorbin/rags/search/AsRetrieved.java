package xyz.briancorbin.rags.search;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.join.DocumentJoiner;

/// A {@link DocumentJoiner} that keeps the order the retriever returned.
///
/// `RetrievalAugmentationAdvisor` joins before it post-processes, and it does so on every call.
/// There is no query expander here, so there is only ever one list to join, and joining it should
/// be a no-op.
///
/// Spring AI's default joiner is not a no-op: `ConcatenationDocumentJoiner` re-sorts by score
/// descending, treating a null score as `0.0`. That would undo two of this pipeline's steps at
/// once.
///
/// The scores reaching it come from BM25, from rank fusion and from a cross-encoder. They are not
/// on one scale and must not be compared. And a unit the identifier lookup pinned carries no score
/// at all, deliberately, because it was looked up rather than ranked. So the default sends the one
/// result we are surest of to the bottom of the list.
///
/// Fusion has already happened, inside {@link ElementRetriever}. By the time a list arrives here,
/// its order is the answer.
public final class AsRetrieved implements DocumentJoiner {

  @Override
  public List<Document> join(Map<Query, List<List<Document>>> documentsForQuery) {
    var out = new LinkedHashMap<String, Document>();
    // One query, one list, in practice. Should a query expander ever be added, this concatenates
    // rather than fuses, and `ReciprocalRankFusion` is what should be here instead.
    for (List<List<Document>> lists : documentsForQuery.values()) {
      for (List<Document> documents : lists) {
        for (Document document : documents) {
          out.putIfAbsent(document.getId(), document);
        }
      }
    }
    return List.copyOf(out.values());
  }
}
