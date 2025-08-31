package com.example.xdlocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xdlocker.ui.components.*
import com.example.xdlocker.utils.PasswordUtils
import com.example.xdlocker.viewmodel.AddEditPasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditPasswordViewModel = hiltViewModel()
) {
    // Define a local data class to hold password strength results
    data class PasswordStrengthResult(val score: Int, val label: String)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Convert passwordStrength to local data class
    val passwordStrengthData = remember(uiState.passwordStrength) {
        PasswordStrengthResult(uiState.passwordStrength.score, uiState.passwordStrength.label)
    }

    val titleFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val urlFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val tagFocusRequester = remember { FocusRequester() }

    var showPasswordGenerator by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        if (!uiState.isEditing) {
            titleFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditing) "Edit Password" else "Add Password")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Favorite toggle
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Toggle favorite",
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Save button
                    IconButton(
                        onClick = { viewModel.saveEntry(onNavigateBack) },
                        enabled = canSaveEntry(uiState) && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, "Save")
                        }
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
            // Loading state
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // Error display
            uiState.error?.let { error ->
                ErrorCard(
                    message = error,
                    onDismiss = viewModel::clearError
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Title field
            CustomTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                label = "Title *",
                leadingIcon = Icons.Default.Title,
                isError = uiState.titleError != null,
                errorMessage = uiState.titleError,
                imeAction = ImeAction.Next,
                onImeAction = { usernameFocusRequester.requestFocus() },
                modifier = Modifier.focusRequester(titleFocusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Username field
            CustomTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChanged,
                label = "Username",
                leadingIcon = Icons.Default.Person,
                imeAction = ImeAction.Next,
                onImeAction = { urlFocusRequester.requestFocus() },
                modifier = Modifier.focusRequester(usernameFocusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // URL field
            CustomTextField(
                value = uiState.url,
                onValueChange = viewModel::onUrlChanged,
                label = "Website URL",
                leadingIcon = Icons.Default.Language,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
                onImeAction = { passwordFocusRequester.requestFocus() },
                modifier = Modifier.focusRequester(urlFocusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field with generator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Password *",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                TextButton(onClick = { showPasswordGenerator = true }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate")
                }
            }

            PasswordTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChanged,
                label = "Password",
                isError = uiState.passwordError != null,
                errorMessage = uiState.passwordError,
                imeAction = ImeAction.Next,
                onImeAction = { tagFocusRequester.requestFocus() },
                modifier = Modifier.focusRequester(passwordFocusRequester)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password strength indicator
            if (uiState.password.isNotBlank()) {
                PasswordStrengthIndicator(
                    strength = passwordStrengthData.score,
                    label = passwordStrengthData.label
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tag field
            CustomTextField(
                value = uiState.tag,
                onValueChange = viewModel::onTagChanged,
                label = "Tag",
                leadingIcon = Icons.Default.Tag,
                imeAction = ImeAction.Next,
                modifier = Modifier.focusRequester(tagFocusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notes field
            CustomTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = "Notes",
                leadingIcon = Icons.AutoMirrored.Filled.Notes,
                maxLines = 4,
                singleLine = false,
                imeAction = ImeAction.Done
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = { viewModel.saveEntry(onNavigateBack) },
                enabled = canSaveEntry(uiState) && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isEditing) "Updating..." else "Saving...")
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isEditing) "Update Password" else "Save Password")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Password generator dialog
    if (showPasswordGenerator) {
        PasswordGeneratorDialog(
            onPasswordGenerated = { password ->
                viewModel.onPasswordChanged(password)
                showPasswordGenerator = false
            },
            onDismiss = { showPasswordGenerator = false }
        )
    }
}

@Composable
private fun PasswordGeneratorDialog(
    onPasswordGenerated: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var length by remember { mutableIntStateOf(12) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var excludeSimilar by remember { mutableStateOf(true) }
    var generatedPassword by remember { mutableStateOf("") }

    fun generatePassword() {
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

        if (chars.isEmpty()) return

        generatedPassword = (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    LaunchedEffect(length, includeUppercase, includeLowercase, includeNumbers, includeSymbols, excludeSimilar) {
        generatePassword()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password Generator") },
        text = {
            Column {
                // Length slider
                Text("Length: $length")
                Slider(
                    value = length.toFloat(),
                    onValueChange = { length = it.toInt() },
                    valueRange = 4f..32f,
                    steps = 27
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = includeUppercase,
                                onCheckedChange = { includeUppercase = it }
                            )
                            Text("Uppercase")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = includeLowercase,
                                onCheckedChange = { includeLowercase = it }
                            )
                            Text("Lowercase")
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = includeNumbers,
                                onCheckedChange = { includeNumbers = it }
                            )
                            Text("Numbers")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = includeSymbols,
                                onCheckedChange = { includeSymbols = it }
                            )
                            Text("Symbols")
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = excludeSimilar,
                        onCheckedChange = { excludeSimilar = it }
                    )
                    Text("Exclude similar characters (i, l, 1, L, o, 0, O)")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Generated password preview
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = generatedPassword,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { generatePassword() }) {
                            Icon(Icons.Default.Refresh, "Regenerate")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPasswordGenerated(generatedPassword) },
                enabled = generatedPassword.isNotBlank()
            ) {
                Text("Use Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null
            )
            Text(
                text = message,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss error"
                )
            }
        }
    }
}

// Existing canSaveEntry function will follow here...

private fun canSaveEntry(uiState: com.example.xdlocker.viewmodel.AddEditPasswordUiState): Boolean {
    return uiState.title.isNotBlank() &&
            uiState.password.isNotBlank() &&
            uiState.titleError == null &&
            uiState.passwordError == null
}