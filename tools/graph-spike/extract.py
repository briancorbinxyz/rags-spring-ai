#!/usr/bin/env python3
"""Extract typed relations from each document with a local LLM (qwen3:8b).

Design constraint for honesty: this prompt is GENERIC. It does not know the
question, does not know that a cardinality claim matters, does not know which
companies are the answer, and treats every document type identically. The
relation schema is a closed set chosen before the answer was looked at, and it
deliberately includes non-supply co-mention types so that the graph has the
vocabulary to record "these two are mentioned together but no supply relation is
asserted" -- which is the only way any arm can avoid the decoys.

Output: relations.json  (one record per extracted relation, with provenance)

Usage:
  python3 extract.py --corpus corpus --out out/relations.json
  python3 extract.py --corpus corpus --out out/rel.json --limit 5   # smoke test
"""

import argparse
import json
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common  # noqa: E402

SCHEMA = """
Relation types (use ONLY these):

  SUPPLIES          {from: Company, to: Company, component: str}
        An actual commercial supply of physical components from one company to
        another is asserted by this document.
  MAKES_PART        {from: Company, to: Part}
        The company manufactures or owns the named part/product family.
  USES_PART         {from: Company or Platform, to: Part, context: str}
        The company or platform uses/integrates the named part. context is a
        short phrase such as "production", "job advert", "bring-up".
  MAKES_PLATFORM    {from: Company, to: Platform}
        The company builds the named platform/product line.
  QUALIFIED_ON      {from: Part, to: Platform, customer_named: true|false}
        The part has passed qualification on the platform.
  PROGRAMME_REF     {from: Company, to: Company, note: str}
        One company refers to a named commercial programme or account involving
        the other, without stating a supply relation outright.
  CO_PRESENTS       {from: Company, to: Company, venue: str}
        The two companies jointly present, exhibit or publish together.
  CO_MEMBER         {from: Company, to: Company, body: str}
        The two companies are both members of a standards body, working group,
        trade body or board.
  LITIGATES_AGAINST {from: Company, to: Company, matter: str}
        Legal action between the two companies.
  PARTNERS_NON_SUPPLY {from: Company, to: Company, note: str}
        A collaboration that the document says involves no component supply.
  RANKED_TOP        {from: Company, rank: int, universe: str}
        The document ranks the company within a named universe.
  AGGREGATE_SUPPLY_CLAIM {from: Company, n: int, of_m: int, customer_class: str}
        The company claims to supply some COUNT of a CLASS of customers without
        naming them.

Rules:
  - Extract only what THIS document asserts. Never infer across documents.
  - If a document mentions two companies but asserts no supply relation between
    them, do NOT emit SUPPLIES. Use CO_MEMBER / LITIGATES_AGAINST /
    CO_PRESENTS / PARTNERS_NON_SUPPLY / PROGRAMME_REF as appropriate.
  - Use the canonical company names from the register below. If a surface form
    is an alias, map it to the canonical name. If a company is not in the
    register, use the surface form as written.
  - "stance" is "asserted" normally, or "denied" if the document explicitly
    denies the relation.
  - "quote" must be a short verbatim span (under 20 words) from the document
    that supports the relation. Do not paraphrase in the quote.
  - NEVER put a company in "from" or "to" unless that company is named
    explicitly in this document. If the document describes a COUNT of customers
    without naming them ("four of the five largest", "a tier-one customer", "two
    customers above ten per cent"), you must NOT guess who they are: emit
    AGGREGATE_SUPPLY_CLAIM, or QUALIFIED_ON with customer_named false, and emit
    nothing naming a company.
  - A threshold or partial supplier list ("suppliers above ten per cent of
    spend") tells you nothing about companies absent from it. Do not emit any
    relation with stance "denied" on the strength of an absence.
  - DIRECTION. SUPPLIES always runs supplier -> customer. Read the direction off
    the sentence, not off the order the names appear in. Worked examples:
      "Acme Ltd will supply widgets to Beta Inc"      => SUPPLIES Acme -> Beta
      "Beta Inc today named Acme Ltd as its supplier"  => SUPPLIES Acme -> Beta
      "Beta Inc's tier 1 suppliers were: Acme Ltd"     => SUPPLIES Acme -> Beta
      "We have appointed Acme Ltd to supply us"        => SUPPLIES Acme -> publisher
    A document that lists a company's OWN suppliers yields edges pointing INTO
    that company, one per listed supplier.
  - MAKES_PART. If the document says a company's part, product family or stack is
    its own ("its KM-9 stack", "the Acme AX-100 family"), emit MAKES_PART from
    that company to that part, in addition to any other relation.
  - QUALIFIED_ON. Prefer the PART as "from" when the document names the part
    ("its KM-9 stack completed qualification on the Zeta platform" =>
    QUALIFIED_ON KM-9 -> Zeta). Set props.customer_named false when the document
    withholds the customer's identity.
"""

