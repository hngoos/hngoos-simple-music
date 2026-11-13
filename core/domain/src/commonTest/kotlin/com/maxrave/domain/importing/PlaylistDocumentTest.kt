package com.maxrave.domain.importing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlaylistDocumentTest {
    @Test fun readsUtf8WithBomAndVietnamese() {
        assertEquals("Tên,Artist", PlaylistDocument.decode(byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()) + "Tên,Artist".encodeToByteArray()))
    }

    @Test fun readsNativeAppleUtf16InBothByteOrders() {
        val text = "Name\tArtist\nNhạc 🎵\tNgọt"
        for (little in listOf(true, false)) {
            val bom = if (little) listOf(0xff, 0xfe) else listOf(0xfe, 0xff)
            val values = bom + text.flatMap { char ->
                val low = char.code and 255
                val high = char.code ushr 8
                if (little) listOf(low, high) else listOf(high, low)
            }
            assertEquals(text, PlaylistDocument.decode(values.map { it.toByte() }.toByteArray()))
        }
    }

    @Test fun rejectsTruncatedUtf16RatherThanChangingTrackNames() {
        assertFailsWith<IllegalArgumentException> { PlaylistDocument.decode(byteArrayOf(0xff.toByte(), 0xfe.toByte(), 65)) }
    }
}
