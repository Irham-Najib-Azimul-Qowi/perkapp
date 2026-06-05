package com.example.perkapp.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.perkapp.core.database.AppDatabase
import com.example.perkapp.core.network.RetrofitClient
import com.example.perkapp.features.alat.api.AlatApiService
import com.example.perkapp.features.alat.data.repository.AlatRepository
import com.example.perkapp.features.media.api.MediaApiService
import com.example.perkapp.features.media.data.MediaRepository

/**
 * Worker yang menjalankan sinkronisasi data pending ke server.
 * Dipanggil otomatis oleh WorkManager saat ada koneksi internet.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)

        // Lakukan silent login sebelum sinkronisasi agar semua request terautentikasi
        RetrofitClient.performSilentLogin(applicationContext)

        // Sync Alat data
        val alatApi = RetrofitClient.instance.create(AlatApiService::class.java)
        val alatDao = database.alatDao()
        val alatRepository = AlatRepository(alatApi, alatDao, applicationContext)
        val alatSuccess = alatRepository.syncPendingData()

        // Sync Images
        val mediaApi = RetrofitClient.instance.create(MediaApiService::class.java)
        val imageDao = database.imageDao()
        val mediaRepository = MediaRepository(mediaApi, imageDao, applicationContext)
        val imageSuccess = mediaRepository.syncPendingImages()

        return if (alatSuccess && imageSuccess) {
            Result.success()
        } else {
            // Retry nanti jika ada yang gagal
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "perkapp_sync_worker"
    }
}
