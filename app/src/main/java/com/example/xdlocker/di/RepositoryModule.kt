package com.example.xdlocker.di

import android.content.Context
import com.example.xdlocker.data.database.DatabaseManager
import com.example.xdlocker.data.repository.MetadataRepository
import com.example.xdlocker.data.repository.PasswordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides DatabaseManager singleton
     */
    @Provides
    @Singleton
    fun provideDatabaseManager(@ApplicationContext context: Context): DatabaseManager {
        return DatabaseManager.getInstance(context)
    }

    /**
     * Provides MetadataRepository singleton
     */
    @Provides
    @Singleton
    fun provideMetadataRepository(databaseManager: DatabaseManager): MetadataRepository {
        return MetadataRepository(databaseManager)
    }

    /**
     * Provides PasswordRepository singleton
     */
    @Provides
    @Singleton
    fun providePasswordRepository(databaseManager: DatabaseManager): PasswordRepository {
        return PasswordRepository(databaseManager)
    }
}