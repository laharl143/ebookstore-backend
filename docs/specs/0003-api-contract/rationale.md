# 0003. REST API contract: rationale

Decision record for [index.md](index.md). `/develop` does not read this file.

## Context

The capstone brief asks for API first development: an OpenAPI file at `src/main/resources/openapi.yaml`, written before or alongside the code, with controllers that match it. Spec 0001 already settled the tooling (a hand written file, no code generation, springdoc serving Swagger UI, `ProblemDetail` errors, HS256 JWT bearer tokens). Spec 0002 settled every stored value: tables, totals, points, order numbers, delivery dates and payment outcomes. What is left is the shape of the API itself: which URLs exist, what they take and return, how errors look, and how the contract stays true as nine more features add controllers.

Three forces shape it. First, the reviewer judges "API design" and tries the app in Insomnia, so the surface must read clearly and cover all of the brief's customer journeys. Second, journey features are built one at a time, so without a fixed contract each one would invent its own paging, money format and errors, and they would drift apart. Third, a hand written file can silently disagree with the code, and nothing in a plain Spring build notices.

Feature 5 (authentication) has no spec yet. This contract has to fix the auth endpoints' request and response shapes and the token's `sub` claim now, so feature 5 inherits them as constraints rather than deciding them.

## Options considered

### Option 1: Resource oriented REST with action subresources

Nouns for resources, HTTP verbs for reads and edits, and `POST /orders/{id}/payments`, `/cancel`, `/buy-again` for the order actions that trigger state changes and side effects.

**Pros**: reads like textbook REST to a reviewer; each action gets its own request schema, errors and 402/409 statuses; ownership checks hang off one `{orderId}`.
**Cons**: `/cancel` and `/buy-again` are verbs in a path, which REST purists dislike; more operations to document than a status PATCH.

### Option 2: Pure CRUD with status updates

Order state changes through `PATCH /orders/{id}` with `{ "status": "CANCELLED" }`; payment as `POST /payments` with an `orderId`.

**Pros**: fewest URLs; strictly resource shaped.
**Cons**: one PATCH hides very different side effects (refunds, stock and points changes) behind one field; allowed values depend on the current state, which is hard to express in OpenAPI; "buy again" has no natural resource.

### Option 3: RPC style endpoints

`POST /checkout`, `POST /pay`, `POST /cancel-order`, `POST /reorder`, each taking ids in the body.

**Pros**: maps one to one onto service methods; simple to write.
**Cons**: reviewers grading "API design" expect REST; ids in bodies make ownership checks and Location headers awkward; no resource to GET back.

## Rationale

Option 1 fits the forces best. A reviewer grading API design sees conventional resources, and the three order actions stay explicit, each with its own schema, status codes and error codes. That matters because the risky behaviour (declines, the 48 hour window, stock and points reversals) lives exactly there. Option 2 would bury those side effects behind a status field that OpenAPI cannot describe per state. Option 3 is quicker to write but reads as RPC, and it pushes ownership into request bodies.

The surrounding conventions follow the same line. One `Problem` schema with a stable `code` gives clients something to switch on without parsing English. 404 for another user's data stops id probing. Paging on every list keeps responses bounded. The drift test turns "controllers match the spec" into a build failure instead of a promise, which matters most because the contract is written once now and then built against by nine later features.

## Decision log (auto picked)

You asked for the recommended option at every decision after the first round. Rows 1 to 4 are your own answers; every other row was auto picked. Review and override any row.

