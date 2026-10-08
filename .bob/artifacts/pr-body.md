## What

This PR delivers the full Book Worm e-bookstore REST API: every customer journey from browsing the catalogue to paying for a book and cancelling an order. All 25 endpoints defined in the OpenAPI contract are implemented, tested, and green. A README and an Insomnia collection are included so a reviewer can run and verify the whole API without reading the code.

## Why

IBM Applied AI Specialist (Cloud FullStack) capstone requirement. The brief asks for a Spring Boot 3 and PostgreSQL REST API covering the 12 customer journeys on the use case slide, built API-first with an OpenAPI spec, automated tests, JWT authentication, and a documented agentic IDE workflow.

Specs governing this branch:
- [docs/specs/0001-stack-architecture/index.md](docs/specs/0001-stack-architecture/index.md) — Java 17, Spring Boot 3.5.16, Maven, PostgreSQL 16, H2 for tests
- [docs/specs/0002-data-model/index.md](docs/specs/0002-data-model/index.md) — 13 tables, Flyway migrations, seed catalogue
- [docs/specs/0003-api-contract/index.md](docs/specs/0003-api-contract/index.md) — 25 operations, OpenAPI 3.0.3, RFC 9457 errors, Swagger UI
- [docs/specs/0004-authentication/index.md](docs/specs/0004-authentication/index.md) — BCrypt passwords, HS256 JWT, Spring Security OAuth2 resource server
- [docs/specs/0005-checkout-and-order/index.md](docs/specs/0005-checkout-and-order/index.md) — server-side totals, stock lock, gift points, single transaction checkout

AI tool used: IBM Bob (from 2026-10-07 onward, after initial setup in Claude Code). Full workflow and every prompt are in [docs/ai-usage-log.md](docs/ai-usage-log.md).

## Changes

**Foundation**
- Project scaffold: `pom.xml` with Spring Web, Data JPA, Flyway, Security, OAuth2 Resource Server, PostgreSQL driver, H2 for tests
- Flyway `V1__create_schema.sql`: 13 tables, named constraints, sequence, indexes
- Flyway `V2__seed_catalogue.sql`: 19 categories, 10 genres, 9 authors, 4 publishers, 27 books in Philippine pesos
- `openapi.yaml` (hand-written, 25 operations, RFC 9457 Problem schema, bearer security, Swagger UI at `/swagger-ui.html`)
- `GlobalExceptionHandler` with `ApiErrorCode` mapping validation, auth, stock and business errors to `application/problem+json`
- Spring Security filter chain: BCrypt, HS256 JWT (60 min), stateless, public browsing passes without a token

**Authentication (Journey step 1)**
- `POST /api/v1/auth/register` and `POST /api/v1/auth/login` returning a signed JWT
- Password masked in `toString()`, email lowercased and trimmed at intake

**Catalogue browsing (Journey steps 3, 5, 6, 7)**
- `GET /api/v1/categories`, `GET /api/v1/books` (full-text search, category, author, publisher, language, format, price range, sort), `GET /api/v1/books/{id}`, `GET /api/v1/books/{id}/related`
- `GET /api/v1/authors`, `GET /api/v1/authors/{id}`, `GET /api/v1/publishers`, `GET /api/v1/publishers/{id}`
- Estimated delivery date from format (eBook same day, print 5 days), related books ranked by author match and shared genre count

**Cart (Journey step 8)**
- `GET /api/v1/cart`, `POST /api/v1/cart/items`, `PUT /api/v1/cart/items/{bookId}`, `DELETE /api/v1/cart/items/{bookId}`, `DELETE /api/v1/cart`
- eBook quantity capped at 1, print at 10, stock checked on every write, per-user pessimistic lock

**Checkout and order (Journey steps 9–10)**
- `GET /api/v1/me`, `GET /api/v1/me/addresses`, `POST /api/v1/me/addresses`
- `POST /api/v1/orders`: server-computed subtotal, 12% VAT, delivery charge, gift points deduction, stock decrement in `book_id` order to prevent deadlock, basket clear, all in one transaction
- `GET /api/v1/orders`, `GET /api/v1/orders/{orderId}`

**Simulated payment (Journey steps 10–12)**
- `POST /api/v1/orders/{orderId}/payments`: credit card, debit card, UPI, wallet; Luhn check; only last 4 digits stored; gift points earned on confirmation
- Full card number, CVV, and expiry never reach the database or logs

**Cancel within 48 hours (Journey step 12)**
- `POST /api/v1/orders/{orderId}/cancel`: status guards, stock restored for print books, redeemed points restored, earned points reversed on confirmed orders, payment marked REFUNDED

**Order history and Buy It Again (Journey step 2)**
- `GET /api/v1/orders` returns the caller's orders newest first
- `POST /api/v1/orders/{orderId}/buy-again`: copies past order items to cart, skips out-of-stock and eBooks already owned, reports added and skipped books

**Recommendations (Journey steps 1–2)**
- `GET /api/v1/me/recommendations`: scores by author match (2 pts) and shared genre count, tops up with newest books for new users

**Run kit**
- `README.md`: PostgreSQL Windows setup, env var credential approach, run steps, full 25-endpoint table
- `docs/insomnia-collection.json`: Insomnia v4 collection, 6 folders covering all 12 journeys

## How to test / verify

1. Follow the setup steps in `README.md` to create the PostgreSQL role and database.
2. Set `DB_USERNAME` and `DB_PASSWORD` as environment variables (or copy `application-local.properties.example`).
3. Run `mvn clean install` — expect 141 tests, 0 failures.
4. Run `mvn spring-boot:run` — Flyway applies two migrations; the app starts on port 8080.
5. Open http://localhost:8080/swagger-ui.html to browse the live API contract.
6. Import `docs/insomnia-collection.json` into Insomnia, set `base_url` to `http://localhost:8080`, run Register, copy the token into `token`, then run folders in order.

## Risk and rollout

Low risk. No cloud deploy is required; local run only (as allowed by the brief). No feature flags. Flyway migrations are applied automatically on startup. Credentials come from environment variables only and nothing sensitive is committed.

The `local-db-setup.sql` file at the project root contains a randomly generated password for the `bookworm_app` role. It is committed for local setup convenience but is not connected to any shared or production system.

## Notes for reviewers

- All totals (subtotal, VAT, delivery charge, gift points discount, total amount) are computed on the server. The client sends a basket and an address; the server computes everything else.
- The order creation transaction uses `SELECT ... FOR UPDATE` on the user row so two concurrent checkouts cannot double-spend gift points or overrun stock.
- Payment is intentionally always simulated as SUCCESS. No payment gateway is involved; the request is validated and then discarded except for the method, last 4 digits, a generated transaction ID, and the status.
- `docs/ai-usage-log.md` records every AI prompt, what was accepted or changed, and which tool (Claude Code or IBM Bob) ran each step. It is the primary evidence for the agentic IDE section of the rubric.
