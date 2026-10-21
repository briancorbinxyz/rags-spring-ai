# The golden set

Questions with known answers and known sources, so a change to the pipeline can be measured
instead of judged by feel. One JSON object per line.

**Version 2, frozen 20 October 2026**, against spectastic `c64d41c` (v0.1.0-pre.28). Every
question has been signed off by a person, and every passage a search returned instead of an
expected one, where it could change a score, was judged blind (see "The audit" below). Episode 3
measured version 1 (50 questions, unaudited); its numbers are not comparable with these.

| File | What it holds | Questions |
|---|---|---|
| `questions.jsonl` | The main set, scored by address | 53 (47 answerable) |
| `questions-heldout.jsonl` | Held out: scored once, at the end of an episode, never during the build | 13 |
| `questions-time.jsonl` | History and time questions, scored by version (Episode 5 and the time episode) | 24 |

## Fields

| Field | Meaning |
|---|---|
| `id` | Stable id. Ids are never reused: a dropped question's id stays retired |
| `question` | What a reader would ask |
| `stratum` | One of the strata below |
| `expected` | Element addresses that answer it: `specs/<spec>/<file>.html#<element-id>`; empty when unanswerable |
| `accept` | Passages the audit accepted as answering it too: `{address, for}`, where `for` is `question` (it answers the whole question) or one expected address (it can stand in for that one) |
| `stale` | For stale questions: the withdrawn or superseded elements that would answer it wrongly |
| `answer` | The reference answer |
| `core` | The least a correct answer must contain |
| `grading` | Where the sign-off loosened or clarified what counts as correct |
| `commit` | The spectastic commit it was checked against |
| `status` | `accepted` for every question in version 2 |
| `evidence` | Words copied from the first expected element |
| `note` | What the question tests, and anything a reader of the results should know |

## Main set

| Stratum | Questions | Tests |
|---|---|---|
| single-fact | 10 | The easy case: one element answers it |
| paraphrase | 10 | Meaning without the element's own words |
| identifier | 8 | An id such as FR-001, which exists in over a hundred specs, scoped by its spec |
| relation | 7 | A typed link followed: `validates`, `defer-to`, superseded-by, dependencies |
| stale | 7 | A withdrawn or superseded record that would answer it wrongly |
| intent | 5 | Why a feature exists: the problem it was introduced to solve, from the spec's Context |
| unanswerable | 6 | The right answer is "Not in the archive." |

**Scoring.** A question is a hit at k when an expected address, or an accepted one, is in the
top k. It is all-evidence at k when every expected element is covered: by itself, or by an
accepted passage that stands in for it. A passage accepted `for: question` covers all of them.

## As a TREC test collection

`qrels.txt` is the same set in information retrieval's standard format, so a scorer we didn't
write can check our numbers: one judgment per line, `question 0 passage relevance`. Relevance 1 is
an expected passage or one the audit accepted; 0 is a stale record or an audit decision of "partly"
or "doesn't answer it". Unanswerable questions have no lines. 291 judgments in all.

