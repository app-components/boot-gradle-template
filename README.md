# Spring Boot Gradle Template

**A Spring Boot Gradle template that “just works” from laptop to production.**

This repository shows you how to create a Spring Boot project that “just works” from the moment you clone it. You can import it into your IDE and run the main class, or simply execute `bootRun`—without the brittleness and frustration of wrestling with complex local configurations. Built on a philosophy of *convention over configuration*, it provides a clear structure for multi-module applications and a consistent development experience from local dev to production.

## Conventions

This template follows a set of conventions to ensure a predictable and consistent development experience:

**Project structure**  
Defines how applications, shared components, and platform code are organized.  
*Outcome: a predictable layout that makes it easy to navigate and extend the codebase.*  

**Local development**  
Establishes how developers run the project locally, including services like databases and message brokers.  
*Outcome: a consistent “clone and run” experience that works across all developer machines.*  

**Configuration management**  
Standardizes how application settings are defined and overridden for different environments.  
*Outcome: simple, reliable configuration from local development through production.*  

**Dependency management**  
Centralizes version control for all third-party libraries and plugins.  
*Outcome: consistent dependencies across modules and fewer version conflicts.*  

**Build and quality enforcement**  
Applies automated formatting, testing, and security checks during the build.  
*Outcome: a maintainable, high-quality codebase with fewer regressions.*  

**CI/CD pipelines**  
Defines how builds, tests, and deployments are automated in the pipeline.  
*Outcome: faster feedback and reliable releases without manual intervention.*  

**Documentation**  
Captures architectural decisions and module-specific information.  
*Outcome: shared context and easier onboarding for new team members.*  

**Modular and optimized builds**  
The modular structure and clear separation of responsibilities make builds faster and more reliable.  
*Outcome: support for parallel builds, better Gradle build caching, and incremental compilation for quicker feedback loops.*  

## Local Postgres

Stage 1 of local Docker support is a shared PostgreSQL plus pgAdmin setup in [compose.yaml](/Users/adib/dev/app-components/boot-gradle-template/compose.yaml).

### Why this shape

This repository can eventually host multiple applications and shared components. Rather than giving each application its own PostgreSQL container, the default convention is:

- one shared local PostgreSQL instance
- one database per application
- shared local credentials
- pgAdmin preconfigured to connect without extra setup

That keeps local infrastructure simple while still isolating applications at the database level.

### Commands

Use the `compose` helper from `buildSrc/scripts`:

```bash
compose up
compose ps
compose down
compose clean
```

If direnv is active, `.envrc` adds `buildSrc/scripts` to `PATH` so `compose ...` works from anywhere in the repo. Without direnv, run [buildSrc/scripts/compose](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/compose) directly.

### Compose helper

The helper script lives at [buildSrc/scripts/compose](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/compose) and is a thin wrapper around `docker compose` for this repository.

What it does:

- always targets the root [compose.yaml](/Users/adib/dev/app-components/boot-gradle-template/compose.yaml)
- works from any directory in the repo
- loads `.env` and `.env.local` from the repo root before running Docker Compose
- provides `compose clean`, which runs `docker compose down -v --remove-orphans`

What it does not do:

- it does not replace Docker Compose concepts or hide service definitions
- it does not create extra config files outside the repository conventions
- it does not manage multiple compose files yet

Requirements:

- `docker`

Examples:

```bash
compose up
compose logs -f postgres
compose clean
```

## Build Image

Use [buildSrc/scripts/build-image](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/build-image) from an application directory that contains a `Dockerfile`.

Example for the template app:

```bash
cd applications/template
../../gradlew bootJar
build-image
```

Optional image tag:

```bash
cd applications/template
../../gradlew bootJar
build-image my-registry/template:1.0.0
```

The script looks for a `Dockerfile` in the current directory. If it does not find one, it fails immediately.
The script assumes the application has already been built. It does not invoke Gradle for you.
It is only a local convenience wrapper around `docker build -t ... .`.

## Run Image

Use [buildSrc/scripts/run-image](/Users/adib/dev/app-components/boot-gradle-template/buildSrc/scripts/run-image) to run the image locally after building it.

Example:

```bash
cd applications/template
run-image
```

Optional image tag and host port:

```bash
cd applications/template
run-image template:latest 8081
```

By default `run-image` sets `DB_HOST=host.docker.internal` so the containerized app connects back to the local PostgreSQL instance started by `compose up`:

```text
jdbc:postgresql://host.docker.internal:15432/template_app
```

The template app builds its JDBC URL from [application.yml](/Users/adib/dev/app-components/boot-gradle-template/applications/template/src/main/resources/application.yml):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:15432}/${DB_NAME:template_app}
```

That means:

- `run-image` only needs to override `DB_HOST` for the common local container case
- `DB_PORT` and `DB_NAME` stay available when an application needs something different
- the database name stays with the application config instead of being hardcoded in the script

The username and password still come from [application.yml](/Users/adib/dev/app-components/boot-gradle-template/applications/template/src/main/resources/application.yml). Override the host if needed:

```bash
DB_HOST=192.168.1.50 run-image
```

Override other datasource parts the same way if needed:

```bash
DB_PORT=25432 DB_NAME=billing run-image
```

### Default connection details

- PostgreSQL: `localhost:15432`
- pgAdmin: `http://localhost:15433`
- Admin user: `postgres`
- Password: `password`
- Template app database: `template_app`
- Template app database user: `template_app`

### Template app database

The shared PostgreSQL server is started by [compose.yaml](/Users/adib/dev/app-components/boot-gradle-template/compose.yaml), and the template app database is created by the inline `postgres_init` script in that same file:

```sql
CREATE USER template_app WITH PASSWORD 'password';

CREATE DATABASE template_app
    WITH
    OWNER = template_app
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;
```

This is the convention the repo should follow as more applications are added: one Postgres server, many logical databases, and one app-specific user per database.

### Adding another application database

When a new application is added, extend the same inline init block in [compose.yaml](/Users/adib/dev/app-components/boot-gradle-template/compose.yaml), for example:

```sql
CREATE USER billing WITH PASSWORD 'password';
CREATE DATABASE billing
    WITH
    OWNER = billing;
```

The important point is that local isolation happens by database, not by starting a separate PostgreSQL container per application.

### Spring Boot alignment

The sample app includes `spring-boot-docker-compose`, so `bootRun` can start the local Postgres services automatically. The app datasource uses the standard `spring.datasource.url` property, but the host, port, and database name are externalized so local image runs can override only what they need.

## Template Dockerfile

The template application now has its own Dockerfile at [applications/template/Dockerfile](/Users/adib/dev/app-components/boot-gradle-template/applications/template/Dockerfile).

It is inspired by the centralized Spring Boot Dockerfile pattern from the other repo, but kept local to the application so the packaging flow stays straightforward in this template.

Build it with:

```bash
cd applications/template
../../gradlew bootJar
docker build -t template:latest .
```
