# Absence and cardinality: the formal and empirical basis for discovering an undisclosed supplier link

Research current as of 2026-10-29. Every figure below carries a URL. Where I could not verify
something, it is in a **Gaps** block with the URL I tried, not asserted in the findings.

**Metric hygiene note for the writer.** Three different families of numbers appear below and must
never be mixed in the prose:

- **Retrieval metrics** — pairwise accuracy, RR, R@1, MRR, nDCG. Measure whether a ranker puts the
  right passage above the wrong one. NevIR and ExcluIR numbers are all of this kind.
- **Link-prediction metrics** — MRR, Hits@k (filtered), precision@k over predicted edges. Measure
  whether a model scores a held-out *edge* above corrupted alternatives. KGE and rule-mining
  numbers are of this kind.
- **Extraction / answer-quality metrics** — precision, recall, F1, audit-confirmation rate on
  extracted triples or on a final answer. CINEX's 60% precision and the supply-chain
  audit-confirmation figure are of this kind.

A retrieval score says nothing about whether the elimination answer is right, and vice versa.

---

## 1. Name the task properly

### Takeaway

The task has no single established name, but its formal core is well named in the knowledge-base
literature: it is **negation (or negative-statement) inference under a partial closed-world
assumption, where the local closure is licensed by a cardinality assertion**. The nearest formal
relatives, in descending order of fit, are (a) completeness/recall assessment via cardinality
assertions, (b) constraint satisfaction / model enumeration with a cardinality constraint, (c)
bipartite link prediction under incomplete observation, and (d) truth discovery. "Set-complement
inference" and "elimination" are accurate descriptions of the *answer shape* but are not terms of
art in this literature.

### Cited Findings

