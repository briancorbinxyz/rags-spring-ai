package xyz.briancorbin.rags.eval;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import xyz.briancorbin.rags.search.ElementRetriever;
import xyz.briancorbin.rags.search.IdentifierLookup;
import xyz.briancorbin.rags.search.KeywordSearch;
import xyz.briancorbin.rags.search.PostProcessors;
import xyz.briancorbin.rags.search.StopWords;
import xyz.briancorbin.rags.search.SupersessionRedirect;
import xyz.briancorbin.rags.search.TopDocuments;
import xyz.briancorbin.rags.spectastic.Lifecycle;
import xyz.briancorbin.rags.spectastic.SpecCorpus;
import xyz.briancorbin.rags.spectastic.SpecGrammar;
import xyz.briancorbin.rags.spectastic.SpectasticReader;
import xyz.briancorbin.rags.units.ElementUnits;
import xyz.briancorbin.rags.units.StandingFilter;

/// How many of Episode 4's hits are flattering: scored on a unit whose retrieved **part** does not
/// hold the answer.
///
/// A long element is split into parts that share one address, and a hit is scored by address, so a
/// question counts as found when any part comes back — including a part that does not contain the
/// sentence the question is about. The reader is shown that part and nothing else, so the score and
/// the reader can disagree. RAG-19 asked how often, and this is the how often.
///
/// **The answer-bearing part is decided by a heuristic, and the report says so.** There is no
/// ground truth at part granularity: the golden set records the address a question is answered at,
/// not which piece of a split element holds it. So the part whose words most overlap the question's
/// recorded answer is taken to be the one. That is good enough to bound the problem — it cannot
/// turn a one-part unit into a split one — and not good enough to publish a precise figure from,
/// which is why what it writes is a count and a list of questions rather than a rate.
///
/// Needs, and fails rather than skips under `ratchetTest`: the spectastic checkout at the pinned
/// commit. No model, no embeddings, no reranker — this is the keyword line only.
class SplitPartHitsTest {

  /// The reader's window, which is the only part of the ranking anybody sees.
  private static final int K = 4;

  private static final Path REPORT = Path.of("build/episode-4/split-part-hits.md");
  private static final Pattern WORDS = Pattern.compile("[^a-z0-9]+");

  private static KeywordSearch keywords;
  private static List<GoldenSet.Question> questions;
  private static List<RetrievalRun.Result> results;

  @BeforeAll
  static void theArchiveIsRead() {
    Path checkout = frozenCheckout();
    questions = BlindAudit.audited(GoldenSet.read(new ClassPathResource("golden/questions.jsonl")));
    keywords =
        KeywordSearch.of(
            new ElementUnits(new SpectasticReader(checkout).get(), Lifecycle::of),
            StopWords.of("stopwords/english.txt", "stopwords/spectastic.txt"));
    results = RetrievalRun.run(episode4sLastStep(), questions);
  }

  @AfterAll
  static void theKeywordTableIsClosed() {
    if (keywords != null) {
      keywords.close();
    }
  }

  /// One question's hit, and whether the part that scored it is the part holding the answer.
  private record Scored(String id, String address, int parts, int answerPart, int shownPart) {

    boolean split() {
      return parts > 1;
    }

    boolean flattering() {
      return split() && shownPart != answerPart;
    }
  }

  @Test
  void countsTheHitsScoredOnAPartThatDoesNotHoldTheAnswer() {
    List<Scored> scored = everyHit();
    List<Scored> split = scored.stream().filter(Scored::split).toList();
    List<Scored> flattering = scored.stream().filter(Scored::flattering).toList();

    write(report(scored, split, flattering));

    // Not a threshold anybody tuned: it asserts the report was produced over the frozen set, so a
    // change to the splitter or the units cannot quietly empty this measurement.
    assertTrue(!scored.isEmpty(), "no hits at all on Episode 4's last step, which cannot be right");
  }

  // --- measuring ---------------------------------------------------------------------------------

  /// Every answerable question whose expected or accepted passage is in the top {@value #K}, with
  /// the part that scored it and the part that holds its answer.
  private static List<Scored> everyHit() {
    var out = new ArrayList<Scored>();
    for (RetrievalRun.Result result : results) {
      GoldenSet.Question question = result.question();
      Integer rank = result.score().firstHit();
      if (question.stratum().equals("unanswerable") || rank == null || rank > K) {
        continue;
      }
      String address = result.ranked().get(rank - 1);
      List<Document> parts = keywords.units().parts(address);
      if (parts.isEmpty()) {
        continue; // redirected to a unit this reader no longer has; not a split-part question
      }
      out.add(
          new Scored(
              question.id(),
              address,
              parts.size(),
              bestOverlap(parts, question),
              whichPartWasShown(address, question)));
    }
    return out;
  }

  /// The part whose words most overlap the question's recorded answer, one-based. The heuristic.
  private static int bestOverlap(List<Document> parts, GoldenSet.Question question) {
    Set<String> answer = words(question.core());
    int best = 1;
    int most = -1;
    for (int part = 0; part < parts.size(); part++) {
      Set<String> shared = new HashSet<>(words(parts.get(part).getText()));
      shared.retainAll(answer);
      if (shared.size() > most) {
        most = shared.size();
        best = part + 1;
      }
    }
    return best;
  }

