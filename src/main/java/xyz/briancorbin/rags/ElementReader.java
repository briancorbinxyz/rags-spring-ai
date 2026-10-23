package xyz.briancorbin.rags;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
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

  private static final Pattern WHITESPACE =
      Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

  /** One space: a constant so the split is not recompiled per unit, and has no regex surprises. */
  private static final Pattern SPACE = Pattern.compile(" ", Pattern.LITERAL);

  private static final Pattern SENTENCE_END =
      Pattern.compile("(?<=[.!?;:])\\s+", Pattern.UNICODE_CHARACTER_CLASS);
  private static final Set<String> HEADINGS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");
  private static final Set<String> RAW = Set.of("script", "style");

  /** One page of the archive, parsed. */
  record Page(String source, org.jsoup.nodes.Document html, Element body) {}

  private final Path checkout;

  /**
   * @param checkout a spectastic checkout: the folder that holds {@code specs/}
   */
  public ElementReader(Path checkout) {
    this.checkout = checkout;
  }

  @Override
  public List<Document> get() {
    List<Page> pages = pages();
    var units = new LinkedHashMap<String, Unit>(); // address -> unit, in archive order
    pages.forEach(page -> units.putAll(units(page)));
    var supersession = Supersession.read(pages, units);
    var documents = new ArrayList<Document>();
    for (Unit unit : units.values()) {
      for (int part = 0; part < unit.parts().size(); part++) {
        documents.add(document(unit, part, supersession));
      }
    }
    return documents;
  }

  /** One part of one unit, as the search and the reader will see it. */
  private static Document document(Unit unit, int part, Supersession supersession) {
    return Document.builder()
        .id(unit.parts().size() == 1 ? unit.address() : unit.address() + "@" + (part + 1))
        .text(unit.parts().get(part))
        .metadata(metadata(unit, part, supersession))
        .build();
  }

  /** Everything the search filters, pins, redirects or shows a reader, read once here. */
  private static Map<String, Object> metadata(Unit unit, int part, Supersession supersession) {
    var metadata = new LinkedHashMap<String, Object>();
    metadata.put(SOURCE, unit.source());
    metadata.put(ANCHOR, unit.anchor());
    metadata.put(SPEC, unit.spec());
    metadata.put(TITLE, unit.title());
    metadata.put(HEADING_PATH, unit.headingPath());
    metadata.put(HEADING, unit.heading());
    metadata.put(LIFECYCLE, unit.lifecycle().value());
    metadata.put(PART, part + 1);
    metadata.put(PARTS, unit.parts().size());
    supersession.successor(unit.address()).ifPresent(to -> metadata.put(SUPERSEDED_BY, to));
    supersession.pageSuccessor(unit.source()).ifPresent(to -> metadata.put(PAGE_SUPERSEDED_BY, to));
    return metadata;
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

  /** What a page says about itself, read once rather than once per element. */
  private record Meta(String title, boolean superseded) {
    static Meta of(Page page) {
      Element title = page.html().selectFirst("title");
      // The page's own status sits in its <spec-meta>; a status inside an element is that
      // element's.
      Element status = page.html().selectFirst("spec-meta spec-status");
      return new Meta(
          title == null ? "" : strippedText(title),
          status != null && "superseded".equals(status.attr("value")));
    }
  }

  @SuppressWarnings("ReferenceEquality") // the body element itself, not one equal to it
  private static Map<String, Unit> units(Page page) {
    var units = new LinkedHashMap<String, Unit>();
    Meta meta = Meta.of(page);
    for (Element el : page.body().getAllElements()) {
      // Identity on purpose: "is this the body element itself", not "equal to some body".
      if (el == page.body() || el.id().isEmpty()) {
        continue;
      }
      // An element with an id is its own unit, so its text does not count as this one's.
      String text = text(el, inner -> !inner.id().isEmpty());
      if (words(text) < MIN_WORDS) {
        continue; // a heading on its own is a label, not a passage
      }
      String address = page.source() + "#" + el.id();
      if (units.containsKey(address)) {
        continue; // a repeated id: the first is the element, as getElementById says
      }
      units.put(address, unit(page, el, text, meta));
    }
    return units;
  }

  /** One element as a unit: where it sits, what it reads as, and what state it is in. */
  private static Unit unit(Page page, Element el, String text, Meta meta) {
    var lifecycle = Lifecycle.read(page.source(), el);
    if (meta.superseded() && lifecycle == Lifecycle.CURRENT) {
      lifecycle = Lifecycle.SUPERSEDED;
    }
    var ownHeading = first(el, inner -> Set.of("h2", "h3", "h4").contains(inner.normalName()));
    return new Unit(
        page.source(),
        el.id(),
        page.source().split("/", -1)[1],
        meta.title(),
        headingPath(meta.title(), el),
        ownHeading == null ? "" : strippedText(ownHeading),
        lifecycle,
        parts(text));
  }

  /** Page title › the heading of each section with an id around the element, outermost first. */
  static String headingPath(String title, Element el) {
    var path = new ArrayList<String>();
    if (!title.isEmpty()) {
      path.add(title);
    }
    var outward = el.parents();
    for (Element ancestor : outward.reversed()) {
      if (ancestor.id().isEmpty()
          || Set.of("body", "main", "html").contains(ancestor.normalName())) {
        continue;
      }
      String heading = headingOf(ancestor);
      if (!heading.isEmpty() && (path.isEmpty() || !path.getLast().equals(heading))) {
        path.add(heading);
      }
    }
    return String.join(" › ", path);
  }

  /**
   * Splits a long unit into parts of at most {@code max} words, ending at a sentence where it can:
   * as many parts as the length needs, each about the same size.
   */
  static List<String> parts(String text, int max) {
    String[] words = SPACE.split(text, -1);
    if (words.length <= max) {
      return List.of(text);
    }
    int n = (int) Math.ceil(words.length / (double) max);
    double target = words.length / (double) n;
    var pieces = new ArrayList<List<String>>();
    for (String sentence : SENTENCE_END.split(text, -1)) {
      List<String> remaining = sentence.isEmpty() ? List.of() : List.of(SPACE.split(sentence, -1));
      while (remaining.size() > max) { // one very long "sentence", a table say: cut by words
        pieces.add(remaining.subList(0, max));
        remaining = remaining.subList(max, remaining.size());
      }
      if (!remaining.isEmpty()) {
        pieces.add(remaining);
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
    collect(el, nothing -> false, true, out);
    return collapse(out.toString());
  }

  /** Text nodes joined by one space, untrimmed: what surrounds a link. */
  static String rawText(Element el) {
    var out = new StringBuilder();
    collect(el, nothing -> false, false, out);
    return out.isEmpty() ? "" : out.substring(1);
  }

  private static void collect(
      Element el, Predicate<Element> skip, boolean strip, StringBuilder out) {
    for (Node child : el.childNodes()) {
      if (child instanceof TextNode text) {
        String words = strip ? text.getWholeText().strip() : text.getWholeText();
        if (!strip || !words.isEmpty()) {
          out.append(' ').append(words);
        }
      } else if (child instanceof Element nested
          && !RAW.contains(nested.normalName())
          && !skip.test(nested)) {
        collect(nested, skip, strip, out);
      }
    }
  }

  static String collapse(String s) {
    return WHITESPACE.matcher(s).replaceAll(" ").strip();
  }

  static int words(String collapsed) {
    return collapsed.isEmpty() ? 0 : SPACE.split(collapsed, -1).length;
  }

  /**
   * The first element inside {@code el} (not {@code el} itself), in document order, that matches.
   */
  @SuppressWarnings("ReferenceEquality") // the node itself is excluded, not one equal to it
  static Element first(Element el, Predicate<Element> test) {
    for (Element inside : el.getAllElements()) {
      if (inside != el && test.test(inside)) {
        return inside;
      }
    }
    return null;
  }

  // --- pages ---------------------------------------------------------------------------------

  /** Every page under {@code specs/}, in path order. */
  List<Page> pages() {
    try (Stream<Path> files = Files.walk(checkout.resolve("specs"))) {
      var pages = new ArrayList<Page>();
      for (Path file : sortedHtmlFilesIn(files)) {
        var html = Jsoup.parse(Files.readString(file, StandardCharsets.UTF_8));
        pages.add(new Page(slash(checkout.relativize(file)), html, html.body()));
      }
      return pages;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the archive at " + checkout, e);
    }
  }

  /** An element's own heading, as a reader sees it; empty if it has none of its own. */
  private static String headingOf(Element element) {
    return element.children().stream()
        .filter(child -> HEADINGS.contains(child.normalName()))
        .findFirst()
        .map(ElementReader::strippedText)
        .orElse("");
  }

  /** The HTML files among {@code files}, ordered by their '/' path so a run is reproducible. */
  private static List<Path> sortedHtmlFilesIn(Stream<Path> files) {
    return files
        .filter(
            path -> path.getFileName().toString().endsWith(".html") && Files.isRegularFile(path))
        .sorted(Comparator.comparing(ElementReader::slash))
        .toList();
  }

  private static String slash(Path p) {
    return p.toString().replace('\\', '/');
  }
}
