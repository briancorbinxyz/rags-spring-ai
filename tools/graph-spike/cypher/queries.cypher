// Graph arm queries. Run verbatim by arm_graph.py via cypher-shell.
// Each query is delimited by a "// @name" marker line.

// @Q0_inventory
MATCH (n) WITH labels(n)[0] AS lab, count(*) AS c
RETURN lab, c ORDER BY c DESC;

// @Q0_edges
MATCH ()-[r]->() WITH type(r) AS t, count(*) AS c
RETURN t, c ORDER BY c DESC;

// @Q1_aggregate_claim
// The cardinality constraint, as a first-class node. This is the thing a
// passage retriever has nowhere to put.
MATCH (c:Company)-[:MAKES_CLAIM]->(k:Claim)
RETURN c.name AS claimant, k.n AS n, k.of_m AS of_m,
       k.customer_class AS customer_class, k.doc_id AS doc
ORDER BY claimant;

// @Q2_universe
// Who the top five are, taken from the corpus's own ranking record.
MATCH (c:Company)-[r:RANKED_IN]->(u:Universe)
RETURN u.name AS universe, r.rank AS rank, c.name AS company, c.kind AS kind
ORDER BY universe, rank;

// @Q3_explicit_supply
// Route A: a document directly asserts the supply relation.
MATCH (s:Company {name:'Kestrel Memory Works'})-[r:SUPPLIES]->(g:Company)
WHERE g.kind = 'GPUProducer'
RETURN g.name AS producer, count(DISTINCT r.doc_id) AS docs,
       collect(DISTINCT r.doc_id) AS evidence
ORDER BY producer;

// @Q4_part_chain
// Route B: the producer uses or recruits for a part the supplier makes.
// Two hops, never jointly stated in any single document.
MATCH (s:Company {name:'Kestrel Memory Works'})-[m:MAKES_PART]->(p:Part)
      <-[u:USES_PART]-(g:Company)
WHERE g.kind = 'GPUProducer'
RETURN g.name AS producer, p.name AS part,
       collect(DISTINCT u.doc_id) AS producer_side_docs,
       collect(DISTINCT m.doc_id) AS supplier_side_docs
ORDER BY producer;

// @Q5_qualification_chain
// Route C: the supplier's part qualified on a platform the producer makes, where
// the qualification document did NOT name the customer. Three hops. Accepts the
// part-level shape and the company-level shape, because the extractor produces
// both.
MATCH (s:Company {name:'Kestrel Memory Works'})-[:MAKES_PART]->(p:Part)
      -[q:QUALIFIED_ON]->(pl:Platform)<-[mp:MAKES_PLATFORM]-(g:Company)
WHERE g.kind = 'GPUProducer'
RETURN 'part-level' AS shape, g.name AS producer, pl.name AS platform,
       q.doc_id AS qual_doc, q.customer_named AS customer_was_named,
       mp.doc_id AS platform_doc
UNION
MATCH (s:Company {name:'Kestrel Memory Works'})-[q:QUALIFIED_ON]->(pl:Platform)
      <-[mp:MAKES_PLATFORM]-(g:Company)
WHERE g.kind = 'GPUProducer'
RETURN 'company-level' AS shape, g.name AS producer, pl.name AS platform,
       q.doc_id AS qual_doc, q.customer_named AS customer_was_named,
       mp.doc_id AS platform_doc;

// @Q6_weak_signals
// Route D: a named commercial programme, or a joint technical presentation.
MATCH (s:Company {name:'Kestrel Memory Works'})-[r:PROGRAMME_REF|CO_PRESENTS]-(g:Company)
WHERE g.kind = 'GPUProducer'
RETURN g.name AS producer, type(r) AS signal, r.doc_id AS doc, r.quote AS quote
ORDER BY producer, signal;

// @Q7_decoy_exposure
// The decoys, named explicitly: co-mentions that assert NO supply relation.
// A ranking that counts these will put Veradyne near the top. The graph can
// keep them and score them zero, because the edge TYPE carries the semantics.
MATCH (s:Company {name:'Kestrel Memory Works'})
      -[r:LITIGATES_AGAINST|CO_MEMBER|PARTNERS_NON_SUPPLY]-(o:Company)
RETURN o.name AS other, o.kind AS other_kind, type(r) AS signal,
       count(DISTINCT r.doc_id) AS docs, collect(DISTINCT r.doc_id) AS evidence
