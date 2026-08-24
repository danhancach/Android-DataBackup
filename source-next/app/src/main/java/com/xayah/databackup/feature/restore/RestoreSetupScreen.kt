package com.xayah.databackup.feature.restore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.feature.RestoreAppsRoute
import com.xayah.databackup.feature.RestoreProcessRoute
import com.xayah.databackup.feature.RestoreSnapshotRoute
import com.xayah.databackup.ui.component.ActionButtonState
import com.xayah.databackup.ui.component.AutoScreenOffSwitch
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.PreferenceGroup
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.SmallCheckActionButton
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.ui.component.shimmer
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.ui.component.rememberCallLogPermissionsState
import com.xayah.databackup.ui.component.rememberContactPermissionsState
import com.xayah.databackup.ui.component.rememberMessagePermissionsState
import com.xayah.databackup.util.AppsOptionSelectedRestore
import com.xayah.databackup.util.CallLogsOptionSelectedRestore
import com.xayah.databackup.util.ContactsOptionSelectedRestore
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.MessagesOptionSelectedRestore
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.NetworksOptionSelectedRestore
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.popBackStackSafely
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun RestoreSetupScreen(
    navigator: Navigator,
    viewModel: RestoreSetupViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val selectedBackup by viewModel.selectedBackup.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle(null)
    val nextBtnEnabled by viewModel.nextBtnEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.initialize()
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.restore),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            modifier = Modifier.shimmer(selectedItems == null),
                            text = selectedItems?.let { stringResource(R.string.items_selected, it.first, it.second) } ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStackSafely() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.surfaceTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState),
            ) {
                SelectedBackupInfo(
                    backup = selectedBackup,
                    isLoading = isLoading,
                )
                SectionHeader(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.target),
                    color = MaterialTheme.colorScheme.primary,
                )
                RestoreTargetRow(navigator = navigator, viewModel = viewModel)
                SectionHeader(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.settings),
                    color = MaterialTheme.colorScheme.primary,
                )
                PreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
                    item { AutoScreenOffSwitch() }
                }
                Spacer(modifier = Modifier.height(0.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    modifier = Modifier.wrapContentSize(),
                    enabled = nextBtnEnabled,
                    onClick = {
                        viewModel.resetProcessRepo()
                        if (viewModel.isCurrentBackupRustic()) {
                            navigator.navigateSafely(RestoreSnapshotRoute)
                        } else {
                            navigator.navigateSafely(RestoreProcessRoute)
                        }
                    },
                ) {
                    Text(text = stringResource(R.string.next))
                }
            }
            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun RestoreTargetRow(
    navigator: Navigator,
    viewModel: RestoreSetupViewModel,
) {
    val appsItem by viewModel.appsItem.collectAsStateWithLifecycle(null)
    val networksItem by viewModel.networksItem.collectAsStateWithLifecycle(null)
    val contactsItem by viewModel.contactsItem.collectAsStateWithLifecycle(null)
    val callLogsItem by viewModel.callLogsItem.collectAsStateWithLifecycle(null)
    val messagesItem by viewModel.messagesItem.collectAsStateWithLifecycle(null)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SmallCheckActionButton(
                modifier = Modifier.weight(1f),
                checked = appsItem?.selected ?: false,
                icon = ImageVector.vectorResource(R.drawable.ic_layout_grid),
                title = stringResource(R.string.apps),
                titleShimmer = appsItem == null,
                subtitle = stringResource(
                    R.string.items_selected,
                    appsItem?.selections?.first ?: 0,
                    appsItem?.selections?.second ?: 0,
                ),
                subtitleShimmer = appsItem == null,
                onCheckedChange = {
                    viewModel.withLock(Dispatchers.Default) {
                        App.application.saveBoolean(AppsOptionSelectedRestore.first, it)
                    }
                },
            ) {
                navigator.navigateSafely(RestoreAppsRoute)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SmallCheckActionButton(
                modifier = Modifier.weight(1f),
                checked = networksItem?.selected ?: false,
                checkBoxVisible = (networksItem?.selections?.second ?: 0) > 0,
                icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                title = stringResource(R.string.networks),
                titleShimmer = networksItem == null,
                subtitle = stringResource(
                    R.string.items_selected,
                    networksItem?.selections?.first ?: 0,
                    networksItem?.selections?.second ?: 0,
                ),
                subtitleShimmer = networksItem == null,
                onCheckedChange = {
                    if ((networksItem?.selections?.second ?: 0) > 0) {
                        viewModel.withLock(Dispatchers.Default) {
                            App.application.saveBoolean(NetworksOptionSelectedRestore.first, it)
                        }
                    }
                },
            ) {}

            val contactsPermissionState = rememberContactPermissionsState()
            SmallCheckActionButton(
                modifier = Modifier.weight(1f),
                state = if (contactsPermissionState.allPermissionsGranted) ActionButtonState.NORMAL else ActionButtonState.ERROR,
                checked = contactsItem?.selected ?: false,
                checkBoxVisible = contactsPermissionState.allPermissionsGranted &&
                    (contactsItem?.selections?.second ?: 0) > 0,
                icon = ImageVector.vectorResource(R.drawable.ic_user_round),
                title = stringResource(R.string.contacts),
                titleShimmer = contactsItem == null,
                subtitle = if (contactsPermissionState.allPermissionsGranted) {
                    stringResource(
                        R.string.items_selected,
                        contactsItem?.selections?.first ?: 0,
                        contactsItem?.selections?.second ?: 0,
                    )
                } else {
                    stringResource(R.string.no_permissions)
                },
                subtitleShimmer = contactsItem == null,
                onCheckedChange = {
                    if (contactsPermissionState.allPermissionsGranted &&
                        (contactsItem?.selections?.second ?: 0) > 0
                    ) {
                        viewModel.withLock(Dispatchers.Default) {
                            App.application.saveBoolean(ContactsOptionSelectedRestore.first, it)
                        }
                    }
                },
            ) {
                if (contactsPermissionState.allPermissionsGranted.not()) {
                    contactsPermissionState.launchMultiplePermissionRequest()
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val callLogsPermissionState = rememberCallLogPermissionsState()
            SmallCheckActionButton(
                modifier = Modifier.weight(1f),
                state = if (callLogsPermissionState.allPermissionsGranted) ActionButtonState.NORMAL else ActionButtonState.ERROR,
                checked = callLogsItem?.selected ?: false,
                checkBoxVisible = callLogsPermissionState.allPermissionsGranted &&
                    (callLogsItem?.selections?.second ?: 0) > 0,
                icon = ImageVector.vectorResource(R.drawable.ic_phone),
                title = stringResource(R.string.call_logs),
                titleShimmer = callLogsItem == null,
                subtitle = if (callLogsPermissionState.allPermissionsGranted) {
                    stringResource(
                        R.string.items_selected,
                        callLogsItem?.selections?.first ?: 0,
                        callLogsItem?.selections?.second ?: 0,
                    )
                } else {
                    stringResource(R.string.no_permissions)
                },
                subtitleShimmer = callLogsItem == null,
                onCheckedChange = {
                    if (callLogsPermissionState.allPermissionsGranted &&
                        (callLogsItem?.selections?.second ?: 0) > 0
                    ) {
                        viewModel.withLock(Dispatchers.Default) {
                            App.application.saveBoolean(CallLogsOptionSelectedRestore.first, it)
                        }
                    }
                },
            ) {
                if (callLogsPermissionState.allPermissionsGranted.not()) {
                    callLogsPermissionState.launchMultiplePermissionRequest()
                }
            }

            val messagesPermissionState = rememberMessagePermissionsState()
            SmallCheckActionButton(
                modifier = Modifier.weight(1f),
                state = if (messagesPermissionState.allPermissionsGranted) ActionButtonState.NORMAL else ActionButtonState.ERROR,
                checked = messagesItem?.selected ?: false,
                checkBoxVisible = messagesPermissionState.allPermissionsGranted &&
                    (messagesItem?.selections?.second ?: 0) > 0,
                icon = ImageVector.vectorResource(R.drawable.ic_message_circle),
                title = stringResource(R.string.messages),
                titleShimmer = messagesItem == null,
                subtitle = if (messagesPermissionState.allPermissionsGranted) {
                    stringResource(
                        R.string.items_selected,
                        messagesItem?.selections?.first ?: 0,
                        messagesItem?.selections?.second ?: 0,
                    )
                } else {
                    stringResource(R.string.no_permissions)
                },
                subtitleShimmer = messagesItem == null,
                onCheckedChange = {
                    if (messagesPermissionState.allPermissionsGranted &&
                        (messagesItem?.selections?.second ?: 0) > 0
                    ) {
                        viewModel.withLock(Dispatchers.Default) {
                            App.application.saveBoolean(MessagesOptionSelectedRestore.first, it)
                        }
                    }
                },
            ) {
                if (messagesPermissionState.allPermissionsGranted.not()) {
                    messagesPermissionState.launchMultiplePermissionRequest()
                }
            }
        }
    }
}

@Composable
private fun SelectedBackupInfo(
    backup: BackupConfig?,
    isLoading: Boolean,
) {
    backup?.let { selectedBackup ->
        val rusticBackend = selectedBackup.backupBackend is BackupBackend.Rustic
        val relativePath = remember(selectedBackup.path) {
            PathHelper.getChildPath(selectedBackup.path).ifEmpty { selectedBackup.path }
        }
        PreferenceGroup(
            modifier = Modifier
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Preference(
                    icon = ImageVector.vectorResource(
                        if (rusticBackend) R.drawable.ic_database_backup else R.drawable.ic_archive,
                    ),
                    title = selectedBackup.displayName,
                    subtitle = stringResource(if (rusticBackend) R.string.rustic else R.string.archive),
                )
            }
            item {
                Preference(
                    icon = ImageVector.vectorResource(R.drawable.ic_map_pin),
                    title = stringResource(R.string.backup_dir),
                    subtitle = relativePath,
                    subtitleIcon = ImageVector.vectorResource(R.drawable.ic_folder),
                )
            }
        }
    } ?: Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            LoadingIndicator()
        } else {
            Text(
                text = stringResource(R.string.no_item_selected),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
