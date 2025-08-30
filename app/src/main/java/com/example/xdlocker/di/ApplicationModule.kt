package com.example.xdlocker.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.xdlocker.data.database.DatabaseManager
import com.example.xdlocker.utils.SecurityUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    /**
     * Provides DatabaseManager singleton
     */
    @Provides
    @Singleton
    fun provideDatabaseManager(@ApplicationContext context: Context): DatabaseManager {
        return DatabaseManager.getInstance(context)
    }

    /**
     * Provides regular SharedPreferences for non-sensitive data
     */
    @Provides
    @Singleton
    @RegularPreferences
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("xdlocker_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Provides encrypted SharedPreferences for sensitive data
     */
    @Provides
    @Singleton
    @EncryptedPreferences
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "xdlocker_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Provides SecurityUtils singleton
     */
    @Provides
    @Singleton
    fun provideSecurityUtils(): SecurityUtils {
        return SecurityUtils
    }
}

/**
 * Qualifiers for different types of SharedPreferences
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RegularPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EncryptedPreferences