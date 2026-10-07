# Scope: Book Worm e-bookstore backend

A Spring Boot and PostgreSQL REST API for an online bookstore where customers browse, choose and buy books. It is the IBM Applied AI Specialist (Cloud FullStack) capstone, so the target is the smallest build that rates "Met" on every review criterion: agentic IDE usage, API design, a working app, and best practices.

**Build approach:** Journey (finish one customer journey from the use case slide end to end, then start the next).
**Workflow:** Alpha (after `/develop`, run `/check verify` on the real app). A few risky features carry `· Beta` so they also get a `/test` pass, because a passing `mvn test` is part of the evidence. `/architect` is the recommended first stop for a feature with a real decision, but you can skip it when you already know the build.

**MVP boundary:** the 12 customer journeys on the use case slide, each built thin. Everything else waits in Deferred.
**Agentic tool:** Claude Code for features 1 to 4 design, then IBM Bob as well (from 2026-10-07; workflow in `.bob/`). Kiro is not used. The AI usage log names the tool on every entry, and the report says so plainly.
**Deployability:** skipped (local run only, which the brief allows).

_These are recommendations to keep your build orderly, not requirements. Skip anything that does not fit: if you already know how to build a feature, use `/develop` and skip `/architect`. You decide when a feature is `done`._

## At a glance

| # | Feature | Phase | Status |
|---|---------|-------|--------|
| 1 | Stack & architecture | Foundation | done |
| 2 | Coding standards & tooling | Foundation | done |
| 3 | Data model | Foundation | done |
| 4 | API contract (OpenAPI) | Foundation | done |
| 5 | Authentication | Foundation | planned |
| 6 | Browse the catalogue | Journey 1: Catalogue | planned |
| 7 | Book detail | Journey 1: Catalogue | planned |
| 8 | Basket | Journey 2: Payment & purchase | planned |
| 9 | Checkout and order | Journey 2: Payment & purchase | planned |
| 10 | Simulated payment and confirmation | Journey 2: Payment & purchase | planned |
| 11 | Cancel within 48 hours | Journey 2: Payment & purchase | planned |
| 12 | Order history and Buy It Again | Journey 3: E-store Home | planned |
| 13 | Recommendations | Journey 3: E-store Home | planned |
| 14 | Run kit (Postgres setup, README, Insomnia) | Submission | planned |
| 15 | Git branch and pull request | Submission | planned |
| 16 | Walkthrough video, report and submission | Submission | planned |

## Foundations

### 1. Stack & architecture · done
The brief fixes Java 17, Spring Boot 3, Maven and PostgreSQL. This decides the rest: package layout (controller, service, repository, DTO), how migrations and seed data run, how JWT is issued, and what database the tests use. Then it scaffolds a project that boots.
**Done when:** the choices are recorded in a spec, and `mvn clean install` and `mvn spring-boot:run` succeed on an empty app with DB credentials read from environment variables (nothing secret in git).
spec [0001](../specs/0001-stack-architecture/index.md) · code in `src/`, `pom.xml`
- [x] Decide the stack (spec): `/architect stack & architecture`
- [x] Scaffold from the decision: `/develop stack & architecture`
- [x] Verify it: `/check verify stack & architecture` (skipped: you marked it done after `mvn clean install` passed and `mvn spring-boot:run` started against PostgreSQL 16.15)

### 2. Coding standards & tooling · done
Records the project conventions in `AGENTS.md` from the real scaffold, then adds the light tooling (formatting, `.gitignore` for local properties).
**Done when:** root `AGENTS.md` reflects the real stack, and the build runs clean with no secrets tracked.
- [x] Capture conventions + tooling choices: `/audit`
- [x] Install the tooling: skipped (no formatter, no hooks, and CI is not a capstone deliverable)

### 3. Data model · done
Trims the draft in `docs/data-model.md` to the MVP (drops wishlist, reviews and coupons) and settles its open questions: category vs tags, formats, gift point rates, one or two step payment, and the order totals are calculated in.
**Done when:** the ERD and table list match the MVP and every open question has an answer, and the schema plus seed data (categories, authors, publishers, about 20 books) load into PostgreSQL.
spec [0002](../specs/0002-data-model/index.md) · code in `src/main/resources/db/migration/`, `src/main/java/com/bookworm/ebookstore/{entity,repository,config}/`
- [x] Design it (spec): `/architect data model`
- [x] Build it: `/develop data model`
   - [x] Schema migration V1: 13 tables, named constraints, sequence, indexes (AC-1, AC-2, AC-4, AC-6)
   - [x] Seed catalogue V2: 19 categories, 10 genres, authors, publishers, 24+ books in pesos (AC-3)
   - [x] Entities, repositories, `app.*` config and Clock bean (AC-5, AC-6)
   - [x] Tests on H2, run on PostgreSQL, final ERD in `docs/data-model.md` (AC-1 to AC-7)
