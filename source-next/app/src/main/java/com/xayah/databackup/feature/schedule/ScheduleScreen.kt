package com.xayah.databackup.feature.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogActionButton
import com.xayah.databackup.ui.component.DialogDismissButton
import com.xayah.databackup.ui.component.DialogIcon
import com.xayah.databackup.ui.component.LocalFloatingNavigationBarBottomPadding
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.PreferenceGroup
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.SwitchablePreference
import com.xayah.databackup.ui.component.rememberFadingEdgeState
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.TimeHelper
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun ScheduleScreen(
    navigator: Navigator,
    viewModel: ScheduleViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val floatingNavigationBarBottomPadding = LocalFloatingNavigationBarBottomPadding.current
    val scrollState = rememberScrollState()
    val fadingEdgeState = rememberFadingEdgeState(scrollState, label = "schedule")
    var openTimeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (openTimeDialog) {
        ScheduleTimeDialog(
            hour = uiState.schedule.hour,
            minute = uiState.schedule.minute,
            onDismissRequest = { openTimeDialog = false },
            onConfirm = { hour, minute ->
                viewModel.updateTime(hour, minute)
                openTimeDialog = false
            },
        )
    }

    val timeLabel = formatScheduleTime(uiState.schedule.hour, uiState.schedule.minute)
    val nextRunText = when {
        uiState.schedule.enabled.not() -> stringResource(R.string.schedule_next_run_disabled)
        uiState.hasSelectedBackup.not() -> stringResource(R.string.schedule_no_backup_selected)
        else -> {
            val nextRunAt = uiState.schedule.nextRunAt ?: 0L
            stringResource(R.string.schedule_next_run, TimeHelper.formatTimestampInShort(nextRunAt))
        }
    }
    val lastRunText = if (uiState.schedule.lastRunAt > 0L) {
        TimeHelper.formatTimestampInShort(uiState.schedule.lastRunAt)
    } else {
        stringResource(R.string.schedule_last_run_never)
    }
    val backupTargetSubtitle = if (uiState.hasSelectedBackup) {
        uiState.backupTargetTitle
    } else {
        stringResource(R.string.schedule_no_backup_selected)
    }
    val scheduleConfigured = uiState.isLoaded && uiState.schedule.enabled

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.schedule),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = TopAppBarDefaults.surfaceTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalFadingEdges(fadingEdgeState)
                .verticalScroll(scrollState),
        ) {
            SectionHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = stringResource(R.string.configure_automatic_backups),
            )
            PreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
                item {
                    SwitchablePreference(
                        enabled = uiState.isLoaded,
                        checked = uiState.schedule.enabled,
                        icon = ImageVector.vectorResource(R.drawable.ic_calendar_check),
                        title = stringResource(R.string.schedule_enabled),
                        subtitle = stringResource(R.string.schedule_enabled_desc),
                        onCheckedChange = viewModel::setEnabled,
                    )
                }
                item {
                    Preference(
                        enabled = scheduleConfigured,
                        icon = ImageVector.vectorResource(R.drawable.ic_clock),
                        title = stringResource(R.string.schedule_time),
                        subtitle = timeLabel,
                        onClick = { openTimeDialog = true },
                    )
                }
                item {
                    Preference(
                        enabled = scheduleConfigured,
                        icon = ImageVector.vectorResource(R.drawable.ic_archive),
                        title = stringResource(R.string.schedule_backup_target),
                        subtitle = backupTargetSubtitle,
                    )
                }
                item {
                    Preference(
                        enabled = uiState.isLoaded,
                        icon = ImageVector.vectorResource(R.drawable.ic_clock_arrow_up),
                        title = stringResource(R.string.schedule_next_run_title),
                        subtitle = nextRunText,
                    )
                }
                item {
                    Preference(
                        enabled = uiState.isLoaded,
                        icon = ImageVector.vectorResource(R.drawable.ic_clock_plus),
                        title = stringResource(R.string.schedule_last_run),
                        subtitle = lastRunText,
                    )
                }
            }
            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding() + floatingNavigationBarBottomPadding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimeDialog(
    hour: Int,
    minute: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val timeState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true,
    )

    DataBackupDialog(
        title = stringResource(R.string.schedule_time),
        onDismissRequest = onDismissRequest,
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_clock)) },
        content = {
            TimePicker(
                modifier = Modifier.fillMaxWidth(),
                state = timeState,
            )
        },
        confirmButton = {
            DialogActionButton(
                text = stringResource(R.string.save),
                icon = ImageVector.vectorResource(R.drawable.ic_check),
                onClick = { onConfirm(timeState.hour, timeState.minute) },
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

private fun formatScheduleTime(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
