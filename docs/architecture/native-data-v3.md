# Native portable data contract — draft version 3

Status: pre-implementation proposal linked to [the Android plan](android-implementation-plan.md), specification revision 29. No native serializer, importer, migration or database is implemented. The browser keeps emitting the existing [version-2 contract](data-formats.md). Format version 3, if finalized, is independent of app and Room schema versions.

## Why a new version

Version 2 describes fictional prototype histories, reload-reset executions, example media/setup and a recent obligation snapshot. Native records require genuine provenance, durable set observation timestamps, effective schedule intervals, local media references and dataset/backup revisions. Reinterpreting a fictional sample as a real gym visit would be incompatible. Use a new format and explicit migration rather than silently changing version-2 meaning.

## Proposed JSON fields

Keep `format: gym-assystant-status`, UTF-8, explicit dictionary/units, ISO civil dates, UTC ISO timestamps and decimal-point numbers. Use `formatVersion: 3`, `dataKind: user_data`, dataset UUID and monotonically increasing dataset revision. Top-level metadata includes export timestamp/time zone, application version for diagnosis and the dictionary; source/provenance also accompanies imported legacy records. App version never determines how a file is parsed.

| Field group | Contents |
| --- | --- |
| Settings | Requested language/theme and comparison preferences; backup label/metadata is informational. A content URI in a file grants no access. |
| Exercise definitions/variants | Stable identity, name, machine/load convention, configured default setup and optional stable media identity. No invented effort/range-of-motion measurement. |
| Plans/drafts | Saved immutable program revisions, active plan ID, independent retained drafts and globally unique template/entry IDs. Drafts never silently activate. |
| Effective schedules | Revision/activation intervals and explicit date exceptions needed to reconstruct obligations during weeks away. Preserve original effective civil dates/time zones. |
| Obligations/executions | Original plan/template/revision/planned date, frozen prescription/recovery and current execution order/lifecycle; no rewrite from later plans. |
| Sets/setup | Stable set/entry identity, warm-up versus working kind, nullable unrecorded reps/kg, done flag, raw notes/setup context and actual observation UTC/local date/time zone. Decimal kg is serialized as an exact JSON number, not a UI-localized string. |
| Attendance | First qualifying working-set visit date/time zone keyed by original occurrence; planned date remains separate. Warm-ups/Start/Finish alone do not create a visit. |
| Media | File ID, original filename, MIME/size/content hash, variant association and relative backup path. No absolute app-private path is a portable file reference. |
| Portable timers | Mode, original recovery target, settled elapsed values, paused timer state and captured-running metadata. Do not import monotonic/device boot anchors as runnable clocks. |
| Extensions/provenance | Preserve unknown additive fields and original legacy source labels without interpreting them as genuine training. |

The history-inclusion branch and local exercise/restore extensions remain product choices in the plan. No shape above claims that those UI features are approved or implemented. First native actual history is empty; sample/demo input stays distinguishable and does not inflate genuine attendance/performance.

## CSV and TXT

Version-3 CSV keeps a documented, fixed quoted UTF-8 header, comma delimiter, CRLF records, English identifiers, decimal points, units and planned/actual dates. Add explicit genuine/demo source, observation identity/grain and relevant actual time/date/context fields; finalize the exact header alongside the chosen history policy before writing the exporter. Preserve per-occurrence measurements and distinguish same-day observations; never silently average records or rewrite their dates. Null/unavailable values stay blank with a reason.

The UTF-8 TXT recovery guide describes JSON fields, media layout and standard JSON/CSV reading. Statistics are derived and cannot replace full raw-state JSON. No proprietary binary-only recovery, app account, network service or mandatory password is introduced.

## Backup generations and media

Automatic files are full immutable generations identified by dataset/revision. Close/reopen, parse and compare the written bytes/hash before acknowledging a verified backup. Preserve at least two verified automatic generations; only prune app-owned older automatic files after a newer verified generation. Retain manual/pre-restore exports. A provider-specific latest pointer or modification time cannot be the sole recovery mechanism.

Video bytes live as ordinary local media files separately from JSON, identified by hash/relative path and copied once per content identity. No base64 video payload is put inside text state. Missing media is reported without discarding/restoring unrelated logs. Runtime file copies remain app-owned, so removing the original import source cannot break already imported playback.

## Validation, migration and restore

Validate bounded size/depth/counts, supported format, identities/references, enums, civil dates/timestamps, numeric range/finite semantics and media paths before a transactional import. Reject traversal/absolute-path media references and unsupported semantic versions. Validate known fields without erasing unknown additive extensions. Readable old files remain intact; migrations produce a separate representation, never destructively rewrite the source.

Version 1 first uses the existing v1→v2 mapping. Version 2 retains its sample/visit-demo provenance and original IDs/notes/working/warm-up data. Do not invent missing timestamps, schedule intervals, effort or folder permission; preserve uncertainty. Optional restore presents the resulting counts/source and obtains explicit replacement confirmation after preserving the current dataset. No merge import is proposed. Imported timers are paused and folder grants must be obtained again.

The Room schema has its own exported version history and migration tests; no destructive fallback. Room is the runtime authority, JSON is the portable recovery contract. External manual edits are not auto-ingested. A failed folder write/restore leaves device data and prior verified generations available. The eventual tests must recover files with standard JSON/CSV readers as well as the native reader.