- [x] Verify it: `/check verify data model` (skipped: you marked it done after `mvn clean install` passed with 40 tests and `mvn spring-boot:run` applied both migrations on PostgreSQL 16.15)

### 4. API contract (OpenAPI) · done
API first: write `src/main/resources/openapi.yaml` before the code, with one endpoint group per journey step, request and response schemas, the error format and JWT security. The code must match it.
**Done when:** the spec validates, covers all 12 journey steps, and the endpoint list can go straight into the README.
spec [0003](../specs/0003-api-contract/index.md) · code in `src/main/resources/openapi.yaml`, `src/main/java/com/bookworm/ebookstore/{config,exception}/`
- [x] Design it (spec): `/architect API contract`
- [x] Build it: `/develop API contract`
   - [x] Write `openapi.yaml`: 25 operations, schemas, Problem and error codes, shared conventions (AC-2, AC-4, AC-6, AC-7, AC-8)
   - [x] Contract tests: swagger-parser validity, operation, paging and sensitive field checks (AC-1, AC-2, AC-6, AC-8)
   - [x] Error codes: `ApiErrorCode` and `GlobalExceptionHandler` mapping with tests (AC-4)
   - [x] Drift test and Swagger UI through springdoc (AC-3, AC-5)
- [x] Verify it: `/check verify API contract` (skipped: you marked it done after `mvn clean install` passed with 54 tests and Swagger UI was verified)

### 5. Authentication · needs a decision · Beta
Journey step 1: register and login. Passwords are hashed with BCrypt, login returns a JWT, and every customer endpoint requires it.
**Done when:** a user can register and log in and gets a token; protected calls without a valid token return 401; wrong credentials and a duplicate email return clear errors.
- [ ] Design it (spec): `/architect authentication`

## Journey 1: Catalogue (slide steps 3, 5, 6, 7)

### 6. Browse the catalogue
Public browsing: list categories, list books in a category, search by text with simple filters (language, format, price range, sort), and browse by brand (authors and publishers).
**Done when:** each browse call returns paged books with the data a book card needs, including the estimated delivery date; an unknown category returns 404; an empty result returns an empty page, not an error.
- [ ] Build it: `/develop browse the catalogue`

### 7. Book detail
One book with everything the product page shows (covers, author with bio, publisher, format, genre tags, price, stock, copies sold, delivery date) plus related books (same genre or author, excluding itself).
**Done when:** detail and related calls return the right book and a related list; an unknown id returns 404.
- [ ] Build it: `/develop book detail`

## Journey 2: Payment & purchase (slide steps 8 to 12)

### 8. Basket
Journey step 8: the logged in user's server side basket. Add a book, change its quantity, remove it, and view it with a running subtotal.
**Done when:** basket changes persist per user; adding more than the stock or a quantity below 1 is rejected with a clear error.
- [ ] Build it: `/develop basket`

### 9. Checkout and order · needs a decision · Beta
Journey steps 9 and 10: choose a saved or new delivery address, optionally redeem gift points, and place the order. The server computes the subtotal, tax, delivery charge, points discount and total, checks and decrements stock, and empties the basket.
**Done when:** an order is created with correct server side totals and an address snapshot; redeeming more points than the balance or the total is rejected; insufficient stock is rejected and nothing is changed.
- [ ] Design it (spec): `/architect checkout and order`

### 10. Simulated payment and confirmation · Beta
Journey steps 10 to 12: pay with credit card, debit card, UPI or wallet. The request is validated and then discarded except for the method, the last 4 digits, the status and a generated transaction id. The order is confirmed and gift points are earned.
**Done when:** a valid payment confirms the order and returns a confirmation with the purchased books; an invalid card format is rejected; no full card number or CVV ever reaches the database or the logs.
- [ ] Build it: `/develop simulated payment and confirmation`

### 11. Cancel within 48 hours · Beta
Journey step 12: the customer cancels a confirmed order within 48 hours, before it ships. The payment is marked refunded and stock and gift points are restored.
**Done when:** a cancel inside the window succeeds and restores stock and points; a cancel after 48 hours, on a shipped order, or on someone else's order is rejected.
- [ ] Build it: `/develop cancel within 48 hours`

## Journey 3: E-store Home (slide steps 1 and 2)

### 12. Order history and Buy It Again
Journey step 2: the user's past orders, newest first, with their items. Buy It Again copies a past order's items back into the basket (skipping anything out of stock).
**Done when:** history shows only the caller's orders; Buy It Again fills the basket and reports any skipped books.
- [ ] Build it: `/develop order history and buy it again`

### 13. Recommendations
Journey step 2: books in the genres and by the authors the user has bought, excluding books they already own; a new user gets the newest books instead.
**Done when:** a buyer gets books related to their history; a user with no orders still gets a non empty list.
- [ ] Build it: `/develop recommendations`

