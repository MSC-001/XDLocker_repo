package com.example.xdlocker.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockStateService @Inject constructor() {

    // Tracks if the PIN screen has been successfully passed in the current app session.
    // Defaults to false, meaning the app lock screen should be shown if a PIN is configured.
    private val _unlockedInSession = MutableStateFlow(false)
    val unlockedInSession = _unlockedInSession.asStateFlow()

    fun unlockApp() {
        // TODO: Implement the logic for unlocking the app.
        // This might involve changing a stateFlow, a LiveData,
        // or a simple boolean flag that indicates the app is unlocked.
        println("AppLockStateService: App unlocked!") // Placeholder
    }

    fun onUnlockSucceeded() {
        _unlockedInSession.value = true
    }

    // Called when the app goes to the background or when a re-lock is explicitly needed.
    // This ensures that the next time the app comes to the foreground (and a PIN is set),
    // the AppLockScreen will be shown.
    fun onAppGoesToBackgroundOrLockNeeded() {
        _unlockedInSession.value = false
    }

    // Synchronous check, typically used by MainActivity during initial route determination.
    fun isCurrentlyUnlocked(): Boolean {
        return _unlockedInSession.value
    }
}
