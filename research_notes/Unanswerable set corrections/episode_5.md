# Unanswerable set corrections — Episode 5, Bigger Models

Audited file: `/Users/briancorbin/Documents/Brain/Library/03 Resources/RAGs to Riches with Spring AI, Episode 5, Bigger Models.md`
(416 lines, 5,989 words; read whole). Line numbers below are from that file as of 27 Oct 2026. Nothing was edited.

Corroborating facts checked in the repo (not from the article):
`src/main/resources/golden/questions.jsonl` = 53 questions; `stratum` counts are single-fact 10,
paraphrase 10, identifier 8, relation 7, stale 7, intent 5 (= 47 answerable) and **unanswerable 6**,
whose ids are exactly `G-045`–`G-050`. Main-set ids are not contiguous (they run past G-050 to G-081),
so `G-076`, `G-079`, `G-080` in the article are main-set questions, not held-out ones.
`questions-heldout.jsonl` = 13 questions, `H-001`–`H-013`, 8 relation + 5 history, **none unanswerable**.
`docs/design-history.md:127-131` already states the 53/47 split; the article does not.

## The headline problem, in one line

In the **published** body of this article the words "53" appear once (line 323, as "8 of 53"), the six
unanswerable questions appear once (lines 205–221, in a scoring aside), and the fact that `Hit@4` has a
denominator of 47 because six questions were removed from it is **never stated at all**. The only place
the split is written down, line 22, sits inside the `>[!todo] Draft status (delete before publishing)`
callout and is deleted before publishing. A reader of the shipped article sees nineteen numbers out of 47
and one unexplained 53.

---

# Edit list

## 1. The set's size and shape are never stated in the published text

- **Quote:** "Keyword search, an identifier lookup, a lifecycle filter and a redirect took it from 10 of 47 questions to 32."
- **Location:** line 36, the "Previously, on …" recap, before `## Step 5: meaning, fused`.
- **Class:** prose. **Addition.**
- **Should say:** add one sentence after it giving the set as 53 questions, 47 answerable and 6 written to
  have no answer (`G-045`–`G-050`), and saying plainly that `Hit@4` is computed over the 47 and cannot
  express a refusal — so every `n/47` below is a score on the questions that have an answer. This is the
  one edit the rest depend on; without it the reader's default reading is "the system answers everything".

## 2. Step 5's numbers table carries no groundless row

- **Quote (table heading/caption):** "**The numbers.** Step 5 is Episode 4's step 4 with the dense list fused in:" then the table with rows `Hit@4` "32/47 | **36/47**", `All-evidence@4` "26 | **33**", `Stale in top four`, `Paraphrase`, `Relation`.
- **Location:** lines 113–121, under `## Step 5: meaning, fused`.
- **Class:** table. **Addition** (plus a denominator correction).
- **Should say:** rename the first row `Hit@4 (of 47 answerable)` and add a row
  `groundless@4 (of 53)` — questions whose top four holds no expected passage — with the step 5 laptop
  value, and a row for how many of those scored above the median grounded question. The laptop
  BM25-only reference point is `S1` 23/53 groundless, 4 confident; see gap G3 before attributing values
  to specific step rows.

## 3. The ratchet's rules are named without a refusal clause

- **Quote:** "And it fails the ratchet, on the third rule: *any one kind of question loses more than one question*."
- **Location:** line 123, `## Step 5: meaning, fused`.
- **Class:** prose (ratchet). **Addition.**
- **Should say:** this is the only place in the article where a ratchet rule is quoted verbatim, so it is
  where a reader learns the rule set. The rules governing `Hit@4`, `all-evidence@4`, per-stratum losses
  and `stale@4` have no clause about answers served without evidence; a fifth clause —
  *`groundless@4` must not rise* — belongs here as a forward reference, and its full statement belongs
  at edit 26.

## 4. The cascade paragraph describes a pipeline that cannot decline

- **Quote:** "The reranker is too expensive to read more than fifty, so its job is **precision**: get the answer into the four the reader is given. It can reorder what it's handed but never add to it. If the answer is fifty-first, no reranker can save it. And once it re-sorts the fifty, the fusion's order is gone: all step 5 decides now is who gets in."
- **Location:** line 162, `## Step 6: a cross-encoder reads it again`.
- **Class:** prose. **Addition.**
- **Should say:** the reranker has a second job the paragraph omits — it is the only stage that scores a
  question against a passage, so it is the only stage that can decide *nothing here is good enough*.
  Add that sentence here; it sets up both the calibration finding (edit 9) and the refusal branch in the
  diagram (edit 12). As written, "never add to it" is the only limit named, and the limit that matters
  to this audit — it never returns fewer than four — is not.

