# 0005. Checkout and order

**Date**: 2026-10-14
**Status**: Accepted

## Summary

This spec covers journey steps 9 and 10: the logged-in customer places an order from their basket by choosing a delivery address, optionally redeeming gift points, and submitting. The server computes every total, checks and decrements stock for print books, deducts redeemed points from the user's balance, records one ledger row for the redemption, clears the basket, and returns an `OrderResponse` with a `Location` header. It also covers the three account endpoints that checkout depends on: list saved addresses, save a new address, and get the current user profile (including gift points balance). All of these are already declared in `openapi.yaml` (spec 0003, operations #3, #4, #5, #20); this spec is the build contract for the service and controller logic behind them.

## Requirements

**User stories**:
- As a customer, I want to see my saved addresses and pick one at checkout so that I do not have to retype my address every time.
- As a customer, I want to redeem my gift points at checkout so that my total is reduced by the points value.
- As a customer, I want the server to check stock before confirming my order so that I am never charged for a book that is out of stock.
- As a customer, I want my basket emptied after a successful order so that I start fresh for the next purchase.

**Acceptance criteria**:
- **AC-1**: `POST /api/v1/orders` with a valid basket, a valid address and `pointsToRedeem = 0` returns 201 `OrderResponse` with a `Location` header, correct server-computed totals (`subtotal`, `vatAmount`, `deliveryCharge`, `giftPointsAmount = 0`, `totalAmount`), a shipping address snapshot, status `PENDING_PAYMENT`, and an `orderNumber` matching `BW-yyyyMMdd-NNNNNN`. Stock is decremented for every print book line. The basket is empty after the call.
- **AC-2**: `POST /api/v1/orders` with `pointsToRedeem > 0` (and within the user's balance and the order limit) reduces `totalAmount` by `pointsToRedeem × app.points.peso-value`, writes one `REDEEMED` ledger row, and reduces `users.gift_points_balance` by the same amount. The response carries correct `giftPointsRedeemed` and `giftPointsAmount`.
- **AC-3**: `POST /api/v1/orders` with `pointsToRedeem` above the user's balance returns 400 `POINTS_EXCEED_BALANCE`; with `pointsToRedeem` above `floor(subtotal + vatAmount + deliveryCharge) - 1` returns 400 `POINTS_EXCEED_LIMIT`. The error check order follows spec 0003: `VALIDATION_FAILED` → `CART_EMPTY` → `ADDRESS_NOT_FOUND` → `POINTS_EXCEED_BALANCE` → `POINTS_EXCEED_LIMIT` → `INSUFFICIENT_STOCK`. Nothing is changed in the database on any error.
- **AC-4**: `POST /api/v1/orders` when one or more print books in the basket have insufficient stock returns 409 `INSUFFICIENT_STOCK` with `bookIds` listing every failing book id (all checked before throwing). Nothing is changed in the database.
- **AC-5**: `POST /api/v1/orders` with an empty basket returns 409 `CART_EMPTY`. With an `addressId` that does not belong to the caller returns 404 `ADDRESS_NOT_FOUND`. With neither `addressId` nor `shippingAddress`, or with both, returns 400 `VALIDATION_FAILED` with `errors["addressId"]`.
- **AC-6**: `POST /api/v1/orders` with an inline `shippingAddress` and `saveAddress = true` persists that address for the user (setting `is_default = true` if it is the user's first saved address), within the same transaction as the order creation.
- **AC-7**: `GET /api/v1/me/addresses` returns the caller's saved addresses with the default first, then by `created_at` descending.
- **AC-8**: `POST /api/v1/me/addresses` saves a new address and returns 201 `AddressResponse`. If it is the first address for that user, `is_default` is set to `true` regardless of the request flag. If `isDefault = true` and the user already has other addresses, those are updated to `is_default = false` in the same transaction.
- **AC-9**: `GET /api/v1/me` returns the caller's profile including the current `giftPointsBalance`.
- **AC-10**: The user's row is locked (`SELECT ... FOR UPDATE`) at the start of `POST /api/v1/orders` so that two concurrent checkouts run serially and the points balance cannot be double spent.

## Decision

**Chosen option**: Single transaction for order creation with inline address save.

All of order creation — user row lock, basket load, stock checks, address resolution, points validation and deduction, stock decrement (per line, ascending `book_id` order), ledger write, order and order-item insert, basket clear — runs in one `@Transactional` method on `OrderService`. The `saveAddress` side effect is inside the same transaction: on rollback the address is not saved either, which is the correct behaviour (no dangling addresses from failed orders).

**Implementation skills**: `java-springboot` (`.claude/skills/java-springboot/`)

## Rationale

Reasoning, options and the full decision log: see [rationale.md](rationale.md).

## Feature design

**Data model sketch**: no new tables. This feature writes to `orders`, `order_items`, `addresses` (when `saveAddress = true`), `cart_items` (delete), `books` (stock decrement, guarded), `users` (points balance, guarded), and `gift_point_transactions` (one `REDEEMED` row when points are used).

**State transitions**: order creation always produces `PENDING_PAYMENT`. No other transitions in this feature (payment and cancel are features 10 and 11).

**API surface**: operations #3, #4, #5 and #20 from spec 0003. No new operations; all paths and schemas are already in `openapi.yaml`.

| # | Method | Path | This feature builds |
|---|---|---|---|
| 3 | GET | `/api/v1/me` | `AccountController.getCurrentUser` |
| 4 | GET | `/api/v1/me/addresses` | `AccountController.getAddresses` |
| 5 | POST | `/api/v1/me/addresses` | `AccountController.createAddress` |
| 20 | POST | `/api/v1/orders` | `OrderController.createOrder` |
| 21 | GET | `/api/v1/orders` | stub only (returns empty page); full build in feature 12 |
| 22 | GET | `/api/v1/orders/{orderId}` | `OrderController.getOrderById` |

Operations #21 (`GET /orders`), #23 (payment), #24 (cancel) and #25 (buy again) are declared in the contract; stubs that return the correct error or an empty page satisfy the drift test without implementing the full logic.

**Value sourcing** (all values this feature produces; spec 0002 governs totals and ledger math):

| Action | Value | Source |
|---|---|---|
| `POST /orders` | acting user | JWT `sub` claim (never a request field) |
| `POST /orders` | user row lock | `SELECT ... FOR UPDATE` on `users` via `UserRepository.findByIdForUpdate` |
| `POST /orders` | `subtotal` | `Σ books.price × cart_items.quantity`; prices copied to `order_items.unit_price` |
| `POST /orders` | `vat_rate` | `app.vat-rate` (0.12) |
| `POST /orders` | `vat_amount` | `round(subtotal × vat_rate, 2, HALF_UP)` |
| `POST /orders` | `delivery_charge` | `app.delivery-charge` (0.00) |
| `POST /orders` | `gift_points_amount` | `pointsToRedeem × app.points.peso-value` (₱1 per point) |
| `POST /orders` | `total_amount` | `subtotal + vat_amount + delivery_charge − gift_points_amount` |
| `POST /orders` | `estimated_delivery_date` | latest date over lines: EBOOK → today; print → today + `app.delivery-days` (5); "today" from `Clock` bean in `app.zone` |
| `POST /orders` | `order_number` | `BW-` + order date `yyyyMMdd` in `app.zone` + `-` + `nextval('order_number_seq')` padded to 6 digits |
| `POST /orders` | `placed_at`, `updated_at` | `OffsetDateTime.now(clock)`, UTC |
| `POST /orders` | shipping snapshot | from `addresses` row (by `addressId`) or from inline `shippingAddress`; 13 `ship_*` columns on `orders` |
| `POST /orders` | `saveAddress = true` | persist inline address; `is_default = true` if first address for user, else honour request `isDefault` flag (clearing others if true); same transaction |
| `POST /orders` | stock decrement | `UPDATE books SET stock_quantity = stock_quantity - :q WHERE id = :id AND stock_quantity >= :q`; 0 rows → 409; lines in ascending `book_id` order; eBooks skipped |
| `POST /orders` | points deduction | `UPDATE users SET gift_points_balance = gift_points_balance - :pts WHERE id = :id AND gift_points_balance >= :pts`; + one `REDEEMED` ledger row; skipped when `pointsToRedeem = 0` |
| `POST /orders` | basket clear | `DELETE FROM cart_items WHERE user_id = :id` after all items are validated and stock decremented |
| `POST /orders` | `INSUFFICIENT_STOCK bookIds` | all failing book ids collected before throwing (one pass through all lines) |
| `POST /orders` | `gift_points_earned` | 0 at order creation; set on payment (feature 10) |
| `POST /orders` | `canCancel` | `PENDING_PAYMENT` → true |
| `POST /orders` | `cancelDeadline` | `PENDING_PAYMENT` → null |
| `POST /orders` | `payment` | null (no payment yet) |
| `POST /orders` | `Location` header | `/api/v1/orders/{id}` |
| `GET /me/addresses` | order | `is_default` first, then `created_at` desc |
| `POST /me/addresses` | `is_default` | true if first address for user; otherwise honour request flag; if true, clear others in same transaction |
| `POST /me/addresses` | `created_at` | `OffsetDateTime.now(clock)` |
| `GET /me` | `giftPointsBalance` | `users.gift_points_balance` |

**Key invariants**:
- `orders.total_amount = subtotal + vat_amount + delivery_charge − gift_points_amount` (database CHECK enforces; service computes the same way).
- `total_amount >= 1` (service enforces via the `POINTS_EXCEED_LIMIT` guard: `pointsToRedeem ≤ floor(subtotal + vat + delivery) - 1`).
- `users.gift_points_balance` always equals the running sum of that user's `gift_point_transactions.points`; the deduction and the ledger write are in the same transaction.
- Stock is decremented in ascending `book_id` order in all operations to prevent deadlocks when two orders share books.
- The user row lock (`SELECT ... FOR UPDATE`) serialises concurrent checkouts so the balance cannot be double-spent.
- The basket is cleared only after all stock decrements succeed. On any error the transaction rolls back and stock is untouched.
- An `addressId` that exists but belongs to another user returns 404 `ADDRESS_NOT_FOUND` (same code as missing, so ids cannot be probed).

**Security model**: the acting user comes exclusively from the JWT `sub` claim. No user id is accepted from the request body or path. Addresses are filtered by the caller's user id before resolving `addressId`. Order writes are inside the user row lock.

**Configuration required**: no new properties. Uses `app.vat-rate`, `app.delivery-charge`, `app.delivery-days`, `app.points.peso-value`, `app.zone`, `app.currency`, `app.cancel-window-hours` (all from spec 0002).

**Critical test scenarios**:
- Happy path (points = 0): `POST /orders` with a basket of two print books and one eBook, valid `addressId`, returns 201 with correct `subtotal`, `vatAmount`, `totalAmount`, empty basket after, stock decremented; satisfies **AC-1**.
- Happy path (points > 0): same call with `pointsToRedeem = 50` reduces `totalAmount` by ₱50, ledger row written, `giftPointsBalance` reduced; satisfies **AC-2**.
- Points validation: `pointsToRedeem` above balance → 400 `POINTS_EXCEED_BALANCE`; `pointsToRedeem` = total floor (leaving ₱0) → 400 `POINTS_EXCEED_LIMIT`; satisfies **AC-3**.
- Insufficient stock: two books fail stock check → 409 `INSUFFICIENT_STOCK` with both ids in `bookIds`, nothing changed; satisfies **AC-4**.
- Error cases: empty basket → 409 `CART_EMPTY`; wrong `addressId` → 404; both address fields → 400 `VALIDATION_FAILED` with `errors["addressId"]`; satisfies **AC-5**.
- Save address at checkout: `saveAddress = true`, user has no prior addresses → address saved with `is_default = true`; satisfies **AC-6**.
- Address endpoints: list returns default first; create sets default when first; create with `isDefault = true` clears others; satisfies **AC-7**, **AC-8**.

## Build plan

1. Add DTOs: `CreateOrderRequest` (class-level `@AddressSource` constraint), `AddressRequest`, `AddressResponse`, `OrderResponse`, `OrderItemResponse`, `ShippingAddress`, `PaymentResponse` (nullable fields for the null-payment case). satisfies **AC-1**, **AC-2**, **AC-5**
2. Add `AddressMapper` and `OrderMapper` (hand-written, no MapStruct). satisfies **AC-1**, **AC-7**
3. Add `AccountController` (`GET /api/v1/me`, `GET /api/v1/me/addresses`, `POST /api/v1/me/addresses`) and `AccountService` using `UserRepository`, `AddressRepository`, and `Clock`; address save sets `is_default` correctly and clears others in a single transaction. satisfies **AC-7**, **AC-8**, **AC-9**
4. Add `OrderController` (`POST /api/v1/orders`, `GET /api/v1/orders`, `GET /api/v1/orders/{orderId}`) with stubs for `GET /orders` (empty `OrderPage`) and the full `createOrder` and `getOrderById` delegating to `OrderService`. satisfies **AC-1** (controller layer)
5. Add `OrderService.createOrder`: lock user row, load cart items (fail if empty), resolve address (`addressId` or inline), validate points, collect all stock failures and throw if any, compute totals, persist order + items, decrement stock per line ascending book_id, deduct points + write ledger if `pointsToRedeem > 0`, clear basket, optionally save address. satisfies **AC-1**, **AC-2**, **AC-3**, **AC-4**, **AC-5**, **AC-6**, **AC-10**
6. Add `OrderService.getOrderById`: load order filtered by user id (404 if missing or wrong owner), map to `OrderResponse` with `canCancel`, `cancelDeadline` and `payment` derived per spec 0003 value sourcing. satisfies **AC-1** (read back)
7. Run `mvn clean install`; fix any drift-test failures from the new handlers. All existing tests must stay green.

## Consequences

**Positive**:
- The full checkout flow is working end to end, including address management and gift point redemption, which enables the simulated payment feature (10) to build directly on top.
- All stock and points guards are in the service, backed by guarded database updates, so concurrent checkouts cannot oversell or double-spend.
- Keeping the inline address save inside the order transaction means a failed order never leaves orphan addresses.

**Negative / tradeoffs**:
- The `OrderService.createOrder` method is long by necessity (lock, validate, compute, persist, side-effects); splitting it further would scatter the atomicity guarantee across transaction boundaries.
- Stock is held by unpaid orders; a user who places but never pays holds stock until they cancel (no expiry job in scope).
- `GET /orders` is a stub in this feature; order history is a separate journey (feature 12).

**Neutral**:
- `giftPointsEarned` is stored as 0 on creation; feature 10 sets it on payment.
- The `payment` field in `OrderResponse` is null here; feature 10 populates it.

## Follow-up

- [ ] Feature 10 (simulated payment): reads the `OrderResponse` shape from this spec; sets `paidAt`, `giftPointsEarned`, writes the `payments` row and the `EARNED` ledger row.
- [ ] Feature 11 (cancel): uses `OrderService` patterns established here for guarded updates and ledger writes.
- [ ] Feature 12 (order history): replaces the `GET /orders` stub with the real paged query.
- [ ] The `AddressRequest` Bean Validation constraints must exactly match the `openapi.yaml` schema constraints (spec 0003, `AddressRequest` row).
- [ ] The `@AddressSource` class-level constraint on `CreateOrderRequest` maps to `VALIDATION_FAILED` with `errors["addressId"]`; ensure `GlobalExceptionHandler` handles class-level `ConstraintViolation` (field path is empty for class-level; use the annotation's designated field name).
