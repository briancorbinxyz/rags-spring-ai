package xyz.briancorbin.rags.eval;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/// Reading a golden run back off disk.
///
/// A run's `.jsonl` holds everything that happened, which is what lets an answer be graded long
/// after the models that produced it have been put away.
public final class SavedRun {

  private SavedRun() {}

  /// The answering role's claims, joined, by question id. The first line of a run is its setup, not
  /// a question.
  ///
  /// Runs name that role two ways. Episode 3's file calls it `alpha`, after the persona; the code
  /// now writes `answerer`, after the job. Both are read, because a run already on disk is the
  /// thing this class exists to go back to, and the older ones are not going to be rewritten.
  public static Map<String, String> answersIn(Path run) {
    try {
      var out = new LinkedHashMap<String, String>();
      for (String line : Files.readAllLines(run, StandardCharsets.UTF_8)) {
        if (line.isBlank()) {
          continue;
        }
        JsonNode row = JsonMapper.shared().readTree(line);
        if (!row.has("id")) {
          continue;
        }
        out.put(row.path("id").asString(), whatItAnswered(row));
      }
      refuseAnEmptyRead(out, run);
      return Map.copyOf(out);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the run " + run, e);
    }
  }

  /// The claims of whichever role did the answering, joined in the order they were made.
  private static String whatItAnswered(JsonNode row) {
    JsonNode answering = row.has("answerer") ? row.path("answerer") : row.path("alpha");
    var said = new ArrayList<String>();
    for (JsonNode claim : answering.path("claims")) {
      said.add(claim.path("text").asString());
    }
    return String.join(" ", said);
  }

  /// Every answer blank means the run's shape changed under us, not that it declined every
  /// question. Reporting that as a result would be the worst outcome available, so it is an error.
  private static void refuseAnEmptyRead(Map<String, String> answers, Path run) {
    if (!answers.isEmpty() && answers.values().stream().allMatch(String::isBlank)) {
      throw new IllegalStateException(
          ("Read %d questions from %s and not one answer: its shape is not one this knows."
                  + " Expected claims under `answerer` or `alpha`.")
              .formatted(answers.size(), run));
    }
  }
}
