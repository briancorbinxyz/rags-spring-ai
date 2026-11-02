#!/usr/bin/env python3
"""Sensitivity input: drop one document's relations from an extraction result.

Extraction is strictly per-document, so removing the aggregate-claim document's
relations is exactly equivalent to re-extracting a corpus that never contained
it -- at no LLM cost. The corpus_noagg/ directory exists so the retrieval arm,
which indexes documents rather than relations, can be rerun the same way.

Usage:
  python3 drop_claim_doc.py --relations out/relations.json \
      --doc KMW-2026-014 --out out/relations_noagg.json
"""

import argparse
import json


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--relations", required=True)
    ap.add_argument("--doc", required=True)
    ap.add_argument("--out", required=True)
    a = ap.parse_args()

    d = json.load(open(a.relations))
    before = len(d["relations"])
    d["relations"] = [r for r in d["relations"] if r["doc_id"] != a.doc]
    d["dropped_doc"] = a.doc
    d["dropped_relations"] = before - len(d["relations"])
    with open(a.out, "w") as f:
        json.dump(d, f, indent=1)
    print("dropped %d relations from %s: %d -> %d"
          % (d["dropped_relations"], a.doc, before, len(d["relations"])))


if __name__ == "__main__":
    main()
