package nl.healthjournal.app.ui.mindfulness

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import nl.healthjournal.domain.model.mindfulness.BellKind
import nl.healthjournal.domain.model.mindfulness.BreathingPhaseKind
import nl.healthjournal.domain.model.mindfulness.CueChoices
import nl.healthjournal.domain.model.mindfulness.ContentLanguage
import nl.healthjournal.domain.model.mindfulness.ExerciseSession
import nl.healthjournal.domain.model.mindfulness.ToneSynth

private val toneAttributes: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

/**
 * The cues of one session: generated tones, a short vibration and spoken text, each only when chosen. It never
 * touches volume, ringer mode or do-not-disturb. [release] gives back the tone tracks, the speech engine and the
 * vibrator; the session calls it when it ends.
 */
class CuePlayer internal constructor(
    context: Context,
    private val choices: CueChoices,
    language: ContentLanguage,
) {
    private val tracks = mutableListOf<Pair<AudioTrack, Int>>()
    private val vibrator: Vibrator? = if (choices.haptics) vibratorOf(context) else null
    private var speech: TextToSpeech? = null
    private var speechReady = false
    private var pendingSpeech: String? = null

    init {
        if (choices.speech) {
            val locale = Locale.forLanguageTag(language.tag)
            speech = TextToSpeech(context.applicationContext) { status ->
                val engine = speech
                if (engine != null && status == TextToSpeech.SUCCESS && prepareOfflineVoice(engine, locale, choices.voiceName)) {
                    engine.setAudioAttributes(speechAttributes)
                    speechReady = true
                    pendingSpeech?.let { speak(it) }
                    pendingSpeech = null
                }
            }
        }
    }

    fun bell(kind: BellKind) {
        if (choices.tones) playSamples(ToneSynth.bell(kind))
        buzz(if (kind == BellKind.END) 400 else 150)
    }

    /** A cue for the start of a breathing phase; [spoken] is the phase word in the app language. */
    fun phase(kind: BreathingPhaseKind, spoken: String) {
        if (choices.tones) playSamples(ToneSynth.phaseTone(kind))
        buzz(80)
        if (choices.speech) speak(spoken)
    }

    fun release() {
        tracks.forEach { (track, _) -> runCatching { track.release() } }
        tracks.clear()
        runCatching { vibrator?.cancel() }
        speech?.let { runCatching { it.stop(); it.shutdown() } }
        speech = null
        pendingSpeech = null
    }

    private fun speak(text: String) {
        val engine = speech ?: return
        if (!speechReady) {
            pendingSpeech = text
            return
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue")
    }

    private fun buzz(millis: Long) {
        vibrator?.takeIf { it.hasVibrator() }
            ?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun playSamples(samples: ShortArray) {
        tracks.removeAll { (track, frames) ->
            val done = track.playbackHeadPosition >= frames
            if (done) runCatching { track.release() }
            done
        }
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(toneAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(ToneSynth.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
        }.getOrNull() ?: return
        track.write(samples, 0, samples.size)
        track.play()
        tracks += track to samples.size
    }
}

/** Creates the cues for this session and releases them when it ends. Cues that are not chosen cost nothing. */
fun ExerciseSession.cues(context: Context, choices: CueChoices, language: ContentLanguage): CuePlayer {
    val player = CuePlayer(context, choices, language)
    hold { player.release() }
    return player
}

private fun vibratorOf(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

/** The voices for [locale] that work without a network, best quality first. */
private fun findOfflineVoices(engine: TextToSpeech, locale: Locale): List<Voice> =
    runCatching {
        engine.voices.orEmpty().filter { voice ->
            voice.locale.language == locale.language &&
                !voice.isNetworkConnectionRequired &&
                !voice.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }.sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.name })
    }.getOrDefault(emptyList())

/** Uses the voice the user picked when it is still there, otherwise the best one. False when there is none. */
private fun prepareOfflineVoice(engine: TextToSpeech, locale: Locale, preferred: String?): Boolean {
    val voices = findOfflineVoices(engine, locale)
    val voice = voices.firstOrNull { it.name == preferred } ?: voices.firstOrNull() ?: return false
    return engine.setVoice(voice) == TextToSpeech.SUCCESS
}

/** The offline voices of the app language, with a way to hear one. It owns a speech engine until it is dropped. */
class OfflineVoices internal constructor(private val engine: TextToSpeech?, val voices: List<Voice>) {
    fun preview(name: String, text: String) {
        val voice = voices.firstOrNull { it.name == name } ?: return
        val tts = engine ?: return
        tts.setAudioAttributes(speechAttributes)
        tts.setVoice(voice)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview")
    }
}

/**
 * The offline voices for the app language: null while they are being looked up, then a list that is empty
 * when the device has none. The engine used for the lookup and for previews is shut down when the screen leaves.
 */
@Composable
fun rememberOfflineVoices(language: ContentLanguage): OfflineVoices? {
    val context = LocalContext.current
    var found by remember(language) { mutableStateOf<OfflineVoices?>(null) }
    DisposableEffect(language) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            val ready = engine
            found = if (status == TextToSpeech.SUCCESS && ready != null) {
                OfflineVoices(ready, findOfflineVoices(ready, Locale.forLanguageTag(language.tag)))
            } else {
                OfflineVoices(null, emptyList())
            }
        }
        onDispose { runCatching { engine.shutdown() } }
    }
    return found
}
