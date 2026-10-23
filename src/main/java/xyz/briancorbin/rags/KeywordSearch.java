package xyz.briancorbin.rags;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;

/**
 * Keyword search over the element units: BM25, as SQLite's full-text search (FTS5) ships it.
 *
 * <p>One row per unit, in three weighted columns: the heading path and the element's own heading
 * (×5), the element's id (×10) and its text (×1). FTS5's default tokenizer, {@code unicode61},
 * splits on hyphens, so {@code FR-012} is the two tokens {@code fr 012}, and quoting it as a phrase
 * keeps them together. The question becomes every word that isn't a stop word, each quoted, OR-ed:
 * a unit needs only one of them to be a candidate, and BM25 ranks the candidates.
 *
 * <p>The lifecycle is a fourth column, unindexed, so a {@link Lifecycle} filter runs inside the
 * query, before the results are cut to a depth, as a vector store's metadata filter does.
 */
public final class KeywordSearch implements AutoCloseable {

  /** The heading path, the id, the text. */
  static final double[] WEIGHTS = {5.0, 10.0, 1.0};

  /** One space: declared above STOP_WORDS, which splits on it while the class initialises. */
  private static final Pattern SPACE = Pattern.compile(" ", Pattern.LITERAL);

  /** English a question can spare. */
  private static final String COMMON =
      "a an the of in on for to and or is are was were be by with what which how does do did "
          + "that this it its as at from when who whom why where after before into than then "
          + "there their they them should must can could would will say says about";

  /**
   * Words this archive says so often they carry nothing: every question is about a spec. The one
   * piece of the corpus's vocabulary still sitting in the search — a second archive would hand its
   * own in, and this would be a parameter.
   */
  private static final String OF_THE_ARCHIVE = "spec spectastic";

  private static final Set<String> STOP_WORDS =
      Set.of(SPACE.split(COMMON + " " + OF_THE_ARCHIVE, -1));

  private static final Pattern WORD = Pattern.compile("[A-Za-z0-9][A-Za-z0-9\\-]*");

  private final ElementUnits units;
  private final Connection db;

  /** A keyword index over these units. Built once; it owns a SQLite connection, so close it. */
  public static KeywordSearch of(ElementUnits units) {
    return new KeywordSearch(units);
  }

  private KeywordSearch(ElementUnits units) {
    this.units = units;
    try {
      db = DriverManager.getConnection("jdbc:sqlite::memory:");
      create();
      fill();
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot build the keyword index", e);
    }
  }

  /**
   * The table. {@code lifecycle} is stored but not indexed: Step 3 filters on it, never matches.
   */
  private void create() throws SQLException {
    try (var create = db.createStatement()) {
      create.execute(
          "create virtual table units using fts5"
              + "(heading_path, anchor, body, lifecycle unindexed)");
    }
  }

  /** Every unit, in archive order, so a rowid is the unit's position. */
  private void fill() throws SQLException {
    db.setAutoCommit(false);
    try (var insert =
        db.prepareStatement(
            "insert into units(rowid, heading_path, anchor, body, lifecycle)"
                + " values (?, ?, ?, ?, ?)")) {
      var all = units.all();
      for (int row = 0; row < all.size(); row++) {
        bind(insert, row, all.get(row));
        insert.addBatch();
      }
      insert.executeBatch();
    }
    db.commit();
  }

  /** One unit's columns: what a reader sees in front of the text, the id, the text, the state. */
  private static void bind(PreparedStatement insert, int row, Document unit) throws SQLException {
    var marks = unit.getMetadata();
    String path = String.valueOf(marks.get(Units.HEADING_PATH));
    insert.setInt(1, row);
    insert.setString(
        2, join(path.isEmpty() ? marks.get(Units.TITLE) : path, marks.get(Units.HEADING)));
    insert.setString(3, String.valueOf(marks.get(Units.ANCHOR)));
    insert.setString(4, unit.getText());
    insert.setString(5, String.valueOf(marks.get(Units.STANDING)));
  }

  /** The units this searches. */
  public ElementUnits units() {
    return units;
  }

  /**
   * The {@code depth} best units for a question, best first, each with its BM25 score (higher is
   * better). Only units whose lifecycle is in {@code filter} come back; the filter runs before the
   * cut, so a filtered search still returns {@code depth} units when it can.
   */
  public List<Document> search(String question, int depth, Set<? extends Standing> filter) {
    String where = where(filter);
    String sql =
        "select rowid, bm25(units, %s) as score from units where units match ?%s order by score, rowid limit ?"
            .formatted(weights(), where.isEmpty() ? "" : " and " + where);
    return query(sql, question, depth, null);
  }

  /**
   * Ranks the given units, and only them, for a question; units with no matching word are left out.
   */
  public List<Document> rank(String question, Collection<Document> scope) {
    String ids =
        scope.stream()
            .map(unit -> String.valueOf(units.row(unit)))
            .collect(Collectors.joining(",", "[", "]"));
    String sql =
        "select rowid, bm25(units, %s) as score from units where units match ? and rowid in (select value from json_each(?)) order by score, rowid limit ?"
            .formatted(weights());
    return query(sql, question, scope.size(), ids);
  }

  /**
   * A standing filter as SQL: {@code lifecycle in ('current', 'history')}, nothing at all when
   * every standing is allowed, and a condition that matches nothing when none is.
   *
   * <p>This lives here rather than on {@link Standing} because it is SQL — the shape of the filter
   * is the archive's, and expressing it in FTS5 is this adapter's.
   */
  private String where(Set<? extends Standing> filter) {
    if (filter.containsAll(Lifecycle.ALL)) {
      return "";
    }
    if (filter.isEmpty()) {
      return "1 = 0"; // asked for no standing at all: match nothing, rather than write "in ()"
    }
    return filter.stream()
        .map(standing -> "'" + standing.label() + "'")
        .collect(Collectors.joining(", ", "lifecycle in (", ")"));
  }

  /** The question as an FTS5 query: each word that isn't a stop word, quoted, OR-ed. */
  static String matchQuery(String question) {
    var words = new ArrayList<String>();
    var matchingWords = WORD.matcher(question);
    while (matchingWords.find()) {
      String word = matchingWords.group();
      if (!STOP_WORDS.contains(word.toLowerCase(Locale.ROOT))) {
        words.add('"' + word.replace("\"", "\"\"") + '"');
      }
    }
    return String.join(" OR ", words);
  }

  private List<Document> query(String sql, String question, int depth, String scope) {
    String match = matchQuery(question);
    if (match.isEmpty() || depth <= 0) {
      return List.of();
    }
    try (PreparedStatement select = db.prepareStatement(sql)) {
      int i = 1;
      select.setString(i++, match);
      if (scope != null) {
        select.setString(i++, scope);
      }
      select.setInt(i, depth);
      var out = new ArrayList<Document>();
      try (var rs = select.executeQuery()) {
        while (rs.next()) {
          out.add(ElementUnits.scored(units.all().get(rs.getInt(1)), -rs.getDouble(2)));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new IllegalStateException("Keyword search failed for: " + question, e);
    }
  }

  private static String weights() {
    // Double.toString, not a format: a locale with a decimal comma would break the SQL.
    return Double.toString(WEIGHTS[0]) + ", " + WEIGHTS[1] + ", " + WEIGHTS[2];
  }

  private static String join(Object first, Object second) {
    String before = first == null ? "" : first.toString();
    String after = second == null ? "" : second.toString();
    return before.isEmpty() ? after : after.isEmpty() ? before : before + " " + after;
  }

  @Override
  public void close() {
    try {
      db.close();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }
}
