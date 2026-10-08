# 0004. Customer authentication and JWT security

**Date**: 2026-10-07
**Status**: Accepted

## Summary

This spec designs customer registration, login, and JWT bearer authentication for the Book Worm store. Customers register with email and password or log in to receive an HS256-signed JWT token valid for 60 minutes. Spring Security and OAuth2 Resource Server authenticate incoming requests, making the user ID available from the token subject claim. Catalogue browsing, authentication endpoints, and OpenAPI/Swagger UI remain public, while all customer operations require a valid token with uniform RFC 9457 error handling.

## Requirements

**User stories**:
- As a new customer, I want to create an account so that I can save addresses, build a basket, and place orders.
- As a returning customer, I want to log in with my email and password to receive a token for authenticated requests.
- As an API client, I want unauthenticated access to protected endpoints to return a clear RFC 9457 Problem response with `code: UNAUTHORIZED`.

**Acceptance criteria** (the contract):
- **AC-1**: `POST /api/v1/auth/register` creates a user record in the `users` table with BCrypt hashed password, role `CUSTOMER`, initial points 0, and returns `201 Created` with `AuthResponse` containing `accessToken`, `tokenType: Bearer`, `expiresIn: 3600`, and `user` summary.
- **AC-2**: `POST /api/v1/auth/register` rejects duplicate emails (case-insensitive) with `409 Conflict` and `code: EMAIL_TAKEN`.
- **AC-3**: `POST /api/v1/auth/register` and `POST /api/v1/auth/login` normalize email input by trimming whitespace and converting to lowercase before processing.
- **AC-4**: `POST /api/v1/auth/login` verifies BCrypt password against the stored hash and returns `200 OK` with `AuthResponse`. Invalid email or incorrect password returns `401 Unauthorized` with `code: INVALID_CREDENTIALS`.
- **AC-5**: Spring Security validates incoming `Authorization: Bearer <token>` headers on protected endpoints. Expired, malformed, or missing tokens on protected endpoints return `401 Unauthorized` with `code: UNAUTHORIZED` in RFC 9457 `application/problem+json` format.
- **AC-6**: Public endpoints (`/api/v1/auth/**`, `/api/v1/books/**`, `/api/v1/categories/**`, `/api/v1/authors/**`, `/api/v1/publishers/**`, `/openapi.yaml`, `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`) allow unauthenticated access. An expired or invalid token supplied on a public endpoint is ignored and does not block the request.
- **AC-7**: JWT tokens are signed using HMAC-SHA256 (HS256) with a secret key from `app.jwt.secret` (environment variable `JWT_SECRET` with a safe local fallback default of at least 32 bytes) and carry `sub` set to the user ID integer string, standard `iat`, and `exp` set to 3600 seconds.
- **AC-8**: Request passwords are never logged, never returned in response DTOs, and are masked in DTO `toString()` implementations.

## Decision

**Chosen option**: Option 1: Spring Security OAuth2 Resource Server with Nimbus JOSE JWT Encoder/Decoder and Custom AuthenticationEntryPoint.

Spring Security filters validate JWT tokens statelessly. A dedicated `AuthService` handles registration, password hashing with `BCryptPasswordEncoder`, and token issuance with `NimbusJwtEncoder`. A custom `AuthenticationEntryPoint` bridges security authentication failures into the project's standard `GlobalExceptionHandler` and RFC 9457 `ProblemDetail` responses.

**Implementation skills**: `java-springboot` (`github/awesome-copilot`, `.claude/skills/java-springboot/`)

## Rationale

Reasoning, the options weighed, and the decision log: see [rationale.md](rationale.md).

## Feature design

**Data model sketch**:
Uses the existing `users` table from spec 0002. No new database tables or Flyway migrations required.

| Field | Source / Handling |
|---|---|
| `users.email` | `RegisterRequest.email` trimmed and lower-cased |
| `users.password_hash` | `BCryptPasswordEncoder.encode(RegisterRequest.password)` |
| `users.first_name` | `RegisterRequest.firstName` |
| `users.last_name` | `RegisterRequest.lastName` |
| `users.phone` | `RegisterRequest.phone` (optional) |
| `users.role` | `CUSTOMER` (fixed default) |
| `users.gift_points_balance` | `0` (initial balance) |
| `users.created_at`, `users.updated_at` | `OffsetDateTime.now(clock.withZone(ZoneOffset.UTC))` |

**State transitions**:
- Register → creates persistent `User` row with BCrypt hash → issues JWT token → returns `201 AuthResponse`.
- Login → looks up user by normalized email → verifies BCrypt hash → issues JWT token → returns `200 AuthResponse`.

### API surface

| Method | Path | Auth | Key DTOs / Result | Error Codes |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | public | `RegisterRequest` → `201 AuthResponse` | `VALIDATION_FAILED`, `EMAIL_TAKEN` |
| `POST` | `/api/v1/auth/login` | public | `LoginRequest` → `200 AuthResponse` | `VALIDATION_FAILED`, `INVALID_CREDENTIALS` |

