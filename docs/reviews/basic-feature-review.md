# Basic feature review — gym assystant

Review date: 13 October 2026. Baseline: `4c89bfe`, specification revision 29. This is a review of basic user tasks before Android implementation, with recommendations rather than new requirements. The [specification](../product/spec.md) and [native implementation plan](../architecture/android-implementation-plan.md) retain their scope and pending choices. No mockup behavior or Android source changes accompany this review.

## Review method

Two isolated agents worked in separate roles. The proposer navigated the mockup and identified blocked tasks, workarounds and the smallest useful additions. The challenger independently navigated the existing flows before receiving the proposer's unfiltered candidates, then tried to refute or reduce them. The coordinator checked their evidence and consolidated the decisions.

Navigation used real headless Chromium interactions with the existing browser prototype, a 390×844 touch viewport and a fixed review date of 12 October 2026 in Europe/Rome. Narrow dark-mode Settings/Progress and Italian Settings were sampled. These are synthetic walkthroughs, not observations of gym users or evidence about how frequently a problem occurs. Native device, screen-reader, process-death and offline delivery claims require later Android verification.

Both roles reached all 12 view IDs across their main and focused follow-up walks. The exercised tasks included calendar selection for today/rest/future/overdue dates, selecting/reordering exercises, working and warm-up entries, seat notes, both timers, Pause/Resume/Finish and finished review, saved/inactive/new plans, retained drafts, explicit saving/activation, weekly/date scheduling, plot inspection and metric switching, overflow/Settings/folder previews and JSON/CSV/TXT downloads.

Two initial script steps failed: the proposer looked for an Edit action on an already-editable new draft; the challenger's capture helper blurred focus before testing Enter on the activation dialog. Focused follow-ups completed the intended planning tasks using the correct controls, including verifying initial Cancel focus and dismissal. A nonexistent timer selector in the proposer's focused follow-up was also corrected. Those script failures are not recorded as missing app capabilities. Video playback, reload persistence, real-history charts and physical folder access are specified native work or deliberate prototype limitations; their placeholders do not independently justify extra features.

## Candidates submitted to challenge

| ID | Blocked task and reproduction | Current alternative | Requirement boundary |
| --- | --- | --- | --- |
| F01 | Enter a cable-row prescription. Plan → Add plan → session → exercise selection offers only the three illustrative machines. | Demonstrate with the samples or keep unsupported movements elsewhere; renaming a session does not establish an exercise identity. | GYM-018/026; a local exercise editor is already proposed, pending a product choice, in native-plan §9. |
| F02 | Abandon an unwanted new plan. Add plan → enter a name → Back → reopen: the retained uncommitted draft has no selective discard action. | Ignore or repurpose it. Reload clears everything and is not selective cancellation. | GYM-026 deliberately retains drafts. Discarding only an unsaved new draft would be new scope; saved-plan deletion remains excluded. |
| F03 | Record a third actual working set in a workout prescribed for two. Start the two-set session: only Add warm-up is available. | Increase the plan before starting, or record extra work outside the app. Later plan changes intentionally cannot rewrite a protected workout. | GYM-004/005/014/018; adding/removing working sets during execution is explicitly deferred. |
| F04 | Continue the same workout after an unintended Finish. Complete one of two working sets → tap Finish once → remaining rows become read-only. | Pause is available before finalization; no faithful recovery exists after it. | GYM-017 deliberately allows partial/empty Finish and makes it terminal. A pre-Finish safeguard would refine that interaction, not add reopening/history editing. |

Representative evidence: [limited catalog](history/basic-feature-catalog.png), [retained unwanted draft](history/basic-feature-unwanted-draft.png), [partial workout before Finish](history/basic-feature-before-finish.png) and [terminal log afterwards](history/basic-feature-finished-log.png). These captures record the unchanged review baseline; they do not depict proposed controls.

## Challenge and recommendations

The coordinator accepts the challenger's dispositions below. Keep/shrink means retain a recommendation for product review, not implement it. None is a confirmed regression against the current specification.

