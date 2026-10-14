package it.matteogrechi.gymassystant.data

import it.matteogrechi.gymassystant.domain.*
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.serialization.json.*

// GYM-010/020/024/027: portable text has no Android dependency or presentation selection.
object PortableFiles {
    const val MAX_JSON_BYTES = 16 * 1024 * 1024
    const val MAX_MEDIA_BYTES = 512L * 1024 * 1024

    fun snapshot(dataset: Dataset, clock: ClockReading): Dataset = Training.portableSnapshot(dataset, clock).also { validateMedia(it.media) }

    internal fun validateMedia(media: List<MediaMetadata>) {
        media.forEach {
            require(it.size in 1..MAX_MEDIA_BYTES && it.sha256.matches(Regex("[0-9a-f]{64}")))
            require(it.relativePath == "media/${it.sha256}")
        }
        require(media.groupBy { it.sha256 }.values.all { group -> group.map { it.size }.distinct().size == 1 }) { "Conflicting media sizes" }
    }

    // A retry can reuse the immutable capture made before acknowledgement, despite later clock readings.
    internal fun matchesRevision(captured: Dataset, live: Dataset, clock: ClockReading): Boolean {
        val expected = snapshot(live, clock)
        if (captured.occurrences.size != live.occurrences.size) return false
        val normalized = captured.copy(occurrences = captured.occurrences.mapIndexed { occurrenceIndex, occurrence ->
            val original = live.occurrences[occurrenceIndex]
            if (occurrence.exercises.size != original.exercises.size) return false
            occurrence.copy(exercises = occurrence.exercises.mapIndexed { entryIndex, entry ->
                val source = original.exercises[entryIndex]
                fun matches(saved: TimerState, timer: TimerState): Boolean = if (timer.running)
                    saved.capturedRunning && saved.elapsedMs >= timer.elapsedMs
                else saved == timer.copy(anchorMonotonicMs = null, bootId = null)
                if (!matches(entry.executionTimer, source.executionTimer) || !matches(entry.recoveryTimer, source.recoveryTimer)) return false
                val portable = expected.occurrences[occurrenceIndex].exercises[entryIndex]
                entry.copy(executionTimer = portable.executionTimer, recoveryTimer = portable.recoveryTimer)
            })
        })
        return normalized == expected
    }

    fun generationName(dataset: Dataset): String {
        require(UUID.fromString(dataset.id).toString() == dataset.id) { "Invalid dataset UUID" }
        require(dataset.revision >= 0)
        return "gym-assystant-${dataset.id}-r${dataset.revision}.json"
    }

