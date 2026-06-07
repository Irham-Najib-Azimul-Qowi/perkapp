package com.example.perkapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kegiatan")
data class KegiatanEntity(
    @PrimaryKey
    val id: String,
    val judul: String,
    val kategori: String,
    val lokasi: String,
    val tanggal: String,
    val status: String, // "BERLANGSUNG", "SELESAI", "DRAFT"
    val peminjam: String = "",
    val deskripsi: String = "",
    val sync_status: String = "synced",
    val pending_action: String? = null
)
