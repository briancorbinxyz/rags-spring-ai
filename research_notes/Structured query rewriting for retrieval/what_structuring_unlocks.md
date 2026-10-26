# What a question-structuring step would have to produce, and whether the question text contains it

Scope note on sourcing: this assignment is a measurement over local artefacts, not a literature
search. Every claim below is sourced to a file path or a git object in this repository, cited in
the same inline style the research template asks for. No web sources were consulted and none are
relevant. Where a number could not be measured without running the pipeline, it is in **Gaps**.

Provenance of the three evidence bases used throughout:

- Question text and expectations: `/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions.jsonl` (53), `.../questions-heldout.jsonl` (13), `.../questions-time.jsonl` (24).
- Per-question ranks, `pinned`, `top10`: `git show origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr-e6.jsonl` (main; 5 configs x 53) and `...-e6-heldout.jsonl` (held-out; 4 configs x 13). Rows with `"config": "E5-2"` are the best config. Main-set numbers are post-audit and current; **held-out numbers are unaudited floors** and every held-out claim below inherits that caveat.
- Corpus units and typed edges: `/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/corpus-c64d41c-units-v1-ep4-p250.json` (11,492 units, 12,283 edges, 627 pages).
- Mechanism: `git show origin/episode-4-prototype:tools/ratchet-proto/ratchet/idlookup.py` and `...:tools/ratchet-proto/ratchet/pipeline.py`.

Method check: the `idlookup.parse`/`lookup` rule was re-implemented from `idlookup.py` and run over
all 90 questions. Its `pinned > 0` verdict agrees with the recorded `pinned` field on **all 66
evaluated questions, zero mismatches** — so the feature extraction below is the pipeline's own
rule, not a paraphrase of it.

Two mechanism facts established here that the rest of the note leans on:

- `EXCLUDED = {"withdrawn", "superseded", "rejected"}` in `ratchet/corpus.py:32`. `history` is **not** excluded. So the lifecycle filter removes 196 of 11,492 units (90 withdrawn, 96 superseded, 10 rejected) and keeps all 1,710 `history` units.
- `Graph.typed_neighbours` (`ratchet/pipeline.py:98-103`) returns only destinations whose node kind is `"u"`. In this corpus **all 166 `typed:defer-to` edges are unit→page** (`('u','p','typed:defer-to'): 166`), and there are zero unit→unit defer-to edges. The expansion docstring says "defer-to resolved to a unit", but no such edge exists, so **defer-to is present in the graph and unfollowable by E5-2's expansion**. The followable types are `typed:cites` (866), `typed:target` (255), `typed:delta` (168), `typed:validates` (122), all unit→unit.

---

## Tabulation: does each question name a spec, an id, a lifecycle word, a date or release tag?

### Takeaway

Across all 90 questions, **no question ever names an element id without also naming a spec** (0
cases), and the lookup's deliberate dead zone — a spec with no id, which pins nothing — is the
single largest structured-field opportunity at **22 of 90 questions**. Lifecycle words appear in
only 12 of 90 and dates or release tags in 15 of 90, concentrated in `questions-time.jsonl`.

### Cited Findings

Prevalence, measured by running `idlookup.parse` over each question against the corpus anchor set — [questions.jsonl, questions-heldout.jsonl, questions-time.jsonl](/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/) with [idlookup.py](git show origin/episode-4-prototype:tools/ratchet-proto/ratchet/idlookup.py):

| set | n | names id | names spec | id+spec | **spec only (pins nothing)** | id only | neither | lifecycle word | date/tag | pinned>0 |
|---|---|---|---|---|---|---|---|---|---|---|
| questions (main) | 53 | 10 | 19 | 10 | **9** | 0 | 34 | 3 | 4 | 10 |
| questions-heldout | 13 | 1 | 6 | 1 | **5** | 0 | 7 | 4 | 1 | 1 |
| questions-time | 24 | 1 | 9 | 1 | **8** | 0 | 15 | 5 | 10 | 1 |
| **total** | **90** | **12** | **34** | **12** | **22** | **0** | **56** | **12** | **15** | **12** |

