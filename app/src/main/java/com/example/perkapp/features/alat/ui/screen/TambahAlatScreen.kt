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
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.runtime.livedata.observeAsState
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

/**
 * TambahAlatScreen — Halaman form untuk memasukkan barang baru ke inventaris.
 *
 * Mendukung input data dasar (nama, kategori, jumlah, kondisi) serta
 * pengambilan foto barang langsung dari kamera atau galeri HP.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TambahAlatScreen(
    viewModel: AlatViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.observeAsState(false)

    // Menyimpan state dari setiap inputan form
    var nama by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("") }
    var jumlah by remember { mutableStateOf("") }
    var kondisi by remember { mutableStateOf("good") }
    var expandedKondisi by remember { mutableStateOf(false) } // Untuk membuka tutup dropdown
    
    // Menyimpan URI (alamat) foto dan bitmap untuk ditampilkan di layar
    var imageUriString by remember { mutableStateOf<String?>(null) }
    var bitmapPreview by remember { mutableStateOf<Bitmap?>(null) }

    // Membuka Galeri Foto
    val gallerylauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Copy gambar dari galeri ke memori internal app agar aman walau offline
            val file = ImageUtils.getFileFromUri(context, it.toString())
            if (file != null) {
                imageUriString = android.net.Uri.fromFile(file).toString()
            } else {
                imageUriString = it.toString()
            }
            // Muat gambar untuk ditampilkan di preview
            bitmapPreview = ImageUtils.loadBitmapFromUri(context, imageUriString)
        }
    }

    // Membuka Kamera Android
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            // Simpan hasil jepretan kamera ke file fisik di HP
            val savedUriString = ImageUtils.saveBitmapToFile(context, it)
            if (savedUriString != null) {
                imageUriString = savedUriString
                bitmapPreview = it
            }
        }
    }

    val kondisiOptions = listOf("good", "damaged")

    // Pewarnaan kustom untuk semua kotak input agar seragam
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
                // Agar form bisa di-scroll jika layar kekecilan
                .verticalScroll(rememberScrollState())
        ) {
            // --- Input Nama Alat ---
            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama Alat") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- Input Kategori ---
            OutlinedTextField(
                value = kategori,
                onValueChange = { kategori = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kategori") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- Input Jumlah ---
            OutlinedTextField(
                value = jumlah,
                onValueChange = { jumlah = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Jumlah") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- Dropdown Pilihan Kondisi ---
            ExposedDropdownMenuBox(
                expanded = expandedKondisi,
                onExpandedChange = { expandedKondisi = !expandedKondisi }
            ) {
                OutlinedTextField(
                    value = kondisi.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true, // Supaya user tidak ngetik manual, harus pilih dari list
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
                                expandedKondisi = false // Tutup menu setelah dipilih
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Bagian Gambar Alat ---
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
                // Tombol Buka Kamera
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

                // Tombol Buka Galeri
                OutlinedButton(
                    onClick = { gallerylauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galeri")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tampilkan foto yang berhasil diambil (Preview)
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
                        contentScale = ContentScale.Crop, // Potong gambar biar pas di kotak
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Tombol Simpan ---
            Button(
                onClick = {
                    // Validasi Dasar agar data tidak bodong
                    if (nama.isBlank()) {
                        android.widget.Toast.makeText(context, "Nama Alat tidak boleh kosong", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (kategori.isBlank()) {
                        android.widget.Toast.makeText(context, "Kategori tidak boleh kosong", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val qty = jumlah.toIntOrNull() ?: 0
                    if (qty <= 0) {
                        android.widget.Toast.makeText(context, "Jumlah harus lebih dari 0", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    // Kirim ke ViewModel untuk di-insert ke Room Database
                    viewModel.createAlat(nama, kategori, qty, kondisi, imageUriString ?: "")
                    onBack() // Kembali ke halaman sebelumnya
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (isLoading) "Menyimpan..." else "Simpan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}