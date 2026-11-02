# Discovering undisclosed supplier links: a measured Neo4j vs non-graph spike

Everything here comes from code that ran on this machine today. Code, corpus and
raw outputs: `/Users/briancorbin/Code/rags/rags-spring-ai/tools/graph-spike/`.
Rerun with `bash run_all.sh`.

## Verdict first

**The graph wins the task, but not for the reason the experiment was designed to
test, and the win does not belong to the graph database.**

Four findings, in order of how much they should change your mind:

1. **The cardinality claim is not what licenses the elimination.** The
   sensitivity run says so outright: remove the "four of five" claim entirely
   and the structured arms *still* return the correct four and the correct
   excluded one. The advantage is the ability to express **absence**, not the
   count. The count turned out to matter for something else — detecting that
   your extraction is wrong (see finding 4).
2. **SQLite and DuckDB match Neo4j exactly, in every configuration, 200–1000x
   faster.** Same answer, same ranking, same evidence gradient. The win belongs
   to *having a structured layer*, not to a graph database.
3. **The strongest non-graph arm is at chance on the decision that matters.**
   B3 looked like a clean win (exact set match, 3/3 reps) and it is an artefact:
   three of the five candidates tie at 0.0 evidence, so the cut is decided by
   candidate list order. Across all 120 orderings B3 is correct **40/120 = 33%**,
   exactly 1-in-3 chance.
4. **LLM relation extraction is the weak link, and unguarded it is dangerous.**
   On the claim document the local 8B model hallucinated named supply edges to
   **all five** producers, including the one that is the answer. The primary run
   only survived because a corrupted JSON token truncated the hallucination
   before it reached that fifth edge. The graph arm is sound only with a
   provenance check; without it, it is right by luck.

So: a structured layer earns its place on this task. Neo4j specifically does
not, on this data size. And the honest headline is not "graphs win" but
"**absence is only expressible over a structured, fully-extracted
representation, and a passage retriever has nowhere to put it**".

## The task and the corpus

Supplier **Kestrel Memory Works** claims it supplies memory to "four of the
world's five top GPU producers". Which four, and which one is out? All companies
are fictional. Ground truth lives in `truth/ground_truth.json`, which no arm
reads — only `score.py` opens it.

- supplied: Tessara Graphics, Nyxvolt Systems, Orbitine Compute, Halcyon Microsystems
- excluded: **Veradyne Silicon N.V.**

119 documents, 31–412 words (median 147), seed 20261029. Evidence is planted
asymmetrically so there is a real gradient rather than five identical cases:

| producer | evidence | documents |
| --- | --- | --- |
| Tessara | explicit joint press release | TSG-2026-031 |
| Orbitine | named supplier of record, buried in a long statutory filing | ORB-SCD-2026 |
| Nyxvolt Systems | 3-document chain, never jointly stated | KMW-2026-009 + NVS-2026-022 + NVS-JOB-4471 |
| Halcyon | informal alias in an earnings call + a joint conference session | KMW-CALL-Q3 + GMS-2026-AGENDA |
| Veradyne | **none**, only decoys | — |

Traps: five decoys co-mentioning Kestrel and Veradyne with no supply relation
(two lawsuits, a standards working group, a trade body board, a software-only
partnership); three-way entity resolution ("Nyxvolt Labs" must not merge into
"Nyxvolt Systems", "Halcyon Micro" must merge into "Halcyon Microsystems",
"Kestrel Analytics Group" must not merge into "Kestrel Memory Works"); three
non-GPU Kestrel customers so naive edge-counting answers 7; seven look-alike
statutory filings so the one naming Kestrel is not identifiable by shape; and
competing suppliers announced for every producer including Veradyne. The filings
use explicitly threshold-based supplier lists ("above ten per cent of spend"), so
no document is negative evidence and the elimination genuinely rests on
absence-plus-count rather than on a stated exclusion.

## Results

### Primary scores (`out/scores.json`, `out/scores.txt`)

