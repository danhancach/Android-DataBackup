package com.xayah.databackup.entity

import androidx.compose.ui.state.ToggleableState

data class RestoreOption(
    var apk: Boolean = true,
    var internalData: Boolean = true,
    var externalData: Boolean = true,
    var additionalData: Boolean = true,
)

data class RestoreApp(
    val dirName: String,
    val label: String,
    val packageName: String,
    val userId: Int = 0,
    val hasApk: Boolean = false,
    val hasInternalData: Boolean = false,
    val hasExternalData: Boolean = false,
    val hasAdditionalData: Boolean = false,
    val isSystemApp: Boolean = false,
    var option: RestoreOption = RestoreOption(),
) {
    val isDataAllSelected: Boolean
        get() = (!hasInternalData || option.internalData) &&
            (!hasExternalData || option.externalData) &&
            (!hasAdditionalData || option.additionalData)
    val isSelected: Boolean
        get() = (option.apk && hasApk) ||
            (option.internalData && hasInternalData) ||
            (option.externalData && hasExternalData) ||
            (option.additionalData && hasAdditionalData)

    val toggleableState: ToggleableState
        get() {
            val available = buildList {
                if (hasApk) add(option.apk)
                if (hasInternalData) add(option.internalData)
                if (hasExternalData) add(option.externalData)
                if (hasAdditionalData) add(option.additionalData)
            }
            if (available.isEmpty()) return ToggleableState.Off
            return when {
                available.all { it } -> ToggleableState.On
                available.none { it } -> ToggleableState.Off
                else -> ToggleableState.Indeterminate
            }
        }
}
