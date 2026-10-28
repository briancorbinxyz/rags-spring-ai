# Honest Evaluation and Improvement of a Retrieval System Against a Very Small Question Set

Scope: what a 90-question evaluation (53 main / 47 answerable, 24 "time", 13 held-out) can and cannot support;
what process keeps a solo developer honest; whether "we got to 100%" is an honest claim.

All interval and power numbers below were computed directly (exact binomial / exact McNemar / two-proportion
z-power) for the specific n values in play; the arithmetic is reproducible from the formulas named beside each table.

## Statistical power and confidence on tiny IR eval sets (n=13, 24, 47)

### Takeaway
At these sizes a proportion like Hit@4 carries a 95% interval 13-45 percentage points wide, and a *perfect* score
of 47/47 only establishes a true rate of **>=93.8%**. Under any paired test, flipping 3 questions is unreachable
significance (exact two-sided p=0.25); you need **6 clean wins with zero regressions** before a change is
statistically distinguishable from noise — which means the entire 8-question gap is, at best, exactly one
significant step.

### Cited Findings
- Clopper-Pearson is the exact (conservative, guaranteed-coverage) binomial interval; Wilson is the recommended
  closed-form approximation and is preferred to the Wald/normal interval at small n and extreme p. Recht et al.
  use Clopper-Pearson for exactly this purpose and note that at n=10,000 the 95% interval is "at most +/-1%",
  which is the scale at which a proportion becomes a measurement — [Do ImageNet Classifiers Generalize to ImageNet?](https://arxiv.org/abs/1902.10811)
- Webber, Moffat & Zobel show that estimating between-system variability from a prior or trial experiment "leaves
  wide margins of error", i.e. small-sample variance estimates are themselves unreliable — [Statistical Power in Retrieval Experimentation, CIKM 2008](https://people.eng.unimelb.edu.au/ammoffat/abstracts/wmz08cikm.html)
- Fuhr's list of common IR evaluation mistakes explicitly includes ignoring effect sizes and ignoring multiple
  comparisons when significance testing — [Some Common Mistakes In IR Evaluation, SIGIR Forum 2017](https://unpaywall.org/10.1145%2F3190580.3190586); Sakai disputes several items but agrees researchers
  must know that other views exist — [On Fuhr's Guideline for IR Evaluation, SIGIR Forum](https://unpaywall.org/10.1145%2F3451964.3451976)
- Recent simulation work finds that multiple testing without adjustment "drastically increases the Type I error
  rate", and recommends Wilcoxon signed-rank with Benjamini-Hochberg adjustment as the power/error-control
  balance — [Towards Reliable Testing for Multiple IR System Comparisons, 2025](https://arxiv.org/pdf/2501.03930)
- A 2025 study measures hypothesis-testing errors in retrieval evaluation directly, confirming these are
  empirical not merely theoretical concerns — [Measuring Hypothesis Testing Errors in the Evaluation of Retrieval Systems](https://arxiv.org/pdf/2507.07924)

### Computed numbers (the core deliverable)

**95% two-sided confidence intervals on a proportion.** Wilson first, Clopper-Pearson (exact) second.

| n | score | point | Wilson | width | Clopper-Pearson | width |
|---|---|---|---|---|---|---|
| 13 | 10/13 | 0.769 | [0.497, 0.918] | 0.421 | [0.462, 0.950] | 0.488 |
| 13 | 12/13 | 0.923 | [0.667, 0.986] | 0.319 | [0.640, 0.998] | 0.358 |
| 13 | 13/13 | 1.000 | [0.772, 1.000] | 0.228 | [0.753, 1.000] | 0.247 |
| 24 | 17/24 | 0.708 | [0.508, 0.851] | 0.343 | [0.489, 0.874] | 0.385 |
| 24 | 20/24 | 0.833 | [0.641, 0.933] | 0.292 | [0.626, 0.953] | 0.326 |
| 24 | 22/24 | 0.917 | [0.742, 0.977] | 0.235 | [0.730, 0.990] | 0.260 |
| 24 | 24/24 | 1.000 | [0.862, 1.000] | 0.138 | [0.858, 1.000] | 0.142 |
| 47 | 33/47 | 0.702 | [0.560, 0.813] | 0.253 | [0.551, 0.827] | 0.276 |
| 47 | 39/47 | 0.830 | [0.699, 0.911] | 0.213 | [0.692, 0.924] | 0.232 |
| 47 | 42/47 | 0.894 | [0.774, 0.954] | 0.180 | [0.769, 0.965] | 0.196 |
| 47 | 45/47 | 0.957 | [0.858, 0.988] | 0.131 | [0.855, 0.995] | 0.140 |
| 47 | 47/47 | 1.000 | [0.924, 1.000] | 0.076 | [0.925, 1.000] | 0.075 |
| 53 | 53/53 | 1.000 | [0.932, 1.000] | 0.068 | [0.933, 1.000] | 0.067 |
| 90 | 90/90 | 1.000 | [0.959, 1.000] | 0.041 | [0.960, 1.000] | 0.040 |

**One-sided 95% lower bound on a perfect score** (the "rule of three" exactly: `0.05^(1/n)`):

| n | perfect score | 95% one-sided lower bound |
|---|---|---|
| 13 | 13/13 | **0.794** |
| 24 | 24/24 | **0.883** |
| 47 | 47/47 | **0.938** |
| 53 | 53/53 | 0.945 |
| 90 | 90/90 | 0.967 |

So: *47/47 is statistically indistinguishable from a system whose true Hit@4 is 94%.* 24/24 is indistinguishable
from 89%. 13/13 is indistinguishable from 80%.

**One question is worth:** 7.69pp at n=13; 4.17pp at n=24; 2.13pp at n=47; 1.89pp at n=53; 1.11pp at n=90.
Granularity, not precision — a proportion on n=24 can only take 25 values, so a bootstrap of it adds nothing over
the exact binomial and will undercover.

**Paired tests (sign test / exact McNemar).** Only discordant questions (ones that flip) count; n is irrelevant.

| questions flipped, all in one direction | two-sided p | one-sided p |
|---|---|---|
| 3 | **0.250** | 0.125 |
| 4 | 0.125 | 0.0625 |
| 5 | 0.0625 | 0.031 |
| 6 | **0.031** | 0.016 |
| 7 | 0.016 | 0.008 |
| 8 | **0.0078** | 0.004 |

**Minimum net wins for p<0.05 two-sided, allowing regressions:**

| regressions | wins needed | net |
|---|---|---|
| 0 | 6 | +6 |
| 1 | 8 | +7 |
| 2 | 10 | +8 |
| 3 | 12 | +9 |

**Minimum detectable difference, two independent systems, alpha=0.05, power=0.80** (two-proportion z). This is
the relevant number if you ever compare against a published baseline rather than your own previous run:

| n | baseline 0.70 | 0.80 | 0.85 | 0.90 |
|---|---|---|---|---|
| 13 | unreachable | unreachable | unreachable | unreachable |
| 24 | 0.287 (6.9 q) | unreachable | unreachable | unreachable |
| 47 | 0.224 (10.5 q) | 0.179 (8.4 q) | unreachable | unreachable |
| 53 | 0.213 (11.3 q) | 0.172 (9.1 q) | 0.145 (7.7 q) | unreachable |
| 90 | 0.171 (15.4 q) | 0.140 (12.6 q) | 0.120 (10.8 q) | 0.094 (8.5 q) |

"Unreachable" means no difference in [0,1-p] achieves 80% power at that n — the ceiling binds first.

### Inferences
- **The three pure-ranking questions cannot be a significant improvement under any test.** 3 flips, 0 regressions
  is p=0.25. This is not a sample-size problem that more main-set questions would fix: it is a property of the
  effect size. The honest framing of such a fix is "a mechanism with measured firing behaviour and no observed
  regression", never "an improvement".
- **The 8-question gap is worth at most one significant result.** Closing all 8 with zero regressions gives
  p=0.0078. Closing them across four episodes of 2 gives four results at p=0.50 each. If you want a defensible
  statistical claim out of this work, bank the whole gap into a single pre-registered confirmatory evaluation
  rather than spending it two questions at a time.
- Paired randomisation/permutation tests are the IR-standard choice and are more powerful than the sign test
  because they use per-query magnitudes, so they are worth running on MRR. But with binary Hit@4 the
  randomisation test degenerates to the sign test, so the table above is the hard floor for Hit@4 and
  all-evidence@4 regardless of which test you pick.
- Report the lower bound, not the point estimate. "Hit@4 >= 93.8% (95%, one-sided, n=47)" is a defensible
  sentence; "Hit@4 = 100%" is a statement about 47 specific strings.

### Gaps
- I could not find a published IR paper that reports exact binomial bounds for Hit@k on sets this small; the
  practice in IR is to report means over topics with t-test/randomisation CIs, which assumes a continuous measure.
  The binomial treatment here is standard statistics applied to the IR setting, not a cited IR convention.

## How many queries do IR evaluations need?

### Takeaway
The IR community's own floor is **25 topics minimum, 50 for a reliable evaluation**, and the power-analysis
literature pushes that to **150-200+** for distinguishing systems. 47 answerable questions sits just above the
absolute floor; the 24-question time set and the 13-question held-out set are below every published
recommendation, and the literature's advice when you cannot have more topics is to judge *more topics shallowly*
rather than few topics deeply.

### Cited Findings
- Voorhees & Buckley (SIGIR 2002, "The effect of topic set size on retrieval experiment error") computed error
  rates directly for topic sets up to size 25 and extrapolated beyond; they found "the error rates ... are larger
  than anticipated" and concluded researchers should be cautious about declaring one method better, especially
  with few topics — reported in [Cheap IR Evaluation (Roitero thesis, arXiv 2011.00479)](https://arxiv.org/abs/2011.00479)
- The resulting community guideline: at least 25 topics, more is better, with 50 topics producing reliable
  evaluations; Carterette et al. broadly confirmed this using 200 topics — [same source](https://arxiv.org/abs/2011.00479)
- Webber, Moffat & Zobel recommend roughly **150 topics** to statistically distinguish one system from another;
  Sakai's power analyses suggest "hundreds of topics" may be needed for reasonable power in current test
  collections — [Statistical Power in Retrieval Experimentation, CIKM 2008](https://people.eng.unimelb.edu.au/ammoffat/abstracts/wmz08cikm.html); [Cheap IR Evaluation](https://arxiv.org/abs/2011.00479)
- An empirical reading of the standard 50-topic TREC collection: it supports detecting **AP differences of only
  0.06-0.08**; required sample sizes for individual system pairs ranged from about **10 to 722 topics** depending
  on the pair — reported in [Cheap IR Evaluation](https://arxiv.org/abs/2011.00479)
- Sakai's topic-set-size design gives three methods (paired t-test, one-way ANOVA, confidence intervals) for
  choosing the number of topics; the vital input is an estimate of the **population variance of the chosen
  measure**, which requires a topic-by-run score matrix, so **a pilot dataset is a prerequisite**. Required topic
  counts "vary vastly" by measure because within-system variances differ — [Topic set size design, Information Retrieval Journal 19(3):256-283](https://link.springer.com/article/10.1007/s10791-015-9273-z); [Sakai & Shang, EVIA 2016 correction using within-system rather than total variance](https://research.nii.ac.jp/ntcir/workshop/OnlineProceedings12/pdf/evia/02-EVIA2016-SakaiT.pdf)
- Worked example of the method in practice: NTCIR-12 Short Text Conversation set its topic count at **100** using
  topic-set-size design from a pilot of only five similar runs — [Topic Set Size Design with the Evaluation Measures for Short Text Conversation, AIRS 2015](https://ir.webis.de/anthology/2015.airs_conference-2015.25/)
- Sakai's book covers this material with downloadable Excel tools for topic-set-size design based on paired and
  two-sample t-tests, ANOVA and confidence intervals (Ch. 6 topic set size design, Ch. 7 power analysis from past
  experiments) — [Laboratory Experiments in Information Retrieval, Springer 2018](https://www.springer.com/jp/book/9789811311987)
- When effort is fixed, Webber/Moffat/Zobel find "greater statistical power is achieved for the same relevance
  assessment effort by evaluating a large number of topics shallowly than a small number deeply" — [CIKM 2008](https://people.eng.unimelb.edu.au/ammoffat/abstracts/wmz08cikm.html)
- Counter-evidence that small sets can rank *systems*: Roitero et al. found a 22-topic subset could match the full
  50-topic set for ranking runs, but warned this may not hold against a larger ground truth, since Sakai argues
  ground-truth sets should be ~200 topics or more — [Cheap IR Evaluation](https://arxiv.org/abs/2011.00479)
- Topic-count requirements are task-dependent: Urbano et al. found the number needed for a reliable evaluation
  varies across tasks — [Cheap IR Evaluation](https://arxiv.org/abs/2011.00479)

### Inferences
- The Roitero "22 topics is enough" result does not transfer to this situation. It is about *ranking many systems
  relative to each other*, where errors average out across the system pool. It says nothing about estimating one
  system's absolute Hit@4, or about detecting a 3-question change in one system over time.
- The actionable translation of topic-set-size design: you cannot run it without a pilot variance estimate, but
  you already have one — the per-question results from past episodes are a topic-by-run matrix. Computing
  within-system variance of Hit@4 across your own episodes and feeding Sakai's tool would give a defensible
  statement of the form "to detect a difference of 0.05 in Hit@4 at 80% power I would need N questions". Publishing
  that N next to the current n=47 is the single most honest sentence available.
- "Judge more topics shallowly" maps directly onto this project: **adding questions is worth more than deepening
  the qrels on existing questions.** A set growing by ~15 questions per episode reaches the 150-topic
  power-analysis threshold in about seven episodes; freezing at 90 never reaches it.
- 47 answerable questions is adequate for *characterising* the system (an estimate with a +/-10-20pp interval) and
  inadequate for *comparing* versions of it (needs 6+ flips). These are different jobs and should be reported
  differently.

### Gaps
- I could not retrieve the full text of Voorhees & Buckley 2002 or Sakai 2016, so the specific error-rate tables
  (e.g. error rate at a given MAP delta and topic count) are quoted second-hand from the Roitero thesis and search
  summaries, not verified against the originals. The 0.06-0.08 AP figure in particular should be checked against
  the primary paper before publication.
- No source gives a recommended minimum for *per-stratum* question counts; the literature treats topics as
  exchangeable.

## Overfitting to a test collection: how many looks before the set is burnt

### Takeaway
Worst-case theory is **vacuous at these sizes** — to keep adaptive bias below one question on n=24 you are allowed
about **one** look. Empirically, large reused test sets have held up better than theory feared, but the protective
factors there (n=10,000, no per-item feedback, no single actor) are all absent here: a solo developer reading
per-question results has the full gradient, which is the maximally dangerous configuration.

### Cited Findings
- Classic holdout validation "assumes the procedure is fixed before the data are examined", whereas "data analysis
  is an intrinsically adaptive process"; reusing a holdout adaptively "can easily lead to overfitting to the
  holdout set itself" — [The Reusable Holdout: Preserving Validity in Adaptive Data Analysis (Dwork, Feldman, Hardt, Pitassi, Reingold, Roth; Science 2015)](https://www.research.google/blog/the-reusable-holdout-preserving-validity-in-adaptive-data-analysis/); [technical companion](https://arxiv.org/abs/1506.02629)
- The reusable-holdout mechanism validates many adaptively chosen hypotheses while provably avoiding overfitting,
  using differential privacy as the tool — [Generalization in Adaptive Data Analysis and Holdout Reuse](https://ar5iv.labs.arxiv.org/html/1506.02629)
- Blum & Hardt's Ladder, quantitatively — **non-adaptive** case: Hoeffding + union bound gives
  `Pr{exists t: |R_S(f_t) - R_D(f_t)| > eps} <= 2k exp(-2 eps^2 n)`, i.e. error scales as **sqrt(log k / n)**.
  **Adaptive** case: the naive leaderboard's error "scales with the number of submissions as **sqrt(k)**".
  Boosting attack (Cor. 5.2): with rounding alpha <= 1/sqrt(n), leaderboard error is at least **Omega(sqrt(k/n))**
  with probability 2/3. The Ladder's worst-case leaderboard error is **O(log^(1/3)(kn) / n^(1/3))**, an exponential
  improvement in k; the information-theoretic lower bound **for any estimator** is **Omega(sqrt(log k / n))** —
  [The Ladder: A Reliable Leaderboard for Machine Learning Competitions, ICML 2015](https://proceedings.mlr.press/v37/blum15.html); [arXiv:1502.04585](https://arxiv.org/abs/1502.04585)
- Ladder experiments: with n=4000 public labels and rounding 1e-5, the boosting attack produces a strongly biased
  loss estimate for a final submission whose true loss is about 1/2; the parameter-free Ladder moves the estimate
  only by about O(sqrt(log k / n)), attributed to there being at most O(log k) score-lowering steps. On real Kaggle
  data (Photo Quality, 1,785 submissions) the Ladder leaderboard was very close to Kaggle's, with only small rank
  swaps in the top 10 — [arXiv:1502.04585](https://arxiv.org/abs/1502.04585)
- The countervailing empirical result: building fresh test sets by replicating the original collection process
  dropped accuracy by **3-15% on CIFAR-10 and 11-14% on ImageNet**, but the authors attribute this to a
  distribution gap rather than adaptivity — "it is possible to recover the original ImageNet accuracies almost
  exactly if we only include the easiest images from our candidate pool" — and crucially, **ranking was preserved
  and gains on the original set translated to larger gains on the new set** — [Do ImageNet Classifiers Generalize to ImageNet? ICML 2019](https://proceedings.mlr.press/v97/recht19a)
- IR's own leaderboard-overfitting response: MS MARCO's organisers found via bootstrapping that "the leaderboards
  are quite stable", but noted "a stable leaderboard can still have overfitting through multiple submission", so
  they **limited submissions per group and monitored a private leaderboard**, concluding "the gold standard is to
  submit to evaluation efforts such as TREC" — [MS MARCO: Benchmarking Ranking Models in the Large-Data Regime, SIGIR 2021](https://arxiv.org/pdf/2105.04021)
- BEIR's own framing of why it exists: "existing benchmarks quickly saturate as they are prone to overfitting
  affecting retrieval model generalization", and BEIR challenges the assumption that in-domain performance
  predicts out-of-domain performance (BM25 remains a strong zero-shot baseline) — [Waterloo seminar abstract](https://cs.uwaterloo.ca/node/4182); [BEIR overview](https://alphaxiv.org/overview/2104.08663v4)
- Label staleness as a distinct failure: "Shallow Pooling for Sparse Labels" investigates whether modern neural
  retrieval models retrieve documents *more relevant than the original top relevance annotations*, which bears on
  whether MS MARCO is still informative — [Arabzadeh et al. summary](https://it.player.fm/1BUuSAH) (primary paper not retrieved)
- Averages hide domain-specific failures, and because different toolkits average over different dataset subsets,
  a mean nDCG@10 can be misleading across papers — [BEIR practitioner guide](https://zeroentropy.dev/concepts/beir-benchmark/)
- Practitioner consensus on small RAG golden sets: "if you fix issues found on a set and then re-evaluate on that
  same set, you are no longer really evaluating"; keep a hidden holdout, rotate in fresh cases monthly, and report
  confidence intervals when the set is small — [Overfitting in RAG Evaluation, Towards Data Science](https://towardsdatascience.com/water-cooler-small-talk-ep-11-overfitting-in-rag-evaluation/); [10 Common Pitfalls in RAG Evaluation](https://vectorize.io/blog/10-common-pitfalls-in-rag-evaluation-and-how-to-avoid-them)
- Two leakage modes specific to RAG golden sets: **chunk leakage** (labels match an old chunking scheme after the
  chunk size changed) and **query leakage** (test questions mirror document headings too closely) — [10 Common Pitfalls in RAG Evaluation](https://vectorize.io/blog/10-common-pitfalls-in-rag-evaluation-and-how-to-avoid-them)

### Computed numbers: the burn rate

`sqrt(log k / n)` — the non-adaptive union bound, and also the information-theoretic lower bound Omega(sqrt(log k / n))
that applies to *any* estimator including a protected one. Expressed as a fraction of the set, then as questions:

| n | k=2 | k=5 | k=10 | k=20 | k=50 | k=100 |
|---|---|---|---|---|---|---|
| 13 | 0.231 | 0.352 | 0.421 | 0.480 | 0.549 | 0.595 |
| 24 | 0.170 | 0.259 | 0.310 | 0.353 | 0.404 | 0.438 |
| 47 | 0.121 | 0.185 | 0.221 | 0.252 | 0.289 | 0.313 |
| 53 | 0.114 | 0.174 | 0.208 | 0.238 | 0.272 | 0.295 |
| 90 | 0.088 | 0.134 | 0.160 | 0.182 | 0.208 | 0.226 |

In questions:

| n | k=2 | k=5 | k=10 | k=20 | k=50 | k=100 |
|---|---|---|---|---|---|---|
| 13 | 3.0 | 4.6 | 5.5 | 6.2 | 7.1 | 7.7 |
| 24 | 4.1 | 6.2 | 7.4 | 8.5 | 9.7 | 10.5 |
| 47 | 5.7 | 8.7 | 10.4 | 11.9 | 13.6 | 14.7 |
| 53 | 6.1 | 9.2 | 11.0 | 12.6 | 14.4 | 15.6 |
| 90 | 7.9 | 12.0 | 14.4 | 16.4 | 18.8 | 20.4 |

`sqrt(k/n)` — the unprotected-adaptive / boosting-attack scale:

| n | k=5 | k=10 | k=20 | k=50 |
|---|---|---|---|---|
| 24 | 0.46 | 0.65 | 0.91 | 1.44 |
| 47 | 0.33 | 0.46 | 0.65 | 1.03 |
| 90 | 0.24 | 0.33 | 0.47 | 0.75 |

**How many looks are "free"?** Solving `sqrt(log k / n) <= 1/n` (bias under one question) gives **k <= 1.04 at
n=24** and **k <= 1.02 at n=47**. Allowing two questions of bias at n=24 still gives k <= 1.18.

### Inferences
- **The worst-case bound is uninformative at these sizes for any k >= 2.** This is the blunt finding: there is no
  theoretical defence of a 24-question or 47-question set under repeated adaptive use. The only defence available
  is procedural — the set's credibility rests entirely on the discipline of the person using it, which is why the
  no-tuning rule, the pre-registration, and the once-only held-out set are not stylistic preferences but the whole
  of the protection.
- But the bound is *worst-case against an adversary*. Recht et al. is the strongest evidence that honest,
  mechanism-driven development does not burn a test set anything like as fast as the bound implies: years of
  ImageNet reuse left ranking intact. The operative question is therefore not "how many looks" but **"are my
  changes adversarial in form?"** A change derived from the data schema is not an attack. A change derived from
  inspecting which three questions failed is precisely a low-order boosting step, and three such steps across an
  episode is a small-scale version of the attack the Ladder paper demonstrates.
- The project's own structure already implements the Ladder's core idea without naming it: scoring the 13 held-out
  questions exactly once per episode is a hard submission limit, which is the same remedy MS MARCO adopted
  (limiting submissions per group, private leaderboard). That is the right instinct and should be stated as such.
- Practical burn budget for a solo developer, derived from the above rather than cited: **the main set tolerates
  roughly one confirmatory look per episode plus unlimited looks at a separate development set.** If the main set
  is read more than a handful of times per episode, its headline number should be reported as development
  telemetry, not as a result.
- Chunk leakage is a live risk for this specific project: the qrels name expected *passages*, so any change to
  chunking, section seaming or the collection split silently changes what "all-evidence@4" means. A frozen golden
  set is only frozen if the passage identifiers it references are stable; otherwise the set must be re-judged and
  the comparison across episodes is broken. This deserves an explicit check each episode.
- The Arabzadeh shallow-pooling result is the direct threat to the all-evidence metric: a system that retrieves a
  *better* passage than the one in the qrels is scored as a failure, and a fix that forces the labelled passage
  into the top 4 is then measured as an improvement while being a regression in reader quality.

### Gaps
- I found no study measuring adaptivity-driven overfitting at n < 100 empirically; both the theory (Ladder,
  reusable holdout) and the empirical work (ImageNet, CIFAR, MS MARCO) operate at n in the thousands to tens of
  thousands. The "one free look" figure above is the theory evaluated at n=24/47, not an empirical finding, and
  should be presented as such.
- The reusable-holdout mechanism is not practically usable here: its noise addition is calibrated to large n and
  would swamp a 13-question signal entirely. I found no small-n variant.

## Discipline that preserves honesty

### Takeaway
On a set this small, the only things that carry evidential weight are **pre-registration, one-change-at-a-time
ablations, derivation of every rule from the schema rather than the eval answers, leave-one-out for any threshold,
reporting floors rather than point estimates, and publishing per-question results.** The "ratchet" is a useful
guardrail but it is not evidence, and it has a specific statistical pathology worth naming.

### Cited Findings
- The reusable-holdout framing is explicitly that validity is preserved by *constraining how the analyst
  interacts with the data*, not by a better estimator — [Reusable Holdout](https://www.research.google/blog/the-reusable-holdout-preserving-validity-in-adaptive-data-analysis/)
- MS MARCO's organisers' chosen controls were submission limits plus a private leaderboard, and their
  recommendation was external evaluation — [MS MARCO SIGIR 2021](https://arxiv.org/pdf/2105.04021)
- Fuhr lists "proof by experimentation" and reproducibility failures among common IR evaluation mistakes,
  alongside ignoring effect sizes — [SIGIR Forum 2017](https://unpaywall.org/10.1145%2F3190580.3190586)
- Unadjusted multiple testing "drastically increases the Type I error rate"; among adjusted procedures only
  randomised Tukey HSD controlled error across topic-set sizes, with Wilcoxon + Benjamini-Hochberg recommended as
  the practical compromise — [Towards Reliable Testing for Multiple IR System Comparisons](https://arxiv.org/pdf/2501.03930)
- Carterette's position is that in a TREC-like setting "it is not clear how to properly correct values ... or
  whether it should be done at all" — reported in [Cheap IR Evaluation](https://arxiv.org/abs/2011.00479), original: [Multiple testing in statistical analysis of systems-based IR experiments, TOIS 30(1)](https://ir.webis.de/anthology/2012.tois_journal-ir0anthology0volumeA30A1.3)
- Keep a hidden holdout; rotate fresh cases in from new tickets monthly; report confidence intervals when the set
  is small — [RAG Quality Guide](https://apptension.com/guides/rag-quality-guide-evaluation-that-holds-up)
- Golden sets should include multi-hop questions, no-answer cases, conflicting or stale documents and adversarial
  phrasing; stale documents are "the most skipped category" — [Overfitting in RAG Evaluation](https://towardsdatascience.com/water-cooler-small-talk-ep-11-overfitting-in-rag-evaluation/)
- Test sets should cover a broad subset of the underlying data with variations in phrasing and question
  complexity — [Google Cloud, Optimizing RAG retrieval](https://cloud.google.com/blog/products/ai-machine-learning/optimizing-rag-retrieval)

### Inferences
- **The ratchet makes the headline metric a running maximum of a noisy process, so it must rise.** A rule that
  accepts only non-decreasing measurements means the reported Hit@4 is, by construction, the max over all
  candidate changes evaluated — including changes whose true effect is zero or negative but whose measured effect
  happened to be non-negative. Over k candidates the measured score drifts upward by roughly the expected maximum
  of k noise draws, which is the `sqrt(log k / n)` quantity tabulated above. The ratchet is therefore good
  engineering (it stops obvious regressions shipping) and **zero** evidence of improvement. This should be said
  out loud in the write-up, because a reader will otherwise read the monotone curve as the result.
- The ratchet's "lose no more than one question in any stratum" clause is the one part with real teeth, because it
  blocks the characteristic signature of fitting: trading strata against each other to buy headline points. Keep
  it, and report the per-stratum deltas it was checking.
- **"stale@4 must not rise" is nearly uninformative at current counts.** With a stale stratum of a handful of
  questions and a base count of 1-2, "did not rise" is the overwhelmingly likely outcome under *any* change, good
  or bad. It is a tripwire for catastrophe, not a measurement. The fix is not statistical — it is to count stale
  hits across all 90 questions rather than only the stale stratum, which turns a count of 2 into a count with
  enough base rate to move.
- **The no-tuning rule is statistically well-founded, not merely aesthetic.** Any threshold chosen by looking at
  the eval set consumes the degrees of freedom the set has, and at n=24 there are 25 distinguishable values, so
  one threshold fit on them is close to a free parameter per achievable score. Deriving thresholds from corpus
  statistics (score distributions over the whole collection, schema cardinalities, chunk lengths) costs nothing
  from the eval budget.
- **If a threshold must be fit, use nested leave-one-out and report the LOO score, never the in-sample score.**
  Outer loop: hold out question i. Inner loop: choose the threshold on the remaining 46. Score question i with
  that threshold. Repeat 47 times. The useful diagnostic is the *gap*: if LOO equals in-sample, the threshold is
  not being fit; a drop of 2+ questions means it is. LOO on n=47 is high-variance but it is the right unbiased-ish
  estimate of the *procedure*, and the gap is more informative than either number alone.
- **Pre-registration template that fits this project** (write before running, commit it, then run once):
  1. The mechanism, specified by reference to the data schema only. If you cannot write the spec without naming an
     eval question, stop — it is a fit, not a mechanism.
  2. The predicted firing set: which of the 90 questions the rule should fire on, and which it must not touch.
  3. The predicted direction and size per stratum.
  4. The pass/fail criterion, including the regression tolerance.
  5. The number of evaluation runs this change is allowed.
  Then the write-up reports prediction vs outcome, which is a far stronger artefact than the outcome alone — and
  a wrong prediction that you publish is more credible than a right one you did not commit to.
- **Ablation hygiene**: one change per evaluation, and a full re-run of the frozen set per change, never a
  differential re-score of only the questions you expected to move. Re-scoring only the expected movers makes the
  unexpected regressions invisible, which is the exact blind spot that makes the ratchet feel safe.
- On multiple comparisons: given Carterette's scepticism about correction in TREC-like settings and the 2025
  finding that no correction is worse, the pragmatic choice here is to **declare one primary endpoint per episode**
  (e.g. main-set Hit@4) and treat everything else — all 12 strata, MRR, all-evidence@4, stale@4 — as explicitly
  descriptive, with no p-values attached. One pre-declared endpoint needs no correction.

### Gaps
- I found no pre-registration convention established within IR for incremental system development; the
  pre-registration literature is from psychology and clinical trials, and I did not locate an IR-specific
  template to cite.

## Per-stratum reporting with 1-7 items per stratum

### Takeaway
A stratum of 1-7 questions supports **no score at all** — only a count. The minimum detectable change within a
stratum of 3 is one question, i.e. 33 percentage points, and under Bonferroni correction across 12 strata **no
per-stratum result can reach family-wise significance at any attainable effect size**, because the required 9
discordant pairs exceed the largest stratum.

### Cited Findings
- Averages hide domain-specific failures, and mean scores computed over different subsets are not comparable —
  [BEIR practitioner guide](https://zeroentropy.dev/concepts/beir-benchmark/)
- Tracking coverage by topic is recommended precisely because averages hide entire product areas that
  underperform — [RAG evaluation guide](https://dataaspirant.com/blog/rag-evaluation/)
- Fuhr lists ignoring multiple comparisons as a common mistake — [SIGIR Forum 2017](https://unpaywall.org/10.1145%2F3190580.3190586)
- Unadjusted multiple testing drastically inflates Type I error — [Towards Reliable Testing](https://arxiv.org/pdf/2501.03930)
- Topic-count requirements vary by task (Urbano et al.), which is the per-stratum analogue: different strata would
  need different numbers of questions for equal reliability — [Cheap IR Evaluation](https://arxiv.org/abs/2011.00479)

### Computed numbers
- 12 strata, Bonferroni: alpha = 0.05/12 = **0.00417**. A sign test needs `2*0.5^b <= 0.00417`, i.e. **b >= 9
  discordant questions**. Maximum stratum size is 7. Therefore **no per-stratum claim can be significant**, even
  if every question in the stratum flips.
- Benjamini-Hochberg is more forgiving but cannot rescue this either: the smallest attainable two-sided p in a
  7-question stratum is 0.0156 (all 7 flip), and in a 3-question stratum 0.25.
- Granularity per stratum: 1 question = 100pp in a stratum of 1; 50pp in a stratum of 2; 33pp in 3; 25pp in 4;
  20pp in 5; 17pp in 6; 14pp in 7.

### Inferences
- **Report strata as `k/n` counts, never as percentages.** "2/3" is honest; "67%" implies a precision that does not
  exist and invites comparison with a 47-question 67%. This is the single highest-value reporting change available.
- **Report strata as a matrix, not a scoreboard.** The right artefact is a table with one row per question
  (question id, stratum, Hit@4 before, Hit@4 after, rank of first expected passage before/after, which mechanism
  fired). A reader can then do their own analysis, which is the only way a 90-question result can be audited.
  Aggregating to 12 stratum percentages destroys exactly the information needed to judge it.
- **Strata are a design device, not a measurement device.** Their job is coverage — guaranteeing that the set
  exercises identifier lookup, staleness, abstention, as-of reasoning and so on, so that a change cannot improve
  the headline while silently breaking a behaviour. They are a checklist against blind spots. Framing them as
  per-category scores over-reads them by a wide margin.
- The honest per-stratum sentence is of the form: "stale: 3/3 before, 3/3 after (stratum n=3; a single question is
  33 percentage points, so no trend can be read from this)". Putting the granularity caveat inline, once, next to
  the table is better than a footnote.
- A stratum that drops from 3/3 to 2/3 and a headline that rises by 4 questions is the canonical fitting
  signature, and the ratchet's per-stratum clause is designed to catch exactly this. Report every stratum delta
  the ratchet evaluated, including the zeroes — showing the checks that passed is what makes the ones that failed
  credible.

### Gaps
- No source I found gives explicit guidance on reporting IR results for strata of n<10; the IR literature's
  per-category analyses (e.g. BEIR per-dataset) operate at dataset sizes in the thousands. The recommendations
  above are derived from the arithmetic plus the general multiple-comparisons literature.

## Distinguishing a generalising mechanism from a fit

### Takeaway
There is no statistical test that can do this on 90 questions — significance testing cannot distinguish "fixed by
a general rule" from "fixed by a lookup table" because both produce the same flips. The discriminating evidence is
all **structural**: derivability of the rule from the schema, the rule's firing precision measured across
questions it should *not* help, firing behaviour on unlabelled queries, and confirmation on a never-seen set.

### Cited Findings
- The Ladder's boosting attack is the formal demonstration that per-item feedback lets an actor raise a measured
  score with a model of true loss ~1/2, i.e. measured gains can be entirely mechanism-free — [arXiv:1502.04585](https://arxiv.org/abs/1502.04585)
- BEIR's central empirical result is that **in-domain performance does not predict out-of-domain performance**, and
  that a well-understood mechanism (BM25) generalises better zero-shot than models tuned in-domain — [BEIR overview](https://alphaxiv.org/overview/2104.08663v4); [Waterloo abstract](https://cs.uwaterloo.ca/node/4182)
- Recht et al. provide the positive criterion: genuine progress *transfers and amplifies* — "accuracy gains on the
  original test sets translate to larger gains on the new test sets" — whereas pure adaptivity would predict gains
  that vanish on a fresh set — [ICML 2019](https://proceedings.mlr.press/v97/recht19a)
- Reproducibility-by-component is the established IR framing: PRIMAD decomposes an experiment into **Platform,
  Research objective, Implementation, Method, Actor, Data**, and Ferro et al. used it to measure reproducibility by
  re-running experiments and checking how changing individual components changed outcomes — [PRIMAD-LID](https://arxiv.org/abs/2601.02349); [ir_metadata](https://arxiv.org/abs/2207.08922v1)
- Neural models that win on average can be "considerably less effective than BM25" on a particular subset (Touché
  2020), i.e. an average gain can conceal a mechanism that does not generalise to a question type — [BEIR practitioner guide](https://zeroentropy.dev/concepts/beir-benchmark/)
- Query leakage — test questions mirroring document headings too closely — is a named RAG-specific failure that
  makes a retrieval mechanism look general when it is exploiting the set's construction — [10 Common Pitfalls](https://vectorize.io/blog/10-common-pitfalls-in-rag-evaluation-and-how-to-avoid-them)

### Inferences
These are my synthesis; I found no published checklist for this exact question, so present them as a proposed
discipline rather than an established standard.

- **The derivability test.** Write the rule's specification referring only to the corpus and schema — field names,
  identifier formats, section structure, date fields, collection boundaries. If the spec cannot be written without
  naming an eval question or its answer, it is a fit. For the three pure-ranking questions this is the decisive
  test: a scoring change justified as "identifier-shaped queries should weight exact-match on the identifier
  field" is derivable; "boost documents in collection C when the query contains these tokens" is a lookup table
  wearing a function's clothes.
- **The firing-precision report.** For every mechanism, publish four numbers measured over all 90 questions:
  fired-and-helped, fired-and-harmed, fired-and-no-op, did-not-fire. A mechanism that fires on exactly the three
  broken questions and nothing else has firing precision 3/3 and is almost certainly a lookup table — *high
  precision on the eval set is the suspicious outcome, not the reassuring one*. A mechanism that fires on 20
  questions, helps 3, harms 0 and no-ops 17 is a plausible general rule. This inverts the usual intuition and is
  the most useful single diagnostic available at this sample size.
- **The off-set firing test.** Generate 50-200 queries the mechanism should plausibly apply to, without labelling
  them (restated questions, questions from corpus headings, synthetic identifier queries). Measure only the
  *firing rate* — no relevance judgement needed. If a rule meant to catch "as-of style temporal queries" fires on
  2% of such queries, the rule is keyed to the eval phrasings. This is cheap, requires no qrels, and directly
  tests generality.
- **The never-seen-set requirement.** A mechanism claim is confirmed only when it moves a set scored for the first
  time. The 13 held-out questions do this but cannot measure anything (see below). The stronger move is to write
  new questions *for the mechanism's predicted firing set* after the mechanism is specified and before it is run —
  if the rule is general, those questions are easy to write and it should pass them.
- **The negative-control requirement.** Include questions the mechanism must *not* change. If a change moves
  questions outside its predicted firing set, the mechanism is not what you think it is, regardless of whether the
  movement was favourable.
- **Mechanism precision alongside benefit.** For a router or classifier, report its measured precision/recall as a
  classifier (how often does it route correctly?) separately from the end-to-end retrieval gain. A router with 60%
  routing precision that nonetheless gains 3 questions is riding luck; one with 95% routing precision that gains 3
  questions is a working component whose benefit on this corpus happens to be small. Those are completely different
  engineering situations and the retrieval metric alone cannot tell them apart.
- **On the 13 held-out questions, honestly:** one question is 7.7pp; detecting a 2-question regression is p=0.50
  by sign test; 13/13 establishes only >=79.4%. The held-out set is a **tripwire against catastrophe and a test of
  procedural honesty, not a measurement**. Its legitimate uses: confirming no gross regression, and demonstrating
  publicly that you scored it once and reported the number whatever it was. Its illegitimate use: quoting its
  percentage as a generalisation estimate.

### Gaps
- I found no established test or criterion in the literature for "is this a mechanism or a fit" at small n. The
  closest formal analogue is the adaptive-data-analysis framing (restrict interaction) and the BEIR framing
  (require out-of-domain confirmation), neither of which is a per-change test. The checklist above is constructed,
  not cited.

## Metric choice: the @4 cutoff, and the properties of an "all-evidence@4" metric

### Takeaway
Choosing the cutoff from the downstream reader context is the **principled** way to choose it, and is better
justified than the field's own convention: the literature's criticism is precisely that depths like k=20 or k=100
are chosen "without necessarily fully considering the ramifications". But all-evidence@4 is a strict-recall metric
with four known pathologies — cardinality confounding, non-monotonicity, permanent zeros, and maximum coarseness —
and recall-based metrics are the family most sensitive to depth choice.

### Cited Findings
- "In both families, the pragmatics of forming judgments mean that it is usual to evaluate the metric to some
  chosen depth such as k=20 or k=100, without necessarily fully considering the ramifications associated with that
  choice" — [Lu, Moffat & Culpepper, The effect of pooling and evaluation depth on IR metrics](https://people.eng.unimelb.edu.au/ammoffat/abstracts/lmc16irj.pdf)
- Recall-based metrics (AP, nDCG) "are more susceptible to such issues than are utility-based ones such as RBP and
  ERR"; the authors recommend pairing recall-based results with utility-based measurements and caution against
  recall-based metrics alone in large-scale testing. They also find that limiting evaluation depth to the pooling
  depth "does not necessarily result in more consistent outcomes" — [same](https://people.eng.unimelb.edu.au/ammoffat/abstracts/lmc16irj.pdf)
- nDCG's discriminative power relative to other measures has been studied specifically — [Webber, Moffat & Zobel, EVIA 2010](https://research.nii.ac.jp/ntcir/workshop/OnlineProceedings8/EVIA/03-EVIA2010-WebberW.pdf)
- A 2025 benchmark justifies nDCG@10 as "a standard, comparable measure that reflects first-page satisfaction" and
  pairs it with Recall@100/@1000 "because they demonstrate the upper bound of these models to retrieve relevant
  information" — i.e. the convention is to report a shallow utility-oriented cutoff *and* a deep recall ceiling —
  [Benchmarking Information Retrieval Models on Complex Retrieval Tasks](https://arxiv.org/pdf/2509.07253)
- Fuhr argues averaging reciprocal rank is not a legitimate interval-scale operation; Sakai disputes this and
  considers averaging RR legitimate. The disagreement is live and both positions are published —
  [Fuhr](https://unpaywall.org/10.1145%2F3190580.3190586); [Sakai](https://unpaywall.org/10.1145%2F3451964.3451976); [context](https://arxiv.org/pdf/2212.11735)
- Sparse/shallow labels mean modern rankers may retrieve documents *more relevant than the labelled ones*, with
  "important implications" for whether such a benchmark is informative — [Arabzadeh et al., Shallow Pooling for Sparse Labels](https://it.player.fm/1BUuSAH)
- Required topic counts "vary vastly" between evaluation measures because their within-system variances differ, so
  the choice of measure directly determines how many questions you need — [Sakai, Topic set size design](https://link.springer.com/article/10.1007/s10791-015-9273-z)

### Inferences
- **@4 is defensible, and should be defended explicitly and in one sentence:** the reader receives 4 passages, so
  Hit@4 is the probability the reader can answer, and @5 or @10 would measure a system nobody runs. This is a
  stronger justification than the field's own habit, and the Lu/Moffat/Culpepper critique is the citation that
  makes the point: depth should follow the task, not convention. Worth stating that the cutoff was fixed by the
  context budget *before* measuring, since a cutoff chosen after seeing which k flatters the system is a fit of
  the most basic kind.
- **A shallow cutoff is coarse.** Hit@4 is a step function: substantial score improvements are invisible until
  something crosses rank 4, and substantial degradations are invisible until something crosses back. For the three
  pure-ranking questions this is the whole problem — and the diagnostic fix is to report the **rank of the first
  expected passage** and the **maximum rank over expected passages**, per question, as continuous companions.
  These move when the system genuinely improves even when @4 does not, they cannot be gamed by a threshold, and
  they turn "3 questions failed" into "3 questions whose evidence sits at ranks 5, 6 and 11" — which is a far more
  informative statement and distinguishes a near miss from a retrieval failure.
- **all-evidence@4 is strict recall@4 binarised at 1.0.** Its specific pathologies:
  1. *Cardinality confounding.* A question with one expected passage is satisfied by a single hit in four slots;
     a question with three expected passages must win three of four slots. The metric mixes question difficulty
     with evidence cardinality, so the mean across questions is not interpretable without the cardinality
     distribution. **Publish the histogram of expected-passage counts next to the metric.**
  2. *Permanent zeros.* Any question with more than 4 expected passages scores 0 forever and is pure dead weight
     dragging the mean down; such questions should be identified and either re-judged or excluded with the
     exclusion stated.
  3. *Zero-sum at small k.* With only four slots, adding a correct passage can evict another expected passage, so
     the metric is not monotone in retrieval quality. A change can improve every question's graded recall@4 and
     reduce all-evidence@4.
  4. *Maximum coarseness.* It is 0/1 per question, so it has strictly lower power than Hit@4 and far lower than a
     graded measure. All the significance figures above are therefore *optimistic* for all-evidence@4.
  5. *Label-completeness sensitivity.* Per Arabzadeh et al., a system that finds a better passage than the qrels
     name is scored as a failure, and forcing the labelled passage into the top 4 is measured as a gain while
     being a regression for the reader.
  **Mitigation:** report graded recall@4 (fraction of expected passages retrieved) as the primary continuous
  measure and all-evidence@4 as the strict pass/fail summary. Graded recall@4 shows partial progress, is monotone,
  and is not confounded by cardinality in the same way.
- Given Fuhr/Sakai, MRR is reportable but should not be the primary endpoint and its mean should not carry a claim
  — report the full distribution or median rank alongside, which sidesteps the interval-scale objection entirely.
- Following Lu/Moffat/Culpepper's pairing advice, the metric set that would be hard to criticise here is:
  Hit@4 (task-aligned, primary), graded recall@4 (continuous, diagnostic), rank-of-first-expected and
  max-rank-of-expected (continuous, ungameable), all-evidence@4 (strict summary), stale@4 counted over all 90
  questions (safety), plus an RBP-style utility measure if one is cheap to add.

### Gaps
- I found no paper that studies the "all relevant in top-k" (strict recall) metric as such; it appears in practice
  (multi-hop and RAG evaluation) but I located no analysis of its statistical properties. The five pathologies
  above are derived from the metric's definition and from the recall-vs-utility findings, not cited directly.
- I did not find literature specifically on cutoffs as shallow as k=4; the published depth work concerns k=10 and
  deeper.

## What to report so a reader can judge for themselves

### Takeaway
IR has reproducibility vocabulary and metadata standards worth adopting (ACM badging definitions, PRIMAD,
ir_metadata), and the project already does the hard part by pinning seed, top_p, top_k and keep_alive. The missing
pieces for a public blog series are the per-question result table, the evaluation-run count, and the floor-not-
point-estimate framing.

### Cited Findings
- ACM Artifact Review and Badging definitions, as adopted by ECIR: **replicability** = different team, same
  experimental setup; **reproducibility** = different team, different setup; **repeatability** = same team, same
  setup (ECIR does not accept repeatability papers from the same authors). ACM's definitions "still leave room for
  interpretation when applied to IR experiments" — [ECIR 2024 reproducibility track](https://www.ecir2024.org/?p=831); [ir_metadata](https://ar5iv.labs.arxiv.org/html/2207.08922)
- PRIMAD's six factors to record: Platform, Research objective, Implementation, Method, Actor, Data —
  [PRIMAD-LID, Jan 2026](https://arxiv.org/abs/2601.02349)
- ir_metadata is an extensible metadata schema for TREC run files built on PRIMAD, intended to record the factors
  that affect reproducibility — [arXiv:2207.08922](https://arxiv.org/abs/2207.08922v1)
- The Dagstuhl 23031 seminar report covers the state of information-access experimentation for research and
  education, including reproducibility infrastructure — [arXiv:2305.01509](https://arxiv.org/pdf/2305.01509)
- Fuhr names reproducibility problems and "proof by experimentation" as common mistakes —
  [SIGIR Forum 2017](https://unpaywall.org/10.1145%2F3190580.3190586)
- Mean scores computed over differing subsets are not comparable across sources — [BEIR practitioner guide](https://zeroentropy.dev/concepts/beir-benchmark/)
- Report confidence intervals when the evaluation set is small — [RAG Quality Guide](https://apptension.com/guides/rag-quality-guide-evaluation-that-holds-up)

### Inferences: the reporting checklist for this project
- **The set.** Publish the frozen questions, the qrels (expected passage ids), the stratum labels, the freeze date
  and commit, and the 6 unanswerable questions' expected behaviour. State whether passage ids changed since the
  freeze (chunk-leakage check).
- **The per-question table.** One row per question per episode: id, stratum, Hit@4, all-evidence@4, graded
  recall@4, rank of first expected passage, max rank of expected, which mechanisms fired. At n=90 this table *is*
  the result; the aggregates are a summary of it. This is the single artefact that lets a reader reach their own
  conclusion, and it is cheap to publish.
- **The floors.** Every headline proportion as `k/n` plus a 95% interval, and perfect scores as a one-sided lower
  bound: "47/47 (95% one-sided lower bound 93.8%)".
- **The look count.** How many times the main set, the time set and the held-out set were each scored this
  episode, and whether development used a separate set. This is the number that lets a reader discount the result
  appropriately, and it is the number nobody publishes.
- **The pre-registration, as committed.** The hypothesis, the predicted firing set, the predicted stratum effects,
  and then the outcome beside it.
- **The mechanism's own numbers.** Firing counts (helped / harmed / no-op / did-not-fire) over all 90 questions,
  the off-set firing rate, and for any classifier its measured precision and recall as a classifier.
- **The negative result space.** What was tried and rejected by the ratchet, and why. A series that reports only
  accepted changes is reporting the max of a noisy process (see the ratchet inference above); reporting the
  rejected candidates is what turns the monotone curve back into evidence.
- **PRIMAD-style environment record.** Model id and quantisation, seed, top_p, top_k, keep_alive, embedding model
  and version, chunking parameters, index type and parameters, hardware tier, library versions. The project's
  existing habit of pinning the model call maps exactly onto PRIMAD's Platform and Implementation axes and is
  worth naming as such — it is what makes a "different team, same setup" replication possible.
- **The explicit non-claims.** A short, standing paragraph: this set cannot distinguish 100% from 94%; no
  per-stratum result can be significant; a 3-question change is p=0.25; the ratchet guarantees monotonicity and is
  not evidence. Publishing the limits once, prominently, buys more credibility than any individual number.

### Gaps
- There is no IR reporting checklist aimed at single-developer incremental development on a small private set; the
  ACM/PRIMAD/ir_metadata machinery is aimed at published system comparisons with shared collections. The checklist
  above is an adaptation.

## Direct answer: is "we got to 100%" an honest claim?

### Takeaway
**As a claim about the set, yes. As a claim about the system, no.** The honest sentence is narrow, bounded, and
must be accompanied by the held-out number and the look count. "We got to 100%" unqualified is not defensible,
and the specific danger in this situation — 3 of 8 remaining questions being pure ranking losses — is the textbook
configuration for a fit.

### What is honest
- "The system now returns the expected evidence within the top 4 for **all 47 answerable main questions** and all
  24 time questions." A factual statement about 71 specific strings, verifiable from the published per-question
  table.
- "That is a 95% one-sided lower bound of **93.8%** on the main set and **88.3%** on the time set. These sets
  cannot distinguish 100% from 94%." — the required accompaniment.
- "On the 13 held-out questions, scored once, the result was X/13 (lower bound Y%)." — the only number in the
  project with any claim to generalisation, and it is a weak one.
- "N mechanisms were added. Each is specified by reference to the schema; each fired on M questions with H helped,
  0 harmed. The ratchet rejected K candidate changes."

### What is not honest
- "Our retrieval is 100% accurate" / "we solved retrieval for this corpus" / anything in the present tense about
  the system rather than the set.
- Any implied generalisation from a set the changes were developed against. BEIR's headline finding is exactly
  that in-domain performance does not predict out-of-domain performance — [BEIR](https://alphaxiv.org/overview/2104.08663v4)
- Calling a 3-question gain an improvement. p=0.25. There is no sample size at which 3 flips is significant; it
  needs 6.
- Per-stratum percentages, or any per-stratum claim at all.
- Reading the monotone ratchet curve as a progress curve.

### Conditions under which the 100% means something
All seven, not a subset:
1. The set was frozen before the work began, and the passage ids it references have not changed.
2. Every mechanism was pre-registered and its spec is derivable from the schema without reference to eval answers.
3. The number of times each set was scored is disclosed.
4. The held-out 13 was scored exactly once and did not regress.
5. Per-question results are published for every episode.
6. The number is stated as a floor with its interval, and the non-claims paragraph accompanies it.
7. At least one genuinely new set — written after the mechanisms were specified — confirms the direction.

### The specific trap in the current state
The 3 pure-ranking questions are the ones where the answer is already in the correct sub-collection and loses on
score. Fixing them by adjusting scoring weights, thresholds or boosts chosen with reference to those three
questions is a boosting step in the Ladder's sense — a change with no model behind it that raises the measured
score. The Ladder paper's demonstration is that such steps can drive a measured score arbitrarily while true loss
stays at chance ([arXiv:1502.04585](https://arxiv.org/abs/1502.04585)). Three defences, in order of strength:
1. **Derive the scoring change from the schema.** If identifier-shaped queries should weight exact field matches,
   that is a rule about the data model. Implement it as such, predict its firing set, and accept whatever it does
   to those 3 questions — including nothing.
2. **Require the fix to be wider than the fault.** A mechanism aimed at 3 questions that fires on only those 3 is
   a lookup table. Require it to fire on at least ~3x its target count with zero harm.
3. **If neither holds, leave the questions failing and report them.** An episode that ends at 44/47 with a
   published explanation of the three residual ranking losses is a stronger artefact than one that ends at 47/47
   with an unexplained scoring tweak — and far stronger than one where a reader cannot tell which happened. The
   series' credibility is the asset; three questions are not.

### The process, compressed
1. Keep the 90 frozen. Build a **separate development set** for iteration; all exploration happens there.
2. Grow the golden set every episode (target: toward 150 questions, per the power literature). More questions
   judged shallowly beats fewer judged deeply.
3. Per change: pre-register, implement from the schema, one change at a time, full re-run.
4. Score the main+time sets **once per episode** as the confirmatory run. Declare one primary endpoint.
5. Any threshold: nested leave-one-out, report the LOO score and the in-sample/LOO gap.
6. Score the held-out 13 once. Report whatever it says.
7. Report floors, counts not percentages, the per-question table, the look count, the firing precision, and the
   rejected candidates.
8. Claim mechanisms, not improvements, until you have 6 clean wins.
