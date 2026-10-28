# The whole question set: Episode 6's surviving arms, 5090 tier

The series has written **105 questions** across four files. This scores **105** of them from runs that already happened.

## Sources, in precedence order

| set | run | config | rows | questions it owns |
|---|---|---|---|---|
| time | `5090-slice-moved.jsonl` | `TK` | 14 | 14 |
| time | `5090-qwen3-emb-8b-qwen3-rr-time-e6.jsonl` | `GA` | 24 | 10 |
| main | `5090-qwen3-emb-8b-qwen3-rr-sections.jsonl` | `GA` | 53 | 53 |
| heldout | `5090-qwen3-emb-8b-qwen3-rr-heldout-e6.jsonl` | `GA` | 13 | 13 |
| routing | `5090-routing.jsonl` | `GA` | 15 | 15 |

## The headline

| questions | answerable | Hit@4 | all-evidence@4 | MRR@50 | groundless@4 | unanswerable | confidently wrong | stale@4 |
|---|---|---|---|---|---|---|---|---|
| 105 | 98 | **83/98** (84%) | 75 | 0.762 | 22/105 | 7 | 2 | 8 |

## By set

| set | questions | Hit@4 | groundless@4 | confidently wrong |
|---|---|---|---|---|
| main | 53 | 47/47 | 6/53 | 0 |
| time | 24 | 15/23 | 9/24 | 0 |
| heldout (reserve) | 13 | 7/13 | 6/13 | 2 |
| routing (reserve) | 15 | 14/15 | 1/15 | 0 |

## By stratum

| stratum | questions | Hit@4 | groundless@4 |
|---|---|---|---|
| as-known | 4 | 2/3 | 2/4 |
| as-of | 5 | 5/5 | 0/5 |
| history | 12 | 7/12 | 5/12 |
| history-decoy | 3 | 3/3 | 0/3 |
| identifier | 8 | 8/8 | 0/8 |
| intent | 5 | 5/5 | 0/5 |
| intent-noword | 8 | 7/8 | 1/8 |
| paraphrase | 10 | 10/10 | 0/10 |
| relation | 15 | 11/15 | 4/15 |
| single-fact | 10 | 10/10 | 0/10 |
| stale | 7 | 7/7 | 0/7 |
| triage-why | 7 | 7/7 | 0/7 |
| unanswerable | 6 | 0/0 | 6/6 |
| what-changed | 5 | 1/5 | 4/5 |
