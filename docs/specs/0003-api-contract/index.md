# 0003. REST API contract for the Book Worm store

**Date**: 2026-10-07
**Status**: Proposed

## Summary

This spec fixes the public REST API (the URLs a client calls and the JSON it sends and gets back) for every customer journey: sign in, browse the catalogue, the basket, checkout, payment, cancel, order history and recommendations. It is written first as a hand written `openapi.yaml` file (a standard, machine readable API description), and every controller built later must match it. Two automatic tests keep it honest: one checks the file is valid, the other fails the build if the code serves an endpoint the file does not describe. Swagger UI (an interactive API page) shows the file in a browser, so you can demo the API without reading code.

## Requirements

**User stories**:
- As the developer, I want one agreed contract so that every journey feature builds endpoints with the same paths, JSON shapes and error format, and none of them invents its own.
- As a reviewer, I want to read and try the API in Swagger UI and import it into Insomnia so that I can check the design without reading Java.
- As a client developer, I want every error to carry a stable machine readable `code` so that I can react to "out of stock" differently from "card declined".

**Acceptance criteria** (the contract):
- **AC-1**: `src/main/resources/openapi.yaml` is OpenAPI 3.0.3 and a JUnit test parses it with `swagger-parser` during `mvn clean install` with zero errors and zero warnings.
- **AC-2**: The file describes exactly the 25 operations in the **API surface** table below. Each has a unique `operationId`, exactly one tag (its endpoint group), a request body schema where the table lists one, a success response (2xx with a schema, or 204), every error status the table lists, and an explicit security setting (`bearerAuth`, or `security: []` for public operations). A test checks the operation count, the unique ids, the tags and the explicit security.
- **AC-3**: With the app running, `/swagger-ui.html` renders this hand written contract, loaded from `/openapi.yaml`, and both URLs answer without a token (feature 5 must keep them public). A `@SpringBootTest` GETs both URLs and checks `/openapi.yaml` returns the file.
- **AC-4**: Every 4xx and 5xx response in the file uses one `Problem` schema with media type `application/problem+json` (RFC 9457 fields plus `code`, plus `errors` for validation). The `Problem.code` enum equals the Java `ApiErrorCode` enum (a test compares them), and `GlobalExceptionHandler` sets `code` on every error body it produces, validation errors included.
- **AC-5**: A drift test fails the build when any Spring MVC handler under `/api/v1` has no matching operation (same HTTP method and same path template) in `openapi.yaml`.
- **AC-6**: The conventions below hold across the file: every path starts with `/api/v1`; properties are camelCase; money is `type: number` described as "2 decimal places" (no `multipleOf`, which breaks float based validators), and every top level response object that carries money also carries `currency` (objects nested inside one are exempt); `vatRate` is a plain number, not money; instants are `format: date-time` (UTC); calendar dates are `format: date`. The four paged operations (#8, #11, #13, #21) take `page` (0 based, default 0) and `size` (default 12, maximum 50) and return a `*Page` schema with `content`, `page`, `size`, `totalElements`, `totalPages`; #4, #6, #7 and #10 return plain arrays. A test checks the paging parameters and page schema shape on those four.
- **AC-7**: The **API surface** table in this spec lists method, path, auth, journey and purpose for every operation, so feature 14 can copy it into the README unchanged.
- **AC-8**: No response schema has a `cardNumber`, `cvv`, `expiry` or `walletMobileNumber` property, and those request properties are marked `writeOnly: true`. A test checks both.

## Decision

**Chosen option**: Option 1: Resource oriented REST with action subresources.

Nouns for resources (`/books`, `/cart`, `/orders`), plain HTTP verbs for reads and edits, and `POST` subresources for the three state changing actions on an order (`/payments`, `/cancel`, `/buy-again`), all under `/api/v1`, written by hand in OpenAPI 3.0.3 and kept in step with the code by tests.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.claude/skills/java-springboot/`)

## Rationale

Reasoning, the options weighed and the decision log: see [rationale.md](rationale.md).

## Feature design

**Data model sketch**: no new tables or columns. The API reads and writes the spec 0002 tables only.

| API resource | Backing tables (spec 0002) |
|---|---|
| User, `/me` | `users` |
| Address | `addresses` |
| Category, Author, Publisher | `categories`, `authors`, `publishers` |
| Book summary / detail | `books` joined to `categories`, `authors`, `publishers`, `book_genres`, `genres` |
| Cart | `cart_items` joined to `books` |
| Order | `orders`, `order_items` (+ `books` for cover and author), latest `payments` row |
| Payment / purchase confirmation | `payments`, `orders`, `gift_point_transactions` |

**State transitions**: the order state machine is spec 0002's. The API triggers it like this:

| Operation | Transition |
|---|---|
| `POST /orders` | creates `PENDING_PAYMENT` |
| `POST /orders/{orderId}/payments` | `PENDING_PAYMENT` → `CONFIRMED` on success; a decline leaves `PENDING_PAYMENT` |
| `POST /orders/{orderId}/cancel` | `PENDING_PAYMENT` → `CANCELLED` (any time); `CONFIRMED` → `CANCELLED` (within 48 hours) |
| none | `SHIPPED`, `DELIVERED` (no endpoint in the MVP, tests set them) |

### Conventions (every operation)

- Base path `/api/v1`; server `http://localhost:8080`; JSON bodies; property names camelCase; ids are integers (`int64`).
- **Money**: decimal numbers serialized with exactly 2 places (`425.00`, `0.00`; `BigDecimal.setScale(2)`), VAT exclusive unless named `total`; each top level object that carries money also has `"currency": "PHP"`.
- **Time**: instants are ISO 8601 UTC (`2026-10-07T12:30:00Z`); calendar dates (`estimatedDeliveryDate`, `publishDate`) are `2026-10-12`, already computed in `Asia/Manila`. "Today" and "now" always come from the `Clock` bean (`LocalDate.now(clock.withZone(zone))`), never the system clock, so tests can fix them.
- **Paging** (#8, #11, #13, #21 only): `page` 0 based (default 0, minimum 0), `size` default 12, minimum 1, maximum 50. Controllers take them as `@RequestParam` with `@Min`/`@Max`, not Spring's `Pageable` (which silently clamps). The response is `{ content: [...], page, size, totalElements, totalPages }`. Every sort ends with `id` as a tie breaker so pages are stable. #4, #6, #7 and #10 return plain arrays.
- **Parameter errors**: a range or cross field failure on a query or path parameter (`size` 0, `minPrice > maxPrice`, `q` over 100 characters) → 400 `VALIDATION_FAILED` with `errors` keyed by parameter name; a type or enum failure (`size=abc`, `sort=foo`, `format=AUDIO`) → 400 `MALFORMED_REQUEST`.
- **Check order** when several errors apply: 400 validation, then 404, then 409, then 402 (operation specific orders below).
- **Status codes**: 200 read or edit, 201 create (`Location` only for `POST /orders`, whose target `GET /orders/{id}` exists), 204 empty, 400 bad input or a broken business limit, 401 no or bad token, 402 payment declined, 404 not found or not yours, 409 state or stock conflict, 500 unexpected.
- **Auth**: `bearerAuth` (HTTP bearer, JWT). Public operations declare `security: []`. Every bearer operation lists 401 through a shared `Unauthorized` response component. The token's `sub` claim is the user id; every customer operation uses it and never takes a user id from the request.
- **Errors**: always `application/problem+json` with the `Problem` schema (below), also when the client sends `Accept: application/json` (a test checks there is no bare 406).
- **No idempotency keys**: retries are made safe by the server instead. A repeated `POST /orders` finds the basket empty (409 `CART_EMPTY`) and a repeated payment finds the order already `CONFIRMED` (409 `ORDER_NOT_PAYABLE`). Concurrent duplicates are serialized by locking the user's row (see Key invariants).

### API surface (25 operations; also the README endpoint list)

All paths below are under `/api/v1`. Auth: **public** = no token, **bearer** = valid JWT.

| # | Method | Path | Auth | Journey | Purpose |
|---|---|---|---|---|---|
| 1 | POST | `/auth/register` | public | Home: login page | Create an account and get a token |
| 2 | POST | `/auth/login` | public | Home: user authentication | Log in and get a token |
| 3 | GET | `/me` | bearer | Home, Payment: redeem gift points | Profile and gift points balance |
| 4 | GET | `/me/addresses` | bearer | Payment: select delivery address | Saved addresses, default first |
| 5 | POST | `/me/addresses` | bearer | Payment: select delivery address | Save a new address |
| 6 | GET | `/me/recommendations` | bearer | Home and Cart: recommendations | Books based on order history |
| 7 | GET | `/categories` | public | Catalogue: select category | All 19 categories, sidebar order |
| 8 | GET | `/books` | public | Catalogue: catalogue per category, search, brands | Search and filter books (paged) |
| 9 | GET | `/books/{bookId}` | public | Catalogue: select product | Book detail with delivery date |
| 10 | GET | `/books/{bookId}/related` | public | Catalogue: related products | Related reads |
| 11 | GET | `/authors` | public | Catalogue: browse brands | Authors (paged, by name) |
| 12 | GET | `/authors/{authorId}` | public | Catalogue: browse brands | One author with bio |
| 13 | GET | `/publishers` | public | Catalogue: browse brands | Publishers (paged, by name) |
| 14 | GET | `/publishers/{publisherId}` | public | Catalogue: browse brands | One publisher |
| 15 | GET | `/cart` | bearer | Cart: basket | The basket with totals |
| 16 | POST | `/cart/items` | bearer | Cart: add to basket | Add a book (adds to an existing line) |
| 17 | PUT | `/cart/items/{bookId}` | bearer | Cart: quantity stepper | Set a line's quantity |
| 18 | DELETE | `/cart/items/{bookId}` | bearer | Cart: basket | Remove a line |
| 19 | DELETE | `/cart` | bearer | Cart: basket | Empty the basket |
| 20 | POST | `/orders` | bearer | Payment: address, gift points | Place an order from the basket |
| 21 | GET | `/orders` | bearer | Home: order history | My orders, newest first (paged) |
| 22 | GET | `/orders/{orderId}` | bearer | Home: order history | One of my orders |
| 23 | POST | `/orders/{orderId}/payments` | bearer | Payment: pay, payment and purchase confirmation | Pay (simulated) |
| 24 | POST | `/orders/{orderId}/cancel` | bearer | Payment: cancel within 48 hours | Cancel and refund |
| 25 | POST | `/orders/{orderId}/buy-again` | bearer | Home: Buy It Again | Copy an order's books into the basket |

**Tags** (one per operation): `Auth` #1 to #2 · `Account` #3 to #6 · `Catalogue` #7 to #14 · `Cart` #15 to #19 · `Orders` #20 to #22, #24, #25 · `Payments` #23.

Inputs, outputs and errors per operation (schemas defined in the next table; 401 `UNAUTHORIZED` applies to every bearer operation and 400 `VALIDATION_FAILED` to every operation with a body or bounded parameters, so they are listed only where they matter):

| # | Key inputs | Success | Key errors (status `CODE`) |
|---|---|---|---|
| 1 | `RegisterRequest` | 201 `AuthResponse` | 400 `VALIDATION_FAILED`, 409 `EMAIL_TAKEN` |
| 2 | `LoginRequest` | 200 `AuthResponse` | 400 `VALIDATION_FAILED`, 401 `INVALID_CREDENTIALS` |
| 3 | none | 200 `UserResponse` | 401 `UNAUTHORIZED` |
| 4 | none | 200 `AddressResponse[]` | 401 |
| 5 | `AddressRequest` | 201 `AddressResponse` (no `Location`, there is no GET by address id) | 400, 401 |
| 6 | `size` (1 to 50, default 12) | 200 `BookSummaryResponse[]` | 400, 401 |
| 7 | none | 200 `CategoryResponse[]` | none |
| 8 | `q` (max 100), `category` (slug), `authorId`, `publisherId`, `language`, `format`, `minPrice`, `maxPrice`, `sort`, `page`, `size` | 200 `BookPage` (unknown `authorId` or `publisherId` → empty page) | 400 `VALIDATION_FAILED` / `MALFORMED_REQUEST`, 404 `CATEGORY_NOT_FOUND` |
| 9 | `bookId` | 200 `BookDetailResponse` | 404 `BOOK_NOT_FOUND` |
| 10 | `bookId`, `size` (1 to 20, default 8) | 200 `BookSummaryResponse[]` | 404 `BOOK_NOT_FOUND` |
| 11 | `page`, `size` | 200 `AuthorPage` | 400 |
| 12 | `authorId` | 200 `AuthorResponse` | 404 `AUTHOR_NOT_FOUND` |
| 13 | `page`, `size` | 200 `PublisherPage` | 400 |
| 14 | `publisherId` | 200 `PublisherResponse` | 404 `PUBLISHER_NOT_FOUND` |
| 15 | none | 200 `CartResponse` | 401 |
| 16 | `AddCartItemRequest` | 200 `CartResponse` | 400 `QUANTITY_LIMIT`, 404 `BOOK_NOT_FOUND`, 409 `INSUFFICIENT_STOCK` |
| 17 | `bookId`, `UpdateCartItemRequest` | 200 `CartResponse` (never creates a line) | 400 `QUANTITY_LIMIT`, 404 `CART_ITEM_NOT_FOUND` (book not in the basket, whether or not it exists), 409 `INSUFFICIENT_STOCK` |
| 18 | `bookId` | 200 `CartResponse` | 404 `CART_ITEM_NOT_FOUND` |
| 19 | none | 204 | 401 |
| 20 | `CreateOrderRequest` | 201 `OrderResponse`, `Location` | 400 `VALIDATION_FAILED` / `POINTS_EXCEED_BALANCE` / `POINTS_EXCEED_LIMIT`, 404 `ADDRESS_NOT_FOUND`, 409 `CART_EMPTY` / `INSUFFICIENT_STOCK` |
| 21 | `status` (optional filter, one of the five order statuses), `page`, `size` | 200 `OrderPage` | 400, 401 |
| 22 | `orderId` | 200 `OrderResponse` | 404 `ORDER_NOT_FOUND` |
| 23 | `orderId`, `PaymentRequest` | 201 `PurchaseConfirmationResponse` | 400 `VALIDATION_FAILED`, 402 `PAYMENT_DECLINED`, 404 `ORDER_NOT_FOUND`, 409 `ORDER_NOT_PAYABLE` |
| 24 | `orderId` | 200 `OrderResponse` | 404 `ORDER_NOT_FOUND`, 409 `ORDER_NOT_CANCELLABLE` / `CANCEL_WINDOW_EXPIRED` |
| 25 | `orderId` | 200 `BuyAgainResponse` (also when every line is skipped) | 404 `ORDER_NOT_FOUND` |

Check order per operation, where several errors can apply:
- #16, #17: `VALIDATION_FAILED` → `BOOK_NOT_FOUND` / `CART_ITEM_NOT_FOUND` → `QUANTITY_LIMIT` → `INSUFFICIENT_STOCK`.
- #20: `VALIDATION_FAILED` → `CART_EMPTY` → `ADDRESS_NOT_FOUND` → `POINTS_EXCEED_BALANCE` → `POINTS_EXCEED_LIMIT` → `INSUFFICIENT_STOCK`.
- #23: `VALIDATION_FAILED` → `ORDER_NOT_FOUND` → `ORDER_NOT_PAYABLE` → `PAYMENT_DECLINED`.

### Schemas (names match the Java records in `dto`)

Requests (Bean Validation on the record must match the constraints here):

| Schema | Fields (required unless marked opt) |
|---|---|
| `RegisterRequest` | `email` (email, max 255), `password` (at least 8 characters and at most 72 bytes in UTF-8, the BCrypt input limit; `writeOnly`), `firstName` (1 to 100), `lastName` (1 to 100), `phone` opt (`^\+639\d{9}$`). The record's compact constructor trims and lower cases `email` before validation runs, so `" A@b.com "` is accepted as `a@b.com` |
| `LoginRequest` | `email` (normalized the same way), `password` (`writeOnly`) |
| `AddressRequest` | `firstName`, `lastName` (1 to 100), `email` (email), `phone` (`^\+639\d{9}$`), `streetAddress` (1 to 255), `barangay` opt (max 100), `city`, `province` (1 to 100), `zipCode` (`^\d{4}$`), `country` opt (max 60, default `Philippines`), `isDefault` opt (default false) |
| `AddCartItemRequest` | `bookId`, `quantity` opt (1 to 10, default 1) |
| `UpdateCartItemRequest` | `quantity` (1 to 10) |
| `CreateOrderRequest` | exactly one of `addressId` or `shippingAddress` (`AddressRequest`); neither or both → `VALIDATION_FAILED` with `errors["addressId"]` (a class level constraint). `saveAddress` opt (default false; ignored with `addressId`), `pointsToRedeem` opt (0 to 2147483647, default 0; a number that does not fit an int → `MALFORMED_REQUEST`) |
| `PaymentRequest` | `method` (`CREDIT_CARD`, `DEBIT_CARD`, `E_WALLET`), required. In the file every other field is optional, with its rule in `description`, because the rules depend on `method` (OpenAPI 3.0.3 and plain Bean Validation cannot express that cleanly). Card methods need `cardNumber` (digits only, no spaces or dashes, 13 to 19, Luhn valid), `cardHolderName` (1 to 100), `cvv` (`^\d{3,4}$`), `expiry` (`^(0[1-9]\|1[0-2])/\d{4}$`, not before the current month in `app.zone` per the `Clock`). `E_WALLET` needs `walletMobileNumber` (`^\+639\d{9}$`). Fields for the other method are ignored. The service checks these and throws a validation exception that the handler maps to `VALIDATION_FAILED` with `errors` keyed by field. `cardNumber`, `cvv`, `expiry`, `walletMobileNumber` are `writeOnly`, masked in `toString` and never logged |

Responses:

| Schema | Fields |
|---|---|
| `AuthResponse` | `accessToken`, `tokenType` (`Bearer`), `expiresIn` (seconds, 3600), `user` (`UserResponse`) |
| `UserResponse` | `id`, `email`, `firstName`, `lastName`, `phone` (nullable), `giftPointsBalance` |
| `AddressResponse` | `id`, all `AddressRequest` fields, `isDefault`, `createdAt` |
| `CategoryResponse` | `id`, `name`, `slug` |
| `AuthorSummary` / `AuthorResponse` | `id`, `name` / plus `photoUrl` (nullable), `bio` (nullable) |
| `PublisherSummary` / `PublisherResponse` | `id`, `name` / plus `description` (nullable) |
| `BookSummaryResponse` (book card) | `id`, `title`, `shortDescription`, `format`, `language`, `price`, `currency`, `frontCoverUrl`, `author` (`AuthorSummary`), `publisher` (`PublisherSummary`), `genres` (string list, by name), `inStock`, `estimatedDeliveryDate` |
| `BookDetailResponse` (product page) | all summary fields except `shortDescription`, plus `description`, `backCoverUrl`, `author` (`AuthorResponse`), `publisher` (`PublisherResponse`), `category` (`CategoryResponse`), `stockQuantity` (nullable: null for `EBOOK`), `copiesSold`, `publishDate`, `otherFormats` (list of `OtherFormat`) |
| `OtherFormat` | `id`, `format`, `price` |
| `CartResponse` | `items` (list of `CartItemResponse`), `itemCount`, `subtotal`, `vatAmount`, `deliveryCharge`, `estimatedTotal`, `currency` |
| `CartItemResponse` | `bookId`, `title`, `format`, `authorName`, `frontCoverUrl`, `unitPrice`, `quantity`, `maxQuantity`, `lineTotal`, `inStock`, `estimatedDeliveryDate` |
| `OrderResponse` | `id`, `orderNumber`, `status`, `items` (list of `OrderItemResponse`), `shippingAddress` (`ShippingAddress`), `subtotal`, `vatRate`, `vatAmount`, `deliveryCharge`, `giftPointsRedeemed`, `giftPointsAmount`, `totalAmount`, `giftPointsEarned`, `currency`, `estimatedDeliveryDate`, `placedAt`, `paidAt` (nullable), `cancelledAt` (nullable), `canCancel`, `cancelDeadline` (nullable), `payment` (`PaymentResponse`, nullable) |
| `OrderItemResponse` | `bookId`, `title`, `format`, `authorName`, `frontCoverUrl`, `unitPrice`, `quantity`, `lineTotal` |
| `ShippingAddress` | the `AddressRequest` address fields, without `isDefault` |
| `PaymentResponse` | `transactionId`, `method`, `status`, `amount`, `currency`, `cardLast4` (nullable), `failureReason` (nullable), `createdAt`, `refundedAt` (nullable) |
| `PurchaseConfirmationResponse` | `payment` (`PaymentResponse`), `order` (`OrderResponse`, now `CONFIRMED`, items are the purchased books) |
| `BuyAgainResponse` | `cart` (`CartResponse`), `added` (list of `{ bookId, title, quantity }`), `skipped` (list of `{ bookId, title, reason }`, reason `OUT_OF_STOCK` or `LIMIT_REACHED`) |
| `BookPage`, `OrderPage`, `AuthorPage`, `PublisherPage` | `content`, `page`, `size`, `totalElements`, `totalPages` (Java: one generic `PageResponse<T>` record) |
| `Problem` | `type` (default `about:blank`), `title`, `status`, `detail`, `instance`, `code` (enum below), `errors` (object, field or parameter name to its first message, validation only), `bookIds` (`INSUFFICIENT_STOCK` only), `transactionId` (`PAYMENT_DECLINED` only) |

**Error codes** (`Problem.code`, mirrored by `ApiErrorCode` in `exception`). Each `ApiErrorCode` value carries its HTTP status and a fixed `title`; the handler sets `status` and `title` from it, `detail` from the exception message, and `instance` to the request path. Every app exception carries an `ApiErrorCode`. A `DataIntegrityViolationException` naming `uq_users_email` maps to `EMAIL_TAKEN`, one naming `uq_cart_items_user_book` is handled inside the cart service (below), and any other maps to `INTERNAL_ERROR`.

| Code | Status | When |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Bean Validation failed on a body or a parameter, or a service side field check failed; `errors` lists each field or parameter |
| `MALFORMED_REQUEST` | 400 | Unreadable JSON, wrong parameter type, missing required parameter, unknown enum value, a number too large for its type |
| `QUANTITY_LIMIT` | 400 | The resulting line quantity would exceed the line limit: above 1 for an eBook (including adding an eBook already in the basket), or existing plus added above 10 for a print book. A single request quantity above 10 is already `VALIDATION_FAILED` |
| `POINTS_EXCEED_BALANCE` | 400 | `pointsToRedeem` above the user's balance |
| `POINTS_EXCEED_LIMIT` | 400 | `pointsToRedeem` above `floor(subtotal + VAT + delivery) − 1` |
| `UNAUTHORIZED` | 401 | Missing, expired or invalid token |
| `INVALID_CREDENTIALS` | 401 | Wrong email or password (same message for both) |
| `PAYMENT_DECLINED` | 402 | Simulated decline (card ending `0002`); carries `transactionId` |
| `BOOK_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `AUTHOR_NOT_FOUND`, `PUBLISHER_NOT_FOUND`, `ADDRESS_NOT_FOUND`, `ORDER_NOT_FOUND`, `CART_ITEM_NOT_FOUND` | 404 | The thing does not exist, or belongs to another user |
| `NOT_FOUND` | 404 | No such route, for an authenticated caller (an anonymous caller on an unknown non public path gets 401 first, from security) |
| `METHOD_NOT_ALLOWED` | 405 | Wrong HTTP method on a known path |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Body is not JSON |
| `EMAIL_TAKEN` | 409 | Register with an email already used |
| `INSUFFICIENT_STOCK` | 409 | Not enough stock (basket or checkout); carries `bookIds` |
| `CART_EMPTY` | 409 | Checkout with an empty basket |
| `ORDER_NOT_PAYABLE` | 409 | Pay an order that is not `PENDING_PAYMENT` |
| `ORDER_NOT_CANCELLABLE` | 409 | Cancel an order that is `SHIPPED`, `DELIVERED` or `CANCELLED` |
| `CANCEL_WINDOW_EXPIRED` | 409 | Cancel a `CONFIRMED` order more than 48 hours after `placedAt` |
| `INTERNAL_ERROR` | 500 | Anything unexpected (no stack trace in the body; logged on the server) |

**Value sourcing** (values this contract adds on top of spec 0002's table, which still governs totals, points, order numbers, delivery dates and payment outcomes):

| Action | Value produced / displayed | Source |
|---|---|---|
| Any customer operation | the acting user | JWT `sub` claim = `users.id` (feature 5 issues it); never a request field |
| Register, login | `accessToken`, `expiresIn` | feature 5 per spec 0001 (HS256, 60 minutes, so `expiresIn` = 3600) |
| Register, login | email match | request `email` trimmed and lower cased (spec 0002) |
| Book card | `shortDescription` | `books.description` unchanged when 150 characters or fewer; otherwise cut at the last space within the first 150 characters (a hard cut at 150 when there is none), plus `...` |
| Book card, detail, cart | `inStock`, `estimatedDeliveryDate` | spec 0002 value sourcing |
| Book card, detail | `genres` | `book_genres` → `genres.name`, sorted by name |
| Book detail | `otherFormats` | other `books` rows with the same `title`, sorted by format |
| `GET /books` | `q` match | spec 0002: title, author name, publisher name, case insensitive substring; `%`, `_` and `\` in `q` are escaped for `LIKE`; a blank `q` counts as absent |
| `GET /books` | `category` filter | `categories.slug`; unknown slug → 404 `CATEGORY_NOT_FOUND` |
| `GET /books` | `authorId`, `publisherId` filters | `books.author_id`, `books.publisher_id`; an unknown id simply matches nothing (empty page) |
| `GET /books` | `language`, `format`, price range | `books.language` (case insensitive equals), `books.format` (`PAPERBACK`, `HARDCOVER`, `EBOOK`), `books.price` between `minPrice` and `maxPrice` inclusive (VAT exclusive, each 0 or more); `minPrice > maxPrice` → 400 `VALIDATION_FAILED` |
| `GET /books` | order | `sort`: `relevance` (`lower(title)`, default; same on H2 and PostgreSQL), `price_asc`, `price_desc`, `newest` (`publish_date` desc), `bestselling` (`copies_sold` desc), then `id`. This refines spec 0002's single `price` key into two directions |
| `GET /books/{id}/related` | which books, in what order | books sharing the author or at least one genre, excluding every row with the same title; order: same author first, then shared genre count desc (the number of the candidate's distinct genres that the book also has), then `copies_sold` desc, then `id` |
| `GET /authors`, `/publishers` | order | `name` ascending, then `id` |
| `GET /categories` | order | `categories.id` (seed order = sidebar order) |
| `GET /me/recommendations` | which books, in what order | candidates share an author or genre with books in the user's "bought" orders (spec 0002: `CONFIRMED`, `SHIPPED`, `DELIVERED`), excluding any title the user bought (any format); score = 2 × (author matches a bought book's author) + shared genre count (the candidate's distinct genres that appear on any bought book), then `copies_sold` desc, then `id`; topped up to `size` with the newest unbought books, so a new user gets the newest books. Books already in the basket are not excluded and stock does not filter (`inStock` shows it) |
| `GET /me/addresses` | order | `is_default` first, then `created_at` desc |
| Cart | line order | `cart_items.created_at`, then `id` |
| Cart | `maxQuantity` | `EBOOK` → 1; print → min(10, `stock_quantity`) (0 when out of stock) |
| Cart | a line whose stock dropped below its quantity | kept as is, with `inStock` and `maxQuantity` showing the problem; totals include it; checkout then fails with `INSUFFICIENT_STOCK` and its `bookIds` |
| Cart | `itemCount` | sum of line quantities |
| Cart | `subtotal`, `vatAmount`, `deliveryCharge`, `estimatedTotal` | same rules as order creation in spec 0002, with no points (a preview; the order recomputes them); `estimatedTotal = subtotal + vatAmount + deliveryCharge`; an empty basket shows all four as `0.00` |
| Cart add, set quantity | line limit check | resulting quantity above 1 (eBook) or 10 (print) → 400 `QUANTITY_LIMIT`; above `stock_quantity` for print → 409 `INSUFFICIENT_STOCK` |
| Buy again | added quantity | per order line, in `order_items.id` order: `add = min(ordered quantity, min(line limit, stock for print) − quantity already in the basket)`; `add ≤ 0` → `skipped` with `OUT_OF_STOCK` when a print book has stock 0, else `LIMIT_REACHED`; a reduced add appears in `added` with the reduced quantity; uses the same `book_id`, so the same format |
| Buy again | which orders | any order the user owns, any status |
| Order | `items` order | `order_items.id` |
| Order | `items[].authorName`, `frontCoverUrl` | current `books` row via `order_items.book_id` (title, format and prices stay the snapshot) |
| Order | `giftPointsEarned` | the stored column: 0 until payment, then spec 0002's rule; a cancelled paid order keeps the stored value (the ledger shows the reversal). When the rule gives 0 points, no `EARNED` ledger row is written (its CHECK needs points above 0) |
| Order | `canCancel`, `cancelDeadline` | `PENDING_PAYMENT` → true, deadline null; `CONFIRMED` → true while `now(clock)` ≤ `placed_at` + `app.cancel-window-hours` (inclusive), deadline = that instant; other statuses → false, null |
| Order | `payment` | the order's `SUCCESS` or `REFUNDED` payment if one exists, else its latest `FAILED` one (`created_at` desc, then `id` desc), else null |
| Pay | `payment.amount` | `orders.total_amount` |
| Cancel | which error on failure | the guarded update includes the window: `UPDATE orders ... WHERE id = :id AND (status = 'PENDING_PAYMENT' OR (status = 'CONFIRMED' AND placed_at >= :cutoff))`; zero rows → read the status again: `CONFIRMED` → `CANCEL_WINDOW_EXPIRED`, `SHIPPED`, `DELIVERED` or `CANCELLED` → `ORDER_NOT_CANCELLABLE` |
| `GET /orders` | order, filter | `placed_at` desc, then `id` desc; optional `status` equals filter |
| Pay | expiry check | current month in `app.zone` |
| Create order | `Location` header | `/api/v1/orders/{id}` |
| Every error | `code` | the `ApiErrorCode` the service throws or the handler maps |
| Every money response | `currency` | `app.currency` (spec 0002) |

**Key invariants**:
- `openapi.yaml` is the single source of truth. A controller method that is not in the file fails the build (AC-5); a change to the API is a change to the file first.
- No operation takes a user id from the path, query or body; ownership always comes from the token.
- Totals in any response are computed on the server. No request carries a price, subtotal or total.
- No response ever contains a full card number, CVV, expiry or wallet number (AC-8), and the request DTO's `toString` masks them.
- Error bodies are always `Problem` with a `code`; controllers never build error bodies (`AGENTS.md`).
- **Per user serialization**: `POST /orders`, `POST /cart/items` and `POST /orders/{id}/buy-again` first lock the caller's `users` row (`SELECT ... FOR UPDATE`, inside the transaction). Two concurrent checkouts then run one after the other, so the second finds the basket empty, and the points balance cannot be double spent. If a concurrent add still hits `uq_cart_items_user_book`, the cart service retries once as an update in a new transaction.
- **A decline is kept**: the `FAILED` payment row is written in its own transaction (`REQUIRES_NEW`), then the service throws the decline exception, so the 402 response never rolls back the record of the attempt.

**Security model**: catalogue reads (#7 to #14), `/auth/*`, `/openapi.yaml`, `/swagger-ui.html`, `/swagger-ui/**` and `/v3/api-docs/**` (Swagger UI loads its config from there) are public. Everything else needs a valid bearer token. On public paths a token is ignored, even a stale one (feature 5: a `BearerTokenResolver` that returns null there), so an old token never turns a catalogue read into a 401. There are no roles in the MVP API (`ADMIN` exists in the schema but no operation uses it, so there is no 403). A resource owned by another user returns 404 with the same `*_NOT_FOUND` code as a missing one, so ids cannot be probed. Card data follows spec 0002 (last 4 digits only), and payment request fields are `writeOnly`. No rate limiting and no CORS configuration (local run, no browser front end); see Consequences.

**Configuration required**:
- No new environment variables.
- New application properties: `springdoc.swagger-ui.url=/openapi.yaml`, `springdoc.swagger-ui.path=/swagger-ui.html`. Leave `springdoc.api-docs.enabled` at its default (`true`): Swagger UI reads its config from `/v3/api-docs/swagger-config`, and turning api docs off would likely break it. The generated `/v3/api-docs` is an unused second description; nothing links to it, and the hand written file stays the contract.
- Unknown paths: Spring Boot 3.5 raises `NoResourceFoundException` for them; the handler maps it to `NOT_FOUND` (no extra property needed).
- New dependencies: `org.springdoc:springdoc-openapi-starter-webmvc-ui` (newest 2.8.x patch, the Boot 3.5 line from spec 0001) and `io.swagger.parser.v3:swagger-parser` (test scope, newest 2.1.x).

**Critical test scenarios**:
- Happy path: `OpenApiContractTest` parses the file with zero messages and finds 25 operations, unique ids, one tag each, explicit security, paged list parameters and page schemas, and no sensitive response fields, verifies **AC-1**, **AC-2**, **AC-6**, **AC-8**
- Failure case: a temporary controller method `GET /api/v1/ping` added in a test fails `ContractDriftTest` (run once by hand to prove the test bites, then removed), verifies **AC-5**
- Error format: `ApiErrorCodeContractTest` compares the enum with `Problem.code`; a `@WebMvcTest` on a stub controller checks a body validation error, a bad `size` parameter (`VALIDATION_FAILED` keyed `size`), `size=abc` (`MALFORMED_REQUEST`) and a request with `Accept: application/json` (a `Problem` body, not a bare 406), verifies **AC-4**
- Swagger UI: a `@SpringBootTest` GETs `/openapi.yaml` (200, contains `openapi: 3.0.3`) and `/swagger-ui.html` (200 after redirects); then by hand, `mvn spring-boot:run` shows the 25 operations in 6 tags, verifies **AC-3**

## Build plan

Journey approach: the contract is a foundation the three journeys share, so it is written whole now; each journey feature later builds only its own controllers against it, and the drift test keeps them honest.

1. Write `src/main/resources/openapi.yaml` (OpenAPI 3.0.3): `info`, the server, 6 tags (`Auth`, `Account`, `Catalogue`, `Cart`, `Orders`, plus `Payments` for #23), the `bearerAuth` scheme, shared parameters (`page`, `size`, path ids), every schema and the error responses above, and all 25 operations, satisfies **AC-2**, **AC-4**, **AC-6**, **AC-7**, **AC-8**
2. Add `swagger-parser` (test) and `OpenApiContractTest`: parse with zero messages, then check the count, ids, tags, explicit security, paging parameters, page schema shape, `writeOnly` sensitive fields and no sensitive response fields, satisfies **AC-1**, **AC-2**, **AC-6**, **AC-8**
3. Add `exception/ApiErrorCode` (the enum above, each value with its status and title); give every app exception a code (`ResourceNotFoundException` takes one); make `GlobalExceptionHandler` set `code`, `title` and `instance` on every body, validation included; map parameter validation (`HandlerMethodValidationException`) to `VALIDATION_FAILED` keyed by parameter, type mismatches and unreadable bodies to `MALFORMED_REQUEST`, `NoResourceFoundException` to `NOT_FOUND`, the 405 and 415 cases to theirs, `DataIntegrityViolationException` by constraint name (above), and anything else to `INTERNAL_ERROR`; add `ApiErrorCodeContractTest` and the `@WebMvcTest` error body tests, satisfies **AC-4**
4. Add `ContractDriftTest` as a `@SpringBootTest`: read every handler from `RequestMappingHandlerMapping`, keep those under `/api/v1`, expand mappings with several methods, fail on a mapping with no method, normalize every `{...}` path variable to `{}` on both sides (the code may say `{id}` where the file says `{bookId}`), and fail on any (method, path) missing from the file, satisfies **AC-5**
5. Add the springdoc dependency, `config/OpenApiConfig` (a resource handler that serves `classpath:openapi.yaml` at `/openapi.yaml` and nothing else from the classpath root) and the springdoc properties; add the `@SpringBootTest` for both URLs, then run the app and open `/swagger-ui.html`, satisfies **AC-3**
6. Add the AI usage log entry; the endpoint table above is the README source for feature 14, satisfies **AC-7**

## Consequences

**Positive**:
- Every journey feature starts with its URLs, JSON shapes and error codes already decided, so the build is mostly wiring.
- Reviewers can read and try the API in Swagger UI and import the same file into Insomnia.
- The drift and contract tests make "the code matches the spec" a build fact, not a promise.

**Negative / tradeoffs**:
- Hand writing 25 operations is slow and the file is long; a typo in a schema is caught only by the parser and by review, not by the compiler.
- The drift test checks paths and methods, not JSON field shapes; a DTO can still drift from its schema until `/check verify` or a test catches it.
- No rate limiting on public endpoints (login can be brute forced) and no idempotency keys; retries rely on the user row lock and the state guards instead. Acceptable for a local capstone, not for production.
- Several rules (payment fields per method, check order, buy again quantities) live in service code and in `description` text, not in machine checked schema; `/check verify` has to exercise them.
- Anonymous callers get 401, not 404, on unknown non public paths; a client cannot tell "no such route" from "log in first" without a token.
- A declined payment returns 402 but still writes a `FAILED` payment row, so clients must treat 402 as "recorded, try again".
- Feature 5 is now bound to `sub` = user id, the `AuthResponse` shape and the public path list above.

**Neutral**:
- `SHIPPED` and `DELIVERED` have no endpoint; the contract can add admin operations later under a new tag.
- Wishlist, reviews, coupons and bestseller sections are not in the contract; adding them later is additive (new paths, no version bump).

## Follow-up

- [ ] Review the auto picked decisions in [rationale.md](rationale.md) (decision log); every pick was the recommended option under your "pick recommended" instruction.
- [ ] Feature 5 (authentication): issue tokens with `sub` = user id, return `AuthResponse`, keep the public path list above, ignore tokens on public paths (`BearerTokenResolver`), and return `Problem` with `code: UNAUTHORIZED` from the authentication entry point.
- [ ] Features 8 to 10 inherit the Key invariants above (user row lock, the cart insert retry, the decline row in its own transaction); their `/develop` runs should read this spec alongside spec 0002.
- [ ] Scope feature 10 still says "UPI or wallet"; spec 0002 and this contract use `CREDIT_CARD`, `DEBIT_CARD`, `E_WALLET`. Worth correcting with `/scope` or `/sync`.
- [ ] Feature 14 (run kit): copy the API surface table into the README, and build the Insomnia collection by importing `openapi.yaml`. Test cards: `4242424242424242` succeeds, `4000000000000002` is declined.
- [ ] Agent Skills and MCP servers for springdoc and swagger-parser were not searched (the search runs a third party npm package, which needs your explicit go ahead). Run the search later if you want one.
- [ ] Optional later: a reverse drift check (every operation in the file has a handler) once all journeys are built, for example in feature 14.
