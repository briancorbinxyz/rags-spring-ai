package xyz.briancorbin.rags.spectastic;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.nodes.Element;
import xyz.briancorbin.rags.search.Successors;
import xyz.briancorbin.rags.spectastic.SpectasticReader.Page;
import xyz.briancorbin.rags.spectastic.SpectasticReader.Unit;

/**
 * The paper trail: which current element replaced each out-of-date one.
 *
 * <p>spectastic says it three ways, and all three are read:
 *
 * <ul>
 *   <li>a change proposal's {@code <spec-delta target="FR-003">}: an archived or withdrawn copy of
 *       a requirement points at the requirement it changed;
 *   <li>an element marked superseded in place, with a {@code <spec-status value="superseded">}
 *       inside it, followed by a link to its successor;
 *   <li>prose: "superseded by" just before a link.
 * </ul>
 *
 * <p>A whole page can be superseded too ({@code <spec-status value="superseded">} on the page,
 * "superseded by" a link to the new page); then {@link #pageSuccessor} names the new page. The
 * first link found for an element wins, read page by page in path order.
 */
public final class Supersession implements Successors {

  private static final Pattern LIST = Pattern.compile("[\\s,]+", Pattern.UNICODE_CHARACTER_CLASS);

  private final Map<String, String> successors = new HashMap<>(); // address -> address
  private final Map<String, String> pageSuccessors = new HashMap<>(); // source -> source

  private Supersession() {}

  @Override
  public Optional<String> successor(String address) {
    return Optional.ofNullable(successors.get(address));
  }

  @Override
  public Optional<String> pageSuccessor(String source) {
    return Optional.ofNullable(pageSuccessors.get(source))
        .filter(replacement -> !replacement.equals(source));
  }

  static Supersession read(List<Page> pages, Map<String, Unit> units) {
    var supersession = new Supersession();
    var sources = new HashSet<String>();
    pages.forEach(page -> sources.add(page.source()));
    for (Page page : pages) {
      supersession.prose(page, sources, units);
      supersession.deltas(page, units);
      supersession.inPlace(page, units);
    }
    return supersession;
  }

  /** "Superseded by" in the words just before a link. */
  private void prose(Page page, Set<String> sources, Map<String, Unit> units) {
    String source = page.source();
    boolean pageSuperseded =
        page.html().select("spec-meta spec-status").stream()
            .anyMatch(status -> "superseded".equals(status.attr("value")));
    for (Element link : page.body().getAllElements()) {
      Target target = linkTarget(link, source, sources);
      if (target == null || !saysSupersededBy(link)) {
        continue;
      }
      String from = unitAround(source, link, units);
      String to = units.containsKey(target.address()) ? target.address() : null;
      if (from == null ? to == null && target.page().equals(source) : from.equals(to)) {
        continue; // a link to itself
      }
      if (from != null && to != null && units.get(from).lifecycle() != Lifecycle.CURRENT) {
        successors.putIfAbsent(from, to);
      } else if (pageSuperseded && !source.contains("/changes/")) {
        pageSuccessors.put(source, target.page());
      }
    }
  }

  /** Where a link points inside the archive: the page, and the address its fragment names. */
  private record Target(String page, String address) {}

  /** The link's target, or null if it is not a link, or leaves the archive, or points nowhere. */
  private static Target linkTarget(Element link, String source, Set<String> sources) {
    if (!link.normalName().equals("a") || !link.hasAttr("href")) {
      return null;
    }
    String href = link.attr("href");
    if (href.startsWith("http") || href.startsWith("mailto")) {
      return null;
    }
    String page = resolve(source, href);
    return sources.contains(page) ? new Target(page, page + "#" + fragment(href)) : null;
  }

  /** Whether the forty characters before the link say "superseded ... by". */
  private static boolean saysSupersededBy(Element link) {
    String context =
        link.parent() == null ? "" : lower(head(SpectasticReader.rawText(link.parent()), 200));
    String linkText = lower(SpectasticReader.rawText(link));
    String before = "";
    if (!linkText.isEmpty()) {
      int at = context.indexOf(linkText);
      before = tail(at < 0 ? context : context.substring(0, at), 40);
    }
    return before.contains("superseded") && before.contains("by");
  }

