package com.example.perkapp.core.network

/**
 * ApiResponse — Wadah (Pembungkus) standar untuk setiap jawaban server.
 *
 * Server Laravel di aplikasi ini selalu mengirimkan JSON dengan format:
 * {
 *   "success": true/false,
 *   "message": "Pesan sukses/error",
 *   "data": { ...isi data... }
 * }
 * Kelas ini mempermudah kita membaca format baku tersebut.
 * Huruf <T> (Generic) artinya isi `data` bisa berubah-ubah 
 * (bisa berupa List, Objek Tunggal, atau Kosong).
 */
data class ApiResponse<T>(
    // Penanda berhasil atau gagal dari server
    val success: Boolean,
    // Pesan keterangan (contoh: "Data berhasil disimpan")
    val message: String,
    // Data intinya (contoh: profil user, daftar alat)
    val data: T?
)
