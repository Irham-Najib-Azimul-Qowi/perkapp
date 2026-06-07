# Tutorial Mengubah Desain Perkapp — Part 1
## Setup Fondasi Desain: Color, Typography & Theme

> **Referensi**: UI Style Guide yang dilampirkan
> **Teknologi**: Jetpack Compose + Material3

---

## Daftar Warna dari Style Guide

| Nama | Kode Hex | Kegunaan |
|------|----------|----------|
| **Primary** | `#22C55E` | Warna utama (tombol, FAB, TopBar) |
| **Secondary** | `#16A34A` | Warna pendukung (aksen, ikon aktif) |
| **Tertiary** | `#FF887C` | Warna aksen (peringatan, label khusus) |
| **Neutral** | `#1F2937` | Warna teks & background gelap |

**Font**: Inter (Google Font)

---

## Tahap 1: Modifikasi `Color.kt`

### File: `app/src/main/java/com/example/perkapp/ui/theme/Color.kt`

**Hapus SELURUH isi file**, lalu ganti dengan kode berikut:

```kotlin
package com.example.perkapp.ui.theme

import androidx.compose.ui.graphics.Color

// === PALET WARNA DARI UI STYLE GUIDE ===

// Primary - Hijau utama
val PerkPrimary = Color(0xFF22C55E)
val PerkPrimaryLight = Color(0xFF4ADE80)
val PerkPrimaryDark = Color(0xFF16A34A)
val PerkPrimaryContainer = Color(0xFFDCFCE7)
val PerkOnPrimaryContainer = Color(0xFF052E16)

// Secondary - Hijau gelap
val PerkSecondary = Color(0xFF16A34A)
val PerkSecondaryLight = Color(0xFF22C55E)
val PerkSecondaryDark = Color(0xFF15803D)
val PerkSecondaryContainer = Color(0xFFBBF7D0)
val PerkOnSecondaryContainer = Color(0xFF052E16)

// Tertiary - Coral/Salmon
val PerkTertiary = Color(0xFFFF887C)
val PerkTertiaryLight = Color(0xFFFFB4AB)
val PerkTertiaryDark = Color(0xFFE57373)
val PerkTertiaryContainer = Color(0xFFFFDAD6)
val PerkOnTertiaryContainer = Color(0xFF410002)

// Neutral - Dark Blue Gray
val PerkNeutral = Color(0xFF1F2937)
val PerkNeutralLight = Color(0xFF374151)
val PerkNeutralLighter = Color(0xFF6B7280)
val PerkNeutralSurface = Color(0xFFF9FAFB)
val PerkNeutralOutline = Color(0xFFD1D5DB)

// Background & Surface
val PerkBackground = Color(0xFFFAFAFA)
val PerkSurface = Color(0xFFFFFFFF)
val PerkSurfaceVariant = Color(0xFFF1F5F9)
val PerkOnBackground = Color(0xFF1F2937)
val PerkOnSurface = Color(0xFF1F2937)

// Status Colors
val PerkSuccess = Color(0xFF22C55E)
val PerkWarning = Color(0xFFFBBF24)
val PerkError = Color(0xFFEF4444)
val PerkInfo = Color(0xFF3B82F6)
```

---

## Tahap 2: Modifikasi `Type.kt` (Tambah Font Inter)

### File: `app/src/main/java/com/example/perkapp/ui/theme/Type.kt`

**Hapus SELURUH isi file**, lalu ganti dengan kode berikut:

```kotlin
package com.example.perkapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.perkapp.R

// Font Inter dari resource
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val Typography = Typography(
    // Headline - untuk judul besar
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Title - untuk judul section
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Body - untuk teks konten
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Label - untuk label kecil, tombol, chip
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

---

## Tahap 3: Download Font Inter

Anda perlu mendownload file font Inter dan menaruhnya di folder `res/font/`.

### Langkah-langkah:

1. **Buat folder `font`** di dalam `app/src/main/res/font/` (klik kanan pada folder `res` → New → Directory → ketik `font`)

2. **Download font Inter** dari Google Fonts: https://fonts.google.com/specimen/Inter

3. **Salin 4 file font** ke folder `app/src/main/res/font/`:
   - `inter_regular.ttf`
   - `inter_medium.ttf`
   - `inter_semibold.ttf`
   - `inter_bold.ttf`

> ⚠️ **PENTING**: Nama file font harus **huruf kecil semua** dan menggunakan **underscore** (bukan strip). Contoh: `inter_regular.ttf`, BUKAN `Inter-Regular.ttf`. Jika file yang didownload bernama `Inter-Regular.ttf`, rename menjadi `inter_regular.ttf`.

> 💡 **Alternatif lebih mudah di Android Studio**: Klik kanan pada folder `res/font` → New → Font Resource File → cari "Inter" → download otomatis dari Google Fonts.

---

## Tahap 4: Modifikasi `Theme.kt`

### File: `app/src/main/java/com/example/perkapp/ui/theme/Theme.kt`

**Hapus SELURUH isi file**, lalu ganti dengan kode berikut:

```kotlin
package com.example.perkapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

// === DARK COLOR SCHEME ===
private val PerkDarkColorScheme = darkColorScheme(
    primary = PerkPrimaryLight,
    onPrimary = PerkOnPrimaryContainer,
    primaryContainer = PerkPrimaryDark,
    onPrimaryContainer = PerkPrimaryContainer,

    secondary = PerkSecondaryLight,
    onSecondary = PerkOnSecondaryContainer,
    secondaryContainer = PerkSecondaryDark,
    onSecondaryContainer = PerkSecondaryContainer,

    tertiary = PerkTertiaryLight,
    onTertiary = PerkOnTertiaryContainer,
    tertiaryContainer = PerkTertiaryDark,
    onTertiaryContainer = PerkTertiaryContainer,

    background = Color(0xFF111827),
    onBackground = Color(0xFFF9FAFB),
    surface = Color(0xFF1F2937),
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = Color(0xFF374151),
    onSurfaceVariant = Color(0xFF9CA3AF),

    outline = Color(0xFF4B5563),
    outlineVariant = Color(0xFF374151),

    error = Color(0xFFFF6B6B),
    onError = Color(0xFF410002)
)

@Composable
fun PerkappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Gunakan palet warna kustom, BUKAN dynamic color
    val colorScheme = if (darkTheme) PerkDarkColorScheme else PerkLightColorScheme

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
```

> ⚠️ **Perubahan penting**: Dynamic color dihapus agar warna dari Style Guide selalu dipakai, tidak di-override oleh warna Material You dari sistem Android.

---

## Tahap 5: Modifikasi `colors.xml`

### File: `app/src/main/res/values/colors.xml`

**Hapus SELURUH isi file**, lalu ganti dengan kode berikut:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Primary -->
    <color name="perk_primary">#FF22C55E</color>
    <color name="perk_primary_dark">#FF16A34A</color>

    <!-- Secondary -->
    <color name="perk_secondary">#FF16A34A</color>

    <!-- Tertiary -->
    <color name="perk_tertiary">#FFFF887C</color>

    <!-- Neutral -->
    <color name="perk_neutral">#FF1F2937</color>

    <!-- Basic -->
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
```

---

## ✅ Checklist Part 1

Setelah selesai Part 1, pastikan:

- [ ] File `Color.kt` sudah berisi palet warna baru (hijau, bukan ungu)
- [ ] File `Type.kt` sudah menggunakan font `InterFontFamily`
- [ ] Folder `res/font/` sudah ada dan berisi 4 file font Inter (`.ttf`)
- [ ] File `Theme.kt` sudah menggunakan `PerkLightColorScheme` dan `PerkDarkColorScheme`
- [ ] File `colors.xml` sudah diperbarui
- [ ] Sync Gradle di Android Studio (File → Sync Project with Gradle Files)

> ➡️ **Lanjut ke [TUTORIAL_DESAIN_PART2.md](./TUTORIAL_DESAIN_PART2.md)** untuk modifikasi komponen UI (TopAppBar, FAB, Card, Button)