- Every question that names an id also names a spec, so the "id exists in exactly one spec" fallback branch of `IdIndex.lookup` is never exercised on this golden set — [idlookup.py](git show origin/episode-4-prototype:tools/ratchet-proto/ratchet/idlookup.py).
- Zero questions contain an id-shaped token that is **not** a corpus anchor, so the lookup's `t in anchors` guard never discards anything here — measured over all 90 questions.
- Per-question features for the 12 that pin: `G-021` FR-001/040, `G-022` NFR-001/045, `G-023` SC-002/119, `G-024` D-002/026, `G-025` T-100/065, `G-026` FR-003/103, `G-027` D-001/078, `G-028` SC-001/100, `G-029` FR-008/109, `G-030` SC-002/110, `H-001` SC-002/113, `G-072` D-005/004 — [e6.jsonl / e6h.jsonl](git show origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr-e6.jsonl).
- Lifecycle words found, by question: main `G-033` "dropped", `G-034` "superseded", `G-042` "no longer"; held-out `H-009` "withdrawn", `H-010` "abandoned", `H-011` "used to", `H-012` "retired"; time `G-051` "withdrawn", `G-052` "withdrawn", `G-057` "replaced", `G-060` "no longer", `G-061` "originally".
- Dates/release tags found, by question: main `G-006` "v0.1.0", `G-017`/`G-079` "before", `G-033` "after"; held-out `H-013` "before"; time `G-055`/`G-056`/`G-066` "before", `G-062`/`G-063`/`G-065`/`G-066` "as of", `G-063` "release v0.1.0-pre.24", `G-068` "v0.1.0", `G-070` "v0.1.0", `G-071` "v0.1.0".
- `questions-time.jsonl` carries an explicit `cutoff` on 9 of 24 (`G-062`–`G-066`, `G-072`–`G-075`) and a `router` string on all 24 — [questions-time.jsonl](/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions-time.jsonl).

### Inferences

- The feature distribution explains the measured 10/53 firing rate directly: typed expansion is gated on a pin, a pin needs an id, and only 10 main-set questions spell one. Nothing about the expansion is weak — it is starved.
- Because id-without-spec never occurs, a structuring step that emitted only `spec` and `id` would be exactly as precise as the regex on the 12 questions that already pin, and would add nothing there. Its entire value is on the other 78.

### Gaps

- My date/tag regex counts bare prepositions ("before", "after") as temporal markers, which overcounts relative to the golden set's own `cutoff` field (15 by regex vs 9 with a cutoff). Treat the 15 as an upper bound on "names a date or tag".

---

## How many questions name a spec but no id, and could a structuring step supply the id?

### Takeaway

