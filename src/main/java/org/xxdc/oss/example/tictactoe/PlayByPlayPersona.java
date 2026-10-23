package org.xxdc.oss.example.tictactoe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.xxdc.oss.example.Game;
import org.xxdc.oss.example.GameState;
import org.xxdc.oss.example.analysis.StrategicTurningPoint;
import org.xxdc.oss.example.commentary.CommentaryPersona;

/// Alpha, the series' ship's computer, as a tic-tac-toe commentator with a memory.
///
/// It still implements Episode 1's {@link CommentaryPersona}, and still decorates a scripted
/// persona, which it falls back to when the model cannot be reached. What is new is {@link
/// #broadcast(Game)}: a commentary that hears about every move, not just turning points, and
/// remembers the game it is watching.
public final class PlayByPlayPersona implements CommentaryPersona {

  private static final Logger log = LoggerFactory.getLogger(PlayByPlayPersona.class);

  /// Who is in the commentary booth.
  /// @param playByPlay the model Alpha speaks through
  /// @param scripted the persona to fall back to when that model cannot be reached
  /// @param colourCommentator Beta, or empty for a single-commentator broadcast, in which case
  ///     Alpha is offered the colour lines itself
  public record Booth(
      ChatClient playByPlay,
      CommentaryPersona scripted,
      Optional<ColourCommentator> colourCommentator) {}

  /// The prompt that asks for the post-game summary. A constant because it is sent verbatim, and a
  /// word changed here is a different measurement.
  private static final String SUMMARY_PROMPT =
      "The game is over. Give the post-game summary: one claim per turning point you can"
          + " see in this conversation. Say what it meant for the game, put its move number in"
          + " the text (\"move 5\"), and put that move in the claim's sources.";

  private final Booth booth;
  private final ColourLines colour;
  private final int memoryWindow;
  private final GameFactChecker checker = new GameFactChecker();

  public PlayByPlayPersona(Booth booth, ColourLines colour, int memoryWindow) {
    this.booth = booth;
    this.colour = colour;
    this.memoryWindow = memoryWindow;
  }

  /// One move's commentary, with what it cost and a verdict on each of Alpha's claims. `colour` is
  /// Beta's contribution on a quiet move, when there is one.
  public record MoveCommentary(
      int move,
      String prompt,
      Commentary commentary,
      Integer promptTokens,
      List<FactChecker.Verdict> verdicts,
      boolean fromModel,
      Optional<ColourCommentator.Colour> colour) {}

  /// Episode 1's contract: comment on a turning point. The conversation id comes from the game's
  /// `GameContext`, a scoped value bound for the duration of `Game.play`, so this only works while
  /// a game is being played.
  @Override
  public String comment(StrategicTurningPoint turningPoint) {
    Optional<String> gameId = Game.gameContext().map(c -> c.id() + ":alpha");
    if (gameId.isEmpty()) {
      return booth.scripted().comment(turningPoint);
    }
    var prompt =
        "Move %d: %s %s.\nBoard:\n%s"
            .formatted(
                turningPoint.moveNumber(),
                turningPoint.playerMarker(),
                MoveReport.describe(turningPoint),
                MoveReport.render(turningPoint.gameState()));
    return ask(gameId.get(), prompt)
        .map(ResponseEntity::entity)
        .map(Commentary::answer)
        .orElseGet(() -> booth.scripted().comment(turningPoint));
  }

  /// Starts commentary for one game. Pass `broadcast::onMove` to `playWithAction`.
  public Broadcast broadcast(Game game) {
    return new Broadcast(game.id().toString());
  }

  /// Commentary for one game. Each commentator has its own conversation, and so its own memory: in
  /// a shared one, both would appear as "the assistant" and the model would blur who said what.
  public final class Broadcast {

    private final String conversationId;
    private final String colourConversationId;
    private final List<MoveCommentary> moves = new ArrayList<>();
    private int quietMoves;

    private Broadcast(String gameId) {
      this.conversationId = gameId + ":alpha";
      this.colourConversationId = gameId + ":beta";
    }

    /// Called by the game after every move.
    public void onMove(Game game) {
      List<GameState> history = List.copyOf(game.history());
      int move = game.moveNumber();
      var called = theMoveAsAlphaWillSeeIt(history, move);
      var prompt = called.report().toPrompt();
      var reply = ask(conversationId, prompt);
      var commentary =
          reply
              .map(ResponseEntity::entity)
              .orElseGet(() -> Commentary.factual(called.report().plainLine()));
      var verdicts =
          checker.checkAll(
              commentary, new GameFactChecker.Evidence(history, move, earliestVisible(move)));
      announce(move, commentary, verdicts);
      moves.add(
          new MoveCommentary(
              move,
              prompt,
              commentary,
              promptTokens(reply),
              verdicts,
              answered(reply),
              whatBetaMadeOfIt(called, commentary)));
    }

