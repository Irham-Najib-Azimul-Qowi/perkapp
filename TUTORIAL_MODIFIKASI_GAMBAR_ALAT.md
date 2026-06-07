# Tutorial Penambahan Fitur Gambar Alat (Kamera & Galeri) — Bagian Najib

Tutorial ini akan memandu Anda untuk memodifikasi kode lokal agar aplikasi Perkapp Anda bisa:
1. **Mengambil Gambar dari Kamera** langsung dari HP.
2. **Memilih Gambar dari Album/Galeri** HP.
3. **Menyimpan Gambar** tersebut ke dalam database lokal (Room).
4. **Menampilkan Gambar** pada daftar halaman utama (*AlatCard*) dan halaman *Detail Alat*.

> [!IMPORTANT]
> **PENTING SEBELUM MULAI:**  
> Karena kita akan memodifikasi struktur tabel database lokal Room (menambahkan kolom baru `image_path`), pastikan setelah melakukan tutorial ini, Anda **meng-uninstall (menghapus) aplikasi Perkapp lama dari HP** terlebih dahulu sebelum menekan Run lagi. Ini dilakukan untuk menghindari *crash* migrasi database Room.

---

## 🛠️ Langkah 1: Update Model Data (`AlatEntity.kt` & `CreateAlatRequest.kt`)

Kita perlu menambahkan kolom baru bernama `image_path` pada model data lokal dan jaringan.

### 1. Buka `AlatEntity.kt`
Ubah isi file `app/src/main/java/com/example/perkapp/features/alat/data/local/AlatEntity.kt` dengan menambahkan kolom `image_path`:

```kotlin
package com.example.perkapp.features.alat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alat")
data class AlatEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val total_qty: Int,
    val available_qty: Int,
    val condition: String,
    val sync_status: String = "Synced",
    val image_path: String? = null // Tambahkan Kolom Ini
)
```

### 2. Buka `CreateAlatRequest.kt`
Ubah isi file `app/src/main/java/com/example/perkapp/features/alat/data/remote/CreateAlatRequest.kt` agar mencakup parameter opsional `image_path`:

```kotlin
package com.example.perkapp.features.alat.data.remote

data class CreateAlatRequest(
    val name: String,
    val category: String,
    val total_qty: Int,
    val condition: String,
    val image_path: String? = null // Tambahkan Parameter Ini
)
```

---

## 🛠️ Langkah 2: Buat Helper Pengolah Gambar (`ImageUtils.kt`)

Kita akan membuat utilitas helper agar proses merender gambar Uri dan menyimpan jepretan kamera langsung dari bitmap ke file penyimpanan lokal HP berjalan otomatis tanpa perlu pustaka eksternal tambahan.

Buat file baru di path **`app/src/main/java/com/example/perkapp/core/utils/ImageUtils.kt`** dengan isi sebagai berikut:

```kotlin
package com.example.perkapp.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    
    // Fungsi memuat Uri string gambar menjadi Bitmap agar bisa dirender di Compose Image
    fun loadBitmapFromUri(context: Context, uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Fungsi menyimpan jepretan Kamera (Bitmap) ke File Cache Lokal agar bisa mendapatkan URI String
    fun saveBitmapToFile(context: Context, bitmap: Bitmap): String? {
        return try {
            val filename = "img_${UUID.randomUUID()}.jpg"
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
```

---

## 🛠️ Langkah 3: Update Repositori & ViewModel

Kita perlu memodifikasi fungsi tambah dan edit agar parameter `imagePath` ikut disimpan.

### 1. Buka `AlatRepository.kt`
Sesuaikan fungsi `createAlat` dan `updateAlat` di `app/src/main/java/com/example/perkapp/features/alat/data/repository/AlatRepository.kt` untuk menerima `imagePath`:

```kotlin
    suspend fun createAlat(name: String, category: String, totalQty: Int, condition: String, imagePath: String?) {
        val localEntity = AlatEntity(
            id  = UUID.randomUUID().toString(),
            name = name,
            category = category,
            total_qty =  totalQty,
            available_qty =  totalQty,
            condition = condition,
            sync_status =  "pending",
            image_path = imagePath // Set Image Path
        )
        dao.insertAlat(localEntity)

        try {
            val request = CreateAlatRequest(name, category, totalQty, condition, imagePath)
            val response = api.createAlat(request)
            if (response.isSuccessful) {
                response.body()?.data?.let { apiAlat ->
                    dao.deleteAlat(localEntity.id)
                    dao.insertAlat(
                        AlatEntity(
                            id = apiAlat.id,
                            name = apiAlat.name,
                            category = apiAlat.category,
                            total_qty =  apiAlat.total_qty,
                            available_qty =  apiAlat.available_qty,
                            condition = apiAlat.condition,
                            sync_status =  "synced",
                            image_path = imagePath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateAlat(alat: AlatEntity, request: CreateAlatRequest) {
        val updated = alat.copy(
            name =  request.name,
            category =  request.category,
            total_qty =  request.total_qty,
            condition =  request.condition,
            sync_status =  "pending",
            image_path = request.image_path // Gunakan image_path yang baru
        )
        dao.updateAlat(updated)

        try {
            val response = api.updateAlat(alat.id, request)
            if (response.isSuccessful) {
                dao.updateAlat(updated.copy(sync_status =  "synced"))
            }
        } catch ( e: Exception) {
            e.printStackTrace()
        }
    }
```

