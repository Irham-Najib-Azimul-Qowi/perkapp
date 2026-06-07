# Tutorial Mengubah Desain Perkapp — Part 4
## Modifikasi EditAlatScreen, Dependency & Finishing

> **Prasyarat**: Sudah menyelesaikan [TUTORIAL_DESAIN_PART3.md](./TUTORIAL_DESAIN_PART3.md)

---

## Tahap 1: Modifikasi `EditAlatScreen.kt`

### File: `app/src/main/java/com/example/perkapp/features/alat/ui/screen/EditAlatScreen.kt`

**Hapus SELURUH isi file**, lalu ganti dengan kode berikut:

```kotlin
package com.example.perkapp.features.alat.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.perkapp.features.alat.data.remote.CreateAlatRequest
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlatScreen(
    alatId: String,
    viewModel: AlatViewModel,
    onBack: () -> Unit = {}
) {
    val alat by viewModel.selectedAlat.observeAsState()

    var nama by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("") }
    var jumlah by remember { mutableStateOf("") }
    var kondisi by remember { mutableStateOf("good") }
    var expandedKondisi by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }

    val kondisiOptions = listOf("good", "damaged")

    LaunchedEffect(alatId) {
        viewModel.getAlatById(alatId)
    }

    LaunchedEffect(alat) {
        if (!isLoaded && alat != null) {
            nama = alat!!.name
            kategori = alat!!.category
            jumlah = alat!!.total_qty.toString()
            kondisi = alat!!.condition
            isLoaded = true
        }
    }

    // Warna kustom untuk OutlinedTextField
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Alat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama Alat") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = kategori,
                onValueChange = { kategori = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kategori") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = jumlah,
                onValueChange = { jumlah = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Jumlah") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dropdown Kondisi
            ExposedDropdownMenuBox(
                expanded = expandedKondisi,
                onExpandedChange = { expandedKondisi = !expandedKondisi }
            ) {
                OutlinedTextField(
                    value = kondisi.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kondisi") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKondisi)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
                ExposedDropdownMenu(
                    expanded = expandedKondisi,
                    onDismissRequest = { expandedKondisi = false }
                ) {
                    kondisiOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                kondisi = option
                                expandedKondisi = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Simpan Perubahan
            Button(
                onClick = {
                    val qty = jumlah.toIntOrNull() ?: 0
                    if (nama.isNotBlank() && kategori.isNotBlank() && qty > 0 && alat != null) {
                        val request = CreateAlatRequest(nama, kategori, qty, kondisi)
                        viewModel.updateAlat(alat!!, request)
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Simpan Perubahan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
```

### Apa yang berubah?
1. **TopAppBar** hijau dengan teks putih (konsisten)
2. **OutlinedTextField** menggunakan `RoundedCornerShape(12.dp)` dan warna hijau
3. **Dropdown** nilai ditampilkan dengan huruf kapital pertama
4. **Tombol Simpan** full-width, rounded 12dp, warna primary
5. **Ditambahkan** `verticalScroll` agar form bisa di-scroll
6. **Dihapus** import yang tidak terpakai (`okhttp3.internal.threadName`)

---

## Tahap 2: Tambah Dependency Material Icons Extended (Opsional)

> ⚠️ Tahap ini **hanya perlu dilakukan** jika Anda ingin menggunakan ikon `CameraAlt` dan `Image` dari Material Icons Extended di TambahAlatScreen. Jika tidak, lewati tahap ini.

### File: `gradle/libs.versions.toml`

Tambahkan baris berikut di bagian `[libraries]`:

```toml
# Cari bagian [libraries] dan tambahkan baris ini di bawah baris material3:
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
```

### File: `app/build.gradle.kts`

Tambahkan baris berikut di bagian `dependencies`:

```kotlin
// Cari bagian dependencies dan tambahkan baris ini:
implementation(libs.androidx.compose.material.icons.extended)
```

> Setelah menambahkan, lakukan **Sync Gradle** (File → Sync Project with Gradle Files)

### Alternatif Tanpa Dependency Tambahan

Jika Anda **tidak ingin** menambah dependency baru, ubah baris ikon di `TambahAlatScreen.kt`:

```kotlin
// GANTI baris ini:
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image

// MENJADI:
// (hapus kedua import di atas)

// Dan ganti penggunaannya:
// GANTI: Icons.Default.CameraAlt → Icons.Default.Add
// GANTI: Icons.Default.Image → Icons.Default.Add
```

---

## Tahap 3: Cek `MainActivity.kt` (Tidak Perlu Diubah)

### File: `app/src/main/java/com/example/perkapp/MainActivity.kt`

> ✅ **Kesimpulan: File ini TIDAK perlu diubah.** Penjelasan di bawah ini hanya untuk memastikan semuanya sudah benar.

Pada **Part 1**, kita sudah mengubah `Theme.kt` agar menggunakan palet warna kustom (hijau) dan **tidak** menggunakan `dynamicColor` dari Material You. Karena perubahan tema sudah dilakukan di `Theme.kt`, maka `MainActivity.kt` **tidak perlu dimodifikasi** — cukup verifikasi saja.

### Yang Perlu Diverifikasi

Buka file `MainActivity.kt` dan cari bagian `setContent` (sekitar baris 43-44). Pastikan `PerkappTheme` dipanggil **tanpa parameter `dynamicColor`**:

```kotlin
// ✅ BENAR — seperti ini sudah benar, tidak perlu diubah
setContent {
    PerkappTheme {
        val navController = rememberNavController()
        // ... sisa kode navigasi ...
    }
}
```

### Jika Masih Ada Parameter `dynamicColor`

Jika di kode kamu masih ada parameter `dynamicColor`, seperti contoh di bawah, **hapus parameter tersebut**:

