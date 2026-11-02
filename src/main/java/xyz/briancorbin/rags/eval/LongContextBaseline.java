package xyz.briancorbin.rags.eval;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import xyz.briancorbin.rags.units.ElementUnits;
import xyz.briancorbin.rags.units.Units;

/// What retrieval is worth, measured against not retrieving at all.
///
/// The question RAG-I7 asks is the one worth being able to answer: if you simply put the whole
/// specification in the prompt, how much worse is it? For the questions that name a spec outright
/// that is a real option — a spec is thousands of words, not a million — and if the answers come
/// back just as good, then four selected passages bought ranking machinery and nothing else.
///
/// **Same corpus, same persona, same judge; the only variable is selection.** The context here is
/// built from the same element units the retriever searches, in archive order, for the one spec the
/// question names — so this is not "a different pipeline", it is the identical text with the
/// choosing removed.
///
/// It covers only the questions that name exactly one spec. A question naming none has no spec to
/// paste, and one naming two is a different experiment.
public final class LongContextBaseline {

  private static final Logger log = LoggerFactory.getLogger(LongContextBaseline.class);

  /// `spec 045`, `slice 045` or the `045-some-slug` form, as the archive and its readers write it.
  private static final Pattern SPEC =
      Pattern.compile(
          "\\b(?:spec|specs|slice)\\s+(?<inProse>\\d{3,})\\b|(?<![\\w-])(?<inSlug>\\d{3,})-[a-z]",
          Pattern.CASE_INSENSITIVE);

  /// Something that answers a question. The baseline holds two of these and varies nothing else:
  /// the archive's own retrieval line, and a model shown a whole spec.
  public interface Answerer {
    String answerTo(String question);
  }

  /// A model shown a whole spec, asked for the window that spec needs.
  ///
  /// The window is part of the request because the specs differ by an order of magnitude — 2,600
  /// words to 24,598 — and a context window is memory reserved whether the text fills it or not.
  /// Asking for the largest spec's window on every question made this laptop reserve a key-value
  /// cache it does not have, and it answered by swapping until it stopped answering at all.
  public interface WholeSpecAnswerer {
    String answerTo(String prompt, int window);
  }

  /// The windows this baseline will ask for, smallest first. The largest is the one measured
  /// against the largest spec in the set: 20,482 tokens by Ollama's own count, so 24,576 holds it
  /// with room to answer in.
  private static final int[] WINDOWS = {8192, 12288, 16384, 24576};

  /// Room left for the question, the prompt's scaffolding and the answer itself, on top of the
  /// spec. `num_ctx` covers what goes in and what comes out.
  private static final int TO_ANSWER_IN = 2048;

  private final ElementUnits units;
  private final WholeSpecAnswerer wholeSpec;
  private final Answerer retrieval;

  public LongContextBaseline(ElementUnits units, WholeSpecAnswerer wholeSpec, Answerer retrieval) {
    this.units = units;
    this.wholeSpec = wholeSpec;
    this.retrieval = retrieval;
  }

  /// One question answered twice: from the whole spec, and by the retrieval line.
  public record Answered(
      String id, String spec, int units, int words, String fromTheWholeSpec, String fromRetrieval) {

    Answered andFromRetrieval(String answer) {
      return new Answered(id, spec, units, words, fromTheWholeSpec, answer);
    }
  }

  /// Every question that names exactly one spec, answered from that spec entire and by the
  /// retrieval line — **one answerer at a time, in two passes over the same questions**.
  ///
  /// The passes are not tidiness, and the lesson was not the one it first looked like. Reloading
  /// the model between questions is slow but survivable; what is not survivable is the window.
  /// Ollama reserves a key-value cache for the window it is asked for, filled or not, so asking for
  /// the largest spec's 24,576 on a 2,648-word spec reserves memory this laptop has to find in swap
  /// — and with the disk near full, swap has nowhere to go. Two runs died that way, the second on
  /// its first question.
  ///
  /// So each answerer gets its own pass, and pass one asks smallest window first: the model loads
  /// once per window, four times rather than nineteen, and most questions never ask for the big one
  /// at all. Nothing about the answers changes — every question still gets both, from the same
  /// corpus, graded by the same judge, and every spec still arrives whole.
  public List<Answered> run(List<GoldenSet.Question> questions) {
    return thenTheRetrievalLine(fromTheWholeSpecs(questions), questions);
  }

