package org.xxdc.oss.example.tictactoe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.xxdc.oss.example.GameState;

/// Checks Alpha's claims against the game's own history, with no model involved.
///
/// For each claim it asks:
///
///  1. Does every move it mentions or cites exist, and come no later than this one?
///  2. Could Alpha see it? With a window of W messages, Alpha sees about the last W/2 move
///     reports. A move older than that is a memory Alpha cannot have.
///  3. Is every earlier move it mentions also in its sources?
///  4. When it names one move and one player, is that the player who made the move?
///  5. When it says a player has won, does the board agree, and if it names the line, is it that
///     line?
///  6. When it puts a player on a square ("X holds the centre"), does the player have a mark
///     there?
///  7. Does every line it names exist? A "diagonal" from top-right to bottom-right is the right
///     column.
///
/// It checks what can be checked cheaply and exactly, and says so when it cannot. A claim that X
/// "took the corner" on a move where X took the centre still passes when X holds a corner from
/// earlier: the square check asks whether the player has a mark there, not when it was made. The
/// win check is lexical: it looks for words like "wins" and skips sentences about blocking, threats
/// or what might happen, so an unusual phrasing can slip past it.
public final class GameFactChecker implements FactChecker<GameFactChecker.Evidence> {

  private static final Pattern MOVE = Pattern.compile("(?i)\\bmove\\s+(?<number>\\d+)\\b");
  private static final Pattern WIN =
      Pattern.compile(
          "(?i)\\b(?<winWord>wins|won|win|winning|victory|victorious|three in a row"
              + "|complete[sd]? (?:a|the|its) (?:row|column|diagonal|line))\\b");

  /// Words that turn a sentence about winning into something else, so it is not checked as a claim
  /// that someone has won. Four families: stopping a win (blocks, preventing), a win that might
  /// come (threatens, potential, a future win), a win being worked towards (aimed, trying, a
  /// setup), and a win denied (not, without, too late).
  ///
  /// Each verb is a stem with its endings, so a new inflection ("aimed", "preventing") does not
  /// slip through. The guard trades recall for precision: "X wins, and O cannot recover" is skipped
  /// too, because of "cannot".
  private static final Pattern NOT_A_WIN_CLAIM =
      Pattern.compile(
          "(?i)\\b(?<hedge>"
              // stopping a win
              + "block(?:s|ed|ing)?|stop(?:s|ped|ping)?|prevent(?:s|ed|ing)?"
              + "|den(?:y|ies|ied|ying)|thwart(?:s|ed|ing)?|foil(?:s|ed|ing)?"
              // a win that might come
              + "|threat(?:s|en|ens|ened|ening)?|would|could|might|nearly|almost|chances?"
              + "|potential(?:ly)?|possib(?:le|ly)|promis(?:e|es|ed|ing)|future|eventual(?:ly)?"
              // a win being worked towards
              + "|tr(?:y|ies|ied|ying)|attempt(?:s|ed|ing)?|aim(?:s|ed|ing)?|seek(?:s|ing)?|sought"
              + "|hop(?:e|es|ed|ing)|set(?:s|ting)? up|setups?|towards?"
              + "|chas(?:e|es|ed|ing)|pursu(?:e|es|ed|ing)"
              // a win denied
              + "|not|never|no|without|fail(?:s|ed|ing)?|unable|cannot|can't|won't|isn't|wasn't"
              + "|too late|short of|fell short|no one|nobody|neither)\\b");

  /// What the claims are checked against.
  /// @param history the game's states; index m is the board after move m, index 0 the empty board
  /// @param currentMove the move being commented on, or the last move after the game
  /// @param earliestVisibleMove the oldest move report still in Alpha's memory window
  /// @param summary true for the post-game summary, where there is no current move
  public record Evidence(
      List<GameState> history, int currentMove, int earliestVisibleMove, boolean summary) {

    public Evidence {
      history = List.copyOf(history);
    }

    /// Evidence for commentary on a move during the game.
    public Evidence(List<GameState> history, int currentMove, int earliestVisibleMove) {
      this(history, currentMove, earliestVisibleMove, false);
    }

    List<String> players() {
      return history.getLast().playerMarkers();
    }

    /// Who made move n: players move in turn, starting with the first marker.
    String playerOf(int move) {
      var players = players();
      return players.get((move - 1) % players.size());
    }
  }

