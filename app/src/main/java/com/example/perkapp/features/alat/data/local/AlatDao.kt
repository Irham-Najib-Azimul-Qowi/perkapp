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
    // Mengambil semua alat yang masih aktif (belum dihapus oleh user)
    // Syarat: pending_action-nya bukan 'delete'
    @Query("SELECT * FROM alat WHERE pending_action != 'delete' OR pending_action IS NULL")
    suspend fun getAllAlat(): List<AlatEntity>

    // Mengambil semua alat tanpa terkecuali (termasuk yang mau dihapus)
    @Query("SELECT * FROM alat")
    suspend fun getAllAlatIncludeDeleted(): List<AlatEntity>

    // Mencari satu alat secara spesifik berdasarkan ID-nya
    @Query("SELECT * FROM alat WHERE id = :id")
    suspend fun getAlatById(id: String): AlatEntity?

    // Menyimpan atau menimpa satu alat ke database lokal
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlat(alat: AlatEntity)

    // Menyimpan daftar banyak alat sekaligus (biasanya dipakai saat pertama kali sync dari server)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAlat(alat: List<AlatEntity>)

    // Mengubah data alat yang sudah ada
    @Update
    suspend fun updateAlat(alat: AlatEntity)

    // Menghapus satu alat secara permanen dari database lokal
    @Query("DELETE FROM alat WHERE id = :id")
    suspend fun deleteAlat(id: String)

    // Mengambil daftar alat yang punya perubahan lokal namun belum dikirim ke server (offline mode)
    @Query("SELECT * FROM alat WHERE sync_status = 'pending'")
    suspend fun getPendingAlat(): List<AlatEntity>

    // Mengosongkan seluruh tabel alat (misal: saat user logout)
    @Query("DELETE FROM alat")
    suspend fun deleteAllAlat()
}