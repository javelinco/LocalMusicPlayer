package com.javelinco.localmusicplayer.data.scan

import java.io.File
import java.security.MessageDigest

class ArtworkCache(private val directory: File) {
    init {
        directory.mkdirs()
    }

    fun read(albumKey: String): ByteArray? = fileFor(albumKey).takeIf(File::isFile)?.readBytes()

    fun write(albumKey: String, bytes: ByteArray) {
        val target = fileFor(albumKey)
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeBytes(bytes)
        if (!temporary.renameTo(target)) {
            target.writeBytes(bytes)
            temporary.delete()
        }
    }

    private fun fileFor(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return File(directory, digest.joinToString("") { "%02x".format(it) })
    }
}
