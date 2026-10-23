package xyz.briancorbin.rags.answer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import xyz.briancorbin.rags.answer.FactChecker.Verdict;

/**
 * One question, one hand-off: Quoter searches the archive and quotes it, the program traces the
 * quotations to their chunks, and Answerer answers from the traced quotations only.
 *
 * <p>Quoter's {@link ChatClient} carries {@link QuestionAnswerAdvisor}, so every call to Quoter is
 * a vector search followed by a prompt with the chunk texts pasted in. Answerer's carries no
 * retriever: Answerer never touches the archive, and it never sees a chunk, only quotations that
 * {@link QuoteResolver} found word for word in one.
 *
 * <p>In solo mode the retriever moves to Answerer, which searches, quotes and answers in one call,
 * and its quotations go through the same resolver.
 */
public final class Archive {

  private static final Logger log = LoggerFactory.getLogger(Archive.class);

  public enum Mode {
    /** Quote first, then answer only from what was traced. */
    QUOTED,
    /** One model answers straight from what the retriever returned. */
    DIRECT
  }

  /** One model call: the reply if there was one, and what came with it. */
  public record Reply(
      Optional<Commentary> commentary,
      List<Document> retrieved,
      Integer promptTokens,
      long millis) {}

  /** What one model call cost: what it was charged for, and how long it took. */
  public record Cost(Integer promptTokens, long millis) {

    static Cost none() {
      return new Cost(null, 0);
    }
  }

  /**
   * What one role did on one question: what it said, the verdicts on what it said, and what the
   * call cost. The two roles are measured the same way, so they are the same shape.
   */
  public record Turn(Optional<Commentary> said, List<Verdict> verdicts, Cost cost) {

    static Turn silent() {
      return new Turn(Optional.empty(), List.of(), Cost.none());
    }
  }

  /**
   * Everything that happened for one question, so it can be checked and written down.
   *
   * @param retrieved the chunks the search returned, best first; whichever role did the searching
   * @param quoter what the quoter said and what it cost; silent in direct mode
   * @param answerer what the answerer said and what it cost
   * @param quotes the quotations traced to a chunk, as the answerer was shown them
   * @param abstained the answerer said the answer is not in the archive and cited nothing
   * @param answererPrompt the prompt the answerer was given, kept so a run can be read back
   */
  public record Ruling(
      String question,
      Mode mode,
      List<Document> retrieved,
      Turn quoter,
      Turn answerer,
      List<Quote> quotes,
      boolean abstained,
      String answererPrompt) {

    public long millis() {
      return quoter.cost().millis() + answerer.cost().millis();
    }

    /** Named rather than positional: eight components in a row is a mistake waiting to happen. */
    public static Builder of(String question, Mode mode) {
      return new Builder(question, mode);
    }

    public static final class Builder {

      private final String question;
      private final Mode mode;
      private List<Document> retrieved = List.of();
      private Turn quoter = Turn.silent();
      private Turn answerer = Turn.silent();
      private List<Quote> quotes = List.of();
      private boolean abstained;
      private String answererPrompt = "";

      private Builder(String question, Mode mode) {
        this.question = question;
        this.mode = mode;
      }

      public Builder retrieved(List<Document> retrieved) {
        this.retrieved = retrieved;
        return this;
      }

      public Builder quoter(Turn quoter) {
        this.quoter = quoter;
        return this;
      }

      public Builder answerer(Turn answerer) {
        this.answerer = answerer;
        return this;
      }

      public Builder quotes(List<Quote> quotes) {
        this.quotes = quotes;
        return this;
      }

      public Builder abstained(boolean abstained) {
        this.abstained = abstained;
        return this;
      }

      public Builder answererPrompt(String answererPrompt) {
        this.answererPrompt = answererPrompt;
        return this;
      }

      public Ruling build() {
        return new Ruling(
            question, mode, retrieved, quoter, answerer, quotes, abstained, answererPrompt);
      }
    }
  }

  private final ChatClient quoter;
  private final ChatClient answerer;
  private final ChatClient soloAnswerer;
  private final VectorStore store;
  private final QuoteResolver resolver;
  private final CitationChecker citations;
  private final Mode mode;

  public Archive(
      ChatClient quoter,
      ChatClient answerer,
      ChatClient soloAnswerer,
      VectorStore store,
      QuoteResolver resolver,
      CitationChecker citations,
      Mode mode) {
    this.quoter = quoter;
    this.answerer = answerer;
    this.soloAnswerer = soloAnswerer;
    this.store = store;
    this.resolver = resolver;
    this.citations = citations;
    this.mode = mode;
  }

  public Mode mode() {
    return mode;
  }

  /**
   * Answers one question. The conversation id keeps each commentator's memory separate, as in One
   * conversation per persona: {@code <id>:quoter} and {@code <id>:answerer}. A golden run uses a
   * fresh id per question, so no question's memory leaks into the next.
   */
  public Ruling ask(String question, String conversationId) {
    log.info("Q: {}", question);
    return mode == Mode.DIRECT
        ? direct(question, conversationId)
        : quoted(question, conversationId);
  }

