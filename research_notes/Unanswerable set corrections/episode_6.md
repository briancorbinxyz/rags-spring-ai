# Episode 6 — where the draft misrepresents or omits the unanswerable six

Audit of `/Users/briancorbin/Documents/Brain/Library/03 Resources/RAGs to Riches with Spring AI, Episode 6, Links, History and Time.md`
(401 lines, ~5,800 words, `stage: seedling`, `publish: false`, dated 2026-10-27). File not edited.

Line numbers below are the draft's own, as of this audit.

## Verification done first (so the list can be trusted)

Checked against the repo rather than assumed:

- `src/main/resources/golden/questions.jsonl` is **53 lines**. Strata: `single-fact` 10, `paraphrase` 10,
  `identifier` 8, `relation` 7, `stale` 7, **`unanswerable` 6**, `intent` 5. So 47 answerable, and the
  draft's "All five of the main set's intent questions" (line 229) is **correct**.
- The six are G-045 to G-050, each `"expected": []`, `"answer": "Not covered in specs/."`,
  `"core": "Declines: not covered."`
- **Only the main set carries an `unanswerable` stratum.** `questions-time.jsonl` (24) = `history` 7,
  `history-decoy` 3, `as-of` 5, `as-known` 4, `what-changed` 5. `questions-heldout.jsonl` (13) =
  `relation` 8, `history` 5. `questions-routing.jsonl` (15) = `intent-noword` 8, `triage-why` 7.
  So the 24 and 13 denominators are *sound as arithmetic* — they just never say they are.
- Held-out: 11 of 13 need two passages (H-010 and H-012 need one), matching line 315.
- `research_notes/Refusal as a first-class metric/groundless_at_4.md` is the authority for
  `groundless@4`. Episode 6's own line (`GA`, 5090): **47/53 grounded, 6/53 groundless, 0 of 6
  confident**. Episode 5's line (`E5-2`): 46/53, 7/53, 0 of 7. Episode 3's naive attempt (`EP3`):
  10/47, 37/47, **24 of 37 confident**.