22 of 90 questions hit the "a spec with no id pins nothing" rule. In 13 of them the expected unit
does sit on an id-shaped anchor inside the named spec, so a structuring step that guessed the id
would pin exactly the right unit — but in **only 1 of those 13 is the id anywhere in the question
text** (`G-072`'s `D-005`, which already pins). In the other 9 the expected anchor inside the named
spec is a *section* (`requirements`, `dependencies`, `intent`, `risks`, `changelog`), which the id
lookup cannot express at all.

### Cited Findings

All 22, with what the expected units look like inside the named spec — derived from `expected` in the golden files joined to the corpus unit keys:

| id | set | spec named | expected anchors inside that spec | id-shaped? | expected outside that spec |
|---|---|---|---|---|---|
| G-031 | main | 058 | `requirements` | no | `051/spec#FR-003` |
| G-032 | main | 055 | `requirements` | no | `054/spec#FR-001` |
| G-033 | main | 023 | `D-006` | yes | `024/spec#FR-001` |
| G-034 | main | 001 | — (none) | — | `003/spec#FR-010` |
| G-036 | main | 076 | `dependencies` | no | `071/design#D-002` |
| G-037 | main | 004 | `D-006` | yes | — |
| G-041 | main | 094 | `NFR-001` | yes | — |
| G-042 | main | 099 | `FR-004` | yes | — |
| G-044 | main | 021 | `FR-003` | yes | — |
| H-003 | held-out | 052 | — (none) | — | `053/spec#FR-001`, `053/spec#FR-002` |
| H-004 | held-out | 106 | `requirements` | no | `105/spec#FR-013` |
| H-009 | held-out | 003 | `FR-002`, `intent` | mixed | — |
| H-010 | held-out | 099 | `risks` | no | — |
| H-012 | held-out | 105 | `T-1404` | yes | — |
| G-051 | time | 003 | `SC-001`, `changelog` | mixed | — |
| G-055 | time | 093 | `T-1100`,`T-1101`,`T-1200`,`T-1201` | yes | — |
| G-057 | time | 004 | `D-005` | yes | — |
| G-063 | time | 078 | `FR-005` | yes | — |
| G-067 | time | 095 | `FR-013` | yes | — |
| G-071 | time | 105 | `FR-010` | yes | — |
| G-073 | time | 095 | `T-1203` | yes | — |
| G-074 | time | 105 | `D-001` | yes | — |

- The brief's own worked example, "Spec 058's corpus-license rule", is in fact the *hardest* case in this table: `G-031`'s in-spec expected unit is `specs/058-corpus-licensing/spec.html#requirements`, a section anchor, and `ID_TOKEN` can never match `requirements` — [questions.jsonl G-031](/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions.jsonl); [idlookup.py](git show origin/episode-4-prototype:tools/ratchet-proto/ratchet/idlookup.py).
- 5 of the 9 main-set spec-only questions are already full hits at rank 1-4 without any pin (`G-032` [2,1], `G-034` [4], `G-037` [1], `G-041` [1], `G-042` [1], `G-044` [1]) — so for most of this class the lookup pinning nothing costs nothing — [e6.jsonl](git show origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr-e6.jsonl).
- `T-1404` (held-out `H-012`) appears as an anchor in only 3 specs, and `FR-013` (held-out `H-004`) in 18, whereas `FR-001` appears in 115, `FR-002` in 114, `D-001` in 112 — measured over the corpus anchor index. So some ids would pin even without a spec; most would not.

### Inferences

- The dead zone is real but misdescribed as an id-supply problem. For the 9 spec-only questions whose in-spec target is a section, no value of a `spec`+`id` pair unlocks them; what is missing is a *third* field (`section`) and a pin mechanism that accepts it. That is new pipeline surface, not a query rewrite.
- For the 13 whose in-spec target is id-shaped, the id is the answer, not the question: `H-012` asks "why was spec 105's task for reporting a disagreement ... retired" and the structuring step would have to already know that task is `T-1404`. A question-structuring step that reads only the question text cannot produce it. A step allowed to look up the corpus could, but then it is doing retrieval, and the claim "structuring unlocks this" becomes circular.
- `G-072` is the one case where id and spec are both spelled (`D-005`, spec `004`) and it already pins — i.e. the golden set contains no example of a recoverable in-text id the current regex misses.

### Gaps

- Whether an LLM structuring step could in practice map a description like "the task for reporting a disagreement between a declared token and a derived value" onto `T-1404` is not measurable from the question text alone, which is the scope I was given. It would need a corpus-grounded extraction run, which the constraints forbid.

---

## Per-miss account: the structured field, whether the text carries it, and the capability it triggers

### Takeaway

Of the 12 answerable questions E5-2 misses at full recall@10 (3 main, 9 held-out), **4 have the
unlocking value literally in the question text** (`H-003`, `H-009`, `H-010`, `H-012`) and 8 do not.
Of those 4, three need the *lifecycle filter relaxed*, not the identifier lookup improved. The
single cleanest typed-relation win, `H-004`, is a recall@4 miss whose unlocking anchor is already
at rank 1.

### Definitions used

A **miss** = at least one `expected` unit at rank `None` or `>10` in the E5-2 row. A **near miss**
= all expected within 10 but at least one `>4` (the pipeline reports `context_tokens_top4`, so
rank 4 is the operationally interesting boundary). Unanswerable questions (`G-045`–`G-050`, all
with empty `expected`) are excluded, leaving 47 answerable main-set questions.

- Main-set misses (recall@10): **G-033, G-036, G-080** → 44/47.
- Main-set near misses (recall@4 only): **G-031** (ranks [1,6]), **G-076** (ranks [5,5,1]).
- Held-out misses (recall@10): **H-002, H-003, H-005, H-006, H-007, H-009, H-010, H-012, H-013** → 4/13 clean.
- Held-out near miss: **H-004** (ranks [1,5]).

### Cited Findings — main set

- **G-033** ranks [1,17], pinned 0. Q: *"Spec 023's design dropped its decision on generating restore tasks after an exploration graduates. Which slice took that over, and how is restore invoked now?"* Expected `023/design#D-006` (rank 1) and `024/spec#FR-001` (rank 17). Field needed: the *successor spec* (024). **Not in the text** — "which slice took that over" is the question. There are **zero typed edges** touching either expected unit, so even a relation field (`taken-over-by`) would have nothing to follow. Capability: none existing.
- **G-036** ranks [17,4], pinned 0. Q names spec 076. Missed unit `076/tasks.html#dependencies` has **zero typed edges in or out**. Field needed: `spec=076, section=dependencies`. Spec is in the text; the section kind is **inferred** from "which spec provides that promotion hook" → the cross-spec dependency list. Capability: a section-level pin, which does not exist (`ID_TOKEN` cannot match `dependencies`).
- **G-080** ranks [None], pinned 0. Q: *"What problem led spectastic to add checks that the same spec number or requirement id isn't handed out twice?"* Expected `025-id-uniqueness/spec.html#context`, zero typed edges. Field needed: `spec=025` + `section=context`. **Neither in the text** — the question describes the spec's topic and never numbers it. Top-10 is dominated by `000-spectastic/triage-log.html`. Capability: none; would require slug resolution from topic plus a section pin.
- **G-031** (near miss) ranks [1,6]. Missed unit at depth 4 is `058/spec#requirements`, which is itself the source of two `typed:defer-to` edges to the pages `051-knowledge-corpus/spec.html` and `053-corpus-grounding-gates/spec.html`. The question states the defer-to relation in plain words ("reads a license field that 058 says it does not own. Which spec defines that field"). Field needed: `spec=058, section=requirements` (or `relation=defer-to, from=058`). Spec and relation in text; anchor granularity inferred.
- **G-076** (near miss) ranks [5,5,1], pinned 0. All three expected units are in `031-init-tools`; the question says "spectastic's init with the tools option" — the spec's *slug* almost verbatim, its number nowhere. Even a `spec=031` field pins nothing under the current rule. Capability: a spec-scope restriction or the existing sibling expansion (`SIBLING_SEEDS=5`, `SIBLING_MAX=200`, `ratchet/pipeline.py`), not the id pin.

### Cited Findings — held-out (unaudited floors)

- **H-003** ranks [None,None], pinned 0, golden `link: "typed:defer-to (to the page)"`. Q: *"Spec 052 defines how corpus citations are written but leaves checking them to another spec. Which one, and how strict is each check?"* `052/spec#requirements` emits `typed:defer-to` to the pages `053-corpus-grounding-gates/spec.html`, `054-...`, `057-...`; **both** expected units (`053/spec#FR-001`, `#FR-002`) are on the first of those pages. Field: `relation=defer-to, from=spec 052`. **In the text** (spec number + the relation in words). Capability: pin the deferring section, then follow `typed:defer-to` *to a page* and admit that page's units. The second half does not work today — `typed_neighbours` drops `kind != "u"`, so all 166 defer-to edges are inert.
- **H-004** (near miss) ranks [1,5], golden `link: "typed:defer-to + link to FR-013"`. `106/spec#requirements` is **already at rank 1** and emits `typed:defer-to` to `105-design-source-import/spec.html`, the page holding the expected `105/spec#FR-013`. I confirmed by simulation that seeding expansion on the top-1 item and resolving defer-to to page units reaches `FR-013` at seed depth 1, 3 and 5. Field: `relation=defer-to` (the question says "the requirement it relies on"). **In the text.** Capability: unpinned anchoring + page-resolved defer-to.
- **H-002** ranks [3,27], golden `link: "typed:validates (unanchored)"`. `115/spec#SC-002` emits `typed:validates` to **both** expected units (`115/spec#FR-004`, `#FR-007`). But `SC-002` is not in the top-10 (`SC-001` is, at rank 1, and its validates edges go to FR-001/002/005/006 — the wrong four). Field would be `id=SC-002`; **not in the text**, and the question's wording ("what is each violation guaranteed to tell you") does not name the validates relation either. Capability: typed expansion from an unpinned anchor would help only if the anchor chosen were `SC-002`.
- **H-005** ranks [None,None], golden `link: "typed:validates (unanchored)"`. `118/spec#SC-002` --validates--> `118/spec#FR-003`; both are the expected pair, so pinning `SC-002` delivers the whole answer in one hop. Top-10 holds `118/design#principles-check`, `118/spec#scenarios`, `118/spec#SC-003` — and `SC-003`'s only typed edge goes to `NFR-001`, the wrong requirement. Field: `id=SC-002`; **not in the text**.
- **H-006** ranks [1,None], golden `link: "prose only"`. Expected `007/design#D-001` (1) and `013/design#D-001` (None). **Zero typed edges on either.** The second spec is the answer. Not unlockable.
- **H-007** ranks [5,None], golden `link: "prose only"`. Expected `012/design#D-006` (5) and `011/design#D-002` (None). Zero typed edges on either. Not unlockable.
- **H-013** ranks [None,None], golden `link: "prose only"`. Expected `008/design#D-005` and `007/design#D-001`. Zero typed edges on either. Top-10 is triage logs. Not unlockable.
- **H-009** ranks [None,None]. Q: *"A **withdrawn** change to **spec 003** argued that the list of files init must write was wrong."* Both expected units live in `003/changes/withdrawn/2026-08-15-the-bundle-is-the-list/proposal.html` (`#intent`, `#FR-002`), lifecycle `withdrawn`, therefore removed by `EXCLUDED` before candidate generation — and also removed from `pinned_keys` by the `_live_items(k, cfg.lifecycle)` filter in `Pipeline.run`. Fields: `lifecycle=withdrawn` and `spec=003`, **both literally in the text**. Capability: admit the class. Bonus finding: `#FR-002` is reachable by a unit→unit typed edge from the current rank-1 item (`003/spec#FR-002`), so once the class is admitted, graph expansion alone reaches one of the two.
- **H-010** ranks [None]. Q names **spec 099** and "**abandoned**". Expected `099/changes/withdrawn/2026-08-18-the-coordinate-gets-a-consumer/proposal.html#risks`, lifecycle `withdrawn`, excluded. Fields: `lifecycle=withdrawn` (from "abandoned") + `spec=099`, both in the text. Note the whole top-10 sits in spec **095**, not 099 — so the spec field is doing independent work here, pulling retrieval back to the right directory.
- **H-012** ranks [None]. Q names **spec 105** and "**retired**". Expected `105/tasks.html#T-1404`, lifecycle **`superseded`**, excluded. Fields: `lifecycle=superseded` (inferred from "retired") + `spec=105`. The word is in the text; the mapping retired→superseded is a vocabulary decision.

### Cited Findings — questions-time (feature-only; not in the result files)

These 24 were never run, so there are no ranks. What is measurable is the lifecycle of their expected units against `EXCLUDED`:

- **7 of 24 have at least one expected unit the lifecycle filter removes**: `G-051` (1 of 2 withdrawn), `G-052` (1 of 2 withdrawn), `G-054` (rejected), `G-055` (all 4 superseded), `G-056` (superseded), `G-058` (both withdrawn), `G-073` (superseded).
- **5 of those are wholly unreachable without admitting the class**: `G-054`, `G-055`, `G-056`, `G-058`, `G-073`.
- `G-072`'s expected unit `specs/004-npm-publish-workflow/plan.html#D-005` is **absent from the corpus snapshot entirely** — there are zero `plan.html` pages at commit `c64d41c`, and spec 004 has only `design.html`, `spec.html`, `tasks.html`, `triage-log.html`. No structured field reaches it; it needs a store that carries the old file name. `G-075` has no `expected` at all (unanswerable at its cutoff).
- The golden set's own `router` notes already record where a lifecycle word list would fail: `G-054` *"'set aside' is not a narrow-list marker"*, `G-055` *"'shelved' is not on the narrow list"*, `G-056` *"'original' is close to the narrow marker 'originally' but is not it"*, `G-057` *"'replaced' is not on the list"*, `G-058` *"no narrow-list marker ('ever consider', 'didn't go ahead')"* — [questions-time.jsonl](/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions-time.jsonl). So 4-5 of the HISTORY questions carry the lifecycle intent semantically but not lexically: a word list misses them, a model would have to infer.
- Conversely the three `history-decoy` questions (`G-059`, `G-060`, `G-061`) all have `current` expected units, and two of them *do* carry lifecycle words ("no longer", "originally"). `G-061`'s router note calls this an "expected misroute under the report's rule list". So a lifecycle field has a measurable false-positive cost: 2 of 3 decoys would be misrouted by lexical extraction.
- All 5 CHANGE questions (`G-067`–`G-071`) name both endpoints in the text (dates or `v0.1.0-pre.NN` tags) and all their expected units are `current`. Their difficulty is version history, not retrieval depth.
- All 5 AS_OF questions (`G-062`–`G-066`) carry a `cutoff` in the golden set and 4 of 5 carry "as of" or a date in the text; `G-064` phrases it as "On 12 August 2026", which the golden router note flags as not covered by the stated rule.

