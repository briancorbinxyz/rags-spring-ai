package xyz.briancorbin.rags;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

/**
 * Reads a spectastic checkout one element at a time (Episode 4, Step 1).
 *
 * <p>Every element with an {@code id} is a unit: a requirement, a decision, a task, a success
 * criterion, a section. Its text is its own text only; an element inside it with an id of its own,
 * a requirement inside its section say, is a unit of its own. Units of fewer than three words are
 * left out. Each unit's heading path (page title › the headings of the sections around it) is kept
 * in its metadata, and {@link #passage(Document)} puts it in front of the text, so {@code FR-001}
 * on its own still says which spec's requirements it belongs to. A unit over {@value #PART_WORDS}
 * words is split into parts at sentence ends, and every part carries the unit's address.
 *
 * <p>The metadata is the provenance: {@code source}, {@code anchor}, {@code spec}, {@code title},
 * {@code heading_path}, {@code heading}, {@code lifecycle}, {@code part} and {@code parts}, and,
 * for a record that something newer replaced, {@code superseded_by} or {@code page_superseded_by}
 * (see {@link Supersession}).
 */
public final class ElementReader implements DocumentReader {

  public static final String SOURCE = Address.SOURCE;
  public static final String SPEC = Address.SPEC;
  public static final String ANCHOR = "anchor";
  public static final String TITLE = "title";
  public static final String HEADING_PATH = "heading_path";
  public static final String HEADING = "heading";
  public static final String LIFECYCLE = "lifecycle";
  public static final String PART = "part";
  public static final String PARTS = "parts";
  public static final String SUPERSEDED_BY = "superseded_by";
  public static final String PAGE_SUPERSEDED_BY = "page_superseded_by";

  /** Units shorter than this many words are labels, not passages. */
  static final int MIN_WORDS = 3;

  /** Longer units are split: about 350 tokens a part, so a question and a part fit a reranker. */
  static final int PART_WORDS = 250;

  private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Pattern SENTENCE_END =
      Pattern.compile("(?<=[.!?;:])\\s+", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Set<String> HEADINGS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");
  private static final Set<String> RAW = Set.of("script", "style");

  /** One page of the archive, parsed. */
  record Page(String source, org.jsoup.nodes.Document html, Element body) {}

  private final Path checkout;

  /** @param checkout a spectastic checkout: the folder that holds {@code specs/} */
  public ElementReader(Path checkout) {
    this.checkout = checkout;
  }

  @Override
  public List<Document> get() {
    List<Page> pages = pages();
    var units = new LinkedHashMap<String, Unit>(); // address -> unit, in archive order
    for (Page page : pages) {
      units.putAll(units(page));
    }
    var supersession = Supersession.read(pages, units);
    var documents = new ArrayList<Document>();
    for (Unit u : units.values()) {
      for (int i = 0; i < u.parts().size(); i++) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(SOURCE, u.source());
        metadata.put(ANCHOR, u.anchor());
        metadata.put(SPEC, u.spec());
        metadata.put(TITLE, u.title());
        metadata.put(HEADING_PATH, u.headingPath());
        metadata.put(HEADING, u.heading());
        metadata.put(LIFECYCLE, u.lifecycle().value());
        metadata.put(PART, i + 1);
        metadata.put(PARTS, u.parts().size());
        supersession.successor(u.address()).ifPresent(s -> metadata.put(SUPERSEDED_BY, s));
        supersession.pageSuccessor(u.source()).ifPresent(s -> metadata.put(PAGE_SUPERSEDED_BY, s));
        documents.add(
            Document.builder()
                .id(u.parts().size() == 1 ? u.address() : u.address() + "@" + (i + 1))
                .text(u.parts().get(i))
                .metadata(metadata)
                .build());
      }
    }
    return documents;
  }

  /** What a reader is given: the heading path, then the unit's text. */
  public static String passage(Document unit) {
    String path = String.valueOf(unit.getMetadata().getOrDefault(HEADING_PATH, ""));
    return path.isEmpty() ? unit.getText() : path + "\n" + unit.getText();
  }

  /** {@code specs/<spec>/<file>.html#<anchor>}: the address the golden set uses. */
  public static String address(Document unit) {
    return unit.getMetadata().get(SOURCE) + "#" + unit.getMetadata().get(ANCHOR);
  }

  public static Lifecycle lifecycle(Document unit) {
    return Lifecycle.of(unit.getMetadata().get(LIFECYCLE));
  }

  // --- units ---------------------------------------------------------------------------------

  record Unit(
      String source,
      String anchor,
      String spec,
      String title,
      String headingPath,
      String heading,
      Lifecycle lifecycle,
      List<String> parts) {

    String address() {
      return source + "#" + anchor;
    }
  }

  private static Map<String, Unit> units(Page page) {
    var units = new LinkedHashMap<String, Unit>();
    Element titleElement = page.html().selectFirst("title");
    String title = titleElement == null ? "" : strippedText(titleElement);
    // The page's own status sits in its <spec-meta>; a status inside an element is that element's.
    Element status = page.html().selectFirst("spec-meta spec-status");
    boolean pageSuperseded = status != null && "superseded".equals(status.attr("value"));
    for (Element el : page.body().getAllElements()) {
      if (el == page.body() || el.id().isEmpty()) {
        continue;
      }
      String text = text(el, e -> !e.id().isEmpty());
      if (words(text) < MIN_WORDS) {
        continue;
      }
      String address = page.source() + "#" + el.id();
      if (units.containsKey(address)) {
        continue; // a repeated id: the first is the element, as getElementById says
      }
      Lifecycle lifecycle = Lifecycle.read(page.source(), el);
      if (pageSuperseded && lifecycle == Lifecycle.CURRENT) {
        lifecycle = Lifecycle.SUPERSEDED;
      }
      Element heading = first(el, e -> Set.of("h2", "h3", "h4").contains(e.normalName()));
      units.put(
          address,
          new Unit(
              page.source(),
              el.id(),
              page.source().split("/")[1],
              title,
              headingPath(title, el),
              heading == null ? "" : strippedText(heading),
              lifecycle,
              parts(text)));
    }
    return units;
  }

