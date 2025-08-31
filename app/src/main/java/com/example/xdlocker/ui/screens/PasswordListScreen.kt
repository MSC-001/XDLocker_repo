package com.example.xdlocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xdlocker.data.entities.PasswordEntry
import com.example.xdlocker.data.entities.UserDatabaseInfo
import com.example.xdlocker.data.repository.SortCriteria
import com.example.xdlocker.ui.components.*
import com.example.xdlocker.viewmodel.PasswordListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordListScreen(
    databaseInfo: UserDatabaseInfo,
    onNavigateBack: () -> Unit,
    onNavigateToAddPassword: () -> Unit,
    onNavigateToEditPassword: (PasswordEntry) -> Unit,
    viewModel: PasswordListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val passwordEntries by viewModel.passwordEntries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val availableTags by viewModel.availableTags.collectAsStateWithLifecycle(initialValue = emptyList()) // Added initialValue
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle(initialValue = false) // Added initialValue

    var showSortMenu by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(!uiState.isDatabaseOpen) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<PasswordEntry?>(null) }

    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(databaseInfo.databaseLabel, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${passwordEntries.size} entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.closeDatabase()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                    }

                    IconButton(onClick = viewModel::toggleShowFavoritesOnly) {
                        Icon(
                            imageVector = if (showFavoritesOnly) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Filter favorites",
                            tint = if (showFavoritesOnly) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortCriteria.entries.forEach { criteria ->
                            DropdownMenuItem(
                                text = { Text(getSortDisplayName(criteria)) },
                                onClick = {
                                    viewModel.onSortCriteriaChanged(criteria)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.isDatabaseOpen) {
                FloatingActionButton(onClick = onNavigateToAddPassword) {
                    Icon(Icons.Default.Add, "Add password")
                }
            }
        }
    ) { paddingValues ->

        if (!uiState.isDatabaseOpen) {
            // Show password input to open database
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (showPasswordDialog) {
                    DatabasePasswordDialog(
                        database = databaseInfo,
                        onPasswordConfirmed = { password ->
                            viewModel.openDatabase(databaseInfo, password)
                            showPasswordDialog = false
                        },
                        onDismiss = {
                            showPasswordDialog = false
                            onNavigateBack()
                        }
                    )
                }
            }
        } else {
            // Main content when database is open
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Search bar
                SearchTextField(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    placeholder = "Search passwords...",
                    onClearClick = { viewModel.onSearchQueryChanged("") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tag filter chips
                if (availableTags.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableTags) { tag ->
                            FilterChip(
                                selected = tag == selectedTag,
                                onClick = { viewModel.onTagSelected(tag) },
                                label = { Text(tag) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Error display
                uiState.error?.let { error ->
                    ErrorCard(
                        message = error,
                        onDismiss = viewModel::clearError
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Content
                when {
                    uiState.isLoading -> {
                        LoadingContent()
                    }

                    passwordEntries.isEmpty() -> {
                        EmptyPasswordCard(
                            onAddPassword = onNavigateToAddPassword
                        )
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(passwordEntries) { entry ->
                                PasswordEntryCard(
                                    entry = entry,
                                    onClick = { onNavigateToEditPassword(entry) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(entry) },
                                    onCopyUsername = {
                                        clipboardManager.setText(AnnotatedString(entry.username))
                                        viewModel.copyToClipboard(entry.username, "Username")
                                    },
                                    onCopyPassword = {
                                        clipboardManager.setText(AnnotatedString(entry.password))
                                        viewModel.copyToClipboard(entry.password, "Password")
                                    },
                                    onDelete = {
                                        selectedEntry = entry
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedEntry != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedEntry = null
            },
            title = { Text("Delete Password") },
            text = {
                Text("Are you sure you want to delete \"${selectedEntry!!.title}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEntry(selectedEntry!!)
                        showDeleteDialog = false
                        selectedEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !uiState.isDeleting
                ) {
                    if (uiState.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedEntry = null
                    },
                    enabled = !uiState.isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading passwords...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun DatabasePasswordDialog(
    database: UserDatabaseInfo,
    onPasswordConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Password") },
        text = {
            Column {
                Text("Enter the master password for:")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${database.databaseLabel}\"",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                PasswordTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isError = false
                    },
                    label = "Master Password",
                    isError = isError,
                    errorMessage = if (isError) "Password cannot be empty" else null,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    onImeAction = {
                        if (password.isNotBlank()) {
                            onPasswordConfirmed(password)
                        } else {
                            isError = true
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isNotBlank()) {
                        onPasswordConfirmed(password)
                    } else {
                        isError = true
                    }
                },
                enabled = password.isNotBlank()
            ) {
                Text("Open")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getSortDisplayName(criteria: SortCriteria): String {
    return when (criteria) {
        SortCriteria.TITLE_ASC -> "Title (A-Z)"
        SortCriteria.TITLE_DESC -> "Title (Z-A)"
        SortCriteria.DATE_CREATED_ASC -> "Oldest First"
        SortCriteria.DATE_CREATED_DESC -> "Newest First"
        SortCriteria.DATE_UPDATED_ASC -> "Least Recently Updated"
        SortCriteria.DATE_UPDATED_DESC -> "Most Recently Updated"
        SortCriteria.FAVORITE_FIRST -> "Favorites First"
    }
}