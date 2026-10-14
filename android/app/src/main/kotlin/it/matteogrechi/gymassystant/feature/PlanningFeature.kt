package it.matteogrechi.gymassystant.feature

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.matteogrechi.gymassystant.R
import it.matteogrechi.gymassystant.app.GymViewModel
import it.matteogrechi.gymassystant.domain.Dataset
import it.matteogrechi.gymassystant.domain.ExerciseDefinition
import it.matteogrechi.gymassystant.domain.Plan
import it.matteogrechi.gymassystant.domain.Prescription
import it.matteogrechi.gymassystant.domain.SessionTemplate
import it.matteogrechi.gymassystant.domain.Training
import it.matteogrechi.gymassystant.domain.validatePlan
import it.matteogrechi.gymassystant.ui.ButtonEmphasis
import it.matteogrechi.gymassystant.ui.Disclosure
import it.matteogrechi.gymassystant.ui.DropdownField
import it.matteogrechi.gymassystant.ui.Field
import it.matteogrechi.gymassystant.ui.GymButton
import it.matteogrechi.gymassystant.ui.GymColors
import it.matteogrechi.gymassystant.ui.GymIcons
import it.matteogrechi.gymassystant.ui.Heading
import it.matteogrechi.gymassystant.ui.LibraryRow
import it.matteogrechi.gymassystant.ui.PlanningTabs
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

// GYM-018/026/028: only retained prescriptions are edited; execution remains in Train.
@Composable
fun PlanningFeature(route: String, dataset: Dataset, vm: GymViewModel,
    onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val parts = route.split(':')
    when (parts.first()) {
        "plan" -> if (parts.size == 1) PlanningLibrary(dataset, vm, onNavigate)
            else PlanEditor(parts[1], null, dataset, vm, onNavigate, onBack)
        "template" -> if (parts.size == 3) PlanEditor(parts[1], parts[2], dataset, vm, onNavigate, onBack)
        "definition" -> DefinitionDetails(parts.getOrNull(1), dataset, onBack)
    }
}

