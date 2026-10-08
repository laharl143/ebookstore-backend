# 0005. Checkout and order: rationale

Decision record for [index.md](index.md). `/develop` does not read this file.

## Context

Feature 9 is the first feature that writes to multiple tables in one user action: it touches `orders`, `order_items`, `cart_items` (delete), `books` (stock), `users` (points balance), `gift_point_transactions`, and optionally `addresses`. Specs 0002 and 0003 settle the schema, the totals formula, the points rules, the address input shape, the error codes, the `OrderResponse` schema, and the user row lock requirement. What is left to decide for this feature is (1) transaction boundary strategy, (2) how to report multiple stock failures, and (3) the auto-default rule for addresses saved at checkout.

## Options considered

### Option 1 (chosen): Single transaction, inline address save inside the order transaction

All of order creation — user row lock, basket load, stock checks, address resolution, points validation, stock decrements, points deduction, ledger write, order insert, basket clear, and optional address save — runs in one `@Transactional` method.

**Pros**: atomicity is guaranteed by one transaction boundary; a failed order never leaves a dangling address; the user row lock held for the full duration prevents any concurrent mutation; straightforward to read and reason about.
**Cons**: the method is long; the address side effect is coupled to the order transaction, so if address save is ever needed independently it would need extraction.

### Option 2: Separate transaction for address save

`saveAddress = true` triggers a call to `AddressService.save(...)` in its own transaction before the order transaction starts.

**Pros**: cleaner separation; `AddressService` is reused.
**Cons**: if the order transaction fails after the address save commits, the address is saved but the order is not, leaving the user with a new default address from a failed order — confusing. Requires a compensating delete or a saga pattern that is far too heavy for a capstone MVP.

### Option 3 (collect all stock failures vs. fail fast)

Spec 0003 defines `INSUFFICIENT_STOCK.bookIds` as a list, implying a multi-failure response. Collecting all failures is the right user experience; fail-fast would force the user to retry once per bad item.

**Pros of collecting all**: one call surfaces every problem; matches spec 0003's list-typed `bookIds`.
**Cons of collecting all**: slightly more code; no meaningful downside.

## Rationale

Option 1 is chosen because it is the safest choice for a multi-table write that must be atomic. The address-save coupling is acceptable — checkout is the only place `saveAddress` is used, and the spec explicitly calls out that rollback should undo the address save too. Collecting all stock failures (Option 3 recommendation) follows from the contract's `bookIds` list and avoids user frustration.

The auto-default rule for addresses (first address for a user always becomes default, regardless of the request flag) mirrors the existing `AddressService` pattern documented in spec 0002 and is applied consistently whether the address is saved via `POST /me/addresses` or via `saveAddress = true` at checkout.

## Decision log

| # | Question | Options | Pick |
|---|---|---|---|
| 1 | Stock failure reporting | A: collect all failing ids; B: fail on first | A (recommended): collect all |
| 2 | Transaction boundary | A: all in one transaction; B: address save in its own transaction | A (recommended): single transaction |
| 3 | First-address auto-default at checkout | A: apply same auto-default rule as address endpoint; B: honour only the request flag | A (recommended): apply auto-default rule |
| 4 | Address endpoint controller placement | A: new `AccountController`; B: separate `AddressController` | A (recommended): `AccountController` (same `Account` tag as `/me` and recommendations) |

All four picks were the recommended option, taken under the "pick recommended" instruction from the user.
