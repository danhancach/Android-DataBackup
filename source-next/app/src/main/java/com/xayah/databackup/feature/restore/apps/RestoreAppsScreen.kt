package com.xayah.databackup.feature.restore.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
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
import com.xayah.databackup.entity.RestoreApp
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.popBackStackSafely
import org.koin.androidx.compose.koinViewModel

@Composable
fun RestoreAppsScreen(
    navigator: Navigator,
    viewModel: RestoreAppsViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.apps))
                        Text(
                            text = stringResource(R.string.items_selected, selectedCount, apps.size),
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
                actions = {
                    IconButton(onClick = { viewModel.selectAll(selectedCount < apps.size) }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_list_checks),
                            contentDescription = stringResource(R.string.batch_select),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.surfaceTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                bottom = innerPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            items(apps, key = { it.dirName }) { app ->
                RestoreAppRow(
                    app = app,
                    onCheckedChange = { viewModel.toggleSelection(app.dirName, it) },
                )
            }
        }
    }
}

@Composable
private fun RestoreAppRow(
    app: RestoreApp,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = app.selected,
            onCheckedChange = onCheckedChange,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
