package nl.healthjournal.app.ui.fluid

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import nl.healthjournal.app.settings.FluidGaugeStyle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.healthjournal.app.R
import nl.healthjournal.app.ui.common.asString
import nl.healthjournal.domain.metrics.model.FluidDrink
import nl.healthjournal.domain.metrics.model.FluidPosition

val FluidDrink.nameRes: Int
    get() = when (this) {
        FluidDrink.WATER -> R.string.fluid_drink_water
        FluidDrink.COFFEE -> R.string.fluid_drink_coffee
        FluidDrink.TEA -> R.string.fluid_drink_tea
        FluidDrink.MILK -> R.string.fluid_drink_milk
        FluidDrink.JUICE -> R.string.fluid_drink_juice
        FluidDrink.SOUP -> R.string.fluid_drink_soup
        FluidDrink.OTHER -> R.string.fluid_drink_other
    }

/**
 * The Fluid tab: large drink tiles, a glass that fills, today's drinks and the person's own limits. It shows plain
 * numbers and the neutral position label; it has no goal, percentage, streak or encouragement (ADR 0030).
 */
@Composable
fun FluidLogScreen(viewModel: FluidLogViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    // Today's total belongs to the local calendar day, so ask for a recount just after each midnight.
    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.ZonedDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            kotlinx.coroutines.delay(java.time.Duration.between(now, nextMidnight).toMillis() + 500)
            viewModel.refreshToday()
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        state.message?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(it.asString(), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 600.dp
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                    Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Summary(state) }
                    Column(Modifier.weight(1f)) { DrinkTiles(state, viewModel, onTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Summary(state)
                    DrinkTiles(state, viewModel, onTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) })
                }
            }
        }

        state.lastAdded?.let { last ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.fluid_added, stringResource(last.drink.nameRes), last.amount.value),
                        Modifier.weight(1f), color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    TextButton(onClick = viewModel::undo) { Text(stringResource(R.string.fluid_undo)) }
                }
            }
            LaunchedEffect(last.id) {
                kotlinx.coroutines.delay(UNDO_MS)
                viewModel.dismissUndo()
            }
        }

        LimitsCard(state, viewModel)
        GaugeCard(state, viewModel)
        ReferenceNote()
    }
}

@Composable
private fun Summary(state: FluidUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        FluidGauge(
            style = state.gaugeStyle,
            totalMl = state.todayMl,
            minMl = state.limits.minMl,
            maxMl = state.limits.maxMl,
            glassMl = state.portions.of(FluidDrink.WATER),
            modifier = Modifier.width(if (state.gaugeStyle == FluidGaugeStyle.GLASS) 96.dp else 80.dp).height(150.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.fluid_today, state.todayMl), style = MaterialTheme.typography.titleLarge)
            PositionLabel(state)
            TodayStrip(state)
        }
    }
}

