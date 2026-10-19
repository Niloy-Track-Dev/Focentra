package com.niloy.focentra.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Brand Accent - Modern Cobalt / Indigo (Deynexa Light Design System)
val AccentPrimary = Color(0xFF3B82F6)
val AccentPrimaryDark = Color(0xFF60A5FA)
val AccentPrimaryContainer = Color(0xFFDBEAFE)
val AccentPrimaryContainerDark = Color(0xFF1E3A8A)

// Neutral Canvas - Light Mode (Deynexa Theme)
val BackgroundLight = Color(0xFFF1F5F9) // Slate 100 soft white canvas
val SurfaceLight = Color(0xFFFFFFFF) // Pure white cards
val SurfaceElevatedLight = Color(0xFFE2E8F0)
val SurfaceHighlightLight = Color(0xFFCBD5E1)
val OutlineLight = Color(0xFFCBD5E1)
val OutlineVariantLight = Color(0xFF94A3B8)
val TextPrimaryLight = Color(0xFF0F172A) // Dark Slate 900
val TextSecondaryLight = Color(0xFF334155) // Slate 700
val TextTertiaryLight = Color(0xFF64748B)

// Semantic State Colors
val StateCompleted = Color(0xFF10B981) // Emerald
val StateCompletedContainerLight = Color(0xFFECFDF5)

val StatePending = Color(0xFF64748B) // Slate
val StatePendingContainerLight = Color(0xFFF1F5F9)

val StateSkipped = Color(0xFFF43F5E) // Rose
val StateSkippedContainerLight = Color(0xFFFFF1F2)

// Midnight Theme (Mapped to Deynexa Light Color System for Consistent Aesthetics)
val MidnightBackground = BackgroundLight
val MidnightSurface = SurfaceLight
val MidnightSurfaceVariant = SurfaceElevatedLight
val MidnightPrimary = AccentPrimary
val MidnightPrimaryVariant = AccentPrimary
val MidnightSecondary = Color(0xFF64748B)
val MidnightTertiary = StateCompleted
val MidnightTextPrimary = TextPrimaryLight
val MidnightTextSecondary = TextSecondaryLight
val MidnightAccentGreen = StateCompleted
val MidnightAccentAmber = Color(0xFFF59E0B)

// Heatmap Levels (Light Mode)
val HeatmapLevel0 = Color(0xFFE2E8F0)
val HeatmapLevel1 = Color(0xFFDBEAFE)
val HeatmapLevel2 = Color(0xFF93C5FD)
val HeatmapLevel3 = Color(0xFF3B82F6)
val HeatmapLevel4 = Color(0xFF1D4ED8)
