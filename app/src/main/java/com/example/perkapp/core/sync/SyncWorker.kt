package com.example.perkapp.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.perkapp.core.database.AppDatabase
import com.example.perkapp.core.network.RetrofitClient
import com.example.perkapp.core.datastore.dataStore
import com.example.perkapp.features.alat.api.AlatApiService
import com.example.perkapp.features.alat.data.repository.AlatRepository
import com.example.perkapp.features.media.api.MediaApiService
import com.example.perkapp.features.media.data.MediaRepository

/**
 * SyncWorker — Pekerja background yang mensinkronisasi data pending ke server.
 *
 * SyncWorker adalah "kurir yang bekerja di balik layar". Tugasnya:
 * - Mengambil semua data yang belum terkirim ke server (status "pending")
 * - Mengirimkan data tersebut ke server saat ada koneksi internet
 * - Mengirim ulang (retry) jika pengiriman gagal
 *
 * SyncWorker dijalankan oleh WorkManager — sistem penjadwalan Android yang
 * bisa menjalankan tugas bahkan setelah aplikasi ditutup.
 *
 * Extends CoroutineWorker (bukan Worker biasa) agar bisa pakai suspend function
 * untuk operasi database dan network yang bersifat asynchronous.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    /**
     * Fungsi utama yang dijalankan WorkManager secara otomatis.
     *
     * Return value menentukan nasib worker ini:
     * - Result.success() → selesai, WorkManager tidak akan jadwalkan ulang
     * - Result.retry()   → ada yang gagal, WorkManager akan coba lagi nanti
     * - Result.failure() → gagal total, tidak akan dicoba lagi
     */
    override suspend fun doWork(): Result {
        // Menyiapkan akses ke database lokal
        val database = AppDatabase.getDatabase(applicationContext)

        // Menyiapkan akses ke pengaturan user (untuk ambil token) dan Retrofit (untuk internet)
        val userPrefs = com.example.perkapp.core.datastore.UserPreferences(applicationContext.dataStore)
        val retrofit = RetrofitClient.getClient(userPrefs)

        // 1. Proses Sinkronisasi Data Alat
        val alatApi = retrofit.create(AlatApiService::class.java)
        val alatDao = database.alatDao()
        val alatRepository = AlatRepository(alatApi, alatDao, applicationContext)
        val alatSuccess = alatRepository.syncPendingData()

        // 2. Proses Sinkronisasi Data Kegiatan
        val kegiatanApi = retrofit.create(com.example.perkapp.features.kegiatan.api.KegiatanApiService::class.java)
        val kegiatanDao = database.kegiatanDao()
        val kegiatanRepository = com.example.perkapp.features.kegiatan.data.KegiatanRepositoryImpl(kegiatanApi, kegiatanDao, applicationContext)
        val kegiatanSuccess = kegiatanRepository.syncPendingKegiatan()

        // 3. Proses Sinkronisasi Gambar (Media)
        val mediaApi = retrofit.create(MediaApiService::class.java)
        val imageDao = database.imageDao()
        val mediaRepository = MediaRepository(mediaApi, imageDao, applicationContext)
        val imageSuccess = mediaRepository.syncPendingImages()

        // Mengecek apakah seluruh proses berhasil
        return if (alatSuccess && imageSuccess && kegiatanSuccess) {
            // Jika sukses semua, hentikan worker
            Result.success()
        } else {
            // Jika ada satu saja yang gagal (misal internet putus di tengah jalan),
            // minta WorkManager untuk menjadwalkan ulang nanti (retry)
            Result.retry()
        }
    }

    companion object {
        // Nama unik untuk membedakan worker ini dengan tugas background lainnya
        const val WORK_NAME = "perkapp_sync_worker"
    }
}
