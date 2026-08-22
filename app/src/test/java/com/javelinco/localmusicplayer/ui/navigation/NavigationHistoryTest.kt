package com.javelinco.localmusicplayer.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationHistoryTest {
    @Test
    fun backUnwindsVisitedScreensInReverseOrderThenStaysHome() {
        val settings = NavigationHistory(Destination.HOME)
            .navigateTo(Destination.MORE)
            .navigateTo(Destination.SETTINGS)

        val more = settings.goBack()
        val home = more.goBack()
        val rootHome = home.goBack()

        assertEquals(Destination.MORE, more.current)
        assertEquals(Destination.HOME, home.current)
        assertEquals(NavigationHistory(Destination.HOME), rootHome)
    }

    @Test
    fun emptyHistoryFallsBackToHomeAndDuplicateNavigationAddsNothing() {
        assertEquals(
            NavigationHistory(Destination.HOME),
            NavigationHistory(Destination.LIBRARY).goBack(),
        )
        assertEquals(
            NavigationHistory(Destination.LIBRARY),
            NavigationHistory(Destination.LIBRARY).navigateTo(Destination.LIBRARY),
        )
    }

    @Test
    fun stringSnapshotRoundTripsNavigationState() {
        val state = NavigationHistory(
            current = Destination.QUEUE,
            previous = listOf(Destination.HOME, Destination.NOW_PLAYING),
        )

        assertEquals(state, restoreNavigationHistory(saveNavigationHistory(state)))
    }
}
