package xyz.briancorbin.rags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import xyz.briancorbin.rags.RetrievalRun.Result;

/**
 * The ratchet: a step is kept only if it doesn't go backwards. Against the step before it, a step
 * fails if
 *
 * <ul>
 *   <li>fewer questions have an expected passage in the top four (Hit@4);
 *   <li>fewer have every expected passage in the top four (all-evidence@4), so a step can't look
 *       better by finding one end of a multi-hop question and losing the other;
 *   <li>any one kind of question loses more than one question at Hit@4;
 *   <li>or more questions have a stale record in the top four.
 * </ul>
 *
 * <p>Unanswerable questions have nothing to find, so they are not counted here.
 */
public final class Ratchet {

  /** Counts over the answerable questions of one run. */
  public record Tally(int questions, int hits, int allEvidence, int stale, Map<String, Integer> hitsByStratum) {

    public static Tally of(List<Result> results) {
      int n = 0;
      int hits = 0;
      int all = 0;
      int stale = 0;
      var by = new LinkedHashMap<String, Integer>();
      for (String s : GoldenSet.STRATA) {
        if (!s.equals("unanswerable")) {
          by.put(s, 0);
        }
      }
      for (Result r : results) {
        if (!r.question().answerable()) {
          continue;
        }
        n++;
        if (r.score().hitAt(RetrievalRun.K)) {
          hits++;
          by.merge(r.question().stratum(), 1, Integer::sum);
        }
        all += r.score().allAt(RetrievalRun.K) ? 1 : 0;
        stale += r.staleAt(RetrievalRun.K) ? 1 : 0;
      }
      return new Tally(n, hits, all, stale, by);
    }

    @Override
    public String toString() {
      return "Hit@4 %d/%d, all-evidence@4 %d, stale@4 %d, by kind %s".formatted(hits, questions, allEvidence, stale, hitsByStratum);
    }
  }

  private Ratchet() {}

  /** Why {@code current} fails against {@code previous}; empty if it passes. */
  public static List<String> failures(Tally previous, Tally current) {
    var why = new ArrayList<String>();
    if (current.hits() < previous.hits()) {
      why.add("Hit@4 %d→%d".formatted(previous.hits(), current.hits()));
    }
    if (current.allEvidence() < previous.allEvidence()) {
      why.add("all-evidence@4 %d→%d".formatted(previous.allEvidence(), current.allEvidence()));
    }
    previous.hitsByStratum().forEach((stratum, before) -> {
      int after = current.hitsByStratum().getOrDefault(stratum, 0);
      if (before - after > 1) {
        why.add("%s %d→%d".formatted(stratum, before, after));
      }
    });
    if (current.stale() > previous.stale()) {
      why.add("stale@4 %d→%d".formatted(previous.stale(), current.stale()));
    }
    return why;
  }
}