### Inferences

- The misses split into three mechanically distinct families, and only one is a query-structuring problem:
  1. **Lifecycle admission** (`H-009`, `H-010`, `H-012`, plus 5 of the time set). The field is a single enum, the word is usually in the text, and the capability is a one-line change to how `EXCLUDED` is applied. This is the highest-yield family and the cheapest.
  2. **Defer-to resolution** (`H-003`, `H-004`, and `G-031`/`G-032` at depth 4). The relation is in the question's words and the edge is in the graph, but it is unit→page and `typed_neighbours` discards it. Structuring helps only if the expansion is fixed at the same time; fixing the expansion may help even without structuring, since `H-004`'s anchor is already rank 1.
  3. **Prose-only, no edge** (`G-033`, `G-080`, `H-002`, `H-005`, `H-006`, `H-007`, `H-013`). Either the value a structured field would carry *is the answer*, or no typed edge exists to follow. No structured field over the question text reaches these.
- The two held-out "unanchored validates" misses (`H-002`, `H-005`) are the strongest argument for a change that is *not* query structuring: in both cases a single unit (`SC-002` in its spec) sits one validates-hop from the entire expected set, and in both cases the question never names it. Letting expansion anchor on the reranker's top hit would be a retrieval change; no amount of question parsing produces `SC-002`.
- The decoy evidence means a lifecycle field must be a routing *hypothesis* that retrieval can reject, not a hard filter switch — 2 of 3 decoys carry the trigger word while needing current units.

