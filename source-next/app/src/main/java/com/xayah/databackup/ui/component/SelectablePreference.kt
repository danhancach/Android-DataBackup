package com.xayah.databackup.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val DisabledOpacity = 0.38f

data class SelectablePreferenceItemInfo(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

@Composable
fun SelectablePreferenceGroup(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedIndex: Int,
    items: List<SelectablePreferenceItemInfo>,
    onSelectedIndexChanged: (Int) -> Unit,
    content: @Composable PreferenceGroupScope.() -> Unit = {},
) {
    val extraScope = remember { PreferenceGroupScope() }
    extraScope.items.clear()
    extraScope.content()
    CompositionLocalProvider(LocalInPreferenceGroup provides true) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(PreferenceGroupCornerShape),
            verticalArrangement = Arrangement.spacedBy(PreferenceGroupItemSpacing),
        ) {
            items.forEachIndexed { index, preferenceItem ->
                val selected = index == selectedIndex
                val backgroundColor = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    color = Color.Transparent,
                    onClick = { onSelectedIndexChanged(index) },
                ) {
                    SelectablePreferenceItemContent(
                        enabled = enabled,
                        icon = preferenceItem.icon,
                        title = preferenceItem.title,
                        subtitle = preferenceItem.subtitle,
                        backgroundColor = backgroundColor,
                    )
                }
            }
            extraScope.items.forEach { item ->
                item()
            }
        }
    }
}

@Composable
private fun SelectablePreferenceItemContent(
    enabled: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    backgroundColor: Color,
) {
    val animatedIconColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = DisabledOpacity)
        },
        label = "animatedIconColor",
    )
    val animatedTitleColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledOpacity)
        },
        label = "animatedTitleColor",
    )
    val animatedSubtitleColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(DisabledOpacity)
        },
        label = "animatedSubtitleColor",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PreferenceItemMinHeight)
            .background(backgroundColor, PreferenceItemShape)
            .padding(
                start = PreferenceHorizontalPadding,
                end = PreferenceHorizontalPadding,
                top = PreferenceItemVerticalPadding,
                bottom = PreferenceItemVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(PreferenceIconContainerSize),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(PreferenceIconSize),
                tint = animatedIconColor,
                imageVector = icon,
                contentDescription = null,
            )
        }
        Spacer(modifier = Modifier.width(PreferenceIconSpacing))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = animatedTitleColor,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = animatedSubtitleColor,
            )
        }
    }
}