```
arm                            exact    excl-ok   prec   rec    decoy  expressible
graph_neo4j[strict]            YES      YES       1.0    1.0    -      yes
graph_neo4j[prov]              no       no        1.0    0.75   -      yes
graph_neo4j[full]              YES      YES       1.0    1.0    -      yes
graph_neo4j[oracle]            YES      YES       1.0    1.0    -      yes
graph_neo4j[halluc_strict]     no       no        0.8    1.0    FP     yes
graph_neo4j[halluc_full]       YES      YES       1.0    1.0    -      yes
sql_sqlite[strict]             YES      YES       1.0    1.0    -      yes
sql_sqlite[prov]               no       no        1.0    0.75   -      yes
sql_sqlite[full]               YES      YES       1.0    1.0    -      yes
sql_sqlite[oracle]             YES      YES       1.0    1.0    -      yes
retrieval_B1_rep0/1/2          no       no        0.4    0.5    FP     NO
retrieval_B2_rep0/1/2          no       no        1.0    0.5    -      NO
retrieval_B3_rep0/1/2          YES      YES       1.0    1.0    -      yes   <- see caveat
```

Configuration ladder: `strict` = raw extraction; `prov` = + provenance check
(every edge endpoint must appear in its own source document); `full` = + direction
repair; `oracle` = + the aggregate claim the extractor never captured;
`halluc_*` = the counterfactual where the model's full hallucination survives.

### The graph's answer, and the ranking that produces it

`Q8_absence_ranked` on the `full` configuration — the whole point of the exercise:

```
producer                       supply_evidence  non_supply_comentions  no_supply_evidence_at_all
"Veradyne Silicon N.V."        0.0              2                      TRUE
"Halcyon Microsystems"         0.25             0                      FALSE
"Orbitine Compute AB"          1.0              0                      FALSE
"Tessara Graphics Corporation" 1.0              0                      FALSE
"Nyxvolt Systems Inc."         1.1              0                      FALSE
```

That recovers the planted gradient exactly: Veradyne at zero with two non-supply
co-mentions, Halcyon weakest of the four. `Q11_absence_only_answer` turns it into
the answer: `4, 1, [the correct four], ["Veradyne Silicon N.V."]`.

The evidence routes, each a query in `cypher/queries.cypher`:

| route | weight | what it found |
| --- | --- | --- |
| `Q3` explicit `SUPPLIES` | 1.0 | Tessara (TSG-2026-031), Orbitine (ORB-SCD-2026) |
| `Q4` part chain, 2 hops | 0.6 | Nyxvolt uses KM-9, which Kestrel makes (NVS-JOB-4471) |
| `Q5` qualification chain, 3 hops | 0.5 | Kestrel's part qualified on AURORA, which Nyxvolt makes, customer unnamed |
| `Q6` weak signals | 0.25–0.4 | Halcyon co-presents with Kestrel (GMS-2026-AGENDA) |
| `Q7` decoys | **0.0** | Veradyne: 2 `LITIGATES_AGAINST` docs |

The decoy query is what makes this work. `Q7` keeps the lawsuit and
working-group edges and scores them zero, because the edge *type* carries the
semantics. Entity resolution held in both directions: "Kestrel Analytics Group"
and "Nyxvolt Labs" stayed separate from their near-duplicates and were typed
non-supply.

### The decoy trap, measured rather than asserted

`Q10_naive_cooccurrence` ranks the five by *any* edge to Kestrel, ignoring type —
what a system with no relation semantics sees:

```
producer                       any_edges  edge_types
"Veradyne Silicon N.V."        2          ["LITIGATES_AGAINST"]
"Halcyon Microsystems"         1          ["CO_PRESENTS"]
"Orbitine Compute AB"          1          ["SUPPLIES"]
"Tessara Graphics Corporation" 1          ["SUPPLIES"]
"Nyxvolt Systems Inc."         0          []
```

The excluded producer ranks **first**; a correct one ranks **last**. Independently,
BM25 over the passage index for "Kestrel Memory Works supply Veradyne Silicon"
returns the two lawsuit decoys as its top four chunks. B1 and B2 both retrieved
3 of the 5 decoy documents (`LIT-2026-0041`, `LIT-2026-0088`, `NXL-2026-005`).

### Which arm fell for the decoy

- **B1 (single vague query): yes.** All three reps returned
  `[Nyxvolt, Halcyon, Veradyne, Brightfell, Cindermere]` — it named the excluded
  producer as supplied, and added two companies that are not in the top five at
  all. Precision 0.4, recall 0.5.
- **B2 (per-pair): no.** It correctly answered "no" for Veradyne, citing absence
  of supporting passages. Credit where due: the targeted query defeats the decoy.
- **Graph, `halluc_strict`: yes**, via a different mechanism — not the decoy
  documents but the model's own hallucination (precision 0.8, FP on Veradyne).