  /// One question and the spec it names, with the window that spec will need.
  private record Asked(
      GoldenSet.Question question, String spec, String context, int units, int words, int window) {}

  /// Pass one: the whole spec, for every question that names exactly one.
  ///
  /// Asked smallest window first, so the model loads once per window rather than once per question,
  /// and reported back in the order the questions came in.
  private List<Answered> fromTheWholeSpecs(List<GoldenSet.Question> questions) {
    List<Asked> asks = asksIn(questions);
    var answers = new LinkedHashMap<String, Answered>();
    for (Asked ask : asks.stream().sorted(Comparator.comparingInt(Asked::window)).toList()) {
      log.info(
          "{}: spec {}, {} units, about {} words, window {}",
          ask.question().id(),
          ask.spec(),
          ask.units(),
          ask.words(),
          ask.window());
      answers.put(ask.question().id(), answeredFrom(ask));
    }
    log.info("The whole-spec pass answered {} questions; now the retrieval line.", answers.size());
    return asks.stream().map(ask -> answers.get(ask.question().id())).toList();
  }

  /// Every question that names one spec the archive actually holds, in question order.
  private List<Asked> asksIn(List<GoldenSet.Question> questions) {
    var asks = new ArrayList<Asked>();
    for (GoldenSet.Question question : questions) {
      theOneSpecNamed(question.question())
          .flatMap(spec -> ask(question, spec))
          .ifPresent(asks::add);
    }
    return List.copyOf(asks);
  }

  private Answered answeredFrom(Asked ask) {
    return new Answered(
        ask.question().id(),
        ask.spec(),
        ask.units(),
        ask.words(),
        wholeSpecSaid(prompt(ask.question().question(), ask.context()), ask.window()),
        "");
  }

  /// Pass two: the same questions through the archive's retrieval line.
  private List<Answered> thenTheRetrievalLine(
      List<Answered> rows, List<GoldenSet.Question> questions) {
    var asked = new LinkedHashMap<String, String>();
    for (GoldenSet.Question question : questions) {
      asked.put(question.id(), question.question());
    }
    var out = new ArrayList<Answered>();
    for (Answered row : rows) {
      log.info("{}: the retrieval line's turn", row.id());
      out.add(row.andFromRetrieval(askTheRetrievalLine(asked.get(row.id()))));
    }
    return List.copyOf(out);
  }

  /// One question with every page of the spec it names, or empty when the archive has no such spec
  /// — which is a gap in the question, not a failure of the baseline.
  private Optional<Asked> ask(GoldenSet.Question question, String spec) {
    List<Document> pages = pagesOf(spec);
    if (pages.isEmpty()) {
      log.warn("{} names spec {} and the archive has no such spec", question.id(), spec);
      return Optional.empty();
    }
    String context = contextOf(pages);
    int words = context.split("\\s+").length;
    return Optional.of(new Asked(question, spec, context, pages.size(), words, windowFor(words)));
  }

  /// The smallest window that holds this spec, counting a token per word.
  ///
  /// A word is worth about 0.83 tokens in this corpus — measured, not assumed: Ollama counted
  /// 20,482 for the 24,598-word spec. Counting a whole token per word therefore leaves about a
  /// fifth in hand, and {@value #TO_ANSWER_IN} more on top for the question and the answer. The
  /// largest window is a cap, because the largest spec is known to fit inside it.
  static int windowFor(int words) {
    int needed = words + TO_ANSWER_IN;
    for (int window : WINDOWS) {
      if (window >= needed) {
        return window;
      }
    }
    return WINDOWS[WINDOWS.length - 1];
  }

  /// One question through the archive's retrieval line, asked here rather than read off a recorded
  /// run, so both sides are this build's answers graded by one judge in one session.
  private String askTheRetrievalLine(String question) {
    try {
      return retrieval.answerTo(question);
    } catch (RuntimeException e) {
      log.warn("The retrieval line failed: {}", e.getMessage());
      return "";
    }
  }

  /// The spec a question names, when it names exactly one. Two is a different experiment and none
  /// has nothing to paste.
  static Optional<String> theOneSpecNamed(String question) {
    var found = new java.util.LinkedHashSet<String>();
    var matching = SPEC.matcher(question);
    while (matching.find()) {
      String inProse = matching.group("inProse");
      found.add(inProse != null ? inProse : matching.group("inSlug"));
    }
    return found.size() == 1 ? Optional.of(found.iterator().next()) : Optional.empty();
  }

