package com.maxrave.data.repository

import com.maxrave.data.db.datasource.LocalDataSource
import com.maxrave.domain.data.entities.LocalPlaylistEntity
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.importdata.ImportData
import com.maxrave.domain.data.model.importdata.ImportPlaylist
import com.maxrave.domain.data.model.importdata.ImportResult
import com.maxrave.domain.data.model.importdata.ImportSong
import com.maxrave.domain.repository.ImportProgress
import com.maxrave.domain.repository.ImportRepository
import com.maxrave.domain.utils.MusicVideoType
import com.maxrave.logger.Logger
import com.maxrave.domain.importing.ImportedTrack
import com.maxrave.domain.importing.PlaylistTextImport
import com.maxrave.kotlinytmusicscraper.YouTube
import com.maxrave.kotlinytmusicscraper.models.SongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

private const val TAG = "ImportRepositoryImpl"

/**
 * How many songs go into one transaction.
 *
 * No DAO method takes a list and there is no ambient transaction, so writing 10,000 songs one at a
 * time means 10,000 commits. Batching is the whole performance story here.
 */
private const val SONG_BATCH_SIZE = 500

internal class ImportRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : ImportRepository {
    /**
     * Same posture as the Room converters: unknown keys are dropped rather than rejected, so a file
     * written by a newer converter still imports.
     */
    private val jsonFormat =
        Json {
            ignoreUnknownKeys = true
        }

    override fun import(
        json: String,
        invalidFileMessage: String,
    ): Flow<ImportProgress> =
        flow {
            emit(ImportProgress.Preparing)

            var lookupFailures = 0
            val data =
                runCatching {
                    if (json.removePrefix("\uFEFF").trimStart().startsWith("{")) {
                        jsonFormat.decodeFromString<ImportData>(json.removePrefix("\uFEFF"))
                    } else {
                        val entries = PlaylistTextImport.parse(json)
                        val importId = Uuid.random().toString()
                        val resolved = mutableMapOf<ImportedTrack, SongItem?>()
                        val songs = entries.mapIndexed { index, track ->
                            emit(ImportProgress.Resolving(index, entries.size))
                            val query = "${track.title} ${track.artist}".trim()
                            val recording = if (resolved.containsKey(track)) resolved[track] else {
                                var lookupCompleted = false
                                val result = withTimeoutOrNull(15_000) {
                                    val response = youTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                                    response.exceptionOrNull()?.let { if (it is CancellationException) throw it }
                                    val page = response.getOrNull()
                                    lookupCompleted = page != null
                                    page?.items?.filterIsInstance<SongItem>()
                                        ?.map { candidate -> candidate to track.matchScore(candidate.title, candidate.artists.joinToString(" ") { it.name }, candidate.duration) }
                                        ?.filter { it.second >= 0.9 }
                                        ?.sortedByDescending { it.second }
                                        ?.let { candidates ->
                                            val best = candidates.firstOrNull()
                                            val next = candidates.getOrNull(1)
                                            best?.takeIf { next == null || best.second - next.second >= 0.03 || best.first.album?.name == track.album }?.first
                                        }
                                }
                                if (!lookupCompleted) lookupFailures++
                                result.also { resolved[track] = it }
                            }
                            ImportSong(
                                videoId = recording?.id ?: "hsm-unmatched:$importId:$index",
                                title = track.title,
                                artistName = listOf(track.artist),
                                albumName = track.album,
                                durationSeconds = recording?.duration ?: track.durationSeconds,
                                thumbnails = recording?.thumbnail,
                                isAvailable = recording != null,
                                sourceUri = track.sourceUri,
                            )
                        }
                        ImportData(songs, listOf(ImportPlaylist("Imported playlist", videoIds = songs.map { it.videoId })))
                    }
                }
                    .getOrElse { throwable ->
                        if (throwable is CancellationException) throw throwable
                        Logger.e(TAG, "import: cannot parse file - ${throwable.message}")
                        emit(ImportProgress.Error(throwable.message ?: invalidFileMessage))
                        return@flow
                    }

            // A file that parses but carries nothing is almost always the wrong file: with unknown
            // keys ignored, any JSON object decodes into an empty ImportData. Reporting that as an
            // error beats reporting a successful import of zero songs.
            if (data.songs.isEmpty() && data.playlists.isEmpty()) {
                Logger.e(TAG, "import: file has no songs and no playlists")
                emit(ImportProgress.Error(invalidFileMessage))
                return@flow
            }

            runCatching {
                val songsById = data.songs.associateBy { it.videoId }
                val total = data.songs.size

                var written = 0
                data.songs.chunked(SONG_BATCH_SIZE).forEach { chunk ->
                    localDataSource.insertSongs(chunk.map { it.toSongEntity() })
                    written += chunk.size
                    emit(ImportProgress.Importing(processed = written, total = total))
                }

                var playlistsCreated = 0
                var skippedEntries = 0
                data.playlists.forEach { playlist ->
                    // Dropping ids with no song row is what keeps the foreign key on
                    // pair_song_local_playlist satisfied; positions are then taken from the index
                    // in the filtered list so they stay contiguous from 0.
                    val videoIds = playlist.videoIds.filter { songsById.containsKey(it) }
                    skippedEntries += playlist.videoIds.size - videoIds.size
                    val playlistId =
                        localDataSource.insertLocalPlaylistWithTracks(
                            localPlaylist = playlist.toLocalPlaylistEntity(videoIds),
                            videoIds = videoIds,
                        )
                    if (playlistId != -1L) playlistsCreated++
                }

                ImportResult(
                    playlistsCreated = playlistsCreated,
                    songsImported = written,
                    skippedEntries = skippedEntries,
                    lookupFailures = lookupFailures,
                    unmatchedTracks = data.songs.filter { !it.isAvailable }.map { "${it.title} — ${it.artistName.orEmpty().joinToString(", ")}" },
                )
            }.onSuccess { result ->
                Logger.i(TAG, "import: $result")
                emit(ImportProgress.Success(result))
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                Logger.e(TAG, "import: failed while writing - ${throwable.message}")
                emit(ImportProgress.Error(throwable.message ?: invalidFileMessage))
            }
        }.flowOn(Dispatchers.IO)
}

