package xyz.briancorbin.rags.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import xyz.briancorbin.rags.units.StandingFilter;

/// The search, as a Spring AI {@link DocumentRetriever}: keyword search over the element units, the
/// units a question names pinned above it, out-of-date units filtered out, and dense search fused
/// in.
///
/// ```java
/// ElementRetriever.builder()
///     .keywords(keywords)                 // BM25 over the element units
///     .identifiers(new IdentifierLookup(units, new SpecGrammar()))
///     .filter(StandingFilter.of(Lifecycle.IN_FORCE))
///     .build();
/// ```
///
/// The ranking is the keyword search's, behind the standing filter. Pinned units go first and
/// carry no score: they were looked up, not ranked.
///
/// What comes back is the pinned units, then the first {@value #DEPTH} of the ranking — everything
/// that can reach a top {@value #DEPTH}, since each pinned unit pushes a ranked one out and never
/// pulls a further one in. Cutting to {@value #DEPTH} is {@link TopDocuments}'s job, after any
/// reranking, so a reranker sees the same candidates however many units were pinned.
///
/// Spring AI 2.0.1 has no fusion of its own. Its `ConcatenationDocumentJoiner` is not it: that one
/// re-sorts the joined lists by {@link Document#getScore()} descending, which compares scores from
/// different scales and throws on a pinned unit, whose score is null.
public final class ElementRetriever implements DocumentRetriever {

  /// The results a step returns, and so the most candidates anything after it can need.
  public static final int DEPTH = 50;

  /// Metadata marking a unit that was looked up rather than ranked, so nothing reranks it.
  public static final String PINNED = "pinned";

  private final KeywordSearch keywords;
  private final IdentifierLookup identifiers; // null: none
  private final StandingFilter filter;

  private ElementRetriever(Builder b) {
    this.keywords = Objects.requireNonNull(b.keywords, "keywords");
    this.identifiers = b.identifiers;
    this.filter = b.filter;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public List<Document> retrieve(Query query) {
    String question = query.text();
    List<Document> ranked = keywords.search(question, DEPTH, filter);

    var out = new LinkedHashMap<String, Document>(); // by id, in order, each once
    for (Document unit : pinned(question)) {
      out.putIfAbsent(unit.getId(), unit);
    }
    int room = DEPTH;
    for (Document hit : ranked) {
      if (room == 0) {
        break;
      }
      if (!out.containsKey(hit.getId())) {
        out.put(hit.getId(), hit);
        room--;
      }
    }
    return List.copyOf(out.values());
  }

  /// Every part of every unit the question names, in the lookup's order, that the filter allows.
  private List<Document> pinned(String question) {
    if (identifiers == null) {
      return List.of();
    }
    var out = new ArrayList<Document>();
    for (String address : identifiers.lookup(question)) {
      for (Document part : keywords.units().parts(address)) {
        if (filter.allows(keywords.units().standing(part))) {
          out.add(mark(part));
        }
      }
    }
    return out;
  }

  /// A copy of a unit with no score and the pinned mark: looked up, not ranked.
  private static Document mark(Document unit) {
    var metadata = new HashMap<>(unit.getMetadata());
    metadata.put(PINNED, true);
    return Document.builder().id(unit.getId()).text(unit.getText()).metadata(metadata).build();
  }

  /// Whether a unit was pinned by the identifier lookup, and so is not a ranked result.
  public static boolean isPinned(Document unit) {
    return Boolean.TRUE.equals(unit.getMetadata().get(PINNED));
  }

  public static final class Builder {
    private KeywordSearch keywords;
    private IdentifierLookup identifiers;
    private StandingFilter filter = StandingFilter.all();

    private Builder() {}

    /// Keyword search over the element units. Required.
    public Builder keywords(KeywordSearch keywords) {
      this.keywords = keywords;
      return this;
    }

    /// Pin the units a question names by id and scope above the search.
    public Builder identifiers(IdentifierLookup identifiers) {
      this.identifiers = identifiers;
      return this;
    }

    /// Which standings may come back, named or searched; every one of them by default. The filter
    /// is a value, so it already copies what it was given.
    public Builder filter(StandingFilter filter) {
      this.filter = filter;
      return this;
    }

    public ElementRetriever build() {
      return new ElementRetriever(this);
    }
  }
}
