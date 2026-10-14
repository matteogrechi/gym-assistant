# Native portable files and local backup sidecar

Specification revision 34; GYM-010/012/020/023/024/027. The native version-3 exports implement the [native data contract](native-data-v3.md). The browser's [version-2 files](data-formats.md) remain separate. This document describes export and backup, not a restore UI, migration or cloud service.

## APIs and authority

[`PortableFiles`](../../android/app/src/main/kotlin/it/matteogrechi/gymassystant/data/PortableFiles.kt) is Android-independent:

- `snapshot(dataset, clock): Dataset` calls `Training.portableSnapshot`. It settles and pauses a copy of both occurrence timers, removes monotonic/boot anchors, retains `capturedRunning` and interruption metadata, and removes `backupTreeUri`. Live clocks and the repository row are unchanged.
- `json(dataset, clock, appVersion): String` returns the complete UTF-8 version-3 envelope. `readJson(text): Dataset` validates a generation for backup verification; it is not an importer.
- `csv(dataset, today): String` uses the shared pure domain performance/attendance functions. All recorded reference weights and exercise/setup contexts are included regardless of selected chart/preferences. Warm-ups, missing actuals and invalid working sets do not become performance; qualifying sets from unfinished sessions are included.
- `guide(): String` returns a standalone UTF-8 recovery/dictionary guide with standard Python JSON/CSV reading examples.
- `generationName(dataset): String` provides the canonical UUID/revision filename. `readText(input)` bounds and decodes UTF-8. `copyMedia(input, output, metadata)` and `verifyMedia(input, metadata)` stream size/hash verification; callers own/close their streams.

The transactional Room `StoredDataset` is authoritative. Its `revision` and `backupRevision` form the durable outbox. There is no second mutable external-state store. Dataset mutations enqueue [`BackupWorker.enqueue(context)`](../../android/app/src/main/kotlin/it/matteogrechi/gymassystant/data/BackupWorker.kt); initialization/relaunch can enqueue the same hook to reconcile pending work. Each worker initializes the repository and reads/decodes the newest stored row rather than a revision embedded in a queued job. An already acknowledged revision can be skipped. A newer mutation remains pending if an older in-flight revision is acknowledged.

## JSON envelope and media layout

```text
selected local document tree/
  gym-assystant-<dataset UUID>-r<revision>.json
  gym-assystant-<dataset UUID>-r<previous revision>.json
  gym-assystant-pending-<random UUID>.json        # verified before publication
  gym-assystant-unverified-<random UUID>-<original name>  # preserved conflicting file
  media/
    <64 lowercase hex SHA-256>
    gym-assystant-pending-<random UUID>
    gym-assystant-unverified-<random UUID>-<original hash>
```

The top-level keys are `format: "gym-assystant-status"`, numeric `formatVersion: 3`, `dataKind: "user_data"`, `datasetId`, numeric `revision`, UTC ISO `exportedAt`, IANA `timeZone`, diagnostic `appVersion`, `dataDictionary`, and `dataset`. The payload preserves all saved/inactive plans, independent drafts, catalog, active identity, effective schedules, protected workouts, original owner/revision/prescriptions, set identities/order/kinds/raw nullable values/observations, setup, attendance first visits, settings and media metadata. Exact `BigDecimal` kg values remain ordinary JSON numbers; locale never formats them. Missing values are JSON null, not invented zeros. The app-private folder URI is stripped; its label is informational and grants no permission.

Videos are separate ordinary files, with relative `media/<sha256>` references and original filename, MIME, size and exercise association in JSON. No base64 or absolute device path is emitted. A manual JSON download needs those separate media bytes to recover videos. The worker copies each unique content hash once, reuses existing files only after streaming hash/size verification, and preserves conflicting files under unique quarantine names before publishing a verified replacement. It checks all referenced media before acknowledging the JSON generation. Media is not pruned, protecting references from retained/manual generations.

Text exports/readback are bounded at 16 MiB; JSON verification rejects nesting deeper than 64 and malformed UTF-8. CSV analysis spans at most 36,600 civil days. Videos are bounded at 512 MiB per clip and use a 64 KiB buffer for copying and reopened verification. Large media is never loaded into a byte array. These bounds fail the export/backup rather than truncate records or change private data.

## Fixed CSV schema

Every field, including the English header, is quoted; embedded quotes double, Unicode/newlines remain literal, comma separates fields and CRLF terminates records. Numeric values use decimal points. Empty values mean unavailable, with `missing_reason`; zero remains a valid observed value only where the shared domain calculation returns it.

The fixed header order is:

```text
record_type,source,dataset_id,revision,occurrence_id,plan_id,plan_revision_id,plan_name,session_id,session_name,planned_date,actual_date,last_actual_date,actual_instant,actual_time_zone,exercise_id,exercise_name,machine_variant,setup,comparison,metric,reference_kg,value,unit,completed_working_sets,unfinished,missing_reason,delay_days,period_start,period_end,planned_sessions,logged_sessions,late_sessions,attendance_percent,average_delay_days
```

| Record type | Grain and fields |
| --- | --- |
| `performance` | One occurrence/exercise/setup/metric; `best_reps_at_reference_kg` emits each recorded comparable reference kg, or a blank reference with a reason. `heaviest_kg_at_10_reps` and `volume_load` occur once per occurrence/context, with `kg` and `kg-reps` units; best reps uses `reps`. `completed_working_sets` is the shared count. Original planned date and earliest/latest qualifying actual dates remain separate; `actual_instant`/zone identify a qualifying observation on the earliest date. Raw per-set timestamps remain in JSON. Comparisons are `unadjusted`: effort/ROM is unknown. |
| `attendance` | One original obligation through today across full available history, with its original owner/planned date, first-visit actual date/instant/zone and domain-calculated `delay_days`. Missing visits are blank with `not_logged`; no fabricated strength value is emitted. |
| `attendance_summary_28d` | The shared Progress cohort: previous 28 complete local civil days, visits accepted through today. `period_start`/`period_end`, planned/logged/late counts, percentage and mean positive delay. Zero planned sessions gives blank percentage; no late visits gives blank mean, with reasons. |

