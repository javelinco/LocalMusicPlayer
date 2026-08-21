package com.javelinco.localmusicplayer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SourceEntity::class,
        TrackEntity::class,
        TrackSearchFts::class,
        ScanCheckpointEntity::class,
        ScanErrorEntity::class,
        PlaylistEntity::class,
        PlaylistEntryEntity::class,
        FavoriteEntity::class,
        QueueSessionEntity::class,
        SettingsMetadataEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LocalMusicDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    abstract fun userDataDao(): UserDataDao
}
