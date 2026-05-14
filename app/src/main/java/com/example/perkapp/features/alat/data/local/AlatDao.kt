package com.example.perkapp.features.alat.data.local

import androidx.core.view.WindowInsetsCompat
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AlatDao {
    @Query("select * from Alat")
    suspend fun getAllAlat(): List<AlatEntity>

    @Query("select * from alat where id = :id")
    suspend fun getAlatById(id: String): AlatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlat(alat: AlatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAlat(alat: List<AlatEntity>)

    @Update
    suspend fun updateAlat(alat: AlatEntity)

    @Query("delete from alat where id = :id")
    suspend fun deleteAlat(id: String)
}