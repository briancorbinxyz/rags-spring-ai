# Headroom for a graph or multi-hop mechanism, from saved retrieval runs only

**Scope and method.** No retrieval, embedding or model stage was run. Every figure below is read
out of a `.jsonl` already committed on `origin/episode-4-prototype` under
`tools/ratchet-proto/results/`, scored against the frozen `expected`/`accept` fields in
`src/main/resources/golden/`. No passage was judged for relevance. Sources are repo paths at a
git ref, not URLs — this corpus has no URLs, so `<ref>:<path>` is the citation form.

**Tier.** Every figure in these notes is the **5090 tier** (llama.cpp Qwen3-Embedding-8B-Q8_0 +
Qwen3-Reranker-8B-Q8_0). No laptop-tier number is mixed into any column anywhere in this file.
The laptop floor is only available for the held-out set and only at pre-Episode-6 configs — see
the tier section under Key Question 1.

**Semantics of the fields used.** From `<ref>:tools/ratchet-proto/ratchet/score.py:60-130`:
`ranks[i]` is the first rank (1-based) at which expected address *i* is fully covered, by itself
or by an audit-accepted stand-in; `None` means it was not covered anywhere in the returned list.
`Pipeline.run` returns `[:50]` (`<ref>:tools/ratchet-proto/ratchet/pipeline.py:917`), so
**`None` means "absent from the top 50"**, not "absent from the corpus". `hit@4` is any expected
address at rank ≤ 4; `all_at(4)` ("all-evidence@4") is *every* expected address at rank ≤ 4.

Throughout, `<ref>` = `origin/episode-4-prototype`.

---

## Key Question 1 — Which configurations are the current main line, which saved runs hold them, and which runs carry stale qrels or the old supersession rule

### Takeaway

`E5-2` (Episode 5's published line), `G9` and `GA` (Episode 6's surviving arms) are all present at
the 5090 tier, but **only in the late run families**: `…-sections` (main), `…-heldout-e6`
(held-out), `…-time-e6` (time) and `5090-routing` (routing). The big main-set file
`5090-qwen3-emb-8b-qwen3-rr.jsonl` and the whole `*-drop-superseded.*` family, plus
`…-qwen3-rr-heldout.*`, are **not current** — the first on qrels, the others on the supersession
rule.

### Cited Findings

**Where the current arms live (all 5090 tier):**

| config | set | run file (all under `<ref>:tools/ratchet-proto/results/`) | date | rows |
|---|---|---|---|---|
| `E5-2`, `G9`, `GA` | main (53) | `5090-qwen3-emb-8b-qwen3-rr-sections.jsonl` | 2026-10-26 23:26 | 53 each |
| `E5-2`, `G9`, `GA` | held-out (13) | `5090-qwen3-emb-8b-qwen3-rr-heldout-e6.jsonl` | 2026-10-27 00:57 | 13 each |
| `E5-2`, `G5`, `G7`, `G9`, `GA` | time (24) | `5090-qwen3-emb-8b-qwen3-rr-time-e6.jsonl` | 2026-10-27 00:56 | 24 each |
| `E5-2`, `G9`, `GA` (+ `S1`–`S5`, `A`–`C`) | routing (15) | `5090-routing.jsonl` | 2026-10-28 12:12 | 15 each |
| `T5`, `TD`, `TF`, `TK` | time slice (14 cutoff) | `5090-slice-moved.jsonl`, `5090-slice-context.jsonl`, `5090-slice-changed-arms.jsonl` | 2026-10-28 12:44 | 14 each |
| `E5-2`, `G9`, `GA` | main, sections variant | `5090-qwen3-emb-8b-qwen3-rr-scope-pin.jsonl` holds `E5-2`, `G2`, `G4` only | 2026-10-26 | 53 each |

— run headers in each `.md` sibling; config inventories counted from the `.jsonl` files.

**The combined-105 precedence (reproduced exactly).** `<ref>:…/5090-combined-105.md` scores 105
questions from: `5090-slice-moved/TK` for the 14 cutoff questions, `…-time-e6/GA` for the other 10
time questions, `…-sections/GA` for the 53 main, `…-heldout-e6/GA` for the 13 held-out, and
`5090-routing/GA` for the 15 routing. Re-deriving it from the raw `.jsonl` files gives **105
questions, 98 answerable, Hit@4 83/98, all-evidence@4 75** — byte-identical to the published
headline, which confirms the precedence table is the right reading of the artefacts.

**Current-arm headline per set, 5090 tier** (derived from the files above; matches the published
`.md` tables):

| set | n answerable | `E5-2` Hit@4 / all@4 | `G9` Hit@4 / all@4 | `GA` Hit@4 / all@4 |
|---|---|---|---|---|
| main | 47 | 46 / 42 | 46 / 42 | **47 / 43** |
| held-out | 13 | 6 / 3 | 7 / 5 | **7 / 5** |
| time (snapshot path) | 23 | 11 / 10 | 15 / 13 | **15 / 13** |
| routing | 15 | — (14/15 at `GA`) | — | **14 / 14** |

