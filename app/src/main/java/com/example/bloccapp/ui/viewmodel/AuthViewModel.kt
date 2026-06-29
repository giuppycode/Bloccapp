package com.example.bloccapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloccapp.data.preferences.AuthPreferencesManager
import com.example.bloccapp.data.preferences.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Stato UI della schermata di autenticazione. */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String?     = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = AuthPreferencesManager(application.applicationContext)

    /** True se l'utente è già loggato (persiste al riavvio). */
    val isLoggedIn: StateFlow<Boolean> = prefsManager.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val userInfo: StateFlow<UserInfo?> = prefsManager.userInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ---- Validazione --------------------------------------------------------

    private fun validateEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun validatePassword(password: String): Boolean =
        password.length >= 6

    // ---- Azioni pubbliche ---------------------------------------------------

    fun login(email: String, password: String) {
        if (!validateEmail(email)) {
            _uiState.value = AuthUiState(error = "Email non valida")
            return
        }
        if (!validatePassword(password)) {
            _uiState.value = AuthUiState(error = "La password deve avere almeno 6 caratteri")
            return
        }
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            // Simulazione auth locale: login sempre riuscito se le credenziali sono valide
            val displayName = email.substringBefore("@")
                .replaceFirstChar { it.uppercaseChar() }
            prefsManager.saveLogin(email = email, displayName = displayName)
            _uiState.value = AuthUiState()
        }
    }

    fun register(email: String, password: String, displayName: String) {
        val name = displayName.trim()
        if (name.isBlank()) {
            _uiState.value = AuthUiState(error = "Inserisci un nome")
            return
        }
        if (!validateEmail(email)) {
            _uiState.value = AuthUiState(error = "Email non valida")
            return
        }
        if (!validatePassword(password)) {
            _uiState.value = AuthUiState(error = "La password deve avere almeno 6 caratteri")
            return
        }
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            prefsManager.saveLogin(email = email, displayName = name)
            _uiState.value = AuthUiState()
        }
    }

    /** Simula un login con Google usando un account di test. */
    fun loginWithGoogle() {
        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            prefsManager.saveLogin(
                email       = "utente.google@gmail.com",
                displayName = "Utente Google"
            )
            _uiState.value = AuthUiState()
        }
    }

    fun logout() {
        viewModelScope.launch {
            prefsManager.logout()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
