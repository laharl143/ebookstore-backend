# 0002. Data model: rationale

## Context

The Book Worm backend must support the 12 customer journeys on the capstone's use case slide: login, order history with Buy It Again, recommendations, a catalogue per category, browsing brands, a product page with a delivery date and related books, a basket, choosing an address, paying with a chosen method, redeeming gift points, confirmation, and cancelling within 48 hours. Every one of those journeys reads or writes the same core records, so the table design is the most expensive thing to change later. The capstone also lists "data model design based on wireframes (manual)" as its own step, and the reviewer expects to see an ERD.

Forces at play: the stack is fixed by spec 0001 (PostgreSQL 16, Flyway SQL migrations, Hibernate in `validate` mode, H2 in PostgreSQL mode for tests), so every SQL statement must run on both databases. Money must be exact. Payments are simulated, but the brief forbids storing card numbers or CVV. The engineer is in the Philippines, so prices are in pesos and addresses follow the Philippine format, even though the wireframes show rupees and an Indian address form. The scope is a bare minimum build for one developer, so every extra table and rule has a cost.

Not deciding this now would mean each journey inventing its own tables, and later journeys (cancel, history, recommendations) colliding with choices made earlier.

## Options considered

### Option 1: Minimal tables, compute everything live

Users, books, cart and orders only. Category, author and publisher are text columns on `books`; the order keeps a foreign key to the book and reads the current price; points are a single number on the user.

**Pros**:
- Fewest tables and the quickest migration to write.
- Nothing to keep in sync.

**Cons**:
- Changing a book's price silently changes every past order total.
- Browsing by author or publisher means grouping by free text, which breaks on spelling differences.
- A cancelled order cannot prove which points to restore or reverse; there is no history.

### Option 2: Normalised schema with order snapshots and a points ledger (chosen)

Separate catalogue tables (categories, genres, authors, publishers) linked to books; orders copy prices, titles and the address at checkout; a payments row per attempt; a gift points ledger next to a running balance.

**Pros**:
- Every journey's query has a clean join (category, brand, genre related books, history).
- Past orders never change, and cancel logic knows exactly what to undo.
- The ledger explains every balance change, which is easy to show in the demo.

**Cons**:
- More tables and entities to write up front.
- Duplicate data (order snapshots, one row per format).

### Option 3: Product plus variants (book and book formats)

Like Option 2, but one `books` row per title with a `book_formats` child holding format, price and stock; basket and order lines point at the variant.

**Pros**:
- No duplicated title and description across formats.
- Closest to how large stores model products.

**Cons**:
- Every basket, order, stock and recommendation query goes through an extra table.
- More work than the wireframe needs: each card shows one format and one price.

## Rationale

Option 2 fits the forces best. The cancel journey (restore stock, refund, restore and reverse points) and order history both depend on orders that remember what was paid, which rules out Option 1. Option 3's variant table solves duplication the demo catalogue barely has (a few titles in two formats), at the cost of an extra join in every purchase query, which is the wrong trade for a one person bare minimum build.

Within Option 2, the smaller choices follow the same logic: one author per book and one category plus many tags match the wireframe exactly and keep "related books" and "recommendations" to simple joins. A basket keyed directly by user removes a header table that would never hold anything. Bigint ids keep Insomnia URLs readable in the demo video. Constraints that H2 cannot express (partial unique index, regex) move to Bean Validation rather than splitting the migrations by database.

The Philippine localisation changes values, not structure: pesos, 12% VAT, 4 digit ZIP codes, provinces and +63 phones. E-wallet replaces the wireframe's UPI, which does not exist in the Philippines.

## Decision log

Asked one at a time; the engineer answered rounds 1 to 3 directly. From round 4 on, the engineer asked for the recommended option at every remaining decision until `/architect` finished, so those rows are auto picked and worth a review.

