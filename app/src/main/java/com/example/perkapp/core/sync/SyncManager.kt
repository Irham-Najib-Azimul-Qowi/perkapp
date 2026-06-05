package com.example.perkapp.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Manager untuk mengatur sinkronisasi data pending.
 * Menjadwalkan SyncWorker setiap kali ada data baru yang perlu di-sync.
 */
object SyncManager {

    /**
     * Jadwalkan sync saat koneksi internet tersedia.
     * Menggunakan OneTimeWorkRequest dengan constraint CONNECTED.
     * Jika sudah ada sync yang dijadwalkan, akan digantikan.
     */
    fun scheduleSyncWhenOnline(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SyncWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }

    /**
     * Jalankan sync segera (tanpa menunggu).
     * Tetap membutuhkan koneksi internet.
     */
    fun syncNow(context: Context) {
        scheduleSyncWhenOnline(context)
    }
}
