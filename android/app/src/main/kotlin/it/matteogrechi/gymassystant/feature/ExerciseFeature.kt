package it.matteogrechi.gymassystant.feature

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import it.matteogrechi.gymassystant.R
import it.matteogrechi.gymassystant.app.GymViewModel
import it.matteogrechi.gymassystant.data.LocalDocuments
import it.matteogrechi.gymassystant.domain.*
import it.matteogrechi.gymassystant.domain.Lifecycle as SessionLifecycle
import it.matteogrechi.gymassystant.ui.*
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.UUID

// GYM-002/003/004/005/009/012/014/017: timers, actual sets and setup; session actions stay on Session.
@Composable
fun ExerciseFeature(day: String, entryId: String, dataset: Dataset, vm: GymViewModel, back: () -> Unit) {
    val clock = currentClock(vm)
    val occurrence = remember(dataset.revision, day) { Training.openOccurrence(dataset, day) } ?: return
    val entry = occurrence.exercises.find { it.prescription.id == entryId } ?: return
    val editable = occurrence.lifecycle != SessionLifecycle.FINISHED && day <= clock.observation.localDate
    var modeIndex by rememberSaveable { mutableIntStateOf(0) }
    val mode = TimerMode.entries[modeIndex]
    val timer = if (mode == TimerMode.EXECUTION) entry.executionTimer else entry.recoveryTimer
    val milliseconds = Timers.readingMs(timer, mode, entry.prescription.recoverySeconds, clock)
    val settled = Timers.settle(timer, clock, if (mode == TimerMode.RECOVERY) entry.prescription.recoverySeconds * 1000L else null)
    var video by rememberSaveable { mutableStateOf(false) }
    val colors = GymColors
    val enlargedText = (LocalDensity.current.fontScale - 1f).coerceIn(0f, 1f)
    val typeWidth = 56.dp + 44.dp * enlargedText
    val doneWidth = 48.dp + 8.dp * enlargedText
    LaunchedEffect(timer.running && mode == TimerMode.RECOVERY && milliseconds == 0L) {
        if (timer.running && mode == TimerMode.RECOVERY && milliseconds == 0L) vm.checkpoint()
    }
    ScreenColumn {
        Heading(entry.definition.name, back)
        Spacer(Modifier.height(12.dp))
        val context = listOf(if (day != clock.observation.localDate) dateLabel(day) else "",
            if (occurrence.planId != dataset.activePlanId) occurrence.planName else "",
            if (day > clock.observation.localDate) stringResource(R.string.preview_session) else "").filter(String::isNotBlank)
        if (context.isNotEmpty()) Help(context.joinToString(" · "))
        if (occurrence.lifecycle == SessionLifecycle.PAUSED || occurrence.lifecycle == SessionLifecycle.FINISHED)
            Help(lifecycleLabel(occurrence.lifecycle), Modifier.padding(bottom = 8.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceContainer) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Selector(listOf(stringResource(R.string.execution), stringResource(R.string.recovery)), modeIndex, { modeIndex = it })
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    val seconds = if (mode == TimerMode.RECOVERY) (milliseconds + 999) / 1000 else milliseconds / 1000
                    Text("%02d:%02d".format(seconds / 60, seconds % 60), Modifier.weight(1f).widthIn(min = 112.dp * LocalDensity.current.fontScale),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSans, fontSize = 32.sp, lineHeight = 32.sp,
                            letterSpacing = 0.sp, fontWeight = FontWeight.Bold), color = colors.onSurface)
                    if (editable) Row(verticalAlignment = Alignment.CenterVertically) {
                        GymButton(stringResource(if (settled.running) R.string.pause else if (settled.elapsedMs > 0) R.string.resume else R.string.start),
                            { vm.change({ state, reading -> Training.changeTimer(state, day, entryId, mode,
                                if (settled.running) TimerAction.PAUSE else TimerAction.START, reading) }) },
                            enabled = occurrence.lifecycle != SessionLifecycle.PAUSED, horizontalPadding = 12.dp)
                        GymButton(stringResource(R.string.reset), { vm.change({ state, reading -> Training.changeTimer(state, day, entryId, mode, TimerAction.RESET, reading) }) },
                            emphasis = ButtonEmphasis.Text)
                    }
                }
                if (occurrence.lifecycle == SessionLifecycle.PAUSED) Help(stringResource(R.string.session_paused_hint))
                if (settled.interrupted) Help(stringResource(R.string.timer_interrupted))
                if (mode == TimerMode.RECOVERY && milliseconds == 0L) Help(stringResource(R.string.recovery_complete), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        Spacer(Modifier.height(16.dp))
        Help(stringResource(R.string.prescription, entry.prescription.workingSets, entry.prescription.minReps, entry.prescription.maxReps))
        FlowRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sets), Modifier.weight(1f).widthIn(min = 64.dp * LocalDensity.current.fontScale), style = MaterialTheme.typography.titleSmall)
            Help(stringResource(R.string.working_short, entry.sets.count { it.kind == SetKind.WORKING && it.done }, entry.prescription.workingSets))
            if (editable) GymButton(stringResource(R.string.warm_up), { vm.change({ state, reading -> Training.addWarmUp(state, day, entryId, UUID.randomUUID().toString(), reading) }) },
                emphasis = ButtonEmphasis.Text, icon = GymIcons.Add)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(stringResource(R.string.set_header), stringResource(R.string.reps), "KG", stringResource(R.string.done)).forEachIndexed { index, label ->
                val modifier = when (index) { 0 -> Modifier.width(typeWidth); 1 -> Modifier.weight(1.2f); 2 -> Modifier.weight(.85f); else -> Modifier.width(doneWidth) }
                Text(label, modifier.padding(bottom = 9.dp), fontSize = 10.sp, lineHeight = 13.sp, textAlign = TextAlign.Center,
                    color = colors.onSurfaceVariant, fontWeight = if (index == 1) FontWeight.Bold else FontWeight.Normal)
            }
        }
        entry.sets.forEachIndexed { index, set ->
            key(set.id) {
                SetRow(set, index, editable, typeWidth, doneWidth,
                    update = { transform -> vm.change({ state, reading ->
                        val latest = requireNotNull(Training.openOccurrence(state, day)).exercises.single { it.prescription.id == entryId }.sets.single { it.id == set.id }
                        val changed = transform(latest)
                        Training.updateSet(state, day, entryId, set.id, changed.reps, changed.kg, changed.done, reading)
                    }) },
                    remove = { vm.change({ state, reading -> Training.removeWarmUp(state, day, entryId, set.id, reading) }) })
            }
        }
        Spacer(Modifier.height(22.dp))
        Field(stringResource(R.string.seat_position), entry.setup,
            { value -> if (value.length <= 160) vm.change({ state, reading -> Training.setSetup(state, day, entryId, value, reading) }) },
            readOnly = !editable, placeholder = stringResource(R.string.seat_hint), containerColor = colors.surfaceContainer)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GymButton(stringResource(R.string.exercise_video), { video = true }, emphasis = ButtonEmphasis.Text, icon = GymIcons.Play)
        }
    }
    if (video) ExerciseVideo(entry.definition.id, dataset, vm, { video = false })
}

