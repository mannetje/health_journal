package nl.healthjournal.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLayoutTest {

    @Test
    fun portraitPhoneKeepsTheBottomBar() {
        assertEquals(NavigationLayout.BAR, AdaptiveLayout.navigationLayout(widthDp = 411, heightDp = 891))
    }

    @Test
    fun landscapePhoneUsesTheRail() {
        assertEquals(NavigationLayout.RAIL, AdaptiveLayout.navigationLayout(widthDp = 891, heightDp = 411))
    }

    @Test
    fun tabletUsesTheRail() {
        assertEquals(NavigationLayout.RAIL, AdaptiveLayout.navigationLayout(widthDp = 1280, heightDp = 800))
    }

    @Test
    fun widthEdgeIsInclusive() {
        assertEquals(NavigationLayout.BAR, AdaptiveLayout.navigationLayout(widthDp = 599, heightDp = 900))
        assertEquals(NavigationLayout.RAIL, AdaptiveLayout.navigationLayout(widthDp = 600, heightDp = 900))
    }

    @Test
    fun heightEdgeIsExclusive() {
        assertEquals(NavigationLayout.BAR, AdaptiveLayout.navigationLayout(widthDp = 400, heightDp = 480))
        assertEquals(NavigationLayout.RAIL, AdaptiveLayout.navigationLayout(widthDp = 400, heightDp = 479))
    }

    @Test
    fun contentIsCappedAt840Dp() {
        assertEquals(840, AdaptiveLayout.CONTENT_MAX_WIDTH_DP)
    }
}
