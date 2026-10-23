package xyz.briancorbin.rags.answer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import xyz.briancorbin.rags.units.Address;

class ArchiveTest {

  @Test
  void theAnswererSeesEachQuotationWithItsLabelAndAddress() {
    var quotes =
        List.of(
            new Quote(
                "Q1",
                "Retrieval MUST NOT be built",
                new Address("specs/059/spec.html", 2, 3, "T"),
                1));
    assertEquals(
        String.join(
            System.lineSeparator(),
            "Question: Why no index?",
            "",
            "Quotations Beta traced to the archive. Cite them by label.",
            "Q1 (specs/059/spec.html, chunk 2 of 3)",
            "\"Retrieval MUST NOT be built\""),
        Archive.answererPrompt("Why no index?", quotes));
  }

  @Test
  void withNothingTracedTheAnswererIsToldSo() {
    assertEquals(
        String.join(
            System.lineSeparator(),
            "Question: Why no index?",
            "",
            "Quotations Beta traced to the archive: none."),
        Archive.answererPrompt("Why no index?", List.of()));
  }
}
