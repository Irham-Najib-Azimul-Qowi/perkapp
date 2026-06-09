package com.example.perkapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * UserEntity — Struktur Data (Tabel) Profil Aktif.
 *
 * MENGAPA TABEL INI PENTING?
 * Tabel ini sangat eksklusif karena hanya boleh berisi MAKSIMAL 1 BARIS DATA.
 * Siapa pun yang datanya ada di tabel ini, dialah orang yang sedang "Memegang HP" 
 * dan aktif login ke dalam aplikasi. Jika user menekan tombol Logout, 
 * data di tabel ini akan dikosongkan (di-wipe).
 */
@Entity(tableName = "users")
data class UserEntity(
    // ID unik pengguna dari server (Primary Key)
    @PrimaryKey
    val id: String,
    // Nama lengkap pengguna
    val name: String,
    // Alamat email yang terdaftar
    val email: String,
    // Password (disimpan opsional untuk keperluan login ulang saat offline)
    val password: String? = null,
    // Peran pengguna: "admin" atau "member"
    val role: String,
    // Kapan akun ini dibuat di server
    val created_at: String
)
