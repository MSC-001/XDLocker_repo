package com.example.xdlocker.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "password_entries")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "url")
    val url: String = "",

    @ColumnInfo(name = "password")
    val password: String, // Will be encrypted by SQLCipher at database level

    @ColumnInfo(name = "tag")
    val tag: String = "",

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false
) {
    // Helper function to get formatted date
    fun getFormattedCreatedDate(): String {
        return java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            .format(Date(createdAt))
    }

    fun getFormattedUpdatedDate(): String {
        return java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
            .format(Date(updatedAt))
    }
}