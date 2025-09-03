package com.example.xdlocker.security

interface AppLockManager {
    /**
     * Sets up a new PIN.
     * Returns PinResult.Success(Unit) if successful, PinResult.Error otherwise.
     */
    suspend fun setupPin(pin: String): PinResult<Unit>

    /**
     * Verifies the given PIN against the stored PIN.
     * Returns PinResult.Success(true) if PIN matches.
     * Returns PinResult.Success(false) if PIN does not match.
     * Returns PinResult.Error for other issues (e.g., PIN not configured, storage error).
     */
    suspend fun verifyPin(pin: String): PinResult<Boolean>

    /**
     * Changes the existing PIN.
     * Requires the old PIN for verification.
     * Returns PinResult.Success(Unit) if successful, PinResult.Error otherwise.
     */
    suspend fun changePin(oldPin: String, newPin: String): PinResult<Unit>

    /**
     * Removes the currently configured PIN.
     * Returns PinResult.Success(Unit) if successful, PinResult.Error otherwise.
     * (Consider if this needs verification, e.g. by requiring current PIN)
     */
    suspend fun removePin(currentPin: String): PinResult<Unit> // Added currentPin for verification

    /**
     * Checks if a PIN is currently configured.
     * Returns true if a PIN is set, false otherwise.
     */
    fun isPinConfigured(): Boolean

    /**
     * Forcibly clears all PIN configuration data (hashed PIN, salt, configured flag).
     * This method does not require the current PIN and is used for administrative resets,
     * such as after clearing all application data.
     */
    fun forceClearPinConfiguration()
}
