package com.example.perkapp.model

/**
 * CreateAlatRequest — Model DTO untuk mengirim data Alat BARU ke Server.
 *
 * Hanya berisi data-data form yang diketik/dipilih oleh pengguna.
 */
data class CreateAlatRequest(
    val name: String,
    val category: String,
    val total_qty: Int,
    val condition: String,
    // URL gambar (sementara opsional/belum terintegrasi penuh via Multipart)
    val image_path: String? = null
)
