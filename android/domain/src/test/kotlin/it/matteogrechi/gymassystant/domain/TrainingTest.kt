package it.matteogrechi.gymassystant.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

// GYM-004/010/012/014/016/017/018/019/020/026/028: genuine history and protected execution invariants.
class TrainingTest {
    private val monday = "2026-10-12"
    private fun clock(day: String = monday, ms: Long = 1000, boot: String? = "boot-1") =
        ClockReading(ObservationTime("${day}T12:00:00Z", day, "UTC"), ms, boot)
    private fun seed() = initialDataset(clock())
    private fun occurrence(dataset: Dataset, day: String = monday) = Training.openOccurrence(dataset, day)!!
    private fun entry(dataset: Dataset, day: String = monday) = occurrence(dataset, day).exercises.first()
    private fun context(dataset: Dataset) = entry(dataset).let { ComparisonContext(it.definition.id, it.setup) }
    private fun log(dataset: Dataset, reps: Int? = 10, kg: String? = "20.1", done: Boolean = true,
        day: String = monday, actual: String = day, setIndex: Int = 0): Dataset {
        val entry = entry(dataset, day)
        return Training.updateSet(dataset, day, entry.prescription.id, entry.sets[setIndex].id,
            reps, kg?.let(::BigDecimal), done, clock(actual))
    }
    private fun rejected(action: () -> Unit) {
        try { action(); fail("Expected rejection") } catch (_: IllegalArgumentException) { }
    }

    @Test fun starterMatchesMockupButContainsNoHistoryOrPreInstallObligations() {
        val dataset = seed()
        assertEquals(listOf("Full body foundations", "Two-day routine"), dataset.plans.map { it.name })
        assertEquals(listOf("Leg press", "Chest press", "Lat pulldown"), dataset.exercises.map { it.name })
        assertEquals(listOf("Session A", "Session B", "Session C"), dataset.plans.first().sessions.map { it.name })
        assertEquals(listOf("Chest press", "Lat pulldown", "Leg press"),
            Training.openOccurrence(dataset, "2026-10-14")!!.exercises.map { it.definition.name })
        assertTrue(dataset.occurrences.isEmpty())
        assertTrue(occurrence(dataset).exercises.flatMap { it.sets }.all { it.reps == null && it.kg == null && !it.done })
        assertNull(Training.openOccurrence(dataset, "2026-10-05"))
        assertEquals(0, Metrics.attendance(dataset, monday).planned)
        assertNull(Metrics.attendance(dataset, monday).percentage)
        assertEquals(dataset, decodeDataset(encodeDataset(dataset)))
    }

    @Test fun removingTemplateWithPastOverrideRetainsOriginalObligation() {
        var dataset = seed()
        val plan = dataset.plans.first()
        val removed = plan.sessions.first()
        dataset = Training.beginEdit(dataset, plan.id)
        dataset = Training.retainDraft(dataset, plan.copy(dateOverrides = mapOf(monday to removed.id)))
        dataset = Training.commitDraft(dataset, plan.id, "with-override", monday)
        val before = occurrence(dataset)
        dataset = Training.beginEdit(dataset, plan.id)
        dataset = Training.retainDraft(dataset, Training.removeSession(dataset.drafts.single().plan, removed.id))
        dataset = Training.commitDraft(dataset, plan.id, "without-template", "2026-10-13")
        assertEquals(before, occurrence(dataset))
        assertEquals(removed.id, Metrics.pending(dataset, "2026-10-13").single().sessionId)
        assertFalse(dataset.plans.single { it.id == plan.id }.sessions.any { it.id == removed.id })
    }

    @Test fun correctingCompletedSetKeepsObservationDateUntilUndoneAndRecompleted() {
        var dataset = log(seed(), 10, "20")
        val observed = entry(dataset).sets.first().observedAt
        dataset = log(dataset, 10, "20.0", actual = "2026-10-13")
        assertEquals(observed, entry(dataset).sets.first().observedAt)
        assertEquals(monday, Metrics.performance(dataset, context(dataset), BigDecimal("20"), monday, "2026-10-13").first().actualDate)
        dataset = log(dataset, 10, "20", done = false, actual = "2026-10-13")
        assertNull(entry(dataset).sets.first().observedAt)
        dataset = log(dataset, 10, "20", actual = "2026-10-14")
        assertEquals("2026-10-14", entry(dataset).sets.first().observedAt?.localDate)
        assertEquals(monday, occurrence(dataset).firstVisit?.localDate)
    }

