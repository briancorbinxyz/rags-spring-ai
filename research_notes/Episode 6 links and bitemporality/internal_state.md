# Episode 6 ("Links and history"): what is already planned, coded and measured — and what is stale

Scope note: every "source" below is a local path, git object or vault note, not a URL. Commits are
on `/Users/briancorbin/Code/rags/rags-spring-ai`. Read-only research; nothing was run.

Two facts that frame every number here:

- **The frozen golden set is 53 questions, 47 answerable** (`questions.jsonl`, v2, frozen
  20 Oct 2026 against spectastic `c64d41c`). Strata: single-fact 10, paraphrase 10, identifier 8,
  relation 7, stale 7, intent 5, unanswerable 6. Any `/44` or `/41` denominator in the prototype
  README or in an Episode 3 row is a **v1-era denominator** and is not comparable.
  — [src/main/resources/golden/README.md](/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/README.md)
- **Two supersession rules exist**, and commit `d7a1be4` (23 Oct 2026) swapped which key means
  which. "drop" = `redirect="replace"`; "keep" = `redirect="behind"`.

---

## Q1. What exactly do X2, H2, E5-2 and EpiB do, what numbers exist, and under which rule and qrels?

### Takeaway

All four are Episode 6's actual subject matter (typed-link expansion and the rejected
graph-fusion design), all four are implemented only in the Python prototype, and **every saved
number for all four is doubly stale**: produced under the old "keep applied history behind"
redirect *and* before the round-3 audit. None of the four was re-run when the rule changed, and
none exists in the Java harness at all.

### Cited findings — definitions

- `X2` = "S3 + redirect + anchored typed expansion", on the BM25 line, no embedder. Config:
  `ids=True, lifecycle=True, redirect="replace", expand=True`, `**ep4` units.
  — [run.py @ origin/episode-4-prototype](git show origin/episode-4-prototype:tools/ratchet-proto/run.py)
- `H2` = "H1 + anchored typed expansion (no reranker)" — BM25 + dense RRF + id lookup + lifecycle
  + redirect + expansion, reranker skipped. Config: `ids, lifecycle, dense, redirect="replace",
  expand=True, needs=("dense",)`. — same file
- `E5-2` = "E5-2 + anchored typed expansion" — the full main line (S5: id lookup, lifecycle,
  dense RRF, cross-encoder) plus redirect plus expansion. Config: `ids, lifecycle, dense, rerank,
  redirect="replace", expand=True`. — same file
- `EpiB` = "epilogue: RRF-merge neighbours (k=60, w=0.5) instead of anchored expansion" — the
  **rejected** design: neighbours of the top 5, all link types, fused rather than inserted.
  Config: `ids, lifecycle, dense, rerank, redirect="replace", rrf_neighbours=True`. — same file
- The expansion step itself, in the README's own words: "From the first pinned unit, follow typed
  links only (validates, cites, target incl. spec-delta, defer-to), one hop, at most 5 units
  ranked by BM25 among themselves, inserted right after the anchor, after the reranker."
  — [tools/ratchet-proto/README.md ~line 314 @ episode-4-prototype](git show origin/episode-4-prototype:tools/ratchet-proto/README.md)
