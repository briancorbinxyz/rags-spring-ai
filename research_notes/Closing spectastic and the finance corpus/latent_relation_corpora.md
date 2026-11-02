# Corpora with genuinely undeclared cross-document relations: candidate comparison

Research date: **29 October 2026**. All claims dated where the source carries a date.

**Scope note for the report writer.** This note builds on and does not repeat
`research_notes/Schema implied relations in spectastic/schema_implied_edges.md` (the SEC form-type relation
family, DITA/S1000D/XBRL as document-type grammars, requirements traceability) and
`research_notes/Multi hop questions and graph retrieval/multihop_external_evidence.md` (traversal depth, the
authored-link null result, benchmark validity). Both are taken as given. The decision context is the
conclusion in `reports/Multi hop questions and graph retrieval.md` that spectastic is a **schema-governed**
corpus, not a latent-link corpus.

**Three research-process limits up front, because they shape what is and is not sourced below.**
1. `ecfr.gov` is behind bot protection: every fetch redirected to `unblock.federalregister.gov`. I have **no
   primary source** for eCFR point-in-time behaviour, its API, or its copyright status. Recorded as a gap, not
   asserted.
2. `eur-lex.europa.eu/content/legal-notice/legal-notice.html` returned an **empty body** on two attempts. The
   EUR-Lex reuse licence is therefore sourced indirectly (see Q3) and must be verified before shipping.
3. `case.law/about/` and `case.law/docs/` returned **title-only pages**. The Caselaw Access Project's own
   licence text could not be retrieved. Its status is reported below from secondary sources and explicitly
   flagged.

**Labelling convention used throughout.** `[MEASURED]` = a number from a primary source.
`[REPORTED]` = a claim from a secondary or non-peer-reviewed source. `[MY REASONING]` = my own inference, not
measured by anyone. Licences are given by exact name; where I could not retrieve the licence text, that is
stated rather than paraphrased.

---

## Q1. Finance, honestly assessed: which filing relations are genuinely latent?

### Takeaway
**Finance's relational structure is mostly declared or derivable, with exactly one genuine exception.** Of the
nine relation families in the brief, I judge six declared or derivable by identifier join or regex, two
partially latent, and **one — entity resolution across filers, subsidiaries and prose-named counterparties —
genuinely latent and measurably hard.** The one hard number I found is damning for the "finance needs
GraphRAG" framing and supportive of the "finance needs record linkage" framing: an open, well-engineered
Exhibit 21 pipeline matches only **2% of unique extracted subsidiaries to a SEC CIK** `[MEASURED]`. Moving to
finance for a *latent-relation* episode would repeat the spectastic outcome in a new costume; moving to
finance for an *entity-resolution* episode would not.

### Cited Findings

