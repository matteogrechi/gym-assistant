# Information and workflow review — revision 28

Four fresh-context agents navigated disjoint screen groups before source changes: training (VIEW-001/002/003/009), planning (004/005/006/012), Progress (007) and Settings/popups (008/010/011). They used real Chromium interactions, screenshots and state/geometry probes in light/dark and representative 320/390/430px layouts. Stress fixtures were explicitly browser-only. Representative captures were visually inspected; the number of screenshots is not a claim that each image was inspected separately.

The coordinator consolidated their findings, wrote revision-28 acceptance criteria before source changes and integrated the fixes. Two bounded workers owned only chart geometry/initial reveal and dialog placement; other source, tests and delivery stayed with the coordinator.

## Accepted findings

| View | Observed user problem | Correction |
| --- | --- | --- |
| 003 | A future exercise loses its preview date; an old plan loses identity at the log. | Conditional non-today/preview and original-plan context beside the exercise flow. |
| 003 | The combined log's completion total silently excludes warm-ups. | Working: done/total, with a full accessible working-set completion label. |
| 003 | Paused timer instruction points toward an unavailable Resume action. | Explicit Back to session, then Resume path; timer stays disabled. |
| 003 | Essential row types use 8px uppercase captions. | 11px title-case Warm-up/Working captions; tint and numbering retained. |
| 005 | Reopening a read-only plan hides its future date exceptions behind disabled controls. | Existing date assignments listed as date/session/rest text in Date changes; mutation controls remain Edit-only. |
| 006 | Save appears local but commits the entire retained plan draft. | Save plan action with a full accessible scope label; same whole-plan transaction. |
| 006 | First read-only prescription repeats all its summary values and displaces other exercises. | Collapsed default for browsing; first-open editor retained. |
| 005/006 | Save/status disappear at the end of a long form. | One persistent edit heading/action/status group, with space for focused fields and correct layering below overflow. |
| 005/006 | Reversed min/max reps produce an error above a hidden disclosure. | Open the affected template/card, identify both invalid fields and focus Min reps; no draft loss. |
| 007 | A gap point's 48px target intercepts date-lane panning. | Entire gap target remains inside the plot; unavailable value remains a dash. |
| 007 | Dense initial history selects a point outside the visible timeline. | Reveal selected observation once after measurable first layout; preserve later deliberate pan/zoom. |
| 010 | A short desktop viewport lets the folder card/focused actions scroll offscreen. | Card centered and bounded in the phone/visible-viewport intersection, with internal scrolling; background wheel scroll blocked. |
| 011/008 | Selecting Settings again discards its reading position. | Dismiss the already-current destination's menu and restore trigger focus without navigating. |
| 008 | Noninteractive spacing partly hides About the files on a standard phone. | Reduced heading/helper/location margins and padding; full disclosure and action targets retained. |

The coordinator also reproduced gray rectangular browser tap flashes during mixed touch/keyboard screenshot flows. They were transient browser overlays, not selected-state colors. Shared controls already provide pressed and keyboard-focus feedback, so the duplicate tap overlay is suppressed. This is one additional consistency correction beyond the agents' 14 findings.

## Intentional information retained

VIEW-001 retains its requested fixed 224px banner and reserved rest-day action space. VIEW-002 retains date, lifecycle, one working completion total and delayed actual attendance context. VIEW-004 keeps browsing separate from activation, and VIEW-012 keeps the named, Cancel-first confirmation. VIEW-009 keeps its truthful unavailable-video placeholder. Empty space after short content is not filled with extra information.

Progress's formulas, comparison setup/effort, workload limitation, sample-history boundary, planned period, as-of date and unknown-attendance definition remain useful. Settings keeps language-preview scope, folder-preview/browser-download boundaries and distinct JSON/CSV/TXT purposes. These statements describe different data scopes and were not removed as redundant. No new metric, storage capability, plan mutation shortcut or native Android implementation was introduced.

The online [generated UI research](../research/generated-ui-review.md) supplies general review hypotheses. It found no measured ranking specifically for GPT-6.1 Sol at High effort; this review does not attribute its findings to that model.

## Verification and limits

Before integration, the coordinator captured 67 baseline states with no undersized standard controls. Agent walkthroughs covered real navigation, lifecycle interruptions, working/warm-up logging, delayed attendance, editing/retained drafts, activation cancellation, chart mouse/touch exploration, disclosures, translated Settings, downloads and short-dialog focus. Their reports distinguish live flows from injected edge histories. No P0/P1 defect was reported in the reviewed browser flows.

Integrated results, refreshed previews and remaining limits are recorded after regression verification in [interface-review.md](interface-review.md) and [traceability.md](../product/traceability.md). This is browser evidence, not native Android, physical-device, screen-reader or cross-browser certification. Production persistence, real videos, full localization and Android background timing remain outside the authorized mockup phase.
