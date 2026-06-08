package com.example.perkapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * UserEntity — Merepresentasikan satu baris data di tabel 'users'.
 *
 * Kelas ini menyimpan informasi akun dari pengguna yang sedang aktif (login).
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