## Submission

### 14. Run kit (Postgres setup, README, Insomnia)
Everything a reviewer needs to run and check it: PostgreSQL setup steps for Windows, `application.properties` reading credentials from environment variables, a README with run steps and the endpoint list, and an Insomnia collection that walks all 12 journeys.
**Done when:** following the README on a fresh Windows machine starts the app; the Insomnia collection runs every journey successfully and the new rows are visible in PostgreSQL; `mvn test` passes.
- [ ] Build it: `/develop run kit`

### 15. Git branch and pull request
A personal GitHub repo with the work on `feature/api-implementation` and a pull request whose description carries the API documentation. Your manager is added as a collaborator.
**Done when:** the PR is open against `main` with the endpoint list, run steps and a link to the OpenAPI file.
- [ ] Write the PR: `/document pr`

### 16. Walkthrough video, report and submission
The required deliverable is a short video (about 3 minutes) explaining the steps taken with the agentic IDE, submitted with the GitHub repo link (slide 12 and the AI review page). An optional `Capstone-Report.pdf` (5 to 8 pages) can be attached as backup: the AI workflow with screenshots, the API design, proof that it runs and test results. `docs/ai-usage-log.md` is kept current at every step and is the script source.
**Done when:** the video is recorded and shows the AI workflow, the OpenAPI spec, the app starting, Insomnia calls, database rows and `mvn test`; the repo and PR links plus a 4 to 6 sentence summary are ready for the text field; screenshots in `docs/screenshots/` back it up.
- [ ] Write the video script from the AI usage log
- [ ] Record the video (about 3 minutes)
- [ ] Optional: assemble `Capstone-Report.pdf`

## Deferred
Outside the 12 journeys, kept so the plan stays honest. Any of them can come back as a later release.
- **Wishlist** ("My Wishlist" in the wireframe nav)
- **Reviews and ratings** (product page reviews)
- **Coupons** ("Apply Coupon" in checkout)
- **Bestsellers and New Launches** home sections
- **"My Writers"** nav item
- **Guest users, store and catalogue admin** (architecture slide)
- **Shipping, shipment tracking and returns** (architecture slide)
- **Deployability** (Dockerfile and docker compose, or ROKS)
- **Reverse contract drift check** (every operation in `openapi.yaml` has a handler; from spec 0003)

## Legend

**The decision box.** Every feature carries exactly one, the sub task whose label ends with `(spec)`. Its wording varies (`Design it (spec)` normally, `Decide the stack (spec)` on Stack & architecture), so skills locate it by that `(spec)` suffix, never by an exact label. Every other box is an execution box and `/architect` never ticks one.

**Feature lifecycle**: the scope updates as a feature moves; each row is what it shows and who sets it:

| State | Set by | The feature shows |
|---|---|---|
| `planned` · needs a decision | `/scope` | one box: `Design it (spec): /architect <feature>` |
| `in-progress` (designed) | **`/architect` at spec capture** | `Design it` ticked; spec linked; `Build it: /develop <feature>` + **2 to 5 milestones**; the tier's closing boxes (`Verify it` Alpha+, `Test it` Beta+, `Review it` + `Document it` GA); any surfaced follow up enrolled |
| `in-progress` (building) | `/develop` | milestone sub boxes tick one by one; code pointer filled |
| `in-progress` (verified) | `/check verify` | `Build it` + milestones ticked; `Verify it` ticked |
| `done` | **you, when you decide it is** (any skill sets it when you say so); `/sync` reconciles | boxes you ran ticked, skipped ones marked skipped; the tier's last stage (`Prototype` → after `/develop`; `Alpha` → after `/check verify`; `Beta`/`GA` → after `/test`) is the suggested point to call it done; `/sync` captures conventions |

- **Next step** = the first unticked box (always a command or a tracked milestone).
- **needs a decision** = run `/architect` first; otherwise straight to `/develop` (or `/audit` for standards & tooling). The tag drops once the spec is captured.
- **Atomic build tasks live in the spec's `## Build plan`, not here**: the scope carries only the milestone rollup.
- **Status** `planned` → `in-progress` → `done`, plus `existing` (pre workflow) and `dropped` (de scoped, kept for history).
- **Workflow tier tag** beside a heading (e.g. `· Beta`) sets that one feature's rigor above or below the project default; no tag inherits the default.
- **Workflow** (header line) is the project default, what runs after `/develop`: **Prototype** = nothing; **Alpha** = `/check verify`; **Beta** = `/check verify` then `/test`; **GA** = adds a fresh model `/check review` then `/document`.
- **Pointer line** (`spec <n> · code in <path>`): the spec link added by `/architect`, the code path by `/develop`.
