package com.xayah.databackup.feature.restore.apps

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.xayah.databackup.ui.component.FadeVisibility
import com.xayah.databackup.ui.component.FilterButton
import com.xayah.databackup.ui.component.PreferenceGroupItemSpacing
import com.xayah.databackup.ui.component.PreferenceGroupListItem
import com.xayah.databackup.ui.component.PreferenceHorizontalPadding
import com.xayah.databackup.ui.component.PreferenceItemMinHeight
import com.xayah.databackup.ui.component.PreferenceItemVerticalPadding
import com.xayah.databackup.ui.component.SearchTextField
import com.xayah.databackup.ui.component.SelectableChip
import com.xayah.databackup.ui.component.filterButtonSecondaryColors
import com.xayah.databackup.ui.component.rememberFadingEdgeState
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.ui.material3.ModalDropdownMenu
import com.xayah.databackup.ui.material3.ModalDropdownMenuItem
import com.xayah.databackup.util.LaunchedEffect as IoLaunchedEffect
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.SortsSequence
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun RestoreAppsScreen(
    navigator: Navigator,
    viewModel: RestoreAppsViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val searchScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val allSelected by viewModel.allSelected.collectAsStateWithLifecycle()
    val sourcePath = remember { viewModel.getSourcePath() }
    val context = LocalContext.current
    val filterSheetState = rememberModalBottomSheetState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    var onSearch by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val normalLazyListState = rememberLazyListState()
    val searchLazyListState = rememberLazyListState()
    val activeLazyListState = if (onSearch) searchLazyListState else normalLazyListState
    val fadingEdgeState = rememberFadingEdgeState(activeLazyListState, label = "restoreApps")

    LaunchedEffect(onSearch) {
        if (onSearch) {
            searchLazyListState.scrollToItem(0)
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(if (onSearch) searchScrollBehavior.nestedScrollConnection else scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            AnimatedContent(onSearch) { target ->
                if (target) {
                    TopAppBar(
                        title = {
                            SearchTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp)
                                    .focusRequester(focusRequester),
                                value = searchText,
                                onClose = {
                                    onSearch = false
                                    viewModel.changeSearchText("")
                                },
                            ) { viewModel.changeSearchText(it) }
                        },
                        actions = {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_funnel),
                                    contentDescription = stringResource(R.string.filters),
                                )
                            }
                            RestoreSelectIconButton(viewModel = viewModel)
                        },
                        scrollBehavior = searchScrollBehavior,
                    )
                } else {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = stringResource(R.string.apps),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = stringResource(R.string.items_selected, allSelected, apps.size),
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
                            IconButton(onClick = { onSearch = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                                    contentDescription = stringResource(R.string.search),
                                )
                            }
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_funnel),
                                    contentDescription = stringResource(R.string.filters),
                                )
                            }
                            RestoreSelectIconButton(viewModel = viewModel)
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.surfaceTopAppBarColors(),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            AnimatedContent(targetState = apps.isEmpty()) { isAppsEmpty ->
                if (isAppsEmpty) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            modifier = Modifier.size(300.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.img_empty),
                            contentDescription = stringResource(R.string.it_is_empty),
                        )
                        Text(
                            text = stringResource(R.string.it_is_empty),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.verticalFadingEdges(fadingEdgeState),
                        state = activeLazyListState,
                        verticalArrangement = Arrangement.spacedBy(PreferenceGroupItemSpacing),
                        contentPadding = PaddingValues(
                            start = PreferenceHorizontalPadding,
                            end = PreferenceHorizontalPadding,
                            top = 8.dp,
                        ),
                    ) {
                        itemsIndexed(
                            items = apps,
                            key = { _, app -> app.dirName },
                        ) { index, app ->
                            PreferenceGroupListItem(
                                modifier = Modifier.animateItem(),
                                isFirstInGroup = index == 0,
                                isLastInGroup = index == apps.lastIndex,
                                onClick = { viewModel.selectAll(app.dirName, app.toggleableState) },
                            ) {
                                RestoreAppRowContent(
                                    context = context,
                                    sourcePath = sourcePath,
                                    app = app,
                                    viewModel = viewModel,
                                )
                            }
                        }

                        item(key = "-1") {
                            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = filterSheetState,
            ) {
                val sortSequence by viewModel.sortSequence.collectAsStateWithLifecycle()
                val selectedFirst by viewModel.selectedFirst.collectAsStateWithLifecycle()
                val filterUserApps by viewModel.filterUserApps.collectAsStateWithLifecycle()
                val filterSystemApps by viewModel.filterSystemApps.collectAsStateWithLifecycle()

                RestoreAppsFilterSheetContent(
                    modifier = Modifier.fillMaxWidth(),
                    sortSequence = sortSequence,
                    selectedFirst = selectedFirst,
                    filterUserApps = filterUserApps,
                    filterSystemApps = filterSystemApps,
                    onSequenceClick = { viewModel.changeSequence(sortSequence) },
                    onSelectedFirstClick = { viewModel.changeSelectedFirst(selectedFirst.not()) },
                    onFilterUserAppsClick = { viewModel.changeFilterUserApps(filterUserApps.not()) },
                    onFilterSystemAppsClick = { viewModel.changeFilterSystemApps(filterSystemApps.not()) },
                )
            }
        }
    }
}

