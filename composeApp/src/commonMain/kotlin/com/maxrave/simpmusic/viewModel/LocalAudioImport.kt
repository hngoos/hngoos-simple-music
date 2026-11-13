package com.maxrave.simpmusic.viewModel

import com.eygraber.uri.Uri
import com.maxrave.domain.data.model.importdata.ImportData

/** Returns null for playlist documents, handled by the text importer. */
expect suspend fun importLocalAudio(uri: Uri): ImportData?
