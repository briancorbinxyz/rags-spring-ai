package xyz.briancorbin.rags;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.SequencedCollection;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import tools.jackson.databind.json.JsonMapper;
import xyz.briancorbin.rags.FactChecker.Tally;
import xyz.briancorbin.rags.GoldenScorer.Hit;
import xyz.briancorbin.rags.GoldenScorer.Score;
import xyz.briancorbin.rags.GoldenSet.Question;

/**
 * Runs every golden question through the archive and writes down what happened.
 *
 * <p>Three files go to {@code build/}, named {@code episode-3-<model>-<mode>-k4}:
 *
 * <ul>
 *   <li>{@code .csv}: one row per question, with the retrieval score, Beta's traced quotations,
 *       Alpha's claims by verdict, abstention, stale answers, tokens and time;
 *   <li>{@code .jsonl}: the same rows in full, with every chunk retrieved, every quotation and
 *       every claim, so a failure can be diagnosed from the run itself;
 *   <li>{@code -summary.md}: the per-stratum table the article prints.
 * </ul>
 *
 * <p>Every row records the commit, the models and a hash of the prompts. Change any of them and
 * the numbers are a new baseline, not a comparison.
 */
public final class GoldenRun {

  private static final Logger log = LoggerFactory.getLogger(GoldenRun.class);

  /** The advisor's default: four chunks per question. */
  static final int K = 4;

