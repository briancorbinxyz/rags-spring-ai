# Schema-implied relations in spectastic: what the meta declares

All sources are local files in the spectastic checkout at `/Users/briancorbin/Code/rags/spectastic` (full clone, 722 commits) and the parsed corpus at `/Users/briancorbin/Code/rags/rags-spring-ai/tools/ratchet-proto/cache/corpus-c64d41c-units-v1-ep4-p250.json`. Citations are given as file path plus element id, since there are no URLs. All counts were produced by grep/Python over the checkout on 2026-10-24 and the exact measurement scope is stated with each number.

**Headline:** spectastic's meta defines the artifact *family* explicitly and normatively, and defines the *within-artifact* vocabulary explicitly and normatively (`REQ-AUTHOR-001`: "Tag name is schema"). But almost none of the *between-artifact* derivation relations — design-from-spec, tasks-from-design — are declared anywhere normative. They live in command markdown, which the meta's own `P-8` explicitly disqualifies as a place a guarantee may live. The one derivation relation that *is* a MUST, with a validator behind it, is verify → (spec + tasks).

---

## What are the artifact types, and where is the family defined?

### Takeaway
The family is defined in two normative places and one descriptive one. `REQ-LIFECYCLE-001` (spec `089-lifecycle-contract`) is the only MUST that enumerates artifact types, and it enumerates **five templates** — `principles.html`, `spec.html`, `design.html`, `tasks.html`, `proposal.html` — which notably **excludes** `verify.html`, `triage-log.html`, `inbox.html` and `roadmap.html`. Those four are each defined by a separate spec or by convention, never collected into one enumeration. `verify.html` is defined as a *derived view*, a distinct category.

### Cited Findings
- `REQ-LIFECYCLE-001` (priority `must`): "The tooling **MUST** ship five lifecycle templates corresponding to the spec-driven lifecycle: `principles.html`, `spec.html`, `design.html`, `tasks.html`, and `proposal.html`. Each **MUST** use the same design system." — `/Users/briancorbin/Code/rags/spectastic/specs/089-lifecycle-contract/spec.html`, element `id="REQ-LIFECYCLE-001"`
- `089-lifecycle-contract` is itself a carve-out of the meta-spec and says so: "This spec is a slice of the spectastic meta-spec. Its requirements were authored there and moved here intact, keeping their stable ids: a requirement that was `REQ-LIFECYCLE-001` in the parent is `REQ-LIFECYCLE-001` here." — same file, §1 Context
- `REQ-LIFECYCLE-005` names a three-member *bundle* (not five, not six): "The three artifacts of a single spec slice — `spec.html`, `design.html`, and `tasks.html` under `specs/<spec-id>/` — **MUST** share a single bundled lifecycle status." — `specs/089-lifecycle-contract/spec.html`, `id="REQ-LIFECYCLE-005"`
- `REQ-LIFECYCLE-004` enumerates a *different*, wider set when it talks about status: "The lifecycle status of any artifact (spec, plan, tasks, principles, triage log, inbox) **MUST** evolve as a finite state machine over the six values named in `REQ-LIFECYCLE-002`" (`Draft`, `In Review`, `Accepted`, `Superseded`, `Deprecated`, `Blocked`) — same file, `id="REQ-LIFECYCLE-004"` and `id="REQ-LIFECYCLE-002"`
- `verify.html` is defined by spec `021-verify-view`, `FR-001`: "A `spectastic verify <spec-id>` generator **MUST** produce `specs/<spec-id>/verify.html` as a single self-contained HTML artifact in the spectastic design system." — `specs/021-verify-view/spec.html`, `id="FR-001"`
- `verify.html` is a distinct *statusless* category, declared twice: `021` `FR-007` "`verify.html` **MUST NOT** carry its own `<spec-status>`; it derives its lifecycle state from the spec bundle (per `REQ-LIFECYCLE-005`)" — `specs/021-verify-view/spec.html`, `id="FR-007"`; and `020` `FR-014` "A derived-view artifact — a regenerated, statusless view of the bundle such as `verify.html` — **MUST** render as a distinct node category." — `specs/020-vscode-extension/spec.html`, `id="FR-014"`
- `triage-log.html` is referenced as a path by requirement but never given a family-membership requirement: `007-core-triage` `FR-004` "Kernel returns one `TriageCard`; caller persists to `specs/<spec-id>/triage-log.html`." — `specs/007-core-triage/spec.html`, `id="FR-004"`; and `090-command-surface` `REQ-TOOL-005` "an open `<spec-triage>` card in a spec's `triage-log.html`" — `specs/090-command-surface/spec.html`, `id="REQ-TOOL-005"`
- The lifecycle *order* is declared twice outside `089`. `REQ-TOOL-001` (priority `must`): "Spectastic **MUST** ship eight Claude Code slash commands across the lifecycle — `/spectastic.principles`, `/spectastic.spec`, `/spectastic.design`, `/spectastic.tasks`, `/spectastic.implement`, `/spectastic.propose`, `/spectastic.apply`, `/spectastic.triage`" — `specs/090-command-surface/spec.html`, `id="REQ-TOOL-001"`. And `020` `FR-001`: the panel "**MUST** render the selected spec's lifecycle as a canvas of nodes — one node per existing artifact file — ordered along the lifecycle sequence `principles → spec → design → tasks → implement → propose → apply → triage` … plus any derived-view artifacts the bundle carries (e.g. `verify.html`)" — `specs/020-vscode-extension/spec.html`, `id="FR-001"`
- The repo front door states the family descriptively, with paths: "Four core phases. Each is a Claude Code slash command that fills a template." then `/spectastic.principles → ./principles.html` ("The non-negotiable principles every downstream artifact must respect"), `/spectastic.spec → specs/<id>/spec.html`, `/spectastic.design → specs/<id>/design.html`, `/spectastic.tasks → specs/<id>/tasks.html` — `/Users/briancorbin/Code/rags/spectastic/index.html`, §"The lifecycle". The same page says "One file. Twelve semantic custom elements."
- The shipped template set on disk is 12 files, wider than `REQ-LIFECYCLE-001`'s five and with no `verify.html`: `design.html`, `explore.html`, `inbox.html`, `knowledge/`, `principles.html`, `proposal.html`, `screen.html`, `spec.html`, `tasks.html`, `tokens.html`, `triage-log.html`, `variants.html` — `ls /Users/briancorbin/Code/rags/spectastic/templates/`
- `commands.json` declares the verb tiers, which is the nearest thing to a machine-readable family manifest: `core` = principles, spec, design, tasks, implement, propose, apply, triage; `extended` = explain, explore; "A command file whose verb is listed in neither tier defaults to core (fail-safe…). Per `specs/018-explain/design.html` `D-002`." — `/Users/briancorbin/Code/rags/spectastic/commands.json`
- `specs/` holds 121 spec directories, `000-spectastic` through `120-ownership-aware-verdict-output` — `ls /Users/briancorbin/Code/rags/spectastic/specs/`
- The validator's own file-type regexes are the de-facto machine-readable family: only three are recognised — `/(?:^|\/)specs\/([^/]+)\/spec\.html$/`, `…/tasks\.html$/`, `…/verify\.html$/` — `/Users/briancorbin/Code/rags/spectastic/packages/schema/src/rules/verify-view-stale.ts` (constants `SPEC_FILE`, `TASKS_FILE`, `VERIFY_FILE`) and `…/rules/verify-view-missing.ts`. No rule anywhere recognises `design.html` or `triage-log.html` by path.
- The parsed corpus agrees with the *observed* family rather than the declared one: 627 pages, by filename `proposal.html` 137, `spec.html` 121, `tasks.html` 119, `design.html` 111, `verify.html` 98, `triage-log.html` 40, `roadmap.html` 1 — computed from the `pages` key of `corpus-c64d41c-units-v1-ep4-p250.json`

