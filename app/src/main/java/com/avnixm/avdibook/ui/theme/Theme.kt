package com.avnixm.avdibook.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightFallbackColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = OnBluePrimaryLight,
    primaryContainer = BluePrimaryContainerLight,
    onPrimaryContainer = OnBluePrimaryContainerLight,
    secondary = IndigoSecondaryLight,
    onSecondary = OnIndigoSecondaryLight,
    secondaryContainer = IndigoSecondaryContainerLight,
    onSecondaryContainer = OnIndigoSecondaryContainerLight,
    tertiary = OrchidTertiaryLight,
    onTertiary = OnOrchidTertiaryLight,
    tertiaryContainer = OrchidTertiaryContainerLight,
    onTertiaryContainer = OnOrchidTertiaryContainerLight,
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
    primary = BluePrimaryDark,
    onPrimary = OnBluePrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = OnBluePrimaryContainerDark,
    secondary = IndigoSecondaryDark,
    onSecondary = OnIndigoSecondaryDark,
    secondaryContainer = IndigoSecondaryContainerDark,
    onSecondaryContainer = OnIndigoSecondaryContainerDark,
    tertiary = OrchidTertiaryDark,
    onTertiary = OnOrchidTertiaryDark,
    tertiaryContainer = OrchidTertiaryContainerDark,
    onTertiaryContainer = OnOrchidTertiaryContainerDark,
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

@Composable
fun AvdiBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    pureBlackDark: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
