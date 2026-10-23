package xyz.briancorbin.rags;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

/**
 * Follows the paper trail (Episode 4, Step 4): a result that something newer replaced gives its
 * place to its successor.
 *
 * <p>The successor is read from the unit's metadata, where {@link ElementReader} put it ({@code
 * superseded_by}), following up to {@value #HOPS} hops. On a superseded page, the successor is the
 * element with the same id on the new page or, failing that, the new page's best match for the
 * question. Whatever was superseded is then dropped, applied changes included. A requirement can be
 * rewritten by several applied changes in turn, and each one left a proposal in the archive saying
 * it rewrote that requirement; nothing on those proposals says which rewrite is the live one, so
 * keeping them puts three versions of the same rule in front of a reader. Episode 5 measured it.
 */
public final class SupersessionRedirect implements DocumentPostProcessor {

  static final int HOPS = 3;

  private final KeywordSearch keywords;

  public SupersessionRedirect(KeywordSearch keywords) {
    this.keywords = keywords;
  }

  @Override
  public List<Document> process(Query query, List<Document> documents) {
    var out = new LinkedHashMap<String, Document>(); // by id, in order, each once
    for (Document doc : documents) {
      String address = ElementReader.address(doc);
      String current = follow(query.text(), address); // superseded_by, up to three hops
      if (current.equals(address)) {
        out.putIfAbsent(doc.getId(), doc);
        continue;
      }
      Document successor = bestPart(query.text(), current);
      out.putIfAbsent(
          successor.getId(), redirected(successor, address)); // remembers where it came from
    }
    // Two stale results can share one successor, so the list can come back shorter than it went in.
    return List.copyOf(out.values());
  }

  /** The current address for {@code address}: itself, or where its paper trail ends. */
  private String follow(String question, String address) {
    String at = address;
    for (int hop = 0; hop < HOPS; hop++) {
      var marks = keywords.units().parts(at).getFirst().getMetadata();
      Object next = marks.get(ElementReader.SUPERSEDED_BY);
      Object page = marks.get(ElementReader.PAGE_SUPERSEDED_BY);
      if (next != null) {
        at = next.toString();
      } else if (page != null) {
        String sameId = page + "#" + marks.get(ElementReader.ANCHOR);
        if (!keywords.units().parts(sameId).isEmpty()) {
          at = sameId;
        } else {
          Optional<Document> best = bestOnPage(question, page.toString());
          if (best.isEmpty()) {
            break;
          }
          at = ElementReader.address(best.get());
        }
      } else {
        break;
      }
    }
    return at;
  }

  /**
   * The part of a split unit that best matches the question; the unit itself if it has one part.
   */
  private Document bestPart(String question, String address) {
    List<Document> parts = keywords.units().parts(address);
    if (parts.size() == 1) {
      return parts.getFirst();
    }
    List<Document> ranked = keywords.rank(question, parts);
    return ranked.isEmpty() ? parts.getFirst() : ranked.getFirst();
  }

  private Optional<Document> bestOnPage(String question, String source) {
    var live = liveUnitsOn(source);
    return live.isEmpty() ? Optional.empty() : keywords.rank(question, live).stream().findFirst();
  }

  /** The units of page {@code source} that are still in force, in archive order. */
  private List<Document> liveUnitsOn(String source) {
    return keywords.units().all().stream()
        .filter(unit -> source.equals(unit.getMetadata().get(ElementReader.SOURCE)))
        .filter(unit -> !ElementReader.lifecycle(unit).outOfDate())
        .toList();
  }

  /** The successor, saying which result it replaced: provenance for the redirect itself. */
  private static Document redirected(Document successor, String from) {
    var copy = ElementUnits.scored(successor, successor.getScore());
    copy.getMetadata().put("redirected_from", from);
    return copy;
  }
}
