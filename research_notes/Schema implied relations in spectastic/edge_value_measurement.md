# Edge-value measurement: would schema-implied sibling relations reach the questions retrieval misses?

Scope: reachability analysis over saved artefacts only. No model and no prototype stage was run.
Everything below is computed from three files:

- Golden sets — [questions.jsonl](/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions.jsonl) (53 rows, 47 answerable) and [questions-heldout.jsonl](/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions-heldout.jsonl) (13 rows, 13 answerable).
- Saved rankings — `git show origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr.jsonl` (main, 32 configs × 53) and `…-heldout.jsonl` (held-out, configs S3, E5-1, X1, H1 only).
- Corpus units + parsed edge graph — [corpus-c64d41c-units-v1-ep4-p250.json](/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/corpus-c64d41c-units-v1-ep4-p250.json).
- Pipeline semantics (cap, seeding, graph symmetry) — `git show origin/episode-4-prototype:tools/ratchet-proto/ratchet/pipeline.py`.

**Staleness caveat, as instructed:** the main-set rows in `5090-qwen3-emb-8b-qwen3-rr.jsonl` carry the old supersession rule and pre-audit qrels. Every main-set figure below is used for *reachability* (is an expected address adjacent to something already retrieved?) and for *config-vs-config* comparison under the identical stale qrels. No main-set Hit@4 figure here should be quoted as a current score. The held-out rows are unaudited too (the episode note calls every held-out figure a floor), but they are the set the question is about and their qrels are the ones used to define the 8 misses.

**Definitions used throughout.** A unit's address is `<source>#<anchor>`. *Reached* means: some address in the question's retrieved top-10 is adjacent to an expected address under the mechanism — i.e. the expected passage becomes a *candidate*. It is a reachability ceiling, not a predicted hit: nothing here models whether a reranker would then place it in the top 4. Mechanisms:

