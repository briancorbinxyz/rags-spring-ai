package xyz.briancorbin.rags.eval;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import xyz.briancorbin.rags.eval.Ratchet.Tally.Outcomes;
import xyz.briancorbin.rags.eval.RetrievalRun.Result;

/// The ratchet: a step is kept only if it doesn't go backwards. Against the step before it, a step
/// fails if
/// - fewer questions have an expected passage in the top four (Hit@4);
/// - fewer have every expected passage in the top four (all-evidence@4), so a step can't look
///   better by finding one end of a multi-hop question and losing the other;
/// - any one kind of question loses more than one question at Hit@4;
/// - or more questions have a stale record in the top four.
///
/// Those four measure finding, so they are over the answerable questions only. A fifth condition
/// measures what the other four cannot: that the search did not serve passages to a question it had
/// nothing for. Every question gets one of three outcomes over the whole set — a hit, `unsupported`
/// (it served a top four holding no expected passage), or `declined` (it served nothing) — and a
/// step fails if `unsupported` rises, if `declined` falls, or if it declines a question that does
/// have an answer.
///
/// The published clause has a second half: no unsupported answer may outscore the median supported
/// one. That one is not enforced here, because [RetrievalRun.Step] returns addresses without their
/// scores, so this tally has no score to compare. It is checked in the prototype.
public final class Ratchet {

  /// Hit@4 and friends over the answerable questions; the three outcomes over all of them.
  public record Tally(
      int questions,
      int hits,
      int allEvidence,
      int stale,
      Outcomes outcomes,
      Map<String, Integer> hitsByStratum) {

    /// What happened to every question, answerable or not: a hit, or a top four holding nothing it
    /// was entitled to serve, or a decline. `falseRefusals` are the declines on questions that do
    /// have an answer, which is the error the plain count cannot see.
    public record Outcomes(int asked, int unsupported, int declined, int falseRefusals) {

      /// Counted over every question, answerable or not, which is the point of them.
      static Outcomes of(List<Result> results) {
        int asked = 0;
        int unsupported = 0;
        int declined = 0;
        int falseRefusals = 0;
        for (Result result : results) {
          asked++;
          if (result.ranked().isEmpty()) {
            declined++;
            if (result.question().answerable()) {
              falseRefusals++;
            }
          } else if (!result.score().hitAt(RetrievalRun.K)) {
            unsupported++;
          }
        }
        return new Outcomes(asked, unsupported, declined, falseRefusals);
      }
    }

    public static Tally of(List<Result> results) {
      int answered = 0;
      int hits = 0;
      int all = 0;
      int stale = 0;
      var byStratum = new LinkedHashMap<String, Integer>();
      for (String stratum : GoldenSet.STRATA) {
        if (!stratum.equals("unanswerable")) {
          byStratum.put(stratum, 0);
        }
      }
      for (Result result : results) {
        if (!result.question().answerable()) {
          continue;
        }
        answered++;
        if (result.score().hitAt(RetrievalRun.K)) {
          hits++;
          byStratum.merge(result.question().stratum(), 1, Integer::sum);
        }
        all += result.score().allAt(RetrievalRun.K) ? 1 : 0;
        stale += result.staleAt(RetrievalRun.K) ? 1 : 0;
      }
      return new Tally(answered, hits, all, stale, Outcomes.of(results), byStratum);
    }

    @Override
    public String toString() {
      return ("Hit@4 %d/%d, all-evidence@4 %d, stale@4 %d, unsupported@4 %d/%d,"
              + " declined@4 %d, by kind %s")
          .formatted(
              hits,
              questions,
              allEvidence,
              stale,
              outcomes.unsupported(),
              outcomes.asked(),
              outcomes.declined(),
              hitsByStratum);
    }
  }

  private Ratchet() {}

  /// Why `current` fails against `previous`; empty if it passes.
  public static List<String> failures(Tally previous, Tally current) {
    var why = new ArrayList<String>();
    if (current.hits() < previous.hits()) {
      why.add("Hit@4 %d→%d".formatted(previous.hits(), current.hits()));
    }
    if (current.allEvidence() < previous.allEvidence()) {
      why.add("all-evidence@4 %d→%d".formatted(previous.allEvidence(), current.allEvidence()));
    }
    previous
        .hitsByStratum()
        .forEach(
            (stratum, before) -> {
              int after = current.hitsByStratum().getOrDefault(stratum, 0);
              if (before - after > 1) {
                why.add("%s %d→%d".formatted(stratum, before, after));
              }
            });
    if (current.stale() > previous.stale()) {
      why.add("stale@4 %d→%d".formatted(previous.stale(), current.stale()));
    }
    why.addAll(outcomeFailures(previous.outcomes(), current.outcomes()));
    return why;
  }

  /// The fifth condition, which the four above cannot see: a step may not serve more questions it
  /// has nothing for, may not stop declining once it has started, and may never decline a question
  /// that does have an answer.
  private static List<String> outcomeFailures(Outcomes was, Outcomes now) {
    var why = new ArrayList<String>();
    if (now.unsupported() > was.unsupported()) {
      why.add("unsupported@4 %d→%d".formatted(was.unsupported(), now.unsupported()));
    }
    if (now.declined() < was.declined()) {
      why.add("declined@4 %d→%d".formatted(was.declined(), now.declined()));
    }
    if (now.falseRefusals() > 0) {
      why.add("declined %d answerable question(s)".formatted(now.falseRefusals()));
    }
    return why;
  }
}
