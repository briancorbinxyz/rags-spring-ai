# Hop structure of the 105-question golden universe

Sources throughout are local, read-only:

- `/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/README.md`
- `/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions.jsonl` (53)
- `/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions-heldout.jsonl` (13)
- `/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions-time.jsonl` (24)
- `/Users/briancorbin/Code/rags/rags-spring-ai/src/main/resources/golden/questions-routing.jsonl` (15)
- the corpus at `/Users/briancorbin/Code/spectastic`, read only via
  `git show c64d41c2e5d0d42c4fefd1b506cb083b9812bf6c:<path>` (the commit
  `spectastic.commit` pins in `src/main/resources/application.properties`)

No relevance judgment was made on any retrieved passage; nothing in the golden set was
edited. Branch `episode-5` was not changed.

### Classification rule actually applied

For each `expected` address B other than the first:

- **Single-passage** — one `expected` address in total.
- **Multi-evidence (parallel)** — B's identity *or* B's content is given by the question's
  own words closely enough that a recall-good retriever surfaces it in the same shot. No
  reading of A is required.
- **Multi-hop (chained)** — the question's words give neither B's identity (spec
  number/name/element id) nor a description of B's content specific enough to retrieve it;
  you must read A first to learn where B is.
- **Bridge (2+ hops)** — resolving B needs more than one intermediate read: A → B → C.

Where a question has one `expected` address but the reference `answer` plainly required a
second document to compose, it is flagged **latent multi-hop, scored single**.

---

## How many of the 105 are multi-hop (chained), and which ids?

### Takeaway

Eleven of the 105 are genuinely chained: **G-030, G-031, G-032, G-033, G-036** in the main
set and **H-001, H-003, H-004, H-006, H-007, H-008** in the held-out set. Every one of them
sits in the `relation` stratum, every one is a 1-hop chain, and the chaining is concentrated
in 13 of the 105 questions' worth of `relation` design — no chained question exists anywhere
in the time set or the routing set.

### Cited findings — the 11 chained questions, with the words that fail to identify B

- **G-030** `relation`, expected `specs/110-visual-one-step/spec.html#FR-003` and
  `specs/110-visual-one-step/spec.html#NFR-001`. Question: *"Which requirements does success
  criterion SC-002 of spec 110, visual one-step, validate, and what do they demand?"* The
  question's words identify only `SC-002` and `spec 110`; the strings "FR-003" and "NFR-001"
  appear nowhere in it, and *"what do they demand"* describes no content. Hop 1 is SC-002
  itself, which carries `validates="FR-003,NFR-001"` (verified in the corpus). The golden
  set's own `note` says: "Follows SC-002's validates=\"FR-003,NFR-001\" to the far end.
  SC-002 itself (mistyped export path loses nothing) is the near end and **is not listed as
  expected**." — `src/main/resources/golden/questions.jsonl`
- **G-031** `relation`, expected `specs/051-knowledge-corpus/spec.html#FR-003` and
  `specs/058-corpus-licensing/spec.html#requirements`. Question: *"Spec 058's corpus-license
  rule reads a license field that 058 says it does not own. **Which spec defines that
  field**, and what else goes in that frontmatter?"* The question names 058 (the near end)
  and explicitly *asks for* B's identity. `note`: "Follows defer-to=\"051-knowledge-corpus\"
  on 058's out-of-scope item 'The license provenance field this rule reads.'" Corpus confirms
  `<li defer-to="051-knowledge-corpus">The …` in `specs/058-corpus-licensing/spec.html`.
- **G-032** `relation`, expected `specs/054-corpus-in-prompt/spec.html#FR-001` and
  `specs/055-corpus-in-review/spec.html#requirements`. Question: *"Spec 055 (corpus in
  review) leaves injecting the corpus into the authoring verbs out of scope. **Which spec
  handles that**, and what does it require?"* Same shape as G-031: near end named, far end
  asked for. Corpus confirms `<li defer-to="054-corpus-in-prompt">Injecting the corpus into
  the authoring verbs (…` in `specs/055-corpus-in-review/spec.html`.
- **G-033** `relation`, expected `specs/023-explore-graduation/design.html#D-006` and
  `specs/024-explore-restore/spec.html#FR-001`. Question: *"Spec 023's design dropped its
  decision on generating restore tasks after an exploration graduates. **Which slice took
  that over**, and how is restore invoked now?"* Nothing in the question says 024 or
  "explore-restore". `note`: "D-006 names the placeholder TBD-explore-restore; that it became
  024 is stated in 024's tasks.html ('024 now exists'), not in D-006. Brian should check he
  accepts that hop." Corpus confirms D-006's text is
  `(superseded — moved to TBD-explore-restore)` and that
  `specs/024-explore-restore/tasks.html` carries
  "Point the restore-scaffold at the real 024 — update the explore command doc's
  'restore-scaffold: TBD-explore-restore' pointers to … (024 now exists)".
- **G-036** `relation`, expected `specs/076-contract-export-handover/tasks.html#dependencies`
  and `specs/071-contract-promotion/design.html#D-002`. Question: *"Spec 076 emits a change
  notification when a contract is promoted. **Which spec provides that promotion hook**, and
  where does promotion actually run?"* 076 is named; 071 is asked for. Corpus confirms the
  near end, `section id="dependencies"`, contains the bare line
  `071 US1           → US2            (the promotion hook the notification fires from)` with
  no hyperlink in that section (076's tasks.html does link `../071-contract-promotion/spec.html`
  from a *different* section, around line 100).
- **H-001** `relation`, expected `specs/113-guardrail-retrieval/spec.html#FR-003` and
  `specs/113-guardrail-retrieval/spec.html#NFR-002`. Question: *"What does success criterion
  SC-002 of spec 113, guardrail retrieval, check, and **which requirements does it prove**?"*
  Identical structure to G-030, intra-document. Corpus confirms
  `<spec-criterion id="SC-002" priority="must" actor="a CI job" validates="FR-003,NFR-002"
  method="test">`. SC-002 is again *not* an expected address.
