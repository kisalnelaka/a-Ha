package org.audhd.aha.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = PureBlack,
    secondary = TextSecondary,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = DarkCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCharcoal,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
)

@Composable
fun AHaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // a-Ha defaults strictly to dopamine-neutral pure black dark mode to eliminate sensory glare
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
