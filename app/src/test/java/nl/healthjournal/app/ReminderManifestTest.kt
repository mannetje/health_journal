package nl.healthjournal.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reminder alarms only survive a reboot, an update and a clock change when the manifest says so, and nothing
 * else notices when a line is lost. This reads the manifest (ADR 0022) so a lost permission or action fails a test.
 */
class ReminderManifestTest {

    private val manifest = File("..", "app/src/main/AndroidManifest.xml").readText()

    private fun permission(name: String) =
        Regex("""<uses-permission\s+android:name="android\.permission\.$name"([^>]*)/>""").find(manifest)

    @Test
    fun exactAlarmsAreGrantedAtInstallFromAndroid13AndAskedForBefore() {
        assertTrue("USE_EXACT_ALARM is declared", permission("USE_EXACT_ALARM") != null)
        val legacy = permission("SCHEDULE_EXACT_ALARM")
        assertTrue("SCHEDULE_EXACT_ALARM stays for Android 12", legacy != null)
        assertTrue("SCHEDULE_EXACT_ALARM is limited to Android 12L and below", """android:maxSdkVersion="32"""" in legacy!!.groupValues[1])
    }

    @Test
    fun bootAndNotificationPermissionsAreDeclared() {
        assertTrue(permission("RECEIVE_BOOT_COMPLETED") != null)
        assertTrue(permission("POST_NOTIFICATIONS") != null)
    }

    @Test
    fun theRescheduleReceiverListensForEveryEventThatMovesAnAlarm() {
        val receiver = Regex("""<receiver\s+android:name="\.reminder\.RescheduleReceiver".*?</receiver>""", RegexOption.DOT_MATCHES_ALL)
            .find(manifest)?.value
        assertTrue("RescheduleReceiver is declared", receiver != null)
        val actions = Regex("""<action android:name="([^"]+)"""").findAll(receiver!!).map { it.groupValues[1] }.toSet()
        assertEquals(
            setOf(
                "android.intent.action.BOOT_COMPLETED",
                "android.intent.action.MY_PACKAGE_REPLACED",
                "android.intent.action.TIME_SET",
                "android.intent.action.TIMEZONE_CHANGED",
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
            ),
            actions
        )
    }

    @Test
    fun onlyTheRescheduleReceiverIsExported() {
        val exported = Regex("""<receiver\s+android:name="([^"]+)"\s+android:exported="true"""").findAll(manifest).map { it.groupValues[1] }.toList()
        assertEquals(listOf(".reminder.RescheduleReceiver"), exported)
    }
}