    @Test fun decimalsRemainExactJsonNumbersAndMetricsUseChosenRecordedWeight() {
        var dataset = log(seed(), 10, "0.123456789123456789")
        dataset = log(dataset, 12, "0.123456789123456789", setIndex = 1)
        val text = encodeDataset(dataset)
        assertTrue(text.contains("\"kg\":0.123456789123456789"))
        assertFalse(text.contains("\"kg\":\"0.123456789123456789\""))
        assertEquals(dataset, decodeDataset(text))
        val result = Metrics.performance(dataset, context(dataset), BigDecimal("0.123456789123456789"), monday, monday).single()
        assertEquals(12, result.bestReps)
        assertEquals(BigDecimal("0.123456789123456789"), result.loadAt10)
        assertEquals(BigDecimal("2.716049360716049358"), result.volumeLoad)
        assertEquals(2, result.completedWorkingSets)
        assertTrue(result.unfinished)
        rejected { Metrics.performance(dataset, context(dataset), BigDecimal("99"), monday, monday) }
        assertNull(Metrics.performance(dataset, context(dataset), null, monday, monday).single().bestReps)
        rejected { decodeDataset(text.replace("\"kg\":0.123456789123456789", "\"kg\":\"0.123456789123456789\"")) }
    }

    @Test fun equivalentDecimalScalesAndZeroLoadsAreComparable() {
        var dataset = log(seed(), 10, "20.10")
        dataset = log(dataset, 11, "20.1", setIndex = 1)
        val weights = Metrics.referenceWeights(dataset, context(dataset))
        assertEquals(1, weights.size)
        assertEquals(11, Metrics.performance(dataset, context(dataset), BigDecimal("20.1000"), monday, monday).single().bestReps)
        dataset = log(dataset, 10, "0", setIndex = 2)
        assertEquals(2, Metrics.referenceWeights(dataset, context(dataset)).size)
        assertEquals(10, Metrics.performance(dataset, context(dataset), BigDecimal.ZERO, monday, monday).single().bestReps)
    }

    @Test fun warmupsInvalidPlaceholdersAndUndoDoNotInflatePerformanceOrEraseFirstVisit() {
        var dataset = seed()
        val id = entry(dataset).prescription.id
        dataset = Training.addWarmUp(dataset, monday, id, "warm-up", clock())
        dataset = Training.updateSet(dataset, monday, id, "warm-up", 30, BigDecimal("100"), true, clock())
        assertNull(occurrence(dataset).firstVisit)
        assertTrue(Metrics.referenceWeights(dataset, context(dataset)).isEmpty())
        dataset = log(dataset, 0)
        assertNull(occurrence(dataset).firstVisit)
        dataset = log(dataset, 10, null)
        assertNull(occurrence(dataset).firstVisit)
        dataset = log(dataset, 10, "20")
        val firstVisit = occurrence(dataset).firstVisit
        assertNotNull(firstVisit)
        dataset = log(dataset, 10, "20", false, actual = "2026-10-13")
        assertEquals(firstVisit, occurrence(dataset).firstVisit)
        val result = Metrics.performance(dataset, context(dataset), null, monday, monday).single()
        assertNull(result.volumeLoad); assertNull(result.loadAt10); assertEquals(0, result.completedWorkingSets)
        assertEquals(1, Metrics.attendance(dataset, "2026-10-13").logged)
        dataset = Training.removeWarmUp(dataset, monday, id, "warm-up", clock())
        assertEquals(3, entry(dataset).sets.size)
        rejected { Training.removeWarmUp(dataset, monday, id, entry(dataset).sets.first().id, clock()) }
        rejected { log(dataset, -1) }
        rejected { log(dataset, kg = "-0.01") }
    }

