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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
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
    var expandedStatus by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }

    val statusOptions = listOf("BERLANGSUNG", "SELESAI")

    // Find current activity
    val aktivitas = remember(uiState.aktivitasList, kegiatanId) {
        uiState.aktivitasList.find { it.id == kegiatanId }
    }

    LaunchedEffect(kegiatanId) {
        viewModel.loadActivities()
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
                    if (judul.isNotBlank() && lokasi.isNotBlank() && tanggal.isNotBlank()) {
                        viewModel.updateKegiatan(
                            id = kegiatanId,
                            judul = judul,
                            kategori = "Umum",
                            lokasi = lokasi,
                            tanggal = tanggal,
                            status = status,
                            onSuccess = {
                                Toast.makeText(context, "Kegiatan berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        )
                    } else {
                        Toast.makeText(context, "Mohon lengkapi semua field!", Toast.LENGTH_SHORT).show()
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
