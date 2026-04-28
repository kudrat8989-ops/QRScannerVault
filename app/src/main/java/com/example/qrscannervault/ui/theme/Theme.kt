package com.example.qrscannervault.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Цвета для Тёмной темы (глубокий синий/черный)
private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFD0BCFF),
    secondary = androidx.compose.ui.graphics.Color(0xFFCCC2DC),
    tertiary = androidx.compose.ui.graphics.Color(0xFFEFB8C8),
    background = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
)

// Цвета для Светлой темы
private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6750A4),
    secondary = androidx.compose.ui.graphics.Color(0xFF625b71),
    tertiary = androidx.compose.ui.graphics.Color(0xFF7D5260),
    background = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
)

@Composable
fun QRScannerVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Динамические цвета (доступны на Android 12+)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}