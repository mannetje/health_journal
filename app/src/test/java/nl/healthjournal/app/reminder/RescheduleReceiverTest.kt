package nl.healthjournal.app.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * RescheduleReceiver is exported so the system can send it boot and time broadcasts, which means another app
 * can send it an intent too. It only acts on the actions of its manifest filter; this keeps the two in step.
 */
class RescheduleReceiverTest {

    @Test
    fun the_allow_list_equals_the_actions_of_the_manifest_filter() {
        // Unit tests run with the module directory as working directory.
        val manifest = File("src/main/AndroidManifest.xml")
        assertTrue("${manifest.absolutePath} must exist", manifest.exists())
        val receiver = manifest.readText()
            .substringAfter("""android:name=".reminder.RescheduleReceiver"""")
            .substringBefore("</receiver>")
        val declared = Regex("""<action\s+android:name="([^"]+)"""").findAll(receiver).map { it.groupValues[1] }.toSet()

        assertEquals(declared, RescheduleReceiver.ACTIONS)
    }
}