## 5. The step-6 ceiling is a ceiling over the answerable questions only

- **Quote:** "Step 5 leaves an expected passage somewhere in its top fifty for **40 of 47** questions on the laptop, and for **45 of 47** with `Qwen3-Embedding-8B` on the 5090. That is as high as step 6 can go on either tier"
- **Location:** line 164, `## Step 6`.
- **Class:** number. **Correction** (clarifying).
- **Should say:** keep the arithmetic, add the reason the denominator is 47: six questions have no
  expected passage to reach, so recall is defined only over the 47 — and for those six the ceiling
  question is the opposite one, whether the pipeline can be made to return nothing.

## 6. Step 6's numbers table carries no groundless row

- **Quote (caption):** "**The numbers.** It should be better. It is much worse:" then rows `Hit@4` "36/47 | **32/47**", `All-evidence@4` "33 | **29**", `Single fact` "9/10 | 7/10", `Paraphrase` "6/10 | 3/10"; followed by the block "step-6 vs step-5: FAIL — Hit@4 36→32; all-evidence@4 33→29; / single-fact 9→7; paraphrase 6→3".
- **Location:** lines 189–201, `## Step 6`.
- **Class:** table. **Addition.**
- **Should say:** add `groundless@4 (of 53)` and the confident count. This is the table where the
  episode's new finding is loudest in the wrong direction: the row says the reranker is "much worse",
  while `S5` (this row) is 21/53 groundless with **0 confident** against `S1`'s 23/53 with **4**. One
  extra line turns the table from a verdict into a trade.

## 7. "Knows when the archive has no answer" — the six arrive 170 lines late, and "knows" overstates

