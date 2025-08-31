package com.example.xdlocker.viewmodel

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val metadataRepository: MetadataRepository, // Keep for export/import/clearAllData
    @ApplicationContext private val context: Context, // Keep for SharedPreferences
    private val appLockManager: AppLockManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    companion object {
        private const val APP_SETTINGS_PREFS = "app_settings_prefs"
    }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // App Lock Status from AppLockManager
            val isPinActuallyConfigured = appLockManager.isPinConfigured()
            _uiState.update { it.copy(isAppLockEnabled = isPinActuallyConfigured) }

            // --- Dark Theme ---
            val isDarkThemeEnabled = getPreference("dark_theme", false) as? Boolean ?: false
            if (isDarkThemeEnabled) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            // --- Other Settings ---
            val savedBiometricPref = getPreference("biometric_enabled", false) as? Boolean ?: false
            // Ensure biometric is off if app lock is off, even if preference says otherwise
            val actualBiometricEnabled = if (isPinActuallyConfigured) savedBiometricPref else false

            val autoLockTimeoutMinutes = getPreference("auto_lock_timeout", 5) as? Int ?: 5
            val defaultSortOrder = getPreference("default_sort_order", "Title (A-Z)") as? String ?: "Title (A-Z)"

            _uiState.update {
                it.copy(
                    isDarkTheme = isDarkThemeEnabled,
                    isBiometricEnabled = actualBiometricEnabled,
                    autoLockTimeoutMinutes = autoLockTimeoutMinutes,
                    defaultSortOrder = defaultSortOrder
                    // isAppLockEnabled is already set from appLockManager
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

    fun toggleAppLock() {
        // This function might need re-evaluation. Do we allow toggling the preference
        // if the PIN setup/removal should be done via setupAppLock/removeAppLock?
        // For now, let's assume it reflects the preference that UI might use to show/hide PIN setup options.
        val currentAppLockEnabledState = _uiState.value.isAppLockEnabled
        if (currentAppLockEnabledState) {
            // If currently enabled, this toggle implies user wants to navigate to remove it.
            // The actual removal will be handled by removeAppLock() after PIN verification.
            // We don't change the underlying PIN state here.
            // We just reflect an *intent* or a UI preference.
            // This part is tricky as isAppLockEnabled is now primarily driven by appLockManager.isPinConfigured()
            _uiState.update { it.copy(error = "Please use 'Remove App Lock' with your PIN to disable.") }
        } else {
            // If currently disabled, this toggle implies user wants to navigate to set it up.
            // The actual setup will be handled by setupAppLock().
            // We don't change the underlying PIN state here.
            _uiState.update { it.copy(error = "Please use 'Setup App Lock' to enable.") }
        }
        // savePreference("app_lock_enabled", newValue) // Preference for app_lock_enabled is less reliable now
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
                        it.copy(
                            isLoading = false,
                            isAppLockEnabled = true,
                            showSetupSuccess = true
                        )
                    }
                    savePreference("app_lock_enabled", true) // Keep for quick UI hints if needed
                    onSuccess()
                }
                is PinResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun changeAppLockPin(oldPin: String, newPin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = appLockManager.changePin(oldPin, newPin)) {
                is PinResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showChangeSuccess = true
                        )
                    }
                    onSuccess()
                }
                is PinResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun removeAppLock(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = appLockManager.removePin(pin)) {
                is PinResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAppLockEnabled = false,
                            isBiometricEnabled = false, // Also disable biometric when PIN is removed
                            showRemoveSuccess = true
                        )
                    }
                    savePreference("app_lock_enabled", false)
                    savePreference("biometric_enabled", false)
                    onSuccess()
                }
                is PinResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun exportData(onSuccess: (String) -> Unit) { /* ... no change ... */ }
    fun importData(data: String, onSuccess: () -> Unit) { /* ... no change ... */ }

    fun clearAllData(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (_uiState.value.isAppLockEnabled) {
                when (val pinVerifyResult = appLockManager.verifyPin(pin)) {
                    is PinResult.Success -> {
                        if (!pinVerifyResult.data) {
                            _uiState.update { it.copy(isLoading = false, error = "Incorrect PIN") }
                            return@launch
                        }
                        // PIN verified, proceed to clear data
                    }
                    is PinResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = pinVerifyResult.message) }
                        return@launch
                    }
                }
            } // else: App lock not enabled, no PIN verification needed

            // Clear all data - this would delete all databases
            // TODO: metadataRepository.clearAllDatabases() - this method needs to exist
            // For now, simulating success
            _uiState.update {
                it.copy(
                    isLoading = false,
                    showClearSuccess = true
                )
            }
            onSuccess()
            // Consider also removing app lock PIN if all data is cleared
            // appLockManager.removePin() - but this requires current PIN, which might be an issue if already verified
            // Or have a specific method in AppLockManager like forceRemovePin()
        }
    }

    private fun savePreference(key: String, value: Any) {
        val prefs = context.getSharedPreferences(APP_SETTINGS_PREFS, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("This type cannot be saved into SharedPreferences")
            }
            apply() // Or commit() if you need synchronous saving
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
        // TODO: Replace with actual app version retrieval, e.g., using BuildConfig.VERSION_NAME
        // For now, returning a placeholder.
        // try {
        //     val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        //     return packageInfo.versionName
        // } catch (e: PackageManager.NameNotFoundException) {
        //     Log.e("SettingsViewModel", "Failed to get app version", e)
        // }
        return "1.0.0 (Placeholder)"
    }

    fun getAvailableSortOrders(): List<String> {
        return listOf(
            "Title (A-Z)",
            "Title (Z-A)",
            "Date Added (Newest First)",
            "Date Added (Oldest First)",
            "Last Modified (Newest First)",
            "Last Modified (Oldest First)"
            // Add other sort orders as needed
        )
    }

    fun getAutoLockOptions(): List<Int> {
        // Options in minutes. 0 could represent 'Immediately' or 'When screen is locked'
        // A separate option for 'Never' could be handled if desired, though risky.
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
