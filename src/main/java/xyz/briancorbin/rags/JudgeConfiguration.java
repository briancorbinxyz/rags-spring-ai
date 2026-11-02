package xyz.briancorbin.rags;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import xyz.briancorbin.rags.eval.AnswerJudge;
import xyz.briancorbin.rags.eval.SupportAgreement;

/// The judge, which grades an answer against the golden set's reference answer.
///
/// It is not a commentator and it is configured like an instrument rather than a character: no
/// memory, no retriever, and temperature 0, because a measurement that changes between runs is not
/// one. Everything else is pinned the same way the commentators are, for the reasons {@link
/// ArchiveConfiguration} gives.
@Configuration
class JudgeConfiguration {

  @Bean
  ChatClient judgeChatClient(
      ChatClient.Builder builder, @Value("classpath:persona/judge.md") Resource persona) {
    return builder.defaultSystem(persona).defaultOptions(ArchiveConfiguration.pinned(0.0)).build();
  }

  @Bean
  AnswerJudge answerJudge(@Qualifier("judgeChatClient") ChatClient judge) {
    return new AnswerJudge(judge);
  }

  /// Spring AI's own entailment check, for comparison against the quotation tracer.
  ///
  /// The docs' example is this evaluator over Ollama with `bespoke-minicheck`, a small model built
  /// for exactly this. That is the right model and it is not the one here: a 7-billion-parameter
  /// pull does not fit on the machine these runs are made on. So the comparison is run with the
  /// chat model already pinned for the series, and what it measures is the method rather than that
  /// model — a general-purpose model asked to judge entailment, which is the common case anyway.
  /// `forBespokeMinicheck` is one line away when there is room for it.
  @Bean
  FactCheckingEvaluator factCheckingEvaluator(ChatClient.Builder builder) {
    return FactCheckingEvaluator.builder(builder.defaultOptions(ArchiveConfiguration.pinned(0.0)))
        .build();
  }

  @Bean
  SupportAgreement supportAgreement(FactCheckingEvaluator evaluator) {
    return new SupportAgreement(evaluator);
  }

  /// Alpha with room for a whole specification instead of four passages.
  ///
  /// The same persona and the same pinned sampling as the measured line — the comparison is only
  /// worth making if the single variable is how much text the model was given. `num_ctx` is the
  /// exception it has to be: the largest spec in the set is about 15,000 tokens, and the series'
  /// usual 8,192 would silently truncate it, which would measure the truncation.
  @Bean
  ChatClient longContextChatClient(
      ChatClient.Builder builder,
      @Value("classpath:persona/answerer.md") Resource persona,
      @Value("${archive.longcontext.num-ctx:24576}") int numCtx) {
    return builder
        .defaultSystem(persona)
        .defaultOptions(ArchiveConfiguration.pinned(0.0).numCtx(numCtx))
        .build();
  }
}
