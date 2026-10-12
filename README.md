# gym assystant

An exploratory mockup for an Android training-program app. Android implementation has **not started**; scope is being refined through this prototype.

Open `mockup/index.html` in a browser, or run:

```sh
python3 -m http.server 8080
```

Visit http://localhost:8080/mockup/ . On a phone, use the computer's LAN address and the same port.

Use the Program calendar to see today’s planned session and upcoming workouts. Browse weeks and tap a date to assign Session A/B/C or no session in its planning dialog. The editable sample repeats A on Monday, B on Wednesday, and C on Friday. Open a dated session, then use Start session to begin with its first unfinished exercise. Use Continue while active, Stop to pause the session and its timers, and Resume to keep going with your entries retained. Logs and timers are isolated between dates. Log reps and weight in the shared working/warm-up set list, edit seat notes, and try the execution stopwatch and recovery countdown. The Program / Progress navigation stays at the bottom. Progress shows explicitly fictional history and excludes current edits. Video access is a secondary placeholder. Everything resets on reload.

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