— `…-sections.md`, `…-heldout-e6.md`, `…-time-e6.md`, `5090-routing.md`, and recomputed from the
`.jsonl`s. `GA` is the single best current config on every set, so "best current config" below
always means `GA` (or `TK` on the 14 cutoff questions, per the combined-105 precedence).

**Runs that carry PRE-AUDIT qrels — NOT CURRENT:**

- Round 3 of the blind audit landed in commit `ac6e6ba` / `70e6920`, **2026-10-24 19:38**. It
  judged 54 candidates: 46 "no", 7 "partly", **1 "whole"** —
  `specs/031-init-tools/spec.html#scenarios`, accepted whole for **G-076**
  (`results/episode-5/golden-audit-round3.jsonl`).
- The golden set is frozen, so round 3 was *not* written back into `questions.jsonl`
  (`git log` on `src/main/resources/golden/questions.jsonl` ends at 2026-10-20 18:09; the file is
  identical on `episode-5` and `origin/episode-4-prototype`). Instead `run.py` grew `--audit`,
  which folds each round's acceptances into `accept` at scoring time (commit message, `70e6920`).
- Therefore **`5090-qwen3-emb-8b-qwen3-rr.jsonl/.md` (2026-10-23 00:52)**, and every other main-set
  run dated before 2026-10-24 19:38, was scored without round 3.
- **Measured effect, exactly one question:** `E5-2` on G-076 is `ranks [7, None, 1]` in the
  pre-audit run and `[5, 5, 1]` in `…-sections`. The published `E5-2` row moves all@10 43 → 44
  accordingly (`5090-qwen3-emb-8b-qwen3-rr.md` vs `…-sections.md`).
- `qrels.txt` itself went from 291 to 345 judgments at that commit, all of which change `Judged@4`
  and nothing in Hit@4 or all-evidence@4 (README, `src/main/resources/golden/README.md`, and the
  `ac6e6ba` message: "What it moved: nothing anyone had quoted").

**Runs that carry the OLD SUPERSESSION RULE — NOT CURRENT:**

- The same config key `E5-1` is labelled differently across families:
  "E5-1 + supersession redirect **(history kept behind)**" in `5090-qwen3-emb-8b-qwen3-rr.md`, but
  "**(superseded history dropped)**" in `5090-qwen3-emb-8b-qwen3-rr-drop-superseded.md` and in
  `5090-qwen3-emb-8b-qwen3-rr-heldout.md`. The label is set by the run's corpus flag.
- So the **`*-drop-superseded.*` family (9 run files each for nomic / qwen3-emb / qwen3-emb+rr)**
  and **`5090-qwen3-emb-8b-qwen3-rr-heldout.*` (2026-10-24 20:14)** hold the dropped-superseded
  corpus. `E5-1b` / `X1b` are explicitly "Episode 4's first rule"
  (`<ref>:tools/ratchet-proto/run.py:65`).
- The current held-out numbers are `…-heldout-e6` (2026-10-27), not `…-heldout`.

**A third staleness, beyond qrels:** `E5-2`'s *ranking* also differs between the 2026-10-23 main
run and the 2026-10-26 `…-sections` run on 7 questions (G-006, G-010, G-019, G-038, G-042, G-043,
G-044 all have a different top-10), and G-043's `stale_ranks` move `[4, 3]` → `[None, None]`,
which is why the published `E5-2` stale@4 is 1 in the old file and 0 in the current one. Stale
ranks do not depend on `accept`, so this is a corpus/section change, not the audit. **The
2026-10-23 main-set run is stale on both counts.**

**Tier availability.** `<ref>:tools/ratchet-proto/README.md:199-200` says plainly: "The Mac results
are not in the repository". The only laptop-tier artefact committed anywhere is
`results/episode-5/heldout-laptop.{jsonl,md}` (Ollama `nomic-embed-text:v1.5` +
`ms-marco-MiniLM-L-6-v2`, 2026-10-24 19:58), and it holds only configs `S3`, `E5-1`, `X1`, `H1` —
**no `E5-2`, no `G9`, no `GA`**. Its held-out figures are `S3` 2/13 (all@4 0), `E5-1` 4/13 (all@4
1), `X1` 2/13, `H1` 2/13, with relation · history strata `E5-1` = 2·0·1 · 2·1·2.

### Inferences

- There is no laptop-tier figure for any current arm, so no laptop/5090 column can be built for
  this question at all. Every number in these notes is 5090 tier and must be labelled so.
- The pre-audit main-set run is still usable as *reachability* evidence (if an address ranked well
  there, it is reachable), because pre-audit scoring under-counts coverage and so is conservative
  in that direction. It is not usable for any headline figure.

### Gaps

