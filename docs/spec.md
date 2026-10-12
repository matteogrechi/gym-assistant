# Gym session assistant — draft specification

Status: draft for exploration. Only a disposable browser mockup is authorized now. Android implementation starts after the user decides the scope. Mockup coverage does not mean a production requirement is implemented.

## Purpose

Help the user follow a gym session, verify the exercise machine and movement using an in-app video, record seat position, and record repetitions and weight.

## Requirements

| ID | Requirement | Acceptance criteria |
| --- | --- | --- |
| GYM-001 | Show the exercises in a gym session and allow selecting the exercise to perform. | Selecting an exercise opens its own details. The mockup uses a fixed, explicitly labeled sample session. Session creation and editing are undecided. |
| GYM-002 | Offer video access within the exercise details to check the machine and movement. | The final app plays a video appropriate to the selected exercise inside the app. The mockup opens an in-app video placeholder naming that exercise and explicitly says it is not a playable video. Actual playback is deferred, not considered implemented. |
| GYM-003 | Record seat position for each exercise. | The user can enter free text (for example, “Seat 4, backrest 2”), switch exercises, and return to the same note during the mockup session. |
| GYM-004 | Record repetitions and weight for each set of an exercise. | Each sample set has editable reps and weight. Reps accept nonnegative whole numbers; weight accepts nonnegative numbers. Values remain associated with their exercise while navigating. The mockup provisionally displays kilograms. |
| GYM-005 | Track sets completed during the session. | A set can be marked done and undone. Each exercise shows its completed-set count. This is a proposed requirement for exploration, not yet a confirmed production requirement. |
| GYM-006 | Provide a dummy mockup that can be explored before committing to implementation. | The mockup adapts to a narrow phone viewport and desktop, identifies itself as a prototype, and supports GYM-001–005 as specified above. Sample data and edits exist only in memory and reset on reload. |

## Interaction scope

The session view lists three sample machine exercises. Selecting one opens its detail view, which contains video access, seat notes, and three sample sets with reps, weight, and completion controls. A back button returns to the session. A video dialog can be closed using its button, Escape, or its backdrop.

Sample exercises, sample values, three sets, kilogram units, and visual styling are mockup assumptions, not finalized product decisions. No recommendations about suitable loads or exercise technique are provided.

## Deliberately undecided

- Video source, licensing, language, offline availability, and machine-specific variations.
- Creating routines, adding or removing exercises and sets, and reordering a session.
- Whether seat settings and prior weights should carry over to the next session.
- Kilograms versus pounds, and per-side versus total machine weight.
- Persistence, backups, accounts, history, and Android technology choices.

These are questions for later requirements discussions, not features to build now. Timers, statistics, social features, subscriptions, coaching, and notifications are outside the current scope.

## Change and traceability rules

Every functional feature must have a requirement ID before code is added. Code anchors and UI `data-req` attributes identify the requirements they serve. [traceability.md](traceability.md) maps each requirement to code, a manual check, and its delivery state. Structural layout and accessibility support belong to GYM-006. Update this spec and the matrix together whenever scope changes. Traceability supports review; it does not by itself guarantee correctness.

Before Android implementation, review the draft with the user, resolve the implementation-critical decisions, and explicitly approve the scope. Keep mockup coverage and actual Android delivery separate in the matrix.
