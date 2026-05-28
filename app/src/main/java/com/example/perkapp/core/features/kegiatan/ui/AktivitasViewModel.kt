package com.example.perkapp.core.features.kegiatan.ui

/**
 * AktivitasViewModel.kt
 *
 * ViewModel untuk halaman AktivitasScreen.
 * Bertanggung jawab atas:
 *  - Menyimpan dan mengekspos UI state ke AktivitasScreen
 *  - Logika pencarian (search) berdasarkan judul dan deskripsi
 *  - Logika filter berdasarkan StatusAktivitas
 *  - (Nanti) Mengambil data dari KegiatanRepository
 *  - (Nanti) Mendeteksi status koneksi dari NetworkMonitor milik Adam
 *
 * Pola yang digunakan: UDF (Unidirectional Data Flow)
 *  Screen → event → ViewModel → update state → Screen recompose
 */

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// =============================================================================
// UI STATE
// Satu data class yang menjadi "sumber kebenaran tunggal" untuk AktivitasScreen.
// Semua yang perlu diketahui UI ada di sini.
// =============================================================================

/**
 * Representasi lengkap state UI halaman Aktivitas.
 * AktivitasScreen hanya membaca dari sini, tidak menyimpan state sendiri.
 *
 * @param aktivitasList  Daftar aktivitas yang sudah difilter, siap ditampilkan di list
 * @param isLoading      true saat data sedang dimuat (untuk tampilkan loading spinner)
 * @param isOffline      true saat tidak ada koneksi internet → tampilkan OfflineBanner
 * @param searchQuery    Teks pencarian yang sedang diketik user di search bar
 * @param activeFilter   Filter status yang aktif. null berarti tampilkan semua status.
 * @param errorMessage   Pesan error jika ada gagal load, ditampilkan sebagai snackbar/toast
 */
data class AktivitasUiState(
    val aktivitasList: List<Aktivitas> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val searchQuery: String = "",
    val activeFilter: StatusAktivitas? = null,
    val errorMessage: String? = null,
)

// =============================================================================
// VIEWMODEL
// =============================================================================

/**
 * @HiltViewModel → Hilt akan otomatis membuat dan meng-inject ViewModel ini.
 * Tidak perlu buat ViewModelFactory manual.
 *
 * @Inject constructor → Hilt akan mengisi parameter constructor secara otomatis.
 * Tambahkan dependency (Repository, NetworkMonitor, dll) di sini saat sudah siap.
 */
