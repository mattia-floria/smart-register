package com.afloria.smartregister.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    fontFamily: String = "DEFAULT",
    fontWeight: Float = 400f,
    fontWidth: Float = 100f,
    fontOpsz: Float = 14f,
    fontGrad: Float = 0f,
    fontRond: Float = 0f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    
    val selectedFont = remember(fontFamily, fontWeight, fontWidth, fontOpsz, fontGrad, fontRond) {
        if (fontFamily == "GOOGLE_SANS") {
            getGoogleSansFlex(fontWeight, fontWidth, fontOpsz, fontGrad, fontRond)
        } else {
            androidx.compose.ui.text.font.FontFamily.Default
        }
    }
    val dynamicTypography = remember(selectedFont) {
        getTypography(selectedFont)
    }
    
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.PURE_BLACK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    val colorScheme = when {
        seedColor == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        seedColor != null -> {
            if (useDarkTheme) {
                darkColorScheme(
                    primary = seedColor,
                    secondary = secondaryColor ?: seedColor,
                    tertiary = tertiaryColor ?: seedColor,
                    surface = if (themeMode == ThemeMode.PURE_BLACK) Color.Black else Color(0xFF1A1C1E),
                    background = if (themeMode == ThemeMode.PURE_BLACK) Color.Black else Color(0xFF1A1C1E),
                )
            } else {
                lightColorScheme(
                    primary = seedColor,
                    secondary = secondaryColor ?: seedColor,
                    tertiary = tertiaryColor ?: seedColor
                )
            }
        }
        useDarkTheme -> darkColorScheme(
            primary = PrimaryExpressiveDark,
            secondary = SecondaryExpressiveDark,
            tertiary = TertiaryExpressiveDark,
            surface = if (themeMode == ThemeMode.PURE_BLACK) Color.Black else Color(0xFF1A1C1E),
            background = if (themeMode == ThemeMode.PURE_BLACK) Color.Black else Color(0xFF1A1C1E),
            onSurface = Color.White,
            onBackground = Color.White,
            onSurfaceVariant = Color(0xFFCAC4D0)
        )
        else -> lightColorScheme(
            primary = PrimaryExpressiveLight,
            secondary = SecondaryExpressiveLight,
            tertiary = TertiaryExpressiveLight
        )
    }

    // Apply M3 Expressive Tonal Surface Container Tokens and ensure Pure Black background
    val finalColorScheme = if (useDarkTheme) {
        val isBlack = themeMode == ThemeMode.PURE_BLACK
        colorScheme.copy(
            surface = if (isBlack) Color.Black else Color(0xFF1A1C1E),
            background = if (isBlack) Color.Black else Color(0xFF1A1C1E),
            onSurface = Color.White,
            onBackground = Color.White,
            surfaceContainerLowest = if (isBlack) Color.Black else SurfaceContainerLowestDark,
            surfaceContainerLow = if (isBlack) Color(0xFF080808) else SurfaceContainerLowDark,
            surfaceContainer = if (isBlack) Color(0xFF0C0C0C) else SurfaceContainerDark,
            surfaceContainerHigh = if (isBlack) Color(0xFF121212) else SurfaceContainerHighDark,
            surfaceContainerHighest = if (isBlack) Color(0xFF1A1A1A) else SurfaceContainerHighestDark
        )
    } else {
        colorScheme.copy(
            surfaceContainerLowest = SurfaceContainerLowestLight,
            surfaceContainerLow = SurfaceContainerLowLight,
            surfaceContainer = SurfaceContainerLight,
            surfaceContainerHigh = SurfaceContainerHighLight,
            surfaceContainerHighest = SurfaceContainerHighestLight
        )
    }

    MaterialTheme(
        colorScheme = finalColorScheme,
        typography = dynamicTypography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    SmartRegisterTheme(content = content)
}
