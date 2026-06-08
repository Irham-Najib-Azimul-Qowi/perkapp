package com.example.perkapp.core.features.kegiatan.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Calendar

/**
 * EditKegiatanScreen — Halaman untuk mengubah (mengedit) data kegiatan yang sudah ada.
 *
 * Form otomatis terisi dengan data kegiatan sebelumnya (seperti judul, lokasi, dll).
 * Pengguna dapat mengubah info tersebut dan menyimpannya ke sistem.
 *
 * @param kegiatanId ID kegiatan yang akan diedit
 * @param onBack Fungsi callback untuk kembali ke layar sebelumnya
 * @param viewModel ViewModel aktivitas yang diinjeksi via Hilt
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditKegiatanScreen(
    kegiatanId: String,
    onBack: () -> Unit,
    viewModel: AktivitasViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var judul by remember { mutableStateOf("") }
    var lokasi by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("BERLANGSUNG") }
    var deskripsi by remember { mutableStateOf("") }
    val daftarPeminjam = remember { mutableStateListOf<String>() }
    var peminjamInput by remember { mutableStateOf("") }
    
    var expandedStatus by remember { mutableStateOf(false) }
    var dropdownTerbuka by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }

    val statusOptions = listOf("BERLANGSUNG", "SELESAI")

    // Find current activity
    val aktivitas = remember(uiState.aktivitasList, kegiatanId) {
        uiState.aktivitasList.find { it.id == kegiatanId }
    }

    LaunchedEffect(kegiatanId) {
        viewModel.loadActivities()
        viewModel.fetchRegisteredUsers()
    }

    LaunchedEffect(aktivitas) {
        if (!isLoaded && aktivitas != null) {
            judul = aktivitas.judul
            lokasi = aktivitas.deskripsi.replace("Lokasi: ", "")
            tanggal = aktivitas.tanggal
            status = when (aktivitas.status) {
                StatusAktivitas.BERLANGSUNG -> "BERLANGSUNG"
                StatusAktivitas.SELESAI -> "SELESAI"
                StatusAktivitas.DRAFT -> "DRAFT"
            }
            deskripsi = aktivitas.realDeskripsi
            daftarPeminjam.clear()
            val names = aktivitas.peminjam.split(",").map { it.trim() }.filter { it.isNotBlank() }
            daftarPeminjam.addAll(names)
            isLoaded = true
        }
    }

    // Date picker setup
    val calendar = Calendar.getInstance()
    val tahunKini = calendar.get(Calendar.YEAR)
    val bulanKini = calendar.get(Calendar.MONTH)
    val hariKini = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, tahun, bulan, hari ->
            tanggal = String.format("%02d/%02d/%04d", hari, bulan + 1, tahun)
        },
        tahunKini, bulanKini, hariKini
    )

    val daftarAnggota by viewModel.registeredUsers.collectAsState(initial = emptyList())
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
                        "Edit Kegiatan",
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
                value = judul,
                onValueChange = { judul = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama Kegiatan") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = lokasi,
                onValueChange = { lokasi = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Lokasi") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date Picker Field
            OutlinedTextField(
                value = tanggal,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tanggal Kegiatan") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Peminjam (Dropdown & Chips)
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
                        label = { Text("Tambah Peminjam") },
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
                        it.contains(peminjamInput, ignoreCase = true) && !daftarPeminjam.contains(it) 
                    }

                    DropdownMenu(
                        expanded = dropdownTerbuka && (anggotaDifilter.isNotEmpty() || peminjamInput.isNotBlank()),
                        onDismissRequest = { dropdownTerbuka = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        // Tambah nama kustom
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
                        
                        // Daftar anggota sistem
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

            Spacer(modifier = Modifier.height(12.dp))

            // Deskripsi
            OutlinedTextField(
                value = deskripsi,
                onValueChange = { deskripsi = it },
                label = { Text("Deskripsi") },
                placeholder = { Text("Brief details about activity...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dropdown Status
            ExposedDropdownMenuBox(
                expanded = expandedStatus,
                onExpandedChange = { expandedStatus = !expandedStatus }
            ) {
                OutlinedTextField(
                    value = if (status == "BERLANGSUNG") "In Progress" else "Completed",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
                ExposedDropdownMenu(
                    expanded = expandedStatus,
                    onDismissRequest = { expandedStatus = false }
                ) {
                    statusOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(if (option == "BERLANGSUNG") "In Progress" else "Completed") },
                            onClick = {
                                status = option
                                expandedStatus = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        judul.isBlank() -> Toast.makeText(context, "Nama Kegiatan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        lokasi.isBlank() -> Toast.makeText(context, "Lokasi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        tanggal.isBlank() -> Toast.makeText(context, "Tanggal tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        daftarPeminjam.isEmpty() -> Toast.makeText(context, "Peminjam harus diisi minimal 1", Toast.LENGTH_SHORT).show()
                        else -> {
                            val peminjamString = daftarPeminjam.joinToString(", ")
                            viewModel.updateKegiatan(
                                id = kegiatanId,
                                judul = judul,
                                kategori = "Umum",
                                lokasi = lokasi,
                                tanggal = tanggal,
                                status = status,
                                peminjam = peminjamString,
                                deskripsi = deskripsi,
                                onSuccess = {
                                    Toast.makeText(context, "Kegiatan berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            )
                        }
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
