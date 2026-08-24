package com.xayah.databackup.util

enum class ThemeType {
    AUTO,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromStored(value: String?): ThemeType = when (value?.uppercase()) {
            LIGHT.name -> LIGHT
            DARK.name -> DARK
            else -> AUTO
        }
    }
}
