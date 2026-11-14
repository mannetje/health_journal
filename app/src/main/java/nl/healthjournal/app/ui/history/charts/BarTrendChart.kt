package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/** One bar: a [label] under the chart and a [value] (null: no bar, for a period without data). */
data class Bar(val label: String, val value: Double?)

private val SlotWidth = 44.dp
private val LabelHeight = 18.dp
private val PlotHeight = 110.dp

/**
 * Bars above a zero line for a gain and below it for a loss, the value printed at the end of each bar.
 * Gain and loss share one neutral colour. The chart scrolls sideways when the bars do not fit, so labels
 * never overlap at large font sizes.
 */
@Composable
fun BarTrendChart(
    bars: List<Bar>,
    valueText: (Double) -> String,
    modifier: Modifier = Modifier,
    /** Values drawn as dashed lines across the bars, for the person's own numbers; they share the bars' scale. */
    referenceLines: List<Double> = emptyList()
) {
    val values = bars.mapNotNull { it.value }
    val highestLine = referenceLines.maxOrNull() ?: 0.0
    val scale = max(max(values.maxOfOrNull { abs(it) } ?: 0.0, highestLine), 1e-9)
    val above = max(values.filter { it > 0 }.maxOrNull() ?: 0.0, highestLine)
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val below = values.filter { it < 0 }.minOfOrNull { abs(it) } ?: 0.0
    val aboveArea = PlotHeight * (above / scale).toFloat() + LabelHeight
    val belowArea = PlotHeight * (below / scale).toFloat() + LabelHeight
    val color = MaterialTheme.colorScheme.secondary
    // A dashed line may run through a value label; the card's own colour behind the label keeps the number readable.
    val labelBackground = if (referenceLines.isEmpty()) androidx.compose.ui.graphics.Color.Transparent else CardDefaults.cardColors().containerColor

    Row(modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        bars.forEach { bar ->
            val value = bar.value
            val description = if (value == null) bar.label else "${bar.label}, ${valueText(value)}"
            Column(
                modifier = Modifier.width(SlotWidth).clearAndSetSemantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.heightIn(min = aboveArea).width(SlotWidth).drawBehind {
                        referenceLines.forEach { v ->
                            val y = size.height - PlotHeight.toPx() * (v / scale).toFloat()
                            drawLine(
                                lineColor, Offset(0f, y), Offset(size.width, y),
                                strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                            )
                        }
                    },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (value != null && value > 0) {
                        BarWithValue(valueText(value), PlotHeight * (value / scale).toFloat(), color, valueAbove = true, labelBackground = labelBackground)
                    }
                }
                HorizontalDivider()
                Box(modifier = Modifier.heightIn(min = belowArea).width(SlotWidth), contentAlignment = Alignment.TopCenter) {
                    if (value != null && value < 0) {
                        BarWithValue(valueText(value), PlotHeight * (abs(value) / scale).toFloat(), color, valueAbove = false, labelBackground = labelBackground)
                    }
                }
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BarWithValue(text: String, barHeight: Dp, color: androidx.compose.ui.graphics.Color, valueAbove: Boolean, labelBackground: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val valueLabel = @Composable {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.background(labelBackground)
            )
        }
        val bar = @Composable { Box(modifier = Modifier.width(24.dp).height(barHeight).background(color)) }
        if (valueAbove) { valueLabel(); bar() } else { bar(); valueLabel() }
    }
}
