package com.example.bloccapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Modalità del tema dell'applicazione. */
enum class ThemeMode {
    LIGHT,   // Tema chiaro
    DARK,    // Tema scuro
    SYSTEM   // Segue le impostazioni di sistema (default)
}

// Estensione singleton del DataStore, legata al Context dell'applicazione.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Gestisce le preferenze persistenti del tema tramite DataStore.
 *
 * Utilizzo:
 * ```
 * val manager = ThemePreferencesManager(context)
 * manager.themeMode.collect { mode -> ... }
 * manager.setThemeMode(ThemeMode.DARK)
 * ```
 */
class ThemePreferencesManager(private val context: Context) {

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    /**
     * Flow che emette il [ThemeMode] corrente.
     * Emette [ThemeMode.SYSTEM] se non è ancora stato impostato un valore.
     */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(raw)
    }

    /** Persiste la modalità tema scelta. */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }
}
