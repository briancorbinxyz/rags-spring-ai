"""Episode 4 step 2: identifier lookup.

"FR-012 in spec 040" names an element id and a spec. The parser finds both; the lookup returns
the units with that id in that spec, which the pipeline pins above everything else.

- Element ids: any token shaped like an id (``FR-012``, ``NFR-001``, ``SC-002``, ``D-006``,
  ``T-100``, ``REQ-TOOL-001``, ``US5``, ``R-1``) that is also an anchor somewhere in the corpus.
  Case-sensitive: ids are written in capitals, and "d-001" in prose is not an id.
- Specs: "spec 040", "specs 040", "slice 040" in any case ("Spec 058's"), or a slug
  ("001-cli", "040-run-budget").
- An id with a spec pins that id in that spec (every file of the spec). An id with no spec pins
  only when the id exists in exactly one spec; otherwise ``FR-001`` alone would pin over a hundred
  units. A spec with no id pins nothing.
- Order among pinned units: current before history before excluded, then files outside
  ``changes/`` first, then corpus order.
"""

from __future__ import annotations

import json
import re
from collections import defaultdict
from pathlib import Path

ID_TOKEN = re.compile(r"(?<![\w-])(?P<prefix>[A-Z]+-)*[A-Z]+-?\d+(?![\w-])")
SPEC = re.compile(r"\b(?P<spec_word>spec|specs|slice)\s+(?P<in_prose>\d{3,})\b|(?<![\w-])(?P<in_slug>\d{3,})-[a-z]", re.IGNORECASE)

_LC_ORDER = {"current": 0, "history": 1}


def spec_of(key: str) -> str:
    """The number of the spec a unit key belongs to: specs/045-run-budget/... is "045".

    Read to the slug's first dash, not to a fixed three characters, so that a thousandth spec
    cannot read as the hundredth. The archive is three digits wide today.
    """
    return key.split("/")[1].split("-")[0]


def declared_names(root) -> dict:
    """The archive's own names for its specs: `specs/042-profile-enforcement/` declares both.

    A question rarely says "spec 042". It says "profile enforcement", and the folder name is the
    archive stating that those are the same thing — so this inventory is read, never guessed, and
    costs no labelling.

    One alias is dropped, and by declaration rather than by judgement: `spectastic.json` carries
    `"project": "spectastic/spectastic"`, and the project's own name is not a name for one spec.
    Measured, it would fire on about twenty questions and be right on one, because every question
    about the archive mentions the archive.

    What is deliberately *not* dropped is the generic slug. `001-cli` declares "cli", which occurs
    in 112 of the specs' own text, and `108-success-criteria` declares a phrase occurring in 117;
    those are vocabulary, not names, and measured they are where every wrong answer came from. The
    archive can separate them — the aliases that worked occur in 0 to 2 specs, the ones that failed
    in 11 to 117 — but turning that into a filter needs a cutoff, and ten questions cannot choose
    one. So the resolver stays inclusive and what it finds is **fused, never committed**: a wrong
    name costs ranking positions, not the answer.
    """
    root = Path(root)
    project = json.loads((root / "spectastic.json").read_text(encoding="utf-8"))["project"]
    own_name = project.rsplit("/", 1)[-1]
    names = {}
    for directory in sorted((root / "specs").iterdir()):
        number, _, slug = directory.name.partition("-")
        alias = slug.replace("-", " ")
        if directory.is_dir() and slug and alias != own_name:
            names[alias] = number
    return names


def parse(question: str, anchors: set, names: dict | None = None) -> tuple[list, list]:
    """(element ids, spec numbers) named in a question. ``anchors`` is the corpus's id set.

    ``names`` is the optional declared-alias inventory from :func:`declared_names`. It is read only
    when the question named no spec the literal way, so a question saying "spec 042" is parsed
    exactly as it was before this existed.
    """
    ids = []
    for m in ID_TOKEN.finditer(question):
        t = m.group(0)
        if t in anchors and t not in ids:
            ids.append(t)
    specs = []
    for m in SPEC.finditer(question):
        s = m["in_prose"] or m["in_slug"]  # "spec 040" in prose, or the "040-slug" form
        if s not in specs:
            specs.append(s)
    if not specs and names:
        lowered = question.lower()
        for alias in sorted(names, key=len, reverse=True):  # "core plan" before "plan"
            if re.search(rf"\b{re.escape(alias)}\b", lowered):
                specs.append(names[alias])
                break
    return ids, specs


class IdIndex:
    def __init__(self, units: list, names: dict | None = None):
        #: The archive's declared aliases, from :func:`declared_names`. Empty unless a run supplies
        #: them, so every configuration measured before this existed parses exactly as it did.
        self.names = names or {}
        self.units = units
        self.by_anchor = defaultdict(list)  # anchor -> unit keys in corpus order
        self.first_item = {}  # unit key -> item positions (all parts)
        for it in units:
            k = f"{it.source}#{it.anchor}"
            if k not in self.first_item:
                self.first_item[k] = []
                self.by_anchor[it.anchor].append(k)
            self.first_item[k].append(it.n)
        self.anchors = set(self.by_anchor)

    def lookup(self, question: str, linker: str = "ep4") -> list:
        """Unit keys to pin, in pin order. ``linker="prior"`` is the earlier prototype's rule
        (fixed id families, lower-case "spec NNN" only, an id with no spec pins every match)."""
        if linker == "prior":
            return self._prior(question)
        ids, specs = parse(question, self.anchors)
        out = []
        for i in ids:
            keys = self.by_anchor[i]
            if specs:
                keys = [k for k in keys if spec_of(k) in specs]
            elif len({spec_of(k) for k in keys}) != 1:
                keys = []
            out += [k for k in keys if k not in out]

        def order(k):
            it = self.units[self.first_item[k][0]]
            return (_LC_ORDER.get(it.lifecycle, 2), "/changes/" in it.source, it.n)

        return sorted(out, key=order)

    def items(self, key: str) -> list:
        return self.first_item.get(key, [])

    # The earlier prototype's linker, reproduced verbatim for P0-P3: three digits and a [:3] read
    # included. Fixing it here would stop it being a reproduction, so the fix lives above instead.
    _PRIOR_ID = re.compile(r"\b(?P<kind>FR|NFR|SC|D|T|US|REQ-[A-Z]+)-\d+\b")
    _PRIOR_SPEC = re.compile(r"\b(?P<spec_word>spec|specs|slice)\s+(?P<in_prose>\d{3})\b|\b(?P<in_slug>\d{3})-[a-z]")

    def _prior(self, question: str) -> list:
        found = [m[0] for m in self._PRIOR_ID.finditer(question)]  # whole ids, not the kind group
        specs = [m["in_prose"] or m["in_slug"] for m in self._PRIOR_SPEC.finditer(question)]
        keys = [k for k, ns in self.first_item.items()
                if self.units[ns[0]].anchor in found and (not specs or k.split("/")[1][:3] in specs)]
        keys.sort(key=lambda k: self.units[self.first_item[k][0]].n)
        return sorted(keys, key=lambda k: (self.units[self.first_item[k][0]].lifecycle != "current", "/changes/" in k))
