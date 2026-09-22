package com.familyguard.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = TrustBlue40,
    onPrimary = NeutralGrey99,
    primaryContainer = TrustBlue90,
    onPrimaryContainer = TrustBlue20,
    secondary = TealSecondary40,
    onSecondary = NeutralGrey99,
    secondaryContainer = TealSecondary80,
    background = NeutralGrey99,
    surface = NeutralGrey99,
    surfaceVariant = NeutralGrey95,
    onBackground = NeutralGrey10,
    onSurface = NeutralGrey10,
    error = SosRed40,
    onError = NeutralGrey99
)

private val DarkColors = darkColorScheme(
    primary = TrustBlue80,
    onPrimary = TrustBlue20,
    primaryContainer = TrustBlue40,
    onPrimaryContainer = TrustBlue90,
    secondary = TealSecondary80,
    onSecondary = NeutralGrey10,
    background = NeutralGrey10,
    surface = NeutralGrey10,
    surfaceVariant = NeutralGrey30,
    onBackground = NeutralGrey99,
    onSurface = NeutralGrey99,
    error = SosRed80,
    onError = NeutralGrey10
)

@Composable
fun FamilyGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FamilyGuardTypography,
        content = content
    )
}
