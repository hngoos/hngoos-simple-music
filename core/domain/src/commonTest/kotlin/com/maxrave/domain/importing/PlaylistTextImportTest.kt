package com.maxrave.domain.importing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlaylistTextImportTest {
    @Test
    fun spotifyCsvPreservesQuotedFieldsUnicodeAndDuplicates() {
        val csv = "\uFEFFTrack Name,Artist Name(s),Album Name,Duration (ms),Track URI\r\n" +
            "\"Hello, \"\"World\"\"\",Ngọt,Album,210000,spotify:track:abc\r\n" +
            "\"Hello, \"\"World\"\"\",Ngọt,Album,210000,spotify:track:abc\r\n"
        val tracks = PlaylistTextImport.parse(csv)
        assertEquals(2, tracks.size)
        assertEquals("Hello, \"World\"", tracks[0].title)
        assertEquals("Ngọt", tracks[0].artist)
        assertEquals(210, tracks[0].durationSeconds)
        assertEquals(tracks[0], tracks[1])
    }

    @Test
    fun appleTsvReadsTotalTimeAndEmbeddedNewlines() {
        val tracks = PlaylistTextImport.parse("Name\tArtist\tAlbum\tTotal Time\n\"First\nSong\"\tArtist\tRecord\t185000")
        assertEquals("First\nSong", tracks.single().title)
        assertEquals(185, tracks.single().durationSeconds)
    }

    @Test
    fun colonDurationIsSecondsAndTrailingEmptyRowsAreIgnored() {
        assertEquals(245, PlaylistTextImport.parse("Title,Artist,Duration\nSong,Artist,4:05\n\n").single().durationSeconds)
    }

    @Test
    fun rejectsUnknownHeadersAndUnclosedQuotesWithoutLosingRows() {
        assertFailsWith<IllegalArgumentException> { PlaylistTextImport.parse("wrong,headers\na,b") }
        assertFailsWith<IllegalArgumentException> { PlaylistTextImport.parse("Title,Artist\n\"Song,Artist") }
        assertFailsWith<IllegalArgumentException> { PlaylistTextImport.parse("Title,Artist\n,Artist") }
    }

    @Test
    fun exactRecordingBeatsLiveCoverAndWrongArtist() {
        val track = ImportedTrack("Hello", "Adele", durationSeconds = 295)
        assertTrue(track.matchScore("Hello", "Adele", 295) >= 0.9)
        assertEquals(0.0, track.matchScore("Hello (Live)", "Adele", 295))
        assertEquals(0.0, track.matchScore("Hello", "Someone else", 295))
        assertEquals(0.0, track.matchScore("Hello (Cover)", "Adele", 295))
        assertEquals(0.0, track.matchScore("Hello", "Adele", 130))
    }

    @Test
    fun acceptsOfficialAudioSuffixWithoutConfusingAcousticVersion() {
        val track = ImportedTrack("Chuyện rằng", "Thịnh Suy", durationSeconds = 210)
        assertTrue(track.matchScore("Chuyện rằng (Official Audio)", "Thịnh Suy", 212) >= 0.9)
        assertEquals(0.0, track.matchScore("Chuyện rằng (Acoustic)", "Thịnh Suy", 210))
    }
}
