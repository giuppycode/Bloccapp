package com.example.bloccapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.bloccapp.data.preferences.ThemeMode

private val LightColors = lightColorScheme()
private val DarkColors  = darkColorScheme()

/**
 * Tema principale dell'applicazione.
 * Applica il ColorScheme corretto in base al [ThemeMode] corrente.
 */
@Composable
fun BloccappTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content
    )
}
