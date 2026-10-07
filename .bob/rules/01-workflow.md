# Book Worm workflow (for every Bob mode)

`AGENTS.md` (loaded automatically) holds the stack, coding rules, commands and git settings. This file adds the workflow that ties the docs together. Where they differ, `AGENTS.md` wins.

## Where the state lives

- `docs/scope/scope.md`: every feature, its status (`planned`, `in-progress`, `done`) and its checkboxes. The next step for a feature is its first unticked box.
- `docs/specs/NNNN-title/index.md`: the build spec for a feature. Its `## Requirements` (acceptance criteria `AC-1`, `AC-2`, ...) is the contract, and its `## Build plan` is the ordered task list. `rationale.md` beside it is decision history; you do not need it to build.
- `docs/ai-usage-log.md`: one entry per AI step. The capstone is graded on how AI was used, so never skip it.
- `.claude/skills/java-springboot/SKILL.md`: general Spring Boot practices. Read it before writing Java; `AGENTS.md` and the specs win where they differ.

## The loop

1. **Design** (`/architect <feature>`): only for a feature marked `needs a decision`. Writes a spec, links it in the scope.
2. **Build** (`/develop <feature>`): builds the spec's `## Build plan` in order, keeps `mvn clean install` green.
3. **Verify** (`/verify <feature>`): proves each acceptance criterion on the real app.
4. **Sync** (`/sync`): reconciles the scope with the repo and closes the log entry.

## Hard rules

- Never build a feature marked `needs a decision` without a spec. If a value or behavior the build needs is not named in the spec, stop and ask me instead of inventing it.
- Never edit an applied Flyway migration (`V1__`, `V2__`); schema changes are a new `V<n>__` file.
- `src/main/resources/openapi.yaml` is the API contract (spec 0003). A controller that is not in the file must not exist; change the file first.
- Spec status line (`**Status**:`): `Proposed` → `In Progress` when a build starts → `Accepted` only when I say the feature is done. Change only that line, never other spec content during a build.
- Scope edits: change only the one checkbox, status word or pointer line you need. Never rewrite the file. Keep its CRLF line endings.
- Tick a box only for work that exists and passes `mvn clean install`. Never tick `Verify it` during a build.
- `done` is my call. When a build lands, ask me whether to mark it done or verify first.
- Never put real passwords, tokens or full card numbers in any tracked file.
- Git: commit at the end of a build with a conventional subject (`feat:`, `fix:`, `docs:`, `test:`, `chore:`). Pushing to `main` is allowed for foundation features (1 to 4). From feature 5 on, work goes on `feature/api-implementation`.
- Every step that changes files ends with a new numbered entry in `docs/ai-usage-log.md`, using the same headings as the earlier entries (Date, Prompt (summary), AI output, my decisions or choices to review, Review / changes by me). Name the tool as **IBM Bob**.

## Writing style for docs

Plain simple words, short sentences. No dashes or hyphens as punctuation in prose (code, paths and identifiers keep theirs).
