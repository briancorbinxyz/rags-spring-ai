package xyz.briancorbin.rags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.SequencedSet;
import java.util.Set;
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
 *       {@code 040-run-budget}. Three digits throughout the archive today, but the number is read
 *       as written, so a thousandth spec needs nothing changed here;
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
          "\\b(?<specWord>spec|specs|slice)\\s+(?<inProse>\\d{3,})\\b|(?<![\\w-])(?<inSlug>\\d{3,})-[a-z]",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

  private final ElementUnits units;
  private final Set<String> anchors;
  private final Map<String, List<String>> byAnchor = new LinkedHashMap<>(); // id -> addresses

  public IdentifierLookup(ElementUnits units) {
    this.units = units;
    this.anchors = units.anchors();
    for (String address : units.addresses()) {
      byAnchor
          .computeIfAbsent(address.substring(address.indexOf('#') + 1), id -> new ArrayList<>())
          .add(address);
    }
  }

  /** What the question names: each id and each spec once, in the order the question said them. */
  record Named(SequencedSet<String> ids, SequencedSet<String> specs) {}

  Named parse(String question) {
    var ids = new LinkedHashSet<String>();
    var matchingIds = ID.matcher(question);
    while (matchingIds.find()) {
      String id = matchingIds.group();
      if (anchors.contains(id)) {
        ids.add(id);
      }
    }
    var specs = new LinkedHashSet<String>();
    var matchingSpecs = SPEC.matcher(question);
    while (matchingSpecs.find()) {
      String inProse = matchingSpecs.group("inProse"); // one branch matched, so one of the two is set
      specs.add(inProse != null ? inProse : matchingSpecs.group("inSlug"));
    }
    return new Named(ids, specs);
  }

  /** The addresses of the units the question names, in the order they should be pinned. */
  public SequencedCollection<String> lookup(String question) {
    Named named = parse(question);
    var pinned = new LinkedHashSet<String>(); // two ids can name one address; it is pinned once
    for (String id : named.ids()) {
      pinned.addAll(addressesNamedBy(id, named.specs()));
    }
    return pinned.stream().sorted(pinOrder()).toList();
  }

  /** Every address carrying {@code id}, narrowed to {@code specs} if the question named any. */
  private List<String> addressesNamedBy(String id, Set<String> specs) {
    List<String> addresses = byAnchor.getOrDefault(id, List.of());
    if (!specs.isEmpty()) {
      return addresses.stream().filter(address -> specs.contains(specOf(address))).toList();
    }
    if (addresses.stream().map(IdentifierLookup::specOf).distinct().count() != 1) {
      return List.of(); // FR-001 alone would pin over a hundred units
    }
    return addresses;
  }

  /** Current first, then applied history; pages outside {@code changes/} first; then archive order. */
  private Comparator<String> pinOrder() {
    return Comparator.<String>comparingInt(address -> rank(lifecycleOf(address)))
        .thenComparing(address -> address.contains("/changes/"))
        .thenComparingInt(address -> units.row(firstPart(address)));
  }

  private Document firstPart(String address) {
    return units.parts(address).getFirst();
  }

  private Lifecycle lifecycleOf(String address) {
    return ElementReader.lifecycle(firstPart(address));
  }

  private static int rank(Lifecycle lifecycle) {
    return switch (lifecycle) {
      case CURRENT -> 0;
      case HISTORY -> 1;
      default -> 2;
    };
  }

  /**
   * The number of the spec an address belongs to, however many digits it has: {@code specs/045-…}
   * is "045" and {@code specs/1000-…} is "1000". Read to the slug's first dash rather than to a
   * fixed width, so that a thousandth spec cannot read as the hundredth.
   */
  private static String specOf(String address) {
    String slug = address.split("/")[1];
    int dash = slug.indexOf('-');
    return dash < 0 ? slug : slug.substring(0, dash);
  }
}
