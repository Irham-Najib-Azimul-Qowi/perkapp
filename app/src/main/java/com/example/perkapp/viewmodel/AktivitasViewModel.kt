/**
 * FUNGSI: AktivitasViewModel
 * TUJUAN: Berperan sebagai State Holder dan otak logika untuk semua layar yang berhubungan dengan
 * fitur Kegiatan/Aktivitas (misal: AktivitasScreen, DetailKegiatanScreen, dll).
 *
 * ALUR LOGIKA PENGERJAAN:
 * ViewModel ini mengelola state UI (`AktivitasUiState`) menggunakan `StateFlow`.
 * Tugas utamanya meliputi:
 * 1. Memuat daftar kegiatan dari repository (lokal/Room).
 * 2. Melakukan filter/pencarian daftar kegiatan.
 * 3. Mengelola sinkronisasi data kegiatan antara server (API) dan database lokal (Room).
 * 4. Menyimpan data sementara yang dibutuhkan UI seperti `currentDetailAlatList`.
 *
 * `@HiltViewModel` menandakan bahwa instance kelas ini bisa di-inject otomatis
 * oleh Hilt, lengkap dengan parameter yang dibutuhkan (`KegiatanRepository`).
 */
package com.example.perkapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perkapp.model.KegiatanAlatEntity
import com.example.perkapp.repository.KegiatanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.example.perkapp.database.dataStore
import com.example.perkapp.model.UserInfo
import com.example.perkapp.ui.screens.Aktivitas
import com.example.perkapp.ui.screens.StatusAktivitas

