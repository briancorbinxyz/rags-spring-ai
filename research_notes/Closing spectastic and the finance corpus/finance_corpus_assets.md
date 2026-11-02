# Finance-corpus assets: inventory and staleness

Scope: everything already built for a finance corpus in *RAGs to Riches with Spring AI*, with an honest
staleness read on each piece, for the decision whether to close spectastic at Episode 6 and move the next
two or three episodes onto finance.

Headline correction to the brief, up front, because it changes the size of the head start in both
directions:

1. **`docs/kb-schema/` is not on a side branch. It is already on `episode-5` and in the working tree.**
   The four schema commits (`71ee850`, `b32b6fb`, `4e51f8d`, `9c05b45`) sit on the `episode-3` line that
   `episode-5` descends from. `git diff --stat origin/finance-corpus..episode-5 -- docs/kb-schema` is
   empty. There is nothing to merge and nothing to port. The only thing unique to `origin/finance-corpus`
   is the fetcher: 12 files, 1,209 lines, all under `tools/corpus-fetch/`.
2. **A live fetch did happen and its output is retained — in a different repository.**
   `/Users/briancorbin/Code/rags/rags-spring-ai/rags-finance-corpus` is a separate git repo
   (`https://github.com/briancorbinxyz/rags-finance-corpus.git`, private) holding `manifest.jsonl` with
   **98 responses, all HTTP 200, 178,892,093 bytes (~170 MiB)**, fetched 2026-10-14 22:59:09Z–23:16:38Z.
   It is excluded from the main repo via `.git/info/exclude:7`, which is why it does not appear in
   `git status`, and it has **no working tree checked out** (only `.git`; `refs/heads/main` is unborn,
   only `refs/remotes/origin/main` = `7899ef1` exists).

---

## What exactly does the knowledge-base schema model? Tables, and link types split by derivation source

### Takeaway

The schema is a 40-table bitemporal SQLite design in three files (`schema.sql` 38,358 B core,
`extensions.sql` 50,614 B packs, `apply.sql` 6,341 B rule step) with 19 link types catalogued in
`link-types.yaml`. The decisive split for the finance decision: of the 19 types, **only 4 carry a status
effect that any profile derives from structured filing metadata**, 4 more come from authored
markup/citation, and exactly **1 (`resolves`) is inference over prose** — and that one is provisional by
construction. The dense authored-citation families (`cites`, `refers-to`, `quotes`, `replies-to`) all have
`effect: none`.

### Cited Findings

**Core tables (`schema.sql`, on `episode-5` and `origin/finance-corpus`, identical):**
`source`, `ingest_run`, `blob` (write-once, SHA-256 addressed, enforced by triggers `blob_write_once_u`/
`blob_write_once_d`), `document`, `unit`, `identifier`, `version`, `version_fts` (FTS5 virtual,
contentless, `bm25(version_fts, 5, 10, 1)`), `embedding`, `link`, `clock`, `authority_kind`,
`status_value`, `assessment`, `answer`, `answer_context`, `quotation`, `question`, `expected`, `run`,
`result`. Views: `assessment_live`, `status_now`, `document_trust_now`, `current_version`, `status_scope`,
`current_labeled`, `official_version`. — `git show origin/finance-corpus:docs/kb-schema/schema.sql`

**Extension tables (`extensions.sql`):** `access_grant`, `facet`, `derived`, `derived_input`,
`version_dup`, `entity`, `entity_identifier`, `alias`, `entity_link`, `mention`, `mention_candidate`,
`claim`, `evidence`, `feedback`, `key_scheme`, `link_type`, `rule_version`, `link_resolution`,
`justification`, `work_item`, `answer_recheck`, `subject`, `unit_subject`, `retention_class`, `hold`,
`document_hold`. Views include `derive_link_effect`, `derive_resolves`, `desired_assessment`,
`assessment_explain`, `link_pending`, `supersession_edge`, `supersession_head`, `resolve_key_candidate`,
`desired_link_resolution`, `link_binding`, `rule_window`, `source_rule_due`, `answer_exposure`,
`unit_time`, `document_time`, `mention_now`, `claim_now`, `consequence`.

**The 19 link types, split by how a profile actually derives them.** The `origin` enum on `link` is the
schema's own name for this split: `CHECK (origin IN ('markup','status+link','prose-pattern','model','human'))`
(`schema.sql`). `status+link` means structured filing metadata; `markup` means an authored, typed
hyperlink/citation relation; `prose-pattern` means inference over prose.

| Link type | `effect` / `effect_value` | Derived from | Evidence |
|---|---|---|---|
| `amends` | `label` / `amended` | **structured metadata.** `sec-edgar-10k.yaml` link `amendment`: `when: "fields.form.endsWith('/A')"`, `origin: status+link`, `confidence: 1.0`. Also authored markup via `eur-lex.yaml` `eli.amends` (`origin: markup`, `confidence: 1.0`) | profiles |
| `corrects` | `label` / `corrected` | **structured metadata.** `federal-register.yaml` `relations.correction_of`: `origin: status+link`, `confidence: 1.0`, keyed on `$.correction_of`. Also `eur-lex.yaml` `eli.corrects` (`markup`) | profiles |
| `supersedes` | `standing` / `superseded` | **authored by a human analyst.** Only occurrence is `federal-register.yaml` `manual_links`, `owner: analyst-1` | profiles |
| `superseded-by` | `standing` / `superseded` | v1 spectastic type. `note: "v1 type (spectastic). Stored on the older item"` | link-types.yaml |
| `do-not-rely` | `standing` / `do_not_rely` | **prose inference over structured trigger.** `sec-edgar-8k.yaml` `rules.item-4.02`: the item code `4.02` is structured, the *target* is not — `origin: prose-pattern`, `confidence: "pair.form != '' && pair.period != '' ? 0.9 : 0.6"` | profiles |
| `restates` | `standing` / `restated` | **prose inference.** `sec-edgar-10k.yaml` `restatement`: `origin: prose-pattern`, `confidence: 0.9`, gated on `fields.amendment_description.matches('(?i)restat')` | profiles |
| `repeals` | `standing` / `repealed` | **authored markup.** `eur-lex.yaml` `eli.repeals`: `origin: markup`, `confidence: 1.0` | profiles |
| `consolidates` | `none` | **authored markup.** `eur-lex.yaml` `eli.consolidates`, `origin: markup` | profiles |
| `refers-to` | `none` | **structured metadata.** `sec-edgar-10k.yaml` `late-notice-fulfilled` and `sec-edgar-8k.yaml` `rules.auditor-letter`, both `origin: status+link`, `confidence: 1.0` | profiles |
| `replies-to` | `none` | authored markup (email headers). `note: "In-Reply-To/References, Slack thread_ts != ts, Teams replyToId. Ordering only, never supersession."` | link-types.yaml |
| `resolves` | `label`, scope `claim` | **the only prose-inference type with an effect.** `note: "A later message resolves a hedged claim or query in the target... provisional until a human confirms."`, `exclusion_requires: [human]` | link-types.yaml |
| `same-thread-candidate` | `none` | prose/heuristic. `note: "Subject-only thread match... A suggestion for the inbox, nothing more."` | link-types.yaml |
| `quotes` | `none` | authored markup. `note: "Forwards and inline quotes. Never replies-to, never supersedes."` | link-types.yaml |
| `duplicates` | `none` | computed. `note: "Near-duplicate; clusters live in version_dup"` | link-types.yaml |
| `cites` | `none` | authored citation. `note: "v1 type."`, `interop: {dcterms: "dcterms:references", cito: "cito:cites"}` | link-types.yaml |
| `validates` | `none` | spectastic. `note: "v1 type (spectastic: a test or check validates a requirement)."` | link-types.yaml |
| `defers-to` | `none` | spectastic. `note: "v1 type (spectastic)."` | link-types.yaml |
| `parent` | `none` | structural. `note: "v1 structural type."` | link-types.yaml |
| `revises` | `none` | v1 compatibility. `note: "v1 type, kept with no status effect so v1 behaviour is unchanged. New adapters emit amends, corrects or restates."` | link-types.yaml |

- Nine of the 19 are inherited spectastic/v1 types carried forward unchanged, four of them explicitly
  labelled so: `superseded-by`, `refers-to`, `cites`, `validates`, `defers-to`, `parent`, `revises`.
  So **finance work added roughly 10 new types**, not 19. — `docs/kb-schema/link-types.yaml`
- The authored-citation families that make legislative corpora attractive carry **no status effect at
  all**: `cites`, `refers-to`, `quotes`, `replies-to`, `consolidates` all have `effect: none` and
  `exclusion_requires: []`. The README states their job: "`consolidates`, `refers-to` (v1),
  `replies-to`, `same-thread-candidate`, `quotes`, `duplicates`, `cites` (v1), `validates` (v1),
  `defers-to` (v1), `parent` (v1), `revises` (v1) | none: navigation and ranking only".
  — `docs/kb-schema/README.md`, "Link types" table
- The schema explicitly refuses transitive status: "`transitive` always false for status. Chains
  (A superseded by B superseded by C) are followed at query time with a cycle-safe recursive CTE
  (supersession_head)". — `link-types.yaml` header comment

### Inferences

- The split lands where the brief suspected it would, but the ratio is harsher than "filings are
  structured": **EDGAR gives the schema a structured *trigger* and a prose *target* for both of its
  high-value status relations.** `sec-edgar-8k.yaml` says it outright: "the item code '4.02' is
  structured... the AFFECTED FILING IS NOT", and `sec-edgar-10k.yaml`'s field-status header says "does
  NOT exist: any structured field saying which accession an /A amends". The headline finance
  relation (`do-not-rely`) is therefore a regex over Item 4.02 prose plus a composite-key resolver, not
  a metadata read.
- The genuinely clean, confidence-1.0, markup-derived status relations are **EU/ELI only**
  (`amends`, `corrects`, `repeals`, `consolidates`). If the next episodes want authored typed relations
  they can trust without extraction, EUR-Lex is the source that supplies them, not EDGAR.
- A new finance episode built on authored citation density would be building on types the schema
  currently gives `effect: none`. That is a design choice to revisit, not a gap to fill: the schema
  already decided citations rank, they do not exclude.

### Gaps

- No profile emits `cites` at all. The 19-type catalogue is seeded and frozen, but only 11 types are
  referenced by any of the seven profiles. Which types a finance episode would actually exercise is
  undetermined by the artefacts.

---

## `consequences.yaml`, the status ledger, and the v2 relationship rules

### Takeaway

`consequences.yaml` is a DMN-style decision table, FIRST hit policy, that decides per link type and per
emitter authority whether a derived status row **applies**, is a **provisional label**, or does
**nothing**. The "status ledger" is `assessment`: an append-only bitemporal table of every trust or
status judgment, with effective status computed per query by `status_now` and never stored. Both were
added whole in `9c05b45` (14 Oct 2026, v2) and both still execute.

### Cited Findings

- `consequences.yaml` (5,960 B) holds **8 rules** — one per status-bearing link type: `amends`,
  `corrects`, `supersedes`, `superseded-by`, `do-not-rely`, `restates`, `repeals`, `resolves`. Header:
  "Read it like a DMN decision table with hit policy FIRST: within one link type, rows are tried top to
  bottom and the first match wins. '*' matches anything."