### Inferences
- There is no single artifact that defines the whole family. `REQ-LIFECYCLE-001` is the closest thing to a family declaration and it is already out of date relative to the estate: it predates `verify.html` (021) and never mentions `triage-log.html`, which `REQ-LIFECYCLE-004` nonetheless subjects to the status machine. A consumer that trusted `REQ-LIFECYCLE-001` alone would miss two of the six artifact types actually present.
- The family partitions into three categories that the meta *does* distinguish normatively: **status-bearing bundle members** (`spec`, `design`, `tasks` — `REQ-LIFECYCLE-005`), **status-bearing standalone artifacts** (`principles`, meta-spec, `triage-log`, `inbox` — `REQ-LIFECYCLE-004`'s author-affirmation clause), and **derived statusless views** (`verify.html` — `021` `FR-007`, `020` `FR-014`). This three-way split is a machine-usable type taxonomy and it is declared, just never collected in one place.
- `roadmap.html` appears once in the corpus and is not mentioned in any requirement I found; it lives at `docs/roadmap.html`. It is convention, not declaration.

### Gaps
- I found no requirement anywhere that declares `triage-log.html` as a member of the artifact family (as opposed to naming its path incidentally). If one exists it is not in `089`, `090`, `091`, `007` or `088`.
- `roadmap.html` has no declaring requirement I could locate; `docs/roadmap.html` is referenced only as a document.
- I did not read `specs/000-spectastic/spec.html` in full (50 KB). Its requirement families (`REQ-CHANGE` 31, `REQ-LIFECYCLE` 30, `REQ-FORMAT` 16, `REQ-TOOL` 15, `REQ-GIT` 6, `REQ-AUTHOR` 6, `REQ-THEME` 4, `REQ-RENDER` 2, `REQ-DEMO` 1 — counted by `grep -rhoE 'spec-requirement id="REQ-[A-Z]+' --include='*.html' specs`) have mostly been carved into 088/089/090/091, so the carve-outs are the live text; but a residual family-definition clause could remain in 000 that I did not see.

---

## What relation does the meta assert between artifact types? Is it ever a MUST?

### Takeaway
Only **two** inter-artifact relations are asserted as MUSTs with enforcement: (1) **verify → spec + tasks** (`021` `FR-002`/`FR-003`/`FR-008`/`FR-010`), and (2) **sibling status bundling** across spec/design/tasks (`REQ-LIFECYCLE-005`). The derivation relations the question asks about — *design derives from spec*, *tasks derive from design* — are **never** stated as a requirement in any spec. They exist only in command-markdown prose and in spec titles/context prose, and spectastic's own `P-8` principle explicitly says command markdown cannot be a guarantee. Nothing validates that a `design.html` corresponds to its `spec.html` at all.

### Cited Findings

**verify → spec + tasks: declared MUST, enforced.**
- `021` `FR-002`: "`verify.html` **MUST** aggregate the spec's success criteria, per-story acceptance scenarios, and the test-first tasks that close them **by reference only** — as links to the anchors in the sibling `spec.html` / `tasks.html` — and **MUST NOT** copy their prose." — `specs/021-verify-view/spec.html`, `id="FR-002"`
- `021` `FR-003`: "The aggregation **MUST** trace each `SC-NNN` to its acceptance scenario and to the test task(s) that verify it… These traces **MUST** be materialised as static links at generation time, with no cross-file fetching at render time." — same file, `id="FR-003"`
- `021` `FR-008`: "`spectastic validate` **MUST** report a finding when a `verify.html`'s derived links no longer match the current `SC-NNN`, acceptance, or test-task IDs in its sibling bundle, or when a closed `SC-NNN` resolves to no test task." — same file, `id="FR-008"`
- `021` `FR-010`: "A spec bundle whose `<spec-status>` has reached a terminal state (`accepted`/`superseded`/`deprecated`) and which falls under the verify-view convention **MUST** carry a `verify.html`. `spectastic validate` **MUST** report a finding — the `verify-view-missing` rule — when such a bundle lacks one. The convention floor is the lowest spec id in the project that already carries a `verify.html` (forward-only from this spec; specs 001–020 predate it and are exempt)." — same file, `id="FR-010"`
- `021` `NFR-002`: "Regeneration **MUST** be idempotent: running the generator twice against an unchanged bundle … yields a byte-identical `verify.html`, so a CI rebuild-and-diff is a meaningful drift signal." — same file, `id="NFR-002"`

**Sibling status bundling: declared MUST, not enforced by any rule I found.**
- `REQ-LIFECYCLE-005`: the three bundle artifacts "**MUST** share a single bundled lifecycle status. When the `spec.html` status transitions per `REQ-LIFECYCLE-004`, the `design.html` and `tasks.html` status **MUST** transition to the same value in the same gesture." Rationale names the failure it fixes: "a `spec.html` cannot be Accepted while its `design.html` is Draft without inviting drift … prevents the gap surfaced in `T-002` of the meta-spec triage log, where nine sibling artifacts sat in Draft despite their parent specs being shipped." It also bounds itself: "Cross-spec bundling is not implied: each spec slice has its own bundle; one slice's status never tracks another slice's." — `specs/089-lifecycle-contract/spec.html`, `id="REQ-LIFECYCLE-005"`

**Parent → child slicing: declared via element + attribute, enforced both directions.**
- `parent-child-reciprocity` rule docstring: "For every document that declares `<spec-parent specid="X">`, when the parent spec X is also present in the validation set, X **MUST** carry a matching `<spec-out-of-scope> <li defer-to="<this-spec-id>">` recording the carve-out. If the parent doesn't, the relationship is one-sided — the child knows about the parent; the parent doesn't know it's been sliced." — `/Users/briancorbin/Code/rags/spectastic/packages/schema/src/rules/parent-child-reciprocity.ts`
- The same rule file states the scope limit: "Only child → parent direction is enforced. The parent → child direction would over-fire on legitimate non-parent/child defer-tos (a spec deferring scope to a sibling that took over the work, without being its child)." — same file

**proposal → spec: declared MUST, typed, deterministic.**
- `REQ-CHANGE-001`: "Spec evolution **MUST** happen via change-proposal artifacts at `specs/<spec-id>/changes/<YYYY-MM-DD>-<slug>/proposal.html`. Each artifact **MUST** use the `<spec-change>` wrapper and contain at least one `<spec-delta>`." — `specs/088-change-management/spec.html`, `id="REQ-CHANGE-001"`
- `REQ-CHANGE-002`: "Each `<spec-delta>` **MUST** carry an `op` attribute equal to one of `added | modified | removed | renamed`, and a `target` attribute naming what it touches: a requirement ID (`FR-*`, `REQ-*`, `NFR-*`, `SC-*`, `D-*`) for a requirement delta, or a manifest data key/path … for a data delta." — same file, `id="REQ-CHANGE-002"`
- `REQ-CHANGE-008`: "When applying each `<spec-delta>`, the kernel **MUST** branch on whether the delta embeds a `<spec-requirement>`. Present — it is a requirement delta: apply the post-state into `spec.html`." — same file, `id="REQ-CHANGE-008"`

**triage-log → tasks: declared MUST, and declared as deliberately *not* producing an edge.**
- `REQ-TOOL-005`: "The implement verb **MUST** treat an open `<spec-triage>` card in a spec's `triage-log.html` as dispatchable work when it carries both `layer="implementation"` and a passing regeneration result … Closing the card **MUST** be the whole record — **no task is written into `tasks.html`** for dispatched work." — `specs/090-command-surface/spec.html`, `id="REQ-TOOL-005"`

**design → spec and tasks → design: NOT declared in any requirement.** What exists instead:
- Command markdown (prose, advisory): "You are producing the **design doc** for an existing spectastic specification … **Pair every design with exactly one spec.**" and "1. **Locate the spec**. Resolve to `specs/<spec-id>/spec.html`. Read it in full — you need every requirement and success criterion before you design." — `/Users/briancorbin/Code/rags/spectastic/commands/spectastic.design.md`, body §Inputs/§Procedure
- Command markdown: "1. **Locate inputs**: `specs/<spec-id>/spec.html` and `specs/<spec-id>/design.html`. Read both end-to-end before generating tasks. You need every requirement, success criterion, and decision in working memory." — `/Users/briancorbin/Code/rags/spectastic/commands/spectastic.tasks.md`, §Procedure step 1
- Command frontmatter `sibling-boundary` keys encode the ordering: design is "Not spec (the what, not the how); not tasks (the ordered breakdown that comes after the design)"; tasks is "Not design (architecture and decisions, not the task list); not implement (which executes the tasks this produces)" — `commands/spectastic.design.md`, `commands/spectastic.tasks.md` frontmatter. This metadata *is* mandated to exist (`REQ-TOOL-004`, priority `must`, requires `triggers`, `use-when`, `sibling-boundary`) — but as router metadata, not as a derivation declaration.
- Spec prose, not requirement text: `009-core-tasks`' subtitle is "Core tasks verb — derive the breakdown from spec + plan in the kernel", and its context says "The kernel takes `spec.html` + `design.html` paths through `ctx.fs`, parses with `@spectastic/schema`" — `specs/009-core-tasks/spec.html`, header + §1 Context (outside any `<spec-requirement>`)
- **The meta disqualifies its own prose location.** `P-8` · *Guarantees live in the kernel or CI*: "A step whose omission is a defect **MUST NOT** rely on command markdown as its guarantee. A verb loaded as a skill is *advisory*: the model can skip, reorder, or half-satisfy any instruction in the prompt, so command markdown, skill descriptions, and rules files **guide but never enforce**." — `/Users/briancorbin/Code/rags/spectastic/principles.html`, `id="P-8"`
- `REQ-FORMAT-009`'s rationale applies `P-8` to exactly this kind of instruction: "Every authoring command already says to replace them all. That instruction lives in command markdown, which triage `T-009` and `P-8` both exclude as a place a guarantee may live — it is a nudge." — `specs/091-artifact-format/spec.html`, `id="REQ-FORMAT-009"`
- The validator confirms the absence structurally: of 64 rules, none references `design.html` by path and none compares a design to its spec. Rule list: `annotation-typed choreography component-provenance component-shape component-states content-budget contract-declaration-shape contract-name-unique corpus-citation-form criterion-actor criterion-binding criterion-indicative criterion-observable criterion-single criterion-threshold-justified criterion-validates data-delta-shape date-format decision-well-formed delta-op-required delta-target-required empty-document file-too-large flow-resolves format-band-coupling hidden-instruction-pattern id-within-file-unique invest-row-failed matrix-winner-integrity no-broken-defer-to no-duplicate-ids no-executable-content no-missing-defer-to no-placeholder-question no-unreplaced-placeholder no-unresolved-question parent-child-reciprocity release-deprecation render-shape requirement-id-required resource-scope-well-formed rice-well-formed risk-status-required risk-target-required screen-shape slo-target-required slo-well-formed spec-element-nesting spec-id-unique spec-parent-well-formed split-well-formed state-source-required subtraction-register-required superseded-task-closed task-id-required task-title-bold-scope token-set-shape tracking-plan variant-grid-shape variant-same-resolves verify-view-missing verify-view-stale` — `ls /Users/briancorbin/Code/rags/spectastic/packages/schema/src/rules/`
- Only 7 of the 64 rules are cross-file at all: `noDuplicateIdsRule, noBrokenDeferToRule, parentChildReciprocityRule, verifyViewStaleRule, verifyViewMissingRule, formatBandCouplingRule, specIdUniqueRule` — `packages/schema/src/rules/index.ts`, `export const rules` array

### Inferences
- **The design→spec and tasks→design derivations are convention, not declaration.** They are asserted in exactly the one register the project's own `P-8` rules out, and in spec *context* prose rather than in requirement text. Anyone citing a "declared" derivation relation between spec, design and tasks would be over-claiming. The strongest honest statement is: *the meta declares that the three co-exist as one status-bundled slice in one directory, and declares the generating verbs' order; it does not declare a content-derivation relation between them, and nothing checks one.*
- The verify relation is the counter-example that proves the pattern is deliberate rather than accidental: when the project wanted a derivation to be real, it wrote it as four MUSTs (`FR-002`, `FR-003`, `FR-008`, `FR-010`), gave it a generator, made regeneration byte-idempotent, and shipped two validator rules. That is the shape of a declared relation in this codebase; design→spec has none of it.
- The `verify-view-missing` rule shows how this meta handles retrofitted conventions: a *forward-only floor* derived from the data itself ("the convention floor is the lowest spec id in the validated set that already carries a `verify.html`"), so a newly-declared relation never retroactively invalidates the estate. The same device appears in `criterion-validates` ("absence is never a finding") and `REQ-FORMAT-005`/`009` (warn-not-error during migration). A graph builder should expect relation *coverage* to be a function of spec number.

### Gaps
- I did not find any requirement, anywhere, stating that `design.html` must correspond to, cite, or cover its `spec.html`. I searched requirement bodies across all 121 `specs/*/spec.html` for `derive`, `derivation`, `downstream artifact`, `regenerat`, `reads the sibling spec`, `from spec + plan`, `spec.html + design.html`, `design.html as input`, `requires an accepted spec`. Every hit was either context prose, out-of-scope-list prose, or about a different subject. I cannot rule out such a clause existing in `specs/000-spectastic/spec.html`'s unread portions.
- `REQ-LIFECYCLE-005`'s bundling MUST appears to have no validator rule. I found no rule name or cross-file rule comparing sibling `<spec-status>` values. I did not exhaustively read all 64 rule bodies, so a rule could implement it under an unrelated name.

---

## What is the full custom-element vocabulary, and which attributes encode relations?

### Takeaway
The vocabulary is **large and open**: 56 distinct `<spec-*>` elements inside `specs/`, 85 across the whole repo — far more than `index.html`'s advertised "Twelve semantic custom elements". There is no closed-set definition anywhere; `REQ-AUTHOR-001` mandates the *style* ("Tag name is schema") without enumerating the tags, and the validator deliberately treats the tag namespace as open (it pattern-matches `spec-[a-z0-9-]+`). Nine attributes carry relations; `validates`, `cites`, `target`, `op`, `defer-to`, `specid`, `grounding`, `status` and `reqs`.

### Cited Findings

**The governing requirements, with no enumeration.**
- `REQ-AUTHOR-001` (priority `must`): "Components **MUST** be expressed as custom elements (`<spec-requirement>`, `<spec-decision>`, etc.) — never as `<div>` + class names. **Tag name is schema.**" — `specs/091-artifact-format/spec.html`, `id="REQ-AUTHOR-001"`. Note the `etc.` — the set is explicitly open.
- `REQ-AUTHOR-002` (priority `must`): "Every requirement **MUST** carry a stable `id` attribute of the form `REQ-<TOPIC>-<NNN>` and a `priority` attribute of `must | should | may`." — same file, `id="REQ-AUTHOR-002"`
- `REQ-AUTHOR-005` (priority `must`): "A `<spec-questions>` register with no open questions **MUST** contain no `<li>` items. Each `<li>` in the register counts as exactly one open question to every consumer — health extraction, the lifecycle needs-attention signal, validators — so an absence of `<li>` is the single unambiguous representation of zero." — same file, `id="REQ-AUTHOR-005"`
- `REQ-FORMAT-008` (priority `must`): "Every `<spec-*>` element in an artifact's source **MUST** close, matched and in order, and a mismatched or unclosed one **MUST** be reported rather than repaired silently… Scoped to `<spec-*>` rather than to every element, because that is the set a source scan can actually decide." — same file, `id="REQ-FORMAT-008"`
- `P-3` · *IDs are contracts*: "Every requirement and decision has a stable, human-meaningful ID … IDs are anchors, anchors are review comments, anchors are how LLMs target edits. They survive rewrites; they don't get reused after removal." — `principles.html`, `id="P-3"`
- The only *closed* set defined in code is two elements wide: `export const CONFORMANCE_ELEMENTS = ['spec-requirement', 'spec-criterion'] as const;` — "Every element whose `id=` participates in conformance tracing — the verify trace, the drift check, the conformance index, and the size gauge's requirement count." — `/Users/briancorbin/Code/rags/spectastic/packages/schema/src/conformance.ts`

**Element counts.** Counted with `grep -rhoE '<spec-[a-z-]+' --include='*.html'`. Two columns: inside `specs/` (the archive proper), and repo-wide (adds `inbox.html` 206 KB, `docs/`, `templates/`, `examples/`, `packages/**/__fixtures__`).

| element | in `specs/` | repo-wide | | element | in `specs/` | repo-wide |
|---|---|---|---|---|---|---|
| `spec-rule` | 3144 | 3253 | | `spec-arch` | 87 | 99 |
| `spec-task` | 2697 | 2748 | | `spec-contract` | 41 | 90 |
| `spec-requirement` | 1881 | 1973 | | `spec-observed-signals` | 75 | 76 |
| `spec-status` | 1695 | 1964 | | `spec-observed-endpoint` | 75 | 76 |
| `spec-pill` | 1347 | 1381 | | `spec-observed-block` | 75 | 76 |
| `spec-meta` | 626 | 767 | | `spec-criterion` | 55 | 75 |
| `spec-note` | 613 | 6191 | | `spec-warning` | 19 | 63 |
| `spec-decision` | 600 | 767 | | `spec-diff` | 46 | 53 |
| `spec-changelog` | 549 | 613 | | `spec-cstate` | 0 | 52 |
| `spec-tldr` | 521 | 593 | | `spec-triage-log` | 40 | 44 |
| `spec-assumption` | 413 | 427 | | `spec-state` | 0 | 40 |
| `spec-risk` | 361 | 385 | | `spec-screen` | 0 | 27 |
| `spec-questions` | 353 | 382 | | `spec-parent` | 22 | 27 |
| `spec-tab` | 290 | 299 | | `spec-context` | 0 | 27 |
| `spec-delta` | 277 | 306 | | `spec-visual` | 1 | 26 |
| `spec-out-of-scope` | 200 | 272 | | `spec-annotation` | 0 | 24 |
| `spec-triage` | 159 | 246 | | `spec-component` | 0 | 23 |
| `spec-change` | 135 | 151 | | `spec-newthought` | 22 | 144 |
| `spec-risk-log` | 132 | 143 | | `spec-sidenote` | 20 | 141 |
| `spec-goals` | 128 | 141 | | `spec-reason` | 4 | 22 |
| `spec-non-goals` | 118 | 139 | | `spec-step` | 0 | 21 |
| `spec-budget` | 121 | 126 | | `spec-subtraction` | 15 | 17 |
| `spec-conformance` | 121 | 124 | | `spec-cue` | 0 | 17 |
| `spec-audience-map` | 114 | 148 | | `spec-scope` | 2 | 16 |
| `spec-matrix` | 102 | 125 | | `spec-slo` | 0 | 14 |
| `spec-tabs` | 99 | 102 | | `spec-copy-budget` | 0 | 14 |
| `spec-toggle` | 98 | 100 | | `spec-path` | 3 | 13 |
| `spec-tests` | 98 | 100 | | `spec-axis` | 0 | 12 |
| `spec-runblock` | 98 | 100 | | `spec-enforcement` | 2 | 11 |
| `spec-run` | 98 | 100 | | `spec-flow` | 0 | 10 |
| `spec-exercise` | 98 | 99 | | `spec-field` | 0 | 10 |
| `spec-demo` | 98 | 100 | | | | |

Long tail, repo-wide only (count ≤ 9): `spec-same` 9, `spec-refusal` 9, `spec-reduced-motion` 9, `spec-event` 9, `spec-choreography` 9, `spec-transition` 8, `spec-release` 8, `spec-render` 7, `spec-baseline` 7, `spec-variant-grid` 6, `spec-token-set` 6, `spec-contract-view` 6, `spec-branch` 6, `spec-principles-apply` 5, `spec-consent-gate` 5, `spec-rice` 4, `spec-resource` 4, `spec-message-shape` 4, `spec-id` 4, `spec-visual-view` 3, `spec-glossary` 3, `spec-tip` 2, `spec-split` 2, `spec-question` 2, `spec-none` 2, `spec-tradeoff` 1, `spec-module` 1, `spec-badge` 1. Totals: **56 distinct elements inside `specs/`, 85 repo-wide**. Note `spec-questions` (353, the register) vs `spec-question` (2, singular) — the singular is almost unused despite `P-8` citing "an unresolved `<spec-question>`" as the residue a rule must reject.

**Relation-bearing attributes, scoped to their owning element.** Counted repo-wide over `specs/**/*.html`; `attrs` = number of elements carrying it, `values` = number of comma/space-separated items inside (i.e. the edge count).

| element | attribute | attrs | values | relation |
|---|---|---|---|---|
| `li` (inside `spec-out-of-scope`) | `defer-to` | 594 | 594 | scope carve-out → target spec id / `never` / `TBD` |
| `spec-decision` | `grounding` | 469 | 469 | decision → evidence class (`verified`/`spike`/`assumed`) |
| `spec-risk` | `target` | 360 | 483 | risk → delta id / requirement id / `§<n>` anchor |
| `spec-risk` | `status` | 360 | 360 | `identified`/`accepted`/`mitigated`/`rejected` |
| `spec-delta` | `target` | 277 | 277 | delta → requirement id or manifest data key |
| `spec-delta` | `op` | 277 | 277 | `added`/`modified`/`removed`/`renamed` |
| `spec-triage` | `status` + `data-status` | 140 | 140 | card lifecycle |
| `spec-change` | `status` | 134 | 134 | proposal lifecycle (`applied`, `withdrawn`, …) |
| `spec-budget` | `reqs` | 121 | 121 | declared requirement budget |
| `spec-tests` | `cites` | 85 | **581** | verify test block → test task ids |
| `spec-demo` | `cites` | 85 | **287** | verify demo path → `SC-NNN` ids |
| `spec-out-of-scope` | `defer-to` | 68 | 68 | block-level carve-out |
| `spec-criterion` | `validates` | 55 | **122** | criterion → requirement ids |
| `spec-parent` | `specid` | 22 | 22 | child spec → parent spec |
| `spec-task` | `status` + `data-status` | 17 | 17 | closure *manner* (e.g. retired/superseded) |
| `spec-matrix` | `target` | 10 | 10 | decision matrix → subject |

Raw attribute frequency inside `specs/` (single-line tag match, so a lower bound): `id=` 5499, `priority=` 1908, `value=` 1695, `level=` 656, `target=` 608, `status=` 498, `grounding=` 469, `label=` 290, `by=` 242, `op=` 238, `cites=` 170, `data-status=` 160, `layer=` 159, `words=` 121, `reqs=` 121, `minutes=` 121, `defer-to=` 69, `shape=` 40, `validates=` 27, `method=` 27, `actor=` 27, `specid=` 22.

**The declaring text for each relation attribute.**
- `validates`: "`<spec-criterion>` must declare `validates=`, and every requirement must be validated by some criterion." Both directions in one rule, per `108-success-criteria` `FR-007`/`SC-002`/`T-114`. "Resolved entirely within the document (`design.html` `D-002`): `validates=` targets are requirement ids in the same `spec.html`, so this needs no filesystem access and no cross-file scan." — `packages/schema/src/rules/criterion-validates.ts`. Note the implication: `validates` is an *intra-file* edge by design, SC→FR inside one `spec.html`.
- `cites`: `021` `FR-004` (priority `must`): "`verify.html` **MUST** carry a Run/Demo block built from typed elements: a run command, a **test command citing the test task(s) it runs**, and a **demo path citing the `SC-NNN` it satisfies**." — `specs/021-verify-view/spec.html`, `id="FR-004"`
- `target` + `op`: `REQ-CHANGE-002`, quoted above.
- `target` + `status` + `by` on `spec-risk`: `REQ-CHANGE-004` (priority `should`): "Each `<spec-risk>` **MUST** carry: `target=` — a delta ID, requirement ID, or `§<n>` section anchor. **Missing renders the visible label `MISSING TARGET`.** `status=` — one of `identified | accepted | mitigated | rejected` (or `no-value-found` …). Default `identified`. `by=` — when status is `accepted | mitigated | rejected`, the …" — `specs/088-change-management/spec.html`, `id="REQ-CHANGE-004"`
- `defer-to`: "`<spec-out-of-scope defer-to="…">` values must be `never`, `TBD` / `TBD-<topic>`, or an existing sibling spec ID." Semantics: "Lexical malformed → fire unconditionally. Spec-ID-format value not present in the validation set → fire ONLY when the set has 2+ documents … Sentinels (`never`, `TBD`, `TBD-*`) → never fire." — `packages/schema/src/rules/no-broken-defer-to.ts`
- `specid`: "`<spec-parent>` must declare a non-empty `specid=` attribute matching the spec-id format" — `^[0-9]+-[a-z][a-z0-9-]*$` — `packages/schema/src/rules/spec-parent-well-formed.ts`. The authoring convention: "The new spec is *still a regular `spec.html`* — the only mark of slicehood is the `<spec-parent specid="…">` reference in the header." — `commands/spectastic.spec.md`, §Procedure step 2
- `grounding`: `REQ-LIFECYCLE-006` (priority `must` on the gate, `should` on the discipline): "Each `<spec-decision>` **SHOULD** declare its `grounding` and cite a verified or spike-resolved source in its Context … The gate is the binding clause: the plan **MUST NOT** be declared ready for `/spectastic.tasks` while a `must`-tier decision rests on an `assumed` or unresolved spike fact, unless the author has accepted it as a tracked `<spec-risk>`." — `specs/089-lifecycle-contract/spec.html`, `id="REQ-LIFECYCLE-006"`
- `status`/`data-status` on `spec-task`: `REQ-LIFECYCLE-003`: "The box **MUST** be the sole authority on whether a task is closed and the mark the sole authority on *how*: a mark **MUST NOT** restate closure, and the box **MUST NOT** encode the manner of it. CSS **MUST** read closure via `:has(input:checked)`, and **MAY** read the mark for what the box does not carry." — same file, `id="REQ-LIFECYCLE-003"`
- `layer` on `spec-triage`: `REQ-TOOL-005`, quoted above, makes `layer="implementation"` + a passing regeneration result the dispatch predicate. (168 `layer` attrs, 170 values, in `specs/`.)

### Inferences
- `index.html`'s "Twelve semantic custom elements" is marketing copy that the estate outgrew by roughly 7×. Any consumer that hard-codes a vocabulary will be wrong; the validator's `TAG_RE = /<(\/?)(spec-[a-z0-9-]+)\b([^>]*)>/gi` is the right model — treat `spec-*` as an open namespace and switch on the ones you know.
- `spec-rule` (3144 in `specs/`) is the densest element and is **not** id-bearing: it is the inline wrapper around each normative keyword (`MUST`, `SHOULD`, `MAY`), carrying an optional `level=` (656 occurrences). It is a within-requirement modality marker, not a relation. A reader looking for "requirement strength" should read `priority=` on `spec-requirement` and `level=` on `spec-rule`, and the two can disagree in a documented way (`REQ-FORMAT-005`: "The requirement is a genuine MUST; the rule's warn severity is a migration-scoped choice").
- Two attribute pairs are *type* declarations rather than edges and are worth extracting as node properties: `spec-decision grounding` (469, three-valued evidence class) and `spec-risk status` (360, four-valued disposition with `by=` attribution at 242). Neither is in the reader's current extraction set.

### Gaps
- My attribute-frequency table over `specs/` uses a single-line tag regex, so an attribute written on a continuation line is undercounted. The element-scoped table (second table) used the same `<tag ...>` match and has the same limitation. Treat all attribute counts as lower bounds; the element-scoped values for `cites` (868 total) and `validates` (122) match the reader's independently-derived 866 and 122 closely enough to suggest the undercount is small.
- `spec-rule` has a `level=` attribute whose full value domain I did not enumerate (I saw `should` and `may`; absence appears to mean MUST).
- I did not locate a declaring requirement for `shape=` (40), `method=` (27), `actor=` (27), `words=`/`minutes=` on `spec-budget`, or `label=` (290).

---

## Which relations are attribute-declared, which structurally implied, which prose only?

### Takeaway
Three tiers, cleanly separable. **Attribute-declared (machine-readable today):** 9 relations, ~2,800 edges. **Structurally implied (machine-derivable but requiring a convention the meta states only in prose):** directory co-membership, the `nnn-slug` prefix, the terminal-status→verify floor, the test-task-by-preceding-heading signal. **Prose only:** the design→spec and tasks→design derivations, and — numerically the largest class — the ~1,675 bare `P-N` principle citations and ~1,200 bare sibling-id mentions that sit in text with no hyperlink and no attribute.

### Cited Findings

**Tier 1 — attribute-declared.** The nine relations in the table above. Each has a named attribute, a declaring requirement, and (for 6 of 9) a validator rule.

**Tier 2 — structurally implied.** Each of these is a real, decidable structure that the meta describes in prose rather than marking up:
- *Directory co-membership is the bundle.* `REQ-LIFECYCLE-005` identifies the bundle by path — "`spec.html`, `design.html`, and `tasks.html` **under `specs/<spec-id>/`**" — not by any cross-reference inside the files. — `specs/089-lifecycle-contract/spec.html`, `id="REQ-LIFECYCLE-005"`
- *The `nnn-slug` prefix is the spec identity.* The format is pinned in code as `^[0-9]+-[a-z][a-z0-9-]*$` in both `spec-parent-well-formed.ts` and `no-broken-defer-to.ts`, and the numeric prefix is used as an *ordering* in `verify-view-missing.ts`: `specNum()` parses the leading digits so the rule can compute a forward-only convention floor. — `packages/schema/src/rules/{spec-parent-well-formed,no-broken-defer-to,verify-view-missing}.ts`
- *The directory name, not a file, is the id-uniqueness scope.* "The three bundle files in one dir (`spec.html` + `design.html` + `tasks.html` under `025-foo/`) share the dir id `025-foo` — that is the bundle, not a collision. The rule groups by **distinct directory name**, then by number." — `specs/025-id-uniqueness/spec.html`, §Edge cases
- *A test task is identified by its preceding heading, not by any attribute.* `021` `FR-003`: "The primary signal for a test task is a `<spec-task>` whose **nearest preceding heading** is the story's `<h3>Tests (write & fail first)</h3>` subsection, identified by that structural position and independent of its file path … When a story phase declares no Tests subsection, the aggregation **MUST** fall back to identifying test tasks by file path — a `<spec-task>` whose declared path matches a test-file pattern (`.test.` / `.spec.`)." The validator mirrors it: `const TESTS_HEADING = /^tests\b/i;` and `const TEST_PATH = /\.(test|spec)\./;` — `specs/021-verify-view/spec.html` `id="FR-003"` and `packages/schema/src/rules/verify-view-stale.ts`
- *Archive depth is encoded in link prefixes, not metadata.* "A proposal lives at `specs/<id>/changes/<date>-<slug>/proposal.html` — 4 directory levels deep … The live spec lives at `specs/<id>/spec.html` — 2 levels deep … so re-basing is simply 'strip the first two `../` segments'." — `/Users/briancorbin/Code/rags/spectastic/packages/core/src/archive-paths.ts`, `shallowProposalPaths`

**Tier 3 — prose only, and large.** Counts over `specs/**/{spec,design,tasks,verify,triage-log}.html`, masking `<a>…</a>` elements and all tag/attribute text, then counting remaining `(FR|SC|NFR|D|T|P)-\d+` tokens in visible prose:

| artifact | `href="…#ID"` links | bare `FR` | bare `SC` | bare `NFR` | bare `D` | bare `T` | bare `P` |
|---|---|---|---|---|---|---|---|
| `spec.html` | 779 | 423 | 60 | 52 | 89 | 92 | 132 |
| `design.html` | 1421 | 494 | 162 | 144 | 1425 | 58 | 1224 |
| `tasks.html` | 2432 | 268 | 166 | 69 | 240 | 1308 | 39 |
| `verify.html` | 1130 | 3 | 4 | 99 | 1 | 3 | 2 |
| `triage-log.html` | 449 | 285 | 31 | 23 | 26 | 333 | 74 |

- Principle citations are overwhelmingly unlinked: across `specs/**/*.html` there are **202** `href="…principles.html#P-N"` hyperlinks against **1,675** bare `P-N` text mentions — a 1:8 ratio. (Computed by the same masking method.)
- And the corpus does not contain `principles.html` at all. The 627 corpus pages are `proposal.html`, `spec.html`, `tasks.html`, `design.html`, `verify.html`, `triage-log.html`, `roadmap.html` only; the only `principles` matches are specs *about* principles (`008-core-principles`). Edges whose target path contains "principles" total 152 and all point at `008-core-principles`'s own bundle, not at the root `principles.html`. — `corpus-c64d41c-units-v1-ep4-p250.json`, `pages` and `edges` keys
- The ids a design/tasks artifact *declares* are disjoint from the ids its spec declares, so "same anchor id in two artifacts" is **not** the implicit edge mechanism. Declared `id="[A-Z]+-\d+"` by artifact: `spec.html` → FR 994, SC 437, NFR 229, D 4; `design.html` → D 596, R 14, SC 1; `tasks.html` → T 2688; `triage-log.html` → T 156, TRIAGE 12. Only **2** ids are declared in both a `spec.html` and its sibling `design.html` across the entire archive.

### Inferences
- **The implicit cross-artifact edge is the unlinked prose mention, not a shared id.** The research brief's hypothesis ("same anchor id appearing in two artifacts") is empirically false — each artifact type owns a disjoint id namespace (`FR`/`SC`/`NFR` in spec, `D`/`R` in design, `T` in tasks). The edge is realised either as a hyperlink (captured already) or as a bare in-prose token (not captured). The recoverable gain is concentrated in: `design.html`'s 494 bare FR + 162 bare SC mentions, `tasks.html`'s 268 bare FR + 166 bare SC, and `triage-log.html`'s 285 bare FR.
- **The single biggest missing edge class is artifact → principle.** 1,675 bare `P-N` mentions, 1:8 linked-to-bare, and the target document is absent from the corpus entirely. Every design decision in the estate is justified against principles that the graph cannot reach. Resolving `P-N` is trivial (14 ids, `P-1`…`P-14`, all in one file at `principles.html`) and would add a dense, high-value hub node.
- Bare-token extraction is low-risk *within* an artifact's own spec dir because the id namespaces are disjoint by type: an `FR-003` token in `specs/042/design.html` can only mean `specs/042/spec.html#FR-003`. The ambiguity is cross-spec (an unqualified `FR-003` in prose that means another spec's FR-003), and that case is usually hyperlinked precisely because it needs the path. So resolve bare tokens to the same spec dir by default and accept a small error rate.
- `verify.html` is the one artifact with essentially *no* prose-only edges (3 bare FR, 4 bare SC against 1,130 links) — exactly what you would expect from a generated artifact whose generating requirement says "by reference only … **MUST NOT** copy their prose" (`021` `FR-002`). Generated artifacts are fully machine-legible; authored ones are not.

