package com.example.xdlocker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xdlocker.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAppLockSetup: () -> Unit,
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSortOrderDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showRemovePinDialog by remember { mutableStateOf(false) }
    var pinInputForRemoval by remember { mutableStateOf("") }

    // Observe lifecycle to refresh UI state on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAppLockStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Security Section
            SettingsSection(title = "Security") {
                // App Lock
                SettingsItem(
                    title = "App Lock",
                    subtitle = if (uiState.isAppLockEnabled) "Enabled" else "Disabled",
                    icon = Icons.Default.Lock,
                    onClick = {
                        if (uiState.isAppLockEnabled) {
                            pinInputForRemoval = "" // Reset PIN input
                            viewModel.clearError() // Clear previous errors if any
                            showRemovePinDialog = true
                        } else {
                            onNavigateToAppLockSetup()
                        }
                    },
                    trailing = null
                )

                // Biometric Authentication
                if (uiState.isAppLockEnabled) {
                    SettingsItem(
                        title = "Biometric Authentication",
                        subtitle = "Use fingerprint or face unlock",
                        icon = Icons.Default.Fingerprint,
                        onClick = { viewModel.toggleBiometric() },
                        trailing = {
                            Switch(
                                checked = uiState.isBiometricEnabled,
                                onCheckedChange = { viewModel.toggleBiometric() }
                            )
                        }
                    )

                    // Auto Lock Timeout
                    SettingsItem(
                        title = "Auto Lock",
                        subtitle = "Lock after ${uiState.autoLockTimeoutMinutes} minutes of inactivity",
                        icon = Icons.Default.Timer,
                        onClick = { showAutoLockDialog = true }
                    )
                }
            }

            // Appearance Section
            SettingsSection(title = "Appearance") {
                // Dark Theme
                SettingsItem(
                    title = "Dark Theme",
                    subtitle = if (uiState.isDarkTheme) "Enabled" else "Use system setting",
                    icon = Icons.Default.DarkMode,
                    onClick = { viewModel.toggleDarkTheme() },
                    trailing = {
                        Switch(
                            checked = uiState.isDarkTheme,
                            onCheckedChange = { viewModel.toggleDarkTheme() }
                        )
                    }
                )

                // Default Sort Order
                SettingsItem(
                    title = "Default Sort Order",
                    subtitle = uiState.defaultSortOrder,
                    icon = Icons.AutoMirrored.Filled.Sort,
                    onClick = { showSortOrderDialog = true }
                )
            }

            // Data Management Section
            SettingsSection(title = "Data Management") {
                // Export Data
                SettingsItem(
                    title = "Export Data",
                    subtitle = "Export all databases and settings",
                    icon = Icons.Default.FileDownload,
                    onClick = { showExportDialog = true }
                )

                // Import Data
                SettingsItem(
                    title = "Import Data",
                    subtitle = "Import from backup file",
                    icon = Icons.Default.FileUpload,
                    onClick = { showImportDialog = true }
                )

                // Clear All Data
                SettingsItem(
                    title = "Clear All Data",
                    subtitle = "Delete all databases and reset app",
                    icon = Icons.Default.DeleteForever,
                    onClick = { showClearDataDialog = true },
                    isDestructive = true
                )
            }

            // About Section
            SettingsSection(title = "About") {
                // App Version
                SettingsItem(
                    title = "Version",
                    subtitle = viewModel.getAppVersion(),
                    icon = Icons.Default.Info,
                    onClick = { /* Show version details */ }
                )

                // Open Source Licenses
                SettingsItem(
                    title = "Open Source Licenses",
                    subtitle = "View third-party licenses",
                    icon = Icons.Default.Code,
                    onClick = { /* Show licenses */ }
                )

                // Privacy Policy
                SettingsItem(
                    title = "Privacy Policy",
                    subtitle = "How we handle your data",
                    icon = Icons.Default.PrivacyTip,
                    onClick = { /* Open privacy policy */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        } // End Column
    } // End Scaffold

    // Dialogs
    if (showSortOrderDialog) {
        SortOrderDialog(
            currentSortOrder = uiState.defaultSortOrder,
            sortOrders = viewModel.getAvailableSortOrders(),
            onSortOrderSelected = { sortOrder ->
                viewModel.updateDefaultSortOrder(sortOrder)
                showSortOrderDialog = false
            },
            onDismiss = { showSortOrderDialog = false }
        )
    }

    if (showAutoLockDialog) {
        AutoLockDialog(
            currentTimeout = uiState.autoLockTimeoutMinutes,
            timeoutOptions = viewModel.getAutoLockOptions(),
            onTimeoutSelected = { timeout ->
                viewModel.updateAutoLockTimeout(timeout)
                showAutoLockDialog = false
            },
            onDismiss = { showAutoLockDialog = false }
        )
    }

    if (showExportDialog) {
        ExportDataDialog(
            onExport = {
                viewModel.exportData { /* exportData -> Handle export data */ }
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false },
            isLoading = uiState.isLoading
        )
    }

    if (showImportDialog) {
        ImportDataDialog(
            onImport = { data ->
                viewModel.importData(data) { /* Handle import success */ }
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false },
            isLoading = uiState.isLoading
        )
    }

    if (showClearDataDialog) {
        ClearDataDialog(
            requirePin = uiState.isAppLockEnabled,
            onConfirm = { pin ->
                viewModel.clearAllData(pin) { /* Handle clear success */ }
                showClearDataDialog = false
            },
            onDismiss = { showClearDataDialog = false },
            isLoading = uiState.isLoading
        )
    }

    if (showRemovePinDialog) {
        RemovePinDialog(
            pinInput = pinInputForRemoval,
            onPinInputChange = { pinInputForRemoval = it },
            onDismiss = {
                showRemovePinDialog = false
                viewModel.clearError() // Clear error on dismiss
            },
            onConfirm = {
                if (pinInputForRemoval.isNotBlank()) {
                    viewModel.removeAppLock(
                        pin = pinInputForRemoval,
                        onSuccess = {
                            showRemovePinDialog = false
                            viewModel.refreshAppLockStatus() // Explicitly refresh
                            Toast.makeText(context, "App Lock Disabled", Toast.LENGTH_SHORT).show()
                        },
                    )
                } else {
                    // Optionally, show a local error if PIN is blank, though ViewModel should handle it too
                    // viewModel.setError("PIN cannot be empty") // Example
                }
            },
            error = uiState.error
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDestructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun SortOrderDialog(
    currentSortOrder: String,
    sortOrders: List<String>,
    onSortOrderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default Sort Order") },
        text = {
            Column {
                sortOrders.forEach { sortOrder ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortOrder == currentSortOrder,
                            onClick = { onSortOrderSelected(sortOrder) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(sortOrder)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun AutoLockDialog(
    currentTimeout: Int,
    timeoutOptions: List<Int>,
    onTimeoutSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto Lock Timeout") },
        text = {
            Column {
                timeoutOptions.forEach { timeout ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = timeout == currentTimeout,
                            onClick = { onTimeoutSelected(timeout) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$timeout minutes")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun ExportDataDialog(
    onExport: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Text("Export all your databases and settings to a backup file. This file will contain encrypted data that can be imported later.")
        },
        confirmButton = {
            Button(
                onClick = onExport,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Export")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ImportDataDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    var importData by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Data") },
        text = {
            Column {
                Text("Paste or enter your backup data below:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = importData,
                    onValueChange = { importData = it },
                    label = { Text("Backup Data") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(importData) },
                enabled = importData.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Import")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ClearDataDialog(
    requirePin: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear All Data") },
        text = {
            Column {
                Text(
                    "This will permanently delete ALL databases and reset the app to its initial state.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("This action cannot be undone!")

                if (requirePin) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text("Enter PIN to confirm") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = (!requirePin || pin.isNotBlank()) && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Text("Clear All Data")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

// Dialog to remove PIN
@Composable
private fun RemovePinDialog(
    pinInput: String,
    onPinInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    error: String?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Current PIN to Disable") },
        text = {
            Column {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = onPinInputChange,
                    label = { Text("Current PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm Disable")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