@Composable
fun RestoreAppsFilterSheetContent(
    modifier: Modifier = Modifier,
    sortSequence: SortsSequence,
    selectedFirst: Boolean,
    filterUserApps: Boolean,
    filterSystemApps: Boolean,
    onSequenceClick: () -> Unit,
    onSelectedFirstClick: () -> Unit,
    onFilterUserAppsClick: () -> Unit,
    onFilterSystemAppsClick: () -> Unit,
) {
    val isAscending = sortSequence == SortsSequence.ASCENDING
    val animatedSequenceIcon = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animted_arrow_up_down_a_z),
        atEnd = isAscending,
    )

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(minSize = 80.dp),
        contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "sorts_header") {
            Text(
                text = stringResource(R.string.sorts),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item(key = "sort_sequence") {
            FilterButton(
                selected = true,
                title = if (isAscending) stringResource(R.string.ascending) else stringResource(R.string.descending),
                colors = filterButtonSecondaryColors(),
                icon = animatedSequenceIcon,
            ) {
                onSequenceClick()
            }
        }
        item(key = "sort_selected_first") {
            FilterButton(
                selected = selectedFirst,
                title = stringResource(R.string.selected_first),
                colors = filterButtonSecondaryColors(),
                icon = ImageVector.vectorResource(R.drawable.ic_square_check_big),
            ) {
                onSelectedFirstClick()
            }
        }
        item(key = "sort_a2z") {
            FilterButton(
                selected = true,
                title = stringResource(R.string.a2z),
                icon = ImageVector.vectorResource(R.drawable.ic_book_a),
                onClick = {},
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "sorts_divider") {
            HorizontalDivider()
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "filters_header") {
            Text(
                text = stringResource(R.string.filters),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item(key = "filter_user_apps") {
            FilterButton(
                selected = filterUserApps,
                title = stringResource(R.string.user_apps),
                icon = ImageVector.vectorResource(R.drawable.ic_resource_package),
            ) {
                onFilterUserAppsClick()
            }
        }
        item(key = "filter_system_apps") {
            FilterButton(
                selected = filterSystemApps,
                title = stringResource(R.string.system_apps),
                icon = ImageVector.vectorResource(R.drawable.ic_package_2),
            ) {
                onFilterSystemAppsClick()
            }
        }
    }
}

@Composable
private fun RestoreAppRowContent(
    context: Context,
    sourcePath: String,
    app: RestoreApp,
    viewModel: RestoreAppsViewModel,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(
            horizontal = PreferenceHorizontalPadding,
            vertical = PreferenceItemVerticalPadding,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PreferenceItemMinHeight - PreferenceItemVerticalPadding * 2),
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
                IoLaunchedEffect(context = Dispatchers.IO, app.packageName, app.dirName, sourcePath) {
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
            val animatedCheckIcon = rememberAnimatedVectorPainter(
                animatedImageVector = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_chevron_right_to_down),
                atEnd = expanded,
            )
            IconButton(onClick = { expanded = expanded.not() }) {
                Icon(
                    painter = animatedCheckIcon,
                    contentDescription = if (expanded) {
                        stringResource(R.string.collapsed)
                    } else {
                        stringResource(R.string.expand)
                    },
                )
            }
            TriStateCheckbox(
                state = app.toggleableState,
                onClick = { viewModel.selectAll(app.dirName, app.toggleableState) },
            )
        }
        FadeVisibility(expanded) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (app.hasApk) {
                    SelectableChip(
                        selected = app.option.apk,
                        icon = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_resource_package),
                        text = stringResource(R.string.apk),
                        onCheckedChange = { viewModel.selectApk(app.dirName, it.not()) },
                    )
                }
                if (app.hasInternalData) {
                    SelectableChip(
                        selected = app.option.internalData,
                        icon = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_user),
                        text = stringResource(R.string.internal_data),
                        onCheckedChange = { viewModel.selectInternalData(app.dirName, it.not()) },
                    )
                }
                if (app.hasExternalData) {
                    SelectableChip(
                        selected = app.option.externalData,
                        icon = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_database),
                        text = stringResource(R.string.external_data),
                        onCheckedChange = { viewModel.selectExternalData(app.dirName, it.not()) },
                    )
                }
                if (app.hasAdditionalData) {
                    SelectableChip(
                        selected = app.option.additionalData,
                        icon = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_gamepad_2),
                        text = stringResource(R.string.additional_data),
                        onCheckedChange = { viewModel.selectAdditionalData(app.dirName, it.not()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RestoreSelectIconButton(viewModel: RestoreAppsViewModel) {
    var mainExpanded by remember { mutableStateOf(false) }
    var customExpanded by remember { mutableStateOf(false) }

    val apkAllSelected by viewModel.apkAllSelected.collectAsStateWithLifecycle()
    val dataAllSelected by viewModel.dataAllSelected.collectAsStateWithLifecycle()
    val intDataAllSelected by viewModel.intDataAllSelected.collectAsStateWithLifecycle()
    val extDataAllSelected by viewModel.extDataAllSelected.collectAsStateWithLifecycle()
    val addlDataAllSelected by viewModel.addlDataAllSelected.collectAsStateWithLifecycle()

    Box {
        IconButton(onClick = { mainExpanded = mainExpanded.not() }) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_list_checks),
                contentDescription = stringResource(R.string.select_all),
            )
        }
        ModalDropdownMenu(
            expanded = mainExpanded,
            onDismissRequest = { mainExpanded = false },
        ) {
            ModalDropdownMenuItem(
                text = {
                    Text(
                        if (apkAllSelected.not()) {
                            stringResource(R.string.select_all_apk)
                        } else {
                            stringResource(R.string.unselect_all_apk)
                        },
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (apkAllSelected.not()) {
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        } else {
                            ImageVector.vectorResource(R.drawable.ic_square)
                        },
                        contentDescription = null,
                    )
                },
                onClick = { viewModel.selectAllApk() },
            )
            ModalDropdownMenuItem(
                text = {
                    Text(
                        if (dataAllSelected.not()) {
                            stringResource(R.string.select_all_data)
                        } else {
                            stringResource(R.string.unselect_all_data)
                        },
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (dataAllSelected.not()) {
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        } else {
                            ImageVector.vectorResource(R.drawable.ic_square)
                        },
                        contentDescription = null,
                    )
                },
                onClick = { viewModel.selectAllData() },
            )
            HorizontalDivider()
            ModalDropdownMenuItem(
                text = { Text(stringResource(R.string.custom_selection)) },
                trailingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_right),
                        contentDescription = stringResource(R.string.custom_selection),
                    )
                },
                onClick = {
                    mainExpanded = false
                    customExpanded = true
                },
            )
        }

        ModalDropdownMenu(
            expanded = customExpanded,
            onDismissRequest = { customExpanded = false },
        ) {
            ModalDropdownMenuItem(
                text = { Text(stringResource(R.string.word_return)) },
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                        contentDescription = stringResource(R.string.word_return),
                    )
                },
                onClick = {
                    mainExpanded = true
                    customExpanded = false
                },
            )
            HorizontalDivider()
            ModalDropdownMenuItem(
                text = {
                    Text(
                        if (apkAllSelected.not()) {
                            stringResource(R.string.select_all_apk)
                        } else {
                            stringResource(R.string.unselect_all_apk)
                        },
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (apkAllSelected.not()) {
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        } else {
                            ImageVector.vectorResource(R.drawable.ic_square)
                        },
                        contentDescription = null,
                    )
                },
                onClick = { viewModel.selectAllApk() },
            )
            ModalDropdownMenuItem(
                text = {
                    Text(
                        if (intDataAllSelected.not()) {
                            stringResource(R.string.select_all_int_data)
                        } else {
                            stringResource(R.string.unselect_all_int_data)
                        },
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (intDataAllSelected.not()) {
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        } else {
                            ImageVector.vectorResource(R.drawable.ic_square)
                        },
                        contentDescription = null,
                    )
                },
                onClick = { viewModel.selectAllIntData() },
            )
            ModalDropdownMenuItem(
                text = {
                    Text(
                        if (extDataAllSelected.not()) {
                            stringResource(R.string.select_all_ext_data)
                        } else {
                            stringResource(R.string.unselect_all_ext_data)
                        },
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (extDataAllSelected.not()) {
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        } else {
                            ImageVector.vectorResource(R.drawable.ic_square)
                        },
                        contentDescription = null,
                    )
                },
                onClick = { viewModel.selectAllExtData() },
            )
            ModalDropdownMenuItem(
                text = {
                    Text(
                        if (addlDataAllSelected.not()) {
                            stringResource(R.string.select_all_addl_data)
                        } else {
                            stringResource(R.string.unselect_all_addl_data)
                        },
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (addlDataAllSelected.not()) {
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        } else {
                            ImageVector.vectorResource(R.drawable.ic_square)
                        },
                        contentDescription = null,
                    )
                },
                onClick = { viewModel.selectAllAddlData() },
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
