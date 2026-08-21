package com.javelinco.localmusicplayer.ui.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySourcePlacementTest {
    @Test
    fun sourceSetupAppearsInLibraryOnlyUntilAFolderExists() {
        assertTrue(shouldShowSourceSetupInLibrary(sourceCount = 0))
        assertFalse(shouldShowSourceSetupInLibrary(sourceCount = 1))
        assertFalse(shouldShowSourceSetupInLibrary(sourceCount = 3))
    }
}