- **Graph `full` / SQL `full`: no.**

### Where the non-graph arm cannot express the question

B1 and B2 are marked "expressible: NO" and that is not a scoring dodge:

- **B1** has no way to represent "which entity has no evidence". It ranks
  passages by similarity and reads the top-k. Absence of a passage is
  indistinguishable from absence of a fact, and nothing in the pipeline can
  quantify over the five candidates.
- **B2** gets per-pair verdicts but produces no answer to the actual question:
  with 2 "yes" verdicts it cannot say which one of five is excluded, so
  `excluded = None`. It found Tessara and Orbitine (the two with named evidence)
  and missed Nyxvolt and Halcyon entirely — recall 0.5, stable across 3 reps.
- **B3** can express it, because the cardinality constraint is applied in Python
  over an explicit candidate set. That is the honest comparison, and it is the
  arm that matters.

**The B3 caveat, which is the real result.** B3's exact set match is not
evidence-driven. Its scores were Tessara 2.0, Orbitine 2.0, and Nyxvolt, Halcyon
and Veradyne **all tied at 0.0**. The four it keeps are therefore decided by
stable-sort order, i.e. by the order I happened to write the candidate list.
`b3_tiebreak_test.py` recomputes B3 over all 120 permutations using the stored
verdicts:

```
exact-correct on 40/120 candidate orderings (33%)
excluded chosen: {'Veradyne': 40, 'Halcyon': 40, 'Nyxvolt': 40}
```

Chance. B3 cannot distinguish "no evidence exists" from "retrieval did not find
it", and for three of five candidates it had nothing. That is the structural gap:
absence in a graph means "no edge after extracting the whole corpus"; absence in
a passage retriever means "not in my top-k".

### Sensitivity: remove the aggregate claim (`out/scores_noagg.json`)

```
graph_neo4j[noagg]             YES      YES       1.0    1.0    -      yes
sql_sqlite[noagg]              YES      YES       1.0    1.0    -      yes
retrieval_B3_rep0              no       no        -      -      -      yes   (undefined)
```

**The graph's advantage did not collapse, so per your instruction I am saying so
plainly: the advantage was something else.** It is absence, not cardinality.
Veradyne is the *unique* producer with zero supply evidence, so elimination needs
no count. B3, by contrast, becomes undefined without the claim — it has no
constraint to apply and collapses to B2.

This was not visible as a design flaw up front, and it is worth keeping: the
count becomes load-bearing precisely when extraction is noisy enough to give
*all five* candidates some evidence. Which is exactly what happened.

### What the count is actually for: detecting bad extraction

With the hallucinated edges present *and* the claim available, `Q9_elimination`
returns:

```
n, m, n_with_evidence, claim_satisfied, supplied, excluded
4, 5, 5, FALSE, [all five producers], []
```

`claim_satisfied = FALSE`. The stated count did not produce the answer; it
**caught the contradiction** — five producers with evidence against a claim of
four. That is a genuinely useful role for an aggregate claim in a KG pipeline,
and it is not the role the experiment was designed around. A passage retriever
has no equivalent self-check.

### Cost

| | graph (Neo4j 2026.10.1) | relational (SQLite 3.51 / DuckDB 1.5) | non-graph (BM25 + dense) |
| --- | --- | --- | --- |
| index / load | 3.14s load (52 nodes, 113 edges) | **0.0098s** | 0.01s BM25 + 5.90s dense, 243 chunks |
| query | 1.15–1.53s (mean 1.33s over 13 queries); `Q8` 1.50s | **0.0001–0.0067s**; `S5` 0.0067s | B1 29.4s; B2 142.3s (5 pairs); B3 +0s |
| shared prerequisite | extraction: **1327s** for 119 docs, qwen3:8b, 3 workers | same 1327s | none |
| sloc | 901 (common 252 + graphbuild 204 + extract 167 + arm_graph 154 + cypher 124) | 965 (…+ arm_sql 342, SQL inline) | **440** (common 252 + arm_retrieval 188) |

Neo4j's ~1.3s per query is essentially all JVM/`cypher-shell` process startup;
this data is far too small to show a graph engine any advantage. SQL is 200–1000x
faster on the same relations and the same logic.

The non-graph arm is half the code and needs no extraction pass — a real
advantage that the accuracy result then throws away. The 1327s extraction is the
dominant cost of both structured arms, and it is also where all the errors are.

