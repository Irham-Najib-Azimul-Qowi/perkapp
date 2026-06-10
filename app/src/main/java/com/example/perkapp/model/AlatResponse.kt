package com.example.perkapp.model

/**
 * AlatResponse — Model Data Transfer Object (DTO) untuk menangkap data Alat dari Server.
 *
 * Struktur variabel di sini harus SAMA PERSIS dengan apa yang dikirim oleh server Laravel.
 */
data class AlatResponse(
    val id: String,
    val name: String,
    val category: String,
    val total_qty: Int,
    val available_qty: Int,
    val condition: String,
    // Daftar foto alat (opsional, karena bisa jadi belum ada foto)
    val images: List<ImageResponse>? = null
)

/**
 * ImageResponse — Bagian dari AlatResponse untuk menangkap rincian gambar alat.
 */
data class ImageResponse(
    val id: String,
    val entity_type: String,
    val entity_id: String,
    val image_url: String
)
