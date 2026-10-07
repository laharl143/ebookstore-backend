---
name: architect
description: Design a feature that needs a decision and write its spec in docs/specs
metadata:
  user-invocable: true
  disable-model-invocation: true
  argument-hint: '<feature name, e.g. authentication>'
---

Design the feature I named after this command and write its spec. Write no application code in this command.

1. Find the feature in `docs/scope/scope.md` and read its intent and `Done when` line. Read `AGENTS.md`, the existing specs' `index.md` files that it builds on (0001 stack, 0002 data model, 0003 API contract), and only the code it must fit into.
2. List every decision this feature needs (data, endpoints, rules, security, failure cases, config). Mark which ones earlier specs already settle; do not ask those again.
3. Ask me the open decisions **one at a time**, each with 2 to 4 real options, one marked `(recommended)` with a one line reason. If I say "pick recommended", take the recommended option for the rest and record that.
4. Write the spec as `docs/specs/NNNN-<short-title>/index.md` (next free number) plus `rationale.md`, following the same sections as `docs/specs/0003-api-contract/`:
   - `index.md`: `**Date**`, `**Status**: Proposed`, `## Summary` (2 to 4 plain sentences), `## Requirements` (user stories and acceptance criteria `AC-1`...), `## Decision`, `## Rationale` (one line pointing to rationale.md), `## Feature design` (data model sketch, state transitions, API surface, **Value sourcing** table naming the source of every value, key invariants, security model, configuration, critical test scenarios), `## Build plan` (ordered steps, each tagged `satisfies AC-n`), `## Consequences` (with negatives), `## Follow-up`.
   - `rationale.md`: `## Context`, `## Options considered` (2 to 4, each with pros and cons), `## Rationale`, and a decision log table of every question, the options and the pick.
5. Check your own spec: every AC is covered by a Build plan step, and no value in the Value sourcing table lacks a source. Show me any gap and your recommended fix before you finish.
6. Ask me to accept the spec. Then update the feature in the scope: tick the `(spec)` box, remove `needs a decision` from the heading, add the line `spec [NNNN](../specs/NNNN-<title>/index.md)`, add `- [ ] Build it: /develop <feature>` with 2 to 5 milestone sub boxes rolled up from the Build plan, add `- [ ] Verify it: /verify <feature>` (and `- [ ] Test it` if the heading has `· Beta`), and set the status to `in-progress` in the heading and the At a glance table.
7. Add a numbered entry to `docs/ai-usage-log.md` (tool: IBM Bob), then commit and push with a `docs:` subject.
