package com.xayah.databackup.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import com.xayah.databackup.ui.theme.color.dynamiccolor.ColorSpec
import com.xayah.databackup.ui.theme.color.hct.Hct
import com.xayah.databackup.ui.theme.color.scheme.SchemeExpressive

internal fun expressiveColorScheme(context: Context, darkTheme: Boolean): androidx.compose.material3.ColorScheme {
    val seedArgb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamicScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dynamicScheme.primary.toArgb()
    } else {
        GreenSource.toArgb()
    }
    return SchemeExpressive(
        sourceColorHct = Hct.fromInt(seedArgb),
        isDark = darkTheme,
        contrastLevel = 0.0,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
    ).toColorScheme()
}
