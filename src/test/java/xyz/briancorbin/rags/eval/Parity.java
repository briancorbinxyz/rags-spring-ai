package xyz.briancorbin.rags.eval;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/// The Java against the prototype, one ranking at a time.
///
/// Episode 5 claims the two implementations return the same fifty results in the same order for all
/// 53 questions at steps 4, 5 and 6. That was a claim checked by hand once. This makes it a test:
/// the prototype's rankings are committed under `src/test/resources/parity`, and a step whose
/// ranking has drifted says which question, which position, and what each side put there.
///
/// Why not the TREC run file, which is already written for every step? TREC keeps one line per
/// passage, and a split unit's parts share an address, so a ranking that names a unit twice comes
/// out one line shorter than it is. Parity is about the items, duplicates and all, so the items are
/// compared as they are.
///
/// The prototype writes its side with `run.py --parity-out`, mapping its configuration keys onto
/// these step names: `X1` is step 4, `H1` is step 5, `E5-1` is step 6.
final class Parity {

  /// How many disagreeing questions to name before the message stops listing them.
  private static final int NAMED = 3;

  private Parity() {}

  /// One question's ranking, as either side wrote it.
  private record Ranking(String id, List<String> ranked) {}

  /// Writes the Java's ranking for `step` and holds it to the prototype's.
  ///
  /// The file is written either way, because a run that fails this is exactly the run whose output
  /// someone will want to diff by hand.
  static void assertMatchesThePrototype(
      RetrievalRun.Step step, List<RetrievalRun.Result> results, Path out) {
    List<Ranking> java =
        results.stream().map(r -> new Ranking(r.question().id(), r.ranked())).toList();
    write(step, java, out);
    List<Ranking> prototype = committed(step);
    List<String> differences = differences(java, prototype);
    if (!differences.isEmpty()) {
      throw new AssertionError(
          "%s does not reproduce the prototype:%n  %s"
              .formatted(step.name(), summary(differences)));
    }
  }

  // --- comparing ---------------------------------------------------------------------------------

  /// Every way the two sides disagree, in question order. Order and membership first, because a
  /// missing question makes a position-by-position comparison meaningless.
  private static List<String> differences(List<Ranking> java, List<Ranking> prototype) {
    var out = new ArrayList<String>();
    List<String> theirs = prototype.stream().map(Ranking::id).toList();
    List<String> ours = java.stream().map(Ranking::id).toList();
    if (!ours.equals(theirs)) {
      out.add(
          "different questions, or a different order: %d here, %d committed"
              .formatted(ours.size(), theirs.size()));
      return out;
    }
    Map<String, List<String>> byId = new LinkedHashMap<>();
    prototype.forEach(r -> byId.put(r.id(), r.ranked()));
    for (Ranking mine : java) {
      firstDisagreement(mine.id(), byId.get(mine.id()), mine.ranked()).ifPresent(out::add);
    }
    return out;
  }

  /// Where two rankings first part company, said in full: a position and both addresses, or a
  /// length if one simply stops. Empty when they agree item for item.
  private static Optional<String> firstDisagreement(
      String id, List<String> prototype, List<String> java) {
    int shared = Math.min(prototype.size(), java.size());
    for (int at = 0; at < shared; at++) {
      if (!prototype.get(at).equals(java.get(at))) {
        return Optional.of(
            "%s differs at position %d: prototype %s, here %s"
                .formatted(id, at + 1, prototype.get(at), java.get(at)));
      }
    }
    if (prototype.size() != java.size()) {
      return Optional.of(
          "%s agrees for %d results and then one side stops: prototype has %d, here %d"
              .formatted(id, shared, prototype.size(), java.size()));
    }
    return Optional.empty();
  }

  /// The first few disagreements in full, then a count, so a wholesale drift reads as one line
  /// rather than fifty-three.
  private static String summary(List<String> differences) {
    List<String> named = differences.subList(0, Math.min(NAMED, differences.size()));
    String rest =
        differences.size() > NAMED
            ? "%n  ... and %d more question(s)".formatted(differences.size() - NAMED)
            : "";
    return String.join(System.lineSeparator() + "  ", named) + rest;
  }

  // --- reading and writing -----------------------------------------------------------------------

  /// The prototype's rankings for `step`, as committed. A missing file is a failure, not a skip:
  /// the point of the fixture is that it is always there to be contradicted.
  private static List<Ranking> committed(RetrievalRun.Step step) {
    var resource = new ClassPathResource("parity/parity-" + step.name() + ".jsonl");
    if (!resource.exists()) {
      throw new AssertionError(
          ("No committed prototype ranking at parity/parity-%s.jsonl. Write it with"
                  + " `run.py --parity-out`, which names the file after the Java step.")
              .formatted(step.name()));
    }
    try (var lines =
        new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      var out = new ArrayList<Ranking>();
      for (String line : lines.lines().filter(l -> !l.isBlank()).toList()) {
        JsonNode row = JsonMapper.shared().readTree(line);
        var ranked = new ArrayList<String>();
        row.path("ranked").forEach(address -> ranked.add(address.asString()));
        out.add(new Ranking(row.path("id").asString(), List.copyOf(ranked)));
      }
      return List.copyOf(out);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the committed ranking for " + step.name(), e);
    }
  }

  /// This run's rankings, in the shape the prototype writes, so the two files diff directly.
  private static void write(RetrievalRun.Step step, List<Ranking> rankings, Path out) {
    var lines = new ArrayList<String>();
    for (Ranking r : rankings) {
      lines.add(
          JsonMapper.shared()
              .writeValueAsString(Map.of("step", step.name(), "id", r.id(), "ranked", r.ranked())));
    }
    try {
      Files.createDirectories(out);
      Files.write(out.resolve("parity-" + step.name() + ".jsonl"), lines);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