@Composable private fun SetRow(set: SetLog, index: Int, editable: Boolean, typeWidth: Dp, doneWidth: Dp,
    update: ((SetLog) -> SetLog) -> Unit, remove: () -> Unit) {
    val colors = GymColors
    val warm = set.kind == SetKind.WARM_UP
    val repLabel = stringResource(R.string.set_reps, index + 1)
    val weightLabel = stringResource(R.string.set_weight, index + 1)
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.width(typeWidth), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(if (warm) R.string.warm_up else R.string.working), fontSize = 11.sp, lineHeight = 14.sp,
                fontWeight = FontWeight.Bold, color = if (warm) colors.onWarmup else colors.onSurfaceVariant)
            if (warm && editable) IconButton(remove, Modifier.size(48.dp)) {
                Icon(GymIcons.Delete, stringResource(R.string.remove_warm_up, index + 1), Modifier.size(20.dp))
            } else Text("${index + 1}", fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold)
        }
        NumericInput(set.reps?.toString().orEmpty(), repLabel, Modifier.weight(1.2f), editable, true, warm, set.done) { value ->
            if (value.isEmpty() || value.toIntOrNull()?.let { it >= 0 } == true) {
                update { it.copy(reps = value.toIntOrNull()) }; true
            } else false
        }
        NumericInput(set.kg?.toPlainString().orEmpty(), weightLabel, Modifier.weight(.85f), editable, false, warm, set.done) { value ->
            val number = value.toBigDecimalOrNull()
            if (value.isEmpty() || number?.let { it.signum() >= 0 && it.precision() <= 16 && it.scale() in 0..8 } == true) {
                update { it.copy(kg = number) }; true
            } else false
        }
        if (editable) OutlinedIconButton({ update { it.copy(done = !it.done) } }, Modifier.width(doneWidth).height(48.dp),
            enabled = set.done || (set.reps != null && set.kg != null),
            colors = IconButtonDefaults.outlinedIconButtonColors(
                containerColor = if (set.done) colors.success else androidx.compose.ui.graphics.Color.Transparent,
                contentColor = if (set.done) colors.onSuccess else colors.primary),
            border = BorderStroke(1.dp, if (set.done) colors.success else colors.outline)) {
            Icon(GymIcons.Check, stringResource(if (set.done) R.string.set_undo else R.string.set_done, index + 1))
        } else Box(Modifier.width(doneWidth).height(48.dp), contentAlignment = Alignment.Center) {
            if (set.done) Icon(GymIcons.Check, stringResource(R.string.done), tint = colors.success) else Text("—")
        }
    }
}

