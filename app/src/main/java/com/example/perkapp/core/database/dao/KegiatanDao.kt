package com.example.perkapp.core.database.dao

import androidx.room.*
import com.example.perkapp.core.database.entity.KegiatanEntity
import com.example.perkapp.core.database.entity.KegiatanAlatEntity

@Dao
interface KegiatanDao {
    @Query("SELECT * FROM kegiatan WHERE pending_action != 'delete' OR pending_action IS NULL")
    suspend fun getAllKegiatan(): List<KegiatanEntity>

    @Query("SELECT * FROM kegiatan WHERE id = :id")
    suspend fun getKegiatanById(id: String): KegiatanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKegiatan(kegiatan: KegiatanEntity)

    @Update
    suspend fun updateKegiatan(kegiatan: KegiatanEntity)

    @Query("DELETE FROM kegiatan WHERE id = :id")
    suspend fun deleteKegiatan(id: String)

    @Query("SELECT * FROM kegiatan WHERE sync_status = 'pending'")
    suspend fun getPendingKegiatan(): List<KegiatanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKegiatanAlat(alat: KegiatanAlatEntity)

    @Update
    suspend fun updateKegiatanAlat(alat: KegiatanAlatEntity)

    @Query("SELECT * FROM kegiatan_alat WHERE kegiatanId = :kegiatanId")
    suspend fun getAlatForKegiatan(kegiatanId: String): List<KegiatanAlatEntity>

    @Query("SELECT * FROM kegiatan_alat WHERE sync_status = 'pending'")
    suspend fun getPendingKegiatanAlat(): List<KegiatanAlatEntity>

    @Query("DELETE FROM kegiatan_alat WHERE kegiatanId = :kegiatanId")
    suspend fun deleteKegiatanAlatForKegiatan(kegiatanId: String)

    @Query("SELECT * FROM kegiatan_alat WHERE alatId = :alatId AND isReturned = 0")
    suspend fun getActiveBorrowingsForAlat(alatId: String): List<KegiatanAlatEntity>

    @Query("UPDATE kegiatan SET alat_approved = 1 WHERE id = :kegiatanId")
    suspend fun approveAlatForKegiatan(kegiatanId: String)
}
