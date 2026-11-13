package com.maxrave.domain.importing

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LocalAudioLibraryTest {
    @Test fun localDownloadRequestsResolveWithoutRemoteLookup() {
        val root = Files.createTempDirectory("hsm-download-test").toFile()
        try {
            val audio = root.resolve("song.wav").apply { writeBytes(byteArrayOf(1, 2)) }
            assertEquals(3, LocalAudioLibrary.downloadState(audio.toURI().toString()))
            audio.delete()
            assertEquals(0, LocalAudioLibrary.downloadState(audio.toURI().toString()))
            assertEquals(0, LocalAudioLibrary.downloadState("hsm-unmatched:session:0"))
            assertEquals(null, LocalAudioLibrary.downloadState("abcdefghijk"))
        } finally { root.deleteRecursively() }
    }

    @Test fun copiesAudioAndKeepsItPlayableAfterOriginalIsRemoved() {
        val root = Files.createTempDirectory("hsm-local-test").toFile()
        try {
            val source = root.resolve("Nghệ sĩ - Bài hát.wav").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
            val library = root.resolve("library")
            val imported = LocalAudioLibrary.import(source, library)
            source.delete()
            assertEquals("Bài hát", imported.title)
            assertEquals(listOf("Nghệ sĩ"), imported.artistName)
            assertTrue(LocalAudioLibrary.playableFile(imported.videoId)!!.readBytes().contentEquals(byteArrayOf(1, 2, 3, 4)))
            assertTrue(LocalAudioLibrary.playableFile(imported.videoId)!!.canonicalPath.startsWith(library.canonicalPath))
        } finally { root.deleteRecursively() }
    }

    @Test fun deduplicatesContentsAndDistinguishesDifferentAudio() {
        val root = Files.createTempDirectory("hsm-local-test").toFile()
        try {
            val source = root.resolve("song.mp3").apply { writeBytes(byteArrayOf(1, 2)) }
            val library = root.resolve("library")
            val first = LocalAudioLibrary.import(source, library)
            assertEquals(first.videoId, LocalAudioLibrary.import(source, library).videoId)
            source.writeBytes(byteArrayOf(3, 4))
            assertNotEquals(first.videoId, LocalAudioLibrary.import(source, library).videoId)
            assertEquals(2, library.listFiles()!!.size)
        } finally { root.deleteRecursively() }
    }

    @Test fun rejectsUnsupportedAndMissingFilesAndNeverTreatsRemoteIdsAsFiles() {
        val root = Files.createTempDirectory("hsm-local-test").toFile()
        try {
            val wrong = root.resolve("notes.txt").apply { writeText("not audio") }
            assertFailsWith<IllegalArgumentException> { LocalAudioLibrary.import(wrong, root.resolve("library")) }
            assertFailsWith<IllegalArgumentException> { LocalAudioLibrary.import(root.resolve("missing.mp3"), root.resolve("library")) }
            assertEquals(null, LocalAudioLibrary.playableFile("https://example.com/music.mp3"))
            assertEquals(null, LocalAudioLibrary.playableFile("hsm-unmatched:1"))
            assertEquals(null, LocalAudioLibrary.playableFile(root.resolve("missing.mp3").toURI().toString()))
        } finally { root.deleteRecursively() }
    }
}
