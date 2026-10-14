package it.matteogrechi.gymassystant.domain

import java.math.BigDecimal
import java.util.UUID

fun initialDataset(clock: ClockReading): Dataset = Training.newDataset(clock)

// GYM-008/016/018/026/028: transformations return a new dataset; the Room writer owns revision increments.
object Training {
    fun validate(dataset: Dataset) = validateDataset(dataset)

    fun newDataset(clock: ClockReading): Dataset {
        clock(clock)
        val datasetId = UUID.randomUUID().toString()
        fun id(name: String) = stableId("$datasetId/$name")
        val definitions = listOf(
            ExerciseDefinition(id("leg"), "Leg press", "Leg press 01", "Seat 4 · backrest 2"),
            ExerciseDefinition(id("chest"), "Chest press", "Chest press 01", "Seat 3"),
            ExerciseDefinition(id("lat"), "Lat pulldown", "Pulldown 01", "Thigh pad 2"),
        )
        val foundationSessions = (0..2).map { index ->
            val letter = ('A'.code + index).toChar()
            val order = definitions.drop(index) + definitions.take(index)
            SessionTemplate(id("session-$letter"), "Session $letter", listOf("Lower body first", "Push first", "Pull first")[index],
                order.map { exercise -> Prescription(id("$letter/${exercise.id}"), exercise.id, 3,
                    if (index == 1) 8 else 10, if (index == 1) 10 else 12) })
        }
        val foundations = Plan(id("foundations"), id("foundations/r1"), "Full body foundations", foundationSessions,
            mapOf(1 to foundationSessions[0].id, 3 to foundationSessions[1].id, 5 to foundationSessions[2].id))
        val routineSessions = (0..1).map { index ->
            val order = if (index == 0) definitions.drop(1) else definitions.take(2)
            SessionTemplate(id("routine-$index"), if (index == 0) "Upper body" else "Lower body", exercises =
                order.map { Prescription(id("routine-$index/${it.id}"), it.id, 2, 8, 10) })
        }
        val routine = Plan(id("routine"), id("routine/r1"), "Two-day routine", routineSessions,
            mapOf(1 to routineSessions[0].id, 4 to routineSessions[1].id))
        return accepted(Dataset(datasetId, clock.observation.localDate, exercises = definitions,
            plans = listOf(foundations, routine), activePlanId = foundations.id,
            schedules = listOf(EffectiveSchedule(clock.observation.localDate, foundations))))
    }

    fun createExercise(dataset: Dataset, definition: ExerciseDefinition): Dataset {
        require(dataset.exercises.none { it.id == definition.id }) { "Exercise already exists" }
        require(definition.mediaId == null)
        val trimmed = definition.copy(name = definition.name.trim(), machineVariant = definition.machineVariant.trim(), defaultSetup = definition.defaultSetup.trim())
        validateDefinition(trimmed)
        return accepted(dataset.copy(exercises = dataset.exercises + trimmed))
    }

    fun attachMedia(dataset: Dataset, media: MediaMetadata): Dataset {
        require(dataset.media.none { it.id == media.id }) { "Media identity already exists" }
        require(dataset.exercises.any { it.id == media.exerciseId })
        return accepted(dataset.copy(media = dataset.media + media,
            exercises = dataset.exercises.map { if (it.id == media.exerciseId) it.copy(mediaId = media.id) else it }))
    }

    fun updateSettings(dataset: Dataset, settings: AppSettings): Dataset = accepted(dataset.copy(settings = settings))

    fun beginEdit(dataset: Dataset, planId: String): Dataset {
        if (dataset.drafts.any { it.plan.id == planId }) return dataset
        return retainDraft(dataset, dataset.plans.single { it.id == planId })
    }

    fun retainDraft(dataset: Dataset, plan: Plan): Dataset {
        val saved = dataset.plans.find { it.id == plan.id }
        require(saved == null || plan.revisionId == saved.revisionId) { "Stale draft" }
        val draft = PlanDraft(plan, saved?.revisionId)
        return accepted(dataset.copy(drafts = dataset.drafts.filterNot { it.plan.id == plan.id } + draft))
    }

