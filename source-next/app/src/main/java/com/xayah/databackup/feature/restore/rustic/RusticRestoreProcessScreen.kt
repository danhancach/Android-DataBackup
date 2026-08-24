package com.xayah.databackup.feature.restore.rustic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun RusticRestoreProcessScreen(
    navigator: Navigator,
    viewModel: RusticRestoreProcessViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(context = Dispatchers.IO, null) {
        viewModel.start()
    }

    val leave = {
        if (uiState.isProcessing) viewModel.cancel() else navigator.popBackStackSafely()
    }
    BackHandler(onBack = leave)

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.restoring)) },
                navigationIcon = {
                    IconButton(onClick = leave) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.size(32.dp))
            Text(
                text = when (uiState.stage) {
                    RusticRestoreStage.PrepareSnapshot -> stringResource(R.string.preprocessing)
                    RusticRestoreStage.RestoreApps -> stringResource(R.string.restoring)
                    RusticRestoreStage.Finished -> stringResource(R.string.restore_completed)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            if (uiState.isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