## Extraction defects: the honest part

Five real defects, all found by running it, not by reading about it.

1. **Hallucinating named customers from an unnamed count — the serious one.**
   On `KMW-2026-014` the model read "four of the world's five top GPU producers"
   and emitted concrete `SUPPLIES` edges to **all five** producers, Veradyne
   included. Measured stability (`out/claim_stability.txt`): **0/5 reps correct**,
   all five reps hallucinating, and *none* of the five capturing the aggregate
   claim the document exists to state. The prompt explicitly forbids this and
   gives the rule twice.

   The primary run survived only because the raw response contained a corrupted
   token (`"from":"Kest,`, see `out/relations_raw.json`) which desynchronised
   JSON string-parsing and truncated the array after three edges — dropping the
   Halcyon and Veradyne hallucinations. I reconstructed the uncorrupted case in
   `out/relations_hallucinated.json`:

   ```
   strict  : 5, 0, [all five], []            <- complete failure
   full    : 4, 1, [correct four], [Veradyne] <- provenance check saves it
   ```

   **Treat any unguarded LLM-extracted graph as untrustworthy.** The provenance
   check — every edge endpoint must appear in its source document — is cheap,
   generic, answer-agnostic, and load-bearing. It dropped 25 of 138 edges (18%),
   and every drop I inspected was a genuine extraction error.

2. **Supply direction inverted on "our suppliers were:" documents, and the
   prompt fix did not take.** "Orbitine's tier 1 suppliers were: Kestrel Memory
   Works" extracts as `Orbitine SUPPLIES Kestrel`. I added four worked examples
   *including that exact sentence shape*; it still inverted every time. The 8B
   model reads surface order. This costs Orbitine, which is why the `prov`
   configuration scores recall 0.75 — provenance alone removes the hallucination
   that was accidentally covering for it. `--repair-direction` re-orients
   `SUPPLIES` edges running GPUProducer→MemorySupplier using company-register
   information only; it flipped 4 edges. Reported separately rather than folded
   in silently.

3. **Prompt interference.** An earlier, shorter prompt *did* extract the
   aggregate claim correctly on this document. Adding the `MAKES_PART` and
   direction rules broke it. Extraction prompts are not monotonic: adding a rule
   can silently destroy an unrelated behaviour, and nothing warns you.

4. **Universe pollution.** The model faithfully extracted `RANKED_TOP` from noise
   documents ("recognised in the engineering category at this year's industry
   awards" → rank 1). My loader lumped all rankings into one `top5_gpu` node, so
   Sable Dynamics and Cobalt Ridge Networking entered the "top five" and broke
   the cardinality check. The extraction was right and my schema was wrong. Fixed
   by keying `Universe` on the ranking's own text and filtering in the query.

5. **Recall misses and spurious claims.** The earnings call lost the "Halcyon
   Micro programme" reference entirely (Halcyon survives on the conference
   session alone, hence its 0.25) and produced a bogus claim (`n=2, of_m=1`). The
   conference agenda produced a cross-product of spurious `CO_PRESENTS` pairs.
   The bogus claims are excluded by matching the claim's denominator to the
   universe size in the query, not by hand-picking.

### Two of my own bugs, recorded because they were silent

- `first_json` tried `{` before `[`, so a JSON array was parsed as its first
  object then rejected as not-a-list: **every document returned 0 relations** and
  the run looked healthy. Caught only because "0 rels" repeated.
- `graphbuild` spread company props over the key `kind`, which also named the
  node type, so every `Company` node was relabelled `GPUProducer`/`MemorySupplier`
  and all 13 edge `MERGE`s matched nothing. Neo4j reported success throughout.

Both would have produced a confident, wrong conclusion. Validating the query
layer against a hand-written relations file first (`arm_graph.py` on a 20-relation
fixture) is what surfaced them.

## Install notes

`brew install neo4j` was clean: neo4j 2026.10.1 plus `cypher-shell` and
`openjdk@21`. Podman was not needed. Start-up:

```bash
/opt/homebrew/opt/neo4j/bin/neo4j-admin dbms set-initial-password graphspike123
/opt/homebrew/opt/neo4j/bin/neo4j console &
```

One wrinkle: `cypher-shell` is at `/opt/homebrew/bin/cypher-shell`, not under
`/opt/homebrew/opt/neo4j/bin/`. `JAVA_HOME=/opt/homebrew/opt/openjdk@21` must be
set or the system JVM (Temurin 25) is picked up.