```kotlin
// ❌ SALAH — hapus parameter dynamicColor
setContent {
    PerkappTheme(dynamicColor = false) {  // ← hapus "(dynamicColor = false)"
        // ...
    }
}
```

Menjadi:

```kotlin
// ✅ BENAR — cukup panggil PerkappTheme tanpa parameter
setContent {
    PerkappTheme {
        // ...
    }
}
```

### Kenapa Tidak Perlu Diubah?

Karena di `Theme.kt` (Part 1), fungsi `PerkappTheme` sudah kita definisikan ulang **tanpa parameter `dynamicColor`**:

```kotlin
// Di Theme.kt — sudah diubah di Part 1
@Composable
fun PerkappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Langsung pakai palet kustom, BUKAN dynamic color
    val colorScheme = if (darkTheme) PerkDarkColorScheme else PerkLightColorScheme
    // ...
}
```

Jadi pemanggilan `PerkappTheme { ... }` di `MainActivity.kt` otomatis sudah menggunakan warna hijau kustom kita. **Tidak ada aksi yang diperlukan** di tahap ini.

---

## Tahap 4: Ringkasan Semua Perubahan

Berikut daftar semua file yang dimodifikasi:

| # | File | Part | Perubahan |
|---|------|------|-----------|
| 1 | `ui/theme/Color.kt` | Part 1 | Palet warna baru (hijau) |
| 2 | `ui/theme/Type.kt` | Part 1 | Font Inter + semua text style |
| 3 | `ui/theme/Theme.kt` | Part 1 | Light/Dark scheme kustom |
| 4 | `res/values/colors.xml` | Part 1 | Warna XML baru |
| 5 | `res/font/inter_*.ttf` | Part 1 | 4 file font Inter (BARU) |
| 6 | `features/.../InventarisScreen.kt` | Part 2 | TopAppBar hijau, FAB, empty state |
| 7 | `features/.../AlatCard.kt` | Part 2 | Card rounded, chip, warna kondisi |
| 8 | `features/.../DetailAlatScreen.kt` | Part 3 | Layout detail baru, chip, info row |
| 9 | `features/.../TambahAlatScreen.kt` | Part 3 | Form rounded, outlined buttons |
| 10 | `features/.../EditAlatScreen.kt` | Part 4 | Form rounded, konsisten |
| 11 | `gradle/libs.versions.toml` | Part 4 | (Opsional) material-icons-extended |
| 12 | `app/build.gradle.kts` | Part 4 | (Opsional) dependency icons |

---

## ✅ Checklist Final

Setelah menyelesaikan semua part, pastikan:

- [ ] **Part 1**: Color, Type, Theme sudah diubah + font Inter tersedia
- [ ] **Part 2**: InventarisScreen & AlatCard sudah didesain ulang
- [ ] **Part 3**: DetailAlatScreen & TambahAlatScreen sudah didesain ulang
- [ ] **Part 4**: EditAlatScreen sudah didesain ulang
- [ ] Sync Gradle berhasil
- [ ] Build project berhasil tanpa error
- [ ] Jalankan di emulator/device dan pastikan:
  - TopAppBar berwarna hijau (`#22C55E`)
  - Font Inter terlihat di semua teks
  - Card memiliki sudut membulat
  - Tombol berwarna hijau dengan sudut membulat
  - Chip kategori berwarna hijau muda
  - FAB bulat berwarna hijau

---

## 🎨 Mapping Style Guide → Implementasi

| Elemen Style Guide | Implementasi di Kode |
|--------------------|---------------------|
| Primary `#22C55E` | `MaterialTheme.colorScheme.primary` |
| Secondary `#16A34A` | `MaterialTheme.colorScheme.secondary` |
| Tertiary `#FF887C` | `MaterialTheme.colorScheme.tertiary` |
| Neutral `#1F2937` | `MaterialTheme.colorScheme.onSurface` |
| Font Inter | `MaterialTheme.typography.*` (otomatis) |
| Headline | `MaterialTheme.typography.headlineLarge/Medium/Small` |
| Body | `MaterialTheme.typography.bodyLarge/Medium/Small` |
| Label | `MaterialTheme.typography.labelLarge/Medium/Small` |
| Primary Button | `Button(colors = ButtonDefaults.buttonColors(containerColor = primary))` |
| Outlined Button | `OutlinedButton(...)` |
| Search Bar | `OutlinedTextField(shape = RoundedCornerShape(12.dp))` |
| Chip/Label | `Box(background = primaryContainer, shape = RoundedCornerShape(8.dp))` |

---

## 🔧 Troubleshooting

### Error: "Unresolved reference: font" di Type.kt
- Pastikan folder `app/src/main/res/font/` ada
- Pastikan 4 file font Inter sudah ada di dalamnya
- Pastikan nama file **huruf kecil** dengan **underscore**: `inter_regular.ttf`
- Rebuild project: Build → Rebuild Project

### Error: "Unresolved reference: CameraAlt" atau "Image"
- Pilih salah satu:
  - Tambahkan dependency `material-icons-extended` (Tahap 2 di Part 4)
  - Atau ganti ikon dengan `Icons.Default.Add`

### Warna masih ungu/biru (Material You)
- Pastikan `Theme.kt` sudah diubah dan **TIDAK** menggunakan `dynamicColor`
- Pastikan `PerkappTheme` tidak menerima parameter `dynamicColor = true`
- Uninstall aplikasi dari device/emulator, lalu install ulang

### Font tidak berubah
- Pastikan file `.ttf` ada di `res/font/`
- Clean dan rebuild: Build → Clean Project, lalu Build → Rebuild Project

> 🎉 **Selamat!** Desain aplikasi Perkapp Anda sekarang sudah sesuai dengan UI Style Guide!
