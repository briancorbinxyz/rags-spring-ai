package xyz.briancorbin.rags;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;

/**
 * Words a question can spare before it reaches the keyword index.
 *
 * <p>A list, not a constant, and more than one of them: a language contributes words that carry
 * nothing in any archive, and an archive contributes words it says so often that they carry nothing
 * in it. They are unioned, so neither list has to know about the other, and adding an archive is a
 * file rather than an edit.
 *
 * <p>Each file is one word per line; blank lines and lines starting with {@code #} are comments, so
 * a list can say why a word is on it.
 */
public final class StopWords {

  private final Set<String> words;

  private StopWords(Set<String> words) {
    this.words = words;
  }

  /** The union of the named classpath resources, in the order given. */
  public static StopWords of(String... resources) {
    var words = new LinkedHashSet<String>();
    for (String resource : resources) {
      words.addAll(read(resource));
    }
    return new StopWords(Set.copyOf(words));
  }

  /** Nothing is a stop word: every word in the question reaches the index. */
  public static StopWords none() {
    return new StopWords(Set.of());
  }

  public boolean contains(String word) {
    return words.contains(word.toLowerCase(Locale.ROOT));
  }

  public int size() {
    return words.size();
  }

  private static Set<String> read(String resource) {
    var words = new LinkedHashSet<String>();
    try (var lines =
        new BufferedReader(
            new InputStreamReader(
                new ClassPathResource(resource).getInputStream(), StandardCharsets.UTF_8))) {
      for (String line = lines.readLine(); line != null; line = lines.readLine()) {
        String trimmed = line.strip();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        for (String word : trimmed.toLowerCase(Locale.ROOT).split(" ", -1)) {
          if (!word.isEmpty()) {
            words.add(word);
          }
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the stop words at " + resource, e);
    }
    return words;
  }
}
