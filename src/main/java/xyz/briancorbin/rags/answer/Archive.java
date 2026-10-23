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

  /**
   * Everything that happened for one question, so it can be checked and written down.
   *
   * @param retrieved the chunks the search returned, best first; in duo mode Quoter's, in solo mode
   *     Answerer's
   * @param quoter Quoter's reply, empty in solo mode or if Quoter could not be reached
   * @param quoterVerdicts the resolver's verdict on each of Quoter's claims (Answerer's in solo
   *     mode)
   * @param quotes the quotations traced to a chunk, as Answerer was shown them
   * @param answerer Answerer's reply, empty if Answerer could not be reached
   * @param answererVerdicts the citation check of each of Answerer's claims (the resolver's, in
   *     solo)
   * @param abstained Answerer said the answer is not in the archive and cited nothing
   */
  public record Ruling(
      String question,
      Mode mode,
      List<Document> retrieved,
      Optional<Commentary> quoter,
      List<Verdict> quoterVerdicts,
      List<Quote> quotes,
      Optional<Commentary> answerer,
      List<Verdict> answererVerdicts,
      boolean abstained,
      Integer quoterPromptTokens,
      Integer answererPromptTokens,
      long quoterMillis,
      long answererMillis,
      String answererPrompt) {

    public long millis() {
      return quoterMillis + answererMillis;
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

    return new Ruling(
        question,
        Mode.QUOTED,
        retrieved,
        quoterReply.commentary(),
        resolution.verdicts(),
        resolution.quotes(),
        answererReply.commentary(),
        answererVerdicts,
        answererReply.commentary().isPresent()
            && citations.abstained(answererSaid, resolution.quotes()),
        quoterReply.promptTokens(),
        answererReply.promptTokens(),
        quoterReply.millis(),
        answererReply.millis(),
        prompt);
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
    return new Ruling(
        question,
        Mode.DIRECT,
        retrieved,
        Optional.empty(),
        resolution.verdicts(),
        resolution.quotes(),
        answererReply.commentary(),
        resolution.verdicts(),
        abstained,
        null,
        answererReply.promptTokens(),
        0,
        answererReply.millis(),
        question);
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
