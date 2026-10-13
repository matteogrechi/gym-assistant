# Bidirectional traceability

Revision 7. All Android delivery is **not started**. “Covered” below means the disposable mockup meets the acceptance criteria in [spec.md](spec.md), not that the Android product is implemented. Search a requirement ID in source comments or `data-req` attributes to trace code back to scope.

| Requirement | Source anchors / artifact | Verification | Mockup state |
| --- | --- | --- | --- |
| GYM-001 | `app.js`: `renderSession`, `selectExercise`, back handlers; `index.html`: session/exercise | Browser: session/exercise selection, return to matching session | Covered |
| GYM-002 | `app.js`: `openVideo`; `index.html`: `watchVideo`, `videoDialog`; CSS: `.video-link` | Browser: button after set log, exercise-specific title, Escape dismissal | Secondary placeholder; actual playback deferred |
| GYM-003 | `app.js`: `seatInput`, `renderExercise`; `app.js`: `getSession`; `plan.js`: `openOccurrence` | Browser: edits retained; other exercises/sessions isolated | Covered |
| GYM-004 | `app.js`: `setRow`, `updateSet`; `index.html`: `sets`; CSS: rep emphasis | Browser: rep/weight edits, negative/fractional/empty rejection, navigation retention | Covered |
| GYM-005 | `app.js`: `toggleSet`, `completed`, `renderSession`, `renderSets` | Browser: working-set count changes, warm-up completion does not inflate it | Covered; production behavior proposed |
| GYM-006 | `index.html`: shell/disclosures/dialog; `style.css`; `app.js`: `showView` | Browser: 320/390/430px, focus/navigation, reload reset, no page errors | Covered |
| GYM-007 | `index.html`: browser title, wordmarks | Browser: title and visible branding | Covered |
| GYM-008 | `data.js`: program/session/prescription; `app.js`: `renderProgram`, `openSession`; `index.html`: program/bottom navigation | Browser: three session templates, distinct ordering and rep targets | Covered; template editing in GYM-018 |
| GYM-009 | `index.html`: exercise order; CSS: set widths/font sizes | Browser: reps first, set log before secondary video; screenshot review confirms timer at the top | Covered |
| GYM-010 | `metrics.js`: `series`, `countSets`; `data.js`: historical records/context; `app.js`: `renderProgress`; `index.html`: progress | Logic: exact expected metrics, comparison filters, gaps, warm-up exclusion; browser: selectors/current edits excluded | Fictional historical statistics covered; real persistence deferred |
| GYM-011 | [progression-research.md](progression-research.md) | Literature review: eight original studies/reviews with limitations and proposed/included metrics | Research delivered |
| GYM-012 | `timers.js`: timer state/actions; `app.js`: `renderTimer`, interval/actions; `index.html`: timer panel | Logic: pause/resume/reset, expiry/background catch-up, independence, bounds; browser: both timer modes/completion | Covered; native background/alerts deferred |
| GYM-013 | [branding-research.md](branding-research.md); wordmarks/copy in `index.html`, theme in `style.css`, timer copy in `app.js` | Five exercise/motivation sources plus three color/emotion sources; screenshot/copy and color-contrast review | Brand direction delivered and applied |
| GYM-014 | `app.js`: `setRow`, `renderSets`, add/remove/update/toggle warmups; CSS: labeled warm-up rows; `metrics.js`: exclusion | Browser: shared list, labels, retained reps/weight, add/remove, count isolation; logic: warmups excluded | Covered |
| GYM-015 | [android-ui.md](android-ui.md); CSS: `.md-button`, emphasis variants, `.md-icon-button`, `.md-segment`, state layers, target/nav rules; `index.html`: button families, segmented checks, bottom nav; `app.js`: `buttonIcon`, generated actions, `showView` | Official Android button/icon/segmented/accessibility sources; screenshot review: emphasis, consistent labels/icons, selected checks, state layers; browser: navigation stays at bottom, interactive targets ≥48px | Material-style browser approximation; native UI deferred |
| GYM-016 | `calendar.js`: `today`, `week`, `plan`, `upcoming`; `data.js`: sample schedule; `app.js`: `renderCalendar`, `getSession`, date/open handlers; `index.html`: `sessionCard`, week/agenda; `app.js`: Plan scheduling controls | Logic: civil dates, week boundaries, one-date overrides, upcoming/empty state; browser: date taps update the top card without a popup; all seven dates fit; separate Plan assignments and isolated dated logs | Calendar covered; schedule editing in GYM-018; persistence deferred |
| GYM-017 | `app.js`: `renderSession`, `startSession` handler, `stopSession`, per-date lifecycle state; `index.html`: session start action | Browser: Start/Continue select unfinished exercise; Stop pauses all occurrence timers and preserves entries; Resume retains them; stopped timers are disabled; dates and reload isolate/reset state | Covered in mockup |
| GYM-018 | `plan.js`: `validate`, `apply`, `removeSession`, `openOccurrence`; `data.js`: prescribed templates; `app.js`: `renderPlan`, `renderTemplate`, `savePlan`, draft/schedule handlers, `plannedForDate`; `index.html`: Plan/Plan session views and Plan navigation | Logic/browser: draft validation, CRUD/reordering, schedule assignments, stable IDs, new/untouched previews reflect saves, recorded sessions survive edits/removal | Covered in mockup; multiple plans and persistence deferred |
| GYM-019 | `plan.js`: `moveExercise`, `locked`; `app.js`: `renderSession`, exercise-list move handler, Start/Continue selection; `index.html`: execution-order help/status; CSS: `.workout-order-controls` | Logic/browser: data identity and running timer retention, plan/date isolation, boundary controls, adjusted Ready snapshots survive reopening/plan saves, Start/Continue/Resume follow order, 320/390/430px targets | Covered in mockup; persistence deferred |
| GYM-020 | `attendance.js`: `planned`, `observe`, `observations`, `pending`, `summarize`; `data.js`: `attendanceHistory`; `app.js`: past-plan capture, `getSession`, `plannedForDate`, pending selection, set-log observation, `renderAttendance`, date guard; `index.html`: pending list, planned/actual status, attendance panel; CSS: attendance card | Logic/browser: 28-day past cohort, actual date/delay, once-per-occurrence visits, partial sets, warm-up/Start/preview exclusion, zero-plan N/A, pending catch-up, plan-removal protection, unchanged strength history, date validation, reload reset | Covered in mockup; cross-reload persistence deferred |

