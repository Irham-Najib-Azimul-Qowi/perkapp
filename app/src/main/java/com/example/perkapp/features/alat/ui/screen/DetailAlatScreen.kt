package com.example.perkapp.features.alat.ui.screen

import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.perkapp.core.utils.rememberAsyncImage
import com.example.perkapp.features.alat.ui.viewmodel.AlatViewModel

/**
 * DetailAlatScreen — Halaman untuk melihat rincian sebuah barang.
 *
 * Menampilkan foto besar, nama, kategori, jumlah total dan tersedia,
 * serta tombol untuk mengedit atau menghapus barang tersebut.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailAlatScreen(
    alatId: String,
    viewModel: AlatViewModel,
    onBack: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onDeleteClick: (String) -> Unit = {}
) {
    // Membaca data alat spesifik yang sedang kita minta
    val alat by viewModel.selectedAlat.observeAsState()
    
    // State untuk memunculkan atau menyembunyikan Pop-Up peringatan "Hapus"
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Meminta ViewModel untuk menarik data alat dari Room berdasarkan ID-nya
    LaunchedEffect(alatId) {
        viewModel.getAlatById(alatId)
    }

    // --- Dialog Konfirmasi Hapus ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Alat") },
            text = { Text("Apakah Anda yakin ingin menghapus alat ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick(alatId)
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
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
        
        // Memuat gambar secara asinkron di latar belakang
        val bitmap = rememberAsyncImage(alat?.image_path)

        alat?.let { data ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // --- Kotak Gambar Alat ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape =  RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        // Jika gambar sukses dimuat, tampilkan gambarnya
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Foto Alat",
                                contentScale = ContentScale.Crop, // Agar gambar menutupi penuh kotak
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        } ?: Text(
                            // Jika gambar gagal/tidak ada, tampilkan inisial 2 huruf dari nama barang
                            text = data.name.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Kartu Info Detail ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape =  RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Nama dan badge status sinkronisasi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = data.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            // Tanda apakah data ini aman di server (Synced) atau masih nyangkut di HP (Pending)
                            val isSynced = data.sync_status == "synced"
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSynced) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFFF3CD),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSynced) Icons.Default.Cloud else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSynced) MaterialTheme.colorScheme.primary else Color(0xFF856404)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSynced) "Synced" else "Pending",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSynced) MaterialTheme.colorScheme.primary else Color(0xFF856404)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Label Kategori
                        Box(
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape =  RoundedCornerShape(8.dp)
                            ).padding(horizontal = 12.dp, vertical =  4.dp)
                        ) {
                            Text(
                                text = data.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tiga baris statistik detail barang
                        DetailInfoRow(label = "Total Stok", value = "${data.total_qty}")
                        DetailInfoRow(label = "Stok Tersedia", value = "${data.available_qty}")

                        // Mewarnai status kondisi alat: "good" -> primary (hijau/biru), lainnya -> merah
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
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Tombol Edit ---
                Button(
                    onClick = {onEditClick(data.id)},
                    modifier = Modifier.fillMaxWidth().height(52.dp),
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

                Spacer(modifier = Modifier.height(12.dp))

                // --- Tombol Hapus (Outlined Merah) ---
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Hapus Alat",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } ?: run {
            // Tampilan jika data masih proses diambil dari database
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Komponen kecil bantuan untuk menyusun baris informasi di dalam Kartu Detail.
 * Bentuknya: [Label (kiri)]           [Nilai (kanan)]
 */
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