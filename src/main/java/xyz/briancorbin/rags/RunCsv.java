package xyz.briancorbin.rags;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import xyz.briancorbin.rags.GoldenRun.Row;
import xyz.briancorbin.rags.GoldenRun.Setup;

/**
 * One row per question, for a spreadsheet: the scores, what each persona was sure of, and the run's
 * provenance repeated on every line so a row can be read on its own.
 *
 * <p>Each column is declared once, as its heading and how to read it. A header list and a cell list
 * kept side by side would be two things to keep in the same order, and nothing would say when they
 * stopped being.
 */
final class RunCsv {

  private RunCsv() {}

  private record Column(String heading, BiFunction<Row, Setup, String> cell) {}

  private static final List<Column> COLUMNS =
      List.of(
          column("id", (row, setup) -> row.question().id()),
          column("stratum", (row, setup) -> row.question().stratum()),
          column("status", (row, setup) -> row.question().status()),
          column("mode", (row, setup) -> row.ruling().mode().name().toLowerCase(Locale.ROOT)),
          column("hit_at_4", (row, setup) -> bit(row.score().hitAt(GoldenRun.K))),
          column("first_hit_rank", (row, setup) -> str(row.score().firstHit())),
          column("partial_at_4", (row, setup) -> bit(row.score().partialAt(GoldenRun.K))),
          column("first_partial_rank", (row, setup) -> str(row.score().firstPartial())),
          column("all_at_4", (row, setup) -> bit(row.score().allAt(GoldenRun.K))),
          column("beta_quotes_traced", (row, setup) -> count(row.ruling().quotes().size())),
          column("beta_verified_share", (row, setup) -> share(row.betaVerified())),
          column("alpha_claims", (row, setup) -> count(row.alphaTally().claims())),
          column("supported", (row, setup) -> count(row.alphaTally().supported())),
          column("contradicted", (row, setup) -> count(row.alphaTally().contradicted())),
          column("unsupported", (row, setup) -> count(row.alphaTally().unsupported())),
          column("unchecked", (row, setup) -> count(row.alphaTally().unchecked())),
          column("abstained", (row, setup) -> bit(row.ruling().abstained())),
          column("stale_retrieved", (row, setup) -> bit(row.staleRetrieved())),
          column("stale_cited", (row, setup) -> bit(row.staleCited())),
          column("stale_as_current", (row, setup) -> bit(row.staleAsCurrent())),
          column("beta_prompt_tokens", (row, setup) -> str(row.ruling().betaPromptTokens())),
          column("alpha_prompt_tokens", (row, setup) -> str(row.ruling().alphaPromptTokens())),
          column("millis", (row, setup) -> String.valueOf(row.ruling().millis())),
          column("commit", (row, setup) -> setup.corpus().commit().substring(0, 7)),
          column("chat_model", (row, setup) -> setup.chatModel()),
          column("embedding_model", (row, setup) -> setup.embeddingModel()),
          column("prompts_sha", (row, setup) -> setup.promptsHash()));

  static List<String> csv(List<Row> rows, Setup setup) {
    var lines = new ArrayList<String>();
    lines.add(COLUMNS.stream().map(Column::heading).collect(joining(",")));
    rows.forEach(row -> lines.add(cells(row, setup)));
    return lines;
  }

  private static String cells(Row row, Setup setup) {
    return COLUMNS.stream().map(column -> column.cell().apply(row, setup)).collect(joining(","));
  }

  private static Column column(String heading, BiFunction<Row, Setup, String> cell) {
    return new Column(heading, cell);
  }

  private static String bit(boolean yes) {
    return yes ? "1" : "0";
  }

  private static String str(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private static String count(int value) {
    return String.valueOf(value);
  }

  private static String share(Double value) {
    return value == null ? "" : "%.2f".formatted(value);
  }
}
