package nl.healthjournal.app.ui.logging.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun HorizontalRulerPicker(
    value: Double,
    onValueChange: (Double) -> Unit,
    range: ClosedFloatingPointRange<Double>,
    step: Double = 0.5,
    unitLabel: String = "",
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val totalSteps = remember(range, step) {
        ((range.endInclusive - range.start) / step).roundToInt()
    }

    val itemWidthDp = 12.dp
    val itemWidthPx = with(density) { itemWidthDp.toPx() }

    val initialIndex = remember(value, range, step) {
        ((value - range.start) / step).roundToInt().coerceIn(0, totalSteps)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Sync scroll state back to value
    val centerIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex
        }
    }

    LaunchedEffect(centerIndex) {
        val calculatedValue = (range.start + centerIndex * step).coerceIn(range.start, range.endInclusive)
        val roundedValue = (calculatedValue * 10).roundToInt() / 10.0
        if (roundedValue != value) {
            onValueChange(roundedValue)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        val halfWidth = maxWidth / 2

        // Center Indicator Line
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(3.dp)
                .height(60.dp)
                .background(MaterialTheme.colorScheme.primary)
        )

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(horizontal = halfWidth),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(totalSteps + 1) { index ->
                val currentValue = range.start + index * step
                val isWhole = (currentValue % 1.0).roundToInt() == 0 || Math.abs(currentValue - currentValue.roundToInt()) < 0.01

                Box(
                    modifier = Modifier
                        .width(itemWidthDp)
                        .height(100.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        if (isWhole && index % (1 / step).roundToInt() == 0) {
                            Text(
                                text = currentValue.roundToInt().toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        val strokeColor = MaterialTheme.colorScheme.onSurfaceVariant
                        Canvas(
                            modifier = Modifier
                                .width(itemWidthDp)
                                .height(if (isWhole) 40.dp else 22.dp)
                        ) {
                            drawLine(
                                color = if (isWhole) strokeColor else strokeColor.copy(alpha = 0.5f),
                                start = Offset(size.width / 2, 0f),
                                end = Offset(size.width / 2, size.height),
                                strokeWidth = if (isWhole) 2.dp.toPx() else 1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}
