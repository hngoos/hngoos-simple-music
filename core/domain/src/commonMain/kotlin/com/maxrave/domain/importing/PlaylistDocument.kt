package com.maxrave.domain.importing

/** Spotify exports use UTF-8; native Music/iTunes text exports may use UTF-16. */
object PlaylistDocument {
    fun decode(bytes: ByteArray): String {
        require(bytes.size <= 20_000_000) { "Playlist files must be smaller than 20 MB." }
        fun byte(index: Int) = bytes[index].toInt() and 255
        val little = bytes.size >= 2 && byte(0) == 255 && byte(1) == 254
        val big = bytes.size >= 2 && byte(0) == 254 && byte(1) == 255
        if (!little && !big) return bytes.decodeToString(throwOnInvalidSequence = true).removePrefix("\uFEFF")
        require(bytes.size % 2 == 0) { "The UTF-16 playlist file is truncated." }
        return buildString((bytes.size - 2) / 2) {
            for (index in 2 until bytes.size step 2) {
                append((if (little) byte(index) + (byte(index + 1) shl 8) else (byte(index) shl 8) + byte(index + 1)).toChar())
            }
        }
    }
}