  /// Every unit of every page of `spec`, in archive order. Out-of-force units are kept: pasting a
  /// spec means pasting the spec, and deciding what is current is the thing being compared against.
  private List<Document> pagesOf(String spec) {
    String prefix = "specs/" + spec + "-";
    return units.all().stream()
        .filter(unit -> String.valueOf(unit.getMetadata().get(Units.SOURCE)).startsWith(prefix))
        .toList();
  }

  /// The spec as one block, each unit under its address so the model can cite what it used.
  private static String contextOf(List<Document> pages) {
    var out = new StringBuilder();
    for (Document page : pages) {
      out.append('[').append(Units.address(page)).append("]\n");
      out.append(Units.passage(page)).append("\n\n");
    }
    return out.toString();
  }

  /// The same shape of prompt the retrieval line uses, with a whole spec where the quotations go.
  static String prompt(String question, String context) {
    return """
        Question: %s

        The whole of the specification this question names, every page of it, each passage under \
        its address. Cite the addresses you use.
        %s"""
        .formatted(question, context);
  }

  private String wholeSpecSaid(String prompt, int window) {
    try {
      String said = wholeSpec.answerTo(prompt, window);
      return said == null ? "" : said.strip();
    } catch (RuntimeException e) {
      log.warn("The long-context answerer failed: {}", e.getMessage());
      return "";
    }
  }

  /// The comparison: this baseline's grades against the retrieval run's, on the same questions.
  public static String report(
      List<Answered> answered,
      Map<String, Judgement> longContext,
      Map<String, Judgement> retrieved) {
    var out = new StringBuilder();
    out.append(heading(answered, longContext, retrieved));
    out.append("| question | spec | units | words | whole spec | retrieval |\n");
    out.append("|---|---|---|---|---|---|\n");
    for (Answered row : answered) {
      out.append(
          "| %s | %s | %d | %,d | %s | %s |%n"
              .formatted(
                  row.id(),
                  row.spec(),
                  row.units(),
                  row.words(),
                  grade(longContext.get(row.id())),
                  grade(retrieved.get(row.id()))));
    }
    return out.toString();
  }

  private static String heading(
      List<Answered> answered,
      Map<String, Judgement> longContext,
      Map<String, Judgement> retrieved) {
    long whole = right(longContext, answered);
    long four = right(retrieved, answered);
    int words = answered.stream().mapToInt(Answered::words).sum() / Math.max(1, answered.size());
    return """
        # Four passages against the whole spec

        The questions that name exactly one spec, answered twice from the same corpus in the same \
        run: once from the four passages the retrieval line chose, and once from every page of \
        that spec. Same persona, same judge, same questions, one process — the only variable is \
        whether anything did the choosing.

        Both sides are generated here. Neither is read off a recorded run, because the recorded \
        runs that carry answers are older pipelines and the ones from this pipeline record \
        retrieval only. This is the **laptop** tier.

        | | questions right |
        |---|---|
        | answered from the whole spec (mean %,d words of context) | **%d of %d** |
        | answered from the four passages retrieval chose | **%d of %d** |

        """
        .formatted(words, whole, answered.size(), four, answered.size());
  }

  /// Every question in this comparison names a spec, so every one of them is answerable, and
  /// `CORRECT` is the only outcome that counts as right.
  private static long right(Map<String, Judgement> grades, List<Answered> answered) {
    return answered.stream()
        .map(row -> grades.get(row.id()))
        .filter(judgement -> judgement != null && judgement.label() == Judgement.Label.CORRECT)
        .count();
  }

  private static String grade(Judgement judgement) {
    return judgement == null ? "—" : judgement.label().name().toLowerCase(Locale.ROOT);
  }

  /// Writes the answers and the comparison beside the run.
  public static void write(String report, List<Answered> answered, Path to) {
    try {
      Files.createDirectories(to.getParent());
      Files.writeString(to, report, StandardCharsets.UTF_8);
      Files.write(
          to.resolveSibling("long-context-answers.jsonl"),
          answered.stream()
              .map(
                  row ->
                      tools.jackson.databind.json.JsonMapper.shared()
                          .writeValueAsString(
                              Map.of(
                                  "id", row.id(),
                                  "spec", row.spec(),
                                  "units", row.units(),
                                  "words", row.words(),
                                  "whole_spec", row.fromTheWholeSpec(),
                                  "retrieval", row.fromRetrieval())))
              .toList());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    log.info("Wrote {}\n{}", to, report);
  }
}
