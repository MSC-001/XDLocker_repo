package com.example.xdlocker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdlocker.data.entities.UserDatabaseInfo
import com.example.xdlocker.data.repository.DatabaseSortCriteria
import com.example.xdlocker.data.repository.MetadataRepository
import com.example.xdlocker.data.repository.RepositoryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseListViewModel @Inject constructor(
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(DatabaseListUiState())
    val uiState = _uiState.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Sort criteria
    private val _sortCriteria = MutableStateFlow(DatabaseSortCriteria.LAST_ACCESSED_DESC)
    val sortCriteria = _sortCriteria.asStateFlow()

    // Databases flow with search and sort applied
    val databases = combine(
        metadataRepository.getAllDatabases(),
        searchQuery,
        sortCriteria
    ) { databases, query, sort ->
        // Filter by query
        val filtered = if (query.isBlank()) {
            databases
        } else {
            val lowerQuery = query.lowercase()
            databases.filter { database ->
                database.databaseLabel.lowercase().contains(lowerQuery) ||
                database.description.lowercase().contains(lowerQuery)
            }
        }
        
        // Sort by criteria
        when (sort) {
            DatabaseSortCriteria.NAME_ASC -> filtered.sortedBy { it.databaseLabel.lowercase() }
            DatabaseSortCriteria.NAME_DESC -> filtered.sortedByDescending { it.databaseLabel.lowercase() }
            DatabaseSortCriteria.DATE_CREATED_ASC -> filtered.sortedBy { it.createdAt }
            DatabaseSortCriteria.DATE_CREATED_DESC -> filtered.sortedByDescending { it.createdAt }
            DatabaseSortCriteria.LAST_ACCESSED_ASC -> filtered.sortedBy { it.lastAccessed }
            DatabaseSortCriteria.LAST_ACCESSED_DESC -> filtered.sortedByDescending { it.lastAccessed }
            DatabaseSortCriteria.ENTRY_COUNT_ASC -> filtered.sortedBy { it.entryCount }
            DatabaseSortCriteria.ENTRY_COUNT_DESC -> filtered.sortedByDescending { it.entryCount }
        }
    }.catch { error ->
        _uiState.update { it.copy(error = error.message) }
        emptyList<UserDatabaseInfo>()
    }.stateIn<List<UserDatabaseInfo>>(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Database statistics
    val statistics = metadataRepository.getDatabaseStatistics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Load initial data
        refreshDatabases()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortCriteriaChanged(criteria: DatabaseSortCriteria) {
        _sortCriteria.value = criteria
    }

    fun refreshDatabases() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        // The databases flow will automatically update
        _uiState.update { it.copy(isLoading = false) }
    }

    fun deleteDatabase(database: UserDatabaseInfo, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }

            // First validate password by trying to open the database
            val openResult = metadataRepository.createDatabase(
                database.databaseLabel + "_temp_validation",
                password
            )

            if (openResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        error = "Invalid password"
                    )
                }
                return@launch
            }

            // Password is valid, proceed with deletion
            val result = metadataRepository.deleteDatabase(database)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteSuccess = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun renameDatabase(database: UserDatabaseInfo, newLabel: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = metadataRepository.updateDatabaseLabel(database, newLabel, password)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showRenameSuccess = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun updateDatabaseDescription(database: UserDatabaseInfo, description: String) {
        viewModelScope.launch {
            val result = metadataRepository.updateDescription(database.id, description)
            result.fold(
                onSuccess = {
                    // Success handled silently
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun updateDatabaseColorTheme(database: UserDatabaseInfo, colorTheme: String) {
        viewModelScope.launch {
            val result = metadataRepository.updateColorTheme(database.id, colorTheme)
            result.fold(
                onSuccess = {
                    // Success handled silently
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun getDatabaseSize(filename: String): String {
        return metadataRepository.getDatabaseSize(filename)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessages() {
        _uiState.update {
            it.copy(
                showDeleteSuccess = false,
                showRenameSuccess = false
            )
        }
    }
}

data class DatabaseListUiState(
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val showDeleteSuccess: Boolean = false,
    val showRenameSuccess: Boolean = false
)