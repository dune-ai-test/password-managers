package com.aegis.vault.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * The app font family. The mockups are set in Inter; Inter is available as a
 * downloadable Google Font, but that adds a font-provider dependency and a
 * network round-trip on first launch. The design reads faithfully with the
 * system default family (Roboto), so that is what ships.
 */
private val AegisFontFamily = FontFamily.Default

/**
 * Type scale copied from the mockups (sizes in sp exactly as designed; weights as
 * styled in the HTML: 400/500/600/700).
 */
@Immutable
object AegisType {
    /** Onboarding hero title — 30/700. */
    val hero = TextStyle(fontFamily = AegisFontFamily, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, letterSpacing = 0.sp)

    /** Screen titles — 26–28/700 (mock uses 26 in L3, 28 in L4 header). */
    val title = TextStyle(fontFamily = AegisFontFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp, letterSpacing = 0.sp)
    val titleCompact = TextStyle(fontFamily = AegisFontFamily, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, letterSpacing = 0.sp)

    /** Generator display password — 22/600 with 1px letter spacing. */
    val passwordDisplay = TextStyle(fontFamily = AegisFontFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp, letterSpacing = 1.sp)

    /** Navigation bar title — 17/600. */
    val navTitle = TextStyle(fontFamily = AegisFontFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, letterSpacing = 0.sp)

    /** Buttons, row titles — 16/600. */
    val button = TextStyle(fontFamily = AegisFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, letterSpacing = 0.sp)

    /** Section headers and row titles — 15/400 or /600. */
    val body = TextStyle(fontFamily = AegisFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, letterSpacing = 0.sp)
    val bodyStrong = TextStyle(fontFamily = AegisFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, letterSpacing = 0.sp)

    /** Subtitles, descriptions — 14/400 and 13/400. */
    val subhead = TextStyle(fontFamily = AegisFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, letterSpacing = 0.sp)
    val footnote = TextStyle(fontFamily = AegisFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp, letterSpacing = 0.sp)
    val footnoteStrong = TextStyle(fontFamily = AegisFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp, letterSpacing = 0.sp)

    /** Captions — 12, 11, 10, 9.5/500 (tab bar labels). */
    val caption = TextStyle(fontFamily = AegisFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp, letterSpacing = 0.sp)
    val captionStrong = TextStyle(fontFamily = AegisFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp, letterSpacing = 0.sp)
    val mini = TextStyle(fontFamily = AegisFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Normal, lineHeight = 15.sp, letterSpacing = 0.sp)
    val micro = TextStyle(fontFamily = AegisFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Medium, lineHeight = 13.sp, letterSpacing = 0.sp)
    val nano = TextStyle(fontFamily = AegisFontFamily, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp, letterSpacing = 0.sp)

    /** Centered helper for empty states and meta lines. */
    fun centered(style: TextStyle): TextStyle = style.copy(textAlign = TextAlign.Center)
}

/** Entry point for reading theme values in composables. */
object AegisTheme {
    val colors: AegisColors
        @Composable @ReadOnlyComposable get() = LocalAegisColors.current
}

/**
 * True when the system is currently in dark mode. Implemented against
 * [LocalConfiguration] (no Material dependency) so the answer is identical on
 * every supported API level.
 */
@Composable
fun isSystemInDark(): Boolean {
    val configuration = LocalConfiguration.current
    return (configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
}

@Composable
fun AegisTheme(
    darkTheme: Boolean = isSystemInDark(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAegisColors provides if (darkTheme) DarkAegisColors else LightAegisColors,
    ) {
        content()
    }
}
