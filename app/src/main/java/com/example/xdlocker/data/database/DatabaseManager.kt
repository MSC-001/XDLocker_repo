package com.example.xdlocker.data.database

import android.content.Context
import com.example.xdlocker.data.entities.UserDatabaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Centralized manager for all database operations
 * Handles both metadata database and password databases
 */
class DatabaseManager private constructor(private val context: Context) {

    private val metadataDatabase: MetadataDatabase by lazy {
        MetadataDatabase.getDatabase(context)
    }

    private val activeDatabases = mutableMapOf<String, DatabaseConnection>()
    private val databaseMutex = Mutex()

    companion object {
        @Volatile
        private var INSTANCE: DatabaseManager? = null

        fun getInstance(context: Context): DatabaseManager {
            return INSTANCE ?: synchronized(this) {
                val instance = DatabaseManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // Metadata Database Operations

    /**
     * Gets the metadata database DAO
     */
    fun getMetadataDao() = metadataDatabase.userDatabaseInfoDao()

    /**
     * Creates a new password database and adds it to metadata
     */
    suspend fun createPasswordDatabase(
        label: String,
        password: String,
        description: String = "",
        colorTheme: String = "blue"
    ): Result<UserDatabaseInfo> = withContext(Dispatchers.IO) {
        databaseMutex.withLock {
            try {
                // Check if label already exists
                val existing = metadataDatabase.userDatabaseInfoDao().getDatabaseByLabel(label)
                if (existing != null) {
                    return@withContext Result.failure(DatabaseException("Database with label '$label' already exists"))
                }

                // Generate unique filename
                var filename: String
                do {
                    filename = UserDatabaseInfo.generateUniqueFilename()
                } while (metadataDatabase.userDatabaseInfoDao().isFilenameExists(filename) > 0)

                // Create the encrypted password database
                val passwordDb = PasswordDatabase.create(context, filename, password)
                    ?: return@withContext Result.failure(DatabaseException("Failed to create encrypted database"))

                // Create metadata entry
                val databaseInfo = UserDatabaseInfo(
                    databaseLabel = label,
                    databaseFilename = filename,
                    description = description,
                    colorTheme = colorTheme
                )

                // Insert metadata
                val id = metadataDatabase.userDatabaseInfoDao().insertDatabase(databaseInfo)
                val createdInfo = databaseInfo.copy(id = id.toInt())

                // Close the database for now
                passwordDb.close()

                Result.success(createdInfo)

            } catch (e: Exception) {
                Result.failure(DatabaseException("Failed to create password database", e))
            }
        }
    }

    /**
     * Opens a password database and keeps it in memory
     */
    suspend fun openPasswordDatabase(
        databaseInfo: UserDatabaseInfo,
        password: String
    ): Result<DatabaseConnection> = withContext(Dispatchers.IO) {
        databaseMutex.withLock {
            try {
                // Check if already open
                activeDatabases[databaseInfo.databaseFilename]?.let {
                    return@withContext Result.success(it)
                }

                // Validate password first
                if (!PasswordDatabase.validatePassword(context, databaseInfo.databaseFilename, password)) {
                    return@withContext Result.failure(InvalidPasswordException())
                }

                // Open the database
                val passwordDb = PasswordDatabase.open(context, databaseInfo.databaseFilename, password)
                    ?: return@withContext Result.failure(DatabaseException("Failed to open database"))

                // Create connection
                val connection = DatabaseConnection(
                    database = passwordDb,
                    filename = databaseInfo.databaseFilename,
                    label = databaseInfo.databaseLabel
                )

                // Store in active databases
                activeDatabases[databaseInfo.databaseFilename] = connection

                // Update last accessed time
                metadataDatabase.userDatabaseInfoDao().updateLastAccessed(databaseInfo.id)

                Result.success(connection)

            } catch (e: Exception) {
                Result.failure(DatabaseException("Failed to open password database", e))
            }
        }
    }

    /**
     * Closes a password database
     */
    suspend fun closePasswordDatabase(filename: String) = withContext(Dispatchers.IO) {
        databaseMutex.withLock {
            activeDatabases[filename]?.let { connection ->
                connection.close()
                activeDatabases.remove(filename)
            }
        }
    }

    /**
     * Gets an active database connection
     */
    fun getActiveDatabaseConnection(filename: String): DatabaseConnection? {
        return activeDatabases[filename]
    }

    /**
     * Deletes a password database and its metadata
     */
    suspend fun deletePasswordDatabase(databaseInfo: UserDatabaseInfo): Result<Unit> = withContext(Dispatchers.IO) {
        databaseMutex.withLock {
            try {
                // Close database if active
                activeDatabases[databaseInfo.databaseFilename]?.let { connection ->
                    connection.close()
                    activeDatabases.remove(databaseInfo.databaseFilename)
                }

                // Delete database file
                SQLCipherHelper.deleteDatabase(context, databaseInfo.databaseFilename)

                // Delete metadata
                metadataDatabase.userDatabaseInfoDao().deleteDatabase(databaseInfo)

                Result.success(Unit)

            } catch (e: Exception) {
                Result.failure(DatabaseException("Failed to delete database", e))
            }
        }
    }

    /**
     * Renames a database label
     */
    suspend fun renameDatabaseLabel(
        databaseInfo: UserDatabaseInfo,
        newLabel: String,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Validate password
            if (!PasswordDatabase.validatePassword(context, databaseInfo.databaseFilename, password)) {
                return@withContext Result.failure(InvalidPasswordException())
            }

            // Check if new label already exists
            val existing = metadataDatabase.userDatabaseInfoDao().getDatabaseByLabel(newLabel)
            if (existing != null && existing.id != databaseInfo.id) {
                return@withContext Result.failure(DatabaseException("Database with label '$newLabel' already exists"))
            }

            // Update metadata
            metadataDatabase.userDatabaseInfoDao().updateDatabaseLabel(databaseInfo.id, newLabel)

            // Update active connection if exists
            activeDatabases[databaseInfo.databaseFilename]?.let { connection ->
                activeDatabases[databaseInfo.databaseFilename] = connection.copy(label = newLabel)
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to rename database", e))
        }
    }

    /**
     * Changes password for a database
     */
    suspend fun changePassword(
        databaseInfo: UserDatabaseInfo,
        oldPassword: String,
        newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        databaseMutex.withLock {
            try {
                // Close database if active
                activeDatabases[databaseInfo.databaseFilename]?.let { connection ->
                    connection.close()
                    activeDatabases.remove(databaseInfo.databaseFilename)
                }

                // Change password
                val success = SQLCipherHelper.changePassword(
                    context,
                    databaseInfo.databaseFilename,
                    oldPassword,
                    newPassword
                )

                if (!success) {
                    return@withContext Result.failure(DatabaseException("Failed to change password"))
                }

                Result.success(Unit)

            } catch (e: Exception) {
                Result.failure(DatabaseException("Failed to change password", e))
            }
        }
    }

    /**
     * Updates entry count for a database
     */
    suspend fun updateEntryCount(filename: String) = withContext(Dispatchers.IO) {
        try {
            activeDatabases[filename]?.let { connection ->
                val count = connection.database.passwordEntryDao().getEntryCount()
                metadataDatabase.userDatabaseInfoDao().updateEntryCountByFilename(filename, count)
            }
        } catch (e: Exception) {
            // Log error but don't fail
            e.printStackTrace()
        }
    }

    /**
     * Gets database size information
     */
    fun getDatabaseSize(filename: String): String {
        val size = SQLCipherHelper.getDatabaseSize(context, filename)
        return SQLCipherHelper.formatDatabaseSize(size)
    }

    /**
     * Closes all active databases
     */
    suspend fun closeAllDatabases() = withContext(Dispatchers.IO) {
        databaseMutex.withLock {
            activeDatabases.values.forEach { connection ->
                connection.close()
            }
            activeDatabases.clear()
        }
    }

    /**
     * Gets list of all active database filenames
     */
    fun getActiveDatabaseFilenames(): List<String> {
        return activeDatabases.keys.toList()
    }

    /**
     * Cleanup method to be called when app is destroyed
     */
    suspend fun cleanup() {
        closeAllDatabases()
        metadataDatabase.close()
    }
}