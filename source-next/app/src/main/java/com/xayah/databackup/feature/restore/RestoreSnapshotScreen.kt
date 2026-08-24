package com.xayah.databackup.feature.restore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.feature.RusticRestoreProcessRoute
import com.xayah.databackup.ui.component.SelectablePreferenceGroup
import com.xayah.databackup.ui.component.SelectablePreferenceItemInfo
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.TimeHelper
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun RestoreSnapshotScreen(
    navigator: Navigator,
    viewModel: RestoreSnapshotViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()
    val selectedIndex by viewModel.selectedIndex.collectAsStateWithLifecycle()
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
                    Text(
                        text = stringResource(R.string.select_snapshot_to_restore),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
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
                SectionHeader(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.snapshot),
                    color = MaterialTheme.colorScheme.primary,
                )
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingIndicator()
                        }
                    }
                    snapshots.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.no_snapshot_yet),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    else -> {
                        SelectablePreferenceGroup(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            selectedIndex = selectedIndex,
                            items = snapshots.map { snapshot ->
                                val timeLabel = if (snapshot.time > 0L) {
                                    TimeHelper.formatTimestampInShort(snapshot.time)
                                } else {
                                    snapshot.id
                                }
                                SelectablePreferenceItemInfo(
                                    icon = ImageVector.vectorResource(R.drawable.ic_database_backup),
                                    title = timeLabel,
                                    subtitle = snapshot.tags.joinToString(", ").ifBlank { snapshot.id },
                                )
                            },
                            onSelectedIndexChanged = viewModel::selectSnapshot,
                        )
                    }
                }
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
                    onClick = { navigator.navigateSafely(RusticRestoreProcessRoute) },
                ) {
                    Text(text = stringResource(R.string.next))
                }
            }
            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
        }
    }
}
