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

Reviewed [light Settings](previews/settings.png), [dark Settings](previews/settings-dark.png), [dark Train](previews/program-dark.png), [dark logging](previews/exercise-dark.png), and [dark Progress](previews/progress-dark.png). Linked previews show the latest reviewed screens; earlier narrative records describe their original revisions. Theme changes preserve entries and timers; the file feedback says the file was handed to the browser, without claiming it was written to the preview folder. Full JSON, CSV and recovery-guide downloads were opened and checked independently.

## Revision 12: design consistency walkthrough

Reviewed 50 screen/state captures: both light and dark themes, every destination and lifecycle state, warm-up/recovery/finished logs, schedule and template details, all progress metrics, exports/folder/overflow, plus 320px and 430px Settings/template layouts. No sub-48px standard controls were found; calendar dates retain their documented fitted-week exception. Revision 11 moves Settings to the top-right overflow and keeps three bottom destinations.

| Finding | Correction specified before mockup changes |
| --- | --- |
| Session/template Back used a separate text row; exercise/Settings Back used a circular icon. | Shared compact secondary headings and 48px Back icons with destination labels. |
| Fresh/saved plans showed the same bright Save action as unsaved drafts. | Disable Save until a change; keep validation and draft feedback. |
| Finished logs retained round completion buttons and field outlines that implied editing. | Static check/dash status and readable values without editable outlines. |
| Unavailable video showed a large play symbol. | Neutral video-frame placeholder; retain the unavailable message. |
| Number fields could display browser-specific tiny spinner arrows. | Hide spinner decoration while retaining numeric input, validation and arrow-key editing. |
| Clearing the optional focus could leave a leading separator before the session date. | Join only nonempty subtitle fragments. |

Different action styles remain intentional: filled primary actions, tonal Add, outlined Pause/reorder, text secondary actions, and standard delete icons. Workout ordering stays vertical to preserve exercise-name space; template ordering stays horizontal within its card. Calendar cells remain shaped as dates rather than action pills. These differences serve their context and are not defects.

Representative before/after evidence: [mixed Back row before](history/design-before-back.png) → [compact Back heading after](previews/design-session.png), and [finished controls before](history/design-before-finished-log.png) → [static finished review after](previews/design-finished-log.png). The remaining preview images were refreshed to show three bottom destinations and the overflow menu.

Verification: all 29 logic checks and the Chromium regression suite passed, including overflow keyboard/outside/Escape/Tab dismissal, exact screen/scroll return, retained drafts/logs/timers, disabled/dirty/saved Save states, clean optional-focus subtitle, ArrowUp/ArrowDown entry, and static complete/not-logged finished statuses. The repeated 50-capture walkthrough found no standard control below 48px in either theme; narrow calendar cells retain their documented exception. Native Android implementation remains not started.

## Revision 13: larger expand/collapse controls

The user identified the native disclosure triangles as too small. Replaced them across Plan schedule/date/focus, exercise prescriptions, comparison/attendance details, and Settings file details with 24px chevrons in 48px circular indicators at the right of a full-width, minimum 56px row. Labels are 14px and wrap; the whole row toggles. The chevron points up when open. The circle is part of the native summary, not a nested button, preserving keyboard and expanded-state semantics. Language changes update only the label so the indicator remains present.

The browser regression suite passed, including disclosure keyboard activation, collapsed-field validation, translated/narrow Settings, and existing workflows. The repeated light/dark walkthrough checked 50 screen/state captures, including 320/430px layouts and shared row/icon dimensions. [Plan disclosures](previews/plan.png), [narrow exercise prescriptions](previews/disclosures-template.png), and [dark Progress disclosures](previews/progress-dark.png) show the enlarged controls.

## Revision 14: multiple named plans

Plan now opens a [program library](previews/plans.png), where names identify real navigation destinations and one saved program is marked Active. Use plan is a separate outlined action; Add plan is tonal. Opening a program leads to its existing editor with Back to plans. After adding a program, the [three-plan view](previews/plans-multiple.png) keeps only one Active badge. Each editor has an independent retained draft, and activation requires saved changes.

Verified switching with original workout reps/weights, warm-ups, setup, running timers and reordered future snapshots; inactive edits/saves do not activate a program. Old pending workouts show their original plan and retain attendance dates/denominators, including after a civil-date rollover. The library/editor controls fit 320/390/430px with 48px standard targets. Version-2 JSON retains three saved plans and two independent drafts in the exercised export; standard Python also reads the CSV program IDs and unchanged historical cohort. Reload returns to the two fictional defaults. Native Android remains not started.

## Revision 15: Spanish language preview

Added Español to Settings and the native language scope. The mockup translates Settings, folder preview, overflow labels, Back and bottom navigation; other demo screens retain their explicit English-preview boundary. Checked manual Spanish and system es-ES/es-MX, 320/390/430px controls, retained workout entries/timers, untouched user-authored plan/draft names, disclosure indicators and JSON language preference. [Spanish Settings](previews/settings-spanish.png) shows the preview.

## Revision 16: dialogs stay inside the phone

