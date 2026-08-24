package com.xayah.databackup.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R
import com.xayah.databackup.feature.AdvancedSettingsRoute
import com.xayah.databackup.feature.DataMigrationRoute
import com.xayah.databackup.ui.component.LocalFloatingNavigationBarBottomPadding
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.PreferenceGroup
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.rememberFadingEdgeState
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.LogExportHelper
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AdvancedSettingsScreen(navigator: Navigator) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val floatingNavigationBarBottomPadding = LocalFloatingNavigationBarBottomPadding.current
    val scrollState = rememberScrollState()
    val fadingEdgeState = rememberFadingEdgeState(scrollState, label = "advanced-settings")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val exportLogLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                LogExportHelper.createLogsZip()?.let { zip ->
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            zip.inputStream().use { input -> input.copyTo(out) }
                        }
                    }
                    zip.delete()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.advanced_settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navigator.popBackStackSafely() }) {
                        Icon(
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
                .padding(top = innerPadding.calculateTopPadding())
                .verticalFadingEdges(fadingEdgeState)
                .verticalScroll(scrollState),
        ) {
            SectionHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = stringResource(R.string.advanced_settings),
            )
            PreferenceGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
                item {
                    Preference(
                        icon = ImageVector.vectorResource(R.drawable.ic_archive_restore),
                        title = stringResource(R.string.data_migration),
                        subtitle = stringResource(R.string.data_migration_desc),
                        onClick = { navigator.navigateSafely(DataMigrationRoute) },
                    )
                }
                item {
                    Preference(
                        icon = ImageVector.vectorResource(R.drawable.ic_archive),
                        title = stringResource(R.string.export_log),
                        subtitle = stringResource(R.string.export_log_desc),
                        onClick = {
                            exportLogLauncher.launch("DataBackup_logs_${System.currentTimeMillis()}.zip")
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding() + floatingNavigationBarBottomPadding))
        }
    }
}
