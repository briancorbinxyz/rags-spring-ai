# The two figures the refusal correction needed

Written 29 Oct 2026. These generate Excalidraw sources straight into the vault's `attachments/`
folder, in the same format as the drawings already there, so they can be opened and edited in
Obsidian rather than being flat images nobody can change.

| script | drawing |
|---|---|
| `make_chain.py` | **Episode 4 — every stage returns a list.** The retrieval chain the episode described only in prose. Episode 4 had no pipeline figure at all; its only image was the blind-audit screenshot. Drawn, the defect is immediate: a list in and a list out, five times over, with a dashed stub under each stage going nowhere. |
| `make_refusal.py` | **Episode 6 — the refusal branch.** The pipeline's tail forking into *serve four passages* or *decline*, the 0.869-to-0.915 gap on a score line, and the `groundless@4` trajectory underneath. The branch is dashed because it is the one thing on it that does not exist. |

Every number on them is measured, not illustrative: the six unanswerable questions top out at 0.869
and the answerable ones start at 0.915; `groundless@4` runs 37/47 → 9/53 → 6/53 across Episodes 3,
5 and 6, and the count above the median grounded answer runs 24 → 0 → 0.

`preview.py` renders a source to SVG so the geometry can be checked without opening Obsidian. It
pads the canvas to a square on purpose: `qlmanage`, the only rasteriser on this machine, squares SVG
thumbnails and crops whatever overflows, so a square in means nothing is lost. It writes a `.crop`
file alongside, holding the band to crop back to with `sips`.

    python3 make_refusal.py && python3 preview.py

To re-export the PNGs properly, open the drawing in Obsidian and use Excalidraw's own export — it
will be sharper than the thumbnailer's.
