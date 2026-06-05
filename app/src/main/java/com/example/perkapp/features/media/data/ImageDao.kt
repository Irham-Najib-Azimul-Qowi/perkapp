package com.example.perkapp.features.media.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE entity_type = :type AND entity_id = :entityId")
    suspend fun getImagesForEntity(type: String, entityId: String): List<ImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllImages(images: List<ImageEntity>)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImage(id: String)

    // Ambil semua gambar yang belum ter-sync (pending)
    @Query("SELECT * FROM images WHERE sync_status = 'pending'")
    suspend fun getPendingImages(): List<ImageEntity>

    @Update
    suspend fun updateImage(image: ImageEntity)

    @Query("SELECT * FROM images WHERE image_url = :imageUrl LIMIT 1")
    suspend fun getImageByUrl(imageUrl: String): ImageEntity?
}