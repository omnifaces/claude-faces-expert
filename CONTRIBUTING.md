# Contributing

Thanks for your interest in improving Claude Faces Expert!

## Branch model

- **`develop`** — active development branch. **All pull requests must target `develop`.**
- **`main`** — release branch. Only maintainers merge `develop` into `main` to cut a release.

If you open a PR against `main`, you'll be asked to retarget it to `develop`.

## Submitting changes

1. Fork the repository.
2. Create a branch off `develop` for your change (e.g. `feature/my-improvement` or `fix/some-bug`).
3. Make your changes. Keep the scope focused — one logical change per PR.
4. Open a pull request **with `develop` as the base branch**.
5. Describe what changes and *why*. Link related issues if any.

## What kinds of changes are welcome

- New or improved Jakarta Faces rules, examples, or diagnostics in `.claude/faces/`.
- Fixes for incorrect or outdated guidance — please cite the spec section or authoritative source.
- New skills under `.claude/skills/` that complement the existing `/faces-review` and `/faces-migrate`.
- Installer improvements (`install.sh`, `install-opencode.sh`) and tooling support for additional AI coding agents.
- Documentation fixes in `README.md`, `CHANGELOG.md`, or topic files.

## What to avoid

- Sweeping reformatting or renaming changes mixed in with substantive edits — keep them in separate PRs.
- Hallucinated or unverifiable spec claims. If you're unsure, say so in the PR description.
- Unrelated tooling/config changes that aren't tied to the change you're making.

## AI-assisted contributions

AI coding assistants are welcome.
The same rules apply as to any other contribution, plus these:

- **You are the author.**
  You must understand every line you submit, and be able to explain and defend it in review.
  "The AI wrote it" is not an answer to a review comment.
- **Disclose it.**
  State in the pull request description which tool and model you used, and for what, e.g. "Claude Code with Claude Opus 5.5 wrote the test, I wrote the fix".
  Always include the model name and version, as your tool reports it.
  That keeps the history accountable and traceable, and shows over time which models and versions do the job well.
  A `Co-Authored-By` trailer in the commit message counts as disclosure when it names the model and version, e.g. `Co-Authored-By: Claude Opus 5.5`.
- **Verify it.**
  Check every rule, example and spec claim against the spec or the implementation before opening the pull request.
  Never claim a behavior, a default or a spec rule you have not checked.

Pull requests that ignore these rules may be closed without review.

## Integration tests

`/faces-review` has a local integration suite under `tests/`: small Faces projects with violations planted on purpose, reviewed by the skill, then graded on whether the report names each violation and withholds the false positives the rules forbid.

It calls the Anthropic API on your own key, so it is local-only and never runs in CI. See `DEVELOPERS.md` for setup, cost and how to add a fixture.

```sh
./it.sh                              # every fixture
./it.sh faces41-primefaces           # one fixture
```

A pull request that changes a rule does not have to run the suite, but a rule change that a fixture would catch should come with one.

## Releases

Contributors don't need to touch versions — leave that to the release commit. Maintainers cut one as follows.

1. Bump `Version X.Y.Z` across `README.md`, `.claude/faces/rules.md`, all topic files under `.claude/faces/topics/`, and every skill's `SKILL.md`.
2. Rename the `## Unreleased` heading in `CHANGELOG.md` to `## X.Y.Z`. No fresh `## Unreleased` heading is seeded; the next change to land adds one back.
3. Commit those files together with `CHANGELOG.md`, subject `Release X.Y.Z`, on `develop`. Nothing else belongs in a release commit.
4. Tag the release commit `X.Y.Z` — lightweight, no `v` prefix — and push the tag.
5. Fast-forward `main` to the release commit and push it. `main` therefore always points at the most recent release.
6. Publish a GitHub Release for the tag, taking the notes from the `CHANGELOG.md` section so the changelog stays the single source of truth:
   ```bash
   gh release create X.Y.Z --verify-tag --title X.Y.Z --latest \
       --notes "$(awk '/^## X.Y.Z$/ { on = 1; next } on && /^## / { exit } on' CHANGELOG.md)"
   ```
   This is what notifies anyone watching the repository for releases, and what makes `/releases/latest` resolve.

The `protect-main-develop` ruleset blocks deletion and force-pushes on both branches, and `protect-tags` blocks deleting or moving a pushed tag.
