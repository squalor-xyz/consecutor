# Agent guidance

## Slice workflow

Work is planned with `slicer` (see `.slicer/`). For each slice `<ID>`:

1. Pick it with `slicer next`.
2. Work in its own worktree: `git worktree add .worktrees/<ID> -b slice/<ID> main`.
   Never delete or `rmdir` the shared `.worktrees/` directory; remove only your own worktree.
3. Run `slicer start <ID>` when you begin, from inside the worktree so the claim travels with the branch.
   Do not record slicer status on the main checkout. An uncommitted claim there gets swept into the next roadmap commit.
4. When finished, run `slicer handoff <ID> --render` from the worktree to mark it ready for review.
   Do not run `slicer done`: the owner does that after review and merge.
5. Commit only when asked. Slice code is committed as `<ID>: <summary>`; slicer state goes in a separate `Roadmap: ...` commit.

## Wrap-up

When the owner asks to wrap a slice up:

1. Review it against the slice and fix defects on the slice branch before merging.
2. Merge into `main` with `--no-ff`. Leave every other worktree and branch alone. Do not push.
3. Re-run that slice's checks on the merged tree.
4. From the main checkout, run `slicer done <ID> --render` and commit the result as `Roadmap: mark <ID> done`.
5. Remove only that worktree and its `slice/<ID>` branch. Never delete the shared `.worktrees/` directory.

## Builds

A worktree has no gitignored `local.properties`. Build with
`JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
and `ANDROID_HOME` set to the `sdk.dir` value from the main checkout's `local.properties`.
The Homebrew prefix is not the JDK home: it has no `release` file.
`connectedDebugAndroidTest` needs an emulator. `NavigationTest` clears the app database and refuses to run on anything else.
