# Rescore with the frozen golden set

## Decisions read from the workbook

- Sign-off: 45 OK, 2 Fix, 3 Drop, 0 blank (of 50 golden)
- Audit: 745 rows, 226 decided, 519 undecided (undecided = not equivalent)
- Counted equivalents: 14 across 12 questions: G-001 (1), G-007 (1), G-008 (1), G-010 (1), G-012 (1), G-016 (1), G-017 (2), G-032 (1), G-033 (1), G-036 (1), G-037 (2), G-044 (1)
- Dropped (removed from every count): G-005, G-035, G-040
- Fixes flagged (scored with the current expected elements until applied):
    G-031: Whilst it adds to the quality and citability of the answer, the question asks for the spec, not necessarily the functional requirement within the spec. Therefore answers that omit the FR-003 reference should be acceptable.
    G-036: (no text)
- partial answers with no expected element (not counted):
    G-002: specs/027-git-trailers/verify.html#run
    G-002: specs/027-git-trailers/spec.html#SC-001
    G-008: specs/006-kernel-extraction/design.html#technical-context
    G-008: specs/006-kernel-extraction/spec.html#SC-002
    G-012: specs/105-design-source-import/spec.html#US2
    G-013: specs/109-prose-theme/spec.html#scenarios
    G-013: specs/109-prose-theme/spec.html#FR-007
    G-014: specs/059-corpus-retrieval-scale/spec.html#changelog
    G-015: specs/058-corpus-licensing/spec.html#context
    G-015: specs/058-corpus-licensing/spec.html#scenarios
    G-016: specs/108-success-criteria/spec.html#scenarios
    G-029: specs/109-prose-theme/tasks.html#T-301
    G-030: specs/110-visual-one-step/spec.html#SC-002
    G-032: specs/055-corpus-in-review/spec.html#FR-001
    G-033: specs/024-explore-restore/spec.html#requirements
    G-036: specs/076-contract-export-handover/design.html#grounding
    G-036: specs/071-contract-promotion/triage-log.html#T-001
    G-036: specs/076-contract-export-handover/design.html#project-structure
    G-037: specs/004-npm-publish-workflow/tasks.html#T-1403
    G-037: specs/004-npm-publish-workflow/spec.html#FR-006

## 5090-nomic: llamacpp:nomic-embed-text-v1.5-f16 + llamacpp:bge-reranker-v2-m3-fp16

| config | Hit@4 | all@4 | single-fact | paraphrase | identifier | relation | stale | stale@4 | changed questions |
|---|---|---|---|---|---|---|---|---|---|
| S1 | 27→29 /41 | 22→24 | 8→8 /9 | 2→2 /10 | 6→6 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S2 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S3 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 1→1 | G-037 +hit, G-044 +hit |
| S4 | 32→34 /41 | 30→32 | 9→9 /9 | 4→6 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-017 +hit |
| S5 | 32→32 /41 | 29→29 | 8→8 /9 | 5→5 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 2→2 | – |
| X1 | 31→31 /41 | 26→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | – |
| X2 | 32→32 /41 | 28→28 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 7→7 /7 | 7→7 /7 | 0→0 | – |
| H1 | 32→34 /41 | 30→32 | 9→9 /9 | 4→6 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-017 +hit |
| H2 | 33→35 /41 | 32→34 | 9→9 /9 | 4→6 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-017 +hit |
| E5-1 | 32→32 /41 | 29→29 | 8→8 /9 | 5→5 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 1→1 | – |
| E5-2 | 34→34 /41 | 31→31 | 8→8 /9 | 5→5 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 1→1 | – |

**Ratchet verdicts (before → after)**