    @Test fun allExecutionMutationsRejectFutureAndFinishedWithExplicitFutureReorderException() {
        var dataset = seed()
        val future = "2026-10-14"
        val futureEntry = entry(dataset, future)
        rejected { Training.updateSet(dataset, future, futureEntry.prescription.id, futureEntry.sets.first().id, 10, BigDecimal.ONE, true, clock()) }
        rejected { Training.setSetup(dataset, future, futureEntry.prescription.id, "Seat 7", clock()) }
        rejected { Training.addWarmUp(dataset, future, futureEntry.prescription.id, "future-warm", clock()) }
        rejected { Training.removeWarmUp(dataset, future, futureEntry.prescription.id, "missing", clock()) }
        SessionAction.entries.forEach { action -> rejected { Training.transition(dataset, future, action, clock(), true) } }
        TimerAction.entries.forEach { action -> TimerMode.entries.forEach { mode ->
            rejected { Training.changeTimer(dataset, future, futureEntry.prescription.id, mode, action, clock()) }
        } }
        dataset = Training.reorderExercises(dataset, future, occurrence(dataset, future).exercises.map { it.prescription.id }.reversed(), clock())
        assertTrue(occurrence(dataset, future).protected)
        assertNull(occurrence(dataset, future).firstVisit)
        dataset = Training.transition(dataset, monday, SessionAction.START, clock())
        dataset = Training.transition(dataset, monday, SessionAction.FINISH, clock(), true)
        val finished = dataset
        val finishedEntry = entry(dataset)
        rejected { log(finished) }
        rejected { Training.setSetup(finished, monday, finishedEntry.prescription.id, "Seat 7", clock()) }
        rejected { Training.addWarmUp(finished, monday, finishedEntry.prescription.id, "warm", clock()) }
        rejected { Training.removeWarmUp(finished, monday, finishedEntry.prescription.id, "missing", clock()) }
        rejected { Training.reorderExercises(finished, monday, occurrence(finished).exercises.map { it.prescription.id }, clock()) }
        SessionAction.entries.forEach { action -> rejected { Training.transition(finished, monday, action, clock(), true) } }
        TimerAction.entries.forEach { action -> TimerMode.entries.forEach { mode ->
            rejected { Training.changeTimer(finished, monday, finishedEntry.prescription.id, mode, action, clock()) }
        } }
        assertEquals(occurrence(finished), occurrence(Training.checkpoint(finished, clock(ms = 90000))))
        assertNull(occurrence(finished).firstVisit)
    }

    @Test fun loggingInReadyAndPausedIsAllowedAndFinishRequiresConfirmationWithoutFabricatingSets() {
        var dataset = log(seed())
        assertEquals(Lifecycle.READY, occurrence(dataset).lifecycle)
        rejected { Training.transition(dataset, monday, SessionAction.RESUME, clock()) }
        rejected { Training.transition(dataset, monday, SessionAction.FINISH, clock(), true) }
        dataset = Training.transition(dataset, monday, SessionAction.START, clock())
        dataset = Training.transition(dataset, monday, SessionAction.PAUSE, clock())
        dataset = log(dataset, setIndex = 1)
        assertEquals(Lifecycle.PAUSED, occurrence(dataset).lifecycle)
        rejected { Training.transition(dataset, monday, SessionAction.FINISH, clock()) }
        val before = occurrence(dataset)
        dataset = Training.transition(dataset, monday, SessionAction.FINISH, clock(), true)
        assertEquals(before.exercises, occurrence(dataset).exercises)
        assertEquals(Lifecycle.FINISHED, occurrence(dataset).lifecycle)
        assertEquals(2, entry(dataset).sets.count { it.done })
    }

    @Test fun fullCompletionFinishesDirectlyAndStartSelectsChosenOrder() {
        var dataset = seed()
        dataset = Training.reorderExercises(dataset, monday, occurrence(dataset).exercises.map { it.prescription.id }.reversed(), clock())
        val firstId = entry(dataset).prescription.id
        assertEquals(firstId, Training.firstUnfinishedEntryId(occurrence(dataset)))
        for (entry in occurrence(dataset).exercises) for (set in entry.sets) {
            dataset = Training.updateSet(dataset, monday, entry.prescription.id, set.id, 10, BigDecimal.ONE, true, clock())
        }
        assertFalse(Training.needsFinishConfirmation(occurrence(dataset)))
        assertEquals(firstId, Training.firstUnfinishedEntryId(occurrence(dataset)))
        dataset = Training.transition(dataset, monday, SessionAction.START, clock())
        dataset = Training.transition(dataset, monday, SessionAction.FINISH, clock())
        assertEquals(Lifecycle.FINISHED, occurrence(dataset).lifecycle)
    }

