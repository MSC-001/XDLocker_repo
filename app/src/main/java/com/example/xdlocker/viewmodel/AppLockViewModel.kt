package com.example.xdlocker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdlocker.security.AppLockManager
import com.example.xdlocker.security.PinResult // Import PinResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI state for AppLockScreen
data class AppLockUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val appLockManager: AppLockManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState = _uiState.asStateFlow()

    fun verifyPin(pin: String, onUnlockSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = appLockManager.verifyPin(pin)) {
                is PinResult.Success -> {
                    if (result.data) {
                        onUnlockSuccess()
                        // Reset loading state on success, error message is already null
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid PIN") }
                    }
                }
                is PinResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
}
