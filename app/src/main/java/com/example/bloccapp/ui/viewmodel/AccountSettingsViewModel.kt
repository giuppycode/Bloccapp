package com.example.bloccapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloccapp.PermissionManager
import com.example.bloccapp.data.preferences.AuthPreferencesManager
import com.example.bloccapp.data.preferences.ThemeMode
import com.example.bloccapp.data.preferences.ThemePreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val themePrefs = ThemePreferencesManager(application)
    private val authPrefs  = AuthPreferencesManager(application)

    val themeMode: StateFlow<ThemeMode> = themePrefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val userInfo = authPrefs.userInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _usagePermissionGranted = MutableStateFlow(PermissionManager.hasUsageStatsPermission(application))
    val usagePermissionGranted = _usagePermissionGranted.asStateFlow()

    private val _overlayPermissionGranted = MutableStateFlow(PermissionManager.hasOverlayPermission(application))
    val overlayPermissionGranted = _overlayPermissionGranted.asStateFlow()

    private val _notificationsPermissionGranted = MutableStateFlow(PermissionManager.hasNotificationsPermission(application))
    val notificationsPermissionGranted = _notificationsPermissionGranted.asStateFlow()

    fun refreshPermissions() {
        val app = getApplication<Application>()
        _usagePermissionGranted.value = PermissionManager.hasUsageStatsPermission(app)
        _overlayPermissionGranted.value = PermissionManager.hasOverlayPermission(app)
        _notificationsPermissionGranted.value = PermissionManager.hasNotificationsPermission(app)
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { themePrefs.setThemeMode(mode) }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authPrefs.logout()
            onDone()
        }
    }
}
