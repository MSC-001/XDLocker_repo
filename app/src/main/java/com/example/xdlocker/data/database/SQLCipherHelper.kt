package com.example.xdlocker.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
// import androidx.sqlite.db.SupportSQLiteOpenHelper // This import is not strictly necessary if not directly used.
import net.sqlcipher.database.SupportFactory // <- UPDATED IMPORT

object SQLCipherHelper {

    /*
     Creates a SupportFactory for SQLCipher encryption
      //@param password The encryption password for the database
     */
    fun createSupportFactory(password: String): SupportFactory {
        val passphrase = password.toByteArray()
        return SupportFactory(passphrase)
    }

    /**
     * Validates if a password can decrypt the database
     * @param context Application context
     * @param databaseName Name of the database file
     * @param password Password to test
     */
    fun validatePassword(context: Context, databaseName: String, password: String): Boolean {
        return try {
            val factory = createSupportFactory(password)
            val tempDb = Room.databaseBuilder(
                context,
                PasswordDatabase::class.java,
                databaseName
            )
                .openHelperFactory(factory)
                .allowMainThreadQueries() // Only for validation
                .build()

            // Try to perform a simple query
            // Note: tempDb.passwordEntryDao().getEntryCount() was flagged as a suspend function call.
            // This is a separate issue we should address if it persists after dependency fixes.
            // For now, let's assume it works or will be fixed.
            tempDb.query("SELECT COUNT(*) FROM password_entries", null) // A simple raw query
            tempDb.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Changes the password of an encrypted database
     * @param context Application context
     * @param databaseName Name of the database file
     * @param oldPassword Current password
     * @param newPassword New password
     */
    fun changePassword(
        context: Context,
        databaseName: String,
        oldPassword: String,
        newPassword: String
    ): Boolean {
        return try {
            val oldFactory = createSupportFactory(oldPassword)
            val database = Room.databaseBuilder(
                context,
                PasswordDatabase::class.java,
                databaseName
            )
                .openHelperFactory(oldFactory)
                .build()

            // Get writable database and change password
            val writableDb = database.openHelper.writableDatabase // This is SupportSQLiteDatabase
            writableDb.execSQL("PRAGMA rekey = '${newPassword.replace("'", "''")}'")

            database.close()

            // Verify the new password works
            validatePassword(context, databaseName, newPassword)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Creates an encrypted database with initial setup
     * @param context Application context
     * @param databaseName Name of the database file
     * @param password Encryption password
     */
    fun createEncryptedDatabase(
        context: Context,
        databaseName: String,
        password: String
    ): PasswordDatabase? {
        return try {
            val factory = createSupportFactory(password)
            Room.databaseBuilder(
                context,
                PasswordDatabase::class.java,
                databaseName
            )
                .openHelperFactory(factory)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Database created successfully
                    }
                })
                .build()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Opens an existing encrypted database
     * @param context Application context
     * @param databaseName Name of the database file
     * @param password Encryption password
     */
    fun openEncryptedDatabase(
        context: Context,
        databaseName: String,
        password: String
    ): PasswordDatabase? {
        return try {
            val factory = createSupportFactory(password)
            Room.databaseBuilder(
                context,
                PasswordDatabase::class.java,
                databaseName
            )
                .openHelperFactory(factory)
                .build()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deletes a database file
     * @param context Application context
     * @param databaseName Name of the database file
     */
    fun deleteDatabase(context: Context, databaseName: String): Boolean {
        return try {
            context.deleteDatabase(databaseName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a database file exists
     * @param context Application context
     * @param databaseName Name of the database file
     */
    fun databaseExists(context: Context, databaseName: String): Boolean {
        return context.getDatabasePath(databaseName).exists()
    }

    /**
     * Gets the size of a database file in bytes
     * @param context Application context
     * @param databaseName Name of the database file
     */
    fun getDatabaseSize(context: Context, databaseName: String): Long {
        val dbFile = context.getDatabasePath(databaseName)
        return if (dbFile.exists()) dbFile.length() else 0L
    }

    /**
     * Formats database size in human readable format
     */
    fun formatDatabaseSize(sizeInBytes: Long): String {
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
            else -> "${sizeInBytes / (1024 * 1024)} MB"
        }
    }
}
