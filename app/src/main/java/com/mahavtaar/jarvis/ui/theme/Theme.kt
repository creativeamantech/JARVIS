package com.mahavtaar.jarvis.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisPrimary,
    secondary = JarvisSecondary,
    background = JarvisBackground,
    surface = JarvisSurface,
    onPrimary = JarvisBackground,
    onSecondary = JarvisBackground,
    onBackground = JarvisTextPrimary,
    onSurface = JarvisTextPrimary,
    error = JarvisError
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = JarvisColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
