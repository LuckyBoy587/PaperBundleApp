package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),          // StitchIndigo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),   // StitchBlue50
    onPrimaryContainer = Color(0xFF6366F1), // StitchIndigo
    
    secondary = AppaPrimary,              // AppaPrimary (Sage/Mint Deep Teal)
    onSecondary = Color.White,
    secondaryContainer = AppaSurface,     // AppaSurface (Light sage cream)
    onSecondaryContainer = AppaOnSurface, // AppaOnSurface (Deep moss teal)
    
    tertiary = AmmaPrimary,               // AmmaPrimary (Rose/Apricot Terra Rose)
    onTertiary = Color.White,
    tertiaryContainer = AmmaSurface,      // AmmaSurface (Soft pink cream)
    onTertiaryContainer = AmmaOnSurface,  // AmmaOnSurface (Elegant ruby burgundy)
    
    background = WarmPaperBackground,
    onBackground = PencilCharcoal,
    surface = PaperCardLight,
    onSurface = PencilCharcoal,
    
    surfaceVariant = Color(0xFFF1F5F9),   // StitchBg / Light Slate-100
    onSurfaceVariant = PencilGray,
    
    outline = Color(0xFFE2E8F0),          // StitchBorder
    outlineVariant = SoftDivider,
    
    error = Color(0xFFEF4444),            // StitchRed500
    errorContainer = Color(0xFFFEF2F2),     // StitchRed50
    onErrorContainer = Color(0xFFEF4444)
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),          // StitchIndigoDark
    onPrimary = Color(0xFF0B0C0E),
    primaryContainer = Color(0xFF312E81),   // Cozy dark Indigo container
    onPrimaryContainer = Color(0xFFC7D2FE),
    
    secondary = AppaPrimaryDark,          // AppaPrimaryDark (Sage/Mint Deep Teal)
    onSecondary = Color(0xFF0B0C0E),
    secondaryContainer = AppaSurfaceDark, // AppaSurfaceDark (Deep forest teal shadow)
    onSecondaryContainer = AppaOnSurfaceDark,
    
    tertiary = AmmaPrimaryDark,           // AmmaPrimaryDark (Rose/Apricot Terra Rose)
    onTertiary = Color(0xFF0B0C0E),
    tertiaryContainer = AmmaSurfaceDark,  // AmmaSurfaceDark (Deep cozy burgundy shadow)
    onTertiaryContainer = AmmaOnSurfaceDark,
    
    background = DarkPaperBackground,
    onBackground = ChalkWhite,
    surface = PaperCardDark,
    onSurface = ChalkWhite,
    
    surfaceVariant = Color(0xFF1E2024),   // Premium Slate-800 equivalent for dark mode
    onSurfaceVariant = ChalkGray,
    
    outline = Color(0xFF2E3035),          // Subtle dark border for sheet separation
    outlineVariant = DarkDivider,
    
    error = RedChalk,                     // Soft light rose red
    errorContainer = Color(0xFF7F1D1D),   // Deep crimson shadow
    onErrorContainer = Color(0xFFFECDD3)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Configure status and navigation bar icons to shift dynamically
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