### Gaps

- Admitting `withdrawn`/`superseded` makes `H-009`, `H-010`, `H-012` and the five time-set questions *candidates*; whether BM25 and the dense retriever then rank them inside top-10 is **not measurable without running the pipeline**, which the constraints forbid. So the lifecycle family's yield is bounded above by 3 (held-out) + 5 (time), and bounded below by 0. The one positive signal is `H-009`, where the expected `#FR-002` is a typed unit→unit neighbour of the current rank-1 item.
- `questions-time.jsonl` has no result rows in either file, so none of its 24 questions has a measured rank under any config. Everything said about it is a feature and lifecycle analysis only.
- Held-out figures are unaudited floors; an audit could reclassify any of `H-002`–`H-013`'s expected sets, which would change the per-question accounting above.

---

## The ceiling: how many misses are reachable from what the question text actually contains?

### Takeaway

If a perfect structuring step extracted everything present in the question text and the pipeline
gained exactly the two capabilities that text would trigger (lifecycle admission and page-resolved
defer-to), the honest ceiling is **4 of 12 recall@10 misses** — all four in the held-out set, none
in the main set — rising to **6 of 14** if the two recall@4 near misses `G-031` and `H-004` are
counted. **8 of 12** misses are not reachable by any structured field derivable from the question.

