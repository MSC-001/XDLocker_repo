package com.example.xdlocker.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_databases")
data class UserDatabaseInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "database_label")
    val databaseLabel: String, // User-assigned name like "Work Passwords"

    @ColumnInfo(name = "database_filename")
    val databaseFilename: String, // Unique internal filename like "db_12345.sqlite"

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "entry_count")
    val entryCount: Int = 0, // Cache of password entries count for UI

    @ColumnInfo(name = "color_theme")
    val colorTheme: String = "blue", // For visual distinction in UI

    @ColumnInfo(name = "description")
    val description: String = "" // Optional description
) {
    // Helper function to get formatted dates
    fun getFormattedCreatedDate(): String {
        return java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            .format(Date(createdAt))
    }

    fun getFormattedLastAccessed(): String {
        return java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
            .format(Date(lastAccessed))
    }

    // Generate unique filename for new databases
    companion object {
        fun generateUniqueFilename(): String {
            val timestamp = System.currentTimeMillis()
            val random = (1000..9999).random()
            return "db_${timestamp}_$random.sqlite"
        }

        // Predefined color themes for databases
        val availableColorThemes = listOf(
            "blue", "green", "purple", "orange", "red", "teal", "pink", "indigo"
        )
    }
}