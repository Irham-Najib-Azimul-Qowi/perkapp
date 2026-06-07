package com.example.perkapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registered_users")
data class RegisteredUserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val email: String,
    val role: String
)
