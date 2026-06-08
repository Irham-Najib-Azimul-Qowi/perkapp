package com.example.perkapp.features.alat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.perkapp.core.sync.SyncManager
import com.example.perkapp.features.alat.data.local.AlatEntity
import com.example.perkapp.features.alat.data.remote.CreateAlatRequest
import com.example.perkapp.features.alat.data.repository.AlatRepository
import kotlinx.coroutines.launch

/**
 * AlatViewModel — Mengelola semua logika terkait data inventaris alat.
 *
 * ViewModel ini adalah penghubung antara UI (layar daftar alat, tambah alat)
 * dengan sumber data (AlatRepository). Tugas utamanya adalah mengambil data,
 * menyimpan perubahan, dan menyediakan status/kondisi saat ini ke UI.
 *
 * Memanfaatkan AndroidViewModel (menggunakan Application) karena beberapa proses 
 * butuh 'context' (misalnya mengecek status internet atau jadwal sinkronisasi).
 */
class AlatViewModel(
    private val repository: AlatRepository,
    private val application: Application
) : ViewModel(){
    
    // MutableLiveData adalah "kotak reaktif" penyimpan data yang bisa diubah oleh ViewModel.
    // UI akan "berlangganan" (observe) kotak ini, jadi kalau isinya berubah, UI otomatis update.
    
    // Menyimpan daftar semua alat yang akan ditampilkan di layar inventaris
    val alatList = MutableLiveData<List<AlatEntity>>()
    
    // Menyimpan satu alat spesifik yang sedang dipilih (misal untuk diedit atau dilihat detailnya)
    val selectedAlat = MutableLiveData<AlatEntity?>()
    
    // Menyimpan status apakah sedang proses loading atau tidak (untuk menampilkan spinner)
    val isLoading = MutableLiveData(false)
    
    // Menyimpan pesan error jika ada proses yang gagal, agar bisa ditampilkan ke user (misal lewat Toast/Snackbar)
    val errorMessage = MutableLiveData<String?>()

    init {
        // Saat ViewModel pertama kali dibuat, langsung ambil data alat dari repository
        getAllAlat()
        // Mulai memantau perubahan koneksi internet (online/offline)
        observeNetworkChanges()
    }

    /**
     * Memantau perubahan status jaringan (internet).
     * Jika mendeteksi HP kembali online, otomatis memicu sinkronisasi data 
     * yang masih berstatus "pending" (belum terkirim ke server).
     */
    private fun observeNetworkChanges() {
        viewModelScope.launch {
            // Memantau flow status jaringan secara terus-menerus
            com.example.perkapp.core.utils.NetworkUtils.observeNetworkStatus(application)
                .collect { isOnline ->
                    // Jika jaringan baru saja kembali online
                    if (isOnline) {
                        try {
                            // Coba sinkronisasi data yang belum terkirim
                            repository.syncPendingData()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            // Selalu perbarui daftar alat di layar agar data terupdate
                            getAllAlat()
                        }
                    }
                }
        }
    }

    /**
     * Mengambil daftar semua alat dari Repository.
     *
     * Alur kerja:
     * 1. Set loading jadi true
     * 2. Jika online, coba sinkronisasi data pending terlebih dahulu
     * 3. Minta data ke repository (repository akan mengatur apakah ambil dari lokal atau server)
     * 4. Jika sukses, simpan hasilnya ke 'alatList' agar UI bisa menampilkannya
     * 5. Jika gagal, tampilkan pesan error
     */
    fun getAllAlat() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                // Mengecek koneksi internet
                if (com.example.perkapp.core.utils.NetworkUtils.isOnline(application)) {
                    try {
                        repository.syncPendingData()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // Meminta data dari repository dan menyimpannya di LiveData
                alatList.value = repository.getAllAlat()
            } catch (e: Exception) {
                // Menangkap exception dan menentukan jenis errornya
                val errorMsg = if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    "Gagal terhubung ke server. Periksa koneksi internet Anda."
                } else {
                    "Terjadi kesalahan saat memuat data alat. Silakan coba lagi."
                }
                errorMessage.value = errorMsg
            } finally {
                // Selalu matikan loading, baik proses sukses maupun gagal
                isLoading.value = false
            }
        }
    }

    /**
     * Menambahkan alat baru ke inventaris.
     *
     * @param name Nama alat
     * @param category Kategori alat
     * @param totalQty Jumlah total barang
     * @param condition Kondisi alat
     * @param imagePath Lokasi penyimpanan gambar (jika ada)
     */
    fun createAlat(name: String, category: String, totalQty: Int, condition: String, imagePath: String?) {
        // Cegah eksekusi ganda jika tombol diklik berkali-kali saat masih loading
        if (isLoading.value == true) return
        
        viewModelScope.launch {
            isLoading.value = true
            try {
                // Minta repository untuk menyimpan data alat baru
                repository.createAlat(name, category, totalQty, condition, imagePath)
                // Jadwalkan sinkronisasi di background lewat WorkManager untuk berjaga-jaga
                SyncManager.scheduleSyncWhenOnline(application)
            } catch (e: Exception) {
                // Jika sedang offline, pembuatan alat tetap berhasil di lokal
                val errorMsg = if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    "Gagal terhubung ke server. Alat akan disimpan dan disinkronkan nanti saat online."
                } else {
                    "Terjadi kesalahan saat menyimpan alat. Silakan coba lagi."
                }
                errorMessage.value = errorMsg
            } finally {
                // Refresh daftar alat dan matikan loading
                getAllAlat()
                isLoading.value = false
            }
        }
    }

    /**
     * Mengambil detail satu alat berdasarkan ID-nya.
     * Hasilnya disimpan di 'selectedAlat' agar bisa dibaca oleh layar Detail atau Edit.
     *
     * @param id ID unik alat yang ingin dicari
     */
    fun getAlatById(id: String) {
        viewModelScope.launch {
            selectedAlat.value = repository.getAlatById(id)
        }
    }

    /**
     * Memperbarui data alat yang sudah ada.
     *
     * @param alat Data lama alat sebelum diubah (AlatEntity)
     * @param request Data baru hasil perubahan yang diisi user di form edit
     */
    fun updateAlat(alat: AlatEntity, request: CreateAlatRequest) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                // Minta repository untuk memperbarui data
                repository.updateAlat(alat, request)
                // Jadwalkan sync jika data berubah dan perlu dikirim ke server nanti
                SyncManager.scheduleSyncWhenOnline(application)
            } catch (e: Exception) {
                val errorMsg = if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    "Gagal terhubung ke server. Perubahan akan disinkronkan nanti saat online."
                } else {
                    "Terjadi kesalahan saat memperbarui alat. Silakan coba lagi."
                }
                errorMessage.value = errorMsg
            } finally {
                getAllAlat()
                isLoading.value = false
            }
        }
    }

    /**
     * Menghapus alat dari inventaris.
     *
     * Mengecek dulu ke database lokal, apakah alat ini sedang digunakan/dipinjam 
     * oleh suatu kegiatan. Jika ya, penghapusan ditolak.
     *
     * @param id ID unik alat yang akan dihapus
     * @param onSuccess Fungsi yang dijalankan jika penghapusan sukses
     * @param onError Fungsi yang dijalankan jika penghapusan gagal
     */
    fun deleteAlat(id: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                // Cek apakah alat sedang dipinjam oleh kegiatan
                // Mengambil instance database secara langsung (bisa direfaktor ke repository nantinya)
                val db = com.example.perkapp.core.database.AppDatabase.getDatabase(application)
                val borrowings = db.kegiatanDao().getActiveBorrowingsForAlat(id)
                
                // Jika ada riwayat peminjaman yang masih aktif, batalkan penghapusan
                if (borrowings.isNotEmpty()) {
                    throw Exception("Alat sedang dipinjam oleh kegiatan dan tidak bisa dihapus!")
                }

                // Lanjut hapus jika aman
                repository.deleteAlat(id)
                // Jadwalkan sync agar penghapusan dikirim ke server saat online
                SyncManager.scheduleSyncWhenOnline(application)
                
                // Panggil callback berhasil
                onSuccess()
            } catch (e: Exception) {
                // Identifikasi error: apakah ditolak karena dipinjam, jaringan, atau lainnya
                val errorMsg = if (e.message?.contains("Alat sedang dipinjam") == true) {
                    e.message
                } else if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    "Gagal terhubung ke server. Penghapusan akan disinkronkan nanti."
                } else {
                    "Terjadi kesalahan saat menghapus alat. Silakan coba lagi."
                }
                errorMessage.value = errorMsg
                // Panggil callback gagal dengan pesan error
                onError(errorMsg ?: "Gagal menghapus alat")
            } finally {
                getAllAlat()
                isLoading.value = false
            }
        }
    }
}

/**
 * AlatViewModelFactory — "Pabrik" pembuat AlatViewModel.
 *
 * Karena AlatViewModel membutuhkan parameter (repository dan application) di konstruktornya,
 * kita tidak bisa membiarkan Android membuatkannya secara otomatis. Factory ini memberi tahu
 * Android bagaimana cara membuat instance AlatViewModel yang benar.
 */
class AlatViewModelFactory(
    private val repository: AlatRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
         if (modelClass.isAssignableFrom(AlatViewModel::class.java)) {
             @Suppress("UNCHECKED_CAST")
             return AlatViewModel(repository, application) as T
         }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}