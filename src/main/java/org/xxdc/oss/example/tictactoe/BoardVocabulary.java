package org.xxdc.oss.example.tictactoe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// The words commentators use for squares and lines on a 3x3 board, and the squares they mean.
///
/// Squares are numbered as the game numbers them: 0 to 8, left to right, top to bottom. Lines are
/// matched before squares, and each match is blanked out before the next pattern runs, so "the
/// top-left to bottom-right diagonal" is one line, not two corners, and "the top-left corner" is
/// one square, not also "a corner".
final class BoardVocabulary {

  /// A phrase and what it can mean. A line has one or more readings ("a diagonal" has two); a
  /// square phrase has one reading, which may cover several squares ("a corner" covers four).
  record Named(String phrase, List<Set<Integer>> readings) {}

  private record Term(Pattern pattern, String name, List<Set<Integer>> readings) {}

  private static final String TL = "top[- ]left";
  private static final String TR = "top[- ]right";
  private static final String BL = "bottom[- ]left";
  private static final String BR = "bottom[- ]right";
  private static final String TO = "(?: corner)? (?:to|through|into) (?:the )?";

  private static final List<Term> LINES =
      List.of(
          line(
              "(?:"
                  + TL
                  + TO
                  + BR
                  + "|"
                  + BR
                  + TO
                  + TL
                  + ")(?: diagonal)?"
                  + "|main diagonal|leading diagonal",
              "the top-left to bottom-right diagonal",
              Set.of(0, 4, 8)),
          line(
              "(?:" + TR + TO + BL + "|" + BL + TO + TR + ")(?: diagonal)?" + "|anti[- ]?diagonal",
              "the top-right to bottom-left diagonal",
              Set.of(2, 4, 6)),
          new Term(
              Pattern.compile("(?i)\\bdiagonal\\b"),
              "a diagonal",
              List.of(Set.of(0, 4, 8), Set.of(2, 4, 6))),
          line("top row", "the top row", Set.of(0, 1, 2)),
          line("(?:middle|centre|center) row", "the middle row", Set.of(3, 4, 5)),
          line("bottom row", "the bottom row", Set.of(6, 7, 8)),
          line("left(?:-hand)? column", "the left column", Set.of(0, 3, 6)),
          line("(?:middle|centre|center) column", "the middle column", Set.of(1, 4, 7)),
          line("right(?:-hand)? column", "the right column", Set.of(2, 5, 8)));

  /// "middle", "centre" or "center", as in "bottom-middle", "bottom centre", "centre-left".
  private static final String MID = "(?:middle|centre|center)";

  /// The bare centre, but not the "centre" inside "bottom-centre" or "centre-left".
  private static final String BARE_CENTRE =
      "(?<!top[- ])(?<!bottom[- ])(?<!left[- ])(?<!right[- ])"
          + "(?:centre|center)(?![- ](?:top|bottom|left|right)\\b)";

  // Order matters: each match is blanked before the next pattern runs, so the four corners and
  // the four edges ("bottom-centre") are taken out before the bare centre and the generic terms.
  private static final List<Term> SQUARES =
      List.of(
          square(TL + "(?: corner| square)?", "the top-left corner", Set.of(0)),
          square(TR + "(?: corner| square)?", "the top-right corner", Set.of(2)),
          square(BL + "(?: corner| square)?", "the bottom-left corner", Set.of(6)),
          square(BR + "(?: corner| square)?", "the bottom-right corner", Set.of(8)),
          square("top(?:[- ]" + MID + "| edge)|" + MID + "[- ]top", "the top edge", Set.of(1)),
          square(
              "bottom(?:[- ]" + MID + "| edge)|" + MID + "[- ]bottom",
              "the bottom edge",
              Set.of(7)),
          square("left(?:[- ]" + MID + "| edge)|" + MID + "[- ]left", "the left edge", Set.of(3)),
          square(
              "right(?:[- ]" + MID + "| edge)|" + MID + "[- ]right", "the right edge", Set.of(5)),
          square(
              "(?:centre|center|middle) square|(?:the )?" + BARE_CENTRE, "the centre", Set.of(4)),
          square("corner", "a corner", Set.of(0, 2, 6, 8)),
          square("edge|side", "an edge", Set.of(1, 3, 5, 7)));

  /// "from the top-right to the bottom-right": two corners joined, which name a row or a column
  /// when they share one, and a diagonal only when they are opposite.
  private static final Pattern CORNER_SPAN =
      Pattern.compile(
          "(?i)\\b(?<fromVertical>top|bottom)[- ](?<fromHorizontal>left|right)(?: corner)?"
              + " (?:to|through|into|and) (?:the )?"
              + "(?<toVertical>top|bottom)[- ](?<toHorizontal>left|right)\\b(?: corner)?");

  /// A diagonal said to run along a row or a column, as in "a diagonal across the bottom row".
  private static final Pattern DIAGONAL_ALONG_A_LINE =
      Pattern.compile(
          "(?i)\\bdiagonal(?: line)? (?:across|along|down|on|in|through) (?:the )?"
              + "(?<line>(?:top|middle|centre|center|bottom) row"
              + "|(?:left|middle|centre|center|right) column)\\b");

  private static final Pattern ROW_COLUMN =
      Pattern.compile("(?i)\\brow (?<row>\\d),? column (?<column>\\d)\\b");

  /// Whether the text uses the word at all, which decides if a corner span is a claimed diagonal.
  private static final Pattern SAYS_DIAGONAL = Pattern.compile("(?i)\\bdiagonal\\b");

