package com.example.xdlocker.utils

/**
 * App-wide constants
 */
object Constants {

    // Database
    const val METADATA_DATABASE_NAME = "metadata_database.db"
    const val DATABASE_VERSION = 1

    // Security
    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_PIN_LENGTH = 4
    const val MAX_PIN_LENGTH = 8
    const val MIN_DATABASE_NAME_LENGTH = 3
    const val MAX_DATABASE_NAME_LENGTH = 50

    // Auto-lock timeouts (in minutes)
    val AUTO_LOCK_TIMEOUTS = listOf(1, 2, 5, 10, 15, 30)
    const val DEFAULT_AUTO_LOCK_TIMEOUT = 5

    // Password generation
    const val DEFAULT_PASSWORD_LENGTH = 16
    const val MIN_PASSWORD_GEN_LENGTH = 4
    const val MAX_PASSWORD_GEN_LENGTH = 128

    // UI
    const val SEARCH_DEBOUNCE_DELAY = 300L
    const val CLIPBOARD_CLEAR_DELAY = 30000L // 30 seconds
    const val SPLASH_SCREEN_DURATION = 1500L

    // File operations
    const val EXPORT_FILE_EXTENSION = ".xdlocker"
    const val EXPORT_MIME_TYPE = "application/octet-stream"

    // Preferences keys
    object PreferenceKeys {
        const val IS_DARK_THEME = "is_dark_theme"
        const val IS_APP_LOCK_ENABLED = "is_app_lock_enabled"
        const val IS_BIOMETRIC_ENABLED = "is_biometric_enabled"
        const val AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        const val DEFAULT_SORT_ORDER = "default_sort_order"
        const val APP_PIN_HASH = "app_pin_hash"
        const val LAST_APP_CLOSE_TIME = "last_app_close_time"
        const val IS_FIRST_LAUNCH = "is_first_launch"
    }

    // Error messages
    object ErrorMessages {
        const val DATABASE_NOT_FOUND = "Database not found"
        const val INVALID_PASSWORD = "Invalid password"
        const val DATABASE_CREATION_FAILED = "Failed to create database"
        const val DATABASE_OPEN_FAILED = "Failed to open database"
        const val ENTRY_SAVE_FAILED = "Failed to save password entry"
        const val ENTRY_DELETE_FAILED = "Failed to delete password entry"
        const val NETWORK_ERROR = "Network error occurred"
        const val UNKNOWN_ERROR = "An unknown error occurred"
        const val LABEL_ALREADY_EXISTS = "Database name already exists"
        const val WEAK_PASSWORD = "Password is too weak"
        const val PASSWORDS_DONT_MATCH = "Passwords do not match"
        const val PIN_TOO_SHORT = "PIN must be at least 4 digits"
        const val INVALID_PIN = "Invalid PIN"
    }

    // Success messages
    object SuccessMessages {
        const val DATABASE_CREATED = "Database created successfully"
        const val DATABASE_DELETED = "Database deleted successfully"
        const val PASSWORD_COPIED = "Password copied to clipboard"
        const val USERNAME_COPIED = "Username copied to clipboard"
        const val ENTRY_SAVED = "Password entry saved"
        const val ENTRY_DELETED = "Password entry deleted"
        const val SETTINGS_SAVED = "Settings saved"
        const val APP_LOCK_ENABLED = "App lock enabled"
        const val DATA_EXPORTED = "Data exported successfully"
        const val DATA_IMPORTED = "Data imported successfully"
    }

    // Regular expressions
    object Regex {
        const val URL_PATTERN = "^(https?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(/.*)?$"
        const val EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    }

    // Password generation character sets
    object PasswordChars {
        const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
        const val NUMBERS = "0123456789"
        const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        const val SIMILAR_CHARS = "il1Lo0O" // Characters to exclude when requested
    }

    // App metadata
    const val APP_NAME = "XDLocker"
    const val APP_VERSION = "1.0.0"
    const val DEVELOPER_NAME = "Your Name"
    const val SUPPORT_EMAIL = "support@xdlocker.app"

    // Deep link schemes
    const val DEEP_LINK_SCHEME = "xdlocker"
    const val DEEP_LINK_HOST = "app"
}