package com.maxrave.domain.importing

import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.utils.toArrayListTrack
import com.maxrave.domain.utils.youtubeArtworkOrEmpty
import kotlin.test.Test
import kotlin.test.assertEquals

class HsmMediaBoundaryTest {
    @Test fun neverBuildsRemoteArtworkFromLocalPathsOrUnmatchedIds() {
        assertEquals("", youtubeArtworkOrEmpty("file:/C:/Users/name/private%20music/song.mp3"))
        assertEquals("", youtubeArtworkOrEmpty("hsm-unmatched:session:1"))
        assertEquals("", youtubeArtworkOrEmpty(null))
        assertEquals("https://i.ytimg.com/vi/abcdefghijk/maxresdefault.jpg", youtubeArtworkOrEmpty("abcdefghijk"))
    }

    @Test fun queueSkipsUnmatchedEntriesAndPreservesPlayableOrderAndDuplicates() {
        fun song(id: String) = SongEntity(
            videoId = id, title = id, duration = "", durationSeconds = 0,
            isAvailable = !id.startsWith("hsm-unmatched:"), isExplicit = false,
            likeStatus = "", videoType = "", category = null, resultType = null,
        )
        val ids = listOf("hsm-unmatched:0", "abcdefghijk", "file:/music/local.wav", "hsm-unmatched:2", "abcdefghijk")
        assertEquals(listOf("abcdefghijk", "file:/music/local.wav", "abcdefghijk"), ids.map(::song).toArrayListTrack().map { it.videoId })
    }
}
