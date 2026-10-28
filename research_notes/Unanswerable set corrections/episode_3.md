# Unanswerable set corrections — Episode 3, Knowledge Base

Audited file: `/Users/briancorbin/Documents/Brain/Library/03 Resources/RAGs to Riches with Spring AI, Episode 3, Knowledge Base.md`
(520 lines, ~5,500 words, read in full, not edited). Figures for `EP3` taken from
`/Users/briancorbin/Code/rags/rags-spring-ai/research_notes/Refusal as a first-class metric/groundless_at_4.md`.

## Key question 1 — Where does this episode misrepresent or silently omit the unanswerable half?

### Takeaway

Episode 3 is more honest than the brief assumed: it never claims completeness, it names the six
unanswerable questions in its kinds table, and it says in plain words that its two retrieval numbers
are computed "over the answerable questions". The defect here is not a false claim but a missing
dimension — every headline, table, figure and ceiling number in "The baseline" silently narrows the
universe from 50 questions to 44, the pipeline diagram has no branch on which anything declines, and
the one sentence that gets closest to the truth ("Beta always has something real to quote") is left
as an aside instead of being measured. Two further things are genuinely wrong rather than merely
absent: the summary callout's "92 of 95 of Alpha's claims passed every check" counts claims made on
questions that had no right answer, and the kinds table says the six questions test "Saying 'Not in
the archive.'" when nothing in this episode's pipeline can decline at all.

### IMPORTANT — denominator warning for whoever applies these edits

The brief's "47" is **not** this episode's denominator and must not be pasted over it.

- **This article's set**: 50 questions — 10 single fact, 10 paraphrase, 8 identifier, 8 relation,
  8 stale, 6 unanswerable. **44 answerable + 6 unanswerable.** Every published number in the article
  (20%, 9 of 44, 17 of 44, 39 of 44, 38 of 44, 92 of 95) is over that laptop run.
  Source: article lines 35, 356, 358-365, 403-407, 411-420, 424-431.
- **The frozen set the series uses later**: 47 questions — the same 50 minus three that the blind
  audit dropped (G-005, G-035, G-040 are absent from `src/main/resources/golden/questions.jsonl`).
  **41 answerable + 6 unanswerable.** This is the denominator of the new `EP3` rescore:
  10/47 grounded, 37/47 groundless (31 missed + 6 unanswerable), 24 of 37 confident.
- **The current main set**: 53 questions — the frozen 47 plus G-076..G-081 (five `intent`, one
  more). **47 answerable + 6 unanswerable.** This is the "47/47" of Episode 6 and the 47/53
  grounded in the contrast row. Verified by reading `questions.jsonl`: strata are single-fact 10,
  paraphrase 10, identifier 8, relation 7, stale 7, intent 5, unanswerable 6; exactly the six
  G-045..G-050 carry an empty `expected` list.

So "47" means three different things across the series, and in Episode 3 it means *the frozen set*,
not *the answerable questions*. Every edit below that introduces an `EP3` figure must label it as a
rescore of the same configuration over the later 47-question frozen set on the 5090 tier, kept
visibly separate from the article's own 44-question laptop numbers. Per the project rule that no
published number moves, nothing in the existing tables should be restated — the refusal dimension is
added beside them.

## Key question 2 — The edit list

Classification key: **correction** = currently says something untrue or misleading;
**addition** = currently silent on something it should say.

---

**1. The promise bullet undercounts the set**
Quote: `- a golden set of 50 questions, and the baseline the next episode has to beat`
Location: line 35, bullet list under "In this episode we'll build the simplest version…"
Type: **number** — **addition**
Should say: give the split, not just the total — "a golden set of 50 questions, six of which have no
answer in the archive, and the baseline the next episode has to beat". The reader meets the set here
first and should learn at once that part of it is about declining.

---

**2. The advisor's threshold of 0 is described as naive but not as un-refusable**
Quote: `The defaults are four chunks (the top k) and a similarity threshold of 0,
`SIMILARITY_THRESHOLD_ACCEPT_ALL`: the four closest chunks always come back, however far they are
from the question. *That's purposefully naive* — we want to see how far the defaults get us.`
Location: line 227, under "## Beta searches"
Type: **prose** — **addition**
Should say: keep it and add the consequence — with a threshold of 0 there is no input on which
retrieval returns nothing, so the pipeline cannot decline by construction, and the six unanswerable
questions will each be served four confident passages. This is the mechanical cause of everything in
edits 10-17 and the natural place to plant it.