**No Python packages were installed and no virtual environment was created** —
every script is standard library only, including BM25 and cosine similarity. A
dense retriever was available locally (Ollama with `nomic-embed-text:v1.5`,
768-dim, the project's default), so the baseline is BM25 **plus** dense with
reciprocal rank fusion, not BM25 alone. Reader and extractor are local
`qwen3:8b`. Generation pinned: `temperature 0, top_p 1.0, top_k 1, seed 20261029,
keep_alive 20m`.

Nothing else heavyweight was installed. Not run, as instructed: the 5090, the
ratchet suites, the golden question sets. Nothing committed, staged or pushed;
still on `episode-5`; nothing outside `tools/graph-spike/` and this notes
directory was touched.

## Real-data sanity check — mechanism only, kept separate

Six genuinely public newsroom pages, fetched once each, 6s apart, descriptive
User-Agent, no crawling, 6/6 at HTTP 200. Downloads are in
`realdata_downloads/`, treated as untrusted data, with scripts elsewhere and
paths passed as arguments.

The same extraction prompt over real prose produced 16 typed relations across 6
documents: `USES_PART` 6, `CO_PRESENTS` 4, `QUALIFIED_ON` 3, `MAKES_PLATFORM` 2,
`SUPPLIES` 1. **The mechanism behaves the same way**: it produced typed edges,
and it kept supply distinct from co-mention — 1 supply edge against 4 non-supply
co-mention edges on pages that are mostly partnership announcements. One page
yielded nothing, which matches the controlled run's behaviour on index pages.

**This run asserts nothing about which real company supplies which, and nothing
in it may be used to claim that.** Six pages is incomplete evidence and that
inference is commercially sensitive. The limit is restated in `realdata_run.py`
and inside `out/realdata.json`; per-company detail there is explicitly labelled
`detail_not_a_finding`.

## What I could not get working, and what I would do next

- **I could not make the 8B extractor capture the aggregate claim.** 0/5 with the
  final prompt. Everything claim-gated (`Q9`, `S6`) is therefore reported from an
  explicitly labelled oracle injection (`inject_claim.py`), using the exact claim
  the same prompt produced on an earlier isolated run. Treat `oracle` rows as
  "what the graph does when extraction works", not as a measured result.
- **Extraction was not repeated at corpus scale.** 1327s per pass; the stability
  measurement covers one document over 5 reps, not all 119. The per-arm scores
  therefore have no error bars on the extraction step. The retrieval arm *was*
  run 3 times and was perfectly stable.
- **Next, in order:** (a) try a larger extraction model or a two-pass
  extract-then-verify loop and measure whether the claim and the directions
  survive — extraction quality dominates everything else here; (b) drop Neo4j and
  keep SQLite, since it matched on every configuration at a fraction of the cost,
  unless a future task needs variable-length path queries that SQL expresses
  badly; (c) build the adversarial corpus variant where two producers have zero
  evidence, which is the case that would actually make the cardinality constraint
  necessary rather than merely confirmatory.
- **Ask before I install anything else.** Nothing beyond Neo4j was added.

## Caveat on generalisation

This is one synthetic corpus of 119 short documents with a planted answer, run
once end to end. The corpus was written by me to be hard in specific ways, which
means it tests the mechanisms I thought of and not the ones I did not. The
decoy-inversion result (`Q10`) and the B3 tie-break result (33%) are the two I
would most expect to replicate, because both are structural rather than
model-dependent. The accuracy numbers themselves depend heavily on an 8B
extractor that demonstrably hallucinates, and should be read as "what this
pipeline does today", not as a bound on what graph retrieval can do.

---
---

# FOLLOW-UP: the variant where cardinality is genuinely necessary

**This is a separate experiment from everything above.** The primary run made
absence *sufficient*, because exactly one producer sat at zero evidence. This
follow-up removes that crutch: it builds corpora where **two** producers have
zero extracted evidence, so absence alone cannot pick the excluded one and the
count has to do real work.

Built by perturbing the same generator, not a second corpus, so the runs stay
comparable: `gen_corpus.py --two-zero`, `--claim-n N`, `--veradyne-evidence`.
Neo4j was **still running** from the primary session (verified on
`http://localhost:7474/`); no restart was needed.

## Verdict on the follow-up

**The count does become load-bearing, and the honest answer depends on whether
the count and the evidence gap happen to line up.** Three outcomes, all three
reported correctly by both engines:

- With two zeros and a claim of **4 of 5**, the count **narrows without
  closing**: it proves exactly one of the two zero-evidence producers is
  excluded, but cannot say which. The correct output is *ambiguous between
  Halcyon and Veradyne*, and that is what the queries report.
- With two zeros and a claim of **3 of 5**, the count **closes it**: the number
  of zero-evidence producers equals the number the claim excludes, so both are
  excluded and the answer is unique. Without the claim this same corpus is
  unresolvable. **This is the case where cardinality is necessary and
  sufficient, and it is the clean demonstration the primary run could not give.**
- With five producers carrying evidence against a claim of four, the
  contradiction is **detected and reported as unsatisfiable**, not absorbed.

**The structured-layer finding survives intact.** SQLite returned the same state
as Cypher in all six cells, at 1.328s vs 0.0005s — about 2600x faster. Moving
from absence-driven to cardinality-driven reasoning did **not** make the graph
database pull ahead.

## The three variants

| variant | perturbation | producers with evidence | zero-evidence | claim | expected |
| --- | --- | --- | --- | --- | --- |
| `ambig` | `--two-zero` | Tessara, Nyxvolt, Orbitine | Halcyon, **Veradyne** | 4 of 5 | ambiguous |
| `unique` | `--two-zero --claim-n 3` | Tessara, Nyxvolt, Orbitine | Halcyon, **Veradyne** | 3 of 5 | unique |
| `unsat` | `--veradyne-evidence` | all five | none | 4 of 5 | unsatisfiable |

`--two-zero` removes Halcyon's two evidence links (the "Halcyon Micro programme"
alias in the earnings call becomes "The co-packaged programme"; the joint
conference session becomes a Kestrel-only session). Halcyon is *still truly
supplied* — the evidence is simply no longer in the corpus, which is the
realistic case. Expected states are computed in the generator and stored in each
variant's own `truth_v_*/ground_truth.json` under `evidence_resolvability`.

