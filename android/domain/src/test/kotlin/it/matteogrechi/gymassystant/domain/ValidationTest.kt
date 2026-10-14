package it.matteogrechi.gymassystant.domain

import org.junit.Assert.*
import org.junit.Test

// GYM-016/018/020/023/024/026/028: persistence boundaries and interval/date ownership.
class ValidationTest {
    private fun clock(day: String = "2026-10-12") = ClockReading(ObservationTime("${day}T12:00:00Z", day, "UTC"), 1000, "boot")
    private fun rejected(action: () -> Unit) {
        try { action(); fail("Expected rejection") } catch (_: IllegalArgumentException) { }
    }

    @Test fun pendingContainsOnly28PastCompleteDaysAndRetainsAllPastHistoryForExports() {
        val dataset = initialDataset(clock("2026-09-01"))
        val today = "2026-10-13"
        val pending = Metrics.pending(dataset, today)
        assertTrue(pending.isNotEmpty())
        assertTrue(pending.all { it.plannedDate >= "2026-09-15" && it.plannedDate < today && it.firstVisit == null })
        assertEquals(Metrics.attendance(dataset, today).planned, pending.size)
        assertTrue(Metrics.attendanceRecords(dataset, today).any { it.plannedDate < "2026-09-15" })
        assertTrue(Metrics.pending(initialDataset(clock(today)), today).isEmpty())
    }

    @Test fun explicitRestOverridesWeeklyAndRemovingTemplatesClearsAssignmentsButNotExecution() {
        var dataset = initialDataset(clock())
        val first = dataset.plans.first()
        val rest = first.copy(dateOverrides = mapOf("2026-10-14" to null, "2026-10-15" to first.sessions.last().id))
        dataset = Training.retainDraft(dataset, rest)
        dataset = Training.commitDraft(dataset, first.id, "rest-revision", "2026-10-12")
        assertNull(Training.openOccurrence(dataset, "2026-10-14"))
        assertEquals(first.sessions.last().id, Training.openOccurrence(dataset, "2026-10-15")!!.sessionId)
        dataset = Training.transition(dataset, "2026-10-15", SessionAction.START, clock("2026-10-15"))
        val pinned = Training.openOccurrence(dataset, "2026-10-15")!!
        val removed = Training.removeSession(dataset.plans.single { it.id == first.id }, first.sessions.last().id)
        assertTrue(removed.weeklySchedule.values.none { it == first.sessions.last().id })
        assertFalse(removed.dateOverrides.containsKey("2026-10-15"))
        dataset = Training.retainDraft(dataset, removed)
        dataset = Training.commitDraft(dataset, first.id, "removed-revision", "2026-10-15")
        assertEquals(pinned, Training.openOccurrence(dataset, "2026-10-15"))
        assertNull(Training.openOccurrence(dataset, "2026-10-16"))
    }

    @Test fun oldSnapshotIdentitiesCannotBeReusedByAnotherPlanAfterSameDayRevisionReplacement() {
        var dataset = initialDataset(clock())
        val first = dataset.plans.first()
        dataset = Training.transition(dataset, "2026-10-12", SessionAction.START, clock())
        val oldSession = first.sessions.first()
        val updated = Training.removeSession(first, oldSession.id)
        dataset = Training.retainDraft(dataset, updated)
        dataset = Training.commitDraft(dataset, first.id, "replacement", "2026-10-12")
        val second = dataset.plans.first { it.id != first.id }
        rejected { Training.retainDraft(dataset, second.copy(sessions = listOf(oldSession))) }
        val reusedEntry = second.sessions.first().copy(exercises = listOf(oldSession.exercises.first()))
        rejected { Training.retainDraft(dataset, second.copy(sessions = listOf(reusedEntry))) }
    }

    @Test fun mediaReplacementKeepsOldSnapshotAssociationAndRejectsTraversalOrBrokenReference() {
        var dataset = initialDataset(clock())
        val exercise = dataset.exercises.first()
        val media = MediaMetadata("media-1", exercise.id, "Prèss.mp4", "video/mp4", 100, "a".repeat(64), "media/a.mp4")
        dataset = Training.attachMedia(dataset, media)
        dataset = Training.transition(dataset, "2026-10-12", SessionAction.START, clock())
        dataset = Training.attachMedia(dataset, media.copy(id = "media-2", relativePath = "media/b.mp4"))
        assertEquals("media-1", Training.openOccurrence(dataset, "2026-10-12")!!.exercises.first().definition.mediaId)
        assertEquals("media-2", dataset.exercises.first().mediaId)
        assertEquals(2, dataset.media.size)
        assertEquals(dataset, decodeDataset(encodeDataset(dataset)))
        rejected { Training.attachMedia(dataset, media.copy(id = "bad", relativePath = "../escape.mp4")) }
        rejected { Training.attachMedia(dataset, media.copy(id = "bad", relativePath = "/absolute.mp4")) }
        rejected { Training.attachMedia(dataset, media.copy(id = "bad", sha256 = "no hash")) }
        rejected { Training.attachMedia(dataset, media.copy(id = "bad", exerciseId = "absent")) }
    }

    @Test fun persistedInvalidVersionsDatesReferencesAndPrescriptionBoundsAreRejected() {
        val dataset = initialDataset(clock())
        rejected { validateDataset(dataset.copy(formatVersion = 99)) }
        rejected { validateDataset(dataset.copy(dataKind = "demo")) }
        rejected { validateDataset(dataset.copy(createdDate = "2026-02-30")) }
        rejected { validateDataset(dataset.copy(activePlanId = "unknown")) }
        rejected { validateDataset(dataset.copy(exercises = dataset.exercises + dataset.exercises.first())) }
        val plan = dataset.plans.first()
        listOf(0, 3601).forEach { seconds ->
            rejected { validatePlan(plan.copy(sessions = plan.sessions.map { session ->
                session.copy(exercises = session.exercises.map { it.copy(recoverySeconds = seconds) })
            }), dataset.exercises) }
        }
        rejected { validatePlan(plan.copy(weeklySchedule = mapOf(8 to plan.sessions.first().id)), dataset.exercises) }
        rejected { validatePlan(plan.copy(dateOverrides = mapOf("2026-10-12" to "unknown")), dataset.exercises) }
        rejected { decodeDataset(encodeDataset(dataset).replace("\"formatVersion\":3", "\"formatVersion\":2")) }
    }
}
