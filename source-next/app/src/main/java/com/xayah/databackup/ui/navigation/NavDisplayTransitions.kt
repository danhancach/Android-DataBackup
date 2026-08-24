package com.xayah.databackup.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

private val navSpringSpec = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)

fun navForwardTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = navSpringSpec,
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = navSpringSpec,
    )
}

fun navPopTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = navSpringSpec,
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = navSpringSpec,
    )
}

fun navPredictivePopTransitionSpec():
    AnimatedContentTransitionScope<Scene<NavKey>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = {
    // Disable drag-to-preview: back only commits on gesture completion.
    EnterTransition.None togetherWith ExitTransition.None
}
