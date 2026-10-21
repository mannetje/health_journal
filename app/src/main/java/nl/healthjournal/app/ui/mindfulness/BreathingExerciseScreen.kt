package nl.healthjournal.app.ui.mindfulness

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.mindfulness.BreathingPattern
import nl.healthjournal.domain.model.mindfulness.BreathingPhaseKind
import nl.healthjournal.domain.model.mindfulness.BreathingState
import nl.healthjournal.domain.model.mindfulness.BreathingTimer

/**
 * Runs one breathing exercise. The screen keeps the display on while it is shown and records nothing:
 * when the exercise ends or the user leaves, the state is simply dropped.
 */
@Composable
fun BreathingExerciseScreen(
    timer: BreathingTimer,
    visual: BreathingVisual,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    KeepScreenOn()
    val reduceMotion = rememberReduceMotion()

    var elapsed by remember(timer) { mutableLongStateOf(0L) }
    LaunchedEffect(timer) {
        val start = withFrameMillisCompat()
        while (elapsed < timer.totalMillis) {
            elapsed = withFrameMillisCompat() - start
        }
    }
    val state = timer.stateAt(elapsed)

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        val fill = when (state) {
            is BreathingState.Running -> if (reduceMotion) steppedFill(state.phase.kind) else state.fill
            else -> 0f
        }
        BreathingCanvas(
            visual = visual,
            fill = fill,
            squareProgress = squareProgress(timer.pattern, state),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        val label = when (state) {
            is BreathingState.Ready -> stringResource(R.string.breathing_ready)
            is BreathingState.Running -> stringResource(phaseLabel(state.phase.kind))
            BreathingState.Done -> stringResource(R.string.breathing_done)
        }
        Text(
            label,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        val count = when (state) {
            is BreathingState.Ready -> state.secondsLeft.toString()
            is BreathingState.Running -> state.secondsLeft.toString()
            BreathingState.Done -> ""
        }
        Text(count, style = MaterialTheme.typography.displayMedium)
        OutlinedButton(onClick = onClose) {
            Text(stringResource(if (state == BreathingState.Done) R.string.breathing_back else R.string.breathing_stop))
        }
    }
}

private suspend fun withFrameMillisCompat(): Long =
    androidx.compose.runtime.withFrameMillis { it }

private fun steppedFill(kind: BreathingPhaseKind): Float = when (kind) {
    BreathingPhaseKind.INHALE, BreathingPhaseKind.INHALE_AGAIN, BreathingPhaseKind.HOLD -> 1f
    BreathingPhaseKind.EXHALE, BreathingPhaseKind.HOLD_EMPTY -> 0f
}

/** The dot travels one side of the square per phase of box breathing; other patterns just use their phase share. */
private fun squareProgress(pattern: BreathingPattern, state: BreathingState): Float {
    if (state !is BreathingState.Running) return 0f
    val sides = pattern.phases.size.coerceAtLeast(1)
    return (state.phaseIndex + state.phaseProgress) / sides
}

internal fun phaseLabel(kind: BreathingPhaseKind): Int = when (kind) {
    BreathingPhaseKind.INHALE -> R.string.breathing_phase_inhale
    BreathingPhaseKind.INHALE_AGAIN -> R.string.breathing_phase_inhale_again
    BreathingPhaseKind.HOLD -> R.string.breathing_phase_hold
    BreathingPhaseKind.EXHALE -> R.string.breathing_phase_exhale
    BreathingPhaseKind.HOLD_EMPTY -> R.string.breathing_phase_pause
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** True when the user has turned system animations off; the visual then jumps between full and empty. */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
