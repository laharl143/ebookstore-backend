## What

This PR delivers the full Book Worm e-bookstore REST API: every customer journey from browsing the catalogue to paying for a book and cancelling an order. All 25 endpoints in the OpenAPI contract are implemented and tested. The app was also run end to end against PostgreSQL, and the evidence is in `docs/screenshots/`. A README and an Insomnia collection let a reviewer run and check the whole API without reading the code.

## Why

IBM Applied AI Specialist (Cloud FullStack) capstone. The brief asks for a Spring Boot 3 and PostgreSQL REST API covering the 12 customer journeys on the use case slide, built API first with an OpenAPI spec, automated tests, JWT authentication and a documented agentic IDE workflow.

Specs governing this branch:
- [docs/specs/0001-stack-architecture/index.md](docs/specs/0001-stack-architecture/index.md): Java 17, Spring Boot 3.5.16, Maven, PostgreSQL 16, H2 for tests
- [docs/specs/0002-data-model/index.md](docs/specs/0002-data-model/index.md): 13 tables, Flyway migrations, seed catalogue in Philippine pesos
- [docs/specs/0003-api-contract/index.md](docs/specs/0003-api-contract/index.md): 25 operations, OpenAPI 3.0.3, RFC 9457 errors, Swagger UI
- [docs/specs/0004-authentication/index.md](docs/specs/0004-authentication/index.md): BCrypt passwords, HS256 JWT, Spring Security resource server
- [docs/specs/0005-checkout-and-order/index.md](docs/specs/0005-checkout-and-order/index.md): server side totals, stock lock, gift points, one transaction checkout

## How AI was used

Two agentic tools, with every prompt and decision recorded in [docs/ai-usage-log.md](docs/ai-usage-log.md):
- **Claude Code** designed specs 0001 to 0003 (each cross checked by a second model), built the data model, set up the IBM Bob workflow, and ran the end to end verification and reviews.
- **IBM Bob** built features 4 to 15 from those specs using the shared workflow in `.bob/` (`/architect`, `/develop`, `/verify`, `/sync`), and fixed the bugs the end to end run found.

The end to end run against the real database caught bugs that the unit tests missed: a declined card was accepted, cancel did not persist (so a second cancel restored stock and points twice), and after the first fix an order with a declined attempt made the order history fail. All are fixed, with regression tests.

## Changes

**Foundation**
- Project scaffold: `pom.xml` with Spring Web, Data JPA, Flyway, Security, OAuth2 Resource Server, PostgreSQL driver, H2 for tests
- Flyway `V1__create_schema.sql`: 13 tables, 61 named constraints, order number sequence, indexes
- Flyway `V2__seed_catalogue.sql`: 19 categories, 10 genres, 9 authors, 4 publishers, 27 books in pesos
- Hand written `openapi.yaml` (25 operations, Problem schema, bearer security), Swagger UI at `/swagger-ui.html`
- `GlobalExceptionHandler` with `ApiErrorCode`: every error is `application/problem+json` with a stable `code`; unexpected errors are logged
- Spring Security: BCrypt, HS256 JWT (60 minutes), stateless, public browsing needs no token

**Authentication (journey step 1)**
- `POST /api/v1/auth/register` and `POST /api/v1/auth/login` return a signed JWT
- Password masked in `toString()`, email trimmed and lower cased at intake

**Catalogue browsing (journey steps 3, 5, 6, 7)**
- `GET /api/v1/categories`, `GET /api/v1/books` (text search, category, author, publisher, language, format, price range, sort), `GET /api/v1/books/{bookId}`, `GET /api/v1/books/{bookId}/related`
- `GET /api/v1/authors`, `GET /api/v1/authors/{authorId}`, `GET /api/v1/publishers`, `GET /api/v1/publishers/{publisherId}`
- Estimated delivery date by format (eBook today, print in 5 days); related books ranked by author match and shared genres

