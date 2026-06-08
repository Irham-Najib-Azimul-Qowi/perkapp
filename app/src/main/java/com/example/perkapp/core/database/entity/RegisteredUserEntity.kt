package com.example.perkapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * RegisteredUserEntity — Merepresentasikan tabel 'registered_users'.
 *
 * Menyimpan data *semua* pengguna yang pernah didaftarkan ke sistem.
 * Tabel ini beda dengan 'users'. Tabel 'users' cuma untuk 1 orang (yang sedang login).
 * Tabel ini untuk banyak orang, fungsinya sebagai daftar nama "Peminjam" alat.
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
