package nl.healthjournal.app.ui.mindfulness

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.mindfulness.ExerciseSession
import nl.healthjournal.domain.model.mindfulness.GuidedExercise
import nl.healthjournal.domain.model.mindfulness.SessionEnd

/**
 * Plays one bundled recording. Each press on Play starts a session that holds the screen-on flag, the audio
 * focus and the player; Pause, Back, the app going to the background or another app taking the audio ends it
 * and gives everything back. Only the position is kept in memory so Play can continue; nothing is stored.
 */
@Composable
fun GuidedPlayerScreen(exercise: GuidedExercise, onClose: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val launcher = rememberSessionLauncher()
    val appLanguage = appContentLanguage()

    var playing by remember { mutableStateOf(false) }
    var playback by remember { mutableStateOf<GuidedPlayback?>(null) }
    var resumeMs by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableIntStateOf(0) }
    val durationMs = playback?.durationMs?.takeIf { it > 0 } ?: (exercise.durationSeconds * 1000)

    var current by remember { mutableStateOf<ExerciseSession?>(null) }

    fun startPlayback() {
        val session = launcher.start { reason ->
            val last = playback?.lastPositionMs ?: resumeMs
            playing = false
            playback = null
            resumeMs = if (reason == SessionEnd.FINISHED) 0 else last
            positionMs = resumeMs
        }
        current = session
        session.keepScreenOn(view)
        if (!session.requestAudioFocus(context)) {
            session.end(SessionEnd.STOPPED)
            return
        }
        val started = session.playRecording(context, exercise, resumeMs)
        if (started == null) {
            session.end(SessionEnd.STOPPED)
        } else {
            playback = started
            playing = true
        }
    }

    fun leave() {
        current?.end(SessionEnd.STOPPED)
        onClose()
    }

    BackHandler(onBack = ::leave)

    LaunchedEffect(playing) {
        while (playing) {
            positionMs = playback?.positionMs ?: positionMs
            delay(250)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        Text(
            exercise.title.forLanguage(appLanguage),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        if (!exercise.audioMatches(appLanguage)) {
            Text(
                stringResource(R.string.guided_audio_language, languageName(exercise.audioLanguage)),
                style = MaterialTheme.typography.labelLarge
            )
        }
        val progressDescription = stringResource(R.string.guided_progress_desc)
        LinearProgressIndicator(
            progress = { (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = progressDescription }
        )
        Text(
            clockText(positionMs / 1000) + " / " + clockText(durationMs / 1000),
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { if (playing) current?.end(SessionEnd.STOPPED) else startPlayback() }) {
                Text(stringResource(if (playing) R.string.guided_pause else R.string.guided_play))
            }
            OutlinedButton(onClick = ::leave) { Text(stringResource(R.string.breathing_back)) }
        }
        val credit = exercise.credit
        Text(
            stringResource(R.string.guided_credit, credit.author, credit.sourceName, credit.licenceName),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

/** The player of one session; remembers where it stopped so Play can continue from there. */
private class GuidedPlayback(private val player: MediaPlayer) {
    var lastPositionMs = 0
        private set

    val durationMs: Int get() = player.duration
    val positionMs: Int get() = player.currentPosition

    fun release() {
        lastPositionMs = runCatching { player.currentPosition }.getOrDefault(lastPositionMs)
        player.release()
    }
}

/** Starts the recording at [startAtMs]; the player is released when the session ends, and finishing ends it. */
private fun ExerciseSession.playRecording(context: Context, exercise: GuidedExercise, startAtMs: Int): GuidedPlayback? {
    val resId = context.resources.getIdentifier(exercise.rawResourceName, "raw", context.packageName)
    if (resId == 0) return null
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val player = MediaPlayer.create(context, resId, speechAttributes, audioManager.generateAudioSessionId()) ?: return null
    val playback = GuidedPlayback(player)
    hold { playback.release() }
    player.setOnCompletionListener { end(SessionEnd.FINISHED) }
    if (startAtMs > 0) player.seekTo(startAtMs)
    player.start()
    return playback
}
