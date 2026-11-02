#!/usr/bin/env python3
"""How much of a ratchet step is distinguishable from chance.

Reads the per-question run files a ratchet step writes and reports, for each consecutive pair, a
paired exact McNemar test over the questions whose outcome changed, plus a Wilson interval on each
step's Hit@4. Writes nothing and calls nothing: the step files already hold every outcome, so this
needs no model, no corpus and no GPU.

    tools/stats/ratchet_significance.py build/episode-4 step-1 step-2 step-3 step-4

Why exact rather than the chi-square form: with two discordant pairs the chi-square approximation
is meaningless, and every comparison in Episode 4 has fewer than five.
"""

import json
import math
import sys
from math import comb
from pathlib import Path

#: Hit@4 and all-evidence@4 are read at the reader's window, which is what the articles report.
K = 4


def outcomes(directory: Path, step: str) -> dict:
    rows = {}
    for line in (directory / f"{step}.jsonl").read_text(encoding="utf-8").splitlines():
        if line.strip():
            row = json.loads(line)
            rows[row["id"]] = row
    return rows


def hit(row: dict) -> bool:
    return row["first_hit"] is not None and row["first_hit"] <= K


def all_evidence(row: dict) -> bool:
    return bool(row["expected"]) and row["first_all"] is not None and row["first_all"] <= K


def stale(row: dict) -> bool:
    return row["first_stale"] is not None and row["first_stale"] <= K


def exact_mcnemar(gained: int, lost: int) -> float:
    """Two-sided exact binomial test on the discordant pairs; 1.0 when nothing changed."""
    discordant = gained + lost
    if discordant == 0:
        return 1.0
    fewer = min(gained, lost)
    tail = sum(comb(discordant, i) for i in range(fewer + 1)) / 2**discordant
    return min(1.0, 2 * tail)


def wilson(successes: int, n: int, z: float = 1.959963985) -> tuple:
    """A Wilson score interval, which behaves at the small n a golden set has."""
    if n == 0:
        return (0.0, 0.0)
    p = successes / n
    denominator = 1 + z * z / n
    centre = (p + z * z / (2 * n)) / denominator
    half = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / denominator
    return (max(0.0, centre - half), min(1.0, centre + half))


def report(directory: Path, steps: list) -> None:
    by_step = {step: outcomes(directory, step) for step in steps}
    answerable = [
        qid for qid, row in by_step[steps[0]].items() if row["stratum"] != "unanswerable"
    ]
    everything = list(by_step[steps[0]])
    print(f"{len(answerable)} answerable of {len(everything)} asked, at K={K}\n")

    print("| comparison | gained | lost | Hit@4 | exact p (two-sided) |")
    print("|---|---|---|---|---|")
    for before, after in zip(steps, steps[1:]):
        gained = [q for q in answerable if not hit(by_step[before][q]) and hit(by_step[after][q])]
        lost = [q for q in answerable if hit(by_step[before][q]) and not hit(by_step[after][q])]
        was = sum(hit(by_step[before][q]) for q in answerable)
        now = sum(hit(by_step[after][q]) for q in answerable)
        p = exact_mcnemar(len(gained), len(lost))
        print(
            f"| {before} → {after} | {len(gained)} | {len(lost)} "
            f"| {was}/{len(answerable)} → {now}/{len(answerable)} | {p:.2f} |"
        )
        if gained:
            print(f"|  gained: {', '.join(gained)} ||||")
        if lost:
            print(f"|  lost: {', '.join(lost)} ||||")

    first, last = steps[0], steps[-1]
    gained = [q for q in answerable if not hit(by_step[first][q]) and hit(by_step[last][q])]
    lost = [q for q in answerable if hit(by_step[first][q]) and not hit(by_step[last][q])]
    print(
        f"\n{first} → {last}, the whole chain: gained {len(gained)}, lost {len(lost)}, "
        f"exact p = {exact_mcnemar(len(gained), len(lost)):.2f}"
    )

    print("\n| step | Hit@4 | Wilson 95% | stale@4 (of all asked) |")
    print("|---|---|---|---|")
    for step in steps:
        hits = sum(hit(by_step[step][q]) for q in answerable)
        low, high = wilson(hits, len(answerable))
        stales = sum(stale(by_step[step][q]) for q in everything)
        print(
            f"| {step} | {hits}/{len(answerable)} = {hits / len(answerable):.0%} "
            f"| {low:.0%} – {high:.0%} (±{(high - low) / 2 * 100:.0f} pts) | {stales} |"
        )

    print("\nWhat moved on the other columns, which Hit@4 does not show:")
    for before, after in zip(steps, steps[1:]):
        fixed = [q for q in everything if stale(by_step[before][q]) and not stale(by_step[after][q])]
        broke = [q for q in everything if not stale(by_step[before][q]) and stale(by_step[after][q])]
        evidence = [
            q
            for q in answerable
            if not all_evidence(by_step[before][q]) and all_evidence(by_step[after][q])
        ]
        print(
            f"  {before} → {after}: stale fixed {fixed or '—'}, stale broke {broke or '—'},"
            f" all-evidence gained {evidence or '—'}"
        )


if __name__ == "__main__":
    if len(sys.argv) < 4:
        sys.exit(f"usage: {sys.argv[0]} <run directory> <step> <step> [step ...]")
    report(Path(sys.argv[1]), sys.argv[2:])