  /// Which part of the unit the search actually ranked into the window, one-based. Re-ranks the
  /// unit's own parts the way the keyword search would, because the run file records the address
  /// and a split unit's parts all share it.
  private static int whichPartWasShown(String address, GoldenSet.Question question) {
    List<Document> parts = keywords.units().parts(address);
    if (parts.size() == 1) {
      return 1;
    }
    List<Document> ranked = keywords.rank(question.question(), parts);
    Document shown = ranked.isEmpty() ? parts.getFirst() : ranked.getFirst();
    for (int part = 0; part < parts.size(); part++) {
      if (parts.get(part).getId().equals(shown.getId())) {
        return part + 1;
      }
    }
    return 1;
  }

  /// The words worth comparing: lowercase, longer than three letters, each once. Short words are
  /// dropped because every part of every element shares them.
  private static Set<String> words(String text) {
    return Stream.of(WORDS.split(text == null ? "" : text.toLowerCase(Locale.ROOT)))
        .filter(word -> word.length() > 3)
        .collect(Collectors.toSet());
  }

  // --- reporting ---------------------------------------------------------------------------------

  private static String report(List<Scored> scored, List<Scored> split, List<Scored> flattering) {
    return summary(scored, split, flattering) + detail(split);
  }

  /// What was counted, and the caveat on how, so the table is never read without it.
  ///
  /// Two methods rather than one because the prose and the counts answer to different rules: the
  /// method-length limit is the right limit for code and the wrong one for a paragraph, and
  /// hoisting the paragraph into a constant to get under it only trades that complaint for Error
  /// Prone's `InlineFormatString`. Two short format strings, each used where it is written, and
  /// neither rule has anything to say.
  private static String summary(List<Scored> scored, List<Scored> split, List<Scored> flattering) {
    return caveat() + counts(scored, split, flattering);
  }

  private static String caveat() {
    return """
        # Hits scored on a split unit

        Episode 4's last step, keyword line, at K=%d over the frozen set. A hit is scored by
        address; a long element is split into parts that share one. So a question can count as
        found while the reader is shown a part that does not hold the answer.

        **Which part holds the answer is a heuristic** — the part whose words most overlap the
        question's recorded answer. The golden set records addresses, not parts, so there is no
        ground truth at this granularity. Read the counts as a bound, not a rate.

        """
        .formatted(K);
  }

  private static String counts(List<Scored> scored, List<Scored> split, List<Scored> flattering) {
    return """
        | | count |
        |---|---|
        | units in the archive | %d |
        | of those, split into more than one part | %d (%.1f%%) |
        | hits at K=%d | %d |
        | of those, on a unit with more than one part | %d |
        | of those, shown a part that is not the answer-bearing one | %d |

        """
        .formatted(
            units(),
            splitUnits(),
            100.0 * splitUnits() / units(),
            K,
            scored.size(),
            split.size(),
            flattering.size());
  }

  /// Every hit that landed on a split unit, named, because two of them is few enough to check by
  /// hand and a reader should not have to take the count on trust.
  private static String detail(List<Scored> split) {
    if (split.isEmpty()) {
      return "No hit in the window landed on a split unit, so this cannot be happening here.\n";
    }
    var out = new StringBuilder("## Every hit on a split unit\n\n");
    out.append("| question | address | parts | answer in part | shown part | flattering |\n");
    out.append("|---|---|---|---|---|---|\n");
    for (Scored row : split.stream().sorted(Comparator.comparing(Scored::id)).toList()) {
      out.append(
          "| %s | `%s` | %d | %d | %d | %s |%n"
              .formatted(
                  row.id(),
                  row.address(),
                  row.parts(),
                  row.answerPart(),
                  row.shownPart(),
                  row.flattering() ? "**yes**" : "no"));
    }
    return out.toString();
  }

  /// Addresses in the archive, each once however many parts it has.
  private static int units() {
    return keywords.units().addresses().size();
  }

  /// How many of those are long enough to have been split. Reported so that a count of zero
  /// flattering hits is read as "splitting is rare and lands well" rather than "nothing is split".
  private static long splitUnits() {
    return keywords.units().addresses().stream()
        .filter(address -> keywords.units().parts(address).size() > 1)
        .count();
  }

  private static void write(String report) {
    try {
      Files.createDirectories(REPORT.getParent());
      Files.writeString(REPORT, report, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    System.out.println(report);
  }

  // --- given -----------------------------------------------------------------------------------

  private static Path frozenCheckout() {
    Path checkout = Path.of(System.getProperty("spectastic.path", "../spectastic"));
    RatchetInputs.present(
        Files.isDirectory(checkout.resolve("specs")),
        "no spectastic checkout at " + checkout.toAbsolutePath());
    RatchetInputs.present(
        SpecCorpus.commitOf(checkout).equals(RatchetTest.COMMIT),
        "the golden set is frozen at " + RatchetTest.COMMIT);
    return checkout;
  }

  private static RetrievalRun.Step episode4sLastStep() {
    return new RetrievalRun.Step(
        "step-4",
        "+ supersession redirect",
        ElementRetriever.builder()
            .keywords(keywords)
            .identifiers(new IdentifierLookup(keywords.units(), new SpecGrammar()))
            .filter(StandingFilter.of(Lifecycle.IN_FORCE))
            .build(),
        PostProcessors.of(
            new TopDocuments(ElementRetriever.DEPTH), new SupersessionRedirect(keywords)));
  }
}
