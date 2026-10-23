package xyz.briancorbin.rags;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HexFormat;

/**
 * Vectors on disk, so a corpus is embedded once per model.
 *
 * <p>Keyed by the SHA-256 of the model id and the exact text that was sent, prefix included: a new
 * model, a new prefix or an edited unit is a new key, and so a new vector, with no way to serve a
 * stale one by mistake. The model id carries the model's digest, not just its name, so re-pulling a
 * tag that changed underneath does not quietly reuse the old vectors.
 *
 * <p>The file is a SQLite table of {@code (hash, dimensions, little-endian float32 blob)} — the
 * same table, the same keys and the same bytes as the prototype's {@code ratchet/embed.py}, so both
 * implementations can share one cache file. That is what makes the parity check mean anything: when
 * the Java and the prototype read the same vectors, a difference in their results is a difference in
 * the search, not in the arithmetic.
 */
public final class VectorCache implements AutoCloseable {

  private final Connection db;
  private final PreparedStatement select;
  private final PreparedStatement insert;

  public VectorCache(Path file) {
    try {
      if (file.getParent() != null) {
        Files.createDirectories(file.getParent());
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot make room for the vector cache " + file, e);
    }
    try {
      db = DriverManager.getConnection("jdbc:sqlite:" + file);
      try (var create = db.createStatement()) {
        create.execute("create table if not exists v (h text primary key, dim int, vec blob)");
      }
      select = db.prepareStatement("select dim, vec from v where h = ?");
      insert = db.prepareStatement("insert or replace into v values (?, ?, ?)");
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot open the vector cache " + file, e);
    }
  }

  /** The cached vector for this text under this model, or null. */
  public float[] get(String modelId, String text) {
    try {
      select.setString(1, key(modelId, text));
      try (var rs = select.executeQuery()) {
        return rs.next() ? floats(rs.getBytes(2), rs.getInt(1)) : null;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot read the vector cache", e);
    }
  }

  /** Remembers one vector. Call {@link #commit()} when a batch is done. */
  public void put(String modelId, String text, float[] vector) {
    try {
      insert.setString(1, key(modelId, text));
      insert.setInt(2, vector.length);
      insert.setBytes(3, bytes(vector));
      insert.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot write to the vector cache", e);
    }
  }

  public void commit() {
    try {
      if (!db.getAutoCommit()) {
        db.commit();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot commit the vector cache", e);
    }
  }

  /** The cache key: {@code sha256(modelId + NUL + text)}, as the prototype computes it. */
  static String key(String modelId, String text) {
    try {
      var sha = MessageDigest.getInstance("SHA-256");
      return HexFormat.of()
          .formatHex(sha.digest((modelId + "\0" + text).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static float[] floats(byte[] blob, int dim) {
    var out = new float[dim];
    ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(out);
    return out;
  }

  private static byte[] bytes(float[] vector) {
    var buffer = ByteBuffer.allocate(vector.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
    buffer.asFloatBuffer().put(vector);
    return buffer.array();
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
