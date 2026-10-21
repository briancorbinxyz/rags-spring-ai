package xyz.briancorbin.rags;

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
import xyz.briancorbin.rags.ElementReader.Page;
import xyz.briancorbin.rags.ElementReader.Unit;

/**
 * The paper trail: which current element replaced each out-of-date one (Episode 4, Step 4).
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
public final class Supersession {

  private static final Pattern LIST = Pattern.compile("[\\s,]+", Pattern.UNICODE_CHARACTER_CLASS);

  private final Map<String, String> successors = new HashMap<>(); // address -> address
  private final Map<String, String> pageSuccessors = new HashMap<>(); // source -> source

  private Supersession() {}

  /** The current element that replaced the one at {@code address}, if one did. */
  public Optional<String> successor(String address) {
    return Optional.ofNullable(successors.get(address));
  }

  /** The page that replaced the page {@code source}, if one did. */
  public Optional<String> pageSuccessor(String source) {
    return Optional.ofNullable(pageSuccessors.get(source)).filter(s -> !s.equals(source));
  }

  static Supersession read(List<Page> pages, Map<String, Unit> units) {
    var s = new Supersession();
    Set<String> sources = new HashSet<>();
    pages.forEach(p -> sources.add(p.source()));
    for (Page page : pages) {
      s.prose(page, sources, units);
      s.deltas(page, units);
      s.inPlace(page, units);
    }
    return s;
  }

  /** "Superseded by" in the words just before a link. */
  private void prose(Page page, Set<String> sources, Map<String, Unit> units) {
    String source = page.source();
    boolean pageSuperseded =
        page.html().select("spec-meta spec-status").stream().anyMatch(e -> "superseded".equals(e.attr("value")));
    for (Element a : page.body().getAllElements()) {
      if (!a.normalName().equals("a") || !a.hasAttr("href")) {
        continue;
      }
      String href = a.attr("href");
      if (href.startsWith("http") || href.startsWith("mailto")) {
        continue;
      }
      String target = resolve(source, href);
      String fragment = fragment(href);
      if (!sources.contains(target)) {
        continue;
      }
      String from = unitAround(source, a, units);
      String to = units.containsKey(target + "#" + fragment) ? target + "#" + fragment : null;
      if (from == null ? to == null && target.equals(source) : from.equals(to)) {
        continue; // a link to itself
      }
      String context = a.parent() == null ? "" : lower(head(ElementReader.rawText(a.parent()), 200));
      String linkText = lower(ElementReader.rawText(a));
      String before = "";
      if (!linkText.isEmpty()) {
        int at = context.indexOf(linkText);
        before = tail(at < 0 ? context : context.substring(0, at), 40);
      }
      if (!(before.contains("superseded") && before.contains("by"))) {
        continue;
      }
      if (from != null && to != null && units.get(from).lifecycle() != Lifecycle.CURRENT) {
        successors.putIfAbsent(from, to);
      } else if (pageSuperseded && !source.contains("/changes/")) {
        pageSuccessors.put(source, target);
      }
    }
  }

  /** A change proposal's {@code <spec-delta target="…">}: the old copy points at its successor. */
  private void deltas(Page page, Map<String, Unit> units) {
    String source = page.source();
    for (Element e : page.body().getAllElements()) {
      if (!e.normalName().equals("spec-delta") || e.attr("target").isEmpty()) {
        continue;
      }
      Element copy = ElementReader.first(e, k -> !k.id().isEmpty() && units.containsKey(source + "#" + k.id()));
      String from = copy != null ? source + "#" + copy.id() : unitAround(source, e, units);
      for (String id : LIST.split(e.attr("target"))) {
        if (id.isEmpty()) {
          continue;
        }
        String to =
            source.contains("/changes/")
                ? find(specDir(source), id, List.of("spec.html", "design.html", "tasks.html"), units)
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
    for (Element e : page.body().getAllElements()) {
      String address = source + "#" + e.id();
      if (e.id().isEmpty() || !units.containsKey(address) || successors.containsKey(address)) {
        continue;
      }
      Element mark =
          ElementReader.first(e, k -> k.normalName().equals("spec-status") && "superseded".equals(k.attr("value")));
      if (mark == null) {
        continue;
      }
      Element link = next(page.html(), mark);
      if (link == null) {
        continue;
      }
      String to = resolve(source, link.attr("href")) + "#" + fragment(link.attr("href"));
      if (units.containsKey(to) && !to.equals(address)) {
        successors.put(address, to);
      }
    }
  }

  // --- helpers -------------------------------------------------------------------------------

  /** The unit an element sits in: itself or the nearest element around it that is a unit. */
  private static String unitAround(String source, Element el, Map<String, Unit> units) {
    for (Element e = el; e != null; e = e.parent()) {
      if (!e.id().isEmpty() && units.containsKey(source + "#" + e.id())) {
        return source + "#" + e.id();
      }
    }
    return null;
  }

  /** An id on this page, else on the spec's spec, tasks, design or verify page. */
  private static String find(String source, String id, Map<String, Unit> units) {
    if (units.containsKey(source + "#" + id)) {
      return source + "#" + id;
    }
    return find(specDir(source), id, List.of("spec.html", "tasks.html", "design.html", "verify.html"), units);
  }

  private static String find(String specDir, String id, List<String> files, Map<String, Unit> units) {
    for (String f : files) {
      String address = specDir + "/" + f + "#" + id;
      if (units.containsKey(address)) {
        return address;
      }
    }
    return null;
  }

  /** The first link with an href after {@code start}, in document order. */
  private static Element next(Element root, Element start) {
    boolean seen = false;
    for (Element e : root.getAllElements()) {
      if (e == start) {
        seen = true;
      } else if (seen && e.normalName().equals("a") && e.hasAttr("href")) {
        return e;
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
    Path dir = Path.of(source).getParent();
    return (dir == null ? Path.of(path) : dir.resolve(path)).normalize().toString().replace('\\', '/');
  }

  private static String fragment(String href) {
    int hash = href.indexOf('#');
    return hash < 0 ? "" : href.substring(hash + 1);
  }

  private static String specDir(String source) {
    String[] parts = source.split("/");
    return parts[0] + "/" + parts[1];
  }

  private static String lower(String s) {
    return s.toLowerCase(Locale.ROOT);
  }

  /** The first {@code n} characters (code points, not UTF-16 units). */
  private static String head(String s, int n) {
    return s.codePointCount(0, s.length()) <= n ? s : s.substring(0, s.offsetByCodePoints(0, n));
  }

  /** The last {@code n} characters. */
  private static String tail(String s, int n) {
    int count = s.codePointCount(0, s.length());
    return count <= n ? s : s.substring(s.offsetByCodePoints(0, count - n));
  }
}