### Cited Findings

Ceiling accounting, miss by miss. "In text" means the unlocking value appears in the question's own
words; "inferred" means a model would have to supply it.

| miss | depth | unlocking field | in text? | capability it triggers | reachable? |
|---|---|---|---|---|---|
| G-033 | @10 | successor spec 024 | **no — it is the answer** | none (zero typed edges) | no |
| G-036 | @10 | `spec=076, section=dependencies` | spec yes, section inferred | section pin (new) | no — target has zero typed edges and no pin can name it |
| G-080 | @10 | `spec=025, section=context` | **no** | slug resolution + section pin (new) | no |
| H-002 | @10 | `id=SC-002` | **no** | typed:validates from an unpinned anchor | no |
| H-003 | @10 | `relation=defer-to, from=052` | **yes** | defer-to resolved to page | **yes** |
| H-005 | @10 | `id=SC-002` | **no** | typed:validates | no |
| H-006 | @10 | successor spec 013 | **no — it is the answer** | none (zero typed edges) | no |
| H-007 | @10 | source spec 011 | **no — it is the answer** | none (zero typed edges) | no |
| H-009 | @10 | `lifecycle=withdrawn, spec=003` | **yes** | admit the excluded class | **yes (candidacy); rank unmeasured** |
| H-010 | @10 | `lifecycle=withdrawn, spec=099` | **yes** ("abandoned") | admit the class + spec scope | **yes (candidacy); rank unmeasured** |
| H-012 | @10 | `lifecycle=superseded, spec=105` | **yes** ("retired") | admit the class | **yes (candidacy); rank unmeasured** |
| H-013 | @10 | source spec 007 | **no — it is the answer** | none (zero typed edges) | no |
| G-031 | @4 | `spec=058, section=requirements` | spec + relation yes, section inferred | section pin + defer-to to page | borderline |
| H-004 | @4 | `relation=defer-to` | **yes** | defer-to to page from rank-1 anchor | **yes (simulated)** |

