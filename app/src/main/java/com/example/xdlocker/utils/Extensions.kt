package com.example.xdlocker.utils

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Extension functions and utility methods
 */

// String extensions
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidUrl(): Boolean {
    return android.util.Patterns.WEB_URL.matcher(this).matches()
}

fun String.toSHA256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun String.maskPassword(visibleChars: Int = 4): String {
    return if (length <= visibleChars) {
        "•".repeat(length)
    } else {
        "•".repeat(length - visibleChars) + takeLast(visibleChars)
    }
}

fun String.capitalize(): String {
    return replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

// Date extensions
fun Long.toFormattedDate(pattern: String = "MMM dd, yyyy"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

fun Long.toFormattedDateTime(pattern: String = "MMM dd, yyyy HH:mm"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        diff < 604800_000 -> "${diff / 86400_000} days ago"
        else -> toFormattedDate()
    }
}

// Context extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Context.getAppVersion(): String {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: Constants.APP_VERSION
    } catch (e: PackageManager.NameNotFoundException) {
        Constants.APP_VERSION
    }
}

fun Context.getAppVersionCode(): Long {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.longVersionCode
    } catch (e: PackageManager.NameNotFoundException) {
        1L
    }
}

// Validation extensions
fun String.isStrongPassword(): Boolean {
    val hasMinLength = length >= Constants.MIN_PASSWORD_LENGTH
    val hasUpperCase = any { it.isUpperCase() }
    val hasLowerCase = any { it.isLowerCase() }
    val hasDigit = any { it.isDigit() }
    val hasSpecial = any { !it.isLetterOrDigit() }

    return hasMinLength && hasUpperCase && hasLowerCase && hasDigit && hasSpecial
}

fun String.calculatePasswordStrength(): Int {
    if (isEmpty()) return 0

    var score = 0

    // Length scoring
    score += when {
        length >= 16 -> 25
        length >= 12 -> 20
        length >= 8 -> 15
        length >= 6 -> 10
        else -> 0
    }

    // Character variety
    if (any { it.isLowerCase() }) score += 5
    if (any { it.isUpperCase() }) score += 5
    if (any { it.isDigit() }) score += 5
    if (any { !it.isLetterOrDigit() }) score += 10

    // Complexity bonus
    val charTypes = listOf(
        any { it.isLowerCase() },
        any { it.isUpperCase() },
        any { it.isDigit() },
        any { !it.isLetterOrDigit() }
    ).count { it }
    score += charTypes * 10

    // Penalty for common patterns
    if (contains("123") || contains("abc") || contains("password")) {
        score -= 10
    }

    return minOf(score, 100)
}

// File size formatting
fun Long.formatFileSize(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> "${this / 1024} KB"
        this < 1024 * 1024 * 1024 -> "${this / (1024 * 1024)} MB"
        else -> "${this / (1024 * 1024 * 1024)} GB"
    }
}

// Collection extensions
fun <T> List<T>.safe(index: Int): T? {
    return if (index in 0 until size) this[index] else null
}

fun <T> MutableList<T>.addIfNotExists(item: T) {
    if (!contains(item)) {
        add(item)
    }
}

// Compose utilities
@Composable
fun ShowToastEffect(
    message: String?,
    onMessageShown: () -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            context.showToast(it)
            onMessageShown()
        }
    }
}

@Composable
fun DelayedEffect(
    delayMs: Long,
    action: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(delayMs)
        action()
    }
}

// Security utilities
object SecurityUtils {

    fun generateSalt(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..16)
            .map { chars.random() }
            .joinToString("")
    }

    fun hashWithSalt(input: String, salt: String): String {
        return (input + salt).toSHA256()
    }

    fun generateSecurePassword(
        length: Int = Constants.DEFAULT_PASSWORD_LENGTH,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true,
        excludeSimilar: Boolean = true
    ): String {
        var chars = ""

        if (includeUppercase) chars += Constants.PasswordChars.UPPERCASE
        if (includeLowercase) chars += Constants.PasswordChars.LOWERCASE
        if (includeNumbers) chars += Constants.PasswordChars.NUMBERS
        if (includeSymbols) chars += Constants.PasswordChars.SYMBOLS

        if (excludeSimilar) {
            chars = chars.filterNot { Constants.PasswordChars.SIMILAR_CHARS.contains(it) }
                .joinToString("")
        }

        if (chars.isEmpty()) return ""

        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    fun validateDatabaseName(name: String): String? {
        return when {
            name.isBlank() -> "Database name cannot be empty"
            name.length < Constants.MIN_DATABASE_NAME_LENGTH ->
                "Database name must be at least ${Constants.MIN_DATABASE_NAME_LENGTH} characters"
            name.length > Constants.MAX_DATABASE_NAME_LENGTH ->
                "Database name cannot exceed ${Constants.MAX_DATABASE_NAME_LENGTH} characters"
            name.contains(Regex("[<>:\"/\\|?*]")) ->
                "Database name contains invalid characters"
            else -> null
        }
    }

    fun isWeakPassword(password: String): Boolean {
        return password.length < 8 ||
                !password.any { it.isUpperCase() } ||
                !password.any { it.isLowerCase() } ||
                !password.any { it.isDigit() }
    }
}

// Input validation utilities
object ValidationUtils {

    fun validatePasswordEntry(
        title: String,
        password: String
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (title.isBlank()) {
            errors.add("Title is required")
        }

        if (password.isBlank()) {
            errors.add("Password is required")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    fun validateDatabaseCreation(
        label: String,
        password: String,
        confirmPassword: String
    ): ValidationResult {
        val errors = mutableListOf<String>()

        SecurityUtils.validateDatabaseName(label)?.let { errors.add(it) }

        if (password.length < Constants.MIN_PASSWORD_LENGTH) {
            errors.add("Password must be at least ${Constants.MIN_PASSWORD_LENGTH} characters")
        }

        if (password != confirmPassword) {
            errors.add("Passwords do not match")
        }

        if (password.calculatePasswordStrength() < 30) {
            errors.add("Password is too weak")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()
}

// Debounce utility for search
class Debouncer(private val delayMs: Long = Constants.SEARCH_DEBOUNCE_DELAY) {
    private var debounceJob: kotlinx.coroutines.Job? = null

    fun debounce(
        coroutineScope: kotlinx.coroutines.CoroutineScope,
        action: suspend () -> Unit
    ) {
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(delayMs)
            action()
        }
    }
}

// Clipboard utilities
object ClipboardUtils {

    @Composable
    fun ClearClipboardAfterDelay(
        delayMs: Long = Constants.CLIPBOARD_CLEAR_DELAY
    ) {
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            delay(delayMs)
            // Clear clipboard for security
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE)
                    as android.content.ClipboardManager
            clipboardManager.clearPrimaryClip()
        }
    }
}