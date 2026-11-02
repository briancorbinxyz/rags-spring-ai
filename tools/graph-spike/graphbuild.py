#!/usr/bin/env python3
"""Turn extract.py's relations.json into normalised nodes and edges.

Both the Neo4j arm and the SQL arm consume THIS output, unchanged, so the only
difference between those two arms is the query language and engine. That is the
point: it isolates "graph database" from "structured layer".

Normalisation here is deliberately generic: part numbers and platform names are
canonicalised by shape, not by a lookup table of the answer.
"""

import re
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import common  # noqa: E402

ALL_SURFACE = []
for _e in common.ENTITIES:
    for _s in [_e["canon"]] + _e["aliases"]:
        ALL_SURFACE.append(_s)
ALL_SURFACE.sort(key=len, reverse=True)

PARTNO = re.compile(r"\b([A-Z]{2,4}-?\d+[A-Z]*)\b")


def strip_companies(s):
    out = s
    for surf in ALL_SURFACE:
        out = re.sub(re.escape(surf), " ", out, flags=re.I)
    return re.sub(r"\s+", " ", out).strip(" -,.")


def norm_part(s):
    """'Kestrel KM-9 HBM3E stacks' -> 'KM-9'. Shape-based, not a lookup."""
    if not s:
        return None
    base = strip_companies(str(s))
    m = PARTNO.search(base.upper().replace(" -", "-"))
    if m:
        return m.group(1).replace("-", "-")
    base = re.sub(r"\b(stacks?|memory|family|families|modules?|parts?|components?)\b",
                  " ", base, flags=re.I)
    base = re.sub(r"\s+", " ", base).strip()
    return base.upper() or None


STOPWORDS_PLAT = re.compile(
    r"\b(class|platform|architecture|accelerator|accelerators|programme|program|"
    r"series|edition|based|generation|the|its|a|an)\b", re.I)


def norm_platform(s):
    """'Aurora-class accelerator platform' -> 'AURORA'. Shape-based."""
    if not s:
        return None
    base = strip_companies(str(s))
    base = base.replace("-", " ")
    base = STOPWORDS_PLAT.sub(" ", base)
    base = re.sub(r"[^A-Za-z0-9 ]", " ", base)
    toks = [t for t in base.split() if t]
    if not toks:
        return None
    return toks[0].upper()


# Which relation types connect which node kinds.
EDGE_SPEC = {
    "SUPPLIES":            ("Company", "Company"),
    "MAKES_PART":          ("Company", "Part"),
    "USES_PART":           ("Company", "Part"),
    "MAKES_PLATFORM":      ("Company", "Platform"),
    "QUALIFIED_ON":        ("Part", "Platform"),
    "PROGRAMME_REF":       ("Company", "Company"),
    "CO_PRESENTS":         ("Company", "Company"),
    "CO_MEMBER":           ("Company", "Company"),
    "LITIGATES_AGAINST":   ("Company", "Company"),
    "PARTNERS_NON_SUPPLY": ("Company", "Company"),
    "RANKED_TOP":          ("Company", None),
    "AGGREGATE_SUPPLY_CLAIM": ("Company", None),
}

# Evidence weight that each edge type lends to "X supplies to Y".
# Chosen on the semantics of the edge type, before looking at any result:
# an asserted supply is strong; a part/platform chain is indirect but real;
# a co-mention with no supply asserted is worth exactly nothing.
SUPPLY_WEIGHT = {
    "SUPPLIES": 1.0,
    "USES_PART_CHAIN": 0.6,      # Y uses a part X makes
    "QUALIFIED_CHAIN": 0.5,      # X's part qualified on Y's platform
    "PROGRAMME_REF": 0.4,        # X names a commercial programme involving Y
    "CO_PRESENTS": 0.25,         # joint technical presentation about a ramp
    "CO_MEMBER": 0.0,
    "LITIGATES_AGAINST": 0.0,
    "PARTNERS_NON_SUPPLY": 0.0,
}


def filter_provenance(edges, docs):
    """Drop edges whose endpoints do not appear in their own source document.

    WARRANT: the extraction prompt forbids naming a company the document does
    not name, and the local 8B model ignores it. On the aggregate-claim document
    it read "four of the world's five top GPU producers" and emitted three
    concrete SUPPLIES edges, inventing part of the answer. This check enforces
    the rule mechanically and generically: an edge is only admitted if both of
    its endpoints' surface forms occur in the source text.

    It is answer-agnostic -- it never consults the planted truth, only each
    document against its own extracted edges -- and it is applied uniformly to
    every edge type. Returns (kept, dropped).
    """
    text = {d["id"]: d["text"].lower() + " " + d["title"].lower() for d in docs}

    def named(entity, doc_id):
        body = text.get(doc_id)
        if body is None:
            return True            # unknown document: do not silently drop
        e = CANON_LOOKUP.get(entity)
        forms = ([entity] + e["aliases"]) if e else [entity]
        for form in forms:
            f = str(form).lower()
            if f and f in body:
                return True
            # a distinctive first token is enough ("Tessara", "Veradyne")
            head = f.split()[0] if f.split() else ""
            if len(head) >= 5 and head in body:
                return True
        return False

    kept, dropped = [], []
    for e in edges:
        # Part and Platform names are normalised by shape, so only check the
        # Company endpoints, whose surface forms are what the register knows.
        ok = True
        if e["from_kind"] == "Company" and not named(e["from"], e["doc_id"]):
            ok = False
        if e["to_kind"] == "Company" and not named(e["to"], e["doc_id"]):
            ok = False
        (kept if ok else dropped).append(e)
    return kept, dropped


