"""SparseIndex.search keeps BM25's contract, so it can stand in for BM25's candidate list."""

import unittest

import numpy as np
from scipy.sparse import csr_matrix

from ratchet.splade import SparseIndex


class FakeEncoder:
    vocab = 4

    def _encode(self, texts):
        # term 0 for "a", term 1 for "b"
        out = []
        for t in texts:
            ids = [i for i, w in enumerate("ab") if w in t.split()]
            out.append((np.array(ids, dtype=np.int32), np.ones(len(ids), dtype=np.float32)))
        return out


def index():
    rows = np.array([[1, 0, 0, 0],   # 0: a
                     [2, 1, 0, 0],   # 1: a a b -> best for "a b"
                     [0, 0, 3, 0],   # 2: shares nothing with a/b
                     [1, 0, 0, 0],   # 3: ties with 0
                     [0, 1, 0, 0]],  # 4: b
                    dtype=np.float32)
    return SparseIndex(FakeEncoder(), csr_matrix(rows))


class Search(unittest.TestCase):
    def test_order_and_ties_break_on_item_number(self):
        self.assertEqual([1, 0, 3], [n for n, _ in index().search("a")])

    def test_items_sharing_no_term_are_not_returned(self):
        self.assertNotIn(2, [n for n, _ in index().search("a b")])

    def test_allow_filters_before_the_depth_cut(self):
        got = index().search("a b", allow=lambda n: n != 1, depth=2)
        self.assertEqual([0, 3], [n for n, _ in got])

    def test_scope(self):
        self.assertEqual([3, 4], [n for n, _ in index().search("a b", scope=[3, 4])])


if __name__ == "__main__":
    unittest.main()
