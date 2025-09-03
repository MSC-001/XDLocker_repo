package com.example.xdlocker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
// import androidx.compose.ui.Alignment // Removed unused import
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

    // State for RemovePinDialog
    var pinInputForRemoval by rememberSaveable { mutableStateOf("") }

    // State for ClearDataDialog
    var clearDataConfirmationStep by rememberSaveable { mutableStateOf(0) } // 0: idle, 1: step 1, 2: step 2, 3: step 3
    var pinForClearDataDialog by rememberSaveable { mutableStateOf("") }
    var clearDataDialogError by remember { mutableStateOf<String?>(null) }
    var isClearDataStepLoading by remember { mutableStateOf(false) }


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

    LaunchedEffect(uiState.showClearSuccess) {
        if (uiState.showClearSuccess) {
            Toast.makeText(context, "All data cleared successfully!", Toast.LENGTH_LONG).show()
            showClearDataDialog = false
            clearDataConfirmationStep = 0
            pinForClearDataDialog = ""
            clearDataDialogError = null
            isClearDataStepLoading = false
            viewModel.clearSuccessMessages()
            viewModel.clearError() // Clear global error from viewmodel
            viewModel.refreshAppLockStatus()
        }
    }

    LaunchedEffect(uiState.error) {
        // If the dialog is on its final step and a global error appears from clearAllData
        if (showClearDataDialog && clearDataConfirmationStep == 3 && uiState.error != null) {
            clearDataDialogError = uiState.error
            isClearDataStepLoading = false // Ensure local step loading stops if global error takes over
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
            SettingsSection(title = "Security") {
                SettingsItem(
                    title = "App Lock",
                    subtitle = if (uiState.isAppLockEnabled) "Enabled" else "Disabled",
                    icon = Icons.Default.Lock,
                    onClick = {
                        if (uiState.isAppLockEnabled) {
                            pinInputForRemoval = ""
                            viewModel.clearError()
                            showRemovePinDialog = true
                        } else {
                            onNavigateToAppLockSetup()
                        }
                    },
                    trailing = null
                )

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
                    SettingsItem(
                        title = "Auto Lock",
                        subtitle = "Lock after ${uiState.autoLockTimeoutMinutes} minutes of inactivity",
                        icon = Icons.Default.Timer,
                        onClick = { showAutoLockDialog = true }
                    )
                }
            }

            SettingsSection(title = "Appearance") {
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
                SettingsItem(
                    title = "Default Sort Order",
                    subtitle = uiState.defaultSortOrder,
                    icon = Icons.AutoMirrored.Filled.Sort,
                    onClick = { showSortOrderDialog = true }
                )
            }

            SettingsSection(title = "Data Management") {
                SettingsItem(
                    title = "Export Data",
                    subtitle = "Export all databases and settings",
                    icon = Icons.Default.FileDownload,
                    onClick = { showExportDialog = true }
                )
                SettingsItem(
                    title = "Import Data",
                    subtitle = "Import from backup file",
                    icon = Icons.Default.FileUpload,
                    onClick = { showImportDialog = true }
                )
                SettingsItem(
                    title = "Clear All Data",
                    subtitle = "Delete all databases and reset app",
                    icon = Icons.Default.DeleteForever,
                    onClick = {
                        viewModel.clearError()      // Clear global ViewModel error first
                        clearDataDialogError = null // Clear local dialog error
                        pinForClearDataDialog = ""  // Reset PIN input
                        isClearDataStepLoading = false // Reset step loading
                        clearDataConfirmationStep = 1 // Start confirmation flow from step 1
                        showClearDataDialog = true
                    },
                    isDestructive = true
                )
            }
            SettingsSection(title = "About") {
                SettingsItem(
                    title = "Version",
                    subtitle = viewModel.getAppVersion(),
                    icon = Icons.Default.Info,
                    onClick = { /* Show version details */ }
                )
                SettingsItem(
                    title = "Open Source Licenses",
                    subtitle = "View third-party licenses",
                    icon = Icons.Default.Code,
                    onClick = { /* Show licenses */ }
                )
                SettingsItem(
                    title = "Privacy Policy",
                    subtitle = "How we handle your data",
                    icon = Icons.Default.PrivacyTip,
                    onClick = { /* Open privacy policy */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

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
                viewModel.exportData(/* Pass any necessary params or callbacks */)
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false },
            isLoading = uiState.isLoading
        )
    }

    if (showImportDialog) {
        ImportDataDialog(
            onImport = { data ->
                viewModel.importData(/* Pass data and any callbacks */)
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false },
            isLoading = uiState.isLoading
        )
    }

    if (showClearDataDialog && clearDataConfirmationStep > 0) {
        val stepToExecute = clearDataConfirmationStep // Capture for the lambda
        ClearDataDialogInternal(
            currentStep = stepToExecute,
            isAppLockEnabled = uiState.isAppLockEnabled,
            pinInput = pinForClearDataDialog,
            onPinInputChange = { pinForClearDataDialog = it },
            dialogError = clearDataDialogError,
            isLoadingViewModel = uiState.isLoading, // From ViewModel, for final clearAllData call
            isStepLoading = isClearDataStepLoading, // Local loading for intermediate PIN checks
            onConfirm = {
                val currentStepVal = stepToExecute // Use captured stepToExecute for consistency in this lambda
                val isAppLockEnabledVal = uiState.isAppLockEnabled // Capture current state
                val pinForDialogVal = pinForClearDataDialog // Capture current PIN input

                isClearDataStepLoading = true
                clearDataDialogError = null // Clear previous step error

                if (isAppLockEnabledVal) { // Use captured state
                    viewModel.verifyPinForConfirmationStep(pinForDialogVal) { success, errorMsg ->
                        if (success) {
                            if (currentStepVal < 3) {
                                clearDataConfirmationStep++
                                pinForClearDataDialog = "" // Clear PIN for next step's input
                            } else { // Final step (Step 3)
                                viewModel.clearAllData(pinForDialogVal) // Pass the last verified PIN
                            }
                        } else {
                            clearDataDialogError = errorMsg
                        }
                        isClearDataStepLoading = false // Stop step loading after verification attempt
                    }
                } else { // No App Lock Enabled (or perceived as OFF by current uiState)
                    isClearDataStepLoading = false // No async op for no-PIN steps
                    if (currentStepVal < 3) {
                        clearDataConfirmationStep++
                    } else { // Final step (Step 3)
                        viewModel.clearAllData("")
                    }
                }
            },
            onDismiss = {
                showClearDataDialog = false
                clearDataConfirmationStep = 0
                pinForClearDataDialog = ""
                clearDataDialogError = null
                isClearDataStepLoading = false
                viewModel.clearError() // Clear global error from viewmodel if any
            }
        )
    }

    if (showRemovePinDialog) {
        RemovePinDialog(
            pinInput = pinInputForRemoval, // This is used here
            onPinInputChange = { pinInputForRemoval = it },
            onDismiss = {
                showRemovePinDialog = false
                viewModel.clearError()
            },
            onConfirm = {
                if (pinInputForRemoval.isNotBlank()) {
                    viewModel.removeAppLock(
                        pin = pinInputForRemoval,
                        onSuccess = {
                            showRemovePinDialog = false
                            viewModel.refreshAppLockStatus()
                            Toast.makeText(context, "App Lock Disabled", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            },
            error = uiState.error
        )
    }
}

// Helper composables (SettingsSection, SettingsItem, dialogs etc.)
// These are assumed to be complete in the user's actual file.

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
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
    icon: ImageVector,
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
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
private fun ClearDataDialogInternal(
    currentStep: Int,
    isAppLockEnabled: Boolean,
    pinInput: String,
    onPinInputChange: (String) -> Unit,
    dialogError: String?,
    isLoadingViewModel: Boolean,
    isStepLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogTitle: String
    val confirmButtonText: String

    // Determine title and confirm button text based on current step
    when (currentStep) {
        1 -> {
            dialogTitle = "Clear All Data (Step 1 of 3)"
            confirmButtonText = "Confirm Step 1"
        }
        2 -> {
            dialogTitle = "Clear All Data (Step 2 of 3)"
            confirmButtonText = "Confirm Step 2"
        }
        3 -> {
            dialogTitle = "FINAL WARNING (Step 3 of 3)"
            confirmButtonText = "CLEAR ALL DATA"
        }
        else -> {
            onDismiss() // Should not happen if dialog is only shown for steps 1-3
            return
        }
    }

    val actualIsLoading = if (currentStep == 3) isLoadingViewModel else isStepLoading

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column {
                // Determine text content based on current step
                when (currentStep) {
                    1 -> {
                        Text("This will permanently delete ALL user databases and reset the app.")
                        if (isAppLockEnabled) Spacer(modifier = Modifier.height(8.dp))
                        Text("This action cannot be undone. Please confirm to proceed.")
                    }
                    2 -> {
                        Text("SECOND CONFIRMATION: You are about to delete all data.")
                        if (isAppLockEnabled) Spacer(modifier = Modifier.height(8.dp))
                        Text("Are you absolutely sure you wish to continue?")
                    }
                    3 -> {
                        Text("ALL YOUR DATA WILL BE PERMANENTLY DELETED.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        if (isAppLockEnabled) Spacer(modifier = Modifier.height(8.dp))
                        Text("There is no going back after this step.")
                    }
                }

                if (isAppLockEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = onPinInputChange,
                        label = { Text("Enter PIN (Step $currentStep of 3)") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = dialogError != null,
                        singleLine = true,
                        enabled = !actualIsLoading
                    )
                    if (dialogError != null) {
                        Text(
                            text = dialogError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = (if (isAppLockEnabled) pinInput.isNotBlank() else true) && !actualIsLoading,
                colors = if (currentStep == 3) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
            ) {
                if (actualIsLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = if (currentStep == 3) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(confirmButtonText)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !actualIsLoading) {
                Text("Cancel")
            }
        }
    )
}

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