- **H-003** `relation`, expected `specs/053-corpus-grounding-gates/spec.html#FR-001` and
  `#FR-002`. Question: *"Spec 052 defines how corpus citations are written but leaves checking
  them to another spec. **Which one**, and how strict is each check?"* Hop 1 is 052's
  out-of-scope list, which is **not** an expected address. Corpus confirms the spec is
  `052-corpus-citation-contract` and carries
  `<li defer-to="053-corpus-grounding-gates">The validate rules that a cited ID resolves
  (error) and that a pinned edition is current (warning).`
- **H-004** `relation`, expected `specs/106-visual-render/spec.html#requirements` and
  `specs/105-design-source-import/spec.html#FR-013`. Question: *"Why doesn't spec 106's render
  step open a design-tool archive itself, and what does **the requirement it relies on**
  demand?"* "the requirement it relies on" is a pure under-specification. Corpus confirms
  `<li defer-to="105-design-source-import">Opening an archive.` in 106's requirements section.
- **H-006** `relation`, expected `specs/007-core-triage/design.html#D-001` and
  `specs/013-core-propose/design.html#D-001`. Question: *"The first AI provider shipped with
  one of its methods deliberately left unfinished. Why, and **how was it completed later**?"*
  Neither spec is named; A is findable from "first AI provider … left unfinished", B is not.
  Corpus confirms 007 D-001's prose: `throws "not implemented; lands with 013-core-propose."`
  and "Dead code until 013."
- **H-007** `relation`, expected `specs/012-core-plan/design.html#D-006` and
  `specs/011-core-spec/design.html#D-002`. Question: *"**Where did the design command get its
  rules** for revisiting a design that already exists, and what may it do to an existing
  decision?"* "the design command" resolves to 012; 011 is asked for. Corpus confirms D-006's
  Context: "011's re-entry pattern (parse, diff, ADD-or-ENHANCE) applies directly."
- **H-008** `history`, expected `specs/062-corpus-identity-migration/spec.html#FR-009` and
  `specs/063-corpus-discoverability/spec.html#FR-007`. Question: *"Spectastic's own repository
  was once only a consumer of its knowledge corpus. **What reversed that**, and what does it
  publish now?"* A is findable from "once only a consumer"; B is asked for. Corpus confirms
  062 FR-009 contains
  `<a href="../063-corpus-discoverability/spec.html#FR-007">063-corpus-discoverability (FR-007)</a>`.

### Inferences

- Chaining in this universe is a property of exactly one stratum. All 7 main-set `relation`
  questions and all 8 held-out `relation` questions were examined; 5 and 6 of them respectively
  are chained, and no question outside `relation` (plus H-008, a `history` question whose
  structure is a supersession link) chains at all.
- Two chained questions (G-030, H-001) have the striking property that **hop 1 is not a scored
  address**. A retriever can land SC-002 — the only thing the question names — and score
  nothing, because the expected set is the far end only. These are the two questions where
  Hit@k most understates what the system actually found.
- Three chained questions (H-003's hop 1 in 052; G-030's and H-001's SC-002) have an unscored
  hop 1; the other eight have hop 1 in `expected`, so a chain-following retriever is rewarded
  for the intermediate as well as the destination.

### Gaps

- None. Every chained call is backed by the question text, its `expected` list, and the
  pointer verified in the pinned corpus.

---

## How many are merely multi-evidence (parallel)? Which ids?

### Takeaway

Thirteen: **G-012, G-029, G-076** (main), **H-002, H-005, H-009, H-011, H-013** (held-out),
**G-051, G-052, G-055, G-058, G-059** (time). Nine of the thirteen are intra-document (both
or all addresses in one file), so they test recall and chunk coverage, not graph traversal.

### Cited findings

- **G-012** `paraphrase`, `specs/105-design-source-import/spec.html#FR-009` +
  `specs/105-design-source-import/design.html#D-002`. The question describes the behaviour in
  full — *"If someone tweaks a file that came in from a design tool and later brings in a fresh
  version from that tool, are their tweaks kept?"* — and both addresses answer it directly.
  `note`: "The answer is in two places (requirement and the design decision that implements
  it)." Parallel, two documents, one spec.
- **G-029** `relation`, `specs/109-prose-theme/spec.html#SC-003` + `#SC-001`. *"Which success
  criteria in spec 109, the prose theme, **validate FR-008** (the receding sticky header)?"*
  The question supplies the pivot token `FR-008` itself, so both targets are reachable in one
  operation; neither requires reading the other. Corpus confirms
  `SC-001 … validates="FR-002,FR-003,FR-004,FR-005,FR-008,FR-009"` and
  `SC-003 … validates="FR-008"`. This is a **reverse-attribute** structure: the typed link
  lives on B pointing back at a token the question already gave, not on A pointing to B. It is
  parallel in hop terms but lexically hostile — `note`: "The link lives only in an attribute,
  so body-text chunks don't contain 'FR-008' in either criterion."
- **G-076** `single-fact`, three addresses, all in `specs/031-init-tools/spec.html`: `#FR-002`,
  `#FR-007`, `#FR-006`. The question asks two things that map onto them — *"What does running
  spectastic's init with the tools option set up in a project, and what happens if the project
  already has a git hook of its own?"* `note`: "Three expected elements in one spec: all@4
  needs three of the four slots." Parallel; the only 3-address question in the main set.
- **H-002** `relation`, `specs/115-guardrail-verdict/spec.html#FR-004` + `#FR-007`. Two
  independently described sub-questions: *"how does it decide which findings count"* (FR-004)
  and *"what is each violation guaranteed to tell you"* (FR-007). Parallel, intra-document.
- **H-005** `relation`, `specs/118-verdict-explain/spec.html#FR-003` + `#SC-002`. *"How can a
  team be sure that asking the guardrail for an explanation won't change what their CI pipeline
  sees?"* describes the guarantee (FR-003) and implicitly the check (SC-002), and the corpus
  confirms `SC-002 … validates="FR-003"` — so the typed link exists, but here **both ends are
  expected and both are semantically described**, which is what makes it parallel rather than
  chained. The contrast with G-030/H-001, where only the far end is expected, is the cleanest
  natural experiment in the set.
