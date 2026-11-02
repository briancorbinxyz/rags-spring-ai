# Multi-hop retrieval, traversal depth, latent links and authored links: the published evidence

Scope note for the report writer. This note extends
`research_notes/Episode 6 links and bitemporality/graph_expansion_evidence.md` and does not repeat it. That
note already establishes Microsoft GraphRAG's evaluation design and cost, the LazyGraphRAG 1000x framing, the
GraphRAG-Bench and RAG-vs-GraphRAG headline tables, HippoRAG 2's recall numbers and error analysis, and the
MDR-vs-Asai hyperlink story. Read it first.

**Metric discipline used throughout.** Every number below is tagged `[RETRIEVAL]` (recall@k, precision@k,
nDCG, Success@k, LinkRecall) or `[ANSWER]` (EM, F1 over answer tokens, accuracy, BERTScore against a
reference answer, LLM-judge preference, task-consistency validators). Several of the most-cited "graph wins"
in this area are `[ANSWER]`-only; three of the papers most relevant to this decision report **no retrieval
metric at all**.

**Three contradictions / corrections to the existing note, flagged up front:**

1. The existing note concludes "One hop is where the value is. Every source that measured hop depth found the
   second hop costing an order of magnitude more for a few points of recall, with one contrary case." That
   generalisation needs softening. The contrary case (GRAFT) is not an outlier: its finding is that **one hop
   is clearly insufficient and the second hop raises recall substantially**, with the third adding nothing.
   The reconciled shape is *two hops is the ceiling, and whether hop 1→2 pays depends entirely on how strong
   the first stage is* — see Q2.
2. The existing note's inference "link expansion substitutes for retrieval strength" now has direct
   quantitative support it did not have: HippoRAG's own ablation shows that adding one-hop neighbours to the
   seed set **without** a weighting mechanism is *worse than not expanding at all* on two of three multi-hop
   benchmarks (Q2, Cited Findings).
3. The existing note's gap — "no published evaluation uses a BM25+dense hybrid with a cross-encoder reranker
   as the baseline against Microsoft GraphRAG" — is wider than stated. The one study that evaluates traversal
   over **authored** documentation links (LARAG) also has no reranker; the most recent depth-ablation paper
   (HELP) has no reranked baseline; and the 2026 cost-focused paper (EffiRAG) reports no retrieval metrics at
   all. Across the whole literature surveyed here I found **zero** papers evaluating graph or link traversal
   against a tuned BM25+dense+cross-encoder baseline.

---

## Q1: The latent-link claim — is there any ablation isolating edges only an LLM could infer?

### Takeaway
**No.** I found no study anywhere that holds a corpus fixed, partitions its edges into (i) recoverable from
explicit links/structure and (ii) only inferable by an LLM, and measures the retrieval contribution of (ii)
alone. The claim that GraphRAG's value lies in connecting information whose relationship was never written
down is, as of October 2026, an **unevidenced claim** in the retrieval literature. Worse for the claim: every
ablation I could find that *does* separate structural edges from semantically-inferred edges finds the
structural edges carry the larger share, and that inferred edges are noisy enough to need gating.

### Cited Findings
- Microsoft's own paper makes no measurable latent-link claim. Its evaluated conditions are all global
  community-summary variants plus map-reduce over raw text, judged pairwise on comprehensiveness / diversity /
  empowerment / directness — [Edge et al., arXiv:2404.16130](https://arxiv.org/html/2404.16130v2) (established
  in the existing note; re-stated here because it is the baseline against which "no isolation exists" is
  asserted).
