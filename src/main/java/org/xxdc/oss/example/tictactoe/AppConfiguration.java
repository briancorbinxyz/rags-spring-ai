package org.xxdc.oss.example.tictactoe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xxdc.oss.example.BotPlayer;
import org.xxdc.oss.example.Game;
import org.xxdc.oss.example.GameState;
import org.xxdc.oss.example.PlayerNode;
import org.xxdc.oss.example.bot.BotStrategy;

/// Plays one bot-versus-bot game with Alpha commentating, then writes two files to `build/`, named
/// `episode-2-<model>-<mode>-window-<N>`:
///
/// - `.csv`: one row per move, with prompt tokens, Alpha's claims counted by verdict and the
///   reasons for any that failed, how many claims only echo the move report, and whether Beta
///   quoted its colour line exactly;
///
/// - `.jsonl`: the same rows in full, with every prompt, claim and source, so a failure can be
///   diagnosed from the run itself.
@Configuration
public class AppConfiguration {

  private static final Logger log = LoggerFactory.getLogger(AppConfiguration.class);

  /// AlphaBeta plays perfectly, so against MCTS almost every game is a draw. Set
  /// `game.x-strategy=RANDOM` to see wins, and with them real win commentary for the checks.
  Game newGame(ToIntFunction<GameState> x, ToIntFunction<GameState> o) {
    return new Game(
        3,
        false,
        new PlayerNode.Local<>("X", new BotPlayer(x)),
        new PlayerNode.Local<>("O", new BotPlayer(o)));
  }

  /// A strategy that replays a fixed game: the n-th move of the game is `moves[n]`, whichever
  /// player makes it. Models commentate different games when MCTS chooses the moves, so two models
  /// can only be compared like for like on a replayed game. Every run logs its moves in this form.
  static ToIntFunction<GameState> replay(int[] moves) {
    return state -> {
      int played = 0;
      for (String cell : state.board().content()) {
        if (cell != null && !cell.isEmpty()) {
          played++;
        }
      }
      if (played >= moves.length) {
        throw new IllegalStateException("game.moves ran out after " + moves.length + " moves");
      }
      return moves[played];
    };
  }

  /// The named strategies are static fields on {@link BotStrategy}, not enum constants.
  static ToIntFunction<GameState> strategy(String name) {
    return switch (name.strip().toUpperCase(Locale.ROOT)) {
      case "RANDOM" -> BotStrategy.RANDOM;
      case "MINIMAX" -> BotStrategy.MINIMAX;
      case "ALPHABETA" -> BotStrategy.ALPHABETA;
      case "MAXN" -> BotStrategy.MAXN;
      case "PARANOID" -> BotStrategy.PARANOID;
      case "MCTS" -> BotStrategy.MCTS;
      case "DEFAULT" -> BotStrategy.DEFAULT;
      default -> throw new IllegalArgumentException("unknown bot strategy: " + name);
    };
  }

  @Bean
  CommandLineRunner playOneGame(
      PlayByPlayPersona alpha,
      @Value("${alpha.memory.max-messages:20}") int window,
      @Value("${alpha.play-on-startup:true}") boolean play,
      @Value("${commentary.mode:duo}") String mode,
      @Value("${spring.ai.ollama.chat.model:mistral}") String model,
      @Value("${game.x-strategy:ALPHABETA}") String xStrategy,
      @Value("${game.o-strategy:MCTS}") String oStrategy,
      @Value("${game.moves:}") String fixedMoves) {
    var bots = bots(fixedMoves, xStrategy, oStrategy);
    // The model is in the name so runs on different models sit side by side.
    String name =
        "episode-2-%s-%s-window-%d"
            .formatted(model.replaceAll("[^A-Za-z0-9.]+", "-"), mode, window);
    return _ -> {
      if (play) {
        commentateOneGame(alpha, bots, name);
      }
    };
  }

  /// The two bots that will play the game.
  private record Bots(ToIntFunction<GameState> x, ToIntFunction<GameState> o) {}

  /// Both bots replay the fixed game when there is one, so the whole game is settled in advance.
  private static Bots bots(String fixedMoves, String xStrategy, String oStrategy) {
    if (fixedMoves.isBlank()) {
      return new Bots(strategy(xStrategy), strategy(oStrategy));
    }
    int[] moves =
        Arrays.stream(fixedMoves.split(","))
            .map(String::strip)
            .mapToInt(Integer::parseInt)
            .toArray();
    return new Bots(replay(moves), replay(moves));
  }

