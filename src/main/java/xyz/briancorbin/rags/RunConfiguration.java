package xyz.briancorbin.rags;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import xyz.briancorbin.rags.answer.Archive;
import xyz.briancorbin.rags.answer.CitationChecker;
import xyz.briancorbin.rags.answer.Commentary;
import xyz.briancorbin.rags.answer.Quote;
import xyz.briancorbin.rags.eval.AnswerJudge;
import xyz.briancorbin.rags.eval.GoldenRun;
import xyz.briancorbin.rags.eval.GoldenScorer;
import xyz.briancorbin.rags.eval.GoldenSet;
import xyz.briancorbin.rags.eval.GradedAnswers;
import xyz.briancorbin.rags.eval.JudgeCalibration;
import xyz.briancorbin.rags.eval.Judgement;
import xyz.briancorbin.rags.eval.LongContextBaseline;
import xyz.briancorbin.rags.eval.SupportAgreement;
import xyz.briancorbin.rags.search.OllamaModel;
import xyz.briancorbin.rags.search.PrefixedEmbeddingModel;
import xyz.briancorbin.rags.spectastic.SpecCorpus;
import xyz.briancorbin.rags.units.ElementUnits;

/// What a run does, set by `archive.run`:
/// - `golden` (the default): every golden question, with results written to `build/`;
/// - `ask`: the question in `archive.question`, or, with none, questions typed at the terminal in
///   one conversation, so follow-ups work;
/// - `grade`: grades the answers of a saved run in `archive.judge.run` and writes the summary;
/// - `judge`: grades Episode 3's recorded answers again and reports how often the judge agrees with
///   the person who graded them by hand. Needs no archive and no retrieval;
/// - `demo`: the embedding demo, which needs no archive;
/// - `ingest`: build or load the store and stop;
/// - `none`: nothing, for tests.
@Configuration
class RunConfiguration {

  private static final Logger log = LoggerFactory.getLogger(RunConfiguration.class);

  @Bean
  CommandLineRunner run(
      @Value("${archive.run:golden}") String run,
      @Value("${archive.question:}") String question,
      @Value("${archive.golden:classpath:golden/questions.jsonl}") Resource golden,
      @Value("${spring.ai.ollama.base-url}") String ollama,
      @Value("${spring.ai.ollama.chat.model}") String chatModel,
      @Value("${spring.ai.ollama.embedding.model}") String embeddingModel,
      @Value("classpath:persona/answerer.md") Resource answererPersona,
      @Value("classpath:persona/quoter.md") Resource quoterPersona,
      @Value("classpath:persona/answerer-direct.md") Resource directPersona,
      @Value("${archive.search:elements}") String search,
      @Value("${archive.judge.labels:results/episode-3/labels.csv}") String handGrades,
      @Value("${archive.judge.run:results/episode-3/episode-3-qwen3_8b-duo-k4.jsonl}")
          String recordedRun,
      AnswerJudge judge,
      SupportAgreement agreement,
      Optional<ElementUnits> units,
      @Qualifier("longContextChatClient") ChatClient longContextAnswerer,
      SpecCorpus corpus,
      Archive archive,
      CitationChecker citations,
      PrefixedEmbeddingModel embedder) {
    return args -> {
      switch (run.strip().toLowerCase(Locale.ROOT)) {
        case "none" -> {}
        case "demo" -> log.info("Embedding demo:\n{}", new EmbeddingDemo(embedder).run());
        case "ingest" -> log.info("Archive ready: {}", corpus.open());
        case "ask" -> ask(corpus, archive, question);
        case "grade", "judge" -> grade(run, judge, golden, handGrades, recordedRun);
        case "support" -> support(agreement, recordedRun, chatModel);
        case "longcontext" -> longContext(units, longContextAnswerer, archive, judge, golden);
        case "golden" ->
            golden(
                corpus,
                goldenRun(archive, corpus, citations, search),
                GoldenSet.read(golden),
                new GoldenRun.Conditions(
                    chatModel,
                    embeddingModel,
                    digests(ollama),
                    sampling(ollama),
                    promptsHash(answererPersona, quoterPersona, directPersona)));
        default -> throw new IllegalArgumentException("Unknown archive.run: " + run);
      }
    };
  }

  /// The runner, scored the way this search has to be scored.
  private static GoldenRun goldenRun(
      Archive archive, SpecCorpus corpus, CitationChecker citations, String search) {
    return new GoldenRun(archive, new GoldenScorer(corpus.checkout()), citations, scoreBy(search));
  }

  /// Grading a saved run, or checking the judge against the hand labels: the same inputs either
  /// way, so they share an arm.
  private static void grade(
      String run, AnswerJudge judge, Resource golden, String handGrades, String recordedRun) {
    if ("grade".equals(run.strip().toLowerCase(Locale.ROOT))) {
      GradedAnswers.of(judge, GoldenSet.read(golden), Path.of(recordedRun));
    } else {
      calibrate(judge, GoldenSet.read(golden), handGrades, recordedRun);
    }
  }

  /// Four passages against the whole spec, on the questions that name one.
  ///
  /// Both sides are graded by the same judge in the same process, so neither inherits a grade from
  /// an older run with a different prompt.
  private static void longContext(
      Optional<ElementUnits> units,
      ChatClient wholeSpec,
      Archive archive,
      AnswerJudge judge,
      Resource golden) {
    var corpus =
        units.orElseThrow(
            () ->
                new IllegalStateException(
                    "The long-context baseline needs the element units; run with"
                        + " --archive.search=elements (the default)."));
    List<GoldenSet.Question> questions = GoldenSet.read(golden);
    List<LongContextBaseline.Answered> answered =
        new LongContextBaseline(corpus, wholeSpecLine(wholeSpec), retrievalLine(archive))
            .run(questions);
    var byId = questions.stream().collect(Collectors.toMap(GoldenSet.Question::id, q -> q));
    var whole = new LinkedHashMap<String, Judgement>();
    var four = new LinkedHashMap<String, Judgement>();
    for (LongContextBaseline.Answered row : answered) {
      GoldenSet.Question question = byId.get(row.id());
      whole.put(row.id(), judge.on(question, row.fromTheWholeSpec()));
      four.put(row.id(), judge.on(question, row.fromRetrieval()));
    }
    LongContextBaseline.write(
        LongContextBaseline.report(answered, whole, four),
        answered,
        Path.of("build", "long-context.md"));
  }

