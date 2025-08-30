package com.example.xdlocker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdlocker.data.entities.UserDatabaseInfo
import com.example.xdlocker.data.repository.MetadataRepository
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
                passwordStrength = calculatePasswordStrength(password)
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

        // Validate inputs
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
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

    private fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) {
            return PasswordStrength(0, "")
        }

        var score = 0
        var feedback = mutableListOf<String>()

        // Length scoring
        when {
            password.length >= 12 -> {
                score += 25
                feedback.add("Good length")
            }
            password.length >= 8 -> {
                score += 15
                feedback.add("Adequate length")
            }
            password.length >= 6 -> {
                score += 10
                feedback.add("Short password")
            }
            else -> {
                feedback.add("Too short")
            }
        }

        // Character variety
        if (password.any { it.isLowerCase() }) {
            score += 5
            feedback.add("Has lowercase")
        }
        if (password.any { it.isUpperCase() }) {
            score += 5
            feedback.add("Has uppercase")
        }
        if (password.any { it.isDigit() }) {
            score += 5
            feedback.add("Has numbers")
        }
        if (password.any { !it.isLetterOrDigit() }) {
            score += 10
            feedback.add("Has symbols")
        }

        // Complexity bonus
        val charTypes = listOf(
            password.any { it.isLowerCase() },
            password.any { it.isUpperCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() }
        ).count { it }

        score += charTypes * 10

        // Penalty for common patterns
        if (password.contains("123") || password.contains("abc")) {
            score -= 10
            feedback.add("Avoid common patterns")
        }

        val strength = when {
            score >= 80 -> "Very Strong"
            score >= 60 -> "Strong"
            score >= 40 -> "Medium"
            score >= 20 -> "Weak"
            else -> "Very Weak"
        }

        return PasswordStrength(
            score = minOf(score, 100),
            label = strength
        )
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
    val passwordStrength: PasswordStrength = PasswordStrength(0, "")
)

data class PasswordStrength(
    val score: Int,
    val label: String
) {
    val color: String
        get() = when {
            score >= 80 -> "green"
            score >= 60 -> "blue"
            score >= 40 -> "orange"
            score >= 20 -> "red"
            else -> "gray"
        }
}