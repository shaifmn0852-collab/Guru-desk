package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SemanticColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val isDark: Boolean,
)

private val LightSemanticColors = SemanticColors(
    success = EmeraldSuccessLight,
    successContainer = EmeraldSuccessContainerLight,
    onSuccessContainer = OnEmeraldSuccessContainerLight,
    warning = AmberWarningLight,
    warningContainer = AmberWarningContainerLight,
    onWarningContainer = OnAmberWarningContainerLight,
    isDark = false,
)

private val DarkSemanticColors = SemanticColors(
    success = EmeraldSuccessDark,
    successContainer = EmeraldSuccessContainerDark,
    onSuccessContainer = OnEmeraldSuccessContainerDark,
    warning = AmberWarningDark,
    warningContainer = AmberWarningContainerDark,
    onWarningContainer = OnAmberWarningContainerDark,
    isDark = true,
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = OnNavyPrimary,
    primaryContainer = NavyPrimaryContainer,
    onPrimaryContainer = OnNavyPrimaryContainer,
    secondary = SaffronSecondary,
    onSecondary = OnSaffronSecondary,
    secondaryContainer = SaffronSecondaryContainer,
    onSecondaryContainer = OnSaffronSecondaryContainer,
    tertiary = TealTertiary,
    onTertiary = OnTealTertiary,
    tertiaryContainer = TealTertiaryContainer,
    onTertiaryContainer = OnTealTertiaryContainer,
    background = AlabasterBackground,
    onBackground = OnAlabasterBackground,
    surface = PureCardSurface,
    onSurface = OnPureCardSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = OnWarmSurfaceVariant,
    surfaceContainerLowest = PureCardSurface,
    surfaceContainerLow = Color(0xFFFAF9F6),
    surfaceContainer = WarmSurfaceVariant,
    surfaceContainerHigh = Color(0xFFEBE8DF),
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
    error = CrimsonError,
    onError = OnCrimsonError,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = OnCrimsonErrorContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkNavyPrimary,
    onPrimary = DarkOnNavyPrimary,
    primaryContainer = DarkNavyPrimaryContainer,
    onPrimaryContainer = DarkOnNavyPrimaryContainer,
    secondary = DarkSaffronSecondary,
    onSecondary = DarkOnSaffronSecondary,
    secondaryContainer = DarkSaffronSecondaryContainer,
    onSecondaryContainer = DarkOnSaffronSecondaryContainer,
    tertiary = DarkTealTertiary,
    onTertiary = DarkOnTealTertiary,
    tertiaryContainer = DarkTealTertiaryContainer,
    onTertiaryContainer = DarkOnTealTertiaryContainer,
    background = MidnightBackground,
    onBackground = OnMidnightBackground,
    surface = SlateSurface,
    onSurface = OnSlateSurface,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = OnSlateSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0B1120),
    surfaceContainerLow = Color(0xFF152033),
    surfaceContainer = SlateSurface,
    surfaceContainerHigh = SlateSurfaceVariant,
    outline = SlateOutline,
    outlineVariant = SlateOutlineVariant,
    error = DarkCoralError,
    onError = DarkOnCoralError,
    errorContainer = DarkCoralErrorContainer,
    onErrorContainer = DarkOnCoralErrorContainer,
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun GuruDeskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
