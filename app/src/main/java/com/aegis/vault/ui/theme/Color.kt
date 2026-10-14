package com.aegis.vault.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The complete Aegis color system, taken verbatim from the UI mockups.
 *
 * There is no Material theme anywhere in this app: screens read every color from
 * this palette via [AegisTheme]. Tints are accent colors at low alpha so they
 * automatically read correctly on both the light and dark card backgrounds.
 */
@Immutable
data class AegisColors(
    // Surfaces
    val background: Color,
    val card: Color,
    val cardElevated: Color,
    val cardBorder: Color,
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    // Lines & fills
    val separator: Color,
    val chip: Color,
    val track: Color,
    // Accents (the same hues the mockups use)
    val blue: Color,
    val green: Color,
    val amber: Color,
    val red: Color,
    val purple: Color,
    val indigo: Color,
    // Accent tints (badge backgrounds)
    val blueTint: Color,
    val greenTint: Color,
    val amberTint: Color,
    val redTint: Color,
    val purpleTint: Color,
    // Chrome
    val tabBar: Color,
    val tabBarBorder: Color,
    val highlightPill: Color,
    val shadow: Color,
    val isDark: Boolean,
) {
    /** Tinted badge background for an accent, e.g. [blueTint] for [blue]. */
    fun tintFor(accent: Color): Color = accent.copy(alpha = if (isDark) 0.22f else 0.12f)

    /** Foreground color that reads on a filled accent surface. */
    val onAccent: Color get() = Color.White

    /** Gradient stops for the logo / FAB, straight from the mockups. */
    val gradientStart: Color get() = if (isDark) blue else Color(0xFF007AFF)
    val gradientEnd: Color get() = if (isDark) indigo else Color(0xFF5E5CE6)
}

/** Light palette: the exact values used in every mockup screen. */
val LightAegisColors = AegisColors(
    background = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFFFFFFF),
    cardBorder = Color(0x00000000),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF8E8E93),
    textTertiary = Color(0xFFC7C7CC),
    separator = Color(0xFFE5E5EA),
    chip = Color(0xFFEDEDF0),
    track = Color(0xFFE5E5EA),
    blue = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    amber = Color(0xFFFF9500),
    red = Color(0xFFFF3B30),
    purple = Color(0xFFAF52DE),
    indigo = Color(0xFF5E5CE6),
    blueTint = Color(0xFFE8F1FE),
    greenTint = Color(0xFFE4F8EB),
    amberTint = Color(0xFFFFF3E2),
    redTint = Color(0xFFFFE9E7),
    purpleTint = Color(0xFFF3EAFB),
    tabBar = Color(0xB3FFFFFF), // #FFFFFFB3 from the mockups
    tabBarBorder = Color(0x0A1C1C1E),
    highlightPill = Color(0xFFE8F1FE),
    shadow = Color(0x1F000000), // #0000001F from the mockups
    isDark = false,
)

/** Dark palette: iOS-style dark equivalents of the same tokens. */
val DarkAegisColors = AegisColors(
    background = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    cardElevated = Color(0xFF2C2C2E),
    cardBorder = Color(0x1FFFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0x99EBEBF5),
    textTertiary = Color(0x4DEBEBF5),
    separator = Color(0x33EBEBF5),
    chip = Color(0xFF3A3A3C),
    track = Color(0xFF3A3A3C),
    blue = Color(0xFF0A84FF),
    green = Color(0xFF30D158),
    amber = Color(0xFFFF9F0A),
    red = Color(0xFFFF453A),
    purple = Color(0xFFBF5AF2),
    indigo = Color(0xFF7B7BE8),
    blueTint = Color(0x1F0A84FF),
    greenTint = Color(0x1F30D158),
    amberTint = Color(0x1FFF9F0A),
    redTint = Color(0x1FFF453A),
    purpleTint = Color(0x1FBF5AF2),
    tabBar = Color(0xC41C1C1E),
    tabBarBorder = Color(0x1FFFFFFF),
    highlightPill = Color(0x330A84FF),
    shadow = Color(0x66000000),
    isDark = true,
)

val LocalAegisColors = staticCompositionLocalOf { LightAegisColors }
