package com.afloria.smartregister.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, PURE_BLACK
}

@Composable
fun SmartRegisterTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    seedColor: Color? = null,
    secondaryColor: Color? = null,
    tertiaryColor: Color? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.PURE_BLACK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    val colorScheme = when {
        // Dynamic colors from system (Palette icon selected)
        seedColor == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (themeMode == ThemeMode.PURE_BLACK && useDarkTheme) {
                base.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF111111),
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onSurfaceVariant = Color(0xFFC4C7C5),
                    outline = Color(0xFF8E918F)
                )
            } else base
        }
        
        // Custom colors from palette selection
        seedColor != null -> {
            val primary = if (useDarkTheme) (secondaryColor ?: seedColor) else seedColor
            val onPrimary = if (useDarkTheme) (tertiaryColor ?: Color.Black) else Color.White
            val primaryContainer = if (useDarkTheme) (tertiaryColor ?: seedColor) else (secondaryColor ?: seedColor.copy(alpha = 0.1f))
            val onPrimaryContainer = if (useDarkTheme) (secondaryColor ?: Color.White) else (tertiaryColor ?: seedColor)
            
            val secondary = secondaryColor ?: seedColor
            val tertiary = tertiaryColor ?: seedColor

            if (useDarkTheme) {
                darkColorScheme(
                    primary = primary,
                    onPrimary = onPrimary,
                    primaryContainer = primaryContainer,
                    onPrimaryContainer = onPrimaryContainer,
                    secondary = secondary,
                    onSecondary = tertiary,
                    secondaryContainer = seedColor,
                    onSecondaryContainer = secondary,
                    tertiary = tertiary,
                    background = if (themeMode == ThemeMode.PURE_BLACK) Color.Black else Color(0xFF1C1B1F),
                    surface = if (themeMode == ThemeMode.PURE_BLACK) Color.Black else Color(0xFF1C1B1F),
                    onBackground = Color.White,
                    onSurface = Color.White,
                    surfaceVariant = if (themeMode == ThemeMode.PURE_BLACK) Color(0xFF111111) else Color(0xFF444746),
                    onSurfaceVariant = Color(0xFFC4C7C5),
                    outline = Color(0xFF8E918F)
                )
            } else {
                lightColorScheme(
                    primary = primary,
                    onPrimary = onPrimary,
                    primaryContainer = primaryContainer,
                    onPrimaryContainer = onPrimaryContainer,
                    secondary = secondary,
                    onSecondary = Color.White,
                    secondaryContainer = secondary.copy(alpha = 0.2f),
                    tertiary = tertiary,
                    onBackground = Color(0xFF1C1B1F),
                    onSurface = Color(0xFF1C1B1F),
                    onSurfaceVariant = Color(0xFF444746),
                    outline = Color(0xFF747775)
                )
            }
        }

        useDarkTheme -> {
            val base = darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)
            if (themeMode == ThemeMode.PURE_BLACK) {
                base.copy(
                    background = Color.Black, 
                    surface = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onSurfaceVariant = Color(0xFFC4C7C5),
                    outline = Color(0xFF8E918F)
                )
            } else base
        }
        else -> lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    SmartRegisterTheme(content = content)
}