- The authoritative framing is Razniewski, Arnaout, Ghosh and Suchanek, **"Completeness, Recall,
  and Negation in Open-World Knowledge Bases: A Survey"**, arXiv:2305.05403, submitted 9 May 2023,
  v2 revised 6 December 2023; journal reference listed as "Under review, 2022". Its five declared
  areas are: logical foundations of representation and querying **under partial closed-world
  semantics**; estimating completeness from statistical patterns; extracting recall information
  from KBs and text; **identifying interesting negative statements**; and relaxed notions of
  relative recall — [arXiv abstract page](https://arxiv.org/abs/2305.05403)
- The survey gives the chain that *is* this task, in §2.4: "**cardinality assertions allow to
  evaluate completeness**", and then "**Completeness, in turn, enables establishing negation**" —
  with the worked example that if a list of advisees is complete, unlisted people are not advisees
  — [ar5iv full text, §2.4](https://ar5iv.labs.arxiv.org/html/2305.05403)
- The survey's §2.4 definition of a cardinality assertion: it fixes the number of matching tuples
  in the *ideal* KB, and recall is the KB count divided by that number. If the KB count already
  equals the asserted number, completeness follows —
  [ar5iv, §2.4](https://ar5iv.labs.arxiv.org/html/2305.05403)
- §5.1 states the elimination step explicitly: a count that matches the enumerated objects
  "**indicates that for this subject-predicate pair, the list of objects is complete**", so
  unlisted objects are implicitly negated —
  [ar5iv, §5.1](https://ar5iv.labs.arxiv.org/html/2305.05403)
- The survey separates **counting predicates** (e.g. `numberOfChildren`, store a number) from
  **enumerating predicates** (e.g. `hasChild`, store the objects), and treats aligning the two as a
  task in its own right (§4); the alignment system is CounQER —
  [ar5iv, §4](https://ar5iv.labs.arxiv.org/html/2305.05403);
  [CounQER paper](https://arxiv.org/pdf/2005.03529)
- The "4 of 5" claim is formally a **counting quantifier**: a statement about *how many* objects a
  subject has in a relation, **without naming them**. Mirza, Razniewski, Darari and Weikum,
  "Enriching Knowledge Bases with Counting Quantifiers", **ISWC 2018**, define it that way and note
  such quantifiers "are neglected by prior work" —
  [arXiv:1807.03656](https://arxiv.org/pdf/1807.03656);
  [ISWC 2018 session page](https://iswc2018.semanticweb.org/sessions/enriching-knowledge-bases-with-counting-quantifiers/index.html)
- **Entity set expansion** is a poor fit, and its own literature says why: it has positive seeds and
  **no labelled negatives**, and papers resort to sampling random vocabulary entities as negatives
  ("One challenge of learning the set expansion model is the lack of supervision signals") —
  [SynSetExpan, arXiv:2009.13827](https://arxiv.org/pdf/2009.13827). Set-CoExpan notes that
  "due to no negative sets provided explicitly, these methods suffer from semantic drift" —
  [Set-CoExpan, arXiv:2001.10106](https://fugumt.com/fugumt/paper_check/2001.10106v1_enmode)
- **Cardinality bound mining** is the adjacent sub-literature: Galárraga et al., "Predicting
  Completeness in Knowledge Bases", arXiv:1612.05786, learns rules of the form "fewer than N values
  for a property implies the KB is incomplete for it" via **"Add Cardinality Constraint"** and
  **"Tighten Cardinality Constraint"** operators in AMIE, and reports completeness predictable for
  some relations with up to **100% precision** on YAGO and Wikidata —
  [arXiv:1612.05786](https://arxiv.org/pdf/1612.05786)

### Inferences

- The correct name to use in the blog is **"cardinality-licensed negation inference"** or, if the
  author wants a phrase a reviewer will recognise, **"negative-statement inference under a partial
  closed-world assumption, closed locally by a counting quantifier"**. The Razniewski survey is the
  single citation that covers both halves.
- Because the answer is "which one is *excluded*", the task decomposes into two formally distinct
  steps, and the author should keep them separate in the writing: (i) **align a counting quantifier
  to an enumerating predicate** (CounQER's task); (ii) **enumerate the models consistent with the
  count** (a CSP / model-enumeration task). Step (ii) is where the technique choice in §4 bites.
- **Bipartite link prediction** and **KG completion** describe the *evidence* side correctly (5
  candidate edges, scattered support) but neither has machinery for a hard global count, so they
  are the right name for only half the task.
- **Truth discovery** is the wrong frame: the sources here are not making conflicting claims about
  the same proposition, they are silent. Silence is not conflict.

### Gaps

- I found **no paper that names this exact composite task** (aggregate cardinality claim on one side
  + per-entity evidence on the other + identify the complement). The closest named thing remains
  "negation inference under PCWA". The author can honestly say the composite is unnamed.
- The survey's §6 and §7 (its consolidated practitioner recommendations) were beyond the fetch
  window — [ar5iv full text](https://ar5iv.labs.arxiv.org/html/2305.05403), read at offset 0–100000
  of 191220 characters. Re-fetch with `offset: 100000` to get them.

---

## 2. The open-world problem, and how a cardinality claim closes it

### Takeaway

This is indeed the crux, and the survey states the hinge almost verbatim. Under the open-world
assumption a missing edge is *unknown*, not false; the standard partial fixes (CWA, LCWA, PCA) are
all heuristic and all known to misfire. A cardinality assertion is categorically different: it is
not a heuristic closure, it is a **stated fact about the size of the ideal set**, and it is exactly
what the survey identifies as licensing completeness and therefore negation. **The author's
instinct is correct and citable.**

### Cited Findings

- **CWA** (§2.3): "statements that are not in the KB are false in the real world". **OWA** (§2.3):
  absence means "the truth of these statements is unknown" —
  [ar5iv, §2.3](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **Partial closed-world assumption (PCWA)** (§2.3): "some parts of the KB are treated under
  closed-world semantics", the rest open-world —
  [ar5iv, §2.3](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **Partial completeness assumption (PCA)** (§2.3): if a subject has at least one object for a
  predicate, "there are no other objects in the real world beyond those that are in the KB". The
  survey explicitly calls the PCA "a generic assumption that **cannot mark specific regions as
  complete or incomplete**" — [ar5iv, §2.3](https://ar5iv.labs.arxiv.org/html/2305.05403)
- PCA's origin: it was introduced for rule mining because classical ILP "either need explicit
  negative examples or assume a closed world… and they do not account for the Open World Assumption
  (OWA) made by current KBs"; the PCA was "explicitly designed for KBs" and is used to compute rule
  confidence (the PCA confidence) — AMIE / AMIE+, Galárraga et al., **WWW 2013** —
  [WWW 2013 proceedings PDF](https://archives.iw3c2.org/www2013/proceedings/p413.pdf)
- PCA's known failure mode, which matters directly here: it "does not universally hold, and treats
  cases inappropriately when edges in a graph are **randomly missing**" —
  [RuDaS, arXiv:1909.07095](https://arxiv.org/pdf/1909.07095)
- The survey's own warning (§5.3): in loosely structured KBs the PCA is "**not a very good idea**"
  because general predicates can take hundreds of objects —
  [ar5iv, §5.3](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **How a count closes the world** (§2.4, §5.1): recall = KB count ÷ asserted cardinality; equality
  ⇒ local completeness; local completeness ⇒ "unlisted objects are implicitly negated" —
  [ar5iv, §2.4 and §5.1](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **Other licences for closure**, for contrast: functional predicates (§2.4 — one object for a
  functional predicate means the subject is complete for it) and **obligatory attributes** (§3.2 —
  if an instance of a class lacks an attribute every instance must have, the KB is *necessarily*
  incomplete on it) — [ar5iv, §2.4, §3.2](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **How systems decide when absence is informative** (§5.2, "peer-based inference"): the PCA acts
  as a **gate** — if a subject already has objects for a predicate, the list is treated as complete
  so missing candidates can be negated; candidates without such support are **discarded** rather
  than negated — [ar5iv, §5.2](https://ar5iv.labs.arxiv.org/html/2305.05403)
- Counts can also be checked *against* the data to diagnose the KB: "A perfect match indicates
  local completeness", a smaller observed set points to missing facts, and a larger one suggests
  wrong or stale statements —
  [MPI "Cardinalities from KB and Text" tutorial](https://www.mpi-inf.mpg.de/fileadmin/inf/d5/research/WWW-tutorial/Cardinalities_from_KB_and_Text.pdf)
- The ambiguity the count resolves, stated in the counting-quantifiers paper (§2, on Wikidata):
  without a count there is "uncertainty about whether the others have no children or whether
  Wikidata does not know about them" — [arXiv:1807.03656](https://arxiv.org/pdf/1807.03656)
- That same paper's §7 states that counting quantifiers "allow to infer negative information" and
  that "**Appropriate models require to combine open-world and closed-world reasoning**" —
  [arXiv:1807.03656, §7](https://arxiv.org/pdf/1807.03656)
- Empirically, counts do recover negatives: applying CINEX to all human entities to find subjects
  with **no** objects yielded 1,648 zero-child and 557 zero-spouse cases, increasing known zero
  cases by factors of **25.8×** and **3.3×** —
  [arXiv:1807.03656, §6.3](https://arxiv.org/pdf/1807.03656)
- Cardinality bounds are mined *statistically*, not taken as gospel: bounds are learned at a "given
  confidence level" with a minimum likelihood threshold using **Hoeffding's inequality** for
  robustness — [ar5iv, §4.1](https://ar5iv.labs.arxiv.org/html/2305.05403); Giacometti et al. on
  discovering maximum role cardinalities per concept —
  [ROIA](https://roia.centre-mersenne.org/articles/10.5802/roia.30);
  [Mining Cardinalities from Knowledge Bases](https://research.universityofgalway.ie/en/publications/mining-cardinalities-from-knowledge-bases/)

### Inferences

- The formal hinge, stated precisely for the blog: let `S = {g1..g5}` be the top-5 GPU producers
  (the **known total**), `R ⊆ S` the true supply relation, and the press release assert
  `|R| = 4` (the **known count**). OWA makes each `gi ∉ R` unprovable from silence. The cardinality
  assertion adds the constraint `|R| = 4` over a **domain closed by construction** (the author holds
  records from *all five*). Confirming any 4 members forces the fifth out by counting alone. This is
  not a heuristic closure like the PCA — it is a sound inference from a stated total, and that
  distinction is the strongest honest argument in the whole investigation.
- The inference is **sound only if three side conditions hold**, and each is a real failure mode the
  author should name: (i) the domain is genuinely closed — "world's 5 top GPU producers" must resolve
  to exactly five identifiable entities, and the ranking basis (units, revenue, discrete vs
  integrated) is the soft spot; (ii) the claim is truthful and current as of the dated press
  release; (iii) "supplies memory to" means one relation, consistently, on both sides. A spec change
  on any of the three breaks the elimination, not just degrades it.
- The elimination is **brittle in a specific direction worth stating**: confirming 4 positives
  settles it, but confirming only 3 leaves a 2-way ambiguity, and the count gives no help choosing.
  So the count converts a 5-way open-world problem into a *small* closed-world one, which is a real
  reduction but not a guarantee of an answer.
- The survey's §5.2 "gate" behaviour is the right design pattern to copy: **negate only inside the
  region the count closes, and discard (not negate) everywhere else.** That is the difference
  between a defensible system and one that silently asserts falsehoods.

### Gaps

- I found **no source that treats a third-party aggregate claim (A's claim about S) as a closure
  licence for the entities in S**. The survey's cardinality assertions are *about the subject whose
  objects are being enumerated*. Here the count is asserted by the supplier about its customers,
  i.e. on the opposite side of the relation from where the per-entity evidence sits. This
  **direction mismatch is, as far as I can find, unaddressed in the literature** and is a genuinely
  novel wrinkle the author can claim.
- No source quantifies how often stated corporate counts of this kind are accurate. The Hoeffding /
  confidence-level machinery exists for *mined* bounds, not for *asserted* marketing claims.

---

## 3. Can a vector or passage retriever express absence?

### Takeaway

No, and this is measured, repeatedly, with bad numbers. On the strictest test (NevIR) bi-encoders,
sparse and late-interaction retrievers score **worse than chance**; the best cross-encoder reaches
**50.6%** against a chance baseline of 25% and human ~100%. On the easier exclusionary-query test
(ExcluIR) off-the-shelf BM25 and DPR sit at **53.5%** and **54.0%** against a 50% random floor and
100% human. This is the honest baseline the graph has to beat, and it is a low bar.

### Cited Findings

**NevIR (strict negation; retrieval metric = pairwise accuracy)**

- Weller, Lawrie and Van Durme, **"NevIR: Negation in Neural Information Retrieval"**, arXiv:2305.07614
  (v1 May 2023; **v2 dated 26 Feb 2024**), published at **EACL 2024** —
  [arXiv HTML](https://arxiv.org/html/2305.07614v2);
  [EACL 2024 anthology](https://preview.aclanthology.org/setup/2024.eacl-long.139)
- Design: each item is a **pair of documents differing only by a negation**; the model must rank the
  correct one for each of two queries. Dataset: **2,556 contrastive pairs** (948 train / 225 dev /
  1,383 test) — Table 1, §1 — [arXiv HTML](https://arxiv.org/html/2305.07614v2)
- **Chance baseline: 25%** (§4.2) — because both queries must be ranked correctly —
  [arXiv HTML, §4.2](https://arxiv.org/html/2305.07614v2)
- Measured pairwise accuracy (Table 2, §5.1):
  | Class | Model | Pairwise acc. |
  |---|---|---|
  | Sparse | TF-IDF | **2.0%** |
  | Sparse | SPLADEv2 ensemble-distill | **8.0%** |
  | Sparse | SPLADEv2 self-distill | **8.7%** |
  | Bi-encoder | DPR | **6.8%** |
  | Bi-encoder | multi-qa-mpnet-base-dot-v1 (best of 11) | **11.1%** |
  | Late interaction | ColBERTv2 | **13.0%** |
  | Late interaction | ColBERTv1 | **19.7%** |
  | Cross-encoder | RocketQA v2 | **22.4%** |
  | Cross-encoder | MonoT5 base / large / 3B | **34.9% / 45.8% / 50.6%** |
  — [arXiv HTML, Table 2](https://arxiv.org/html/2305.07614v2)
- Headline reading: **every** bi-encoder, sparse and late-interaction model scores **below the 25%
  chance baseline**; no bi-encoder exceeds 12% and most are in the 5–10% band (§5.1) —
  [arXiv HTML, §5.1](https://arxiv.org/html/2305.07614v2)
- **Humans: 3 annotators ranked 10 sampled test instances and all ranked every query correctly**
  (§3.4) — i.e. ~100%, though the paper gives no aggregate percentage —
  [arXiv HTML, §3.4](https://arxiv.org/html/2305.07614v2)
- Fine-tuning (§6, Figure 5, Appendix E): 20 epochs on NevIR's train split "improves performance
  significantly" but leaves a large gap to 100%; exact post-fine-tuning values appear only in
  Figure 5. **Fine-tuning reduced MSMarco performance** — i.e. there is a measured trade-off
  against general retrieval quality, with ColBERT degrading least —
  [arXiv HTML, §6](https://arxiv.org/html/2305.07614v2)

**ExcluIR (exclusionary queries; retrieval metric = RR, ΔR@1, ΔMRR)**

- Zhang et al., **"ExcluIR: Exclusionary Neural Information Retrieval"**, arXiv:2404.17288,
  submitted **26 Apr 2024**, published at **AAAI 2025** —
  [arXiv HTML](https://arxiv.org/html/2404.17288v1);
  [AAAI 2025 proceedings](https://ojs.aaai.org/index.php/AAAI/article/view/33451)
- Size: **3,452 human-annotated exclusionary queries** as the benchmark, plus a **70,293-query**
  training set (each with a positive and a negative document) —
  [arXiv HTML](https://arxiv.org/html/2404.17288v1)
- Metric: **RR** = proportion of queries where the positive document outranks the negative;
  **random = 50%** — [arXiv HTML](https://arxiv.org/html/2404.17288v1)
- Measured RR (Table 1 = HotpotQA-trained; Table 2 = NQ320k-trained; Appendix C Table 6 = rerankers
  over top-100 BM25):
  | Class | Model | RR | ΔR@1 | ΔMRR |
  |---|---|---|---|---|
  | Sparse | BM25 | **53.48** | 7.82 | 4.66 |
  | Sparse | DocT5Query | **53.85** | 7.85 | 3.81 |
  | Dense | DPR (HotpotQA) | **54.02** | 7.34 | 5.01 |
  | Dense | Sentence-T5 (HotpotQA) | **55.41** | 10.11 | 7.01 |
  | Dense | GTR (HotpotQA) | **57.42** | 14.40 | 8.79 |
  | Late int. | ColBERT (HotpotQA) | **55.57** | 10.72 | 6.42 |
  | Dense | GTR (NQ320k) | **68.31** | 34.85 | 28.12 |
  | Dense | Sentence-T5 (NQ320k) | **67.83** | 32.90 | 27.96 |
  | Generative | GENRE (HotpotQA) | **52.10** | 4.35 | 0.13 |
  | Generative | NCI (HotpotQA) | **50.93** | 1.97 | 2.29 |
  | Generative | SEAL (NQ320k) | **60.02** | 16.11 | 15.27 |
  | Cross-enc. | monot5-large-msmarco-10k | **70.91** | 42.80 | 23.71 |
  | Cross-enc. | monot5-3b-msmarco-10k | **70.74** | 42.17 | 23.35 |
  | Cross-enc. | RocketQA-v1_nq_ce | **70.48** | 41.56 | 22.98 |
  — [arXiv HTML, Tables 1, 2, 6](https://arxiv.org/html/2404.17288v1)
- **Human RR = 100%** (Limitations section) —
  [arXiv HTML](https://arxiv.org/html/2404.17288v1)
- The authors report that models across architectures struggle, fine-tuning helps but falls short of
  humans, and generative retrieval handles exclusion better than the others *in their setup* —
  [arXiv HTML](https://arxiv.org/html/2404.17288v1). **Caution:** the per-model table above shows
  generative models (GENRE 52.10, NCI 50.93) at or barely above the 50% floor on HotpotQA, i.e. the
  "generative is better" claim is not visible in those rows. Flag this as an internal tension in the
  paper rather than repeating the abstract's summary.

**Aggregation / counting in RAG**

- A practitioner source names the failure class directly, listing poor fits for RAG as "aggregation
  queries ('how many', 'what's the total')", tasks requiring code or API execution, and "questions
  whose answer is implicit across thousands of documents", recommending structured query or tool use
  instead — [ZeroEntropy, RAG concepts](https://zeroentropy.dev/concepts/rag/). **This is a vendor
  explainer, not a measurement** — use it as a statement of the design problem, not as evidence.
- Barnett et al., "Seven Failure Points When Engineering a Retrieval Augmented Generation System",
  arXiv:2401.05856, **CAIN 2024**, whose headline finding is that "validation of a RAG system is
  only feasible during operation" — [arXiv](https://arxiv.org/pdf/2401.05856);
  [CAIN 2024](https://conf.researchr.org/details/cain-2024/cain-2024-call-for-papers/5/Seven-Failure-Points-When-Engineering-a-Retrieval-Augmented-Generation-System)

### Inferences

- The two benchmarks measure different difficulties and the author should present both, not one.
  NevIR is the hard case (the negation is the *only* difference between two documents, so the
  embedding must encode it); ExcluIR is the easy case (the exclusion is in the query and lexical
  overlap still helps). The author's task is closer to NevIR in kind — "which producer is *not*
  supplied" — and NevIR's numbers are the ones below chance.
- The structural reason is worth stating once and is consistent with both result sets: a dense
  vector is a **similarity** object. "Supplies X" and "does not supply X" have near-identical
  lexical and topical content, so they land near each other in embedding space, and a top-k
  similarity search over them cannot represent "nothing matched, and that absence is the answer".
  Absence is not a point in the space.
- Cross-encoders doing least badly on both benchmarks (50.6% NevIR, ~71% ExcluIR) is consistent
  with absence being a **joint query-document reasoning** property rather than a representable
  document property — which is also why a reranker in a pipeline cannot fix it: by the time the
  reranker runs, the document that *would* have witnessed the absence was never retrieved.
- Honest baseline statement for the blog: **no retriever in either benchmark reaches the human
  level, and on the strict test the architectures the author actually uses (bi-encoder dense
  retrieval) score below random.** The graph does not have to be good to beat this; it only has to
  be correct.

### Gaps

- I found **no benchmark that measures the specific composite query** "given an aggregate count over
  a known set, identify the excluded member". The nearest measured things are negation ranking
  (NevIR), exclusionary ranking (ExcluIR), and a vendor claim about aggregation queries. **This is
  an unmeasured capability** — the author can say so.
- NevIR's exact post-fine-tuning accuracies are in Figure 5 only and were not extractable from the
  HTML — [arXiv HTML, §6/Fig. 5](https://arxiv.org/html/2305.07614v2).
- A search for a paper measuring RAG end-to-end on negation + counting + aggregation returned no
  such paper; the hits were failure-point taxonomies and blog posts. Searched:
  "retrieval augmented generation negation counting aggregation questions failure".

---

## 4. What is best suited — decisive

### Takeaway

**Use Answer Set Programming (clingo) over a small, explicitly-typed fact store, with weak
constraints for graded evidence. Do not use a property graph as the reasoner, do not use graph
embeddings, and do not use GraphRAG community summarisation at all.** The decisive argument is not
that ASP can *express* the constraint — Cypher and SQL can both count and both have `NOT EXISTS` —
it is that the task is **model enumeration over a constrained hypothesis space**, and that is the
one thing ASP solvers do natively and the query languages do not.

### The comparison

| Technique | Cardinality constraint `\|R\|=4`? | Absence? | Rank by evidence strength? | Index-time cost |
|---|---|---|---|---|
| **Property graph + Cypher (Neo4j)** | Can `count()` declared edges and compare | `NOT EXISTS` over *declared* edges only — i.e. closed-world over the stored graph, with no way to distinguish "absent" from "unknown" | Only by sorting on a stored weight property; no principled combination | Entity resolution + typed edges + a provenance/confidence property per edge |
| **Relational DB + SQL** | Same: `HAVING COUNT(*) = 4`, `NOT EXISTS` | Same limitation | Same | Same, minus traversal |
| **Datalog** | Not in pure Datalog; needs stratified negation + aggregation extensions | Negation-as-failure, i.e. closed-world by construction | No native weighting | Facts + rules |
| **ASP (clingo)** | **Natively** — `#count{...} = 4`, `N { ... } M`, integrity constraints | **Yes**, and crucially can hold the *hypothesis* open: a choice rule generates candidate `R`s, constraints prune them, and the solver **enumerates every surviving model** | **Yes**, via weak constraints / `#minimize` over evidence weights, giving an optimal model and a ranked list | Facts + a ~20-line program; no training |
| **PSL / MLN** | Hard constraints expressible; `|R|=4` as an arithmetic/functional constraint | Yes, with soft truth values rather than three-valued logic | **Yes, and best-in-class** — this is what weighted logic is for | Facts + rule weights (learned or hand-set) |
| **Graph-embedding link prediction** | **No** — scores edges independently; nothing enforces a global count | No — unobserved is conflated with false by the training objective | Yes, by score, but scores are not calibrated probabilities | Training a model on a graph that here has **5 candidate edges** |
| **LLM set reasoning over retrieved evidence** | Only as prose instruction, unverifiable | Only if the absence is *retrievable*, which §3 shows it is not | Informally, unauditably | None, which is its one advantage |
| **GraphRAG community summarisation** | **No** | **No** | No | Full LLM pass over the corpus for entity/claim extraction + community summaries |

### Cited Findings

- **ASP's cardinality constraints are a first-class, long-established language feature**, and
  explicitly credited to the smodels solver: "cardinality and weight constraints that express
  counting and resource limits" sit alongside integrity constraints and choice rules; smodels is
  "an early solver that established cardinality constraints" in the field —
  [IEEE TechNav, Answer Set Programming](https://technav.ieee.org/topic/answer-set-programming/)
- Counting aggregate syntax: written as `#count{...}` with a comparison; an aggregate element is
  "a pair X : L, where X is a tuple of distinct variables, and L is a conjunction of literals and
  comparisons"; the conjunction of aggregate atoms can be written `count{E} = t` —
  [Recursive Aggregates as Intensional Functions in ASP, arXiv:2412.10975](https://ar5iv.labs.arxiv.org/html/2412.10975)
- A **gotcha the author must know** if they write the program: binding a variable on the
  right-hand side changes behaviour — `q :- #count{X : p(X)} = Y, Y >= 1` is **not strongly
  equivalent** to `q :- #count{X : p(X)} >= 1` —
  [Strong Equivalence, arXiv:2205.07783](https://ar5iv.arxiv.org/html/2205.07783)
- Counting in logic programs has current formal foundations: "Deductive Systems for Logic Programs
  with Counting", *Theory and Practice of Logic Programming* (Cambridge, S1471068425100379) —
  [Cambridge PDF](https://www.cambridge.org/core/services/aop-cambridge-core/content/view/31171D4407BEC912767ADDD34311D238/S1471068425100379a.pdf/deductive_systems_for_logic_programs_with_counting.pdf)
- **Negation as failure is default negation**: "if b(o1,o2) is true and it is **not known** that
  c(o2) is true, then a(o1,o2) must be true"; stable-model semantics makes each answer set
  self-supporting, "ruling out conclusions that only justify themselves in a circular way" —
  [ASP primer](https://mag.di.unimi.it/aiclass/aa0405/asp-primer.pdf);
  [ASP course notes, CTU 2021](https://cw.fel.cvut.cz/b211/_media/courses/lup/answer_set_programming_2021.pdf)
- Answer sets relate to Clark's completion: "if X is an answer set for Π then X, viewed as a truth
  assignment, satisfies the completion of Π" — the converse does not always hold (Babovich, Erdem,
  Lifschitz) — [arXiv:cs/0003042](https://arxiv.org/pdf/cs/0003042)
- Negation-as-failure interacts subtly with generalised cardinality-style constraint atoms
  ("c-atoms"); Son et al., **AAAI 2006**, "discuss different approaches for treating
  negation-as-failure c-atoms and characterize situations in which they are equivalent" —
  [AAAI 2006 PDF](https://cdn.aaai.org/AAAI/2006/AAAI06-021.pdf)
- **PSL / HL-MRF** is the right tool if the author wants fully soft reasoning: Bach, Broecheler,
  Huang and Getoor, "Hinge-Loss Markov Random Fields and Probabilistic Soft Logic", **JMLR 18
  (2017)**, introduces HL-MRFs as "a new kind of probabilistic graphical model that generalizes
  different approaches to convex inference", with MAP inference by message passing to "take
  advantage of sparse dependency structures". PSL is listed as applicable to "collective
  classification, entity resolution, **link prediction**, and ontology alignment" —
  [JMLR](https://www.jmlr.org/papers/v18/15-631.html);
  [arXiv:1505.04406](https://ar5iv.arxiv.org/html/1505.04406)
- The survey independently converges on the same architecture for cardinality + prediction: rule
  predictions that violate cardinality constraints are **downgraded rather than removed** (soft
  constraints), and cardinality information is used as a **prior for link prediction** (§4.3) —
  [ar5iv, §4.3](https://ar5iv.labs.arxiv.org/html/2305.05403)
- Embeddings' structural problem, stated by the uncertain-KG literature: prior models "treat
  unobserved facts as false" — [UKGE, AAAI 2019](https://ojs.aaai.org/index.php/AAAI/article/view/4210);
  [author PDF](https://web.cs.ucla.edu/~yzsun/papers/2019_AAAI_UKG.pdf)
- LLMs are measurably bad at the aggregation step specifically: the survey notes that when LLMs must
  aggregate counts themselves they "**fall back to common numbers in the relation of interest**"
  (§4.2) — [ar5iv, §4.2](https://ar5iv.labs.arxiv.org/html/2305.05403)

### Inferences — the decision, stated plainly

**Choose ASP (clingo).** Four reasons, in order of force:

1. **The task is model enumeration, not query evaluation.** Cypher and SQL answer "which edges
   satisfy P over the graph I have". The question here is "which *assignments* of a 4-of-5 relation
   are consistent with my evidence". That is a search over 5 subsets under a hard constraint, and a
   solver returns **all** surviving models — which is exactly the output the author needs, because
   it distinguishes "one answer, determined" from "two answers, undetermined" from "zero answers,
   your evidence contradicts the press release". A Cypher query cannot report the third case, and it
   is the most valuable case: it means somebody is wrong, which is a publishable finding.
2. **Three-valued honesty is in the semantics, not the schema.** In a property graph, "no edge" is
   one thing, and the author must bolt on a convention (an explicit `:NoRelation` edge, or a
   `status` property) to keep "unknown" apart from "false". In ASP, `not supplies(G,A)` is
   negation-as-failure and `-supplies(G,A)` is classical negation, and the two are distinct in the
   language. Section 2's entire problem is a semantics problem, so it should be solved in the
   semantics.
3. **The constraint is one line and auditable.** `:- #count{ G : supplies(a,G), top5(G) } != 4.`
   plus a choice rule over `top5`. The author can show that line in the blog; nobody can show a
   trained embedding.
4. **No index-time training, and the graph is tiny.** With five candidate edges, graph embeddings
   are not merely unsuited, they are **inapplicable**: there is nothing to learn from. Say that
   plainly — it is the cleanest "no" in the whole comparison.

**Does ASP beat a property graph here? Yes, and the margin is the interesting part.** A property
graph is still the right place to *keep* the evidence — typed `(:Company)-[:SUPPLIES {source,
evidence_grade, date}]->(:Company)` edges, with provenance per edge, is a good store and a good
thing to draw in the blog. But Neo4j is the wrong reasoner because Cypher evaluates against the
stored graph under de-facto closed-world semantics, and the author's whole problem is that the
stored graph is incomplete *and he knows exactly how incomplete*. **Store in the graph (or frankly
a single relational table — with 5 nodes the traversal advantage is zero), reason in clingo.** The
author should resist the pull to call this "GraphRAG"; it is a solver over a tiny fact table, and
the honest headline is that the *fourth* answer is still "not GraphRAG" even though it finally
*is* "a graph-shaped problem".

**Datalog: second place, and only just.** Pure Datalog has neither negation nor aggregation;
stratified-negation and aggregate extensions get you there, but you lose the two things that make
ASP decisive — enumeration of multiple models (Datalog computes one minimal model) and weak
constraints for ranking. If the author's stack forces Datalog, it will work; ASP is strictly better.

**PSL/MLN: the right escalation, not the right start.** If the evidence turns out to be so graded
that no crisp answer survives, PSL is the principled home for weighted evidence + hard constraints,
and the survey's "downgrade, don't remove" pattern is the same idea. But it needs rule weights,
which means either hand-tuning (unprincipled) or weight learning (needs labelled data the author
does not have). Start with ASP weak constraints — `#minimize` over integer evidence grades gets 90%
of the benefit with no weight learning.

**GraphRAG community summarisation: wrong by design, and the author has already proven it three
times.** Community summaries are a *lossy compression* of a corpus into prose, built for global
sensemaking over a corpus too large to read. This task has five entities and a known total. Summar-
isation destroys exactly the information the count needs — the per-entity enumeration. Using it
here would be a fourth "no", and the author should say so in one sentence rather than testing it.

**LLM set reasoning: use it, but only as an extractor.** The right division of labour is: LLM reads
each producer's records and emits a *typed, graded, provenance-carrying* candidate edge or an
explicit "no mention found"; the solver does the counting. Never let the LLM do the arithmetic — the
survey's §4.2 finding that LLMs fall back to common numbers when aggregating is the direct citation
for why.

### Gaps

- **The official Potassco/clingo user guide is cited above only indirectly.** `https://potassco.org/clingo/guide/`
  returned **HTTP 404**, and a targeted search did not surface the official page. The syntax claims
  above rest on peer-reviewed ASP papers and the IEEE overview, not on vendor documentation. The
  author should pin the guide (try `https://github.com/potassco/guide/releases`) before publishing
  exact syntax.
- I did not re-verify the GraphRAG paper (Edge et al., "From Local to Global", arXiv:2404.16130)
  in this session, so no figure from it is quoted; the characterisation above is from its design
  purpose as the author's prior rounds established it.
- No source compares ASP against a property graph **on this task**. The §4 verdict is my reasoned
  judgement from each tool's documented capabilities, not a measured head-to-head. Label it as such.

---

## 5. Evidence strength, not just presence

### Takeaway

Two usable published patterns exist, and they compose: (i) **confidence-weighted KGs** attach a
score to each fact and propagate it (UKGE), and (ii) **soft constraints** keep a hard cardinality
constraint as a *penalty* rather than a filter, so graded evidence and a hard count coexist. The
survey states the latter explicitly. The practical recipe is: grade the evidence as integers, let
the solver optimise, and report the margin between the best and second-best model.

### Cited Findings

- **Confidence-weighted KGs:** UKGE (Chen et al., **AAAI 2019**, 33(01):3363–3370) "learns
  embeddings according to the confidence scores of uncertain relation facts" rather than treating
  facts as binary, and uses **probabilistic soft logic** "to infer confidence scores for unseen
  relation facts during training". Evaluated on three real-world uncertain KGs across **confidence
  prediction, relation fact ranking, and relation fact classification** —
  [AAAI](https://ojs.aaai.org/index.php/AAAI/article/view/4210);
  [arXiv:1811.10667](https://ar5iv.arxiv.org/html/1811.10667);
  [author PDF](https://web.cs.ucla.edu/~yzsun/papers/2019_AAAI_UKG.pdf)
- Uncertain KBs that natively carry per-fact confidence, named in UKGE's introduction: **ProBase,
  ConceptNet, NELL** — [arXiv:1811.10667](https://ar5iv.arxiv.org/html/1811.10667)
- **Hard constraint as soft penalty** (the key pattern): rule predictions that violate cardinality
  constraints are **downgraded rather than removed** (§4.3) —
  [ar5iv, §4.3](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **Completeness is binary but recall is graded**: "KB recall is a real-valued concept", whereas
  completeness is binary (§2.2) — [ar5iv, §2.2](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **Ranking negation candidates:** candidates are ranked by **relative peer frequency** or by
  **negative likelihood** (§5.2–5.3) —
  [ar5iv, §5.2–5.3](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **Combining weak signals:** done via rule mining, reporting **F1 of 90–100% for most of the 10
  predicates tested**, with `hasSpouse` the exception (§3.2) —
  [ar5iv, §3.2](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **Per-extraction confidence:** the sequence-labelling count extractor outputs labels "with a
  confidence score", and ties are broken by those scores (§4.2) —
  [ar5iv, §4.2](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **Statistical, not absolute, bounds:** cardinality bounds mined at a "given confidence level"
  with a minimum likelihood threshold, using **Hoeffding's inequality** (§4.1) —
  [ar5iv, §4.1](https://ar5iv.labs.arxiv.org/html/2305.05403);
  [Giacometti et al., ROIA](https://roia.centre-mersenne.org/articles/10.5802/roia.30)
- **Weighted logic as the general home for this:** HL-MRF/PSL inference is convex and scalable via
  message passing, and PSL's stated applications include link prediction — Bach et al., **JMLR 18
  (2017)** — [JMLR](https://www.jmlr.org/papers/v18/15-631.html)
- **Popularity as a signal:** "high precision but low recall" (§3.2) — relevant because "A is
  famous, so trade press mentions it" is exactly a popularity artefact —
  [ar5iv, §3.2](https://ar5iv.labs.arxiv.org/html/2305.05403)

### Inferences

- The author's evidence hierarchy (joint press release > trade publication > job advert) maps
  cleanly onto an **integer cost scale in an ASP weak constraint**. Concretely:
  `#maximize { 100@1, G : supplies(a,G), joint_release(a,G) }` and lower weights for weaker grades,
  under the hard `#count = 4` integrity constraint. This is the §4.3 "downgrade, don't remove"
  pattern implemented in the one tool that has it natively, and it means the hard count determines
  *how many* edges, while the grades determine *which*.
- The combination is more interesting than either part: the count makes the problem **competitive**.
  Four slots and five candidates means evidence is no longer absolute — a weak-but-nonzero signal
  for the fifth producer only matters if it beats one of the other four. That is an argmax, not a
  threshold, and it is why ranking machinery is load-bearing rather than decorative.
- **The job-advert tier deserves a warning.** A job advert naming A is evidence about *hiring*, not
  about a supply relation; it is a correlate. The survey's finding that popularity has "high
  precision but low recall" is the closest citable caution: a famous supplier will appear in weak
  sources for producers it does not supply. The author should grade by **joint attestation**, not
  by mention count, or the weakest tier will dominate by volume.
- **Report the margin, not just the winner.** The honest output of a weak-constraint optimisation is
  the optimal model *and the cost gap to the next-best*. A gap of 1 means the answer flipped on a
  single job advert. A property-graph `ORDER BY` cannot give this; a solver can.

### Gaps

- I found **no published system that combines a graded-evidence ranking with a hard, externally
  asserted cardinality constraint on a specific real-world discovery task**. The two ingredients are
  each well documented (UKGE for weights; survey §4.3 for soft cardinality) but I found no paper
  that puts them together and measures the result.
- The search for how PSL *syntactically* expresses functional/cardinality constraints for link
  prediction returned only general PSL background. Searched: "probabilistic soft logic hinge-loss
  Markov random field link prediction constraints"; the PSL user guide was not surfaced. Record as
  a gap — [JMLR paper](https://www.jmlr.org/papers/v18/15-631.html).

---

## 6. The supply-chain instance

### Takeaway

Supplier-relation extraction from corporate text is an active area with several 2024–2026 systems,
but **every one I found extracts relations that at least one party has disclosed in text.** I found
**no work on inferring a supplier link that neither party discloses, and no work using a disclosed
aggregate ("we supply most of the top N") as a constraint.** This appears genuinely unexplored, and
the author can state that plainly.

### Cited Findings

**Taken as given from prior project research (not re-verified here):** one 2026 paper reporting
**88% audit confirmation over 149 directed edges** from annual reports, with **no recall reported**;
and commercial supply-chain relationship datasets being terminal-licensed with no open equivalent.

- **RAG over filings, no accuracy reported:** Jackson, Saenz, Ma and Ivanov, "Supply chain mapping
  through retrieval-augmented generation: applications to the electronics industry", **MIT Center
  for Transportation and Logistics, published 1 January 2026**. Extracts supplier–customer
  relationships from SEC 10-K filings and earnings-call transcripts, builds a directed graph, and
  analyses it with centrality, modularity and path length. Case study: Foxconn (Hon Hai), Flex,
  Jabil. **The only quantitative result on the landing page is a graph of 4,644 nodes and 8,341
  edges — no precision, no recall, no ground-truth validation**, and no mention of inferring
  undisclosed links or of using aggregate disclosures as constraints —
  [MIT CTL](https://ctl.mit.edu/publications/supply-chain-mapping-through-retrieval-augmented-generation-applications-electronics);
  [DSpace@MIT](https://dspace.mit.edu/handle/1721.1/165214)
- **SC-RED benchmark:** a silver-standard dataset of **1,671 supply-chain relation triples across
  2,328 entities and 1,150 source documents**, drawn from Wikipedia and grounded in Wikidata; the
  authors report that extraction errors "stem mainly from unconstrained generation" —
  [CEUR-WS Vol-4293, paper 55](https://ceur-ws.org/Vol-4293/paper55.pdf)
- **InterCorpRel-LLM:** combines GNNs with LLMs, trained on **FactSet supply-chain records**,
  reporting **F-score 0.8543** on supply-relation identification (authors' own evaluation,
  **arXiv:2510.09735**, October 2025) — [arXiv HTML](https://arxiv.org/html/2510.09735v1)
- **Snippet-based graph construction for non-disclosing firms** — the closest thing to the author's
  problem I found. A **May 2026** arXiv preprint targets firms that structured data misses, building
  a supply-chain KG from web-search snippets because "supplier–customer disclosure is typically
  limited to major partners of listed firms in China" —
  [emergentmind record](https://www.emergentmind.com/papers/2605.27845);
  [ChatPaper record](https://chatpaper.com/paper/287381).
  **Caution: I could not reach the arXiv page itself; both URLs are secondary indexes. Treat the
  figures as unverified and do not quote numbers from it.**
- **UK ONS pilot:** models "can detect buyer-supplier relationships between enterprises in sentences
  from news articles with good accuracy", with performance best on topics seen in training; no
  figure given on the page —
  [ONS Data Science Campus](https://datasciencecampus.ons.gov.uk/using-natural-language-processing-for-the-analysis-of-global-supply-chains)
- **Commercial / patent pipelines** describe sentence-level classifiers that find two company names,
  check for relation-indicating n-grams, and label each firm supplier / customer / neither, over
  inputs including regulatory filings and customs data —
  [US 11,222,052](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/11222052);
  [US 11,392,875](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/11392875)
- **The generic counting-quantifier result that is the closest analogue**, from a different domain:
  CINEX extracted 851K counting-quantifier facts that "state the existence of **2.5M facts not yet
  asserted** for these 110" relation pairs, "increas[ing] the number of facts known to exist for
  these relations by **28.3%**" (§6.4), at **60% average precision** on five human-evaluated
  relations (abstract, §1) and **55–85% precision** for cardinals (§6.2, CINEX-CRF). A separate
  40-relation experiment reports a **77.3%** increase (Table 6, §6.3) — **note these are different
  experiments; do not conflate 28% and 77.3%** —
  [arXiv:1807.03656](https://arxiv.org/pdf/1807.03656)

### Inferences

- The literature's uniform shape is **extraction, not inference**: find a sentence where party X
  names party Y. The author's task has no such sentence by construction. So the existing numbers
  (0.8543 F1, 88% audit confirmation) are **not transferable baselines** — they measure a different
  task, on evidence that exists.
- The absence of recall reporting across this literature is not incidental; it is structural. **You
  cannot measure recall of supplier relations without a complete ground-truth supply graph, and the
  only complete-ish graphs (FactSet) are terminal-licensed.** That is why InterCorpRel-LLM, the one
  paper with a strong F-score, is the one trained on FactSet. This is worth saying: the field's
  metric gap and its licensing gap are the same gap.
- Here is the strongest honest statement of novelty available: the author's setup supplies *from the
  aggregate claim* the very thing this literature lacks *from data* — **a known denominator**. "4 of
  5" is a recall oracle for one tiny slice of the graph. That is the real contribution, and it is a
  methodological one, not a graph-technology one.
- CINEX is the right precedent to cite even though it is about children and spouses, not suppliers:
  it is the only published demonstration that counting quantifiers extracted from text recover
  facts (and **zero**-cases, at 25.8× and 3.3×) that enumeration alone missed. The author's task is
  CINEX's mechanism applied to one hand-picked corporate relation.

### Gaps

- **No work found on inferring a supplier link that neither party discloses.** Searched: "supply
  chain knowledge graph relation extraction supplier customer corporate filings". Every system found
  requires a textual mention by at least one party. **State this plainly — it is unexplored.**
- **No work found using a disclosed aggregate ("we supply N of the top M") as a constraint on
  supplier-link inference.** The aggregate-as-constraint idea exists in the KB-completeness
  literature (§2 above) and in supply chain **not at all** that I could find.
- The May 2026 Chinese supply-chain KG preprint could not be retrieved from a primary source; only
  secondary indexes resolved. Do not cite figures from it —
  [emergentmind](https://www.emergentmind.com/papers/2605.27845).
- The MIT CTL paper's full text was not retrieved (landing page only); precision/recall may exist in
  the PDF — [MIT CTL](https://ctl.mit.edu/publications/supply-chain-mapping-through-retrieval-augmented-generation-applications-electronics).

---

## 7. How would anyone know if it worked?

### Takeaway

The answer is unverifiable by construction, so the honest evaluation is entirely **indirect**:
hide known edges and measure recovery (the standard KG-completion protocol), report filtered
Hits@k / MRR on that held-out set, and for the actual target report the *solver's output state* —
unique model, ambiguous, or unsatisfiable — plus the cost margin, with no accuracy claim attached.
The published critiques of link-prediction evaluation all bear on small candidate sets, which is
exactly the author's regime, so the protocol must be designed defensively.

### Cited Findings

- **The filtered protocol is the standard.** Its purpose is to avoid penalising a model for ranking
  *other true* facts highly: "in the raw scenario, valid entities outscoring the target one are
  considered as mistakes, while in the filtered scenario, valid entities outscoring the target one
  are not considered mistakes." The filtered setting is "the quasi standard" and "unfiltered scores
  are rarely reported" —
  [KGE for Link Prediction: A Comparative Analysis, arXiv:2002.00819](https://arxiv.org/pdf/2002.00819);
  [Learning Representations of Entities and Relations, arXiv:2201.13073](https://arxiv.org/pdf/2201.13073)
- **Hits@k definition:** "the ratio of test queries where the rank i of the correct candidate is
  less than or equal to k"; MR, MRR and Hits@k with **k ∈ {1, 3, 10}** are the standard reported set
  — [arXiv:2002.00819](https://arxiv.org/pdf/2002.00819);
  [PyKEEN, understanding evaluation](https://github.com/pykeen/pykeen/blob/v1.7.0/docs/source/tutorial/understanding_evaluation.rst)
- **Critique 1 — small candidate sets inflate scores**, which is directly the author's situation
  (5 candidates): "a small, randomly chosen candidate set makes it easy to achieve high hits@10 and
  MRR scores" —
  [Reevaluation of Inductive Link Prediction, arXiv:2409.20130](https://arxiv.org/pdf/2409.20130)
- **Critique 2 — tie policy distorts results:** the "min" tie policy "may result in artificially
  boosting performances" — [arXiv:2002.00819](https://arxiv.org/pdf/2002.00819)
- **Critique 3 — filtering assumptions can be plain wrong** for some graph types; static filtering
  "is not appropriate for evaluating the link prediction on temporal KGs" —
  [xERTE, arXiv:2012.15537](https://arxiv.org/pdf/2012.15537);
  [Temporal KG Forecasting with Neural ODE, arXiv:2101.05151](https://arxiv.org/pdf/2101.05151)
- **Semantic-awareness as a complementary metric:** Sem@K measures whether predicted entities are at
  least type-correct, as a check that rank metrics are not rewarding nonsense —
  [Sem@K, arXiv:2301.05601](https://arxiv.org/pdf/2301.05601)
- **The completeness-literature analogue of "held-out truth" is itself fragile:** the survey warns
  that entity-count estimators have a **small ground truth, which makes results hard to judge**
  (§3.3), and that one must restrict to popular entities and exclude outliers above the 90th
  percentile — "**One must be selective**" (§4.2–4.3) —
  [ar5iv, §3.3, §4.2–4.3](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **Completeness is only meaningful in a bounded domain:** the survey restricts completeness claims
  to "well-defined domains" where the ideal KB can reasonably be established (§2.2, §3.1) —
  [ar5iv, §2.2, §3.1](https://ar5iv.labs.arxiv.org/html/2305.05403)
- **Prior-art caution from the supply-chain side:** the 88%-audit-confirmation figure the author
  already holds comes **with no recall reported** (taken as given), and the MIT CTL RAG paper
  reports **no accuracy metrics at all**, only 4,644 nodes / 8,341 edges —
  [MIT CTL](https://ctl.mit.edu/publications/supply-chain-mapping-through-retrieval-augmented-generation-applications-electronics)
- **General RAG caution:** "validation of a RAG system is only feasible during operation" —
  [Barnett et al., arXiv:2401.05856](https://arxiv.org/pdf/2401.05856)

### Inferences — what to claim and what not to

**Design the evaluation on three separate tracks, and never let one borrow the other's credibility:**

1. **Extraction track (answer-quality metrics).** Take relations where *both* parties disclose — the
   joint-press-release cases. Measure per-document extraction precision and recall of the typed
   edge. This is the only place the author can honestly report **recall**, because the joint
   disclosure *is* the ground truth. Report it as extraction precision/recall, not as discovery
   accuracy.
2. **Recovery track (link-prediction metrics).** The standard trick, correctly applied: take a set
   of **known** aggregate claims whose membership *is* public, hide the membership, run the full
   pipeline, and measure whether it recovers the hidden set. Report **filtered Hits@1 and MRR**, and
   — because of arXiv:2409.20130 — **report the candidate-set size alongside every number**, since
   Hits@1 over 5 candidates and over 14,000 are not comparable quantities. Also fix and state the
   tie policy up front (arXiv:2002.00819). For a 4-of-5 task, **precision@4 against the 4-member
   truth set, plus exact-set-match accuracy**, is more informative than Hits@k, because the task is
   a set, not a ranking.
3. **Target track (no metric).** For the actual Company-A question, there is no ground truth, so
   **report the solver's state, not an accuracy**: (a) *unique model* — one assignment survives, name
   the excluded producer and give the cost margin to the next-best model; (b) *ambiguous* — k models
   survive, name all k and say the evidence does not decide; (c) *unsatisfiable* — no assignment is
   consistent with `|R| = 4`, which means the press release, the records, or the top-5 definition is
   wrong, and that is itself the finding.

**Should claim:**
- Extraction precision and recall on the both-parties-disclose subset, labelled as extraction.
- Recovery rate on held-out known aggregates, with candidate-set size and tie policy stated.
- For the target: the model count, the named excluded entity if unique, the cost margin, and the
  explicit side conditions from §2 (top-5 definition, claim date, relation sense).
- That the retrieval baseline is below chance on strict negation (NevIR), with the figures.

**Should not claim:**
- Any accuracy, confidence or probability for the unverifiable target answer. There is no
  denominator. An ASP model count of 1 is a *logical* result conditional on the evidence, not an
  empirical one — the author should say "unique under these inputs", never "correct".
- That recovery-track Hits@1 transfers to the target. A 5-candidate recovery score is inflated by
  construction (arXiv:2409.20130) and the target is n=1.
- Recall on the target. The survey's bounded-domain condition (§2.2) is satisfied only for the
  5-producer slice; nothing licenses a recall claim about A's supply relations generally.
- That this validates GraphRAG. Even on the most favourable reading, what is validated is a
  **solver over a tiny typed fact store**, and §4 says the graph is storage, not the reasoner.

**One pre-registration suggestion.** Because the answer is unverifiable, the author's credibility
rests entirely on having fixed the method before seeing the outcome. Publish the evidence grades,
the weights and the clingo program **first**, then the result. That converts "trust me" into
"here is the program; run it".

### Gaps

- I found **no methodological literature on reporting an unverifiable link prediction** — i.e. no
  published convention for what to say when the target answer cannot be checked. The three-state
  solver reporting above is my construction from the tools' semantics, not a cited practice.
- No source directly criticises or endorses **precision@k** for KG link prediction; the searches
  returned Hits@k/MRR critiques only. Searched: "link prediction evaluation filtered setting hits@k
  precision@k criticism knowledge graph". The precision@4 recommendation above is reasoned from the
  set-valued nature of the task, not cited.
