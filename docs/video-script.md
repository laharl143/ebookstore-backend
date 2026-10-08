# Walkthrough video script

Target length: about 3 minutes.
Record your screen. Talk through each section as you go.
The sections below are what to show on screen and what to say.

---

## Section 1 — Open IBM Bob and show a command running (about 40 seconds)

**Show on screen:** IBM Bob open in VS Code or the browser, this chat window visible.

**Say:**

"I built this project using IBM Bob, the agentic IDE from IBM. Bob runs inside VS Code and takes slash commands to design, build, and verify features one at a time.

For example, when I ran `/develop basket`, Bob read the API contract, checked what already existed in the codebase, implemented the shopping cart service and controller, ran the tests, and committed the result. Every step is logged in `docs/ai-usage-log.md`, which has 26 entries covering the entire build."

**Show on screen:** Briefly scroll through `docs/ai-usage-log.md` in VS Code so the entries are visible.

---

## Section 2 — Show the API contract (about 30 seconds)

**Show on screen:** Open `src/main/resources/openapi.yaml` in VS Code. Scroll slowly through the paths section.

**Say:**

"The project is built API first. Before writing any Java code, I wrote this OpenAPI 3.0.3 specification by hand. It defines 25 operations across 6 tags: Auth, Account, Catalogue, Cart, Orders, and Payments. Every controller had to match this file, and a drift test in the build fails if they drift apart."

---

## Section 3 — Start the app and show Swagger UI (about 30 seconds)

**Show on screen:** A terminal. Run `mvn spring-boot:run`. Wait for the "Started EbookstoreApplication" log line.

**Say:**

"Starting the app. Flyway runs two migrations automatically: the schema with 13 tables, and the seed data with 27 books."

**Then switch to a browser and open `http://localhost:8080/swagger-ui.html`.**

**Say:**

"Swagger UI loads the hand-written contract directly. A reviewer can try every endpoint here without reading code."

---

## Section 4 — Run a few Insomnia calls (about 45 seconds)

**Show on screen:** Open Insomnia with the imported collection from `docs/insomnia-collection.json`. Run the following requests one at a time, pausing briefly on each response:

1. `POST /api/v1/auth/register` — show the 201 response with the JWT token.
2. Set the `token` environment variable. Then run `GET /api/v1/books?category=romance` — show a page of books.
3. Run `POST /api/v1/cart/items` — add a book to the basket.
4. Run `POST /api/v1/orders` — place the order. Show the 201 response with `orderNumber`, `subtotal`, `vatAmount`, and `totalAmount`.
5. Run `POST /api/v1/orders/{orderId}/payments` with the wallet method — show the `PurchaseConfirmationResponse` with the purchased books.

**Say (while running):**

"Register returns a JWT. Browse the catalogue — no token needed. Add a book to the basket. Place the order — the server computes all totals: subtotal, 12% VAT, delivery charge, and the final amount. Pay with a wallet. The response confirms the purchase and shows the books bought."

---

## Section 5 — Show the database rows (about 20 seconds)

**Show on screen:** Open psql or a database GUI (pgAdmin, DBeaver). Run a quick query:

```sql
SELECT id, order_number, status, total_amount FROM orders LIMIT 5;
SELECT id, order_id, quantity FROM order_items LIMIT 5;
```

**Say:**

"The rows are in PostgreSQL. The order number follows the format BW-yyyyMMdd-NNNNNN. Stock was decremented, the basket was cleared, and a gift points transaction was recorded — all in one database transaction."

---

## Section 6 — Run the tests (about 20 seconds)

**Show on screen:** A terminal. Run `mvn test`. Wait for the result line.

**Say:**

"The test suite runs on H2 in PostgreSQL mode — no external database needed for CI. 141 tests, zero failures."

---

## Section 7 — Show the GitHub PR (about 15 seconds)

**Show on screen:** Open https://github.com/laharl143/ebookstore-backend/pull/1 in a browser. Scroll the PR description briefly.

**Say:**

"The work is on the `feature/api-implementation` branch. PR number 1 carries the full API documentation: what changed, how to run it, and the complete endpoint list. That is the link I submitted with this video."

---

## Text field entry (paste this into the submission text box)

```
GitHub repository: https://github.com/laharl143/ebookstore-backend
Pull request: https://github.com/laharl143/ebookstore-backend/pull/1

I built a Spring Boot 3 and PostgreSQL REST API covering all 12 customer journeys
from the capstone brief: authentication, catalogue browsing, basket, checkout with
server-side totals and gift points, simulated payment, order cancellation within
48 hours, order history, Buy It Again, and personalised recommendations. The project
is API-first: the OpenAPI 3.0.3 contract was written before the code, and a drift
test in the build fails if the controllers do not match it. I used IBM Bob as the
agentic IDE throughout; every prompt, AI output, and decision is recorded in
docs/ai-usage-log.md across 26 entries. All 141 automated tests pass on H2 in
PostgreSQL mode.
```

---

## Recording tips

- Use a screen recorder with system audio capture (OBS, Loom, or the Windows Xbox Game Bar with Win+G).
- Keep each section tight. Pause between sections rather than rushing.
- You do not need to narrate the whole log. The sections above are the minimum the grader needs to see.
- If something goes wrong on screen, just say "let me try that again" and redo it. Short pauses are fine.
