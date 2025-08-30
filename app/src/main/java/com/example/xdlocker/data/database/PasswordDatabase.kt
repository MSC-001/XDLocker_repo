package com.example.xdlocker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.xdlocker.data.dao.PasswordEntryDao
import com.example.xdlocker.data.entities.PasswordEntry

@Database(
    entities = [PasswordEntry::class],
    version = 1,
    exportSchema = false
)
abstract class PasswordDatabase : RoomDatabase() {

    abstract fun passwordEntryDao(): PasswordEntryDao

    companion object {
        const val DATABASE_NAME = "password_database"

        // Migration from version 1 to 2 (for future use)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration - add new column
                // database.execSQL("ALTER TABLE password_entries ADD COLUMN new_column TEXT DEFAULT ''")
            }
        }

        /**
         * Creates a new encrypted password database instance
         * @param context Application context
         * @param databaseName Custom database filename
         * @param password Encryption password
         */
        fun create(
            context: Context,
            databaseName: String,
            password: String
        ): PasswordDatabase? {
            return try {
                val factory = SQLCipherHelper.createSupportFactory(password)
                Room.databaseBuilder(
                    context.applicationContext,
                    PasswordDatabase::class.java,
                    databaseName
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabaseCallback())
                    .build()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * Opens an existing encrypted password database
         * @param context Application context
         * @param databaseName Existing database filename
         * @param password Encryption password
         */
        fun open(
            context: Context,
            databaseName: String,
            password: String
        ): PasswordDatabase? {
            return try {
                if (!SQLCipherHelper.databaseExists(context, databaseName)) {
                    throw IllegalArgumentException("Database $databaseName does not exist")
                }

                val factory = SQLCipherHelper.createSupportFactory(password)
                Room.databaseBuilder(
                    context.applicationContext,
                    PasswordDatabase::class.java,
                    databaseName
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2)
                    .build()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * Validates if the password can open the database
         * @param context Application context
         * @param databaseName Database filename
         * @param password Password to validate
         */
        fun validatePassword(
            context: Context,
            databaseName: String,
            password: String
        ): Boolean {
            return SQLCipherHelper.validatePassword(context, databaseName, password)
        }
    }

    /**
     * Database callback for initialization and other events
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Database created - can add initial data if needed
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Database opened - can perform cleanup or checks
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            // Destructive migration occurred - handle data loss
        }
    }
}

/**
 * Data class to hold database connection info
 */
data class DatabaseConnection(
    val database: PasswordDatabase,
    val filename: String,
    val label: String
) {
    fun close() {
        if (database.isOpen) {
            database.close()
        }
    }
}

/**
 * Exception thrown when database operations fail
 */
class DatabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when password is incorrect
 */
class InvalidPasswordException(message: String = "Invalid password for database") : Exception(message)