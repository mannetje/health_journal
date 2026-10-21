package nl.healthjournal.app.ui.mindfulness

import android.content.Context
import android.content.ContextWrapper
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.mindfulness.GuidedExercise

/**
 * Plays one bundled recording. Nothing is stored about the listening. The recording pauses when the
 * app goes to the background or another app takes the audio, and everything is released on leaving.
 */
@Composable
fun GuidedPlayerScreen(exercise: GuidedExercise, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val view = LocalView.current
    val appLanguage = appContentLanguage()

    var playing by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var positionMs by remember { mutableIntStateOf(0) }
    val session = remember(exercise) { GuidedAudioSession(context, exercise) { playing = false; finished = true } }
    val durationMs = session.durationMs.takeIf { it > 0 } ?: (exercise.durationSeconds * 1000)

    DisposableEffect(session) {
        val lifecycle = context.findActivity()?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                session.pause()
                playing = false
            }
        }
        lifecycle?.addObserver(observer)
        session.onFocusLost = { playing = false }
        onDispose {
            lifecycle?.removeObserver(observer)
            session.release()
        }
    }
    DisposableEffect(playing) {
        view.keepScreenOn = playing
        onDispose { view.keepScreenOn = false }
    }
    LaunchedEffect(playing) {
        while (playing) {
            positionMs = session.positionMs
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
            Button(onClick = {
                if (playing) {
                    session.pause()
                    playing = false
                } else {
                    if (finished) {
                        positionMs = 0
                        finished = false
                    }
                    playing = session.play()
                }
            }) {
                Text(stringResource(if (playing) R.string.guided_pause else R.string.guided_play))
            }
            OutlinedButton(onClick = onClose) { Text(stringResource(R.string.breathing_back)) }
        }
        val credit = exercise.credit
        Text(
            stringResource(R.string.guided_credit, credit.author, credit.sourceName, credit.licenceName),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** One recording, its player and its audio focus; all of it is dropped by [release]. */
private class GuidedAudioSession(
    context: Context,
    exercise: GuidedExercise,
    onCompleted: () -> Unit,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private var focusRequest: AudioFocusRequest? = null
    private var player: MediaPlayer? = null

    var onFocusLost: () -> Unit = {}

    init {
        val resId = context.resources.getIdentifier(exercise.rawResourceName, "raw", context.packageName)
        player = if (resId == 0) null else MediaPlayer.create(context, resId, attributes, audioManager.generateAudioSessionId())
            ?.also { it.setOnCompletionListener { _ -> abandonFocus(); onCompleted() } }
    }

    val durationMs: Int get() = player?.duration ?: 0
    val positionMs: Int get() = player?.currentPosition ?: 0

    /** Starts or resumes; false when there is no audio or another app keeps the audio focus. */
    fun play(): Boolean {
        val p = player ?: return false
        if (!requestFocus()) return false
        p.start()
        return true
    }

    fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
        abandonFocus()
    }

    fun release() {
        abandonFocus()
        player?.release()
        player = null
    }

    private fun requestFocus(): Boolean {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    player?.takeIf { it.isPlaying }?.pause()
                    onFocusLost()
                }
            }
            .build()
        focusRequest = request
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}
