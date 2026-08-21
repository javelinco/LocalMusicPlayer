package com.javelinco.localmusicplayer.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.javelinco.localmusicplayer.ui.library.LibraryView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class SettingsState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val reducedMotion: Boolean = false,
    val backupTreeUri: String? = null,
    val libraryView: LibraryView = LibraryView.TRACKS,
)

private val Context.localMusicSettings by preferencesDataStore("local_music_settings")

class AppSettings internal constructor(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.localMusicSettings)

    val state: Flow<SettingsState> = dataStore.data.map { values ->
        SettingsState(
            theme = values[THEME]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
            reducedMotion = values[REDUCED_MOTION] ?: false,
            backupTreeUri = values[BACKUP_TREE_URI],
            libraryView = values[LAST_LIBRARY_VIEW]
                ?.let { runCatching { LibraryView.valueOf(it) }.getOrNull() }
                ?: LibraryView.TRACKS,
        )
    }

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME] = theme.name }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        dataStore.edit { it[REDUCED_MOTION] = enabled }
    }

    suspend fun setBackupTreeUri(uri: String) {
        dataStore.edit { it[BACKUP_TREE_URI] = uri }
    }

    suspend fun setLibraryView(view: LibraryView) {
        dataStore.edit { it[LAST_LIBRARY_VIEW] = view.name }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val BACKUP_TREE_URI = stringPreferencesKey("backup_tree_uri")
        val LAST_LIBRARY_VIEW = stringPreferencesKey("last_library_view")
    }
}
