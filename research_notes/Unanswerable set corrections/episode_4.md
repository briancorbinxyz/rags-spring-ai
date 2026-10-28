# Episode 4 — where the unanswerable half is misrepresented or omitted

Target file: `/Users/briancorbin/Documents/Brain/Library/03 Resources/RAGs to Riches with Spring AI, Episode 4, Knowledge Base.md`
(561 lines, 5,697 words). Line numbers below are the file as it stands today. Quotes are exact.

## Headline finding

The word **"unanswerable" never appears in this episode**. Nor does "decline", "refuse",
"abstain", "not covered", or "Not in the archive". The six unanswerable questions are named
exactly once, obliquely, in a subordinate clause on line 135 ("47 of them answerable from the
archive"), and never again. The stratum table at lines 456–466 lists six strata summing to 47 and
omits `unanswerable` entirely — even though `src/main/resources/golden/questions.jsonl` carries it
as a seventh stratum of 6, and `src/main/resources/golden/README.md` already documents it
("| unanswerable | 6 | The right answer is \"Not in the archive.\" |").

This is the episode in which the set is built, so every later episode inherits the omission.
Episode 5 is the first to discuss abstention at all (its Rule 10, "Know when you don't know",
lines 205–226 of that file).

Corroborating facts checked in the repo (not asserted by the article):

- `questions.jsonl` has 53 entries; `Counter(stratum)` = single-fact 10, paraphrase 10,
  identifier 8, relation 7, stale 7, **unanswerable 6**, intent 5.
- G-045 to G-050 all carry `"expected": []`, `"core": "Declines: not covered."`, no `stale` key.
- `qrels.txt`: **zero** lines for G-045…G-050 (`grep -cE "^G-0(4[5-9]|50) "` = 0). The repo README
  says so outright: "Unanswerable questions have no lines."
- Arithmetic the episode never states: the original fifty were 44 answerable + 6 unanswerable;
  three answerable dropped, six answerable added, giving 47 + 6 = 53. The six unanswerable were
  in version 1 and passed through the audit untouched.

## Constraint for whoever applies these edits

**Do not add a numbered Rule.** Episode 4 introduces Rules 5–8; Episode 5 publishes Rule 9
(keywords as baseline) and Rule 10 (*Know when you don't know* — the refusal rule). A new numbered
rule here would move published numbers. Episode 4 should set Rule 10 up, not pre-empt it.

---

# The edit list

### 1. Episode framing — "the golden set itself"
- **Quote:** "But a measurement is only as good as the thing doing the measuring, so before we let the golden set judge anything else, let's check whether the golden set itself is right." / "Spoiler: it isn't, quite. So this episode has two halves:"
- **Location:** lines 32–37, before `## Building an Evaluation Framework`
- **Kind:** prose — **addition**
- **What it should say:** The audit checked whether the *expected passages* were right. It never
  checked whether the *denominator* was right. Add one sentence here flagging that the set has a
  second half the scoring ignores, so the reader meets the problem at the top rather than finding
  the missing six by subtracting 47 from 53 on line 135.

### 2. The set's construction — the single worst sentence
- **Quote:** "Fifty questions, one or two expected addresses each."
- **Location:** line 43, `## Building an Evaluation Framework`, first paragraph
- **Kind:** prose — **correction**
- **What it should say:** Factually wrong for six of the fifty: G-045 to G-050 have `expected: []`.
  Rewrite as forty-four questions with one or two expected addresses each, plus six written with
  no expected address at all, because the archive does not answer them. This is the first place a
  reader could learn the set has two halves and currently the place where they are told it doesn't.

### 3. Rule 5's justification
- **Quote:** "**Written-down answers turn every change into a measurement**, and expected addresses let you measure the search apart from the answer: did the right passage come back, before you ask whether the model used it well."
- **Location:** line 48, under Rule 5 ("Know the answers before you ask")
- **Kind:** prose — **addition**
- **What it should say:** Rule 5 already covers the six — their written-down answer is "Not in the
  archive." Add a clause saying so: for six questions the written-down answer is that there is no
  passage, and the measurement is whether the search admits it. Good hook, needs one clause.

### 4. The `expected` field gloss
- **Quote:** "What the frameworks usually leave out that we'll be using is `expected`: where the answer lives. That's what lets us score the search before the model says a word."
- **Location:** line 71, after the G-036 `questions.jsonl` sample
- **Kind:** prose — **correction**
- **What it should say:** Add that `expected` is empty for six questions, where `core` reads
  "Declines: not covered." Consider showing G-045 as a second, three-line sample next to G-036 —
  it is the cheapest possible fix and it makes the second half concrete:
  `{"id":"G-045","question":"Is there a JetBrains plugin …?","stratum":"unanswerable","expected":[],"core":"Declines: not covered."}`

### 5. NEW SUBSECTION — the six with no answer
- **Anchor quote (the paragraph to build it around):** "Our frozen set has 53 questions, 47 of them answerable from the archive, and every number from here on is measured on it."
- **Location:** insert as a `###` subsection immediately after line 135, inside `## Building an Evaluation Framework`
- **Kind:** prose — **addition** (the substantive new section)
- **What it should say:** The six, by id and question, and why they exist: to test whether the
  system can say no. How they were written (adjacent to real features — G-045 sits next to spec
  020's VS Code extension — and checked by grep to confirm zero relevant hits; the `note` fields in
  `questions.jsonl` record exactly that). How they are scored: no `expected`, so no `stale`
  entries, so no judgments in `qrels.txt`, so **no metric in this episode can see them**; they are
  scored `answered=None` and dropped from the denominator. And the honest admission: on all six,
  every step of the main line returns four confident passages and nothing in the pipeline declines.

### 6. Audit pool rule — "Every miss"
- **Quote:** "- **Every miss, not just the interesting ones.** For every question a search misses, every passage it returns instead goes into a list."
- **Location:** line 81, the even-handedness bullets
- **Kind:** prose — **correction**
- **What it should say:** As written this claims the pool covers every miss. It does not: the six
  unanswerable questions miss by construction on every run, and their passages were never pooled
  (the repo README: "Passages that could not change any score were not judged"). Add the exception
  explicitly — every miss *on an answerable question*, and say why the other six were skipped.

### 7. Audit pool rule — "Blind"
- **Quote:** "- **Blind.** Each passage sits next to the question, the reference answer and the expected passage, and nothing else."
- **Location:** line 83
- **Kind:** prose — **correction**
- **What it should say:** There is no expected passage to sit beside for the six, so the review
  page as built could not present them. Note that the judging form assumed every question had a
  right answer somewhere — which is why the six were never looked at, not an oversight of nerve.

### 8. The blind-audit figure
- **Figure:** `![[RAGs to Riches Episode 4, blind audit.png]]`
- **Caption quote:** "_The review page. G-036 has two expected passages, so a candidate can stand in for either one. Nothing on the page says which search found it._"
- **Location:** lines 87–88
- **Kind:** diagram — **addition**
- **What it should say:** No change to the image. Add a sentence to the caption: the page always
  shows an expected passage to compare against, so no candidate from an unanswerable question was
  ever shown on it. (This is also the only figure in the episode — see item 18 for the missing
  pipeline diagram.)

### 9. Audit decisions table
- **Caption quote:** "Here's how the 226 that decide this episode come out:"
- **Table header:** `| Decision | Passages | What it does to the score |` with rows "Answers the question | 12", "Stands in for one expected passage | 2", "Partly, not enough | 20", "Doesn't answer it | 192"
- **Location:** lines 90–97
- **Kind:** table — **addition**
- **What it should say:** The numbers are right; the scope is unstated. Add a line under the table:
  all 226 came from the 47 answerable questions; none came from the six, which have no expected
  passage to judge a candidate against. Optionally a fifth row is *not* wanted — the point is the
  absent fifth decision ("nothing here answers it"), which the form had no key for. Say that in a
  sentence instead.

### 10. The hand sign-off bullets
- **Quote:** "- **Three dropped**, and their ids retired." / "- **Six fixed.**" / "- **Six added.** One replaces G-005. Five are a new kind: **intent**, why a feature exists at all."
- **Location:** lines 103–105
- **Kind:** prose — **addition**
- **What it should say:** The arithmetic is 50 − 3 + 6 = 53 and the reader is left to discover the
  split elsewhere. Add a closing line stating the composition after sign-off: 47 answerable and 6
  unanswerable, the six unchanged since version 1. Note the "new kind" phrasing on line 105 implies
  *intent* is the only new stratum; `unanswerable` is simply never introduced as a kind at all.

### 11. Rule 6's explanation
- **Quote:** "A retrieval test is a list of questions and the passages that answer them, written by someone who read one passage and stopped."
- **Location:** line 112, under Rule 6 ("Test the tests")
- **Kind:** prose — **addition**
- **What it should say:** A definitional claim that excludes the six by construction. Add a half
  sentence: and, for the questions no passage answers, the fact that none does. Testing the test
  means checking the denominator as well as the judgments.

### 12. qrels / `Judged@4`
- **Quote:** "A *qrels* file has one judgment per line: question, a zero nobody uses any more, passage, relevance." and the table `| Step | Judged@4 before the audit | after |` with rows "1. Units + BM25 | 17% | 63%" and "4. + redirect | 17% | 61%"
- **Location:** lines 116–133
- **Kind:** prose + table — **addition**
- **What it should say:** `qrels.txt` has **no lines at all** for G-045…G-050 (verified: 0 of 345
  lines), so `Judged@4` on their top four is 0% and every standard measure silently drops them. Add
  one sentence saying so, and that the `Judged@4` percentages quoted are over the 47. This is the
  mechanical reason `groundless@4` has to be computed from the run files rather than from qrels.

### 13. "Our frozen set has 53 questions" — the pivotal number
- **Quote:** "Our frozen set has 53 questions, 47 of them answerable from the archive, and every number from here on is measured on it. Episode 3's search, run again on it, finds the expected passage for 10 of the 47 answerable questions. That's the line to beat."
- **Location:** line 135, end of `## Building an Evaluation Framework`
- **Kind:** number — **correction**
- **What it should say:** "every number from here on is measured on it" is false as written: every
  number from here on is measured on the 47, not the 53. Correct it to say that Hit@4 and
  all-evidence@4 are measured on the 47 answerable questions only, that the other six are scored
  `answered=None` and excluded from the denominator, and that this episode therefore reports no
  number that can express a refusal. Then hand off to the new subsection (item 5).

### 14. The ratchet's failure conditions
- **Quote:** "- the share of questions with an expected passage in the top four (Hit@4) drops;" / "- the share with *every* expected passage in the top four (all-evidence@4) drops;" / "- any one kind of question loses more than one question;" / "- or more stale records reach the top four."
- **Location:** lines 145–148, `## The ratchet`
- **Kind:** prose — **correction**
- **What it should say:** State the denominator on the first two ("the share of the 47 answerable
  questions…"). Then the admission the list needs: none of the four conditions can fail on the six —
  they have no expected passage to lose and no `stale` entry to trip the fourth condition, and
  "any one kind of question" cannot include a kind the tally does not count. Name `groundless@4`
  here as the condition the ratchet was missing: questions whose top four holds no expected
  passage, over all 53, needing no threshold and so recomputable over runs already stored.

### 15. The "*Why?*" paragraph
- **Quote:** "*Why?* Hit@4 alone isn't enough, because some questions take more than one passage to answer." … "so a step can't look better by finding one end of a question and losing the other."
- **Location:** line 150
- **Kind:** prose — **addition**
- **What it should say:** This paragraph is the episode's one piece of sustained reasoning about
  what Hit@4 cannot see, and it names only multi-hop. Add the second half: Hit@4 also cannot see a
  question that has no right passage at all, so a step that serves four confident passages to all
  six unanswerable questions scores exactly the same as one that declines them. Best single place
  in the episode to introduce `groundless@4` conceptually.

### 16. "With 47 questions"
- **Quote:** "And no tuning. Every setting is fixed before the step runs: the part size, the weights, the number of results. With 47 questions, tuning until a step passes would only teach it the golden set."
- **Location:** line 213
- **Kind:** number — **correction**
- **What it should say:** "With 53 questions, 47 of them scored" — or simply "With a set this
  small". As written it quietly re-states 47 as the size of the whole set.

### 17. "every expected address … is one of them"
- **Quote:** "627 files become 10,540 units, and every expected address in the golden set is one of them."
- **Location:** line 236, `## Step 1`
- **Kind:** prose — **correction** (minor)
- **What it should say:** Reads as reassurance that the corpus covers the whole set. True but vacuous
  for the six. Add a clause: the six unanswerable questions have no expected address, by design.

### 18. The retrieve → post-process → prompt chain (where a refusal branch belongs)
- **Quote:** "A post-processor takes the same query and those documents and returns a new list: reordered, trimmed, or, in Step 4, with some of them replaced. The advisor runs the retriever, then each post-processor in turn, and puts what's left into the prompt."
- **Location:** line 168, `## The ratchet` (also the code at line 178, `new TopDocuments(ElementRetriever.DEPTH)`, and line 427, `return out.values().stream().limit(ElementRetriever.DEPTH).toList();`)
- **Kind:** prose (diagram-shaped) — **addition**
- **What it should say:** **The episode contains no pipeline diagram** — the only figure is the
  audit screenshot (item 8) — so there is no figure in which to draw a declining path. This
  paragraph is the prose equivalent and the right place for the admission: every stage in this
  chain returns a list, nothing in it can return nothing, so there is no point at which the
  pipeline could decline even if a score told it to. If a figure is added, the refusal branch
  belongs between the last post-processor and "puts what's left into the prompt".

### 19. Step 1's four-way comparison
- **Caption quote:** "*Each cell is Hit@4: how many of the 47 answerable questions get a right passage in the top four.*"
- **Table:** `| | Embeddings (nomic-embed-text, as in Episode 3) | Keywords (BM25) |`, rows "800-token chunks | 10/47 (Episode 3) | 20/47" and "Elements | 25/47 | **30/47**"
- **Location:** lines 284–289
- **Kind:** table — **addition**
- **What it should say:** The caption is the episode's most honest denominator statement — keep its
  wording and build on it. Add that the six unanswerable questions appear in no cell, and that all
  four configurations serve them four passages. Denominators in the cells can stay `/47`.

### 20. "two questions in three"
- **Quote:** "Keyword search on the same old chunks doubles Episode 3's score. Elements take it to two questions in three, and the top four now cost a fifth of the tokens, 530 on average instead of 2,850"
- **Location:** line 322, end of `## Step 1`
- **Kind:** number — **correction**
- **What it should say:** 30/47 is two in three only if the set is 47. Over the frozen 53 it is 30,
  or a little over half. Rewrite as "two of every three answerable questions" at minimum; better,
  give both: 30 of 47 answerable, 30 of 53 asked.

### 21. Step 2's overall figure
- **Quote:** "Identifier questions go from 6 of 8 to 8 of 8, and overall from 30 to 32."
- **Location:** line 378
- **Kind:** number — **correction**
- **What it should say:** "overall from 30 to 32 of the 47 answerable." The stratum fractions here
  (6 of 8) are correctly denominated; only "overall" is bare.

### 22. Step 3's stale counter
- **Quote:** "Hit@4 stays at 32. What changes is what else is in the top four: questions with a stale record sitting next to the right answer go from 3 to 1. Fewer wrong answers within reach is a pass."
- **Location:** line 392, `## Step 3`
- **Kind:** prose — **addition**
- **What it should say:** The stale counter only fires on questions that carry `stale` entries — the
  7 stale questions. The six unanswerable carry none, so there is no marked-wrong passage to catch
  them with: their four passages are all unmarked, and "fewer wrong answers within reach" cannot
  measure them. One sentence, right after "Fewer wrong answers within reach is a pass."

### 23. Step 4's "counted fairly"
- **Quote:** "Hit@4 stays at 32, and the stale records in the top four go from 1 to 0. Before the audit, this step was worth two questions, and they were the two the audit accepted … Counted fairly, the redirect earns its place on staleness alone."
- **Location:** line 431, `## Step 4`
- **Kind:** number — **addition**
- **What it should say:** "Counted fairly" is doing a lot of work when the count excludes six
  questions. Keep the claim (it is true for the 47) but qualify the phrase: counted fairly across
  the answerable questions. Zero stale records in the top four does not mean zero wrong passages —
  on the six, all four are wrong and none is stale.

### 24. Rule 8 — prose to build on, not replace
- **Quote:** "Every real archive keeps its history, and it should: the withdrawn proposal and the superseded decision are how you find out *why*. But a search that can't tell them from the current text will answer from the past with complete confidence."
- **Location:** line 440, Rule 8 ("Answer in the right tense")
- **Kind:** prose — **build on, no correction needed**
- **What it should say:** Unchanged. "Will answer … with complete confidence" is exactly the shape
  of the refusal defect in the time dimension, and the new material should echo this sentence
  rather than invent a new register: a search that cannot tell *absence* from an answer will answer
  from nothing with the same confidence. (Measured: G-050 scores +1.213 at top-1 and G-046 +0.977,
  where a *correct* answer elsewhere scores +0.057 — confidence is not the signal that is missing.)

### 25. The metric definitions
- **Quote:** "- **Hit@4**: questions with a right passage in the top four." / "- **All-evidence@4**: questions with *every* passage they need in the top four." / "- **Stale in top four**: questions with an out-of-date record in the top four, where Alpha could quote it."
- **Location:** lines 444–446, `## Where Episode 4 lands`
- **Kind:** prose — **correction**
- **What it should say:** Not one of the three states its denominator. Add "of the 47 answerable" to
  the first two, and add a fourth bullet: **Groundless@4** — questions whose top four holds no
  expected passage, over all 53 — plus the one-line admission that the first three measure only
  whether the right passage was found and none of them measures whether a wrong question was
  refused. (MRR is never defined by name in this episode; `RR@10` appears only at line 494.)

### 26. The main results table
- **Table header:** `| Step | Hit@4 | All-evidence@4 | Stale in top four | Verdict |`
- **Rows:** "Episode 3's search | 10/47 | 9 | 1 | baseline"; "1. Units + BM25 | 30/47 | 24 | 3 | start"; "2. + ids | 32/47 | 26 | 3 | **pass**"; "3. + lifecycle | 32/47 | 26 | 1 | **pass**"; "4. + redirect | 32/47 | 26 | 0 | **pass**"
- **Location:** lines 448–454, `## Where Episode 4 lands`
- **Kind:** table — **correction**
- **What it should say:** The All-evidence@4 column has no denominator at all (bare 9, 24, 26) —
  give it `/47`. Add an **Groundless@4** column reading `6/53` on every row, since no step changes
  it, which is itself the point: four steps of improvement and the column never moves. Verdicts stay
  as they are (the steps did pass the ratchet as defined); add a footnote that every verdict was
  reached without the six being scored.

### 27. The stratum table — `unanswerable` is missing
- **Caption quote:** "And by kind, Hit@4 at Step 4:"
- **Table header:** `| Kind | Questions | Step 4 |`
- **Rows:** "Single fact | 10 | 8"; "Paraphrase | 10 | 2"; "Identifier | 8 | 8"; "Relation | 7 | 6 (all-evidence: 1)"; "Stale | 7 | 7"; "Intent | 5 | 1"
- **Location:** lines 456–466
- **Kind:** table — **correction**
- **What it should say:** The six rows sum to 47 and the table presents itself as the whole set.
  Add a seventh row for the stratum the data file actually has:
  `| Unanswerable | 6 | n/a — 0 declined, all six served four passages |`, and either change the
  caption to "And by kind, Hit@4 at Step 4 (Hit@4 is undefined for the unanswerable six):" or add a
  footnote. Use the repo README's own gloss for consistency: "The right answer is 'Not in the
  archive.'" Also consider renaming the column "Kind" to "Stratum", which is what `questions.jsonl`
  calls it.

### 28. "From one question in five to two in three"
- **Quote:** "From one question in five to two in three, and Beta reads a fifth of the text to get there. Fantastic! Well, kind of."
- **Location:** line 467
- **Kind:** number — **correction**
- **What it should say:** Both fractions are over 47. Over the 53 asked it is 32, three in five. And
  the "Well, kind of" that follows is cashed out only as paraphrase/intent — it should also cover
  the six. Rewrite to give the honest fraction, or say "two of every three *answerable* questions".

### 29. The shortfall paragraph
- **Quote:** "Look at paraphrase and intent. **Three of fifteen.** Those are the questions written *not* to share the passage's words, and keyword search can't find what isn't said. Relation shows the other gap: the search finds one end of a relationship in six questions out of seven, and both ends in one."
- **Location:** line 469
- **Kind:** prose — **addition**
- **What it should say:** Two gaps are named; the third is missing. Add a sentence: and six
  questions the archive cannot answer at all, on which the search returns four passages with more
  confidence than it brings to most right answers. "Keyword search can't find what isn't said" is a
  good line to pivot from — it cannot *say* that nothing is there, either.

### 30. The `ir_measures` glossary
- **Quote:** "- **Success@4**: the field's name for our Hit@4." / "- **RR@10**: one over the rank of the first right passage, averaged: 1 if it's first, ½ if it's second, 0 if it's not in the top ten." / "- **R@4**: the share of a question's right passages that make the top four." / "- **Judged@4**: the share of the top four a person has looked at."
- **Location:** lines 493–496, `### Checking the checker`
- **Kind:** prose — **correction**
- **What it should say:** None states its denominator and all four are averaged over the 47 only,
  because `qrels.txt` contains no lines for G-045…G-050. Say that explicitly: the standard tools
  cannot score a question with no relevant passage, which is why `groundless@4` is computed from
  the run files instead of from qrels. This is the technically load-bearing admission in the
  episode — a reader who checks the numbers with `ir_measures` will reproduce the same blind spot.

### 31. The `ir_measures` results table
- **Table (code block):** header `step     Hit@4 (ours)  Success@4  RR@10   R@4 Judged@4` with rows `step-1 30/47 30/47 0.497 0.54 0.63` … `step-4 32/47 32/47 0.624 0.60 0.61`
- **Location:** lines 499–503
- **Kind:** table — **correction**
- **What it should say:** Keep the figures (they are correct over 47) and label the block as scored
  over the 47 answerable questions. Do not add an `groundless@4` column here — `ir_measures`
  cannot produce one; that is the point to state in the surrounding prose (item 30).

### 32. "they agree on every step"
- **Quote:** "Our Hit@4 is what the field calls `Success@4`, and they agree on every step."
- **Location:** line 506
- **Kind:** prose — **addition**
- **What it should say:** They agree on every step *over the questions both can see*. Add the
  qualifier; agreement between two scorers that share a blind spot is not a check on the blind spot.
  "A checker we didn't write found a bug in ours" (line 518) stands — it found a ranking bug, not
  this one.

### 33. Ship's Log — the form that had no key for "nothing answers this"
- **Quote:** "The lesson: a decision is only as good as the form it's made on. If the form has an opinion, it's in your data now." (also "The audit starts life as a spreadsheet: 795 rows, a decision dropdown…" and "The 211 rows that decide this episode take a day.")
- **Location:** lines 526–532, `## Ship's Log: the dropdown that nudged`
- **Kind:** prose — **addition**
- **What it should say:** The strongest existing hook in the episode and the cheapest place to land
  the whole point. The review page had five keys and not one of them was "nothing here answers it",
  because the page always showed an expected passage — so the form's opinion was that every question
  has an answer, and that opinion is in the data. Two sentences here do more than a new section
  elsewhere. (Aside, out of scope: "the 226 that decide this episode" on line 90 versus "The 211
  rows that decide this episode" on line 530 — 211 is round 1 only, 226 is both rounds.)

### 34. "Next: meaning"
- **Quote:** "Beta now finds the right passage two times in three, and keeps stale records out of reach. What it can't do is find a passage that doesn't share the question's words: three of the fifteen paraphrase and intent questions."
- **Location:** line 536, `## Next: meaning`
- **Kind:** prose — **correction**
- **What it should say:** "two times in three" is the 47 denominator again (item 28), and "what it
  can't do" names one failing where there are two. Add the second: it also cannot decline. Then
  trail Episode 5 honestly — the reranker's top score is the first number in the series that
  separates the six from the answerable (cleanly on the 5090: unanswerable top out at 0.869,
  answerable start at 0.915) — **without** stating a rule number, since that is Episode 5's Rule 10.

---

# Summary

**Counts by kind** (34 items; item 24 is a keep-as-is):

| Kind | Corrections | Additions | Total |
|---|---|---|---|
| prose | 9 | 11 (+1 build-on) | 21 |
| table | 4 | 3 | 7 |
| number | 6 | 1 | 7 |
| diagram | 0 | 1 (+1 noted absence, item 18) | 1 |

Counting items rather than cells: **19 corrections, 15 additions.** Six of the corrections are bare
numbers whose denominator is 47 while the set is 53 (lines 213, 322, 378, 431, 467, 536), plus the
pivotal mis-statement on line 135 that "every number from here on" is measured on the 53.

**Tables needing structural change: three.** The main results table (add `Groundless@4` = 6/53 on
every row; give All-evidence@4 its missing `/47`), the stratum table (add the seventh row,
`Unanswerable | 6`), and the `ir_measures` block (label the denominator; do not add a column it
cannot compute). The audit-decision table, the `Judged@4` table and the Step 1 four-way table need
scope notes, not new numbers.

**Diagrams: there are none to fix.** The only figure is the blind-audit screenshot, which needs a
caption clause. The absence of any pipeline figure is itself worth noting — the retrieve →
post-process → prompt chain exists only as prose at line 168, and every stage in it returns a list,
so there is nowhere a declining branch could even be drawn. If one figure is added to this episode,
that is the one.

**Judgement: this needs a new section, not a light edit.** A light edit cannot fix it, for three
reasons. First, the omission is not a slip but a frame: "unanswerable" appears nowhere in 5,700
words, the stratum table presents six strata summing to 47 as the whole set, and line 43 actively
asserts that all fifty questions have expected addresses. Second, this is the episode in which the
set is built, so it is the only place the six can be *introduced*; every later episode inherits
whatever is established here, and Episode 5 currently has to introduce them itself at line 205
("Six of the golden set's questions are unanswerable on purpose") 5,700 words after they were
frozen. Third, the mechanism of the blind spot — no `expected`, therefore no `stale`, therefore no
qrels lines, therefore invisible to our scorer *and* to `ir_measures` — is a chain that takes a
paragraph to explain and cannot be squeezed into a footnote.

Recommended shape: one new `###` subsection in `## Building an Evaluation Framework` (item 5),
roughly 250–350 words, placed where the 53/47 split is first stated; the `Groundless@4` metric
introduced in the "*Why?*" paragraph (item 15) and defined in `## Where Episode 4 lands` (item 25);
two table changes (items 26, 27); the six bare-denominator number fixes; and two sentences in the
Ship's Log (item 33), which is where the lesson lands hardest. No new numbered Rule.