| # | Question | Pick | How |
|---|---|---|---|
| 1 | Category vs genre tags | One category plus many genre tags | engineer |
| 2 | Formats | One book row per format | engineer |
| 3 | Authors per book | One author | engineer |
| 4 | What is a "brand" (journey 6) | Authors and publishers | engineer |
| 5 | Checkout and payment | Two steps (order, then pay) | engineer |
| 6 | Order of totals | VAT on subtotal, then points | engineer |
| 7 | Currency | Philippine pesos (₱, PHP) | engineer (free text) |
| 8 | Gift points | 1 point per ₱100, 1 point = ₱1, earned on payment, reversed on cancel | engineer |
| 9 | Address format | Philippine (4 digit ZIP, +63, province, optional barangay) | engineer |
| 10 | Tax rate | 12% VAT (config) | engineer |
| 11 | Delivery date | 5 calendar days for print, same day for eBook | engineer |
| 12 | Order statuses | PENDING_PAYMENT, CONFIRMED, SHIPPED, DELIVERED, CANCELLED | engineer |
| 13 | Stock timing | Reserve at order creation; user may cancel an unpaid order; no expiry job | engineer |
| 14 | Payments | One row per attempt; a card ending in `0002` is declined | auto (recommended) |
| 15 | Basket tables | `cart_items` keyed by user | engineer |
| 16 | Primary keys | bigint identity, plus a public order number | engineer |
| 17 | Payment methods | `CREDIT_CARD`, `DEBIT_CARD`, `E_WALLET` (UPI dropped for PH) | auto (recommended) |
| 18 | eBook stock | Stock column kept but never checked or decremented for EBOOK | auto (recommended) |
| 19 | Order number | `BW-yyyyMMdd-NNNNNN` from a database sequence | auto (recommended) |
| 20 | Seed users | None; users register through the API in the demo | auto (recommended) |
| 21 | Earned points reversal when balance is short | Balance floors at 0, ledger records what was taken | auto (recommended) |
| 22 | Points cap | Balance and the whole pesos of the pre points total; total may reach ₱0 | auto (recommended) |
| 23 | Timezone for dates | Asia/Manila (`app.zone`) | auto (recommended) |
| 24 | Where config lives | `app.*` properties bound to a `StoreProperties` record | auto (recommended) |
| 25 | References section | None, keep it clean | auto (recommended) |
| 26 | Cross check on a second model | Run on Sonnet (recommended for a foundational spec at Alpha) | auto (recommended) |
| 27 | Spec acceptance | Auto accepted under the "pick recommended" instruction | auto |
| 28 | Cross check findings | Apply the recommended fixes (see below) | auto (recommended) |
| 29 | Points over the limit | Reject with 400 (matches scope feature 9), not silently cap | auto (recommended) |
| 30 | ₱0 orders | Not allowed: points may cover at most the total minus ₱1, so a payment is always needed | auto (recommended) |
| 31 | Concurrency | Guarded single statement updates for stock, points, copies sold and order status; no `version` columns | auto (recommended) |
| 32 | Signup bonus points | None; the demo places a first order to earn points | auto (recommended) |
| 33 | Ledger `balance_after` column | Dropped; the balance on `users` plus the ledger sum is enough | auto (recommended) |
| 34 | Saved addresses | Checkout takes `addressId` or an inline address with `saveAddress`; first saved is default; feature 9 owns the endpoints | auto (recommended) |
| 35 | Categories | The 19 real sidebar categories ("All" is not a row) | auto (recommended) |

## Cross check (Sonnet, read only)

A second model read the draft and returned 21 gaps and 4 soundness notes. All recommended fixes were applied. The ones that changed the design:

- **H2 compatibility**: `text` and `char(n)` would fail Hibernate `validate` on H2, so all text is `varchar`; timestamps are written as `timestamp with time zone`.
- **Seed and identity counters**: explicit ids in the seed would leave the identity counters behind, so the seed uses subselects by unique name (adding `UNIQUE` on author name and on book title plus format).
- **Concurrency**: read then write in Java could oversell stock or spend points twice; stock, points, copies sold and order status now change through guarded single statement updates, and the optimistic `version` columns were dropped.
- **Database enforced invariants**: named constraints everywhere, plus CHECKs for lower case email, order total, line total, ledger sign and card last 4 on card payments.
- **Rules that were left open**: points over the limit are rejected; no ₱0 orders; estimated delivery for mixed orders; saved address handling; eBook stock and quantity; what "bought" and "newest" mean; refunds update the payment row; e-wallet always succeeds; page sizes and sort keys; a `Clock` bean for testable time.
- **Tests**: `@SpringBootTest` instead of `@DataJpaTest`, which would replace the PostgreSQL mode H2 URL with a plain embedded database.

Kept as designed: the snapshot and ledger approach (confirmed sound), the order number sequence (works on both databases), and the unpaid order stock hold (an accepted tradeoff).
