package nl.healthjournal.app.ui.common

/** Where the tab destinations go: a bottom bar on a portrait phone, a rail beside the content otherwise. */
enum class NavigationLayout { BAR, RAIL }

/**
 * The window-size rules of the shell, kept as plain numbers so a test can check them without a device.
 * A rail is used when the window is wide (tablet, unfolded foldable, landscape phone) or short (landscape phone,
 * split screen), so the form keeps the height that is left. Content is centred and capped on wide windows.
 */
object AdaptiveLayout {
    const val RAIL_MIN_WIDTH_DP = 600
    const val RAIL_MAX_HEIGHT_DP = 480
    const val CONTENT_MAX_WIDTH_DP = 840

    fun navigationLayout(widthDp: Int, heightDp: Int): NavigationLayout =
        if (widthDp >= RAIL_MIN_WIDTH_DP || heightDp < RAIL_MAX_HEIGHT_DP) NavigationLayout.RAIL else NavigationLayout.BAR
}
