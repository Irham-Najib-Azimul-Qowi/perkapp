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
 * SyncManager — "Mandor" yang bertugas mengatur jadwal SyncWorker.
 *
 * Kelas ini menyediakan fungsi bantuan agar bagian lain dari aplikasi
 * (seperti ViewModel) bisa dengan mudah meminta agar sinkronisasi dijalankan,
 * baik sekarang juga maupun menunggu sampai ada koneksi internet.
 */
object SyncManager {

    /**
     * Meminta WorkManager untuk menjalankan SyncWorker, tapi dengan syarat:
     * HANYA saat ada koneksi internet (NetworkType.CONNECTED).
     *
     * Jika saat ini tidak ada internet, WorkManager akan menyimpannya 
     * dan otomatis menjalankannya nanti ketika HP terhubung ke internet.
     */
    fun scheduleSyncWhenOnline(context: Context) {
        // Membuat syarat (constraint) bahwa harus terhubung ke jaringan internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Membuat permintaan kerja satu kali (bukan berulang)
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints) // Memasang syarat internet
            // Jika gagal (Result.retry()), tunggu dulu beberapa saat sebelum mencoba lagi.
            // EXPONENTIAL berarti waktunya akan dilipatgandakan (30s, 60s, 120s...) agar server tidak jebol.
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS
            )
            .build()

        // Memasukkan permintaan ke antrean WorkManager
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SyncWorker.WORK_NAME, // Nama unik pekerja
                ExistingWorkPolicy.REPLACE, // Jika sebelumnya sudah ada jadwal, ganti dengan yang baru
                syncRequest
            )
    }

    /**
     * Meminta WorkManager untuk langsung menjalankan sinkronisasi SEKARANG JUGA.
     * Tidak ada syarat koneksi internet di sini (WorkManager akan langsung jalan,
     * kalau tidak ada internet otomatis gagal dan akan me-retry).
     */
    fun syncNow(context: Context) {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10, TimeUnit.SECONDS // Jeda retry lebih cepat (10 detik) dibanding metode biasa
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SyncWorker.WORK_NAME + "_now", // Pakai nama sedikit berbeda agar tidak bertabrakan
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }
}