- `5090-qwen3-emb-8b-qwen3-rr-scope-pin.jsonl` holds `E5-2`, `G2`, `G4` but not `G9`/`GA`, so the
  scope-pin arms cannot be compared to the current line on the same questions in one file.
- Whether the `…-sections` corpus change (not the audit) altered anything on the held-out or time
  sets cannot be checked, because no pre-sections run of those sets exists at `G9`/`GA`.

---

## Key Question 2 — Per-question outcomes at the best current config, for the relation stratum and every 2+-address question

### Takeaway

The candidate pool is **26 questions** (relation stratum ∪ 2-or-more expected addresses) out of
105. At `GA` (the best current config), **10 are fully right (all-evidence@4)**, **8 hit on one
address but never retrieve another**, and **8 miss outright**. Relation is the weakest stratum on
both sets: main-set relation is Hit@4 7/7 but all-evidence@4 only 4/7; held-out relation is Hit@4
4/8 and all-evidence@4 2/8.

### Cited Findings

**Pool definition.** Relation stratum: 7 in the main set (G-029…G-034, G-036), 8 in the held-out
set (H-001…H-007, H-013), 0 in the time and routing sets. Questions with 2+ expected addresses: 8
main (G-012, G-029…G-033, G-036, G-076 — G-076 has 3), 11 held-out (all except H-010, H-012), 5
time (G-051, G-052, G-055 with 4, G-058, G-059), 0 routing. Union = **26**
(`src/main/resources/golden/questions*.jsonl`).

**The full per-question table at `GA`, 5090 tier.** Run file per row is the combined-105 source:
main = `…-sections/GA`, held-out = `…-heldout-e6/GA`, time = `…-time-e6/GA`. Ranks are
`[rank of expected[0], rank of expected[1], …]`; `–` means absent from the top 50.

| id | set | stratum | link type | n | Hit@4 | all-ev@4 | all-ev@10 | ranks at `GA` |
|---|---|---|---|---|---|---|---|---|
| G-012 | main | paraphrase | — | 2 | yes | **yes** | yes | 2, 4 |
| G-029 | main | relation | — | 2 | yes | **yes** | yes | 2, 3 |
| G-030 | main | relation | — | 2 | yes | **yes** | yes | 4, 2 |
| G-031 | main | relation | — | 2 | yes | no | yes | 1, **6** |
| G-032 | main | relation | — | 2 | yes | **yes** | yes | 2, 1 |
| G-033 | main | relation | — | 2 | yes | no | no | 1, **17** |
| G-034 | main | relation | — | 1 | yes | **yes** | yes | 4 |
| G-036 | main | relation | — | 2 | yes | no | no | **16**, 4 |
| G-076 | main | single-fact | — | 3 | yes | no | yes | **5**, **5**, 1 |
| G-051 | time | history | — | 2 | **no** | no | no | **–**, 12 |
| G-052 | time | history | — | 2 | yes | no | no | 2, **29** |
| G-055 | time | history | — | 4 | **no** | no | no | **–**, 39, **–**, **–** |
| G-058 | time | history | — | 2 | **no** | no | no | **–**, **–** |
| G-059 | time | history-decoy | — | 2 | yes | no | no | 1, **11** |
| H-001 | held-out | relation | typed:validates (anchored) | 2 | yes | **yes** | yes | 2, 3 |
| H-002 | held-out | relation | typed:validates (unanchored) | 2 | yes | no | no | 3, **27** |
| H-003 | held-out | relation | typed:defer-to (to the page) | 2 | **no** | no | no | **–**, **–** |
| H-004 | held-out | relation | typed:defer-to + link to FR-013 | 2 | yes | **yes** | yes | 1, 4 |
| H-005 | held-out | relation | typed:validates (unanchored) | 2 | **no** | no | no | **–**, **–** |
| H-006 | held-out | relation | prose only | 2 | yes | no | no | 1, **–** |
| H-007 | held-out | relation | prose only | 2 | **no** | no | no | 5, **–** |
| H-008 | held-out | history | prose "superseded by" + link | 2 | yes | **yes** | yes | 1, 4 |
| H-009 | held-out | history | withdrawn proposal | 2 | **no** | no | no | 8, **–** |
| H-011 | held-out | history | applied proposal → current requirement | 2 | yes | **yes** | yes | 3, 2 |
| H-013 | held-out | relation | prose only | 2 | **no** | no | no | **–**, **–** |

Pool totals: 26 questions, 18 Hit@4, **10 all-evidence@4**, 12 all-evidence@10.

**Stratum roll-up at `GA`, 5090 tier** (`Hit@4 · all@4 · all@10`):

- Main set relation (7): **7 · 4 · 5** — `…-sections.md`, `GA` row.
- Held-out relation (8): **4 · 2 · 2**; held-out history (5): **3 · 3 · 3** — `…-heldout-e6.md`.
- Combined 105 relation (15, main 7 + held-out 8): **Hit@4 11/15**, groundless@4 4/15 —
  `5090-combined-105.md`.

