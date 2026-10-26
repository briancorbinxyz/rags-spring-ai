# Graph expansion over declared links vs LLM-extracted knowledge graphs: the measured evidence

Scope note for the report writer: nearly every "GraphRAG wins" headline in this area comes from one of two
evaluation styles — (a) LLM-as-judge *pairwise preference* on LLM-generated "sensemaking" questions with no
gold answers (Microsoft GraphRAG, LightRAG), or (b) retrieval recall / answer F1 on multi-hop QA benchmarks
(HippoRAG, HippoRAG 2, GraphRAG-Bench). These two styles do not measure the same thing, and the systems that
win under (a) are not the ones that win under (b). Where a strong baseline (hybrid or reranked retrieval) is
present at all, the graph advantage narrows sharply or disappears on factoid lookup. Only two sources below
include an explicit reranked baseline (GraphRAG-Bench, and the RAG-vs-GraphRAG study); I found **no** published
evaluation that uses a proper BM25+dense hybrid with a cross-encoder reranker as the baseline against
Microsoft GraphRAG. That is itself a finding.

## Q1: What Microsoft GraphRAG does, what it costs to index, and what its own evaluation claims

### Takeaway
GraphRAG is an LLM-at-index-time pipeline: extract an entity/relationship graph from every chunk, partition it
hierarchically with Leiden, and pre-generate a natural-language summary for every community; queries are
answered map-reduce over those summaries. Its published evaluation measures *judge preference for
comprehensiveness and diversity on global sensemaking questions* — not retrieval accuracy, not factoid
correctness — and Microsoft's own follow-up (LazyGraphRAG) states that the indexing cost it replaces was
1000x higher than vector RAG's.

