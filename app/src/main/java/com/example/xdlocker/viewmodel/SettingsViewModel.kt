package com.example.xdlocker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdlocker.data.repository.MetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        // Load saved preferences from SharedPreferences or DataStore
        _uiState.update {
            it.copy(
                // These would be loaded from actual preferences
                isDarkTheme = false, // Load from preferences
                isAppLockEnabled = false, // Load from preferences
                isBiometricEnabled = false, // Load from preferences
                autoLockTimeoutMinutes = 5, // Load from preferences
                defaultSortOrder = "Title (A-Z)" // Load from preferences
            )
        }
    }

    fun toggleDarkTheme() {
        val newValue = !_uiState.value.isDarkTheme
        _uiState.update { it.copy(isDarkTheme = newValue) }
        // Save to preferences
        savePreference("dark_theme", newValue)
    }

    fun toggleAppLock() {
        val newValue = !_uiState.value.isAppLockEnabled
        _uiState.update { it.copy(isAppLockEnabled = newValue) }
        // Save to preferences
        savePreference("app_lock_enabled", newValue)

        // If disabling app lock, also disable biometric
        if (!newValue) {
            _uiState.update { it.copy(isBiometricEnabled = false) }
            savePreference("biometric_enabled", false)
        }
    }

    fun toggleBiometric() {
        if (!_uiState.value.isAppLockEnabled) {
            _uiState.update { it.copy(error = "Enable app lock first") }
            return
        }

        val newValue = !_uiState.value.isBiometricEnabled
        _uiState.update { it.copy(isBiometricEnabled = newValue) }
        // Save to preferences
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

            // In a real implementation, you'd hash and store the PIN securely
            try {
                // Save PIN securely (use Android Keystore or similar)
                savePinSecurely(pin)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAppLockEnabled = true,
                        showSetupSuccess = true
                    )
                }
                savePreference("app_lock_enabled", true)
                onSuccess()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to setup app lock: ${e.message}"
                    )
                }
            }
        }
    }

    fun changeAppLockPin(oldPin: String, newPin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Verify old PIN
                if (!verifyPinSecurely(oldPin)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Incorrect current PIN"
                        )
                    }
                    return@launch
                }

                // Save new PIN
                savePinSecurely(newPin)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showChangeSuccess = true
                    )
                }
                onSuccess()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to change PIN: ${e.message}"
                    )
                }
            }
        }
    }

    fun removeAppLock(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Verify PIN
                if (!verifyPinSecurely(pin)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Incorrect PIN"
                        )
                    }
                    return@launch
                }

                // Remove PIN and disable app lock
                removePinSecurely()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAppLockEnabled = false,
                        isBiometricEnabled = false,
                        showRemoveSuccess = true
                    )
                }
                savePreference("app_lock_enabled", false)
                savePreference("biometric_enabled", false)
                onSuccess()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to remove app lock: ${e.message}"
                    )
                }
            }
        }
    }

    fun exportData(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Get all databases metadata
                val statistics = metadataRepository.getDatabaseStatistics()
                // In a real implementation, you'd export actual data
                val exportData = "XDLocker Data Export - ${System.currentTimeMillis()}"

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showExportSuccess = true
                    )
                }
                onSuccess(exportData)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to export data: ${e.message}"
                    )
                }
            }
        }
    }

    fun importData(data: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Parse and import data
                // This is a placeholder - actual implementation would parse the data

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showImportSuccess = true
                    )
                }
                onSuccess()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to import data: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearAllData(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Verify PIN if app lock is enabled
                if (_uiState.value.isAppLockEnabled && !verifyPinSecurely(pin)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Incorrect PIN"
                        )
                    }
                    return@launch
                }

                // Clear all data - this would delete all databases
                // Implementation would go here

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showClearSuccess = true
                    )
                }
                onSuccess()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to clear data: ${e.message}"
                    )
                }
            }
        }
    }

    // Security utilities (placeholder implementations)
    private fun savePinSecurely(pin: String) {
        // Use Android Keystore to securely store the PIN
        // Implementation would use proper encryption
    }

    private fun verifyPinSecurely(pin: String): Boolean {
        // Verify PIN against stored hash
        // Implementation would use proper verification
        return true // Placeholder
    }

    private fun removePinSecurely() {
        // Remove stored PIN from secure storage
        // Implementation would clear the stored PIN
    }

    private fun savePreference(key: String, value: Any) {
        // Save to SharedPreferences or DataStore
        // Implementation would use actual preference storage
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
        return "1.0.0" // Get from BuildConfig
    }

    fun getAvailableSortOrders(): List<String> {
        return listOf(
            "Title (A-Z)",
            "Title (Z-A)",
            "Date Created (Newest)",
            "Date Created (Oldest)",
            "Date Updated (Newest)",
            "Date Updated (Oldest)",
            "Favorites First"
        )
    }

    fun getAutoLockOptions(): List<Int> {
        return listOf(1, 2, 5, 10, 15, 30) // Minutes
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