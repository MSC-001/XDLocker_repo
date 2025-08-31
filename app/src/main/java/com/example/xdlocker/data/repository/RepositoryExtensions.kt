package com.example.xdlocker.data.repository

import com.example.xdlocker.data.entities.PasswordEntry
import com.example.xdlocker.data.entities.UserDatabaseInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Extension functions and utilities for repositories
 */

// Sorting enums
enum class SortCriteria {
    TITLE_ASC,
    TITLE_DESC,
    DATE_CREATED_ASC,
    DATE_CREATED_DESC,
    DATE_UPDATED_ASC,
    DATE_UPDATED_DESC,
    FAVORITE_FIRST
}

enum class DatabaseSortCriteria {
    NAME_ASC,
    NAME_DESC,
    DATE_CREATED_ASC,
    DATE_CREATED_DESC,
    LAST_ACCESSED_ASC,
    LAST_ACCESSED_DESC,
    ENTRY_COUNT_ASC,
    ENTRY_COUNT_DESC
}

// Password Entry Extensions
fun Flow<List<PasswordEntry>>.sortBy(criteria: SortCriteria): Flow<List<PasswordEntry>> {
    return this.map { entries ->
        when (criteria) {
            SortCriteria.TITLE_ASC -> entries.sortedBy { it.title.lowercase() }
            SortCriteria.TITLE_DESC -> entries.sortedByDescending { it.title.lowercase() }
            SortCriteria.DATE_CREATED_ASC -> entries.sortedBy { it.createdAt }
            SortCriteria.DATE_CREATED_DESC -> entries.sortedByDescending { it.createdAt }
            SortCriteria.DATE_UPDATED_ASC -> entries.sortedBy { it.updatedAt }
            SortCriteria.DATE_UPDATED_DESC -> entries.sortedByDescending { it.updatedAt }
            SortCriteria.FAVORITE_FIRST -> entries.sortedWith(
                compareByDescending<PasswordEntry> { it.isFavorite }
                    .thenBy { it.title.lowercase() }
            )
        }
    }
}

fun Flow<List<PasswordEntry>>.filterEntriesByQuery(query: String): Flow<List<PasswordEntry>> {
    return this.map { entries ->
        if (query.isBlank()) {
            entries
        } else {
            val lowerQuery = query.lowercase()
            entries.filter { entry ->
                entry.title.lowercase().contains(lowerQuery) ||
                        entry.username.lowercase().contains(lowerQuery) ||
                        entry.url.lowercase().contains(lowerQuery) ||
                        entry.tag.lowercase().contains(lowerQuery) ||
                        entry.notes.lowercase().contains(lowerQuery)
            }
        }
    }
}

fun Flow<List<PasswordEntry>>.filterByTag(tag: String): Flow<List<PasswordEntry>> {
    return this.map { entries ->
        if (tag.isBlank() || tag == "All") {
            entries
        } else {
            entries.filter { it.tag.equals(tag, ignoreCase = true) }
        }
    }
}

fun Flow<List<PasswordEntry>>.filterFavorites(favoritesOnly: Boolean): Flow<List<PasswordEntry>> {
    return this.map { entries ->
        if (favoritesOnly) {
            entries.filter { it.isFavorite }
        } else {
            entries
        }
    }
}

fun Flow<List<PasswordEntry>>.groupByFirstLetter(): Flow<Map<Char, List<PasswordEntry>>> {
    return this.map { entries ->
        entries.groupBy {
            it.title.firstOrNull()?.uppercaseChar() ?: '#'
        }.toSortedMap()
    }
}

fun Flow<List<PasswordEntry>>.groupByTag(): Flow<Map<String, List<PasswordEntry>>> {
    return this.map { entries ->
        entries.groupBy {
            it.tag.ifBlank { "Untagged" }
        }
    }
}

