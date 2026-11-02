#!/usr/bin/env python3
"""Build a variant's relations by re-extracting ONLY the documents that changed.

Extraction is strictly per-document, so re-extracting the handful of perturbed
documents and splicing their relations into the primary result is exactly
equivalent to a full re-extraction of the variant corpus -- at a few per cent of
the cost (a full pass is ~22 minutes).

Documents are matched by id. A document whose text is byte-identical to the
primary keeps its primary relations verbatim; a changed or new document is
re-extracted; a document absent from the variant has its relations dropped.

Usage:
  python3 variant_relations.py --base-corpus corpus --base-relations out/relations.json \
      --corpus corpus_v_ambig --out out/relations_v_ambig.json
"""

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common   # noqa: E402
import extract  # noqa: E402


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base-corpus", required=True)
    ap.add_argument("--base-relations", required=True)
    ap.add_argument("--corpus", required=True)
    ap.add_argument("--out", required=True)
    a = ap.parse_args()

    base_docs = {d["id"]: d for d in common.load_corpus(a.base_corpus)}
    var_docs = {d["id"]: d for d in common.load_corpus(a.corpus)}
    payload = json.load(open(a.base_relations))
    base_rels = payload["relations"]

    changed = [i for i, d in var_docs.items()
               if i not in base_docs or base_docs[i]["text"] != d["text"]]
    removed = [i for i in base_docs if i not in var_docs]
    print("variant %s: %d docs, %d changed/new, %d removed"
          % (a.corpus, len(var_docs), len(changed), len(removed)))
    for i in sorted(changed):
        print("   changed: %s" % i)
    for i in sorted(removed):
        print("   removed: %s" % i)

    keep = [r for r in base_rels
            if r["doc_id"] in var_docs and r["doc_id"] not in changed]
    register = common.entity_dictionary_text()
    fresh = []
    for i in sorted(changed):
        r, dt, raw = extract.extract_doc(var_docs[i], register)
        print("   re-extracted %-16s %5.1fs  %d rels" % (i, dt, len(r)), flush=True)
        fresh.extend(r)

    payload["relations"] = keep + fresh
    payload["variant_corpus"] = a.corpus
    payload["reextracted_docs"] = sorted(changed)
    payload["dropped_docs"] = sorted(removed)
    with open(a.out, "w") as f:
        json.dump(payload, f, indent=1)
    print("relations: %d kept + %d fresh = %d -> %s"
          % (len(keep), len(fresh), len(payload["relations"]), a.out))


if __name__ == "__main__":
    main()
