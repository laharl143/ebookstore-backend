---
name: verify
description: Prove a built feature against its spec's acceptance criteria on the real app
metadata:
  user-invocable: true
  disable-model-invocation: true
  argument-hint: '<feature name, e.g. API contract>'
---

Verify the feature I named after this command against its spec. Do not change application code in this command; if something fails, report it and stop.

1. Find the feature in `docs/scope/scope.md` and open its spec `index.md`. If the spec folder has a `verify.md`, use its steps as the checklist.
2. List every acceptance criterion (`AC-1`, `AC-2`, ...) and, for each, a concrete check: a command, an HTTP call, or a database query.
3. Run `mvn clean install` and report the test count and result.
4. Start the app with `mvn spring-boot:run` (from the project root, so `application-local.properties` is found). If port 8080 is busy, use `-Dspring-boot.run.arguments=--server.port=8091` and do not stop the other process.
5. Run each check. For HTTP calls use `curl` and show the status and the key part of the body. For data, use psql at `C:\Program Files\PostgreSQL\16\bin\psql.exe` with the credentials from `application-local.properties`, and never print the password.
6. Also check each row of the spec's Value sourcing table that this feature produces, including an edge case (a different input, an empty result, an error path).
7. Stop the app.
8. Write the results to `docs/reviews/<feature-slug>-verify.md`: a table of AC, check, expected, actual, PASS or FAIL, plus anything you could not check and why.
9. If every AC passes, tick the feature's `Verify it` box in the scope (only that box) and ask me whether to mark the feature `done`. If I say yes, set the scope status to `done` and the spec's `**Status**:` to `Accepted`.
10. Add a numbered entry to `docs/ai-usage-log.md` (tool: IBM Bob), then commit and push (`test:` or `docs:` subject).