**Where each current arm actually moved a pool question** (same three files, `E5-2` → `G9` → `GA`
columns, 5090 tier):

| id | `E5-2` | `G9` | `GA` | what moved |
|---|---|---|---|---|
| G-036 | hit [17, 4] | hit [16, 4] | hit [16, 4] | rank only, no verdict change |
| G-051 | MISS [–, 10] | MISS [–, 12] | MISS [–, 12] | hop 2 got *worse* |
| G-052 | MISS [–, 27] | hit [2, 33] | hit [2, 29] | `admit_history` won hop 1 |
| G-055 | MISS [–, –, –, –] | MISS [–, 39, –, –] | MISS [–, 39, –, –] | one address entered the top 50 |
| H-004 | hit [1, 5] | **ALL** [1, 4] | **ALL** [1, 4] | hop 2 crossed the depth-4 line |
| H-009 | MISS [–, –] | MISS [8, –] | MISS [8, –] | hop 1 entered the top 10 |

Every other pool question has the same verdict under all three arms. `GA`'s only *additional*
main-set win over `G9` is an `intent` question (intent stratum 4 → 5, Hit@4 46 → 47), not a
relation one (`…-sections.md`).

**The link mechanism already shipped.** `E5-2` *is* "E5-2 + anchored typed expansion" — a
link-following step. On the main set it won **+2 at Hit@4 (G-029, G-030)** over `E5-1` and took
relation from `5 · 2 · 3` to `7 · 4 · 5`
(`<ref>:…/5090-qwen3-emb-8b-qwen3-rr.md`, `E5-2` row and per-stratum table; **pre-audit run, not
current for headline purposes**, but the per-stratum delta is a ranking fact independent of the
G-076 acceptance). On the held-out set the same expansion leaves relation at `4 · 1 · 2`, and it
is `G9`'s arms, not the expansion, that lift it to `4 · 2 · 2` (`…-heldout-e6.md`).

### Inferences

- Anchoring is what makes typed expansion work. Both **anchored** typed-link questions are
  all-evidence@4 (H-001 `typed:validates`, H-004 `typed:defer-to + link to FR-013`); both
  **unanchored** `typed:validates` questions fail (H-002 hit-not-all, H-005 outright miss), and
  the unanchored `typed:defer-to (to the page)` H-003 misses outright. That is 2/2 vs 0/3 on the
  held-out set — small numbers, but the split lines up exactly with the `anchored` flag.
- `prose only` links are the worst case: H-006 hit-not-all, H-007 and H-013 outright misses, 0/3
  all-evidence. The shipped expansion has no edge to walk for these by construction.

### Gaps

- 15 of the 26 pool questions are held-out or time questions. The constraint is that these are
  scored once per episode; the numbers above are what the saved runs already contain, and no new
  score was generated. But it means the relation evidence is thinner than 26 suggests: only the 7
  main-set relation questions have been measured repeatedly.
- Strata of 4–8 questions mean one question is 12–25% of a stratum rate. `README.md` in the
  golden set says this explicitly for the time set ("compare these per question, not as rates").

---

## Key Question 3 — The Hit@4-to-all-evidence@4 gap: how many questions hit but are not all-evidence, and which

### Takeaway

Across the whole 105, **8 questions hit@4 but are not all-evidence@4**: G-031, G-033, G-036,
G-052, G-059, G-076, H-002, H-006. That is exactly the published 83 − 75. Six of the eight are
relation-shaped or history-link-shaped; one (G-076) is not a link question at all, and one
(G-059) is a decoy.

### Cited Findings

**The gap is the whole difference.** At the combined-105 configs, 98 answerable questions give
Hit@4 83 and all-evidence@4 75 (`5090-combined-105.md`, and recomputed from the `.jsonl` files).
**83 − 75 = 8**, so every question in the gap is enumerated below; there is no residue.

| id | set / stratum | run file + config (5090 tier) | ranks | the uncovered address | its rank |
|---|---|---|---|---|---|
| G-031 | main / relation | `…-sections` / `GA` | 1, 6 | `specs/058-corpus-licensing/spec.html#requirements` | 6 |
| G-033 | main / relation | `…-sections` / `GA` | 1, 17 | `specs/024-explore-restore/spec.html#FR-001` | 17 |
| G-036 | main / relation | `…-sections` / `GA` | 16, 4 | `specs/076-contract-export-handover/tasks.html#dependencies` | 16 |
| G-052 | time / history | `…-time-e6` / `GA` | 2, 29 | `specs/094-visual-sidecar-convention/spec.html#changelog` | 29 |
| G-059 | time / history-decoy | `…-time-e6` / `GA` | 1, 11 | `specs/093-design-visual-section/spec.html#FR-015` | 11 |
| G-076 | main / single-fact | `…-sections` / `GA` | 5, 5, 1 | `#FR-002` and `#FR-007` of `specs/031-init-tools/spec.html` | 5, 5 |
| H-002 | held-out / relation | `…-heldout-e6` / `GA` | 3, 27 | `specs/115-guardrail-verdict/spec.html#FR-007` | 27 |
| H-006 | held-out / relation | `…-heldout-e6` / `GA` | 1, – | `specs/013-core-propose/design.html#D-001` | absent >50 |

