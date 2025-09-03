package com.example.xdlocker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdlocker.data.dao.UserDatabaseInfoDao // Added import
import com.example.xdlocker.data.database.SQLCipherHelper
import com.example.xdlocker.data.entities.UserDatabaseInfo // Added import
import com.example.xdlocker.security.AppLockManager
import com.example.xdlocker.security.PinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_ATTEMPTS = 3

// UI state for AppLockScreen
data class AppLockUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val remainingAttempts: Int = MAX_ATTEMPTS,
    val databaseCleared: Boolean = false,
    val appLockDisabledPostClear: Boolean = false // New state to indicate app lock is off
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    @ApplicationContext private val context: Context,
    private val userDatabaseInfoDao: UserDatabaseInfoDao // Added UserDatabaseInfoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState = _uiState.asStateFlow()

    fun verifyPin(pin: String, onUnlockSuccess: () -> Unit) {
        if (_uiState.value.databaseCleared) {
            _uiState.update { it.copy(errorMessage = "All databases have been cleared. App access restricted. Please restart the app again :/") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // Assuming verifyPin is global for the app lock.
            // If you had per-database PINs, this logic would need the active database name.
            when (val result = appLockManager.verifyPin(pin)) {
                is PinResult.Success -> {
                    if (result.data) { // Pin is correct
                        onUnlockSuccess()
                        _uiState.update { it.copy(isLoading = false, remainingAttempts = MAX_ATTEMPTS) }
                    } else { // Pin is incorrect
                        handleIncorrectPin()
                    }
                }
                is PinResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message ?: "An unknown error occurred during PIN verification.") }
                }
            }
        }
    }

    private fun handleIncorrectPin() {
        val currentAttempts = _uiState.value.remainingAttempts - 1
        if (currentAttempts > 0) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Incorrect PIN. $currentAttempts attempts remaining, DATABASE WILL BE CLEARED \uD83D\uDCA3 .",
                    remainingAttempts = currentAttempts
                )
            }
        } else {
            // Max attempts reached, clear all databases
            clearAllDatabases() // This will also update the UI state
        }
    }

    private fun clearAllDatabases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Indicate loading for the clear operation
            var allOperationsSuccessful = true
            var finalErrorMessage = "Too many incorrect attempts. ALL user databases have been cleared and app lock has been reset \uD83D\uDCA5 ."

            try {
                val databaseInfos: List<UserDatabaseInfo> = userDatabaseInfoDao.getAllDatabasesSuspend()

                if (databaseInfos.isEmpty()) {
                    // android.util.Log.i("AppLockViewModel", "No user databases found to clear.")
                } else {
                    // android.util.Log.i("AppLockViewModel", "Attempting to clear data for ${databaseInfos.size} database(s).")
                    for (dbInfo in databaseInfos) {
                        val dbFilename = dbInfo.databaseFilename
                        val fileDeleted = SQLCipherHelper.deleteDatabase(context, dbFilename)
                        if (fileDeleted) {
                            // android.util.Log.i("AppLockViewModel", "DATABASE FILE '$dbFilename' DELETED successfully!")
                            try {
                                userDatabaseInfoDao.deleteDatabaseByFilename(dbFilename)
                                // android.util.Log.i("AppLockViewModel", "DAO entry for '$dbFilename' DELETED successfully!")
                            } catch (_: Exception) { // Catching specific DAO exception is better
                                allOperationsSuccessful = false
                                // android.util.Log.e("AppLockViewModel", "Failed to delete DAO entry for '$dbFilename'.")
                            }
                        } else {
                            allOperationsSuccessful = false
                            // android.util.Log.w("AppLockViewModel", "Failed to delete database file '$dbFilename'.")
                        }
                    }
                }

                if (!allOperationsSuccessful) {
                    finalErrorMessage = "Too many incorrect attempts. Some data may not have been cleared. App lock has been reset \uD83D\uDCA5 ."
                }

                // Regardless of partial success in DB deletion, always try to reset app lock
                appLockManager.forceClearPinConfiguration()
                // android.util.Log.i("AppLockViewModel", "App lock configuration has been forcibly cleared.")

            } catch (e: Exception) {
                // android.util.Log.e("AppLockViewModel", "Exception while clearing all databases: ${e.message}", e)
                allOperationsSuccessful = false // Mark as unsuccessful due to overall error
                finalErrorMessage = "An error occurred during data wipe. App lock has been reset. ${e.message}"
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = finalErrorMessage,
                    remainingAttempts = 0,
                    databaseCleared = true, // Indicates data wipe occurred
                    appLockDisabledPostClear = true // Indicates app lock is now off
                )
            }
        }
    }
}

