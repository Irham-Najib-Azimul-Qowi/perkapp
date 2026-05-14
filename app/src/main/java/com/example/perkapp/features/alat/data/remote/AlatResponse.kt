package com.example.perkapp.features.alat.data.remote

data class AlatResponse(

    val id: String,

    val name: String,

    val category: String,

    val total_qty: Int,

    val available_qty: Int,

    val condition: String
)
