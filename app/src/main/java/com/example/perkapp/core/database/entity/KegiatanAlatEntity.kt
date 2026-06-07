package com.example.perkapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kegiatan_alat")
data class KegiatanAlatEntity(
    @PrimaryKey
    val id: String,
    val kegiatanId: String,
    val alatId: String,
    val name: String,
    val category: String,
    val qty: Int,
    val isExternal: Boolean,
    val isReturned: Boolean,
    val sync_status: String = "synced",
    val pending_action: String? = null,
    val image_path: String? = null
)

