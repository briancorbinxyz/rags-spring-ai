package xyz.briancorbin.rags.eval;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jsoup.Jsoup;
import org.springframework.ai.document.Document;
import xyz.briancorbin.rags.units.Address;

/**
 * Decides, in code, whether a retrieved chunk found the passage a question expects.
 *
 * <p>An expected address names one element, but the naive chunks know nothing about elements: they
 * are 800-token slices of a page's text. So the scorer finds where the element's text sits in the
 * page's text, finds where the chunk sits, and measures the overlap. A chunk that holds the whole
 * element is a <em>hit</em>. One that holds at least half of it is a <em>partial hit</em>, which is
 * what an element cut in two by a chunk boundary looks like.
 *
 * <p>The page text is read exactly as the archive read it, with jsoup's {@code body().text()}, so a
 * chunk's text is a slice of it and positions can be compared directly.
 *
 * <p>From golden set version 2 a question can also carry passages the blind audit accepted (see
 * {@link GoldenSet.Accept}). An accepted passage found whole counts as a hit, and it covers the
 * expected element it stands in for, or all of them when it answers the whole question.
 */
public final class GoldenScorer {

  public enum Hit {
    FULL,
    PARTIAL,
    NONE;

    /** The better of two outcomes on the same chunk. Said out, not left to declaration order. */
    Hit or(Hit other) {
      if (this == FULL || other == FULL) {
        return FULL;
      }
      return this == PARTIAL || other == PARTIAL ? PARTIAL : NONE;
    }
  }

  /**
   * Where the first full and first partial hits came in the results, counting from 1, or null when
   * there was none. A full hit also counts as a partial one. {@code firstAll} is the rank by which
   * every expected element had been found whole (all-evidence), or null if that never happened.
   */
  public record Score(Integer firstHit, Integer firstPartial, Integer firstAll) {

    public Score(Integer firstHit, Integer firstPartial) {
      this(firstHit, firstPartial, null);
    }

    /** Every expected element found whole in the first {@code k} results. */
    public boolean allAt(int k) {
      return firstAll != null && firstAll <= k;
    }

    public boolean hitAt(int k) {
      return firstHit != null && firstHit <= k;
    }

    public boolean partialAt(int k) {
      return firstPartial != null && firstPartial <= k;
    }

    /** The reciprocal rank of the first full hit: 1, 1/2, 1/3, …, or 0 for a miss. */
    public double reciprocalRank() {
      return firstHit == null ? 0 : 1.0 / firstHit;
    }
  }

  private record Page(String text, org.jsoup.nodes.Document html) {}

  private final Path checkout;
  private final Map<String, Page> pages = new HashMap<>();

  public GoldenScorer(Path checkout) {
    this.checkout = checkout;
  }

  /** Scores a ranked list of chunks against any of the expected addresses. */
  public Score score(List<String> expected, List<Document> retrieved) {
    return score(expected, List.of(), retrieved);
  }

  /** Scores a question, counting the passages its audit accepted. */
  public Score score(GoldenSet.Question q, List<Document> retrieved) {
    return score(q.expected(), q.accept(), retrieved);
  }

  /** What a question will accept as an answer: the passages it expects, and the audit's rulings. */
  private record Wanted(List<String> expected, List<GoldenSet.Accept> accept) {}

  /**
   * Scores a ranked list of chunks against the expected addresses and the accepted ones. A partial
   * hit on an accepted passage counts as partial, never as a hit.
   */
  public Score score(
      List<String> expected, List<GoldenSet.Accept> accept, List<Document> retrieved) {
    var wanted = new Wanted(expected, accept);
    Integer hit = null;
    Integer partial = null;
    Integer all = null;
    var covered = new HashSet<String>();
    for (int rank = 0; rank < retrieved.size(); rank++) {
      Hit best = cover(wanted, retrieved.get(rank), covered);
      if (best == Hit.FULL && hit == null) {
        hit = rank + 1;
      }
      if (best != Hit.NONE && partial == null) {
        partial = rank + 1;
      }
      if (all == null && !expected.isEmpty() && covered.containsAll(expected)) {
        all = rank + 1;
      }
    }
    return new Score(hit, partial, all);
  }

  /**
   * Notes which expected passages this chunk covers, and returns the best it managed on any of
   * them. An accepted passage can stand for one expected passage or for the whole question, which
   * is what the blind audit decided for it.
   */
  private Hit cover(Wanted wanted, Document chunk, Set<String> covered) {
    Hit best = Hit.NONE;
    for (String address : wanted.expected()) {
      Hit found = hit(address, chunk);
      if (found == Hit.FULL) {
        covered.add(address);
      }
      best = best.or(found);
    }
    for (GoldenSet.Accept accepted : wanted.accept()) {
      Hit found = hit(accepted.address(), chunk);
      if (found == Hit.FULL) {
        if (accepted.wholeQuestion()) {
          covered.addAll(wanted.expected());
        } else if (wanted.expected().contains(accepted.forTarget())) {
          covered.add(accepted.forTarget());
        }
      }
      best = best.or(found);
    }
    return best;
  }

