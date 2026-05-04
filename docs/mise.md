# mise: Tool Versions and PATH Activation in This Repo

## What mise does for this template

[mise](https://mise.jdx.dev/) is a single tool that manages:

- JDK and Node versions (replacing `.sdkmanrc` and `.nvmrc`)
- PATH additions for the local helper scripts under [buildSrc/scripts](../buildSrc/scripts) (replacing `.envrc`)
- A small set of project tasks invokable via `mise run <task>`

Before mise, this repo used three tools — SDKMAN for Java, nvm for Node, and direnv for PATH — each with its own dotfile. mise consolidates them into a single [mise.toml](../mise.toml) at the repo root.

## What `mise.toml` declares

### Tools

```toml
[tools]
java = "temurin-25"
node = "24"
```

The JDK is pinned to **Temurin 25** and Node to **24**. `mise install` from the repo root downloads both into mise's local install directory and exposes them on `PATH` whenever mise is active in your shell.

There is no lockfile pinning specific patch versions. mise resolves `node = "24"` to the latest matching `24.x.x` at install time — `24.15.0` today, `24.16.0` next month. This is deliberate for a *template*: a consumer cloning the template months later should get current tools, not whatever was current at publish time.

### PATH

```toml
[env]
_.path = [
    "buildSrc/scripts",
    "buildSrc/scripts/gradle",
    "."
]
```

Three directories get prepended to `PATH` when you `cd` into this repo:

- `buildSrc/scripts` exposes the dev convenience scripts: `compose`, `build-image`, `run-image`, `stop-image`, `renovate`, `run-renovate`.
- `buildSrc/scripts/gradle` exposes the short gradle aliases: `g`, `b`, `cb`, `ct`, `s`, `outdated`.
- `.` lets you run scripts in the repo root without a `./` prefix.

### Enter hook

```toml
[hooks]
enter = "mise current"
```

When mise activates this directory (typically after `cd`), it runs `mise current`, which prints the active tool versions:

```text
java temurin-25.0.3+9.0.LTS
node 24.15.0
```

This is intentional — a visible reminder that you're operating under specific tool versions. Silence it by removing the hook block (or changing the command) in your local copy.

### Tasks

```toml
[tasks.renovate]
description = "Start the Renovate workflow on GitHub"
run = "gh workflow run renovate.yml --ref main"
```

Run it with `mise run renovate`. Useful after ticking a Dependency Dashboard checkbox — Renovate only re-reads dashboard state at the start of a run, so a checkbox click does nothing until the next dispatch (or the daily 01:00 UTC scheduled run).

This is **distinct from** the [`renovate` script](../buildSrc/scripts/renovate) on PATH, which runs Renovate locally in dry-run mode for previewing changes. Same name, different purposes — `mise run renovate` hits GitHub; the local script doesn't.

### Settings

```toml
[settings]
experimental = true
```

Opts into mise's experimental features. Currently unused but kept ready for future additions.

## How mise interacts with Gradle's toolchain

This is the part most people get wrong, so it's worth being explicit.

When you run `./gradlew` inside this repo, **two** JDK selections happen:

1. **The daemon JVM** — what `gradlew` uses to launch the Gradle daemon. The wrapper script consults `$JAVA_HOME` first, then the bare `java` on `PATH`. mise sets `JAVA_HOME` to its installed Temurin 25, so the daemon launches under that.

2. **The compile/test toolchain** — the JDK Gradle uses to actually compile and run your code. The convention plugin in [`buildSrc`](../buildSrc/src/main/java/build/conventions/JavaConventionsPlugin.java) requests `JavaLanguageVersion.of(25)`. Gradle resolves this by auto-detecting installed JDKs and matching one against the request.

[gradle.properties](../gradle.properties) sets `org.gradle.java.installations.auto-download=false`, which means Gradle will **never** silently download a missing JDK. If no installed JDK matches the toolchain request, the build fails loudly. This is intentional — mise is the one place where the JDK comes from; we don't want a parallel `~/.gradle/jdks/` install drifting from it.

So: install mise → `mise install` → Gradle finds the JDK via `JAVA_HOME` → build works. Skip the mise step → Gradle fails with *"no toolchain matches and auto-provisioning is disabled"* → you go install mise.

## How CI uses mise

Currently, CI does **not** use mise. The [build workflow](../.github/workflows/build.yml) invokes `actions/setup-java` and `actions/setup-node` directly:

- Java version is hardcoded as `"25"`
- Node version is read from `applications/frontend/package.json`'s `engines.node` field

`mise.toml` is ignored by CI. This is workable but means three places declare versions: `mise.toml`, `build.yml`, and `package.json`. Consolidating via [`jdx/mise-action`](https://github.com/jdx/mise-action) would collapse this to one source of truth — a candidate evolution, not currently done.

## Updating tool versions

Edit `mise.toml`, then run `mise install` to fetch the new versions. There's no lockfile to bump separately; the constraint in `mise.toml` is the only declaration.

If you bump Java, also update `.github/workflows/build.yml`'s `java-version` and the Spring Boot Gradle plugin's expectations (Spring Boot supports a known set of JDK versions per release).

If you bump Node, also update `applications/frontend/package.json`'s `engines.node` so CI's `setup-node` and local npm's `engine-strict` enforcement see the same value.

## Useful commands

```bash
mise install        # install/refresh pinned tools
mise current        # show active tool versions
mise tasks          # list all defined tasks
mise run renovate   # run a named task
mise prune          # remove tool versions no longer referenced by any project
```

## See also

- [mise documentation](https://mise.jdx.dev/)
- [Gradle toolchains](https://docs.gradle.org/current/userguide/toolchains.html) — the mechanism the convention plugin uses to pin the JDK
- [.github/README.md](../.github/README.md) — how CI installs tools (currently bypasses mise)
