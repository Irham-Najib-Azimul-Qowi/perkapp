package com.example.perkapp.features.kegiatan.ui

// Mengimpor komponen dan fungsi Android/Compose yang dibutuhkan untuk membangun antarmuka pengguna
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.perkapp.features.kegiatan.domain.HomeUiState
import com.example.perkapp.features.kegiatan.domain.Kegiatan
import com.example.perkapp.features.kegiatan.domain.StatusKegiatan

/**
 * Composable utama untuk halaman Home.
 * Tampilan disederhanakan hanya memuat judul "SIEPERKAP" dan daftar kegiatan yang sedang aktif (berlangsung).
 *
 * @param navController Pengontrol navigasi untuk berpindah halaman.
 * @param viewModel ViewModel penyedia data dan logika halaman Home.
 */
@Composable
fun HomeScreen( 
    navController: NavController,
    viewModel: HomeViewModel = viewModel(), // Inisialisasi ViewModel secara default
    onNavigateToActivities: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToBorrow: () -> Unit = {},
    onNavigateToLogActivity: () -> Unit = {}
) {
    // Mengobservasi state UI dari ViewModel secara aman berdasarkan daur hidup (lifecycle)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Menggunakan Box sebagai wadah utama dengan padding sistem bar agar tidak tertutup notch/kamera
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF)) // Mengatur warna latar belakang abu-biru sangat muda
            .systemBarsPadding() // Otomatis menambahkan padding agar tidak bertabrakan dengan status bar / navigasi bar bawah
    ) {
        when {
            // Jika statusnya sedang memuat data dari server/lokal
            uiState.isLoading -> {
                CircularProgressIndicator(
                    color = HijauUtama,
                    modifier = Modifier.align(Alignment.Center) // Indikator berada tepat di tengah layar
                )
            }

            // Jika terjadi kegagalan pemuatan data
            uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Menampilkan teks deskripsi error
                    Text(uiState.errorMessage!!, color = Color.Red)
                    Spacer(Modifier.height(8.dp)) // Jarak spasi vertikal
                    // Tombol untuk mencoba memuat ulang data
                    Button(onClick = { viewModel.muatDataHome() }) {
                        Text("Coba Lagi")
                    }
                }
            }

            // Jika data berhasil dimuat dengan sukses
            else -> {
                // Merender tata letak konten utama halaman Home (hanya Judul + Kegiatan Aktif)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp), // Padding sekeliling halaman
                    verticalArrangement = Arrangement.spacedBy(16.dp) // Jarak antar elemen vertikal
                ) {
                    // Judul utama aplikasi "SIEPERKAP"
                    Text(
                        text = "SIEPERKAP",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = HijauUtama, // Warna hijau utama
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Kolom scroll berisi daftar kegiatan yang sedang aktif/berlangsung
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Mengambil sisa ruang layar
                            .verticalScroll(rememberScrollState()), // Mengaktifkan scroll vertikal
                        verticalArrangement = Arrangement.spacedBy(16.dp) // Jarak antar kartu kegiatan
                    ) {
                        // Menyaring data kegiatan dari repositori agar HANYA status AKTIF yang ditampilkan
                        val kegiatanBerlangsung = uiState.kegiatanAktif.filter { 
                            it.statusType == StatusKegiatan.AKTIF 
                        }

                        if (kegiatanBerlangsung.isEmpty()) {
                            // Teks keterangan jika tidak ada kegiatan aktif yang sedang berjalan
                            Text(
                                text = "Tidak ada aktivitas berlangsung saat ini.",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        } else {
                            // Melakukan perulangan untuk merender masing-masing kartu kegiatan yang aktif
                            kegiatanBerlangsung.forEach { kegiatan ->
                                KartuKegiatan(kegiatan = kegiatan)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Komponen kartu individual kegiatan dengan bar progres dan aksen garis warna di bagian bawah.
 *
 * @param kegiatan Objek data kegiatan yang akan digambar di kartu.
 */
@Composable
fun KartuKegiatan(kegiatan: Kegiatan) {
    // Menentukan warna aksen secara dinamis berdasarkan nilai enum StatusKegiatan
    val warnaBorder = when (kegiatan.statusType) {
        StatusKegiatan.AKTIF        -> HijauUtama            // Hijau untuk aktif
        StatusKegiatan.MAINTENANCE  -> Color(0xFFFF8B7C)     // Oranye untuk pemeliharaan
        StatusKegiatan.AUDIT        -> Color(0xFF6D7B6C)     // Abu-abu untuk audit
    }

    Surface(
        shape         = RoundedCornerShape(16.dp),
        color         = Color(0xFFF8F9FF),
        shadowElevation = 2.dp,
        modifier      = Modifier.fillMaxWidth() // Lebar kartu penuh menyesuaikan layar
    ) {
        Box {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Baris atas: Kategori kegiatan di kiri
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (kegiatan.statusType == StatusKegiatan.AKTIF) HijauMuda else Color(0xFFD9E3F6)
                    ) {
                        Text(
                            text     = kegiatan.kategori,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color    = if (kegiatan.statusType == StatusKegiatan.AKTIF) Color(0xFF007230) else Color(0xFF3D4A3D)
                        )
                    }
                }

                // Nama atau Judul kegiatan
                Text(kegiatan.judul, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF121C2A))

                // Baris Lokasi (Ikon Map pin + Nama tempat)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF3D4A3D),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(kegiatan.lokasi, fontSize = 14.sp, color = Color(0xFF3D4A3D))
                }
            }

            // Garis pembatas warna-warni di bagian paling bawah kartu
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(warnaBorder)
            )
        }
    }
}

// Konstanta warna hijau utama
val HijauUtama = Color(0xFF006E2F)  
// Konstanta warna hijau muda
val HijauMuda  = Color(0xFF7CF994)  

// Preview untuk mendemonstrasikan rancangan antarmuka HomeScreen di IDE Android Studio
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview(){
    HomeScreen(navController = rememberNavController())
}
