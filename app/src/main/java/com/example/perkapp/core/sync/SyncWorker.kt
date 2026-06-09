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
     * FUNGSI: doWork
     * TUJUAN: Inilah jantung dari sistem *offline-first* aplikasi Perkapp.
     * `doWork` adalah metode suspend yang berjalan di luar utas utama (UI Thread),
     * ia mengoordinasikan tiga jenis data (Alat, Kegiatan, dan Media) untuk dikirim 
     * ke server (atau diunduh bila ada hal baru).
     * 
     * ALUR LOGIKA PENGERJAAN:
     * 1. Mengakses `AppDatabase` dan `RetrofitClient` melalui suntikan manual 
     *    karena `Worker` tidak di-inject langsung oleh Hilt dalam versi ini.
     * 2. Menjalankan tiga fungsi sinkronisasi dari repositori masing-masing secara berurutan:
     *    - `alatRepository.syncPendingData()`
     *    - `kegiatanRepository.syncPendingKegiatan()`
     *    - `mediaRepository.syncPendingImages()`
     * 3. Jika ke-3 proses mengembalikan status BOLEAN `true` (artinya tidak ada *Exception* / 
     *    jaringan stabil 100%), maka operasi disimpulkan Sukses (`Result.success()`).
     * 4. Bila satu saja gagal (misal server mati, paket internet habis), Worker merespons 
     *    dengan `Result.retry()` yang akan memberitahu Android untuk mencobanya lagi 
     *    dalam puluhan detik atau menit ke depan.
     * 
     * @return `Result` yang menentukan nasib dari eksekusi *background task* ini.
     */
    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val userPrefs = com.example.perkapp.core.datastore.UserPreferences(applicationContext.dataStore)
        val retrofit = RetrofitClient.getClient(userPrefs)

        // 1. Proses Sinkronisasi Data Alat (Inventaris)
        val alatApi = retrofit.create(AlatApiService::class.java)
        val alatDao = database.alatDao()
        val alatRepository = AlatRepository(alatApi, alatDao, applicationContext)
        val alatSuccess = alatRepository.syncPendingData()

        // 2. Proses Sinkronisasi Data Kegiatan (Log Peminjaman)
        val kegiatanApi = retrofit.create(com.example.perkapp.features.kegiatan.api.KegiatanApiService::class.java)
        val kegiatanDao = database.kegiatanDao()
        val kegiatanRepository = com.example.perkapp.features.kegiatan.data.KegiatanRepositoryImpl(kegiatanApi, kegiatanDao, applicationContext)
        val kegiatanSuccess = kegiatanRepository.syncPendingKegiatan()

        // 3. Proses Sinkronisasi Media (Upload Foto Barang/Bukti)
        val mediaApi = retrofit.create(MediaApiService::class.java)
        val imageDao = database.imageDao()
        val mediaRepository = MediaRepository(mediaApi, imageDao, applicationContext)
        val imageSuccess = mediaRepository.syncPendingImages()

        // Evaluasi Akhir
        return if (alatSuccess && imageSuccess && kegiatanSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        // Nama unik untuk membedakan worker ini dengan tugas background lainnya
        const val WORK_NAME = "perkapp_sync_worker"
    }
}
