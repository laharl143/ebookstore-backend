---
name: develop
description: >-
  Build a feature from its spec's Build plan, keep the build green, update scope
  and log
metadata:
  user-invocable: true
  disable-model-invocation: true
  argument-hint: '<feature name, e.g. API contract>'
---

Build the feature I named after this command. If I named none, use the first feature in `docs/scope/scope.md` whose status is `in-progress` and whose `Build it` box is unticked.

Follow these steps in order. Tell me at each step what you found before you change anything big.

1. **Check the repo.** Run `git status` and `git pull`. If there are uncommitted changes in the area you will touch, stop and ask me.
2. **Gate.** Find the feature in `docs/scope/scope.md`. If it says `needs a decision` and has no `spec` link, stop and tell me to run `/architect <feature>` first. Do not build.
3. **Read only what you need.** The feature's section in the scope, its spec `index.md` (Requirements, Decision, Feature design, Build plan, Consequences), and `.claude/skills/java-springboot/SKILL.md`. Skip `rationale.md`.
4. **Coverage check.** List every value the acceptance criteria need the code to produce. If the spec does not name where one comes from, stop and ask me. Do not invent it.
5. **Resume, do not redo.** Look at the code and the scope milestones to see which Build plan steps already exist. Say where you pick up.
6. **Mark the start.** Set the spec's `**Status**:` line from `Proposed` to `In Progress` (only that line). Make sure the feature's status in the scope is `in-progress`.
7. **Build.** Work through the Build plan steps in order. After each step run `mvn clean install` and fix it until it is green. If the spec turns out to be wrong, stop and tell me before you deviate from it.
8. **Update the scope.** Tick each milestone under `Build it` whose steps are done and green, then tick `Build it` when all are done. Add `· code in <paths>` to the feature's `spec` pointer line. Never tick `Verify it`.
9. **Log it.** Add the next numbered entry to `docs/ai-usage-log.md` (tool: IBM Bob): what you built, the tests, any choices you made inside the spec.
10. **Commit and push** with one conventional commit, e.g. `feat: <what landed> (spec NNNN)`.
11. **Report.** List what you built, the test result, and each acceptance criterion with how it is covered. Then ask me: mark the feature done now, or run `/verify <feature>` first?
