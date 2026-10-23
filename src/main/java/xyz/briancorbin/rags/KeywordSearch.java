package xyz.briancorbin.rags;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

  private static final Set<String> STOP_WORDS =
      Set.of(
          ("a an the of in on for to and or is are was were be by with what which how does do did that this "
                  + "it its as at from when who whom why where after before into than then there their they them "
                  + "should must can could would will spec spectastic say says about")
              .split(" "));

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
      try (var create = db.createStatement()) {
        create.execute(
            "create virtual table units using fts5(heading_path, anchor, body, lifecycle unindexed)");
      }
      db.setAutoCommit(false);
      try (var insert =
          db.prepareStatement(
              "insert into units(rowid, heading_path, anchor, body, lifecycle) values (?, ?, ?, ?, ?)")) {
        var all = units.all();
        for (int row = 0; row < all.size(); row++) {
          var unit = all.get(row);
          var marks = unit.getMetadata();
          String path = String.valueOf(marks.get(ElementReader.HEADING_PATH));
          insert.setInt(1, row);
          insert.setString(
              2, join(path.isEmpty() ? marks.get(ElementReader.TITLE) : path, marks.get(ElementReader.HEADING)));
          insert.setString(3, String.valueOf(marks.get(ElementReader.ANCHOR)));
          insert.setString(4, unit.getText());
          insert.setString(5, String.valueOf(marks.get(ElementReader.LIFECYCLE)));
          insert.addBatch();
        }
        insert.executeBatch();
      }
      db.commit();
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot build the keyword index", e);
    }
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
  public List<Document> search(String question, int depth, Set<Lifecycle> filter) {
    String where = Lifecycle.sql(filter);
    String sql =
        "select rowid, bm25(units, %s) as score from units where units match ?%s order by score, rowid limit ?"
            .formatted(weights(), where.isEmpty() ? "" : " and " + where);
    return query(sql, question, depth, null);
  }

  /** Ranks the given units, and only them, for a question; units with no matching word are left out. */
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
