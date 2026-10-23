# What was added, and why

The code is meant to read on its own, so no class says which episode it belongs to. This is where
that lives instead: each step, what it was for, and what it cost. The articles tell the story with
the numbers; this is the short version a reader of the code needs.

Branches hold the episodes, so `git diff episode-4...episode-5` shows a step rather than a summary.

## Vocabulary

Two words carry most of the design, and neither is spectastic's.

**A unit** is one element of the archive with an id of its own — a requirement, a decision, an
acceptance criterion — holding its own text and nothing belonging to an element nested inside it.
Its address is `<source>#<anchor>`. A unit longer than 250 words is split into parts at sentence
ends, and every part answers to the unit's address. `units/` holds what a unit carries;
`spectastic/` holds how one archive's HTML becomes units.

**A standing** is where a record sits in its life: in force, or left behind. The search needs only
three things of it — a label to store, whether to leave the record out, and which of two records to
prefer — and never learns what the standings are. Spectastic expresses standing as a *lifecycle* of
five states, read from folders and `data-status` marks.

## The steps

Each one was kept only if it passed the ratchet: Hit@4 must not fall, all-evidence@4 must not fall,
no stratum may lose more than one question, and stale-in-top-4 must not rise. A step that failed is
in the articles with its numbers, not quietly dropped.

### The quoter and the answerer

Two roles. The **quoter** searches and comes back with passages it is willing to quote; the
**answerer** answers using only the quotations traced back to the archive word for word, and
abstains when they do not support an answer.

The names are deliberate. The classic pair in the literature is *retriever* and *reader* (DrQA,
2017), and both halves are wrong here: `DocumentRetriever` is already a Spring AI interface that
this codebase implements, `Reader` is Spring AI's word for ingestion, and in the one framework that
still ships a `Reader` it means extractive span selection — which is nearer to what the quoter does
than the answerer. The pair maps backwards. `Quoter` comes from the "quote-first-then-answer"
literature and from this codebase's own nouns: `Quote`, `QuoteResolver`, `quotes()`.

Neither is an *agent*. Anthropic's distinction is between predefined code paths and models that
direct their own tool use, and Spring AI's own reference calls this shape a chain workflow: the
quoter gets exactly one search and does not choose it. Alpha and Beta remain the characters in the
prompts and in the console output — Beta is "permanently, proudly, in beta", which only works with
the name — while the code says what the roles do.

The two modes are `QUOTED` and `DIRECT`: quote-then-answer, or one model searching and answering in
the same call. The split exists so that "the answer is grounded" is a property of the pipeline rather than
a hope: `QuoteResolver` finds each quotation in what was retrieved or rejects it, and
`CitationChecker` asks whether the answer cited what it claimed to.

Both checkers are plain code, not models. Whether a quotation *supports* a claim is a judgement, and
a judge would itself have to be measured against human labels before it could be trusted to grade.

`FactChecker` is the shared shape, because what counts as evidence changed as the series moved from
a game's own move history to a million words of specifications.

### 1 · Element units and keyword search

A naive baseline split the corpus into 800-token chunks, which cut requirements in half and buried
short ones. One unit per element with an id instead, with its heading path kept in front of the text
so a requirement's own id still carries where it belongs. Keyword search is BM25 through SQLite's
FTS5, weighted towards the heading path and the id.

### 2 · Identifier lookup

"What does FR-001 in spec 045 require?" names a passage, so searching for it is the wrong move:
`FR-001` alone is in over a hundred specs. An id with a scope pins that id in that scope; an id with
no scope pins something only if exactly one scope has it. What a name *looks* like is
`IdentifierGrammar`'s business and the archive's; whether it exists is the lookup's, because the
lookup holds the units.

### 3 · The standing filter

Every archive keeps its history, and it should — a withdrawn proposal is how you find out *why*. But
a search that cannot tell history from current text will answer from the past with complete
confidence. Withdrawn, superseded and rejected units are filtered inside the search rather than
after it, so none of the fifty results is out of date.

### 4 · The supersession redirect

A filter only sees what the markup says. One decision was superseded by a status *element* inside
it, not by the attribute the filter reads, so the filter kept it. The redirect follows the paper
trail instead: a result that something newer replaced gives its place to its successor.

`Successors` is the port — "what replaced this record, what replaced this page" — and an archive may
answer it from markup, prose, a side table or not at all.

> This step originally kept an applied change behind its successor, on the reasoning that it is how
> the current text came to be. That was wrong where a requirement had been rewritten more than once:
> three applied proposals, each marked `status="applied"`, each with a delta naming the same
> requirement, and nothing on any of them saying which rewrite is live. It now drops what it
> replaced. Telling the live rewrite from the superseded ones needs ordering — the spec's changelog
> lists its applied changes in sequence — which is later work.

### 5 · Dense search, fused

Keyword search cannot find what a question does not say. Passages are embedded and searched by
cosine, then the keyword and dense rankings are fused with Reciprocal Rank Fusion (k = 60), with
pinned units above the fusion. The scan is exact rather than approximate on purpose: ten thousand
units is small, and an exact answer means a difference in the results is never the index's fault.

Vectors are stored unscaled, as the Python prototype stores them, and scaled to length one when read
so that a dot product is a cosine. The embedding stack is three decorators — prefixes, then cache,
then the model — and prefixing sits outermost so that what is cached is the text that was sent.

### 6 · Reranking

A cross-encoder reads the question and a passage together rather than comparing two vectors made
apart, and reorders the first fifty. It runs behind one HTTP endpoint rather than in-process,
because exporting the model to ONNX and carrying a tokenizer is an episode of its own — and because
it then gets the same scores from the same library as the prototype, which is what makes the parity
check mean anything.

## Measurement

`src/main/resources/golden/` holds the golden set: 53 questions, 47 answerable, stratified by what
makes them hard — single-fact, paraphrase, identifier, relation, stale, intent, unanswerable. It is
frozen against a pinned spectastic commit, and the questions in `questions-heldout.jsonl` are scored
once, at an episode's end, never during a build.

It is also a TREC test collection, so `ir_measures` can check our scorer. The two agree on every
step bar one known disagreement, which is in the articles: a TREC run file keeps one line per
passage, and cannot say "this slot was spent on a passage you already had".

`Judged@4` is reported next to every result, because dense search surfaces passages nobody has
judged and an unjudged passage counts as wrong.

## Enforcement

`./gradlew check` fails on unformatted code, an Error Prone warning, any javac warning, a failing
test, or a broken architecture rule.

The architecture rules in `ArchitectureTest` are the ones worth knowing about. The load-bearing one
is that nothing in `search`, `units`, `eval` or `answer` may depend on `spectastic`, because a second
corpus should need a new adapter rather than edits to the search — which is the whole claim the
separation makes.
