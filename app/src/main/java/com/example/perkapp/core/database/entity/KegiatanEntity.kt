package com.example.perkapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
@Entity(tableName = "kegiatan")
data class KegiatanEntity(
    // ID unik kegiatan (menggunakan UUID lokal jika sedang offline)
    @PrimaryKey
    val id: String,
    // Nama atau judul acara/kegiatan
    val judul: String,
    // Kategori acara (contoh: "Praktikum", "Seminar")
    val kategori: String,
    // Lokasi di mana alat-alat ini akan digunakan
    val lokasi: String,
    // Tanggal pelaksanaan acara
    val tanggal: String,
    // Status acara (contoh: "BERLANGSUNG", "SELESAI", "DRAFT")
    val status: String, 
    // Daftar nama orang yang meminjam/bertanggung jawab
    val peminjam: String = "",
    // Catatan tambahan mengenai peminjaman ini
    val deskripsi: String = "",
    
    // Status sinkronisasi ("synced" = sudah ada di server, "pending" = antre dikirim)
    val sync_status: String = "synced",
    // Jenis tindakan yang sedang tertunda ("create", "update", "delete", atau null)
    val pending_action: String? = null,
    // ID dari akun user yang membuat catatan kegiatan ini
    val created_by: String? = null,
    // Penanda apakah daftar pinjaman alat sudah disetujui (biasanya oleh admin)
    val alat_approved: Boolean = false
)