---

**3. The pipeline figure has no refusal branch**
Figure: `![[RAGs to Riches Episode 3, pipeline.png]]`, captioned "Here's the whole pipeline. The top
row runs once, the bottom row runs for every question".
Location: line 88 (image), line 86 (caption), under "## Why not just paste the whole thing in?"
Type: **diagram** — **addition**
Should say: the drawn flow is a single unbroken line — `SimpleVectorStore → Beta (QuestionAnswerAdvisor)
→ QuoteResolver → Alpha → CitationChecker` — with the arrow labelled "top 4 chunks, as text only: no
addresses" and no branch anywhere. A refusal branch would have to be drawn on that store→Beta arrow:
a decision point that in this episode has only one exit, annotated something like "no branch here:
threshold 0, so four chunks always come back". The figure also has no output path for Alpha's "Not
in the archive." even though the prompt (line 333) mandates it; the honest version shows the decline
exit existing only at Alpha, after the passages have already been served.

---

**4. The worked exchanges show two answerable questions and no unanswerable one**
Quote: `Two real exchanges from the golden run show the range.`
Location: line 337, under "## Alpha answers from the evidence"
Type: **prose** — **addition**
Should say: the two cases shown are a success and a false decline, both on answerable questions, so
the section's "range" never includes the case the six questions exist for. Add a third exchange: an
unanswerable question (G-050 or G-046 are the strongest, scoring top-1 +1.213 and +0.977) where four
passages come back, Beta quotes them correctly, every check passes, and the answer is about
something nobody asked.

---

**5. Alpha's "Not in the archive." instruction — correct, build on**
Quote: `If the quotations do not answer the question, or there are none, reply with exactly one
claim, "Not in the archive.", with sources []. Do not guess.`
Location: lines 332-333, prompt block under "## Alpha answers from the evidence"
Type: **prose** — **no change, anchor**
Should say: leave exactly as is. This is the only place in the episode where declining is actually
built, and it sits at the very end of the pipeline, after retrieval has already committed. Edit 2
should forward-reference it: the model is asked to decline on evidence that was selected so as never
to be empty.

---

**6. The kinds table says the six test something the episode cannot do**
Table: "The golden set" kinds table. Columns: `Kind | Questions | Example | What it tests`.
Row quoted: `| Unanswerable | 6 | Is there a JetBrains plugin for the lifecycle canvas? | Saying "Not
in the archive." |`
Location: lines 358-365, under "## The golden set" (introduced at line 356 by `This one has 50
questions in six kinds, because each kind fails differently`)
Type: **table** — **correction**
Should say: the denominator is stated (the Questions column sums to 50) and the row exists, which is
good — but "What it tests" is wrong in this episode. Nothing is tested about saying "Not in the
archive." when retrieval always returns four passages. Change that cell to something like "Whether
anything in the pipeline can decline — in this episode, nothing can". A caption line under the table
should state the split in words: 44 answerable, 6 unanswerable.

---

**7. "Each question records the addresses of the elements that answer it" — silent on the six**
Quote: `Each question records the addresses of the elements that answer it, a one-line reference
answer, and the commit it was checked against.`
Location: line 367, under "## The golden set"
Type: **prose** — **addition**
Should say: add the exception — the six unanswerable questions record no addresses at all (their
`expected` list is empty), and their reference answer is a decline. That empty list is precisely what
makes them unscoreable by Hit@4 and what makes `groundless@4` true for them by construction.

---

**8. The definition of the two retrieval numbers — correct, and the single best anchor**
Quote: `Two numbers summarize retrieval, both over the answerable questions and counting full hits
only.`
Location: line 375, under "## The golden set"
Type: **prose** — **no change, anchor; then addition**
Should say: keep this sentence verbatim — it is the one place the article states the exclusion
honestly. Immediately after it, add the third number: `groundless@4`, the share of questions whose
top four holds no expected passage, computed over **all** the questions, with the note that an
unanswerable question is groundless by definition because there is nothing it could correctly
serve, and that it needs no threshold, which is why it can be computed backwards over runs already
stored. Also add the companion count: how many groundless answers score above the median grounded
one.

