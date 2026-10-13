# gym assystant — draft specification

Status: revision 4, for exploration. Only a disposable browser mockup is authorized. Android implementation starts after the user decides the scope. Mockup coverage does not mean a production requirement is implemented.

## Purpose

Follow a training program made of multiple training sessions. During each session, prioritize logging repetitions and weight, time execution and recovery, retain machine setup notes, and offer secondary access to an exercise video. Review performance across comparable sessions and see which session is planned today and on future dates.

## Requirements and acceptance criteria

| ID | Requirement | Acceptance criteria |
| --- | --- | --- |
| GYM-001 | Select exercises within a session. | Each session shows its exercises. Selecting an exercise opens its own log; back returns to the correct session. |
| GYM-002 | Secondary in-app exercise video access. | A small button below logging, timers, and setup opens the selected exercise's video panel. No large thumbnail or autoplay. The mockup labels the panel as a non-playable placeholder. Actual video playback is deferred. |
| GYM-003 | Record seat position per exercise occurrence. | Free text persists while navigating during the mockup visit. Separate sessions have separate notes; cross-session carryover is undecided. |
| GYM-004 | Record reps and weight for each set. | Reps are prominent and appear before weight. Reps accept nonnegative integers; kg accepts nonnegative finite numbers. Invalid input restores the last valid value. Values remain associated with the exercise occurrence. Sample values are not prescriptions. |
| GYM-005 | Mark sets done and undone. | Completion counts update in exercise and session views. This remains a proposed production behavior. |
| GYM-006 | Explore a responsive dummy mockup. | Narrow phone and desktop layouts, visible prototype disclosure, keyboard access and focus restoration. Edits and timers are in memory and reset on reload; statistics are labeled fictional history. |
| GYM-007 | Name the app gym assystant. | Browser title and visible branding use this exact spelling. |
| GYM-008 | Center the app on a training program containing sessions. | Home identifies one sample program whose three distinct session templates are accessible through the calendar and upcoming agenda. Each defines ordered exercises, number of sets, and target rep ranges. Selecting a session shows those prescriptions separately from actual logged sets. Train, Plan, and Progress are persistent bottom navigation destinations. Train opens dated workouts; Plan edits their reusable templates. |
| GYM-009 | Prioritize performance logging. | Exercise screen order: back navigation, execution/recovery timers at the top, exercise and prescribed target, one combined warm-up/working-set list with reps/weight controls, machine setup, small video button. Set controls are visible before video access at mobile width. |
| GYM-010 | Show training performance statistics. | A Progress view lets the user select an exercise and metric. Show dated history for best reps at a fixed weight, heaviest weight at exactly 10 reps, and volume load; also show completed working-set count for the displayed period. Values are computed from explicit fictional completed logs, not hardcoded chart labels. No comparable data means a gap, not zero. Details and comparison rules appear below. |
| GYM-011 | Research progression metrics before finalizing analytics. | A linked literature brief distinguishes outcomes, workload, and consistency; cites original research and evidence syntheses; records limitations, formulas, and proposed versus included metrics. Research recommendations do not automatically authorize features. |
| GYM-012 | Time execution and recovery separately. | Execution counts up; recovery counts down from a user-editable whole number of seconds (1–3600; default 90). Each has start/resume, pause, and reset. Recovery stops at zero and announces completion. Timers use elapsed clock time, retain independent state when switching modes/views, and belong to the selected exercise occurrence. Reload resets them. No automatic set completion or automatic transition. |
| GYM-013 | Use an inviting, evidence-informed brand. | Retain the exact name gym assystant. Use the promise “Your pace. Your progress.”, clear next actions, personal progress feedback, and neutral language for rest or unfinished sets. Apply the sunrise-amber, deep-teal, and ivory palette, wordmark, and tone documented in branding-research.md. Warm accents emphasize action, calmer teal surfaces support logging/recovery, and text/control contrast remains readable. No shame, body-transformation promises, streak pressure, or unsupported claims that colors increase exercise adherence. |
| GYM-014 | Track warm-up reps alongside working sets. | Add/remove warm-up sets; edit nonnegative whole-number reps and nonnegative finite kg; mark each done/undone. Show warm-up and working sets together in one list, with a visible type label on every row. Keep their data distinct, retain them when navigating, and exclude warm-ups from working-set completion, records, and all GYM-010 metrics. |
| GYM-015 | Follow Android Material 3 UI conventions. | Use consistent shape tokens, pill-shaped action buttons, joined segmented selectors, rounded cards and dialog, an icon-and-label bottom navigation bar with a selected indicator, and at least 48 × 48 CSS-pixel touch areas for standard controls in the phone mockup. Calendar date cells adapt their width to fit seven columns, retaining at least 48 pixels of height. Keep navigation outside the scrolling content. Real Android delivery must use dp and native Material components. Document official guidance in android-ui.md. |
| GYM-016 | Schedule sessions and browse dated workouts. | Train initially selects today. Selecting a calendar date updates the existing top card with that date, its session (or “No session planned”), and an action opening that dated workout; no popup opens. Previous/next week and Today update the same card. All seven Monday–Sunday dates fit in one row without horizontal scrolling. Upcoming-session taps select that date in the same card. Weekly assignments and individual-date overrides are edited in Plan, not in the execution card. The sample starts with A on Monday, B on Wednesday, C on Friday. Dated logs stay isolated. Future dates say “Preview session”; today/past dates say “View session”. Reload resets edits. |
| GYM-017 | Start, continue, stop, and resume a session. | Each dated session has Ready, Active, or Stopped state. Start session changes Ready to Active and opens the first exercise with unfinished working sets. Continue session keeps Active and reopens the next unfinished exercise; all sets logged allows reviewing the first exercise. Stop session is available on session and exercise screens while Active; it changes to Stopped, pauses both timers for all exercises in that occurrence, and retains sets, warm-ups, and setup notes. Resume session changes Stopped to Active and reopens the next unfinished exercise without clearing data. Stopped sessions cannot run timers until resumed. Resume does not automatically restart timers; timer start remains manual. Future dates retain preview context. State is isolated by date; reload resets it. No session history is saved. |
| GYM-018 | Manage a training plan separately from execution. | Plan edits one sample program: its name; 1–7 named session templates with optional focus descriptions; catalog exercises with add/remove/reorder; working-set counts (1–10) and rep ranges (1–50, min ≤ max); assignments for each weekday; and optional per-date assignments or “Use weekly pattern”. Edits are drafts until Save plan validates and applies them. Session templates require at least one exercise. Plan contains prescriptions and schedule controls, not completed sets, actual weights/reps, timers, or Start/Stop actions. Train contains dated execution. Started/stopped sessions and sessions with recorded entries retain a copy of their original prescription; newly opened untouched previews use the saved plan. Removing a template clears its weekly/date assignments but retains existing execution records and access to them. State resets on reload. |

