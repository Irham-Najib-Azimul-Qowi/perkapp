package com.example.perkapp.features.alat.data.remote

data class CreateAlatRequest(
    val name: String,

    val category: String,

    val total_qty: Int,

    val condition: String,

    val image_path: String? = null
)