| mechanism | definition |
|---|---|
| `typed-uu` | parsed `typed:*` edge, unit↔unit (the graph is built symmetric, see below) |
| `href-uu` | parsed `href:*` edge, unit↔unit |
| `same-page` | same `source`, different anchor (same artifact) |
| `schema3` | sibling artifact in the same `specs/<nnn-slug>/`, restricted to the literal triple design↔spec, tasks↔design, verify↔spec |
| `spec-hub` | sibling artifact in the same dir where one end is `spec.html` |
| `sibling-dir` | any sibling artifact in the same `specs/<nnn-slug>/` directory (the brief's definition) |
| `sibling-spec` | any artifact anywhere under the spec, i.e. `sibling-dir` plus `changes/**/proposal.html` |
| `anchor-dir` / `anchor-spec` | the narrower edge: same anchor id, different artifact, within the dir / within the whole spec |

---

## Q1. For held-out misses, is an expected passage in a sibling artifact of a page already retrieved?

### Takeaway
Yes, for 6 of 8 — and for all 8 if the spec's `changes/` subtree counts as sibling. But the literal triple in the objective (design→spec, tasks→design, verify→spec) only reaches 4 of 8; the extra reach comes from relations outside that triple (tasks→spec, triage-log→tasks, spec→proposal), and 7 of the 8 misses have no pinned identifier, so the existing expansion trigger cannot fire on them at all.

### Cited Findings
- Best held-out configuration is E5-1 at 5/13 Hit@4; its 8 misses are H-001, H-003, H-005, H-007, H-009, H-010, H-012, H-013 — [heldout results](origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr-heldout.jsonl), summary table in `…-heldout.md`.
- Reach from the retrieved top-10, per mechanism (computed over those 8 misses) — [heldout results](origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr-heldout.jsonl) + [corpus edges](/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/corpus-c64d41c-units-v1-ep4-p250.json):

| mechanism | reach | question ids reached | not reached |
|---|---|---|---|
| `typed-uu` (baseline) | **2/8** | H-001, H-009 | H-003 H-005 H-007 H-010 H-012 H-013 |
| `typed-uu` + `href-uu` (baseline) | **3/8** | H-001, H-007, H-009 | H-003 H-005 H-010 H-012 H-013 |
| `anchor-dir` | **0/8** | — | all 8 |
| `anchor-spec` | **1/8** | H-009 | the other 7 |
| `same-page` | **4/8** | H-001, H-003, H-005, H-013 | H-007 H-009 H-010 H-012 |
| `schema3` (the literal triple) | **4/8** | H-003, H-005, H-007, H-013 | H-001 H-009 H-010 H-012 |
| `spec-hub` | **5/8** | + H-001 | H-009 H-010 H-012 |
| `sibling-dir` | **6/8** | H-001 H-003 H-005 H-007 H-012 H-013 | H-009, H-010 |
| `sibling-spec` | **8/8** | all | — |
| `same-page` + `sibling-spec` | **8/8** | all | — |

- The reconstruction reproduces the stated baseline exactly: unit↔unit `typed:*` reaches H-001 and H-009 and nothing else; adding unit↔unit `href:*` adds H-007 and nothing else. This is a check on the method, not a new result — [brief's stated baseline vs. computed].
- The baseline's two typed routes: H-001 is `specs/113-guardrail-retrieval/spec.html#SC-002 --typed:validates--> #FR-003/#NFR-002`, and the seed `#SC-002` is at **rank 1**. H-009 is `specs/003-init-node-port/spec.html#FR-002 --typed:delta--> changes/withdrawn/2026-08-15-the-bundle-is-the-list/proposal.html#FR-002`, traversed from the spec side.
- `Graph.__init__` adds every edge in both directions (`self.adj[s].append(...)`; `self.adj[d].append(...)`), so `typed_neighbours` is undirected — the baseline's mixing of forward (H-001) and backward (H-009) traversal is what the implementation actually does — [pipeline.py](origin/episode-4-prototype:tools/ratchet-proto/ratchet/pipeline.py).
- H-012's only graph edge is `tasks.html#T-1404 --href:same-spec--> <proposal page>`, i.e. unit→**page**, not unit→unit. That is why the parsed-edge baseline is 3 and not 4: the hyperlink exists but its far end is a whole page, so an anchored expansion cannot land on `#T-1404` — [corpus edges](/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/corpus-c64d41c-units-v1-ep4-p250.json).
- Which artifact pair does the work, per reached question (retrieved artifact → expected artifact):
  - H-001 `tasks→spec` (and `same-page` spec→spec)
  - H-003 `design→spec` (and `same-page`)
  - H-005 `design→spec` (and `same-page`)
  - H-007 `spec→design`, `triage-log→design`, `proposal→design`
  - H-009 `spec→proposal` (changes/withdrawn)
  - H-010 `spec→proposal` (changes/withdrawn)
  - H-012 `proposal→tasks`, `triage-log→tasks`
  - H-013 `spec→design`, `tasks→design` (and `same-page`)
- Reachability is almost entirely *new*, not a re-ranking problem: 7 of the 8 misses have **every** expected address at `rank = None` (never retrieved at depth 50). Only H-007 has an expected address in range at all (`specs/012-core-plan/design.html#D-006` at rank 5) — [heldout results](origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr-heldout.jsonl).
- Two expected addresses are reached by **no** mechanism at any setting: `specs/011-core-spec/design.html#D-002` (H-007's second) and `specs/008-core-principles/design.html#D-005` (H-013's first). Both are in a *different spec* from anything retrieved; all sibling mechanisms are intra-spec by construction.
- Seed depth matters. Restricting seeds to the top-4 instead of the top-10: `sibling-dir` falls 6→**5** (loses H-001, whose only sibling seed `tasks.html#T-010` sits at rank 10), `sibling-spec` falls 8→**7** (loses H-001), `same-page` stays 4, `typed-uu` stays 2. At top-3, `sibling-dir` falls to 2.
- **Only H-001 of the 8 misses has `pinned = 1`**; the other seven are unpinned. The current expansion (`cfg.expand`) is seeded from `pinned_keys[0]` only, so on the held-out misses it can fire on exactly one question regardless of which edge type it follows — [heldout results](origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr-heldout.jsonl) + [pipeline.py](origin/episode-4-prototype:tools/ratchet-proto/ratchet/pipeline.py).

### Inferences
- The headline for the objective: **a whole-artifact sibling edge within the spec directory doubles the reachable held-out misses versus the parsed-edge baseline (6 vs 3), and the spec-wide version reaches all 8.** Because 7 of 8 are currently unretrieved at any depth, this is genuinely new reachability, not re-ranking of things already in hand.
- But the objective's exact hypothesis under-delivers: design→spec / tasks→design / verify→spec as literally stated reaches 4/8, no better than a same-page edge and only one more than hyperlinks. The two relations that actually earn their keep in the held-out set are **tasks→spec** (not in the triple; tasks relate to design there) and **spec→proposal** into `changes/`, which is not an artifact type at all but a subtree.
- `same-page` is the surprise: reaching 4/8 (H-001, H-003, H-005, H-013) from the top-4 alone. In all four the retriever landed on the *right artifact* and the wrong section — a requirement's sibling `SC-`/`scenarios`/`context` block. A within-artifact relation is a different, cheaper mechanism than siblinghood and gets half the held-out reach.
- The pinned-seed finding reframes the whole question: on the held-out set the binding constraint is the expansion *trigger*, not the edge *type*. Any mechanism that reaches H-003/H-005/H-007/H-009/H-010/H-012/H-013 must be seeded from the ranking, which is the regime that measured worse (Q4).
- Even at `sibling-spec`, all-evidence@4 cannot close for H-007 and H-013 — their second passage is in another spec. Sibling edges can move Hit@4 by up to +6 (or +8) on the held-out set but all@4 by at most +6.

### Gaps
- Whether a reached candidate would actually be *ranked* into the top 4 is not answerable from saved files; it needs a reranker pass, which the brief forbids. Every reach figure is a ceiling.
- The held-out qrels are unaudited (the commit message for `c971f2b` says so), so a "miss" could in principle be a qrel defect rather than a retrieval failure. H-013's unreachable `008-core-principles/design.html#D-005` is the kind of row worth re-checking against the audit.

---

## Q2. Same question for the main set

### Takeaway
On the main set the mechanism is nearly redundant with what is already parsed: under the stale best configuration there is one miss left and a sibling edge reaches it, but two of the three misses under E5-1 are already fixed by the shipped typed expansion.

### Cited Findings
- Main-set Hit@4 under the stale qrels: S5 44/47, E5-1 44/47, E5-2 and E5-2r 46/47, X1 32/47, H1 37/47, EpiB 36/47 — computed from [main results](origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr.jsonl). **Stale; comparison only.**
- E5-2r (best, 46/47) misses **G-080** only. Reached by `same-page` (from the top-4), by `sibling-dir` and `sibling-spec` (from the top-10); reached by **no** parsed edge, typed or href. Its expected address is `specs/025-id-uniqueness/spec.html#context`, `rank = None` — unreachable at any depth today. The route is `verify.html → spec.html`, the one member of the literal triple that fires anywhere in this analysis.
- E5-1 (44/47) misses G-029, G-030, G-080. `typed-uu` reaches G-029 and G-030; `sibling-dir` reaches all three. G-029 and G-030 are the exact two questions E5-2r converts with the shipped typed expansion (won G-029, G-030; lost none).
- G-029 and G-030 have `pinned = 1`; G-080 has `pinned = 0`.
- H1 (no reranker, 37/47) misses G-007, G-011, G-012, G-014, G-015, G-017, G-030, G-036, G-078, G-080. `typed-uu` reaches 1 (G-030), `typed+href` 3 (G-017, G-030, G-036), `sibling-dir` **7** (G-007, G-011, G-012, G-017, G-030, G-036, G-080), `same-page` 9 of 10 from the top-10 (all but G-078). `anchor-dir` and `anchor-spec` reach **0**.
- X1 (BM25 line, 32/47) misses 15; `sibling-dir` reaches 6, `sibling-spec` 7, `typed-uu` 1, `href-uu` 0, `anchor-*` 0.
- Base rate of "right spec dir already in the top-10, wrong artifact" among misses: held-out E5-1 6/8; main E5-2r 1/1; main H1 9/10 — computed from the two results files.

### Inferences
- On the main set the sibling edge buys **+1 question over the best shipped configuration** (G-080) and +1 over typed expansion's own gains; the value concentrates almost entirely in the held-out set, exactly as the brief suspected.
- The misses a sibling edge reaches on the weaker main-set configurations (H1, X1) are the same shape as the held-out ones — the retriever found the right spec, scored the wrong artifact — which is why the mechanism looks strong against H1 (7/10) and weak against E5-2r (1/1): the reranker already fixes most of that class on the main set, and the held-out set is where it does not.
- G-080 is the cleanest single piece of evidence in favour of the objective's hypothesis: `verify→spec`, unreachable at any depth, no hyperlink and no typed link, reached by siblinghood. It is one question.

### Gaps
- The main set's stale qrels mean the per-config win/loss lists cannot be used to claim a current score; they are internally consistent only.
- The held-out results file contains no typed-expansion configuration (only S3, E5-1, X1, H1), so the comparison "shipped typed expansion vs sibling edge" is measured on the main set and only *reachability-modelled* on the held-out set.

---

## Q3. Does a same-anchor-id edge give a narrower, cheaper route to the same questions?

### Takeaway
No. The premise does not hold in this corpus: **zero** identifier-shaped anchors recur across the sibling artifacts of a spec directory, and the anchors that do recur are boilerplate section names plus a `T-nnn` namespace collision between `tasks.html` and `triage-log.html` that denotes *different objects*.

### Cited Findings
- Across all 258 spec directories there are 10,068 (directory, anchor) pairs; 321 of them (**3.2%**) have that anchor in more than one artifact of the directory — [corpus units](/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/corpus-c64d41c-units-v1-ep4-p250.json).
- Of those 321, the number whose anchor matches an identifier shape (`FR-`, `NFR-`, `SC-`, `D-`, `US-`, `R-`, `AC-` + digits) is **0**.
- The recurring anchors are, by shape: `changelog` 119, `questions` 104, `T-n` 86, `data` 7, `context` 5. By artifact pair: design+spec 116, tasks+triage-log 86, design+spec+tasks 75, design+spec+tasks+triage-log 36, spec+tasks 4, spec+tasks+triage-log 4.
- The `T-n` recurrences are not the same object. `specs/000-spectastic/tasks.html#T-001` is a work item ("Add the fold-proposal-tasks step … append a provenance-linked `<spec-task>` phase"); `specs/000-spectastic/triage-log.html#T-001` is a triage entry ("Closed by (a) Hotfix 15 Jun 2026 — flipped 002-validate-cli and 003-init-node-port to Accepted…") — unit text from [corpus units](/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/corpus-c64d41c-units-v1-ep4-p250.json).
- Widening to the whole spec (including `changes/**`) does produce 123 identifier recurrences, all of the form `spec.html#FR-002` ↔ `changes/**/proposal.html#FR-002` — the delta's rendered post-state. Examples: `002-validate-cli#FR-011`, `003-init-node-port#FR-002/#FR-009/#SC-001/#FR-007`, `004-npm-publish-workflow#SC-004`.
- Reach: `anchor-dir` 0/8 held-out misses, 0/3 main E5-1 misses, 0/10 main H1 misses. `anchor-spec` reaches exactly one held-out miss, H-009 — and H-009 is already in the typed baseline via `typed:delta`, which encodes the same `spec#FR-002 ↔ proposal#FR-002` pair with a type attached.
- Cost: `anchor-dir` fan-out per retrieved unit is mean 0.2 / median 0 / p90 1 / max 3; `anchor-spec` mean 1.6 / median 0 / p90 5 / max 26 — the cheapest mechanisms measured, cheaper than `href-uu`.

### Inferences
- The narrow edge is cheap and reaches nothing. In spectastic, `tasks.html` refers to a requirement by **hyperlink**, not by re-anchoring its id — so the identity the hypothesis assumes is already carried by the parsed `href`/`typed` graph, and the residual anchor recurrences are template section names.
- Worse than useless in one respect: an unqualified anchor-identity edge would wire `tasks.html#T-001` to `triage-log.html#T-001`, and `spec.html#changelog` to `tasks.html#changelog` and `triage-log.html#changelog` — 86 + 119 wrong or vacuous edges against 0 right ones inside a directory.
- The only anchor identity worth anything (`spec#FR-n ↔ proposal#FR-n`) is better expressed as what it already is, a typed `delta` edge, because the type carries the bitemporal direction that a bare id match loses.

### Gaps
- None material. The question is answered negatively by construction over the whole corpus, not by sampling.

---

## Q4. What would a sibling-artifact edge cost in precision?

### Takeaway
Between one and two orders of magnitude more candidates than a typed link — median 242 extra units per query against the cap of 5 — and the one broad-neighbour expansion that was actually measured won 1 question and lost 9.

### Cited Findings
- The shipped expansion is capped at `EXPAND_MAX = 5` units, seeded from `pinned_keys[0]`, following unit↔unit `typed:*` edges with weights `validates 2.0, cites 2.0, delta 2.0, target 1.5, defer-to 1.5` — [pipeline.py](origin/episode-4-prototype:tools/ratchet-proto/ratchet/pipeline.py).
- Fan-out per retrieved unit, over the 118 distinct units in the held-out E5-1 top-10s (and 483 for main E5-2r):

| mechanism | held-out: mean / med / p90 / max | main: mean / med / p90 / max |
|---|---|---|
| `anchor-dir` | 0.2 / 0 / 1 / 3 | 0.2 / 0 / 0 / 3 |
| `typed-uu` | **0.6 / 0 / 2 / 11** | 0.4 / 0 / 1 / 16 |
| `anchor-spec` | 1.6 / 0 / 5 / 26 | 1.6 / 0 / 2 / 35 |
| `href-uu` | 2.2 / 1 / 6 / 15 | 1.8 / 1 / 5 / 16 |
| `typed+href` uu | 2.8 / 2 / 8 / 19 | 2.2 / 1 / 6 / 22 |
| `same-page` | 18.4 / 18 / 31 / 38 | — |
| `schema3` | 18.4 / 18 / 48 / 68 | 21.9 / 18 / 53 / 129 |
| `sibling-dir`, anchored targets only | 28.3 / 25 / 56 / 103 | 32.1 / 30 / 55 / 121 |
| `spec-hub` | 31.9 / 29 / 60 / 133 | 36.6 / 29 / 65 / 134 |
| `sibling-dir` | **48.0 / 47 / 84 / 148** | 53.7 / 53 / 86 / 155 |
| `sibling-spec` | **99.5 / 71 / 248 / 487** | 101.4 / 66 / 178 / 488 |

- Per-query candidate set, deduplicated over all 10 retrieved units and minus what was already retrieved (held-out n=13; main n=47): `typed-uu` mean 4.6 / med 3 / max 11 (main 2.7 / 1 / 17); `typed+href` 18.5 / 18 / 39; `sibling-dir` **240.8 / 242 / 366** (main 179.2 / 134 / 580); `sibling-spec` 436.8 / 318 / 899 (main 281.8 / 189 / 1010); `anchor-dir` 1.5 / 1 / 4.
- Corpus shape for scale: 10,540 distinct unit addresses across 627 pages, 258 spec directories, 121 specs; units per page mean 16.8 / median 15 / max 97; units per spec directory mean 40.9 / median 13 / p90 88 / max 171; units per spec mean 87.1 / max 497 — [corpus units](/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/corpus-c64d41c-units-v1-ep4-p250.json).
- So a `sibling-dir` expansion offers ~2.3% of the corpus as candidates per query (240.8 / 10,540), against the typed expansion's 0.04% (4.6 / 10,540) — a **52× larger candidate set per query**, or 80× per retrieved unit.
- **Measured, not modelled:** config EpiB (`rrf_neighbours`: seeds = top 5 of the final list, *all* link types, page-ended edges collapsed to the best-BM25 unit in that page, `add = 5`, RRF `k = 60` at weight 0.5) scores **36/47** against E5-1's 44/47 on the main set under identical qrels. It won **G-029** and lost **nine**: G-002, G-007, G-017, G-021, G-022, G-023, G-025, G-034, G-036 — 4 identifier, 2 single-fact, 2 relation, 1 paraphrase — computed from [main results](origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr.jsonl); the docstring itself records "measured worse before" — [pipeline.py](origin/episode-4-prototype:tools/ratchet-proto/ratchet/pipeline.py).
- By contrast the typed anchored expansion (E5-2r) won G-029 and G-030 and lost **zero** on the same qrels.
- Lifecycle composition of the candidate pool (union over all held-out top-10s): `sibling-dir` 2,622 units, **100% current** (2,611 current, 9 superseded, 2 rejected), 0 superseded-per-`supersede`-map; `sibling-spec` 3,751 units, 76% current, **21% history, 2% withdrawn**, 75 addresses in the supersession map; `typed-uu` 67 units, 63% current, 33% history, 19 superseded. Corpus-wide lifecycle: 8,998 current, 1,363 history, 95 superseded, 81 withdrawn, 3 rejected.
- Narrowing `sibling-dir` to identifier-anchored targets only cuts fan-out 48.0 → 28.3 per unit and keeps all 6 held-out reaches, but on the main set it loses G-080 (whose expected address is `spec.html#context`, a prose section) — `sibling-dir-anchored-only` reaches 2/3 E5-1 misses vs 3/3, and 6/10 H1 misses vs 7/10.

### Inferences
- Cost ordering is unambiguous: `anchor-* « typed « href « same-page ≈ schema3 « spec-hub « sibling-dir « sibling-spec`. The mechanism with the best reach-per-candidate on the held-out set is `same-page` (4/8 reach at median 18 fan-out, seeds in the top-4), then `schema3` (4/8 at the same cost), then `sibling-dir` (6/8 at median 47).
- EpiB is the closest thing to an experiment on this proposal and it is discouraging: its page-end collapse (`_best_in_page`) already behaves much like a sibling-artifact edge, since `href:same-spec` is the largest edge class at 6,754 of 12,283. It converted exactly one of the three reachable main-set misses and displaced nine hits. The failure mode is visible in the loss list — 4 of 9 lost questions are `identifier` stratum, i.e. pinned lookups whose correct unit got pushed out of the top 4 by neighbours merged at weight 0.5.
- The precision argument is therefore not "sibling candidates are bad passages"; it is that 240 candidates per query cannot be inserted into a top-4 budget without a scoring stage strong enough to re-order them, and the only scoring stage measured on this shape (RRF by link weight / seed rank) was not.
- The two sibling variants differ sharply in staleness risk, and in the direction that matters: the cheap one (`sibling-dir`) is 100% current because `changes/` is a subdirectory, while the only variant that reaches the two withdrawn-proposal history questions (`sibling-spec`) is 23% history-or-withdrawn and drags 75 superseded addresses into the pool. Since `stale@4` is a ratchet gate and currently 0 everywhere on the held-out set, `sibling-spec` is the variant most likely to fail the gate it would be judged by.
- Restricting sibling targets to identifier anchors is a reasonable cost-halving move for the held-out set specifically (all 6 reaches survive), at the price of the one main-set question the mechanism uniquely wins.

### Gaps
- No measurement exists of a sibling-artifact edge actually scored by the cross-encoder (the stage that makes S5/E5-1 strong). EpiB used RRF by link weight, not reranking, so its −9 bounds a *cheap* implementation, not the proposal at its best. This is the single most valuable experiment the analysis cannot do from saved files.
- The fan-out figures count candidate *units*, not context tokens. Held-out `context_tokens_top4` is 854 mean / 1,157 max for E5-1; what a sibling expansion would do to that is not computable without choosing a selection policy.

---

## Q5. Which strata benefit — relation, history, or broad?

### Takeaway
Broad, but with a sharp split by variant: the cheap `sibling-dir` edge is a relation mechanism on the held-out set (5 of 5 relation misses, 1 of 3 history), and only the expensive `sibling-spec` variant is a history mechanism (it alone reaches the two withdrawn-proposal questions). On the main set the same edge rescues paraphrase, single-fact, relation and intent alike.

### Cited Findings
- Held-out strata: 8 relation, 5 history — [questions-heldout.jsonl](/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions-heldout.jsonl). E5-1's per-stratum Hit@4 is relation 3/8, history 2/5 — [heldout summary](origin/episode-4-prototype:tools/ratchet-proto/results/5090-qwen3-emb-8b-qwen3-rr-heldout.md).
- E5-1's 8 misses split 5 relation (H-001, H-003, H-005, H-007, H-013) and 3 history (H-009, H-010, H-012).
- `sibling-dir` reaches **5/5 relation misses** and **1/3 history** (H-012 only). `sibling-spec` reaches 5/5 and 3/3, the two extra being H-009 and H-010, whose expected passages both live in `changes/withdrawn/**/proposal.html`.
- `schema3` reaches 4 relation (H-003, H-005, H-007, H-013) and **0 history**. `same-page` reaches 4 relation and 0 history. `anchor-spec` reaches 0 relation and 1 history (H-009).
- Main set, H1 (stale), 10 misses: `sibling-dir` reaches 7 spanning **paraphrase** (G-011, G-012, G-017), **single-fact** (G-007), **relation** (G-030, G-036) and **intent** (G-080); it misses G-014 and G-015 (paraphrase) and G-078 (intent).
- Main set, E5-1 (stale), 3 misses: 2 relation (G-029, G-030) + 1 intent (G-080), all reached by `sibling-dir`.
- Main set, X1 (stale), 15 misses: `sibling-dir` reaches 6 — single-fact (G-007), paraphrase (G-012, G-017, G-020), relation (G-030), intent (G-080).
- The stratum label and the mechanism do not line up: the questions a sibling edge reaches are characterised by *where the answer sits*, not by what the question asks. Of the misses it fails to reach, G-078 (`specs/018-explain/spec.html#context`, rank 45) and G-014/G-015 have no retrieved unit anywhere in the right spec directory.

### Inferences
- The honest characterisation is a *failure-mode* mechanism, not a stratum mechanism: it fixes "the retriever found the right spec and scored the wrong artifact". That failure mode happens to dominate the held-out relation stratum, which is why it looks relation-shaped there, and it recurs across every main-set stratum once the reranker is removed (H1, X1).
- The history gain is specifically a `changes/` subtree gain, which argues that if this is built it should be built as two separate relations with different costs and different staleness treatment: a current-artifact sibling edge inside the directory (cheap, 100% current, relation-shaped) and a spec→proposal history edge into `changes/` (expensive, 23% non-current, history-shaped, and already partly covered by `typed:delta` and the `spec.html#changelog` hyperlink).
- H-010 is the weakest case for any edge: the only route to it from the top-10 is `spec.html#changelog → changes/withdrawn/**/proposal.html#risks`, which is a whole-page hyperlink from a changelog listing. It is reached by `sibling-spec` and by a page-level href, and by nothing narrower.

### Gaps
- Held-out strata are 8 + 5 questions, so every per-stratum figure here is a count out of single digits; "5/5 relation" is five questions, not a rate with a usable confidence interval. The main-set strata are larger but stale.
- `questions-time.jsonl` (24 rows) was not in scope and was not analysed; if any of it is bitemporal/history-shaped it would materially change the history-stratum picture.

---

## Summary table for the report writer

Held-out set, best configuration E5-1 (5/13 Hit@4, 8 misses), reach from the retrieved top-10 vs cost:

| mechanism | held-out misses reached | of which unreachable at any depth today | per-query extra candidates (med) | staleness in pool | measured evidence |
|---|---|---|---|---|---|
| `typed-uu` (today's baseline) | 2 — H-001, H-009 | 2 | 3 | 33% history | E5-2r won 2 / lost 0 on main |
| `typed+href` (baseline w/ links) | 3 — + H-007 | 2 (H-007 was rank 5) | 18 | — | — |
| `anchor-dir` (narrow, same id) | 0 | 0 | 1 | — | premise absent: 0 identifier recurrences |
| `anchor-spec` | 1 — H-009 (already typed) | 1 | 4 | — | — |
| `same-page` (within artifact) | 4 — H-001, H-003, H-005, H-013 | 4 | ~18 | current | — |
| `schema3` (the literal triple) | 4 — H-003, H-005, H-007, H-013 | 3 | ~18 | current | — |
| `sibling-dir` (brief's definition) | **6** — + H-001, H-012 | 5 | **242** | 100% current | EpiB-like broad expansion: won 1 / lost 9 on main |
| `sibling-spec` (incl. `changes/`) | **8** — + H-009, H-010 | 7 | **318** | 21% history, 2% withdrawn | — |

Unreachable by every mechanism tested: `specs/011-core-spec/design.html#D-002` (H-007) and `specs/008-core-principles/design.html#D-005` (H-013) — both cross-spec, so all@4 stays shut for those two even at `sibling-spec`.
