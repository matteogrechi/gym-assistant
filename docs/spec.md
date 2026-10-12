# gym assystant — draft specification

Status: revision 2, for exploration. Only a disposable browser mockup is authorized. Android implementation starts after the user decides the scope. Mockup coverage does not mean a production requirement is implemented.

## Purpose

Follow a training program made of multiple training sessions. During each session, prioritize logging repetitions and weight, time execution and recovery, retain machine setup notes, and offer secondary access to an exercise video. Review performance across comparable sessions.

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
| GYM-008 | Center the app on a training program containing sessions. | Home shows one named sample program and three distinct session templates. Each defines ordered exercises, number of sets, and target rep ranges. Selecting a session shows those prescriptions separately from actual logged sets. Program and Progress are persistent bottom navigation destinations. No program editor is implied yet. |
| GYM-009 | Prioritize performance logging. | Exercise screen order: exercise and prescribed target, one combined warm-up/working-set list with reps/weight controls, execution/recovery timers, machine setup, small video button. Set controls are visible before video access at mobile width. |
| GYM-010 | Show training performance statistics. | A Progress view lets the user select an exercise and metric. Show dated history for best reps at a fixed weight, heaviest weight at exactly 10 reps, and volume load; also show completed working-set count for the displayed period. Values are computed from explicit fictional completed logs, not hardcoded chart labels. No comparable data means a gap, not zero. Details and comparison rules appear below. |
| GYM-011 | Research progression metrics before finalizing analytics. | A linked literature brief distinguishes outcomes, workload, and consistency; cites original research and evidence syntheses; records limitations, formulas, and proposed versus included metrics. Research recommendations do not automatically authorize features. |
| GYM-012 | Time execution and recovery separately. | Execution counts up; recovery counts down from a user-editable whole number of seconds (1–3600; default 90). Each has start/resume, pause, and reset. Recovery stops at zero and announces completion. Timers use elapsed clock time, retain independent state when switching modes/views, and belong to the selected exercise occurrence. Reload resets them. No automatic set completion or automatic transition. |
| GYM-013 | Use an inviting, evidence-informed brand. | Retain the exact name gym assystant. Use the promise “Your pace. Your progress.”, clear next actions, personal progress feedback, and neutral language for rest or unfinished sets. Apply the palette, wordmark, and tone documented in branding-research.md. No shame, body-transformation promises, streak pressure, or unsupported claims that colors increase exercise adherence. |
| GYM-014 | Track warm-up reps alongside working sets. | Add/remove warm-up sets; edit nonnegative whole-number reps and nonnegative finite kg; mark each done/undone. Show warm-up and working sets together in one list, with a visible type label on every row. Keep their data distinct, retain them when navigating, and exclude warm-ups from working-set completion, records, and all GYM-010 metrics. |
| GYM-015 | Follow Android Material 3 UI conventions. | Use consistent shape tokens, pill-shaped action buttons, joined segmented selectors, rounded cards and dialog, an icon-and-label bottom navigation bar with a selected indicator, and at least 48 × 48 CSS-pixel touch areas in the phone mockup. Keep navigation outside the scrolling content. Real Android delivery must use dp and native Material components. Document official guidance in android-ui.md. |

## Information model and navigation

`Training program → session template → exercise prescription → working sets`

Historical performance belongs to dated session occurrences, not to the reusable template. Historical records must retain the program/template identity and the exercise/machine/setup used at the time. Program editing and versioning UI are deferred.

Mockup navigation: Program → Session → Exercise; top navigation also opens Progress. The example program has sessions A, B, and C. Targets are ranges; entered repetitions are actuals. Session duration estimates, scheduling, streaks, and recommendations are not part of this revision.

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

## Deferred decisions and exclusions

- Program creation/import, editing, scheduling, and adding/removing/reordering exercises or prescribed working sets (warm-up rows are editable).
- Primary training outcome: strength, hypertrophy, or general progress; optional RIR logging and estimated 1RM are research proposals only.
- Video source, licensing, language, offline access, and machine-specific variations.
- Seat carryover, persistence, backups, accounts, unit preferences, and per-side/total weight conventions.
- History editing and persistence, completion flow, plan versioning, and Android technology.
- Timer auto-transitions, per-set/program rest prescriptions, background reliability, alerts, and storing execution/rest duration.

Statistics and timers are now in scope, superseding revision 1 exclusions. Social features, subscriptions, coaching, and notifications remain outside scope. No load-increase advice or personalized training prescription is implemented.

## Change and traceability rules

Write or update requirement IDs before changing features. Code anchors, `data-req` attributes, and [traceability.md](traceability.md) provide both mapping directions and verification. Tests check meaningful behavior, not merely presence of IDs. Traceability supports review; it cannot guarantee correctness by itself. Android delivery remains distinct from mockup delivery.