| ID | Strongest challenge | Evidence that survived the challenge | Disposition |
| --- | --- | --- | --- |
| F01 | The catalog is intentionally illustrative; an editor duplicates a pending native choice and requires stable comparison identities. The user's actual unsupported movements have not been established. | Renaming a session to Cable row session and saving leaves its exercise as Leg press. Neither naming nor setup notes creates a faithful movement identity. | **Already proposed.** Reuse native-plan §9's local exercise editor decision; do not add another backlog item or silently approve it. |
| F02 | Ignoring/repurposing a draft is usable today; retention protects work. A removal feature could expand into saved-plan deletion or interrupt every Back action. | Creating wanted and unwanted drafts, then repurposing the latter, retains both. No selective abandonment exists. Exported JSON preserves both distinct drafts. | **Shrink.** Recommend explicit discard of only an uncommitted new-plan draft. Preserve normal Back retention and exclude saved-plan deletion or reverting existing-plan edits. |
| F03 | Execution set variation is explicitly deferred. Planned counts already support the preparatory task. Actual-versus-prescribed counts, stable rows, exports and metrics need more than a new Add button. No extra physical set was observed. | Completing an additional warm-up leaves Working at 1/2. Changing the saved prescription to three after Start leaves the protected workout at two; the faithful logging gap is real. | **Defer.** Acknowledge the limitation; reopen scope only if actual working-set variance is needed. Neither warm-up relabeling nor retrospective prescription mutation is an acceptable workaround. |
| F04 | Deliberate partial Finish is legitimate; confirmation costs an extra tap. Clearer Finish session wording could reduce ambiguity. Accidental-tap incidence is unknown. | Pause/Resume works before finalization, but one tap on Finish makes the same dated partial session terminal. Reopening the date/exercise does not restore editing. Copy alone cannot intercept a mistaken tap. | **Keep as a recommendation.** Confirm Finish only when working rows remain unfinished across the whole session. Keep partial/empty finishing valid and Finished terminal. |

The two new additions worth considering first are **discarding an unwanted unsaved new plan** and **guarding incomplete terminal Finish**. Both can reuse existing secondary actions/modal patterns and avoid permanent controls in the main set log. The local exercise editor remains the existing native product choice. Extra working-set logging remains an acknowledged later decision, with no claim that an equivalent already exists.

## Smallest acceptance scenarios if selected

| Candidate | Required observable outcome; these tests are proposed, not claimed run |
| --- | --- |
| F01 | Create/select/log Cable row as a local stable identity; its measurements never merge with Leg press. Rename it without changing recorded identities. Keep creation in planning and decide optional local video separately. |
| F02 | With another wanted draft and an active workout, Discard new draft names the selected uncommitted plan. Cancel retains everything. Confirm removes only that draft from the library and subsequent status export; saved plans, active ID, other drafts, logs and timers remain intact. Do not delete previous manual exports or add a confirmation on ordinary Back. |
| F03 | If reopened, append an initially unrecorded working row only to an unfinished workout. Preserve the original two-set target while three actual working rows retain their IDs through reorder/navigation/export. Warm-ups remain excluded, no extra attendance visit appears and Finished stays locked. Resolve count presentation and real-history inclusion before metric acceptance; do not assume deletion or template changes. |
| F04 | With unfinished rows, show whole-session completion and the terminal consequence, with initial Cancel focus. Opening/Cancel/Escape leave lifecycle, entries and independent clocks untouched; running clocks continue until confirmation. Explicit Finish from In progress or Paused, including zero logs, locks the session and settles clocks without completing rows or inventing attendance. |

Before selecting F02/F04, weigh their concrete benefits against the additional action/confirmation. These walkthroughs establish possible blocked tasks and consequences, not how often the user will need them.

## Verification and limits

The proposer produced 46 captures across its completed main/focused walks. The challenger produced 25 baseline/planning captures and five focused challenge captures; its final challenge exercised all four candidate paths and alternatives without script or page errors. The coordinator visually inspected the representative candidate images and the challenger's protected-workout and terminal-session evidence. The challenger parsed the actual downloaded JSON, CSV and TXT with ordinary readers. These checks establish the recorded tasks, not complete format compliance or restore functionality.

This review did not repeat the app regression suite because no runtime source changed. Reordering was exercised through its keyboard interaction; touch drag, the complete plot gesture matrix and elapsed/background timer accuracy were not reverified. The fixed clock permits interaction testing, not realistic timer-duration evidence. Theme/language/width coverage was selective, not a full matrix. First-install, persistence, local-video and process-relaunch verification remain native implementation work.

No new GYM requirement or specification revision is created by recommendations. If selected, each behavior needs acceptance criteria in the specification before source changes, then both requirement-to-source and source-to-requirement entries in [traceability](../product/traceability.md). Native coverage remains not started.
