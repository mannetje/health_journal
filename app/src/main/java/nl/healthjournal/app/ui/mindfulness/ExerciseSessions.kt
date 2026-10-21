package nl.healthjournal.app.ui.mindfulness

import android.content.Context
import android.content.ContextWrapper
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import nl.healthjournal.domain.model.mindfulness.ExerciseSession
import nl.healthjournal.domain.model.mindfulness.SessionEnd

/**
 * Tells the rest of the app that an exercise, timer or recording is running, so spontaneous dialogs wait
 * until it ends. The app lock needs no gate: it only fires after the app has been in the background, and
 * going to the background ends the session first.
 */
class ExerciseGate {
    private var count by mutableIntStateOf(0)
    val active: Boolean get() = count > 0
    internal fun opened() { count++ }
    internal fun closed() { count-- }
}

val LocalExerciseGate = staticCompositionLocalOf { ExerciseGate() }

/**
 * Starts sessions for one screen and ends the running one when the app goes to the background or the screen
 * leaves. Callbacks stop after the screen has left, so a late event cannot navigate somewhere new.
 */
class SessionLauncher internal constructor(private val gate: ExerciseGate) {
    private var current: ExerciseSession? = null
    private var alive = true

    fun start(onEnded: (SessionEnd) -> Unit): ExerciseSession {
        current?.end(SessionEnd.STOPPED)
        val session = ExerciseSession { reason -> if (alive) onEnded(reason) }
        gate.opened()
        session.hold { gate.closed() }
        current = session
        return session
    }

    internal fun endCurrent(reason: SessionEnd) {
        current?.end(reason)
    }

    internal fun close() {
        alive = false
        endCurrent(SessionEnd.STOPPED)
        current = null
    }
}

@Composable
fun rememberSessionLauncher(): SessionLauncher {
    val context = LocalContext.current
    val gate = LocalExerciseGate.current
    val launcher = remember { SessionLauncher(gate) }
    DisposableEffect(launcher) {
        val lifecycle = context.findActivity()?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) launcher.endCurrent(SessionEnd.BACKGROUND)
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
            launcher.close()
        }
    }
    return launcher
}

/** Keeps the display on until the session ends. */
fun ExerciseSession.keepScreenOn(view: View) {
    view.keepScreenOn = true
    hold { view.keepScreenOn = false }
}

internal val speechAttributes: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
    .build()

/**
 * Takes audio focus for the session and gives it back when the session ends. Losing the focus to another app
 * (a call, an alarm) ends the session as INTERRUPTED. Returns false when the focus is not granted.
 */
fun ExerciseSession.requestAudioFocus(context: Context): Boolean {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(speechAttributes)
        .setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                end(SessionEnd.INTERRUPTED)
            }
        }
        .build()
    if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
    hold { audioManager.abandonAudioFocusRequest(request) }
    return true
}

internal tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