CANON_LOOKUP = {e["canon"]: e for e in common.ENTITIES}


def repair_supply_direction(edges):
    """Re-orient SUPPLIES edges that run GPUProducer -> MemorySupplier.

    WARRANT, and why this is a separate, labelled layer rather than a silent fix:
    the local 8B extractor inverts supply direction on "these were our tier 1
    suppliers" documents every single time, even when the prompt gives that exact
    sentence shape as a worked example. The strict (unrepaired) numbers are
    reported alongside the repaired ones so the cost of this layer is visible.

    The rule uses only company-register information -- which companies are
    memory suppliers and which are GPU producers -- and never any fact about who
    supplies whom. It is applied uniformly to every edge.

    Returns (repaired_edges, n_flipped).
    """
    out, flipped = [], 0
    for e in edges:
        e = dict(e)
        if (e["type"] == "SUPPLIES"
                and common.KIND.get(e["from"]) == "GPUProducer"
                and common.KIND.get(e["to"]) == "MemorySupplier"):
            e["from"], e["to"] = e["to"], e["from"]
            e["direction_repaired"] = True
            flipped += 1
        out.append(e)
    return out, flipped


def build(relations, repair_direction=False, docs=None):
    """Return (nodes, edges, claims) with normalised identities.

    docs: when given, the provenance check is applied (see filter_provenance).
    """
    nodes = {}     # (kind, name) -> props
    edges = []
    claims = []

    def node(kind, name):
        if not name:
            return None
        if kind == "Company":
            c = common.resolve(name) or str(name).strip()
            k = common.KIND.get(c, "UnknownCompany")
            nodes[("Company", c)] = {"kind": k}
            return c
        if kind == "Part":
            p = norm_part(name)
            if p:
                nodes[("Part", p)] = {}
            return p
        if kind == "Platform":
            p = norm_platform(name)
            if p:
                nodes[("Platform", p)] = {}
            return p
        return None

    for r in relations:
        t = r["type"]
        spec = EDGE_SPEC.get(t)
        if not spec:
            continue
        fk, tk = spec
        # QUALIFIED_ON is specified Part -> Platform, but the extractor
        # sometimes answers at company level ("Kestrel's stack qualified on
        # ..." -> from = Kestrel). Accept both shapes rather than silently
        # dropping the edge, and record which shape it was.
        if t == "QUALIFIED_ON" and common.resolve(r.get("from")):
            fk = "Company"
        f = node(fk, r.get("from"))
        if not f:
            continue
        if tk is None:
            claims.append({
                "type": t, "company": f, "doc_id": r["doc_id"],
                "props": r.get("props") or {}, "quote": r.get("quote", ""),
                "stance": r.get("stance", "asserted"),
            })
            continue
        to = node(tk, r.get("to"))
        if not to:
            continue
        if f == to:
            continue          # self-loops from sloppy extraction, dropped
        edges.append({
            "type": t, "from_kind": fk, "from": f, "to_kind": tk, "to": to,
            "customer_named": bool((r.get("props") or {}).get("customer_named", True)),
            "stance": r.get("stance", "asserted"),
            "doc_id": r["doc_id"], "doc_type": r.get("doc_type", ""),
            "quote": r.get("quote", ""),
        })

    # de-duplicate identical (type, from, to, doc) edges
    seen = set()
    uniq = []
    for e in edges:
        k = (e["type"], e["from"], e["to"], e["doc_id"])
        if k in seen:
            continue
        seen.add(k)
        uniq.append(e)

    # "label" is the node type (Company/Part/Platform); "kind" is the company
    # kind (GPUProducer/MemorySupplier/...). They must not share a key: an
    # earlier version spread the props over "kind" and silently relabelled every
    # Company node, which made every edge MERGE match nothing.
    dropped = []
    if docs:
        uniq, dropped = filter_provenance(uniq, docs)
    build.n_provenance_dropped = len(dropped)
    build.provenance_dropped = dropped

    n_flipped = 0
    if repair_direction:
        uniq, n_flipped = repair_supply_direction(uniq)

    node_list = [{"label": k, "name": n, **p} for (k, n), p in sorted(nodes.items())]
    build.n_direction_flipped = n_flipped
    return node_list, uniq, claims


def count_inverted_supply(edges):
    """How many SUPPLIES edges run GPUProducer -> MemorySupplier. Diagnostic."""
    return [e for e in edges
            if e["type"] == "SUPPLIES"
            and common.KIND.get(e["from"]) == "GPUProducer"
            and common.KIND.get(e["to"]) == "MemorySupplier"]
