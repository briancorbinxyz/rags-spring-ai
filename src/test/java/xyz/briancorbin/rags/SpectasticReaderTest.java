package xyz.briancorbin.rags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;

class SpectasticReaderTest {

  private static final String SPEC = "specs/001-demo/spec.html";
  private static final String CHANGE =
      "specs/001-demo/changes/archive/2026-01-01-log-to-stdout/proposal.html";

  private static Map<String, Document> read(Path dir) throws Exception {
    return new SpectasticReader(Archives.tiny(dir))
        .get().stream().collect(Collectors.toMap(Units::address, Function.identity()));
  }

  @Test
  void everyElementWithAnIdIsAUnitAndShortOnesAreLeftOut(@TempDir Path dir) throws Exception {
    var units = new SpectasticReader(Archives.tiny(dir)).get();
    assertEquals(
        List.of(
            CHANGE + "#FR-003",
            SPEC + "#requirements",
            SPEC + "#FR-001",
            SPEC + "#FR-002",
            SPEC + "#FR-003",
            SPEC + "#D-001",
            SPEC + "#D-002",
            // a section that holds only its heading is a label: "Decisions", "Requirements"
            "specs/002-other/spec.html#FR-001"),
        units.stream().map(Units::address).toList());
  }

  @Test
  void aUnitHoldsOnlyItsOwnTextWithItsHeadingPathInFront(@TempDir Path dir) throws Exception {
    var units = read(dir);
    assertEquals(
        "Requirements The requirements for the demo service follow.",
        units.get(SPEC + "#requirements").getText());
    Document fr1 = units.get(SPEC + "#FR-001");
    assertEquals("001 Demo › Requirements", fr1.getMetadata().get(Units.HEADING_PATH));
    assertEquals(
        "001 Demo › Requirements\nThe service MUST record every request.", Units.passage(fr1));
    assertEquals("001-demo", fr1.getMetadata().get(Units.SCOPE));
  }

  @Test
  void theLifecycleComesFromTheFolderAndTheStatus(@TempDir Path dir) throws Exception {
    var units = read(dir);
    assertEquals(Lifecycle.CURRENT, Units.standing(units.get(SPEC + "#FR-001")));
    assertEquals(Lifecycle.SUPERSEDED, Units.standing(units.get(SPEC + "#FR-002")));
    assertEquals(Lifecycle.HISTORY, Units.standing(units.get(CHANGE + "#FR-003")));
    // only a status element inside it says so: the filter can't see it
    assertEquals(Lifecycle.CURRENT, Units.standing(units.get(SPEC + "#D-001")));
  }

  @Test
  void anOutOfDateUnitNamesItsSuccessor(@TempDir Path dir) throws Exception {
    var units = read(dir);
    // "Superseded by" a link
    assertEquals(
        SPEC + "#FR-003", units.get(SPEC + "#FR-002").getMetadata().get(Units.SUPERSEDED_BY));
    // marked superseded in place, with a link after the mark
    assertEquals(
        SPEC + "#D-002", units.get(SPEC + "#D-001").getMetadata().get(Units.SUPERSEDED_BY));
    // an applied change's spec-delta points at the requirement it changed
    assertEquals(
        SPEC + "#FR-003", units.get(CHANGE + "#FR-003").getMetadata().get(Units.SUPERSEDED_BY));
    assertFalse(units.get(SPEC + "#FR-003").getMetadata().containsKey(Units.SUPERSEDED_BY));
  }

  @Test
  void aLongUnitIsSplitAtSentenceEndsIntoEvenParts() {
    String text = "One two three four five six seven eight nine. ".repeat(60).strip(); // 540 words
    List<String> parts = SpectasticReader.parts(text);
    assertEquals(3, parts.size());
    for (String part : parts) {
      int words = part.split(" ", -1).length;
      assertTrue(
          words <= SpectasticReader.PART_WORDS && words >= 150,
          part.length() + " chars, " + words + " words");
      assertTrue(part.endsWith("."));
    }
    assertEquals(text, String.join(" ", parts));
  }

  @Test
  void aShortUnitIsOnePart() {
    assertEquals(List.of("Just one sentence."), SpectasticReader.parts("Just one sentence."));
  }
}