  /**
   * Scores a ranked list of element addresses. With element units a result <em>is</em> an element,
   * so a hit is an address that matches: an expected one, or one the audit accepted for the whole
   * question or for one of the expected passages. There are no partial hits.
   */
  public static Score byAddress(GoldenSet.Question question, List<String> ranked) {
    Integer hit = null;
    Integer all = null;
    var covered = new HashSet<String>();
    for (int rank = 0; rank < ranked.size(); rank++) {
      if (cover(question, ranked.get(rank), covered) && hit == null) {
        hit = rank + 1;
      }
      if (all == null
          && !question.expected().isEmpty()
          && covered.containsAll(question.expected())) {
        all = rank + 1;
      }
    }
    return new Score(hit, hit, all);
  }

  /**
   * Notes which expected passages {@code address} covers, and says whether it covered any. An
   * accepted passage can stand for one expected passage or for the whole question, which is what
   * the blind audit decided for it.
   */
  private static boolean cover(GoldenSet.Question question, String address, Set<String> covered) {
    boolean found = false;
    if (question.expected().contains(address)) {
      covered.add(address);
      found = true;
    }
    for (GoldenSet.Accept accepted : question.accept()) {
      if (!accepted.address().equals(address)) {
        continue;
      }
      if (accepted.wholeQuestion()) {
        covered.addAll(question.expected());
        found = true;
      } else if (question.expected().contains(accepted.forTarget())) {
        covered.add(accepted.forTarget());
        found = true;
      }
    }
    return found;
  }

  /** How much of the element at {@code address} one chunk holds. */
  public Hit hit(String address, Document chunk) {
    String file = address.contains("#") ? address.substring(0, address.indexOf('#')) : address;
    if (!file.equals(Address.of(chunk).source())) {
      return Hit.NONE;
    }
    String element = elementText(address).orElse("");
    String text = normalise(chunk.getText());
    if (element.isEmpty() || text.isEmpty()) {
      return Hit.NONE;
    }
    Page page = page(file);
    int chunkAt = page.text().indexOf(text);
    return chunkAt < 0 || element.length() < 2
        ? byContaining(text, element)
        : byOverlap(page, element, chunkAt, text.length());
  }

  /**
   * When the chunk is not a clean slice of the page — a multi-byte character the tokenizer split,
   * say — fall back to asking whether the element's text, or half of it, is in there at all.
   */
  private static Hit byContaining(String text, String element) {
    if (text.contains(element)) {
      return Hit.FULL;
    }
    int half = Math.max(1, element.length() / 2);
    return text.contains(element.substring(0, half)) || text.contains(element.substring(half))
        ? Hit.PARTIAL
        : Hit.NONE;
  }

  /**
   * The element's text can occur more than once on a page — in a summary table, say — so judge the
   * chunk by the occurrence it overlaps most.
   */
  private static Hit byOverlap(Page page, String element, int chunkAt, int chunkLength) {
    int best = 0;
    for (int at = page.text().indexOf(element);
        at >= 0;
        at = page.text().indexOf(element, at + 1)) {
      int overlap = Math.min(at + element.length(), chunkAt + chunkLength) - Math.max(at, chunkAt);
      best = Math.max(best, overlap);
    }
    if (best >= element.length()) {
      return Hit.FULL;
    }
    return best * 2 >= element.length() ? Hit.PARTIAL : Hit.NONE;
  }

  /**
   * The text of the element an address names, or the whole page for an address with no {@code #}.
   */
  public Optional<String> elementText(String address) {
    int hash = address.indexOf('#');
    String file = hash < 0 ? address : address.substring(0, hash);
    Page page = page(file);
    if (hash < 0) {
      return Optional.of(page.text());
    }
    var element = page.html().getElementById(address.substring(hash + 1));
    return element == null ? Optional.empty() : Optional.of(normalise(element.text()));
  }

  private Page page(String file) {
    return pages.computeIfAbsent(
        file,
        missing -> {
          try {
            var html = Jsoup.parse(Files.readString(checkout.resolve(missing)));
            return new Page(normalise(html.body().text()), html);
          } catch (IOException e) {
            throw new UncheckedIOException("No such file in the archive: " + missing, e);
          }
        });
  }

  static String normalise(String text) {
    return text == null ? "" : text.replaceAll("\\s+", " ").strip();
  }
}
