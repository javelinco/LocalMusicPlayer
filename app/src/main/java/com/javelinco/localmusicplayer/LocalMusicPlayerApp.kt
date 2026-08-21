package com.javelinco.localmusicplayer

import android.app.Application
import androidx.room.Room
import com.javelinco.localmusicplayer.data.db.LocalMusicDatabase
import com.javelinco.localmusicplayer.data.source.RoomSourceRegistry

class LocalMusicPlayerApp : Application() {
    val database: LocalMusicDatabase by lazy {
        Room.databaseBuilder(this, LocalMusicDatabase::class.java, "local-music.db").build()
    }

    val sourceRegistry: RoomSourceRegistry by lazy { RoomSourceRegistry(database.libraryDao()) }
}
