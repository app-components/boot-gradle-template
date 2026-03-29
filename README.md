# Spring Boot Gradle Template

This repository is a working Spring Boot template with:

- a multi-module Gradle layout
- one Spring Boot sample application under [applications/backend](applications/backend)
- one Vite Vue + TypeScript sample application under [applications/frontend](applications/frontend)
- one local Traefik edge proxy in [compose.yaml](compose.yaml) for browser access during development
- one shared local PostgreSQL + pgAdmin setup in [compose.yaml](compose.yaml)
- a local Docker packaging flow for the backend sample application
- GitHub Actions workflows for CI and tagged releases

## Repository Layout

- [applications](applications)
  Deployable applications. The current examples are [applications/backend](applications/backend) and [applications/frontend](applications/frontend).
- [components](components)
  Reusable shared code.
- [platform](platform)
  Shared dependency platform for the build.
- [buildSrc](buildSrc)
  Gradle build logic and small local helper scripts.

## Quick Start

Start the shared local infrastructure:

```bash
compose up
```

Run the backend app directly with Spring Boot:

```bash
./gradlew :applications:backend:bootRun
```

Run the frontend SPA locally:

```bash
cd applications/frontend
npm install
npm run dev
```

Open the local edge URL:

```text
http://localhost:7070
```

Run the tests:

```bash
./gradlew test
```

## Local Database

The root [compose.yaml](compose.yaml) starts:

- Traefik on `http://localhost:7070`
- PostgreSQL on `localhost:15432`
- pgAdmin on `http://localhost:15433`

Default local credentials:

- Postgres admin user: `postgres`
- Postgres admin password: `password`
- Backend app database: `backend`
- Backend app database user: `backend`
- Backend app database password: `password`

The local convention is:

- one shared local edge proxy per repository
- one shared Postgres server per repository
- one logical database per application
- one application-specific database user per application

When another application is added, extend the inline SQL in [compose.yaml](compose.yaml) with another user/database pair.

## Compose Helper

The repo includes [buildSrc/scripts/compose](buildSrc/scripts/compose), a thin wrapper around `docker compose`.

Supported commands:

- `compose up`
- `compose down`
- `compose ps`
- `compose logs`
- `compose clean`

The helper always targets the root [compose.yaml](compose.yaml) and loads `.env` and `.env.local` if they exist.

## Local Edge Proxy

Traefik runs in Docker Compose and acts as the single browser-facing entrypoint during local development.

The local environment is split into two parts:

- Docker Compose services
  - Traefik on `localhost:7070`
  - PostgreSQL on `localhost:15432`
  - pgAdmin on `localhost:15433`
- locally launched processes
  - the frontend Vite dev server on `localhost:5173`
  - the Spring Boot backend on `localhost:8080`

Current routing:

- `/` -> the local Vite dev server on `host.docker.internal:5173`
- `/api` -> the local Spring Boot backend on `host.docker.internal:8080`

That means developers should browse to:

```text
http://localhost:7070
```

instead of opening the Vite dev server directly.

This keeps local browser traffic aligned with the future “one edge proxy in front of apps” deployment model without forcing the frontend and backend themselves into containers during development.

Traffic flow during local development:

- the browser connects to Traefik on `localhost:7070`
- Traefik routes `/` requests to the frontend dev server
- Traefik routes `/api` requests to the backend
- the backend connects to PostgreSQL
- pgAdmin connects to PostgreSQL as a local operator tool

## Gradle Helper Scripts

The repo also includes short Gradle helper commands under [buildSrc/scripts/gradle](buildSrc/scripts/gradle).

If direnv is active, [.envrc](.envrc) adds this directory to `PATH`, so these commands can be run directly from the repo.

Available commands:

- [g](buildSrc/scripts/gradle/g)
  Runs `gradlew` with the arguments you pass through.
- [b](buildSrc/scripts/gradle/b)
  Runs `gradlew spotlessApply build --parallel`.
- [cb](buildSrc/scripts/gradle/cb)
  Runs `gradlew clean build --parallel --no-build-cache --warning-mode all`.
