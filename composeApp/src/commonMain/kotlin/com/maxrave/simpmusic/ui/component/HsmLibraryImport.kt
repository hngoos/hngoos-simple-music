package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eygraber.uri.Uri
import com.maxrave.domain.repository.ImportProgress
import com.maxrave.simpmusic.expect.ui.filePickerResult
import com.maxrave.simpmusic.ui.screen.home.ImportProgressDialog
import com.maxrave.simpmusic.viewModel.ImportViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.hsm_bring_music
import simpmusic.composeapp.generated.resources.hsm_import_hint
import simpmusic.composeapp.generated.resources.hsm_choose_file

@Composable
fun HsmLibraryImport(onImported: () -> Unit) {
    val viewModel: ImportViewModel = koinViewModel()
    val progress by viewModel.importState.collectAsStateWithLifecycle()
    val picker = filePickerResult { path -> path?.let { viewModel.import(Uri.parse(it)) } }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(stringResource(Res.string.hsm_bring_music), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(Res.string.hsm_import_hint),
            Modifier.padding(top = 8.dp, bottom = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = picker::launch, enabled = progress == null) {
            Text(stringResource(Res.string.hsm_choose_file), color = MaterialTheme.colorScheme.onPrimary)
        }
    }
    progress?.let { current ->
        ImportProgressDialog(current) {
            if (current is ImportProgress.Success) onImported()
            viewModel.dismiss()
        }
    }
}
