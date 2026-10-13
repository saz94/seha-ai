package ai.seha.app.ui

import ai.seha.app.R
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The look comes from a tailor's measuring tape and a clinic at first light: one deep accent
// (sea-teal by default), a saffron for energy, a plum for fat, on cool sage paper.

/** One selectable colour theme: the accent in light and dark, and the two ends of the headline card. */
class Palette(
    val id: String,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val darkHeroStart: Color,
    val darkHeroEnd: Color,
)

val Palettes = listOf(
    Palette(
        "oasis", Color(0xFF0B7A6E), Color(0xFFCDEDE6), Color(0xFF05312C),
        Color(0xFF62D6C2), Color(0xFF00382F), Color(0xFF0E4D45), Color(0xFFCDEDE6),
        Color(0xFF0A3F3B), Color(0xFF0F7F72), Color(0xFF0B3531), Color(0xFF115F56),
    ),
    Palette(
        "dusk", Color(0xFF4153C9), Color(0xFFDFE3FF), Color(0xFF0F1A63),
        Color(0xFFB3BDFF), Color(0xFF14207A), Color(0xFF2A388F), Color(0xFFDFE3FF),
        Color(0xFF1B256F), Color(0xFF4B5FDC), Color(0xFF1A2260), Color(0xFF3443A6),
    ),
    Palette(
        "dune", Color(0xFF8F5410), Color(0xFFFCE3C2), Color(0xFF3A1F00),
        Color(0xFFF2BE7D), Color(0xFF4A2A00), Color(0xFF693F0A), Color(0xFFFCE3C2),
        Color(0xFF5A3306), Color(0xFFA8691C), Color(0xFF4A2B07), Color(0xFF7F4F14),
    ),
    Palette(
        "rose", Color(0xFFA83466), Color(0xFFFBDCE8), Color(0xFF45102A),
        Color(0xFFF5A9C6), Color(0xFF55102F), Color(0xFF76234A), Color(0xFFFBDCE8),
        Color(0xFF6B1C3F), Color(0xFFBC4477), Color(0xFF581733), Color(0xFF8E2E59),
    ),
)

fun paletteOf(id: String): Palette = Palettes.firstOrNull { it.id == id } ?: Palettes[0]

private val Ink = Color(0xFF10201E)

private fun light(p: Palette): ColorScheme = lightColorScheme(
    primary = p.primary,
    onPrimary = Color.White,
    primaryContainer = p.primaryContainer,
    onPrimaryContainer = p.onPrimaryContainer,
    secondary = Color(0xFF8A5A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBE6B8),
    onSecondaryContainer = Color(0xFF3A2500),
    tertiary = Color(0xFF99406C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8DCE8),
    onTertiaryContainer = Color(0xFF43102B),
    error = Color(0xFFBF3A2B),
    onError = Color.White,
    errorContainer = Color(0xFFFBDDD8),
    onErrorContainer = Color(0xFF5C140C),
    background = Color(0xFFF1F4F1),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE4EAE6),
    onSurfaceVariant = Color(0xFF55655F),
    outline = Color(0xFFA9B7B1),
    outlineVariant = Color(0xFFDAE2DD),
    surfaceDim = Color(0xFFDAE0DC),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9F7),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFECF0ED),
    surfaceContainerHighest = Color(0xFFE4EAE6),
    inverseSurface = Color(0xFF22332F),
    inverseOnSurface = Color(0xFFEEF3F0),
    inversePrimary = p.darkPrimary,
    scrim = Color.Black,
)