- **Quote:** "**One thing it is good at.** The reranker's score for its top result is the first number in this series that knows when the archive has no answer. Six of the golden set's questions are unanswerable on purpose, and here are the top scores:"
- **Location:** line 205, `## Step 6`.
- **Class:** prose. **Correction.**
- **Should say:** this is the first and only time the published text tells the reader the six exist, and
  it arrives after nine `/47` figures. Once edit 1 lands, rewrite this as a callback ("the six that have
  no answer, held out of every number so far"). Also soften "knows": the score *separates* them; nothing
  reads the score, so the pipeline does not know anything yet (see edit 10).

## 8. "9 of 37" and "all thirty-seven" — an unexplained third denominator

- **Quotes:** "a threshold just above the highest unanswerable would also refuse **9 of 37** answerable questions" (line 212) and "a threshold anywhere inside it refuses all six and keeps all thirty-seven" (line 221).
- **Location:** lines 212 and 221, `## Step 6`.
- **Class:** number. **Correction.**
- **Should say:** the article has 47 answerable questions; 37 appears twice with no derivation, so a
  careful reader cannot reconcile it. The likely reason is in the article already — "pinned units are
  never reranked" (lines 179–187) and the lookup "pins something for **10 of the 47 questions**"
  (line 235), so only 47 − 10 = 37 questions have a reranker score for their top result. If that is the
  reason, say it in a clause here; if it is not, the figure needs re-deriving before publication. See
  gap G2.

## 9. "A model that ranks worse can still calibrate better" — the new finding's home

- **Quote:** "Not a clean split — a threshold just above the highest unanswerable would also refuse **9 of 37** answerable questions — but a real signal, and a better one than the fused score it replaced, which at the same setting would refuse 13. **A model that ranks worse can still calibrate better.**"
- **Location:** line 212, `## Step 6`.
- **Class:** prose. **Addition.**
- **Should say:** this is the right attachment point for the calibration figure, and it makes the claim
  harder and quantified rather than comparative-by-anecdote: with BM25 over element units (`S1`), 4 of
  23 groundless answers scored above the median grounded one; with the cross-encoder in the chain
  (`S5`), 0 of 21 did. So the reranker's purchase is not only ordering but *knowing when it has nothing*
  — it does not stop serving four passages, but it stops being confident about the wrong four. Keep the
  existing "refuse 13" sentence, which is the same observation on the fused score and supports it.

## 10. "A decision the pipeline can actually make" — the sharpest misrepresentation

- **Quote:** "That is not a ranking signal; it is a decision the pipeline can actually make — and it is the decision Alpha got wrong four times out of six in Episode 3, by answering anyway."
- **Location:** line 221, `## Step 6`.
- **Class:** prose. **Correction.**
- **Should say:** as written, the contrast with Alpha implies Beta now gets it right. Beta does not:
  nothing in the pipeline declines, and on all six it serves four passages every time, exactly as Alpha
  did. The honest form is that the separation makes the decision *available* — a threshold in the
  0.869–0.915 gap would refuse all six — and that wiring it in is not done here. The Alpha comparison
  then still works, as "Alpha had no score to refuse on; Beta now has one and still answers".

## 11. Rule 10's prose asks for a measurement the episode's metric cannot make

- **Quote:** "Retrieval-augmented systems are badly calibrated in both directions: they refuse questions they could answer, and answer questions nobody can. **Abstention**, or *selective answering*, is a measurable property, so measure it. Keep unanswerable questions in the golden set, and record the score your last ranking stage gives their best result. A pipeline that can refuse is worth more than one that ranks slightly better, because a confident wrong answer costs more than a shrug."
- **Location:** line 226, after the `>[!tip] Rule 10 / Know when you don't know.` callout at lines 223–224.
- **Class:** prose. **Addition.**
- **Should say:** the paragraph says "so measure it" while the episode's headline metric structurally
  cannot. Name the counter here: `groundless@4`, questions whose top four holds no expected passage,
  over all 53 — so the six are scored rather than excluded — plus the calibration figure, how many
  groundless answers score above the median grounded one. Also worth one clause admitting the
  previous metric's defect: keeping the unanswerable questions in the set is not enough if the score
  drops them from its denominator, which is what happened here.

## 12. The end-to-end diagram has no declining path

- **Quote (figure + caption):** `![[RAGs to Riches Episode 5, one question end to end.png]]` — "_G-022 as the code actually runs it. The FTS5 query is what `matchQuery` produces for that question, the pinned address is what the lookup returns, and the four passages at the bottom are what Step 5 hands the reader — the answer first. The reranker is dashed because it only exists at Step 6, where on this laptop it lost._"
- **Location:** lines 230–231, end of `## Step 6`, introduced by "That is the whole of it, from a question to the four passages a reader gets:" (line 228).
- **Class:** diagram. **Correction** (redraw) **+ addition** (caption).
- **Should say:** the figure draws a pipeline whose only terminus is four passages, which is the visual
  form of the defect. Add a branch at the reranker box — the one stage that scores question against
  passage — with two outputs: the top four when the best score clears the threshold, and *no answer*
  when it does not, labelled with the 5090 gap (0.869 / 0.915) and drawn dashed/greyed like the reranker
  itself, since it is not implemented. The caption's "from a question to the four passages a reader
  gets" and "the four passages at the bottom are what Step 5 hands the reader" should acknowledge that
  it is always four, on all 53 questions, including the six with nothing to find.

## 13. The build diagram shows no scoring or decision stage at all

- **Quote (figure + caption):** `![[RAGs to Riches Episode 5, building the searches.png]]` — "_One archive, indexed twice. Both sides read the same units, which is what lets their results be fused at all: one unit, found two ways. …_"
- **Location:** lines 79–80, `## Step 5`.
- **Class:** diagram. **No change needed** (indexing-time figure).
- **Should say:** recorded for completeness — it is an index-build picture, so a refusal branch does not
  belong here. Same for `![[RAGs to Riches Episode 5, one unit all the way in.png]]` (lines 84–85). Of
  the three figures, only the one at line 230 needs the branch.

## 14. "10 of the 47 questions" — the lookup's denominator, and an open question about the six

- **Quote:** "It is also a narrow instrument. Across the frozen set it pins something for **10 of the 47 questions** — all eight identifier ones, two relation ones — and for none of the paraphrase, intent, single-fact or stale ones."
- **Location:** line 235, end of `## Step 6`.
- **Class:** number. **Correction** (small).
- **Should say:** "Across the frozen set" reads as all 53 while the figure is over 47. Make it "10 of the
  47 answerable", and state whether the lookup pins anything for any of the six — if it does, that is a
  path by which an unanswerable question acquires a passage no model ever scored, which matters to both
  the refusal branch and `groundless@4`. See gap G4.

## 15. The laptop summary table

- **Quote (table):** header "| Step | Hit@4 | All-evidence@4 | Stale in top four | Verdict |", rows "| 4. Episode 4's line | 32/47 | 26 | 0 | start |", "| 5. + dense, fused (RRF, k = 60) | 36/47 | 33 | 0 | **fail**: relation 6→4 |", "| 6. + MiniLM reranking | 32/47 | 29 | 0 | **fail**: Hit@4 36→32 |", under "On the laptop:".
- **Location:** lines 239–245, `## Where Episode 5 lands, so far`.
- **Class:** table. **Addition.**
- **Should say:** this is the table most likely to be screenshotted, so it needs the counter: a column
  `groundless@4` over 53 and a column for the confident count, with `S5`'s 21/53 and 0 on the step 6
  row. Retitle `Hit@4` as `Hit@4 (of 47)` so the two denominators cannot be confused in a table that now
  holds both.

## 16. The 5090 summary table — the episode's headline row

- **Quote (table):** header "| 5090 | Hit@4 | All-evidence@4 | Stale@4 | Verdict |", rows "| 4. Episode 4's line | 32/47 | 26 | 0 | start |", "| 5. + `Qwen3-Embedding-8B`, fused | **37/47** | **35** | 0 | **pass** |", "| 6. + `bge-reranker-v2-m3` | 36/47 | 33 | 0 | **fail**: Hit@4 37→36; all-evidence@4 35→33 |", "| 6. + `Qwen3-Reranker-8B` | **44/47** | **40** | 0 | **pass** |".
- **Location:** lines 249–254, `## Where Episode 5 lands, so far`, introduced by "And on the 5090, where the same two steps are run with models measured in billions of parameters rather than millions — 7.6 billion for the embedder against nomic's 137 million, and the same again for the reranker against MiniLM's 22:" (line 247).
- **Class:** table. **Addition** — **blocked on gap G1.**
- **Should say:** same two columns as edit 15. The figure supplied for Episode 5's own line (`E5-2`,
  Qwen3 embedder + Qwen3 reranker) is 46/53 grounded and 7/53 groundless, 0 of 7 confident — but this
  row publishes 44/47, which implies 44 grounded and 9 groundless. Those two cannot both go in the
  same table. Reconcile before editing (G1).

