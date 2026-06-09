package com.example.perkapp.core.database.dao // Paket data access object database lokal

import androidx.room.Dao // Mengimpor anotasi Dao Room
import androidx.room.Insert // Mengimpor anotasi Insert Room
import androidx.room.OnConflictStrategy // Mengimpor OnConflictStrategy Room
import androidx.room.Query // Mengimpor anotasi Query Room
import com.example.perkapp.core.database.entity.UserEntity // Mengimpor entitas UserEntity
import kotlinx.coroutines.flow.Flow // Mengimpor pustaka Flow dari Kotlin Coroutines

/**
 * UserDao — Data Access Object untuk tabel 'users'.
 *
 * Interface ini berisi query SQL untuk mengatur data akun pengguna 
 * yang sedang aktif (login) di aplikasi ini.
 */
@Dao // Menandai interface ini sebagai Data Access Object untuk tabel users
interface UserDao { // Deklarasi interface UserDao

    /**
     * FUNGSI: insertUser
     * TUJUAN: Mencatat dan menyimpan data profil pengguna yang berhasil login ke database lokal.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Operasi INSERT dengan menimpa data jika terjadi konflik
    suspend fun insertUser(user: UserEntity) // Fungsi asinkron suspend untuk menyimpan data user login

    /**
     * FUNGSI: getUser
     * TUJUAN: Mengambil rekam profil pengguna yang sedang beroperasi saat ini.
     */
    @Query("SELECT * FROM users LIMIT 1") // Query SQL untuk mengambil satu baris data user aktif login
    fun getUser(): Flow<UserEntity?> // Fungsi reaktif mengembalikan Flow pembungkus data user opsional

    /**
     * FUNGSI: loginUser
     * TUJUAN: Melakukan pengecekan kredensial (login) secara *offline*. 
     */
    @Query("SELECT * FROM users WHERE (email = :email OR name = :email) AND password = :password LIMIT 1") // Query SQL pencarian data user offline
    suspend fun loginUser(email: String, password: String): UserEntity? // Fungsi asinkron suspend untuk verifikasi masuk offline

    /**
     * FUNGSI: clearUser
     * TUJUAN: Membersihkan sesi profil pengguna dari lokal saat proses Logout.
     */
    @Query("DELETE FROM users") // Query SQL untuk membersihkan/menghapus seluruh data dari tabel users
    suspend fun clearUser() // Fungsi asinkron suspend untuk membersihkan sesi login user
}