### Gaps
- The bare-token counts include self-references (e.g. a `T-` token inside `tasks.html` naming a sibling task in the same file), which is why `tasks.html` shows 1,308 bare `T`. I did not separate self from cross-artifact for the same-type prefix. The cross-type numbers (FR/SC/NFR in design and tasks; FR in triage-log) are clean.
- I did not verify how many bare tokens are inside `<details>` rationale blocks versus requirement bodies, which would matter for weighting.

---

## Is there a validator or schema artifact that enforces the family, and what does it check?

### Takeaway
Yes, and it is explicitly *not* a schema document. The rules directory **is** the schema by design decision: "the `rules/` directory IS the schema — no parallel RelaxNG or JSON Schema." 64 rules, 7 of them cross-file. There is no DTD, no XSD, no RelaxNG, and no JSON Schema for the artifact format. (`packages/schema/src/config/schema-gen.ts` and `scripts/gen-schema.mjs` exist but generate a schema for `spectastic.json` *configuration*, not for artifacts.)

### Cited Findings
- The governing design decision: "The canonical spectastic rule registry. Per `D-001` of the plan, the `rules/` directory IS the schema — no parallel RelaxNG or JSON Schema. Adding a rule is one new file under `rules/` plus one import and one array entry here. Per-file rules see exactly one `ParsedDocument`; cross-file rules see all docs the engine was given. The engine runs both kinds during `validate()` (treating a single doc as a one-element set for cross-file rules)." — `/Users/briancorbin/Code/rags/spectastic/packages/schema/src/rules/index.ts`, header comment
- 64 rules total; the 7 cross-file ones are `noDuplicateIdsRule`, `noBrokenDeferToRule`, `parentChildReciprocityRule`, `verifyViewStaleRule`, `verifyViewMissingRule`, `formatBandCouplingRule`, `specIdUniqueRule` — same file, `export const rules` array
- `verify-view-stale` is the one rule that actually reconstructs an inter-artifact derivation and diffs it: "Live ID-set comparison, no stored fingerprint: the rule re-derives the expected SC + closing-test-task id sets from the bundle and compares them to the ids `verify.html` actually links. Cross-file by nature — it only runs when the verify view and its siblings are in the validated set together." Its href patterns are the machine-readable contract: `SC_HREF = /spec\.html#(SC-\d+)\b/`, `TASK_HREF = /tasks\.html#(T-\d+)\b/`, `NFR_HREF = /spec\.html#(NFR-\d+)\b/`. — `packages/schema/src/rules/verify-view-stale.ts`
- `verify-view-missing` is the presence sibling and derives its own applicability floor from the data: "Forward-only, no hard-coded boundary: the convention floor is the lowest spec id in the validated set that already carries a `verify.html`. Specs below the floor predate the convention and are exempt (001–020 today)." Trigger set: `const TERMINAL = new Set(['accepted', 'superseded', 'deprecated']);` — `packages/schema/src/rules/verify-view-missing.ts`
- `spec-element-nesting` is the only rule that reads raw source rather than the parsed tree, and the comment explains why the family needs it: "parse5 repairs a mismatched end tag on the way in … so by the time any other rule sees a document, the defect is gone. A design authored with `</spike>` closing a `<spec-status>` validated clean for months." And: "`037-hands-off-pipeline`'s design lost 251 characters of a documented command exactly that way, invisibly, because this rule declined to look." — `packages/schema/src/rules/spec-element-nesting.ts`
- Cross-file rules degrade safely on partial input rather than guessing: `no-broken-defer-to` — "Spec-ID-format value not present in the validation set → fire ONLY when the set has 2+ documents. Single-doc validation has no signal for whether the target sibling exists"; `parent-child-reciprocity` — "Reciprocity is only checked when both parent and child are in the validation set. Single-doc validation skips the check. Wide-glob project validation engages it." — `packages/schema/src/rules/{no-broken-defer-to,parent-child-reciprocity}.ts`
- The project pins its own validator thresholds in `spectastic.json`: `validate.quantifiedNfrFloor: 69`, `verify.executeCapturedCommands: true`, `git.auto: "commit"`, `git.trailers: "on"`, plus one `enforce.waivers[]` entry for `observability` with `reason`/`until: 2027-01-31`/`owner`, citing `068-enterprise-enforce-floor FR-005`. — `/Users/briancorbin/Code/rags/spectastic/spectastic.json`
- `REQ-FORMAT-006` records a deliberate *ceiling* on what the validator will ever check, which is unusual and worth noting: "The remaining tool-talking surfaces (runtime finding messages, tool-managed markers) stay **review-caught, permanently and by design**: a rule scanning `finding message:` copy would false-positive on a finding legitimately naming the requirement it enforces, so that surface is judged by review, not linted. This ceiling **MUST** be recorded, not hidden — the honest `P-8` posture — and is not a deferred check awaiting a future rule." — `specs/091-artifact-format/spec.html`, `id="REQ-FORMAT-006"`

