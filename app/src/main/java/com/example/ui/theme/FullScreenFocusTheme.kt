package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class FullScreenFocusThemePreset(
    val id: String,
    val name: String,
    val description: String,
    val backgroundColor: Color,
    val backgroundGradient: List<Color>? = null,
    val clockColor: Color,
    val accentColor: Color,
    val cardBackgroundColor: Color,
    val cardTextColor: Color,
    val quoteColor: Color,
    val buttonBackgroundColor: Color,
    val buttonIconColor: Color,
    val previewColors: List<Color>
)

object FullScreenThemePresets {
    val presets = listOf(
        FullScreenFocusThemePreset(
            id = "oled_midnight",
            name = "OLED Midnight",
            description = "Pitch black for OLED screens",
            backgroundColor = Color(0xFF000000),
            clockColor = Color(0xFFFFFFFF),
            accentColor = Color(0xFF6366F1),
            cardBackgroundColor = Color(0xFF1E2135),
            cardTextColor = Color(0xFFFFFFFF),
            quoteColor = Color(0xFF94A3B8),
            buttonBackgroundColor = Color(0xFF1E2135),
            buttonIconColor = Color(0xFFFFFFFF),
            previewColors = listOf(Color(0xFF000000), Color(0xFF6366F1), Color(0xFFFFFFFF))
        ),
        FullScreenFocusThemePreset(
            id = "cyberpunk",
            name = "Cyberpunk Neon",
            description = "Electric glow & cyan neon",
            backgroundColor = Color(0xFF0D0221),
            backgroundGradient = listOf(Color(0xFF0D0221), Color(0xFF1B0330), Color(0xFF0D0221)),
            clockColor = Color(0xFF00F5D4),
            accentColor = Color(0xFFFF007F),
            cardBackgroundColor = Color(0xFF260A47),
            cardTextColor = Color(0xFF00F5D4),
            quoteColor = Color(0xFFFF77C2),
            buttonBackgroundColor = Color(0xFF260A47),
            buttonIconColor = Color(0xFF00F5D4),
            previewColors = listOf(Color(0xFF0D0221), Color(0xFF00F5D4), Color(0xFFFF007F))
        ),
        FullScreenFocusThemePreset(
            id = "nordic_aurora",
            name = "Nordic Aurora",
            description = "Arctic dark & northern lights",
            backgroundColor = Color(0xFF0F172A),
            backgroundGradient = listOf(Color(0xFF0B1329), Color(0xFF0F2634), Color(0xFF0B1329)),
            clockColor = Color(0xFF34D399),
            accentColor = Color(0xFF38BDF8),
            cardBackgroundColor = Color(0xFF1E293B),
            cardTextColor = Color(0xFFE2E8F0),
            quoteColor = Color(0xFF94A3B8),
            buttonBackgroundColor = Color(0xFF1E293B),
            buttonIconColor = Color(0xFF34D399),
            previewColors = listOf(Color(0xFF0F172A), Color(0xFF34D399), Color(0xFF38BDF8))
        ),
        FullScreenFocusThemePreset(
            id = "sunset_glow",
            name = "Sunset Amber",
            description = "Warm energetic amber glow",
            backgroundColor = Color(0xFF140A05),
            backgroundGradient = listOf(Color(0xFF140A05), Color(0xFF281309), Color(0xFF140A05)),
            clockColor = Color(0xFFFBBF24),
            accentColor = Color(0xFFF97316),
            cardBackgroundColor = Color(0xFF2C1810),
            cardTextColor = Color(0xFFFEF3C7),
            quoteColor = Color(0xFFFDE68A),
            buttonBackgroundColor = Color(0xFF2C1810),
            buttonIconColor = Color(0xFFFBBF24),
            previewColors = listOf(Color(0xFF140A05), Color(0xFFFBBF24), Color(0xFFF97316))
        ),
        FullScreenFocusThemePreset(
            id = "tokyo_neon",
            name = "Tokyo Night",
            description = "Shinjuku purple & magenta",
            backgroundColor = Color(0xFF090D16),
            backgroundGradient = listOf(Color(0xFF090D16), Color(0xFF1A1033), Color(0xFF090D16)),
            clockColor = Color(0xFFE086FF),
            accentColor = Color(0xFFEC4899),
            cardBackgroundColor = Color(0xFF19183B),
            cardTextColor = Color(0xFFF5D0FF),
            quoteColor = Color(0xFFC084FC),
            buttonBackgroundColor = Color(0xFF19183B),
            buttonIconColor = Color(0xFFE086FF),
            previewColors = listOf(Color(0xFF090D16), Color(0xFFE086FF), Color(0xFFEC4899))
        ),
        FullScreenFocusThemePreset(
            id = "emerald_oasis",
            name = "Emerald Oasis",
            description = "Botanical deep forest green",
            backgroundColor = Color(0xFF04150E),
            backgroundGradient = listOf(Color(0xFF04150E), Color(0xFF09291C), Color(0xFF04150E)),
            clockColor = Color(0xFF10B981),
            accentColor = Color(0xFF6EE7B7),
            cardBackgroundColor = Color(0xFF0E3324),
            cardTextColor = Color(0xFFD1FAE5),
            quoteColor = Color(0xFFA7F3D0),
            buttonBackgroundColor = Color(0xFF0E3324),
            buttonIconColor = Color(0xFF10B981),
            previewColors = listOf(Color(0xFF04150E), Color(0xFF10B981), Color(0xFF6EE7B7))
        ),
        FullScreenFocusThemePreset(
            id = "matrix_green",
            name = "Matrix Green",
            description = "Hacker terminal high-contrast",
            backgroundColor = Color(0xFF000000),
            clockColor = Color(0xFF22C55E),
            accentColor = Color(0xFF4ADE80),
            cardBackgroundColor = Color(0xFF06230E),
            cardTextColor = Color(0xFF86EFAC),
            quoteColor = Color(0xFF4ADE80),
            buttonBackgroundColor = Color(0xFF06230E),
            buttonIconColor = Color(0xFF22C55E),
            previewColors = listOf(Color(0xFF000000), Color(0xFF22C55E), Color(0xFF4ADE80))
        ),
        FullScreenFocusThemePreset(
            id = "solar_flare",
            name = "Solar Crimson",
            description = "Deep ruby & crimson heat",
            backgroundColor = Color(0xFF1A0505),
            backgroundGradient = listOf(Color(0xFF1A0505), Color(0xFF2B0A0A), Color(0xFF1A0505)),
            clockColor = Color(0xFFFB7185),
            accentColor = Color(0xFFF43F5E),
            cardBackgroundColor = Color(0xFF381013),
            cardTextColor = Color(0xFFFFE4E6),
            quoteColor = Color(0xFFFDA4AF),
            buttonBackgroundColor = Color(0xFF381013),
            buttonIconColor = Color(0xFFFB7185),
            previewColors = listOf(Color(0xFF1A0505), Color(0xFFFB7185), Color(0xFFF43F5E))
        ),
        FullScreenFocusThemePreset(
            id = "deep_cosmos",
            name = "Deep Cosmos",
            description = "Starlight galaxy & violet glow",
            backgroundColor = Color(0xFF07091B),
            backgroundGradient = listOf(Color(0xFF07091B), Color(0xFF141938), Color(0xFF07091B)),
            clockColor = Color(0xFF818CF8),
            accentColor = Color(0xFFA78BFA),
            cardBackgroundColor = Color(0xFF171B42),
            cardTextColor = Color(0xFFE0E7FF),
            quoteColor = Color(0xFFC7D2FE),
            buttonBackgroundColor = Color(0xFF171B42),
            buttonIconColor = Color(0xFF818CF8),
            previewColors = listOf(Color(0xFF07091B), Color(0xFF818CF8), Color(0xFFA78BFA))
        ),
        FullScreenFocusThemePreset(
            id = "zen_slate",
            name = "Zen Slate",
            description = "Sleek monochrome platinum",
            backgroundColor = Color(0xFF111827),
            clockColor = Color(0xFFF8FAFC),
            accentColor = Color(0xFFCBD5E1),
            cardBackgroundColor = Color(0xFF1E293B),
            cardTextColor = Color(0xFFF1F5F9),
            quoteColor = Color(0xFF94A3B8),
            buttonBackgroundColor = Color(0xFF1E293B),
            buttonIconColor = Color(0xFFF8FAFC),
            previewColors = listOf(Color(0xFF111827), Color(0xFFF8FAFC), Color(0xFFCBD5E1))
        )
    )

    fun getPresetById(id: String): FullScreenFocusThemePreset {
        return presets.find { it.id.equals(id, ignoreCase = true) } ?: presets.first()
    }
}
