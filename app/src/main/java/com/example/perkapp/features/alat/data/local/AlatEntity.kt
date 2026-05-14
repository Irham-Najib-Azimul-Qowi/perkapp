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

    val sync_status: String = "Synced"
)
