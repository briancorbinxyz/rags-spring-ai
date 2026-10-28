# Retrieval and QA for "what changed between X and Y" over a versioned corpus

Scope note: this covers prior art, real-system practice, index-the-diff vs diff-the-retrieved, a critical
assessment of the across/within candidate-set predicate, genre/role signals, evaluation, and endpoint
resolution. Bitemporal fundamentals and FTS5 `bm25()` whole-table behaviour are deliberately out of scope.

---

## Q1. Prior art on temporal / version-aware IR and QA where the unit of answer is a change

### Takeaway
Temporal IR is a mature field, but almost all of it ranks *documents by time* rather than returning *a
difference between two revisions of one document*. The change-as-answer formulation exists in only a handful
of places — DiffQG (2023), CHEW (2024), HoH (2025) and VersionRAG/VersionQA (2025) — and every one of them is
built over Wikipedia or technical docs. There is **no benchmark for change-QA over a versioned specification
or standards archive**; that is a genuine gap, not a search failure.

### Cited Findings
- The canonical surveys of temporal IR organise the field around time as a *ranking signal*: query-level,
  document-content-level and ranking-model-level temporal evidence, recency-based ranking, time-dependent
  ranking, event- and entity-aware ranking — Kanhabua, Blanco & Nørvåg, *Temporal Information Retrieval*,
  Foundations and Trends in IR, 2015 — [DOI 10.1561/1500000043](https://unpaywall.org/10.1561%2F1500000043);
  and Campos et al., *Survey of Temporal Information Retrieval and Related Applications*, ACM Computing
  Surveys — [HAL record](https://hal.archives-ouvertes.fr/hal-01076415). A companion review explicitly frames
  the object of study as "timeliness as a relevance signal" —
  [hal-01592046](https://hal.archives-ouvertes.fr/hal-01592046).
- The most current survey covering both temporal IR and temporal QA, including RAG and LLM methods and
  temporal benchmarks, is Piryani et al., *It's High Time: A Survey of Temporal Information Retrieval and
  Question Answering*, 2025 — [arXiv:2505.20243](https://arxiv.org/html/2505.20243v2).
- **Time-travel text search** is the closest classical IR work to the data model here: Berberich, Bedathur,
  Neumann & Weikum, *A Time Machine for Text Search* (SIGIR 2007), extend the inverted index so a query can be
  evaluated "as of" a specified time, and introduce **approximate temporal coalescing** to shrink the index
  without materially hurting quality, plus two index-size/query-performance tradeoffs posed as optimisation
  problems — [MPI-I-2007-5-002](https://domino.mpi-inf.mpg.de/internet/reports.nsf/9cd2c99e39f4ea47c1256e890051b300/bd9f07c1e72cd3d7c125731b0049a242/$FILE/MPI-2007-5-002.pdf);
  demo as *FluxCapacitor* at VLDB 2007 —
  [p1414-berberich.pdf](https://www.vldb.org/conf/2007/papers/demo/p1414-berberich.pdf). A follow-up on
  temporal index sharding for archive search is
  [MPI-I-2011-5-001](https://domino.mpi-inf.mpg.de/internet/reports.nsf/c125634c000710cec125613300585c64/f950b7a3589b921ac12578780029c293/$FILE/MPI-I-2011-5-001.pdf).
  Crucially, time-travel search answers "as of t", **not** "what changed between t1 and t2".
- **DiffQG** (EACL 2023) is the nearest prior art to the change-as-answer unit. It represents factual changes
  between two versions of a document as question-answer pairs where *the same question has a different answer
  in each version*; annotators see paired passages and write questions one passage answers differently or the
  other cannot answer. Released as 759 QA pairs plus **1153 paired passages with no factual change** (i.e. an
  explicit negative set) — [aclanthology 2023.eacl-main.225](https://aclanthology.org/2023.eacl-main.225/).
- **CHEW** builds on TAQA and keeps pairs of Wikipedia article revisions where the answer to a question differs
  between two timestamps, using **similarity filtering to separate real changes from non-changes** —
  [arXiv:2406.19116](https://arxiv.org/pdf/2406.19116).
- **HoH** builds a dynamic benchmark from monthly Wikipedia snapshots, extracting sentence pairs where facts
  changed and generating questions about them, to measure the impact of outdated information on RAG —
  [arXiv:2503.04800](https://arxiv.org/pdf/2503.04800).
- **TREC Temporal Summarization** (2013–2015) is the main track where the output unit is an update rather than
  a document. Tasks: sequential update summarisation and value tracking (2013); by 2015, full
  filtering+summarisation, partial filtering+summarisation, and summarisation-only. Summaries are scored
  against a ground-truth list of information **nuggets**, online as documents are indexed, with an **expected
  latency gain (ELG)** metric rewarding nugget coverage and penalising redundancy, latency and verbosity —
  [TREC 2015 overview](https://trec.nist.gov/pubs/trec24/papers/Overview-TS.pdf),
  [TREC 2014 overview](https://ir.webis.de/anthology/2014.trec_conference-2014.65),
  [NIST trec-browser](https://pages.nist.gov/trec-browser/trec24/tempsumm/overview). Its predecessor,
  DUC/TAC **Update Summarization**, ran 2007–2011 as a single-pass batch task rather than over a stream —
  [HLTCOE TREC 2013 paper](https://terpconnect.umd.edu/~oard/pdf/trec2013.pdf).
- **TREC Novelty** (2002–2004) is sentence-level, query-specific novelty: retrieve relevant sentences, then
  drop later sentences that add no new information. The 2003 edition used 50 topics in "events" and "opinions"
  classes, presented documents in chronological order, and defined four tasks varying how much relevance/
  novelty training data systems got; 14 groups participated —
  [Soboroff & Harman overview slides](https://trec.nist.gov/presentations/TREC2003/noveltyoverview12.pdf).
  Allan, Wade & Bolivar (SIGIR 2003) found the *retrieval* step is the harder half and that "the performance of
  novelty measures is very sensitive to the presence of non-relevant sentences" —
  [CIIR](https://ciir-publications.cs.umass.edu/pub/web/getpdf.php?id=278).
- Structured-change querying over revision histories exists but is not QA: Pellissier Tanon & Suchanek,
  *Querying the Edit History of Wikidata* (2019), a SPARQL system over the revision history —
  [HAL](https://hal.telecom-paris.fr/hal-02096356v1). A 2026 preprint, *WiDiff: Extracting Changes from
  Wikidata's Edit History*, is listed at
  [HuggingFace papers 2609.08508](https://huggingface.co/papers/2609.08508.md) (I could not verify its venue
  or contents beyond the listing).

### Inferences
- The field splits cleanly into (a) *point-in-time retrieval* (time-travel search, point-in-time legislation),
  which is solved and is what the bitemporal model already gives, and (b) *change-as-answer*, which is barely
  studied and has no shared retrieval formulation. The project is operating in (b) with the infrastructure of
  (a) — which is the right way round, but means there is no published retrieval baseline to beat.
- TREC Novelty's finding that the retrieval step dominates difficulty, and that novelty measures degrade badly
  in the presence of non-relevant sentences, is a direct argument for *restricting the candidate set before
  scoring* rather than hoping a reranker separates the change from the narration of the change.
- Approximate temporal coalescing (Berberich) is the ancestor of the "unit-state" idea: a stable address plus
  a half-open interval, with adjacent near-identical states merged. If index size ever becomes a concern,
  coalescing is the principled lever and is a definition (equality under normalisation), not a tuned threshold.

### Gaps
- No benchmark for change-QA over versioned HTML specifications/standards. VersionQA (100 questions, technical
  documentation) is the closest, and its authors call it a single-domain limitation.
- I could not extract DiffQG's revision-selection heuristics, metrics or baseline numbers: the ACL PDF returned
  as un-parseable binary and the landing page carries only the abstract. Those details would be worth a direct
  `pdftotext` read of the local copy.

---

## Q2. How real systems answer "what changed" questions

### Takeaway
Every production system I found is **retrieve-then-diff over a stable address**, not diff-first and not
index-the-diff: the user (or an editorial process) resolves a provision/section identity, the system
reconstructs its text at two points, and a redline is computed on demand. Where a narrative changelog exists it
is treated as a *derived artefact generated from diffs*, not as the retrieval target — which is precisely the
inversion that defeats a naive passage ranker.

### Cited Findings
- **legislation.gov.uk** maintains three distinct views: latest *revised* (editorial team has applied
  amendments), *as-enacted/as-made* (no changes), and *point-in-time* (revised text as it stood on a chosen
  date). Amendments not yet applied live in a separate "Changes to Legislation" area and are deliberately
  **not surfaced in point-in-time views**, only in latest/prospective views. A "Show Timeline of Changes"
  feature lists the dates on which a provision changed, keyed to "the earliest date on which an insertion,
  repeal or substitution came into force", with a base date of 01/02/1991 (01/01/2006 for Northern Ireland)
  before which no earlier versions exist — e.g.
  [s.790T Companies Act 2006 as at 2023-12-01](https://www.legislation.gov.uk/ukpga/2006/46/section/790T/2023-12-01),
  [anaw/2019/4 s.38 point-in-time data](https://www.legislation.gov.uk/anaw/2019/4/section/38/2020-05-06/data.htm).
- **Canada's Justice Laws** site exposes earlier consolidations by date range and, **per amended provision**,
  a link to its prior text together with in-force dates; the site reflects the law as it stood roughly two to
  three weeks before the current date — [laws-lois FAQ](https://laws-lois.justice.gc.ca/eng/faq/).
- **CanLII** point-in-time databases let a user search the text as binding on a past date and **compare two
  versions side by side**; federal statute coverage from 2003, regulations from 2006 —
  [KPU libguide](https://libguides.kpu.ca/law/legislation/history),
  [Slaw announcement](https://www.slaw.ca/2009/09/02/new-point-in-time-legislative-databases-from-canlii/).
- **Commercial legal tooling** reconstructs past versions and generates redlines: Thomson Reuters TimeBase
  links each section to its amendments and offers point-in-time comparison
  ([TimeBase](https://www.thomsonreuters.com.au/en-au/products/timebase/point-in-time.html)); WestlawNZ flags
  static historical versions and unconsolidated amendments
  ([status flags guide](https://support.thomsonreuters.co.nz/sites/default/files/2017-09/NZ-WLNZ-Guide-Legislation-Status-Flags.pdf)).
- **EUR-Lex has no official diff view.** Its release notes describe a sidebar list of all consolidated versions
  with the current one in bold, plus a notice when some amendments/corrigenda are not yet incorporated — but no
  comparison feature ([EUR-Lex developments 03/2020](https://eur-lex.europa.eu/content/development/development_2020-01.html),
  [March 2022](https://eur-lex.europa.eu/content/development/eur-lex-developments-2022-03.html)). The only
  redline tool is an unaffiliated third-party Chrome extension, *EUR-Lex Legislation Diff*, which works by
  picking a base version and then "Compare with current legislation" —
  [Chrome Web Store](https://chromewebstore.google.com/detail/dcccghgbmhkpmpldipomekbbmgmcnmln).
- **Automatic consolidation** (applying an amendment to produce new text) is the hard, well-studied inverse
  problem. Prior, Hof, Wais & Grabmair (NLLP 2025) tested an LLM consolidation pipeline on 908 amendment-law
  pairs from the German Federal Law Gazette: 93–99% textual similarity but only **50.3% exact match for
  single-step and 20.51% for multi-step amendment chains**, concluding outputs need human verification before
  deployment — [aclanthology 2025.nllp-1.29](https://aclanthology.org/2025.nllp-1.29). Etcheverry, Real &
  Chavallard (2024) report >63% success with a quantized LoRA-tuned generative model on a difficult bill.
  Spinosa et al. (2009) built a rule-based NLP system for Italian that handles **repeal, substitution and
  integration** amendments as distinct metadata-oriented operations —
  [CNR record](https://iris.cnr.it/handle/20.500.14243/1330). A separate EU-legislation paper notes that
  detecting textual modifications in legislative acts is well studied across languages, but identifying
  *temporal* provisions (entry into force, end of validity) is harder because of complex wording and
  conditions — [Detecting Transitional Provisions in EU Legislation](https://cris.unibo.it/bitstream/11585/1024381/1/Detecting%20Transitional%20Provisions%20in%20EU%20Legislation.pdf).
- **Documentation/API practice** is spec-diff-then-narrate. Boost.space publishes every release as a
  machine-readable spec served as a diffable document and **generates its API changelog automatically by
  diffing those versions** — [Boost.space changelog](https://docs.boost.space/changelog). The Data City has no
  changelog and instead directs readers to diff its OpenAPI spec for a precise record of changes between two
  points in time — [changelog guide](https://docs.thedatacity.com/api-reference/guides/changelog.md). A 2015
  GBIF thread shows the fallback when no diffable artefact exists: reconstruct changes from commit history —
  [api-users list](https://lists.gbif.org/pipermail/api-users/2015-September/000229.html).

### Inferences
- The legal systems' architecture maps almost exactly onto the project's model: *stable provision address*
  = unit-state address; *point-in-time view* = state in force at a position; *timeline of changes* = the set of
  state boundaries for that address. The "across" predicate is, in legislative terms, the set of provisions
  whose timeline has at least one entry strictly inside the window — which is what a "Show Timeline of Changes"
  listing would return if you asked it for a date range instead of one provision.
- Spinosa's three amendment types (repeal / substitution / integration) and legislation.gov.uk's three
  (insertion / repeal / substitution) are the same taxonomy, and it is a **three-way** taxonomy. The candidate
  mechanism as stated covers only substitution (via `across`) and a particular shape of insertion-plus-churn
  (via `within`). Deletion/repeal — in force at the window start, absent at the end — satisfies neither.
- No production system attempts to *rank* a narrative change-description against the provision it changed;
  they are kept in different stores with different access paths (legislation.gov.uk's "Changes to Legislation"
  annotations are a separate area, excluded from point-in-time views by design). That is strong practical
  support for a structural separation rather than a scoring fix.

### Gaps
- I did not find a published engineering write-up of a *search* system (as opposed to a per-document compare
  UI) that accepts a free-text "what changed between X and Y" query over legislation. Every legal system found
  requires the user to have already identified the provision. If the project's contribution is to do the
  identification from the question, there appears to be no published baseline.

---

## Q3. Index the diff vs diff the retrieved

### Takeaway
There is exactly one systematic piece of evidence, and it favours **index-the-diff**: VersionRAG precomputes
textual diffs between consecutive versions, has an LLM write a semantic description of each, and stores those
as retrievable "change nodes" — lifting change-query accuracy from 25–30% (naive RAG / GraphRAG) to 70%, and
implicit-change accuracy from 0–10% to 60%. The index was cheap (896 nodes for 34 documents). The reported
weakness is the "diff has no good text representation" problem in a specific form: the diff tool misses changes
that are semantic but not textual, and smaller LLMs produce fewer change nodes.

### Cited Findings
- **VersionRAG** (ZHAW, Oct 2025) defines three query types — content retrieval, version listing/inquiry, and
  **change retrieval**, split into *explicit* (documented in changelogs) and *implicit* (undocumented, found by
  comparison). An LLM classifies query intent and extracts document/version parameters, with 92% classification
  accuracy using DeepSeek-R1 70B — [arXiv:2510.08109](https://arxiv.org/html/2510.08109v1).
- VersionRAG's index is a five-level graph: category → document → version (linked in temporal sequence) →
  content nodes (chunk embeddings with version metadata) → **change nodes**. Diffs between consecutive versions
  are computed ahead of time with **DeepDiff**, and an LLM writes semantic descriptions stored as change nodes.
  At query time the system either traverses the graph to explicit change nodes or runs semantic search over the
  indexed changes — [arXiv:2510.08109](https://arxiv.org/html/2510.08109v1).
- Results on **VersionQA** (100 manually curated Q&A pairs over 34 technical documents, 700+ pages; 20 content,
  20 content-complex, 20 content-version-specific, 20 version listing, 10 change-explicit, 10 change-implicit),
  judged by GPT-4.1 with manual review, using DeepSeek-R1 70B:

  | Approach | Overall | Content | Version | Change |
  |---|---|---|---|---|
  | Naive RAG (512-token chunks, text-embedding-3-small, top-5) | 58% | 76.6% | 35% | 25% |
  | GraphRAG (Neo4j HybridCypherRetriever) | 64% | 70% | 80% | 30% |
  | VersionRAG | 90% | 93.3% | 100% | **70%** |

  On implicit change detection specifically, VersionRAG reaches 60% while baselines score 0–10%. With GPT-4o
  Mini it scores 79% overall; with Llama 3 8B only 55%, below GraphRAG's best —
  [arXiv:2510.08109](https://arxiv.org/html/2510.08109v1).
- **Index cost favours index-the-diff heavily**: VersionRAG indexing was ~186K tokens, $0.17, 25 minutes, 896
  graph nodes; GraphRAG was ~2,970K tokens, $6.67, 5h12m, 11,761 nodes — reported as a 97% token reduction —
  [arXiv:2510.08109](https://arxiv.org/html/2510.08109v1).
- **Stated limitations** (authors' own): 100 questions in one domain limits statistical testing; **DeepDiff may
  miss semantic changes that don't appear as textual differences**; smaller models produce fewer change nodes
  and classify queries less accurately; version info must be extractable from the first pages; **non-adjacent
  version dependencies are not captured**; errors propagate and can require reindexing; requires both Neo4j and
  Milvus; and it **does not handle continuous temporal questions (e.g. calendar-based queries)** —
  [arXiv:2510.08109](https://arxiv.org/html/2510.08109v1).
- A practitioner write-up (May 2026) argues the failure is structural, not tuning: two versions of a document
  have nearly identical embeddings so the retriever has no basis for preferring one by time; raising k, adding
  a reranker, switching embedders or rewriting the prompt will not fix it; and faithfulness metrics miss the
  failure because the answer *is* faithful to the retrieved text — "it answers a description question when the
  user asked for a comparison". Its proposed architecture is four parts: (1) temporal index with per-chunk
  content hashes and validity windows so unchanged chunks are not re-indexed; (2) **query intent routing** on
  delta markers ("what changed", "since last quarter"); (3) **stable chunk identity** via structural chunking
  with IDs that persist across edits, so v1 and v2 chunks can be aligned; (4) a comparison-synthesis prompt fed
  labelled before/after text asking for a structured diff (added / removed / rephrased / substantively
  changed) — [tianpan.co](https://tianpan.co/blog/2026/05/02/rag-cant-answer-what-changed). Unbenchmarked; it
  cites VersionRAG's numbers rather than producing its own, and does not quantify the storage cost of retaining
  every version.
- The engineering mainstream in documentation is the hybrid: version the spec, publish it in a diffable format,
  and generate a curated narrative changelog from the diffs — i.e. materialise the diff as a *document for
  humans* — [Boost.space](https://docs.boost.space/changelog),
  [Docsie on diffs feeding changelog generation](https://www.docsie.io/blog/glossary/diff/).

### Inferences
- The index-size objection to index-the-diff is weaker than expected: in VersionRAG, change nodes are a small
  fraction of 896 nodes over 700+ pages, because the number of *changes* between adjacent versions is far
  smaller than the number of chunks. For ~15,600 unit-states, the number of across-window changes is bounded by
  the number of state transitions, which is already materialised in the data model — the diff is essentially
  free to enumerate; only its *text representation* costs anything.
- The decisive tradeoff is therefore not index size but **representation**: an LLM-written change description
  is retrievable by a dense/lexical index but is a lossy paraphrase, invents vocabulary, and is exactly the
  genre ("narration of a change") that already outranks the requirement in this corpus. Materialising diffs as
  retrievable documents in *this* archive risks adding 15,600 synthetic competitors to the real change
  proposals. The candidate mechanism avoids that entirely by never giving the diff a text representation: it
  restricts *which addresses* can be answers and lets the retriever score their actual text.
- VersionRAG's explicit/implicit change split is the formal name for the project's core problem. Its *explicit*
  case is "the changelog narrates it" — which it treats as the easy, desirable path. In this corpus the
  equivalent (a change proposal narrating the change) is the **distractor**, because the expected answer is the
  requirement text itself. So VersionRAG's architecture is partly inapplicable: it would route to the proposal
  and be scored wrong. This is a real divergence worth stating, not a detail.
- VersionRAG's "does not handle continuous temporal questions (e.g. calendar-based)" limitation is exactly the
  project's question form ("between 10 and 12 July 2026"). Its version-sequence graph assumes discrete named
  versions; a date-windowed bitemporal interval model is strictly more general here.

### Gaps
- No study compares index-the-diff against diff-the-retrieved on the *same* corpus with the same retriever.
  VersionRAG compares against version-unaware baselines, so its gain conflates version-awareness with
  diff-indexing. The project's own measurement (candidate restriction, no reranker) is closer to an ablation of
  that confound than anything published.
- No reported figures on recall loss from diff-indexing, nor on what fraction of changes a textual differ misses
  in prose/HTML (DeepDiff is a structured-data differ; its suitability for HTML spec prose is not evaluated).

---

## Q4. Critical assessment of the across/within candidate-set predicate

### Takeaway
The predicate has **no name in the literature** and I found no system that uses it as a *retrieval-time
candidate restriction*. But the "across" half is precisely the filter that DiffQG and CHEW use as an **oracle
for dataset construction** — "keep pairs of revisions where the answer differs between two timestamps" — and
CHEW's experience is instructive: raw textual difference was noisy enough that they had to add similarity
filtering to separate real changes from non-changes. The "within" half (churn count inside the window) I could
not match to any published retrieval or QA technique. The mechanism is sound, cheap and definitional, but it is
incomplete in a specific, enumerable way: it is a *substitution* detector, and changes are a three-way
taxonomy.

### Cited Findings
- **CHEW defines a change exactly as "across"** — "the answer to a question differing between two timestamps" —
  keeping pairs of article revisions where an answer differs, and then applying **similarity filtering to
  separate real changes from non-changes** — [arXiv:2406.19116](https://arxiv.org/pdf/2406.19116).
- **DiffQG pairs passages across revisions and also publishes 1153 no-change pairs**, i.e. it treats
  "text differs but nothing factual changed" as a first-class, frequent category worth 60% as many examples as
  the positives — [aclanthology 2023.eacl-main.225](https://aclanthology.org/2023.eacl-main.225/).
- In temporal relational algebra the endpoint-vs-interval distinction the predicate glosses over is a known
  subtlety: an interval-based difference operator treats intervals as atomic values and arguably is not
  temporal at all, whereas **sequenced difference** operates per time point and "can fragment the validity
  periods of the left-hand relation, which introduces memory complications" —
  [TimeCenter TR-21](https://timecenter.cs.aau.dk/wp-content/uploads/2022/12/TR-21.pdf),
  [Dignös et al. PVLDB 2019 poster](https://www.inf.unibz.it/~dignoes/res/pvldb2019-poster.pdf).
- The practical database pattern for "what changed between two snapshots" is a FULL OUTER JOIN of the two
  snapshots on the entity key, returning rows present in only one side plus rows whose compared columns differ
  — documented as a snapshot-delta SQL recipe —
  [CAST Central Base snapshot delta query](https://doc.castsoftware.com/export/TG/SQL+Queries+-+CAST+Central+Base+-+Queries+on+Objects+-+How+to+get+the+delta+of+objects+between+two+snapshots).
  Note this pattern **includes the only-on-one-side rows**, which `across` excludes by requiring presence at
  both endpoints.
- Legislative practice treats change as three operations, not one: **insertion, repeal, substitution**
  (legislation.gov.uk timeline keying) and **repeal, substitution, integration** (Spinosa et al.) —
  [legislation.gov.uk point-in-time example](https://www.legislation.gov.uk/ukpga/2006/46/section/790T/2023-12-01),
  [Spinosa et al.](https://iris.cnr.it/handle/20.500.14243/1330).
- Formatting-only change is a solved-in-principle problem via normalisation before comparison: AST-based
  differs parse both versions so formatting-only edits drop out before matching, with extra rule sets for
  equivalences like optional commas and unnecessary parentheses, and grouping of renames and moved code —
  [SemanticDiff FAQ](https://semanticdiff.com/vscode/faq/), [SemanticDiff](https://semanticdiff.com/); the
  simple case is `diff.ignoreAllSpace` / GitHub's "hide whitespace changes" —
  [CodeRabbit on reading diffs](https://docs.coderabbit.ai/change-stack/reading-diffs). The known limit is that
  AST filters are precise for formatting but **cannot prove a refactoring preserves meaning**; refactoring-aware
  filtering misreports some non-semantics-preserving refactorings and misses multi-step refactorings split
  across commits — [RefFilter, arXiv:2510.01960](https://arxiv.org/pdf/2510.01960).
- TREC Novelty's lesson is that this kind of pre-filtering matters: the retrieval step is the harder half, and
  "the performance of novelty measures is very sensitive to the presence of non-relevant sentences" —
  [Allan, Wade & Bolivar](https://ciir-publications.cs.umass.edu/pub/web/getpdf.php?id=278).

### Inferences
**On naming.** The closest honest descriptions are: (a) a *snapshot-difference / sequenced-difference* predicate
from temporal databases, restricted to the intersection of the two key sets; (b) a *Boolean/structured prefilter*
in IR terms — a facet constraint on the candidate set rather than a score, the same class of mechanism as a
metadata filter in hybrid vector search; (c) in benchmark-construction terms, the *change oracle* used by CHEW
and DiffQG, repurposed to query time. I would not claim it as a known named technique. "Repurposing the
dataset-construction oracle as a query-time candidate restriction" is a defensible and honest framing, and the
fact that benchmark authors trust this predicate enough to define ground truth with it is the strongest
available argument for its soundness.

**Predictable failure modes**, in rough order of likely impact here:

1. **Change-and-change-back (A→B→A inside the window).** `across` compares endpoint *text*, so it returns a
   false negative. `within` excludes it too, because the address was present at the window start. Fix that
   stays definitional: make the predicate *"has at least one state boundary strictly inside the window"*
   rather than *"endpoint texts differ"*. That generalisation subsumes both halves — `across` and `within`
   become one predicate (`boundaries_inside(addr, window) > 0`) partitioned by presence at the window start,
   with the state count only needed to exclude born-inside-and-never-touched units. It also removes the
   endpoint-text comparison entirely, which is the expensive part.
2. **Deletion/repeal is silently excluded.** An address in force at the window start and absent at the end is
   unambiguously a change, fails "in force at BOTH endpoints", and (being absent at the end) is not caught by
   `within` either. Given the legislative taxonomy is three-way, this is a structural recall hole, not an edge
   case. It did not show up in the 5-question measurement, which is evidence about the questions, not the
   mechanism.
3. **Address instability / rename mid-window.** If the anchor or path changes (`#FR-002` → `#FR-002a`, file
   renamed, section renumbered), the unit looks absent at one endpoint — so it misses `across` — *and* looks
   born-inside-the-window with one state, so it misses `within`. A double miss on the exact case a user is most
   likely to ask about. This is the "stable chunk identity" requirement the practitioner write-up names as a
   necessary component, and the thing legal publishers solve editorially by keeping a provision address stable
   while its text is amended.
4. **Whitespace / markup-only change.** A template change, re-render or reformat makes endpoint text differ for
   thousands of addresses at once and collapses the precision gain back to nothing. The mitigation is
   definitional and therefore compatible with the no-tuning rule: compare a **normalised content hash**
   (DOM-normalised text, collapsed whitespace, attributes dropped) rather than raw text. But note DiffQG's 1153
   no-change pairs: even after normalisation, "text differs, nothing substantive changed" is common, and no
   purely structural predicate can catch paraphrase-only edits. That residual is where a reranker or an LLM
   judge genuinely earns its place.
5. **Both endpoints inside a single state (no boundary in the window).** Both sets come back empty. The system
   must *degrade* to unrestricted retrieval rather than return nothing; a hard restriction with no fallback
   turns a bad answer into no answer, which is worse for Hit@k and much worse for a user. This also covers the
   "window endpoints fall inside one commit" case.
6. **Endpoint resolution under day granularity** makes "in force at the endpoint" set-valued rather than
   single-valued — see Q7.
7. **Statistical fragility of the measured gain.** On n=5, Hit@4 1/5 → 2/5 is one rank flip, i.e. ±20 points
   per question. The rank movements (40→4, 33→8, absent→24, 1→1, 5→6) are more informative than the aggregate
   and include one regression (5→6). VersionRAG's authors flag the same limit at n=100 ("small benchmark … so
   statistical testing is limited"). The defensible claim is about the **candidate set**: 109–4,434 → 13–459 is
   a 1–2 order-of-magnitude reduction with the expected answer retained in all 5 cases — that is a
   recall-preserving restriction, and it is measurable without the ranking noise.

**What the mechanism gets right that the literature supports.** It is a restriction, not a score, which is what
TREC Novelty's sensitivity finding argues for. It gives the diff no text representation, avoiding the exact
distractor-genre problem the corpus already has. It is derivable from the data model (state boundaries over an
address within an interval), so there is nothing to fit to 5 questions. And it is a pure SQL predicate — a
self-join on address over the unit-state table, plus a `COUNT` of states whose half-open interval intersects the
window, plus (recommended) a stored normalised-hash column — expressible in SQLite with one extra indexed
column, with FTS5 constrained by joining to the candidate rowid set rather than by scoring.

### Gaps
- I found no published work using a churn-count-within-window predicate for retrieval candidate selection. In
  software engineering, "churn" (number of revisions / relative code churn) is a well-known *defect-prediction
  feature*, but I did not find it used as an IR candidate restriction and will not claim the connection without
  a source.
- No published figures on the precision/recall of textual-difference filters over HTML prose specifically, so
  the magnitude of failure mode 4 in this corpus is unmeasured.

---

## Q5. Novelty/redundancy signals: separating "the rule" from "the narration about the rule"

### Takeaway
There is **no off-the-shelf classifier for "normative statement vs commentary about a normative statement"** —
I searched for it directly and found nothing. The right research family is sentence-level **rhetorical role
labelling / argumentative zoning**, which is mature in law and science and has transferable label sets, and
**metadiscourse**, which is defined functionally in exactly the way the distinction requires. But in this
corpus the signal is available structurally and for free, and I would argue a structural predicate beats a
classifier here on both cost and the project's no-tuning constraint.

### Cited Findings
- **Metadiscourse is defined by function, not content**: Vande Kopple's taxonomy describes it as discourse that
  helps readers "connect, organize, interpret, evaluate, and develop attitudes toward" the material, with
  Halliday's ideational function corresponding to propositional meaning while the interpersonal and textual
  functions are metadiscourse's domain — [review PDF](https://ueaeprints.uea.ac.uk/71001/1/Accepted_manuscript.pdf).
  The largest annotated resource found is the OpenCourseWare Metadiscourse corpus: over 60,000 hand-annotated
  metadiscourse acts from 106 lectures, with automated classification stated as future work —
  [LREC 2016, L16-1279](https://aclanthology.org/L16-1279/).
- **Rhetorical role labelling** is "a sequential classification problem, classifying each textual segment into
  its role in the given text" — [Rhetorical Role Labeling with Transformers and GNNs, arXiv:2305.04100](https://arxiv.org/pdf/2305.04100).
  It descends from Teufel's **argumentative zoning** (sentence-by-sentence classification of scientific text),
  and Teufel et al. (2009) noted that parts of the AZ scheme overlap with **citation-function classification**,
  where citation function is "the purpose a citation serves relative to the citing paper's contributions" —
  [Citation Recommendation based on Argumentative Zoning, arXiv:2501.18292](https://arxiv.org/pdf/2501.18292).
- Legal adaptations supply directly relevant labels: Grover, Hachey & Korycinski (2003) added legal
  subcategories including **PRECEDENT, LAW, EVENT, LOWER COURT DECISION, JUDGMENT, INTERPRETATION, ARGUMENT**
  to House of Lords judgments; Kalamkar et al. (2022) use 13 categories over Indian Supreme Court judgments
  (facts, issues, petitioner/respondent arguments, analysis, precedent treatment, ratio, decision), with a
  simpler 7-label variant in LegalSeg — [LegalSeg, arXiv:2502.05836](https://arxiv.org/pdf/2502.05836).
- Known difficulty: legal documents are poorly structured, roles interleave, and the roles "may be subjective
  (as evident from variation of opinions between legal experts)" — Bhattacharya et al. 2019, as reported in
  [arXiv:2502.05836](https://arxiv.org/pdf/2502.05836). A 2026 US Supreme Court corpus annotated at three
  granularities for rhetorical sentence function is mentioned in
  [arXiv:2609.25441](https://arxiv.org/pdf/2609.25441) (attribution uncertain — see Gaps).

### Inferences
- The "statement of a rule vs narration about the rule" distinction is, in metadiscourse terms, ideational vs
  interpersonal/textual; in argumentative-zoning terms it is OWN-claim vs OTHER-reference; in
  citation-function terms, the change proposal is functionally a **citing sentence** and the requirement is the
  **cited text**. All three framings give the same operational feature: *does this unit refer to another unit's
  address?* A change proposal that narrates a change to `#FR-002` almost certainly mentions `FR-002`; the
  requirement itself does not mention itself. "Unit-state cites a stable address other than its own" is a
  definition derivable from the data model, costs one regex over the HTML, and needs no training data or
  threshold — which fits the project's constraints far better than a role classifier.
- A genre/role signal is **more robust in one specific respect**: it survives the structural predicate's
  failure modes 2, 3 and 5 (deletion, rename, no-boundary-in-window), because it does not depend on the
  temporal index at all. The two signals fail independently, which argues for using both — structural predicate
  for the candidate set, genre/citation signal as a demotion on whatever survives — rather than choosing.
- A genre signal is **weaker** in that the corpus's own collection structure (`specs/` vs proposals) already
  encodes it perfectly and for free. Training or prompting a classifier to recover a distinction the path
  already makes is work the data model has done. Recommend genre/role only if proposals and requirements ever
  share an address space.
- Rhetorical-role subjectivity (expert disagreement) is a real warning: a label set whose boundaries experts
  dispute cannot be a hard filter, only a soft demotion.

### Gaps
- No corpus or scheme separating normative provisions from commentary on them. The closest adjacent work would
  be deontic/normative-sentence classification in legal NLP, which I did not have search budget to pursue;
  the metadiscourse search returned only general background.
- The SCOTUS-Law / three-granularity rhetorical corpus appeared only as a secondary mention; I could not verify
  its authors, venue or availability.

---

## Q6. Evaluation of change-QA

### Takeaway
There is no accepted way to score "the answer is a diff". The three available precedents are nugget-based
scoring (TREC TS), LLM-as-judge accuracy on a small hand-curated set (VersionRAG), and reformulating the change
as a QA pair so standard QA metrics apply (DiffQG, CHEW, HoH). The strongest methodological recommendation the
literature supports is TREC TS's own structure: **score filtering and summarisation separately** — which maps
directly onto reporting candidate-set recall independently of rank.

### Cited Findings
- **TREC Temporal Summarization** scores against ground-truth **nuggets** with **expected latency gain**,
  rewarding nugget coverage while penalising redundancy, latency and verbosity, and crucially offers separate
  subtasks for *full filtering + summarisation*, *partial filtering + summarisation*, and *summarisation only* —
  i.e. the track deliberately decomposes the filtering stage from the generation stage —
  [TREC 2015 overview](https://trec.nist.gov/pubs/trec24/papers/Overview-TS.pdf),
  [HLTCOE 2013](https://terpconnect.umd.edu/~oard/pdf/trec2013.pdf).
- **VersionRAG/VersionQA** uses plain accuracy, judged by GPT-4.1 and manually reviewed for all 100 questions,
  broken out per query category (including Change-Explicit n=10 and Change-Implicit n=10) — and the authors
  state that 100 questions in one domain limits statistical testing —
  [arXiv:2510.08109](https://arxiv.org/html/2510.08109v1).
- **DiffQG's** evaluation design *is* the metric choice: representing the change as a question whose answer
  differs between versions means "the question-answer format can flexibly and concisely capture the updated
  contents", so ordinary QA scoring applies to a change —
  [aclanthology 2023.eacl-main.225](https://aclanthology.org/2023.eacl-main.225/). Its 1153 no-change pairs make
  *false-positive change detection* directly scorable.
- Temporal-QA metric work is actively moving away from exact match. *Time to Revisit Exact Match*
  ([arXiv:2509.16720](https://arxiv.org/html/2509.16720v1)) argues EM is inadequate for temporal QA.
  **TempAnswerQA** (EMNLP Findings 2025) treats temporal QA as numerical estimation and uses **sMAPE and MASE**
  instead of EM, because answers can be ambiguous
  ([record](https://preview.aclanthology.org/dashboard/2025.findings-emnlp.637)). **TDBench** introduces a
  **"time accuracy"** metric assessing the validity of time references in explanations alongside answer
  accuracy ([overview](https://www.emergentmind.com/topics/tdbench)). **TEMPO** (EMNLP 2026) proposes retrieval
  metrics **Temporal Coverage@k** and **Temporal Precision@k**, measuring whether results span the required
  time periods ([aclanthology 2026.emnlp-main.291](https://aclanthology.org/2026.emnlp-main.291.pdf)). A 2026
  paper (RecencyQA) criticises existing datasets for reducing temporal validity "to a binary distinction —
  current versus outdated — without modeling how frequently answers are expected to change"
  ([arXiv:2603.16544](https://arxiv.org/html/2603.16544v1)).
- Legal consolidation evaluation is a cautionary data point on metric choice: 93–99% textual similarity
  co-existed with 50.3% / 20.51% exact match, so a similarity metric would have declared the system nearly
  solved — [aclanthology 2025.nllp-1.29](https://aclanthology.org/2025.nllp-1.29).

### Inferences
- For this project, the evaluation that the literature best supports is a two-part report: **(1) candidate-set
  recall** — is the expected address in the restricted set, measured as a set property with no ranking involved,
  which is the "filtering" half of TREC TS's decomposition and is exactly what "all 5 expected answers are in
  the union" measures; and **(2) rank/Hit@k on the restricted set**, reported per question rather than
  aggregated, because n=5 makes aggregates uninformative. Reporting candidate-set size reduction alongside
  recall (109–4,434 → 13–459, recall 5/5) is a legitimate, low-variance claim; "Hit@4 1/5 → 2/5" is not.
- TEMPO's Temporal Coverage@k is the nearest published analogue to a metric the project could adopt: for a
  window question, do the retrieved units' validity intervals actually cover both endpoints? That is computable
  from the bitemporal model with no annotation, and it penalises exactly the proposal-narration distractor
  (born inside the window, covering neither endpoint).
- The legal-consolidation similarity/exact-match gap is the direct argument against scoring a generated diff
  description with ROUGE/BERTScore-style similarity. If the final answer is a diff description, the nugget
  approach (did the answer name the categories that were added/removed?) is the more honest scoring, and nuggets
  for 5 questions are cheap to write by hand.

### Gaps
- I could not retrieve DiffQG's actual metrics and baseline numbers (PDF unreadable via fetch).
- TDBench, TEMPO and RecencyQA come from search-result snippets only; I did not open the papers and cannot
  confirm metric definitions precisely. Treat those four metric names as leads, not verified definitions.

---

## Q7. Day-granular/tied timestamps, monotone ordinals, and endpoint resolution

### Takeaway
The project's choice of git first-parent position as the order key is well supported: git itself only
guarantees a *topological* ordering, timestamps are routinely skewed and non-unique, and the MSR literature
documents this as a named data-quality hazard. The refinements worth noting are that (a) first-parent position
is a projection, not the full order — it discards the real position of commits merged in from a branch; (b) a
release-tag endpoint resolves to a commit that may not be on the first-parent chain at all; and (c) day
granularity makes an endpoint a *set* of states, which changes the predicate's semantics.

### Cited Findings
- **Git does not order by time.** "git is happy with a commit with an older timestamp following a commit with a
  newer timestamp"; Subversion "requires a total ordering of commits, whereas git only requires a topological
  ordering" — [PostgreSQL list](https://postgresql.org/message-id/4C753960.5000501%40alum.mit.edu),
  [related thread](https://www.postgresql.org/message-id/4C750871.8000409%40f2s.com).
- Git's maintainer confirms timestamp-driven traversal is defeasible: "the topo-order algorithm can be fooled by
  skewed timestamps … but is helped by being able to leave early, and it will give us the correct answer as
  long as there is no timestamp inversion" — Junio C Hamano,
  [public-inbox](https://public-inbox.org/git/xmqqk1m6ig52.fsf@gitster-ct.c.googlers.com/).
- **Timestamps are not unique.** Committer/date pairs "can easily happen fast enough to give several successive
  commits the same committer-ID and timestamp", and sorting by those pairs "does not take any graph topology
  into account" — [git list](https://public-inbox.org/git/ae62476c-1642-0b9c-86a5-c2c8cddf9dfb@gmail.com/). A
  JGit developer raised the same for reachability checks
  ([jgit-dev](https://eclipse.org/lists/jgit-dev/msg01215.html)), and a CI vendor's users hit it as missed
  commits, with the suggested fix being "constructing a new sort field based on the topological order of the
  commits" — [FinalBuilder forum](https://www.finalbuilder.com/forums/t/continua-misses-commits-when-commit-dates-are-not-in-correct-order/5951).
- **This is a documented research hazard, not folklore.** Flint, Chauhan & Dyer, *Escaping the Time Pit:
  Pitfalls and Guidelines for Using Time-based Git Data* (MSR 2021), observe that many SE papers rely on commit
  timestamps, that "like most real-world data however, time-based data is often dirty", quantify the sources of
  dirty git timestamps using Boa and Software Heritage, and give guidelines for researchers using time-based git
  data — [arXiv:2209.04511](https://ar5iv.labs.arxiv.org/html/2209.04511),
  [MSR'21 slides](https://cse.unl.edu/~rdyer/slides/msr21-slides.pdf),
  [talk](https://mediahub.unl.edu/media/16120).
- **Point-in-time legal systems resolve window endpoints to dates with explicit base dates and explicit
  exclusions.** legislation.gov.uk keys its change timeline to "the earliest date on which an insertion, repeal
  or substitution came into force", sets a base date of 01/02/1991 (01/01/2006 for NI) below which no earlier
  version exists, and excludes not-yet-applied amendments from point-in-time views entirely —
  [example](https://www.legislation.gov.uk/ukpga/2006/46/section/790T/2023-12-01). Justice Laws notes its
  consolidation lags the present by about two to three weeks —
  [FAQ](https://laws-lois.justice.gc.ca/eng/faq/). EUR-Lex warns when a consolidated version omits amendments
  and corrigenda not yet incorporated —
  [developments](https://eur-lex.europa.eu/content/development/eur-lex-developments-2022-03.html).
- Temporal algebra confirms that collapsing an interval question to two instants is a real semantic choice:
  interval-based difference "treats intervals as atomic values", whereas sequenced difference works per time
  point and can fragment validity periods —
  [TimeCenter TR-21](https://timecenter.cs.aau.dk/wp-content/uploads/2022/12/TR-21.pdf).

### Inferences
- **First-parent position is the right order key but is a lossy order.** It is monotone and total along the
  mainline, which is what the predicate needs; but a merge collapses an entire branch's commits to the merge's
  single position, so a change authored on a branch and merged later has its *merge* position, not its
  authoring position. Consequence for the window predicate: a window that ends just before a merge will not
  see changes authored (by date) inside it. This is the correct behaviour for "what was in force", and the
  wrong behaviour for "what did people change" — worth deciding explicitly rather than inheriting.
- **Release-tag endpoints need their own resolution rule.** A tag names a commit, not a position. The clean
  definition is: tag → commit → *nearest ancestor on the first-parent chain* (equivalently, the first-parent
  position of the merge that brought it in), with the degenerate case being a tag on a commit not reachable
  from the mainline at all, which has no position and must be an error rather than a silent 0. This is
  definitional and testable.
- **Day granularity makes the endpoint a set.** "Between 10 and 12 July" with day-resolution timestamps and
  multiple commits per day means "in force at the start endpoint" resolves to a range of positions, not one.
  The least surprising definitional resolution, and the one matching legal practice (point-in-time views are
  *as at end of* a date), is: start endpoint = the *last* position on or before the start date; end endpoint =
  the *last* position on or before the end date. Half-open on positions, inclusive on days. Resolving the start
  to the *first* position of the day instead silently includes same-day changes in the window, which will make
  `within` fire on things the user considers "already there".
- **State the tie-break as part of the answer.** Since the legal systems all publish their currency ("up to
  date with changes known to be in force on <date>", "lags by 2–3 weeks"), and since day-granular resolution is
  genuinely ambiguous, echoing the resolved positions back ("10 July resolves to commit position 418; 12 July
  to 431") costs nothing and makes a wrong resolution visible rather than silent. That is a provenance
  practice with direct precedent in all four legal systems surveyed.

### Gaps
- I found nothing that *challenges* using a monotone commit ordinal as the temporal key — the literature is
  uniformly in favour of graph-derived ordering over timestamps. The absence of counter-argument is itself the
  finding.
- No published work on resolving a natural-language date window to version endpoints in a versioned archive
  (i.e. the query-understanding step). The legal systems all take the date as explicit user input via a UI
  control, so they never face it.
