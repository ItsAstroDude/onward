package com.astro.onward.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = OffWhite,
    primaryContainer = SageContainer,
    onPrimaryContainer = PineDeep,
    secondary = Sage,
    onSecondary = OffWhite,
    secondaryContainer = SageContainer,
    onSecondaryContainer = PineDeep,
    tertiary = Citrus,
    onTertiary = Color.White,
    tertiaryContainer = CitrusContainer,
    onTertiaryContainer = CitrusInk,
    background = OffWhite,
    onBackground = InkLight,
    surface = OffWhite,
    onSurface = InkLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = SageDim,
    outline = OutlineLight,
    error = ErrorMuted,
    onError = OffWhite,
)

private val DarkColors = darkColorScheme(
    primary = SageBright,
    onPrimary = PineDeep,
    primaryContainer = Pine,
    onPrimaryContainer = SageContainer,
    secondary = SageBrightDim,
    onSecondary = PineDeep,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = SageContainer,
    tertiary = CitrusBright,
    onTertiary = Color(0xFF46200A),
    tertiaryContainer = CitrusContainerDark,
    onTertiaryContainer = CitrusContainer,
    background = ForestNight,
    onBackground = InkDark,
    surface = ForestNight,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = Color(0xFFE5A79B),
    onError = Color(0xFF4A150D),
)

/** True when the system animator scale is 0 — skip decorative motion. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

@Composable
fun OnwardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = OnwardTypography,
        content = content,
    )
}
