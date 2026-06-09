package com.example.perkapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * KegiatanAlatEntity — Struktur Data (Tabel) Penghubung Many-to-Many.
 *
 * MENGAPA TABEL INI ADA?
 * Karena satu "Kegiatan" (acara) bisa meminjam banyak "Alat" (barang), 
 * dan satu "Alat" bisa dipinjam oleh banyak "Kegiatan" yang berbeda pada waktu yang berbeda.
 * Tabel ini bertindak sebagai jembatan (Pivot Table) yang merekam rincian transaksi:
 * "Berapa banyak barang A yang dipinjam pada acara B, dan apakah sudah dikembalikan?"
 */
@Entity(tableName = "kegiatan_alat")
data class KegiatanAlatEntity(
    // ID unik untuk baris ini (kombinasi ID kegiatan dan ID alat)
    @PrimaryKey
    val id: String,
    // Merujuk ke ID Kegiatan di tabel 'kegiatan'
    val kegiatanId: String,
    // Merujuk ke ID Alat di tabel 'alat'
    val alatId: String,
    // Nama alat (disimpan di sini juga agar lebih cepat dimuat)
    val name: String,
    // Kategori alat tersebut
    val category: String,
    // Jumlah alat yang dipinjam
    val qty: Int,
    // true = alat ini adalah barang dari luar yang bukan milik lab
    val isExternal: Boolean,
    // true = alat sudah dikembalikan, false = masih dipakai
    val isReturned: Boolean,
    
    // Sama seperti tabel lain: untuk antrean pengiriman data ke server
    val sync_status: String = "synced",
    val pending_action: String? = null,
    
    // URL atau lokasi file gambar alat tersebut
    val image_path: String? = null
)
