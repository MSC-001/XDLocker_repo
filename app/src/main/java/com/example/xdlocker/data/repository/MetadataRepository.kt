package com.example.xdlocker.data.repository

import com.example.xdlocker.data.database.DatabaseManager
import com.example.xdlocker.data.entities.UserDatabaseInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing database metadata operations
 * Handles all operations related to UserDatabaseInfo
 */
@Singleton
class MetadataRepository @Inject constructor(
    private val databaseManager: DatabaseManager
) {

    private val metadataDao = databaseManager.getMetadataDao()

    // Read Operations

    /**
     * Gets all databases sorted by last accessed (most recent first)
     */
    fun getAllDatabases(): Flow<List<UserDatabaseInfo>> {
        return metadataDao.getAllDatabases()
            .catch { emit(emptyList()) }
    }

    /**
     * Gets all databases sorted by name
     */
    fun getAllDatabasesByName(): Flow<List<UserDatabaseInfo>> {
        return metadataDao.getAllDatabasesByName()
            .catch { emit(emptyList()) }
    }

    /**
     * Gets all databases sorted by creation date
     */
    fun getAllDatabasesByCreationDate(): Flow<List<UserDatabaseInfo>> {
        return metadataDao.getAllDatabasesByCreationDate()
            .catch { emit(emptyList()) }
    }

    /**
     * Gets recently accessed databases (last 5)
     */
    fun getRecentlyAccessedDatabases(): Flow<List<UserDatabaseInfo>> {
        return metadataDao.getRecentlyAccessedDatabases()
            .catch { emit(emptyList()) }
    }

    /**
     * Searches databases by label or description
     */
    fun searchDatabases(query: String): Flow<List<UserDatabaseInfo>> {
        return metadataDao.searchDatabases(query)
            .catch { emit(emptyList()) }
    }

    /**
     * Gets databases by color theme
     */
    fun getDatabasesByColorTheme(colorTheme: String): Flow<List<UserDatabaseInfo>> {
        return metadataDao.getDatabasesByColorTheme(colorTheme)
            .catch { emit(emptyList()) }
    }

    /**
     * Gets database by ID
     */
    suspend fun getDatabaseById(databaseId: Int): UserDatabaseInfo? {
        return try {
            metadataDao.getDatabaseById(databaseId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets database by filename
     */
    suspend fun getDatabaseByFilename(filename: String): UserDatabaseInfo? {
        return try {
            metadataDao.getDatabaseByFilename(filename)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets database by label
     */
    suspend fun getDatabaseByLabel(label: String): UserDatabaseInfo? {
        return try {
            metadataDao.getDatabaseByLabel(label)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets total count of databases
     */
    suspend fun getDatabaseCount(): Int {
        return try {
            metadataDao.getDatabaseCount()
        } catch (e: Exception) {
            0
        }
    }

    // Validation Operations

    /**
     * Checks if a database label already exists
     */
    suspend fun isLabelExists(label: String): Boolean {
        return try {
            metadataDao.isLabelExists(label) > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a database filename already exists
     */
    suspend fun isFilenameExists(filename: String): Boolean {
        return try {
            metadataDao.isFilenameExists(filename) > 0
        } catch (e: Exception) {
            false
        }
    }

    // Write Operations

    /**
     * Creates a new password database
     */
    suspend fun createDatabase(
        label: String,
        password: String,
        description: String = "",
        colorTheme: String = "blue"
    ): Result<UserDatabaseInfo> {
        return try {
            databaseManager.createPasswordDatabase(label, password, description, colorTheme)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates database metadata
     */
    suspend fun updateDatabase(databaseInfo: UserDatabaseInfo): Result<Unit> {
        return try {
            metadataDao.updateDatabase(databaseInfo)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates database label
     */
    suspend fun updateDatabaseLabel(
        databaseInfo: UserDatabaseInfo,
        newLabel: String,
        password: String
    ): Result<Unit> {
        return try {
            databaseManager.renameDatabaseLabel(databaseInfo, newLabel, password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates database description
     */
    suspend fun updateDescription(databaseId: Int, description: String): Result<Unit> {
        return try {
            metadataDao.updateDescription(databaseId, description)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates database color theme
     */
    suspend fun updateColorTheme(databaseId: Int, colorTheme: String): Result<Unit> {
        return try {
            metadataDao.updateColorTheme(databaseId, colorTheme)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates last accessed timestamp
     */
    suspend fun updateLastAccessed(databaseId: Int): Result<Unit> {
        return try {
            metadataDao.updateLastAccessed(databaseId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates entry count for a database
     */
    suspend fun updateEntryCount(databaseId: Int, count: Int): Result<Unit> {
        return try {
            metadataDao.updateEntryCount(databaseId, count)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a database and its metadata
     */
    suspend fun deleteDatabase(databaseInfo: UserDatabaseInfo): Result<Unit> {
        return try {
            databaseManager.deletePasswordDatabase(databaseInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Database Management Operations

    /**
     * Changes password for a database
     */
    suspend fun changePassword(
        databaseInfo: UserDatabaseInfo,
        oldPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            databaseManager.changePassword(databaseInfo, oldPassword, newPassword)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets database size information
     */
    fun getDatabaseSize(filename: String): String {
        return databaseManager.getDatabaseSize(filename)
    }

    // Statistics and Analytics

    /**
     * Gets database statistics
     */
    fun getDatabaseStatistics(): Flow<DatabaseStatistics> {
        return getAllDatabases().map { databases ->
            DatabaseStatistics(
                totalDatabases = databases.size,
                totalEntries = databases.sumOf { it.entryCount },
                mostRecentlyAccessed = databases.maxByOrNull { it.lastAccessed },
                oldestDatabase = databases.minByOrNull { it.createdAt },
                colorThemeDistribution = databases.groupBy { it.colorTheme }
                    .mapValues { it.value.size }
            )
        }
    }
}

/**
 * Data class for database statistics
 */
data class DatabaseStatistics(
    val totalDatabases: Int,
    val totalEntries: Int,
    val mostRecentlyAccessed: UserDatabaseInfo?,
    val oldestDatabase: UserDatabaseInfo?,
    val colorThemeDistribution: Map<String, Int>
)