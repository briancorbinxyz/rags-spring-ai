#!/usr/bin/env bash
# Exact commands for the whole spike, in order. Rerun from this directory.
#
#   cd /Users/briancorbin/Code/rags/rags-spring-ai/tools/graph-spike
#   bash run_all.sh
#
# Prerequisites, all already present on this machine:
#   brew install neo4j            # pulls cypher-shell and openjdk@21
#   ollama serve                  # with nomic-embed-text:v1.5 and qwen3:8b pulled
#   sqlite3, duckdb               # already installed
# No Python packages are required: every script is stdlib only.
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export NEO4J_PASSWORD="${NEO4J_PASSWORD:-graphspike123}"
mkdir -p out

echo "### 0. start Neo4j (once per machine: set the initial password first)"
echo "#   /opt/homebrew/opt/neo4j/bin/neo4j-admin dbms set-initial-password graphspike123"
echo "#   /opt/homebrew/opt/neo4j/bin/neo4j console &"
until curl -s -m 2 http://localhost:7474/ >/dev/null 2>&1; do sleep 1; done
echo "neo4j up"

echo "### 1. generate the controlled corpus and the ground truth it never sees"
python3 -I gen_corpus.py --out corpus       --truth truth --seed 20261029
python3 -I gen_corpus.py --out corpus_noagg --seed 20261029 --no-aggregate

echo "### 2. extract typed relations with the local LLM (slowest step, ~45 min)"
python3 -I extract.py --corpus corpus --out out/relations.json \
    --raw out/relations_raw.json --workers 3 | tee out/extract_log.txt

echo "### 3. sensitivity input: the same relations minus the aggregate-claim doc."
# Extraction is per-document, so dropping that document's relations is exactly
# equivalent to re-extracting the no-aggregate corpus, at no LLM cost.
python3 -I drop_claim_doc.py --relations out/relations.json \
    --doc KMW-2026-014 --out out/relations_noagg.json

echo "### 4. graph arm, as a ladder of controls"
#   strict : raw extraction, warts and all
#   prov   : + provenance check (endpoints must appear in their source document)
#   full   : + direction repair (the 8B model inverts "our suppliers were:")
#   oracle : + the aggregate claim the extractor never captured (labelled oracle)
python3 -I arm_graph.py --relations out/relations.json \
    --out out/graph_strict.json --tag strict | tee out/graph_strict_log.txt
python3 -I arm_graph.py --relations out/relations.json --corpus corpus \
    --out out/graph_prov.json --tag prov | tee out/graph_prov_log.txt
python3 -I arm_graph.py --relations out/relations.json --corpus corpus \
    --repair-direction --out out/graph.json --tag full | tee out/graph_log.txt
python3 -I inject_claim.py --relations out/relations.json \
    --out out/relations_oracleclaim.json
python3 -I arm_graph.py --relations out/relations_oracleclaim.json --corpus corpus \
    --repair-direction --out out/graph_oracle.json --tag oracle | tee out/graph_oracle_log.txt
# sensitivity: drop the aggregate claim entirely
python3 -I drop_claim_doc.py --relations out/relations_oracleclaim.json \
    --doc KMW-2026-014 --out out/relations_noagg.json
python3 -I arm_graph.py --relations out/relations_noagg.json --corpus corpus \
    --repair-direction --out out/graph_noagg.json --tag noagg | tee out/graph_noagg_log.txt

echo "### 5. relational arm (SQLite + DuckDB), same relations, same ladder"
for cfg in strict prov full oracle noagg; do
  case $cfg in
    strict) args="--relations out/relations.json" ;;
    prov)   args="--relations out/relations.json --corpus corpus" ;;
    full)   args="--relations out/relations.json --corpus corpus --repair-direction" ;;
    oracle) args="--relations out/relations_oracleclaim.json --corpus corpus --repair-direction" ;;
    noagg)  args="--relations out/relations_noagg.json --corpus corpus --repair-direction" ;;
  esac
  out=out/sql_$cfg.json; [ "$cfg" = full ] && out=out/sql.json
  python3 -I arm_sql.py $args --out $out --tag $cfg | tee out/sql_${cfg}_log.txt