## Information model and navigation

`Training plan → reusable session template → dated execution snapshot → actual working/warm-up sets`

Plan prescriptions and execution entries have separate data objects. Stable session IDs connect schedule assignments to templates; renaming or deleting another template does not change those IDs. Execution copies the program name, session name, ordered prescriptions, and plan revision. Edits to actual sets, seat notes, or timers never change the plan.

An untouched Ready preview can be refreshed from the saved plan when reopened. Starting it or recording any entries preserves its snapshot for subsequent visits. The Train calendar prioritizes an existing recorded/started workout for that date even if its template is later changed or removed. Historical statistics still use fictional immutable data; saving real history remains deferred.

Navigation: Train/calendar → dated Session → Exercise; Plan → template editor; Progress → statistics. The sample catalog remains small and illustrative. Add/remove/reorder actions concern planned exercises; actual session warm-ups retain their existing controls. Multiple plans, importing, automatic rotations, and persistent history are not included yet.

## GYM-010: metric definitions

| Metric | Definition | Interpretation |
| --- | --- | --- |
| Reps at reference weight | Highest completed working-set reps at the selected exercise's displayed reference kg, per dated occurrence. | Repetition performance at comparable load. Reference weights are fixed sample assumptions, not recommended loads. |
| Weight at 10 reps | Highest completed working-set kg with exactly 10 recorded reps, per dated occurrence. | Load performance for a fixed repetition task; not a 10RM claim unless maximal effort was actually established. |
| Volume load | Sum of `kg × reps` over completed working sets of that exercise in each dated occurrence. Unit: kg·reps. | Workload proxy, not mechanical work, strength, or muscle growth. |
| Completed working sets | Count of completed, non-warmup sets with positive whole-number reps and valid nonnegative weight for the exercise over the displayed period. | Training exposure, not a direct outcome measure. |