    @Test fun startLogReorderWarmupsAndSetupPinSnapshotThroughSavingAndActivation() {
        val original = seed()
        val oldPlan = original.plans.first()
        val otherPlan = original.plans.last()
        val protections: List<(Dataset) -> Dataset> = listOf(
            { Training.transition(it, monday, SessionAction.START, clock()) },
            { log(it) },
            { Training.reorderExercises(it, monday, occurrence(it).exercises.map { entry -> entry.prescription.id }.reversed(), clock()) },
            { Training.addWarmUp(it, monday, entry(it).prescription.id, "warm-up", clock()) },
            { Training.setSetup(it, monday, entry(it).prescription.id, "Seat 5", clock()) },
            { Training.changeTimer(it, monday, entry(it).prescription.id, TimerMode.RECOVERY, TimerAction.START, clock()) },
        )
        protections.forEachIndexed { index, protect ->
            var dataset = protect(original)
            val pinned = occurrence(dataset)
            val edited = oldPlan.copy(name = "Changed", sessions = oldPlan.sessions.map { session ->
                session.copy(exercises = session.exercises.reversed().map { it.copy(recoverySeconds = 45, workingSets = 2) })
            })
            dataset = Training.retainDraft(dataset, edited)
            dataset = Training.commitDraft(dataset, oldPlan.id, "revision-$index", monday)
            dataset = Training.activatePlan(dataset, otherPlan.id, monday)
            assertEquals(pinned, occurrence(dataset))
            assertEquals(oldPlan.id, occurrence(dataset).planId)
            assertEquals(90, entry(dataset).prescription.recoverySeconds)
        }
        var untouched = Training.retainDraft(original, oldPlan.copy(name = "Changed"))
        untouched = Training.commitDraft(untouched, oldPlan.id, "untouched-revision", monday)
        assertEquals("Changed", occurrence(untouched).planName)
        untouched = Training.activatePlan(untouched, otherPlan.id, monday)
        assertEquals(otherPlan.id, occurrence(untouched).planId)
    }

    @Test fun missedDatesKeepOriginalPrescriptionsThroughInactiveSaveAndLaterActivation() {
        var dataset = seed()
        val first = dataset.plans.first()
        val second = dataset.plans.last()
        dataset = Training.retainDraft(dataset, first.copy(sessions = first.sessions.take(1), weeklySchedule = mapOf(1 to first.sessions.first().id)))
        dataset = Training.commitDraft(dataset, first.id, "new-plan-revision", "2026-10-19")
        assertEquals(first.sessions[1].id, occurrence(dataset, "2026-10-14").sessionId)
        assertNull(Training.openOccurrence(dataset, "2026-10-21"))
        dataset = Training.activatePlan(dataset, second.id, "2026-10-26")
        assertEquals(first.id, occurrence(dataset, "2026-10-19").planId)
        dataset = log(dataset, day = "2026-10-14", actual = "2026-10-27")
        assertEquals(first.id, occurrence(dataset, "2026-10-14").planId)
        assertEquals("2026-10-14", occurrence(dataset, "2026-10-14").plannedDate)
        assertEquals("2026-10-27", occurrence(dataset, "2026-10-14").firstVisit!!.localDate)
        val summary = Metrics.attendance(dataset, "2026-10-27")
        assertEquals(1, summary.logged); assertEquals(1, summary.late); assertEquals(13.0, summary.averageDelayDays!!, 0.0)
        assertTrue(summary.observations.filter { it.plannedDate < "2026-10-26" }.all { it.planId == first.id })
        assertEquals(0, dataset.revision)
    }

    @Test fun draftsSavingActivationAndDiscardRemainIndependent() {
        var dataset = seed()
        val first = dataset.plans.first()
        val second = dataset.plans.last()
        dataset = Training.retainDraft(dataset, second.copy(name = "Edited"))
        assertEquals(second.name, dataset.plans.last().name)
        rejected { Training.activatePlan(dataset, second.id, monday) }
        dataset = Training.retainDraft(dataset, first.copy(name = "Retained draft"))
        dataset = Training.commitDraft(dataset, second.id, "second-revision", monday)
        assertEquals(first.id, dataset.activePlanId)
        assertEquals("Retained draft", dataset.drafts.single().plan.name)
        dataset = Training.activatePlan(dataset, second.id, monday)
        assertEquals("Retained draft", dataset.drafts.single().plan.name)
        val definition = ExerciseDefinition("custom", "Custom", "Machine", "Seat 8")
        dataset = Training.createExercise(dataset, definition)
        val newPlan = Plan("new-plan", "unsaved-revision", "New", listOf(SessionTemplate("new-session", "One",
            exercises = listOf(Prescription("new-entry", "custom")))))
        dataset = Training.retainDraft(dataset, newPlan)
        val before = dataset
        dataset = Training.discardNewDraft(dataset, newPlan.id)
        assertEquals(before.plans, dataset.plans); assertEquals(before.activePlanId, dataset.activePlanId)
        assertEquals(before.exercises, dataset.exercises); assertEquals(before.occurrences, dataset.occurrences)
        assertEquals(listOf(first.id), dataset.drafts.map { it.plan.id })
        rejected { Training.discardNewDraft(dataset, first.id) }
        rejected { Training.createExercise(dataset, definition) }
    }