    fun commitDraft(dataset: Dataset, planId: String, revisionId: String, today: String): Dataset {
        val day = date(today); require(day >= date(dataset.createdDate))
        val draft = dataset.drafts.single { it.plan.id == planId }
        val previous = dataset.plans.find { it.id == planId }
        require(draft.baseRevisionId == previous?.revisionId)
        val plan = draft.plan.copy(revisionId = revisionId)
        validatePlan(plan, dataset.exercises)
        require((dataset.plans + dataset.schedules.map { it.plan } + dataset.occurrences.map {
            Plan(it.planId, it.planRevisionId, it.planName, emptyList())
        }).none { it.revisionId == revisionId }) { "Revision identity already exists" }
        val oldOverrides = previous?.dateOverrides.orEmpty()
        (oldOverrides.keys + plan.dateOverrides.keys).filter { date(it) < day }.forEach {
            // Effective schedule revisions retain past obligations when a template is removed.
            val removedTemplate = oldOverrides[it]?.let { id -> plan.sessions.none { session -> session.id == id } } == true
            require((removedTemplate && !plan.dateOverrides.containsKey(it)) ||
                (oldOverrides.containsKey(it) == plan.dateOverrides.containsKey(it) && oldOverrides[it] == plan.dateOverrides[it])) { "Cannot change past date assignments" }
        }
        val result = dataset.copy(plans = dataset.plans.filterNot { it.id == planId } + plan,
            drafts = dataset.drafts.filterNot { it.plan.id == planId })
        return accepted(if (dataset.activePlanId == planId) effective(result, plan, today) else result)
    }

    fun activatePlan(dataset: Dataset, planId: String, today: String): Dataset {
        require(date(today) >= date(dataset.createdDate))
        val saved = dataset.plans.single { it.id == planId }
        require(dataset.drafts.none { it.plan.id == planId && it.plan != saved }) { "Save changes before activating" }
        return accepted(effective(dataset.copy(activePlanId = planId), saved, today))
    }

    fun discardNewDraft(dataset: Dataset, planId: String): Dataset {
        require(dataset.plans.none { it.id == planId }) { "Cannot discard a saved plan" }
        require(dataset.drafts.any { it.plan.id == planId && it.baseRevisionId == null })
        return accepted(dataset.copy(drafts = dataset.drafts.filterNot { it.plan.id == planId }))
    }

    fun removeSession(plan: Plan, sessionId: String): Plan {
        require(plan.sessions.any { it.id == sessionId })
        return plan.copy(sessions = plan.sessions.filterNot { it.id == sessionId },
            weeklySchedule = plan.weeklySchedule.filterValues { it != sessionId },
            dateOverrides = plan.dateOverrides.filterValues { it != sessionId })
    }

    // GYM-016/020: schedule intervals reconstruct missed workouts even after weeks without opening the app.
    fun openOccurrence(dataset: Dataset, plannedDate: String): Occurrence? {
        val day = date(plannedDate)
        if (day < date(dataset.createdDate)) return null
        dataset.occurrences.find { it.plannedDate == plannedDate }?.let { return it }
        val plan = dataset.schedules.lastOrNull { date(it.effectiveFrom) <= day }?.plan ?: return null
        val sessionId = if (plan.dateOverrides.containsKey(plannedDate)) plan.dateOverrides[plannedDate]
            else plan.weeklySchedule[day.dayOfWeek.value]
        val session = plan.sessions.find { it.id == sessionId } ?: return null
        val occurrenceId = stableId("${dataset.id}/$plannedDate")
        val entries = session.exercises.map { prescription ->
            val definition = dataset.exercises.single { it.id == prescription.exerciseId }
            ExerciseOccurrence(prescription, definition, definition.defaultSetup, (0 until prescription.workingSets).map { index ->
                SetLog(stableId("$occurrenceId/${prescription.id}/$index"), SetKind.WORKING)
            })
        }
        return Occurrence(occurrenceId, plannedDate, plan.id, plan.revisionId, plan.name,
            session.id, session.name, session.focus, entries)
    }

