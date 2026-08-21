# Plan: Customize this template into a new project

## Context

### Outcome

Transition the repository from boot-gradle-template state into a coherent project owned by the
user: the template's identity (name, group, package root, license header, repository references)
is replaced everywhere, the sample functionality is consciously kept, replaced, or removed, the
documentation describes the resulting project, and the full verification loop passes. The build
conventions, multi-module layout, and work-note system the template provides are retained.

### Approach

An explicit request to execute this todo authorizes the template-to-project transition. Do not try
to infer that intent from the repository's origin, remote, or directory name. Unlike a blank
starter, this template fixes the technology stack: a multi-module Gradle build with a Spring Boot
backend, a Vite Vue + TypeScript frontend, compose-managed local infrastructure, and buildSrc
convention plugins. Customization replaces identity and samples, not the toolchain; if the user
wants a different stack, this note does not apply and says so.

Customization converges through conversation rather than a single interview: each decision-bearing
phase opens with its own focused questions, iterates with the user until its output is approved,
and ends by revising this Approach section and any affected later checklist items before the phase
is checked off. Record only decisions that govern remaining work so a later session can resume
from the current context and unchecked steps.

The verification loop already works and must stay green: `./gradlew build` from the repository
root, then `npm ci && npm run build` in `applications/frontend`. Run it after every phase that
changes code or build configuration. Preserve existing user changes, and do not publish artifacts,
push changes, alter remotes, or create a commit without explicit user authorization.

## Phases

### Phase 1: Confirm authorization and survey the repository

Establish that the transition is authorized and that existing content is understood before any
edits.

- [ ] Confirm that the user explicitly requested customization of the current repository; if the
      request is absent or unclear, stop and ask the user.
- [ ] Inspect the repository and working tree so existing content and user changes are preserved.

### Phase 2: Establish the project identity

Converge with the user on the identity every later phase derives from.

- [ ] Ask what the project is called, what it does and for whom, the Maven group coordinate, the
      root Java package, and the copyright owner and license for the source header.
- [ ] Record the approved answers in this plan's Approach section so later phases and sessions
      apply one consistent identity.

### Phase 3: Replace the template identity in the build

Make the build carry the project's identity with the verification loop still green.

- [ ] Set `rootProject.name` in `settings.gradle.kts` and `group` in the root `build.gradle.kts`.
- [ ] Move the Java sources of `applications/backend` and `components/time` (including tests and
      test fixtures) from `com.adibsaikali.*` to the chosen root package.
- [ ] Replace `JAVA_LICENSE_HEADER` in
      `buildSrc/src/main/java/build/conventions/JavaConventionsPlugin.java` with the chosen
      header and run `./gradlew spotlessApply` so every source file is restamped.
- [ ] Update the frontend identity in `applications/frontend/package.json` and its `index.html`
      title when the user wants the sample name replaced.
- [ ] Sweep the tree for remaining `boot-gradle-template` and template-owner references (for
      example in `mise.toml` tasks and `docs/`) and update each to the project's values.
- [ ] Run the verification loop and resolve every failure.

### Phase 4: Decide the fate of the sample functionality

Turn the template's demonstration code into a conscious keep, replace, or remove decision.

- [ ] Walk through the samples with the user: the Quote REST + mail demo in
      `applications/backend` (with its Flyway migrations and `mail-demo.http`), the
      `components:time` sample component, and the sample frontend UI; decide each one's fate.
- [ ] Apply the decisions, adjusting `compose.yaml` services the project no longer needs (for
      example Mailpit when mail is dropped) while retaining the version-catalog aliases and
      `platform` BOM pattern as the template's dependency guidance.
- [ ] Review `.gitignore`: activate the optional language sections the project needs and delete
      the ones it never will.
- [ ] Review : activate the optional language sections the project needs and delete
      the ones it never will.
- [ ] Run the verification loop and resolve every failure.

### Phase 5: Rewrite the documentation

Make the documentation describe the resulting project and its now-working commands.

- [ ] Rewrite `README.md` with the project's orientation, layout, and the local workflow (mise
      setup, the `g`/`b`/`cb`/`ct`/`s`/`compose`/`build-image` helpers, and the compose stack),
      keeping links to `work/README.md` so the collaboration system stays discoverable.
- [ ] Review `docs/frontend.md`, `docs/mise.md`, and `docs/renovate.md`, updating repository-
      specific references while retaining the tooling guidance.
- [ ] Review `.github/workflows/` (`build.yml`, `release.yml`, `renovate.yml`) and the Renovate
      configuration so CI, releases, and dependency updates target the new repository.

### Phase 6: Capture initial work

Record the backlog that follow-up sessions will resume from.

- [ ] Ask what the first concrete goals or known pieces of work are, now that the identity and
      remaining code exist to ground them.
- [ ] Capture each goal as a distinct note under `work/ideas/`, `work/todo/`, or `work/parked/`
      according to its current intent, referencing the project's real paths and commands.

### Phase 7: Prune, verify, and complete customization

Remove template-only content and demonstrate that the resulting repository is usable.

- [ ] Remove template-state wording and any scaffolding that no longer serves the resulting
      project, while retaining the work-note system, the `manage-work-notes` skill, and the
      checked-in agent symlinks.
- [ ] Run the full verification loop; when Docker is available, also smoke-test the compose stack
      and `build-image` flow the project retains.
- [ ] Run `git diff --check` and confirm final documentation references valid paths, describes
      the resulting project, and contains no template-state placeholders.
- [ ] Complete and delete this bootstrap note only after all required work and verification
      succeed; retain it if anything remains unfinished.
- [ ] Report the outcome, assumptions, and unresolved questions; propose a commit message and
      create the commit only when the user approves.
