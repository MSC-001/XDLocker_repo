package com.example.xdlocker.utils

/**
 * Utility class for password-related functionality
 */
object PasswordUtils {
    
    /**
     * Data class for password strength information
     */
    data class PasswordStrength(
        val score: Int,
        val label: String
    ) {
        val color: String
            get() = when {
                score >= 80 -> "green"
                score >= 60 -> "blue"
                score >= 40 -> "orange"
                score >= 20 -> "red"
                else -> "gray"
            }

        val progress: Float
            get() = score / 100f
    }

    /**
     * Calculates password strength score (0-100) and returns a PasswordStrength object
     */
    fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength(0, "")

        var score = 0
        
        // Length scoring
        when {
            password.length >= 16 -> score += 25
            password.length >= 12 -> score += 20
            password.length >= 8 -> score += 15
            password.length >= 6 -> score += 10
        }

        // Character variety
        if (password.any { it.isLowerCase() }) score += 5
        if (password.any { it.isUpperCase() }) score += 5
        if (password.any { it.isDigit() }) score += 5
        if (password.any { !it.isLetterOrDigit() }) score += 10

        // Complexity bonus
        val charTypes = listOf(
            password.any { it.isLowerCase() },
            password.any { it.isUpperCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() }
        ).count { it }
        score += charTypes * 10

        // Penalty for common patterns
        if (password.contains("123") || password.contains("abc")) {
            score -= 10
        }

        val strength = when {
            score >= 80 -> "Very Strong"
            score >= 60 -> "Strong"
            score >= 40 -> "Medium"
            score >= 20 -> "Weak"
            else -> "Very Weak"
        }

        return PasswordStrength(minOf(score, 100), strength)
    }

    /**
     * Generates a secure random password
     */
    fun generateSecurePassword(length: Int = 16): String {
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercase = "abcdefghijklmnopqrstuvwxyz"
        val numbers = "0123456789"
        val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        val allChars = uppercase + lowercase + numbers + symbols
        return (1..length).map { allChars.random() }.joinToString("")
    }
}