data class AktivitasUiState(
    val aktivitasList: List<Aktivitas> = emptyList(),
    val currentDetailAlatList: List<KegiatanAlatEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val searchQuery: String = "",
    val activeFilter: StatusAktivitas? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class AktivitasViewModel @Inject constructor(
    private val repository: KegiatanRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // _uiState menyimpan data reaktif UI State untuk dibaca oleh Compose
    private val _uiState = MutableStateFlow(AktivitasUiState())
    // uiState diekspos sebagai StateFlow read-only ke luar kelas
    val uiState: StateFlow<AktivitasUiState> = _uiState.asStateFlow()

    // _allAktivitas menampung daftar seluruh kegiatan sebelum difilter
    private val _allAktivitas = MutableStateFlow<List<Aktivitas>>(emptyList())

    // registeredUsers menampung daftar nama seluruh akun pengguna terdaftar
    val registeredUsers = MutableStateFlow<List<String>>(emptyList())
    // currentUserInfo menampung data informasi user yang saat ini sedang login
    val currentUserInfo = MutableStateFlow<UserInfo?>(null)

    init {
        // 1. Ambil daftar kegiatan saat inisialisasi
        loadActivities()
        // 2. Pantau konektivitas jaringan
        observeNetworkChanges()
        // 3. Ambil daftar user terdaftar
        fetchRegisteredUsers()
        // 4. Ambil informasi user yang aktif saat ini
        loadCurrentUserInfo()
    }

    // observeNetworkChanges memantau perubahan koneksi internet di latar belakang
    private fun observeNetworkChanges() {
        // Jalankan coroutine di scope ViewModel
        viewModelScope.launch {
            // Berlangganan flow status jaringan
            com.example.perkapp.util.NetworkUtils.observeNetworkStatus(context)
                .collect { isOnline ->
                    // Jika perangkat kembali online
                    if (isOnline) {
                        try {
                            // Kirim data kegiatan tertunda ke server
                            repository.syncPendingKegiatan()
                        } catch (e: Exception) {
                            // Tulis error ke log jika sinkronisasi gagal
                            e.printStackTrace()
                        } finally {
                            // Segarkan semua data lokal
                            loadActivities()
                            fetchRegisteredUsers()
                            loadCurrentUserInfo()
                        }
                    }
                }
        }
    }

    // loadCurrentUserInfo mengambil informasi profil user saat ini
    fun loadCurrentUserInfo() {
        // Jalankan coroutine
        viewModelScope.launch {
            try {
                // Minta data user dari repository
                currentUserInfo.value = repository.getUserInfo()
            } catch (e: Exception) {
                // Tulis error ke log jika gagal
                e.printStackTrace()
            }
        }
    }

    // fetchRegisteredUsers mengambil daftar nama semua user yang terdaftar
    fun fetchRegisteredUsers() {
        // Jalankan coroutine
        viewModelScope.launch {
            // Dapatkan akses ke database lokal
            val db = com.example.perkapp.database.AppDatabase.getDatabase(context)
            // Dapatkan DAO untuk tabel user terdaftar
            val dao = db.registeredUserDao()
            
            // 1. Ambil data lokal dulu untuk respon cepat
            try {
                // Petakan entitas ke daftar nama
                val local = dao.getAllRegisteredUsers().map { it.name }
                // Jika data lokal tidak kosong
                if (local.isNotEmpty()) {
                    // Update daftar nama di StateFlow
                    registeredUsers.value = local
                }
            } catch (e: Exception) {
                // Log error
                e.printStackTrace()
            }

            // 2. Jika online, perbarui data dari server
            if (com.example.perkapp.util.NetworkUtils.isOnline(context)) {
                try {
                    // Dapatkan preferences pembaca token
                    val userPrefs = com.example.perkapp.database.UserPreferences(context.dataStore)
                    // Bangun service Auth API
                    val authApi = com.example.perkapp.network.RetrofitClient.getClient(userPrefs)
                        .create(com.example.perkapp.network.AuthApiService::class.java)
                    
                    // Request daftar semua user dari server
                    val response = authApi.getAllUsers()
                    // Jika sukses dan respon data tidak null
                    if (response.success && response.data != null) {
                        // Petakan DTO ke entitas lokal
                        val entities = response.data.map { dto ->
                            com.example.perkapp.model.RegisteredUserEntity(
                                id = dto.id,
                                name = dto.name,
                                email = dto.email,
                                role = dto.role
                            )
                        }
                        // Bersihkan tabel lokal lama
                        dao.clearAll()
                        // Masukkan semua entitas baru
                        dao.insertAll(entities)
                        // Perbarui data nama di StateFlow
                        registeredUsers.value = entities.map { it.name }
                    }
                } catch (e: Exception) {
                    // Log error
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * FUNGSI: loadActivities
     * TUJUAN: Mengambil daftar riwayat kegiatan dari database lokal (Room) lalu memetakannya
     * ke dalam kelas data UI (`Aktivitas`) sebelum dilempar ke `StateFlow`.
     */
    fun loadActivities() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (com.example.perkapp.util.NetworkUtils.isOnline(context)) {
                    try {
                        repository.syncPendingKegiatan()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val entities = repository.getAllKegiatanLocal()
                val mapped = entities
                    .filter { it.status == "BERLANGSUNG" || it.status == "SELESAI" }
                    .map { entity ->
                        Aktivitas(
                            id = entity.id,
                            judul = entity.judul,
                            deskripsi = "Lokasi: ${entity.lokasi}",
                            status = when (entity.status) {
                                "BERLANGSUNG" -> StatusAktivitas.BERLANGSUNG
                                "SELESAI" -> StatusAktivitas.SELESAI
                                else -> StatusAktivitas.DRAFT
                            },
                            progress = 0f,
                            tanggal = entity.tanggal,
                            isPending = entity.sync_status == "pending",
                            peminjam = entity.peminjam,
                            realDeskripsi = entity.deskripsi,
                            createdBy = entity.created_by,
                            alatApproved = entity.alat_approved
                        )
                    }
                _allAktivitas.value = mapped
                applyFilter()
            } catch (e: Exception) {
                val errorMsg = if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    "Gagal terhubung ke server. Periksa koneksi internet Anda."
                } else {
                    "Terjadi kesalahan saat memuat data. Silakan coba lagi."
                }
                _uiState.update { it.copy(errorMessage = errorMsg) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // loadAlatForKegiatan mengambil daftar alat yang dipinjam dalam suatu kegiatan
    fun loadAlatForKegiatan(kegiatanId: String) {
        // Jalankan coroutine
        viewModelScope.launch {
            try {
                // Ambil daftar alat dari repository secara lokal
                val tools = repository.getAlatForKegiatanLocal(kegiatanId)
                // Perbarui state list alat detail di UI State Flow
                _uiState.update { it.copy(currentDetailAlatList = tools) }
            } catch (e: Exception) {
                // Cetak error jika terjadi kegagalan
                e.printStackTrace()
            }
        }
    }

    // updateKegiatanAlatStatus memperbarui status pengembalian suatu alat
    fun updateKegiatanAlatStatus(kegiatanAlatId: String, isReturned: Boolean, kegiatanId: String) {
        // Jalankan coroutine
        viewModelScope.launch {
            try {
                // Panggil repository untuk mengupdate status pengembalian secara lokal
                repository.updateKegiatanAlatStatusLocal(kegiatanAlatId, isReturned)
                // Muat ulang daftar alat agar tampilan terupdate
                loadAlatForKegiatan(kegiatanId)
            } catch (e: Exception) {
                // Log error
                e.printStackTrace()
            }
        }
    }

    /**
     * FUNGSI: insertKegiatan
     * TUJUAN: Meracik semua inputan form Tambah Kegiatan untuk dikirim ke Repository agar disimpan/disinkronkan.
     */
    fun insertKegiatan(
        judul: String,
        kategori: String,
        lokasi: String,
        tanggal: String,
        status: String,
        peminjam: String,
        deskripsi: String,
        tools: List<Pair<String, Int>>,
        externalTools: List<String>,
        onSuccess: () -> Unit
    ) {
        // Jalankan coroutine
        viewModelScope.launch {
            try {
                // Simpan kegiatan baru secara lokal
                repository.insertKegiatanLocal(
                    judul = judul,
                    kategori = kategori,
                    lokasi = lokasi,
                    tanggal = tanggal,
                    status = status,
                    peminjam = peminjam,
                    deskripsi = deskripsi,
                    tools = tools,
                    externalTools = externalTools
                )
                // Jalankan callback sukses untuk navigasi kembali
                onSuccess()
            } catch (e: Exception) {
                // Log error jika gagal menyimpan
                e.printStackTrace()
            }
        }
    }

    // updateKegiatan memperbarui rincian data kegiatan
    fun updateKegiatan(
        id: String,
        judul: String,
        kategori: String,
        lokasi: String,
        tanggal: String,
        status: String,
        peminjam: String,
        deskripsi: String,
        onSuccess: () -> Unit
    ) {
        // Jalankan coroutine
        viewModelScope.launch {
            try {
                // Panggil repository untuk mengupdate kegiatan di database lokal
                repository.updateKegiatanLocal(id, judul, kategori, lokasi, tanggal, status, peminjam, deskripsi)
                // Muat ulang data daftar kegiatan
                loadActivities()
                // Panggil callback sukses
                onSuccess()
            } catch (e: Exception) {
                // Log error jika gagal mengupdate
                e.printStackTrace()
            }
        }
    }

    // onSearchQueryChange dipanggil saat user mengetik di kolom pencarian
    fun onSearchQueryChange(query: String) {
        // Perbarui query pencarian di UI State Flow
        _uiState.update { it.copy(searchQuery = query) }
        // Terapkan filter pencarian pada list kegiatan
        applyFilter()
    }

    // onFilterChange dipanggil saat user memilih chip filter status
    fun onFilterChange(filter: StatusAktivitas?) {
        // Perbarui filter aktif di UI State Flow
        _uiState.update { it.copy(activeFilter = filter) }
        // Terapkan filter status pada list kegiatan
        applyFilter()
    }

    // refresh melakukan reload manual terhadap daftar kegiatan
    fun refresh() {
        // Panggil fungsi muat data kegiatan
        loadActivities()
    }

    // deleteKegiatan menghapus data kegiatan berdasarkan ID
    fun deleteKegiatan(kegiatanId: String, onSuccess: () -> Unit) {
        // Jalankan coroutine
        viewModelScope.launch {
            try {
                // Panggil repository untuk memproses penghapusan kegiatan
                repository.deleteKegiatan(kegiatanId)
                // Muat ulang daftar kegiatan
                loadActivities()
                // Jalankan callback sukses
                onSuccess()
            } catch (e: Exception) {
                // Log error jika gagal menghapus
                e.printStackTrace()
            }
        }
    }

    // approveAlat memberikan persetujuan (approval) peminjaman alat oleh admin
    fun approveAlat(kegiatanId: String, onSuccess: () -> Unit) {
        // Jalankan coroutine
        viewModelScope.launch {
            try {
                // Panggil repository untuk menyetujui peminjaman alat
                repository.approveAlatForKegiatan(kegiatanId)
                // Muat ulang daftar kegiatan
                loadActivities()
                // Muat ulang daftar alat kegiatan ini
                loadAlatForKegiatan(kegiatanId)
                // Panggil callback sukses
                onSuccess()
            } catch (e: Exception) {
                // Log error jika gagal melakukan approval
                e.printStackTrace()
            }
        }
    }

    // applyFilter menyaring list kegiatan berdasarkan query pencarian dan chip filter aktif
    private fun applyFilter() {
        // Ambil data UI State saat ini
        val state = _uiState.value
        // Saring list seluruh kegiatan
        val filtered = _allAktivitas.value
            .filter { aktivitas ->
                // Cek apakah judul atau deskripsi mengandung query pencarian
                val matchQuery = state.searchQuery.isBlank() ||
                        aktivitas.judul.contains(state.searchQuery, ignoreCase = true) ||
                        aktivitas.deskripsi.contains(state.searchQuery, ignoreCase = true)

                // Cek apakah status kegiatan cocok dengan filter chip aktif
                val matchFilter = state.activeFilter == null ||
                        aktivitas.status == state.activeFilter

                // Kembalikan true jika memenuhi kriteria pencarian dan filter status
                matchQuery && matchFilter
            }

        // Perbarui list aktivitas yang ditampilkan di UI State Flow
        _uiState.update { it.copy(aktivitasList = filtered) }
    }
}
