package nl.healthjournal.app.ui.history.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import kotlin.math.roundToInt

@Composable
fun TrendCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

data class StatChip(val label: String, val value: String, val valueColor: Color? = null)

@Composable
fun StatChipRow(chips: List<StatChip>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { chip ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                Text(
                    text = chip.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = chip.value,
                    style = MaterialTheme.typography.titleMedium,
                    color = chip.valueColor ?: MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class DistributionSegment(val label: String, val fraction: Float, val count: Int, val color: Color)

@Composable
fun CategoryDistributionBar(segments: List<DistributionSegment>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        segments.forEach { segment ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = segment.label, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = stringResource(R.string.trend_distribution_percent, (segment.fraction * 100).roundToInt(), segment.count),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(segment.fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(segment.color)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(selected: TrendDateRange, onSelect: (TrendDateRange) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(TrendDateRange.SEVEN_DAYS, TrendDateRange.THIRTY_DAYS, TrendDateRange.NINETY_DAYS, TrendDateRange.ALL)
    val labels = listOf(
        stringResource(R.string.trend_range_7d),
        stringResource(R.string.trend_range_30d),
        stringResource(R.string.trend_range_90d),
        stringResource(R.string.trend_range_all)
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(labels[index])
            }
        }
    }
}

/**
 * A horizontal gauge showing where [value] sits within [min]..[max], with a marker line.
 * The gradient itself is a fixed decorative backdrop (low->high); [markerColor] conveys the
 * clinically meaningful NHG category color for the current value.
 */
@Composable
fun GaugeBar(
    value: Float,
    min: Float,
    max: Float,
    markerColor: Color,
    modifier: Modifier = Modifier
) {
    val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(markerColor)
        )
    }
}