  /** Words that mark an answer as knowing its source is out of date. A heuristic, said so. */
  private static final Pattern FLAGS_STALE =
      Pattern.compile("(?i)\\b(?<staleWord>withdrawn|superseded|no longer|retired|archived|was proposed)\\b");

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
      Double betaVerified,
      Tally alphaTally,
      boolean staleRetrieved,
      boolean staleCited,
      boolean staleFlagged) {

    boolean staleAsCurrent() {
      return staleCited && !staleFlagged;
    }

    int tokens() {
      return value(ruling.betaPromptTokens()) + value(ruling.alphaPromptTokens());
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
      log.info("{} [{}] hit@{}={} rank={}", q.id(), q.stratum(), K, rows.getLast().score().hitAt(K), rows.getLast().score().firstHit());
    }
    return List.copyOf(rows);
  }

  /** The quotations the answer leaned on: Alpha's citations in duo mode, all of them in solo. */
  private SequencedCollection<Quote> citedBy(Archive.Ruling ruling) {
    if (ruling.mode() != Archive.Mode.DUO) {
      return ruling.quotes();
    }
    return ruling
        .alpha()
        .map(reply -> citations.cited(reply, ruling.quotes()))
        .orElseGet(LinkedHashSet::new);
  }

  /** Whether any of the question's stale records is in the top four Beta was handed. */
  private boolean anyStaleAmong(Question question, List<Document> top) {
    return top.stream()
        .anyMatch(
            chunk ->
                question.stale().stream().anyMatch(stale -> scorer.hit(stale, chunk) != Hit.NONE));
  }

  /**
   * Whether a quotation Alpha relied on came from a stale element itself, rather than merely from a
   * chunk that also happens to hold it.
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
        ruling.mode() == Archive.Mode.DUO
            ? new QuoteResolver.Resolution(ruling.betaVerdicts(), ruling.quotes()).verifiedShare()
            : null;
    boolean staleRetrieved = anyStaleAmong(question, top);
    var cited = citedBy(ruling);
    boolean staleCited = anyStaleQuotedIn(question, cited);
    boolean staleFlagged =
        ruling.alpha().map(reply -> FLAGS_STALE.matcher(reply.answer()).find()).orElse(false);
    return new Row(
        question,
        ruling,
        score,
        verified,
        Tally.of(ruling.alphaVerdicts()),
        staleRetrieved,
        staleCited,
        staleFlagged);
  }

  /** Writes the three files and returns the summary table. */
  public String write(List<Row> rows, Setup setup, Path dir) {
    String name =
        "episode-3-%s-%s-k%d"
            .formatted(
                setup.chatModel().replaceAll("[^A-Za-z0-9.-]", "_"),
                archive.mode().name().toLowerCase(Locale.ROOT),
                K);
    String summary = summary(rows, setup);
    try {
      Files.createDirectories(dir);
      Files.write(dir.resolve(name + ".csv"), csv(rows, setup));
      Files.write(dir.resolve(name + ".jsonl"), jsonLines(rows, setup));
      Files.writeString(dir.resolve(name + "-summary.md"), summary);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    log.info("Wrote {}.csv, .jsonl and -summary.md to {}", name, dir);
    return summary;
  }

  static List<String> csv(List<Row> rows, Setup setup) {
    var lines = new ArrayList<String>();
    lines.add(
        "id,stratum,status,mode,hit_at_4,first_hit_rank,partial_at_4,first_partial_rank,all_at_4,"
            + "beta_quotes_traced,beta_verified_share,alpha_claims,supported,contradicted,"
            + "unsupported,unchecked,abstained,stale_retrieved,stale_cited,stale_as_current,"
            + "beta_prompt_tokens,alpha_prompt_tokens,millis,commit,chat_model,embedding_model,"
            + "prompts_sha");
    for (Row row : rows) {
      var alphaTally = row.alphaTally();
      lines.add(
          String.join(
              ",",
              row.question().id(),
              row.question().stratum(),
              row.question().status(),
              row.ruling().mode().name().toLowerCase(Locale.ROOT),
              bit(row.score().hitAt(K)),
              str(row.score().firstHit()),
              bit(row.score().partialAt(K)),
              str(row.score().firstPartial()),
              bit(row.score().allAt(K)),
              String.valueOf(row.ruling().quotes().size()),
              row.betaVerified() == null ? "" : "%.2f".formatted(row.betaVerified()),
              String.valueOf(alphaTally.claims()),
              String.valueOf(alphaTally.supported()),
              String.valueOf(alphaTally.contradicted()),
              String.valueOf(alphaTally.unsupported()),
              String.valueOf(alphaTally.unchecked()),
              bit(row.ruling().abstained()),
              bit(row.staleRetrieved()),
              bit(row.staleCited()),
              bit(row.staleAsCurrent()),
              str(row.ruling().betaPromptTokens()),
              str(row.ruling().alphaPromptTokens()),
              String.valueOf(row.ruling().millis()),
              setup.corpus().commit().substring(0, 7),
              setup.chatModel(),
              setup.embeddingModel(),
              setup.promptsHash()));
    }
    return lines;
  }

  static List<String> jsonLines(List<Row> rows, Setup setup) {
    var mapper = JsonMapper.shared();
    var lines = new ArrayList<String>();
    lines.add(mapper.writeValueAsString(Map.of("setup", setup)));
    for (Row row : rows) {
      var ruling = row.ruling();
      var line = new LinkedHashMap<String, Object>();
      line.put("id", row.question().id());
      line.put("question", row.question().question());
      line.put("stratum", row.question().stratum());
      line.put("expected", row.question().expected());
      line.put("accept", row.question().accept());
      line.put(
          "retrieved",
          ruling.retrieved().stream()
              .map(
                  chunk ->
                      Map.of(
                          "address", Address.of(chunk).toString(),
                          "score", chunk.getScore() == null ? 0 : chunk.getScore()))
              .toList());
      line.put("score", row.score());
      line.put("beta", ruling.beta().orElse(null));
      line.put("beta_verdicts", ruling.betaVerdicts());
      line.put("quotes", ruling.quotes());
      line.put("alpha_prompt", ruling.alphaPrompt());
      line.put("alpha", ruling.alpha().orElse(null));
      line.put("alpha_verdicts", ruling.alphaVerdicts());
      line.put("abstained", ruling.abstained());
      line.put("stale_cited", row.staleCited());
      line.put("stale_flagged", row.staleFlagged());
      line.put("millis", ruling.millis());
      lines.add(mapper.writeValueAsString(line));
    }
    return lines;
  }

  /** The table the article prints: one row per stratum, then the whole set. */
  static String summary(List<Row> rows, Setup setup) {
    var byStratum = new LinkedHashMap<String, List<Row>>();
    for (String stratum : GoldenSet.STRATA) {
      byStratum.put(stratum, new ArrayList<>());
    }
    rows.forEach(
        row ->
            byStratum
                .computeIfAbsent(row.question().stratum(), stratum -> new ArrayList<>())
                .add(row));
    var out = new StringBuilder();
    out.append(
        "Commit %s · chat %s · embeddings %s · %d files, %d chunks · ingest %.1f min · largest chunk %d tokens%n%n"
            .formatted(
                setup.corpus().commit().substring(0, 7),
                setup.chatModel(),
                setup.embeddingModel(),
                setup.corpus().files(),
                setup.corpus().chunks(),
                setup.corpus().ingestMillis() / 60000.0,
                setup.corpus().largestChunkTokens()));
    out.append(
        "| Stratum | Questions | Hit@4 | All@4 | Partial@4 | MRR | Alpha abstained | Stale as current |\n");
    out.append("|---|---|---|---|---|---|---|---|\n");
    for (var stratum : byStratum.entrySet()) {
      if (!stratum.getValue().isEmpty()) {
        out.append(line(stratum.getKey(), stratum.getValue()));
      }
    }
    out.append(line("all", rows));
    var answerable = rows.stream().filter(row -> row.question().answerable()).toList();
    var unanswerable = rows.stream().filter(row -> !row.question().answerable()).toList();
    var verified = shareOfQuotationsTraced(rows);
    var millis = sortedMillis(rows);
    out.append(
        "%nAbstained correctly on %d of %d unanswerable questions; abstained wrongly on %d of %d answerable ones.%n"
            .formatted(
                count(unanswerable, row -> row.ruling().abstained()),
                unanswerable.size(),
                count(answerable, row -> row.ruling().abstained()),
                answerable.size()));
    out.append(
        "Beta's quotations traced word for word: %s. Alpha's claims: %s.%n"
            .formatted(
                verified.isPresent() ? "%.0f%%".formatted(verified.getAsDouble() * 100) : "n/a",
                words(Tally.of(allAlphaVerdicts(rows)))));
    out.append(
        "Prompt tokens per question, mean %.0f. Time per question, p50 %.1f s, p95 %.1f s.%n"
            .formatted(
                rows.stream().mapToInt(Row::tokens).average().orElse(0),
                percentile(millis, 50) / 1000.0,
                percentile(millis, 95) / 1000.0));
    return out.toString();
  }

  /** Beta's quotations that traced word for word, as a share; empty if none were checked. */
  private static OptionalDouble shareOfQuotationsTraced(List<Row> rows) {
    return rows.stream()
        .map(Row::betaVerified)
        .filter(share -> share != null)
        .mapToDouble(Double::doubleValue)
        .average();
  }

  /** Every question's wall time, ascending, so a percentile can be read straight off it. */
  private static List<Long> sortedMillis(List<Row> rows) {
    return rows.stream().map(row -> row.ruling().millis()).sorted(Comparator.naturalOrder()).toList();
  }

  /** Every verdict on every claim Alpha made, across the run. */
  private static List<CitationChecker.Verdict> allAlphaVerdicts(List<Row> rows) {
    return rows.stream().flatMap(row -> row.ruling().alphaVerdicts().stream()).toList();
  }

  /** Mean reciprocal rank over the answerable questions; 0 if there are none. */
  private static double meanReciprocalRank(List<Row> rows) {
    return rows.stream()
        .filter(row -> row.question().answerable())
        .mapToDouble(row -> row.score().reciprocalRank())
        .average()
        .orElse(0);
  }

  private static String line(String name, List<Row> rows) {
    double hit = share(rows, row -> row.score().hitAt(K));
    double partial = share(rows, row -> row.score().partialAt(K));
    double all = share(rows, row -> row.score().allAt(K));
    double mrr = meanReciprocalRank(rows);
    boolean none = rows.stream().noneMatch(row -> row.question().answerable());
    return "| %s | %d | %s | %s | %s | %s | %d | %d |%n"
        .formatted(
            name,
            rows.size(),
            none ? "n/a" : "%.0f%%".formatted(hit * 100),
            none ? "n/a" : "%.0f%%".formatted(all * 100),
            none ? "n/a" : "%.0f%%".formatted(partial * 100),
            none ? "n/a" : "%.2f".formatted(mrr),
            count(rows, row -> row.ruling().abstained()),
            count(rows, Row::staleAsCurrent));
  }

  /** Share of answerable rows that pass; unanswerable questions have nothing to retrieve. */
  private static double share(List<Row> rows, java.util.function.Predicate<Row> test) {
    var answerable = rows.stream().filter(row -> row.question().answerable()).toList();
    return answerable.isEmpty() ? 0 : (double) count(answerable, test) / answerable.size();
  }

  private static int count(List<Row> rows, java.util.function.Predicate<Row> test) {
    return (int) rows.stream().filter(test).count();
  }

  /** Nearest-rank percentile of a sorted list. */
  static long percentile(List<Long> sorted, int p) {
    if (sorted.isEmpty()) {
      return 0;
    }
    int rank = (int) Math.ceil(p / 100.0 * sorted.size());
    return sorted.get(Math.max(0, rank - 1));
  }

  private static String bit(boolean b) {
    return b ? "1" : "0";
  }

  private static String str(Object o) {
    return o == null ? "" : o.toString();
  }

  private static int value(Integer i) {
    return i == null ? 0 : i;
  }

  /** Short hash of the persona prompts, so a prompt change shows up in every row. */
  static String hash(List<String> prompts) {
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

  static String words(Tally t) {
    return "%d supported, %d contradicted, %d unsupported, %d unchecked"
        .formatted(t.supported(), t.contradicted(), t.unsupported(), t.unchecked());
  }
}
