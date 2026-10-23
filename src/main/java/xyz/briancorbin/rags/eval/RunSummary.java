package xyz.briancorbin.rags.eval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.SequencedMap;
import xyz.briancorbin.rags.answer.FactChecker;
import xyz.briancorbin.rags.answer.FactChecker.Tally;
import xyz.briancorbin.rags.eval.GoldenRun.Row;
import xyz.briancorbin.rags.eval.GoldenRun.Setup;

/** The markdown table the article prints: one row per stratum, then the whole set. */
final class RunSummary {

  private RunSummary() {}

  /** The table the article prints: one row per stratum, then the whole set. */
  static String summary(List<Row> rows, Setup setup) {
    return provenance(setup) + table(rows) + abstentions(rows) + quotations(rows) + timings(rows);
  }

  /** What the numbers were measured on, so a table is never read without its conditions. */
  private static String provenance(Setup setup) {
    return """
        Commit %s · chat %s · embeddings %s · %d files, %d chunks · ingest %.1f min · \
        largest chunk %d tokens%n%n"""
        .formatted(
            setup.corpus().commit().substring(0, 7),
            setup.chatModel(),
            setup.embeddingModel(),
            setup.corpus().files(),
            setup.corpus().chunks(),
            setup.corpus().ingestMillis() / 60000.0,
            setup.corpus().largestChunkTokens());
  }

  /** One row per stratum that has questions, then the whole set. */
  private static String table(List<Row> rows) {
    var out = new StringBuilder();
    out.append("| Stratum | Questions | Hit@4 | All@4 | Partial@4 | MRR ");
    out.append("| Answerer abstained | Stale as current |\n");
    out.append("|---|---|---|---|---|---|---|---|\n");
    byStratum(rows).forEach((stratum, of) -> out.append(of.isEmpty() ? "" : line(stratum, of)));
    return out.append(line("all", rows)).toString();
  }

  /** Every stratum the golden set declares, in its order, so an empty one is visibly empty. */
  private static SequencedMap<String, List<Row>> byStratum(List<Row> rows) {
    var byStratum = new LinkedHashMap<String, List<Row>>();
    GoldenSet.STRATA.forEach(stratum -> byStratum.put(stratum, new ArrayList<>()));
    rows.forEach(
        row ->
            byStratum
                .computeIfAbsent(row.question().stratum(), stratum -> new ArrayList<>())
                .add(row));
    return byStratum;
  }

  private static String abstentions(List<Row> rows) {
    var answerable = rows.stream().filter(row -> row.question().answerable()).toList();
    var unanswerable = rows.stream().filter(row -> !row.question().answerable()).toList();
    return """
        %nAbstained correctly on %d of %d unanswerable questions; \
        abstained wrongly on %d of %d answerable ones.%n"""
        .formatted(
            count(unanswerable, row -> row.ruling().abstained()),
            unanswerable.size(),
            count(answerable, row -> row.ruling().abstained()),
            answerable.size());
  }

  private static String quotations(List<Row> rows) {
    OptionalDouble verified = shareOfQuotationsTraced(rows);
    return "Quoter's quotations traced word for word: %s. Answerer's claims: %s.%n"
        .formatted(
            verified.isPresent() ? "%.0f%%".formatted(verified.getAsDouble() * 100) : "n/a",
            words(Tally.of(allAnswererVerdicts(rows))));
  }

  private static String timings(List<Row> rows) {
    List<Long> millis = sortedMillis(rows);
    return "Prompt tokens per question, mean %.0f. Time per question, p50 %.1f s, p95 %.1f s.%n"
        .formatted(
            rows.stream().mapToInt(Row::tokens).average().orElse(0),
            percentile(millis, 50) / 1000.0,
            percentile(millis, 95) / 1000.0);
  }

  private static String line(String name, List<Row> rows) {
    double hit = share(rows, row -> row.score().hitAt(GoldenRun.K));
    double partial = share(rows, row -> row.score().partialAt(GoldenRun.K));
    double all = share(rows, row -> row.score().allAt(GoldenRun.K));
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

  static String words(Tally t) {
    return "%d supported, %d contradicted, %d unsupported, %d unchecked"
        .formatted(t.supported(), t.contradicted(), t.unsupported(), t.unchecked());
  }

  /** Quoter's quotations that traced word for word, as a share; empty if none were checked. */
  private static OptionalDouble shareOfQuotationsTraced(List<Row> rows) {
    return rows.stream()
        .map(Row::quoterVerified)
        .filter(share -> share != null)
        .mapToDouble(Double::doubleValue)
        .average();
  }

  /** Every question's wall time, ascending, so a percentile can be read straight off it. */
  private static List<Long> sortedMillis(List<Row> rows) {
    return rows.stream()
        .map(row -> row.ruling().millis())
        .sorted(Comparator.naturalOrder())
        .toList();
  }

  /** Every verdict on every claim Answerer made, across the run. */
  private static List<FactChecker.Verdict> allAnswererVerdicts(List<Row> rows) {
    return rows.stream().flatMap(row -> row.ruling().answererVerdicts().stream()).toList();
  }

  /** Mean reciprocal rank over the answerable questions; 0 if there are none. */
  private static double meanReciprocalRank(List<Row> rows) {
    return rows.stream()
        .filter(row -> row.question().answerable())
        .mapToDouble(row -> row.score().reciprocalRank())
        .average()
        .orElse(0);
  }
}
