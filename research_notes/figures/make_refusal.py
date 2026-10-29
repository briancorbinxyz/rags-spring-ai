#!/usr/bin/env python3
"""The refusal branch, as an Excalidraw source the author can open and edit.

Every number on it is measured: the six unanswerable questions top out at 0.869, the answerable
questions start at 0.915, and none of the groundless answers scores above the median grounded one.
The branch itself is drawn dashed, because it is the one thing here that does not exist.
"""
import json
from pathlib import Path

INK, GREY, RED, GREEN, BLUE = "#1e1e1e", "#868e96", "#e03131", "#2f9e44", "#1971c2"
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


def rule(x, y, w, h, colour=GREY):
    return el(type="line", x=x, y=y, width=w, height=h, strokeColor=colour, strokeWidth=1,
              points=[[0, 0], [w, h]], lastCommittedPoint=None, startBinding=None,
              endBinding=None, startArrowhead=None, endArrowhead=None)


text("The decision the score makes available", 40, 24, size=30)
text("The tail of Episode 6's pipeline on the 5090. Everything solid is built and measured.\n"
     "The branch is dashed because it is the one thing here that does not exist.", 40, 66,
     size=15, colour=GREY)

ROW = 190
for i, (label, w) in enumerate([("fused ranking\n50 candidates", 170),
                                ("cross-encoder\nscores each one", 180)]):
    x = 40 + i * 230
    box(x, ROW, w, 76)
    text(label, x + 16, ROW + 20, size=15)
    arrow(x + w + 10, ROW + 38, [[40, 0]])

box(500, ROW, 150, 76, colour=BLUE)
text("the top score", 518, ROW + 30, size=15, colour=BLUE)
arrow(660, ROW + 38, [[40, 0]])

box(710, ROW - 6, 186, 88, colour=BLUE, dashed=True)
text("is it above\nthe threshold?", 730, ROW + 14, size=15, colour=BLUE)

arrow(900, ROW + 10, [[70, -58]], colour=GREEN)
text("yes", 918, ROW - 38, size=13, colour=GREEN)
box(980, ROW - 112, 250, 76, colour=GREEN)
text("serve four passages", 1000, ROW - 94, size=16, colour=GREEN)
text("what it does today, on everything", 1000, ROW - 70, size=13, colour=GREY)

arrow(900, ROW + 68, [[70, 58]], colour=RED, dashed=True)
text("no", 918, ROW + 94, size=13, colour=RED)
box(980, ROW + 100, 250, 76, colour=RED, dashed=True)
text('"Not in the archive."', 1000, ROW + 118, size=16, colour=RED)
text("never taken. nothing declines.", 1000, ROW + 142, size=13, colour=GREY)

AX = 440
text("Why the fork is possible at all", 40, AX - 46, size=20)
text("Top-1 score of the last ranking stage, frozen set, Qwen3-Reranker-8B.", 40, AX - 18,
     size=14, colour=GREY)

L, R, Y = 60, 1140, AX + 70
rule(L, Y, R - L, 0)


def at(score):
    return L + (R - L) * score


for score in (0.0, 0.25, 0.5, 0.75, 1.0):
    rule(at(score), Y - 6, 0, 12)
    text(f"{score:.2f}", at(score) - 16, Y + 12, size=12, colour=GREY)

box(at(0.106) - 4, Y - 58, at(0.869) - at(0.106) + 8, 44, colour=RED, fill="#ffe3e3")
text("the six that have no answer\n0.106 — 0.869", at(0.106) + 12, Y - 50, size=14, colour=RED)

box(at(0.915), Y - 58, at(0.9999) - at(0.915), 44, colour=GREEN, fill="#d3f9d8")
text("every answerable question\n0.915 — 0.9999", at(0.915) - 44, Y - 132, size=14,
     colour=GREEN, align="center", width=260)
arrow(at(0.957), Y - 96, [[0, 30]], colour=GREEN)

el(type="rectangle", x=at(0.869), y=Y - 70, width=at(0.915) - at(0.869), height=68,
   strokeColor=BLUE, backgroundColor="#d0ebff", fillStyle="cross-hatch", strokeStyle="dashed",
   strokeWidth=1, roughness=1, opacity=100, groupIds=[], frameId=None, roundness=None,
   seed=9931, version=1, versionNonce=9932, isDeleted=False, boundElements=[], updated=1,
   link=None, locked=False, angle=0, id="gapband")
text("the gap: 0.869 to 0.915", at(0.869) - 310, Y + 54, size=13, colour=BLUE)
text("a line anywhere in here refuses all six,\nand keeps every answerable question",
     at(0.869) - 310, Y + 74, size=13, colour=BLUE)
arrow(at(0.869) - 26, Y + 58, [[24, -54]], colour=BLUE)

B = Y + 150
text("And the number that says a line exists", 40, B, size=20)
text("groundless@4: questions whose top four holds no expected passage. How many of them score\n"
     "above the median grounded answer is what decides whether a threshold is buildable at all.",
     40, B + 30, size=14, colour=GREY)

text("groundless@4", 420, B + 70, size=13, colour=GREY)
text("of those, confident", 580, B + 70, size=13, colour=GREY)
rule(60, B + 92, 700, 0)
for i, (label, groundless, confident, colour) in enumerate(
        [("Episode 3, the naive attempt", "37 / 47", "24", RED),
         ("Episode 5, published", "9 / 53", "0", INK),
         ("Episode 6, step 12", "6 / 53", "0", GREEN)]):
    y = B + 104 + i * 36
    text(label, 60, y, size=15, colour=colour)
    text(groundless, 420, y, size=15, colour=colour)
    text(confident, 580, y, size=15, colour=colour)

text("The 6 is the floor: it is the six, and no amount of better retrieval moves it.\n"
     "The right-hand column is the one nobody set out to improve.", 60, B + 226, size=14,
     colour=GREY)

drawing = {"type": "excalidraw", "version": 2, "source": "https://excalidraw.com",
           "elements": elements,
           "appState": {"gridSize": None, "viewBackgroundColor": "#ffffff"},
           "files": {}}

body = ["---", "", "excalidraw-plugin: parsed", "tags: [excalidraw]", "", "---",
        "==⚠  Switch to EXCALIDRAW VIEW in the MORE OPTIONS menu of this document. ⚠==", "",
        "", "# Excalidraw Data", "", "## Text Elements"]
for content, name in texts:
    body.append(f"{content} ^{name}")
    body.append("")
body += ["%%", "## Drawing", "```json", json.dumps(drawing, indent=1), "```", "%%"]

out = Path("/Users/briancorbin/Documents/Brain/Library/03 Resources/attachments/"
           "RAGs to Riches Episode 6, the refusal branch.excalidraw.md")
out.write_text("\n".join(body) + "\n", encoding="utf-8")
print(f"wrote {out.name}: {len(elements)} elements, {len(texts)} labels")
