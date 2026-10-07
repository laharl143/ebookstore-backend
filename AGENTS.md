# Book Worm e-bookstore backend

REST API for an online bookstore (IBM Applied AI Specialist capstone). Brief: `docs/capstone-brief.md`. Scope: `docs/scope/scope.md`. AI usage log: `docs/ai-usage-log.md` (add an entry for every AI step). Build log: [Book Worm Build Log](https://claude.ai/artifact/7V6PiSMXTEEtbqA2gfUis5).

## Stack

- **Language / Runtime**: Java 17 (IBM Semeru OpenJ9 17.0.7)
- **Framework**: Spring Boot 3.5.16, layered monolith (controller → service → repository)
- **Key dependencies**: Spring Web, Bean Validation, Spring Data JPA (Hibernate, `ddl-auto=validate`), Flyway, PostgreSQL 16; H2 (PostgreSQL mode) for tests. Later: Spring Security JWT (feature 5), springdoc (feature 4)
- **Package manager**: Maven 3.9.4, with the committed wrapper (`mvnw`, `mvnw.cmd`)

## Build approach

Journey (finish one customer journey from the use case slide end to end, then start the next).

## Commands

```bash
# Build + all tests (H2, no PostgreSQL needed)
mvn clean install

# Run against local PostgreSQL (needs DB_USERNAME/DB_PASSWORD env vars or ./application-local.properties)
mvn spring-boot:run

# Tests only
mvn test
```

## Specs

Stored in `docs/specs/`. Format: `docs/specs/NNNN-title/index.md`. Spec 0001 is the stack decision.

## Rules

- SOLID OOP: small single purpose classes; constructor injection only (no field `@Autowired`, no service locator); composition over inheritance; name classes after what they do.
- Package by layer under `com.bookworm.ebookstore`: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `exception`, `config`. Controllers stay thin and call services; only repositories touch the database.
- Naming: `BookController`, `BookService`, `BookRepository`, `Book` entity, record DTOs like `BookResponse` / `CreateBookRequest`, hand written `BookMapper`. Tables and columns are `snake_case`. No Lombok, no MapStruct.
- Errors: services throw named exceptions (e.g. `ResourceNotFoundException`); only `GlobalExceptionHandler` turns them into `ProblemDetail` (RFC 9457) responses. No try/catch building error bodies in controllers.
- Validate request DTOs with Bean Validation and `@Valid`; validation errors flow to the global handler as 400.
- Schema changes only through new Flyway scripts in `src/main/resources/db/migration` (`V<n>__name.sql`, numbers only go up, never edit an applied one). SQL must run on both PostgreSQL and H2 in PostgreSQL mode.
- API first: `src/main/resources/openapi.yaml` is the contract; controllers must match it.
- Secrets never in git: DB credentials only from env vars or the gitignored `application-local.properties`. Never store full card numbers or CVV.
- Conventional commit subjects (`feat:`, `fix:`, `docs:`, `test:`, `chore:`), ending with the agent's attribution line.

## Tooling

- Formatting and linting: minimal for now (IDE default formatting, no formatter plugin).
- Pre-commit hooks: none. Run `mvn clean install` before pushing.
- Testing: verification is manual by default (`/check verify` plus the Insomnia collection). Features tagged `· Beta` in the scope also get a `/test` pass (JUnit 5, `@WebMvcTest` + MockMvc, plain service tests). `mvn clean install` must stay green.
- CI: none (not required by the capstone deliverables; skipped).

## Git

- integration: on
- commit: end-of-build
- branches: foundations (features 1 to 4) on `main`; the API work goes on `feature/api-implementation` (name required by the capstone), merged through one PR.
- push: always (safe for this repo; commit and push without asking). Opening a PR still asks first.

## Agent skills

- [java-springboot](.claude/skills/java-springboot/): `github/awesome-copilot` (commit `3a68501`), general Spring Boot practices (constructor injection, DTOs, `@Transactional` services, SLF4J logging). Where it differs from this file (package by feature, Testcontainers, profiles, YAML), the rules above and spec 0001 win.

MCP servers: none (Postgres MCP offered, skipped for now).

## Context files

<!-- Nested AGENTS.md files are listed here as they are created -->

_Drafted by /audit from the repo, worth a quick human pass. Edit freely: once a line stops matching this draft, later runs treat it as curated and will flag rather than overwrite it._
