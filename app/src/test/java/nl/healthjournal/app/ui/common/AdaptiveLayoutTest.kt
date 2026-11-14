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

    @Test
    fun historyHasTwoPanesFrom900Dp() {
        assertEquals(false, AdaptiveLayout.historyTwoPane(widthDp = 411))
        assertEquals(false, AdaptiveLayout.historyTwoPane(widthDp = 800))
        assertEquals(false, AdaptiveLayout.historyTwoPane(widthDp = 899))
        assertEquals(true, AdaptiveLayout.historyTwoPane(widthDp = 900))
        assertEquals(true, AdaptiveLayout.historyTwoPane(widthDp = 1280))
    }

    @Test
    fun twoPaneHistoryUsesTheWiderCap() {
        assertEquals(1240, AdaptiveLayout.contentMaxWidthDp(historyTwoPane = true))
        assertEquals(840, AdaptiveLayout.contentMaxWidthDp(historyTwoPane = false))
    }
}
