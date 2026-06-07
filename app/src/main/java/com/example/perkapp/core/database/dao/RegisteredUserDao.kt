package com.example.perkapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.perkapp.core.database.entity.RegisteredUserEntity

@Dao
interface RegisteredUserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<RegisteredUserEntity>)

    @Query("SELECT * FROM registered_users")
    suspend fun getAllRegisteredUsers(): List<RegisteredUserEntity>

    @Query("DELETE FROM registered_users")
    suspend fun clearAll()
}
