package xyz.briancorbin.rags.units;

import org.springframework.ai.document.Document;

/**
 * Where a passage came from: the file in the archive, which chunk of it, and the page title.
 *
 * <p>This is provenance, and it lives in a chunk's metadata, not its text. Whatever read the
 * archive records the title and the file path, and the splitter adds the chunk number. The model
 * never sees any of it, so nothing here can be mistyped by a model.
 *
 * @param source the file, relative to the checkout it was read from
 * @param chunk which chunk of the file, counting from 1
 * @param chunks how many chunks the file was split into
 * @param title the page's {@code <title>}
 */
public record Address(String source, int chunk, int chunks, String title) {

  /** Metadata key for the file a chunk came from. */
  public static final String SOURCE = "source";

  /** Metadata key for the part of the archive a chunk belongs to. */
  public static final String SPEC = "spec";

  /** Metadata key for the commit the archive was read at, so a run names what it measured. */
  public static final String COMMIT = "commit";

  /** Reads the address back out of a stored chunk's metadata. */
  public static Address of(Document chunk) {
    var marks = chunk.getMetadata();
    return new Address(
        String.valueOf(marks.getOrDefault(SOURCE, "unknown")),
        number(marks.get("chunk_index"), 0) + 1, // the splitter counts from 0
        number(marks.get("total_chunks"), 1),
        String.valueOf(marks.getOrDefault("title", "")));
  }

  /**
   * How the address is shown to a reader, and to the model that must cite it: {@code <source>,
   * chunk 2 of 7}.
   */
  @Override
  public String toString() {
    return "%s, chunk %d of %d".formatted(source, chunk, chunks);
  }

  /**
   * Numbers come back from the store's JSON file as whatever type Jackson chose, so accept any
   * {@link Number} and fall back when the key is missing.
   */
  private static int number(Object value, int fallback) {
    return value instanceof Number n ? n.intValue() : fallback;
  }
}
