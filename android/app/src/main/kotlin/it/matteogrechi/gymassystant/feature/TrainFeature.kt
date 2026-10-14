package it.matteogrechi.gymassystant.feature

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteogrechi.gymassystant.R
import it.matteogrechi.gymassystant.app.GymViewModel
import it.matteogrechi.gymassystant.domain.*
import it.matteogrechi.gymassystant.ui.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle as AndroidLifecycle
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    CompositionLocalProvider(LocalReorderScroll provides scroll) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 24.dp), content = content)
    }
}

@Composable fun Help(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = GymColors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}
@Composable fun SectionTitle(title: String, note: String? = null) {
    FlowRow(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 13.dp),
        itemVerticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).widthIn(min = 100.dp * LocalDensity.current.fontScale),
            style = MaterialTheme.typography.titleSmall)
        if (note != null) Help(note)
    }
}
@Composable fun currentClock(vm: GymViewModel): ClockReading {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(vm.now(), vm, lifecycle) {
        lifecycle.repeatOnLifecycle(AndroidLifecycle.State.STARTED) {
            while (true) { value = vm.now(); delay(1000) }
        }
    }.value
}
@Composable fun currentDate(vm: GymViewModel): String {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(vm.now().observation.localDate, vm, lifecycle) {
        lifecycle.repeatOnLifecycle(AndroidLifecycle.State.STARTED) {
            while (true) { value = vm.now().observation.localDate; delay(1000) }
        }
    }.value
}
@Composable fun dateLabel(date: String, pattern: String = "EEE, d MMM yyyy"): String {
    val locale = appLocale()
    return LocalDate.parse(date).format(DateTimeFormatter.ofPattern(pattern, locale))
}
@Composable fun appLocale(): Locale {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.ENGLISH
    return if (locale.language in listOf("en", "it", "es")) locale else Locale.ENGLISH
}
@Composable fun lifecycleLabel(lifecycle: Lifecycle): String = stringResource(when (lifecycle) {
    Lifecycle.READY -> R.string.ready
    Lifecycle.IN_PROGRESS -> R.string.in_progress
    Lifecycle.PAUSED -> R.string.paused
    Lifecycle.FINISHED -> R.string.finished
})
fun workingDone(occurrence: Occurrence) = occurrence.exercises.sumOf { entry -> entry.sets.count { it.kind == SetKind.WORKING && it.done } }
fun workingTotal(occurrence: Occurrence) = occurrence.exercises.sumOf { it.prescription.workingSets }

private fun calendarBadge(dataset: Dataset, occurrence: Occurrence): String {
    val plan = dataset.schedules.lastOrNull {
        it.plan.id == occurrence.planId && it.plan.revisionId == occurrence.planRevisionId
    }?.plan ?: dataset.plans.find { it.id == occurrence.planId }
    val index = plan?.sessions?.indexOfFirst { it.id == occurrence.sessionId } ?: -1
    return if (index >= 0) ('A' + index).toString() else "—"
}