## 17. "Three things changed when the models got bigger" — a fourth thing changed

- **Quotes:** "Three things changed when the models got bigger." and "And step 6, with a reranker that is itself a language model, finds **44 of 47**, with **paraphrase 10/10 and single-fact 10/10**: every question written to avoid the passage's own words, found."
- **Location:** line 256, `## Where Episode 5 lands, so far`.
- **Class:** prose. **Addition** (and one correction).
- **Should say:** make it four things, the fourth being that the bigger reranker's scores separate the
  unanswerable from the answerable cleanly rather than merely hinting (the material at lines 214–221 is
  already in the article but is not counted among the things that changed). The correction: "every
  question written to avoid the passage's own words, found" is true of the paraphrase and single-fact
  strata and reads, at the end of a paragraph about bigger models, as *everything found*; add "— and the
  six with nothing to find are still served four passages each".

## 18. "One off the ceiling"

- **Quote:** "That 44 is one off the ceiling. Step 5 leaves an expected passage within reach for 45 of the 47, and the reranker converts all but one of them: G-029, which step 5 had at rank **2** and the reranker sent to 13."
- **Location:** line 258, `## Where Episode 5 lands, so far`.
- **Class:** number. **Correction** (clarifying).
- **Should say:** "45 of the 47" is correct as a recall ceiling but the passage never says what the other
  six do to the ceiling: nothing, because they have no expected passage. One parenthetical, consistent
  with edit 5.

## 19. "It passes" — passes a ratchet with no refusal clause

- **Quote:** "It passes. Under Episode 4's redirect it would not have, and on nothing it got wrong: **one** stale record reaching **one** top four — G-043, in all three 5090 runs:"
- **Location:** line 260, `## Where Episode 5 lands, so far`.
- **Class:** prose. **Addition.**
- **Should say:** once the ratchet has the fifth clause (edits 3 and 26), say whether the row passes it
  too — on the supplied figures it does, with 0 of its groundless answers confident, which is a
  stronger "it passes" than the current one and costs a clause.

## 20. The by-kind table silently drops a whole stratum

- **Quote (caption + header):** "By kind, with `Judged@4` beside it — because dense search returns passages nobody has judged, and an unjudged passage counts as wrong:" then "| | single fact | paraphrase | identifier | relation | stale | intent | Judged@4 |" with rows "4", "5, laptop", "6, laptop", "5, 5090", "6, 5090" (the last reading "| 6, 5090 | **10/10** | **10/10** | 8/8 | 5/7 | 7/7 | 4/5 | **0.55** |").
- **Location:** lines 275–283, `## Where Episode 5 lands, so far`.
- **Class:** table. **Correction.**
- **Should say:** the golden set has **seven** strata and this table shows six; `unanswerable` (6
  questions) is dropped without a word, and because every other column is `n/k` the table reads as a
  complete partition of the set — 10+10+8+7+7+5 = 47 is exactly the hidden denominator. Add an
  `unanswerable` column scored the only way it can be — passages served, or refusals made, so currently
  0/6 on every row — or, if a column is unwanted, add an explicit caption clause saying the six are
  scored separately and where. A column is better: it makes the omission structurally impossible to
  repeat.

## 21. The audit's "40 questions" is a third silent denominator

