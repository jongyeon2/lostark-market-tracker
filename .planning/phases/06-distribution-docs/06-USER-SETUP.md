# Phase 6: User Setup Required

**Generated:** 2026-06-25
**Phase:** 06-distribution-docs
**Status:** Incomplete

Claude automated everything verifiable locally (the CI workflow, the seed profile, and the
README). The one remaining item literally requires a human with push access to the GitHub repo —
it cannot be verified from the local working tree.

## Manual Action

- [ ] **Push the branch to GitHub once so the CI workflow runs and the README badge turns green.**
  - Why: `.github/workflows/ci.yml` only executes on GitHub's runners. Until the first push, the
    badge `actions/workflows/ci.yml/badge.svg` (in `README.md`, 06-02) shows "no status".
  - Command (from the repo root):
    ```bash
    git push -u origin main
    ```
  - Prerequisite: the GitHub remote must point at `jongyeon2/lostark-market-tracker` (the org/repo
    the badge URL and `user_setup` assume). Verify with `git remote -v`; if absent, create the repo
    and add the remote first.

## Verification

After pushing:

```bash
# 1. The Actions tab shows a "CI" workflow run for the latest commit.
#    https://github.com/jongyeon2/lostark-market-tracker/actions/workflows/ci.yml
# 2. Once the run is green, the README badge renders green automatically (no further action).
```

Expected: a green "CI" run (≈ the same result as the local `./gradlew build`, which is already
green — 86 tests, 0 failures), and a green badge at the top of the README.

---

**Once the badge is green:** Mark status as "Complete" at the top of this file.
