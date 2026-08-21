package com.javelinco.localmusicplayer.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.javelinco.localmusicplayer.ui.library.LibraryView
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppSettingsTest {
    private lateinit var store: DataStore<Preferences>
    private lateinit var file: File
    private lateinit var storeScope: CoroutineScope

    @Before
    fun createStore() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        file = context.preferencesDataStoreFile("settings-${System.nanoTime()}")
        storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store = PreferenceDataStoreFactory.create(
            scope = storeScope,
            produceFile = { file },
        )
    }

    @After
    fun deleteStore() {
        storeScope.cancel()
        file.delete()
    }

    @Test
    fun libraryViewDefaultsToTracksAndPersistsSelection() = runTest {
        val settings = AppSettings(store)

        assertEquals(LibraryView.TRACKS, settings.state.first().libraryView)
        settings.setLibraryView(LibraryView.ALBUMS)

        assertEquals(LibraryView.ALBUMS, settings.state.first().libraryView)
    }
}
