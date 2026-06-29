package com.example.bloccapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Informazioni sull'utente corrente. */
data class UserInfo(
    val email: String,
    val displayName: String
)

private val Context.authDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "auth_preferences")

/**
 * Gestisce lo stato di autenticazione tramite DataStore.
 * Persiste: isLoggedIn, email, displayName.
 */
class AuthPreferencesManager(private val context: Context) {

    companion object {
        private val KEY_IS_LOGGED_IN  = booleanPreferencesKey("is_logged_in")
        private val KEY_EMAIL         = stringPreferencesKey("user_email")
        private val KEY_DISPLAY_NAME  = stringPreferencesKey("user_display_name")
    }

    /** Emette `true` se l'utente è loggato. */
    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        prefs[KEY_IS_LOGGED_IN] ?: false
    }

    /** Emette le informazioni sull'utente corrente, o `null` se non loggato. */
    val userInfo: Flow<UserInfo?> = context.authDataStore.data.map { prefs ->
        val email = prefs[KEY_EMAIL] ?: return@map null
        val name  = prefs[KEY_DISPLAY_NAME] ?: email
        UserInfo(email = email, displayName = name)
    }

    /** Salva le credenziali dell'utente e imposta isLoggedIn = true. */
    suspend fun saveLogin(email: String, displayName: String) {
        context.authDataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_EMAIL]         = email
            prefs[KEY_DISPLAY_NAME]  = displayName
        }
    }

    /** Cancella tutte le credenziali (logout). */
    suspend fun logout() {
        context.authDataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = false
            prefs.remove(KEY_EMAIL)
            prefs.remove(KEY_DISPLAY_NAME)
        }
    }
}