---

**9. "The run also records abstentions" — correct, build on**
Quote: `The run also records abstentions, answers resting on a stale record, how many quotations
traced, Alpha's claims by verdict, prompt tokens and time.`
Location: line 377, under "## The golden set"
Type: **prose** — **addition**
Should say: keep, and add that abstention is recorded but never scored — it is a field in the row,
not a term in the headline. That is the gap `groundless@4` fills.

---

**10. The four-number summary narrows the universe without saying so**
Quote (callout `>[!summary] The baseline in four numbers`):
`- **20%** of answerable questions found the expected passage in the top four chunks`
`- **17 of 44** answers were right, judged by hand, and 3 more partly right`
`- **92 of 95** of Alpha's claims passed every check`
`- **84%** of Beta's quoting claims traced word for word`
Location: lines 403-407, under "## The baseline"
Type: **number** — **correction** (third bullet) and **addition** (the callout as a whole)
Should say: bullets one and two name their denominator and are fine. Bullet three, "92 of 95 of
Alpha's claims passed every check", is the misleading one: those 95 claims include claims made on
the six questions that had no right answer, so the pass rate counts confidently-wrong answers as
passes. Qualify it ("92 of 95 claims passed every check — including every claim made on the six
questions the archive cannot answer") and add a fifth bullet giving the refusal dimension: on 6 of
the 50 questions there was nothing correct to retrieve, and four passages came back on every one.

---

**11. The retrieval table's "All answerable" row is the headline denominator**
Table: "**Retrieval**, scored by the program: did the expected element come back?"
Columns: `Kind | Questions | Hit@4 | Partial@4 | MRR`. Total row quoted:
`| All answerable | 44 | 20% | 25% | 0.14 |`
Location: lines 411-420, under "## The baseline"
Type: **table** — **addition**
Should say: the denominator is honestly labelled "All answerable", but there is no row for the six
and no row totalling 50, so a reader scanning the table sees the whole evaluation as 44 questions.
Add an `Unanswerable | 6 | — | — | —` row (dashes, not zeros: the metric is undefined, not failed)
and an `All questions | 50` row carrying `groundless@4` rather than Hit@4. Add an `groundless@4`
column, or a second small table beside it, so the six are visible in the same frame as the 44.

---

**12. The answers table has the same 44-row shape**
Table: "**Answers**, labelled by hand against each question's reference answer and source".
Columns: `Kind | Questions | Correct | Partly | Wrong | Said "not in the archive"`. Total row quoted:
`| All answerable | 44 | 17 | 3 | 21 | 3 |`
Location: lines 424-431, under "## The baseline"
Type: **table** — **addition**
Should say: this table already has a `Said "not in the archive"` column, which is where the six
belong. Add an `Unanswerable | 6` row with its hand labels (2 correct declines, 3 changed the
subject, 1 accepted a false premise — the counts already given in the bullet at line 435) and an
`All questions | 50` total, so the hand-labelled picture covers the set rather than the answerable
part of it.

---

**13. The unanswerable bullet — correct, the best existing material, but only half the story**
Quote: `- **Unanswerable questions (6):** Alpha declined correctly, in its own words, twice. Three
times it changed the subject: asked whether spectastic can push its tasks to Jira, it explained how
the task list is generated, and never said there is no Jira integration. And once it accepted a
false premise: asked which compliance frameworks, such as SOC 2, the enterprise profile maps its
controls to, it said the profile does map them, which nothing in the archive says.`
Location: line 435, under "## The baseline", in the "And the rest:" list
Type: **prose** — **addition**
Should say: this is the honest core to build on — it should be promoted out of an afterthought list
into its own short subsection, because it is a 2-in-6 failure rate on the behaviour the episode
claims to want. What it omits is retrieval: it describes only what Alpha said, never that Beta
served four passages on all six, nor that some of those passages scored *higher* than passages for
questions the system got right. Add the `EP3` measurements here (see edit 17).

---

**14. The ceiling number is a ceiling for the answerable half only**
Quote: `Rebuilding the same 2,147 chunks outside the app, 38 of the 44 expected elements sit whole
in some chunk, so a perfect ranking would score 86%.`
Location: line 441, under "### It finds the right neighborhood, not the right passage"
Type: **number** — **addition**
Should say: the denominator is stated, so this is not wrong, but "a perfect ranking would score 86%"
invites the reader to treat 86% as the system's ceiling. Add a clause: a perfect *ranking* still
serves four passages on the six questions that have no answer, so perfect ranking is not perfect
behaviour — the ceiling on the full 50 needs a decline the pipeline cannot make.

---

**15. The neighborhood figure's universe is 44, and it says so**
Figure: `![[RAGs to Riches Episode 3, neighborhood.png]]` — title "It finds the right neighborhood,
not the right passage"; subtitle "Where the best of the top four chunks landed, for each of the 44
answerable questions"; nested bands 5 / 9 / 21 / 9 with "a hit: 9 of 44, Hit@4 = 20%"; footnote "39
of 44 questions reached the right spec."
Location: line 445 (image) and line 447 (`Beta found the right spec for 39 of 44 questions.`)
Type: **diagram** — **addition**
Should say: the figure states its denominator in its own subtitle, which is good. It needs a sixth
element: the 6 unanswerable questions, which belong *outside* all four nested bands with a label
such as "6 unanswerable: four passages served, none of them correct by definition". As drawn, the
outermost band is "Another spec entirely (5)" and a reader infers the chart covers the whole set.

---

**16. "The score undersells the answers" cuts only one way in the text**
Quote: `**The score undersells the answers.** spectastic says the same thing in more than one place,
so Alpha often answered correctly from a sibling page the scorer counts as a miss: 12 of the 17
correct answers came from questions with no hit at all.`
Location: line 453, under "### It finds the right neighborhood, not the right passage"
Type: **prose** — **addition**
Should say: the paragraph does concede "it cuts both ways" with one example, but the larger
overstatement is unmentioned: the score also *oversells* by scoring nothing at all on the six, where
the system is not merely unscored but actively wrong. One sentence closing the loop.

---

**17. "Beta always has something real to quote" — the exact place the new counter belongs**
Quote: `With a threshold of 0, Beta always has something real to quote. So:` followed by the pull
quote `> A real quotation, correctly cited, about the wrong thing, passes every check in this
episode.`
Location: lines 467-471, under "### The checks prove where an answer came from, not that it is right"
Type: **prose** — **addition** (the single highest-value one in the article)
Should say: this is the article's own thesis stated qualitatively, and the new counter is its
quantity. Add, in a clearly labelled retrospective box so no published number appears to move:
rescoring this same naive configuration over the later frozen 47-question set (on the 5090 tier)
gives 10 of 47 grounded and **37 of 47 groundless** — 31 ordinary misses plus all 6 unanswerable
questions — and **24 of those 37 scored above the median of the answers it got right**. So the naive
baseline was not merely poor at finding: it was confidently wrong about roughly two thirds of what
it got wrong. For contrast, note the series' later line reaches 47/53 grounded, 6/53 groundless,
and **0 of 6** above the median correct answer. Box must state the denominator shift explicitly
(frozen set, 47 = 41 answerable + 6 unanswerable, 5090 tier) so it is not read against the
44-question laptop tables above it.

---

**18. The checks table's keyword row — correct, build on**
Table: "The checks didn't fail. They answered a different question:".
Columns: `Check | What it proves | What it doesn't`. Row quoted:
`| Keyword checks | The exact phrase appeared | Declines in Alpha's own words (missed 2), stale
answers (caught 1 of 3) |`
Location: lines 461-465, under "### The checks prove where an answer came from, not that it is right"
Type: **table** — **addition**
Should say: keep the row; it already admits the decline detector missed two. Add a fourth row for
retrieval itself — `Hit@4 | The expected passage was in the top four | Whether there was an expected
passage at all (6 questions have none)` — which makes the metric's blind spot a line in the table
that already catalogues blind spots.

---

**19. The false-decline paragraph — correct, build on**
Quote: `Strictness has a price, too. Three answerable questions got "Not in the archive." because
every quotation Beta offered was rejected`
Location: line 475, under "### The checks prove where an answer came from, not that it is right"
Type: **prose** — **no change, anchor**
Should say: leave as is. It is the mirror image of the defect — declining when it should answer —
and pairing it with the new figure (answering confidently when it should decline) gives the section
both error directions. One connecting sentence is enough.

---

**20. "The series' first fitness function" implies the baseline measures the whole job**
Quote: `These numbers are the series' first fitness function. Before the next episode leans on them,
the questions get checked too, by hand and blind, and then frozen. After that, every change has to
beat this baseline on them.`
Location: line 477, end of "### The checks prove where an answer came from, not that it is right"
Type: **prose** — **correction**
Should say: as written, "every change has to beat this baseline" promises a complete fitness
function while the baseline scores only the answerable 44. Say that the fitness function as it
stands rewards finding and is blind to declining, name `groundless@4` as the term added later to
close that, and — this is also the natural place to foreshadow — note that the freeze is what turns
50 questions into the 47 the rescore uses.

---

**21. "Next: a better Beta" lists four fixes, none of which is declining**
Quote: `- **Chunks that follow the document's structure**, one requirement at a time` /
`- **Keyword search**, for the exact words and ids that embeddings blur` / `- **A lookup for ids**
like FR-001` / `- **A filter** that knows a withdrawn record when it sees one` and
`Each has to beat the numbers above on the same questions, or it doesn't stay.`
Location: lines 491-498, under "## Next: a better Beta"
Type: **prose** — **addition**
Should say: all four improvements make retrieval find more; none makes it return less. Add a fifth
line naming the dimension the episode could not measure — knowing when it has nothing — and say
plainly that none of the four fixes addresses it, so the confident-wrongness number is the one to
watch across the episodes that follow.

---

**22. The abstract-level promise is already correct — build on, do not rewrite**
Quote: `- Alpha answering only from those traced quotations, or saying "Not in the archive."`
Location: line 34, bullet list in the introduction
Type: **prose** — **no change, anchor**
Should say: leave as is. Together with edit 1 (the same bullet list) it lets the introduction set up
both halves of the set in two lines.

---

## Key question 3 — Light edit or structural rewrite?

### Takeaway

A **light-to-moderate edit everywhere except "## The baseline", which needs one new subsection**.

Counts: **4 corrections** (6, 10, 20, and the kinds-table "what it tests" cell — plus edit 6 doubles
as a caption addition) and **15 additions**; **3 anchors** to leave untouched and build around
(5, 19, 22), with 8, 9, 13 and 18 being existing honest material that gains a sentence or a row.

Why not a rewrite: the episode never claims completeness, never says "47", never says the set is
closed, and explicitly labels its retrieval numbers "over the answerable questions". Its structure
is already the right shape — it has a kinds table with an Unanswerable row, an answers table with a
"Said 'not in the archive'" column, a bullet reporting the six, and a pull quote stating the defect
in words. Nothing needs demolishing.

Why not merely cosmetic: the six are reported only as an afterthought bullet under "And the rest",
and the one quantity that would make the episode's own thesis land — 37 of 47 groundless, 24 of
them confident — does not exist anywhere in the text. The right surgery is to promote the
unanswerable material out of the "And the rest" list into a short subsection of "## The baseline"
sitting beside the two tables, with the retrospective `EP3` box (edit 17) placed in "### The checks
prove where an answer came from, not that it is right", directly under the pull quote it quantifies.
Two figures (pipeline, neighborhood) need redrawing; neither needs a new design, only one more
element each.

### Gaps

- The `EP3` rescore exists only as a row in
  `research_notes/Refusal as a first-class metric/groundless_at_4.md`; I did not locate a stored
  per-question run file for it, so the 31/6 split of the 37 is taken from that note, not recomputed.
- I could not verify from the repository which three questions the blind audit dropped beyond
  observing that G-005, G-035 and G-040 are absent from the current `questions.jsonl`; if the editor
  wants to name them in the article, that needs checking against the freeze commit.
- Whether the six unanswerable questions were in the Episode 3 run at identical wording to today's
  G-045..G-050 is unverified; the JetBrains-plugin example in the kinds table matches, and the Jira
  and SOC 2 cases described at line 435 are consistent with the current file, but I did not diff
  question text against the episode-3 branch.
