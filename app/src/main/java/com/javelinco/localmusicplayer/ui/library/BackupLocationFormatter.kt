package com.javelinco.localmusicplayer.ui.library

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal fun backupFolderDisplayPath(treeUri: String): String {
    val encodedTreeId = treeUri.substringAfter("/tree/", "")
        .substringBefore('/')
        .takeIf(String::isNotBlank)
        ?: return treeUri
    val documentId = URLDecoder.decode(
        encodedTreeId.replace("+", "%2B"),
        StandardCharsets.UTF_8.name(),
    )
    val root = documentId.substringBefore(':')
    val relative = documentId.substringAfter(':', "")
    val rootLabel = when (root.lowercase()) {
        "primary" -> "Internal storage"
        "downloads" -> "Downloads"
        "home" -> "Documents"
        "raw" -> "Device storage"
        else -> if (relative.isBlank()) root else "Storage $root"
    }
    return (listOf(rootLabel) + relative.split('/').filter(String::isNotBlank))
        .filter(String::isNotBlank)
        .joinToString(" / ")
        .ifBlank { treeUri }
}
