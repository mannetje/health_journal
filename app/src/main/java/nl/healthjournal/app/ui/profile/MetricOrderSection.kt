package nl.healthjournal.app.ui.profile

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.MetricSlot
import nl.healthjournal.app.ui.logging.nameRes
import kotlin.math.roundToInt

/**
 * The order of the metrics on the Log tabs and History chips. A row moves with the Move up and Move down
 * buttons, or by dragging its handle after a long press; every finished move is passed on at once.
 */
@Composable
fun MetricOrderSection(
    order: List<MetricSlot>,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastMove by remember { mutableStateOf<Pair<MetricSlot, Int>?>(null) }
    var draggedIndex by remember { mutableIntStateOf(NO_ROW) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(1f) }

    fun move(from: Int, to: Int) {
        if (from == to) return
        onMove(from, to)
        lastMove = order[from] to to
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.profile_metric_order_label), style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(R.string.profile_metric_order_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        order.forEachIndexed { index, slot ->
            val name = stringResource(slot.nameRes)
            val dragging = draggedIndex == index
            Surface(
                tonalElevation = if (dragging) 6.dp else 0.dp,
                color = if (dragging) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                    .onSizeChanged { rowHeightPx = it.height.toFloat() }
            ) {
                Row(
                    modifier = Modifier.heightIn(min = 48.dp).padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    IconButton(enabled = index > 0, onClick = { move(index, index - 1) }) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.profile_metric_order_move_up, name))
                    }
                    IconButton(enabled = index < order.lastIndex, onClick = { move(index, index + 1) }) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.profile_metric_order_move_down, name))
                    }
                    val dragDescription = stringResource(R.string.profile_metric_order_drag, name)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = dragDescription }
                            .pointerInput(index, order.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggedIndex = index; dragOffset = 0f },
                                    onDrag = { change, amount -> change.consume(); dragOffset += amount.y },
                                    onDragEnd = {
                                        val target = (index + (dragOffset / rowHeightPx).roundToInt()).coerceIn(0, order.lastIndex)
                                        draggedIndex = NO_ROW
                                        dragOffset = 0f
                                        move(index, target)
                                    },
                                    onDragCancel = { draggedIndex = NO_ROW; dragOffset = 0f }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = null)
                    }
                }
            }
        }
        lastMove?.let { (slot, position) ->
            Text(
                stringResource(R.string.profile_metric_order_position, stringResource(slot.nameRes), position + 1, order.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

private const val NO_ROW = -1
