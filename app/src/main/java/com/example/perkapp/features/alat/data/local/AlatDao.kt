package com.example.perkapp.features.alat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AlatDao {
    @Query("SELECT * FROM alat WHERE pending_action != 'delete' OR pending_action IS NULL")
    suspend fun getAllAlat(): List<AlatEntity>

    @Query("SELECT * FROM alat")
    suspend fun getAllAlatIncludeDeleted(): List<AlatEntity>

    @Query("SELECT * FROM alat WHERE id = :id")
    suspend fun getAlatById(id: String): AlatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlat(alat: AlatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAlat(alat: List<AlatEntity>)

    @Update
    suspend fun updateAlat(alat: AlatEntity)

    @Query("DELETE FROM alat WHERE id = :id")
    suspend fun deleteAlat(id: String)

    // Ambil semua data yang belum ter-sync (pending)
    @Query("SELECT * FROM alat WHERE sync_status = 'pending'")
    suspend fun getPendingAlat(): List<AlatEntity>

    @Query("DELETE FROM alat")
    suspend fun deleteAllAlat()
}