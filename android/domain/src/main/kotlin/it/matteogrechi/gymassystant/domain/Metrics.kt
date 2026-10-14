package it.matteogrechi.gymassystant.domain

import java.math.BigDecimal
import java.time.temporal.ChronoUnit

// GYM-010/014/020: zero-rep placeholders, unrecorded actuals and warm-ups are never performance.
fun SetLog.qualifies(): Boolean = kind == SetKind.WORKING && done && reps != null && reps > 0 &&
    kg != null && kg.signum() >= 0

data class PerformanceObservation(
    val occurrenceId: String,
    val planId: String,
    val plannedDate: String,
    val actualDate: String?,
    val lastActualDate: String?,
    val unfinished: Boolean,
    val bestReps: Int?,
    val loadAt10: BigDecimal?,
    val volumeLoad: BigDecimal?,
    val completedWorkingSets: Int,
)

data class AttendanceObservation(val occurrenceId: String, val planId: String, val plannedDate: String,
    val actualDate: String?, val delayDays: Long?)
data class AttendanceSummary(
    val planned: Int,
    val logged: Int,
    val late: Int,
    val percentage: Double?,
    val averageDelayDays: Double?,
    val observations: List<AttendanceObservation>,
)

// GYM-010: stable exercise identity includes its machine; raw setup matches exactly and effort/ROM remain unadjusted.
object Metrics {
    fun contexts(dataset: Dataset, exerciseId: String): List<ComparisonContext> = dataset.occurrences
        .flatMap { it.exercises }.filter { it.definition.id == exerciseId }
        .map { ComparisonContext(exerciseId, it.setup) }.distinct()

    fun referenceWeights(dataset: Dataset, context: ComparisonContext): List<BigDecimal> = dataset.occurrences
        .flatMap { comparableSets(it, context) }.filter(SetLog::qualifies).map { it.kg!!.stripTrailingZeros() }
        .distinct().sorted()

    fun performance(dataset: Dataset, context: ComparisonContext, referenceKg: BigDecimal?,
        startDate: String, endDate: String): List<PerformanceObservation> {
        referenceKg?.let { kg ->
            decimal(kg)
            require(referenceWeights(dataset, context).any { it.compareTo(kg) == 0 }) { "Choose a recorded reference weight" }
        }
        return occurrences(dataset, startDate, endDate).filter { occurrence ->
            occurrence.exercises.any { it.definition.id == context.exerciseId && it.setup == context.setup }
        }.map { occurrence ->
            val sets = comparableSets(occurrence, context).filter(SetLog::qualifies)
            val dates = sets.mapNotNull { it.observedAt?.localDate }.sorted()
            PerformanceObservation(occurrence.id, occurrence.planId, occurrence.plannedDate, dates.firstOrNull(), dates.lastOrNull(),
                occurrence.lifecycle != Lifecycle.FINISHED,
                if (referenceKg == null) null else sets.filter { it.kg!!.compareTo(referenceKg) == 0 }.maxOfOrNull { it.reps!! },
                sets.filter { it.reps == 10 }.maxOfOrNull { it.kg!! },
                if (sets.isEmpty()) null else sets.fold(BigDecimal.ZERO) { total, set -> total + set.kg!! * BigDecimal(set.reps!!) },
                sets.size)
        }.sortedBy { it.actualDate ?: it.plannedDate }
    }

    // GYM-020: cohort excludes today; catch-up through today retains the original obligation and first visit.
    fun attendance(dataset: Dataset, today: String): AttendanceSummary {
        val now = date(today)
        val observations = attendanceObservations(occurrences(dataset, now.minusDays(28).toString(), now.minusDays(1).toString()), today)
        val logged = observations.count { it.actualDate != null }
        val late = observations.mapNotNull { it.delayDays }.filter { it > 0 }
        return AttendanceSummary(observations.size, logged, late.size,
            if (observations.isEmpty()) null else 100.0 * logged / observations.size,
            if (late.isEmpty()) null else late.average(), observations)
    }

    fun attendanceRecords(dataset: Dataset, today: String): List<AttendanceObservation> {
        return attendanceObservations(occurrences(dataset, dataset.createdDate, today), today)
    }

    private fun attendanceObservations(occurrences: List<Occurrence>, today: String): List<AttendanceObservation> {
        val now = date(today)
        return occurrences.map { occurrence ->
            val actual = occurrence.firstVisit?.localDate?.takeIf { date(it) <= now }
            AttendanceObservation(occurrence.id, occurrence.planId, occurrence.plannedDate, actual,
                actual?.let { ChronoUnit.DAYS.between(date(occurrence.plannedDate), date(it)) })
        }
    }

    fun pending(dataset: Dataset, today: String): List<Occurrence> {
        val now = date(today)
        return occurrences(dataset, now.minusDays(28).toString(), now.minusDays(1).toString())
            .filter { it.firstVisit == null && it.lifecycle != Lifecycle.FINISHED }
    }

    private fun comparableSets(occurrence: Occurrence, context: ComparisonContext): List<SetLog> = occurrence.exercises
        .filter { it.definition.id == context.exerciseId && it.setup == context.setup }.flatMap { it.sets }

    private fun occurrences(dataset: Dataset, startDate: String, endDate: String): List<Occurrence> {
        val start = maxOf(date(startDate), date(dataset.createdDate))
        val end = date(endDate)
        if (start > end) return emptyList()
        return generateSequence(start) { if (it < end) it.plusDays(1) else null }
            .mapNotNull { Training.openOccurrence(dataset, it.toString()) }.toList()
    }
}
