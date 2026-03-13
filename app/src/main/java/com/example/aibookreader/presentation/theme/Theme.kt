package com.example.aibookreader.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = OnPrimary,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    error = Error
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    onPrimary = OnPrimary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    error = Error
)

// Специальная схема для ридера
private val ReaderLightColorScheme = lightColorScheme(
    primary = Primary,
    background = ReadingBackgroundLight,
    surface = ReadingSurfaceLight,
    onBackground = ReadingTextLight,
    onSurface = ReadingTextLight
)

private val ReaderDarkColorScheme = darkColorScheme(
    primary = Primary,
    background = ReadingBackgroundDark,
    surface = ReadingSurfaceDark,
    onBackground = ReadingTextDark,
    onSurface = ReadingTextDark
)

@Composable
fun AIBookReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    isReaderMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isReaderMode -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isReaderMode && darkTheme -> ReaderDarkColorScheme
        isReaderMode && !darkTheme -> ReaderLightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // В режиме ридера цвет статус-бара сливается с фоном
            window.statusBarColor = if (isReaderMode) colorScheme.surface.toArgb() else colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}