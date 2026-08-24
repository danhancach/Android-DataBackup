package com.xayah.databackup.feature.migration

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun DataMigrationScreen(
    navigator: Navigator,
    viewModel: DataMigrationViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var shaInput by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { viewModel.export(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.parse(uri, shaInput.ifBlank { null }) }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.data_migration)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navigator.popBackStackSafely() }) {
                        androidx.compose.material3.Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.surfaceTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.data_migration_export_desc))
            Button(onClick = { scope.launch(Dispatchers.IO) { viewModel.loadApps() } }) {
                Text(stringResource(R.string.refresh))
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.apps) { app ->
                    RowItem(
                        label = app.label,
                        subtitle = app.dirName,
                        checked = uiState.selected.contains(app.dirName),
                        onCheckedChange = { viewModel.toggle(app.dirName) },
                    )
                }
            }
            Button(
                enabled = uiState.selected.isNotEmpty() && !uiState.isBusy,
                onClick = { exportLauncher.launch("DataBackup_migration.tar.zst") },
            ) {
                Text(stringResource(R.string.data_migration_export))
            }
            uiState.lastSha256?.let { sha ->
                Text(text = stringResource(R.string.migration_sha256, sha))
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = shaInput,
                onValueChange = { shaInput = it },
                label = { Text(stringResource(R.string.migration_sha256_input)) },
            )
            Button(
                enabled = !uiState.isBusy,
                onClick = { importLauncher.launch(arrayOf("application/octet-stream", "application/x-compressed-tar")) },
            ) {
                Text(stringResource(R.string.data_migration_import))
            }
            if (uiState.parsedApps.isNotEmpty()) {
                Text(text = stringResource(R.string.migration_parsed_apps, uiState.parsedApps.size))
                Button(onClick = { viewModel.importParsed() }) {
                    Text(stringResource(R.string.confirm))
                }
            }
            uiState.message?.let { Text(text = it) }
        }
    }
}

@Composable
private fun RowItem(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column {
            Text(text = label)
            Text(text = subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