  /// `Game.playWithAction` lets the action throw, so this does too, as the runner does.
  private void commentateOneGame(PlayByPlayPersona alpha, Bots bots, String name) throws Exception {
    try (var game = newGame(bots.x(), bots.o())) {
      var broadcast = alpha.broadcast(game);
      game.playWithAction(broadcast::onMove);
      var summary = broadcast.postGame(game);
      logHowToReplayThisGame(game);

      var rows = new ArrayList<>(broadcast.moves());
      rows.add(summary);
      writeCsv(Path.of("build", name + ".csv"), rows);
      writeJsonLines(Path.of("build", name + ".jsonl"), rows);
    }
  }

  /// The moves in the form `game.moves` takes, so another model can commentate this game.
  private static void logHowToReplayThisGame(Game game) {
    String played =
        game.history().stream()
            .skip(1)
            .map(state -> String.valueOf(state.lastMove()))
            .collect(Collectors.joining(","));
    log.info("Replay this game with --game.moves={}", played);
  }

  private static void writeCsv(Path path, List<PlayByPlayPersona.MoveCommentary> rows)
      throws IOException {
    var file = new MeasurementFile();
    var lines = new ArrayList<String>();
    lines.add(file.header());
    for (var row : rows) {
      lines.add(file.lineFor(row));
    }
    Files.createDirectories(path.getParent());
    Files.write(path, lines);
    file.logWhoRepeatedThemselves();
  }

  /// One column: its name in the header, and how to read its cell from a row.
  record Column(String header, Function<Row, String> cell) {}

  /// One row's inputs, read once.
  ///
  /// `repeatedClaims` and `asideRepeat` are settled before any column asks for them, because they
  /// come from watchers that carry state from row to row: a column that called a watcher itself
  /// would change what the next row says about itself.
  record Row(
      PlayByPlayPersona.MoveCommentary move,
      FactChecker.Tally tally,
      int repeatedClaims,
      boolean asideRepeat) {

    int claims() {
      return move.commentary().claims().size();
    }

    /// A clean sheet means little if most claims had nothing checkable in them.
    String checkedShare() {
      return claims() == 0
          ? ""
          : "%.2f".formatted((double) (claims() - tally.unchecked()) / claims());
    }

    /// Beta's half of a row, which is empty in a solo broadcast.
    String fromColour(Function<ColourCommentator.Colour, String> cell) {
      return move.colour().map(cell).orElse("");
    }
  }

  /// The measurement file: one column per thing worth counting, and the three watchers that can
  /// only answer by remembering the rows before this one.
  ///
  /// Twenty-one columns are a lot to read as one `String.join`, and the names have to match the
  /// header exactly, so each column carries its own name and is written on its own line. The names
  /// are published: the article's numbers are read from this file by column.
  static final class MeasurementFile {

    private final Repeats playByPlayAsides = new Repeats();
    private final Repeats playByPlayClaims = new Repeats();
    private final Repeats colourAsides = new Repeats();

    private final List<Column> columns =
        List.of(
            new Column("move", row -> String.valueOf(row.move().move())),
            new Column("from_model", row -> String.valueOf(row.move().fromModel())),
            new Column("prompt_tokens", MeasurementFile::promptTokens),
            new Column("claims", row -> String.valueOf(row.claims())),
            new Column("over_limit", row -> String.valueOf(row.claims() > Commentary.MAX_CLAIMS)),
            new Column("echoes", MeasurementFile::echoes),
            new Column("claim_repeats", row -> String.valueOf(row.repeatedClaims())),
            new Column("checked_share", Row::checkedShare),
            new Column("supported", row -> String.valueOf(row.tally().supported())),
            new Column("contradicted", row -> String.valueOf(row.tally().contradicted())),
            new Column("unsupported", row -> String.valueOf(row.tally().unsupported())),
            new Column("unchecked", row -> String.valueOf(row.tally().unchecked())),
            new Column("problems", row -> quote(String.join(" | ", row.tally().problems()))),
            new Column("aside", row -> quote(row.move().commentary().aside())),
            new Column("aside_repeat", row -> String.valueOf(row.asideRepeat())),
            new Column("answer", row -> quote(row.move().commentary().answer())),
            new Column(
                "beta_answer", row -> quote(row.fromColour(beta -> beta.commentary().answer()))),
            new Column(
                "beta_quote_verified",
                row -> row.fromColour(beta -> String.valueOf(beta.quoteVerified()))),
            new Column(
                "beta_stray_sources",
                row -> row.fromColour(beta -> String.valueOf(beta.straySources()))),
            new Column(
                "beta_echoes",
                row ->
                    row.fromColour(
                        beta -> String.valueOf(EchoCheck.count(beta.commentary(), beta.report())))),
            new Column(
                "beta_aside_repeat",
                row ->
                    row.fromColour(
                        beta -> String.valueOf(colourAsides.repeats(beta.commentary().aside())))));