@Composable private fun NumericInput(value: String, label: String, modifier: Modifier, editable: Boolean,
    reps: Boolean, warm: Boolean, done: Boolean, accept: (String) -> Boolean) {
    val locale = appLocale()
    val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    fun localize(text: String) = if (reps) text else text.replace('.', separator)
    var text by rememberSaveable { mutableStateOf(localize(value)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value, focused) { if (!focused) text = localize(value) }
    val colors = GymColors
    val style = MaterialTheme.typography.bodyLarge.copy(fontSize = if (reps) 25.sp else 17.sp,
        fontWeight = if (reps) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center,
        color = colors.onSurface)
    if (!editable) {
        Text(if (value.isEmpty()) "—" else localize(value), modifier.heightIn(min = 56.dp).padding(vertical = 14.dp), style = style)
    } else BasicTextField(text, { proposed ->
        val normalized = proposed.replace(separator, '.').replace(',', '.')
        if (proposed.length <= 18 && (normalized.isEmpty() || normalized.matches(Regex(if (reps) "[0-9]+" else "[0-9]+(\\.[0-9]*)?"))) && accept(normalized)) text = proposed
    }, modifier = modifier.heightIn(min = 56.dp).onFocusChanged { focused = it.isFocused }.semantics { contentDescription = label },
        keyboardOptions = KeyboardOptions(keyboardType = if (reps) KeyboardType.Number else KeyboardType.Decimal),
        singleLine = true, textStyle = style, cursorBrush = SolidColor(colors.focus), decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().background(if (warm && reps) colors.warmupContainer else if (reps) colors.repsContainer else colors.surfaceBright, RoundedCornerShape(8.dp))
                .border(1.dp, if (done) colors.success else colors.outline, RoundedCornerShape(8.dp)).padding(vertical = 12.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center) { if (text.isEmpty()) Text("—", style = style.copy(color = colors.onSurfaceVariant)); inner() }
        })
}

// GYM-002/027: local files only, app-owned import and no autoplay/background playback.
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable private fun ExerciseVideo(exerciseId: String, dataset: Dataset, vm: GymViewModel, dismiss: () -> Unit) {
    val context = LocalContext.current
    val definition = dataset.exercises.single { it.id == exerciseId }
    val media = dataset.media.find { it.id == definition.mediaId }
    val file = media?.let { runCatching { LocalDocuments.mediaFile(context, it) }.getOrNull() }
    val busy by vm.busy.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { vm.importVideo(it, exerciseId) }
    }
    var failed by remember { mutableStateOf(false) }
    val player = remember(file) {
        file?.let {
            ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory { FileDataSource() }).build().apply {
                setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(it)))
                playWhenReady = false
                addListener(object : Player.Listener { override fun onPlayerError(error: androidx.media3.common.PlaybackException) { failed = true } })
                prepare()
            }
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) player?.pause() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); player?.release() }
    }
    Dialog(dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(16.dp).widthIn(max = 380.dp).fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Heading(definition.name, actions = { IconButton(dismiss) { Icon(GymIcons.Close, stringResource(R.string.close)) } },
                    titleStyle = MaterialTheme.typography.headlineMedium)
                if (player != null && !failed) AndroidView(factory = { PlayerView(it).apply {
                    this.player = player; setShowPreviousButton(false); setShowNextButton(false)
                } },
                    update = { it.player = player }, modifier = Modifier.fillMaxWidth().height(200.dp))
                else Help(stringResource(if (failed) R.string.video_error else R.string.video_missing))
                if (busy) Help(stringResource(R.string.video_loading))
                if (media == null || file == null || failed) GymButton(stringResource(R.string.import_video), {
                    picker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE); type = "video/*"; putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                    })
                }, enabled = !busy)
            }
        }
    }
}
