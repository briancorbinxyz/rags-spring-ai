package xyz.briancorbin.rags;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

/**
 * Episode 4's search, as a Spring AI {@link DocumentRetriever}, built up one step at a time:
 *
 * <pre>{@code
 * var step1 = ElementRetriever.keywords(index);   // element units, BM25
 * var step2 = step1.withIdentifiers(ids);          // the units a question names, pinned first
 * var step3 = step2.currentOnly();                 // withdrawn, superseded, rejected left out
 * }</pre>
 *
 * Each step is the one before it plus one thing, so the ratchet compares like with like. Step 4 is
 * a {@link SupersessionRedirect} after Step 3.
 */
public final class ElementRetriever implements DocumentRetriever {

  /** How many units a search returns. The golden run scores the first four. */
  public static final int DEPTH = 50;

  private final KeywordIndex index;
  private final Function<String, List<String>> named; // question -> addresses it names
  private final boolean currentOnly;

  private ElementRetriever(KeywordIndex index, Function<String, List<String>> named, boolean currentOnly) {
    this.index = index;
    this.named = named;
    this.currentOnly = currentOnly;
  }

  /** Step 1: keyword search over the element units. */
  public static ElementRetriever keywords(KeywordIndex index) {
    return new ElementRetriever(index, question -> List.of(), false);
  }

  /** Step 2: the units a question names by id and spec go first. */
  public ElementRetriever withIdentifiers(IdentifierLookup ids) {
    return new ElementRetriever(index, ids::lookup, currentOnly);
  }

  /** Step 3: leave out withdrawn, superseded and rejected units, named or searched. */
  public ElementRetriever currentOnly() {
    return new ElementRetriever(index, named, true);
  }

  @Override
  public List<Document> retrieve(Query query) {
    var results = new LinkedHashMap<String, Document>(); // by id, in order, each once
    pinned(query.text()).forEach(unit -> results.putIfAbsent(unit.getId(), unit));
    index.search(query.text(), DEPTH, currentOnly).forEach(hit -> results.putIfAbsent(hit.getId(), hit));
    return results.values().stream().limit(DEPTH).toList();
  }

  /** Every part of every unit the question names, in the lookup's order. */
  private Stream<Document> pinned(String question) {
    return named.apply(question).stream()
        .flatMap(address -> index.parts(address).stream())
        .filter(part -> !currentOnly || !ElementReader.lifecycle(part).excluded());
  }
}
