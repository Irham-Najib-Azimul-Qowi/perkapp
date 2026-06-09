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
     * FUNGSI: scheduleSyncWhenOnline
     * TUJUAN: Menitipkan tugas kepada "mandor" bawaan Android (`WorkManager`). 
     * Memintanya untuk "Tolong jalankan SyncWorker, TAPI tunggu sampai HP ini 
     * mendapat koneksi internet (baik Wi-Fi maupun Seluler)."
     * 
     * ALUR LOGIKA PENGERJAAN:
     * 1. Menetapkan syarat ketat (`Constraints`): jaringan harus `CONNECTED`.
     * 2. Merancang kontrak kerja satu kali pakai (`OneTimeWorkRequestBuilder`).
     * 3. Mengonfigurasi strategi mundur-teratur (`setBackoffCriteria`): bila SyncWorker 
     *    gagal di tengah jalan, jangan langsung paksa ulang seketika. Tunggu 30 detik, 
     *    bila gagal lagi tunggu 60 detik, dst (`EXPONENTIAL`) agar server aman dari *spam*.
     * 4. Memasukkan tugas tersebut ke dalam antrean sistem dengan status `REPLACE` 
     *    (bila ada tugas sinkronisasi lama yang nyangkut, gantikan dengan yang baru).
     * 
     * @param context Konteks aplikasi.
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
     * FUNGSI: syncNow
     * TUJUAN: Kebalikan dari `scheduleSyncWhenOnline`. Memerintahkan `WorkManager` 
     * untuk langsung meledakkan (menjalankan) `SyncWorker` **detik ini juga** 
     * tanpa peduli sedang ada sinyal internet atau tidak.
     * 
     * Fungsi ini sangat krusial dipanggil ketika pengguna "me-refresh" halaman 
     * (pull-to-refresh) atau sesaat setelah ia berhasil mengklik tombol Login, 
     * guna memastikan halaman utama tidak diisi data usang. Jeda retri-nya pun diperpendek (10 detik).
     * 
     * @param context Konteks aplikasi.
     */
    fun syncNow(context: Context) {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10, TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SyncWorker.WORK_NAME + "_now",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }
}
