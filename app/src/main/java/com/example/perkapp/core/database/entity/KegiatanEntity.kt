package com.example.perkapp.core.database.entity // Paket entitas database lokal core

import androidx.room.Entity // Mengimpor anotasi Entity Room
import androidx.room.PrimaryKey // Mengimpor anotasi PrimaryKey Room

/**
 * KegiatanEntity — Struktur Data (Tabel) Induk untuk Peminjaman.
 *
 * MENGAPA TABEL INI PENTING?
 * Tabel ini merekam "Kapan, di mana, dan untuk keperluan apa" alat-alat lab 
 * dikeluarkan dari ruangan. Setiap baris di tabel ini adalah formulir pengajuan 
 * peminjaman barang. Kolom `sync_status` dan `pending_action` memastikan bahwa
 * jika user menekan tombol "Simpan" saat sedang di lapangan tanpa internet, 
 * datanya aman di HP dan akan diluncurkan ke server otomatis setelah dapat sinyal.
 */
@Entity(tableName = "kegiatan") // Mendefinisikan kelas sebagai tabel kegiatan di Room Database
data class KegiatanEntity( // Deklarasi data class KegiatanEntity
    // ID unik kegiatan (menggunakan UUID lokal jika sedang offline)
    @PrimaryKey // Menetapkan variabel id sebagai Primary Key unik
    val id: String, // ID unik kegiatan
    // Nama atau judul acara/kegiatan
    val judul: String, // Judul kegiatan
    // Kategori acara (contoh: "Praktikum", "Seminar")
    val kategori: String, // Kategori kegiatan
    // Lokasi di mana alat-alat ini akan digunakan
    val lokasi: String, // Lokasi kegiatan
    // Tanggal pelaksanaan acara
    val tanggal: String, // Tanggal kegiatan
    // Status acara (contoh: "BERLANGSUNG", "SELESAI", "DRAFT")
    val status: String, // Status pengerjaan kegiatan
    // Daftar nama orang yang meminjam/bertanggung jawab
    val peminjam: String = "", // Nama-nama peminjam alat
    // Catatan tambahan mengenai peminjaman ini
    val deskripsi: String = "", // Uraian deskripsi kegiatan
    
    // Status sinkronisasi ("synced" = sudah ada di server, "pending" = antre dikirim)
    val sync_status: String = "synced", // Status sinkronisasi ke server
    // Jenis tindakan yang sedang tertunda ("create", "update", "delete", atau null)
    val pending_action: String? = null, // Aksi antrean sinkronisasi
    // ID dari akun user yang membuat catatan kegiatan ini
    val created_by: String? = null, // ID pencipta kegiatan
    // Penanda apakah daftar pinjaman alat sudah disetujui (biasanya oleh admin)
    val alat_approved: Boolean = false // Status approval alat oleh admin
)