@Composable
private fun PlanningLibrary(dataset: Dataset, vm: GymViewModel, navigate: (String) -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var newDefinition by rememberSaveable { mutableStateOf<String?>(null) }
    var scrollToNew by rememberSaveable { mutableStateOf(false) }
    val planName = stringResource(R.string.planning_new_plan)
    val sessionName = stringResource(R.string.planning_new_session, 1)
    val newRowFocus = remember { FocusRequester() }
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        Heading(stringResource(R.string.planning_title), actions = {
            GymButton(stringResource(if (tab == 0) R.string.planning_add_plan else R.string.planning_add_exercise),
                onClick = {
                    if (tab == 1) creating = true else {
                        val id = uuid()
                        vm.change({ current, _ ->
                            Training.retainDraft(current, Plan(id, uuid(), planName,
                                listOf(newSession(sessionName, current.exercises.first().id))))
                        }, onSuccess = { vm.setEditing(id, true); navigate("plan:$id") })
                    }
                }, enabled = tab == 1 || dataset.exercises.isNotEmpty(),
                emphasis = ButtonEmphasis.Tonal, icon = GymIcons.Add)
        })
        Spacer(Modifier.height(18.dp))
        PlanningTabs(tab, { tab = it })
        Column(Modifier.weight(1f).verticalScroll(scroll)) {
            if (tab == 0) {
                val plans = dataset.plans + dataset.drafts.filter { draft ->
                    dataset.plans.none { it.id == draft.plan.id }
                }.map { it.plan }
                plans.forEach { saved ->
                    val draft = dataset.drafts.find { it.plan.id == saved.id }?.plan
                    val plan = draft ?: saved
                    val dirty = draft != null && dataset.plans.find { it.id == saved.id } != draft
                    val count = pluralStringResource(R.plurals.planning_session_count, plan.sessions.size, plan.sessions.size)
                    LibraryRow(plan.name,
                        if (dirty) stringResource(R.string.planning_count_unsaved, count) else count,
                        badge = if (dataset.activePlanId == plan.id) stringResource(R.string.planning_active) else null,
                        onClick = { navigate("plan:${plan.id}") })
                }
            } else {
                dataset.exercises.forEach { definition ->
                    key(definition.id) {
                        LibraryRow(definition.name, definition.machineVariant,
                            onClick = { newDefinition = definition.id; navigate("definition:${definition.id}") },
                            modifier = if (newDefinition == definition.id) Modifier.focusRequester(newRowFocus) else Modifier)
                    }
                }
                if (dataset.exercises.isEmpty()) PlanningHelp(stringResource(R.string.planning_no_exercises))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    LaunchedEffect(newDefinition, tab, dataset.exercises.size) {
        if (tab == 1 && newDefinition != null && dataset.exercises.any { it.id == newDefinition }) {
            if (scrollToNew) { scroll.scrollTo(scroll.maxValue); scrollToNew = false }
            newRowFocus.requestFocus()
        }
    }
    if (creating) ExerciseCreator(vm, { creating = false }, { id ->
        newDefinition = id; scrollToNew = true; creating = false
    })
}

@Composable
private fun DefinitionDetails(id: String?, dataset: Dataset, back: () -> Unit) {
    val definition = dataset.exercises.find { it.id == id }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading(definition?.name ?: stringResource(R.string.planning_exercise), back)
        Spacer(Modifier.height(18.dp))
        if (definition != null) {
            Field(stringResource(R.string.planning_variant), definition.machineVariant, {}, readOnly = true)
            Spacer(Modifier.height(18.dp))
            Field(stringResource(R.string.planning_default_setup), definition.defaultSetup, {}, readOnly = true)
        } else PlanningHelp(stringResource(R.string.planning_unavailable))
    }
}

// The coordinator retains this flag per plan in its SavedStateHandle across route/Settings changes.
@Composable
private fun PlanEditor(planId: String, templateId: String?, dataset: Dataset, vm: GymViewModel,
    navigate: (String) -> Unit, back: () -> Unit) {
    val saved = dataset.plans.find { it.id == planId }
    val draft = dataset.drafts.find { it.plan.id == planId }?.plan
    val plan = draft ?: saved
    val editing = vm.isEditing(planId) && draft != null
    val dirty = draft != null && draft != saved
    val valid = plan != null && runCatching { validatePlan(plan, dataset.exercises) }.isSuccess
    val template = plan?.sessions?.find { it.id == templateId }
    var confirmation by rememberSaveable(planId) { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var savedNotice by rememberSaveable(planId) { mutableStateOf(false) }
    val writeError by vm.error.collectAsState()
    val scroll = rememberScrollState()
    val edit: () -> Unit = {
        savedNotice = false
        vm.change({ current, _ -> Training.beginEdit(current, planId) },
            onSuccess = { vm.setEditing(planId, true) })
    }
    val save: () -> Unit = {
        vm.clearError()
        saving = true
        vm.change({ current, clock ->
            Training.commitDraft(current, planId, uuid(), clock.observation.localDate)
        }, onSuccess = {
            vm.setEditing(planId, false); saving = false; savedNotice = true
        })
    }
    // A failed write retains the draft and allows another explicit Save.
    LaunchedEffect(writeError) {
        if (writeError) saving = false
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(GymColors.surface).padding(vertical = 8.dp)) {
            Heading(stringResource(if (templateId == null) {
                if (editing) R.string.planning_edit_plan else R.string.planning_title
            } else {
                if (editing) R.string.planning_edit_session else R.string.planning_session
            }).let { if (templateId != null && !editing) template?.name ?: it else it }, back,
                actions = {
                    if (plan != null && (templateId == null || template != null)) {
                        GymButton(stringResource(if (editing) {
                            if (templateId == null) R.string.planning_save else R.string.planning_save_plan
                        } else R.string.planning_edit),
                            if (editing) save else edit,
                            enabled = !saving && (!editing || dirty && valid),
                            emphasis = if (editing) ButtonEmphasis.Filled else ButtonEmphasis.Outlined)
                    }
                })
            when {
                dirty && !valid -> PlanningHelp(stringResource(R.string.planning_invalid_plan), true)
                dirty -> PlanningHelp(stringResource(R.string.planning_unsaved))
                savedNotice -> PlanningHelp(stringResource(R.string.planning_saved))
            }
        }
        CompositionLocalProvider(LocalReorderScroll provides scroll) {
            Column(Modifier.weight(1f).verticalScroll(scroll)) {
                Spacer(Modifier.height(10.dp))
                if (plan == null || templateId != null && template == null) {
                    PlanningHelp(stringResource(R.string.planning_unavailable))
                } else if (templateId == null) {
                    PlanDetails(plan, editing, dataset, vm, navigate)
                    if (!editing && saved != null) {
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = GymColors.outlineVariant)
                        Spacer(Modifier.height(16.dp))
                        if (dataset.activePlanId == planId) PlanningHelp(stringResource(R.string.planning_active_plan))
                        else {
                            val otherChanges = dataset.drafts.any { it.plan != dataset.plans.find { savedPlan -> savedPlan.id == it.plan.id } }
                            if (otherChanges) PlanningHelp(stringResource(R.string.planning_save_before_activate))
                            GymButton(stringResource(R.string.planning_make_active), { confirmation = "activate" },
                                enabled = !otherChanges, emphasis = ButtonEmphasis.Outlined)
                        }
                    }
                    if (saved == null) {
                        Spacer(Modifier.height(16.dp))
                        GymButton(stringResource(R.string.planning_discard_draft), { confirmation = "discard" },
                            emphasis = ButtonEmphasis.Text)
                    }
                } else if (template != null) {
                    TemplateDetails(planId, template, editing, dataset, vm)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (confirmation == "activate" && saved != null) ConfirmDialog(
        stringResource(R.string.planning_activate_title),
        stringResource(R.string.planning_activate_message, saved.name,
            dataset.plans.find { it.id == dataset.activePlanId }?.name ?: stringResource(R.string.planning_none)),
        stringResource(R.string.planning_make_active),
        onConfirm = {
            vm.change({ current, clock ->
                require(!vm.isEditing(planId))
                require(current.drafts.none { it.plan != current.plans.find { savedPlan -> savedPlan.id == it.plan.id } })
                Training.activatePlan(current, planId, clock.observation.localDate)
            }, onSuccess = { confirmation = null; vm.setEditing(planId, false) })
        }, onDismiss = { confirmation = null })
    if (confirmation == "discard" && plan != null) ConfirmDialog(
        stringResource(R.string.planning_discard_title),
        stringResource(R.string.planning_discard_message, plan.name),
        stringResource(R.string.planning_discard),
        onConfirm = {
            vm.change({ current, _ -> Training.discardNewDraft(current, planId) },
                onSuccess = { confirmation = null; vm.setEditing(planId, false); back() })
        }, onDismiss = { confirmation = null })
}

@Composable
private fun PlanDetails(plan: Plan, editing: Boolean, dataset: Dataset, vm: GymViewModel,
    navigate: (String) -> Unit) {
    DraftTextField(stringResource(R.string.planning_plan_name), plan.name, 80, editing, required = true) {
        value -> vm.editDraft(plan.id) { it.copy(name = value) }
    }
    Spacer(Modifier.height(24.dp))
    val sessionName = stringResource(R.string.planning_new_session, plan.sessions.size + 1)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PlanningSectionTitle(stringResource(R.string.planning_sessions), Modifier.weight(1f))
        if (editing) GymButton(stringResource(R.string.planning_add), {
            vm.editDraft(plan.id) { current ->
                if (current.sessions.size >= 7) current else current.copy(sessions = current.sessions +
                    newSession(sessionName, dataset.exercises.first().id))
            }
        }, enabled = plan.sessions.size < 7 && dataset.exercises.isNotEmpty(),
            emphasis = ButtonEmphasis.Tonal, icon = GymIcons.Add)
    }
    Spacer(Modifier.height(14.dp))
    plan.sessions.forEachIndexed { index, session ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = GymColors.surfaceContainer) {
                androidx.compose.foundation.layout.Box(Modifier.size(44.dp, 48.dp), contentAlignment = Alignment.Center) {
                    Text(('A'.code + index).toChar().toString(), style = MaterialTheme.typography.bodyLarge)
                }
            }
            LibraryRow(session.name,
                pluralStringResource(R.plurals.planning_exercise_count, session.exercises.size, session.exercises.size),
                onClick = { navigate("template:${plan.id}:${session.id}") }, modifier = Modifier.weight(1f))
            if (editing) IconButton(onClick = {
                vm.change({ current, _ ->
                    val draft = current.drafts.single { it.plan.id == plan.id }.plan
                    Training.retainDraft(current, Training.removeSession(draft, session.id))
                })
            }, modifier = Modifier.size(48.dp)) {
                Icon(GymIcons.Delete, stringResource(R.string.planning_remove_session, session.name),
                    tint = GymColors.primary)
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    Disclosure(stringResource(R.string.planning_weekly)) {
        val labels = listOf(R.string.planning_monday, R.string.planning_tuesday, R.string.planning_wednesday,
            R.string.planning_thursday, R.string.planning_friday, R.string.planning_saturday, R.string.planning_sunday)
        val options = listOf(stringResource(R.string.planning_rest)) + plan.sessions.map { it.name }
        labels.forEachIndexed { index, label ->
            val assigned = plan.weeklySchedule[index + 1]
            val dayLabel = stringResource(label)
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(dayLabel, Modifier.weight(.9f), style = MaterialTheme.typography.bodySmall)
                DropdownField("", options, plan.sessions.indexOfFirst { it.id == assigned } + 1,
                    onSelect = { selected ->
                        vm.editDraft(plan.id) { current -> current.copy(weeklySchedule =
                            if (selected == 0) current.weeklySchedule - (index + 1)
                            else current.weeklySchedule + ((index + 1) to plan.sessions[selected - 1].id)) }
                    }, modifier = Modifier.weight(2.4f).semantics { contentDescription = dayLabel }, readOnly = !editing)
            }
        }
    }
    DateAssignments(plan, editing, vm)
}

@Composable
private fun DateAssignments(plan: Plan, editing: Boolean, vm: GymViewModel) {
    val today = vm.now().observation.localDate
    var selectedDate by rememberSaveable(plan.id) { mutableStateOf(today) }
    val locale = appLocale()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM yyyy", locale) }
    val context = LocalContext.current
    Disclosure(stringResource(if (editing) R.string.planning_change_date else R.string.planning_date_changes)) {
        plan.dateOverrides.toSortedMap().forEach { (day, sessionId) ->
            Field(LocalDate.parse(day).format(formatter),
                plan.sessions.find { it.id == sessionId }?.name ?: stringResource(R.string.planning_rest), {}, readOnly = true)
        }
        if (!editing && plan.dateOverrides.isEmpty()) PlanningHelp(stringResource(R.string.planning_no_date_changes))
        if (editing) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.planning_date), style = MaterialTheme.typography.labelSmall)
            GymButton(LocalDate.parse(selectedDate).format(formatter), {
                val initial = LocalDate.parse(selectedDate).coerceAtLeast(LocalDate.parse(vm.now().observation.localDate))
                val dialog = DatePickerDialog(context, { _, year, month, day ->
                    selectedDate = LocalDate.of(year, month + 1, day).toString()
                }, initial.year, initial.monthValue - 1, initial.dayOfMonth)
                dialog.datePicker.minDate = LocalDate.parse(vm.now().observation.localDate)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                dialog.show()
            }, modifier = Modifier.fillMaxWidth(), emphasis = ButtonEmphasis.Outlined)
            Spacer(Modifier.height(18.dp))
            val options = listOf(stringResource(R.string.planning_weekly_pattern), stringResource(R.string.planning_rest)) +
                plan.sessions.map { it.name }
            val selected = if (!plan.dateOverrides.containsKey(selectedDate)) 0
                else if (plan.dateOverrides[selectedDate] == null) 1
                else plan.sessions.indexOfFirst { it.id == plan.dateOverrides[selectedDate] } + 2
            DropdownField(stringResource(R.string.planning_session), options, selected,
                onSelect = { index ->
                    vm.change({ current, clock ->
                        require(selectedDate >= clock.observation.localDate)
                        val draft = current.drafts.single { it.plan.id == plan.id }.plan
                        Training.retainDraft(current, draft.copy(dateOverrides = when (index) {
                            0 -> draft.dateOverrides - selectedDate
                            1 -> draft.dateOverrides + (selectedDate to null)
                            else -> draft.dateOverrides + (selectedDate to plan.sessions[index - 2].id)
                        }))
                    })
                }, enabled = selectedDate >= today)
            PlanningHelp(stringResource(R.string.planning_date_help))
        }
    }
}

