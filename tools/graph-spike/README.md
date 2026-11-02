# graph-spike

A measured head-to-head: Neo4j against a non-graph passage retriever, and
against plain SQL, on a discovery task where the answer is never stated in any
single document and depends on a *count*.

Results and the verdict live in
`research_notes/Discovering undisclosed supplier links/neo4j_spike_results.md`.

## The task

Supplier "Kestrel Memory Works" claims it supplies memory to "four of the
world's five top GPU producers". Find which four, and therefore which one is
excluded. No document states the pairing. The inference needs an aggregate
cardinality claim on one side and scattered, unevenly strong per-entity
evidence on the other.

All companies are fictional. Ground truth is planted and lives in `truth/`,
which no arm reads; only `score.py` opens it.

## Layout

| path | what it is |
| --- | --- |
| `gen_corpus.py` | corpus generator, seeded and perturbable |
| `corpus/` | 119 generated documents (`corpus.json` + `docs/*.txt`) |
| `corpus_noagg/` | same, with the aggregate "4 of 5" claim removed |
| `truth/` | ground truth and per-document tags. Not read by any arm |
| `common.py` | corpus loading, chunking, BM25, Ollama calls, entity register |
| `extract.py` | LLM relation extraction into a closed typed schema |
| `graphbuild.py` | normalises extracted relations into nodes and edges |
| `arm_graph.py` | loads Neo4j via `cypher-shell`, runs `cypher/queries.cypher` |
| `cypher/queries.cypher` | the actual Cypher, run verbatim |
| `arm_sql.py` | the same relations in SQLite and DuckDB, with the SQL inline |
| `arm_retrieval.py` | BM25 + dense retrieval with a local LLM reader, 3 sub-arms |
| `score.py` | the only file that reads `truth/` |
| `drop_claim_doc.py` | builds the sensitivity input without re-running the LLM |
| `inject_claim.py` | ORACLE: injects the aggregate claim the extractor never captured |
| `b3_tiebreak_test.py` | shows B3's apparent win is tie-breaking luck (33%) |
| `variant_relations.py` | re-extracts only perturbed docs and splices them |
| `score_variants.py` | scores the follow-up variants on the three-state outcome |
| `corpus_v_{ambig,unique,unsat}/` | follow-up variants where cardinality is necessary |
| `truth_v_*/` | each variant's own truth, incl. its expected three-state outcome |
| `realdata_fetch.py` | polite one-off fetch of public newsroom pages |
| `realdata_run.py` | mechanism-only check on real prose |
| `realdata_downloads/` | untrusted downloaded data. No script lives here |
| `out/` | every raw output, including per-query timings and raw model text |
| `run_all.sh` | exact commands, in order |
| `loc.sh` | lines of code per arm |

## Running it

```bash
bash run_all.sh
```

Needs Neo4j (`brew install neo4j`), a running Ollama with
`nomic-embed-text:v1.5` and `qwen3:8b`, and `sqlite3` / `duckdb`.

**No Python packages are required.** Every script is standard library only, so
there is no virtual environment to create and nothing to install.

Neo4j, once per machine:

```bash
/opt/homebrew/opt/neo4j/bin/neo4j-admin dbms set-initial-password graphspike123
/opt/homebrew/opt/neo4j/bin/neo4j console &
```

The slow step is `extract.py` (119 documents through a local 8B model).
Everything downstream of `out/relations.json` runs in seconds, so iterate on the
queries without re-extracting.

## Follow-up: cardinality variants

The primary corpus made absence *sufficient* (one producer at zero evidence).
`gen_corpus.py --two-zero` puts **two** producers at zero evidence so the count
has to do real work; `--claim-n N` changes the claim; `--veradyne-evidence`
makes the claim unsatisfiable. `Q12_three_state` / `S8_three_state` report
unique / ambiguous / unsatisfiable explicitly instead of returning a row.
Steps 10-14 of `run_all.sh`.

## Real-data caveat

`realdata_*` fetch a handful of genuinely public newsroom pages to check that
the mechanism behaves the same way on real prose. They draw **no conclusion
about which real company supplies which**: that inference is commercially
sensitive and the evidence here is far too thin to support it. The limit is
restated inside `realdata_run.py` and in its output file.