  /** A change proposal's {@code <spec-delta target="…">}: the old copy points at its successor. */
  private void deltas(Page page, Map<String, Unit> units) {
    String source = page.source();
    for (Element delta : page.body().getAllElements()) {
      if (!delta.normalName().equals("spec-delta") || delta.attr("target").isEmpty()) {
        continue;
      }
      var copy =
          SpectasticReader.first(
              delta,
              inside -> !inside.id().isEmpty() && units.containsKey(source + "#" + inside.id()));
      String from = copy != null ? source + "#" + copy.id() : unitAround(source, delta, units);
      for (String id : LIST.split(delta.attr("target"), -1)) {
        if (id.isEmpty()) {
          continue;
        }
        String to =
            source.contains("/changes/")
                ? find(
                    specDir(source), id, List.of("spec.html", "design.html", "tasks.html"), units)
                : find(source, id, units);
        if (to != null
            && from != null
            && !to.equals(from)
            && units.get(from).lifecycle() != Lifecycle.CURRENT
            && units.get(to).lifecycle() == Lifecycle.CURRENT) {
          successors.putIfAbsent(from, to);
        }
      }
    }
  }

  /** An element marked superseded in place: the first link after the mark is its successor. */
  private void inPlace(Page page, Map<String, Unit> units) {
    String source = page.source();
    for (Element element : page.body().getAllElements()) {
      String address = source + "#" + element.id();
      if (element.id().isEmpty()
          || !units.containsKey(address)
          || successors.containsKey(address)) {
        continue;
      }
      var mark =
          SpectasticReader.first(
              element,
              status ->
                  status.normalName().equals("spec-status")
                      && "superseded".equals(status.attr("value")));
      if (mark == null) {
        continue;
      }
      var replacement = next(page.html(), mark);
      if (replacement == null) {
        continue;
      }
      String to =
          resolve(source, replacement.attr("href")) + "#" + fragment(replacement.attr("href"));
      if (units.containsKey(to) && !to.equals(address)) {
        successors.put(address, to);
      }
    }
  }

  // --- helpers -------------------------------------------------------------------------------

  /** The unit an element sits in: itself or the nearest element around it that is a unit. */
  private static String unitAround(String source, Element el, Map<String, Unit> units) {
    for (Element around = el; around != null; around = around.parent()) {
      if (!around.id().isEmpty() && units.containsKey(source + "#" + around.id())) {
        return source + "#" + around.id();
      }
    }
    return null;
  }

  /** An id on this page, else on the spec's spec, tasks, design or verify page. */
  private static String find(String source, String id, Map<String, Unit> units) {
    if (units.containsKey(source + "#" + id)) {
      return source + "#" + id;
    }
    return find(
        specDir(source),
        id,
        List.of("spec.html", "tasks.html", "design.html", "verify.html"),
        units);
  }

  private static String find(
      String specDir, String id, List<String> files, Map<String, Unit> units) {
    for (String f : files) {
      String address = specDir + "/" + f + "#" + id;
      if (units.containsKey(address)) {
        return address;
      }
    }
    return null;
  }

  /** The first link with an href after {@code start}, in document order. */
  @SuppressWarnings("ReferenceEquality") // the node we were given, not one equal to it
  private static Element next(Element root, Element start) {
    boolean seen = false;
    for (Element element : root.getAllElements()) {
      if (element == start) {
        seen = true;
      } else if (seen && element.normalName().equals("a") && element.hasAttr("href")) {
        return element;
      }
    }
    return null;
  }

  /** The page an href points at, relative to the page it's on. */
  static String resolve(String source, String href) {
    int hash = href.indexOf('#');
    String path = hash < 0 ? href : href.substring(0, hash);
    if (path.isEmpty()) {
      return source;
    }
    var dir = Path.of(source).getParent();
    return (dir == null ? Path.of(path) : dir.resolve(path))
        .normalize()
        .toString()
        .replace('\\', '/');
  }

  private static String fragment(String href) {
    int hash = href.indexOf('#');
    return hash < 0 ? "" : href.substring(hash + 1);
  }

  private static String specDir(String source) {
    String[] parts = source.split("/", -1);
    return parts[0] + "/" + parts[1];
  }

  private static String lower(String text) {
    return text.toLowerCase(Locale.ROOT);
  }

  /** The first {@code wanted} characters (code points, not UTF-16 units). */
  private static String head(String text, int wanted) {
    return text.codePointCount(0, text.length()) <= wanted
        ? text
        : text.substring(0, text.offsetByCodePoints(0, wanted));
  }

  /** The last {@code wanted} characters. */
  private static String tail(String text, int wanted) {
    int count = text.codePointCount(0, text.length());
    return count <= wanted ? text : text.substring(text.offsetByCodePoints(0, count - wanted));
  }
}
