package com.example.xdlocker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.xdlocker.data.dao.UserDatabaseInfoDao
import com.example.xdlocker.data.entities.UserDatabaseInfo

@Database(
    entities = [UserDatabaseInfo::class],
    version = 1,
    exportSchema = false
)
abstract class MetadataDatabase : RoomDatabase() {

    abstract fun userDatabaseInfoDao(): UserDatabaseInfoDao

    companion object {
        const val DATABASE_NAME = "metadata_database.db"

        @Volatile
        private var INSTANCE: MetadataDatabase? = null

        // Migration from version 1 to 2 (for future use)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration
                // database.execSQL("ALTER TABLE user_databases ADD COLUMN new_field TEXT DEFAULT ''")
            }
        }

        /**
         * Gets the singleton instance of MetadataDatabase
         * @param context Application context
         * @param encrypted Whether to encrypt the metadata database (optional, for future use)
         */
        fun getDatabase(context: Context, encrypted: Boolean = false): MetadataDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = if (encrypted) {
                    // Future: Add encryption for metadata database with app-level password
                    buildEncryptedDatabase(context)
                } else {
                    buildUnencryptedDatabase(context)
                }
                INSTANCE = instance
                instance
            }
        }

        /**
         * Builds an unencrypted metadata database
         */
        private fun buildUnencryptedDatabase(context: Context): MetadataDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MetadataDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(MetadataCallback())
                .build()
        }

        /**
         * Builds an encrypted metadata database (for future use)
         */
        private fun buildEncryptedDatabase(context: Context): MetadataDatabase {
            // For future implementation with app-level password
            // val factory = SQLCipherHelper.createSupportFactory(appPassword)
            return Room.databaseBuilder(
                context.applicationContext,
                MetadataDatabase::class.java,
                DATABASE_NAME
            )
                // .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .addCallback(MetadataCallback())
                .build()
        }

        /**
         * Clears the database instance (for testing)
         */
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

        /**
         * Resets the metadata database (removes all data)
         */
        suspend fun resetDatabase(context: Context) {
            val db = getDatabase(context)
            db.clearAllTables()
        }
    }

    /**
     * Callback for metadata database events
     */
    private class MetadataCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Metadata database created
            // Could add default settings or initial data here
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Perform any cleanup or initialization on database open
        }
    }
}

/**
 * Extension functions for MetadataDatabase
 */

/**
 * Safely executes a database operation with error handling
 */
suspend fun <T> MetadataDatabase.safeOperation(operation: suspend () -> T): Result<T> {
    return try {
        Result.success(operation())
    } catch (e: Exception) {
        Result.failure(DatabaseException("Metadata database operation failed", e))
    }
}

/**
 * Checks if the metadata database is accessible
 */
fun MetadataDatabase.isAccessible(): Boolean {
    return try {
        this.isOpen && this.openHelper.readableDatabase != null
    } catch (e: Exception) {
        false
    }
}