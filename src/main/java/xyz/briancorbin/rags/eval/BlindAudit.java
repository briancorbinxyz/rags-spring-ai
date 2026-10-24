package xyz.briancorbin.rags.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.databind.json.JsonMapper;
import xyz.briancorbin.rags.eval.GoldenSet.Accept;
import xyz.briancorbin.rags.eval.GoldenSet.Question;

/// What a person decided about passages the search returned that nobody had judged.
///
/// Dense retrieval surfaces passages the golden set never listed, and an unjudged passage counts
/// as wrong, so each episode puts them in front of a reader in Episode 4's blind format. A round
/// is a file of its own: a later one is added by naming it, never by rewriting a round that has
/// shipped, and never by editing the frozen questions.
///
/// That freeze is why this class exists. Episode 4's round ran before the golden set was frozen,
/// so its acceptances are in `questions.jsonl` itself; every round since lands afterwards and
/// cannot be written back. {@link #fold} is where the two meet, so a question is scored against
/// what it was born with *and* what the audit has accepted since.
public final class BlindAudit {

  /// The rounds this archive ships, oldest first. Episode 4's is named here as well as living in
  /// the golden set's own `accept` lists, so this list is the whole record; folding it twice
  /// changes nothing.
  public static final List<Path> ROUNDS =
      List.of(
          Path.of("results/episode-4/golden-audit-2026-10.jsonl"),
          Path.of("results/episode-5/golden-audit-round3.jsonl"));

  /// One decision: `whole`, `standin`, `partly` or `no`.
  ///
  /// @param id the question it was judged against
  /// @param candidate the passage judged
  /// @param decision what the reader decided
  /// @param forTarget for a stand-in, the one expected address it stands in for
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Decision(
      String id, String candidate, String decision, @JsonProperty("for") String forTarget) {

    /// Whether this passage counts as an answer: `partly` deliberately counts for nothing.
    public boolean accepted() {
      return "whole".equals(decision) || "standin".equals(decision);
    }

    /// The same ruling in the golden set's own words. A decision with no target answered the
    /// whole question, which is what `for: question` means there.
    Accept asAccept() {
      return new Accept(candidate, forTarget == null ? "question" : forTarget);
    }
  }

  private BlindAudit() {}

  /// The shipped rounds, read and folded into the questions: what scoring should use.
  public static List<Question> audited(List<Question> questions) {
    return fold(questions, read(ROUNDS));
  }

  /// Reads the decisions, one JSON object per line, from each round in turn.
  public static List<Decision> read(List<Path> rounds) {
    var decisions = new ArrayList<Decision>();
    for (Path round : rounds) {
      decisions.addAll(decisionsIn(round));
    }
    return List.copyOf(decisions);
  }

  /// The questions with the audit's acceptances added to the ones they already carry.
  ///
  /// A passage a question already accepts is left as it is, so a round that is also in the frozen
  /// file — Episode 4's — folds to no change at all.
  public static List<Question> fold(List<Question> questions, List<Decision> audit) {
    Map<String, List<Decision>> accepted =
        audit.stream().filter(Decision::accepted).collect(Collectors.groupingBy(Decision::id));
    return questions.stream()
        .map(question -> alsoAccepting(question, accepted.getOrDefault(question.id(), List.of())))
        .toList();
  }

  private static Question alsoAccepting(Question question, List<Decision> decisions) {
    var already = question.accept().stream().map(Accept::address).collect(Collectors.toSet());
    var accepts = new ArrayList<>(question.accept());
    decisions.stream()
        .filter(decision -> !already.contains(decision.candidate()))
        .map(Decision::asAccept)
        .forEach(accepts::add);
    return accepts.size() == question.accept().size()
        ? question
        : new Question(
            question.id(),
            question.question(),
            question.stratum(),
            question.expected(),
            accepts,
            question.stale(),
            question.answer(),
            question.commit(),
            question.status());
  }

  private static List<Decision> decisionsIn(Path file) {
    try {
      var out = new ArrayList<Decision>();
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        if (!line.isBlank()) {
          out.add(JsonMapper.shared().readValue(line, Decision.class));
        }
      }
      return out;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the audit " + file, e);
    }
  }
}
