package com.xayah.databackup.feature.restore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.xayah.databackup.R
import com.xayah.databackup.entity.BackupBackend
import com.xayah.databackup.entity.BackupConfig
import com.xayah.databackup.feature.RestoreSetupRoute
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun RestoreHubScreen(
    navigator: Navigator,
    viewModel: RestoreHubViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

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
                        Text(stringResource(R.string.restore))
                        Text(
                            text = stringResource(R.string.restore_your_data),
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
        when (val state = uiState) {
            RestoreHubUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }

            RestoreHubUiState.Empty -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.no_backups),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.no_backups_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is RestoreHubUiState.Content -> LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
                state = gridState,
                columns = GridCells.Adaptive(minSize = 320.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    SectionHeader(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.select_backup_to_restore),
                    )
                }
                itemsIndexed(
                    items = state.backups,
                    key = { _, backup -> backup.uuidString },
                ) { index, backup ->
                    RestoreBackupCard(
                        backup = backup,
                        onClick = {
                            viewModel.withLock(Dispatchers.IO) {
                                viewModel.selectBackup(index)
                            }
                            navigator.navigateSafely(RestoreSetupRoute)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RestoreBackupCard(
    backup: BackupConfig,
    onClick: () -> Unit,
) {
    val relativePath = remember(backup.path) {
        PathHelper.getChildPath(backup.path).ifEmpty { backup.path }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = ImageVector.vectorResource(
                    if (backup.backupBackend is BackupBackend.Rustic) R.drawable.ic_database_backup
                    else R.drawable.ic_archive,
                ),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
            Text(
                text = backup.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = relativePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
