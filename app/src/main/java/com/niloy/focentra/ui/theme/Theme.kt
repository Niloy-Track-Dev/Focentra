package com.niloy.focentra.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CleanMinimalismColorScheme = lightColorScheme(
    primary = CleanMinimalismPrimary,
    onPrimary = CleanMinimalismOnPrimary,
    primaryContainer = CleanMinimalismPrimaryContainer,
    onPrimaryContainer = CleanMinimalismOnPrimaryContainer,
    secondary = CleanMinimalismSecondary,
    onSecondary = CleanMinimalismOnSecondary,
    secondaryContainer = CleanMinimalismSecondaryContainer,
    onSecondaryContainer = CleanMinimalismOnSecondaryContainer,
    tertiary = CleanMinimalismTertiary,
    background = CleanMinimalismBackground,
    onBackground = CleanMinimalismTextPrimary,
    surface = CleanMinimalismSurface,
    onSurface = CleanMinimalismTextPrimary,
    surfaceVariant = CleanMinimalismSurfaceVariant,
    onSurfaceVariant = CleanMinimalismTextSecondary,
    outline = CleanMinimalismOutline,
    outlineVariant = CleanMinimalismOutlineVariant
)

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = MidnightSecondary,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = MidnightTertiary,
    background = MidnightBackground,
    onBackground = MidnightTextPrimary,
    surface = MidnightSurface,
    onSurface = MidnightTextPrimary,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MidnightTextSecondary,
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B)
)

private val OceanColorScheme = darkColorScheme(
    primary = OceanPrimary,
    onPrimary = Color(0xFF030712),
    primaryContainer = Color(0xFF0077B6),
    secondary = OceanSecondary,
    tertiary = OceanTertiary,
    background = OceanBackground,
    onBackground = Color(0xFFF0F9FF),
    surface = OceanSurface,
    onSurface = Color(0xFFF0F9FF),
    surfaceVariant = OceanSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334E68)
)

private val ForestColorScheme = darkColorScheme(
    primary = ForestPrimary,
    onPrimary = Color(0xFF052E16),
    primaryContainer = Color(0xFF2D6A4F),
    secondary = ForestSecondary,
    tertiary = ForestTertiary,
    background = ForestBackground,
    onBackground = Color(0xFFF0FDF4),
    surface = ForestSurface,
    onSurface = Color(0xFFF0FDF4),
    surfaceVariant = ForestSurfaceVariant,
    onSurfaceVariant = Color(0xFF86EFAC),
    outline = Color(0xFF2D6A4F)
)

private val SunsetColorScheme = darkColorScheme(
    primary = SunsetPrimary,
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF7C2D12),
    secondary = SunsetSecondary,
    tertiary = SunsetTertiary,
    background = SunsetBackground,
    onBackground = Color(0xFFFFF7ED),
    surface = SunsetSurface,
    onSurface = Color(0xFFFFF7ED),
    surfaceVariant = SunsetSurfaceVariant,
    onSurfaceVariant = Color(0xFFFDBA74),
    outline = Color(0xFF7C2D12)
)

private val CyberpunkColorScheme = darkColorScheme(
    primary = CyberpunkPrimary,
    onPrimary = Color(0xFF0D0221),
    primaryContainer = Color(0xFF2E175A),
    onPrimaryContainer = Color(0xFF00F5D4),
    secondary = CyberpunkSecondary,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = CyberpunkTertiary,
    background = CyberpunkBackground,
    onBackground = Color(0xFFF0E6FF),
    surface = CyberpunkSurface,
    onSurface = Color(0xFFF0E6FF),
    surfaceVariant = CyberpunkSurfaceVariant,
    onSurfaceVariant = Color(0xFFD8B4FE),
    outline = Color(0xFF7B2CBF)
)

private val MatchaColorScheme = lightColorScheme(
    primary = MatchaPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E6D3),
    onPrimaryContainer = Color(0xFF1B281B),
    secondary = MatchaSecondary,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = MatchaTertiary,
    background = MatchaBackground,
    onBackground = MatchaTextPrimary,
    surface = MatchaSurface,
    onSurface = MatchaTextPrimary,
    surfaceVariant = MatchaSurfaceVariant,
    onSurfaceVariant = MatchaTextSecondary,
    outline = Color(0xFFC4D5BF),
    outlineVariant = Color(0xFFDDE6DA)
)

private val ModernLightColorScheme = CleanMinimalismColorScheme

@Composable
fun FocentraTheme(
    themeName: String = "midnight",
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName.lowercase()) {
        "midnight" -> MidnightColorScheme
        "ocean" -> OceanColorScheme
        "forest" -> ForestColorScheme
        "sunset" -> SunsetColorScheme
        "cyberpunk" -> CyberpunkColorScheme
        "matcha" -> MatchaColorScheme
        "light", "clean_minimalism", "minimalism", "minimal" -> CleanMinimalismColorScheme
        else -> MidnightColorScheme
    }

    val isLight = colorScheme == CleanMinimalismColorScheme || colorScheme == MatchaColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
