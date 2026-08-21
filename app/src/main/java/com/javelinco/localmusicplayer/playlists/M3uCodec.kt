package com.javelinco.localmusicplayer.playlists

data class M3uEntry(val title: String, val location: String)

object M3uCodec {
    fun encode(entries: List<M3uEntry>): String = buildString {
        appendLine("#EXTM3U")
        entries.forEach { entry ->
            appendLine("#EXTINF:-1,${entry.title.replace(Regex("[\\r\\n]"), " ")}")
            appendLine(entry.location)
        }
    }

    fun decode(content: String): List<M3uEntry> {
        val result = mutableListOf<M3uEntry>()
        var pendingTitle: String? = null
        content.lineSequence().map(String::trim).filter(String::isNotEmpty).forEach { line ->
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> pendingTitle = line.substringAfter(',', "")
                line.startsWith('#') -> Unit
                else -> {
                    result += M3uEntry(pendingTitle.orEmpty().ifEmpty { line.substringAfterLast('/') }, line)
                    pendingTitle = null
                }
            }
        }
        return result
    }
}