- **Quotes:** "Dense search had surfaced **272 question-and-passage pairs across 40 questions** that nobody had ever read" and "Of 272 candidates — every unjudged passage in every top four, across all three runs and all three steps — **one** was accepted".
- **Location:** lines 285–287, `## Where Episode 5 lands, so far`.
- **Class:** number. **Addition.**
- **Should say:** say whether those 40 questions are drawn from the 53 or from the 47. The point is not
  pedantry: on the six unanswerable questions *every* passage in the top four is unjudged and
  unjudgeable (there is no expected passage to match), and those twenty-four slots are precisely the
  groundless answers the new counter names. If round 3 excluded them, say so; if it included them, the
  "267 do not answer the question" figure deserves the observation that some of them *could not*.

## 22. The audit's conclusion is the qualitative form of the new counter

- **Quote:** "The honest way to say what the audit found is not that the numbers survived scrutiny, but this: **meaning-based search returns a great deal of material that looks like an answer to a machine, and when a person reads it, almost none of it is one.**"
- **Location:** line 291, `## Where Episode 5 lands, so far`.
- **Class:** prose. **Addition.**
- **Should say:** one sentence noting that `groundless@4` is this claim as a number, and the
  above-median-confidence figure is the part of it that costs a reader something.

## 23. "Same code, same golden set" — the metric does not cover the golden set

- **Quote:** "So the main line moves, and only on the 5090: Episode 4's step 4, plus fusion, plus `Qwen3-Reranker-8B`, at 44 of 47. On the laptop both steps go in the book with their numbers and the reason they lost … Same code, same golden set, same two steps: the size of the models is what decides whether they earn their place."
- **Location:** line 293, `## Where Episode 5 lands, so far`.
- **Class:** prose. **Correction.**
- **Should say:** "same golden set" is the sentence that most needs edit 1 to have landed. Qualify it:
  the same 53 questions, with the headline score reading 47 of them and the new counter reading all 53.
  As written, the episode's closing verdict is stated over a set the verdict does not fully measure.

## 24. "8 of 53" — the only 53 in the published text, unexplained

- **Quote:** "Episode 4's line is **identical** under the new rule — 32/47, all-evidence 26, stale 0, the same MRR, and not one cell of its by-kind table moves. The laptop tier is identical at all three steps. `RatchetTest` and `Episode5RatchetTest` pass without a number being touched. Only 8 of 53 questions shift anything at all, nearly all of it below rank 4."
- **Location:** line 323, `### The rule now`.
- **Class:** number. **Correction.**
- **Should say:** this sentence puts "/47" and "of 53" eight words apart with nothing to explain the
  difference — the clearest single symptom of the omission. It needs no change at all once edit 1 lands,
  and it is the sentence to re-read as the test of whether edit 1 worked.

## 25. The epilogue's keeping/dropping table

- **Quote (table):** "| 5090 run | keeping | dropping |", rows "| `nomic` | 35/47 · all-evidence 32 · stale@4 **1** | 35/47 · 32 · **0** |", "| `Qwen3-Embedding-8B` + `bge` | 36/47 · 33 · **1** | 36/47 · 33 · **0** |", "| `Qwen3-Embedding-8B` + `Qwen3-Reranker-8B` | 44/47 · 40 · **1** | **44/47 · 40 · 0** |", followed by "Identical Hit@4, identical all-evidence@4, identical counts in every stratum."
- **Location:** lines 327–333, `### The rule now`.
- **Class:** table. **Addition** (optional, and dependent on G5).
- **Should say:** the claim being made is that the rule change moves nothing but `stale@4`. Now that a
  fifth metric exists, that claim should cover it: either add `groundless@4` to each cell or add a
  clause to "identical counts in every stratum" saying the new counter is unchanged too. Do not assert
  it unmeasured (G5).

## 26. "What I did not do" — where the fifth ratchet clause belongs

- **Quote:** "Change the ratchet. The temptation was real: a rule that rejects a seven-question improvement over a single record looks broken from the inside. But the rules are absolute so that a step cannot pass by having the bar moved for it, and \"stale must not rise\" is the rule that found this. Loosening it because *this* result was inconvenient is the tuning the whole method exists to prevent — and the article would read identically either way, which is exactly the problem."
- **Location:** lines 354–358, `### What I did not do`.
- **Class:** prose (ratchet). **Addition.**
- **Should say:** this section is about not weakening the ratchet, which makes it the natural and honest
  place to say the ratchet gains a clause: *`groundless@4` must not rise, and no groundless answer may
  score above the median grounded one.* Adding a clause is the opposite of moving a bar, and the
  section's own logic ("the rules are absolute so that a step cannot pass by having the bar moved for
  it") supports saying so explicitly. Note too that the rule that caught the stale record, "stale must
  not rise", is the structural sibling of the new one: both score what the reader should not have been
  given.

