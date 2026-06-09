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
    /**
     * FUNGSI: insertAll
     * TUJUAN: Menyimpan daftar massal (Bulk Insert) seluruh pengguna yang ada di sistem server 
     * ke dalam database lokal perangkat (Room). Jika ID pengguna sudah terdaftar, 
     * datanya akan ditimpa dengan versi terbaru (`OnConflictStrategy.REPLACE`).
     * @param users List daftar pengguna dari API.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<RegisteredUserEntity>)

    /**
     * FUNGSI: getAllRegisteredUsers
     * TUJUAN: Membaca daftar seluruh pengguna sistem yang telah diunduh dan tersimpan di HP.
     * Sangat berguna saat perangkat offline (tidak ada sinyal), aplikasi tetap bisa 
     * menampilkan pilihan nama "Peminjam" pada form tambah kegiatan tanpa perlu tembak API.
     * @return List entitas pengguna terdaftar.
     */
    @Query("SELECT * FROM registered_users")
    suspend fun getAllRegisteredUsers(): List<RegisteredUserEntity>

    /**
     * FUNGSI: clearAll
     * TUJUAN: Mengosongkan seluruh tabel `registered_users`.
     * Metode ini umumnya dipanggil persis sebelum menarik (fetch) data terbaru dari server,
     * agar data pengguna lama yang mungkin sudah dihapus oleh admin di server tidak 
     * menumpuk dan menjadi data "hantu" di HP.
     */
    @Query("DELETE FROM registered_users")
    suspend fun clearAll()
}
