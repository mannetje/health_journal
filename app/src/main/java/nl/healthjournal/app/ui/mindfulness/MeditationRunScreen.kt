package nl.healthjournal.app.ui.mindfulness

import android.os.SystemClock
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
import kotlinx.coroutines.delay
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.mindfulness.CueChoices
import nl.healthjournal.domain.model.mindfulness.ExerciseSession
import nl.healthjournal.domain.model.mindfulness.MeditationTimer
import nl.healthjournal.domain.model.mindfulness.SessionEnd
import nl.healthjournal.domain.model.mindfulness.ToneSynth

/**
 * Runs one silent sitting. The remaining time counts down; bells that come due sound once each
 * (tone and vibration, as chosen). The session keeps the screen on and nothing is recorded: Stop, Back, the background or
 * finishing ends it and the state is dropped.
 */
@Composable
fun MeditationRunScreen(
    timer: MeditationTimer,
    cues: CueChoices,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val language = appContentLanguage()
    val view = LocalView.current
    val launcher = rememberSessionLauncher()
    var elapsed by remember(timer) { mutableLongStateOf(0L) }
    var session by remember(timer) { mutableStateOf<ExerciseSession?>(null) }
    var finished by remember(timer) { mutableStateOf(false) }
    var player by remember(timer) { mutableStateOf<CuePlayer?>(null) }

    DisposableEffect(timer) {
        var leaving = false
        val started = launcher.start { reason ->
            if (reason == SessionEnd.FINISHED) finished = true else if (!leaving) onClose()
        }
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
        val start = SystemClock.elapsedRealtime()
        var nextBell = 0
        while (!timer.isFinished(elapsed)) {
            elapsed = SystemClock.elapsedRealtime() - start
            val due = timer.bellsDue(nextBell, elapsed)
            due.forEach { player?.bell(it.kind) }
            nextBell += due.size
            delay(200)
        }
        finished = true
        // Let the end bell ring out before the audio is released.
        if (cues.tones) delay(ToneSynth.END_BELL_MILLIS)
        session?.end(SessionEnd.FINISHED)
    }

    val controls = rememberFadingControls(keepVisible = finished)
    val remainingSeconds = ((timer.remainingMillis(elapsed) + 999) / 1000).toInt()
    Box(modifier = with(controls) { Modifier.fillMaxSize().revealOnTouch() }) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Text(
            if (finished) stringResource(R.string.breathing_done) else clockText(remainingSeconds),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        OutlinedButton(onClick = { if (controls.canPress) stop() }, modifier = Modifier.alpha(controls.alpha)) {
            Text(stringResource(if (finished) R.string.breathing_back else R.string.breathing_stop))
        }
    }
    }
}
