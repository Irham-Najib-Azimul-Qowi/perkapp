package com.example.perkapp.features.alat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AlatEntity — Merepresentasikan satu baris data inventaris di tabel 'alat'.
 *
 * Menyimpan rincian alat-alat lab (seperti Kamera, Lensa, dll).
 */
@Entity(tableName = "alat")
data class AlatEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    
    // Stok awal alat (misal beli 10 buah)
    val total_qty: Int,
    
    // Sisa stok alat saat ini yang bisa dipinjam
    val available_qty: Int,
    
    // Kondisi barang (misal: "Baik", "Rusak Ringan")
    val condition: String,

    // Status sinkronisasi: "synced" (aman di server) atau "pending" (belum terkirim)
    val sync_status: String = "synced",

    // URL atau lokasi file gambar alat
    val image_path: String? = null,

    // Aksi offline yang tertunda untuk disinkronkan ke server:
    // "create" (bikin baru), "update" (diedit), "delete" (dihapus), atau null (kalau sudah sinkron)
    val pending_action: String? = null
)
