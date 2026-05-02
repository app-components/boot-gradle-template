# buildSrc

Shared Gradle build logic for this repository, packaged as two convention
plugins that modules opt into via their `plugins {}` block. Compiled once
and consumed in-process — never published, no Maven coordinates.

- **`java.conventions`** — every JVM module (applications and libraries).
- **`maven.publish.conventions`** — modules that publish to GitHub Packages.

## java.conventions

Configures the things every JVM module in the repository should agree on:
JDK toolchain, compilation flags, formatting, the test framework, code
coverage, jar provenance, and the shared dependency BOM. See the Javadoc
on each private method in `JavaConventionsPlugin.java` for what each rule
does and why.

### Assumptions

- Build runs inside a git checkout. `gradle-git-properties` is configured
  to fail otherwise.
- No JDK is required on the host. Gradle downloads the configured JDK
  toolchain on demand.
- A single `:platform` Bill of Materials lives at the repository root and
  aggregates Spring Boot + Spring Cloud BOMs plus a few pinned third-party
  libraries. Modules reference dependencies by alias without versions.
- All Java tests use AssertJ for assertions. The plugin actively bans
  JUnit's assertion API.
- Frameworks like Spring and Jackson read method parameter names via
  reflection at runtime, so the build always passes `-parameters` to the
  compiler. Removing it silently breaks runtime parameter binding.
- Catching problems at build time is the right default. Warnings,
  formatting drift, JUnit assertions, and low coverage fail the build
  rather than slipping through; modules can opt out of individual rules
  when a transition makes a check temporarily painful.
- `./gradlew test` is the developer's inner-loop tool and stays free of
  gate-keeping checks. Coverage enforcement is wired to `check` (which
  `build` and CI depend on), not to `test`, so iterative test runs aren't
  blocked by coverage failures.

### Customizing

Edit the relevant constant at the top of `JavaConventionsPlugin.java`. One
edit propagates across the repository on the next build.

- **`JAVA_VERSION`** — JDK toolchain version. Gradle downloads this JDK on
  demand; bump it to migrate the whole repo to a newer Java release.
- **`MINIMUM_COVERAGE_PERCENT`** — minimum bundle-instruction coverage
  required to pass `check`. Builds below this threshold fail.
- **`WARNING_FLAGS`** — the curated `-Xlint` set plus `-Werror`. Add or
  remove individual flags to tighten or relax the strict warning policy.
- **`JAVA_LICENSE_HEADER`** — the Spotless-stamped license header. Edit
  the text to fit your project; Spotless rewrites every Java source file's
  header on the next `spotlessApply`.

## maven.publish.conventions

Wires the `maven-publish` plugin so a module's `java` or `java-platform`
component publishes to GitHub Packages. The plugin is independent of
`java.conventions` — applying it on a module is what opts that module
into publication.

### Assumptions

- The `GITHUB_ACTOR` and `GITHUB_TOKEN` environment variables provide
  credentials at publish time. Locally that means a classic PAT with
  `read:packages` / `write:packages` scope; in CI they're the standard
  GitHub Actions context variables.
- The publication target is a single GitHub Packages repository hardcoded
  in `MavenConventionsPlugin.REPO_URL`.

### Customizing

- **`REPO_URL`** in `MavenConventionsPlugin.java` — the GitHub Packages
  repository the artifacts publish to. Update this when forking the
  template into a different GitHub organization or repository.
