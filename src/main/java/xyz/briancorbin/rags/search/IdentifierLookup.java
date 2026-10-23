package xyz.briancorbin.rags.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.Set;
import org.springframework.ai.document.Document;
import xyz.briancorbin.rags.units.ElementUnits;
import xyz.briancorbin.rags.units.Standing;

/// Finds the passage a question names, so it does not have to be searched for.
///
/// "What does FR-001 in spec 045 require?" names a part of the archive and a scope to look for it
/// in. `FR-001` alone is in over a hundred specs, so:
///
/// - an id with a scope names that id in that scope, on every page of it;
/// - an id with no scope names something only if exactly one scope has it.
///
/// What a name looks like is the {@link IdentifierGrammar}'s business; which names exist is this
/// class's, because it is the one holding the units. The addresses come back by {@link
/// Standing#precedence()}, then pages outside `changes/` before change proposals, then archive
/// order.
public final class IdentifierLookup {

  private final ElementUnits units;
  private final IdentifierGrammar grammar;
  private final Set<String> anchors;
  private final Map<String, List<String>> byAnchor = new LinkedHashMap<>(); // id -> addresses

  public IdentifierLookup(ElementUnits units, IdentifierGrammar grammar) {
    this.units = units;
    this.grammar = grammar;
    this.anchors = units.anchors();
    for (String address : units.addresses()) {
      byAnchor
          .computeIfAbsent(address.substring(address.indexOf('#') + 1), id -> new ArrayList<>())
          .add(address);
    }
  }

  /// The ids the question names that this archive actually has, and the scopes it named them by.
  IdentifierGrammar.Named parse(String question) {
    IdentifierGrammar.Named named = grammar.named(question);
    var ids = new LinkedHashSet<String>();
    named.ids().stream().filter(anchors::contains).forEach(ids::add);
    return new IdentifierGrammar.Named(ids, named.scopes());
  }

  /// The addresses of the units the question names, in the order they should be pinned.
  public SequencedCollection<String> lookup(String question) {
    IdentifierGrammar.Named named = parse(question);
    var pinned = new LinkedHashSet<String>(); // two ids can name one address; it is pinned once
    for (String id : named.ids()) {
      pinned.addAll(addressesNamedBy(id, named.scopes()));
    }
    return pinned.stream().sorted(pinOrder()).toList();
  }

  /// Every address carrying `id`, narrowed to `scopes` if the question named any.
  private List<String> addressesNamedBy(String id, Set<String> scopes) {
    List<String> addresses = byAnchor.getOrDefault(id, List.of());
    if (!scopes.isEmpty()) {
      return addresses.stream()
          .filter(address -> scopes.contains(grammar.scopeOf(address)))
          .toList();
    }
    if (addresses.stream().map(grammar::scopeOf).distinct().count() != 1) {
      return List.of(); // FR-001 alone would pin over a hundred units
    }
    return addresses;
  }

  /// Most authoritative first; pages outside `changes/` first; then archive order.
  private Comparator<String> pinOrder() {
    return Comparator.<String>comparingInt(address -> standingOf(address).precedence())
        .thenComparing(address -> address.contains("/changes/"))
        .thenComparingInt(address -> units.row(firstPart(address)));
  }

  private Document firstPart(String address) {
    return units.parts(address).getFirst();
  }

  private Standing standingOf(String address) {
    return units.standing(firstPart(address));
  }
}
