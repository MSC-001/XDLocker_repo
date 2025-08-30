package com.example.xdlocker.data.repository

import com.example.xdlocker.data.database.DatabaseConnection
import com.example.xdlocker.data.database.DatabaseManager
import com.example.xdlocker.data.database.InvalidPasswordException
import com.example.xdlocker.data.entities.PasswordEntry
import com.example.xdlocker.data.entities.UserDatabaseInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing password entries operations
 * Handles all operations related to PasswordEntry within encrypted databases
 */
@Singleton
class PasswordRepository @Inject constructor(
    private val databaseManager: DatabaseManager
) {

    // Database Connection Management

    /**
     * Opens a password database and returns connection
     */
    suspend fun openDatabase(
        databaseInfo: UserDatabaseInfo,
        password: String
    ): Result<DatabaseConnection> {
        return try {
            databaseManager.openPasswordDatabase(databaseInfo, password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Closes a password database
     */
    suspend fun closeDatabase(filename: String) {
        databaseManager.closePasswordDatabase(filename)
    }

    /**
     * Gets active database connection
     */
    fun getActiveConnection(filename: String): DatabaseConnection? {
        return databaseManager.getActiveDatabaseConnection(filename)
    }

    // Read Operations

    /**
     * Gets all password entries from an active database
     */
    fun getAllEntries(filename: String): Flow<List<PasswordEntry>> {
        return getActiveConnection(filename)?.database?.passwordEntryDao()?.getAllEntries()
            ?.catch { emit(emptyList()) }
            ?: emptyFlow()
    }

    /**
     * Gets all entries sorted by creation date
     */
    fun getAllEntriesByDateCreated(filename: String): Flow<List<PasswordEntry>> {
        return getActiveConnection(filename)?.database?.passwordEntryDao()?.getAllEntriesByDateCreated()
            ?.catch { emit(emptyList()) }
            ?: emptyFlow()
    }

    /**
     * Gets all entries sorted by last updated
     */
    fun getAllEntriesByDateUpdated(filename: String): Flow<List<PasswordEntry>> {
        return getActiveConnection(filename)?.database?.passwordEntryDao()?.getAllEntriesByDateUpdated()
            ?.catch { emit(emptyList()) }
            ?: emptyFlow()
    }

    /**
     * Gets favorite entries
     */
    fun getFavoriteEntries(filename: String): Flow<List<PasswordEntry>> {
        return getActiveConnection(filename)?.database?.passwordEntryDao()?.getFavoriteEntries()
            ?.catch { emit(emptyList()) }
            ?: emptyFlow()
    }

    /**
     * Searches entries by query
     */
    fun searchEntries(filename: String, query: String): Flow<List<PasswordEntry>> {
        return getActiveConnection(filename)?.database?.passwordEntryDao()?.searchEntries(query)
            ?.catch { emit(emptyList()) }
            ?: emptyFlow()
    }

    /**
     * Gets entries by tag
     */
    fun getEntriesByTag(filename: String, tag: String): Flow<List<PasswordEntry>> {
        return getActiveConnection(filename)?.database?.passwordEntryDao()?.getEntriesByTag(tag)
            ?.catch { emit(emptyList()) }
            ?: emptyFlow()
    }

    /**
     * Gets all unique tags
     */
    fun getAllTags(filename: String): Flow<List<String>> {
        return getActiveConnection(filename)?.database?.passwordEntryDao()?.getAllTags()
            ?.catch { emit(emptyList()) }
            ?: emptyFlow()
    }

    /**
     * Gets entries with weak passwords
     */
    fun getEntriesWithWeakPasswords(filename: String): Flow<List<PasswordEntry>> {
        return getActiveConnection(filename)?.database?.passwordEntryDao()?.getEntriesWithWeakPasswords()
            ?.catch { emit(emptyList()) }
            ?: emptyFlow()
    }

    /**
     * Gets entry by ID
     */
    suspend fun getEntryById(filename: String, entryId: Int): PasswordEntry? {
        return try {
            getActiveConnection(filename)?.database?.passwordEntryDao()?.getEntryById(entryId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets total entry count
     */
    suspend fun getEntryCount(filename: String): Int {
        return try {
            getActiveConnection(filename)?.database?.passwordEntryDao()?.getEntryCount() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // Write Operations

    /**
     * Inserts a new password entry
     */
    suspend fun insertEntry(filename: String, entry: PasswordEntry): Result<Long> {
        return try {
            val connection = getActiveConnection(filename)
                ?: return Result.failure(IllegalStateException("Database not open"))

            val entryWithTimestamp = entry.copy(
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val id = connection.database.passwordEntryDao().insertEntry(entryWithTimestamp)

            // Update entry count in metadata
            databaseManager.updateEntryCount(filename)

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing password entry
     */
    suspend fun updateEntry(filename: String, entry: PasswordEntry): Result<Unit> {
        return try {
            val connection = getActiveConnection(filename)
                ?: return Result.failure(IllegalStateException("Database not open"))

            val updatedEntry = entry.copy(updatedAt = System.currentTimeMillis())
            connection.database.passwordEntryDao().updateEntry(updatedEntry)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a password entry
     */
    suspend fun deleteEntry(filename: String, entry: PasswordEntry): Result<Unit> {
        return try {
            val connection = getActiveConnection(filename)
                ?: return Result.failure(IllegalStateException("Database not open"))

            connection.database.passwordEntryDao().deleteEntry(entry)

            // Update entry count in metadata
            databaseManager.updateEntryCount(filename)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes entry by ID
     */
    suspend fun deleteEntryById(filename: String, entryId: Int): Result<Unit> {
        return try {
            val connection = getActiveConnection(filename)
                ?: return Result.failure(IllegalStateException("Database not open"))

            connection.database.passwordEntryDao().deleteEntryById(entryId)

            // Update entry count in metadata
            databaseManager.updateEntryCount(filename)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggles favorite status of an entry
     */
    suspend fun toggleFavorite(filename: String, entryId: Int, isFavorite: Boolean): Result<Unit> {
        return try {
            val connection = getActiveConnection(filename)
                ?: return Result.failure(IllegalStateException("Database not open"))

            connection.database.passwordEntryDao().toggleFavorite(entryId, isFavorite)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates last accessed time for an entry
     */
    suspend fun updateLastAccessed(filename: String, entryId: Int): Result<Unit> {
        return try {
            val connection = getActiveConnection(filename)
                ?: return Result.failure(IllegalStateException("Database not open"))

            connection.database.passwordEntryDao().updateLastAccessed(entryId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Bulk Operations

    /**
     * Inserts multiple entries (for import)
     */
    suspend fun insertEntries(filename: String, entries: List<PasswordEntry>): Result<Unit> {
        return try {
            val connection = getActiveConnection(filename)
                ?: return Result.failure(IllegalStateException("Database not open"))

            val entriesWithTimestamp = entries.map { entry ->
                entry.copy(
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }

            connection.database.passwordEntryDao().insertEntries(entriesWithTimestamp)

            // Update entry count in metadata
            databaseManager.updateEntryCount(filename)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes all entries
     */
    suspend fun deleteAllEntries(filename: String): Result<Unit> {
        return try {
            val connection = getActiveConnection(filename)
                ?: return Result.failure(IllegalStateException("Database not open"))

            connection.database.passwordEntryDao().deleteAllEntries()

            // Update entry count in metadata
            databaseManager.updateEntryCount(filename)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Security Analysis

    /**
     * Gets password security statistics
     */
    fun getPasswordSecurityStats(filename: String): Flow<PasswordSecurityStats> {
        return getAllEntries(filename).map { entries ->
            val weakPasswords = entries.count { it.password.length < 8 }
            val duplicatePasswords = entries.groupBy { it.password }
                .values.count { it.size > 1 }

            val passwordStrengths = entries.map { entry ->
                calculatePasswordStrength(entry.password)
            }

            PasswordSecurityStats(
                totalEntries = entries.size,
                weakPasswords = weakPasswords,
                duplicatePasswords = duplicatePasswords,
                averagePasswordStrength = passwordStrengths.average(),
                strongPasswords = passwordStrengths.count { it >= 80 },
                mediumPasswords = passwordStrengths.count { it in 40..79 }
            )
        }
    }

    /**
     * Calculates password strength score (0-100)
     */
    private fun calculatePasswordStrength(password: String): Int {
        var score = 0

        // Length
        score += when {
            password.length >= 12 -> 25
            password.length >= 8 -> 15
            password.length >= 6 -> 10
            else -> 0
        }

        // Character types
        if (password.any { it.isLowerCase() }) score += 5
        if (password.any { it.isUpperCase() }) score += 5
        if (password.any { it.isDigit() }) score += 5
        if (password.any { !it.isLetterOrDigit() }) score += 10

        // Complexity
        val charTypes = listOf(
            password.any { it.isLowerCase() },
            password.any { it.isUpperCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() }
        ).count { it }

        score += charTypes * 10

        // Bonus for very long passwords
        if (password.length >= 16) score += 10

        return minOf(score, 100)
    }
}

/**
 * Data class for password security statistics
 */
data class PasswordSecurityStats(
    val totalEntries: Int,
    val weakPasswords: Int,
    val duplicatePasswords: Int,
    val averagePasswordStrength: Double,
    val strongPasswords: Int,
    val mediumPasswords: Int
) {
    val weakPasswordsPercentage: Float
        get() = if (totalEntries > 0) (weakPasswords.toFloat() / totalEntries) * 100 else 0f

    val strongPasswordsPercentage: Float
        get() = if (totalEntries > 0) (strongPasswords.toFloat() / totalEntries) * 100 else 0f
}