    // GYM-004/005/014/020: completion and first attendance are recorded in one transformation.
    fun updateSet(dataset: Dataset, plannedDate: String, entryId: String, setId: String,
        reps: Int?, kg: BigDecimal?, done: Boolean, clock: ClockReading): Dataset =
        mutate(dataset, plannedDate, clock) { occurrence ->
            require(reps == null || reps >= 0); kg?.let(::decimal)
            updateEntry(occurrence, entryId) { entry ->
                require(entry.sets.any { it.id == setId })
                entry.copy(sets = entry.sets.map { set ->
                    if (set.id != setId) set else set.copy(reps = reps, kg = kg, done = done,
                        observedAt = if (done && reps != null && reps > 0 && kg != null) set.observedAt ?: clock.observation else null)
                })
            }.let { changed ->
                if (changed.firstVisit == null && changed.exercises.any { it.sets.any(SetLog::qualifies) })
                    changed.copy(firstVisit = clock.observation) else changed
            }
        }

    fun setSetup(dataset: Dataset, plannedDate: String, entryId: String, setup: String, clock: ClockReading): Dataset =
        mutate(dataset, plannedDate, clock) { occurrence ->
            text(setup, 160); updateEntry(occurrence, entryId) { it.copy(setup = setup) }
        }

    fun addWarmUp(dataset: Dataset, plannedDate: String, entryId: String, setId: String, clock: ClockReading): Dataset =
        mutate(dataset, plannedDate, clock) { occurrence ->
            identity(setId)
            require(dataset.occurrences.flatMap { it.exercises }.flatMap { it.sets }.none { it.id == setId })
            require(occurrence.exercises.flatMap { it.sets }.none { it.id == setId })
            updateEntry(occurrence, entryId) { it.copy(sets = it.sets + SetLog(setId, SetKind.WARM_UP)) }
        }

    fun removeWarmUp(dataset: Dataset, plannedDate: String, entryId: String, setId: String, clock: ClockReading): Dataset =
        mutate(dataset, plannedDate, clock) { occurrence ->
            updateEntry(occurrence, entryId) { entry ->
                require(entry.sets.any { it.id == setId && it.kind == SetKind.WARM_UP })
                entry.copy(sets = entry.sets.filterNot { it.id == setId })
            }
        }

    // GYM-019/026: future reorder is allowed, and pins the entire preview without permitting performance.
    fun reorderExercises(dataset: Dataset, plannedDate: String, entryIds: List<String>, clock: ClockReading): Dataset =
        mutate(dataset, plannedDate, clock, allowFuture = true) { occurrence ->
            require(entryIds.size == occurrence.exercises.size && entryIds.toSet() == occurrence.exercises.map { it.prescription.id }.toSet())
            occurrence.copy(exercises = entryIds.map { id -> occurrence.exercises.single { it.prescription.id == id } })
        }

    // GYM-017: confirmation is a caller decision; requiring it never mutates clocks or logs.
    fun needsFinishConfirmation(occurrence: Occurrence): Boolean = occurrence.exercises.any { entry ->
        entry.sets.any { it.kind == SetKind.WORKING && !it.done }
    }

    fun firstUnfinishedEntryId(occurrence: Occurrence): String =
        (occurrence.exercises.find { entry -> entry.sets.any { it.kind == SetKind.WORKING && !it.done } }
            ?: occurrence.exercises.first()).prescription.id

    fun transition(dataset: Dataset, plannedDate: String, action: SessionAction, clock: ClockReading,
        confirmedIncomplete: Boolean = false): Dataset = mutate(dataset, plannedDate, clock) { occurrence ->
        val next = when (action) {
            SessionAction.START -> { require(occurrence.lifecycle == Lifecycle.READY); Lifecycle.IN_PROGRESS }
            SessionAction.RESUME -> { require(occurrence.lifecycle == Lifecycle.PAUSED); Lifecycle.IN_PROGRESS }
            SessionAction.PAUSE -> { require(occurrence.lifecycle == Lifecycle.IN_PROGRESS); Lifecycle.PAUSED }
            SessionAction.FINISH -> {
                require(occurrence.lifecycle == Lifecycle.IN_PROGRESS || occurrence.lifecycle == Lifecycle.PAUSED)
                require(confirmedIncomplete || !needsFinishConfirmation(occurrence)) { "Confirm incomplete Finish" }
                Lifecycle.FINISHED
            }
        }
        val settled = if (action == SessionAction.PAUSE || action == SessionAction.FINISH) pauseAll(occurrence, clock) else occurrence
        settled.copy(lifecycle = next, finishedAt = if (next == Lifecycle.FINISHED) clock.observation else null)
    }