## 27. The closing "an answer, just an expensive one"

- **Quote:** "On my Mac, meaning costs about as much as it buys, and the reranker that should have settled it makes things worse. Models in the billions of parameters rather than the millions, on hardware I had to go and buy, and the same two steps pass — which is an answer, just an expensive one."
- **Location:** line 388, `## Next: links and history`.
- **Class:** prose. **Addition.**
- **Should say:** "the reranker … makes things worse" is the laptop verdict the new figure most directly
  qualifies: on the laptop the cross-encoder loses four questions of `Hit@4` and takes
  confidently-groundless answers from 4 to 0. One clause here makes the episode's closing judgement
  match its own evidence.

## 28. Abstract and front-matter description (optional)

- **Quote (twice, identical):** "Embeddings and rerankers fail the ratchet on a laptop. On an RTX 5090, with bigger models, they don't."
- **Location:** line 9 (`description:` front matter) and line 15 (`>[!Abstract]`).
- **Class:** prose. **Addition** (optional).
- **Should say:** if the refusal finding becomes a named finding of the episode (and edits 9, 11 and 26
  make it one), the abstract is where a reader decides what the episode is about, and it currently
  promises only a ratchet verdict. A second clause — bigger models also buy the pipeline a score it
  could refuse on — would carry it.

## 29. Draft-status callout (housekeeping, deleted before publishing)

- **Quotes:** "**Rule 10 wording, pick one:** *Know when you don't know.* / *A score you can refuse on beats a rank you can't.* / *Measure the refusal, not just the answer.*" (line 30) and "**Rule 10 now has its evidence.** On the laptop MiniLM only hints at it (a threshold would refuse 9 of 37 answerable). On the 5090, `Qwen3-Reranker-8B` separates **cleanly on the frozen set**: unanswerable top out at 0.869, answerable start at 0.915, so any threshold in that gap refuses all six and keeps all thirty-seven." (line 31)
- **Location:** lines 30–31, inside `>[!todo] Draft status (delete before publishing)`.
- **Class:** prose. **Addition** (draft only).
- **Should say:** the third candidate wording, *Measure the refusal, not just the answer*, is now the one
  the evidence supports, and the note should record the new counter and the 4→0 figure so the draft
  history shows where they came from. Also record that line 22's "(53 questions, 47 answerable)" is the
  only statement of the split in the file and is deleted at publication — which is how the omission
  arose.

## 30. The held-out set is not in this episode

