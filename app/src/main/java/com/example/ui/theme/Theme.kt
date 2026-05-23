package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PaperColorScheme = lightColorScheme(
    primary = PencilCharcoal,
    onPrimary = PaperCardLight,
    secondary = AppaPrimary,
    onSecondary = PaperCardLight,
    background = WarmPaperBackground,
    onBackground = PencilCharcoal,
    surface = PaperCardLight,
    onSurface = PencilCharcoal,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = PencilGray
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PaperColorScheme,
        typography = Typography,
        content = content
    )
}
