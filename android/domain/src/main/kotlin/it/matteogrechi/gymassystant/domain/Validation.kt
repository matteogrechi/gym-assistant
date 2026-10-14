package it.matteogrechi.gymassystant.domain

import java.math.BigDecimal
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.json.Json

// GYM-023/024: strict known fields prevent silently treating an incompatible file as user history.
val DatasetJson = Json { encodeDefaults = true }

fun encodeDataset(dataset: Dataset): String {
    validateDataset(dataset)
    return DatasetJson.encodeToString(Dataset.serializer(), dataset)
}

fun decodeDataset(json: String): Dataset = DatasetJson.decodeFromString(Dataset.serializer(), json).also(::validateDataset)

internal fun date(value: String): LocalDate = try {
    LocalDate.parse(value).also { require(it.toString() == value) }
} catch (error: DateTimeException) {
    throw IllegalArgumentException("Invalid civil date", error)
}
internal fun identity(value: String) { require(value.isNotBlank() && value.length <= 200) { "Invalid identity" } }
internal fun text(value: String, max: Int, required: Boolean = false) {
    require(value.length <= max && (!required || value.trim().isNotEmpty())) { "Invalid text" }
}
internal fun decimal(value: BigDecimal) {
    require(value.signum() >= 0 && value.precision() <= 128 && value.scale() in -128..128) { "Invalid kg" }
}
internal fun observation(value: ObservationTime) {
    try {
        val instant = Instant.parse(value.instant)
        require(instant.atZone(ZoneId.of(value.timeZone)).toLocalDate() == date(value.localDate)) { "Observation date/zone mismatch" }
    } catch (error: DateTimeException) {
        throw IllegalArgumentException("Invalid observation time/zone", error)
    }
}
internal fun clock(value: ClockReading) {
    observation(value.observation)
    require(value.monotonicMs >= 0)
    value.bootId?.let(::identity)
}
private fun unique(values: List<String>) { values.forEach(::identity); require(values.distinct().size == values.size) { "Duplicate identity" } }

private fun validatePrescription(entry: Prescription, definitions: Set<String>) {
    identity(entry.id); require(entry.exerciseId in definitions) { "Unknown exercise" }
    require(entry.workingSets in 1..10 && entry.minReps in 1..50 && entry.maxReps in entry.minReps..50)
    require(entry.recoverySeconds in 1..3600)
}

// GYM-018/026/028: incomplete drafts are retained, but only valid prescriptions can be saved.
fun validatePlan(plan: Plan, definitions: List<ExerciseDefinition>) {
    identity(plan.id); identity(plan.revisionId); text(plan.name, 80, true)
    require(plan.sessions.size in 1..7)
    unique(plan.sessions.map { it.id })
    unique(plan.sessions.flatMap { it.exercises }.map { it.id })
    val definitionIds = definitions.map { it.id }.toSet()
    for (session in plan.sessions) {
        text(session.name, 80, true); text(session.focus, 160)
        require(session.exercises.isNotEmpty())
        session.exercises.forEach { validatePrescription(it, definitionIds) }
    }
    val sessions = plan.sessions.map { it.id }.toSet()
    require(plan.weeklySchedule.all { (day, id) -> day in 1..7 && id in sessions })
    plan.dateOverrides.forEach { (day, id) -> date(day); require(id == null || id in sessions) }
}

internal fun validateDefinition(value: ExerciseDefinition) {
    identity(value.id); text(value.name, 80, true); text(value.machineVariant, 80, true); text(value.defaultSetup, 160)
    require(value.name == value.name.trim() && value.machineVariant == value.machineVariant.trim())
}

private fun validateTimer(timer: TimerState, limit: Long?) {
    require(timer.elapsedMs >= 0 && (limit == null || timer.elapsedMs <= limit))
    require(timer.anchorMonotonicMs == null || timer.anchorMonotonicMs >= 0)
    timer.bootId?.let(::identity)
}

