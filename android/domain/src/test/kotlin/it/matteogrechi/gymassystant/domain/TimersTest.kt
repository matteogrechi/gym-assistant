package it.matteogrechi.gymassystant.domain

import org.junit.Assert.*
import org.junit.Test

// GYM-012/017/024/027: recreation, sleep, wall clock changes, reboot and portable export.
class TimersTest {
    private val day = "2026-10-12"
    private fun clock(ms: Long, boot: String? = "one", instant: String = "${day}T12:00:00Z") =
        ClockReading(ObservationTime(instant, day, "UTC"), ms, boot)
    private fun entry(dataset: Dataset) = Training.openOccurrence(dataset, day)!!.exercises.first()
    private fun rejected(action: () -> Unit) {
        try { action(); fail("Expected rejection") } catch (_: IllegalArgumentException) { }
    }

    @Test fun independentTimersSurviveSerializationAndWallClockChangesUsingMonotonicSleepTime() {
        var dataset = initialDataset(clock(1000))
        val id = entry(dataset).prescription.id
        dataset = Training.changeTimer(dataset, day, id, TimerMode.EXECUTION, TimerAction.START, clock(1000))
        dataset = Training.changeTimer(dataset, day, id, TimerMode.RECOVERY, TimerAction.START, clock(4000))
        val reconstructed = decodeDataset(encodeDataset(dataset))
        assertEquals(9000L, Timers.readingMs(entry(reconstructed).executionTimer, TimerMode.EXECUTION, 90,
            clock(10000, instant = "${day}T11:00:00Z")))
        assertEquals(84000L, Timers.readingMs(entry(reconstructed).recoveryTimer, TimerMode.RECOVERY, 90, clock(10000)))
        dataset = Training.changeTimer(reconstructed, day, id, TimerMode.EXECUTION, TimerAction.PAUSE, clock(11000))
        assertFalse(entry(dataset).executionTimer.running)
        assertTrue(entry(dataset).recoveryTimer.running)
        assertEquals(10000L, entry(dataset).executionTimer.elapsedMs)
        dataset = Training.changeTimer(dataset, day, id, TimerMode.EXECUTION, TimerAction.RESET, clock(12000))
        assertEquals(0L, entry(dataset).executionTimer.elapsedMs)
        assertTrue(entry(dataset).recoveryTimer.running)
        assertEquals(0L, Timers.readingMs(entry(dataset).recoveryTimer, TimerMode.RECOVERY, 90, clock(150000)))
        dataset = Training.checkpoint(dataset, clock(150000))
        assertFalse(entry(dataset).recoveryTimer.running)
        dataset = Training.changeTimer(dataset, day, id, TimerMode.RECOVERY, TimerAction.START, clock(151000))
        assertEquals(90000L, Timers.readingMs(entry(dataset).recoveryTimer, TimerMode.RECOVERY, 90, clock(151000)))
        dataset = Training.changeTimer(dataset, day, id, TimerMode.RECOVERY, TimerAction.RESET, clock(152000))
        assertFalse(entry(dataset).recoveryTimer.running)
        assertEquals(90000L, Timers.readingMs(entry(dataset).recoveryTimer, TimerMode.RECOVERY, 90, clock(153000)))
    }

    @Test fun checkpointThenRebootUnknownBootOrRegressedMonotonicClockPausesAtDurableReading() {
        var dataset = initialDataset(clock(1000))
        val id = entry(dataset).prescription.id
        dataset = Training.changeTimer(dataset, day, id, TimerMode.EXECUTION, TimerAction.START, clock(1000))
        dataset = Training.checkpoint(dataset, clock(9000))
        val durable = decodeDataset(encodeDataset(dataset))
        listOf(clock(2000, "two"), clock(10000, null), clock(8000, "one")).forEach { reboot ->
            val paused = Training.checkpoint(durable, reboot)
            val timer = entry(paused).executionTimer
            assertFalse(timer.running); assertTrue(timer.interrupted); assertEquals(8000L, timer.elapsedMs)
            assertNull(timer.bootId)
            assertEquals(Lifecycle.READY, Training.openOccurrence(paused, day)!!.lifecycle)
        }
        rejected { Training.changeTimer(durable, day, id, TimerMode.EXECUTION, TimerAction.START, clock(10000, null)) }
        val resumed = Training.changeTimer(Training.checkpoint(durable, clock(10, "two")), day, id,
            TimerMode.EXECUTION, TimerAction.START, clock(20, "two"))
        assertFalse(entry(resumed).executionTimer.interrupted)
        assertEquals(8100L, Timers.readingMs(entry(resumed).executionTimer, TimerMode.EXECUTION, 90, clock(120, "two")))
    }

