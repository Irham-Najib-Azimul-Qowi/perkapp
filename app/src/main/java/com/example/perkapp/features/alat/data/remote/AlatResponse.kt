package com.example.perkapp.features.alat.data.remote

data class AlatResponse(
    val id: String,
    val name: String,
    val category: String,
    val total_qty: Int,
    val available_qty: Int,
    val condition: String,
    val images: List<ImageResponse>? = null
)

data class ImageResponse(
    val id: String,
    val entity_type: String,
    val entity_id: String,
    val image_url: String
)
