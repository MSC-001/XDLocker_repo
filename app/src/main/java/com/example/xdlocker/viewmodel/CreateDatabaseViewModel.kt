package com.example.xdlocker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdlocker.data.entities.UserDatabaseInfo
import com.example.xdlocker.data.repository.MetadataRepository
import com.example.xdlocker.utils.PasswordUtils
import com.example.xdlocker.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateDatabaseViewModel @Inject constructor(
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateDatabaseUiState())
    val uiState = _uiState.asStateFlow()

    fun onLabelChanged(label: String) {
        _uiState.update {
            it.copy(
                label = label,
                labelError = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                passwordStrength = PasswordUtils.calculatePasswordStrength(password)
            )
        }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.update {
            it.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = null
            )
        }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onColorThemeChanged(colorTheme: String) {
        _uiState.update { it.copy(colorTheme = colorTheme) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun createDatabase(onSuccess: (UserDatabaseInfo) -> Unit) {
        val state = _uiState.value

        viewModelScope.launch {
            // Validate inputs in coroutine scope
            if (!validateInputs()) {
                return@launch
            }
            
            _uiState.update { it.copy(isCreating = true, error = null) }

            val result = metadataRepository.createDatabase(
                label = state.label.trim(),
                password = state.password,
                description = state.description.trim(),
                colorTheme = state.colorTheme
            )

            result.fold(
                onSuccess = { databaseInfo ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            isCreated = true
                        )
                    }
                    onSuccess(databaseInfo)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            error = error.message ?: "Failed to create database"
                        )
                    }
                }
            )
        }
    }

    private suspend fun validateInputs(): Boolean {
        val state = _uiState.value
        var hasErrors = false

        // Validate label
        when {
            state.label.isBlank() -> {
                _uiState.update { it.copy(labelError = "Database name is required") }
                hasErrors = true
            }
            state.label.length < 3 -> {
                _uiState.update { it.copy(labelError = "Database name must be at least 3 characters") }
                hasErrors = true
            }
            metadataRepository.isLabelExists(state.label.trim()) -> {
                _uiState.update { it.copy(labelError = "Database name already exists") }
                hasErrors = true
            }
        }

        // Validate password
        when {
            state.password.isBlank() -> {
                _uiState.update { it.copy(passwordError = "Password is required") }
                hasErrors = true
            }
            state.password.length < 6 -> {
                _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
                hasErrors = true
            }
            state.passwordStrength.score < 30 -> {
                _uiState.update { it.copy(passwordError = "Password is too weak. Please use a stronger password.") }
                hasErrors = true
            }
        }

        // Validate confirm password
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            hasErrors = true
        }

        return !hasErrors
    }

    // Password strength calculation moved to PasswordUtils class

    fun generatePassword() {
        val currentState = _uiState.value // Get current UI state for generation options

        // Determine the character pool using Constants.PasswordChars
        var charPool = ""
        if (currentState.passwordGenIncludeUppercase) { // Assumed UiState field name
            charPool += Constants.PasswordChars.UPPERCASE
        }
        if (currentState.passwordGenIncludeLowercase) { // Assumed UiState field name
            charPool += Constants.PasswordChars.LOWERCASE
        }
        if (currentState.passwordGenIncludeNumbers) { // Assumed UiState field name
            charPool += Constants.PasswordChars.NUMBERS
        }
        if (currentState.passwordGenIncludeSymbols) { // Assumed UiState field name
            charPool += Constants.PasswordChars.SYMBOLS
        }

        // Exclude similar characters if requested and the pool is not empty
        if (currentState.passwordGenExcludeSimilar && charPool.isNotEmpty()) { // Assumed UiState field name
            charPool = charPool.filterNot { Constants.PasswordChars.SIMILAR_CHARS.contains(it) }
        }

        // Handle case where no character sets result in an empty pool
        if (charPool.isEmpty()) {
            _uiState.update {
                it.copy(
                    password = "", // Clear password
                    // Optionally, set an error message in UiState to inform the user
                    // passwordError = "Please select character types for password generation."
                )
            }
            // It's important to also trigger onPasswordChanged if you want UI to update based on passwordError
            onPasswordChanged("") // Pass empty string to trigger validation/strength update for the empty state
            return
        }

        // Determine the length to use from UiState, falling back to default and respecting min/max from Constants
        // Assuming passwordGenLength in UiState could be null or not yet set by the dialog
        val desiredLength = currentState.passwordGenLength ?: Constants.DEFAULT_PASSWORD_LENGTH // Assumed UiState & Constant names

        val lengthToUse = desiredLength.coerceIn(
            Constants.MIN_PASSWORD_GEN_LENGTH, // Assumed Constant name
            Constants.MAX_PASSWORD_GEN_LENGTH  // Assumed Constant name
        )

        // Generate the password
        val newPassword = (1..lengthToUse)
            .map { charPool.random() }
            .joinToString("")

        // Update the UI state with the new password
        _uiState.update {
            it.copy(
                password = newPassword,
                confirmPassword = "", // Clear confirm password field when a new password is generated
                passwordError = null, // Clear any previous password errors
                confirmPasswordError = null // Clear confirm password errors
                // Note: passwordStrength will be updated by the onPasswordChanged call below
            )
        }
        // Call existing onPasswordChanged to trigger validation and password strength calculation
        onPasswordChanged(newPassword)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetForm() {
        _uiState.value = CreateDatabaseUiState()
    }
}

data class CreateDatabaseUiState(
    val label: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val description: String = "",
    val colorTheme: String = UserDatabaseInfo.availableColorThemes.first(),
    val isPasswordVisible: Boolean = false,
    val isCreating: Boolean = false,
    val isCreated: Boolean = false,
    val labelError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val error: String? = null,
    val passwordStrength: PasswordUtils.PasswordStrength = PasswordUtils.PasswordStrength(0, ""),
    // Added password generation fields
    val passwordGenLength: Int = Constants.DEFAULT_PASSWORD_LENGTH,
    val passwordGenIncludeUppercase: Boolean = true,
    val passwordGenIncludeLowercase: Boolean = true,
    val passwordGenIncludeNumbers: Boolean = true,
    val passwordGenIncludeSymbols: Boolean = true,
    val passwordGenExcludeSimilar: Boolean = true
)