#!/usr/bin/env python3
"""Render the Excalidraw source to a plain SVG, so the geometry can be checked outside Obsidian."""
import json, re, html
from pathlib import Path

src = Path("/Users/briancorbin/Documents/Brain/Library/03 Resources/attachments/"
           "RAGs to Riches Episode 6, the refusal branch.excalidraw.md").read_text()
drawing = json.loads(re.search(r"```json\n(.*?)\n```", src, re.S).group(1))

pad = 30
xs, ys = [], []
for e in drawing["elements"]:
    xs += [e["x"], e["x"] + e.get("width", 0)]
    ys += [e["y"], e["y"] + e.get("height", 0)]
    for p in e.get("points", []):
        xs.append(e["x"] + p[0]); ys.append(e["y"] + p[1])
x0, y0, x1, y1 = min(xs) - pad, min(ys) - pad, max(xs) + pad, max(ys) + pad
true_h = y1 - y0
side = max(x1 - x0, y1 - y0)          # qlmanage squares SVG thumbnails and crops the overflow,
y0 -= (side - (y1 - y0)) / 2          # so hand it a square canvas and centre the drawing in it
y1 = y0 + side
x1 = x0 + side

out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{x1-x0:.0f}" height="{y1-y0:.0f}" '
       f'viewBox="{x0:.0f} {y0:.0f} {x1-x0:.0f} {y1-y0:.0f}">',
       f'<rect x="{x0:.0f}" y="{y0:.0f}" width="{x1-x0:.0f}" height="{y1-y0:.0f}" fill="#fff"/>',
       '<defs>']
for colour in {e["strokeColor"] for e in drawing["elements"] if e["type"] == "arrow"}:
    out.append(f'<marker id="a{colour[1:]}" markerWidth="9" markerHeight="7" refX="8" refY="3.5" '
               f'orient="auto"><polygon points="0 0, 9 3.5, 0 7" fill="{colour}"/></marker>')
out.append('</defs>')

for e in drawing["elements"]:
    dash = ' stroke-dasharray="7 5"' if e.get("strokeStyle") == "dashed" else ''
    c, sw = e["strokeColor"], e.get("strokeWidth", 2)
    if e["type"] == "rectangle":
        fill = e["backgroundColor"] if e["backgroundColor"] != "transparent" else "none"
        op = ' fill-opacity="0.45"' if fill != "none" else ''
        out.append(f'<rect x="{e["x"]:.0f}" y="{e["y"]:.0f}" width="{e["width"]:.0f}" '
                   f'height="{e["height"]:.0f}" rx="8" fill="{fill}"{op} stroke="{c}" '
                   f'stroke-width="{sw}"{dash}/>')
    elif e["type"] in ("line", "arrow"):
        pts = " ".join(f'{e["x"]+p[0]:.0f},{e["y"]+p[1]:.0f}' for p in e["points"])
        head = f' marker-end="url(#a{c[1:]})"' if e["type"] == "arrow" else ''
        out.append(f'<polyline points="{pts}" fill="none" stroke="{c}" '
                   f'stroke-width="{sw}"{dash}{head}/>')
    elif e["type"] == "text":
        size, lh = e["fontSize"], e["fontSize"] * 1.25
        for i, line in enumerate(e["text"].split("\n")):
            weight = "600" if size >= 20 else "400"
            out.append(f'<text x="{e["x"]:.0f}" y="{e["y"]+lh*(i+0.82):.0f}" fill="{c}" '
                       f'font-family="Helvetica, Arial, sans-serif" font-size="{size}" '
                       f'font-weight="{weight}">{html.escape(line)}</text>')
out.append('</svg>')

dest = Path("/private/tmp/claude-501/-Users-briancorbin-Code-rags-rags-spring-ai/"
            "4f24c815-84f0-4ab3-aafb-a3f513e16c8c/scratchpad/fig/refusal-branch.svg")
dest.write_text("\n".join(out), encoding="utf-8")
print("wrote", dest, f"({x1-x0:.0f} x {y1-y0:.0f})")
# the square canvas is only to survive qlmanage; record the real band so it can be cropped back
Path(dest.with_suffix(".crop")).write_text(f"{side:.0f} {(side - true_h) / 2:.0f} {true_h:.0f}")
