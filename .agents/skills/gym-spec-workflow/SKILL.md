---
name: gym-spec-workflow
description: "Use when specifying or changing gym assystant product behavior, reviewing scope or requirement coverage, or modifying its durable data contract. Maintain specification-first changes and bidirectional traceability; distinguish the disposable mockup from native Android delivery."
---

# Gym specification workflow

Paths below are relative to this skill's installed location, `.agents/skills/gym-spec-workflow/`.

1. Read the relevant requirements in [spec.md](../../../docs/product/spec.md) and their entries in [traceability.md](../../../docs/product/traceability.md). Confirm the requested phase: the current deliverable is a disposable mockup; native Android implementation requires user authorization. Existing authorization continues to apply.
2. Before feature code, update the specification with the requested behavior, boundaries and observable acceptance criteria. Reuse existing `GYM-###` IDs for refinements; assign the next unused ID only to a new requirement. Keep specification and traceability revision labels aligned for product revisions. Documentation or workflow maintenance does not create a product requirement or revision.
3. Implement only that scope. Treat research as evidence, not a feature backlog. If a request leaves a routine detail open, resolve it from the existing specification and conventions; clarify only material ambiguity while continuing independent work.
4. Add or maintain requirement comments in behavior modules and `data-req` anchors on relevant UI. Update the requirement-to-source table with actual module/function or UI anchors and verification, and update the reverse inventory so each changed product area has an authorized requirement. Do not count an ID's presence as proof of working behavior.
5. Verify observable outcomes against acceptance criteria. Record actual results and limitations in traceability, distinguishing research, mockup placeholders, working mockup behavior and native implementation. Mark Android coverage only after native code and its checks exist.

## Preserve the meaning of training data

When touching plans, sessions, metrics or exports, read the corresponding requirements rather than deriving behavior from current code alone:

- Saved plans and unsaved drafts are distinct; editing or saving an inactive plan does not activate it. Dated execution snapshots retain the original plan, prescription, stable identities and recorded data through plan edits, activation and exercise reordering.
- Warm-up and working sets share the log but have distinct semantics. Warm-ups do not inflate working-set metrics or attendance. Missing comparable performance is unavailable, not zero.
- Keep original planned dates and actual attendance dates separate. Starting, pausing or finishing without a qualifying working set must not fabricate attendance or performed sets.
- Respect the specified session transition diagram and independent timer rules; preserve recorded work when navigating and pausing.

For file changes, read [data-formats.md](../../../docs/architecture/data-formats.md) before editing exporters or future readers. Preserve UTF-8 JSON raw state and the dictionary, CSV headers/units/quoting and the recovery guide. Document incompatible format-version changes and migration/recovery while keeping old files readable. Export a copy of timer state without pausing live timers. Verify downloads with ordinary JSON/CSV readers, including Unicode, missing values and original plan ownership.

Finish by auditing both directions: every affected requirement has concrete evidence or an explicit deferred status, and every new behavior traces back to authorized scope.
