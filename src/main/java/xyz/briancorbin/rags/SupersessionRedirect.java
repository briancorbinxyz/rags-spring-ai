package xyz.briancorbin.rags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * question. An applied change (lifecycle {@code history}) is not wrong, it is how the current text
 * came to be, so it stays, right behind its successor. Anything else out of date is dropped.
 */
public final class SupersessionRedirect implements DocumentPostProcessor {

  static final int HOPS = 3;

  private final KeywordIndex index;

  public SupersessionRedirect(KeywordIndex index) {
    this.index = index;
  }

  @Override
  public List<Document> process(Query query, List<Document> documents) {
    var out = new LinkedHashMap<String, Document>(); // by id, in order, each once
    for (Document doc : documents) {
      String address = ElementReader.address(doc);
      String current = follow(query.text(), address);
      if (current.equals(address)) {
        out.putIfAbsent(doc.getId(), doc);
        continue;
      }
      Document successor = bestPart(query.text(), current);
      out.putIfAbsent(successor.getId(), redirected(successor, address));
      if (ElementReader.lifecycle(doc) == Lifecycle.HISTORY) {
        out.putIfAbsent(doc.getId(), doc);
      }
    }
    // An applied change kept behind its successor adds a result; the list stays at the search's depth.
    return out.values().stream().limit(ElementRetriever.DEPTH).toList();
  }

  /** The current address for {@code address}: itself, or where its paper trail ends. */
  private String follow(String question, String address) {
    String at = address;
    for (int hop = 0; hop < HOPS; hop++) {
      Map<String, Object> m = index.parts(at).getFirst().getMetadata();
      Object next = m.get(ElementReader.SUPERSEDED_BY);
      Object page = m.get(ElementReader.PAGE_SUPERSEDED_BY);
      if (next != null) {
        at = next.toString();
      } else if (page != null) {
        String sameId = page + "#" + m.get(ElementReader.ANCHOR);
        if (!index.parts(sameId).isEmpty()) {
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

  /** The part of a split unit that best matches the question; the unit itself if it has one part. */
  private Document bestPart(String question, String address) {
    List<Document> parts = index.parts(address);
    if (parts.size() == 1) {
      return parts.getFirst();
    }
    List<Document> ranked = index.rank(question, parts);
    return ranked.isEmpty() ? parts.getFirst() : ranked.getFirst();
  }

  private Optional<Document> bestOnPage(String question, String source) {
    List<Document> live =
        index.units().stream()
            .filter(u -> source.equals(u.getMetadata().get(ElementReader.SOURCE)))
            .filter(u -> !ElementReader.lifecycle(u).excluded())
            .toList();
    return live.isEmpty() ? Optional.empty() : index.rank(question, live).stream().findFirst();
  }

  /** The successor, saying which result it replaced: provenance for the redirect itself. */
  private static Document redirected(Document successor, String from) {
    Document copy = KeywordIndex.withScore(successor, successor.getScore());
    copy.getMetadata().put("redirected_from", from);
    return copy;
  }
}
