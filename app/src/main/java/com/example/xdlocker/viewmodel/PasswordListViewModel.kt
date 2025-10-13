package com.example.xdlocker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xdlocker.data.database.InvalidPasswordException
import com.example.xdlocker.data.entities.PasswordEntry
import com.example.xdlocker.data.entities.UserDatabaseInfo
import com.example.xdlocker.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordListViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(PasswordListUiState())
    val uiState = _uiState.asStateFlow()

    // Current database info
    private val _currentDatabase = MutableStateFlow<UserDatabaseInfo?>(null)
    val currentDatabase = _currentDatabase.asStateFlow()

    // Search and filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow("All")
    val selectedTag = _selectedTag.asStateFlow()

    private val _sortCriteria = MutableStateFlow(SortCriteria.TITLE_ASC)
    val sortCriteria = _sortCriteria.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly = _showFavoritesOnly.asStateFlow()

    // Database connection filename as a reactive flow
    private val databaseFilenameFlow = MutableStateFlow<String?>(null)

    // Main password entries flow, reacts to db changes, search, and filters
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val passwordEntries: StateFlow<List<PasswordEntry>> = databaseFilenameFlow.flatMapLatest { filename ->
        if (filename == null) {
            flowOf(emptyList()) // If no db is open, emit an empty list
        } else {
            // Get the base flow of entries from the repository
            passwordRepository.getAllEntries(filename)
                .combine(_searchQuery) { entries, query ->
                    if (query.isBlank()) entries else entries.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.username.contains(query, ignoreCase = true) ||
                        it.url.contains(query, ignoreCase = true)
                    }
                }
                .combine(_selectedTag) { entries, tag ->
                    if (tag == "All") entries else entries.filter { it.tag == tag }
                }
                .combine(_showFavoritesOnly) { entries, favoritesOnly ->
                    if (!favoritesOnly) entries else entries.filter { it.isFavorite }
                }
                .combine(_sortCriteria) { entries, sort ->
                    when (sort) {
                        SortCriteria.TITLE_ASC -> entries.sortedBy { it.title.lowercase() }
                        SortCriteria.TITLE_DESC -> entries.sortedByDescending { it.title.lowercase() }
                        SortCriteria.DATE_CREATED_ASC -> entries.sortedBy { it.createdAt }
                        SortCriteria.DATE_CREATED_DESC -> entries.sortedByDescending { it.createdAt }
                        SortCriteria.DATE_UPDATED_ASC -> entries.sortedBy { it.updatedAt }
                        SortCriteria.DATE_UPDATED_DESC -> entries.sortedByDescending { it.updatedAt }
                        SortCriteria.FAVORITE_FIRST -> entries.sortedByDescending { it.isFavorite }
                    }
                }
                .catch { error ->
                    _uiState.update { it.copy(error = error.message) }
                    emit(emptyList()) // On error, emit empty list
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    // Available tags flow
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val availableTags: StateFlow<List<String>> = databaseFilenameFlow.flatMapLatest { filename ->
        if (filename == null) {
            flowOf(listOf("All"))
        } else {
            passwordRepository.getAllTags(filename)
                .map { tags -> listOf("All") + tags.distinct().sorted() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("All")
    )


    fun openDatabase(databaseInfo: UserDatabaseInfo, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = passwordRepository.openDatabase(databaseInfo, password)
            result.fold(
                onSuccess = { connection ->
                    _currentDatabase.value = databaseInfo
                    databaseFilenameFlow.value = connection.filename // Update the flow
                    _uiState.update { it.copy(isLoading = false, isDatabaseOpen = true) }

                    // Update last accessed time
                    metadataRepository.updateLastAccessed(databaseInfo.id)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = when (error) {
                                is InvalidPasswordException -> "Invalid password"
                                else -> error.message ?: "Failed to open database"
                            }
                        )
                    }
                }
            )
        }
    }

    fun closeDatabase() {
        viewModelScope.launch {
            databaseFilenameFlow.value?.let { filename ->
                passwordRepository.closeDatabase(filename)
            }
            _currentDatabase.value = null
            databaseFilenameFlow.value = null // Clear the filename
            _uiState.update {
                it.copy(
                    isDatabaseOpen = false,
                    error = null
                )
            }
            resetFilters()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTagSelected(tag: String) {
        _selectedTag.value = tag
    }

    fun onSortCriteriaChanged(criteria: SortCriteria) {
        _sortCriteria.value = criteria
    }

    fun toggleShowFavoritesOnly() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavorite(entry: PasswordEntry) {
        viewModelScope.launch {
            databaseFilenameFlow.value?.let { filename ->
                val result = passwordRepository.toggleFavorite(
                    filename,
                    entry.id,
                    !entry.isFavorite
                )

                if (result.isFailure) {
                    _uiState.update { it.copy(error = "Failed to update favorite") }
                }
            }
        }
    }

    fun deleteEntry(entry: PasswordEntry) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }

            databaseFilenameFlow.value?.let { filename ->
                val result = passwordRepository.deleteEntry(filename, entry)
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
                                error = error.message ?: "Failed to delete entry"
                            )
                        }
                    }
                )
            }
        }
    }

    fun copyToClipboard(text: String, label: String) {
        // This will be handled by the UI layer
        _uiState.update {
            it.copy(clipboardMessage = "$label copied to clipboard")
        }
    }

    fun refreshEntries() {
        // This is no longer needed as the flow is now fully reactive.
        // Kept for compatibility if called from UI, but it does nothing.
    }

    private fun resetFilters() {
        _searchQuery.value = ""
        _selectedTag.value = "All"
        _sortCriteria.value = SortCriteria.TITLE_ASC
        _showFavoritesOnly.value = false
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearClipboardMessage() {
        _uiState.update { it.copy(clipboardMessage = null) }
    }

    fun clearSuccessMessages() {
        _uiState.update {
            it.copy(
                showDeleteSuccess = false,
                clipboardMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Close database connection when ViewModel is cleared
        viewModelScope.launch {
            closeDatabase()
        }
    }
}

data class PasswordListUiState(
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isDatabaseOpen: Boolean = false,
    val error: String? = null,
    val showDeleteSuccess: Boolean = false,
    val clipboardMessage: String? = null
)
