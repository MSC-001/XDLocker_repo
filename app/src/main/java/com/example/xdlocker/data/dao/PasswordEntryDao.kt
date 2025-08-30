package com.example.xdlocker.data.dao

import androidx.room.*
import com.example.xdlocker.data.entities.PasswordEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordEntryDao {

    // Get all password entries
    @Query("SELECT * FROM password_entries ORDER BY title ASC")
    fun getAllEntries(): Flow<List<PasswordEntry>>

    // Get all entries sorted by creation date (newest first)
    @Query("SELECT * FROM password_entries ORDER BY created_at DESC")
    fun getAllEntriesByDateCreated(): Flow<List<PasswordEntry>>

    // Get all entries sorted by last updated (newest first)
    @Query("SELECT * FROM password_entries ORDER BY updated_at DESC")
    fun getAllEntriesByDateUpdated(): Flow<List<PasswordEntry>>

    // Get favorite entries
    @Query("SELECT * FROM password_entries WHERE is_favorite = 1 ORDER BY title ASC")
    fun getFavoriteEntries(): Flow<List<PasswordEntry>>

    // Search entries by title, username, or URL
    @Query("""
        SELECT * FROM password_entries 
        WHERE title LIKE '%' || :searchQuery || '%' 
        OR username LIKE '%' || :searchQuery || '%' 
        OR url LIKE '%' || :searchQuery || '%'
        OR tag LIKE '%' || :searchQuery || '%'
        ORDER BY title ASC
    """)
    fun searchEntries(searchQuery: String): Flow<List<PasswordEntry>>

    // Get entries by tag
    @Query("SELECT * FROM password_entries WHERE tag = :tag ORDER BY title ASC")
    fun getEntriesByTag(tag: String): Flow<List<PasswordEntry>>

    // Get all unique tags
    @Query("SELECT DISTINCT tag FROM password_entries WHERE tag != '' ORDER BY tag ASC")
    fun getAllTags(): Flow<List<String>>

    // Get entry by ID
    @Query("SELECT * FROM password_entries WHERE id = :entryId")
    suspend fun getEntryById(entryId: Int): PasswordEntry?

    // Insert new password entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PasswordEntry): Long

    // Update existing password entry
    @Update
    suspend fun updateEntry(entry: PasswordEntry)

    // Delete password entry
    @Delete
    suspend fun deleteEntry(entry: PasswordEntry)

    // Delete entry by ID
    @Query("DELETE FROM password_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Int)

    // Toggle favorite status
    @Query("UPDATE password_entries SET is_favorite = :isFavorite WHERE id = :entryId")
    suspend fun toggleFavorite(entryId: Int, isFavorite: Boolean)

    // Update last accessed time for entry
    @Query("UPDATE password_entries SET updated_at = :timestamp WHERE id = :entryId")
    suspend fun updateLastAccessed(entryId: Int, timestamp: Long = System.currentTimeMillis())

    // Get total count of entries
    @Query("SELECT COUNT(*) FROM password_entries")
    suspend fun getEntryCount(): Int

    // Delete all entries (for database reset)
    @Query("DELETE FROM password_entries")
    suspend fun deleteAllEntries()

    // Bulk insert entries (for import functionality)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<PasswordEntry>)

    // Get entries with weak passwords (less than 8 characters)
    @Query("SELECT * FROM password_entries WHERE LENGTH(password) < 8 ORDER BY title ASC")
    fun getEntriesWithWeakPasswords(): Flow<List<PasswordEntry>>
}