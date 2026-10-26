#!/usr/bin/env python3
"""The ratchet, run against the archive as it stood at each question's cutoff.

`run.py` indexes one snapshot, which is the right thing for every question that asks about the
present and the wrong thing for every question that asks about the past. This runs the same
configurations over a slice of `TimeCorpus` chosen per question, so a retrieved address is the right
*revision* by construction rather than by a caveat.

    tools/ratchet-proto/run_timeline.py --embedder ollama:nomic-embed-text:v1.5 \\
        --reranker cross-encoder/ms-marco-MiniLM-L-6-v2 --out results/time-slice

    tools/ratchet-proto/run_timeline.py --embedder llamacpp:qwen3-embedding-8b-q8_0 \\
        --reranker llamacpp:qwen3-reranker-8b --out results/5090-slice

Only the questions carrying a `cutoff` are scored, because only those have a slice to be scored
against. G-075 has an empty `expected` and no state at its cutoff: it is the abstention row, and it
counts as answered when no passage the golden set marks stale reaches the top four.
"""

from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
sys.path.insert(0, str(HERE))

from ratchet import embed as E  # noqa: E402
from ratchet import rerank as R  # noqa: E402
from ratchet import revisions as V  # noqa: E402
from ratchet import score as S  # noqa: E402
from ratchet import timecorpus as TC  # noqa: E402
from ratchet import timeline as TL  # noqa: E402
from ratchet import tok as T  # noqa: E402
from ratchet.pipeline import Config  # noqa: E402

DEPTH = 4


def configs() -> list[Config]:
    units = dict(items="units", corpus="ep4")
    return [
        Config("T1", "1 BM25 over the slice", **units),
        Config("T2", "2 + identifier lookup", ids=True, **units),
        Config("T3", "3 + standing as of then", ids=True, lifecycle=True, **units),
        Config("T4", "4 + dense, RRF k=60", ids=True, lifecycle=True, dense=True,
               needs=("dense",), **units),
        Config("T5", "5 + cross-encoder rerank", ids=True, lifecycle=True, dense=True, rerank=True,
               needs=("dense", "rerank"), **units),
        Config("T6", "variant: + the named spec admitted to the rerank window", ids=True,
               lifecycle=True, dense=True, rerank=True, scope=True, needs=("dense", "rerank"),
               **units),
        Config("T7", "variant: + the named spec ranked and fused, no reranker", ids=True,
               lifecycle=True, dense=True, scope_rrf=True, needs=("dense",), **units),
        Config("T9", "variant: the reranker as an RRF vote rather than the last word", ids=True,
               lifecycle=True, dense=True, rerank=True, rerank_rrf=True, scope_rrf=True,
               needs=("dense", "rerank"), **units),
        Config("TD", "6 + the named document, reranked only when nothing was named", ids=True,
               lifecycle=True, dense=True, rerank=True, scope_rrf=True, doc_scope=True,
               route_scope=True, needs=("dense", "rerank"), **units),
        Config("TE", "variant: TD reading a bare spec as its spec.html", ids=True, lifecycle=True,
               dense=True, rerank=True, scope_rrf=True, doc_scope=True, doc_scope_default=True,
               route_scope=True, needs=("dense", "rerank"), **units),
    ]


