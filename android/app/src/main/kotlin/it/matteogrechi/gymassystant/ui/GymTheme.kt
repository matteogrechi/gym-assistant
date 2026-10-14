@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package it.matteogrechi.gymassystant.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.matteogrechi.gymassystant.R

@Immutable
data class GymColorTokens(
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val surface: Color,
    val surfaceBright: Color,
    val surfaceContainer: Color,
    val tonalContainer: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val success: Color,
    val onSuccess: Color,
    val warmupContainer: Color,
    val onWarmup: Color,
    val repsContainer: Color,
    val focus: Color,
    val background: Color,
    val chart: Color,
    val scrim: Color,
    val hero: Color = Color(0xFF143F46),
    val onHero: Color = Color.White,
    val onHeroVariant: Color = Color(0xFFD3E9E4),
)

private val LightColors = GymColorTokens(
    primary = Color(0xFF143F46), onPrimary = Color.White,
    accent = Color(0xFFFFBF69), onAccent = Color(0xFF143F46),
    surface = Color(0xFFFFFAF2), surfaceBright = Color.White,
    surfaceContainer = Color(0xFFE6F1F0), tonalContainer = Color(0xFFD3E9E4),
    onSurface = Color(0xFF143F46), onSurfaceVariant = Color(0xFF51676A),
    outline = Color(0xFF758E8F), outlineVariant = Color(0xFFCAD9D6),
    success = Color(0xFF24665F), onSuccess = Color.White,
    warmupContainer = Color(0xFFFFF0DB), onWarmup = Color(0xFF624428),
    repsContainer = Color(0xFFEDF5F2), focus = Color(0xFF9B480F),
    background = Color(0xFFF6EEDF), chart = Color(0xFF377D7D),
    scrim = Color(0xAA143F46),
)

private val DarkColors = GymColorTokens(
    primary = Color(0xFFFFBF69), onPrimary = Color(0xFF302112),
    accent = Color(0xFFFFBF69), onAccent = Color(0xFF143F46),
    surface = Color(0xFF142729), surfaceBright = Color(0xFF1C3335),
    surfaceContainer = Color(0xFF234245), tonalContainer = Color(0xFF2D5051),
    onSurface = Color(0xFFEDF5EE), onSurfaceVariant = Color(0xFFBACFCA),
    outline = Color(0xFF9FB7B5), outlineVariant = Color(0xFF476563),
    success = Color(0xFF95D6C6), onSuccess = Color(0xFF103C35),
    warmupContainer = Color(0xFF423522), onWarmup = Color(0xFFFFDBA8),
    repsContainer = Color(0xFF203D3D), focus = Color(0xFFFFBF69),
    background = Color(0xFF0D1C1E), chart = Color(0xFF8CCFC3),
    scrim = Color(0xCC071719),
)

private val LocalGymColors = staticCompositionLocalOf { LightColors }
val GymColors: GymColorTokens
    @Composable get() = LocalGymColors.current

// Explicit variable-font weights avoid synthesized bold and keep all fonts offline.
val DmSans = FontFamily(
    listOf(400, 500, 600, 700, 800).map { weight ->
        Font(
            R.font.dm_sans, weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight), FontVariation.opticalSizing(14.sp),
            ),
        )
    },
)
val Manrope = FontFamily(
    listOf(400, 500, 600, 700, 800).map { weight ->
        Font(
            R.font.manrope, weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

private fun body(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = DmSans, fontWeight = weight, fontSize = size.sp,
        lineHeight = lineHeight.sp, letterSpacing = 0.sp)

private fun heading(size: Int, lineHeight: Int, spacing: Float = 0f,
    weight: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = Manrope, fontWeight = weight, fontSize = size.sp,
        lineHeight = lineHeight.sp, letterSpacing = spacing.sp)

private val GymTypography = Typography(
    displayLarge = heading(53, 61, -2.5f),
    displayMedium = heading(35, 42),
    displaySmall = heading(32, 40, -1f, FontWeight.ExtraBold),
    headlineLarge = heading(28, 35, -1f),
    headlineMedium = heading(24, 30, -1f),
    headlineSmall = heading(21, 27, -.8f, FontWeight.ExtraBold),
    titleLarge = heading(26, 30, -.7f),
    titleMedium = heading(15, 20),
    titleSmall = heading(14, 20),
    bodyLarge = body(14, 22),
    bodyMedium = body(13, 21),
    bodySmall = body(11, 18),
    labelLarge = body(14, 20, FontWeight.Medium),
    labelMedium = body(12, 16, FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = DmSans, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 14.3.sp, letterSpacing = 0.sp),
)

@Composable
fun GymTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkColors else LightColors
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = colors.accent, onPrimary = colors.onAccent,
        primaryContainer = colors.tonalContainer, onPrimaryContainer = colors.primary,
        secondary = colors.primary, onSecondary = colors.onPrimary,
        secondaryContainer = colors.tonalContainer, onSecondaryContainer = colors.primary,
        tertiary = colors.success, onTertiary = colors.onSuccess,
        tertiaryContainer = colors.warmupContainer, onTertiaryContainer = colors.onWarmup,
        background = colors.surface, onBackground = colors.onSurface,
        surface = colors.surface, onSurface = colors.onSurface,
        surfaceVariant = colors.surfaceContainer, onSurfaceVariant = colors.onSurfaceVariant,
        surfaceTint = Color.Transparent, surfaceBright = colors.surfaceBright,
        surfaceDim = colors.surface, surfaceContainer = colors.surfaceContainer,
        surfaceContainerLowest = colors.surface, surfaceContainerLow = colors.surface,
        surfaceContainerHigh = colors.surfaceBright,
        surfaceContainerHighest = colors.tonalContainer,
        outline = colors.outline, outlineVariant = colors.outlineVariant,
        scrim = colors.scrim,
        error = if (dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
        onError = if (dark) Color(0xFF690005) else Color.White,
        errorContainer = if (dark) Color(0xFF93000A) else Color(0xFFFFDAD6),
        onErrorContainer = if (dark) Color(0xFFFFDAD6) else Color(0xFF410002),
        inverseSurface = colors.onSurface, inverseOnSurface = colors.surface,
        inversePrimary = colors.focus,
    )
    CompositionLocalProvider(LocalGymColors provides colors) {
        MaterialTheme(
            colorScheme = scheme, typography = GymTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp),
                extraLarge = RoundedCornerShape(28.dp),
            ), content = content,
        )
    }
}
