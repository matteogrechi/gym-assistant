---
name: gym-mockup-review
description: "Use when changing or reviewing gym assystant browser screens, interactions, accessibility, theme, language or responsive layout. Walk through realistic gym workflows and verify the mockup against its specification and Android UI guidance."
---

# Gym mockup review

Paths below assume installation at `.agents/skills/gym-mockup-review/`; run commands from the repository root unless noted.

Read the affected requirements in [spec.md](../../../docs/product/spec.md), [android-ui.md](../../../docs/design/android-ui.md) and relevant findings in [interface-review.md](../../../docs/reviews/interface-review.md). For new Android design questions, verify current official Android/Material guidance. Read [branding-research.md](../../../docs/research/branding-research.md) only for branding changes; do not claim that a color alone guarantees gym attendance.

## Review as a person using the app

Choose flows affected by the change and exercise them with realistic entries, navigation and interruptions:

- Train: choose a calendar date, check its top card, open a session, Start, log working and warm-up sets, change seat notes, reorder exercises, use execution and recovery timers, Pause, navigate away, Resume and Finish. Check retained values and finished read-only review.
- Plan: browse plans, edit a draft, leave and return, save and explicitly activate. Check existing workouts and other plans remain intact. Exercise validation in collapsed sections when relevant.
- Progress: select a metric, interpret units and gaps, distinguish working performance from attendance and delayed sessions.
- Settings: enter through the top-right overflow, change relevant theme/language options, return to the originating screen and inspect downloads when affected.

For a request to audit the whole interface, walk every page and major state. For a small correction, review the affected flow and shared components it can change. Remove redundant information only when the remaining UI still communicates the user's next action and necessary context.

## Check shared design behavior

- Keep reps and set logging prominent, videos secondary, timers at the top and Train/Plan/Progress navigation at the bottom. Use the existing Material-style button families and SVG icons.
- Verify standard controls have at least 48 × 48 CSS-pixel targets in the mockup; the fitted seven-column calendar has its documented width exception. Disclosure rows have a minimum 56px height and a clear 24px chevron; the whole row operates with pointer and keyboard.
- Check narrow layouts at 320, 390 and 430px, light/dark appearance and affected translated labels. Preserve user-entered names/data. The mockup's full-screen localization remains limited to its specified preview coverage.
- Check modal surfaces and scrims stay inside the phone on desktop and narrow/short viewports, scroll internally when needed and align after page scroll/resize. Verify keyboard focus, Escape dismissal and return to the trigger. Calendar selection updates the card without opening a dialog.
- Inspect screenshots as well as dimensions: clipping, hierarchy, selected/disabled/read-only states and focus visibility are not proved by target-size assertions alone.

## Verification and evidence

Use existing [logic tests](../../../prototypes/web/tests/logic.test.cjs) for changed data/timer behavior. For browser changes, run from `prototypes/web/`:

```sh
CHROMIUM_PATH=/usr/bin/chromium npm run test:browser
```

Use the available Playwright browser when system Chromium is absent; setup is in the [prototype README](../../../prototypes/web/README.md). Add checks for meaningful changed behavior when needed, not assertions that merely restate the implementation. Review generated screenshots; temporary downloads/captures stay outside tracked source. Update representative images under `docs/reviews/previews/` only when they document the changed design. Record findings, actual verification and remaining mockup limitations in the relevant review and traceability entries.
