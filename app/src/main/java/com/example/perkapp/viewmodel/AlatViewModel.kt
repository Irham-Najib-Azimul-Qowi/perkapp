package com.example.perkapp.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.perkapp.sync.SyncManager
import com.example.perkapp.model.AlatEntity
import com.example.perkapp.model.CreateAlatRequest
import com.example.perkapp.repository.AlatRepository
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
    // Parameter repository sebagai penyedia tunggal data alat (local & remote)
    private val repository: AlatRepository,
    // Parameter application untuk akses context global Android (cek internet, worker, dll)
    private val application: Application
) : ViewModel(){
    
    // LiveData reaktif penyimpan daftar semua entitas alat untuk dibaca oleh UI daftar alat
    val alatList = MutableLiveData<List<AlatEntity>>()
    
    // LiveData penyimpan satu entitas alat yang terpilih (untuk detail atau edit)
    val selectedAlat = MutableLiveData<AlatEntity?>()
    
    // LiveData penunjuk status loading (putar progress bar jika bernilai true)
    val isLoading = MutableLiveData(false)
    
    // LiveData penyimpan pesan kesalahan untuk ditampilkan sebagai Toast/Snackbar di UI
    val errorMessage = MutableLiveData<String?>()

    init {
        // Saat pertama kali ViewModel dibuat, langsung ambil data alat terbaru
        getAllAlat()
        // Daftarkan pengamat perubahan koneksi internet
        observeNetworkChanges()
    }

    /**
     * Memantau status koneksi internet HP secara real-time.
     * Jika HP kembali online, otomatis mengirim sisa data pending ke server.
     */
    private fun observeNetworkChanges() {
        // Meluncurkan coroutine dalam cakupan daur hidup ViewModel
        viewModelScope.launch {
            // Berlangganan status jaringan (true jika ada internet, false jika offline)
            com.example.perkapp.util.NetworkUtils.observeNetworkStatus(application)
                .collect { isOnline ->
                    // Jika terdeteksi HP baru saja mendapatkan koneksi internet kembali
                    if (isOnline) {
                        try {
                            // Memicu sinkronisasi data-data offline yang masih tertunda
                            repository.syncPendingData()
                        } catch (e: Exception) {
                            // Cetak error ke logcat jika sinkronisasi background gagal
                            e.printStackTrace()
                        } finally {
                            // Selalu segarkan kembali daftar data alat di layar
                            getAllAlat()
                        }
                    }
                }
        }
    }

    /**
     * FUNGSI: getAllAlat
     * TUJUAN: Memuat seluruh daftar alat dari database lokal ke UI.
     */
    fun getAllAlat() {
        // Meluncurkan coroutine latar belakang
        viewModelScope.launch {
            // Aktifkan indikator loading berputar di layar
            isLoading.value = true
            try {
                // Cek apakah perangkat terhubung internet saat ini
                if (com.example.perkapp.util.NetworkUtils.isOnline(application)) {
                    try {
                        // Jika ada internet, usahakan langsung sinkronkan antrean pending terlebih dahulu
                        repository.syncPendingData()
                    } catch (e: Exception) {
                        // Abaikan kegagalan sinkronisasi kecil di sini agar pemuatan data utama tidak terhambat
                        e.printStackTrace()
                    }
                }
                // Ambil daftar data alat dari repository dan taruh ke LiveData untuk dibaca UI
                alatList.value = repository.getAllAlat()
            } catch (e: Exception) {
                // Tentukan pesan kesalahan berdasarkan jenis exception jaringan atau lainnya
                val errorMsg = if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    "Gagal terhubung ke server. Periksa koneksi internet Anda."
                } else {
                    "Terjadi kesalahan saat memuat data alat. Silakan coba lagi."
                }
                // Kirim pesan error ke LiveData agar UI memunculkan Toast
                errorMessage.value = errorMsg
            } finally {
                // Matikan indikator loading di layar
                isLoading.value = false
            }
        }
    }

    /**
     * FUNGSI: createAlat
     * TUJUAN: Menambahkan data alat baru secara offline-first.
     */
    fun createAlat(name: String, category: String, totalQty: Int, condition: String, imagePath: String?) {
        // Jika sedang melakukan proses loading lain, hentikan pemicuan dobel
        if (isLoading.value == true) return
        
        // Meluncurkan coroutine latar belakang
        viewModelScope.launch {
            // Aktifkan status loading
            isLoading.value = true
            try {
                // Simpan alat baru ke database lokal lewat repository (flag pending_action = 'create')
                repository.createAlat(name, category, totalQty, condition, imagePath)
                // Jadwalkan WorkManager untuk sinkronisasi di latar belakang saat online nanti
                SyncManager.scheduleSyncWhenOnline(application)
            } catch (e: Exception) {
                // Tangani exception jika terjadi kegagalan jaringan saat coba sync instan
                val errorMsg = if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    "Gagal terhubung ke server. Alat akan disimpan dan disinkronkan nanti saat online."
                } else {
                    "Terjadi kesalahan saat menyimpan alat. Silakan coba lagi."
                }
                // Set LiveData error message agar UI menampilkan Toast
                errorMessage.value = errorMsg
            } finally {
                // Ambil ulang data alat terbaru untuk memperbarui layar
                getAllAlat()
                // Matikan loading
                isLoading.value = false
            }
        }
    }

    /**
     * FUNGSI: getAlatById
     * TUJUAN: Mengambil satu data alat spesifik berdasarkan ID.
     */
    fun getAlatById(id: String) {
        // Jalankan coroutine
        viewModelScope.launch {
            // Ambil data dari lokal dan simpan ke selectedAlat LiveData
            selectedAlat.value = repository.getAlatById(id)
        }
    }

    /**
     * FUNGSI: updateAlat
     * TUJUAN: Memperbarui data alat yang sudah ada.
     */
    fun updateAlat(alat: AlatEntity, request: CreateAlatRequest) {
        // Jalankan coroutine
        viewModelScope.launch {
            // Aktifkan loading
            isLoading.value = true
            try {
                // Kirim perintah update data alat ke repository
                repository.updateAlat(alat, request)
                // Jadwalkan sinkronisasi otomatis via WorkManager
                SyncManager.scheduleSyncWhenOnline(application)
            } catch (e: Exception) {
                // Tentukan jenis pesan error
                val errorMsg = if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    "Gagal terhubung ke server. Perubahan akan disinkronkan nanti saat online."
                } else {
                    "Terjadi kesalahan saat memperbarui alat. Silakan coba lagi."
                }
                errorMessage.value = errorMsg
            } finally {
                // Segarkan data di layar dan matikan loading
                getAllAlat()
                isLoading.value = false
            }
        }
    }

    /**
     * FUNGSI: deleteAlat
     * TUJUAN: Menghapus alat dengan perlindungan pengecekan peminjaman aktif.
     */
    fun deleteAlat(id: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        // Jalankan coroutine
        viewModelScope.launch {
            // Aktifkan loading
            isLoading.value = true
            try {
                // Ambil database lokal secara langsung untuk mengecek relasi kegiatan aktif
                val db = com.example.perkapp.database.AppDatabase.getDatabase(application)
                // Ambil daftar peminjaman aktif alat ini pada kegiatan yang belum selesai
                val borrowings = db.kegiatanDao().getActiveBorrowingsForAlat(id)
                
                // Jika masih ada kegiatan yang meminjam alat ini, cegat proses hapus
                if (borrowings.isNotEmpty()) {
                    throw Exception("Alat sedang dipinjam oleh kegiatan dan tidak bisa dihapus!")
                }

                // Jika aman, lanjutkan penghapusan via repository
                repository.deleteAlat(id)
                // Jadwalkan sinkronisasi WorkManager untuk menghapus alat dari server
                SyncManager.scheduleSyncWhenOnline(application)
                
                // Panggil callback sukses
                onSuccess()
            } catch (e: Exception) {
                // Tangkap pesan kesalahan
                val errorMsg = if (e.message?.contains("Alat sedang dipinjam") == true) {
                    e.message
                } else if (e is java.net.UnknownHostException || e is java.net.ConnectException) {
                    "Gagal terhubung ke server. Penghapusan akan disinkronkan nanti."
                } else {
                    "Terjadi kesalahan saat menghapus alat. Silakan coba lagi."
                }
                errorMessage.value = errorMsg
                // Jalankan callback error
                onError(errorMsg ?: "Gagal menghapus alat")
            } finally {
                // Perbarui daftar layar dan matikan loading
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
