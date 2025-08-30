package com.example.xdlocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xdlocker.data.entities.UserDatabaseInfo
import com.example.xdlocker.data.repository.DatabaseSortCriteria
import com.example.xdlocker.ui.components.*
import com.example.xdlocker.viewmodel.DatabaseListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseListScreen(
    onNavigateToCreateDatabase: () -> Unit,
    onNavigateToPasswordList: (UserDatabaseInfo) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DatabaseListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val databases by viewModel.databases.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortCriteria by viewModel.sortCriteria.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var selectedDatabase by remember { mutableStateOf<UserDatabaseInfo?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "XDLocker",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, "Sort databases")
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DatabaseSortCriteria.values().forEach { criteria ->
                            DropdownMenuItem(
                                text = { Text(getSortDisplayName(criteria)) },
                                onClick = {
                                    viewModel.onSortCriteriaChanged(criteria)
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateDatabase
            ) {
                Icon(Icons.Default.Add, "Create database")
            }
        }
    ) { paddingValues ->
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
                placeholder = "Search databases...",
                onClearClick = { viewModel.onSearchQueryChanged("") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics overview
            statistics?.let { stats ->
                if (stats.totalDatabases > 0) {
                    StatsOverviewCard(
                        totalDatabases = stats.totalDatabases,
                        totalEntries = stats.totalEntries
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Error display
            uiState.error?.let { error ->
                ErrorCard(
                    message = error,
                    onDismiss = viewModel::clearError
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Content
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }

                databases.isEmpty() -> {
                    EmptyStateContent(
                        onCreateDatabase = onNavigateToCreateDatabase
                    )
                }

                else -> {
                    DatabaseListContent(
                        databases = databases,
                        onDatabaseClick = { database ->
                            selectedDatabase = database
                            showPasswordDialog = true
                        },
                        onDatabaseDelete = { database ->
                            selectedDatabase = database
                            showDeleteDialog = true
                        },
                        getDatabaseSize = viewModel::getDatabaseSize
                    )
                }
            }
        }
    }

    // Dialogs
    if (showPasswordDialog && selectedDatabase != null) {
        DatabasePasswordDialog(
            database = selectedDatabase!!,
            onPasswordConfirmed = { password ->
                // Here we would validate password and navigate
                onNavigateToPasswordList(selectedDatabase!!)
                showPasswordDialog = false
                selectedDatabase = null
            },
            onDismiss = {
                showPasswordDialog = false
                selectedDatabase = null
            }
        )
    }

    if (showDeleteDialog && selectedDatabase != null) {
        DeleteDatabaseDialog(
            database = selectedDatabase!!,
            onConfirm = { password ->
                viewModel.deleteDatabase(selectedDatabase!!, password)
                showDeleteDialog = false
                selectedDatabase = null
            },
            onDismiss = {
                showDeleteDialog = false
                selectedDatabase = null
            },
            isLoading = uiState.isDeleting
        )
    }
}

@Composable
private fun StatsOverviewCard(
    totalDatabases: Int,
    totalEntries: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                value = totalDatabases.toString(),
                label = "Databases",
                icon = Icons.Default.Storage
            )
            VerticalDivider(modifier = Modifier.height(48.dp))
            StatColumn(
                value = totalEntries.toString(),
                label = "Total Entries",
                icon = Icons.Default.Key
            )
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                text = "Loading databases...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyStateContent(
    onCreateDatabase: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        EmptyDatabaseCard(onCreateDatabase = onCreateDatabase)
    }
}

@Composable
private fun DatabaseListContent(
    databases: List<UserDatabaseInfo>,
    onDatabaseClick: (UserDatabaseInfo) -> Unit,
    onDatabaseDelete: (UserDatabaseInfo) -> Unit,
    getDatabaseSize: (String) -> String
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(databases) { database ->
            DatabaseCard(
                database = database,
                onClick = { onDatabaseClick(database) },
                onEdit = { /* Handle edit */ },
                onDelete = { onDatabaseDelete(database) }
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
                Text("Enter password for \"${database.databaseLabel}\"")
                Spacer(modifier = Modifier.height(16.dp))
                PasswordTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isError = false
                    },
                    label = "Password",
                    isError = isError,
                    errorMessage = if (isError) "Invalid password" else null,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    onImeAction = {
                        if (password.isNotBlank()) {
                            onPasswordConfirmed(password)
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

@Composable
private fun DeleteDatabaseDialog(
    database: UserDatabaseInfo,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Database") },
        text = {
            Column {
                Text("This will permanently delete \"${database.databaseLabel}\" and all its ${database.entryCount} password entries.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This action cannot be undone!",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                PasswordTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        isError = false
                    },
                    label = "Enter password to confirm",
                    isError = isError,
                    errorMessage = if (isError) "Password required" else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isNotBlank()) {
                        onConfirm(password)
                    } else {
                        isError = true
                    }
                },
                enabled = password.isNotBlank() && !isLoading,
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
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun getSortDisplayName(criteria: DatabaseSortCriteria): String {
    return when (criteria) {
        DatabaseSortCriteria.NAME_ASC -> "Name (A-Z)"
        DatabaseSortCriteria.NAME_DESC -> "Name (Z-A)"
        DatabaseSortCriteria.DATE_CREATED_ASC -> "Oldest First"
        DatabaseSortCriteria.DATE_CREATED_DESC -> "Newest First"
        DatabaseSortCriteria.LAST_ACCESSED_ASC -> "Least Recently Used"
        DatabaseSortCriteria.LAST_ACCESSED_DESC -> "Most Recently Used"
        DatabaseSortCriteria.ENTRY_COUNT_ASC -> "Fewest Entries"
        DatabaseSortCriteria.ENTRY_COUNT_DESC -> "Most Entries"
    }
}