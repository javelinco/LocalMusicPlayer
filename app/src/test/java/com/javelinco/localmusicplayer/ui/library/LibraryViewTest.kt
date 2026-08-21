package com.javelinco.localmusicplayer.ui.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryViewTest {
    @Test
    fun selectorLabelsStateTheirPurposeAndCurrentSelection() {
        assertEquals(
            listOf(
                "View: Tracks",
                "View: Artists",
                "View: Albums",
                "View: Genres",
                "View: Playlists",
            ),
            LibraryView.entries.map(LibraryView::selectorLabel),
        )
    }
}
