# Capstone brief: "Book Worm" e-bookstore backend

Source: IBM "AI Specialist - Cloud FullStack - Capstone" instructions (12 slides) and the
wireframe screenshots on slides 5–10. This file is the full context for building the backend.

## Goal

Build the **backend** for an e-commerce platform where customers browse, select and buy books.
The capstone is graded on the working result **and** on how AI-assisted development was used
(the course names IBM BOB or AWS Kiro as the agentic IDE). Final deliverables are a PR on a
personal GitHub repo (manager has access) and a recorded walkthrough video.

## Required stack (from slide 11–12)

- Java 17, Spring Boot 3.x, Maven (`mvn clean install`, `mvn spring-boot:run`)
- PostgreSQL (local install) — configured in `application.properties`
- OpenAPI specification (API-first: spec written before/alongside the code)
- Automated tests (the course suggests an ICA test-generator agent; JUnit/MockMvc in repo)
- Local run is enough; cloud deploy (ROSA/ROKS) is optional
- Git: feature branch `feature/api-implementation`, PR with API documentation

## Customer journeys (slide 3)

Home: login page, user authentication, order history with **Buy It Again**, recommendations
based on order history.
Catalogue: select category, product catalogue per category, browse brands (publishers/writers),
select product (shows **tentative delivery date**), related products.
Cart: add products to basket, recommendations based on order history.
Payment & purchase: select delivery address, pay with the chosen option, **redeem gift points**,
payment confirmation, purchase confirmation, **cancel order within 48 hours**.

## Wireframe details

### 1. Home / catalogue page
- Top nav: "Book Worm" logo, My Orders, My Wishlist, My Writers, cart icon with item count, profile.
- Left sidebar categories: All, Romance, Mystery, Science Fiction, Fantasy, Historical, Biography,
  Self-help, Memoir, Travel, Cooking, Children's, Young Adult, Comics & Graphic Novels, Poetry,
  Drama, Science, Philosophy, Religion, Language Learning.
- Filters: free-text search, Language, Format (Paperback, Hardcover, eBook), Price Range,
  Sort by (Relevance, plus price/newest/rating).
- Sections: **Recommended for You**, **Bestsellers this Month**, **New Launches**.
- Book card: cover image, title, author (link), short description, format, genre tags
  (multiple, e.g. "Non-fiction, Self Help" / "Fiction, Thriller, Horror"), price in ₹,
  "Delivery by Mon, 21 Jul".

### 2. Product detail page
- Breadcrumb: Home / Non-Fiction / Self Help.
- Front and back cover images.
- Title, author (link), description, "Published by: ABC Publishers" (link), format, genre tags,
  price, delivery-by date.
- Buttons: Add to Cart, Add to Wishlist.
- Stats: Language, average Rating (stars), "Sells: 145 copies sold".
- About the writer: photo, name, bio.
- Reviews: "Leave Your Review" text box with a 0/100 character counter, 1–5 star rating, Submit;
  list of reviews (reviewer name, text, stars).
- Related Reads: list of book cards.

### 3. Shopping cart / checkout page
- Breadcrumb ending in Checkout.
- Cart items: cover, title, author, description, format, tags, price, delivery date,
  quantity stepper (− / +).
- Address form: "Use Saved Address" checkbox, First Name, Last Name, Address, e-mail, City,
  Pin (6 digits), Phone (country code +91 + number), State, Country (default India).
- Grand Total panel: Price (n items), Tax, Delivery Charges (Free), Apply Coupon, Discount,
  Total Amount, Pay Now.

### 4. Payment screen
- "Complete Payment" with Payable Amount.
- Methods: Credit Card, Debit Card, UPI, Wallet.
- Card fields: Card Number, Name on Card, CVV, Date of Expiry (MM/YYYY), Pay Now.

### 5. Purchase confirmation
- Success message "Your purchase of the following reads is successful", list of purchased books
  (same card layout), "Continue your Shopping".

## Entities to support (starting point — the data model design is a manual step to review)

User, Address, Category, Genre/Tag, Author (writer: name, photo, bio), Publisher, Book
(title, description, format, language, price, cover images, stock, copies sold, publish date),
Review, Wishlist, Cart, CartItem, Coupon, Order, OrderItem, Payment, GiftPoints (balance +
transactions).

## Business rules

- Authentication: register/login with JWT; passwords hashed (BCrypt).
- Delivery date: estimated from order date + standard shipping days (configurable).
- Order totals are **computed on the server**: subtotal, tax (configurable rate), delivery
  charge, coupon discount, gift-points redemption, total. (The wireframe numbers are mock data
  and don't add up — don't copy them.)
- Gift points: earned on completed orders, redeemable at checkout, cannot exceed balance or
  order total; restored if an order is cancelled.
- Cancel order: allowed only within **48 hours** of placing it and only if not yet shipped;
  refunds payment status and restores stock and gift points.
- Buy It Again: re-add items from a past order to the cart.
- Recommendations: based on genres/authors from the user's order history; fall back to
  bestsellers for new users.
- Related products: same genre/author, excluding the current book.
- Bestsellers: by copies sold in the current month. New launches: by publish date.
- Reviews: 1–5 stars, text max 100 characters, one review per user per book.
- Payments are **simulated**. Never store full card numbers or CVV — validate format, store only
  method, last 4 digits, status and a generated transaction ID.
- Stock is checked and decremented when an order is placed.

## Workflow and deliverables (slide 12)

1. Analyze wireframes → entities and features (this brief).
2. Data model design (ERD) — reviewed manually.
3. Generate OpenAPI spec → save as `src/main/resources/openapi.yaml`.
4. Generate Spring Boot code from the spec (controllers, services, repositories, entities, DTOs).
5. Set up and review project structure.
6. Configure PostgreSQL in `application.properties` (keep secrets out of git; use env vars).
7. `mvn clean install` then `mvn spring-boot:run`, no errors.
8. Test every endpoint (Insomnia collection or similar) and verify rows in the database.
9. Git: `feature/api-implementation` branch, push, PR with API documentation.
10. Record a walkthrough video for the manager.

Keep a short `docs/ai-usage-log.md` of the AI prompts used and what was accepted or changed —
the capstone is assessed on how AI was used, so the video and PR should describe the tools
honestly.
