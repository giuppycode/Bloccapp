package com.example.bloccapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloccapp.data.preferences.ThemeMode
import com.example.bloccapp.data.preferences.ThemePreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel che espone la preferenza del tema come [StateFlow] e permette di cambiarla.
 *
 * Usa [AndroidViewModel] per accedere al Context dell'applicazione in modo sicuro
 * (non tiene un riferimento a un Activity/Fragment).
 */
class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = ThemePreferencesManager(application.applicationContext)

    /**
     * Stato corrente del tema.
     * Il valore iniziale è [ThemeMode.SYSTEM] fino alla prima emissione del DataStore.
     */
    val themeMode: StateFlow<ThemeMode> = preferencesManager.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM
        )

    /** Persiste la nuova modalità tema. */
    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }
}