**Main-set share.** Four of the eight are main-set (G-031, G-033, G-036, G-076), which is exactly
why the main set reads Hit@4 47/47 but all-evidence@4 43/47 at `GA` (`…-sections.md`). **The main
set has zero Hit@4 headroom and 4 questions of all-evidence headroom.**

**G-076 is an audit artefact, not a retrieval one.** Round 3 accepted
`specs/031-init-tools/spec.html#scenarios` *whole* for G-076, and `#scenarios` sits at **rank 5**
in `GA`'s list. That single stand-in is why G-076's two uncovered FRs both read "rank 5": one
passage, one rank, covering both. Pre-audit the same `E5-2` run read `[7, None, 1]`
(`5090-qwen3-emb-8b-qwen3-rr.jsonl`). **G-076's shortfall is a depth-4-versus-5 problem on a single
passage and has nothing to do with links.**

**H-006 is the only gap question whose uncovered address is a genuine recall failure.** Its
`specs/013-core-propose/design.html#D-001` is `None` in **all 19** saved held-out rows across all
run families and all configs — it has never once entered a top 50. The other seven gap questions'
uncovered addresses are all inside the top 50 at `GA`.

### Inferences

- Of the 8, **6 are reordering problems and 2 are not.** The six (G-031, G-033, G-036, G-052,
  G-059, G-076) all have the uncovered address inside the top 50 *and* another passage from the
  same document already in the top 4 — see Key Question 4. H-002 is borderline (rank 27, same
  document in the top 4). Only H-006 has no anchor and no presence.
- Fixing all 8 cannot move Hit@4 by a single question, by definition — they already hit. The gap
  is purely an all-evidence@4 quantity: 75 → at most 83 out of 98.

### Gaps

- Whether the reader actually needs the second address to answer correctly is a question about the
  `core` field and the chat model, not about retrieval, and no saved artefact measures it. A
  question can be all-evidence-incomplete and still answerable in practice; nothing here settles
  that, and it is not judged in these notes.

---

## Key Question 4 — Where the missing address sits: reranking problem, or recall problem

### Takeaway

Of the 8 gap questions, **6 are reranking/depth problems** (uncovered address at rank 5–29, with a
sibling from the same document already in the top 4) and **1 is a pure recall problem** (H-006).
Of the 8 outright misses that are relation-shaped or multi-address, **5 have at least one address
that has never entered a top 50 in any saved run** — those are the only recall-shaped arguments
for expansion in the whole 105.

### Cited Findings

**Band distribution of every uncovered expected address, `GA`, 5090 tier** (counting addresses,
not questions, across the 16 incompletely-answered pool questions):

| band | count | addresses |
|---|---|---|
| rank 5–10 | 5 | G-076 ×2 (both rank 5), H-007 hop 1 (5), H-009 hop 1 (8), G-031 hop 2 (6) |
| rank 11–20 | 4 | G-059 (11), G-051 hop 2 (12), G-036 hop 1 (16), G-033 hop 2 (17) |
| rank 21–50 | 3 | H-002 hop 2 (27), G-052 hop 2 (29), G-055 hop 2 (39) |
| absent >50 | 13 | G-051 hop 1; G-055 hops 1, 3, 4; G-058 both; H-003 both; H-005 both; H-006 hop 2; H-007 hop 2; H-009 hop 2; H-013 both |

**Same-document and same-spec anchors in the top 4.** For each uncovered address I checked whether
the top 4 already holds a passage from the same `.html` file (same-document) or the same spec
directory (same-spec). An anchor is the thing a link-following or sibling-expansion step would
have to walk from.

