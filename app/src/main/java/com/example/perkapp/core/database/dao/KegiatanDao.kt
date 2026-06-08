package com.example.perkapp.core.database.dao

import androidx.room.*
import com.example.perkapp.core.database.entity.KegiatanEntity
import com.example.perkapp.core.database.entity.KegiatanAlatEntity

/**
 * KegiatanDao — Data Access Object untuk tabel 'kegiatan' dan 'kegiatan_alat'.
 *
 * Menyediakan fungsi-fungsi untuk membaca, menambah, mengubah, dan menghapus 
 * data riwayat peminjaman/kegiatan di database SQLite.
 */
@Dao
interface KegiatanDao {
    // Ambil semua kegiatan KECUALI yang sudah ditandai untuk dihapus (pending_action = 'delete')
    @Query("SELECT * FROM kegiatan WHERE pending_action != 'delete' OR pending_action IS NULL")
    suspend fun getAllKegiatan(): List<KegiatanEntity>

    // Cari kegiatan spesifik berdasarkan ID-nya
    @Query("SELECT * FROM kegiatan WHERE id = :id")
    suspend fun getKegiatanById(id: String): KegiatanEntity?

    // Simpan kegiatan baru atau timpa jika sudah ada
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKegiatan(kegiatan: KegiatanEntity)

    // Perbarui data kegiatan yang sudah ada
    @Update
    suspend fun updateKegiatan(kegiatan: KegiatanEntity)

    // Hapus kegiatan dari database lokal
    @Query("DELETE FROM kegiatan WHERE id = :id")
    suspend fun deleteKegiatan(id: String)

    // Ambil kegiatan yang berstatus 'pending' untuk disinkronkan ke server oleh SyncWorker
    @Query("SELECT * FROM kegiatan WHERE sync_status = 'pending'")
    suspend fun getPendingKegiatan(): List<KegiatanEntity>

    // Simpan rincian alat yang dipinjam pada sebuah kegiatan
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKegiatanAlat(alat: KegiatanAlatEntity)

    // Perbarui status alat yang dipinjam (misalnya: ditandai "sudah dikembalikan")
    @Update
    suspend fun updateKegiatanAlat(alat: KegiatanAlatEntity)

    // Ambil daftar alat apa saja yang dipinjam di dalam satu kegiatan
    @Query("SELECT * FROM kegiatan_alat WHERE kegiatanId = :kegiatanId")
    suspend fun getAlatForKegiatan(kegiatanId: String): List<KegiatanAlatEntity>

    // Ambil alat-alat dalam kegiatan yang perlu di-sync ke server
    @Query("SELECT * FROM kegiatan_alat WHERE sync_status = 'pending'")
    suspend fun getPendingKegiatanAlat(): List<KegiatanAlatEntity>

    // Hapus semua rincian alat jika kegiatannya dihapus
    @Query("DELETE FROM kegiatan_alat WHERE kegiatanId = :kegiatanId")
    suspend fun deleteKegiatanAlatForKegiatan(kegiatanId: String)

    // Mengecek apakah suatu alat (ID tertentu) sedang dipinjam dan BELUM dikembalikan
    // Berguna untuk mencegah alat dihapus permanen dari inventaris saat masih dipakai
    @Query("SELECT * FROM kegiatan_alat WHERE alatId = :alatId AND isReturned = 0")
    suspend fun getActiveBorrowingsForAlat(alatId: String): List<KegiatanAlatEntity>

    // Menandai persetujuan peminjaman alat di sebuah kegiatan oleh admin
    @Query("UPDATE kegiatan SET alat_approved = 1 WHERE id = :kegiatanId")
    suspend fun approveAlatForKegiatan(kegiatanId: String)
}
