package nl.healthjournal.app.ui.mindfulness

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import nl.healthjournal.domain.model.mindfulness.CueChoices
import nl.healthjournal.domain.model.mindfulness.ExerciseSession
import nl.healthjournal.domain.model.mindfulness.SessionEnd

/**
 * Runs one breathing exercise. Its session keeps the display on and records nothing:
 * Stop, Back, the background or finishing ends the session and the state is simply dropped.
 */
@Composable
fun BreathingExerciseScreen(
    timer: BreathingTimer,
    visual: BreathingVisual,
    cues: CueChoices,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val language = appContentLanguage()
    val view = LocalView.current
    val launcher = rememberSessionLauncher()
    val reduceMotion = rememberReduceMotion()

    var elapsed by remember(timer) { mutableLongStateOf(0L) }
    val controls = rememberFadingControls(keepVisible = elapsed >= timer.totalMillis)
    var session by remember(timer) { mutableStateOf<ExerciseSession?>(null) }
    var player by remember(timer) { mutableStateOf<CuePlayer?>(null) }
    DisposableEffect(timer) {
        // Leaving for the background or losing the audio drops the exercise; finishing keeps the Done screen.
        var leaving = false
        val started = launcher.start { reason -> if (!leaving && reason != SessionEnd.FINISHED) onClose() }
        started.keepScreenOn(view)
        player = started.cues(context, cues, language)
        session = started
        onDispose {
            leaving = true
            started.end(SessionEnd.STOPPED)
        }
    }
    fun stop() {
        // Ending a running session closes the screen through its callback; after Done there is nothing to end.
        if (session?.end(SessionEnd.STOPPED) != true) onClose()
    }
    BackHandler(onBack = ::stop)
    LaunchedEffect(timer) {
        val start = withFrameMillisCompat()
        while (elapsed < timer.totalMillis) {
            elapsed = withFrameMillisCompat() - start
        }
        session?.end(SessionEnd.FINISHED)
    }
    val state = timer.stateAt(elapsed)
    val phaseKey = (state as? BreathingState.Running)?.let { it.cycleIndex * 1000 + it.phaseIndex } ?: -1
    val phaseWord = (state as? BreathingState.Running)?.let { stringResource(phaseLabel(it.phase.kind)) }
    LaunchedEffect(phaseKey) {
        val running = timer.stateAt(elapsed) as? BreathingState.Running
        if (running != null && phaseWord != null) player?.phase(running.phase.kind, phaseWord)
    }

    Box(modifier = with(controls) { Modifier.fillMaxSize().revealOnTouch() }) {
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
        OutlinedButton(onClick = { if (controls.canPress) stop() }, modifier = Modifier.alpha(controls.alpha)) {
            Text(stringResource(if (state == BreathingState.Done) R.string.breathing_back else R.string.breathing_stop))
        }
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

/** True when the user has turned system animations off; the visual then jumps between full and empty. */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