fun Flow<List<PasswordEntry>>.getStatistics(): Flow<EntryStatistics> {
    return this.map { entries ->
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)
        val oneMonthAgo = now - (30 * 24 * 60 * 60 * 1000)

        EntryStatistics(
            totalEntries = entries.size,
            favoriteEntries = entries.count { it.isFavorite },
            entriesAddedThisWeek = entries.count { it.createdAt >= oneWeekAgo },
            entriesAddedThisMonth = entries.count { it.createdAt >= oneMonthAgo },
            entriesUpdatedThisWeek = entries.count { it.updatedAt >= oneWeekAgo },
            totalTags = entries.map { it.tag }.filter { it.isNotBlank() }.distinct().size,
            averagePasswordLength = entries.map { it.password.length }.average().takeIf { !it.isNaN() } ?: 0.0,
            mostUsedTag = entries.map { it.tag }.filter { it.isNotBlank() }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        )
    }
}

// Database Info Extensions
fun Flow<List<UserDatabaseInfo>>.sortBy(criteria: DatabaseSortCriteria): Flow<List<UserDatabaseInfo>> {
    return this.map { databases ->
        when (criteria) {
            DatabaseSortCriteria.NAME_ASC -> databases.sortedBy { it.databaseLabel.lowercase() }
            DatabaseSortCriteria.NAME_DESC -> databases.sortedByDescending { it.databaseLabel.lowercase() }
            DatabaseSortCriteria.DATE_CREATED_ASC -> databases.sortedBy { it.createdAt }
            DatabaseSortCriteria.DATE_CREATED_DESC -> databases.sortedByDescending { it.createdAt }
            DatabaseSortCriteria.LAST_ACCESSED_ASC -> databases.sortedBy { it.lastAccessed }
            DatabaseSortCriteria.LAST_ACCESSED_DESC -> databases.sortedByDescending { it.lastAccessed }
            DatabaseSortCriteria.ENTRY_COUNT_ASC -> databases.sortedBy { it.entryCount }
            DatabaseSortCriteria.ENTRY_COUNT_DESC -> databases.sortedByDescending { it.entryCount }
        }
    }
}

fun Flow<List<UserDatabaseInfo>>.filterDatabasesByQuery(query: String): Flow<List<UserDatabaseInfo>> {
    return this.map { databases ->
        if (query.isBlank()) {
            databases
        } else {
            val lowerQuery = query.lowercase()
            databases.filter { database ->
                database.databaseLabel.lowercase().contains(lowerQuery) ||
                        database.description.lowercase().contains(lowerQuery)
            }
        }
    }
}

fun Flow<List<UserDatabaseInfo>>.groupByColorTheme(): Flow<Map<String, List<UserDatabaseInfo>>> {
    return this.map { databases ->
        databases.groupBy { it.colorTheme }
    }
}

// Data classes for statistics
data class EntryStatistics(
    val totalEntries: Int,
    val favoriteEntries: Int,
    val entriesAddedThisWeek: Int,
    val entriesAddedThisMonth: Int,
    val entriesUpdatedThisWeek: Int,
    val totalTags: Int,
    val averagePasswordLength: Double,
    val mostUsedTag: String?
)

// Result wrapper for repository operations
sealed class RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>()
    data class Error(val exception: Throwable) : RepositoryResult<Nothing>()
    data class Loading(val message: String = "") : RepositoryResult<Nothing>()
}

// Extension functions for Result handling
fun <T> Result<T>.toRepositoryResult(): RepositoryResult<T> {
    return if (isSuccess) {
        RepositoryResult.Success(getOrThrow())
    } else {
        RepositoryResult.Error(exceptionOrNull() ?: Exception("Unknown error"))
    }
}

inline fun <T> RepositoryResult<T>.onSuccess(action: (T) -> Unit): RepositoryResult<T> {
    if (this is RepositoryResult.Success) action(data)
    return this
}

inline fun <T> RepositoryResult<T>.onError(action: (Throwable) -> Unit): RepositoryResult<T> {
    if (this is RepositoryResult.Error) action(exception)
    return this
}

inline fun <T> RepositoryResult<T>.onLoading(action: (String) -> Unit): RepositoryResult<T> {
    if (this is RepositoryResult.Loading) action(message)
    return this
}

// Flow combination utilities
fun combineEntriesWithStatistics(
    entriesFlow: Flow<List<PasswordEntry>>,
    securityStatsFlow: Flow<PasswordSecurityStats>
): Flow<Pair<List<PasswordEntry>, PasswordSecurityStats>> {
    return combine(entriesFlow, securityStatsFlow) { entries, stats ->
        Pair(entries, stats)
    }
}