    @Test fun sessionPauseAndFinishSettleEveryTimerAtOneReadingAndResumeStartsNone() {
        var dataset = initialDataset(clock(1000))
        dataset = Training.transition(dataset, day, SessionAction.START, clock(1000))
        val exercises = Training.openOccurrence(dataset, day)!!.exercises
        exercises.forEach { exercise -> TimerMode.entries.forEach { mode ->
            dataset = Training.changeTimer(dataset, day, exercise.prescription.id, mode, TimerAction.START, clock(1000))
        } }
        val beforeConfirmation = dataset
        rejected { Training.transition(beforeConfirmation, day, SessionAction.FINISH, clock(8000)) }
        assertTrue(entry(dataset).executionTimer.running)
        dataset = Training.transition(dataset, day, SessionAction.PAUSE, clock(8000))
        Training.openOccurrence(dataset, day)!!.exercises.forEach { exercise ->
            assertEquals(7000L, exercise.executionTimer.elapsedMs); assertEquals(7000L, exercise.recoveryTimer.elapsedMs)
            assertFalse(exercise.executionTimer.running); assertFalse(exercise.recoveryTimer.running)
            rejected { Training.changeTimer(dataset, day, exercise.prescription.id, TimerMode.EXECUTION, TimerAction.START, clock(9000)) }
        }
        dataset = Training.transition(dataset, day, SessionAction.RESUME, clock(12000))
        assertFalse(entry(dataset).executionTimer.running)
        dataset = Training.changeTimer(dataset, day, entry(dataset).prescription.id, TimerMode.EXECUTION, TimerAction.START, clock(13000))
        dataset = Training.transition(dataset, day, SessionAction.FINISH, clock(15000), true)
        assertEquals(9000L, entry(dataset).executionTimer.elapsedMs)
        assertEquals(7000L, entry(dataset).recoveryTimer.elapsedMs)
        assertNull(Training.openOccurrence(dataset, day)!!.firstVisit)
        assertTrue(Training.openOccurrence(dataset, day)!!.exercises.flatMap { it.sets }.none { it.done })
    }

    @Test fun exportSettlesAPausedCopyWithoutChangingLiveStateAndStripsOnlyPrivateFolderGrant() {
        var dataset = initialDataset(clock(1000))
        val id = entry(dataset).prescription.id
        dataset = Training.updateSettings(dataset, dataset.settings.copy(backupTreeUri = "content://local/tree/id", backupLabel = "My backups"))
        dataset = Training.changeTimer(dataset, day, id, TimerMode.EXECUTION, TimerAction.START, clock(1000))
        dataset = Training.changeTimer(dataset, day, id, TimerMode.RECOVERY, TimerAction.START, clock(2000))
        val liveBefore = encodeDataset(dataset)
        val portable = Training.portableSnapshot(dataset, clock(11000))
        assertEquals(liveBefore, encodeDataset(dataset))
        assertTrue(entry(dataset).executionTimer.running); assertTrue(entry(dataset).recoveryTimer.running)
        assertEquals(10000L, entry(portable).executionTimer.elapsedMs)
        assertEquals(9000L, entry(portable).recoveryTimer.elapsedMs)
        assertFalse(entry(portable).executionTimer.running); assertFalse(entry(portable).recoveryTimer.running)
        assertTrue(entry(portable).executionTimer.capturedRunning)
        assertNull(entry(portable).executionTimer.bootId)
        assertNull(portable.settings.backupTreeUri); assertEquals("My backups", portable.settings.backupLabel)
        assertEquals(dataset.revision, portable.revision)
        assertEquals(portable, decodeDataset(encodeDataset(portable)))
    }

    @Test fun recoveryTargetsStayPinnedAndReorderingRetainsRunningClocksAndSetIdentity() {
        var dataset = initialDataset(clock(1000))
        val originalEntry = entry(dataset)
        dataset = Training.changeTimer(dataset, day, originalEntry.prescription.id, TimerMode.RECOVERY, TimerAction.START, clock(1000))
        val ids = Training.openOccurrence(dataset, day)!!.exercises.map { it.prescription.id }.reversed()
        dataset = Training.reorderExercises(dataset, day, ids, clock(3000))
        val plan = dataset.plans.first()
        val edited = plan.copy(sessions = plan.sessions.map { session -> session.copy(exercises = session.exercises.map { it.copy(recoverySeconds = 45) }) })
        dataset = Training.retainDraft(dataset, edited)
        dataset = Training.commitDraft(dataset, plan.id, "timer-new-revision", day)
        dataset = Training.activatePlan(dataset, dataset.plans.first { it.id != plan.id }.id, day)
        val pinned = Training.openOccurrence(dataset, day)!!.exercises.last()
        assertEquals(originalEntry.prescription.id, pinned.prescription.id)
        assertEquals(originalEntry.sets, pinned.sets)
        assertEquals(90, pinned.prescription.recoverySeconds)
        assertEquals(80000L, Timers.readingMs(pinned.recoveryTimer, TimerMode.RECOVERY, pinned.prescription.recoverySeconds, clock(11000)))
        dataset = Training.changeTimer(dataset, day, pinned.prescription.id, TimerMode.RECOVERY, TimerAction.RESET, clock(12000))
        val reset = Training.openOccurrence(dataset, day)!!.exercises.last()
        assertEquals(90000L, Timers.readingMs(reset.recoveryTimer, TimerMode.RECOVERY, 90, clock(20000)))
    }
}
