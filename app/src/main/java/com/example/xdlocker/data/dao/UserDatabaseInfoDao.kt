package com.example.xdlocker.data.dao

import androidx.room.*
import com.example.xdlocker.data.entities.UserDatabaseInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDatabaseInfoDao {

    // Get all user databases
    @Query("SELECT * FROM user_databases ORDER BY last_accessed DESC")
    fun getAllDatabases(): Flow<List<UserDatabaseInfo>>

    // Get all databases sorted by name
    @Query("SELECT * FROM user_databases ORDER BY database_label ASC")
    fun getAllDatabasesByName(): Flow<List<UserDatabaseInfo>>

    // Get all databases sorted by creation date
    @Query("SELECT * FROM user_databases ORDER BY created_at DESC")
    fun getAllDatabasesByCreationDate(): Flow<List<UserDatabaseInfo>>

    // Get all user databases as a suspend function returning a List
    @Query("SELECT * FROM user_databases")
    suspend fun getAllDatabasesSuspend(): List<UserDatabaseInfo>

    // Get database by ID
    @Query("SELECT * FROM user_databases WHERE id = :databaseId")
    suspend fun getDatabaseById(databaseId: Int): UserDatabaseInfo?

    // Get database by filename
    @Query("SELECT * FROM user_databases WHERE database_filename = :filename")
    suspend fun getDatabaseByFilename(filename: String): UserDatabaseInfo?

    // Get database by label
    @Query("SELECT * FROM user_databases WHERE database_label = :label")
    suspend fun getDatabaseByLabel(label: String): UserDatabaseInfo?

    // Check if database label already exists
    @Query("SELECT COUNT(*) FROM user_databases WHERE database_label = :label")
    suspend fun isLabelExists(label: String): Int

    // Check if database filename already exists
    @Query("SELECT COUNT(*) FROM user_databases WHERE database_filename = :filename")
    suspend fun isFilenameExists(filename: String): Int

    // Insert new database info
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDatabase(databaseInfo: UserDatabaseInfo): Long

    // Update database info
    @Update
    suspend fun updateDatabase(databaseInfo: UserDatabaseInfo)

    // Update database label
    @Query("UPDATE user_databases SET database_label = :newLabel WHERE id = :databaseId")
    suspend fun updateDatabaseLabel(databaseId: Int, newLabel: String)

    // Update last accessed timestamp
    @Query("UPDATE user_databases SET last_accessed = :timestamp WHERE id = :databaseId")
    suspend fun updateLastAccessed(databaseId: Int, timestamp: Long = System.currentTimeMillis())

    // Update last accessed by filename
    @Query("UPDATE user_databases SET last_accessed = :timestamp WHERE database_filename = :filename")
    suspend fun updateLastAccessedByFilename(filename: String, timestamp: Long = System.currentTimeMillis())

    // Update entry count
    @Query("UPDATE user_databases SET entry_count = :count WHERE id = :databaseId")
    suspend fun updateEntryCount(databaseId: Int, count: Int)

    // Update entry count by filename
    @Query("UPDATE user_databases SET entry_count = :count WHERE database_filename = :filename")
    suspend fun updateEntryCountByFilename(filename: String, count: Int)

    // Update description
    @Query("UPDATE user_databases SET description = :description WHERE id = :databaseId")
    suspend fun updateDescription(databaseId: Int, description: String)

    // Update color theme
    @Query("UPDATE user_databases SET color_theme = :colorTheme WHERE id = :databaseId")
    suspend fun updateColorTheme(databaseId: Int, colorTheme: String)

    // Delete database info
    @Delete
    suspend fun deleteDatabase(databaseInfo: UserDatabaseInfo)

    // Delete database by ID
    @Query("DELETE FROM user_databases WHERE id = :databaseId")
    suspend fun deleteDatabaseById(databaseId: Int)

    // Delete database by filename
    @Query("DELETE FROM user_databases WHERE database_filename = :filename")
    suspend fun deleteDatabaseByFilename(filename: String)

    // Get total count of databases
    @Query("SELECT COUNT(*) FROM user_databases")
    suspend fun getDatabaseCount(): Int

    // Get recently accessed databases (last 5)
    @Query("SELECT * FROM user_databases ORDER BY last_accessed DESC LIMIT 5")
    fun getRecentlyAccessedDatabases(): Flow<List<UserDatabaseInfo>>

    // Search databases by label or description
    @Query("""
        SELECT * FROM user_databases 
        WHERE database_label LIKE '%' || :searchQuery || '%' 
        OR description LIKE '%' || :searchQuery || '%'
        ORDER BY database_label ASC
    """)
    fun searchDatabases(searchQuery: String): Flow<List<UserDatabaseInfo>>

    // Get databases by color theme
    @Query("SELECT * FROM user_databases WHERE color_theme = :colorTheme ORDER BY database_label ASC")
    fun getDatabasesByColorTheme(colorTheme: String): Flow<List<UserDatabaseInfo>>
}