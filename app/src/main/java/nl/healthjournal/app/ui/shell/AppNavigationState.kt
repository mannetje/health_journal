package nl.healthjournal.app.ui.shell

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import nl.healthjournal.app.AppNavDestination

/**
 * Where the person is in the app: a tab, or the pillbox or Mindfulness over the tabs. Holds no Activity, so the
 * rules (opening one screen closes the other, what Back closes) run in a plain JVM test.
 */
class AppNavigationState(
    tab: AppNavDestination = AppNavDestination.LOG,
    pillbox: Boolean = false,
    mindfulness: Boolean = false,
    /** The migraine tab of the Log screen; hoisted so it survives the form opening and closing. */
    logMigraineTab: Boolean = false,
    encryptionBannerDismissed: Boolean = false
) {
    var currentTab by mutableStateOf(tab)
        private set

    /** The pillbox is a separate screen over the tabs, so Back returns to the tab it was opened from. */
    var showPillbox by mutableStateOf(pillbox)
        private set

    /** Mindfulness is also a screen over the tabs; it and the pillbox are never open together. */
    var showMindfulness by mutableStateOf(mindfulness)
        private set

    var logMigraineTab by mutableStateOf(logMigraineTab)

    var encryptionBannerDismissed by mutableStateOf(encryptionBannerDismissed)

    /** Set by a tap on a reminder notification; the pillbox then shows Today whatever it showed before. Not saved. */
    var openPillboxOnToday by mutableStateOf(false)
        private set

    /** The tab to highlight in the bottom bar: none while a screen over the tabs is open. */
    val selectedTab: AppNavDestination? get() = currentTab.takeIf { !showPillbox && !showMindfulness }

    fun openTab(tab: AppNavDestination) {
        showPillbox = false
        showMindfulness = false
        currentTab = tab
    }

    fun openPillbox() {
        showMindfulness = false
        showPillbox = true
    }

    fun openMindfulness() {
        showPillbox = false
        showMindfulness = true
    }

    /** A tap on a reminder notification. */
    fun openPillboxOnToday() {
        openPillbox()
        openPillboxOnToday = true
    }

    fun onOpenedOnToday() {
        openPillboxOnToday = false
    }

    fun closePillbox() {
        showPillbox = false
    }

    fun closeMindfulness() {
        showMindfulness = false
    }

    /** Back closes the migraine form only on the two tabs that show it. */
    fun migraineBackEnabled(migraineOpen: Boolean): Boolean =
        migraineOpen && (currentTab == AppNavDestination.LOG || currentTab == AppNavDestination.HISTORY)

    companion object {
        val Saver: Saver<AppNavigationState, Any> = Saver(
            save = { listOf(it.currentTab.name, it.showPillbox, it.showMindfulness, it.logMigraineTab, it.encryptionBannerDismissed) },
            restore = {
                val values = it as List<*>
                AppNavigationState(
                    tab = AppNavDestination.valueOf(values[0] as String),
                    pillbox = values[1] as Boolean,
                    mindfulness = values[2] as Boolean,
                    logMigraineTab = values[3] as Boolean,
                    encryptionBannerDismissed = values[4] as Boolean
                )
            }
        )
    }
}
