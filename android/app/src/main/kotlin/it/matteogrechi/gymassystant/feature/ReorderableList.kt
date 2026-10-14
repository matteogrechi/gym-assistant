package it.matteogrechi.gymassystant.feature

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.key as composeKey
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import it.matteogrechi.gymassystant.R
import it.matteogrechi.gymassystant.ui.GymColors
import it.matteogrechi.gymassystant.ui.GymIcons
import kotlinx.coroutines.delay
import kotlin.math.abs

val LocalReorderScroll = staticCompositionLocalOf<ScrollState?> { null }

// GYM-015/018/019: one handle-only reorder interaction for prescriptions and execution.
@Composable
fun <T> ReorderableList(items: List<T>, key: (T) -> String, enabled: Boolean,
    itemName: (T) -> String, onReorder: (List<String>) -> Unit, content: @Composable (T) -> Unit) {
    val positions = remember { mutableStateMapOf<String, Rect>() }
    var dragged by remember { mutableStateOf<String?>(null) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    val keys = items.map(key)
    val currentKeys by rememberUpdatedState(keys)
    val commit by rememberUpdatedState(onReorder)
    val target = keys.indices.minByOrNull { abs((positions[keys[it]]?.center?.y ?: Float.MAX_VALUE) - pointerY) }
    val scroll = LocalReorderScroll.current
    val view = LocalView.current
    val colors = GymColors
    val upLabel = stringResource(R.string.move_up)
    val downLabel = stringResource(R.string.move_down)

    fun move(id: String, targetIndex: Int) {
        val from = currentKeys.indexOf(id)
        if (from < 0 || targetIndex !in currentKeys.indices || targetIndex == from) return
        commit(currentKeys.toMutableList().apply { add(targetIndex, removeAt(from)) })
    }

    LaunchedEffect(dragged, enabled) {
        while (dragged != null && enabled && scroll != null) {
            val delta = when {
                pointerY < 140 -> -16f
                pointerY > view.height - 140 -> 16f
                else -> 0f
            }
            if (delta != 0f) scroll.scrollBy(delta)
            delay(40)
        }
    }
    LaunchedEffect(keys, enabled) {
        if (!enabled || dragged !in keys) dragged = null
        positions.keys.filterNot { it in keys }.forEach { positions.remove(it) }
    }
    Column {
        items.forEachIndexed { index, item ->
            composeKey(key(item)) {
                val id = key(item)
                Column(Modifier.onGloballyPositioned { coordinates ->
                    val bounds = Rect(coordinates.positionInWindow(), coordinates.size.toSize())
                    if (positions[id] != bounds) positions[id] = bounds
                }.drawWithContent {
                    drawContent()
                    if (dragged != null && target == index) drawLine(colors.focus, Offset.Zero, Offset(size.width, 0f), 3.dp.toPx())
                }) {
                    Row(Modifier.fillMaxWidth().alpha(if (dragged == id) .55f else 1f)) {
                        Box(Modifier.weight(1f)) { content(item) }
                        if (enabled) {
                            val position = stringResource(R.string.reorder_position, index + 1, items.size)
                            IconButton(onClick = {}, modifier = Modifier.size(48.dp)
                                .semantics {
                                    contentDescription = "${itemName(item)}, $position"
                                    liveRegion = LiveRegionMode.Polite
                                    customActions = buildList {
                                        if (index > 0) add(CustomAccessibilityAction(upLabel) { move(id, index - 1); true })
                                        if (index < items.lastIndex) add(CustomAccessibilityAction(downLabel) { move(id, index + 1); true })
                                    }
                                }
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                                        Key.DirectionUp -> { move(id, index - 1); true }
                                        Key.DirectionDown -> { move(id, index + 1); true }
                                        Key.Escape -> { dragged = null; true }
                                        else -> false
                                    }
                                }
                                .pointerInput(id, enabled) {
                                    var cancelled = false
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            cancelled = false
                                            dragged = id
                                            pointerY = (positions[id]?.top ?: 0f) + offset.y
                                        },
                                        onDrag = { change, delta ->
                                            change.consume()
                                            if (dragged == null) cancelled = true
                                            if (!cancelled) pointerY += delta.y
                                        },
                                        onDragCancel = { dragged = null },
                                        onDragEnd = {
                                            if (!cancelled && dragged == id) {
                                                val index = currentKeys.indices.minByOrNull {
                                                    abs((positions[currentKeys[it]]?.center?.y ?: Float.MAX_VALUE) - pointerY)
                                                }
                                                if (index != null) move(id, index)
                                            }
                                            dragged = null
                                        },
                                    )
                                }) {
                                Icon(GymIcons.DragHandle, contentDescription = null, tint = colors.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
