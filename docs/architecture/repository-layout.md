# Repository layout

Status: native Android implementation is authorized under specification revision 34. Browser and Android projects remain independent.

This repository keeps the native Android project independent from the disposable browser prototype. Each project owns its source, tests, and tool configuration. Shared specifications and evidence stay under `docs/`.

```text
gym-assistant/
├── README.md
├── CONTRIBUTING.md               # workflow, validation and Conventional Commits
├── AGENTS.md                     # repository instructions and skill routing
├── .gitignore
├── .github/workflows/            # signed main-push Android distribution
├── .agents/
│   └── skills/                   # versioned project workflow skills
├── android/
│   ├── app/                      # Android UI, resources, adapters, native tests
│   ├── domain/                   # pure Kotlin rules and JVM tests
│   └── gradle/                   # pinned catalog and verified wrapper
├── prototypes/
│   └── web/
│       ├── README.md             # prototype usage and verification
│       ├── index.html
│       ├── css/style.css
│       ├── js/                   # existing mockup code and fictional fixtures
│       ├── tests/                # existing logic and browser checks
│       ├── package.json
│       └── package-lock.json
└── docs/
    ├── product/                  # specification and bidirectional traceability
    ├── research/                 # progression and branding evidence
    ├── design/                   # Android UI conventions
    ├── reviews/                  # interface review and preview images
    └── architecture/             # repository organization decisions
```

## Android project

`android/` is the Android Studio/Gradle project root. `android/app/` owns production Kotlin/resources in `src/main/`, JVM checks in `src/test/`, installed-device checks in `src/androidTest/`, and the exported Room schema in `schemas/`. `android/domain/` is a pure Kotlin JVM module containing immutable models, training/schedule/timer/metric rules and their tests. [Android project structure](https://developer.android.com/studio/projects)

Project Gradle settings, build configuration, version catalog and wrapper belong under `android/`; module build files belong in their modules. The pinned API26/36 Kotlin/Compose build and manual dependency container implement the [design plan](android-implementation-plan.md). There is no module per screen, DI framework or browser wrapper. [Android build configuration](https://developer.android.com/build)

Browser JavaScript, HTML/CSS, fictional fixtures, Node packages, and mockup tests remain exclusively in `prototypes/web/`. They are design/reference artifacts, not Android source, app resources, or production tests. Review screenshots remain documentation artifacts rather than packaged assets.

## Documentation and traceability

`docs/product/spec.md` remains the feature source of truth. `docs/product/traceability.md` maps requirement IDs separately to mockup and native sources/checks. Android evidence must come from actual native implementation and validation.

Research recommendations are evidence, not automatically authorized features. Design guidance and interface reviews are kept separate from requirements. Relative documentation links and source references must resolve after moves.

Root [CONTRIBUTING.md](../../CONTRIBUTING.md) defines contributor workflow and Conventional Commits for new changes. Root [AGENTS.md](../../AGENTS.md) routes agents to the versioned skills in `.agents/skills/`: specification/traceability, mockup review, repository delivery and the vendored Ponytail coding philosophy. Skills contain `SKILL.md` and optional UI metadata; Ponytail also retains its upstream MIT license. These files are process guidance, not product requirements or Android source.

## Local files and build products

Ignore Node dependency installations, Android Gradle/build caches, IDE workspace files, local SDK configuration and the private `/.signing/` production-key backup. The main-push workflow lives in `.github/workflows/`; its contract is documented in [Android distribution](android-distribution.md). Commit source, specifications, tests, lockfiles, and the Gradle wrapper/build configuration. The root does not carry a Node package; native tooling stays in `android/`.