### Inferences
- The absence of a declarative schema is a decision, not an omission, and it has a consequence for anyone building a graph from the archive: there is no document you can read to learn the element set or the relation set. You must either read 64 rule files or measure the corpus. The measurements in this note are the latter.
- The validator enforces **presence**, **shape**, **intra-file consistency**, and **id uniqueness** well, and enforces exactly one inter-artifact derivation (verify). It does not enforce the status bundle (`REQ-LIFECYCLE-005`), does not enforce any design↔spec correspondence, and does not enforce that tasks cover requirements — although `commands/spectastic.tasks.md` instructs the model to "check every FR / SC is referenced by at least one task" (per `009-core-tasks` §1 Context). By `P-8`'s own test, that coverage check is a nudge.

### Gaps
- `formatBandCouplingRule` is cross-file and I did not read it; I infer from the name it relates a `spec-budget` band to format, but I cannot say what pair of files it couples.
- I did not run the validator (constraint: read-only, do not run the prototype), so I report what the rules claim to check, not observed findings.

---

## Does an anchor id recur across artifacts of one spec, and how often?

### Takeaway
As a *declaration*, essentially never — 2 cases in the whole archive. As a *hyperlink*, constantly: 3,276 resolved intra-spec cross-artifact anchor references, in a strongly directional pattern that reconstructs the lifecycle DAG from the data alone. As a *bare prose token*, roughly 1,200 cross-type mentions that are currently invisible to the reader.

