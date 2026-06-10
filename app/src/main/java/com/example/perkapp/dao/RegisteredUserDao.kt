package com.example.perkapp.dao

import androidx.room.Dao // Mengimpor anotasi Dao untuk Room
import androidx.room.Insert // Mengimpor anotasi Insert untuk penyimpanan data ke database
import androidx.room.OnConflictStrategy // Mengimpor opsi penanganan konflik data duplikat
import androidx.room.Query // Mengimpor anotasi Query untuk menulis query SQL manual
import com.example.perkapp.model.RegisteredUserEntity // Mengimpor entitas model RegisteredUserEntity

/**
 * RegisteredUserDao — Data Access Object untuk tabel 'registered_users'.
 *
 * Digunakan untuk menyimpan daftar seluruh pengguna (user) yang terdaftar 
 * di aplikasi ini (bukan cuma user yang sedang login, tapi SEMUA user).
 * Daftar ini biasanya dipakai untuk memilih nama "Peminjam" saat membuat kegiatan.
 */
@Dao // Menandai interface ini sebagai DAO agar Room memproses kueri database
interface RegisteredUserDao { // Deklarasi interface RegisteredUserDao

    /**
     * FUNGSI: insertAll
     * TUJUAN: Menyimpan daftar massal seluruh pengguna ke database lokal.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Menggunakan operasi INSERT dengan strategi menimpa jika konflik
    suspend fun insertAll(users: List<RegisteredUserEntity>) // Fungsi asinkron suspend untuk menyimpan data massal

    /**
     * FUNGSI: getAllRegisteredUsers
     * TUJUAN: Membaca daftar seluruh pengguna sistem yang tersimpan di HP.
     */
    @Query("SELECT * FROM registered_users") // Query SQL untuk menarik seluruh kolom dari tabel registered_users
    suspend fun getAllRegisteredUsers(): List<RegisteredUserEntity> // Fungsi asinkron suspend pengambil data terdaftar

    /**
     * FUNGSI: clearAll
     * TUJUAN: Mengosongkan seluruh tabel `registered_users`.
     */
    @Query("DELETE FROM registered_users") // Query SQL untuk menghapus bersih semua baris dari tabel registered_users
    suspend fun clearAll() // Fungsi asinkron suspend untuk menghapus tabel
}

