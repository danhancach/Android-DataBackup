package com.xayah.databackup.feature.restore

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.data.ProcessItem
import com.xayah.databackup.ui.component.BackupProgressHeader
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogDestructiveButton
import com.xayah.databackup.ui.component.DialogDismissButton
import com.xayah.databackup.ui.component.DialogIcon
import com.xayah.databackup.ui.component.ProcessItemCard
import com.xayah.databackup.ui.component.ProcessItemHolder
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun RestoreProcessScreen(
    navigator: Navigator,
    viewModel: RestoreProcessViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val overallProgress by viewModel.overallProgress.collectAsStateWithLifecycle()
    val appsItem by viewModel.appsItem.collectAsStateWithLifecycle()
    val networksItem by viewModel.networksItem.collectAsStateWithLifecycle()
    val contactsItem by viewModel.contactsItem.collectAsStateWithLifecycle()
    val callLogsItem by viewModel.callLogsItem.collectAsStateWithLifecycle()
    val messagesItem by viewModel.messagesItem.collectAsStateWithLifecycle()
    var openConfirmExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.loadProcessItems()
    }

    val onBack = remember {
        {
            if (uiState.canBeCanceled) {
                openConfirmExitDialog = true
            } else {
                navigator.popBackStackSafely()
            }
        }
    }

    BackHandler { onBack() }

    val statusLabel = when (uiState.status) {
        RestoreProcessStatus.Canceling -> stringResource(R.string.processing)
        RestoreProcessStatus.Canceled -> stringResource(R.string.canceled)
        RestoreProcessStatus.Processing -> stringResource(R.string.restoring)
        RestoreProcessStatus.Finished -> stringResource(R.string.finished)
    }
    val actionLabel = when (uiState.status) {
        RestoreProcessStatus.Canceling -> stringResource(R.string.processing)
        RestoreProcessStatus.Processing -> stringResource(R.string.cancel)
        else -> stringResource(R.string.finish)
    }

    if (openConfirmExitDialog) {
        DataBackupDialog(
            title = stringResource(R.string.cancel),
            onDismissRequest = { openConfirmExitDialog = false },
            icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info)) },
            content = { Text(stringResource(R.string.prompt_cancel_operation)) },
            confirmButton = {
                DialogDestructiveButton(
                    text = stringResource(R.string.cancel),
                    onClick = {
                        if (uiState.canBeCanceled) viewModel.cancel()
                        openConfirmExitDialog = false
                    },
                )
            },
            dismissButton = {
                DialogDismissButton(
                    text = stringResource(R.string.back),
                    onClick = { openConfirmExitDialog = false },
                )
            },
        )
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            BackupProgressHeader(
                progress = overallProgress,
                statusLabel = statusLabel,
                showLoading = uiState.isProcessing,
            )
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .verticalFadingEdges(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RestoreProcessCategoryItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_layout_grid),
                    title = stringResource(R.string.apps),
                    item = appsItem,
                    showProgress = uiState.status != RestoreProcessStatus.Canceling &&
                        uiState.status != RestoreProcessStatus.Canceled,
                )
                RestoreProcessCategoryItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_wifi),
                    title = stringResource(R.string.networks),
                    item = networksItem,
                    showProgress = uiState.status != RestoreProcessStatus.Canceling &&
                        uiState.status != RestoreProcessStatus.Canceled,
                )
                RestoreProcessCategoryItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_user_round),
                    title = stringResource(R.string.contacts),
                    item = contactsItem,
                    showProgress = uiState.status != RestoreProcessStatus.Canceling &&
                        uiState.status != RestoreProcessStatus.Canceled,
                )
                RestoreProcessCategoryItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_phone),
                    title = stringResource(R.string.call_logs),
                    item = callLogsItem,
                    showProgress = uiState.status != RestoreProcessStatus.Canceling &&
                        uiState.status != RestoreProcessStatus.Canceled,
                )
                RestoreProcessCategoryItem(
                    icon = ImageVector.vectorResource(R.drawable.ic_message_circle),
                    title = stringResource(R.string.messages),
                    item = messagesItem,
                    showProgress = uiState.status != RestoreProcessStatus.Canceling &&
                        uiState.status != RestoreProcessStatus.Canceled,
                )
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
                    enabled = uiState.isCanceling.not(),
                    onClick = onBack,
                ) {
                    AnimatedContent(
                        targetState = actionLabel,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "restoreActionLabel",
                    ) { label ->
                        Text(text = label)
                    }
                }
            }
            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun RestoreProcessCategoryItem(
    icon: ImageVector,
    title: String,
    item: ProcessItem,
    showProgress: Boolean = true,
) {
    if (item.isSelected.not()) return
    ProcessItemHolder(
        modifier = Modifier.fillMaxWidth(),
        process = { item.progress },
        showProgress = showProgress && item.isLoading.not(),
    ) {
        ProcessItemCard(
            icon = icon,
            title = title,
            currentIndex = item.currentIndex,
            totalCount = item.totalCount,
            subtitle = item.msg,
            subtitleShimmer = item.isLoading,
            onIconBtnClick = null,
            onClick = {},
        )
    }
}