- Main-set ceiling from text extraction alone: **0 of 3**. E5-2 already reaches 44 of 47 answerable main-set questions at recall@10, and all three remaining misses need a value the question does not contain — [e6.jsonl](git show origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr-e6.jsonl).
- Held-out ceiling: **4 of 9** (`H-003`, `H-009`, `H-010`, `H-012`), taking held-out numbers as the unaudited floors they are.
- 5 of the 12 misses (`G-033`, `H-006`, `H-007`, `H-013`, plus `G-036`'s in-spec target) have **zero typed edges of any kind** on the missed unit, verified against all 1,411 typed edges in the corpus. For these, no relation field helps because there is no relation recorded.
- Simulation result, stated precisely: seeding typed expansion on the top-1, top-3 or top-5 retrieved units and following unit→unit typed edges plus page-resolved defer-to reaches the missed unit for exactly **2 of 15** misses examined — `H-004` (via defer-to) and `H-009` (via unit→unit, once lifecycle permits). For the other 13 the missed unit is outside the one-hop neighbourhood of everything retrieved.
- Beyond the main and held-out sets, the time set adds a bounded upside: 5 questions (`G-054`, `G-055`, `G-056`, `G-058`, `G-073`) are wholly unreachable while `EXCLUDED` holds, and 4-5 of them express the lifecycle intent in words no fixed marker list catches ("set aside", "shelved", "first settle on", "ever consider") — so for the time set a *model* structuring step would beat a word list, which is the opposite of the main-set finding.

### Inferences

- The honest headline is uncomfortable for the structuring hypothesis as applied to the main set: the main set is already saturated at 44/47, and its three remaining misses are unreachable from their own text. Structured query rewriting cannot move the main-set number.
- Where structuring does pay, it pays on *lifecycle*, not on *identifiers*. Three of the four reachable misses are "admit the withdrawn/superseded class" and the field is a single enum whose trigger word is already in the question. That is the result to lead with, and it also covers the five time-set questions the filter currently makes impossible.
- The identifier half of the hypothesis is close to exhausted. Every in-text id is already pinned (12/12), no question names an id the regex misses, and the dead zone (spec-with-no-id, 22 questions) mostly needs a *section* field the pin mechanism cannot express. Extending `idlookup` would be work with no measurable target on this golden set.
- The defer-to finding is arguably the most actionable item in this note and is not a structuring result at all: 166 edges of the relation type that three questions describe in plain English are in the graph and unreachable by the code that claims to follow them. `H-004` is reachable today from an anchor already at rank 1.

### Gaps

- The ceiling of 4 (or 6 with near misses) is a *reachability* ceiling, not a measured recall gain. Three of the four depend on ranking behaviour after the lifecycle class is admitted, which I could not measure under the no-pipeline constraint.
- I could not quantify the precision cost of a lifecycle field. The three `history-decoy` questions are the only probes available and two of them carry the trigger word while needing current units; with n=3 that is a signal, not a rate.
- No measurement here speaks to answer quality, only to whether the expected unit enters the ranked list. A field could unlock retrieval and still not change the generated answer.
