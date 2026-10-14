package it.matteogrechi.gymassystant.feature

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteogrechi.gymassystant.R
import it.matteogrechi.gymassystant.app.GymViewModel
import it.matteogrechi.gymassystant.domain.*
import it.matteogrechi.gymassystant.ui.*
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun progressNumber(value: BigDecimal): String {
    val locale = LocalConfiguration.current.locales[0]?.takeIf { it.language in listOf("en", "it", "es") } ?: Locale.ENGLISH
    return value.stripTrailingZeros().toPlainString()
        .replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
}

// GYM-010/015/020/025: real, immediately qualifying sets; presentation never writes measurements.
@Composable
fun ProgressFeature(dataset: Dataset, vm: GymViewModel) {
    val today = currentDate(vm)
    var exerciseId by rememberSaveable { mutableStateOf(dataset.exercises.firstOrNull()?.id.orEmpty()) }
    val exercise = dataset.exercises.find { it.id == exerciseId } ?: dataset.exercises.firstOrNull()
    val contexts = remember(dataset.revision, exercise?.id) {
        exercise?.let { Metrics.contexts(dataset, it.id).ifEmpty {
            listOf(ComparisonContext(it.id, it.defaultSetup))
        } }.orEmpty()
    }
    var setup by rememberSaveable(exercise?.id) { mutableStateOf(contexts.firstOrNull()?.setup.orEmpty()) }
    val context = contexts.find { it.setup == setup } ?: contexts.firstOrNull()
    val weights = remember(dataset.revision, context) {
        context?.let { Metrics.referenceWeights(dataset, it) }.orEmpty()
    }
    val reference = dataset.settings.referenceWeights.find { it.context == context }?.kg
        ?.takeIf { stored -> weights.any { it.compareTo(stored) == 0 } }
    var metric by rememberSaveable { mutableIntStateOf(0) }
    val observations = remember(dataset.revision, context, reference, today) {
        context?.let { Metrics.performance(dataset, it, reference, dataset.createdDate, today) }.orEmpty()
    }
    var selectedId by rememberSaveable(exercise?.id, context?.setup) {
        mutableStateOf(observations.lastOrNull()?.occurrenceId.orEmpty())
    }
    val selected = observations.find { it.occurrenceId == selectedId } ?: observations.lastOrNull()
    val completed = observations.sumOf { it.completedWorkingSets }
    val sessionNames = remember(dataset.revision, observations) {
        observations.associate { it.occurrenceId to Training.openOccurrence(dataset, it.plannedDate)?.sessionName.orEmpty() }
    }
    val unit = stringResource(when (metric) {
        0 -> R.string.progress_unit_reps
        1 -> R.string.progress_unit_kg
        else -> R.string.progress_unit_volume
    })
    val title = when (metric) {
        0 -> reference?.let { stringResource(R.string.best_reps, progressNumber(it)) }
            ?: stringResource(R.string.progress_reps_reference)
        1 -> stringResource(R.string.weight_at_ten)
        else -> stringResource(R.string.volume_load)
    }
    val points = observations.map { ProgressPlotObservation(it.occurrenceId,
        it.actualDate ?: it.plannedDate, progressValue(it, metric)) }
    ScreenColumn {
        Heading(stringResource(R.string.progress))
        Spacer(Modifier.height(6.dp))
        DropdownField(stringResource(R.string.exercise), dataset.exercises.map {
            listOf(it.name, it.machineVariant).filter(String::isNotBlank).joinToString(" · ")
        }, dataset.exercises.indexOf(exercise), { exerciseId = dataset.exercises[it].id })
        if (context != null) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(stringResource(R.string.setup), contexts.map {
                    it.setup.ifBlank { stringResource(R.string.progress_no_setup) }
                }, contexts.indexOf(context), { setup = contexts[it].setup }, Modifier.weight(1f))
                DropdownField(stringResource(R.string.reference_weight),
                    listOf(stringResource(R.string.progress_choose_weight)) + weights.map { progressNumber(it) },
                    weights.indexOfFirst { reference != null && it.compareTo(reference) == 0 } + 1,
                    { index ->
                        if (index > 0) {
                            val kg = weights[index - 1]
                            vm.change({ state, _ ->
                                Training.updateSettings(state, state.settings.copy(referenceWeights =
                                    state.settings.referenceWeights.filter { it.context != context } + ReferenceWeight(context, kg)))
                            })
                        }
                    }, Modifier.weight(1f), enabled = weights.isNotEmpty())
            }
        }
        Spacer(Modifier.height(6.dp))
        Selector(listOf(stringResource(R.string.metric_reps), stringResource(R.string.metric_weight),
            stringResource(R.string.metric_volume)), metric, { metric = it })
        Spacer(Modifier.height(8.dp))
        Surface(color = GymColors.surfaceContainer, contentColor = GymColors.onSurface, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (completed == 0) {
                    Help(stringResource(R.string.no_history), Modifier.padding(top = 8.dp, bottom = 8.dp))
                } else if (selected != null) {
                    Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val value = progressValue(selected, metric)
                            Text(value?.let { progressNumber(it) } ?: stringResource(R.string.unavailable),
                                Modifier.weight(1f), style = if (value == null) MaterialTheme.typography.titleMedium
                                    else MaterialTheme.typography.displaySmall)
                            if (value != null) Help(unit)
                        }
                        Help(progressObservationLabel(selected, sessionNames[selected.occurrenceId].orEmpty()))
                        val sameDate = observations.filter { (it.actualDate ?: it.plannedDate) == (selected.actualDate ?: selected.plannedDate) }
                        if (sameDate.size > 1) Help(stringResource(R.string.progress_same_day_position,
                            sameDate.sortedBy { it.occurrenceId }.indexOf(selected) + 1, sameDate.size))
                    }
                    if (metric == 0 && reference == null) Help(stringResource(R.string.progress_reference_needed))
                    Spacer(Modifier.height(4.dp))
                    ProgressPlot(points, selected.occurrenceId, unit,
                        stateKey = "${exercise?.id}/${context?.setup}",
                        onSelect = { selectedId = it },
                        descriptions = observations.map {
                            val value = progressValue(it, metric)
                            val sameDate = observations.filter { candidate -> (candidate.actualDate ?: candidate.plannedDate) == (it.actualDate ?: it.plannedDate) }
                            val position = if (sameDate.size > 1) " · " + stringResource(R.string.progress_same_day_position,
                                sameDate.sortedBy { candidate -> candidate.occurrenceId }.indexOf(it) + 1, sameDate.size) else ""
                            progressObservationLabel(it, sessionNames[it.occurrenceId].orEmpty()) + position + " · " +
                                (value?.let { number -> progressNumber(number) + " " + unit }
                                    ?: stringResource(R.string.unavailable))
                        })
                }
            }
        }
        Disclosure(stringResource(R.string.how_compare)) {
            if (exercise != null) Help(listOf(exercise.name, exercise.machineVariant).filter(String::isNotBlank).joinToString(" · "))
            Help(stringResource(R.string.comparison_note))
            Help(stringResource(R.string.completed_working_sets, completed))
            Help(stringResource(R.string.progress_history_period, dateLabel(dataset.createdDate), dateLabel(today)))
        }
        ProgressAttendance(dataset, today)
    }
}