| id | uncovered address | rank | same-DOC passage in top 4? | diagnosis |
|---|---|---|---|---|
| G-031 | 058/spec#requirements | 6 | **yes** — 058/spec#data, 058/spec#FR-001 | reranking; `#requirements` is the parent section of the `#FR-001` already at rank 3 |
| G-033 | 024/spec#FR-001 | 17 | **yes** — 024/spec#context, 024/spec#requirements | reranking |
| G-036 | 076/tasks#dependencies | 16 | **yes** — 076/tasks#T-211 | reranking; the uncovered unit *is* the dependencies block |
| G-052 | 094/spec#changelog | 29 | no (same spec yes; same doc only at rank 10 via `#US1`) | reranking at depth |
| G-059 | 093/spec#FR-015 | 11 | **yes** — 093/spec#FR-014 at rank 1 (its literal sibling) | reranking |
| G-076 | 031/spec#FR-002, #FR-007 | 5, 5 | **yes** — 031/spec#FR-006, #SC-006 | depth only (one accepted passage at rank 5) |
| H-002 | 115/spec#FR-007 | 27 | **yes** — 115/spec#SC-001 (rank 1), 115/spec#FR-004 | typed link, unanchored: SC-001 validates both FRs and is rank 1 |
| H-006 | 013/design#D-001 | – | **no**, and no 013 passage anywhere in the top 10 | **recall**; cross-document prose hop |
| H-003 | 053/spec#FR-001, #FR-002 | –, – | **yes** — 053/spec#context at rank 2 | **recall** with a same-doc anchor |
| H-005 | 118/spec#FR-003, #SC-002 | –, – | **yes** — 118/spec#scenarios, #SC-003 | **recall** with a same-doc anchor |
| H-007 | 012/design#D-006 (5); 011/design#D-002 (–) | 5, – | hop 1 same-spec yes, same-doc at rank 10; hop 2 **no 011 passage at all** | mixed: hop 1 reranking, hop 2 recall with no anchor |
| H-009 | …the-bundle-is-the-list/proposal#FR-002 | – | same doc present at rank 8 (`#intent`) | **recall** with a same-doc anchor one rank band away |
| H-013 | 008/design#D-005, 007/design#D-001 | –, – | no same-doc; same-spec 007 via 007/spec#FR-012 at rank 4 | **recall** with only a same-spec anchor |
| G-055 | 093/tasks#T-1100/1101/1200/1201 | –, 39, –, – | no `tasks.html` passage in the top 10; same spec yes | **recall** at document granularity |
| G-058 | …implement-to-build/proposal#risks, #changelog | –, – | no; a *different* 000-spectastic archive proposal is at rank 1 and 3 | **recall**, and policy-bound (see below) |
| G-051 | …what-init-writes/proposal#SC-001 (–); 003/spec#changelog (12) | –, 12 | hop 2 same-doc yes; hop 1 no | **filter/fusion**, see below |

**Reachability across every saved run** (does the address ever enter a top 50, under any config in
any committed run file, any embedder, 5090 tier — the pre-audit files included, which makes this
check conservative):

- **Reachable and in fact already top-4 somewhere:** G-031 hop 2 (rank 1 at `…-qwen3-emb-8b/EpiB`,
  top-4 in 56 saved rows), G-033 hop 2 (rank 3 at `5090-nomic/C`, top-4 in 32 rows), G-036 hop 1
  (rank 2 at `5090-nomic/F4`, top-4 in 41 rows), G-076 all three (top-4 in dozens of rows),
  G-051 hop 1 (**rank 1 at `…-time-e6/G7`**).
- **Reachable but never top-4:** G-052 hop 2 (best 6), G-059 hop 2 (best 6), H-002 hop 2 (best 27,
  identical across every config), H-007 hop 1 (best 5), H-009 hop 1 (best 8), G-051 hop 2 (best 9),
  G-055 hop 1 (best 49), hop 2 (best 39).
- **Never in any top 50, in any saved run:** H-006 hop 2, H-003 both, H-005 both, H-007 hop 2,
  H-009 hop 2, H-013 both, G-055 hops 3 and 4, G-058 both. **13 addresses across 8 questions.**

**Two cases that are policy, not retrieval.** `5090-episode6-ceiling.md` documents both:
G-051's hop 1 "`G7` has it at **1**; adding the scope arms (`scope_rrf`, `doc_scope`) moves it to
12. They are what close the main set, so the trade stands." And G-058 is "born withdrawn":
`admit_history` "*does* offer its units… and they still rank outside 50, so this is a ranking
problem. The `#risks` collection ranks it **1st**, one fitted trigger away." Also: "G-055 …
Reachable, then buried by the fusion. Every pin that unburies history costs the decoy G-061
(`history_pin`, rejected)."

### Inferences

- **The main set contains no recall-shaped argument for expansion whatsoever.** All four main-set
  incomplete questions (G-031, G-033, G-036, G-076) have the uncovered address inside the top 50
  *and* a same-document sibling already in the top 4. A wider cut, a sibling/parent rule, or a
  better reranker reaches all four; a graph adds nothing they need.
- **The recall-shaped argument lives entirely in the held-out and time sets**, and it is concentrated
  on `prose only` and `unanchored typed` links (H-003, H-005, H-006, H-007 hop 2, H-013) and on
  withdrawn/task-level history (G-055, G-058, H-009 hop 2).
- **H-003, H-005 and H-009 are the cleanest expansion candidates**: an uncovered address absent from
  every top 50, with a passage from the *same document* already at rank 2–8. A document- or
  section-level expansion from a retrieved anchor would reach them without a typed edge. H-006 and
  H-007 hop 2 are the hard cases — a true cross-document hop with no anchor in the top 10 — and
  both are `prose only`, so no typed edge exists to follow.
- G-051 and G-058 should be scored out of the graph case. Both are already reachable at rank 1
  under some saved arm or collection; what blocks them is a fusion trade and a filter policy that
  the author has explicitly reserved as a decision.

