package com.example.perkapp.features.media.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * FUNGSI: ImageEntity
 * TUJUAN: Merepresentasikan tabel 'images' untuk menyimpan referensi gambar.
 *
 * ALUR LOGIKA PENGERJAAN:
 * Tabel ini sangat penting untuk offline-first. Saat kita memotret alat/kegiatan 
 * tanpa internet, aplikasi belum bisa mengunggah gambarnya (dapat URL).
 * Jadi, aplikasi akan menyimpan rute "lokal" ke file gambar (local_path).
 * Nanti kalau ada internet, baru diunggah untuk mendapatkan "image_url" dari server.
 */
@Entity(tableName =  "images")
data class ImageEntity(
    // ID unik lokal gambar (biasanya UUID lokal)
    @PrimaryKey
    val id: String,
    // Gambar ini milik entitas apa? (contoh: "alat" atau "kegiatan")
    val entity_type: String,
    // ID entitas pemiliknya (contoh: ID dari kamera canon)
    val entity_id: String,
    // Link alamat web gambar (diisi jika sukses upload ke server)
    val image_url: String,
    // Alamat direktori fisik gambar di HP pengguna (misal: file:///storage/emulated/...)
    val local_path: String = "",
    // Status sinkronisasi: "pending" jika sedang menunggu di-upload, "synced" jika beres
    val sync_status: String = "synced"
)