### 2. Buka `AlatViewModel.kt`
Sesuaikan fungsi pemanggilan di `app/src/main/java/com/example/perkapp/features/alat/ui/viewmodel/AlatViewModel.kt`:

```kotlin
    fun createAlat(name: String, category: String, totalQty: Int, condition: String, imagePath: String?) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                repository.createAlat(name, category, totalQty, condition, imagePath)
                getAllAlat()
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }
```

---

## 🛠️ Langkah 4: Modifikasi Tampilan Layar UI (Tambah & Edit Alat)

Kita akan membuat komponen tombol pemilihan media serta pratinjau (*preview*) gambar di dalam form.

### 1. Buka `TambahAlatScreen.kt`
Buka file `app/src/main/java/com/example/perkapp/features/alat/ui/screen/TambahAlatScreen.kt`. Modifikasi kodenya agar memiliki launcher kamera dan galeri:

```kotlin
package com.example.perkapp.features.alat.ui.screen

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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

    // State Gambar
    var imageUriString by remember { mutableStateOf<String?>(null) }
    var bitmapPreview by remember { mutableStateOf<Bitmap?>(null) }

    // Launcher untuk memilih dari Galeri
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUriString = it.toString()
            bitmapPreview = ImageUtils.loadBitmapFromUri(context, imageUriString)
        }
    }

    // Launcher untuk mengambil foto dari Kamera
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Alat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama Alat") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = kategori,
                onValueChange = { kategori = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kategori") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = jumlah,
                onValueChange = { jumlah = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Jumlah") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expandedKondisi,
                onExpandedChange = { expandedKondisi = !expandedKondisi }
            ) {
                OutlinedTextField(
                    value = kondisi,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kondisi") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKondisi) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedKondisi,
                    onDismissRequest = { expandedKondisi = false }
                ) {
                    kondisiOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                kondisi = option
                                expandedKondisi = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // UI Pemilihan Gambar
            Text(text = "Gambar Alat", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { cameraLauncher.launch() }) {
                    Text("Kamera")
                }
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Galeri")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tampilan Preview
            bitmapPreview?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Preview Gambar",
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val qty = jumlah.toIntOrNull() ?: 0
                    if (nama.isNotBlank() && kategori.isNotBlank() && qty > 0) {
                        viewModel.createAlat(nama, kategori, qty, kondisi, imageUriString)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Alat")
            }
        }
    }
}
```

---

## 🛠️ Langkah 5: Tampilkan Gambar di List & Detail

### 1. Perbarui Kartu Alat (`AlatCard.kt`)
Buka file `app/src/main/java/com/example/perkapp/features/alat/ui/component/AlatCard.kt`. Modifikasi kodenya agar memuat pratinjau gambar mini di sisi kiri kartu teks:

```kotlin
package com.example.perkapp.features.alat.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.perkapp.core.utils.ImageUtils
import com.example.perkapp.features.alat.data.local.AlatEntity

@Composable
fun AlatCard(
    alat: AlatEntity,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val bitmap = remember(alat.image_path) {
        ImageUtils.loadBitmapFromUri(context, alat.image_path)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Gambar mini disebelah kiri
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Gambar Alat",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .padding(end = 16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alat.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Kategori: ${alat.category}")
                Row {
                    Text(text = "Stok: ${alat.available_qty}/${alat.total_qty}")
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "Kondisi: ${alat.condition}")
                }
            }
        }
    }
}
```

### 2. Perbarui Halaman Detail (`DetailAlatScreen.kt`)
Buka file `app/src/main/java/com/example/perkapp/features/alat/ui/screen/DetailAlatScreen.kt`. Tambahkan tampilan gambar di atas informasi detail alat:

```kotlin
// Pastikan bagian di dalam Scaffold detail alat memuat pemanggilan gambar seperti ini:
val context = LocalContext.current
val bitmap = remember(alat?.image_path) {
    ImageUtils.loadBitmapFromUri(context, alat?.image_path)
}

// Kemudian di dalam Column detail alat, taruh komponen Image di atas teks informasi:
bitmap?.let {
    Image(
        bitmap = it.asImageBitmap(),
        contentDescription = "Foto Alat",
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(bottom = 16.dp)
    )
}
```
*(Sesuaikan dengan struktur `DetailAlatScreen.kt` yang Anda miliki saat ini)*
