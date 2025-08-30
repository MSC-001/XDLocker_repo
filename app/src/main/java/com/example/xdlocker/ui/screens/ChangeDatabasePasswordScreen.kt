package com.example.xdlocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xdlocker.ui.components.PasswordStrengthIndicator
import com.example.xdlocker.ui.components.PasswordTextField
import com.example.xdlocker.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeDatabasePasswordScreen(
    databaseId: Int,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var oldPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val oldPasswordFocusRequester = remember { FocusRequester() }
    val newPasswordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    // Password strength calculation
    val passwordStrength = remember(newPassword) {
        calculatePasswordStrength(newPassword)
    }

    fun validateInputs(): Boolean {
        oldPasswordError = null
        newPasswordError = null
        confirmPasswordError = null

        var isValid = true

        if (oldPassword.isBlank()) {
            oldPasswordError = "Current password is required"
            isValid = false
        }

        if (newPassword.isBlank()) {
            newPasswordError = "New password is required"
            isValid = false
        } else if (newPassword.length < 6) {
            newPasswordError = "Password must be at least 6 characters"
            isValid = false
        } else if (passwordStrength.score < 30) {
            newPasswordError = "Password is too weak"
            isValid = false
        }

        if (newPassword != confirmPassword) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        }

        if (oldPassword == newPassword) {
            newPasswordError = "New password must be different from current password"
            isValid = false
        }

        return isValid
    }

    fun changePassword() {
        if (!validateInputs()) return

        // TODO: Implement password change logic
        // viewModel.changeDatabasePassword(databaseId, oldPassword, newPassword) {
        //     onNavigateBack()
        // }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Warning card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Important",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Changing the database password will re-encrypt all stored data. Make sure to remember your new password as it cannot be recovered.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current password
            PasswordTextField(
                value = oldPassword,
                onValueChange = {
                    oldPassword = it
                    oldPasswordError = null
                },
                label = "Current Password",
                isError = oldPasswordError != null,
                errorMessage = oldPasswordError,
                imeAction = ImeAction.Next,
                onImeAction = { newPasswordFocusRequester.requestFocus() },
                modifier = Modifier.focusRequester(oldPasswordFocusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // New password
            PasswordTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    newPasswordError = null
                },
                label = "New Password",
                isError = newPasswordError != null,
                errorMessage = newPasswordError,
                imeAction = ImeAction.Next,
                onImeAction = { confirmPasswordFocusRequester.requestFocus() },
                showGenerateButton = true,
                onGeneratePassword = {
                    // Generate secure password
                    newPassword = generateSecurePassword()
                },
                modifier = Modifier.focusRequester(newPasswordFocusRequester)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password strength
            if (newPassword.isNotBlank()) {
                PasswordStrengthIndicator(
                    strength = passwordStrength.score,
                    label = passwordStrength.label
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm new password
            PasswordTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = null
                },
                label = "Confirm New Password",
                isError = confirmPasswordError != null,
                errorMessage = confirmPasswordError,
                imeAction = ImeAction.Done,
                onImeAction = { changePassword() },
                showPasswordToggle = false,
                modifier = Modifier.focusRequester(confirmPasswordFocusRequester)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Change password button
            Button(
                onClick = { changePassword() },
                enabled = oldPassword.isNotBlank() &&
                        newPassword.isNotBlank() &&
                        confirmPassword.isNotBlank() &&
                        !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Changing Password...")
                } else {
                    Icon(Icons.Default.VpnKey, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Password")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "This process may take a few moments as all data needs to be re-encrypted with the new password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Error display
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

// Helper function for password strength calculation
private fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength(0, "No Password")

    var score = 0

    // Length scoring
    when {
        password.length >= 16 -> score += 25
        password.length >= 12 -> score += 20
        password.length >= 8 -> score += 15
        password.length >= 6 -> score += 10
    }

    // Character variety
    if (password.any { it.isLowerCase() }) score += 5
    if (password.any { it.isUpperCase() }) score += 5
    if (password.any { it.isDigit() }) score += 5
    if (password.any { !it.isLetterOrDigit() }) score += 10

    // Complexity bonus
    val charTypes = listOf(
        password.any { it.isLowerCase() },
        password.any { it.isUpperCase() },
        password.any { it.isDigit() },
        password.any { !it.isLetterOrDigit() }
    ).count { it }
    score += charTypes * 10

    val strength = when {
        score >= 80 -> "Very Strong"
        score >= 60 -> "Strong"
        score >= 40 -> "Medium"
        score >= 20 -> "Weak"
        else -> "Very Weak"
    }

    return PasswordStrength(minOf(score, 100), strength)
}

private fun generateSecurePassword(length: Int = 16): String {
    val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lowercase = "abcdefghijklmnopqrstuvwxyz"
    val numbers = "0123456789"
    val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    val allChars = uppercase + lowercase + numbers + symbols
    return (1..length).map { allChars.random() }.joinToString("")
}

data class PasswordStrength(
    val score: Int,
    val label: String
)