- Rename: `git -C /Users/briancorbin/Code/spectastic show 836dc1a` is dated **2026-07-30 21:58:39 -0400**,
  "refactor: rename the `plan` lifecycle verb to `design`", **321 files changed**, and contains
  `R099 specs/004-npm-publish-workflow/plan.html -> specs/004-npm-publish-workflow/design.html`.
  **There are no commits at all dated 2026-07-31** — the log jumps 07-30 to 08-02. So "31 July" is
  wrong twice over. (A follow-up, `8140e71` on 2026-08-02, "complete the plan -> design rename in
  templates and the estate", finished the job elsewhere in the estate but did not move the 004 page.)
- Routing set: **superseded 28 Oct by the 5090 run** (`66eaeff`). The laptop figures `E5-2` 6/15
  and `GA` 5/15, and the sign-test caution built on their one-question gap, are laptop artefacts.
  At the main tier `S5`, `E5-2`, `G9` and `GA` all answer **14/15**, missing only `R-004`, and the
  inversion does not reproduce. The live numbers are in
  `research_notes/The whole question set/the 5090 runs of 28 Oct.md`. All 15 are still
  `"status": "draft"`.

---

## The edit list

### 1. The lede understates the set and overstates the coverage

- **Quote:** "it found 44 of 47 questions, and every one of them asks about the archive as it stands today."
- **Location:** line 31, the "Previously, on" paragraph, before the first `---` (unheaded).
- **Class:** prose / number. **Correction.**
- **Should say:** name the real set once, here, before any score is quoted: 53 questions, 47 answerable
  and 6 written to be declined, and `Hit@4` is over the 47. "Every one of them asks about the archive as
  it stands today" is true of the 47 and should be scoped to them, since the six ask about things the
  archive never covered at any time.

### 2. Step 7's table has no refusal column

- **Quote (table heading row and both data rows):**
  `| 5090 | Hit@4 | all-evidence@4 | relation | verdict |`
  `| step 6 (Episode 5's line) | 44/47 | 40 | 5/7 | start |`
  `| **step 7: + typed expansion** | **46/47** | **42** | **7/7** | **pass**, +2 / −0 |`
- **Location:** lines 73-76, under `## Step 7: follow the links the archive already declares`.
- **Class:** table. **Addition.**
- **Should say:** add an `groundless@4` column. Step 6 is 7/53 and step 7 is 6/53 — step 7 closes one
  genuine miss, and the remainder is the six. This is the cheapest place in the article to introduce the
  counter, because it is the first table with a 47 in it.

### 3. "10 of 53" is the only 53 in the article, and it is unexplained

- **Quote:** "It fires on 10 of 53 questions, changes the top ten on three, and does nothing at all on the other 43."
- **Location:** line 78, under `## Step 7`.
- **Class:** prose / number. **Correction (clarifying).**
- **Should say:** the arithmetic is right (10 + 43 = 53) but a reader three lines below a `44/47` table
  now has two denominators and no account of either. Either gloss it here ("53 — the main set's 47
  answerable plus the six it should refuse") or, better, let edit 1 do that work and leave this
  sentence alone.

### 4. Step 10's "main-set ratchet" cell does not say what it guards

- **Quote:** `| **step 10: + the named spec and document, their own rankings** | **8/9** | **passes, both tiers** |`
  (header `| 5090, over the slice | answered | main-set ratchet |`, prior row `| step 9 (Episode 5's line) | 7/9 | clean |`)
- **Location:** lines 187-190, under `## Step 10: the question names its haystack`.
- **Class:** table. **Addition.**
- **Should say:** "clean" and "passes, both tiers" are verdicts over the 47. One clause in the caption —
  that the ratchet guards the 47 answerable questions and that `groundless@4` is unchanged at 6/53
  across both rows — makes the claim honest without touching the numbers.

### 5. Step 11's table invites the decoys to be read as refusal questions

- **Quote (header and rows):**
  `| 5090, time set | history (7) | decoys (3) | stale@4 |`
  `| step 7 (Episode 6's line, before this step) | 0 | 3 | 10 |`
  `| **step 11: + both arms** | **4** | **3** | **9** |`
- **Location:** lines 214-217, under `## Step 11: history`.
- **Class:** table. **Addition.**
- **Should say:** add a caption line stating that **the three decoys are answerable** — they have
  expected passages and today's text is the right answer — and that the time set carries no
  unanswerable stratum at all. Without this, "decoys 3/3" reads as "refusal works", which is the exact
  misreading the episode must not leave standing.

### 6. Step 11 already says "refusable" in a different sense, and must disambiguate

- **Quote:** "Whatever we build here has to be refusable, and the question set is what forces it: three of the ten are decoys, with a history word in them and today's text as the answer."
- **Location:** line 209, under `## Step 11: history`.
- **Class:** prose. **Correction.**
- **Should say:** this is the draft's strongest existing hook and also its most confusing word. Here
  "refusable" means *the ranking may decline to promote an admitted list* — not *the system may decline
  to answer*. Keep the sentence, then add one clause separating the two senses, and point forward to
  the refusal discussion that edits 13-15 add. A reader who meets "refusable" at Step 11 and "47 of 47"
  at the results will reasonably conclude refusal is solved.

### 7. The same ambiguity in the `admit_history` bullet

- **Quote:** "It's a list the ranking may decline, not an unfiltering of the search."
- **Location:** line 211, under `## Step 11: history`.
- **Class:** prose. **Addition (one clause).**
- **Should say:** keep it — it is a good sentence — and make explicit that the ranking declining a
  *list* is not the pipeline declining a *question*. Nothing in the pipeline does the latter.

### 8. Step 12's table is the headline's feeder and carries no refusal column

- **Quote (header and rows):**
  `| 5090, main set | Hit@4 | all-evidence@4 | stale@4 | MRR | ratchet |`
  `| step 11 | 46/47 | 42 | 0 | 0.890 | — |`
  `| **step 12: + `#context` as a collection** | **47/47** | **43** | 0 | **0.911** | **pass** |`
- **Location:** lines 248-251, under `## Step 12: why is a section`.
- **Class:** table / number. **Addition.**
- **Should say:** add `groundless@4`: **7/53** at step 11, **6/53** at step 12, with a footnote that
  the six remaining are the six unanswerable and that **0 of 6 outscore the median correct answer**.
  The column is what stops `47/47` from arriving unqualified in the next section.

### 9. "The main set's last miss" is false on the real set

- **Quote:** "That left the main set's last miss on the 5090: **G-080**, "What problem led spectastic to add checks that the same spec number or requirement id isn't handed out twice?""
- **Location:** line 227, opening `## Step 12: why is a section`.
- **Class:** prose. **Correction.**
- **Should say:** "the last of the 47 it should find". Six questions still receive four passages each
  that hold nothing expected; calling G-080 the last miss writes them out of existence at the very
  moment the episode is about to claim completeness.

### 10. The three unshipped collections are the natural home for the routing set

- **Quote (table):**
  `| collection | size | the expected unit's rank |`
  `| `#changelog` | 596 | G-051 **2**, G-052 **3** |`
  `| `#risks` | 415 | G-058 **1**, H-010 **2** |`
  `| `#intent` | 156 | H-009 **2**, H-011 **1** |`
  and the prose at line 263: "But for `#context` the mapping is the archive's own schema and holds 5 of 5. For the others I'd be inventing a trigger from two or three examples each"
- **Location:** lines 257-263, under `## Step 12: why is a section`.
- **Class:** table / prose. **Addition.**
- **Should say:** this is where the 15-question routing set belongs, *if it belongs anywhere* — it was
  written to test exactly this trigger (8 `intent-noword` questions asking a spec's intent without the
  word "why", 7 `triage-why` questions). Two or three sentences at most, flagged as **draft, no blind
  audit round yet**.

  **Rewritten 28 Oct.** The finding is no longer about the `#context` arm, which gains nothing here:
  `G9` and `GA` both answer **14/15**. It is about where the generalisation comes from. The ladder
  goes `S4` 6/15 → `S5` **14/15** — eight questions won by the cross-encoder alone, losing none — on
  a set no part of the stack was built against. A 22M-parameter reranker answers 5 of them and an 8B
  one answers 14, which lands Episode 5's argument about model size on questions Episode 5 never saw.

  The refusal half has flipped too, and the old instruction must not be followed: at this tier only
  **one** answer is groundless and it is **not** confident, so "the clean separation does not survive
  unfamiliar questions" is false of the main line. It holds on the BM25 line (`S3`: groundless on 12,
  confident on 10) and at the laptop tier. Say that, because the contrast is the point.

  Recommendation unchanged: keep it out of the results tables entirely and put the finding in
  `## Next` (edit 18), with only a one-line forward reference here.

### 11. `#context` generalises "because the archive says so" — on five questions

- **Quote:** "And this generalises, because the *archive* says so rather than because three questions do. **All five of the main set's intent questions expect a `#context` unit**"
- **Location:** lines 229, under `## Step 12: why is a section`.
- **Class:** prose. **Addition (optional, one clause).**
- **Should say:** the count is correct (`intent` stratum is exactly 5). But the routing set's 8
  `intent-noword` questions are the first test of that generalisation outside the five, and the trigger
  does not fire on them by construction. If edit 10's forward reference goes in anywhere, here is the
  honest place to hedge "generalises".

### 12. The headline table

- **Quote (header and all four rows):**
  `| 5090 | Hit@4 | All-evidence@4 | Stale@4 | Verdict |`
  `| 6. Episode 5's line | 44/47 | 40 | 0 | start |`
  `| 7. + typed expansion | 46/47 | 42 | 0 | **pass** |`
  `| 10–11. + the scope arms, history admission | 46/47 | 42 | 0 | **pass** |`
  `| 12. + `#context` as a collection | **47/47** | **43** | 0 | **pass** |`
  (preceded by "The main set, on the 5090:" at line 285)
- **Location:** lines 285-292, under `## Where Episode 6 lands`.
- **Class:** table. **Correction + addition.**
- **Should say:** this is the table the episode is judged on, and it needs both a new column and a new
  caption. Add `groundless@4`: **7/53, 7/53, 7/53, 6/53** down the rows (Episode 5's line and step 7
  both sit at 7 by the stored recomputation; step 7 closing two answerable questions is already in the
  `Hit@4` column, and the extra groundless question falls between step 7 and step 12). Replace the
  "The main set, on the 5090:" line with one that states the denominator: 53 questions, 47 answerable
  plus 6 written to be refused, `Hit@4` over the 47. *Flag:* the per-row `groundless@4` for rows 7 and
  10-11 should be confirmed against `scratchpad/groundless.py` before publication; only the `GA` row
  (6/53) and the `E5-2` row (7/53) are stated outright in the source note.

### 13. "47 of 47" — the headline sentence itself

- **Quote:** "**47 of 47.** Steps 8 and 9 don't apply to the main set, because its questions ask about the present. The ratchet's job there was to confirm they cost nothing, and they don't."
- **Location:** line 294, under `## Where Episode 6 lands`.
- **Class:** prose / number. **Correction — the single most important edit in the list.**
- **Should say:** the honest sentence, in the draft's own register: 47 of the 47 it should find, and on
  the other 6 it serves four confident passages with no basis. Nothing in the pipeline declines. Then
  the turn that makes this a better story than the one it replaces: **0 of the 6 scores above the
  median correct answer**, so a refusal line demonstrably exists on this corpus and the system is one
  threshold away from being able to decline. That is a stronger claim than 47/47, and it was in the
  stored data already.

### 14. A refusal paragraph is missing entirely from the results section

- **Location:** `## Where Episode 6 lands`, new material after line 294 and before "The time set, by the
  configuration that ships for each kind of question:" (line 296).
- **Class:** prose + table. **Addition.**
- **Should say:** a short subsection introducing `groundless@4` — questions whose top four holds no
  expected passage; always true of an unanswerable question, since there is nothing it could correctly
  serve; **no threshold in the definition**, which is what lets it be computed backwards over stored
  runs, so no published number moves. Then the series' own trajectory, which is the payload:
  Episode 3's naive attempt 37/47 groundless with **24 of 37 confident**; Episode 5's line 7/53 with
  0 of 7; this episode's 6/53 with 0 of 6. The series fixed calibration, not only recall, and never
  said so.

### 15. The main set has no per-stratum table, so `unanswerable` is nowhere a row

- **Quote (the only per-stratum table, which is the *time* set):**
  `| stratum | answered | what stops the rest |`
  `| `as-of` + `as-known` (9) | **8** | G-074 at rank 5, the mirror of G-073 |`
  `| `history` (7) | **4** | G-051 (12th, see the epilogue), G-058 (admitted, outside the top 50; `#risks` ranks it first) |`
  `| history-decoys (3) | **3** | — |`
  `| `what-changed` (5) | **1**, located and told | the story of a change outranks the change |`
  `| **total** | **16 of 24** | |`
- **Location:** lines 298-304, under `## Where Episode 6 lands`.
- **Class:** table. **Addition.**
- **Should say:** direct answer to the audit question — **`unanswerable` is not a row in this or any
  table, and the main set has no per-stratum breakdown at all.** The time set's strata are complete and
  correct as printed (9 + 7 + 3 + 5 = 24, and the set genuinely has no unanswerable stratum), so this
  table needs only a caption saying so. What is missing is a **sibling table for the main set**:
  `single-fact` 10, `paraphrase` 10, `identifier` 8, `relation` 7, `stale` 7, `intent` 5 — all answered
  — and **`unanswerable` 6, answered 0 of 6, correctly refused 0 of 6, confidently wrong 0 of 6**. That
  last row is the whole correction in one line, and it reads as a finding rather than an apology.

### 16. The held-out table's denominator is sound but silent

- **Quote:** `| held-out, 13 | Hit@4 | all-evidence@4 | all-evidence@10 | MRR |` with rows
  `| step 7 | 6/13 | 3 | 4 | 0.349 |` and `| step 12 | **7/13** | **5** | **5** | **0.435** |`,
  and the prose "**It's a floor.** Nobody has judged the held-out set's 105 candidate passages yet, and an unjudged passage counts as wrong."
- **Location:** lines 310-315, under `## Where Episode 6 lands`.
- **Class:** table. **Addition (one clause).**
- **Should say:** 13 is the whole set — `relation` 8 + `history` 5, no unanswerable stratum — so unlike
  the main set nothing is excluded here. One clause saying that, and the same for the 24, removes the
  reader's suspicion that every denominator in the episode hides something. The existing "an unjudged
  passage counts as wrong" is the right neighbour for it.

### 17. The epilogue's arms table reports bare numbers out of 47

- **Quote (rows):**
  `| `route_scope` | rerank only when the question named no haystack | main set 33 → **31** (laptop) |`
  `| `scope_pin` | a language model reads the named scope, and its picks are pinned | 9/9 on the dated questions, and main set 46 → **45** on the 5090 |`
  `| `section_scope` | a chooser picks the section kind | G-080 21 → **23**, and 92 seconds a question |`
- **Location:** lines 323-330, under `## Epilogue: what the 5090 made unnecessary`.
- **Class:** table / number. **Correction.**
- **Should say:** "main set 33 → 31" and "46 → 45" are out of 47 and never say so; "G-080 21 → 23" is a
  rank, not a score, in the same column. Write the denominator into the cells ("33 → 31 of 47") or into
  the caption. Low stakes individually, but this table is where a reader who already distrusts the
  headline goes looking.

### 18. "On a 33-of-47 baseline" — the one place the draft gets it right

- **Quote:** "On a 33-of-47 baseline that was absorbed. At 46 there's no slack, and **a pin on a strong baseline can only displace**."
- **Location:** line 335, under `## Epilogue`.
- **Class:** prose. **No change — model it.**
- **Should say:** nothing. This is the only "of 47" written out in full anywhere in the article, and it
  is the phrasing the other edits should copy. Worth noting so an editor does not "tidy" it to "33".

### 19. "Close the main set"

- **Quote:** "The scope arms are what close the main set, so the trade stands and G-051 goes in the table above with its reason."
- **Location:** line 343, under `## Epilogue`.
- **Class:** prose. **Correction.**
- **Should say:** "close" is the completeness claim in miniature. "What close the answerable 47" or
  "what take the answerable set to 47 of 47". Also the related commit subject on `episode-5`
  (`3b51337`, "the 5090 closes the main set at 47/47") carries the same framing; not in the article, but
  it is where the phrase came from.

### 20. The only refusal in the whole article is in the Ship's Log, and it is about truncation

- **Quote:** "A prompt over that limit isn't refused. **It is silently truncated.** Part of the candidate list is simply never seen, and the ranking still comes back looking like a judgement: confident, well-formed, and wrong in a way nothing downstream could detect. It would have gone in the table as a result."
  and "The fix is two lines and a check: set `num_ctx` explicitly, and refuse the result if the prompt's evaluated token count reaches it."
- **Location:** lines 349-357, under `## Ship's Log: the reader that never read the list`, including the
  code block `if answer.get("prompt_eval_count", 0) >= self.CONTEXT: raise RuntimeError(...)`.
- **Class:** prose. **Addition (two sentences).**
- **Should say:** this is the article's best writing on confident wrongness and the **only** declining
  path it contains — and it declines a *ranking* for being truncated, never an *answer* for having no
  basis. Two sentences drawing the parallel: the same failure shape the Ship's Log catches in the
  reader is what the six unanswerable questions catch in the pipeline, and the counter that makes it
  visible needs no threshold either. "It would have gone in the table as a result" is the hinge — on
  the six, four baseless passages *did* go in the table as a result.

### 21. "Next" needs the refusal thread and is the right home for the routing set

- **Quote (current list):**
  "- **History questions name no date**, so something has to infer which past the reader means.
   - **`what-changed` needs its address found**, and the search prefers the story of a change to the change.
   - **Whether a withdrawn record counts as history** is a definition, and it's mine to make."
  and the closing "Beta can tell you what the archive said last Tuesday now. It still can't tell you why it changed its mind."
- **Location:** lines 363-373, under `## Next: three problems wearing one label`.
- **Class:** prose. **Addition.**
- **Should say:** add a fourth item: **a declining mechanism**, which the episode has measured the
  ground for but deliberately not built. State why it waits — a line at zero on the cross-encoder logit
  is principled, but fitted on either available set it disagrees with the other, so **confidence is not
  calibrated across corpus regions** and a single threshold would be fitting. This is where the routing
  set's one load-bearing number belongs: on 15 unfamiliar draft questions, **4 of 9 groundless answers
  outscore the median correct one**, so the clean 0-of-6 separation is a property of the questions the
  system was developed against, not of the system. **Superseded 28 Oct:** at the 5090 tier that is
  false of the main line — one groundless answer, not confident — and true of the BM25 line and the
  laptop. Flag the set as draft, no blind-audit round yet, and keep its figures out of the results
  tables; `GA` is **14/15**, not 5/15.

### 22. The rename date is wrong

- **Quote:** "Its answer lives in `004/plan.html`, and the current archive has no such page: it was renamed to `design.html` on 31 July."
- **Location:** line 173, under `## Step 9: a slice is a collection`, in the **G-072** paragraph.
- **Class:** number. **Correction.**
- **Should say:** **30 July 2026**, commit `836dc1a` ("refactor: rename the `plan` lifecycle verb to
  `design`", 321 files changed), which contains
  `R099 specs/004-npm-publish-workflow/plan.html -> specs/004-npm-publish-workflow/design.html`.
  There are no commits dated 31 July at all. Nothing else in the paragraph depends on the date —
  G-072 asks about 1 July, which is before either date — so this is a one-word fix.

### 23. The draft-status callout

- **Quote (first bullet, in full):** "- **First draft, written overnight on 27 Oct** from the Series Plan's *Episode 6* sections, the overnight 5090 runs, and the committed JSONL. Every number below is from a committed run, and the commit is named where it matters. Structure follows Episodes 4 and 5. It's about 5,800 words and needs a pass for voice."
- **Location:** line 21, inside `>[!todo] Draft status (delete before publishing)` (lines 20-29).
- **Class:** prose. **Correction.**
- **Should say:** "needs a pass for voice" is stale — drop it. Add a bullet recording this audit: the
  main set is 53 and `Hit@4` is over 47; `groundless@4` is now reported beside it; the headline is "47
  of the 47 it should find" and the positive claim is 0 of 6 confident. Note that the "Every number
  below is from a committed run" promise is the reason the rename date (edit 22) matters — it was the
  one number that was not.

### 24. The three planned diagrams, and where a refusal branch belongs

- **Quote:** "- **Diagrams to draw:** the state table (one unit, three intervals); a slice cut through it at one position; the four collections (the slice, the named document, the hidden units, `#context`) as subsets of one archive."
- **Location:** line 28, inside the draft-status callout.
- **Class:** diagram. **Addition.**
- **Should say:** direct answer to the audit question — **the article contains no diagrams or figures at
  all**; these three are planned and undrawn, and **none of them has a declining path**. Each shows a
  subset of the archive being selected; none shows the pipeline's exit. The refusal branch belongs in a
  **fourth diagram**: the pipeline's tail, where the fused ranking's top score meets a threshold and
  forks — serve four passages, or decline. Drawn with the six unanswerable questions sitting on the
  low side of the line and the median correct answer on the high side, it is the whole correction in one
  picture, and it is the figure the episode currently lacks. Second choice: extend the four-collections
  diagram with the 82 addresses "excluded in every state" (line 221) as a fifth region nothing reaches,
  which pairs with the refusal point.

### 25. Non-findings, recorded so they are not re-audited

- Lines 49-54 (the four-row `0/9` table under the lede) and lines 161-167 (the "over the slice" table):
  denominator 9, the `as-of` + `as-known` subset. Verified correct, no unanswerable questions involved.
- Line 246 ("the expected one ranks *first* for four of the five intent questions") and line 229
  ("All five"): both correct against the `intent` stratum of 5.
- Line 281, `## What changed is a diff, not a passage`: "Some of those passages may well answer the
  question. That's a judgement for the blind audit, not for me." — already the right epistemics;
  nothing to correct. Same for line 315's "an unjudged passage counts as wrong" and line 326's
  "everything lands at rank 7–8, committing to nothing".
- Line 334, "A cross-encoder is useful precisely because it overrules, and making it vote removes what
  it was for." — worth knowing that the source note adds a second reason the reranker earns its place
  ("BM25 alone leaves 4 of 23 groundless answers scoring above the median correct one; adding the
  reader takes that to 0"), i.e. *knowing when it has nothing*. An optional one-clause enrichment, not
  a correction.

---

## Counts and judgement

**24 actionable edits + 1 non-findings record.**

By class: **prose 11** (1, 3, 6, 7, 9, 11, 13, 14, 19, 20, 21, 23 — counting 11 and 23 as prose),
**table 8** (2, 4, 5, 8, 10, 12, 15, 16), **number 3** (17, 22, and 13 which is both),
**diagram 1** (24), **no-change/model 1** (18).

By kind: **corrections 9** (1, 3, 6, 9, 13, 17, 19, 22, 23), **additions 15**.

Statements whose denominator is 47: **eleven** — lines 31, 75, 76, 250, 251, 289, 290, 291, 292, 294,
325, 327, 335, 343 (fourteen if each table row is counted separately). Tables: **eleven**, of which
**six need a column, a caption or a denominator line**. Diagrams: **none exist**; three are planned and
none declines. Per-stratum tables: **one**, for the time set; `unanswerable` appears as a row **nowhere**.

**Judgement: a restructured results section, not a light edit.**

Everything outside `## Where Episode 6 lands` is genuinely light — edits 22 (one word), 23, 17 and 19
are small, and 2, 4, 5, 8 are one column or one caption each. But `## Where Episode 6 lands` cannot be
patched. Its headline sentence is wrong in the one way the episode most needs to be right (13), its
main table is missing the column that would make the sentence true (12), the main set has no
per-stratum table in which the six could appear at all (15), and the refusal result the episode has
already earned — 0 of 6 confident, a line that demonstrably exists — has nowhere to go (14). Those four
are one piece of work: a results section that reports `grounded / groundless / confidently wrong`
and leads on calibration rather than on 47/47.

The compensating point, and the reason this is worth doing rather than conceding: the corrected story
is **better** than the one it replaces. "47 of 47" is a number a sceptical reader discounts on sight.
"47 of the 47 it should find, nothing declines yet, and not one of the six baseless answers outscores
the median correct one — a refusal line exists and we are one threshold from it" is a finding, it was
already in the stored data, and it sets up Episode 7. Edits 6, 7 and 20 show the draft was reaching
for this on its own: "has to be refusable", "a list the ranking may decline", and the Ship's Log's
"confident, well-formed, and wrong in a way nothing downstream could detect". The article is three
tables and one paragraph away from saying what it already knows.

One caution for whoever applies the list: the per-row `groundless@4` values for the intermediate rows
of edit 12 are an inference from the stored note, which states only the `E5-2` (7/53) and `GA` (6/53)
endpoints. Recompute with `scratchpad/groundless.py` before those cells are published.