Only 2–3 documents change per variant, so `variant_relations.py` re-extracts
just those and splices them into the primary `relations.json`. Extraction is
strictly per-document, so this is exactly equivalent to a full re-extraction, at
6–17s instead of 22 minutes.

## Results (`out/scores_variants.txt`)

```
variant    claim    expected        got(cypher)     state   set     agree  |E|    |Z|
ambig      claim    AMBIGUOUS       AMBIGUOUS       YES     YES     YES    3      2
ambig      noclaim  NO_CLAIM        NO_CLAIM        YES     YES     YES    3      2
unique     claim    UNIQUE          UNIQUE          YES     YES     YES    3      2
unique     noclaim  NO_CLAIM        NO_CLAIM        YES     YES     YES    3      2
unsat      claim    UNSATISFIABLE   UNSATISFIABLE   YES     YES     YES    5      0
unsat      noclaim  NO_CLAIM        NO_CLAIM        YES     YES     YES    5      0

ambiguous pool reported : ['Halcyon Microsystems', 'Veradyne Silicon N.V.']
true excluded inside it : True
cost: cypher Q12 1.328s vs sqlite S8 0.0005s
```

### 1. Does the count become load-bearing? Yes — and it narrows without closing in one case

Same corpus, claim toggled, `Q12_three_state`:

```
ambig / noclaim : NO_CLAIM   n=NULL m=5  |E|=3 |Z|=2   pool=NULL
ambig / claim   : AMBIGUOUS  n=4    m=5  |E|=3 |Z|=2   pool=[Veradyne, Halcyon]
```

Without the claim the arm knows only that three producers are supplied and two
are unaccounted for — it cannot say anything about exclusion. With the claim it
proves **exactly one** of those two is excluded. That is strictly more than
absence gives, so the count is doing real work. But it is not enough to name
which, and that is a logical limit, not an implementation failure: the two
candidates are genuinely indistinguishable on the available evidence.

The `unique` variant is where the count closes the question:

```
unique / noclaim : NO_CLAIM  |E|=3 |Z|=2   (unresolvable)
unique / claim   : UNIQUE    n=3 m=5 n_excluded=2  excluded_certain=[Veradyne, Halcyon]
```

