# Verify: data model · spec 0002 · updated 2026-10-07
_Steps derived from spec 0002 acceptance criteria. `/check verify` runs these; `/test` locks the durable ones._

## Commands
- [ ] `mvn clean install` → BUILD SUCCESS; Flyway logs "Successfully applied 2 migrations" on the H2 URL from spec 0001; 40 tests pass → AC-2
- [ ] Drop and recreate the empty `bookworm` database, then `mvn spring-boot:run` → log shows "Migrating schema ... version 1" and "... version 2", then "Started EbookstoreApplication" (no Hibernate schema validation error) → AC-1
- [ ] In psql: `SELECT version, success FROM flyway_schema_history;` → rows `1 t` and `2 t` → AC-1
- [ ] In psql: count rows → 19 categories (sidebar order by id), 10 genres, at least 6 authors, at least 4 publishers, at least 24 books → AC-3
- [ ] In psql: `SELECT count(*) FROM books b WHERE NOT EXISTS (SELECT 1 FROM book_genres g WHERE g.book_id = b.id);` → 0 → AC-3
- [ ] In psql: `SELECT count(DISTINCT format) FROM books;` → 3; a title appears in two formats; at least one PAPERBACK or HARDCOVER has `stock_quantity = 0`; no price outside ₱99 to ₱3,000 → AC-3

## Database rules (psql, run each inside `BEGIN; ... ROLLBACK;`)
- [ ] Insert a user with email `Upper@Example.com` → fails on `ck_users_email_lower` → AC-4
- [ ] Insert the same lower case email twice → fails on `uq_users_email` → AC-4
- [ ] `UPDATE books SET stock_quantity = -1 WHERE id = 1;` → fails on `ck_books_stock` → AC-4
- [ ] `UPDATE books SET price = -1 WHERE id = 1;` → fails on `ck_books_price` → AC-4
- [ ] Insert two cart lines for the same user and book → fails on `uq_cart_items_user_book`; a quantity of 0 fails on `ck_cart_items_qty` → AC-4
- [ ] Insert a book with an existing title and format → fails on `uq_books_title_format` → AC-4
- [ ] Set an order status to `LOST`, a book format to `AUDIOBOOK`, a payment method to `CASH`, a ledger type to `BONUS` → each fails on its `ck_` constraint → AC-4
- [ ] Insert an address with ZIP `123`, or a card payment with `card_last4 = '42'` or NULL → fails on `ck_addresses_zip`, `ck_payments_last4`, `ck_payments_card` → AC-4
- [ ] Insert an order whose total is not `subtotal + vat_amount + delivery_charge - gift_points_amount` → fails on `ck_orders_total`; an order line whose `line_total` is not `unit_price × quantity` → fails on `ck_order_items_line` → AC-4
- [ ] Insert an EARNED ledger row with negative points → fails on `ck_gpt_sign` → AC-4
- [ ] Insert a duplicate order number or payment transaction id → fails on `uq_orders_number` / `uq_payments_txn` → AC-4

## Code review
- [ ] Every table except `book_genres` has an entity in `entity/` and a repository in `repository/`; enums use `EnumType.STRING`, money is `BigDecimal`, timestamps are `OffsetDateTime`, every association is LAZY; `Book.genres` is a `@ManyToMany Set<Genre>` → AC-5
- [ ] No controller exists yet that returns an entity → AC-5
- [ ] `SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND (column_name LIKE '%card_number%' OR column_name LIKE '%cvv%' OR column_name LIKE '%expir%' OR column_name LIKE '%wallet%');` → no rows → AC-6
- [ ] `docs/data-model.md` shows the 13 table ERD in pesos, with no wishlist, review or coupon tables → AC-7

## Value sources this schema feeds (check now, again when the feature that uses them lands)
- [ ] In stock: an EBOOK row with stock 0 counts as in stock, a PAPERBACK with stock 0 does not (`Book.isInStock()`) → Value sourcing "in stock"
- [ ] Order number: `SELECT nextval('order_number_seq')` twice → increasing numbers, never reset by date → Value sourcing "order_number"
- [ ] Config: `app.currency=PHP`, `app.zone=Asia/Manila`, `app.vat-rate=0.12`, `app.delivery-charge=0.00`, `app.delivery-days=5`, `app.points.pesos-per-point=100`, `app.points.peso-value=1`, `app.cancel-window-hours=48` bind into `StoreProperties` (change one in `application-local.properties` and confirm the bound value changes) → Value sourcing currency, VAT, delivery, points, cancel window
- [ ] Timestamps: a row written through JPA at 23:30 Manila time stores the UTC instant (15:30Z), so the date does not shift (`hibernate.jdbc.time_zone=UTC`, `Clock` bean) → Value sourcing "any timestamp"

## Acceptance-criteria coverage
- AC-1 covered by the `spring-boot:run` and `flyway_schema_history` steps · AC-2 by `mvn clean install` · AC-3 by the count steps · AC-4 by the database rules steps · AC-5 by the code review steps · AC-6 by the information_schema step · AC-7 by the docs step
