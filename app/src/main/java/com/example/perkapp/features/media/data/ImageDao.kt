package com.example.perkapp.features.media.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * ImageDao — Data Access Object untuk tabel 'images'.
 *
 * Bertugas mengurus data gambar-gambar secara lokal, baik gambar yang sudah 
 * tersimpan rapi dari server (URL) maupun gambar foto langsung dari HP 
 * yang masih tertahan belum bisa di-upload.
 */
@Dao
interface ImageDao {
    // Mengambil semua gambar milik satu item (misal: semua gambar untuk alat dengan ID 'X')
    @Query("SELECT * FROM images WHERE entity_type = :type AND entity_id = :entityId")
    suspend fun getImagesForEntity(type: String, entityId: String): List<ImageEntity>

    // Menyimpan referensi satu gambar baru ke database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity)

    // Menyimpan banyak gambar sekaligus
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllImages(images: List<ImageEntity>)

    // Menghapus data gambar dari database lokal
    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImage(id: String)

    // Mencari gambar-gambar fisik lokal yang belum berhasil dikirim (di-upload) ke server
    // (Akan dipanggil terus oleh SyncWorker tiap kali internet nyala)
    @Query("SELECT * FROM images WHERE sync_status = 'pending'")
    suspend fun getPendingImages(): List<ImageEntity>

    // Memperbarui informasi gambar (contoh: mengubah status jadi 'synced' setelah sukses upload)
    @Update
    suspend fun updateImage(image: ImageEntity)

    // Mencari gambar berdasarkan alamat URL-nya
    @Query("SELECT * FROM images WHERE image_url = :imageUrl LIMIT 1")
    suspend fun getImageByUrl(imageUrl: String): ImageEntity?
}