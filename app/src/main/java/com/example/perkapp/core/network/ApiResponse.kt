package com.example.perkapp.core.network

/**
 * ApiResponse — Wadah (Pembungkus/Wrapper) Standar Respon Server.
 *
 * MENGAPA KELAS INI DIBUAT?
 * Backend (Laravel) yang melayani aplikasi ini selalu mengirim data dengan pola 
 * JSON yang kaku (seragam) di semua ujung-pangkal rute (endpoint).
 * Polanya selalu terdiri dari 3 pasang kunci:
 * 1. "success": penanda boolean (true/false) operasi di server berhasil.
 * 2. "message": pesan teks manusiawi (contoh: "Login Berhasil", "Kata sandi salah").
 * 3. "data": isi utama yang diminta (bisa daftar barang, token, atau null).
 *
 * Dengan mendefinisikan kelas `<T>` (Generic) ini, Retrofit tidak akan kebingungan 
 * menerjemahkan respon API menjadi objek Kotlin. Variabel `T` bisa diganti 
 * menjadi `UserEntity`, `List<AlatEntity>`, dsb sesuai kebutuhan halaman.
 */
data class ApiResponse<T>(
    // Penanda berhasil atau gagal dari server
    val success: Boolean,
    // Pesan keterangan (contoh: "Data berhasil disimpan")
    val message: String,
    // Data intinya (contoh: profil user, daftar alat)
    val data: T?
)
