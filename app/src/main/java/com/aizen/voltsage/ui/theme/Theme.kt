package com.aizen.voltsage.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VoltCyan,
    onPrimary = Color(0xFF0B132B),
    primaryContainer = VoltBlue,
    onPrimaryContainer = Color(0xFFD7E3FF),
    secondary = VoltAmber,
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5B4300),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = VoltNeonBlue,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = VoltNeonBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = VoltBlue,
    secondary = VoltAmber,
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFFFFE082),
    onSecondaryContainer = Color(0xFF261900),
    tertiary = VoltCyan,
    onTertiary = Color(0xFF0B132B),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    amoledTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme && amoledTheme -> {
            DarkColorScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceVariant = Color(0xFF161616)
            )
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun VoltSageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    amoledTheme: Boolean = false,
    content: @Composable () -> Unit,
) = MyApplicationTheme(darkTheme, dynamicColor, amoledTheme, content)

