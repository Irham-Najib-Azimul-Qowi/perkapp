# Tutorial Perbaikan Perkapp — Part 2: UI & MainActivity

## Tahap 4: Perbaiki UI Screens {#tahap-4}

### File: `features/alat/ui/viewmodel/AlatViewModel.kt`
Ganti SELURUH isi file:

```kotlin
package com.example.perkapp.features.alat.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.perkapp.features.alat.data.local.AlatEntity
import com.example.perkapp.features.alat.data.repository.AlatRepository
import kotlinx.coroutines.launch

class AlatViewModel(
    private val repository: AlatRepository
) : ViewModel() {

    val alatList = MutableLiveData<List<AlatEntity>>()
    val selectedAlat = MutableLiveData<AlatEntity?>()
    val isLoading = MutableLiveData(false)
    val errorMessage = MutableLiveData<String?>()

    fun getAllAlat() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                alatList.value = repository.getAllAlat()
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    fun createAlat(name: String, category: String, totalQty: Int, condition: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                repository.createAlat(name, category, totalQty, condition)
                getAllAlat() // Refresh list
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    fun getAlatById(id: String) {
        viewModelScope.launch {
            selectedAlat.value = repository.getAlatById(id)
        }
    }
}

class AlatViewModelFactory(
    private val repository: AlatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

---

### File: `features/alat/ui/component/AlatCard.kt`
Ganti SELURUH isi file:

```kotlin
package com.example.perkapp.features.alat.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.perkapp.features.alat.data.local.AlatEntity

@Composable
fun AlatCard(
    alat: AlatEntity,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
```

---

### File: `features/alat/ui/screen/InventarisScreen.kt`
Ganti SELURUH isi file:

```kotlin
package com.example.perkapp.features.alat.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.perkapp.features.alat.ui.component.AlatCard
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarisScreen(
    viewModel: AlatViewModel,
    onAddClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {}
) {
    val alatList by viewModel.alatList.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)

    LaunchedEffect(Unit) {
        viewModel.getAllAlat()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Inventaris Alat") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Alat")
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                items(alatList) { alat ->
                    AlatCard(
                        alat = alat,
                        onClick = { onItemClick(alat.id) }
                    )
                }
            }
        }
    }
}
```

---

### File BARU: `features/alat/ui/screen/DetailAlatScreen.kt`
Lokasi: `app/src/main/java/com/example/perkapp/features/alat/ui/screen/DetailAlatScreen.kt`

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
    onBack: () -> Unit = {}
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
            } ?: run {
                Text("Memuat data...")
            }
        }
    }
}
```

---

### File: `features/alat/ui/screen/TambahAlatScreen.kt`
Ganti SELURUH isi file:

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TambahAlatScreen(
    viewModel: AlatViewModel,
    onBack: () -> Unit = {}
) {
    var nama by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("") }
    var jumlah by remember { mutableStateOf("") }
    var kondisi by remember { mutableStateOf("good") }
    var expandedKondisi by remember { mutableStateOf(false) }

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
                    if (nama.isNotBlank() && kategori.isNotBlank() && qty > 0) {
                        viewModel.createAlat(nama, kategori, qty, kondisi)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan")
            }
        }
    }
}
```

---

## Tahap 5: Hubungkan ke MainActivity {#tahap-5}

### File: `MainActivity.kt`
Ganti SELURUH isi file:

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
import com.example.perkapp.features.alat.ui.screen.InventarisScreen
import com.example.perkapp.features.alat.ui.screen.TambahAlatScreen
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModelFactory
import com.example.perkapp.ui.theme.PerkappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inisialisasi dependensi
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

                    composable("tambah_alat") {
                        TambahAlatScreen(
                            viewModel = alatViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

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

## Checklist Akhir

Setelah semua perubahan selesai, pastikan:

- [ ] Sync Gradle berhasil tanpa error
- [ ] Build project (Build > Make Project) berhasil
- [ ] Run di emulator/device, halaman Inventaris tampil
- [ ] Klik tombol (+) bisa membuka halaman Tambah Alat
- [ ] Isi form lalu klik Simpan, data tersimpan ke database lokal
- [ ] Klik item di list bisa membuka halaman Detail Alat

## Catatan Penting

1. **API belum jalan?** Tidak masalah. Karena project ini **offline-first**, data tetap tersimpan di database lokal (Room). Ketika API server sudah ready, data akan otomatis sync.

2. **Bagian Adam & Reja:** Tutorial ini sudah menyertakan infrastruktur minimal (RetrofitClient, AppDatabase, Navigation) yang dibutuhkan agar bagian Najib bisa berjalan. Adam dan Reja bisa melanjutkan di atas fondasi ini.

3. **Urutan pengerjaan file:**
   1. `libs.versions.toml` → Sync Gradle
   2. `build.gradle.kts` (root & app) → Sync Gradle
   3. `AndroidManifest.xml`
   4. File-file di `core/`
   5. File-file di `features/alat/data/`
   6. File-file di `features/alat/api/`
   7. File-file di `features/alat/ui/`
   8. `MainActivity.kt`
