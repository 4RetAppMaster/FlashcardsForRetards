package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = SemiDarkGreen,
        onPrimary = Color.White,
        primaryContainer = DeepForestGreen,
        onPrimaryContainer = SoftMint,
        secondary = SoftMint,
        onSecondary = DarkBackground,
        background = DarkBackground,
        onBackground = Color.White,
        surface = HeaderGreenSurface,
        onSurface = Color.White,
        surfaceVariant = SurfaceControl,
        onSurfaceVariant = MutedText,
        outline = NeutralCardBorder
    )

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
