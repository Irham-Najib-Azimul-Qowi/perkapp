# Tutorial Mengubah Desain Perkapp — Part 3
## Modifikasi DetailAlatScreen & TambahAlatScreen

> **Prasyarat**: Sudah menyelesaikan [TUTORIAL_DESAIN_PART2.md](./TUTORIAL_DESAIN_PART2.md)

---

## Tahap 1: Modifikasi `DetailAlatScreen.kt`

### File: `app/src/main/java/com/example/perkapp/features/alat/ui/screen/DetailAlatScreen.kt`

**Hapus SELURUH isi file**, lalu ganti dengan kode berikut:

```kotlin
package com.example.perkapp.features.alat.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.perkapp.core.utils.ImageUtils
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailAlatScreen(
    alatId: String,
    viewModel: AlatViewModel,
    onBack: () -> Unit = {},
    onEditClick: (String) -> Unit = {}
) {
    val alat by viewModel.selectedAlat.observeAsState()

    LaunchedEffect(alatId) {
        viewModel.getAlatById(alatId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detail Alat",
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
        val context = LocalContext.current
        val bitmap = remember(alat?.image_path) {
            ImageUtils.loadBitmapFromUri(context, alat?.image_path)
        }

        alat?.let { data ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Gambar Alat
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Foto Alat",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        } ?: Text(
                            text = data.name.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Info Detail
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = data.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Label Kategori
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = data.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Baris info detail
                        DetailInfoRow(label = "Total Stok", value = "${data.total_qty}")
                        DetailInfoRow(label = "Stok Tersedia", value = "${data.available_qty}")

                        val kondisiColor = if (data.condition == "good") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                        DetailInfoRow(
                            label = "Kondisi",
                            value = data.condition.replaceFirstChar { it.uppercase() },
                            valueColor = kondisiColor
                        )

                        DetailInfoRow(
                            label = "Status Sync",
                            value = data.sync_status.replaceFirstChar { it.uppercase() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Edit
                Button(
                    onClick = { onEditClick(data.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Edit Alat",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } ?: run {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (valueColor != Color.Unspecified) valueColor
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}
```

### Apa yang berubah?
1. **TopAppBar** hijau primary dengan ikon kembali putih
2. **Gambar** di dalam Card dengan rounded corner, ada fallback huruf jika tidak ada gambar
3. **Detail info** menggunakan layout Row label-value yang rapi
4. **Kategori** dalam chip hijau muda
5. **Tombol Edit** dengan ikon dan rounded corner 12dp
6. **Loading state** mengganti teks "Memuat data..." dengan `CircularProgressIndicator`
7. **Scrollable** — ditambahkan `verticalScroll`

---

## Tahap 2: Modifikasi `TambahAlatScreen.kt`

### File: `app/src/main/java/com/example/perkapp/features/alat/ui/screen/TambahAlatScreen.kt`

**Hapus SELURUH isi file**, lalu ganti dengan kode berikut:

```kotlin
package com.example.perkapp.features.alat.ui.screen

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.perkapp.core.utils.ImageUtils
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TambahAlatScreen(
    viewModel: AlatViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    var nama by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("") }
    var jumlah by remember { mutableStateOf("") }
    var kondisi by remember { mutableStateOf("good") }
    var expandedKondisi by remember { mutableStateOf(false) }
    var imageUriString by remember { mutableStateOf<String?>(null) }
    var bitmapPreview by remember { mutableStateOf<Bitmap?>(null) }

    val gallerylauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUriString = it.toString()
            bitmapPreview = ImageUtils.loadBitmapFromUri(context, imageUriString)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val savedUriString = ImageUtils.saveBitmapToFile(context, it)
            if (savedUriString != null) {
                imageUriString = savedUriString
                bitmapPreview = it
            }
        }
    }

    val kondisiOptions = listOf("good", "damaged")

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
                        "Tambah Alat",
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
            // Form fields dengan styling baru
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

            Spacer(modifier = Modifier.height(20.dp))

            // Section Gambar
            Text(
                text = "Gambar Alat",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tombol Kamera (Outlined style)
                OutlinedButton(
                    onClick = { cameraLauncher.launch() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kamera")
                }

                // Tombol Galeri (Outlined style)
                OutlinedButton(
                    onClick = { gallerylauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galeri")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview Gambar
            bitmapPreview?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Preview Gambar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Simpan (Primary style)
            Button(
                onClick = {
                    val qty = jumlah.toIntOrNull() ?: 0
                    if (nama.isNotBlank() && kategori.isNotBlank() && qty > 0) {
                        viewModel.createAlat(nama, kategori, qty, kondisi, imageUriString ?: "")
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
                    "Simpan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
```

### Apa yang berubah?
1. **TopAppBar** hijau dengan teks putih (konsisten dengan screen lain)
2. **OutlinedTextField** menggunakan `RoundedCornerShape(12.dp)` dan warna fokus hijau primary
3. **Tombol Kamera & Galeri** menjadi `OutlinedButton` dengan ikon Material Icons
4. **Preview gambar** di-clip dengan rounded corner
5. **Tombol Simpan** full-width dengan rounded corner dan warna primary
6. **Ditambahkan** `verticalScroll` agar form bisa di-scroll
7. **Dropdown** nilai ditampilkan dengan huruf kapital pertama

> ⚠️ **Catatan**: Ikon `CameraAlt` dan `Image` memerlukan dependency `material-icons-extended`. Lihat Part 4 untuk cara menambahkannya. Jika tidak mau menambah dependency, ganti dengan `Icons.Default.Add` saja.

---

## ✅ Checklist Part 3

Setelah selesai Part 3, pastikan:

- [ ] `DetailAlatScreen.kt` sudah diperbarui — TopAppBar hijau, card rounded, chip kategori
- [ ] `TambahAlatScreen.kt` sudah diperbarui — form field rounded, tombol styled
- [ ] Build project dan pastikan tidak ada error

> ➡️ **Lanjut ke [TUTORIAL_DESAIN_PART4.md](./TUTORIAL_DESAIN_PART4.md)** untuk modifikasi EditAlatScreen dan penambahan dependency