  /// The lines a text names, in order.
  static List<Named> lines(String text) {
    return findLines(new StringBuilder(text == null ? "" : text));
  }

  /// Lines a text names that cannot exist on the board: a "diagonal" between two corners that share
  /// a row or a column, or a diagonal said to run along a row or a column. Each entry is the
  /// reason, in words.
  static List<String> impossibleLines(String text) {
    var reasons = new ArrayList<String>();
    if (text == null) {
      return reasons;
    }
    boolean saysDiagonal = SAYS_DIAGONAL.matcher(text).find();
    var matchingSpans = CORNER_SPAN.matcher(text);
    while (matchingSpans.find()) {
      int from = fromCorner(matchingSpans);
      int to = toCorner(matchingSpans);
      var shared = sharedLine(from, to);
      if (saysDiagonal && shared != null) {
        reasons.add(
            "calls %s to %s a diagonal, but those corners are %s"
                .formatted(cornerName(from), cornerName(to), shared.name()));
      }
    }
    var matchingDiagonals = DIAGONAL_ALONG_A_LINE.matcher(text);
    while (matchingDiagonals.find()) {
      reasons.add(
          "a diagonal cannot run along the "
              + matchingDiagonals.group("line").toLowerCase(Locale.ROOT));
    }
    return reasons;
  }

  /// The corner a span starts at, which its group names spell out.
  private static int fromCorner(Matcher span) {
    return corner(span.group("fromVertical"), span.group("fromHorizontal"));
  }

  /// The corner a span ends at.
  private static int toCorner(Matcher span) {
    return corner(span.group("toVertical"), span.group("toHorizontal"));
  }

  /// The squares a text names, after the lines have been taken out.
  static List<Named> squares(String text) {
    var working = new StringBuilder(text == null ? "" : text);
    findLines(working);
    var found = new ArrayList<Named>();
    var matchingRowColumns = ROW_COLUMN.matcher(working);
    while (matchingRowColumns.find()) {
      int row = Integer.parseInt(matchingRowColumns.group("row"));
      int column = Integer.parseInt(matchingRowColumns.group("column"));
      if (row >= 1 && row <= 3 && column >= 1 && column <= 3) {
        found.add(
            new Named(matchingRowColumns.group(), List.of(Set.of((row - 1) * 3 + (column - 1)))));
      }
      blank(working, matchingRowColumns.start(), matchingRowColumns.end());
    }
    found.addAll(find(working, SQUARES));
    return found;
  }

  /// "row 1, column 2" for a square index, the way the move reports say it.
  static String describe(int square) {
    return "row %d, column %d".formatted(square / 3 + 1, square % 3 + 1);
  }

  /// Named diagonals first, then corner-to-corner spans that name a row or a column, then the
  /// remaining lines, so "from top-right to bottom-right" is the right column, not two corners.
  private static List<Named> findLines(StringBuilder working) {
    var found = new ArrayList<>(find(working, LINES.subList(0, 2)));
    var matchingSpans = CORNER_SPAN.matcher(working);
    while (matchingSpans.find()) {
      var shared = sharedLine(fromCorner(matchingSpans), toCorner(matchingSpans));
      if (shared != null) {
        found.add(new Named(shared.name(), List.of(shared.squares())));
        blank(working, matchingSpans.start(), matchingSpans.end());
      }
    }

    found.addAll(find(working, LINES.subList(2, LINES.size())));
    return found;
  }

  private record Line(String name, Set<Integer> squares) {}

  /// The row or column two different squares share, or null for a diagonal pair or no line.
  private static Line sharedLine(int from, int to) {
    if (from == to) {
      return null;
    }
    if (from / 3 == to / 3) {
      int row = from / 3;
      return new Line(
          "the " + List.of("top", "middle", "bottom").get(row) + " row",
          Set.of(row * 3, row * 3 + 1, row * 3 + 2));
    }
    if (from % 3 == to % 3) {
      int column = from % 3;
      return new Line(
          "the " + List.of("left", "middle", "right").get(column) + " column",
          Set.of(column, column + 3, column + 6));
    }
    return null;
  }

  private static int corner(String vertical, String horizontal) {
    return (vertical.equalsIgnoreCase("top") ? 0 : 6)
        + (horizontal.equalsIgnoreCase("left") ? 0 : 2);
  }

  private static String cornerName(int square) {
    return switch (square) {
      case 0 -> "top-left";
      case 2 -> "top-right";
      case 6 -> "bottom-left";
      default -> "bottom-right";
    };
  }

  private static List<Named> find(StringBuilder working, List<Term> terms) {
    var found = new ArrayList<Named>();
    for (Term term : terms) {
      var matchingTerms = term.pattern().matcher(working);
      while (matchingTerms.find()) {
        found.add(new Named(term.name(), term.readings()));
        blank(working, matchingTerms.start(), matchingTerms.end());
      }
    }
    return found;
  }

  private static void blank(StringBuilder working, int start, int end) {
    for (int at = start; at < end; at++) {
      working.setCharAt(at, ' ');
    }
  }

  private static Term line(String regex, String name, Set<Integer> squares) {
    return new Term(Pattern.compile("(?i)\\b(?:" + regex + ")\\b"), name, List.of(squares));
  }

  private static Term square(String regex, String name, Set<Integer> squares) {
    return new Term(Pattern.compile("(?i)\\b(?:" + regex + ")\\b"), name, List.of(squares));
  }

  private BoardVocabulary() {}
}
