package xyz.briancorbin.rags.eval;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/// Spring AI's `FactCheckingEvaluator` against this series' own `FactChecker`, claim by claim.
///
/// The two answer the same question by different means, which is the only reason comparing them is
/// interesting. `FactChecker` **computes** the answer: a claim is supported when its quotation is
/// found word for word in a passage that was actually retrieved, which is Rule 1 — compute what can
/// be computed. `FactCheckingEvaluator` **asks a model**: here is a document, here is a claim, say
/// yes or no.
///
/// So this is not a judge of answers and it is not a replacement for {@link AnswerJudge}, which
/// grades an answer against a reference answer. It is the second opinion on *supportedness*, and
/// where it disagrees with the tracer, one of two things is true: either the claim paraphrases its
/// passage rather than quoting it, which the tracer calls unsupported and a reader would not, or
/// the model is wrong. Both are worth seeing, and the report keeps them apart by printing the
/// claim.
///
/// Nothing here retrieves or answers anything. It reads a run off disk, so a comparison costs one
/// model call per claim and no retrieval at all.
public final class SupportAgreement {

  private static final Logger log = LoggerFactory.getLogger(SupportAgreement.class);

  /// How many disagreements to print in full before the report just counts them.
  private static final int SHOWN = 12;

  private final FactCheckingEvaluator evaluator;

  public SupportAgreement(FactCheckingEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  /// One claim, as both instruments saw it.
  public record Verdict(String question, String claim, boolean traced, boolean modelSaysSupported) {

    boolean agree() {
      return traced == modelSaysSupported;
    }
  }

  /// Every claim in `run`, put to the evaluator and compared with the verdict already recorded.
  List<Verdict> compare(Path run) {
    var out = new ArrayList<Verdict>();
    for (JsonNode row : rows(run)) {
      List<Document> retrieved = passagesOf(row);
      for (JsonNode verdict : row.path("answerer_verdicts")) {
        String claim = verdict.path("claim").path("text").asString();
        if (claim.isBlank()) {
          continue;
        }
        out.add(
            new Verdict(
                row.path("id").asString(),
                claim,
                "SUPPORTED".equals(verdict.path("status").asString()),
                asks(row.path("question").asString(), retrieved, claim)));
      }
    }
    return List.copyOf(out);
  }

  /// What the evaluator says about one claim. A model that will not answer the question counts as
  /// "not supported", which is what `FactCheckingEvaluator` itself does with anything but `yes`.
  private boolean asks(String question, List<Document> retrieved, String claim) {
    try {
      return evaluator.evaluate(new EvaluationRequest(question, retrieved, claim)).isPass();
    } catch (RuntimeException e) {
      log.warn("The evaluator failed on a claim of {}: {}", question, e.getMessage());
      return false;
    }
  }

  /// The passages the run recorded as retrieved, as documents for the evaluator to read. Their text
  /// is what the quoter was shown, so both instruments see the same evidence.
  private static List<Document> passagesOf(JsonNode row) {
    var out = new ArrayList<Document>();
    for (JsonNode quote : row.path("quotes")) {
      String text = quote.path("text").asString();
      if (!text.isBlank()) {
        out.add(new Document(text));
      }
    }
    return out;
  }

  private static List<JsonNode> rows(Path run) {
    try {
      var out = new ArrayList<JsonNode>();
      for (String line : Files.readAllLines(run, StandardCharsets.UTF_8)) {
        if (!line.isBlank()) {
          JsonNode row = JsonMapper.shared().readTree(line);
          if (row.has("id")) {
            out.add(row);
          }
        }
      }
      if (out.isEmpty()) {
        throw new IllegalStateException("No question rows in " + run);
      }
      return out;
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the run " + run, e);
    }
  }

  /// The comparison as a table, with every disagreement named.
  static String asMarkdown(List<Verdict> verdicts, String model) {
    return heading(model, Counts.of(verdicts))
        + disagreements(verdicts.stream().filter(v -> !v.agree()).toList());
  }

  /// The tally the heading prints, counted once and travelling as one thing.
  private record Counts(int claims, long traced, long byModel, long agree) {

    static Counts of(List<Verdict> verdicts) {
      return new Counts(
          verdicts.size(),
          verdicts.stream().filter(Verdict::traced).count(),
          verdicts.stream().filter(Verdict::modelSaysSupported).count(),
          verdicts.stream().filter(Verdict::agree).count());
    }

    double agreementRate() {
      return claims == 0 ? 0.0 : 100.0 * agree / claims;
    }
  }

  private static String heading(String model, Counts counts) {
    return """
        # Two ways to ask whether a claim is supported

        `FactChecker` traces a quotation word for word. Spring AI's `FactCheckingEvaluator` asks a
        model. Same claims, same passages, one run off disk, evaluator model `%s`.

        | | claims |
        |---|---|
        | claims in the run | %d |
        | supported, by tracing | %d |
        | supported, by the evaluator | %d |
        | **the two agree** | **%d of %d (%.0f%%)** |

        """
        .formatted(
            model,
            counts.claims(),
            counts.traced(),
            counts.byModel(),
            counts.agree(),
            counts.claims(),
            counts.agreementRate());
  }

  private static String disagreements(List<Verdict> apart) {
    if (apart.isEmpty()) {
      return "They never disagreed, which on this many claims is worth being suspicious of.\n";
    }
    var out = new StringBuilder("## Where they disagree\n\n");
    out.append("| question | tracer | evaluator | claim |\n|---|---|---|---|\n");
    for (Verdict v : apart.stream().limit(SHOWN).toList()) {
      out.append(
          "| %s | %s | %s | %s |%n"
              .formatted(
                  v.question(),
                  v.traced() ? "supported" : "not",
                  v.modelSaysSupported() ? "supported" : "not",
                  v.claim().replace("|", "\\|")));
    }
    if (apart.size() > SHOWN) {
      out.append("%n...and %d more.%n".formatted(apart.size() - SHOWN));
    }
    return out.toString();
  }

  /// Runs the comparison and writes the report. The only thing a caller outside this package needs;
  /// `model` is recorded in the report because the evaluator's verdicts are that model's.
  public void report(Path run, String model, Path to) {
    String report = asMarkdown(compare(run), model);
    try {
      Files.createDirectories(to.getParent());
      Files.writeString(to, report, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    log.info("Wrote {}\n{}", to, report);
  }
}
