# Git history as a transaction-time source: ordering and reconstruction pitfalls

Scope note: every numbered experiment below was run by me in this session on **git 2.55.0, macOS 26.5, 2026-10-24**, against throwaway local repositories and against `https://github.com/git/git`. Experiments are labelled **[measured]** and the exact commands are given so the report writer can reproduce or drop them. Everything else carries a documentation URL. Where the official docs are silent on a behaviour that is widely assumed, I say so explicitly rather than citing a blog as if it were normative.

## Author date vs committer date: semantics, monotonicity, and what rewrites them

### Takeaway

A commit carries two independent timestamps, both attacker- and user-settable via environment variables, and neither is guaranteed monotonic along a branch: the author date travels with the patch (so it can predate its own parent), while the committer date is refreshed to "now" by every operation that creates a new commit object (rebase, cherry-pick, amend, squash). Git's own documentation states only that "the history walking machinery assumes that commits have non-decreasing commit timestamps" — an assumption, not an invariant.

### Cited Findings

- Both identities and both timestamps are taken from the environment if set: `GIT_AUTHOR_NAME`, `GIT_AUTHOR_EMAIL`, `GIT_AUTHOR_DATE`, `GIT_COMMITTER_NAME`, `GIT_COMMITTER_EMAIL`, `GIT_COMMITTER_DATE` — [git-commit, COMMIT INFORMATION](https://git-scm.com/docs/git-commit). The committer date is settable only through `GIT_COMMITTER_DATE`; the page does not document what it is when unset.
- `git commit --date=<date>` "Override the author date used in the commit." — [git-commit](https://git-scm.com/docs/git-commit)
- `git commit --amend`: "The new commit has the same parents and author as the current one (the `--reset-author` option can countermand this)." The page names the *author* but says nothing about the author *timestamp* or about the committer date — [git-commit](https://git-scm.com/docs/git-commit)
- `--reset-author`: "When used with `-C`/`-c`/`--amend` options, or when committing after a conflicting cherry-pick, declare that the authorship of the resulting commit now belongs to the committer. This also renews the author timestamp." — [git-commit](https://git-scm.com/docs/git-commit)
- `git commit -C <commit>` "Take an existing <commit> object, and reuse the log message and the authorship information (including the timestamp) when creating the commit." — [git-commit](https://git-scm.com/docs/git-commit)
- Rebase explicitly documents that the committer date defaults to wall-clock now: `--committer-date-is-author-date` — "Instead of using the current time as the committer date, use the author date of the commit being rebased as the committer date. This option implies `--force-rebase`." — [git-rebase](https://git-scm.com/docs/git-rebase)
- The same entry is the clearest statement in the official docs that date monotonicity is only an assumption: "The history walking machinery assumes that commits have non-decreasing commit timestamps. You should consider if you really need to use this option." — [git-rebase](https://git-scm.com/docs/git-rebase)
- `--ignore-date` / `--reset-author-date`: "Instead of using the author date of the original commit, use the current time as the author date of the rebased commit. This option implies `--force-rebase`." — [git-rebase](https://git-scm.com/docs/git-rebase)
- Rebase replays commits: "Replay the commits, one by one, in order. This is similar to running `git cherry-pick <commit>` for each commit"; `--force-rebase` "ensures that the entire history of the rebased branch is composed of new commits." — [git-rebase](https://git-scm.com/docs/git-rebase)
- Cherry-pick is documented as creating new objects: "Given one or more existing commits, apply the change each one introduces, recording a new commit for each." The page says **nothing** about dates, and only implies retained authorship via the `--no-commit` note ("what you are building is your own work … rather than a reproduction of the original commit") — [git-cherry-pick](https://git-scm.com/docs/git-cherry-pick)
- Interactive rebase folding: "If the commits had different authors, the folded commit will be attributed to the author of the first commit." No statement about the folded commit's dates — [git-rebase, INTERACTIVE MODE](https://git-scm.com/docs/git-rebase)
- `git filter-branch` exports `GIT_AUTHOR_DATE` and `GIT_COMMITTER_DATE` "from the current commit … in order to affect the author and committer identities of the replacement commit", and `--env-filter` exists precisely so you "might want to rewrite the author/committer name/email/time environment variables" — [git-filter-branch](https://git-scm.com/docs/git-filter-branch)
- filter-branch's warning is unambiguous about identity loss: "The rewritten history will have different object names for all the objects and will not converge with the original branch." Originals are retained under `refs/original/`, but "The backup in refs/original/ is not a real backup; it dereferences tags first." Git's own docs now recommend [git filter-repo](https://github.com/newren/git-filter-repo/) instead — [git-filter-branch](https://git-scm.com/docs/git-filter-branch)
- **[measured] Experiment 1 — plain commit, amend, cherry-pick, rebase variants.** In a fresh repo with both dates forced to 2020-01-01 (`base`) and 2021-06-01 (`feat`):
  - `git cherry-pick main` onto `main~1` produced a new SHA with `A=2021-06-01 00:00:00 +0000 C=2026-10-24 20:32:39 -0400` — author date preserved, committer date reset to now.
  - `git rebase --force-rebase main~1` produced exactly the same split (`A=2021-06-01`, `C=` now).
  - `git rebase --committer-date-is-author-date main~1` reproduced the *original* commit hash `c900d4d` with both dates at 2021-06-01 — i.e. this flag makes the replay bit-identical.
  - `git rebase --ignore-date main~1` produced `A=2026-10-24 … C=2026-10-24 …` — the original authoring time is destroyed, not merely shadowed.
  - `git commit --amend -m …` (earlier run) kept `A=2021-06-01` and set `C=` now.
- **[measured] Experiment 2 — author date is not monotonic in real upstream history.** `git clone --depth 3 --single-branch https://github.com/git/git.git` then `git log --format='%h parent=%p A=%ad C=%cd' --date=iso -3`:
  ```
  7e34d81 parent=7ac84b7           A=2026-10-22 07:22:36 -0700  C=2026-10-22 07:23:16 -0700
  7ac84b7 parent=8f352ee 60e16b0   A=2026-10-22 07:23:16 -0700  C=2026-10-22 07:23:16 -0700
  ```
  The child `7e34d81` has an author date 40 s **earlier** than its parent (a merge commit). Author date decreases as you walk forward along the branch. Committer dates are equal here, so committer date is non-strictly monotonic at best — ties are normal, not exceptional, because an integrator's batch of commits all land in the same second.

### Inferences

- For a bitemporal model, **author date is a valid-time-ish field and committer date is the closest thing to transaction time** — but committer date records when the object was *created*, not when it became visible in the archive (that is the push/ref-update moment, which commits do not record at all). Any claim of the form "the archive said X at time T" derived from `%cd` is really "someone constructed this object at T", which is weaker.
- Because every replay operation sets a fresh committer date while preserving author date, a repository that has been rebased or squash-merged has committer dates clustered at integration time and author dates scattered across the real past. Picking either one alone gives a systematically wrong transaction axis; the two disagree by exactly the review latency.
- `--committer-date-is-author-date` reproducing the original SHA (Experiment 1) means the pair (tree, parents, author, committer, message) is the identity of a commit: any date rewrite is indistinguishable from a content change at the hash level, so you cannot detect "same change, rewritten dates" by hash comparison — only by patch-id (`git patch-id`) or by content hashing of trees.

### Gaps

- The official `git-commit` page never states that the committer date defaults to the current time, nor that `--amend` preserves the author timestamp; both are only implied (by `--reset-author`'s "This also renews the author timestamp" and by `git-rebase`'s "Instead of using the current time as the committer date"). My Experiment 1 confirms the behaviour empirically, but I found no normative sentence to cite for it.
- `git-cherry-pick` documents no date behaviour at all; the "author preserved, committer reset" behaviour is from measurement only.
- I did not find a documented statement about whether GitHub/GitLab's *server-side* squash or rebase buttons set the author date to the original authoring time or to the merge time (see the squash section).

## Is `git log` order chronological? What topological order does and does not guarantee

### Takeaway

The default `git log` order is reverse chronological by **commit timestamp**, with a parents-after-children constraint; topological order guarantees only ancestry (and non-interleaving of lines of history) and explicitly *sacrifices* date order. Worse, date-based limiting (`--since`/`--until`) **stops traversal** at the first commit older than the date, so one skewed commit near the tip can silently truncate an as-of query to nothing.

### Cited Findings

- "By default, the commits are shown in reverse chronological order." — [git-log, Commit Ordering](https://git-scm.com/docs/git-log)
- `--date-order`: "Show no parents before all of its children are shown, but otherwise show commits in the commit timestamp order." `--author-date-order`: same, "in the author timestamp order." — [git-log](https://git-scm.com/docs/git-log)
- `--topo-order`: "Show no parents before all of its children are shown, and avoid showing commits on multiple lines of history intermixed." The docs' own worked example, with numbers denoting commit-timestamp order:
  ```
  ---1----2----4----7
     \               \
      3----5----6----8---
  ```
  "`--date-order` show the commits in the timestamp order: 8 7 6 5 4 3 2 1. With `--topo-order`, they would show 8 6 5 3 7 4 2 1 (or 8 7 4 2 6 5 3 1); some older commits are shown before newer ones in order to avoid showing the commits from two parallel development track mixed together." — [git-log](https://git-scm.com/docs/git-log)
- Note the parenthetical "(or 8 7 4 2 6 5 3 1)": **topological order is not unique**, so `--topo-order` is not a stable key for reproducible indexing across git versions or repack states — [git-log](https://git-scm.com/docs/git-log)
- `--graph` "implies the `--topo-order` option by default" — [git-log](https://git-scm.com/docs/git-log)
- `--since-as-filter=<date>`: "Show all commits more recent than <date>. This visits all commits in the range, rather than stopping at the first commit which is older than <date>." The existence of this flag is the documentation of `--since`'s cutoff behaviour; the `--since` entry itself only says "Show commits more recent than <date>." — [git-rev-list](https://git-scm.com/docs/git-rev-list)
- Git's own commit-graph design treats date comparison as a defeasible heuristic: "If A and B are commits with commit time X and Y, respectively, and X < Y, then A *probably* cannot reach B" — used only when the computation "is allowed to violate topological relationships due to clock skew", and "not used when the topological order is required." — [Documentation/technical/commit-graph.adoc](https://raw.githubusercontent.com/git/git/master/Documentation/technical/commit-graph.adoc)
- The replacement invariant is generation numbers, not dates: "If A and B are commits with generation numbers N and M, respectively, and N <= M, then A cannot reach B." Corrected committer dates (generation v2) are defined as at least one more than the max over parents, and "a root commit with timestamp zero has corrected commit date of 1". Commits absent from the graph are treated as having infinite generation number, forcing the weaker strict-inequality form — [commit-graph.adoc](https://raw.githubusercontent.com/git/git/master/Documentation/technical/commit-graph.adoc)
- **[measured] Experiment 3 — one skewed commit destroys an as-of query.** Fresh repo, commits at 2020-01-01 (`base`) and 2021-06-01 (`feat amended`), then a *new tip* committed with both dates forced to 2019-01-01 (`skewed`):
  ```
  $ git log --since=2020-01-01 --format='%h %s'      # → prints NOTHING
  $ git log --since-as-filter=2020-01-01 --format='%h %s'
  bf62d6b feat amended
  38fb7df base
  ```
  `--since` traverses from the tip, hits the 2019 commit immediately, and stops — two newer commits are invisible.
- **[measured] Experiment 3b — "as of" returns the future.** In that same skewed repo:
  ```
  $ git rev-list -n1 --before=2021-01-01 skew
  402f5983ea8e7a2623ffe59b53fac4ae2e30c43d      # the skewed TIP, i.e. the newest state
  ```
  The canonical "what did the archive say on 2021-01-01" idiom (`git rev-list -n1 --before=<date> <branch>`) returned the *latest* commit on the branch, whose tree contains content added after that date.
- **[measured] Experiment 3c — ordering flags do not rescue it.** For the same skewed branch, `git log` (default), `git log --date-order` and `git log --topo-order` all printed the identical sequence `402f598 (2019) → bf62d6b (2026) → 38fb7df (2026)`: the parents-after-children rule forces the skewed tip first, so no ordering flag yields date-sorted output when the DAG and the dates disagree.

### Inferences

- The documented semantics mean `git log` output is "a topological sort tie-broken by commit date", not "a chronological list". Treating row order as a time series is only safe if (a) you also verify non-decreasing `%ct`, and (b) you accept that merges linearise history in an order no one ever observed.
- A merge commit's committer date is the integration moment, but the commits beneath it can have arbitrarily old dates; so a merge makes a large block of history become visible at one instant. Any transaction-time reconstruction that ignores merges will show content as "present" long before it was reachable from the branch the archive publishes.
- Practical rule for an as-of query: never use `--since`/`--before` for correctness. Enumerate commits with `git rev-list --first-parent <branch>`, read `%ct` for each, and do the time selection yourself after sorting — and audit for `%ct` inversions (`git rev-list --format='%ct' --first-parent` then check monotonicity) rather than assuming it.
- Generation numbers in the commit-graph are the only ancestry-safe ordering primitive git exposes, but they are a *reachability* order, not a time order; they cannot answer "what did it say on date D" either.

### Gaps

- I found no official statement quantifying how common clock skew is in the wild, nor a git doc sentence of the form "commit dates may be arbitrary"; the strongest normative hints are the commit-graph "probably"/"clock skew" language and git-rebase's "assumes … non-decreasing".
- The second half of the `git-log` and `git-rev-list` pages was truncated by the fetch tool (162 k and 109 k characters; ~100 k read), so there may be further relevant notes on date handling I did not see.

## Squash merges and bundled commits: states the archive passed through that no commit records

### Takeaway

Squash merging is, by design, a lossy projection: GitHub documents that "intermediate commits from the pull request are not preserved as separate commits on the base branch", so the published branch contains no object for states the working archive genuinely occupied; locally, `git merge --squash` also stamps the new commit with the *squash* time as both author and committer date, erasing the authoring interval.

### Cited Findings

- "the commits are combined into one commit and merged into the default branch"; "Squashing turns all commits in the pull request into one commit on the base branch"; and the tradeoff: "intermediate commits from the pull request are not preserved as separate commits on the base branch." — [GitHub Docs, About pull request merges](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/incorporating-changes-from-a-pull-request/about-pull-request-merges)
- The squash commit message is synthesised, not inherited: the default can include "the pull request title, pull request description, or commit information." — [GitHub Docs](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/incorporating-changes-from-a-pull-request/about-pull-request-merges)
- Rebase merging on GitHub: it "Always updates the committer information and creates new commit SHAs" and amounts to "re-writing the commit history on the base branch with new commits", while "preserving the individual commits from the pull request." — [GitHub Docs](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/incorporating-changes-from-a-pull-request/about-pull-request-merges)
- Interactive-rebase folding attributes the folded commit to "the author of the first commit" and concatenates messages, dropping `fixup` messages entirely — [git-rebase, INTERACTIVE MODE](https://git-scm.com/docs/git-rebase)
- **[measured] Experiment 4 — local squash merge dates.** Branch at `main~1` (dated 2020-01-01), then `git merge --squash main && git commit -m squashed`:
  ```
  f2e6081 A=2026-10-24 20:32:39 -0400 C=2026-10-24 20:32:39 -0400 squashed
  2841d34 A=2020-01-01 00:00:00 +0000 C=2020-01-01 00:00:00 +0000 base
  ```
  The squashed commit carries neither of the original commit's timestamps, and the 2021-06-01 commit has no representative object on this branch at all.

### Inferences

- For an archive, squash-merged history means the transaction-time axis has holes with *known* endpoints: you know state S0 (before the squash commit) and S1 (after), and you know from the PR that the archive passed through intermediate states, but no tree hash records them. A bitemporal store built from git commits can therefore only ever claim "the archive was in state S1 from time T onward", never "it was in state S_i between T0 and T1".
- Conversely, squash merges make the *branch* timeline cleaner and nearly monotonic (one commit per integration, committer date = integration time). So the choice is: squashed history gives you a trustworthy but coarse transaction axis; preserved history gives you fine granularity with untrustworthy ordering. This trade-off is worth stating explicitly in the episode.
- Any commit whose parents number ≥ 2 (a merge) also bundles states: the merge's tree can differ from both parents' trees (conflict resolution), so the merge introduces content at integration time that was never in any single-parent commit.
- Amends and force-pushes are the third hole: the pre-amend object may still exist in the reflog locally but is unreachable, and on a server it is typically unreachable and eventually GC'd, so "what the archive said yesterday" can be irrecoverable even in principle.

### Gaps

- GitHub's docs do not state what author/committer *dates* a server-side squash or rebase commit receives, nor whose identity the author field gets. My Experiment 4 covers only local `git merge --squash`. This should be verified against a real squash-merged archive repo before the episode asserts anything about it.

## Shallow clones, unshallowing, and whether a commit referenced by hash can be fetched individually

### Takeaway

A `--depth 1` clone grafts the boundary commits to look like roots, so historical commits are not merely absent but *invisible* (`%p` is empty, `rev-list --count` lies); `--unshallow` costs essentially a full clone. The load-bearing good news, which I verified against GitHub: **a single reachable commit can be fetched by full SHA into a depth-1 repo** — `git fetch --depth 1 origin <sha>` — because a refspec's source "can also be a fully spelled hex object name", provided the server allows SHA-1 wants.

### Cited Findings

- `--depth=<depth>`: "Create a *shallow* clone with a history truncated to the specified number of commits. Implies `--single-branch` unless `--no-single-branch` is given…"; `--shallow-since=<date>` and `--shallow-exclude=<ref>` are the date/ref-bounded variants; `--bundle-uri` "is incompatible with `--depth`, `--shallow-since`, and `--shallow-exclude`" — [git-clone](https://git-scm.com/docs/git-clone)
- `--reject-shallow` / `clone.rejectShallow`: "Fail if the source repository is a shallow repository." — [git-clone](https://git-scm.com/docs/git-clone)
- Representation: "$GIT_DIR/shallow lists commit object names and tells Git to pretend as if they are root commits." Git builds a graft per entry with a negative parent count to distinguish it from user grafts. Traversal stops at shallow commits ("git log" ends after showing them), and `git fsck` does **not** report the missing parents, "because the library honors shallow commits" — [Documentation/technical/shallow.adoc](https://raw.githubusercontent.com/git/git/master/Documentation/technical/shallow.adoc)
- Also from shallow.adoc: deepening "does not retrieve tags on the newly stored, older commits"; `$GIT_DIR/shallow` is not locked but timestamp-checked before write; "How 'push into/from a shallow repo' should behave is unclear"; depth 2147483647 (0x7fffffff) means infinite depth.
- `git fetch --unshallow`: "If the source repository is complete, convert a shallow repository to a complete one, removing all the limitations imposed by shallow repositories. If the source repository is shallow, fetch as much as possible so that the current repository has the same history as the source repository." — [git-fetch](https://git-scm.com/docs/git-fetch)
- `--depth` on fetch: "Limit fetching to the specified number of commits from the tip of each remote branch history… deepen or shorten the history to the specified number of commits. **Tags for the deepened commits are not fetched.**" `--deepen=<n>` counts "from the current shallow boundary instead of from the tip"; `--shallow-since=<date>` deepens/shortens "to include all reachable commits after <date>" — [git-fetch](https://git-scm.com/docs/git-fetch)
- Fetch-by-hash is documented at the refspec level: "<src> is typically a ref, or a glob pattern with a single `*`… but it can also be a fully spelled hex object name." (Negative refspecs are excluded: "Fully spelled out hex object names are also not supported.") — [git-fetch](https://git-scm.com/docs/git-fetch)
- Server side gating: `uploadpack.allowReachableSHA1InWant` — "Allow `upload-pack` to accept a fetch request that asks for an object that is reachable from any ref tip. However, note that calculating object reachability is computationally expensive. Defaults to `false`." `uploadpack.allowAnySHA1InWant` — "Allow `upload-pack` to accept a fetch request that asks for any object at all. It implies `uploadpack.allowTipSHA1InWant` and `uploadpack.allowReachableSHA1InWant`… By default not set." `uploadpack.allowTipSHA1InWant` applies only "When `uploadpack.hideRefs` is in effect". `uploadpack.allowFilter` — "If this option is set, `upload-pack` will support partial clone and partial fetch object filtering." — [Documentation/config/uploadpack.adoc](https://raw.githubusercontent.com/git/git/master/Documentation/config/uploadpack.adoc)
- Partial clone is explicitly *not* a history mechanism: "Partial clone is a mechanism to limit the number of blobs and trees downloaded **within** a given range of commits — and is therefore independent of and not intended to conflict with existing DAG-level mechanisms… (i.e. shallow clone, single branch, or fetch <refspec>)." — [Documentation/partial-clone](https://git-scm.com/docs/partial-clone)
- Partial clone mechanics and costs: promisor packfiles carry a `<name>.promisor` marker so "there is no need for the client to explicitly maintain an expensive-to-modify list of missing objects"; missing objects are "faulted in" via `promisor_remote_get_direct()`; `checkout`/`unpack-trees` "bulk pre-fetch all required missing blobs in a single batch"; `rev-list --objects --quiet --missing=print A..B` can prefetch in bulk. Costs: "Use of partial clone requires that the user be online"; "Dynamic object fetching invokes fetch-pack once **for each item**… This may incur significant overhead — and multiple authentication requests — if many objects are needed"; it "currently uses the existing pack protocol V0… The server will send a full set of info/refs when the connection is established"; and "fetching currently fetches all objects referred to by the requested objects, even though they are not necessary." — [Documentation/partial-clone](https://git-scm.com/docs/partial-clone)
- `git clone --filter=blob:none` "will filter out all blobs (file contents) until needed by Git"; `--filter=blob:limit=<size>` filters by size — [git-clone](https://git-scm.com/docs/git-clone)
- `git fetch --refetch`: "fetches all objects as a fresh clone would. Use this to reapply a partial clone filter from configuration or using `--filter=` when the filter definition has changed." — [git-fetch](https://git-scm.com/docs/git-fetch)
- **[measured] Experiment 5 — fetching a reachable non-tip commit by SHA from GitHub works.** `git clone --depth 3 --single-branch https://github.com/git/git.git` gave target `8f352ee2dc57db650acbfb04e04307257bb814ce` (= `HEAD~2`, reachable but not a ref tip). Then in a **fresh empty repo** with only `origin` configured:
  ```
  $ git fetch --depth 1 origin 8f352ee2dc57db650acbfb04e04307257bb814ce
  From https://github.com/git/git
   * branch  8f352ee2dc57db650acbfb04e04307257bb814ce -> FETCH_HEAD     (exit 0)
  $ git cat-file -t 8f352ee2dc57db650acbfb04e04307257bb814ce
  commit
  ```
  So GitHub (as of 2026-10-24) serves individual reachable commits by full SHA, and `--depth 1` keeps the transfer to that one commit's snapshot.
- **[measured] Experiment 6 — shallow clones misreport history.** In the depth-3 clone, `git log --format='%h parent=%p'` showed the boundary commit as `8f352ee parent=` — an apparent root commit, not an error. After `git clone --depth 1` + `git fetch --unshallow`, `git rev-list --count HEAD` went from 1 to **82,563**.
- **[measured] Experiment 7 — costs on git/git (82.5 k commits, 4,857 files at HEAD), warm network, wall-clock:**
  | operation | time | resulting `.git` |
  |---|---|---|
  | `git clone --depth 1` | 4.0 s | 14 MB |
  | `git clone --filter=blob:none` | 10.3 s | 137 MB |
  | `git clone` (full) | 18.8 s | 323 MB |
  | `git fetch --unshallow` on the depth-1 clone | 57.6 s | 321 MB |
  | `git clone --depth 3` | — | 15 MB |
  Unshallowing was ~3× slower than a fresh full clone and landed at the same size — consistent with it being a full history transfer plus local reconciliation rather than an incremental top-up.

### Inferences

- For the stated situation (a depth-1 clone whose referenced historical commits are absent), the cheapest correct path is **per-commit targeted fetch**: `git fetch --depth 1 origin <full-sha>` for each needed commit, which transfers only that commit, its root tree and the blobs for that snapshot. Experiment 5 shows this works on GitHub for reachable commits; the documented preconditions are the hex-object-name refspec ([git-fetch](https://git-scm.com/docs/git-fetch)) plus server-side `allowReachableSHA1InWant`/`allowAnySHA1InWant` ([uploadpack.adoc](https://raw.githubusercontent.com/git/git/master/Documentation/config/uploadpack.adoc)). Against a self-hosted or mirrored remote, neither is on by default, so the fetch can fail with "Server does not allow request for unadvertised object" and the fallback is `--unshallow` (or `--shallow-since`).
- Abbreviated SHAs are risky here: the refspec doc requires a "fully spelled hex object name", so stored links must retain full 40/64-hex ids if they are to be fetchable later.
- `--filter=blob:none` is the better shape for *indexing many commits*: it gets the full commit DAG and all trees cheaply (137 MB vs 323 MB here, 10 s vs 19 s) and faults blobs in only for the paths you actually read. But the docs' warning that dynamic fetching "invokes fetch-pack once for each item" means a naive per-file read loop over N commits will be pathological; batch the prefetch (`rev-list --objects --missing=print` then one fetch) as the design doc suggests.
- A commit fetched by SHA into a shallow repo arrives grafted (no parents present), so it can be read as a *snapshot* but cannot be used to compute diffs against its real parent or to establish ordering relative to other fetched commits. Ordering must come from metadata you already hold (or from a separate deepened clone), not from the grafted DAG.
- `git fsck` not reporting missing parents (shallow.adoc) means an indexing pipeline cannot use fsck as a completeness check; completeness must be asserted by comparing `rev-list --count` or by checking for `.git/shallow`'s existence (`git rev-parse --is-shallow-repository`).

### Gaps

- Experiment 5 is one observation against GitHub on one date; GitHub does not document this guarantee, and a 2017 jgit-dev thread reported the opposite behaviour at the time ("with github I still see that I am not allowed to fetch any SHA1 which is known to the service side repo" — [jgit-dev archive](https://www.eclipse.org/lists/jgit-dev/msg03278.html)), and noted JGit clients failing where native git succeeded. Treat "GitHub allows reachable-SHA fetch" as empirically true now, not contractually guaranteed; I found no GitHub documentation page stating it.
- I did not test fetching an **unreachable** SHA (e.g. a commit orphaned by a force-push), which is exactly the case `allowAnySHA1InWant` governs and which matters for amended/rewritten archive history.
- Timings in Experiment 7 are single runs on one machine and network; they indicate order of magnitude only.

## Materialising a repository at N past commits: mechanics and cost

### Takeaway

For indexing, reading trees directly out of the object store (`git ls-tree -r` + `git cat-file --batch --buffer`) is roughly an order of magnitude cheaper than checking out a working tree and costs no disk: on git/git's 4,857-file HEAD I measured 0.11 s and 0 bytes, versus 0.97 s and 61 MB for `git worktree add`. Worktrees share the object store and refs but each is a full working-tree copy on disk.

### Cited Findings

- A linked worktree "is linked to the current repository, sharing everything except per-worktree files such as `HEAD`, `index`, etc."; "all pseudo refs are per-worktree and all refs starting with `refs/` are shared" (exceptions: `refs/bisect`, `refs/worktree`, `refs/rewritten`); `$GIT_DIR` points at a private dir under `$GIT_DIR/worktrees` while `$GIT_COMMON_DIR` points back to the main repo, via a `.git` file at the worktree root — [git-worktree](https://git-scm.com/docs/git-worktree)
- `git worktree add -d <path>` / `--detach` "creates a new worktree with a detached HEAD at the same commit as the current branch" — the right mode for materialising arbitrary historical commits, since `add` otherwise "refuses to create a new worktree when <commit-ish> is a branch name and is already checked out by another worktree" — [git-worktree](https://git-scm.com/docs/git-worktree)
- Stale worktree admin files are pruned automatically per `gc.worktreePruneExpire` or manually via `git worktree prune`; `git worktree lock` prevents pruning; "The main worktree cannot be removed." The page also warns: "Multiple checkout in general is still experimental, and the support for submodules is incomplete." — [git-worktree](https://git-scm.com/docs/git-worktree)
- `git cat-file --batch`: "Print object information and contents for each object provided on stdin." Input is "objects from stdin, one per line… in the same order as they have been read", default output format `%(objectname) %(objecttype) %(objectsize)` followed by contents; unresolvable names yield `<object> SP missing LF` — a cheap existence probe that matters in partial/shallow repos — [git-cat-file](https://git-scm.com/docs/git-cat-file)
- `--buffer`: "Normally batch output is flushed after each object is output… With this option, the output uses normal stdio buffering; this is much more efficient when invoking `--batch-check` or `--batch-command` on a large number of objects." — [git-cat-file](https://git-scm.com/docs/git-cat-file)
- `--batch-all-objects` operates "on all objects in the repository and any alternate object stores (not just reachable objects)… visited in order sorted by their hashes"; it ignores the replace mechanism — [git-cat-file](https://git-scm.com/docs/git-cat-file)
- **[measured] Experiment 8 — materialising git/git's HEAD (4,857 tracked files), warm cache, single runs:**
  | approach | wall time | disk |
  |---|---|---|
  | `git archive HEAD -o arch.tar` | 0.64 s | one tar (not retained) |
  | `git worktree add --detach <path> HEAD` | 0.97 s | 61 MB working tree |
  | `git ls-tree -r HEAD \| awk '{print $3}' \| git cat-file --batch --buffer > /dev/null` | 0.11 s | 0 |
  Extrapolating linearly, 1,000 historical commits of that size cost ~2 min and 0 B via `cat-file --batch`, versus ~16 min and ~61 GB if each were checked out and kept.

### Inferences

- The object-store read path (`ls-tree -r` + `cat-file --batch`) is the right default for an indexer: it needs no working tree (so it works in a bare repo), it is stream-shaped, and `missing` lines make partial-clone gaps explicit instead of silently empty. Worktrees are only worth it when you must run external tools that insist on real files; `git archive` sits in between (one process per commit, streamable to a tar consumer, no index churn).
- Reuse across commits is the dominant optimisation: between adjacent commits most blob OIDs are identical, so an indexer keyed by blob OID does work proportional to the *change*, not to N × files. This is the same observation GitHub reports for its code-search corpus (dedup + delta indexing, below).
- Because trees are themselves content-addressed, subtree OIDs let you skip unchanged directories wholesale (`git diff-tree -r` between consecutive commits is cheaper still than two full `ls-tree`s) — worth preferring when the commits form a chain rather than an arbitrary set.
- In a blobless partial clone, `cat-file --batch` on a missing blob triggers a per-object fetch (per the partial-clone doc's "once for each item"), so the batch read must be preceded by a bulk prefetch or the measured 0.11 s becomes thousands of round trips.

### Gaps

- All timings are single runs with a warm page cache on one laptop, on a repo whose HEAD is 61 MB / 4,857 files; they are not a benchmark. I found no published, citable benchmark comparing worktree vs archive vs cat-file for history materialisation.
- `git-worktree`'s docs say nothing about the cost or scaling of many worktrees (only the "experimental" caution), so there is no citable statement on, e.g., an upper bound on worktree count.

## Documented experience from systems that index git history

### Takeaway

The two best-documented code-search systems both avoid treating history as a time axis: they index a small set of *refs* (default branch, optionally a bounded number of branches) and fall back to unindexed search for anything else; GitHub's write-up frames correctness as per-commit consistency plus blob-level deduplication, not as a chronological archive.

### Cited Findings

- Sourcegraph historically "still only indexed your default branch (whichever branch HEAD points to in Git, conventionally the branch called master)", so "when searching a non-default branch for the first time, Sourcegraph would not consult an index" — making those searches slow and liable to time out — [Sourcegraph blog: Indexed non-default branches](https://sourcegraph.com/blog/indexed-non-default-branches)
- From Sourcegraph 3.18, admins opt in via `experimentalFeatures.search.index.branches`, with a cap of "up to 64 branches per repository"; "Any repo@revision mentioned in a version context or in search.index.branches will be indexed by the periodic indexing process", and the cleanup job that removes old indexes also removes deleted branches — [Sourcegraph blog](https://sourcegraph.com/blog/indexed-non-default-branches); [Sourcegraph 3.18 release post](https://sourcegraph.com/blog/sourcegraph-3.18)
- Cost model: "The indexer will deduplicate documents between branches. So the size of your index will grow in relation to the number of unique documents", while "indexing multiple branches will add additional resource requirements to Sourcegraph (particularly memory)" — [Sourcegraph blog](https://sourcegraph.com/blog/indexed-non-default-branches)
- Freshness is scope-dependent: searches scoped to specific repositories "are always up-to-date" because contents are fetched on user action, while unscoped searches over large repository sets may lag the latest default-branch revisions — [Sourcegraph code search features docs](https://sourcegraph.com/docs/code-search/features.md)
- GitHub's code-search engine is "consistent on a commit-level basis", and newly pushed content is not searchable "until they are fully processed by the system"; crawling is event-driven off `git push` — [GitHub blog: The technology behind GitHub's new code search](https://github.blog/engineering/the-technology-behind-githubs-new-code-search/)
- GitHub shards by Git blob ID "while avoiding any duplication", crawls "the blobs unique to that repository (not the entire repository)", and records per-document "the list of locations where a document appears (which path, branch, and repository)" — noting "This data can be quite large for popular content" — [GitHub blog](https://github.blog/engineering/the-technology-behind-githubs-new-code-search/)
- Scale and the payoff of dedup/delta indexing: a corpus of "115 TB of code and 15.5 billion documents" reduces to "around 28 TB of unique content", with "the index itself clock[ing] in at just 25 TB", "roughly a quarter the size of the original data"; delta indexing "reduces the number of documents we have to crawl by over 50%", enabling a full re-index in "about 18 hours" versus an estimated ~36 — [GitHub blog](https://github.blog/engineering/the-technology-behind-githubs-new-code-search/)

### Inferences

- Both systems chose **ref-scoped snapshots with per-commit consistency** over a historical time axis. That is strong circumstantial evidence that indexing "the repository as of arbitrary past times" is not a solved, off-the-shelf pattern — the industry practice is to index a handful of named revisions and treat anything else as an unindexed, on-demand query.
- GitHub's blob-ID sharding and >50 % delta-crawl saving quantify the inference from Experiment 8: across commits the unique-content ratio is roughly 1:4 (28 TB of 115 TB), so a history indexer keyed on blob OID should expect work and storage proportional to unique blobs, not to commits × files.
- "Consistent on a commit-level basis" is the right correctness statement to borrow: a retrieval answer should name the commit it was computed from, and reconstruction should be *per-commit* (one tree, one snapshot), never a blend of commits assembled by date.

### Gaps

- I could not retrieve Zoekt's own design documentation. Its README (fetched) says nothing about branches, HEAD-only indexing, history, or a `-branches` flag, and gives no size figures — [zoekt README](https://raw.githubusercontent.com/sourcegraph/zoekt/main/README.md). A targeted read of `doc/` in that repo would be needed.
- The Sourcegraph quotes above come from search-result extracts of the blog post and docs page; my direct fetch of `https://sourcegraph.com/blog/indexed-non-default-branches` returned HTTP 404, so I could not verify them verbatim against the page. Flag them as second-hand until re-verified.
- I found no write-up from a code-search or RAG system that explicitly indexes *many historical commits* and discusses ordering or reproducibility. Hound and GitHub's older systems were not covered in my searches. If the episode wants prior art for history-as-corpus, that appears to be a genuine gap in the public literature rather than a search failure on my part — but one more pass (git-annex/`git log -S` tooling, Software Heritage's archive-of-all-history, Google's Code Search papers) would be the place to look.
