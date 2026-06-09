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
package com.example.perkapp.core.features.kegiatan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perkapp.core.database.entity.KegiatanAlatEntity
import com.example.perkapp.features.kegiatan.data.KegiatanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.example.perkapp.core.datastore.dataStore
import com.example.perkapp.features.kegiatan.domain.UserInfo

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

    private val _uiState = MutableStateFlow(AktivitasUiState())
    val uiState: StateFlow<AktivitasUiState> = _uiState.asStateFlow()

    private val _allAktivitas = MutableStateFlow<List<Aktivitas>>(emptyList())

    val registeredUsers = MutableStateFlow<List<String>>(emptyList())
    val currentUserInfo = MutableStateFlow<UserInfo?>(null)

    init {
        loadActivities()
        observeNetworkChanges()
        fetchRegisteredUsers()
        loadCurrentUserInfo()
    }

    private fun observeNetworkChanges() {
        viewModelScope.launch {
            com.example.perkapp.core.utils.NetworkUtils.observeNetworkStatus(context)
                .collect { isOnline ->
                    if (isOnline) {
                        try {
                            repository.syncPendingKegiatan()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            loadActivities()
                            fetchRegisteredUsers()
                            loadCurrentUserInfo()
                        }
                    }
                }
        }
    }

    fun loadCurrentUserInfo() {
        viewModelScope.launch {
            try {
                currentUserInfo.value = repository.getUserInfo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchRegisteredUsers() {
        viewModelScope.launch {
            val db = com.example.perkapp.core.database.AppDatabase.getDatabase(context)
            val dao = db.registeredUserDao()
            
            // 1. Ambil data lokal dulu
            try {
                val local = dao.getAllRegisteredUsers().map { it.name }
                if (local.isNotEmpty()) {
                    registeredUsers.value = local
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Jika online, perbarui dari server
            if (com.example.perkapp.core.utils.NetworkUtils.isOnline(context)) {
                try {
                    val userPrefs = com.example.perkapp.core.datastore.UserPreferences(context.dataStore)
                    val authApi = com.example.perkapp.core.network.RetrofitClient.getClient(userPrefs)
                        .create(com.example.perkapp.features.auth.api.AuthApiService::class.java)
                    
                    val response = authApi.getAllUsers()
                    if (response.success && response.data != null) {
                        val entities = response.data.map { dto ->
                            com.example.perkapp.core.database.entity.RegisteredUserEntity(
                                id = dto.id,
                                name = dto.name,
                                email = dto.email,
                                role = dto.role
                            )
                        }
                        dao.clearAll()
                        dao.insertAll(entities)
                        registeredUsers.value = entities.map { it.name }
                    }
                } catch (e: Exception) {
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
                if (com.example.perkapp.core.utils.NetworkUtils.isOnline(context)) {
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

    fun loadAlatForKegiatan(kegiatanId: String) {
        viewModelScope.launch {
            try {
                val tools = repository.getAlatForKegiatanLocal(kegiatanId)
                _uiState.update { it.copy(currentDetailAlatList = tools) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateKegiatanAlatStatus(kegiatanAlatId: String, isReturned: Boolean, kegiatanId: String) {
        viewModelScope.launch {
            try {
                repository.updateKegiatanAlatStatusLocal(kegiatanAlatId, isReturned)
                loadAlatForKegiatan(kegiatanId)
            } catch (e: Exception) {
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
        viewModelScope.launch {
            try {
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
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
        viewModelScope.launch {
            try {
                repository.updateKegiatanLocal(id, judul, kategori, lokasi, tanggal, status, peminjam, deskripsi)
                loadActivities()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilter()
    }

    fun onFilterChange(filter: StatusAktivitas?) {
        _uiState.update { it.copy(activeFilter = filter) }
        applyFilter()
    }

    fun refresh() {
        loadActivities()
    }

    fun deleteKegiatan(kegiatanId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteKegiatan(kegiatanId)
                loadActivities()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun approveAlat(kegiatanId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.approveAlatForKegiatan(kegiatanId)
                loadActivities()
                loadAlatForKegiatan(kegiatanId)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyFilter() {
        val state = _uiState.value
        val filtered = _allAktivitas.value
            .filter { aktivitas ->
                val matchQuery = state.searchQuery.isBlank() ||
                        aktivitas.judul.contains(state.searchQuery, ignoreCase = true) ||
                        aktivitas.deskripsi.contains(state.searchQuery, ignoreCase = true)

                val matchFilter = state.activeFilter == null ||
                        aktivitas.status == state.activeFilter

                matchQuery && matchFilter
            }

        _uiState.update { it.copy(aktivitasList = filtered) }
    }
}
