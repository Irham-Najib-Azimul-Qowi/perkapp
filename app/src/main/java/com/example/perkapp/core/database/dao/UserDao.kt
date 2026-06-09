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
    /**
     * FUNGSI: insertUser
     * TUJUAN: Mencatat dan menyimpan data profil pengguna yang berhasil login ke database lokal.
     * Berkat argumen `OnConflictStrategy.REPLACE`, jika sudah ada akun sebelumnya,
     * akun tersebut akan tergantikan oleh profil yang baru.
     * @param user Model data berisi ID, nama, dan email pengguna.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * FUNGSI: getUser
     * TUJUAN: Mengambil rekam profil pengguna yang sedang beroperasi saat ini.
     * Mengembalikan `Flow` artinya fungsi ini memantau secara langsung (*real-time*). 
     * Kapan pun data di tabel `users` berubah, nilai baru otomatis dipancarkan ke komponen UI.
     * `LIMIT 1` menegaskan bahwa aplikasi ini dirancang *single-account* per sesi.
     * @return Flow dari UserEntity, atau null jika belum ada user (logout).
     */
    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): Flow<UserEntity?>

    /**
     * FUNGSI: loginUser
     * TUJUAN: Melakukan pengecekan kredensial (login) secara *offline*. 
     * Berguna jika user ingin masuk aplikasi tapi tidak ada sinyal internet,
     * aplikasi bisa mencocokkan inputan dengan sandi terakhir yang terekam di HP.
     * @param email Inputan dari field email (atau username).
     * @param password Inputan kata sandi.
     * @return Entitas pengguna bila email & sandi cocok, atau null jika salah.
     */
    @Query("SELECT * FROM users WHERE (email = :email OR name = :email) AND password = :password LIMIT 1")
    suspend fun loginUser(email: String, password: String): UserEntity?

    /**
     * FUNGSI: clearUser
     * TUJUAN: Membersihkan sesi profil pengguna dari lokal saat proses Logout.
     * Begitu tabel kosong, `Flow` dari getUser() akan menginfokan UI untuk pindah ke layar Login.
     */
    @Query("DELETE FROM users")
    suspend fun clearUser()
}