    @Test fun invalidDraftRemainsRetainedButCannotBeSavedAndIdsCannotCollideAcrossPlans() {
        var dataset = seed()
        val plan = dataset.plans.first()
        val invalid = plan.copy(name = "", sessions = plan.sessions.map { session ->
            session.copy(exercises = session.exercises.map { it.copy(minReps = 40, maxReps = 10) })
        })
        dataset = Training.retainDraft(dataset, invalid)
        assertEquals(invalid, dataset.drafts.single().plan)
        rejected { Training.commitDraft(dataset, plan.id, "invalid-revision", monday) }
        val colliding = dataset.plans.last().copy(sessions = listOf(plan.sessions.first()))
        rejected { Training.retainDraft(dataset, colliding) }
        val second = dataset.plans.last()
        dataset = Training.retainDraft(dataset, second.copy(dateOverrides = mapOf("2026-10-11" to second.sessions.first().id)))
        rejected { Training.commitDraft(dataset, second.id, "past-revision", monday) }
    }

    @Test fun setupAndDistinctExerciseIdentitiesNeverMergeAndMissingMetricsStayNull() {
        var dataset = log(seed())
        val oldContext = context(dataset)
        dataset = Training.setSetup(dataset, monday, entry(dataset).prescription.id, "Seat 9", clock())
        assertTrue(Metrics.referenceWeights(dataset, oldContext).isEmpty())
        assertEquals(1, Metrics.referenceWeights(dataset, context(dataset)).size)
        dataset = Training.createExercise(dataset, ExerciseDefinition("duplicate-name", "Leg press", "Leg press 01", "Seat 9"))
        assertTrue(Metrics.referenceWeights(dataset, ComparisonContext("duplicate-name", "Seat 9")).isEmpty())
        val gaps = Metrics.performance(dataset, context(seed()), null, "2026-10-14", "2026-10-14")
        assertTrue(gaps.all { it.bestReps == null && it.loadAt10 == null && it.volumeLoad == null })
        assertEquals("Seat 4 · backrest 2", dataset.exercises.first().defaultSetup)
    }

    @Test fun attendanceUses28CompleteCivilDaysAndFirstVisitSurvivesSpanningDaysAndUndo() {
        var dataset = log(seed(), actual = "2026-10-13")
        dataset = log(dataset, actual = "2026-10-14", setIndex = 1)
        assertEquals("2026-10-13", occurrence(dataset).firstVisit!!.localDate)
        val performance = Metrics.performance(dataset, context(dataset), BigDecimal("20.1"), monday, monday).single()
        assertEquals("2026-10-13", performance.actualDate); assertEquals("2026-10-14", performance.lastActualDate)
        val summary = Metrics.attendance(dataset, "2026-10-14")
        assertEquals(1, summary.planned); assertEquals(1, summary.logged)
        assertEquals(100.0, summary.percentage!!, 0.0); assertEquals(1.0, summary.averageDelayDays!!, 0.0)
        assertEquals(1, Metrics.attendance(dataset, "2026-11-09").logged)
        assertEquals(0, Metrics.attendance(dataset, "2026-11-10").logged)
        rejected { Training.updateSet(dataset, monday, entry(dataset).prescription.id, entry(dataset).sets.first().id,
            10, BigDecimal.ONE, true, ClockReading(ObservationTime("2026-10-13T00:00:00Z", "2026-10-12", "UTC"), 1, "boot")) }
        assertEquals(28, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse("2026-10-13"), LocalDate.parse("2026-11-10")))
    }
}