- **H-009** `history`, two addresses in **one** document:
  `specs/003-init-node-port/changes/withdrawn/2026-08-15-the-bundle-is-the-list/proposal.html#intent`
  + `#FR-002`. *"How far off was it"* → `#FR-002`'s delta; *"what did it want instead"* →
  `#intent`. Same-page siblings, both described. Parallel.
- **H-011** `history`, `specs/012-core-plan/spec.html#FR-016` +
  `specs/012-core-plan/changes/archive/2026-08-23-preserve-what-the-generator-does-not-emit/proposal.html#intent`.
  *"**What was lost**, and **what now prevents it**?"* — two explicitly separate asks, one per
  address. Parallel.
- **H-013** `relation`, `specs/008-core-principles/design.html#D-005` +
  `specs/007-core-triage/design.html#D-001`. *"Why couldn't **the principles command** ship
  before **the triage command**, and what did it need from it?"* — the question names **both**
  endpoints' documents, which is why this is parallel and H-006 (which names neither) is
  chained. This is the **weakest parallel call in the set**: the question names triage's
  document but not its element, and D-005's own pointer to the element is the bare prose name
  "007's ClaudeProvider"; the corpus shows D-005's only *hyperlink* is to
  `../007-core-triage/design.html#D-007` — a **different element** from the expected D-001. A
  reader who followed the link would land on the wrong decision. Recorded as parallel with that
  caveat rather than undetermined, because "what did it need from it" describes D-001's subject.
- **G-051** `history`, `…/changes/withdrawn/2026-08-15-what-init-writes/proposal.html#SC-001`
  + `specs/003-init-node-port/spec.html#changelog`. Parallel: the question names spec 003 and
  describes the proposal's content (*"what did that attempt want the success check to compare
  every installed file against"*), and the changelog of the named spec is directly reachable.
  **But** it carries a disambiguation chain — see the time-set section below.
- **G-052** `history`,
  `specs/094-visual-sidecar-convention/changes/withdrawn/2026-08-14-bound-what-it-binds/proposal.html#NFR-001`
  + `specs/094-visual-sidecar-convention/spec.html#changelog`. Parallel for the same reason,
  and here the question's own words do the sibling disambiguation: *"kept the file-system budget
  for resolving declared paths exactly as it was and tried to reword **only the justification**
  attached to it"* separates `bound-what-it-binds` from its same-day sibling `bind-the-checks`.
- **G-055** `history`, four addresses, all in `specs/093-design-visual-section/tasks.html`:
  `#T-1100`, `#T-1101`, `#T-1200`, `#T-1201`. The question names 093 and announces the plurality
  — *"Spec 093 **twice** drew up work … and **both batches** were shelved"* — and each task
  carries its own retirement note ("Retired by 2026-08-14-a-screen-has-a-name"), so *"what
  shelved them"* is answerable in place. Parallel, four same-page siblings; the widest
  all-evidence demand in the universe.
- **G-058** `history`, two addresses in one document:
  `specs/000-spectastic/changes/withdrawn/2026-06-13-implement-to-build/proposal.html#risks` +
  `#changelog`. *"Why didn't that go ahead?"* is split across the risk register (the R1 finding)
  and the proposal's own changelog (the withdrawal). Parallel, same-page siblings.
- **G-059** `history-decoy`, `specs/093-design-visual-section/spec.html#FR-014` + `#FR-015`.
  *"What does it require now?"* — FR-014 is the rule, FR-015 "supplies the means". Parallel, and
  the corpus shows FR-014 carries a genuine same-page sibling anchor, `<a href="#FR-015">FR-015</a>`
  (and FR-015 links back with `<a href="spec.html#FR-014">FR-014</a>`), so the link is available
  even though no hop is required to earn the second address.

### Inferences

- Intra-document parallel: G-076, H-002, H-005, H-009, G-055, G-058, G-059 (7). Cross-document
  but same spec: G-012, H-011 (2). Cross-spec: G-029 (same spec, two criteria), H-013, G-051,
  G-052. The dominant shape is "one spec, several elements", which is a chunking and
  all-evidence@k problem, not a retrieval-graph problem.
- All-evidence@4 is where these bite: G-055 needs four of four slots and G-076 three of four,
  so these two questions alone can dominate an all-evidence rate without any hop being involved.

### Gaps

- H-013 is the one call that could legitimately be argued the other way; it is recorded as
  parallel with the reasoning and the mis-targeted hyperlink made explicit so the report-writer
  can re-decide.

---

## Is there ANY question requiring 2+ hops (a true bridge)?

### Takeaway

**No. Not one of the 105 questions requires a true A → B → C bridge as the golden set scores
it.** All 11 chained questions are exactly one hop. This is a negative result and should be
stated without hedging: the golden set contains no recursive or bridge multi-hop question.

### Cited findings

- All 11 chained questions have exactly two `expected` addresses, with hop 1 producing the
  pointer to hop 2 in a single read. No question in any of the four files has three or more
  `expected` addresses that form a chain: the only questions with 3+ addresses are **G-076** (3,
  all in one spec, parallel) and **G-055** (4, all in one file, parallel).
- The single near-miss is **G-033**. The pointer in `specs/023-explore-graduation/design.html#D-006`
  is the *placeholder* `TBD-explore-restore`, not a spec number, and the mapping to 024 is
  recorded in `specs/024-explore-restore/tasks.html` ("024 now exists"), a document that is
  **not** in `expected`. The golden set flags exactly this: "that it became 024 is stated in
  024's tasks.html … not in D-006. Brian should check he accepts that hop."
- G-033's bridge nevertheless collapses to one hop in practice, because the placeholder slug
  `explore-restore` is lexically identical to the directory name `024-explore-restore`, so the
  second expected address is reachable from hop 1's text by string match alone, with no
  intermediate document read.
- A second, weaker near-miss is the family **G-031, G-032, G-036, H-003, H-004, H-006, H-007**:
  their pointer names only a **spec**, never an element, so landing the expected element needs a
  within-spec content search after the hop. That is a two-stage *retrieval* but not a second
  hop — the second stage consumes the question's own words, not a new identifier.

