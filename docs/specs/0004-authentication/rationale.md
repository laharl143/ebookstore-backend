# Rationale: Customer authentication and JWT security

## Context

The Book Worm store requires customer authentication to identify users for their shopping baskets, saved delivery addresses, checkout, order history, and cancellation. Authentication must conform to the contract established in spec 0003 and stack architecture from spec 0001.

## Options considered

### Option 1: Spring Security with OAuth2 Resource Server & Nimbus JOSE (Chosen)
- **Pros**: Standard Spring Boot 3 framework integration; robust JWT validation; automatic integration with Spring Security context; minimal custom code.
- **Cons**: Requires configuring secret key encoder and decoder beans explicitly for symmetric HS256.

### Option 2: Custom Filter with JJWT / Auth0 library
- **Pros**: Fine-grained control over filter chain.
- **Cons**: Reinvents Spring Security token resolution and context setup; higher maintenance and testing overhead.

### Option 3: Stateful HTTP Sessions
- **Pros**: Simpler client state.
- **Cons**: Violates REST stateless requirement and spec 0001/0003 architecture; does not scale horizontally.

## Rationale

Option 1 provides the cleanest, most maintainable Spring Boot implementation while matching the specifications and conventions laid out in specs 0001, 0002, and 0003.

## Decision log

| # | Question | Options offered | Pick | Reason |
|---|---|---|---|---|
| 1 | JWT secret key configuration fallback | Safe local fallback default vs Fail fast if unset | Safe local fallback default | Smooth developer experience and out-of-the-box test execution |