- I ran four distinct searches for an explicit-vs-inferred edge ablation in GraphRAG
  (`GraphRAG implicit relations ablation explicit vs inferred edges`, `GraphRAG connections not explicitly
  stated implicit connections Microsoft claim evidence`, `ablation adding semantic inferred edges on top of
  explicit structural edges retrieval recall`, plus the authored-link searches in Q4). **None returned such a
  study.** The surveys returned instead
  ([Retrieval-Augmented Generation with Graphs, arXiv:2501.00309](https://arxiv.org/html/2501.00309v2)) confirm
  the standard pipeline treats all edges as LLM-derived with no provenance distinction.
- **T2RAG states the field has barely looked at this.** It notes that few works study *implicit* graph
  construction, where nodes and edges are supplied at answer time rather than pre-built —
  [Beyond Chunks and Graphs: RAG through Triplet-Driven Thinking, arXiv:2508.02435](https://arxiv.org/pdf/2508.02435)
- **Closest attempt #1 — LEDGER's progressive edge-type ablation.** On a hierarchy-only baseline, adding
  semantic `RELATED` edges gave about **+2.7 points** and adding `DEPENDS` edges about **+4.7 points**, with
  the authors writing that `DEPENDS` edges "encode implicit prerequisites that semantic similarity cannot
  reliably recover" — [LEDGER, arXiv:2606.28379](https://arxiv.org/pdf/2606.28379).
  **Three caveats that disqualify it as the isolation study.** (a) The `DEPENDS` edges are *themselves
  LLM-inferred*, by a counterfactual test ("would the target unit be semantically incomplete without the
  prerequisite unit?") over candidate pairs restricted to units within about 5–10 positions — so both arms are
  inferred and neither is authored. (b) The metric is `[ANSWER]`/task: "consistency", measured by
  rule-based validators for reference validity, terminology and hierarchy over the *edited document*, plus a
  separate LLM-judged edit-quality score. There is no retrieval metric. (c) Retrieval is **single-hop only**
  (upstream dependencies and downstream dependents of the target nodes) with no depth parameter. Confirmed
  from the HTML: corpus is 1,900 test cases across six editing scenarios, documents 5K–100K tokens; baselines
  are full-document regeneration, 4K sliding window, RAG semantic search, Claude+Memory and Hierarchical RAG
  (DOM-containment expansion) — **no hybrid and no reranked baseline** —
  [LEDGER HTML](https://arxiv.org/html/2606.28379v1). *The +2.7 / +4.7 figures come from a search-snippet of
  Table 9 (Appendix E.3); my HTML fetch did not reach that appendix. Verify before quoting.*
- **Closest attempt #2 — hypergraph semantic-module ablation on HotpotQA.** Removing the semantic enhancement
  module cost up to **4.5% Recall@5** `[RETRIEVAL]`; removing the semantic-related hypergraph weight matrix
  caused about a **26% drop in Recall@5** `[RETRIEVAL]` —
  [Cross-Granularity Hypergraph RAG for Multi-hop QA, arXiv:2508.11247](https://arxiv.org/pdf/2508.11247).
  Caveat: "semantic" here means embedding similarity and weighting, not LLM-inferred latent relations, and
  there is no explicit-link arm to compare against. *Snippet-level evidence.*
- **Inferred edges measured as net-harmful without gating.** A validation gate that removed roughly **103
  noisy semantic candidate edges per topic** raised mean recall only from **0.164 to 0.182** `[RETRIEVAL]`;
  the authors state that "unsupported semantic edges may place noisy papers in the highest-ranked slots" and
  that with only four topics they read the gains descriptively rather than as statistically significant —
  [SurveyGraph, arXiv:2610.31317](https://arxiv.org/html/2610.31317v1). *Snippet-level evidence.*
- **Where edge provenance *has* been ablated, structure beats semantics.** In a textbook-graph study,
  "removing hierarchical edges causes performance to collapse, indicating that hierarchical structure is the
  most important signal," while "the drop from discarding sequential or semantic edges is measurable but
  modest relative to the full model." In RippleMem, dropping structural cues *raised* F1/BLEU-1 on some
  open-domain questions but *lowered* judge accuracy `[ANSWER]`; in REMem, removing synonymy edges lowered
  lexical recall while reasoning quality barely moved —
  [RippleMem, arXiv:2608.13334](https://arxiv.org/pdf/2608.13334);
  [REMem, arXiv:2602.13530](https://arxiv.org/pdf/2602.13530). *Snippet-level evidence; 2026 preprints.*
- **The one true explicit-vs-implicit edge-type ablation I found is not retrieval at all.** In a
  discourse-aware graph network for textual logical reasoning, collapsing explicit and implicit discourse
  edges into a single edge type lowered accuracy by about **4.6% (dev) and 1.8% (test)** `[ANSWER]` —
  [Discourse-Aware Graph Networks for Textual Logical Reasoning, arXiv:2207.01450](https://arxiv.org/pdf/2207.01450).
  This is a reasoning-over-given-text task, not retrieval over a corpus, and the "implicit" edges are
  discourse relations, not latent document relationships. It establishes only that edge *typing* can carry a
  few points of signal.
- **The authors of the only authored-link retrieval study say the comparison has not been run.** LARAG's
  limitations section states that the method uses only authored links and does **not** combine them with
  induced semantic graphs, which the authors leave to future work —
  [LARAG, arXiv:2605.07517](https://arxiv.org/html/2605.07517v1).
- Practitioner criticism of the claim's epistemics, for colour only (not evidence): one post argues
  GraphRAG's confidence comes from graph structure rather than document evidence, so transitively inferred
  relationships look as reliable as directly retrieved ones —
  [aisysdesign.substack.com](https://aisysdesign.substack.com/p/graphrag-failures-when-knowledge). Not
  peer-reviewed; cite as opinion if at all.

### Inferences
- The latent-link claim is a **mechanism story, not a measurement**. It is entirely plausible as a mechanism
  and it has never been isolated. The honest statement for the blog is: GraphRAG's published evaluation
  measures judge preference for breadth on whole-corpus sensemaking questions; no published work attributes
  any part of any win to edges that could not have been recovered from explicit structure.
- The direction of the available edge-provenance evidence is *against* the claim, not neutral: hierarchy and
  dependency edges dominate, semantic/inferred edges add a smaller increment, and in two of the ablations the
  inferred edges needed filtering to stop hurting. If you already have `validates` / `defer-to` / `delta`
  declared in markup, you hold the edge class that measured best in every ablation that distinguished them.
- The strongest *pro*-latent-link data point available (LEDGER's `DEPENDS` edges at +4.7pp) is an argument for
  **prerequisite/dependency semantics**, which in Brian's corpus is precisely what `defer-to` and `delta`
  already declare. The paper's own framing — edges that "semantic similarity cannot reliably recover" — is an
  argument for *reading* declared dependencies, not for inferring them.

### Gaps
- **The isolation study does not exist.** No corpus-fixed, edge-partitioned, retrieval-measured ablation of
  explicit-recoverable vs LLM-only-inferable edges, in GraphRAG or anywhere else. This is the headline gap of
  this note and should be stated as such.
- No paper reports the *proportion* of an LLM-extracted graph's edges that were already recoverable from
  explicit markup. The closest numbers are duplication rates (Q5), not provenance rates.
- The LEDGER Table 9 figures (+2.7 / +4.7) are snippet-level and I could not reach Appendix E.3 — verify at
  [arXiv:2606.28379](https://arxiv.org/abs/2606.28379) before quoting.

---

## Q2: Recursion depth — does hop 2 pay, does hop 3+ ever pay, how does precision degrade?

### Takeaway
Hop 2 is the only hop with a consistently positive record, and it pays **only when the seed set is weak or
the expansion is weighted**. Hop 3 is measured as roughly free-of-value and expensive: the clearest ablation
finds the third hop adding **nothing at R@20 and +0.005 at R@50** while increasing traversal cost. Hop 4 is
reported as degrading quality and prohibitive in latency. And the single most decision-relevant number: adding
one-hop neighbours *without* a scoring mechanism is **worse than not expanding at all** on MuSiQue and
HotpotQA.

### Cited Findings
- **Unweighted one-hop expansion hurts recall. HippoRAG's own ablation** (R@2 / R@5 on MuSiQue / 2Wiki /
  HotpotQA) `[RETRIEVAL]`:
  - Query nodes only: 37.1 / 41.0 — 59.1 / 61.4 — 55.9 / 66.2
  - Query nodes **plus neighbours, no PPR**: **25.4 / 38.5** — 53.4 / 74.7 — **47.8 / 64.5**
  - Full HippoRAG (PPR): 40.9 / 51.9 — 70.7 / 89.1 — 60.5 / 77.7

  So naive neighbour addition loses **11.7 R@2 on MuSiQue** and **8.1 R@2 on HotpotQA** against not expanding
  at all, and only helps 2Wiki R@5 (61.4 → 74.7). The whole benefit of the graph in HippoRAG lives in the
  *weighting*, not in the neighbours —
  [HippoRAG, arXiv:2405.14831](https://arxiv.org/html/2405.14831v3). Related ablations in the same table:
  removing node specificity drops MuSiQue R@5 to 50.2 and HotpotQA R@5 to 73.7; removing synonymy edges drops
  2Wiki R@5 to 85.6.
- **The cleanest explicit depth sweep: 1 insufficient, 2 substantial, 3 worthless.** GRAFT's traversal
  ablation reports that "a single hop is clearly insufficient", "extending to two hops raises recall
  substantially", and "a third hop adds nothing at R@20 and **0.005** at R@50" while increasing traversal
  cost; the authors fix the system at a **two-hop breadth-first walk** over the top-20 edges per facet
  `[RETRIEVAL]` — [GRAFT, arXiv:2608.22381](https://arxiv.org/html/2608.22381v1). Confirmed from the HTML:
  corpus is LitWeave, 11,359 S2ORC papers from \*ACL venues 2019–2026, split 6,875 train (2019–22) / 1,543 dev
  (2023) / 2,941 test (2024–26); the dense-graph teacher retriever scores **R@20 0.357 / R@50 0.560** on
  in-corpus test queries. Edges are *hybrid provenance*: facet similarity (weight 0.75, ColBERT-style maxsim
  over LLM-extracted facet embeddings) plus a citation score (weight 0.25, bibliographic coupling and
  co-citation). *The depth numbers are from a snippet of Appendix B.3, which my HTML fetch did not reach.*
- **Depth 3 is the peak but the whole sweep is within 2 F1 points, and cost explodes.** HELP's hop ablation on
  HotpotQA: QA F1 `[ANSWER]` peaks at **N=3 = 76.18%**, and across all N "remarkably remains above **74.5%**";
  N=4 shows "performance degradation". Retrieval time at N=3 is **577.2 s**, described as "manageable at N=1
  and N=2", surging at N=3 and "prohibitive" at N=4; the authors describe exponential growth in retrieval time
  with expansion hops — [HELP, arXiv:2602.20926](https://arxiv.org/html/2602.20926v1). Edges are LLM-extracted
  OpenIE triples (Llama-3.3-70B-Instruct), same splits/corpora as HippoRAG 2; baselines include BM25,
  Contriever, GTR, GTE-Qwen2-7B, GritLM-7B, NV-Embed-v2, RAPTOR, GraphRAG, LightRAG, HippoRAG, HippoRAG 2,
  LinearRAG, HyperGraphRAG — **no reranked baseline**.
- **Hop 2 measured as net-negative via hub nodes.** G-Long reports that "extending the subgraph expansion to
  L=2 leads to a notable degradation in performance", attributed to "traversing high-degree hub nodes
  [which] introduces semantic drift"; they ship **L=1** for both accuracy and efficiency —
  [G-Long, arXiv:2606.13115](https://arxiv.org/pdf/2606.13115). *Snippet-level.*
- **Precision falls monotonically with hops where both are reported.** DotRAG's MetaQA hop table: "restricting
  the hop expansion significantly reduces recall" while "allowing excessive hop expansion increases" recall at
  the cost of precision; precision generally fell as hops increased, and 3-hop recall was much lower than 1-
  or 2-hop recall in the default-iterations setting `[RETRIEVAL]` —
  [DotRAG, arXiv:2605.18760](https://arxiv.org/pdf/2605.18760). *Snippet-level; the quoted passage is
  truncated, so the full trade-off is unconfirmed.*
- **On authored documentation links, depth 3 scored best on F1 and was still rejected.** LARAG's tuning sweep
  over `(n_links, depth, top_m)` tested (0,0,0), (1,1,1), (1,3,1), (1,3,3), (2,1,2), (3,1,3), (3,2,3). Best F1
  `[ANSWER]` was **(1,3,1) at 0.8276**, but the authors shipped **(1,1,1)** for its better balance of chunks,
  tokens and time; the deeper/broader settings (1,3,3) and (3,2,3) retrieved **about 40 chunks** and scored
  **about 0.80** — [LARAG, arXiv:2605.07517](https://arxiv.org/html/2605.07517v1). This is the single most
  transferable depth result for Brian's situation: on real authored technical-doc links, fan-out at depth ≥2
  cost ~2.8 F1 points and 5x the chunks.
- **Deeper helps in a dedicated GNN, with a cost warning.** GNN-RAG's framing is that "deep GNNs (L=3) are
  found to be more effective at retrieving multi-hop information compared to shallow GNNs (L=1) and LLM-based
  retrievers", motivated by graph context that "expands exponentially at deeper levels"; the paper reports
  outperforming competitors by **8.9–15.5 points answer F1** `[ANSWER]` on multi-hop/multi-entity questions —
  [GNN-RAG, arXiv:2405.20139](https://www.alphaxiv.org/abs/2405.20139);
  [Findings of ACL 2025](https://aclanthology.org/2025.findings-acl.856.pdf). *The L=1 vs L=3 claim is from
  the alphaXiv summary, not the paper's own table — unverified.* Note this is KGQA over a curated KG (Freebase-
  style), not RAG over a text corpus, so it is weak evidence for the text-retrieval case.
- **PathRAG caps at 3 hops and never ablates depth.** PathRAG's reported ablation is on *path ordering*, not
  depth: average win rates of **56.44%** vs a random ordering and **55.64%** vs a hop-first ordering
  `[ANSWER]`, judge preference — [PathRAG, arXiv:2502.14902](https://arxiv.org/pdf/2502.14902). A secondary
  walkthrough states PathRAG is limited to a maximum depth of three hops
  ([Medium](https://medium.com/@sheenkocher/redefining-retrieval-with-pathrag-b8c6de88df22)) — **unverified,
  blog source**. A later spreading-activation paper that uses PathRAG as a baseline varies propagation depth T
  with a default of **T=3** and decay **α=0.7** —
  [Query-Aware Spreading Activation, arXiv:2606.30133](https://arxiv.org/html/2606.30133v1). *Snippet-level.*
- **Depth pays in a dense curated medical KG.** Raising the hop cap from 5 to 7 increased **recall@5 by +3.4
  points** `[RETRIEVAL]` at mild p95 latency cost, while plain BFS showed superlinear tail latency beyond 4
  hops — [TTFT-Aware Graph Chain-of-Thought, arXiv:2606.23108](https://arxiv.org/pdf/2606.23108).
  *Snippet-level; curated medical KG, not a document corpus.*
- **Hop depth can be a no-op in practice.** A bug report against a graph-memory tool found hops 1–3 returned
  identical result sets to hop 0 in most tests, hypothesising expanded neighbours were being truncated by
  top-k — [GitHub issue](https://github.com/Ntrakiyski/shared-living-memory/issues/3). Not a benchmark; cite
  only as an engineering caution that an unbounded expansion behind a fixed top-k silently does nothing.
- **Planner accuracy decays with hop count, independent of the graph.** GRASP's planner is **89.4% accurate on
  2-hop, 78.5% on 3-hop, 66.9% on 4-hop** (82.2% overall); EM is **52.1%** when the planned step count matches
  the true hop count vs **43.3%** when it does not `[ANSWER]` —
  [GRASP, arXiv:2605.16598](https://arxiv.org/html/2605.16598).

### Inferences
- **The depth evidence is consistent once you condition on seed strength.** Where the seed is weak (TF-IDF,
  BM25, entity-only NER, a citation-sparse first stage), hop 1→2 buys real recall. Where the seed is strong,
  hop 1 is already marginal and hop 2 is net-negative via hub-node drift and fan-out. Brian's seed is
  BM25+dense+RRF+cross-encoder — the strongest seed in this entire literature. No paper measures expansion on
  top of a seed that strong, and every paper that measured a progressively stronger seed saw the expansion
  gain shrink.
- **Precision and recall move in opposite directions with depth in every paper that reports both.** The
  blog-usable formulation: depth buys recall and sells precision, and a cross-encoder reranker is already the
  cheapest instrument for buying precision. Spending depth to buy recall and then spending the reranker to buy
  the precision back is a round trip.
- **Depth ≥3 has no supporting evidence in a text-corpus RAG setting.** The two positive depth-3+ results
  (GNN-RAG L=3, TTFT hop cap 5→7) are both over curated knowledge graphs with clean entity identity, not
  LLM-extracted graphs over prose. For a prose corpus, the measured ceiling is 2.
- Fan-out, not depth, is the controlling variable. LARAG's (1,3,1) at ~8 chunks scored 0.8276 while (1,3,3)
  and (3,2,3) at ~40 chunks scored ~0.80 — same depth, 5x the fan-out, 2.8 F1 points worse. A bounded-slot
  design (reserve n slots for expansion) is the only pattern with a positive record.

### Gaps
- **No study sweeps 1/2/3 hops reporting recall *and* precision side by side under one setup.** Every source
  gives one or the other, or gives recall at a fixed depth. I searched for this directly and it does not
  exist.
- No depth ablation exists over *typed* edges where expansion is filtered by relation type. The typed-edge
  gap flagged in the existing note (no retrieval literature on `supersedes` / `validates` / `depends-on`
  semantics) is confirmed and extends to depth.
- GRAFT's and DotRAG's depth numbers are snippet-level (Appendix B.3 and a truncated table respectively).
  HELP reports exact figures only for N=3; N=1, N=2 and N=4 are in a figure.

---

## Q3: Graph construction versus query-side multi-hop (decomposition, self-ask, IRCoT, agentic loops)

### Takeaway
Where a single paper runs both arms with one retriever, **query-side iteration beats the graph on MuSiQue and
HotpotQA and loses badly on 2WikiMultihopQA** — and the ranking flips with the benchmark, not with the method
class. The best-measured configuration in every such paper is *both*: decomposition plus a structured
retriever. Decomposition alone is actively harmful on the hardest benchmark (MuSiQue, −4.0 EM vs flat RAG) and
on the easiest (HotpotQA, −2.8 EM), and helps enormously only on the template-generated one (2Wiki, +17.0 EM).

### Cited Findings
- **The cleanest same-paper, same-retriever comparison: HippoRAG Tables 2 and 3** `[RETRIEVAL]`, R@2 / R@5:

  | Method | MuSiQue | 2Wiki | HotpotQA |
  |---|---|---|---|
  | ColBERTv2 (single-shot dense) | 37.9 / 49.2 | 59.2 / 68.2 | 64.7 / 79.3 |
  | Contriever | 34.8 / 46.6 | 46.6 / 57.5 | 57.2 / 75.5 |
  | **IRCoT + ColBERTv2** (query-side, no graph) | **41.7 / 53.7** | 64.1 / 74.4 | **67.9 / 82.0** |
  | **HippoRAG (ColBERTv2)** (graph, single-step) | 40.9 / 51.9 | **70.7 / 89.1** | 60.5 / 77.7 |
  | IRCoT + HippoRAG (ColBERTv2) (both) | 45.3 / 57.6 | 75.8 / 93.9 | 67.0 / 83.0 |
  | IRCoT + HippoRAG (Contriever) | 43.9 / 56.6 | 75.3 / 93.4 | 65.8 / 82.3 |

  — [HippoRAG, arXiv:2405.14831](https://arxiv.org/html/2405.14831v3). **Read the middle two rows against
  each other.** Query-side iteration over a plain retriever beats the graph over the same retriever on MuSiQue
  (+0.8 R@2 / +1.8 R@5) and on HotpotQA (+7.4 R@2 / +4.3 R@5), and loses heavily on 2Wiki (−6.6 / −14.7). Also
  note single-shot ColBERTv2 alone **beats the graph on HotpotQA** (64.7/79.3 vs 60.5/77.7) — graph expansion
  is net-negative there.
  All-recall (all gold passages retrieved, AR@2 / AR@5) `[RETRIEVAL]`: HippoRAG 10.2 / 22.4 (MuSiQue), 45.4 /
  75.7 (2Wiki), 33.8 / 57.9 (HotpotQA); ColBERTv2 6.8 / 16.1, 25.1 / 37.1, 33.3 / 59.0 — i.e. on HotpotQA the
  plain retriever has **higher** all-recall than the graph.
- **HippoRAG's cost claim for single-step vs iterative:** the body text says single-step HippoRAG is about
  **10–30× cheaper and 6–13× faster online** than IRCoT; the abstract says 10–20× for cost. Exact figures are
  in Appendix G, which I could not reach — [arXiv:2405.14831](https://arxiv.org/html/2405.14831v3).
- **GRASP (May 2026) is the best side-by-side of query-side and graph arms under one protocol.** EM / F1 /
  Judge `[ANSWER]`, retrieval setting, 1,000 queries per dataset:

  | Method | MuSiQue | 2Wiki | HotpotQA |
  |---|---|---|---|
  | Flat RAG (Table 5) | .372 / .466 / .551 | .483 / .570 / .685 | **.603 / .733 / .876** |
  | Self-Ask + RAG (Table 5) | .332 / .438 / .530 | .653 / .732 / .829 | .575 / .722 / .866 |
  | GeAR (Table 1) | .414 / .530 / .689 | .444 / .478 / .542 | .557 / .677 / .793 |
  | IRCoT + HippoRAG 2 (Table 1) | .478 / .604 / .673 | .717 / .805 / .913 | **.678 / .807 / .924** |
  | GRASP (Table 1) | **.505 / .651 / .765** | **.737 / .825 / .943** | .627 / .772 / .898 |

  — [GRASP, arXiv:2605.16598](https://arxiv.org/html/2605.16598). GRASP's MuSiQue F1 is .651 in Table 1 and
  .650 in Table 5 (the paper is internally inconsistent by 0.001). **Decomposition alone is a loss on two of
  three benchmarks**: Self-Ask+RAG is −4.0 EM vs flat RAG on MuSiQue and −2.8 EM on HotpotQA, and +17.0 EM on
  2Wiki. The authors' own reading: Self-Ask "improves over flat RAG on 2Wiki (+17 EM), where sub-questions are
  cleanly separable compositional lookups, but underperforms flat RAG on MuSiQue (−3 EM)… without structured
  retrieval to ground each sub-question's evidence, errors in decomposition cascade through the chain."
- **GRASP retrieval metrics, reported separately (MuSiQue passage recall, Table 2)** `[RETRIEVAL]`: single-pass
  k=5 — HippoRAG 2 **0.702**, GRASP propositions+RankVoting **0.729**, propositions+uniform 0.610,
  sentences+RankVoting 0.656, sentences+uniform 0.429. Simulated agentic retrieval k_i = 5 × hops — HippoRAG 2
  **0.818**, GRASP propositions+RankVoting **0.904**, propositions+uniform 0.848, sentences+RankVoting 0.853,
  sentences+uniform 0.648 — [arXiv:2605.16598](https://arxiv.org/html/2605.16598).
- **GRASP token cost** (total millions, retrieval setting): GRASP 15.89M / 10.9M / 12.68M on MuSiQue / 2Wiki /
  HotpotQA vs IRCoT+HippoRAG 2 at **31.49M / 21.52M / 26.82M** and GeAR at 16.69M / 9.89M / 13.18M. Indexing:
  GRASP **5.4M** tokens vs HippoRAG 2 **17.6M** (a 69% reduction); sentence-level indexing 3.3M. Success
  economy C_w (lower better): GRASP 12,616 / 9,477 / 8,847 vs IRCoT+HippoRAG 2 26,655 / 20,245 / 26,370 —
  [arXiv:2605.16598](https://arxiv.org/html/2605.16598).
- **GRASP corpora** (for size calibration against Brian's archive): retrieval setting — MuSiQue 11,656
  passages (avg 2,344 tokens), 2Wiki 6,119 (avg 903), HotpotQA 9,811 (avg 1,300); extended-context setting —
  MuSiQue 2,218 passages (avg 15,976 tokens), 2Wiki 1,986, HotpotQA 1,722 —
  [arXiv:2605.16598](https://arxiv.org/html/2605.16598).
- **GRASP's authors on HotpotQA's weakness as a discriminator:** they call HotpotQA the least informative
  dataset in their retrieval setting, "partly due to parametric leakage" —
  [arXiv:2605.16598](https://arxiv.org/html/2605.16598). Note flat RAG's HotpotQA judge score of **.876** is
  within 5 points of the best system in the table.
- **A 2026 cost-focused study reaches a near-tie with HippoRAG 2 on multi-hop QA and prefers it on judging.**
  EffiRAG reports EM/F1 **0.460 / 0.583** against HippoRAG 2's **0.447 / 0.570** `[ANSWER]` on 100-question
  dev slices of HotpotQA / 2Wiki / MuSiQue, while "the judge preferred HippoRAG2 in **312 of 600** order-level
  decisions". Its thesis: "the graph locates evidence, while source chunks supply the answer material."
  Removing source chunks cut cost 49.9% but dropped quality (195/44/1 against it in their testbed) —
  [When Is Graph Structure Worth Its Cost?, arXiv:2609.18099](https://arxiv.org/html/2609.18099v1)
  (17 Sep 2026, 6 pages). **The paper reports no retrieval metric of any kind** — retrieval is assessed only
  indirectly through answer quality.
- **The cheap non-graph control in that same study is close behind the graph system at ~1/7 the cost.** On
  UltraDomain (120 queries, 4 domains, 5 docs/domain), win/loss/split against EffiRAG and total cost
  `[ANSWER]`, judge preference: LightRAG-hybrid 93/7/20 at **$0.952**; PathRAG 39/26/55 at $0.954; NaiveRAG
  58/15/47 at **$0.139**; HyDE 51/20/49 at $0.194; **BM25-vector 43/21/56 at $0.145**; EffiRAG w/o LLM
  extraction 44/26/50 at $0.226; EffiRAG itself **$0.408**. EffiRAG was *slower* in total query time (1,163 s
  vs 888 s) — [arXiv:2609.18099](https://arxiv.org/html/2609.18099v1). The BM25-vector row — a lexical+dense
  hybrid with **no** graph and **no** reranker — loses only 43/21 to the proposed system at 35% of its cost.
- **Decomposition-free single retrievers are competitive at the frontier.** A survey notes GRITHopper, a
  single dense retriever, reaches state-of-the-art multi-hop results without explicit decomposition,
  "challenging the assumption that complex queries need it" —
  [A Survey of Query Optimization in LLMs, arXiv:2412.17558](https://arxiv.org/pdf/2412.17558).
  *Secondary/survey characterisation.*
- **Self-Ask's gains concentrate on the harder sets in a third paper.** PIKE-RAG reports Self-Ask's largest
  gains on the harder benchmarks, best in combination with retrieval on 2Wiki and MuSiQue, and notes GraphRAG
  in local mode was closely competitive on HotpotQA, "the easiest of the benchmarks tested" —
  [PIKE-RAG, arXiv:2501.11551](https://arxiv.org/pdf/2501.11551). *Snippet-level; note this conflicts in
  emphasis with GRASP's finding that Self-Ask loses 3 EM on MuSiQue. Report both.*
- Corroboration from the existing note's source: RAG performs better on single-hop and detail-oriented
  factual queries while GraphRAG is more effective on multi-hop reasoning-intensive questions —
  [RAG vs GraphRAG, arXiv:2502.11371](https://arxiv.org/html/2502.11371v3).

### Inferences
- **The method class does not determine the winner; the benchmark does.** Graph wins run
  2Wiki ≫ MuSiQue > HotpotQA, and query-side iteration runs the reverse. 2WikiMultihopQA is the one benchmark
  whose questions are template-generated from Wikidata relation paths, which is exactly the structure a
  relation graph reconstructs. Read Q6 before trusting any 2Wiki-driven win.
- **For a corpus with no training data and no gold multi-hop set, the query-side arm is the cheaper
  experiment.** IRCoT and self-ask need zero index-time spend and zero schema work; the graph arm needs an
  index pass whose cost the existing note already quantifies (115.5M input tokens per ~12k passages for MS
  GraphRAG; 17.6M for HippoRAG 2 per GRASP's measurement). A decomposition stage over an existing
  hybrid+rerank pipeline is a prompt, not an index.
- **The decisive caveat for Brian's pipeline: every baseline in this section is weaker than his.** The best
  non-graph baselines here are ColBERTv2 (dense late-interaction, no lexical fusion, no cross-encoder), flat
  dense RAG at k=5, and BM25-vector without a reranker. A BM25+dense RRF(k=60)+cross-encoder pipeline sits
  above all of them, and the measured pattern is that graph gains shrink monotonically as the baseline
  strengthens. Nothing in the literature estimates the residual gain at his baseline strength — it is
  extrapolation past the end of the data.
- Where decomposition + structure is the winner (GRASP, IRCoT+HippoRAG), the structure doing the work is a
  *proposition or passage* index with weighted propagation, not community summaries. GRASP's proposition graph
  beat HippoRAG's triple graph on recall (0.729 vs 0.702 single-pass; 0.904 vs 0.818 agentic), and GRASP's
  authors conclude "LLM-driven traversal alone is expensive and insufficient".

### Gaps
- No paper runs IRCoT / self-ask / agentic loops against a graph arm **on top of the same
  hybrid+cross-encoder first stage**. Every comparison uses a dense-only or ColBERT first stage.
- GRASP reports no results for standalone IRCoT, GraphRAG or LightRAG, so the query-side-vs-MS-GraphRAG cell
  of the matrix is empty in the one paper best placed to fill it.
- The PIKE-RAG vs GRASP disagreement on whether Self-Ask helps MuSiQue is unresolved; the setups differ
  (different readers, different retrievers) and I did not fetch PIKE-RAG's tables.

---

## Q4: Corpora that already have authored links — does traversing them help, and do LLM edges add anything?

### Takeaway
There is exactly **one** published retrieval study on traversing author-written links in technical
documentation, and its result is a **null**: following authored hyperlinks scored F1 0.8382 against 0.8378 for
simply retrieving twice as many chunks — a **+0.0004** difference — while cutting tokens about 18%. Its authors
state they did not test adding induced semantic edges on top, which is precisely the question. Meanwhile the
field with 20 years of measurement on *recovering* links rather than reading them — requirements traceability —
reports recovered-link precision of roughly **15–34%** at usable cutoffs. Authored links are free and exact;
inferred links are expensive and mostly wrong.

### Cited Findings
- **LARAG is the directly analogous study** — Sphinx-generated technical documentation where "links and
  anchors are author-written". Method: parse HTML with BeautifulSoup, normalise each link to an absolute URL,
  store ~12 words of surrounding context (`links_context`); retrieve top-k=5 by cosine; then a depth-first
  search follows *outgoing authored links on the fly*, controlled by `n_links`, `depth`, `top_m`, at chunk
  level with a global visited set, discarding unresolved links. **No graph is stored.** Candidate chunks are
  scored by cosine similarity between the *link context* and the candidate chunk. **No separate reranker
  model.** Shipped configuration `(n_links, depth, top_m) = (1,1,1)` —
  [LARAG, arXiv:2605.07517](https://arxiv.org/html/2605.07517v1) (arXiv preprint, metadata date 2026-05-11;
  no venue named).
- **LARAG's headline result (Table 7)** — 20 expert-written queries × 4 prompt strategies = 240 runs; metrics
  are BERTScore-style precision/recall/F1 against reference answers, i.e. `[ANSWER]` quality, **not**
  retrieval:

  | Config | Chunks | Tokens | Time (s) | P | R | F1 |
  |---|---|---|---|---|---|---|
  | RAG_k5 | 5 | 1135 | 8.22 | 0.8295 | 0.8430 | 0.8360 |
  | RAG_k10 | 10 | 1853 | 8.78 | 0.8290 | 0.8471 | 0.8378 |
  | **LARAG** | 8 | 1527 | 11.37 | 0.8303 | 0.8469 | **0.8382** |

  — [arXiv:2605.07517](https://arxiv.org/html/2605.07517v1). The authored-link stage beats plain top-5 by
  **0.0022 F1** and plain top-10 by **0.0004 F1**, at 1.4× the latency. An earlier run (Table 6, from prior
  work): RAG k=5 F1 0.8227 at 1226 tokens / 8.52 s; RAG k=10 F1 0.8229 at 2035 tokens / 10.65 s; LARAG (1,1,1)
  F1 0.8253 at 1669 tokens / 9.72 s.
- **LARAG's stated limitations**, verbatim in substance: higher latency in the final run (11.37 s), attributed
  partly to local execution variability; BERTScore may undervalue useful extra content; single corpus, single
  generator (gpt-4o-mini), no human evaluation; **hyperlinks may lead to outdated or deprecated content**,
  flagged as a reliability risk; and the method "uses only authored links and does not combine them with
  induced semantic graphs", left to future work —
  [arXiv:2605.07517](https://arxiv.org/html/2605.07517v1).
- **The one large authored-structure win is hierarchy traversal, not link traversal.** GraphSkill traverses
  the documentation *hierarchy* top-down with early pruning instead of ranking a flat corpus, reporting
  retrieval F1 rising from **about 28% to 79%** `[RETRIEVAL]` and per-task search time falling from **23.3 s
  to 9.1 s** versus flat agentic retrieval; it introduces its own benchmark, ComplexGraph —
  [GraphSkill, arXiv:2603.06620](https://arxiv.org/html/2603.06620). Caveat: the baseline is "flat agent
  retrieval", not a hybrid with a reranker, and the benchmark is the authors' own.
- **Authored code structure helps top-1 retrieval precision substantially.** Code-Craft builds hierarchical
  graph summaries of code using the **Language Server Protocol** — i.e. real compiler-resolved symbol
  references, not LLM-inferred edges — and reports "up to **82% relative improvement in top-1 retrieval
  precision** for large codebases like libsignal" `[RETRIEVAL]` across five codebases —
  [Code-Craft, arXiv:2504.08975](https://arxiv.org/html/2504.08975v1).
- **A repository-level KG maps documentation nodes back to the functions and classes they describe**, then
  expands to immediate context and dependencies —
  [Knowledge Graph Based Repository-Level Code Generation, arXiv:2505.14394](https://arxiv.org/html/2505.14394v1).
- **A typed graph over a real codebase enriched with PR history is explicitly exploratory**: CODENS evaluates
  on **11 questions** from one industrial Rails codebase and the authors call the results exploratory —
  [CODENS, arXiv:2607.18356](https://arxiv.org/pdf/2607.18356).
- **Requirements traceability — the precision of *recovered* links.** TraceRank frames TLR as IR (requirements
  as queries, artefacts ranked by similarity) and reports its baselines (TF-IDF, BM25, LSI, logistic
  learning-to-rank) at **LinkRecall@30 roughly 50–64%** but **ReviewPrecision@30 only about 15–19%**
  `[RETRIEVAL]` — [TraceRank, Frontiers in Computer Science, 2026](https://www.frontiersin.org/journals/computer-science/articles/10.3389/fcomp.2026.1960635/full)
- A 2026 multimodal RAG approach to TLR recovers **49 of 171 industrial test links** at **precision .338,
  recall .287, F1 .310** `[RETRIEVAL]`, against **F1 0.114** for a replication of a text-only baseline;
  confidence thresholding yields smaller, higher-precision link sets —
  [Multimodal Requirements Traceability Link Recovery in Industrial Settings, arXiv:2610.34503](https://arxiv.org/abs/2610.34503)
  (Oct 2026).
- T-SimCSE's authors judge that "its precision and recall are not yet sufficient to enable reliable full
  automation in industrial practice" and recommend semi-automated use with analysts reviewing ranked
  candidates — [T-SimCSE, arXiv:2603.11800](https://arxiv.org/html/2603.11800)
- Unfiltered requirement text actively damages link precision: one study argues that if irrelevant parts "are
  not filtered, they can lead to erroneously identified connections between artifacts", and proposes
  requirements classification as a pre-filter —
  [Requirements Classification for Traceability Link Recovery, KIT](https://publikationen.bibliothek.kit.edu/1000170117/152716343)
- A GraphRAG-based TLR method (DSDR) reports higher precision than the strongest LLM-based baseline when
  validated with GPT-4o, though the recall–precision balance varies by project —
  [Requirements Traceability Link Recovery via Retrieval-Augmented Generation](https://www.researchgate.net/publication/391481002_Requirements_Traceability_Link_Recovery_via_Retrieval-Augmented_Generation).
  *Snippet-level; no numbers obtained.*
- Historical contrast: an 11-year-old interactive method combining user feedback and configuration-management
  logs reported **precision 94.1% with recall 76.2%** on automatic parts — but the setting differs (human in
  the loop, VCS co-change evidence) and the result is dated —
  [Interactive Recovery of Requirements Traceability Links](https://www.researchgate.net/publication/272943419_Interactive_Recovery_of_Requirements_Traceability_Links_Using_User_Feedback_and_Configuration_Management_Logs).
  Methodological background on how trace-recovery evaluations should be read:
  [Advancing Trace Recovery Evaluation, arXiv:1602.07633](https://arxiv.org/pdf/1602.07633)
- **Legal citation networks:** I found no retrieval evaluation of traversing legal citation networks that
  separates authored citations from inferred edges. The closest commercial-registry work addresses hub-node
  duplication rather than retrieval quality —
  [Agentic Graph RAG for Auditable Commercial Registry Analysis, arXiv:2605.18770](https://arxiv.org/pdf/2605.18770).
  Recorded as a gap, not a finding.

### Inferences
- **The traceability literature is the strongest quantitative argument in this whole note, and it is an
  argument against inference.** Two decades of IR-based link recovery over exactly the kind of corpus Brian
  has — specs, requirements, typed relations — plateaus at roughly 15–34% precision at usable cutoffs, with
  its own authors declining to recommend full automation. An authored `validates` edge is 100% precise by
  construction and costs nothing. Spending LLM tokens to infer edges in a corpus that *declares* them is
  paying a high price for a lower-precision copy of something you already have.
- **But the authored-link traversal result is a null, which is the uncomfortable half.** LARAG's +0.0004 F1
  over RAG_k10 says that on a hyperlinked technical-doc corpus with no reranker, following the authored links
  is worth about as much as retrieving five more chunks. The one real benefit it demonstrates is **token
  efficiency** — 8 chunks and 1,527 tokens for the same answer quality as 10 chunks and 1,853 tokens. If a
  link stage is built, the defensible objective is *context efficiency at equal answer quality*, not a quality
  win.
- **The authored-structure results that *do* show large gains are hierarchy and compiler-resolved references,
  not prose links.** GraphSkill (hierarchy, 28%→79% retrieval F1) and Code-Craft (LSP references, up to 82%
  relative top-1 precision) both exploit structure with exact, machine-checkable semantics. Brian's typed
  relations (`validates`, `defer-to`, `delta`) are closer to this class than to a bare hyperlink — which is
  the one honest reason for optimism, and it is an argument for a *typed, bounded* expansion, not for a
  general graph index.
- Combining the two halves: the literature supports using declared structure as a **cheap, exact, bounded
  signal** and does not support inferring structure. It does not support expecting a large retrieval win from
  either, on top of a strong baseline.

### Gaps
- **Nobody has run the experiment LARAG's authors name as future work**: authored links vs authored links +
  LLM-induced semantic edges, same corpus, same queries, same reader. This is the second headline gap of this
  note and is the direct form of Brian's decision.
- LARAG measures answer BERTScore only. There is **no retrieval metric** (recall@k, nDCG) for authored-link
  traversal anywhere in the literature I surveyed.
- No study of authored-link traversal includes a cross-encoder reranker in the baseline or in the pipeline.
- No retrieval evaluation found on legal citation networks separating authored from inferred edges; none found
  on wikis with real hyperlinks since the HotpotQA-era work the existing note covers.
- The requirements-traceability numbers are precision of *recovery*, not of *traversal*. No TLR paper measures
  whether traversing the authored trace matrix improves downstream retrieval — the field's framing is
  recovering the matrix, not using it.

---

## Q5: Small corpora — is there a size below which graph indexing does not pay, and what fails?

### Takeaway
No paper names a document-count or token-count break-even threshold — that is a genuine gap. But the one
scaling experiment that exists points the wrong way for graphs at small scale: at the **smallest** corpus size
tested (~56k tokens), plain RAG **outperformed** the graph method on both fact retrieval and complex
reasoning, and the graph's advantage appeared only as the corpus grew to ~1.1M tokens. Separately, the failure
modes specific to small, homogeneous, heavily cross-referenced corpora are well documented and quantified:
~30.5% redundant nodes in baseline GraphRAG extraction, a known open bug that **discards** distinct entities
sharing a title, no entity resolution in stock Microsoft GraphRAG, and hub-node drift measured as making
2-hop expansion net-negative.

### Cited Findings
- **The only corpus-size scaling experiment.** GraphRAG-Bench / "When to use Graphs in RAG", Appendix G.5,
  tests the Novel subset at about **56k, 603k and 1,132k tokens**, comparing HippoRAG 2 against standard RAG.
  Standard RAG's complex-reasoning accuracy `[ANSWER]` fell from **58.64% at the small size to 43.20% at the
  large size**, attributed to vector retrieval pulling in similar-looking but irrelevant passages as the
  search space grows; HippoRAG 2 held roughly steady, with fact retrieval near 60%, credited to explicit
  entity and triple matching filtering that noise. **At the smallest size, standard RAG outperformed HippoRAG 2
  on both fact retrieval and complex reasoning** —
  [arXiv:2506.05690](https://arxiv.org/pdf/2506.05690); ICLR 2026 camera-ready at
  [proceedings.iclr.cc](https://proceedings.iclr.cc/paper_files/paper/2026/file/6c9e01d6cefbbf4cdd265032550e767f-Paper-Conference.pdf).
  **Verification warning:** these figures come from search-result extraction of Appendix G.5; two offset
  fetches of the HTML (offsets 20000 and 60000) did not reach that appendix, and the value **58.64** also
  appears in the paper's main table as RAG (w/ rerank) Complex-Reasoning accuracy on the **Medical** corpus
  (per the existing note), so the secondary summary may have mis-attributed the corpus. **Verify Appendix G.5
  directly before publishing this number.** The qualitative direction (small corpus favours plain RAG, large
  corpus favours the graph) is corroborated independently by
  [Lacuna's summary](https://lacuna.tiptreesystems.com/work/when-to-use-graphs-in-rag-a-comprehensive-analysis-for-graph-retrieval-augmented/wrk_c0305fd3be98c1031ccf0194297048a8).
- **Extracted graphs over technical corpora are nearly disconnected — a verified, decision-relevant number.**
  GraphRAG-Bench Table 9 (Appendix E) reports per-1k-token graph density: Novel corpus **19.6 entities / 20.9
  relations per 1k tokens, average degree 2.27, 0.66 proportion of non-isolated entities, average connected
  component size 3.99**; Medical corpus **11.8 entities / 6.2 relations per 1k tokens, average degree 1.05,
  0.48 non-isolated, average component size 3.15** —
  [arXiv:2506.05690v2](https://arxiv.org/html/2506.05690v2) (confirmed by direct fetch). On the
  clinical-guidelines corpus, **more than half the extracted entities are isolated** and the average component
  holds about three nodes. There is almost nothing to traverse.
- **Reasoning depth of the benchmark questions, same paper (Table 4, Appendix A.3)**: Novel — 1.69 (Fact
  Retrieval), 6.25 (Complex Reasoning), 4.64 (Contextual Summarize), 7.81 (Creative Generation); Medical —
  1.82, 5.23, 4.27, 8.27 — [arXiv:2506.05690v2](https://arxiv.org/html/2506.05690v2). Note these "depths" are
  inference hops as the authors define them, not retrieval hops, and the whole benchmark is 4,076 questions
  (Figure 13).
- **Entity resolution is not implemented in stock Microsoft GraphRAG.** Maintainers stated an
  entity-resolution setting was left over from a dropped step and the feature is not implemented; users report
  variants such as "Sherlock" and "Sherlock Holmes" remaining separate nodes —
  [microsoft/graphrag discussion #778](https://github.com/microsoft/graphrag/discussions/778);
  [issue #401](https://github.com/microsoft/graphrag/issues/401)
- **An open bug *discards* distinct entities that share a title** — directly the "entity resolution collapsing
  distinct ids" failure mode. Deduplication groups entities by title *and* type, but the drop step does not
  account for type, so "only the first node with a given title survives and the rest are discarded". Open,
  roughly 618 days old at time of search —
  [microsoft/graphrag issue #1718](https://github.com/microsoft/graphrag/issues/1718)
- **Attribute loss during merging** in a third-party implementation: resolution code discards non-null
  attributes from merged duplicates when the survivor holds a null for that key, and a merged duplicate can
  overwrite the survivor's description —
  [FalkorDB/GraphRAG-SDK issue #362](https://github.com/FalkorDB/GraphRAG-SDK/issues/362)
- **Duplication measured.** On 20 legal case graphs, **baseline GraphRAG had about 30.5% redundant nodes**;
  adding coreference resolution and structured prompts cut this to about **20.3%**. Surface variants such as
  "I-35" and "Interstate Highway 35" stayed fragmented without coreference resolution —
  [Inside CORE-KG, arXiv:2510.26512](https://arxiv.org/pdf/2510.26512);
  [CORE-KG, arXiv:2506.21607](https://arxiv.org/pdf/2506.21607)
- **Hub-node fan-out, measured as a quality loss.** G-Long reports 2-hop expansion degrading performance
  because "traversing high-degree hub nodes introduces semantic drift", and ships 1-hop —
  [arXiv:2606.13115](https://arxiv.org/pdf/2606.13115). *Snippet-level.*
- **Duplicate hub nodes from parallel ingestion**, with the remediation pattern (elect a master hub per
  normalised key and rewire edges from redundant hubs onto it) —
  [Agentic Graph RAG for Auditable Commercial Registry Analysis, arXiv:2605.18770](https://arxiv.org/pdf/2605.18770)
- **Extraction incompleteness scales the problem down, not up** — corroborating the existing note's 65.8% /
  65.5% answer-entity coverage figures: a small corpus gives the extractor fewer chances to mention an entity,
  so coverage gaps are more likely to be fatal rather than redundantly repaired —
  [RAG vs GraphRAG, arXiv:2502.11371](https://arxiv.org/html/2502.11371v3) (established in the existing note).
- **Practitioner heuristics, explicitly not evidence.** One guide lists four triggers against graph overhead —
  "your queries are mostly single-hop lookups, your corpus is small and stable, your latency budget is tight,
  and your users rarely ask relationship questions. Hit three of those and graph overhead rarely pays off" —
  [techsy.io](https://techsy.io/en/blog/graphrag-guide). Another notes that on corpus churn "you re-extract
  and re-summarize the affected subgraph, not just embed a delta", so a small but frequently updated corpus
  can carry a high graph cost — [mnemoverse.com, "The GraphRAG Tax"](https://mnemoverse.com/docs/library/graphrag-tax).
  Both are blog posts with no measurements; cite as framing only. A vendor page claiming ungoverned sources
  produce "30–40% more duplicate nodes" ([Atlan](https://atlan.com/know/what-is-graphrag/)) is **unverified**
  and should not be used.
- Counter-evidence to the "graph is stable at scale" reading: a 2026 large-corpus paper reports that LightRAG
  and HippoRAG show **lower context relevance than vanilla RAG** in its tests —
  [LinearRAG, arXiv:2510.10114](https://arxiv.org/pdf/2510.10114). *Snippet-level; conflicts with the
  GraphRAG-Bench scaling story and should be reported as a conflict.*

### Inferences
- **The scaling argument for graphs is an argument against them at Brian's size.** The pro-graph mechanism in
  the only scaling experiment is that vector retrieval degrades as the candidate pool grows; a few thousand
  retrievable units from ~120 specs is at or below the small end of that experiment, where plain RAG won. And
  the specific degradation mechanism — "similar-looking but irrelevant passages entering the top-k" — is
  exactly what a cross-encoder reranker is for. Brian already owns the cheap fix for the failure the graph is
  credited with avoiding.
- **A small, homogeneous, heavily cross-referenced corpus is the worst case for LLM extraction, not the best.**
  Homogeneity maximises surface-form collisions between distinct ids (the #1718 failure: same title, different
  type, silently dropped); heavy cross-referencing concentrates degree on a few hub specs (the G-Long drift
  failure); and small size means the ~30.5% redundancy and ~35% answer-entity miss rates are not averaged away
  by repetition. Declared edges have none of these failure modes, because identity is given rather than
  inferred.
- **The GraphRAG-Bench density numbers are the most quotable single fact for a blog post on this decision.**
  On a clinical-guidelines corpus — technical, entity-dense, well-structured, the nearest published analogue
  to a spec archive — LLM extraction produced average degree **1.05** and average component size **3.15**,
  with **52% of entities isolated**. A graph you cannot traverse more than three nodes into cannot deliver
  multi-hop traversal, whatever the depth parameter says.

### Gaps
- **No paper gives a corpus-size break-even threshold** in documents, tokens or passages. I searched for this
  directly. The three-point sweep in GraphRAG-Bench Appendix G.5 is the only size-varying experiment and it
  covers one dataset and one graph method, so it cannot establish a general threshold.
- The ~56k-token small-corpus figures are unverified at the primary source (see the verification warning
  above).
- No study measures hub-node degree distribution or duplicate-entity fan-out *as a function of corpus
  homogeneity*. The hub-node evidence is one dialogue-memory ablation and one registry engineering report.
- The LinearRAG claim that LightRAG and HippoRAG show lower context relevance than vanilla RAG conflicts with
  the GraphRAG-Bench scaling narrative and I did not fetch either primary table to adjudicate.

---

## Q6: Multi-hop benchmark validity — what fraction genuinely requires chaining?

### Takeaway
The critique is strong, primary, and old. A **single-hop** BERT reader reaches **67 F1 on HotpotQA**,
"comparable to state-of-the-art multi-hop models", and humans shown an *incomplete* evidence set still answer
**over 80%** of questions. Chen and Durrett independently conclude that **more than half** of both HotpotQA and
WikiHop "do not require multi-hop reasoning". The sharpest consequence for this decision: the benchmark where
graph methods win by the largest margin — 2WikiMultihopQA, where HippoRAG gains +14.7 R@5 over ColBERTv2 — is
itself heavily shortcut-solvable (DiRe F1 63.4 vs MuSiQue's 37.8) and is template-generated from Wikidata
relation paths. The graph's biggest win is on the benchmark least able to prove it.

### Cited Findings
- **Min et al., ACL 2019 — the primary quantification.** Verified from the abstract: "even highly
  compositional questions can be answered with a single hop if they target specific entity types, or the facts
  needed to answer them are redundant"; "single-hop reasoning can solve much more of the dataset than
  previously thought"; "we introduce a single-hop BERT-based RC model that achieves **67 F1** — comparable to
  state-of-the-art multi-hop models"; and in a setting "where humans are not shown all of the necessary
  paragraphs for the intended multi-hop reasoning", they "can still answer **over 80%** of questions". The
  authors recommend "possibly even a shift towards information retrieval style evaluations with large and
  diverse evidence collections" —
  [Min et al., *Compositional Questions Do Not Necessitate Multi-hop Reasoning*, arXiv:1906.02900](https://arxiv.org/abs/1906.02900);
  [ACL Anthology P19-1416](https://aclanthology.org/P19-1416/); code at
  [github.com/shmsw25/single-hop-rc](https://github.com/shmsw25/single-hop-rc). **The abstract gives no single
  percentage of single-hop-solvable questions.**
- A survey reports Min et al. split the HotpotQA dev set into **3,426 single-hop solvable** and **3,979
  single-hop non-solvable** questions — i.e. about **46%** single-hop solvable —
  [Multi-hop Question Answering (survey), arXiv:2204.09140](https://arxiv.org/pdf/2204.09140). **Secondary
  source; I could not verify the split against Min et al. itself.**
- **Chen and Durrett, NAACL 2019 — the second primary.** Verified from the ACL Anthology landing page: their
  sentence-level models, which "cannot perform multi-hop reasoning by design", are "still able to solve a
  large number of examples in both datasets", and "**more than half of the questions in WikiHop and HotpotQA
  do not require multi-hop reasoning** to solve"; a question-and-answer-only baseline performs strongly on
  WikiHop and a modified HotpotQA — [Chen & Durrett, NAACL 2019, N19-1405](https://aclanthology.org/N19-1405/);
  PDF at [cs.utexas.edu](https://www.cs.utexas.edu/~gdurrett/papers/chen-durrett-naacl2019.pdf)
- **The frequently-quoted Chen & Durrett category breakdown is UNVERIFIED.** A secondary summary gives: 35% of
  HotpotQA questions "solvable only by matching the entity type", 26% single-hop solvable even in the open
  domain, 8% non-compositional single-hop, leaving **only 27% requiring multi-hop reasoning**. I could not
  reach the paper's tables (the PDF streams were unreadable to my fetch tool) and a targeted search for the
  "27%" figure explicitly failed to find it in any primary source. **Record as unverified; verify at
  [N19-1405.pdf](https://aclanthology.org/N19-1405.pdf) before publishing.**
- **Das et al. 2019 BM25 easy/hard split:** BM25 accuracy **53.7% on the "easy" (mostly single-hop) subset,
  dropping to 25.9% on the "hard" (mostly multi-hop) subset** —
  reported in [arXiv:2204.09140](https://arxiv.org/pdf/2204.09140). *Secondary.*
- **MuSiQue's own measurements (from the existing note, repeated here because they are the discriminator).**
  DiRe disconnected-reasoning answer F1: **MuSiQue 37.8, HotpotQA 68.8, 2WikiMultihopQA 63.4**; human–model
  gap **9.6 points on HotpotQA vs 28.2 on MuSiQue-Ans**; MuSiQue's authors describe existing multi-hop
  benchmarks as "largely solvable via shortcuts" —
  [Trivedi et al., TACL 2022, arXiv:2108.00573](https://arxiv.org/pdf/2108.00573);
  [TACL](https://direct.mit.edu/tacl/article/doi/10.1162/tacl_a_00475/110996/MuSiQue-Multihop-Questions-via-Single-hop-Question)
- **A 2026 paper still calls HotpotQA uninformative, for a new reason.** GRASP's authors name HotpotQA the
  least informative dataset in their retrieval setting, "partly due to parametric leakage" — i.e. modern
  readers already know the answers —
  [GRASP, arXiv:2605.16598](https://arxiv.org/html/2605.16598). Their flat-RAG HotpotQA judge score of **.876**
  against the best system's **.924** quantifies the compression.
- **Corroboration that non-chaining questions dominate the measurable effects.** HippoRAG's gains are
  concentrated on 2Wiki (R@5 68.2 → 89.1) and small on MuSiQue (49.2 → 51.9) and *negative* on HotpotQA
  (79.3 → 77.7) `[RETRIEVAL]` — [arXiv:2405.14831](https://arxiv.org/html/2405.14831v3). Plain RAG leads on
  single-hop NQ (F1 64.78 vs 63.01 for the best graph variant) `[ANSWER]` —
  [arXiv:2502.11371](https://arxiv.org/html/2502.11371v3) (existing note).

### Inferences
- **The honest ranking of the three benchmarks by how much a "multi-hop win" on them should move a decision:**
  MuSiQue (DiRe 37.8, built to resist shortcuts) ≫ 2Wiki (DiRe 63.4, Wikidata templates) > HotpotQA (DiRe 68.8,
  single-hop reader at 67 F1, parametric leakage). Graph methods' margins run in roughly the **reverse** order
  of benchmark validity: largest on 2Wiki, modest on MuSiQue, zero-to-negative on HotpotQA. That inverse
  correlation is the single most important thing this note can tell the decision.
- **2WikiMultihopQA deserves an explicit caveat wherever a graph win is quoted from it.** Its questions are
  generated from Wikidata relation paths, so a retriever that reconstructs entity-relation structure is being
  tested on questions built from entity-relation structure. That is close to a circularity, and it is where
  every graph system posts its biggest number.
- **Min et al.'s own recommendation is, in effect, the pipeline Brian already has.** Their conclusion is to
  shift towards "information retrieval style evaluations with large and diverse evidence collections" —
  i.e. that the fix for shortcut-solvable multi-hop benchmarks is harder retrieval, not more reasoning
  structure.
- A practical corollary for Brian's own evaluation: if a graph stage is trialled, it must be judged on
  questions that are *verified* to need chaining (a DiRe-style disconnected-reasoning control, or a check that
  no single spec contains the answer), or the measurement will be dominated by the shortcut fraction and will
  not discriminate.

### Gaps
- **The exact "fraction that genuinely requires chaining" is not cleanly established in any primary source
  I could verify.** The two verifiable primary claims are "more than half do not require multi-hop reasoning"
  (Chen & Durrett) and "67 F1 single-hop / >80% human on incomplete evidence" (Min et al.). The precise
  percentages that circulate (46%, 27%, 35%/26%/8%/27%) are all secondary and I could not verify any of them.
- No equivalent shortcut audit exists for 2WikiMultihopQA beyond its DiRe score, and none at all for
  MultiHop-RAG.
- No paper re-runs modern graph-vs-hybrid comparisons restricted to the verified-multi-hop subset of a
  benchmark. That ablation — which would say directly how much of any graph win is a shortcut artefact — does
  not exist.

---

## Appendix: source-quality ledger

**Fetched and verified directly (primary tables read):**
LARAG ([2605.07517](https://arxiv.org/html/2605.07517v1)) · EffiRAG ([2609.18099](https://arxiv.org/html/2609.18099v1)) ·
HippoRAG ([2405.14831](https://arxiv.org/html/2405.14831v3)) · GRASP ([2605.16598](https://arxiv.org/html/2605.16598)) ·
GraphRAG-Bench Tables 4/9 ([2506.05690v2](https://arxiv.org/html/2506.05690v2)) ·
GRAFT corpus/baselines ([2608.22381v1](https://arxiv.org/html/2608.22381v1)) ·
HELP ([2602.20926v1](https://arxiv.org/html/2602.20926v1)) · LEDGER method/metric ([2606.28379v1](https://arxiv.org/html/2606.28379v1)) ·
Min et al. abstract ([1906.02900](https://arxiv.org/abs/1906.02900))

**Snippet-level only — agrees with fetched sources but not independently confirmed:**
GRAFT depth numbers (Appendix B.3) · LEDGER Table 9 edge-type deltas · GraphRAG-Bench Appendix G.5 scaling ·
DotRAG · G-Long · RippleMem · REMem · SurveyGraph · Cross-granularity hypergraph · TTFT medical ·
PathRAG depth cap · GNN-RAG L=1 vs L=3 · Chen & Durrett category breakdown · Min et al. dev-set split ·
Das et al. easy/hard · requirements-traceability figures (TraceRank, T-SimCSE, DSDR)

**Practitioner / vendor / non-peer-reviewed — framing only, never as evidence:**
techsy.io · mnemoverse.com "GraphRAG Tax" · Atlan (the 30–40% duplicate claim is unverified and should be
dropped) · aisysdesign.substack.com · the Medium PathRAG walkthrough · DEV Community GraphRAG explainer ·
Lacuna summary

**2026 preprints with no venue, flagged as such throughout:**
2605.07517 (LARAG) · 2605.16598 (GRASP) · 2609.18099 (EffiRAG) · 2606.28379 (LEDGER) · 2608.22381 (GRAFT) ·
2602.20926 (HELP) · 2606.13115 (G-Long) · 2605.18760 (DotRAG) · 2608.13334 (RippleMem) · 2602.13530 (REMem) ·
2610.31317 (SurveyGraph) · 2610.34503 (multimodal TLR) · 2603.11800 (T-SimCSE) · 2603.06620 (GraphSkill) ·
2607.18356 (CODENS) · 2605.18770 (registry) · 2606.23108 (TTFT) · 2606.30133 (spreading activation) ·
2510.10114 (LinearRAG) · 2510.26512 (Inside CORE-KG)

**Superseded / date notes:** GraphRAG-Bench ("When to use Graphs in RAG", arXiv:2506.05690, Jun 2025) now has
an ICLR 2026 camera-ready version at
[proceedings.iclr.cc](https://proceedings.iclr.cc/paper_files/paper/2026/file/6c9e01d6cefbbf4cdd265032550e767f-Paper-Conference.pdf);
numbers in this note are from arXiv v2 and should be re-checked against the camera-ready. A second, distinct
paper also named GraphRAG-Bench exists ([arXiv:2506.02404](https://arxiv.org/html/2506.02404)) — do not
conflate it with 2506.05690.