### Inferences

- The multi-hop ceiling in this corpus is architectural, not accidental. spectastic's typed
  links (`validates`, `defer-to`, dependency rows, supersession notes) are all one-step
  relations between a spec/element and another spec/element. Nothing in the document-type
  grammar composes two relations, so no question built from the markup could have been
  2-hop without inventing a question whose chain crosses two different link types.
- Any claim in the final report that the pipeline "handles multi-hop" can only mean one hop.
  There is no evidence in this question universe either way about bridge retrieval.

### Gaps

- G-033 is the only question where a human could reasonably call it two hops. It is recorded as
  1 hop with a lexical shortcut, and the ambiguity is flagged rather than resolved, since
  resolving it would amount to a judgment on retrievability.

---

## Do the time-set strata create chaining of a different kind?

### Takeaway

The 24 time questions contain **zero** chained questions in address space, but they contain a
different and systematically unscored structure: 10 of the 24 require reading *N versions of one
address*, and their reference answers routinely name a change proposal whose slug is only
obtainable from a `#changelog` the question never names. `#changelog` → `changes/**/proposal.html`
**is** a real pointer (a plain `<a href>`), but the golden set either scores the changelog as an
expected address (G-051, G-052) or does not require the proposal at all (G-062–G-071).

### Cited findings — per stratum

**`history` (7: G-051, G-052, G-054, G-055, G-056, G-057, G-058, plus H-008/H-009/H-010/H-011/H-012 in held-out)**

- `#changelog` → proposal is a **verified plain hyperlink**. In
  `specs/003-init-node-port/spec.html`, the changelog contains
  `<a href="./changes/withdrawn/2026-08-15-the-bundle-is-the-list/proposal.html">2026-08-15-the-bundle-is-the-list</a>`
  and `<a href="./changes/withdrawn/2026-08-15-what-init-writes/proposal.html">2026-08-15-what-init-writes</a>`.
  In `specs/094-visual-sidecar-convention/spec.html` it contains
  `<a href="./changes/withdrawn/2026-08-14-bound-what-it-binds/proposal.html">…</a>` and
  `<a href="./changes/withdrawn/2026-08-14-bind-the-checks/proposal.html">…</a>`. The link is
  document-level (to `proposal.html`), never to an element anchor.
- **G-051 carries a genuine disambiguation chain.** It asks for *"a **second** withdrawn
  attempt"* at spec 003. Both withdrawn proposals are dated **2026-08-15**, both concern what
  init must write, and the *other* one is the target of **H-009**. The ordinal "second" is only
  resolvable from the changelog's ordering of the two links — which is why
  `specs/003-init-node-port/spec.html#changelog` is itself an expected address. So the chain is
  rewarded, and a retriever that answers from `the-bundle-is-the-list` instead of
  `what-init-writes` is answering H-009's question.
- **G-052** is the same sibling pair on spec 094 (`bound-what-it-binds` vs `bind-the-checks`),
  but the question's own words disambiguate, and the sibling is the `stale` record for a *third*
  question, **G-041**, whose `note` records: "A second withdrawn sibling (bound-what-it-binds)
  kept 'path resolution' but reworded the purpose … not listed as stale because its answer to
  this question is the same."
- **G-054, G-056, G-057** are single-passage. G-057's answer, though, ends "D-005 is now marked
  superseded by D-006, which took exactly that escape hatch" — a second document's existence,
  reflected in the question's `stale` list (`…design.html#D-006`) rather than in `expected`.

**`history-decoy` (3: G-059, G-060, G-061)**

- No chaining. The decoy mechanism is that the question *sounds* historical while the answer is
  current, so the work is rejecting a stale candidate, not following a link. In **G-060** and
  **G-062–G-066, G-072–G-074** the same address appears in both `expected` and `stale`
  (e.g. G-060: `expected` and `stale` are both `specs/004-npm-publish-workflow/spec.html#NFR-002`),
  which encodes "right address, wrong version" — a versioning distinction with no hop in it.

**`as-of` (5: G-062, G-063, G-064, G-065, G-066)**

- All single-passage, one address each, resolved at a past commit. No chain: the question supplies
  the cutoff and the subject, e.g. G-063 *"As of release v0.1.0-pre.24, which kinds of spectastic://
  coordinate did spec 078 say its one parser had to accept?"* names the spec outright.
- **Latent, though**: every as-of answer narrates the later replacement by a *named proposal* —
  G-063 "On 14 Aug 2026 (2026-08-14-kinds-from-the-closed-set) the list was replaced", G-065
  "Since the 2026-06-23-sticky-header-in-calm change (recorded 24 Jun)", G-066 "On 26 Jun 2026
  (2026-06-25-spec-budget-bands) the trigger became the amber band of at least 80%". Those slugs
  come from the changelog, not from the single expected address.
- G-063 and G-064 are the tag-versus-date paraphrase pair the README mentions: identical expected
  address (`specs/078-federated-resource-uri/spec.html#FR-005`), one cutoff given as
  `v0.1.0-pre.24`, the other as `12 August 2026`.

**`what-changed` (5: G-067, G-068, G-069, G-070, G-071)**

- This is the clearest "different kind of chaining". Each has **one** expected address and a
  `versions` array of 2–4 commits of that same address. G-067 has four entries (one `before` with
  `"absent": true`, two `intermediate`, one `after`); G-069 has four; G-070 has three; G-068 and
  G-071 have two.
- Every one of these reference answers names the change proposals by slug: G-067 names
  `2026-08-13-address-a-screen`, `2026-08-14-use-the-anchor`, `2026-08-14-screen-as-a-kind`;
  G-068 names `2026-09-14-oidc-trusted-publishing`; G-069's answer ends "The changelog dates all
  three to 11 Jul; git records them on 12 Jul UTC"; G-070 names
  `2026-08-15-init-installs-the-bundle`, `2026-09-12-installable-trees-by-target` and
  `2026-09-12-claude-installs-skills`; G-071 names `2026-08-22-what-a-candidate-is-made-of`.
