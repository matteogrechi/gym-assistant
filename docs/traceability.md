# Bidirectional traceability

All Android delivery is **not started**. Links below refer only to the disposable mockup. Search the code for a requirement ID to trace an implementation back to [the specification](spec.md).

| Requirement | Mockup code anchors | Manual acceptance check | Mockup delivery |
| --- | --- | --- | --- |
| GYM-001 | `mockup/app.js`: `sampleExercises`, `renderSession`, `selectExercise`; `mockup/index.html`: `session` | Select each exercise; verify its name and distinct data; go back. | Implemented in mockup |
| GYM-002 | `mockup/app.js`: `openVideo`; `mockup/index.html`: `videoDialog`, `watchVideo` | Open video panel from each exercise; verify matching name and placeholder label; close it. | Placeholder only; playback deferred |
| GYM-003 | `mockup/app.js`: `seatInput` handler, `renderExercise`; `mockup/index.html`: `seatInput` | Edit a seat note, switch exercises, return; verify note remains isolated. | Implemented in mockup |
| GYM-004 | `mockup/app.js`: `updateSet`, `renderExercise`; `mockup/index.html`: `sets` | Change reps and weight; reject negative numbers and fractional reps; navigate away and back. | Implemented in mockup |
| GYM-005 | `mockup/app.js`: `toggleSet`, `renderSession`, `renderExercise` | Mark and unmark sets; verify counts in detail and session views. | Implemented in mockup; proposed scope |
| GYM-006 | `mockup/index.html`: shell and prototype disclosure; `mockup/style.css`; `mockup/app.js`: dialog and navigation handlers | Check phone and desktop widths, keyboard navigation, dialog dismissal; reload to reset samples. | Implemented in mockup |

## Reverse inventory

- Sample session cards and back navigation → GYM-001.
- Machine illustration, video button, and video placeholder dialog → GYM-002.
- Seat note field → GYM-003.
- Set table, numeric input validation, and unit label → GYM-004.
- Done buttons and completion counts → GYM-005.
- Responsive shell, typography, prototype disclosure, focus states, and dialog behavior → GYM-006.

No other functional features are included. The machine illustration is schematic decoration, not a technique guide.

## Verification record

2026-10-12: JavaScript syntax check passed. Chromium interaction checks passed for exercise navigation, seat-note isolation, reps/weight retention, rejection of negative and fractional reps, completion toggling/counts, exercise-specific video dialog and Escape dismissal, reload reset, and absence of horizontal overflow at 390px. Desktop screenshot reviewed at 1280px. These checks validate the mockup only; they do not validate exercise content or Android behavior.
