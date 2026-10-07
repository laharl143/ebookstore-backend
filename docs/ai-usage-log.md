# AI usage log

Tool: Claude Code (desktop app, Code tab), model Claude Opus 5.5.
Each entry records the prompt given, what the AI produced, and what I accepted, changed or rejected.

---

## 1. Data model proposal (workflow step 2)

**Date:** 2026-10-07

**Prompt (summary):** Read `docs/capstone-brief.md`; propose the data model as a Mermaid ERD plus
table list; this step is my manual design and I review it before anything else is generated. Ask
before making assumptions not covered in the brief.

**AI output:** `docs/data-model.md` — 17-table ERD, table list with constraints, proposed order
lifecycle, indexes, and 10 open questions where the brief is silent (category vs tags, formats,
authors per book, "My Writers", gift-point rates, payment flow, coupon limits, review eligibility,
admin role, order of totals).

**Review / changes by me:** _pending (open questions will be settled in the data model spec)_

---

## 2. Scoping the minimum passing build

**Date:** 2026-10-07

**Prompt (summary):** Shared the capstone AI review rubric and asked for a bare minimum build that
passes, using the `/scope` skill. Then shared the use case slide (12 customer journeys) and the
architecture slide.

**AI output:** `docs/scope/scope.md`, a 16 feature plan: 5 foundations (stack, standards, data
model, OpenAPI contract, auth), the 12 journeys grouped into 3 journey phases (Catalogue,
Payment & purchase, E-store Home), and 3 submission features (run kit, PR, report). Wishlist,
reviews, coupons, bestsellers, admin, shipping and Docker were moved to Deferred.

**My decisions (asked as option panels):**
- MVP = the 12 journeys on the use case slide, built thin (accepted the AI recommendation).
- Agentic tool = Claude Code only; no IBM Bob access, and the report will say so.
- Deployability skipped; no hard deadline.
- Build approach = Journey (accepted the recommendation).
- Workflow = Alpha (I chose this over the AI's Beta recommendation). The AI tagged auth, checkout,
  payment and cancel as Beta so they still get tests.
