import unittest

from ratchet import trec


class TrecTest(unittest.TestCase):
    def test_qrels_judge_each_passage_once_in_a_fixed_order(self):
        qs = [
            {"id": "G-002", "stratum": "stale", "expected": ["a#1"], "stale": ["a#old"],
             "accept": [{"address": "b#2", "for": "question"}]},
            {"id": "G-001", "stratum": "relation", "expected": ["c#1", "d#2"],
             "accept": [{"address": "e#3", "for": "d#2"}]},
            {"id": "G-009", "stratum": "unanswerable", "expected": []},
        ]
        audit = [
            {"id": "G-001", "candidate": "z#9", "decision": "no"},
            {"id": "G-001", "candidate": "e#3", "decision": "standin"},
            {"id": "G-001", "candidate": "f#4", "decision": "partly"},
            {"id": "G-002", "candidate": "a#old", "decision": "whole"},  # already judged stale: first rule wins
        ]
        self.assertEqual(trec.qrels_lines(qs, audit), [
            "G-001 0 c#1 1", "G-001 0 d#2 1", "G-001 0 e#3 1", "G-001 0 f#4 0", "G-001 0 z#9 0",
            "G-002 0 a#1 1", "G-002 0 b#2 1", "G-002 0 a#old 0",
        ])

    def test_run_lines_keep_the_first_occurrence_of_a_split_passage(self):
        lines = trec.run_lines("G-044", ["p#1", "p#2", "p#1", "p#3"], "S1")
        self.assertEqual([l.split()[2] for l in lines], ["p#1", "p#2", "p#3"])
        self.assertEqual([int(l.split()[3]) for l in lines], [1, 2, 3])
        self.assertEqual(lines[0], "G-044 Q0 p#1 1 1.000000 S1")

    def test_run_lines_stop_at_depth(self):
        self.assertEqual(len(trec.run_lines("q", [f"p#{i}" for i in range(100)], "x", depth=50)), 50)


if __name__ == "__main__":
    unittest.main()
