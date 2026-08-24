package com.xayah.databackup.feature.restore.apps

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.xayah.databackup.R
import com.xayah.databackup.entity.RestoreApp
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun RestoreAppsScreen(
    navigator: Navigator,
    viewModel: RestoreAppsViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()
    val sourcePath = remember { viewModel.getSourcePath() }
    val context = LocalContext.current

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
                    context = context,
                    sourcePath = sourcePath,
                    app = app,
                    onToggle = { viewModel.toggleSelection(app.dirName, !app.selected) },
                )
            }
        }
    }
}

@Composable
private fun RestoreAppRow(
    context: Context,
    sourcePath: String,
    app: RestoreApp,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.ic_launcher_background)),
                contentAlignment = Alignment.Center,
            ) {
                var icon: Drawable? by remember { mutableStateOf(null) }
                LaunchedEffect(context = Dispatchers.IO, app.packageName, app.dirName, sourcePath) {
                    icon = loadRestoreAppIcon(context, sourcePath, app)
                }
                AsyncImage(
                    modifier = Modifier.size(36.dp),
                    model = ImageRequest.Builder(context)
                        .data(icon)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                )
            }
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
            Checkbox(
                checked = app.selected,
                onCheckedChange = null,
            )
        }
    }
}

private suspend fun loadRestoreAppIcon(
    context: Context,
    sourcePath: String,
    app: RestoreApp,
): Drawable {
    val packageManager = context.packageManager
    val installedIcon = runCatching { packageManager.getApplicationIcon(app.packageName) }.getOrNull()
    if (installedIcon != null) return installedIcon

    if (app.hasApk && sourcePath.isNotBlank()) {
        val apkPath = PathHelper.getBackupAppsApkFilePath(sourcePath, app.dirName)
        if (RemoteRootService.exists(apkPath)) {
            val archiveIcon = runCatching {
                packageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES)
                    ?.applicationInfo
                    ?.let { applicationInfo ->
                        applicationInfo.sourceDir = apkPath
                        applicationInfo.publicSourceDir = apkPath
                        packageManager.getApplicationIcon(applicationInfo)
                    }
            }.getOrNull()
            if (archiveIcon != null) return archiveIcon
        }
    }

    return AppCompatResources.getDrawable(context, android.R.drawable.sym_def_app_icon)
        ?: packageManager.defaultActivityIcon
}