  /** Page title › the heading of each section with an id around the element, outermost first. */
  static String headingPath(String title, Element el) {
    var path = new ArrayList<String>();
    if (!title.isEmpty()) {
      path.add(title);
    }
    List<Element> outward = el.parents();
    for (Element a : outward.reversed()) {
      if (a.id().isEmpty() || Set.of("body", "main", "html").contains(a.normalName())) {
        continue;
      }
      String h =
          a.children().stream()
              .filter(c -> HEADINGS.contains(c.normalName()))
              .findFirst()
              .map(ElementReader::strippedText)
              .orElse("");
      if (!h.isEmpty() && (path.isEmpty() || !path.getLast().equals(h))) {
        path.add(h);
      }
    }
    return String.join(" › ", path);
  }

  /**
   * Splits a long unit into parts of at most {@code max} words, ending at a sentence where it can:
   * as many parts as the length needs, each about the same size.
   */
  static List<String> parts(String text, int max) {
    String[] words = text.split(" ");
    if (words.length <= max) {
      return List.of(text);
    }
    int n = (int) Math.ceil(words.length / (double) max);
    double target = words.length / (double) n;
    var pieces = new ArrayList<List<String>>();
    for (String sentence : SENTENCE_END.split(text)) {
      List<String> w = sentence.isEmpty() ? List.of() : List.of(sentence.split(" "));
      while (w.size() > max) { // one very long "sentence", a table say: cut by words
        pieces.add(w.subList(0, max));
        w = w.subList(max, w.size());
      }
      if (!w.isEmpty()) {
        pieces.add(w);
      }
    }
    var parts = new ArrayList<String>();
    var current = new ArrayList<String>();
    for (List<String> piece : pieces) {
      if (!current.isEmpty() && (current.size() + piece.size() > max || current.size() >= target)) {
        parts.add(String.join(" ", current));
        current = new ArrayList<>();
      }
      current.addAll(piece);
    }
    if (!current.isEmpty()) {
      parts.add(String.join(" ", current));
    }
    return parts;
  }

  static List<String> parts(String text) {
    return parts(text, PART_WORDS);
  }

  // --- reading text --------------------------------------------------------------------------

  /**
   * An element's text: every text node, joined by a space and collapsed, leaving out scripts,
   * styles and any element {@code skip} matches.
   */
  static String text(Element el, Predicate<Element> skip) {
    var out = new StringBuilder();
    collect(el, skip, false, out);
    return collapse(out.toString());
  }

  /** All the element's text, each text node trimmed: for titles and headings. */
  static String strippedText(Element el) {
    var out = new StringBuilder();
    collect(el, e -> false, true, out);
    return collapse(out.toString());
  }

  /** Text nodes joined by one space, untrimmed: what surrounds a link. */
  static String rawText(Element el) {
    var out = new StringBuilder();
    collect(el, e -> false, false, out);
    return out.isEmpty() ? "" : out.substring(1);
  }

  private static void collect(Element el, Predicate<Element> skip, boolean strip, StringBuilder out) {
    for (Node child : el.childNodes()) {
      if (child instanceof TextNode t) {
        String s = strip ? t.getWholeText().strip() : t.getWholeText();
        if (!strip || !s.isEmpty()) {
          out.append(' ').append(s);
        }
      } else if (child instanceof Element e && !RAW.contains(e.normalName()) && !skip.test(e)) {
        collect(e, skip, strip, out);
      }
    }
  }

  static String collapse(String s) {
    return WHITESPACE.matcher(s).replaceAll(" ").strip();
  }

  static int words(String collapsed) {
    return collapsed.isEmpty() ? 0 : collapsed.split(" ").length;
  }

  /** The first element inside {@code el} (not {@code el} itself), in document order, that matches. */
  static Element first(Element el, Predicate<Element> test) {
    for (Element e : el.getAllElements()) {
      if (e != el && test.test(e)) {
        return e;
      }
    }
    return null;
  }

  // --- pages ---------------------------------------------------------------------------------

  /** Every page under {@code specs/}, in path order. */
  List<Page> pages() {
    try (Stream<Path> files = Files.walk(checkout.resolve("specs"))) {
      var pages = new ArrayList<Page>();
      for (Path f :
          files
              .filter(p -> p.getFileName().toString().endsWith(".html") && Files.isRegularFile(p))
              .sorted((a, b) -> slash(a).compareTo(slash(b)))
              .toList()) {
        var html = Jsoup.parse(Files.readString(f, StandardCharsets.UTF_8));
        pages.add(new Page(slash(checkout.relativize(f)), html, html.body()));
      }
      return pages;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the archive at " + checkout, e);
    }
  }

  private static String slash(Path p) {
    return p.toString().replace('\\', '/');
  }
}