  @Override
  public Verdict check(Claim claim, Evidence evidence) {
    var reading = new Reading(claim, evidence);
    var findings = new Findings();

    everyMoveWasPlayedAndIsStillInMemory(reading, findings);
    everyEarlierMoveItMentionsIsAlsoCited(reading, findings);
    theMoveBelongsToThePlayerItNames(reading, findings);
    theWinIsOnTheBoardAndOnTheLineItNames(reading, findings);
    everyLineItNamesCouldExist(reading, findings);
    theNamedPlayerHasAMarkOnTheSquare(reading, findings);

    return findings.verdictOn(claim);
  }

  /// What a claim says, read once.
  ///
  /// Every check needs some of this and no check needs all of it, and reading the same sentence six
  /// times with six regexes is both slower and harder to follow than reading it once.
  private static final class Reading {

    private final Evidence evidence;
    private final String text;

    /// Moves named in the claim itself, and moves named in the sources it cites.
    private final Set<Integer> mentioned;

    private final Set<Integer> cited;

    /// Every move either names, so the checks that apply to all of them make one pass.
    private final Set<Integer> everyMove;

    /// The players the claim names, in the board's order.
    private final List<String> named;

    /// The move the claim is about: the one in its text.
    ///
    /// During the game, sources are not a guide: models cite what a move responds to, so a claim
    /// about move 8 sourced to move 7 is still about move 8. Only in the post-game summary, where
    /// there is no current move, does a claim with no move in its text take the move from its
    /// sources: "O blocked an immediate loss" with sources ["move 4"] is about move 4.
    private final Set<Integer> about;

    /// The move whose board the claim describes: the one it names, or the current one.
    private final int boardMove;

    /// True when exactly one board can answer the claim, which the board checks need.
    private final boolean oneBoard;

    private final List<BoardVocabulary.Named> lines;
    private final List<BoardVocabulary.Named> squares;

    Reading(Claim claim, Evidence evidence) {
      this.evidence = evidence;
      this.text = claim.text();
      this.mentioned = movesIn(text);
      this.cited = new TreeSet<>();
      for (String source : claim.sources()) {
        cited.addAll(movesIn(source));
      }
      this.everyMove = new TreeSet<>(mentioned);
      everyMove.addAll(cited);
      this.named = evidence.players().stream().filter(player -> names(text, player)).toList();
      this.about = mentioned.isEmpty() && evidence.summary() ? cited : mentioned;
      this.boardMove = about.size() == 1 ? about.iterator().next() : evidence.currentMove();
      this.oneBoard = about.size() <= 1 && playedByNow(boardMove);
      this.lines = BoardVocabulary.lines(text);
      this.squares = BoardVocabulary.squares(text);
    }

    private boolean playedByNow(int move) {
      return move >= 1 && move <= evidence.currentMove();
    }

    /// The one player the claim names, when it names exactly one and one board can answer it.
    private Optional<String> soleNamedPlayerOnOneBoard() {
      return named.size() == 1 && oneBoard ? Optional.of(named.getFirst()) : Optional.empty();
    }

    private GameState board() {
      return evidence.history().get(boardMove);
    }
  }

  /// What the checks found: what the game contradicts, what nothing supports, and how many checks
  /// had something to say.
  ///
  /// The count matters as much as the lists. A claim with nothing checkable in it is neither right
  /// nor wrong, and saying so is the honest answer.
  private static final class Findings {

    private final List<String> contradicted = new ArrayList<>();
    private final List<String> unsupported = new ArrayList<>();
    private int checksRun;

    /// A check applied to this claim, whatever it then concluded.
    void ran() {
      checksRun++;
    }

    void contradicted(String why) {
      contradicted.add(why);
    }

    void unsupported(String why) {
      unsupported.add(why);
    }

    Verdict verdictOn(Claim claim) {
      if (!contradicted.isEmpty()) {
        return new Verdict(claim, Status.CONTRADICTED, String.join("; ", contradicted));
      }
      if (!unsupported.isEmpty()) {
        return new Verdict(claim, Status.UNSUPPORTED, String.join("; ", unsupported));
      }
      if (checksRun == 0) {
        return new Verdict(claim, Status.UNCHECKED, "nothing checkable against the game");
      }
      return new Verdict(claim, Status.SUPPORTED, "consistent with the game's history");
    }
  }

  /// A move that has not happened is wrong; one Alpha can no longer see is merely unsupported.
  private static void everyMoveWasPlayedAndIsStillInMemory(Reading reading, Findings findings) {
    for (int move : reading.everyMove) {
      findings.ran();
      if (!reading.playedByNow(move)) {
        findings.contradicted("move " + move + " has not been played");
      } else if (move < reading.evidence.earliestVisibleMove()) {
        findings.unsupported("move " + move + " is outside Alpha's memory window");
      }
    }
  }

