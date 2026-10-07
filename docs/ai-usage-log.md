# AI usage log

Tool: Claude Code (desktop app, Code tab), model Claude Opus 5.5.
Each entry records the prompt given, what the AI produced, and what I accepted, changed or rejected.

---

## 1. Data model proposal (workflow step 2)

**Date:** 2026-10-07

**Prompt (summary):** Read `docs/capstone-brief.md`; propose the data model as a Mermaid ERD plus
table list; this step is my manual design and I review it before anything else is generated. Ask
before making assumptions not covered in the brief.

**AI output:** `docs/data-model.md` — 17-table ERD, table list with constraints, proposed order
lifecycle, indexes, and 10 open questions where the brief is silent (category vs tags, formats,
authors per book, "My Writers", gift-point rates, payment flow, coupon limits, review eligibility,
admin role, order of totals).

**Review / changes by me:** _pending (open questions will be settled in the data model spec)_

---

## 2. Scoping the minimum passing build

**Date:** 2026-10-07

**Prompt (summary):** Shared the capstone AI review rubric and asked for a bare minimum build that
passes, using the `/scope` skill. Then shared the use case slide (12 customer journeys) and the
architecture slide.

**AI output:** `docs/scope/scope.md`, a 16 feature plan: 5 foundations (stack, standards, data
model, OpenAPI contract, auth), the 12 journeys grouped into 3 journey phases (Catalogue,
Payment & purchase, E-store Home), and 3 submission features (run kit, PR, report). Wishlist,
reviews, coupons, bestsellers, admin, shipping and Docker were moved to Deferred.

