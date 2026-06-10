package com.example.perkapp.model

import androidx.room.Entity // Mengimpor anotasi Entity Room
import androidx.room.PrimaryKey // Mengimpor anotasi PrimaryKey Room

/**
 * UserEntity — Struktur Data (Tabel) Profil Aktif.
 *
 * MENGAPA TABEL INI PENTING?
 * Tabel ini sangat eksklusif karena hanya boleh berisi MAKSIMAL 1 BARIS DATA.
 * Siapa pun yang datanya ada di tabel ini, dialah orang yang sedang "Memegang HP" 
 * dan aktif login ke dalam aplikasi. Jika user menekan tombol Logout, 
 * data di tabel ini akan dikosongkan (di-wipe).
 */
@Entity(tableName = "users") // Mendefinisikan kelas ini sebagai tabel users di database Room
data class UserEntity( // Mendeklarasikan data class UserEntity
    // ID unik pengguna dari server (Primary Key)
    @PrimaryKey // Menentukan id sebagai primary key unik
    val id: String, // ID unik pengguna
    // Nama lengkap pengguna
    val name: String, // Nama lengkap pengguna
    // Alamat email yang terdaftar
    val email: String, // Email pengguna
    // Password (disimpan opsional untuk keperluan login ulang saat offline)
    val password: String? = null, // Kata sandi opsional
    // Peran pengguna: "admin" atau "member"
    val role: String, // Hak akses peran pengguna
    // Kapan akun ini dibuat di server
    val created_at: String // String waktu pembuatan akun
)