### Cited Findings
Computed over `specs/*/{spec,design,tasks,verify,triage-log}.html` by collecting declared `id="[A-Z]+-\d+"` per artifact, then resolving every `href="<path>#<ID>"` against the declaring artifact in the same spec directory.

- **Re-declaration:** ids declared in a `spec.html` and *also declared* as an `id=` in a sibling artifact of the same spec: `design.html` **2**, `tasks.html` **0**, `verify.html` **0**, `triage-log.html` **0**. The id namespaces are disjoint by artifact type (see the Tier-3 table above).
- **Resolved intra-spec cross-artifact anchor references, by direction:**

| edge | count |
|---|---|
| `tasks.html` → `spec.html` | 1413 |
| `design.html` → `spec.html` | 596 |
| `verify.html` → `spec.html` | 501 |
| `verify.html` → `tasks.html` | 393 |
| `tasks.html` → `design.html` | 187 |
| `triage-log.html` → `spec.html` | 121 |
| `spec.html` → `triage-log.html` | 28 |
| `tasks.html` → `triage-log.html` | 26 |
| `triage-log.html` → `design.html` | 6 |
| `design.html` → `triage-log.html` | 5 |
| `spec.html` → `design.html` | 2 |
| `triage-log.html` → `tasks.html` | 2 |
| **total** | **3280** |

