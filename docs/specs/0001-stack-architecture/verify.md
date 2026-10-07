# Verify: Stack & architecture · spec 0001 · updated 2026-10-07
_Steps derived from the feature's Done when line and spec 0001 `## Proposed stack` (a decision spec, so it has no AC IDs; each step names the Done when clause it checks). `/check verify` runs these; `/test` locks the durable ones._

## One time setup (you, not the AI)
- [ ] In psql as `postgres` (`"C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres`): `CREATE ROLE bookworm_app LOGIN PASSWORD '<your password>';` then `CREATE DATABASE bookworm OWNER bookworm_app;`
- [ ] In the PowerShell window you will run from: `$env:DB_USERNAME="bookworm_app"; $env:DB_PASSWORD="<your password>"` (or copy `application-local.properties.example` to `application-local.properties` and fill it in)

## Commands
- [ ] `mvn clean install` (or `.\mvnw.cmd clean install`) → `BUILD SUCCESS`, `Tests run: 1, Failures: 0`, with no PostgreSQL and no `DB_*` needed → Done when: build succeeds
- [ ] With `DB_USERNAME` and `DB_PASSWORD` set, `mvn spring-boot:run` from the project root → log shows `Started EbookstoreApplication` and `Tomcat started on port 8080`; Flyway logs `No migrations found` (expected, feature 3 adds them) → Done when: app runs on an empty app
- [ ] `curl -i http://localhost:8080/anything` while running → `404` with a JSON error body → app is serving HTTP
- [ ] Open a new terminal with `DB_USERNAME` and `DB_PASSWORD` unset, run `mvn spring-boot:run` → startup stops with `password authentication failed for user "${DB_USERNAME}"` (fails loudly, no silent fallback) → Done when: credentials come from env vars
- [ ] `git ls-files | Select-String -Pattern "application-local.properties$|\.env$"` → no output; `git grep -n -i "password=" -- ':!*.example' ':!docs'` shows only `${DB_PASSWORD}` → Done when: nothing secret in git
- [ ] After a successful run, in psql: `\c bookworm` then `\dt` → `flyway_schema_history` exists, owned by `bookworm_app` → DB credentials actually used

## Acceptance-criteria coverage
- Choices recorded in a spec: spec 0001 exists (`index.md`, `rationale.md`)
- `mvn clean install` succeeds: Commands step 1
- `mvn spring-boot:run` succeeds on an empty app: Commands steps 2, 3, 6
- DB credentials read from environment variables: Commands steps 2, 4
- Nothing secret in git: Commands step 5