    String header() {
      return columns.stream().map(Column::header).collect(Collectors.joining(","));
    }

    String lineFor(PlayByPlayPersona.MoveCommentary move) {
      var row = read(move);
      return columns.stream()
          .map(column -> column.cell().apply(row))
          .collect(Collectors.joining(","));
    }

    /// Asks the watchers about this row, before any column can.
    private Row read(PlayByPlayPersona.MoveCommentary move) {
      int repeatedClaims = 0;
      for (var claim : move.commentary().claims()) {
        repeatedClaims += playByPlayClaims.repeats(claim.text()) ? 1 : 0;
      }
      boolean asideRepeat = playByPlayAsides.repeats(move.commentary().aside());
      return new Row(move, FactChecker.Tally.of(move.verdicts()), repeatedClaims, asideRepeat);
    }

    void logWhoRepeatedThemselves() {
      log.info(
          "Asides: Alpha {}, Beta {}. Alpha's claims: {}",
          playByPlayAsides.summary(),
          colourAsides.summary(),
          playByPlayClaims.summary());
    }

    private static String promptTokens(Row row) {
      var tokens = row.move().promptTokens();
      return tokens == null ? "" : tokens.toString();
    }

    private static String echoes(Row row) {
      return String.valueOf(EchoCheck.count(row.move().commentary(), row.move().prompt()));
    }
  }

  /// Lines one commentator has already used in a game: asides or claims. A joke told twice is a
  /// weaker joke, a claim made twice is filler, and a model with a memory of the game can still
  /// repeat itself; nothing else in the run would notice.
  static final class Repeats {
    private final Set<String> seen = new HashSet<>();
    private int told;

    /// True when this line, ignoring case, spacing and punctuation, was already used.
    boolean repeats(String line) {
      String key =
          line == null ? "" : line.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip();
      if (key.isEmpty()) {
        return false;
      }
      told++;
      return !seen.add(key);
    }

    String summary() {
      return "%d distinct of %d".formatted(seen.size(), told);
    }
  }

  /// One JSON object per row, written by hand so the episode needs no JSON library of its own.
  private static void writeJsonLines(Path path, List<PlayByPlayPersona.MoveCommentary> rows)
      throws IOException {
    var lines = new ArrayList<String>();
    for (var row : rows) {
      lines.add(
          "{\"move\":%d,\"prompt\":%s,\"reply\":%s,\"verdicts\":%s,\"beta\":%s}"
              .formatted(
                  row.move(),
                  json(row.prompt()),
                  json(row.commentary()),
                  jsonVerdicts(row.verdicts()),
                  jsonColour(row.colour())));
    }
    Files.createDirectories(path.getParent());
    Files.write(path, lines);
  }

  private static String jsonVerdicts(List<FactChecker.Verdict> verdicts) {
    return verdicts.stream()
        .map(
            verdict ->
                "{\"claim\":%s,\"status\":%s,\"reason\":%s}"
                    .formatted(
                        json(verdict.claim().text()),
                        json(verdict.status().name()),
                        json(verdict.reason())))
        .collect(Collectors.joining(",", "[", "]"));
  }

  /// Beta's half of a row, or JSON `null` in a solo broadcast.
  private static String jsonColour(Optional<ColourCommentator.Colour> colour) {
    return colour
        .map(
            beta ->
                "{\"prompt\":%s,\"offered\":%s,\"reply\":%s,\"quote_verified\":%s}"
                    .formatted(
                        json(beta.prompt()),
                        json(beta.offered()),
                        json(beta.commentary()),
                        beta.quoteVerified()))
        .orElse("null");
  }

  private static String json(Commentary commentary) {
    var claims =
        commentary.claims().stream()
            .map(
                claim ->
                    "{\"text\":%s,\"sources\":%s}"
                        .formatted(
                            json(claim.text()),
                            claim.sources().stream()
                                .map(AppConfiguration::json)
                                .collect(Collectors.joining(",", "[", "]"))))
            .collect(Collectors.joining(",", "[", "]"));
    return "{\"aside\":%s,\"claims\":%s}".formatted(json(commentary.aside()), claims);
  }

  static String json(String text) {
    if (text == null) {
      return "null";
    }
    var out = new StringBuilder("\"");
    for (int at = 0; at < text.length(); at++) {
      char ch = text.charAt(at);
      switch (ch) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (ch < 0x20) {
            out.append("\\u%04x".formatted((int) ch));
          } else {
            out.append(ch);
          }
        }
      }
    }
    return out.append('"').toString();
  }

  private static String quote(String cell) {
    return "\"" + (cell == null ? "" : cell.replace("\"", "\"\"")) + "\"";
  }
}
