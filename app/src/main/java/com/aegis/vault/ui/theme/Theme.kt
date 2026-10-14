@file:OptIn(ExperimentalTextApi::class)

package com.aegis.vault.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.aegis.vault.R

/**
 * Inter, loaded as a Google downloadable font — the mockups are set in Inter, and
 * this avoids shipping binary font assets in the repo. `providerPackage` is the
 * Play Services font provider present on every Android device with Play services.
 */
private val InterFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val InterGoogleFont = GoogleFont("Inter")

/** Inter at every weight the design uses (regular through bold). */
val InterFontFamily = FontFamily(
    // A variable-font single file would do, but static weights are downloadable
    // individually and cache per weight in Play services.
    Font(googleFont = InterGoogleFont, fontProvider = InterFontProvider, weight = FontWeight.W400),
    Font(googleFont = InterGoogleFont, fontProvider = InterFontProvider, weight = FontWeight.W500),
    Font(googleFont = InterGoogleFont, fontProvider = InterFontProvider, weight = FontWeight.W600),
    Font(googleFont = InterGoogleFont, fontProvider = InterFontProvider, weight = FontWeight.W700),
    Font(googleFont = InterGoogleFont, fontProvider = InterFontProvider, weight = FontWeight.W800),
    // bestEffort fallback weights let any requested weight still resolve.
    Font(
        googleFont = InterGoogleFont,
        fontProvider = InterFontProvider,
        weight = FontWeight.W400,
        style = FontStyle.Italic,
    ),
)

/**
 * Type scale copied from the mockups (sizes in sp exactly as designed; weights as
 * styled in the HTML: 400/500/600/700).
 */
@Immutable
object AegisType {
    /** Onboarding hero title — 30/700. */
    val hero = TextStyle(fontFamily = InterFontFamily, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, letterSpacing = 0.sp)

    /** Screen titles — 26–28/700 (mock uses 26 in L3, 28 in L4 header). */
    val title = TextStyle(fontFamily = InterFontFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp, letterSpacing = 0.sp)
    val titleCompact = TextStyle(fontFamily = InterFontFamily, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, letterSpacing = 0.sp)

    /** Generator display password — 22/600 with 1px letter spacing. */
    val passwordDisplay = TextStyle(fontFamily = InterFontFamily, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp, letterSpacing = 1.sp)

    /** Navigation bar title — 17/600. */
    val navTitle = TextStyle(fontFamily = InterFontFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, letterSpacing = 0.sp)

    /** Buttons, row titles — 16/600. */
    val button = TextStyle(fontFamily = InterFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, letterSpacing = 0.sp)

    /** Section headers and row titles — 15/400 or /600. */
    val body = TextStyle(fontFamily = InterFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, letterSpacing = 0.sp)
    val bodyStrong = TextStyle(fontFamily = InterFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, letterSpacing = 0.sp)

    /** Subtitles, descriptions — 14/400 and 13/400. */
    val subhead = TextStyle(fontFamily = InterFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, letterSpacing = 0.sp)
    val footnote = TextStyle(fontFamily = InterFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp, letterSpacing = 0.sp)
    val footnoteStrong = TextStyle(fontFamily = InterFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp, letterSpacing = 0.sp)

    /** Captions — 12, 11, 10, 9.5/500 (tab bar labels). */
    val caption = TextStyle(fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp, letterSpacing = 0.sp)
    val captionStrong = TextStyle(fontFamily = InterFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp, letterSpacing = 0.sp)
    val mini = TextStyle(fontFamily = InterFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Normal, lineHeight = 15.sp, letterSpacing = 0.sp)
    val micro = TextStyle(fontFamily = InterFontFamily, fontSize = 10.sp, fontWeight = FontWeight.Medium, lineHeight = 13.sp, letterSpacing = 0.sp)
    val nano = TextStyle(fontFamily = InterFontFamily, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp, letterSpacing = 0.sp)

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
