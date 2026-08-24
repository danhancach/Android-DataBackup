package com.xayah.databackup.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val DisabledOpacity = 0.38f

val LocalInPreferenceGroup = compositionLocalOf { false }

// SettingsLib expressive tokens (dimens_expressive.xml / SettingsSpace / SettingsRadius)
val PreferenceGroupCornerRadius = 20.dp
val PreferenceGroupCornerShape = RoundedCornerShape(PreferenceGroupCornerRadius)
val PreferenceGroupItemSpacing = 2.dp
val PreferenceItemCornerRadius = 4.dp
val PreferenceItemShape = RoundedCornerShape(PreferenceItemCornerRadius)
val PreferenceItemMinHeight = 72.dp
val PreferenceHorizontalPadding = 16.dp
val PreferenceItemVerticalPadding = 12.dp
val PreferenceIconContainerSize = 40.dp
val PreferenceIconSize = 24.dp
val PreferenceIconSpacing = 12.dp
val PreferenceDividerStartPadding = 52.dp

fun preferenceGroupItemShape(
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
): RoundedCornerShape {
    val top = if (isFirstInGroup) PreferenceGroupCornerRadius else 0.dp
    val bottom = if (isLastInGroup) PreferenceGroupCornerRadius else 0.dp
    return RoundedCornerShape(
        topStart = top,
        topEnd = top,
        bottomEnd = bottom,
        bottomStart = bottom,
    )
}

class PreferenceGroupScope internal constructor() {
    internal val items = mutableListOf<@Composable () -> Unit>()

    fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}

@Composable
fun PreferenceDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = PreferenceDividerStartPadding),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    )
}

@Composable
fun PreferenceGroupSurface(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    shape: Shape = PreferenceGroupCornerShape,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = shape,
        color = color,
        onClick = { onClick?.invoke() },
    ) {
        CompositionLocalProvider(LocalInPreferenceGroup provides true) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content,
            )
        }
    }
}

@Composable
fun PreferenceGroupListItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    val clipShape = preferenceGroupItemShape(isFirstInGroup, isLastInGroup)
    Box(modifier = modifier.clip(clipShape)) {
        CompositionLocalProvider(LocalInPreferenceGroup provides true) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                color = Color.Transparent,
                onClick = { onClick?.invoke() },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor, PreferenceItemShape),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun PreferenceGroup(
    modifier: Modifier = Modifier,
    content: @Composable PreferenceGroupScope.() -> Unit,
) {
    val scope = remember { PreferenceGroupScope() }
    scope.items.clear()
    scope.content()
    CompositionLocalProvider(LocalInPreferenceGroup provides true) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(PreferenceGroupCornerShape),
            verticalArrangement = Arrangement.spacedBy(PreferenceGroupItemSpacing),
        ) {
            scope.items.forEach { item ->
                item()
            }
        }
    }
}

@Composable
private fun RowScope.PreferenceIcon(
    enabled: Boolean,
    icon: ImageVector,
    alignWithSingleLineText: Boolean,
) {
    val animatedIconColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = DisabledOpacity)
        },
        label = "animatedIconColor",
    )
    Box(
        modifier = Modifier
            .size(PreferenceIconContainerSize)
            .align(
                if (alignWithSingleLineText) {
                    Alignment.CenterVertically
                } else {
                    Alignment.Top
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(PreferenceIconSize),
            tint = animatedIconColor,
            imageVector = icon,
            contentDescription = null,
        )
    }
}

@Composable
fun Preference(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector,
    title: String,
    subtitle: String,
    subtitleShimmer: Boolean = false,
    subtitleIcon: ImageVector? = null,
    titleMaxLines: Int = 1,
    titleOverflow: TextOverflow = TextOverflow.Ellipsis,
    titleFontFamily: FontFamily? = null,
    subtitleMaxLines: Int = Int.MAX_VALUE,
    subtitleOverflow: TextOverflow = TextOverflow.Clip,
    subtitleFontFamily: FontFamily? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    slot: @Composable (RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val inGroup = LocalInPreferenceGroup.current
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
    val alignWithSingleLineText = titleMaxLines == 1 && subtitleMaxLines == 1
    val rowBackgroundColor = when {
        inGroup -> MaterialTheme.colorScheme.surfaceContainer
        else -> containerColor
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        color = Color.Transparent,
        shape = RectangleShape,
        onClick = { onClick?.invoke() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PreferenceItemMinHeight)
                .then(
                    if (inGroup) {
                        Modifier.background(rowBackgroundColor, PreferenceItemShape)
                    } else {
                        Modifier.background(rowBackgroundColor)
                    },
                )
                .padding(
                    start = PreferenceHorizontalPadding,
                    end = PreferenceHorizontalPadding,
                    top = PreferenceItemVerticalPadding,
                    bottom = PreferenceItemVerticalPadding,
                ),
            verticalAlignment = if (alignWithSingleLineText) {
                Alignment.CenterVertically
            } else {
                Alignment.Top
            },
        ) {
            PreferenceIcon(
                enabled = enabled,
                icon = icon,
                alignWithSingleLineText = alignWithSingleLineText,
            )
            Spacer(modifier = Modifier.width(PreferenceIconSpacing))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = titleFontFamily,
                    maxLines = titleMaxLines,
                    overflow = titleOverflow,
                    color = animatedTitleColor,
                )
                if (subtitle.isNotBlank()) {
                    if (subtitleIcon == null) {
                        Text(
                            modifier = Modifier.shimmer(subtitleShimmer),
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = subtitleFontFamily,
                            maxLines = subtitleMaxLines,
                            overflow = subtitleOverflow,
                            color = animatedSubtitleColor,
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shimmer(subtitleShimmer),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = subtitleIcon,
                                contentDescription = null,
                                tint = animatedSubtitleColor,
                            )
                            Text(
                                modifier = Modifier.weight(1f),
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = subtitleFontFamily,
                                color = animatedSubtitleColor,
                                maxLines = subtitleMaxLines,
                                overflow = subtitleOverflow,
                            )
                        }
                    }
                }
            }
            slot?.invoke(this)
        }
    }
}

@Composable
fun SwitchablePreference(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    checked: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    Preference(
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        title = title,
        subtitle = subtitle,
        containerColor = containerColor,
        slot = {
            Switch(
                enabled = enabled,
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) {
                            ImageVector.vectorResource(R.drawable.ic_check)
                        } else {
                            ImageVector.vectorResource(R.drawable.ic_x)
                        },
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        },
    ) {
        onCheckedChange?.invoke(checked.not())
    }
}

@Composable
fun SwitchablePreference(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    dataStorePair: Pair<Preferences.Key<Boolean>, Boolean>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val option by context.readBoolean(dataStorePair).collectAsStateWithLifecycle(initialValue = dataStorePair.second)

    SwitchablePreference(
        modifier = modifier,
        enabled = enabled,
        checked = option,
        icon = icon,
        title = title,
        subtitle = subtitle,
        containerColor = containerColor,
    ) {
        scope.launch(Dispatchers.Default) {
            context.saveBoolean(dataStorePair.first, option.not())
        }
    }
}
