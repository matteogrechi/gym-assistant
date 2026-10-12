# Bidirectional traceability

Revision 2. All Android delivery is **not started**. “Covered” below means the disposable mockup meets the acceptance criteria in [spec.md](spec.md), not that the Android product is implemented. Search a requirement ID in source comments or `data-req` attributes to trace code back to scope.

| Requirement | Source anchors / artifact | Verification | Mockup state |
| --- | --- | --- | --- |
| GYM-001 | `app.js`: `renderSession`, `selectExercise`, back handlers; `index.html`: session/exercise | Browser: session/exercise selection, return to matching session | Covered |
| GYM-002 | `app.js`: `openVideo`; `index.html`: `watchVideo`, `videoDialog`; CSS: `.video-link` | Browser: button after set log, exercise-specific title, Escape dismissal | Secondary placeholder; actual playback deferred |
| GYM-003 | `app.js`: `seatInput`, `renderExercise`; `data.js`: exercise instances | Browser: edits retained; other exercises/sessions isolated | Covered |
| GYM-004 | `app.js`: `setRow`, `updateSet`; `index.html`: `sets`; CSS: rep emphasis | Browser: rep/weight edits, negative/fractional/empty rejection, navigation retention | Covered |
| GYM-005 | `app.js`: `toggleSet`, `completed`, `renderSession`, `renderSets` | Browser: working-set count changes, warm-up completion does not inflate it | Covered; production behavior proposed |
| GYM-006 | `index.html`: shell/disclosures/dialog; `style.css`; `app.js`: `showView` | Browser: 320/390/430px, focus/navigation, reload reset, no page errors | Covered |
| GYM-007 | `index.html`: browser title, wordmarks | Browser: title and visible branding | Covered |
| GYM-008 | `data.js`: program/session/prescription; `app.js`: `renderProgram`, session selection; `index.html`: program/bottom navigation | Browser: three session templates, distinct ordering and rep targets | Covered; editing deferred |
| GYM-009 | `index.html`: exercise order; CSS: set widths/font sizes | Browser: reps first, set log before secondary video; screenshot review confirms timer at the top | Covered |
| GYM-010 | `metrics.js`: `series`, `countSets`; `data.js`: historical records/context; `app.js`: `renderProgress`; `index.html`: progress | Logic: exact expected metrics, comparison filters, gaps, warm-up exclusion; browser: selectors/current edits excluded | Fictional historical statistics covered; real persistence deferred |
| GYM-011 | [progression-research.md](progression-research.md) | Literature review: eight original studies/reviews with limitations and proposed/included metrics | Research delivered |
| GYM-012 | `timers.js`: timer state/actions; `app.js`: `renderTimer`, interval/actions; `index.html`: timer panel | Logic: pause/resume/reset, expiry/background catch-up, independence, bounds; browser: both timer modes/completion | Covered; native background/alerts deferred |
| GYM-013 | [branding-research.md](branding-research.md); wordmarks/copy in `index.html`, theme in `style.css`, timer copy in `app.js` | Five exercise/motivation sources plus three color/emotion sources; screenshot/copy and color-contrast review | Brand direction delivered and applied |
| GYM-014 | `app.js`: `setRow`, `renderSets`, add/remove/update/toggle warmups; CSS: labeled warm-up rows; `metrics.js`: exclusion | Browser: shared list, labels, retained reps/weight, add/remove, count isolation; logic: warmups excluded | Covered |
| GYM-015 | [android-ui.md](android-ui.md); CSS theme/target/nav/selector rules; `index.html`: bottom nav; `app.js`: `showView` | Official Android sources; browser: navigation stays at bottom, interactive targets ≥48px | Material-style browser approximation; native UI deferred |

Source filenames above are under `mockup/`. Meaningful automated checks live in [logic.test.cjs](../tests/logic.test.cjs) and [browser.cjs](../tests/browser.cjs). `package.json` and its lockfile support those checks only; the mockup itself has no build or runtime package dependency.

## Reverse inventory

| Code/UI area | Requirements |
| --- | --- |
| Program data, three session cards, targets, ordered exercises, back navigation | GYM-001, GYM-008 |
| Video text button and placeholder dialog | GYM-002, GYM-006 |
| Setup input and per-occurrence note state | GYM-003 |
| Shared set list, prominent reps, kg validation, done controls/counts | GYM-004, GYM-005, GYM-009 |
| Warm-up labels/tint, add/remove controls, separate data semantics | GYM-014 |
| Metric selector, fictional histories, chart, counts, context/gaps | GYM-010 |
| Execution/recovery selectors, clock state, duration, start/pause/reset/completion | GYM-012 |
| Wordmark, promise, copy, palette and typography | GYM-007, GYM-013 |
| Responsive shell, disclosures, keyboard focus and dialog dismissal | GYM-006 |
| Bottom navigation, common shape tokens, action hierarchy, target dimensions | GYM-008, GYM-010, GYM-015 |
| Research briefs and evidence boundaries | GYM-011, GYM-013, GYM-015 |

No e1RM, RIR-entry, adherence, scheduling, history saving, notifications, streaks, program editor, or recommendations have been added. Research proposals do not count as UI requirements.

## Verification record

Revision 1, 2026-10-12: the original session-only mockup passed syntax and browser checks. That record does not validate the revised program workflow.

Revision 2, 2026-10-12: eight logic tests passed for numerical metrics, matched contexts, missing data, warm-up exclusion, and timer semantics. The Chromium browser check passed for program/session navigation, working and warm-up logging, validation, timers, secondary video, progress selection, state isolation, 320/390/430px layouts, persistent bottom navigation, minimum touch targets, and reload reset. Program, exercise, progress, and desktop screenshots were reviewed. These checks validate the browser mockup only.

Palette update, 2026-10-12: GYM-013 now uses sunrise amber, deep teal, and ivory, informed by the 2025 systematic review and two supporting color studies/reviews. Checked text pairs exceed 4.5:1 and essential outline/chart pairs exceed 3:1. Existing Chromium checks passed; all four preview screenshots were refreshed. No functional scope was added.
