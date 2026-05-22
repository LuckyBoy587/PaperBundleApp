package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PaperColorScheme = lightColorScheme(
    primary = PencilCharcoal,
    onPrimary = WarmPaperBackground,
    secondary = AppaPrimary,
    onSecondary = PaperCardLight,
    background = WarmPaperBackground,
    onBackground = PencilCharcoal,
    surface = PaperCardLight,
    onSurface = PencilCharcoal
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
