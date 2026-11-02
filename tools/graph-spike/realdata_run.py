#!/usr/bin/env python3
"""Real-data face-validity check, partitioned from the primary experiment.

Runs the SAME extraction prompt over real newsroom text and reports only
MECHANISM behaviour: does the extractor produce typed edges, and does it keep
supply relations distinct from partnership and co-mention relations?

IT DOES NOT, AND MUST NOT, ASSERT WHICH REAL COMPANY SUPPLIES WHICH. A handful
of press releases is incomplete evidence, and that inference is commercially
sensitive. Only aggregate edge-type counts and the supply/non-supply split are
reported. Company-level pairings are written to the detail file for the author's
eyes but are explicitly NOT a finding of this spike.

The download directory is untrusted data: it is passed in as an argument and
nothing in it is executed or imported.

Usage:
  python3 realdata_run.py --src realdata_downloads --out out/realdata.json
"""

import argparse
import collections
import glob
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common    # noqa: E402
import extract   # noqa: E402

# A register of real company names, kinds only. No supply relation encoded.
REAL_REGISTER = """
- NVIDIA Corporation [GPUProducer] (also written: NVIDIA, Nvidia)
- Advanced Micro Devices, Inc. [GPUProducer] (also written: AMD)
- Intel Corporation [GPUProducer] (also written: Intel)
- Micron Technology, Inc. [MemorySupplier] (also written: Micron)
- SK hynix Inc. [MemorySupplier] (also written: SK hynix, SK Hynix)
- Samsung Electronics Co., Ltd. [MemorySupplier] (also written: Samsung)
- Oracle Corporation [OtherCompany] (also written: Oracle)
- Meta Platforms, Inc. [OtherCompany] (also written: Meta)
Any other company named in the document: use the surface form as written.
"""

NON_SUPPLY_TYPES = {"CO_MEMBER", "LITIGATES_AGAINST", "PARTNERS_NON_SUPPLY",
                    "CO_PRESENTS", "PROGRAMME_REF"}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", required=True, help="download dir (untrusted data)")
    ap.add_argument("--out", required=True)
    ap.add_argument("--maxchars", type=int, default=6000)
    a = ap.parse_args()

    paths = sorted(glob.glob(os.path.join(a.src, "*.txt")))
    log = {}
    lp = os.path.join(a.src, "fetch_log.json")
    if os.path.exists(lp):
        for e in json.load(open(lp)):
            if e.get("file"):
                log[e["file"]] = e["url"]

    docs = []
    for p in paths:
        name = os.path.basename(p)[:-4]
        text = open(p, errors="replace").read()[:a.maxchars]
        docs.append({"id": name, "title": name, "date": "", "type": "web_page",
                     "source": log.get(name, "unknown"), "text": text})
    print("%d real documents, truncated to %d chars" % (len(docs), a.maxchars))

    rels = []
    for d in docs:
        raw, dt = common.generate(extract.PROMPT.format(
            register=REAL_REGISTER, schema=extract.SCHEMA,
            doc_id=d["id"], body=common.doc_blob(d)), think=False)
        arr = common.first_json(raw)
        n = 0
        if isinstance(arr, list):
            for r in arr:
                if isinstance(r, dict) and r.get("type"):
                    rels.append({"doc_id": d["id"], "url": d["source"],
                                 "type": str(r["type"]).strip().upper(),
                                 "from": r.get("from"), "to": r.get("to"),
                                 "stance": r.get("stance", "asserted"),
                                 "quote": (r.get("quote") or "")[:200],
                                 "props": r.get("props") or {}})
                    n += 1
        print("  %-8s %5.1fs  %2d rels  %s" % (d["id"], dt, n, d["source"][:60]))

    by_type = collections.Counter(r["type"] for r in rels)
    supply = [r for r in rels if r["type"] == "SUPPLIES"]
    nonsupply = [r for r in rels if r["type"] in NON_SUPPLY_TYPES]

    summary = {
        "n_documents": len(docs),
        "n_relations": len(rels),
        "by_type": dict(by_type),
        "n_supply_edges": len(supply),
        "n_non_supply_comention_edges": len(nonsupply),
        "mechanism_checks": {
            "produced_typed_edges": len(rels) > 0,
            "distinguished_supply_from_comention":
                len(nonsupply) > 0 or len(supply) == 0,
            "emitted_aggregate_claims": int(by_type.get("AGGREGATE_SUPPLY_CLAIM", 0)),
        },
        "LIMIT": "Mechanism check only. No conclusion is drawn, and none may be "
                 "drawn from this file, about which real company supplies which. "
                 "The sample is a handful of pages and the evidence is incomplete.",
    }
    print("\nby type:", by_type.most_common())
    print("supply edges: %d   non-supply co-mention edges: %d"
          % (len(supply), len(nonsupply)))

    os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
    with open(a.out, "w") as f:
        json.dump({"summary": summary, "detail_not_a_finding": rels}, f, indent=1)
    print("wrote", a.out)


if __name__ == "__main__":
    main()
