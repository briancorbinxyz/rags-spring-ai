# Bitemporal table schema design for a document/passage store with a full-text index

Scope note: target store is SQLite + FTS5, so SQL:2011 temporal syntax is unavailable. The standard forms are documented below so it is clear what the hand-rolled predicates are emulating. DDL marked **[design, not cited]** is my own adaptation, not a vendor pattern.

## Q1. The canonical bitemporal schema and the SQL:2011 forms

### Takeaway
SQL:2011 standardises two things only: a named `PERIOD` built from two ordinary columns with closed-open semantics, and the reserved period name `SYSTEM_TIME` which turns on system-versioning (`GENERATED ALWAYS AS ROW START/END`, `FOR SYSTEM_TIME AS OF`). Everything else — history storage, the sentinel "still current" value, retention, indexes, and the entire question of what `AS OF` means on the *application*-time axis — is left to vendors, and they diverge sharply.

### Cited findings
- SQL:2011 (ISO/IEC 9075:2011) is the seventh revision of the standard and its headline feature is temporal support; a `PERIOD` is defined over two ordinary table columns serving as start and end, with **closed-open** semantics — [Survey of SQL:2011 Temporal Features](https://illuminatedcomputing.com/posts/2019/08/sql2011-survey/); [PostgreSQL wiki: SQL2011Temporal](https://wiki.postgresql.org/wiki/SQL2011Temporal)
- The period name is free-form *except* `SYSTEM_TIME`, which is reserved and enables the system-time features; the two source columns must be `NOT NULL` (the implementation adds the constraint if absent), and the period implies a check that start precedes end — [Survey of SQL:2011 Temporal Features](https://illuminatedcomputing.com/posts/2019/08/sql2011-survey/)
- Because the source columns are `NOT NULL`, an open-ended "until further notice" row needs a **sentinel** date (the survey uses `3000-01-01` as its example) rather than `NULL` — [Survey of SQL:2011 Temporal Features](https://illuminatedcomputing.com/posts/2019/08/sql2011-survey/)
- A table may carry one `SYSTEM_TIME` period plus at most one application-time period; MariaDB rejects a second application-time period with error 4154 and Oracle with ORA-55603 — [pgsql-hackers, "SQL:2011 application time"](https://www.postgresql.org/message-id/CADkLM=ekdXZceMyggb6Lqj2447VfiS-Bw22Gdgd5tBotfKmi6A@mail.gmail.com)
- Standard DML on application-time rows **splits** periods on `UPDATE`/`DELETE` (`FOR PORTION OF`) — [Survey of SQL:2011 Temporal Features](https://illuminatedcomputing.com/posts/2019/08/sql2011-survey/)
- Canonical system-time DDL (SQL Server, verbatim):
  ```sql
  CREATE TABLE Employee
  (
      [EmployeeID] INT NOT NULL PRIMARY KEY CLUSTERED,
      [Name] NVARCHAR (100) NOT NULL,
      [ValidFrom] DATETIME2 (2) GENERATED ALWAYS AS ROW START,
      [ValidTo] DATETIME2 (2) GENERATED ALWAYS AS ROW END,
      PERIOD FOR SYSTEM_TIME (ValidFrom, ValidTo)
  )
  WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = dbo.EmployeeHistory));
  ```
  — [SQL Server: Temporal table usage scenarios](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/usage-scenarios)
- Retrofitting onto an existing table uses `HIDDEN` period columns, with the explicit sentinel default `'9999.12.31 23:59:59.99'`:
  ```sql
  ALTER TABLE Employee
  ADD
      ValidFrom DATETIME2 (2) GENERATED ALWAYS AS ROW START HIDDEN
          CONSTRAINT DF_ValidFrom DEFAULT DATEADD(SECOND, -1, SYSUTCDATETIME()),
      ValidTo DATETIME2 (2) GENERATED ALWAYS AS ROW END HIDDEN
          CONSTRAINT DF_ValidTo DEFAULT '9999.12.31 23:59:59.99',
      PERIOD FOR SYSTEM_TIME (ValidFrom, ValidTo);
  ALTER TABLE Employee SET (SYSTEM_VERSIONING = ON (HISTORY_TABLE = dbo.Employee_History));
  ```
  — [SQL Server: Temporal table usage scenarios](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/usage-scenarios)
- `HIDDEN` period columns are omitted from `SELECT *` and must be named explicitly to be returned — [SQL Server: Query data in a system-versioned temporal table](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/query-data)
- SQL Server stores period values **in UTC** and `AS OF` literals/variables are interpreted as UTC — [SQL Server: Query data in a system-versioned temporal table](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/query-data)
- The precision of the `datetime2` period columns must match between the current table and the history table — [SQL Server: Temporal table usage scenarios](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/usage-scenarios)
- MariaDB offers both an explicit form (`GENERATED ALWAYS AS ROW START/END` + `PERIOD FOR SYSTEM_TIME(start,end)` + `WITH SYSTEM VERSIONING`) and a *simplified* form (`WITH SYSTEM VERSIONING` alone), where the versioning data is invisible to `SELECT *` and is read through the `ROW_START` / `ROW_END` pseudo-columns; individual columns can be excluded from history with `WITHOUT SYSTEM VERSIONING` — [MariaDB: System-versioned tables](https://mariadb.com/docs/server/reference/sql-structure/temporal-tables/system-versioned-tables)
- Oracle's application-time axis ("temporal validity") declares the period, optionally auto-creating the columns:
  ```sql
  CREATE TABLE employees_temp (
         employee_id NUMBER(6) NOT NULL, first_name VARCHAR2(20), ...
         PERIOD FOR emp_track_time);
  ```
  The auto-created columns are hidden `EMP_TRACK_TIME_START` / `EMP_TRACK_TIME_END` of type `TIMESTAMP(6) WITH TIME ZONE` — [Oracle VLDB Guide 5.3: Controlling the Validity and Visibility of Data](https://docs.oracle.com/en/database/oracle/oracle-database/18/vldbg/control-validity-visibility-data.html)
- XTDB names all four bounds explicitly as row columns: `_valid_from`, `_valid_to` (user-editable, real-world) and `_system_from`, `_system_to` (database-set, read-only); an open-ended bound renders as `null` in query output — [XTDB: Time in XTDB](https://docs.xtdb.com/about/time-in-xtdb)
- MarkLogic models each axis as "a named pair of range indexes that serves as the container for periods"; bitemporal documents have both a `valid` and a `system` axis, uni-temporal documents only a `system` axis — [MarkLogic: Introduction to bitemporal](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/intro.html)

### Inferences
- The minimum portable column set for your case is six fields, not four: the business key (`doc_path`), the two valid-time bounds, the two system-time bounds, and a monotonic tiebreaker (see Q3). The standard gives you no tiebreaker, which is precisely why you need to add one.
- SQL:2011 standardises the *system*-time `AS OF` but not an application-time `AS OF`; Oracle invented `AS OF PERIOD FOR`, MariaDB has no application-time `AS OF` at all, and XTDB invented `FOR VALID_TIME AS OF`. Emulating "what was in force at T" on SQLite therefore has no single standard to copy — pick the XTDB vocabulary (`FOR VALID_TIME AS OF` / `FOR SYSTEM_TIME AS OF`) since it is the only one that names both axes symmetrically.

### SQLite emulation **[design, not cited]**
```sql
-- One table serving both "what is current" and "as in force at T".
CREATE TABLE passage (
  -- surrogate identity of THIS version row; also the FTS5 rowid
  version_id     INTEGER PRIMARY KEY,          -- rowid alias

  -- business identity (stable across renames; see Q6)
  doc_id         INTEGER NOT NULL,             -- identity that survives a rename
  doc_path       TEXT    NOT NULL,             -- path AS OF this version
  passage_ord    INTEGER NOT NULL,             -- position within the document

  -- valid time: when the fact was true of the world  [valid_from, valid_to)
  valid_from     TEXT    NOT NULL,             -- 'YYYY-MM-DDTHH:MM:SSZ'
  valid_to       TEXT    NOT NULL DEFAULT '9999-12-31T00:00:00Z',

  -- system time: when the store learned it      [system_from, system_to)
  system_from    TEXT    NOT NULL,
  system_to      TEXT    NOT NULL DEFAULT '9999-12-31T00:00:00Z',

  -- monotonic tiebreaker; see Q3
  seq            INTEGER NOT NULL,             -- global, gap-free-ish, never reused
  commit_sha     TEXT,                         -- provenance for the seq

  body           TEXT    NOT NULL,

  CHECK (valid_from  < valid_to),
  CHECK (system_from < system_to)
) STRICT;
```
Emulated query forms (each is the plain-predicate equivalent of the standard clause):
```sql
-- FOR SYSTEM_TIME AS OF :t   ("as known at :t")
WHERE system_from <= :t AND :t < system_to

-- FOR VALID_TIME AS OF :t    ("as in force at :t")
WHERE valid_from  <= :t AND :t < valid_to

-- bitemporal point: what the store believed at :st about what held at :vt
WHERE system_from <= :st AND :st < system_to
  AND valid_from  <= :vt AND :vt < valid_to

-- current (both axes open) -- the fast path
WHERE system_to = '9999-12-31T00:00:00Z'
  AND valid_to   = '9999-12-31T00:00:00Z'

-- FOR SYSTEM_TIME FROM :a TO :b  (overlap, upper EXCLUSIVE)
WHERE system_from < :b AND system_to > :a

-- FOR SYSTEM_TIME BETWEEN :a AND :b  (overlap, upper INCLUSIVE)
WHERE system_from <= :b AND system_to > :a

-- CONTAINED IN (:a, :b)  (version lived wholly inside the window)
WHERE system_from >= :a AND system_to <= :b

-- FOR SYSTEM_TIME ALL  -- no predicate at all
```

### Gaps
- I did not read the ISO/IEC 9075:2011 text itself (paywalled). The closed-open semantics, the `NOT NULL` rule and the `SYSTEM_TIME` reservation are from a secondary survey and the PostgreSQL wiki, both of which agree, but should be treated as second-hand.
- Oracle's `VERSIONS PERIOD FOR <period> BETWEEN ... AND ...` grammar is referenced on the VLDB page but not shown there; the SQL Language Reference would be needed for the exact form.

## Q2. Interval representation: closed-open vs closed-closed, and the sentinel

### Takeaway
Every system I could check that documents its convention uses **closed-open `[from, to)`** — SQL:2011, PostgreSQL range types (which *canonicalise* discrete ranges to `[)`), XTDB, SQL Server. Nobody uses `NULL` for the open end in a SQL temporal table, because the period source columns are `NOT NULL`; they use a max-value sentinel (`9999-12-31 23:59:59.99` in SQL Server, `2038-01-19 05:14:07.999999` in MariaDB). Your `23:59:59Z` cutoffs are a closed-closed habit and are the single most likely source of off-by-one-second bugs.

### Cited findings
- PostgreSQL's two-argument range constructor "constructs a range in standard form (lower bound inclusive, upper bound exclusive)", i.e. `[)` — [PostgreSQL: Range Types](https://www.postgresql.org/docs/current/rangetypes.html)
- "The built-in range types `int4range`, `int8range`, and `daterange` all use a canonical form that includes the lower bound and excludes the upper bound; that is, `[)`." `int8range(1, 14, '(]')` *displays* as `[2,15)`. The stated reason is that without canonicalisation, two ranges holding the same values would compare as unequal — [PostgreSQL: Range Types](https://www.postgresql.org/docs/current/rangetypes.html)
- Continuous element types (`numeric`, `timestamp`) have **no** canonical form in PostgreSQL and keep whatever bounds you give them — so a `tstzrange` will happily store `[a, b]` and compare unequal to `[a, b')` — [PostgreSQL: Range Types](https://www.postgresql.org/docs/current/rangetypes.html)
- An omitted PostgreSQL bound is "beyond any range element type's +/-infinity values"; an inclusive missing bound is silently converted to exclusive (`[,]` becomes `(,)`); passing `NULL` as a bound also produces an unbounded side; `[today,infinity)` excludes the `infinity` value while `[today,infinity]` includes it — [PostgreSQL: Range Types](https://www.postgresql.org/docs/current/rangetypes.html)
- SQL:2011 period source columns are `NOT NULL`, so an open-ended row requires a sentinel date rather than `NULL` — [Survey of SQL:2011 Temporal Features](https://illuminatedcomputing.com/posts/2019/08/sql2011-survey/)
- SQL Server's sentinel is `'9999.12.31 23:59:59.99'` and the docs' own "is this the current row?" idiom is `IIF (YEAR(ValidTo) = 9999, 1, 0) AS IsActual` / `WHERE ValidTo < '9999-12-31 23:59:59.99'` / `WHERE YEAR(ValidTo) < 9999` — [SQL Server: Temporal table usage scenarios](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/usage-scenarios); [SQL Server: Query data](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/query-data)
- MariaDB's current-row sentinel is `row_end = 2147483647.999999`, i.e. the `FROM_UNIXTIME` maximum, displayed as `2038-01-19 05:14:07.999999` — [MariaDB: System-versioned tables](https://mariadb.com/docs/server/reference/sql-structure/temporal-tables/system-versioned-tables)
- XTDB shows an open-ended bound as `null` in query output and as ∞ in its tables, and its timeline notation is half-open ("version 1 for T1 ≤ time < T3") — [XTDB: Time in XTDB](https://docs.xtdb.com/about/time-in-xtdb)
- PostgreSQL's non-overlap guarantee for a keyed temporal table is an exclusion constraint, not `UNIQUE`:
  ```sql
  CREATE EXTENSION btree_gist;
  CREATE TABLE room_reservation (
      room text,
      during tsrange,
      EXCLUDE USING GIST (room WITH =, during WITH &&)
  );
  ```
  — [PostgreSQL: Range Types](https://www.postgresql.org/docs/current/rangetypes.html)

### Inferences
- Arguments actually borne out by the sources: `[)` makes adjacency exact (`prev.to = next.from`, no gap and no overlap to adjudicate), makes "as of T" a single uniform predicate (`from <= T AND T < to`) with no double-counting at a boundary, and makes equality well-defined — PostgreSQL's canonicalisation rationale is precisely that last point. Closed-closed's only real argument is human readability of a printed end date, and it buys that at the cost of needing a "smallest representable increment" to compute the next row's start — which differs per type and per column precision (SQL Server's `datetime2(2)` vs `datetime2(7)` gives different sentinels, which is why the docs insist the precision match between current and history tables).
- `23:59:59Z` as a stated cutoff is a closed-closed end. Mixing it into a `[)` schema produces a one-second hole each day: a fact stamped `2024-03-01T23:59:59.500Z` belongs to neither `[.., 2024-03-01T23:59:59Z)` nor `[2024-03-02T00:00:00Z, ..)`. Since your document dates are day-granular anyway (Q3), the clean fix is to normalise every stated `…T23:59:59Z` cutoff to the exclusive `[)` upper bound `…+1 dayT00:00:00Z` **at ingest**, keep the user-facing phrasing as "through 1 March", and never store `23:59:59`.
- Prefer a **sentinel over `NULL`** in SQLite even though SQLite would permit `NULL`: with a sentinel, every predicate is the same three-way comparison with no `IS NULL` branch, `ORDER BY valid_to` sorts correctly, and `UNIQUE`/index equality on the sentinel works (in SQLite, as in standard SQL, multiple `NULL`s do not collide in a unique index, so `NULL` would silently permit duplicate current rows). Use a lexicographically-sortable sentinel string that matches your ISO-8601 text format — `'9999-12-31T00:00:00Z'` — so string comparison equals chronological comparison.
- Store timestamps as fixed-width, zero-padded, UTC, `Z`-suffixed ISO-8601 text. Mixed offsets or variable-width text break the `<=`/`<` comparisons that every predicate above depends on, and SQLite will not warn you.

### Gaps
- I found no vendor documentation arguing *for* closed-closed. The closed-closed camp is essentially the Date/Darwen/Lorentzos "closed-closed with a granularity" position, which I did not locate a primary citation for in this pass.

## Q3. Ordering keys when timestamps tie or are coarse

### Takeaway
No vendor temporal-table documentation I found prescribes a tiebreaker column — this is the single biggest gap between the standard and a working system. The systems that actually cope with coarse or tied time do it by *not ordering on wall clock at all*: Dolt addresses versions by commit hash or branch (the DAG, not the clock), MarkLogic introduces LSQT (Last Stable Query Time) as a watermark separating "queryable" from "still being ingested", and the academic treatment of genuinely unknown event times is Dyreson & Snodgrass's valid-time indeterminacy.

### Cited findings
- Dolt's `AS OF` takes "a commit hash, a branch name, or a timestamp", and a different `AS OF` may be used per table in one query; uncommitted working-set changes are *not* visible through `AS OF` — [Dolt: Querying history](https://docs.dolthub.com/sql-reference/version-control/querying-history)
- Dolt exposes per-row history through a read-only `dolt_history_$TABLENAME` system table per user table, carrying `commit_hash`, `committer` and `commit_date` **plus every user column** — i.e. the commit identity travels with each historical row — [Dolt: Querying history](https://docs.dolthub.com/sql-reference/version-control/querying-history); [Dolt: System tables](https://docs.dolthub.com/sql-reference/version-control/dolt-system-tables)
- Querying a Dolt *view* `AS OF` uses the **current** view definition with historical rows; getting the historical definition requires checking out the database at that commit — [Dolt: Querying history](https://docs.dolthub.com/sql-reference/version-control/querying-history)
- MarkLogic's LSQT: "Documents with a system start time before LSQT can be queried. Documents with a system start time after LSQT can be updated and ingested." — [MarkLogic: Introduction to bitemporal](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/intro.html)
- `cts:lsqt-query` "Returns only documents before LSQT or a timestamp before LSQT for stable query results"; companions are `cts.lsqtQueryTimestamp`, `cts.lsqtQueryTemporalCollection`, `cts.lsqtQueryWeight` — [MarkLogic: Searching temporal documents](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/searching.html)
- XTDB's `_system_from`/`_system_to` are database-set and read-only, while `_valid_from`/`_valid_to` are user-editable; a query with no temporal clause means "as best known" × "as of now" — [XTDB: Time in XTDB](https://docs.xtdb.com/about/time-in-xtdb)
- XTDB `INSERT` acts as an **upsert**: the existing row stays reachable to historical queries and the new row becomes current; backdated and future-dated changes are recorded with a valid time earlier or later than the recording time, and the original belief remains queryable in system time — [XTDB: Time in XTDB](https://docs.xtdb.com/about/time-in-xtdb)
- Valid-time indeterminacy — "an event is known to have occurred but its exact time is not" — was introduced by Dyreson & Snodgrass at ICDE 1993 (Best Paper); each event is represented by a **set of possible chronons** delimiting when it might have occurred, plus a probability distribution over that set, and the query language is extended with user-supplied *credibility* (in the data) and *plausibility* (in the relationships among data), with a tuple-calculus semantics that reduces to the determinate semantics on determinate data — [Dyreson & Snodgrass, "Valid-time Indeterminacy", ICDE 1993](https://ulp.cs.arizona.edu/people/rts/pubs/ICDE93p335.pdf)
- The full treatment is Dyreson's 1994 Arizona dissertation and the 1998 ACM TODS paper "Supporting Valid-time Indeterminacy"; later work by Anselma, Terenziani & Snodgrass (IEEE TKDE 2012) frames it as "don't know when" indeterminacy and proposes a reference representation plus a family of sixteen more compact representational models — [U. Arizona repository](https://repository.arizona.edu/handle/10150/186920); [Anselma, Terenziani & Snodgrass, TKDE 2012](https://ulp.cs.arizona.edu/people/rts/pubs/LucaTKDE12.pdf)
- SQL Server's own documentation, when it needs a stable per-row version ordinal, does not have one and synthesises it: `ROW_NUMBER() OVER (PARTITION BY EmployeeID ORDER BY [ValidTo] DESC)` over `FOR SYSTEM_TIME ALL` — [SQL Server: Temporal table usage scenarios](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/usage-scenarios)
- MariaDB's system-time axis is clock-derived with microsecond `row_start`/`row_end`, with no documented tiebreaker — [MariaDB: System-versioned tables](https://mariadb.com/docs/server/reference/sql-structure/temporal-tables/system-versioned-tables)

### Inferences
- Your two measurements are different problems and want different fixes.
  - **Day-granular document dates** are *valid-time* coarseness. The honest representation is `[day T00:00:00Z, day+1 T00:00:00Z)` — an interval, not an instant. Do not synthesise a within-day time you do not have. If a query needs a within-day ordering, it must come from the system-time axis or from `seq`, never from the valid-time stamp.
  - **68 of 722 commits sharing a committer timestamp** is *system-time* coarseness. Per Dolt's model, the right identity for a version is the commit, not the clock: resolve ties by the commit's position in the DAG (a topological ordering you compute once at ingest), materialise that as a dense integer `seq`, and treat the timestamp as a lookup *into* `seq` rather than as the ordering key.
- Concretely: build `seq` as a gap-free ascending integer over a topological sort of the commit DAG (ties within one topological level broken deterministically — e.g. by commit sha — so the import is reproducible). Then `AS OF <timestamp>` becomes a two-step: map the timestamp to the highest `seq` whose commit time `<= :t`, then select on `seq`. Integer comparison on `seq` is exact, index-friendly, and immune to ties.
- MarkLogic's LSQT is the pattern to copy for ingest safety: keep a single-row watermark table holding the highest `seq` that is fully ingested, and make every read-side query filter `seq <= (SELECT lsqt_seq FROM watermark)`. Without it, a partially-imported commit is visible as a document set with some passages updated and some not — the exact failure LSQT exists to prevent.
- Dyreson & Snodgrass's framework is the formally correct answer to day-granular dates, but it is heavyweight (probability distributions over chronon sets, credibility/plausibility query parameters). The pragmatic subset for your case is: store the possible-extent interval honestly as `[day, day+1)`, and record the *granularity* you were given so a later query can report "within 2024-03-01" rather than asserting a false instant.

### SQLite emulation **[design, not cited]**
```sql
-- Commit ordering resolved once, at import; the clock is never the ordering key.
CREATE TABLE commit_seq (
  seq          INTEGER PRIMARY KEY,   -- dense topological order, 1..N
  commit_sha   TEXT NOT NULL UNIQUE,
  committed_at TEXT NOT NULL,         -- may tie across rows; NOT an ordering key
  topo_level   INTEGER NOT NULL
) STRICT;
CREATE INDEX commit_seq_time ON commit_seq(committed_at, seq);

-- MarkLogic-style LSQT watermark: nothing above this seq is queryable.
CREATE TABLE watermark (
  id       INTEGER PRIMARY KEY CHECK (id = 1),
  lsqt_seq INTEGER NOT NULL
) STRICT;

-- Timestamp -> seq resolution for an AS OF query (ties resolved by MAX(seq)).
-- :t is already normalised to an exclusive-upper instant (never 23:59:59).
SELECT MAX(seq) FROM commit_seq
WHERE committed_at < :t_exclusive
  AND seq <= (SELECT lsqt_seq FROM watermark);

-- Valid-time granularity recorded, not guessed.
ALTER TABLE passage ADD COLUMN valid_granularity TEXT NOT NULL DEFAULT 'day'
  CHECK (valid_granularity IN ('day','second'));
```

### Gaps
- I found **no** vendor documentation (SQL Server, MariaDB, Oracle, XTDB) that specifies a tiebreaker for identical period timestamps or states what happens when two system-time versions share a timestamp. SQL Server's `datetime2` precision makes it unlikely in practice but it is not documented as impossible. This appears to be genuinely under-specified across the board.
- I did not find a primary source on how git itself orders commits with tied committer timestamps (`git log` default vs `--topo-order` vs `--date-order`). The topological-sort recommendation above is an inference from Dolt's commit-addressed model, not a cited git behaviour.

## Q4. Indexing a bitemporal table, and its interaction with a separate full-text index

### Takeaway
Range-bound indexes are the standard answer (MarkLogic literally defines an axis *as* a pair of range indexes; PostgreSQL uses GiST/SP-GiST on range columns), but the critical practical finding for SQLite+FTS5 is structural, not index-shaped: **FTS5 cannot filter on a non-FTS column**, and an FTS5 query without `MATCH` bypasses the index entirely. The temporal predicate and the text predicate must therefore meet in a join, and which side drives it decides your performance.

### Cited findings
- PostgreSQL: GiST and SP-GiST index range columns and accelerate `=`, `&&`, `<@`, `@>`, `<<`, `>>`, `-|-`, `&<`, `&>`; B-tree and hash indexes are "of limited use" on ranges, practically only equality, and mainly serve internal sorting/hashing — [PostgreSQL: Range Types](https://www.postgresql.org/docs/current/rangetypes.html)
  ```sql
  CREATE INDEX reservation_idx ON reservation USING GIST (during);
  ```
- MarkLogic: an axis *is* "a named pair of range indexes"; and `cts:period-range-query` "can be used only when there are range indexes for them on the database" — [MarkLogic: Introduction](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/intro.html); [MarkLogic: Searching temporal documents](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/searching.html)
- Bitemporal documents in MarkLogic use four range indexes over the four time values, and querying them and merging the results is central to bitemporal queries — [Progress: Seven Reasons to Use a Range Index in MarkLogic](https://www.progress.com/amp/seven-reasons-for-range-indexes/dWU5d09jQTFGZndVVnJod2xERzk5TENYb204PQ2)
- SQL Server: `FOR SYSTEM_TIME` predicates are *SARGable*; but applying `AT TIME ZONE` to a period column forces a table or index scan, and the doc explicitly tells you to write `<period column> {<|>|=} date_condition AT TIME ZONE 'UTC'` and **avoid** `<period column> AT TIME ZONE '<tz>' {<|>|=} date_condition` — [SQL Server: Temporal table usage scenarios](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/usage-scenarios)
- SQL Server physically separates current from history (two tables), with the default history table carrying a clustered rowstore B-tree, and recommends a **clustered columnstore index** on the history table for trend/DW workloads; the history table has "its own set of indexes and statistics" — [SQL Server: Temporal table usage scenarios](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/usage-scenarios)
- SQL Server: "If you search for non-current row versions only, query the history table directly for best query performance", and `CONTAINED IN` "is efficient because it only queries the history table" — [SQL Server: Query data](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/query-data); [usage scenarios](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/usage-scenarios)
- A SQL Server history table **cannot have a primary key** or any table/column constraints — it is the same columns "with all constraints removed" — [Temporal table considerations and limitations](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal-table-considerations-and-limitations)
- FTS5 external content tables: `content=` names a table/virtual table/view in the same database and `content_rowid=` names the rowid column (defaulting to literal `rowid`); FTS5 does **not** keep them in sync — you write `AFTER INSERT`/`AFTER DELETE`/`AFTER UPDATE` triggers, and the delete/update triggers must first issue the `'delete'` command with the **old** values. Triggers do not index pre-existing rows; use the `'rebuild'` command. On update/delete you must write the FTS table *before* the content table because FTS5 still needs to read the old row values — [SQLite FTS5](https://sqlite.org/fts5.html)
- FTS5: "Queries without MATCH go straight to the content table, so they ignore the FTS index"; queries with `MATCH` use the index then fetch column values from the content table, and if the two drift apart results look inconsistent — [SQLite FTS5](https://sqlite.org/fts5.html)
- FTS5 column filters (`colname : term`) restrict a MATCH to specific **FTS** columns; they do not filter by non-FTS columns, and the FTS5 documentation does not cover joins to other tables or filtering on non-FTS columns — [SQLite FTS5](https://sqlite.org/fts5.html)
- FTS5 ranking: `ORDER BY rank` gives the same result as `ORDER BY bm25(ft)`, and reading `rank` is faster than calling `bm25()` directly when sorting, especially with `LIMIT`; `rank MATCH 'bm25(...)'` (or `rank = '...'`) overrides the ranking function for one query — [SQLite FTS5](https://sqlite.org/fts5.html)
- FTS5 `columnsize` defaults to on and stores per-column token counts in a `<name>_docsize` table, which `bm25()` uses; `columnsize=0` saves space but makes `xColumnSize` count tokens on demand (slower), and on a contentless table with `columnsize=0` every insert needs an explicit rowid, `xColumnSize` returns -1, and **every query must be a full-text query** — [SQLite FTS5](https://sqlite.org/fts5.html)
- FTS5 `detail`: `full` (default) stores rowid+column+offset; `detail=column` drops offsets, breaking phrase and NEAR queries and slowing the highlight APIs; `detail=none` stores only rowids, additionally breaking column filters. The doc's measured index sizes on one email corpus: ~743 MiB (full), ~340 MiB (column), ~134 MiB (none) — [SQLite FTS5](https://sqlite.org/fts5.html)
- FTS5 contentless (`content=''`): no `UPDATE`/`DELETE`, inserts must supply a non-NULL rowid, `REPLACE` is treated as a plain `INSERT`, deletion only via the `'delete'` command, and reading any column but rowid returns NULL. `contentless_delete=1` (SQLite 3.43.0+) supports `DELETE` and `INSERT OR REPLACE`, supports `UPDATE` only when new values are given for *every* user-defined column, and does **not** support the `'delete'` command. `contentless_unindexed=1` requires a contentless or contentless-delete table and stores/returns only `UNINDEXED` column values — [SQLite FTS5](https://sqlite.org/fts5.html)
- The names `rowid` and `rank` cannot be used as FTS5 column names — [SQLite FTS5](https://sqlite.org/fts5.html)

### Inferences
- **Index on the integer, not the timestamp.** Since `seq` (Q3) is a dense integer and your `AS OF` resolves a timestamp to a `seq` in one cheap lookup, the version-visibility index should be over integers: store `system_from_seq` / `system_to_seq` alongside the text timestamps and index those. Integer key comparison in a SQLite B-tree is smaller and faster than fixed-width text, and it removes the collation/format risk entirely.
- **The "current row" fast path wants a partial index, and SQLite supports one.** Because you are using a sentinel rather than `NULL`, write the partial index predicate against the sentinel. A partial index on current rows is typically a small fraction of the table and turns "what is current" into an index-only scan.
- **FTS5 should index every version, with the temporal filter applied as a post-join.** The alternatives both fail: a current-only FTS index cannot answer "what was in force at T", and a second current-only index duplicates tokens and risks divergence. Index all versions in one external-content FTS5 table whose rowid *is* `passage.version_id`, then filter.
- **Direction of the join matters and FTS5 gives you no choice about it.** Because FTS5 cannot filter on `seq`, the plan is necessarily: run the MATCH, get candidate `version_id`s, join to `passage`, apply the temporal predicate. For a selective query that is fine; for a broad query over a long history the MATCH returns every version of every matching passage and most get discarded. Two mitigations: (a) put a coarse bucket (e.g. the year, or the commit `seq` band) into the FTS5 table as an *indexed* column so a column-filtered MATCH term can pre-narrow it — this works precisely because it is an FTS column rather than a joined column; (b) for "current only" searches, keep the temporal predicate as a `WHERE` on an indexed current-row partial index and rely on SQLite joining from the FTS side, measuring with `EXPLAIN QUERY PLAN`.
- **Relevance across versions is the real trap, not the index.** BM25 in FTS5 is computed over the whole FTS table, so if all versions are indexed, document frequency and average document length are computed across *versions*, not documents. A term that appears in every version of one heavily-revised document gets a depressed IDF. There is no FTS5 facility to scope BM25 to a filtered subset. The practical remedies are to (i) accept the skew, (ii) deduplicate after ranking — rank versions then keep the best-scoring version per `doc_id` — or (iii) maintain two FTS5 tables (one over current rows, one over all rows) and route the query by whether an `AS OF` was supplied, accepting the storage cost. Measure before choosing; `detail=column` roughly halves index size per the doc's own figures if you can give up phrase/NEAR.
- The external-content pattern is strictly better than duplicating text: FTS5 reads `body` from `passage` on demand, so the text is stored once. But the trigger ordering rule (write FTS before the content table on update/delete) is a correctness requirement, and in a bitemporal store you mostly *insert* new versions and *close* old ones rather than updating text — which means your triggers are simpler than the doc's general case, but the "close a version" update must not touch `body`, or it becomes a delete+insert in FTS terms.

### SQLite emulation **[design, not cited]**
```sql
-- Integer-keyed visibility, in addition to the human-readable timestamps.
ALTER TABLE passage ADD COLUMN system_from_seq INTEGER NOT NULL;
ALTER TABLE passage ADD COLUMN system_to_seq   INTEGER NOT NULL DEFAULT 2147483647;

-- AS OF (system time), driven by integers:
--   WHERE system_from_seq <= :seq AND :seq < system_to_seq
CREATE INDEX passage_sys_asof   ON passage(doc_id, system_from_seq, system_to_seq);
CREATE INDEX passage_valid_asof ON passage(doc_id, valid_from, valid_to);

-- Current-row fast path: partial indexes, cheap because they cover few rows.
CREATE INDEX passage_current
  ON passage(doc_id, passage_ord)
  WHERE system_to_seq = 2147483647
    AND valid_to      = '9999-12-31T00:00:00Z';

CREATE UNIQUE INDEX passage_current_unique
  ON passage(doc_id, passage_ord)
  WHERE system_to_seq = 2147483647
    AND valid_to      = '9999-12-31T00:00:00Z';   -- at most one current version

-- Lookup by the old name of a renamed/deleted path (Q6).
CREATE INDEX passage_path_hist ON passage(doc_path, system_from_seq);

-- FTS5 over EVERY version, external content, rowid == passage.version_id.
-- 'era' is an INDEXED FTS column so it can pre-narrow a MATCH; FTS5 cannot
-- filter on a joined column, so the only pre-filter available is an FTS term.
CREATE VIRTUAL TABLE passage_fts USING fts5(
  body,
  era,                             -- e.g. 'e2024 s0300' coarse bucket tokens
  content='passage',
  content_rowid='version_id',
  tokenize='unicode61 remove_diacritics 2'
);

-- Insert-only trigger set: a bitemporal store appends versions and closes old
-- ones; closing must not touch body, or FTS sees a delete+insert.
CREATE TRIGGER passage_ai AFTER INSERT ON passage BEGIN
  INSERT INTO passage_fts(rowid, body, era) VALUES (new.version_id, new.body, new.era);
END;
CREATE TRIGGER passage_ad AFTER DELETE ON passage BEGIN
  INSERT INTO passage_fts(passage_fts, rowid, body, era)
  VALUES('delete', old.version_id, old.body, old.era);
END;
CREATE TRIGGER passage_au AFTER UPDATE OF body, era ON passage BEGIN
  INSERT INTO passage_fts(passage_fts, rowid, body, era)
  VALUES('delete', old.version_id, old.body, old.era);
  INSERT INTO passage_fts(rowid, body, era) VALUES (new.version_id, new.body, new.era);
END;
-- after bulk import of pre-existing rows:
--   INSERT INTO passage_fts(passage_fts) VALUES('rebuild');

-- "Search as of T": resolve T -> :seq first (Q3), then join.
WITH hit AS (
  SELECT rowid AS version_id, rank
  FROM passage_fts
  WHERE passage_fts MATCH :q
)
SELECT p.doc_path, p.passage_ord, p.body, h.rank
FROM hit h
JOIN passage p ON p.version_id = h.version_id
WHERE p.system_from_seq <= :seq AND :seq < p.system_to_seq
  AND p.valid_from <= :t AND :t < p.valid_to
ORDER BY h.rank
LIMIT 50;

-- "Search current": same shape, sentinel predicate, hits the partial index.
WITH hit AS (
  SELECT rowid AS version_id, rank FROM passage_fts WHERE passage_fts MATCH :q
)
SELECT p.doc_path, p.body, h.rank
FROM hit h JOIN passage p ON p.version_id = h.version_id
WHERE p.system_to_seq = 2147483647
  AND p.valid_to = '9999-12-31T00:00:00Z'
ORDER BY h.rank LIMIT 50;

-- Best version per document, to blunt cross-version relevance skew.
SELECT doc_id, doc_path, body, MIN(rank) AS best
FROM ( /* the AS OF query above, without LIMIT */ )
GROUP BY doc_id
ORDER BY best LIMIT 50;
```

### Gaps
- The FTS5 documentation explicitly does not cover joining to another table or filtering by a non-FTS column, so there is no vendor guidance on which join direction the SQLite planner chooses for the pattern above. This must be settled empirically with `EXPLAIN QUERY PLAN` on your own data; I could not cite it.
- I found no documentation on how FTS5's BM25 global statistics behave when a table contains many near-duplicate versions of the same text. The IDF-skew concern above is an inference from how BM25 is defined, not a documented FTS5 behaviour.
- I read only the first ~100k characters of the FTS5 page (it is ~172k); the remainder may contain further relevant material on the `'rebuild'`/`'optimize'`/`'merge'` commands and tokenizer details.

## Q5. Combining time-travel with full-text search in real systems

### Takeaway
MarkLogic is the closest analogue and it resolves the problem by **storing every version as a separate document in one searchable database**, then narrowing with collection and period queries — meaning the text index naturally covers all versions and relevance is computed per version-document. At the other extreme, SQL Server's physical split into current + constraint-free history table makes a single full-text index over all versions structurally awkward. XTDB, Datomic, Dolt and TerminusDB I could not confirm text-search-across-versions behaviour for from primary docs.

### Cited findings — MarkLogic (closest analogue)
- "A temporal document is stored as a series of versioned documents in a protected collection. The original insert never changes. Updates are inserted as new documents with different valid and system times, and deletes are also inserted as new documents." — [MarkLogic: Introduction to bitemporal](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/intro.html)
- "A split creates a new document with the same content but different valid timestamps and a different system end time." — same source
- "Each document gets its own URI collection, which holds all of its versions. The `latest` collection holds the latest version of each document. Deleting a temporal document removes it from the `latest` collection." — same source
- A temporal collection is a *protected* collection whose documents share the same axes, defined by the same range indexes; `temporal-admin` or `admin` is required to create axes and temporal collections, and permission changes affect only the latest version — same source
- Valid and system timestamps plus archival information live in document **metadata** (string key/value pairs outside the root node), kept in all fragments of the document — same source
- `Wipe` deletes all versions of a temporal document; `Protect` prevents operations such as temporal update; WORM support lets you set an archive date after which documents can move to write-once storage — same source
- `cts:period-range-query(X, operator, Y)` matches documents by a named axis (`"valid"`, `"system"`) against a period value and requires range indexes. Examples: `cts.periodRangeQuery("valid", "ALN_BEFORE", ...)`; `cts.periodRangeQuery("valid", "ALN_MEETS", period)` — [MarkLogic: Searching temporal documents](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/searching.html)
- `cts:period-compare-query(X, operator, Y)` compares **two axes** of the same document, and `cts.periodCompare(period1, "ALN_MEETS", period2)` compares two literal periods returning a boolean — same source
- Two operator families: Allen operators prefixed `ALN_` (more restrictive) and ISO SQL:2011 operators prefixed `ISO_` (some map to several Allen relations). The guide's worked example uses `ISO_CONTAINS` to find "the order version valid at 11:30 when queried at 11:51", returning Version 2 — i.e. the canonical bitemporal point query — same source
- Operator list includes `aln_equals`, `aln_contained_by`, `aln_meets`, `aln_overlapped_by`, `aln_before`, plus `iso_contains`, `iso_overlaps`, `iso_succeeds`, `iso_precedes` — [MarkLogic: cts:period-compare-query](https://docs.marklogic.com/cts:period-compare-query)
- **Caution on the docs themselves:** the `ALN_CONTAINS` definition on the `cts:period-compare-query` page reads "period1 starts before period2 starts and ends before period2 ends", which is not Allen's `contains` (that requires period1 to end *after* period2). The page also shows an `"aln_overlaps"` example using an operator absent from its own operator list. Verify against data before relying on either — [MarkLogic: cts:period-compare-query](https://docs.marklogic.com/cts:period-compare-query)
- `cts:lsqt-query` restricts results to documents at or before LSQT for stable query results — [MarkLogic: Searching temporal documents](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/searching.html)

### Cited findings — other systems
- **SQL Server**: history lives in a separate table that cannot have a primary key or any constraints — [Temporal table considerations and limitations](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal-table-considerations-and-limitations). Temporal and history tables also cannot use FileTable or FILESTREAM — same source. `FOR SYSTEM_TIME AS OF` can be applied to a **view** that joins several temporal tables, and SQL Server pushes `AS OF` transparently into every temporal table in the view; temporal and non-temporal tables can be mixed in one view and `AS OF` applies only to the temporal ones; applying a temporal clause to a view referencing no temporal table is an error — [SQL Server: Query data](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/query-data)
- **PostgreSQL**: no SQL:2011 temporal syntax in core as of the material I read; community patches cover `PERIOD`s, temporal `PRIMARY KEY`/`UNIQUE`, `UPDATE`/`DELETE FOR PORTION OF`, and temporal `FOREIGN KEY`s, and accept range columns wherever the standard accepts a `PERIOD`. The `temporal_tables` extension covers only system-period tables and "does not follow the SQL:2011 design" — [pgsql-hackers, "SQL:2011 application time"](https://www.postgresql.org/message-id/CADkLM=ekdXZceMyggb6Lqj2447VfiS-Bw22Gdgd5tBotfKmi6A@mail.gmail.com); [PostgreSQL wiki: SQL2011Temporal](https://wiki.postgresql.org/wiki/SQL2011Temporal). Range columns index with GiST/SP-GiST and non-overlap is enforced with `EXCLUDE USING GIST (key WITH =, during WITH &&)` + `btree_gist` — [PostgreSQL: Range Types](https://www.postgresql.org/docs/current/rangetypes.html)
- **XTDB**: four named bound columns; `FOR VALID_TIME AS OF DATE '2023-08-01'`, `FOR VALID_TIME BETWEEN ... AND ...`, `FOR ALL VALID_TIME`, `FOR SYSTEM_TIME AS OF ...`, `FOR SYSTEM_TIME BETWEEN ... AND ...`, `FOR ALL SYSTEM_TIME`, and clauses combine (`FOR ALL SYSTEM_TIME FOR ALL VALID_TIME`). No clause = current system time × current valid time, so plain SQL behaves atemporally. Valid-time edits use `FOR PORTION OF VALID_TIME FROM ... TO ...` — [XTDB: Time in XTDB](https://docs.xtdb.com/about/time-in-xtdb). XTDB v2 exposes SQL:2011 bitemporal support over HTTP and the Postgres wire protocol — [XTDB: launching v2](https://www.xtdb.com/blog/launching-xtdb-v2)
- **XTDB doc inconsistencies worth knowing**: the "Time in XTDB" page's worked example writes `UPDATE addresses FOR VALID_TIME FROM TIMESTAMP '2025-09-01Z' SET ...`, inconsistent with the `FOR PORTION OF VALID_TIME` syntax it documents — [XTDB: Time in XTDB](https://docs.xtdb.com/about/time-in-xtdb). A forum thread records that v2 used `FOR ALL SYSTEM_TIME` where SQL Server uses `FOR SYSTEM_TIME ALL`, acknowledged by maintainers as an oversight — [XTDB discuss: V2 SQL FOR ALL SYSTEM_TIME](https://discuss.xtdb.com/t/v2-sql-for-all-system-time-or-for-system-time-all/323). Another reports an error using timestamp arithmetic (`CURRENT_TIMESTAMP - interval`) inside `FOR VALID_TIME AS OF` on a 2.0.0-SNAPSHOT — [XTDB discuss: timestamp arithmetic](https://discuss.xtdb.com/t/error-querying-valid-time-with-timestamp-arithmetic/398)
- **Datomic**: `asOf` and `since` are *filters* applied to database indexes, filtering out data that does not match the predicate, and "cannot do anything that could not be done directly with indexes". `with` + `as-of` always means "with followed by as-of" regardless of call order, and does not let you branch the past. A `since` filter may not see lookup identifiers established in an entity's first transaction, so the idiom is to resolve the entity on the unfiltered db and pull attributes from the `since` db. An entity is a point-in-time view and cannot be created from a `history` database — [Datomic: Database Filters](https://docs.datomic.com/time/filters.html)
- **Dolt**: `AS OF` accepts commit hash / branch / timestamp, per-table; `dolt_history_$TABLENAME` gives per-row history with `commit_hash`, `committer`, `commit_date`; `dolt_commit_diff` / `dolt_diff` for commit-to-commit change sets; views under `AS OF` use the current definition with historical rows — [Dolt: Querying history](https://docs.dolthub.com/sql-reference/version-control/querying-history); [Dolt: System tables](https://docs.dolthub.com/sql-reference/version-control/dolt-system-tables)

### Inferences
- MarkLogic's answer to "does the text index cover all versions?" follows from its storage model rather than from a special text-index feature: because every version is an ordinary document in the same database, the universal text index necessarily covers all versions, and narrowing to "current" is done the same way as any other narrowing — a collection query on `latest`. Relevance is therefore computed per *version-document*, so all versions of one document compete against each other in one result set unless you constrain the collection or the period axis. This is a strong architectural argument for the single-table, all-versions-indexed, filter-afterwards design in Q4, and it is also a warning: MarkLogic has the same cross-version relevance competition you will have.
- MarkLogic's `latest` collection is the direct analogue of a current-row partial index, and `cts:lsqt-query` of the watermark filter.
- SQL Server is the *worst* model to copy here. Full-text indexing in SQL Server requires a unique single-column key index on the table, and the history table is documented as having no primary key and no constraints — so a full-text index over history is at best awkward and likely impossible without copying history elsewhere. I could not find a doc page stating this directly (see Gaps), so treat it as a strong inference, not a fact. The transferable lesson is: do not physically split current from history if you need one text index over both.
- Datomic's "filters cannot do anything that could not be done directly with indexes" is the key sentence: it implies any index not reachable by datom traversal (a Lucene text index is a plausible example) is outside the filter mechanism's guarantees. This is suggestive but not conclusive.

### Gaps
- **TerminusDB: not researched in this pass.** I have no citable findings on its time-travel model or text search.
- **Datomic + fulltext under `as-of`**: the Database Filters page does not mention fulltext at all — no statement either way about whether a `:db/fulltext` query respects an `as-of` filter, and no "filters are not applied" caveat. A Clojurians log has a maintainer calling Lucene "an implementation detail" and a user noting fulltext indexing works only with the default Lucene tokenizer, but these are forum comments, not documentation. **Unresolved.**
- **XTDB text search**: I found no XTDB documentation on full-text search in v2. (v1-era Lucene module behaviour I could not confirm from a primary source.) **Unresolved.**
- **Dolt full-text indexes under `AS OF`**: Dolt supports MySQL-compatible `FULLTEXT` indexes, but I did not verify this nor how such an index behaves under `AS OF`. **Unresolved.**
- **SQL Server full-text on a temporal/history table**: the temporal limitations page does not mention full-text indexes, and I found no doc page confirming or denying the combination. **Unresolved** — my reasoning from the "no PK/no constraints on history" rule is inference only.
- **MarkLogic text index scope across versions**: neither the Introduction nor the Searching chapter states how the text index or relevance scoring treats multiple versions. The `cts.lsqtQueryWeight` function's existence hints that LSQT queries carry a relevance weight, but the pages do not explain the scoring model. **Partially unresolved.**

## Q6. Known pitfalls

### Takeaway
Four confirmed traps: "as of" means different things in SQL Server (as *known* at) and Oracle (as *valid* at); boundary inclusivity differs between `FROM...TO` and `BETWEEN...AND` *within one product* (MariaDB documents the difference explicitly); system-time-derived validity is simply wrong when loads are delayed; and identity change (your 98 renamed/deleted paths) is unaddressed by every temporal feature I examined, because all of them key history on a primary key that cannot itself change.

### Cited findings — the "as of" ambiguity
- SQL Server's `FOR SYSTEM_TIME AS OF <date_time>` reconstructs "the state of data as it was at any specific time in the past" on the **system/transaction** axis, with precision limited by the `datetime2` precision of the period columns; literals and variables are interpreted as UTC — [SQL Server: Query data](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/query-data)
- Oracle's `AS OF PERIOD FOR <period_name> <timestamp>` queries the **user-defined valid-time** dimension, not database commit history:
  ```sql
  SELECT employee_id FROM employees_temp AS OF PERIOD FOR
         emp_track_time TIMESTAMP '2012-12-01 12:00:01 Europe/Paris';
  ```
  Oracle's docs describe valid-time flashback as "as-of and versions queries run against a user-defined valid-time dimension, rather than against database commit history" — [Oracle VLDB Guide 5.3](https://docs.oracle.com/en/database/oracle/oracle-database/18/vldbg/control-validity-visibility-data.html)
- Oracle additionally has a *session-level* valid-time visibility switch, which silently changes what every subsequent query returns:
  ```sql
  EXECUTE DBMS_FLASHBACK_ARCHIVE.enable_at_valid_time('ASOF', '31-DEC-12 12.00.01 PM');
  EXECUTE DBMS_FLASHBACK_ARCHIVE.enable_at_valid_time('CURRENT');
  EXECUTE DBMS_FLASHBACK_ARCHIVE.enable_at_valid_time('ALL');   -- the default
  ```
  `'CURRENT'` shows only currently valid data within the valid-time period; `'ALL'` shows the full table and is the default — [Oracle VLDB Guide 5.3](https://docs.oracle.com/en/database/oracle/oracle-database/18/vldbg/control-validity-visibility-data.html)
- XTDB disambiguates by naming the axis in the clause: `FOR VALID_TIME AS OF` vs `FOR SYSTEM_TIME AS OF`; "as of now" is the valid-time default and "as best known" the system-time default — [XTDB: Time in XTDB](https://docs.xtdb.com/about/time-in-xtdb)

### Cited findings — boundary inclusivity within one product
- MariaDB, same table, same product: `BETWEEN start AND end` is **inclusive at both ends**, while `FROM start TO end` **includes `start` and excludes `end`**. The docs' own summary: "`BETWEEN ... AND` includes rows visible exactly at `end`. `FROM ... TO` excludes them." Without any `FOR SYSTEM_TIME` clause the table shows current data, "usually the same as `AS OF CURRENT_TIMESTAMP`" — [MariaDB: System-versioned tables](https://mariadb.com/docs/server/reference/sql-structure/temporal-tables/system-versioned-tables)
  ```sql
  SELECT * FROM t FOR SYSTEM_TIME FROM '2016-01-01 00:00:00' TO '2017-01-01 00:00:00';
  ```
- SQL Server: `FROM...TO` and `BETWEEN...AND` "return row versions that overlap with a specified period (that is, those that started before the given period and ended after it), while `CONTAINED IN` returns only those that existed within the specified period boundaries" — [SQL Server: Query data](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/query-data)
- SQL Server's `BETWEEN` upper bound is **inclusive**: the docs describe a `BETWEEN '2021-01-01' AND '2022-01-01'` query as returning versions active for part of that period "(including the upper boundary)" — [SQL Server: Temporal table usage scenarios](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/usage-scenarios)
- `BETWEEN` returns **multiple rows per key**, so aggregating over it double-counts entities: "When an employee's salary changed, the query returns multiple versions for that employee, and each version contributes to the aggregates." — [SQL Server: Temporal table usage scenarios](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/usage-scenarios)
- PostgreSQL silently rewrites bounds you supply on discrete ranges — `int8range(1, 14, '(]')` displays as `[2,15)`, and `[,]` becomes `(,)` — while leaving `timestamp` ranges exactly as given, so the same `[a,b]` literal means different things depending on the element type — [PostgreSQL: Range Types](https://www.postgresql.org/docs/current/rangetypes.html)

### Cited findings — backdated corrections and out-of-order arrival
- SQL Server states the limitation plainly: using system-versioned temporal tables for slowly-changing dimensions "is acceptable if the period of validity calculated based on database transaction time works for your business logic. If you load data with a significant delay, transaction time might not be acceptable." It adds that by default historical data cannot be changed after loading (you must set `SYSTEM_VERSIONING = OFF` first), which "might be a limitation in cases where changing historical data happens regularly", and that a row version is generated on **any** column change unless the ETL suppresses it — [SQL Server: Temporal table usage scenarios](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal/usage-scenarios)
- XTDB: "Backdated and future-dated changes are recorded with a valid time earlier or later than the time of recording. The original belief stays in system time, so both views remain queryable." Valid time is derived from the transaction's system time *unless specified otherwise*, and documents may be written with past or future valid time — [XTDB: Time in XTDB](https://docs.xtdb.com/about/time-in-xtdb); [XTDB v1: Bitemporality](https://v1-docs.xtdb.com/concepts/bitemporality/)
- MarkLogic's LSQT exists precisely to make out-of-order arrival safe: documents with system start time before LSQT are queryable, those after it are still ingestible/updatable — [MarkLogic: Introduction](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/intro.html)
- SQL:2011's `FOR PORTION OF` `UPDATE`/`DELETE` split periods automatically, which is how a backdated correction to part of a valid-time interval is expressed in the standard — [Survey of SQL:2011 Temporal Features](https://illuminatedcomputing.com/posts/2019/08/sql2011-survey/)
- MarkLogic's split operation "creates a new document with the same content but different valid timestamps and a different system end time" — the document-store equivalent — [MarkLogic: Introduction](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/intro.html)

### Cited findings — identity change (renames and deletes)
- SQL Server requires the current table to have a primary key "in order to correlate records between the current table and the history table" — history is keyed on the PK, and the PK is not itself versioned — [Temporal table considerations and limitations](https://learn.microsoft.com/en-us/sql/relational-databases/tables/temporal-table-considerations-and-limitations)
- MarkLogic's unit of history is the document URI: "Each document gets its own URI collection, which holds all of its versions", and deleting a temporal document removes it from `latest` (the versions remain in the URI collection) — [MarkLogic: Introduction](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/intro.html)
- MarkLogic `Wipe` is the only operation that removes all versions of a temporal document — [MarkLogic: Introduction](https://docs.progress.com/bundle/marklogic-server-use-bi-temporal-features-12/page/topics/intro.html)
- Dolt's `AS OF` on a view uses the **current** view definition with historical rows; to get the historical definition you must check out the database at that commit — the schema-identity analogue of the rename problem — [Dolt: Querying history](https://docs.dolthub.com/sql-reference/version-control/querying-history)
- Dolt's `AS OF` does not see uncommitted working-set changes — [Dolt: Querying history](https://docs.dolthub.com/sql-reference/version-control/querying-history)

### Inferences
- **Name the axis in every API you expose.** Given that SQL Server's `AS OF` and Oracle's `AS OF PERIOD FOR` mean opposite things, never accept a bare `as_of` parameter. Take `as_known_at` and `as_valid_at` separately, default both to "now", and say which one a result was computed under.
- **Pick one window form and never offer the other.** The MariaDB `FROM...TO` / `BETWEEN...AND` discrepancy is a genuine within-product footgun. Expose only the `[a, b)` overlap form (`from < :b AND to > :a`) and document it as half-open.
- **Window queries are not point queries.** Per SQL Server's own aggregate warning, any window form returns multiple versions per document, so a search over a window must deduplicate per `doc_id` before counting or ranking, or one heavily-revised document dominates the result set.
- **For renames, separate identity from name — the temporal features will not do it for you.** Every system above keys history on something immutable (SQL Server's PK, MarkLogic's URI, Dolt's commit+PK). A path that changes is therefore *not* the key. The schema in Q1 already splits `doc_id` (stable) from `doc_path` (versioned attribute). Your 98 renamed/deleted paths then become answerable: resolve an old name to a `doc_id` by searching the historical `doc_path` values, then run the normal temporal query on `doc_id`.
- **Deletion must be a closing row, not a `DELETE`.** Following MarkLogic ("deletes are also inserted as new documents") and XTDB (the existing row stays reachable to historical queries), a deleted path's final act is to set `system_to_seq`/`valid_to` on the live version — and, if you need to distinguish "deleted" from "merely superseded", to insert a tombstone version carrying `is_deleted = 1`. Only then does "as of T, this document existed and said X" remain answerable after the delete.
- **A rename is ambiguous on the valid-time axis and you must choose.** Either (a) the document always had its new name and the old name was a recording error — a *system-time* correction, closing the old row and opening a new one with the same `valid_from`; or (b) the document genuinely was called the old name until the rename — a *valid-time* change, closing `[old_valid_from, rename_instant)` and opening `[rename_instant, ∞)`. Git renames are case (b). Getting this backwards makes "what was this file called in March?" return the wrong answer while "what is it called now?" stays right, so the bug hides.

### SQLite emulation **[design, not cited]**
```sql
-- Identity vs name, so a rename is a new version and not a new document.
CREATE TABLE document (
  doc_id      INTEGER PRIMARY KEY,
  first_seen  INTEGER NOT NULL          -- commit_seq of the add
) STRICT;

-- doc_path lives on the version row (see Q1), so every name a document ever
-- had is recoverable. Deletion is a closing row plus a tombstone, never a DELETE.
ALTER TABLE passage ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0
  CHECK (is_deleted IN (0,1));

-- "People still ask about the old name": old path -> doc_id, any era.
SELECT DISTINCT doc_id FROM passage WHERE doc_path = :old_path;

-- Then the normal AS OF query on doc_id, excluding tombstones.
SELECT doc_path, passage_ord, body
FROM passage
WHERE doc_id = :doc_id
  AND is_deleted = 0
  AND system_from_seq <= :seq AND :seq < system_to_seq
  AND valid_from <= :t AND :t < valid_to
ORDER BY passage_ord;

-- Rename as a VALID-TIME change (the git case): close the old interval at the
-- rename instant, open a new one with the new path. No row is ever overwritten.
UPDATE passage
   SET valid_to = :rename_instant
 WHERE doc_id = :doc_id
   AND valid_to = '9999-12-31T00:00:00Z';

INSERT INTO passage (doc_id, doc_path, passage_ord,
                     valid_from, valid_to,
                     system_from, system_to,
                     system_from_seq, system_to_seq, seq, body)
SELECT doc_id, :new_path, passage_ord,
       :rename_instant, '9999-12-31T00:00:00Z',
       :now, '9999-12-31T00:00:00Z',
       :seq, 2147483647, :seq, body
  FROM passage
 WHERE doc_id = :doc_id AND valid_to = :rename_instant;

-- Backdated correction = SQL:2011 FOR PORTION OF, by hand: close the system-time
-- interval of the wrong belief, then insert the corrected valid-time slices.
UPDATE passage SET system_to_seq = :seq, system_to = :now
 WHERE version_id = :wrong_version_id;
-- ... then INSERT one row per surviving valid-time slice (before, corrected, after).
```

### Gaps
- SQL Server's `FROM...TO` upper-bound exclusivity is not stated verbatim on either page I fetched; both pages only group `FROM...TO` with `BETWEEN...AND` as "overlap" forms, and only `BETWEEN`'s upper-inclusivity is spelled out ("including the upper boundary"). The SQL Server `FROM` clause reference page would need checking for the exact inclusivity table. MariaDB's difference *is* documented verbatim and is the citable instance of this pitfall.
- Neither Oracle page I read states how a `NULL` valid-time start or end is interpreted. The VLDB page is silent; the SQL Language Reference and Development Guide would be needed.
- `CONTAINED IN` inclusivity: SQL Server says only "existed within the specified period boundaries", without stating whether the boundaries themselves are inclusive. Unverified.
- I found no vendor documentation of any kind addressing key/identity change in a temporal table — no product I examined offers a "this key was renamed" facility. The identity-vs-name split above is a standard modelling response, not a cited pattern.