**My decisions (asked as option panels):**
- MVP = the 12 journeys on the use case slide, built thin (accepted the AI recommendation).
- Agentic tool = Claude Code only; no IBM Bob access, and the report will say so.
- Deployability skipped; no hard deadline.
- Build approach = Journey (accepted the recommendation).
- Workflow = Alpha (I chose this over the AI's Beta recommendation). The AI tagged auth, checkout,
  payment and cancel as Beta so they still get tests.

---

## 3. Stack & architecture (architect + develop, run by a Claude Code subagent)

**Date:** 2026-10-07

**Prompt (summary):** I asked the main Claude Code session to spawn a subagent that runs `/architect stack & architecture` and then `/develop stack & architecture` for scope feature 1, choosing the recommended option at every decision because I was not available to answer questions.

**AI output:**
- Environment check: IBM Semeru OpenJDK 17.0.7, Maven 3.9.4, PostgreSQL 16.15 running as the Windows service `postgresql-x64-16` on port 5432 (`psql` not on PATH), Docker not installed.
- Version check against Maven Central: newest Spring Boot 3.x is 3.5.16 (its free support ended 2026-06-30; 4.x is outside the brief's 3.x rule).
- Spec `docs/specs/0001-stack-architecture/` (`index.md`, `rationale.md`, `verify.md`), status In Progress, with a 25 row decision log of every auto picked option. A read only cross check on a second model (Sonnet) found 20 gaps and 8 soundness notes; the recommended fixes were applied (datasource wiring, H2 test config, `bookworm_app` role, migration numbering, JWT encoder and decoder beans).
- Scaffold: `pom.xml` (web, validation, data JPA, Flyway, PostgreSQL driver, test, H2), Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`), `EbookstoreApplication`, `GlobalExceptionHandler` (ProblemDetail errors) and `ResourceNotFoundException`, `application.properties` reading `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`, a test `application.properties` on H2, `application-local.properties.example` with placeholders only, and one context load test.
- `mvn clean install`: BUILD SUCCESS, 1 test, 0 failures. `mvn spring-boot:run` without credentials stops with `password authentication failed for user "${DB_USERNAME}"`; a successful run against PostgreSQL was not done because the AI has no database password.
- Scope feature 1: decision box and scaffold box ticked, spec linked, status in-progress; `Verify it` left open.

**Decisions auto-picked (recommended):**
- Layered monolith, package by layer under `com.bookworm.ebookstore`.
- Spring Boot 3.5.16, Maven plus a committed Maven wrapper, `application.properties`.
- Credentials from env vars, plus an optional gitignored `application-local.properties`; database `bookworm` owned by a dedicated `bookworm_app` role.
- Flyway migrations, Hibernate `ddl-auto=validate`, seed data as a Flyway migration (feature 3).
- Tests on H2 in PostgreSQL mode (Docker missing), `@WebMvcTest` slices plus one `@SpringBootTest` smoke test.
- JWT (feature 5) through Spring Security's OAuth2 resource server, HS256 with `JWT_SECRET`, 60 minutes, no refresh token, BCrypt for passwords.
- RFC 9457 ProblemDetail errors, Java record DTOs, no Lombok, hand written mappers.
- Hand written `openapi.yaml` served by springdoc, no code generation (feature 4).
- Default console logging, no Actuator; local hosting only.
- References with web verified links; cross check on another model; apply its recommended fixes; save verify steps to `verify.md`.
- Agent Skills / MCP search: recommended "find them" was not run, because it downloads and runs a third party package and nothing could be installed without my pick; recorded as a follow up.
- Spec acceptance was auto accepted under my "pick recommended" instruction; I still need to review the decision log.

**Follow up in the main session:** PostgreSQL needed the `postgres` admin password, which only I have, so the AI generated a local password for `bookworm_app`, wrote it to the gitignored `application-local.properties` and `local-db-setup.sql`, and opened psql in a terminal tab where I typed the admin password. `CREATE ROLE` and `CREATE DATABASE` succeeded, and `mvn spring-boot:run` then connected to PostgreSQL 16.15 and logged `Started EbookstoreApplication in 8.896 seconds` on port 8080.

**Review / changes by me:** _pending (decision log in spec 0001 still to review)_

---

## 4. Coding standards (`/audit`)

**Date:** 2026-10-07

**Prompt (summary):** Ran `/audit` after the scaffold. The skill asked coding standard and tooling questions as option panels, ran a tool discovery subagent (Haiku) to search for Agent Skills and MCP servers, then wrote the context files.

**AI output:** root `AGENTS.md` (stack from spec 0001, Journey build approach, commands, rules, tooling, git) and a `CLAUDE.md` pointer. Installed one Agent Skill, `java-springboot` from `github/awesome-copilot`, into `.claude/skills/` (the normal install timed out on the large repo, so the AI sparse cloned just that folder and installed from it). Scope feature 2 now has a `/develop tooling` step for the CI workflow.

**My decisions:**
- SOLID OOP style; consistent error handling, conventional commits and consistent naming as rules (all recommended).
- Minimal formatting (no plugin) and no pre commit hooks (recommended).
- Testing: manual verify by default (I chose this over the recommended unit plus integration). Beta tagged features still get `/test`.
- Basic GitHub Actions build on push and PR (recommended).
- Git integration on, commit at the end of each build (I chose this over per milestone).
- Agent Skills: only `java-springboot`; no MCP server.

**Review / changes by me:** _pending_

---

## 5. Data model design (`/architect data model`, my manual design step)

**Date:** 2026-10-07

**Prompt (summary):** Ran `/architect data model`. The AI asked the open design questions one at a time as option panels, each with a recommended answer. I answered rounds 1 to 3 myself, then asked it to pick the recommended option for the rest of the run.

**My decisions:** one category plus many genre tags; one book row per format; one author per book; brands = authors and publishers; two step checkout then payment; VAT on the subtotal, then points; **Philippine pesos** (my change: the wireframes use rupees); 1 point per ₱100, 1 point = ₱1, earned on payment; **Philippine address format** (4 digit ZIP, +63, province); 12% VAT; delivery in 5 days (eBook same day); statuses PENDING_PAYMENT, CONFIRMED, SHIPPED, DELIVERED, CANCELLED; stock reserved at order; basket keyed by user; bigint ids.

**AI output:** spec `docs/specs/0002-data-model/` (`index.md` build spec with the ERD, 13 tables, value sourcing, invariants, build plan; `rationale.md` with options, a 35 row decision log and the cross check). A read only cross check on a second model (Sonnet) found 21 gaps, including SQL types that would fail on H2, seed ids that would break identity counters, and races on stock and points. All recommended fixes were applied (auto picked). The AI also corrected its own draft from 20 to the 19 real wireframe categories instead of inventing one. Scope feature 3 now has build milestones.

**Review / changes by me:** _pending (auto picked rows 14 and 17 to 35 in the decision log still to review)_
