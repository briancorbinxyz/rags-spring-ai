package xyz.briancorbin.rags;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import xyz.briancorbin.rags.GoldenRun.Row;
import xyz.briancorbin.rags.GoldenRun.Setup;

/**
 * One row per question, for a spreadsheet: the scores, what each persona said it was sure of, and
 * the run's provenance repeated on every line so a row can be read on its own.
 */
final class RunCsv {

  private RunCsv() {}

  private static final String HEADER =
      "id,stratum,status,mode,hit_at_4,first_hit_rank,partial_at_4,first_partial_rank,all_at_4,"
          + "beta_quotes_traced,beta_verified_share,alpha_claims,supported,contradicted,"
          + "unsupported,unchecked,abstained,stale_retrieved,stale_cited,stale_as_current,"
          + "beta_prompt_tokens,alpha_prompt_tokens,millis,commit,chat_model,embedding_model,"
          + "prompts_sha";

  static List<String> csv(List<Row> rows, Setup setup) {
    var lines = new ArrayList<String>();
    lines.add(HEADER);
    rows.forEach(row -> lines.add(String.join(",", cells(row, setup))));
    return lines;
  }

  /** One question's cells, in the header's order. */
  private static List<String> cells(Row row, Setup setup) {
    var alphaTally = row.alphaTally();
    return List.of(
        row.question().id(),
        row.question().stratum(),
        row.question().status(),
        row.ruling().mode().name().toLowerCase(Locale.ROOT),
        bit(row.score().hitAt(GoldenRun.K)),
        str(row.score().firstHit()),
        bit(row.score().partialAt(GoldenRun.K)),
        str(row.score().firstPartial()),
        bit(row.score().allAt(GoldenRun.K)),
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
        setup.promptsHash());
  }

  private static String bit(boolean b) {
    return b ? "1" : "0";
  }

  private static String str(Object o) {
    return o == null ? "" : o.toString();
  }
}
