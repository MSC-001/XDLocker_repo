package com.example.xdlocker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.zetetic.database.sqlcipher.SQLiteDatabase

@HiltAndroidApp
class XDLockerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize SQLCipher
        SQLiteDatabase.loadLibs(this)

        // Initialize other components if needed
        initializeApp()
    }

    private fun initializeApp() {
        // Any additional initialization can go here
        // For example: Crash reporting, analytics, etc.
    }

    override fun onTerminate() {
        super.onTerminate()
        // Cleanup when app terminates
    }
}