### Gaps

- "Absent from the top 50" is not "absent from the corpus". For the 13 never-seen addresses I
  cannot say, from saved artefacts, whether a first-stage recall change would surface them —
  determining that needs a run, which is out of scope.
- Several uncovered passages in the top-4 lists of these questions are flagged in
  `5090-episode6-ceiling.md` as **audit candidates** (`#changelog` for G-069/G-070, the archived
  proposal copy for G-067, `#risks` for G-058). None is judged here; that is the author's.

---

## Key Question 5 — Outright misses, and how many are relation-shaped or multi-address

### Takeaway

At the combined-105 configs the best current line misses **15 of 98 answerable questions
outright** (no expected address in the top 4). **8 of those 15 are relation-shaped or
multi-address**; the other 7 are single-address `what-changed`, `as-known`, `history` and
`intent-noword` questions that a graph mechanism has no claim on.

### Cited Findings

98 answerable − 83 Hit@4 = **15 outright misses**, enumerated (run file + config per the
combined-105 precedence; all 5090 tier):

| id | set / stratum | run / config | ranks | relation or multi-address? |
|---|---|---|---|---|
| G-051 | time / history | `…-time-e6` / `GA` | –, 12 | **yes** (2 addresses) |
| G-055 | time / history | `…-time-e6` / `GA` | –, 39, –, – | **yes** (4 addresses) |
| G-058 | time / history | `…-time-e6` / `GA` | –, – | **yes** (2 addresses) |
| H-003 | held-out / relation | `…-heldout-e6` / `GA` | –, – | **yes** |
| H-005 | held-out / relation | `…-heldout-e6` / `GA` | –, – | **yes** |
| H-007 | held-out / relation | `…-heldout-e6` / `GA` | 5, – | **yes** |
| H-009 | held-out / history | `…-heldout-e6` / `GA` | 8, – | **yes** (2 addresses) |
| H-013 | held-out / relation | `…-heldout-e6` / `GA` | –, – | **yes** |
| G-067 | time / what-changed | `5090-slice-moved` / `TK` | 14 | no (1 address) |
| G-069 | time / what-changed | `5090-slice-moved` / `TK` | 10 | no |
| G-070 | time / what-changed | `5090-slice-moved` / `TK` | – | no |
| G-071 | time / what-changed | `5090-slice-moved` / `TK` | 5 | no |
| G-074 | time / as-known | `5090-slice-moved` / `TK` | 5 | no |
| H-010 | held-out / history | `…-heldout-e6` / `GA` | – | no (1 address, withdrawn proposal) |
| R-004 | routing / intent-noword | `5090-routing` / `GA` | – | no |

**Breakdown: 8 relation-shaped or multi-address, 7 not.** By set: 5 held-out (H-003, H-005, H-007,
H-009, H-013), 3 time (G-051, G-055, G-058), **0 main**. The main set at `GA` misses no answerable
question (47/47, `…-sections.md`).

**The 7 non-relation misses already have documented, non-graph explanations** in
`5090-episode6-ceiling.md`: G-067's uncovered unit is an unjudged audit candidate at rank 2;
G-069 and G-070 "name no spec, so the scope arms never fire"; G-071 is at rank 3 under `TD`, whose
router fails the main set 33 → 31; G-074 is one half of a pair with G-073 where "under every
measured configuration, one of the two is outside the cut". R-004 and H-010 have no such note.

**Groundless@4 cross-check.** `5090-combined-105.md` reports groundless@4 22/105 = 15 missed +
7 unanswerable, consistent with the 15 above. Relation's groundless@4 is 4/15 and history's 5/12.

### Inferences

- Every Hit@4 point a graph mechanism could possibly win sits in the **reserve sets** (held-out and
  time), not in the published main line. That matters for how a headroom claim can be stated: the
  published 47/47 figure is already saturated, so a graph arm cannot be justified by main-set Hit@4
  at all, and a main-set-driven ratchet would score it as inert.
- Of the 8 relation/multi misses, H-007 and H-009 already retrieve hop 1 inside the top 10 (ranks 5
  and 8) — they are "one depth step plus one hop" away. H-003, H-005 and H-013 retrieve neither
  expected address, so a mechanism would have to work from a non-expected anchor.

### Gaps

- H-010 and R-004 have no diagnosis in any saved `.md`, and their expected addresses are absent
  from the top 50 — I can only report that, not explain it.

---

## Key Question 6 — The net ceiling: the maximum number of questions out of 105 a graph or multi-hop mechanism could possibly convert

### Takeaway

**At most 16 of 105.** That is a ceiling, not a prediction: it is simply the count of every
relation-shaped or multi-address question the best current configuration does not already get
fully right, with no claim that any mechanism would convert any of them. The honest, mechanism-
shaped subset is much smaller: **5 questions** where the uncovered address has never entered a top
50 and a graph hop is the kind of thing that could reach it.

### Cited Findings

