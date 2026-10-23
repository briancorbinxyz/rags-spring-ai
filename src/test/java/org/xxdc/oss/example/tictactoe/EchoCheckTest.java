package org.xxdc.oss.example.tictactoe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EchoCheckTest {

  private static final String REPORT =
      """
      Move 4: O played row 1, column 2.
      Turning point: O blocks an immediate loss.
      Board:
      X O .
      . X .
      . . .
      """;

  @Test
  void readingTheReportBackIsAnEcho() {
    assertTrue(EchoCheck.echoes("O played row 1, column 2.", REPORT));
    assertTrue(EchoCheck.echoes("O blocks an immediate loss.", REPORT));
  }

  @Test
  void sayingWhatTheMoveMeansIsNotAnEcho() {
    assertFalse(
        EchoCheck.echoes(
            "O shuts the door on the diagonal X has been building since move 1.", REPORT));
  }

  @Test
  void veryShortClaimsNeverCount() {
    assertFalse(EchoCheck.echoes("O blocks.", REPORT));
  }

  @Test
  void countsEchoesInAReply() {
    var commentary =
        new Commentary(
            "",
            List.of(
                new Claim("O played row 1, column 2.", List.of()),
                new Claim("A late answer to X's opening gambit on move 1.", List.of("move 1"))));
    assertEquals(1, EchoCheck.count(commentary, REPORT));
  }
}
