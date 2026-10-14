package it.matteogrechi.gymassystant.feature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteogrechi.gymassystant.R
import it.matteogrechi.gymassystant.ui.GymColors
import it.matteogrechi.gymassystant.ui.GymIcons
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal data class ProgressPlotObservation(val id: String, val date: String, val value: BigDecimal?)
internal data class ProgressPlotPoint(val observation: ProgressPlotObservation, val x: Float, val y: Float?)
internal data class ProgressPlotLayout(val points: List<ProgressPlotPoint>, val width: Float,
    val maximum: BigDecimal, val ticks: List<BigDecimal>, val segments: List<Pair<Int, Int>>)

// GYM-010: only geometry uses floating point. Values and tick calculations remain exact decimals.
internal fun progressPlotLayout(observations: List<ProgressPlotObservation>, viewport: Float,
    zoom: Float, integer: Boolean = false): ProgressPlotLayout {
    require(viewport > 0 && zoom in 1f..4f)
    require(observations.map { it.id }.distinct().size == observations.size)
    require(observations.all { it.value == null || it.value.signum() >= 0 })
    val ordered = observations.sortedWith(compareBy<ProgressPlotObservation> { it.date }.thenBy { it.id })
    val largest = ordered.mapNotNull { it.value }.maxOrNull()?.takeIf { it.signum() > 0 } ?: BigDecimal.ONE
    val rough = largest.divide(BigDecimal(4), MathContext.DECIMAL128)
    val exponent = rough.precision() - rough.scale() - 1
    val magnitude = BigDecimal.ONE.scaleByPowerOfTen(exponent)
    var step = listOf(1, 2, 5, 10).map { magnitude * BigDecimal(it) }.first { it >= rough }
    if (integer) step = maxOf(BigDecimal.ONE, step)
    val maximum = largest.divide(step, 0, RoundingMode.CEILING) * step
    val tickCount = maximum.divide(step).intValueExact()
    val ticks = (tickCount downTo 0).map { step * BigDecimal(it) }
    val groups = ordered.groupBy { LocalDate.parse(it.date).toEpochDay() }.entries.toList()
    val firstDay = groups.firstOrNull()?.key ?: 0L
    val span = (groups.lastOrNull()?.key ?: firstDay) - firstDay
    val spacing = 56f
    val firstRadius = ((groups.firstOrNull()?.value?.size ?: 1) - 1) * spacing / 2
    val lastRadius = ((groups.lastOrNull()?.value?.size ?: 1) - 1) * spacing / 2
    val edge = 24f
    val minimumDayWidth = groups.zipWithNext().maxOfOrNull { (left, right) ->
        (left.value.size + right.value.size) * spacing / 2 / (right.key - left.key)
    } ?: 0f
    val timeWidth = max(minimumDayWidth * span, viewport - edge * 2 - firstRadius - lastRadius)
        .coerceAtLeast(0f) * zoom
    val width = if (span == 0L) max(viewport, edge * 2 + firstRadius * 2 * zoom)
        else edge * 2 + (firstRadius + lastRadius) * zoom + timeWidth
    val points = groups.flatMap { (day, entries) ->
        val center = if (span == 0L) width / 2 else edge + firstRadius * zoom + (day - firstDay).toFloat() / span * timeWidth
        entries.mapIndexed { index, entry ->
            ProgressPlotPoint(entry, center + (index - (entries.size - 1) / 2f) * spacing * zoom,
                entry.value?.divide(maximum, MathContext.DECIMAL128)?.toFloat()?.let { 1f - it })
        }
    }
    val segments = points.indices.drop(1).filter { points[it - 1].y != null && points[it].y != null }.map { it - 1 to it }
    return ProgressPlotLayout(points, width, maximum, ticks, segments)
}

internal fun progressNearest(points: List<ProgressPlotPoint>, x: Float): Int =
    points.indices.minByOrNull { abs(points[it].x - x) } ?: -1

internal fun progressReveal(x: Float, pan: Float, viewport: Float, width: Float): Float {
    val next = when {
        x - 24 < pan -> x - 24
        x + 24 > pan + viewport -> x + 24 - viewport
        else -> pan
    }
    return next.coerceIn(0f, max(0f, width - viewport))
}

// The plot scrubs observations; only the separate 48dp date lane pans the timeline.
private val zoomSteps = listOf(1f, 1.5f, 2f, 3f, 4f)

