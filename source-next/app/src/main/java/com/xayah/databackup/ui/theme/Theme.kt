package com.xayah.databackup.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xayah.databackup.ui.theme.color.hct.Hct
import com.xayah.databackup.ui.theme.color.scheme.SchemeContent

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DataBackupTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val materialColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            expressiveColorScheme(context = context, darkTheme = darkTheme)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = materialColorScheme.copy(
        background = materialColorScheme.surfaceContainer,
        surface = materialColorScheme.surfaceContainer,
        surfaceContainer = materialColorScheme.surfaceBright,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

@Immutable
class CustomColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val surfaceContainer: Color,
)

private val GreenDarkColorScheme = SchemeContent(
    sourceColorHct = Hct.fromInt(GreenSource.toArgb()),
    isDark = true,
    contrastLevel = 0.0,
).let { scheme ->
    CustomColorScheme(
        primary = Color(scheme.primary),
        onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        surfaceContainer = Color(scheme.surfaceContainer),
    )
}

private val GreenLightColorScheme = SchemeContent(
    sourceColorHct = Hct.fromInt(GreenSource.toArgb()),
    isDark = false,
    contrastLevel = 0.0,
).let { scheme ->
    CustomColorScheme(
        primary = Color(scheme.primary),
        onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        surfaceContainer = Color(scheme.surfaceContainer),
    )
}

object DataBackupTheme {
    val greenColorScheme: CustomColorScheme
        @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) {
            GreenDarkColorScheme
        } else {
            GreenLightColorScheme
        }
}