@Composable
private fun PositionLabel(state: FluidUiState) {
    val limits = state.limits
    val text = when (state.position) {
        FluidPosition.NONE -> null
        FluidPosition.BELOW_MIN -> stringResource(R.string.fluid_position_below, limits.minMl ?: 0)
        FluidPosition.WITHIN -> stringResource(R.string.fluid_position_within, limits.minMl ?: 0, limits.maxMl ?: 0)
        FluidPosition.ABOVE_MAX -> stringResource(R.string.fluid_position_above, limits.maxMl ?: 0)
    } ?: return
    // One neutral container for every position: never green, yellow, orange or red.
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TodayStrip(state: FluidUiState) {
    if (state.todayEntries.isEmpty()) {
        Text(stringResource(R.string.fluid_today_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val shown = state.todayEntries.take(STRIP_MAX)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        shown.forEach { DrinkIcon(it.drink, Modifier.size(24.dp)) }
        val more = state.todayEntries.size - shown.size
        if (more > 0) Text(stringResource(R.string.fluid_today_more, more), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DrinkTiles(state: FluidUiState, viewModel: FluidLogViewModel, onTap: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = (maxWidth / 150.dp).toInt().coerceIn(2, 4)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FluidDrink.entries.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { drink ->
                        DrinkTile(
                            drink = drink,
                            portionMl = state.portions.of(drink),
                            count = state.countOf(drink),
                            onAdd = { onTap(); viewModel.add(drink) },
                            onStep = { viewModel.stepCount(drink, it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - row.size) { Column(Modifier.weight(1f)) {} }
                }
            }
        }
    }
}

@Composable
private fun DrinkTile(
    drink: FluidDrink,
    portionMl: Int,
    count: Int,
    onAdd: () -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val name = stringResource(drink.nameRes)
    val total = portionMl * count
    val addDescription = stringResource(R.string.fluid_add_description, name, total)
    Card(
        modifier = modifier.height(TILE_HEIGHT),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            Modifier.fillMaxWidth().clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onAdd).semantics { contentDescription = addDescription }.padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DrinkIcon(drink, Modifier.size(40.dp))
            Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(stringResource(R.string.fluid_ml, total), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val lessDescription = stringResource(R.string.fluid_less_description, name)
            val moreDescription = stringResource(R.string.fluid_more_description, name)
            IconButton(onClick = { onStep(-1) }, enabled = count > 1, modifier = Modifier.semantics { contentDescription = lessDescription }) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            Text("×$count", style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = { onStep(1) }, enabled = count < 10, modifier = Modifier.semantics { contentDescription = moreDescription }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private val TILE_HEIGHT = 150.dp

@Composable
private fun LimitsCard(state: FluidUiState, viewModel: FluidLogViewModel) {
    var min by remember(state.limits) { mutableStateOf(state.limits.minMl?.toString().orEmpty()) }
    var max by remember(state.limits) { mutableStateOf(state.limits.maxMl?.toString().orEmpty()) }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.fluid_limits_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.fluid_limits_help), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = min, onValueChange = { min = it.filter(Char::isDigit).take(5) },
                    label = { Text(stringResource(R.string.fluid_min_label)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = max, onValueChange = { max = it.filter(Char::isDigit).take(5) },
                    label = { Text(stringResource(R.string.fluid_max_label)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                )
            }
            Button(onClick = { viewModel.saveLimits(min.toIntOrNull(), max.toIntOrNull()) }) {
                Text(stringResource(R.string.fluid_limits_save))
            }
        }
    }
}

@Composable
private fun GaugeCard(state: FluidUiState, viewModel: FluidLogViewModel) {
    val options = listOf(
        FluidGaugeStyle.GLASS to R.string.fluid_gauge_glass,
        FluidGaugeStyle.WOMAN to R.string.fluid_gauge_woman,
        FluidGaugeStyle.MAN to R.string.fluid_gauge_man
    )
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.fluid_gauge_title), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (style, label) ->
                    SegmentedButton(
                        selected = state.gaugeStyle == style,
                        onClick = { viewModel.setGaugeStyle(style) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    ) { Text(stringResource(label)) }
                }
            }
        }
    }
}

@Composable
private fun ReferenceNote() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.fluid_reference_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.fluid_reference_body), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private const val STRIP_MAX = 8
private const val UNDO_MS = 6000L

/** Own-drawn drink icons: simple shapes in the theme colours, no third-party assets and no per-drink status colour. */
@Composable
fun DrinkIcon(drink: FluidDrink, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.onSecondaryContainer
    val fill = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.07f)
        when (drink) {
            FluidDrink.COFFEE, FluidDrink.TEA -> {
                drawRoundRect(line, Offset(w * 0.12f, h * 0.35f), Size(w * 0.56f, h * 0.45f), androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = stroke)
                drawArc(line, -90f, 180f, false, Offset(w * 0.58f, h * 0.42f), Size(w * 0.28f, h * 0.25f), style = stroke)
                if (drink == FluidDrink.TEA) drawLine(line, Offset(w * 0.4f, h * 0.35f), Offset(w * 0.55f, h * 0.15f), strokeWidth = w * 0.05f)
                else drawLine(line, Offset(w * 0.3f, h * 0.28f), Offset(w * 0.3f, h * 0.12f), strokeWidth = w * 0.05f)
            }
            FluidDrink.MILK -> {
                drawRoundRect(line, Offset(w * 0.25f, h * 0.2f), Size(w * 0.5f, h * 0.65f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f), style = stroke)
                drawLine(line, Offset(w * 0.25f, h * 0.2f), Offset(w * 0.5f, h * 0.05f), strokeWidth = w * 0.06f)
                drawLine(line, Offset(w * 0.75f, h * 0.2f), Offset(w * 0.5f, h * 0.05f), strokeWidth = w * 0.06f)
            }
            FluidDrink.SOUP -> {
                drawArc(line, 0f, 180f, false, Offset(w * 0.1f, h * 0.2f), Size(w * 0.8f, h * 0.7f), style = stroke)
                drawLine(line, Offset(w * 0.1f, h * 0.55f), Offset(w * 0.9f, h * 0.55f), strokeWidth = w * 0.07f)
                drawLine(line, Offset(w * 0.4f, h * 0.45f), Offset(w * 0.4f, h * 0.2f), strokeWidth = w * 0.05f)
                drawLine(line, Offset(w * 0.6f, h * 0.45f), Offset(w * 0.6f, h * 0.2f), strokeWidth = w * 0.05f)
            }
            FluidDrink.JUICE -> {
                drawPath(glassPath(size), line, style = stroke)
                drawCircle(fill, w * 0.16f, Offset(w * 0.5f, h * 0.55f))
            }
            FluidDrink.WATER -> {
                drawPath(glassPath(size), line, style = stroke)
                clipPath(glassPath(size)) { drawRect(fill.copy(alpha = 0.55f), Offset(0f, h * 0.45f), Size(w, h * 0.55f)) }
            }
            FluidDrink.OTHER -> drawPath(
                Path().apply {
                    moveTo(w * 0.5f, h * 0.1f)
                    cubicTo(w * 0.95f, h * 0.55f, w * 0.8f, h * 0.9f, w * 0.5f, h * 0.9f)
                    cubicTo(w * 0.2f, h * 0.9f, w * 0.05f, h * 0.55f, w * 0.5f, h * 0.1f)
                    close()
                },
                fill.copy(alpha = 0.7f)
            )
        }
    }
}
