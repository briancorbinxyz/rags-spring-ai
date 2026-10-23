package xyz.briancorbin.rags.search;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

/// Cuts a ranking to its first `k`.
///
/// A step's own depth, as a post-processor, so the cut can be placed where it belongs: after any
/// reranking, and after the redirect, which can hand back more than it was given when it keeps an
/// applied change behind its successor.
public record TopDocuments(int k) implements DocumentPostProcessor {

  @Override
  public List<Document> process(Query query, List<Document> documents) {
    return documents.size() <= k ? documents : List.copyOf(documents.subList(0, k));
  }
}
