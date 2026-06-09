package com.example.perkapp.core.database.entity // Paket entitas database lokal

import androidx.room.Entity // Mengimpor anotasi Entity Room
import androidx.room.PrimaryKey // Mengimpor anotasi PrimaryKey Room

/**
 * KegiatanAlatEntity — Struktur Data (Tabel) Penghubung Many-to-Many.
 *
 * MENGAPA TABEL INI ADA?
 * Karena satu "Kegiatan" (acara) bisa meminjam banyak "Alat" (barang), 
 * dan satu "Alat" bisa dipinjam oleh banyak "Kegiatan" yang berbeda pada waktu yang berbeda.
 * Tabel ini bertindak sebagai jembatan (Pivot Table) yang merekam rincian transaksi:
 * "Berapa banyak barang A yang dipinjam pada acara B, dan apakah sudah dikembalikan?"
 */
@Entity(tableName = "kegiatan_alat") // Mendefinisikan kelas sebagai tabel kegiatan_alat di database Room
data class KegiatanAlatEntity( // Deklarasi data class KegiatanAlatEntity
    // ID unik untuk baris ini (kombinasi ID kegiatan dan ID alat)
    @PrimaryKey // Menetapkan variabel id sebagai Primary Key unik di tabel
    val id: String, // ID unik pivot relasi kegiatan-alat
    // Merujuk ke ID Kegiatan di tabel 'kegiatan'
    val kegiatanId: String, // ID kegiatan luar
    // Merujuk ke ID Alat di tabel 'alat'
    val alatId: String, // ID alat inventaris
    // Nama alat (disimpan di sini juga agar lebih cepat dimuat)
    val name: String, // Nama alat
    // Kategori alat tersebut
    val category: String, // Kategori alat
    // Jumlah alat yang dipinjam
    val qty: Int, // Jumlah alat yang disewa/dipinjam
    // true = alat ini adalah barang dari luar yang bukan milik lab
    val isExternal: Boolean, // Penanda alat eksternal/dari luar
    // true = alat sudah dikembalikan, false = masih dipakai
    val isReturned: Boolean, // Status pengembalian alat
    
    // Sama seperti tabel lain: untuk antrean pengiriman data ke server
    val sync_status: String = "synced", // Status sinkronisasi pivot ke server
    val pending_action: String? = null, // Aksi antrean sinkronisasi tertunda
    
    // URL atau lokasi file gambar alat tersebut
    val image_path: String? = null // Path/tautan gambar alat pivot
)

