---
name: sync
description: 'Reconcile the scope, specs and AI usage log with what is actually in the repo'
metadata:
  user-invocable: true
  disable-model-invocation: true
---

Bring the project docs in line with the repo. Change docs only, never application code.

1. Run `git pull` and `git log --oneline -15`.
2. For each feature in `docs/scope/scope.md` that is `in-progress`: compare its ticked boxes with the code and the spec's Build plan. Report any box that is ticked without the code, or code that exists without its box ticked. Fix only clear cases, and tell me what you changed.
3. For each spec in `docs/specs/`: check its `**Status**:` line matches its feature (`Proposed` = not started, `In Progress` = building, `Accepted` = feature done). Report mismatches and fix them only when the scope status is clear.
4. Check `AGENTS.md` still matches reality (dependencies in `pom.xml`, packages under `src/main/java/com/bookworm/ebookstore`). List any new convention that should be added and ask me before you edit `AGENTS.md`.
5. Make sure the last steps all have an entry in `docs/ai-usage-log.md`; add a short one for anything missing (tool: IBM Bob).
6. Commit and push any doc changes with a `docs:` subject, then tell me the next step: the first unticked box in the scope.
