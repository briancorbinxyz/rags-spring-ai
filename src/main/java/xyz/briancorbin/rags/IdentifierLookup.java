package xyz.briancorbin.rags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.document.Document;

/**
 * Finds the passage a question names, so it doesn't have to be searched for (Episode 4, Step 2).
 *
 * <p>"What does FR-001 in spec 045 require?" names an element id and a spec. {@code FR-001} alone
 * is in over a hundred specs, so:
 *
 * <ul>
 *   <li>an <b>id</b> is anything shaped like one ({@code FR-012}, {@code NFR-001}, {@code T-100},
 *       {@code REQ-TOOL-001}, {@code US5}) that is also an element id somewhere in the archive.
 *       Ids are written in capitals, so this is case-sensitive: "d-001" in prose is not an id;
 *   <li>a <b>spec</b> is "spec 040", "Spec 058's" or "slice 040" in any case, or a slug like
 *       {@code 040-run-budget};
 *   <li>an id with a spec names that id in that spec, on every page of it. An id with no spec
 *       names something only if exactly one spec has it.
 * </ul>
 *
 * <p>The addresses come back current first, then applied history, then the rest; pages outside
 * {@code changes/} before change proposals; then archive order.
 */
public final class IdentifierLookup {

  private static final Pattern ID =
      Pattern.compile("(?<![\\w-])(?<prefix>[A-Z]+-)*[A-Z]+-?\\d+(?![\\w-])", Pattern.UNICODE_CHARACTER_CLASS);

  private static final Pattern SPEC =
      Pattern.compile(
          "\\b(?<specWord>spec|specs|slice)\\s+(?<inProse>\\d{3})\\b|(?<![\\w-])(?<inSlug>\\d{3})-[a-z]",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

  private final ElementUnits units;
  private final Set<String> anchors;
  private final Map<String, List<String>> byAnchor = new LinkedHashMap<>(); // id -> addresses

  public IdentifierLookup(ElementUnits units) {
    this.units = units;
    this.anchors = units.anchors();
    for (String address : units.addresses()) {
      byAnchor.computeIfAbsent(address.substring(address.indexOf('#') + 1), a -> new ArrayList<>()).add(address);
    }
  }

  /** What the question names. */
  record Named(List<String> ids, List<String> specs) {}

  Named parse(String question) {
    var ids = new ArrayList<String>();
    Matcher m = ID.matcher(question);
    while (m.find()) {
      if (anchors.contains(m.group()) && !ids.contains(m.group())) {
        ids.add(m.group());
      }
    }
    var specs = new ArrayList<String>();
    Matcher s = SPEC.matcher(question);
    while (s.find()) {
      String spec = s.group("inProse") != null ? s.group("inProse") : s.group("inSlug");
      if (!specs.contains(spec)) {
        specs.add(spec);
      }
    }
    return new Named(ids, specs);
  }

  /** The addresses of the units the question names, in the order they should be pinned. */
  public List<String> lookup(String question) {
    Named named = parse(question);
    var out = new ArrayList<String>();
    for (String id : named.ids()) {
      List<String> addresses = byAnchor.getOrDefault(id, List.of());
      if (!named.specs().isEmpty()) {
        addresses = addresses.stream().filter(a -> named.specs().contains(spec(a).substring(0, 3))).toList();
      } else if (addresses.stream().map(IdentifierLookup::spec).distinct().count() != 1) {
        addresses = List.of(); // FR-001 alone would pin over a hundred units
      }
      addresses.stream().filter(a -> !out.contains(a)).forEach(out::add);
    }
    out.sort(
        Comparator.<String>comparingInt(a -> rank(ElementReader.lifecycle(first(a))))
            .thenComparing(a -> a.contains("/changes/"))
            .thenComparingInt(a -> units.row(first(a))));
    return out;
  }

  private Document first(String address) {
    return units.parts(address).getFirst();
  }

  private static int rank(Lifecycle lifecycle) {
    return switch (lifecycle) {
      case CURRENT -> 0;
      case HISTORY -> 1;
      default -> 2;
    };
  }

  private static String spec(String address) {
    return address.split("/")[1];
  }
}
