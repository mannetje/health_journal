package nl.healthjournal.app

import androidx.annotation.StringRes

/** The three tabs of the bottom navigation bar, with the text shown for each. */
enum class AppNavDestination(@StringRes val titleRes: Int, @StringRes val tabRes: Int, val icon: String) {
    LOG(R.string.nav_title_log, R.string.nav_tab_log, "📝"),
    HISTORY(R.string.nav_title_history, R.string.nav_tab_history, "📊"),
    PROFILE(R.string.nav_title_profile, R.string.nav_tab_profile, "👤")
}