- Authority vocabulary, computed from the data, not declared: `human`, `model`, `publisher` ("the linking
  document has the same publisher as the target (same CIK, same sender)"), `markup`, `heuristic`.
  Outcomes: `apply` / `label` / `none`.
- The 8-K Item 4.02 rule is the sharpest row, and it is deliberately asymmetric:
  `- {authority: publisher, origin: "*", min_confidence: 0.8, outcome: apply, dimension: standing, value: do_not_rely}`
  then `- {authority: "*", origin: "*", min_confidence: 0.0, outcome: label, ...}`, with the comment
  "Anyone else saying it is a label (an outsider-triggered exclusion would be an availability attack)."
- An unofficial correction is explicitly neutered: for `corrects`, the first row is
  `{authority: publisher, origin: prose-pattern, min_confidence: 0.0, outcome: none, ...}` — comment:
  "An unofficial correction (an email 'correction:' cue) acts on claims, not documents."
- `resolves` never auto-applies: `- {authority: human, ... outcome: apply}` then
  `{authority: "*", ..., min_confidence: 0.5, outcome: label}` — "Even the original sender's
  self-correction stays provisional: denial detection from text is unreliable, so a human confirms."
- **Status ledger.** `assessment` has six mutually-exclusive targets (`document_id`, `unit_id`,
  `version_id`, `link_id`, `claim_id`, `mention_id`, enforced by a `CHECK (...) = 1`), with
  `direction IN ('initial','promote','demote','lateral')`, `provisional`, `valid_from`/`valid_to`,
  `recorded_from`, `supersedes`, `evidence_version_id`, `evidence_link_id`,
  `method IN ('source','rule','human','model')`, `authority`, `rule_id`/`rule_sha256`, `derivation_key`,
  `proposed_by`/`rated_by`. Triggers `assessment_no_update` and `assessment_no_delete` make it
  append-only. — `schema.sql`
- The rules are in the DDL, not in application code. Four that matter:
  `CHECK (direction <> 'promote' OR (method = 'human' AND proposed_by IS NOT NULL AND proposed_by <> rated_by))`;
  `CHECK (direction <> 'demote' OR authority NOT IN ('model','third_party','heuristic') OR provisional = 1)`;
  trigger `assessment_exclusion_authority` raising `'exclusion needs publisher, markup or human
  authority; record it as provisional'`; trigger `assessment_publisher_is_publisher` raising
  `'authority=publisher but the evidence comes from another publisher'`. — `schema.sql`
- `authority_kind` ranks seven authorities with `may_exclude`: `publisher` (1,1), `markup` (2,1),
  `human` (3,1), `profile` (4,0), `third_party` (5,0), `heuristic` (6,0), `model` (7,0). `status_value`
  defines the vocabulary over seven dimensions (`standing`, `register`, `world`, `verification`,
  `resolution`, plus the mirrored trust facts) with `supports IN ('yes','labelled','no')` and
  `official IN (0,1)`. — `schema.sql`
- **Relationship rules in three layers** (README): layer 1 relation semantics in `link_type` seeded from
  `link-types.yaml`; layer 2 detection in `profiles/*.yaml`, versioned by `ingest_run.profile_sha256`;
  layer 2b resolution in `link_resolution` ("Exactly one candidate published before the linking document
  binds it; two candidates bind nothing and become an inbox item"); layer 3 consequence in
  `rule_version` bodies compiled from `consequences.yaml`.
- Two kinds of rule change, with different replay semantics: `change_kind: fix` ("our interpretation was
  wrong. Applies to the old version's whole valid range from its adoption time; earlier AS_KNOWN results
  are unchanged") vs `change_kind: new` ("the world's rule changed from effective_from. Derivations for
  earlier valid times are left exactly as they were"). Enforced by trigger
  `rule_version_fix_same_window`. — `consequences.yaml`, `extensions.sql`
- `apply.sql` is "the only writer for layers 2b and 3. It binds keys, appends the rows the derive views
  say are missing, records each row's premises in `justification`, and appends `void` rows for
  derivations whose premises stopped holding. Re-running adds nothing (`derivation_key`)." — README
- The `9c05b45` commit message claims "93 checks: ACME golden questions at three clocks, 10-K → 8-K 4.02
  → 10-K/A in all arrival orders, superseded T+1 playbook, negative tests, rule fix vs effective-dated
  version".

### Inferences

- The consequence table is the piece most directly reusable by a finance episode and the piece least
  dependent on anything else: it is 5,960 bytes of YAML compiled to JSON rule bodies, read by two SQL
  views. Nothing in it references spectastic.
- The design makes a specific, measurable promise a finance episode could test as its headline: a
  publisher-authored exclusion changes retrieval membership *before* top-k, while anyone else's
  claim becomes a label. That is testable with the three filings already fetched (`ipoe-402-2021` plus
  its target) without any model.

### Gaps

- `rule_version` bodies are "compiled from `consequences.yaml`" by `tests/gen_seed.py`, which needs
  PyYAML. PyYAML is not installed in this environment, so the compile step was not exercised here; the
  seeded bodies in `extensions.sql` were checked against the YAML only in the author's own run.

---

## What the source profiles cover, field by field

### Takeaway

Seven profiles, 32,893 bytes total. The four finance/regulatory ones are detailed to field level with
JSONPath/XPath/CEL, each carries `owner: brian` and `review_by: 2027-04-14`, and each honestly marks its
unverified field names. None has a single test fixture, and every Java processor they name is
unimplemented.

### Cited Findings

**`profiles/sec-edgar-10k.yaml` (5,844 B).** `source_id: sec-edgar-10k`, `kind: filing`,
`adapter: edgar-ixbrl`, matching `10-K`, `10-K/A`, `10-KT`, `10-KT/A`.

- Fields extracted: `cik` (`$.cik`), `form`, `accession`, `report_date`
  (`$.filings.recent.reportDate[i]` — marked `[unverified field name]`), `accepted`
  (`acceptanceDateTime[i]`, `[unverified]`), `period_end`
  (`//ix:nonNumeric[@name='dei:DocumentPeriodEndDate']`), `amendment_flag` (`dei:AmendmentFlag`),
  `amendment_description` (`dei:AmendmentDescription`, "may be absent").
- Identifiers declared: `edgar:accession`, `edgar:cik+form+period`,
  `edgar:cik+form+period+item` (per unit), `ticker` (optional).
- **Relations it declares it can derive: three.** `amends` (`origin: status+link`, conf 1.0),
  `restates` (`origin: prose-pattern`, conf 0.9, from `Item 8` units only),
  `refers-to` (NT 10-K anticipation, `status+link`, conf 1.0, `optional_target: true`).
- Dating: `valid_from: "period()"` with the explicit rule "period() = fields.report_date if present,
  else fields.period_end. Never the filing date."; `published_at: "fields.accepted"`;
  "recorded_from is NOT mapped".
- Honest field-status header: "verified on SEC pages: dei:DocumentType, dei:AmendmentFlag,
  dei:DocumentPeriodEndDate, dei:EntityCentralIndexKey, dei:EntityFileNumber... does NOT exist: any
  structured field saying which accession an /A amends."
- Partition and gates: `processor: java:EdgarItemSplitter@1.2`,
  `quality_gates: ["parse.items_found >= 4", "parse.text_chars > 20000"]`.

**`profiles/sec-edgar-8k.yaml` (4,775 B).** Matching `8-K`, `8-K/A`.

- Fields: `cik`, `form`, `accession`, `report_date`, `accepted`, and
  `items: "$.filings.recent.items[i].split(',')"` ("e.g. ['4.02', '9.01'] [unverified field name]").
- Prose extraction over the Item 4.02 unit only, five named patterns: `period_ended`
  (`'(?i)(?:fiscal )?(?:year|quarter|quarterly period|six months|nine months)s? ended (\w+ \d{1,2}, \d{4})'`),
  `as_of_date`, `form_name`
  (`'(?i)(Annual Report on Form 10-K|Quarterly Reports? on Form 10-Q|Form S-[14](?:/A)?|Form 10-K/A)'`),
  `originally_filed`, `conclusion_date`
  (`'(?i)on (\w+ \d{1,2}, \d{4}),? the (?:Audit Committee|Board)'`).
- **Relations: two.** `do-not-rely` (`origin: prose-pattern`, `confidence: "pair.form != '' && pair.period != '' ? 0.9 : 0.6"`,
  with the inline note `# below 0.8 -> label only`) and `refers-to` for the Item 4.02(c) auditor letter
  arriving by 8-K/A (`status+link`, conf 1.0).
- Review queues with a named `expedite` class: `non-reliance-target-not-found`, `ambiguous-target`,
  `non-reliance-target-not-ingested` (`pending_days > 7`).
- Only one identifier (`edgar:accession`) — no composite key emitted *on* the 8-K itself.

**`profiles/eur-lex.yaml` (3,908 B).** `kind: regulation`, `adapter: eurlex-html`.

- Fields read from RDFa with a SPARQL fallback: `celex`
  (`rdfa('eli:id_local') ?? sparql('cdm:resource_legal_id_celex')`), `eli`, `date_document`,
  `in_force` (`eli:first_date_entry_in_force`), `applies` (`eli:date_applicability`), `no_longer`,
  plus `rdfa_all()` over `eli:amends`, `eli:corrects`, `eli:repeals`, `eli:consolidates`.
- **Relations: four, all `origin: markup`, all `confidence: 1.0`** — the densest authored-relation set in
  the whole schema. Header: "ELI publishes typed, directional relations an ingester can read directly:
  amends/amended_by, corrects/corrected_by, repeals/repealed_by, consolidates/consolidated_by".
- Three unit granularities: `article`, `article/paragraph` (both `statement: normative`), `recital`
  (`statement: record`).
- A documented date trap: "for Reg. (EU) 2025/2075 the EUR-Lex metadata panel appeared to show
  2027-10-11 as 'first date of entry into force', while the act enters into force 20 days after
  publication and APPLIES from 11 Oct 2027. Never map a panel label to valid_from."
  Resolved by `valid_from: "has(fields.applies) ? fields.applies : fields.in_force"`.
- Bot-page defence: `reject_if: "response.bytes < 20000 || response.text.contains('cookie') && !response.text.contains('Article 1')"`.
- Flagged unverified: "whether EUR-Lex exposes these as RDFa on each page or only via CELLAR SPARQL
  (both readers are configured; the first that answers wins)."

**`profiles/federal-register.yaml` (4,725 B).** `adapter: fedreg-api`.

- Fields (confirmed against the R client's manual, per its header): `document_number`, `citation`
  ("'88 FR 13872'"), `type`, `action`, `publication_date`, `effective_on`, `dates_text`, `rins`,
  `cfr_references`, `correction_of`, plus a derived `compliance_date:
  "regex(fields.dates_text, '(?i)Compliance [Dd]ate[s]?:\\s*(.+?)\\.')"`.
- **Relations: two derived, one hand-authored.** `corrects` (`origin: status+link`, conf 1.0, handling
  `correction_of` as either a document number or a URL); `amends` over CFR sections with a conditional
  origin — `origin: "has(doc.full_text_xml) ? 'markup' : 'prose-pattern'"`,
  `confidence: "has(doc.full_text_xml) ? 1.0 : 0.85"`, driven by
  `for_each: "regex_all(unit.text, 'Amend § ?(\\d+\\.[\\d-]+)')"`.
- A `manual_links:` block — the only authored `supersedes` in the repository:
  `from: {scheme: sec:release, value: "34-96930", unit: compliance-date}`, `type: supersedes`,
  `to: {document: "sifma-ici-dtcc-t1-playbook-2022-08-01", unit: transition-date}`,
  `valid_from: 2023-02-15`, `owner: analyst-1`, `review_by: 2027-10-01`,
  `evidence: "https://www.sec.gov/rule-release/34-96930"`. Comment: "Relations no publisher encodes."
- The dating rule that carries the episode's point: "the valid time of rule TEXT is its compliance or
  application date (US T+1: 28 May 2024, not 5 May 2023). The release itself is a record whose valid
  time is its publication." Implemented as a per-statement `valid_from` map:
  `record: "fields.publication_date"`, `normative: "has(fields.compliance_date) ? fields.compliance_date : fields.effective_on"`.
- Identifiers: `fr:doc`, `fr:cite`, `sec:release` (regex `'Release No\\. (\\d{2}-\\d{5})'`, optional),
  `cfr` per normative unit.
- Flagged unverified: "the sub-keys of cfr_references and whether correction_of holds a document number
  or a URL (handled both ways below)."

**The other three profiles** cover the private/synthetic layer: `email.yaml` (6,000 B — the largest),
`chat-slack.yaml` (3,556 B), `research-note.yaml` (3,685 B).

**Absences, confirmed:**

- Every profile ends with a `fixtures:` line (`fixtures: fixtures/sec-edgar-10k/{case}/{input.bin, expected.jsonl}`,
  and so on, including the instruction `# include IPOE's 2021 4.02 (form + period, no accession)` and
  `# include 32025R2075`). **No `fixtures/` directory exists on either branch.**
  `git ls-tree -r --name-only origin/finance-corpus | grep -i fixture` returns only
  `src/test/resources/fixtures/specs/001-demo/spec.html`, which is spectastic's.
- The three Java processors the profiles name — `java:EdgarItemSplitter@1.2`,
  `java:FormexArticleSplitter@0.2`, `java:FederalRegisterXmlSplitter@0.3` — **do not exist**.
  `git grep -l` for those names across `origin/finance-corpus` and `episode-5` matches only the four
  YAML files that name them.
- `sec-edgar-10k.yaml` and `sec-edgar-8k.yaml` both cite
  `research_notes/.../official_relationships.md` ("see research_notes/.../official_relationships.md",
  "official_relationships.md, section 2"). **That file does not exist** anywhere in the repo or in the
  vault at `/Users/briancorbin/Documents/Brain/` (`find` returns nothing for `official_relationships*`).
  The research it refers to appears to have been folded into the vault note
  `RAGs to Riches, Corpus Ingester.md`, whose section "Official publishers encode relations; unofficial
  ones only hint at them" (line 492) covers the same ground.

### Inferences

- The four regulatory profiles are **specifications, not running code**. Every one is a complete,
  reviewed design with no implementation, no fixture and no parser behind it. Their value is the design
  decisions already made and justified (which field, which date, which confidence, what to do when the
  key is ambiguous) — that is real and expensive work, but it is zero executable lines.
- EUR-Lex and the Federal Register are the two profiles where a finance episode would get the most for
  the least: four conf-1.0 markup relations and a structured `correction_of` respectively, versus
  EDGAR's prose-pattern targets. The brief's instinct about legislative citation networks is right about
  *typed* relations, though note again that the dense *citation* families carry `effect: none`.
- `review_by: 2027-04-14` on all four profiles means nothing has expired. The design is about 12 months
  from its own review date.

### Gaps

- No measurement exists of whether any of these extraction patterns work. The 8-K Item 4.02 regexes in
  particular were written without a single real 4.02 in hand on that branch.

---

## How far did the fetcher get? Was there a live run?

### Takeaway

**Yes — a complete, clean live run, and its output is retained.** 98 requests, all HTTP 200,
~170 MiB, on 2026-10-14 between 22:59:09Z and 23:16:38Z, covering 24 filings across 15 companies plus
22 regulatory documents. One item failed. But the outputs live in the separate private repo
`rags-finance-corpus`, not on `origin/finance-corpus`, and the **document bytes themselves are not
retained anywhere locally** — `raw/` is git-ignored by design, and that repo has no working tree checked
out. What exists is a reproducible manifest with per-response SHA-256s, not a corpus on disk.

### Cited Findings

**Nothing was committed to `origin/finance-corpus`.** `git ls-tree -r --name-only origin/finance-corpus -- data`
returns empty. `git log --all --diff-filter=A --name-only -- 'data/*' '*manifest.jsonl*' '*finance-corpus*'`
returns nothing: no manifest has ever existed in the main repo's history on any branch.
`tools/corpus-fetch/.gitignore` is `data/finance-corpus/raw/`, `__pycache__/`, `*.pyc`.

**The live run is evidenced three ways.**

1. Commit `c2d7965` (2026-10-14 19:03:48 -0400), "corpus-fetch: fix first live run's three misses;
   relative manifest paths", body: "BK is now BNY in company_tickers.json: pin BNY Mellon by CIK...
   STT: a busy filer's recent submissions page ends before 2024, so read only the older pages that
   overlap the window... DTCC moved the 2022 T+1 playbook: Internet Archive fallback, plus SIFMA's 2024
   revision as a same-publisher supersession pair".
2. Commit `ece42b0` (19:23:57 -0400): "A 429 from the Internet Archive paused every non-SEC request for
   11 minutes and used up the run's one SEC throttle strike" — a defect only a live run surfaces.
3. The retained manifest itself, in `rags-finance-corpus` at `7899ef1` ("Fetch public layer",
   2026-10-14 19:25:12 -0400 = 23:25:12Z, two minutes after the last fetch).

**The run, quantified** (`data/finance-corpus/manifest.jsonl`, 98 lines, keys `url`, `file`, `sha256`,
`bytes`, `status`, `content_type`, `fetched_at`):

- **98 responses, every one status 200.** Zero non-200 rows.
- **178,892,093 bytes** (~170 MiB) fetched.
- Window: `2026-10-14T22:59:09Z` to `2026-10-14T23:16:38Z` — about 17.5 minutes.
- Hosts: `www.sec.gov` 51, `data.sec.gov` 16, `efts.sec.gov` 11, `www.federalregister.gov` 6,
  `www.ecfr.gov` 5, `assets.publishing.service.gov.uk` 3, and one each from `eur-lex.europa.eu`,
  `www.esma.europa.eu`, `www.gov.uk`, `www.osc.ca`, `www.sebi.gov.in`, `www.sifma.org`.
- Every row is content-addressed, e.g. `{"url": "https://efts.sec.gov/LATEST/search-index?q=%22T%2B1%22+%22settlement+cycle%22&forms=10-K&dateRange=custom&startdt=2023-01-01&enddt=2025-12-31", "file": "raw/e62aebcb4b7854f6_search-index", "sha256": "d53f2037cb4045478cd610337c077ea8ba865fe8536b0b23fbcf6a2407bfc220", "bytes": 14337, "status": 200, "content_type": "application/json", "fetched_at": "2026-10-14T22:59:09Z"}`.

**The 24 filings** (`filings.jsonl`, 24 lines, keys include `cik`, `company`, `ticker`, `form`,
`filing_date`, `report_date`, `accepted`, `adsh`, `document`, `sha256`, `bytes`, `why`) — all fetched, no
`error` field on any row:

| Ticker/id | Form | Filed | Period | Accession |
|---|---|---|---|---|
| ACU | 10-K | 2026-03-11 | 2025-12-31 | 0001193125-26-102079 |
| ACU | 10-K | 2025-03-06 | 2024-12-31 | 0000950170-25-034843 |
| ACI | 10-K | 2026-04-27 | 2026-02-28 | 0001646972-26-000032 |
| WBD | 10-K | 2024-02-23 | 2023-12-31 | 0001437107-24-000017 |
| HOOD | 10-K | 2025-02-18 / 2024-02-27 | 2024-12-31 / 2023-12-31 | 0001783879-25-000049 / -24-000054 |
| SCHW | 10-K | 2025-02-26 / 2024-02-23 | 2024-12-31 / 2023-12-31 | 0000316709-25-000010 / -24-000018 |
| IBKR | 10-K | 2025-02-27 / 2024-02-27 | 2024-12-31 / 2023-12-31 | 0001381197-25-000036 / -24-000083 |
| VIRT | 10-K | 2025-02-21 / 2024-02-16 | 2024-12-31 / 2023-12-31 | 0001592386-25-000055 / -24-000026 |
| BR | 10-K | 2025-08-05 / 2024-08-06 | 2025-06-30 / 2024-06-30 | 0001628280-25-037656 / 0001383312-24-000039 |
| BNY | 10-K | 2024-02-28 | 2023-12-31 | 0001390777-24-000051 |
| STT | 10-K | 2024-02-15 | 2023-12-31 | 0000093751-24-000498 |
| PLTR | 10-K | 2026-02-17 | 2025-12-31 | 0001321655-26-000011 |
| INFY | 20-F | 2026-06-15 | 2026-03-31 | 0001193125-26-270520 |
| HDB | 20-F | 2026-07-29 | 2026-03-31 | 0001193125-26-322004 |
| SHOP | 10-K | 2026-02-11 | 2025-12-31 | 0001594805-26-000007 |
| BCS | 20-F | 2026-02-10 | 2025-12-31 | 0000312069-26-000004 |
| pltr-s1-2020 | S-1 | — | — | 0001193125-20-230013 |
| pltr-classf-8k-2022 | 8-K | — | — | 0001193125-22-313008 |
| ipoe-402-2021 | 8-K | — | — | 0001104659-21-053883 |

That is **15 distinct companies, 21 periodic filings in matched before/after T+1 pairs, plus the three
explicitly targeted filings** — including `ipoe-402-2021`, the Item 4.02 non-reliance notice the whole
`sec-edgar-8k.yaml` profile was written around (`"why": "Item 4.02: audited statements 'should no longer
be relied upon'"`).

**The 22 regulatory documents** (`regs.jsonl`, 23 lines, keys `id`, `url`, `file`, `sha256`, `bytes`,
`role`, `redistribute`): SEC releases `34-80295` (T+2) and `34-96930` (T+1); Federal Register
`2017-06037` and `2023-03566`, each in JSON, text and HTML (6 rows); **five eCFR point-in-time snapshots
of 17 CFR 240.15c6-1** at `2017-03-01`, `2017-09-06`, `2024-05-29` and `2026-10-01` plus the versions
index — i.e. the same rule at four points in valid time; EUR-Lex `CELEX:32025R2075`; the ESMA 2024
report; four UK items (GOV.UK collection API, the 2024 taskforce report, the 2025 technical group
report, the 2025 draft SI); CSA 24-101 and the SEBI 2021 circular (both `redistribute: false`); and the
SIFMA 2024 playbook (`redistribute: false`).

**The one failure**, recorded exactly as the README promises:
`dtcc-t1-playbook-2022` — `ERR dtcc-t1-playbook-2022: no URL worked (Throttled again (HTTP 429) on
https://web.archive.org/web/2022id_/https://www.dtcc.com/-/media/Files/PDFs/T2/T1-Industry-Implementation-Playbook.pdf.
Stopping. ...)`. This is the 2022 playbook that `federal-register.yaml`'s `manual_links` block names as
the `supersedes` target (`sifma-ici-dtcc-t1-playbook-2022-08-01`). Its SIFMA 2024 replacement
(`sifma-t1-playbook-2024`) *was* fetched.

**Eight EDGAR full-text searches** (`search/*.jsonl`, 674 hit lines total): `rule-15c6-1.jsonl` 200,
`t1-foreign.jsonl` 154, `item-402-2021.jsonl` 100, `rule-412.jsonl` 100, `acme-markets.jsonl` 55,
`acme-united.jsonl` 26, `t1-us-10k.jsonl` 22, `t2-2017.jsonl` 16, `coyote-acme.jsonl` 1.

**What is *not* retained.** `rags-finance-corpus/.gitignore` line 1 is `data/finance-corpus/raw/`, and
its `data/finance-corpus/README.md` says "`raw/` is git-ignored and can be rebuilt from the manifest."
`git ls-tree -r origin/main` on that repo lists 23 blobs, none under `raw/`. The local clone has **no
checked-out working tree at all** — `ls -la rags-finance-corpus` shows only `.git`; `git show-ref` lists
only `refs/remotes/origin/HEAD` and `refs/remotes/origin/main`; `.git/HEAD` points at the unborn
`refs/heads/main`. `du -sh` is 312K.

**The fetcher still runs.** Extracted to a scratchpad and run offline:
`python3 -I -m unittest discover -s tests` → **"Ran 24 tests in 2.214s / OK"** on Python 3.14.7, against
the branch's own local stand-in server. Matches the count claimed in `ece42b0` ("24 tests"). The tool is
stdlib-only (`tools/corpus-fetch/README.md`: "It uses only the Python standard library (3.9 or later,
which macOS already ships), so there is nothing to install").

### Inferences

- The head start on acquisition is **larger than the brief assumed and smaller than the manifest looks**.
  The selection work is done and justified per-row (every `companies.json` entry carries a `"why"`:
  "T+1 before and after", "the year Coyote vs. Acme was shelved", "UK bank: UK T+1 not until 11 Oct
  2027"). The rate-limit engineering is done and proven against the real SEC. But re-materialising the
  170 MiB means **re-running against EDGAR**, because the cache was never retained. Budget one clean
  ~18-minute run plus the DTCC retry, not zero.
- The run's shape is already built for the time episodes rather than for retrieval breadth: matched
  before/after pairs for six brokers and custodians, and **five point-in-time eCFR snapshots of a single
  rule**. That is a valid-time test set, deliberately assembled.
- The `rags-finance-corpus` repo's "Order of work" is explicit about where the work stopped: "1. Fetch
  the public layer... 2. Fill the ledger... 3. Write and freeze the finance questions... 4. Only then
  write the synthetic documents." Step 1 is complete, step 2 is 6 rows, steps 3 and 4 are untouched.

### Gaps

- Whether `raw/` still exists on the author's machine outside git cannot be determined: the nested repo
  has no working tree, and the brief forbids fetching. If the author has a `raw/` directory elsewhere,
  the corpus is on disk; nothing in either repository records that.
- No `fetch.log` is committed, so the full `!!` failure list from the live run is unavailable beyond the
  one error captured in `regs.jsonl`.

---

## Is there a golden set or any questions for the finance corpus?

### Takeaway

**No finance retrieval questions exist.** `b010193`'s "13 held-out candidates" is spectastic's
(H-001..H-013, all about spec requirements). The only finance-shaped questions anywhere are **six
synthetic ACME questions seeded inside the schema test**, all `status = 'candidate'`, which exercise
ambiguity and AS_KNOWN over fabricated rows rather than over any fetched filing. The private repo's
`golden/finance/` is marked "(to come)" and does not exist.

### Cited Findings

- `b010193` is spectastic's. `src/main/resources/golden/questions-heldout.jsonl` has **13 lines**,
  H-001..H-013, e.g. H-001: `"What does success criterion SC-002 of spec 113, guardrail retrieval,
  check, and which requirements does it prove?"`, `"expected": ["specs/113-guardrail-retrieval/spec.html#FR-003", ...]`.
  `questions.jsonl` has **50 lines** (G-001..G-050).
- Zero finance content in either: `grep -ciE "edgar|10-K|SEC |CIK|filing"` returns **0** for both files.
- `src/main/resources/golden/README.md` confirms the subject: "Fifty questions with known answers and
  known sources... | `commit` | The spectastic commit it was checked against |", with strata
  `single-fact` 10, `paraphrase` 10, `identifier` 8, `relation` 8, `stale` 8, `unanswerable` 6.
- The same 13 held-out questions are the *only* golden file in the private corpus repo:
  `golden/spectastic/questions-heldout.jsonl` (18,258 B). Its `golden/README.md` says:
  "`spectastic/questions-heldout.jsonl`: the Episode 3 held-out set (H-001..H-013). Note: a copy was
  already pushed to the public rags-spring-ai repo on the `episode-3` branch, so treat it as seen...
  **`finance/` (to come): finance questions, each tagged with the retrieval mode it tests (CURRENT,
  HISTORY, AS_OF, CHANGE, AS_KNOWN) and the ledger rows that answer it.**"
  `git ls-tree -r origin/main` on that repo confirms no `golden/finance/` path exists.
- **The six ACME questions** are in `docs/kb-schema/tests/test_schema.py` function `acme_questions()`
  (line 358), inserted with `status = 'candidate'`:
  - `ACME-01` "Is ACME being sued?", stratum `ambiguity-fictional`, mode `CURRENT`
  - `ACME-02` "Is ACME being sued?", `ambiguity-as-known`, `AS_KNOWN` at 09:00 — "Enumerates the
    candidates or asks which ACME; no resolution yet."
  - `ACME-02b` same question, `AS_KNOWN` at 10:00 (marked `[drafted]`)
  - `ACME-03` "Which ACMEs file with the SEC, and are any in litigation?", `ambiguity-enumerate`, `CURRENT`
  - `ACME-04` "What is Acme Corporation's LEI?", `unanswerable-fictional`, `CURRENT`
  - `ACME-05` "Is Acme United being sued?", `conflation-trap`, `CURRENT` (marked `(drafted)`)
  with `expected` rows carrying roles `answer`, `conflict`, `decoy`.
- They run over **synthetic seed data**, not fetched filings. The test's own comment at line 212:
  "ACU FY2025 10-K. Accession and the Item 3 sentence are from the ACME report; **times illustrative**",
  and line 248: "08:12 -0400 rumour email (**fictional**), as in the ACME report."
- The README describes them as five plus one: "The five golden questions used are in the trace
  (`ACME-01` to `ACME-05`, plus a drafted `ACME-02b`)."
- **The ledger that finance questions were to be generated from exists but is nearly empty.**
  `rags-finance-corpus/ledger/facts.csv` has **6 rows**, F-001..F-006, all `check=yes`, all `kind=real`
  with a source URL: US T+3 (`valid_from 1995-06-07`, `valid_to 2017-09-05`), US T+2
  (`2017-09-05`/`2024-05-28`, `known_from 2017-03-22`), US T+1 (`2024-05-28`/—, `known_from 2023-02-15`),
  Canada T+1 (`2024-05-27`), EU T+1 (`2027-10-11`), UK T+1 (`2027-10-11`, draft SI).
  `ledger/events.csv` is **a header line and nothing else**:
  `event_id,at,author,channel,says,true_per_ledger,notes`.
- `synthetic/` contains only `README.md` (714 B). No synthetic document has been written. Its rules are
  set, though: every file must start `SYNTHETIC — fictional document for the RAGs to Riches series`, and
  "'ACME' appears only as an ambiguous mention (the rumour email), never as an invented company with its
  own filings".
- `ledger/README.md` names the mechanism the questions were to come from: "`valid_from` / `valid_to`:
  when the fact is true in the world... `known_from`: when it became knowable... **The gap between the
  two is where 'as known on' questions come from.**"

### Inferences

- The evaluation head start is **effectively nil**, and this is the single largest gap. The series'
  method is a frozen golden set scored by a ratchet; on finance there are no scoreable questions, no
  qrels, no runs, no baseline. Compare Episode 6, which has 24 time questions, saved runs across four
  model configurations, and `qrels.txt` adjudication.
- The six ACME questions are worth something precise but narrow: they are a **worked specification of
  what an AS_KNOWN finance question looks like**, with expected roles including `decoy` and `conflict`,
  and they already pass against the schema. They are a template for writing the real set, not a set.
- The 6-row ledger plus the five eCFR snapshots of 15c6-1 is enough raw material to write a
  genuine valid-time/knowledge-time question set about settlement cycles with no synthetic layer at all
  — the T+2 row alone has a 2017-03-22 `known_from` against a 2017-09-05 `valid_from`, a 167-day gap.
  That is the cheapest path to a first finance question set, and it does not depend on step 4.

### Gaps

- No count exists anywhere of how many finance questions were planned. `golden/README.md` names the five
  retrieval modes they should tag (CURRENT, HISTORY, AS_OF, CHANGE, AS_KNOWN) but no target n.

---

## How stale is the code, and is the kb-schema work portable without a merge?

### Takeaway

The flat-versus-packaged Java divergence is **a non-issue, because the kb-schema work is already on
`episode-5` and contains no Java at all.** Reconciling the two Java trees is not a cost anyone has to
pay: `origin/finance-corpus`'s flat tree is simply the old `episode-3` tree, and the only thing on that
branch that is not already on `episode-5` is the Python fetcher, which has zero Java coupling. The SQL
and YAML are not merely portable — they are already in the working tree, and they still execute
unchanged two years past their SQLite target.

### Cited Findings

**The branch topology (`git log --graph origin/finance-corpus`):**

```
*   4f89d0e Merge branch 'episode-3' into finance-corpus
|\
| * f2aaf03 README: the five pre-flight checks before quoting Episode 3 numbers
* | 2a17ad9 Merge branch 'episode-3' into finance-corpus
|\|
| * 9c05b45 Knowledge base schema v2: status ledger, relationship rules, ACME end-to-end test
* | ece42b0 corpus-fetch: other sites' 429s no longer trigger the SEC's 11-minute pause
* | c2d7965 corpus-fetch: fix first live run's three misses; relative manifest paths
* | 2f6b107 corpus-fetch: polite EDGAR and regulatory fetcher for the finance corpus
|/
* 4e51f8d Knowledge base schema v1.1: frozen clock, both valid-time bounds in CURRENT
* b32b6fb Knowledge base schema v1: trust facts, lineage, authorship, extensions
* 71ee850 Knowledge base schema v0 (living)
```

Ancestry of each commit in `episode-5`, by `git merge-base --is-ancestor`:

| Commit | In `episode-5`? |
|---|---|
| `71ee850` schema v0 | **YES** |
| `b32b6fb` schema v1 | **YES** |
| `4e51f8d` schema v1.1 | **YES** |
| `9c05b45` schema v2 | **YES** |
| `f2aaf03` README pre-flight | **YES** |
| `2f6b107` corpus-fetch | NO |
| `c2d7965` corpus-fetch fixes | NO |
| `ece42b0` corpus-fetch 429 fix | NO |
| `2a17ad9`, `4f89d0e` merges | NO |

So the schema commits are on the `episode-3` line, and only the three fetcher commits are exclusive to
`origin/finance-corpus`.

- `git ls-tree -r --name-only episode-5 -- docs/kb-schema` lists all **16** kb-schema files.
- The files are physically present and untouched since the branch work:
  `ls -la docs/kb-schema` shows `schema.sql` 38 KB, `extensions.sql` 49 KB, `apply.sql` 6.2 KB,
  `link-types.yaml` 8.3 KB, `consequences.yaml` 5.8 KB, `README.md` 30 KB, all mtime
  **Fri Oct 16 10:06:58 2026**.
- `git diff --stat origin/finance-corpus..episode-5 -- docs/kb-schema tools/corpus-fetch` reports
  **"12 files changed, 1209 deletions(-)"**, and every named file is under `tools/corpus-fetch/`
  (`polite.py` 290, `edgar.py` 178, `regs.py` 68, `tests/test_corpusfetch.py` 343, …).
  **`docs/kb-schema` contributes nothing to that diff: the two branches hold it byte-identically.**
- `git log --diff-filter=D episode-5 -- tools/corpus-fetch` returns nothing — the fetcher was never
  deleted from `episode-5`; it was never merged into that line.

**The Java divergence, for the record** (it bills to nobody, but here is its size):
`origin/finance-corpus` has 17 flat classes in `src/main/java/xyz/briancorbin/rags/`. `episode-5` has 52
classes across `answer/`, `eval/`, `search/`, `spectastic/`, `units/` plus 4 at the root, and 21 test
classes including `ArchitectureTest.java`. The full divergence between the merge base `f2aaf03` and
`episode-5` is **63 commits, 186 files changed, 25,621 insertions, 1,796 deletions** — but none of it
touches `docs/kb-schema`, and the fetcher is Python.

**Both executable artefacts still run, tested here.**

- The schema test, extracted to a scratchpad and run with `python3 -I`:
  **"84 checks passed, 0 failed"**, against the **93** claimed in `9c05b45`. The 9-check difference is
  fully explained and benign: the trace records `(PyYAML not installed: YAML cross-checks skipped)`, and
  `diff` against the committed `acme_walkthrough_output.txt` shows the only other difference is the
  header line — `SQLite 3.45.1` (committed) versus `SQLite 3.53.4` (here). The nine skipped checks are
  named in the committed trace: seven `profiles/*.yaml parses, has source_id and match`, plus
  `link-types.yaml matches the seeded link_type table (19 types)` and
  `consequences.yaml compiles to exactly the seeded rule bodies (8 rules)`.
  **The trace is otherwise byte-identical across SQLite 3.45.1 → 3.53.4 and Python 3.14.7.**
- The fetcher's suite: **"Ran 24 tests in 2.214s / OK"** on Python 3.14.7.

**Portability of the schema work, from the README's own account.** "The SQL is in `schema.sql` (core),
`extensions.sql` (optional packs) and `apply.sql` (the rule step)... They run as written on SQLite 3.45.
`tests/test_schema.py` (standard library only) loads them into memory". The only Java referenced
anywhere is the three unimplemented processor beans named in the profiles.

### Inferences

- **The correct framing of the head start is not "22 commits on a stale branch".** It is: the schema,
  profiles, consequence table and end-to-end test are *already in the current working tree and
  currently green*; one Python tool worth 1,209 lines needs a cherry-pick of three commits that touch no
  Java and no shared file; and the acquisition output lives in a sibling private repo that needs
  checking out.
- Staleness in the ordinary sense is close to zero for the SQL and YAML. An artefact whose trace
  reproduces byte-for-byte across two SQLite minor-version jumps has no bit rot. What it has instead is
  **unexercised-ness**: nothing has ever run it against a real document.
- The three Java beans are the real implementation boundary, and they are where the packaged
  `episode-5` tree becomes an advantage rather than a cost: `EdgarItemSplitter`,
  `FormexArticleSplitter` and `FederalRegisterXmlSplitter` are exactly the shape of the existing
  `spectastic/SpectasticReader.java` + `units/ElementUnits.java` pair — a source-specific reader
  producing units — and the packaging (`spectastic/` alongside a hypothetical `edgar/`) is what makes a
  second corpus a sibling package rather than a fork.

### Gaps

- Whether `episode-5`'s `Standings`/`Standing` interfaces can carry the schema's richer status model
  (seven dimensions, provisional rows, authority ranking) is untested; the Java interface exposes only
  `label()`, `outOfDate()` and `precedence()`.

---

## Is the schema bitemporal, and does it match what Episode 6 built for spectastic?

### Takeaway

The finance schema is **fully bitemporal on three axes**, with named columns on `version`, `link`,
`assessment` and `rule_version`. Episode 6's spectastic implementation is **not the same design and not
the same shape**: it has no SQL store, no validity intervals and no transaction-time columns. It keys
the corpus on **git first-parent position** and carries a single `lifecycle` label per unit. The two
meet only conceptually. The finance schema is strictly the richer of the two — which means the finance
corpus would exercise machinery Episode 6 never needed, and nothing Episode 6 built transfers as a
schema.

### Cited Findings

**The finance schema's bitemporal columns, named exactly.**

- `version`: `valid_from`, `valid_to` ("NULL = still in effect"), `valid_grade`
  (`'element','change','page','none'`: "how the date was derived"), `recorded_from` ("when this KB first
  recorded this text"), `recorded_to` ("NULL = still recorded this way"), and `published_at` ("when the
  source published / sent / accepted it"). Header comment: "A version is one text of one unit over two
  time axes... A change opens a new valid period; a correction keeps valid_from and closes only the
  recorded period."
- `link`: `valid_from` ("when the relation holds in the world (e.g. adoption date)"), `recorded_from`,
  `recorded_to` ("set once when the link is withdrawn; never deleted"), with
  `CHECK (recorded_to IS NULL OR recorded_to > recorded_from)` and the `link_append_only` trigger.
- `assessment`: `valid_from`, `valid_to` ("when the judgment holds in the world; NULL = always"),
  `recorded_from` ("when this KB recorded it; rows are never updated"), plus `supersedes`.
- `clock`: `now_valid` and `now_recorded`, id-pinned to 1. "Freeze it so answers don't flip when a
  future-effective rule comes into force mid-run. AS_KNOWN(t) is the same views with now_recorded = t
  (and usually now_valid = t)."
- `rule_version` is bitemporal too (`extensions.sql`, with `rule_window` giving "each version's
  knowledge-time end (when a fix replaced it)").
- **Three clocks, not two**, and the README is explicit that this was the v2 decision: "v2 adds
  `version.published_at` for the source's own time (EDGAR acceptance, an email's `Date:`, a Federal
  Register publication date)... Keeping `recorded_from` as the KB's own clock and putting the source's
  clock in its own column does that. AS_KNOWN means what this store said, the resolver can still ask
  'was this filing published before that 8-K?', and a backfilled filing is known from the day it
  arrived, not from its acceptance date."
- The v1.1 changelog entry records a real bitemporal bug found *by* designing the finance corpus:
  "CURRENT checked only `valid_to`, so a rule adopted but not yet in force (EU T+1, applicable from
  11 Oct 2027) showed as current. CURRENT now checks both valid-time bounds and the knowledge-time
  bounds against a frozen `clock` row."
- Valid time is derived differently per statement kind, which is the design's sharpest claim:
  `federal-register.yaml` sets `valid_from: {record: "fields.publication_date", normative: "has(fields.compliance_date) ? fields.compliance_date : fields.effective_on"}`.
- The ledger's canonical worked example, from the README: "A 10-K that its issuer later says not to rely
  on keeps its text and its versions. The 8-K adds a `standing = do_not_rely` row on it, valid from the
  10-K's own period ('the figures were never reliable') and recorded when the 8-K arrived ('we learned
  it then')."
- The private corpus `ledger/facts.csv` carries the same two axes in CSV form: `valid_from`, `valid_to`,
  `known_from`, exercised by F-002 (`valid_from 2017-09-05`, `known_from 2017-03-22`) and F-003
  (`valid_from 2024-05-28`, `known_from 2023-02-15`).

**What Episode 6 actually built for spectastic.**

- **No SQL schema exists.** `find . -name "*.sql"` over the whole working tree (excluding `build/` and
  `.git/`) returns exactly three files, all of them `docs/kb-schema/`: `schema.sql`, `extensions.sql`,
  `apply.sql`. There is no spectastic store.
- The index key is a git position, not a timestamp. `results/episode-6/README.md`: "**First-parent
  position is the index key.** It reproduces the declared before/intermediate/after order on all five
  `what-changed` questions, including G-069, whose two intermediates share a committer timestamp so no
  date can order them." And: "Committer date is monotonic along first-parent (0 backward steps) and
  author date is not... but committer date is not injective: 68 commits share a timestamp, in 16 groups.
  **Order by position, derive it once, never sort by a date at query time.**"
- Standing is a single label with no interval. `units/Standing.java`: "The search needs three things of
  it and nothing else — a label to store, whether to leave the record out, and which of two records
  answering the same question to prefer", exposing `String label()`, `boolean outOfDate()`,
  `int precedence()`. `spectastic/Lifecycle.java` is a 5-value enum: `CURRENT`, `HISTORY`, `WITHDRAWN`,
  `SUPERSEDED`, `REJECTED`, with `OUT_OF_DATE = EnumSet.of(WITHDRAWN, SUPERSEDED, REJECTED)`.
  `units/Units.java` stores it as metadata key `STANDING = "lifecycle"`. There is no `valid_from`,
  `valid_to`, `recorded_from` or equivalent in the unit metadata — the keys are `SOURCE`, `SCOPE`,
  `ANCHOR`, `TITLE`, `HEADING_PATH`, `HEADING`, `STANDING`, `PART`, `PARTS`, `SUPERSEDED_BY`,
  `PAGE_SUPERSEDED_BY`.
- Episode 6 does call its corpus bitemporal, and reports it as a win: "the bitemporal corpus alone takes
  the cutoff questions from 0 of 9 to 7 of 9 with no new retrieval mechanism at all — identifier lookup,
  standing filter, dense fusion and cross-encoder, unchanged", and "A bitemporal corpus is **1.36×** the
  snapshot, not 555×; a slice costs **0.09 s**."
- But its own coverage table treats the two time axes as one stratum, not two mechanisms:
  `| as-of + as-known | 9 | **8 answered** (TD), G-073 outstanding |`, with the summary "a bitemporal
  corpus answers the questions that name **one** point in time, almost completely. Questions that name
  **two** points need a mechanism that does not exist yet".
- The already-established multi-hop report records the matching absence on the edge side:
  "**The time corpus has no edges at all** — `timecorpus-c64d41c-v1.json` has three keys and
  `timecorpus.py` contains no edge structure. So no edge carries a validity interval, and the
  consequence is large: of the 7,956 distinct unit→unit snapshot edges, only **27.6%** had both
  endpoints in force at position 300 of 721". — `reports/Multi hop questions and graph retrieval.md`
- And it names a defect that is precisely a finance-schema concern wearing spectastic's clothes:
  "G-072's `expected` is `plan.html#D-005` against a `stale` of `design.html#D-005` — a file rename,
  i.e. a bitemporal addressing problem wearing a relation's clothes."

### Inferences

- **They do not match, and the gap runs one way.** The finance schema models valid time, knowledge time
  and source time as separate columns per version, per link and per judgment; Episode 6 models a
  revision position per unit plus a lifecycle enum. Episode 6's "as-of" and "as-known" distinction lives
  in the *question strata*, answered by slicing one axis — not in two stored axes. Nothing about
  Episode 6's implementation would need to change to adopt the finance schema, because there is nothing
  there to migrate; equally, nothing Episode 6 built reduces the work of standing up the finance store.
- The finance corpus is, however, the natural place to pay off two debts Episode 6 recorded and could not
  address with one axis: edges with validity intervals (the 386 dangling traversals at p=300) and
  `what-changed` questions needing two slices. The finance schema already has both — `link.valid_from`
  with `recorded_to`, and `clock` plus `status_now` for repeated AS_KNOWN slices — and the eCFR
  snapshots of 15c6-1 at four dates are a ready two-slice test.
- The schema's claim that it is generic rests on finance, by its own statement: "A schema fitted to one
  corpus is just that corpus's schema. The proof is a second, very different source going through an
  adapter with no change to the tables. The finance corpus (real filings and regulation plus a fictional
  private layer) is now that test, and **it has already changed the schema twice**: the CURRENT
  valid-time bug in v1.1, and the trust-replay defect and relationship rules in v2." That is a reason to
  go to finance that the author has already written down and half-collected on.

### Gaps

- The `episode-4-prototype` branch holds `tools/ratchet-proto/revisions.py` and `timecorpus.py`, which
  are where Episode 6's slicing actually lives. I inspected only the committed `episode-5` Java and the
  Episode 6 results README, so a residual possibility remains that the prototype carries valid-time
  fields the Java port dropped. The README's "Order by position... never sort by a date at query time"
  argues against it, but it is not proof.
- The README's "1.36× the snapshot" figure is a corpus-size ratio; whether it holds for a filings corpus
  with real valid-time intervals (rather than git revisions) is unmeasured.

---

## Cross-cutting: the vault notes, and what they add

### Takeaway

The two vault notes are the research behind the branch, not additional assets. One is a near-copy of the
repo README. The other, larger one post-dates the branch by six days and is the only artefact whose
content may have moved on.

### Cited Findings

- `/Users/briancorbin/Documents/Brain/Library/03 Resources/RAGs to Riches, Knowledge Base Schema.md`
  — 30 KB, **mtime Wed Oct 14 22:16:24 2026**. Its headings are the same 13 as
  `docs/kb-schema/README.md`, in the same order ("The principles it encodes", "The tables, in plain
  words", "Three clocks, and which one `recorded_from` is", "The status ledger", "Relationship rules in
  three layers", "Link types", "Retrieval modes, with status", "The ACME walkthrough, in brief", "Design
  choices worth knowing", "Is it generic? The test is a second corpus", "Files", "Open questions",
  "Changelog"). Its mtime is ~16 minutes after `9c05b45` (2026-10-14 21:59:48 -0400 = 01:59:48Z) in
  local time — i.e. written alongside v2.
- `/Users/briancorbin/Documents/Brain/Library/03 Resources/RAGs to Riches, Corpus Ingester.md`
  — 180 KB, **mtime Tue Oct 20 22:11:52 2026**, six days after the last branch commit. It is the
  research the v2 changelog credits ("from the ingester research and the business-rules research").
  Sections: "Build the ingester around a status ledger", "Five questions, five short answers",
  "Ten idempotent stages turn re-runs into no-ops", "A kanban board is a GROUP BY over a work-item
  table", "Promotion and demotion append rows; they never edit text", "Analysts should rule on sources,
  not documents", "Keep Lua's contract and swap its runtime", "**Twelve proposed schema changes (now in
  schema v2)**", "The ingester mostly rides outside the ratchet", then a second part: "Three rule layers
  decide what supersedes what", "Meaning, detection and consequence are three different rules", "Rules
  change two ways, and only one rewrites what we believed", "**Official publishers encode relations;
  unofficial ones only hint at them**", "The rules, as YAML".
- That note embeds the YAML verbatim — its lines 533-557 reproduce the `link-types.yaml` header comment
  word for word, including the `inverse`/`scope`/`effect`/`effect_on`/`effect_value`/`time_anchor`/
  `exclusion_requires`/`transitive`/`interop` field descriptions.
- Its "Official publishers encode relations; unofficial ones only hint at them" section (line 492) is
  the research the two SEC profiles cite as the missing
  `research_notes/.../official_relationships.md`.

### Inferences

- The vault's KB Schema note and `docs/kb-schema/README.md` are the same document in two places. For
  the decision, read the repo copy: it is version-controlled and identical in structure.
- The Corpus Ingester note is the one place where thinking continued after the branch stopped
  (20 Oct vs 14 Oct). Its "Twelve proposed schema changes (now in schema v2)" heading suggests it was
  updated to record what landed, so the drift is probably bookkeeping rather than new design — but it
  is the only artefact where that cannot be ruled out from mtimes alone.

### Gaps

- I read the vault notes' structure and mtimes only, per the read-only constraint and their size
  (180 KB). Whether the Ingester note's 20 Oct revision contains design decisions *not* in schema v2 is
  undetermined.

---

## Summary ledger: what exists, and how stale

| Asset | Where | Size / scale | State |
|---|---|---|---|
| Core schema (`schema.sql`) | **already on `episode-5`**, working tree | 38,358 B, 22 tables, 7 views | **Green.** Trace reproduces byte-for-byte on SQLite 3.53.4 vs the committed 3.45.1 |
| Extensions (`extensions.sql`) | `episode-5` | 50,614 B, 26 tables, 18 views | **Green.** Same run |
| Rule step (`apply.sql`) | `episode-5` | 6,341 B | **Green.** Exercised by the test's all-6-arrival-orders case |
| `link-types.yaml` | `episode-5` | 8,467 B, **19 types** (9 inherited from v1/spectastic) | Current. Cross-check vs the seeded table needs PyYAML |
| `consequences.yaml` | `episode-5` | 5,960 B, **8 rules** | Current. Compile-to-rule-bodies check needs PyYAML |
| Schema test | `episode-5` | 81,330 B, **84/93 checks** here (9 skipped for PyYAML), 0 failed | **Green** |
| 4 finance/reg profiles | `episode-5` | 19,252 B | **Design only.** 0 fixtures, 3 named Java beans unimplemented, cited research note absent |
| 3 private-layer profiles | `episode-5` | 13,241 B | Design only |
| Fetcher (`tools/corpus-fetch`) | **`origin/finance-corpus` only** — 3 commits, no Java coupling | 12 files, 1,209 lines, stdlib only | **Green. 24/24 tests pass on Python 3.14.7** |
| Fetch manifest | `rags-finance-corpus` `7899ef1` | **98 responses, all 200, 178,892,093 B**, 17.5 min | Valid, reproducible. **Bytes not retained** (`raw/` git-ignored, no working tree checked out) |
| Filings | same | **24 filings, 15 companies**, incl. `ipoe-402-2021` Item 4.02 | Fetched once, not retained |
| Regulatory texts | same | **22 fetched, 1 failed** (DTCC 2022 playbook, IA 429); 5 point-in-time eCFR snapshots of 15c6-1 | Fetched once, not retained |
| EDGAR searches | same | 8 queries, 674 hit lines | Fetched once |
| Ground-truth ledger | same | `facts.csv` **6 rows** bitemporal; `events.csv` **header only** | Barely started |
| Synthetic layer | same | README only (714 B) | **Not started** |
| Finance golden set | `golden/finance/` | — | **Does not exist.** Marked "(to come)" |
| Finance questions | `test_schema.py` | **6 ACME candidates** over synthetic rows | Template, not a set |
| Java tree reconciliation | — | — | **Not a cost.** `docs/kb-schema` is byte-identical across branches; the exclusive delta is Python |

---
---

# Added scope: can the kb-schema serve aggregation and top-K queries?

Context: the author has raised a new question class for the finance episodes — "top 5 companies a
company is exposed to", "top 10 companies exposed to company Y", "top K companies by exposure", built
from 13D/13F/13G. These are relational aggregations (join, group, order, limit), not top-k passage
retrieval.

**Verdict in one line: the schema models documents, units, texts, typed relations and judgments. It does
not model a measured fact extracted from inside a filing. There is no column anywhere in
`schema.sql` or `extensions.sql` that can hold a number you would sum.** The bitemporal machinery that
would make an "as at Q2 2026" aggregate correct is already built and is the strongest part of the
argument; the fact table it would operate on does not exist. That gap is small, well-bounded and
squarely in the schema's own extension idiom — but it is a gap, and it is the whole of the work.

## Does `schema.sql` already support a holdings aggregation?

### Takeaway

No. A 13F holdings row needs (manager, issuer, CUSIP, value, shares, period) — six attributes, two
entity endpoints, two numeric measures. **Every candidate table in the schema fails on the numeric
measure, and most also fail on the second endpoint.** The `GROUP BY issuer ORDER BY sum(value) DESC
LIMIT 10` cannot be written against the schema as it stands, because there is no numeric column to pass
to `sum()`.

### Cited Findings

**Candidate 1: `facet` — document-scoped string tags, not rows.**

```sql
CREATE TABLE facet (                        -- domain detail that stays out of core
  document_id INTEGER NOT NULL REFERENCES document,
  name TEXT NOT NULL, value TEXT NOT NULL,  -- 'sec_status'='furnished', 'assurance'='audited', 'tlp'='AMBER'
  PRIMARY KEY (document_id, name, value)) WITHOUT ROWID;
```
— `extensions.sql`

It attaches to a `document`, not to an entity pair; `value` is `TEXT`; and because `value` is in the
primary key it is a set of tags, not a record with fields. One 13F holds hundreds of positions; `facet`
can express "this document is `furnished`", not "this manager held 412,000 shares of issuer X worth
$18.3m at 2026-06-30". The profiles use it exactly as designed — `eur-lex.yaml` declares
`facets: [{name: first_date_entry_in_force, ...}, {name: date_applicability, ...}]` and
`federal-register.yaml` declares `effective_on`, `compliance_date`, `rin`. All single-valued document
attributes.

**Candidate 2: `claim` — free text, one subject, no object, no measure.**

```sql
CREATE TABLE claim (
  claim_id INTEGER PRIMARY KEY, text TEXT NOT NULL,
  modality TEXT NOT NULL DEFAULT 'asserted' CHECK (modality IN ('asserted','hedged','denied','in_universe')),
  subject_entity INTEGER REFERENCES entity,           -- when the subject is known at ingest
  subject_mention_id INTEGER REFERENCES mention,      -- when it is only a mention ("ACME")
  as_of TEXT);
```
— `extensions.sql`

`text` is a sentence. The ACME trace shows exactly what a claim is in practice:
`claim_id 1 | "Acme United is a party to pending material legal proceedings" | subject: Acme United
Corporation`. There is one subject and no object entity, so a manager→issuer pair has nowhere to go, and
nothing numeric. `as_of TEXT` is the only quantitative-adjacent column and it is a date.

**Candidate 3: `entity_link` — the one entity→entity table, and it is unweighted.**

```sql
CREATE TABLE entity_link (                  -- Acme Markets brand_of Albertsons; Acme Corporation depicted_in the film
  from_entity INTEGER NOT NULL REFERENCES entity, to_entity INTEGER NOT NULL REFERENCES entity,
  type TEXT NOT NULL CHECK (type IN ('subsidiary_of','brand_of','depicted_in','acquired_by','successor_of')),
  valid_from TEXT, valid_to TEXT, origin TEXT NOT NULL,
  PRIMARY KEY (from_entity, type, to_entity)) WITHOUT ROWID;
```
— `extensions.sql`

This is the closest thing in the schema to the edge the author's questions need, and it fails three
ways: (a) **no measure column** — no `value`, `shares`, `weight` or `pct`; (b) the `type` vocabulary is
a closed five-value `CHECK` with no holding, exposure or position relation, and a new type means altering
the constraint; (c) `PRIMARY KEY (from_entity, type, to_entity)` permits **exactly one row per entity
pair per type**, so the same manager→issuer pair cannot carry a Q1 row and a Q2 row. Quarterly holdings
are inherently many rows per pair.

**Candidate 4: `link` — unit-to-unit, and its only `REAL` is epistemic.**

```sql
  origin        TEXT NOT NULL CHECK (origin IN ('markup','status+link','prose-pattern','model','human')),
  confidence    REAL NOT NULL DEFAULT 1.0 CHECK (confidence BETWEEN 0 AND 1),
```
— `schema.sql`

`confidence` is the only floating-point column on `link`, it is bounded `BETWEEN 0 AND 1`, and
`consequences.yaml` uses it solely as an epistemic gate (`min_confidence: 0.8` deciding `apply` vs
`label`). It is how sure we are that the relation exists, never how much of anything it represents.
`link` also connects `from_unit` to `to_unit`/`to_document`/`to_key` — documents and passages, not
entities.

**What the schema does have that a holdings pack would need:** the identifier layer is genuinely ready.

```sql
CREATE TABLE entity_identifier (            -- CIK, LEI, ticker+venue, with validity: never invent one
  entity_id INTEGER NOT NULL REFERENCES entity,       -- for a fictional entity (it has none)
  scheme TEXT NOT NULL, value TEXT NOT NULL, venue TEXT,
  valid_from TEXT, valid_to TEXT,
  PRIMARY KEY (entity_id, scheme, value)) WITHOUT ROWID;
```
— `extensions.sql`

CIK is already a modelled, validity-bounded identifier scheme, and `entity.kind` already includes
`'issuer'` in its `CHECK`. CUSIP is not among the schemes used anywhere, but `scheme` is free text, so
`('cusip', '00846U101')` needs no DDL change. `key_scheme` (`extensions.sql`) is the typed-key registry
that would declare it.

**Where an information table lands today.** `version.body TEXT NOT NULL` — "exactly what quotations are
matched against". And `sec-edgar-10k.yaml` makes the treatment explicit:
`split_long: {max_tokens: 1200, keep_tables_whole: true}`. **Tables are kept whole as text so they
survive chunking — they are passages to retrieve, not rows to parse.** That one line is the clearest
statement in the branch that the design's unit of truth is the quoted passage, not the extracted datum.

**The `GROUP BY` test, concretely.** The query the author wants is of the shape:

```sql
-- NOT WRITABLE against the schema as it stands
SELECT issuer.name, sum(h.value) AS exposure
FROM holding h JOIN entity issuer ON issuer.entity_id = h.issuer_entity
WHERE h.period = '2026-06-30'
GROUP BY h.issuer_entity ORDER BY exposure DESC LIMIT 10;
```

There is no `holding` table and no `h.value`. Every aggregate that *does* appear in the schema is
structural bookkeeping over times and labels, never over a measure — the complete list, from
`grep -nE "sum\(|count\(|avg\(|GROUP BY|group_concat"` across all three SQL files:

- `unit_time`: `SELECT unit_id, min(valid_from) ..., min(published_at) ..., min(recorded_from) ... FROM version GROUP BY unit_id`
- `document_time`: `min(t.valid_start) ... GROUP BY u.document_id`
- `status_now` / `current_labeled`: `group_concat(p.value || ' (provisional, ' || p.authority || ')', '; ')` and `group_concat(s.flags, '; ')` — building label strings
- `desired_link_resolution`: `ROW_NUMBER() OVER (PARTITION BY k.link_id ...)` and `COUNT(*) OVER (PARTITION BY k.link_id) AS total` — counting binding candidates to detect ambiguity
- `work_item`, in a comment only: `-- the board: SELECT stage, state, count(*) FROM work_item GROUP BY 1, 2;`

`min()`, `count(*)` and `group_concat()` over dates, rows and labels. **No `sum()` and no `avg()`
anywhere in 95,313 bytes of SQL.**

### Inferences

- **This is the crucial gap, stated plainly: the schema models documents and links but not the facts
  inside a filing's information table.** Every one of the author's three questions is a `sum()` over a
  measure the schema has no column for.
- The gap is a *missing pack*, not a design conflict. The schema's own idiom invites it: `facet` is
  introduced as "domain detail that stays out of core", and the README's design note says "The ledger is
  core, the rule engine is an extension... a pack you adopt when you have relations worth deriving
  from." A `holding` table — `(ingest_run_id, version_id, manager_entity, issuer_entity, cusip, period,
  value, shares, share_type)` with the three time columns — is perhaps 15 lines of DDL that references
  `entity`, `version` and `ingest_run` as they already stand, and would compose with `assessment`
  (a 13F/A restating a position becomes a ledger row, not an edit) exactly as the existing tables do.
- The honest framing for the decision: the aggregation idea is **not** supported by what exists, but it
  is also **not blocked** by it. What exists is the hard half — identity, validity, provenance, status —
  and what is missing is the easy half, which nobody has written.

### Gaps

- No design note anywhere proposes a holdings or quantitative-fact table. The README's "Open questions"
  section lists eight items; none concerns numeric facts or aggregation. The idea is new as of this
  request, and nothing in the branch anticipates it.

## What do `link-types.yaml` and `consequences.yaml` express about quantified relations?

### Takeaway

Nothing. Both files are entirely qualitative. Every value either names a **status** from a closed
vocabulary or an **epistemic confidence** in [0,1]. The schema has unweighted unit→unit and
document→document relations whose payload is a status label; it does not have weighted entity→entity
edges. An aggregatable edge is precisely what it lacks.

### Cited Findings

- The three fields in `link-types.yaml` that could carry a quantity do not. `effect` is one of exactly
  three words — the header says: "`effect` — `none` navigation and ranking only / `label` shown beside
  the text; never changes membership / `standing` may change CURRENT/OFFICIAL membership (an
  exclusion)". `effect_value` is a status string drawn from `status_value` (`amended`, `corrected`,
  `superseded`, `do_not_rely`, `restated`, `repealed`, or `null`). `time_anchor` is one of `link`,
  `target`, `emitter` — a date source, not a magnitude.
- There is no `weight`, `value`, `quantity`, `measure` or `pct` key in any of the 19 link-type entries.
  The only numeric-typed field in the layer-1 catalogue is `transitive`, and it is a boolean that is
  `false` on every single type.
- `consequences.yaml`'s only number is `min_confidence`, and it is a threshold on belief, not a
  quantity: `{authority: publisher, origin: "*", min_confidence: 0.8, outcome: apply, dimension: standing, value: do_not_rely}`.
  Its three outcomes are `apply`, `label`, `none`.
- The dimension/value space is closed and categorical. `status_value` enumerates every legal value
  across seven dimensions, with `supports TEXT NOT NULL CHECK (supports IN ('yes','labelled','no'))`
  and `official INTEGER NOT NULL CHECK (official IN (0,1))`. The `assessment_vocab` trigger enforces it:
  `WHEN NEW.value <> 'void' AND NOT EXISTS (SELECT 1 FROM status_value s WHERE s.dimension = NEW.dimension AND NEW.value GLOB s.value) BEGIN SELECT RAISE(ABORT, 'unknown dimension/value (see status_value)'); END;`
  **A number could not be stored as an assessment value at all** — it would have to match a
  `status_value` GLOB pattern, and only `sensitivity` has a numeric-looking one (`'[0-3]'`).
- `value_rank INTEGER NOT NULL` on `status_value` is "lower = more severe; breaks ties between live
  rows" — an ordinal for conflict resolution between judgments, not a measure of anything in the world.

### Inferences

- The schema's relation layer answers "does this relation hold, how do we know, and what does it do to
  retrieval membership" — three qualitative questions. The author's questions ask "how much", which is a
  fourth question the layer was not built for and does not pretend to answer.
- Adding a measure to `link` would be the wrong move and the schema's own rules say why: `link` is
  append-only with `recorded_to` closable once, and `consequences.yaml` reads `confidence` as belief.
  Overloading it would conflate "we are 0.9 sure this relation exists" with "this position is 0.9% of
  the portfolio". A separate `holding` table keeps the two apart, which is the same reasoning the schema
  already applies in "Text changes make versions; judgment changes make assessments" and in splitting
  `published_at` from `recorded_from`.

### Gaps

- Whether a quantified edge should emit status effects at all (does a position falling below 5% emit
  anything?) is unaddressed by any artefact, and is a genuine design question rather than a lookup.

## Could an aggregate be computed "as of" a date?

### Takeaway

**The machinery is there and is the single strongest asset for this question class — it just has nothing
to aggregate yet.** Every column needed for "top 10 holders of Y as at Q2 2026, using only what had been
filed by then" exists and is already exercised by the test. Crucially, the schema's *three*-clock design
is exactly right for 13F, where the 45-day filing lag means period end, filing date and ingestion date
are three genuinely different times.

### Cited Findings

- **The three clocks, named.** On `version`: `valid_from`/`valid_to` ("when something holds in the
  world"), `recorded_from`/`recorded_to` ("when **this knowledge base** recorded it"), and
  `published_at` ("when the source published / sent / accepted it"). The README is explicit about why
  the third exists: "v2 adds `version.published_at` for the source's own time (EDGAR acceptance...)...
  AS_KNOWN means what this store said, the resolver can still ask 'was this filing published before that
  8-K?', and a backfilled filing is known from the day it arrived, not from its acceptance date."
- **The frozen clock is the "as of" mechanism**, and it is two-dimensional:
  ```sql
  CREATE TABLE clock (
    id            INTEGER PRIMARY KEY CHECK (id = 1),
    now_valid     TEXT NOT NULL,             -- the valid-time "today": a date (start of day) or timestamp
    now_recorded  TEXT NOT NULL              -- the knowledge-time "now", a timestamp
  );
  ```
  with the stated protocol: "**AS_KNOWN(t)** is the same views after `UPDATE clock SET now_recorded = t,
  now_valid = t`." A top-K aggregate written as a view over a `holding` table would inherit this for
  free — `JOIN clock c ON c.id = 1` is how every existing view does it, and the ACME-03 query does
  exactly that.
- **"Only what had been filed by then" is already implemented, for links.** The resolver view's header:
  "Candidates for a keyed link: identifier rows with the same scheme and value, in another document
  **published no later than the linking document (source time, so the 10-K/A that an 8-K announces can
  never be its target)**. Knowledge time of a candidate = when this KB recorded its document."
  (`extensions.sql`, `resolve_key_candidate`). That is precisely the filed-by-then predicate the
  aggregate needs, over the same `published_at` column.
- **Valid-time bounds are checked on both sides**, after a bug the finance corpus itself found. v1.1
  changelog: "CURRENT checked only `valid_to`, so a rule adopted but not yet in force (EU T+1,
  applicable from 11 Oct 2027) showed as current. CURRENT now checks both valid-time bounds and the
  knowledge-time bounds against a frozen `clock` row."
- **The entity layer is validity-aware too**, and the test proves it. `entity_identifier.valid_from`/
  `valid_to` and `entity_link.valid_from`/`valid_to` both exist, and ACME-03's query uses them:
  `CASE WHEN ei.valid_to IS NOT NULL AND ei.valid_to <= c.now_valid THEN 'historical' ELSE 'current' END AS filer`,
  asserting `{("2098", "current"), ("1646972", "current"), ("1130258", "historical")}`. So an
  as-of aggregate would correctly treat a CIK that lapsed, and correctly follow `brand_of`/
  `subsidiary_of` only while those relations held.
- **A restatement would be handled by the ledger, not by an update.** `assessment` carries
  `valid_from`, `valid_to`, `recorded_from` and `supersedes`, is append-only by trigger, and
  `status_now` computes effective status under the frozen clock: "A row counts if it was recorded by
  `now_recorded`, is valid at `now_valid` and has not been superseded by a row recorded by then."
  A 13F/A correcting a position is the same shape as the 10-K → 8-K 4.02 → 10-K/A chain the test
  already walks in all six arrival orders.
- **Rule-level time too**: `rule_version` is bitemporal with `change_kind` `fix` vs `new`, so a change to
  *how* exposure is computed replays differently from a change in *what* the world requires.

### Inferences

- An as-of top-K over a `holding` table would need no new temporal machinery at all: the columns, the
  frozen clock, the both-bounds CURRENT semantics, the filed-by-then predicate and the append-only
  restatement path are all built and all tested. A `holding` table carrying `valid_from` (period end),
  `published_at` (acceptance) and `recorded_from` (ingestion) would drop straight into the existing
  idiom.
- **This is the sharpest argument for the finance corpus that the artefacts support.** The three-clock
  distinction is close to unobservable on spectastic — the README concedes "In spectastic only 9% of live
  elements carry their own date", and Episode 6 found committer date so unreliable it keyed on git
  position instead. On 13F it is unavoidable and consequential: a Q2 position is *valid* at 30 June,
  *knowable* only from mid-August, and a reader asking "who held Y as at Q2" on 1 August must get a
  different, correct, smaller answer than one asking on 1 September. No spectastic question can make
  that distinction bite.
- It also lands a defect Episode 6 recorded and could not fix with one axis: "no edge carries a validity
  interval... of the 7,956 distinct unit→unit snapshot edges, only 27.6% had both endpoints in force at
  position 300 of 721" (`reports/Multi hop questions and graph retrieval.md`). A holdings edge with
  `valid_from`/`valid_to` is a validity-bearing edge by construction.

### Gaps

- `status_now` is flagged as unproven at scale in the README's open questions: "A status join on
  contentless FTS at a million versions. `status_now` is a window function over the live ledger; check
  it at scale before relying on it, or materialise it stamped with the clock it was built for." A
  13F corpus is the first thing in this series that would have enough rows to test that, and the cost of
  an as-of aggregate joining it is unmeasured.
- The three-clock design has only ever run against the test's hand-seeded rows. No fetched filing has
  been through it.

## Do the profiles extract numeric facts? Is there a 13F/13D/13G profile?

### Takeaway

**No numeric facts, and no profile for any of the three form families the author named.** The two SEC
profiles extract document structure, cover-page metadata and prose dates — nothing from a financial
statement or an information table. Both use iXBRL for `dei:` document metadata only; no `us-gaap:`
concept or any other financial tag appears in any profile. And `13F`, `13D`, `13G` and `CUSIP` appear
**nowhere on the branch at all** — not in the profiles, not in the schema, not in the fetcher's config.

### Cited Findings

- **`sec-edgar-10k.yaml` extracts eight fields, none of them a financial figure**: `cik`, `form`,
  `accession`, `report_date`, `accepted`, `period_end`, `amendment_flag`, `amendment_description`. The
  three XPath expressions are all document metadata:
  `//ix:nonNumeric[@name='dei:DocumentPeriodEndDate']`, `//ix:nonNumeric[@name='dei:AmendmentFlag']`,
  `//ix:nonNumeric[@name='dei:AmendmentDescription']`. **Every one is `ix:nonNumeric`.** There is no
  `ix:nonFraction` expression (the iXBRL element that carries numeric facts) in any profile.
- The profile's own field-status header names only `dei:` concepts as verified: "dei:DocumentType,
  dei:AmendmentFlag, dei:DocumentPeriodEndDate, dei:EntityCentralIndexKey, dei:EntityFileNumber". No
  `us-gaap:` concept is named anywhere in the branch.
- Its unit model is sectional, not tabular: `anchor: item` ("'Item 1A', 'Item 8'; the cover page is
  'cover'"), `heading_path: "'Part ' + unit.part + ' > ' + unit.item + '. ' + unit.title"`, and the
  quality gates are about prose volume — `"parse.items_found >= 4"`, `"parse.text_chars > 20000"`.
  Item 8 is where financial statements live, and the profile's only use of it is as a *link source*:
  `from: "units.filter(u, u.item == 'Item 8')"` for the `restates` relation.
- **`sec-edgar-8k.yaml` extracts five prose patterns, all of them dates or form names**: `period_ended`,
  `as_of_date`, `form_name`, `originally_filed`, `conclusion_date`. No numeric pattern.
- `eur-lex.yaml` and `federal-register.yaml` likewise extract only dates, identifiers and relation
  targets. `federal-register.yaml`'s one regex over body text is `'Amend § ?(\d+\.[\d-]+)'` — a CFR
  section number, not a quantity.
- **No 13F/13D/13G profile exists.** `profiles/` contains exactly seven files: `sec-edgar-10k.yaml`,
  `sec-edgar-8k.yaml`, `eur-lex.yaml`, `federal-register.yaml`, `email.yaml`, `chat-slack.yaml`,
  `research-note.yaml`.
- **Nor anywhere else on the branch.** `git grep -n "13F\|13D\|13G\|CUSIP\|cusip" origin/finance-corpus`
  returns **no match in any schema, profile, config or test** — the only hits for the search were the
  substring "holding"/"Holding" in unrelated prose (`apply.sql`'s "a derivation that stops holding",
  `GoldenScorerTest.java`'s "aChunkHoldingTheWholeElementIsAHit"). The same grep restricted to
  `episode-5 -- docs/kb-schema` returns nothing.
- **The fetcher never asked for them either.** `config/companies.json` requests only
  `"forms": ["10-K"]`, `["20-F"]` and `["40-F", "10-K"]`; its three `explicit` entries are an S-1 and two
  8-Ks. The live run's `filings.jsonl` confirms the outcome: 24 filings, forms **10-K (18), 20-F (3),
  S-1 (1), 8-K (2)**. Zero 13F, 13D or 13G.
- The two SEC profiles' `match` expressions would not even route such a filing: both require
  `fields.form in [...]` over 10-K and 8-K variants respectively, so a 13F arriving at the ingester
  would match no profile and fall to `review: [{when: "fields.cik == '' || period() == ''", queue: {class: standard, reason: unclassifiable-arrival-path}}]` —
  quarantine, as designed ("quarantine; never default silently").

### Inferences

- **The three form families the author named are exactly the three with no profile, no fetch and no
  mention.** That is a clean statement of the work: a new source profile per family, a numeric fact pack,
  and a fetch config extension. None of it is blocked by anything; none of it is started.
- There is a real asymmetry worth naming. The existing profiles were designed around *relations between
  documents* because that is what 10-K and 8-K give you. 13F gives something different in kind: a
  structured XML information table of thousands of numeric rows, with no prose and no interesting
  document relations at all. A 13F profile is therefore **not a variation on the existing ones** — it is
  the first profile whose output is rows rather than units and links, which is why the missing `holding`
  table and the missing profile are one piece of work, not two.
- This also means the aggregation idea would exercise the schema's genericity claim far harder than the
  current corpus does. The README's test for genericity is "a second, very different source going
  through an adapter **with no change to the tables**". A holdings pack would change the tables. That is
  worth knowing before it is presented as the proof.

### Gaps

- `research_notes/Schema implied relations in spectastic/schema_implied_edges.md` (45 KB, mtime
  2026-10-28 11:33:39 — the most recently updated artefact in this whole inventory) already covers the
  13F family, 13D/13G, EDGAR's submissions and XBRL APIs in depth, and is taken as given per the brief.
  It is the one place where the author's new question class may already have been researched; I did not
  re-derive it, so whether it proposes a data model (as opposed to an edge family) is outside what I
  checked.

## Does anything in the branch exercise an aggregation or a numeric rollup?

### Takeaway

**No.** Nothing in the fetcher, the tests, `gen_seed.py` or the ACME walkthrough computes or checks a
numeric rollup. The 84-check end-to-end test is entirely about *status, identity, time and provenance* —
it demonstrates that the right passages are admitted or excluded, and that judgments replay. It never
demonstrates that a number is right, because no number is stored. The evaluation tables could not score
a top-K entity answer even if one were produced.

### Cited Findings

**What the end-to-end test actually demonstrates.** `tests/acme_walkthrough_output.txt` (46,588 B,
485 lines) has seven sections:

| Section | What it proves |
|---|---|
| A. "ACME: one rumour, its resolution, and the real Acmes" | entity ambiguity (`ambiguous:4` → `entity:4`), claim modality, world/verification/resolution dimensions |
| A2. "The ledger (layer 3 output and human rows), in knowledge-time order" | derived rows carry authority, provisionality and premises |
| A3. "Golden questions (ACME report table, plus two drafted in the same style)" | the six ACME-0n questions at three clocks |
| A5. "A Slack edit is a new version of the same unit, not a 'replaces' link" | versions vs links |
| B. "10-K → 8-K Item 4.02 → 10-K/A (fictional issuer Harrowgate Securities, x-cik 9000001)" | keyed-link resolution, backfill, **order independence across all 6 arrival orders** |
| C. "2022 T+1 playbook superseded by SEC Release 34-96930 (hand-written link)" | human-authored supersession, withdrawal and re-entry, cycle safety |
| D. "Negative tests: what the schema refuses" | the CHECKs and triggers |
| E. "Rule versions: a FIX (interpretation) versus a NEW version (the world's rule changed)" | bitemporal rule replay |
| F. "Integrity: rule hashes, catalogue and profiles" | sha256 of rule bodies, catalogue/YAML agreement |

Every assertion is set-equality, membership or ordering. Representative checks from the trace:
`[ok] the rumour's ACME resolves to the fictional Acme Corporation, world = universe of 'Coyote vs.
Acme', real-world reading refuted`; `[ok] no claim that a real Acme is being sued has real-world
support`; `[ok] as known at 09:00 the rumour is open: ambiguous, unverified, no flags`.

- **Claims in the trace are sentences, not quantities.** The ACME-01 claims table holds
  `"Acme United is a party to pending material legal proceedings"`, `"ACME is being sued"`,
  `"Acme Corporation is being sued (by Wile E. Coyote)"`. Its columns are `world`, `world_flags`,
  `verification`, `verification_flags`, `rw_support`, `stance` — all categorical.
- **The closest thing to the author's query in the entire branch is ACME-03**, and it is genuinely
  encouraging as a precedent. From `tests/test_schema.py` lines 454-462:
  ```sql
  WITH cand AS (SELECT entity_id FROM alias WHERE surface = 'ACME'
                UNION SELECT el.to_entity FROM entity_link el JOIN alias a ON a.entity_id = el.from_entity
                WHERE a.surface = 'ACME' AND el.type IN ('brand_of','subsidiary_of'))
  SELECT e.name, ei.value AS cik, CASE WHEN ei.valid_to IS NOT NULL AND ei.valid_to <= c.now_valid
                                      THEN 'historical' ELSE 'current' END AS filer,
         (SELECT group_concat(x.name) FROM entity_link l JOIN entity x ON x.entity_id = l.from_entity
          WHERE l.to_entity = e.entity_id AND l.type = 'brand_of') AS via
  FROM cand JOIN entity e USING (entity_id) JOIN entity_identifier ei ON ei.entity_id = e.entity_id AND ei.scheme = 'cik'
  JOIN clock c ON c.id = 1 ORDER BY filer, e.name
  ```
  This is a **relational entity query, not passage retrieval**: a set-building CTE over `alias` ∪
  `entity_link`, a join to validity-bounded CIKs, a valid-time test against the frozen clock, a
  correlated `group_concat` for the provenance path, and an `ORDER BY`. It has joins, grouping-by-proxy
  and ordering. **What it does not have is a measure and a `LIMIT`** — the shape is right, the payload is
  missing.
- **The evaluation tables cannot score a ranked entity list.** `expected` is keyed on units:
  `unit_id INTEGER NOT NULL REFERENCES unit`, with `role CHECK (role IN ('answer','stale','decoy','conflict','poison','inject','candidate'))`.
  `result` records `first_hit` ("rank of the first expected unit, NULL = miss"), `all_evidence`
  ("rank by which every expected unit was retrieved"), `stale_in_top`, `poison_in_top`,
  `followed_injection`, `leaked`. **Every metric is a rank over retrieved passages.** There is no column
  for a correct ordered list of entities, a numeric answer, or a tolerance.
- `answer.mode` is `'CURRENT'` by default with the comment `-- CURRENT, HISTORY, AS_OF, CHANGE, AS_KNOWN`.
  No aggregate or top-K mode.
- **`gen_seed.py` (3,520 B) does no arithmetic.** Its job, per the README: "Regenerates the seed in
  `extensions.sql` from the two YAML files (needs PyYAML)" — YAML to canonical JSON to sha256.
- **The fetcher counts bytes and requests, nothing else.** Its manifest fields are `url`, `file`,
  `sha256`, `bytes`, `status`, `content_type`, `fetched_at`; its 24 tests cover "the rate limiter, the
  User-Agent check, gzip, caching and the manifest..., the 11-minute throttle pause and the stop on a
  second throttle..., full-text search pagination and its HTTP-200 error bodies, submissions parsing...,
  picking the main document from `index.json`, and eCFR dates that don't exist"
  (`tools/corpus-fetch/README.md`). No parsing of filing content at all — it fetches bytes and records
  digests.
- Section F ("Integrity") is the only section that computes anything, and it computes hashes:
  `consequences.yaml compiles to exactly the seeded rule bodies (8 rules)`,
  `link-types.yaml matches the seeded link_type table (19 types)`.

### Inferences

- The 84 passing checks are strong evidence for a claim the author can make today — that status, time and
  identity are modelled correctly and replay deterministically — and **no evidence at all** for the
  aggregation claim. Presenting the test as support for top-K exposure queries would overstate it.
- ACME-03 is the right thing to point at when arguing the schema is *ready* for this class of question:
  it shows the entity layer already answering a join-and-order question with valid-time correctness, and
  it shows the author has already written one. Three of the four ingredients of
  `GROUP BY issuer ORDER BY sum(value) DESC LIMIT 10` are demonstrated; the fourth is the one with no
  column behind it.
- The evaluation gap compounds the earlier finding that there are no finance questions. A top-K
  aggregation episode would need **a new scoring shape as well as a new question set** — rank
  correlation or set overlap over an ordered entity list, which `result`'s six rank-based columns cannot
  express. That is a second, independent piece of missing work, and it is the kind this series takes
  seriously (the ratchet is the method).

### Gaps

- Whether the author intends these aggregates to be answered *by SQL* (a tool the model calls) or *by
  retrieval over a pre-computed passage* is undetermined by any artefact, and the two imply very
  different work. The schema's design — "Measure inside the system: the golden set, runs and labels live
  next to the data, so the ratchet is a query" — suggests the former, but nothing states it.
- No artefact estimates the row count a 13F corpus would reach, so the scale question flagged against
  `status_now` cannot be sized from what exists.
