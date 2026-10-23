package xyz.briancorbin.rags.search;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

/// Post-processors in a row, each given what the one before it returned.
///
/// Spring AI's `DocumentPostProcessor` is a single step, and its advisor takes a list, but there is
/// no composite that makes several of them one. A step of the main line is one search and one thing
/// after it, so here is the composite.
public record PostProcessors(List<DocumentPostProcessor> steps) implements DocumentPostProcessor {

  public PostProcessors {
    steps = List.copyOf(steps);
  }

  public static DocumentPostProcessor of(DocumentPostProcessor... steps) {
    return new PostProcessors(List.of(steps));
  }

  @Override
  public List<Document> process(Query query, List<Document> documents) {
    List<Document> out = documents;
    for (DocumentPostProcessor step : steps) {
      out = step.process(query, out);
    }
    return out;
  }
}
