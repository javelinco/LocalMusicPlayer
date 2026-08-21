package com.javelinco.localmusicplayer.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class SettingsState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val reducedMotion: Boolean = false,
    val backupTreeUri: String? = null,
)

private val Context.localMusicSettings by preferencesDataStore("local_music_settings")

class AppSettings(private val context: Context) {
    val state: Flow<SettingsState> = context.localMusicSettings.data.map { values ->
        SettingsState(
            theme = values[THEME]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
            reducedMotion = values[REDUCED_MOTION] ?: false,
            backupTreeUri = values[BACKUP_TREE_URI],
        )
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.localMusicSettings.edit { it[THEME] = theme.name }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.localMusicSettings.edit { it[REDUCED_MOTION] = enabled }
    }

    suspend fun setBackupTreeUri(uri: String) {
        context.localMusicSettings.edit { it[BACKUP_TREE_URI] = uri }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val BACKUP_TREE_URI = stringPreferencesKey("backup_tree_uri")
    }
}
