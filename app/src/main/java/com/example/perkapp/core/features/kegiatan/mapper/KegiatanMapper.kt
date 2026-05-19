package com.example.perkapp.features.kegiatan.mapper

import com.example.perkapp.features.kegiatan.api.InventoryStatsResponse
import com.example.perkapp.features.kegiatan.api.KegiatanResponse
import com.example.perkapp.features.kegiatan.domain.InventoryStats
import com.example.perkapp.features.kegiatan.domain.Kegiatan
import com.example.perkapp.features.kegiatan.domain.StatusKegiatan

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


// Mengubah KegiatanResponse (dari API) → Kegiatan (domain model)
fun KegiatanResponse.toDomain(): Kegiatan {
    return Kegiatan(
        id = this.id,
        kategori = this.kategori,
        judul = this.judul,
        lokasi = this.lokasi,
        labelWaktu = this.label_waktu,
        progress = this.progress.coerceIn(0f, 1f), // Pastikan nilai 0.0 - 1.0
        statusType = when (this.status.uppercase()) {
            // Konversi string "AKTIF" dari API → enum StatusKegiatan.AKTIF
            "AKTIF"       -> StatusKegiatan.AKTIF
            "MAINTENANCE" -> StatusKegiatan.MAINTENANCE
            "AUDIT"       -> StatusKegiatan.AUDIT
            else          -> StatusKegiatan.AKTIF  // default jika tidak dikenal
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