@Composable
internal fun ProgressPlot(observations: List<ProgressPlotObservation>, selectedId: String, unit: String,
    stateKey: String, onSelect: (String) -> Unit, descriptions: List<String>) {
    if (observations.isEmpty()) return
    val colors = GymColors
    val density = LocalDensity.current.density
    var zoom by rememberSaveable(stateKey) { mutableFloatStateOf(1f) }
    var pan by rememberSaveable(stateKey) { mutableFloatStateOf(0f) }
    var revealed by rememberSaveable(stateKey) { mutableStateOf(false) }
    val latestSelect by rememberUpdatedState(onSelect)
    val observationDescriptions = observations.mapIndexed { index, it -> it.id to descriptions[index] }.toMap()
    val inspectLabel = stringResource(R.string.progress_inspect)
    val timelineLabel = stringResource(R.string.progress_timeline)
    val missingLabel = stringResource(R.string.unavailable)
    val zoomOutLabel = stringResource(R.string.zoom_out)
    val resetLabel = stringResource(R.string.reset_view)
    val integer = unit == stringResource(R.string.progress_unit_reps)
    val scrubberFocus = remember(stateKey) { FocusRequester() }
    var scrubberFocused by remember(stateKey) { mutableStateOf(false) }
    val zoomLabel = stringResource(R.string.zoom_status, progressNumber(BigDecimal(zoom.toString())))
    Column {
        Row(Modifier.fillMaxWidth()) {
            // Fixed value axis stays readable when the date lane moves.
            Column(Modifier.width(44.dp)) {
                Box(Modifier.height(180.dp)) {
                    val axis = progressPlotLayout(observations, 100f, zoom, integer)
                    axis.ticks.forEach { tick ->
                        val fraction = 1 - tick.divide(axis.maximum, MathContext.DECIMAL128).toFloat()
                        Text(progressNumber(tick), Modifier.fillMaxWidth().offset(y = (12 + fraction * 156 - 7).dp)
                            .padding(end = 8.dp), textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
                Box(Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(unit, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                        color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            BoxWithConstraints(Modifier.weight(1f)) {
                val viewport = maxWidth.value.coerceAtLeast(48f)
                val layout = remember(observations, viewport, zoom, integer) {
                    progressPlotLayout(observations, viewport, zoom, integer)
                }
                val maximumPan = max(0f, layout.width - viewport)
                val boundedPan = pan.coerceIn(0f, maximumPan)
                val selectedIndex = layout.points.indexOfFirst { it.observation.id == selectedId }.coerceAtLeast(0)
                fun select(index: Int, reveal: Boolean = false) {
                    val point = layout.points.getOrNull(index) ?: return
                    latestSelect(point.observation.id)
                    if (reveal) pan = progressReveal(point.x, boundedPan, viewport, layout.width)
                }
                LaunchedEffect(stateKey, layout.width, viewport) {
                    pan = boundedPan
                    if (!revealed) {
                        pan = progressReveal(layout.points[selectedIndex].x, pan, viewport, layout.width)
                        revealed = true
                    }
                }
                val keyboard = Modifier.onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val next = when (event.key) {
                        Key.DirectionLeft -> (selectedIndex - 1).coerceAtLeast(0)
                        Key.DirectionRight -> (selectedIndex + 1).coerceAtMost(layout.points.lastIndex)
                        Key.MoveHome -> 0
                        Key.MoveEnd -> layout.points.lastIndex
                        Key.Enter, Key.Spacebar -> if (scrubberFocused) selectedIndex else return@onPreviewKeyEvent false
                        else -> return@onPreviewKeyEvent false
                    }
                    select(next, true)
                    scrubberFocus.requestFocus()
                    true
                }
                Column {
                    Box(Modifier.fillMaxWidth().height(180.dp).clipToBounds().then(keyboard)
                        .border(1.dp, if (scrubberFocused) colors.focus else Color.Transparent)
                        .semantics {
                            contentDescription = inspectLabel
                            stateDescription = observationDescriptions[selectedId].orEmpty()
                            progressBarRangeInfo = ProgressBarRangeInfo(selectedIndex.toFloat(),
                                0f..layout.points.lastIndex.toFloat(), (layout.points.size - 2).coerceAtLeast(0))
                            setProgress { value -> select(value.roundToInt().coerceIn(layout.points.indices), true); true }
                        }.focusRequester(scrubberFocus).onFocusChanged { scrubberFocused = it.isFocused }.focusable()
                        .pointerInput(layout, boundedPan) {
                            detectTapGestures { position ->
                                select(progressNearest(layout.points, position.x / density + boundedPan))
                            }
                        }
                        .pointerInput(layout, boundedPan) {
                            detectHorizontalDragGestures(onDragStart = { position ->
                                select(progressNearest(layout.points, position.x / density + boundedPan))
                            }, onHorizontalDrag = { change, _ ->
                                change.consume()
                                select(progressNearest(layout.points, change.position.x / density + boundedPan))
                            })
                        }) {
                        Canvas(Modifier.fillMaxSize()) {
                            fun point(index: Int): Offset {
                                val p = layout.points[index]
                                return Offset((p.x - boundedPan) * density, (12 + (p.y ?: 1f) * 156) * density)
                            }
                            layout.ticks.forEach { tick ->
                                val y = (12 + (1 - tick.divide(layout.maximum, MathContext.DECIMAL128).toFloat()) * 156) * density
                                drawLine(if (tick.signum() == 0) colors.outline else colors.outlineVariant,
                                    Offset(0f, y), Offset(size.width, y), density)
                            }
                            layout.segments.forEach { (from, to) -> drawLine(colors.chart, point(from), point(to), 3 * density) }
                            val guideX = point(selectedIndex).x
                            drawLine(colors.onSurfaceVariant, Offset(guideX, 12 * density), Offset(guideX, 168 * density),
                                density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * density, 3 * density)))
                            layout.points.forEachIndexed { index, p ->
                                if (p.y != null) {
                                    drawCircle(if (index == selectedIndex) colors.onSurface else colors.chart, 5 * density, point(index))
                                    if (index == selectedIndex) drawCircle(colors.onSurface, 10 * density, point(index), style = Stroke(2 * density))
                                }
                            }
                        }
                        layout.points.forEachIndexed { index, point ->
                            val x = point.x - boundedPan
                            if (x >= 24 && x <= viewport - 24) {
                                val top = (12 + (point.y ?: 1f) * 156 - 24).coerceIn(0f, 132f)
                                Box(Modifier.offset { IntOffset(((x - 24) * density).roundToInt(), (top * density).roundToInt()) }
                                    .size(48.dp).onFocusChanged { if (it.isFocused) select(index) }
                                    .clickable(role = Role.Button) { select(index) }
                                    .semantics {
                                        contentDescription = observationDescriptions[point.observation.id].orEmpty()
                                        selected = index == selectedIndex
                                        stateDescription = if (point.y == null) missingLabel else observationDescriptions[point.observation.id].orEmpty()
                                    }, contentAlignment = Alignment.Center) {
                                    if (point.y == null) Text("—", color = colors.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(48.dp).clipToBounds()
                        .semantics {
                            contentDescription = timelineLabel
                            horizontalScrollAxisRange = ScrollAxisRange({ boundedPan }, { maximumPan })
                            scrollBy { x, _ -> pan = (pan + x / density).coerceIn(0f, maximumPan); true }
                        }.draggable(rememberDraggableState { delta ->
                            pan = (pan - delta / density).coerceIn(0f, maximumPan)
                        }, Orientation.Horizontal)) {
                        layout.points.forEach { point ->
                            val sameDay = layout.points.filter { it.observation.date == point.observation.date }
                            val ordinal = if (sameDay.size > 1) " · ${sameDay.indexOf(point) + 1}" else ""
                            val x = point.x - boundedPan
                            if (x >= 0 && x <= viewport) Text(dateLabel(point.observation.date, "d MMM") + ordinal,
                                Modifier.offset { IntOffset(((x - 28) * density).roundToInt(), 14.dp.roundToPx()) }.width(56.dp),
                                style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                                textAlign = TextAlign.Center, color = colors.onSurfaceVariant)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        fun changeZoom(next: Float, reset: Boolean = false) {
                            val nextLayout = progressPlotLayout(observations, viewport, next, integer)
                            val oldCenter = boundedPan + viewport / 2
                            pan = if (reset) progressReveal(nextLayout.points[selectedIndex].x, 0f, viewport, nextLayout.width)
                                else (oldCenter * nextLayout.width / layout.width - viewport / 2)
                                    .coerceIn(0f, max(0f, nextLayout.width - viewport))
                            zoom = next
                        }
                        OutlinedIconButton({ changeZoom(zoomSteps.lastOrNull { it < zoom } ?: 1f) }, Modifier.size(48.dp),
                            enabled = zoom > 1f && layout.points.size > 1,
                            border = BorderStroke(1.dp, colors.outline)) {
                            Text("−", Modifier.semantics { contentDescription = zoomOutLabel }, fontSize = 24.sp)
                        }
                        Help(zoomLabel, Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }
                            .padding(horizontal = 4.dp))
                        OutlinedIconButton({ changeZoom(zoomSteps.firstOrNull { it > zoom } ?: 4f) }, Modifier.size(48.dp),
                            enabled = zoom < 4f && layout.points.size > 1,
                            border = BorderStroke(1.dp, colors.outline)) {
                            Icon(GymIcons.Add, stringResource(R.string.zoom_in), Modifier.size(24.dp))
                        }
                        OutlinedIconButton({ changeZoom(1f, true) }, Modifier.size(48.dp),
                            border = BorderStroke(1.dp, colors.outline)) {
                            Text("↺", Modifier.semantics { contentDescription = resetLabel }, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
        Help(stringResource(R.string.chart_help))
        if (observations.groupBy { it.date }.any { it.value.size > 1 }) Help(stringResource(R.string.progress_same_day))
    }
}
