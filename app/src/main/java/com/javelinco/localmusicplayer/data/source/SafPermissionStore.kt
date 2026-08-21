package com.javelinco.localmusicplayer.data.source

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

/** Owns Android's persisted read grants without coupling the source registry to ContentResolver. */
interface SafPermissionStore {
    fun takeReadPermission(uri: String)

    fun releaseReadPermission(uri: String)

    fun hasReadPermission(uri: String): Boolean
}

class AndroidSafPermissionStore(
    private val contentResolver: ContentResolver,
) : SafPermissionStore {
    override fun takeReadPermission(uri: String) {
        contentResolver.takePersistableUriPermission(
            Uri.parse(uri),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    override fun releaseReadPermission(uri: String) {
        contentResolver.releasePersistableUriPermission(
            Uri.parse(uri),
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }

    override fun hasReadPermission(uri: String): Boolean =
        contentResolver.persistedUriPermissions.any { permission ->
            permission.uri.toString() == uri && permission.isReadPermission
        }
}
