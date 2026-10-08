# Book Worm e-bookstore backend

REST API for an online bookstore, built with Java 17, Spring Boot 3.5, Maven and PostgreSQL 16.
IBM Applied AI Specialist (Cloud FullStack) capstone project.

- OpenAPI spec: `src/main/resources/openapi.yaml`
- Interactive docs (when running): http://localhost:8080/swagger-ui.html
- API prefix: `/api/v1`

---

## Prerequisites

| Tool | Version tested |
|---|---|
| Java (IBM Semeru or any JDK 17) | 17.0.7 |
| Maven | 3.9.4 |
| PostgreSQL | 16 |

---

## PostgreSQL setup (Windows)

These steps use `psql`. On a default Windows install, `psql` is in  
`C:\Program Files\PostgreSQL\16\bin\` — add it to your PATH or open  
**SQL Shell (psql)** from the Start menu.

### 1. Open a psql session as the postgres admin

```
psql -U postgres
```

Enter the admin password when prompted.

### 2. Create the application role and database

```sql
CREATE ROLE bookworm_app LOGIN PASSWORD 'choose_a_strong_password';
CREATE DATABASE bookworm OWNER bookworm_app;
\q
```

Replace `choose_a_strong_password` with a password of your choice.  
Record it; you will need it in the next step.

---

## Configuration

The app reads credentials from environment variables (or a local override file).  
No secret is ever committed to git.

### Option A: environment variables (recommended)

Set these before running:

```powershell
$env:DB_USERNAME = "bookworm_app"
$env:DB_PASSWORD = "choose_a_strong_password"
# DB_URL defaults to jdbc:postgresql://localhost:5432/bookworm
```

### Option B: local properties file

Copy the example file and fill in your password:

```powershell
Copy-Item application-local.properties.example application-local.properties
```

Then edit `application-local.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bookworm
spring.datasource.username=bookworm_app
spring.datasource.password=choose_a_strong_password
```

This file is gitignored and will never be committed.

---

## Run the application

```bash
# Build and run all tests (H2, no PostgreSQL needed)
mvn clean install

# Start against local PostgreSQL (credentials must be set first)
mvn spring-boot:run
```

On first start, Flyway applies two migrations automatically:

- `V1__create_schema.sql` — creates 13 tables
- `V2__seed_catalogue.sql` — seeds 19 categories, 9 authors, 4 publishers and 27 books

The app starts on port **8080**.  
Open http://localhost:8080/swagger-ui.html to browse the interactive API docs.

---

## Running tests

```bash
mvn test
```

Tests run on H2 in PostgreSQL mode — no external database required.  
Expected result: **141 tests, 0 failures**.

---

## JWT secret (optional override)

A default development secret is set in `application.properties`.  
For a more secure local run, set:

```powershell
$env:JWT_SECRET = "my-custom-secret-at-least-32-characters-long"
```

---

## API endpoints

All protected endpoints require `Authorization: Bearer <token>`.  
Obtain a token from `POST /api/v1/auth/register` or `POST /api/v1/auth/login`.

### Authentication

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register a new customer account |
| POST | `/api/v1/auth/login` | Public | Login and receive a JWT |

### Account

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/me` | Required | Get current user profile and gift points balance |
| GET | `/api/v1/me/addresses` | Required | List saved delivery addresses |
| POST | `/api/v1/me/addresses` | Required | Save a new delivery address |
| GET | `/api/v1/me/recommendations` | Required | Get personalised book recommendations |

### Catalogue (public)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/categories` | Public | List all catalogue categories |
| GET | `/api/v1/books` | Public | Search and filter books with pagination |
| GET | `/api/v1/books/{bookId}` | Public | Get full book details |
| GET | `/api/v1/books/{bookId}/related` | Public | Get related books |
| GET | `/api/v1/authors` | Public | List authors with pagination |
| GET | `/api/v1/authors/{authorId}` | Public | Get author details |
| GET | `/api/v1/publishers` | Public | List publishers with pagination |
| GET | `/api/v1/publishers/{publisherId}` | Public | Get publisher details |

### Cart

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/cart` | Required | View the current basket with subtotal |
| POST | `/api/v1/cart/items` | Required | Add a book to the basket |
| PUT | `/api/v1/cart/items/{bookId}` | Required | Update quantity in the basket |
| DELETE | `/api/v1/cart/items/{bookId}` | Required | Remove a book from the basket |
| DELETE | `/api/v1/cart` | Required | Empty the basket |

### Orders

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/orders` | Required | Place an order from the basket |
| GET | `/api/v1/orders` | Required | List order history (newest first) |
| GET | `/api/v1/orders/{orderId}` | Required | Get order details |
| POST | `/api/v1/orders/{orderId}/cancel` | Required | Cancel an order within 48 hours |
| POST | `/api/v1/orders/{orderId}/buy-again` | Required | Re-add a past order's items to the basket |

### Payments

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/orders/{orderId}/payments` | Required | Process simulated payment |

---

## Error format

All errors follow [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Insufficient stock for book IDs: [3]",
  "errorCode": "INSUFFICIENT_STOCK"
}
```

---

## Project layout

```
src/main/java/com/bookworm/ebookstore/
  config/       Spring Security and store-properties beans
  controller/   REST controllers (thin, call services)
  dto/          Request and response record types
  entity/       JPA entities and enums
  exception/    Named exceptions and GlobalExceptionHandler
  mapper/       Hand-written entity-to-DTO mappers
  repository/   Spring Data JPA repositories
  service/      Business logic

src/main/resources/
  openapi.yaml                API contract (spec 0003)
  db/migration/               Flyway scripts (V1 schema, V2 seed)
  application.properties      Main config (no secrets)

src/test/java/                JUnit 5 tests (H2)
docs/                         Specs, scope, AI usage log, data model
```

---

## Insomnia collection

Import `docs/insomnia-collection.json` into [Insomnia](https://insomnia.rest) to run all 12 customer journeys end to end.

The collection uses environment variables `base_url`, `token`, `bookId`, `orderId` and `addressId`.  
Set `base_url` to `http://localhost:8080` before running.  
Run the requests in folder order: Auth first, then Catalogue, Cart, Orders, and Payments.

---

## Seed data quick reference

| Category slug | Example book ID |
|---|---|
| `romance` | 5 (Pride and Prejudice paperback) |
| `mystery` | 9 (The Hound of the Baskervilles) |
| `science-fiction` | 12 (Frankenstein paperback) |
| `historical` | 1 (Noli Me Tángere paperback) |
| `self-help` | 18 (Small Habits, Big Mornings paperback) |

IDs are assigned by the PostgreSQL identity sequence when Flyway seeds the data.  
If you reset the database, the IDs start at 1 again.
