# Interface review — GYM-021

Review date: 13 October 2026. Browser user walkthrough at 390 × 844 CSS pixels, with a repeatable 12 October sample date. This is an interface inspection, not a usability study with independent participants.

The reviewer opened the calendar, started a session, selected execution/recovery, opened the video, paused/resumed, logged a working set, finished and reviewed a log, opened Plan and its template editor, and selected progress metrics. Actual screenshots and visible content were inspected before changing the specification.

| Page / task | Observed friction | Specified change |
| --- | --- | --- |
| Train: choose a dated workout | Calendar legend explained labels already present; agenda repeated each date; pending copy repeated its heading. | Keep banner/week; concise pending/upcoming rows and dates once. |
| Session: start, select an available machine, pause/resume | Program name and logging/reordering instructions repeated existing context. Ready session took 1.3 scroll areas; Paused took 1.4. | State/date/actions, one completion total, exercise targets and order controls. |
| Finished session: review | Completion total appeared in status and section header; attendance repeated the same planned/logged date; instructions still described logging. | State and total once; only different actual dates; review list without editing instructions. |
| Exercise: log reps and weight | Large lifecycle buttons wrapped onto a second row. Execution exposed only one full set row; recovery exposed none before scrolling. | Compact header/timer, inline warm-up action, no category/tutorial copy, keep inputs large. |
| Warm-up log: edit or remove | A full-width “Remove warm-up N” row followed every warm-up. | Trash icon in the corresponding warm-up row, with a full accessible name. |
| Finished exercise: read data | Disabled Start/Reset/Add/remove controls and editing instructions remained visible. | Hide unavailable actions; keep values and timer-mode review. |
| Video: check the movement | Several paragraphs explained a single non-playable placeholder. | Exercise name and one truthful demo-placeholder message. |
| Plan: manage reusable sessions | Intro/draft/scheduling prose dominated a 2.2-scroll-area page. Seven weekday selectors and a date override were always open. | Name/sessions/Save first; expandable weekly/date settings; concise stateful feedback. |
| Template: adjust targets | Name repeated in heading and input; focus and every prescription editor were always open (2.1 scroll areas). | Name once; optional focus disclosure; target summaries and first editor open. |
| Progress: compare performance and attendance | Attendance/explanation preceded the chart; both Reps and Volume pages spanned 2.0 scroll areas. | Chart first; compact attendance; definitions, context, counts, and limitations on demand. |

All changes must retain the original behaviors and numeric definitions. A missing performance observation stays a gap. Unlogged attendance stays unknown. Demo disclosures stay visible. Shapes and touch targets follow GYM-015. The after-change walkthrough and regression evidence will be recorded below.

## After-change walkthrough

Repeated the same 12 page/state captures and inspected every screen. All three working-set rows now fit at 390 × 844 in execution **and** recovery mode; the earlier recovery screen showed none. Reps/weight inputs remain 56px high, completion/removal controls remain 48px, and video remains below the log and seat notes.

| Page | Before: scroll-area heights | After: scroll-area heights |
| --- | --- | --- |
| Train | 1.8 | 1.5 |
| Ready session | 1.3 | 1.0 |
| Paused session | 1.4 | 1.0 |
| Finished session | 1.1 | 1.0 |
| Execution log | 1.6 | 1.1 |
| Recovery log | 1.7 | 1.1 |
| Finished log | 1.6 | 1.1 |
| Plan | 2.2 | 1.0 |
| Template editor | 2.1 | 1.3 |
| Reps / Volume progress | 2.0 | 1.1 |

Ratios are content scroll height divided by the available scrolling viewport, rounded to one decimal. They describe these fictional fixtures and closed optional disclosures, not arbitrary programs or font scales. Expanded settings/definitions naturally need more space.

Save is now next to the heading on both Plan editors, with draft/error feedback immediately below. Session name is edited once, and prescription summaries update without resetting the field cursor. Native disclosure summaries support keyboard activation and 48px targets; validation opens a collapsed invalid field before focusing it. Metric formulas, comparison context, working-set count, and attendance definitions remain accessible. Warm-up, order, lifecycle, calendar, timers, saved-plan isolation, and all numeric metrics passed the existing regression flows.

The video dialog keeps the exercise name, a clear close control, and one non-playable demo statement. Finished review hides actions that cannot be used, retaining values and timer-mode review. Reordering announcements and routine timer status remain available to screen readers.

Verification: all 25 logic tests and the Chromium user-flow suite passed, including 320/390/430px controls/disclosures, both timer modes, full visible working rows, first-view chart, keyboard metric disclosure, collapsed-field validation, optional focus editing, and finished-action hiding. The [specification](../product/spec.md) and [traceability](../product/traceability.md) record the retained feature scope.

## Revision 10: Settings review

Settings groups language/appearance above the save location and two primary file actions. File details and the recovery-guide download expand on demand; folder selection is explicitly a preview. At 390px, both downloads and the file disclosure fit above the persistent four-destination navigation bar. English/Italian controls and navigation fit at 320/390/430px with 48px targets. Native full translation and filesystem behavior remain deferred.

Reviewed [light Settings](previews/settings.png), [dark Settings](previews/settings-dark.png), [dark Train](previews/program-dark.png), [dark logging](previews/exercise-dark.png), and [dark Progress](previews/progress-dark.png). Earlier screenshots are retained as revision-9 review artifacts. Theme changes preserve entries and timers; the file feedback says the file was handed to the browser, without claiming it was written to the preview folder. Full JSON, CSV and recovery-guide downloads were opened and checked independently.