Here `|Z| = m - n = 2`, so the zero-evidence set *is* the excluded set. Absence
alone cannot get there; the count is both necessary and sufficient. This answers
the question the primary experiment was built for and could not test.

### 2. Does it resolve to the right one? It correctly refuses to

In the `ambig` variant the queries do **not** silently return a first row. The
`state` column says `AMBIGUOUS`, `excluded_certain` is `NULL`, and
`ambiguous_pool` lists both candidates. The true excluded producer (Veradyne) is
inside the reported pool, so the arm is honest rather than wrong — but it has
not identified it, and it does not claim to have.

`Q13_enumerate_worlds` lists the consistent worlds explicitly:

```
excluded_candidate       supplied_in_this_world                                      n_worlds
"Halcyon Microsystems"   [Veradyne, Orbitine, Nyxvolt, Tessara]                      2
"Veradyne Silicon N.V."  [Halcyon, Orbitine, Nyxvolt, Tessara]                       2
```

One of those two worlds is the truth. Nothing in the corpus distinguishes them.
On the `unique` variant the same query correctly returns nothing, because there
is only one world.

This is the part worth keeping: **I had to go looking for this behaviour.** The
primary run's `Q9`/`Q11` would have returned a row here and it would have looked
like an answer. Returning rows is the failure mode; the state column is the fix.

### 3. Does the structured-layer finding still hold? Yes

The coordinator's hypothesis was that cardinality-driven reasoning is where a
solver-style approach might pull ahead of a single query. On this data it did not,
and neither did the graph:

| | Cypher `Q12` | SQLite `S8` | DuckDB `S8` |
| --- | --- | --- | --- |
| three-state result | correct in 6/6 | correct in 6/6, identical | identical |
| query time | 1.328–1.502s | **0.0005s** | sub-ms |
| load time | 3.68s | 0.0021s | — |

Both languages express the whole three-state rule in one statement. SQL is ~2600x
faster and the logic is the same length. The primary finding stands: **the win
belongs to having a structured layer, and the graph database is not required**,
including when the reasoning is cardinality-driven.

### 4. The three-state outcome: expressible, and here is exactly what it costs

`unique` / `ambiguous` / `unsatisfiable` are reportable from a single query in
both languages. With `E` = producers carrying evidence, `Z` = producers with
none, and the claim asserting `n` of `m`:

```
|E| > n       -> UNSATISFIABLE   (more evidence than the claim allows)
|Z| = m - n   -> UNIQUE          (Z is exactly the excluded set)
|Z| > m - n   -> AMBIGUOUS       (excluded is some subset of Z)
```

It cost about 30 lines of Cypher and 45 of SQL, and it works because of one
modelling assumption: evidence is **positive-only and monotone**, so every
excluded producer must come from `Z`. That single assumption is what collapses
the search to a counting problem.

**Where a single query stops being enough, concretely:**

- **Enumerating the consistent worlds.** `Q13` enumerates them with one `UNWIND`
  only because `m - n = 1`. For a general `k = m - n` you need `k` nested
  `UNWIND`s with an ordering predicate to suppress permutations — query text
  whose *shape* depends on the data — or recursion in Cypher / a recursive CTE in
  SQL. Neither language enumerates `choose(|Z|, k)` declaratively.
- **Non-monotone evidence.** Add a document that *denies* a supply relation and
  the partition into `E` and `Z` stops being a partition: a producer can carry
  both positive and negative evidence, and you need to reason over which
  combinations are jointly consistent rather than count set sizes.
- **Graded confidence.** My weights (1.0 explicit, 0.6 part chain, 0.25
  co-presentation) are thresholded at `> 0` to make the partition. The moment you
  want "supplied if confidence > 0.5, and tell me how the answer changes as the
  threshold moves", you are solving a family of problems, not running a query.
- **Multiple interacting claims.** One aggregate claim is a single constraint.
  Two or more ("4 of the top 5", "2 of the top 3") intersect, and the query would
  have to conjoin constraint sets rather than compare two integers.

So: **the three states are cheap today and the cost is concentrated entirely in
the monotone, single-constraint, threshold-free special case.** That is the
argument for a solver, stated as precisely as this spike can state it — not
"queries cannot do it", but "queries do it for free until any one of those four
assumptions breaks, and then the query shape becomes data-dependent, which is
the point at which a solver is cheaper than maintaining the SQL."

### 5. The unsatisfiable case, made deliberate

