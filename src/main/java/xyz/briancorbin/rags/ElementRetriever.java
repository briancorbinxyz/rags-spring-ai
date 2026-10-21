package xyz.briancorbin.rags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

/**
 * Episode 4's search, as a Spring AI {@link DocumentRetriever}: keyword search over element units
 * (Step 1), with the units a question names pinned first (Step 2), and out-of-date records left
 * out (Step 3). Step 4 is a {@link SupersessionRedirect} after it.
 *
 * <p>Pinned units come first, in the lookup's order, every part of each; then the keyword results
 * that aren't already there. At most {@value #DEPTH} units come back.
 */
public final class ElementRetriever implements DocumentRetriever {

  /** How many units a search returns. The golden run scores the first four. */
  public static final int DEPTH = 50;

  private final KeywordIndex index;
  private final IdentifierLookup ids; // null: no identifier lookup
  private final boolean currentOnly;

  /**
   * @param ids the identifier lookup, or null to search without one (Step 1)
   * @param currentOnly leave out withdrawn, superseded and rejected units (Step 3)
   */
  public ElementRetriever(KeywordIndex index, IdentifierLookup ids, boolean currentOnly) {
    this.index = index;
    this.ids = ids;
    this.currentOnly = currentOnly;
  }

  @Override
  public List<Document> retrieve(Query query) {
    String question = query.text();
    List<Document> pinned = new ArrayList<>();
    if (ids != null) {
      for (String address : ids.lookup(question)) {
        index.parts(address).stream()
            .filter(part -> !currentOnly || !ElementReader.lifecycle(part).excluded())
            .filter(part -> !pinned.contains(part))
            .forEach(pinned::add);
      }
    }
    Set<String> seen = new HashSet<>();
    pinned.forEach(p -> seen.add(p.getId()));
    List<Document> results = new ArrayList<>(pinned);
    for (Document hit : index.search(question, DEPTH, currentOnly)) {
      if (results.size() == DEPTH) {
        break;
      }
      if (seen.add(hit.getId())) {
        results.add(hit);
      }
    }
    return List.copyOf(results.subList(0, Math.min(DEPTH, results.size())));
  }
}
