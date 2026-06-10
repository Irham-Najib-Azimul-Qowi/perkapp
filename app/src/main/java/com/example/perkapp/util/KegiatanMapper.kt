package com.example.perkapp.util

import com.example.perkapp.network.InventoryStatsResponse
import com.example.perkapp.network.KegiatanResponse
import com.example.perkapp.model.InventoryStats
import com.example.perkapp.model.Kegiatan
import com.example.perkapp.model.StatusKegiatan

// ============================================================
// FILE: KegiatanMapper.kt
// LOKASI: features/kegiatan/mapper/KegiatanMapper.kt
// FUNGSI: Mengubah (mapping) data dari format API (DTO)
//         ke format domain model yang dipakai UI.
//
// Kenapa perlu Mapper?
// - Data dari API formatnya mengikuti backend (snake_case, string, dll)
// - Data di UI butuh format Kotlin yang bersih (camelCase, enum, dll)
// - Kalau backend berubah, kita cukup ubah mapper ini saja,
//   tidak perlu ubah ViewModel atau Screen
// ============================================================


import com.example.perkapp.repository.parseDescription

// Mengubah KegiatanResponse (dari API) → Kegiatan (domain model)
fun KegiatanResponse.toDomain(): Kegiatan {
    val parsed = parseDescription(this.description)
    return Kegiatan(
        id = this.id,
        kategori = parsed.kategori.ifBlank { "Umum" },
        judul = this.name ?: "",
        lokasi = parsed.lokasi.ifBlank { "Unknown" },
        labelWaktu = this.date ?: "",
        progress = if (this.status?.uppercase() == "SELESAI") 1f else 0f,
        statusType = when (this.status?.uppercase() ?: "AKTIF") {
            "AKTIF", "BERLANGSUNG" -> StatusKegiatan.AKTIF
            "MAINTENANCE"          -> StatusKegiatan.MAINTENANCE
            "AUDIT"                -> StatusKegiatan.AUDIT
            else                   -> StatusKegiatan.AKTIF
        }
    )
}


// Mengubah List<KegiatanResponse> → List<Kegiatan>
// Fungsi extension ini memudahkan mapping banyak data sekaligus
fun List<KegiatanResponse>.toDomainList(): List<Kegiatan> {
    return this.map { it.toDomain() }
}


// Mengubah InventoryStatsResponse (dari API) → InventoryStats (domain model)
fun InventoryStatsResponse.toDomain(): InventoryStats {
    return InventoryStats(
        borrowedCount = this.borrowed_count,
        availableCount = this.available_count,
        pendingSyncCount = this.pending_sync_count
    )
}
