package com.example.xdlocker.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdlocker.data.entities.PasswordEntry
import com.example.xdlocker.data.repository.PasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class AddEditPasswordViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditPasswordUiState())
    val uiState = _uiState.asStateFlow()

    // Navigation parameters
    private val databaseFilename: String = savedStateHandle.get<String>("databaseFilename") ?: ""
    private val entryId: Int = savedStateHandle.get<Int>("entryId") ?: -1
    private val isEditing = entryId != -1

    init {
        _uiState.update { it.copy(isEditing = isEditing) }
        if (isEditing) {
            loadEntry()
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val entry = passwordRepository.getEntryById(databaseFilename, entryId)
            if (entry != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        title = entry.title,
                        username = entry.username,
                        url = entry.url,
                        password = entry.password,
                        tag = entry.tag,
                        notes = entry.notes,
                        isFavorite = entry.isFavorite,
                        passwordStrength = calculatePasswordStrength(entry.password)
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = "Entry not found")
                }
            }
        }
    }

    // Input handlers
    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title, titleError = null) }
    }

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(url = url) }
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

    fun onTagChanged(tag: String) {
        _uiState.update { it.copy(tag = tag) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    // Password generation
    fun generatePassword() {
        val generatedPassword = generateSecurePassword()
        _uiState.update {
            it.copy(
                password = generatedPassword,
                passwordStrength = calculatePasswordStrength(generatedPassword),
                showPasswordGenerated = true
            )
        }
    }

    fun generateCustomPassword(
        length: Int = 12,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true,
        excludeSimilar: Boolean = true
    ) {
        val password = generatePasswordWithOptions(
            length, includeUppercase, includeLowercase,
            includeNumbers, includeSymbols, excludeSimilar
        )
        _uiState.update {
            it.copy(
                password = password,
                passwordStrength = calculatePasswordStrength(password),
                showPasswordGenerated = true
            )
        }
    }

    // Save functionality
    fun saveEntry(onSuccess: () -> Unit) {
        if (!validateInputs()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val state = _uiState.value
            val entry = createPasswordEntry(state)

            val result = if (isEditing) {
                passwordRepository.updateEntry(databaseFilename, entry)
            } else {
                passwordRepository.insertEntry(databaseFilename, entry).map { }
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isSaving = false, error = error.message ?: "Failed to save entry")
                    }
                }
            )
        }
    }

    private fun createPasswordEntry(state: AddEditPasswordUiState): PasswordEntry {
        return if (isEditing) {
            PasswordEntry(
                id = entryId,
                title = state.title.trim(),
                username = state.username.trim(),
                url = state.url.trim(),
                password = state.password,
                tag = state.tag.trim(),
                notes = state.notes.trim(),
                isFavorite = state.isFavorite,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            PasswordEntry(
                title = state.title.trim(),
                username = state.username.trim(),
                url = state.url.trim(),
                password = state.password,
                tag = state.tag.trim(),
                notes = state.notes.trim(),
                isFavorite = state.isFavorite
            )
        }
    }

    private fun validateInputs(): Boolean {
        val state = _uiState.value
        var hasErrors = false

        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Title is required") }
            hasErrors = true
        }

        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            hasErrors = true
        }

        return !hasErrors
    }

    // Password generation utilities
    private fun generateSecurePassword(length: Int = 16): String {
        return generatePasswordWithOptions(
            length = length,
            includeUppercase = true,
            includeLowercase = true,
            includeNumbers = true,
            includeSymbols = true,
            excludeSimilar = true
        )
    }

    private fun generatePasswordWithOptions(
        length: Int,
        includeUppercase: Boolean,
        includeLowercase: Boolean,
        includeNumbers: Boolean,
        includeSymbols: Boolean,
        excludeSimilar: Boolean
    ): String {
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercase = "abcdefghijklmnopqrstuvwxyz"
        val numbers = "0123456789"
        val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        val similar = "il1Lo0O"

        var chars = ""
        if (includeUppercase) chars += uppercase
        if (includeLowercase) chars += lowercase
        if (includeNumbers) chars += numbers
        if (includeSymbols) chars += symbols

        if (excludeSimilar) {
            chars = chars.filterNot { similar.contains(it) }
        }

        if (chars.isEmpty()) return ""

        return (1..length)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    private fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength(0, "No Password")

        var score = 0
        val feedback = mutableListOf<String>()

        // Length scoring
        when {
            password.length >= 16 -> { score += 25; feedback.add("Excellent length") }
            password.length >= 12 -> { score += 20; feedback.add("Good length") }
            password.length >= 8 -> { score += 15; feedback.add("Adequate length") }
            password.length >= 6 -> { score += 10; feedback.add("Short") }
            else -> feedback.add("Too short")
        }

        // Character variety
        if (password.any { it.isLowerCase() }) { score += 5; feedback.add("Lowercase") }
        if (password.any { it.isUpperCase() }) { score += 5; feedback.add("Uppercase") }
        if (password.any { it.isDigit() }) { score += 5; feedback.add("Numbers") }
        if (password.any { !it.isLetterOrDigit() }) { score += 10; feedback.add("Symbols") }

        // Complexity bonus
        val charTypes = listOf(
            password.any { it.isLowerCase() },
            password.any { it.isUpperCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() }
        ).count { it }
        score += charTypes * 10

        // Penalties
        if (password.contains("123") || password.contains("abc")) {
            score -= 10; feedback.add("Avoid sequences")
        }

        val strength = when {
            score >= 80 -> "Very Strong"
            score >= 60 -> "Strong"
            score >= 40 -> "Medium"
            score >= 20 -> "Weak"
            else -> "Very Weak"
        }

        return PasswordStrength(minOf(score, 100), strength)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(showPasswordGenerated = false) }
    }
}

data class AddEditPasswordUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val title: String = "",
    val username: String = "",
    val url: String = "",
    val password: String = "",
    val tag: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val titleError: String? = null,
    val passwordError: String? = null,
    val error: String? = null,
    val passwordStrength: PasswordStrength = PasswordStrength(0, "No Password"),
    val showPasswordGenerated: Boolean = false
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

    val progress: Float
        get() = score / 100f
}