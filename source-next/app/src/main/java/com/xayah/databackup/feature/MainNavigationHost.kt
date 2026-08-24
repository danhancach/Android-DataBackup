package com.xayah.databackup.feature

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xayah.databackup.feature.backup.BackupLibraryScreen
import com.xayah.databackup.feature.dashboard.DashboardScreen
import com.xayah.databackup.feature.schedule.ScheduleScreen
import com.xayah.databackup.feature.settings.SettingsScreen
import com.xayah.databackup.ui.component.FloatingNavigationBar
import com.xayah.databackup.ui.component.FloatingNavigationItem
import com.xayah.databackup.ui.component.FloatingNavigationItems
import com.xayah.databackup.ui.component.LocalFloatingNavigationBarBottomPadding
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.navigateSafely

private const val TabAnimationDurationMillis = 400
private val MainNavigationItems = FloatingNavigationItems

private fun tabTransitionSpec(
    from: FloatingNavigationItem,
    to: FloatingNavigationItem,
): ContentTransform {
    val fromIndex = MainNavigationItems.indexOf(from)
    val toIndex = MainNavigationItems.indexOf(to)
    val tabAnimationSpec = tween<IntOffset>(
        durationMillis = TabAnimationDurationMillis,
        easing = EaseInOut,
    )
    return if (toIndex > fromIndex) {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tabAnimationSpec,
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tabAnimationSpec,
        )
    } else {
        slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tabAnimationSpec,
        ) togetherWith slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tabAnimationSpec,
        )
    }
}

@Composable
fun MainNavigationHost(
    navigator: Navigator,
    enableTabBackHandler: Boolean = true,
) {
    var selectedItem by rememberSaveable { mutableStateOf(FloatingNavigationItem.HOME) }
    val density = LocalDensity.current
    var floatingNavigationBarBottomPadding by remember { mutableStateOf(0.dp) }

    fun selectItem(item: FloatingNavigationItem) {
        if (selectedItem != item) {
            selectedItem = item
        }
    }

    BackHandler(
        enabled = enableTabBackHandler && selectedItem != FloatingNavigationItem.HOME,
    ) {
        selectItem(FloatingNavigationItem.HOME)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalFloatingNavigationBarBottomPadding provides floatingNavigationBarBottomPadding) {
            AnimatedContent(
                modifier = Modifier.fillMaxSize(),
                targetState = selectedItem,
                transitionSpec = { tabTransitionSpec(initialState, targetState) },
                label = "mainNavigationTabs",
            ) { item ->
                when (item) {
                    FloatingNavigationItem.HOME -> DashboardScreen(
                        navigator = navigator,
                        onShowBackups = { navigator.navigateSafely(BackupLibraryRoute) },
                    )
                    FloatingNavigationItem.BACKUP -> BackupLibraryScreen(navigator)
                    FloatingNavigationItem.SCHEDULE -> ScheduleScreen(navigator)
                    FloatingNavigationItem.SETTINGS -> SettingsScreen(navigator)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        val measuredPadding = with(density) { size.height.toDp() }
                        if (floatingNavigationBarBottomPadding != measuredPadding) {
                            floatingNavigationBarBottomPadding = measuredPadding
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                FloatingNavigationBar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    selectedItem = selectedItem,
                    onSelected = ::selectItem,
                )
            }
        }
    }
}