// GYM-008/016/020/025: a stable card, a fitted week and preserved overdue obligations.
@Composable
fun TrainFeature(dataset: Dataset, vm: GymViewModel, navigate: (String) -> Unit) {
    val today = currentDate(vm)
    var selected by rememberSaveable { mutableStateOf(today) }
    var week by rememberSaveable { mutableStateOf(today) }
    val occurrence = remember(dataset.revision, selected) { Training.openOccurrence(dataset, selected) }
    val active = dataset.plans.find { it.id == dataset.activePlanId }
    val monday = LocalDate.parse(week).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val pending = remember(dataset.revision, today) { Metrics.pending(dataset, today) }
    val upcoming = remember(dataset.revision, today) { (1L..28L).mapNotNull {
        Training.openOccurrence(dataset, LocalDate.parse(today).plusDays(it).toString())
    }.take(3) }
    val colors = GymColors
    ScreenColumn {
        Text(active?.name.orEmpty(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = colors.hero, contentColor = colors.onHero,
            modifier = Modifier.fillMaxWidth().height(224.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text((if (selected == today) stringResource(R.string.today).uppercase(appLocale()) + " · " else "") + dateLabel(selected),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.2.sp),
                    color = colors.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(occurrence?.sessionName ?: stringResource(R.string.no_session),
                    style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(occurrence?.let { listOf(if (it.planId != dataset.activePlanId) it.planName else "", it.focus).filter(String::isNotBlank).joinToString(" · ") }
                    ?: stringResource(R.string.assign_session),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = colors.onHeroVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Box(Modifier.height(48.dp).fillMaxWidth()) {
                    if (occurrence != null) GymButton(stringResource(if (selected > today) R.string.preview_session else R.string.view_session),
                        { navigate("session:$selected") }, Modifier.fillMaxWidth())
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.calendar), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            GymButton(stringResource(R.string.today), { selected = today; week = today }, emphasis = ButtonEmphasis.Text)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedIconButton({ week = monday.minusWeeks(1).toString(); selected = week }, Modifier.size(48.dp),
                border = BorderStroke(1.dp, colors.outline)) {
                Icon(GymIcons.Back, stringResource(R.string.previous_week), Modifier.size(24.dp))
            }
            Text(dateLabel(monday.toString(), "d MMM") + " – " + dateLabel(monday.plusDays(6).toString(), "d MMM yyyy"),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            OutlinedIconButton({ week = monday.plusWeeks(1).toString(); selected = week }, Modifier.size(48.dp),
                border = BorderStroke(1.dp, colors.outline)) {
                Icon(GymIcons.ChevronRight, stringResource(R.string.next_week), Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints {
            Row(Modifier.wrapContentWidth(Alignment.Start, unbounded = true).requiredWidth(maxWidth + 24.dp).offset(x = (-12).dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                (0L..6L).forEach { offset ->
                    val day = monday.plusDays(offset).toString()
                    val planned = remember(dataset.revision, day) { Training.openOccurrence(dataset, day) }
                    val dayName = if (day == today) stringResource(R.string.today) else dateLabel(day, "EEE")
                    val description = dateLabel(day) + ": " + (planned?.sessionName ?: stringResource(R.string.no_session))
                    Surface(onClick = { selected = day },
                        modifier = Modifier.weight(1f).heightIn(min = 82.dp).semantics { contentDescription = description; this.selected = selected == day },
                        color = if (selected == day) colors.accent else colors.surfaceContainer,
                        contentColor = if (selected == day) colors.onAccent else colors.onSurface,
                        shape = RoundedCornerShape(12.dp), border = BorderStroke(if (day == today) 2.dp else 1.dp,
                            if (day == today || selected == day) colors.focus else colors.outlineVariant)) {
                        Column(Modifier.padding(vertical = 7.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                            Text(dayName, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1)
                            Text(dateLabel(day, "d"), fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
                            Text(planned?.let { calendarBadge(dataset, it) } ?: "—", fontSize = 11.sp, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }
        SectionTitle(stringResource(R.string.pending), stringResource(R.string.not_logged_count, pending.size))
        if (pending.isEmpty()) Help(stringResource(R.string.pending_empty))
        pending.take(3).forEach { AgendaRow(it, { selected = it.plannedDate; week = it.plannedDate }) }
        SectionTitle(stringResource(R.string.coming_up))
        if (upcoming.isEmpty()) Help(stringResource(R.string.upcoming_empty))
        upcoming.forEach { AgendaRow(it, { selected = it.plannedDate; week = it.plannedDate }) }
    }
}

@Composable private fun AgendaRow(occurrence: Occurrence, onClick: () -> Unit) {
    val colors = GymColors
    Column {
        Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceContainer, modifier = Modifier.width(44.dp).heightIn(min = 49.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(dateLabel(occurrence.plannedDate, "d"), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(dateLabel(occurrence.plannedDate, "MMM"), fontSize = 10.sp)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(occurrence.sessionName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Help(dateLabel(occurrence.plannedDate, "EEEE"))
                }
                Icon(GymIcons.ChevronRight, null, Modifier.size(24.dp))
            }
        }
        HorizontalDivider(color = colors.outlineVariant)
    }
}

// GYM-001/005/017/019: session lifecycle actions live here, separate from exercise logging.
@Composable
fun SessionFeature(day: String, dataset: Dataset, vm: GymViewModel, navigate: (String) -> Unit, back: () -> Unit) {
    val today = currentDate(vm)
    val occurrence = remember(dataset.revision, day) { Training.openOccurrence(dataset, day) } ?: return
    val future = day > today
    var finishing by remember { mutableStateOf(false) }
    val done = workingDone(occurrence)
    val total = workingTotal(occurrence)
    fun transition(action: SessionAction, confirm: Boolean = false) {
        vm.change({ state, reading -> Training.transition(state, day, action, reading, confirm) }, onSuccess = { saved ->
            finishing = false
            if (action == SessionAction.START || action == SessionAction.RESUME) {
                val changed = requireNotNull(Training.openOccurrence(saved, day))
                navigate("exercise:$day:${Training.firstUnfinishedEntryId(changed)}")
            }
        })
    }
    ScreenColumn {
        Heading(occurrence.sessionName, back)
        Spacer(Modifier.height(12.dp))
        Help(listOf(if (occurrence.planId != dataset.activePlanId) occurrence.planName else "", occurrence.focus, dateLabel(day),
            if (future) stringResource(R.string.preview_session) else "").filter { it.isNotBlank() }.joinToString(" · "))
        Spacer(Modifier.height(16.dp))
        if (!future && occurrence.lifecycle != Lifecycle.FINISHED) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (occurrence.lifecycle) {
                    Lifecycle.READY, Lifecycle.PAUSED -> {
                        val ready = occurrence.lifecycle == Lifecycle.READY
                        val actionLabel = stringResource(if (ready) R.string.start_session else R.string.resume_session)
                        GymButton(stringResource(if (ready) R.string.start else R.string.resume),
                            { transition(if (ready) SessionAction.START else SessionAction.RESUME) },
                            Modifier.weight(1f).semantics { contentDescription = actionLabel })
                    }
                    Lifecycle.IN_PROGRESS -> GymButton(stringResource(R.string.pause), { transition(SessionAction.PAUSE) }, emphasis = ButtonEmphasis.Outlined)
                    else -> Unit
                }
                if (occurrence.lifecycle != Lifecycle.READY) GymButton(stringResource(R.string.finish), {
                    if (Training.needsFinishConfirmation(occurrence)) finishing = true else transition(SessionAction.FINISH)
                }, Modifier.weight(1f), emphasis = if (occurrence.lifecycle == Lifecycle.PAUSED) ButtonEmphasis.Outlined else ButtonEmphasis.Filled)
            }
        }
        Help(lifecycleLabel(occurrence.lifecycle), Modifier.padding(top = 10.dp))
        occurrence.firstVisit?.takeIf { it.localDate != occurrence.plannedDate }?.let { Help(stringResource(R.string.planned_actual, dateLabel(day), dateLabel(it.localDate)), Modifier.padding(top = 8.dp)) }
        SectionTitle(stringResource(R.string.exercises), stringResource(R.string.working_count, done, total))
        ReorderableList(occurrence.exercises, { it.prescription.id }, !future && occurrence.lifecycle != Lifecycle.FINISHED,
            itemName = { it.definition.name },
            onReorder = { ids -> vm.change({ state, reading -> Training.reorderExercises(state, day, ids, reading) }) }) { entry ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = GymColors.surfaceContainer,
                    modifier = Modifier.width(43.dp).height(49.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("${occurrence.exercises.indexOf(entry) + 1}", fontSize = 14.sp) }
                }
                Box(Modifier.weight(1f)) {
                    LibraryRow(entry.definition.name, stringResource(R.string.prescription, entry.prescription.workingSets,
                        entry.prescription.minReps, entry.prescription.maxReps), onClick = { navigate("exercise:$day:${entry.prescription.id}") })
                }
            }
        }
    }
    if (finishing) ConfirmDialog(stringResource(R.string.finish_title), stringResource(R.string.finish_warning, occurrence.sessionName, done, total),
        stringResource(R.string.finish), { transition(SessionAction.FINISH, true) }, { finishing = false })
}
