package com.example.perkapp.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.perkapp.core.database.dao.UserDao
import com.example.perkapp.core.database.dao.KegiatanDao
import com.example.perkapp.core.database.entity.UserEntity
import com.example.perkapp.core.database.entity.KegiatanEntity
import com.example.perkapp.core.database.entity.KegiatanAlatEntity
import com.example.perkapp.core.database.entity.RegisteredUserEntity
import com.example.perkapp.core.database.dao.RegisteredUserDao
import com.example.perkapp.features.alat.data.local.AlatDao
import com.example.perkapp.features.alat.data.local.AlatEntity
import com.example.perkapp.features.media.data.ImageDao
import com.example.perkapp.features.media.data.ImageEntity

/**
 * AppDatabase — Database lokal aplikasi menggunakan Room.
 *
 * Room adalah library Android untuk menyimpan data secara lokal di HP
 * menggunakan SQLite. Database ini adalah "gudang data offline" aplikasi.
 *
 * Semua data (alat, kegiatan, user, gambar) tersimpan di sini dan tetap
 * ada walaupun aplikasi ditutup atau tidak ada internet.
 *
 * Versi saat ini: 9 (setiap kali ada perubahan struktur tabel, versi harus naik)
 */
// @Database: memberitahu Room daftar tabel yang ada dan versi database
// entities: daftar semua kelas Entity yang akan dibuat tabelnya
// exportSchema: false → tidak ekspor skema ke file JSON (lebih simpel untuk proyek ini)
@Database(
    entities = [UserEntity::class, AlatEntity::class, ImageEntity::class, KegiatanEntity::class, KegiatanAlatEntity::class, RegisteredUserEntity::class],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // Setiap abstract fun di sini adalah "pintu masuk" ke satu tabel
    // Room akan otomatis membuatkan implementasinya
    abstract fun userDao(): UserDao
    abstract fun registeredUserDao(): RegisteredUserDao
    abstract fun alatDao(): AlatDao
    abstract fun imageDao(): ImageDao
    abstract fun kegiatanDao(): KegiatanDao

    companion object {
        // @Volatile: memastikan perubahan nilai INSTANCE langsung terlihat di semua thread
        // Ini penting agar tidak ada dua instance database yang dibuat bersamaan
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migrasi dari versi 1 ke 2: tambah kolom pending_action dan image_path pada tabel alat
        // Jika user update aplikasi dari versi lama ke baru, data lama tidak hilang, cuma ditambah kolom
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE alat ADD COLUMN pending_action TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    db.execSQL("ALTER TABLE alat ADD COLUMN image_path TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Migrasi dari versi 7 ke 8: tambah kolom created_by di tabel kegiatan
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE kegiatan ADD COLUMN created_by TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Migrasi dari versi 8 ke 9: tambah penanda apakah alat sudah disetujui (alat_approved)
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE kegiatan ADD COLUMN alat_approved INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * Mengambil instance database — hanya akan dibuat sekali (Singleton pattern).
         *
         * Singleton pattern: pastikan hanya ada SATU instance database di seluruh app.
         * Kenapa? Membuat banyak instance database bisa menyebabkan konflik data dan memori penuh.
         *
         * @param context — Context diperlukan Room untuk tahu lokasi penyimpanan database
         */
        fun getDatabase(context: Context): AppDatabase {
            // Jika instance sudah ada, langsung kembalikan (tidak buat baru)
            return INSTANCE ?: synchronized(this) {
                // synchronized: pastikan hanya satu thread yang bisa masuk blok ini sekaligus
                // Mencegah race condition saat dua coroutine minta database bersamaan
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "perkapp_database" // Nama file SQLite di penyimpanan internal HP
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_7_8, MIGRATION_8_9) // instruksi upgrade skema antar versi
                    .fallbackToDestructiveMigration() // jika gagal migrasi, hapus data lama & buat tabel baru dari nol
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