- [ct](buildSrc/scripts/gradle/ct)
  Runs `gradlew cleanTest test --parallel`.
- [s](buildSrc/scripts/gradle/s)
  Runs `gradlew spotlessApply`.

Examples:

```bash
g :applications:backend:bootRun
b
ct
s
```

## Template Application

The sample application is in [applications/backend](applications/backend).

It currently demonstrates:

- Spring MVC
- Spring Data JPA
- Flyway
- PostgreSQL
- Testcontainers

The sample domain is a small quotes application backed by a seeded `quotes` table.

## Frontend Application

The frontend example is in [applications/frontend](applications/frontend).

It currently demonstrates:

- Vue 3
- Vite
- TypeScript
- a frontend-to-backend integration pattern using relative `/api` calls
- local development through the shared Traefik edge proxy

Run it locally:

```bash
compose up

./gradlew :applications:backend:bootRun

cd applications/frontend
npm install
npm run dev
```

Then open:

```text
http://localhost:7070
```

Build it for production:

```bash
npm run build
```

The frontend also has an app-local Dockerfile at [applications/frontend/Dockerfile](applications/frontend/Dockerfile):

```bash
cd applications/frontend
npm install
npm run build
docker build -t frontend:latest .
```

The frontend fetches a random quote from:

```text
/api/quotes/random
```

During local development, Traefik proxies `/api` to the backend and `/` to the Vite dev server, so the browser does not need direct cross-origin access and the backend does not need a default CORS policy just to support local development.

## Configuration Convention

The backend app uses standard Spring Boot datasource configuration in [application.yml](applications/backend/src/main/resources/application.yml):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:15432}/${DB_NAME:backend}
    username: backend
    password: password
```

That means:

- local `bootRun` works against the shared local Postgres by default
- container runs can override only the host, port, or database name when needed
- the database name stays owned by the application config, not by helper scripts

## Local Image Workflow

The backend app has its own Dockerfile at [applications/backend/Dockerfile](applications/backend/Dockerfile).

Build the jar:

```bash
cd applications/backend
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

Stop the image:

```bash
stop-image
```

What the helper scripts do:

- [buildSrc/scripts/build-image](buildSrc/scripts/build-image)
  Runs `docker build -t <current-directory>:latest .` from an application directory.
- [buildSrc/scripts/run-image](buildSrc/scripts/run-image)
  Runs the local image and reads optional per-application defaults from `.image.env`.
- [buildSrc/scripts/stop-image](buildSrc/scripts/stop-image)
  Stops the container started by `run-image`, using the same optional `.image.env` defaults.

Examples:

```bash
cd applications/backend
../../gradlew bootJar
build-image
run-image
stop-image
```

Custom image tag:

```bash
build-image my-registry/backend:1.0.0
```

Custom host port:

```bash
run-image backend:latest 8081
```

Custom container name:

```bash
CONTAINER_NAME=my-backend run-image
CONTAINER_NAME=my-backend stop-image
```

Custom database overrides:

```bash
DB_HOST=192.168.1.50 run-image
DB_PORT=25432 DB_NAME=billing run-image
```

Application-specific image defaults can be kept in `.image.env` inside each application directory. The current examples use:

- [applications/backend/.image.env](applications/backend/.image.env)
  backend listens on container port `8080` and defaults `DB_HOST` to `host.docker.internal`
- [applications/frontend/.image.env](applications/frontend/.image.env)
  frontend listens on container port `80` and defaults the host port to `8081`

## Testing

Run all tests:

```bash
./gradlew test
```

The backend app tests use Spring Boot Testcontainers support and start PostgreSQL automatically during the test run.

## GitHub Workflows

This repo includes two workflows under [`.github/workflows`](.github/workflows):

- [ci.yml](.github/workflows/ci.yml)
  Runs `./gradlew test` on pull requests and pushes to `main`.
- [release.yml](.github/workflows/release.yml)
  Runs on tag push, builds the backend app jar, creates a GitHub release, and attaches the built jar.