- S3→S4: FAIL (relation 6→4)  →  FAIL (relation 6→4)
- S4→S5: FAIL (all@4 30→29; stale@4 0→2)  →  FAIL (Hit@4 34→32; all@4 32→29; stale@4 0→2)
- X2→H2: FAIL (relation 7→5)  →  FAIL (relation 7→5)
- X2→E5-2: FAIL (stale@4 0→1)  →  FAIL (stale@4 0→1)
- H2→E5-2: FAIL (all@4 32→31; stale@4 0→1)  →  FAIL (Hit@4 35→34; all@4 34→31; stale@4 0→1)

## 5090-qwen3-emb-4b: llamacpp:qwen3-embedding-4b-q8_0 + llamacpp:bge-reranker-v2-m3-fp16

| config | Hit@4 | all@4 | single-fact | paraphrase | identifier | relation | stale | stale@4 | changed questions |
|---|---|---|---|---|---|---|---|---|---|
| S1 | 27→29 /41 | 22→24 | 8→8 /9 | 2→2 /10 | 6→6 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S2 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S3 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 1→1 | G-037 +hit, G-044 +hit |
| S4 | 32→33 /41 | 29→31 | 8→8 /9 | 4→5 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 1→1 | G-016 +hit, G-032 +all |
| S5 | 31→31 /41 | 28→28 | 8→8 /9 | 4→4 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 2→2 | – |
| X1 | 31→31 /41 | 26→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | – |
| X2 | 32→32 /41 | 28→28 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 7→7 /7 | 7→7 /7 | 0→0 | – |
| H1 | 32→33 /41 | 29→31 | 8→8 /9 | 4→5 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-032 +all |
| H2 | 33→34 /41 | 31→33 | 8→8 /9 | 4→5 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-032 +all |
| E5-1 | 31→31 /41 | 28→28 | 8→8 /9 | 4→4 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 1→1 | – |
| E5-2 | 33→33 /41 | 30→30 | 8→8 /9 | 4→4 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 1→1 | – |

**Ratchet verdicts (before → after)**

- S3→S4: pass  →  pass
- S4→S5: FAIL (Hit@4 32→31; all@4 29→28; stale@4 1→2)  →  FAIL (Hit@4 33→31; all@4 31→28; stale@4 1→2)
- X2→H2: pass  →  pass
- X2→E5-2: FAIL (stale@4 0→1)  →  FAIL (stale@4 0→1)
- H2→E5-2: FAIL (all@4 31→30; stale@4 0→1)  →  FAIL (Hit@4 34→33; all@4 33→30; stale@4 0→1)

## 5090-qwen3-emb-8b-qwen3-rr: llamacpp:qwen3-embedding-8b-q8_0 + llamacpp:qwen3-reranker-8b-q8_0

| config | Hit@4 | all@4 | single-fact | paraphrase | identifier | relation | stale | stale@4 | changed questions |
|---|---|---|---|---|---|---|---|---|---|
| S1 | 27→29 /41 | 22→24 | 8→8 /9 | 2→2 /10 | 6→6 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S2 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S3 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 1→1 | G-037 +hit, G-044 +hit |
| S4 | 33→34 /41 | 30→32 | 9→9 /9 | 4→5 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 1→1 | G-016 +hit, G-032 +all |
| S5 | 37→39 /41 | 34→36 | 9→9 /9 | 10→10 /10 | 8→8 /8 | 4→5 /7 | 6→7 /7 | 1→1 | G-012 +all, G-036 +hit, G-037 +hit |
| X1 | 31→31 /41 | 26→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | – |
| X2 | 32→32 /41 | 28→28 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 7→7 /7 | 7→7 /7 | 0→0 | – |
| H1 | 33→34 /41 | 30→32 | 9→9 /9 | 4→5 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-032 +all |
| H2 | 34→35 /41 | 32→34 | 9→9 /9 | 4→5 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-032 +all |
| E5-1 | 37→39 /41 | 34→36 | 9→9 /9 | 10→10 /10 | 8→8 /8 | 4→5 /7 | 6→7 /7 | 1→1 | G-012 +all, G-036 +hit, G-037 +hit |
| E5-2 | 39→41 /41 | 36→38 | 9→9 /9 | 10→10 /10 | 8→8 /8 | 6→7 /7 | 6→7 /7 | 1→1 | G-012 +all, G-036 +hit, G-037 +hit |