@Composable
private fun TemplateDetails(planId: String, template: SessionTemplate, editing: Boolean,
    dataset: Dataset, vm: GymViewModel) {
    var selectedExercise by rememberSaveable(template.id) { mutableStateOf(dataset.exercises.firstOrNull()?.id) }
    var creating by rememberSaveable(template.id) { mutableStateOf(false) }
    fun change(update: (SessionTemplate) -> SessionTemplate) {
        vm.editDraft(planId) { plan -> plan.copy(sessions = plan.sessions.map {
            if (it.id == template.id) update(it) else it
        }) }
    }
    DraftTextField(stringResource(R.string.planning_session_name), template.name, 80, editing, required = true) {
        value -> change { it.copy(name = value) }
    }
    Spacer(Modifier.height(18.dp))
    Disclosure(stringResource(R.string.planning_optional_focus)) {
        DraftTextField(stringResource(R.string.planning_focus), template.focus, 160, editing) {
            value -> change { it.copy(focus = value) }
        }
    }
    Spacer(Modifier.height(24.dp))
    PlanningSectionTitle(stringResource(R.string.planning_exercises))
    Spacer(Modifier.height(14.dp))
    ReorderableList(template.exercises, { it.id }, editing,
        itemName = { entry -> dataset.exercises.find { it.id == entry.exerciseId }?.name.orEmpty() },
        onReorder = { order -> change { session ->
            session.copy(exercises = order.map { id -> session.exercises.single { it.id == id } })
        } }) { prescription ->
        PrescriptionCard(prescription, template.exercises.indexOfFirst { it.id == prescription.id }, editing,
            dataset.exercises, onChange = { update -> change { session ->
                session.copy(exercises = session.exercises.map { if (it.id == prescription.id) update(it) else it })
            } }, onRemove = { change { session ->
                session.copy(exercises = session.exercises.filterNot { it.id == prescription.id })
            } })
    }
    if (template.exercises.isEmpty()) PlanningHelp(stringResource(R.string.planning_required_exercise), editing)
    if (editing) {
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownField(stringResource(R.string.planning_exercise_to_add),
                dataset.exercises.map { "${it.name} · ${it.machineVariant}" },
                dataset.exercises.indexOfFirst { it.id == selectedExercise },
                { selectedExercise = dataset.exercises[it].id }, modifier = Modifier.weight(1f))
            GymButton(stringResource(R.string.planning_add), {
                selectedExercise?.let { exerciseId -> change {
                    it.copy(exercises = it.exercises + Prescription(uuid(), exerciseId))
                } }
            }, enabled = dataset.exercises.any { it.id == selectedExercise },
                emphasis = ButtonEmphasis.Tonal, icon = GymIcons.Add)
        }
        Spacer(Modifier.height(8.dp))
        GymButton(stringResource(R.string.planning_create_exercise), { creating = true }, emphasis = ButtonEmphasis.Text)
    }
    if (creating) ExerciseCreator(vm, { creating = false }, { id ->
        selectedExercise = id; creating = false
    })
}

