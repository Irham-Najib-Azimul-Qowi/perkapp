package com.example.perkapp.features.alat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * AlatDao — Data Access Object untuk tabel 'alat'.
 *
 * Kumpulan perintah SQL yang dipermudah menjadi fungsi Kotlin untuk 
 * mengatur data inventaris alat di dalam database lokal (SQLite/Room).
 */
@Dao
interface AlatDao {
    /**
     * FUNGSI: getAllAlat
     * TUJUAN: Mengambil semua barang inventaris yang masih aktif dari database lokal.
     * Secara otomatis menyaring (filter) barang yang sudah ditandai untuk dihapus 
     * oleh pengguna (`pending_action != 'delete'`) saat perangkat sedang offline.
     * @return List entitas Alat.
     */
    @Query("SELECT * FROM alat WHERE pending_action != 'delete' OR pending_action IS NULL")
    suspend fun getAllAlat(): List<AlatEntity>

    /**
     * FUNGSI: getAllAlatIncludeDeleted
     * TUJUAN: Menarik *semua* data alat tanpa terkecuali, termasuk yang sedang 
     * mengantre untuk dihapus ke server. Fungsi ini sering dipakai oleh proses 
     * sinkronisasi (`SyncWorker`) agar ia tahu alat mana yang perlu dikirim perintah hapusnya.
     * @return List entitas Alat (termasuk yang 'deleted').
     */
    @Query("SELECT * FROM alat")
    suspend fun getAllAlatIncludeDeleted(): List<AlatEntity>

    /**
     * FUNGSI: getAlatById
     * TUJUAN: Mencari detail spesifik sebuah alat berdasarkan UUID uniknya.
     * Dipakai saat membuka halaman Detail Alat atau Edit Alat.
     * @param id String UUID alat yang dituju.
     * @return AlatEntity jika alat ditemukan, atau null jika ID salah/tidak ada.
     */
    @Query("SELECT * FROM alat WHERE id = :id")
    suspend fun getAlatById(id: String): AlatEntity?

    /**
     * FUNGSI: insertAlat
     * TUJUAN: Memasukkan satu alat baru ke dalam database lokal.
     * Strategi `REPLACE` berarti jika ada alat dengan ID yang sama, baris lama 
     * akan ditimpa dengan data alat yang baru ini.
     * @param alat Entitas alat tunggal.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlat(alat: AlatEntity)

    /**
     * FUNGSI: insertAllAlat
     * TUJUAN: Memasukkan daftar/kumpulan alat sekaligus secara massal (Bulk Insert).
     * Sangat efisien digunakan saat pertama kali mengunduh seluruh data inventaris 
     * dari server (sinkronisasi awal).
     * @param alat List daftar alat dari API.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAlat(alat: List<AlatEntity>)

    /**
     * FUNGSI: updateAlat
     * TUJUAN: Menerapkan perubahan (edit) pada data alat yang sudah tersimpan.
     * (Misalnya user mengubah kondisi alat dari "Baik" menjadi "Rusak").
     * @param alat Entitas alat yang datanya sudah berubah.
     */
    @Update
    suspend fun updateAlat(alat: AlatEntity)

    /**
     * FUNGSI: deleteAlat
     * TUJUAN: Menghapus data alat secara permanen dari penyimpanan lokal Android.
     * @param id String UUID barang yang akan dilenyapkan.
     */
    @Query("DELETE FROM alat WHERE id = :id")
    suspend fun deleteAlat(id: String)

    /**
     * FUNGSI: getPendingAlat
     * TUJUAN: Mengidentifikasi barang mana saja yang baru dibuat, diedit, atau 
     * dihapus secara offline (belum sempat terhubung ke backend).
     * Berbekal daftar ini, `SyncWorker` akan melakukan push ke server Laravel.
     * @return List entitas Alat berstatus 'pending'.
     */
    @Query("SELECT * FROM alat WHERE sync_status = 'pending'")
    suspend fun getPendingAlat(): List<AlatEntity>

    /**
     * FUNGSI: deleteAllAlat
     * TUJUAN: Menyapu bersih (mengosongkan) seluruh data tabel alat.
     * Biasa dipanggil sebagai bagian dari rutinitas `Logout`, agar data inventaris 
     * pengguna A tidak bisa dibaca jika pengguna B login di HP yang sama.
     */
    @Query("DELETE FROM alat")
    suspend fun deleteAllAlat()
}