// GYM-004/012/014/017/020/023: validate references and frozen work at the persistence boundary.
fun validateDataset(dataset: Dataset) {
    require(dataset.format == "gym-assystant-status" && dataset.formatVersion == 3 && dataset.dataKind == "user_data")
    identity(dataset.id); val created = date(dataset.createdDate); require(dataset.revision >= 0)
    unique(dataset.exercises.map { it.id }); dataset.exercises.forEach(::validateDefinition)
    val definitionIds = dataset.exercises.map { it.id }.toSet()
    unique(dataset.plans.map { it.id }); dataset.plans.forEach { validatePlan(it, dataset.exercises) }
    unique(dataset.drafts.map { it.plan.id })
    dataset.drafts.forEach { draft ->
        identity(draft.plan.id); identity(draft.plan.revisionId)
        unique(draft.plan.sessions.map { it.id }); unique(draft.plan.sessions.flatMap { it.exercises }.map { it.id })
        val saved = dataset.plans.find { it.id == draft.plan.id }
        require(draft.baseRevisionId == saved?.revisionId) { "Stale draft" }
        require(draft.plan.sessions.flatMap { it.exercises }.all { entry -> dataset.exercises.any { it.id == entry.exerciseId } })
    }
    require(dataset.activePlanId == null || dataset.plans.any { it.id == dataset.activePlanId })
    val effectiveDates = dataset.schedules.map { date(it.effectiveFrom) }
    require(effectiveDates.all { it >= created } && effectiveDates.zipWithNext().all { (a, b) -> a < b })
    dataset.schedules.forEach { validatePlan(it.plan, dataset.exercises); require(dataset.plans.any { plan -> plan.id == it.plan.id }) }
    require(dataset.schedules.lastOrNull()?.plan?.id == dataset.activePlanId)
    val sessionOwners = mutableMapOf<String, String>()
    val entryOwners = mutableMapOf<String, Pair<String, String>>()
    val revisions = mutableMapOf<String, Plan>()
    for (plan in dataset.plans + dataset.schedules.map { it.plan }) {
        require(revisions.putIfAbsent(plan.revisionId, plan).let { it == null || it == plan }) { "Revision identity reused" }
    }
    for (plan in dataset.plans + dataset.schedules.map { it.plan } + dataset.drafts.map { it.plan }) {
        for (session in plan.sessions) {
            require(sessionOwners.putIfAbsent(session.id, plan.id).let { it == null || it == plan.id })
            for (entry in session.exercises) {
                val owner = plan.id to session.id
                require(entryOwners.putIfAbsent(entry.id, owner).let { it == null || it == owner })
            }
        }
    }
    unique(dataset.media.map { it.id })
    dataset.media.forEach { media ->
        require(dataset.exercises.any { it.id == media.exerciseId }); text(media.originalName, 255, true)
        require(media.size > 0 && media.mime.startsWith("video/"))
        require(media.sha256.matches(Regex("[a-fA-F0-9]{64}")))
        require(media.relativePath.isNotBlank() && !media.relativePath.startsWith("/") && '\\' !in media.relativePath)
        require(media.relativePath.split('/').all { it.isNotEmpty() && it != "." && it != ".." && ':' !in it })
    }
    dataset.exercises.forEach { exercise ->
        require(exercise.mediaId == null || dataset.media.any { it.id == exercise.mediaId && it.exerciseId == exercise.id })
    }
    unique(dataset.occurrences.map { it.id }); unique(dataset.occurrences.map { it.plannedDate })
    unique(dataset.occurrences.flatMap { it.exercises }.flatMap { it.sets }.map { it.id })
    dataset.occurrences.forEach { occurrence ->
        require(date(occurrence.plannedDate) >= created && occurrence.protected)
        identity(occurrence.planRevisionId); identity(occurrence.sessionId)
        text(occurrence.planName, 80, true); text(occurrence.sessionName, 80, true); text(occurrence.focus, 160)
        require(dataset.plans.any { it.id == occurrence.planId })
        require(sessionOwners.putIfAbsent(occurrence.sessionId, occurrence.planId).let { it == null || it == occurrence.planId })
        require(occurrence.exercises.isNotEmpty()); unique(occurrence.exercises.map { it.prescription.id })
        occurrence.firstVisit?.let { observation(it); require(date(it.localDate) >= date(occurrence.plannedDate)) }
        require((occurrence.lifecycle == Lifecycle.FINISHED) == (occurrence.finishedAt != null))
        occurrence.finishedAt?.let { observation(it); require(date(it.localDate) >= date(occurrence.plannedDate)) }
        occurrence.exercises.forEach { entry ->
            validateDefinition(entry.definition); require(dataset.exercises.any { it.id == entry.definition.id })
            val owner = occurrence.planId to occurrence.sessionId
            require(entryOwners.putIfAbsent(entry.prescription.id, owner).let { it == null || it == owner })
            require(dataset.exercises.single { it.id == entry.definition.id }.copy(mediaId = entry.definition.mediaId) == entry.definition)
            require(entry.prescription.exerciseId == entry.definition.id)
            validatePrescription(entry.prescription, definitionIds)
            text(entry.setup, 160)
            require(entry.sets.count { it.kind == SetKind.WORKING } == entry.prescription.workingSets)
            validateTimer(entry.executionTimer, null); validateTimer(entry.recoveryTimer, entry.prescription.recoverySeconds * 1000L)
            if (occurrence.lifecycle == Lifecycle.PAUSED || occurrence.lifecycle == Lifecycle.FINISHED) {
                require(!entry.executionTimer.running && !entry.recoveryTimer.running)
            }
            entry.sets.forEach { set ->
                require(set.reps == null || set.reps >= 0); set.kg?.let(::decimal)
                set.observedAt?.let { observation(it); require(date(it.localDate) >= date(occurrence.plannedDate)) }
                require(!set.qualifies() || set.observedAt != null)
            }
            require(entry.sets.none { it.qualifies() } || occurrence.firstVisit != null)
        }
    }
    require(dataset.settings.referenceWeights.map { it.context }.distinct().size == dataset.settings.referenceWeights.size)
    dataset.settings.referenceWeights.forEach { preference ->
        require(dataset.exercises.any { it.id == preference.context.exerciseId }); text(preference.context.setup, 160); decimal(preference.kg)
    }
}
