package nl.healthjournal.domain.model.mindfulness

/** Why a session ended. Kept only long enough to decide what the screen shows next; never stored. */
enum class SessionEnd {
    /** The exercise, timer or recording reached its end. */
    FINISHED,

    /** The user pressed Stop, Pause or Back. */
    STOPPED,

    /** The app went to the background (home, another app, the screen turned off, a call screen). */
    BACKGROUND,

    /** Another app took the audio, for example a call or an alarm. */
    INTERRUPTED,
}

/** Something a session holds on the phone (the screen-on flag, audio focus, a player) and must give back. */
fun interface SessionResource {
    fun release()
}

/**
 * Everything one exercise, timer or recording holds is registered here, so ending the session gives all of it
 * back in one place and nothing can outlive the screen. A session ends once: the first reason counts, resources
 * are released in the reverse order they were taken, and a failing release never stops the others.
 *
 * Not thread-safe; it is used from the main thread only. It holds no history of what was practised.
 */
class ExerciseSession(private val onEnded: (SessionEnd) -> Unit = {}) {
    private val resources = ArrayDeque<SessionResource>()

    var endedBy: SessionEnd? = null
        private set

    val isActive: Boolean get() = endedBy == null

    /** Registers a resource. When the session has already ended it is released at once, so late arrivals do not leak. */
    fun hold(resource: SessionResource) {
        if (isActive) resources.addLast(resource) else releaseQuietly(resource)
    }

    /** Ends the session. Returns false when it had already ended. */
    fun end(reason: SessionEnd): Boolean {
        if (!isActive) return false
        endedBy = reason
        while (resources.isNotEmpty()) releaseQuietly(resources.removeLast())
        onEnded(reason)
        return true
    }

    private fun releaseQuietly(resource: SessionResource) {
        // Giving a resource back can fail (a player already gone); that is not actionable and must not leave the rest held.
        runCatching { resource.release() }
    }
}
