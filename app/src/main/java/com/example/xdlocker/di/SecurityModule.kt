package com.example.xdlocker.di

import com.example.xdlocker.security.AppLockManager
import com.example.xdlocker.security.AppLockManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindAppLockManager(impl: AppLockManagerImpl): AppLockManager
}