- The redirect step (E5-1, the base all four sit on), post-`d7a1be4`: "A stale item is replaced by
  its current successor (spec-delta targets, in-place `superseded` status with a link, 'superseded
  by' prose; up to three hops) and is itself dropped, applied history included. Two stale items
  can share a successor, so a list can come back shorter than it went in." — same README
- Pipeline order is documented as: BM25/dense → fusion → pinning → rerank (top 50 unpinned) →
  **expansion (5)** → **redirect (6)**; every list cut to 50.
  — [ratchet/pipeline.py docstring @ episode-4-prototype](git show origin/episode-4-prototype:tools/ratchet-proto/ratchet/pipeline.py)
- Fixed, untuned defaults that apply: part size 250 words, depth 100 per list before fusion,
  RRF k=60, rerank top 50, **at most 5 neighbours**. "None was tuned on the golden set." — README

### Cited findings — every saved number (all STALE, see judgement)

From `tools/ratchet-proto/results/5090-nomic.md`, run **2026-10-23 00:44**, last written by commit
`6615f99` "5090 results", header says "Golden set: questions.jsonl (answerable 47, unanswerable 6)":

| key | Hit@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx mean/max | verdict / won-lost |
|---|---|---|---|---|---|---|---|
| X2 | 33/47 (70%) | 0.641 | 28 | 34 | 0 | 526 / 1227 | pass vs X1; +1 G-030 / −0 |
| H2 | 37/47 (79%) | 0.702 | 35 | 36 | 0 | 664 / 1341 | (ablation table, no verdict) |
| E5-2 | 37/47 (79%) | 0.670 | 33 | 37 | 1 | 830 / 1539 | pass vs E5-1; +2 G-029 G-030 / −0 |
| EpiB | 27/47 (57%) | 0.348 | 24 | 35 | 2 | 756 / 1539 | (ablation table) |

From `5090-qwen3-emb-8b.md` (Qwen3-Embedding-8B + `bge-reranker-v2-m3`), run 2026-10-23 00:50,
same commit `6615f99`:

| key | Hit@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx mean/max |
|---|---|---|---|---|---|---|
| X2 | 33/47 (70%) | 0.641 | 28 | 34 | 0 | 526 / 1227 |
| H2 | 38/47 (81%) | 0.726 | 36 | 39 | 0 | 580 / 1164 |
| E5-2 | 38/47 (81%) | 0.735 | 34 | 39 | 1 | 812 / 1492 |
| EpiB | 29/47 (62%) | 0.417 | 26 | 36 | 2 | 747 / 1492 |

From `5090-qwen3-emb-8b-qwen3-rr.md` (Qwen3-Embedding-8B + Qwen3-Reranker-8B), run 2026-10-23
00:52, same commit `6615f99` — the strongest tier:

| key | Hit@4 | MRR@50 | all@4 | all@10 | stale@4 | ctx mean/max |
|---|---|---|---|---|---|---|
| X2 | 33/47 (70%) | 0.641 | 28 | 34 | 0 | 526 / 1227 |
| H2 | 38/47 (81%) | 0.726 | 36 | 39 | 0 | 580 / 1164 |
| E5-2 | **46/47 (98%)** | 0.890 | 42 | 43 | 1 | 636 / 1371 |
| EpiB | 36/47 (77%) | 0.481 | 31 | 41 | 1 | 638 / 1371 |

Also saved, and under the old naming these were the *dropping* rule, i.e. **today's E5-2 semantics
scored on 23 Oct**: `E5-2r` = 37/47 · 0.670 · all@4 33 · all@10 37 · stale@4 **0** (nomic);
38/47 · 0.736 · 34 · 39 · **0** (qwen3-emb-8b); 46/47 · 0.890 · 42 · 43 · **0** (qwen3-rr).
— the three `*.md` files above, "Variants, epilogue and ablations" tables

- The X2 ratchet comparison is explicitly against X1: "pass (vs X1) | +1 G-030 / −0" on all three
  runs; the BM25-line table in all three files is byte-identical (no embedder involved), so X2's
  33/47 is a single measurement reported three times, not three independent ones.
- E5-2's gain over E5-1 is **the same two questions on every tier**: +2 **G-029 G-030**, −0.
- The prototype also carries a laptop-era proxy run, `results/proxy-wordllama.{jsonl,md}`, whose
  dense rows are explicitly "PROXY" and "not Episode 4 numbers" (WordLlama scores 13/44 on config
  A where nomic scored 9/44, i.e. *more* lexical than nomic). — README caveats

### Cited findings — provenance of each number

- `5090-nomic.md`, `5090-qwen3-emb-8b.md`, `5090-qwen3-emb-8b-qwen3-rr.md` were each last written
  by **`6615f99` "5090 results"** (2026-10-23) and never touched again.
  — `git log --oneline origin/episode-4-prototype -- tools/ratchet-proto/results/5090-nomic.md`
- The post-rule-change re-runs are the `*-drop-superseded.*` family, written by `6d2dbab` →
  `a71891d` (add S4/S5) → **`4e45d50` "re-score the 5090 drop-superseded runs with the audit"**
  (2026-10-24). Those files contain **only** S3, S4, S5, X1, X1b, H1, H1b, E5-1, E5-1b.
  There is **no X2, H2, E5-2, E5-2b or EpiB run file or row anywhere in the drop-superseded set**.
  — `git ls-tree -r --name-only origin/episode-4-prototype -- tools/ratchet-proto/results/`
- The held-out family (`*-heldout.*`, commit `c971f2b`, 2026-10-24) contains only S3, X1, H1,
  E5-1. No expansion configuration has ever been run on the held-out set. — same listing
- The reason the four were skipped is stated in the vault: "`X2`, `H2`, `E5-2` and `EpiB` also
  changed meaning in this commit but appear nowhere in the article, so they are not in the list."
  — ["RAGs to Riches, What Is Left to Run on the 5090.md"](/Users/briancorbin/Documents/Brain/Library/03%20Resources/RAGs%20to%20Riches,%20What%20Is%20Left%20to%20Run%20on%20the%205090.md)

### Stale / current judgement, per number

| number | judgement | why |
|---|---|---|
| X2 33/47 · all@4 28 · stale@4 0 (all three runs) | **STALE on two counts** | produced under `redirect="behind"` (pre-`d7a1be4`); and pre-audit qrels (round 3 landed 24 Oct in `70e6920`, re-score only reached the drop-superseded files in `4e45d50`). Mitigating: the vault reports the two rules "cannot be told apart at all" on the keyword line, so X2's Hit@4 is very likely unchanged — but that is an inference, not a measurement. |
| H2 37/47 (nomic) / 38/47 (both qwen3 runs) | **STALE on two counts** | same; and H1 vs H1b were measured identical post-change, so the redirect half is probably neutral — unverified for the expansion half. |
| E5-2 37/47 (nomic) / 38/47 (qwen-bge) / 46/47 (qwen-rr) | **STALE on two counts, and the rule change is known to bite here** | `stale@4` is **1** on all three E5-2 rows, exactly the failure mode `d7a1be4` was written to remove. The `E5-2r` twin (= today's rule, 23 Oct) shows stale@4 **0** at identical Hit@4. So the honest current E5-2 figures are the `E5-2r` rows, still pre-audit. |
| EpiB 27/47 / 29/47 / 36/47 | **STALE on two counts; never re-run** | no `E5-2r`-style twin exists for EpiB, so there is no saved measurement of EpiB under today's rule at all. |
| all four, all-evidence@4 and all@10 | **STALE and known to move** | the audit note states `all@10` "rose on nearly every row" and `all@4` "also moved on S4, S5 and the b variants"; the drop-superseded re-score moved nomic E5-1 all@4 31→32 and qwen-bge 32→33. The X2/H2/E5-2/EpiB all@4 figures above sit on the pre-audit side of exactly that shift. |
| `Judged@4` for these rows | **does not exist** | post-audit `Judged@4` was measured only for X1 (0.73), H1 (0.63) and the 44/47 E5-1 row (0.55). Pre-audit it was 0.61 / 0.56 / 0.49. — "What Is Left to Run on the 5090.md" |

### Inferences

- Episode 6's headline step (anchored typed expansion) currently has **zero numbers under the
  shipping supersession rule and the shipping qrels**. The cheapest first task of the episode is
  a re-run of `X2,H2,E5-2,E5-2b,EpiB` with `--out-tag` on the three 5090 models, plus the
  held-out set.
- Because X2's BM25-line table is identical across the three runs, the expansion step's
  *measured* effect on the keyword line is a single +1 question (G-030) and +2 all-evidence
  (26→28). Everything larger comes from the reranked tier.

### Gaps

- The series plan claims "on the 5090 line E5-2 reaches 41/41 on the shared questions". I could
  not locate the run or table that produces a `41/41` denominator; no saved `.md` reports it.
  Treat as unverified.
- `origin/fix/run5090-python-quoting` carries `f53c55a` whose `5090-qwen3-emb-8b.*` results
  **differ** from the ones on `episode-4-prototype`; the vault flags that canonicality is
  undecided. I did not diff them.

---

## Q2. Which Episode 6 keys does `d7a1be4` affect, and which saved files are mislabelled?

### Takeaway

`d7a1be4` renamed keys and swapped their meanings in one commit. Every pre-`d7a1be4` run file
named `X1`, `X2`, `H1`, `H2`, `E5-1`, `E5-2` or `EpiB` holds the **keeping** rule, which today is
spelled `*b`; and the files named `E5-1r`/`E5-2r` hold what is today plain `E5-1`/`E5-2`.

### Cited findings

- Commit: `d7a1be47232a9ae4724aa4f7b53b13f13ebdd546`, "ratchet-proto: the main line's redirect
  drops superseded history", 2026-10-23 02:33 -0400, on `episode-4-prototype` only. Message:
  "X1, X2, H1, H2, E5-1, E5-2 and EpiB now drop it. The old rule stays as X1b, H1b, E5-1b and
  E5-2b for the epilogue's comparison." — `git show d7a1be4`
- The before/after config diff: `E5-1`, `E5-2`, `EpiB`, `X1`, `X2`, `H1`, `H2` all moved from
  `redirect="behind"` to `redirect="replace"`; `E5-1r`/`E5-2r` (which were `"replace"`) were
  **deleted** and replaced by new `E5-1b`/`E5-2b`/`X1b`/`H1b` carrying `"behind"`.
  — `git show d7a1be4 -- tools/ratchet-proto/run.py`
- Note the asymmetry introduced: `X2b`, `H2b` and `EpiBb` were **never created**. Only `X1b`,
  `H1b`, `E5-1b`, `E5-2b` exist as keeping-rule keys, so the old rule is no longer runnable for
  X2, H2 or EpiB.  — same diff
- The authoritative mapping table, written in the vault under the heading "A trap in the results
  already on disk":

  | saved file | rule it actually used | key for that rule today |
  |---|---|---|
  | `5090-*.X1.run` | keeps history behind | `X1b` |
  | `5090-*.H1.run` | keeps history behind | `H1b` |
  | `5090-*.E5-1.run` | keeps history behind | `E5-1b` |
  | `5090-*.E5-2.run` | keeps history behind | `E5-2b` |
  | `5090-*.E5-1r.run` | drops superseded | `E5-1` |
  | `5090-*.E5-2r.run` | drops superseded | `E5-2` |
  | `5090-*.X2.run`, `.H2.run`, `.EpiB.run` | keeps history behind | **no key today**; today's `X2`/`H2`/`EpiB` drop |

  with the instruction "Do not rename the saved files by pattern — the data would end up
  mislabelled." — "RAGs to Riches, What Is Left to Run on the 5090.md"
- Concretely mislabelled, relative to today's names, in `tools/ratchet-proto/results/` on
  `episode-4-prototype` (all written by `6615f99`, 2026-10-23):
  `5090-nomic.{X1,X2,H1,H2,E5-1,E5-2,EpiB}.run`,
  `5090-qwen3-emb-8b.{X1,X2,H1,H2,E5-1,E5-2,EpiB}.run`,
  `5090-qwen3-emb-8b-qwen3-rr.{X1,X2,H1,H2,E5-1,E5-2,EpiB}.run`
  — 21 run files, plus the three `.jsonl` and three `.md` whose row keys carry the same labels.
  — `git ls-tree -r --name-only origin/episode-4-prototype -- tools/ratchet-proto/results/`
- Correctly labelled, under today's names: the nine files per run in each `*-drop-superseded.*`
  set (`4e45d50`) and the four per run in each `*-heldout.*` set (`c971f2b`), plus the local
  untracked `results/episode-5/heldout-laptop.{S3,X1,H1,E5-1}.run` (laptop, 2026-10-24 19:58).
- The Java side also moved: "`SupersessionRedirect.java` — the `Lifecycle.HISTORY` branch is
  gone." — ["RAGs to Riches, The G-043 Problem.md"](/Users/briancorbin/Documents/Brain/Library/03%20Resources/RAGs%20to%20Riches,%20The%20G-043%20Problem.md)

### Inferences

- For Episode 6 the practical consequence is narrow but sharp: of the four configurations the
  episode is about, **three (`X2`, `H2`, `EpiB`) have no keeping-rule twin and no dropping-rule
  measurement.** Their only saved numbers are under a rule that no longer ships, and there is no
  way to reproduce the old rule for them without re-adding `*b` variants.
- `E5-2` is the one exception: `E5-2r` on disk already *is* today's E5-2, so an Episode 6 writer
  can quote 37/47 (nomic) / 38/47 (qwen-bge) / 46/47 (qwen-rr) with stale@4 0 under today's rule,
  subject only to the pre-audit caveat.

### Gaps

- Episode 4's published prose is known to be wrong post-change ("Episode 4's **prose** does still
  need amending — 'an applied change… stays, right behind its successor' is no longer what the
  code does") and I found no commit that amended it. — "The G-043 Problem.md"

---

## Q3. The two Episode 6 question sets: every field and what it drives

### Takeaway

`questions-time.jsonl` holds **24** questions over five strata (history 7, history-decoy 3, as-of
5, what-changed 5, as-known 4) and has **never been run against a retriever**; it is scored by
*version*, not only by address. `questions-heldout.jsonl` holds **13** (relation 8, history 5) and
has been run once on the laptop and once per 5090 model, at four configurations only.

### Cited findings — `questions-time.jsonl` (24)

- Ids and strata (note **G-053 does not exist**; the range is G-051…G-075 minus G-053):
  history `G-051 G-052 G-054 G-055 G-056 G-057 G-058`; history-decoy `G-059 G-060 G-061`;
  as-of `G-062 G-063 G-064 G-065 G-066`; what-changed `G-067 G-068 G-069 G-070 G-071`;
  as-known `G-072 G-073 G-074 G-075`. Every row carries `status: "accepted"` and
  `commit: c64d41c2e5d0d42c4fefd1b506cb083b9812bf6c`.
  — [src/main/resources/golden/questions-time.jsonl](/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions-time.jsonl)
- Stratum definitions, in the README's words: history = "Asks about a withdrawn, superseded,
  retired or rejected record on purpose"; history-decoy = "Sounds historical ('no longer',
  'originally') but the answer is current"; as-of = "What an element said on a date, including one
  tag-versus-date paraphrase pair"; what-changed = "The versions of an element inside a window, in
  order"; as-known = "What the archive said at the time, before a later correction".
  — golden/README.md, "Time set"
- README warning: "With 4 or 5 questions a stratum, one question is a fifth of it: compare these
  per question, not as rates." — same
- Field-by-field, with presence counts (my own census of the file) and what each drives:

| field | where it appears | what it carries / drives |
|---|---|---|
| `version` | all 24 | `{commit, date}` of the version that answers the question. Makes the answer a *version*, not an address: the harness must resolve an address **at a commit**. |
| `cutoff` | as-of 5, as-known 4 (9) | ISO instant, usually `…T23:59:59Z` (day-granular, UTC); drives an "as at" filter. `G-063` is the exception, `2026-08-12T18:11:23Z`, an exact tag timestamp. |
| `cutoff_ref` | as-of 1 (`G-063`) | `"v0.1.0-pre.24"` — a release tag resolved to the cutoff instant. Drives tag→date resolution. |
| `window` | what-changed 5 | `{from, to}` plus optional `from_ref` / `to_ref` tags (`G-068` pre.27→pre.28, `G-071` pre.25→pre.26, `G-070` date→pre.28 mixed). Drives a range query. |
| `versions` | what-changed 5 | Ordered list of `{commit, date, address, role}` with `role ∈ {before, intermediate, after}` and an optional `absent: true`. **This is the ordering ground truth** — `G-067` and `G-069` each carry two `intermediate` rows. Drives "what came after what". |
| `stale_versions` | as-of 5, as-known 4, history-decoy 1 (`G-060`) (10) | `{commit, date, address, note}` for versions that would answer *wrongly* at the given cutoff — including the **current** text (`G-062`: "current: 40-file tree") and the next version after the cutoff (`G-062`: "next version after the cutoff: 20-file tree, 2026-07-24"). Drives a bitemporal `stale@4`. |
| `stale` | all 24 | the Episode-4-style per-question stale addresses (no commit). Drives today's `stale@4`. |
| `related` | history 4, what-changed 1 (5) | sibling records that are *not* the answer but are adjacent (e.g. `G-051`'s sibling withdrawn proposal). Reads as near-miss / distractor documentation. |
| `evidence` | all 24 | list of `{address, commit, quote}` — a verbatim quote **pinned to a commit**. Drives quote-level grounding checks across history (the Java `QuoteResolver`/`CitationChecker` idea, extended in time). |
| `attr_checks` | as-known 1 (`G-073`) | `{address, commit, attr, expected}` pairs asserting `data-status` is `null` at `b15717a` and `"superseded"` at `93f50c4`. Drives "the attribute did not exist yet" — i.e. proves the as-known semantics mechanically. |
| `absent_checks` | as-known 1 (`G-075`) | `{path, commit}` asserting `specs/111-codex-skill-adapters/spec.html` does not exist at `b632e93`. Drives "the record was not in the archive yet". |
| `lexical_overlap` | all 24 | the words the question shares with its expected element, e.g. `G-051 ["003","nothing","scaffold","second"]`; **empty for G-056, G-062, G-066, G-070, G-073**. Drives the BM25-favouritism caveat per question: an empty list is a question BM25 cannot win on wording. |
| `router` | all 24 | the intended route and an explicit prediction of whether the router will get it. See Q4. |
| `notes` | all 24 | what the question tests, the likeliest wrong answer, and disambiguation from held-out questions (`G-051` is explicitly "Distinct from H-009"). |
| `paraphrase_of` | as-of 1 (`G-064` → `G-063`) | the tag-versus-date paraphrase pair the README mentions. Drives "does phrasing change the route/result". |
| `answer`, `core` | all 24 | reference answer and the minimum a correct answer must contain. |

- The day-granularity convention is written into the set: "Dates are UTC calendar days… Only one
  question depends on it (**G-067**: two of its three rewrites were on the evening of 13 August in
  New York), and that one says so." — golden/README.md, "Conventions"
- `G-067` is deliberately the G-043 case: window `2026-08-13T23:59:59Z` → `2026-08-14T23:59:59Z`
  over `specs/095-visual-element-vocabulary/spec.html#FR-013`, with four versions — one `before`
  marked `absent: true` (8a1b8a0, 2026-08-12), two `intermediate` (048f5fb 02:11, 4f87f0e 02:30),
  one `after` (8677b60 12:59). Cross-referenced in the vault: "`questions-time.jsonl` `G-067` asks
  about this same FR-013 rewrite history on purpose." — "The G-043 Problem.md"
- `G-069` is the same-day-collision case on the what-changed side: two `intermediate` versions with
  the **identical** date `2026-07-12T01:39:22Z` but different commits (945f1b2, 013cfd4), over
  `specs/042-profile-enforcement/spec.html#FR-002`.
- **Status: never measured.** "questions-time.jsonl: 24 history and time questions, scored by
  version, never run against a retriever." — commit `5d87cc0` "Golden set v2: signed off,
  blind-audited and frozen at c64d41c" (2026-10-20). Repeated in the README: "They were checked
  against the full spectastic history (every address exists at its commit, every quote is
  verbatim, every date is the committer date in UTC) and **have never been run against a
  retriever**."

### Cited findings — `questions-heldout.jsonl` (13)

- Fields (uniform across all 13): `id, question, stratum, expected, stale, answer, core, commit,
  status, heldout: true, link, anchored, evidence`. No `router`, no `version`, no `cutoff`, no
  `lexical_overlap`. `stale` is empty (`[]`) for all 13.
- `link` records which link type carries the answer, and `anchored` whether the identifier lookup
  can find the anchor:

| id | stratum | `link` | `anchored` |
|---|---|---|---|
| H-001 | relation | `typed:validates` | true |
| H-002 | relation | `typed:validates (unanchored)` | false |
| H-003 | relation | `typed:defer-to (to the page)` | true |
| H-004 | relation | `typed:defer-to + link to FR-013` | true |
| H-005 | relation | `typed:validates (unanchored)` | false |
| H-006 | relation | `prose only` | false |
| H-007 | relation | `prose only` | false |
| H-013 | relation | `prose only` | false |
| H-008 | history | `prose "superseded by" + link` | false |
| H-009 | history | `withdrawn proposal` | false |
| H-010 | history | `withdrawn proposal` | false |
| H-011 | history | `applied proposal → current requirement` | false |
| H-012 | history | `superseded (retired) task` | false |

  So: 5 of 8 relation questions are typed-link questions, 3 are prose-only (unreachable by typed
  expansion by construction); only 3 of 13 are anchored, and anchored expansion starts from the
  pinned anchor.
- Held-out measurements that exist, all four configurations only (S3, X1, H1, E5-1), all under
  today's dropping rule, all 13 questions, 0 unanswerable:

| tier / run file | S3 | X1 | H1 | E5-1 |
|---|---|---|---|---|
| laptop, `ollama:nomic-embed-text:v1.5` + MiniLM, 2026-10-24 19:58 (`results/episode-5/heldout-laptop.md`, **untracked**) | 2/13 · all@4 0 · MRR 0.070 | 2/13 · 1 · 0.082 | 2/13 · 0 · 0.112 | 4/13 · 1 · 0.315 |
| 5090 `nomic` + bge, 2026-10-24 20:14 (`5090-nomic-heldout.md`, `c971f2b`) | 2/13 · 0 · 0.070 | 2/13 · 1 · 0.082 | 2/13 · 0 · 0.112 | 4/13 · 3 · 0.229 |
| 5090 Qwen3-Emb-8B + Qwen3-Reranker-8B (`5090-qwen3-emb-8b-qwen3-rr-heldout.md`, `c971f2b`) | 2/13 · 0 · 0.070 | 2/13 · 1 · 0.082 | 3/13 · 1 · 0.206 | **5/13 · 2 · 0.310** |

  Per stratum, relation (8) · history (5) at Hit@4: laptop S3 0·2, E5-1 2·2; 5090-nomic E5-1 2·2;
  5090-qwen-rr E5-1 3·2. Won questions named: laptop/5090-nomic E5-1 "+2 H-004 H-006";
  5090-qwen-rr E5-1 "+3 H-002 H-004 H-006".
- The uncomfortable reading, in the vault's words: "Episode 4's line finds **none of the eight**
  relation questions it has not seen, against 6 of 7 on the frozen set. And the order inverts:
  MiniLM, which fails the ratchet outright on the main set, is the best row here. On 13 questions
  a two-question gap is noise, so the finding is the level, not the ranking."
  — "What Is Left to Run on the 5090.md"
- And the floor caveat: "Nothing in the held-out set has ever been audited — the main set has had
  three rounds, this has had none — so every returned passage counts as wrong unless it is an
  exact expected match… The pack is built and waiting at `build/audit/round4-heldout.html`, 105
  candidates over all 13 questions, deferred by decision." — same

### Stale / current judgement

- **`questions-time.jsonl`: current as a specification, zero numbers.** Frozen at `c64d41c`, every
  address and quote verified against full history. There is nothing to be stale *about* — but
  equally nothing Episode 6 can quote as a baseline. This is the single largest measurement gap.
- **Held-out numbers: current on the rule, stale on the audit, and a floor by construction.**
  They were produced by `c971f2b` / the local laptop run on 24 Oct, after `d7a1be4`, so the
  redirect is today's. But round 4 of the audit has not run, so every figure is a lower bound.
- **One vault staleness to flag:** "To run: the held-out set on the 5090" says the 5090 tier is
  missing. It is not any more — `c971f2b` ran it at 20:14 on 24 Oct; the note's own mtime is
  2026-10-24 20:11:39, three minutes earlier. The note is stale by minutes, not by substance.
- The series plan's Episode 6 row says the history stratum is "7 questions plus 3 decoys", which
  matches the file exactly (history 7 + history-decoy 3) and is **current**.

### Gaps

- The 14 **draft ordering questions** are listed as outstanding: "The 14 draft ordering questions,
  once a file and an id range are chosen." They are not in the repository under any id range I
  could find, and no file holds them. — "What Is Left to Run on the 5090.md"
- The "ORD questions" named in the G-043 note ("the same ordering capability the ORD questions
  were written for") appear nowhere in the repo or the vault under an `ORD-` id. Most likely an
  informal name for either the `what-changed` stratum (G-067…G-071) or the 14 drafts above, but I
  could not confirm which.

---

## Q4. Reconstructing the routing design the `router` annotations assume

### Takeaway

The `router` field encodes a four-route design — `CURRENT`, `HISTORY`, `AS_OF(cutoff)`,
`CHANGE(element, from, to)`, `AS_KNOWN(cutoff)` — driven by a two-tier marker list: a **narrow**
list of lifecycle words that forces HISTORY, and a **broad** list of words that merely *sound*
historical and must stay CURRENT. The annotations deliberately predict **five of the seven history
questions will misroute** and **one of the three decoys will misroute**, i.e. the set is built to
fail the rule list it documents.

### Cited findings — all 24 `router` values verbatim

- `G-051` "HISTORY: 'withdrawn' is in the narrow lifecycle list."
- `G-052` "HISTORY: 'withdrawn'."
- `G-054` "HISTORY intended; 'set aside' is not a narrow-list marker. Expect CURRENT routing (a known miss) unless explicit params are passed."
- `G-055` "HISTORY: 'shelved' is not on the narrow list; 'twice drew up' is not either. Likely a router miss; kept because it is how a person asks."
- `G-056` "HISTORY: 'original' is close to the narrow marker 'originally' but is not it; 'Before the Node rewrite' is not a marker. Likely router miss."
- `G-057` "HISTORY: 'first settle on ... before that choice was replaced' has no narrow marker; 'replaced' is not on the list. Likely router miss."
- `G-058` "HISTORY intended; no narrow-list marker ('ever consider', 'didn't go ahead'). Likely router miss."
- `G-059` "CURRENT (decoy). Broad-list markers present: 'changed'."
- `G-060` "CURRENT (decoy). Broad-list marker: 'no longer'."
- `G-061` "CURRENT (decoy). Narrow-list marker present: 'originally' (expected misroute under the report's rule list)."
- `G-062` "AS_OF(2026-07-01): 'As of' + date."
- `G-063` "AS_OF(v0.1.0-pre.24): 'As of' + release tag."
- `G-064` "AS_OF(2026-08-12): 'On' + date. The report's rule ('as of / in / at' + date) does not list 'On <date>'; record whether the router catches it."
- `G-065` "AS_OF(2026-06-21)." · `G-066` "AS_OF(2026-06-20)."
- `G-067` "CHANGE(095 FR-013, 2026-08-14)."
- `G-068` "CHANGE(004 FR-006, pre.27, pre.28): 'Between <tag> and <tag>'."
- `G-069` "CHANGE(042 FR-002, 2026-07-10, 2026-07-12)."
- `G-070` "CHANGE(003 FR-002, 2026-08-01, pre.28): mixed date and tag endpoints."
- `G-071` "CHANGE(105 FR-010, pre.25, pre.26)."
- `G-072` "AS_KNOWN(2026-07-01): 'As recorded on' + date."
- `G-073` "AS_KNOWN(2026-08-15): 'As ... stood on' + date. Not literally 'as recorded on'; record whether the router maps it."
- `G-074` "AS_KNOWN(2026-08-16)."
- `G-075` "AS_KNOWN(2026-09-11): 'As the archive stood on' + date."
— all from [questions-time.jsonl](/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions-time.jsonl)

### The reconstructed design

**Narrow list** — lifecycle words that force HISTORY. Only two members are positively attested:
`withdrawn` (G-051, G-052) and `originally` (named "the narrow marker 'originally'" in G-056,
"Narrow-list marker present: 'originally'" in G-061). G-051 calls it "the narrow **lifecycle**
list", which — read against `Lifecycle` in Java — implies the plausible full membership
`withdrawn`, `superseded`, `rejected`, `retired` (the four words the README uses for the history
stratum) plus `originally`. That extension is an inference, not a citation.

**Explicitly *not* on the narrow list**, each named as a near-miss: `set aside` (G-054),
`shelved`, `twice drew up` (G-055), `original`, `Before the Node rewrite` (G-056), `replaced`,
`first settle on … before that choice was replaced` (G-057), `ever consider`, `didn't go ahead`
(G-058). G-056 is the sharpest: `original` is "close to the narrow marker 'originally' but is not
it", so the list is matched as exact tokens, not stems.

**Broad list** — words that sound historical but must route CURRENT. Attested members: `changed`
(G-059) and `no longer` (G-060). The README names the same two: decoys "Sound historical ('no
longer', 'originally') but the answer is current".

**AS_OF rule**, quoted in G-064: *'as of / in / at' + date*. Also catches `as of` + release tag
(G-063). Does **not** list `On <date>`.

**CHANGE rule**: an element plus two endpoints. Endpoints may be two dates (G-067, G-069), two
tags (G-068 pre.27/pre.28, G-071 pre.25/pre.26), or mixed (G-070 date + pre.28). G-068 names the
surface form `'Between <tag> and <tag>'`.

**AS_KNOWN rule**: `'as recorded on' + date` (G-072). G-073 (`As … stood on`) and G-075 (`As the
archive stood on`) are variants the rule does not literally cover.

**Expected misroutes, as the file itself predicts them:**

| question | intended route | predicted actual | why |
|---|---|---|---|
| G-054 | HISTORY | CURRENT ("a known miss") | `set aside` not a marker |
| G-055 | HISTORY | CURRENT ("likely a router miss") | `shelved`, `twice drew up` not markers; "kept because it is how a person asks" |
| G-056 | HISTORY | CURRENT ("likely router miss") | `original` ≠ `originally` |
| G-057 | HISTORY | CURRENT ("likely router miss") | `replaced` not on the list |
| G-058 | HISTORY | CURRENT ("likely router miss") | `ever consider`, `didn't go ahead` not markers |
| G-061 | CURRENT (decoy) | HISTORY ("expected misroute") | decoy carries the narrow marker `originally` |
| G-064 | AS_OF | unknown — "record whether the router catches it" | `On <date>` not in the rule |
| G-073 | AS_KNOWN | unknown — "record whether the router maps it" | `As … stood on` not literal |

So: 2 of 7 history questions are expected to route correctly (G-051, G-052), 5 to misroute to
CURRENT; 2 of 3 decoys to hold (G-059, G-060), 1 to misroute to HISTORY; 2 of 9 date-bearing
questions are flagged as open (G-064, G-073). The symmetry is deliberate: G-056/G-061 form a
matched pair in which the near-miss word and the real marker are on opposite sides of the correct
answer.

### Stale / current judgement

**Current as a design document; unmeasured.** The annotations are part of the frozen v2 set
(`5d87cc0`, 20 Oct) and no router exists in code to contradict them (see Q5), so there is nothing
to go stale. They are predictions, not results.

### Gaps

- **I could not find "the report"** that the annotations cite for the rule list. It is referenced
  three times (G-061, G-064, and implicitly in G-051's "narrow lifecycle list") but no file in the
  repository, and no vault note under `03 Resources`, states the narrow or broad list in full. The
  memberships above are therefore reconstructed from the annotations alone; the full narrow list
  is my inference. The report-writer should flag this as a missing source, not report the lists as
  authoritative.
- No `router` field, routing code, intent classifier or route enum exists anywhere in
  `src/main/java` or `tools/ratchet-proto/ratchet/`. The entire routing design is annotation-only.

---

## Q5. Typed links: what the prototype parses, what Java has, what Episode 6 must extend

### Takeaway

The **Python prototype has a full typed-link graph and a supersession map; the Java harness has
neither.** Java has supersession (one-directional, forward) and lifecycle, but no `validates`,
`cites`, `defer-to` or `target` edges, no graph, no expansion step and no ordering. Episode 6
extends Java from a filtered ranked list into a graph, and extends `Successors` from "what
replaced this" to "what did this replace, and in what order".

### Cited findings — the prototype (`ratchet/corpus.py` @ `episode-4-prototype`)

- Module docstring: "**Links** for Episode 5: typed attribute links (validates, cites, target,
  defer-to), hrefs, and a supersession map (stale element -> the current one that replaced it)."
  Rules are "the earlier prototype's (`units.py`, `graph.py` in the 'Graph traversal before
  retrieval' notes), rebuilt on the standard-library parser."
- `_links()` — "Port of graph.py: resolved typed links, hrefs snapped to the nearest unit,
  supersession." It produces `c.edges` with these edge kinds:
  - `href:same-page`, `href:same-spec`, `href:cross-spec` — every relative `<a href>` whose target
    page is in the corpus, snapped via `node_of()` to the nearest id'd ancestor unit, else the page.
  - `typed:delta` — `<spec-delta target="…">`; on a `/changes/` page the target is resolved against
    `<specdir>/spec.html` with preference order `(spec.html, design.html, tasks.html)`, otherwise
    against the same page with preference `("same", "spec.html", "tasks.html", "design.html",
    "verify.html")`. Multiple targets split on `[\s,]+`.
  - `typed:validates`, `typed:cites` — attribute value resolved as an id (`lstrip('#')`).
  - `typed:defer-to` — resolved **only to a page**, and only when the value matches `^\d{3,}-` and
    `specs/<value>/spec.html` exists. So defer-to never reaches an element.
- Three distinct supersession sources populate `c.supersede` / `c.page_supersede`:
  1. **Prose**: an `<a>` whose preceding 40 characters of context contain both "superseded" and
     "by"; element→element when the source unit is non-current, else page→page when the page
     carries `<spec-status value="superseded">` and the source is not under `/changes/`.
  2. **The delta rule**: `typed:delta` where `lc[src] != "current"` **and** `lc[dst] == "current"`.
     This is exactly the rule the G-043 note indicts.
  3. **In-place status**: an id'd element with `data-status="superseded"` or a child
     `<spec-status value="superseded">`, taking the first `<a href>` after the status element
     (`_find_next`) as its successor.
- Volumes, from the plan: "about 2,100 same-page, 7,000 same-spec and 1,800 cross-spec links,
  recorded but not followed in the main line." — "RAGs to Riches, Series Plan.md" (§"Episode 5:
  relationships", which after the 20 Oct swap is Episode 6's plan)

### Cited findings — the Java side (branch `episode-5`)

- `search/Successors.java` — a port, deliberately technology-free: "What replaced what, so a search
  can follow a paper trail instead of answering from the past. Two questions, because an archive
  can replace a record or a whole page: which record now says what this one used to, and which
  page now stands where this one did." Methods: `Optional<String> successor(String address)`,
  `Optional<String> pageSuccessor(String source)`, plus `Successors.none()`. Note it is
  **forward-only** — there is no `predecessors()` and no ordering.
  — [Successors.java](/Users/briancorbin/Code/rags/rags-spring-ai/src/main/java/xyz/briancorbin/rags/search/Successors.java)
- `search/SupersessionRedirect.java` — a `DocumentPostProcessor`. `HOPS = 3`. Reads
  `superseded_by` / `page_superseded_by` from unit metadata; on a superseded page prefers the same
  anchor on the new page, else `bestOnPage` = BM25 rank over that page's units that are not
  `outOfDate()`. Dedupes into a `LinkedHashMap` by document id — "Two stale results can share one
  successor, so the list can come back shorter than it went in." Records provenance on the
  survivor: `copy.getMetadata().put("redirected_from", from)`. Javadoc states the post-`d7a1be4`
  rule: "Whatever was superseded is then dropped, applied changes included… nothing on those
  proposals says which rewrite is the live one, so keeping them puts three versions of the same
  rule in front of a reader, which was measured."
- `spectastic/Lifecycle.java` — five states `CURRENT, HISTORY, WITHDRAWN, SUPERSEDED, REJECTED`;
  `OUT_OF_DATE = {WITHDRAWN, SUPERSEDED, REJECTED}`; `IN_FORCE = complementOf(OUT_OF_DATE)`;
  `ALL`. `precedence()` = CURRENT 0, HISTORY 1, else 2. `read(source, el)` returns WITHDRAWN for a
  `/changes/withdrawn/` path, otherwise walks ancestors reading `data-status` then `status` ("the
  nearest status wins", and *any* excluded status anywhere in the ancestry excludes), otherwise
  HISTORY for `/changes/archive/` and CURRENT. Explicit caveat in the Javadoc: "`IN_FORCE` is not
  the same idea as the `stale@4` the ratchet counts… four of the nine it names are current or
  applied history. A record can be in force and still be the wrong thing to hand a reader."
- `units/Standing.java` + `units/StandingFilter.java` — the generic port behind Lifecycle.
  `Standing` is `{label(), outOfDate(), precedence()}`. `StandingFilter(Set<? extends Standing>
  allowed, boolean everything)` with `all()`, `of(...)`, `allows(...)`, `allowsNothing()`;
  "'all' is a state of this filter rather than a set, and the search can be told to look at
  everything without ever naming a single standing."
- `spectastic/Supersession.java` — the adapter implementing `Successors`: "The paper trail: which
  current element replaced each out-of-date one", from (a) `<spec-delta target="FR-003">` on an
  archived or withdrawn copy, (b) in-place status, (c) prose. `deltas()` walks
  `spec-delta` elements, `rewrittenBy()` finds the id'd element inside the delta (else the
  enclosing unit), and the comment at line 150 states the restriction: "Only an out-of-date unit is
  replaced, and only by one that is current."
- `search/PostProcessors.java` — a composite `DocumentPostProcessor`, "Spring AI's
  `DocumentPostProcessor` is a single step, and its advisor takes a list, but there is no composite
  that makes several of them one." This is the seam an expansion step would plug into.
- **What Java does not have:** a grep for `validates|defer-to|cites|neighbour|neighbor|expansion|
  anchored` across `src/main/java` and `src/test/java` matches only
  `answer/Quote.java`, `answer/CitationChecker.java` and `answer/CitationCheckerTest.java` — all
  about quote citation, none about typed links. There is **no link table, no graph, no
  anchored-expansion post-processor and no RRF-neighbour step in Java**.

### What Episode 6 would extend

- **`Successors` needs a second direction and an order.** Today: `successor(address)`. The G-043
  fix needs "group a spec's applied changes by the requirement their delta targets, order them by
  the spec's own changelog, and only the last one produced the current text" — that is
  `predecessorsInOrder(address)`, which `Successors` cannot express. The port's own framing
  ("How that is discovered is the archive's business") means the changelog parsing belongs in
  `spectastic/Supersession.java`, not in the port.
- **`Lifecycle` needs the missing category.** The epilogue names it: "That is a category we never
  made: *superseded applied change*. We had `history` for 'this is how the text got here' and used
  it for 'this change happened', and those are different things the moment a requirement is
  rewritten twice." `Lifecycle.OUT_OF_DATE`/`IN_FORCE` are written so "adding a sixth lifecycle
  puts it on one side only" — the extension point is deliberate.
- **`StandingFilter`/`Standing` need nothing structural** for lifecycle, but a bitemporal filter
  (as-of / as-known) is a *different* axis from standing and has no port at all.
- **A typed-link reader and an anchored-expansion post-processor are new Java work**, ported from
  `corpus.py::_links` and `pipeline.py`'s expansion, slotting into `PostProcessors`.
- Spring AI wiring already decided in the plan: "step 2 is a `DocumentPostProcessor` that reads the
  anchor recorded by the retriever. Post-processors receive the original query, not a rewritten
  one, so the anchor travels in document metadata. Step 1 is the last post-processor."
  — Series Plan, §"Episode 5: relationships"
- The plan's own honest-scope paragraph, which Episode 6 inherits: "Typed links fix one kind of
  question: those where spectastic links the named element to the answer. That's two questions in
  the current set, so the held-out relation questions decide whether the result stands. Links
  across specs reach the right page, not the right element. G-033's relationship is only in prose.
  Paraphrase questions are untouched." — same

### Stale / current judgement

- The prototype's link parsing is **current** — `corpus.py` was not touched by `d7a1be4` (which
  changed only README, pipeline.py and run.py).
- The Java `SupersessionRedirect` Javadoc and code are **current** (post-`d7a1be4`, the
  `Lifecycle.HISTORY` branch removed).
- The Series Plan's §"Episode 5: relationships" step table is **stale in two ways**: it is numbered
  Episode 5 (the swap on 20 Oct moved it to Episode 6, and the plan says so at line 110: "Episodes
  5 and 6 swapped (20 Oct)… The prototype's config names (E5-1, E5-2) still say Episode 5"), and
  its step-1 description — "For applied history, keep the archived copy behind it rather than
  replacing it" with "Hit@4 32 → 34; stale stratum 6/8 → 8/8; stale in top four 1 → 0" — is the
  **pre-`d7a1be4` rule on the pre-v2, /44-era keyword prototype**. Step 2's "All-evidence@4
  26 → 30 with step 1" likewise predates v2; the v2 measurement is 26 → 28 (X1 → X2).
- The plan's step-2 wrong-anchor figure "17/20, against 19/20 when the anchor is right" and the
  epilogue figures "30/44", "19/20 falls to 8/20" are all **old keyword-prototype, /44 numbers**;
  none has a v2 equivalent.

### Gaps

- `docs/design-history.md` exists in the repo and may record further Java-side intent; I did not
  read it.
- I did not verify whether `SpectasticReader` emits any link metadata at all (only that nothing
  downstream reads typed links).

---

## Q6. What the Episode 5 article's epilogue commits Episode 6 to, in its own words

### Takeaway

The epilogue commits Episode 6 to **ordering**: grouping a spec's applied changes by the
requirement their `spec-delta` targets, ordering them by the spec's own changelog, and keeping only
the last. It names the cost of not doing so (losing the byte-identical provenance passage) and
states that dates alone cannot do it.

### Cited findings — quotations

From §"Epilogue: three versions of one rule", subsection **"What would distinguish it"**
— ["RAGs to Riches with Spring AI, Episode 5, Bigger Models.md"](/Users/briancorbin/Documents/Brain/Library/03%20Resources/RAGs%20to%20Riches%20with%20Spring%20AI,%20Episode%205,%20Bigger%20Models.md):

> Order. Both halves are already in the corpus and neither needs a model

> Group a spec's applied changes by the requirement their delta targets, order them by the spec's
> own changelog, and only the last one produced the current text. The dates will not do it alone —
> two of these three applied on the same day. It is the sequence that settles it, and asking a
> corpus "what came after what" is the one thing this search still cannot do. **Episode 6.**

The two corpus signals it quotes as already present:

> ```html
> <spec-delta op="modified" target="FR-013">        <!-- which requirement this change rewrote -->
> ```
> ```text
> 14 Aug 2026  Applied 2026-08-13-address-a-screen: ...
> 14 Aug 2026  Applied 2026-08-14-use-the-anchor: Correction from an independent adversarial pass...
> 14 Aug 2026  Applied 2026-08-14-screen-as-a-kind: FR-013 is restated on the mechanism the grammar
>              actually composes...
> ```

What the current rule gives up, from the same section:

> It is not free, though, and the thing it gives up is the one worth naming.
> `2026-08-14-screen-as-a-kind`'s FR-013 is byte-for-byte the current FR-013 — it *is* the
> provenance of the text, the most useful archived passage in that spec, and it goes with the rest,
> because nothing distinguishes it.

The missing category, from §"Epilogue" proper:

> That is not a conflict between two defensible rules. It is a category we never made: *superseded
> applied change*. We had `history` for "this is how the text got here" and used it for "this
> change happened", and those are different things the moment a requirement is rewritten twice.

The ratchet position, from §"What I did not do":

> So the bar stayed and the design changed, stated openly, with every earlier step re-measured
> under the new rule. The ratchet refused a good step and was right to. It just turned out the
> thing that needed fixing was behind it.

From §"Next: links and history":

> Next: links and history, where the archive stops being a bag of passages and starts being a graph
> — and where the ordering this episode's epilogue needed finally gets written.

And the scope note in the G-043 vault note, §"Still worth doing, in Episode 6":

> Option 1 drops `screen-as-a-kind` too — the genuine provenance of the current text,
> byte-identical to it. The rule above would keep that one and drop only the two it superseded. It
> is cheaper than I first said, since both signals are already in the corpus, but it is the ordering
> work Episode 6 is for, and Episode 5's numbers do not need it.

The series plan's own Episode 6 row:

> | 6 | Links and history | Anchored typed expansion (X2 on the BM25 line: Hit@4 33/47,
> all-evidence@4 26 → 28; on the 5090 line E5-2 reaches 41/41 on the shared questions); following
> supersession links backwards; query routing for the history stratum (7 questions plus 3 decoys) |
> Rule 8 applied to the past on purpose |

— "RAGs to Riches, Series Plan.md", §"Episode map (tentative numbering)". The plan also records
"History questions | Query routing, intent classification | Episode 6" in the shared-vocabulary
table, and Rule 8 ("Answer in the right tense") as "Episode 6 answers about the past on purpose,
and the time episode by date". The as-of/what-changed/as-known questions are assigned to a **later
"Time" episode**, not Episode 6: "| later | Time | As-of, what-changed and as-known questions;
'now' frozen at 2026-09-14 (UTC days) | Rule 8 extended to dates |".

### Inferences

- Episode 6's committed scope is therefore three things, in the plan's order: anchored typed
  expansion, supersession followed **backwards** (ordering), and query routing for the history
  stratum. The bitemporal strata of `questions-time.jsonl` (as-of, what-changed, as-known — 14 of
  the 24) belong to the later Time episode; only history (7) and history-decoy (3) are Episode 6's.
  That matters for how much of the time set Episode 6 is accountable for.
- The epilogue's ordering rule would, if implemented, re-introduce exactly one passage per rewritten
  requirement (the last applied change) — which means the ratchet measure most likely to move is
  **all-evidence@4** (and `stale@4` must not rise), not Hit@4.

### Stale / current judgement

- The epilogue quotations are **current**: the Episode 5 article's mtime is 2026-10-24 19:50, after
  `d7a1be4` and after the round-3 audit, and its epilogue table ("keeping" vs "dropping": nomic
  35/47·32·1 → 35/47·32·0; qwen-bge 36/47·33·1 → 36/47·33·0; qwen-rr 44/47·40·1 → 44/47·40·0)
  matches the post-audit `*-drop-superseded.md` files.
- **One internal inconsistency to flag:** the article's epilogue table gives nomic all-evidence
  **32** and qwen-bge **33**, which are the *post*-audit figures (`4e45d50`); the vault note
  "What Is Left to Run on the 5090.md" gives **31** and **32** in its first table, which are the
  *pre*-audit figures (`6615f99`/`a71891d`). Both are internally correct for their date; a report
  must not mix them.
- The series plan's Episode 6 row's "X2 … Hit@4 33/47, all-evidence@4 26 → 28" is **stale**: those
  are the `6615f99` keeping-rule, pre-audit X2 numbers (see Q1).

### Gaps

- "E5-2 reaches 41/41 on the shared questions" in the Episode 6 plan row — denominator unlocatable;
  see Q1 Gaps.