Source filenames above are under `mockup/`. Meaningful automated checks live in [logic.test.cjs](../tests/logic.test.cjs) and [browser.cjs](../tests/browser.cjs). `package.json` and its lockfile support those checks only; the mockup itself has no build or runtime package dependency.

## Reverse inventory

| Code/UI area | Requirements |
| --- | --- |
| Plan templates, calendar cards, targets, ordered exercises, back navigation | GYM-001, GYM-008 |
| Video text button and placeholder dialog | GYM-002, GYM-006 |
| Setup input and per-occurrence note state | GYM-003 |
| Shared set list, prominent reps, kg validation, done controls/counts | GYM-004, GYM-005, GYM-009 |
| Warm-up labels/tint, add/remove controls, separate data semantics | GYM-014 |
| Metric selector, fictional histories, chart, counts, context/gaps | GYM-010 |
| Execution/recovery selectors, clock state, duration, start/pause/reset/completion | GYM-012 |
| Wordmark, promise, copy, palette and typography | GYM-007, GYM-013 |
| Responsive shell, disclosures, keyboard focus and dialog dismissal | GYM-006 |
| Shared button families, SVG icons, state layers, selected segment checks, bottom navigation, shape tokens, target dimensions | GYM-008, GYM-010, GYM-015 |
| Selected-date card, fitted week, Plan date assignment and upcoming agenda; per-date session/timer state | GYM-016 |
| Session Start/Continue/Stop/Resume actions and isolated lifecycle state | GYM-017 |
| Plan drafts, session/exercise CRUD and order, working-set/rep prescriptions, weekly/date assignments, immutable execution copies | GYM-018 |
| Dated workout permutation, retained exercise identity and selection, order announcement | GYM-019 |
| Past obligations, pending catch-up, actual attendance dates, visit/delay metrics, protected denominators | GYM-020 |
| Research briefs and evidence boundaries | GYM-011, GYM-013, GYM-015 |

