package it.matteogrechi.gymassystant.data

import it.matteogrechi.gymassystant.domain.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.security.MessageDigest
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class PortableFilesTest {
    private val today = "2026-03-10"
    private val visit = ObservationTime("2026-03-09T12:00:00Z", "2026-03-09", "Europe/Rome")
    private val clock = ClockReading(ObservationTime("2026-03-10T12:00:00Z", today, "Europe/Rome"), 4_000, "boot-1")
    private val kg = BigDecimal("80.125000000000000000001")
    private val setup = "Seat \"4\", café\r\n深い"
    private val clip = "local video bytes — è".toByteArray(Charsets.UTF_8)

    private fun fixture(): Dataset {
        val hash = MessageDigest.getInstance("SHA-256").digest(clip).joinToString("") { "%02x".format(it) }
        val exercise = ExerciseDefinition("exercise", "Presse, \"é\" 深", "Machine 01", setup, "clip")
        val prescription = Prescription("entry", exercise.id, workingSets = 2)
        val session = SessionTemplate("session", "Séance 深", exercises = listOf(prescription))
        val plan = Plan("plan", "plan-r1", "Programme é", listOf(session), dateOverrides = mapOf("2026-03-07" to session.id))
        val inactive = Plan("inactive", "inactive-r1", "Inactive", listOf(
            session.copy(id = "inactive-session", exercises = listOf(prescription.copy(id = "inactive-entry")))))
        val timer = TimerState(250, 1_000, "boot-1")
        val occurrence = Occurrence("workout", "2026-03-07", plan.id, plan.revisionId, plan.name, session.id, session.name, "",
            listOf(ExerciseOccurrence(prescription, exercise, setup, listOf(
                SetLog("warm", SetKind.WARM_UP, 99, BigDecimal("999"), true, visit),
                SetLog("done", SetKind.WORKING, 10, kg, true, visit),
                SetLog("empty", SetKind.WORKING),
            ), timer, timer)), Lifecycle.IN_PROGRESS, protected = true, firstVisit = visit)
        return Dataset("c28cb3bd-0a2b-4666-9d79-a813ca027fdc", "2026-02-01", revision = 12,
            settings = Settings(backupTreeUri = "content://com.android.externalstorage.documents/tree/primary%3AGym", backupLabel = "Local SD"),
            exercises = listOf(exercise), media = listOf(MediaMetadata("clip", exercise.id, "vidéo.mp4", "video/mp4", clip.size.toLong(), hash, "media/$hash")),
            plans = listOf(plan, inactive), activePlanId = plan.id,
            drafts = listOf(PlanDraft(plan.copy(name = "Retained edit", revisionId = "draft-r1"), plan.revisionId)),
            schedules = listOf(EffectiveSchedule("2026-02-01", plan)), occurrences = listOf(occurrence)).also(::validateDataset)
    }

    @Test fun jsonKeepsCompleteRawStateExactNumbersAndUnicodeWithoutChangingLiveClocks() {
        val live = fixture()
        val before = encodeDataset(live)
        val json = PortableFiles.json(live, clock, "test-version")
        val root = DatasetJson.parseToJsonElement(json).jsonObject
        assertEquals(JsonPrimitive(3), root["formatVersion"])
        assertEquals(JsonPrimitive(live.id), root["datasetId"])
        assertEquals(JsonPrimitive(12), root["revision"])
        assertTrue(root.getValue("dataDictionary").jsonObject.isNotEmpty())
        val weight = root.getValue("dataset").jsonObject.getValue("occurrences").jsonArray[0].jsonObject
            .getValue("exercises").jsonArray[0].jsonObject.getValue("sets").jsonArray[1].jsonObject.getValue("kg").jsonPrimitive
        assertFalse(weight.isString)
        assertEquals(kg.toPlainString(), weight.content)
        val recovered = PortableFiles.readJson(json)
        assertEquals(PortableFiles.snapshot(live, clock), recovered)
        assertEquals(live.plans, recovered.plans)
        assertEquals(live.drafts, recovered.drafts)
        assertEquals(live.schedules, recovered.schedules)
        assertEquals(live.media, recovered.media)
        assertEquals(live.exercises, recovered.exercises)
        assertEquals(setup, recovered.occurrences.single().exercises.single().setup)
        assertNull(recovered.settings.backupTreeUri)
        val entry = recovered.occurrences.single().exercises.single()
        assertNull(entry.sets.last().kg)
        assertNull(entry.sets.last().reps)
        for (timer in listOf(entry.executionTimer, entry.recoveryTimer)) {
            assertEquals(3_250L, timer.elapsedMs)
            assertFalse(timer.running)
            assertNull(timer.bootId)
            assertTrue(timer.capturedRunning)
        }
        assertEquals(before, encodeDataset(live))
        assertTrue(live.occurrences.single().exercises.single().executionTimer.running)
    }

    @Test fun rebootCapturePausesAtDurableTimeAndExplainsInterruption() {
        val live = fixture()
        val recovered = PortableFiles.readJson(PortableFiles.json(live, clock.copy(bootId = "boot-2"), "test"))
        val timer = recovered.occurrences.single().exercises.single().executionTimer
        assertEquals(250L, timer.elapsedMs)
        assertTrue(timer.interrupted)
        assertTrue(timer.capturedRunning)
        assertFalse(timer.running)
    }

    @Test fun immutableGenerationCanBeReusedAfterClockAdvancesButNeverForChangedRawLogs() {
        val live = fixture()
        val captured = PortableFiles.readJson(PortableFiles.json(live, clock, "test"))
        assertTrue(PortableFiles.matchesRevision(captured, live, clock.copy(monotonicMs = 9_000)))
        assertFalse(PortableFiles.matchesRevision(captured.copy(revision = 13), live, clock))
        assertFalse(PortableFiles.matchesRevision(captured.copy(plans = captured.plans.map { it.copy(name = "Changed") }), live, clock))
        val altered = captured.copy(occurrences = captured.occurrences.map { occurrence ->
            occurrence.copy(exercises = occurrence.exercises.map { it.copy(setup = "Changed") })
        })
        assertFalse(PortableFiles.matchesRevision(altered, live, clock))
    }

    @Test fun csvQuotesEveryFieldUsesCrLfAndExportsSharedMetricsAttendanceAndNulls() {
        val live = fixture()
        val csv = PortableFiles.csv(live, today)
        assertTrue(csv.startsWith("\"record_type\",\"source\""))
        assertTrue(csv.endsWith("\r\n"))
        assertTrue(csv.contains("\"Seat \"\"4\"\", café\r\n深い\""))
        val rows = parseCsv(csv)
        assertEquals(PortableFiles.CSV_HEADERS, rows.first())
        assertTrue(rows.all { it.size == PortableFiles.CSV_HEADERS.size })
        val records = rows.drop(1).map { PortableFiles.CSV_HEADERS.zip(it).toMap() }
        val performance = records.filter { it["record_type"] == "performance" }
        assertEquals(3, performance.size)
        assertEquals(setOf("best_reps_at_reference_kg", "heaviest_kg_at_10_reps", "volume_load"), performance.map { it.getValue("metric") }.toSet())
        assertEquals("10", performance.single { it["metric"] == "best_reps_at_reference_kg" }["value"])
        assertEquals(kg.toPlainString(), performance.single { it["metric"] == "heaviest_kg_at_10_reps" }["value"])
        assertEquals((kg * BigDecimal.TEN).toPlainString(), performance.single { it["metric"] == "volume_load" }["value"])
        assertTrue(performance.all { it["completed_working_sets"] == "1" && it["comparison"] == "unadjusted" && it["unfinished"] == "true" })
        assertTrue(performance.all { it["exercise_name"] == live.exercises.single().name && it["setup"] == setup })
        val attendance = records.single { it["record_type"] == "attendance" }
        assertEquals("2026-03-07", attendance["planned_date"])
        assertEquals("2026-03-09", attendance["actual_date"])
        assertEquals("2", attendance["delay_days"])
        assertEquals("", attendance["value"])
        val summary = records.single { it["record_type"] == "attendance_summary_28d" }
        val expected = Metrics.attendance(live, today)
        assertEquals(expected.planned.toString(), summary["planned_sessions"])
        assertEquals(expected.logged.toString(), summary["logged_sessions"])
        assertEquals(expected.percentage.toString(), summary["attendance_percent"])
    }

    @Test fun csvIncludesEveryRecordedReferenceAndContextRegardlessOfPreference() {
        val original = fixture().let { dataset -> dataset.copy(occurrences = dataset.occurrences.map { occurrence ->
            occurrence.copy(exercises = occurrence.exercises.map { entry -> entry.copy(sets = entry.sets.map { set ->
                if (set.id == "empty") set.copy(done = true, reps = 9, kg = BigDecimal("70.5"), observedAt = visit) else set
            }) })
        }) }
        val occurrence = original.occurrences.single()
        val entry = occurrence.exercises.single()
        val additional = occurrence.copy(id = "second-workout", plannedDate = "2026-03-08", exercises = listOf(entry.copy(
            setup = "Seat 8", sets = entry.sets.map { it.copy(id = "other-${it.id}", kg = if (it.kind == SetKind.WORKING && it.done) BigDecimal("65.5") else it.kg) })))
        val dataset = original.copy(occurrences = listOf(occurrence, additional))
        val records = parseCsv(PortableFiles.csv(dataset, today)).drop(1).map { PortableFiles.CSV_HEADERS.zip(it).toMap() }
        assertEquals(setOf(setup, "Seat 8"), records.filter { it["record_type"] == "performance" }.map { it.getValue("setup") }.toSet())
        assertEquals(setOf("70.5", kg.toPlainString()), records.filter { it["metric"] == "best_reps_at_reference_kg" && it["setup"] == setup }
            .map { it.getValue("reference_kg") }.toSet())
        val selected = dataset.copy(settings = dataset.settings.copy(referenceWeights = listOf(ReferenceWeight(ComparisonContext("exercise", "Seat 8"), BigDecimal("65.5")))))
        assertEquals(PortableFiles.csv(dataset, today), PortableFiles.csv(selected, today))
    }

    @Test fun attendanceRecordsPreserveOlderHistoryOutsideThe28DaySummary() {
        val dataset = fixture()
        val oldVisit = ObservationTime("2026-02-03T12:00:00Z", "2026-02-03", "Europe/Rome")
        val older = dataset.occurrences.single().copy(id = "older-workout", plannedDate = "2026-02-02", firstVisit = oldVisit,
            exercises = dataset.occurrences.single().exercises.map { entry -> entry.copy(sets = entry.sets.map { set ->
                set.copy(id = "old-${set.id}", observedAt = if (set.observedAt == null) null else oldVisit)
            }) })
        val records = parseCsv(PortableFiles.csv(dataset.copy(occurrences = listOf(older) + dataset.occurrences), today))
            .drop(1).map { PortableFiles.CSV_HEADERS.zip(it).toMap() }
        val attendance = records.filter { it["record_type"] == "attendance" }
        assertEquals(2, attendance.size)
        assertEquals("2026-02-03", attendance.single { it["occurrence_id"] == "older-workout" }["actual_date"])
        assertEquals("1", records.single { it["record_type"] == "attendance_summary_28d" }["planned_sessions"])
    }

    @Test fun missingPerformanceAndEmptyCohortRemainBlankWithReasons() {
        val dataset = fixture()
        val missing = dataset.copy(occurrences = dataset.occurrences.map { occurrence ->
            occurrence.copy(firstVisit = null, exercises = occurrence.exercises.map { entry ->
                entry.copy(sets = entry.sets.map { it.copy(done = false, reps = null, kg = null, observedAt = null) })
            })
        })
        val records = parseCsv(PortableFiles.csv(missing, today)).drop(1).map { PortableFiles.CSV_HEADERS.zip(it).toMap() }
        assertTrue(records.filter { it["record_type"] == "performance" }.all { it["value"] == "" && !it["missing_reason"].isNullOrEmpty() })
        val empty = Dataset(dataset.id, today)
        val summary = parseCsv(PortableFiles.csv(empty, today)).last().let { PortableFiles.CSV_HEADERS.zip(it).toMap() }
        assertEquals("", summary["attendance_percent"])
        assertEquals("", summary["average_delay_days"])
        assertTrue(summary.getValue("missing_reason").contains("no_planned_sessions"))
    }

    @Test fun verificationRejectsWrongEnvelopeVersionLiveAnchorsAndFolderGrants() {
        val json = PortableFiles.json(fixture(), clock, "test")
        fun replace(field: String, value: JsonElement): String = JsonObject(DatasetJson.parseToJsonElement(json).jsonObject + (field to value)).toString()
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.readJson(replace("formatVersion", JsonPrimitive(2))) }
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.readJson(replace("revision", JsonPrimitive(13))) }
        val liveWithoutGrant = fixture().let { it.copy(settings = it.settings.copy(backupTreeUri = null)) }
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.readJson(replace("dataset", DatasetJson.encodeToJsonElement(Dataset.serializer(), liveWithoutGrant))) }
        val pausedWithGrant = PortableFiles.snapshot(fixture(), clock).copy(settings = fixture().settings)
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.readJson(replace("dataset", DatasetJson.encodeToJsonElement(Dataset.serializer(), pausedWithGrant))) }
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.generationName(fixture().copy(id = "../other")) }
    }

    @Test fun textVerificationBoundsNestingBytesAndRejectsMalformedUtf8() {
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.readJson("[".repeat(65) + "]".repeat(65)) }
        val oversized = object : InputStream() {
            var remaining = PortableFiles.MAX_JSON_BYTES + 1
            override fun read(): Int = if (remaining-- > 0) 32 else -1
            override fun read(buffer: ByteArray, off: Int, len: Int): Int {
                if (remaining == 0) return -1
                val count = minOf(len, remaining)
                buffer.fill(32, off, off + count)
                remaining -= count
                return count
            }
        }
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.readText(oversized) }
        assertThrows(java.nio.charset.CharacterCodingException::class.java) { PortableFiles.readText(ByteArrayInputStream(byteArrayOf(0xc3.toByte(), 0x28))) }
    }

    @Test fun mediaCopiesAndReopensWithExactSizeHashAndRelativePath() {
        val media = fixture().media.single()
        val output = ByteArrayOutputStream()
        PortableFiles.copyMedia(ByteArrayInputStream(clip), output, media)
        assertArrayEquals(clip, output.toByteArray())
        PortableFiles.verifyMedia(ByteArrayInputStream(output.toByteArray()), media)
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.verifyMedia(ByteArrayInputStream(clip + 0), media) }
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.verifyMedia(ByteArrayInputStream(clip.copyOf(clip.size - 1)), media) }
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.verifyMedia(ByteArrayInputStream(clip), media.copy(sha256 = "0".repeat(64), relativePath = "media/${"0".repeat(64)}")) }
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.verifyMedia(ByteArrayInputStream(clip), media.copy(relativePath = "../video")) }
        assertThrows(IllegalArgumentException::class.java) { PortableFiles.verifyMedia(ByteArrayInputStream(clip), media.copy(size = PortableFiles.MAX_MEDIA_BYTES + 1)) }
    }

    @Test fun mediaCopyUsesBoundedChunksWithoutAllocatingAWholeLargeVideo() {
        val size = PortableFiles.MAX_MEDIA_BYTES
        val digest = MessageDigest.getInstance("SHA-256")
        val block = ByteArray(64 * 1024) { 42 }
        var remaining = size
        while (remaining > 0) {
            val count = minOf(remaining, block.size.toLong()).toInt()
            digest.update(block, 0, count)
            remaining -= count
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        val media = fixture().media.single().copy(size = size, sha256 = hash, relativePath = "media/$hash")
        val input = object : InputStream() {
            var left = size
            override fun read(): Int = error("Bulk reads required")
            override fun read(buffer: ByteArray, off: Int, len: Int): Int {
                assertTrue(len <= 64 * 1024)
                if (left == 0L) return -1
                val count = minOf(left, len.toLong()).toInt()
                buffer.fill(42, off, off + count)
                left -= count
                return count
            }
        }
        var written = 0L
        val output = object : OutputStream() {
            override fun write(value: Int) = error("Bulk writes required")
            override fun write(buffer: ByteArray, off: Int, len: Int) { assertTrue(len <= 64 * 1024); written += len }
        }
        PortableFiles.copyMedia(input, output, media)
        assertEquals(size, written)
    }

    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val character = text[index++]
            if (character == '"') {
                if (quoted && index < text.length && text[index] == '"') { field.append('"'); index++ }
                else quoted = !quoted
            } else if (!quoted && character == ',') {
                fields.add(field.toString()); field.setLength(0)
            } else if (!quoted && character == '\r') {
                assertEquals('\n', text[index++])
                fields.add(field.toString()); field.setLength(0)
                rows.add(fields); fields = mutableListOf()
            } else field.append(character)
        }
        assertFalse(quoted)
        assertEquals(0, field.length)
        return rows
    }
}