ORDER BY docs DESC, other;

// @Q8_absence_ranked
// THE QUERY. For every top-five GPU producer, total the supply evidence across
// all routes, count the non-supply co-mentions separately, and rank ascending
// so the producer with no supply evidence surfaces first.
MATCH (g:Company)-[rk:RANKED_IN]->(u:Universe)
WHERE u.name CONTAINS 'gpu' AND rk.rank <= 5 AND g.kind = 'GPUProducer'
WITH g
OPTIONAL MATCH (s:Company {name:'Kestrel Memory Works'})-[a:SUPPLIES]->(g)
WITH g, count(DISTINCT a.doc_id) * 1.0 AS w_explicit
OPTIONAL MATCH (s:Company {name:'Kestrel Memory Works'})-[:MAKES_PART]->(p:Part)
               <-[u:USES_PART]-(g)
WITH g, w_explicit, count(DISTINCT u.doc_id) * 0.6 AS w_part
OPTIONAL MATCH (s:Company {name:'Kestrel Memory Works'})-[:MAKES_PART*0..1]->()
               -[q:QUALIFIED_ON]->(pl:Platform)<-[:MAKES_PLATFORM]-(g)
WITH g, w_explicit, w_part, count(DISTINCT q.doc_id) * 0.5 AS w_qual
OPTIONAL MATCH (s:Company {name:'Kestrel Memory Works'})-[pr:PROGRAMME_REF]-(g)
WITH g, w_explicit, w_part, w_qual, count(DISTINCT pr.doc_id) * 0.4 AS w_prog
OPTIONAL MATCH (s:Company {name:'Kestrel Memory Works'})-[cp:CO_PRESENTS]-(g)
WITH g, w_explicit, w_part, w_qual, w_prog,
     count(DISTINCT cp.doc_id) * 0.25 AS w_conf
OPTIONAL MATCH (s:Company {name:'Kestrel Memory Works'})
               -[nx:LITIGATES_AGAINST|CO_MEMBER|PARTNERS_NON_SUPPLY]-(g)
WITH g.name AS producer,
     w_explicit + w_part + w_qual + w_prog + w_conf AS supply_evidence,
     count(DISTINCT nx.doc_id) AS non_supply_comentions
RETURN producer, round(supply_evidence * 1000) / 1000 AS supply_evidence,
       non_supply_comentions,
       supply_evidence = 0.0 AS no_supply_evidence_at_all
ORDER BY supply_evidence ASC, producer;

// @Q10_naive_cooccurrence
// The control: rank the top five by how often they are connected to the
// supplier by ANY edge, ignoring edge type. This is what a system with no
// relation semantics sees -- a co-occurrence counter, which is effectively what
// a passage retriever ranks by. If the decoys work, the excluded producer comes
// out at or near the TOP here, and this query is the measurement of that rather
// than an assertion about it.
MATCH (g:Company)-[rk:RANKED_IN]->(u:Universe)
WHERE u.name CONTAINS 'gpu' AND rk.rank <= 5 AND g.kind = 'GPUProducer'
OPTIONAL MATCH (s:Company {name:'Kestrel Memory Works'})-[r]-(g)
RETURN g.name AS producer, count(r) AS any_edges,
       collect(DISTINCT type(r)) AS edge_types,
       count(DISTINCT r.doc_id) AS docs
ORDER BY any_edges DESC, producer;

