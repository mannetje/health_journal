package nl.healthjournal.app.ui.logging.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StackedBpPulsePicker(
    systolic: Int,
    onSystolicChange: (Int) -> Unit,
    diastolic: Int,
    onDiastolicChange: (Int) -> Unit,
    pulse: Int,
    onPulseChange: (Int) -> Unit,
    systolicLabel: String,
    diastolicLabel: String,
    pulseLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Row: Systolic (Red Box)
        SingleNumberRowPicker(
            label = systolicLabel,
            value = systolic,
            onValueChange = onSystolicChange,
            range = 40..260,
            highlightColor = Color(0xFFE53935) // Red
        )

        // Middle Row: Diastolic (Blue Box)
        SingleNumberRowPicker(
            label = diastolicLabel,
            value = diastolic,
            onValueChange = onDiastolicChange,
            range = 20..200,
            highlightColor = Color(0xFF1E88E5) // Blue
        )

        // Bottom Row: Pulse (Green Box)
        SingleNumberRowPicker(
            label = pulseLabel,
            value = pulse,
            onValueChange = onPulseChange,
            range = 30..220,
            highlightColor = Color(0xFF43A047) // Green
        )
    }
}

@Composable
private fun SingleNumberRowPicker(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val totalCount = remember(range) { range.last - range.first + 1 }
    val initialIndex = remember(value, range) { (value - range.first).coerceIn(0, totalCount - 1) }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val selectedIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    LaunchedEffect(selectedIndex) {
        val newValue = (range.first + selectedIndex).coerceIn(range.first, range.last)
        if (newValue != value) {
            onValueChange(newValue)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            val halfWidth = maxWidth / 2
            val boxWidth = 56.dp

            // Center Highlight Bounding Box
            Box(
                modifier = Modifier
                    .width(boxWidth)
                    .height(48.dp)
                    .background(highlightColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(2.dp, highlightColor, RoundedCornerShape(8.dp))
            )

            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(horizontal = halfWidth - (boxWidth / 2)),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(totalCount) { index ->
                    val currentValue = range.first + index
                    val isSelected = index == selectedIndex

                    Box(
                        modifier = Modifier
                            .width(boxWidth)
                            .height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentValue.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = if (isSelected) 20.sp else 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}
