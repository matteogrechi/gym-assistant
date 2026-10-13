# Repository layout

Status: repository organization only. Android implementation is not started. Product behavior and requirement IDs remain unchanged.

This repository keeps the future Android project independent from the disposable browser prototype. Each project owns its source, tests, and tool configuration. Shared specifications and evidence stay under `docs/`.

```text
gym-assistant/
├── README.md
├── .gitignore
├── android/
│   └── README.md                 # reserved project location, no implementation
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

## Future Android project

`android/` will be the Android Studio/Gradle project root when implementation is authorized. The standard app module will live at `android/app/`, with production sources/resources in `src/main/`, local tests in `src/test/`, and device tests in `src/androidTest/`. Android documents these module and source-set conventions. [Android project structure](https://developer.android.com/studio/projects)

Project-level Gradle settings, build configuration, and the Gradle wrapper will belong under `android/`; module build configuration will belong under `android/app/`. Those files are intentionally not generated in this change. [Android build configuration](https://developer.android.com/build)

The choice to place the Android project in its own repository subdirectory is a repository convention, not a requirement imposed by Android. No framework, SDK/version, language, package name, storage approach, or feature-module structure is selected here. Additional modules can be introduced later when actual responsibilities justify them. [Android module patterns](https://developer.android.com/topic/modularization/patterns)

Browser JavaScript, HTML/CSS, fictional fixtures, Node packages, and mockup tests remain exclusively in `prototypes/web/`. They are design/reference artifacts, not Android source, app resources, or production tests. Review screenshots remain documentation artifacts rather than packaged assets.

## Documentation and traceability

`docs/product/spec.md` remains the feature source of truth. `docs/product/traceability.md` maps requirement IDs to the browser prototype and its checks; Android delivery remains marked not started. New Android mappings must be added only when actual implementation exists.

Research recommendations are evidence, not automatically authorized features. Design guidance and interface reviews are kept separate from requirements. Relative documentation links and source references must resolve after moves.

## Local files and build products

Ignore Node dependency installations, Android Gradle/build caches, IDE workspace files, and local SDK configuration. Commit source, specifications, tests, lockfiles, and the future Gradle wrapper/build configuration. The root does not carry a Node package or Android build scaffold at this stage.