  /// The whole-spec answerer, asked for one window at a time.
  ///
  /// Same persona and same pinned sampling as the bean it wraps; only `num_ctx` moves, and it moves
  /// because a window is memory reserved up front. The bean's own window stays the ceiling.
  private static LongContextBaseline.WholeSpecAnswerer wholeSpecLine(ChatClient wholeSpec) {
    return (prompt, window) ->
        wholeSpec
            .prompt()
            .user(prompt)
            .options(ArchiveConfiguration.pinned(0.0).numCtx(window))
            .call()
            .content();
  }

  /// The archive's ordinary answering path, as the baseline's second answerer. An abstention is an
  /// empty answer, which is what the judge should see: the line declined.
  private static LongContextBaseline.Answerer retrievalLine(Archive archive) {
    return question -> {
      Archive.Ruling ruling = archive.ask(question, UUID.randomUUID().toString());
      return ruling.answerer().said().map(Commentary::answer).orElse("");
    };
  }

  /// Spring AI's entailment check against this series' quotation tracer, over a run on disk.
  private static void support(SupportAgreement agreement, String recordedRun, String model) {
    agreement.report(Path.of(recordedRun), model, Path.of("build", "support-agreement.md"));
  }

  /// One hash over all three personas, so a word changed in any of them shows up in every row.
  static String promptsHash(Resource answerer, Resource quoter, Resource direct) {
    return GoldenRun.hash(List.of(text(answerer), text(quoter), text(direct)));
  }

  /// The options every measured call was made with, for the run stamp. Pinning them in
  /// `ArchiveConfiguration` and leaving them out of the stamp would record a run as repeatable
  /// without saying what it would have to be repeated with.
  static GoldenRun.Sampling sampling(String ollamaUrl) {
    return new GoldenRun.Sampling(
        ArchiveConfiguration.SEED,
        ArchiveConfiguration.TOP_P,
        ArchiveConfiguration.TOP_K,
        ArchiveConfiguration.KEEP_ALIVE,
        OllamaModel.version(ollamaUrl));
  }

  /// An element search is scored by address, Episode 3's chunk store by text overlap. Getting this
  /// wrong does not fail: it reports a much lower Hit@4 and says nothing about why.
  static GoldenRun.By scoreBy(String search) {
    return "chunks".equalsIgnoreCase(search.strip()) ? GoldenRun.By.OVERLAP : GoldenRun.By.ADDRESS;
  }

  /// Checks the judge against the person who graded Episode 3's answers by hand.
  private static void calibrate(
      AnswerJudge judge, List<GoldenSet.Question> questions, String handGrades, String run) {
    JudgeCalibration.run(
        judge,
        questions,
        new JudgeCalibration.Comparison(
            Path.of(handGrades), Path.of(run), Path.of("build", "judge-calibration.md")));
  }

  /// One question from the command line, or a conversation at the terminal if none was given.
  private static void ask(SpecCorpus corpus, Archive archive, String question) throws IOException {
    corpus.open();
    if (question.isBlank()) {
      converse(archive);
      return;
    }
    print(archive.ask(question, UUID.randomUUID().toString()));
  }

  /// Every golden question, scored, with the run files written beside the summary.
  private static void golden(
      SpecCorpus corpus,
      GoldenRun goldenRun,
      List<GoldenSet.Question> questions,
      GoldenRun.Conditions conditions) {
    var manifest = corpus.open();
    var rows = goldenRun.run(questions);
    log.info("Baseline:\n{}", goldenRun.write(rows, conditions.over(manifest), Path.of("build")));
  }

  /// Questions typed at the terminal, one conversation, until an empty line.
  private static void converse(Archive archive) throws IOException {
    String session = UUID.randomUUID().toString();
    var typed = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    System.out.println("Ask the archive. An empty line ends the session.");
    for (String line = typed.readLine(); line != null && !line.isBlank(); line = typed.readLine()) {
      print(archive.ask(line.strip(), session));
    }
  }

  /// The quotations and their addresses: provenance, printed.
  private static void print(Archive.Ruling ruling) {
    for (Quote q : ruling.quotes()) {
      log.info("  {} {} (rank {})", q.label(), q.address(), q.rank());
    }
    ruling
        .answerer()
        .verdicts()
        .forEach(verdict -> log.info("  [{}] {}", verdict.status(), verdict.reason()));
  }

  /// The exact model builds, from Ollama's `/api/tags`. A tag such as `qwen3:8b` can be re-pointed
  /// at new weights; the digest cannot.
  static Map<String, String> digests(String baseUrl) {
    var out = new LinkedHashMap<String, String>();
    try (var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
      var response =
          http.send(
              HttpRequest.newBuilder(URI.create(baseUrl + "/api/tags")).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      var tags = JsonMapper.shared().readTree(response.body());
      for (JsonNode model : tags.path("models")) {
        out.put(model.path("name").asString(""), model.path("digest").asString(""));
      }
    } catch (IOException | RuntimeException e) {
      log.warn("Could not read model digests from Ollama: {}", e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return out;
  }

  private static String text(Resource resource) {
    try {
      return resource.getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
