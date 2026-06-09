package com.example.perkapp.features.kegiatan.ui

// Mengimpor kelas ViewModel dari Android Jetpack
import androidx.lifecycle.ViewModel
// Mengimpor ViewModelProvider untuk membuat instance ViewModel dengan parameter
import androidx.lifecycle.ViewModelProvider
// Mengimpor cakupan Coroutine (CoroutineScope) khusus untuk ViewModel
import androidx.lifecycle.viewModelScope
// Mengimpor interface KegiatanRepository dari lapisan data
import com.example.perkapp.features.kegiatan.data.KegiatanRepository
// Mengimpor data-data model domain yang digunakan oleh UI
import com.example.perkapp.features.kegiatan.domain.HomeUiState
import com.example.perkapp.features.kegiatan.domain.InventoryStats
import com.example.perkapp.features.kegiatan.domain.UserInfo
// Mengimpor fungsi pembantu async untuk menjalankan tugas secara paralel
import kotlinx.coroutines.async
// Mengimpor StateFlow dan MutableStateFlow untuk mengelola state secara reaktif
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// Mengimpor launch untuk menjalankan coroutine baru tanpa memblokir thread
import kotlinx.coroutines.launch

/**
 * FUNGSI: HomeViewModel
 * TUJUAN: Bertindak sebagai jembatan antara repositori data dan antarmuka pengguna (HomeScreen).
 * ViewModel bertanggung jawab mempertahankan status (state) UI agar tidak hilang saat rotasi layar.
 */
class HomeViewModel(
    // Menerima dependency interface KegiatanRepository melalui constructor (untuk kemudahan testing)
    private val repository: KegiatanRepository
) : ViewModel() {

    // _uiState bertipe MutableStateFlow, bersifat private agar isinya hanya bisa dimutasi di dalam kelas ini
    private val _uiState = MutableStateFlow(buatStateAwal())
    // uiState bertipe StateFlow (read-only), diekspos ke luar agar bisa diamati secara pasif oleh HomeScreen
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Blok inisialisasi awal yang dijalankan ketika pertama kali objek HomeViewModel diciptakan
    init {
        // Otomatis memanggil fungsi untuk memuat data beranda saat mulai berjalan
        muatDataHome()
    }

    /**
     * FUNGSI: muatDataHome
     * TUJUAN: Mengambil seluruh data kebutuhan halaman Home secara paralel agar waktu respons cepat.
     */
    fun muatDataHome() {
        // Meluncurkan coroutine di dalam lingkup daur hidup ViewModel
        viewModelScope.launch {
            // Mengubah state ke posisi memuat (isLoading = true) dan mengosongkan pesan error sebelumnya
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Menggunakan async untuk memicu pemanggilan 3 fungsi data secara asinkron dan paralel
            val statsDeferred    = async { repository.getInventoryStats() } // Mengambil data statistik barang
            val kegiatanDeferred = async { repository.getKegiatanAktif() } // Mengambil daftar kegiatan aktif
            val userInfoDeferred = async { repository.getUserInfo() } // Mengambil info user profil

            // Menanti (await) hasil eksekusi dari masing-masing pemrosesan asinkron
            val statsResult    = statsDeferred.await()
            val kegiatanResult = kegiatanDeferred.await()
            val userInfo       = userInfoDeferred.await()

            // Jika salah satu dari pengambilan data inventaris atau kegiatan mengalami kegagalan
            if (statsResult.isFailure || kegiatanResult.isFailure) {
                // Memperbarui state UI dengan mengubah loading menjadi selesai dan memasukkan pesan kesalahan
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Gagal memuat data. Periksa koneksi internet kamu."
                )
                // Keluar dari eksekusi coroutine
                return@launch
            }

            // Jika semua proses di atas berhasil tanpa hambatan, perbarui data state UI utama
            _uiState.value = HomeUiState(
                isLoading    = false,
                isSynced     = true, // Menandakan data sukses disinkronkan dengan backend
                errorMessage = null,
                userInfo     = userInfo,
                // Mengambil nilai kembalian dari Result, jika kosong kembalikan nilai default kosong
                inventoryStats = statsResult.getOrDefault(
                    InventoryStats(borrowedCount = 0, availableCount = 0, pendingSyncCount = 0)
                ),
                kegiatanAktif = kegiatanResult.getOrDefault(emptyList())
            )
        }
    }

    /**
     * Dipanggil saat user menekan ikon sinkronisasi di bar atas.
     */
    fun onSyncDitekan() {
        // Memuat ulang data beranda secara realtime
        muatDataHome()
    }

    /**
     * Dipanggil ketika tombol "Borrow Equipment" diklik.
     */
    fun onPinjamAlatDitekan() {
        // Di sini bisa ditambahkan logika validasi izin peminjaman di masa mendatang
    }

    /**
     * Dipanggil ketika tombol "Log Activity" diklik.
     */
    fun onCatatKegiatanDitekan() {
        // Di sini bisa diletakkan logika bisnis tambahan untuk pencatatan
    }

    /**
     * FUNGSI: buatStateAwal
     * TUJUAN: Membuat objek representasi state awal sewaktu data belum berhasil dimuat.
     */
    private fun buatStateAwal(): HomeUiState {
        return HomeUiState(
            isLoading      = true, // Default awal bernilai loading
            isSynced       = false,
            userInfo       = UserInfo(nama = "", sapaan = "", fotoUrl = ""),
            inventoryStats = InventoryStats(0, 0, 0),
            kegiatanAktif  = emptyList()
        )
    }

    /**
     * Factory class yang digunakan untuk menginisialisasi HomeViewModel karena membutuhkan parameter
     * pada constructor-nya (tidak bisa dibuat secara instan oleh Compose jika tanpa factory).
     */
    class HomeViewModelFactory(
        private val repository: KegiatanRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            // Memeriksa apakah modelClass yang diminta mewarisi kelas HomeViewModel
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                // Mengembalikan instance ViewModel dengan menyematkan KegiatanRepository
                return HomeViewModel(repository) as T
            }
            // Melempar error apabila tipe ViewModel tidak sesuai kelas target
            throw IllegalArgumentException("ViewModel tidak dikenal")
        }
    }
}