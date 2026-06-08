package com.example.perkapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.perkapp.core.database.entity.RegisteredUserEntity

/**
 * RegisteredUserDao — Data Access Object untuk tabel 'registered_users'.
 *
 * Digunakan untuk menyimpan daftar seluruh pengguna (user) yang terdaftar 
 * di aplikasi ini (bukan cuma user yang sedang login, tapi SEMUA user).
 * Daftar ini biasanya dipakai untuk memilih nama "Peminjam" saat membuat kegiatan.
 */
@Dao
interface RegisteredUserDao {
    // Menyimpan daftar banyak user sekaligus ke database lokal
    // Jika ada ID yang sama, timpa dengan data terbaru dari server
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<RegisteredUserEntity>)

    // Mengambil semua daftar user yang tersimpan di memori HP
    @Query("SELECT * FROM registered_users")
    suspend fun getAllRegisteredUsers(): List<RegisteredUserEntity>

    // Menghapus seluruh daftar user (misal: saat mau ambil data baru dari server)
    @Query("DELETE FROM registered_users")
    suspend fun clearAll()
}
