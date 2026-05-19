package com.example.perkapp.features.kegiatan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.perkapp.features.kegiatan.data.KegiatanRepository
import com.example.perkapp.features.kegiatan.domain.HomeUiState
import com.example.perkapp.features.kegiatan.domain.InventoryStats
import com.example.perkapp.features.kegiatan.domain.UserInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ============================================================
// FILE: HomeViewModel.kt
// LOKASI: features/kegiatan/ui/HomeViewModel.kt
//         (ikut struktur Adam → ViewModel di folder ui/)
//
// FUNGSI: Lapisan VIEWMODEL dalam MVVM.
//         Menghubungkan Repository (data) dengan HomeScreen (UI).
//         Menyimpan state halaman Home sebagai StateFlow.
// ============================================================


class HomeViewModel(
    // ViewModel menerima Repository lewat constructor
    // Ini memudahkan testing dan penggantian sumber data
    private val repository: KegiatanRepository
) : ViewModel() {

    // ------------------------------------------------------------
    // STATE MANAGEMENT
    // _uiState → private, hanya ViewModel yang bisa ubah
    //  uiState → public read-only, diobservasi oleh HomeScreen
    // ------------------------------------------------------------
    private val _uiState = MutableStateFlow(buatStateAwal())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()


    // Dipanggil otomatis saat ViewModel pertama kali dibuat
    init {
        muatDataHome()
    }


    // ------------------------------------------------------------
    // FUNGSI: muatDataHome()
    // Mengambil semua data Home secara paralel menggunakan async
    // supaya lebih cepat dari pada request satu-satu (antri)
    // ------------------------------------------------------------
    fun muatDataHome() {
        viewModelScope.launch {
            // Tampilkan loading dulu sebelum data datang
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Jalankan 3 request secara paralel
            val statsDeferred    = async { repository.getInventoryStats() }
            val kegiatanDeferred = async { repository.getKegiatanAktif() }
            val userInfoDeferred = async { repository.getUserInfo() }

            // Tunggu semua selesai
            val statsResult    = statsDeferred.await()
            val kegiatanResult = kegiatanDeferred.await()
            val userInfo       = userInfoDeferred.await()

            // Cek apakah ada yang error
            if (statsResult.isFailure || kegiatanResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Gagal memuat data. Periksa koneksi internet kamu."
                )
                return@launch
            }

            // Semua berhasil → update state dengan data dari API
            _uiState.value = HomeUiState(
                isLoading    = false,
                isSynced     = true,
                errorMessage = null,
                userInfo     = userInfo,
                inventoryStats = statsResult.getOrDefault(
                    InventoryStats(borrowedCount = 0, availableCount = 0, pendingSyncCount = 0)
                ),
                kegiatanAktif = kegiatanResult.getOrDefault(emptyList())
            )
        }
    }


    // Dipanggil saat user tekan tombol sync di header
    fun onSyncDitekan() {
        muatDataHome()
    }

    // Dipanggil saat tombol "Borrow Equipment" ditekan
    // Navigasi ditangani di HomeScreen, ViewModel hanya handle logika
    fun onPinjamAlatDitekan() {
        // TODO: Tambah logika bisnis jika perlu (misal: cek izin user)
    }

    // Dipanggil saat tombol "Log Activity" ditekan
    fun onCatatKegiatanDitekan() {
        // TODO: Tambah logika bisnis jika perlu
    }


    // State awal sebelum data dari API datang (supaya UI tidak crash)
    private fun buatStateAwal(): HomeUiState {
        return HomeUiState(
            isLoading      = true,
            isSynced       = false,
            userInfo       = UserInfo(nama = "", sapaan = "", fotoUrl = ""),
            inventoryStats = InventoryStats(0, 0, 0),
            kegiatanAktif  = emptyList()
        )
    }


    // ------------------------------------------------------------
    // FACTORY: Dibutuhkan karena ViewModel punya constructor parameter
    // Cara pakai di HomeScreen:
    //   val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(repo))
    // ------------------------------------------------------------
    class HomeViewModelFactory(
        private val repository: KegiatanRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel tidak dikenal")
        }
    }
}