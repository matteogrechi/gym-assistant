# Durable data files — version 2

GYM-023, GYM-024 and GYM-026 define these files. Android implementation is not started. The web prototype downloads real files containing fictional history and this visit's demo edits; it does not save to its preview folder, persist across reloads, or import backups.

## Principles

Use open text files, UTF-8, explicit field names and units, stable IDs, and a format version independent of the app version. Preserve original data as well as derived statistics. JSON stores nested state without flattening away warm-ups, targets, setup, order, or planned versus actual dates. CSV is for analysis, not a complete backup. Neither requires the app to remain maintained.

JSON follows [RFC 8259](https://www.rfc-editor.org/rfc/rfc8259). CSV follows the quoting and record conventions in [RFC 4180](https://www.rfc-editor.org/rfc/rfc4180). Files use decimal points regardless of interface language, ISO `YYYY-MM-DD` civil dates, UTC ISO timestamps, and explicit units (`kg`, `reps`, `kg-reps`, `sets`, `days`, `percent`, `sessions`, `ms`, `seconds`). Null/blank means unavailable, never a fabricated zero observation.

## Full status: `gym-assystant-status-<timestamp>.json`

Top-level keys:

| Field | Meaning |
| --- | --- |
| `format`, `formatVersion`, `exportedAt`, `timeZone` | Stable format identity, integer version 2, UTC capture time, device IANA time zone. |
| `dataKind`, `dataDictionary` | Explicit prototype provenance and a field/semantics guide included inside the file. |
| `settings` | Requested language/theme and preview folder label. Native backups will use non-authoritative folder access metadata; an exported URI cannot grant permission. |
| `savedPlans`, `activePlanId`, `unsavedPlanDrafts` | All saved programs with sessions, prescriptions and weekday/date assignments; one active saved-plan ID; an array of separate per-plan drafts (empty when absent). Drafts, including new unsaved plans, are never silently committed. |
| `exerciseCatalog` | Exercise IDs, machine identity, sample default setup/weight/reps. |
| `executionSessions` | Every opened occurrence, with stable `occurrenceId`, planned `date`, original `programId`, globally unique template identity, original prescription copy, current exercise order, lifecycle, entries and actual attendance/finish dates where present. |
| `pastPlannedSessions` | Frozen recent obligations including original dates, original `programId`, program revision and template snapshots, even when the saved plan changes. |
| `samplePerformanceHistory` | Fictional raw working sets plus machine/setup/load/ROM/effort context and actual observation dates. |
| `sampleAttendanceHistory` | Fictional original program IDs and planned/actual visit dates; execution-session attendance is retained separately. |

Execution exercises contain `sets` (working sets), `warmups`, seat notes (`seat`), prescribed rep range (`target`), and timers. Position in each array is the recorded order. A set's `done` flag is completion, not proof of attendance by itself; the documented positive-rep working-set rule still applies. Original immutable working-set prescriptions are in `prescribedExercises` rather than inferred from edited values. User text is preserved, including Unicode and line breaks.

Timer fields retain `mode`, `duration` in seconds, and `execution`/`recovery` records with `elapsed` in milliseconds and `startedAt: null`. `wasRunningAtExport` records prior running status. Running elapsed time is folded into the snapshot without mutating the original timer or changing session lifecycle; recovery elapsed is capped at duration. A future restore starts timers paused and does not count time spent away as exercise. `inProgress` session state does not implicitly restart a timer.

All current mockup records are demo data. `dataKind` and CSV source labels distinguish fictional past samples from this visit's edits; neither is presented as a real user's training archive.

## Statistics: `gym-assystant-statistics-<timestamp>.csv`

Long-form rows have the fixed header:

`format_version,record_type,source,program_id,exercise_id,machine,setup,load_convention,rom,rir,occurrence_id,template_id,planned_date,actual_date,period_start,period_end,as_of_date,metric,reference_weight_kg,value,unit,missing_reason`

| Record type | Rows and semantics |
| --- | --- |
| `performance` | For every catalog exercise, each dated comparable sample: `reps_at_reference_weight`, `weight_at_10_reps`, `volume_load`. Context columns retain comparison constraints. `source=fictional_sample`; actual date is observation date; unavailable values are blank with `no_comparable_set`. Also one period `completed_working_sets` row per exercise. Current editable logs are excluded exactly as in Progress. |
| `attendance` | One row per protected planned occurrence in the displayed past 28-day cohort. `metric=attendance_status`, value `on_time`, `late`, `early`, or `unlogged`; planned/actual dates remain distinct. Source is `fictional_sample`, `this_visit_demo`, or `planned_demo` for unobserved obligations. Future observed visits do not establish attendance as of today. |
| `attendance_summary` | `planned_sessions`, `logged_sessions`, `on_time_sessions`, `late_sessions`, `early_sessions`, `unlogged_sessions`, `logged_percent`, `mean_delay_days`, matching Progress. `source=sample_and_this_visit_demo`; include cohort start/end and as-of date. Unavailable percentages/delays have blank values with `no_planned_sessions` / `no_late_sessions`. |

Only derived values appear here; full state JSON retains raw logs. Values do not depend on UI language, current chart selection, or rounded display text. Literal quotation marks are doubled, all cells are quoted, and records end in CRLF. Unrepresentable numeric overflow is blank with `numeric_overflow`, not converted into a valid observation.

## Recovery without the app

The Settings recovery-guide download is plain UTF-8 text and includes the version-2 header and dictionary. It includes all saved programs and retained drafts, including inactive ones. CSV program IDs distinguish original plan ownership; attendance cohorts remain historical snapshots rather than being recalculated from a newly active plan. Open JSON in any text editor to inspect raw records. Standard Python can extract sets without the app:

```python
import json
from pathlib import Path
backup = json.loads(Path('status.json').read_text(encoding='utf-8'))
for session in backup['executionSessions']:
    for exercise in session['exercises']:
        print(session['date'], session.get('attendedDate'), exercise['name'], exercise['seat'])
        for kind in ('warmups', 'sets'):
            for row in exercise[kind]:
                print(kind, row['reps'], row['weight'], 'kg', row['done'])
```

Open CSV in a spreadsheet using UTF-8 and comma separation, or `csv.DictReader` in Python. Retain JSON as the complete original; CSV alone cannot restore the plan or logs. Exporting does not mark sets complete, record attendance, finish a session, or apply an unsaved plan.

## Native save and format evolution

On Android, choose a writable location through the [system document picker](https://developer.android.com/training/data-storage/shared/documents-files) and retain the granted URI permission. A folder can later become unavailable; keep current data and report an unsaved state rather than claiming success. Save after committed changes and timer pause/background events. Write a complete new snapshot, verify success, then replace/promote it while retaining a previous valid file; document-provider capabilities determine the safe replacement strategy. No every-second writes.

Version 2 is an exploratory interchange contract. Future incompatible changes increment `formatVersion`, keep earlier files intact, and document how to migrate/read them. Additive fields must not silently reinterpret existing fields. A future reader should preserve unknown fields and clearly reject unsupported versions rather than erase data. The prototype includes no importer or native persistence implementation.

## Version 1 → version 2 recovery

Version 1 backups remain readable. For conversion, keep the original file: wrap `savedPlan` as the single-element `savedPlans` array, set `activePlanId` to that plan's ID, and wrap a non-null `unsavedPlanDraft` as `unsavedPlanDrafts` (otherwise an empty array). Add that original plan ID to execution/obligation/attendance records that lack `programId`; version-1 sample performance records already carry it. Existing occurrence and template IDs remain unchanged; future plans use globally unique IDs. Set `formatVersion` to 2 only after converting and documenting the dictionary. Version-2 CSV adds `program_id` after `source`; an isolated version-1 CSV without its backup has unknown plan ownership, so preserve that uncertainty rather than inventing a plan ID. No importer is implemented in the mockup.