- **Resolved same-file anchor references:** `spec.html` 457, `design.html` 221, `triage-log.html` 65, `tasks.html` 31 (total 774). My scope was narrower than the reader's `href:same-page` 2170 — I restricted to five artifact types (no `proposal.html`) and to `[A-Z]+-\d+` anchors only.
- **Unresolved:** 157 references using `../` (cross-spec or to a root file such as `principles.html`), plus 9 same-spec references to an anchor the target artifact does not declare — i.e. 9 genuine broken intra-spec anchors (4 `spec.html`→`triage-log.html`, 2 `spec.html`→`spec.html`, 1 each `tasks`→`spec`, `design`→`spec`, `triage-log`→`spec`).
- **The reader's current typed extraction, for comparison** (from the `edges` key of `corpus-c64d41c-units-v1-ep4-p250.json`, 12,283 edges total): `href:same-spec` 6754, `href:same-page` 2170, `href:cross-spec` 1782, `typed:cites` 866, `typed:target` 255, `typed:delta` 168, `typed:defer-to` 166, `typed:validates` 122. Endpoint kinds: `typed:cites` is entirely `u→u` (866), `typed:defer-to` entirely `u→p` (166), `typed:validates` entirely `u→u` (122).
- **Declared-vs-extracted for the typed attributes.** `validates` 122 values declared / 122 extracted — complete. `cites` 868 declared (581 `spec-tests` + 287 `spec-demo`) / 866 extracted — essentially complete. `target` 770 declared (483 `spec-risk` + 277 `spec-delta` + 10 `spec-matrix`) / 255 `typed:target` + 168 `typed:delta` = 423 extracted — **~45% loss**. `defer-to` 674 declared (594 `li` + 68 `spec-out-of-scope` + 12 `span`) / 166 extracted — **~75% loss**.

