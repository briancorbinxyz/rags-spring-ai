# SQLite FTS5 Internals for a Bitemporal Full-Text Index

**Scope note.** Every empirical claim below marked "verified" was executed locally during this research against **SQLite 3.53.4** (via Python's `sqlite3` module) and cross-checked against the macOS system CLI (reports `3.51.0 ... apl`, an Apple build). The newest release at time of writing is **3.54.1 (2026-10-16)** ([Release History Of SQLite](https://sqlite.org/changes.html)); I did not have a 3.54.x binary to test against, but the changelog shows **no FTS5 functional changes in 3.52–3.54** (see "Recent changes" under Q3), so 3.53.4 results should hold.

---

## Q1. How `bm25()` computes its statistics, and whether they can be scoped to a row subset

### Takeaway

`bm25()` is **irreducibly whole-table**. `N` (row count), `avgdl` (average document length) and `nHit` (per-phrase document frequency) all come from table-wide FTS5 extension-API calls that take no row-set argument, are computed once per query and cached in auxdata, and are completely unaffected by any extra `WHERE` constraint — verified: the same document scores `-3.04055731` with and without a `valid_from <= 55` filter applied. There is **no SQL-level mechanism** to scope them. The only real escape hatches are (a) physically partitioning the index so that "the subset" *is* a table, or (b) writing a custom auxiliary function in C via `fts5_api.xCreateFunction`.

### Cited Findings

- `bm25()` "returns a real value indicating how well the current row matches the full-text query"; the result is multiplied by −1 so better matches sort first under a plain ascending `ORDER BY`; the constants `k1` and `b` are **hard-coded at 1.2 and 0.75** and are not exposed as arguments. Trailing arguments are per-column weights; "if there are not enough arguments for all table columns, remaining columns are assigned a weight of 1.0" — [SQLite FTS5 Extension §5.1.1](https://sqlite.org/fts5.html)
- Exact syntax for weights: `SELECT * FROM email WHERE email MATCH ? ORDER BY bm25(email, 10.0, 5.0);` — [fts5.html §5.1.1](https://sqlite.org/fts5.html)
- The negation was a deliberate 2015 change "so that better matches have a lower numerical score, so that `ORDER BY rank` (not `ORDER BY rank DESC`) does what you want" — [SQLite check-in](https://sqlite.org/src/info/71d32f53e81921e4)
- Implementation (`ext/fts5/fts5_aux.c`): `Fts5Bm25Data` is a per-query cache holding `nPhrase`, `avgdl` (double), `aIDF[]` (per-phrase IDF) and `aFreq[]` (scratch per-row frequencies); `aIDF`/`aFreq` are carved out of the same allocation as the struct — [ext/fts5/fts5_aux.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_aux.c)
- `fts5Bm25GetData()` calls `xGetAuxdata(pFts, 0)` first and reuses an existing stats object; otherwise it computes stats once and stores them with `xSetAuxdata(pFts, p, sqlite3_free)`, so they are freed at end of query — [fts5_aux.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_aux.c)
- `avgdl` is built from `xRowCount` → `nRow` and `xColumnTotalSize(pFts, -1, &nToken)` → total tokens, then `avgdl = nToken / nRow` — [fts5_aux.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_aux.c)
- Document frequency per phrase (`nHit`) comes from `xQueryPhrase()` driving the `fts5CountCb` counter callback; the source states the formula as `IDF = log( (N - nHit + 0.5) / (nHit + 0.5) )`, and **clamps IDF to `1e-6` when the result is `<= 0`**, with the comment explaining that "if (N < 2*nHit), the IDF is negative" — [fts5_aux.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_aux.c)
- Per-row work in `fts5Bm25Function`: zero `aFreq`, walk `xInstCount`/`xInst` accumulating per-phrase weighted frequency `f`, get row length `D` from `xColumnSize(pFts, -1, ...)`, then accumulate `IDF * (f * (k1 + 1)) / (f + k1 * (1 - b + b * D / avgdl))` per phrase and return `-1.0 * score` — [fts5_aux.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_aux.c)
- API scope is documented as table-wide, not row-set-wide: `xRowCount` "is used to retrieve the total number of rows in the table", equivalent to `SELECT count(*) FROM ftstable;`; `xColumnTotalSize` returns "the total number of tokens in column iCol, considering all rows in the FTS5 table", and reports the whole-table total when `iCol` is negative — [ext/fts5/fts5.h](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5.h)
- `xColumnSize` "may be quite inefficient if used with an FTS5 table created with the `columnsize=0` option"; `xInstCount` "can be quite slow if used with an FTS5 table created with the `detail=none` or `detail=column` option" — [fts5.h](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5.h)
- `xQueryPhrase` opens a **new cursor**, clones only the expression for the single phrase `iPhrase`, and walks every matching rowid from `SMALLEST_INT64` to `LARGEST_INT64`, invoking the callback per row; a callback returning non-`SQLITE_OK` abandons the query, and `SQLITE_DONE` makes `xQueryPhrase` return `SQLITE_OK` — [ext/fts5/fts5_main.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_main.c), [fts5.h](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5.h)
- Custom auxiliary functions are registered through `fts5_api`: `int (*xCreateFunction)(fts5_api *pApi, const char *zName, void *pUserData, fts5_extension_function xFunction, void (*xDestroy)(void*));` — "Create a new auxiliary function". Current interface versions: `Fts5ExtensionApi.iVersion` is "currently always set to 4", `fts5_api.iVersion` "currently always set to 3" — [fts5.h](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5.h). The fts5 extension API last changed shape in 3.20.0 (2017-08-01) — [fts5.html](https://sqlite.org/fts5.html)
- **Verified (3.53.4)**: 52-row table, term `rare` in rows 1 and 2.
  `SELECT rowid, round(bm25(d),8) FROM d WHERE d MATCH 'rare' ORDER BY rank` → `[(1, -3.04055731), (2, -1.89637546)]`.
  Adding `AND vf <= 55` → `[(1, -3.04055731)]` — **identical score**, proving `N`, `avgdl` and `nHit` were not restricted to the filtered set.
- **Verified (3.51.0 CLI)**: the same two documents alone in a 2-row table score `-1.21e-06` and `-8.5e-07` (IDF clamped to `1e-6` because `N < 2*nHit`), vs `-3.04` / `-1.90` in the 52-row table. The same documents, same query, different corpus → scores differ by six orders of magnitude and are **not comparable across partitions**.
- **Verified (3.53.4)**: `bm25()` with no `MATCH` in the query returns `0.0` for every row (not an error, not NULL) — i.e. it silently degenerates rather than failing loudly.

### Inferences

- The `1e-6` IDF clamp is a serious hazard for time-travel ranking: in any small corpus, or any slice where a term is common, every hit collapses to a near-identical floor value and BM25 ranking becomes effectively random. Partition-per-time-slice designs hit this constantly because partitions are small.
- Because stats are cached in auxdata keyed to the query (not to the row set), you cannot even trick FTS5 into recomputing them mid-scan; the cost model is "one `xQueryPhrase` full-index walk per phrase per query, then O(1) per row".
- A C auxiliary function registered via `xCreateFunction` is the only way to get true subset statistics while keeping a single index: it would have to take the subset predicate as data (e.g. a time point) and compute `N`, `avgdl`, `nHit` itself — which means it cannot use `xRowCount`/`xColumnTotalSize`/`xQueryPhrase` as-is, and would need either its own `fts5vocab`-style scan or precomputed per-slice statistics tables. Cost: C extension, loaded per-connection, versus zero if you accept global stats.
- Accepting global IDF is often defensible for bitemporal ranking (IDF drift over time is usually small); accepting global `avgdl` and global `N` is likewise usually tolerable. What is *not* tolerable is comparing BM25 across partitions.

### Gaps

- I found no SQLite forum/sqlite-users thread in which the maintainers address subset-scoped BM25 directly; a search for exactly that turned up nothing authoritative ([search result set](https://sqlite.org/fts5.html)). So "there is no way to scope it" rests on the source and the API documentation plus local verification, not on an explicit maintainer statement.
- `Fts5ExtensionApi` version 4 members (added after the original set) were not individually enumerated here; if a newer member offers scoped counting, I did not find it. The members relevant to BM25 (`xRowCount`, `xColumnTotalSize`, `xQueryPhrase`) are all table-wide.

---

## Q2. `fts5vocab` row / col / instance — exact columns, and subset document frequency by join

### Takeaway

`fts5vocab(..., instance)` **can** be joined against a filtered row set to compute an exact per-term document frequency restricted to that subset, and it works on contentless and external-content tables, and it correctly excludes deleted rows (verified, including on `contentless_delete=1`). The gotchas are performance, not correctness: **only `term` constraints are pushed down** — `doc` and `col` are never pushed down, so a `doc` range cannot prune the index scan, and a per-term `instance` scan reads every posting for that term.

### Cited Findings

- Creation syntax; the vocab table must live in the same database as its FTS5 table, except in `temp`, where a three-argument form may name another attached database. Using three arguments outside `temp` is an error:
  ```sql
  CREATE VIRTUAL TABLE ft1_v USING fts5vocab('ft1', 'row');
  CREATE VIRTUAL TABLE ft2_v USING fts5vocab(ft2, col);
  CREATE VIRTUAL TABLE ft3_v USING fts5vocab(ft3, instance);
  CREATE VIRTUAL TABLE temp.ft1_v USING fts5vocab(main, 'ft1', 'row');
  ```
  — [fts5.html §8](https://sqlite.org/fts5.html)
- **Exact columns** — [fts5.html §8](https://sqlite.org/fts5.html):
  - `row`: `(term, doc, cnt)` — one row per distinct term; `doc` = number of rows containing the term at least once; `cnt` = total instances across the table.
  - `col`: `(term, col, doc, cnt)` — one row per term/column pair; `col` = column *name*; `doc` = rows where that column contains the term; `cnt` = instances in that column across all rows.
  - `instance`: `(term, doc, col, offset)` — one row per stored term instance; here **`doc` is the rowid of the containing row**, `col` is the column name, `offset` is the 0-based token index within the column.
- `instance` assumes `detail='full'`. With `detail='col'`, `offset` is always NULL and there is one row per term/doc/col; with `detail='none'`, both `offset` and `col` are NULL and there is one row per term/doc — [fts5.html §8](https://sqlite.org/fts5.html)
- The `instance` variant was added in **SQLite 3.21.0 (2017-10-24)**, described in the release history as "a new fts5vocab virtual table type that provides direct access to an FTS5 full-text index at the lowest possible level" — [Release History Of SQLite](https://sqlite.org/changes.html); the source history for `ext/fts5/fts5_vocab.c` shows the `instance` type added in an August 2017 check-in — [fts5_vocab.c file history](https://www2.sqlite.org/cgi/src/finfo?name=ext/fts5/fts5_vocab.c)
- **`xBestIndex` (`fts5VocabBestIndexMethod`)**: only constraints on **column 0 (`term`)** are considered, and only with a binary collation. `idxNum` starts as the `colUsed` mask (`FTS5_VOCAB_COLUSED_MASK` = `0xFF`) then ORs in `FTS5_VOCAB_TERM_EQ` (`0x0100`), `FTS5_VOCAB_TERM_GE` (`0x0200`, for `>=` and `>`) or `FTS5_VOCAB_TERM_LE` (`0x0400`, for `<=` and `<`). Costs: `100` with an EQ constraint (GE/LE then ignored); otherwise `1,000,000` halved per GE/LE bound (500,000 for one, 250,000 for both). Constraints on `doc`, `col` and `cnt` are **never examined** (`p->iColumn==0` is the only branch), so SQLite must evaluate them after the vtab returns rows. A single ascending `ORDER BY term` sets `orderByConsumed` — the table "always delivers results in ascending order" of `term` — [ext/fts5/fts5_vocab.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_vocab.c)
- `fts5VocabInstanceNext` is a lazy forward iterator: it advances one posting at a time via `sqlite3Fts5IterNextScan()` / `fts5VocabInstanceNewTerm()`, stopping at the end of the index or at the LE bound. Nothing is precomputed or materialised — [fts5_vocab.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_vocab.c)
- **Verified (3.51.0 CLI)** — exact subset DF via join, and that it differs from the global DF:
  ```sql
  CREATE VIRTUAL TABLE v_inst USING fts5vocab(docs, instance);
  SELECT i.term, count(DISTINCT i.doc) AS df_slice
    FROM v_inst i JOIN docs d ON d.rowid = i.doc
   WHERE i.term = 'alpha' AND d.vf <= 55 AND d.vt > 55;
  -- alpha|1   (global v_row.doc for 'alpha' was 4)
  ```
- **Verified**: `EXPLAIN QUERY PLAN` for that join → `SCAN i VIRTUAL TABLE INDEX 259:` then `SCAN d VIRTUAL TABLE INDEX 0:=`. `259 = 0x103` = colUsed `0x03` | `TERM_EQ 0x100`, confirming term-equality pushdown and a rowid-lookup-driven inner side.
- **Verified**: `... WHERE term='w5' AND doc BETWEEN 1 AND 500` → `SCAN dv VIRTUAL TABLE INDEX 271:` — **the same plan code as `term='w5'` alone** (`271 = 0x10F`), i.e. the `doc` range contributed nothing. A term range `term>='w' AND term<'x'` → `INDEX 1551:` = `0x60F` = colUsed | `GE | LE`.
- **Verified — deleted rows are not exposed.** Content-bearing table: after `DELETE FROM docs WHERE rowid=2` (which held `alpha alpha delta`), `fts5vocab(docs,row)` reported `alpha|3|3` and no `delta` row at all.
- **Verified — tombstones in contentless-delete tables are honoured too (3.53.4).** `content='', contentless_delete=1, contentless_unindexed=1`, rows 1 (`alpha beta`) and 2 (`alpha gamma`); after `DELETE FROM cu WHERE rowid=1`, `fts5vocab(cu,row)` returned exactly `[('alpha',1,1), ('gamma',1,1)]` — `beta` gone, `alpha`'s doc and cnt both decremented.
- **Verified — `fts5vocab` works on contentless and external-content tables.** `fts5vocab(ec, instance)` over `content=base, content_rowid=id` returned `alpha|1|body|0` and `alpha|2|body|0`; `fts5vocab(cd, row)` over `content='', contentless_delete=1` returned correct counts. **Note** that on a plain `content=''` table without `contentless_delete=1`, rows cannot be deleted at all (see Q3), so the vocab table can only ever be "append-correct".
- **Verified — bulk subset DF for all terms at once is one full instance scan**: `SELECT i.term, count(DISTINCT i.doc) FROM dv i JOIN d ON d.rowid=i.doc WHERE d.vf<=500 GROUP BY i.term` over a 2000-row/101-term table returned 101 groups. This is an unavoidable whole-index walk.
- A known FTS5 bug in the vocab module was fixed in **3.51.1 (2025-11-28)**: "Fix a latent bug in fts5vocab virtual table, exposed by new optimizations in the 3.51.0 release" — [Release History Of SQLite](https://sqlite.org/changes.html). Another: "Truncated Words bug with fts5vocab" — [SQLite User Forum](https://sqlite.org/forum/info/fb1e6b3c2591d3fa)

### Inferences

- Performance characteristics of the subset-DF join: for a single term, cost is O(total postings for that term) plus one rowid lookup per posting into the content/metadata table. The term-equality pushdown makes the vocab side cheap to *start*, but there is no way to stop early or to skip postings whose `doc` falls outside the time slice, because `doc` is not a pushdown-capable constraint. For a high-frequency term in a large index this is the same cost as evaluating the query itself — so computing subset DF this way roughly doubles the work per query term, and more if you also need subset `avgdl` (which needs `xColumnSize` per surviving row, i.e. a `docsize` read per row).
- `count(DISTINCT i.doc)` is required, not `count(*)`: `instance` yields one row per *occurrence*, so `count(*)` gives collection frequency, not document frequency. With `detail=none`/`detail=col` the distinction partly disappears (one row per term/doc, or per term/doc/col).
- `instance` is also the only way to get subset *term frequency* (`cnt`-equivalent) for a time slice, since `row.cnt`/`col.cnt` are whole-table.
- Because `fts5vocab` honours tombstones on `contentless_delete=1` tables, the "logical delete = tombstone" model stays consistent with vocab-derived statistics. That is important: it means a bitemporal design that *physically deletes* superseded rows (rather than keeping them with a closed `valid_to`) gets self-consistent vocab stats for the current state, but loses the ability to reconstruct past states at all.
- The 3.51.1 vocab bug fix is a reason to require **>= 3.51.1** for any design that puts `fts5vocab` on the critical path.

### Gaps

- `fts5_vocab.c` itself contains no comment about contentless/external-content tables or about tombstone filtering; my claims there are from local verification on 3.51.0/3.53.4, not from documentation. The FTS5 docs' `fts5vocab` section says nothing about deleted rows, contentless tables, or performance.
- I found no published benchmark numbers for `fts5vocab(instance)` scan throughput.
- Whether the 3.51.1 vocab bug could have produced *wrong statistics* (vs. a crash/assert) is not stated in the one-line changelog entry, and I did not locate the check-in.

---

## Q3. `content=`, `content=''`, `contentless_delete=1`, `contentless_unindexed` — storage, columns, aux functions, versions

### Takeaway

For a bitemporal index the decisive combination is **`content='' , contentless_delete=1, contentless_unindexed=1`** (SQLite **3.47.0+**): it drops the document text entirely while *persisting* `UNINDEXED` columns, so `valid_from`/`valid_to` live inside the FTS5 table and are readable and filterable, and rows can be deleted/superseded. Auxiliary functions that need text (`snippet`, `highlight`) stop working on contentless tables; `bm25()` still works everywhere.

### Cited Findings

- **Version timeline** (all from primary sources):
  - FTS5 shipped in **3.9.0 (2015-10-14)**; the fts5 extension API changed in **3.20.0 (2017-08-01)** — [fts5.html](https://sqlite.org/fts5.html)
  - `fts5vocab(..., instance)`: **3.21.0 (2017-10-24)** — [changes.html](https://sqlite.org/changes.html)
  - FTS5 **secure-delete**: **3.42.0** — [fts5.html](https://sqlite.org/fts5.html)
  - **`contentless_delete=1`**: "As of version **3.43.0**" — [fts5.html §4.4.3](https://sqlite.org/fts5.html) (3.43.0 was 2023-08-24)
  - **`contentless_unindexed=1`**: **3.47.0 (2024-10-21)** — "Add the `contentless_unindexed=1` option, for creating contentless fts5 tables that store the values of any UNINDEXED columns persistently in the database" — [SQLite Release 3.47.0](https://sqlite.org/releaselog/3_47_0.html)
  - Also 3.47.0: `fts5_tokenizer_v2` API and the `locale=1` option — [SQLite Release 3.47.0](https://sqlite.org/releaselog/3_47_0.html)
- **External content** (`content=`): "The `content` option names a table, virtual table or view in the same database"; syntax `CREATE VIRTUAL TABLE ft USING fts5(a, c, content=t1, content_rowid=d);` — [fts5.html §4.4](https://sqlite.org/fts5.html)
- **Contentless** (`content=''`): `CREATE VIRTUAL TABLE ft USING fts5(a, b, c, content='');` — reading any column except `rowid` returns NULL, and such tables "do not support UPDATE or DELETE statements" in the usual way — [fts5.html §4.4](https://sqlite.org/fts5.html)
- **Contentless-delete**: `CREATE VIRTUAL TABLE ft USING fts5(a, b, c, content='', contentless_delete=1);` supports `DELETE` and `INSERT OR REPLACE`, and `UPDATE` only when all user-defined columns are supplied — [fts5.html §4.4.3](https://sqlite.org/fts5.html)
- **Contentless-unindexed**: `CREATE VIRTUAL TABLE ft USING fts5(a, b UNINDEXED, content='', contentless_unindexed=1);` — "It is an error to specify `contentless_unindexed=1` for an fts5 table that is not a contentless or contentless-delete table." Without the option, values written to `UNINDEXED` columns of a contentless table are "neither indexed nor stored", so reads return NULL — [fts5.html §4.9](https://sqlite.org/fts5.html); [3.47.0 release notes](https://sqlite.org/releaselog/3_47_0.html)
- Pre-release check-ins on the `fts5-contentless-unindexed` branch (Sept 2024) first blocked regular `DELETE`/`UPDATE` on such tables unless `contentless_delete=1` was also set, then a later check-in "allowed UPDATEs of unindexed columns" — [search of sqlite.org check-ins](https://sqlite.org/releaselog/3_47_0.html) (I could not pin each check-in to a specific release; see Gaps)
- **Verified (3.51.0 CLI)** — plain contentless:
  ```sql
  CREATE VIRTUAL TABLE cl USING fts5(body, content='');
  SELECT rowid, body IS NULL FROM cl;            -- 1|1  2|1   (body always NULL)
  SELECT snippet(cl,0,'[',']','...',5) FROM cl WHERE cl MATCH 'alpha';  -- empty strings
  SELECT rowid, bm25(cl) FROM cl WHERE cl MATCH 'alpha' ORDER BY rank;  -- works (-1.0e-06)
  DELETE FROM cl WHERE rowid=1;
  -- Runtime error: cannot DELETE from contentless fts5 table: cl
  ```
- **Verified (3.51.0 CLI)** — contentless-delete: after `DELETE FROM cd WHERE rowid=1`, `SELECT rowid FROM cd WHERE cd MATCH 'alpha'` returned only `2`, and `bm25(cd)` still evaluated.
- **Verified (3.53.4)** — contentless + unindexed, the bitemporal shape:
  ```sql
  CREATE VIRTUAL TABLE cu USING fts5(
    body, vf UNINDEXED,
    content='', contentless_delete=1, contentless_unindexed=1);
  INSERT INTO cu(rowid,body,vf) VALUES(1,'alpha beta',10),(2,'alpha gamma',20);
  SELECT rowid, body, vf FROM cu;                       -- (1,None,10), (2,None,20)
  SELECT rowid FROM cu WHERE cu MATCH 'alpha' AND vf>15; -- 2
  UPDATE cu SET vf=99 WHERE rowid=2;                     -- succeeds; vf now 99
  ```
  i.e. `body` reads back NULL but `vf` persists, is filterable, **and can be UPDATEd on its own** without supplying `body` (contrary to the blanket "UPDATE only when all user-defined columns are supplied" rule, which applies to the indexed columns).
- **Verified (3.51.0 CLI)**: `contentless_unindexed=1` is accepted on a plain `content=''` table *without* `contentless_delete=1`, and `UNINDEXED` values persist there too — matching the doc wording ("contentless **or** contentless-delete").
- **Verified (3.51.0 CLI)** — external content: after `INSERT INTO ec(ec) VALUES('rebuild');`, the join pattern works, and `bm25()` and `fts5vocab(ec, instance)` both function:
  ```sql
  SELECT e.rowid, bm25(ec) FROM ec e JOIN base b ON b.id = e.rowid
   WHERE ec MATCH 'alpha' AND b.vf <= 55 ORDER BY rank;
  ```
- **Recent changes (2025–2026)** — the only FTS5 entries in the changelog since 3.49 are: 3.51.1 (2025-11-28) "Fix a latent bug in fts5vocab virtual table, exposed by new optimizations in the 3.51.0 release"; 3.51.0 (2025-11-04) "Improved error messages generated by FTS5"; 3.50.3 (2025-07-17) memory-error fix on FTS5 index queries; 3.50.2 (2025-06-28) fix for UPDATEs on fts5 tables containing BLOB values. **No FTS5 entries at all in 3.52, 3.53 or 3.54.** Most recent release: **3.54.1 (2026-10-16)** — [Release History Of SQLite](https://sqlite.org/changes.html)

### Inferences

- Storage trade-offs for a bitemporal corpus, where every version of a document is a row:
  - **Default (content-bearing)**: FTS5 stores the full text in the `%_content` shadow table *in addition to* the index — roughly 2× the text. With many versions of similar documents this is the worst case.
  - **`content=` (external)**: no `%_content` duplication; you own the base table, which is exactly where `valid_from`/`valid_to` and a `txn_from`/`txn_to` pair naturally live. Cost: FTS5 cannot see your filter columns at all, so time filtering is always a join, and you must maintain the index by trigger or by `'rebuild'`.
  - **`content=''` + `contentless_unindexed=1`**: smallest — index plus the unindexed columns only, no text. The decisive advantage for bitemporal work is that the temporal columns are *inside* the FTS5 table, so no join is needed to filter. The cost is that you can never retrieve, snippet or highlight the matched text from FTS5 (verified: `snippet()` returns empty), so you need the text somewhere else anyway — at which point the storage win over `content=` narrows to "FTS5 doesn't duplicate it".
- For a *bi*temporal model (valid time + transaction time), four integer columns (`valid_from, valid_to, tx_from, tx_to`) as `UNINDEXED` in a `contentless_unindexed=1` table is the most compact arrangement that still allows the "as the collection stood at time T" predicate to be evaluated without a join. Requires **3.47.0+** (2024-10-21), which is the hard version floor for that design.
- Because `UPDATE` of an `UNINDEXED`-only column succeeds (verified), closing a row's transaction-time interval (`tx_to = now`) does **not** require re-indexing the document text. That makes the "never delete, close the interval" bitemporal discipline cheap in FTS5 — a genuinely important property, and one that was explicitly enabled by a late pre-3.47.0 check-in.
- Conversely, on a plain `content=''` table (pre-3.43.0 semantics) you cannot delete or update at all, so bitemporal corrections are impossible without rebuilding — the workaround cost is a full reindex.

### Gaps

- I did not find a documentation statement quantifying the storage saving of `content=''` versus the default; the claim that the default roughly doubles text storage is an inference from the existence of the `%_content` shadow table (which I observed locally: `a_data`, `a_idx`, `a_content`, `a_docsize`, `a_config`), not a cited figure.
- Exact release in which "UPDATE of only unindexed columns" became legal: the pre-release check-ins are dated Sept 2024 (so presumably 3.47.0), but I could not confirm it shipped in 3.47.0 rather than a later point release. Verified working in 3.53.4.
- `contentless_delete=1` tombstone storage overhead (the `%_tombstone`/delete-marker representation) is not quantified in anything I read.

---

## Q4. Extra non-indexed columns, `UNINDEXED`, and filtering inside vs outside `MATCH`

### Takeaway

Yes — `valid_from`/`valid_to` integers can live in the FTS5 table as `UNINDEXED` columns, and that is the recommended shape. `UNINDEXED` keeps them out of the index so they cannot pollute `bm25()` (there is nothing to match, and the column's token count does not exist). Filtering on an `UNINDEXED` column **inside** the `MATCH` string is syntactically accepted but **silently matches nothing** — a dangerous trap. It must be a separate `WHERE` term, and that term is **not** pushed into the virtual table: FTS5's `xBestIndex` never claims plain `=`/`<`/`>` on ordinary columns.

### Cited Findings

- "The contents of columns qualified with the UNINDEXED column option are not added to the FTS index"; example `CREATE VIRTUAL TABLE customers USING fts5(name, addr, uuid UNINDEXED);` — [fts5.html §4.1](https://sqlite.org/fts5.html)
- **`fts5BestIndexMethod` — what FTS5 claims** ([ext/fts5/fts5_main.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_main.c)):
  - `MATCH` on a table column: claimed, with `omit = 1` (SQLite does not re-check it).
  - `MATCH` on `rank`: claimed, `omit = 1`, and only **one** such constraint is accepted (a second is skipped).
  - rowid `=`: claimed with an `argvIndex` but **`omit` is not set**, so SQLite re-checks it.
  - rowid `<`/`<=` and `>`/`>=`: claimed (argvIndex, not omit); at most one lower and one upper bound; range constraints are ignored if a rowid `=` is present.
  - `LIKE`/`GLOB` on ordinary columns: claimed **only for indexed columns** (`abUnindexed[iCol]==0`) and only when the tokenizer's pattern mode matches (i.e. `trigram`).
  - **Plain `=`, `<`, `>` on ordinary columns: not claimed at all** — those branches only handle rowid comparisons.
- `idxStr` encoding, per the source comment: `"m"` match against table column, `"r"` match against rank, `"M<column-number>"` match against another column, `"L<column-number>"` LIKE, `"G<column-number>"` GLOB, `"="` rowid equality, `"<"` rowid `<`/`<=`, `">"` rowid `>`/`>=`. At most one `r` or `=`; `=` excludes `<` and `>` — [fts5_main.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_main.c)
- Cost estimates per the comment: **with** MATCH — no other constraints `50000.0`, one rowid range `37500.0`, both ranges `30000.0`, rowid `==` `25000.0`; **without** MATCH — `3000000.0`, one range `2250000.0`, both ranges `750000.0`, rowid `==` `25.0`. The code and comment disagree in one case: with MATCH plus both rowid bounds, the code assigns `50000.0`, not `30000.0` — [fts5_main.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_main.c)
- **Verified (3.53.4)** — the plan proves the asymmetry:
  ```
  ... WHERE d MATCH 'rare' AND vf<=55    ORDER BY rank  ->  SCAN d VIRTUAL TABLE INDEX 32:M2
  ... WHERE d MATCH 'rare' AND rowid<10  ORDER BY rank  ->  SCAN d VIRTUAL TABLE INDEX 32:M2<
  ```
  and on 3.51.0, `AND rowid BETWEEN 100 AND 200` → `INDEX 32:M2><`. The `UNINDEXED` constraint contributes nothing to `idxStr`; rowid bounds do.
- **Verified (3.51.0 CLI)** — the `MATCH`-side trap:
  ```sql
  SELECT count(*) FROM d1 WHERE d1 MATCH 'vf:1';        -- 0   (no error!)
  SELECT count(*) FROM d1 WHERE d1 MATCH 'nosuchcol:1'; -- Runtime error: no such column: nosuchcol
  ```
  A column filter naming an `UNINDEXED` column parses, resolves, and returns zero rows; only a genuinely non-existent column errors.
- **Verified (3.51.0 CLI)** — `bm25()` is unaffected by the presence of `UNINDEXED` columns carrying integers: scores for `MATCH 'rare'` were `-3.04055731` / `-1.89637546` in a table with `vf`/`vt` populated, and the column-weight argument list only covers columns that exist (remaining columns default to weight 1.0 — [fts5.html §5.1.1](https://sqlite.org/fts5.html)).
- **Verified (3.51.0 CLI)** — on a `contentless_unindexed=1` table the `UNINDEXED` filter still works as a separate `WHERE` term: `SELECT rowid FROM cu WHERE cu MATCH 'alpha' AND vf>15` → `2`.
- **Alias gotcha, verified (3.51.0 CLI)**: neither `tbl AS f ... WHERE f MATCH ?` nor `bm25(f)` is legal — both fail with `no such column: f`. The real table name must be used as the `MATCH` left-hand side and as the auxiliary function's first argument, even when the table is aliased elsewhere in the query. Independently reported on the forum — [SQLite User Forum (2024)](https://sqlite.org/forum/forumpost/5b303ab003f91660). The working form (verified) keeps the alias only for column references: `SELECT e.rowid, bm25(ec) FROM ec e JOIN base b ON b.id=e.rowid WHERE ec MATCH 'alpha' AND b.vf<=55 ORDER BY rank;`

### Inferences

- `MATCH 'vf:1'` returning zero rows rather than an error is the single most likely source of a silent correctness bug in a bitemporal FTS5 design: a developer who writes the temporal predicate into the match string gets an empty result set with no diagnostic. Any query builder should reject column filters naming `UNINDEXED` columns.
- `UNINDEXED` is strictly better than an indexed metadata column for temporal bounds: an indexed integer column would add tokens to the index, inflate `xColumnTotalSize` (and therefore `avgdl`), and make `MATCH '1'` match documents by their timestamp. `UNINDEXED` avoids all three.
- Because no `UNINDEXED` constraint is pushed down, the temporal filter is always a post-filter over the full match result. For a query whose term matches 10^6 rows of which 10^3 are live at time T, FTS5 does the full 10^6-row scan and SQLite throws away 999,000 rows — *after* `bm25()` has been evaluated on them if the aux function appears in the output or `ORDER BY` (see Q5).

### Gaps

- The documentation does not state what a column filter against an `UNINDEXED` column is *supposed* to do; the `fts5_main.c` `MATCH` branch does not check `abUnindexed`, so this may be unspecified behaviour rather than a guarantee. Treat "returns zero rows" as observed on 3.51.0/3.53.4, not promised.

---

## Q5. `rank`, `ORDER BY rank`, extra `WHERE` constraints, and filter pushdown

### Takeaway

The order is **match → score → filter → sort**, and it is **not controllable**. The only filter FTS5 will accept *before* scoring is a **rowid equality or rowid range** (and `LIKE`/`GLOB` on a `trigram`-tokenized indexed column). That makes "encode the time dimension in the rowid" the only true pushdown lever available for a bitemporal index.

### Cited Findings

- `rank` returns NULL for non-full-text queries; for full-text queries it defaults to `bm25()` with no trailing arguments. Usage forms — [fts5.html §5.2, §6.11](https://sqlite.org/fts5.html):
  ```sql
  SELECT * FROM ft WHERE ft MATCH ? ORDER BY rank;
  SELECT * FROM ft WHERE ft MATCH ? AND rank MATCH 'bm25(10.0, 5.0)' ORDER BY rank;
  INSERT INTO ft(ft, rank) VALUES('rank', 'bm25(10.0, 5.0)');   -- persistent default
  ```
- `rank MATCH ...` is itself a claimed constraint with `omit=1`, and **only one** `rank` constraint is accepted per query — [fts5_main.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_main.c)
- Only rowid `=`/`<`/`<=`/`>`/`>=` and (indexed-column, pattern-mode) `LIKE`/`GLOB` reach the vtab besides `MATCH`/`rank`; plain comparisons on ordinary columns are not claimed — [fts5_main.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_main.c)
- **Verified (3.51.0 CLI)**: `rank MATCH 'bm25(2.0)'` composes correctly with an additional non-pushed-down `WHERE` term: `SELECT rowid FROM d1 WHERE d1 MATCH 'rare' AND rank MATCH 'bm25(2.0)' AND vf<=55 ORDER BY rank` returned `1`.
- **Verified (3.53.4)**: rowid-range pushdown narrows the scan but **does not change the statistics**: `MATCH 'w5'` alone and `MATCH 'w5' AND rowid BETWEEN 1 AND 500` both returned `-4.52254663` for every hit. So pushdown buys I/O, not scoping.
- **Verified**: `ORDER BY rank` with an extra `WHERE` yields a single plan line — `SCAN docs VIRTUAL TABLE INDEX 32:M3` — i.e. no separate index/sort step is introduced; FTS5 delivers rows and SQLite filters/sorts them.
- SQLite's own FTS5 tests use `ORDER BY bm25(t8) DESC` as well as `ORDER BY rank`, confirming both spellings — [SQLite source](https://sqlite.org/src/artifact/0c0712b1430158f9)

### Inferences

- There is no `LIMIT`-aware interaction that saves you: `ORDER BY rank LIMIT k` still requires FTS5 to produce every matching row and `bm25()` to be evaluated on each, because the sort is external to the vtab. Combined with a selective temporal post-filter, a `LIMIT 10` query can cost a full posting-list traversal.
- **The rowid is the one pushdown channel, and it is monotonic.** If rowids are assigned in transaction-time order (which they naturally are for an append-only bitemporal log), then `rowid <= <rowid watermark at T>` is a *pushed-down* restriction to "rows that existed as of transaction time T". That gives genuine as-of-T transaction-time travel with index-level pruning, at the cost of maintaining a time→rowid watermark table. This does **not** work for valid time, which is not monotonic in insertion order.
- Combining both: push transaction time down via a rowid upper bound, post-filter valid time on `UNINDEXED` columns. Statistics remain global in both cases.
- `rank MATCH 'bm25(...)'` only reweights columns; it cannot inject a predicate or change the corpus, so it is not a scoping mechanism.

### Gaps

- I found no documented way to make FTS5 evaluate an arbitrary predicate before `bm25()`; no forum thread or doc section on "pushing a filter down before scoring" surfaced. The absence of such a mechanism follows from `xBestIndex` not claiming the constraint, which is strong but is an argument from source rather than an explicit statement.
- The discrepancy between the `fts5_main.c` cost comment (`30000.0`) and the code (`50000.0`) for MATCH + two rowid bounds is noted above; I did not find a check-in explaining which is intended, and it could affect planner choices in queries joining FTS5 to a metadata table.

---

## Q6. Partitioning an FTS5 index across several tables

### Takeaway

Partitioning is the only way to get genuinely slice-scoped BM25 statistics, and it works mechanically (`UNION ALL` over partitions, or routing to one partition), but **BM25 scores from different FTS5 tables are not comparable** and there is **no published SQLite guidance** on the pattern — it is folklore, not documentation. Small partitions additionally trigger the `1e-6` IDF clamp.

### Cited Findings

- **Verified (3.51.0 CLI)** — the comparability problem, concretely. The same two documents, same query `'rare'`:
  - in a 52-row table: `-3.04055731`, `-1.89637546`
  - alone in a 2-row table: `-1.21e-06`, `-8.5e-07`

  A `UNION ALL` over the two produced an ordering in which *every* row from the big partition outranked *every* row from the small one, purely as an artefact of corpus size:
  ```
  d1|1|-3.04055730587643
  d1|2|-1.89637546255011
  p1|1|-1.21259842519685e-06
  p1|2|-8.50828729281768e-07
  ```
- The small-partition scores are at the IDF floor: the source clamps IDF to `1e-6` whenever `log((N-nHit+0.5)/(nHit+0.5)) <= 0`, i.e. whenever `N < 2*nHit` — which is common in a small time-slice partition — [fts5_aux.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_aux.c)
- The pattern of enumerating FTS tables from `sqlite_master` and building a `UNION` query across them is in circulation, with the explicit caveat that "there's no way to compare the scores you're getting across different tables" — [Datasette issue comment](https://datasette-graphql-demo.datasette.io/github/issue_comments/504793988) (secondary source, illustrative)
- `UNION ALL` views over partitioned tables as a general SQLite idiom are described by practitioners as workable but "hacky and a bit brittle" — secondary/illustrative only.
- Alias restriction applies per partition: `MATCH` and the auxiliary functions need the **real** table name, so a generated `UNION ALL` must repeat the table name in three places per branch (verified; see Q4).
- Each FTS5 table has its own independent shadow tables — verified locally: `a_data`, `a_idx`, `a_content`, `a_docsize`, `a_config` for table `a`.

### Inferences

- Two viable partitioning schemes for time travel, with different costs:
  1. **Partition by time bucket (e.g. one FTS5 table per year/epoch).** A query "as of T" touches the partitions overlapping T. BM25 is then scoped to those partitions — which is *closer* to the semantics you want — but scores across partitions in a multi-partition query are incomparable, and you must either re-rank with your own statistics or accept the distortion. Write amplification: a document whose validity spans N buckets is indexed N times.
  2. **Partition by "current" vs "historical".** One partition holds the live state (fast, small, correct stats for "now"), the other holds superseded versions (queried only for as-of queries). This gives correct stats for the overwhelmingly common "now" query and degrades gracefully for historical ones.
- To make a `UNION ALL` across partitions rankable, you must recompute a comparable score yourself. The ingredients are all obtainable: per-partition `N` via `SELECT count(*)`, per-partition total tokens via `fts5vocab(..., row)` summed `cnt` (or `xColumnTotalSize` through a custom function), per-partition DF via `fts5vocab(..., row).doc`, and per-row term frequency via `fts5vocab(..., instance)`. The cost is reimplementing BM25 in SQL and losing the per-row efficiency of the C implementation — an order-of-magnitude slowdown is the realistic expectation, though I have no benchmark for it.
- A cheaper approximation: compute a **global** IDF once (from a union of per-partition `fts5vocab(row)` counts) and apply it uniformly, keeping FTS5's `bm25()` only for the term-frequency/length component. This makes partitions comparable without reimplementing the whole scorer — but it is exactly the opposite of slice-scoped IDF, so it only helps the "partitioning for size" case, not the "scores should reflect the historical corpus" case.

### Gaps

- **I found no sqlite.org documentation, forum post, or maintainer statement offering guidance on partitioning FTS5 indexes.** The FTS5 docs do not discuss it at all. Everything above about the pattern is inference plus secondary sources.
- No published benchmarks comparing a single large FTS5 index against N partitioned ones for the same corpus.

---

## Q7. `tokenize` options and sharing a tokenizer/vocabulary across partitions

### Takeaway

Two FTS5 tables can be configured with an **identical tokenizer** (built-in or custom, by name and arguments) and will therefore produce an identical term vocabulary *in the linguistic sense* — verified. They can **never share a vocabulary or an index**: each FTS5 table owns a private, independent set of shadow tables, and there is no mechanism for one FTS5 table to read another's index. `fts5vocab` reads one FTS5 table only.

### Cited Findings

- `tokenize` takes "either an FTS5 bareword, or an SQL text literal"; the first element is the tokenizer name and the rest are its arguments. Valid: `CREATE VIRTUAL TABLE ft USING fts5(x, tokenize = 'porter ascii');` Invalid: `tokenize = '"porter" "ascii"'` and `tokenize = 'porter' 'ascii'` — [fts5.html §4.3](https://sqlite.org/fts5.html)
- Built-in tokenizers: **`unicode61`** (the default), **`ascii`**, **`porter`**, **`trigram`** — [fts5.html §4.3](https://sqlite.org/fts5.html)
- `fts5_tokenizer_v2` and the `locale=1` option were added in **3.47.0 (2024-10-21)** — [SQLite Release 3.47.0](https://sqlite.org/releaselog/3_47_0.html)
- Custom tokenizers are registered through the `fts5_api` structure, the same mechanism as custom auxiliary functions (`xCreateFunction` sits alongside the tokenizer registration entry points); `fts5_api.iVersion` is "currently always set to 3" — [fts5.h](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5.h)
- **Verified (3.51.0 CLI)** — identical tokenizer configuration on two tables yields identical stemming behaviour:
  ```sql
  CREATE VIRTUAL TABLE a USING fts5(x, tokenize='porter unicode61 remove_diacritics 2');
  CREATE VIRTUAL TABLE b USING fts5(x, tokenize='porter unicode61 remove_diacritics 2');
  INSERT INTO a VALUES('running quickly');  INSERT INTO b VALUES('runs quick');
  SELECT rowid FROM a WHERE a MATCH 'run';  -- 1
  SELECT rowid FROM b WHERE b MATCH 'run';  -- 1
  ```
  The `tokenize=` clause is preserved verbatim in the table's schema (`.schema a` shows it), so partitions can be created from a single stored DDL template.
- **Verified**: each table has its own shadow tables (`a_data`, `a_idx`, `a_content`, `a_docsize`, `a_config`), and `fts5vocab` takes exactly one FTS5 table name — there is no syntax for a union of two.
- `LIKE`/`GLOB` pushdown into FTS5 only happens "when the tokenizer's pattern mode matches" (i.e. `trigram`) and only for indexed columns — [fts5_main.c](https://raw.githubusercontent.com/sqlite/sqlite/master/ext/fts5/fts5_main.c)

### Inferences

- "Sharing a tokenizer" across partitions is a *configuration* discipline, not a runtime capability: store one canonical `tokenize=` string and use it in every partition's DDL. A mismatch between partitions is silent and produces subtly incomparable indexes (e.g. one partition stemming and another not), with no error at query time — worth asserting on at startup by reading `sqlite_master` and comparing the DDL across partitions.
- For a custom tokenizer, registration is per-database-connection, so every connection that opens the database must register it before touching any partition — otherwise `CREATE VIRTUAL TABLE`/queries against tables using that tokenizer fail. This is an operational cost of partitioning with custom tokenizers, multiplied by nothing (one registration covers all partitions on that connection).
- `trigram` is the only tokenizer that enables `LIKE`/`GLOB` pushdown, which is irrelevant to temporal filtering (integers) but relevant if a bitemporal design wants substring search alongside.
- Because vocabularies cannot be shared, any cross-partition statistic must be computed by the application from per-partition `fts5vocab` reads and summed — reinforcing the Q6 conclusion.

### Gaps

- I did not find documentation on whether `locale=1` / `fts5_tokenizer_v2` (3.47.0) has any bearing on cross-table vocabulary consistency; it appears orthogonal (per-value locale for tokenization), but I did not read its section.
- No source addresses whether two FTS5 tables could be made to share shadow tables by any supported means; the absence of any such mechanism in the docs and the per-table shadow naming scheme make it safe to say no, but this is inference.

---

## Cross-cutting design note (inference, not a key question)

The empirically-supported shape for "rank these documents as the collection stood at time T" on a single FTS5 index, as of SQLite 3.47.0+:

```sql
CREATE VIRTUAL TABLE doc_fts USING fts5(
  body,
  valid_from UNINDEXED, valid_to UNINDEXED,
  tx_from    UNINDEXED, tx_to    UNINDEXED,
  content='', contentless_delete=1, contentless_unindexed=1,
  tokenize='porter unicode61 remove_diacritics 2'
);

SELECT rowid, bm25(doc_fts) AS score
  FROM doc_fts
 WHERE doc_fts MATCH :q
   AND valid_from <= :t AND valid_to > :t
   AND tx_from    <= :a AND tx_to    > :a
 ORDER BY rank
 LIMIT 20;
```

What this gets you (all verified above): temporal columns stored and filterable without a join; deletes and interval-closing `UPDATE`s on the unindexed columns alone; `bm25()` functional. What it does **not** get you: scoped statistics (`N`, `avgdl`, IDF are whole-table, including every historical version of every document), any pushdown of the four temporal predicates, or retrievable text/snippets. The two costs in order of severity are (1) IDF inflated by duplicate historical versions of the same document, and (2) a full posting-list scan per query regardless of how selective the time point is. Mitigations and their prices are in Q1 (custom C aux function), Q2 (`fts5vocab(instance)` join for exact slice DF, ~doubling per-term work), Q5 (rowid watermark for transaction-time pushdown), and Q6 (partitioning, at the price of incomparable scores).