A run file has one line per result, `question Q0 passage rank score run-name`, each passage once
(`TrecFiles.run`; a split unit's later parts are dropped, or the scorer would keep the later rank).
With the prototype's runs:

```bash
pip install ir-measures
python3 tools/ratchet-proto/trec_check.py tools/ratchet-proto/results/run   # on the episode-4-prototype branch
```

| Ours | `ir_measures` |
|---|---|
| Hit@4 | `Success@4` |
| MRR | `RR` |
| (none) | `Judged@4`: the share of the top four that anyone has judged |
| All-evidence@4 | no exact counterpart: a stand-in covers one expected element, which qrels can't say |

`qrels.txt` is generated, never edited: `TrecFiles.qrels(questions, audit)` in Java, or
`python3 -m ratchet.trec` in the prototype, which give byte-for-byte the same file.
`TrecFilesTest` fails if the shipped file and the golden set disagree.

## Time set

`questions-time.jsonl` asks about the past, so an answer is a version, not only an address. Each
question names the commit and date of the version that answers it (`version`), and as-of and
as-known questions name the cutoff (`cutoff`), what-changed questions the window (`window`) and
the versions inside it (`versions`).

| Stratum | Questions | Tests |
|---|---|---|
| history | 7 | Asks about a withdrawn, superseded, retired or rejected record on purpose |
| history-decoy | 3 | Sounds historical ("no longer", "originally") but the answer is current |
| as-of | 5 | What an element said on a date, including one tag-versus-date paraphrase pair |
| what-changed | 5 | The versions of an element inside a window, in order |
| as-known | 4 | What the archive said at the time, before a later correction |

With 4 or 5 questions a stratum, one question is a fifth of it: compare these per question, not
as rates. They were checked against the full spectastic history (every address exists at its
commit, every quote is verbatim, every date is the committer date in UTC) and have never been run
against a retriever.

## Conventions

- **Dates are UTC calendar days.** "On 14 August" means 00:00 to 23:59 UTC. Only one question
  depends on it (G-067: two of its three rewrites were on the evening of 13 August in New York),
  and that one says so.
- **An applied archived change can answer a question.** A change proposal in `changes/archive/`
  counts as an answer when its text matches the current element. A withdrawn or superseded record,
  or an applied change whose text has since been amended, does not: it answers wrongly.
- **"Partly" never counts.** A passage that holds part of the answer but not enough to answer
  is recorded in the audit and scores nothing.
- The set is versioned with the corpus. When the pinned spectastic commit changes, re-check every
  question before comparing numbers.

## The audit

Every search setup from the Episode 4 and 5 prototype runs (seven runs: Ollama on a Mac, and
llama.cpp on an RTX 5090 with stronger embedding and reranking models) left top-4 lists behind.
Where a question missed, the passages each list held instead were judged against the expected
ones, one at a time, without any indication of which setup found them, so a correction could not
favour one setup over another. Passages that could not change any score were not judged.

- Round 1, the 211 passages that the setups on the main line returned when they missed, was
  judged in full: 10 accepted (8 for the whole question, 2 standing in for one expected element),
  17 partly, 184 not. None showed a reference answer to be wrong.
- Round 2, 295 passages that only side experiments returned, is judged only where it was done
  before the rounds were split: 15 passages, 4 accepted, 3 partly, 8 not. Side experiments are
  otherwise scored without equivalents.
- `accept` holds all 14 accepted passages, from both rounds.
- `results/episode-4/golden-audit-2026-10.jsonl` has every decision with its note, and
  `results/episode-4/golden-rescore-2026-10.md` every saved run rescored before and after.
  No step's ratchet verdict changed in any run.

## Changes from version 1

Dropped, with ids retired:

- **G-005** (does `init --tools` wire in `enforce`?): a poor question with an ambiguous expected
  result. Replaced by G-076, what `init --tools` sets up.
- **G-035** (which task proves 062's SC-002?): assumes one task, and two say they do.
- **G-040** (how many files does `init` produce?): a count says nothing useful either way, and
  breaks whenever the corpus is re-pinned.

Fixed:

- **G-007** reworded to say which address it means. The archive contradicts itself here: 087's
  design says the address returned HTTP 200 at pre.23; task T-301, and its later note, say 404.
  The reference answer follows T-301.
- **G-022**: an answer may describe FR-001 and FR-002 rather than name them.
- **G-031**: naming spec 051 is enough; citing FR-003 is preferred, not required.
- **G-036**: the reference answer now mentions the open triage finding 071/T-001 (no entry point
  for a first design). Promotion still runs inside apply at the pinned tag.
- **G-039**: the extended verbs are neither required nor credited.
- **H-001**: the core answer must describe the check, not only name the requirements.

Added: G-076 (single-fact), G-077 to G-081 (intent), and the 24 time questions G-051 to G-075
(G-053 was drafted and dropped).
