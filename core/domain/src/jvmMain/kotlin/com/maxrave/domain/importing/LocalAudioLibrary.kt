package com.maxrave.domain.importing

import com.maxrave.domain.data.model.importdata.ImportSong
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Managed copies keep a local playlist playable when the original file is moved. */
object LocalAudioLibrary {
    private val extensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "aiff", "aif", "wma")

    fun supports(file: File): Boolean = file.extension.lowercase() in extensions

    fun import(source: File, library: File): ImportSong {
        require(source.isFile && source.length() > 0) { "The audio file is missing or empty." }
        require(supports(source)) { "Choose MP3, M4A, AAC, FLAC, WAV, OGG, Opus, AIFF or WMA audio." }
        Files.createDirectories(library.toPath())
        val temporary = Files.createTempFile(library.toPath(), ".import-", ".tmp").toFile()
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            source.inputStream().buffered().use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (Thread.currentThread().isInterrupted) throw InterruptedException("Audio import cancelled")
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            val target = library.resolve("$hash.${source.extension.lowercase()}")
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            val parts = source.nameWithoutExtension.split(" - ", limit = 2)
            return ImportSong(
                videoId = target.toURI().toASCIIString(),
                title = parts.last(),
                artistName = if (parts.size == 2) listOf(parts.first()) else null,
                sourceUri = source.toURI().toASCIIString(),
            )
        } finally { temporary.delete() }
    }

    fun pickedFile(value: String): File = if (value.startsWith("file:", ignoreCase = true)) File(URI(value)) else File(value)

    fun playableFile(id: String): File? = if (id.startsWith("file:", ignoreCase = true)) {
        runCatching { File(URI(id)).takeIf { it.isFile && it.length() > 0 } }.getOrNull()
    } else null
}
