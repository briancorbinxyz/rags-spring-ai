#!/usr/bin/env python3
"""Builds the blind audit page for passages a search returned that nobody has judged.

Episode 4's method, applied to whatever runs you point it at:

- **Every miss, not just the interesting ones.** For a question a run missed, every unjudged
  passage in its top four becomes a candidate.
- **Only the ones that matter.** A run that already has every expected passage in the top four has
  full marks on that question, and nothing else in its top four can change its score, so those are
  skipped (``--all`` keeps them: judging them cannot change Hit@4, but it is the only way to raise
  Judged@4).
- **Blind.** A candidate sits next to the question, the reference answer and the expected passage,
  and nothing else. Nothing says which search, model or rank found it, and candidates are listed
  alphabetically inside a question so the order leaks nothing either.

    tools/audit/build_audit.py --runs <prototype>.jsonl [...] --round 3 --out build/audit/round3.html

Decisions come back as the same JSON Lines Episode 4 wrote, one object per decision:
``{"id", "candidate", "round", "decision", "note"}`` with ``whole``, ``standin``, ``partly`` or
``no`` — the shape ``TrecFiles.readAudit`` reads. ``partly`` counts for nothing, deliberately.
"""

from __future__ import annotations

import argparse
import collections
import glob
import html
import json
from pathlib import Path

DECISIONS = [
    ("whole", "1", "Answers the question", "Counts as a hit, as if it were expected"),
    ("standin", "2", "Stands in for an expected passage", "Counts in place of that passage"),
    ("partly", "3", "Partly, not enough", "Counts for nothing"),
    ("no", "4", "Doesn't answer it", "Counts for nothing"),
]


def load_golden(path: Path) -> dict:
    return {d["id"]: d for d in (json.loads(l) for l in path.read_text(encoding="utf-8").splitlines() if l.strip())}


def load_qrels(path: Path) -> dict:
    judged = collections.defaultdict(set)
    for line in path.read_text(encoding="utf-8").splitlines():
        parts = line.split()
        if len(parts) >= 4:
            judged[parts[0]].add(parts[2])
    return judged


def load_passages(cache_glob: str) -> dict:
    """address -> the passage text a reader would get, parts joined in order."""
    files = sorted(glob.glob(cache_glob))
    if not files:
        raise SystemExit(f"no corpus cache matching {cache_glob}: run the prototype once to build it")
    corpus = json.loads(Path(files[0]).read_text(encoding="utf-8"))
    by = collections.defaultdict(list)
    for unit in corpus["units"]:
        by[f"{unit['source']}#{unit['anchor']}"].append(unit)
    out = {}
    for address, units in by.items():
        units.sort(key=lambda u: u.get("part") or 0)
        first = units[0]
        out[address] = {
            "path": first.get("path") or "",
            "heading": first.get("heading") or "",
            "lifecycle": first.get("lifecycle") or "",
            "text": "\n\n".join(u.get("text") or "" for u in units).strip(),
            "parts": len(units),
        }
    return out


def candidates(runs: list, golden: dict, judged: dict, keep_full: bool) -> dict:
    found = collections.defaultdict(set)
    skipped = 0
    for row in runs:
        question = golden.get(row["id"])
        if not question or question["stratum"] == "unanswerable":
            continue
        ranks = row.get("ranks") or []
        if not keep_full and ranks and all(r is not None and r <= 4 for r in ranks):
            skipped += 1  # full marks already: nothing else in its top four can change the score
            continue
        for address in (row.get("top10") or [])[:4]:
            if address not in judged[row["id"]]:
                found[row["id"]].add(address)
    print(f"  run-question pairs with full marks, skipped: {skipped}")
    return {q: sorted(v) for q, v in sorted(found.items())}