- The candidate chain the brief asked about therefore **exists and is required by the answers**,
  but it is **not scored**: no `what-changed` question lists a `#changelog` or a
  `changes/**/proposal.html` address in `expected`. The hop is from one address's history to the
  proposals that explain it, and the metric records only the address.

**`as-known` (4: G-072, G-073, G-074, G-075)**

- All single-passage; G-075 is unanswerable (`expected` empty).
- **G-072** is an address *migration*, not a hop: `expected` is
  `specs/004-npm-publish-workflow/**plan.html**#D-005` while `stale` is
  `…/**design.html**#D-005` — the file was renamed between the as-known commit and today. That
  is a bitemporal addressing problem masquerading as a relation.
- **G-073** and **G-074** need only one element at one commit, though both answers depend on a
  *correction date* ("first recorded on 17 Aug 2026 (00:53 UTC)", "first recorded 19 Aug 00:54
  UTC") which is commit metadata, not a second address.
- **G-075**'s answer explicitly describes the trap as a bitemporal one, not a hop: spec 111's
  pages are dated 9–10 Sep but "were first committed on 13 Sep 2026", so "A system using the
  pages' Created dates as valid time would wrongly answer yes for 11 Sep."

### Inferences

- The time set's difficulty is orthogonal to hop count. It asks for the *right version of one
  address*, and the one structure that would make it multi-hop — changelog entry → proposal — is
  scored in only 2 of 24 questions (G-051, G-052) and required-but-unscored in at least 5
  (G-067–G-071).
- If a graph-retrieval episode wants a time-set story, the `#changelog` → proposal edge is the
  edge to build on: it is a real, machine-followable `<a href>`, it is the only edge that
  explains *why* a version changed, and the current metric is blind to whether a system followed
  it.

### Gaps

- Whether a retriever *could* answer G-067–G-071's "why did each give way" from the single
  expected address alone cannot be settled without reading the FR-013/FR-006/FR-002 elements at
  each commit and judging sufficiency — that is a relevance judgment, explicitly out of scope
  here. Recorded as a strong latent-multi-hop *suspicion* backed by the answers' own citation of
  proposal slugs, not as a proven gap.

---

## Do the 15 routing questions involve any chaining?

### Takeaway

No. All 15 are single-passage by construction, and the fact that none has 2+ expected addresses
is deliberate, not incidental: the notes repeatedly record a *second* relevant document and
exclude it on purpose. The routing set tests intent matching and symptom→cause matching inside
one element, and it contributes nothing to any multi-evidence or multi-hop count.

### Cited findings

- Eight `intent-noword` questions each point at exactly one `spec.html#context`: **R-001**
  `000-spectastic`, **R-002** `002-validate-cli`, **R-003** `003-init-node-port`, **R-004**
  `004-npm-publish-workflow`, **R-005** `015-ai-stub-injection`, **R-006** `016-theme-support`,
  **R-007** `021-verify-view`, **R-008** `111-codex-skill-adapters`. Six of the eight notes are
  literally "The spec's own #context."
- Seven `triage-why` questions each point at exactly one triage card: **R-009**
  `002-validate-cli/triage-log.html#T-001`, **R-010** `004-npm-publish-workflow#T-001`, **R-011**
  `010-core-apply#T-001`, **R-012** `019-explain-course#T-001`, **R-013** `021-verify-view#T-001`,
  **R-014** `022-explore#T-001`, **R-015** `016-theme-support#T-001`. The notes locate the answer
  *within* the card: "The root cause is in the card's headline and its Diagnosis" (R-009), "The
  card's headline states the root cause; its Diagnosis names the two workflow steps" (R-010).
- The single-passage design is enforced, not assumed. **R-007**: "The design rationale lives in
  verify-considerations.html, a different document, and is not expected here." **R-005**: "The
  fuller reasoning lives in a project memory outside the corpus, so #context is the only address
  holding it." **R-009**: "The spec's own FR-002 is explicitly not at fault, so spec.html is not
  expected evidence." **R-014**: "FR-006 in the spec is correct as written, so spec.html is not
  expected evidence."
- **R-008**'s note records why the *question* was chosen to avoid ambiguity rather than to add a
  hop: "Chosen as thematically standalone: the contexts of specs 006-014 all say 'today's verb is
  markdown and does not reach MCP', and 112-120 are one guardrail programme, so a question on
  either group would have ambiguous gold."

### Inferences

- "0 multi-expected addresses" in the routing set means the author decided, case by case, that the
  single card or single Context section is *sufficient* — and in four cases explicitly ruled a
  second, topically related document out of the gold. So the zero is a design decision about
  answer sufficiency, not a sign that routing questions are shallow.
- Several `triage-why` answers (R-009, R-011, R-012, R-013, R-014) cite source code with
  `file:line` (e.g. R-013 quotes `TEST_PATH` at `packages/core/src/commands/verify.ts:62` "and
  identically at `packages/schema/src/rules/verify-view-stale.ts:25`"). The second evidence source
  for these questions is the *codebase*, which is outside the corpus and therefore unaddressable —
  a different flavour of "latent" from the document hops.

### Gaps

- None.

---

## What kind of pointer does hop 1 contain?

### Takeaway

Of the 11 chained questions: **(a) typed link in the markup — 6**; **(b) plain hyperlink — 1**;
**(c) bare identifier in prose with no link — 4**; **(d) same-page sibling anchor — 0**;
**(e) no pointer at all — 0**. The sharper finding is orthogonal to that split: only **3 of 11**
pointers name the destination *element*; the other **8 name only a spec**, so eight of the eleven
chains cannot be completed by link-following alone.

### Cited findings

**(a) Typed link in the markup — 6**

| Question | Pointer, as it appears in the corpus | Granularity |
|---|---|---|
| G-030 | `<spec-criterion id="SC-002" … validates="FR-003,NFR-001" method="mechanical">` | element |
| H-001 | `<spec-criterion id="SC-002" … validates="FR-003,NFR-002" method="test">` | element |
| G-031 | `<li defer-to="051-knowledge-corpus">The …` | spec only |
| G-032 | `<li defer-to="054-corpus-in-prompt">Injecting the corpus into the authoring verbs (…` | spec only |
| H-003 | `<li defer-to="053-corpus-grounding-gates">The validate rules that a cited ID resolves (error) and that a pinned edition is current (warning).` | spec only |
| H-004 | `<li defer-to="105-design-source-import">Opening an archive.` | spec only |

**(b) Plain hyperlink — 1**

- **H-008**: `specs/062-corpus-identity-migration/spec.html#FR-009` contains
  `<a href="../063-corpus-discoverability/spec.html#FR-007">063-corpus-discoverability (FR-007)</a>`.
  This is the **only** pointer in the entire chained set that is both a real hyperlink and
  element-level — it resolves straight to the expected address. The edge is also bidirectional:
  063 FR-007 reads "Discoverable-by-default SHOULD supersede 062 FR-009's consumer-only default".

**(c) Bare identifier in prose, no link — 4**

- **G-036**: `specs/076-contract-export-handover/tasks.html#dependencies` holds the plain-text row
  `071 US1           → US2            (the promotion hook the notification fires from)`. Names a
  spec and two user stories; the expected element is `071 design.html#D-002`, which the row never
  mentions. 076's tasks.html does carry `<a href="../071-contract-promotion/spec.html">071` — but
  in a different section, so the *expected* near-end element contains only the bare id.
- **G-033**: `specs/023-explore-graduation/design.html#D-006` holds
  `(superseded — moved to TBD-explore-restore)`. The worst case in the set: the identifier is a
  **placeholder**, not a spec number, and it is not a link.
- **H-006**: `specs/007-core-triage/design.html#D-001` holds
  `throws "not implemented; lands with 013-core-propose."` plus "Dead code until 013." A bare
  spec id in a code string, in prose.
- **H-007**: `specs/012-core-plan/design.html#D-006` holds "011's re-entry pattern (parse, diff,
  ADD-or-ENHANCE) applies directly." A possessive spec number in prose; the expected element is
  `011 design.html#D-002`, which is not named.

**(d) Same-page sibling anchor — 0 among the chained**, but it exists and matters elsewhere:

- **G-059** (parallel): `specs/093-design-visual-section/spec.html#FR-014` carries
  `<a href="#FR-015">FR-015</a>`, and FR-015 carries `<a href="spec.html#FR-014">FR-014</a>`.
  Both ends are expected, so the anchor is available but earns nothing extra.
- **H-009**, **G-055**, **G-058** are same-page-sibling parallel questions (one document, 2 and 4
  and 2 addresses), i.e. the structure exists without a hop.
- **H-013** (parallel) is the cautionary case: its only hyperlink,
  `<a href="../007-core-triage/design.html#D-007">007 D-007</a>`, points at an element that is
  **not** the expected one (`#D-001`).

**(e) No pointer at all — 0 among the chained.** The nearest thing to a pointer-free relation is
**G-029**, which is parallel: the `validates` attribute sits on the *destinations* and points back
at the token the question already gave, and the golden `note` records that "body-text chunks don't
contain 'FR-008' in either criterion" — the relation is invisible to lexical search over prose even
though it is fully machine-readable in the markup.

### Inferences

- Two distinct capabilities are being conflated by the phrase "multi-hop". G-030 and H-001 need an
  **attribute reader**: follow `validates` from one element to named elements, no search involved.
  The other nine need an **identifier resolver plus a second search**: extract a spec number (or a
  placeholder slug) from prose or a `defer-to` attribute, then search inside that spec for the
  element the question describes. A graph index over typed links would fully solve 3 of 11
  (G-030, H-001, H-008) and only half-solve the other 8.
- Category (c) is 4 of 11 and is the category no markup-derived graph can capture without NER over
  prose: `013-core-propose` inside a thrown-error string, `011's` as a possessive, `071 US1 → US2`
  in an ASCII dependency table, and `TBD-explore-restore` as a placeholder that never became an id.
- The `defer-to` family (4 of 11) is the most tractable unexploited edge: it is a real attribute on
  a real element, it is already machine-readable, and it is the mechanism behind G-031, G-032,
  H-003 and H-004 — four of the six main-set-plus-held-out chains.

### Gaps

- I did not check whether the near-end elements are large enough to survive chunking (G-031's note
  says its near end is "the whole 247-word requirements section", G-032's "279-word"), because
  chunk-level sufficiency is a retrievability judgment reserved for the human author.

---

## The full table: all 105 questions

Classes: **SP** single-passage · **ME** multi-evidence (parallel) · **MH** multi-hop (1 hop,
chained) · **UA** unanswerable (no expected address). `n` is the number of `expected` addresses.
"latent" marks a question whose reference `answer` visibly required a second document that the
metric does not score.

### Main set — `questions.jsonl` (53)

| id | stratum | n | class | why |
|---|---|---|---|---|
| G-001 | single-fact | 1 | SP | one design decision; `note`: "D-001 is the smallest element" |
| G-002 | single-fact | 1 | SP | one design decision |
| G-003 | single-fact | 1 | SP | one triage card |
| G-004 | single-fact | 1 | SP | one triage card |
| G-006 | single-fact | 1 | SP | one archived proposal `#intent` |
| G-007 | single-fact | 1 | SP, **latent** | `note`: "The archive contradicts itself" — 087 `design.html#grounding` says HTTP 200, `tasks.html#T-301` says 404; only T-301 is expected |
| G-008 | single-fact | 1 | SP | one spec NFR |
| G-009 | single-fact | 1 | SP | one verify Run/Demo block |
| G-010 | single-fact | 1 | SP | one design decision |
| G-011 | paraphrase | 1 | SP | one design decision |
| G-012 | paraphrase | 2 | **ME** | question describes the behaviour; FR-009 and design D-002 both answer it |
| G-013 | paraphrase | 1 | SP | one design decision |
| G-014 | paraphrase | 1 | SP | one spec FR |
| G-015 | paraphrase | 1 | SP | one spec FR |
| G-016 | paraphrase | 1 | SP | one spec FR |
| G-017 | paraphrase | 1 | SP | expected is `110 spec.html#FR-003` — **the same address G-030 reaches by a hop**; asked directly here |
| G-018 | paraphrase | 1 | SP | one archived proposal `#intent` |
| G-019 | paraphrase | 1 | SP | one triage card |
| G-020 | paraphrase | 1 | SP | one triage card |
| G-021 | identifier | 1 | SP | scoped id lookup |
| G-022 | identifier | 1 | SP, **latent** | `note`: "The element refers to FR-001/FR-002 without restating them; Brian may want the answer to say what those are" |
| G-023 | identifier | 1 | SP | scoped id lookup |
| G-024 | identifier | 1 | SP | scoped id lookup |
| G-025 | identifier | 1 | SP | scoped id lookup |
| G-026 | identifier | 1 | SP | scoped id lookup |
| G-027 | identifier | 1 | SP | scoped id lookup |
| G-028 | identifier | 1 | SP | scoped id lookup |
| G-029 | relation | 2 | **ME** | question supplies the pivot `FR-008`; SC-001 and SC-003 both carry it in `validates`; reverse-attribute, no chain |
| G-030 | relation | 2 | **MH (a)** | names only `SC-002`/`spec 110`; `validates="FR-003,NFR-001"`; hop 1 **unscored** |
| G-031 | relation | 2 | **MH (a)** | "**Which spec** defines that field"; `defer-to="051-knowledge-corpus"`, spec-level |
| G-032 | relation | 2 | **MH (a)** | "**Which spec** handles that"; `defer-to="054-corpus-in-prompt"`, spec-level |
| G-033 | relation | 2 | **MH (c)** | "**Which slice** took that over"; pointer is the placeholder `TBD-explore-restore`; only near-bridge in the set |
| G-034 | relation | 1 | SP, **latent** | question names 001-cli, and `003 FR-010` names both 001-cli and the Python scripts, so the far end is self-identifying; `note`: "The forward link in 001-cli sits in a header `<spec-note>` with no id, so only the far end is addressable" |
| G-036 | relation | 2 | **MH (c)** | "**Which spec** provides that promotion hook"; bare `071 US1 → US2` row, spec-level |
| G-037 | stale | 1 | SP, **latent** | `note`: "the risks table still cites D-005's bash conditional" |
| G-038 | stale | 1 | SP | stale candidates in superseded 001-cli |
| G-039 | stale | 1 | SP | stale candidate in a withdrawn proposal |
| G-041 | stale | 1 | SP | two withdrawn siblings on 094; only `bind-the-checks` is listed stale |
| G-042 | stale | 1 | SP | stale candidate in a withdrawn proposal |
| G-043 | stale | 1 | SP, **latent** | two archived proposals are the stale candidates; current element is 326 words |
| G-044 | stale | 1 | SP, **latent** | one archived proposal is `accept`ed `for: question`, another is stale |
| G-045 | unanswerable | 0 | UA | no address; `note` lists the greps that return 0 hits |
| G-046 | unanswerable | 0 | UA | as above |
| G-047 | unanswerable | 0 | UA | as above |
| G-048 | unanswerable | 0 | UA | as above |
| G-049 | unanswerable | 0 | UA | as above |
| G-050 | unanswerable | 0 | UA | as above |
| G-076 | single-fact | 3 | **ME** | three FRs in one spec; question asks two things that cover them |
| G-077 | intent | 1 | SP | `spec.html#context` |
| G-078 | intent | 1 | SP | `spec.html#context` |
| G-079 | intent | 1 | SP | `spec.html#context` |
| G-080 | intent | 1 | SP | `spec.html#context` |
| G-081 | intent | 1 | SP | `spec.html#context` |

Main-set totals: SP 39 · ME 3 · MH 5 · UA 6.

### Held-out — `questions-heldout.jsonl` (13)

| id | stratum | n | class | why |
|---|---|---|---|---|
| H-001 | relation | 2 | **MH (a)** | "**which requirements does it prove**"; `validates="FR-003,NFR-002"`; hop 1 (SC-002) unscored |
| H-002 | relation | 2 | **ME** | two separately described asks → FR-004 and FR-007, same spec |
| H-003 | relation | 2 | **MH (a)** | "**Which one**"; `defer-to="053-corpus-grounding-gates"` in 052, which is unscored |
| H-004 | relation | 2 | **MH (a)** | "**the requirement it relies on**"; `defer-to="105-design-source-import">Opening an archive.` |
| H-005 | relation | 2 | **ME** | both ends expected and both semantically described; `SC-002 validates="FR-003"` is available but not needed |
| H-006 | relation | 2 | **MH (c)** | "**how was it completed later**"; `"not implemented; lands with 013-core-propose."` |
| H-007 | relation | 2 | **MH (c)** | "**Where did** the design command **get its rules**"; "011's re-entry pattern … applies directly" |
| H-008 | history | 2 | **MH (b)** | "**What reversed that**"; the only element-level `<a href=…#FR-007>` chain in the set |
| H-009 | history | 2 | **ME** | both addresses are `#intent` and `#FR-002` of **one** withdrawn proposal |
| H-010 | history | 1 | SP | one `#risks` section; self-contained |
| H-011 | history | 2 | **ME** | "**What was lost**" → proposal `#intent`; "**what now prevents it**" → FR-016 |
| H-012 | history | 1 | SP | T-1404's own text carries the retirement reason, so self-contained |
| H-013 | relation | 2 | **ME** (weakest call) | question names **both** commands; D-005's only link points at `#D-007`, not the expected `#D-001` |

Held-out totals: SP 2 · ME 5 · MH 6 · UA 0. **Six of 13 held-out questions are chained — the
held-out set is far more hop-dense than the main set (6/13 = 46% vs 5/53 = 9%).**

### Time set — `questions-time.jsonl` (24)

| id | stratum | n | class | why |
|---|---|---|---|---|
| G-051 | history | 2 | **ME** + disambiguation chain | the proposal is content-findable, but "a **second** withdrawn attempt" is only resolvable from `003 spec.html#changelog`, which **is** expected; its sibling is H-009's target, same date, same spec |
| G-052 | history | 2 | **ME** | proposal + `094 spec.html#changelog`; the question's own words separate it from its same-day sibling (G-041's stale record) |
| G-054 | history | 1 | SP | one triage card, including its correction |
| G-055 | history | 4 | **ME** | four same-page sibling tasks (phases 7 and 8 of one tasks.html); each carries its own retirement note |
| G-056 | history | 1 | SP | one decision in superseded 001-cli |
| G-057 | history | 1 | SP, **latent** | answer ends "D-005 is now marked superseded by D-006"; D-006 is in `stale`, not `expected` |
| G-058 | history | 2 | **ME** | `#risks` + `#changelog` of **one** withdrawn proposal |
| G-059 | history-decoy | 2 | **ME** | FR-014 + FR-015, same spec, with a real `<a href="#FR-015">` sibling anchor |
| G-060 | history-decoy | 1 | SP | same address in `expected` and `stale`: right address, wrong version |
| G-061 | history-decoy | 1 | SP | decoy is 001-cli's D-003, listed as stale |
| G-062 | as-of | 1 | SP, **latent** | answer traces the count 16 → 20 → 21 → 40 across later versions |
| G-063 | as-of | 1 | SP, **latent** | answer names `2026-08-14-kinds-from-the-closed-set` |
| G-064 | as-of | 1 | SP | same address as G-063, date-phrased cutoff; the paraphrase pair |
| G-065 | as-of | 1 | SP, **latent** | answer names `2026-06-23-sticky-header-in-calm` |
| G-066 | as-of | 1 | SP, **latent** | answer names `2026-06-25-spec-budget-bands` |
| G-067 | what-changed | 1 | SP (4 versions), **latent** | one address, `versions` = before(absent)/2 intermediate/after; answer names three proposals |
| G-068 | what-changed | 1 | SP (2 versions), **latent** | answer names `2026-09-14-oidc-trusted-publishing` |
| G-069 | what-changed | 1 | SP (4 versions), **latent** | answer: "The changelog dates all three to 11 Jul; git records them on 12 Jul UTC" |
| G-070 | what-changed | 1 | SP (3 versions), **latent** | answer names three proposals |
| G-071 | what-changed | 1 | SP (2 versions), **latent** | answer names `2026-08-22-what-a-candidate-is-made-of` |
| G-072 | as-known | 1 | SP | `expected` is `plan.html#D-005`, `stale` is `design.html#D-005` — a file rename, not a hop |
| G-073 | as-known | 1 | SP | one task at one commit; same address in `expected` and `stale` |
| G-074 | as-known | 1 | SP | one decision at one commit; same address in `expected` and `stale` |
| G-075 | as-known | 0 | UA | unanswerable as of the cutoff; `stale` is `111 spec.html#FR-001` |

Time-set totals: SP 18 · ME 5 · MH **0** · UA 1. Latent multi-hop: 11 of 24.

### Routing — `questions-routing.jsonl` (15)

| id | stratum | n | class | why |
|---|---|---|---|---|
| R-001 | intent-noword | 1 | SP | `000-spectastic/spec.html#context`; "the only place the benchmark figure appears" |
| R-002 | intent-noword | 1 | SP | `002-validate-cli/spec.html#context` |
| R-003 | intent-noword | 1 | SP | `003-init-node-port/spec.html#context` |
| R-004 | intent-noword | 1 | SP | `004-npm-publish-workflow/spec.html#context`, "a single short section" |
| R-005 | intent-noword | 1 | SP | `015-ai-stub-injection/spec.html#context`; fuller reasoning is outside the corpus |
| R-006 | intent-noword | 1 | SP | `016-theme-support/spec.html#context` |
| R-007 | intent-noword | 1 | SP | `021-verify-view/spec.html#context`; `verify-considerations.html` deliberately excluded |
| R-008 | intent-noword | 1 | SP | `111-codex-skill-adapters/spec.html#context`; chosen for standalone-ness |
| R-009 | triage-why | 1 | SP | card headline + Diagnosis; spec.html explicitly not expected |
| R-010 | triage-why | 1 | SP | card headline + Diagnosis |
| R-011 | triage-why | 1 | SP | card headline + Diagnosis |
| R-012 | triage-why | 1 | SP | card headline + Diagnosis |
| R-013 | triage-why | 1 | SP | card headline + Diagnosis, quoting both call sites |
| R-014 | triage-why | 1 | SP | card headline + Diagnosis; spec.html explicitly not expected |
| R-015 | triage-why | 1 | SP | card headline + Diagnosis |

Routing totals: SP 15 · ME 0 · MH 0 · UA 0.

### Universe totals (105)

| class | count | ids |
|---|---|---|
| Single-passage | **74** | main 39, held-out 2, time 18, routing 15 |
| Multi-evidence (parallel) | **13** | G-012, G-029, G-076, H-002, H-005, H-009, H-011, H-013, G-051, G-052, G-055, G-058, G-059 |
| Multi-hop (chained, 1 hop) | **11** | G-030, G-031, G-032, G-033, G-036, H-001, H-003, H-004, H-006, H-007, H-008 |
| Bridge (2+ hops) | **0** | — |
| Unanswerable (no address) | **7** | G-045, G-046, G-047, G-048, G-049, G-050, G-075 |

Latent multi-hop, scored single: **G-007, G-022, G-034, G-037, G-043, G-044, G-057, G-062, G-063,
G-065, G-066, G-067, G-068, G-069, G-070, G-071** — 16 questions, 11 of them in the time set.

### Undetermined

None. Every one of the 105 has a class. The two softest calls, recorded with their reasoning
rather than hidden, are **H-013** (classed ME; could be argued MH because only D-005's prose pins
the element in triage, and its one hyperlink points at the wrong element) and **G-033** (classed
MH at 1 hop; could be argued a 2-hop bridge because the `TBD-explore-restore` → `024` mapping
lives in an unscored third document).
