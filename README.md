# gym assystant

A native, entirely offline Android gym-session assistant, implemented from its interactive browser mockup. Plans, actual logs, timers and settings are saved on the device; optional local-folder JSON backups and CSV/TXT exports remain readable without the app. The mockup remains a separate visual reference.

| Location | Purpose |
| --- | --- |
| [android/](android/README.md) | Kotlin/Compose app, pure Kotlin domain, Gradle build and native checks. |
| [prototypes/web/](prototypes/web/README.md) | Disposable interactive mockup, its Node tooling, and its tests. |
| [docs/product/](docs/product/spec.md) | Product specification and [bidirectional traceability](docs/product/traceability.md). |
| [docs/research/](docs/research/progression-research.md) | Progression metrics and [motivating-brand evidence](docs/research/branding-research.md). |
| [docs/design/](docs/design/android-ui.md) | Android UI conventions. |
| [docs/reviews/](docs/reviews/native-implementation-review.md) | Native validation/screenshots and the separate [mockup review](docs/reviews/interface-review.md). |
| [docs/architecture/](docs/architecture/repository-layout.md) | Repository boundaries, offline architecture and portable data contracts. |
| [.agents/skills/](AGENTS.md) | Project workflow skills for specification, mockup review and delivery. |

## Build the Android app

Follow [Android setup and installation](android/README.md). With JDK17 and the documented Android SDK installed:

```sh
cd android
./gradlew :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The installed app works without connectivity. The [native review](docs/reviews/native-implementation-review.md) records actual device coverage and remaining checks.

## Explore the prototype

Open [prototypes/web/index.html](prototypes/web/index.html) directly, or run from the repository root:

```sh
python3 -m http.server 8080
```

Visit http://localhost:8080/prototypes/web/ . The strip above the phone provides stable view references for feedback (for example, `VIEW-003` for Exercise). Plan lets you browse, create, explicitly edit, and confirm activation of named programs while retaining existing workouts. Set recovery seconds in the plan; reorder exercises with drag handles. The top-right ⋮ → Settings menu previews language, theme, and a save folder, and downloads status JSON, statistics CSV, and a recovery guide. All data is fictional; edits reset on reload. Detailed usage is in the [prototype README](prototypes/web/README.md).

## Verify the prototype

From the repository root, run the existing logic checks:

```sh
node prototypes/web/tests/logic.test.cjs
```

Install and run browser checks within the prototype project:

```sh
cd prototypes/web
npm ci
CHROMIUM_PATH=/usr/bin/chromium npm run test:browser
```

Alternatively, install Playwright's browser with `npx playwright install chromium` and run `npm run test:browser`. Screenshots are written to the system temporary directory; set `PREVIEW_DIR` to override it. These checks validate the browser mockup, not an Android app.

The [native file contract](docs/architecture/native-file-layout.md) documents genuine-data backups and recovery without the app. The [prototype contract](docs/architecture/data-formats.md) remains separate and labels fictional data.

## Development boundary

Update the specification before changing features and maintain traceability alongside code. Research proposals are not automatically authorized features. Native implementation belongs under `android/`; the prototype is design evidence rather than production source. See the [repository layout](docs/architecture/repository-layout.md) and [Android build instructions](android/README.md).

Follow [CONTRIBUTING.md](CONTRIBUTING.md) for repository conventions, validation and **Conventional Commits**. [AGENTS.md](AGENTS.md) routes agents to the project skills under `.agents/skills/`.