**Basket (journey step 8)**
- `GET /api/v1/cart`, `POST /api/v1/cart/items`, `PUT /api/v1/cart/items/{bookId}`, `DELETE /api/v1/cart/items/{bookId}`, `DELETE /api/v1/cart`
- eBook quantity capped at 1, print at 10, stock checked on every write, per user lock

**Checkout and order (journey steps 9 and 10)**
- `GET /api/v1/me`, `GET /api/v1/me/addresses`, `POST /api/v1/me/addresses`
- `POST /api/v1/orders`: server computed subtotal, 12% VAT, delivery charge and gift points discount; stock taken in `book_id` order; basket cleared; all in one transaction
- `GET /api/v1/orders`, `GET /api/v1/orders/{orderId}`

**Simulated payment (journey steps 10 to 12)**
- `POST /api/v1/orders/{orderId}/payments`: `CREDIT_CARD`, `DEBIT_CARD` or `E_WALLET`; Luhn check; only the last 4 digits are stored
- A card ending in `0002` is declined: 402 `PAYMENT_DECLINED`, a FAILED payment row kept in its own transaction, the order stays payable
- On success the order is confirmed, gift points are earned and `copies_sold` rises
- Full card number, CVV and expiry never reach the database or the logs

**Cancel within 48 hours (journey step 12)**
- `POST /api/v1/orders/{orderId}/cancel`: one guarded update, 48 hours from `placed_at`; a second cancel returns 409 and changes nothing
- Print stock and redeemed points restored, earned points reversed, the successful payment marked REFUNDED with `refunded_at`, `copies_sold` reduced

**Order history and Buy It Again (journey step 2)**
- `GET /api/v1/orders` returns the caller's orders, newest first
- `POST /api/v1/orders/{orderId}/buy-again` copies a past order's books into the basket and reports any skipped as `OUT_OF_STOCK` or `LIMIT_REACHED`

**Recommendations (journey step 2)**
- `GET /api/v1/me/recommendations`: scores by author match and shared genres from the user's paid orders, topped up with the newest books (so a new user gets the newest books)

**Run kit**
- `README.md`: PostgreSQL setup on Windows, credentials from environment variables, run steps, the 25 endpoint table
- `docs/insomnia-collection.json`: Insomnia collection with 6 folders covering all 12 journeys

## How to test / verify

1. Follow the setup steps in `README.md` to create the PostgreSQL role and database.
2. Set `DB_USERNAME` and `DB_PASSWORD` as environment variables (or copy `application-local.properties.example` to `application-local.properties`).
3. Run `mvn clean install`: expect 146 tests, 0 failures.
4. Run `mvn spring-boot:run`: Flyway applies two migrations and the app starts on port 8080.
5. Open http://localhost:8080/swagger-ui.html to browse and try the live contract.
6. Import `docs/insomnia-collection.json` into Insomnia, set `base_url` to `http://localhost:8080`, run Register, copy the token into `token`, then run the folders in order.
7. Test cards: `4242424242424242` succeeds, `4000000000000002` is declined.

Evidence from the end to end run on 2026-10-08 (40 of 40 calls as expected, then checked in PostgreSQL) is in `docs/screenshots/`: Swagger UI, a live call, book detail, the journey results, the database rows and the test run.

## Risk and rollout

Low risk. Local run only, as the brief allows; no cloud deploy and no feature flags. Flyway applies the migrations on startup. Database credentials come only from environment variables or a gitignored local file, and nothing secret is committed.

## Notes for reviewers

- All totals (subtotal, VAT, delivery charge, points discount, total) are computed on the server. The client sends a basket and an address; the server computes everything else.
- Checkout and basket writes lock the user's row first (`SELECT ... FOR UPDATE`), so two requests at once cannot double spend gift points or oversell stock.
- Payments are simulated; no gateway is involved. The request is validated and then discarded, apart from the method, the last 4 digits, a generated transaction id and the status.
- `docs/ai-usage-log.md` records every AI step, what was accepted or changed, and which tool ran it. It is the main evidence for the agentic IDE part of the rubric.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
