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

/**
 * Runs every golden question through the archive and writes down what happened.
 *
 * <p>Three files go to {@code build/}, named {@code episode-3-<model>-<mode>-k4}:
 *
 * <ul>
 *   <li>{@code .csv}: one row per question, with the retrieval score, Quoter's traced quotations,
 *       Answerer's claims by verdict, abstention, stale answers, tokens and time;
 *   <li>{@code .jsonl}: the same rows in full, with every chunk retrieved, every quotation and
 *       every claim, so a failure can be diagnosed from the run itself;
 *   <li>{@code -summary.md}: the per-stratum table the article prints.
 * </ul>
 *
 * <p>Every row records the commit, the models and a hash of the prompts. Change any of them and the
 * numbers are a new baseline, not a comparison.
 */
public final class GoldenRun {

  private static final Logger log = LoggerFactory.getLogger(GoldenRun.class);

  /** The advisor's default: four chunks per question. */
  static final int K = 4;

  /** Words that mark an answer as knowing its source is out of date. A heuristic, said so. */
  private static final Pattern FLAGS_STALE =
      Pattern.compile(
          "(?i)\\b(?<staleWord>withdrawn|superseded|no longer|retired|archived|was proposed)\\b");

  /** What the run was measured on. */
  public record Setup(
      SpecCorpus.Manifest corpus,
      String chatModel,
      String embeddingModel,
      Map<String, String> digests,
      String promptsHash) {}

  /** One question's outcome. */
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
      return value(ruling.quoterPromptTokens()) + value(ruling.answererPromptTokens());
    }
  }

  private final Archive archive;
  private final GoldenScorer scorer;
  private final CitationChecker citations;

  public GoldenRun(Archive archive, GoldenScorer scorer, CitationChecker citations) {
    this.archive = archive;
    this.scorer = scorer;
    this.citations = citations;
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

  /** The quotations the answer leaned on: Answerer's citations in duo mode, all of them in solo. */
  private SequencedCollection<Quote> citedBy(Archive.Ruling ruling) {
    if (ruling.mode() != Archive.Mode.QUOTED) {
      return ruling.quotes();
    }
    return ruling
        .answerer()
        .map(reply -> citations.cited(reply, ruling.quotes()))
        .orElseGet(LinkedHashSet::new);
  }

  /** Whether any of the question's stale records is in the top four Quoter was handed. */
  private boolean anyStaleAmong(Question question, List<Document> top) {
    return top.stream()
        .anyMatch(
            chunk ->
                question.stale().stream().anyMatch(stale -> scorer.hit(stale, chunk) != Hit.NONE));
  }

  /**
   * Whether a quotation Answerer relied on came from a stale element itself, rather than merely
   * from a chunk that also happens to hold it.
   */
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
    Score score = scorer.score(question, top);
    Double verified =
        ruling.mode() == Archive.Mode.QUOTED
            ? new QuoteResolver.Resolution(ruling.quoterVerdicts(), ruling.quotes()).verifiedShare()
            : null;
    boolean staleRetrieved = anyStaleAmong(question, top);
    var cited = citedBy(ruling);
    boolean staleCited = anyStaleQuotedIn(question, cited);
    boolean staleFlagged =
        ruling.answerer().map(reply -> FLAGS_STALE.matcher(reply.answer()).find()).orElse(false);
    return new Row(
        question,
        ruling,
        score,
        verified,
        Tally.of(ruling.answererVerdicts()),
        staleRetrieved,
        staleCited,
        staleFlagged);
  }

  private static int value(Integer i) {
    return i == null ? 0 : i;
  }

  /** Writes the three files and returns the summary table. */
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

  /** Short hash of the persona prompts, so a prompt change shows up in every row. */
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
