package xyz.briancorbin.rags.eval;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import xyz.briancorbin.rags.answer.Archive;
import xyz.briancorbin.rags.answer.CitationChecker;
import xyz.briancorbin.rags.answer.FactChecker.Tally;
import xyz.briancorbin.rags.answer.Quote;
import xyz.briancorbin.rags.answer.QuoteResolver;
import xyz.briancorbin.rags.eval.GoldenScorer.Hit;
import xyz.briancorbin.rags.eval.GoldenScorer.Score;
import xyz.briancorbin.rags.eval.GoldenSet.Question;
import xyz.briancorbin.rags.spectastic.SpecCorpus;
import xyz.briancorbin.rags.units.Units;

/// Runs every golden question through the archive and writes down what happened.
///
/// Three files go to `build/`, named `episode-3-<model>-<mode>-k4`:
/// - `.csv`: one row per question, with the retrieval score, Quoter's traced quotations, Answerer's
///   claims by verdict, abstention, stale answers, tokens and time;
/// - `.jsonl`: the same rows in full, with every chunk retrieved, every quotation and every claim,
///   so a failure can be diagnosed from the run itself;
/// - `-summary.md`: the per-stratum table the article prints.
///
/// Every row records the commit, the models and a hash of the prompts. Change any of them and the
/// numbers are a new baseline, not a comparison.
public final class GoldenRun {

  private static final Logger log = LoggerFactory.getLogger(GoldenRun.class);

  /// The advisor's default: four chunks per question.
  static final int K = 4;

  /// Words that mark an answer as knowing its source is out of date. A heuristic, said so.
  private static final Pattern FLAGS_STALE =
      Pattern.compile(
          "(?i)\\b(?<staleWord>withdrawn|superseded|no longer|retired|archived|was proposed)\\b");

  /// How the tokens were chosen, and by which build of Ollama.
  ///
  /// Spring AI omits an option it was not given and Ollama fills in its own, so an unpinned run
  /// samples with a random seed and cannot be repeated. These are the values {@link
  /// xyz.briancorbin.rags.ArchiveConfiguration#pinned} sends. The server version is here too: the
  /// digest says which weights, this says which build of the runtime drew from them, and Ollama has
  /// changed its defaults between releases. The version is empty when Ollama did not answer, which
  /// is recorded rather than guessed.
  public record Sampling(
      int seed, double topP, int topK, String keepAlive, Optional<String> ollamaVersion) {}

  /// What the run was measured on.
  public record Setup(
      SpecCorpus.Manifest corpus,
      String chatModel,
      String embeddingModel,
      Map<String, String> digests,
      Sampling sampling,
      String promptsHash) {}

  /// Everything the stamp carries except the corpus, which only the run can know, because the
  /// manifest is what opening the archive returns. Collected so that the conditions travel as one
  /// thing rather than as five parameters that must stay in the right order.
  public record Conditions(
      String chatModel,
      String embeddingModel,
      Map<String, String> digests,
      Sampling sampling,
      String promptsHash) {

    public Setup over(SpecCorpus.Manifest corpus) {
      return new Setup(corpus, chatModel, embeddingModel, digests, sampling, promptsHash);
    }
  }

  /// One question's outcome.
  public record Row(
      Question question,
      Archive.Ruling ruling,
      Score score,
      Double quoterVerified,
      Tally answererTally,
      boolean staleRetrieved,
      boolean staleCited,
      boolean staleFlagged) {

    boolean staleAsCurrent() {
      return staleCited && !staleFlagged;
    }

    int tokens() {
      return value(ruling.quoter().cost().promptTokens())
          + value(ruling.answerer().cost().promptTokens());
    }
  }

  /// How a run's results are compared with the golden set, which depends on what was retrieved.
  ///
  /// Episode 3's 800-token chunks need `OVERLAP`: a chunk holds part of an element, sometimes most
  /// of it, so the only honest question is how much. Element units are addresses, and an address
  /// either matches or does not, which is what the ratchet has always scored.
  ///
  /// Scoring elements as though they were chunks is not a rounding error. It drops the `#anchor`,
  /// compares the page, and then measures text overlap on a fragment far smaller than the chunks
  /// the thresholds were chosen for, so it under-counts badly and silently.
  public enum By {
    OVERLAP,
    ADDRESS
  }

  private final Archive archive;
  private final GoldenScorer scorer;
  private final CitationChecker citations;
  private final By by;

  public GoldenRun(Archive archive, GoldenScorer scorer, CitationChecker citations, By by) {
    this.archive = archive;
    this.scorer = scorer;
    this.citations = citations;
    this.by = by;
  }