Video and folder dialogs previously lived outside the phone and were centered over the browser presentation. Each is now a phone-owned native modal with a clipped, phone-sized scrim and a centered card. The desktop introduction remains undimmed. Cards retain at least 16px display margins and scroll internally on short displays; native modal blocking/Escape and close focus restoration remain, with explicit Tab wrapping for the single-action video dialog. Resize and actual page scrolling preserve frame alignment.

Reviewed [desktop video](previews/video-desktop-contained.png), [desktop folder preview](previews/folder-desktop-contained.png), [phone folder preview](previews/folder-contained.png), and [dark folder preview](previews/folder-contained-dark.png). Browser checks passed for phone/card bounds, resizing between 320px/390px and desktop, actual page scrolling, a 320 × 260 short viewport, internal scrolling to actions, native modal state, Tab/Shift+Tab, Escape/button/scrim dismissal, folder cancellation, trigger focus and the existing full app workflows. The top-right overflow remains inside the header. Android implementation remains deferred.

## Revision 17: view references, planning safeguards and current previews

The mockup now identifies every screen, dialog and overflow menu using the strip above the phone. The [view catalog](../product/spec.md#gym-006-mockup-only-view-references) assigns VIEW-001 through VIEW-012; app state and exports do not contain these presentation references. The phone remains the only dimmed region during a dialog.

Saved programs open in a [read-only plan preview](previews/plan.png) and [read-only session template](previews/plan-session-readonly.png). Edit explicitly unlocks the [plan controls](previews/plan-edit.png) or [exercise prescriptions](previews/plan-session.png); Save returns to read-only. Switching the active program requires [a named confirmation](previews/activate-plan.png), so a single Use plan tap cannot change the calendar. Cancel/Escape retain the current program, drafts and workouts.

[Workout sorting](previews/session-reordered.png) and template sorting use 48px drag grips, replacing earlier/later buttons. Mouse/touch dragging shows an insertion marker and supports internal autoscroll; Escape or an interrupted gesture cancels. Keyboard users reorder with ArrowUp/ArrowDown on the same handle. Prescriptions and expanded cards stay attached to their exercises. Recovery seconds are edited in the plan prescription; the [workout timer](previews/recovery.png) displays the preserved target read-only.

Repeated the walkthrough across 67 light/dark page/state captures, including narrow templates/settings and desktop dialog presentations. The browser suite passed for the actual drag gestures, keyboard ordering, cancellations, autoscroll, preserved logs/timers/drafts, recovery validation and snapshot boundaries, read-only/explicit editing, activation confirmation, all view references and existing workflows. No standard interactive target measured below 48px; the fitted calendar keeps its documented exception.

All 34 images under `previews/` were freshly captured from this design and visually inspected in five galleries. Their reference strips, drag handles, planned recovery targets, three bottom destinations, themes and popup bounds match the current mockup. Historical before images remain under `history/`, outside the current-preview collection. This record describes revision 17; later reviews refresh affected linked previews. Android implementation remains not started.

## Revision 18: fixed selected-day card in VIEW-001

The selected-day card keeps a 224px height and responsive width on training days and days without a session. Its grid reserves the real action's 48px row even when the empty state has no button. Selecting dates no longer moves the calendar. Names and focus text can occupy two visible lines, with full text retained in the DOM for accessibility, without pushing the action out of the card.

Refreshed and visually compared [a training day](previews/program.png), [a day without a session](previews/program-rest-day.png), their [dark training](previews/program-dark.png)/[dark empty](previews/program-rest-day-dark.png) states and the [desktop layout](previews/desktop.png). The browser checks compare card dimensions and calendar position across states and verify long saved names/focus, action containment, both themes and 320/390/430px/desktop layouts. No placeholder action or new behavior was added to empty days.

## Revision 19: readable and inspectable plots in VIEW-007

The former 80px bars had small labels, no labelled vertical scale and no way to inspect a session. Replaced them with a 180px dated plot, 12px axis/date labels, a readable metric heading and a full-date/value readout. Actual civil-date spacing determines horizontal positions. The scale starts at zero and missing observations break the connecting line. No fitted trend, new performance metric or invented measurement is added.

The [default plot](previews/progress.png), [weight plot](previews/progress-weight.png), [workload plot](previews/volume.png) and [dark plot](previews/progress-dark.png) retain the existing metric definitions. Tap a point to [inspect an earlier session](previews/progress-selected-session.png); its selected ring, pressed semantics and header identify the exact value/date. Left/right keys move between observations; Enter/Space use native activation. Switching metrics retains the selected date. The entire plot remains visible on entry at 390×844.

Each point has a 48px target. Dense dates pan inside the chart with a swipe hint and a fixed visible value axis, preserving date spacing without overlapping targets or horizontal page overflow. [Unavailable observations](previews/progress-gap.png) remain gaps; [empty history](previews/progress-empty.png) shows a clear message. These two captures use isolated fictional fixtures in the capture browser, not changes to the repository's default history. The [attendance panel](previews/attendance.png) retains its separate semantics and values.

All 35 logic checks and the Chromium suite passed, including irregular-date geometry, zero versus missing data, line breaks, empty/single series, touch/keyboard point inspection, selected dates across metrics, decimal values, dense-date panning, 48px non-overlapping targets, light/dark 320/390/430px layouts and unchanged raw history/attendance/exports. Visually inspected and refreshed the eight affected previews. Android implementation remains not started.

## Revision 20: exercise name beside Back in VIEW-003

Moved the exercise name into the shared heading beside the 48px Back arrow, before the timer. The separate heading below the timer is removed. Session actions sit below the heading; Ready omits that row entirely. Long names wrap within the phone width without shrinking the Back target.

Reviewed [Ready](previews/exercise-ready.png), [active logging](previews/exercise.png), [dark logging](previews/exercise-dark.png), [recovery](previews/recovery.png) and [finished review](previews/finished-log.png), plus paused and 320/430px captures. Refreshed the seven affected existing previews, including the [desktop video overlay](previews/video-desktop-contained.png), and added the Ready preview. The Chromium suite checks heading alignment/order, long-name containment, Ready/active action-row visibility, touch targets and existing session workflows. Three working rows remain visible in both timer modes at 390×844.

## Revision 21: deliberate activation from plan detail

Removed the adjacent Use plan buttons from [VIEW-004](previews/plans.png). Every library row now opens a read-only plan; it cannot activate one. Switching requires opening the [inactive plan](previews/plan-inactive.png), choosing the outlined Make active plan action below its content and confirming. Editing and unsaved new plans omit activation. Dirty saved drafts disable the action and explain that Save is required.

The [confirmation](previews/activate-plan.png) names the selected and current plans and retains its schedule explanation. Cancel receives initial focus; opening then pressing Enter cancels. Cancel, Escape and scrim dismissal restore the detail trigger and leave the program unchanged. Confirmation keeps the detail view open with Active plan status; Back shows the updated single Active badge in the [three-plan library](previews/plans-multiple.png).

Reviewed light/dark library, detail and dialog captures and 320/390/430px detail layouts; existing phone-contained short/desktop dialog checks pass. Refreshed four affected existing previews and added inactive-plan, dark-library and dark-confirmation examples. All 35 logic checks and the Chromium workflow suite passed, covering dirty draft guards, default keyboard cancellation, focus return, explicit switching and retained workouts, order, running timers, drafts, attendance and exports. Native implementation remains not started.

## Revision 22: consistent dropdown fields and menus

Replaced plain browser selector styling with shared outlined fields and 24px chevrons. The [language menu](previews/dropdown-language.png) and [dark language menu](previews/dropdown-language-dark.png) use the app surfaces, rounded outlines/shadows, selected checkmarks and 48px options. Keyboard focus has a visible outline. Read-only plan values retain their plain presentation and cannot open.

The same native-select presentation covers [planning schedules](previews/dropdown-schedule.png), [exercise prescriptions](previews/dropdown-exercise.png), [Progress](previews/dropdown-progress.png) and the [folder preview](previews/dropdown-folder.png). A small placement module constrains menus to the phone, chooses above/below based on available room and repositions on scroll/resize. Options wrap, and long/short-screen menus scroll internally. Existing plan, metric, settings and folder change handlers remain the source of selected state.

The full Chromium suite passed, including real touch option selection, keyboard End/Enter schedule changes, ArrowDown/Escape without changes, outside dismissal, focus return, translated Settings, disabled/read-only schedules, dynamically generated fields, both themes and 320/390/430px/desktop bounds, a 300px folder-dialog viewport and a long wrapping list. Reviewed and refreshed affected Settings/folder/planning/Progress previews and added six open-menu examples. Browsers without customizable-select support keep functional browser-owned pickers; the styled open-menu containment is verified in Chromium. No metric or durable-data behavior changed; Android implementation remains not started.

## Revision 23: explore Progress plots

[VIEW-007](previews/progress.png) now supports dragging across the plot to inspect the nearest recorded session, with a vertical guide and the existing exact date/value readout. Horizontal touch inspection remains separate from vertical page scrolling; cancelled gestures release their state. Mouse dragging the date lane and native touch swiping pan the timeline. Missing values remain unavailable and never become interpolated measurements.

[Zoomed inspection](previews/progress-explored.png) uses 100–400% bounded temporal zoom with 48px Zoom in/out and Reset view controls, plus an accessible zoom status. The value axis stays zero-based. Reset restores the default time scale and reveals the selected observation. Home/End and existing arrow navigation select/reveal observations. Metric switches and navigation retain the explored window for the same exercise; a different exercise starts at the default view. Empty histories omit controls, and single observations disable zoom.

All 35 logic checks and the full Chromium suite passed. Interaction checks use real mouse and touch events, touch cancellation and vertical scrolling, date-lane panning, zoom limits/reset, immediate metric/navigation restoration, keyboard endpoints, exercise reset and unchanged raw history. Existing gaps, exports, attendance, themes, narrow bounds and 48px targets remain covered. Reviewed refreshed Progress/metric/gap/empty/dropdown previews in both themes and at 320px; the plot and controls fit the 390×844 entry screen. A fresh-context worker implemented the bounded source changes; integration testing caught and corrected deferred width restoration before delivery. Native Android remains not started.
