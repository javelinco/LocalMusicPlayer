package com.javelinco.localmusicplayer.data.source

/** Owns Android's persisted read grants without coupling the source registry to ContentResolver. */
interface SafPermissionStore {
    fun takeReadPermission(uri: String)

    fun releaseReadPermission(uri: String)

    fun hasReadPermission(uri: String): Boolean
}