done

echo "### 6. non-graph arm (BM25 + dense, LLM reader), 3 reps"
python3 -I -u arm_retrieval.py --corpus corpus --out out/retrieval.json --reps 3 \
    | tee out/retrieval_log.txt
python3 -I -u arm_retrieval.py --corpus corpus_noagg --out out/retrieval_noagg.json \
    --no-aggregate --reps 1 | tee out/retrieval_noagg_log.txt

echo "### 7. score everything against truth"
python3 -I score.py --truth truth/ground_truth.json \
    --graph strict=out/graph_strict.json --graph prov=out/graph_prov.json \
    --graph full=out/graph.json --graph oracle=out/graph_oracle.json \
    --sql strict=out/sql_strict.json --sql prov=out/sql_prov.json \
    --sql full=out/sql.json --sql oracle=out/sql_oracle.json \
    --retrieval out/retrieval.json \
    --out out/scores.json --label primary | tee out/scores.txt
python3 -I score.py --truth truth/ground_truth.json \
    --graph noagg=out/graph_noagg.json --sql noagg=out/sql_noagg.json \
    --retrieval out/retrieval_noagg.json \
    --out out/scores_noagg.json --label sensitivity_no_aggregate_claim \
    | tee out/scores_noagg.txt

echo "### 8. real-data face validity, kept separate. Mechanism only."
python3 -I realdata_fetch.py --urls realdata_urls.txt \
    --dest realdata_downloads --delay 6
python3 -I realdata_run.py --src realdata_downloads --out out/realdata.json \
    | tee out/realdata_log.txt

echo "### 9. lines of code per arm"
bash loc.sh | tee out/loc.txt

# =====================================================================
# FOLLOW-UP: variants where cardinality is necessary, not merely available
# =====================================================================
echo "### 10. generate the three cardinality variants"
python3 -I gen_corpus.py --out corpus_v_ambig  --truth truth_v_ambig  --seed 20261029 --two-zero
python3 -I gen_corpus.py --out corpus_v_unique --truth truth_v_unique --seed 20261029 --two-zero --claim-n 3
python3 -I gen_corpus.py --out corpus_v_unsat  --truth truth_v_unsat  --seed 20261029 --veradyne-evidence

echo "### 11. re-extract ONLY the perturbed documents and splice (2-3 docs, not 119)"
for v in ambig unique unsat; do
  python3 -I variant_relations.py --base-corpus corpus \
      --base-relations out/relations.json --corpus corpus_v_$v \
      --out out/relations_v_$v.json | tee out/variant_${v}_log.txt
done

echo "### 12. oracle claim per variant (the extractor never captures it: 0/5)"
python3 -I inject_claim.py --relations out/relations_v_ambig.json  --out out/relations_v_ambig_claim.json  --n 4
python3 -I inject_claim.py --relations out/relations_v_unique.json --out out/relations_v_unique_claim.json --n 3
python3 -I inject_claim.py --relations out/relations_v_unsat.json  --out out/relations_v_unsat_claim.json  --n 4
for v in ambig unique unsat; do
  python3 -I drop_claim_doc.py --relations out/relations_v_${v}_claim.json \
      --doc KMW-2026-014 --out out/relations_v_${v}_noclaim.json
done

echo "### 13. three-state query, both engines, with and without the claim"
for v in ambig unique unsat; do for c in claim noclaim; do
  python3 -I arm_graph.py --relations out/relations_v_${v}_${c}.json \
      --corpus corpus_v_$v --repair-direction \
      --out out/graph_v_${v}_${c}.json --tag v_${v}_${c} > out/graph_v_${v}_${c}_log.txt 2>&1
  python3 -I arm_sql.py --relations out/relations_v_${v}_${c}.json \
      --corpus corpus_v_$v --repair-direction \
      --out out/sql_v_${v}_${c}.json --tag v_${v}_${c} > out/sql_v_${v}_${c}_log.txt 2>&1
done; done

echo "### 14. score the variants on the three-state outcome"
python3 -I score_variants.py --out out/scores_variants.json | tee out/scores_variants.txt
