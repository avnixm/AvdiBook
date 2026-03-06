package com.avnixm.avdibook.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.avnixm.avdibook.data.prefs.AppPreferences

private val LightFallbackColorScheme = lightColorScheme(
    primary = AmberPrimaryLight,
    onPrimary = OnAmberPrimaryLight,
    primaryContainer = AmberPrimaryContainerLight,
    onPrimaryContainer = OnAmberPrimaryContainerLight,
    secondary = OliveSecondaryLight,
    onSecondary = OnOliveSecondaryLight,
    secondaryContainer = OliveSecondaryContainerLight,
    onSecondaryContainer = OnOliveSecondaryContainerLight,
    tertiary = SlateTertiaryLight,
    onTertiary = OnSlateTertiaryLight,
    tertiaryContainer = SlateTertiaryContainerLight,
    onTertiaryContainer = OnSlateTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkFallbackColorScheme = darkColorScheme(
    primary = AmberPrimaryDark,
    onPrimary = OnAmberPrimaryDark,
    primaryContainer = AmberPrimaryContainerDark,
    onPrimaryContainer = OnAmberPrimaryContainerDark,
    secondary = OliveSecondaryDark,
    onSecondary = OnOliveSecondaryDark,
    secondaryContainer = OliveSecondaryContainerDark,
    onSecondaryContainer = OnOliveSecondaryContainerDark,
    tertiary = SlateTertiaryDark,
    onTertiary = OnSlateTertiaryDark,
    tertiaryContainer = SlateTertiaryContainerDark,
    onTertiaryContainer = OnSlateTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

val LocalReducedMotion = staticCompositionLocalOf { false }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun AvdiBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    pureBlackDark: Boolean = false,
    textScalePreset: AppPreferences.TextScalePreset = AppPreferences.TextScalePreset.STANDARD,
    reducedMotionEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkFallbackColorScheme
        else -> LightFallbackColorScheme
    }
    val colorScheme = if (darkTheme && pureBlackDark) {
        darkColorScheme(
            primary = baseScheme.primary,
            onPrimary = baseScheme.onPrimary,
            primaryContainer = baseScheme.primaryContainer,
            onPrimaryContainer = baseScheme.onPrimaryContainer,
            secondary = baseScheme.secondary,
            onSecondary = baseScheme.onSecondary,
            secondaryContainer = baseScheme.secondaryContainer,
            onSecondaryContainer = baseScheme.onSecondaryContainer,
            tertiary = baseScheme.tertiary,
            onTertiary = baseScheme.onTertiary,
            tertiaryContainer = baseScheme.tertiaryContainer,
            onTertiaryContainer = baseScheme.onTertiaryContainer,
            error = baseScheme.error,
            onError = baseScheme.onError,
            errorContainer = baseScheme.errorContainer,
            onErrorContainer = baseScheme.onErrorContainer,
            background = PureBlack,
            onBackground = baseScheme.onBackground,
            surface = PureBlack,
            onSurface = baseScheme.onSurface,
            surfaceVariant = baseScheme.surfaceVariant,
            onSurfaceVariant = baseScheme.onSurfaceVariant,
            outline = baseScheme.outline
        )
    } else {
        baseScheme
    }

    CompositionLocalProvider(LocalReducedMotion provides reducedMotionEnabled) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography(textScalePreset.scaleMultiplier),
            shapes = AppShapes,
            content = content
        )
    }
}