@Composable
private fun PrescriptionCard(entry: Prescription, index: Int, editing: Boolean,
    definitions: List<ExerciseDefinition>, onChange: ((Prescription) -> Prescription) -> Unit,
    onRemove: () -> Unit) {
    val definition = definitions.find { it.id == entry.exerciseId }
    val summary = stringResource(R.string.planning_prescription_summary, index + 1,
        definition?.name.orEmpty(), entry.workingSets, entry.minReps, entry.maxReps, entry.recoverySeconds)
    val invalid = entry.workingSets !in 1..10 || entry.minReps !in 1..50 ||
        entry.maxReps !in entry.minReps..50 || entry.recoverySeconds !in 1..3600
    val minRepsFocus = remember { FocusRequester() }
    Surface(Modifier.fillMaxWidth().padding(bottom = 18.dp), shape = RoundedCornerShape(12.dp),
        color = GymColors.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, GymColors.outlineVariant)) {
        Column(Modifier.padding(12.dp)) {
            key(editing) {
                Disclosure(summary, initiallyExpanded = editing && (index == 0 || invalid)) {
                    DropdownField(stringResource(R.string.planning_exercise),
                        definitions.map { "${it.name} · ${it.machineVariant}" },
                        definitions.indexOfFirst { it.id == entry.exerciseId },
                        { selected -> onChange { it.copy(exerciseId = definitions[selected].id) } }, readOnly = !editing)
                    Spacer(Modifier.height(15.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrescriptionNumber(stringResource(R.string.planning_working_sets), entry.workingSets,
                            1..10, editing, Modifier.weight(1f)) { value -> onChange { it.copy(workingSets = value) } }
                        PrescriptionNumber(stringResource(R.string.planning_min_reps), entry.minReps,
                            1..50, editing, Modifier.weight(1f).focusRequester(minRepsFocus), entry.minReps > entry.maxReps) { value ->
                            onChange { it.copy(minReps = value) }
                        }
                        PrescriptionNumber(stringResource(R.string.planning_max_reps), entry.maxReps,
                            1..50, editing, Modifier.weight(1f), entry.minReps > entry.maxReps) { value ->
                            onChange { it.copy(maxReps = value) }
                        }
                    }
                    if (editing && entry.minReps > entry.maxReps) PlanningHelp(stringResource(R.string.planning_rep_range), true)
                    LaunchedEffect(entry.minReps > entry.maxReps) {
                        if (editing && entry.minReps > entry.maxReps) minRepsFocus.requestFocus()
                    }
                    Spacer(Modifier.height(14.dp))
                    PrescriptionNumber(stringResource(R.string.planning_recovery), entry.recoverySeconds,
                        1..3600, editing) { value -> onChange { it.copy(recoverySeconds = value) } }
                    if (editing) {
                        Spacer(Modifier.height(12.dp))
                        GymButton(stringResource(R.string.planning_remove_exercise), onRemove, emphasis = ButtonEmphasis.Text)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrescriptionNumber(label: String, value: Int, range: IntRange, editing: Boolean,
    modifier: Modifier = Modifier, crossError: Boolean = false, onChange: (Int) -> Unit) {
    var input by rememberSaveable { mutableStateOf(if (value == 0) "" else value.toString()) }
    val invalid = editing && (value !in range || crossError)
    Field(label, if (editing) input else value.toString(), { text ->
        if (text.length <= 4 && text.all { it in '0'..'9' }) {
            input = text; onChange(text.toIntOrNull() ?: 0)
        }
    }, modifier, readOnly = !editing, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = invalid, supportingText = if (editing && value !in range)
            stringResource(R.string.planning_number_bounds, range.first, range.last) else null)
}

@Composable
private fun DraftTextField(label: String, value: String, limit: Int, editing: Boolean,
    required: Boolean = false, onChange: (String) -> Unit) {
    // Room writes are asynchronous: keep typing responsive while each draft change is retained.
    var input by rememberSaveable { mutableStateOf(value) }
    LaunchedEffect(value, editing) { if (!editing) input = value }
    val invalid = editing && required && input.isBlank()
    Field(label, if (editing) input else value, { text ->
        if (text.length <= limit) { input = text; onChange(text) }
    }, readOnly = !editing, isError = invalid,
        supportingText = if (invalid) stringResource(R.string.planning_required) else null)
}

// GYM-028: one contextual creator, with catalog Save separate from explicit session Add.
@Composable
private fun ExerciseCreator(vm: GymViewModel, dismiss: () -> Unit, created: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var variant by rememberSaveable { mutableStateOf("") }
    var setup by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val writeError by vm.error.collectAsState()
    val id = rememberSaveable { uuid() }
    val cancelFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = { if (!saving) dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(16.dp).heightIn(max = (LocalConfiguration.current.screenHeightDp - 48).coerceAtLeast(240).dp),
            shape = RoundedCornerShape(28.dp), color = GymColors.surface) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.planning_create_exercise), style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(12.dp))
                Field(stringResource(R.string.planning_exercise_name), name,
                    { if (it.length <= 80) name = it }, isError = attempted && name.isBlank(),
                    supportingText = if (attempted && name.isBlank()) stringResource(R.string.planning_required) else null)
                Spacer(Modifier.height(18.dp))
                Field(stringResource(R.string.planning_variant), variant,
                    { if (it.length <= 80) variant = it }, isError = attempted && variant.isBlank(),
                    supportingText = if (attempted && variant.isBlank()) stringResource(R.string.planning_required) else null)
                Spacer(Modifier.height(18.dp))
                Field(stringResource(R.string.planning_optional_setup), setup, { if (it.length <= 160) setup = it })
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    GymButton(stringResource(R.string.planning_cancel), dismiss,
                        Modifier.focusRequester(cancelFocus), emphasis = ButtonEmphasis.Text, enabled = !saving)
                    GymButton(stringResource(R.string.planning_save_exercise), {
                        attempted = true
                        if (name.isNotBlank() && variant.isNotBlank()) {
                            vm.clearError()
                            saving = true
                            vm.change({ dataset, _ -> Training.createExercise(dataset,
                                ExerciseDefinition(id, name.trim(), variant.trim(), setup.trim())) },
                                onSuccess = { saving = false; created(id) })
                        }
                    }, enabled = !saving)
                }
            }
        }
        LaunchedEffect(Unit) { cancelFocus.requestFocus() }
        LaunchedEffect(writeError) { if (writeError) saving = false }
    }
}

@Composable
private fun PlanningHelp(text: String, error: Boolean = false) {
    Text(text, Modifier.padding(vertical = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodySmall,
        color = if (error) MaterialTheme.colorScheme.error else GymColors.onSurfaceVariant)
}

@Composable
private fun PlanningSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium,
        color = GymColors.onSurface)
}

private fun uuid() = UUID.randomUUID().toString()
private fun newSession(name: String, exerciseId: String) =
    SessionTemplate(uuid(), name, exercises = listOf(Prescription(uuid(), exerciseId)))