    /// The move report Alpha is sent, and the colour line this move earned, if it earned one.
    private record Called(MoveReport report, Optional<String> colourLine) {}

    /// Picks the colour line first, because who gets it depends on whether Beta is in the booth: in
    /// a solo broadcast the line goes into Alpha's own report, otherwise Beta keeps it.
    private Called theMoveAsAlphaWillSeeIt(List<GameState> history, int move) {
      GameState before = history.get(history.size() - 2);
      GameState after = history.getLast();
      var withoutColour = MoveReport.of(before, after, move, Optional.empty());
      Optional<String> line =
          withoutColour.quiet() ? colour.forQuietMove(quietMoves++) : Optional.empty();
      var report =
          booth.colourCommentator().isPresent()
              ? withoutColour
              : MoveReport.of(before, after, move, line);
      return new Called(report, line);
    }

    /// Beta speaks on quiet moves only, after Alpha. Turning points are Alpha's.
    private Optional<ColourCommentator.Colour> whatBetaMadeOfIt(
        Called called, Commentary commentary) {
      return booth
          .colourCommentator()
          .flatMap(
              beta ->
                  called
                      .colourLine()
                      .map(
                          line ->
                              beta.onQuietMove(
                                  colourConversationId, called.report(), commentary, line)));
    }

    private static void announce(
        int move, Commentary commentary, List<FactChecker.Verdict> verdicts) {
      log.info("Move {}: {}", move, commentary.answer());
      logProblems(verdicts);
      if (commentary.hasAside()) {
        log.info("  ({})", commentary.aside());
      }
    }

    /// The post-game summary, from memory. Turning points are not re-sent.
    public MoveCommentary postGame(Game game) {
      List<GameState> history = List.copyOf(game.history());
      int last = game.moveNumber();
      var reply = ask(conversationId, SUMMARY_PROMPT);
      var commentary =
          reply
              .map(ResponseEntity::entity)
              .orElseGet(() -> Commentary.factual("No summary: the model could not be reached."));
      var verdicts =
          checker.checkAll(
              commentary,
              new GameFactChecker.Evidence(history, last, earliestVisible(last + 1), true));
      log.info("Post-game: {}", commentary.answer());
      logProblems(verdicts);
      return new MoveCommentary(
          last + 1,
          SUMMARY_PROMPT,
          commentary,
          promptTokens(reply),
          verdicts,
          answered(reply),
          Optional.empty());
    }

    public List<MoveCommentary> moves() {
      return List.copyOf(moves);
    }

    public String conversationId() {
      return conversationId;
    }
  }

  private static void logProblems(List<FactChecker.Verdict> verdicts) {
    for (var verdict : verdicts) {
      if (verdict.status() == FactChecker.Status.CONTRADICTED
          || verdict.status() == FactChecker.Status.UNSUPPORTED) {
        log.warn("  {}: {} ({})", verdict.status(), verdict.claim().text(), verdict.reason());
      }
    }
  }

  /// The oldest move whose report is still in memory when Alpha answers about move n. Each move is
  /// two messages (report and reply), so a window of W messages holds about W/2 moves.
  int earliestVisible(int move) {
    return Math.max(1, move - memoryWindow / 2);
  }

  private Optional<ResponseEntity<ChatResponse, Commentary>> ask(
      String conversationId, String prompt) {
    try {
      return Optional.of(
          booth
              .playByPlay()
              .prompt()
              .user(prompt)
              .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
              .call()
              .responseEntity(
                  Commentary.class, spec -> spec.useProviderStructuredOutput().validateSchema()));
    } catch (RuntimeException e) {
      // The memory advisor stored the report before the call failed, so this conversation now
      // holds a report with no reply. The row is marked from_model=false; leave it out of any
      // comparison of windows.
      log.warn("Alpha is unavailable, falling back: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private static boolean answered(Optional<ResponseEntity<ChatResponse, Commentary>> reply) {
    return reply.map(ResponseEntity::entity).isPresent();
  }

  /// Prompt tokens as Ollama reports them. If a reply failed validation and was retried, Spring AI
  /// adds up the usage of every attempt, so treat a sudden jump as a retry, not a longer prompt.
  private static Integer promptTokens(Optional<ResponseEntity<ChatResponse, Commentary>> reply) {
    return reply
        .map(ResponseEntity::response)
        .map(r -> r.getMetadata().getUsage().getPromptTokens())
        .orElse(null);
  }
}
