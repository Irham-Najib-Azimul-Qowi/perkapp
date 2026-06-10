/**
 * File: Color.kt
 *
 * FUNGSI UTAMA:
 * File ini mendefinisikan seluruh palet warna (color palette) aplikasi Perkapp
 * sesuai dengan UI Style Guide yang telah ditentukan.
 *
 * PENJELASAN MENDALAM:
 * Warna-warna ini diorganisasikan ke dalam beberapa kelompok semantik:
 * - Primary (Hijau utama): Warna dominan untuk tombol, header, dan elemen interaktif utama
 * - Secondary (Hijau gelap): Warna pendukung untuk elemen sekunder
 * - Tertiary (Coral/Salmon): Warna aksen untuk elemen yang perlu menonjol
 * - Neutral (Dark Blue Gray): Warna netral untuk teks dan background
 * - Status Colors: Warna untuk indikator sukses, peringatan, error, dan info
 *
 * Warna-warna ini akan dipasangkan ke dalam MaterialTheme (lihat Theme.kt)
 * sehingga komponen Material 3 otomatis menggunakan warna yang konsisten.
 *
 * PERAN DALAM ARSITEKTUR:
 * Color.kt → digunakan oleh Theme.kt (lightColorScheme) → diterapkan via MaterialTheme
 * → seluruh komponen UI secara otomatis memakai warna dari skema ini
 */
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
val PerkTertiary = Color(0xFFFF8B7C)
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
