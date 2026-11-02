package xyz.briancorbin.rags.eval;

/// One graded answer, as structured output from the judge.
///
/// Four labels, because they are the four things that can happen to an answer and because a judge
/// with more labels agrees with a person less often. `DECLINED` is deliberately not a verdict about
/// whether declining was right: that depends on the question, which the scorer knows and the judge
/// does not need to.
///
/// @param label which of the four it was
/// @param reason one sentence naming what the answer did or did not state, for a person checking
public record Judgement(Label label, String reason) {

  public enum Label {
    /// States the core, in whatever words.
    CORRECT,
    /// States some of the core, or states it and contradicts the reference as well.
    PARTLY,
    /// Does not state the core, including answering a different question convincingly.
    WRONG,
    /// Refuses to answer. Right on a question with no answer, wrong on one with an answer.
    DECLINED
  }

  public Judgement {
    reason = reason == null ? "" : reason.strip();
  }

  /// Whether this is the outcome a question of this kind should have had.
  ///
  /// On a question with an answer, only `CORRECT` is right. On one without, `DECLINED` is right and
  /// so is `CORRECT`, because the golden set's core for those questions *is* a decline — "Declines:
  /// not covered." An answer that says the archive does not cover it has stated that core, whether
  /// it reads as a refusal or as a plain statement, and Episode 3's hand grades counted both:
  /// `declined in own words` is one of the two it got right.
  public boolean isRightFor(boolean answerable) {
    return answerable ? label == Label.CORRECT : label == Label.DECLINED || label == Label.CORRECT;
  }
}
