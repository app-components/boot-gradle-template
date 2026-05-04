# GitHub configuration

This directory holds the repository's GitHub-specific configuration:
GitHub Actions workflows, the Renovate config link, and (eventually)
issue/PR templates if added. This README documents the
**architectural decisions** baked into those workflows — the *why*
behind non-default choices.

For Renovate's own deep-dive, see [docs/renovate.md](../docs/renovate.md).
For the user-facing summary of all workflows, see the
[root README](../README.md#github-workflows).

## Workflows in this directory

| File | Purpose |
|---|---|
| [`workflows/build.yml`](workflows/build.yml) | PR + main-branch verification: `./gradlew build` plus `npm ci && npm run build` |
| [`workflows/release.yml`](workflows/release.yml) | Tag-triggered: builds the backend jar and publishes a GitHub release |
| [`workflows/renovate.yml`](workflows/renovate.yml) | Daily/on-demand dependency-update bot — see [docs/renovate.md](../docs/renovate.md) |

## Architectural decisions

### Pinned to `ubuntu-24.04`, not `ubuntu-latest`

All three workflows use `runs-on: ubuntu-24.04` (Noble Numbat — the
latest LTS image GitHub publishes today). The `ubuntu-latest` label
silently moves to whichever LTS GitHub considers current; pinning
removes that drift.

When Ubuntu 26.04 lands as a GitHub-Actions runner image (typically
3-6 months after the Ubuntu LTS release), this is a one-line edit
in each workflow. Renovate's `github-actions` manager doesn't
manage runner-label strings, so the bump won't be automated — it's
a deliberate periodic decision.

### Action pinning policy: first-party at major, third-party where it matters

The actions used across these workflows split into two trust tiers:

| Source | Pin level used here | Examples |
|---|---|---|
| First-party (GitHub, Vue/Vite/Gradle/Docker official orgs) | `@v<major>` | `actions/checkout@v6`, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v6` |
| Third-party (anything outside trusted orgs) | `@v<major.minor.patch>` (specific) or SHA pin | `renovatebot/github-action@v46.1.12` |

The principle: **the trust footprint isn't reduced by SHA-pinning
an action whose vendor you already trust with the underlying
runtime.** `gradle/actions` is published by Gradle Inc., the same
org whose binary `./gradlew` invokes — if their org account is
compromised, a malicious action is the smaller of your two
problems. Pinning their action's major and letting Renovate keep
it current is the right balance of safety and maintenance cost.

For high-assurance environments (regulated builds, SLSA-3 chains
of trust, or strict supply-chain policies), the upgrade path is to
flip Renovate's `pinDigests: true` and re-render every action
reference as a SHA pin. That's intentional but not free — it adds
PR noise.

### `gradle/actions/setup-gradle@v6` with `cache-provider: basic`

`build.yml` and `release.yml` both pin:

```yaml
- uses: gradle/actions/setup-gradle@v6
  with:
    cache-provider: basic
```

The `cache-provider: basic` flag is non-default and deserves
explanation.

**The licensing change in v6:** `gradle/actions` v6 introduces a
dual-licensing model. The action core remains MIT-licensed (free,
open source). But the new default caching backend — a library
called `gradle-actions-caching` — is **proprietary**. It's
currently free in preview, and Gradle Inc. has signaled it will
become paid for commercial use. See
[`DISTRIBUTION.md`](https://github.com/gradle/actions/blob/main/DISTRIBUTION.md)
for the official statement.

**What `cache-provider: basic` does:** forces the action onto the
fallback MIT-licensed cache implementation (backed by
`actions/cache`). This keeps the workflow 100% OSS-licensed
regardless of how Gradle Inc. monetises the proprietary cache
later.

**What you give up:**

- Fine-grained caching of the Gradle User Home (the proprietary
  cache splits the home into more granular entries; `basic`
  caches the home as a single blob).
- Intelligent cache cleanup (the proprietary version prunes stale
  entries more aggressively to stay under GitHub's 10 GB cache
  cap per repo).
- A few smaller niceties around concurrent-job cache contention.

**What you keep:**

- Dependency caching via `actions/cache`.
- Wrapper integrity validation (still bundled into the action
  core).
- Gradle distribution caching.
- The build summary in the Actions UI.

For this template's footprint — small to medium Gradle projects
with predictable dep churn — the basic provider is fine. If a
fork ever hits the 10 GB cache wall on a huge build, they can
flip back to the enhanced provider for that fork.

### `./gradlew build`, not `./gradlew test`

`build.yml` runs `./gradlew build`, not `./gradlew test`. The
difference matters: `build` is a superset that runs `check` (which
includes `test`, `spotlessCheck`, the `restrict-imports` check, and
every other `check`-task contributor) plus `assemble` (compile and
jar everything).

The `test` target alone would have skipped `spotlessCheck` and the
`restrict-imports` enforcement — both repo conventions enforced
by build-side plugins. Renovate-style PRs that pass formatting/lint
but break a check or compile gate would slip through if CI only
ran `test`.

The integration tests use Testcontainers, which need Docker. The
`ubuntu-24.04` runner ships with Docker pre-installed, so no extra
runner setup is required. If a fork hits Docker Hub rate limits
(anonymous pulls are 100 / 6h per IP, shared across GitHub
runners' NAT'd IPs), the cheapest fix is `docker/login-action`
with a Docker Hub token — but it's unlikely to bite a small
project.

### Frontend job runs sequentially in the same job, not in parallel

`build.yml` runs Java + Gradle build first, then Node setup +
frontend build, all in one job. Splitting into parallel `jvm` and
`frontend` jobs would shave wall-clock time at the cost of more
config and a small amount of CI minutes. For a template, the
single-job shape is simpler to reason about and easy to split
later if the wait becomes painful.

When that split makes sense, the rough shape is:

```yaml
jobs:
  jvm:
    steps: [checkout, setup-java, setup-gradle, ./gradlew build]
  frontend:
    steps: [checkout, setup-node, npm ci, npm run build]
  ci-success:
    needs: [jvm, frontend]
    runs: echo ok    # aggregator for branch protection
```

The `ci-success` aggregator pattern matters once you're using
required status checks in branch protection — it gives you one
stable check name that depends on all the others.

### `actions/setup-node` reads `applications/frontend/package.json`

The Node version isn't pinned in `build.yml`. Instead, the
workflow uses `node-version-file:`:

```yaml
- uses: actions/setup-node@v6
  with:
    node-version-file: applications/frontend/package.json
    cache: npm
    cache-dependency-path: applications/frontend/package-lock.json
```

This reads the `engines.node` field of `package.json` (currently
`"24"`) and installs that Node version. The same field is enforced
locally by npm via `.npmrc`'s `engine-strict=true`, so CI and local
development read from the same value. The repo also pins Node in
`mise.toml`'s `[tools]` for the local-tooling activation path —
both should match.

### `permissions: contents: read` at the workflow level

All three workflows declare:

```yaml
permissions:
  contents: read
```

This is least-privilege at the workflow level. The default
`GITHUB_TOKEN` permissions in this repo would grant write access
to many resources by default; explicitly setting `contents: read`
narrows the implicit token's authority.

`release.yml` escalates to `contents: write` because it creates
GitHub releases. The Renovate workflow uses the App token
(short-lived, App-scoped) rather than `GITHUB_TOKEN`, so its
declared `contents: read` is mostly cosmetic — the App token is
what actually does the work.

### Triggers: `pull_request` + `push: main`

`build.yml` runs on both PRs (gating merges) and pushes to `main`
(post-merge confirmation). Same workflow, two triggers — not
separate "PR" and "main" workflows. This is the standard shape;
it gives you:

- Pre-merge: PR is gated by green CI before it can be merged.
- Post-merge: green check on the main-branch commit confirms
  the merged result is healthy. Useful when looking at the
  Actions tab to see "is `main` currently broken?"

`release.yml` triggers only on tag push (`tags: '*'`). Releases
are intentional, separate from regular merges.

`renovate.yml` triggers on schedule (daily 01:00 UTC) and on
`workflow_dispatch` (manual). Not on PRs — Renovate isn't tested
by Renovate.

## See also

- [`docs/renovate.md`](../docs/renovate.md) — full Renovate
  documentation including the GitHub App setup, the Dependency
  Dashboard, and PR review guidance.
- The header comment of
  [`workflows/renovate.yml`](workflows/renovate.yml) — inline
  context for the Renovate workflow's structure.
- The [root README](../README.md) — top-level template
  documentation.
