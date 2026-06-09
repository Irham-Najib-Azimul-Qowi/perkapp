package com.example.perkapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * RegisteredUserEntity — Struktur Data Direktori Akun (Buku Telepon Pengguna).
 *
 * MENGAPA TABEL INI ADA?
 * Saat Admin/Member membuat laporan peminjaman, mereka harus memilih "Siapa nama peminjamnya?".
 * Agar tidak salah ketik, aplikasi butuh daftar *Dropdown* berisi nama semua mahasiswa/dosen.
 * Tabel inilah yang menyimpan daftar nama-nama tersebut. Ia bertindak seperti 
 * buku kontak lokal di dalam HP.
 */
@Entity(tableName = "registered_users")
data class RegisteredUserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    // Peran pengguna: "admin" atau "member"
    val role: String
)
