package xyz.briancorbin.rags.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StopWordsTest {

  @Test
  void aLanguageListAndAnArchiveListUnion() {
    var english = StopWords.of("stopwords/english.txt");
    var both = StopWords.of("stopwords/english.txt", "stopwords/spectastic.txt");
    assertTrue(english.contains("the"));
    assertFalse(english.contains("spectastic"), "no archive's words are in the language list");
    assertTrue(both.contains("the"));
    assertTrue(both.contains("spectastic"));
    assertEquals(english.size() + 2, both.size(), "the archive adds exactly its two");
  }

  @Test
  void commentsAndBlankLinesAreNotWords() {
    var words = StopWords.of("stopwords/spectastic.txt");
    assertEquals(2, words.size());
    assertFalse(words.contains("#"));
  }

  @Test
  void matchingIgnoresCase() {
    var words = StopWords.of("stopwords/english.txt");
    assertTrue(words.contains("The"));
    assertTrue(words.contains("WHICH"));
  }

  @Test
  void noneLetsEveryWordThrough() {
    assertFalse(StopWords.none().contains("the"));
    assertEquals(0, StopWords.none().size());
  }
}
