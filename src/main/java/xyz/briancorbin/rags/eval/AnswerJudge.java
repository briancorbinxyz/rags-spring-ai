package xyz.briancorbin.rags.eval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import xyz.briancorbin.rags.eval.GoldenSet.Question;

/// Grades an answer against the golden set's reference answer, so an episode can report whether
/// better retrieval became better answers.
///
/// A model grading a model is weak evidence on its own, which is why {@link JudgeCalibration}
/// checks it against the answers a person graded by hand in Episode 3 before any number from it is
/// published.
///
/// The judge sees the reference answer and the core, and never the retrieved passages. It is
/// grading the answer, not the search, and giving it the passages would let a wrong answer look
/// right for having quoted the right place.
public final class AnswerJudge {

  private static final Logger log = LoggerFactory.getLogger(AnswerJudge.class);

  private final ChatClient judge;

  public AnswerJudge(ChatClient judge) {
    this.judge = judge;
  }

  /// The grade for one answer. An empty answer is a refusal and needs no model call.
  public Judgement on(Question question, String answer) {
    if (answer == null || answer.isBlank()) {
      return new Judgement(Judgement.Label.DECLINED, "no answer given");
    }
    try {
      Judgement judgement =
          judge.prompt().user(asked(question, answer)).call().entity(Judgement.class);
      return judgement == null ? unreachable("the judge returned nothing") : judgement;
    } catch (RuntimeException e) {
      return unreachable(e.getMessage());
    }
  }

  private static Judgement unreachable(String why) {
    log.warn("No grade: {}", why);
    return new Judgement(Judgement.Label.PARTLY, "ungraded: " + why);
  }

  /// Everything the judge is allowed to know, in a fixed order so the prompt hash is stable.
  private static String asked(Question question, String answer) {
    return """
        Question: %s

        Reference answer: %s

        Core (the least a correct answer must contain): %s

        Answer to grade: %s"""
        .formatted(question.question(), question.answer(), question.core(), answer.strip());
  }
}
