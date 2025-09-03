package com.example.xdlocker.data.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

object SQLCipherHelper {

    /*
     Creates a SupportFactory for SQLCipher encryption
     */
    fun createSupportFactory(password: String): SupportOpenHelperFactory {
        val passphrase = password.toByteArray()
        return SupportOpenHelperFactory(passphrase)
    }

    /**
     * Validates if a password can decrypt the database
     * @param context Application context
     * @param databaseName Name of the database file
     * @param password Password to test
     */
    fun validatePassword(context: Context, databaseName: String, password: String): Boolean {
        Log.d("SQLCipherHelper", "Validating password for database: $databaseName")
        return try {
            val factory = createSupportFactory(password)
            val tempDb = Room.databaseBuilder(
                context,
                PasswordDatabase::class.java,
                databaseName
            )
                .openHelperFactory(factory)
                .allowMainThreadQueries()
                .build()

            // Try to perform a simple query
            tempDb.query("SELECT COUNT(*) FROM password_entries", null) // A simple raw query
            tempDb.close()
            Log.i("SQLCipherHelper", "Password validation successful for $databaseName")
            true
        } catch (e: Exception) {
            Log.w("SQLCipherHelper", "Password validation failed for $databaseName: ${e.message}")
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
        Log.d("SQLCipherHelper", "Attempting to change password for database: $databaseName")
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
            Log.i("SQLCipherHelper", "PRAGMA rekey executed for $databaseName")

            database.close()

            // Verify the new password works
            val verificationResult = validatePassword(context, databaseName, newPassword)
            if (verificationResult) {
                Log.i("SQLCipherHelper", "Password change successful and verified for $databaseName")
            } else {
                Log.w("SQLCipherHelper", "Password change rekey seemed to work, but new password validation FAILED for $databaseName")
            }
            verificationResult
        } catch (e: Exception) {
            Log.e("SQLCipherHelper", "Error changing password for $databaseName: ${e.message}", e)
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
    ): PasswordDatabase? { // Assuming PasswordDatabase is your Room DB class
        Log.d("SQLCipherHelper", "Attempting to create encrypted database: $databaseName")
        return try {
            val factory = createSupportFactory(password)
            val db = Room.databaseBuilder(
                context,
                PasswordDatabase::class.java, // Assuming PasswordDatabase is your Room DB class
                databaseName
            )
                .openHelperFactory(factory)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Log.i("SQLCipherHelper", "Encrypted database $databaseName created successfully (onCreate callback).")
                    }
                })
                .build()
            // To ensure it's created, maybe do a dummy operation or just rely on build()
            db.query("SELECT 1", null) // force open
            Log.i("SQLCipherHelper", "Encrypted database $databaseName instance built.")
            db
        } catch (e: Exception) {
            Log.e("SQLCipherHelper", "Error creating encrypted database $databaseName: ${e.message}", e)
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
    ): PasswordDatabase? { // Assuming PasswordDatabase is your Room DB class
        Log.d("SQLCipherHelper", "Attempting to open encrypted database: $databaseName")
        return try {
            val factory = createSupportFactory(password)
            val db = Room.databaseBuilder(
                context,
                PasswordDatabase::class.java, // Assuming PasswordDatabase is your Room DB class
                databaseName
            )
                .openHelperFactory(factory)
                .build()
            // To ensure it's opened, maybe do a dummy operation
            db.query("SELECT 1", null) // force open
            Log.i("SQLCipherHelper", "Encrypted database $databaseName opened successfully.")
            db
        } catch (e: Exception) {
            Log.e("SQLCipherHelper", "Error opening encrypted database $databaseName: ${e.message}", e)
            null
        }
    }

    /**
     * Deletes a database file
     * @param context Application context
     * @param databaseName Name of the database file
     */
    fun deleteDatabase(context: Context, dbFilename: String): Boolean {
        Log.d("SQLCipherHelper", "Attempting to delete database: $dbFilename")
        val dbFile = context.getDatabasePath(dbFilename)
        var mainDbDeleted = false

        if (dbFile.exists()) {
            Log.d("SQLCipherHelper", "Database file exists at: ${dbFile.absolutePath}")
            // Try to delete the main database file
            mainDbDeleted = context.deleteDatabase(dbFilename)
            if (mainDbDeleted) {
                Log.i("SQLCipherHelper", "Successfully deleted main database file: $dbFilename using context.deleteDatabase()")
            } else {
                Log.e("SQLCipherHelper", "Failed to delete main database file: $dbFilename using context.deleteDatabase()")
                // Try a direct delete as a fallback, though context.deleteDatabase should handle it
                if (dbFile.delete()) {
                    Log.i("SQLCipherHelper", "Successfully deleted main database file: $dbFilename using direct dbFile.delete()")
                    mainDbDeleted = true // Consider it deleted if direct delete works
                } else {
                    Log.e("SQLCipherHelper", "Failed to delete main database file: $dbFilename using direct dbFile.delete() as well.")
                }
            }

            // SQLCipher also creates journal files, and potentially -wal, -shm files for WAL mode
            val journalFile = File(dbFile.path + "-journal")
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")

            var auxFilesDeleted = true // Assume true, set to false if any aux file fails

            if (journalFile.exists()) {
                Log.d("SQLCipherHelper", "Journal file exists: ${journalFile.path}")
                if (!journalFile.delete()) {
                    Log.w("SQLCipherHelper", "Failed to delete journal file: ${journalFile.path}")
                    auxFilesDeleted = false
                } else {
                    Log.i("SQLCipherHelper", "Successfully deleted journal file: ${journalFile.path}")
                }
            } else {
                Log.d("SQLCipherHelper", "Journal file not found: ${journalFile.path}")
            }

            if (walFile.exists()) {
                Log.d("SQLCipherHelper", "WAL file exists: ${walFile.path}")
                if (!walFile.delete()) {
                    Log.w("SQLCipherHelper", "Failed to delete WAL file: ${walFile.path}")
                    auxFilesDeleted = false
                } else {
                    Log.i("SQLCipherHelper", "Successfully deleted WAL file: ${walFile.path}")
                }
            } else {
                Log.d("SQLCipherHelper", "WAL file not found: ${walFile.path}")
            }

            if (shmFile.exists()) {
                Log.d("SQLCipherHelper", "SHM file exists: ${shmFile.path}")
                if (!shmFile.delete()) {
                    Log.w("SQLCipherHelper", "Failed to delete SHM file: ${shmFile.path}")
                    auxFilesDeleted = false
                } else {
                    Log.i("SQLCipherHelper", "Successfully deleted SHM file: ${shmFile.path}")
                }
            } else {
                Log.d("SQLCipherHelper", "SHM file not found: ${shmFile.path}")
            }

            val overallSuccess = mainDbDeleted && auxFilesDeleted
            Log.d("SQLCipherHelper", "Overall deletion result for $dbFilename: $overallSuccess (MainDB: $mainDbDeleted, AuxFiles: $auxFilesDeleted)")
            return overallSuccess
        } else {
            Log.i("SQLCipherHelper", "Database file $dbFilename not found at ${dbFile.absolutePath}, nothing to delete (considered success).")
            return true // File not existing is a success state for deletion.
        }
    }


    /**
     * Checks if a database file exists
     * @param context Application context
     * @param databaseName Name of the database file
     */
    fun databaseExists(context: Context, databaseName: String): Boolean {
        val exists = context.getDatabasePath(databaseName).exists()
        Log.d("SQLCipherHelper", "Database $databaseName exists: $exists")
        return exists
    }

    /**
     * Gets the size of a database file in bytes
     * @param context Application context
     * @param databaseName Name of the database file
     */
    fun getDatabaseSize(context: Context, databaseName: String): Long {
        val dbFile = context.getDatabasePath(databaseName)
        val size = if (dbFile.exists()) dbFile.length() else 0L
        Log.d("SQLCipherHelper", "Database $databaseName size: $size bytes")
        return size
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