private fun dark(p: Palette): ColorScheme = darkColorScheme(
    primary = p.darkPrimary,
    onPrimary = p.darkOnPrimary,
    primaryContainer = p.darkContainer,
    onPrimaryContainer = p.darkOnContainer,
    secondary = Color(0xFFF6C263),
    onSecondary = Color(0xFF3A2500),
    secondaryContainer = Color(0xFF4F3600),
    onSecondaryContainer = Color(0xFFFBE6B8),
    tertiary = Color(0xFFF0A8C8),
    onTertiary = Color(0xFF43102B),
    tertiaryContainer = Color(0xFF5E2241),
    onTertiaryContainer = Color(0xFFF8DCE8),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF5C140C),
    errorContainer = Color(0xFF772318),
    onErrorContainer = Color(0xFFFBDDD8),
    background = Color(0xFF0A1413),
    onBackground = Color(0xFFE6EEEA),
    surface = Color(0xFF121E1C),
    onSurface = Color(0xFFE6EEEA),
    surfaceVariant = Color(0xFF1E2C29),
    onSurfaceVariant = Color(0xFF9DB0A9),
    outline = Color(0xFF4A5E57),
    outlineVariant = Color(0xFF233330),
    surfaceDim = Color(0xFF0A1413),
    surfaceBright = Color(0xFF283A36),
    surfaceContainerLowest = Color(0xFF060E0D),
    surfaceContainerLow = Color(0xFF0E1918),
    surfaceContainer = Color(0xFF121E1C),
    surfaceContainerHigh = Color(0xFF192725),
    surfaceContainerHighest = Color(0xFF1E2C29),
    inverseSurface = Color(0xFFE6EEEA),
    inverseOnSurface = Color(0xFF22332F),
    inversePrimary = p.primary,
    scrim = Color.Black,
)

/** Colours with a meaning that Material has no slot for. */
class SehaColors(
    val heroStart: Color,
    val heroEnd: Color,
    val protein: Color,
    val carbs: Color,
    val fat: Color,
    val good: Color,
    val water: Color,
)

val LocalSehaColors = staticCompositionLocalOf {
    SehaColors(Color.Black, Color.Black, Color.Black, Color.Black, Color.Black, Color.Black, Color.Black)
}

/** IBM Plex Sans Arabic: one family drawn for both Latin and Arabic, so mixed text sits together. */
val Plex = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
)

private fun TextStyle.plex(weight: FontWeight = FontWeight.Normal) = copy(fontFamily = Plex, fontWeight = weight)

private val AppTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.plex(FontWeight.SemiBold),
        displayMedium = base.displayMedium.plex(FontWeight.SemiBold),
        displaySmall = base.displaySmall.plex(FontWeight.SemiBold).copy(fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp),
        headlineLarge = base.headlineLarge.plex(FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.plex(FontWeight.SemiBold).copy(letterSpacing = (-0.3).sp),
        headlineSmall = base.headlineSmall.plex(FontWeight.SemiBold),
        titleLarge = base.titleLarge.plex(FontWeight.SemiBold),
        titleMedium = base.titleMedium.plex(FontWeight.SemiBold).copy(letterSpacing = 0.sp),
        titleSmall = base.titleSmall.plex(FontWeight.SemiBold).copy(letterSpacing = 0.sp),
        bodyLarge = base.bodyLarge.plex().copy(letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.plex().copy(letterSpacing = 0.sp),
        bodySmall = base.bodySmall.plex().copy(letterSpacing = 0.sp),
        labelLarge = base.labelLarge.plex(FontWeight.Medium).copy(letterSpacing = 0.sp),
        labelMedium = base.labelMedium.plex(FontWeight.Medium).copy(letterSpacing = 0.sp),
        labelSmall = base.labelSmall.plex(FontWeight.Medium).copy(letterSpacing = 0.sp),
    )
}

// Radius follows size: the headline card is the roundest thing, small controls the tightest.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Figures line up when every digit has the same width. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

@Composable
fun SehaTheme(paletteId: String, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = paletteOf(paletteId)
    val extra = if (dark) {
        SehaColors(p.darkHeroStart, p.darkHeroEnd, p.darkPrimary, Color(0xFFF6C263), Color(0xFFF0A8C8), Color(0xFF7FD9A6), Color(0xFF7FC4F0))
    } else {
        SehaColors(p.heroStart, p.heroEnd, p.primary, Color(0xFFD08A00), Color(0xFFB04E7F), Color(0xFF1C8A56), Color(0xFF2F86C4))
    }
    CompositionLocalProvider(LocalSehaColors provides extra) {
        MaterialTheme(
            colorScheme = if (dark) dark(p) else light(p),
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
