# Android implementation plan — gym assystant

Status: design presented before native coding, 13 October 2026; specification revision 32. The user has requested implementation and entirely offline operation. No Android sources, manifest, Gradle scaffold, database, player or APK exists yet. Two isolated planning reviewers examined data durability and native runtime/UI; the coordinator owns these recommendations and their scope decisions.

The [specification](../product/spec.md) remains the product source of truth. [Traceability](../product/traceability.md) distinguishes implemented browser evidence from planned Android work. The reviewed mockup supplies interaction/layout references, not production code. No WebView conversion is planned.

The subsequent [basic feature review](../reviews/basic-feature-review.md) challenges proposed additions against actual mockup workflows. The user subsequently selected draft discard, incomplete Finish protection and local exercise creation in specification revision 30. Other pending choices in §9 remain unresolved.

## 1. Offline contract and boundaries

All runtime tasks work without connectivity, including first launch: create/edit programs, schedule, log working/warm-up sets and setup, reorder, run both timers, view supplied local exercise videos, calculate statistics, change settings and write/read local files. There is no account, backend, sync, analytics, advertising, remote font or video URL. The merged release manifest must contain no INTERNET permission, including permissions contributed by dependencies. No connectivity gate appears in any app workflow.

App data is excluded from Android platform cloud backup using version-appropriate manifest/extraction rules. User-controlled local exports are the recovery path. Android backup behavior differs by platform/manufacturer, so exclusions and restore behavior require device tests; this does not promise control over other apps copying a file. [Android backup configuration](https://developer.android.com/identity/data/autobackup)

The system picker can expose cloud providers. Request local-only content and validate local provenance; initially support tested Android local shared-storage roots, including an available SD card. Reject an unverifiable remote provider for automatic backups. Provider removal/revocation can prevent an external backup, but cannot prevent local gym logging. [Local provider flag](https://developer.android.com/reference/android/provider/DocumentsContract.Root#FLAG_LOCAL_ONLY)

Toolchain/SDK/dependency acquisition may require connectivity during development. Once installed, the app needs none. Local videos must already have been supplied; offline operation cannot produce an unavailable clip or grant rights to somebody else's material.

## 2. Technology decisions

| Area | Choice and rationale |
| --- | --- |
| Platform | Native Android, phone-first, usable in landscape and resizable windows; Kotlin rather than a browser wrapper or cross-platform runtime. |
| SDK range | Proposed minimum API 26 (Android 8); compile/target API 36 initially. This supports java.time directly and the verified current Play submission baseline. Test on minimum, API 33 locale behavior and API 36; reassess build compatibility at scaffold creation. Older phones would require revisiting this choice. |
| Identity | Display name gym assystant; application ID `it.matteogrechi.gymassystant`; first native app version 0.1.0. Interchange format and Room schema versions are independent of app version. |
| UI | Jetpack Compose and Material 3; one activity, native navigation/back handling and system dialogs. Native dp/sp/insets replace browser CSS pixels. |
| State | Unidirectional data flow: UI events → ViewModel → repositories/domain rules → Room → observed Flow/StateFlow. Lifecycle-aware collection; composables do not mutate the database directly. |
| Modules | `:app` for Android/UI/data adapters and `:domain` for pure Kotlin training, clock, calendar and metric rules. Feature packages, not one Gradle module per screen. |
| Dependency injection | Small explicit application container and ViewModel factories. No DI framework until a demonstrated need appears. |
| Runtime persistence | Room/SQLite transactions and indexed queries. A user-selected folder is a backup destination, not a second live database. |
| Settings | Persist requested language/theme and comparison preferences locally; bridge per-app locale APIs. Avoid an additional generic preference store duplicating the same settings. |
| Text interchange | Kotlin serialization plus explicit validators/migrations; UTF-8 JSON, CSV and TXT. Decimal kg values use BigDecimal in domain calculations and canonical decimal representation. |
| Backups | One serialized snapshot writer with a durable pending revision/outbox; WorkManager for bounded retries and launch reconciliation. No network constraints or periodic online job. |
| Media | Media3 ExoPlayer with local data sources and accessible controls; app-owned imported files. No streaming/HTTP playback or background media service. |
| Charts | Focused Compose Canvas renderer with semantic observation targets, built around pure metric/geometry models. Avoid a general chart package with unused capabilities or inconsistent interactions. |
| Resources | Bundle existing brand font after verifying its redistribution terms, vector icons and all translations. Include relevant third-party notices; system font fallback is local. |
| Build | Gradle Kotlin DSL, wrapper and pinned version catalog. Select mutually compatible stable Kotlin/Compose/AGP/Room/Media3 versions when scaffolding; no dynamic dependency versions or preview SDK dependency. |
| Delivery | Build/installable APK with documented checks. Source goes to the existing repository using Conventional Commits. Play publication, release signing credentials and public release uploads are separate delivery decisions. |

These choices follow [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations). API 36 is the verified current new-app/update Play baseline, not a claim that store publication is requested. [Target API requirement](https://developer.android.com/google/play/requirements/target-sdk)

Proposed structure:

```text
android/
  settings.gradle.kts, build.gradle.kts, gradlew, gradle/
  app/src/main/
    AndroidManifest.xml
    kotlin/it/matteogrechi/gymassystant/
      app/                 # activity, navigation, dependency container
      data/                # Room, repositories, JSON/CSV, backup, media
      ui/components/       # shared controls and editor/log/chart pieces
      ui/theme/            # palette, typography, shapes
      feature/             # train, plans, session, exercise, progress, settings
    res/                   # English/Italian/Spanish, icons, bundled font
  app/src/test/, app/src/androidTest/, app/schemas/
  domain/src/main/kotlin/, domain/src/test/kotlin/
docs/product/              # requirements and actual coverage
prototypes/web/            # retained design reference, not native source
```

## 3. UI and navigation decisions

Keep the current amber/deep-teal/ivory brand, calm copy and reps-first hierarchy. System/light/dark options use the branded palettes; wallpaper-derived dynamic colors are disabled. Native Material components supply shapes, pressed/focus states and accessibility. Full English, Italian and Spanish include errors, plurals, descriptions and playback labels; user-entered names/notes are never translated. Dates/numbers are locale-aware, files remain language-independent. AppCompatActivity hosts Compose when using the compatibility per-app locale API. [Per-app language implementation](https://developer.android.com/guide/topics/resources/app-languages)

Train/Plan/Progress stay at the bottom; Settings remains in the top-right overflow. Back restores the actual prior route/scroll rather than starting a new session. Persist lightweight navigation/date/exploration state separately from durable domain data; ViewModel memory is not persistence. Support keyboard/IME avoidance, rotation and expanded font sizes. No fixed portrait lock or fake phone frame/development view reference appears in production.

Reuse shared components for app scaffolding/top bars, button emphasis, a single text-only timer/metric selector, disclosure rows, form fields, working/warm-up set rows, persistent plan editor actions/status, date context and chart controls. Shared components accept data/events, while repositories retain training semantics. Standard targets are at least 48dp and disclosures at least 56dp; type labels use native readable scaled typography. [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)

The seven-column week retains the user's fitted-width requirement. On narrow phones this necessarily preserves the documented calendar width exception; targets do not overlap, retain generous height, and each date is keyboard/TalkBack selectable. Week arrows and Today retain full targets. The home card has the same reserved dimensions for session/rest states at a given window/font configuration; increase its common height when font scaling requires it, rather than clipping text or making only rest days shorter.

Exercise keeps Back/name, exceptional date/original-plan context, concise Paused/Finished status, timer at the top, target, combined sets, seat/setup and secondary video. Session lifecycle actions are available only on Session; Exercise Back retains live timers and data. Ready/In progress have no empty action row. Show Working: done/total, retain distinct readable Warm-up/Working labels and emphasize reps. Finished is readable and terminal. Plan browsing defaults disclosures collapsed, lists date exceptions, explicitly unlocks editing and uses one visible Save plan/status group; invalid fields are revealed and focused. Saved plan browsing, draft edits, saving and activation remain distinct.

Drag handles change execution order independently from prescriptions. Preserve long-press drag, cancellation/autoscroll and identity; provide accessible Move up/down actions without restoring visible arrow-button clutter. All modal surfaces follow the actual app window and visible insets, with internal scrolling and focus restoration. No calendar popup is added.

## 4. Persistence, scheduling and actual records

Room is authoritative while the app runs. Commit validated log edits, completion, seat/setup, order and lifecycle transitions transactionally; retain editor drafts without pretending that an incomplete numeric input is a performed measurement. Unsaved plan drafts survive closing the app but never become saved/active automatically. Save validates and creates an immutable plan revision; activation remains the named Cancel-first confirmation. Persist stable UUID identities for programs, revisions, templates, entries, sets and dated occurrences; renaming/reordering never changes identity.

Data groups: exercise definitions/variants; saved plan revisions and separate drafts; effective schedule intervals/date exceptions; frozen obligations; execution occurrences with original prescription/recovery/order; working/warm-up set records; actual attendance; timer anchors; language/theme/comparison preferences; media metadata; dataset/backup revisions. Immutable plan/occurrence snapshots may be versioned JSON columns; index the identifiers and dates needed for calendar/history queries. Keep all recorded history; 28 days is the attendance display cohort, not data retention.

Store planned civil dates separately from UTC timestamps and actual local observation dates/time zones. Time-zone/DST changes do not move an already recorded visit to a different day. Preserve schedule effective dates so opening the app after weeks offline reconstructs obligations from the program/revision that applied then. Never invent obligations before initial plan activation or rewrite old missed sessions from a newly active plan. Start/log/reorder protects an occurrence's original plan, prescription and timer target. Untouched today/future previews can reflect the newly saved/activated plan.

Start → In progress, Pause → Paused, Resume → In progress, Finish → Finished remain the only transitions. Finish can be partial/empty, locks edits and never creates unperformed sets or attendance. Before incomplete Finish, confirm whole-session completion and terminal meaning; opening/cancelling leaves clocks and lifecycle unchanged. A never-saved new-plan draft may be explicitly discarded without affecting saved plans or workouts. No automatic rotation/rescheduling, prescription advice, streak or reopening Finished in v1.

Start, warm-ups, zero reps, future previews and Finish do not establish attendance. Record the first actual visit once with the first qualifying completed working set, atomically with completion. The current first-observation rule remains: undo changes working performance, but does not erase the already recorded visit. Retrospective attendance/history correction is a separate scope choice.

Native starts with no fictional completed history. Supplied catalog/program examples, if included, contain no measurements and are clearly examples. Actual reps/kg begin unrecorded; a target is not copied into a performed result. Proposed exercise definitions may supply explicitly configured default machine setup, while session-specific notes remain snapshots; there is no silent last-session carryover.

## 5. Timers

Each exercise occurrence owns independent execution and recovery clocks. Recovery uses its saved plan target (1–3600 whole seconds, default 90); Reset uses that original target. Persist timer actions before acknowledging success. Session Pause/Finish settles all running timers at one timestamp in the same transaction. Timer selection does not start/stop either clock and session Resume does not automatically resume clocks.

Use monotonic `elapsedRealtime`, which includes sleep, for duration rather than the device's wall clock. Same-boot backgrounding/process death does not stop a started timer; reconstruct it from its durable anchor on return, clamp recovery at zero, and show completion. UI ticks redraw only; no database write each second, continuous timer service, exact alarm, sound, vibration or notification. [Android clocks](https://developer.android.com/reference/android/os/SystemClock)

Use boot identity plus monotonic anchors to detect restart. Chosen conservative policy: after reboot or an unverifiable boot, restore running clocks paused at the last durable checkpoint with a clear recovery message; do not guess powered-off time from UTC. Keep the session's lifecycle/sets intact. Portable imports likewise restore timers paused. Explicit actions and background checkpoints persist settled elapsed values; an abrupt reboot can leave time since the checkpoint unrecoverable. The boot count API supports detection, not precise cross-reboot duration recovery. [BOOT_COUNT](https://developer.android.com/reference/android/provider/Settings.Global#BOOT_COUNT)

## 6. Real statistics and explorable plots

Keep the existing metrics: best working reps at a chosen recorded kg, heaviest completed set at exactly 10 reps, volume load (kg × reps) and completed working sets. Warm-ups and invalid/unfinished sets are excluded. Attendance and performance remain separate; late/missed training never becomes a strength zero. The 28-day attendance cohort and civil-day delays keep their current definitions.

History inclusion is a product choice below. Recommended: query qualifying completed working sets immediately, including partial/paused sessions, and lock them on Finish; do not manufacture a second copied history on every Finish. Undo/valid edits before Finish update the same records. Display when an observation is still from an unfinished session. Alternative: only Finished occurrences appear in performance, while attendance remains immediate. These policies need distinct tests and cannot be silently mixed.

Record actual set observation timestamps/local dates alongside original planned dates. Selected-session readouts identify actual versus planned dates when they differ, and date spans when a workout spans days. Per-occurrence metric values remain per occurrence. Same-day catch-up observations share a date group with conditional previous/next-session selection in the readout; retain individual values and identities rather than inventing x offsets or silently averaging them.

Reference weight is selected from actually recorded loads and remembered for that exercise/context; its initial default is an available recorded load, not a recommended load. Comparisons use identical exercise/machine/setup/load convention. Raw setup text is retained; only documented textual normalization is used for matching. Context selection appears only when multiple comparable groups exist. No RIR=2 or full-ROM assumption is inherited from fictional fixtures: absent effort/ROM remains unknown and comparisons say unadjusted. No e1RM, aggregate strength score or workload-as-muscle-growth claim.

The native chart keeps zero-based values, civil-date spacing, missing-data gaps, a selected ring/guide and exact readout, with 48dp semantic targets. First measurable layout reveals selection once; subsequent metric/navigation changes retain deliberate exploration. Drag the plot to inspect nearest actual observation; pan the separate date lane; 100–400% temporal zoom/reset leaves metrics/value scale unchanged. Directional gesture arbitration preserves vertical scrolling; accessible observation actions and keyboard endpoints remain available. Gap targets cannot intercept the date lane. Empty/single histories retain honest unavailable/disabled states.

## 7. Offline video design

Recommended first capability: attach a local video while managing an exercise, using a local-only system document selection. Copy into a temporary internal file, enforce a proposed 100MB per-clip limit, validate format/playback and only then replace the exercise association. Cancellation, unreadable/unsupported files or insufficient space preserve the previous clip. After import, playback is independent of the source provider/file. Associate video with the machine/movement variant; historical snapshots retain stable media identity. No guessed substitutions between different machines.

Open the existing secondary video action in a native contained panel with Play/Pause/seek and Close, no autoplay. Pause playback when the panel closes or the app backgrounds; release resources. Session timers remain independent. Prefer MP4/H.264/AAC for interoperability, while reporting actual unsupported codecs honestly. Imported speech/subtitles are not automatically translated. [Media3 format support](https://developer.android.com/media/media3/exoplayer/supported-formats)

A bundled starter library is optional, pending a source/license/variant decision. Do not ship placeholders labelled as playable clips or silently fetch YouTube. JSON describes video metadata/relative references, not video bytes. If selected, local backup copies each referenced media file separately once by content hash; old media still referenced by snapshots is retained. Missing restored media does not block plans/logging and is explicitly unavailable.

## 8. Text backups and recovery

Local database saving and backup completion are different states. Saved on device means a successful Room transaction. Backup pending/verified reflects the selected folder's latest verified dataset revision. Ordinary successful logging does not spam status; surface a persistent failure/recovery action when the mirror falls behind or access is lost.

Each accepted change advances the dataset revision atomically with its pending backup intent. A single writer reads a consistent snapshot, performs provider I/O outside the database transaction and may coalesce rapid edits without losing the pending latest revision. Start/pause/reset/background settle timer state for a portable paused snapshot without stopping the live clocks. Pending backup work is reconciled after process death; bounded local retries do not block the workout.

Use uniquely named full JSON generations, close/reopen and validate/hash the bytes before marking that revision verified. Retain the two newest verified automatic generations and prune only older app-owned automatic generations after a replacement is verified. Explicit manual exports/pre-restore recovery files are retained. A latest pointer is convenience, never the only recovery record. Do not depend on provider-wide atomic rename/replacement guarantees or provider timestamps. [SAF access/grants](https://developer.android.com/training/data-storage/shared/documents-files)

CSV exports all existing performance/attendance metrics with clear source/grain/units and planned/actual dates; UTF-8, quoted RFC4180-style cells, CRLF, stable English headers and decimal points. Full raw-state JSON and a UTF-8 TXT dictionary/recovery guide remain readable without this app. Both manual export and automatic backup are nonmutating. Room schema migrations are tested and never use destructive fallback. [Room migration guidance](https://developer.android.com/training/data-storage/room/migrating-db-versions)

Proposed restore capability: select a local JSON snapshot/folder, validate supported version, bounds, IDs/references/dates/numbers/media and show a counts/identity summary before explicitly replacing the whole dataset. First preserve current data in a verified local pre-restore snapshot; failure cancels replacement. Apply a validated import in one transaction, pause imported timers and request folder access anew. No automatic import of externally edited files or merge/conflict feature. Malformed/unsupported files leave current data untouched. Preserve unknown additive fields in explicit extension payloads; refuse unknown semantic versions.

The [draft native v3 contract](native-data-v3.md) distinguishes genuine data, set timestamps and schedule intervals from legacy demo provenance. Existing v1/v2 remain readable and retain source labels; importing a demo cannot count its fictional visits as genuine attendance. UI/live migrations do not overwrite the original input.

## 9. Product choices presented before coding

| Choice | Recommended default | Scope status |
| --- | --- | --- |
| Video content | Local import first; bundled library only with supplied/cleared clips. | User preference requested; basic offline playback follows GYM-002/027, catalogue content is unresolved. |
| Real-history inclusion | Qualifying completed working sets appear immediately, including unfinished/partial workouts; Finish locks them. | User preference requested; alternative Finished-only documented. |
| Local exercise creation | Plan → Exercises shared list/read-only details and the same creator available independently or from a session (GYM-028). Create name, stable machine/movement identity and optional default setup. No new tab, definition editing/deletion or implied video import. | Creation approved in revision 30; library placement in revision 31. Working mockup, native source not started. |
| Restore UI | Local whole-dataset restore with preview and replacement confirmation, to make backups useful after reinstall. | Proposed native scope extension; explicit preference requested. |
| Device range | Android 8/API26 and later. | Proposed technical baseline; change if the user's device is older. |
| Reboot clocks | Pause at last durable checkpoint and explain recovery; do not claim exact powered-off timing. | Recommended implementation policy, presented for review. |
| Media limit/backup | 100MB per clip; copy referenced media separately with its JSON metadata. | Proposed technical policy; larger supplied clips may require revisiting the bound. |

Unanswered product extensions remain proposals; elapsed time is not approval. Core implementation can proceed with existing requirements while optional extension decisions remain isolated. Finalize native acceptance criteria/format semantics for the chosen branches before implementing those branches. This plan is the requested presentation before code, not a request for another generic implementation authorization.

## 10. Implementation sequence and acceptance gates

| Milestone | Concrete delivery | Validation before continuing |
| --- | --- | --- |
| M0 — native specification | Record chosen product branches, native semantics, v3 dictionary/migrations, requirement ↔ planned source/test inventory and exclusions. | Both directions reviewed; every new feature has explicit authorization; no browser/native coverage conflation. |
| M1 — runnable foundation | Toolchain, two modules, pinned Gradle build, app identity, local resources, theme/languages, native scaffold and reusable components. | Build/lint/unit checks; offline clean first launch; merged manifest has no INTERNET; locale/theme/rotation/insets/large-text checks. |
| M2 — durable domain | Room schema/repositories, plan revisions/drafts/effective schedules, execution/set/attendance records and clock abstraction. | Port the 35 existing invariant fixtures to Kotlin; transactions, migration/failure/process-relaunch tests; multi-week absence and activation history. |
| M3 — plan/calendar | Library, safe read-only/Edit/Save/activate flows, prescriptions/recovery, fitted calendar/pending/future sessions and selected scope for exercise definitions. | Draft isolation, protected workouts, cancellation, collapsed validation recovery, date override review and reorder accessibility. |
| M4 — gym execution | Lifecycle, unified set logging, setup, reorder and both durable timers; supplied offline video import/playback. | Pause/Resume/Finish atomicity, warm-up exclusion, future-context honesty, independent clocks, same-boot kill/sleep/clock changes, reboot recovery, failed import/source deletion. |
| M5 — progress | Chosen real-history policy, exact existing metrics, delayed attendance and native explorable charts/context/reference selection. | Known-value fixtures, partial/undo/Finish semantics, same-day/spanning-day records, gaps/dense histories, gesture/keyboard/TalkBack exploration and retained view. |
| M6 — independent recovery | Automatic JSON generation mirror, CSV/TXT, local folder failures and selected restore capability/media recovery. | Unicode/decimal standard-reader round trips, malformed/unsupported files, revoked/removed/full storage, crash mid-write, prior-good generation, paused import and read-only exports. |
| M7 — release candidate | Installed APK, full offline scenario walkthrough, required source/spec/test traceability, updated native screenshots and installation notes. | API26/33/36 matrix, physical device when available, airplane mode from clean install, all languages/themes, 200% font/landscape, full session/backup/relaunch; distinguish actual device checks from unrun ones. |

Use focused fresh-context workers for independent domain/storage/UI slices once their contracts are fixed, with disjoint ownership and one coordinator integrating. No parallel agents edit the same unresolved schema or shared component. Each slice gets a Conventional Commit with its GYM IDs and real verification; push to `matteogrechi/gym-assistant` within existing authorization. Preserve the working browser reference.

## 11. Build readiness and remaining exclusions

Read-only environment check: java, javac, adb, sdkmanager, emulator and gradle are not on PATH; ANDROID_HOME and ANDROID_SDK_ROOT are unset. An existing installation elsewhere has not been ruled out. M1 must locate or provision the required JDK/SDK in an authorized location before claiming a build; SDK installation outside writable roots requires tool approval. No toolchain install was attempted during this planning delivery.

No accounts/cloud, subscriptions/social, exercise prescription/coaching, RIR entry, e1RM, unit preferences beyond kg, history editing/reopening, plan duplication/deletion, automatic missed-session rescheduling, timer alerts, background video or per-set timer analytics are included. Local exercise creation is approved under GYM-028. Restore remains a proposed exception to prior mockup exclusions. Discarding only never-saved new drafts and confirming incomplete Finish are approved GYM-026/017 refinements; saved-plan deletion, definition editing and Finished reopening remain excluded. Installed font/media licenses and real device/toolchain evidence remain implementation work.