Every row carries `source: user_data`, dataset identity and revision. Text fields are literal user text; spreadsheet users should import those columns as text. CSV is derived analysis and does not replace full raw-state JSON.

## Verification, retention and failure reporting

`BackupWorker` is a `CoroutineWorker` running local I/O with unique work name `gym-assystant-local-backup` and `APPEND_OR_REPLACE`. There are no network constraints or clients. Exponential retries start at 30 seconds and stop after five total attempts per job. A later commit/relaunch may enqueue a new job; terminal failure never erases the durable pending revision.

[`LocalDocuments.tree`](../../android/app/src/main/kotlin/it/matteogrechi/gymassystant/data/LocalDocuments.kt) requires the supported local external-storage document provider (internal shared storage or local SD). Missing/revoked/unavailable access fails the worker while private logging remains usable. No URI returns success with `not_configured`, leaves `backupRevision` unchanged and does not claim an external backup.

New JSON uses `LocalDocuments.writeVerified` with a unique `gym-assystant-pending-<UUID>.json` name to close, reopen and compare exact bytes. The worker also reopens/parses this staged generation, validating the embedded UUID/revision, paused timers and absent folder permission without requiring a canonical filename yet. Only a fully verified stage is renamed to `gym-assystant-<dataset UUID>-r<revision>.json`, then reopened/parsed again with canonical filename and payload verification. Media follows the same ordering: unique pending name, streaming copy, close/reopen size/hash verification, rename to the exact content hash, then reopen/verify. A provider-added extension is tolerated on staging files; published names must exactly match the documented relative references.

On an idempotent retry, an existing canonical generation is accepted only after validation and comparison with the stored raw revision; elapsed timer snapshots can reflect the earlier capture. Valid JSON for a different revision/raw dataset is not blindly reused. Invalid canonical JSON/media, including partial files left by older worker versions, is retained under `gym-assystant-unverified-<UUID>-<original name>` after the replacement stage is verified. No existing unverified user file is deleted or overwritten. Read-access failures are reported unsaved; they do not trigger quarantine merely because bytes could not be read. Quarantine/publication rename failure reports unsaved without acknowledgement or pruning; prior verified generations remain available.

Process interruption during a new write can leave only a unique pending file, which neither occupies a canonical name nor participates in retention. A later attempt creates a fresh stage from private data. Interruption after quarantine leaves the original bytes preserved and the canonical name available for recovery. Pending/quarantine leftovers are retained; this fix introduces no cleanup or import feature. All media and the published generation must verify before acknowledgement.

Pruning starts only after newest-generation/media verification and acknowledgement. It sorts canonical generation names by embedded matching revision, never modification time or a latest pointer, and verifies two complete generations including their media before deleting older matching app-owned JSON. The pruner never removes invalid/mismatched files, other dataset IDs, manual filenames, or pending/quarantine files. The first backup cannot fabricate a second generation; subsequent verified generations establish the pair. At most 256 candidate generations are inspected per run; excess files remain retained. Prune failure retains extra files and does not invalidate a successful backup. No failure path mutates training data or deletes prior valid generations to make space.

WorkManager progress/output keys are `backup_status`, `backup_revision` and `backup_error` (bounded failure detail); failed attempts also expose `attempt`. Status values are `saving`, `saved`, `up_to_date`, `not_configured` and `external_unsaved`. Retry status lives in progress, terminal failure in output. The UI can observe unique-work state and the persistent revision difference; WorkManager display state is not the durable authority.

## Verification boundary

[`PortableFilesTest`](../../android/app/src/test/kotlin/it/matteogrechi/gymassystant/data/PortableFilesTest.kt) exercises complete Unicode/exact-decimal JSON, retained raw plans/drafts/media, nulls, immutable live clocks, reboot interruption, retry reuse, quoted CSV/all contexts/shared metrics/attendance, malformed/bounded input and streaming media corruption/size/path checks. JVM tests do not prove document-provider failures, WorkManager process death or retention on a physical SD card; those require device/provider exercises. No restore or migration is claimed.

Sidecar validation: eleven portable-file tests passed both in the isolated JVM harness (including a synthetic full 512 MiB copy under a 256 MiB JVM heap) and in the actual app task `:app:testDebugUnitTest --tests it.matteogrechi.gymassystant.data.PortableFilesTest`, which checks compilation against the real Android/WorkManager/DocumentFile APIs. Standard Python `json` (with `Decimal`) and `csv.DictReader` recovered the generated fixture, including exact kg, Unicode/quoted embedded CRLF, raw saved plans/drafts and performance/attendance rows. These checks do not claim provider/device verification.

Retry-fix validation: the actual worker and current domain sources were copied into a clone of the independent integrity-review harness at `/tmp/gym-backup-retry-check`. Seven worker regressions plus the eleven portable-file tests passed. The worker tests cover previously truncated canonical media/JSON, revisions 10→11, valid JSON with mismatched revision, process interruption during media/JSON writes, quarantine/publication rename failure with prior-generation preservation, unreadable canonical files, and extension changes requiring exact published hashes. These are JVM provider doubles, not an actual SAF/provider or process-kill device test; the coordinator owns that check.