| # | Question | Options | Pick |
|---|---|---|---|
| 1 | Base path and versioning | `/api/v1` · `/api` · no prefix | `/api/v1` (auto picked; your answer to this question was the "pick recommended" instruction) |
| 2 | OpenAPI version | 3.0.3 · 3.1.0 | 3.0.3 (your answer) |
| 3 | Money in JSON | decimal + `currency` · decimal string · integer centavos | decimal + `currency` (your answer) |
| 4 | Dates and times | UTC instants + local dates · Manila offset | UTC instants + local dates (your answer) |
| 5 | API style | resource REST + action subresources · CRUD with status PATCH · RPC | resource REST + action subresources |
| 6 | Page shape | own `PageResponse` (`content`, `page`, `size`, `totalElements`, `totalPages`) · Spring `Page` JSON as is · cursor paging | own `PageResponse` (Spring's own JSON is not a stable contract) |
| 7 | Page numbering | 0 based · 1 based | 0 based (matches Spring `Pageable`, no conversion) |
| 8 | Page size | default 12, max 50 · default 20, max 100 | 12 / 50 (spec 0002 value sourcing) |
| 9 | Error body | `ProblemDetail` + `code` + `errors` · `ProblemDetail` with `type` URIs · plain `ProblemDetail` | `code` property (simple to switch on, no fake domain for `type` URIs) |
| 10 | Someone else's order or address | 404 · 403 | 404 (no id probing) |
| 11 | Business limit errors (points, quantity) | 400 · 422 | 400 (spec 0002 already says 400; one less status to explain) |
| 12 | Catalogue browse | one `GET /books` with filters · nested `/categories/{slug}/books`, `/authors/{id}/books` | one `/books` with filters (one paging and sort implementation) |
| 13 | Sort parameter | `sort` enum with direction in the value · `sort` + `direction` | `sort` enum (`relevance`, `price_asc`, `price_desc`, `newest`, `bestselling`) |
| 14 | Category filter key | slug · id | slug (readable URLs) |
| 15 | Book card description | server cut `shortDescription` · full description | `shortDescription` (150 characters at a word boundary) |
| 16 | Show other formats on detail | yes (`otherFormats`) · no | yes (one row per format, spec 0002) |
| 17 | Related ordering | same author, shared genres, copies sold · random · newest | same author, then shared genre count, then copies sold |
| 18 | Authors and publishers lists | paged · unpaged | paged ("every list paginates") |
| 19 | Basket line key | `bookId` · cart item id | `bookId` (one line per user and book) |
| 20 | Basket mutation response | full `CartResponse` · 204 / line only | full `CartResponse` (client redraws totals in one call) |
| 21 | Totals preview in the basket | yes (VAT, delivery, estimated total) · subtotal only | yes (the wireframe's grand total panel) |
| 22 | Order actions | `/payments`, `/cancel`, `/buy-again` subresources | as listed |
| 23 | Order ids in paths | numeric `id` · `orderNumber` | numeric `id` (`orderNumber` is shown in the body) |
| 24 | History contents | all statuses with items, optional `status` filter · bought statuses only | all statuses with items |
| 25 | Buy again allowed for | any order the user owns · bought orders only | any order (also handy after a cancel) |
| 26 | Register response | token + user (`AuthResponse`) · user only | token + user (one less call in the demo) |
| 27 | Password rule | 8 to 72 characters · stronger composition rules | 8 to 72 (72 is the BCrypt input limit) |
| 28 | Profile and points | `GET /me` with balance · add a points ledger endpoint | `GET /me` only (the ledger is not in a journey) |
| 29 | Address endpoints | list + create · full CRUD | list + create (spec 0002 follow up) |
| 30 | Recommendations path and shape | `GET /me/recommendations` list · paged under `/recommendations` | `/me/recommendations`, plain list with `size` |
| 31 | Payment request shape | one flat schema with per method rules · `oneOf` with a discriminator | flat (simpler in 3.0.3 and in Bean Validation) |
| 32 | Card checks | 13 to 19 digits + Luhn, CVV, expiry not past · digits only | digits + Luhn + CVV + expiry |
| 33 | E wallet input | `walletMobileNumber`, checked then discarded · nothing | `walletMobileNumber` (something real to validate, never stored) |
| 34 | Declined payment | 402 Problem with `transactionId` · 201 with `status: FAILED` | 402 (a decline reads as an error in Insomnia, the row is still kept) |
| 35 | Idempotency keys | none (guarded state makes retries safe) · `Idempotency-Key` header on POST /orders and payments | none |
| 36 | Contract validity check | `swagger-parser` JUnit test · Redocly CLI (needs Node) · manual paste in an online editor | `swagger-parser` test (runs in `mvn clean install`) |
| 37 | Code to contract check | drift test, code paths must exist in the file · openapi-generator interfaces · none | drift test (keeps spec 0001's no codegen choice) |
| 38 | Swagger UI source | springdoc showing `/openapi.yaml`, generated docs off · springdoc generated docs | show the hand written file |
| 39 | Rate limiting, CORS | none (local only) · add both | none, recorded as a negative consequence |
| 40 | References section | none · sources · sources + links | none (keeps the spec lean) |
| 41 | Look for Agent Skills and MCP servers for springdoc and swagger-parser? | Yes, find them · I'll name them · No, skip · Not now, later | Recommended was "Yes, find them". Not run: the search downloads and runs a third party npm package, which needs your explicit go ahead even under "pick recommended". Treated as "Not now, later" (see Follow-up). |
| 42 | Cross check the drafted spec? | Another model · Same model · I'll review it myself · Skip | Another model (recommended for a foundational spec at Alpha). A read only Sonnet pass found 18 decision gaps and 7 soundness issues. |
| 43 | How to handle the cross check findings | Apply the recommended fixes · Answer each one · Leave them | Apply the recommended fixes. Two were adjusted: email normalization happens in the request record's compact constructor (not a custom JSON deserializer), and the declined payment row is written with `REQUIRES_NEW` so services keep throwing named exceptions (`AGENTS.md`). Main changes: paging applies to four operations only; no `Location` for addresses; parameter error codes; check orders; buy again counts the basket; user row lock for checkout and cart; cancel window inside the guarded update; springdoc api docs stay on; `multipleOf` dropped; drift test normalizes path variables. |
| 44 | Accept the spec | Accept · Change something · Rethink | Accept (auto picked under "pick recommended"; you have not reviewed it yet). |