**Ratchet verdicts (before → after)**

- S3→S4: pass  →  pass
- S4→S5: pass  →  pass
- X2→H2: pass  →  pass
- X2→E5-2: FAIL (stale@4 0→1)  →  FAIL (stale@4 0→1)
- H2→E5-2: FAIL (stale@4 0→1)  →  FAIL (stale@4 0→1)

## 5090-qwen3-emb-8b: llamacpp:qwen3-embedding-8b-q8_0 + llamacpp:bge-reranker-v2-m3-fp16

| config | Hit@4 | all@4 | single-fact | paraphrase | identifier | relation | stale | stale@4 | changed questions |
|---|---|---|---|---|---|---|---|---|---|
| S1 | 27→29 /41 | 22→24 | 8→8 /9 | 2→2 /10 | 6→6 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S2 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S3 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 1→1 | G-037 +hit, G-044 +hit |
| S4 | 33→34 /41 | 30→32 | 9→9 /9 | 4→5 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 1→1 | G-016 +hit, G-032 +all |
| S5 | 31→31 /41 | 28→28 | 8→8 /9 | 4→4 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 2→2 | – |
| X1 | 31→31 /41 | 26→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | – |
| X2 | 32→32 /41 | 28→28 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 7→7 /7 | 7→7 /7 | 0→0 | – |
| H1 | 33→34 /41 | 30→32 | 9→9 /9 | 4→5 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-032 +all |
| H2 | 34→35 /41 | 32→34 | 9→9 /9 | 4→5 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-032 +all |
| E5-1 | 31→31 /41 | 28→28 | 8→8 /9 | 4→4 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 1→1 | – |
| E5-2 | 33→33 /41 | 30→30 | 8→8 /9 | 4→4 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 1→1 | – |

**Ratchet verdicts (before → after)**

- S3→S4: pass  →  pass
- S4→S5: FAIL (Hit@4 33→31; all@4 30→28; stale@4 1→2)  →  FAIL (Hit@4 34→31; all@4 32→28; stale@4 1→2)
- X2→H2: pass  →  pass
- X2→E5-2: FAIL (stale@4 0→1)  →  FAIL (stale@4 0→1)
- H2→E5-2: FAIL (Hit@4 34→33; all@4 32→30; stale@4 0→1)  →  FAIL (Hit@4 35→33; all@4 34→30; stale@4 0→1)

## mac-nomic-embed-text: ollama:nomic-embed-text:v1.5 + cross-encoder/ms-marco-MiniLM-L-6-v2

| config | Hit@4 | all@4 | single-fact | paraphrase | identifier | relation | stale | stale@4 | changed questions |
|---|---|---|---|---|---|---|---|---|---|
| S1 | 27→29 /41 | 22→24 | 8→8 /9 | 2→2 /10 | 6→6 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S2 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S3 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 1→1 | G-037 +hit, G-044 +hit |
| S4 | 32→34 /41 | 30→32 | 9→9 /9 | 4→6 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-017 +hit |
| S5 | 30→30 /41 | 26→27 | 7→7 /9 | 3→3 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 1→1 | G-032 +all |
| X1 | 31→31 /41 | 26→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | – |
| X2 | 32→32 /41 | 28→28 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 7→7 /7 | 7→7 /7 | 0→0 | – |
| E5-1 | 30→30 /41 | 26→27 | 7→7 /9 | 3→3 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 0→0 | G-032 +all |
| E5-2 | 31→31 /41 | 28→29 | 7→7 /9 | 3→3 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | G-032 +all |

**Ratchet verdicts (before → after)**