**Value sourcing**:

| Action / Field | Value produced / displayed | Source |
|---|---|---|
| Register | `user.id` | Auto-generated database identity |
| Register / Login | `accessToken` | Generated JWT token containing `sub: "<id>"`, `iat`, `exp` |
| Register / Login | `tokenType` | Literal `"Bearer"` |
| Register / Login | `expiresIn` | `3600` (seconds, from config `app.jwt.expiration-seconds`) |
| Register / Login | `user` | Mapped `UserResponse` (`id`, `email`, `firstName`, `lastName`, `phone`, `giftPointsBalance`) |
| Token issuance | `sub` | String representation of `user.id` |
| Token issuance | `exp` | `now(clock) + 3600 seconds` |
| Protected endpoint | authenticated user ID | Extracted from `Jwt.getSubject()` via custom resolver or helper |

**Key invariants**:
- Passwords are never stored or logged in plain text; BCrypt hash is used exclusively.
- Email uniqueness is enforced both at application level and through database constraint `uq_users_email`.
- Tokens are stateless; no server-side token table or session state.
- Authentication entry point produces RFC 9457 `ProblemDetail` with `code: UNAUTHORIZED`.

**Security model**:
- Stateless JWT authentication (`SessionCreationPolicy.STATELESS`).
- CSRF disabled (stateless REST API with bearer tokens).
- Public endpoints matching `POST /api/v1/auth/**`, `/api/v1/categories/**`, `/api/v1/books/**`, `/api/v1/authors/**`, `/api/v1/publishers/**`, `/openapi.yaml`, `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`.
- All other endpoints require authenticated JWT.
- A custom `BearerTokenResolver` allows requests to public paths to proceed even if an expired/invalid token is attached.

**Configuration required**:
- `pom.xml`: Add `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server`.
- `application.properties`:
  ```properties
  app.jwt.secret=${JWT_SECRET:bookworm-secret-key-must-be-at-least-32-bytes-long-for-hmac-sha256}
  app.jwt.expiration-seconds=3600
  ```

**Critical test scenarios**:
- Registration success: `POST /api/v1/auth/register` creates user and returns valid token (**AC-1**).
- Registration duplicate email: `POST /api/v1/auth/register` with existing email returns 409 `EMAIL_TAKEN` (**AC-2**).
- Email normalization: Registration with `  Test.User@Example.COM  ` allows login with `test.user@example.com` (**AC-3**).
- Login success: `POST /api/v1/auth/login` returns valid token and user profile (**AC-4**).
- Login failure: Incorrect password or non-existent email returns 401 `INVALID_CREDENTIALS` (**AC-4**).
- Protected endpoint rejection: Calling protected endpoint without token returns 401 `UNAUTHORIZED` (**AC-5**).
- Public endpoint access: Calling catalogue endpoint without token succeeds (**AC-6**).
- Token inspection: Decoded token has `sub` matching user id and expires in 3600s (**AC-7**).
- Password masking: DTO `toString()` does not reveal password (**AC-8**).

## Build plan

1. Add `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server` to `pom.xml` and configure properties, satisfies **AC-5**, **AC-7**
2. Create DTO records (`RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserResponse`) with Bean Validation and custom `toString()` masking, satisfies **AC-1**, **AC-3**, **AC-8**
3. Add `findByEmail(String email)` and `existsByEmail(String email)` to `UserRepository`, satisfies **AC-2**, **AC-4**
4. Implement `JwtService` with `JwtEncoder` and `JwtDecoder` beans for token generation and extraction, satisfies **AC-7**
5. Implement `AuthService` for registration, login, password verification, and user mapping, satisfies **AC-1**, **AC-2**, **AC-3**, **AC-4**
6. Configure Spring Security (`SecurityConfig`) with stateless session, public URL matchers, `BearerTokenResolver`, and custom `ProblemAuthenticationEntryPoint`, satisfies **AC-5**, **AC-6**
7. Implement `AuthController` under `/api/v1/auth` for register and login, satisfies **AC-1**, **AC-4**
8. Write comprehensive unit and integration tests (`AuthServiceTest`, `AuthControllerTest`, `SecurityConfigTest`), satisfies **AC-1** to **AC-8**

## Consequences

**Positive**:
- Standard Spring Security architecture with zero custom filter boilerplate.
- Completely stateless authentication ready for all subsequent journey endpoints.
- Uniform RFC 9457 error responses across both application and security layers.

**Negative / tradeoffs**:
- Symmetric HS256 secret shared within the backend (acceptable for monolith, not for distributed identity provider).
- No token revocation or refresh token in MVP; tokens remain valid for full 60-minute duration.

**Neutral**:
- `ADMIN` role is stored in user table but not utilized by MVP endpoints.

## Follow-up

- [ ] Feature 6 & 7 (Catalogue) build against the public endpoint rules established here.
- [ ] Feature 8 (Basket) and Feature 9 (Checkout) extract current user ID from the authenticated token subject.