### Cited Findings
- Method, in the authors' own words: an LLM "derive[s] an entity knowledge graph from the source documents"
  and then "pregenerate[s] community summaries for all groups of closely related entities"; at query time each
  community summary produces a partial response and the partial responses are summarised into a final answer —
  [Edge et al., *From Local to Global: A Graph RAG Approach to Query-Focused Summarization*, arXiv:2404.16130](https://arxiv.org/abs/2404.16130)
- Community detection is **Leiden**, via the `graspologic` library, applied hierarchically — sub-communities are
  detected recursively until leaf communities cannot be partitioned further, giving a mutually exclusive,
  collectively exhaustive partition at each level —
  [arXiv:2404.16130v2](https://arxiv.org/html/2404.16130v2)
- **The paper does not evaluate local search at all.** The evaluated conditions C0–C3 are all global
  (community summaries at different hierarchy levels) plus TS (map-reduce over raw source text). "Local" appears
  only in Future Work, as a proposal to embed-match queries against graph annotations and generate community
  reports just-in-time. The vector-RAG condition (SS) is the closest thing to local retrieval in the experiments —
  [arXiv:2404.16130v2](https://arxiv.org/html/2404.16130v2)
- Datasets: podcast transcripts of *Behind the Tech with Kevin Scott* (1,669 chunks of 600 tokens with 100-token
  overlap, ~1M tokens) and news articles Sept 2013–Dec 2023 (3,197 chunks, ~1.7M tokens). Questions are
  LLM-generated via persona → task → question with K=M=N=5, giving 125 questions per dataset, deliberately *not*
  drawn from the corpus — [arXiv:2404.16130v2](https://arxiv.org/html/2404.16130v2)
- Metrics are head-to-head LLM-judge comparisons on **comprehensiveness, diversity, empowerment** plus
  **directness** as a control, each comparison repeated five times and averaged —
  [arXiv:2404.16130v2](https://arxiv.org/html/2404.16130v2)
- Win rates vs vector RAG (SS): comprehensiveness 72–83% (podcast) and 72–80% (news), all p < .001; diversity
  75–82% (podcast, p < .001) and 62–71% (news, p < .01). **Vector RAG produced the most direct answers in every
  comparison**, and empowerment results were mixed — the authors attribute that to the graph index losing
  specific examples, quotes and citations — [arXiv:2404.16130v2](https://arxiv.org/html/2404.16130v2)
- Win rates vs map-reduce summarisation of the *raw text* (TS) are small: e.g. 57% comprehensiveness / 57%
  diversity for intermediate-level podcast summaries, 64% / 60% for low-level news summaries. In the paper's
  second (claim-based) experiment, **no statistically significant differences were found among the global
  conditions, or between the global conditions and TS** — [arXiv:2404.16130v2](https://arxiv.org/html/2404.16130v2)
- Query-time context sizes (Table 2): podcast C0 26,657 tokens (2.6% of TS), C1 225,756, C2 565,720, C3 746,100,
  TS 1,014,611; news C0 39,770 (2.3%), C3 1,140,266, TS 1,707,694 —
  [arXiv:2404.16130v2](https://arxiv.org/html/2404.16130v2)
- Indexing time for the ~1M-token podcast dataset was **281 minutes** with gpt-4-turbo on a 16GB VM against a
  public OpenAI endpoint (2M TPM, 10k RPM). The paper gives **no total indexing token count and no dollar cost** —
  [arXiv:2404.16130v2](https://arxiv.org/html/2404.16130v2)

### Indexing cost measured by third parties (the number the paper omits)
- **GraphRAG indexing the MuSiQue corpus (11,656 passages) consumed 115.5M input + 36.1M output tokens.** On the
  same corpus LightRAG used 68.5M + 18.3M, HippoRAG 2 used 9.2M + 3.0M, and RAPTOR used 1.7M + 0.2M. For scale,
  HippoRAG 2's own indexing of that corpus cost **under $22** on the gpt-4o-mini batch API, finishing inside 24
  hours — so GraphRAG's token volume is roughly 12x that, on a corpus of ~12k passages —
  [Gutiérrez et al., *From RAG to Memory*, arXiv:2502.14802, Table 12](https://arxiv.org/html/2502.14802v1)
- Wall-clock index construction on MultiHop-RAG: plain RAG **135 s**, Community-GraphRAG **5,560 s**,
  KG-GraphRAG **7,702 s** (41x and 57x). Retrieval time: RAG 1,724 s, Community-GraphRAG 1,249 s, KG-GraphRAG
  14,434 s. Storage: RAG 127 MB, KG-GraphRAG 117 MB, Community-GraphRAG 165 MB —
  [*RAG vs. GraphRAG: A Systematic Evaluation and Key Insights*, arXiv:2502.11371, Table 4](https://arxiv.org/html/2502.11371v3)
- Microsoft's own follow-up states LazyGraphRAG's "indexing cost is identical to vector RAG and **0.1% of the
  costs of full GraphRAG**", that it requires no LLM summarisation at index time, and that it matches global
  search's answer quality on global queries at "**more than 700 times lower query cost**". The post also concedes
  full GraphRAG's upfront indexing costs "may be prohibitive for some users and use cases" —
  [Microsoft Research, *LazyGraphRAG: setting a new standard for quality and cost*](https://www.microsoft.com/en-us/research/blog/lazygraphrag-setting-a-new-standard-for-quality-and-cost/)
- Query-time cost of GraphRAG's global search, as measured by the LightRAG authors on their Legal dataset: 610
  level-2 community reports at ~1,000 tokens each ≈ **610,000 tokens per query**, and traversing each community
  requires hundreds of API calls; LightRAG's retrieval used **<100 tokens and a single API call** —
  [Guo et al., *LightRAG*, arXiv:2410.05779](https://arxiv.org/html/2410.05779v2)
- Independent per-query prompt-token measurement (GraphRAG-Bench, novel / medical corpora): vanilla RAG
  **879 / 954**; HippoRAG 2 1,008 / 1,020; RAPTOR 3,441 / 3,510; Fast-GraphRAG 4,204 / 4,298; HippoRAG 7,208 /
  7,342; **MS-GraphRAG local 38,707 / 39,821**; LightRAG 100,832 / 100,310; **MS-GraphRAG global 331,375 /
  332,881** — [*When to use Graphs in RAG*, arXiv:2506.05690](https://arxiv.org/html/2506.05690v2)
- Incremental update cost: adding a same-sized dataset forces GraphRAG to regenerate its community structure —
  the LightRAG authors estimate 1,399 communities × 2 × ~5,000 tokens ≈ **14 million tokens** plus ~1,399 × 2 +
  C_extract API calls, whereas LightRAG merges new nodes/edges by union with no reconstruction —
  [arXiv:2410.05779](https://arxiv.org/html/2410.05779v2)

### Inferences
- GraphRAG's published claim is narrow and it is honest about the shape of it: it wins on *perceived breadth* of
  whole-corpus answers and loses on *directness*. Nothing in the paper supports using it to improve retrieval
  precision for a specific-fact query.
- The comparison that matters for a cost decision is GraphRAG vs TS (map-reduce over raw text), not GraphRAG vs
  vector RAG — and that comparison is a near-tie (57–64% win rates in experiment 1; not significant in
  experiment 2). The index-time LLM spend buys mostly a reduction in *query-time* tokens (C0 at 2.6% of TS),
  i.e. it is an amortisation trade, not a quality trade.
- Microsoft shipping LazyGraphRAG at 0.1% of GraphRAG's indexing cost with comparable global-query quality is
  the strongest available evidence — from the vendor — that full graph extraction at index time is not where
  the quality came from.

### Gaps
- The GraphRAG paper reports no total indexing token count or dollar cost; the 281-minute figure is the only
  direct index-cost datum from the authors.
- Microsoft's LazyGraphRAG blog gives no numeric win rates (they appear only in a figure) and no token counts.
- I found no evaluation of GraphRAG's *local* search by its own authors.

## Q2: GraphRAG-style approaches against strong baselines — including a reranked baseline, and where they lose

### Takeaway
On the two independent benchmarks that include a reranked baseline, plain retrieval with a reranker **beats or
ties** every graph system on simple fact retrieval, and on one of the two corpora it beats Microsoft GraphRAG on
*every* task type including complex reasoning and summarisation. The graph advantage is real but confined to
multi-hop reasoning and corpus-level summarisation, and it is bought with 40x–400x the per-query prompt tokens.

### Cited Findings
- GraphRAG-Bench (ICLR 2026) compares Basic RAG with and without rerank against MS-GraphRAG (local/global),
  HippoRAG, HippoRAG 2, LightRAG, Fast-GraphRAG, RAPTOR, Lazy-GraphRAG, KGP, StructRAG and KET-RAG over four
  task levels (Fact Retrieval, Complex Reasoning, Contextual Summarize, Creative Generation) on a Gutenberg
  novel corpus and an NCCN clinical-guidelines corpus, with GPT-4o-mini —
  [arXiv:2506.05690](https://arxiv.org/html/2506.05690v2)
- **Novel corpus accuracy** — RAG (w/ rerank) 60.92 Fact / 42.93 Complex / 51.30 Summarize; MS-GraphRAG (local)
  49.29 / 50.93 / 64.40; HippoRAG 2 60.14 / 53.38 / 64.10; LightRAG 58.62 / 49.07 / 48.85; RAPTOR 49.25 / 38.59 /
  47.10. So the reranked baseline wins Fact outright, loses Complex by ~10 points to HippoRAG 2, and loses
  Summarize by ~13 points — [arXiv:2506.05690](https://arxiv.org/html/2506.05690v2)
- **Medical corpus accuracy** — RAG (w/ rerank) 64.73 Fact / 58.64 Complex / 65.75 Summarize beats MS-GraphRAG
  (local) 38.63 / 47.04 / 41.87 on all three, beats LightRAG on Fact (63.32) and Summarize (63.14), and is only
  narrowly behind HippoRAG 2 on Fact (66.28) and Complex (61.98) while beating it on Summarize (63.08). **This is
  a clean case of a reranked non-graph baseline beating Microsoft GraphRAG on every task type** —
  [arXiv:2506.05690](https://arxiv.org/html/2506.05690v2)
- The retrieval table shows the precision cost directly: on the novel corpus, RAG (w/ rerank) scores
  recall/relevance 83.21/77.77 on Fact and 64.47/**82.08** on Complex, while HippoRAG reaches recall **87.91** on
  Complex but relevance only **58.75** — higher recall, far lower precision —
  [arXiv:2506.05690](https://arxiv.org/html/2506.05690v2)
- The authors' stated conclusions: basic RAG "is comparable to or outperforms GraphRAG in simple fact retrieval
  tasks", attributed to graph processing adding redundant or noisy information; global GraphRAG "retrieves more
  relevant information but with more redundancy"; GraphRAG "may also introduce noisy context due to prompt
  inflation"; and the design takeaway "**more relationships ≠ better performance**" (build quality graphs, not
  large ones) — [arXiv:2506.05690](https://arxiv.org/html/2506.05690v2)
- The second independent study (RAG vs GraphRAG) finds complementary rather than dominant behaviour, with
  Llama-3.1-8B: **NQ F1** RAG **64.78** > Community-GraphRAG (local) 63.01 > HippoRAG 2 61.03 >> KG-GraphRAG
  (triplets only) 34.28; **HotpotQA F1** RAG 60.04 < local 61.66 < HippoRAG 2 63.01; **MultiHop-RAG accuracy**
  RAG 67.02 < local 69.01 < HippoRAG 2 70.27, KG-GraphRAG 41.24 —
  [arXiv:2502.11371](https://arxiv.org/html/2502.11371v3)
- Summarisation in the same study: ROUGE-2 F1 on SQuALITY RAG 10.08 ≈ Community-GraphRAG (local) 10.10; on
  **ODSum-story RAG 9.81 > local 8.49 > global 5.46** — i.e. GraphRAG global *loses* on multi-document
  summarisation under reference-based metrics, while the LLM-as-judge comparison in the same paper favours it for
  Diversity and favours RAG for Comprehensiveness. The metric choice flips the verdict —
  [arXiv:2502.11371](https://arxiv.org/html/2502.11371v3)
- That study's reranker is `BAAI/bge-reranker-large` and its iterative-retrieval variant is IRCoT; reranking
  raised HippoRAG 2 and Community-GraphRAG scores on MultiHop-RAG. **It does not include a lexical+dense hybrid
  baseline** — its "RAG" is dense retrieval over ~256-token chunks at top-k=10 —
  [arXiv:2502.11371](https://arxiv.org/html/2502.11371v3)
- Routing/fusion gains are modest: selecting a pipeline per query type improved the best baseline by **1.1%** on
  MultiHop-RAG with Llama-70B, and running both pipelines and integrating them by **6.4%** — the latter at double
  the query cost — [arXiv:2502.11371](https://arxiv.org/html/2502.11371v3)
- In LightRAG's own evaluation, its **overall** win rate against Microsoft GraphRAG is 54.8 / 52.0 / 52.8 /
  **49.6** on Agriculture / CS / Legal / Mix — a coin flip or a loss — even though its win rate against NaiveRAG
  is 67.6 / 61.2 / 84.8 / 60.0 — [arXiv:2410.05779](https://arxiv.org/html/2410.05779v2)
- In HippoRAG 2's retrieval-oriented evaluation, **GraphRAG's average F1 across seven datasets is 49.6 — below
  the plain dense retriever NV-Embed-v2 at 57.0 and below BM25 at 47.7 only marginally** (HippoRAG 2 59.8,
  HippoRAG 53.1, GTR 50.4, RAPTOR 48.8, Contriever 46.9, no retrieval 38.4) —
  [arXiv:2502.14802, Table 2](https://arxiv.org/html/2502.14802v1)

### Inferences
- Taken together, three independent evaluations (GraphRAG-Bench, RAG-vs-GraphRAG, HippoRAG 2) put Microsoft
  GraphRAG *below* a competent dense retriever on short-answer QA, and the only benchmark that includes a
  reranker puts the reranked baseline above it on fact retrieval on both corpora and on all task types on one.
  The decision-relevant conclusion is that GraphRAG is not a retrieval-quality upgrade; it is a
  query-focused-summarisation system.
- The systems that *do* beat strong baselines on retrieval (HippoRAG 2) are the ones that use the graph only to
  re-rank/propagate over passages rather than to replace passages with generated text.
- Because the only reranked comparison that favours the baseline on all tasks is the *medical* (clinical
  guidelines) corpus, there is a plausible pattern — technical, well-structured, entity-dense documents are
  where extraction adds least — but this is a single corpus and should be stated as a hypothesis, not a finding.

### Gaps
- No published evaluation I could find pits Microsoft GraphRAG against a BM25+dense hybrid with a cross-encoder
  reranker, tuned, on the same corpus. Every "strong baseline" in the literature is either dense-only or
  dense+reranker.
- A SAGE/IOS Press regulatory case study (EU banking) reports an optimised GraphRAG pipeline winning 58–67% of
  pairwise comparisons against Naive, HyDE and Hybrid RAG baselines with a bge-reranker-v2-m3 variant, but the
  authors propose the method and it is a single domain — [journals.sagepub.com/doi/full/10.3233/SSW260013](https://journals.sagepub.com/doi/full/10.3233/SSW260013).
  I did not fetch the full text and cannot confirm the baselines were tuned; treat as vendor-adjacent.
- I found no independent *reproduction* of GraphRAG's own podcast/news win rates.

## Q3: What LightRAG, HippoRAG / HippoRAG 2 and RAPTOR do differently, and what their evaluations measured

### Takeaway
LightRAG is GraphRAG with the community-summary layer removed and one-hop neighbour expansion put in its place,
and it is evaluated only on judge preference; HippoRAG 2 keeps passages as the retrieval unit and uses the graph
purely as a propagation structure (Personalized PageRank), and it is the only one of the three that wins on
measured recall and F1; RAPTOR replaces the graph with a recursive summary tree and trails the graph methods in
every independent table I found.

### Cited Findings
- **LightRAG** method: an LLM extracts entities and relationships from chunks, generates key-value profiles for
  each node and edge, and deduplicates into a KG. Retrieval is dual-level — low-level targets specific entities
  and **their immediate neighbours**, high-level targets broader themes and relations — both via keyword
  extraction, vector matching and **one-hop neighbour expansion**. Updates merge by union of nodes and edges
  with no rebuild — [arXiv:2410.05779](https://arxiv.org/html/2410.05779v2)
- LightRAG's evaluation: UltraDomain Agriculture (12 docs, ~2.0M tokens), CS (10, ~2.3M), Legal (94, ~5.1M), Mix
  (61, ~0.6M); 125 generated sensemaking questions each; baselines NaiveRAG, RQ-RAG, HyDE, GraphRAG; metrics are
  pairwise LLM-judged comprehensiveness / diversity / empowerment / overall with alternated positions.
  **No accuracy, EM, F1 or recall is reported anywhere** — [arXiv:2410.05779](https://arxiv.org/html/2410.05779v2)
- **HippoRAG 2** method: LLM OpenIE extracts triples per passage into an open KG; phrase nodes with embedding
  cosine above **0.8** get synonym edges; each passage becomes a passage node linked to its phrases by
  "contains" edges. At query time the query is matched to triples, an LLM filters the **top-5** triples
  ("recognition memory"), the surviving phrase nodes *and all passage nodes* seed **Personalized PageRank**, and
  passages are ranked by PageRank score; passage-node reset probabilities are scaled by a weight factor of
  **0.05** — [arXiv:2502.14802](https://arxiv.org/html/2502.14802v1)
- HippoRAG 2's explicit contrast: GraphRAG and LightRAG "use LLM-generated structures (summaries, KG-based
  expansion) to build or extend the retrieval corpus", whereas HippoRAG 2 uses the KG to *aid retrieval*, which
  "introduce[s] less LLM-generated noise" — [arXiv:2502.14802](https://arxiv.org/html/2502.14802v1)
- HippoRAG 2 measured results (Llama-3.3-70B reader, NV-Embed-v2 retriever, 7 datasets: NQ, PopQA, MuSiQue,
  2Wiki, HotpotQA, LV-Eval, NarrativeQA): **average recall@5 78.2 vs 73.4** for NV-Embed-v2 alone; MuSiQue
  recall@5 74.7 vs 69.7; 2Wiki recall@5 **90.4 vs 76.5**; 2Wiki F1 71.0 vs 61.5; LV-Eval F1 12.9 vs 9.8;
  abstract claims ~7% improvement on associative memory tasks over the best embedding model —
  [arXiv:2502.14802](https://arxiv.org/html/2502.14802v1)
- HippoRAG 2 ablations (average recall@5): full model 87.1; replacing query-to-triple with NER-to-node 74.6;
  with query-to-node **59.6**; removing passage nodes 81.0; removing the LLM triple filter 86.4. The anchoring
  mechanism, not the graph, carries most of the gain —
  [arXiv:2502.14802, Table 4](https://arxiv.org/html/2502.14802v1)
- HippoRAG 2 generalises across retrievers: it improved every dense retriever it was layered on, e.g. NV-Embed-v2
  from 69.7 to 74.7 recall@5 on the MuSiQue subset — [arXiv:2502.14802, Table 6](https://arxiv.org/html/2502.14802v1)
- **RAPTOR** method: recursively embed, cluster and summarise chunks into a tree of summaries at increasing
  abstraction, then retrieve from the tree. Own headline claim: combining RAPTOR retrieval with GPT-4 improves
  the best QuALITY accuracy by **20 absolute points** —
  [Sarthi et al., *RAPTOR*, arXiv:2401.18059](https://arxiv.org/abs/2401.18059)
- Independent placement of RAPTOR: average F1 48.8 across HippoRAG 2's seven datasets (below BM25's 47.7 only
  marginally, below NV-Embed-v2's 57.0) — [arXiv:2502.14802](https://arxiv.org/html/2502.14802v1); and in
  GraphRAG-Bench, novel-corpus 49.25 Fact / 38.59 Complex / 47.10 Summarize, below RAG (w/ rerank) on all three,
  **but the highest faithfulness score on creative generation (70.85)** —
  [arXiv:2506.05690](https://arxiv.org/html/2506.05690v2)
- In HippoRAG's original QA table, RAPTOR scored 35.7 EM / 45.3 F1 on MuSiQue, 46.3 / 53.8 on 2Wiki and 58.1 /
  71.2 on HotpotQA — [HippoRAG, NeurIPS 2024](https://proceedings.neurips.cc/paper_files/paper/2024/file/6ddc001d07ca4f319af96a3024f6dbd1-Paper-Conference.pdf)
- HippoRAG 2's paper notes that the original HippoRAG's entity-centric extraction "causes context loss during
  indexing and inference" — [arXiv:2502.14802](https://arxiv.org/html/2502.14802v1)

### Inferences
- The axis that predicts measured retrieval quality is not "graph vs no graph" but **"does the graph replace the
  retrieval unit or only re-rank it?"** LightRAG and GraphRAG substitute generated text for passages and lose on
  recall/F1; HippoRAG 2 keeps passages and wins. For an engineer with typed links, this argues for using links
  to *re-score passages*, never to replace them with synthesised link summaries.
- HippoRAG 2's ablation (87.1 → 59.6 when anchoring degrades from query-to-triple to query-to-node) says the
  expensive part is picking the right anchor. A corpus with declared links gets the graph for free but still has
  to solve anchoring, which is where the measured errors concentrate (see Q5).

### Gaps
- HippoRAG 2's Table 2 lists LightRAG's average F1 as **6.6**, which is almost certainly an answer-format
  artifact (LightRAG emits long sensemaking prose, scored against short gold answers) rather than a real
  retrieval result. Do not report it as a quality comparison.
- There is no single benchmark that evaluates LightRAG, HippoRAG 2 and RAPTOR with a shared reader, shared
  retriever and controlled context budget. An independent two-arm study on 1,000 MuSiQue and 1,000
  2WikiMultiHopQA questions reportedly found HippoRAG 2 best on recall@5 and LightRAG best on answer accuracy
  while warning the frameworks deliver 2.5 to 20 documents per question with no budget control — I could not
  locate a primary, citable URL for this study and so report it only as an unverified gap.
- RAPTOR's own paper tables (NarrativeQA, QASPER, QuALITY vs DPR/BM25/SBERT) were not retrievable from the
  abstract page; only the +20-point QuALITY claim is confirmed from the primary source.

## Q4: Using the typed links a corpus already carries, instead of extracting a graph

### Takeaway
The best measured evidence on declared links is a decade of HotpotQA work, and it says something uncomfortable:
on the benchmark whose bridge questions were *literally constructed from Wikipedia hyperlinks*, a graph retriever
that followed those hyperlinks held the state of the art for about a year and was then beaten outright by a
plain two-stage dense retriever that uses no links at all. Link expansion reliably rescues a *weak* anchor
retriever and reliably hurts a *strong* one — that pattern repeats across hyperlink and citation settings.

### Cited Findings
- HotpotQA's bridge questions were created by having annotators write questions over **pairs of paragraphs
  connected by a hyperlink**, which is why hyperlink graphs fit that benchmark unusually well — surfaced in
  search of the HotpotQA construction literature; the primary graph-retrieval paper built on exactly this
  structure — [Asai et al., *Learning to Retrieve Reasoning Paths over Wikipedia Graph for Question Answering*, ICLR 2020, arXiv:1911.10470](https://arxiv.org/html/1911.10470v2)
- Asai et al. build a paragraph graph from **Wikipedia hyperlinks plus document structure** and use a recurrent
  retriever with beam search to score *sequences* of paragraphs, reporting a gain of **more than 14 points** over
  the prior best on HotpotQA full-wiki — [arXiv:1911.10470](https://arxiv.org/html/1911.10470v2); summarised also
  by [Salesforce Research](https://www.salesforce.com/blog/learning-to-retrieve-reasoning-paths-from-the-wikipedia-graph-for-question-answering/)
- **Then the link-free dense retriever beat it.** HotpotQA full-wiki test, answer EM/F1 — Graph Recurrent
  Retriever (hyperlinked passage graph) **60.0 / 73.0**; MDR with an ELECTRA reader and *no* hyperlinks
  **62.3 / 75.3**. Support EM/F1 49.1 / 76.4 vs **57.5 / 80.9**; joint EM/F1 35.4 / 61.2 vs **41.8 / 66.6**.
  Other link-using systems in the same table: Semantic Retrieval 46.5 / 58.8, Transformer-XH 51.6 / 64.1, HGN
  56.7 / 69.2, DrKIT 42.1 / 51.7 — [Xiong et al., *Answering Complex Open-Domain Questions with Multi-Hop Dense Retrieval*, ICLR 2021, arXiv:2009.12756](https://ar5iv.labs.arxiv.org/html/2009.12756)
- MDR states it "does not require access to any corpus-specific information" and that inference relies only on
  the dense passage index and query representations, **with no explicit graph construction from hyperlinks or
  entity linking** — [arXiv:2009.12756](https://ar5iv.labs.arxiv.org/html/2009.12756)
- Retrieval-stage numbers from the same paper show the nuance precisely. Recall at **2 retrieved passages** on
  HotpotQA: TF-IDF **10.3**, **TF-IDF + Linked 17.3**, DrKIT 38.3, **MDR 65.9**. Adding hyperlink expansion to a
  lexical anchor nearly *doubled* recall@2 for essentially no model cost — and was still beaten by a factor of
  ~4 by the dense retriever — [arXiv:2009.12756, Table 1](https://ar5iv.labs.arxiv.org/html/2009.12756)
- With reranking, MDR reaches SP EM 81.2 / answer recall 88.2 vs the Graph Recurrent Retriever's 75.7 / 87.5;
  **without** reranking MDR is behind at 65.9 / 75.4. So the link-based graph retriever beat un-reranked dense
  retrieval and lost to reranked dense retrieval —
  [arXiv:2009.12756, Table 2](https://ar5iv.labs.arxiv.org/html/2009.12756)
- MDR on multi-evidence FEVER: retrieval precision/recall/F1 **45.7 / 69.1 / 55.0** vs **30.6 / 53.8 / 39.0** for
  the entity-linking baseline; downstream KGAT+BERT-rerank label accuracy rises from 65.9 to 77.9 (loose-multi)
  and 51.0 to 72.1 (strict-multi) when MDR supplies the evidence —
  [arXiv:2009.12756](https://ar5iv.labs.arxiv.org/html/2009.12756)
- MDR is "about 10 times faster than current state-of-the-art systems at a similar performance level", using
  HNSW on 16 CPU cores with retrieval time negligible against the model forward pass —
  [arXiv:2009.12756](https://ar5iv.labs.arxiv.org/html/2009.12756)
- Citation-graph expansion shows the same weak-anchor/strong-anchor split. In a taxonomy-generation study over a
  citation network, **BM25 with 1-hop neighbour information reached 0.7445 accuracy vs 0.3308 without**, while
  dense SentenceBERT got *worse* with neighbours (**0.2602 with vs 0.5234 without**); the authors conclude
  neighbour information and topological structure play a crucial role because papers on the same topic cite each
  other — [*Taxonomy Tree Generation from Citation Graph*, arXiv:2410.03761](https://arxiv.org/pdf/2410.03761)
  (numbers from search-result extraction, not a full fetch — verify before quoting)
- Diminishing returns past one hop, measured on a citation corpus: 1-hop Recall@50 **0.331**, adding relations
  among the retrieved neighbours ("1.5-hop") **0.372**, full 2-hop **0.386** — the authors conclude full two-hop
  expansion gives only a small additional recall gain while increasing the candidate graph and entailment
  workload by an order of magnitude —
  [*Structurally-bounded Agentic Graph Exploration for Evidence-Grounded Scholarly DeepSearch*, arXiv:2608.24809](https://arxiv.org/pdf/2608.24809)
  (snippet-level evidence only)
- Contrary data point on hop depth: a facet-aware scientific-literature retrieval system judged one hop
  insufficient and settled on two, finding a third added little —
  [*GRAFT*, arXiv:2608.22381](https://arxiv.org/pdf/2608.22381) (snippet-level only)
- Anchored one-hop expansion as a *slot reservation* in a ranked list: reserving one of five context slots for
  one-hop expansion gave **+1.9 to +2.9 points at early progress levels and −0.16 at 60%** in a troubleshooting
  agent — [*RAFT*, arXiv:2609.20754](https://arxiv.org/pdf/2609.20754) (snippet-level only)
- One-hop expansion for long-term dialog memory: its benefit over no expansion was **marginal under augmented
  re-ranking, and it degraded results when only session-level scoring was used** —
  [*Does Memory Need Graphs?*, arXiv:2601.01280](https://arxiv.org/pdf/2601.01280) (snippet-level only)

### Inferences
- The consistent shape across hyperlinks, citations, dialog memory and troubleshooting corpora: **link expansion
  substitutes for retrieval strength.** Its measured gain is largest where the first-stage ranker is weak
  (BM25-only, TF-IDF, no reranker) and shrinks to noise — or goes negative — once a dense retriever plus a
  reranker is in place. An engineer whose baseline is already hybrid+rerank should expect a small gain at best
  from one-hop expansion, and should expect the gain to show up as recall, not precision.
- One hop is where the value is. Every source that measured hop depth found the second hop costing an order of
  magnitude more for a few points of recall (0.331 → 0.372 → 0.386), with one contrary case.
- Declared links are strictly cheaper than extracted ones — zero model calls at index time against GraphRAG's
  115.5M input tokens per ~12k passages — and in the one setting where the two were effectively compared on the
  same questions (HotpotQA bridge: real hyperlinks vs LLM/learned graph structure), neither beat a well-trained
  two-stage dense retriever. The cost asymmetry is therefore the decisive argument for declared links over
  extraction, not a quality advantage.

### Gaps
- **I found no published study that holds the corpus fixed and compares "follow the typed links already in the
  markup" against "extract a graph with an LLM" on the same queries with the same reader.** This is the single
  biggest hole in the literature for the decision at hand. The closest proxies are MDR vs Asai (declared
  hyperlinks vs no graph) and HippoRAG 2 vs GraphRAG/LightRAG (extracted graph as re-ranker vs as corpus).
- I found **no** retrieval literature at all on semantic link types of the "supersedes" / "validates" /
  "depends-on" kind. All the link-expansion evidence concerns untyped or single-typed edges (hyperlink,
  citation, co-mention). Any claim that typed edges let you filter expansion by relation is, as far as I can
  find, untested in a published retrieval evaluation.
- Several of the one-hop expansion data points above (arXiv 2601.01280, 2608.24809, 2608.22381, 2609.20754,
  2410.03761) are from search-result extraction rather than full fetches, and most are 2026 preprints. They
  agree with each other and with the fetched sources, but the writer should either verify them or hedge them.

## Q5: Documented failure modes of graph expansion in retrieval

### Takeaway
The measured failures are not in the graph: they are in picking the anchor and in the propagation step. HippoRAG
2's own error analysis finds graph construction at fault in only 2% of failures, while anchor/filter errors
account for 26% and bad PageRank rankings over *correct* anchors for 50%. The precision cost of merging
neighbours into the ranking shows up directly as a 20-point relevance drop in GraphRAG-Bench, and global
community summaries invite confabulation on unanswerable questions (19.27 vs 96.01).

### Cited Findings
- **Anchor error dominates.** In 100 HippoRAG 2 samples with recall@5 < 1.0, triple filtering and graph search
  were the main error sources: before filtering, 7% of samples had no supporting-document phrase matching the
  query-derived phrases; **after filtering, 26% had none**; **18% were left with zero triples** and fell back to
  dense retrieval. The authors note the triple filter's precision "has room for further improvement" —
  [arXiv:2502.14802, Appendix E](https://arxiv.org/html/2502.14802v1)
- **The graph itself is rarely the problem.** Only **2%** of those samples lacked supporting phrases within the
  one-hop neighbourhood of the linked nodes —
  [arXiv:2502.14802, Appendix E](https://arxiv.org/html/2502.14802v1)
- **Propagation dilutes.** In **50%** of the failing samples, at least half the linked phrases *were* in
  supporting documents, yet Personalized PageRank still returned unsatisfactory rankings —
  [arXiv:2502.14802, Appendix E](https://arxiv.org/html/2502.14802v1)
- **Neighbourhood dilution, measured.** On GraphRAG-Bench's novel corpus, HippoRAG reaches 87.91 recall on
  Complex Reasoning with only **58.75** relevance, against RAG (w/ rerank) at 64.47 recall / **82.08** relevance.
  The authors describe global GraphRAG as retrieving "more relevant information but with more redundancy", warn
  that GraphRAG "may also introduce noisy context due to prompt inflation", and conclude "more relationships ≠
  better performance" — [arXiv:2506.05690](https://arxiv.org/html/2506.05690v2)
- **Confabulation on unanswerable queries.** On MultiHop-RAG's Null queries (no answer in the corpus),
  Community-GraphRAG (Global) scores **19.27 against plain RAG's 96.01** — the community-summary layer answers
  questions it should refuse — [arXiv:2502.11371](https://arxiv.org/html/2502.11371v3)
- **Extraction incompleteness is large and measurable.** In the same study, only **65.8% of HotpotQA answer
  entities and 65.5% of NQ answer entities appear in the constructed KG** at all, which the authors give as the
  reason KG-GraphRAG (triplets only) collapses to 34.28 F1 on NQ and 41.24 accuracy on MultiHop-RAG —
  [arXiv:2502.11371](https://arxiv.org/html/2502.11371v3)
- **Graph quality tracks extractor quality, i.e. cost.** Rebuilding the graph with GPT-4o instead of GPT-4o-mini
  raised MultiHop-RAG overall accuracy from **71.17 to 75.08** with Llama-70B — four points of answer quality
  purchased with a more expensive index pass — [arXiv:2502.11371](https://arxiv.org/html/2502.11371v3)
- **Error amplification under imperfect KGs.** A 2026 study of GraphRAG robustness reports that structurally
  plausible but unsupported triples "can be amplified during graph matching or propagation, leading to retrieval
  drift", identifies two recurring issue modes in LLM-built graphs (spurious noise and incomplete information),
  and finds in its own failure analysis that a planner omitting a bridge variable sends retrieval to the wrong
  entity — [*Toward Robust GraphRAG: Mitigating Retrieval Drift and Hallucination from Imperfect Knowledge Graphs*, arXiv:2603.14828](https://arxiv.org/html/2603.14828v2)
  (snippet-level evidence; single preprint)
- **Entity-centric extraction loses context.** HippoRAG 2's authors state that HippoRAG's entity-centric
  extraction "causes context loss during indexing and inference" —
  [arXiv:2502.14802](https://arxiv.org/html/2502.14802v1)
- **Topic drift / topically-adjacent distractors** are documented generically in RAG failure taxonomies —
  topically adjacent distractor chunks score high, enter the context window and anchor the generator on related
  but wrong content, with reranking, a precision filter and a required cited span as the mitigations —
  [ActiveWizards RAG failure taxonomy](https://activewizards.com/blog/the-rag-failure-taxonomy-12-ways-production-retrieval-pipelines-break/);
  [latenteval.ai, RAG pipeline failure modes](https://latenteval.ai/research/rag-pipeline-failure-modes)
  (practitioner sources, not peer-reviewed)

### Inferences
- For a declared-link pipeline, the HippoRAG 2 error profile is the most transferable evidence available: the
  expensive, error-prone part is deciding *which* node to expand from, and the second most expensive part is
  deciding how much weight an expanded neighbour inherits. Both of those are design decisions you keep even
  when the edges are free.
- The relevance/recall split in GraphRAG-Bench is the quantified form of "neighbours merged into the ranking
  cost precision". If neighbours are to be added, the measured-safe pattern is the one HippoRAG 2 and RAFT use —
  reserve a bounded number of slots, or re-score existing passages, rather than extending the candidate pool
  unboundedly.
- The Null-query result argues against ever letting a synthesised, graph-derived summary stand in for a cited
  source passage: it removes the signal that would otherwise let the system abstain.

### Gaps
- I found no study that isolates and quantifies *topic drift specifically caused by graph expansion* with a
  controlled experiment (e.g. drift as a function of hop count or neighbour degree). The drift evidence is
  either generic-RAG practitioner taxonomy or a single 2026 GraphRAG-robustness preprint.
- The phrase "neighbourhood dilution" has no established measurement in this literature; GraphRAG-Bench's
  recall/relevance pair is the closest available proxy.

## Q6: Multi-hop QA — including the case where a passage only *points at* the answer passage

### Takeaway
Graph structure helps most exactly where the benchmark forbids shortcuts: HippoRAG 2's largest recall gain is
+13.9 points on 2WikiMultihopQA and +5.0 on MuSiQue, while on single-hop NQ plain RAG wins. But the canonical
"pointer" case — HotpotQA bridge questions, built from hyperlinked paragraph pairs — was solved at least as well
by a dense retriever that re-embeds the query *with the first retrieved passage* and never touches the link
graph. The pointer can be followed through text rather than through an edge.

### Cited Findings
- Existing multi-hop benchmarks are "largely solvable via shortcuts", which is why MuSiQue was built bottom-up
  from composable single-hop pairs where one step depends on the other's answer, plus unanswerable contrast
  questions (MuSiQue-Full). The DiRe disconnected-reasoning answer F1 is **37.8 for MuSiQue vs 68.8 for HotpotQA
  and 63.4 for 2WikiMultihopQA**, and a single-hop model drops 30 F1 on MuSiQue-Ans. MuSiQue-Ans holds ~25,000
  answerable 2–4 hop questions — [Trivedi et al., *MuSiQue: Multihop Questions via Single-hop Question Composition*, TACL 2022, arXiv:2108.00573](https://arxiv.org/pdf/2108.00573);
  [TACL version](https://direct.mit.edu/tacl/article/doi/10.1162/tacl_a_00475/110996/MuSiQue-Multihop-Questions-via-Single-hop-Question)
- Graph gains concentrate on the shortcut-resistant sets. HippoRAG 2 recall@5: **2Wiki 90.4 vs 76.5** for
  NV-Embed-v2 alone (+13.9), **MuSiQue 74.7 vs 69.7** (+5.0); 2Wiki F1 71.0 vs 61.5 (+9.5) —
  [arXiv:2502.14802](https://arxiv.org/html/2502.14802v1)
- On single-hop and detail-oriented questions the graph is a liability: plain RAG leads on NQ (F1 **64.78** vs
  63.01 for the best graph variant) and on the single-hop and detail subsets of NovelQA —
  [arXiv:2502.11371](https://arxiv.org/html/2502.11371v3)
- On HotpotQA, where shortcuts exist, the graph margin is small: RAG 60.04 F1 → Community-GraphRAG (local) 61.66
  → HippoRAG 2 63.01 — [arXiv:2502.11371](https://arxiv.org/html/2502.11371v3)
- The pointer case, stated structurally: HotpotQA bridge questions were written over paragraph pairs connected by
  a hyperlink, and Asai et al.'s retriever scores *sequences* of paragraphs along those hyperlinks with beam
  search — exactly "follow the declared pointer" —
  [arXiv:1911.10470](https://arxiv.org/html/1911.10470v2)
- HopRetriever formalises a "hop" as a hyperlink plus the document it points to, encoding the mention embedding
  alongside the document embedding, and reports large gains over earlier evidence-retrieval methods on HotpotQA —
  [HopRetriever, AAAI](https://ojs.aaai.org/index.php/AAAI/article/download/17568/17375)
- And the pointer case resolved without pointers: MDR beats the hyperlink-graph retriever on HotpotQA full-wiki
  (62.3/75.3 vs 60.0/73.0 answer EM/F1; 41.8/66.6 vs 35.4/61.2 joint) using only a dense index, "with no explicit
  graph construction from hyperlinks or entity linking" —
  [arXiv:2009.12756](https://ar5iv.labs.arxiv.org/html/2009.12756)
- Secondary report (unverified): GraphRAG-Bench's multi-hop retrieval gains are said to concentrate on harder
  cross-document sets at **+31 points on MuSiQue and +28 on 2Wiki**, and vanilla RAG's complex-reasoning accuracy
  is said to fall from ~58% to ~43% as corpus size grows while GraphRAG stays stable — both figures come from
  blog summaries of [arXiv:2506.05690](https://arxiv.org/html/2506.05690v2) (e.g.
  [VentureBeat](https://venturebeat.com/orchestration/stop-graphing-everything-when-graphrag-actually-beats-vector-rag),
  [Persistent](https://www.persistent.com/blogs/when-does-graphrag-outperform-traditional-rag/)) and I could not
  confirm them in the paper's visible tables. Do not report as measured.

### Inferences
- The strongest argument *for* following declared links in a multi-hop setting is not a quality number, it is a
  training-data argument: MDR's second hop works because a bi-encoder was trained on 90k HotpotQA-style
  question-plus-first-passage pairs. On a private corpus with no such training data, the declared link is the
  only pointer signal available for free, and the comparison is against un-reranked dense retrieval — the regime
  where the graph retriever *did* win (MDR direct 65.9 SP EM vs Graph Recurrent Retriever 75.7).
- The corpus-size claim, if it holds up, is the most decision-relevant open question: all the head-to-head
  numbers above come from corpora of 4k–23k passages or 0.6M–5M tokens. Nothing here tells you how a reranked
  hybrid behaves against graph expansion at 10^6 passages.

### Gaps
- **No study I found isolates the case the question asks about most sharply**: a bridging passage that contains
  only a reference or pointer (a citation, a "supersedes" attribute, a bare link) with no topical discussion of
  the target. HotpotQA bridge paragraphs discuss their link targets; they are not pure pointers. This case
  appears to be genuinely unmeasured in the published literature.
- I could not verify the GraphRAG-Bench per-dataset multi-hop deltas (+31 MuSiQue / +28 2Wiki) against the
  primary paper; they circulate only in secondary write-ups.
- No published multi-hop evaluation I found varies hop-count or anchor correctness while holding everything else
  fixed, so "error amplification when the anchor is wrong" remains quantified only inside HippoRAG 2's 100-sample
  error analysis.
