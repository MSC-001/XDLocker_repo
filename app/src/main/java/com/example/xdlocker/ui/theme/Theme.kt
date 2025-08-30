package com.example.xdlocker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light theme colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),

    secondary = Color(0xFF43A047),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E8),
    onSecondaryContainer = Color(0xFF1B5E20),

    tertiary = Color(0xFFFF7043),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBE9E7),
    onTertiaryContainer = Color(0xFFBF360C),

    error = Color(0xFFE53935),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),

    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF212121),
    surface = Color.White,
    onSurface = Color(0xFF212121),

    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF616161),
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0)
)

// Dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFE3F2FD),

    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF388E3C),
    onSecondaryContainer = Color(0xFFE8F5E8),

    tertiary = Color(0xFFFFAB91),
    onTertiary = Color(0xFFBF360C),
    tertiaryContainer = Color(0xFFFF5722),
    onTertiaryContainer = Color(0xFFFBE9E7),

    error = Color(0xFFEF5350),
    onError = Color(0xFFB71C1C),
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color(0xFFFFEBEE),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),

    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF616161),
    outlineVariant = Color(0xFF424242)
)

// Database color themes
object DatabaseColors {
    val Blue = Color(0xFF2196F3)
    val Green = Color(0xFF4CAF50)
    val Purple = Color(0xFF9C27B0)
    val Orange = Color(0xFFFF9800)
    val Red = Color(0xFFF44336)
    val Teal = Color(0xFF009688)
    val Pink = Color(0xFFE91E63)
    val Indigo = Color(0xFF3F51B5)

    fun getColor(colorName: String): Color {
        return when (colorName.lowercase()) {
            "blue" -> Blue
            "green" -> Green
            "purple" -> Purple
            "orange" -> Orange
            "red" -> Red
            "teal" -> Teal
            "pink" -> Pink
            "indigo" -> Indigo
            else -> Blue
        }
    }

    val allColors = mapOf(
        "blue" to Blue,
        "green" to Green,
        "purple" to Purple,
        "orange" to Orange,
        "red" to Red,
        "teal" to Teal,
        "pink" to Pink,
        "indigo" to Indigo
    )
}

// Password strength colors
object PasswordStrengthColors {
    val VeryWeak = Color(0xFFE53935)
    val Weak = Color(0xFFFF5722)
    val Medium = Color(0xFFFF9800)
    val Strong = Color(0xFF43A047)
    val VeryStrong = Color(0xFF2E7D32)

    fun getColor(strength: String): Color {
        return when (strength.lowercase()) {
            "very weak" -> VeryWeak
            "weak" -> Weak
            "medium" -> Medium
            "strong" -> Strong
            "very strong" -> VeryStrong
            else -> Medium
        }
    }
}

@Composable
fun XDLockerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}