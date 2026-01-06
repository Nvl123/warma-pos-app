package com.dicoding.warmapos.ui.theme

import androidx.compose.ui.graphics.Color

// ===== THEME DEFINITIONS =====
enum class AppTheme(
    val displayName: String,
    val emoji: String,
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color
) {
    EMERALD(
        displayName = "Hijau Zamrud",
        emoji = "💚",
        primary = Color(0xFF10B981),
        primaryDark = Color(0xFF059669),
        primaryLight = Color(0xFF34D399),
        primaryContainer = Color(0xFFD1FAE5),
        onPrimaryContainer = Color(0xFF064E3B),
        secondary = Color(0xFF14B8A6),
        secondaryContainer = Color(0xFFCCFBF1)
    ),
    OCEAN(
        displayName = "Biru Laut",
        emoji = "💙",
        primary = Color(0xFF3B82F6),
        primaryDark = Color(0xFF2563EB),
        primaryLight = Color(0xFF60A5FA),
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E3A8A),
        secondary = Color(0xFF0EA5E9),
        secondaryContainer = Color(0xFFE0F2FE)
    ),
    SUNSET(
        displayName = "Jingga Senja",
        emoji = "🧡",
        primary = Color(0xFFF97316),
        primaryDark = Color(0xFFEA580C),
        primaryLight = Color(0xFFFB923C),
        primaryContainer = Color(0xFFFFEDD5),
        onPrimaryContainer = Color(0xFF7C2D12),
        secondary = Color(0xFFF59E0B),
        secondaryContainer = Color(0xFFFEF3C7)
    ),
    ROSE(
        displayName = "Merah Mawar",
        emoji = "💗",
        primary = Color(0xFFF43F5E),
        primaryDark = Color(0xFFE11D48),
        primaryLight = Color(0xFFFB7185),
        primaryContainer = Color(0xFFFFE4E6),
        onPrimaryContainer = Color(0xFF881337),
        secondary = Color(0xFFEC4899),
        secondaryContainer = Color(0xFFFCE7F3)
    ),
    PURPLE(
        displayName = "Ungu Royal",
        emoji = "💜",
        primary = Color(0xFF8B5CF6),
        primaryDark = Color(0xFF7C3AED),
        primaryLight = Color(0xFFA78BFA),
        primaryContainer = Color(0xFFEDE9FE),
        onPrimaryContainer = Color(0xFF4C1D95),
        secondary = Color(0xFFA855F7),
        secondaryContainer = Color(0xFFF3E8FF)
    ),
    DARK(
        displayName = "Gelap",
        emoji = "🖤",
        primary = Color(0xFF6366F1),
        primaryDark = Color(0xFF4F46E5),
        primaryLight = Color(0xFF818CF8),
        primaryContainer = Color(0xFF312E81),
        onPrimaryContainer = Color(0xFFE0E7FF),
        secondary = Color(0xFF8B5CF6),
        secondaryContainer = Color(0xFF4C1D95)
    )
}

// Default colors (Emerald theme - for compatibility)
val Primary = Color(0xFF10B981)
val PrimaryDark = Color(0xFF059669)
val PrimaryLight = Color(0xFF34D399)
val PrimaryContainer = Color(0xFFD1FAE5)
val OnPrimaryContainer = Color(0xFF064E3B)

val Secondary = Color(0xFF14B8A6)
val SecondaryContainer = Color(0xFFCCFBF1)
val OnSecondaryContainer = Color(0xFF134E4A)

val Tertiary = Color(0xFF0EA5E9)
val TertiaryContainer = Color(0xFFE0F2FE)

// Background & Surface - Light
val Background = Color(0xFFF0FDF4)
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFECFDF5)
val SurfaceContainer = Color(0xFFF5F5F5)

// Background & Surface - Dark
val BackgroundDark = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val SurfaceVariantDark = Color(0xFF334155)

// Text colors
val OnPrimary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF1F2937)
val OnSurface = Color(0xFF1F2937)
val OnBackgroundDark = Color(0xFFF1F5F9)
val OnSurfaceDark = Color(0xFFF1F5F9)

// Status colors
val Success = Color(0xFF22C55E)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)
val Info = Color(0xFF3B82F6)

// Cart & Receipt
val CartBackground = Color(0xFFFEFCE8)
val ReceiptBackground = Color(0xFFFFFEF0)
