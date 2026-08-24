package com.xayah.databackup.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogActionButton
import com.xayah.databackup.ui.component.DialogDismissButton
import com.xayah.databackup.ui.component.DialogIcon
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.PreferenceGroup
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.Navigator
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupDirectoryScreen(
    navigator: Navigator,
    viewModel: BackupDirectoryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomPathDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(context = Dispatchers.Main) {
        viewModel.loadDirectories()
    }

    uiState.errorMessage?.let { message ->
        DataBackupDialog(
            title = stringResource(R.string.error),
            onDismissRequest = viewModel::dismissError,
            icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_circle_x)) },
            iconContainerColor = MaterialTheme.colorScheme.errorContainer,
            iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
            content = { Text(message) },
            confirmButton = {
                DialogActionButton(
                    text = stringResource(R.string.confirm),
                    onClick = viewModel::dismissError,
                )
            },
        )
    }

    if (showCustomPathDialog) {
        CustomBackupDirectoryDialog(
            initialPath = uiState.selectedPath,
            onDismissRequest = { showCustomPathDialog = false },
            onConfirm = { path ->
                viewModel.selectDirectory(path)
                showCustomPathDialog = false
            },
        )
    }

    SettingsSubScreen(
        title = stringResource(R.string.backup_dir),
        navigator = navigator,
    ) {
        SectionHeader(
            modifier = Modifier.padding(16.dp),
            title = stringResource(R.string.backup_dir),
        )
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.setup_backup_dir_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(24.dp))
        } else {
            PreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                uiState.directories.forEach { path ->
                    val selected = path == uiState.selectedPath
                    Preference(
                        icon = ImageVector.vectorResource(
                            if (selected) R.drawable.ic_check else R.drawable.ic_folder
                        ),
                        title = path,
                        subtitle = if (selected) stringResource(R.string.current) else "",
                        titleMaxLines = Int.MAX_VALUE,
                        titleOverflow = TextOverflow.Visible,
                        titleFontFamily = FontFamily.Monospace,
                        onClick = { viewModel.selectDirectory(path) },
                    )
                }
                Preference(
                    icon = ImageVector.vectorResource(R.drawable.ic_plus),
                    title = stringResource(R.string.custom_directory),
                    subtitle = stringResource(R.string.pick_custom_directory_desc),
                    onClick = { showCustomPathDialog = true },
                )
            }
        }
    }
}

@Composable
private fun CustomBackupDirectoryDialog(
    initialPath: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var path by rememberSaveable(initialPath) { mutableStateOf(initialPath) }
    var isError by rememberSaveable { mutableStateOf(initialPath.isBlank()) }

    DataBackupDialog(
        title = stringResource(R.string.choose_a_custom_path),
        onDismissRequest = onDismissRequest,
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_folder)) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = path,
                    onValueChange = {
                        path = it
                        isError = it.isBlank()
                    },
                    isError = isError,
                    shape = MaterialTheme.shapes.large,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    label = { Text(text = stringResource(R.string.backup_dir)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_folder),
                            contentDescription = null,
                        )
                    },
                    supportingText = if (isError) {
                        { Text(text = stringResource(R.string.required)) }
                    } else {
                        null
                    },
                )
            }
        },
        confirmButton = {
            DialogActionButton(
                text = stringResource(R.string.save),
                enabled = isError.not(),
                icon = ImageVector.vectorResource(R.drawable.ic_check),
                onClick = { onConfirm(path) },
            )
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.cancel),
                onClick = onDismissRequest,
            )
        },
    )
}
