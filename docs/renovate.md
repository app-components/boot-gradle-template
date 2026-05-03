# Renovate

This template uses [Renovate](https://docs.renovatebot.com/) to keep
dependencies current. The setup is **self-hosted** — Renovate runs on
GitHub-hosted Actions runners under our control, not on Mend's SaaS.
This document explains how the pieces fit together, the architectural
choices we made, and how to use the system day to day.

For inline detail on any specific decision, the workflow file
[`.github/workflows/renovate.yml`](../.github/workflows/renovate.yml)
and the config file [`renovate.json`](../renovate.json) carry the
short version next to the code.

## Architecture at a glance

```
┌─────────────────────────────────────────────────────┐
│  GitHub Actions (.github/workflows/renovate.yml)    │
│                                                      │
│   • Daily at 01:00 UTC                              │
│   • Plus on-demand via workflow_dispatch            │
│   • Authenticates as a GitHub App                   │
│   • Runs ghcr.io/renovatebot/renovate Docker image  │
└─────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────┐
│  This repository                                     │
│                                                      │
│   • Reads renovate.json for config                  │
│   • Scans Gradle, npm, Docker, GitHub Actions deps  │
│   • Opens PRs for new versions                      │
│   • Maintains the Dependency Dashboard issue       │
└─────────────────────────────────────────────────────┘
```

## Architectural decisions and why

These are the non-default choices in our setup. Each is a deliberate
trade-off; if a fork has different needs, this section is the place
to push back.

### Self-hosted, not Mend SaaS

The default way to use Renovate is to install the Mend-hosted GitHub
App. We chose self-hosted because **this template gets forked into
private projects**. The Mend-hosted App clones every private repo it
runs against to Mend's SaaS infrastructure to scan it. Self-hosted
Renovate runs on GitHub-hosted Actions runners — clones happen on
ephemeral runners and never leave GitHub's boundary.

If your fork is fully public and you don't care about source-code
boundaries, the Mend-hosted App is simpler (no workflow, no token to
manage) and equally functional.

### GitHub App auth, not a Personal Access Token

The workflow authenticates as a GitHub App
(`actions/create-github-app-token@v1`), not as a personal token. Two
reasons:

1. **PATs are owned by an individual.** If the token's owner leaves
   the org or rotates the token, Renovate silently breaks for the org's
   repos.
2. **Apps mint short-lived tokens at runtime.** The repo holds the
   App ID and private key as secrets; the actual access token is
   generated per-run and expires in ~1 hour. No long-lived
   broad-scope token sits in the repo.

Either approach satisfies the *other* reason a separate identity is
needed: GitHub Actions blocks workflows triggered by `GITHUB_TOKEN`.
A PAT or App token has a different actor, so PRs Renovate opens
trigger `build.yml` normally.

### Major-version bumps gated behind dashboard approval

Default Renovate behavior is to open a PR for *every* available
update, including majors. We added a `packageRules` entry that
gates major bumps behind a Dependency Dashboard checkbox:

```json
"packageRules": [
  {
    "matchUpdateTypes": ["major"],
    "dependencyDashboardApproval": true
  }
]
```

Patches and minors flow as PRs unchanged. Majors land on the
Dependency Dashboard issue under "Pending Approval" with a
checkbox. Tick the box, run Renovate, and *then* it opens the
PR. The intent is to keep the review queue free of breaking-change
PRs that demand real decisions — those should be intentional, not
background noise.

### PR limits

```json
"prHourlyLimit": 0,
"prConcurrentLimit": 10
```

- **`prHourlyLimit: 0`** disables the rolling 1-hour throttle.
  `config:recommended` defaults to 2/hour, designed for
  shared-tenancy bots that risk drowning maintainers in
  notifications. For a single-repo, single-reviewer setup with a
  daily schedule plus on-demand runs, the throttle just slows the
  daily run from doing the work it already evaluated. Everything
  beyond the first two updates piles up in the dashboard's
  Rate-Limited section.
- **`prConcurrentLimit: 10`** is the cognitive cap. Above 10 open
  Renovate PRs the review queue stops being manageable. This is
  also the `config:recommended` default; we set it explicitly to
  signal intent.

## The Dependency Dashboard

The single load-bearing UI of this whole setup is the Dependency
Dashboard issue (auto-created and auto-maintained by Renovate, with
title "Dependency Dashboard"). Every Renovate run rewrites its body
in place.

### How it works

The dashboard is a Markdown issue body containing GitHub-flavored
task lists. Each `- [ ]` checkbox is encoded with a hidden HTML
comment that Renovate parses on its next run. Clicking a checkbox
edits one character of the issue body (the space → `x`); on the
next Renovate run, that flip becomes an instruction.

**Important:** clicking a checkbox does not trigger Renovate. The
workflow runs on a schedule (daily at 01:00 UTC) and on
`workflow_dispatch`. After ticking a box, Renovate sees the flipped
state at the start of its next run. To make the click take effect
immediately, dispatch the workflow yourself with the
[`run-renovate`](../buildSrc/scripts/run-renovate) helper or the
"Run workflow" button in the Actions tab.

### Sections you'll see

1. **Pending Approval** — major-version bumps held back by our
   `dependencyDashboardApproval: true` rule. Tick a box to opt into
   a PR for that bump on the next run.
2. **Rate-Limited** — updates Renovate evaluated but didn't open
   PRs for, due to `prHourlyLimit` or `prConcurrentLimit`. With our
   current settings (hourly limit disabled, concurrent limit 10),
   this section appears only when 10+ Renovate PRs are already
   open. Tick to bypass the limit for a specific update.
3. **Open** — links to existing Renovate PRs. Tick to force a
   rebase (useful after merging a conflicting PR).
4. **Detected Dependencies** — informational inventory of every
   dependency Renovate found. Useful for understanding what's in
   scope.

There's also "🔐 Create all rate-limited PRs at once" master
checkboxes that act on every entry in a section — useful for
draining backlogs.

### Closing the dashboard issue

Don't bother — Renovate reopens it on the next run. To suppress the
dashboard entirely, set `"dependencyDashboard": false` in
`renovate.json`. Closing without that config change is at most a
few-hour reprieve.

## Pull request shapes you'll see

Renovate produces three categories of PR. Knowing which is which
makes review faster.

### 1. Lockfile-only updates (most common)

Branch name suffix: `-lockfile`.

Example: PR titled "Update dependency vue to v3.5.33", branch
`renovate/vue-monorepo` — but the diff only changes
`package-lock.json`, not `package.json`.

This happens when your declared range in `package.json` (e.g.,
`"vue": "^3.5.30"`) already permits the new version. Renovate just
re-records which version the lockfile pins to. No range change is
needed.

These are usually safe to merge after CI passes — the *declared
intent* hasn't changed, only the recorded resolution.

### 2. Range-widening updates

Diff touches both `package.json` and `package-lock.json`.

Example: PR titled "Update dependency typescript to v6", which
rewrites the package.json line from `"typescript": "~5.9.3"` to
`"typescript": "~6.0.0"` and updates the lockfile to pin 6.0.x.

These happen when the new version is outside your existing range —
for tilde-pinned packages on a major bump, or for caret-pinned
packages on a major bump. Review more carefully: the version range
you've committed to has changed.

### 3. Major version bumps (gated behind dashboard approval)

These don't appear as PRs by default — they're parked on the
Dependency Dashboard with a checkbox until you opt in. After
ticking the box and running Renovate, the resulting PR is one of
the above two shapes (usually the range-widening kind).

Major bumps are the "real decision" category. The CI gate confirms
the build still compiles, but breaking semantics changes are
possible. Read the release notes (Renovate links them in the PR
body) before merging.

## Day-to-day usage

### Triggering a Renovate run

Three ways:

1. **Wait** for the daily 01:00 UTC scheduled run.
2. **`run-renovate`** from anywhere in the repo (`direnv` adds
   `buildSrc/scripts/` to your PATH). Dispatches the workflow on
   GitHub and tails its logs.
3. **GitHub UI**: Actions tab → Renovate → Run workflow.

Use the manual trigger after ticking a Dependency Dashboard
checkbox, or after merging a PR that conflicts with several other
open Renovate PRs (so the rebases happen now, not at 01:00 UTC).

### Previewing locally without touching GitHub

The [`renovate`](../buildSrc/scripts/renovate) script (also on PATH
via direnv) runs Renovate against your working tree in
`--dry-run=full --platform=local` mode. It prints what Renovate
*would* propose without opening any PRs or modifying any files.

```
renovate
```

Useful for tuning `renovate.json` before committing. The trailing
summary lists every PR Renovate would open. Ignore "Error updating
branch" warnings — those are the local platform's expected
limitation, not real failures.

### Reviewing a Renovate PR

The standard checklist:

1. **CI is green.** This is the load-bearing signal. Our
   `build.yml` runs `./gradlew build` (full check + assemble,
   including Testcontainers integration tests) and
   `npm ci && npm run build` (frontend type-check and bundle). A
   green check means the upgrade compiles, formats, lints, and
   passes existing tests.
2. **Read the release notes.** Renovate links them in the PR body.
   For patches/minors, they're usually bug fixes — skim. For
   majors, read carefully.
3. **Look at the diff.** For lockfile-only PRs the diff is a wall
   of `"resolved": "..."` changes — no review needed beyond the
   one-line summary. For range-widening PRs check the
   `package.json` change matches what the title says.
4. **For majors**, check the Mend Merge Confidence badges in the
   PR body. They show ecosystem adoption (% of npm projects that
   have moved) and CI passing rate (% of CI runs that stayed
   green after the bump). Low adoption + low passing rate = wait.

If multiple Renovate PRs are open, merging one will conflict with
the others. Renovate auto-rebases the rest on its next run; with
`run-renovate` you can force the rebase immediately rather than
waiting for the schedule.

## Setup for forks

A fresh fork of this template will fail the Renovate workflow until
the GitHub App is installed and its secrets are configured. The
header of [`.github/workflows/renovate.yml`](../.github/workflows/renovate.yml)
documents this in detail; the short version:

1. **Create a GitHub App** (org-owned) with these repository
   permissions:
   - Contents: Read and write
   - Pull requests: Read and write
   - Issues: Read and write
   - Workflows: Read and write
   - Metadata: Read (granted automatically)
2. **Generate a private key** for the App and download the PEM file.
3. **Install the App** on the forked repository.
4. **Add two repository secrets**:
   - `RENOVATE_APP_ID` — the App's numeric App ID
   - `RENOVATE_APP_PRIVATE_KEY` — the full PEM contents

The first run will probably fail with a clear error if any of these
are missing or misconfigured — the workflow has a pre-flight step
that probes "can the token read this repo?" before running the
full Renovate scan.

## Troubleshooting

### The workflow runs but no PRs open

Check the Dependency Dashboard issue. The most common causes are
visible there:

- Updates listed under "Pending Approval" are gated by our major-
  bump rule — tick a checkbox to opt in.
- Updates listed under "Rate-Limited" hit `prConcurrentLimit`
  (10 PRs). Merge or close some open Renovate PRs first.

If there's no dashboard issue and no PRs, check the workflow run
logs. The `Validate GitHub App token can access the repo` step
fails fast with a clear error if the App is misinstalled.

### A Renovate PR's CI is failing

Three categories:

1. **Genuine breakage** — the upgrade actually broke something.
   Read the test output, decide whether to fix forward (commit a
   fix to the Renovate branch) or close the PR (Renovate adds the
   update to its "previously rejected" list).
2. **Lockfile drift** — the PR was opened against an older `main`
   and the lockfile no longer applies cleanly. Tick the
   "rebase/retry" checkbox in the PR body and run Renovate; it'll
   regenerate the branch.
3. **Flaky CI** — re-run the failed check. Not a Renovate problem.

### A Renovate PR I closed keeps reappearing

By default Renovate respects "user closed this" and doesn't reopen.
But if the underlying branch wasn't deleted (`gh pr close` without
`--delete-branch`), some configurations can cause Renovate to retry.
Always close with:

```
gh pr close <number> --delete-branch
```

This deletes the branch on origin too. Renovate sees "user rejected
this update" and will only re-propose it under different
configuration (e.g., a different version is available later).

### A major-bump PR is open even though we configured dashboard approval

Configuration changes to `renovate.json` apply to *future* update
decisions, not retroactively. PRs opened before the rule existed
stay open until you close them manually. Close the old PR (with
`--delete-branch`); on the next Renovate run the major will be
re-evaluated under the new rule and surface as a dashboard
checkbox instead.

### The workflow says "No repositories found" and exits

The action needs `RENOVATE_REPOSITORIES` set in the workflow's env
block. We set it to `${{ github.repository }}`. If the env is
missing or empty, the action defaults to scanning whatever the
token can see, which for an App-installation token returns no
repositories.

## See also

- The header of [`.github/workflows/renovate.yml`](../.github/workflows/renovate.yml)
  explains the workflow's structure and the GitHub App setup
  inline, next to the code.
- [`renovate.json`](../renovate.json) has inline `description`
  fields on each `packageRules` entry — quick context without
  leaving the file.
- [Renovate's own documentation](https://docs.renovatebot.com/)
  is comprehensive and well-written; this doc covers our
  *opinions*, not Renovate's full feature surface.