// @Q11_absence_only_answer
// Elimination WITHOUT the cardinality claim: name the top-five producers that
// carry no supply evidence by any route, and the ones that do. This is what
// actually answered the question in the primary run, because extraction lost
// the "four of five" claim. It is the claim-free form of the elimination, and
// comparing it with Q9 is how we tell whether the graph's advantage comes from
// the COUNT or merely from being able to express ABSENCE.
MATCH (g:Company)-[rk:RANKED_IN]->(u:Universe)
WHERE u.name CONTAINS 'gpu' AND rk.rank <= 5 AND g.kind = 'GPUProducer'
WITH DISTINCT g
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[a:SUPPLIES]->(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[:MAKES_PART]->(p:Part)
               <-[uu:USES_PART]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[:MAKES_PART*0..1]->()
               -[qq:QUALIFIED_ON]->(:Platform)<-[:MAKES_PLATFORM]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[pr:PROGRAMME_REF]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[cp:CO_PRESENTS]-(g)
WITH g.name AS producer,
     count(DISTINCT a.doc_id) + count(DISTINCT uu.doc_id) + count(DISTINCT qq.doc_id)
     + count(DISTINCT pr.doc_id) + count(DISTINCT cp.doc_id) AS routes
WITH collect(CASE WHEN routes > 0 THEN producer END) AS we,
     collect(CASE WHEN routes = 0 THEN producer END) AS wo
WITH [x IN we WHERE x IS NOT NULL] AS supplied,
     [x IN wo WHERE x IS NOT NULL] AS excluded
RETURN size(supplied) AS n_with_evidence, size(excluded) AS n_without,
       supplied, excluded;

// @Q9_elimination
// Apply the cardinality claim inside the query: if the claimant says it supplies
// n of m, and exactly n producers carry non-zero supply evidence, the remaining
// m - n are the excluded ones. Returns the answer, or returns nothing if the
// evidence does not satisfy the claim.
// The universe size is read from the graph, and the relevant claim is the one
// whose denominator matches it. That matters: extraction also produced a
// spurious claim ("two customers above ten per cent"), and this filter is what
// keeps it out without hand-picking the right claim.
MATCH (c:Company)-[rk:RANKED_IN]->(u:Universe)
WHERE u.name CONTAINS 'gpu' AND rk.rank <= 5 AND c.kind = 'GPUProducer'
WITH count(DISTINCT c) AS m
MATCH (claimant:Company {name:'Kestrel Memory Works'})-[:MAKES_CLAIM]->(k:Claim)
WHERE k.of_m = m
WITH m, k.n AS n
MATCH (g:Company)-[rk:RANKED_IN]->(u:Universe)
WHERE u.name CONTAINS 'gpu' AND rk.rank <= 5 AND g.kind = 'GPUProducer'
WITH n, m, g
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[a:SUPPLIES]->(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[:MAKES_PART]->(p:Part)
               <-[u:USES_PART]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[:MAKES_PART*0..1]->()
               -[q:QUALIFIED_ON]->(pl:Platform)<-[:MAKES_PLATFORM]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[pr:PROGRAMME_REF]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[cp:CO_PRESENTS]-(g)
WITH n, m, g.name AS producer,
     count(DISTINCT a.doc_id) + count(DISTINCT u.doc_id) + count(DISTINCT q.doc_id)
     + count(DISTINCT pr.doc_id) + count(DISTINCT cp.doc_id) AS routes
WITH n, m,
     collect(CASE WHEN routes > 0 THEN producer END) AS with_evidence,
     collect(CASE WHEN routes = 0 THEN producer END) AS without_evidence
WITH n, m,
     [x IN with_evidence WHERE x IS NOT NULL] AS supplied,
     [x IN without_evidence WHERE x IS NOT NULL] AS excluded
RETURN n, m, size(supplied) AS n_with_evidence,
       (size(supplied) = n) AS claim_satisfied,
       supplied, excluded;

// @Q12_three_state
// THE THREE-STATE QUERY. Reports unique / ambiguous / unsatisfiable explicitly
// instead of returning a row and letting the caller assume it is the answer.
//
// Reasoning, with E = producers carrying supply evidence, Z = producers with
// none, and the claim asserting n of m supplied (so m - n excluded):
//   every member of E is supplied, so every excluded producer must come from Z
//   |E| > n            -> UNSATISFIABLE  (more evidence than the claim allows)
//   |Z| = m - n        -> UNIQUE         (Z is exactly the excluded set)
//   |Z| > m - n        -> AMBIGUOUS      (excluded is some subset of Z; the
//                                         count narrows but does not close)
MATCH (c:Company)-[rk:RANKED_IN]->(u:Universe)
WHERE u.name CONTAINS 'gpu' AND rk.rank <= 5 AND c.kind = 'GPUProducer'
WITH count(DISTINCT c) AS m
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[:MAKES_CLAIM]->(k:Claim)
WHERE k.of_m = m
WITH m, k.n AS n
MATCH (g:Company)-[rk2:RANKED_IN]->(u2:Universe)
WHERE u2.name CONTAINS 'gpu' AND rk2.rank <= 5 AND g.kind = 'GPUProducer'
WITH m, n, g
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[a:SUPPLIES]->(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[:MAKES_PART]->(p:Part)
               <-[uu:USES_PART]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[:MAKES_PART*0..1]->()
               -[qq:QUALIFIED_ON]->(:Platform)<-[:MAKES_PLATFORM]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[pr:PROGRAMME_REF]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[cp:CO_PRESENTS]-(g)
WITH m, n, g.name AS producer,
     count(DISTINCT a.doc_id) + count(DISTINCT uu.doc_id) + count(DISTINCT qq.doc_id)
     + count(DISTINCT pr.doc_id) + count(DISTINCT cp.doc_id) AS routes
WITH m, n,
     [x IN collect(CASE WHEN routes > 0 THEN producer END) WHERE x IS NOT NULL] AS E,
     [x IN collect(CASE WHEN routes = 0 THEN producer END) WHERE x IS NOT NULL] AS Z
WITH m, n, E, Z, CASE WHEN n IS NULL THEN NULL ELSE m - n END AS n_excluded
RETURN
  CASE
    WHEN n IS NULL             THEN 'NO_CLAIM'
    WHEN size(E) > n           THEN 'UNSATISFIABLE'
    WHEN size(Z) = n_excluded  THEN 'UNIQUE'
    WHEN size(Z) > n_excluded  THEN 'AMBIGUOUS'
    ELSE 'UNSATISFIABLE'
  END AS state,
  n, m, n_excluded, size(E) AS n_with_evidence, size(Z) AS n_zero_evidence,
  E AS supplied_certain,
  CASE WHEN n IS NOT NULL AND size(Z) = n_excluded THEN Z ELSE NULL END AS excluded_certain,
  CASE WHEN n IS NOT NULL AND size(Z) > n_excluded THEN Z ELSE NULL END AS ambiguous_pool;

// @Q13_enumerate_worlds
// Enumerate the consistent worlds when the state is AMBIGUOUS: each way of
// choosing (m - n) excluded producers from the zero-evidence pool Z.
//
// COST NOTE, which is the argument for a solver. This works only because
// m - n = 1 here, so one UNWIND over Z enumerates every world. For a general
// (m - n) = k you need k nested UNWINDs with an ordering predicate to avoid
// permutations, i.e. query text that changes shape with the data, or recursion
// (Cypher) / a recursive CTE (SQL). Neither language enumerates
// choose(|Z|, k) declaratively. That is where a constraint solver stops being
// a luxury.
MATCH (c:Company)-[rk:RANKED_IN]->(u:Universe)
WHERE u.name CONTAINS 'gpu' AND rk.rank <= 5 AND c.kind = 'GPUProducer'
WITH count(DISTINCT c) AS m
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[:MAKES_CLAIM]->(k:Claim)
WHERE k.of_m = m
WITH m, k.n AS n
MATCH (g:Company)-[rk2:RANKED_IN]->(u2:Universe)
WHERE u2.name CONTAINS 'gpu' AND rk2.rank <= 5 AND g.kind = 'GPUProducer'
WITH m, n, g
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[a:SUPPLIES]->(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[:MAKES_PART]->(p:Part)
               <-[uu:USES_PART]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[:MAKES_PART*0..1]->()
               -[qq:QUALIFIED_ON]->(:Platform)<-[:MAKES_PLATFORM]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[pr:PROGRAMME_REF]-(g)
OPTIONAL MATCH (:Company {name:'Kestrel Memory Works'})-[cp:CO_PRESENTS]-(g)
WITH m, n, g.name AS producer,
     count(DISTINCT a.doc_id) + count(DISTINCT uu.doc_id) + count(DISTINCT qq.doc_id)
     + count(DISTINCT pr.doc_id) + count(DISTINCT cp.doc_id) AS routes
WITH m, n,
     [x IN collect(CASE WHEN routes > 0 THEN producer END) WHERE x IS NOT NULL] AS E,
     [x IN collect(CASE WHEN routes = 0 THEN producer END) WHERE x IS NOT NULL] AS Z
WITH m, n, E, Z, m - n AS k
WHERE n IS NOT NULL AND k = 1 AND size(Z) > k
UNWIND Z AS excluded_candidate
RETURN excluded_candidate,
       [x IN Z WHERE x <> excluded_candidate] + E AS supplied_in_this_world,
       size(Z) AS n_worlds
ORDER BY excluded_candidate;
