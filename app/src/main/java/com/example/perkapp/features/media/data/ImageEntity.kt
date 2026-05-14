package com.example.perkapp.features.media.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName =  "images")
data class ImageEntity(
    @PrimaryKey
    val id: String,
    val entity_type: String,
    val entity_id: String,
    val image_url: String,
    val local_path: String = "",
    val sync_status: String = "synced"
)
