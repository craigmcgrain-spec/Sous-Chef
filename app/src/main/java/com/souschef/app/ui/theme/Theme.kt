@file:OptIn(ExperimentalTextApi::class)

package com.souschef.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.souschef.app.R

private val PaperLight = Color(0xFFFAF5EB)
private val SurfaceLight = Color(0xFFFFFDF8)
private val InkLight = Color(0xFF211C18)
private val InkMutedLight = Color(0xFF6B5F55)
private val TerracottaLight = Color(0xFFB4442A)
private val TerracottaWashLight = Color(0xFFF3DDD4)
private val RuleLight = Color(0xFFE4D9C8)
private val SageLight = Color(0xFF5A6650)
private val ErrorLight = Color(0xFFA33A2B)

private val PaperDark = Color(0xFF171310)
private val SurfaceDark = Color(0xFF201B17)
private val InkDark = Color(0xFFF2E9DC)
private val InkMutedDark = Color(0xFFB3A697)
private val TerracottaDark = Color(0xFFE48263)
private val TerracottaWashDark = Color(0xFF3A241D)
private val RuleDark = Color(0xFF3A312A)
private val SageDark = Color(0xFF93A487)
private val ErrorDark = Color(0xFFF08A76)

private val Fraunces = FontFamily(
    Font(
        R.font.fraunces,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.fraunces,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

private val Inter = FontFamily(
    Font(
        R.font.inter,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.inter,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.inter,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

private val SousChefTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp,
    ),
)

private val SousChefShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
)

private val LightColors = lightColorScheme(
    primary = TerracottaLight,
    onPrimary = PaperLight,
    primaryContainer = TerracottaWashLight,
    onPrimaryContainer = InkLight,
    secondary = SageLight,
    onSecondary = PaperLight,
    background = PaperLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = InkMutedLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = SurfaceLight,
    surfaceContainerHighest = SurfaceLight,
    outline = InkMutedLight,
    outlineVariant = RuleLight,
    error = ErrorLight,
    onError = PaperLight,
)

private val DarkColors = darkColorScheme(
    primary = TerracottaDark,
    onPrimary = PaperDark,
    primaryContainer = TerracottaWashDark,
    onPrimaryContainer = InkDark,
    secondary = SageDark,
    onSecondary = PaperDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = InkMutedDark,
    surfaceContainerLowest = SurfaceDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceDark,
    surfaceContainerHighest = SurfaceDark,
    outline = InkMutedDark,
    outlineVariant = RuleDark,
    error = ErrorDark,
    onError = PaperDark,
)

/** The cookbook palette is the brand, so dynamic color stays off. */
@Composable
fun SousChefTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SousChefTypography,
        shapes = SousChefShapes,
        content = content,
    )
}
