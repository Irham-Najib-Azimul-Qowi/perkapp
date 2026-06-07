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
 * Worker yang menjalankan sinkronisasi data pending ke server.
 * Dipanggil otomatis oleh WorkManager saat ada koneksi internet.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)

        val userPrefs = com.example.perkapp.core.datastore.UserPreferences(applicationContext.dataStore)
        val retrofit = RetrofitClient.getClient(userPrefs)

        // Sync Alat data
        val alatApi = retrofit.create(AlatApiService::class.java)
        val alatDao = database.alatDao()
        val alatRepository = AlatRepository(alatApi, alatDao, applicationContext)
        val alatSuccess = alatRepository.syncPendingData()

        // Sync Kegiatan data
        val kegiatanApi = retrofit.create(com.example.perkapp.features.kegiatan.api.KegiatanApiService::class.java)
        val kegiatanDao = database.kegiatanDao()
        val kegiatanRepository = com.example.perkapp.features.kegiatan.data.KegiatanRepositoryImpl(kegiatanApi, kegiatanDao, applicationContext)
        val kegiatanSuccess = kegiatanRepository.syncPendingKegiatan()

        // Sync Images
        val mediaApi = retrofit.create(MediaApiService::class.java)
        val imageDao = database.imageDao()
        val mediaRepository = MediaRepository(mediaApi, imageDao, applicationContext)
        val imageSuccess = mediaRepository.syncPendingImages()

        return if (alatSuccess && imageSuccess && kegiatanSuccess) {
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