**The ceiling, built from the two disjoint lists above:**

| band | n | ids | metric it could move |
|---|---|---|---|
| hit@4 but not all-evidence@4 | 8 | G-031, G-033, G-036, G-052, G-059, G-076, H-002, H-006 | all-evidence@4 only — **cannot move Hit@4** |
| outright miss, relation-shaped or multi-address | 8 | G-051, G-055, G-058, H-003, H-005, H-007, H-009, H-013 | Hit@4 **and** all-evidence@4 |
| **ceiling** | **16 / 105** | | |

**What converting all 16 would do, at best** (from the combined-105 base of 98 answerable,
Hit@4 83, all-evidence@4 75, 5090 tier):

- Hit@4: **83/98 → at most 91/98 (93%)**, from the 8 outright misses only.
- all-evidence@4: **75 → at most 91**.
- Main set: Hit@4 **47/47 → 47/47 (zero headroom)**; all-evidence@4 **43/47 → at most 47/47**
  (G-031, G-033, G-036, G-076).
- By set, the 16 split: **4 main**, **7 held-out**, **5 time**, **0 routing**.

**Why 16 is a generous ceiling, and the narrower figures inside it.** Subtracting the cases where
the saved artefacts already name a non-graph cause:

| strike out | why | n left |
|---|---|---|
| start | | 16 |
| G-076 | not a link question; one audit-accepted passage at rank 5 covers all three addresses | 15 |
| G-059 | history-*decoy*; the uncovered FR-015 is the literal sibling of FR-014 at rank 1 | 14 |
| G-031, G-033, G-036 | uncovered address inside the top 50 with a same-document sibling already in the top 4; all three reach top-4 under other saved arms | 11 |
| G-051, G-058 | already rank 1 under `…-time-e6/G7` and under the `#risks` collection respectively; blocked by a fusion trade and a reserved filter-policy decision (`5090-episode6-ceiling.md`) | 9 |
| G-052, H-002, H-007 hop 1 | uncovered address reachable (ranks 29, 27, 5) with a same-document anchor in the top 4 — reranking or depth | 7 at most, 6 cleanly |
| **mechanism-shaped remainder** | uncovered address never in any top 50, and a hop is the plausible route: **H-003, H-005, H-006, H-009, H-013** (+ G-055 at document granularity, + H-007 hop 2) | **5–7** |

So the defensible range is: **16 is the arithmetic ceiling; about 5 to 7 questions are the ones
where the saved runs actually show a link-shaped failure**, and of those, 5 are held-out relation
or history questions and 1–2 are time questions.

**What a link mechanism already bought, for calibration.** The shipped `E5-2` *is* an anchored
typed-expansion step. It won **2** main-set questions at Hit@4 (G-029, G-030) and took main-set
relation all@4 from 2 to 4 (`5090-qwen3-emb-8b-qwen3-rr.md`, pre-audit run, 5090 tier). On the
held-out set it won **0** at Hit@4 relative to `E5-1`'s relation stratum, and the held-out relation
all@4 gain of +1 came from `G9`'s arms, not the expansion (`…-heldout-e6.md`). The 2 it won were
both anchored; everything in the mechanism-shaped remainder is `prose only` or unanchored.

### Inferences

- **This is a ceiling, explicitly not a prediction.** Nothing in the saved artefacts shows any
  mechanism converting any of the 16. The number is a bound on what is even available to win: no
  mechanism can convert a question the pipeline already answers, and 89 of the 105 are either
  already fully right, unanswerable, or not relation-shaped.
- A graph arm cannot be ratcheted on the main set. The main set offers it 0 Hit@4 and 4
  all-evidence@4, and 3 of those 4 are reranking problems that a cheaper arm also fixes. Any
  honest case for it has to be argued on the held-out set, which `5090-episode6-ceiling.md` says
  the episode spends once — so the case would have to be made in advance, on the shape of the
  failures, not on a measured win.
- The failure shape that survives every subtraction is narrow and specific: **a second expected
  address, in a different document from the one the question names, linked only by prose, that no
  saved configuration has ever retrieved within 50.** That is 2 questions in the purest form
  (H-006, H-007 hop 2) and 5 with the same-document variant included. If a mechanism is built, that
  is the thing it has to do, and `anchored` typed expansion is already shown not to do it.

### Gaps

- The 16 is computed at `GA` / `TK`. A different "best" choice per question (cherry-picking the best
  saved arm for each) would give a smaller pool, but that configuration does not exist as a
  shippable line and would not be a fair base.
- Whether any of the 16 would actually convert is unmeasurable from saved artefacts, by
  construction. It needs a run, which needs the author's permission.
- The 13 never-retrieved addresses cannot be distinguished, from saved runs alone, between "a
  first-stage recall problem a graph would also not fix" and "reachable by one hop from something
  already retrieved". The same-document/same-spec anchor analysis in Key Question 4 is the closest
  proxy available, and it is a proxy, not a measurement.