- **Quote:** none. The words "held-out", "heldout" and the ids `H-001`–`H-013` do not appear anywhere in the file.
- **Location:** n/a.
- **Class:** prose. **No change.**
- **Should say:** nothing. Recorded because it was asked for: this episode publishes no held-out number,
  so there is no denominator to correct. For future episodes, the denominator question does **not** bite
  there — `questions-heldout.jsonl` is 13 questions, 8 relation and 5 history, and **none** are
  unanswerable, so a held-out `Hit@4` of `n/13` is already over the whole set. `docs/design-history.md`
  (lines 127–131) is where the held-out scoring rule lives ("scored once, at an episode's end, never
  during a build") and it already states the 53/47 split the article omits.

---

# Counts and judgement

**Totals.** 30 numbered items: 15 prose, 7 table, 3 diagram, 7 number (counting edit 13 and edit 30 as
recorded-no-change). 8 corrections, 19 additions, 1 mixed (edit 12), 2 no-change.

**Statements with a 47 denominator** (published text only; the draft callout at lines 22–34 has eight
more): prose at lines 36, 164, 235, 256, 258, 293; tables at lines 117, 193, 243–245, 251–254, 329–331;
plus the implicit 47 in the by-kind table's column denominators at lines 279–283 and in "9 of 37" /
"thirty-seven" at lines 212 and 221. Nineteen `n/47` figures in total. Statements with a 53 denominator:
one, line 323.

**Tables needing a column or row:** five — lines 115–121, 191–196, 241–245, 249–254, 277–283 — plus one
optional, lines 327–331.

**Diagrams:** three, of which one needs redrawing (line 230, the end-to-end figure) with a refusal branch
at the reranker. The other two are index-build figures and are fine.

**Judgement: not a light edit.** Three of the items are structural rather than local.

1. The split is never stated in the published article, so edit 1 is load-bearing and everything else
   reads differently once it lands.
2. Five tables and the by-kind stratum table need new columns; the by-kind table in particular currently
   presents six strata as a complete partition of the set when the set has seven.
3. The material at lines 205–226 is nearly the right paragraph already — it has the six, the scores, the
   threshold, the gap and Rule 10 — but it claims a decision the pipeline never makes (edit 10) and it
   sits inside the step-6 narrative as an aside rather than as a result.

What I would do: edit 1 as a new paragraph in the recap; a short new subsection in
`## Where Episode 5 lands, so far` — somewhere after the 5090 table and before the by-kind table —
stating the defect in the old metric, defining `groundless@4` and the above-median-confidence figure,
giving the 4→0 contrast, and saying plainly that nothing declines yet; the fifth ratchet clause stated in
`### What I did not do`; and the remaining ~12 inline clauses. The one genuinely new claim — that the
cross-encoder's value includes knowing when it has nothing — has a natural home at line 212 and a natural
restatement at line 388, so it does not need a section of its own, but the metric correction does.

---

# Conflicts and gaps

**Settled 28 Oct from the run files. Read this before the list below.**

- **G1 — resolved.** `E5-1` is the configuration published as 44/47; `E5-2` is Episode 6's step 7 at
  46/47. No convention mismatch, no two-question gap. The published row's counter is **9/53
  groundless, 0 confident**.
- **G2 — resolved, the inference was right.** Exactly **10** questions have something pinned, so
  47 − 10 = **37** is the number with a reranker score on their top result. Say it in a clause.
- **G3 — resolved, and the 4→0 attribution was wrong.** The confident count across the chain is
  **4, 4, 4, 2, 0** for `S1`, `S2`, `S3`, `S4`, `S5`. So the identifier lookup and the lifecycle
  filter change nothing; **dense fusion halves it** (4→2) and **the cross-encoder closes it** (2→0).
  And only a big enough cross-encoder does: on the `nomic` + `bge-reranker` pair `S5` is still at 2,
  while on the Qwen3-8B pair it reaches 0. Do not write "the reranker takes it from 4 to 0" — two
  stages share the credit, and the second one is size-dependent.
- **G4 — resolved.** **None** of `G-045`–`G-050` has anything pinned, so all six do get a reranker
  score and a threshold applies to them cleanly. The refusal branch has no bypass to worry about.
- **G5 — resolved, and the identity holds.** Measured under both rules at both tiers:
  `E5-1` and `E5-1b` are **identical** on groundless@4 (9/53 on the Qwen3 pair, 18/53 on nomic) and
  on the confident count, as are `X1`/`X1b` and `H1`/`H1b`. The *only* cell that moves anywhere is
  `stale@4`, which is 0 under the dropping redirect and 1 under the keeping one — which is the
  epilogue's whole point. So edit 25 **can** be written as a claim: the new counter is unchanged by
  the redirect change, exactly as `Hit@4` and `all-evidence@4` are.
- **G6 — resolved. No audit round has ever judged a candidate for any of the six.** Checked in both
  committed files: `results/episode-4/golden-audit-2026-10.jsonl` (226 decisions over 26 questions)
  and `results/episode-5/golden-audit-round3.jsonl` (54 over 7). Neither contains a single row for
  `G-045`–`G-050`. That is consistent with Episode 4's documented pooling rule — "passages that could
  not change any score were not judged" — and for a question with no expected passage, no decision
  about a candidate can change any score. So "267 do not answer the question they were returned for"
  is a statement about answerable questions only, and should say so.

## RESOLVED 29 Oct — the round-3 tally, and how the file caught it

The discrepancy below is settled, and the article is corrected. **54 decisions were made, not 272.**

The deciding evidence is `qrels.txt`: every decision becomes a line there, a *no* included — which
Episode 3 states outright, "a 0 is a judgment too". All 192 *no*s from rounds 1 and 2 are present
as 0-lines, and all 46 from round 3. Had 267 *no* decisions been made, qrels would carry 267 of
them. It gained exactly 54 lines, 291 → 345, in the same commit (`ac6e6ba`) that added the file.

What is true, and what each claim rests on:

| claim | status |
|---|---|
| the pool spanned **40 questions** | **reproduced exactly** — `build_audit.py --all` against the pre-round-3 qrels (`git show 2b093ab:…/qrels.txt`) over the three 5090 runs reports 40 |
| the pool held **272** candidates | **not reproducible.** Closest is 263 (configs `X1,H1,E5-1`); 262 with other triples. It shifts with which configs are passed, so the article should quote no pool size |
| **54** judged, over 7 questions | the file, and qrels' 54 new lines |
| the 7 are `G-007`, `G-014`, `G-076`–`G-081` | the file — and they are exactly the questions the freeze changed: the six added, plus G-007 which was reworded and G-014 which was among the six fixed |
| 1 whole, **7** *partly*, 46 *no* | the file. The article said 4 *partly*; G-076 alone has four and G-080 supplies the other three, which is the likely origin of the error |
| `Judged@4` 0.73 / 0.55 etc. | untouched — computed from the 345-line qrels, so consistent with 54 |

**Commit `ac6e6ba`'s message carries the wrong tally too.** It is pushed, so it stays; the article
is the thing that gets corrected.

The lesson worth keeping: the prose was a hand tally and the evidence file was generated. When they
disagree, the generated artefact wins — and here a third artefact, qrels, was able to adjudicate
because it records refusals as well as acceptances.

## The original statement of the discrepancy, 28 Oct

**Episode 5's round-3 figures do not reconcile with its own evidence file.** The article (line 27)
says "272 unjudged pairs over 40 questions … One accepted (`031-init-tools/spec.html#scenarios`,
G-076, whole), four *partly*, 267 *no*." The committed file holds **54 decisions over 7 questions**
— `G-007`, `G-014`, `G-076`, `G-078`, `G-079`, `G-080`, `G-081` — being **1 whole, 7 partly, 46 no**.

