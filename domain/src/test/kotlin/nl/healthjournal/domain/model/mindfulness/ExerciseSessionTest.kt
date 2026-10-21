package nl.healthjournal.domain.model.mindfulness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSessionTest {
    private val log = mutableListOf<String>()
    private fun resource(name: String) = SessionResource { log += name }

    @Test
    fun `a new session is active and has not ended`() {
        val session = ExerciseSession()
        assertTrue(session.isActive)
        assertNull(session.endedBy)
    }

    @Test
    fun `ending releases every resource in the reverse order they were taken`() {
        val session = ExerciseSession()
        session.hold(resource("screen"))
        session.hold(resource("focus"))
        session.hold(resource("player"))

        session.end(SessionEnd.STOPPED)

        assertEquals(listOf("player", "focus", "screen"), log)
    }

    @Test
    fun `nothing is released before the session ends`() {
        val session = ExerciseSession()
        session.hold(resource("screen"))
        assertTrue(log.isEmpty())
    }

    @Test
    fun `ending twice releases once and keeps the first reason`() {
        val session = ExerciseSession()
        session.hold(resource("screen"))

        assertTrue(session.end(SessionEnd.BACKGROUND))
        assertFalse(session.end(SessionEnd.STOPPED))

        assertEquals(listOf("screen"), log)
        assertEquals(SessionEnd.BACKGROUND, session.endedBy)
        assertFalse(session.isActive)
    }

    @Test
    fun `the end callback runs once, after the resources are released`() {
        val seen = mutableListOf<String>()
        val session = ExerciseSession { reason -> seen += "ended:$reason" }
        session.hold(SessionResource { seen += "released" })

        session.end(SessionEnd.FINISHED)
        session.end(SessionEnd.INTERRUPTED)

        assertEquals(listOf("released", "ended:FINISHED"), seen)
    }

    @Test
    fun `a resource taken after the session ended is released at once`() {
        val session = ExerciseSession()
        session.end(SessionEnd.INTERRUPTED)

        session.hold(resource("late"))

        assertEquals(listOf("late"), log)
    }

    @Test
    fun `a resource that fails to release does not stop the others`() {
        val session = ExerciseSession()
        session.hold(resource("screen"))
        session.hold(SessionResource { error("player already gone") })
        session.hold(resource("focus"))

        session.end(SessionEnd.STOPPED)

        assertEquals(listOf("focus", "screen"), log)
        assertEquals(SessionEnd.STOPPED, session.endedBy)
    }

    @Test
    fun `a resource released while ending may end the session again without effect`() {
        lateinit var session: ExerciseSession
        session = ExerciseSession()
        session.hold(SessionResource { session.end(SessionEnd.INTERRUPTED) })
        session.hold(resource("screen"))

        session.end(SessionEnd.FINISHED)

        assertEquals(listOf("screen"), log)
        assertEquals(SessionEnd.FINISHED, session.endedBy)
    }
}
