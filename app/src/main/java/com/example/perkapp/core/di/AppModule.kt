package com.example.perkapp.core.di

import android.content.Context
import com.example.perkapp.core.database.AppDatabase
import com.example.perkapp.core.database.dao.KegiatanDao
import com.example.perkapp.features.alat.data.local.AlatDao
import com.example.perkapp.features.kegiatan.data.KegiatanRepository
import com.example.perkapp.features.kegiatan.data.KegiatanRepositoryImpl
import com.example.perkapp.features.kegiatan.api.KegiatanApiService
import com.example.perkapp.core.network.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideKegiatanDao(database: AppDatabase): KegiatanDao {
        return database.kegiatanDao()
    }

    @Provides
    @Singleton
    fun provideAlatDao(database: AppDatabase): AlatDao {
        return database.alatDao()
    }

    @Provides
    @Singleton
    fun provideKegiatanApiService(): KegiatanApiService {
        return RetrofitClient.instance.create(KegiatanApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideKegiatanRepository(
        apiService: KegiatanApiService,
        kegiatanDao: KegiatanDao,
        @ApplicationContext context: Context
    ): KegiatanRepository {
        return KegiatanRepositoryImpl(apiService, kegiatanDao, context)
    }
}