**Entity resolution across filers and subsidiaries — LATENT, and measured.**
- **Exhibit 21 carries no shared key.** The PUDL SEC 10-K pipeline documentation states plainly that "Ex. 21
  does not report CIKs," so extracted subsidiaries have no identifier in common with SEC filers, and that
  Exhibit 21 files "are not standardized in format or content" —
  [PUDL SEC 10-K ownership extraction modeling](https://docs.catalyst.coop/pudl/en/latest/methodology/sec10k_modeling.html.md)
- **Match coverage is tiny.** PUDL matches subsidiaries to 10-K filers on company name with location and
  report year as tiebreakers, and reports that about **2% of unique `subsidiary_company_id_sec10k` match to
  19% of filers**, averaging about **3.5 subsidiary IDs per central index key**. The approach is deliberately
  precision-biased, so most subsidiaries stay unmatched `[MEASURED]` — [PUDL modeling](https://docs.catalyst.coop/pudl/en/latest/methodology/sec10k_modeling.html.md)
- **The matching rule is exact-name-plus-jurisdiction.** The published association table defines a match as
  "identical names and the same location of incorporation," and builds the subsidiary key by concatenating the
  parent's CIK, the subsidiary name and the location of incorporation — which means **the same subsidiary
  under two different parents gets two different IDs** —
  [core_sec10k__assn_exhibit_21_subsidiaries_and_filers](https://data.catalyst.coop/pudl/core_sec10k__assn_exhibit_21_subsidiaries_and_filers)
- **Even subsidiary→external-registry matching collapses.** Matching Exhibit 21 subsidiaries to EIA utilities
  on shared company names alone adds 1,703 utilities, covering about **0.04% of
  `subsidiary_company_id_sec10k`** `[MEASURED]` — [PUDL modeling](https://docs.catalyst.coop/pudl/en/latest/methodology/sec10k_modeling.html.md)
- **Extraction itself is a vision problem before it is a linkage problem.** PUDL fine-tuned LayoutLMv3
  (`microsoft/layoutlmv3-base`) for NER over four labels (subsidiary name, location of incorporation,
  ownership percentage, other) on **160 labelled Exhibit 21 documents**, validated against **72 manually
  transcribed tables**, reaching **95% accuracy**; precision, recall and F1 are not reported. Filing years
  **2018–2022 were not extracted** `[MEASURED]` — [PUDL modeling](https://docs.catalyst.coop/pudl/en/latest/methodology/sec10k_modeling.html.md)
- **Filer→external-registry linkage needs a probabilistic record linker.** PUDL uses **Splink** for
  one-to-one probabilistic linkage of SEC filers to EIA utilities, keeping the highest-probability match, and
  could only run it on the most recent year because of memory limits — [PUDL modeling](https://docs.catalyst.coop/pudl/en/latest/methodology/sec10k_modeling.html.md)
- A third-party write-up of a sanctions-screening pipeline claims a rule-based Exhibit 21 extractor at **91%
  recall on a 400-document held-out test set**, with matching run in precision-ordered passes starting from
  exact normalised match `[REPORTED]`. Blog-style, no clear provenance, figures unverified —
  [ai-analytics.org](https://ai-analytics.org/writing/federal-entity-matching/)

**Beneficial-ownership chains through intermediate holding entities — PARTIALLY DECLARED, with a real latent residue.**
- GLEIF's **Level 2 relationship data** declares parent–child links directly. The October 2025 LEI Data
  Quality Report counts **2,892,126 LEIs — 93.13% of the LEI population — with a complete, verified set of
  parent details**, plus **135,451 parent relationships** and **150,774 fund relationships** `[MEASURED]` —
  [GLEIF LEI Data Quality Report, October 2025](https://www.gleif.org/media/pages/lei-data/gleif-data-quality-management/quality-reports/download-data-quality-report-october-2025/4749627653-1765550577/2025-11-07-lei-data-quality-report-october-2025.pdf)
- **But Level 2 is not beneficial ownership.** It records **accounting consolidation under IFRS or US-GAAP**,
  not ownership percentages or beneficial ownership, and it **excludes natural persons**; in the source data
  the start node is the child and the end node is the parent `[REPORTED]`, from a third-party API doc —
  [GDELT Cloud GLEIF reference](https://docs.gdeltcloud.com/api-reference/gleif.md);
  [GLEIF Level 2 relationships](https://docs.gdeltcloud.com/api-reference/reference-data/gleif-level-2-relationships.md)
- Historic coverage was far thinner: GLEIF's Q2 2017 business report found roughly **14% of entities reported
  a parent with an LEI** and about half reported having no parent. A Q3 2017 report's 99.5% direct-parent
  figure appears to count reporting exceptions as reported, so the two are not like-for-like `[MEASURED, with
  the comparability caveat]` —
  [GLEIF Q2 2017 blog](https://gleif.org/en/newsroom/blog/gleif-publishes-the-quarterly-global-lei-system-business-report-covering-the-second-quarter-of-2017);
  [GLEIF Q3 2017 business report](https://www.gleif.org/lei-data/global-lei-index/download-global-lei-system-business-reports/download-global-lei-system-business-report-q3-2017/2017-11-14_business_report_v1.0.pdf)

**Prose-named counterparties and the acronym problem — LATENT, and the one place a published retrieval benchmark admits it.**
- FinDER's authors built the benchmark specifically because professional queries "frequently include
  abbreviations, acronyms, and concise expressions," giving as their worked example **"MS trading revenue
  CAGR," where "MS" must be resolved to Morgan Stanley** — i.e. the hard step in their own financial retrieval
  benchmark is **entity resolution, not relation traversal** —
  [FinDER, arXiv:2504.15800](https://arxiv.org/html/2504.15800v2)

**Verdict table.** The declared/derivable/latent calls for the remaining families are `[MY REASONING]`
built on the already-established form-type findings, and are flagged as such in Inferences below, because I
found **no published measurement of any of them as a retrieval relation**.

### Inferences

All of the following are `[MY REASONING]`, not measured by any source I found. Each needs a verification step
named.

| Relation family | Call | Why |
|---|---|---|
| Entity resolution across filers/subsidiaries | **LATENT** | Measured above. Ex-21 has no CIK; 2%/19% coverage. |
| Supply-chain / customer-concentration named only in prose | **LATENT (but as an NER + linkage problem)** | The disclosure obligation attaches to the *percentage*, not the *name*: filings routinely say "Customer A accounted for X% of revenues." Where a name is given, connecting it to a filer is the Q2 problem again, not relation inference. **Verify** against ASC 280-10-50-42 and whether Compustat's Segment Customer file is the de facto gold. |
| Risk-factor cross-references | **DERIVABLE** | Intra-document pointers ("see Item 7") are regex-recoverable from the Item structure; cross-*company* risk-factor similarity is similarity, not a relation. |
| "Incorporated by reference" exhibit chains | **DECLARED** | SEC rulemaking mandated machine-readable exhibit hyperlinks and HTML format (Release 33-10322, 2017) and the FAST Act Regulation S-K modernisation (Release 33-10618, 2019). If that is right, the chain is an authored hyperlink and falls squarely inside the already-established authored-link null result. **Verify the two release numbers before publishing** — I could not retrieve either in this session. |
| Auditor and officer overlaps across companies | **DERIVABLE by identifier join** | Auditor name is a tagged XBRL DEI fact; insider officers carry their own **reporting-owner CIK** on Forms 3/4/5. A shared key exists, so this is a SQL join, not a latent edge. **Verify** `dei:AuditorName`/`dei:AuditorFirmId` tagging requirements and the Form 345 reporting-owner schema. |
| Beneficial-ownership chains | **PARTIALLY DECLARED** | SC 13D/G cover page plus GLEIF Level 2 at 93.13% verified parent details; the residue (natural persons, non-consolidation control, nominee layers) is genuinely latent but also **largely not in the filing text at all** — so it is not recoverable by graph expansion over this corpus either. |
| Restatement and amendment chains | **DECLARED** | The `/A` form-type suffix plus the Item 4.02 8-K are exactly the schema-implied family the prior note documents; recoverable by a short deterministic parser over the form-type field. |
| Filing → later enforcement action or news event | **LATENT, but cross-corpus** | SEC litigation releases and AAERs name respondents in prose without a CIK. The edge is real and useful, but building it is (a) entity resolution again and (b) a join across two corpora, not multi-hop retrieval within one. |

- **The honest headline for the series: finance fails the same test spectastic failed, for the same reason.**
  Where the relation is informative, it is usually carried by the form type, the structured cover page, an
  authored exhibit hyperlink, or a shared identifier. Where it is genuinely not recoverable that way, the
  obstacle is almost always **a name that needs resolving**, not **a relation that needs inferring**.
- The one structural difference from spectastic is worth stating: spectastic's gap was parser loss against a
  grammar that *was* written down; finance's gap is a missing crosswalk that *nobody* wrote down. That is a
  better problem, but it is a different problem.

### Gaps
- **No published measurement exists** for any of: "incorporated by reference" exhibit chains as retrieval
  edges; risk-factor cross-reference traversal; auditor/officer overlap edges; or filing→enforcement linkage.
  I searched for each and found only entity-resolution and extraction work. This is itself a finding: the
  finance NLP literature treats these as data-engineering problems, not retrieval-graph problems.
- SEC Releases 33-10322 and 33-10618 could not be retrieved (both are the kind of SEC PDF/HTML the prior note
  also failed to extract). The exhibit-hyperlink claim rests on them.
- No source gave a measured figure for what share of 10-K customer-concentration disclosures name the customer
  versus anonymising it as "Customer A." That number would settle the supply-chain row above.
- GLEIF's own licence page was not retrieved; the CC0 1.0 designation for the Golden Copy appears only in a
  third-party API doc. **Verify on gleif.org before shipping GLEIF data.**

---

## Q2. Is the genuine difficulty entity resolution rather than relation inference?

### Takeaway
**Yes, and it should be said plainly.** Every measured obstacle I found in finance reduces to record linkage:
no shared key between Exhibit 21 and EDGAR, exact-name matching achieving 2% subsidiary coverage, probabilistic
linkers (Splink) needed for filer↔registry joins, and a retrieval benchmark whose worked hard case is expanding
"MS" to Morgan Stanley. This is a **different technical problem with a different literature** — record
linkage, blocking, probabilistic matching — and it is a legitimately interesting one. It is also, critically,
**not a multi-hop retrieval problem**: once the crosswalk exists the graph is declared, and before it exists no
amount of traversal helps.

### Cited Findings
- All of the Q1 entity-resolution findings apply here and are not repeated: no CIK in Ex-21, 2%/19% match
  coverage, 0.04% subsidiary→utility coverage, Splink for probabilistic linkage, 160-document LayoutLMv3
  training set — [PUDL modeling](https://docs.catalyst.coop/pudl/en/latest/methodology/sec10k_modeling.html.md)
- The published approaches are **precision-biased by design**: PUDL states that many subsidiaries go unmatched
  but "there are unlikely to be subsidiaries that are erroneously matched to a CIK," and the third-party
  pipeline runs passes "ordered by precision, starting with exact normalized match" —
  [PUDL association table](https://data.catalyst.coop/pudl/core_sec10k__assn_exhibit_21_subsidiaries_and_filers);
  [ai-analytics.org](https://ai-analytics.org/writing/federal-entity-matching/)
- Practitioner literature frames the failure mode as **over-merging distinct entities**, and recommends
  confidence thresholds, deterministic identifiers where available, and manual review of ambiguous cases
  `[REPORTED]`, vendor sources only —
  [neo4j on entity resolution](https://neo4j.com/blog/what-is-entity-resolution/);
  [SymphonyAI glossary](https://www.symphonyai.com/glossary/entity-resolution);
  [AWS Entity Resolution accuracy measurement](https://aws.amazon.com/blogs/industries/measuring-the-accuracy-of-rule-or-ml-based-matching-in-aws-entity-resolution/)
- One directly relevant connection back to the existing report: the spectastic analysis already identified
  "the missing entity resolution" and "the open bug that silently discards distinct entities sharing a title"
  as live defects —
  [reports/Multi hop questions and graph retrieval.md](/Users/briancorbin/Code/rags/rags-spring-ai/reports/Multi%20hop%20questions%20and%20graph%20retrieval.md)

### Inferences
- `[MY REASONING]` **This is the strongest single recommendation in the note, and it is not a corpus
  recommendation.** The author has already found, in his own corpus, that entity resolution is the missing
  piece. The finance research independently finds the same thing with public numbers attached. An episode on
  *entity resolution for retrieval* — blocking, normalisation, precision-biased cascades, and what happens to
  Hit@k when two distinct entities share a surface form — is better evidenced than any multi-hop episode this
  research can support, and it reuses the existing qrels machinery directly.
- `[MY REASONING]` Entity resolution also has a property graph expansion lacks: **it is measurable against a
  crosswalk**. PUDL's 2%/19% is a retrieval-shaped number. A blog post could reproduce that arithmetic on a
  few hundred Exhibit 21 rows without crawling anything, since the gold crosswalk is the published association
  table.
- `[MY REASONING]` The trap to name explicitly in the episode: entity resolution *looks* like multi-hop
  because the symptom is "the system can't connect these two documents." The diagnosis is different, and
  GraphRAG does not fix it — an LLM extractor that mints a node per surface form makes it worse, which is
  exactly the ~30.5% node redundancy already measured in spectastic.

### Gaps
- I did not retrieve a canonical **record-linkage benchmark for company names** (no Magellan/DeepMatcher-style
  company-name benchmark surfaced in these searches). If the episode goes this way, that is the next search.
- No source quantified **how much retrieval recall improves** once a crosswalk is applied. That experiment
  appears not to have been published.

---

## Q3. Alternative corpora: what is latent, is there a gold set, what is the licence?

### Takeaway
**Case-law *treatment* is the only candidate where the latency of the relation is independently measured — and
it is measured as hard: the best LLM reaches 79.1% on a coarse treatment schema and 67.7% on a fine-grained
one.** Legislation and regulation are the opposite: superb bitemporality, but their amendment relations are
*declared* in editorial data, which would repeat the spectastic outcome. Patents, Wikipedia-derived sets and
open-source issue trackers all have authored citation/cross-reference layers and therefore fall inside the
already-established authored-link null result. Incident reports have genuinely latent causal structure and
**no released gold corpus**. One licence finding is decisive and easy to miss: **the UK's Find Case Law
service excludes computational analysis from its licence**, so the obvious UK legal corpus cannot ship in a
public retrieval repo.

### Cited Findings

**Case law and judicial opinions — latent *treatment* over authored citation.**
- The treatment relation is measurably latent. "Validate Your Authority" benchmarks LLMs on multi-label
  precedent treatment classification using an expert-annotated dataset of **239 real-world legal citations**,
  derived from Hellyer (2018)'s citator-evaluation corpus after excluding ambiguous relationships, leaving
  **329 high-confidence citing relationships**. Best results: **Gemini 2.5 Flash at 79.1% on the high-level
  task and GPT-5-mini at 67.7% on the fine-grained schema**; the authors propose an Average Severity Error
  metric `[MEASURED]` —
  [Demir & Canbaz, NLLP 2025](https://aclanthology.org/2025.nllp-1.13/);
  [arXiv:2605.17691](https://arxiv.org/pdf/2605.17691)
- The same authors criticise **CaseHOLD** — over **53,000 multiple-choice questions derived from US case law
  citations** — as a binary overruled-or-not task over a single holding sentence, lacking broader case context
  and fine-grained multi-label treatment —
  [arXiv:2605.17691](https://arxiv.org/pdf/2605.17691)
- An Australian Federal Court dataset on Kaggle carries citation classes indicating "the type of treatment
  given to the cases cited" `[REPORTED]`, provenance unverified —
  [baselight mirror](https://baselight.app/u/kaggle/dataset/shivamb_legal_citation_text_classification)
- A 2026 preprint on citation relevance uses **CLERC** and **BriefMe**, but targets whether a citation is
  substantive rather than its treatment `[REPORTED]` — [arXiv:2608.12571](https://arxiv.org/pdf/2608.12571)
- Broader task/dataset landscape for legal NLP —
  [NLP for the Legal Domain survey, arXiv:2410.21306](https://arxiv.org/pdf/2410.21306)

**Case law licences — the decisive split.**
- **Caselaw Access Project (US).** Full unrestricted release of nearly **7 million US state and federal court
  decisions on 1 March 2024**, when the 2018 Harvard/Ravel–LexisNexis agreement giving LexisNexis commercial
  control expired; Harvard's Library Innovation Lab continues hosting bulk data at case.law. Coverage excludes
  material not designated as officially published, i.e. most lower-court decisions —
  [LIL, "Transitions for the Caselaw Access Project," 26 Mar 2024](https://lil.law.harvard.edu/blog/2024/03/26/transitions-for-the-caselaw-access-project);
  [LawNext guest post, Mar 2024](https://www.lawnext.com/2024/03/guest-post-the-caselaw-access-project-then-now-tomorrow.html)
  — bulk layout (per-reporter directories plus `ReportersMetadata.json`, `VolumesMetadata.json`,
  `JurisdictionsMetadata.json`) confirmed at [static.case.law](https://static.case.law/)
- **CAP licence: unconfirmed.** A third-party mirror states "the Caselaw Access Project dataset is licensed
  under the CC0 License," but that is a repackaging, and CAP's own `about`/`docs` pages returned title-only
  content in this session `[REPORTED, flagged]` —
  [TeraflopAI/Caselaw_Access_Project](https://huggingface.co/datasets/TeraflopAI/Caselaw_Access_Project)
- **UK Find Case Law — NOT usable for this project.** The **Open Justice Licence v2.0** permits copying,
  publishing, distributing and transmitting the information, including commercially, with the attribution
  "Contains information licensed under the Open Justice - Licence v2.0." **But computational analysis,
  including indexing by search engines, is not covered by the licence and requires a separate licence.** It
  also requires removing documents that are no longer published or have been replaced by a revised version —
  [Open Justice Licence v2.0](https://caselaw.nationalarchives.gov.uk/open-justice-licence)

**Legislation and regulation — declared amendment effects, outstanding bitemporality.**
- `legislation.gov.uk` content is published under the **Open Government Licence v3.0**, "except where
  otherwise stated"; some content derived from EUR-Lex is reused under **Commission Decision 2011/833/EU**.
  The API offers consolidated UK legislation in several formats with **permanent URIs for items and for
  sections within them**, and search by title, year, number and type —
  [legislation.gov.uk/developer](https://www.legislation.gov.uk/developer)
- Point-in-time access is real and section-level: URLs carry a date and the page displays "Point in time view
  as at" that date, reachable via Advanced Features → Show Timeline of Changes or point-in-time advanced
  search. Dated pages carry status lines stating the text is "up to date with changes known to be in force on"
  a given date, flag provisions that are prospective, and announce when a version is superseded —
  [Companies Act 2006 s.790T as at 2023-12-01](https://www.legislation.gov.uk/ukpga/2006/46/section/790T/2023-12-01);
  [anaw 2019/4 s.38 as at 2020-05-06 (data.htm)](https://www.legislation.gov.uk/anaw/2019/4/section/38/2020-05-06/data.htm);
  [nisr 2014/189 reg.22 as at 2015-04-01, extent view](https://www.legislation.gov.uk/nisr/2014/189/regulation/22/2015-04-01?view=extent)
- **The declared/latent trap, explicitly:** changes not yet applied by the editorial team are visible only in
  the latest or prospective version and are therefore **not** accessible in a dated view — which means the
  amendment relation exists as *editorial data*, not as something to be inferred —
  [legislation.gov.uk point-in-time pages, as above](https://www.legislation.gov.uk/ukpga/2006/46/section/790T/2023-12-01)
- **EUR-Lex.** The site links to "the current consolidated version" and "a list of all versions of an act,
  with the current one marked in bold"; consolidated texts "have no legal effect" and are documentation only,
  though they form the basis for codification. **Cellar**, the repository behind EUR-Lex, exposes an open
  SPARQL endpoint requiring no key or account `[the SPARQL claim is REPORTED, from a third-party package
  README]` —
  [EUR-Lex developments 03/2020](https://eur-lex.europa.eu/content/development/development_2020-01.html);
  [EUR-Lex consleg collection](https://eur-lex.europa.eu/collection/eu-law/consleg.html?locale=it);
  [EUR-Lex developments 03/2022](https://eur-lex.europa.eu/content/development/eur-lex-developments-2022-03.html);
  [omnilex/eurlex README](https://packagist.org/packages/omnilex/eurlex)
- The only EUR-Lex licence statement I could source is second-hand: `legislation.gov.uk` states that its
  EUR-Lex-derived content is reused under **Commission Decision 2011/833/EU** —
  [legislation.gov.uk/developer](https://www.legislation.gov.uk/developer)

**Patents — citations authored, similarity latent but not a relation.**
- The **Harvard USPTO Patent Dataset (HUPD)** holds **over 4.5 million patent documents**, which its authors
  say is two to three times larger than comparable corpora. Its distinguishing feature is **inventor-submitted
  application versions rather than granted patents**, enabling study of patentability at filing time; its
  headline task is **patent acceptance prediction**, not prior-art retrieval. **Sources conflict on the date
  window**: the paper says applications filed 2004–2018, the Hugging Face card says January 2004 to December
  2014 — [HUPD, arXiv:2207.04043](https://arxiv.org/pdf/2207.04043);
  [NeurIPS 2023 D&B paper](https://papers.neurips.cc/paper_files/paper/2023/file/b4b02a09f2e6ad29fdbeb1386d68f4c4-Paper-Datasets_and_Benchmarks.pdf);
  [HUPD dataset card](https://huggingface.co/datasets/HUPD/hupd/blob/78e851b63e9d1e0b1325493f3c5eab05c2ab0759/README.md)
- **HUPD licence: `cc-by-sa-4.0`** per the Hugging Face metadata; I found **no licence statement in the paper
  itself** — [HUPD dataset card](https://huggingface.co/datasets/HUPD/hupd/blob/78e851b63e9d1e0b1325493f3c5eab05c2ab0759/README.md)

**Biomedical literature and clinical guidelines — genuinely latent contradiction and reversal, no retrieval gold.**
- Guideline conflict has been formalised as interactions between recommendations, with **contradiction** as
  one such interaction (one recommendation urging avoidance of an action another recommends), resolved by
  argumentation — [Resolving Conflicts in Clinical Guidelines using Argumentation, arXiv:1902.07526](https://arxiv.org/pdf/1902.07526)
- **Recommendation reversals are a documented empirical phenomenon**: a study of guidelines from **20
  gastroenterology societies published between 1990 and 2019** examines recommendations reversed when evidence
  shows them futile or unsafe `[MEASURED study design; I did not retrieve the reversal count]` —
  [Recommendation Reversals in Gastroenterology CPGs, PMC8972276](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC8972276/)
- Supersession in guidelines can be rule-based rather than latent: Singapore's MOH treats guidelines as
  **withdrawn five years after publication unless otherwise specified**, while warning that new evidence can
  supersede recommendations —
  [MOH withdrawn clinical practice guidelines](https://moh.gov.sg/hpp/all-healthcare-professionals/guidelines/GuidelineDetails/withdrawn-clinical-practice-guidelines-(medical))

**Incident reports and postmortems — latent causal structure, no released gold.**
- Maritime accident investigation reports: a causal-relation extraction study annotated only **four reports
  with 151 causal relations total**, expert-labelled, using connective-word matching scored by
  precision/recall/F `[MEASURED, and tiny]` — [arXiv:1507.02447](https://arxiv.org/pdf/1507.02447)
- Cloud service incident postmortems: a root-cause knowledge graph covering **1,072 symptoms, 1,915 root
  causes and 2,250 resolutions**, with the authors suggesting it could support organisation-wide post-mortem
  analysis of past incidents `[MEASURED]` — [arXiv:2204.11598](https://arxiv.org/pdf/2204.11598)
- Aviation safety: a study over roughly **140,000 NASA ASRS narratives** states explicitly that **the
  structured fields do not cover why the incident took place**, so the work relies on the free-text narratives
  — the cleanest published statement anywhere in this note that the informative relation is *not* in the
  metadata — [arXiv:1401.4436](https://arxiv.org/pdf/1401.4436)
- Retrieval over past incidents to rank likely root causes is an industrially-patented approach, indicating
  practical interest but not an open benchmark —
  [USPTO 11836037](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/11836037)
- Process-safety incident reports: a 2020 AIChE presentation extracts causes by topic modelling and
  unsupervised learning, concluding more training text is needed —
  [AIChE 2020 GCPS](https://aiche.org/academy/conferences/aiche-spring-meeting-and-global-congress-on-process-safety/2020/proceeding/paper/126c-extracting-causal-relations-incident-reports-natural-language-processing-and-topic-modeling)

**News archives, Wikipedia-derived sets, Enron, and open-source repositories.**
- News: **TREC-NEWS** has qrels (57 test queries, 595K documents, 19.6 relevant per query) but is **not
  publicly distributed** in BEIR; **Signal-1M (RT)** likewise non-public (97 queries, 2.86M documents) —
  [BEIR](https://github.com/beir-cellar/beir)
- Wikipedia-derived: **HotpotQA** in BEIR is 7,405 queries over a 5.23M-document corpus at 2.0 relevant
  documents per query — [BEIR](https://github.com/beir-cellar/beir). Its bridge edges are authored Wikipedia
  hyperlinks, which is precisely the authored-link condition already established as a null result in
  [multihop_external_evidence.md](/Users/briancorbin/Code/rags/rags-spring-ai/research_notes/Multi%20hop%20questions%20and%20graph%20retrieval/multihop_external_evidence.md)

### Inferences
- `[MY REASONING]` **Case law is the only candidate where latency is both plausible and measured.** The
  citation is authored (so the retrieval *candidate set* is cheap and reliable), but the **treatment** —
  followed, distinguished, limited, overruled — is in the prose, and the best current models get it wrong
  roughly one time in five on the coarse schema and one time in three on the fine schema. That is the shape
  the author wants: a relation that exists, matters for answering, and is demonstrably not recoverable from
  metadata, a hyperlink, a citation string, or an identifier regex.
- `[MY REASONING]` **Legislation would repeat the spectastic result almost exactly.** Its amendment relations
  are editorial data with in-force dates attached — a declared graph, maintained by professionals, with the
  undeclared residue being precisely the "not yet applied" backlog that is *also* published as a list. Using
  it for a latent-relation episode would be the second instalment of the same null result. Using it for a
  *bitemporal* episode would be excellent (Q5).
- `[MY REASONING]` **Open-source codebases plus issue trackers fail the latency test for a mechanical reason**:
  `#123` references, `Fixes #` commit trailers and PR-to-issue links are authored and machine-readable by
  regex. The latent part is duplicate-issue identity and issue↔root-cause — which is, again, entity resolution
  and causal inference, not relation traversal. I found no open gold retrieval set for either, so this would
  be hand-built from scratch with no comparison point.
- `[MY REASONING]` **Enron is the weakest serious candidate**: no revision history at all (so the bitemporal
  machinery is discarded), no passage-level gold retrieval set that I could find, and a licence status that is
  "published without a formal licence" rather than an open licence — unacceptable under this project's
  redistributability constraint. I deliberately did not search further on it, because the bitemporality
  failure alone is disqualifying.
- `[MY REASONING]` **Incident reports are the most intellectually honest latent-relation corpus and the least
  practical one.** The ASRS finding — the structured fields do not say why — is the exact thesis. But the
  largest annotated causal set I found is 151 relations over four documents, there is no qrels-style gold
  anywhere, and the richest corpora (internal postmortems) are not redistributable.

### Gaps
- **eCFR and Federal Register: no primary source obtained** (bot protection). Point-in-time behaviour, API,
  bulk XML and copyright status are all unverified. Likely strong on bitemporality and licence (US Government
  works), but I will not assert it.
- **EUR-Lex reuse licence not verified directly.** Two fetches of the legal notice returned empty bodies.
- **CAP's own licence text not obtained.** CC0 is secondary-sourced only.
- No licence citation obtained in this session for: Wikipedia dumps, the Enron email corpus, PatentsView, or
  the US Code. Omitted rather than guessed.
- The gastroenterology reversal study's actual reversal *count* was not retrieved, only the study design.

---

## Q4. Which candidates have a gold set with passage-level relevance judgments?

### Takeaway
**None of the genuinely-latent candidates has a usable qrels-style passage-level gold set, and every corpus
that does have one is either relationally flat or authored-link-based.** This is the sharpest trade in the
whole assessment. BEIR's small collections (NFCorpus at 3.6K documents, SciFact at 5K, ArguAna at 8.67K) are
the right *size* and have real qrels, but none of them has cross-document relational structure worth
expanding. The finance retrieval benchmark closest to this project's methodology, **FinDER, is not publicly
released**.

### Cited Findings

**BEIR — what actually exists, with sizes.** Repository licence **Apache-2.0**; per-dataset licences are
**not stated**, and the maintainers say explicitly that they "do not vouch for their quality or fairness, or
claim that you have license to use the dataset" and that "it remains the user's responsibility to determine
whether you as a user have permission to use the dataset." The page is internally inconsistent on how many
datasets it contains (17 / 18 / "15+"). All figures from
[BEIR](https://github.com/beir-cellar/beir):

| Collection | Public | Test queries | Corpus | Avg relevant docs/query |
|---|---|---|---|---|
| **NFCorpus** (biomedical) | Yes | 323 (across splits) | **3.6K** | 38.2 |
| **SciFact** | Yes | 300 (across splits) | **5K** | 1.1 |
| **ArguAna** | Yes | 1,406 | **8.67K** | 1.0 |
| **SCIDOCS** | Yes | 1,000 | 25K | 4.9 |
| **FiQA-2018** (finance) | Yes | 648 (across splits) | 57K | 2.6 |
| **TREC-COVID** | Yes | 50 | 171K | **493.5** |
| Touché-2020 | Yes | 49 | 382K | 19.0 |
| CQADupStack | Yes | 13,145 | 457K | 1.4 |
| Quora | Yes | 10,000 (across splits) | 523K | 1.6 |
| Robust04 | **No** | 249 | 528K | 69.9 |
| **TREC-NEWS** | **No** | 57 | 595K | 19.6 |
| **HotpotQA** | Yes | 7,405 (across splits) | 5.23M | 2.0 |
| FEVER | Yes | 6,666 (across splits) | 5.42M | 1.2 |
| Climate-FEVER | Yes | 1,535 | 5.42M | 3.0 |
| DBPedia | Yes | 400 (across splits) | 4.63M | 38.2 |
| NQ | Yes | 3,452 (across splits) | 2.68M | 1.2 |
| **BioASQ** | **No** | 500 (across splits) | 14.91M | 4.7 |
| Signal-1M (RT) | **No** | 97 | 2.86M | 19.6 |
| MSMARCO | Yes | 6,980 (across splits) | 8.84M | 1.1 |

BEIR does **not** state whether judgments are binary or graded for any dataset.

**Legal retrieval gold.**
- **CLERC** (Case Law Evaluation Retrieval Corpus) was built with legal professionals for two tasks:
  retrieving the citations behind a piece of legal analysis, and generating the analysis from those cases.
  **Zero-shot IR models reach 48.3% recall@1000** `[MEASURED]`, which the authors present as evidence that
  current approaches struggle. GPT-4o produced the highest ROUGE F-scores and hallucinated the most. The arXiv
  page does **not** state corpus size, the number of queries, how judgments were built, or a dataset licence;
  the source is described only as "a large open-source legal corpus" (the Caselaw Access Project is
  **not confirmed** from that page) — [CLERC, arXiv:2406.17186](https://arxiv.org/abs/2406.17186)
- **CaseHOLD**: over **53,000 multiple-choice questions derived from US case law citations** — a
  classification benchmark, not a retrieval one —
  [arXiv:2605.17691](https://arxiv.org/pdf/2605.17691)
- **Treatment gold**: the only expert-annotated treatment set found is **239 citations / 329 high-confidence
  citing relationships** — [Demir & Canbaz, NLLP 2025](https://aclanthology.org/2025.nllp-1.13/)

**Finance retrieval gold.**
- **FinanceBench**: **10,231 questions** about publicly traded companies, each with an answer and an
  **evidence string**. Headline result: **GPT-4-Turbo with a retrieval system incorrectly answered or refused
  81% of questions**; 16 model configurations were tested on a **150-case sample** with **2,400 answers
  manually reviewed**. The paper carries **CC BY-NC-ND 4.0**; the abstract says the cases are "available
  open-source" via Hugging Face, but the arXiv page does **not** say whether the open release covers all
  10,231 questions or only the 150-case sample, nor whether the dataset shares the paper's licence
  `[MEASURED, with the licence/coverage ambiguity flagged]` —
  [FinanceBench, arXiv:2311.11944](https://arxiv.org/abs/2311.11944)
- **FinDER**: **5,703 expert-annotated query–evidence–answer triplets** over S&P 500 10-Ks, annotated by two
  domain experts (an investment-bank analyst and a CPA) who cross-validated each other. Topic mix includes
  Company Overview 18.95%, Financials 17.36%, Footnotes 16.71%, Governance 12.59%. **Availability is the
  problem**: the paper states the authors used it for an official evaluation with partners and "plan to
  release it publicly at a later time," directing interested parties to contact LinqAlpha. **I found no public
  qrels file** — [FinDER, arXiv:2504.15800](https://arxiv.org/html/2504.15800v2);
  [alphaxiv benchmark page](https://alphaxiv.org/benchmarks/yonsei-university/finder);
  [ICLR 2025 record](https://www.iclr.cc/virtual/2025/33836)
- **DocFinQA**: **1,236 documents and 7,612 QA pairs**, documents averaging about **123,000 words**, extending
  FinQA with full SEC reports; samples contain a full document, question, answer and an interpretable Python
  program `[REPORTED, from a review site summarising the paper]` —
  [Liner review of DocFinQA](https://liner.com/review/docfinqa-longcontext-financial-reasoning-dataset);
  [arXiv:2401.06915](https://arxiv.org/pdf/2401.06915v1)
- **FinQA**: **8,281 QA pairs derived from 10-K filings**, covering single-step and multi-hop reasoning, per
  the FinGEAR paper — [FinGEAR, arXiv:2509.12042](https://arxiv.org/pdf/2509.12042)
- **Fin-RATE**: 17 LLMs benchmarked on **7,500 expert-curated QA pairs from 2,472 SEC filings** `[REPORTED,
  secondary]` — [beancount.io financial-reporting tag](https://beancount.io/bean-labs/research-logs/tags/financial-reporting)
- Note a contradiction in the secondary sources: one blog claims DocFinQA extends FinanceBench with harder
  multi-hop questions, while the paper summaries point to **FinQA** as the base. Treat the lineage claim as
  wrong — [beancount.io DocFinQA log](https://beancount.io/bean-labs/research-logs/2026/06/20/docfinqa-long-context-financial-reasoning-dataset)

### Inferences
- `[MY REASONING]` **Named candidates that would actually serve as a drop-in qrels corpus, ranked by fit to
  this project's 10,540-unit scale:** NFCorpus (3.6K documents, 38.2 relevant per query — by far the best
  size/judgment-density match), SciFact (5K, 1.1), ArguAna (8.67K, 1.0), SCIDOCS (25K, 4.9). TREC-COVID is
  attractive for its 493.5 relevant documents per query but its 171K corpus is 16x the current scale, and the
  multihop evidence note's small-corpus failure modes run the other way.
- `[MY REASONING]` **None of those four has cross-document relational structure worth expanding.** NFCorpus is
  query–document relevance over nutrition articles; SciFact is claim verification; ArguAna is
  counter-argument retrieval; SCIDOCS is citation-based but its citations are authored. So the qrels-ready
  corpora and the latent-relation corpora are **disjoint sets** in everything I surveyed.
- `[MY REASONING]` **FinanceBench's 81% failure rate is the most quotable finance number available, and it is
  an answer-quality number, not a retrieval number** — exactly the metric-discipline problem already
  documented in the multihop note. Its "evidence string" annotation is also not confirmed to be passage-aligned
  in a way qrels could consume. Do not present it as retrieval evidence.
- `[MY REASONING]` For a ~100-question hand-built gold set, **CLERC's 48.3% recall@1000 is the single most
  useful external anchor in this note**: it establishes that legal retrieval is hard at a published, retrieval-metric
  number, so a hand-built case-law gold set would sit against a real baseline rather than in a vacuum.

### Gaps
- BEIR does not state binary vs graded judgments; I could not confirm per-collection judgment scales.
- CLERC's corpus size, query count and licence are all unstated on the arXiv abstract page. **These need the
  paper PDF or the project repository before committing to CLERC.**
- No TREC CAsT details were obtained in this session. No TREC legal-track collection surfaced in these
  searches with licence and size attached. Both remain open.
- FinanceBench's open-release scope and dataset licence are genuinely ambiguous from the primary page.

---

## Q5. Bitemporality: which candidates have genuine valid time and transaction time?

### Takeaway
**UK legislation via legislation.gov.uk is the clear winner and the only candidate where both axes are
explicit in the published data**: in-force dates give valid time, and the editorial "changes known to be in
force on <date>" / "not yet applied" distinction gives transaction time. Case-law treatment is a strong and
underexploited second — a precedent's authority *as known* in 1990 differs from its authority *as known* in
2026, which is a genuine "as known" axis over an immutable text. SEC filings are third and already understood
by this project.

### Cited Findings
- **legislation.gov.uk — both axes, at section granularity.** Dated section URLs render a "Point in time view
  as at" that date (valid time), and status lines state the text is "up to date with changes known to be in
  force on" a given date (transaction time). Crucially, **changes not yet applied by the editorial team are
  visible only in the latest or prospective version and are not accessible in a dated view** — which is
  exactly a transaction-time boundary, and the site says so. Superseded versions carry notices pointing to the
  later version, and some dated views flag provisions that are prospective, with commencement orders listed
  as not yet applied —
  [Companies Act 2006 s.790T as at 2023-12-01](https://www.legislation.gov.uk/ukpga/2006/46/section/790T/2023-12-01);
  [uksi 1996/207 reg.3F as at 2001-03-19](https://www.legislation.gov.uk/uksi/1996/207/regulation/3F/2001-03-19);
  [asp 2020/13 sch.1 para.17A as at 2025-04-01](https://www.legislation.gov.uk/asp/2020/13/schedule/1/paragraph/17A/2025-04-01);
  [anaw 2019/4 s.38 as at 2020-01-01](https://www.legislation.gov.uk/anaw/2019/4/section/38/2020-01-01/data.htm)
- **Permanent section-level URIs** make the addressing problem tractable rather than incidental —
  [legislation.gov.uk/developer](https://www.legislation.gov.uk/developer)
- **EUR-Lex — valid time strong, transaction time weaker.** EUR-Lex links the current consolidated version and
  "a list of all versions of an act, with the current one marked in bold," but consolidated texts "have no
  legal effect" and only texts published in the Official Journal are authentic —
  [EUR-Lex developments 03/2020](https://eur-lex.europa.eu/content/development/development_2020-01.html);
  [EUR-Lex consleg](https://eur-lex.europa.eu/collection/eu-law/consleg.html?locale=it);
  [omnilex/eurlex README](https://packagist.org/packages/omnilex/eurlex)
- **Case law treatment over time** is inherently a transaction-time phenomenon: the Demir & Canbaz dataset
  exists because citators disagree about treatment, and Hellyer (2018) empirically evaluated **commercial
  citators** against each other — i.e. "what was known about this case's authority" is a time-varying,
  provider-varying quantity — [arXiv:2605.17691](https://arxiv.org/pdf/2605.17691)
- **Clinical guidelines** have real valid time: rule-based lapse (withdrawn five years after publication
  unless otherwise specified) plus evidence-driven supersession —
  [MOH Singapore](https://moh.gov.sg/hpp/all-healthcare-professionals/guidelines/GuidelineDetails/withdrawn-clinical-practice-guidelines-(medical))
  — and documented reversals across 20 societies, 1990–2019 —
  [PMC8972276](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC8972276/)
- **Open Justice Licence's revision clause** is a bitemporality signal in licence form: licensees must remove
  documents that "have been replaced by a revised version," meaning the service itself maintains revised
  judgment versions —
  [Open Justice Licence v2.0](https://caselaw.nationalarchives.gov.uk/open-justice-licence)

### Inferences

`[MY REASONING]` Ranking the candidates on bitemporality, best first:

1. **UK legislation (legislation.gov.uk)** — both axes explicit, section-granular, permanent URIs, OGL v3.0.
   Nothing else is close. It preserves the project's bitemporal machinery exactly.
2. **US case law treatment (CAP + a treatment layer)** — the opinion text is immutable (clean valid time), the
   treatment accrues over decades (genuine "as known"), and the disagreement between citators makes
   transaction time *interesting* rather than bookkeeping. This is the one candidate where bitemporality and
   latency coincide.
3. **EUR-Lex consolidated acts** — version lists and consolidation dates, but the "no legal effect"
   disclaimer and the Official-Journal-only authenticity rule make the transaction-time axis awkward to
   motivate for a reader.
4. **SEC filings** — filing date vs period-of-report is a genuine two-axis structure and amendments are
   declared, but the project has already concluded this family is schema-implied, so it adds bitemporality
   without adding latency.
5. **Clinical guidelines** — real valid time, weak transaction time (no published "as known on" axis), and
   assembling versions means collecting from many societies with many licences.
6. **Documentation versions** — valid time only; and this is effectively spectastic again.
7. **Federal Register / eCFR** — probably belongs at rank 2 or 3 on the strength of effective dates and
   historical point-in-time access, but **I have no primary source** and will not place it.
8. **Incident reports, news archives, patents, Wikipedia, Enron, OSS issue trackers** — no recoverable
   dual-axis revision history. (Wikipedia has full revision history, but its relations are authored
   hyperlinks, so it fails Q1; OSS repos have git history, which is transaction time over code, not over
   retrievable document versions.)

### Gaps
- eCFR / Federal Register unverified (bot protection), as above. This is the most consequential gap in this
  section, because US regulation would pair naturally with US case law on licence grounds.
- I found no source quantifying **how many** UK legislation sections have more than one point-in-time version,
  which would determine whether a scoped legislation corpus actually exercises the bitemporal machinery or
  merely permits it.
- No source confirms whether CAP's bulk data includes subsequent-treatment metadata (I expect not — treatment
  is a commercial citator product), which is why the treatment layer would have to be hand-built.

---

## Q6. The combination that would actually work

### Takeaway
**No single corpus satisfies all five constraints, and the constraint that has to give is the gold set.** The
qrels-ready corpora and the genuinely-latent corpora are disjoint in everything I surveyed: every candidate
with passage-level relevance judgments has either authored links or no relational structure, and every
candidate with genuinely latent relations requires a hand-built gold. My recommendation is **US case law (a
scoped Caselaw Access Project subset) with a hand-annotated treatment layer**, with **UK legislation
(legislation.gov.uk, OGL v3.0)** as the safe second and the bitemporal showcase — and an explicit warning that
**finance should be reframed as an entity-resolution episode, not a multi-hop one.**

### Cited Findings
Consolidated from above; the five constraints scored against each serious candidate.

| Candidate | Latent relations | Bitemporal | Licence (exact) | Size fit | Gold set |
|---|---|---|---|---|---|
| **US case law (CAP subset) + treatment** | **Yes, measured: 79.1% / 67.7% best-LLM** ([arXiv:2605.17691](https://arxiv.org/pdf/2605.17691)) | Good ("as known" authority) | **Unconfirmed**; CC0 per third-party mirror ([HF mirror](https://huggingface.co/datasets/TeraflopAI/Caselaw_Access_Project)); commercial restriction expired 1 Mar 2024 ([LIL](https://lil.law.harvard.edu/blog/2024/03/26/transitions-for-the-caselaw-access-project)) | ~7M cases → **must subsample** | **None at passage level.** CLERC exists as a baseline anchor at 48.3% recall@1000 ([arXiv:2406.17186](https://arxiv.org/abs/2406.17186)) |
| **UK legislation (legislation.gov.uk)** | **No — amendment effects are editorial data** ([dated views](https://www.legislation.gov.uk/ukpga/2006/46/section/790T/2023-12-01)) | **Best in class, both axes** ([developer](https://www.legislation.gov.uk/developer)) | **Open Government Licence v3.0**, except where otherwise stated ([developer](https://www.legislation.gov.uk/developer)) | Scopable to one Act family | None found |
| **UK Find Case Law** | Yes (same as US) | Revised versions maintained | **Open Justice Licence v2.0 — computational analysis, including search indexing, NOT covered; separate licence required** ([licence](https://caselaw.nationalarchives.gov.uk/open-justice-licence)) | Scopable | None found |
| **EUR-Lex** | No (citation authored, amendments declared) | Good, with "no legal effect" caveat ([consleg](https://eur-lex.europa.eu/collection/eu-law/consleg.html?locale=it)) | **Unverified**; reuse under **Commission Decision 2011/833/EU** per [legislation.gov.uk](https://www.legislation.gov.uk/developer) | Scopable | None found |
| **SEC filings** | **Mostly no** (see Q1) | Yes (filing date vs period) | US Government work — **not verified in this session** | Scopable | FinanceBench 10,231 Qs, CC BY-NC-ND 4.0 on the paper, release scope ambiguous ([arXiv:2311.11944](https://arxiv.org/abs/2311.11944)); FinDER **not public** ([arXiv:2504.15800](https://arxiv.org/html/2504.15800v2)) |
| **Patents (HUPD)** | Citations authored; similarity is not a relation | Application→grant only | **cc-by-sa-4.0** per dataset card ([card](https://huggingface.co/datasets/HUPD/hupd/blob/78e851b63e9d1e0b1325493f3c5eab05c2ab0759/README.md)) | 4.5M+ → subsample | No retrieval gold found; headline task is acceptance prediction ([arXiv:2207.04043](https://arxiv.org/pdf/2207.04043)) |
| **Clinical guidelines** | **Yes** (contradiction, reversal) ([arXiv:1902.07526](https://arxiv.org/pdf/1902.07526); [PMC8972276](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC8972276/)) | Valid time yes, transaction time weak | Per-society, heterogeneous — **not researched** | Good | None; NFCorpus (3.6K, 38.2 rel/q) is adjacent but not relational ([BEIR](https://github.com/beir-cellar/beir)) |
| **Incident reports** | **Yes, best-stated thesis**: ASRS structured fields "do not cover why" ([arXiv:1401.4436](https://arxiv.org/pdf/1401.4436)) | No | Varies | Good | **None**; largest annotated causal set is 151 relations over 4 documents ([arXiv:1507.02447](https://arxiv.org/pdf/1507.02447)) |
| **News archives** | Event coreference latent | Weak | Varies | Varies | TREC-NEWS has qrels but is **not publicly distributed** ([BEIR](https://github.com/beir-cellar/beir)) |
| **Wikipedia-derived (HotpotQA)** | **No — authored hyperlinks** ([BEIR](https://github.com/beir-cellar/beir)) | Revision history exists | — | 5.23M | Yes, but it is the established null condition |
| **Enron** | Social/organisational, latent | **No revision history** | No open licence identified | Good | None found |
| **OSS code + issue trackers** | Cross-refs authored (`Fixes #`) | git history, not document versions | Varies | Good | None found |

### Inferences

- `[MY REASONING]` **Primary recommendation: a scoped Caselaw Access Project subset with a hand-built treatment
  layer.** It is the only candidate that clears the latency bar on *measured* evidence rather than on a
  plausible mechanism, and the treatment relation has the exact property the brief demands: it is not in
  structured metadata, not in an authored hyperlink, not recoverable from the citation string, and not
  reachable by an identifier regex — the citation tells you *that* case B was cited, and says nothing about
  whether case A followed it or gutted it. The bitemporal angle survives intact and gets *better*: "as of" is
  the opinion's date, "as known" is the state of the case's authority at a chosen date, and the two genuinely
  diverge.
- `[MY REASONING]` **What has to give: the gold set.** Budget for hand-building ~100 questions and expect the
  annotation to be the expensive part — the only published expert treatment set is 239 citations, which tells
  you what expert annotation at this scale costs. Two mitigations: (a) CLERC's 48.3% recall@1000 gives an
  external baseline so the hand-built numbers are not unanchored; (b) CaseHOLD's 53,000 overruled-or-not items
  can seed *question generation* even though they are classification items, not qrels.
- `[MY REASONING]` **Second recommendation, and the honest hedge: pair the case-law corpus with US statutory or
  regulatory text rather than with UK legislation.** The statute/regulation → interpreting-case relation is
  genuinely latent (a judgment construing a provision rarely links to it), and keeping both sides inside US
  federal/state government works keeps the licence story single-jurisdiction. **UK Find Case Law cannot be
  used** — the Open Justice Licence v2.0 excludes computational analysis, which is the whole project — so a
  UK legislation + UK case law pairing is off the table even though it would otherwise be the most natural fit
  in the world.
- `[MY REASONING]` **Use UK legislation for the bitemporality episode and say so.** It is the best bitemporal
  corpus available at any size, under a named open licence (OGL v3.0), with section-level permanent URIs, and
  it is scopable to a few thousand retrievable units by picking one Act and its amending instruments. Just do
  not frame it as a latent-relation corpus: its amendment graph is declared, and claiming otherwise would be
  the spectastic error repeated with a new corpus.
- `[MY REASONING]` **Reframe, do not abandon, finance.** The finance research in Q1/Q2 is not a dead end; it is
  an episode about the wrong thing being blamed. "Why your multi-hop failure is actually an entity-resolution
  failure," with PUDL's 2%-of-subsidiaries-matched-to-19%-of-filers as the opening number and the project's own
  node-redundancy and shared-title bug as the local evidence, is better supported by measurement than any
  graph-expansion episode this research can justify.
- `[MY REASONING]` **One combination I would reject explicitly**: picking NFCorpus or SciFact because they fit
  the size and have qrels. They would make the metrics easy and the episode empty — neither has relations to
  expand, so the result would be a third null in a row, and this time a self-inflicted one.

### Gaps
- **The single most important unresolved item: CAP's actual licence.** If CAP is not CC0 or public domain, the
  primary recommendation needs a redistribution plan (ship question IDs and hashes rather than text), and the
  report should say so conditionally rather than asserting redistributability.
- Whether CAP bulk data can be scoped to a coherent few-thousand-document subset (one jurisdiction, one
  subject area, one date window) with enough internal citation density to make expansion meaningful. Not
  answerable without inspecting the data, which this task's no-bulk-fetch constraint rules out.
- eCFR / Federal Register remain unverified, which is why the statute-pairing suggestion above is labelled
  reasoning and not a sourced recommendation.
- No source anywhere measures the **retrieval** contribution of treatment relations — only their
  **classification** difficulty. So the episode's central experiment would be, as far as this research can
  tell, genuinely unpublished. That is an opportunity and a risk, and it is the same shape as the headline gap
  already recorded in `multihop_external_evidence.md`.
