package com.javelinco.localmusicplayer

import android.app.Application
class LocalMusicPlayerApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    val database get() = container.database
    val sourceRegistry get() = container.sourceRegistry
}