def abstains(question: dict, ranked: list) -> bool:
    """For a question with nothing to find: did the ranking avoid offering a wrong answer?

    Not a free pass. The snapshot corpus fails this on G-075 — it serves four passages from a spec
    first recorded two days after the cutoff, with the one the golden set marks stale third — and a
    slice passes it because that spec is not in the collection at all.
    """
    return not (set(question.get("stale") or []) & set(ranked[:DEPTH]))


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--spectastic", default=str(REPO.parent / "spectastic"))
    ap.add_argument("--golden", default=str(REPO / "src/main/resources/golden/questions-time.jsonl"))
    ap.add_argument("--embedder", default="wordllama")
    ap.add_argument("--reranker", default="none")
    ap.add_argument("--ollama-url", default="http://localhost:11434")
    ap.add_argument("--llamacpp-url", default="http://127.0.0.1:8090")
    ap.add_argument("--llamacpp-rerank-url", default="http://127.0.0.1:8091")
    ap.add_argument("--cache", default=str(HERE / "cache"))
    ap.add_argument("--cl100k-file", default=None)
    ap.add_argument("--only", default="")
    ap.add_argument("--out", default=str(HERE / "results/time-slice"))
    args = ap.parse_args()

    root = Path(args.spectastic).resolve()
    if not (root / "specs").is_dir():
        raise SystemExit(f"no spectastic checkout at {root}")
    history = V.GitHistory(root)
    if history.is_shallow():
        raise SystemExit(f"{root} is a shallow clone: git -C {root} fetch --unshallow")

    questions = [json.loads(l) for l in Path(args.golden).read_text(encoding="utf-8").splitlines()
                 if l.strip()]
    cutoffs = [q for q in questions if q.get("cutoff")]
    if not cutoffs:
        raise SystemExit(f"{args.golden} holds no question with a cutoff")

    cache = Path(args.cache)
    commit = next(iter({q["commit"] for q in questions}))
    print(f"building the bitemporal corpus at {commit[:7]}", file=sys.stderr)
    corpus = TC.load_or_build(root, cache, commit)
    print(f"{len(corpus.states):,} unit-states; {len(cutoffs)} questions with a cutoff",
          file=sys.stderr)

    enc = T.load(args.cl100k_file)
    emb = None
    if args.embedder != "none":
        emb = E.make(args.embedder, args.ollama_url, args.llamacpp_url, cache)
        emb.attach_cache(cache)
    rr, msg = R.make(args.reranker, args.llamacpp_rerank_url, cache)
    print(msg, file=sys.stderr)

    timeline = TL.Timeline(corpus, reranker=rr, embedder=emb)
    qvec = {q["id"]: emb.embed_queries([q["question"]])[0] for q in cutoffs} if emb else {}

    only = set(filter(None, args.only.split(",")))
    rows, summary = [], []
    for cfg in configs():
        if only and cfg.key not in only:
            continue
        if "dense" in cfg.needs and emb is None:
            print(f"{cfg.key} skipped: no embedder", file=sys.stderr)
            continue
        if "rerank" in cfg.needs and rr is None:
            print(f"{cfg.key} skipped: {msg}", file=sys.stderr)
            continue
        started, hits, out = time.time(), 0, []
        for q in cutoffs:
            at = history.at(q["cutoff"]).position
            items, pipe = timeline.at(at)
            final, top1, pinned = pipe.run(cfg, q["question"], qvec.get(q["id"]))
            ranked = [items[n].address for n in final]
            scored = S.rank_list([items[n] for n in final], q, S.unit_hit,
                                 lambda it: T.count(it.passage()), top1, pinned)
            answered = (bool(scored.first and scored.first <= DEPTH) if q["expected"]
                        else abstains(q, ranked))
            out.append({"config": cfg.key, "label": cfg.label,
                        "embedder": args.embedder, "reranker": args.reranker,
                        "id": scored.qid, "stratum": scored.stratum,
                        "cutoff": q["cutoff"], "position": at, "slice_units": len(items),
                        "expected": scored.expected, "ranks": scored.ranks,
                        "partial_ranks": scored.partial_ranks,
                        "stale_ranks": scored.stale_ranks, "first_hit": scored.first,
                        "pinned": scored.pinned, "top10": scored.top,
                        "context_tokens_top4": scored.context_tokens,
                        "top1_score": scored.top1_score,
                        "abstention": not q["expected"], "answered": answered})
            hits += answered
        rows += out
        summary.append((cfg.key, cfg.label, hits, len(cutoffs), time.time() - started))
        print(f"{cfg.key:4} {hits}/{len(cutoffs)}  {cfg.label}", file=sys.stderr)

    stem = Path(args.out)
    stem.parent.mkdir(parents=True, exist_ok=True)
    stem.with_suffix(".jsonl").write_text(
        "".join(json.dumps(r) + "\n" for r in rows), encoding="utf-8")
    lines = [f"# The slice ratchet: {args.embedder} / {args.reranker}", "",
             f"Run {time.strftime('%Y-%m-%d %H:%M')} · spectastic {commit[:7]} · "
             f"{len(corpus.states):,} unit-states · {len(cutoffs)} questions with a cutoff", "",
             "Each question is answered against the archive as it stood at its own cutoff, so a "
             "retrieved address is the right revision by construction. G-075 is the abstention "
             "row: answered when no passage the golden set marks stale reaches the top four.", "",
             "| key | configuration | answered |", "|---|---|---|"]
    for key, label, hits, total, _ in summary:
        lines.append(f"| {key} | {label} | **{hits}/{total}** |")
    lines += ["", "## Per question", "",
              "| question | " + " | ".join(k for k, *_ in summary) + " |",
              "|---" * (len(summary) + 1) + "|"]
    by = {(r["config"], r["id"]): r for r in rows}
    for q in cutoffs:
        cells = []
        for key, *_ in summary:
            r = by.get((key, q["id"]))
            cells.append("—" if r is None else
                         ("abstained" if r.get("abstention") and r["answered"] else
                          "**offered a stale answer**" if r.get("abstention") else
                          f"rank {r['first_hit']}" if r["answered"] else
                          f"({r['first_hit']})" if r.get("first_hit") else "miss"))
        lines.append(f"| {q['id']} {q['stratum']} | " + " | ".join(cells) + " |")
    stem.with_suffix(".md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {stem.with_suffix('.md')} and {stem.with_suffix('.jsonl')}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
