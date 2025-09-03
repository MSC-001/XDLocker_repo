package com.example.xdlocker.viewmodel

import android.content.Context
import android.util.Log // Added for logging
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit // For SharedPreferences KTX
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdlocker.data.dao.UserDatabaseInfoDao
import com.example.xdlocker.data.database.SQLCipherHelper
import com.example.xdlocker.data.entities.UserDatabaseInfo
import com.example.xdlocker.data.repository.MetadataRepository
import com.example.xdlocker.security.AppLockManager
import com.example.xdlocker.security.PinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val metadataRepository: MetadataRepository,
    @ApplicationContext private val context: Context,
    private val appLockManager: AppLockManager,
    private val userDatabaseInfoDao: UserDatabaseInfoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    companion object {
        private const val APP_SETTINGS_PREFS = "app_settings_prefs"
        private const val TAG = "SettingsViewModel" // Logging Tag
    }

    // ... (other methods remain the same) ...
    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val isPinActuallyConfigured = appLockManager.isPinConfigured()
            _uiState.update { it.copy(isAppLockEnabled = isPinActuallyConfigured) }

            val isDarkThemeEnabled = getPreference("dark_theme", false) as? Boolean ?: false
            if (isDarkThemeEnabled) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            val savedBiometricPref = getPreference("biometric_enabled", false) as? Boolean ?: false
            val actualBiometricEnabled = if (isPinActuallyConfigured) savedBiometricPref else false
            val autoLockTimeoutMinutes = getPreference("auto_lock_timeout", 5) as? Int ?: 5
            val defaultSortOrder = getPreference("default_sort_order", "Title (A-Z)") as? String ?: "Title (A-Z)"

            _uiState.update {
                it.copy(
                    isDarkTheme = isDarkThemeEnabled,
                    isBiometricEnabled = actualBiometricEnabled,
                    autoLockTimeoutMinutes = autoLockTimeoutMinutes,
                    defaultSortOrder = defaultSortOrder
                )
            }
        }
    }

    private val _themeChangedEvent = MutableSharedFlow<Unit>()
    val themeChangedEvent = _themeChangedEvent.asSharedFlow()

    fun toggleDarkTheme() {
        val newValue = !_uiState.value.isDarkTheme
        _uiState.update { it.copy(isDarkTheme = newValue) }
        savePreference("dark_theme", newValue)
        if (newValue) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        viewModelScope.launch { _themeChangedEvent.emit(Unit) }
    }

    fun toggleBiometric() {
        if (!_uiState.value.isAppLockEnabled) {
            _uiState.update { it.copy(error = "Enable app lock first before enabling biometrics.") }
            return
        }
        val newValue = !_uiState.value.isBiometricEnabled
        _uiState.update { it.copy(isBiometricEnabled = newValue) }
        savePreference("biometric_enabled", newValue)
    }

    fun updateAutoLockTimeout(minutes: Int) {
        _uiState.update { it.copy(autoLockTimeoutMinutes = minutes) }
        savePreference("auto_lock_timeout", minutes)
    }

    fun updateDefaultSortOrder(sortOrder: String) {
        _uiState.update { it.copy(defaultSortOrder = sortOrder) }
        savePreference("default_sort_order", sortOrder)
    }

    fun setupAppLock(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = appLockManager.setupPin(pin)) {
                is PinResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, isAppLockEnabled = true, showSetupSuccess = true)
                    }
                    onSuccess()
                }
                is PinResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun refreshAppLockStatus() {
        viewModelScope.launch {
            val isPinActuallyConfigured = appLockManager.isPinConfigured()
            val savedBiometricPref = getPreference("biometric_enabled", false) as? Boolean ?: false
            val actualBiometricEnabled = if (isPinActuallyConfigured) savedBiometricPref else false
            _uiState.update { currentState ->
                currentState.copy(isAppLockEnabled = isPinActuallyConfigured, isBiometricEnabled = actualBiometricEnabled)
            }
        }
    }

    fun removeAppLock(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = appLockManager.removePin(pin)) {
                is PinResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, isAppLockEnabled = false, isBiometricEnabled = false, showRemoveSuccess = true)
                    }
                    savePreference("biometric_enabled", false)
                    onSuccess()
                }
                is PinResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    fun verifyPinForConfirmationStep(pin: String, onResult: (success: Boolean, errorMessage: String?) -> Unit) {
        viewModelScope.launch {
            when (val result = appLockManager.verifyPin(pin)) {
                is PinResult.Success -> {
                    onResult(result.data, if (result.data) null else "Incorrect PIN")
                }
                is PinResult.Error -> {
                    onResult(false, result.message ?: "PIN verification failed")
                }
            }
        }
    }

    fun exportData(/* onSuccess: (String) -> Unit */) { /* ... implementation ... */ }
    fun importData(/* data: String, onSuccess: () -> Unit */) { /* ... implementation ... */ }


    fun clearAllData(pin: String) {
        Log.d(TAG, "clearAllData called. AppLockEnabled: ${_uiState.value.isAppLockEnabled}, PIN provided: ${pin.isNotEmpty()}")
        viewModelScope.launch {
            if (_uiState.value.isAppLockEnabled && pin.isNotEmpty()) {
                Log.d(TAG, "Verifying PIN for final confirmation.")
                when (val pinVerifyResult = appLockManager.verifyPin(pin)) {
                    is PinResult.Success -> {
                        if (!pinVerifyResult.data) {
                            Log.w(TAG, "PIN verification FAILED for final step.")
                            _uiState.update { it.copy(isLoading = false, error = "Incorrect PIN for final confirmation.") }
                            return@launch
                        }
                        Log.i(TAG, "PIN verification SUCCESSFUL for final step.")
                    }
                    is PinResult.Error -> {
                        Log.e(TAG, "PIN verification ERROR for final step: ${pinVerifyResult.message}")
                        _uiState.update { it.copy(isLoading = false, error = pinVerifyResult.message ?: "PIN verification failed for final step.") }
                        return@launch
                    }
                }
            } else if (_uiState.value.isAppLockEnabled && pin.isEmpty()) {
                Log.e(TAG, "App lock is enabled but clearAllData called with an empty PIN. This shouldn't happen after 3-step PIN confirm.")
                _uiState.update { it.copy(isLoading = false, error = "Cannot clear data: PIN required but not provided for final step.")}
                return@launch
            } else {
                Log.d(TAG, "No PIN verification needed or PIN already verified.")
            }

            _uiState.update { it.copy(isLoading = true, error = null) } // Clear previous errors, set loading
            Log.d(TAG, "Starting data deletion process...")

            try {
                val databaseInfos: List<UserDatabaseInfo> = userDatabaseInfoDao.getAllDatabasesSuspend()
                Log.d(TAG, "Found ${databaseInfos.size} user databases to clear.")

                var allOperationsSuccessful = true

                if (databaseInfos.isEmpty()) {
                    Log.i(TAG, "No user databases found in DAO. Nothing to clear from file system or DAO based on this list.")
                } else {
                    for (dbInfo in databaseInfos) {
                        val dbFilename = dbInfo.databaseFilename
                        Log.d(TAG, "Processing database: $dbFilename (ID: ${dbInfo.id})")

                        Log.d(TAG, "Attempting to delete database file: $dbFilename")
                        val fileDeleted = SQLCipherHelper.deleteDatabase(context, dbFilename)
                        if (fileDeleted) {
                            Log.i(TAG, "Successfully deleted database file: $dbFilename (or it didn't exist).")
                            try {
                                Log.d(TAG, "Attempting to delete DAO entry for: $dbFilename")
                                userDatabaseInfoDao.deleteDatabaseByFilename(dbFilename)
                                Log.i(TAG, "Successfully deleted DAO entry for: $dbFilename")
                            } catch (eDao: Exception) {
                                Log.e(TAG, "Failed to delete DAO entry for: $dbFilename", eDao)
                                allOperationsSuccessful = false
                            }
                        } else {
                            Log.w(TAG, "Failed to delete database file: $dbFilename (as reported by SQLCipherHelper).")
                            allOperationsSuccessful = false
                        }
                    }
                }

                if (allOperationsSuccessful) {
                    Log.i(TAG, "All data clearing operations successful.")
                    appLockManager.forceClearPinConfiguration()
                    Log.i(TAG, "App lock configuration cleared.")
                    savePreference("biometric_enabled", false)
                    _uiState.update {
                        it.copy(isLoading = false, showClearSuccess = true, isAppLockEnabled = false, isBiometricEnabled = false, error = null)
                    }
                } else {
                    Log.w(TAG, "Not all data clearing operations were successful. App lock will still be reset.")
                    appLockManager.forceClearPinConfiguration()
                    savePreference("biometric_enabled", false)
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to clear some or all data. App lock has been reset.", isAppLockEnabled = false, isBiometricEnabled = false)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception during clearAllData process: ${e.message}", e)
                appLockManager.forceClearPinConfiguration()
                savePreference("biometric_enabled", false)
                _uiState.update {
                    it.copy(isLoading = false, error = "An error occurred: ${e.message}. App lock has been reset.", isAppLockEnabled = false, isBiometricEnabled = false)
                }
            }
        }
    }
    private fun savePreference(key: String, value: Any) {
        val prefs = context.getSharedPreferences(APP_SETTINGS_PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("This type cannot be saved into SharedPreferences")
            }
        }
    }

    private fun getPreference(key: String, defaultValue: Any): Any {
        val prefs = context.getSharedPreferences(APP_SETTINGS_PREFS, Context.MODE_PRIVATE)
        return when (defaultValue) {
            is String -> prefs.getString(key, defaultValue) ?: defaultValue
            is Int -> prefs.getInt(key, defaultValue)
            is Boolean -> prefs.getBoolean(key, defaultValue)
            is Float -> prefs.getFloat(key, defaultValue)
            is Long -> prefs.getLong(key, defaultValue)
            else -> throw IllegalArgumentException("This type cannot be retrieved from SharedPreferences")
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessages() {
        _uiState.update {
            it.copy(
                showSetupSuccess = false,
                showChangeSuccess = false,
                showRemoveSuccess = false,
                showExportSuccess = false,
                showImportSuccess = false,
                showClearSuccess = false
            )
        }
    }

    fun getAppVersion(): String {
        return "1.0.0 (Placeholder)"
    }

    fun getAvailableSortOrders(): List<String> {
        return listOf(
            "Title (A-Z)", "Title (Z-A)",
            "Date Added (Newest First)", "Date Added (Oldest First)",
            "Last Modified (Newest First)", "Last Modified (Oldest First)"
        )
    }

    fun getAutoLockOptions(): List<Int> {
        return listOf(1, 2, 5, 10, 15, 30, 60)
    }
}
data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDarkTheme: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val autoLockTimeoutMinutes: Int = 5,
    val defaultSortOrder: String = "Title (A-Z)",
    val showSetupSuccess: Boolean = false,
    val showChangeSuccess: Boolean = false,
    val showRemoveSuccess: Boolean = false,
    val showExportSuccess: Boolean = false,
    val showImportSuccess: Boolean = false,
    val showClearSuccess: Boolean = false
)

