# 0001. Stack and architecture: rationale

Decision record for [index.md](index.md). `/develop` does not need this file.

## Context

Book Worm is the backend for the IBM Applied AI capstone: a REST API where customers browse, buy and cancel book orders. The brief fixes Java 17, Spring Boot 3.x, Maven and PostgreSQL, wants the OpenAPI spec written first (`src/main/resources/openapi.yaml`), JWT login with BCrypt, JUnit and MockMvc tests, and `mvn clean install` plus `mvn spring-boot:run` with no errors. Local run is enough. The grade rests on a working app, clean API design, best practices, and an honest record of AI use.

One developer builds it on a Windows 11 machine. Environment found on 2026-10-07: IBM Semeru OpenJDK 17.0.7 (OpenJ9), Maven 3.9.4, PostgreSQL 16.15 running as the Windows service `postgresql-x64-16` on port 5432 (`psql` not on PATH), and no Docker. Database credentials must never land in git; `.gitignore` already excludes `application-local.properties`, `.env` and `target/`.

Left open, every later feature would invent its own answer for layout, migrations, test database and token handling, and the scaffold could not be built. The data model (feature 3), the API contract (feature 4) and auth (feature 5) each have their own spec; this one decides only what they build on.

## Options considered

### Option 1: Layered Spring Boot 3.5 monolith, Flyway, H2 tests, Spring Security resource server JWT

Package by layer, Flyway SQL migrations with Hibernate validating, H2 in PostgreSQL mode for tests, JWT through Spring's resource server support.

**Pros**: smallest moving parts; tests need only the JDK; JWT validation and 401 responses come from Spring itself; layer folders match the rubric wording.
**Cons**: H2 differs from PostgreSQL in places; Boot 3.5 is past its free support window.

### Option 2: Same monolith, Testcontainers PostgreSQL for tests, JJWT with a custom filter

Tests run against a real PostgreSQL in a container; tokens built and checked with JJWT in a hand written `OncePerRequestFilter`.

**Pros**: tests hit the real database engine; JJWT is the most common tutorial approach.
**Cons**: needs Docker Desktop, which is not installed (and installing it is outside this run); a custom filter is more security code to get right.

### Option 3: Package by feature monolith, Liquibase, Hibernate generated schema for tests

Folders per domain (`catalogue`, `order`, `auth`), Liquibase changelogs, `ddl-auto=create-drop` in tests.

**Pros**: feature folders scale better as the app grows.
**Cons**: tests would not run the real migrations; Liquibase XML/YAML is heavier to read than SQL; the rubric looks for controller/service/repository layers.

### Option 4: Code generated API with openapi-generator, otherwise Option 1

Generate controller interfaces and models from `openapi.yaml` during the build.

**Pros**: code cannot drift from the spec.
**Cons**: generator config, generated sources and naming quirks add build complexity for a 12 journey API.

## Rationale

Option 1 fits the forces that matter here. Docker is absent, so H2 is the only test database that runs with no extra install, and keeping Flyway on in tests means the same migrations are exercised everywhere. A single developer with a fixed deadline gains nothing from feature folders or a code generator; the layered layout is also what the reviewers expect to see. Spring's resource server already validates JWTs and returns 401, so feature 5 writes only token issuing code, which avoids the "reinventing auth" failure pattern. Spring Boot 3.5.16 is the newest release allowed by the fixed 3.x rule; its ended free support is a known, accepted cost for a capstone.

The runner up is Option 2. Switch to it if Docker Desktop gets installed and H2 incompatibilities start costing time.

## References

**Project sources**:
- `docs/capstone-brief.md` (required stack, business rules, workflow steps)
- `docs/scope/scope.md` feature 1 (done when), features 3 to 5 (what is deferred to them)
- `.gitignore` (local config and `.env` already excluded)

**Practices & standards**:
- Monolith first for a small team; ORM for CRUD with SQL migrations
- RFC 9457 problem details for HTTP error bodies
- Use a proven auth library instead of a custom token filter
- Twelve factor config: secrets in the environment, not in code

**Links** (web verified 2026-10-07):
- Maven Central, spring-boot-starter-parent versions (latest 3.x is 3.5.16): https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml
- endoflife.date Spring Boot support dates (3.5 open source support ended 2026-06-30): https://endoflife.date/api/spring-boot.json
