"""The golden set as a TREC test collection: qrels for the judgments, run files for the rankings.

Information retrieval's standard tools (``trec_eval``, ``ir_measures``, ``ranx``) read two plain-text
formats, so exporting to them lets anyone check our numbers with a scorer we didn't write:

- **qrels**: one judgment per line, ``question 0 passage relevance``. Relevance 1 is an expected
  passage or one the blind audit accepted; 0 is a passage someone judged and found wanting: a stale
  record, or an audit decision of "partly" or "doesn't answer it". Unanswerable questions have no
  relevant passage, so they have no lines.
- **run**: one result per line, ``question Q0 passage rank score run-name``. A long unit is split
  into parts, so a ranking can name the same passage twice; the standard tools keep one entry per
  passage, so only the first occurrence is written (keeping the later one would move it down).

Our Hit@4 is ``Success@4`` in these tools and our MRR is ``RR``. All-evidence@4 has no exact
counterpart: a stand-in passage covers one expected element, which qrels can't express, so it stays
our own measure.
"""

from __future__ import annotations

import json
from pathlib import Path

ACCEPTED = {"whole", "standin"}


def qrels_lines(questions: list, audit: list) -> list:
    """Judgments in a fixed order: per question, expected, then accepted, then stale, then the audit's
    other decisions sorted by address. A passage is judged once, by the first rule that names it."""
    by_q: dict = {}
    for a in audit:
        by_q.setdefault(a["id"], []).append(a)
    lines = []
    for q in sorted(questions, key=lambda q: q["id"]):
        if q["stratum"] == "unanswerable":
            continue
        seen: dict = {}
        for e in q.get("expected") or []:
            seen.setdefault(e, 1)
        for a in q.get("accept") or []:
            seen.setdefault(a["address"], 1)
        for s in q.get("stale") or []:
            seen.setdefault(s, 0)
        for a in sorted(by_q.get(q["id"], []), key=lambda a: a["candidate"]):
            seen.setdefault(a["candidate"], 1 if a["decision"] in ACCEPTED else 0)
        lines += [f"{q['id']} 0 {p} {rel}" for p, rel in seen.items()]
    return lines


def run_lines(qid: str, ranked: list, run_name: str, depth: int = 50) -> list:
    """One line per passage, first occurrence only, ranks from 1; the score is 1/rank so the tools,
    which sort by score, keep our order."""
    out, seen = [], set()
    for addr in ranked:
        if addr in seen:
            continue
        seen.add(addr)
        n = len(out) + 1
        out.append(f"{qid} Q0 {addr} {n} {1.0 / n:.6f} {run_name}")
        if n == depth:
            break
    return out


def load_jsonl(path: Path) -> list:
    return [json.loads(l) for l in Path(path).read_text(encoding="utf-8").splitlines() if l.strip()]


def write_qrels(questions_path: Path, audit_paths, out: Path) -> int:
    """Qrels from the golden set and every round of the audit.

    A round is a file of its own, so a later one is added by naming it rather than by appending to
    a round that has already shipped. Rounds are read in the order given, and where two judge the
    same passage the first one named wins, as ``qrels_lines`` does throughout. A single path is
    still accepted, as the one-round call sites pass."""
    if isinstance(audit_paths, (str, Path)):
        audit_paths = [audit_paths]
    audit = []
    for path in audit_paths:
        if Path(path).exists():
            audit.extend(load_jsonl(path))
    lines = qrels_lines(load_jsonl(questions_path), audit)
    Path(out).write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    return len(lines)


if __name__ == "__main__":  # python3 -m ratchet.trec questions.jsonl audit.jsonl [...] qrels.txt
    import sys

    print(write_qrels(Path(sys.argv[1]), [Path(a) for a in sys.argv[2:-1]], Path(sys.argv[-1])),
          "judgments")