    fun json(dataset: Dataset, clock: ClockReading, appVersion: String): String {
        require(appVersion.isNotBlank() && appVersion.length <= 100)
        generationName(dataset)
        val portable = snapshot(dataset, clock)
        val envelope = buildJsonObject {
            put("format", "gym-assystant-status")
            put("formatVersion", 3)
            put("dataKind", "user_data")
            put("datasetId", portable.id)
            put("revision", portable.revision)
            put("exportedAt", clock.observation.instant)
            put("timeZone", clock.observation.timeZone)
            put("appVersion", appVersion)
            put("dataDictionary", dictionary)
            put("dataset", DatasetJson.encodeToJsonElement(Dataset.serializer(), portable))
        }
        return envelope.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) { "JSON exceeds 16 MiB" } }
    }

    /** Validates a generation for backup verification; this is not a restore/migration API. */
    fun readJson(value: String): Dataset {
        require(value.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) { "JSON exceeds 16 MiB" }
        var depth = 0
        var quoted = false
        var escaped = false
        for (character in value) {
            if (quoted) {
                if (escaped) escaped = false else if (character == '\\') escaped = true else if (character == '"') quoted = false
            } else when (character) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 64) { "JSON nesting exceeds 64" } }
                '}', ']' -> { depth--; require(depth >= 0) }
            }
        }
        require(depth == 0 && !quoted)
        val root = DatasetJson.parseToJsonElement(value).jsonObject
        require(root["format"]?.jsonPrimitive?.content == "gym-assystant-status")
        require(root["formatVersion"] == JsonPrimitive(3) && root["dataKind"] == JsonPrimitive("user_data"))
        Instant.parse(root.getValue("exportedAt").jsonPrimitive.content)
        ZoneId.of(root.getValue("timeZone").jsonPrimitive.content)
        val version = root.getValue("appVersion").jsonPrimitive
        require(version.isString && version.content.isNotBlank() && version.content.length <= 100)
        require(root.getValue("dataDictionary").jsonObject.isNotEmpty())
        val dataset = decodeDataset(root.getValue("dataset").toString())
        validateMedia(dataset.media)
        generationName(dataset)
        require(root["datasetId"] == JsonPrimitive(dataset.id) && root["revision"] == JsonPrimitive(dataset.revision))
        require(dataset.settings.backupTreeUri == null) { "Portable files cannot grant folder access" }
        require(dataset.occurrences.all { occurrence -> occurrence.exercises.all { entry ->
            listOf(entry.executionTimer, entry.recoveryTimer).all { !it.running && it.bootId == null }
        } }) { "Portable timers must be paused" }
        return dataset
    }

    fun readText(input: InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_JSON_BYTES) { "JSON exceeds 16 MiB" }
            output.write(buffer, 0, count)
        }
        val bytes = output.toByteArray()
        return Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    }

    val CSV_HEADERS = listOf(
        "record_type", "source", "dataset_id", "revision", "occurrence_id", "plan_id", "plan_revision_id", "plan_name",
        "session_id", "session_name", "planned_date", "actual_date", "last_actual_date", "actual_instant", "actual_time_zone",
        "exercise_id", "exercise_name", "machine_variant", "setup", "comparison", "metric", "reference_kg", "value", "unit",
        "completed_working_sets", "unfinished", "missing_reason", "delay_days", "period_start", "period_end",
        "planned_sessions", "logged_sessions", "late_sessions", "attendance_percent", "average_delay_days",
    )

    fun csv(dataset: Dataset, today: String): String {
        validateDataset(dataset)
        val end = LocalDate.parse(today)
        val start = LocalDate.parse(dataset.createdDate)
        require(!end.isBefore(start) && java.time.temporal.ChronoUnit.DAYS.between(start, end) <= 36_600) { "Statistics exceed 100 years" }
        val output = StringBuilder()
        fun append(values: List<Any?>) {
            output.append(values.joinToString(",") { value ->
                val text = if (value is BigDecimal) value.toPlainString() else value?.toString().orEmpty()
                "\"${text.replace("\"", "\"\"")}\""
            }).append("\r\n")
            require(output.length <= MAX_JSON_BYTES) { "CSV exceeds text limit" }
        }
        append(CSV_HEADERS)
        fun emit(fields: Map<String, Any?>) = append(CSV_HEADERS.map { header ->
            when (header) {
                "source" -> "user_data"
                "dataset_id" -> dataset.id
                "revision" -> dataset.revision
                else -> fields[header]
            }
        })
        for (definition in dataset.exercises) {
            val contexts = (Metrics.contexts(dataset, definition.id) + ComparisonContext(definition.id, definition.defaultSetup)).distinct()
            for (context in contexts) {
                val references: List<BigDecimal?> = Metrics.referenceWeights(dataset, context).ifEmpty { listOf(null) }
                for ((index, reference) in references.withIndex()) {
                    for (record in Metrics.performance(dataset, context, reference, dataset.createdDate, today)) {
                        val occurrence = requireNotNull(Training.openOccurrence(dataset, record.plannedDate))
                        val observed = occurrence.exercises.filter { it.definition.id == context.exerciseId && it.setup == context.setup }
                            .flatMap { it.sets }.filter { it.qualifies() && it.observedAt?.localDate == record.actualDate }
                            .mapNotNull { it.observedAt }.minByOrNull { Instant.parse(it.instant) }
                        val base: Map<String, Any?> = mapOf(
                            "record_type" to "performance", "occurrence_id" to record.occurrenceId,
                            "plan_id" to record.planId, "plan_revision_id" to occurrence.planRevisionId, "plan_name" to occurrence.planName,
                            "session_id" to occurrence.sessionId, "session_name" to occurrence.sessionName,
                            "planned_date" to record.plannedDate, "actual_date" to record.actualDate, "last_actual_date" to record.lastActualDate,
                            "actual_instant" to observed?.instant, "actual_time_zone" to observed?.timeZone,
                            "exercise_id" to definition.id, "exercise_name" to definition.name, "machine_variant" to definition.machineVariant,
                            "setup" to context.setup, "comparison" to "unadjusted", "completed_working_sets" to record.completedWorkingSets,
                            "unfinished" to record.unfinished,
                        )
                        emit(base + mapOf("metric" to "best_reps_at_reference_kg", "reference_kg" to reference,
                            "value" to record.bestReps, "unit" to "reps", "missing_reason" to when {
                                reference == null -> "no_recorded_reference_weight"
                                record.bestReps == null -> "no_working_set_at_reference_weight"
                                else -> null
                            }))
                        if (index == 0) {
                            emit(base + mapOf("metric" to "heaviest_kg_at_10_reps", "value" to record.loadAt10,
                                "unit" to "kg", "missing_reason" to if (record.loadAt10 == null) "no_working_set_at_10_reps" else null))
                            emit(base + mapOf("metric" to "volume_load", "value" to record.volumeLoad,
                                "unit" to "kg-reps", "missing_reason" to if (record.volumeLoad == null) "no_qualifying_working_sets" else null))
                        }
                    }
                }
            }
        }
        val summary = Metrics.attendance(dataset, today)
        for (record in Metrics.attendanceRecords(dataset, today)) {
            val occurrence = requireNotNull(Training.openOccurrence(dataset, record.plannedDate))
            emit(mapOf("record_type" to "attendance", "occurrence_id" to record.occurrenceId, "plan_id" to record.planId,
                "plan_revision_id" to occurrence.planRevisionId, "plan_name" to occurrence.planName,
                "session_id" to occurrence.sessionId, "session_name" to occurrence.sessionName, "planned_date" to record.plannedDate,
                "actual_date" to record.actualDate, "actual_instant" to occurrence.firstVisit?.takeIf { record.actualDate != null }?.instant,
                "actual_time_zone" to occurrence.firstVisit?.takeIf { record.actualDate != null }?.timeZone,
                "delay_days" to record.delayDays, "unit" to "days", "missing_reason" to if (record.actualDate == null) "not_logged" else null))
        }
        emit(mapOf("record_type" to "attendance_summary_28d", "period_start" to end.minusDays(28), "period_end" to end.minusDays(1),
            "planned_sessions" to summary.planned, "logged_sessions" to summary.logged, "late_sessions" to summary.late,
            "attendance_percent" to summary.percentage, "average_delay_days" to summary.averageDelayDays,
            "missing_reason" to listOfNotNull(if (summary.percentage == null) "no_planned_sessions" else null,
                if (summary.averageDelayDays == null) "no_late_visits" else null).joinToString(";")))
        return output.toString().also { require(it.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) { "CSV exceeds 16 MiB" } }
    }

    // The hash and byte count, not a provider filename/mtime, determine media identity.
    fun verifyMedia(input: InputStream, media: MediaMetadata) { transferMedia(input, null, media) }
    fun copyMedia(input: InputStream, output: OutputStream, media: MediaMetadata) { transferMedia(input, output, media) }

    private fun transferMedia(input: InputStream, output: OutputStream?, media: MediaMetadata) {
        require(media.size in 1..MAX_MEDIA_BYTES && media.sha256.matches(Regex("[0-9a-f]{64}")))
        require(media.relativePath == "media/${media.sha256}")
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var size = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            size += count
            require(size <= media.size) { "Media size mismatch" }
            digest.update(buffer, 0, count)
            output?.write(buffer, 0, count)
        }
        require(size == media.size && digest.digest().joinToString("") { "%02x".format(it) } == media.sha256) { "Media hash mismatch" }
    }

    private val dictionary = buildJsonObject {
        put("dataset", "Complete raw user state: catalog, media metadata, saved plans, distinct retained drafts, active plan, effective schedules and protected workouts. List order preserves prescriptions/execution order; IDs preserve original ownership.")
        put("sets", "WORKING/WARM_UP; nullable reps/kg are unrecorded, done alone is not performance. Exact decimal kg is a JSON number. observedAt stores UTC instant, actual civil date and IANA zone.")
        put("attendance", "firstVisit is the first qualifying working-set visit; plannedDate remains the original obligation. Undo changes performance, not the recorded first visit.")
        put("timers", "Elapsed milliseconds settled at export, paused, no monotonic/boot anchors; capturedRunning records prior running state. Recovery target remains in prescription.recoverySeconds. Export does not alter live clocks.")
        put("settings", "Language/theme/referenceWeights preserved. backupLabel is informational; backupTreeUri is null and grants must be obtained again.")
        put("media", "Ordinary separate video bytes at media/<sha256>; original name/MIME/size/hash and exercise association retained. No base64, absolute path or network reference.")
        put("comparisons", "Stable exercise/machine identity plus exact setup; kg load convention. Effort/ROM unknown, comparisons unadjusted. Immediate qualifying working sets, including unfinished sessions. Missing values are null/blank, never invented zero.")
        put("metrics", "best_reps_at_reference_kg: highest qualifying reps at each recorded reference kg (reps). heaviest_kg_at_10_reps: highest qualifying kg at exactly 10 reps (kg), not a maximal-effort claim. volume_load: sum(kg * reps) over qualifying sets (kg-reps), not strength/mechanical work. completed_working_sets counts qualifying sets; raw observations remain in JSON.")
        put("attendanceSummary", "attendance rows retain all original obligations through today. attendance_summary_28d uses the previous 28 complete civil days with visits accepted through today: logged/planned * 100 (percent), late means positive actual-minus-planned civil-day delay, average_delay_days is the mean positive delay. No planned sessions/no late visits produces blank unavailable values.")
        put("csv", "Quoted UTF-8 comma fields, CRLF rows, fixed English headers; all comparison contexts and three metrics, attendance records and the previous 28 complete civil days summary independent of screen selection.")
        put("versions", "formatVersion=3 is independent of app/Room versions. This release does not restore/migrate files or reinterpret version-2 fictional history as user data.")
    }

    fun guide(): String = """
        gym assystant — portable files, format version 3

        JSON is the complete raw dataset. Read it with a UTF-8 text editor or Python:
        import json
        from decimal import Decimal
        with open('backup.json', encoding='utf-8') as f:
            envelope = json.load(f, parse_float=Decimal)
        dataset = envelope['dataset']
        print(envelope['dataDictionary'])

        CSV is derived statistics, not a replacement for JSON. Open as UTF-8 in a
        spreadsheet with comma delimiter, quoted fields and decimal points, or:
        import csv
        with open('statistics.csv', encoding='utf-8', newline='') as f:
            records = list(csv.DictReader(f))
        Empty numeric fields mean unavailable; missing_reason explains them.
        Text entered by users is literal text: import text columns as text in spreadsheets.
        Distinct exercise/setup and occurrence identities must not be averaged together.

        Automatic generations: gym-assystant-<dataset UUID>-r<revision>.json.
        Use the validated embedded dataset/revision, never modification time alone.
        Keep JSON and its relative media/<sha256> video files together. Media metadata
        gives byte size and SHA-256 for independent verification. JSON contains no video bytes.
        Manual JSON export needs the separate media folder for video recovery.
        Private Room storage remains authoritative if shared-folder writes fail.
        Two verified automatic generations are retained before older ones are pruned;
        manual exports are not pruned. Exported timers are paused copies and exported
        folder metadata confers no access. This release has no restore UI or migration.
        Keep original older-format files; version-2 samples are not genuine user history.
    """.trimIndent() + "\n\nData dictionary\n\n" +
        dictionary.entries.joinToString("\n\n") { (key, value) -> "$key: ${value.jsonPrimitive.content}" } +
        "\n\nCSV header order (fields are quoted in the file):\n" + CSV_HEADERS.joinToString(",") + "\n"
}