def page(items: list, round_number: int) -> str:
    """One self-contained page. The data is inlined; nothing is fetched."""
    data = json.dumps(items, ensure_ascii=False)
    keys = "".join(
        f'<div class="key"><kbd>{k}</kbd><div><b>{label}</b><small>{effect}</small></div></div>'
        for _, k, label, effect in DECISIONS
    )
    return f"""<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Blind audit, round {round_number}</title>
<style>
  :root {{
    --bg: #fbfaf8; --fg: #1a1a1a; --muted: #6b6b6b; --line: #e3e0da;
    --card: #ffffff; --accent: #8a5a2b; --mark: #fdf2c0; --shadow: 0 1px 3px rgba(0,0,0,.06);
  }}
  @media (prefers-color-scheme: dark) {{
    :root:not([data-theme="light"]) {{
      --bg: #17171a; --fg: #ececec; --muted: #9a9a9a; --line: #2e2e33;
      --card: #1f1f23; --accent: #d7a06a; --mark: #4a3f1a; --shadow: none;
    }}
  }}
  :root[data-theme="dark"] {{
    --bg: #17171a; --fg: #ececec; --muted: #9a9a9a; --line: #2e2e33;
    --card: #1f1f23; --accent: #d7a06a; --mark: #4a3f1a; --shadow: none;
  }}
  * {{ box-sizing: border-box; }}
  body {{ margin: 0; background: var(--bg); color: var(--fg);
    font: 16px/1.6 ui-sans-serif, -apple-system, "Segoe UI", Roboto, sans-serif; }}
  .wrap {{ max-width: 54rem; margin: 0 auto; padding: 1.5rem 1.25rem 6rem; }}
  .bar {{ position: sticky; top: 0; background: var(--bg); border-bottom: 1px solid var(--line);
    padding: .75rem 0; display: flex; gap: 1rem; align-items: baseline; flex-wrap: wrap; z-index: 5; }}
  .bar b {{ font-size: 1rem; }}
  .bar small {{ color: var(--muted); }}
  .track {{ flex: 1 1 8rem; height: 6px; background: var(--line); border-radius: 3px; overflow: hidden; min-width: 6rem; }}
  .track div {{ height: 100%; background: var(--accent); width: 0; transition: width .2s; }}
  .card {{ background: var(--card); border: 1px solid var(--line); border-radius: 10px;
    padding: 1rem 1.1rem; margin: 1rem 0; box-shadow: var(--shadow); }}
  .label {{ font-size: .72rem; letter-spacing: .09em; text-transform: uppercase; color: var(--muted); margin-bottom: .35rem; }}
  .q {{ font-size: 1.1rem; font-weight: 600; }}
  .ask {{ flex: 1 0 100%; max-height: 34vh; overflow: auto; margin-top: .55rem;
    padding-top: .55rem; border-top: 1px solid var(--line); }}
  .ask .q {{ font-size: 1rem; }}
  .ask .core {{ color: var(--muted); font-size: .93rem; margin-top: .3rem; }}
  .addr {{ font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: .82rem;
    color: var(--accent); word-break: break-all; }}
  .passage {{ white-space: pre-wrap; max-height: 24rem; overflow-y: auto; }}
  mark {{ background: var(--mark); color: inherit; padding: 0 .1em; border-radius: 2px; }}
  .key {{ display: flex; gap: .7rem; align-items: center; padding: .35rem 0; }}
  .key kbd {{ border: 1px solid var(--line); border-bottom-width: 2px; border-radius: 5px;
    padding: .15rem .5rem; font-family: inherit; min-width: 1.6rem; text-align: center; background: var(--bg); }}
  .key small {{ display: block; color: var(--muted); }}
  .standin button {{ display: block; width: 100%; text-align: left; margin: .3rem 0; padding: .5rem .6rem;
    border: 1px solid var(--line); border-radius: 6px; background: var(--bg); color: var(--fg); cursor: pointer;
    font: inherit; }}
  .standin button:hover {{ border-color: var(--accent); }}
  textarea {{ width: 100%; min-height: 3rem; background: var(--bg); color: var(--fg);
    border: 1px solid var(--line); border-radius: 6px; padding: .5rem; font: inherit; }}
  .done {{ text-align: center; padding: 2rem 0; }}
  button.primary {{ background: var(--accent); color: var(--bg); border: 0; border-radius: 7px;
    padding: .6rem 1.1rem; font: inherit; font-weight: 600; cursor: pointer; }}
  pre.out {{ background: var(--card); border: 1px solid var(--line); border-radius: 8px;
    padding: .8rem; overflow-x: auto; font-size: .78rem; max-height: 20rem; text-align: left; }}
  .hint {{ color: var(--muted); font-size: .85rem; }}
</style>
<div class="wrap">
  <div class="bar">
    <b id="count"></b><div class="track"><div id="fill"></div></div>
    <small id="where"></small>
    <small class="hint">u undo · s skip</small>
    <button id="sofar" class="primary" style="margin-left:auto">Download so far</button>
    <div class="ask" id="ask"></div>
  </div>
  <div id="app"></div>
</div>
<script>
const ITEMS = {data};
const ROUND = {round_number};
const DECIDED = [];
let at = 0;

/** Judgements are hand work: they survive a reload, and can be taken out part-way. */
const KEY = "blind-audit-" + ROUND + "-" + ITEMS.length;
function remember() {{
  try {{ localStorage.setItem(KEY, JSON.stringify({{at: at, decided: DECIDED}})); }} catch (e) {{}}
}}
(function recall() {{
  try {{
    const saved = JSON.parse(localStorage.getItem(KEY) || "null");
    if (saved && Array.isArray(saved.decided) && saved.decided.length) {{
      DECIDED.push(...saved.decided);
      at = Math.min(saved.at || 0, ITEMS.length);
    }}
  }} catch (e) {{}}
}})();

function asJsonl() {{ return DECIDED.map(d => JSON.stringify(d)).join("\\n") + "\\n"; }}
function download() {{
  const a = document.createElement("a");
  a.href = URL.createObjectURL(new Blob([asJsonl()], {{type: "application/json"}}));
  a.download = `golden-audit-round${{ROUND}}.jsonl`;
  a.click();
}}

const esc = s => String(s ?? "").replace(/[&<>]/g, c => ({{"&":"&amp;","<":"&lt;",">":"&gt;"}}[c]));

/** The reference answer's words, highlighted in the passage: the audit's reading aid. */
function highlight(text, core) {{
  let out = esc(text);
  const words = String(core || "").toLowerCase().match(/[a-z0-9][a-z0-9._-]{{3,}}/g) || [];
  for (const w of [...new Set(words)].sort((a, b) => b.length - a.length).slice(0, 24)) {{
    out = out.replace(new RegExp("(?<![\\\\w>])(" + w.replace(/[.*+?^${{}}()|[\\]\\\\]/g, "\\\\$&") + ")(?![\\\\w<])", "gi"),
                      "<mark>$1</mark>");
  }}
  return out;
}}

function render() {{
  const app = document.getElementById("app");
  document.getElementById("count").textContent = at < ITEMS.length ? `${{at + 1}} of ${{ITEMS.length}}` : "done";
  document.getElementById("fill").style.width = (100 * at / ITEMS.length) + "%";
  if (at >= ITEMS.length) {{
    document.getElementById("where").textContent = "";
    document.getElementById("ask").innerHTML = "";
    app.innerHTML = `<div class="done"><h2>All ${{ITEMS.length}} judged.</h2>
      <p class="hint">Save this as <code>golden-audit-round${{ROUND}}.jsonl</code> in <code>results/</code>.</p>
      <p><button class="primary" id="dl">Download .jsonl</button>
         <button class="primary" id="cp">Copy</button></p>
      <pre class="out" id="out"></pre></div>`;
    const text = asJsonl();
    document.getElementById("out").textContent = text;
    document.getElementById("cp").onclick = () => navigator.clipboard.writeText(text);
    document.getElementById("dl").onclick = download;
    return;
  }}
  const it = ITEMS[at];
  document.getElementById("where").textContent = `${{it.stratum}}`;
  document.getElementById("ask").innerHTML =
    `<div class="q">${{esc(it.question)}}</div>` +
    (it.core ? `<div class="core">${{esc(it.core)}}</div>` : "");
  app.innerHTML = `
    <div class="card">
      <div class="label">Expected passage${{it.expected.length > 1 ? "s" : ""}}</div>
      ${{(it.expected_full || []).map(e =>
          `<div class="addr">${{esc(e.address)}}</div>` +
          (e.text ? `<div class="passage">${{highlight(e.text, it.core)}}</div>` : "")
        ).join("")}}
    </div>
    <div class="card">
      <div class="label">Candidate — does this passage answer the question?</div>
      <div class="addr">${{esc(it.candidate)}}</div>
      <div class="hint" style="margin:.3rem 0 .7rem">${{esc(it.path)}}${{it.parts > 1 ? ` · ${{it.parts}} parts` : ""}}${{it.lifecycle && it.lifecycle !== "current" ? ` · ${{esc(it.lifecycle)}}` : ""}}</div>
      <div class="passage">${{highlight(it.text, it.core)}}</div>
    </div>
    <div class="card" id="choice">{keys}</div>
    <div class="card"><div class="label">Note (optional)</div><textarea id="note"></textarea></div>`;
}}

function record(decision, forWhich) {{
  const it = ITEMS[at];
  const note = (document.getElementById("note")?.value || "").trim();
  const row = {{id: it.id, candidate: it.candidate, round: ROUND, decision: decision}};
  if (forWhich) row.for = forWhich;
  if (note) row.note = note;
  DECIDED.push(row);
  at++;
  remember();
  render();
}}

/** standin needs to say which expected passage it stands in for; one expected needs no question. */
function standin() {{
  const it = ITEMS[at];
  if (it.expected.length === 1) return record("standin", it.expected[0]);
  document.getElementById("choice").innerHTML =
    `<div class="label">Stands in for which expected passage?</div><div class="standin">` +
    (it.expected_full || []).map((e, i) =>
      `<button data-e="${{esc(e.address)}}"><kbd>${{i + 1}}</kbd> ${{esc(e.address)}}` +
      (e.text ? `<small style="display:block;margin-top:.35rem;color:var(--muted);font-weight:400">${{esc(e.text.slice(0, 500))}}${{e.text.length > 500 ? "…" : ""}}</small>` : "") +
      `</button>`).join("") +
    `</div>`;
  document.querySelectorAll(".standin button").forEach(b =>
    b.onclick = () => record("standin", b.dataset.e));
}}

document.addEventListener("keydown", e => {{
  if (e.target.tagName === "TEXTAREA" || at >= ITEMS.length) return;
  if (e.key === "1") record("whole");
  else if (e.key === "2") standin();
  else if (e.key === "3") record("partly");   // counts for nothing, on purpose
  else if (e.key === "4") record("no");
  else if (e.key === "s") {{ at++; remember(); render(); }}
  else if (e.key === "u" && DECIDED.length) {{ DECIDED.pop(); at--; remember(); render(); }}
}});
document.getElementById("sofar").onclick = download;
render();
</script>
"""


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--runs", nargs="+", required=True, help="prototype .jsonl run files")
    ap.add_argument("--configs", default="", help="comma-separated config keys (default: all in the files)")
    ap.add_argument("--golden", default="src/main/resources/golden/questions.jsonl")
    ap.add_argument("--qrels", default="src/main/resources/golden/qrels.txt")
    ap.add_argument("--corpus", default="tools/ratchet-proto/cache/corpus-*units-v1-ep4-p250.json")
    ap.add_argument("--round", type=int, required=True, help="the audit round, written into every decision")
    ap.add_argument("--done", nargs="*", default=[],
                    help="decision files already written: those candidates are left out of the pack")
    ap.add_argument("--all", action="store_true",
                    help="keep questions a run already has full marks on (raises Judged@4, cannot change Hit@4)")
    ap.add_argument("--out", default="build/audit/audit.html")
    args = ap.parse_args()

    golden = load_golden(Path(args.golden))
    judged = load_qrels(Path(args.qrels))
    passages = load_passages(args.corpus)
    wanted = {c for c in args.configs.split(",") if c} or None

    rows = []
    for path in args.runs:
        for line in Path(path).read_text(encoding="utf-8").splitlines():
            if line.strip():
                row = json.loads(line)
                if wanted is None or row.get("config") in wanted:
                    rows.append(row)
    print(f"read {len(rows)} run-question rows from {len(args.runs)} file(s)")

    found = candidates(rows, golden, judged, args.all)

    already = set()
    for path in args.done:
        for line in Path(path).read_text(encoding="utf-8").splitlines():
            if line.strip():
                decision = json.loads(line)
                already.add((decision["id"], decision["candidate"]))
    if already:
        kept = {q: [a for a in v if (q, a) not in already] for q, v in found.items()}
        found = {q: v for q, v in kept.items() if v}
        print(f"  already judged, left out of this pack: {len(already)}")

    items = []
    for qid, addresses in found.items():
        q = golden[qid]
        for address in addresses:  # alphabetical: the order says nothing about rank or search
            p = passages.get(address, {})
            items.append({
                "id": qid, "stratum": q["stratum"], "question": q["question"],
                "core": q.get("core") or "", "expected": q.get("expected") or [],
                # A stand-in decision is "this one does the job of that one", which cannot be made
                # against an address. The expected passage travels with its text.
                "expected_full": [
                    {"address": e,
                     "text": (passages.get(e) or {}).get("text", "(passage text not in the corpus cache)"),
                     "path": (passages.get(e) or {}).get("path", ""),
                     "lifecycle": (passages.get(e) or {}).get("lifecycle", "")}
                    for e in (q.get("expected") or [])
                ],
                "candidate": address, "path": p.get("path", ""), "lifecycle": p.get("lifecycle", ""),
                "parts": p.get("parts", 1), "text": p.get("text", "(passage text not in the corpus cache)"),
            })
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(page(items, args.round), encoding="utf-8")
    print(f"  candidates to judge: {len(items)} over {len(found)} questions")
    by = collections.Counter(i["stratum"] for i in items)
    for s, n in by.most_common():
        print(f"    {s:12} {n}")
    print(f"wrote {out}")


if __name__ == "__main__":
    main()
