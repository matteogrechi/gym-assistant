# gym assystant

An exploratory mockup for an Android training-program app. Android implementation has **not started**; scope is being refined through this prototype.

Open `mockup/index.html` in a browser, or run:

```sh
python3 -m http.server 8080
```

Visit http://localhost:8080/mockup/ . On a phone, use the computer's LAN address and the same port.

The bottom navigation separates three activities:

- **Train:** the calendar and dated workout execution. Tap a day to update the top card; all seven days fit across the screen. Start, Continue, Stop, and Resume retain that workout’s logs and timers.
- **Plan:** edit the sample program, session templates, exercises/order, prescribed sets and rep ranges, and weekly/date assignments. Changes remain drafts until Save plan. New workouts use the saved plan; sessions with recorded entries keep their original targets.
- **Progress:** explicitly fictional historical performance, independent of current edits.

Reps and weights, warm-up/working-set completion, seat notes, and execution/recovery timers belong to Train. Template prescriptions contain no actual logged sets or timers. Video access remains a secondary placeholder. The sample starts with A on Monday, B on Wednesday, and C on Friday. Everything resets on reload.

The preview uses Material 3 conventions for shapes, selection, bottom navigation, and touch targets. It is a browser prototype, not a native Android app. No build step or runtime package dependencies are needed. Google Fonts are optional; system fonts are the fallback.

- [Specification](docs/spec.md): stable requirement IDs and acceptance criteria.
- [Traceability](docs/traceability.md): spec ↔ source ↔ verification.
- [Progression literature](docs/progression-research.md): performance versus workload, formulas and evidence limitations.
- [Branding and psychological literature](docs/branding-research.md): “Your pace. Your progress.” and evidence for motivation.
- [Android UI guidance](docs/android-ui.md): sources and preview component conventions.

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
