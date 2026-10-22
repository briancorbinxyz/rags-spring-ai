package xyz.briancorbin.rags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

/**
 * Episode 5's search (Step 5): keyword and dense search over the same element units, fused.
 *
 * <p>Both sides search {@value #DEPTH_EACH} deep, both behind the same lifecycle filter, and
 * {@link Rrf} fuses them on rank alone. The units a question names by id are pinned above the
 * fusion, as in Episode 4, and carry no score: they were not ranked, they were looked up.
 *
 * <p>What comes back is the pinned units, then the first {@value #RERANK_TOP} of the fusion. That is
 * everything that can reach a top {@value #DEPTH}, whether or not a reranker runs next: with no
 * pinned units the final list is the first {@value #DEPTH} of the fusion, and every pinned unit
 * pushes one fused unit out, never pulls a further one in. Cutting to {@value #DEPTH} is
 * {@link TopDocuments}'s job, after any reranking, so a reranker sees the same
 * {@value #RERANK_TOP} candidates no matter how many units were pinned.
 *
 * <p>Spring AI 2.0.1 has no fusion of its own. Its {@code ConcatenationDocumentJoiner} is not it:
 * that one re-sorts the joined lists by {@link Document#getScore()} descending, which both
 * compares scores from different scales and throws on a pinned unit, whose score is null.
 */
public final class FusedRetriever implements DocumentRetriever {

  /** Each list's depth before fusion. Deeper than the answer, so fusion has room to disagree. */
  public static final int DEPTH_EACH = 100;

  /** What the reranker may reorder, and so all that any later cut can need. */
  public static final int RERANK_TOP = 50;

  /** The results a step returns, as Episode 4's {@link ElementRetriever#DEPTH}. */
  public static final int DEPTH = ElementRetriever.DEPTH;

  /** Metadata marking a unit that was looked up rather than ranked, so nothing reranks it. */
  public static final String PINNED = "pinned";

  private final KeywordIndex index;
  private final UnitVectors vectors;
  private final IdentifierLookup identifiers; // null: none
  private final Set<Lifecycle> filter;

  private FusedRetriever(Builder b) {
    this.index = Objects.requireNonNull(b.index, "index");
    this.vectors = Objects.requireNonNull(b.vectors, "vectors");
    this.identifiers = b.identifiers;
    this.filter = b.filter;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public List<Document> retrieve(Query query) {
    String question = query.text();
    List<Document> keyword = index.search(question, DEPTH_EACH, filter);
    List<Document> dense = vectors.search(question, DEPTH_EACH, filter);
    List<Document> fused = Rrf.fuse(List.of(keyword, dense));

    var out = new LinkedHashMap<String, Document>(); // by id, in order, each once
    for (Document unit : pinned(question)) {
      out.putIfAbsent(unit.getId(), unit);
    }
    int room = RERANK_TOP;
    for (Document hit : fused) {
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

  /** Every part of every unit the question names, in the lookup's order, that the filter allows. */
  private List<Document> pinned(String question) {
    if (identifiers == null) {
      return List.of();
    }
    var out = new ArrayList<Document>();
    for (String address : identifiers.lookup(question)) {
      for (Document part : index.parts(address)) {
        if (filter.contains(ElementReader.lifecycle(part))) {
          out.add(mark(part));
        }
      }
    }
    return out;
  }

  /** A copy of a unit with no score and the pinned mark: looked up, not ranked. */
  private static Document mark(Document unit) {
    var metadata = new HashMap<>(unit.getMetadata());
    metadata.put(PINNED, true);
    return Document.builder().id(unit.getId()).text(unit.getText()).metadata(metadata).build();
  }

  /** Whether a unit was pinned by the identifier lookup. */
  public static boolean isPinned(Document unit) {
    return Boolean.TRUE.equals(unit.getMetadata().get(PINNED));
  }

  public static final class Builder {
    private KeywordIndex index;
    private UnitVectors vectors;
    private IdentifierLookup identifiers;
    private Set<Lifecycle> filter = Lifecycle.ALL;

    private Builder() {}

    /** The units to search with BM25. Required. */
    public Builder index(KeywordIndex index) {
      this.index = index;
      return this;
    }

    /** The same units as vectors. Required. */
    public Builder vectors(UnitVectors vectors) {
      this.vectors = vectors;
      return this;
    }

    /** Pin the units a question names by id and spec above the fusion. */
    public Builder identifiers(IdentifierLookup identifiers) {
      this.identifiers = identifiers;
      return this;
    }

    /** Which lifecycles may come back, named or searched; {@link Lifecycle#ALL} by default. */
    public Builder filter(Set<Lifecycle> filter) {
      this.filter = Set.copyOf(filter);
      return this;
    }

    public FusedRetriever build() {
      return new FusedRetriever(this);
    }
  }
}
