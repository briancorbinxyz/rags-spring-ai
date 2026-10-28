# One denominator, not four

Written 28 Oct 2026.

The series grew four question files and reported each on its own. Four headlines is three too many,
and the loudest of them — **47 of 47** — is the narrowest, because the main set is the one the work
was done against. So these two reports put every question the series has ever written into a single
denominator: **105 questions**, being the main set's 53, the 24 about time, the 13 held out and the
15 about routing.

Nothing is scored here. `combine.py`, kept in this folder, reads the per-question rows the two
runners already write and merges them, so a question's result is whichever run already produced it
and no measurement can be invented in the merge. It lives beside the runners at
`tools/ratchet-proto/` on `episode-4-prototype`, with the rest of the prototype work — `moved_in`,
`declared_names`, `intent_ranking`, `groundless` and the new configs.

## Where it stands

| tier | questions scored | Hit@4 | groundless@4 | confidently wrong |
|---|---|---|---|---|
| 5090, the main line | **105 of 105** | **83/98** (84%) | 22/105 | 2 |
| laptop, the floor | 105 of 105 | 60/98 (61%) | 45/105 | 3 |

Read the 5090 row as the series' real position and the laptop row as the guaranteed floor, which is
the standing rule for every other number in the series. Complete as of 28 Oct: the two runs that had
held the main line at 90 of 105 are in, recorded in *the 5090 runs of 28 Oct*.

The 15 remaining misses are **`what-changed` 4, `relation` 4, `history` 5, `as-known` 1,
`intent-noword` 1.** The six unanswerable questions are not among them: they are groundless by
construction, and there to be declined.

## What finishing the figure changed

**The routing set was not hard; the laptop was.** On the laptop the ladder ran 1 of 15 to 6 of 15,
and this folder said these questions had headroom the main set had lost. On the 5090 the same ladder
runs 1 of 15 to **14 of 15**, missing only `R-004`. The headroom was the tier's.

**Eight of the fifteen are won by the cross-encoder alone** — `S4` 6/15 to `S5` 14/15, losing none —
on a set no part of the stack was built against. A 22M-parameter reranker manages 5 of them; an 8B
one manages 14. That is Episode 5's argument about model size, landing on questions Episode 5 never
saw.

**`moved` passes the ratchet but wins nothing at this tier.** Every cutoff row is identical to
`began`, and *located* stays at 1 of 5 rather than reaching the laptop's 3. It lifts every
`what-changed` rank — G-068 3 → 1, G-071 22 → 5, G-069 miss → 10 — without carrying one over the
cut. So it is the better predicate and can ship, and the earlier claim that the 5090 row understated
`what-changed` by two was wrong: the row was right.

## A flag that cost a run

`--window-rule` is read only inside `run_timeline.py`'s `--changed-only` branch. The first 5090
slice run of 28 Oct was given `moved` without `--changed-only`, so it ranked the unrestricted slice,
produced a file named for a rule it had never read, and had to be done again. The flag now refuses
that combination rather than ignoring it.

**The time set is read in two pieces, and that is deliberate.** Fourteen of its questions name a
cutoff or a window and belong on a slice of the archive, so the slice runner answers for those; the
other ten have no cutoff and fall through to the snapshot run. `combine.py` gets this from source
precedence rather than a special case — the slice run is declared first, and it simply has no rows
for the ten.

## The reserve

Held-out and routing questions are scored **once, at an episode's end**. A combined headline
recomputed on every run would quietly spend them, so `combine.py` refuses a reserve source unless
`--at-episode-end` is passed, exactly as `run.py` refuses the held-out file. Both reports here were
produced with that flag, which is correct for a figure meant to close Episode 6 and wrong for a
figure watched while working. The working number stays on the **77 open questions** — the main set's
53 and the 24 about time.

## Reproducing

    combine.py --at-episode-end \
      --golden-dir src/main/resources/golden \
      --source "time=win-moved.jsonl:TD" \
      --source "time=laptop-time.jsonl:GA" \
      --source "main=unw-check.jsonl:GA" \
      --source "heldout=laptop-heldout.jsonl:GA" \
      --source "routing=routing-ladder.jsonl:GA"

Sources are `<set>=<run>:<config>` in precedence order; the first source holding a question owns it.
The tool reads all four question files to know its own denominator and refuses to run if one is
missing, because a short denominator is the one error here that would not announce itself.
