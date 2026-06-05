package com.example.perkapp.core.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// === LIGHT COLOR SCHEME (sesuai Style Guide) ===
private val PerkLightColorScheme = lightColorScheme(
    primary = PerkPrimary,
    onPrimary = Color.White,
    primaryContainer = PerkPrimaryContainer,
    onPrimaryContainer = PerkOnPrimaryContainer,

    secondary = PerkSecondary,
    onSecondary = Color.White,
    secondaryContainer = PerkSecondaryContainer,
    onSecondaryContainer = PerkOnSecondaryContainer,

    tertiary = PerkTertiary,
    onTertiary = Color.White,
    tertiaryContainer = PerkTertiaryContainer,
    onTertiaryContainer = PerkOnTertiaryContainer,

    background = PerkBackground,
    onBackground = PerkOnBackground,
    surface = PerkSurface,
    onSurface = PerkOnSurface,
    surfaceVariant = PerkSurfaceVariant,
    onSurfaceVariant = PerkNeutralLight,

    outline = PerkNeutralOutline,
    outlineVariant = Color(0xFFE5E7EB),

    error = PerkError,
    onError = Color.White
)

@Composable
fun PerkappTheme(
    content: @Composable () -> Unit
) {
    // Selalu gunakan tema terang (light-only)
    val colorScheme = PerkLightColorScheme

    // Atur warna status bar
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