Statistics include only completed working sets with positive whole-number reps and finite nonnegative weights; zero-rep entries are placeholders, not performance observations. Compare only identical exercise, machine identity, setup, unit/load convention, range-of-motion protocol, and effort context. The fictional history uses the same setup and a stated 2-reps-in-reserve context. Actual effort collection is not implemented; without it, future real comparisons must be labeled unadjusted. A changed setup must not silently merge histories. Missing baselines or zero baselines do not produce percentage gains. Charts show actual dates, units, raw values, and gaps. No aggregate strength score across unrelated machines.

The Progress view explicitly excludes the current editable session: four past fictional occurrences are used to explore the design. Editing today's samples must not rewrite that history. Saving completed sessions to history is a future implementation decision.

## GYM-012: timer assumptions

Manual stopwatch and countdown controls are mockup choices, pending exploration. Recovery duration is editable only while paused. Changing it resets that countdown. Reset stops the selected timer and restores zero (execution) or the chosen duration (recovery). Resuming a finished recovery starts a fresh interval. No sound, vibration, background service, notifications, or timing history is included. Browser rendering can pause in the background; elapsed time is recalculated on return. Native Android background/lock-screen behavior remains undecided.

## GYM-016: calendar assumptions

Today follows the browser/device local date. The week starts Monday. Civil-date calculations preserve calendar dates across daylight-saving changes and month/year boundaries. The weekly sample is an exploration assumption, not a recommended training frequency. Saving a date assignment in Plan changes only that date; there is no drag/drop, automatic rescheduling, scheduling notification, or integration with an external calendar.

The week is a seven-column grid with all dates visible. On narrow previews, date cells may be less than 48 CSS pixels wide to satisfy the requested fit; they retain generous height and never overlap. Standard controls keep their existing touch targets. The agenda considers future dates strictly after today and shows up to three planned occurrences within the next 28 days. If there are none, show a clear empty state. Past plans are not declared completed; scheduling and actual performance are different concepts. Finishing working sets does not persist history or automatically reschedule the plan.

## Deferred decisions and exclusions

- Multiple programs, importing, automatic rotations, and adding/removing working sets during execution. Plan template/schedule editing and execution warm-ups are now included.
- Primary training outcome: strength, hypertrophy, or general progress; optional RIR logging and estimated 1RM are research proposals only.
- Video source, licensing, language, offline access, and machine-specific variations.
- Seat carryover, persistence, backups, accounts, unit preferences, and per-side/total weight conventions.
- History editing/persistence, completion flow, a plan revision history UI, and Android technology. Snapshot revision tracking is included to protect execution data.
- Timer auto-transitions, per-set/program rest prescriptions, background reliability, alerts, and storing execution/rest duration.

Statistics, timers, calendar planning, and a separate plan manager are now in scope, superseding earlier exclusions. Social features, subscriptions, coaching, and notifications remain outside scope. No load-increase advice or personalized training prescription is implemented.

## Change and traceability rules

Write or update requirement IDs before changing features. Code anchors, `data-req` attributes, and [traceability.md](traceability.md) provide both mapping directions and verification. Tests check meaningful behavior, not merely presence of IDs. Traceability supports review; it cannot guarantee correctness by itself. Android delivery remains distinct from mockup delivery.