- S3→S4: FAIL (relation 6→4)  →  FAIL (relation 6→4)
- S4→S5: FAIL (Hit@4 32→30; all@4 30→26; single-fact 9→7; stale@4 0→1)  →  FAIL (Hit@4 34→30; all@4 32→27; single-fact 9→7; paraphrase 6→3; stale@4 0→1)
- X2→H2: n/a (run lacks H2)
- X2→E5-2: FAIL (Hit@4 32→31)  →  FAIL (Hit@4 32→31)
- H2→E5-2: n/a (run lacks H2)

## mac-nomic-hybrid: ollama:nomic-embed-text:v1.5 + no reranker

| config | Hit@4 | all@4 | single-fact | paraphrase | identifier | relation | stale | stale@4 | changed questions |
|---|---|---|---|---|---|---|---|---|---|
| S3 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 1→1 | G-037 +hit, G-044 +hit |
| S4 | 32→34 /41 | 30→32 | 9→9 /9 | 4→6 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-017 +hit |
| X1 | 31→31 /41 | 26→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | – |
| X2 | 32→32 /41 | 28→28 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 7→7 /7 | 7→7 /7 | 0→0 | – |
| H1 | 32→34 /41 | 30→32 | 9→9 /9 | 4→6 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-017 +hit |
| H2 | 33→35 /41 | 32→34 | 9→9 /9 | 4→6 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 0→0 | G-016 +hit, G-017 +hit |

**Ratchet verdicts (before → after)**

- S3→S4: FAIL (relation 6→4)  →  FAIL (relation 6→4)
- S4→S5: n/a (run lacks S5)
- X2→H2: FAIL (relation 7→5)  →  FAIL (relation 7→5)
- X2→E5-2: n/a (run lacks E5-2)
- H2→E5-2: n/a (run lacks E5-2)

## mac-qwen3-embedding: ollama:qwen3-embedding:0.6b + cross-encoder/ms-marco-MiniLM-L-6-v2

| config | Hit@4 | all@4 | single-fact | paraphrase | identifier | relation | stale | stale@4 | changed questions |
|---|---|---|---|---|---|---|---|---|---|
| S1 | 27→29 /41 | 22→24 | 8→8 /9 | 2→2 /10 | 6→6 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S2 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 3→3 | G-037 +hit, G-044 +hit |
| S3 | 29→31 /41 | 24→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 5→7 /7 | 1→1 | G-037 +hit, G-044 +hit |
| S4 | 31→32 /41 | 29→30 | 8→8 /9 | 4→5 /10 | 8→8 /8 | 4→4 /7 | 7→7 /7 | 1→1 | G-016 +hit |
| S5 | 30→30 /41 | 26→26 | 7→7 /9 | 3→3 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 1→1 | – |
| X1 | 31→31 /41 | 26→26 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | – |
| X2 | 32→32 /41 | 28→28 | 8→8 /9 | 2→2 /10 | 8→8 /8 | 7→7 /7 | 7→7 /7 | 0→0 | – |
| E5-1 | 30→30 /41 | 26→26 | 7→7 /9 | 3→3 /10 | 8→8 /8 | 5→5 /7 | 7→7 /7 | 0→0 | – |
| E5-2 | 31→31 /41 | 28→28 | 7→7 /9 | 3→3 /10 | 8→8 /8 | 6→6 /7 | 7→7 /7 | 0→0 | – |

**Ratchet verdicts (before → after)**

- S3→S4: FAIL (relation 6→4)  →  FAIL (relation 6→4)
- S4→S5: FAIL (Hit@4 31→30; all@4 29→26)  →  FAIL (Hit@4 32→30; all@4 30→26; paraphrase 5→3)
- X2→H2: n/a (run lacks H2)
- X2→E5-2: FAIL (Hit@4 32→31)  →  FAIL (Hit@4 32→31)
- H2→E5-2: n/a (run lacks H2)

## Verdicts that change

- none
