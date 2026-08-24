package com.xayah.databackup.entity

data class RestoreApp(
    val dirName: String,
    val label: String,
    val packageName: String,
    val userId: Int = 0,
    val hasApk: Boolean = false,
    val hasInternalData: Boolean = false,
    val hasExternalData: Boolean = false,
    val hasAdditionalData: Boolean = false,
    var selected: Boolean = true,
) {
    val isSelected: Boolean
        get() = selected
}
