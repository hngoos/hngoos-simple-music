package com.maxrave.simpmusic.viewModel

import com.eygraber.uri.Uri
import com.maxrave.data.io.getHomeFolderPath
import com.maxrave.domain.data.model.importdata.ImportData
import com.maxrave.domain.data.model.importdata.ImportPlaylist
import com.maxrave.domain.importing.LocalAudioLibrary
import java.io.File
import kotlinx.coroutines.runInterruptible

actual suspend fun importLocalAudio(uri: Uri): ImportData? {
    val source = LocalAudioLibrary.pickedFile(uri.toString())
    if (!LocalAudioLibrary.supports(source)) return null
    val song = runInterruptible { LocalAudioLibrary.import(source, File(getHomeFolderPath(listOf(".hsm", "library")))) }
    return ImportData(listOf(song), listOf(ImportPlaylist("Local · ${song.title}", videoIds = listOf(song.videoId))))
}
