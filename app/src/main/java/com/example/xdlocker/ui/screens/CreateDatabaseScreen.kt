package com.example.xdlocker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xdlocker.data.entities.UserDatabaseInfo
import com.example.xdlocker.ui.components.*
import com.example.xdlocker.ui.theme.DatabaseColors
import com.example.xdlocker.viewmodel.CreateDatabaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDatabaseScreen(
    onNavigateBack: () -> Unit,
    onDatabaseCreated: (UserDatabaseInfo) -> Unit,
    viewModel: CreateDatabaseViewModel = hiltViewModel()
) {
    // Define a local data class to hold password strength results
    data class PasswordStrengthResult(val score: Int, val label: String)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    // Convert passwordStrength to local data class
    val passwordStrengthData = remember(uiState.passwordStrength) {
        PasswordStrengthResult(uiState.passwordStrength.score, uiState.passwordStrength.label)
    }

    val labelFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.isCreated) {
        if (uiState.isCreated) {
            // Navigate back or to the new database
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Database") },
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
            // Header
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "New Encrypted Database",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Create a secure vault for your passwords",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Database Name
            CustomTextField(
                value = uiState.label,
                onValueChange = viewModel::onLabelChanged,
                label = "Database Name",
                leadingIcon = Icons.Default.Storage,
                isError = uiState.labelError != null,
                errorMessage = uiState.labelError,
                imeAction = ImeAction.Next,
                onImeAction = { passwordFocusRequester.requestFocus() },
                modifier = Modifier.focusRequester(labelFocusRequester)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            CustomTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = "Description (Optional)",
                leadingIcon = Icons.Default.Description,
                maxLines = 3,
                singleLine = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            PasswordTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChanged,
                label = "Master Password",
                isError = uiState.passwordError != null,
                errorMessage = uiState.passwordError,
                imeAction = ImeAction.Next,
                onImeAction = { confirmPasswordFocusRequester.requestFocus() },
                showGenerateButton = true,
                onGeneratePassword = viewModel::generatePassword,
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

            // Confirm Password
            PasswordTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChanged,
                label = "Confirm Password",
                isError = uiState.confirmPasswordError != null,
                errorMessage = uiState.confirmPasswordError,
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (canCreateDatabase(uiState)) {
                        viewModel.createDatabase(onDatabaseCreated)
                    }
                },
                showPasswordToggle = false,
                modifier = Modifier.focusRequester(confirmPasswordFocusRequester)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Color Theme Selection
            Text(
                text = "Choose Color Theme",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(UserDatabaseInfo.availableColorThemes) { theme ->
                    ColorThemeChip(
                        colorTheme = theme,
                        isSelected = theme == uiState.colorTheme,
                        onClick = { viewModel.onColorThemeChanged(theme) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Create Button
            Button(
                onClick = { viewModel.createDatabase(onDatabaseCreated) },
                enabled = canCreateDatabase(uiState) && !uiState.isCreating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating...")
                } else {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Encrypted Database")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security note
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
                    Column {
                        Text(
                            text = "Security Notice",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your master password encrypts the entire database. Make sure to use a strong, memorable password. Lost passwords cannot be recovered.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorThemeChip(
    colorTheme: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = DatabaseColors.getColor(colorTheme)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color)
                .then(
                    if (isSelected) {
                        Modifier.background(
                            MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = colorTheme.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun canCreateDatabase(uiState: com.example.xdlocker.viewmodel.CreateDatabaseUiState): Boolean {
    return uiState.label.isNotBlank() &&
            uiState.password.isNotBlank() &&
            uiState.password == uiState.confirmPassword &&
            uiState.labelError == null &&
            uiState.passwordError == null &&
            uiState.confirmPasswordError == null
}