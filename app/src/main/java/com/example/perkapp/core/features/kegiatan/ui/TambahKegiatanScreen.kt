package com.example.perkapp.core.features.kegiatan.ui

// Mengimpor library dasar Jetpack Compose untuk menyusun UI
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import java.util.Calendar

/**
 * Composable screen untuk menambah kegiatan baru (Step 1: Info Kegiatan).
 *
 * @param navController Pengontrol navigasi untuk kembali ke halaman sebelumnya (Batal).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TambahKegiatanScreen(navController: NavController) {
    // Mendapatkan context Android untuk menampilkan DatePickerDialog
    val context = LocalContext.current

    // Mendapatkan objek Calendar saat ini
    val calendar = Calendar.getInstance()
    val tahunKini = calendar.get(Calendar.YEAR)
    val bulanKini = calendar.get(Calendar.MONTH)
    val hariKini = calendar.get(Calendar.DAY_OF_MONTH)

    // State untuk menyimpan data input form
    var namaAktivitas by remember { mutableStateOf("") }
    var tanggalPinjam by remember { mutableStateOf("") }
    var tanggalKembali by remember { mutableStateOf("") }
    var peminjam by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }

    // State untuk kontrol Dropdown Peminjam
    var dropdownTerbuka by remember { mutableStateOf(false) }

    // Daftar nama anggota/user untuk pilihan peminjam (dummy data)
    val daftarAnggota = listOf("Reja", "Adam", "Najib", "Alex", "Budi", "Chandra", "Irham", "Najib Azimul")

    // Filter daftar anggota secara real-time berdasarkan input teks dari user
    val anggotaDifilter = daftarAnggota.filter {
        it.contains(peminjam, ignoreCase = true)
    }

    // Penampung DatePickerDialog untuk Tanggal Pinjam
    val datePickerPinjam = DatePickerDialog(
        context,
        { _, tahun, bulan, hari ->
            // Mengubah state tanggalPinjam dengan format dd/mm/yyyy
            tanggalPinjam = String.format("%02d/%02d/%04d", hari, bulan + 1, tahun)
        },
        tahunKini,
        bulanKini,
        hariKini
    )

    // Penampung DatePickerDialog untuk Tanggal Kembali
    val datePickerKembali = DatePickerDialog(
        context,
        { _, tahun, bulan, hari ->
            // Mengubah state tanggalKembali dengan format dd/mm/yyyy
            tanggalKembali = String.format("%02d/%02d/%04d", hari, bulan + 1, tahun)
        },
        tahunKini,
        bulanKini,
        hariKini
    )

    // Layout utama berlatar belakang abu-biru sangat muda yang dapat di-scroll
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        
        // ── TOP HEADER ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "SIEPERKAP",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF006E2F)
            )
        }

        // ── STEP INDICATOR ───────────────────────────────────────────────────
        // Menampilkan step 1 aktif (hijau) dan step 2 pasif (abu-abu)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Step 1: Info Kegiatan (Aktif)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF006E2F)), // Latar belakang hijau
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("info Kegiatan", fontSize = 11.sp, color = Color(0xFF006E2F), fontWeight = FontWeight.SemiBold)
            }

            // Garis Penghubung antar Step
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(1.dp)
                    .background(Color(0xFFD9E3F6))
                    .padding(horizontal = 8.dp)
            )

            // Step 2: Perlengkapan (Pasif)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD9E3F6)), // Latar belakang abu-biru
                    contentAlignment = Alignment.Center
                ) {
                    Text("2", color = Color(0xFF3D4A3D), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Perlengkapan", fontSize = 11.sp, color = Color(0xFF6D7B6C))
            }
        }

        // ── FORM INPUTS ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. INPUT: Nama Aktivitas
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nama Aktivitas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF121C2A)
                )
                OutlinedTextField(
                    value = namaAktivitas,
                    onValueChange = { namaAktivitas = it },
                    placeholder = { Text("e.g. Field Research Seminar", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFEFF4FF),
                        focusedContainerColor = Color(0xFFEFF4FF),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color(0xFF006E2F)
                    )
                )
            }

            // 2. INPUT: Tanggal Pinjam & Tanggal Kembali (Berdampingan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tanggal Pinjam
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tanggal Pinjam",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF121C2A)
                    )
                    OutlinedTextField(
                        value = tanggalPinjam,
                        onValueChange = {}, // Read-only, diisi via DatePicker
                        placeholder = { Text("dd/mm/yyyy", color = Color.LightGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerPinjam.show() }, // Menampilkan dialog kalender saat ditap
                        enabled = false, // Menonaktifkan input keyboard manual
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Pilih Tanggal Pinjam",
                                tint = Color.Gray,
                                modifier = Modifier.clickable { datePickerPinjam.show() }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = Color(0xFFEFF4FF),
                            disabledBorderColor = Color.Transparent,
                            disabledTextColor = Color(0xFF121C2A)
                        )
                    )
                }

                // Tanggal Kembali
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tanggal kembali",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF121C2A)
                    )
                    OutlinedTextField(
                        value = tanggalKembali,
                        onValueChange = {}, // Read-only, diisi via DatePicker
                        placeholder = { Text("dd/mm/yyyy", color = Color.LightGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerKembali.show() }, // Menampilkan dialog kalender saat ditap
                        enabled = false, // Menonaktifkan input keyboard manual
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Pilih Tanggal Kembali",
                                tint = Color.Gray,
                                modifier = Modifier.clickable { datePickerKembali.show() }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = Color(0xFFEFF4FF),
                            disabledBorderColor = Color.Transparent,
                            disabledTextColor = Color(0xFF121C2A)
                        )
                    )
                }
            }

            // 3. INPUT: Peminjam (Dengan Dropdown Pilihan Nama)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Peminjam",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF121C2A)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = peminjam,
                        onValueChange = {
                            peminjam = it
                            dropdownTerbuka = true // Otomatis buka dropdown saat mengetik
                        },
                        placeholder = { Text("Search members...", color = Color.LightGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                // Buka dropdown jika kolom mendapatkan fokus ketikan
                                if (focusState.isFocused) {
                                    dropdownTerbuka = true
                                }
                            },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari Peminjam",
                                tint = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFEFF4FF),
                            focusedContainerColor = Color(0xFFEFF4FF),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color(0xFF006E2F)
                        )
                    )

                    // Dropdown menu berisi daftar nama yang bisa dipilih
                    DropdownMenu(
                        expanded = dropdownTerbuka && anggotaDifilter.isNotEmpty(),
                        onDismissRequest = { dropdownTerbuka = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f) // Menyesuaikan lebar input
                            .background(Color.White)
                    ) {
                        anggotaDifilter.forEach { nama ->
                            DropdownMenuItem(
                                text = { Text(nama, color = Color(0xFF121C2A)) },
                                onClick = {
                                    peminjam = nama // Menyalin nama terpilih ke teks input
                                    dropdownTerbuka = false // Menutup dropdown
                                }
                            )
                        }
                    }
                }
            }

            // 4. INPUT: Deskripsi (Multi-line)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Deskripsi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF121C2A)
                )
                OutlinedTextField(
                    value = deskripsi,
                    onValueChange = { deskripsi = it },
                    placeholder = { 
                        Text(
                            "Brief details about the activity goals and requirements...", 
                            color = Color.LightGray,
                            fontSize = 14.sp
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp), // Tinggi lebih besar khusus deskripsi
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFEFF4FF),
                        focusedContainerColor = Color(0xFFEFF4FF),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color(0xFF006E2F)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── ACTION BUTTONS ────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tombol Hijau Utama: Lanjut ke penambahan perlengkapan
            Button(
                onClick = {
                    // Logic lanjut ke langkah berikutnya (sementara memicu navigasi balik ke list)
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp), // Bentuk melingkar ujung
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)) // Warna hijau terang
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Lanjut:Tambah Perlengkapan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                }
            }

            // Tombol Teks: Batal
            Text(
                text = "Batal",
                color = Color(0xFF006E2F), // Hijau utama
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable {
                        // Kembali ke halaman sebelumnya
                        navController.popBackStack()
                    }
                    .padding(8.dp)
            )
        }
    }
}

// Preview untuk mendemonstrasikan rancangan antarmuka TambahKegiatanScreen di IDE Android Studio
@Preview(showBackground = true)
@Composable
fun TambahKegiatanScreenPreview() {
    TambahKegiatanScreen(navController = rememberNavController())
}
