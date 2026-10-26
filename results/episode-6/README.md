# Episode 6: the time questions, measured for the first time

`questions-time.jsonl` had never been run against a retriever. These are its first numbers.

```
tools/ratchet-proto/run.py \
  --golden src/main/resources/golden/questions-time.jsonl \
  --audit \
  --embedder ollama:nomic-embed-text:v1.5 \
  --reranker cross-encoder/ms-marco-MiniLM-L-6-v2 \
  --only S3,X1,X2,H1,E5-1,E5-2        --out results/episode-6/time-laptop
  --only S3,X1,X1b,H1,H1b,E5-1,E5-1b,E5-2,E5-2b --out results/episode-6/time-laptop-keep
```

`--audit` is passed empty on purpose: no blind-audit round has ever looked at a time candidate, so
every number here is a floor in the same sense the held-out numbers are.

## What it says

The main line scores **0 of 7 history questions**, and the one history question it could reach it
loses at the step that was added to fix the present. Full reading in the series plan, under
*Episode 6: links, history and time*. The three results that matter:

1. **The ratchet rejects Episode 5's own line on this set.** `X1` fails against `S3`
   (Hit@4 4→3, all@4 4→3, −1 G-057) and `E5-1` fails against `S3` (stale@4 4→6). The rule the
   series publishes, applied to the new set, calls the published steps a regression for time.
2. **G-057 is the spine.** Its expected unit, `004/design.html#D-005`, is `lifecycle=current` —
   the filter never touches it. The **supersession redirect** takes it out, swapping it for D-006
   at rank 3. D-005 is the named motivating example for that redirect in the published Episode 4
   article. Episode 4's hero fix is the exact unit a history question asks about. Both redirect
   rules do it: `X1b`/`E5-1b`/`H1b` (keep the replaced unit behind its successor) are identical on
   every measure, because that rule governs applied changes, not a decision superseded by a sibling.
3. **Four of the five as-of "hits" are not hits.** On G-062 through G-065 the same address is both
   the expected answer and a listed stale answer, so Hit@4 and stale@4 fire on one passage. The
   address is right and the revision served is wrong, and an address-level measure cannot tell the
   difference. `as-of` and `as-known` are unmeasurable here until there is a revision axis.

## Confirmed on the 5090 — run 26 Oct, `e59d5c9` on `episode-4-prototype`

All 24 through the same six configurations on `nomic`, `qwen3-emb-8b` and `qwen3-emb-8b-qwen3-rr`,
against an unshallowed corpus that `revisions.py` verified. Results in
`tools/ratchet-proto/results/5090-*-time.{md,jsonl}` on that branch. `E5-1`:

| tier | Hit@4 | stale@4 | history | h-decoy | as-of | what-changed | as-known |
|---|---|---|---|---|---|---|---|
| laptop, nomic | 7/24 | 6 | **0/7** | 3/3 | 4/5 | 0/5 | 0/4 |
| 5090, nomic | 10/24 | 7 | **0/7** | 3/3 | 4/5 | 2/5 | 1/4 |
| 5090, Qwen3-Emb-8B | 10/24 | 8 | **0/7** | 3/3 | **5/5** | 1/5 | 1/4 |
| 5090, + Qwen3-Reranker-8B | 11/24 | 10 | **0/7** | 3/3 | **5/5** | 2/5 | 1/4 |

1. **History is 1/7 on `S3` and 0/7 on `X1` for every model.** The mechanism is wrong, not the
   model, and that is what the run existed to establish.
2. **A better model makes this set worse.** `stale@4` on `E5-1` goes 6 → 7 → 8 → 10 while `S3` stays
   at 4. A better retriever finds the right address more often, and the right address holds the
   wrong text.
3. **The best configuration scores 5/5 on `as-of`, all five the artefact.** Counting only hits with
   no stale passage in the same top four, the best row's 11 hits are 4 clean and 7 artefact; the
   clean ones are G-059 and G-061 (decoys, wanting current text) and G-067, G-068 (`what-changed`).
   Of the 21 questions that genuinely ask about the past, the best models available answer **two**.

## Reachability, before any retrieval

Of 29 expected addresses across the 24 questions: **18 reachable, 11 removed by the lifecycle
filter, 1 absent from the corpus entirely**. Six questions are wholly unreachable at any depth by
any model — G-054 (rejected), G-055, G-056, G-073 (superseded), G-058 (withdrawn), and G-072, whose
`plan.html` does not exist at `c64d41c` because it was renamed to `design.html` on 2026-07-31.

G-051 and G-052 are permanently partial: one of two expected addresses is withdrawn, so
all-evidence@4 is out of reach while the filter stands.

## A fix the run needed

`run.py:element_texts` read every expected address out of the checkout and crashed on G-072, whose
page is not in this snapshot. A missing page now yields the empty text, exactly as a missing element
already did. The time questions make that case ordinary rather than exceptional: they name addresses
as they stood at a cutoff, and this corpus is one snapshot.

## The ground truth is recoverable from git — tested 26 Oct

The prior question, before any retrieval design: can the answers be recovered at all?

- **All 23 distinct commits** named in `questions-time.jsonl` (29 mentions; several questions share
  one) are reachable in the unshallowed clone.
- **8 of 9 cutoff questions** recover their address as it stood at the cutoff, and **all 8 differ
  from what the retriever serves today**. The ninth, G-075, has no page at its cutoff — correct,
  since it is the unanswerable-by-design row.
- Spot-checked against each question's own `core`, matching word for word: G-062 "16-file tree" then
  against "40-file tree" now; G-065 "Under spectastic-vivid" then against "Every artifact" now;
  G-072 "Status Accepted" then, address absent now.
- **G-072 is recoverable after all.** It is unreachable by a snapshot index and clean under a
  revision one, so the earlier "unreachable regardless of any mechanism" was wrong.

**First-parent position is the index key.** It reproduces the declared before/intermediate/after
order on all five `what-changed` questions, including G-069, whose two intermediates share a
committer timestamp so no date can order them.

Three research gotchas corrected against the real repository: spectastic has **0 merge commits** in
722, so neither the `--before`-traversal trap nor the non-unique-topological-order trap applies
here. Committer date is monotonic along first-parent (0 backward steps) and **author date is not**
(one backward step, `b88df5e8` → `9691dc25`) — but committer date is not injective: 68 commits share
a timestamp, in 16 groups. Order by position, derive it once, never sort by a date at query time.

The probe that produced this is `tools/ratchet-proto/revisions.py` on `episode-4-prototype`
(`1ef9377`), and is the seed of the `Revisions` port. It re-runs in a second with no models:

```bash
python3 tools/ratchet-proto/revisions.py --spectastic ../spectastic
```

It exits non-zero on an unreachable commit and names the likely cause, because the likely cause is a
`--depth 1` clone and both runner scripts make one.