The `whole` matches. Nothing else does: 7 partly against 4, 46 no against 267, 7 questions against 40.

For contrast, Episode 4's numbers reconcile exactly: 226 rows, 12 whole + 2 standin + 20 partly +
192 no, and 211 of them from round 1 — every figure the article prints.

The likeliest reading is that 272 over 40 was the **pool presented** and the file holds only the
decisions that could move a score, but the article presents the 272/4/267 split as decisions made,
and the partly count disagrees even on that reading. **Do not paste either number over the other.**
Either the full round-3 decision file is missing from the repository, or the article's split needs
re-deriving from whatever produced it. Nothing is published, so this is a fix rather than an erratum.

Also useful, and it removes most of the uncertainty the table edits worried about: with six
unanswerable questions and nothing declining, **groundless@4 over 53 is just `53 − Hit@4`**. Every
groundless row below is therefore exact arithmetic from a published Hit@4, not a new measurement. The
*confident* counts are the only figures that need a run.

## The original statement of the gaps, as written 27 Oct

These must be settled before the table edits are applied; I could not settle them from the article.

- **G1 — `E5-2`'s 46 does not reconcile with the published 44/47.** The supplied figure is 46/53
  grounded and 7/53 groundless (1 missed + 6 unanswerable). The article's headline row is
  `Hit@4` **44/47** (line 254, repeated at lines 256, 258, 289, 293, 331), which implies 44 grounded
  and 9 groundless — a two-question gap. Candidate explanations: `E5-2` is a different configuration
  from the row published at line 254; or the grounded count deduplicates passages by address while the
  article's scorer deliberately counts items, duplicates and all — which is precisely the known
  disagreement the Ship's Log documents ("our Hit@4 is 32 while `ir_measures` says `Success@4` is 33",
  line 368). If the second is the cause, the counter is computed on a different convention from every
  other number in the article and must be recomputed or captioned before it is published beside them.
- **G2 — where 37 comes from.** The 47 − 10 pinned-questions reading (lines 179–187 and 235) fits the
  arithmetic exactly but is my inference; the article never derives it and I did not verify it against
  the run files.
- **G3 — the 4→0 contrast spans more than the reranker.** `S1` is element units with BM25 only and
  scores 30/53 grounded, which matches no row the article publishes (Episode 4's step 4 is 32/47), so
  `S1` sits earlier in the chain than the article's step 4. Between `S1` and `S5` lie the identifier
  lookup, the lifecycle filter, the redirect and the fusion as well as the cross-encoder. Attributing
  4→0 to the reranker alone therefore overclaims unless the confident counts for the intervening
  configurations (`S2`–`S4`) show the drop happening at `S5`. Either obtain those, or phrase the finding
  as "by the time the cross-encoder is in the chain" rather than "the reranker takes it from 4 to 0".
  `S5` itself is internally consistent with the article: 32 grounded + 21 groundless = 53, and 32/47
  is exactly the published laptop step 6.
- **G4 — does the identifier lookup pin anything on `G-045`–`G-050`?** Line 235 gives the lookup's reach
  only over the 47. If it pins for any of the six, an unanswerable question can receive a passage that
  no model scored, which bears on both the refusal branch and the counter.
- **G5 — is `groundless@4` unchanged by the dropping redirect?** The epilogue claims identity on
  `Hit@4`, `all-evidence@4` and every stratum (line 333). I have no measurement of the new counter under
  the two rules, so edit 25 cannot be written as a claim yet.
- **G6 — the audit's 40 questions.** Whether round 3's pool of 272 pairs over 40 questions included the
  six unanswerable questions' top fours is not stated (lines 27, 285, 287), and it changes what "267 do
  not answer the question they were returned for" means.
