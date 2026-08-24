package com.xayah.databackup.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.S3CloudConfig
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogActionButton
import com.xayah.databackup.ui.component.DialogDismissButton
import com.xayah.databackup.ui.component.DialogIcon
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.SelectablePreferenceGroup
import com.xayah.databackup.ui.component.SelectablePreferenceItemInfo
import com.xayah.databackup.ui.component.SwitchablePreference

@Composable
fun NewBackupDialog(
    viewModel: NewBackupViewModel,
    onDismissRequest: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backupBackend by viewModel.backupBackend.collectAsStateWithLifecycle()
    val selectedBackendIndex = when (backupBackend) {
        is BackupBackend.Archive -> 0
        is BackupBackend.Rustic -> 1
    }
    var showEditPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var showEditS3Dialog by rememberSaveable { mutableStateOf(false) }
    val dismissDialog = {
        viewModel.discardChanges()
        onDismissRequest()
    }

    uiState.saveError?.let { error ->
        SaveNewBackupErrorDialog(
            error = error,
            onDismissRequest = viewModel::dismissSaveError,
        )
    }

    (backupBackend as? BackupBackend.Rustic)?.let { rusticBackend ->
        if (showEditPasswordDialog) {
            EditNewBackupPasswordDialog(
                password = rusticBackend.password,
                onDismissRequest = { showEditPasswordDialog = false },
                onConfirm = { password ->
                    viewModel.changeRusticPassword(password)
                    showEditPasswordDialog = false
                },
            )
        }
        if (showEditS3Dialog && rusticBackend.storage.isCloud) {
            EditS3CloudConfigDialog(
                config = rusticBackend.storage.s3 ?: S3CloudConfig(
                    endpoint = "",
                    bucket = "",
                    accessKey = "",
                    secretKey = "",
                ),
                onDismissRequest = { showEditS3Dialog = false },
                onConfirm = { s3 ->
                    viewModel.configureRusticCloud(s3)
                    showEditS3Dialog = false
                },
            )
        }
    }

    DataBackupDialog(
        title = stringResource(R.string.new_backup),
        onDismissRequest = {
            if (uiState.isSaving.not()) {
                dismissDialog()
            }
        },
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_plus)) },
        content = {
            NewBackupBackendSelector(
                backupBackend = backupBackend,
                enabled = uiState.isSaving.not(),
                selectedIndex = selectedBackendIndex,
                onSelectedIndexChanged = {
                    showEditPasswordDialog = false
                    showEditS3Dialog = false
                    viewModel.selectBackupBackend(it)
                },
                onEditPassword = { showEditPasswordDialog = true },
                onToggleCloud = { enabled ->
                    viewModel.toggleRusticCloud(enabled)
                    if (enabled) {
                        showEditS3Dialog = true
                    }
                },
                onEditS3Config = { showEditS3Dialog = true },
            )
        },
        confirmButton = {
            Button(
                enabled = uiState.isSaving.not(),
                onClick = { viewModel.saveNewBackup(onSaved = onDismissRequest) },
            ) {
                if (uiState.isSaving) {
                    LoadingIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                        contentDescription = null,
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.cancel),
                enabled = uiState.isSaving.not(),
                onClick = dismissDialog,
            )
        },
    )
}

@Composable
private fun NewBackupBackendSelector(
    backupBackend: BackupBackend,
    enabled: Boolean,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    onEditPassword: () -> Unit,
    onToggleCloud: (Boolean) -> Unit,
    onEditS3Config: () -> Unit,
) {
    val items = listOf(
        SelectablePreferenceItemInfo(
            icon = ImageVector.vectorResource(R.drawable.ic_archive),
            title = stringResource(R.string.archive),
            subtitle = stringResource(R.string.archive_backup_backend_desc),
        ),
        SelectablePreferenceItemInfo(
            icon = ImageVector.vectorResource(R.drawable.ic_database_backup),
            title = stringResource(R.string.rustic),
            subtitle = stringResource(R.string.rustic_backup_backend_desc),
        ),
    )

    SelectablePreferenceGroup(
        enabled = enabled,
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChanged = onSelectedIndexChanged,
    ) {
        val rusticBackend = backupBackend as? BackupBackend.Rustic
        NewBackupPasswordPreference(
            password = rusticBackend?.password ?: BackupBackend.DEFAULT_PASSWORD,
            enabled = enabled && rusticBackend != null,
            onClick = onEditPassword,
        )
        rusticBackend?.let { rustic ->
            SwitchablePreference(
                enabled = enabled,
                checked = rustic.storage.isCloud,
                icon = ImageVector.vectorResource(R.drawable.ic_cloud_upload),
                title = stringResource(R.string.s3_cloud_storage),
                subtitle = stringResource(R.string.s3_cloud_storage_desc),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                onCheckedChange = onToggleCloud,
            )
            if (rustic.storage.isCloud) {
                val s3 = rustic.storage.s3
                Preference(
                    enabled = enabled,
                    icon = ImageVector.vectorResource(R.drawable.ic_settings),
                    title = stringResource(R.string.s3_configure),
                    subtitle = s3?.summaryOrNull() ?: stringResource(R.string.s3_not_configured),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = onEditS3Config,
                )
            }
        }
    }
}

