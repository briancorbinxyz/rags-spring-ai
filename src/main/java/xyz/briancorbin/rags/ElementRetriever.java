package xyz.briancorbin.rags;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

/**
 * Episode 4's search, as a Spring AI {@link DocumentRetriever}: keyword search over element units
 * (Step 1), with the units a question names pinned first (Step 2), and out-of-date units filtered
 * out (Step 3). Step 4 is a {@link SupersessionRedirect} after it. Built the way Spring AI's own
 * {@code VectorStoreDocumentRetriever} is:
 *
 * <pre>{@code
 * ElementRetriever.builder()
 *     .index(index)
 *     .identifiers(new IdentifierLookup(index))
 *     .filter(Lifecycle.CURRENT_ONLY)
 *     .build();
 * }</pre>
 */
public final class ElementRetriever implements DocumentRetriever {

  /** How many units a search returns. The golden run scores the first four. */
  public static final int DEPTH = 50;

  private final KeywordIndex index;
  private final IdentifierLookup identifiers; // null: none
  private final Set<Lifecycle> filter;

  private ElementRetriever(Builder b) {
    this.index = Objects.requireNonNull(b.index, "index");
    this.identifiers = b.identifiers;
    this.filter = b.filter;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public List<Document> retrieve(Query query) {
    var results = new LinkedHashMap<String, Document>(); // by id, in order, each once
    pinned(query.text()).forEach(unit -> results.putIfAbsent(unit.getId(), unit));
    index.search(query.text(), DEPTH, filter).forEach(hit -> results.putIfAbsent(hit.getId(), hit));
    return results.values().stream().limit(DEPTH).toList();
  }

  /** Every part of every unit the question names, in the lookup's order, that the filter allows. */
  private Stream<Document> pinned(String question) {
    if (identifiers == null) {
      return Stream.empty();
    }
    return identifiers.lookup(question).stream()
        .flatMap(address -> index.parts(address).stream())
        .filter(part -> filter.contains(ElementReader.lifecycle(part)));
  }

  public static final class Builder {
    private KeywordIndex index;
    private IdentifierLookup identifiers;
    private Set<Lifecycle> filter = Lifecycle.ALL;

    private Builder() {}

    /** The units to search. Required. */
    public Builder index(KeywordIndex index) {
      this.index = index;
      return this;
    }

    /** Pin the units a question names by id and spec above the search (Step 2). */
    public Builder identifiers(IdentifierLookup identifiers) {
      this.identifiers = identifiers;
      return this;
    }

    /** Which lifecycles may come back, named or searched; {@link Lifecycle#ALL} by default (Step 3). */
    public Builder filter(Set<Lifecycle> filter) {
      this.filter = java.util.Collections.unmodifiableSet(java.util.EnumSet.copyOf(filter));
      return this;
    }

    public ElementRetriever build() {
      return new ElementRetriever(this);
    }
  }
}
