# Declared vs latent relations in the spectastic archive: the corpus-wide edge inventory

Scope: static analysis of saved artefacts only. No model, embedding or retrieval stage was run. Everything
below is computed from four sources:

- Snapshot corpus with its edge graph — `/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/corpus-c64d41c-units-v1-ep4-p250.json`
  (commit `c64d41c2e5d0d42c4fefd1b506cb083b9812bf6c`, root `/Users/briancorbin/Code/rags/spectastic`).
- Bitemporal corpus — `/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/timecorpus-c64d41c-v1.json`.
- Parser and pipeline semantics — `git show origin/episode-4-prototype:tools/ratchet-proto/ratchet/{corpus,pipeline,idlookup,timecorpus}.py`.
- Golden sets — `/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions{,-heldout,-routing,-time}.jsonl` (53 + 13 + 15 + 24 = 105 rows).

**Units vs unit-states, stated once and then respected everywhere below.** Three different denominators are
in play and they are not interchangeable:

| denominator | value | what it is |
|---|---|---|
| unit-**parts** (`Item` rows in `corpus.units`) | **11,492** | retrievable rows; a unit over `PART_WORDS = 250` words is split |
| distinct unit **keys** (`<source>#<anchor>`, the graph's nodes) | **10,540** | the elements the graph actually addresses |
| **pages** | **627** | the other node kind in the graph (`["p", source]`) |
| unit-**states** (bitemporal) | **15,616** | one row per (address, interval it was in force) |
| distinct addresses ever (bitemporal) | **12,022** | 10,540 current + 1,482 closed |

Every graph figure below is over **unit keys (10,540)**, because `Graph.adj` is keyed on
`("u", "<source>#<anchor>")`, not on parts. Where I quote a part-level number I say so.

Reproduction: the four scripts are inlined in full at the end of each section. They live in
`/private/tmp/claude-501/-Users-briancorbin-Code-rags-rags-spring-ai/4f24c815-84f0-4ab3-aafb-a3f513e16c8c/scratchpad/`
(`inv.py`, `latent.py`, `latent2.py`, `trigger.py`, `bitemp.py`) and nothing was written into the repo
except this file.

---

## Q1. What edge kinds does the parser produce, with exact counts, coverage and fan-out?

### Takeaway
Eight kinds, 12,283 raw edge rows (11,258 distinct `(src,dst,kind)` triples) over 10,540 unit keys and 627
pages. The graph is *sparse at the unit level and saturated at the page level*: no kind covers more than 41.5%
of units, but `href:same-spec` touches **624 of 627 pages (99.5%)** — so any mechanism that collapses an
`href:same-spec` edge to its page (which is exactly what `_best_in_page` and config EpiB do) is operating on a
relation that is true of essentially every page in the archive and therefore carries almost no information.

### Cited Findings

**Per-kind counts, endpoint split and distinct triples** — computed from the `edges` key of
[corpus-c64d41c-units-v1-ep4-p250.json](/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/corpus-c64d41c-units-v1-ep4-p250.json):

| kind | raw rows | distinct triples | u→u | u→p | p→u | p→p |
|---|---|---|---|---|---|---|
| `href:same-spec` | 6,754 | 6,215 | 5,485 | 413 | 140 | 716 |
| `href:same-page` | 2,170 | 1,912 | 910 | 61 | 1,199 | 0 |
| `href:cross-spec` | 1,782 | 1,601 | 856 | 578 | 54 | 294 |
| `typed:cites` | 866 | 866 | **866** | 0 | 0 | 0 |
| `typed:target` | 255 | 214 | **255** | 0 | 0 | 0 |
| `typed:delta` | 168 | 168 | **168** | 0 | 0 | 0 |
| `typed:defer-to` | 166 | 160 | 0 | **166** | 0 | 0 |
| `typed:validates` | 122 | 122 | **122** | 0 | 0 | 0 |
| **total** | **12,283** | **11,258** | 8,662 | 1,218 | 1,393 | 1,010 |

- These eight kinds are the complete set the parser emits. `corpus._links` constructs `"href:" + kind` for
  `kind in {same-page, same-spec, cross-spec}` and `"typed:" + attr` for
  `attr in {validates, cites, target, defer-to}`, plus the special `typed:delta` for a `target=` on a
  `<spec-delta>` element — [corpus.py](origin/episode-4-prototype:tools/ratchet-proto/ratchet/corpus.py).
- There is a **ninth declared relation that is not an edge at all**: supersession, kept in two separate dicts
  rather than in `c.edges` — `supersede` (170 entries, unit key → unit key) and `page_supersede` (2 entries).
  `Graph` reads them directly and `redirect()` uses them; `typed_neighbours` never sees them — [corpus.py], [pipeline.py].
- 1,025 of the 12,283 rows are exact duplicates of another row (same source, destination and kind): 12,283 raw
  against 11,258 distinct triples. `Graph.__init__` appends every row, so duplicated rows appear twice in
  `self.adj`; `typed_neighbours` de-duplicates on the way out (`if … d not in out`) so the duplication is
  harmless but it means raw row counts overstate the relation count by ~8% — [pipeline.py].

**Fan-out and coverage per kind**, over unit nodes, on the symmetric adjacency `Graph.__init__` actually
builds (every edge appended in both directions), neighbours de-duplicated. "cov%" is the share of the 10,540
unit keys with at least one neighbour of that kind. The right-hand block restricts neighbours to *unit*
destinations, i.e. what an anchored expansion could actually land on:

| kind | units with edge | cov% | mean | median | max | — unit-neighbours only: units | cov% | mean | median | max |
|---|---|---|---|---|---|---|---|---|---|---|
| `href:same-spec` | 4,369 | 41.5 | 2.39 | 2 | 40 | 4,159 | 39.5 | 2.39 | 2 | 21 |
| `href:same-page` | 1,918 | 18.2 | 1.40 | 1 | 8 | 955 | 9.1 | 1.70 | 1 | 8 |
| `href:cross-spec` | 1,042 | 9.9 | 2.03 | 1 | 39 | 730 | 6.9 | 2.08 | 1 | 39 |
| `typed:cites` | 951 | 9.0 | 1.82 | 1 | 26 | 951 | 9.0 | 1.82 | 1 | 26 |
| `typed:target` | 326 | 3.1 | 1.31 | 1 | 4 | 326 | 3.1 | 1.31 | 1 | 4 |
| `typed:delta` | 305 | 2.9 | 1.10 | 1 | 6 | 305 | 2.9 | 1.10 | 1 | 6 |
| `typed:validates` | 175 | 1.7 | 1.39 | 1 | 12 | 175 | 1.7 | 1.39 | 1 | 12 |
| `typed:defer-to` | 81 | 0.8 | 1.98 | 1 | 8 | **0** | **0.0** | — | — | — |
| **any kind** | **6,168** | **58.5** | 2.94 | 2 | 48 | 5,188 | 49.2 | 3.04 | 2 | 39 |

- **4,372 unit keys (41.5%) have no edge of any kind**, and **5,352 (50.8%) have no unit→unit neighbour at
  all** — i.e. half the addressable corpus is invisible to any anchored one-hop expansion.
- `typed_neighbours()` — the only thing the shipped expansion follows — is **non-empty for 1,571 of 10,540
  unit keys (14.9%)**, mean 1.74, median 1.0, max 26. Only 84 of those 1,571 (5.3%) have more than
  `EXPAND_MAX = 5` typed unit neighbours, so **the cap almost never bites**: the constraint on expansion is
  the absence of edges, not the cap.
- **The near-universal kind is `href:same-spec`, and it is near-universal at page granularity.** Collapsing
  both endpoints to their page: `href:same-spec` yields 894 distinct page pairs touching **624 of 627 pages
  (99.5%)**, mean page fan-out 2.87, max 36. The other kinds, page-collapsed:
  `href:cross-spec` 736 pairs / 344 pages (54.9%); `typed:cites` 170 pairs / 255 pages (40.7%);
  `typed:defer-to` 137 pairs / 108 pages (17.2%); `typed:delta` 94 pairs / 138 pages (22.0%);
  `typed:target` 55 pairs / 91 pages (14.5%).
- At *unit* granularity `href:same-spec` is still selective: 5,485 u→u edges against roughly
  121 × (75 × 74 / 2) ≈ 3.4 × 10⁵ same-directory unit pairs, so ~1.6% of candidate pairs. A spec directory
  holds a mean of **75.2 units** (median 75, max 171) and a page a mean of **16.8** (median 15, max 97).

### Inferences
- The information in `href:same-spec` lives entirely in **which unit** it points at, not in which page. A
  page-collapsed `href:same-spec` edge says "this page is in the same spec directory as that one", which is
  true of 99.5% of pages and therefore says nothing. This is a direct, mechanical explanation for why config
  EpiB — which collapses page-ended edges to the best-BM25 unit in the page and fuses at weight 0.5 — scored
  36/47 against E5-1's 44/47 and displaced nine hits: it diluted the ranking with a relation of near-zero
  selectivity ([edge_value_measurement.md](/Users/briancorbin/Code/rags/rags-spring-ai/research_notes/Schema%20implied%20relations%20in%20spectastic/edge_value_measurement.md)
  already reports the 36/47 and the nine losses; the 99.5% page coverage is the reason).
- The typed kinds are the opposite: all five are 100% single-endpoint-typed (every `typed:cites`,
  `typed:target`, `typed:delta`, `typed:validates` row is u→u; every `typed:defer-to` row is u→p), low
  fan-out (mean 1.10–1.98), and low coverage (0.8–9.0%). They are high-precision and almost absent. That is
  the whole shape of the problem: the declared typed graph is *correct* and *tiny*.
- `typed:defer-to` is structurally useless to `typed_neighbours` by construction, not by accident: the parser
  only ever emits it as `["p", f"specs/{x}/spec.html"]` (the `defer-to` branch builds a page node and nothing
  else), so all 166 rows are u→p and `typed_neighbours`'s `if kind == "u"` filter drops 100% of them. The
  `typed_pages` helper added later recovers them via `_best_in_page`.

### Reconciliation with the established notes
- **Exact agreement.** `declared_meta.md` line 288 states "12,283 edges total: `href:same-spec` 6754,
  `href:same-page` 2170, `href:cross-spec` 1782, `typed:cites` 866, `typed:target` 255, `typed:delta` 168,
  `typed:defer-to` 166, `typed:validates` 122" and "`typed:cites` entirely u→u (866), `typed:defer-to`
  entirely u→p (166), `typed:validates` entirely u→u (122)". My independent recomputation reproduces every
  one of those figures and extends the endpoint split to the remaining five kinds. No disagreement.
- **No disagreement, added precision.** `edge_value_measurement.md` reports `href:same-spec` as "the largest
  edge class at 6,754 of 12,283". Confirmed.

### Gaps
- `declared_meta.md` establishes that the parser **loses declared edges**: `target` 770 declared against
  255 + 168 = 423 extracted (~45% loss), `defer-to` 674 declared against 166 extracted (~75% loss), and four
  relation attributes (`spec-decision grounding` 469, `spec-parent specid` 22, `spec-risk status`/`by`
  360/242, `spec-triage layer` ~168) not extracted at all. I did not re-derive those declared-side counts
  (they require reading the spectastic source HTML, outside this brief's artefact scope) and I take them as
  given. They matter enormously to the conclusion and are flagged again in Q4.

### Script (`inv.py`)
```python
import json, collections, statistics as st
C='.../cache/corpus-c64d41c-units-v1-ep4-p250.json'
d=json.load(open(C)); units=d['units']; edges=d['edges']
keys=sorted({f"{u['source']}#{u['anchor']}" for u in units})
print("unit-PARTS:",len(units), "unit keys:",len(keys), "pages:",len(d['pages']), "edge rows:",len(edges))
ded={(tuple(s),tuple(dd),t) for s,dd,t in edges}
rows=collections.Counter(); dedc=collections.Counter(); endp=collections.defaultdict(collections.Counter)
for s,dd,t in edges: rows[t]+=1; endp[t][s[0]+'->'+dd[0]]+=1
for s,dd,t in ded: dedc[t]+=1
for t,c in rows.most_common():
    print(t, c, dedc[t], dict(endp[t]))
adj=collections.defaultdict(lambda: collections.defaultdict(set))
for s,dd,t in edges:
    s=tuple(s); dd=tuple(dd); adj[s][t].add(dd); adj[dd][t].add(s)   # mirrors Graph.__init__
nk=len(keys)
for t,_ in rows.most_common():
    fo=[len(adj[('u',k)][t]) for k in keys if adj[('u',k)].get(t)]
    fou=[len([x for x in adj[('u',k)][t] if x[0]=='u']) for k in keys if adj[('u',k)].get(t)]
    fou=[x for x in fou if x]
    print(t, len(fo), 100*len(fo)/nk, st.mean(fo), st.median(fo), max(fo),
          len(fou), (st.mean(fou), st.median(fou), max(fou)) if fou else None)
tn=[]
for k in keys:
    s=set()
    for t,v in adj[('u',k)].items():
        if t.startswith('typed:'): s |= {x for x in v if x[0]=='u'}
    if s: tn.append(len(s))
print("typed_neighbours non-empty:",len(tn), st.mean(tn), st.median(tn), max(tn),
      "over EXPAND_MAX=5:", sum(1 for x in tn if x>5))
```

---

## Q2. How many edges land unit→unit (usable by an anchored expansion) versus unit→whole-page (unusable)?

### Takeaway
**8,662 of 12,283 raw edge rows (70.5%) are unit→unit; 3,621 (29.5%) have at least one page end and are
unusable by an anchored expansion.** The loss is concentrated in two places: 100% of `typed:defer-to` (166
rows, the entire kind) and 19% of `href:same-spec` + 52% of `href:cross-spec`. H-012's unfollowable hyperlink
is not a one-off — it is a sample from a class of 1,218 u→p rows.

### Cited Findings
- Endpoint split over all 12,283 raw rows: **u→u 8,662 (70.5%)**, p→u 1,393 (11.3%), u→p 1,218 (9.9%),
  p→p 1,010 (8.2%). Anything with a page end: **3,621 (29.5%)** — computed from the `edges` key of
  [corpus-c64d41c-units-v1-ep4-p250.json](/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/corpus-c64d41c-units-v1-ep4-p250.json).
- Distinct unit→unit edge **triples** (after de-duplication): **7,956**.
- Direction matters for usability. An anchored expansion seeds at a *unit* and asks for its neighbours, so the
  rows that strand it are the **u→p** rows (1,218) and, on the symmetric adjacency, also the p→u rows read
  backwards. The p→u rows (1,393) are *mostly* benign in the other direction — they are page→unit, so a unit
  reached from them is fine, but the page end can never be a seed because `pinned_keys` only ever holds unit
  keys from `IdIndex.lookup` — [idlookup.py], [pipeline.py].
- Per-kind u→p share: `typed:defer-to` 166/166 = **100%**; `href:cross-spec` 578/1,782 = 32.4% (and a further
  294 p→p, so 49.0% of the kind has a page end on at least one side); `href:same-spec` 413/6,754 = 6.1%
  (plus 716 p→p and 140 p→u, so 19.0% of the kind); `href:same-page` 61/2,170 = 2.8% (plus 1,199 p→u).
- The `href:same-page` p→u figure (1,199) is an artefact of the parser's node resolution rather than of the
  markup: `node_of(source, a)` walks the anchor's ancestors for an id that is a unit, and returns
  `["p", source]` when none is found — which happens for links in a page's prologue, nav or `<main>` before the
  first id'd element. So 1,199 same-page links originate from "the page" rather than from a unit — [corpus.py].
- Why this is decisive and not cosmetic: `typed_neighbours` is `[d for (kind, d), w, t in self.adj[("u", key)]
  if kind == "u" and t.startswith("typed:")]`. The `kind == "u"` test is the exact line that discards every
  page-ended edge, and the docstring on `typed_pages` says so in terms: "All 166 `defer-to` edges are of this
  shape … `typed_neighbours` keeps only unit destinations, so until these are resolved to a unit the relation
  is parsed, stored and then dropped on the floor" — [pipeline.py](origin/episode-4-prototype:tools/ratchet-proto/ratchet/pipeline.py).
- Coverage consequence: restricting to unit neighbours drops the "any kind" coverage from **6,168 units
  (58.5%)** to **5,188 (49.2%)**, i.e. 980 unit keys have edges but none that an anchored expansion can
  traverse.

### Inferences
- The established note's explanation of H-012 ("`tasks.html#T-1404 --href:same-spec--> <proposal page>`, i.e.
  unit→**page** … that is why the parsed-edge baseline is 3 and not 4") generalises to roughly three in ten
  parsed relations. The fix is not a new relation type and not LLM extraction: it is (a) resolving a page-ended
  edge to a unit, which `_best_in_page` already does for `typed_pages` in `expand()`, and (b) making
  `node_of` fall back to the page's *first* unit rather than to the page, which would convert a large share of
  the 1,199 p→u `href:same-page` rows into u→u.
- `typed:defer-to` is the single highest-value repair per line of code: 166 rows, the whole kind, currently
  100% discarded by `typed_neighbours`, and `declared_meta.md` says the parser is additionally losing ~75% of
  the declared `defer-to` population (166 extracted of 674 declared). Resolving `defer-to` to a unit **and**
  fixing the extraction would take one typed relation from 166 page-ended rows to ~674 unit-ended ones.

### Gaps
- I did not measure how often `_best_in_page` picks the *right* unit, because that needs a retrieval run
  (BM25 over the page's units against the question) and the brief forbids running any retrieval stage.

---

## Q3. What relations are LATENT — informative but not declared as a link?

### Takeaway
Of the five candidates, three are **already declared at 72–100%** (prose identifier mentions 72.3%, sibling
artifact pairs 76.3% with the schema triple at 100%, changelog-by-date 76.3%), one is **entirely spurious**
(same identifier id across artifacts: 86 same-directory pairs, 0 declared, and all 86 are `tasks.html#T-nnn`
vs `triage-log.html#T-nnn` namespace collisions between unrelated items), and only one has real latent mass
(cross-spec concept co-reference: 3,760 spec pairs latent at the loosest threshold) — but that one has such
poor precision that at any usable threshold most of its surviving pairs are *already linked*.
**Composite: 5,074 of 6,963 concrete, instance-level candidate relations (72.9%) are already written down.**

### Cited Findings

**(a) The same identifier id in two artifacts of the same spec — the hypothesis is false.**
- Same-id pairs across different artifacts **within one `specs/<nnn-slug>/` directory**: **659**, of which
  **0** are joined by a declared unit→unit edge. But the 659 decompose as **86 ID-shaped**
  (`^[A-Z]+-\d+$`) and **573 section/slug anchors**.
- All **86** ID-shaped pairs are `('tasks.html', 'triage-log.html')` — not one other artifact pair occurs.
  They are namespace collisions, not relations. Verbatim check:
  `specs/016-theme-support/tasks.html#T-011` = "Move the colour layer to attribute-keyed modes: dark palette
  from `html.dark` → `[data-mode="dark"]` …" against `specs/016-theme-support/triage-log.html#T-011` = "The
  rule above every section heading has never rendered — `h2:first-of-type` matches every `h2` …". Unrelated
  items that happen to share a counter.
- The 573 section-anchor pairs are vacuous by inspection: `#changelog` 457, `#questions` 104, `#data` 7,
  `#context` 5. The archive's most-repeated anchors across pages are `#changelog` (521 distinct pages),
  `#questions` (360), `#approach` (248), `#risks` (237), `#scope` (137), `#deltas` (137).
- Widening to the whole spec **subtree** (including `changes/**/proposal.html`) gives **7,210** same-id pairs
  of which **169** are declared. This is where the one genuine instance lives: H-009's
  `specs/003-init-node-port/spec.html#FR-002 ↔ changes/withdrawn/…#FR-002`. The ratio is 169/7,210 = 2.3%.
- Independent confirmation from the established note: "each artifact type owns a disjoint id namespace
  (`FR`/`SC`/`NFR` in spec, `D`/`R` in design, `T` in tasks) … Only **2** ids are declared in both a
  `spec.html` and its sibling `design.html` across the entire archive" —
  [declared_meta.md](/Users/briancorbin/Code/rags/rags-spring-ai/research_notes/Schema%20implied%20relations%20in%20spectastic/declared_meta.md).

**(b) A bare identifier named in prose with no hyperlink — mostly already linked.**
Method: for every unit key, take the distinct `(FR|SC|NFR|D|T|P|R|TRIAGE)-\d+` tokens in its text, resolve
each with the parser's own preference order (same page, then `spec.html`, `tasks.html`, `design.html`,
`verify.html`, `triage-log.html` in the same directory), then ask whether a declared edge joins the two.
Because the corpus `text` field has already had its tags stripped, this counts *mentions* — hyperlinked and
bare alike — which is the right denominator for the question "is this relation already written down?".

| measure | count |
|---|---|
| (unit, distinct identifier token in its text) pairs | 13,583 |
| resolves to a real unit in the same directory | 9,831 |
| — unresolvable (no such unit anywhere in the directory) | 3,752 |
| resolves **within the same page** | 3,800 |
| resolves to a **different artifact in the same directory** | **6,031** |
| … joined by a declared unit→unit edge | **4,293 (71.2%)** |
| … joined by a declared unit→unit **or** unit→page edge | **4,363 (72.3%)** |
| … **LATENT** (no declared edge at all) | **1,668 (27.7%)** |

- Where the latent 1,668 sits: `design.html→spec.html` 684, `tasks.html→spec.html` 477, `tasks.html→design.html`
  224, `triage-log.html→spec.html` 112, `spec.html→tasks.html` 77, `spec.html→design.html` 46,
  `triage-log.html→tasks.html` 38, `design.html→tasks.html` 31, `triage-log.html→design.html` 20,
  `spec.html→triage-log.html` 10.
- Same-page mentions are the *least* declared: 2,957 not declared against 843 declared (22.2% declared). But a
  same-page relation is already reachable by the `same-page` mechanism without any edge, and
  [edge_value_measurement.md](/Users/briancorbin/Code/rags/rags-spring-ai/research_notes/Schema%20implied%20relations%20in%20spectastic/edge_value_measurement.md)
  measures that mechanism at 4/8 reach.
- Examples of genuinely latent cross-artifact mentions:
  `specs/000-spectastic/spec.html#changelog` names `T-019`/`T-020`/`T-021` which exist as
  `specs/000-spectastic/triage-log.html#T-019…021` with no declared edge;
  `specs/000-spectastic/triage-log.html#T-009` names `T-603` which exists as `…/tasks.html#T-603`, unlinked.
- The established note reaches the same conclusion from the markup side and quantifies the bare-token mass:
  `design.html` 494 bare FR + 162 bare SC, `tasks.html` 268 bare FR + 166 bare SC, `triage-log.html` 285 bare
  FR; and 1,675 bare `P-N` principle mentions against 202 hyperlinks (1:8) — [declared_meta.md].

**(c) Schema-implied sibling artifacts — 76.3% already joined, and the schema triple is 100% joined.**
Method: page-collapse every declared edge, then enumerate every sibling-artifact page pair inside each of the
121 `specs/<nnn-slug>/` directories (no `changes/`) and ask whether at least one declared edge joins the pair
in either direction.

| artifact pair | pairs | already joined | % |
|---|---|---|---|
| `spec.html` ↔ `tasks.html` | 119 | 119 | **100.0** |
| `design.html` ↔ `spec.html` | 111 | 111 | **100.0** |
| `design.html` ↔ `tasks.html` | 111 | 111 | **100.0** |
| `spec.html` ↔ `verify.html` | 98 | 98 | **100.0** |
| `tasks.html` ↔ `verify.html` | 98 | 98 | **100.0** |
| `spec.html` ↔ `triage-log.html` | 40 | 40 | **100.0** |
| `design.html` ↔ `triage-log.html` | 36 | 8 | 22.2 |
| `tasks.html` ↔ `triage-log.html` | 40 | 7 | 17.5 |
| `design.html` ↔ `verify.html` | 91 | **0** | **0.0** |
| `triage-log.html` ↔ `verify.html` | 28 | **0** | **0.0** |
| `roadmap.html` ↔ (4 others) | 4 | 0 | 0.0 |
| **total** | **776** | **592** | **76.3** |

- Page census behind those pairs (627 pages): `proposal.html` 137, `spec.html` 121, `tasks.html` 119,
  `design.html` 111, `verify.html` 98, `triage-log.html` 40, `roadmap.html` 1; **121 spec directories**.
- Current-artifact ↔ own `changes/**/proposal.html` page pairs: **565, of which 297 (52.6%) already joined**.
- So the *entire* undeclared sibling-page surface inside a directory is four relations:
  `design↔verify` (91), `triage-log↔verify` (28), `tasks↔triage-log` (33 of 40), `design↔triage-log` (28 of 36)
  — 180 of the 184 undeclared pairs, plus the 4 `roadmap` pairs.

**(e) `#changelog` entries naming a change by date — 76.3% already linked.**
- 521 `#changelog` units exist. **95** of them name at least one ISO date. Those produce **156**
  (changelog unit, date) pairs for which a `changes/**/<date>-<slug>/proposal.html` with that date actually
  exists in the same spec. **119 (76.3%)** already have a declared edge from that changelog unit to that
  proposal page or one of its units; **37 (23.7%)** are latent.
- The latent 37 are dominated by *proposal-to-proposal* changelog references — e.g.
  `specs/000-spectastic/changes/archive/2026-07-06-date-format-contract/proposal.html#changelog` names
  `2026-06-29`, which is `…/2026-06-29-record-reviewer-on-accept/proposal.html`, unlinked; and several cases
  where a proposal's own changelog names its own date (self-reference, vacuous).

**(d) Cross-spec concept co-reference — the only candidate with real latent mass, and it is imprecise.**
Method: 121 specs → 7,260 unordered spec pairs. "Distinctive token" = a lowercase word of ≥4 characters,
stopworded, occurring in **2 to 5** specs' unit text (4,450 such tokens out of a 17,332-token vocabulary).

| threshold (shared distinctive tokens) | spec pairs | already declared-linked | % | LATENT |
|---|---|---|---|---|
| ≥ 1 | 4,158 | 398 | 9.6 | 3,760 |
| ≥ 3 | 1,427 | 329 | 23.1 | 1,098 |
| ≥ 5 | 707 | 265 | 37.5 | 442 |
| ≥ 10 | 246 | 144 | 58.5 | 102 |
| ≥ 20 | 87 | 65 | **74.7** | 22 |

- Baseline: **421 of 7,260 spec pairs (5.8%)** are joined by at least one declared cross-spec edge.
- The precision/recall trade is brutal and monotone: the tighter the co-reference evidence, the more likely the
  pair is *already linked*. At ≥20 shared distinctive tokens, 74.7% of pairs are already declared — the same
  ratio as the sibling-page and changelog families.

### Inferences
- **Candidate (a) is dead and should be reported as a negative result.** It is not merely redundant, it is
  actively harmful: an unqualified anchor-identity rule inside a directory would wire 86 pairs of unrelated
  items plus 573 vacuous section-anchor pairs, against zero correct ones. This matches
  `edge_value_measurement.md`'s `anchor-dir` reach of **0/8** exactly, and its `anchor-spec` reach of 1/8 is
  the single `typed:delta`-encoded H-009 pair, which the declared graph already has.
- **The headline composite.** Summing the three instance-level families that are directly countable:
  6,031 cross-artifact prose mentions + 776 sibling page pairs + 156 changelog-date pairs = **6,963** candidate
  relation instances, of which 4,363 + 592 + 119 = **5,074 are already declared — 72.9%**. Round number for the
  report: **roughly seven in ten of the archive's concrete, instance-level relations are already written down
  as links**, and the declared share rises to 100% for the five sibling relations the schema actually mandates
  or conventionally implies.
- **The latent 27% is a long tail with a predictable shape**: `design→spec` and `tasks→spec` prose mentions
  (1,161 of 1,668), `design↔verify` and `*↔triage-log` sibling pairs (180 of 184), proposal-to-proposal
  changelog dates (37). All three are *families*, not individual judgements — which is exactly what makes them
  derivable rather than extractable (Q4).
- **The biggest genuinely missing relation is not in any of the five candidates.** `declared_meta.md` finds
  1,675 bare `P-N` principle mentions against 202 hyperlinks, and that `principles.html` **is not in the
  corpus at all** (the 627 pages are only `proposal/spec/tasks/design/verify/triage-log/roadmap`). I confirm
  the absence independently: no corpus page path matches `principles.html`; the 152 edges whose target path
  contains "principles" all point at `008-core-principles`'s own bundle. A missing *document* is not a graph
  problem and no amount of extraction fixes it.

### Gaps
- My (b) method cannot separate *bare* mentions from *hyperlinked* mentions, because `Item.text` is
  tag-stripped. So "71.2% of resolvable cross-artifact mentions are declared" conflates "this mention is itself
  the hyperlink" with "this mention is bare but some other hyperlink in the same unit joins the pair". Both
  readings support the same conclusion (the pair is reachable without new extraction), but the split would
  need the spectastic source HTML, which `declared_meta.md` already measured from that side.
- My (d) token-overlap operationalisation of "same concept" is crude. A better one (shared noun phrases,
  embedding similarity) is unavailable here: embeddings would be a model stage, which the brief forbids. The
  monotone trend is robust to the threshold, but the absolute latent counts at low thresholds should be read
  as an upper bound full of noise.
- I did not quantify (d) at *unit* granularity, only spec-pair granularity, so "two specs discuss the same
  concept" is not resolved to "these two passages do".

### Reconciliation with the established notes
- **Reconciled exactly, after first appearing to disagree.** `edge_value_measurement.md` says an unqualified
  anchor-identity edge would create "86 + 119 wrong or vacuous edges against 0 right ones inside a directory".
  My ID-shaped count is **86 — identical**. My section-anchor count is **573**, not 119. Decomposing the 457
  `#changelog` pairs by artifact pair resolves it: `('spec.html','tasks.html')` = **119**, then
  `design↔spec` 111, `design↔tasks` 111, `spec↔triage-log` 40, `tasks↔triage-log` 40, `design↔triage-log` 36.
  The note's "119" is the `spec.html#changelog ↔ tasks.html#changelog` pairs it names as its example; mine is
  the full section-anchor population across all artifact pairs and all repeated section anchors. **Scope
  difference, not a contradiction** — and the corrected figure strengthens the note's conclusion: the vacuous
  edge count is 659, not 205.
- **Consistent.** `declared_meta.md`'s href direction table (`tasks→spec` 1,413, `design→spec` 596,
  `spec→design` 2) is the page-level shadow of my (c) table: the 100%-declared sibling pairs are exactly the
  directions that table shows carrying traffic.
- No figure in either note is contradicted by anything above.

### Scripts (`latent.py`, `latent2.py`)
```python
# --- (a) and (b) ---
import json, collections, re
d=json.load(open(C)); units=d['units']; edges=d['edges']
key=lambda u: f"{u['source']}#{u['anchor']}"
keys={key(u) for u in units}
text=collections.defaultdict(list)
for u in units: text[key(u)].append(u['text'])
text={k:' '.join(v) for k,v in text.items()}
src_of={k:k.split('#')[0] for k in keys}
spec_of={k:'/'.join(src_of[k].split('/')[:2]) for k in keys}
pair_u=set(); pagepair=set()
for s,dd,t in edges:
    s=tuple(s); dd=tuple(dd)
    if s[0]=='u' and dd[0]=='u': pair_u.add(frozenset((s[1],dd[1])))
    for a,b in ((s,dd),(dd,s)):
        if a[0]=='u' and b[0]=='p': pagepair.add((a[1],b[1]))
declared=lambda a,b: frozenset((a,b)) in pair_u
# (a) same anchor, different artifact, same dir / same subtree
byid=collections.defaultdict(list)
for k in keys: byid[k.split('#',1)[1]].append(k)
ID=re.compile(r'^[A-Z]+-\d+$')
# ... pairwise over byid[a], skipping same source, splitting on same-dir vs same-subtree and ID.match(a)
# (b) prose mentions
TOK=re.compile(r'\b(FR|SC|NFR|D|T|P|R|TRIAGE)-\d+\b')
def resolve(k, ident):
    s=src_of[k]; dirn=s.rsplit('/',1)[0]
    for cand in [s]+[dirn+'/'+p for p in ('spec.html','tasks.html','design.html','verify.html','triage-log.html')]:
        kk=f"{cand}#{ident}"
        if kk in keys and kk!=k: return kk
for k,tx in text.items():
    for ident in {m.group(0) for m in TOK.finditer(tx)}:
        r=resolve(k,ident)                    # None -> unresolvable
        # same page / different artifact; declared(k,r) and the u->p fallback decide declared vs latent

# --- (c) sibling page pairs, (e) changelog dates ---
import itertools
pp=set()                                      # page-collapsed declared pairs
for s,dd,t in edges:
    a=src_of.get(s[1],s[1]) if s[0]=='u' else s[1]
    b=src_of.get(dd[1],dd[1]) if dd[0]=='u' else dd[1]
    if a!=b: pp.add(frozenset((a,b)))
bydir=collections.defaultdict(list)
for p in d['pages']: bydir[p.rsplit('/',1)[0]].append(p)
flat=[x for x in bydir if '/changes/' not in x]
for dd in flat:
    for a,b in itertools.combinations(sorted(bydir[dd]),2):
        joined = frozenset((a,b)) in pp       # tally by (artifact(a), artifact(b))
DATE=re.compile(r'\b(\d{4}-\d{2}-\d{2})\b')
PROP=re.compile(r'(specs/[^/]+)/changes/(?:archive/|withdrawn/)?(\d{4}-\d{2}-\d{2}-[a-z0-9-]+)/proposal\.html$')
# for each '#changelog' unit: dates in its text x proposals in its spec with that date -> declared?
```

---

## Q4. Which latent relation needs LLM extraction, versus a regex or the directory grammar?

### Takeaway
**None of the countable ones.** Every latent family quantified in Q3 is derivable by a short deterministic
parser: (b) is `re.compile(r'\b(FR|SC|NFR|D|T|P|R)-\d+\b')` plus the parser's existing `resolve_id` preference
order; (c) is `source.rsplit('/',1)[0]`; (e) is an ISO-date regex against the `changes/` directory listing;
(a) should not be built at all. Only (d) cross-spec concept co-reference is not deterministic — and it is the
one family whose precision collapses exactly where its recall lives.

### Cited Findings
- **(b) is an id regex plus code the parser already contains.** `corpus._links` already has
  `resolve_id(source, ident, prefer=("same","spec.html","tasks.html","design.html","verify.html"))` — the
  exact resolution function needed. Deriving the 1,668 latent prose mentions means calling that existing
  function on regex hits in the text instead of only on attribute values. The ambiguity is bounded and the
  established note bounds it: "the id namespaces are disjoint by type: an `FR-003` token in
  `specs/042/design.html` can only mean `specs/042/spec.html#FR-003`. The ambiguity is cross-spec … and that
  case is usually hyperlinked precisely because it needs the path" — [declared_meta.md].
  My measurement agrees: of 13,583 (unit, token) pairs, 9,831 resolve inside the directory and 3,752 do not —
  and the 3,752 non-resolving ones are where a cross-spec or `principles.html` reference would live.
- **(c) is the directory grammar, stated in the archive's own requirement.** `REQ-LIFECYCLE-005` identifies the
  bundle by path — "`spec.html`, `design.html`, and `tasks.html` **under `specs/<spec-id>/`**" — and the spec
  id format is pinned in code as `^[0-9]+-[a-z][a-z0-9-]*$` in `spec-parent-well-formed.ts` and
  `no-broken-defer-to.ts` — [declared_meta.md]. The derivation is one `rsplit`. The `siblings()` method in the
  shipped pipeline already does it: `d = source.rsplit("/", 1)[0]`, and its docstring names it an inferred
  relation — [pipeline.py].
- **(e) is a date regex against a directory listing.** Proposal paths are
  `specs/<id>/changes/[archive/|withdrawn/]<YYYY-MM-DD>-<slug>/proposal.html`, and
  `archive-paths.ts` encodes the depth convention explicitly ("a proposal lives at
  `specs/<id>/changes/<date>-<slug>/proposal.html` — 4 directory levels deep … so re-basing is simply 'strip
  the first two `../` segments'") — [declared_meta.md]. My script derived all 156 pairs with one regex.
- **(a) must not be built.** 86 ID-shaped same-directory pairs, 0 correct (verified verbatim above), plus 573
  vacuous section-anchor pairs. Reach 0/8 on the held-out misses — [edge_value_measurement.md].
- **(d) is the only non-deterministic family, and it is the weakest.** At ≥20 shared distinctive tokens,
  74.7% of its surviving pairs are already declared and only 22 are latent; at ≥1 token it offers 3,760 latent
  pairs but admits 4,158 of 7,260 possible spec pairs (57%), i.e. it is barely a filter. The declared baseline
  it would have to beat is 421 cross-spec-linked pairs.
- **What actually costs nothing and is being left on the table is DECLARED, not latent.** From
  [declared_meta.md]: `target` 770 declared / 423 extracted (~45% loss); `defer-to` 674 declared / 166
  extracted (~75% loss); and four relation attributes extracted not at all — `spec-decision grounding` (469),
  `spec-parent specid` (22, "the only *explicit* spec-to-spec structural hierarchy in the archive"),
  `spec-risk status`/`by` (360/242), `spec-triage layer` (~168). Adding those is ~**2,000 declared relation
  values** currently discarded, against a latent population of 1,668 prose mentions and 184 sibling pairs.

### Inferences
- **The decisive sentence for the report: a relation a 20-line parser can derive is not an argument for
  GraphRAG, and every latent relation in this archive that is both countable and informative is of that
  kind.** The archive is a *schema-governed* corpus: its relations are either declared in attributes and
  hyperlinks (72.9% of instance-level relations), or implied by a directory grammar that the archive's own
  requirements and TypeScript validators state in machine-readable form. An LLM extractor would be
  rediscovering a grammar that is already written down in `packages/schema/src/rules/*.ts`.
- **The one thing an LLM could add is the thing the measurement says not to add.** (d) is the only family
  needing semantic judgement, and it is the family where evidence strength and existing-declaration rate move
  together: the pairs an extractor would be most confident about are the pairs already linked. That is the
  structural reason a GraphRAG graph would mostly duplicate the declared graph at much higher cost.
- **Ordering of work, by measured value per unit of effort, with none of it GraphRAG:**
  1. Resolve page-ended edges to units (fixes ~1,218 u→p rows incl. 100% of `typed:defer-to`; H-012's class).
  2. Recover the ~2,000 declared-but-unextracted attribute values (`target`, `defer-to`, `grounding`, `specid`).
  3. Derive the sibling-directory relation (`sibling-spec` reaches **8/8** held-out misses —
     [edge_value_measurement.md] — and is one `rsplit`).
  4. Derive prose id mentions by regex (1,668 latent pairs).
  5. Add `principles.html` to the corpus (1,675 bare `P-N` mentions currently point at a document the corpus
     does not contain).
- The caution from the established note stands over all of this: the sibling relation's *reachability* of 8/8
  is a ceiling, and the one configuration measured on something like it (EpiB) scored 36/47 against 44/47. An
  edge that makes a passage a candidate is not an edge that makes it an answer.

### Gaps
- I cannot quantify what an LLM extractor would actually find, because running one is forbidden by the brief
  and no saved extraction artefact exists in the repo. The argument above is an argument from the *derivability
  of the latent population*, not a head-to-head measurement. That is the honest limit of this finding and the
  report should say so.

---

## Q5. What does the pipeline already exploit, and what gates it?

### Takeaway
Graph expansion is gated on a **pinned identifier**, and across all 105 questions only **12 (11%)** pin
anything, of which only **4 (4%)** have a first-pinned anchor with any typed neighbour or typed page at all.
The cap (`EXPAND_MAX = 5`) is essentially never the binding constraint — only 5.3% of the units that have
typed neighbours have more than five.

### Cited Findings
- **The trigger.** `final = self.expand(final, pinned_keys[0], q)` runs only `if cfg.expand and pinned_keys`.
  `pinned_keys = [k for k in self.idx.lookup(q, cfg.linker) if self._live_items(k, cfg.lifecycle)]` and is
  populated only inside `if cfg.ids:` — [pipeline.py](origin/episode-4-prototype:tools/ratchet-proto/ratchet/pipeline.py).
  So three things must all hold: the config enables `ids` and `expand`, the identifier lookup returns at least
  one key, and that key has at least one item surviving the lifecycle filter.
- **The seed is a single anchor: `pinned_keys[0]`** — the *first* pinned key in pin order, not all of them.
  Pin order is `(lifecycle rank, "/changes/" in source, corpus position)` with `current < history < excluded`,
  so the seed is deterministically the most-current, non-`changes/` instance — [pipeline.py], [idlookup.py].
- **The lookup rule.** `IdIndex.lookup`: ids are `(?<![\w-])(?P<prefix>[A-Z]+-)*[A-Z]+-?\d+(?![\w-])` tokens
  that are *also an anchor in the corpus*; specs are `spec|specs|slice \d{3,}` or a `\d{3,}-[a-z]` slug.
  "An id with a spec pins that id in that spec. An id with no spec pins only when the id exists in exactly one
  spec … A spec with no id pins nothing." Note that `lookup` calls `parse(question, self.anchors)` **without**
  `names`, so the declared-alias inventory never widens pinning — [idlookup.py].
- **The cap.** `EXPAND_MAX = 5`. Candidates are `typed_neighbours(anchor)` filtered to live items, plus one
  `_best_in_page` unit for each `typed_pages(anchor)`; they are ranked by BM25 among themselves and the first 5
  are inserted immediately after the anchor's own items. Edge weights
  `TYPED_WEIGHTS = {validates 2.0, cites 2.0, delta 2.0, target 1.5, defer-to 1.5}` exist but `expand()` does
  not read them — they are only consulted by `rrf_neighbours`-style configs. One hop only. Every list is cut
  to 50 — [pipeline.py].
- **How many of the 105 questions could possibly trigger it**, from a faithful re-implementation of
  `IdIndex.lookup(linker="ep4")` plus the `_live_items(k, lifecycle=True)` filter, run over all four golden
  files:

| file | n | pins something | **and** the seed has ≥1 typed neighbour or typed page |
|---|---|---|---|
| `questions.jsonl` (main) | 53 | 10 | **3** |
| `questions-heldout.jsonl` | 13 | 1 | **1** |
| `questions-routing.jsonl` | 15 | 0 | **0** |
| `questions-time.jsonl` | 24 | 1 | **0** |
| **total** | **105** | **12 (11.4%)** | **4 (3.8%)** |

- The 10 main-set pinning questions are exactly the **8 `identifier`-stratum questions plus 2 of the 7
  `relation`-stratum questions**. By stratum, questions naming a corpus id: `identifier` 8/8,
  `relation` 2/7, and **0** of `single-fact` (10), `paraphrase` (10), `stale` (7), `unanswerable` (6),
  `intent` (5). Held-out: `relation` 1/8, `history` 0/5.
- Independent agreement with the established note: "7 of the 8 misses have no pinned identifier, so the
  existing expansion trigger cannot fire on them at all" — [edge_value_measurement.md]. My recomputation gives
  1 of 13 held-out questions pinning, consistent with 7 of the 8 misses (H-001 is the pinning one, seeded at
  `#SC-002` at rank 1).
- The cap is slack: of the 1,571 unit keys with a non-empty `typed_neighbours`, **84 (5.3%)** exceed 5.
- Two other graph-adjacent stages exist and are gated differently. `siblings()` is seeded from
  `SIBLING_SEEDS = 5` top-ranked items' directories and capped at `SIBLING_MAX = 200` units handed to the
  reranker — it needs **no** pinned id, which is why it is the mechanism that can reach the 7 unpinned misses.
  `scope_units()` is capped at `SCOPE_MAX = 200` and fires only when the question "named exactly one spec, and
  no id that would pin". `context_ranking()` is gated on a `why`-shaped question
  (`\bwhy\b|what problem|what led|what prompted`), fires on 19 questions across the three sets, and only 4 of
  them want a `#context` — [pipeline.py].
- The one stage that exploits a declared relation without any trigger at all is `redirect()`: supersession,
  up to three hops, over the 170-entry `supersede` map and the 2-entry `page_supersede` map.

### Inferences
- **The gate, not the graph, is the binding constraint.** 3.8% of the question set can have anything inserted
  by the shipped expansion. Even a perfect LLM-extracted graph, inserted behind this trigger, would change at
  most 4 of 105 answers. Any argument for a richer graph is therefore first an argument for a *different
  trigger* — and `siblings()`/`scope_units()` are already that argument, built and measured, without a graph.
- The 12→4 collapse is the sharper half of the finding: even when a question *does* pin, two thirds of the
  time the pinned anchor has no typed edge. That is the 14.9% `typed_neighbours` coverage showing up in the
  question set. A pinned `FR-nnn` in a `spec.html` typically has `validates`/`cites` edges only if someone
  wrote them.
- Raising `EXPAND_MAX` would do nothing: the cap binds on 5.3% of eligible anchors, and on the 4 triggering
  questions the median typed fan-out is 1.

### Gaps
- My re-implementation of `lookup` is faithful to the `ep4` linker but I did not execute the prototype's own
  `IdIndex`, so a subtle divergence is possible. The cross-check that it is right: it reproduces exactly the
  8/8 `identifier`-stratum pin rate and the 1-of-13 held-out rate that `edge_value_measurement.md` implies.
- `questions-routing.jsonl` pins 0 of 15, which I report as computed; I did not investigate whether the
  routing set was deliberately constructed without identifier questions.

### Script (`trigger.py`, lookup portion)
```python
ID_TOKEN=re.compile(r"(?<![\w-])(?:[A-Z]+-)*[A-Z]+-?\d+(?![\w-])")
SPEC=re.compile(r"\b(spec|specs|slice)\s+(?P<in_prose>\d{3,})\b|(?<![\w-])(?P<in_slug>\d{3,})-[a-z]",re.I)
spec_num=lambda k:k.split('/')[1].split('-')[0]; LC={"current":0,"history":1}
EXCL={"withdrawn","superseded","rejected"}
def lookup(q):
    ids=[t for m in ID_TOKEN.finditer(q) for t in [m.group(0)] if t in anchors]
    specs=[m["in_prose"] or m["in_slug"] for m in SPEC.finditer(q)]
    out=[]
    for i in dict.fromkeys(ids):
        ks=by_anchor[i]
        if specs: ks=[k for k in ks if spec_num(k) in specs]
        elif len({spec_num(k) for k in ks})!=1: ks=[]
        out+=[k for k in ks if k not in out]
    return sorted(out,key=lambda k:(LC.get(units[first[k][0]]['lifecycle'],2),
                                    "/changes/" in k, units[first[k][0]]['n']))
live=lambda k:[n for n in first.get(k,[]) if units[n]['lifecycle'] not in EXCL]
# pins = bool([k for k in lookup(q) if live(k)]); can_add = typed_neighbours(seed) or typed_pages(seed)
```

---

## Q6. Does the bitemporal corpus change the edge picture?

### Takeaway
**Edges do not exist in the bitemporal corpus at all.** `timecorpus-c64d41c-v1.json` has exactly three keys —
`root`, `positions`, `states` — and `timecorpus.py` contains no mention of edges, links, graphs or
supersession. So edges carry **no validity interval**, and the consequence is measurable and large: of the
7,956 distinct unit→unit edges in the HEAD snapshot graph, only **27.6% had both endpoints in force at
position 300** of 721, and **20.9% at position 200**. An edge in this graph can and routinely does point at a
state that is not in force at the asked position.

### Cited Findings
- `timecorpus-c64d41c-v1.json` keys: `['root', 'positions', 'states']`. **15,616 unit-states**,
  **12,022 distinct addresses**, **722 first-parent positions** (max position 721). Grepping
  `git show origin/episode-4-prototype:tools/ratchet-proto/ratchet/timecorpus.py` for
  `edge|link|graph|supersede` returns one hit — the word "edge" in the prose phrase "converts a cutoff to a
  position once, at the edge". There is no edge data structure.
- `State` is `(address, source, anchor, spec, lifecycle, title, path, heading, text, part, parts, from_pos,
  to_pos)` with `to_pos` defaulting to `LIVE = 1 << 30`; the filter is `from_pos <= at < to_pos` — half-open,
  sentinel-ended — [timecorpus.py].
- Reconciliation of the two denominators: **11,492** of the 15,616 states are still in force
  (`to_pos == LIVE`) — identical to the snapshot's 11,492 unit-**parts**, confirming that a state is a part,
  not an element. **4,124** states have an explicit end. **0** snapshot addresses are missing from the time
  corpus; **1,482** historical addresses are closed and absent from the snapshot (the docstring's "98 of the
  725 spec pages that have ever existed are not in the current tree — 004's `plan.html` became
  `design.html`").
- **Anachronism of the snapshot graph, measured.** Taking each address's first `from_pos` and last `to_pos`
  from the 15,616 states, and asking of the 7,956 distinct snapshot u→u edges how many have *both* endpoints
  in force at position `p`:

| position `p` | both ends in force | % | anachronistic |
|---|---|---|---|
| 0 | 9 | 0.1 | 7,947 |
| 100 | 974 | 12.2 | 6,982 |
| 200 | 1,662 | 20.9 | 6,294 |
| 300 | 2,194 | 27.6 | 5,762 |
| 400 | 4,920 | 61.8 | 3,036 |
| 500 | 6,266 | 78.8 | 1,690 |
| 600 | 6,745 | 84.8 | 1,211 |
| 700 | 7,826 | 98.4 | 130 |
| 721 (HEAD) | 7,956 | 100.0 | 0 |

- **The sharper failure: the near end exists but the far end does not.** Edges whose source address is in force
  at `p` while the destination is not — at `p = 300`: `href:cross-spec` 208, `href:same-spec` 176,
  `href:same-page` 2 (386 total). At `p = 500`: `typed:cites` 20, `href:same-page` 11, `href:cross-spec` 5,
  `href:same-spec` 2 (38 total). These are exactly the dangling traversals: a time-filtered retrieval seeded
  on a unit that existed then, expanded along an edge parsed from the archive as it is *now*, landing on a
  passage that did not exist yet.
- Nine of the 24 time questions carry an explicit `cutoff` (`G-062` … `G-075`), with cutoffs ranging from
  `2026-06-20T23:59:59Z` to `2026-09-13T19:27:51Z`, against a HEAD `version.date` of `2026-09-14T18:30:23Z`.
  The time set's strata are `history` 7, `as-of` 5, `what-changed` 5, `as-known` 4, `history-decoy` 3.
- The expansion trigger cannot fire on them anyway: of the 24 time questions, 1 pins an identifier and **0**
  have a seed with a typed neighbour (Q5 table).

### Inferences
- **The bitemporal corpus does not change the edge picture; it exposes that there is no bitemporal edge
  picture.** Episode 5's graph and Episode 6's time model are, at the artefact level, disjoint: one has
  nodes-with-intervals and no edges, the other has edges and no intervals. Anything that wants to expand at a
  past position needs edges extracted *per revision*, or an edge interval derived as the intersection of its
  endpoints' intervals.
- The intersection is the cheap construction and the table above is exactly that computation. It is
  deterministic — no re-parse of history required for a *lower bound*, since an edge cannot be in force when an
  endpoint is absent. It is only a lower bound: an edge may also have been *added or removed* while both
  endpoints existed, which needs per-revision parsing to detect.
- The asymmetry in the table matters for how the risk should be described. Before ~position 400 the snapshot
  graph is mostly fiction (62% to 99.9% of its edges reference something not yet in force); after ~position 600
  it is ~85% sound. So the exposure is a function of how far back a question reaches, and the nine
  cutoff-bearing questions span the whole range.
- Because 0 of the 24 time questions can trigger expansion today, this is a **latent correctness bug, not a
  current scoring loss**. It becomes a real bug the moment the trigger is widened — which is precisely what
  `siblings()` and `scope_units()` do. A sibling-directory relation is itself time-sensitive: a page that did
  not exist at the cutoff is still a "sibling" by path.

### Reconciliation with the established notes
- Consistent with
  [internal_state.md](/Users/briancorbin/Code/rags/rags-spring-ai/research_notes/Episode%206%20links%20and%20bitemporality/internal_state.md),
  which records that the Java implementation "has supersession (one-directional, forward) and lifecycle, but no
  `validates`, `cites`, `defer-to` or `target` edges, no graph, no expansion step and no ordering" — the Java
  side has even less graph than the prototype, so nothing there supplies edge intervals either.
- The docstring of `timecorpus.py` quotes "**14,225 distinct unit-states against 10,540 units in the snapshot
  — 1.3x**". The saved artefact has **15,616** states. The 10,540 agrees exactly with my snapshot unit-key
  count. **I flag the 14,225 vs 15,616 difference loudly**: the docstring figure is stale relative to
  `timecorpus-c64d41c-v1.json` (the ratio is 1.48x, not 1.3x). This is a docstring-vs-artefact mismatch inside
  the prototype, not a disagreement with any research note, and the brief's own stated "~15,616 unit-states"
  matches the artefact. Anyone quoting the 1.3x ratio should recompute it as **15,616 / 10,540 = 1.48x**.

### Script (`bitemp.py`)
```python
import json, collections
d=json.load(open(CORPUS)); t=json.load(open(TIMECORPUS)); LIVE=1<<30
F=['address','source','anchor','spec','lifecycle','title','path','heading','text','part','parts','from_pos','to_pos']
S=[dict(zip(F,s)) for s in t['states']]        # states are stored as positional lists
print(len(S), len({s['address'] for s in S}), len(t['positions']), max(t['positions'].values()))
print([k for k in t if 'edge' in k or 'link' in k])      # -> []  : no edges, no intervals on edges
first=collections.defaultdict(lambda:LIVE); last=collections.defaultdict(lambda:-1)
for s in S:
    first[s['address']]=min(first[s['address']],s['from_pos'])
    last[s['address']]=max(last[s['address']], s['to_pos'])
uu={(s[1],dd[1],k) for s,dd,k in d['edges'] if s[0]=='u' and dd[0]=='u'}
for p in [0,100,200,300,400,500,600,700,max(t['positions'].values())]:
    ok=sum(1 for a,b,k in uu if first[a]<=p<last[a] and first[b]<=p<last[b])
    print(p, ok, len(uu)-ok)
# "near end in force, far end not", per kind, at p in (300,500)
```

---

## Summary table for the report writer

| claim | figure | denominator | source |
|---|---|---|---|
| edge kinds the parser emits | 8 (3 `href:*`, 5 `typed:*`) + supersession as 2 dicts | — | `corpus.py` |
| total edge rows / distinct triples | 12,283 / 11,258 | — | corpus JSON `edges` |
| unit→unit (anchored, usable) | **8,662 (70.5%)** | 12,283 rows | corpus JSON |
| has a page end (unusable anchored) | **3,621 (29.5%)**, of which u→p 1,218 | 12,283 rows | corpus JSON |
| the low-information kind | `href:same-spec`, **624/627 pages = 99.5%** page coverage | 627 pages | corpus JSON |
| unit keys with no edge at all | 4,372 (41.5%) | 10,540 unit keys | corpus JSON |
| unit keys with no unit→unit neighbour | 5,352 (50.8%) | 10,540 unit keys | corpus JSON |
| `typed_neighbours` non-empty | 1,571 (14.9%), mean 1.74, max 26 | 10,540 unit keys | corpus JSON + `pipeline.py` |
| `EXPAND_MAX = 5` actually binds | 84 of 1,571 (5.3%) | units with typed nbrs | corpus JSON |
| questions that can pin → can expand | **12 (11.4%) → 4 (3.8%)** | 105 questions | golden sets + `idlookup.py` |
| latent (a) same-id, same dir | 86 ID-shaped pairs, **0 declared, 0 correct** (+573 vacuous) | 659 pairs | corpus JSON |
| latent (b) cross-artifact prose mention | **4,363 of 6,031 already declared (72.3%)**; 1,668 latent | 6,031 pairs | corpus JSON |
| latent (c) sibling artifact pages | **592 of 776 already declared (76.3%)**; schema triple 100% | 776 pairs | corpus JSON |
| latent (d) cross-spec co-reference | 3,760 latent at ≥1 token, but 74.7% declared at ≥20 | 7,260 spec pairs | corpus JSON |
| latent (e) `#changelog` by date | **119 of 156 already declared (76.3%)**; 37 latent | 156 pairs | corpus JSON |
| **composite: relations already written down** | **5,074 of 6,963 = 72.9%** | (b)+(c)+(e) instances | corpus JSON |
| declared-but-unextracted (not latent — lost) | ~2,000 attribute values | — | `declared_meta.md` |
| bitemporal: edges with intervals | **none — the time corpus has no edges** | 3 keys | `timecorpus.py` + JSON |
| snapshot edges sound at position 300/721 | **27.6%** (2,194 of 7,956) | 7,956 u→u edges | both JSONs |
| unit-states vs unit keys | 15,616 vs 10,540 = **1.48x** (docstring says 1.3x — stale) | — | timecorpus JSON |
