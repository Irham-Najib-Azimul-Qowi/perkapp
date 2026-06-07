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

@Database(
    entities = [UserEntity::class, AlatEntity::class, ImageEntity::class, KegiatanEntity::class, KegiatanAlatEntity::class, RegisteredUserEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun registeredUserDao(): RegisteredUserDao
    abstract fun alatDao(): AlatDao
    abstract fun imageDao(): ImageDao
    abstract fun kegiatanDao(): KegiatanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migrasi dari versi 1 ke 2: tambah kolom pending_action dan image_path pada tabel alat
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "perkapp_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