No e1RM, RIR-entry, multiple plans, persistent history, full-prescription adherence, notifications, streaks, or recommendations have been added. Research proposals do not count as UI requirements.

## Verification record

Revision 1, 2026-10-12: the original session-only mockup passed syntax and browser checks. That record does not validate the revised program workflow.

Revision 2, 2026-10-12: eight logic tests passed for numerical metrics, matched contexts, missing data, warm-up exclusion, and timer semantics. The Chromium browser check passed for program/session navigation, working and warm-up logging, validation, timers, secondary video, progress selection, state isolation, 320/390/430px layouts, persistent bottom navigation, minimum touch targets, and reload reset. Program, exercise, progress, and desktop screenshots were reviewed. These checks validate the browser mockup only.

Palette update, 2026-10-12: GYM-013 now uses sunrise amber, deep teal, and ivory, informed by the 2025 systematic review and two supporting color studies/reviews. Checked text pairs exceed 4.5:1 and essential outline/chart pairs exceed 3:1. Existing Chromium checks passed; all four preview screenshots were refreshed. No functional scope was added.

Revision 3, 2026-10-12: 12 logic tests passed, including week/year/leap-day/DST calculations, date overrides, and upcoming/empty agenda rules. The Chromium check passed for today/future planning, dialog assignment, date-isolated logs, Start/Continue/Stop/Resume, pausing all exercise timers, retained entries, stopped timer controls, navigation, and reload reset. Mobile layouts and touch targets were checked; program, session, exercise, progress, and desktop previews were refreshed and reviewed.

Calendar selection update, 2026-10-12: Chromium checks passed for direct top-card updates without a calendar popup, selected-date session navigation, inline assignment, empty days, Today/week/agenda navigation, and a seven-day row fully visible without scrolling at 320/360/390/430px. Standard controls retain 48px targets; narrow calendar cells follow the documented width exception. Previews were refreshed and reviewed.

Revision 4, 2026-10-13: 16 logic tests passed, including plan/actual separation, stable identities, draft validation, preview refresh, and retention of recorded or stopped snapshots after template removal. Chromium checks passed for session/exercise editing and ordering, weekly/date assignments, Train/Plan/Progress navigation, save validation, updated future workouts, retained active logs, deletion cleanup, and narrow Plan layouts (320/390/430px) with standard touch targets. Plan and template-editor previews were added; existing previews were refreshed and reviewed.

Revision 5, 2026-10-13: 18 logic tests passed. Chromium checks passed for dated workout permutation, preserved working/warm-up logs and seat notes, running timer continuity, Start/Continue/Resume using the selected order, reordering while stopped, disabled boundary controls, untouched plan/other dates, and reordered Ready previews surviving navigation and plan saves. Session controls retain 48px targets at 320/390/430px. The reordered-session preview was added and reviewed; existing previews were refreshed.

Revision 6, 2026-10-13: reviewed official Android button, icon-button, segmented-button, and accessibility guidance. Applied shared button families, 14px centered labels, consistent SVG action icons, selected-segment checks, and hover/press/focus/disabled state layers. All 18 logic checks and the Chromium interaction suite passed. Exercise, session, and Plan controls fit 320/390/430px previews and retain 48px targets; the calendar retains its documented width exception. Refreshed previews; exercise, reordered-session, and template-editor screenshots were visually reviewed.

Revision 7, 2026-10-13: 22 logic checks and the Chromium suite passed. Verified original planned dates and actual first-logged dates, late/early/on-time classification, 28-day cohort boundaries, N/A values, civil-day delay across DST, today entering the cohort only when past, partial attendance, future/Start/warm-up exclusion, once-per-occurrence recording, catch-up removing a pending item, past-date validation, pending prescription access after template deletion, stable denominators after plan removal, unchanged exercise metrics, and reload reset. Attendance layouts fit 320/390/430px. Added and reviewed the attendance preview; refreshed existing previews and reviewed Train home.
