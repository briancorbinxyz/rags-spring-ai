package org.xxdc.oss.example.tictactoe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/// The measurement file's columns are published: the article's numbers are read from them.
class MeasurementFileTest {

  /// The header as Episode 2's run files carry it. The column table is the one definition of these
  /// names, so this is what stops a tidy-up from renaming a column and quietly invalidating every
  /// run file already written.
  private static final String PUBLISHED_HEADER =
      "move,from_model,prompt_tokens,claims,over_limit,echoes,claim_repeats,checked_share,"
          + "supported,contradicted,unsupported,unchecked,problems,aside,aside_repeat,answer,"
          + "beta_answer,beta_quote_verified,beta_stray_sources,beta_echoes,beta_aside_repeat";

  private final AppConfiguration.MeasurementFile file = new AppConfiguration.MeasurementFile();

  @Test
  void theColumnsSpellTheHeaderTheArticlePublished() {
    assertEquals(PUBLISHED_HEADER, file.header());
  }
}
