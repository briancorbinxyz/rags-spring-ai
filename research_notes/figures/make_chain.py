#!/usr/bin/env python3
"""Episode 4's retrieve -> post-process -> prompt chain, and the exit it does not have.

The episode describes this chain in prose and has no figure of it at all. Drawn, the defect is
immediate: every stage takes a list and returns a list, so there is nowhere a decline could happen
even if a score asked for one.
"""
import json
from pathlib import Path

INK, GREY, RED, BLUE = "#1e1e1e", "#868e96", "#e03131", "#1971c2"
elements, texts, seq = [], [], [0]


def el(**kw):
    seq[0] += 1
    name = kw.pop("name", None) or f"el{seq[0]}"
    base = dict(id=name, angle=0, strokeColor=INK, backgroundColor="transparent",
                fillStyle="solid", strokeWidth=2, strokeStyle="solid", roughness=1, opacity=100,
                groupIds=[], frameId=None, roundness=None, seed=1000 + seq[0] * 37,
                version=1, versionNonce=2000 + seq[0] * 53, isDeleted=False,
                boundElements=[], updated=1, link=None, locked=False)
    base.update(kw)
    elements.append(base)
    return name


def text(body, x, y, size=16, colour=INK, align="left", width=None):
    lines = body.split("\n")
    w = width or int(max(len(l) for l in lines) * size * 0.58)
    name = el(type="text", x=x, y=y, width=w, height=int(len(lines) * size * 1.25),
              strokeColor=colour, text=body, fontSize=size, fontFamily=1, textAlign=align,
              verticalAlign="top", containerId=None, originalText=body, lineHeight=1.25)
    texts.append((body, name))
    return name


def box(x, y, w, h, colour=INK, dashed=False, fill="transparent"):
    return el(type="rectangle", x=x, y=y, width=w, height=h, strokeColor=colour,
              backgroundColor=fill, fillStyle="hachure",
              strokeStyle="dashed" if dashed else "solid", roundness={"type": 3})


def arrow(x, y, pts, colour=INK, dashed=False):
    xs = [0] + [p[0] for p in pts]
    ys = [0] + [p[1] for p in pts]
    return el(type="arrow", x=x, y=y, width=max(xs) - min(xs), height=max(ys) - min(ys),
              strokeColor=colour, strokeStyle="dashed" if dashed else "solid",
              points=[[0, 0]] + [list(p) for p in pts], lastCommittedPoint=None,
              startBinding=None, endBinding=None, startArrowhead=None, endArrowhead="arrow")


text("Every stage returns a list", 40, 24, size=30)
text("Episode 4's retrieval chain. The advisor runs the retriever, then each post-processor in\n"
     "turn, and puts what is left into the prompt. Nothing in it can return nothing.", 40, 66,
     size=15, colour=GREY)

ROW, W, H, GAP = 180, 196, 86, 58
stages = [("the question", "", BLUE),
          ("ElementRetriever", "top 4 units", INK),
          ("lifecycle filter", "a list, shorter", INK),
          ("supersession\nredirect", "a list, swapped", INK),
          ("the prompt", "always four", BLUE)]
for i, (label, under, colour) in enumerate(stages):
    x = 40 + i * (W + GAP)
    box(x, ROW, W, H, colour=colour)
    text(label, x + 16, ROW + (22 if "\n" not in label else 14), size=15, colour=colour)
    if under:
        text(under, x + 16, ROW + H + 12, size=13, colour=GREY)
    if i < len(stages) - 1:
        arrow(x + W + 10, ROW + H / 2, [[GAP - 20, 0]])

# the exit that is not there: one dashed stub under every stage, going nowhere
BR = ROW + 190
for i in range(1, 4):
    x = 40 + i * (W + GAP) + W / 2
    arrow(x, ROW + H + 34, [[0, 74]], colour=RED, dashed=True)
box(40 + (W + GAP), BR + 120, 3 * W + 2 * GAP - 56, 72, colour=RED, dashed=True)
text("return nothing", 40 + (W + GAP) + 24, BR + 136, size=16, colour=RED)
text("no stage has this exit, so no score could ever be acted on", 40 + (W + GAP) + 24, BR + 160,
     size=13, colour=GREY)

text("A list in, a list out — five times over. So there is no point at which this pipeline could\n"
     "decline, even if something told it to. That is not a bug in a stage. It is the shape of the\n"
     "chain, and it is why six of the fifty-three questions get four confident passages each.",
     40, BR + 240, size=15)

drawing = {"type": "excalidraw", "version": 2, "source": "https://excalidraw.com",
           "elements": elements,
           "appState": {"gridSize": None, "viewBackgroundColor": "#ffffff"}, "files": {}}
body = ["---", "", "excalidraw-plugin: parsed", "tags: [excalidraw]", "", "---",
        "==⚠  Switch to EXCALIDRAW VIEW in the MORE OPTIONS menu of this document. ⚠==", "",
        "", "# Excalidraw Data", "", "## Text Elements"]
for content, name in texts:
    body += [f"{content} ^{name}", ""]
body += ["%%", "## Drawing", "```json", json.dumps(drawing, indent=1), "```", "%%"]

out = Path("/Users/briancorbin/Documents/Brain/Library/03 Resources/attachments/"
           "RAGs to Riches Episode 4, every stage returns a list.excalidraw.md")
out.write_text("\n".join(body) + "\n", encoding="utf-8")
print(f"wrote {out.name}: {len(elements)} elements")
