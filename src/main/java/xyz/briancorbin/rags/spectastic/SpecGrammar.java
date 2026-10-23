package xyz.briancorbin.rags.spectastic;

import java.util.LinkedHashSet;
import java.util.SequencedSet;
import java.util.regex.Pattern;
import xyz.briancorbin.rags.search.IdentifierGrammar;

/// Spectastic's names, and nothing else in the search knows them.
///
/// - an **id** is anything shaped like one — `FR-012`, `NFR-001`, `T-100`, `REQ-TOOL-001`, `US5`.
///   Ids are written in capitals, so this is case-sensitive: "d-001" in prose is not an id;
///
/// - a **scope** is a spec: "spec 040", "Spec 058's" or "slice 040" in any case, or a slug like
///   `040-run-budget`. Three digits throughout the archive today, but the number is read as
///   written, so a thousandth spec needs nothing changed here.
public final class SpecGrammar implements IdentifierGrammar {

  private static final Pattern ID =
      Pattern.compile(
          "(?<![\\w-])(?<prefix>[A-Z]+-)*[A-Z]+-?\\d+(?![\\w-])", Pattern.UNICODE_CHARACTER_CLASS);

  private static final Pattern SPEC =
      Pattern.compile(
          "\\b(?<specWord>spec|specs|slice)\\s+(?<inProse>\\d{3,})\\b"
              + "|(?<![\\w-])(?<inSlug>\\d{3,})-[a-z]",
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

  @Override
  public Named named(String question) {
    return new Named(ids(question), specs(question));
  }

  private static SequencedSet<String> ids(String question) {
    var ids = new LinkedHashSet<String>();
    var matching = ID.matcher(question);
    while (matching.find()) {
      ids.add(matching.group());
    }
    return ids;
  }

  private static SequencedSet<String> specs(String question) {
    var specs = new LinkedHashSet<String>();
    var matching = SPEC.matcher(question);
    while (matching.find()) {
      // One branch of SPEC matched, so exactly one of the two groups is set.
      String inProse = matching.group("inProse");
      specs.add(inProse != null ? inProse : matching.group("inSlug"));
    }
    return specs;
  }

  /// The number of the spec an address belongs to, however many digits it has: `specs/045-…` is
  /// "045" and `specs/1000-…` is "1000". Read to the slug's first dash rather than to a fixed
  /// width, so that a thousandth spec cannot read as the hundredth.
  @Override
  public String scopeOf(String address) {
    String slug = address.split("/", -1)[1];
    int dash = slug.indexOf('-');
    return dash < 0 ? slug : slug.substring(0, dash);
  }
}
