package com.example.perkapp.model

import androidx.room.Entity // Mengimpor komponen anotasi Entity Room
import androidx.room.PrimaryKey // Mengimpor komponen PrimaryKey Room

/**
 * RegisteredUserEntity — Struktur Data Direktori Akun (Buku Telepon Pengguna).
 *
 * MENGAPA TABEL INI ADA?
 * Saat Admin/Member membuat laporan peminjaman, mereka harus memilih "Siapa nama peminjamnya?".
 * Agar tidak salah ketik, aplikasi butuh daftar *Dropdown* berisi nama semua mahasiswa/dosen.
 * Tabel inilah yang menyimpan daftar nama-nama tersebut. Ia bertindak seperti 
 * buku kontak lokal di dalam HP.
 */
@Entity(tableName = "registered_users") // Mendefinisikan kelas ini sebagai tabel database bernama registered_users
data class RegisteredUserEntity( // Mendeklarasikan data class RegisteredUserEntity
    @PrimaryKey // Menentukan properti di bawah ini sebagai Primary Key unik di tabel
    val id: String, // Variabel string penyimpan ID pengguna unik
    val name: String, // Variabel string nama pengguna
    val email: String, // Variabel string email pengguna
    // Peran pengguna: "admin" atau "member"
    val role: String // Variabel string peran hak akses pengguna
)