@HiltViewModel
class AktivitasViewModel @Inject constructor(
    // TODO: Uncomment setelah KegiatanRepository.kt di folder data/ selesai dibuat
    // private val repository: KegiatanRepository,

    // TODO: Uncomment setelah NetworkMonitor.kt milik Adam siap digunakan
    // private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    // _uiState adalah MutableStateFlow yang hanya bisa diubah di dalam ViewModel (private)
    private val _uiState = MutableStateFlow(AktivitasUiState())

    // uiState adalah versi read-only yang di-expose ke AktivitasScreen (public)
    // Screen hanya bisa membaca, tidak bisa mengubah langsung → mencegah bug
    val uiState: StateFlow<AktivitasUiState> = _uiState.asStateFlow()

    /**
     * Menyimpan semua aktivitas mentah SEBELUM filter/search diterapkan.
     * Filter bekerja dengan memfilter list ini lalu hasilnya dimasukkan ke uiState.aktivitasList.
     * Dengan cara ini, saat filter dilepas, data asli tidak hilang.
     */
    private val _allAktivitas = MutableStateFlow<List<Aktivitas>>(emptyList())

    // init {} dipanggil otomatis saat ViewModel pertama kali dibuat
    init {
        // Saat ini pakai data dummy dulu agar UI bisa dicoba tanpa API
        // Ganti loadDummyData() dengan loadAktivitas() setelah repository siap
        loadDummyData()

        // TODO: Aktifkan ini setelah NetworkMonitor dari Adam sudah bisa dipakai
        // observeNetwork()
    }

    // =========================================================================
    // PUBLIC ACTIONS
    // Fungsi-fungsi ini dipanggil dari AktivitasScreen sebagai respons aksi user
    // =========================================================================

    /**
     * Dipanggil setiap kali user mengetik di search bar.
     * Memperbarui searchQuery di state lalu menerapkan ulang filter.
     *
     * @param query Teks terbaru dari search bar
     */
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilter() // terapkan filter dengan query baru
    }

    /**
     * Dipanggil saat user tap chip filter (Berlangsung / Selesai / Draft).
     * Jika filter yang sama di-tap lagi, nilainya null (tampilkan semua).
     *
     * @param filter Status yang dipilih, atau null untuk hapus filter
     */
    fun onFilterChange(filter: StatusAktivitas?) {
        _uiState.update { it.copy(activeFilter = filter) }
        applyFilter() // terapkan filter dengan status baru
    }

    /**
     * Dipanggil saat user melakukan pull-to-refresh.
     * TODO: Ganti loadDummyData() dengan sinkronisasi ke server via repository
     */
    fun refresh() {
        // TODO: panggil repository.syncAktivitas() untuk ambil data terbaru dari API
        loadDummyData()
    }

    // =========================================================================
    // PRIVATE HELPERS
    // Logika internal ViewModel, tidak perlu diketahui oleh Screen
    // =========================================================================

    /**
     * Menerapkan filter pencarian dan filter status ke _allAktivitas,
     * lalu memasukkan hasilnya ke uiState.aktivitasList.
     *
     * Logika filter:
     *  - searchQuery kosong → semua aktivitas lolos filter teks
     *  - activeFilter null → semua status lolos filter status
     *  - Keduanya AND: aktivitas harus memenuhi kedua kondisi sekaligus
     */
    private fun applyFilter() {
        val state = _uiState.value
        val filtered = _allAktivitas.value
            .filter { aktivitas ->
                // Cek apakah judul atau deskripsi mengandung teks pencarian
                // ignoreCase = true agar "audit" bisa menemukan "Audit"
                val matchQuery = state.searchQuery.isBlank() ||
                        aktivitas.judul.contains(state.searchQuery, ignoreCase = true) ||
                        aktivitas.deskripsi.contains(state.searchQuery, ignoreCase = true)

                // Cek apakah status aktivitas sesuai filter yang dipilih
                val matchFilter = state.activeFilter == null ||
                        aktivitas.status == state.activeFilter

                // Aktivitas lolos hanya jika memenuhi keduanya
                matchQuery && matchFilter
            }

        // Update state dengan list yang sudah difilter
        _uiState.update { it.copy(aktivitasList = filtered) }
    }

    /**
     * Mengisi _allAktivitas dan uiState dengan data contoh/dummy.
     * HANYA UNTUK DEVELOPMENT — hapus/ganti fungsi ini setelah
     * KegiatanRepository.kt selesai dibuat dan bisa menyuplai data nyata.
     */
    private fun loadDummyData() {
        val dummy = listOf(
            Aktivitas(
                id = "1",
                judul = "Audit Fasilitas Kampus",
                deskripsi = "Inspeksi keselamatan dan infrastruktur triwulan untuk Gedung Barat.",
                status = StatusAktivitas.BERLANGSUNG,
                progress = 0.65f, // 65% selesai
                tanggal = "Berlangsung",
            ),
            Aktivitas(
                id = "2",
                judul = "Stok Tahunan 2023",
                deskripsi = "Verifikasi global semua aset berkategori dan perangkat IT.",
                status = StatusAktivitas.SELESAI,
                progress = 1f, // 100% selesai
                tanggal = "18 Okt 2023",
            ),
            Aktivitas(
                id = "3",
                judul = "Pemeliharaan AC Gedung C",
                deskripsi = "Pengecekan rutin unit AC di seluruh ruangan Gedung C lantai 2-4.",
                status = StatusAktivitas.DRAFT,
                progress = 0f, // belum mulai
                tanggal = "Draft",
            ),
        )

        // Simpan ke raw list (untuk keperluan filter ulang)
        _allAktivitas.value = dummy

        // Langsung tampilkan semua ke UI tanpa filter
        _uiState.update { it.copy(aktivitasList = dummy) }
    }

    // =========================================================================
    // NETWORK MONITOR
    // Aktifkan blok ini setelah NetworkMonitor.kt dari Adam selesai
    // =========================================================================

    /**
     * Mengamati status koneksi internet secara real-time menggunakan Flow.
     * Saat koneksi terputus → isOffline = true → OfflineBanner muncul di Screen.
     * Saat koneksi kembali → isOffline = false → OfflineBanner hilang dengan animasi.
     */
    // private fun observeNetwork() {
    //     viewModelScope.launch {
    //         networkMonitor.isOnline.collect { isOnline ->
    //             _uiState.update { it.copy(isOffline = !isOnline) }
    //         }
    //     }
    // }
}
