package com.example.xdlocker.security

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Replace with secure implementation using Android Keystore
@Singleton
class AppLockManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context // Example dependency
) : AppLockManager {

    companion object {
        private const val TAG = "AppLockManagerImpl"
        private var storedPin: String? = null // In-memory, insecure placeholder
    }

    override suspend fun setupPin(pin: String): PinResult<Unit> {
        if (pin.length < 4) {
            return PinResult.Error("PIN must be at least 4 digits.")
        }
        // In a real implementation, hash and store securely
        storedPin = pin
        Log.i(TAG, "PIN setup successful.")
        return PinResult.Success(Unit)
    }

    override suspend fun verifyPin(pin: String): PinResult<Boolean> {
        if (storedPin == null) {
            return PinResult.Error("PIN not configured.")
        }
        val isMatch = storedPin == pin
        return PinResult.Success(isMatch)
    }

    override suspend fun changePin(oldPin: String, newPin: String): PinResult<Unit> {
        if (storedPin == null) {
            return PinResult.Error("PIN not configured. Cannot change.")
        }
        if (storedPin != oldPin) {
            return PinResult.Error("Incorrect old PIN.")
        }
        if (newPin.length < 4) {
            return PinResult.Error("New PIN must be at least 4 digits.")
        }
        storedPin = newPin
        Log.i(TAG, "PIN changed successfully.")
        return PinResult.Success(Unit)
    }

    override suspend fun removePin(currentPin: String): PinResult<Unit> {
        if (storedPin == null) {
            // Or return Success if PIN not existing is considered successful removal
            return PinResult.Error("PIN not configured. Cannot remove.")
        }
        if (storedPin != currentPin) {
            return PinResult.Error("Incorrect PIN for removal.")
        }
        storedPin = null
        Log.i(TAG, "PIN removed successfully.")
        return PinResult.Success(Unit)
    }

    override suspend fun isPinConfigured(): Boolean {
        return storedPin != null
    }
}