`--veradyne-evidence` plants a genuine joint supply release for Veradyne
(`VRD-2026-077`), so all five producers carry evidence against a claim of four:

```
state=UNSATISFIABLE  n=4  m=5  n_excluded=1  |E|=5  |Z|=0
supplied_certain=[all five producers]  excluded_certain=NULL  ambiguous_pool=NULL
```

The inconsistency is reported, not absorbed, and both `excluded_certain` and
`ambiguous_pool` are correctly `NULL` — the query declines to invent an excluded
producer. This confirms deliberately what the primary run found by accident via
the hallucination, and it is the same mechanism: `claim_satisfied = FALSE` there,
`UNSATISFIABLE` here.

Note what this means operationally: the arm cannot tell whether the *claim* is
false or the *extraction* is wrong. In the primary run it was extraction; here it
is the planted corpus. Distinguishing those two would need provenance-weighted
reasoning the current pipeline does not have, and that is a genuine limitation.

## Follow-up failures and limits

- **The extractor still never captures the aggregate claim.** 0/5 in the primary
  measurement, and in all three variants the claim document again produced the
  same three hallucinated named `SUPPLIES` edges and no claim — **including in the
  `unique` variant, whose text says "three of the world's five"**. It hallucinated
  the identical three companies regardless of the number in the sentence. Every
  claim-bearing cell above therefore uses the labelled oracle injection
  (`inject_claim.py --n`). The three-state machinery is sound; the thing feeding
  it is not.
- **`--two-zero` was verified by inspection, not by a separate check:** I
  confirmed `grep -c "Halcyon Micro programme"` is 1 in the primary corpus and 0
  in the variant, and that Halcyon's evidence count fell to zero in `|Z|=2`. I did
  not separately verify that no *other* Halcyon route survived by accident; the
  `|E|=3` figure is the evidence that none did.
- **The retrieval arm was not rerun on the variants.** It was already at chance
  on the primary corpus (33% over candidate orderings) and these variants only
  remove evidence, so it can only do worse. I did not spend the ~25 minutes to
  confirm that, and so I am not reporting a number for it.
- **One run per cell.** Extraction variance is not characterised on the variants;
  the spliced documents were extracted once each.

## Exact commands for the follow-up

```bash
cd /Users/briancorbin/Code/rags/rags-spring-ai/tools/graph-spike
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
# steps 10-14 of run_all.sh, or in full:
python3 -I gen_corpus.py --out corpus_v_ambig  --truth truth_v_ambig  --seed 20261029 --two-zero
python3 -I gen_corpus.py --out corpus_v_unique --truth truth_v_unique --seed 20261029 --two-zero --claim-n 3
python3 -I gen_corpus.py --out corpus_v_unsat  --truth truth_v_unsat  --seed 20261029 --veradyne-evidence
for v in ambig unique unsat; do
  python3 -I variant_relations.py --base-corpus corpus --base-relations out/relations.json \
      --corpus corpus_v_$v --out out/relations_v_$v.json
done
python3 -I inject_claim.py --relations out/relations_v_ambig.json  --out out/relations_v_ambig_claim.json  --n 4
python3 -I inject_claim.py --relations out/relations_v_unique.json --out out/relations_v_unique_claim.json --n 3
python3 -I inject_claim.py --relations out/relations_v_unsat.json  --out out/relations_v_unsat_claim.json  --n 4
for v in ambig unique unsat; do
  python3 -I drop_claim_doc.py --relations out/relations_v_${v}_claim.json \
      --doc KMW-2026-014 --out out/relations_v_${v}_noclaim.json
  for c in claim noclaim; do
    python3 -I arm_graph.py --relations out/relations_v_${v}_${c}.json --corpus corpus_v_$v \
        --repair-direction --out out/graph_v_${v}_${c}.json --tag v_${v}_${c}
    python3 -I arm_sql.py   --relations out/relations_v_${v}_${c}.json --corpus corpus_v_$v \
        --repair-direction --out out/sql_v_${v}_${c}.json --tag v_${v}_${c}
  done
done
python3 -I score_variants.py --out out/scores_variants.json
```

The three-state queries themselves are `Q12_three_state` and
`Q13_enumerate_worlds` in `cypher/queries.cypher`, and `S8_three_state` in
`arm_sql.py`. Nothing was committed, staged or pushed; still on `episode-5`;
nothing outside `tools/graph-spike/` and this file was touched.