### Inferences
- **The href direction table *is* the artifact-type relation graph, recovered from data.** `tasks→spec` 1413 and `design→spec` 596 dominate, and the reverse directions are near-zero (`spec→design` 2). That is a derivation DAG: downstream artifacts cite upstream ones; upstream artifacts do not cite downstream. The meta does not declare this relation — but the archive exhibits it at a 300:1 directional ratio, which is strong enough to *infer* the relation the meta failed to state. A typed edge `derives-from` built from `(artifact-type-of-source, artifact-type-of-target)` is defensible, provided it is labelled as inferred from link direction rather than as declared.
- `verify→spec` 501 + `verify→tasks` 393 matches the `021` `FR-002`/`FR-003` contract almost exactly in shape (every SC traced to an acceptance scenario in spec and a test task in tasks), which is a good consistency check on the measurement.
- The `target` and `defer-to` losses are worth chasing: together ~830 declared edges are not reaching the graph. For `defer-to` the likely cause is that the reader only resolves `<spec-out-of-scope>`-level attributes or only those in live `specs/*/spec.html` (ignoring `changes/archive/**`), and the value is often a bare spec id (`042-profile-enforcement`) rather than a path, so it needs a spec-id→page lookup rather than href resolution. For `target` the likely cause is that `spec-risk target=` values are often `§<n>` section anchors or delta ids that are not units in the corpus — `REQ-CHANGE-004` explicitly permits "a delta ID, requirement ID, or `§<n>` section anchor", and only one of those three resolves to a unit. I state these as likely causes; I did not read the reader's extraction code.
- Four relation attributes the meta declares are **not extracted at all**: `spec-decision grounding` (469), `spec-parent specid` (22), `spec-risk status`/`by` (360/242), `spec-triage layer` (~168). The `specid` one is the most valuable per-edge: 22 parent↔child slice edges, each with a validator-enforced reciprocal `defer-to`, that define the only *explicit* spec-to-spec structural hierarchy in the archive.

### Gaps
- My href resolution only matched `href="<path>#<ID>"` with `ID` of form `[A-Z]+-\d+`; section anchors (`#requirements`, `§3`) and lowercase anchors were excluded, so my totals are not comparable to the reader's `href:*` counts and I do not present them as such.
- I did not inspect `changes/**/proposal.html` in the anchor analysis, which the corpus shows is the single largest page type (137 pages). Proposal→spec edges are therefore absent from my direction table, although `REQ-CHANGE-002`'s `target=` is the declared mechanism there and is partly captured as `typed:delta` 168.
- I could not confirm why `typed:defer-to` is 166 rather than ~674 without reading the reader's extraction code, which was out of scope.
