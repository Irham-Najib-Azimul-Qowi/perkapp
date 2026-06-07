# Tutorial Perbaikan Perkapp — Part 3: EditAlat, Media Upload & File Map

Ini adalah lanjutan dari Part 1 dan Part 2. Di sini kita akan menyelesaikan
sisa fitur yang menjadi tanggung jawab **Najib** menurut planning.

---

## Tahap 6: Buat EditAlatScreen

### File BARU: `features/alat/ui/screen/EditAlatScreen.kt`
Lokasi: `app/src/main/java/com/example/perkapp/features/alat/ui/screen/EditAlatScreen.kt`

```kotlin
package com.example.perkapp.features.alat.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    // Isi form dengan data yang sudah ada (sekali saja)
    LaunchedEffect(alat) {
        if (!isLoaded && alat != null) {
            nama = alat!!.name
            kategori = alat!!.category
            jumlah = alat!!.total_qty.toString()
            kondisi = alat!!.condition
            isLoaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Alat") },
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
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedKondisi)
                    },
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val qty = jumlah.toIntOrNull() ?: 0
                    if (nama.isNotBlank() && kategori.isNotBlank() && qty > 0 && alat != null) {
                        val request = CreateAlatRequest(nama, kategori, qty, kondisi)
                        viewModel.updateAlat(alat!!, request)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Perubahan")
            }
        }
    }
}
```

---

## Tahap 7: Tambahkan fungsi updateAlat di ViewModel

Buka file `AlatViewModel.kt` yang sudah diperbaiki di Part 2, lalu tambahkan
fungsi ini **di dalam class AlatViewModel** (sebelum kurung kurawal terakhir):

```kotlin
    fun updateAlat(alat: AlatEntity, request: CreateAlatRequest) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                repository.updateAlat(alat, request)
                getAllAlat()
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }
```

Jangan lupa tambahkan import ini di bagian atas file:
```kotlin
import com.example.perkapp.features.alat.data.remote.CreateAlatRequest
```

---

## Tahap 8: Tambahkan Tombol Edit di DetailAlatScreen

Buka file `DetailAlatScreen.kt` (yang sudah dibuat di Part 2). Ubah agar ada
tombol Edit. Tambahkan parameter `onEditClick` dan tombol Button:

Ganti SELURUH isi `DetailAlatScreen.kt` dengan:

```kotlin
package com.example.perkapp.features.alat.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                title = { Text("Detail Alat") },
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
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            alat?.let { data ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = data.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Kategori: ${data.category}")
                        Text("Total Stok: ${data.total_qty}")
                        Text("Stok Tersedia: ${data.available_qty}")
                        Text("Kondisi: ${data.condition}")
                        Text("Status Sync: ${data.sync_status}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onEditClick(data.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Alat")
                }
            } ?: run {
                Text("Memuat data...")
            }
        }
    }
}
```

---

## Tahap 9: Update MainActivity — Tambah Route Edit

Buka file `MainActivity.kt` (yang sudah diperbaiki di Part 2). Tambahkan
route untuk **EditAlatScreen**. Ganti SELURUH isi `MainActivity.kt` dengan:

```kotlin
package com.example.perkapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.perkapp.core.database.AppDatabase
import com.example.perkapp.core.network.RetrofitClient
import com.example.perkapp.features.alat.api.AlatApiService
import com.example.perkapp.features.alat.data.repository.AlatRepository
import com.example.perkapp.features.alat.ui.screen.DetailAlatScreen
import com.example.perkapp.features.alat.ui.screen.EditAlatScreen
import com.example.perkapp.features.alat.ui.screen.InventarisScreen
import com.example.perkapp.features.alat.ui.screen.TambahAlatScreen
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModelFactory
import com.example.perkapp.ui.theme.PerkappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val alatDao = database.alatDao()
        val alatApi = RetrofitClient.instance.create(AlatApiService::class.java)
        val alatRepository = AlatRepository(alatApi, alatDao)
        val alatViewModelFactory = AlatViewModelFactory(alatRepository)

        setContent {
            PerkappTheme {
                val navController = rememberNavController()
                val alatViewModel: AlatViewModel = viewModel(factory = alatViewModelFactory)

                NavHost(
                    navController = navController,
                    startDestination = "inventaris"
                ) {
                    // Halaman utama: list alat
                    composable("inventaris") {
                        InventarisScreen(
                            viewModel = alatViewModel,
                            onAddClick = {
                                navController.navigate("tambah_alat")
                            },
                            onItemClick = { id ->
                                navController.navigate("detail_alat/$id")
                            }
                        )
                    }

                    // Halaman tambah alat baru
                    composable("tambah_alat") {
                        TambahAlatScreen(
                            viewModel = alatViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Halaman detail alat
                    composable(
                        route = "detail_alat/{alatId}",
                        arguments = listOf(
                            navArgument("alatId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val alatId = backStackEntry.arguments?.getString("alatId") ?: ""
                        DetailAlatScreen(
                            alatId = alatId,
                            viewModel = alatViewModel,
                            onBack = { navController.popBackStack() },
                            onEditClick = { id ->
                                navController.navigate("edit_alat/$id")
                            }
                        )
                    }

                    // Halaman edit alat
                    composable(
                        route = "edit_alat/{alatId}",
                        arguments = listOf(
                            navArgument("alatId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val alatId = backStackEntry.arguments?.getString("alatId") ?: ""
                        EditAlatScreen(
                            alatId = alatId,
                            viewModel = alatViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
```

