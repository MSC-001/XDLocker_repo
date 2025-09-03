package com.example.xdlocker.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit // KTX for SharedPreferences
// Removed: import androidx.preference.contains // This was unused and causing confusion
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppLockManager {

    companion object {
        private const val PREFS_NAME = "app_lock_prefs"
        private const val KEY_PIN_CONFIGURED = "pin_configured"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"

        private const val ITERATION_COUNT = 65536
        private const val KEY_LENGTH = 256
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun setupPin(pin: String): PinResult<Unit> { // Fixed return type
        if (pin.length < 4) {
            return PinResult.Error("PIN must be at least 4 digits.")
        }
        return try {
            val salt = generateSalt()
            val hash = hashPin(pin, salt)

            prefs.edit { // KTX version
                putBoolean(KEY_PIN_CONFIGURED, true)
                putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                // apply() is implicit
            }
            PinResult.Success(Unit) // Fixed: Provide Unit for Success
        } catch (e: Exception) {
            PinResult.Error("Failed to set up PIN. ${e.message}")
        }
    }

    override suspend fun verifyPin(pin: String): PinResult<Boolean> {
        if (!isPinConfigured()) {
            // It's possible isPinConfigured itself could return PinResult<Boolean>
            // but for now, let's keep its signature simple and handle error here.
            return PinResult.Error("PIN not configured.")
        }
        return try {
            val saltString = prefs.getString(KEY_PIN_SALT, null)
            val hashString = prefs.getString(KEY_PIN_HASH, null)

            if (saltString == null || hashString == null) {
                prefs.edit { // KTX version
                    putBoolean(KEY_PIN_CONFIGURED, false)
                }
                return PinResult.Error("PIN data inconsistent. Please re-setup PIN.")
            }

            val salt = Base64.decode(saltString, Base64.NO_WRAP)
            val storedHash = Base64.decode(hashString, Base64.NO_WRAP)
            val providedPinHash = hashPin(pin, salt)
            val matches = storedHash.contentEquals(providedPinHash)

            // No explicit "Incorrect PIN" error, just return Success(false)
            PinResult.Success(matches)
        } catch (e: Exception) {
            PinResult.Error("Failed to verify PIN. ${e.message}")
        }
    }

    // Fixed parameter name to match interface
    override suspend fun removePin(currentPin: String): PinResult<Unit> { // Fixed return type
        val verificationResult = verifyPin(currentPin) // Use the corrected parameter name
        return when (verificationResult) {
            is PinResult.Success -> {
                if (verificationResult.data) {
                    prefs.edit { // KTX version
                        remove(KEY_PIN_CONFIGURED)
                        remove(KEY_PIN_SALT)
                        remove(KEY_PIN_HASH)
                    }
                    PinResult.Success(Unit) // Fixed: Provide Unit for Success
                } else {
                    PinResult.Error("Incorrect PIN. Cannot remove.") // This could be more generic too
                }
            }
            is PinResult.Error -> PinResult.Error("Cannot verify PIN to remove: ${verificationResult.message}")
        }
    }

    override suspend fun changePin(oldPin: String, newPin: String): PinResult<Unit> { // Fixed return type
        if (newPin.length < 4) {
            return PinResult.Error("New PIN must be at least 4 digits.")
        }
        val verificationResult = verifyPin(oldPin)
        return when (verificationResult) {
            is PinResult.Success -> {
                if (verificationResult.data) {
                    // Re-use setupPin's logic. setupPin itself returns PinResult<Unit>.
                    setupPin(newPin)
                } else {
                    PinResult.Error("Incorrect old PIN. Cannot change.")
                }
            }
            is PinResult.Error -> PinResult.Error("Cannot verify old PIN to change: ${verificationResult.message}")
        }
    }

    override fun isPinConfigured(): Boolean {
        // Corrected to use 'prefs' and 'KEY_PIN_CONFIGURED'
        return prefs.contains(KEY_PIN_CONFIGURED)
    }

    override fun forceClearPinConfiguration() {
        prefs.edit {
            remove(KEY_PIN_CONFIGURED)
            remove(KEY_PIN_SALT)
            remove(KEY_PIN_HASH)
            // apply() is implicit with KTX edit {}
        }
        println("AppLockManagerImpl: PIN configuration forcibly cleared.")
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
