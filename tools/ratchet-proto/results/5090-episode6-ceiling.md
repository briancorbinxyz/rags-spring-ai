# Episode 6 on the 5090: the time set's measured ceiling

Qwen3-Embedding-8B with Qwen3-Reranker-8B, 27 Oct. Every number is a floor: no blind-audit round has
looked at a time candidate. Each question is scored by the configuration that ships:

- **`TF`** over the slice for the nine cutoff questions (`5090-slice-context.*`).
- **`TF`** over the changed units for the five `what-changed` (`5090-slice-changed-arms.*`, scored
  *located · told*).
- **`GA`**, the main line, over the snapshot for history and its decoys
  (`5090-qwen3-emb-8b-qwen3-rr-time-e6.*`).

**16 of 24**, against the handover's 14:

- **Cutoff rows:** 8 of 9.
- **`what-changed`:** 1 of 5, located and told.
- **History:** 4 of 7.
- **Decoys:** 3 of 3.

On the time set run through `run.py`, `GA` passes the ratchet against `E5-2`. Hit@4 goes from 11 to
15, all@4 from 10 to 13, and stale@4 from 10 to 9. The decoys are untouched at 1, 1 and 1.

| question | stratum | rank | answered | why it does not move, if it does not |
|---|---|---|---|---|
| G-051 | history | 12 | | `G7` has it at **1**; adding the scope arms (`scope_rrf`, `doc_scope`) moves it to 12. They are what close the main set, so the trade stands. |
| G-052 | history | 2 | yes | |
| G-054 | history | 1 | yes | |
| G-055 | history | 39 | | Reachable, then buried by the fusion. Every pin that unburies history costs the decoy G-061 (`history_pin`, rejected). |
| G-056 | history | 1 | yes | |
| G-057 | history | 1 | yes | The D-005 spine question, answered by `history_keeps_superseded`. |
| G-058 | history | – | | Born withdrawn, and outside the top 50 even with `admit_history`. It needs a change of policy, not a ranking: see below. |
| G-059 | decoy | 1 | yes | |
| G-060 | decoy | 1 | yes | |
| G-061 | decoy | 1 | yes | |
| G-062 | as-of | 2 | yes | |
| G-063 | as-of | 1 | yes | |
| G-064 | as-of | 1 | yes | |
| G-065 | as-of | 1 | yes | |
| G-066 | as-of | 2 | yes | |
| G-067 | what-changed | 20 | | The change proposal's own copy, `changes/archive/2026-08-13-address-a-screen/proposal.html#FR-013`, ranks **2nd**. It is an audit candidate, not judged here. |
| G-068 | what-changed | 3 | yes | |
| G-069 | what-changed | – | | It names no spec, so the scope arms never fire. The right spec ranks first, through its `#changelog` (rank 1) and tasks, above `#FR-002`. `#changelog` is an audit candidate. |
| G-070 | what-changed | – | | The same shape: spec 003's `#SC-001` and its `#changelog` (6th) lead, over a window of 4,667 changed units. |
| G-071 | what-changed | 22 | | `TD` has it at **3**, by not reranking a named document. `TD`'s router, `route_scope`, fails the main set (33 → 31). |
| G-072 | as-known | 1 | yes | |
| G-073 | as-known | 2 | yes | The negation question, now answered by reordering alone. |
| G-074 | as-known | 5 | | The mirror of G-073. Reranking the named document puts `triage-log#T-001`, `spec#context` and `design#approach` above `design#D-001`. `TD` has it at **1** and loses G-073. Under every measured configuration, one of the two is outside the cut. `context_scope` fires (the question says "why") and moves nothing. |
| G-075 | as-known | abstained | yes | |

## What was tried tonight and did not move anything

- **`context_scope` on the time set (`TK` = `TF` + `context_scope`):** identical to `TF` on every
  question, with changed-only and without.
- **Fitted triggers for `#changelog`, `#risks` and `#intent`:** declined. Each would be chosen from
  two or three of the questions it fixes. Showing that one is inert means scoring the held-out set,
  which this episode spends once, on the main line, and not on a trigger. If they are wanted, they
  are a deliberate fit and should be labelled as one.

## The policy case: withdrawn records and history questions

G-051, G-052 and G-058 expect units inside **withdrawn** proposals. A withdrawn proposal was never in
force, so no position in the history admits it, and over the slice the standing filter excludes it
at every cutoff. `EXCLUDED` removes 196 of 11,492 units, and 82 addresses are excluded in *every*
state.

- **For admitting them to history questions:** a history question asks what was *considered*, not
  only what was in force. A withdrawn proposal is exactly the record of a road not taken, and on the
  snapshot `admit_history` already lets a history question see them. That is how G-051 and G-052
  reach ranks 1–2 there.
- **Against:** it changes what the series means by "in force" for the slice, where the filter is
  what makes a retrieved address the right revision by construction. A withdrawn unit has no
  standing at any position, so admitting it is a different definition, not a different ranking.

This is Brian's decision, and it is not made here.
