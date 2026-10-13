# gym assystant

An exploratory browser mockup for an Android training-program app. Run the commands below from `prototypes/web/`. Android implementation has **not started**; scope is being refined through this prototype.

From this directory, open `index.html` in a browser, or run:

```sh
python3 -m http.server 8080
```

Visit http://localhost:8080/ . On a phone, use the computer's LAN address and the same port.

Secondary screens use consistent Back icons; Save reflects pending changes; finished logs show static status rather than editing controls. The screens focus on the next action: compact workout timers and set entry, one Save action per plan editor, expandable scheduling/settings, and performance charts before attendance. Metric definitions and comparison rules remain available through disclosures.

The bottom navigation separates three activities:

- **Train:** the calendar and dated workout execution. Tap a day to update the top card; all seven days fit across the screen. Sessions move between Ready, In progress, Paused, and Finished using Start, Pause, Resume, and Finish. Pause retains entries for Resume; Finish stops timers and preserves a read-only log, even for a partial workout. Past sessions without recorded attendance remain available to catch up unless explicitly Finished, with their original planned dates.
- **Plan:** browse named programs, add a new plan, open a separate editor for each, and choose one saved plan with Use plan. Each program has its own session templates, exercises/order, prescribed sets and rep ranges, weekly/date assignments, and retained draft. Saving does not activate an inactive plan; a dirty target must be saved before activation. Changes remain drafts until Save plan. Untouched today/future workouts use the active saved plan; past obligations keep their original plan, and started, recorded, or reordered workouts keep their own targets and order.
- **Progress:** fictional exercise-performance history stays independent of current edits. Attendance separately shows logged/planned visits, on-time and delayed sessions, not-yet-logged sessions, and average delay for the previous 28 complete planned days, updated through today. Attendance uses fictional visits plus this visit’s working-set logs.

The top-right **⋮ → Settings** menu previews System/English/Italian language choices (Settings and navigation only), System/Light/Dark appearance across all screens, and a save-folder choice. Folder access is a labeled Android preview; JSON status, CSV statistics, and TXT recovery-guide downloads are real browser downloads. Version-2 JSON includes every saved plan, the active-plan ID, independent drafts, raw logs, original prescriptions, settings, and paused timer snapshots without changing live state; CSV retains the current sample-performance/attendance boundaries. [File format and recovery contract](../../docs/architecture/data-formats.md). Back returns to the previous screen and scroll position with drafts, logs, and timers retained. Nothing is auto-saved or imported, and choices still reset on reload.

Move exercises up/down in the Train session list when machines are busy. Only that dated workout changes; sets, notes, and timers stay with each exercise. Start/Resume follows the chosen order.

Reps and weights, warm-up/working-set completion, seat notes, and execution/recovery timers belong to Train. Template prescriptions contain no actual logged sets or timers. Video access remains a secondary placeholder. The initial active sample starts with A on Monday, B on Wednesday, and C on Friday; a second fictional two-day program demonstrates switching. Logging at least one valid working set records attendance on the actual training day; Start, warm-ups, and future previews alone do not. A logged visit can be partial and is counted once. Past plans survive template edits/removal, and future dates keep their schedule. Unlogged sessions do not become zero strength observations. Everything resets on reload; downloaded text files remain readable independently.

The preview uses Material 3 button families: filled primary actions, tonal Add actions, outlined Pause, text actions, and consistent icon controls. Timer/metric segments include a selected check. Shapes, interaction states, bottom navigation, and touch targets follow the documented Android guidance. It is a browser prototype, not a native Android app. No build step or runtime package dependencies are needed. Google Fonts are optional; system fonts are the fallback.

- [Specification](../../docs/product/spec.md): stable requirement IDs and acceptance criteria.
- [Traceability](../../docs/product/traceability.md): spec ↔ source ↔ verification.
- [Progression literature](../../docs/research/progression-research.md): performance versus workload, formulas and evidence limitations.
- [Branding and psychological literature](../../docs/research/branding-research.md): “Your pace. Your progress.” and evidence for motivation.
- [Android UI guidance](../../docs/design/android-ui.md): sources and preview component conventions.
- [Interface review](../../docs/reviews/interface-review.md): user walkthrough findings and before/after measurements.

Every feature change must update the specification before code and update traceability alongside it. Research proposals remain separate from delivered scope.

## Verification

Logic checks require Node and no dependencies:

```sh
node tests/logic.test.cjs
```

For browser interaction/layout checks, install development dependencies, then use a local Chromium or install Playwright's browser:

```sh
npm ci
CHROMIUM_PATH=/usr/bin/chromium npm run test:browser
```

Alternatively, run `npx playwright install chromium` and `npm run test:browser`. Browser checks write screenshots to the system temporary directory under `gym-assystant-previews`; set `PREVIEW_DIR` to change it. These checks validate the mockup, not real training content or Android background behavior.
