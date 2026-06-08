package com.example.perkapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.perkapp.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * UserDao — Data Access Object untuk tabel 'users'.
 *
 * Interface ini berisi query SQL untuk mengatur data akun pengguna 
 * yang sedang aktif (login) di aplikasi ini.
 */
// @Dao: memberitahu Room bahwa interface ini adalah Data Access Object
@Dao
interface UserDao {
    // Menyimpan data user ke database lokal
    // OnConflictStrategy.REPLACE: timpa data lama jika sudah ada user dengan ID yang sama
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Mengambil data user yang sedang login
    // Pakai Flow agar ViewModel otomatis mendapat update jika isi tabel ini berubah
    // LIMIT 1 memastikan hanya ada 1 akun yang aktif pada satu waktu
    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): Flow<UserEntity?>

    // Mencocokkan email/username dan password untuk login saat offline
    @Query("SELECT * FROM users WHERE (email = :email OR name = :email) AND password = :password LIMIT 1")
    suspend fun loginUser(email: String, password: String): UserEntity?

    // Menghapus data user lokal (dipanggil saat logout)
    @Query("DELETE FROM users")
    suspend fun clearUser()
}
