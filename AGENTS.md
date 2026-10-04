# Agent guidance

## Slice workflow

Work is planned with `slicer` (see `.slicer/`). For each slice `<ID>`:

1. Pick it with `slicer next`.
2. Work in its own worktree: `git worktree add .worktrees/<ID> -b slice/<ID> main`.
   Never delete or `rmdir` the shared `.worktrees/` directory; remove only your own worktree.
3. Run `slicer start <ID>` when you begin, from inside the worktree so the claim travels with the branch.
4. When finished, run `slicer handoff <ID> --render` to mark it ready for review.
   Do not run `slicer done`: the owner does that after review and merge.
5. Commit only when asked. Slice code is committed as `<ID>: <summary>`; slicer state goes in a separate `Roadmap: ...` commit.

A worktree has no gitignored `local.properties`. Build with `JAVA_HOME=/opt/homebrew/opt/openjdk@17` and
`ANDROID_HOME` set inline to the `sdk.dir` value from the main checkout's `local.properties`.