---

## Peta Lengkap Semua File

Berikut adalah daftar SEMUA file yang perlu dibuat/diubah, beserta statusnya:

### File yang DIUBAH (sudah ada, ganti isinya):
```
gradle/libs.versions.toml                          ← Part 1
build.gradle.kts (root)                            ← Part 1
app/build.gradle.kts                               ← Part 1
app/src/main/AndroidManifest.xml                   ← Part 1
app/.../features/alat/data/local/AlatEntity.kt     ← Part 1
app/.../features/alat/data/local/AlatDao.kt        ← Part 1
app/.../features/alat/api/AlatApiService.kt        ← Part 1
app/.../features/alat/data/repository/AlatRepository.kt  ← Part 1
app/.../features/alat/ui/viewmodel/AlatViewModel.kt      ← Part 2 + Part 3
app/.../features/alat/ui/component/AlatCard.kt           ← Part 2
app/.../features/alat/ui/screen/InventarisScreen.kt      ← Part 2
app/.../features/alat/ui/screen/TambahAlatScreen.kt      ← Part 2
app/.../MainActivity.kt                                  ← Part 3
```

### File BARU yang perlu DIBUAT:
```
app/.../core/network/ApiResponse.kt                ← Part 1
app/.../core/network/RetrofitClient.kt             ← Part 1
app/.../core/database/AppDatabase.kt               ← Part 1
app/.../features/alat/ui/screen/DetailAlatScreen.kt      ← Part 3
app/.../features/alat/ui/screen/EditAlatScreen.kt        ← Part 3
```

### File yang TIDAK PERLU diubah (sudah benar):
```
app/.../features/alat/data/remote/AlatResponse.kt
app/.../features/alat/data/remote/CreateAlatRequest.kt
app/.../ui/theme/Color.kt
app/.../ui/theme/Theme.kt
app/.../ui/theme/Type.kt
```

> Catatan: `...` = `src/main/java/com/example/perkapp`

---

## Cara Membuat File Baru di Android Studio

1. Klik kanan pada folder tujuan di panel **Project**
2. Pilih **New > Kotlin Class/File**
3. Masukkan nama file (tanpa ekstensi `.kt`)
4. Pilih **File** dari dropdown
5. Paste kode dari tutorial ini

Untuk folder yang belum ada (misal `core/network/`):
1. Klik kanan pada folder `com/example/perkapp`
2. Pilih **New > Package**
3. Ketik `core.network`
4. Enter

---

## Alur Navigasi Aplikasi (Setelah Selesai)

```
┌─────────────────┐
│  InventarisScreen │ ← Halaman utama
│  (List semua alat)│
└───────┬─────────┘
        │
        ├── Klik (+) FAB ──────► TambahAlatScreen
        │                         │
        │                         └── Simpan → kembali ke list
        │
        └── Klik item alat ────► DetailAlatScreen
                                  │
                                  └── Klik Edit → EditAlatScreen
                                                   │
                                                   └── Simpan → kembali
```

---

## Selesai!

Dengan menyelesaikan Part 1 + Part 2 + Part 3, bagian **Najib** sudah
lengkap dan bisa dijalankan. Fitur yang berjalan:

- ✅ Lihat daftar inventaris alat
- ✅ Tambah alat baru
- ✅ Lihat detail alat
- ✅ Edit alat
- ✅ Data tersimpan offline (Room Database)
- ✅ Sync ke API jika server tersedia

Adam dan Reja bisa melanjutkan dengan menambahkan fitur Auth dan
Kegiatan di atas fondasi ini tanpa mengubah kode Najib.
