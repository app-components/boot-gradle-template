# Spring Boot Gradle Template

This repository is a working Spring Boot template with:

- a multi-module Gradle layout
- one Spring Boot sample application under [applications/template](/Users/adib/dev/app-components/boot-gradle-template/applications/template)
- one Vite Vue + TypeScript sample application under [applications/frontend](/Users/adib/dev/app-components/boot-gradle-template/applications/frontend)
- one shared local PostgreSQL + pgAdmin setup in [compose.yaml](/Users/adib/dev/app-components/boot-gradle-template/compose.yaml)
- a local Docker packaging flow for the Spring Boot sample application
- GitHub Actions workflows for CI and tagged releases

## Repository Layout

- [applications](/Users/adib/dev/app-components/boot-gradle-template/applications)
  Deployable applications. The current examples are [applications/template](/Users/adib/dev/app-components/boot-gradle-template/applications/template) and [applications/frontend](/Users/adib/dev/app-components/boot-gradle-template/applications/frontend).
- [components](/Users/adib/dev/app-components/boot-gradle-template/components)
  Reusable shared code.
- [platform](/Users/adib/dev/app-components/boot-gradle-template/platform)
  Shared dependency platform for the build.
- [buildSrc](/Users/adib/dev/app-components/boot-gradle-template/buildSrc)
  Gradle build logic and small local helper scripts.

## Quick Start

Start the shared local database:

```bash
compose up
```

Run the template app directly with Spring Boot:

```bash
./gradlew :applications:template:bootRun
```

Run the frontend SPA locally:

```bash
cd applications/frontend
npm install
npm run dev
```

Run the tests:

```bash
./gradlew test
```

## Local Database

The root [compose.yaml](/Users/adib/dev/app-components/boot-gradle-template/compose.yaml) starts:

- PostgreSQL on `localhost:15432`
- pgAdmin on `http://localhost:15433`

Default local credentials:

- Postgres admin user: `postgres`
- Postgres admin password: `password`
- Template app database: `template_app`
- Template app database user: `template_app`
- Template app database password: `password`

The local convention is:

- one shared Postgres server per repository
- one logical database per application
- one application-specific database user per application

When another application is added, extend the inline SQL in [compose.yaml](/Users/adib/dev/app-components/boot-gradle-template/compose.yaml) with another user/database pair.

## Compose Helper

The repo includes [buildSrc/scripts/compose](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/compose), a thin wrapper around `docker compose`.

Supported commands:

- `compose up`
- `compose down`
- `compose ps`
- `compose logs`
- `compose clean`

The helper always targets the root [compose.yaml](/Users/adib/dev/app-components/boot-gradle-template/compose.yaml) and loads `.env` and `.env.local` if they exist.

## Gradle Helper Scripts

The repo also includes short Gradle helper commands under [buildSrc/scripts/gradle](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/gradle).

If direnv is active, [.envrc](/Users/adib/dev/app-components/boot-gradle-template/.envrc) adds this directory to `PATH`, so these commands can be run directly from the repo.

Available commands:

- [g](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/gradle/g)
  Runs `gradlew` with the arguments you pass through.
- [b](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/gradle/b)
  Runs `gradlew spotlessApply build --parallel`.
- [cb](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/gradle/cb)
  Runs `gradlew clean build --parallel --no-build-cache --warning-mode all`.
- [ct](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/gradle/ct)
  Runs `gradlew cleanTest test --parallel`.
- [s](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/gradle/s)
  Runs `gradlew spotlessApply`.

Examples:

```bash
g :applications:template:bootRun
b
ct
s
```

## Template Application

The sample application is in [applications/template](/Users/adib/dev/app-components/boot-gradle-template/applications/template).

It currently demonstrates:

- Spring MVC
- Spring Data JPA
- Flyway
- PostgreSQL
- Testcontainers

The sample domain is a small quotes application backed by a seeded `quotes` table.

## Frontend Application

The frontend example is in [applications/frontend](/Users/adib/dev/app-components/boot-gradle-template/applications/frontend).

It currently demonstrates:

- Vue 3
- Vite
- TypeScript
- the default Vite Vue scaffold

Run it locally:

```bash
cd applications/frontend
npm install
npm run dev
```

Build it for production:

```bash
npm run build
```

## Configuration Convention

The template app uses standard Spring Boot datasource configuration in [application.yml](/Users/adib/dev/app-components/boot-gradle-template/applications/template/src/main/resources/application.yml):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:15432}/${DB_NAME:template_app}
    username: template_app
    password: password
```

That means:

- local `bootRun` works against the shared local Postgres by default
- container runs can override only the host, port, or database name when needed
- the database name stays owned by the application config, not by helper scripts

## Local Image Workflow

The template app has its own Dockerfile at [applications/template/Dockerfile](/Users/adib/dev/app-components/boot-gradle-template/applications/template/Dockerfile).

Build the jar:

```bash
cd applications/template
../../gradlew bootJar
```

Build the image:

```bash
build-image
```

Run the image:

```bash
run-image
```

What the helper scripts do:

- [buildSrc/scripts/build-image](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/build-image)
  Runs `docker build -t <current-directory>:latest .` from an application directory.
- [buildSrc/scripts/run-image](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/run-image)
  Runs the local image and sets `DB_HOST=host.docker.internal` so the container can connect back to the shared local Postgres.

Examples:

```bash
cd applications/template
../../gradlew bootJar
build-image
run-image
```

Custom image tag:

```bash
build-image my-registry/template:1.0.0
```

Custom host port:

```bash
run-image template:latest 8081
```

Custom database overrides:

```bash
DB_HOST=192.168.1.50 run-image
DB_PORT=25432 DB_NAME=billing run-image
```

## Testing

Run all tests:

```bash
./gradlew test
```

The template app tests use Spring Boot Testcontainers support and start PostgreSQL automatically during the test run.

## GitHub Workflows

This repo includes two workflows under [`.github/workflows`](/Users/adib/dev/app-components/boot-gradle-template/.github/workflows):

- [ci.yml](/Users/adib/dev/app-components/boot-gradle-template/.github/workflows/ci.yml)
  Runs `./gradlew test` on pull requests and pushes to `main`.
- [release.yml](/Users/adib/dev/app-components/boot-gradle-template/.github/workflows/release.yml)
  Runs on tag push, builds the template app jar, creates a GitHub release, and attaches the built jar.