@Composable
private fun NewBackupPasswordPreference(
    password: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    var showPassword by rememberSaveable(password) { mutableStateOf(false) }
    val togglePasswordDescription = stringResource(
        if (showPassword) R.string.hide_password else R.string.show_password
    )

    Preference(
        enabled = enabled,
        icon = ImageVector.vectorResource(R.drawable.ic_key_round),
        title = stringResource(R.string.password),
        subtitle = password.takeIf { showPassword } ?: HIDDEN_PASSWORD,
        slot = {
            IconButton(
                enabled = enabled,
                onClick = { showPassword = showPassword.not() },
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        if (showPassword) R.drawable.ic_eye_off else R.drawable.ic_eye
                    ),
                    contentDescription = togglePasswordDescription,
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun EditNewBackupPasswordDialog(
    password: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable(password) { mutableStateOf(password) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    DataBackupDialog(
        title = stringResource(R.string.password),
        onDismissRequest = onDismissRequest,
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_key_round)) },
        content = {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                label = { Text(text = stringResource(R.string.password)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val contentDescription = stringResource(
                        if (showPassword) R.string.hide_password else R.string.show_password
                    )
                    IconButton(onClick = { showPassword = showPassword.not() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (showPassword) R.drawable.ic_eye_off else R.drawable.ic_eye
                            ),
                            contentDescription = contentDescription,
                        )
                    }
                },
            )
        },
        confirmButton = {
            DialogActionButton(
                text = stringResource(R.string.save),
                icon = ImageVector.vectorResource(R.drawable.ic_check),
                onClick = { onConfirm(text) },
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

@Composable
private fun EditS3CloudConfigDialog(
    config: S3CloudConfig,
    onDismissRequest: () -> Unit,
    onConfirm: (S3CloudConfig) -> Unit,
) {
    var endpoint by rememberSaveable(config.endpoint) { mutableStateOf(config.endpoint) }
    var bucket by rememberSaveable(config.bucket) { mutableStateOf(config.bucket) }
    var accessKey by rememberSaveable(config.accessKey) { mutableStateOf(config.accessKey) }
    var secretKey by rememberSaveable(config.secretKey) { mutableStateOf(config.secretKey) }
    var region by rememberSaveable(config.region) { mutableStateOf(config.region) }
    var root by rememberSaveable(config.root) { mutableStateOf(config.root) }
    var allowInsecure by rememberSaveable(config.allowInsecure) { mutableStateOf(config.allowInsecure) }
    var showSecretKey by rememberSaveable { mutableStateOf(false) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    DataBackupDialog(
        title = stringResource(R.string.s3_configure),
        onDismissRequest = onDismissRequest,
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_cloud_upload)) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = endpoint,
                    onValueChange = {
                        endpoint = it
                        validationError = null
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    label = { Text(text = stringResource(R.string.s3_endpoint)) },
                    placeholder = { Text(text = stringResource(R.string.s3_endpoint_hint)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = bucket,
                    onValueChange = {
                        bucket = it
                        validationError = null
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    label = { Text(text = stringResource(R.string.s3_bucket)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = accessKey,
                    onValueChange = {
                        accessKey = it
                        validationError = null
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    label = { Text(text = stringResource(R.string.s3_access_key)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = secretKey,
                    onValueChange = {
                        secretKey = it
                        validationError = null
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    label = { Text(text = stringResource(R.string.s3_secret_key)) },
                    visualTransformation = if (showSecretKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val contentDescription = stringResource(
                            if (showSecretKey) R.string.hide_password else R.string.show_password
                        )
                        IconButton(onClick = { showSecretKey = showSecretKey.not() }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(
                                    if (showSecretKey) R.drawable.ic_eye_off else R.drawable.ic_eye
                                ),
                                contentDescription = contentDescription,
                            )
                        }
                    },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = region,
                    onValueChange = { region = it },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    label = { Text(text = stringResource(R.string.s3_region)) },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = root,
                    onValueChange = { root = it },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    label = { Text(text = stringResource(R.string.s3_root)) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.s3_allow_insecure),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.s3_allow_insecure_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = allowInsecure,
                        onCheckedChange = {
                            allowInsecure = it
                            validationError = null
                        },
                    )
                }
                validationError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = stringResource(R.string.save),
                icon = ImageVector.vectorResource(R.drawable.ic_check),
                onClick = {
                    val draft = S3CloudConfig(
                        endpoint = endpoint.trim(),
                        bucket = bucket.trim(),
                        accessKey = accessKey.trim(),
                        secretKey = secretKey,
                        region = region.trim(),
                        root = root.trim().ifBlank { "databackup" },
                        allowInsecure = allowInsecure,
                    )
                    validationError = when {
                        draft.isConfigured().not() -> context.getString(R.string.s3_validation_missing)
                        draft.allowInsecure.not() && draft.endpoint.startsWith("http://", ignoreCase = true) -> {
                            context.getString(R.string.s3_validation_http)
                        }
                        else -> null
                    }
                    if (validationError == null) {
                        onConfirm(draft)
                    }
                },
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

@Composable
private fun SaveNewBackupErrorDialog(
    error: String,
    onDismissRequest: () -> Unit,
) {
    DataBackupDialog(
        title = stringResource(R.string.error),
        onDismissRequest = onDismissRequest,
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_circle_x)) },
        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.save_backup_failed))
                if (error.isNotBlank()) {
                    Text(error)
                }
            }
        },
        confirmButton = {
            DialogActionButton(
                text = stringResource(R.string.confirm),
                onClick = onDismissRequest,
            )
        },
    )
}
