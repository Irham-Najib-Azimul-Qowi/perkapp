package com.example.perkapp.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.perkapp.navigation.Screen
import com.example.perkapp.model.AlatEntity
import com.example.perkapp.viewmodel.AlatViewModel
import com.example.perkapp.viewmodel.AktivitasViewModel
import com.example.perkapp.model.UserInfo
import java.util.Calendar
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * FUNGSI: TambahKegiatanScreen
 * TUJUAN: Halaman form (wizard 2 tahap) untuk mencatat kegiatan baru.
 *
 * ALUR LOGIKA PENGERJAAN:
 * Tahap 1: Pengisian info umum kegiatan (nama, tanggal pinjam/kembali, lokasi, deskripsi, peminjam).
 * Tahap 2: Pemilihan alat dari inventaris dan/atau alat tambahan dari luar.
 *
 * @param navController Navigasi utama
 * @param viewModel ViewModel alat
 * @param kegiatanViewModel ViewModel kegiatan (aktivitas)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TambahKegiatanScreen(
    navController: NavController,
    viewModel: AlatViewModel,
    kegiatanViewModel: AktivitasViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Status langkah wizard saat ini (Tahap 1 atau Tahap 2)
    var currentStep by rememberSaveable { mutableStateOf(1) }

    // --- STATE UNTUK STEP 1: Informasi Dasar Kegiatan ---
    var namaAktivitas by rememberSaveable { mutableStateOf("") } // Input nama/judul kegiatan
    var tanggalPinjam by rememberSaveable { mutableStateOf("") } // Input tanggal mulai kegiatan
    var tanggalKembali by rememberSaveable { mutableStateOf("") } // Input tanggal akhir kegiatan
    val daftarPeminjam = remember { mutableStateListOf<String>() } // Daftar nama peminjam yang sudah ditambahkan (berupa Chip)
    var peminjamInput by rememberSaveable { mutableStateOf("") } // Teks pencarian saat mengetik nama peminjam
    var lokasi by rememberSaveable { mutableStateOf("") } // Lokasi kegiatan
    var deskripsi by rememberSaveable { mutableStateOf("") } // Detail/catatan tambahan kegiatan
    var dropdownTerbuka by rememberSaveable { mutableStateOf(false) } // Status dropdown pencarian user aktif atau tidak

    // --- STATE UNTUK STEP 2: Pemilihan Alat ---
    // Mengambil daftar semua alat dari ViewModel, diobservasi secara reaktif
    val alatList by viewModel.alatList.observeAsState(emptyList<AlatEntity>())
    // Menyimpan kuantitas alat yang dipilih dalam format [ID Alat -> Jumlah Pinjam]
    var selectedQuantities by remember { mutableStateOf(emptyMap<String, Int>()) }
    // Menyimpan daftar alat eksternal/luar yang baru ditambahkan secara manual
    var externalTools by remember { mutableStateOf(emptyList<String>()) }

    // Listen to external tools added
    // Mendengarkan/menangkap pengembalian data dari halaman "TambahAlatLuarScreen" (jika user menambahkan alat luar)
    val navBackStackEntry = navController.currentBackStackEntry
    val newToolState = navBackStackEntry?.savedStateHandle?.getLiveData<String>("alat_luar_nama")?.observeAsState()
    LaunchedEffect(newToolState?.value) {
        newToolState?.value?.let { newTool ->
            // Jika ada alat baru dikembalikan dan belum ada dalam list, maka tambahkan
            if (newTool.isNotBlank() && !externalTools.contains(newTool)) {
                externalTools = externalTools + newTool
                // Hapus data dari savedStateHandle agar tidak ditambahkan berulang kali jika layar di-recompose
                navBackStackEntry.savedStateHandle.remove<String>("alat_luar_nama")
            }
        }
    }

    // Pemicu (Trigger) yang dijalankan sekali (saat layar ini pertama kali dibuka)
    LaunchedEffect(Unit) {
        viewModel.getAllAlat() // Memuat data semua alat dari inventaris lokal/API
        kegiatanViewModel.fetchRegisteredUsers() // Memuat daftar nama pengguna sistem untuk autocompletion peminjam
    }

    // Konfigurasi sistem kalender untuk fitur Date Picker Dialog
    val calendar = Calendar.getInstance()
    val tahunKini = calendar.get(Calendar.YEAR)
    val bulanKini = calendar.get(Calendar.MONTH)
    val hariKini = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerPinjam = DatePickerDialog(
        context,
        { _, tahun, bulan, hari ->
            tanggalPinjam = String.format("%02d/%02d/%04d", hari, bulan + 1, tahun)
        },
        tahunKini, bulanKini, hariKini
    )

    val datePickerKembali = DatePickerDialog(
        context,
        { _, tahun, bulan, hari ->
            tanggalKembali = String.format("%02d/%02d/%04d", hari, bulan + 1, tahun)
        },
        tahunKini, bulanKini, hariKini
    )

    // Mengambil state daftar anggota yang terdaftar di aplikasi (untuk fitur autocompletion peminjam)
    val daftarAnggota by kegiatanViewModel.registeredUsers.collectAsState(initial = emptyList())
    // Mengambil info pengguna yang sedang login
    val currentUserInfo by kegiatanViewModel.currentUserInfo.collectAsState()
    
    // Mendefinisikan gaya warna yang seragam (konsisten) untuk semua kolom input teks (TextField) di halaman ini
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
                        text = if (currentStep == 1) "Tambah Kegiatan (1/2)" else "Tambah Perlengkapan (2/2)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentStep == 2) {
                                currentStep = 1
                            } else {
                                navController.popBackStack()
                            }
                        }
                    ) {
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
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Step Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Step 1: Info Kegiatan
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (currentStep >= 1) MaterialTheme.colorScheme.primary else Color(0xFFD9E3F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1", color = if (currentStep >= 1) Color.White else Color(0xFF3D4A3D), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Info Kegiatan", fontSize = 11.sp, color = if (currentStep >= 1) MaterialTheme.colorScheme.primary else Color(0xFF6D7B6C), fontWeight = FontWeight.Bold)
                }

                // Garis Penghubung
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(2.dp)
                        .background(if (currentStep == 2) MaterialTheme.colorScheme.primary else Color(0xFFD9E3F6))
                        .padding(horizontal = 8.dp)
                )

                // Step 2: Perlengkapan
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (currentStep == 2) MaterialTheme.colorScheme.primary else Color(0xFFD9E3F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("2", color = if (currentStep == 2) Color.White else Color(0xFF3D4A3D), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Perlengkapan", fontSize = 11.sp, color = if (currentStep == 2) MaterialTheme.colorScheme.primary else Color(0xFF6D7B6C))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentStep == 1) {
                // STEP 1 FORM
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Nama Aktivitas
                    OutlinedTextField(
                        value = namaAktivitas,
                        onValueChange = { namaAktivitas = it },
                        label = { Text("Nama Aktivitas") },
                        placeholder = { Text("e.g. Field Research Seminar") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )

                    // 2. Tanggal Pinjam & Tanggal Kembali (Berdampingan)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = tanggalPinjam,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tanggal Pinjam") },
                            placeholder = { Text("dd/mm/yyyy") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { datePickerPinjam.show() },
                            enabled = false,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Pilih Tanggal Pinjam",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { datePickerPinjam.show() }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = Color.Transparent,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = tanggalKembali,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tanggal Kembali") },
                            placeholder = { Text("dd/mm/yyyy") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { datePickerKembali.show() },
                            enabled = false,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Pilih Tanggal Kembali",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { datePickerKembali.show() }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledContainerColor = Color.Transparent,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // 3. Peminjam (Dropdown & Chips)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Daftar Peminjam",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Chips list
                        if (daftarPeminjam.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                daftarPeminjam.forEach { nama ->
                                    InputChip(
                                        selected = true,
                                        onClick = { daftarPeminjam.remove(nama) },
                                        label = { Text(nama) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Hapus",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = peminjamInput,
                                onValueChange = {
                                    peminjamInput = it
                                    dropdownTerbuka = true
                                },
                                label = { Text("Peminjam") },
                                placeholder = { Text("Ketik nama kustom atau cari user...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            dropdownTerbuka = true
                                        }
                                    },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Cari Peminjam",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                singleLine = true
                            )

                            val anggotaDifilter = daftarAnggota.filter { 
                                it.contains(peminjamInput, ignoreCase = true) && !daftarPeminjam.contains(it) && it != currentUserInfo?.nama
                            }

                            DropdownMenu(
                                expanded = dropdownTerbuka && (anggotaDifilter.isNotEmpty() || peminjamInput.isNotBlank()),
                                onDismissRequest = { dropdownTerbuka = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                // Mendaftarkan custom name jika diketik
                                if (peminjamInput.isNotBlank() && !daftarPeminjam.contains(peminjamInput)) {
                                    DropdownMenuItem(
                                        text = { Text("+ Tambah '$peminjamInput'") },
                                        onClick = {
                                            daftarPeminjam.add(peminjamInput.trim())
                                            peminjamInput = ""
                                            dropdownTerbuka = false
                                        }
                                    )
                                }
                                
                                // Daftar anggota sistem yang terdaftar
                                anggotaDifilter.forEach { nama ->
                                    DropdownMenuItem(
                                        text = { Text(nama) },
                                        onClick = {
                                            daftarPeminjam.add(nama)
                                            peminjamInput = ""
                                            dropdownTerbuka = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3.5 Lokasi
                    OutlinedTextField(
                        value = lokasi,
                        onValueChange = { lokasi = it },
                        label = { Text("Lokasi Kegiatan") },
                        placeholder = { Text("e.g. Ruang Rapat Utama, Lab Kimia") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        singleLine = true
                    )

                    // 4. Deskripsi
                    OutlinedTextField(
                        value = deskripsi,
                        onValueChange = { deskripsi = it },
                        label = { Text("Deskripsi") },
                        placeholder = { Text("Brief details about activity goals...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        maxLines = 5
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions for Step 1
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            // Validasi tahap 1: Mencegah perpindahan ke tahap 2 jika ada kolom penting yang kosong
                            when {
                                namaAktivitas.isBlank() -> Toast.makeText(context, "Nama Kegiatan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                tanggalPinjam.isBlank() -> Toast.makeText(context, "Tanggal tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                daftarPeminjam.isEmpty() -> Toast.makeText(context, "Peminjam harus diisi minimal 1", Toast.LENGTH_SHORT).show()
                                lokasi.isBlank() -> Toast.makeText(context, "Lokasi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                else -> currentStep = 2 // Jika semua validasi lolos, pindah ke Langkah 2 (Pemilihan Alat)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Lanjut: Pilih Perlengkapan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }

                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Batal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // STEP 2 FORM (Pilihan Alat Inventaris)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pilih Alat dari Inventaris",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                navController.navigate(Screen.TambahAlatLuar.route)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Alat Luar", fontSize = 12.sp)
                        }
                    }

                    if (alatList.isEmpty() && externalTools.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Belum ada alat di inventaris.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (externalTools.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Alat Luar yang Dipinjam",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(externalTools) { compositeVal ->
                                    val parts = compositeVal.split("|")
                                    val name = parts.getOrNull(0) ?: ""
                                    val imagePath = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
                                    val bitmap = com.example.perkapp.util.rememberAsyncImage(imagePath)

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                bitmap?.let {
                                                    androidx.compose.foundation.Image(
                                                        bitmap = it.asImageBitmap(),
                                                        contentDescription = "Gambar Alat Luar",
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                } ?: Text(
                                                    text = name.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Text(
                                                text = name,
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text("1 unit (Pinjaman)", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            items(alatList) { alat ->
                                ToolSelectionCard(
                                    alat = alat,
                                    qtySelected = selectedQuantities[alat.id] ?: 0,
                                    onQtyChange = { newQty ->
                                        selectedQuantities = selectedQuantities.toMutableMap().apply {
                                            put(alat.id, newQty)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions for Step 2
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                // Memfilter map alat: Hanya ambil alat yang jumlahnya (qty) lebih dari 0
                                val selectedToolsList = selectedQuantities.entries
                                    .filter { it.value > 0 }
                                    .map { Pair(it.key, it.value) }

                                // Validasi tahap 2: Harus ada alat yang dipinjam, baik dari inventaris maupun dari alat luar
                                if (selectedToolsList.isEmpty() && externalTools.isEmpty()) {
                                    Toast.makeText(context, "Pilih minimal 1 perlengkapan atau tambah alat dari luar!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Menggabungkan array nama peminjam menjadi satu string utuh yang dipisah dengan koma
                                val peminjamString = daftarPeminjam.joinToString(", ")

                                // Mengirim seluruh data form ke ViewModel untuk disimpan ke database dan disinkronkan ke server
                                kegiatanViewModel.insertKegiatan(
                                    judul = namaAktivitas,
                                    kategori = "Umum",
                                    lokasi = lokasi,
                                    tanggal = tanggalPinjam,
                                    status = "BERLANGSUNG",
                                    peminjam = peminjamString,
                                    deskripsi = deskripsi,
                                    tools = selectedToolsList,
                                    externalTools = externalTools.toList(),
                                    onSuccess = {
                                        // Callback jika proses simpan berhasil
                                        Toast.makeText(context, "Kegiatan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack() // Keluar dari halaman form dan kembali ke beranda
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Simpan Kegiatan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = { currentStep = 1 }) {
                            Text("Kembali ke Info Kegiatan", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * FUNGSI: ToolSelectionCard
 * TUJUAN: Komponen UI berupa kartu alat untuk halaman Tambah Kegiatan (Tahap 2).
 * Digunakan untuk memilih jumlah alat (kuantitas) yang ingin dipinjam.
 *
 * @param alat Data alat yang tersedia di inventaris
 * @param qtySelected Jumlah yang sedang dipilih pengguna
 * @param onQtyChange Callback ketika pengguna menambah atau mengurangi jumlah pinjaman
 */
@Composable
fun ToolSelectionCard(
    alat: AlatEntity,
    qtySelected: Int,
    onQtyChange: (Int) -> Unit
) {
    val bitmap = com.example.perkapp.util.rememberAsyncImage(alat.image_path)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Gambar Alat",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } ?: Text(
                    text = alat.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alat.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tersedia: ${alat.available_qty} unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { if (qtySelected > 0) onQtyChange(qtySelected - 1) },
                    enabled = qtySelected > 0,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (qtySelected > 0) MaterialTheme.colorScheme.primaryContainer else Color(0xFFD9E3F6),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Kurang",
                        modifier = Modifier.size(16.dp),
                        tint = if (qtySelected > 0) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                Text(
                    text = "$qtySelected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(20.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                IconButton(
                    onClick = { if (qtySelected < alat.available_qty) onQtyChange(qtySelected + 1) },
                    enabled = qtySelected < alat.available_qty,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (qtySelected < alat.available_qty) MaterialTheme.colorScheme.primaryContainer else Color(0xFFD9E3F6),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah",
                        modifier = Modifier.size(16.dp),
                        tint = if (qtySelected < alat.available_qty) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}
