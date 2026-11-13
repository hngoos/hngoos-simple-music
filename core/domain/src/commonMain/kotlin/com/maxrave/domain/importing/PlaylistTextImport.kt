package com.maxrave.domain.importing

import kotlin.math.abs

/** Source metadata stays independent of any chosen YouTube recording. */
data class ImportedTrack(
    val title: String,
    val artist: String,
    val album: String = "",
    val durationSeconds: Int = 0,
    val sourceUri: String = "",
) {
    fun matchScore(candidateTitle: String, candidateArtist: String, candidateDuration: Int?): Double {
        val wanted = words(title)
        val candidate = words(candidateTitle)
        if (wanted.isEmpty() || candidate.isEmpty()) return 0.0
        if (wanted.intersect(versions) != candidate.intersect(versions)) return 0.0
        val titleScore = similarity(wanted, candidate)
        val artistScore = similarity(words(artist), words(candidateArtist))
        if (titleScore < 0.8 || (artist.isNotBlank() && artistScore < 0.5)) return 0.0
        val difference = if (durationSeconds > 0 && candidateDuration != null && candidateDuration > 0) {
            abs(durationSeconds - candidateDuration)
        } else 0
        if (difference > 30) return 0.0
        return (titleScore * 0.65 + artistScore * 0.35) * (1.0 - difference / 300.0)
    }

    private fun similarity(a: Set<String>, b: Set<String>): Double =
        if (a.isEmpty() && b.isEmpty()) 1.0 else if (a.isEmpty() || b.isEmpty()) 0.0
        else a.intersect(b).size.toDouble() / a.union(b).size

    private fun words(value: String): Set<String> = value.lowercase()
        .replace(Regex("\\b(official|audio|video|lyrics|lyric|hd|hq|topic)\\b"), " ")
        .map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("")
        .split(' ').filter { it.isNotBlank() }.toSet()

    private companion object {
        val versions = setOf("live", "remix", "acoustic", "instrumental", "karaoke", "cover", "sped", "slowed", "remaster", "remastered")
    }
}

/** Reads Spotify-style CSV or Apple Music/iTunes TSV, without dropping malformed records. */
object PlaylistTextImport {
    fun parse(text: String): List<ImportedTrack> {
        require(text.length <= 20_000_000) { "The playlist file is too large (maximum 20 MB)." }
        val content = text.removePrefix("\uFEFF")
        val firstLine = content.lineSequence().firstOrNull().orEmpty()
        val delimiter = if (firstLine.count { it == '\t' } > firstLine.count { it == ',' }) '\t' else ','
        val rows = readRows(content, delimiter).filter { row -> row.any { it.isNotBlank() } }
        require(rows.size > 1) { "The playlist file is empty." }
        val header = rows.first().map { it.trim().lowercase() }
        fun index(vararg names: String): Int = header.indexOfFirst { it in names }
        val titleIndex = index("track name", "track title", "title", "name", "song", "song name")
        val artistIndex = index("artist name(s)", "artist name", "artists", "artist", "album artist")
        val albumIndex = index("album name", "album")
        val durationIndex = index("duration (ms)", "duration_ms", "total time", "duration", "time")
        val uriIndex = index("track uri", "track url", "spotify uri", "url", "uri")
        require(titleIndex >= 0 && artistIndex >= 0) { "Expected Title/Track Name and Artist columns." }
        require(rows.size <= 10_001) { "Import at most 10,000 tracks at a time." }
        return rows.drop(1).mapIndexed { rowIndex, row ->
            fun field(column: Int): String = row.getOrNull(column)?.trim().orEmpty()
            val title = field(titleIndex)
            require(title.isNotBlank()) { "Track title is empty at row ${rowIndex + 2}." }
            val duration = field(durationIndex)
            val seconds = if (':' in duration) {
                val parts = duration.split(':').map { it.toIntOrNull() ?: 0 }
                parts.fold(0) { total, part -> total * 60 + part }
            } else {
                val value = duration.toDoubleOrNull() ?: 0.0
                if (durationIndex >= 0 && header[durationIndex] in setOf("duration (ms)", "duration_ms", "total time")) {
                    (value / 1000).toInt()
                } else value.toInt()
            }
            ImportedTrack(title, field(artistIndex), field(albumIndex), seconds.coerceAtLeast(0), field(uriIndex))
        }
    }

    private fun readRows(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var position = 0
        fun endField() { row.add(field.toString()); field.clear() }
        fun endRow() { endField(); rows.add(row); row = mutableListOf() }
        while (position < text.length) {
            val char = text[position]
            when {
                char == '"' && quoted && text.getOrNull(position + 1) == '"' -> { field.append('"'); position++ }
                char == '"' -> quoted = !quoted
                quoted -> field.append(char)
                char == delimiter -> endField()
                char == '\n' -> endRow()
                char == '\r' -> { endRow(); if (text.getOrNull(position + 1) == '\n') position++ }
                else -> field.append(char)
            }
            position++
        }
        require(!quoted) { "Unclosed quoted field in playlist file." }
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}