  public List<Row> run(List<Question> questions) {
    var rows = new ArrayList<Row>();
    for (Question q : questions) {
      // A fresh conversation per question: no question's memory leaks into the next.
      var ruling = archive.ask(q.question(), "golden-" + q.id() + "-" + UUID.randomUUID());
      rows.add(row(q, ruling));
      log.info(
          "{} [{}] hit@{}={} rank={}",
          q.id(),
          q.stratum(),
          K,
          rows.getLast().score().hitAt(K),
          rows.getLast().score().firstHit());
    }
    return List.copyOf(rows);
  }

  /// The quotations the answer leaned on: Answerer's citations in duo mode, all of them in solo.
  private SequencedCollection<Quote> citedBy(Archive.Ruling ruling) {
    if (ruling.mode() != Archive.Mode.QUOTED) {
      return ruling.quotes();
    }
    return ruling
        .answerer()
        .said()
        .map(said -> citations.cited(said, ruling.quotes()))
        .orElseGet(LinkedHashSet::new);
  }

  /// Whether any of the question's stale records is in the top four Quoter was handed.
  private boolean anyStaleAmong(Question question, List<Document> top) {
    return top.stream()
        .anyMatch(
            chunk ->
                question.stale().stream().anyMatch(stale -> scorer.hit(stale, chunk) != Hit.NONE));
  }

  /// Whether a quotation Answerer relied on came from a stale element itself, rather than merely
  /// from a chunk that also happens to hold it.
  private boolean anyStaleQuotedIn(Question question, SequencedCollection<Quote> cited) {
    List<String> staleTexts =
        question.stale().stream()
            .map(stale -> scorer.elementText(stale).map(QuoteResolver::normalise).orElse(""))
            .toList();
    return cited.stream()
        .map(quote -> QuoteResolver.normalise(quote.text()))
        .anyMatch(
            quoted ->
                staleTexts.stream().anyMatch(text -> !text.isEmpty() && text.contains(quoted)));
  }

  Row row(Question question, Archive.Ruling ruling) {
    List<Document> top = ruling.retrieved().subList(0, Math.min(K, ruling.retrieved().size()));
    return new Row(
        question,
        ruling,
        scoreOf(question, top),
        verifiedShare(ruling),
        Tally.of(ruling.answerer().verdicts()),
        anyStaleAmong(question, top),
        anyStaleQuotedIn(question, citedBy(ruling)),
        flagsStale(ruling));
  }

  /// The row's score, by whichever comparison suits what was retrieved.
  private Score scoreOf(Question question, List<Document> top) {
    return by == By.ADDRESS
        ? GoldenScorer.byAddress(question, top.stream().map(Units::address).toList())
        : scorer.score(question, top);
  }

  /// How much of what the quoter offered traced word for word; nothing to share in direct mode.
  private static Double verifiedShare(Archive.Ruling ruling) {
    return ruling.mode() == Archive.Mode.QUOTED
        ? new QuoteResolver.Resolution(ruling.quoter().verdicts(), ruling.quotes()).verifiedShare()
        : null;
  }

  /// Whether the answer says, in words, that what it found is out of date. A heuristic, said so.
  private static boolean flagsStale(Archive.Ruling ruling) {
    return ruling
        .answerer()
        .said()
        .map(said -> FLAGS_STALE.matcher(said.answer()).find())
        .orElse(false);
  }

  private static int value(Integer i) {
    return i == null ? 0 : i;
  }

  /// Writes the three files and returns the summary table.
  public String write(List<Row> rows, Setup setup, Path dir) {
    String name =
        "episode-3-%s-%s-k%d"
            .formatted(
                setup.chatModel().replaceAll("[^A-Za-z0-9.-]", "_"),
                archive.mode().name().toLowerCase(Locale.ROOT),
                K);
    String summary = RunSummary.summary(rows, setup);
    try {
      Files.createDirectories(dir);
      Files.write(dir.resolve(name + ".csv"), RunCsv.csv(rows, setup));
      Files.write(dir.resolve(name + ".jsonl"), RunJsonl.jsonLines(rows, setup));
      Files.writeString(dir.resolve(name + "-summary.md"), summary);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    log.info("Wrote {}.csv, .jsonl and -summary.md to {}", name, dir);
    return summary;
  }

  /// Short hash of the persona prompts, so a prompt change shows up in every row.
  public static String hash(List<String> prompts) {
    try {
      var digest = java.security.MessageDigest.getInstance("SHA-256");
      for (String prompt : prompts) {
        digest.update(prompt.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      return java.util.HexFormat.of().formatHex(digest.digest()).substring(0, 12);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
