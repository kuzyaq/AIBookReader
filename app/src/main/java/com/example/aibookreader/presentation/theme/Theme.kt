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

// Тёмная тема приложения
private val AppDarkColorScheme = darkColorScheme(
    primary = PurpleLight,
    onPrimary = NeutralWhite,
    primaryContainer = PurpleDark,
    onPrimaryContainer = PurpleContainer,
    secondary = Emerald,
    onSecondary = Neutral900,
    secondaryContainer = EmeraldDark,
    onSecondaryContainer = EmeraldContainer,
    background = Color(0xFF121018),
    onBackground = Color(0xFFE6E1F0),
    surface = Color(0xFF1E1C2A),
    onSurface = Color(0xFFE6E1F0),
    surfaceVariant = Color(0xFF302E3F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF958DA5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

// Светлая тема приложения
private val AppLightColorScheme = lightColorScheme(
    primary = Purple,
    onPrimary = NeutralWhite,
    primaryContainer = PurpleContainer,
    onPrimaryContainer = OnPurpleContainer,
    secondary = Emerald,
    onSecondary = NeutralWhite,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = OnEmeraldContainer,
    background = Neutral100,
    onBackground = Neutral900,
    surface = NeutralWhite,
    onSurface = Neutral900,
    surfaceVariant = Color(0xFFF0EEFF),
    onSurfaceVariant = Neutral700,
    outline = Color(0xFF79757F),
    error = ErrorRed,
    onError = NeutralWhite,
)

// Светлая тема ридера
private val ReaderLightColorScheme = lightColorScheme(
    primary = Purple,
    onPrimary = NeutralWhite,
    primaryContainer = PurpleContainer,
    onPrimaryContainer = OnPurpleContainer,
    secondary = Emerald,
    onSecondary = NeutralWhite,
    secondaryContainer = Color(0xFFCFC8E8),
    onSecondaryContainer = Color(0xFF1A1528),
    background = ReaderBgLight,
    onBackground = ReaderTextLight,
    surface = ReaderSurfaceLight,
    onSurface = ReaderTextLight,
    surfaceVariant = Color(0xFFF0EEFF),
    onSurfaceVariant = Color(0xFF6B6680),
)

//Тёмная тема ридера
private val ReaderDarkColorScheme = darkColorScheme(
    primary = PurpleLight,
    onPrimary = NeutralWhite,
    primaryContainer = PurpleDark,
    onPrimaryContainer = PurpleContainer,
    secondary = EmeraldLight,
    onSecondary = Neutral900,
    secondaryContainer = Color(0xFF3D3A55),
    onSecondaryContainer = Color(0xFFE8E4F5),
    background = ReaderBgDark,
    onBackground = ReaderTextDark,
    surface = ReaderSurfaceDark,
    onSurface = ReaderTextDark,
    surfaceVariant = Color(0xFF2E2C40),
    onSurfaceVariant = Color(0xFFCAC4D0),
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
        darkTheme -> AppDarkColorScheme
        else -> AppLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (isReaderMode)
                colorScheme.background.toArgb()
            else
                colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}