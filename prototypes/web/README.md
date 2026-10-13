# gym assystant

An exploratory browser mockup for an Android training-program app. Run the commands below from `prototypes/web/`. Android implementation has **not started**; scope is being refined through this prototype.

From this directory, open `index.html` in a browser, or run:

```sh
python3 -m http.server 8080
```

Visit http://localhost:8080/ . On a phone, use the computer's LAN address and the same port.

Secondary screens use consistent Back icons; Save reflects pending changes; finished logs show static status rather than editing controls. The screens focus on the next action: compact workout timers and set entry, one Save action per plan editor, expandable scheduling/settings, and performance charts before attendance. Metric definitions and comparison rules remain available through disclosures.

The bottom navigation separates three activities:

- **Train:** the calendar and dated workout execution. Tap a day to update the top card; all seven days fit across the screen. Sessions move between Ready, In progress, Paused, and Finished using Start, Pause, Resume, and Finish on the session screen. The exercise log keeps timer controls; Back returns to session actions without changing entries or live timers. Pause retains entries for Resume; Finish stops timers and preserves a read-only log, even for a partial workout; incomplete sessions require confirmation, while Cancel leaves the workout and clocks running. Past sessions without recorded attendance remain available to catch up unless explicitly Finished, with their original planned dates.
- **Plan:** use Plans / Exercises secondary tabs. Exercises lists shared definitions and opens read-only name/machine/default-setup details; Add exercise works without opening a plan. The session editor retains the same contextual creator. The selected section is retained during the visit. In Plans, browse named programs, add a new plan, open a read-only preview for each, explicitly choose Edit to change its draft, then choose Make active plan in its read-only detail view and confirm the named switch. The library contains no activation shortcuts; Cancel has initial focus in the confirmation. Each program has its own session templates, exercises/order, prescribed sets, rep ranges and recovery seconds, weekly/date assignments, and retained draft. Saving does not activate an inactive plan; a dirty target must be saved before activation. Changes remain drafts until Save plan, which returns to read-only mode. Back retains drafts without saving them. Discard new draft selectively removes a never-saved plan after confirmation. In an editable session, Create exercise saves a shared local name/machine variant and optional default setup; Add and Save plan remain separate. Untouched today/future workouts use the active saved plan; past obligations keep their original plan, and started, recorded, or reordered workouts keep their own targets and order.
- **Progress:** larger dated plots show a labelled value scale, exact units and selectable sessions. Tap a point to read its value and full date; keyboard users can press left/right on a point. Metric changes retain the selected date. Missing observations break the line and remain unavailable, while closely spaced dates pan inside the chart. Fictional exercise-performance history stays independent of current edits. Attendance separately shows logged/planned visits, on-time and delayed sessions, not-yet-logged sessions, and average delay for the previous 28 complete planned days, updated through today. Attendance uses fictional visits plus this visit’s working-set logs. Drag/swipe across the plot to inspect recorded dates, zoom the timeline with Zoom in/out, pan the date lane and use Reset view to restore the default scale. Keyboard arrows and Home/End select and reveal observations; the exact readout and exploration view persist across metric switches.

The top-right **⋮ → Settings** menu previews System/English/Italian/Spanish language choices (Settings and navigation only), System/Light/Dark appearance across all screens, and a save-folder choice. Folder access is a labeled Android preview; JSON status, CSV statistics, and TXT recovery-guide downloads are real browser downloads. Version-2 JSON includes every saved plan, the active-plan ID, independent drafts, raw logs, original prescriptions, settings, and paused timer snapshots without changing live state; CSV retains the current sample-performance/attendance boundaries. [File format and recovery contract](../../docs/architecture/data-formats.md). Back returns to the previous screen and scroll position with drafts, logs, and timers retained. Nothing is auto-saved or imported, and choices still reset on reload.

Drag exercises by their grip in the Train session list when machines are busy; the same handles reorder exercise prescriptions in Plan. Keyboard users focus a handle and press ArrowUp/ArrowDown. Only that dated workout changes; sets, notes, and timers stay with each exercise. Start/Resume follows the chosen order.

Reps and weights, warm-up/working-set completion, seat notes, and execution/recovery timers belong to Train. Set recovery duration in the exercise prescription during planning (1–3600 seconds, default 90). The workout countdown uses that saved target and displays it read-only; protected workouts retain their original duration. Template prescriptions contain no actual logged sets or running timers. Video access remains a secondary placeholder. The initial active sample starts with A on Monday, B on Wednesday, and C on Friday; a second fictional two-day program demonstrates switching. Logging at least one valid working set records attendance on the actual training day; Start, warm-ups, and future previews alone do not. A logged visit can be partial and is counted once. Past plans survive template edits/removal, and future dates keep their schedule. Unlogged sessions do not become zero strength observations. Everything resets on reload; downloaded text files remain readable independently.

The preview uses Material 3 button families: filled primary actions, tonal Add actions, outlined Pause, text actions, and consistent icon controls. Timer and metric selectors reuse `GymSegments.mount` for native buttons, pressed selection and keyboard navigation. Both use centered text, selected tonal fill, heavier labels and an underline; view placement and domain callbacks stay separate. Shapes, interaction states, bottom navigation, and touch targets follow the documented Android guidance. It is a browser prototype, not a native Android app. No build step or runtime package dependencies are needed. Google Fonts are optional; system fonts are the fallback.

- [Specification](../../docs/product/spec.md): stable requirement IDs and acceptance criteria.
- [Traceability](../../docs/product/traceability.md): spec ↔ source ↔ verification.
- [Progression literature](../../docs/research/progression-research.md): performance versus workload, formulas and evidence limitations.
- [Branding and psychological literature](../../docs/research/branding-research.md): “Your pace. Your progress.” and evidence for motivation.
- [Android UI guidance](../../docs/design/android-ui.md): sources and preview component conventions.
- [Interface review](../../docs/reviews/interface-review.md): user walkthrough findings and before/after measurements.

Every feature change must update the specification before code and update traceability alongside it. Research proposals remain separate from delivered scope.

## Mockup view references

The strip above the phone shows a stable view ID, including dialogs and the overflow menu. Use it in feedback, for example “VIEW-003: improve the reps controls.” The [view catalog](../../docs/product/spec.md#gym-006-mockup-only-view-references) lists all seventeen references. These labels exist only in the mockup presentation and are excluded from Android UI and downloaded app data.

## Verification

Logic checks require Node and no dependencies:

```sh
npm test
```

For browser interaction/layout checks, install development dependencies, then use a local Chromium or install Playwright's browser:

```sh
npm ci
CHROMIUM_PATH=/usr/bin/chromium npm run test:browser
```

Alternatively, run `npx playwright install chromium` and `npm run test:browser`. Browser checks write screenshots to the system temporary directory under `gym-assystant-previews`; set `PREVIEW_DIR` to change it. These checks validate the mockup, not real training content or Android background behavior.

Dropdown fields share the app theme and retain native selection/keyboard behavior. Chromium and other browsers supporting `appearance: base-select` also show styled, phone-contained option menus. Other browsers retain functional native pickers. Read-only planning values remain disabled and have no dropdown chevron.

The [information review](../../docs/reviews/information-review.md) records the latest all-view walkthrough. Saved plan browsing lists date exceptions and starts prescription details collapsed. Editing keeps one Save/status group visible while scrolling; the session editor's Save plan commits the complete draft and reveals invalid rep ranges. Exercise completion explicitly counts working sets, and non-today/original-plan logs retain context. Short desktop dialogs use the visible portion of the phone.
