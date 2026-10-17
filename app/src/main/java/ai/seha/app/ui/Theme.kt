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

// The look: soft lavender paper, white rounded cards, one periwinkle accent, and a glowing
// gradient orb for the assistant. Saffron marks carbs and energy, pink marks fat.

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
        "aura", Color(0xFF6B6AF5), Color(0xFFE4E3FF), Color(0xFF1F1D6B),
        Color(0xFFB4B2FF), Color(0xFF1F1D6B), Color(0xFF3A38A8), Color(0xFFE4E3FF),
        Color(0xFF5D5BE6), Color(0xFF9A6BF2), Color(0xFF3A38A8), Color(0xFF6B4FC7),
    ),
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

private val Ink = Color(0xFF1C1B2E)

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
    background = Color(0xFFF2F3FA),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE6E7F3),
    onSurfaceVariant = Color(0xFF686A86),
    outline = Color(0xFFAFB1C9),
    outlineVariant = Color(0xFFE1E2EE),
    surfaceDim = Color(0xFFDCDDEA),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F8FD),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEDEEF7),
    surfaceContainerHighest = Color(0xFFE6E7F3),
    inverseSurface = Color(0xFF2A2945),
    inverseOnSurface = Color(0xFFF1F1FA),
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
    background = Color(0xFF0F1020),
    onBackground = Color(0xFFE9E9F5),
    surface = Color(0xFF191A2E),
    onSurface = Color(0xFFE9E9F5),
    surfaceVariant = Color(0xFF24253D),
    onSurfaceVariant = Color(0xFFA5A7C4),
    outline = Color(0xFF4E5074),
    outlineVariant = Color(0xFF2A2B45),
    surfaceDim = Color(0xFF0F1020),
    surfaceBright = Color(0xFF31325A),
    surfaceContainerLowest = Color(0xFF0A0B18),
    surfaceContainerLow = Color(0xFF13142A),
    surfaceContainer = Color(0xFF191A2E),
    surfaceContainerHigh = Color(0xFF1F2038),
    surfaceContainerHighest = Color(0xFF24253D),
    inverseSurface = Color(0xFFE9E9F5),
    inverseOnSurface = Color(0xFF2A2945),
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
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp),
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
