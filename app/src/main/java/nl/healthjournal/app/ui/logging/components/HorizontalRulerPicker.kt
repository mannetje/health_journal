package nl.healthjournal.app.ui.logging.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HorizontalRulerPicker(
    value: Double,
    onValueChange: (Double) -> Unit,
    range: ClosedFloatingPointRange<Double>,
    step: Double = 0.1,
    unitLabel: String = "",
    tapeColor: Color = Color(0xFFFFC107), // Yellow Measuring Tape
    tickColor: Color = Color(0xFF212121),  // Dark charcoal ticks & numbers
    indicatorColor: Color = Color(0xFFD32F2F), // Red pointer
    tapeHeight: Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    val totalSteps = remember(range, step) {
        ((range.endInclusive - range.start) / step).roundToInt()
    }

    val itemWidthDp = 12.dp
    val initialIndex = remember(value, range, step) {
        ((value - range.start) / step).roundToInt().coerceIn(0, totalSteps)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    LaunchedEffect(centerIndex) {
        val calculatedValue = (range.start + centerIndex * step).coerceIn(range.start, range.endInclusive)
        val roundedValue = (calculatedValue * 10).roundToInt() / 10.0
        if (abs(roundedValue - value) >= 0.05) {
            onValueChange(roundedValue)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Large Prominent Readout Display
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = String.format(java.util.Locale.US, "%.1f", value),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (unitLabel.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unitLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        // Measuring Tape Ruler Container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(tapeHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(tapeColor),
            contentAlignment = Alignment.TopCenter
        ) {
            val halfWidth = maxWidth / 2

            // Top Center Indicator Arrow
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(16.dp)
                    .height(12.dp)
            ) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, size.height)
                    close()
                }
                drawPath(path, color = indicatorColor)
            }

            // Center Indicator Line through Tape
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(indicatorColor)
            )

            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(horizontal = halfWidth),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxSize()
            ) {
                items(totalSteps + 1) { index ->
                    val currentValue = range.start + index * step
                    val roundedCurrent = (currentValue * 10).roundToInt()
                    val isWhole = roundedCurrent % 10 == 0
                    val isHalf = roundedCurrent % 5 == 0 && !isWhole

                    val tickHeight = when {
                        isWhole -> 36.dp
                        isHalf -> 24.dp
                        else -> 16.dp
                    }

                    Box(
                        modifier = Modifier
                            .width(itemWidthDp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            // Tick Line
                            Canvas(
                                modifier = Modifier
                                    .width(itemWidthDp)
                                    .height(tickHeight)
                            ) {
                                drawLine(
                                    color = tickColor,
                                    start = Offset(size.width / 2, 0f),
                                    end = Offset(size.width / 2, size.height),
                                    strokeWidth = if (isWhole) 2.5.dp.toPx() else 1.2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }

                            // Unclipped Horizontal Whole Number Label
                            if (isWhole) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier.requiredWidth(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (currentValue.roundToInt()).toString(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tickColor,
                                        textAlign = TextAlign.Center,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
