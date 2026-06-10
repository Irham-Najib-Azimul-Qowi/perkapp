/**
 * File: Theme.kt
 *
 * FUNGSI UTAMA:
 * File ini mengonfigurasi tema visual (Theme) Material 3 untuk seluruh aplikasi Perkapp.
 *
 * PENJELASAN MENDALAM:
 * Dalam Jetpack Compose, tema didefinisikan menggunakan MaterialTheme yang menerima:
 * - colorScheme: Skema warna (dari Color.kt) yang diterapkan ke semua komponen Material
 * - typography: Gaya tipografi/font (dari Type.kt) untuk konsistensi teks
 * - content: Seluruh UI Composable yang dibungkus oleh tema ini
 *
 * Aplikasi ini hanya menggunakan LIGHT THEME (tanpa dark mode).
 * Tema juga mengatur warna status bar agar sesuai dengan warna primary aplikasi.
 *
 * CARA KERJA:
 * PerkappTheme dipanggil di MainActivity.kt → membungkus PerkappApp() →
 * semua komponen di dalamnya otomatis menggunakan warna dan tipografi yang ditentukan.
 */
package com.example.perkapp.ui.theme

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
// Memetakan warna-warna dari Color.kt ke dalam slot warna Material 3
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