  private Ruling quoted(String question, String conversationId) {
    // 1. Quoter searches. The advisor runs the vector search and pastes the chunk texts in.
    var quoterReply = call(quoter, conversationId + ":quoter", question);
    var retrieved = quoterReply.retrieved().isEmpty() ? search(question) : quoterReply.retrieved();
    var quoterSaid =
        quoterReply.commentary().orElse(Commentary.factual("Beta found nothing it would quote."));
    say("Beta", quoterSaid);

    // 2. The program traces every quotation to a retrieved chunk, or rejects it.
    QuoteResolver.Resolution resolution = resolver.resolve(quoterSaid, retrieved);

    // 3. Answerer answers from the traced quotations, and only from them.
    String prompt = answererPrompt(question, resolution.quotes());
    var answererReply = call(answerer, conversationId + ":answerer", prompt);
    var answererSaid =
        answererReply.commentary().orElse(Commentary.factual("Alpha is unavailable."));
    say("Alpha", answererSaid);
    var answererVerdicts = citations.checkAll(answererSaid, resolution.quotes());

    return Ruling.of(question, Mode.QUOTED)
        .retrieved(retrieved)
        .quoter(new Turn(quoterReply.commentary(), resolution.verdicts(), cost(quoterReply)))
        .answerer(new Turn(answererReply.commentary(), answererVerdicts, cost(answererReply)))
        .quotes(resolution.quotes())
        .abstained(
            answererReply.commentary().isPresent()
                && citations.abstained(answererSaid, resolution.quotes()))
        .answererPrompt(prompt)
        .build();
  }

  private Ruling direct(String question, String conversationId) {
    var answererReply = call(soloAnswerer, conversationId + ":answerer", question);
    var retrieved =
        answererReply.retrieved().isEmpty() ? search(question) : answererReply.retrieved();
    var answererSaid =
        answererReply.commentary().orElse(Commentary.factual("Alpha is unavailable."));
    say("Alpha", answererSaid);
    var resolution = resolver.resolve(answererSaid, retrieved);
    boolean abstained =
        answererReply.commentary().isPresent()
            && answererSaid.claims().stream().anyMatch(CitationChecker::abstains)
            && resolution.quotes().isEmpty();
    // In direct mode the answerer did its own searching, so the quoter has no turn and the
    // resolver's verdicts are on the answerer's own quotations.
    return Ruling.of(question, Mode.DIRECT)
        .retrieved(retrieved)
        .answerer(new Turn(answererReply.commentary(), resolution.verdicts(), cost(answererReply)))
        .quotes(resolution.quotes())
        .abstained(abstained)
        .answererPrompt(question)
        .build();
  }

  /** What Answerer is given: the question, and each traced quotation with its label and address. */
  static String answererPrompt(String question, List<Quote> quotes) {
    if (quotes.isEmpty()) {
      return "Question: %s%n%nQuotations Beta traced to the archive: none.".formatted(question);
    }
    return "Question: %s%n%nQuotations Beta traced to the archive. Cite them by label.%n%s"
        .formatted(
            question,
            quotes.stream()
                .map(Quote::toPrompt)
                .collect(Collectors.joining(System.lineSeparator())));
  }

  /**
   * Calls a commentator and asks for a {@link Commentary}. A failure costs the answer, not the run.
   * The retrieved chunks come back in the response metadata, where {@link QuestionAnswerAdvisor}
   * puts them under {@code qa_retrieved_documents}.
   */
  private static Cost cost(Reply reply) {
    return new Cost(reply.promptTokens(), reply.millis());
  }

  private Reply call(ChatClient client, String conversationId, String user) {
    long start = System.nanoTime();
    try {
      ResponseEntity<ChatResponse, Commentary> response =
          client
              .prompt()
              .user(user)
              .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
              .call()
              .responseEntity(
                  Commentary.class, spec -> spec.useProviderStructuredOutput().validateSchema());
      ChatResponse chat = response.response();
      List<Document> retrieved =
          chat == null
              ? List.of()
              : Objects.requireNonNullElse(
                  chat.getMetadata().<List<Document>>get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS),
                  List.of());
      Integer tokens = chat == null ? null : chat.getMetadata().getUsage().getPromptTokens();
      return new Reply(Optional.ofNullable(response.entity()), retrieved, tokens, since(start));
    } catch (RuntimeException e) {
      log.warn("No reply ({}): {}", conversationId, e.getMessage());
      return new Reply(Optional.empty(), List.of(), null, since(start));
    }
  }

  /**
   * The advisor's own search, run directly. Used only when a chat call failed before the advisor
   * could report what it found, so a model failure is not counted as a retrieval miss.
   */
  private List<Document> search(String question) {
    return store.similaritySearch(SearchRequest.builder().query(question).build());
  }

  private static void say(String who, Commentary c) {
    log.info("  {}: {}", who, c.answer());
    if (c.hasAside()) {
      log.info("    ({})", c.aside());
    }
  }

  private static long since(long start) {
    return (System.nanoTime() - start) / 1_000_000;
  }
}
