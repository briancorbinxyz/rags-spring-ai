#!/usr/bin/env bash
# Lines of code per arm: non-blank, non-comment lines, so the cost comparison is
# about work done rather than about how much I commented.
cd "$(dirname "$0")"

sloc() {  # python
  grep -vE '^\s*($|#)' "$1" | grep -vE '^\s*"""' | wc -l | tr -d ' '
}
qloc() {  # cypher / sql, stripping -- and // comment lines
  grep -vE '^\s*($|--|//)' "$1" | wc -l | tr -d ' '
}

printf '%-44s %6s\n' "file" "sloc"
printf '%-44s %6s\n' "--------------------------------------------" "------"
for f in common.py graphbuild.py extract.py arm_graph.py arm_sql.py \
         arm_retrieval.py score.py gen_corpus.py drop_claim_doc.py \
         realdata_fetch.py realdata_run.py; do
  printf '%-44s %6s\n' "$f" "$(sloc $f)"
done
printf '%-44s %6s\n' "cypher/queries.cypher" "$(qloc cypher/queries.cypher)"

echo
echo "Per-arm totals (shared infrastructure counted once per arm that needs it):"
SH=$(( $(sloc common.py) ))
GB=$(( $(sloc graphbuild.py) ))
EX=$(( $(sloc extract.py) ))
printf '  %-40s %5d  (= common %d + graphbuild %d + extract %d + arm_graph %d + cypher %d)\n' \
  "graph arm (Neo4j)" \
  $(( SH + GB + EX + $(sloc arm_graph.py) + $(qloc cypher/queries.cypher) )) \
  $SH $GB $EX $(sloc arm_graph.py) $(qloc cypher/queries.cypher)
printf '  %-40s %5d  (= common %d + graphbuild %d + extract %d + arm_sql %d, SQL inline)\n' \
  "relational arm (SQLite/DuckDB)" \
  $(( SH + GB + EX + $(sloc arm_sql.py) )) \
  $SH $GB $EX $(sloc arm_sql.py)
printf '  %-40s %5d  (= common %d + arm_retrieval %d; no extraction needed)\n' \
  "non-graph arm (BM25 + dense + reader)" \
  $(( SH + $(sloc arm_retrieval.py) )) $SH $(sloc arm_retrieval.py)
echo
echo "Shared by all arms but not part of any arm's own cost:"
printf '  %-40s %5d\n' "gen_corpus.py (the experiment itself)" "$(sloc gen_corpus.py)"
printf '  %-40s %5d\n' "score.py (measurement)" "$(sloc score.py)"