    // GYM-012/017: timer mode/actions never imply a session lifecycle transition.
    fun changeTimer(dataset: Dataset, plannedDate: String, entryId: String, mode: TimerMode,
        action: TimerAction, clock: ClockReading): Dataset = mutate(dataset, plannedDate, clock) { occurrence ->
        require(action != TimerAction.START || occurrence.lifecycle != Lifecycle.PAUSED) { "Resume session before starting a timer" }
        updateEntry(occurrence, entryId) { entry ->
            if (mode == TimerMode.EXECUTION) entry.copy(executionTimer = Timers.change(entry.executionTimer, action, clock, null))
            else entry.copy(recoveryTimer = Timers.change(entry.recoveryTimer, action, clock, entry.prescription.recoverySeconds * 1000L))
        }
    }

    fun checkpoint(dataset: Dataset, clock: ClockReading): Dataset {
        clock(clock)
        return accepted(dataset.copy(occurrences = dataset.occurrences.map { occurrence ->
            if (occurrence.lifecycle == Lifecycle.FINISHED) occurrence else occurrence.copy(exercises = occurrence.exercises.map { entry ->
                entry.copy(executionTimer = Timers.settle(entry.executionTimer, clock),
                    recoveryTimer = Timers.settle(entry.recoveryTimer, clock, entry.prescription.recoverySeconds * 1000L))
            })
        }))
    }

    // GYM-024/027: export is a paused copy, never a change to the live clocks or a portable storage grant.
    fun portableSnapshot(dataset: Dataset, clock: ClockReading): Dataset {
        clock(clock)
        return accepted(dataset.copy(settings = dataset.settings.copy(backupTreeUri = null), occurrences = dataset.occurrences.map { occurrence ->
            occurrence.copy(exercises = occurrence.exercises.map { entry ->
                entry.copy(executionTimer = portableTimer(entry.executionTimer, clock, null),
                    recoveryTimer = portableTimer(entry.recoveryTimer, clock, entry.prescription.recoverySeconds * 1000L))
            })
        }))
    }

    private fun portableTimer(timer: TimerState, clock: ClockReading, limit: Long?): TimerState =
        Timers.pause(timer, clock, limit).copy(capturedRunning = timer.running || timer.capturedRunning)

    private fun effective(dataset: Dataset, plan: Plan, today: String): Dataset {
        require(dataset.schedules.lastOrNull()?.let { date(it.effectiveFrom) <= date(today) } != false) { "Cannot rewrite earlier schedule intervals" }
        return dataset.copy(schedules = dataset.schedules.filterNot { it.effectiveFrom == today } + EffectiveSchedule(today, plan))
    }

    private fun mutate(dataset: Dataset, plannedDate: String, clock: ClockReading, allowFuture: Boolean = false,
        change: (Occurrence) -> Occurrence): Dataset {
        clock(clock)
        val occurrence = requireNotNull(openOccurrence(dataset, plannedDate)) { "No session planned" }
        require(occurrence.lifecycle != Lifecycle.FINISHED) { "Finished workout is immutable" }
        require(allowFuture || date(plannedDate) <= date(clock.observation.localDate)) { "Future workout is a preview" }
        val changed = change(occurrence).copy(protected = true)
        return accepted(dataset.copy(occurrences = dataset.occurrences.filterNot { it.plannedDate == plannedDate } + changed))
    }

    private fun updateEntry(occurrence: Occurrence, entryId: String, change: (ExerciseOccurrence) -> ExerciseOccurrence): Occurrence {
        require(occurrence.exercises.any { it.prescription.id == entryId }) { "Unknown entry" }
        return occurrence.copy(exercises = occurrence.exercises.map { if (it.prescription.id == entryId) change(it) else it })
    }

    private fun pauseAll(occurrence: Occurrence, clock: ClockReading): Occurrence = occurrence.copy(exercises = occurrence.exercises.map { entry ->
        entry.copy(executionTimer = Timers.pause(entry.executionTimer, clock),
            recoveryTimer = Timers.pause(entry.recoveryTimer, clock, entry.prescription.recoverySeconds * 1000L))
    })

    private fun accepted(dataset: Dataset): Dataset = dataset.also(::validateDataset)
    private fun stableId(value: String): String = UUID.nameUUIDFromBytes(value.toByteArray(Charsets.UTF_8)).toString()
}
