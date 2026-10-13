# gym assystant

An Android gym-session assistant being specified through an interactive browser prototype. Android implementation has **not started**.

| Location | Purpose |
| --- | --- |
| [android/](android/README.md) | Reserved Android Studio/Gradle project location; documentation only. |
| [prototypes/web/](prototypes/web/README.md) | Disposable interactive mockup, its Node tooling, and its tests. |
| [docs/product/](docs/product/spec.md) | Product specification and [bidirectional traceability](docs/product/traceability.md). |
| [docs/research/](docs/research/progression-research.md) | Progression metrics and [motivating-brand evidence](docs/research/branding-research.md). |
| [docs/design/](docs/design/android-ui.md) | Android UI conventions. |
| [docs/reviews/](docs/reviews/interface-review.md) | Interface walkthrough and preview images. |
| [docs/architecture/](docs/architecture/repository-layout.md) | Repository organization and future project boundaries. |

## Explore the prototype

Open [prototypes/web/index.html](prototypes/web/index.html) directly, or run from the repository root:

```sh
python3 -m http.server 8080
```

Visit http://localhost:8080/prototypes/web/ . The top-right ⋮ → Settings menu previews language, theme, and a save folder, and downloads status JSON, statistics CSV, and a recovery guide. All data is fictional; edits reset on reload. Detailed usage is in the [prototype README](prototypes/web/README.md).

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

The [text-file contract](docs/architecture/data-formats.md) documents versioned backups and recovery without the app.

## Development boundary

Update the specification before changing features and maintain traceability alongside code. Research proposals are not automatically authorized features. The next implementation phase belongs under `android/` after scope is agreed; the prototype is not production source. See the [repository layout](docs/architecture/repository-layout.md).