PROMPT = """You extract a knowledge graph from corporate documents.

COMPANY REGISTER (canonical names, their kind, and known aliases):
{register}

{schema}

Return ONLY a JSON array, as COMPACT single-line JSON with no indentation and no
spaces after punctuation. Each element:
  {{"type":"<relation type>","from":"...","to":"...","stance":"asserted"|"denied","quote":"...","props":{{...}}}}

For RANKED_TOP and AGGREGATE_SUPPLY_CLAIM there is no "to"; use "to": null and
put rank/universe or n/of_m/customer_class in props.

If the document asserts no relation at all, return [].

DOCUMENT (id {doc_id}):
{body}

JSON array:"""


def extract_doc(d, register):
    p = PROMPT.format(register=register, schema=SCHEMA, doc_id=d["id"],
                      body=common.doc_blob(d))
    raw, dt = common.generate(p, think=False)
    arr = common.first_json(raw)
    if not isinstance(arr, list):
        return [], dt, raw
    out = []
    for r in arr:
        if not isinstance(r, dict) or "type" not in r:
            continue
        out.append({
            "doc_id": d["id"],
            "doc_type": d["type"],
            "doc_date": d["date"],
            "type": str(r.get("type", "")).strip().upper(),
            "from_raw": r.get("from"),
            "to_raw": r.get("to"),
            "from": common.resolve(r.get("from")) or r.get("from"),
            "to": common.resolve(r.get("to")) or r.get("to"),
            "stance": (r.get("stance") or "asserted"),
            "quote": (r.get("quote") or "")[:300],
            "props": r.get("props") if isinstance(r.get("props"), dict) else {},
        })
    return out, dt, raw


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--corpus", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--raw", default=None, help="also dump raw model responses")
    ap.add_argument("--workers", type=int, default=3)
    a = ap.parse_args()

    docs = common.load_corpus(a.corpus)
    if a.limit:
        docs = docs[:a.limit]
    register = common.entity_dictionary_text()

    rels, raws = [], {}
    t0 = time.time()

    def work(d):
        try:
            return d, extract_doc(d, register)
        except Exception as e:
            return d, ([], 0.0, "ERROR: %s" % e)

    from concurrent.futures import ThreadPoolExecutor
    done = 0
    with ThreadPoolExecutor(max_workers=a.workers) as ex:
        for d, (r, dt, raw) in ex.map(work, docs):
            done += 1
            rels.extend(r)
            raws[d["id"]] = raw
            print("[%3d/%3d] %-16s %5.1fs  %d rels%s"
                  % (done, len(docs), d["id"], dt, len(r),
                     "  TRUNCATED" if raw and not raw.rstrip().endswith("]") else ""),
                  flush=True)
    wall = time.time() - t0

    os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
    with open(a.out, "w") as f:
        json.dump({"model": common.GEN_MODEL, "options": common.GEN_OPTS,
                   "n_docs": len(docs), "wall_seconds": round(wall, 1),
                   "relations": rels}, f, indent=1)
    if a.raw:
        with open(a.raw, "w") as f:
            json.dump(raws, f, indent=1)

    import collections
    print("\nwall=%.1fs  docs=%d  relations=%d" % (wall, len(docs), len(rels)))
    print("by type:", collections.Counter(r["type"] for r in rels).most_common())


if __name__ == "__main__":
    main()
