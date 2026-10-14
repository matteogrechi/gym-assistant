# Mockup alignment with the Android app

14 October 2026, specification revision36. The browser presentation was compared with the current native Train, Exercise, Planning, Progress and Settings sources. A fresh independent reviewer identified five material differences; the coordinator implemented and checked them, plus the requested page logo.

| Area | Result |
| --- | --- |
| Brand | Android's approved dumbbell vector appears in the desktop page wordmark, browser icon and external reference strip. The phone retains the native text wordmark. SVG paths, stroke, colors and license are shared with the [brand assets](../design/branding/README.md). |
| Progress | Setup and Reference weight (kg) appear above the shared metric selector. Only comparable setups and valid completed working-set weights are offered. Reps require explicit reference selection; choices persist per exercise/setup. Changed context resets plot exploration. Closed labels truncate without clipping the chevron; popup options retain full text. |
| Future previews | Session actions, logging, warm-up changes, reordering and timer execution are unavailable. Untouched future previews follow plan edits/activation; recorded past/today snapshots remain protected. |
| Session | Pause precedes Finish, matching Android. Existing Start/Resume accessible names and the independent exercise timer controls remain. |
| Planning | Library heading/tabs and plan/template heading/actions remain visible when scrolling in both browsing and editing. The sticky surface covers scrolled content. Prescription removal is the shared labelled text action “Remove exercise”. |

The additional comparison row follows the native screen's hierarchy. The 180px plot and 48px exploration targets retain their sizes; vertical scrolling reveals the complete card and guidance when needed.

Validation: the pure logic suite, full Chromium interaction suite and focused visual checks are recorded in [traceability](../product/traceability.md#mockup-alignment--revision-36). Focused checks cover both themes at320/390/430px and1440px desktop, logo loading/placement and overflow, selected reference values, read-only future exercises, lifecycle action order and pinned planning controls. The regression suite additionally verifies context isolation/reference retention, warm-up/invalid-weight exclusion, snapshot protection, plot gestures, phone-contained dropdowns/dialogs, translated Settings and existing exercise/plan workflows. Updated representative captures accompany that evidence.

This is UI alignment, not conversion of the disposable browser into the native app. Browser records are explicitly fictional; reload resets edits. Its performance charts use labelled sample history, while Android computes immediate statistics from real completed sets. Browser file-provider/video panels remain disclosed previews; Android uses genuine local files. System bars, native pickers, persistence and full native localization are separate platform behavior. No exact pixel parity, physical-device check or all-platform rendering guarantee is claimed by this review.
