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
     * FUNGSI: getAllAlat
     * TUJUAN: Menjadi pintu masuk data dari Repository ke UI (Layar).
     *
     * ALUR LOGIKA PENGERJAAN:
     * 1. Mengubah `isLoading` menjadi true agar Spinner muncul di layar.
     * 2. Jika internet hidup, secara agresif memaksa `repository.syncPendingData()`
     *    untuk mengirim barang-barang yang masih nyangkut.
     * 3. Meminta seluruh daftar alat (`getAllAlat`) dari Repository dan menampungnya ke `alatList`.
     * 4. Jika koneksi putus (Error), tampilkan pesan "Gagal terhubung".
     * 5. Pada blok `finally`, matikan efek Loading.
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
     * FUNGSI: createAlat
     * TUJUAN: Menjadi jembatan ketika tombol "Simpan" ditekan di layar `TambahAlatScreen`.
     *
     * ALUR LOGIKA PENGERJAAN:
     * 1. Mencegah Dobel Klik: Cek `isLoading.value == true`, jika ya, batalkan aksi.
     * 2. Nyalakan Loading.
     * 3. Minta Repository membuatkan data.
     * 4. Menjadwalkan WorkManager (`SyncManager.scheduleSyncWhenOnline()`) sebagai jaring 
     *    pengaman jika proses sinkronisasi gagal di tengah jalan.
     * 5. *Refresh* (Panggil `getAllAlat()`) agar alat yang baru terbuat langsung nongol di layar.
     *
     * @param name Nama barang.
     * @param category Kategori barang.
     * @param totalQty Jumlah awal.
     * @param condition Kondisi (Baik/Rusak).
     * @param imagePath Lokasi foto.
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
     * FUNGSI: getAlatById
     * TUJUAN: Menarik satu data spesifik dari gudang penyimpanan.
     * Hasil pencarian akan ditaruh di keranjang `selectedAlat`.
     * Layar `DetailAlatScreen` dan `EditAlatScreen` sudah berlangganan ke keranjang ini,
     * sehingga saat datanya masuk, layarnya langsung merender informasi alat tersebut.
     *
     * @param id ID unik alat.
     */
    fun getAlatById(id: String) {
        viewModelScope.launch {
            selectedAlat.value = repository.getAlatById(id)
        }
    }

    /**
     * FUNGSI: updateAlat
     * TUJUAN: Mengeksekusi penyimpanan hasil edit (Ubah Alat).
     *
     * @param alat Objek data barang versi usang (Sebelum diedit).
     * @param request Bungkusan data form yang baru diisi.
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
     * FUNGSI: deleteAlat
     * TUJUAN: Menghapus alat dengan perlindungan ekstra.
     *
     * ALUR LOGIKA PENGERJAAN:
     * 1. Validasi Silang (Cross-Check): Sebelum menghapus, ia mengintip tabel `KegiatanAlatEntity`.
     *    Jika alat ini ternyata masih dipinjam/sedang dipakai di sebuah kegiatan (`borrowings.isNotEmpty()`),
     *    maka proses Hapus akan DICEGAT dengan pesan galak "Alat sedang dipinjam!".
     * 2. Jika alat nganggur/bebas, serahkan pada Repository untuk dihapus secara halus (Soft-Delete).
     * 3. Jadwalkan `WorkManager` untuk melaporkannya ke server nanti.
     *
     * @param id ID unik alat.
     * @param onSuccess Callback jika berhasil menghapus.
     * @param onError Callback jika gagal (menampilkan Toast kemarahan).
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