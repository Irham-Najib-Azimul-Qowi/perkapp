package com.example.perkapp.features.alat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "alat")
data class AlatEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    val category: String,

    val total_qty: Int,

    val available_qty: Int,

    val condition: String,

    // Status sinkronisasi: "synced", "pending"
    val sync_status: String = "synced",

    val image_path: String? = null,

    // Aksi pending untuk sync: "create", "update", "delete", atau null jika sudah synced
    val pending_action: String? = null
)
