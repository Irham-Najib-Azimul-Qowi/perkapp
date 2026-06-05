package com.example.perkapp.core.network

// Model standar untuk menangkap balasan JSON (success, message, data)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)
