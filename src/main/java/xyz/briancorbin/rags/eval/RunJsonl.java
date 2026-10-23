package xyz.briancorbin.rags.eval;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import tools.jackson.databind.json.JsonMapper;
import xyz.briancorbin.rags.answer.Archive;
import xyz.briancorbin.rags.eval.GoldenRun.Row;
import xyz.briancorbin.rags.eval.GoldenRun.Setup;
import xyz.briancorbin.rags.units.Address;

/**
 * One JSON object per question, for anything that wants the whole record: every retrieved address
 * and score, both personas' replies, every verdict and every quotation.
 */
final class RunJsonl {

  private RunJsonl() {}

  static List<String> jsonLines(List<Row> rows, Setup setup) {
    var mapper = JsonMapper.shared();
    var lines = new ArrayList<String>();
    lines.add(mapper.writeValueAsString(Map.of("setup", setup)));
    rows.forEach(row -> lines.add(mapper.writeValueAsString(object(row))));
    return lines;
  }

  /** One question's record, in the order it reads best. */
  private static SequencedMap<String, Object> object(Row row) {
    var ruling = row.ruling();
    var line = new LinkedHashMap<String, Object>();
    line.put("id", row.question().id());
    line.put("question", row.question().question());
    line.put("stratum", row.question().stratum());
    line.put("expected", row.question().expected());
    line.put("accept", row.question().accept());
    line.put("retrieved", retrieved(ruling));
    line.put("score", row.score());
    line.put("quoter", ruling.quoter().orElse(null));
    line.put("quoter_verdicts", ruling.quoterVerdicts());
    line.put("quotes", ruling.quotes());
    line.put("answerer_prompt", ruling.answererPrompt());
    line.put("answerer", ruling.answerer().orElse(null));
    line.put("answerer_verdicts", ruling.answererVerdicts());
    line.put("abstained", ruling.abstained());
    line.put("stale_cited", row.staleCited());
    line.put("stale_flagged", row.staleFlagged());
    line.put("millis", ruling.millis());
    return line;
  }

  /** Each retrieved chunk as its address and its score, in the order it was ranked. */
  private static List<Map<String, Object>> retrieved(Archive.Ruling ruling) {
    return ruling.retrieved().stream()
        .map(
            chunk ->
                Map.<String, Object>of(
                    "address",
                    Address.of(chunk).toString(),
                    "score",
                    chunk.getScore() == null ? 0 : chunk.getScore()))
        .toList();
  }
}