internal fun progressValue(observation: PerformanceObservation, metric: Int): BigDecimal? = when (metric) {
    0 -> observation.bestReps?.let { BigDecimal(it) }
    1 -> observation.loadAt10
    else -> observation.volumeLoad
}

@Composable
private fun progressObservationLabel(observation: PerformanceObservation, sessionName: String): String {
    val actual = observation.actualDate
    val dates = if (actual == null) stringResource(R.string.progress_unlogged_date, dateLabel(observation.plannedDate))
        else if (actual != observation.plannedDate) stringResource(R.string.planned_actual,
            dateLabel(observation.plannedDate), dateLabel(actual)) else dateLabel(actual)
    val through = observation.lastActualDate?.takeIf { it != actual }?.let {
        stringResource(R.string.progress_logged_through, dateLabel(it))
    }
    return listOfNotNull(sessionName.takeIf(String::isNotBlank), dates, through,
        if (actual != null && observation.unfinished) stringResource(R.string.progress_unfinished) else null).joinToString(" · ")
}

// GYM-020: use the domain's past due-date cohort and first visit, including catch-up through today.
@Composable
private fun ProgressAttendance(dataset: Dataset, today: String) {
    val summary = remember(dataset.revision, today) { Metrics.attendance(dataset, today) }
    val locale = LocalConfiguration.current.locales[0]?.takeIf { it.language in listOf("en", "it", "es") } ?: Locale.ENGLISH
    val format = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 } }
    Spacer(Modifier.height(18.dp))
    Surface(Modifier.fillMaxWidth(), color = GymColors.surfaceContainer, contentColor = GymColors.onSurface, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(R.string.attendance), style = MaterialTheme.typography.titleSmall)
            Help(stringResource(R.string.attendance_window), Modifier.padding(top = 4.dp))
            if (summary.planned == 0) {
                Text(stringResource(R.string.unavailable), Modifier.padding(vertical = 10.dp),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp))
                Help(stringResource(R.string.progress_attendance_empty))
            } else {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.progress_attendance_count, summary.logged, summary.planned),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp))
                    Column {
                        Help(stringResource(R.string.progress_sessions_logged))
                        Text(summary.percentage?.let { NumberFormat.getNumberInstance(locale).apply {
                            maximumFractionDigits = 0
                        }.format(it) + "%" } ?: stringResource(R.string.unavailable),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    listOf(summary.late to stringResource(R.string.progress_late),
                        summary.planned - summary.logged to stringResource(R.string.progress_attendance_unlogged).lowercase(locale)).forEach { (count, label) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(format.format(count), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Disclosure(stringResource(R.string.attendance_details)) {
                    listOf(
                        stringResource(R.string.on_time) to format.format(summary.observations.count { it.delayDays == 0L }),
                        stringResource(R.string.early) to format.format(summary.observations.count { (it.delayDays ?: 0) < 0 }),
                        stringResource(R.string.average_delay) to (summary.averageDelayDays?.let { delay ->
                            stringResource(R.string.days_value, format.format(delay))
                        } ?: stringResource(R.string.unavailable)),
                    ).forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Help(label, Modifier.weight(1f))
                            Text(value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}
