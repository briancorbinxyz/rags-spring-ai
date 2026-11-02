package xyz.briancorbin.rags.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/// Reading Ollama's own version out of its answer.
///
/// The body here is the one a local Ollama actually returned, copied rather than invented, because
/// a parse tested against a body of my own devising only proves the parse agrees with me.
class OllamaModelTest {

  @Test
  void readsTheVersionOllamaReports() {
    assertEquals(Optional.of("0.40.3"), OllamaModel.versionIn("{\"version\":\"0.40.3\"}"));
  }

  /// A body without the field, or with it empty, is no version. Reporting `""` would put an empty
  /// field in a run stamp, which reads as a version rather than as an absent one.
  @Test
  void aBodyWithNoVersionIsEmpty() {
    assertEquals(Optional.empty(), OllamaModel.versionIn("{}"));
    assertEquals(Optional.empty(), OllamaModel.versionIn("{\"version\":\"\"}"));
    assertEquals(Optional.empty(), OllamaModel.versionIn("{\"version\":\"  \"}"));
  }

  /// Ollama is not the only thing that can answer on its port. A proxy's error page is not a
  /// version, and must not become one.
  @Test
  void somethingElseAnsweringIsNotAVersion() {
    assertEquals(Optional.empty(), OllamaModel.versionIn("{\"error\":\"not found\"}"));
  }
}