/**
 * Mirrors `Track.toSongEntity()`, filling the runtime-only columns with the values an imported
 * track starts life with: not liked, never played, not downloaded, available.
 *
 * [ImportSong.artistId] is kept only when it lines up one-to-one with [ImportSong.artistName].
 * `SongEntity.toTrack()` walks the name list and indexes into the id list, so a non-null id list
 * that is shorter would be read past its end.
 *
 * [ImportSong.videoType] arrives from a file this app did not write, so it is normalized rather
 * than trusted: a real `MUSIC_VIDEO_TYPE_*` is kept, anything else becomes the empty "unknown"
 * the column already uses, and the playback path fills it in from the API on first play.
 */
private fun ImportSong.toSongEntity(): SongEntity =
    SongEntity(
        videoId = videoId,
        albumId = albumId,
        albumName = albumName,
        artistId = artistId?.takeIf { it.size == (artistName?.size ?: 0) },
        artistName = artistName,
        duration = duration,
        durationSeconds = durationSeconds,
        isAvailable = isAvailable,
        isExplicit = isExplicit,
        likeStatus = "",
        thumbnails = thumbnails,
        title = title,
        videoType = MusicVideoType.normalize(videoType) ?: "",
        category = sourceUri.takeIf { it.isNotBlank() },
        resultType = if (isAvailable) null else "HSM_UNMATCHED",
        liked = false,
        totalPlayTime = 0,
        downloadState = 0,
    )

/**
 * Imported playlists are local-only: no YouTube id, no sync state, nothing to download yet.
 *
 * [videoIds] is the already-filtered track list, written to the `tracks` column once rather than
 * updated per track.
 */
private fun ImportPlaylist.toLocalPlaylistEntity(videoIds: List<String>): LocalPlaylistEntity =
    LocalPlaylistEntity(
        title = title,
        thumbnail = thumbnail,
        tracks = videoIds,
    )