  /// The move being commented on needs no citation; anything earlier does.
  private static void everyEarlierMoveItMentionsIsAlsoCited(Reading reading, Findings findings) {
    for (int move : reading.mentioned) {
      if (!reading.cited.contains(move) && move != reading.evidence.currentMove()) {
        findings.unsupported("mentions move " + move + " without citing it");
      }
    }
  }

  /// One move and one player: players move in turn, so the game knows whose it was.
  private static void theMoveBelongsToThePlayerItNames(Reading reading, Findings findings) {
    if (reading.about.size() != 1 || reading.named.size() != 1) {
      return;
    }
    int move = reading.about.iterator().next();
    if (!reading.playedByNow(move)) {
      return;
    }
    findings.ran();
    String whoseItWas = reading.evidence.playerOf(move);
    String whoItNames = reading.named.getFirst();
    if (!whoseItWas.equals(whoItNames)) {
      findings.contradicted("move %d was %s's, not %s's".formatted(move, whoseItWas, whoItNames));
    }
  }

  /// A win is on the board or it is not, and a win on a named line is on that line or it is not.
  private static void theWinIsOnTheBoardAndOnTheLineItNames(Reading reading, Findings findings) {
    if (!isWinClaim(reading.text)) {
      return;
    }
    var claimant = reading.soleNamedPlayerOnOneBoard();
    if (claimant.isEmpty()) {
      return;
    }
    findings.ran();
    String player = claimant.get();
    GameState board = reading.board();
    if (!board.lastPlayerHasChain() || !board.lastPlayer().equals(player)) {
      findings.contradicted(
          "says %s has won, but after move %d nobody has".formatted(player, reading.boardMove));
    } else if (reading.lines.size() == 1) {
      var line = reading.lines.getFirst();
      if (line.readings().stream().noneMatch(squares -> holdsAll(board, squares, player))) {
        findings.contradicted("%s won, but not with %s".formatted(player, line.phrase()));
      }
    }
  }

  /// A line that cannot exist is wrong whatever else the claim says, win or no win.
  private static void everyLineItNamesCouldExist(Reading reading, Findings findings) {
    for (String impossible : BoardVocabulary.impossibleLines(reading.text)) {
      findings.ran();
      findings.contradicted(impossible);
    }
  }

  /// Lenient on purpose: the claim holds if the player has a mark on any square the phrase can
  /// mean, after that move. "X holds the centre" passes whether X took it now or earlier.
  private static void theNamedPlayerHasAMarkOnTheSquare(Reading reading, Findings findings) {
    if (reading.squares.size() != 1 || !reading.lines.isEmpty()) {
      return;
    }
    var claimant = reading.soleNamedPlayerOnOneBoard();
    if (claimant.isEmpty()) {
      return;
    }
    findings.ran();
    String player = claimant.get();
    GameState board = reading.board();
    var square = reading.squares.getFirst();
    if (square.readings().getFirst().stream().noneMatch(at -> holds(board, at, player))) {
      findings.contradicted(
          "after move %d, %s has no mark on %s"
              .formatted(reading.boardMove, player, square.phrase()));
    }
  }

  /// Also flags an aside that makes a checkable claim, since asides are never checked.
  @Override
  public List<Verdict> checkAll(Commentary commentary, Evidence evidence) {
    var verdicts = new ArrayList<>(FactChecker.super.checkAll(commentary, evidence));
    String aside = commentary.aside();
    if (!movesIn(aside).isEmpty() || isWinClaim(aside)) {
      verdicts.add(
          new Verdict(
              new Claim(aside, List.of()),
              Status.UNSUPPORTED,
              "the aside makes a claim about the game, where nothing checks it"));
    }
    return List.copyOf(verdicts);
  }

  private static boolean holds(GameState state, int square, String player) {
    return player.equals(state.board().content()[square]);
  }

  private static boolean holdsAll(GameState state, Set<Integer> squares, String player) {
    return squares.stream().allMatch(i -> holds(state, i, player));
  }

  static boolean isWinClaim(String text) {
    return text != null && WIN.matcher(text).find() && !NOT_A_WIN_CLAIM.matcher(text).find();
  }

  static Set<Integer> movesIn(String text) {
    Set<Integer> moves = new TreeSet<>();
    if (text == null) {
      return moves;
    }
    var matchingMoves = MOVE.matcher(text);
    while (matchingMoves.find()) {
      moves.add(Integer.parseInt(matchingMoves.group("number")));
    }
    return moves;
  }

  private static boolean names(String text, String marker) {
    return Pattern.compile("\\b" + Pattern.quote(marker) + "\\b").matcher(text).find();
  }
}
