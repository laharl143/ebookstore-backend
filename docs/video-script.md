# Walkthrough video script

Target length: about 3 minutes (7 sections, timings add up to 2:55).
Record your screen and talk through each section. "Show" is what is on screen, "Say" is what you say.

Before you record:
- Start PostgreSQL, run `mvn clean install` once, and close other apps that use port 8080.
- Open these ready in tabs: IBM Bob, Claude Code, `docs/ai-usage-log.md`, `src/main/resources/openapi.yaml`, Insomnia with `docs/insomnia-collection.json` imported, psql, and the PR page.
- In Insomnia, set `base_url` to `http://localhost:8080`.

---

## 1. How I used AI (35 seconds)

**Show:** Claude Code with the `docs/specs/` folder, then IBM Bob with a `/develop` task, then scroll `docs/ai-usage-log.md`.

**Say:**
"I built this with two agentic tools. In Claude Code I designed the project first: the stack, the data model and the API contract, each written as a spec and cross checked by a second model. Then I moved to IBM Bob. I gave Bob a small workflow in the `.bob` folder, with commands like `/develop basket`, and Bob built each journey from the specs, ran the tests and committed. Every prompt and decision, and which tool made it, is in this AI usage log."

## 2. The API contract (20 seconds)

**Show:** `openapi.yaml`, scroll the paths slowly.

**Say:**
"The project is API first. This OpenAPI 3.0.3 file was written before the controllers. It has 25 operations in 6 groups, one error format, and JWT security. A drift test in the build fails if a controller is not in this file."

## 3. Start the app and Swagger UI (20 seconds)

**Show:** A terminal running `mvn spring-boot:run` until "Started EbookstoreApplication". Then open `http://localhost:8080/swagger-ui.html`.

**Say:**
"Starting the app against PostgreSQL. Flyway applies two migrations, 13 tables and a seed catalogue of 27 books priced in pesos. Swagger UI shows the same contract, so a reviewer can try any endpoint here."

## 4. Customer journeys in Insomnia (45 seconds)

**Show:** Insomnia. Run these, pausing on each response:
1. Folder 1: **Register**, then **Log in**. Copy `accessToken` into `token`.
2. Folder 3: **GET /books?category=romance**.
3. Folder 5: **add book**, then **place order**. Copy the `id` into `orderId`.
4. **Declined card**: show the 402 `PAYMENT_DECLINED`.
5. **Credit card**: show the purchase confirmation with the books bought.
6. Folder 6: **second order**, copy its `id` into `orderId2`, pay with **e-wallet**, then **cancel**.

**Say:**
"Register and log in to get a token. Browse the catalogue without one. Add a book and place the order: the server computes the subtotal, 12 percent VAT and the total, never the client. A test card ending in 0002 is declined with a clear error, and a valid card confirms the purchase and earns gift points. Then a second order, paid with an e-wallet and cancelled within 48 hours: the payment is refunded and stock and points come back."

## 5. The rows in PostgreSQL (15 seconds)

**Show:** psql:
```sql
SELECT order_number, status, total_amount, gift_points_earned FROM orders ORDER BY id DESC LIMIT 2;
SELECT method, status, card_last4, failure_reason FROM payments ORDER BY id DESC LIMIT 3;
```

**Say:**
"The rows are in PostgreSQL. The declined attempt is kept as FAILED, only the last 4 card digits are stored, and the cancelled order's payment is REFUNDED."

## 6. What end to end testing caught (25 seconds)

**Show:** `docs/screenshots/05-journeys-part2.jpg`, then a terminal running `mvn test` until "146 tests, 0 failures".

**Say:**
"The unit tests passed early on, but when Claude Code ran every journey against the real database it found bugs the tests missed: a declined card was accepted, and cancelling twice gave stock and points back twice. Bob fixed them, the next live run caught one more, and now all 40 live calls pass and the suite has 146 tests, all green."

## 7. The pull request (15 seconds)

**Show:** https://github.com/laharl143/ebookstore-backend/pull/1, scroll the description.

**Say:**
"The work is on the `feature/api-implementation` branch. Pull request 1 explains what changed, how AI was used, how to run it, and lists every endpoint. That is the link I am submitting with this video."

---

## Text for the submission box

```
GitHub repository: https://github.com/laharl143/ebookstore-backend
Pull request: https://github.com/laharl143/ebookstore-backend/pull/1

I built a Spring Boot 3 and PostgreSQL REST API for the Book Worm bookstore that covers all 12 customer journeys in the brief: sign up and log in, browsing and search, book detail, basket, checkout with server side totals and gift points, simulated payment, cancel within 48 hours, order history, Buy It Again and recommendations. It is API first: a hand written OpenAPI 3.0.3 contract came before the controllers, and a drift test fails the build if they disagree. I used Claude Code to design the specs and verify the app end to end, and IBM Bob to build the features from those specs; every prompt and decision is in docs/ai-usage-log.md. Running the real app against PostgreSQL caught bugs the unit tests missed, which Bob fixed. The final build has 146 passing tests, all 40 live journey calls pass, and the screenshots are in docs/screenshots.
```

---

## Recording tips

- Any screen recorder works: OBS, Loom, or Windows Game Bar (Win+G).
- Keep each section tight and pause between sections instead of rushing.
- If something goes wrong on screen, say "let me try that again" and redo it.
- If you run out of time, shorten section 3 first; sections 1, 4 and 6 matter most for the AI workflow grade.
